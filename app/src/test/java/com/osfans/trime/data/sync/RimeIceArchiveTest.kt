package com.osfans.trime.data.sync

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory

private fun archive(file: File, vararg paths: String) {
    ZipOutputStream(file.outputStream()).use { zip ->
        paths.forEach {
            zip.putNextEntry(ZipEntry(it))
            if (!it.endsWith('/')) zip.write("test".toByteArray())
            zip.closeEntry()
        }
    }
}

class RimeIceArchiveTest :
    StringSpec({
        "directory entries work and personal files and caches are excluded" {
            val root = createTempDirectory().toFile()
            try {
                val zip = root.resolve("input.zip")
                archive(zip, "lua/", "lua/test.lua", "default.custom.yaml", "user.yaml", "build/test.yaml", "test.userdb/a", "rime_ice.schema.yaml")
                val output = root.resolve("out")
                RimeIceUpdate.extract(zip, output)
                output.walkTopDown().filter { it.isFile }.map { it.relativeTo(output).invariantSeparatorsPath }.toSet() shouldBe
                    setOf("lua/test.lua", "rime_ice.schema.yaml")
            } finally {
                root.deleteRecursively()
            }
        }
        "parent traversal is rejected" {
            val root = createTempDirectory().toFile()
            try {
                val zip = root.resolve("input.zip")
                archive(zip, "../outside.yaml")
                shouldThrow<SyncRelativePath.PathEscapeException> { RimeIceUpdate.extract(zip, root.resolve("out")) }
                root.resolve("outside.yaml").exists() shouldBe false
            } finally {
                root.deleteRecursively()
            }
        }
        "normalized duplicate paths are rejected" {
            val root = createTempDirectory().toFile()
            try {
                val zip = root.resolve("input.zip")
                archive(zip, "test.yaml", "/test.yaml")
                shouldThrow<IllegalStateException> { RimeIceUpdate.extract(zip, root.resolve("out")) }
            } finally {
                root.deleteRecursively()
            }
        }
    })
