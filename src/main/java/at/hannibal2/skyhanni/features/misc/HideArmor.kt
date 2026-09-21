package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.misc.HideArmorConfig
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.PlayerMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.EffectsCompat
import at.hannibal2.skyhanni.utils.compat.EffectsCompat.Companion.hasPotionEffect
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.world.entity.player.Player

@SkyHanniModule
object HideArmor {

    internal val config: HideArmorConfig get() = SkyHanniMod.feature.misc.hideArmor

    private var playerFilter = PlayerMatcher.builder { }

    fun shouldHideArmor(entity: Player): Boolean {
        if (!SkyBlockUtils.inSkyBlock) return false
        if (entity.hasPotionEffect(EffectsCompat.INVISIBILITY)) return false
        if (entity.isNpc()) return false

        val name = entity.gameProfile.name
        val matches = playerFilter.matches(name)
        return if (config.invertSelection) !matches else matches
    }

    @HandleEvent
    private fun onConfigLoad() {
        config.playerSelection.afterChange(init = true) {
            playerFilter = PlayerMatcher.builder {
                include(config.playerSelection.get())
            }
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(91, "misc.hideArmor2", "misc.hideArmor")
        event.move(
            147,
            "misc.hideArmor.mode",
            "misc.hideArmor.playerSelection"
        ) { element ->
            val oldValue = element.asString
            val newValue: List<PlayerNameSource> = when (oldValue) {
                "ALL" -> listOf(SELF, ISLAND_PLAYERS)
                "OWN" -> listOf(SELF)
                "OTHERS" -> listOf(ISLAND_PLAYERS)
                "OFF" -> emptyList()
                else -> return@move element
            }

            JsonArray().apply {
                newValue.forEach { add(JsonPrimitive(it.name)) }
            }
        }
    }
}
