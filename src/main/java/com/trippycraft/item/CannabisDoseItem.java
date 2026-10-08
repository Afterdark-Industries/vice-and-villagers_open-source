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

public final class CannabisDoseItem extends Item {
    private final int totalThcMilligrams;
    private final int thcMilligramsPerHit;
    private final String perUseTooltipKey;
    private final String stackingTooltipKey;
    private final int totalUses;

    public CannabisDoseItem(int totalThcMilligrams, int thcMilligramsPerHit, Properties properties) {
        this(totalThcMilligrams, thcMilligramsPerHit,
                "tooltip.trippycraft.cannabis_hit_mg",
                "tooltip.trippycraft.cannabis_stacks",
                properties);
    }

    public CannabisDoseItem(int totalThcMilligrams, int thcMilligramsPerHit, String perUseTooltipKey, String stackingTooltipKey, Properties properties) {
        super(properties);
        this.totalThcMilligrams = totalThcMilligrams;
        this.thcMilligramsPerHit = thcMilligramsPerHit;
        this.perUseTooltipKey = perUseTooltipKey;
        this.stackingTooltipKey = stackingTooltipKey;
        this.totalUses = Math.max(1, totalThcMilligrams / Math.max(1, thcMilligramsPerHit));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addCannabisDose(serverPlayer, thcMilligramsPerHit);
            StackableDoseItemSupport.consumeUse(serverPlayer, hand, totalUses);
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
        tooltip.add(Component.translatable("tooltip.trippycraft.cannabis_total_mg", totalThcMilligrams));
        tooltip.add(Component.translatable(perUseTooltipKey, thcMilligramsPerHit));
        tooltip.add(Component.translatable(stackingTooltipKey));
    }

    public int totalThcMilligrams() {
        return totalThcMilligrams;
    }

    public int thcMilligramsPerHit() {
        return thcMilligramsPerHit;
    }
}