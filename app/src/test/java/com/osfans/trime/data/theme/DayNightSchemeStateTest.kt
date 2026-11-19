/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.theme

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DayNightSchemeStateTest :
    StringSpec({
        val astra = ThemeTestSupport.decodeBuiltinTheme("astra.trime.yaml")

        fun active(state: DayNightSchemeState, follow: Boolean = true, selected: String = "astra_solar") = astra.presetColorSchemes[state.manualSchemeId]
            ?: ColorSchemeResolver.resolve(astra.presetColorSchemes, selected, follow, state.isNight)

        "Solar and Luna follow repeated real system transitions" {
            val state = DayNightSchemeState()
            active(state) shouldBe astra.presetColorSchemes.getValue("astra_solar")
            state.updateSystemMode(true)
            active(state) shouldBe astra.presetColorSchemes.getValue("astra_luna")
            state.updateSystemMode(false)
            active(state) shouldBe astra.presetColorSchemes.getValue("astra_solar")
        }

        "manual dark mode survives duplicate configuration and resumes following on the next transition" {
            val state = DayNightSchemeState()
            state.manualSchemeId = "astra_luna"
            state.updateSystemMode(false)
            active(state) shouldBe astra.presetColorSchemes.getValue("astra_luna")
            state.updateSystemMode(true)
            state.manualSchemeId shouldBe null
            active(state) shouldBe astra.presetColorSchemes.getValue("astra_luna")
            state.updateSystemMode(false)
            active(state) shouldBe astra.presetColorSchemes.getValue("astra_solar")
        }

        "manual light mode at night survives a repeated night event but not the next day" {
            val state = DayNightSchemeState()
            state.updateSystemMode(true)
            state.manualSchemeId = "astra_solar"
            state.updateSystemMode(true)
            active(state) shouldBe astra.presetColorSchemes.getValue("astra_solar")
            state.updateSystemMode(false)
            state.manualSchemeId shouldBe null
            state.updateSystemMode(true)
            active(state) shouldBe astra.presetColorSchemes.getValue("astra_luna")
        }

        "explicitly disabled following retains the selected scheme across system changes" {
            val state = DayNightSchemeState()
            state.updateSystemMode(true)
            active(state, follow = false) shouldBe astra.presetColorSchemes.getValue("astra_solar")
        }
    })
