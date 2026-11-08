package com.osfans.trime.data.theme

import com.osfans.trime.data.theme.model.KeyActionToken
import com.osfans.trime.ime.keyboard.KeyBehavior
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AstraThemeTest :
    StringSpec({
        val astra = ThemeTestSupport.decodeBuiltinTheme("astra.trime.yaml")
        fun token(key: com.osfans.trime.data.theme.model.TextKeyboard.TextKey, behavior: KeyBehavior): String? = ((if (behavior == KeyBehavior.CLICK) key.click else key.longClick) as? KeyActionToken.Plain)?.token

        "Astra paired schemes follow upstream resolution without overriding unrelated choices" {
            for (selected in listOf("astra_solar", "astra_luna")) {
                for (night in listOf(false, true)) {
                    ColorSchemeResolver.resolve(astra.presetColorSchemes, selected, true, night) shouldBe
                        astra.presetColorSchemes.getValue(if (night) "astra_luna" else "astra_solar")
                    ColorSchemeResolver.resolve(astra.presetColorSchemes, selected, false, night) shouldBe
                        astra.presetColorSchemes.getValue(selected)
                }
            }
        }
        "approved palettes include distinct enter colors and paired mode links" {
            val solar = astra.presetColorSchemes.getValue("astra_solar")
            val luna = astra.presetColorSchemes.getValue("astra_luna")
            solar["benter"] shouldBe "0xffDEC69E"
            luna["benter"] shouldBe "0xff4A6483"
            solar["dark_scheme"] shouldBe "astra_luna"
            luna["light_scheme"] shouldBe "astra_solar"
        }

        "Astra inherits personal sizes without changing Standard" {
            astra.name shouldBe "Astra"
            astra.style.verticalGap shouldBe 8
            astra.style.keyboardHeight shouldBe 230
            astra.style.keyboardHeightLand shouldBe 180
            astra.style.candidateTextSize shouldBe 22f
            astra.style.candidateTextSizeByLengthPortrait shouldBe true
            val standard = ThemeTestSupport.decodeBuiltinTheme("tongwenfeng.trime.yaml")
            standard.style.verticalGap shouldBe 12
            standard.style.candidateTextSize shouldBe 18f
            standard.style.candidateTextSizeByLengthPortrait shouldBe false
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
        "both text layouts expose voice input and symbol long-press actions" {
            listOf("default", "letter").forEach { layout ->
                val keys = astra.presetKeyboards.getValue(layout).keys
                fun longPress(click: String) = token(keys.single { token(it, KeyBehavior.CLICK) == click }, KeyBehavior.LONG_CLICK)
                longPress("z") shouldBe "Astra_z_symbol"
                longPress(",") shouldBe "Astra_comma_symbol"
                longPress(".") shouldBe "Astra_tilde"
                longPress("space") shouldBe "VOICE_ASSIST"
                longPress("Astra_symbols") shouldBe "Astra_color"
                keys.single { token(it, KeyBehavior.CLICK) == "z" }.labelSymbol shouldBe ""
            }
        }
        "literal long-press symbols differ only where specified" {
            AstraThemeActions.hint("z", false) shouldBe "·"
            AstraThemeActions.hint("z", true) shouldBe "-"
            AstraThemeActions.symbolsLabel(false) shouldBe "☯"
            AstraThemeActions.symbolsLabel(true) shouldBe "⌘"
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
