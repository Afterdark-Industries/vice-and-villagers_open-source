package com.trippycraft.item;

import com.trippycraft.substance.SubstanceEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A two-use solvent bottle; each fictional half-bottle contributes ten alcohol drink units. */
public final class EthanolItem extends Item {
    public static final int TOTAL_USES = 2;
    public static final double STANDARD_DRINKS_PER_USE = 10.0D;

    public EthanolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addAlcoholDose(serverPlayer, STANDARD_DRINKS_PER_USE);
            if (!serverPlayer.getAbilities().instabuild) {
                int usesBefore = StackableDoseItemSupport.remainingUses(stack, TOTAL_USES);
                StackableDoseItemSupport.consumeUse(serverPlayer, hand, TOTAL_USES);
                if (usesBefore <= 1) {
                    returnEmptyBottle(serverPlayer, hand);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private static void returnEmptyBottle(Player player, InteractionHand hand) {
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, bottle);
        } else if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return StackableDoseItemSupport.isBarVisible(stack, TOTAL_USES);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return StackableDoseItemSupport.getBarWidth(stack, TOTAL_USES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return StackableDoseItemSupport.getBarColor(stack, TOTAL_USES);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.ethanol"));
        tooltip.add(Component.translatable("tooltip.trippycraft.ethanol_half_drinks"));
        tooltip.add(Component.translatable("tooltip.trippycraft.ethanol_total_drinks"));
        tooltip.add(Component.translatable("tooltip.trippycraft.ethanol_remaining_halves",
                StackableDoseItemSupport.remainingUses(stack, TOTAL_USES), TOTAL_USES));
    }
}
