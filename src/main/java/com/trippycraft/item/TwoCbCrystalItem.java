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

/** Five saved 1000mg servings; the mortar packages only the crystal's remaining content. */
public final class TwoCbCrystalItem extends Item {
    public static final int TOTAL_MILLIGRAMS = 5_000;
    public static final int MILLIGRAMS_PER_USE = 1_000;
    public static final int POWDER_MILLIGRAMS = 100;
    private static final String REMAINING_KEY = "TrippyCraftTwoCbCrystalRemaining";

    public TwoCbCrystalItem(Properties properties) {
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
            SubstanceEngine.addTwoCbDose(serverPlayer, MILLIGRAMS_PER_USE);
            if (!player.getAbilities().instabuild) {
                consumeMilligrams(stack, MILLIGRAMS_PER_USE);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return remainingMilligrams(stack) < TOTAL_MILLIGRAMS;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * remainingMilligrams(stack) / TOTAL_MILLIGRAMS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF815A;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.two_cb_crystal_remaining",
                remainingMilligrams(stack), TOTAL_MILLIGRAMS));
        tooltip.add(Component.translatable("tooltip.trippycraft.two_cb_crystal_use", MILLIGRAMS_PER_USE));
        tooltip.add(Component.translatable("tooltip.trippycraft.two_cb_stacks"));
    }

    public static int remainingMilligrams(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TwoCbCrystalItem)) {
            return 0;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int remaining = NbtCompat.getIntOr(customData.copyTag(), REMAINING_KEY, TOTAL_MILLIGRAMS);
        return Math.max(0, Math.min(TOTAL_MILLIGRAMS, remaining));
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
