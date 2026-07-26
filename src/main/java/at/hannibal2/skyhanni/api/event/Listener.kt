package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import java.util.function.Consumer

class Listener(
    val name: String,
    val invoker: Consumer<Any>,
    options: HandleEvent,
    val generic: Class<*>?,
) {
    val priority: Int = options.priority
    val receiveCancelled: Boolean = options.receiveCancelled
    val islandIndices: List<Int> = IslandBuckets.createListenerIndices(options)

    @Suppress("JoinDeclarationAndAssignment")
    private val cachedPredicates: List<EventPredicate>
    private var lastCacheGeneration = -1
    private var cachedPredicateValue = false

    private val predicates: List<EventPredicate>

    fun shouldInvoke(event: SkyHanniEvent): Boolean {
        val generation = SkyHanniEvents.getListenerCacheGeneration()
        if (generation != lastCacheGeneration) {
            cachedPredicateValue = cachedPredicates.all { it(event) }
            lastCacheGeneration = generation
        }
        return cachedPredicateValue && predicates.all { it(event) }
    }

    init {
        cachedPredicates = buildList {
            options.onlyOnSkyblockOrFeatures.takeIfNotEmpty()?.let { features ->
                @Suppress("DEPRECATION")
                add { _ -> SkyBlockUtils.inSkyBlock || features.any { it.isSelected() } }
            }
            add { _ -> !SkyHanniEvents.isDisabledInvoker(name) }
        }
        // These predicates can't be cached since they depend on info about the actual event
        predicates = buildList {
            if (!receiveCancelled) add { event -> !event.isCancelled }
        }
    }
}
