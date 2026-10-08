package com.trippycraft.item;

import com.trippycraft.compat.NbtCompat;
import com.trippycraft.substance.SubstanceEngine;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/** One persistent supply shared between drinking and coating blotter sheets. */
public final class AlLadConcentrateItem extends Item {
    public static final int CAPACITY_MILLIGRAMS = 5_000;
    public static final int USE_MILLIGRAMS = 1_000;
    // Two preserved 500mg tabs: 450 + 450 calibrated effect units.
    public static final double EFFECT_DOSE_PER_USE = 900.0D;
    private static final String REMAINING_KEY = "TrippyCraftAlLadConcentrateRemaining";

    public AlLadConcentrateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getCount() != 1 || remainingMilligrams(stack) < USE_MILLIGRAMS) {
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addAlLadDose(serverPlayer, EFFECT_DOSE_PER_USE, USE_MILLIGRAMS);
            if (!player.getAbilities().instabuild) {
                consumeMilligrams(stack, USE_MILLIGRAMS);
                if (stack.isEmpty()) {
                    player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return remainingMilligrams(stack) < CAPACITY_MILLIGRAMS;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * remainingMilligrams(stack) / CAPACITY_MILLIGRAMS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xEC19D7;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.al_lad_concentrate_remaining",
                remainingMilligrams(stack), CAPACITY_MILLIGRAMS));
        tooltip.add(Component.translatable("tooltip.trippycraft.al_lad_concentrate_use", USE_MILLIGRAMS));
        tooltip.add(Component.translatable("tooltip.trippycraft.al_lad_stacks"));
    }

    public static int remainingMilligrams(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AlLadConcentrateItem)) {
            return 0;
        }
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int remaining = NbtCompat.getIntOr(data.copyTag(), REMAINING_KEY, CAPACITY_MILLIGRAMS);
        return Math.max(0, Math.min(CAPACITY_MILLIGRAMS, remaining));
    }

    public static boolean consumeMilligrams(ItemStack stack, int milligrams) {
        if (milligrams <= 0 || milligrams % USE_MILLIGRAMS != 0
                || stack.getCount() != 1 || remainingMilligrams(stack) < milligrams) {
            return false;
        }
        int remaining = remainingMilligrams(stack) - milligrams;
        if (remaining == 0) {
            stack.shrink(1);
        } else {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(REMAINING_KEY, remaining));
        }
        return true;
    }
}
