package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.test.command.ErrorManager.maybeSkipError
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting

class EventHandler<T : SkyHanniEvent> private constructor(
    name: String,
    listeners: List<Listener>,
    private val canReceiveCancelled: Boolean,
): AbstractEventHandler<T>(name) {

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

    constructor(event: Class<out SkyHanniEvent>, listeners: List<Listener>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        listeners,
        listeners.any { it.receiveCancelled },
    )

    override fun post(event: T, onError: ((Throwable) -> Unit)?) {
        invokeLog.invokeCount++
        if (SkyHanniEvents.isDisabledHandler(name)) return
        val listeners = buckets.getOrNull(SkyHanniEvents.getCurrentIslandIndex()) ?: return

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
                break
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
