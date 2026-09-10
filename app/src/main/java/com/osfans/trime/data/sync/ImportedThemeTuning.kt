package com.osfans.trime.data.sync

import android.content.Context
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File

object ImportedThemeTuning {
    fun apply(context: Context, staging: File) {
        val parser = Yaml(SafeConstructor(LoaderOptions()))
        val bundled = context.assets.open("shared/tongwenfeng.trime.yaml").bufferedReader().use {
            parser.load<Map<String, Any>>(it)
        }
        val style = bundled.getValue("style") as Map<*, *>
        val custom = staging.resolve("tongwenfeng.trime.custom.yaml")
        val root = if (custom.exists()) {
            custom.copyTo(staging.resolve("tongwenfeng.trime.custom.yaml.${System.currentTimeMillis()}.bak"))
            (parser.load<Map<String, Any>?>(custom.readText()) ?: emptyMap()).toMutableMap()
        } else {
            mutableMapOf()
        }
        require(root["patch"] == null || root["patch"] is Map<*, *>) { "Invalid theme patch mapping" }
        val patch = (root["patch"] as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }?.toMutableMap()
            ?: mutableMapOf()
        listOf(
            "candidate_text_size",
            "candidate_spacing",
            "candidate_text_size_by_length_portrait",
            "vertical_gap",
            "keyboard_height",
            "keyboard_height_land",
        ).forEach {
            patch["style/$it"] = checkNotNull(style[it])
        }
        root["patch"] = patch
        val options = DumperOptions().apply { defaultFlowStyle = DumperOptions.FlowStyle.BLOCK }
        custom.writeText(Yaml(options).dump(root))
        // Do not let a generated luna_pinyin patch shadow the imported schema list.
        val defaultCustom = staging.resolve("default.custom.yaml")
        if (!defaultCustom.exists()) defaultCustom.writeText("patch: {}\n")
    }
}
