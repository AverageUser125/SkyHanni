package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration

@SkyHanniModule
object DialogUtils {

    private val dialogMutex = Mutex()

    private val popupCoroutine = CoroutineSettings(
        "openPopupWindow",
        timeout = Duration.INFINITE,
        withIOContext = true,
    ).withMutex(dialogMutex)

    /**
     * Opens a modal SDL message box outside the game window.
     *
     * [message] is plain text; only `\n` is supported for line breaks.
     */
    fun openPopupWindow(
        title: String,
        message: String,
        condition: () -> Boolean = { true }) {
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestdialog") {
            description = "Opens a test dialog."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                openPopupWindow("SkyHanni Test Dialog", "Hello World!")
            }
        }
    }
}
