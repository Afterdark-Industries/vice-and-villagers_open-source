package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import com.trippycraft.crafting.CapsuleBlendRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, TrippyCraft.MOD_ID);

    public static final Supplier<RecipeSerializer<CapsuleBlendRecipe>> CAPSULE_BLEND =
            RECIPE_SERIALIZERS.register("capsule_blend", () -> new SimpleCraftingRecipeSerializer<>(CapsuleBlendRecipe::new));

    private ModRecipeSerializers() {
    }
}
