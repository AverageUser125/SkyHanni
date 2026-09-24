package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SpidersDenApi {

    private val patternGroup = RepoPattern.group("combat.spidersden")

    /**
     * REGEX-TEST: Broodmother: Soon
     */
    val broodmotherPattern by patternGroup.pattern(
        "broodmother.colorless",
        "Broodmother: (?:Slain|Dormant|Soon|Awakening|Imminent|Alive!)",
    )

    fun inSpidersDen() = IslandType.SPIDER_DEN.isInIsland()

    fun isAtTopOfNest() = inSpidersDen() && broodmotherPattern.anyMatches(ScoreboardData.cleanSidebarLines)
}
