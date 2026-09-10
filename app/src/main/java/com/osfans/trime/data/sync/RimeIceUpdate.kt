package com.osfans.trime.data.sync

import android.content.Context
import android.widget.Toast
import com.osfans.trime.R
import com.osfans.trime.TrimeApplication
import com.osfans.trime.daemon.RimeDaemon
import com.osfans.trime.data.base.DataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream

object RimeIceUpdate {
    private val running = AtomicBoolean()

    fun start(context: Context) {
        val ctx = context.applicationContext
        if (!running.compareAndSet(false, true)) return
        Toast.makeText(ctx, R.string.configuration_working, Toast.LENGTH_LONG).show()
        TrimeApplication.getInstance().coroutineScope.launch {
            val work = File(ctx.cacheDir, "ice-${UUID.randomUUID()}")
            val name = "ice-update-${UUID.randomUUID()}"
            var sessionCreated = false
            try {
                val staging = withContext(Dispatchers.IO) { download(work) }
                // Runtime choices and personal overrides belong to the user.
                staging.walkTopDown().filter { it.isFile }.toList().forEach {
                    val path = it.relativeTo(staging).invariantSeparatorsPath
                    if ((path == "default.yaml" || path == "custom_phrase.txt") &&
                        DataManager.userDataDir.resolve(path).exists()
                    ) {
                        check(it.delete())
                    }
                }
                val install = ConfigurationInstall(
                    staging,
                    DataManager.userDataDir,
                    File(ctx.getExternalFilesDir(null), "backups/ice-${System.currentTimeMillis()}.bak"),
                )
                val external = if (RimeDataSync.usesExternalSync(ctx)) {
                    check(RimeDataSync.hasExternalAccess(ctx)) { "Storage permission is missing" }
                    ExternalConfigurationInstall(ctx, RimeDataSync.treeUri()!!, staging, work.resolve("external-before"))
                } else {
                    null
                }
                val session = RimeDaemon.createSession(name)
                sessionCreated = true
                session.runOnReady {
                    replaceConfiguration(
                        {
                            install.install()
                        },
                        {
                            try {
                                external?.rollback()
                            } finally {
                                install.rollback()
                            }
                        },
                        {
                            external?.prepare()
                            external?.install()
                        },
                    )
                }
                SyncIndex.clear()
                Toast.makeText(ctx, R.string.done, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Timber.e(e, "Rime Ice update failed")
                Toast.makeText(ctx, ctx.getString(R.string.configuration_failed, e.message), Toast.LENGTH_LONG).show()
            } finally {
                withContext(NonCancellable) {
                    if (sessionCreated) RimeDaemon.destroySession(name)
                    withContext(Dispatchers.IO) { work.deleteRecursively() }
                    running.set(false)
                }
            }
        }
    }

    private fun download(work: File): File {
        check(work.mkdirs())
        val metadata = request("https://api.github.com/repos/iDvel/rime-ice/releases/tags/nightly") {
            it.bufferedReader().readText()
        }
        val asset = Json.parseToJsonElement(metadata).jsonObject.getValue("assets").jsonArray
            .first { it.jsonObject.getValue("name").jsonPrimitive.content == "full.zip" }.jsonObject
        val digest = asset.getValue("digest").jsonPrimitive.content
        require(digest.matches(Regex("sha256:[0-9a-fA-F]{64}"))) { "Missing SHA-256 digest" }
        val archive = work.resolve("full.zip")
        request(asset.getValue("browser_download_url").jsonPrimitive.content) { input ->
            archive.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    check(total <= 64L * 1024 * 1024) { "Archive too large" }
                    output.write(buffer, 0, read)
                }
            }
        }
        val hash = MessageDigest.getInstance("SHA-256")
        archive.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                hash.update(buffer, 0, read)
            }
        }
        check(hash.digest().joinToString("") { "%02x".format(it) }.equals(digest.removePrefix("sha256:"), true)) {
            "Archive checksum mismatch"
        }
        val staging = work.resolve("staging")
        extract(archive, staging)
        listOf("rime_ice.schema.yaml", "rime_ice.dict.yaml", "default.yaml").forEach {
            check(staging.resolve(it).isFile) { "Incomplete archive: $it" }
        }
        return staging
    }

    internal fun extract(archive: File, staging: File) {
        var total = 0L
        var count = 0
        val paths = mutableSetOf<String>()
        ZipInputStream(archive.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                check(++count <= 4096) { "Too many ZIP entries" }
                val path = SyncRelativePath.normalize(entry.name.trimEnd('/'))
                val target = SyncRelativePath.resolveContained(staging, path)
                if (entry.isDirectory) continue
                check(paths.add(path)) { "Duplicate ZIP path: $path" }
                val allowed = ConfigurationTransfer.portable(path) &&
                    path !in setOf("squirrel.yaml", "weasel.yaml", "user.yaml") &&
                    !path.endsWith(".custom.yaml") &&
                    target.extension in setOf("yaml", "txt", "lua", "json", "db", "gram")
                val output = if (allowed) {
                    target.parentFile!!.mkdirs()
                    target.outputStream()
                } else {
                    null
                }
                output.use {
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = zip.read(buffer)
                        if (read < 0) break
                        total += read
                        check(total <= 256L * 1024 * 1024) { "Extracted archive too large" }
                        it?.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    private fun <T> request(url: String, block: (java.io.InputStream) -> T): T {
        require(URL(url).protocol == "https")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.setRequestProperty("User-Agent", "Trime")
        try {
            check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
            return connection.inputStream.use(block)
        } finally {
            connection.disconnect()
        }
    }
}
