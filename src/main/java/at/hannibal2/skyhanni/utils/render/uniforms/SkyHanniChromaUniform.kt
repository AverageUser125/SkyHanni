package at.hannibal2.skyhanni.utils.render.uniforms

import com.mojang.renderpearl.api.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import net.minecraft.client.renderer.DynamicGpuDataStorage
import java.nio.ByteBuffer

class SkyHanniChromaUniform : AutoCloseable {
    private val uniformSize = Std140SizeCalculator().putFloat().putFloat().putFloat().putInt().get()


    // Imperative to clear DynamicUniformStorage every frame.
    // Handled in MixinRenderSystem.
    fun clear() {
    }

    override fun close() {
    }
}
