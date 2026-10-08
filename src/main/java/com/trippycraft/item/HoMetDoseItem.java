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
import java.util.Locale;

/** Bottled content and effect strength are separate so changing formats preserves existing balance. */
public final class HoMetDoseItem extends Item {
    private final int amountMilligrams;
    private final double doseMilligrams;

    public HoMetDoseItem(int amountMilligrams, double doseMilligrams, Properties properties) {
        super(properties);
        this.amountMilligrams = amountMilligrams;
        this.doseMilligrams = doseMilligrams;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addHoMetDose(serverPlayer, doseMilligrams, amountMilligrams);
            ContainerReturnItemSupport.consumeAndReturnRemainder(player, hand, stack);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.four_ho_met_gameplay_mg", String.format(Locale.ROOT, "%dmg", amountMilligrams)));
        tooltip.add(Component.translatable("tooltip.trippycraft.four_ho_met_stacks"));
    }

    public double doseMilligrams() {
        return doseMilligrams;
    }

    public int amountMilligrams() {
        return amountMilligrams;
    }
}
