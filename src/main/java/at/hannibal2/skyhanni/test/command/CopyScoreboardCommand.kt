package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import net.minecraft.network.chat.Component

@SkyHanniModule
object CopyScoreboardCommand {

    private fun command(args: Array<String>) {
        val resultList = mutableListOf<String>()
        val noColor = args.contains("-nocolor")
        val raw = args.contains("-raw")
        val complex = args.contains("-complex")

        if (complex && noColor) {
            ChatUtils.userError("Cannot use -complex and -nocolor together.")
            return
        }

        val transformer = componentTransformer(complex, noColor)
        resultList.add("Title:")
        resultList.add(transformer(ScoreboardData.objectiveTitle))
        resultList.add("")

        val rawComponents = if (raw) {
            ScoreboardData.sidebarLinesRaw
        } else {
            ScoreboardData.sidebarLines
        }

        resultList.add("Lines:")
        val lines = rawComponents.map(transformer)
        resultList.addAll(lines)

        OSUtils.copyToClipboard(resultList.joinToString("\n"))
        ChatUtils.chat("Scoreboard copied into your clipboard!")
    }

    private fun componentTransformer(
        complex: Boolean,
        noColor: Boolean,
    ): (Component) -> String = when {
        complex -> { component ->
            ConfigManager.gson.toJson(component)
        }
        noColor -> { component ->
            component.string.removeColor()
        }
        else -> { component ->
            component.formattedTextCompat()
        }
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("copyboard") {
            description = "Copy the scoreboard to your clipboard"
            category = CommandCategory.DEVELOPER_TEST
            legacyCallbackArgs { args ->
                command(args)
            }
        }
    }
}
