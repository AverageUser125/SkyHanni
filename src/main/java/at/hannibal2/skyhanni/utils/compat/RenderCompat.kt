package at.hannibal2.skyhanni.utils.compat

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.RenderPipelines
import java.util.OptionalDouble

import java.util.Optional

object RenderCompat {

    /**
     * The depth of an empty render target. 26.2 renders with a reversed depth range,
     * so "nothing drawn" is 0 rather than 1.
     */
    const val CLEAR_DEPTH = 0.0

    fun getMinecraftGuiTextured(): RenderPipeline = RenderPipelines.GUI_TEXTURED

    fun RenderPass.enableRenderPassScissorStateIfAble() {
        val scissorState = RenderSystem.getScissorStateForRenderTypeDraws()
        if (scissorState.enabled()) {
            this.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height())
        }
    }

    fun RenderPass.drawIndexed(indices: Int) {
        drawIndexed(
            0,
            0,
            indices,
            1,
            0,
        )
    }

    private fun RenderTarget.findColorAttachment() = this.colorTextureView

    private fun RenderTarget.findDepthAttachment() = if (this.useDepth) this.depthTextureView else null

    fun GpuDevice.createRenderPass(name: String, framebuffer: RenderTarget): RenderPass {
        val colorAttachment = framebuffer.findColorAttachment() ?: error("color attachment is null")
        return this.createCommandEncoder().createRenderPass(
            { name },
            colorAttachment,
            Optional.empty(),
            framebuffer.findDepthAttachment(),
            OptionalDouble.empty(),
        )
    }

}
