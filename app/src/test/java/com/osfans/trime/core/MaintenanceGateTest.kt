package com.osfans.trime.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MaintenanceGateTest : StringSpec({
    "UI option reads return cache immediately during maintenance" {
        val gate = MaintenanceGate()
        gate.run {
            gate.tryRun("cached") { error("native engine is closed") } shouldBe "cached"
        }
        gate.tryRun("cached") { "native" } shouldBe "native"
    }
    "input cannot reach an engine stopped across suspended import" {
        coroutineScope {
            val gate = MaintenanceGate()
            val imported = CompletableDeferred<Unit>()
            val stages = mutableListOf<String>()
            val maintenance = async(start = CoroutineStart.UNDISPATCHED) {
                gate.run {
                    stages += "stop"
                    imported.await()
                    stages += "restart"
                }
            }
            val input = async(start = CoroutineStart.UNDISPATCHED) { gate.run { stages += "input" } }
            input.isCompleted shouldBe false
            imported.complete(Unit)
            maintenance.await()
            input.await()
            stages shouldBe listOf("stop", "restart", "input")
        }
    }
    "failure releases the gate for recovery and later operations" {
        val gate = MaintenanceGate()
        runCatching { gate.run { error("failed") } }.isFailure shouldBe true
        gate.run { "recovered" } shouldBe "recovered"
    }
    "cancelling a waiting caller does not release active maintenance" {
        coroutineScope {
            val gate = MaintenanceGate()
            val release = CompletableDeferred<Unit>()
            val maintenance = async(start = CoroutineStart.UNDISPATCHED) { gate.run { release.await() } }
            val cancelled = async(start = CoroutineStart.UNDISPATCHED) { gate.run { error("must not run") } }
            cancelled.cancel()
            val next = async(start = CoroutineStart.UNDISPATCHED) { gate.run { true } }
            next.isCompleted shouldBe false
            release.complete(Unit)
            maintenance.await()
            next.await() shouldBe true
        }
    }
})
