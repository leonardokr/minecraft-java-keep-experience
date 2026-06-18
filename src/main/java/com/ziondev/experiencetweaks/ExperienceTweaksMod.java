package com.ziondev.experiencetweaks;

import com.ziondev.experiencetweaks.network.ClientEnchantLevelCache;
import com.ziondev.experiencetweaks.network.SyncEnchantLevelsPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

@Mod(ExperienceTweaksMod.MODID)
public class ExperienceTweaksMod {
    public static final String MODID = "experiencetweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ExperienceTweaksMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::onRegisterPayloads);
        LOGGER.info("Metalion's Experience Tweaks Mod initialized...");
    }

    private void onConfigReload(net.neoforged.fml.event.config.ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            LOGGER.info("Experience Tweaks config reloaded!");
        }
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID);
        registrar.playToClient(
                SyncEnchantLevelsPacket.TYPE,
                SyncEnchantLevelsPacket.STREAM_CODEC,
                (packet, ctx) -> ClientEnchantLevelCache.update(packet.requiredLevels())
        );
    }

    /**
     * Returns the persistent {@link PlayerEnchantData} from the overworld saved data store,
     * or {@code null} if the server is not running (e.g. on the client side).
     */
    public static PlayerEnchantData getEnchantData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        return server.overworld().getDataStorage().computeIfAbsent(PlayerEnchantData.TYPE);
    }

    /**
     * Sends the player's current required levels to their client.
     * Should be called whenever the enchantment menu opens or after a successful enchant.
     */
    public static void syncEnchantLevels(ServerPlayer player) {
        PlayerEnchantData data = getEnchantData();
        if (data == null) {
            return;
        }

        int currentLevel = player.experienceLevel;
        List<Integer> levels = new ArrayList<>();
        for (int b = 0; b < 3; b++) {
            levels.add(data.getRequiredLevel(player.getUUID(), b, currentLevel));
        }

        PacketDistributor.sendToPlayer(player, new SyncEnchantLevelsPacket(levels));
    }
}
