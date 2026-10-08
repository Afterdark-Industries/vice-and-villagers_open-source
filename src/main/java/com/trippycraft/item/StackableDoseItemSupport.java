package com.trippycraft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public final class StackableDoseItemSupport {
    private static final String REMAINING_USES_KEY = "TrippyCraftRemainingUses";
    private static final String PARTIAL_STACK_ID_KEY = "TrippyCraftPartialStackId";

    private StackableDoseItemSupport() {
    }

    static void consumeUse(Player player, InteractionHand hand, int totalUses) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }

        if (stack.getCount() > 1) {
            ItemStack remainder = stack.copyWithCount(stack.getCount() - 1);
            ItemStack active = stack.copyWithCount(1);
            int remaining = remainingUses(active, totalUses) - 1;
            if (remaining > 0) {
                setRemainingUses(active, remaining, totalUses);
                player.setItemInHand(hand, active);
            } else {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            if (!remainder.isEmpty() && !player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
            return;
        }

        int remaining = remainingUses(stack, totalUses) - 1;
        if (remaining > 0) {
            setRemainingUses(stack, remaining, totalUses);
        } else {
            player.setItemInHand(hand, ItemStack.EMPTY);
        }
    }

    static int remainingUses(ItemStack stack, int totalUses) {
        if (stack.isEmpty() || totalUses <= 1) {
            return Math.max(0, totalUses);
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty() || !customData.contains(REMAINING_USES_KEY)) {
            return totalUses;
        }
        CompoundTag tag = customData.copyTag();
        return clampUses(com.trippycraft.compat.NbtCompat.getIntOr(tag, REMAINING_USES_KEY, totalUses), totalUses);
    }

    static boolean isBarVisible(ItemStack stack, int totalUses) {
        return totalUses > 1 && remainingUses(stack, totalUses) < totalUses;
    }

    static int getBarWidth(ItemStack stack, int totalUses) {
        if (totalUses <= 0) {
            return 0;
        }
        return Math.max(1, Math.round(13.0F * remainingUses(stack, totalUses) / (float) totalUses));
    }

    static int getBarColor(ItemStack stack, int totalUses) {
        if (totalUses <= 0) {
            return 0;
        }
        float ratio = remainingUses(stack, totalUses) / (float) totalUses;
        return Mth.hsvToRgb(Math.max(0.0F, ratio / 3.0F), 1.0F, 1.0F);
    }

    private static void setRemainingUses(ItemStack stack, int remaining, int totalUses) {
        int clamped = clampUses(remaining, totalUses);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (clamped >= totalUses) {
                tag.remove(REMAINING_USES_KEY);
                tag.remove(PARTIAL_STACK_ID_KEY);
            } else {
                tag.putInt(REMAINING_USES_KEY, clamped);
                if (!tag.contains(PARTIAL_STACK_ID_KEY)) {
                    tag.putString(PARTIAL_STACK_ID_KEY, UUID.randomUUID().toString());
                }
            }
        });
    }

    public static void setRemainingUsesForLoot(ItemStack stack, int totalUses, int remainingUses) {
        if (stack.isEmpty() || totalUses <= 1) {
            return;
        }
        setRemainingUses(stack, remainingUses, totalUses);
    }

    private static int clampUses(int uses, int totalUses) {
        return Math.max(0, Math.min(totalUses, uses));
    }
}
