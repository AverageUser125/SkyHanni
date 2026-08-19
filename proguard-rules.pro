-dontwarn com.sun.management.**
-dontwarn lombok.NonNull
-dontwarn lombok.**
-dontwarn com.terraformersmc.modmenu.api.**
-dontwarn org.apache.avalon.framework.**
-dontwarn javax.servlet.**
-dontwarn at.hannibal2.skyhanni.skyhannimodule.**
-dontwarn com.google.auto.service.**
-dontwarn org.apache.log4j.**
-dontwarn net.minecraft.client.renderer.block.FluidModel
-dontwarn net.minecraft.client.renderer.entity.state.EntityRenderState
-dontwarn net.minecraft.client.renderer.feature.ItemFeatureRenderer$Submit
-dontwarn net.minecraft.client.renderer.feature.ModelFeatureRenderer$Submit
-dontwarn net.minecraft.network.chat.MutableComponent
-dontwarn org.apache.commons.codec.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.io.**
-dontwarn java.lang.invoke.MethodHandle
-dontwarn java.lang.invoke.MethodHandles
-dontwarn org.apache.log.**

-dontobfuscate
-dontoptimize

-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations,RuntimeInvisibleTypeAnnotations
-keepattributes AnnotationDefault
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

-keep class at.hannibal2.skyhanni.SkyHanniModLoader {
    *;
}

-keep class at.hannibal2.skyhanni.mixins.init.SkyHanniMixinPlugin {
    *;
}

-keep class at.hannibal2.skyhanni.mixins.** {
    *;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep enum * {
    *;
}

-keep class at.hannibal2.skyhanni.compat.* {
    *;
}
