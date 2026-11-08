package com.osfans.trime.data.sync

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ConfigurationTransferTest :
    StringSpec({
        "original provider with only shared files is imported" {
            ConfigurationTransfer.sourceRoots(listOf("shared/default.yaml", "shared/luna_pinyin.schema.yaml")) shouldBe
                listOf("shared/", "rime/")
        }
        "runtime user files override shared files" {
            ConfigurationTransfer.sourceRoots(listOf("shared/default.yaml", "rime/default.yaml")) shouldBe
                listOf("shared/", "rime/")
        }
        "direct configuration directory remains supported" {
            ConfigurationTransfer.sourceRoots(listOf("default.yaml", "rime_ice.schema.yaml")) shouldBe listOf("")
        }
        "empty source cannot invent a schema" {
            ConfigurationTransfer.sourceRoots(emptyList()) shouldBe listOf("")
        }
    })
