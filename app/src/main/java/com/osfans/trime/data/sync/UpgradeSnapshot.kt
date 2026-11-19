/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.sync

import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Publish a complete, verified snapshot; a failed attempt never becomes a migration marker. */
internal object UpgradeSnapshot {
    fun ensure(destination: File, populate: (File) -> Unit) {
        if (destination.resolve(".complete").isFile) return
        check(!destination.exists()) { "Incomplete upgrade snapshot: $destination" }
        destination.parentFile!!.mkdirs()
        val staging = File(destination.parentFile, ".${destination.name}-${UUID.randomUUID()}")
        check(staging.mkdirs())
        try {
            populate(staging)
            staging.resolve(".complete").writeText("1\n")
            check(staging.renameTo(destination)) { "Cannot publish upgrade snapshot" }
        } catch (e: Exception) {
            // Leave partial backups available for diagnosis; never remove source data.
            throw e
        }
    }

    fun copyTree(source: File, target: File) {
        if (!source.exists()) return
        if (source.isFile) {
            target.parentFile!!.mkdirs()
            source.copyTo(target)
            check(digest(source).contentEquals(digest(target))) { "Snapshot verification failed: ${source.name}" }
            target.setLastModified(source.lastModified())
        } else {
            check(target.mkdirs() || target.isDirectory)
            val children = source.listFiles() ?: error("Cannot list snapshot source: $source")
            children.forEach { copyTree(it, target.resolve(it.name)) }
        }
    }

    private fun digest(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32768)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                digest.update(buffer, 0, size)
            }
        }
        return digest.digest()
    }
}
