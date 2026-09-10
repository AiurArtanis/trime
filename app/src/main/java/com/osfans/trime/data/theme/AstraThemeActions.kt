package com.osfans.trime.data.theme

/** Literal output is independent of the selected Rime punctuation mapping. */
internal object AstraThemeActions {
    fun symbol(kind: String, ascii: Boolean): String = when (kind) {
        "z" -> if (ascii) "_" else "·"
        "comma" -> if (ascii) "`" else "、"
        "tilde" -> "~"
        else -> ""
    }

    fun hint(kind: String, ascii: Boolean): String = if (kind == "z" && ascii) "-" else symbol(kind, ascii)

    fun symbolsLabel(ascii: Boolean): String = if (ascii) "⌘" else "☯"

    fun themeCatalog(shared: List<ThemeItem>, user: List<ThemeItem>): List<ThemeItem> =
        (user + shared).distinctBy { it.configId }
}
