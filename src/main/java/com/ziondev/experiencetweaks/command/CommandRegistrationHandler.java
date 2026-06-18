package com.ziondev.experiencetweaks.command;

import com.ziondev.experiencetweaks.ExperienceTweaksMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Registers all mod commands when the server starts.
 */
@EventBusSubscriber(modid = ExperienceTweaksMod.MODID)
public class CommandRegistrationHandler {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EnchantCooldownCommand.register(event.getDispatcher());
    }
}
