package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import com.trippycraft.crafting.WaterBottleIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public final class ModIngredientSerializers {
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, TrippyCraft.MOD_ID);

    public static final Supplier<IngredientType<?>> WATER_BOTTLE =
            INGREDIENT_SERIALIZERS.register("water_bottle", () -> new IngredientType<>(WaterBottleIngredient.CODEC));

    private ModIngredientSerializers() {
    }
}
