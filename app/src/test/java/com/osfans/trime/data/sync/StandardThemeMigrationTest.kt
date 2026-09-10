package com.osfans.trime.data.sync

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files

class StandardThemeMigrationTest : StringSpec({
    "restore only owned geometry keys and keep unrelated custom settings with one backup" {
        val root = Files.createTempDirectory("standard-migration").toFile()
        try {
            val file = root.resolve("tongwenfeng.trime.custom.yaml")
            file.writeText("patch:\n  style/vertical_gap: 8\n  style/candidate_text_size: 22\n  style/key_text_size: 28\n  preset_keys/Custom/label: mine\n")
            ImportedThemeTuning.restoreStandard(root, mapOf("vertical_gap" to 12, "candidate_text_size" to 18))
            val content = file.readText()
            val patch = Yaml().load<Map<String, Map<String, Any>>>(content).getValue("patch")
            patch shouldBe mapOf("style/key_text_size" to 28, "preset_keys/Custom/label" to "mine")
            ImportedThemeTuning.restoreStandard(root, mapOf("vertical_gap" to 12, "candidate_text_size" to 18))
            file.readText() shouldBe content
            root.listFiles()!!.count { it.name.endsWith(".bak") } shouldBe 1
        } finally { root.deleteRecursively() }
    }
})
