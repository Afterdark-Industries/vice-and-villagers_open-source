package com.trippycraft.economy;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;

public record MarketContract(
        String id,
        String productId,
        String type,
        int quantity,
        int rewardEmeralds,
        int reputationReward,
        int bonusMultiplier
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("productId", productId);
        tag.putString("type", type);
        tag.putInt("quantity", quantity);
        tag.putInt("rewardEmeralds", rewardEmeralds);
        tag.putInt("reputationReward", reputationReward);
        tag.putInt("bonusMultiplier", bonusMultiplier);
        return tag;
    }

    public static MarketContract load(CompoundTag tag) {
        return new MarketContract(
                NbtCompat.getStringOr(tag, "id", ""),
                NbtCompat.getStringOr(tag, "productId", ""),
                NbtCompat.getStringOr(tag, "type", "Order"),
                Math.max(1, NbtCompat.getIntOr(tag, "quantity", 8)),
                Math.max(1, NbtCompat.getIntOr(tag, "rewardEmeralds", 1)),
                Math.max(1, NbtCompat.getIntOr(tag, "reputationReward", 1)),
                Math.max(100, NbtCompat.getIntOr(tag, "bonusMultiplier", 135))
        );
    }
}
