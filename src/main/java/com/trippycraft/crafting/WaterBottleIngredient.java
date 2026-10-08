package com.trippycraft.crafting;

import com.mojang.serialization.MapCodec;
import com.trippycraft.registry.ModIngredientSerializers;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/** Matches only a water potion while keeping the exact potion stack visible in recipe viewers. */
public final class WaterBottleIngredient implements ICustomIngredient {
    public static final WaterBottleIngredient INSTANCE = new WaterBottleIngredient();
    public static final MapCodec<WaterBottleIngredient> CODEC = MapCodec.unit(INSTANCE);
    private static final ItemStack DISPLAY_STACK = PotionContents.createItemStack(Items.POTION, Potions.WATER);

    private WaterBottleIngredient() {
    }

    @Override
    public boolean test(ItemStack input) {
        if (!input.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.is(Potions.WATER);
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(DISPLAY_STACK.copy());
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ModIngredientSerializers.WATER_BOTTLE.get();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WaterBottleIngredient;
    }

    @Override
    public int hashCode() {
        return WaterBottleIngredient.class.hashCode();
    }
}
