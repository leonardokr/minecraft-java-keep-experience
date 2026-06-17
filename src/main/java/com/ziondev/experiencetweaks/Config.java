package com.ziondev.experiencetweaks;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.Collections;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_PLAYERS = BUILDER
            .comment("List of player names who do NOT want to keep their experience upon death.")
            .defineList("blacklistedPlayers", Collections.emptyList(), obj -> obj instanceof String);

    static final ModConfigSpec SPEC = BUILDER.build();
}
