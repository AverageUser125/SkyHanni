package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils

@SkyHanniModule
object VisitorNavigation {
    data class VisitorNavigationData(
        val position: LorenzVec,
        val name: String,
    )

    private var visitorJson = mapOf<IslandType, List<VisitorNavigationData>>()

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val visitors = event.getConstant<GardenJson>("Garden").visitors

        val visitorsByIsland = visitors.entries
            .groupBy { it.value.mode }
            .mapNotNull { (mode, visitors) ->
                // TODO: Fix repo missing island type for visitors
                @Suppress("UNNECESSARY_SAFE_CALL")
                val island = mode?.let(IslandType::getByIdOrNull) ?: return@mapNotNull null
                island to visitors
            }

        visitorJson = visitorsByIsland.associate { (island, visitors) ->
            val navigationData = visitors.mapNotNull { (name, visitor) ->
                visitor.position?.let { position ->
                    VisitorNavigationData(
                        position = position,
                        name = name,
                    )
                }
            }

            island to navigationData
        }
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shvisitornav") {
            description = "Navigates to visitors on the current island"
            category = USERS_ACTIVE

            simpleCallback {
                startNavigation(getCurrentIslandVisitors())
            }

            argCallback(
                "visitor",
                BrigadierArguments.greedyString(),
                BrigadierUtils.dynamicSuggestionProvider {
                    getCurrentIslandVisitors().map { it.name } + "all"
                },
            ) { name ->
                if (name == "all") {
                    startNavigation(getCurrentIslandVisitors())
                    return@argCallback
                }
                val visitor = getCurrentIslandVisitors().firstOrNull { it.name == name }

                if (visitor == null) {
                    ChatUtils.userError("Visitor '$name' not found on this island")
                    return@argCallback
                }

                startNavigation(visitor)
            }
        }
    }

    private fun startNavigation(visitors: List<VisitorNavigationData>) {
        val graph = IslandGraphs.currentIslandGraph ?: return

        val nodes = visitors.map { visitor ->
            graph.getNearestNode(visitor.position)
        }

        if (nodes.isEmpty()) {
            ChatUtils.userError("Could not find any visitors in the navigation graph")
            return
        }

        NavigateAllApi.navigateAll(
            nodes,
            "Visitors",
            LorenzColor.DARK_PURPLE.toColor(),
            onFinish = {
                ChatUtils.chat("Reached all ${StringUtils.pluralize(nodes.size, "§dvisitor", withNumber = true)}§e.")
            },
            continueNavigationCondition = None,
            condition = { true },
        )
    }

    private fun startNavigation(visitor: VisitorNavigationData) {
        IslandGraphs.pathFind(
            visitor.position,
            visitor.name,
            color = LorenzColor.DARK_PURPLE.toColor(),
            condition = { true },
        )
    }

    private fun getCurrentIslandVisitors() =
        visitorJson[SkyBlockUtils.currentIsland].orEmpty()
}
