package com.osfans.trime.data.sync

import java.io.File
import java.io.IOException
import java.util.UUID

/** A rollback record is registered before touching each destination file. */
class ConfigurationInstall(private val source: File, private val target: File, val backup: File) {
    private val changes = mutableListOf<Pair<File, File?>>()

    fun install() {
        source.walkTopDown().filter { it.isFile }.forEach { file ->
            val path = file.relativeTo(source).invariantSeparatorsPath
            val dest = SyncRelativePath.resolveContained(target, path)
            dest.parentFile!!.mkdirs()
            check(!dest.exists() || dest.isFile) { "Cannot replace directory $path" }
            val old = if (dest.isFile) {
                backup.resolve(path).also {
                    it.parentFile!!.mkdirs()
                    dest.copyTo(it)
                }
            } else {
                null
            }
            changes += dest to old
            val temp = dest.resolveSibling(".install-${UUID.randomUUID()}")
            try {
                file.copyTo(temp)
                if (!temp.renameTo(dest)) {
                    temp.copyTo(dest, overwrite = true)
                }
            } finally {
                temp.delete()
            }
        }
    }

    fun rollback() {
        var failure: Exception? = null
        changes.asReversed().forEach { (dest, old) ->
            try {
                if (old != null) {
                    old.copyTo(dest, overwrite = true)
                } else if (dest.exists() && !dest.delete()) {
                    throw IOException("Cannot remove $dest")
                }
            } catch (e: Exception) {
                failure = failure?.also { it.addSuppressed(e) } ?: e
            }
        }
        failure?.let { throw it }
        changes.clear()
    }
}
