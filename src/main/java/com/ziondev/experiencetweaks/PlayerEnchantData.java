package com.ziondev.experiencetweaks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side persistent data tracking the minimum experience level each player
 * must reach before they can use each enchantment table button again.
 * <p></p>
 * Serialized via Codec and stored in world saves under the id defined by {@link #TYPE}.
 */
public class PlayerEnchantData extends SavedData {

    /** Codec for a single player entry: { "uuid": [...], "levels": [n, n, n] } */
    private static final Codec<Map.Entry<UUID, int[]>> ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(Map.Entry::getKey),
            Codec.INT.listOf().fieldOf("levels").forGetter(e -> {
                int[] arr = e.getValue();
                List<Integer> list = new ArrayList<>(arr.length);
                for (int v : arr) list.add(v);
                return list;
            })
    ).apply(inst, (uuid, list) -> {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return Map.entry(uuid, arr);
    }));

    /** Full data codec: list of player entries. */
    private static final Codec<PlayerEnchantData> CODEC = ENTRY_CODEC.listOf()
            .xmap(
                    entries -> {
                        PlayerEnchantData data = new PlayerEnchantData();
                        for (Map.Entry<UUID, int[]> e : entries) {
                            data.playerRequiredLevels.put(e.getKey(), e.getValue());
                        }
                        return data;
                    },
                    data -> new ArrayList<>(data.playerRequiredLevels.entrySet())
            );

    public static final SavedDataType<PlayerEnchantData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ExperienceTweaksMod.MODID, "enchant_data"),
            PlayerEnchantData::new,
            CODEC
    );

    private static final int BUTTON_COUNT = 3;

    /**
     * Maps player UUID → int[3] where index 0/1/2 is the minimum level
     * required for enchantment buttons 1/2/3 respectively.
     */
    private final Map<UUID, int[]> playerRequiredLevels = new HashMap<>();

    /** No-arg constructor used by the SavedDataType factory. */
    public PlayerEnchantData() {}

    /**
     * Returns the minimum player level required to use enchantment button {@code buttonId}
     * for the given player.
     *
     * <p>On first use (no saved entry), the required level is the higher of:
     * <ul>
     *   <li>the base level from config (e.g., 10 / 15 / 20), scaled by buttonLevel/3</li>
     *   <li>the player's current level scaled by buttonLevel/3 (so button 3 = currentLevel,
     *       button 2 = ~66%, button 1 = ~33%)</li>
     * </ul>
     *
     * @param playerId     player UUID
     * @param buttonId     0-based button index (0, 1, 2)
     * @param currentLevel player's current experience level (used for first-use baseline)
     */
    public int getRequiredLevel(UUID playerId, int buttonId, int currentLevel) {
        int[] levels = playerRequiredLevels.get(playerId);
        if (levels == null) {
            int[] configMins = new int[BUTTON_COUNT];
            for (int b = 0; b < BUTTON_COUNT; b++) configMins[b] = ModConfig.getEnchantmentBaseRequiredLevel(b);
            return EnchantCooldownCalculator.computeFirstUseLevels(currentLevel, configMins)[buttonId];
        }
        return levels[buttonId];
    }

    /**
     * Removes all cooldown data for the given player, resetting their required levels
     * back to the configured base values on next use.
     *
     * @param playerId player UUID
     * @return {@code true} if the player had data that was removed, {@code false} if there was nothing to reset
     */
    public boolean resetPlayer(UUID playerId) {
        boolean had = playerRequiredLevels.remove(playerId) != null;
        if (had) {
            setDirty();
        }
        return had;
    }

    /**
     * Called after a successful enchantment. Updates the next minimum required level
     * for ALL three buttons.
     *
     * <p>Every button receives its own independent cooldown calculation, then a minimum
     * gap of 1 level is enforced between consecutive buttons (button 2 ≥ button 1 + 1,
     * button 3 ≥ button 2 + 1). The gap used is always {@code max(naturalGap, 1)}.
     *
     * <p>This call only ever raises a button's required level, never lowered.
     *
     * @param playerId  player UUID
     * @param level     player's level or last level at the time of enchanting
     */
    public void recordEnchant(UUID playerId, int level) {
        int[] levels = playerRequiredLevels.computeIfAbsent(playerId, _ -> buildDefaultLevels(level));

        double bias = ModConfig.getEnchantmentRequiredLevelBias();
        int[] next = EnchantCooldownCalculator.computeNextLevels(level, bias);

        for (int b = 0; b < BUTTON_COUNT; b++) {
            if (next[b] > levels[b]) {
                levels[b] = next[b];
            }
        }

        setDirty();
    }

    private static int[] buildDefaultLevels(int currentPlayerLevel) {
        int[] configMins = new int[BUTTON_COUNT];
        for (int b = 0; b < BUTTON_COUNT; b++) configMins[b] = ModConfig.getEnchantmentBaseRequiredLevel(b);
        return EnchantCooldownCalculator.computeFirstUseLevels(currentPlayerLevel, configMins);
    }
}
