package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.fishing.trophyfishing.ChatMessagesConfig.DesignFormat
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.fishing.TrophyFishCaughtEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getTooltip
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.ordinal
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TrophyFishMessages {
    private val config get() = SkyHanniMod.feature.fishing.trophyFishing.chatMessages

    /**
     * REGEX-TEST:  TROPHY FISH! You caught a Lavahorse GOLD!
     * REGEX-TEST:  TROPHY FISH! You caught a Soul Fish BRONZE!
     * REGEX-TEST:  TROPHY FISH! You caught a Mana Ray BRONZE!
     * REGEX-TEST:  TROPHY FISH! You caught a Blobfish SILVER!
     * REGEX-TEST:  TROPHY FISH! You caught a Golden Fish SILVER!
     * REGEX-TEST:  TROPHY FISH! You caught a Lavahorse BRONZE x2!
     */
    @Suppress("MaxLineLength")
    val trophyFishPattern by RepoPattern.pattern(
        "fishing.trophy.trophyfish.colorless",
        "${SkyblockStat.TROPHY_FISH_CHANCE.hypixelIcon} TROPHY FISH! You caught an? (?<displayName>[\\w -]+?) (?<displayRarity>[A-Z]+)(?: x(?<amount>\\d+))?!",
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val (displayName, displayRarity, amountCaught) = trophyFishPattern.matchMatcher(event.cleanMessage) {
            val displayName = group("displayName")
            val displayRarity = group("displayRarity")
            val amount = groupOrNull("amount")?.formatIntOrNull() ?: 1
            Triple(displayName, displayRarity, amount)
        } ?: return

        val internalName = TrophyFishApi.getInternalName(displayName)
        val rarity = TrophyRarity.getByName(displayRarity) ?: return

        val trophyFishes = TrophyFishManager.fish ?: return
        val trophyFishCounts = trophyFishes.getOrPut(internalName) { mutableMapOf() }
        val amount = trophyFishCounts.addOrPut(rarity, amountCaught)
        TrophyFishCaughtEvent(internalName, rarity).post()

        if (shouldBlockTrophyFish(rarity, amount)) {
            event.blockedReason = "low_trophy_fish"
            return
        }

        if (config.duplicateHider) event.chatLineId = (internalName + rarity).hashCode()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Modify) {
        val (displayName, displayRarity, amountCaught) = trophyFishPattern.matchMatcher(event.cleanMessage) {
            val displayName = group("displayName")
            val displayRarity = group("displayRarity")
            val amount = groupOrNull("amount")?.formatIntOrNull() ?: 1
            Triple(displayName, displayRarity, amount)
        } ?: return

        val internalName = TrophyFishApi.getInternalName(displayName)
        val rarity = TrophyRarity.getByName(displayRarity) ?: return

        val trophyFishes = TrophyFishManager.fish ?: return
        val trophyFishCounts = trophyFishes.getOrPut(internalName) { mutableMapOf() }
        val amount = trophyFishCounts[rarity] ?: amountCaught

        when (rarity) {
            GOLD -> if (config.goldAlert) {
                sendTitle(displayName, displayRarity, amount)
                if (config.playSound) SoundUtils.playBeepSound()
            }
            DIAMOND -> if (config.diamondAlert) {
                sendTitle(displayName, displayRarity, amount)
                if (config.playSound) SoundUtils.playBeepSound()
            }
            else -> {}
        }

        val edited = if (config.enabled) {
            val designFormat = when (config.design) {
                DesignFormat.STYLE_1 -> if (amount == 1) "§c§lFIRST §r$displayRarity $displayName"
                else "§7$amount${amount.ordinal()} §r$displayRarity $displayName"

                DesignFormat.STYLE_2 -> "§bYou caught a $displayName $displayRarity§b. §7(${amount.addSeparators()})"
                else -> "§bYou caught your ${amount.addSeparators()}${amount.ordinal()} $displayRarity $displayName§b."
            }
            "§6${SkyblockStat.TROPHY_FISH_CHANCE.icon} §6§lTROPHY FISH! $designFormat".asComponent()
        } else event.chatComponent.copy()

        if (config.totalAmount) {
            val total = trophyFishCounts.sumAllValues()
            edited.append((" §7(${total.addSeparators()}${total.ordinal()} total)"))
        }

        if (config.tooltip) {
            getTooltip(internalName)?.let {
                edited.toFlatList(it)
            }
        }

        event.replaceComponent(edited, "TROPHY_FISH")
    }

    private fun sendTitle(displayName: String, displayRarity: String?, amount: Int) {
        val text = "$displayName $displayRarity §8$amount§c!"
        TitleManager.sendTitle(text)
    }

    private fun shouldBlockTrophyFish(rarity: TrophyRarity, amount: Int) =
        config.bronzeHider &&
            rarity == TrophyRarity.BRONZE &&
            amount != 1 ||
            config.silverHider &&
            rarity == TrophyRarity.SILVER &&
            amount != 1

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.trophyCounter", "fishing.trophyFishing.chatMessages.enabled")
        event.move(2, "fishing.trophyDesign", "fishing.trophyFishing.chatMessages.design")
        event.move(2, "fishing.trophyFishTotalAmount", "fishing.trophyFishing.chatMessages.totalAmount")
        event.move(2, "fishing.trophyFishTooltip", "fishing.trophyFishing.chatMessages.tooltip")
        event.move(2, "fishing.trophyFishDuplicateHider", "fishing.trophyFishing.chatMessages.duplicateHider")
        event.move(2, "fishing.trophyFishBronzeHider", "fishing.trophyFishing.chatMessages.bronzeHider")
        event.move(2, "fishing.trophyFishSilverHider", "fishing.trophyFishing.chatMessages.silverHider")
    }
}
