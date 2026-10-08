package com.trippycraft.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EmeraldPayoutLedger {
    private static final Map<UUID, Integer> PENDING_PAYOUTS = new HashMap<>();

    private EmeraldPayoutLedger() {
    }

    public static void queue(ServerPlayer player, int emeralds) {
        if (player == null || emeralds <= 0) {
            return;
        }
        UUID playerId = player.getUUID();
        PENDING_PAYOUTS.put(playerId, safeAdd(PENDING_PAYOUTS.getOrDefault(playerId, 0), emeralds));
    }

    public static void flush(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Integer payout = PENDING_PAYOUTS.remove(player.getUUID());
        if (payout == null || payout <= 0) {
            return;
        }
        EmeraldWallet.pay(player, payout);
        player.displayClientMessage(Component.translatable("message.trippycraft.sales.payout", payout), true);
    }

    public static void clearAll() {
        PENDING_PAYOUTS.clear();
    }

    private static int safeAdd(int left, int right) {
        return left >= Integer.MAX_VALUE - right ? Integer.MAX_VALUE : left + right;
    }
}
