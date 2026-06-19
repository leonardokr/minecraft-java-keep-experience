package com.ziondev.experiencetweaks.network;

import com.ziondev.experiencetweaks.ExperienceTweaksMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from server → client to inform the client of the player's
 * current per-button required levels at the enchantment table.
 */
public record SyncEnchantLevelsPacket(List<Integer> requiredLevels) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncEnchantLevelsPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ExperienceTweaksMod.MODID, "sync_enchant_levels"));

    public static final StreamCodec<ByteBuf, SyncEnchantLevelsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VAR_INT),
            SyncEnchantLevelsPacket::requiredLevels,
            SyncEnchantLevelsPacket::new
    );

    @Override
    public CustomPacketPayload.@NonNull Type<SyncEnchantLevelsPacket> type() {
        return TYPE;
    }
}
