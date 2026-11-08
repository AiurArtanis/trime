/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.theme

/** A manual choice lasts until the next actual system day/night transition. */
internal class DayNightSchemeState {
    var isNight: Boolean = false
        private set

    var manualSchemeId: String? = null

    fun updateSystemMode(night: Boolean) {
        if (isNight != night) manualSchemeId = null
        isNight = night
    }
}
