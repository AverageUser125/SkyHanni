package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.network.chat.Component

class RawScoreboardUpdateEvent(val rawScoreboard: List<Component>) : SkyHanniEvent()
