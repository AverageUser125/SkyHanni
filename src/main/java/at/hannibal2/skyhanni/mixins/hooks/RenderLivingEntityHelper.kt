package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.GlobalRender
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.hasVisibleEquipment
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIfKey
import at.hannibal2.skyhanni.utils.compat.deceased
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.util.ARGB
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

@SkyHanniModule
object RenderLivingEntityHelper {

    /**
     * Attached to EntityRenderState to apply SkyHanni custom glow colors.
     */
    @JvmField
    val ENTITY_CUSTOM_GLOW_COLOUR: RenderStateDataKey<Int> =
        RenderStateDataKey.create { "SkyHanni custom glow colour" }

    private val entityColorMap = mutableMapOf<LivingEntity, Color>()
    private val entityColorCondition = ConcurrentHashMap<LivingEntity, () -> Boolean>()

    @JvmStatic
    var isUsingCustomGlow = false
        private set

    @JvmStatic
    fun setUsingCustomGlow() {
        isUsingCustomGlow = true
    }

    private var currentGlowEvent: RenderEntityOutlineEvent? = null

    private fun getEntityGlowEventColor(entity: Entity): Int? =
        currentGlowEvent?.entitiesToOutline?.get(entity)?.rgb?.takeIf { it != 0 }

    @JvmStatic
    fun postNoXrayOutlineEvent() {
        isUsingCustomGlow = entityColorCondition.values.any { it() } ||
            currentGlowEvent?.entitiesToOutline.orEmpty().isNotEmpty()

        val event = RenderEntityOutlineEvent(NO_XRAY)
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

    /**
     * Returns the entity glow color formatted for EntityRenderState.
     */
    @JvmStatic
    fun getEntityGlowColorARGB(entity: Entity): Int =
        getEntityGlowColor(entity)
            ?.let { ARGB.opaque(it) }
            ?: EntityRenderState.NO_OUTLINE

    private fun getLivingEntityGlowColor(entity: LivingEntity): Int? =
        internalSetColorMultiplier(entity, 0).takeIf { it != 0 }

    @HandleEvent
    fun onWorldChange() {
        entityColorMap.clear()
        entityColorCondition.clear()
    }

    @HandleEvent(SkyHanniTickEvent::class)
    fun onTick() {
        entityColorMap.removeIfKey { it.deceased }
        entityColorCondition.removeIfKey { it.deceased }
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

    @JvmStatic
    fun <T : LivingEntity> internalSetColorMultiplier(entity: T, default: Int): Int {
        if (GlobalRender.renderDisabled) return default
        if (entityColorMap.containsKey(entity)) {
            val condition = entityColorCondition[entity] ?: return default
            if (condition.invoke()) {
                return entityColorMap[entity]?.rgb ?: return default
            }
        }
        return default
    }
}
