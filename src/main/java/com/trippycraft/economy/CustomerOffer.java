package com.trippycraft.economy;

import net.minecraft.world.item.ItemStack;

public record CustomerOffer(
        String id,
        String productId,
        String requestType,
        int quantity,
        int rewardEmeralds,
        int reputationReward,
        int unitPrice,
        int habitLevel
) {
    public ItemStack stack() {
        return DealerRegistries.product(productId)
                .map(product -> product.stack(1))
                .orElse(ItemStack.EMPTY);
    }
}
