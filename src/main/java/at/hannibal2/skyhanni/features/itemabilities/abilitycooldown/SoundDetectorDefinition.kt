package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

data class SoundDetectorDefinition(
    val soundName: String,
    val pitch: Float? = null,
    val volume: Float? = null
) : AbilityDetectorDefinition
