package com.trippycraft.economy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public record DealerProduct(
        String id,
        Supplier<Item> item,
        String category,
        int emeraldPricePerUnit,
        int unlockLevel,
        CustomerAddictionProfile addictionProfile
) {
    public ItemStack stack(int count) {
        return new ItemStack(item.get(), Math.max(1, count));
    }
}
