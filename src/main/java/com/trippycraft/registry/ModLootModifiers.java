package com.trippycraft.registry;

import com.mojang.serialization.MapCodec;
import com.trippycraft.TrippyCraft;
import com.trippycraft.loot.AddSubstanceLootTableModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TrippyCraft.MOD_ID);

    public static final Supplier<MapCodec<? extends IGlobalLootModifier>> ADD_SUBSTANCE_LOOT_TABLE =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("add_substance_loot_table", () -> AddSubstanceLootTableModifier.CODEC);

    private ModLootModifiers() {
    }
}
