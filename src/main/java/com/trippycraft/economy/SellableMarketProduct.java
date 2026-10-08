package com.trippycraft.economy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public record SellableMarketProduct(
        String id,
        Supplier<Item> item,
        String category,
        int baseEmeraldValue,
        boolean contractEligible,
        int demandWeight,
        int reputationUnlockLevel
) {
    public ItemStack stack() {
        return new ItemStack(item.get());
    }
}
