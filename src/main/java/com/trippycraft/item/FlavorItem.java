package com.trippycraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;


import java.util.List;

public final class FlavorItem extends Item {
    private final String flavorTooltipKey;

    public FlavorItem(String flavorTooltipKey, Properties properties) {
        super(properties);
        this.flavorTooltipKey = flavorTooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(flavorTooltipKey));
    }
}
