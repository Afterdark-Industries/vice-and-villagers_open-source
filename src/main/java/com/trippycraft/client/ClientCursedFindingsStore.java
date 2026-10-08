package com.trippycraft.client;

import com.trippycraft.network.CursedFindingsSyncPacket;
import net.minecraft.client.Minecraft;

import java.util.Set;

public final class ClientCursedFindingsStore {
    private static Set<String> unlockedFindings = Set.of();

    private ClientCursedFindingsStore() {
    }

    public static void accept(CursedFindingsSyncPacket packet) {
        unlockedFindings = Set.copyOf(packet.unlockedFindings());
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.openJournal()) {
            minecraft.setScreen(new CursedFindingsScreen(unlockedFindings));
        } else if (minecraft.screen instanceof CursedFindingsScreen screen) {
            screen.updateUnlockedFindings(unlockedFindings);
        }
    }

    public static boolean isUnlocked(String findingId) {
        return unlockedFindings.contains(findingId);
    }

    public static Set<String> unlockedFindings() {
        return unlockedFindings;
    }
}
