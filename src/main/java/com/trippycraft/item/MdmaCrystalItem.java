package com.trippycraft.item;

import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.compat.NbtCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** A persistent 5,000mg fictional gameplay reservoir used by both MDMA packaging machines. */
public final class MdmaCrystalItem extends Item {
    public static final int CAPACITY_MILLIGRAMS = 5_000;
    private static final String REMAINING_MILLIGRAMS_KEY = "TrippyCraftMdmaCrystalRemaining";
    private static final String PARTIAL_STACK_ID_KEY = "TrippyCraftMdmaCrystalId";

    public MdmaCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int remaining = remainingMilligrams(stack);
            if (remaining > 0) {
                serverPlayer.getCooldowns().addCooldown(this, 15);
                SubstanceEngine.addMdmaDose(serverPlayer, remaining);
                if (!serverPlayer.getAbilities().instabuild) {
                    stack.shrink(1);
                    clearPartialState(stack);
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
        return Math.max(1, Math.round(13.0F * remainingMilligrams(stack) / CAPACITY_MILLIGRAMS));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float ratio = remainingMilligrams(stack) / (float) CAPACITY_MILLIGRAMS;
        return Mth.hsvToRgb(0.82F - ratio * 0.34F, 0.72F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.mdma_crystal_remaining",
                remainingMilligrams(stack), CAPACITY_MILLIGRAMS));
        tooltip.add(Component.translatable("tooltip.trippycraft.mdma_crystal_use"));
    }

    public static int remainingMilligrams(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MdmaCrystalItem)) {
            return 0;
        }
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty() || !customData.contains(REMAINING_MILLIGRAMS_KEY)) {
            int legacyDamage = Math.max(0, stack.getOrDefault(DataComponents.DAMAGE, 0));
            return Math.max(0, CAPACITY_MILLIGRAMS - Math.min(CAPACITY_MILLIGRAMS, legacyDamage));
        }
        CompoundTag tag = customData.copyTag();
        return Math.max(0, Math.min(CAPACITY_MILLIGRAMS,
                NbtCompat.getIntOr(tag, REMAINING_MILLIGRAMS_KEY, CAPACITY_MILLIGRAMS)));
    }

    public static boolean consumeMilligrams(ItemStack stack, int milligrams) {
        int amount = Math.max(0, milligrams);
        if (amount <= 0 || stack.getCount() != 1 || remainingMilligrams(stack) < amount) {
            return false;
        }
        int remaining = remainingMilligrams(stack) - amount;
        if (remaining <= 0) {
            stack.shrink(1);
        } else {
            setRemainingMilligrams(stack, remaining);
        }
        return true;
    }

    private static void setRemainingMilligrams(ItemStack stack, int remaining) {
        int clamped = Math.max(0, Math.min(CAPACITY_MILLIGRAMS, remaining));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (clamped >= CAPACITY_MILLIGRAMS) {
                tag.remove(REMAINING_MILLIGRAMS_KEY);
                tag.remove(PARTIAL_STACK_ID_KEY);
            } else {
                tag.putInt(REMAINING_MILLIGRAMS_KEY, clamped);
                if (!tag.contains(PARTIAL_STACK_ID_KEY)) {
                    tag.putString(PARTIAL_STACK_ID_KEY, UUID.randomUUID().toString());
                }
            }
        });
        stack.remove(DataComponents.DAMAGE);
    }

    private static void clearPartialState(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.remove(REMAINING_MILLIGRAMS_KEY);
            tag.remove(PARTIAL_STACK_ID_KEY);
        });
        stack.remove(DataComponents.DAMAGE);
    }
}
