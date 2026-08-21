package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.compat.RenderCompat
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.renderpearl.api.textures.FilterMode
import com.mojang.renderpearl.api.textures.GpuTexture
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.Projection
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.BlitRenderState
import net.minecraft.client.renderer.state.gui.GuiRenderState
import kotlin.math.roundToInt

internal class SkyHanniRealtimeItemSlot(val slotSize: Int) : SkyHanniAbstractItemTexture() {

    init { allocate(slotSize) }

    private fun allocate(size: Int) {
        allocateTextures(
            size, "SkyHanni realtime item", "SkyHanni realtime item depth",
            GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_COPY_DST,
        )
    }

    fun render(
        context: SkyHanniItemRenderContext,
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
        projectionBuffer: ProjectionMatrixBuffer,
    ) {
    }

    private fun submitBlit(
        state: SkyHanniGuiItemRenderState,
        guiRenderState: GuiRenderState,
    ) {
        val textureView = textureView ?: return
        // u/v: full slot occupies [0,1] x [0,1] in the per-item texture
        guiRenderState.addBlitToCurrentLayer(
            BlitRenderState(
                RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(textureView, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
                state.pose(),
                state.x0(), state.y0(), state.x1(), state.y1(),
                0f,
                1f,
                1f,
                0f,
                ((state.alpha * 255).roundToInt() shl 24) or 0x00FFFFFF,
                state.scissorArea(),
            )
        )
    }
}
