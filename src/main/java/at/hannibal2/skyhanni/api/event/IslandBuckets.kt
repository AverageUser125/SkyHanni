package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.collections.plusAssign

object IslandBuckets {

    const val OUTSIDE = 0

    /**
     * Offset applied to island ordinals because bucket 0
     * is reserved for the outside-SkyBlock state.
     */
    const val ISLAND_OFFSET = 1
    val BUCKET_COUNT = IslandType.entries.size + ISLAND_OFFSET

    /**
     * Use [SkyHanniEvents.getCurrentIslandIndex] instead
     */
    internal fun currentIndex(): Int {
        if (!SkyBlockUtils.inSkyBlock) return OUTSIDE
        return SkyBlockUtils.currentIsland.ordinal + ISLAND_OFFSET
    }

    internal fun createListenerIndices(options: HandleEvent): List<Int> {
        val islands = getIslands(options)
            .map { it.ordinal }

        if (islands.isEmpty()) {
            return if (options.onlyOnSkyblock) {
                (ISLAND_OFFSET until BUCKET_COUNT).toList()
            } else {
                (OUTSIDE until BUCKET_COUNT).toList()
            }
        }

        return islands.map { it + ISLAND_OFFSET }
    }

    private fun getIslands(options: HandleEvent): List<IslandType> {
        val islandTypes = mutableSetOf<IslandType>()

        options.onlyOnIsland
            .takeUnless { it == IslandType.ANY }
            ?.let(islandTypes::add)

        islandTypes += options.onlyOnIslands

        options.onlyOnIslandTypeTag.forEach { tag -> islandTypes += tag.getTypes() }

        return islandTypes.toList()
    }
}
