package com.ziondev.experiencetweaks;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Keeps the player's experience level, total experience and progress bar
 * intact after death by copying them from the old player instance to the new one.
 *
 * Players listed in {@code blacklistedPlayers} are excluded and lose XP normally.
 */
@EventBusSubscriber(modid = ExperienceTweaksMod.MODID)
public class KeepExperienceHandler {

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        Player oldPlayer = event.getOriginal();
        if (Config.BLACKLISTED_PLAYERS.get().contains(oldPlayer.getName().getString())) {
            return;
        }

        event.getEntity().experienceLevel = oldPlayer.experienceLevel;
        event.getEntity().totalExperience = oldPlayer.totalExperience;
        event.getEntity().experienceProgress = oldPlayer.experienceProgress;
    }
}
