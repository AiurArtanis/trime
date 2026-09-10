package com.osfans.trime.data.sync

import android.content.Context
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File

/** Retire Astra 1-3's modifications to Standard; Astra now has its own source. */
object ImportedThemeTuning {
    private val keys = listOf(
        "candidate_text_size", "candidate_spacing", "candidate_text_size_by_length_portrait",
        "vertical_gap", "keyboard_height", "keyboard_height_land",
    )

    fun apply(context: Context, staging: File) {
        val parser = Yaml(SafeConstructor(LoaderOptions()))
        val bundled = context.assets.open("shared/tongwenfeng.trime.yaml").bufferedReader().use {
            parser.load<Map<String, Any>>(it)
        }
        restoreStandard(staging, bundled.getValue("style") as Map<*, *>)
        val defaultCustom = staging.resolve("default.custom.yaml")
        if (!defaultCustom.exists()) defaultCustom.writeText("patch: {}\n")
    }

    internal fun restoreStandard(staging: File, upstreamStyle: Map<*, *>) {
        val parser = Yaml(SafeConstructor(LoaderOptions()))
        val options = DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK }
        fun update(file: File, custom: Boolean) {
            if (!file.isFile) return
            val root = (parser.load<Map<String, Any>?>(file.readText()) ?: emptyMap()).toMutableMap()
            val section = if (custom) "patch" else "style"
            val previous = root[section] as? Map<*, *> ?: return
            val values = previous.entries.associate { it.key.toString() to it.value }.toMutableMap()
            keys.forEach { key ->
                if (custom) {
                    values.remove("style/$key")
                } else if (upstreamStyle.containsKey(key)) {
                    values[key] = upstreamStyle[key]
                } else {
                    values.remove(key)
                }
            }
            if (custom && values["style"] is Map<*, *>) {
                val nested = (values["style"] as Map<*, *>).entries.associate { it.key.toString() to it.value }.toMutableMap()
                keys.forEach { nested.remove(it) }
                values["style"] = nested
            }
            if (values == previous) return
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(file.readBytes())
                .take(8).joinToString("") { "%02x".format(it) }
            val backup = file.resolveSibling("${file.name}.pre-astra-$hash.bak")
            if (!backup.exists()) file.copyTo(backup)
            root[section] = values
            file.writeText(Yaml(options).dump(root))
        }
        update(staging.resolve("tongwenfeng.trime.yaml"), false)
        update(staging.resolve("tongwenfeng.trime.custom.yaml"), true)
    }
}
