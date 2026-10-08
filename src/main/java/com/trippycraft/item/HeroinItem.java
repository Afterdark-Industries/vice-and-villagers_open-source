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

import java.util.Locale;

import java.util.List;

public final class HeroinItem extends Item {
    private final double doseMilligrams;

    public HeroinItem(double doseMilligrams, Properties properties) {
        super(properties);
        this.doseMilligrams = doseMilligrams;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addHeroinDose(serverPlayer, doseMilligrams);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.heroin_gameplay_mg", String.format(Locale.ROOT, "%.0fmg", doseMilligrams)));
        tooltip.add(Component.translatable("tooltip.trippycraft.heroin_stacks"));
    }

    public double doseMilligrams() {
        return doseMilligrams;
    }
}
