package com.trippycraft.item;

import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.SubstanceProfile;
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

public class PsychoactiveItem extends Item {
    private final SubstanceProfile profile;

    public PsychoactiveItem(SubstanceProfile profile, Properties properties) {
        super(properties);
        this.profile = profile;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCooldowns().addCooldown(this, 15);
            SubstanceEngine.addExposure(serverPlayer, profile, profile.defaultAbstractDoseUnits());
            stack.consume(1, player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        tooltip.add(Component.translatable("tooltip.trippycraft.family", Component.translatable(profile.family().translationKey())));
    }

    public SubstanceProfile profile() {
        return profile;
    }
}
