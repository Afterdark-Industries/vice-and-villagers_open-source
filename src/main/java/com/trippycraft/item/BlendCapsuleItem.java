package com.trippycraft.item;

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

public final class BlendCapsuleItem extends Item {
    public BlendCapsuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && CapsuleBlend.hasBlend(stack)) {
            int applied = CapsuleBlend.apply(stack, serverPlayer);
            if (applied > 0) {
                stack.consume(1, player);
                player.displayClientMessage(Component.translatable("message.trippycraft.blend_capsule", applied), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<CapsuleBlend.Entry> entries = CapsuleBlend.read(stack);
        tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
        if (entries.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.trippycraft.blend_capsule_empty"));
            return;
        }

        tooltip.add(Component.translatable("tooltip.trippycraft.blend_capsule_contents"));
        for (CapsuleBlend.Entry entry : entries) {
            tooltip.add(Component.translatable(
                    "tooltip.trippycraft.blend_capsule_entry",
                    CapsuleBlend.displayName(entry),
                    CapsuleBlend.doseLabel(entry)));
        }
        tooltip.add(Component.translatable("tooltip.trippycraft.blend_capsule_stacks"));
    }
}
