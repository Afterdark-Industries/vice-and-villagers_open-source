package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import com.trippycraft.economy.MarketService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MarketActionPacket implements CustomPacketPayload {
    public static final Type<MarketActionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "market_action"));
    public static final StreamCodec<FriendlyByteBuf, MarketActionPacket> STREAM_CODEC = StreamCodec.ofMember(MarketActionPacket::encode, MarketActionPacket::decode);

    @Override
    public Type<MarketActionPacket> type() {
        return TYPE;
    }
    private final Action action;
    private final String entryId;
    private final int amount;

    public MarketActionPacket(Action action, String entryId, int amount) {
        this.action = action;
        this.entryId = entryId == null ? "" : entryId;
        this.amount = amount;
    }

    public static void encode(MarketActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action.ordinal());
        buffer.writeUtf(packet.entryId);
        buffer.writeVarInt(packet.amount);
    }

    public static MarketActionPacket decode(FriendlyByteBuf buffer) {
        int actionIndex = buffer.readVarInt();
        Action[] actions = Action.values();
        Action action = actionIndex >= 0 && actionIndex < actions.length ? actions[actionIndex] : Action.SELL_PRODUCT;
        return new MarketActionPacket(action, buffer.readUtf(), buffer.readVarInt());
    }

    public static void handle(MarketActionPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (player != null) {
            MarketService.handleMarketAction(player, packet);
        }
    }

    public Action action() {
        return action;
    }

    public String entryId() {
        return entryId;
    }

    public int amount() {
        return amount;
    }

    public enum Action {
        SELL_PRODUCT,
        BUY_MATERIAL,
        COMPLETE_CONTRACT,
        CLOSE_MARKET
    }
}
