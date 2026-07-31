package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection {

    @WrapOperation(
        method = "submitModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelSubmit;)V"
        )
    )
    private void skyhanni$markCustomGlowModel(
        ModelFeatureRenderer.Storage storage,
        RenderType renderType,
        SubmitNodeStorage.ModelSubmit<?> submit,
        Operation<Void> original
    ) {
        skyhanni$applyGlow(submit);
        original.call(storage, renderType, submit);
    }

    @WrapOperation(
        method = "submitModelPart",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ModelPartFeatureRenderer$Storage;add(Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/SubmitNodeStorage$ModelPartSubmit;)V"
        )
    )
    private void skyhanni$markCustomGlowModelPart(
        ModelPartFeatureRenderer.Storage storage,
        RenderType renderType,
        SubmitNodeStorage.ModelPartSubmit submit,
        Operation<Void> original
    ) {
        skyhanni$applyGlow(submit);
        original.call(storage, renderType, submit);
    }

    @WrapOperation(
        method = "submitItem",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
        )
    )
    private boolean skyhanni$markCustomGlowItem(
        List<Object> list,
        Object command,
        Operation<Boolean> original
    ) {
        skyhanni$applyGlow(command);
        return original.call(list, command);
    }

    private void skyhanni$applyGlow(Object submit) {
        if (!(submit instanceof GlowingStateStore glowingState)) {
            return;
        }

        EntityRenderState state = EntityRenderDispatcherHookKt.getEntityRenderState();

        if (state == null) {
            return;
        }

        int glowColor = state.getDataOrDefault(
            RenderLivingEntityHelper.ENTITY_CUSTOM_GLOW_COLOUR,
            EntityRenderState.NO_OUTLINE
        );

        if (glowColor != EntityRenderState.NO_OUTLINE) {
            glowingState.skyhanni$setCustomGlowColour(glowColor);
        }
    }

}
