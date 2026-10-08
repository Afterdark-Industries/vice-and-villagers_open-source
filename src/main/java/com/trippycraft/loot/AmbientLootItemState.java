package com.trippycraft.loot;

import com.trippycraft.item.CannabisDoseItem;
import com.trippycraft.item.KetamineBottleItem;
import com.trippycraft.item.StackableDoseItemSupport;
import com.trippycraft.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AmbientLootItemState {
    private AmbientLootItemState() {
    }

    public static void apply(ItemStack stack, RandomSource random) {
        if (stack.isEmpty() || stack.getCount() != 1) {
            return;
        }

        Item item = stack.getItem();
        if (item instanceof CannabisDoseItem cannabisDoseItem) {
            applyCannabisState(stack, cannabisDoseItem, random);
            return;
        }

        if (item instanceof KetamineBottleItem ketamineBottleItem) {
            applyKetamineState(stack, ketamineBottleItem, random);
        }
    }

    private static void applyCannabisState(ItemStack stack, CannabisDoseItem item, RandomSource random) {
        int totalUses = Math.max(1, item.totalThcMilligrams() / Math.max(1, item.thcMilligramsPerHit()));
        if (totalUses <= 1) {
            return;
        }

        if (stack.is(ModItems.CANNABIS_JOINT.get())) {
            if (random.nextFloat() < 0.55F) {
                int remainingUses = random.nextIntBetweenInclusive(1, totalUses - 1);
                StackableDoseItemSupport.setRemainingUsesForLoot(stack, totalUses, remainingUses);
            }
            return;
        }

        if (stack.is(ModItems.CANNABIS_DAB_PEN.get())) {
            if (random.nextFloat() < 0.75F) {
                int remainingUses = random.nextIntBetweenInclusive(Math.max(2, totalUses / 4), totalUses - 1);
                StackableDoseItemSupport.setRemainingUsesForLoot(stack, totalUses, remainingUses);
            }
            return;
        }

        if (stack.is(ModItems.CANNABIS_GUMMIES.get()) || stack.is(ModItems.CANNABIS_CHOCOLATE_BAR.get())) {
            if (random.nextFloat() < 0.30F) {
                int remainingUses = Mth.clamp(totalUses - 1, 1, totalUses - 1);
                StackableDoseItemSupport.setRemainingUsesForLoot(stack, totalUses, remainingUses);
            }
        }
    }

    private static void applyKetamineState(ItemStack stack, KetamineBottleItem item, RandomSource random) {
        int totalUses = Math.max(1, item.totalMilligrams() / Math.max(1, item.milligramsPerUse()));
        if (totalUses <= 1) {
            return;
        }

        if (random.nextFloat() < 0.35F) {
            int remainingUses = random.nextIntBetweenInclusive(Math.max(2, totalUses / 3), totalUses - 1);
            StackableDoseItemSupport.setRemainingUsesForLoot(stack, totalUses, remainingUses);
        }
    }
}
