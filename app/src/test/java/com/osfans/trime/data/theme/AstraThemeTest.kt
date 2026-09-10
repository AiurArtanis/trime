package com.osfans.trime.data.theme

import com.osfans.trime.data.theme.model.KeyActionToken
import com.osfans.trime.ime.keyboard.KeyBehavior
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AstraThemeTest : StringSpec({
    val astra = ThemeTestSupport.decodeBuiltinTheme("astra.trime.yaml")
    fun token(key: com.osfans.trime.data.theme.model.TextKeyboard.TextKey, behavior: KeyBehavior): String? =
        (key.behaviors[behavior] as? KeyActionToken.Plain)?.token

    "Astra inherits personal sizes without changing Standard" {
        astra.name shouldBe "Astra"
        astra.generalStyle.verticalGap shouldBe 8
        astra.generalStyle.keyboardHeight shouldBe 230
        astra.generalStyle.keyboardHeightLand shouldBe 180
        astra.generalStyle.candidateTextSize shouldBe 22f
        astra.generalStyle.candidateTextSizeByLengthPortrait shouldBe true
        val standard = ThemeTestSupport.decodeBuiltinTheme("tongwenfeng.trime.yaml")
        standard.generalStyle.verticalGap shouldBe 12
        standard.generalStyle.candidateTextSize shouldBe 18f
        standard.generalStyle.candidateTextSizeByLengthPortrait shouldBe false
    }
    "number layout matches the four rows in the reference" {
        val clicks = astra.presetKeyboards.getValue("number").keys.map { token(it, KeyBehavior.CLICK) }
        clicks.chunked(6) shouldBe listOf(
            listOf("{}{Left}", "1", "2", "3", "+", "BackSpace"),
            listOf("_", "4", "5", "6", "-", "Keyboard_defaulten"),
            listOf("(){Left}", "7", "8", "9", "*", "space1"),
            listOf("Keyboard_default", ".", "0", "=", "/", "Return"),
        )
    }
    "both text layouts keep mode switching and new long-press actions" {
        listOf("default", "letter").forEach { layout ->
            val keys = astra.presetKeyboards.getValue(layout).keys
            fun longPress(click: String) = token(keys.single { token(it, KeyBehavior.CLICK) == click }, KeyBehavior.LONG_CLICK)
            longPress("z") shouldBe "Astra_z_symbol"
            longPress(",") shouldBe "Astra_comma_symbol"
            longPress(".") shouldBe "Astra_tilde"
            longPress("space") shouldBe "Mode_switch"
            longPress("Astra_symbols") shouldBe "Astra_color"
        }
    }
    "literal long-press symbols differ only where specified" {
        AstraThemeActions.symbol("z", false) shouldBe "·"
        AstraThemeActions.symbol("z", true) shouldBe "_"
        AstraThemeActions.symbol("comma", false) shouldBe "、"
        AstraThemeActions.symbol("comma", true) shouldBe "`"
        AstraThemeActions.symbol("tilde", false) shouldBe "~"
        AstraThemeActions.symbol("tilde", true) shouldBe "~"
    }
    "duplicate source IDs collapse with user overrides but different IDs survive" {
        val shared = listOf(ThemeItem("trime", "預設"), ThemeItem("tongwenfeng.trime", "标准"), ThemeItem("astra.trime", "Astra"))
        val user = listOf(ThemeItem("trime", "我的預設"), ThemeItem("tongwenfeng.trime", "标准"), ThemeItem("other.trime", "标准"))
        val result = AstraThemeActions.themeCatalog(shared, user)
        result.size shouldBe 4
        result.first { it.configId == "trime" }.name shouldBe "我的預設"
    }
})
