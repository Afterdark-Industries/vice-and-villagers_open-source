package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.crafting.CapsuleBlendRecipe;
import com.trippycraft.guide.CursedFindingCategory;
import com.trippycraft.guide.CursedFindingIds;
import com.trippycraft.guide.CursedFindingsRegistry;
import com.trippycraft.guide.CursedRecipeCodex;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.item.MescalineDoseItem;
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
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MescalineCraftingGameTests {
    private MescalineCraftingGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void cactusCountSelectsExistingDoseInEveryInventoryArrangement(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(ModItems.MESCALINE_CACTUS.get()).getMaxStackSize() == 64,
                "Harvested cactus stock must stack like other raw crafting ingredients");
        // Each occupied slot supplies one cactus, even when that slot holds a larger stack.
        for (int mask = 1; mask < 16; mask++) {
            int cactusCount = Integer.bitCount(mask);
            CraftingInput input = cactusInput(2, mask, 7);
            CraftingRecipe recipe = crafting(helper, input, doses().get(cactusCount - 1).item());
            helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),
                    "Mescaline crafting returned an unintended cactus or container remainder");
            assertStock(helper, input, 7);
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void cactusDosageRecipesAreShapelessInEveryTableArrangement(GameTestHelper helper) {
        // Include separated slots, edge positions, and every arrangement of all four quantities.
        for (int mask = 1; mask < 512; mask++) {
            int cactusCount = Integer.bitCount(mask);
            if (cactusCount > 4) continue;
            CraftingInput input = cactusInput(3, mask, 2);
            crafting(helper, input, doses().get(cactusCount - 1).item());
            assertStock(helper, input, 2);
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void extraCactiAndUnrelatedIngredientsCannotCraftMescaline(GameTestHelper helper) {
        for (int mask = 1; mask < 512; mask++) {
            if (Integer.bitCount(mask) > 4) {
                assertNoCrafting(helper, cactusInput(3, mask, 1),
                        "Five or more cactus ingredients produced an undocumented Mescaline dose");
            }
        }
        assertNoCrafting(helper, CraftingInput.of(2, 2, emptySlots(4)), "An empty grid produced Mescaline");
        assertNoCrafting(helper, input2x2(ModItems.MESCALINE_CACTUS.get(), Items.DIRT),
                "Mescaline crafting ignored an unrelated ingredient");
        assertNoCrafting(helper, input2x2(Items.CACTUS), "Vanilla cactus substituted for Mescaline Cactus");
        assertNoCrafting(helper, input2x2(ModItems.MESCALINE_CACTUS.get(), Items.CACTUS),
                "Vanilla cactus substituted for another Mescaline Cactus ingredient");
        for (Dose dose : doses()) {
            assertNoCrafting(helper, input2x2(ModItems.MESCALINE_CACTUS.get(), dose.item()),
                    "An existing Mescaline dose substituted for a cactus ingredient");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void harvestedCactusDoesNotDirectlyDoseOrEnterCapsuleBlends(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            Item cactus = ModItems.MESCALINE_CACTUS.get();
            for (InteractionHand hand : InteractionHand.values()) {
                player.setItemInHand(hand, new ItemStack(cactus, 5));
                var result = cactus.use(helper.getLevel(), player, hand);
                helper.assertTrue(result.getResult() == InteractionResult.PASS,
                        "Raw Mescaline Cactus reported direct dose consumption");
                assertStack(helper, result.getObject(), cactus, 5, "Raw cactus use changed the returned stock");
                assertStack(helper, player.getItemInHand(hand), cactus, 5, "Raw cactus use consumed source stock");
            }
            helper.assertTrue(SubstanceEngine.getExperience(player.getUUID()).isEmpty(),
                    "Using the raw cactus started a Mescaline experience");
            helper.assertFalse(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.MESCALINE),
                    "Using the raw cactus unlocked Mescaline before a prepared dose was consumed");
            helper.assertTrue(CapsuleBlend.entryFromDoseItem(new ItemStack(cactus)).isEmpty(),
                    "Raw cactus bypassed its dosage crafting step through capsule blending");
            CapsuleBlendRecipe blend = new CapsuleBlendRecipe(CraftingBookCategory.MISC);
            CraftingInput input = input2x2(ModItems.EMPTY_CAPSULE.get(), cactus);
            helper.assertFalse(blend.matches(input, helper.getLevel()), "An empty capsule accepted raw cactus");
            helper.assertTrue(blend.assemble(input, helper.getLevel().registryAccess()).isEmpty(),
                    "A rejected raw-cactus capsule still assembled a product");
        } finally {
            SubstanceEngine.clear(player.getUUID());
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void existingFormatsKeepDirectUseDosesInBothHandsAndGameModes(GameTestHelper helper) {
        List<Dose> formats = new ArrayList<>(doses());
        formats.add(new Dose(ModItems.MESCALINE_BUTTON_LEGACY.get(), 100.0D));
        for (Dose dose : formats) {
            helper.assertTrue(dose.item() instanceof MescalineDoseItem,
                    "An existing Mescaline format lost its established item behavior");
            helper.assertTrue(((MescalineDoseItem) dose.item()).doseMilligrams() == dose.milligrams(),
                    "An existing Mescaline format changed its calibrated dose");
            for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
                for (InteractionHand hand : InteractionHand.values()) {
                    ServerPlayer player = player(helper);
                    try {
                        player.setGameMode(mode);
                        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND
                                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                        player.setItemInHand(otherHand, new ItemStack(Items.STICK, 3));
                        player.setItemInHand(hand, new ItemStack(dose.item(), 5));
                        helper.assertFalse(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.MESCALINE),
                                "A new player's Mescaline finding was already open");
                        var result = dose.item().use(helper.getLevel(), player, hand);
                        helper.assertTrue(result.getResult().consumesAction(), "A prepared Mescaline dose could not be consumed");
                        int remaining = mode == GameType.CREATIVE ? 5 : 4;
                        assertStack(helper, result.getObject(), dose.item(), remaining,
                                "Mescaline use changed its established serving consumption");
                        assertStack(helper, player.getItemInHand(hand), dose.item(), remaining,
                                "Mescaline use consumed the wrong hand or quantity");
                        assertStack(helper, player.getItemInHand(otherHand), Items.STICK, 3,
                                "Mescaline use changed the other hand's stock");
                        helper.assertTrue(player.getCooldowns().isOnCooldown(dose.item()),
                                "Mescaline use lost its established cooldown");
                        var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().mescalineState();
                        helper.assertTrue(state.active() && state.targetDoseMilligrams() == dose.milligrams(),
                                "A prepared Mescaline format changed the experience dose");
                        helper.assertTrue(state.repeatedDoses() == 0, "A first serving was counted as a repeated dose");
                        helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.MESCALINE),
                                "Consuming an existing Mescaline dose failed to unlock Cursed Findings");
                    } finally {
                        SubstanceEngine.clear(player.getUUID());
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void existingMescalineCapsulesKeepTheirDoseAndSavedContent(GameTestHelper helper) {
        CapsuleBlendRecipe blend = new CapsuleBlendRecipe(CraftingBookCategory.MISC);
        for (Dose dose : doses()) {
            CapsuleBlend.Entry ingredient = CapsuleBlend.entryFromDoseItem(new ItemStack(dose.item(), 6)).orElseThrow();
            helper.assertTrue(ingredient.kind().equals("mescaline_milligrams")
                            && ingredient.amount() == dose.milligrams() && ingredient.effectAmount() == dose.milligrams(),
                    "A Mescaline dose changed its existing capsule contribution");
            CraftingInput input = CraftingInput.of(2, 2, List.of(new ItemStack(ModItems.EMPTY_CAPSULE.get()),
                    ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(dose.item(), 6)));
            helper.assertTrue(blend.matches(input, helper.getLevel()), "A prepared Mescaline dose no longer entered capsule blends");
            ItemStack capsule = blend.assemble(input, helper.getLevel().registryAccess());
            ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(),
                    (CompoundTag) capsule.save(helper.getLevel().registryAccess()));
            List<CapsuleBlend.Entry> entries = CapsuleBlend.read(restored);
            helper.assertTrue(entries.size() == 1 && entries.getFirst().kind().equals("mescaline_milligrams")
                            && entries.getFirst().amount() == dose.milligrams(),
                    "A saved Mescaline capsule lost or multiplied its one-serving dose");
            helper.assertTrue(input.getItem(3).getCount() == 6,
                    "Capsule crafting preview consumed stock or counted the entire Mescaline stack");
            helper.assertTrue(blend.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),
                    "Capsule blending returned an unintended Mescaline container");
            ServerPlayer player = player(helper);
            try {
                helper.assertTrue(CapsuleBlend.apply(restored, player) == 1, "A saved Mescaline capsule did not apply its serving");
                var state = SubstanceEngine.getExperience(player.getUUID()).orElseThrow().mescalineState();
                helper.assertTrue(state.active() && state.targetDoseMilligrams() == dose.milligrams(),
                        "A saved capsule changed the existing Mescaline effect dose");
                helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.MESCALINE),
                        "A Mescaline Blend Capsule did not unlock the finding");
            } finally {
                SubstanceEngine.clear(player.getUUID());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void firstPreparedDoseDiscoveryPersistsAfterTripAndPlayerDataRoundTrip(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ServerPlayer restored = player(helper);
        try {
            helper.assertFalse(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.MESCALINE),
                    "The Mescaline finding was unlocked before discovery");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.MESCALINE_25MG.get()));
            ModItems.MESCALINE_25MG.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.MESCALINE),
                    "The first Mescaline serving did not open its finding");
            SubstanceEngine.clear(player.getUUID());
            helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.MESCALINE),
                    "Ending a Mescaline experience erased journal discovery");
            CompoundTag savedData = player.getPersistentData().copy();
            restored.getPersistentData().merge(savedData);
            helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(restored, CursedFindingIds.MESCALINE),
                    "Restoring persistent player data lost the Mescaline finding");
            helper.assertFalse(CursedRecipeCodex.unlockFinding(restored, CursedFindingIds.MESCALINE),
                    "A previously discovered Mescaline chapter was unlocked twice");
        } finally {
            SubstanceEngine.clear(player.getUUID());
            SubstanceEngine.clear(restored.getUUID());
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void journalHasAllFiveTabsAndRecipeCardsForEveryMescalineDose(GameTestHelper helper) {
        var finding = CursedFindingsRegistry.byId(CursedFindingIds.MESCALINE).orElseThrow();
        helper.assertTrue(finding.category() == CursedFindingCategory.PSYCHEDELICS,
                "Mescaline is missing from the journal's psychedelic category");
        helper.assertTrue(!finding.overview().isEmpty() && !finding.effects().isEmpty()
                        && !finding.productionChain().isEmpty() && !finding.recipes().isEmpty() && !finding.notes().isEmpty(),
                "A Mescaline journal tab was left empty");
        for (int index = 0; index < doses().size(); index++) {
            Item output = doses().get(index).item();
            var cards = finding.recipes().stream().filter(recipe -> recipe.output().stack().is(output)).toList();
            helper.assertTrue(cards.size() == 1, "Mescaline journal omitted or duplicated a dosage recipe");
            var card = cards.getFirst();
            helper.assertTrue(card.inputs().size() == 1 && card.inputs().getFirst().stack().is(ModItems.MESCALINE_CACTUS.get())
                            && card.inputs().getFirst().label().equals((index + 1) + "x Mescaline Cactus"),
                    "Mescaline journal dosage card lists the wrong cactus ingredient quantity");
        }
        helper.assertTrue(finding.recipes().stream().allMatch(recipe -> !recipe.output().stack().isEmpty()
                        && recipe.inputs().stream().allMatch(input -> !input.stack().isEmpty())),
                "A Mescaline recipe card references an unregistered item");
        helper.assertTrue(finding.productionChain().stream().allMatch(step -> !step.input().stack().isEmpty()
                        && !step.station().stack().isEmpty() && !step.output().stack().isEmpty()),
                "A Mescaline production step references an unregistered item");
        helper.succeed();
    }

    private static CraftingRecipe crafting(GameTestHelper helper, CraftingInput input, Item output) {
        var holder = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(holder.isPresent(), "No registered recipe matches the Mescaline cactus quantity and arrangement");
        CraftingRecipe recipe = holder.orElseThrow().value();
        assertStack(helper, recipe.assemble(input, helper.getLevel().registryAccess()), output, 1,
                "Registered Mescaline recipe produced the wrong dose or quantity");
        return recipe;
    }

    private static void assertNoCrafting(GameTestHelper helper, CraftingInput input, String message) {
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(), message);
    }

    private static CraftingInput cactusInput(int size, int mask, int count) {
        List<ItemStack> slots = emptySlots(size * size);
        for (int slot = 0; slot < slots.size(); slot++) {
            if ((mask & (1 << slot)) != 0) slots.set(slot, new ItemStack(ModItems.MESCALINE_CACTUS.get(), count));
        }
        return CraftingInput.of(size, size, slots);
    }

    private static void assertStock(GameTestHelper helper, CraftingInput input, int count) {
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty()) assertStack(helper, stack, ModItems.MESCALINE_CACTUS.get(), count,
                    "Mescaline crafting preview consumed stock or counted stack size instead of ingredient slots");
        }
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

    private static List<Dose> doses() {
        return List.of(new Dose(ModItems.MESCALINE_25MG.get(), 25.0D),
                new Dose(ModItems.MESCALINE_100MG.get(), 100.0D),
                new Dose(ModItems.MESCALINE_250MG.get(), 250.0D),
                new Dose(ModItems.MESCALINE_500MG.get(), 500.0D));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "mescaline-test"));
        player.getInventory().clearContent();
        BlockPos position = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
        return player;
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count, String message) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, message);
    }

    private record Dose(Item item, double milligrams) {
    }
}
