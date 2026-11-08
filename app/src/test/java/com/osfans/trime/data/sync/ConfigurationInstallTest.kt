package com.osfans.trime.data.sync

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.io.path.createTempDirectory

class ConfigurationInstallTest :
    StringSpec({
        "replacement backs up old files and rollback removes new files" {
            val root = createTempDirectory().toFile()
            try {
                val source = root.resolve("source").apply { mkdirs() }
                val target = root.resolve("target").apply { mkdirs() }
                source.resolve("default.yaml").writeText("new")
                source.resolve("new.yaml").writeText("added")
                target.resolve("default.yaml").writeText("old")
                target.resolve("personal.yaml").writeText("personal")
                val install = ConfigurationInstall(source, target, root.resolve("backup"))
                install.install()
                target.resolve("default.yaml").readText() shouldBe "new"
                root.resolve("backup/default.yaml").readText() shouldBe "old"
                install.rollback()
                target.resolve("default.yaml").readText() shouldBe "old"
                target.resolve("new.yaml").exists() shouldBe false
                target.resolve("personal.yaml").readText() shouldBe "personal"
            } finally {
                root.deleteRecursively()
            }
        }
        "directory collision does not destroy existing directory" {
            val root = createTempDirectory().toFile()
            try {
                val source = root.resolve("source").apply { mkdirs() }
                val target = root.resolve("target").apply { mkdirs() }
                source.resolve("blocked.yaml").writeText("new")
                target.resolve("blocked.yaml").mkdirs()
                val install = ConfigurationInstall(source, target, root.resolve("backup"))
                shouldThrow<IllegalStateException> { install.install() }
                install.rollback()
                target.resolve("blocked.yaml").isDirectory shouldBe true
            } finally {
                root.deleteRecursively()
            }
        }
    })
