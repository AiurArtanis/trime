package com.osfans.trime.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RimeApiDelegateTest :
    StringSpec({
        "compiled daemon delegate implements the configuration transaction ABI" {
            val proxy = Class.forName("com.osfans.trime.daemon.RimeDaemon\$rimeImpl\$2\$1", false, javaClass.classLoader)
            val api = RimeApi::class.java.methods.single { it.name == "replaceConfiguration" }
            proxy.getDeclaredMethod(api.name, *api.parameterTypes).returnType shouldBe api.returnType
        }
    })
