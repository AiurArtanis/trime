/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.daemon

import com.osfans.trime.core.data.UserDataManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/** UI file sync shares the same native maintenance exclusion as background sync. */
suspend fun syncUserDataSafely(): Boolean {
    if (!UserDataManager.isStorageAvailable()) return false
    val name = "astra-sync-${UUID.randomUUID()}"
    val session = RimeDaemon.createSession(name)
    return try {
        withTimeout(10.minutes) { session.runOnReady { syncUserData() } }
    } finally {
        withContext(NonCancellable) { RimeDaemon.destroySession(name) }
    }
}
