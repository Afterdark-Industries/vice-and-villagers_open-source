package com.trippycraft.network;

import com.trippycraft.TrippyCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Identifies the closest live tavern, its orientation, and its music source. */
public record TavernAmbiencePacket(boolean active,
                                   BlockPos origin,
                                   Rotation rotation,
                                   BlockPos jukebox,
                                   long playlistTick)
        implements CustomPacketPayload {
    public static final Type<TavernAmbiencePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "tavern_ambience")
    );
    public static final StreamCodec<FriendlyByteBuf, TavernAmbiencePacket> STREAM_CODEC =
            StreamCodec.ofMember(TavernAmbiencePacket::encode, TavernAmbiencePacket::decode);

    public static TavernAmbiencePacket inactive() {
        return new TavernAmbiencePacket(false, BlockPos.ZERO, Rotation.NONE, BlockPos.ZERO, 0L);
    }

    public static void encode(TavernAmbiencePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        if (packet.active) {
            buffer.writeBlockPos(packet.origin);
            buffer.writeEnum(packet.rotation);
            buffer.writeBlockPos(packet.jukebox);
            buffer.writeVarLong(packet.playlistTick);
        }
    }

    public static TavernAmbiencePacket decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) {
            return inactive();
        }
        return new TavernAmbiencePacket(
                true,
                buffer.readBlockPos(),
                buffer.readEnum(Rotation.class),
                buffer.readBlockPos(),
                buffer.readVarLong()
        );
    }

    public static void handle(TavernAmbiencePacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.trippycraft.client.ClientPacketHandlers.handleTavernAmbience(packet);
        }
    }

    @Override
    public Type<TavernAmbiencePacket> type() {
        return TYPE;
    }
}
