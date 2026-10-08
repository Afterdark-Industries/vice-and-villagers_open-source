package com.trippycraft.network;

import com.trippycraft.substance.PsychoactiveExperience;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "21";

    private ModNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(ExperienceSnapshotPacket.TYPE, ExperienceSnapshotPacket.STREAM_CODEC, ExperienceSnapshotPacket::handle);
        registrar.playToClient(AlcoholDeathPresentationPacket.TYPE, AlcoholDeathPresentationPacket.STREAM_CODEC, AlcoholDeathPresentationPacket::handle);
        registrar.playToServer(TripScrubPacket.TYPE, TripScrubPacket.STREAM_CODEC, TripScrubPacket::handle);
        registrar.playToClient(CursedFindingsSyncPacket.TYPE, CursedFindingsSyncPacket.STREAM_CODEC, CursedFindingsSyncPacket::handle);
        registrar.playToClient(MarketSnapshotPacket.TYPE, MarketSnapshotPacket.STREAM_CODEC, MarketSnapshotPacket::handle);
        registrar.playToServer(MarketActionPacket.TYPE, MarketActionPacket.STREAM_CODEC, MarketActionPacket::handle);
        registrar.playToClient(DealerSnapshotPacket.TYPE, DealerSnapshotPacket.STREAM_CODEC, DealerSnapshotPacket::handle);
        registrar.playToServer(DealerActionPacket.TYPE, DealerActionPacket.STREAM_CODEC, DealerActionPacket::handle);
        registrar.playToClient(CustomerSnapshotPacket.TYPE, CustomerSnapshotPacket.STREAM_CODEC, CustomerSnapshotPacket::handle);
        registrar.playToServer(CustomerActionPacket.TYPE, CustomerActionPacket.STREAM_CODEC, CustomerActionPacket::handle);
        registrar.playToClient(TavernAmbiencePacket.TYPE, TavernAmbiencePacket.STREAM_CODEC, TavernAmbiencePacket::handle);
    }

    public static void sendSnapshot(ServerPlayer player, PsychoactiveExperience experience) {
        PacketDistributor.sendToPlayer(player, ExperienceSnapshotPacket.from(experience));
    }

    public static void sendClear(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, ExperienceSnapshotPacket.clear());
    }

    public static void sendAlcoholDeathPresentation(ServerPlayer player, boolean blackoutWakeUp) {
        PacketDistributor.sendToPlayer(player,
                blackoutWakeUp ? AlcoholDeathPresentationPacket.blackout() : AlcoholDeathPresentationPacket.clear());
    }

    public static void sendScrub(int newAgeTicks) {
        PacketDistributor.sendToServer(new TripScrubPacket(newAgeTicks));
    }

    public static void sendCursedFindings(ServerPlayer player, Set<String> unlockedFindings, boolean openJournal) {
        PacketDistributor.sendToPlayer(player, new CursedFindingsSyncPacket(unlockedFindings, openJournal));
    }

    public static void sendMarketSnapshot(ServerPlayer player, MarketSnapshotPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendMarketAction(MarketActionPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendDealerSnapshot(ServerPlayer player, DealerSnapshotPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendDealerAction(DealerActionPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendCustomerSnapshot(ServerPlayer player, CustomerSnapshotPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendCustomerAction(CustomerActionPacket packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendTavernAmbience(ServerPlayer player, TavernAmbiencePacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
