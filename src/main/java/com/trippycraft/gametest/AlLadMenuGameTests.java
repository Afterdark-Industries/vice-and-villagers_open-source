package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.block.entity.AdvancedChemistryTableBlockEntity;
import com.trippycraft.block.entity.BrewCauldronBlockEntity;
import com.trippycraft.menu.AdvancedChemistryTableMenu;
import com.trippycraft.menu.BrewCauldronMenu;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;
import java.util.function.Supplier;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlLadMenuGameTests {
    private static final BlockPos STATION = new BlockPos(3, 2, 3);

    private AlLadMenuGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void extractFirstShiftClickRoutesTheAlLadReagentToLiquid(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
        BrewCauldronMenu menu = new BrewCauldronMenu(1, player.getInventory(), brew, new SimpleContainerData(4));
        player.getInventory().setItem(9, new ItemStack(ModItems.LYSERGIC_EXTRACT.get(), 3));
        player.getInventory().setItem(10, new ItemStack(ModItems.AMINOETHYL_REAGENT.get(), 4));
        helper.assertFalse(menu.quickMoveStack(player, 5).isEmpty(), "The extract did not shift into the cauldron");
        helper.assertFalse(menu.quickMoveStack(player, 6).isEmpty(), "The reagent did not shift into the cauldron");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.LYSERGIC_EXTRACT.get(), 3,
                "Extract-first routing changed the primary ingredient");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), ModItems.AMINOETHYL_REAGENT.get(), 4,
                "Extract-first routing did not put the reagent in the liquid slot");
        helper.assertTrue(player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(10).isEmpty(),
                "Shifted ingredients remained duplicated in player inventory");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void reagentFirstShiftClickMovesItsWholeStackBeforeInsertingExtract(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
        BrewCauldronMenu menu = new BrewCauldronMenu(1, player.getInventory(), brew, new SimpleContainerData(4));
        ItemStack reagent = new ItemStack(ModItems.AMINOETHYL_REAGENT.get(), 4);
        CustomData.update(DataComponents.CUSTOM_DATA, reagent, tag -> tag.putInt("AlLadRoutingMarker", 123));
        ItemStack original = reagent.copy();
        player.getInventory().setItem(9, reagent);
        player.getInventory().setItem(10, new ItemStack(ModItems.LYSERGIC_EXTRACT.get(), 3));
        helper.assertFalse(menu.quickMoveStack(player, 5).isEmpty(), "The initial reagent did not shift into the cauldron");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.AMINOETHYL_REAGENT.get(), 4,
                "The initial reagent must keep the existing 4-HO-MET primary route");
        helper.assertFalse(menu.quickMoveStack(player, 6).isEmpty(), "The later extract did not shift into the cauldron");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.LYSERGIC_EXTRACT.get(), 3,
                "The later extract did not take the primary slot");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), ModItems.AMINOETHYL_REAGENT.get(), 4,
                "Reagent-first routing lost or duplicated reagent quantities");
        helper.assertTrue(ItemStack.isSameItemSameComponents(original, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT)),
                "Moving the reagent between machine slots lost its saved components");
        helper.assertTrue(player.getInventory().getItem(9).isEmpty() && player.getInventory().getItem(10).isEmpty(),
                "Reagent-first routing left duplicate ingredients in player inventory");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void ethanolRoutingAndOccupiedLiquidKeepTheHoMetInputsIntact(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BrewCauldronBlockEntity brew = place(helper, ModBlocks.BREW_CAULDRON, BrewCauldronBlockEntity.class);
        BrewCauldronMenu menu = new BrewCauldronMenu(1, player.getInventory(), brew, new SimpleContainerData(4));
        player.getInventory().setItem(9, new ItemStack(ModItems.AMINOETHYL_REAGENT.get(), 4));
        player.getInventory().setItem(10, new ItemStack(ModItems.ETHANOL.get(), 2));
        player.getInventory().setItem(11, new ItemStack(ModItems.LYSERGIC_EXTRACT.get(), 3));
        helper.assertFalse(menu.quickMoveStack(player, 5).isEmpty(), "4-HO-MET reagent did not shift into the primary slot");
        helper.assertFalse(menu.quickMoveStack(player, 6).isEmpty(), "4-HO-MET Ethanol did not shift into the liquid slot");
        helper.assertTrue(menu.quickMoveStack(player, 7).isEmpty(), "An occupied liquid slot must block the AL-LAD ingredient swap");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.BARK_SLOT), ModItems.AMINOETHYL_REAGENT.get(), 4,
                "A blocked AL-LAD swap changed the 4-HO-MET reagent");
        assertStack(helper, brew.getItem(BrewCauldronBlockEntity.WATER_SLOT), ModItems.ETHANOL.get(), 2,
                "A blocked AL-LAD swap changed the existing Ethanol");
        assertStack(helper, player.getInventory().getItem(11), ModItems.LYSERGIC_EXTRACT.get(), 3,
                "A blocked AL-LAD swap changed the player's extract stock");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void advancedMenuRoutesMaterialsAndPreservesReturnedBottleUpdates(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        AdvancedChemistryTableBlockEntity table = place(helper, ModBlocks.ADVANCED_CHEMISTRY_TABLE, AdvancedChemistryTableBlockEntity.class);
        AdvancedChemistryTableMenu menu = new AdvancedChemistryTableMenu(1, player.getInventory(), table, new SimpleContainerData(3));
        player.getInventory().setItem(9, new ItemStack(ModItems.AL_LAD_CONCENTRATE.get()));
        player.getInventory().setItem(10, new ItemStack(ModItems.BLOTTER_SHEET.get(), 3));
        helper.assertFalse(menu.quickMoveStack(player, 3).isEmpty(), "The concentrate did not shift into the Advanced Chemistry Table");
        helper.assertFalse(menu.quickMoveStack(player, 4).isEmpty(), "Blotter sheets did not shift into the Advanced Chemistry Table");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT), ModItems.AL_LAD_CONCENTRATE.get(), 1,
                "The concentrate did not use the primary slot");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT), ModItems.BLOTTER_SHEET.get(), 3,
                "Blotter sheets did not use the secondary slot");
        helper.assertTrue(menu.getSlot(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT)
                .getMaxStackSize(new ItemStack(ModItems.AL_LAD_CONCENTRATE.get())) == 1, "The concentrate slot accepts shared bottle charges");

        table.clearContent();
        player.getInventory().setItem(11, new ItemStack(Items.GLASS_BOTTLE, 2));
        helper.assertFalse(menu.quickMoveStack(player, 5).isEmpty(), "4-HO-MET glass bottles did not shift into the table");
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).isEmpty(),
                "Adding an exhausted-bottle return path changed where player glass bottles are inserted");
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.GLASS_SLOT), Items.GLASS_BOTTLE, 2,
                "4-HO-MET glass bottles no longer use the secondary slot");

        table.clearContent();
        // The network slot-update path calls this setter when the final concentrate charge returns its bottle.
        menu.setItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, 0, new ItemStack(Items.GLASS_BOTTLE));
        assertStack(helper, table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT), Items.GLASS_BOTTLE, 1,
                "The primary-slot client update discarded the exhausted concentrate bottle");
        helper.assertFalse(menu.getSlot(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).mayPlace(new ItemStack(Items.GLASS_BOTTLE)),
                "Returned bottle synchronization must not let players insert bottles as primary reagents");
        helper.assertFalse(menu.quickMoveStack(player, AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).isEmpty(),
                "The exhausted concentrate bottle could not be shifted out");
        helper.assertTrue(table.getItem(AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT).isEmpty(),
                "The exhausted bottle remained duplicated in its machine slot");
        int returnedBottles = 0;
        for (int inventorySlot = 0; inventorySlot < player.getInventory().getContainerSize(); inventorySlot++) {
            ItemStack stack = player.getInventory().getItem(inventorySlot);
            if (stack.is(Items.GLASS_BOTTLE)) {
                returnedBottles += stack.getCount();
            }
        }
        helper.assertTrue(returnedBottles == 1, "Shifting the returned bottle lost it or duplicated it in inventory");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "al-lad-menu-test"));
        player.getInventory().clearContent();
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

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count, String message) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, message + ": " + stack);
    }
}
