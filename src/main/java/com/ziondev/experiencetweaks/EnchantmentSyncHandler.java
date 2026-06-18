package com.ziondev.experiencetweaks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

/**
 * Listens for enchantment table menu open events on the server side and
 * sends the player's current required levels to their client.
 */
@EventBusSubscriber(modid = ExperienceTweaksMod.MODID)
public class EnchantmentSyncHandler {

    @SubscribeEvent
    public static void onMenuOpen(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof EnchantmentMenu
                && event.getEntity() instanceof ServerPlayer serverPlayer) {
            ExperienceTweaksMod.syncEnchantLevels(serverPlayer);
        }
    }
}
