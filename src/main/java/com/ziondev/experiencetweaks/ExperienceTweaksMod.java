package com.ziondev.experiencetweaks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(ExperienceTweaksMod.MODID)
public class ExperienceTweaksMod {
    public static final String MODID = "experiencetweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExperienceTweaksMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("Metalion's Experience Tweaks Mod initialized...");
    }
}
