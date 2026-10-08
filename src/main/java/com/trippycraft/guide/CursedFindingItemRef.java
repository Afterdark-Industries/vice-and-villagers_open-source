package com.trippycraft.guide;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

public record CursedFindingItemRef(Supplier<? extends ItemLike> item, String label) {
    public ItemStack stack() {
        return new ItemStack(item.get());
    }
}
