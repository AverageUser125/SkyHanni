package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenVisitor
import at.hannibal2.skyhanni.data.jsonobjects.repo.WarpLocationData
import at.hannibal2.skyhanni.data.jsonobjects.repo.WarpsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils

@SkyHanniModule
object VisitorNavigation {
    private data class VisitorNavigationData(
        val island: IslandType,
        val position: LorenzVec,
        val name: String,
    )

    private data class Warp(
        val command: String,
        val island: IslandType,
        val position: LorenzVec,
    )

    private var visitors = mapOf<IslandType, List<VisitorNavigationData>>()
    private var warps: Map<IslandType, List<Warp>> = emptyMap()
    private var noPositionVisitors = setOf<String>()

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val visitors = event.getConstant<GardenJson>("Garden").visitors
        val warps = event.getConstant<WarpsJson>("Warps").warpLocation
        loadVisitors(visitors)
        loadWarps(warps)
    }

    private fun loadVisitors(visitorsJson: Map<String, GardenVisitor>) {
        val otherVisitors = mutableSetOf<String>()
        val visitorsByIsland = visitorsJson.entries
            .groupBy { it.value.mode }
            .mapNotNull { (mode, visitors) ->
                val island = mode?.let(IslandType::getByIdOrNull) ?: run {
                    otherVisitors += visitors.map { it.key }
                    return@mapNotNull null
                }
                island to visitors
            }

        this.visitors = visitorsByIsland.associate { (island, visitors) ->
            val navigationData = visitors.mapNotNull { (name, visitor) ->
                val position = visitor.position ?: run {
                    noPositionVisitors += name
                    return@mapNotNull null
                }
                VisitorNavigationData(
                    island = island,
                    position = position,
                    name = name,
                )
            }

            island to navigationData
        }

        noPositionVisitors = otherVisitors.map { it.lowercase() }.toSet()
    }

    private fun loadWarps(warps: Map<String, WarpLocationData>) {
        this.warps = warps.map { (command, warp) ->
            Warp(
                command = command.lowercase(),
                island = warp.island,
                position = LorenzVec(warp.x, warp.y, warp.z),
            )
        }.groupBy { it.island }
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shvisitornav") {
            description = "Navigates to visitors on the current island"
            category = USERS_ACTIVE

            coroutineSimpleCallback {
                startNavigation(getCurrentIslandVisitors())
            }

            coroutineArgCallback(
                "visitor",
                BrigadierArguments.greedyString(),
                BrigadierUtils.dynamicSuggestionProvider {
                    getCurrentIslandVisitors().map { it.name } + "all"
                },
            ) { name ->
                if (name == "all") {
                    startNavigation(getCurrentIslandVisitors())
                    return@coroutineArgCallback
                }
                val visitor = getCurrentIslandVisitors().firstOrNull { it.name.equals(name, ignoreCase = true)}

                if (visitor == null) {
                    visitorNotFound(name)
                    return@coroutineArgCallback
                }

                startNavigation(visitor)
            }
        }
    }

    private fun visitorNotFound(rawName: String) {
        val visitor = visitors.values
            .flatten()
            .firstOrNull { it.name.equals(rawName, ignoreCase = true) }

        if (visitor == null) {
            if (rawName.lowercase() in noPositionVisitors) {
                ChatUtils.userError("Visitor §a'$rawName' §cposition is not found.")
                WikiManager.sendWikiMessage(rawName, autoOpen = false)
                return
            }

            ChatUtils.userError("Visitor §a'$rawName' §cis not known to the visitor repository")
            return
        }
        val name = visitor.name

        val warp = getNearestWarp(visitor.island, visitor.position)

        if (warp != null) {
            ChatUtils.chat(
                "§7Visitor §a'$name' §7is at §a${visitor.island.displayName}"
            )
            ChatUtils.clickableChat(
                "§7Click §eHERE §7to warp there using §e/${warp.command}§7!",
                onClick = {
                    HypixelCommands.warp(warp.command)
                },
            )
        } else {
            ChatUtils.chat(
                "§7Visitor §a'$name' §7is at §a${visitor.island.displayName}§c"
            )
        }
        WikiManager.sendWikiMessage(name, autoOpen = false)
    }

    private fun startNavigation(visitors: List<VisitorNavigationData>) {
        val graph = IslandGraphs.currentIslandGraph ?: return

        val npcNodes = graph.getNodesWithTags(NPC)

        val nodes = visitors.map { visitor ->
            val position = visitor.position
            npcNodes.filter { it.name == visitor.name }
                .minByOrNull { it.position.distanceSq(position) }
                ?: graph.getNearestNode(position)
        }

        if (nodes.isEmpty()) {
            ChatUtils.userError("Could not find any visitors in the navigation graph")
            return
        }

        NavigateAllApi.navigateAll(
            nodes,
            "Visitor",
            LorenzColor.DARK_PURPLE.toColor(),
            onFinish = {
                ChatUtils.chat(
                    "Reached all ${StringUtils.pluralize(nodes.size, "§aVisitor", withNumber = true)}§e."
                )
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

    private fun getNearestWarp(island: IslandType, position: LorenzVec): Warp? {
        return warps[island]
            ?.minByOrNull { it.position.distanceSq(position) }
    }

    private fun getCurrentIslandVisitors() =
        visitors[SkyBlockUtils.currentIsland].orEmpty()
}
