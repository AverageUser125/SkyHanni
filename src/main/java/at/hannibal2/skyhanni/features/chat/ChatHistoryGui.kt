package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.compat.convertToJsonString
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

class ChatHistoryGui(
    private val history: List<ChatManager.MessageFilteringResult>,
) : SkyHanniBaseScreen() {

    private val w = 500
    private val h = 300
    private val padding = 5
    private val spacing = 5

    private val scrollValue = ScrollValue()

    private val historyRenderable by lazy {
        Renderable.scrollList(
            list = history.map(::createHistoryEntry),
            height = h - padding * 2,
            scrollValue = scrollValue,
            velocity = 2.0,
            showScrollableTipsInList = false,
            showScrollbar = true,
        )
    }

    private fun ChatManager.MessageFilteringResult.getReason(): String? =
        actionReason ?: modifiedReason

    private fun createHistoryEntry(
        result: ChatManager.MessageFilteringResult,
    ): Renderable {
        val message = Renderable.vertical(
            buildList {
                add(
                    Renderable.horizontal(
                        listOfNotNull(
                            Renderable.text(result.actionKind.renderedString),
                            result.getReason()?.let(Renderable::text),
                        ),
                        spacing = spacing,
                    )
                )

                add(
                    Renderable.text(result.message)
                )

                val reason = result.modified
                if (reason != null) {
                    add(Renderable.text("§e§lNEW TEXT"))
                    add(Renderable.text(reason))
                }
            },
        )

        return Renderable.clickable(
            Renderable.hoverTips(
                content = message,
                tips = result.hoverInfo,
            ),
            onLeftClick = {
                copyMessage(result)
            },
            onHover = {
                if (
                    KeyboardManager.isShiftKeyDown() &&
                    result.hoverExtraInfo.isNotEmpty()
                ) {
                    RenderableTooltips.setTooltipForRender(
                        result.hoverExtraInfo.map(StringRenderable::from),
                    )
                }
            },
        )
    }

    private fun copyMessage(
        result: ChatManager.MessageFilteringResult,
    ) {
        if (KeyboardManager.isShiftKeyDown()) {
            OSUtils.copyToClipboard(
                result.message.convertToJsonString(),
            )
            ChatUtils.chat(
                "Copied structured chat line to clipboard",
                false,
            )
        } else {
            val message = result.message
                .formattedTextCompat()
                .stripHypixelMessage()

            OSUtils.copyToClipboard(message)
            ChatUtils.chat("Copied chat line to clipboard")
        }
    }

    override fun onInitGui() {
        // Renderable.scrollList owns the scroll position.
    }

    override fun onDrawScreen(
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
    ) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)

        val left = (width - w) / 2
        val top = (height - h) / 2

        GuiRenderUtils.drawFloatingRectDark(
            left,
            top,
            w,
            h,
        )

        Renderable.withMousePosition(
            mouseX - left - padding,
            mouseY - top - padding,
        ) {
            historyRenderable.render(
                mouseX - left - padding,
                mouseY - top - padding,
            )
        }
    }
}
