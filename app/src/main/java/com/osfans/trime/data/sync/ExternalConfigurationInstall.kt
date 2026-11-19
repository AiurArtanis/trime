package com.osfans.trime.data.sync

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

class ExternalConfigurationInstall(
    private val context: Context,
    private val tree: Uri,
    private val source: File,
    private val snapshot: File,
) {
    private val newPaths = mutableListOf<String>()
    private var started = false

    fun prepare() {
        val resolver = context.contentResolver
        val root = DocumentsContract.getTreeDocumentId(tree)
        val existing = SafTreeWalker.listFiles(resolver, tree, root).associateBy { it.relativePath }
        source.walkTopDown().filter { it.isFile }.forEach { file ->
            val path = file.relativeTo(source).invariantSeparatorsPath
            val entry = existing[path]
            if (entry == null) {
                newPaths += path
            } else {
                val dest = SyncRelativePath.resolveContained(snapshot, path)
                dest.parentFile!!.mkdirs()
                val uri = DocumentsContract.buildDocumentUriUsingTree(tree, entry.documentId)
                resolver.openInputStream(uri)!!.use { input -> dest.outputStream().use { input.copyTo(it) } }
            }
        }
    }

    fun install() {
        started = true
        ConfigurationTransfer.writeTree(context, tree, source)
    }

    fun rollback() {
        if (!started) return
        ConfigurationTransfer.writeTree(context, tree, snapshot)
        val resolver = context.contentResolver
        val root = DocumentsContract.getTreeDocumentId(tree)
        val files = SafTreeWalker.listFiles(resolver, tree, root).associateBy { it.relativePath }
        newPaths.forEach { path ->
            files[path]?.let {
                check(DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, it.documentId)))
            }
        }
        started = false
    }
}
