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
 * Server-side persistent data tracking player-related progression state:
 * - Minimum required level for enchantment table buttons.
 * - Survival streak information for the daily experience rewards.
 * <p></p>
 * Serialized via Codec and stored in world saves under the id defined by {@link #TYPE}.
 */
public class PlayerEnchantData extends SavedData {

    /** Inner class representing a player's complete custom progression state. */
    public static class PlayerState {
        private final int[] levels;
        private long lastRewardDay;
        private int daysSurvived;

        public PlayerState(int[] levels, long lastRewardDay, int daysSurvived) {
            this.levels = levels;
            this.lastRewardDay = lastRewardDay;
            this.daysSurvived = daysSurvived;
        }

        public int[] getLevels() {
            return levels;
        }

        public long getLastRewardDay() {
            return lastRewardDay;
        }

        public void setLastRewardDay(long lastRewardDay) {
            this.lastRewardDay = lastRewardDay;
        }

        public int getDaysSurvived() {
            return daysSurvived;
        }

        public void setDaysSurvived(int daysSurvived) {
            this.daysSurvived = daysSurvived;
        }
    }

    /** Codec for a single player entry, supporting backwards compatibility with old saves. */
    private static final Codec<Map.Entry<UUID, PlayerState>> ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(Map.Entry::getKey),
            Codec.INT.listOf().fieldOf("levels").forGetter(e -> {
                int[] arr = e.getValue().getLevels();
                List<Integer> list = new ArrayList<>(arr.length);
                for (int v : arr) list.add(v);
                return list;
            }),
            Codec.LONG.optionalFieldOf("lastRewardDay", -1L).forGetter(e -> e.getValue().getLastRewardDay()),
            Codec.INT.optionalFieldOf("daysSurvived", 0).forGetter(e -> e.getValue().getDaysSurvived())
    ).apply(inst, (uuid, list, lastRewardDay, daysSurvived) -> {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return Map.entry(uuid, new PlayerState(arr, lastRewardDay, daysSurvived));
    }));

    /** Full data codec: list of player entries. */
    private static final Codec<PlayerEnchantData> CODEC = ENTRY_CODEC.listOf()
            .xmap(
                    entries -> {
                        PlayerEnchantData data = new PlayerEnchantData();
                        for (Map.Entry<UUID, PlayerState> e : entries) {
                            data.playerStates.put(e.getKey(), e.getValue());
                        }
                        return data;
                    },
                    data -> new ArrayList<>(data.playerStates.entrySet())
            );

    public static final SavedDataType<PlayerEnchantData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ExperienceTweaksMod.MODID, "enchant_data"),
            PlayerEnchantData::new,
            CODEC
    );

    private static final int BUTTON_COUNT = 3;

    /** Maps player UUID → PlayerState (levels & daily survival streak data). */
    private final Map<UUID, PlayerState> playerStates = new HashMap<>();

    /** No-arg constructor used by the SavedDataType factory. */
    public PlayerEnchantData() {}

    /**
     * Returns the minimum player level required to use enchantment button {@code buttonId}
     * for the given player.
     */
    public int getRequiredLevel(UUID playerId, int buttonId, int currentLevel) {
        PlayerState state = playerStates.get(playerId);
        if (state == null) {
            int[] configMins = new int[BUTTON_COUNT];
            for (int b = 0; b < BUTTON_COUNT; b++) configMins[b] = ModConfig.getEnchantmentBaseRequiredLevel(b);
            
            if (ModConfig.getEnchantmentCooldownType().equalsIgnoreCase("last_level")) {
                return configMins[buttonId];
            }
            return EnchantCooldownCalculator.computeFirstUseLevels(currentLevel, configMins)[buttonId];
        }
        return state.getLevels()[buttonId];
    }

    /**
     * Removes all data for the given player.
     */
    public boolean resetPlayer(UUID playerId) {
        boolean had = playerStates.remove(playerId) != null;
        if (had) {
            setDirty();
        }
        return had;
    }

    /**
     * Called after a successful enchantment. Updates the next minimum required level
     * for the buttons.
     */
    public void recordEnchant(UUID playerId, int buttonId, int currentPlayerLevel) {
        PlayerState state = playerStates.computeIfAbsent(playerId, _ -> new PlayerState(buildDefaultLevels(currentPlayerLevel), -1L, 0));
        int[] levels = state.getLevels();

        double bias = ModConfig.getEnchantmentRequiredLevelBias();
        String cooldownType = ModConfig.getEnchantmentCooldownType();

        if (cooldownType.equalsIgnoreCase("last_level")) {
            int prevLevel = levels[buttonId];
            int[] next = EnchantCooldownCalculator.computeNextLevels(prevLevel, bias);
            levels[buttonId] = Math.max(levels[buttonId], next[buttonId]);

            // Enforce minimum gap of 1 level between consecutive buttons
            for (int b = 1; b < BUTTON_COUNT; b++) {
                if (levels[b] < levels[b - 1] + 1) {
                    levels[b] = levels[b - 1] + 1;
                }
            }
        } else {
            // "current_level" (default)
            int[] next = EnchantCooldownCalculator.computeNextLevels(currentPlayerLevel, bias);
            for (int b = 0; b < BUTTON_COUNT; b++) {
                if (next[b] > levels[b]) {
                    levels[b] = next[b];
                }
            }
        }

        setDirty();
    }

    /** Resets the player's survival days to 0 upon death. */
    public void resetDailyXpSurvival(UUID playerId) {
        PlayerState state = playerStates.get(playerId);
        if (state != null) {
            state.setDaysSurvived(0);
            setDirty();
        }
    }

    /** Performs tick checks for daily survival rewards. Checked roughly once per second. */
    public void tickDailyXp(net.minecraft.server.level.ServerPlayer player) {
        if (!ModConfig.isGiveExperienceEveryDayEnabled()) {
            return;
        }

        long rawDay = player.level().getOverworldClockTime() / 24000L;
        final long currentDay = rawDay < 0 ? 0L : rawDay;

        PlayerState state = playerStates.computeIfAbsent(player.getUUID(), _ -> new PlayerState(buildDefaultLevels(player.experienceLevel), currentDay, 0));
        
        long lastReward = state.getLastRewardDay();
        if (lastReward == -1L) {
            state.setLastRewardDay(currentDay);
            setDirty();
            return;
        }

        if (currentDay > lastReward) {
            long daysPassed = currentDay - lastReward;
            state.setLastRewardDay(currentDay);
            state.setDaysSurvived(state.getDaysSurvived() + (int) daysPassed);
            setDirty();

            int basePoints = ModConfig.getGiveExperienceEveryDayBase();
            double growthRate = ModConfig.getGiveExperienceEveryDayGrowth();
            int daysSurvived = state.getDaysSurvived();

            double rewardedPointsDouble = basePoints * (1.0 + growthRate * daysSurvived);
            int rewardedPoints = (int) Math.round(rewardedPointsDouble);

            if (rewardedPoints > 0) {
                player.giveExperiencePoints(rewardedPoints);
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable(
                        "experiencetweaks.daily_xp.reward",
                        rewardedPoints,
                        daysSurvived
                    ).withStyle(net.minecraft.ChatFormatting.GREEN)
                );
            }
        }
    }

    private static int[] buildDefaultLevels(int currentPlayerLevel) {
        int[] configMins = new int[BUTTON_COUNT];
        for (int b = 0; b < BUTTON_COUNT; b++) configMins[b] = ModConfig.getEnchantmentBaseRequiredLevel(b);
        
        if (ModConfig.getEnchantmentCooldownType().equalsIgnoreCase("last_level")) {
            return configMins;
        }
        return EnchantCooldownCalculator.computeFirstUseLevels(currentPlayerLevel, configMins);
    }
}
