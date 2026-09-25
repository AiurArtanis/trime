// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data

import com.osfans.trime.util.appContext

class SymbolHistory(
    val capacity: Int,
    private val file: java.io.File = appContext.filesDir.resolve(FILE_NAME),
) : LinkedHashMap<String, String>(0, .75f, true) {
    companion object {
        const val FILE_NAME = "symbol_history"
        private val fileLock = Any()
    }

    fun load() = synchronized(fileLock) {
        val all = if (file.exists()) file.readLines() else emptyList()
        clear()
        all.forEach {
            if (it.isNotBlank()) {
                put(it, it)
            }
        }
    }

    fun save() = synchronized(fileLock) {
        file.writeText(values.joinToString("\n"))
    }

    /** Merge with the latest persisted history, including commits from another view instance. */
    fun record(items: List<String>) = synchronized(fileLock) {
        if (items.isNotEmpty()) {
            load()
            items.filter { it.isNotBlank() }.forEach { insert(it) }
            save()
        }
    }

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?) = size > capacity

    fun insert(s: String) = put(s, s)

    fun toOrderedList() = values.toList().reversed()
}
