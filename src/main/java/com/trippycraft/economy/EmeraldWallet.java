package com.trippycraft.economy;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EmeraldWallet {
    public static final int EMERALD_BLOCK_VALUE = 10;

    private EmeraldWallet() {
    }

    public static int value(Player player) {
        return player == null ? 0 : value(player.getInventory());
    }

    public static int value(Inventory inventory) {
        int emeralds = 0;
        int blocks = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.EMERALD)) {
                emeralds += stack.getCount();
            } else if (stack.is(Items.EMERALD_BLOCK)) {
                blocks += stack.getCount();
            }
        }
        return emeralds + blocks * EMERALD_BLOCK_VALUE;
    }

    public static boolean spend(Player player, int emeraldCost) {
        if (emeraldCost <= 0) {
            return true;
        }
        if (value(player) < emeraldCost) {
            return false;
        }

        Inventory inventory = player.getInventory();
        int remaining = emeraldCost - removeItem(inventory, Items.EMERALD, emeraldCost);
        int change = 0;
        if (remaining > 0) {
            int blocksNeeded = (remaining + EMERALD_BLOCK_VALUE - 1) / EMERALD_BLOCK_VALUE;
            int removedBlocks = removeItem(inventory, Items.EMERALD_BLOCK, blocksNeeded);
            change = removedBlocks * EMERALD_BLOCK_VALUE - remaining;
        }

        if (change > 0) {
            giveItem(player, new ItemStack(Items.EMERALD, change));
        }
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    public static void pay(Player player, int emeraldValue) {
        if (player == null || emeraldValue <= 0) {
            return;
        }

        int blocks = emeraldValue / EMERALD_BLOCK_VALUE;
        int looseEmeralds = emeraldValue % EMERALD_BLOCK_VALUE;
        if (blocks > 0) {
            giveItem(player, new ItemStack(Items.EMERALD_BLOCK, blocks));
        }
        if (looseEmeralds > 0) {
            giveItem(player, new ItemStack(Items.EMERALD, looseEmeralds));
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private static int removeItem(Inventory inventory, Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        return amount - remaining;
    }

    private static void giveItem(Player player, ItemStack stack) {
        while (!stack.isEmpty()) {
            int count = Math.min(stack.getCount(), stack.getMaxStackSize());
            ItemStack split = stack.copyWithCount(count);
            stack.shrink(count);
            if (!player.addItem(split)) {
                player.drop(split, false);
            }
        }
    }
}
