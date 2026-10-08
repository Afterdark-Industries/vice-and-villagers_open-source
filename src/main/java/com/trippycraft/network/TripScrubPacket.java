package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import com.trippycraft.substance.SubstanceEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class TripScrubPacket implements CustomPacketPayload {
    public static final Type<TripScrubPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "trip_scrub"));
    public static final StreamCodec<FriendlyByteBuf, TripScrubPacket> STREAM_CODEC = StreamCodec.ofMember(TripScrubPacket::encode, TripScrubPacket::decode);

    @Override
    public Type<TripScrubPacket> type() {
        return TYPE;
    }
    private final int newAgeTicks;

    public TripScrubPacket(int newAgeTicks) {
        this.newAgeTicks = Math.max(0, newAgeTicks);
    }

    public static void encode(TripScrubPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.newAgeTicks);
    }

    public static TripScrubPacket decode(FriendlyByteBuf buffer) {
        return new TripScrubPacket(buffer.readInt());
    }

    public static void handle(TripScrubPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (player == null) {
            return;
        }
        SubstanceEngine.scrubPsilocybin(player, packet.newAgeTicks);
    }

    public int newAgeTicks() {
        return newAgeTicks;
    }
}
