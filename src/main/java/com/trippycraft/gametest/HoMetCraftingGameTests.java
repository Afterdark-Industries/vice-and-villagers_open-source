package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.block.entity.AdvancedChemistryTableBlockEntity;
import com.trippycraft.block.entity.BrewCauldronBlockEntity;
import com.trippycraft.block.entity.ChemistryTableBlockEntity;
import com.trippycraft.block.entity.MortarAndPestleBlockEntity;
import com.trippycraft.crafting.CapsuleBlendRecipe;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.item.HoMetCrystalItem;
import com.trippycraft.item.HoMetDoseItem;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Supplier;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HoMetCraftingGameTests {
    private static final BlockPos STATION = new BlockPos(3, 2, 3);
    private static final int CHEMISTRY_CYCLE = 200;
    private static final int BREW_CYCLE = 220;
    private static final int MORTAR_CYCLE = 40;
    private static final int ADVANCED_CYCLE = 220;

    private HoMetCraftingGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void barkBrewsAndRefinesWithExactlyOneBottleReturned(GameTestHelper helper) {
        ChemistryTableBlockEntity chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.ACACIA_BARK.get()));
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickChemistry(helper, chemistry, CHEMISTRY_CYCLE - 1);
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(),
                "Chemistry produced reagent before its cycle completed");
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).getCount() == 1,
                "Chemistry consumed bark before its cycle completed");
        tickChemistry(helper, chemistry, 1);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.AMINOETHYL_REAGENT.get(), 1,
                "One bark must produce one reagent");
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).isEmpty(), "Chemistry did not consume the bark");
        ItemStack reagent = chemistry.removeItemNoUpdate(ChemistryTableBlockEntity.OUTPUT_SLOT);

        BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
        brew.setItem(BrewCauldronBlockEntity.BARK_SLOT, reagent);
        brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(ModItems.ETHANOL.get()));
        brew.setItem(BrewCauldronBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickBrew(helper, brew, BREW_CYCLE);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT), ModItems.MET_INTERMEDIATE.get(), 1,
                "Reagent and Ethanol must produce one MET Intermediate");
        helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.BARK_SLOT).isEmpty(),
                "The reagent bottle must become the product bottle");
        helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.LEAF_SLOT).isEmpty(), "MET brew unexpectedly needs a leaf ingredient");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), Items.GLASS_BOTTLE, 1,
                "Exactly the extra Ethanol bottle must return empty");
        ItemStack intermediate = brew.removeItemNoUpdate(BrewCauldronBlockEntity.OUTPUT_SLOT);

        chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, intermediate);
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickChemistry(helper, chemistry, CHEMISTRY_CYCLE);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_FREEBASE.get(), 1,
                "One intermediate must produce one freebase");
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).isEmpty(),
                "The intermediate bottle must become the freebase bottle without duplicating glass");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void everyHeatingRouteProducesOneFullCrystal(GameTestHelper helper) {
        assertHeatingRoute(helper, RecipeType.SMELTING);
        assertHeatingRoute(helper, RecipeType.SMOKING);
        assertHeatingRoute(helper, RecipeType.BLASTING);
        assertHeatingRoute(helper, RecipeType.CAMPFIRE_COOKING);
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void mortarConservesFullAndPartlyConsumedCrystals(GameTestHelper helper) {
        MortarAndPestleBlockEntity mortar = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, new ItemStack(ModItems.HO_MET_CRYSTAL.get()));
        tickMortar(helper, mortar, MORTAR_CYCLE);
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_POWDER.get(), 50,
                "A full 5000mg crystal must yield fifty 100mg powders");
        helper.assertTrue(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT).isEmpty(), "Mortar failed to consume the full crystal");

        mortar.clearContent();
        ItemStack partial = new ItemStack(ModItems.HO_MET_CRYSTAL.get());
        helper.assertTrue(HoMetCrystalItem.consumeMilligrams(partial, 1000), "Could not take a crystal portion");
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, roundTrip(helper, partial));
        tickMortar(helper, mortar, MORTAR_CYCLE);
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_POWDER.get(), 40,
                "A crystal with 4000mg left must yield forty powders after item save/load");
        helper.assertTrue(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT).isEmpty(), "Mortar failed to consume the partial crystal");

        mortar.clearContent();
        ItemStack legacy = new ItemStack(ModItems.HO_MET_CRYSTAL.get());
        CustomData.update(DataComponents.CUSTOM_DATA, legacy, tag -> tag.putInt("TrippyCraftHoMetCrystalRemaining", 500));
        legacy = roundTrip(helper, legacy);
        helper.assertTrue(HoMetCrystalItem.remainingMilligrams(legacy) == 500,
                "An old crystal's saved 500mg remainder must stay absolute after the capacity change");
        helper.assertFalse(HoMetCrystalItem.consumeMilligrams(legacy, 1000),
                "An old 500mg remainder cannot provide a new 1000mg serving");
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, legacy);
        tickMortar(helper, mortar, MORTAR_CYCLE);
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_POWDER.get(), 5,
                "An old crystal with 500mg saved must yield five 100mg powders without being refilled");
        helper.assertTrue(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT).isEmpty(), "Mortar failed to consume the legacy crystal");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void blockedMortarPreservesCrystalUntilTheWholeYieldFits(GameTestHelper helper) {
        MortarAndPestleBlockEntity mortar = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, new ItemStack(ModItems.HO_MET_CRYSTAL.get()));
        mortar.setItem(MortarAndPestleBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.HO_MET_POWDER.get(), 15));
        tickMortar(helper, mortar, MORTAR_CYCLE * 2);
        helper.assertTrue(HoMetCrystalItem.remainingMilligrams(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT)) == 5000,
                "Mortar consumed content when fifty powders would overflow the output");
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_POWDER.get(), 15,
                "Blocked mortar changed its output");

        mortar.setItem(MortarAndPestleBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIRT));
        tickMortar(helper, mortar, MORTAR_CYCLE * 2);
        helper.assertTrue(HoMetCrystalItem.remainingMilligrams(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT)) == 5000,
                "Mortar consumed a crystal behind an incompatible output");
        mortar.setItem(MortarAndPestleBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.HO_MET_POWDER.get(), 14));
        tickMortar(helper, mortar, MORTAR_CYCLE);
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_POWDER.get(), 64,
                "Mortar should combine the whole yield when exactly fifty output spaces are free");
        helper.assertTrue(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT).isEmpty(), "Successful mortar cycle left its crystal input");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void advancedTableConservesMassForEverySelectedBottleSize(GameTestHelper helper) {
        List<BatchCase> cases = List.of(
                new BatchCase(0, ModItems.HO_MET_25MG.get(), 25, 200, 4, 1),
                new BatchCase(1, ModItems.HO_MET_100MG.get(), 100, 50, 1, 1),
                new BatchCase(2, ModItems.HO_MET_250MG.get(), 250, 20, 2, 5),
                new BatchCase(3, ModItems.HO_MET_500MG.get(), 500, 10, 1, 5));
        for (BatchCase batch : cases) {
            AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
            table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.HO_MET_POWDER.get(), 50));
            helper.assertTrue(table.selectProductByButton(batch.buttonId()), "Could not select the requested bottle size");
            int packaged = 0;
            int suppliedBottles = 0;
            for (int cycle = 0; cycle < batch.outputCount() / batch.cycleOutputCount(); cycle++) {
                int bottlesBefore = table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).getCount();
                if (bottlesBefore < batch.cycleOutputCount()) {
                    int refill = Math.min(64 - bottlesBefore, batch.outputCount() - suppliedBottles);
                    helper.assertTrue(refill > 0, "The full-crystal batch unexpectedly exhausted its bottle supply");
                    bottlesBefore += refill;
                    table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.GLASS_BOTTLE, bottlesBefore));
                    suppliedBottles += refill;
                }
                helper.assertTrue(bottlesBefore <= 64, "A bottle refill exceeded the machine slot limit");
                int powderBefore = table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).getCount();
                tickAdvanced(helper, table, ADVANCED_CYCLE);
                ItemStack output = table.removeItemNoUpdate(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT);
                assertStack(helper, output, batch.item(), batch.cycleOutputCount(),
                        "Wrong cycle output for " + batch.amountMilligrams() + "mg");
                packaged += output.getCount();
                assertAdvancedInputs(helper, table, powderBefore - batch.cyclePowderCount(), bottlesBefore - batch.cycleOutputCount(),
                        "A batch consumed the wrong powder or bottle count for " + batch.amountMilligrams() + "mg");
                helper.assertTrue(packaged * batch.amountMilligrams()
                                + table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).getCount() * 100 == 5000,
                        "A refinement cycle failed to conserve the original 5000mg");
            }
            helper.assertTrue(packaged == batch.outputCount(), "Wrong full-crystal packaging yield for " + batch.amountMilligrams() + "mg");
            helper.assertTrue(suppliedBottles == batch.outputCount(), "Full-crystal packaging supplied an incorrect bottle total");
            helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).isEmpty(),
                    "Full-crystal packaging left unused powder for " + batch.amountMilligrams() + "mg");
            helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).isEmpty(),
                    "The table must consume exactly one empty bottle per filled bottle");
            helper.assertTrue(packaged * batch.amountMilligrams()
                            + table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).getCount() * 100 == 5000,
                    "The packaged and leftover contents do not conserve the original 5000mg");
            tickAdvanced(helper, table, ADVANCED_CYCLE);
            helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(),
                    "The emptied table produced additional bottles");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void advancedTableWaitsForCompleteBatchesAndOutputSpace(GameTestHelper helper) {
        AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        table.selectProductByButton(0);
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.HO_MET_POWDER.get()));
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.GLASS_BOTTLE, 3));
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertAdvancedInputs(helper, table, 1, 3, "A 25mg batch requires four bottles before consuming any powder");
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "An incomplete bottle batch produced output");

        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.GLASS_BOTTLE, 4));
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.HO_MET_25MG.get(), 13));
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertAdvancedInputs(helper, table, 1, 4, "The table must wait when thirteen plus four 25mg bottles exceed the output limit");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_25MG.get(), 13,
                "The table changed a full output stack");

        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.HO_MET_100MG.get()));
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertAdvancedInputs(helper, table, 1, 4, "An incompatible bottle output must block refinement");
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        tickAdvanced(helper, table, ADVANCED_CYCLE);
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_25MG.get(), 4,
                "The table must resume with exactly four 25mg bottles when output is cleared");
        assertAdvancedInputs(helper, table, 0, 0, "Completed 25mg batch consumed the wrong input counts");

        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        table.selectProductByButton(2);
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.HO_MET_POWDER.get(), 4));
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.GLASS_BOTTLE, 2));
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertAdvancedInputs(helper, table, 4, 2, "Four powders must not be rounded into a two-bottle 500mg batch");
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "Insufficient powder produced 250mg bottles");

        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.HO_MET_POWDER.get(), 5));
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.HO_MET_250MG.get(), 15));
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertAdvancedInputs(helper, table, 5, 2, "The table must wait when fifteen plus two 250mg bottles exceed the output limit");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_250MG.get(), 15,
                "The table changed an overflowing 250mg output stack");
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        tickAdvanced(helper, table, ADVANCED_CYCLE);
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_250MG.get(), 2,
                "The table must resume with exactly two 250mg bottles when output is cleared");
        assertAdvancedInputs(helper, table, 0, 0, "Completed 250mg batch consumed the wrong input counts");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void selectedBottleSizeResetsProgressAndSurvivesBlockSaveLoad(GameTestHelper helper) {
        AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.HO_MET_POWDER.get(), 50));
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.GLASS_BOTTLE, 10));
        table.selectProductByButton(2);
        tickAdvanced(helper, table, 100);
        CompoundTag beforeChange = table.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(beforeChange.getInt("RefineProgress") == 100, "Refinement did not accumulate progress");
        table.selectProductByButton(1);
        helper.assertTrue(table.saveWithoutMetadata(helper.getLevel().registryAccess()).getInt("RefineProgress") == 0,
                "Changing bottle size must reset progress before using a different recipe");
        tickAdvanced(helper, table, 20);
        CompoundTag saved = table.saveWithoutMetadata(helper.getLevel().registryAccess());
        AdvancedChemistryTableBlockEntity restored = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.selectedProduct().buttonId() == 1, "The selected bottle size was lost across block save/load");
        assertAdvancedInputs(helper, restored, 50, 10, "The stored machine inputs were lost across block save/load");
        tickAdvanced(helper, restored, ADVANCED_CYCLE - 20);
        assertStack(helper, restored.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.HO_MET_100MG.get(), 1,
                "A restored machine must resume its saved progress and selected recipe");
        assertAdvancedInputs(helper, restored, 49, 9, "Restored refinement consumed the wrong input counts");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void crystalContentsPersistAndExhaustAfterFivePortions(GameTestHelper helper) {
        ItemStack crystal = new ItemStack(ModItems.HO_MET_CRYSTAL.get());
        helper.assertTrue(crystal.getMaxStackSize() == 1, "A crystal must keep its remaining contents private to one item");
        helper.assertTrue(HoMetCrystalItem.remainingMilligrams(crystal) == 5000, "A new crystal is not full");
        helper.assertTrue(HoMetCrystalItem.POWDER_MILLIGRAMS == 100, "A powder item must contain 100mg");
        helper.assertFalse(HoMetCrystalItem.consumeMilligrams(crystal, 500), "A crystal must reject a partial 1000mg serving");
        helper.assertFalse(HoMetCrystalItem.consumeMilligrams(crystal, 6000), "A crystal must reject more than its saved contents");
        helper.assertTrue(HoMetCrystalItem.remainingMilligrams(crystal) == 5000, "Rejected crystal uses changed its contents");
        for (int portion = 1; portion <= 5; portion++) {
            helper.assertTrue(HoMetCrystalItem.consumeMilligrams(crystal, 1000), "Crystal rejected valid portion " + portion);
            crystal = roundTrip(helper, crystal);
            helper.assertTrue(HoMetCrystalItem.remainingMilligrams(crystal) == 5000 - portion * 1000,
                    "Crystal content changed across save/load after portion " + portion);
        }
        helper.assertTrue(crystal.isEmpty(), "The fifth portion must exhaust the crystal item");
        helper.assertFalse(HoMetCrystalItem.consumeMilligrams(crystal, 1000), "An exhausted crystal must not produce another portion");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void newBottleAmountsKeepOldEffectsInMixedAndLegacyCapsules(GameTestHelper helper) {
        assertBottleTier(helper, ModItems.HO_MET_25MG.get(), 25, 5.0D);
        assertBottleTier(helper, ModItems.HO_MET_100MG.get(), 100, 10.0D);
        assertBottleTier(helper, ModItems.HO_MET_250MG.get(), 250, 20.0D);
        assertBottleTier(helper, ModItems.HO_MET_500MG.get(), 500, 40.0D);
        helper.assertTrue(HoMetCrystalItem.EFFECT_DOSE_PER_USE == 80.0D
                        && HoMetCrystalItem.EFFECT_DOSE_PER_USE == 2 * ((HoMetDoseItem) ModItems.HO_MET_500MG.get()).doseMilligrams(),
                "A 1000mg crystal portion must carry the extrapolated effect of two preserved 500mg bottle tiers");

        CapsuleBlendRecipe recipe = new CapsuleBlendRecipe(CraftingBookCategory.MISC);
        CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(ModItems.EMPTY_CAPSULE.get()),
                new ItemStack(ModItems.HO_MET_25MG.get(), 3), new ItemStack(ModItems.HO_MET_100MG.get())));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "New bottle formats were rejected by the blend recipe");
        ItemStack capsule = recipe.assemble(input, helper.getLevel().registryAccess());
        List<CapsuleBlend.Entry> entries = CapsuleBlend.read(roundTrip(helper, capsule));
        helper.assertTrue(entries.size() == 1, "The two bottle tiers did not merge into one 4-HO-MET capsule entry");
        CapsuleBlend.Entry entry = entries.getFirst();
        helper.assertTrue(entry.kind().equals("four_ho_met_milligrams"), "Capsule contains the wrong substance");
        helper.assertTrue(entry.amount() == 125.0D, "Capsule must preserve 125mg content from one bottle per crafting slot");
        helper.assertTrue(entry.effectAmount() == 15.0D, "Capsule must preserve the old 5 + 10 effect tier sum");
        helper.assertTrue(input.getItem(1).getCount() == 3, "Capsule preview consumed source bottles");
        List<ItemStack> remainders = recipe.getRemainingItems(input);
        assertStack(helper, remainders.get(1), Items.GLASS_BOTTLE, 1, "The 25mg bottle crafting remainder was lost or multiplied");
        assertStack(helper, remainders.get(2), Items.GLASS_BOTTLE, 1, "The 100mg bottle crafting remainder was lost");

        ItemStack legacy = new ItemStack(ModItems.BLEND_CAPSULE.get());
        CompoundTag legacyEntry = new CompoundTag();
        legacyEntry.putString("Kind", "four_ho_met_milligrams");
        legacyEntry.putDouble("Amount", 50.0D);
        ListTag legacyEntries = new ListTag();
        legacyEntries.add(legacyEntry);
        CompoundTag legacyRoot = new CompoundTag();
        legacyRoot.put("Entries", legacyEntries);
        CustomData.update(DataComponents.CUSTOM_DATA, legacy, tag -> tag.put("TrippyCraftCapsuleBlend", legacyRoot));
        List<CapsuleBlend.Entry> restoredLegacy = CapsuleBlend.read(roundTrip(helper, legacy));
        helper.assertTrue(restoredLegacy.size() == 1 && restoredLegacy.getFirst().amount() == 50.0D,
                "A legacy capsule lost its stored amount");
        helper.assertTrue(restoredLegacy.getFirst().effectAmount() == 50.0D,
                "Legacy capsules without EffectAmount must retain their old effect strength");
        helper.succeed();
    }

    private static <T extends BlockEntity> T place(GameTestHelper helper, Supplier<Block> block, Class<T> type) {
        BlockPos position = helper.absolutePos(STATION);
        // Replacing with air gives every test case a fresh block entity and machine clock.
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(position, block.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity entity = helper.getLevel().getBlockEntity(position);
        helper.assertTrue(type.isInstance(entity), "Expected placed machine block entity: " + type.getSimpleName());
        return type.cast(entity);
    }

    private static void tickChemistry(GameTestHelper helper, ChemistryTableBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            ChemistryTableBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(),
                    helper.getLevel().getBlockState(machine.getBlockPos()), machine);
        }
    }

    private static void tickBrew(GameTestHelper helper, BrewCauldronBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            BrewCauldronBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(),
                    helper.getLevel().getBlockState(machine.getBlockPos()), machine);
        }
    }

    private static void tickMortar(GameTestHelper helper, MortarAndPestleBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            MortarAndPestleBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(),
                    helper.getLevel().getBlockState(machine.getBlockPos()), machine);
        }
    }

    private static void tickAdvanced(GameTestHelper helper, AdvancedChemistryTableBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            AdvancedChemistryTableBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(),
                    helper.getLevel().getBlockState(machine.getBlockPos()), machine);
        }
    }

    private static <T extends AbstractCookingRecipe> void assertHeatingRoute(GameTestHelper helper, RecipeType<T> type) {
        SingleRecipeInput input = new SingleRecipeInput(new ItemStack(ModItems.HO_MET_FREEBASE.get()));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(type, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "No freebase-to-crystal recipe exists for " + type);
        ItemStack result = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
        assertStack(helper, result, ModItems.HO_MET_CRYSTAL.get(), 1, "Wrong crystal output for " + type);
        helper.assertTrue(HoMetCrystalItem.remainingMilligrams(result) == 5000, "Heating created a partial crystal for " + type);
        helper.assertTrue(input.item().getCount() == 1, "Heating recipe preview consumed the freebase input");
    }

    private static void assertAdvancedInputs(GameTestHelper helper, AdvancedChemistryTableBlockEntity table,
                                             int powder, int bottles, String message) {
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).getCount() == powder
                && table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).getCount() == bottles, message);
    }

    private static void assertBottleTier(GameTestHelper helper, Item item, int amount, double effect) {
        helper.assertTrue(item instanceof HoMetDoseItem, "A finished 4-HO-MET format is not a dose item");
        HoMetDoseItem bottle = (HoMetDoseItem) item;
        helper.assertTrue(bottle.amountMilligrams() == amount, "Wrong labeled bottle content for " + amount + "mg");
        helper.assertTrue(bottle.doseMilligrams() == effect, "The original effect tier changed for " + amount + "mg");
        helper.assertTrue(new ItemStack(item).getCraftingRemainingItem().is(Items.GLASS_BOTTLE),
                "Finished bottle lost its empty-glass remainder");
    }

    private static ItemStack roundTrip(GameTestHelper helper, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CompoundTag saved = (CompoundTag) stack.save(helper.getLevel().registryAccess());
        return ItemStack.parseOptional(helper.getLevel().registryAccess(), saved);
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count, String message) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, message + ": " + stack);
    }

    private record BatchCase(int buttonId, Item item, int amountMilligrams, int outputCount,
                             int cycleOutputCount, int cyclePowderCount) {
    }
}
