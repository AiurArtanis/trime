package com.osfans.trime.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Holds input/API calls while maintenance temporarily closes the native engine. */
internal class MaintenanceGate {
    private val mutex = Mutex()

    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock { block() }

    suspend fun <T> tryRun(fallback: T, block: suspend () -> T): T {
        if (!mutex.tryLock()) return fallback
        return try { block() } finally { mutex.unlock() }
    }
}
