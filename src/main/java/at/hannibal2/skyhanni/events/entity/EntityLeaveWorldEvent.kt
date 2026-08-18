package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.world.entity.Entity

@Deprecated("Use EntityLeaveWorldEvent instead", ReplaceWith("EntityLeaveWorldEvent"))
typealias EntityRemovedEvent<T> = EntityLeaveWorldEvent<T>

class EntityLeaveWorldEvent<T : Entity>(val entity: T) : GenericSkyHanniEvent<T>(entity.javaClass)
