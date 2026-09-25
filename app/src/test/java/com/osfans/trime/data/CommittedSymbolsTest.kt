package com.osfans.trime.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CommittedSymbolsTest : StringSpec({
    "pinyin symbol candidates include fullwidth math and punctuation" {
        CommittedSymbols.extract("＞") shouldBe listOf("＞")
        CommittedSymbols.extract("∞") shouldBe listOf("∞")
        CommittedSymbols.extract("≤ ≥ → ￥ § 、 ·") shouldBe listOf("≤", "≥", "→", "￥", "§", "、", "·")
        CommittedSymbols.extract("∞∞") shouldBe listOf("∞")
    }
    "ordinary text and its punctuation do not pollute history" {
        listOf("你好，世界。", "hello!", "123", "1＞0", "nu", "ＡＢＣ", "", " \n").forEach {
            CommittedSymbols.extract(it) shouldBe emptyList()
        }
        CommittedSymbols.extract("生气🤬，开心😂") shouldBe listOf("🤬", "😂")
    }
    "emoji and combining symbols are not split by generic symbol matching" {
        val symbols = listOf("🤬", "❤️", "👍🏽", "🇨🇳", "1️⃣", "👨‍👩‍👧‍👦", "👩🏽‍💻", "=\u0338", "∞")
        CommittedSymbols.extract(symbols.joinToString(" ")) shouldBe symbols
    }
})
