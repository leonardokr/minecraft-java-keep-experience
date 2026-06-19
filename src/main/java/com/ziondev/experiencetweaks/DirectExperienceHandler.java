package com.ziondev.experiencetweaks;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

/**
 * When {@code directExperience} is enabled, suppresses all XP orb drops and
 * delivers the experience directly:
 * <ul>
 *   <li>Player death drops are canceled (no orbs on the ground).</li>
 *   <li>Mob XP goes straight to the attacking player instead of spawning orbs.</li>
 * </ul>
 *
 * When {@code directExperience} is disabled, this handler does nothing, and
 * vanilla behavior is preserved — orbs spawn normally for both mobs and players.
 */
@EventBusSubscriber(modid = ExperienceTweaksMod.MODID)
public class DirectExperienceHandler {

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!Config.DIRECT_EXPERIENCE.get()) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            event.setCanceled(true);
        } else {
            Player attacker = event.getAttackingPlayer();
            if (attacker != null) {
                attacker.giveExperiencePoints(event.getDroppedExperience());
                event.setCanceled(true);
            }
        }
    }
}
