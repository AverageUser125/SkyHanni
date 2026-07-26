package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.test.command.ErrorManager.maybeSkipError
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting

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

        var errors = 0

        for (bucket in buckets) {
            if (!bucket.type.isAssignableFrom(event.type)) continue

            for (listener in bucket.listeners) {
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
}
