package com.ziondev.experiencetweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.ziondev.experiencetweaks.ExperienceTweaksMod;
import com.ziondev.experiencetweaks.PlayerEnchantData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Registers the {@code /experiencetweaks} command tree.
 * Requires the {@code LEVEL_ADMINS} permission (OP level 3, same as /op itself).
 *
 * <pre>
 * /experiencetweaks reset &lt;player&gt; — resets enchantment cooldown for one or more players
 * </pre>
 */
public class EnchantCooldownCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("experiencetweaks")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.players())
                        .executes(EnchantCooldownCommand::executeReset)
                    )
                )
        );
    }

    private static int executeReset(CommandContext<CommandSourceStack> ctx) {
        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(ctx, "player");
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.translatable("experiencetweaks.command.reset.invalid_player"));
            return 0;
        }

        PlayerEnchantData data = ExperienceTweaksMod.getEnchantData();
        if (data == null) {
            ctx.getSource().sendFailure(Component.translatable("experiencetweaks.command.reset.data_unavailable"));
            return 0;
        }

        int resetCount = 0;
        for (ServerPlayer player : targets) {
            boolean had = data.resetPlayer(player.getUUID());
            // Sync the now-reset levels back to the client immediately
            ExperienceTweaksMod.syncEnchantLevels(player);

            if (had) {
                ctx.getSource().sendSuccess(
                    () -> Component.translatable("experiencetweaks.command.reset.success", player.getName()),
                    true
                );
            } else {
                ctx.getSource().sendSuccess(
                    () -> Component.translatable("experiencetweaks.command.reset.no_data", player.getName()),
                    false
                );
            }
            resetCount++;
        }

        return resetCount;
    }
}
