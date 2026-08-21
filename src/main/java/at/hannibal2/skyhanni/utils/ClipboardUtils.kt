package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.async
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import com.mojang.blaze3d.platform.ClipboardManager
import net.minecraft.client.Minecraft
import kotlinx.coroutines.Deferred

object ClipboardUtils {

    private val clipboardCoroutineSettings = CoroutineSettings(
        "clipboardAccess",
        withIOContext = true,
    )

    @Deprecated("Use copyToClipboardAsync instead", ReplaceWith("copyToClipboardAsync(text).await()"))
    fun copyToClipboard(text: String, step: Int = 0) = copyToClipboardInternal(text, step)

    fun copyToClipboardAsync(text: String, step: Int = 0): Deferred<Boolean?> = clipboardCoroutineSettings.async {
        copyToClipboardInternal(text, step)
    }

    private fun copyToClipboardInternal(text: String, step: Int = 0): Boolean = runCatching {
        ClipboardManager().clipboard = text
        true
    }.getOrElse {
        if (step == 3) {
            ErrorManager.logErrorWithData(it, "Error while trying to access the clipboard.")
            false
        } else copyToClipboardInternal(text, step + 1)
    }

    // TODO: 26.3 better error handling
    fun readFromClipboard(step: Int = 0): String? {
        return ClipboardManager().clipboard
    }
}
