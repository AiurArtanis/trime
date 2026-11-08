package com.osfans.trime.data

/** Symbol-only candidates also belong in history; punctuation in prose does not. */
internal object CommittedSymbols {
    private val tokens = Regex(CommittedEmoji.pattern.pattern + "|[\\p{S}\\p{P}]\\p{M}*")

    fun extract(text: String): List<String> {
        val matches = tokens.findAll(text).toList()
        var end = 0
        for (match in matches) {
            if (text.substring(end, match.range.first).any { !it.isWhitespace() }) {
                return CommittedEmoji.extract(text)
            }
            end = match.range.last + 1
        }
        if (text.substring(end).any { !it.isWhitespace() }) return CommittedEmoji.extract(text)
        return matches.map { it.value }.distinct()
    }
}
