package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.guide.CursedFindingEntry;
import com.trippycraft.guide.CursedFindingIds;
import com.trippycraft.guide.CursedFindingsRegistry;
import com.trippycraft.guide.CursedRecipeCodex;
import com.trippycraft.item.BongItem;
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
import net.minecraft.world.item.crafting.CraftingInput;
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
public final class CannabisBongGameTests {
    private CannabisBongGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void oneDriedBudLoadsOneBongAnywhereInEitherCraftingGrid(GameTestHelper helper) {
        for (int width : new int[]{2, 3}) {
            int size = width * width;
            for (int bongSlot = 0; bongSlot < size; bongSlot++) {
                for (int budSlot = 0; budSlot < size; budSlot++) {
                    if (bongSlot == budSlot) continue;
                    List<ItemStack> slots = emptySlots(size);
                    slots.set(bongSlot, new ItemStack(ModItems.BONG.get()));
                    slots.set(budSlot, new ItemStack(ModItems.DRIED_CANNABIS_BUD.get(), 5));
                    CraftingInput input = CraftingInput.of(width, width, slots);
                    var holder = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
                    helper.assertTrue(holder.isPresent(), "The Bong failed to load a dried bud in a valid crafting position");
                    var recipe = holder.orElseThrow().value();
                    assertStack(helper, recipe.assemble(input, helper.getLevel().registryAccess()), ModItems.BONG_CANNABIS.get(), 1,
                            "One dried bud did not yield exactly one loaded Cannabis Bong");
                    helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),
                            "Loading returned another Bong and duplicated the vessel");
                    assertStack(helper, slots.get(bongSlot), ModItems.BONG.get(), 1, "Loading preview consumed the source Bong");
                    assertStack(helper, slots.get(budSlot), ModItems.DRIED_CANNABIS_BUD.get(), 5, "Loading preview consumed the bud stock");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void cannabisBongLoadingRejectsFreshExtraAndAlreadyLoadedIngredients(GameTestHelper helper) {
        Item empty = ModItems.BONG.get();
        Item bud = ModItems.DRIED_CANNABIS_BUD.get();
        assertNoCrafting(helper, input(empty), "An empty Bong loaded itself");
        assertNoCrafting(helper, input(empty, ModItems.CANNABIS_BUD.get()), "A fresh bud bypassed drying");
        assertNoCrafting(helper, input(empty, bud, bud), "Two buds fitted into one bowl");
        assertNoCrafting(helper, input(empty, empty, bud), "One bud loaded two vessels");
        assertNoCrafting(helper, input(empty, bud, Items.PAPER), "Loading ignored an unrelated ingredient");
        for (Item loaded : List.of(ModItems.BONG_CANNABIS.get(), ModItems.BONG_SALVIA_1X.get(),
                ModItems.BONG_SALVIA_10X.get(), ModItems.BONG_SALVIA_25X.get(), ModItems.BONG_SALVIA_50X.get())) {
            assertNoCrafting(helper, input(loaded, bud), "An already loaded Bong accepted another bud");
            assertNoCrafting(helper, input(loaded, ModItems.SALVIA_1X.get()), "An already loaded Bong accepted a salvia serving");
        }
        for (Item extract : List.of(ModItems.SALVIA_1X.get(), ModItems.SALVIA_10X.get(),
                ModItems.SALVIA_25X.get(), ModItems.SALVIA_50X.get())) {
            assertNoCrafting(helper, input(empty, bud, extract), "One bowl combined cannabis and salvia");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void cannabisHitDelivers200mgAndKeepsOneEmptyVesselInEitherHandAndMode(GameTestHelper helper) {
        Item loadedItem = ModItems.BONG_CANNABIS.get();
        helper.assertTrue(loadedItem instanceof BongItem bong && bong.contents() == BongItem.Contents.CANNABIS
                        && bong.doseUnits() == 200.0D,
                "The registered Cannabis Bong does not carry its complete 200mg bud dose");
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            for (InteractionHand hand : InteractionHand.values()) {
                ServerPlayer player = player(helper);
                try {
                    player.setGameMode(mode);
                    InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    player.setItemInHand(other, new ItemStack(Items.STICK, 3));
                    player.setItemInHand(hand, new ItemStack(loadedItem));
                    helper.assertTrue(player.getItemInHand(hand).getMaxStackSize() == 1, "Loaded Cannabis Bongs can stack");
                    var result = loadedItem.use(helper.getLevel(), player, hand);
                    helper.assertTrue(result.getResult().consumesAction(), "Smoking did not consume the action");
                    assertStack(helper, result.getObject(), ModItems.BONG.get(), 1, "Smoking did not return the empty Bong");
                    assertStack(helper, player.getItemInHand(hand), ModItems.BONG.get(), 1, "Smoking lost the vessel or retained its hit");
                    assertStack(helper, player.getItemInHand(other), Items.STICK, 3, "Smoking changed the other hand");
                    var experience = SubstanceEngine.getExperience(player.getUUID()).orElseThrow();
                    helper.assertTrue(experience.cannabisState().active()
                                    && Math.abs(experience.cannabisState().targetThcMilligrams() - 200.0D) < 0.000001D,
                            "The Bong did not add exactly 200mg THC");
                    helper.assertFalse(experience.salviaState().active(), "Smoking a cannabis bud accidentally started salvia");
                    helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.CANNABIS),
                            "The first cannabis Bong hit did not unlock its Cursed Findings chapter");
                    helper.assertFalse(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.SALVIA),
                            "A cannabis Bong unlocked the Salvia chapter");
                    helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.BONG.get())
                                    && player.getCooldowns().isOnCooldown(loadedItem),
                            "Smoking did not apply the reusable vessel's short cooldown");
                    var emptyResult = ModItems.BONG.get().use(helper.getLevel(), player, hand);
                    helper.assertTrue(emptyResult.getResult() == InteractionResult.PASS, "The empty bowl produced another hit");
                    helper.assertTrue(experience.cannabisState().targetThcMilligrams() == 200.0D, "Empty use repeated the cannabis dose");
                    helper.assertTrue(player.getInventory().countItem(ModItems.BONG.get()) == 1, "Smoking duplicated the vessel");
                } finally {
                    SubstanceEngine.clear(player.getUUID());
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void savedCannabisBongStacksTwoHitsIntoOne400mgSession(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            ItemStack loaded = new ItemStack(ModItems.BONG_CANNABIS.get());
            CompoundTag saved = (CompoundTag) loaded.save(helper.getLevel().registryAccess());
            ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), saved);
            assertStack(helper, restored, ModItems.BONG_CANNABIS.get(), 1, "Saving lost the Cannabis Bong's loaded contents");
            player.setItemInHand(InteractionHand.MAIN_HAND, restored);
            restored.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            var experience = SubstanceEngine.getExperience(player.getUUID()).orElseThrow();
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BONG_CANNABIS.get()));
            ModItems.BONG_CANNABIS.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(SubstanceEngine.getExperience(player.getUUID()).orElseThrow() == experience,
                    "A second cannabis Bong hit restarted the active experience");
            helper.assertTrue(experience.cannabisState().targetThcMilligrams() == 400.0D,
                    "Two one-bud Bong hits did not stack into 400mg THC");
            assertStack(helper, player.getItemInHand(InteractionHand.MAIN_HAND), ModItems.BONG.get(), 1,
                    "The second hit did not return the shared empty vessel");
        } finally {
            SubstanceEngine.clear(player.getUUID());
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void cannabisJournalContainsBongCraftingAndLoadingAlongsideExistingProducts(GameTestHelper helper) {
        CursedFindingEntry entry = CursedFindingsRegistry.byId(CursedFindingIds.CANNABIS).orElseThrow();
        for (Item item : List.of(ModItems.BONG.get(), ModItems.BONG_CANNABIS.get(), ModItems.DRIED_CANNABIS_BUD.get(),
                ModItems.CANNABIS_JOINT.get(), ModItems.CANNABIS_DAB_PEN.get(), ModItems.CANNABIS_GUMMIES.get(),
                ModItems.CANNABIS_CHOCOLATE_BAR.get())) {
            long cards = entry.recipes().stream().filter(recipe -> recipe.output().stack().is(item)).count();
            helper.assertTrue(cards == 1, "The Cannabis chapter is missing or duplicates a product recipe: " + item);
        }
        var loadCard = entry.recipes().stream().filter(recipe -> recipe.output().stack().is(ModItems.BONG_CANNABIS.get())).findFirst().orElseThrow();
        helper.assertTrue(loadCard.inputs().size() == 2
                        && loadCard.inputs().stream().anyMatch(ref -> ref.stack().is(ModItems.BONG.get()))
                        && loadCard.inputs().stream().anyMatch(ref -> ref.stack().is(ModItems.DRIED_CANNABIS_BUD.get())),
                "The Cannabis Bong recipe card shows the wrong ingredients");
        helper.assertTrue(ModItems.creativeItems().stream().anyMatch(item -> item.get() == ModItems.BONG_CANNABIS.get()),
                "The loaded Cannabis Bong is missing from the creative items");
        helper.succeed();
    }

    private static void assertNoCrafting(GameTestHelper helper, CraftingInput input, String message) {
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(), message);
    }

    private static CraftingInput input(Item... items) {
        List<ItemStack> slots = emptySlots(4);
        for (int slot = 0; slot < items.length; slot++) slots.set(slot, new ItemStack(items[slot]));
        return CraftingInput.of(2, 2, slots);
    }

    private static List<ItemStack> emptySlots(int size) {
        List<ItemStack> slots = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) slots.add(ItemStack.EMPTY);
        return slots;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "cannabis-bong-test"));
        player.getInventory().clearContent();
        BlockPos position = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
        return player;
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count, String message) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, message);
    }
}
