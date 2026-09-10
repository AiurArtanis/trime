package com.osfans.trime.data.sync

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.IOException
import java.util.UUID

/** Transfers source files only; compiled caches and live databases are not portable. */
object ConfigurationTransfer {
    /** Complete a shared-only source with the existing /rime tree. User choices win. */
    fun mergeDestination(context: Context, tree: Uri, staging: File) {
        val resolver = context.contentResolver
        val root = DocumentsContract.getTreeDocumentId(tree)
        SafTreeWalker.listFiles(resolver, tree, root).forEach { entry ->
            val path = entry.relativePath
            if (!portable(path)) return@forEach
            val target = SyncRelativePath.resolveContained(staging, path)
            if (!shouldMergeDestination(path, target.exists())) return@forEach
            target.parentFile!!.mkdirs()
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, entry.documentId)
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } ?: throw IOException("Cannot read destination configuration")
        }
    }

    internal fun shouldMergeDestination(path: String, existsInSource: Boolean): Boolean =
        !existsInSource || path.endsWith(".custom.yaml") ||
            path in setOf("default.yaml", "user.yaml", "custom_phrase.txt")

    fun readOriginal(context: Context, tree: Uri, staging: File) {
        require(tree.authority == "com.osfans.trime.provider") { "Select the original Trime folder" }
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val listing = SafTreeWalker.listFiles(resolver, tree, rootId)
        val roots = sourceRoots(listing.map { it.relativePath })
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

    internal fun sourceRoots(paths: List<String>): List<String> =
        if (paths.any { it.startsWith("shared/") || it.startsWith("rime/") }) {
            listOf("shared/", "rime/")
        } else {
            listOf("")
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
                    if (sameContents(file, backup)) return@forEach
                    val backupPath = "$path.$stamp"
                    AtomicSafFileCopy.copyFromFile(resolver, tree, cache, backup, backupPath, path.substringBeforeLast('/', ""), backupPath.substringAfterLast('/'))
                } finally {
                    backup.delete()
                }
            }
            AtomicSafFileCopy.copyFromFile(resolver, tree, cache, file, path, path.substringBeforeLast('/', ""), file.name)
        }
    }

    private fun sameContents(left: File, right: File): Boolean {
        if (left.length() != right.length()) return false
        fun digest(file: File): ByteArray {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest()
        }
        return digest(left).contentEquals(digest(right))
    }
}
