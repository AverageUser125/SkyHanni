package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents

object ItemStackHook {

    @JvmStatic
    fun shouldBlockVanillaEnchants(componentType: DataComponentType<*>): Boolean {
        return (SkyBlockUtils.inSkyBlock
            && SkyHanniMod.feature.inventory.enchantParsing.hideVanillaEnchants.get()
            && componentType === DataComponents.ENCHANTMENTS)
    }
}
