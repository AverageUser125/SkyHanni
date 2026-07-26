package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.utils.ReflectionUtils
import java.lang.reflect.Method

typealias EventPredicate = (event: SkyHanniEvent) -> Boolean

class EventListeners private constructor(val name: String, private val isGeneric: Boolean) {

    private val listeners: MutableList<Listener> = mutableListOf()

    constructor(event: Class<*>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        GenericSkyHanniEvent::class.java.isAssignableFrom(event),
    )

    fun removeListener(listener: Any) {
        listeners.removeIf { it.invoker == listener }
    }

    fun addListener(method: Method, instance: Any, options: HandleEvent) {
        val name = buildListenerName(method)
        val eventConsumer = when (method.parameterCount) {
            0 -> createZeroParameterConsumer(method, instance)
            1 -> createSingleParameterConsumer(method, instance)
            else -> throw IllegalArgumentException(
                "Method ${method.name} must have either 0 or 1 parameters.",
            )
        }
        val generic = if (isGeneric) resolveGenericType(method) else null

        listeners.add(Listener(name, eventConsumer, options, generic))
    }

    private fun buildListenerName(method: Method): String {
        val paramTypesString = method.parameterTypes.joinTo(
            StringBuilder(),
            prefix = "(",
            postfix = ")",
            separator = ", ",
            transform = Class<*>::getTypeName,
        ).toString()

        return "${method.declaringClass.name}.${method.name}$paramTypesString"
    }

    private fun createZeroParameterConsumer(method: Method, instance: Any): (Any) -> Unit {
        val runnable = ReflectionUtils.createRunnableFromMethod(instance, method)
        return { _: Any -> runnable.run() }
    }

    private fun createSingleParameterConsumer(method: Method, instance: Any): (Any) -> Unit {
        val consumer = ReflectionUtils.createConsumerFromMethod(instance, method)
        return { event -> consumer.accept(event) }
    }

    private fun resolveGenericType(method: Method): Class<*> =
        method.genericParameterTypes.getOrNull(0)?.let { genericType ->
            ReflectionUtils.resolveUpperBoundSuperClassGenericParameter(
                genericType,
                GenericSkyHanniEvent::class.java.typeParameters[0],
            ) ?: error(
                "Generic event handler type parameter is not present in " +
                    "event class hierarchy for type $genericType",
            )
        } ?: error("Method ${method.name} does not have a generic parameter type.")

    fun getListeners(): List<Listener> = listeners

}
