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

public final class LsdBlotterItem extends Item {
    private final double doseMicrograms;

    public LsdBlotterItem(double doseMicrograms, Properties properties) {
        super(properties);
        this.doseMicrograms = doseMicrograms;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addLsdDose(serverPlayer, doseMicrograms);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.lsd_gameplay_micrograms", String.format(Locale.ROOT, "%.0fug", doseMicrograms)));
        tooltip.add(Component.translatable("tooltip.trippycraft.lsd_stacks"));
    }

    public double doseMicrograms() {
        return doseMicrograms;
    }
}
