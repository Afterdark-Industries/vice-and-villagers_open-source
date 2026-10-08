package com.trippycraft.substance.heroin;

import com.trippycraft.TrippyCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class HeroinDamageTypes {
    public static final ResourceKey<DamageType> OVERDOSE = key("heroin_overdose");
    public static final ResourceKey<DamageType> WITHDRAWAL = key("opioid_withdrawal");

    private HeroinDamageTypes() {
    }

    public static DamageSource overdose(ServerPlayer player) {
        return new DamageSource(holder(player, OVERDOSE));
    }

    public static DamageSource withdrawal(ServerPlayer player) {
        return new DamageSource(holder(player, WITHDRAWAL));
    }

    private static Holder<DamageType> holder(ServerPlayer player, ResourceKey<DamageType> key) {
        return player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, path));
    }
}
