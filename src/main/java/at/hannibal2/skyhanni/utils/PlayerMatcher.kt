package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource

/**
 * Builder-backed utility for dynamically checking whether a player belongs
 * to one or more player categories or explicit player names.
 *
 * The underlying player sources are evaluated when [matches] is called,
 * since player lists are not constant.
 */
class PlayerMatcher private constructor(
    private val predicate: (String) -> Boolean,
) {

    fun matches(player: String): Boolean = predicate(player)

    class Builder {
        private val filters = mutableListOf<(String) -> Boolean>()

        fun includeAllSources(): Builder {
            filters += { player ->
                PlayerNameSource.entries.any { player in it.usernames }
            }
            return this
        }

        fun include(vararg categories: PlayerNameSource): Builder {
            filters += { player ->
                categories.any { player in it.usernames }
            }
            return this
        }

        fun include(categories: Collection<PlayerNameSource>): Builder {
            filters += { player ->
                categories.any { player in it.usernames }
            }
            return this
        }

        fun exclude(vararg categories: PlayerNameSource): Builder {
            filters += { player ->
                categories.none { player in it.usernames }
            }
            return this
        }

        fun includePlayers(vararg players: String): Builder {
            filters += { player -> player in players }
            return this
        }

        fun includePlayers(players: Collection<String>): Builder {
            filters += { player -> player in players }
            return this
        }

        fun excludePlayers(vararg players: String): Builder {
            filters += { player -> player !in players }
            return this
        }

        fun filter(predicate: (String) -> Boolean): Builder {
            filters += predicate
            return this
        }

        fun filterNot(predicate: (String) -> Boolean): Builder {
            filters += { player -> !predicate(player) }
            return this
        }

        fun build(): PlayerMatcher {
            return PlayerMatcher { player ->
                filters.all { it(player) }
            }
        }
    }

    companion object {
        fun builder(block: Builder.() -> Unit): PlayerMatcher {
            return Builder()
                .apply(block)
                .build()
        }
    }
}
