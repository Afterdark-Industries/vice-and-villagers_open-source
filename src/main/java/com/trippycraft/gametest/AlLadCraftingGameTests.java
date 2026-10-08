package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.block.entity.AdvancedChemistryTableBlockEntity;
import com.trippycraft.block.entity.BrewCauldronBlockEntity;
import com.trippycraft.block.entity.ChemistryTableBlockEntity;
import com.trippycraft.crafting.CapsuleBlendRecipe;
import com.trippycraft.item.AlLadConcentrateItem;
import com.trippycraft.item.AlLadDoseItem;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Supplier;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlLadCraftingGameTests {
    private static final BlockPos STATION = new BlockPos(3, 2, 3);
    private static final int BREW_CYCLE = 220;
    private static final int CHEMISTRY_CYCLE = 200;
    private static final int ADVANCED_CYCLE = 220;
    private static final String CONTENT_KEY = "TrippyCraftAlLadConcentrateRemaining";

    private AlLadCraftingGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void extractAndReagentProduceIntermediateWithOneEmptyBottle(GameTestHelper helper) {
        BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
        brew.setItem(BrewCauldronBlockEntity.BARK_SLOT, new ItemStack(ModItems.LYSERGIC_EXTRACT.get()));
        brew.setItem(BrewCauldronBlockEntity.WATER_SLOT, new ItemStack(ModItems.AMINOETHYL_REAGENT.get()));
        tickBrew(helper, brew, BREW_CYCLE);
        helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT).isEmpty(), "AL-LAD brew ran without fuel");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.LYSERGIC_EXTRACT.get(), 1,
                "Unfueled brew consumed its extract");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), ModItems.AMINOETHYL_REAGENT.get(), 1,
                "Unfueled brew consumed its reagent");
        brew.setItem(BrewCauldronBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickBrew(helper, brew, BREW_CYCLE - 1);
        helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT).isEmpty(), "Intermediate was produced before its brew completed");
        tickBrew(helper, brew, 1);
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.OUTPUT_SLOT), ModItems.ALLYL_LYSERGIC_INTERMEDIATE.get(), 1,
                "One extract and one reagent must produce one intermediate");
        helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.BARK_SLOT).isEmpty(),
                "Lysergic Extract must be consumed without an extra glass return");
        helper.assertTrue(brew.getItem(BrewCauldronBlockEntity.LEAF_SLOT).isEmpty(), "The recipe must leave the leaf slot empty");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), Items.GLASS_BOTTLE, 1,
                "Exactly the extra reagent bottle must return empty");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void chemistryProducesFullConcentrateWithoutDuplicatingGlass(GameTestHelper helper) {
        ChemistryTableBlockEntity chemistry = place(helper, ModBlocks.CHEMISTRY_TABLE, ChemistryTableBlockEntity.class);
        chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.ALLYL_LYSERGIC_INTERMEDIATE.get()));
        chemistry.setItem(ChemistryTableBlockEntity.FUEL_SLOT, new ItemStack(Items.COAL));
        tickChemistry(helper, chemistry, CHEMISTRY_CYCLE);
        ItemStack output = chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT);
        assertStack(helper, output, ModItems.AL_LAD_CONCENTRATE.get(), 1, "One intermediate must produce one concentrate bottle");
        helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(output) == 5000, "Chemistry produced a partly filled concentrate");
        helper.assertTrue(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).isEmpty(),
                "The intermediate bottle must become the concentrate bottle without returning phantom glass");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void allSelectedTabSizesPackageEveryFullBottleCharge(GameTestHelper helper) {
        for (TabCase format : formats()) {
            AdvancedChemistryTableBlockEntity table = newTable(helper, format, new ItemStack(ModItems.AL_LAD_CONCENTRATE.get()), 5);
            int totalTabs = 0;
            for (int cycle = 1; cycle <= 5; cycle++) {
                tickAdvanced(helper, table, ADVANCED_CYCLE);
                ItemStack tabs = table.removeItemNoUpdate(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT);
                assertStack(helper, tabs, format.item(), format.tabsPerCycle(), "Wrong selected AL-LAD batch size");
                totalTabs += tabs.getCount();
                helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).getCount() == 5 - cycle,
                        "The table must consume exactly one sheet per 1000mg cycle");
                assertConcentrateOrEmptyBottle(helper, table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT),
                        5000 - cycle * 1000, "Wrong remaining concentrate after cycle " + cycle);
            }
            helper.assertTrue(totalTabs == format.totalTabs(), "Wrong total full-bottle tab yield for " + format.amountMilligrams() + "mg");
            helper.assertTrue(totalTabs * format.amountMilligrams() == 5000, "The selected format lost or created concentrate content");
            tickAdvanced(helper, table, ADVANCED_CYCLE);
            helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "Exhausted glass produced an extra batch");
            assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT), Items.GLASS_BOTTLE, 1,
                    "The final empty concentrate bottle must stay in the primary slot");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void consumedConcentratePackagesOnlyItsFourRemainingCharges(GameTestHelper helper) {
        for (TabCase format : formats()) {
            ItemStack concentrate = new ItemStack(ModItems.AL_LAD_CONCENTRATE.get());
            helper.assertTrue(AlLadConcentrateItem.consumeMilligrams(concentrate, 1000), "Could not simulate one concentrate portion");
            AdvancedChemistryTableBlockEntity table = newTable(helper, format, roundTrip(helper, concentrate), 5);
            int totalTabs = 0;
            for (int cycle = 0; cycle < 5; cycle++) {
                tickAdvanced(helper, table, ADVANCED_CYCLE);
                ItemStack tabs = table.removeItemNoUpdate(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT);
                if (cycle < 4) {
                    assertStack(helper, tabs, format.item(), format.tabsPerCycle(), "Wrong partial-concentrate batch size");
                    totalTabs += tabs.getCount();
                } else {
                    helper.assertTrue(tabs.isEmpty(), "A previously consumed portion was packaged a second time");
                }
            }
            helper.assertTrue(totalTabs * format.amountMilligrams() == 4000,
                    "Packaging after a 1000mg portion must preserve only the remaining 4000mg");
            assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT), ModItems.BLOTTER_SHEET.get(), 1,
                    "An exhausted concentrate must leave the fifth sheet unconsumed");
            assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT), Items.GLASS_BOTTLE, 1,
                    "Partial concentrate failed to return its final empty bottle");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void blockedOutputsAndWrongPaperPreserveConcentrateCharge(GameTestHelper helper) {
        TabCase format = formats().getFirst();
        AdvancedChemistryTableBlockEntity table = newTable(helper, format, new ItemStack(ModItems.AL_LAD_CONCENTRATE.get()), 5);
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.AL_LAD_25MG.get(), 64));
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertUnchangedInputs(helper, table, 5000, 5, "Full output consumed concentrate or paper");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.AL_LAD_25MG.get(), 64,
                "The full output stack changed while blocked");
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.AL_LAD_25MG.get(), 25));
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertUnchangedInputs(helper, table, 5000, 5, "Insufficient space for forty tabs consumed inputs");
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.AL_LAD_100MG.get()));
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertUnchangedInputs(helper, table, 5000, 5, "An incompatible output consumed inputs");

        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, ItemStack.EMPTY);
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertUnchangedInputs(helper, table, 5000, 0, "Missing paper consumed concentrate");
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(Items.GLASS_BOTTLE));
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT)) == 5000,
                "A Glass Bottle was accepted instead of a Blotter Sheet");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT), Items.GLASS_BOTTLE, 1,
                "Wrong secondary input was consumed");
        helper.assertFalse(table.canPlaceItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT,
                new ItemStack(ModItems.ALLYL_LYSERGIC_INTERMEDIATE.get())), "Unrefined intermediate should not enter the concentrate slot");

        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(ModItems.BLOTTER_SHEET.get()));
        table.setItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, new ItemStack(ModItems.AL_LAD_25MG.get(), 24));
        tickAdvanced(helper, table, ADVANCED_CYCLE);
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.AL_LAD_25MG.get(), 64,
                "The batch should complete when exactly forty output spaces are free");
        assertUnchangedInputs(helper, table, 4000, 0, "Successful resumed cycle used the wrong charge or sheet count");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void machineAndItemPersistenceKeepChargeAndFinalGlassRemainder(GameTestHelper helper) {
        ItemStack partial = new ItemStack(ModItems.AL_LAD_CONCENTRATE.get());
        AlLadConcentrateItem.consumeMilligrams(partial, 1000);
        ItemStack syncedPartial = streamRoundTrip(helper, roundTrip(helper, partial));
        helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(syncedPartial) == 4000,
                "Partial concentrate content changed across item save and network codecs");
        AdvancedChemistryTableBlockEntity table = newTable(helper, formats().get(2), syncedPartial, 4);
        tickAdvanced(helper, table, 100);
        CompoundTag saved = table.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(saved.getInt("RefineProgress") == 100, "The machine did not save its active progress");
        AdvancedChemistryTableBlockEntity restored = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.selectedProduct().buttonId() == 2, "The selected tab size was lost across machine save/load");
        assertUnchangedInputs(helper, restored, 4000, 4, "Machine save/load changed the stored charge or sheets");
        tickAdvanced(helper, restored, ADVANCED_CYCLE - 100);
        assertStack(helper, restored.removeItemNoUpdate(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.AL_LAD_250MG.get(), 4,
                "A restored machine did not resume the saved partial cycle");
        assertUnchangedInputs(helper, restored, 3000, 3, "The first resumed cycle used the wrong inputs");
        tickAdvanced(helper, restored, ADVANCED_CYCLE * 3);
        assertStack(helper, restored.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.AL_LAD_250MG.get(), 12,
                "Restored machine did not package its three final charges");
        assertStack(helper, restored.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT), Items.GLASS_BOTTLE, 1,
                "Exhausted concentrate did not leave its final glass bottle in the machine");

        CompoundTag exhaustedSave = restored.saveWithoutMetadata(helper.getLevel().registryAccess());
        AdvancedChemistryTableBlockEntity exhausted = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        exhausted.loadWithComponents(exhaustedSave, helper.getLevel().registryAccess());
        ItemStack syncedGlass = streamRoundTrip(helper, exhausted.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT));
        assertStack(helper, syncedGlass, Items.GLASS_BOTTLE, 1, "The final glass remainder was lost across machine save/load or item sync");
        helper.assertTrue(exhausted.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).isEmpty(), "Consumed sheets returned after machine reload");
        assertStack(helper, exhausted.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.AL_LAD_250MG.get(), 12,
                "Saved final tabs changed across machine reload");
        tickAdvanced(helper, exhausted, ADVANCED_CYCLE);
        assertStack(helper, exhausted.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT), ModItems.AL_LAD_250MG.get(), 12,
                "Reloaded empty glass produced a phantom concentrate batch");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void concentrateGuardsRejectInvalidAmountsAndStackedReservoirs(GameTestHelper helper) {
        ItemStack fresh = new ItemStack(ModItems.AL_LAD_CONCENTRATE.get());
        helper.assertTrue(fresh.getMaxStackSize() == 1, "Concentrate remaining content must belong to one bottle");
        helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(fresh) == 5000,
                "A fresh bottle without a saved remaining-content key must begin full");
        for (int invalid : new int[]{0, -1000, 500, 6000}) {
            helper.assertFalse(AlLadConcentrateItem.consumeMilligrams(fresh, invalid), "Invalid concentrate amount was accepted: " + invalid);
            helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(fresh) == 5000, "A rejected use changed fresh bottle content");
        }
        helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(new ItemStack(Items.GLASS_BOTTLE)) == 0,
                "Empty glass must not report concentrate content");
        ItemStack countZero = new ItemStack(ModItems.AL_LAD_CONCENTRATE.get());
        countZero.setCount(0);
        helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(countZero) == 0, "A zero-count item reported concentrate content");
        helper.assertFalse(AlLadConcentrateItem.consumeMilligrams(countZero, 1000), "A zero-count item produced a portion");

        ItemStack stacked = new ItemStack(ModItems.AL_LAD_CONCENTRATE.get(), 2);
        helper.assertFalse(AlLadConcentrateItem.consumeMilligrams(stacked, 1000), "A malformed two-bottle stack must not share one reservoir");
        AdvancedChemistryTableBlockEntity table = newTable(helper, formats().get(1), stacked, 1);
        tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT), ModItems.AL_LAD_CONCENTRATE.get(), 2,
                "A rejected concentrate stack was consumed by the table");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT), ModItems.BLOTTER_SHEET.get(), 1,
                "A rejected concentrate stack consumed its sheet");
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "A malformed stack produced tabs");

        for (int invalidRemaining : new int[]{-1000, 0, 999}) {
            ItemStack corrupted = concentrateWithContent(invalidRemaining);
            helper.assertFalse(AlLadConcentrateItem.consumeMilligrams(corrupted, 1000), "Invalid saved content produced a portion");
            table = newTable(helper, formats().get(1), roundTrip(helper, corrupted), 1);
            tickAdvanced(helper, table, ADVANCED_CYCLE * 2);
            helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(), "Insufficient saved content produced tabs");
            assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT), ModItems.BLOTTER_SHEET.get(), 1,
                    "Insufficient saved content consumed paper");
        }
        helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(concentrateWithContent(9000)) == 5000,
                "Corrupted content above capacity must be clamped to 5000mg");
        for (int portion = 1; portion <= 5; portion++) {
            helper.assertTrue(AlLadConcentrateItem.consumeMilligrams(fresh, 1000), "Fresh concentrate rejected valid portion " + portion);
            fresh = roundTrip(helper, fresh);
            helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(fresh) == 5000 - portion * 1000,
                    "Saved concentrate lost its exact remaining content");
        }
        helper.assertTrue(fresh.isEmpty(), "Five portions must exhaust the concentrate item");
        helper.assertFalse(AlLadConcentrateItem.consumeMilligrams(fresh, 1000), "Exhausted concentrate produced a sixth portion");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void renamedTabsAndCapsulesPreserveSeparateContentAndEffectAmounts(GameTestHelper helper) {
        assertTabTier(helper, ModItems.AL_LAD_25MG.get(), 25, 75);
        assertTabTier(helper, ModItems.AL_LAD_100MG.get(), 100, 150);
        assertTabTier(helper, ModItems.AL_LAD_250MG.get(), 250, 300);
        assertTabTier(helper, ModItems.AL_LAD_500MG.get(), 500, 450);
        helper.assertTrue(AlLadConcentrateItem.EFFECT_DOSE_PER_USE == ((AlLadDoseItem) ModItems.AL_LAD_500MG.get()).doseMicrograms() * 2,
                "A 1000mg concentrate portion must equal two preserved 500mg tab effects");

        CapsuleBlendRecipe recipe = new CapsuleBlendRecipe(CraftingBookCategory.MISC);
        CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(ModItems.EMPTY_CAPSULE.get()),
                new ItemStack(ModItems.AL_LAD_25MG.get(), 3), new ItemStack(ModItems.AL_LAD_100MG.get())));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "New AL-LAD tab formats were rejected by native capsule crafting");
        ItemStack capsule = recipe.assemble(input, helper.getLevel().registryAccess());
        List<CapsuleBlend.Entry> entries = CapsuleBlend.read(streamRoundTrip(helper, roundTrip(helper, capsule)));
        helper.assertTrue(entries.size() == 1, "Same-substance tabs failed to merge into one capsule entry");
        CapsuleBlend.Entry entry = entries.getFirst();
        helper.assertTrue(entry.kind().equals("al_lad_milligrams"), "New capsules must store milligram content under the new AL-LAD kind");
        helper.assertTrue(entry.amount() == 125 && entry.effectAmount() == 225,
                "Capsule must separately preserve 125mg content and 225ug calibrated effects");
        helper.assertTrue(CapsuleBlend.doseLabel(entry).equals("125mg"), "New capsule label must display milligram content");
        helper.assertTrue(input.getItem(1).getCount() == 3, "Crafting preview consumed the source stack or counted all stacked tabs");
        helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Blotter tabs returned an unintended container");
        CompoundTag savedData = capsule.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag encoded = savedData.getCompound("TrippyCraftCapsuleBlend").getList("Entries", 10).getCompound(0);
        helper.assertTrue(encoded.getDouble("Amount") == 125 && encoded.getDouble("EffectAmount") == 225,
                "Capsule serialization did not store content and effect amounts separately");

        ItemStack legacy = new ItemStack(ModItems.BLEND_CAPSULE.get());
        CompoundTag legacyEntry = new CompoundTag();
        legacyEntry.putString("Kind", "al_lad_micrograms");
        legacyEntry.putDouble("Amount", 225);
        ListTag legacyEntries = new ListTag();
        legacyEntries.add(legacyEntry);
        CompoundTag legacyRoot = new CompoundTag();
        legacyRoot.put("Entries", legacyEntries);
        CustomData.update(DataComponents.CUSTOM_DATA, legacy, tag -> tag.put("TrippyCraftCapsuleBlend", legacyRoot));
        List<CapsuleBlend.Entry> restoredLegacy = CapsuleBlend.read(roundTrip(helper, legacy));
        helper.assertTrue(restoredLegacy.size() == 1, "Legacy AL-LAD capsule contents were lost");
        CapsuleBlend.Entry legacyDose = restoredLegacy.getFirst();
        helper.assertTrue(legacyDose.kind().equals("al_lad_micrograms") && legacyDose.amount() == 225 && legacyDose.effectAmount() == 225,
                "Legacy capsules must preserve their original calibrated microgram amount");
        helper.assertTrue(CapsuleBlend.doseLabel(legacyDose).equals("225ug"), "Legacy capsule amount was relabeled as new milligram content");
        helper.succeed();
    }

    private static List<TabCase> formats() {
        return List.of(new TabCase(0, ModItems.AL_LAD_25MG.get(), 25, 40, 200),
                new TabCase(1, ModItems.AL_LAD_100MG.get(), 100, 10, 50),
                new TabCase(2, ModItems.AL_LAD_250MG.get(), 250, 4, 20),
                new TabCase(3, ModItems.AL_LAD_500MG.get(), 500, 2, 10));
    }

    private static AdvancedChemistryTableBlockEntity newTable(GameTestHelper helper, TabCase format, ItemStack concentrate, int sheets) {
        AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        table.selectProductByButton(format.buttonId());
        table.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, concentrate);
        table.setItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT, new ItemStack(ModItems.BLOTTER_SHEET.get(), sheets));
        return table;
    }

    private static <T extends BlockEntity> T place(GameTestHelper helper, Supplier<Block> block, Class<T> type) {
        BlockPos position = helper.absolutePos(STATION);
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(position, block.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity entity = helper.getLevel().getBlockEntity(position);
        helper.assertTrue(type.isInstance(entity), "Expected placed machine block entity: " + type.getSimpleName());
        return type.cast(entity);
    }

    private static void tickBrew(GameTestHelper helper, BrewCauldronBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            BrewCauldronBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(),
                    helper.getLevel().getBlockState(machine.getBlockPos()), machine);
        }
    }

    private static void tickChemistry(GameTestHelper helper, ChemistryTableBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            ChemistryTableBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(),
                    helper.getLevel().getBlockState(machine.getBlockPos()), machine);
        }
    }

    private static void tickAdvanced(GameTestHelper helper, AdvancedChemistryTableBlockEntity machine, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            AdvancedChemistryTableBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(),
                    helper.getLevel().getBlockState(machine.getBlockPos()), machine);
        }
    }

    private static void assertUnchangedInputs(GameTestHelper helper, AdvancedChemistryTableBlockEntity table,
                                               int expectedMilligrams, int expectedSheets, String message) {
        helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT)) == expectedMilligrams
                && table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT).getCount() == expectedSheets, message);
    }

    private static void assertConcentrateOrEmptyBottle(GameTestHelper helper, ItemStack input, int remaining, String message) {
        if (remaining == 0) {
            assertStack(helper, input, Items.GLASS_BOTTLE, 1, message);
        } else {
            assertStack(helper, input, ModItems.AL_LAD_CONCENTRATE.get(), 1, message);
            helper.assertTrue(AlLadConcentrateItem.remainingMilligrams(input) == remaining, message);
        }
    }

    private static void assertTabTier(GameTestHelper helper, Item item, int milligrams, double micrograms) {
        helper.assertTrue(item instanceof AlLadDoseItem, "AL-LAD format is not a native dose item");
        AlLadDoseItem tab = (AlLadDoseItem) item;
        helper.assertTrue(tab.amountMilligrams() == milligrams && tab.doseMicrograms() == micrograms,
                "AL-LAD tab label or preserved effect tier changed for " + milligrams + "mg");
        helper.assertTrue(new ItemStack(item).getMaxStackSize() == 64, "AL-LAD tabs must support their forty-tab packaging batch");
    }

    private static ItemStack concentrateWithContent(int remaining) {
        ItemStack concentrate = new ItemStack(ModItems.AL_LAD_CONCENTRATE.get());
        CustomData.update(DataComponents.CUSTOM_DATA, concentrate, tag -> tag.putInt(CONTENT_KEY, remaining));
        return concentrate;
    }

    private static ItemStack roundTrip(GameTestHelper helper, ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CompoundTag saved = (CompoundTag) stack.save(helper.getLevel().registryAccess());
        return ItemStack.parseOptional(helper.getLevel().registryAccess(), saved);
    }

    private static ItemStack streamRoundTrip(GameTestHelper helper, ItemStack stack) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, stack);
            return ItemStack.STREAM_CODEC.decode(buffer);
        } finally {
            buffer.release();
        }
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count, String message) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, message + ": " + stack);
    }

    private record TabCase(int buttonId, Item item, int amountMilligrams, int tabsPerCycle, int totalTabs) {
    }
}
