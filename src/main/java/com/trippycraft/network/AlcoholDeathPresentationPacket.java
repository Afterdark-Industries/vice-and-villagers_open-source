package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.loading.FMLEnvironment;

public final class AlcoholDeathPresentationPacket implements CustomPacketPayload {
    public static final Type<AlcoholDeathPresentationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "alcohol_death_presentation"));
    public static final StreamCodec<FriendlyByteBuf, AlcoholDeathPresentationPacket> STREAM_CODEC = StreamCodec.ofMember(AlcoholDeathPresentationPacket::encode, AlcoholDeathPresentationPacket::decode);

    @Override
    public Type<AlcoholDeathPresentationPacket> type() {
        return TYPE;
    }
    private final boolean blackoutWakeUp;

    public AlcoholDeathPresentationPacket(boolean blackoutWakeUp) {
        this.blackoutWakeUp = blackoutWakeUp;
    }

    public static AlcoholDeathPresentationPacket blackout() {
        return new AlcoholDeathPresentationPacket(true);
    }

    public static AlcoholDeathPresentationPacket clear() {
        return new AlcoholDeathPresentationPacket(false);
    }

    public static void encode(AlcoholDeathPresentationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.blackoutWakeUp);
    }

    public static AlcoholDeathPresentationPacket decode(FriendlyByteBuf buffer) {
        return new AlcoholDeathPresentationPacket(buffer.readBoolean());
    }

    public static void handle(AlcoholDeathPresentationPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.trippycraft.client.ClientPacketHandlers.handleAlcoholDeathPresentation(packet);
        }
    }

    public boolean blackoutWakeUp() {
        return blackoutWakeUp;
    }
}