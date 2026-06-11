package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.VanquisherApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.InvincibilityTimerConfig.MobType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactBoundingBoxExtraEntities
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactLocation
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object InvincibilityTimer {

    private val config get() = SkyHanniMod.feature.misc.invincibilityTimer
    private val INVINCIBILITY = 5.seconds

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        if (config.mobTypes.contains(MobType.SEA_CREATURE)) {
            handleSeaCreatureRender(event)
        }

        if (config.mobTypes.contains(MobType.VANQUISHER)) {
            handleVanquisherRender(event)
        }
    }

    private fun handleSeaCreatureRender(event: SkyHanniRenderWorldEvent) {
        for (seaCreature in SeaCreatureDetectionApi.getSeaCreatures()) {
            if (!seaCreature.exists()) continue
            if (!seaCreature.rarity.isAtLeast(LorenzRarity.LEGENDARY)) continue

            val height = seaCreature.aabb?.ysize ?: continue
            val pos = seaCreature.pos?.up(height) ?: continue

            renderTimer(
                event = event,
                pos = pos,
                spawnTime = seaCreature.spawnTime,
                isOwn = seaCreature.isOwn,
            )
        }
    }

    private fun handleVanquisherRender(event: SkyHanniRenderWorldEvent) {
        for (vanquisher in VanquisherApi.getVanquishers()) {
            val mob = vanquisher.mob
            val height = event.exactBoundingBoxExtraEntities(mob).ysize
            val pos = event.exactLocation(mob).up(height)

            renderTimer(
                event = event,
                pos = pos,
                spawnTime = vanquisher.spawnTime,
                isOwn = vanquisher.isOwn,
            )
        }
    }

    private fun renderTimer(
        event: SkyHanniRenderWorldEvent,
        pos: LorenzVec,
        spawnTime: ServerTimeMark,
        isOwn: Boolean,
    ) {
        val invincibilityEnd = spawnTime + INVINCIBILITY
        if (invincibilityEnd.passedSince() > 1.seconds) return
        val timeLeft = invincibilityEnd.timeUntil()

        event.drawDynamicText(
            pos,
            "§b${timeLeft.format(showMilliSeconds = true)}",
            scaleMultiplier = 1.3,
        )

        if (isOwn) {
            event.drawDynamicText(
                pos.up(0.5),
                "§aOWN MOB",
                scaleMultiplier = 1.3,
            )
        }
    }

    fun isEnabled() = config.enabled && config.mobTypes.isNotEmpty()
}
