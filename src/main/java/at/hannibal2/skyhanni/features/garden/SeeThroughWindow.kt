package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

@SkyHanniModule
object SeeThroughWindow {

    private val config get() = SkyHanniMod.feature.garden.seeThroughWindow

    private var isActive = false
    private var opacityChanged = false
    private val isWayland by lazy {
        GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_WAYLAND
    }

    @HandleEvent
    private fun onConfigLoad() {
        config.seeThroughFarming.afterChange {
            setOpacity()
        }
    }

    @HandleEvent
    private fun onKeyDown() {
        if (!config.keybind.isKeyClicked()) return
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
            if (opacityChanged) {
                resetWindowOpacity()
            }
            return
        }
        val alpha = (config.seeThroughFarming.get() / 100f).coerceAtLeast(0.05f).coerceAtMost(1f)
        if (alpha != 1f) {
            setWindowOpacity(alpha)
        } else if (opacityChanged) {
            resetWindowOpacity()
        }
    }

    private fun setWindowOpacity(alpha: Float) {
        if (isWayland) {
            ErrorManager.skyHanniError(
                "Your platform doesn't support see through window",
            )
        }
        opacityChanged = alpha != 1f

        val handle = Minecraft.getInstance().window.handle()
        GLFW.glfwSetWindowOpacity(handle, alpha)
    }

    private fun resetWindowOpacity() {
        setWindowOpacity(1f)
    }
}
