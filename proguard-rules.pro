-dontoptimize
-dontobfuscate
-dontnote **
-ignorewarnings

-keepattributes *
-keep class at.hannibal2.skyhanni.SkyHanniModLoader { *; }
-keep class at.hannibal2.skyhanni.mixins.init.SkyHanniMixinPlugin { *; }
-keep class at.hannibal2.skyhanni.compat.** { *; }
-keep class at.hannibal2.skyhanni.mixins.** { *; }
-keep class at.hannibal2.skyhanni.** {
    *;
}
