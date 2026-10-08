package com.trippycraft.substance.mdma;

import com.trippycraft.TrippyCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class MdmaDamageTypes {
    public static final ResourceKey<DamageType> HEART_ATTACK = key("mdma_heart_attack");

    private MdmaDamageTypes() {
    }

    public static DamageSource heartAttack(ServerPlayer player) {
        return new DamageSource(holder(player, HEART_ATTACK));
    }

    private static Holder<DamageType> holder(ServerPlayer player, ResourceKey<DamageType> key) {
        return player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, path));
    }
}
