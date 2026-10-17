package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.block.entity.MortarAndPestleBlockEntity;
import com.trippycraft.guide.CursedFindingIds;
import com.trippycraft.guide.CursedFindingsRegistry;
import com.trippycraft.guide.CursedRecipeCodex;
import com.trippycraft.item.BongItem;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.item.DmtItem;
import com.trippycraft.item.DmtPowderItem;
import com.trippycraft.menu.MortarAndPestleMenu;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.SubstanceEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A separate template namespace makes the new item contracts runnable without the full world suite. */
@GameTestHolder("trippycraft_smoking")
@PrefixGameTestTemplate(false)
public final class SmokingToolsGameTests {
    private SmokingToolsGameTests() { }

    @GameTest(template = "empty")
    public static void pipeCraftUsesAnyPlanksAtEveryValidOffsetAndMirrorWithoutRemainders(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        for (var plank : BuiltInRegistries.ITEM.getTag(ItemTags.PLANKS).orElseThrow()) {
            for (int top = 0; top <= 1; top++) for (boolean mirrored : new boolean[]{false, true}) {
                List<ItemStack> slots = blanks(9);
                slots.set(top * 3 + (mirrored ? 2 : 0), new ItemStack(Items.IRON_INGOT));
                slots.set((top + 1) * 3, new ItemStack(plank.value()));
                slots.set((top + 1) * 3 + 1, new ItemStack(ModItems.ACACIA_BARK.get()));
                slots.set((top + 1) * 3 + 2, new ItemStack(plank.value()));
                CraftingInput input = CraftingInput.of(3, 3, slots);
                CraftingRecipe recipe = recipe(helper, input, ModItems.ANCIENT_PIPE.get());
                helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),
                        "Pipe crafting returned an unexpected ingredient remainder");
                takeCraft(helper, player, 3, slots, ModItems.ANCIENT_PIPE.get());
                List<ItemStack> extra = slots.stream().map(ItemStack::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
                extra.set(top * 3 + 1, new ItemStack(Items.STICK));
                noRecipe(helper, CraftingInput.of(3, 3, extra), "Pipe shape ignored an extra ingredient");
            }
        }
        noRecipe(helper, input(2, Items.IRON_INGOT, Items.OAK_PLANKS, ModItems.ACACIA_BARK.get(), Items.OAK_PLANKS),
                "Three-wide pipe shape fitted into a 2x2 grid");
        noRecipe(helper, input(3, Items.IRON_INGOT, Items.AIR, Items.AIR, Items.OAK_PLANKS, Items.STICK, Items.OAK_PLANKS),
                "A stick replaced the required acacia bark");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allTwelveToolLoadsCraftInEveryGridPositionConsumeOneAndNeverReturnAnEmptyDuplicate(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        List<Item> ingredients = ingredients();
        for (Item empty : emptyTools()) {
            List<Item> loaded = loadedTools(empty);
            for (int kind = 0; kind < ingredients.size(); kind++) for (int width : new int[]{2, 3}) {
                for (int vessel = 0; vessel < width * width; vessel++) for (int dose = 0; dose < width * width; dose++) {
                    if (vessel == dose) continue;
                    List<ItemStack> slots = blanks(width * width);
                    slots.set(vessel, new ItemStack(empty));
                    slots.set(dose, new ItemStack(ingredients.get(kind), 3));
                    var recipe = recipe(helper, CraftingInput.of(width, width, slots), loaded.get(kind));
                    helper.assertTrue(recipe.getRemainingItems(CraftingInput.of(width, width, slots)).stream().allMatch(ItemStack::isEmpty),
                            "Loading returned an extra empty vessel");
                    takeCraft(helper, player, width, slots, loaded.get(kind));
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void loadingRejectsCrystalsFreshPlantsMixedDosesExtraVesselsAndAlreadyLoadedTools(GameTestHelper helper) {
        for (Item empty : emptyTools()) {
            noRecipe(helper, input(2, empty), "An empty device loaded itself");
            for (Item invalid : List.of(ModItems.DMT.get(), ModItems.DMT_5X.get(), ModItems.CANNABIS_BUD.get(),
                    ModItems.FRESH_SALVIA_LEAF.get(), ModItems.DRIED_SALVIA_LEAF.get(), Items.DIRT)) {
                noRecipe(helper, input(2, empty, invalid), "A non-serving ingredient loaded a device: " + invalid);
            }
            for (Item ingredient : ingredients()) {
                noRecipe(helper, input(2, empty, ingredient, ingredient), "Two servings loaded one bowl");
                noRecipe(helper, input(2, empty, ingredient, Items.PAPER), "Loading ignored an unrelated ingredient");
                for (Item second : emptyTools()) noRecipe(helper, input(2, empty, second, ingredient), "One serving loaded two devices");
                for (Item other : ingredients()) {
                    if (other != ingredient) noRecipe(helper, input(2, empty, ingredient, other), "One bowl combined different substances/strengths");
                }
            }
            for (Item loaded : loadedTools(empty)) for (Item ingredient : ingredients()) {
                noRecipe(helper, input(2, loaded, ingredient), "An already loaded tool accepted another serving");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mortarGrindsExactOneAndFiveUnitYieldsAndBlocksUntilTheEntireOutputFits(GameTestHelper helper) {
        for (Item crystal : List.of(ModItems.DMT.get(), ModItems.DMT_5X.get())) {
            int yield = crystal == ModItems.DMT.get() ? 1 : 5;
            MortarAndPestleBlockEntity mortar = mortar(helper);
            helper.assertTrue(mortar.isValidInput(new ItemStack(crystal)) && mortar.canPlaceItem(0, new ItemStack(crystal)),
                    "DMT crystal is not accepted by the mortar/hopper input");
            mortar.setItem(0, new ItemStack(crystal, 2));
            tick(helper, mortar, 39);
            helper.assertTrue(mortar.getItem(1).isEmpty(), "Grinding completed before the existing forty-tick cycle");
            tick(helper, mortar, 1);
            stack(helper, mortar.getItem(0), crystal, 1, "Grinding consumed more than one crystal");
            stack(helper, mortar.getItem(1), ModItems.DMT_POWDER.get(), yield, "Crystal strength changed powder yield");
            tick(helper, mortar, 40);
            helper.assertTrue(mortar.getItem(0).isEmpty(), "Second ground crystal remained in input");
            stack(helper, mortar.getItem(1), ModItems.DMT_POWDER.get(), yield * 2, "Two crystals lost powder");

            mortar.clearContent();
            mortar.setItem(0, new ItemStack(crystal));
            mortar.setItem(1, new ItemStack(ModItems.DMT_POWDER.get(), 65 - yield));
            tick(helper, mortar, 80);
            stack(helper, mortar.getItem(0), crystal, 1, "Capacity-blocked grinding destroyed a crystal");
            stack(helper, mortar.getItem(1), ModItems.DMT_POWDER.get(), 65 - yield, "Capacity-blocked grinding changed output");
            mortar.setItem(1, new ItemStack(Items.DIRT));
            tick(helper, mortar, 80);
            stack(helper, mortar.getItem(0), crystal, 1, "Incompatible output destroyed a crystal");
            mortar.setItem(1, new ItemStack(ModItems.DMT_POWDER.get(), 64 - yield));
            tick(helper, mortar, 20);
            CompoundTag saved = mortar.saveWithoutMetadata(helper.getLevel().registryAccess());
            MortarAndPestleBlockEntity restored = new MortarAndPestleBlockEntity(mortar.getBlockPos(), mortar.getBlockState());
            restored.loadWithComponents(saved, helper.getLevel().registryAccess());
            tick(helper, restored, 20);
            stack(helper, restored.getItem(1), ModItems.DMT_POWDER.get(), 64, "Exact-capacity/saved grinding lost or duplicated powder");
            helper.assertTrue(restored.getItem(0).isEmpty(), "Saved grinding retained its consumed crystal");

            mortar.clearContent();
            ServerPlayer player = player(helper);
            MortarAndPestleMenu menu = new MortarAndPestleMenu(1, player.getInventory(), mortar, new SimpleContainerData(2));
            player.getInventory().setItem(9, new ItemStack(crystal));
            helper.assertTrue(menu.getSlot(0).mayPlace(new ItemStack(crystal)) && !menu.quickMoveStack(player, 2).isEmpty(),
                    "Shift-click routing did not put a DMT crystal into the mortar");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "Shift-click left a duplicate crystal");
            tick(helper, mortar, 40);
            helper.assertTrue(!menu.quickMoveStack(player, 1).isEmpty(), "Ground powder could not be shift-clicked out");
            helper.assertTrue(player.getInventory().countItem(ModItems.DMT_POWDER.get()) == yield && mortar.getItem(1).isEmpty(),
                    "Retrieving ground powder changed its quantity");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void crystalsAreProcessingOnlyAndTheirOldHeatingStrengthsRemainAvailable(GameTestHelper helper) {
        List<Item> crystals = List.of(ModItems.DMT.get(), ModItems.DMT_5X.get());
        for (int strength = 0; strength < crystals.size(); strength++) {
            Item crystal = crystals.get(strength);
            helper.assertTrue(crystal instanceof DmtItem dmt && dmt.doseUnits() == (strength == 0 ? 1 : 5), "Legacy crystal strength changed");
            for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) for (InteractionHand hand : InteractionHand.values()) {
                ServerPlayer player = player(helper);
                player.setGameMode(mode);
                player.setItemInHand(hand, new ItemStack(crystal, 3));
                helper.assertTrue(player.gameMode.useItem(player, helper.getLevel(), player.getItemInHand(hand), hand) == InteractionResult.PASS,
                        "A raw crystal can still be consumed");
                stack(helper, player.getItemInHand(hand), crystal, 3, "Raw crystal use consumed crystal stock");
                helper.assertTrue(SubstanceEngine.getExperience(player.getUUID()).isEmpty() && !player.getCooldowns().isOnCooldown(crystal),
                        "Raw crystal use dosed or set a cooldown");
                helper.assertTrue(CapsuleBlend.entryFromDoseItem(new ItemStack(crystal)).isEmpty(), "A raw crystal bypassed grinding via capsules");
            }
            noRecipe(helper, input(2, ModItems.EMPTY_CAPSULE.get(), crystal), "Raw crystal was accepted by the capsule crafting recipe");
            Item extract = strength == 0 ? ModItems.PLANT_EXTRACT.get() : ModItems.BUNDLED_PLANT_EXTRACT.get();
            SingleRecipeInput input = new SingleRecipeInput(new ItemStack(extract));
            for (var type : List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING)) {
                var holder = helper.getLevel().getRecipeManager().getRecipeFor(type, input, helper.getLevel());
                helper.assertTrue(holder.isPresent(), "Legacy DMT heating acquisition disappeared: " + type);
                stack(helper, holder.orElseThrow().value().assemble(input, helper.getLevel().registryAccess()), crystal, 1, "Legacy heating output changed");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void powderDirectUseIsOneUnitInEitherHandAndModeWithRealCooldownAndCapsuleContent(GameTestHelper helper) {
        Item powder = ModItems.DMT_POWDER.get();
        helper.assertTrue(powder instanceof DmtPowderItem item && item.doseUnits() == 1, "Powder does not carry exactly one DMT unit");
        var entry = CapsuleBlend.entryFromDoseItem(new ItemStack(powder)).orElseThrow();
        helper.assertTrue(entry.amount() == 1 && entry.kind().equals("dmt_units"), "Powder capsule entry carries the wrong substance/amount");
        CraftingInput capsuleInput = input(2, ModItems.EMPTY_CAPSULE.get(), powder);
        var capsuleRecipe = recipe(helper, capsuleInput, ModItems.BLEND_CAPSULE.get());
        ItemStack capsule = capsuleRecipe.assemble(capsuleInput, helper.getLevel().registryAccess());
        helper.assertTrue(CapsuleBlend.read(capsule).size() == 1 && CapsuleBlend.read(capsule).getFirst().amount() == 1,
                "Actual capsule crafting changed the prepared powder dose");
        ServerPlayer legacyPlayer = player(helper);
        try {
            ItemStack oldCapsule = CapsuleBlend.createStack(List.of(new CapsuleBlend.Entry("dmt_units", 5, "")));
            oldCapsule = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) oldCapsule.save(helper.getLevel().registryAccess()));
            helper.assertTrue(CapsuleBlend.apply(oldCapsule, legacyPlayer) == 1
                    && SubstanceEngine.getExperience(legacyPlayer.getUUID()).orElseThrow().dmtState().targetDoseUnits() == 5,
                    "Saved filled DMT capsules lost their existing content");
        } finally { SubstanceEngine.clear(legacyPlayer.getUUID()); }
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) for (InteractionHand hand : InteractionHand.values()) {
            ServerPlayer player = player(helper);
            try {
                player.setGameMode(mode);
                player.setItemInHand(hand, new ItemStack(powder, 3));
                helper.assertTrue(player.gameMode.useItem(player, helper.getLevel(), player.getItemInHand(hand), hand).consumesAction(), "Powder use failed");
                stack(helper, player.getItemInHand(hand), powder, mode == GameType.CREATIVE ? 3 : 2, "Powder consumption changed creative/survival stock incorrectly");
                helper.assertTrue(SubstanceEngine.getExperience(player.getUUID()).orElseThrow().dmtState().targetDoseUnits() == 1,
                        "Direct powder did not dose exactly one unit");
                helper.assertTrue(player.getCooldowns().isOnCooldown(powder), "Powder did not apply its short cooldown");
                player.gameMode.useItem(player, helper.getLevel(), player.getItemInHand(hand), hand);
                helper.assertTrue(SubstanceEngine.getExperience(player.getUUID()).orElseThrow().dmtState().targetDoseUnits() == 1,
                        "Powder cooldown allowed an immediate repeated dose");
                helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(player, CursedFindingIds.DMT), "Powder use did not unlock DMT findings");
            } finally { SubstanceEngine.clear(player.getUUID()); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyLoadedToolDeliversItsOneServingReturnsItsOwnVesselAndHonorsFamilyCooldown(GameTestHelper helper) {
        for (Item empty : emptyTools()) for (Item loaded : loadedTools(empty)) {
            BongItem tool = (BongItem) loaded;
            int variant = loadedTools(empty).indexOf(loaded);
            double expectedServing = new double[]{1, 5, 10, 15, 200, 1}[variant];
            BongItem.Contents expectedContents = variant < 4 ? BongItem.Contents.SALVIA : variant == 4 ? BongItem.Contents.CANNABIS : BongItem.Contents.DMT;
            helper.assertTrue(tool.doseUnits() == expectedServing && tool.contents() == expectedContents,
                    "Registered loaded tool has the wrong calibrated serving: " + loaded);
            for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) for (InteractionHand hand : InteractionHand.values()) {
                ServerPlayer player = player(helper);
                try {
                    player.setGameMode(mode);
                    InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    player.setItemInHand(other, new ItemStack(Items.STICK, 3));
                    ItemStack saved = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) new ItemStack(loaded).save(helper.getLevel().registryAccess()));
                    helper.assertTrue(saved.getMaxStackSize() == 1, "Loaded smoking tools stack and risk losing vessels");
                    player.setItemInHand(hand, saved);
                    helper.assertTrue(player.gameMode.useItem(player, helper.getLevel(), saved, hand).consumesAction(), "Loaded tool use did not consume action");
                    stack(helper, player.getItemInHand(hand), empty, 1, "Loaded tool did not return exactly its own empty vessel");
                    stack(helper, player.getItemInHand(other), Items.STICK, 3, "Smoking changed the other hand");
                    assertDose(helper, player, tool, 1);
                    for (Item sibling : loadedTools(empty)) helper.assertTrue(player.getCooldowns().isOnCooldown(sibling), "Family variant escaped the shared cooldown");
                    helper.assertTrue(player.getCooldowns().isOnCooldown(empty), "The empty vessel lacks cooldown");
                    helper.assertTrue(empty.use(helper.getLevel(), player, hand).getResult() == InteractionResult.PASS, "An empty vessel produced a hit");
                    assertDose(helper, player, tool, 1);
                    helper.assertTrue(player.getInventory().countItem(empty) == 1, "Smoking duplicated an empty vessel");
                    player.setItemInHand(hand, new ItemStack(loaded));
                    player.gameMode.useItem(player, helper.getLevel(), player.getItemInHand(hand), hand);
                    assertDose(helper, player, tool, 1);
                    stack(helper, player.getItemInHand(hand), loaded, 1, "Cooldown consumed a fresh loaded bowl");
                    for (int tick = 0; tick < 15; tick++) player.getCooldowns().tick();
                    player.gameMode.useItem(player, helper.getLevel(), player.getItemInHand(hand), hand);
                    assertDose(helper, player, tool, 2);
                    stack(helper, player.getItemInHand(hand), empty, 1, "Reloaded use lost the reusable vessel");
                } finally { SubstanceEngine.clear(player.getUUID()); }
            }
        }
        for (Item empty : emptyTools()) {
            ServerPlayer player = player(helper);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(empty));
            helper.assertTrue(empty.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.PASS
                    && SubstanceEngine.getExperience(player.getUUID()).isEmpty(), "An unused empty tool created a session");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void findingsAndCreativeInventoryExposeNewToolsAndBothGrindingYields(GameTestHelper helper) {
        var dmt = CursedFindingsRegistry.byId(CursedFindingIds.DMT).orElseThrow();
        for (int count : new int[]{1, 5}) helper.assertTrue(dmt.recipes().stream().anyMatch(card -> card.output().stack().is(ModItems.DMT_POWDER.get())
                && card.output().label().startsWith(count + "x ")
                && card.inputs().stream().anyMatch(input -> input.stack().is(count == 1 ? ModItems.DMT.get() : ModItems.DMT_5X.get()))),
                "DMT findings omit or mislabel a mortar yield: " + count);
        ServerPlayer fresh = player(helper);
        helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(fresh, CursedFindingIds.SMOKING_DEVICES),
                "Smoking Devices utility recipes require consuming a substance to discover");
        for (String chapter : List.of(CursedFindingIds.DMT, CursedFindingIds.CANNABIS, CursedFindingIds.SALVIA)) {
            var entry = CursedFindingsRegistry.byId(chapter).orElseThrow();
            helper.assertTrue(entry.recipes().stream().anyMatch(card -> card.output().stack().is(ModItems.ANCIENT_PIPE.get())), "Chapter omits empty pipe craft: " + chapter);
            for (Item item : loadedTools(ModItems.ANCIENT_PIPE.get())) {
                BongItem tool = (BongItem) item;
                boolean relevant = chapter.equals(CursedFindingIds.DMT) ? tool.contents() == BongItem.Contents.DMT
                        : chapter.equals(CursedFindingIds.CANNABIS) ? tool.contents() == BongItem.Contents.CANNABIS : tool.contents() == BongItem.Contents.SALVIA;
                if (relevant) helper.assertTrue(entry.recipes().stream().anyMatch(card -> card.output().stack().is(item)), "Chapter omits loaded pipe: " + item);
            }
        }
        List<Item> additions = new ArrayList<>(loadedTools(ModItems.ANCIENT_PIPE.get()));
        additions.addAll(List.of(ModItems.ANCIENT_PIPE.get(), ModItems.BONG_DMT.get(), ModItems.DMT_POWDER.get()));
        for (Item item : additions) helper.assertTrue(ModItems.creativeItems().stream().anyMatch(entry -> entry.get() == item), "Creative items omit " + item);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeBookDiscoversThePipeAndAllSevenNewLoadsFromRealInventoryCriteria(GameTestHelper helper) {
        for (String recipe : List.of("ancient_pipe", "bong_dmt", "ancient_pipe_dmt", "ancient_pipe_cannabis",
                "ancient_pipe_salvia_1x", "ancient_pipe_salvia_10x", "ancient_pipe_salvia_25x", "ancient_pipe_salvia_50x")) {
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(
                    ResourceLocation.fromNamespaceAndPath("trippycraft", "recipes/misc/" + recipe)) != null,
                    "New recipe advancement failed to load: " + recipe);
        }
        ServerPlayer crafter = discoveryPlayer(helper);
        crafter.getAdvancements();
        crafter.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT));
        crafter.getInventory().setItem(1, new ItemStack(Items.OAK_PLANKS));
        crafter.getInventory().setItem(2, new ItemStack(ModItems.ACACIA_BARK.get()));
        CriteriaTriggers.INVENTORY_CHANGED.trigger(crafter, crafter.getInventory(), crafter.getInventory().getItem(2));
        helper.assertTrue(crafter.getRecipeBook().contains(ResourceLocation.fromNamespaceAndPath("trippycraft", "ancient_pipe")),
                "Inventory criteria did not discover the base Ancient Pipe recipe");
        List<Item> loaded = new ArrayList<>(loadedTools(ModItems.ANCIENT_PIPE.get()));
        loaded.add(ModItems.BONG_DMT.get());
        for (int index = 0; index < loaded.size(); index++) {
            ServerPlayer player = discoveryPlayer(helper);
            player.getAdvancements();
            Item empty = index == 6 ? ModItems.BONG.get() : ModItems.ANCIENT_PIPE.get();
            Item ingredient = ingredients().get(index == 6 ? 5 : index);
            player.getInventory().setItem(0, new ItemStack(empty));
            player.getInventory().setItem(1, new ItemStack(ingredient));
            CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), player.getInventory().getItem(1));
            helper.assertTrue(player.getRecipeBook().contains(BuiltInRegistries.ITEM.getKey(loaded.get(index))),
                    "Inventory criteria did not discover a new smoking load: " + loaded.get(index));
        }
        helper.succeed();
    }

    private static void assertDose(GameTestHelper helper, ServerPlayer player, BongItem tool, int hits) {
        var experience = SubstanceEngine.getExperience(player.getUUID()).orElseThrow();
        double expected = tool.doseUnits() * hits;
        double actual = switch (tool.contents()) {
            case DMT -> experience.dmtState().targetDoseUnits();
            case CANNABIS -> experience.cannabisState().targetThcMilligrams();
            case SALVIA -> experience.salviaState().targetDoseUnits();
            default -> throw new IllegalStateException("Empty tool in loaded fixture");
        };
        helper.assertTrue(Math.abs(actual - expected) < 1e-6, "Tool changed its calibrated dose or repeated a hit: " + tool);
        String finding = switch (tool.contents()) { case DMT -> CursedFindingIds.DMT; case CANNABIS -> CursedFindingIds.CANNABIS; default -> CursedFindingIds.SALVIA; };
        helper.assertTrue(CursedRecipeCodex.isFindingUnlocked(player, finding), "Smoking did not unlock its actual substance finding");
        helper.assertTrue((tool.contents() == BongItem.Contents.DMT || !experience.dmtState().active())
                && (tool.contents() == BongItem.Contents.CANNABIS || !experience.cannabisState().active())
                && (tool.contents() == BongItem.Contents.SALVIA || !experience.salviaState().active()), "A loaded tool dosed a second substance");
    }

    private static void takeCraft(GameTestHelper helper, ServerPlayer player, int width, List<ItemStack> source, Item expected) {
        TransientCraftingContainer grid = new TransientCraftingContainer(player.inventoryMenu, width, width);
        for (int slot = 0; slot < source.size(); slot++) grid.setItem(slot, source.get(slot).copy());
        var holder = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid.asCraftInput(), helper.getLevel()).orElseThrow();
        ResultContainer result = new ResultContainer();
        result.setRecipeUsed(holder);
        result.setItem(0, holder.value().assemble(grid.asCraftInput(), helper.getLevel().registryAccess()));
        ResultSlot slot = new ResultSlot(player, grid, result, 0, 0, 0);
        ItemStack taken = slot.remove(1);
        slot.onTake(player, taken);
        stack(helper, taken, expected, 1, "Native ResultSlot returned the wrong craft");
        for (int index = 0; index < source.size(); index++) {
            ItemStack original = source.get(index), remaining = grid.getItem(index);
            if (original.getCount() <= 1) helper.assertTrue(remaining.isEmpty(), "Crafting retained a consumed vessel/ingredient");
            else stack(helper, remaining, original.getItem(), original.getCount() - 1, "Crafting consumed more than one serving");
        }
        helper.assertTrue(player.getInventory().isEmpty(), "Crafting inserted an unexpected empty-vessel remainder into inventory");
    }

