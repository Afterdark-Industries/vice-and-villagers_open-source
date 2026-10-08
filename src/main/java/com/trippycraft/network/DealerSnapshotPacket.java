package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class DealerSnapshotPacket implements CustomPacketPayload {
    public static final Type<DealerSnapshotPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "dealer_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, DealerSnapshotPacket> STREAM_CODEC = StreamCodec.ofMember(DealerSnapshotPacket::encode, DealerSnapshotPacket::decode);

    @Override
    public Type<DealerSnapshotPacket> type() {
        return TYPE;
    }
    private final boolean openScreen;
    private final int reputation;
    private final int level;
    private final int socialDiscountPercent;

    public DealerSnapshotPacket(boolean openScreen, int reputation, int level, int socialDiscountPercent) {
        this.openScreen = openScreen;
        this.reputation = Math.max(0, reputation);
        this.level = Math.max(0, level);
        this.socialDiscountPercent = Math.clamp(socialDiscountPercent, 0, 25);
    }

    public static void encode(DealerSnapshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.openScreen);
        buffer.writeVarInt(packet.reputation);
        buffer.writeVarInt(packet.level);
        buffer.writeVarInt(packet.socialDiscountPercent);
    }

    public static DealerSnapshotPacket decode(FriendlyByteBuf buffer) {
        return new DealerSnapshotPacket(buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(DealerSnapshotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.trippycraft.client.ClientPacketHandlers.handleDealerSnapshot(packet));
    }

    public boolean openScreen() {
        return openScreen;
    }

    public int reputation() {
        return reputation;
    }

    public int level() {
        return level;
    }

    public int socialDiscountPercent() {
        return socialDiscountPercent;
    }
}
