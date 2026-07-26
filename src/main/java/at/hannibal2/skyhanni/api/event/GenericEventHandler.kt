package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.test.command.ErrorManager.maybeSkipError
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.TimeAndSizeLimitedCache
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting
import kotlin.time.Duration.Companion.minutes

class GenericEventHandler<T : GenericSkyHanniEvent<*>> private constructor(
    name: String,
    listeners: List<Listener>,
    private val canReceiveCancelled: Boolean,
) : AbstractEventHandler<T>(name) {

    private class TypeBucket(
        val type: Class<*>,
        val listeners: Array<Listener>,
    )

    private val buckets: Array<Array<TypeBucket>?>

    private val resolvedTypes = TimeAndSizeLimitedCache<Class<*>, Array<Listener>>(
        maxSize = 128,
        expireAfterWrite = 10.minutes,
    )

    init {
        val localBuckets = arrayOfNulls<MutableMap<Class<*>, MutableList<Listener>>>(IslandBuckets.BUCKET_COUNT)

        for (listener in listeners.sortedBy { it.priority }) {
            requireNotNull(listener.generic) { "Listener ${listener.name} does not have a generic type!" }

            for (index in listener.islandIndices) {
                val bucket = localBuckets[index]
                if (bucket != null) {
                    val typeBucket = bucket[listener.generic]
                    if (typeBucket != null) {
                        typeBucket += listener
                    } else {
                        bucket[listener.generic] = mutableListOf(listener)
                    }
                } else {
                    localBuckets[index] = mutableMapOf(
                        listener.generic to mutableListOf(listener)
                    )
                }
            }
        }

        buckets = Array(IslandBuckets.BUCKET_COUNT) { index ->
            localBuckets[index]
                ?.map { (type, listeners) ->
                    TypeBucket(type, listeners.toTypedArray())
                }
                ?.toTypedArray()
        }
    }

    constructor(event: Class<out GenericSkyHanniEvent<*>>, listeners: List<Listener>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        listeners,
        listeners.any { it.receiveCancelled },
    )

    override fun post(event: T, onError: ((Throwable) -> Unit)?) {
        invokeLog.invokeCount++
        if (SkyHanniEvents.isDisabledHandler(name)) return

        val buckets = buckets.getOrNull(SkyHanniEvents.getCurrentIslandIndex()) ?: return
        val listeners = resolvedTypes[event.type] ?: resolveListeners(event.type, buckets)

        var errors = 0

        for (listener in listeners) {
            if (!listener.shouldInvoke(event)) continue

            try {
                listener.invoker.accept(event)
            } catch (originalThrowable: Throwable) {
                val throwable = originalThrowable.maybeSkipError()
                errors++
                if (errors <= 3) {
                    val errorName = throwable::class.simpleName ?: "error"
                    val aOrAn = StringUtils.optionalAn(errorName)
                    val message = "Caught $aOrAn $errorName in ${listener.name} at $name: ${throwable.message}"
                    ErrorManager.logErrorWithData(throwable, message, ignoreErrorCache = onError != null)
                }
                onError?.invoke(throwable)
            }

            if (event.isCancelled && !canReceiveCancelled) {
                return
            }
        }

        if (errors > 3) {
            val hiddenErrors = errors - 3
            ChatUtils.chat(
                componentBuilder {
                    append("[SkyHanni/${SkyHanniMod.VERSION}] $hiddenErrors more errors in $name are hidden!")
                    withColor(ChatFormatting.RED)
                }
            )
        }
    }

    private fun resolveListeners(
        type: Class<*>,
        buckets: Array<TypeBucket>,
    ): Array<Listener> {
        val listeners = buckets
            .asSequence()
            .filter { it.type.isAssignableFrom(type) }
            .flatMap { it.listeners.asSequence() }
            .sortedBy { it.priority }
            .toList()
            .toTypedArray()

        resolvedTypes[type] = listeners

        return listeners
    }
}