    private static List<Item> emptyTools() { return List.of(ModItems.BONG.get(), ModItems.ANCIENT_PIPE.get()); }
    private static List<Item> ingredients() { return List.of(ModItems.SALVIA_1X.get(), ModItems.SALVIA_10X.get(), ModItems.SALVIA_25X.get(), ModItems.SALVIA_50X.get(), ModItems.DRIED_CANNABIS_BUD.get(), ModItems.DMT_POWDER.get()); }
    private static List<Item> loadedTools(Item empty) {
        return empty == ModItems.BONG.get() ? List.of(ModItems.BONG_SALVIA_1X.get(), ModItems.BONG_SALVIA_10X.get(), ModItems.BONG_SALVIA_25X.get(), ModItems.BONG_SALVIA_50X.get(), ModItems.BONG_CANNABIS.get(), ModItems.BONG_DMT.get())
                : List.of(ModItems.ANCIENT_PIPE_SALVIA_1X.get(), ModItems.ANCIENT_PIPE_SALVIA_10X.get(), ModItems.ANCIENT_PIPE_SALVIA_25X.get(), ModItems.ANCIENT_PIPE_SALVIA_50X.get(), ModItems.ANCIENT_PIPE_CANNABIS.get(), ModItems.ANCIENT_PIPE_DMT.get());
    }
    private static CraftingRecipe recipe(GameTestHelper helper, CraftingInput input, Item expected) {
        var holder = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(holder.isPresent(), "Missing crafting recipe for " + expected);
        stack(helper, holder.orElseThrow().value().assemble(input, helper.getLevel().registryAccess()), expected, 1, "Crafting yielded wrong product/count");
        return holder.orElseThrow().value();
    }
    private static void noRecipe(GameTestHelper helper, CraftingInput input, String message) { helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(), message); }
    private static CraftingInput input(int width, Item... items) {
        List<ItemStack> slots = blanks(width * width);
        for (int i = 0; i < items.length; i++) slots.set(i, new ItemStack(items[i]));
        return CraftingInput.of(width, width, slots);
    }
    private static List<ItemStack> blanks(int size) { List<ItemStack> result = new ArrayList<>(); for (int i = 0; i < size; i++) result.add(ItemStack.EMPTY); return result; }
    private static MortarAndPestleBlockEntity mortar(GameTestHelper helper) {
        BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(position, ModBlocks.MORTAR_AND_PESTLE.get().defaultBlockState(), Block.UPDATE_ALL);
        var mortar = (MortarAndPestleBlockEntity) helper.getLevel().getBlockEntity(position);
        mortar.clearContent();
        return mortar;
    }
    private static void tick(GameTestHelper helper, MortarAndPestleBlockEntity mortar, int count) { for (int tick = 0; tick < count; tick++) MortarAndPestleBlockEntity.serverTick(helper.getLevel(), mortar.getBlockPos(), mortar.getBlockState(), mortar); }
    private static ServerPlayer player(GameTestHelper helper) { ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "smoking-tools-test")); player.getInventory().clearContent(); return player; }
    private static ServerPlayer discoveryPlayer(GameTestHelper helper) {
        // NeoForge deliberately disables all advancements for normal FakePlayers.
        // Keep its dummy network, but exercise Minecraft's real listeners and rewards here.
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "smoking-recipes-test")) {
            @Override public boolean isFakePlayer() { return false; }
        };
    }
    private static void stack(GameTestHelper helper, ItemStack stack, Item expected, int count, String message) { helper.assertTrue(stack.is(expected) && stack.getCount() == count, message + ": " + stack); }
}
