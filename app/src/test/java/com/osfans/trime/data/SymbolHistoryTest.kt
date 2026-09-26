package com.osfans.trime.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class SymbolHistoryTest : StringSpec({
    "frequency wins over recency and ties use latest selection after reload" {
        val dir = Files.createTempDirectory("symbol-frequency").toFile()
        try {
            val file = dir.resolve("history")
            val panel = SymbolHistory(180, file)
            val candidates = SymbolHistory(180, file)
            repeat(3) { panel.record(listOf("🤬")) }
            candidates.record(listOf("∞"))
            panel.record(listOf("＞"))
            panel.toOrderedList() shouldBe listOf("🤬", "＞", "∞")
            repeat(2) { candidates.record(listOf("∞")) }
            val reloaded = SymbolHistory(180, file)
            reloaded.load()
            reloaded.toOrderedList() shouldBe listOf("∞", "🤬", "＞")
            panel.record(listOf("🤬"))
            panel.toOrderedList() shouldBe listOf("🤬", "∞", "＞")
        } finally {
            dir.deleteRecursively()
        }
    }
    "legacy history keeps order and symbols with an unchanged backup" {
        val dir = Files.createTempDirectory("symbol-migration").toFile()
        try {
            val file = dir.resolve("history")
            val legacy = "＞\n🤬\n∞"
            file.writeText(legacy)
            val history = SymbolHistory(180, file)
            history.load()
            history.toOrderedList() shouldBe listOf("∞", "🤬", "＞")
            history.record(listOf("＞"))
            history.toOrderedList() shouldBe listOf("＞", "∞", "🤬")
            history.record(listOf("🤬"))
            history.load()
            history.toOrderedList() shouldBe listOf("🤬", "＞", "∞")
            dir.resolve("history.legacy.bak").readText() shouldBe legacy
        } finally {
            dir.deleteRecursively()
        }
    }
})
