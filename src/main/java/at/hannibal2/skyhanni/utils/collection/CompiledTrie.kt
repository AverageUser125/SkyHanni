package at.hannibal2.skyhanni.utils.collection

import at.hannibal2.skyhanni.data.model.TabWidget

/**
 * A trie that is compiled into a flat array for fast prefix matching.
 * Mostly used for fast matching of prefixes in strings, where each prefix is associated with a value of type [T].
 *
 * @param T The type of the values associated with the prefixes.
 * @property prefixes A list of pairs of prefixes and their associated values.
 */
class CompiledTrie<T> private constructor(
    private val transitions: IntArray,
    private val matches: Array<List<T>>,
) {

    constructor(prefixes: List<Pair<String, T>>) : this(compileTransitions(prefixes), compileMatches(prefixes))

    fun find(input: String): List<T> {
        val result = ArrayList<T>()
        var node = 0

        for (char in input) {
            if (char.code >= CHARACTER_COUNT) break

            val next = transitions[node * CHARACTER_COUNT + char.code]
            if (next == -1) break

            node = next

            if (matches[node].isNotEmpty()) {
                result += matches[node]
            }
        }

        return result
    }

    companion object {
        private const val CHARACTER_COUNT = 128

        fun <T> of(prefixes: List<Pair<String, T>>): CompiledTrie<T> =
            CompiledTrie(prefixes)

        fun <T> empty(): CompiledTrie<T> =
            CompiledTrie(IntArray(0), emptyArray())

        private fun <T> compileTransitions(prefixes: List<Pair<String, T>>): IntArray {
            if (prefixes.isEmpty()) return IntArray(0)

            val nodes = mutableListOf<Node<T>>()
            nodes += Node()

            for ((prefix, value) in prefixes) {
                var node = 0

                for (char in prefix) {
                    require(char.code < CHARACTER_COUNT) {
                        "Non-ASCII character in prefix: $char"
                    }

                    val next = nodes[node].children[char.code]

                    node = if (next != -1) {
                        next
                    } else {
                        val newNode = nodes.size
                        nodes += Node()
                        nodes[node].children[char.code] = newNode
                        newNode
                    }
                }

                nodes[node].matches += value
            }

            return IntArray(nodes.size * CHARACTER_COUNT) { index ->
                nodes[index / CHARACTER_COUNT].children[index % CHARACTER_COUNT]
            }
        }

        private fun <T> compileMatches(prefixes: List<Pair<String, T>>): Array<List<T>> {
            if (prefixes.isEmpty()) return emptyArray()

            val nodes = mutableListOf<Node<T>>()
            nodes += Node()

            for ((prefix, value) in prefixes) {
                var node = 0

                for (char in prefix) {
                    val next = nodes[node].children[char.code]

                    node = if (next != -1) {
                        next
                    } else {
                        val newNode = nodes.size
                        nodes += Node()
                        nodes[node].children[char.code] = newNode
                        newNode
                    }
                }

                nodes[node].matches += value
            }

            return Array(nodes.size) { nodes[it].matches.toList() }
        }

        private class Node<T> {
            val children = IntArray(CHARACTER_COUNT) { -1 }
            val matches = mutableListOf<T>()
        }
    }
}
