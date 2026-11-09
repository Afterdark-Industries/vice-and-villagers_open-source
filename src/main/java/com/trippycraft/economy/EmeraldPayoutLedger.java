package com.trippycraft.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Defers broker and customer payouts until the trading screen closes, the market reopens or the player
 * logs out. The queued amount lives in the player's {@link MarketPlayerData}, so it is saved alongside the
 * inventory the sold items left and survives a crash or forced stop.
 */
public final class EmeraldPayoutLedger {
    private EmeraldPayoutLedger() {
    }

    public static void queue(ServerPlayer player, int emeralds) {
        if (player == null || emeralds <= 0) {
            return;
        }
        MarketDataStore.getOrCreate(player).addPendingPayout(emeralds);
    }

    public static void flush(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int payout = MarketDataStore.get(player.getUUID()).map(MarketPlayerData::takePendingPayout).orElse(0);
        if (payout <= 0) {
            return;
        }
        EmeraldWallet.pay(player, payout);
        player.displayClientMessage(Component.translatable("message.trippycraft.sales.payout", payout), true);
    }
}
