package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashSet;
import java.util.Set;

public final class CursedFindingsSyncPacket implements CustomPacketPayload {
    public static final Type<CursedFindingsSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "cursed_findings_sync"));
    public static final StreamCodec<FriendlyByteBuf, CursedFindingsSyncPacket> STREAM_CODEC = StreamCodec.ofMember(CursedFindingsSyncPacket::encode, CursedFindingsSyncPacket::decode);

    @Override
    public Type<CursedFindingsSyncPacket> type() {
        return TYPE;
    }
    private final Set<String> unlockedFindings;
    private final boolean openJournal;

    public CursedFindingsSyncPacket(Set<String> unlockedFindings, boolean openJournal) {
        this.unlockedFindings = Set.copyOf(unlockedFindings);
        this.openJournal = openJournal;
    }

    public static void encode(CursedFindingsSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.openJournal);
        buffer.writeVarInt(packet.unlockedFindings.size());
        for (String finding : packet.unlockedFindings) {
            buffer.writeUtf(finding);
        }
    }

    public static CursedFindingsSyncPacket decode(FriendlyByteBuf buffer) {
        boolean openJournal = buffer.readBoolean();
        int size = buffer.readVarInt();
        Set<String> unlocked = new LinkedHashSet<>();
        for (int i = 0; i < size; i++) {
            unlocked.add(buffer.readUtf());
        }
        return new CursedFindingsSyncPacket(unlocked, openJournal);
    }

    public static void handle(CursedFindingsSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.trippycraft.client.ClientPacketHandlers.handleCursedFindings(packet));
    }

    public Set<String> unlockedFindings() {
        return unlockedFindings;
    }

    public boolean openJournal() {
        return openJournal;
    }
}
