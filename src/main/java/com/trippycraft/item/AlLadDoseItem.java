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

public final class AlLadDoseItem extends Item {
    private final int amountMilligrams;
    private final double doseMicrograms;

    public AlLadDoseItem(int amountMilligrams, double doseMicrograms, Properties properties) {
        super(properties);
        this.amountMilligrams = amountMilligrams;
        this.doseMicrograms = doseMicrograms;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addAlLadDose(serverPlayer, doseMicrograms, amountMilligrams);
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.al_lad_gameplay_mg",
                String.format(Locale.ROOT, "%dmg", amountMilligrams)));
        tooltip.add(Component.translatable("tooltip.trippycraft.al_lad_stacks"));
    }

    public double doseMicrograms() {
        return doseMicrograms;
    }

    public int amountMilligrams() {
        return amountMilligrams;
    }
}
