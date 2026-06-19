package com.ziondev.experiencetweaks;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.Collections;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DONT_KEEP_EXPERIENCE = BUILDER
            .comment("List of player names who do NOT want keep their experience after death.")
            .defineList("dontKeepExperience", Collections.emptyList(), () -> "", obj -> obj instanceof String);

    public static final ModConfigSpec.BooleanValue DIRECT_EXPERIENCE = BUILDER
            .comment("If true, experience points will be inserted directly into the player instead of dropping as orbs.")
            .define("directExperience", true);

    public static final ModConfigSpec.ConfigValue<String> ENCHANTMENT_COST_ITEM = BUILDER
            .comment("Item consumed instead of experience when enchanting. Use registry name like 'minecraft:diamond'. If empty or invalid, lapis lazuli is used.")
            .define("enchantmentCostItem", "minecraft:emerald");

    public static final ModConfigSpec.DoubleValue ENCHANTMENT_COST_MULTIPLIER = BUILDER
            .comment("Multiplier for the item cost based on the required enchantment level. (e.g., 30 levels * 0.1 = 3 items)")
            .defineInRange("enchantmentCostMultiplier", 1.5, 0.0, 100.0);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> ENCHANTMENT_BASE_REQUIRED_LEVELS = BUILDER
            .comment("Initial player experience levels required for enchantment table buttons 1, 2 and 3. Player progression is stored separately per player.")
            .defineList("enchantmentBaseRequiredLevels", List.of(10, 15, 20), () -> 0, obj -> obj instanceof Integer integer && integer >= 0);

    public static final ModConfigSpec.DoubleValue ENCHANTMENT_REQUIRED_LEVEL_BIAS = BUILDER
            .comment(
                "Difficulty weight for the enchantment cooldown curve. Range: 0.0 to 1.0.",
                "Formula: ceil(buttonIndex x bias x 50 / sqrt(currentPlayerLevel))",
                "Uses a square-root curve so the cooldown stays meaningful at high levels (200+).",
                "  0.0 = minimum difficulty (always +1 level)",
                "  0.5 = balanced default",
                "  1.0 = maximum difficulty",
                "Example increments for button 3 (hardest):",
                "  Level  10: bias 0.1=+5,  bias 0.5=+24, bias 1.0=+48",
                "  Level  50: bias 0.1=+2,  bias 0.5=+11, bias 1.0=+21",
                "  Level 100: bias 0.1=+2,  bias 0.5=+8,  bias 1.0=+15",
                "  Level 200: bias 0.1=+1,  bias 0.5=+5,  bias 1.0=+11",
                "  Level 500: bias 0.1=+1,  bias 0.5=+3,  bias 1.0=+7",
                "  Level 1000: bias 0.1=+1, bias 0.5=+3,  bias 1.0=+5"
            )
            .defineInRange("enchantmentRequiredLevelBias", 0.25, 0.0, 1.0);

    static final ModConfigSpec SPEC = BUILDER.build();
}
