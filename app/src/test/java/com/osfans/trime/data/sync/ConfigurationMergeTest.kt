package com.osfans.trime.data.sync

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ConfigurationMergeTest : StringSpec({
    "shared-only import retains missing external dependencies" {
        ConfigurationTransfer.shouldMergeDestination("cn_dicts/8105.dict.yaml", false) shouldBe true
        ConfigurationTransfer.shouldMergeDestination("lua/rime.lua", false) shouldBe true
    }
    "existing user choices and phrases override source defaults" {
        listOf("default.yaml", "default.custom.yaml", "rime_ice.custom.yaml", "user.yaml", "custom_phrase.txt").forEach {
            ConfigurationTransfer.shouldMergeDestination(it, true) shouldBe true
        }
    }
    "source schema updates are retained" {
        ConfigurationTransfer.shouldMergeDestination("rime_ice.schema.yaml", true) shouldBe false
    }
})
