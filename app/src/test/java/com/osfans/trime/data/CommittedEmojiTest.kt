package com.osfans.trime.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class CommittedEmojiTest : StringSpec({
    "candidate emoji and mixed text record emoji only" {
        CommittedEmoji.extract("🤬") shouldBe listOf("🤬")
        CommittedEmoji.extract("生气🤬，开心😂🤬") shouldBe listOf("🤬", "😂")
        CommittedEmoji.extract("普通文字 nu 123 # * - · ，。𠮷") shouldBe emptyList()
    }
    "combined emoji remain complete" {
        val symbols = listOf("❤️", "👍🏽", "🇨🇳", "1️⃣", "👨‍👩‍👧‍👦", "👩🏽‍💻", "🏳️‍🌈")
        CommittedEmoji.extract(symbols.joinToString(" ")) shouldBe symbols
    }
    "multiple writers merge and restart preserves recent bounded history" {
        val file = Files.createTempFile("symbol-history", ".txt").toFile()
        try {
            val panel = SymbolHistory(3, file)
            val candidates = SymbolHistory(3, file)
            panel.record(listOf("∞", "😂"))
            candidates.record(CommittedEmoji.extract("🤬"))
            panel.record(listOf("👍"))
            candidates.record(listOf("🤬"))
            val restored = SymbolHistory(3, file)
            restored.load()
            restored.toOrderedList() shouldBe listOf("🤬", "👍", "😂")
            candidates.record(emptyList())
            panel.load()
            panel.toOrderedList() shouldBe restored.toOrderedList()
        } finally {
            java.io.File(file.path + ".legacy.bak").delete()
            file.delete()
        }
    }
})
