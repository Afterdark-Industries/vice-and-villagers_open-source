package com.trippycraft.economy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class DealerVillagerEvents {
    private DealerVillagerEvents() {
    }

    public static boolean onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        if (!(event.getTarget() instanceof Villager villager)) {
            return false;
        }
        if (!DealerService.isDealerVillager(villager)) {
            return false;
        }

        event.setCancellationResult(InteractionResult.CONSUME);
        event.setCanceled(true);
        DealerService.openDealer(player);
        return true;
    }
}
