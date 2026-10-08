package com.trippycraft.crafting;

import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.registry.ModItems;
import com.trippycraft.registry.ModRecipeSerializers;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CapsuleBlendRecipe extends CustomRecipe {
    public CapsuleBlendRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return collectEntries(input).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return collectEntries(input)
                .map(CapsuleBlend::createStack)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public RecipeSerializer<CapsuleBlendRecipe> getSerializer() {
        return ModRecipeSerializers.CAPSULE_BLEND.get();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    private static Optional<List<CapsuleBlend.Entry>> collectEntries(CraftingInput input) {
        int capsuleCount = 0;
        List<CapsuleBlend.Entry> entries = new ArrayList<>();

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.EMPTY_CAPSULE.get())) {
                capsuleCount++;
                if (capsuleCount > 1) {
                    return Optional.empty();
                }
                continue;
            }

            Optional<CapsuleBlend.Entry> entry = CapsuleBlend.entryFromDoseItem(stack);
            if (entry.isEmpty()) {
                return Optional.empty();
            }
            entries.add(entry.get());
        }

        return capsuleCount == 1 && !entries.isEmpty() ? Optional.of(entries) : Optional.empty();
    }
}
