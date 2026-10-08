package com.trippycraft.economy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class MarketVillagerEvents {
    private MarketVillagerEvents() {
    }

    public static boolean onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        if (!(event.getTarget() instanceof Villager villager)) {
            return false;
        }
        if (!MarketService.isMarketVillager(villager)) {
            return false;
        }

        event.setCancellationResult(InteractionResult.CONSUME);
        MarketService.openMarket(player);
        return true;
    }
}
