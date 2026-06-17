package com.ziondev.experiencetweaks;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;

@EventBusSubscriber(modid = ExperienceTweaksMod.MODID)
public class ExperienceHandler {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player oldPlayer = event.getOriginal();
            String playerName = oldPlayer.getName().getString();

            if (Config.BLACKLISTED_PLAYERS.get().contains(playerName)) {
                return;
            }

            event.getEntity().experienceLevel = oldPlayer.experienceLevel;
            event.getEntity().totalExperience = oldPlayer.totalExperience;
            event.getEntity().experienceProgress = oldPlayer.experienceProgress;
        }
    }

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof Player player) {
            String playerName = player.getName().getString();

            if (Config.BLACKLISTED_PLAYERS.get().contains(playerName)) {
                return;
            }

            event.setCanceled(true);
        } else if (Config.DIRECT_EXPERIENCE.get()) {
            Player attackingPlayer = event.getAttackingPlayer();
            if (attackingPlayer != null) {
                attackingPlayer.giveExperiencePoints(event.getDroppedExperience());
                event.setCanceled(true);
            }
        }
    }
}
