// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data

import com.osfans.trime.util.appContext

class SymbolHistory(
    val capacity: Int,
    private val file: java.io.File = appContext.filesDir.resolve(FILE_NAME),
) {
    companion object {
        const val FILE_NAME = "symbol_history"
        private const val FORMAT = "#astra-symbol-history-v2"
        private val fileLock = Any()
        // Explicit list: do not discard mathematical signs or emoji by Unicode category.
        private const val COMMON_PUNCTUATION = ",，.。．:：;；、!！?？\"'＂＇“”‘’「」『』()（）[]【】{}｛｝《》〈〉…—–"

        private fun shouldKeep(symbol: String): Boolean =
            symbol.isNotBlank() && '\n' !in symbol && '\r' !in symbol &&
                symbol.any { !it.isWhitespace() && it !in COMMON_PUNCTUATION && it != '\uFE0E' && it != '\uFE0F' }
    }

    // Insertion order stores recency, oldest first; counts determine display priority.
    private val counts = linkedMapOf<String, Long>()

    fun load() = synchronized(fileLock) {
        val lines = if (file.exists()) file.readLines() else emptyList()
        counts.clear()
        val versioned = lines.firstOrNull() == FORMAT
        (if (versioned) lines.drop(1) else lines).forEach { line ->
            val fields = if (versioned) line.split('\t', limit = 2) else emptyList()
            val symbol = if (versioned) fields.getOrNull(1).orEmpty() else line
            val count = if (versioned) fields.firstOrNull()?.toLongOrNull()?.coerceAtLeast(1) ?: 1L else 1L
            if (shouldKeep(symbol)) counts[symbol] = count
        }
        trim()
    }

    private fun save() {
        // Preserve the original uncounted history on the first upgrade.
        if (file.exists() && file.useLines { it.firstOrNull() } != FORMAT) {
            val backup = java.io.File(file.path + ".legacy.bak")
            if (!backup.exists()) file.copyTo(backup)
        }
        file.writeText(FORMAT + "\n" + counts.entries.joinToString("\n") { (symbol, count) -> "$count\t$symbol" })
    }

    /** Reload before merging so different input paths cannot overwrite each other's counts. */
    fun record(items: List<String>) = synchronized(fileLock) {
        val symbols = items.filter(::shouldKeep).distinct()
        if (symbols.isNotEmpty()) {
            load()
            symbols.forEach { symbol ->
                val previous = counts.remove(symbol) ?: 0L
                counts[symbol] = if (previous == Long.MAX_VALUE) previous else previous + 1
            }
            trim()
            save()
        }
    }

    private fun trim() {
        while (counts.size > capacity.coerceAtLeast(0)) counts.remove(counts.keys.first())
    }

    fun toOrderedList(): List<String> = counts.entries.toList().asReversed()
        .sortedByDescending { it.value }.map { it.key }
}
