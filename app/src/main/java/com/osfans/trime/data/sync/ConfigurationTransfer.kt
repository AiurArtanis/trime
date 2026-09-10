package com.osfans.trime.data.sync

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.IOException
import java.util.UUID

/** Transfers source files only; compiled caches and live databases are not portable. */
object ConfigurationTransfer {
    fun readOriginal(context: Context, tree: Uri, staging: File) {
        require(tree.authority == "com.osfans.trime.provider") { "Select the original Trime folder" }
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val listing = SafTreeWalker.listFiles(resolver, tree, rootId)
        val roots = if (listing.any { it.relativePath.startsWith("rime/") }) listOf("shared/", "rime/") else listOf("")
        roots.forEach { prefix ->
            listing.filter { entry ->
                if (prefix.isEmpty()) {
                    !entry.relativePath.startsWith("shared/") && !entry.relativePath.startsWith("rime/")
                } else {
                    entry.relativePath.startsWith(prefix)
                }
            }.forEach entryLoop@{ entry ->
                val path = entry.relativePath.removePrefix(prefix)
                if (!portable(path)) return@entryLoop
                val target = SyncRelativePath.resolveContained(staging, path)
                target.parentFile!!.mkdirs()
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, entry.documentId)
                resolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("Cannot read ${entry.relativePath}")
            }
        }
        check(staging.walkTopDown().any { it.isFile && it.name.endsWith(".schema.yaml") }) {
            "No input schema found; select the original Trime files folder"
        }
    }

    fun portable(path: String): Boolean = path.split('/').none {
        it == "build" || it == "sync" || it.endsWith(".bak") || it.contains(".userdb") || it.startsWith(".")
    } && path != "installation.yaml"

    fun writeTree(context: Context, tree: Uri, source: File) {
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val listing = SafTreeWalker.listTree(resolver, tree, rootId)
        val cache = SafPathCache(listing, rootId)
        val stamp = "${System.currentTimeMillis()}-${UUID.randomUUID()}.bak"
        source.walkTopDown().filter { it.isFile }.forEach { file ->
            val path = file.relativeTo(source).invariantSeparatorsPath
            val old = listing.files.firstOrNull { it.relativePath == path }
            if (old != null) {
                val backup = File(context.cacheDir, "backup-${UUID.randomUUID()}")
                try {
                    val oldUri = DocumentsContract.buildDocumentUriUsingTree(tree, old.documentId)
                    resolver.openInputStream(oldUri)!!.use { input -> backup.outputStream().use { input.copyTo(it) } }
                    val backupPath = "$path.$stamp"
                    AtomicSafFileCopy.copyFromFile(resolver, tree, cache, backup, backupPath, path.substringBeforeLast('/', ""), backupPath.substringAfterLast('/'))
                } finally {
                    backup.delete()
                }
            }
            AtomicSafFileCopy.copyFromFile(resolver, tree, cache, file, path, path.substringBeforeLast('/', ""), file.name)
        }
    }
}
