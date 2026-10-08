package com.trippycraft.item;

import com.trippycraft.substance.salvia.SalviaDoseCurve;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;


import java.util.List;

public final class SalviaLeafItem extends Item {
    private final double doseUnits;

    public SalviaLeafItem(double doseUnits, Properties properties) {
        super(properties);
        this.doseUnits = doseUnits;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.salvia_gameplay_units", SalviaDoseCurve.formatPotencyLabel(doseUnits)));
        tooltip.add(Component.translatable("tooltip.trippycraft.salvia_load_bong"));
        tooltip.add(Component.translatable("tooltip.trippycraft.salvia_stacks"));
    }

    public double doseUnits() {
        return doseUnits;
    }
}
