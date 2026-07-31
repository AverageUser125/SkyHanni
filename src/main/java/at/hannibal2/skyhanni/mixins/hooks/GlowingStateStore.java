package at.hannibal2.skyhanni.mixins.hooks;

public interface GlowingStateStore {

    default boolean skyhanni$isUsingCustomOutline() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    default void skyhanni$setUsingCustomOutline() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    default void skyhanni$setCustomGlowColour(int color) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    default int skyhanni$getCustomGlowColour() {
        throw new UnsupportedOperationException("Implemented via mixin");
    }
}
