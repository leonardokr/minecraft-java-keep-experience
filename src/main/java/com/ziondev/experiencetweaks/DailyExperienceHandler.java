package com.ziondev.experiencetweaks;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Event handler that monitors daily survival streaks.
 * Checks for day transitions during player ticks to reward XP,
 * and resets the survival streak to zero if the player dies.
 */
@EventBusSubscriber(modid = ExperienceTweaksMod.MODID)
public class DailyExperienceHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // Check once per second (every 20 ticks) for efficiency
            if (serverPlayer.tickCount % 20 != 0) {
                return;
            }
            PlayerEnchantData data = ExperienceTweaksMod.getEnchantData();
            if (data != null) {
                data.tickDailyXp(serverPlayer);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            PlayerEnchantData data = ExperienceTweaksMod.getEnchantData();
            if (data != null) {
                data.resetDailyXpSurvival(event.getOriginal().getUUID());
            }
        }
    }
}
