package com.osfans.trime.core

import com.osfans.trime.BuildConfig
import com.osfans.trime.util.appContext
import java.io.File

/** Only fixed stage names and exception class names. Never input, YAML or logcat. */
internal object MaintenanceDiagnostics {
    private val file get() = File(appContext.filesDir, "maintenance-stages.txt")

    @Synchronized
    fun record(stage: String, errorClass: String = "") {
        runCatching {
            val target = file
            if (target.length() > 64 * 1024) {
                target.writeText(target.readLines().takeLast(128).joinToString("\n", postfix = "\n"))
            }
            target.appendText("${System.currentTimeMillis()} $stage $errorClass\n")
        }
    }

    @Synchronized
    fun snapshot(): String = "Astra maintenance stages / ${BuildConfig.BUILD_VERSION_NAME}\n" +
        runCatching { file.readText() }.getOrDefault("No maintenance stages recorded.\n")
}
