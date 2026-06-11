package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class InvincibilityTimerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show time till mobs is no longer invincible")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled = false

    @Expose
    @ConfigOption(name = "Mob Types", desc = "Which mob types to apply this to")
    @ConfigEditorDraggableList
    var mobTypes: List<MobType> = emptyList()

    enum class MobType(val displayName: String) {
        SEA_CREATURE("Sea Creature"),
        VANQUISHER("Vanquiser");

        override fun toString(): String {
            return displayName
        }
    }
}
