package com.trippycraft.economy;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class MarketEvents {
    private MarketEvents() {
    }

    public static void onPlayerLoadFromFile(PlayerEvent.LoadFromFile event) {
        MarketDataStore.load(event.getEntity().getUUID(), event.getPlayerFile(MarketDataStore.PLAYER_DATA_SUFFIX));
    }

    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        MarketDataStore.save(event.getEntity().getUUID(), event.getPlayerFile(MarketDataStore.PLAYER_DATA_SUFFIX));
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        MarketDataStore.clone(event.getOriginal().getUUID(), event.getEntity().getUUID());
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            EmeraldPayoutLedger.flush(serverPlayer);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        EmeraldPayoutLedger.clearAll();
        MarketDataStore.clearAll();
    }
}
