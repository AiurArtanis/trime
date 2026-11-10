/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.db

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import java.nio.file.Files

class AstraRecoveryTest :
    StringSpec({
        "retired database family is restored without changing v15 data and is idempotent" {
            val root = Files.createTempDirectory("astra-recovery").toFile()
            try {
                val db = root.resolve("databases").apply { mkdirs() }
                val files = root.resolve("files").apply { mkdirs() }
                db.resolve("clipboard.db.legacy").writeText("old database")
                db.resolve("clipboard.db-wal.legacy").writeText("old wal")
                db.resolve("clip.db").writeText("new database")
                AstraRecovery.restore(db, files)
                db.resolve("clipboard.db").readText() shouldBe "old database"
                db.resolve("clipboard.db-wal").readText() shouldBe "old wal"
                db.resolve("clip.db").readText() shouldBe "new database"
                db.resolve("clipboard.db").writeText("user changes")
                AstraRecovery.restore(db, files)
                db.resolve("clipboard.db").readText() shouldBe "user changes"
                db.resolve("clipboard.db.legacy").readText() shouldBe "old database"
            } finally {
                root.deleteRecursively()
            }
        }
        "partial sidecar publish retries only when bytes match" {
            val root = Files.createTempDirectory("astra-recovery-retry").toFile()
            try {
                val db = root.resolve("databases").apply { mkdirs() }
                val files = root.resolve("files").apply { mkdirs() }
                db.resolve("clipboard.db.legacy").writeText("old database")
                db.resolve("clipboard.db-wal.legacy").writeText("old wal")
                db.resolve("clipboard.db-wal").writeText("different live wal")
                shouldThrow<IllegalStateException> { AstraRecovery.restore(db, files) }
                db.resolve("clipboard.db").exists() shouldBe false
                db.resolve("clipboard.db-wal").readText() shouldBe "different live wal"
                db.resolve("clipboard.db-wal").writeText("old wal")
                AstraRecovery.restore(db, files)
                db.resolve("clipboard.db").readText() shouldBe "old database"
            } finally { root.deleteRecursively() }
        }
        "only a completed snapshot may restore a missing database" {
            val root = Files.createTempDirectory("astra-recovery-snapshot").toFile()
            try {
                val db = root.resolve("databases").apply { mkdirs() }
                val files = root.resolve("files").apply { mkdirs() }
                val snapshot = files.resolve("astra-upgrade-15/private").apply { mkdirs() }
                snapshot.resolve("collection.db").writeText("favorites")
                AstraRecovery.restore(db, files)
                db.resolve("collection.db").exists() shouldBe false
                snapshot.resolve(".complete").writeText("1")
                AstraRecovery.restore(db, files)
                db.resolve("collection.db").readText() shouldBe "favorites"
            } finally {
                root.deleteRecursively()
            }
        }
    })
