package com.trippycraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/** Stable crystal strengths for processing; grind into powder before using the contents. */
public final class DmtItem extends Item {
    private final double doseUnits;

    public DmtItem(double doseUnits, Properties properties) {
        super(properties);
        this.doseUnits = doseUnits;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.dmt_gameplay_units", String.format(Locale.ROOT, "%.1fx", doseUnits)));
        tooltip.add(Component.translatable("tooltip.trippycraft.dmt_crystal_processing"));
    }

    public double doseUnits() {
        return doseUnits;
    }
}
