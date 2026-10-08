package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import com.trippycraft.economy.CustomerService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public final class CustomerActionPacket implements CustomPacketPayload {
    public static final Type<CustomerActionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "customer_action"));
    public static final StreamCodec<FriendlyByteBuf, CustomerActionPacket> STREAM_CODEC = StreamCodec.ofMember(CustomerActionPacket::encode, CustomerActionPacket::decode);

    @Override
    public Type<CustomerActionPacket> type() {
        return TYPE;
    }
    private final UUID customerId;
    private final String offerId;

    public CustomerActionPacket(UUID customerId, String offerId) {
        this.customerId = customerId;
        this.offerId = offerId == null ? "" : offerId;
    }

    public static CustomerActionPacket close(UUID customerId) {
        return new CustomerActionPacket(customerId, "");
    }

    public static void encode(CustomerActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.customerId);
        buffer.writeUtf(packet.offerId);
    }

    public static CustomerActionPacket decode(FriendlyByteBuf buffer) {
        return new CustomerActionPacket(buffer.readUUID(), buffer.readUtf());
    }

    public static void handle(CustomerActionPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (player != null) {
            CustomerService.handleCustomerAction(player, packet);
        }
    }

    public UUID customerId() {
        return customerId;
    }

    public String offerId() {
        return offerId;
    }

    public boolean isCloseRequest() {
        return offerId.isEmpty();
    }
}
