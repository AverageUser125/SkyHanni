package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils.hasVisibleEquipment
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.awt.Color

//? if >= 26.2 {
import net.azureaaron.renderchest.api.CustomGlowCallback
import net.azureaaron.renderchest.api.GlowConstants
//?}

@SkyHanniModule
object RenderLivingEntityHelper {
    private data class EntityGlowData(val rgb: Int, val condition: () -> Boolean)

    private val entityGlowMap = Int2ObjectOpenHashMap<EntityGlowData>(128)
    private var currentGlowEvent: RenderEntityOutlineEvent? = null

    @JvmStatic
    var isUsingCustomGlow = false
        private set

//? if >= 26.2 {
    init {
        CustomGlowCallback.EVENT.register(::applyRenderChestGlow)
    }

    private fun applyRenderChestGlow(
        entity: Entity,
        @Suppress("UNUSED_PARAMETER") state: EntityRenderState,
    ): Int {
        if (!isUsingCustomGlow) return GlowConstants.NO_GLOW
        return getEntityGlowColor(entity) ?: GlowConstants.NO_GLOW
    }
//?}

    @JvmStatic
    fun postNoXrayOutlineEvent() {
        isUsingCustomGlow = entityGlowMap.values.any { it.condition() } ||
            currentGlowEvent?.entitiesToOutline.orEmpty().isNotEmpty()

        val event = RenderEntityOutlineEvent()
        currentGlowEvent = event
        event.post()
    }

    @JvmStatic
    fun getEntityGlowColor(entity: Entity): Int? {
        if (entity is LivingEntity) {
            if (entity.isInvisible && !entity.hasVisibleEquipment()) return null
            getLivingEntityGlowColor(entity)?.let { return it }
        }
        return getEntityGlowEventColor(entity)
    }

    private fun getEntityGlowEventColor(entity: Entity): Int? =
        currentGlowEvent?.entitiesToOutline?.get(entity)?.rgb

    private fun getLivingEntityGlowColor(entity: LivingEntity): Int? {
        if (GlobalRender.renderDisabled) return null
        val entityGlowData = entityGlowMap[entity.id] ?: return null
        if (!entityGlowData.condition.invoke()) return null
        return entityGlowData.rgb
    }

    @HandleEvent
    private fun onWorldChange() {
        entityGlowMap.clear()
    }

    @HandleEvent
    private fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<LivingEntity>) {
        removeEntityColor(event.entity)
    }

    fun <T : LivingEntity> removeEntityColor(entity: T) {
        val entityId = entity.id
        DelayedRun.runOrNextTick {
            entityGlowMap.remove(entityId)
        }
    }

    fun <T : LivingEntity> setEntityColor(entity: T, color: Color, condition: () -> Boolean) {
        val rgb = color.rgb.takeUnless { it == 0 } ?: return
        val entityId = entity.id
        DelayedRun.runOrNextTick {
            entityGlowMap[entityId] = EntityGlowData(rgb, condition)
        }
    }
}
