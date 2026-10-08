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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/** A five-serving crystal whose saved content is shared with the mortar's powder yield. */
public final class HoMetCrystalItem extends Item {
    public static final int CAPACITY_MILLIGRAMS = 5_000;
    public static final int MILLIGRAMS_PER_USE = 1_000;
    public static final int POWDER_MILLIGRAMS = 100;
    // Extrapolate from the preserved 500mg bottle tier: two bottles contribute 40 + 40.
    public static final double EFFECT_DOSE_PER_USE = 80.0D;
    private static final String REMAINING_KEY = "TrippyCraftHoMetCrystalRemaining";

    public HoMetCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getCount() != 1 || remainingMilligrams(stack) < MILLIGRAMS_PER_USE) {
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addHoMetDose(serverPlayer, EFFECT_DOSE_PER_USE, MILLIGRAMS_PER_USE);
            if (!player.getAbilities().instabuild) {
                consumeMilligrams(stack, MILLIGRAMS_PER_USE);
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
        return 0xA5F51B;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.four_ho_met_crystal_remaining",
                remainingMilligrams(stack), CAPACITY_MILLIGRAMS));
        tooltip.add(Component.translatable("tooltip.trippycraft.four_ho_met_crystal_use", MILLIGRAMS_PER_USE));
        tooltip.add(Component.translatable("tooltip.trippycraft.four_ho_met_stacks"));
    }

    public static int remainingMilligrams(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof HoMetCrystalItem)) {
            return 0;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int remaining = NbtCompat.getIntOr(customData.copyTag(), REMAINING_KEY, CAPACITY_MILLIGRAMS);
        return Math.max(0, Math.min(CAPACITY_MILLIGRAMS, remaining));
    }

    public static boolean consumeMilligrams(ItemStack stack, int milligrams) {
        if (milligrams <= 0 || milligrams % MILLIGRAMS_PER_USE != 0
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
