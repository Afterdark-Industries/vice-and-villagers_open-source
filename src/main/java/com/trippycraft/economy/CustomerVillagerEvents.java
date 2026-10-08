package com.trippycraft.economy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CustomerVillagerEvents {
    private CustomerVillagerEvents() {
    }

    public static boolean onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        if (!(event.getTarget() instanceof Villager villager)) {
            return false;
        }
        if (!CustomerService.isCustomerVillager(villager)) {
            return false;
        }

        event.setCancellationResult(InteractionResult.CONSUME);
        CustomerService.openCustomer(player, villager);
        return true;
    }
}
