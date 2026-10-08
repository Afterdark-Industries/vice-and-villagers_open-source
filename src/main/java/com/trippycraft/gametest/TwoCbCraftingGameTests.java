package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.block.entity.AdvancedChemistryTableBlockEntity;
import com.trippycraft.block.entity.BrewCauldronBlockEntity;
import com.trippycraft.block.entity.ChemistryTableBlockEntity;
import com.trippycraft.block.entity.MortarAndPestleBlockEntity;
import com.trippycraft.item.TwoCbCrystalItem;
import com.trippycraft.item.TwoCbDoseItem;
import com.trippycraft.menu.AdvancedChemistryTableMenu;
import com.trippycraft.menu.BrewCauldronMenu;
import com.trippycraft.menu.ChemistryTableMenu;
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
public final class TwoCbCraftingGameTests {
    private static final BlockPos STATION = new BlockPos(3, 2, 3);

    private TwoCbCraftingGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void upstreamDistillationAndAromaticBrewingConserveTheirBottles(GameTestHelper helper) {
        ChemistryTableBlockEntity chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.ETHANOL.get()));
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickChemistry(helper, chemistry, 200);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.DISTILLED_SOLVENT.get(), 1,
                "One Ethanol bottle did not become one Distilled Ethanol bottle");
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).isEmpty(),
                "Distilling Ethanol returned a duplicate empty bottle in addition to the bottled product");

        BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
        brew.setItem(BrewCauldronBlockEntity.BARK_SLOT, new ItemStack(ModItems.BUNDLED_PLANT_EXTRACT.get()));
        brew.setItem(BrewCauldronBlockEntity.LEAF_SLOT, new ItemStack(ModItems.ETHANOL.get()));
        brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(Items.GLASS_BOTTLE));
        brew.setItem(BrewCauldronBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickBrew(helper, brew, 220);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT), ModItems.AROMATIC_EXTRACT.get(), 1,
                "The upstream aromatic recipe changed its product");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.LEAF_SLOT), Items.GLASS_BOTTLE, 1,
                "Aromatic brewing lost the Ethanol bottle remainder");
        helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.WATER_SLOT).isEmpty(), "The product's source bottle returned a duplicate glass bottle");
        ServerPlayer player = player(helper);
        BrewCauldronMenu menu = new BrewCauldronMenu(1, player.getInventory(), brew, new SimpleContainerData(4));
        menu.setItem(BrewCauldronBlockEntity.LEAF_SLOT, 0, new ItemStack(Items.GLASS_BOTTLE));
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.LEAF_SLOT), Items.GLASS_BOTTLE, 1,
                "The client slot-update path discarded the upstream Ethanol bottle return");
        helper.assertFalse(menu.getSlot(BrewCauldronBlockEntity.LEAF_SLOT).mayPlace(new ItemStack(Items.GLASS_BOTTLE)),
                "Returned-bottle synchronization permitted player glass insertion into the leaf slot");
        helper.assertFalse(menu.quickMoveStack(player, BrewCauldronBlockEntity.LEAF_SLOT).isEmpty(), "The upstream returned bottle could not shift out");
        int glass = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(Items.GLASS_BOTTLE)) glass += player.getInventory().getItem(slot).getCount();
        }
        helper.assertTrue(glass == 1 && brew.getItem(BrewCauldronBlockEntity.LEAF_SLOT).isEmpty(),
                "Shifting the upstream returned bottle lost or duplicated glass");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void defaultAromaticBranchPreservesMdmaAndLegacySaves(GameTestHelper helper) {
        ChemistryTableBlockEntity chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.AROMATIC_EXTRACT.get()));
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        CompoundTag legacySave = chemistry.saveWithoutMetadata(helper.getLevel().registryAccess());
        legacySave.remove("AromaticRecipe");
        chemistry.loadWithComponents(legacySave, helper.getLevel().registryAccess());
        helper.assertTrue(chemistry.aromaticRecipeId() == ChemistryTableBlockEntity.AROMATIC_REFINED_OIL,
                "Existing saves must keep the MDMA aromatic recipe by default");
        tickChemistry(helper, chemistry, 200);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.REFINED_AROMATIC_OIL.get(), 1,
                "The default Chemistry recipe changed the MDMA aromatic output");
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).isEmpty(),
                "The transformed aromatic bottle returned a phantom glass bottle");

        BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
        brew.setItem(BrewCauldronBlockEntity.BARK_SLOT, new ItemStack(ModItems.REFINED_AROMATIC_OIL.get()));
        brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(ModItems.MEDICINAL_REAGENT.get()));
        brew.setItem(BrewCauldronBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickBrew(helper, brew, 220);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT), ModItems.CRUDE_EMPATHOGEN_BASE.get(), 1,
                "The new 2C-B branch broke the existing MDMA brewing recipe");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), Items.GLASS_BOTTLE, 1,
                "MDMA brewing lost the extra reagent bottle");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void aromaticMenuSwitchResetsProgressAndPersistsTheSelectedBranch(GameTestHelper helper) {
        ChemistryTableBlockEntity chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        ServerPlayer player = player(helper);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.AROMATIC_EXTRACT.get(), 2));
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        ChemistryTableMenu menu = (ChemistryTableMenu) chemistry.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu.hasAromaticInput() && menu.getSelectedAromaticRecipeId() == 0, "The aromatic menu did not expose its MDMA default");
        tickChemistry(helper, chemistry, 100);
        CompoundTag midway = chemistry.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(midway.getInt("DistillProgress") == 100, "Chemistry did not advance the original recipe");
        helper.assertTrue(menu.clickMenuButton(player, 1) && menu.getSelectedAromaticRecipeId() == 1,
                "The actual menu button did not select and synchronize the 2C-B branch");
        CompoundTag changed = chemistry.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(changed.getInt("DistillProgress") == 0 && changed.getInt("BurnTime") == midway.getInt("BurnTime"),
                "Switching aromatic recipe must restart progress without spending extra fuel");
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT), ModItems.AROMATIC_EXTRACT.get(), 2,
                "Switching aromatic recipe consumed an ingredient before completion");
        tickChemistry(helper, chemistry, 20);
        helper.assertTrue(menu.clickMenuButton(player, 1), "Reselecting the current aromatic recipe was rejected");
        helper.assertTrue(chemistry.saveWithoutMetadata(helper.getLevel().registryAccess()).getInt("DistillProgress") == 20,
                "Reselecting the current branch unnecessarily restarted progress");
        helper.assertFalse(menu.clickMenuButton(player, 2), "An invalid aromatic recipe ID was accepted");
        player.setPos(chemistry.getBlockPos().getX() + 20, chemistry.getBlockPos().getY(), chemistry.getBlockPos().getZ());
        helper.assertFalse(menu.clickMenuButton(player, 0), "An out-of-range player changed the aromatic recipe");
        CompoundTag saved = chemistry.saveWithoutMetadata(helper.getLevel().registryAccess());
        ChemistryTableBlockEntity restored = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.aromaticRecipeId() == 1, "The selected 2C-B aromatic branch was lost on block save/load");
        tickChemistry(helper, restored, 180);
        assertStack(helper, restored.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.PHENETHYLAMINE_BASE.get(), 1,
                "A switched and restored recipe produced the old output or completed early");
        assertStack(helper, restored.getItem(ChemistryTableBlockEntity.INPUT_SLOT), ModItems.AROMATIC_EXTRACT.get(), 1,
                "The restored aromatic recipe consumed an incorrect input quantity");
        ChemistryTableMenu clientMenu = new ChemistryTableMenu(2, player.getInventory(), restored, new SimpleContainerData(5));
        clientMenu.setData(4, 1);
        helper.assertTrue(clientMenu.getSelectedAromaticRecipeId() == 1, "The fifth client data field did not update the selected branch");
        restored.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get()));
        helper.assertFalse(restored.selectAromaticRecipeByButton(0), "Aromatic selection was allowed for an unrelated ingredient");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void selectedAromaticRecipeWaitsForFuelAndOutputSpace(GameTestHelper helper) {
        ChemistryTableBlockEntity chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.AROMATIC_EXTRACT.get()));
        helper.assertTrue(chemistry.selectAromaticRecipeByButton(1), "Could not select the 2C-B aromatic recipe");
        tickChemistry(helper, chemistry, 250);
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "Chemistry converted an aromatic bottle without fuel");
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        chemistry.setItem(ChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.REFINED_AROMATIC_OIL.get()));
        tickChemistry(helper, chemistry, 250);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT), ModItems.AROMATIC_EXTRACT.get(), 1,
                "An incompatible aromatic output consumed its ingredient");
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.FUEL_SLOT), Items.COAL, 1,
                "A blocked aromatic output consumed fresh fuel");
        chemistry.setItem(ChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.PHENETHYLAMINE_BASE.get(), 64));
        tickChemistry(helper, chemistry, 250);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT), ModItems.AROMATIC_EXTRACT.get(), 1,
                "A full aromatic output consumed its ingredient");
        chemistry.removeItemNoUpdate(ChemistryTableBlockEntity.OUTPUT_SLOT);
        tickChemistry(helper, chemistry, 199);
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "A blocked conversion retained invalid progress");
        tickChemistry(helper, chemistry, 1);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.PHENETHYLAMINE_BASE.get(), 1,
                "The unblocked aromatic conversion did not resume safely");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void bothBrewSlotOrdersReturnExactlyTheExtraSolventBottles(GameTestHelper helper) {
        for (boolean reversed : new boolean[]{false, true}) {
            BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
            int baseSlot = reversed ? BrewCauldronBlockEntity.WATER_SLOT : BrewCauldronBlockEntity.BARK_SLOT;
            int solventSlot = reversed ? BrewCauldronBlockEntity.BARK_SLOT : BrewCauldronBlockEntity.WATER_SLOT;
            brew.setItem(baseSlot, new ItemStack(ModItems.PHENETHYLAMINE_BASE.get(), 2));
            brew.setItem(solventSlot, new ItemStack(ModItems.DISTILLED_SOLVENT.get(), 2));
            brew.setItem(BrewCauldronBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
            int looseBefore = looseBottles(helper, brew);
            tickBrew(helper, brew, 219);
            helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT).isEmpty(), "2C-B brewing completed before its normal cycle");
            tickBrew(helper, brew, 221);
            assertStack(helper, brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT), ModItems.DIMETHOXY_INTERMEDIATE.get(), 2,
                    "The two brewing cycles did not preserve the requested intermediate yield");
            helper.assertTrue(brew.getItem(baseSlot).isEmpty(), "The primary base bottle returned an extra phantom bottle");
            assertStack(helper, brew.getItem(solventSlot), Items.GLASS_BOTTLE, 1, "The final solvent did not return its empty bottle");
            helper.assertTrue(looseBottles(helper, brew) - looseBefore == 1,
                    "The stacked solvent remainder was lost or duplicated while its input slot was occupied");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void wrongSolventAndBlockedBrewOutputPreserveIngredientsAndFuel(GameTestHelper helper) {
        BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
        brew.setItem(BrewCauldronBlockEntity.BARK_SLOT, new ItemStack(ModItems.PHENETHYLAMINE_BASE.get()));
        brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(ModItems.ETHANOL.get()));
        brew.setItem(BrewCauldronBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickBrew(helper, brew, 250);
        helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT).isEmpty(), "Ordinary Ethanol incorrectly substituted for Distilled Ethanol");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.FUEL_SLOT), Items.COAL, 1, "The wrong solvent consumed fuel");
        brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(ModItems.DISTILLED_SOLVENT.get()));
        brew.setItem(BrewCauldronBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get(), 64));
        tickBrew(helper, brew, 250);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.PHENETHYLAMINE_BASE.get(), 1,
                "A full output consumed the base");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), ModItems.DISTILLED_SOLVENT.get(), 1,
                "A full output consumed the solvent");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.FUEL_SLOT), Items.COAL, 1, "A full output consumed fresh fuel");
        brew.setItem(BrewCauldronBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.MET_INTERMEDIATE.get()));
        tickBrew(helper, brew, 250);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.PHENETHYLAMINE_BASE.get(), 1,
                "An incompatible output consumed the base");
        brew.removeItemNoUpdate(BrewCauldronBlockEntity.OUTPUT_SLOT);
        tickBrew(helper, brew, 220);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT), ModItems.DIMETHOXY_INTERMEDIATE.get(), 1,
                "The cleared cauldron did not complete the recipe");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void freebaseHeatsThroughAllRoutesAndSavedPartialCrystalGrindsRemainingContent(GameTestHelper helper) {
        ChemistryTableBlockEntity chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get()));
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickChemistry(helper, chemistry, 200);
        assertStack(helper, chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.TWO_CB_FREEBASE.get(), 1,
                "Dimethoxy Intermediate did not become one 2C-B Freebase");
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).isEmpty(), "The intermediate bottle must become the freebase bottle");
        assertHeating(helper, RecipeType.SMELTING);
        assertHeating(helper, RecipeType.BLASTING);
        assertHeating(helper, RecipeType.SMOKING);
        assertHeating(helper, RecipeType.CAMPFIRE_COOKING);

        MortarAndPestleBlockEntity mortar = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, new ItemStack(ModItems.TWO_CB_CRYSTAL.get()));
        tickMortar(helper, mortar, 40);
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.TWO_CB_POWDER.get(), 50,
                "A full 5000mg crystal did not yield fifty 100mg powders");
        ItemStack partial = new ItemStack(ModItems.TWO_CB_CRYSTAL.get());
        helper.assertTrue(TwoCbCrystalItem.consumeMilligrams(partial, 1000), "Could not take a 1000mg crystal portion");
        partial = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) partial.save(helper.getLevel().registryAccess()));
        helper.assertTrue(TwoCbCrystalItem.remainingMilligrams(partial) == 4000, "A saved partly consumed crystal refilled itself");
        mortar = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, partial);
        tickMortar(helper, mortar, 20);
        CompoundTag saved = mortar.saveWithoutMetadata(helper.getLevel().registryAccess());
        MortarAndPestleBlockEntity restored = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        tickMortar(helper, restored, 20);
        assertStack(helper, restored.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.TWO_CB_POWDER.get(), 40,
                "Saved partial content or saved mortar progress changed its yield");
        helper.assertTrue(restored.getItem(MortarAndPestleBlockEntity.INPUT_SLOT).isEmpty(), "Grinding duplicated the partly consumed crystal");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void mortarPreservesCrystalUntilItsWholeRemainingYieldFits(GameTestHelper helper) {
        MortarAndPestleBlockEntity mortar = place(helper, ModBlocks.MORTAR_AND_PESTLE, MortarAndPestleBlockEntity.class);
        mortar.setItem(MortarAndPestleBlockEntity.INPUT_SLOT, new ItemStack(ModItems.TWO_CB_CRYSTAL.get()));
        mortar.setItem(MortarAndPestleBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CB_POWDER.get(), 15));
        tickMortar(helper, mortar, 80);
        helper.assertTrue(TwoCbCrystalItem.remainingMilligrams(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT)) == 5000,
                "An overflowing powder output consumed crystal content");
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.TWO_CB_POWDER.get(), 15,
                "The blocked mortar changed its output");
        mortar.setItem(MortarAndPestleBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.HO_MET_POWDER.get()));
        tickMortar(helper, mortar, 80);
        helper.assertTrue(TwoCbCrystalItem.remainingMilligrams(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT)) == 5000,
                "An incompatible powder output consumed the crystal");
        mortar.setItem(MortarAndPestleBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CB_POWDER.get(), 14));
        tickMortar(helper, mortar, 40);
        assertStack(helper, mortar.getItem(MortarAndPestleBlockEntity.OUTPUT_SLOT), ModItems.TWO_CB_POWDER.get(), 64,
                "The mortar did not combine the entire crystal yield when exactly enough space was available");
        helper.assertTrue(mortar.getItem(MortarAndPestleBlockEntity.INPUT_SLOT).isEmpty(), "A successfully ground crystal remained in its slot");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void allFourSugarPillFormatsDistributeTheEntireCrystalMass(GameTestHelper helper) {
        List<Batch> batches = List.of(new Batch(0, ModItems.TWO_CB_25MG.get(), 25, 4, 1, 200),
                new Batch(1, ModItems.TWO_CB_100MG.get(), 100, 1, 1, 50),
                new Batch(2, ModItems.TWO_CB_250MG.get(), 250, 2, 5, 20),
                new Batch(3, ModItems.TWO_CB_500MG.get(), 500, 1, 5, 10));
        for (Batch batch : batches) {
            AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
            table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.TWO_CB_POWDER.get(), 50));
            int suppliedSugar = Math.min(64, batch.total());
            table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR, suppliedSugar));
            table.selectProductByButton(batch.button());
            helper.assertTrue(table.previewOutputSummary().equals(batch.output() + "x " + batch.amount() + "mg 2C-B pills"),
                    "The dosage preview did not show the exact pill output");
            helper.assertTrue(table.previewRequirementSummary().equals(batch.powder() + " powder (100mg) + " + batch.output() + " sugar"),
                    "The dosage preview did not show the exact powder and sugar batch");
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
                assertStack(helper, output, batch.item(), batch.output(), "The selected dose produced an incorrect pill batch");
                helper.assertTrue(((TwoCbDoseItem) output.getItem()).doseMilligrams() == batch.amount(),
                        "Crafting changed an existing pill's effect dose");
                assertInputs(helper, table, powderBefore - batch.powder(), sugarBefore - batch.output(),
                        "The selected pill batch consumed incorrect powder or sugar quantities");
                packaged += output.getCount();
            }
            helper.assertTrue(packaged * batch.amount() == 5000 && suppliedSugar == packaged,
                    "A full crystal's mass was lost or extra sugar was supplied");
            helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).isEmpty()
                            && table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).isEmpty(),
                    "Full-crystal packaging left undistributed powder or sugar");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void incompleteAndBlockedPillBatchesKeepBothInputs(GameTestHelper helper) {
        AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        table.selectProductByButton(0);
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.TWO_CB_POWDER.get()));
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR, 3));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 1, 3, "A 25mg batch consumed inputs without all four sugar pieces");
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR, 4));
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CB_25MG.get(), 13));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 1, 4, "A 25mg batch overflowed the existing pill stack");
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.HO_MET_25MG.get()));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 1, 4, "An incompatible output consumed pill ingredients");
        table.removeItemNoUpdate(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT);
        tickAdvanced(helper, table, 220);
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.TWO_CB_25MG.get(), 4,
                "The unblocked 25mg batch did not yield four pills");

        table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        table.selectProductByButton(2);
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.TWO_CB_POWDER.get(), 4));
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR, 2));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 4, 2, "A 250mg batch rounded four powders up to 500mg");
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.TWO_CB_POWDER.get(), 5));
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.TWO_CB_250MG.get(), 15));
        tickAdvanced(helper, table, 250);
        assertInputs(helper, table, 5, 2, "A 250mg batch overflowed the pill stack");
        table.removeItemNoUpdate(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT);
        tickAdvanced(helper, table, 220);
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.TWO_CB_250MG.get(), 2,
                "The 250mg batch lost its half-powder remainder instead of making two pills");
        table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, new ItemStack(ModItems.HO_MET_POWDER.get()));
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.SUGAR));
        tickAdvanced(helper, table, 250);
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "4-HO-MET powder incorrectly used the 2C-B sugar recipe");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void menuRoutingAcceptsEitherBrewArrivalOrderAndPreservesBlockedInputs(GameTestHelper helper) {
        for (boolean solventFirst : new boolean[]{false, true}) {
            ServerPlayer player = player(helper);
            BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
            BrewCauldronMenu menu = new BrewCauldronMenu(1, player.getInventory(), brew, new SimpleContainerData(4));
            ItemStack base = new ItemStack(ModItems.PHENETHYLAMINE_BASE.get(), 3);
            CustomData.update(DataComponents.CUSTOM_DATA, base, tag -> tag.putInt("TwoCbRoutingMarker", 123));
            ItemStack savedBase = base.copy();
            player.getInventory().setItem(9, solventFirst ? new ItemStack(ModItems.DISTILLED_SOLVENT.get(), 4) : base);
            player.getInventory().setItem(10, solventFirst ? base : new ItemStack(ModItems.DISTILLED_SOLVENT.get(), 4));
            helper.assertFalse(menu.quickMoveStack(player, 5).isEmpty(), "The first 2C-B brewing ingredient did not shift into the cauldron");
            helper.assertFalse(menu.quickMoveStack(player, 6).isEmpty(), "The second 2C-B brewing ingredient did not shift into the cauldron");
            assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.PHENETHYLAMINE_BASE.get(), 3,
                    "Either-order shift-click failed to route the base to its primary slot");
            assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), ModItems.DISTILLED_SOLVENT.get(), 4,
                    "Either-order shift-click failed to route solvent to the liquid slot");
            helper.assertTrue(ItemStack.isSameItemSameComponents(savedBase, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT)),
                    "Shift-click lost the base stack's saved components");
            helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.LEAF_SLOT).isEmpty()
                            && player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(10).isEmpty(),
                    "Either-order routing duplicated stock or put solvent in the optional leaf slot");
            brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(Items.GLASS_BOTTLE));
            player.getInventory().setItem(11, new ItemStack(ModItems.DISTILLED_SOLVENT.get(), 2));
            helper.assertTrue(menu.quickMoveStack(player, 7).isEmpty(), "Blocked 2C-B solvent was redirected into an unrelated recipe slot");
            assertStack(helper, player.getInventory().getItem(11), ModItems.DISTILLED_SOLVENT.get(), 2,
                    "A blocked solvent shift-click lost inventory stock");
            helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.LEAF_SLOT).isEmpty(), "A blocked solvent shift-click changed the leaf slot");
            brew.clearContent();
            brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(ModItems.PHENETHYLAMINE_BASE.get()));
            helper.assertFalse(menu.quickMoveStack(player, 7).isEmpty(), "Direct reversed base placement did not route shifted solvent into primary");
            assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.DISTILLED_SOLVENT.get(), 2,
                    "The reversed brew order lost or duplicated solvent");
            brew.clearContent();
            menu.setItem(BrewCauldronBlockEntity.BARK_SLOT, 0, new ItemStack(Items.GLASS_BOTTLE));
            assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), Items.GLASS_BOTTLE, 1,
                    "A reversed recipe's empty bottle was discarded by the client slot setter");
            helper.assertFalse(menu.getSlot(BrewCauldronBlockEntity.BARK_SLOT).mayPlace(new ItemStack(Items.GLASS_BOTTLE)),
                    "A returned bottle update accidentally allowed player glass insertion into primary");
            helper.assertFalse(menu.quickMoveStack(player, BrewCauldronBlockEntity.BARK_SLOT).isEmpty(),
                    "The extra bottle from a reversed recipe could not shift back to inventory");
            helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.BARK_SLOT).isEmpty(), "Returned bottle shift-out duplicated glass");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void pillMenuRoutesSugarAndPowderAndPersistsChangedDosage(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        AdvancedChemistryTableMenu menu = (AdvancedChemistryTableMenu) table.createMenu(1, player.getInventory(), player);
        player.getInventory().setItem(9, new ItemStack(Items.SUGAR, 6));
        player.getInventory().setItem(10, new ItemStack(ModItems.TWO_CB_POWDER.get(), 6));
        helper.assertFalse(menu.quickMoveStack(player, 3).isEmpty(), "Sugar did not shift into the table");
        helper.assertTrue(menu.getPreviewOutputSummary(0).equals("4x 25mg 2C-B pills"), "Sugar-first placement did not expose the 2C-B preview");
        helper.assertFalse(menu.quickMoveStack(player, 4).isEmpty(), "2C-B powder did not shift into the table");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT), ModItems.TWO_CB_POWDER.get(), 6,
                "Powder did not route to primary");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT), Items.SUGAR, 6,
                "Sugar did not route to secondary");
        helper.assertTrue(menu.clickMenuButton(player, 2) && menu.getSelectedProductId() == 2, "The actual pill menu did not synchronize dosage selection");
        tickAdvanced(helper, table, 100);
        helper.assertTrue(menu.clickMenuButton(player, 0), "Could not change pill dosage mid-craft");
        helper.assertTrue(table.saveWithoutMetadata(helper.getLevel().registryAccess()).getInt("RefineProgress") == 0,
                "Changing the selected pill dosage failed to reset progress");
        tickAdvanced(helper, table, 20);
        CompoundTag saved = table.saveWithoutMetadata(helper.getLevel().registryAccess());
        AdvancedChemistryTableBlockEntity restored = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.selectedProduct().buttonId() == 0, "The selected pill dosage was lost across block save/load");
        tickAdvanced(helper, restored, 200);
        assertStack(helper, restored.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.TWO_CB_25MG.get(), 4,
                "A restored changed-dosage recipe produced the old format or wrong quantity");
        assertInputs(helper, restored, 5, 2, "A restored 25mg pill recipe used incorrect ingredient counts");
        AdvancedChemistryTableMenu restoredMenu = (AdvancedChemistryTableMenu) restored.createMenu(2, player.getInventory(), player);
        helper.assertFalse(restoredMenu.quickMoveStack(player, AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(),
                "Finished pills could not shift into player inventory");
        int pills = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(ModItems.TWO_CB_25MG.get())) pills += player.getInventory().getItem(slot).getCount();
        }
        helper.assertTrue(pills == 4 && restored.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(),
                "Finished-pill shift-out lost or duplicated pills");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "two-cb-crafting-test"));
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
        SingleRecipeInput input = new SingleRecipeInput(new ItemStack(ModItems.TWO_CB_FREEBASE.get()));
        var recipe = helper.getLevel().getRecipeManager().getRecipeFor(type, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "Missing 2C-B freebase heating route for " + type);
        ItemStack result = recipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
        assertStack(helper, result, ModItems.TWO_CB_CRYSTAL.get(), 1, "Heating produced an incorrect crystal output");
        helper.assertTrue(TwoCbCrystalItem.remainingMilligrams(result) == 5000 && input.item().getCount() == 1,
                "Heating refilled a partial result or consumed input during recipe preview");
    }

    private static void assertInputs(GameTestHelper helper, AdvancedChemistryTableBlockEntity table, int powder, int sugar, String message) {
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).getCount() == powder
                && table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).getCount() == sugar, message);
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count, String message) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, message + ": " + stack);
    }

    private record Batch(int button, Item item, int amount, int output, int powder, int total) {
    }
}
