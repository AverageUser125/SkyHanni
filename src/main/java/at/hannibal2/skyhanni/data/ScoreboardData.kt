package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.RawScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ScoreboardTitleUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.features.inventory.FixIronman
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.getPlayerNames
import at.hannibal2.skyhanni.utils.compat.getSidebarObjective
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.world.scores.criteria.ObjectiveCriteria

@SkyHanniModule
object ScoreboardData {

    /**
     * Scoreboard lines with their original Minecraft component styling.
     *
     * These are the canonical scoreboard lines. Do not convert these to §-formatted
     * strings just to manipulate them.
     */
    var sidebarLinesRaw: List<Component> = emptyList()
        private set

    /**
     * Scoreboard lines after removing Hypixel's artificial separator characters.
     *
     * This is kept separate from [sidebarLines] so consumers can distinguish the
     * actual scoreboard data from the normalized representation.
     */
    var sidebarLines: List<Component> = emptyList()
        private set(value) {
            field = value
            cleanSidebarLines = value.map { it.string.removeColor() }
        }

    var cleanSidebarLines: List<String> = emptyList()
        private set

    val objectiveTitle: Component
        get() =
            MinecraftCompat.localWorldOrNull
                ?.scoreboard
                ?.getSidebarObjective()
                ?.displayName
                ?: Component.empty()

    val cleanObjectiveTitle: String
        get() = objectiveTitle.string.removeColor()

    private var dirty = false

    @HandleEvent(receiveCancelled = true)
    private fun onPacketReceive(event: PacketReceivedEvent) {
        when (val packet = event.packet) {
            is ClientboundSetScorePacket -> {
                if (packet.objectiveName == "update") {
                    dirty = true
                }
            }

            is ClientboundSetPlayerTeamPacket -> {
                if (packet.name.startsWith("team_")) {
                    dirty = true
                }
            }

            is ClientboundSetObjectivePacket -> {
                val type = packet.renderType
                if (type != ObjectiveCriteria.RenderType.INTEGER) return

                val objectiveName = packet.objectiveName
                if (objectiveName == "health") return

                val objectiveValue = packet.displayName.formattedTextCompat()
                ScoreboardTitleUpdateEvent(objectiveValue, objectiveName).post()
            }
        }
    }

    private var monitor = false
    private var lastMonitorState = emptyList<String>()
    private var lastChangeTime = SimpleTimeMark.farPast()

    private fun monitor() {
        if (!monitor) return

        val currentList = fetchScoreboardLines()
            .map { it.string }

        if (lastMonitorState != currentList) {
            val time = lastChangeTime.passedSince()
            lastChangeTime = SimpleTimeMark.now()

            println("Scoreboard Monitor: (new change after ${time.format(showMilliSeconds = true)})")

            for (line in currentList) {
                println("'$line'")
            }
        }

        lastMonitorState = currentList
        println(" ")
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onTick() {
        if (!dirty) return

        dirty = false
        monitor()

        val newLines = fetchScoreboardLines()

        if (newLines != sidebarLinesRaw) {
            sidebarLinesRaw = newLines

            RawScoreboardUpdateEvent(newLines).post()
        }

        val formatted = newLines.map { removeSplitIcons(it) }

        if (formatted != sidebarLinesRaw) {
            val old = sidebarLinesRaw
            sidebarLinesRaw = formatted

            ScoreboardUpdateEvent(formatted, old).post()
        }
    }

    /**
     * Fetch the scoreboard without converting Components into legacy § formatting.
     */
    private fun fetchScoreboardLines(): List<Component> {
        val scoreboard = MinecraftCompat.localWorldOrNull?.scoreboard
            ?: return emptyList()

        val objective = scoreboard.getSidebarObjective()
            ?: return emptyList()

        val scores = scoreboard.listPlayerScores(objective)

        return scores
            .getPlayerNames(scoreboard)
            .reversed()
    }

    /**
     * Removes the artificial characters Hypixel uses to join/split scoreboard
     * team entries while preserving the Component styling.
     *
     * Example:
     *
     *   " §7(§e3,816§7/§c⚽§7▎▎▎"
     *
     * becomes:
     *
     *   " §7(§e3,816§7/§c§7▎▎▎"
     *
     * except the § codes are no longer present at all; their Style is preserved
     * by ComponentSpan.
     */
    private fun removeSplitIcons(component: Component): Component {
        var result = component.intoSpan()

        for (icon in splitIcons) {
            result = result.removeAll(icon)
        }

        return result.intoComponent()
    }

    /**
     * Tries to replace a scoreboard line with a modified one.
     */
    @JvmStatic
    fun tryToReplaceScoreboardLine(text: Component): Component {
        try {
            return tryToReplaceScoreboardLineHarder(text)
        } catch (t: Throwable) {
            ErrorManager.logErrorWithData(
                t,
                "Error while changing the scoreboard text.",
                "text" to text,
            )
            return text
        }
    }

    private fun tryToReplaceScoreboardLineHarder(component: Component): Component {
        if (SkyHanniMod.feature.misc.hidePiggyScoreboard) {
            PurseApi.piggyPattern.matchMatcher(component) {
                val coins = TextHelper.matcher(component, group("coins")) ?: return@matchMatcher
                return Component.literal("Purse: ").append(coins)
            }
        }

        if (SkyHanniMod.feature.misc.colorMonthNames) {
            for (season in Season.entries) {
                if (component.string.trim().startsWith(season.prefix)) {
                    return (component as MutableComponent).withStyle(season.color)
                }
            }
        }

        FixIronman.fixScoreboard(component)?.let {
            return it
        }

        return component
    }

    enum class Season(val prefix: String, val color: ChatFormatting) {
        EARLY_SPRING("Early Spring", ChatFormatting.LIGHT_PURPLE),
        SPRING("Spring", ChatFormatting.LIGHT_PURPLE),
        LATE_SPRING("Late Spring", ChatFormatting.LIGHT_PURPLE),
        EARLY_SUMMER("Early Summer", ChatFormatting.GOLD),
        SUMMER("Summer", ChatFormatting.GOLD),
        LATE_SUMMER("Late Summer", ChatFormatting.GOLD),
        EARLY_AUTUMN("Early Autumn", ChatFormatting.YELLOW),
        AUTUMN("Autumn", ChatFormatting.YELLOW),
        LATE_AUTUMN("Late Autumn", ChatFormatting.YELLOW),
        EARLY_WINTER("Early Winter", ChatFormatting.BLUE),
        WINTER("Winter", ChatFormatting.BLUE),
        LATE_WINTER("Late Winter", ChatFormatting.BLUE)
    }

    // TODO USE SH-REPO
    private val splitIcons = listOf(
        "\uD83C\uDF6B",
        "\uD83D\uDCA3",
        "\uD83D\uDC7D",
        "\uD83D\uDD2E",
        "\uD83D\uDC0D",
        "\uD83D\uDC7E",
        "\uD83C\uDF20",
        "\uD83C\uDF6D",
        "⚽",
        "\uD83C\uDFC0",
        "\uD83D\uDC79",
        "\uD83C\uDF81",
        "\uD83C\uDF89",
        "\uD83C\uDF82",
        "\uD83D\uDD2B",
    )

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdebugscoreboard") {
            description = "Monitors the scoreboard changes: " +
                "Prints the raw scoreboard lines in the console after each update, with time since last update."
            category = CommandCategory.DEVELOPER_DEBUG

            simpleCallback {
                monitor = !monitor
                val action = if (monitor) "Enabled" else "Disabled"
                ChatUtils.chat("$action scoreboard monitoring in the console.")
            }
        }
    }
}
