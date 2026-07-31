package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.client.renderer.entity.state.EntityRenderState

interface EntityRenderMarker {
    fun `skyhanni$getEntityStateBeingRendered`(): EntityRenderState? {
        throw UnsupportedOperationException("Implemented via mixin")
    }
}
