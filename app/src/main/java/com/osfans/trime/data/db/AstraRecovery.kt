/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.db

import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** v15 retired the old databases; copy them back without touching the new database or originals. */
internal object AstraRecovery {
    fun restore(databaseDir: File, filesDir: File) {
        for (name in listOf("clipboard.db", "collection.db")) {
            val destination = databaseDir.resolve(name)
            if (destination.exists()) continue
            val retired = databaseDir.resolve("$name.legacy")
            val snapshot = filesDir.resolve("astra-upgrade-15/private")
            val source = when {
                retired.isFile -> retired
                snapshot.resolve(".complete").isFile && snapshot.resolve(name).isFile -> snapshot.resolve(name)
                else -> continue // v15 did not retire this database, or it never existed.
            }
            // Stage and verify the whole SQLite family before publishing the main file last.
            val staging = filesDir.resolve("astra-recovery-16/${UUID.randomUUID()}")
            check(staging.mkdirs())
            val files = listOf("-wal", "-shm", "").mapNotNull { suffix ->
                val from = if (source == retired) databaseDir.resolve("$name$suffix.legacy") else snapshot.resolve(name + suffix)
                if (!from.exists()) return@mapNotNull null
                val staged = staging.resolve(name + suffix)
                from.copyTo(staged)
                check(hash(from).contentEquals(hash(staged))) { "Recovery copy verification failed: $name$suffix" }
                staged to databaseDir.resolve(name + suffix)
            }
            // Never overwrite an unexpected live sidecar from an incomplete/new database.
            check(files.all { (staged, target) -> !target.exists() || hash(staged).contentEquals(hash(target)) }) {
                "Recovery destination already exists with different data: $name"
            }
            databaseDir.mkdirs()
            files.forEach { (staged, target) ->
                if (!target.exists()) {
                    val temp = databaseDir.resolve(".recovery-${UUID.randomUUID()}")
                    staged.copyTo(temp)
                    check(hash(staged).contentEquals(hash(temp))) { "Recovery publish verification failed: $name" }
                    check(temp.renameTo(target)) { "Cannot publish recovered database: $name" }
                }
            }
        }
    }

    private fun hash(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32768)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest()
    }
}
