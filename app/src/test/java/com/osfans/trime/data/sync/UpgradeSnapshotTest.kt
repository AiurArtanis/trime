/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.sync

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class UpgradeSnapshotTest :
    StringSpec({
        "snapshot preserves exact nested bytes and never overwrites the original backup on retry" {
            val dir = Files.createTempDirectory("astra-upgrade").toFile()
            try {
                val source = dir.resolve("source").apply { mkdirs() }
                source.resolve("words.userdb").mkdirs()
                source.resolve("words.userdb/data").writeBytes(byteArrayOf(0, 1, -1, 2))
                source.resolve("default.custom.yaml").writeText("personal config")
                val backup = dir.resolve("backup")
                UpgradeSnapshot.ensure(backup) { UpgradeSnapshot.copyTree(source, it.resolve("local")) }
                backup.resolve("local/words.userdb/data").readBytes().toList() shouldBe listOf<Byte>(0, 1, -1, 2)
                source.resolve("default.custom.yaml").writeText("edited later")
                UpgradeSnapshot.ensure(backup) { error("A completed backup must not be overwritten") }
                backup.resolve("local/default.custom.yaml").readText() shouldBe "personal config"
                source.resolve("default.custom.yaml").readText() shouldBe "edited later"
            } finally {
                dir.deleteRecursively()
            }
        }

        "failed backup has no completion marker and a retry can create a complete snapshot" {
            val dir = Files.createTempDirectory("astra-upgrade-failed").toFile()
            try {
                val backup = dir.resolve("backup")
                shouldThrow<IllegalStateException> {
                    UpgradeSnapshot.ensure(backup) {
                        it.resolve("partial").writeText("partial")
                        error("read failure")
                    }
                }
                backup.exists() shouldBe false
                UpgradeSnapshot.ensure(backup) { it.resolve("data").writeText("complete") }
                backup.resolve(".complete").isFile shouldBe true
                backup.resolve("data").readText() shouldBe "complete"
            } finally {
                dir.deleteRecursively()
            }
        }
    })
