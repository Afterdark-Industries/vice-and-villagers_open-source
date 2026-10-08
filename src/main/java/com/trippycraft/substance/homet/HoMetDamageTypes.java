package com.trippycraft.substance.homet;

import com.trippycraft.TrippyCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

public final class HoMetDamageTypes {
    public static final ResourceKey<DamageType> OVERDOSE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "four_ho_met_overdose"));

    private HoMetDamageTypes() { }

    public static DamageSource overdose(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(OVERDOSE));
    }
}
