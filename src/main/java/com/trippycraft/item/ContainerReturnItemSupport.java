package com.trippycraft.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ContainerReturnItemSupport {
    private ContainerReturnItemSupport() {
    }

    public static void consumeAndReturnRemainder(Player player, InteractionHand hand, ItemStack stack) {
        if (player.getAbilities().instabuild || stack.isEmpty()) {
            return;
        }

        ItemStack remainder = stack.getCraftingRemainingItem();
        if (remainder.isEmpty()) {
            stack.consume(1, player);
            return;
        }

        if (stack.getCount() == 1) {
            player.setItemInHand(hand, remainder.copy());
            return;
        }

        stack.shrink(1);
        if (!player.getInventory().add(remainder.copy())) {
            player.drop(remainder.copy(), false);
        }
    }
}
