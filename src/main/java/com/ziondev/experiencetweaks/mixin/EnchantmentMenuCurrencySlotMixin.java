package com.ziondev.experiencetweaks.mixin;

import com.ziondev.experiencetweaks.EnchantmentConfigHandler;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private static Item experienceTweaks$getCostItem() {
        return EnchantmentConfigHandler.getConfiguredItem();
    }
}
