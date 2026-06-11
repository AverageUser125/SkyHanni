package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.VanquisherApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.misc.InvincibilityTimerConfig.MobType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.fishing.SeaCreatureDetectionApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object InvincibilityTimer {
    private val config get() = SkyHanniMod.feature.misc.invincibilityTimer

    private val seaCreatures get() = SeaCreatureDetectionApi.getSeaCreatures()
    private val vanquishers get() = VanquisherApi.getVanquishers()
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
        for (seaCreature in seaCreatures) {
            if (!seaCreature.exists()) continue
            val height = seaCreature.aabb?.ysize ?: continue
            val pos = seaCreature.pos?.up(height) ?: continue
            val time = seaCreature.spawnTime + INVINCIBILITY
            if (time.passedSince() > 1.seconds) continue
            val timeLeft = time.timeUntil()
            event.drawDynamicText(pos, "§b${timeLeft.format(showMilliSeconds = true)}", scaleMultiplier = 1.3)
            if (seaCreature.isOwn) {
                event.drawDynamicText(pos.up(0.5), "§aOWN MOB", scaleMultiplier = 1.3)
            }
        }
    }

    private fun handleVanquisherRender(event: SkyHanniRenderWorldEvent) {
        for (vanquisher in vanquishers) {
            val time = vanquisher.spawnTime + INVINCIBILITY
            if (time.passedSince() > 1.seconds) continue
            val mob = vanquisher.mob
            val height = mob.baseEntity.eyeHeight
            // TODO: confirm that this looks correct, TODO: confirm this TODO is needed
            val pos = vanquisher.mob.getLorenzVec().up(height)
            val timeLeft = time.timeUntil()
            event.drawDynamicText(pos, "§b${timeLeft.format(showMilliSeconds = true)}", scaleMultiplier = 1.3)
            if (vanquisher.isOwn) {
                event.drawDynamicText(pos.up(0.5), "§aOWN MOB", scaleMultiplier = 1.3)
            }
        }
    }

    fun isEnabled() = config.enabled && seaCreatures.isNotEmpty()
}
