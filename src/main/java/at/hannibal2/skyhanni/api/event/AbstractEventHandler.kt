package at.hannibal2.skyhanni.api.event

abstract class AbstractEventHandler<T>(val name: String) {
    val invokeLog = SkyHanniEvents.EventInvokeLog()

    abstract fun post(event: T, onError: ((Throwable) -> Unit)? = null)
}
