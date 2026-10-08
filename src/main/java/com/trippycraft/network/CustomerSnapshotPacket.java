package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CustomerSnapshotPacket implements CustomPacketPayload {
    public static final Type<CustomerSnapshotPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "customer_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, CustomerSnapshotPacket> STREAM_CODEC = StreamCodec.ofMember(CustomerSnapshotPacket::encode, CustomerSnapshotPacket::decode);

    @Override
    public Type<CustomerSnapshotPacket> type() {
        return TYPE;
    }
    private final boolean openScreen;
    private final UUID customerId;
    private final int reputation;
    private final int level;
    private final long gameDay;
    private final List<OfferInfo> offers;

    public CustomerSnapshotPacket(boolean openScreen,
                                  UUID customerId,
                                  int reputation,
                                  int level,
                                  long gameDay,
                                  List<OfferInfo> offers) {
        this.openScreen = openScreen;
        this.customerId = customerId;
        this.reputation = Math.max(0, reputation);
        this.level = Math.max(0, level);
        this.gameDay = Math.max(0L, gameDay);
        this.offers = List.copyOf(offers);
    }

    public static void encode(CustomerSnapshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.openScreen);
        buffer.writeUUID(packet.customerId);
        buffer.writeVarInt(packet.reputation);
        buffer.writeVarInt(packet.level);
        buffer.writeLong(packet.gameDay);
        buffer.writeVarInt(packet.offers.size());
        for (OfferInfo offer : packet.offers) {
            buffer.writeUtf(offer.id());
            buffer.writeUtf(offer.productId());
            buffer.writeUtf(offer.requestType());
            buffer.writeVarInt(offer.quantity());
            buffer.writeVarInt(offer.rewardEmeralds());
            buffer.writeVarInt(offer.reputationReward());
            buffer.writeVarInt(offer.unitPrice());
            buffer.writeVarInt(offer.habitLevel());
        }
    }

    public static CustomerSnapshotPacket decode(FriendlyByteBuf buffer) {
        boolean openScreen = buffer.readBoolean();
        UUID customerId = buffer.readUUID();
        int reputation = buffer.readVarInt();
        int level = buffer.readVarInt();
        long gameDay = buffer.readLong();
        int offerCount = buffer.readVarInt();
        List<OfferInfo> offers = new ArrayList<>();
        for (int i = 0; i < offerCount; i++) {
            offers.add(new OfferInfo(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            ));
        }
        return new CustomerSnapshotPacket(openScreen, customerId, reputation, level, gameDay, offers);
    }

    public static void handle(CustomerSnapshotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.trippycraft.client.ClientPacketHandlers.handleCustomerSnapshot(packet));
    }

    public boolean openScreen() {
        return openScreen;
    }

    public UUID customerId() {
        return customerId;
    }

    public int reputation() {
        return reputation;
    }

    public int level() {
        return level;
    }

    public long gameDay() {
        return gameDay;
    }

    public List<OfferInfo> offers() {
        return offers;
    }

    public record OfferInfo(
            String id,
            String productId,
            String requestType,
            int quantity,
            int rewardEmeralds,
            int reputationReward,
            int unitPrice,
            int habitLevel
    ) {
    }
}
