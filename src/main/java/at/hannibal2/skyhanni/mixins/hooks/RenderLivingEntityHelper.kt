package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.hasVisibleEquipment
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

//? if >= 26.2 {
import net.azureaaron.renderchest.api.CustomGlowCallback
import net.azureaaron.renderchest.api.GlowConstants
//?}

@SkyHanniModule
object RenderLivingEntityHelper {

    private val entityColorMap = mutableMapOf<LivingEntity, Color>()
    private val entityColorCondition = ConcurrentHashMap<LivingEntity, () -> Boolean>()
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
        isUsingCustomGlow = entityColorCondition.values.any { it() } ||
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
        currentGlowEvent?.entitiesToOutline?.get(entity)?.rgb?.takeIf { it != 0 }

    private fun getLivingEntityGlowColor(entity: LivingEntity): Int? {
        if (GlobalRender.renderDisabled) return null
        val entityColor = entityColorMap[entity] ?: return null
        val condition = entityColorCondition[entity] ?: return null
        if (!condition.invoke()) return null
        return entityColor.rgb.takeIf { it != 0 }
    }

    @HandleEvent
    private fun onWorldChange() {
        entityColorMap.clear()
        entityColorCondition.clear()
    }

    @HandleEvent
    private fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<LivingEntity>) {
        removeEntityColor(event.entity)
    }

    fun <T : LivingEntity> removeEntityColor(entity: T) {
        entityColorMap.remove(entity)
        entityColorCondition.remove(entity)
    }

    fun <T : LivingEntity> setEntityColor(entity: T, color: Color, condition: () -> Boolean) {
        if (color.rgb == 0) return
        entityColorMap[entity] = color
        entityColorCondition[entity] = condition
    }
}
