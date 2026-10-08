package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import com.trippycraft.economy.DealerService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class DealerActionPacket implements CustomPacketPayload {
    public static final Type<DealerActionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "dealer_action"));
    public static final StreamCodec<FriendlyByteBuf, DealerActionPacket> STREAM_CODEC = StreamCodec.ofMember(DealerActionPacket::encode, DealerActionPacket::decode);

    @Override
    public Type<DealerActionPacket> type() {
        return TYPE;
    }
    private final String productId;
    private final int amount;

    public DealerActionPacket(String productId, int amount) {
        this.productId = productId == null ? "" : productId;
        this.amount = Math.max(1, amount);
    }

    public static void encode(DealerActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.productId);
        buffer.writeVarInt(packet.amount);
    }

    public static DealerActionPacket decode(FriendlyByteBuf buffer) {
        return new DealerActionPacket(buffer.readUtf(), buffer.readVarInt());
    }

    public static void handle(DealerActionPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (player != null) {
            DealerService.handleDealerAction(player, packet);
        }
    }

    public String productId() {
        return productId;
    }

    public int amount() {
        return amount;
    }
}
