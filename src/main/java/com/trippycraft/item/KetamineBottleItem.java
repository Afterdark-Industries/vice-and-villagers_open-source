package com.trippycraft.item;

import com.trippycraft.substance.SubstanceEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;


import java.util.List;

public final class KetamineBottleItem extends Item {
    private final int totalMilligrams;
    private final int milligramsPerUse;
    private final int totalUses;

    public KetamineBottleItem(int totalMilligrams, int milligramsPerUse, Properties properties) {
        super(properties);
        this.totalMilligrams = totalMilligrams;
        this.milligramsPerUse = milligramsPerUse;
        this.totalUses = Math.max(1, totalMilligrams / Math.max(1, milligramsPerUse));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addKetamineDose(serverPlayer, milligramsPerUse);
            ContainerReturnItemSupport.consumeAndReturnRemainder(player, hand, stack);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return StackableDoseItemSupport.isBarVisible(stack, totalUses);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return StackableDoseItemSupport.getBarWidth(stack, totalUses);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return StackableDoseItemSupport.getBarColor(stack, totalUses);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.ketamine_total_mg", totalMilligrams));
        tooltip.add(Component.translatable("tooltip.trippycraft.ketamine_hit_mg", milligramsPerUse));
        tooltip.add(Component.translatable("tooltip.trippycraft.ketamine_single_use"));
    }

    public int totalMilligrams() {
        return totalMilligrams;
    }

    public int milligramsPerUse() {
        return milligramsPerUse;
    }
}
