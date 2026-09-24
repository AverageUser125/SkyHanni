package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component

class ScoreboardUpdateEvent(
    full: List<Component>,
    val old: List<Component>,
) : SkyHanniEvent() {
    val new = full

    val added: List<Component> = full - old.toSet()
    val cleanAdded = added.map { it.string.removeColor() }
    val removed: List<Component> = old - full.toSet()
}
