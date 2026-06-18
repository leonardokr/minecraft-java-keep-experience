package com.ziondev.experiencetweaks.mixin;

import com.ziondev.experiencetweaks.Config;
import com.ziondev.experiencetweaks.ExperienceTweaksMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.EnchantmentMenu$3")
public abstract class EnchantmentMenuCurrencySlotMixin {
    @Inject(
            method = "mayPlace",
            at = @At("HEAD"),
            cancellable = true
    )
    private void experienceTweaks$allowConfiguredCurrency(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(itemStack.is(experienceTweaks$getCostItem()));
    }

    @Inject(
            method = "getNoItemIcon",
            at = @At("HEAD"),
            cancellable = true
    )
    private void experienceTweaks$hideLapisSlotIcon(CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(null);
    }

    private static Item experienceTweaks$getCostItem() {
        String configuredItem = Config.ENCHANTMENT_COST_ITEM.get();
        if (configuredItem != null && !configuredItem.isBlank()) {
            try {
                return BuiltInRegistries.ITEM.getOptional(Identifier.parse(configuredItem)).orElse(Items.LAPIS_LAZULI);
            } catch (Exception exception) {
                ExperienceTweaksMod.LOGGER.warn("Invalid enchantmentCostItem '{}', falling back to minecraft:lapis_lazuli", configuredItem);
            }
        }

        return Items.LAPIS_LAZULI;
    }
}
