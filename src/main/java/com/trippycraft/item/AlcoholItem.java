package com.trippycraft.item;

import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.alcohol.AlcoholDoseCurve;
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

public final class AlcoholItem extends Item {
    private final double servingOunces;
    private final double alcoholByVolume;
    private final double standardDrinks;

    public AlcoholItem(double servingOunces, double alcoholByVolume, Properties properties) {
        super(properties);
        this.servingOunces = servingOunces;
        this.alcoholByVolume = alcoholByVolume;
        this.standardDrinks = AlcoholDoseCurve.standardDrinks(servingOunces, alcoholByVolume);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addAlcoholDose(serverPlayer, standardDrinks);
            ContainerReturnItemSupport.consumeAndReturnRemainder(player, hand, stack);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.alcohol_abv", String.format(Locale.ROOT, "%.0f%%", alcoholByVolume * 100.0D)));
        tooltip.add(Component.translatable("tooltip.trippycraft.alcohol_serving", String.format(Locale.ROOT, "%.1f fl oz", servingOunces)));
        tooltip.add(Component.translatable("tooltip.trippycraft.alcohol_standard_drinks", String.format(Locale.ROOT, "%.2f", standardDrinks)));
        tooltip.add(Component.translatable("tooltip.trippycraft.alcohol_stacks"));
    }

    public double servingOunces() {
        return servingOunces;
    }

    public double alcoholByVolume() {
        return alcoholByVolume;
    }

    public double standardDrinks() {
        return standardDrinks;
    }
}
