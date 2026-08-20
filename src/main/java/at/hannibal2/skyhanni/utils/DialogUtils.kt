package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import org.lwjgl.sdl.SDL_GetError
import org.lwjgl.sdl.SDL_INIT_VIDEO
import org.lwjgl.sdl.SDL_InitSubSystem
import org.lwjgl.sdl.SDL_MESSAGEBOX_INFORMATION
import org.lwjgl.sdl.SDL_ShowSimpleMessageBox
import org.lwjgl.sdl.SDL_WasInit
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
        condition: () -> Boolean = { true },
    ): Job = popupCoroutine.launch {
        runCatching {
            if (!condition()) return@runCatching

            if (!ensureVideoInitialized()) {
                ErrorManager.logErrorStateWithData(
                    "Failed to open a popup window",
                    "Failed to initialize SDL video subsystem",
                    "error" to SDL_GetError(),
                    "title" to title,
                    "message" to message,
                )
                return@runCatching
            }

            val result = SDL_ShowSimpleMessageBox(
                SDL_MESSAGEBOX_INFORMATION,
                title,
                message,
                0L,
            )

            if (!result) {
                ErrorManager.logErrorStateWithData(
                    "Failed to open a popup window",
                    "SDL_ShowSimpleMessageBox failed",
                    "error" to SDL_GetError(),
                    "title" to title,
                    "message" to message,
                )
            }
        }.onFailure { e ->
            ErrorManager.logErrorWithData(
                e,
                "Failed to open a popup window",
                "title" to title,
                "message" to message,
            )
        }
    }

    private fun ensureVideoInitialized(): Boolean {
        if (SDL_WasInit(SDL_INIT_VIDEO) != 0) return true
        return SDL_InitSubSystem(SDL_INIT_VIDEO)
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
