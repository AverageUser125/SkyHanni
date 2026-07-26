package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.api.event.EventListeners.Listener
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.SkyBlockUtils
class ListenerCollection(
    listeners: List<Listener>,
) {
    private val buckets: Array<Array<Listener>?>

    init {
        val sorted = listeners.sortedBy { it.priority }

        val localBuckets = arrayOfNulls<MutableList<Listener>>(IslandBuckets.BUCKET_COUNT)

        for (listener in sorted) {
            for (index in listener.islandIndices) {
                val bucket = localBuckets[index]
                if (bucket != null) {
                    bucket += listener
                } else {
                    localBuckets[index] = mutableListOf(listener)
                }
            }
        }

        buckets = Array(IslandBuckets.BUCKET_COUNT) {
            localBuckets[it]?.toTypedArray()
        }
    }

    fun current() =
        buckets.getOrNull(IslandBuckets.currentIndex())
}
