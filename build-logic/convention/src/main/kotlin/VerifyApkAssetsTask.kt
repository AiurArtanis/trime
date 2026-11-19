import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

abstract class VerifyApkAssetsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDir: DirectoryProperty

    @TaskAction
    fun verify() {
        val root = apkDir.get().asFile.canonicalFile
        val metadata = Json.parseToJsonElement(root.resolve("output-metadata.json").readText()).jsonObject
        // Verify only this build's outputs, not stale APKs from another checkout.
        val apks = metadata.getValue("elements").jsonArray.map { element ->
            val name = element.jsonObject.getValue("outputFile").jsonPrimitive.content
            root.resolve(name).canonicalFile.also {
                check(it.parentFile == root && it.isFile && it.extension == "apk") { "Invalid APK output: $name" }
            }
        }
        check(apks.isNotEmpty()) { "No APK produced for asset verification" }
        apks.forEach { apk ->
            val count = ApkAssetVerifier.verify(apk)
            logger.lifecycle("Verified ${apk.name}: $count packaged assets match checksums.json")
        }
    }
}

internal object ApkAssetVerifier {
    fun verify(apk: File): Int = ZipFile(apk).use { zip ->
        val manifest = checkNotNull(zip.getEntry("assets/checksums.json")) { "APK has no asset manifest" }
        val entries = Json.parseToJsonElement(zip.getInputStream(manifest).bufferedReader().use { it.readText() })
            .jsonObject.getValue("files").jsonObject
        val declared = entries.filterValues { it.jsonPrimitive.content.isNotEmpty() }
        val packaged = zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith("assets/") && it.name != "assets/checksums.json" }
            .map { it.name.removePrefix("assets/") }.toSet()
        check(declared.keys == packaged) {
            "APK asset manifest mismatch: missing=${declared.keys - packaged}, undeclared=${packaged - declared.keys}"
        }
        declared.forEach { (path, checksum) ->
            val bytes = zip.getInputStream(zip.getEntry("assets/$path")).use { it.readBytes() }
            check(!(bytes.size < 512 && bytes.toString(Charsets.UTF_8).trim().startsWith("../../../../data/rime/"))) {
                "Unresolved Git symlink packaged: $path"
            }
            val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            check(actual == checksum.jsonPrimitive.content) { "APK asset hash mismatch: $path" }
        }
        declared.size
    }
}
