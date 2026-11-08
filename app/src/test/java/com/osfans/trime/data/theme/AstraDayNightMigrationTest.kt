/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.theme

import android.content.SharedPreferences
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.lang.reflect.Proxy

/** Minimal preference store: exercise the real migration without an Android process. */
private fun preferences(values: MutableMap<String, Boolean>): SharedPreferences {
    val editor = Proxy.newProxyInstance(
        SharedPreferences.Editor::class.java.classLoader,
        arrayOf(SharedPreferences.Editor::class.java),
    ) { proxy, method, args ->
        when (method.name) {
            "putBoolean" -> {
                values[args!![0] as String] = args[1] as Boolean
                proxy
            }
            "apply" -> null
            else -> error("Unexpected editor call: ${method.name}")
        }
    } as SharedPreferences.Editor
    return Proxy.newProxyInstance(
        SharedPreferences::class.java.classLoader,
        arrayOf(SharedPreferences::class.java),
    ) { _, method, args ->
        when (method.name) {
            "getBoolean" -> values[args!![0] as String] ?: args[1]
            "edit" -> editor
            else -> error("Unexpected preferences call: ${method.name}")
        }
    } as SharedPreferences
}

class AstraDayNightMigrationTest : StringSpec({
    "only Astra restores following once and later explicit opt-out is preserved" {
        val values = mutableMapOf(ThemePrefs.FOLLOW_SYSTEM_DAY_NIGHT to false)
        val prefs = ThemePrefs(preferences(values))
        prefs.migrateAstraDayNight("标准")
        prefs.followSystemDayNight.getValue() shouldBe false
        prefs.migrateAstraDayNight("Astra")
        prefs.followSystemDayNight.getValue() shouldBe true
        prefs.followSystemDayNight.setValue(false)
        ThemePrefs(preferences(values)).migrateAstraDayNight("Astra")
        prefs.followSystemDayNight.getValue() shouldBe false
    }
})
