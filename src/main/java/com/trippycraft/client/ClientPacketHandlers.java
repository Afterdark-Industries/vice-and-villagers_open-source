package com.trippycraft.client;

import com.trippycraft.network.AlcoholDeathPresentationPacket;
import com.trippycraft.network.CursedFindingsSyncPacket;
import com.trippycraft.network.CustomerSnapshotPacket;
import com.trippycraft.network.DealerSnapshotPacket;
import com.trippycraft.network.ExperienceSnapshotPacket;
import com.trippycraft.network.MarketSnapshotPacket;
import com.trippycraft.network.TavernAmbiencePacket;
import net.minecraft.client.Minecraft;

public final class ClientPacketHandlers {
    private ClientPacketHandlers() {
    }

    public static void handleExperienceSnapshot(ExperienceSnapshotPacket packet) {
        ClientExperienceStore.accept(packet);
    }

    public static void handleAlcoholDeathPresentation(AlcoholDeathPresentationPacket packet) {
        ClientAlcoholDeathPresentation.setBlackoutWakeUp(packet.blackoutWakeUp());
    }

    public static void handleCursedFindings(CursedFindingsSyncPacket packet) {
        ClientCursedFindingsStore.accept(packet);
    }

    public static void handleMarketSnapshot(MarketSnapshotPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof VillageMarketScreen marketScreen) {
            marketScreen.updateSnapshot(packet);
            return;
        }
        if (packet.openScreen()) {
            minecraft.setScreen(new VillageMarketScreen(packet));
        }
    }

    public static void handleDealerSnapshot(DealerSnapshotPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DealerScreen dealerScreen) {
            dealerScreen.updateSnapshot(packet);
            return;
        }
        if (packet.openScreen()) {
            minecraft.setScreen(new DealerScreen(packet));
        }
    }

    public static void handleCustomerSnapshot(CustomerSnapshotPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CustomerDemandScreen customerScreen) {
            customerScreen.updateSnapshot(packet);
            return;
        }
        if (packet.openScreen()) {
            minecraft.setScreen(new CustomerDemandScreen(packet));
        }
    }

    public static void handleTavernAmbience(TavernAmbiencePacket packet) {
        ClientTavernAmbience.accept(packet);
    }
}
