package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.crafting.CapsuleBlendRecipe;
import com.trippycraft.guide.CursedFindingEntry;
import com.trippycraft.guide.CursedFindingIds;
import com.trippycraft.guide.CursedFindingsRegistry;
import com.trippycraft.guide.CursedRecipeCodex;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.SubstanceEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SalviaCraftingGameTests {
    private SalviaCraftingGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void bongRequiresExactGlassShapeAndReturnsWaterBucketRemainder(GameTestHelper helper) {
        List<ItemStack> slots = emptySlots(9);
        for (int slot : new int[]{1, 4, 6, 8}) slots.set(slot, new ItemStack(Items.GLASS, 4));
        slots.set(7, new ItemStack(Items.WATER_BUCKET));
        CraftingInput input = CraftingInput.of(3, 3, slots);
        CraftingRecipe recipe = crafting(helper, input, ModItems.BONG.get());
        List<ItemStack> remainders = recipe.getRemainingItems(input);
        int bucketCount = 0;
        for (int slot = 0; slot < remainders.size(); slot++) {
            ItemStack remainder = remainders.get(slot);
            if (slot == 7) {
                assertStack(helper, remainder, Items.BUCKET, 1, "Bong crafting did not return the water bucket's empty bucket");
                bucketCount += remainder.getCount();
            } else {
                helper.assertTrue(remainder.isEmpty(), "Bong crafting returned an unexpected glass remainder");
            }
        }
        helper.assertTrue(bucketCount == 1, "Bong crafting duplicated its bucket remainder");
        for (int slot : new int[]{1, 4, 6, 8}) {
            assertStack(helper, input.getItem(slot), Items.GLASS, 4, "Crafting preview consumed glass");
        }
        assertStack(helper, input.getItem(7), Items.WATER_BUCKET, 1, "Crafting preview consumed water");

        List<ItemStack> wrongShape = copySlots(slots);
        wrongShape.set(0, wrongShape.get(1));
        wrongShape.set(1, ItemStack.EMPTY);
        assertNoCrafting(helper, CraftingInput.of(3, 3, wrongShape), "A misplaced upper glass block still crafted a Bong");
        List<ItemStack> panes = copySlots(slots);
        panes.set(4, new ItemStack(Items.GLASS_PANE));
        assertNoCrafting(helper, CraftingInput.of(3, 3, panes), "Glass panes substituted for the required glass blocks");
        List<ItemStack> extra = copySlots(slots);
        extra.set(0, new ItemStack(Items.GLASS));
        assertNoCrafting(helper, CraftingInput.of(3, 3, extra), "An extra glass block was accepted by the Bong shape");
        List<ItemStack> dry = copySlots(slots);
        dry.set(7, new ItemStack(Items.BUCKET));
        assertNoCrafting(helper, CraftingInput.of(3, 3, dry), "An empty bucket substituted for water");
        assertNoCrafting(helper, CraftingInput.of(2, 2, List.of(new ItemStack(Items.GLASS), new ItemStack(Items.GLASS),
                new ItemStack(Items.GLASS), new ItemStack(Items.WATER_BUCKET))), "A 2x2 grid crafted the shaped Bong");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void driedLeafCountSelectsExtractStrengthInEveryInventoryArrangement(GameTestHelper helper) {
        List<Item> extracts = extracts();
        // Every subset of the inventory crafting grid is a legal shapeless arrangement.
        for (int mask = 1; mask < 16; mask++) {
            int leaves = Integer.bitCount(mask);
            List<ItemStack> slots = emptySlots(4);
            for (int slot = 0; slot < 4; slot++) {
                if ((mask & (1 << slot)) != 0) slots.set(slot, new ItemStack(ModItems.DRIED_SALVIA_LEAF.get(), 7));
            }
            CraftingInput input = CraftingInput.of(2, 2, slots);
            crafting(helper, input, extracts.get(leaves - 1));
            for (ItemStack stack : input.items()) {
                if (!stack.isEmpty()) assertStack(helper, stack, ModItems.DRIED_SALVIA_LEAF.get(), 7,
                        "Extract preview consumed leaves or treated a stack count as several ingredients");
            }
        }
        List<ItemStack> fiveLeaves = emptySlots(9);
        for (int slot = 0; slot < 5; slot++) fiveLeaves.set(slot, new ItemStack(ModItems.DRIED_SALVIA_LEAF.get()));
        assertNoCrafting(helper, CraftingInput.of(3, 3, fiveLeaves), "Five dried leaves produced an undocumented extract");
        assertNoCrafting(helper, CraftingInput.of(2, 2, List.of(new ItemStack(ModItems.FRESH_SALVIA_LEAF.get()),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)), "Fresh leaves bypassed drying");
        assertNoCrafting(helper, CraftingInput.of(2, 2, List.of(new ItemStack(ModItems.DRIED_SALVIA_LEAF.get()),
                new ItemStack(Items.DIRT), ItemStack.EMPTY, ItemStack.EMPTY)), "An unrelated ingredient was ignored during extract crafting");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void everyExtractLoadsExactlyOneEmptyBongInEitherCraftingGrid(GameTestHelper helper) {
        List<Item> extracts = extracts();
        List<Item> loadedBongs = loadedBongs();
        for (int strength = 0; strength < extracts.size(); strength++) {
            for (int gridSize : new int[]{2, 3}) {
                int slotsCount = gridSize * gridSize;
                // Include every position, both input orders, and separated slots with gaps.
                for (int bongSlot = 0; bongSlot < slotsCount; bongSlot++) {
                    for (int extractSlot = 0; extractSlot < slotsCount; extractSlot++) {
                        if (bongSlot == extractSlot) continue;
                        List<ItemStack> slots = emptySlots(slotsCount);
                        slots.set(bongSlot, new ItemStack(ModItems.BONG.get()));
                        slots.set(extractSlot, new ItemStack(extracts.get(strength), 3));
                        CraftingInput input = CraftingInput.of(gridSize, gridSize, slots);
                        CraftingRecipe recipe = crafting(helper, input, loadedBongs.get(strength));
                        helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),
                                "Loading returned a second empty Bong or extract remainder");
                        helper.assertTrue(slots.get(extractSlot).getCount() == 3 && slots.get(bongSlot).getCount() == 1,
                                "Loading preview changed its source stock");
                    }
                }
            }
            assertNoCrafting(helper, input2x2(ModItems.BONG.get(), extracts.get(strength), extracts.get(strength)),
                    "Two extract servings loaded into one bowl");
            assertNoCrafting(helper, input2x2(ModItems.BONG.get(), extracts.get(strength), extracts.get((strength + 1) % extracts.size())),
                    "Two different strengths loaded into one bowl");
            assertNoCrafting(helper, input2x2(ModItems.BONG.get(), ModItems.BONG.get(), extracts.get(strength)),
                    "One extract loaded two Bongs");
            assertNoCrafting(helper, input2x2(loadedBongs.get(strength), extracts.get(strength)),
                    "An already loaded Bong accepted another extract");
            assertNoCrafting(helper, input2x2(ModItems.BONG.get(), extracts.get(strength), Items.PAPER),
                    "Bong loading ignored an extra unrelated ingredient");
        }
        assertNoCrafting(helper, input2x2(ModItems.BONG.get()), "An empty Bong alone loaded itself");
        assertNoCrafting(helper, input2x2(ModItems.BONG.get(), ModItems.FRESH_SALVIA_LEAF.get()), "A fresh leaf loaded a Bong");
        assertNoCrafting(helper, input2x2(ModItems.BONG.get(), ModItems.DRIED_SALVIA_LEAF.get()), "A dried leaf skipped extract crafting");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void furnaceSmokerAndCampfireEachDryOneLeafWithTheirOwnTiming(GameTestHelper helper) {
        assertHeating(helper, RecipeType.SMELTING, 100);
        assertHeating(helper, RecipeType.SMOKING, 50);
        assertHeating(helper, RecipeType.CAMPFIRE_COOKING, 300);
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void looseExtractUseAndNewOrSavedCapsulesCannotConsumeSalvia(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            CapsuleBlendRecipe blendRecipe = new CapsuleBlendRecipe(CraftingBookCategory.MISC);
            List<Item> rejected = new ArrayList<>(extracts());
            rejected.addAll(loadedBongs());
            rejected.add(ModItems.FRESH_SALVIA_LEAF.get());
            rejected.add(ModItems.DRIED_SALVIA_LEAF.get());
            rejected.add(ModItems.BONG.get());
            for (Item item : extracts()) {
                for (InteractionHand hand : InteractionHand.values()) {
                    player.setItemInHand(hand, new ItemStack(item, 5));
                    var result = item.use(helper.getLevel(), player, hand);
                    helper.assertTrue(result.getResult() == InteractionResult.PASS, "Loose salvia extract reported consumption");
                    assertStack(helper, result.getObject(), item, 5, "Loose extract use changed its returned stack");
                    assertStack(helper, player.getItemInHand(hand), item, 5, "Loose extract use consumed source stock");
                }
            }
            for (Item item : rejected) {
                helper.assertTrue(CapsuleBlend.entryFromDoseItem(new ItemStack(item)).isEmpty(), "Salvia input was accepted by capsule blending");
                CraftingInput input = input2x2(ModItems.EMPTY_CAPSULE.get(), item);
                helper.assertFalse(blendRecipe.matches(input, helper.getLevel()), "A new capsule accepted a salvia item");
                helper.assertTrue(blendRecipe.assemble(input, helper.getLevel().registryAccess()).isEmpty(),
                        "A rejected salvia capsule still assembled a result");
            }
            helper.assertTrue(blendRecipe.matches(input2x2(ModItems.EMPTY_CAPSULE.get(), ModItems.LSD_BLOTTER_100UG.get()), helper.getLevel()),
                    "Removing salvia also disabled another substance's capsule recipe");
            ItemStack legacy = CapsuleBlend.createStack(List.of(new CapsuleBlend.Entry("salvia_units", 15.0D, ""),
                    new CapsuleBlend.Entry("profile_units", 5.0D, "salvia")));
            CompoundTag saved = (CompoundTag) legacy.save(helper.getLevel().registryAccess());
            ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), saved);
            helper.assertTrue(CapsuleBlend.read(restored).size() == 2, "The legacy capsule fixture lost its saved salvia entries");
            helper.assertTrue(CapsuleBlend.apply(restored, player) == 0, "A saved capsule bypassed the bong-only route");
            helper.assertTrue(SubstanceEngine.getExperience(player.getUUID()).isEmpty(), "An excluded salvia route started an experience");
            helper.assertFalse(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.SALVIA),
                    "An excluded salvia route unlocked the journal chapter");
        } finally {
            SubstanceEngine.clear(player.getUUID());
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void loadedBongsKeepAllFourDosesEmptyOnceAndUnlockFirstHitInEitherHandAndMode(GameTestHelper helper) {
        List<Item> loadedBongs = loadedBongs();
        double[] doses = {1.0D, 5.0D, 10.0D, 15.0D};
        for (GameType gameType : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            for (InteractionHand hand : InteractionHand.values()) {
                for (int strength = 0; strength < loadedBongs.size(); strength++) {
                    ServerPlayer player = player(helper);
                    try {
                        player.setGameMode(gameType);
                        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                        player.setItemInHand(otherHand, new ItemStack(Items.STICK, 3));
                        helper.assertFalse(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.SALVIA),
                                "A new player already had the Salvia chapter");
                        Item item = loadedBongs.get(strength);
                        ItemStack loaded = new ItemStack(item);
                        helper.assertTrue(loaded.getMaxStackSize() == 1, "Loaded Bongs can stack and risk losing vessels");
                        player.setItemInHand(hand, loaded);
                        var result = item.use(helper.getLevel(), player, hand);
                        helper.assertTrue(result.getResult().consumesAction(), "Loaded Bong use did not consume the action");
                        assertStack(helper, result.getObject(), ModItems.BONG.get(), 1, "Loaded use did not return the empty Bong");
                        assertStack(helper, player.getItemInHand(hand), ModItems.BONG.get(), 1, "Loaded use lost the glass or retained a loaded bowl");
                        assertStack(helper, player.getItemInHand(otherHand), Items.STICK, 3, "Bong use changed the other hand");
                        var experience = SubstanceEngine.getExperience(player.getUUID()).orElseThrow();
                        helper.assertTrue(experience.salviaState().active(), "Loaded Bong did not start salvia");
                        helper.assertTrue(Math.abs(experience.salviaState().targetDoseUnits() - doses[strength]) < 0.000001D,
                                "The Bong changed an existing salvia strength's calibrated effect dose");
                        helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.SALVIA),
                                "The first Bong hit did not unlock Cursed Findings");
                        helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.BONG.get()), "Bong consumption did not set the empty vessel's cooldown");
                        var emptyResult = ModItems.BONG.get().use(helper.getLevel(), player, hand);
                        helper.assertTrue(emptyResult.getResult() == InteractionResult.PASS, "An empty Bong produced another hit");
                        assertStack(helper, player.getItemInHand(hand), ModItems.BONG.get(), 1, "Empty use discarded the reusable Bong");
                        helper.assertTrue(Math.abs(experience.salviaState().targetDoseUnits() - doses[strength]) < 0.000001D,
                                "Empty use repeated the salvia dose");
                        helper.assertTrue(player.getInventory().countItem(ModItems.BONG.get()) == 1,
                                "Bong consumption duplicated an empty vessel in inventory");
                    } finally {
                        SubstanceEngine.clear(player.getUUID());
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void journalRecipeCardsPointToEveryRegisteredSalviaOutput(GameTestHelper helper) {
        CursedFindingEntry entry = CursedFindingsRegistry.byId(CursedFindingIds.SALVIA).orElseThrow();
        List<Item> expected = new ArrayList<>(extracts());
        expected.addAll(loadedBongs());
        expected.add(ModItems.BONG.get());
        expected.add(ModItems.DRIED_SALVIA_LEAF.get());
        for (Item item : expected) {
            long cards = entry.recipes().stream().filter(recipe -> recipe.output().stack().is(item)).count();
            helper.assertTrue(cards == 1, "Journal is missing or duplicates a Salvia recipe output: " + item);
        }
        helper.assertTrue(entry.recipes().stream().allMatch(recipe -> !recipe.output().stack().isEmpty()
                        && recipe.inputs().stream().allMatch(input -> !input.stack().isEmpty())),
                "A Salvia recipe card references an unregistered item");
        helper.succeed();
    }

    private static CraftingRecipe crafting(GameTestHelper helper, CraftingInput input, Item output) {
        var holder = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(holder.isPresent(), "No registered crafting recipe matches the Salvia inputs");
        CraftingRecipe recipe = holder.orElseThrow().value();
        assertStack(helper, recipe.assemble(input, helper.getLevel().registryAccess()), output, 1,
                "Registered crafting recipe returned the wrong Salvia product or quantity");
        return recipe;
    }

    private static void assertNoCrafting(GameTestHelper helper, CraftingInput input, String message) {
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(), message);
    }

    private static <T extends AbstractCookingRecipe> void assertHeating(GameTestHelper helper, RecipeType<T> type, int ticks) {
        ItemStack leaves = new ItemStack(ModItems.FRESH_SALVIA_LEAF.get(), 5);
        SingleRecipeInput input = new SingleRecipeInput(leaves);
        var holder = helper.getLevel().getRecipeManager().getRecipeFor(type, input, helper.getLevel());
        helper.assertTrue(holder.isPresent(), "Fresh Salvia Leaf has no registered heating recipe for " + type);
        T recipe = holder.orElseThrow().value();
        assertStack(helper, recipe.assemble(input, helper.getLevel().registryAccess()), ModItems.DRIED_SALVIA_LEAF.get(), 1,
                "Heating did not convert one fresh Salvia Leaf into one dried leaf");
        helper.assertTrue(recipe.getCookingTime() == ticks, "Salvia drying timing differs from the documented cooking route");
        assertStack(helper, leaves, ModItems.FRESH_SALVIA_LEAF.get(), 5, "Heating preview consumed its input");
    }

    private static CraftingInput input2x2(Item... items) {
        List<ItemStack> slots = emptySlots(4);
        for (int slot = 0; slot < items.length; slot++) slots.set(slot, new ItemStack(items[slot]));
        return CraftingInput.of(2, 2, slots);
    }

    private static List<ItemStack> emptySlots(int size) {
        List<ItemStack> slots = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) slots.add(ItemStack.EMPTY);
        return slots;
    }

    private static List<ItemStack> copySlots(List<ItemStack> slots) {
        return new ArrayList<>(slots.stream().map(ItemStack::copy).toList());
    }

    private static List<Item> extracts() {
        return List.of(ModItems.SALVIA_1X.get(), ModItems.SALVIA_10X.get(), ModItems.SALVIA_25X.get(), ModItems.SALVIA_50X.get());
    }

    private static List<Item> loadedBongs() {
        return List.of(ModItems.BONG_SALVIA_1X.get(), ModItems.BONG_SALVIA_10X.get(), ModItems.BONG_SALVIA_25X.get(), ModItems.BONG_SALVIA_50X.get());
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "salvia-test"));
        player.getInventory().clearContent();
        BlockPos position = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
        return player;
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count, String message) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, message);
    }
}
