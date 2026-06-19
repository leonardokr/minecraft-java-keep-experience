package com.ziondev.experiencetweaks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Unique;

public final class EnchantmentConfigHandler {
    private EnchantmentConfigHandler() {}

    @Unique
    @NonNull
    public static Item getConfiguredItem() {
        String configuredItem = Config.ENCHANTMENT_COST_ITEM.get();
        if (!configuredItem.isBlank()) {
            try {
                return BuiltInRegistries.ITEM.getOptional(Identifier.parse(configuredItem)).orElse(Items.LAPIS_LAZULI);
            } catch (Exception exception) {
                ExperienceTweaksMod.LOGGER.warn("Invalid enchantmentCostItem '{}', falling back to minecraft:lapis_lazuli", configuredItem);
            }
        }

        return Items.LAPIS_LAZULI;
    }
}
