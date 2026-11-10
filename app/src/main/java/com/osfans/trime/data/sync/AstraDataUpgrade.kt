/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.osfans.trime.data.sync

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.io.File

/** CE-only backups. Private clipboard/history data is never copied into Direct Boot storage. */
object AstraDataUpgrade {
    private const val LEGACY = "astra_upstream_legacy_data"
    private const val SYNC_DONE = "astra_upstream_sync_v1"

    fun prepare(context: Context) {
        if (context.isDeviceProtectedStorage) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val legacyFiles = listOf(
            context.filesDir.resolve("symbol_history"),
            context.getDatabasePath("clipboard.db"),
            context.getDatabasePath("collection.db"),
            context.getDatabasePath("clip.db"),
        )
        if (!prefs.contains(LEGACY)) {
            check(prefs.edit().putBoolean(LEGACY, legacyFiles.any { it.exists() } || prefs.contains("astra_day_night_v2")).commit())
        }
        if (!prefs.getBoolean(LEGACY, false)) return
        // A fresh snapshot is necessary after returning from the v16 recovery build.
        // Keep the original v15 snapshot untouched.
        UpgradeSnapshot.ensure(context.filesDir.resolve("astra-upgrade-17/private")) { backup ->
            legacyFiles.forEach { file ->
                listOf("", "-wal", "-shm").forEach { suffix ->
                    UpgradeSnapshot.copyTree(File(file.path + suffix), backup.resolve(file.name + suffix))
                }
            }
            // Keep the exact old settings for recovery, without changing the application's identity.
            UpgradeSnapshot.copyTree(File(context.applicationInfo.dataDir, "shared_prefs"), backup.resolve("shared_prefs"))
        }
    }

    fun needsSyncMigration(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return !context.isDeviceProtectedStorage && prefs.getBoolean(LEGACY, false) && !prefs.getBoolean(SYNC_DONE, false)
    }

    fun finishSyncMigration(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit(commit = true) { putBoolean(SYNC_DONE, true) }
    }
}
