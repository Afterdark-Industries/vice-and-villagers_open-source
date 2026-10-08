package com.trippycraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;


import java.util.List;

public final class MortarAndPestleBlockItem extends BlockItem {
    private final String flavorTooltipKey;

    public MortarAndPestleBlockItem(Block block, String flavorTooltipKey, Properties properties) {
        super(block, properties);
        this.flavorTooltipKey = flavorTooltipKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(flavorTooltipKey));
    }
}
