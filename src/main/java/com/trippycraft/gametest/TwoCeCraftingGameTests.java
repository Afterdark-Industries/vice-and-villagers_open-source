package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.block.entity.AdvancedChemistryTableBlockEntity;
import com.trippycraft.block.entity.BrewCauldronBlockEntity;
import com.trippycraft.block.entity.ChemistryTableBlockEntity;
import com.trippycraft.block.entity.MortarAndPestleBlockEntity;
import com.trippycraft.item.TwoCeCrystalItem;
import com.trippycraft.item.TwoCeDoseItem;
import com.trippycraft.menu.AdvancedChemistryTableMenu;
import com.trippycraft.menu.BrewCauldronMenu;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TwoCeCraftingGameTests {
    private static final BlockPos STATION = new BlockPos(3, 2, 3);

    private TwoCeCraftingGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void eitherBrewSlotOrderReusesIntermediateAndReturnsOnlyExtraReagentBottles(GameTestHelper helper) {
        for (boolean reversed : new boolean[]{false, true}) {
            BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
            int intermediateSlot = reversed ? BrewCauldronBlockEntity.WATER_SLOT : BrewCauldronBlockEntity.BARK_SLOT;
            int reagentSlot = reversed ? BrewCauldronBlockEntity.BARK_SLOT : BrewCauldronBlockEntity.WATER_SLOT;
            brew.setItem(intermediateSlot, new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get(), 2));
            brew.setItem(reagentSlot, new ItemStack(ModItems.AMINOETHYL_REAGENT.get(), 2));
            brew.setItem(BrewCauldronBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
            int looseBefore = looseBottles(helper, brew);
            tickBrew(helper, brew, 219);
            helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT).isEmpty(), "2C-E brewing completed before its normal cycle");
            tickBrew(helper, brew, 221);
            assertStack(helper, brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_PRECURSOR.get(), 2,
                    "The two brewing cycles did not yield two 2C-E Precursor bottles");
            helper.assertTrue(brew.getItem(intermediateSlot).isEmpty(), "The product's intermediate bottle returned an extra empty bottle");
            assertStack(helper, brew.getItem(reagentSlot), Items.GLASS_BOTTLE, 1, "The final reagent did not return its empty bottle");
            helper.assertTrue(looseBottles(helper, brew) - looseBefore == 1,
                    "A stacked reagent remainder was lost or duplicated while its input slot was occupied");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void wrongReagentAndBlockedPrecursorOutputPreserveInputsAndFuel(GameTestHelper helper) {
        BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
        brew.setItem(BrewCauldronBlockEntity.BARK_SLOT, new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get()));
        brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(ModItems.DISTILLED_SOLVENT.get()));
        brew.setItem(BrewCauldronBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickBrew(helper, brew, 250);
        helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT).isEmpty(), "Distilled Ethanol incorrectly substituted for Aminoethyl Reagent");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.FUEL_SLOT), Items.COAL, 1, "A wrong reagent consumed fresh fuel");
        brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(ModItems.AMINOETHYL_REAGENT.get()));
        brew.setItem(BrewCauldronBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CE_PRECURSOR.get(), 64));
        tickBrew(helper, brew, 250);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.DIMETHOXY_INTERMEDIATE.get(), 1,
                "A full output consumed the intermediate");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), ModItems.AMINOETHYL_REAGENT.get(), 1,
                "A full output consumed the reagent");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.FUEL_SLOT), Items.COAL, 1, "A full output consumed fuel");
        brew.setItem(BrewCauldronBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get()));
        tickBrew(helper, brew, 250);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.DIMETHOXY_INTERMEDIATE.get(), 1,
                "An incompatible output consumed the intermediate");
        brew.removeItemNoUpdate(BrewCauldronBlockEntity.OUTPUT_SLOT);
        tickBrew(helper, brew, 220);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_PRECURSOR.get(), 1,
                "The unblocked cauldron did not produce the precursor");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void sharedIntermediateKeepsTwoCbConversionAndPrecursorBecomesTwoCeFreebase(GameTestHelper helper) {
        ChemistryTableBlockEntity chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get()));
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickChemistry(helper, chemistry, 200);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.TWO_CB_FREEBASE.get(), 1,
                "Adding the 2C-E brew branch changed the existing Dimethoxy-to-2C-B Chemistry path");
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).isEmpty(), "The shared intermediate bottle returned a phantom empty bottle");

        chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.TWO_CE_PRECURSOR.get()));
        tickChemistry(helper, chemistry, 250);
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "2C-E Precursor converted without Chemistry fuel");
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        chemistry.setItem(ChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CB_FREEBASE.get()));
        tickChemistry(helper, chemistry, 250);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT), ModItems.TWO_CE_PRECURSOR.get(), 1,
                "A mismatched freebase output consumed the precursor");
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.FUEL_SLOT), Items.COAL, 1,
                "A mismatched freebase output consumed fresh fuel");
        chemistry.removeItemNoUpdate(ChemistryTableBlockEntity.OUTPUT_SLOT);
        tickChemistry(helper, chemistry, 199);
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "2C-E conversion retained progress from a blocked recipe");
        tickChemistry(helper, chemistry, 1);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_FREEBASE.get(), 1,
                "One precursor did not become one 2C-E Freebase");
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).isEmpty(), "The precursor bottle was not retained in the freebase product");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void allFourHeatingRoutesProduceOneFullCrystal(GameTestHelper helper) {
        assertHeating(helper, RecipeType.SMELTING);
        assertHeating(helper, RecipeType.BLASTING);
        assertHeating(helper, RecipeType.SMOKING);
        assertHeating(helper, RecipeType.CAMPFIRE_COOKING);
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void fullAndSavedPartialCrystalGrindingPreserveRemainingMass(GameTestHelper helper) {
        MortarAndPestleBlockEntity mortar = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, new ItemStack(ModItems.TWO_CE_CRYSTAL.get()));
        tickMortar(helper, mortar, 40);
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_POWDER.get(), 50,
                "A full 5000mg crystal did not yield fifty 100mg powders");
        helper.assertTrue(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT).isEmpty(), "Grinding duplicated the full crystal");
        ItemStack partial = new ItemStack(ModItems.TWO_CE_CRYSTAL.get());
        helper.assertTrue(TwoCeCrystalItem.consumeMilligrams(partial, 1000), "Could not take a 1000mg crystal portion");
        partial = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) partial.save(helper.getLevel().registryAccess()));
        helper.assertTrue(TwoCeCrystalItem.remainingMilligrams(partial) == 4000, "Saving a used crystal refilled its contents");
        mortar = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, partial);
        tickMortar(helper, mortar, 20);
        CompoundTag saved = mortar.saveWithoutMetadata(helper.getLevel().registryAccess());
        MortarAndPestleBlockEntity restored = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        tickMortar(helper, restored, 20);
        assertStack(helper, restored.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_POWDER.get(), 40,
                "Saved partial contents or saved mortar progress changed the powder yield");
        helper.assertTrue(restored.getItem(MortarAndPestleBlockEntity.INPUT_SLOT).isEmpty(), "Grinding duplicated the saved partial crystal");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void blockedMortarPreservesCrystalUntilItsEntireYieldFits(GameTestHelper helper) {
        MortarAndPestleBlockEntity mortar = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, new ItemStack(ModItems.TWO_CE_CRYSTAL.get()));
        mortar.setItem(MortarAndPestleBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CE_POWDER.get(), 15));
        tickMortar(helper, mortar, 80);
        helper.assertTrue(TwoCeCrystalItem.remainingMilligrams(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT)) == 5000,
                "An overflowing powder output consumed crystal content");
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_POWDER.get(), 15,
                "A blocked mortar changed its output");
        mortar.setItem(MortarAndPestleBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CB_POWDER.get()));
        tickMortar(helper, mortar, 80);
        helper.assertTrue(TwoCeCrystalItem.remainingMilligrams(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT)) == 5000,
                "A different substance's powder consumed the crystal");
        mortar.setItem(MortarAndPestleBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CE_POWDER.get(), 14));
        tickMortar(helper, mortar, 40);
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_POWDER.get(), 64,
                "The mortar failed to combine the whole yield when exactly enough space was available");
        helper.assertTrue(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT).isEmpty(), "A ground crystal remained in the input slot");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void allFourPillFormatsDistributeFullCrystalMassAndKeepOriginalEffectTiers(GameTestHelper helper) {
        List<Batch> batches = List.of(new Batch(0, ModItems.TWO_CE_25MG.get(), 25, 10, 4, 1, 200),
                new Batch(1, ModItems.TWO_CE_100MG.get(), 100, 20, 1, 1, 50),
                new Batch(2, ModItems.TWO_CE_250MG.get(), 250, 40, 2, 5, 20),
                new Batch(3, ModItems.TWO_CE_500MG.get(), 500, 80, 1, 5, 10));
        for (Batch batch : batches) {
            AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
            table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.TWO_CE_POWDER.get(), 50));
            int suppliedSugar = Math.min(64, batch.total());
            table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR, suppliedSugar));
            table.selectProductByButton(batch.button());
            helper.assertTrue(table.previewOutputSummary().equals(batch.output() + "x " + batch.amount() + "mg 2C-E pills"),
                    "The selected-dose preview showed the wrong substance or pill quantity");
            helper.assertTrue(table.previewRequirementSummary().equals(batch.powder() + " powder (100mg) + " + batch.output() + " sugar"),
                    "The selected-dose preview did not show the exact powder and sugar batch");
            int packaged = 0;
            while (packaged < batch.total()) {
                int powderBefore = table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).getCount();
                int sugarBefore = table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).getCount();
                if (sugarBefore < batch.output()) {
                    int refill = Math.min(64 - sugarBefore, batch.total() - suppliedSugar);
                    helper.assertTrue(refill > 0, "The exact pill batch exhausted its supplied sugar prematurely");
                    table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR, sugarBefore + refill));
                    suppliedSugar += refill;
                    sugarBefore += refill;
                }
                tickAdvanced(helper, table, 220);
                ItemStack output = table.removeItemNoUpdate(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT);
                assertStack(helper, output, batch.item(), batch.output(), "The selected physical format yielded the wrong pills");
                TwoCeDoseItem pill = (TwoCeDoseItem) output.getItem();
                helper.assertTrue(pill.contentMilligrams() == batch.amount() && pill.doseMilligrams() == batch.effect(),
                        "Crafting changed the pill's physical content or preserved calibrated effect tier");
                assertInputs(helper, table, powderBefore - batch.powder(), sugarBefore - batch.output(),
                        "The selected pill batch consumed incorrect powder or sugar counts");
                packaged += output.getCount();
            }
            helper.assertTrue(packaged * batch.amount() == 5000 && suppliedSugar == packaged,
                    "Full-crystal packaging lost mass or used extra sugar");
            helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).isEmpty()
                            && table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).isEmpty(),
                    "Full-crystal packaging left undistributed ingredients");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void incompleteAndBlockedPillBatchesPreserveExactInputMass(GameTestHelper helper) {
        AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        table.selectProductByButton(0);
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.TWO_CE_POWDER.get()));
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR, 3));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 1, 3, "A 25mg batch consumed a powder before all four sugar pieces were available");
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR, 4));
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CE_25MG.get(), 13));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 1, 4, "A 25mg batch overflowed the pill stack");
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CB_25MG.get()));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 1, 4, "A different substance's output consumed the pill ingredients");
        table.removeItemNoUpdate(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT);
        tickAdvanced(helper, table, 220);
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_25MG.get(), 4,
                "The cleared 25mg recipe did not produce the whole exact batch");

        table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        table.selectProductByButton(2);
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.TWO_CE_POWDER.get(), 4));
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR, 2));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 4, 2, "The 250mg recipe rounded four powders up to 500mg");
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.TWO_CE_POWDER.get(), 5));
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CE_250MG.get(), 15));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 5, 2, "The 250mg recipe overflowed its output stack");
        table.removeItemNoUpdate(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT);
        tickAdvanced(helper, table, 220);
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_250MG.get(), 2,
                "The 250mg recipe discarded a fractional-powder remainder instead of producing two pills");
        table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.TWO_CE_POWDER.get()));
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.GLASS_BOTTLE));
        tickAdvanced(helper, table, 250);
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "2C-E powder incorrectly used the bottled 4-HO-MET recipe");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void eitherBrewArrivalOrderRoutesWholeStacksAndBlockedLiquidPreservesInventory(GameTestHelper helper) {
        for (boolean reagentFirst : new boolean[]{false, true}) {
            ServerPlayer player = player(helper);
            BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
            BrewCauldronMenu menu = new BrewCauldronMenu(1, player.getInventory(), brew, new SimpleContainerData(4));
            ItemStack intermediate = new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get(), 3);
            CustomData.update(DataComponents.CUSTOM_DATA, intermediate, tag -> tag.putInt("TwoCeRoutingMarker", 123));
            ItemStack originalIntermediate = intermediate.copy();
            player.getInventory().setItem(9, reagentFirst ? new ItemStack(ModItems.AMINOETHYL_REAGENT.get(), 4) : intermediate);
            player.getInventory().setItem(10, reagentFirst ? intermediate : new ItemStack(ModItems.AMINOETHYL_REAGENT.get(), 4));
            helper.assertFalse(menu.quickMoveStack(player, 5).isEmpty(), "The first 2C-E brew ingredient did not shift into the cauldron");
            if (reagentFirst) {
                assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.AMINOETHYL_REAGENT.get(), 4,
                        "Reagent-first placement changed the existing 4-HO-MET primary route");
            }
            helper.assertFalse(menu.quickMoveStack(player, 6).isEmpty(), "The second 2C-E brew ingredient did not shift into the cauldron");
            int intermediateSlot = reagentFirst ? BrewCauldronBlockEntity.WATER_SLOT : BrewCauldronBlockEntity.BARK_SLOT;
            int reagentSlot = reagentFirst ? BrewCauldronBlockEntity.BARK_SLOT : BrewCauldronBlockEntity.WATER_SLOT;
            assertStack(helper, brew.getItem(intermediateSlot), ModItems.DIMETHOXY_INTERMEDIATE.get(), 3,
                    "Either-order routing lost or duplicated the intermediate stack");
            assertStack(helper, brew.getItem(reagentSlot), ModItems.AMINOETHYL_REAGENT.get(), 4,
                    "Either-order routing lost or duplicated the reagent stack");
            helper.assertTrue(ItemStack.isSameItemSameComponents(originalIntermediate, brew.getItem(intermediateSlot)),
                    "Either-order routing lost saved intermediate components");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(10).isEmpty()
                            && brew.getItem(BrewCauldronBlockEntity.LEAF_SLOT).isEmpty(),
                    "Either-order routing left duplicate inventory or put an ingredient in the leaf slot");
            brew.clearContent();
            brew.setItem(BrewCauldronBlockEntity.BARK_SLOT, new ItemStack(ModItems.AMINOETHYL_REAGENT.get(), 2));
            brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(ModItems.ETHANOL.get()));
            player.getInventory().setItem(11, new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get(), 2));
            helper.assertTrue(menu.quickMoveStack(player, 7).isEmpty(), "A blocked 2C-E shift-click changed the ongoing 4-HO-MET route");
            assertStack(helper, player.getInventory().getItem(11), ModItems.DIMETHOXY_INTERMEDIATE.get(), 2,
                    "A blocked intermediate shift-click lost inventory stock");
            assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.AMINOETHYL_REAGENT.get(), 2,
                    "A blocked intermediate shift-click replaced the primary reagent");
            assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), ModItems.ETHANOL.get(), 1,
                    "A blocked intermediate shift-click replaced existing Ethanol");
            brew.clearContent();
            menu.setItem(BrewCauldronBlockEntity.BARK_SLOT, 0, new ItemStack(Items.GLASS_BOTTLE));
            assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), Items.GLASS_BOTTLE, 1,
                    "A reversed 2C-E recipe's extra empty bottle was discarded by the client setter");
            helper.assertFalse(menu.getSlot(BrewCauldronBlockEntity.BARK_SLOT).mayPlace(new ItemStack(Items.GLASS_BOTTLE)),
                    "Returned bottle updates permitted player glass insertion into primary");
            helper.assertFalse(menu.quickMoveStack(player, BrewCauldronBlockEntity.BARK_SLOT).isEmpty(), "A returned extra bottle could not shift out");
            helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.BARK_SLOT).isEmpty(), "Returned bottle shift-out duplicated glass");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void actualPillMenuRoutesIngredientsAndChangedDosageSurvivesSaveLoad(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        AdvancedChemistryTableMenu menu = (AdvancedChemistryTableMenu) table.createMenu(1, player.getInventory(), player);
        player.getInventory().setItem(9, new ItemStack(ModItems.TWO_CE_POWDER.get(), 6));
        player.getInventory().setItem(10, new ItemStack(Items.SUGAR, 6));
        helper.assertFalse(menu.quickMoveStack(player, 3).isEmpty(), "2C-E powder did not shift into the table");
        helper.assertTrue(menu.getPreviewOutputSummary(0).equals("4x 25mg 2C-E pills"), "Powder-first placement did not expose the 2C-E preview");
        helper.assertFalse(menu.quickMoveStack(player, 4).isEmpty(), "Sugar did not shift into the table");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT), ModItems.TWO_CE_POWDER.get(), 6,
                "Powder did not route into primary");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT), Items.SUGAR, 6, "Sugar did not route into secondary");
        helper.assertTrue(menu.clickMenuButton(player, 2) && menu.getSelectedProductId() == 2, "The actual menu did not synchronize pill dosage selection");
        tickAdvanced(helper, table, 100);
        helper.assertTrue(menu.clickMenuButton(player, 0), "Could not change dosage during pill crafting");
        helper.assertTrue(table.saveWithoutMetadata(helper.getLevel().registryAccess()).getInt("RefineProgress") == 0,
                "Changing dosage failed to restart pill crafting progress");
        tickAdvanced(helper, table, 20);
        CompoundTag saved = table.saveWithoutMetadata(helper.getLevel().registryAccess());
        AdvancedChemistryTableBlockEntity restored = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.selectedProduct().buttonId() == 0, "The selected pill dosage was lost across block save/load");
        tickAdvanced(helper, restored, 200);
        assertStack(helper, restored.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.TWO_CE_25MG.get(), 4,
                "Restored changed-dosage crafting produced an incorrect pill format or quantity");
        assertInputs(helper, restored, 5, 2, "Restored changed-dosage crafting consumed incorrect ingredient counts");
        AdvancedChemistryTableMenu restoredMenu = (AdvancedChemistryTableMenu) restored.createMenu(2, player.getInventory(), player);
        helper.assertFalse(restoredMenu.quickMoveStack(player, AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(),
                "Finished pills could not shift into inventory");
        int pills = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.TWO_CE_25MG.get())) pills += player.getInventory().getItem(slot).getCount();
        }
        helper.assertTrue(pills == 4 && restored.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(),
                "Finished-pill shift-out lost or duplicated pills");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "two-ce-crafting-test"));
        player.getInventory().clearContent();
        BlockPos position = helper.absolutePos(STATION);
        player.setPos(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
        return player;
    }

    private static <T extends BlockEntity> T place(GameTestHelper helper, Supplier<Block> block, Class<T> type) {
        BlockPos position = helper.absolutePos(STATION);
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(position, block.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity entity = helper.getLevel().getBlockEntity(position);
        helper.assertTrue(type.isInstance(entity), "Expected placed machine: " + type.getSimpleName());
        return type.cast(entity);
    }

    private static void tickChemistry(GameTestHelper helper, ChemistryTableBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) ChemistryTableBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), helper.getLevel().getBlockState(machine.getBlockPos()), machine);
    }

    private static void tickBrew(GameTestHelper helper, BrewCauldronBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) BrewCauldronBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), helper.getLevel().getBlockState(machine.getBlockPos()), machine);
    }

    private static void tickMortar(GameTestHelper helper, MortarAndPestleBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) MortarAndPestleBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), helper.getLevel().getBlockState(machine.getBlockPos()), machine);
    }

    private static void tickAdvanced(GameTestHelper helper, AdvancedChemistryTableBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) AdvancedChemistryTableBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), helper.getLevel().getBlockState(machine.getBlockPos()), machine);
    }

    private static int looseBottles(GameTestHelper helper, BrewCauldronBlockEntity machine) {
        int total = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(machine.getBlockPos()).inflate(2.0D))) {
            if (entity.getItem().is(Items.GLASS_BOTTLE)) total += entity.getItem().getCount();
        }
        return total;
    }

    private static <T extends AbstractCookingRecipe> void assertHeating(GameTestHelper helper, RecipeType<T> type) {
        SingleRecipeInput input = new SingleRecipeInput(new ItemStack(ModItems.TWO_CE_FREEBASE.get()));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(type, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "Missing 2C-E freebase heating route for " + type);
        ItemStack result = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
        assertStack(helper, result, ModItems.TWO_CE_CRYSTAL.get(), 1, "Heating yielded an incorrect crystal output");
        helper.assertTrue(TwoCeCrystalItem.remainingMilligrams(result) == 5000 && input.item().getCount() == 1,
                "Heating produced a partial crystal or consumed input during preview");
    }

    private static void assertInputs(GameTestHelper helper, AdvancedChemistryTableBlockEntity table, int powder, int sugar, String message) {
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).getCount() == powder
                && table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).getCount() == sugar, message);
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count, String message) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, message + ": " + stack);
    }

    private record Batch(int button, Item item, int amount, double effect, int output, int powder, int total) {
    }
}
