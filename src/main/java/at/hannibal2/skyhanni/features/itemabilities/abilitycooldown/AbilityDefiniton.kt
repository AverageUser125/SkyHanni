package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class AbilityDefinition(
    @Expose @SerializedName("ability_name") val abilityName: String,

    @Expose @SerializedName("item_ids") val itemIds: Set<NeuInternalName>,

    @Expose @SerializedName("base_cooldown") val baseCooldown: Int,
    @Expose @SerializedName("uptime") val uptime: Int,

    @Expose @SerializedName("alternative_position") val alternativePosition: Boolean,

    @Expose @SerializedName("sound_detection") val sound: SoundDetectorDefinition?,
    @Expose @SerializedName("actionbar_detection") val actionbar: Boolean,


    @Transient var ability: ItemAbility? = null
)
