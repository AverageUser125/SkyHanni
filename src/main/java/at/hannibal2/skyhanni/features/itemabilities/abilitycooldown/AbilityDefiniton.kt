package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.utils.NeuInternalName
import kotlin.time.Duration

data class AbilityDefinition(
    val abilityName: String,

    val itemIds: Set<NeuInternalName>,

    val baseCooldown: Duration,
    val castTime: Duration,
    val uptime: Duration,

    val resourceCosts: List<AbilityCost>,

    val detectors: List<AbilityDetectorDefinition>,
)
