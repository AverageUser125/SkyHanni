package at.hannibal2.skyhanni.features.event.winter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object NewYearCakeReminder {

    private val config get() = SkyHanniMod.feature.event.winter

    private val patternGroup = RepoPattern.group("event.winter.newyearcake.reminder")

    private val sidebarDetectionPattern by patternGroup.pattern(
        "sidebar.colorless",
        "New Year Event! (?<time>.*)",
    )

    private val claimChatPattern by patternGroup.pattern(
        "chat.claimed",
        "You claimed a New Year Cake!",
    )

    private var lastReminderSend = SimpleTimeMark.farPast()

    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (claimChatPattern.matches(event.cleanMessage)) {
            markCakeClaimed()
        }
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        //  cake already claimed
        if (event.inventoryName == "Baker") {
            markCakeClaimed()
        }
    }

    private fun markCakeClaimed() {
        val playerSpecific = ProfileStorageData.playerSpecific ?: return
        playerSpecific.winter.cakeCollectedYear = SkyBlockTime.now().year
    }

    private fun isClaimed(): Boolean {
        val playerSpecific = ProfileStorageData.playerSpecific ?: return false
        return playerSpecific.winter.cakeCollectedYear == SkyBlockTime.now().year
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.newYearCakeReminder) return
        if (!isCakeTime()) return
        if (ReminderUtils.isBusy()) return
        if (isClaimed()) return

        if (lastReminderSend.passedSince() < 30.seconds) return
        lastReminderSend = SimpleTimeMark.now()
        ChatUtils.clickToActionOrDisable(
            "Reminding you to grab the free New Year Cake. Click here to open the baker menu!",
            config::newYearCakeReminder,
            actionName = "open the baker menu",
            action = { HypixelCommands.openBaker() },
        )
    }

    private fun isCakeTime() = ScoreboardData.cleanSidebarLines.any { sidebarDetectionPattern.matches(it) }
}
