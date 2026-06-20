package com.ziondev.experiencetweaks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Single access point for all Experience Tweaks configuration values.
 * <p>
 * Every getter in this class:
 * <ul>
 *   <li>reads the underlying {@link Config} spec value,</li>
 *   <li>validates/parses where needed.</li>
 *   <li>calls {@link #broadcastConfigError} on bad input — which logs a warning
 *       <em>and</em> sends a red alert to all online server operators, and</li>
 *   <li>returns a safe fallback so callers never receive a broken value.</li>
 * </ul>
 *
 * <p>No other class in the mod should access {@link Config} fields directly,
 * except {@link ExperienceTweaksMod} which registers the spec itself.
 */
public final class ModConfig {

    private ModConfig() {}

    /**
     * Enumeration of all configuration error codes for Experience Tweaks.
     * <p>
     * Each entry carries:
     * <ul>
     *   <li><b>code</b> — short identifier shown in both the log and the in-game alert
     *       (e.g. {@code ET-0x001})</li>
     *   <li><b>playerMessage</b> — human-readable alert sent to server operators</li>
     * </ul>
     */
    public enum ConfigError {

        INVALID_COST_ITEM("ET-0x001"),
        COST_ITEM_NOT_FOUND("ET-0x002"),
        DIRECT_EXPERIENCE("ET-0x003"),
        DONT_KEEP_EXPERIENCE("ET-0x004"),
        ENCHANTMENT_COST_MULTIPLIER("ET-0x005"),
        ENCHANTMENT_COOLDOWN_TYPE("ET-0x006"),
        ENCHANTMENT_BASE_REQUIRED_LEVELS("ET-0x007"),
        ENCHANTMENT_REQUIRED_LEVEL_BIAS("ET-0x008"),
        GIVE_EXPERIENCE_EVERY_DAY("ET-0x009"),
        GIVE_EXPERIENCE_EVERY_DAY_BASE("ET-0x00a"),
        GIVE_EXPERIENCE_EVERY_DAY_GROWTH("ET-0x00b");

        private final String code;

        ConfigError(String code) {
            this.code = code;
        }

        /** Short error code displayed in both log and in-game alert (e.g. {@code ET-0x001}). */
        public String code()          { return code; }
        /** Translation key for the human-readable alert sent to online server operators. */
        public String playerMessageKey() {
            return "experiencetweaks.config.error." + code.toLowerCase().replace("-", "_");
        }
    }

    /**
     * Tracks errors that have already been broadcast during this server session,
     * so the same alert is never repeated twice in chat (it still appears once in the log).
     */
    private static final Set<ConfigError> REPORTED = Collections.synchronizedSet(
            EnumSet.noneOf(ConfigError.class));

    /**
     * Logs a config error and — the first time it is triggered — broadcasts a
     * red alert to all online server operators.
     *
     * <p>The in-game message format is:
     * <pre>  [ExperienceTweaks] [ET-0x001] Human-readable description.</pre>
     *
     * @param error the {@link ConfigError} to report
     */
    private static void broadcastConfigError(ConfigError error) {
        String localizedLog = net.minecraft.locale.Language.getInstance().getOrDefault(error.playerMessageKey());
        ExperienceTweaksMod.LOGGER.warn("[ModConfig] [{}] {}", error.code(), localizedLog);

        // Only broadcast once per session to avoid spamming operators.
        if (!REPORTED.add(error)) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return; // Client-only context; skip in-game broadcast.
        }

        Component alert = Component.empty()
                .append(Component.literal("[ExperienceTweaks] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("[" + error.code() + "] ").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.translatable(error.playerMessageKey()).withStyle(ChatFormatting.RED));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().getOps().get(new NameAndId(player.getGameProfile())) != null) {
                player.sendSystemMessage(alert);
            }
        }
    }

    /**
     * Returns {@code true} if the given player name is on the opt-out list
     * (i.e., they do NOT keep their experience on death).
     *
     * @param playerName the player's display name
     * @return {@code true} if the player should lose XP on death; {@code false} otherwise
     */
    public static boolean isDontKeepExperience(String playerName) {
        try {
            List<? extends String> list = Config.DONT_KEEP_EXPERIENCE.get();
            return list.contains(playerName);
        } catch (Exception e) {
            broadcastConfigError(ConfigError.DONT_KEEP_EXPERIENCE);
            return false;
        }
    }

    /**
     * Returns {@code true} if XP should be delivered directly to the player
     * instead of spawning orbs.
     * <p>Fallback: {@code true}
     */
    public static boolean isDirectExperience() {
        try {
            return Config.DIRECT_EXPERIENCE.get();
        } catch (Exception e) {
            broadcastConfigError(ConfigError.DIRECT_EXPERIENCE);
            return true;
        }
    }

    /**
     * Returns the {@link Item} used as currency when enchanting.
     * <p>Falls back to {@link Items#LAPIS_LAZULI} if the configured registry name
     * is blank, invalid, or not found.
     */
    public static Item getEnchantmentCostItem() {
        try {
            String configuredItem = Config.ENCHANTMENT_COST_ITEM.get();
            if (!configuredItem.isBlank()) {
                return BuiltInRegistries.ITEM
                        .getOptional(Identifier.parse(configuredItem))
                        .orElseGet(() -> {
                            broadcastConfigError(ConfigError.COST_ITEM_NOT_FOUND);
                            return Items.LAPIS_LAZULI;
                        });
            }
        } catch (Exception e) {
            broadcastConfigError(ConfigError.INVALID_COST_ITEM);
        }
        return Items.LAPIS_LAZULI;
    }

    /**
     * Returns the item-cost multiplier applied to the enchantment button index.
     * <p>Fallback: {@code 1.5}
     */
    public static double getEnchantmentCostMultiplier() {
        try {
            return Config.ENCHANTMENT_COST_MULTIPLIER.get();
        } catch (Exception e) {
            broadcastConfigError(ConfigError.ENCHANTMENT_COST_MULTIPLIER);
            return 1.5;
        }
    }

    /**
     * Returns the cooldown progression mode for enchantment buttons.
     * Valid values: {@code "current_level"}, {@code "last_level"}.
     * <p>Fallback: {@code "current_level"}
     */
    public static String getEnchantmentCooldownType() {
        try {
            return Config.ENCHANTMENT_COOLDOWN_TYPE.get();
        } catch (Exception e) {
            broadcastConfigError(ConfigError.ENCHANTMENT_COOLDOWN_TYPE);
            return "current_level";
        }
    }

    /**
     * Returns the configured base required level for the given 0-indexed button.
     * <p>Falls back to {@code (buttonId + 1) * 10} (i.e., 10 / 20 / 30) if the list
     * is too short or the config cannot be read.
     *
     * @param buttonId 0-based enchantment button index (0, 1, 2)
     */
    public static int getEnchantmentBaseRequiredLevel(int buttonId) {
        try {
            List<? extends Integer> baseLevels = Config.ENCHANTMENT_BASE_REQUIRED_LEVELS.get();
            if (buttonId < baseLevels.size()) {
                return baseLevels.get(buttonId);
            }
        } catch (Exception e) {
            broadcastConfigError(ConfigError.ENCHANTMENT_BASE_REQUIRED_LEVELS);
        }
        return (buttonId + 1) * 10;
    }

    /**
     * Returns the difficulty weight (0.0–1.0) used in the cooldown curve formula.
     * <p>Fallback: {@code 0.25}
     */
    public static double getEnchantmentRequiredLevelBias() {
        try {
            return Config.ENCHANTMENT_REQUIRED_LEVEL_BIAS.get();
        } catch (Exception e) {
            broadcastConfigError(ConfigError.ENCHANTMENT_REQUIRED_LEVEL_BIAS);
            return 0.25;
        }
    }

    /**
     * Returns {@code true} if the daily survival experience rewards are enabled.
     * <p>Fallback: {@code false}
     */
    public static boolean isGiveExperienceEveryDayEnabled() {
        try {
            return Config.GIVE_EXPERIENCE_EVERY_DAY.get();
        } catch (Exception e) {
            broadcastConfigError(ConfigError.GIVE_EXPERIENCE_EVERY_DAY);
            return false;
        }
    }

    /**
     * Returns the base experience points given to players each day they survive.
     * <p>Fallback: {@code 5}
     */
    public static int getGiveExperienceEveryDayBase() {
        try {
            return Config.GIVE_EXPERIENCE_EVERY_DAY_BASE.get();
        } catch (Exception e) {
            broadcastConfigError(ConfigError.GIVE_EXPERIENCE_EVERY_DAY_BASE);
            return 5;
        }
    }

    /**
     * Returns the growth multiplier for daily survival rewards.
     * <p>Fallback: {@code 0.1}
     */
    public static double getGiveExperienceEveryDayGrowth() {
        try {
            return Config.GIVE_EXPERIENCE_EVERY_DAY_GROWTH.get();
        } catch (Exception e) {
            broadcastConfigError(ConfigError.GIVE_EXPERIENCE_EVERY_DAY_GROWTH);
            return 0.1;
        }
    }
}
