package at.hannibal2.skyhanni.utils.collection

import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component
import java.util.regex.Pattern

/**
 * A set of patterns optimized for matching many patterns against the same input.
 *
 * Patterns with a known literal prefix are indexed using a compiled trie.
 * Patterns without a useful prefix are kept as fallback patterns.
 *
 * The trie is only used to reduce the number of patterns that need to be
 * actually matched. Pattern matching remains authoritative.
 */
class PatternSet<T> private constructor(
    private val indexed: CompiledTrie<Entry<T>>,
    private val fallback: List<Entry<T>>,
) {
    private data class Entry<T>(
        val pattern: Pattern,
        val value: T,
    )

    fun find(input: String): T? {
        indexed.find(input)
            .firstOrNull { it.pattern.matches(input) }
            ?.let { return it.value }

        return fallback
            .firstOrNull { it.pattern.matches(input) }
            ?.value
    }

    fun find(input: Component): T? = find(input.string.removeColor())

    companion object {
        fun <T> of(patterns: List<Pair<Pattern, T>>): PatternSet<T> {
            val indexed = mutableListOf<Pair<String, Entry<T>>>()
            val fallback = mutableListOf<Entry<T>>()

            for ((pattern, value) in patterns) {
                val entry = Entry(pattern, value)
                val prefixes = extractPrefixes(pattern.pattern())

                if (prefixes.isEmpty()) {
                    fallback += entry
                } else {
                    prefixes.forEach { prefix ->
                        indexed += prefix to entry
                    }
                }
            }

            return PatternSet(
                indexed = CompiledTrie.of(indexed),
                fallback = fallback,
            )
        }

        fun <T> empty(): PatternSet<T> =
            PatternSet(
                indexed = CompiledTrie.empty(),
                fallback = emptyList(),
            )

        private fun extractPrefixes(regex: String): List<String> {
            val prefix = regex
                .removePrefix("\\s*")
                .substringBeforeFirst(
                    '(', '[', '\\', '.', '*', '+', '?', '|'
                )

            return if (prefix.isEmpty()) emptyList()
            else listOf(prefix)
        }

        private fun String.substringBeforeFirst(vararg delimiters: Char): String =
            substring(0, indexOfFirst { it in delimiters }.takeIf { it >= 0 } ?: length)
    }
}
