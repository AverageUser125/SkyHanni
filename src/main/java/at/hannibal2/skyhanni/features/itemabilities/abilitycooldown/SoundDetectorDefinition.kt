package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class SoundDetectorDefinition(
    @Expose @SerializedName("sound_name") val soundName: String,
    @Expose @SerializedName("pitch") val pitch: Float? = null,
    @Expose @SerializedName("volume") val volume: Float? = null,
    @Expose @SerializedName("check_recently_held") val checkRecentlyHeld: Boolean = false
)
