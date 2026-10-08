package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.crafting.CapsuleBlendRecipe;
import com.trippycraft.economy.DealerRegistries;
import com.trippycraft.guide.CursedFindingIds;
import com.trippycraft.guide.CursedFindingsRegistry;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoMetContentGameTests {
    private HoMetContentGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void mixedCapsulePreservesAmountsAndBottleRemainders(GameTestHelper helper) {
        CapsuleBlendRecipe recipe = new CapsuleBlendRecipe(CraftingBookCategory.MISC);
        CraftingInput input = CraftingInput.of(3, 2, List.of(
                new ItemStack(ModItems.EMPTY_CAPSULE.get()),
                new ItemStack(ModItems.HO_MET_25MG.get(), 3),
                new ItemStack(ModItems.HO_MET_100MG.get()),
                new ItemStack(ModItems.MESCALINE_100MG.get()),
                new ItemStack(ModItems.LSD_BLOTTER_100UG.get()),
                ItemStack.EMPTY));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "4-HO-MET was rejected by the native blend recipe");
        ItemStack capsule = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(capsule.is(ModItems.BLEND_CAPSULE.get()), "Native blend recipe did not create a capsule");

        // Save through the real item codec, including the CUSTOM_DATA component, then reload it.
        CompoundTag saved = (CompoundTag) capsule.save(helper.getLevel().registryAccess());
        ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), saved);
        List<CapsuleBlend.Entry> entries = CapsuleBlend.read(restored);
        helper.assertTrue(entries.size() == 3, "A blended substance was lost or same-substance amounts failed to merge");
        assertStoredDose(helper, entries, "four_ho_met_milligrams", 125.0D);
        assertStoredDose(helper, entries, "mescaline_milligrams", 100.0D);
        assertStoredDose(helper, entries, "lsd_micrograms", 100.0D);
        helper.assertTrue(CapsuleBlend.read(capsule).equals(entries), "Capsule contents changed across item save/load");
        helper.assertTrue(input.getItem(1).getCount() == 3, "Recipe preview consumed source items");

        List<ItemStack> remainders = recipe.getRemainingItems(input);
        helper.assertTrue(remainders.get(1).is(Items.GLASS_BOTTLE) && remainders.get(1).getCount() == 1,
                "The first bottle's crafting remainder was lost or multiplied by its stack count");
        helper.assertTrue(remainders.get(2).is(Items.GLASS_BOTTLE) && remainders.get(2).getCount() == 1,
                "The second bottle's crafting remainder was lost");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void dealerCatalogKeepsOldProductsAlongsideNewPsychedelics(GameTestHelper helper) {
        List<String> expectedProducts = List.of(
                "cannabis_joint", "psilocybin_capsule_1g", "cocaine_powder", "heroin_100mg",
                "lsd_blotter_100ug", "dmt_crystal", "ketamine_vial", "mdma_100mg", "two_cb_100mg", "salvia_leaf",
                "four_ho_met_25mg", "four_ho_met_100mg", "four_ho_met_250mg", "four_ho_met_500mg",
                "al_lad_25mg", "al_lad_100mg", "al_lad_250mg", "al_lad_500mg",
                "two_ce_10mg", "two_ce_20mg", "two_ce_40mg", "two_ce_80mg",
                "nitrous_canister",
                "mescaline_25mg", "mescaline_100mg", "mescaline_250mg", "mescaline_500mg");
        for (String id : expectedProducts) {
            helper.assertTrue(DealerRegistries.product(id).isPresent(), "Dealer product missing: " + id);
            helper.assertFalse(DealerRegistries.product(id).orElseThrow().stack(1).isEmpty(), "Dealer product has no registered item: " + id);
        }
        helper.assertTrue(CursedFindingsRegistry.contains(CursedFindingIds.HO_MET), "4-HO-MET finding cannot be unlocked");
        helper.assertTrue(CursedFindingsRegistry.contains(CursedFindingIds.AL_LAD), "AL-LAD finding cannot be unlocked");
        helper.assertTrue(CursedFindingsRegistry.contains(CursedFindingIds.TWO_CE), "2C-E finding cannot be unlocked");
        helper.assertTrue(CursedFindingsRegistry.contains(CursedFindingIds.NITROUS), "Nitrous finding cannot be unlocked");
        helper.succeed();
    }

    private static void assertStoredDose(GameTestHelper helper, List<CapsuleBlend.Entry> entries, String kind, double expected) {
        double actual = entries.stream().filter(entry -> entry.kind().equals(kind)).mapToDouble(CapsuleBlend.Entry::amount).sum();
        helper.assertTrue(Math.abs(actual - expected) < 0.00001D, "Wrong saved dose for " + kind + ": " + actual);
    }
}
