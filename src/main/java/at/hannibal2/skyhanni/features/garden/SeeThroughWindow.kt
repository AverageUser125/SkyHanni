package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

@SkyHanniModule
object SeeThroughWindow {

    private val config get() = SkyHanniMod.feature.garden.seeThroughWindow

    private var isActive = false

    @HandleEvent
    private fun onConfigLoad(event: ConfigLoadEvent) {
        config.seeThroughFarming.afterChange {
            setOpacity()
        }
    }

    @HandleEvent
    private fun onKeyPressed(event: KeyDownEvent) {
        if (event.keyCode != config.keybind) return
        if (MinecraftCompat.screen != null) return
        isActive = !isActive
        setOpacity()
    }

    @HandleEvent
    private fun onWorldChange() {
        isActive = false
        setOpacity()
    }

    private fun setOpacity() {
        if (!isActive) {
            setWindowOpacity(1f)
            return
        }
        val alpha = (config.seeThroughFarming.get() / 100f).coerceAtLeast(0.05f).coerceAtMost(1f)
        setWindowOpacity(alpha)
    }

    private fun setWindowOpacity(alpha: Float) {
        val handle = Minecraft.getInstance().window.handle()
        GLFW.glfwSetWindowOpacity(handle, alpha)

        MemoryStack.stackPush().use { stack ->
            val descriptionBuffer = stack.mallocPointer(1)
            val error = GLFW.glfwGetError(descriptionBuffer)

            if (error == GLFW.GLFW_NO_ERROR) return
            val descriptionAddress = descriptionBuffer.get(0)
            val errorMessage = if (descriptionAddress != 0L) MemoryUtil.memUTF8(descriptionAddress) else "Unknown error"
            ErrorManager.logErrorStateWithData(
                "Failed to set window opacity: $errorMessage ($error)",
                "Window Opacity Error",
                "alpha" to alpha
            )
        }
    }
}
