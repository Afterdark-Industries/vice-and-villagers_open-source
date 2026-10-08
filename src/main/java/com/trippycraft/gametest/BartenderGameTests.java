package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.economy.BartenderService;
import com.trippycraft.economy.CustomerService;
import com.trippycraft.economy.DealerRegistries;
import com.trippycraft.economy.DealerService;
import com.trippycraft.economy.MarketRegistries;
import com.trippycraft.economy.MarketService;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import com.trippycraft.registry.ModSounds;
import com.trippycraft.registry.ModVillagers;
import com.trippycraft.world.TavernAudioModel;
import com.trippycraft.world.TavernPlaylist;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BartenderGameTests {
    // GameTest's relative origin sits one block below the structure template.
    private static final BlockPos STATION = new BlockPos(4, 2, 11);
    private static final BlockPos ANCHOR = new BlockPos(4, 2, 9);

    private BartenderGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void oakTaproomLoads(GameTestHelper helper) {
        assertStationAndRegionalRoof(helper, Blocks.SPRUCE_STAIRS);
    }

    @GameTest(template = "village/bars/desert_cantina", rotationSteps = 1)
    public static void desertCantinaLoads(GameTestHelper helper) {
        assertStationAndRegionalRoof(helper, Blocks.RED_SANDSTONE_STAIRS);
    }

    @GameTest(template = "village/bars/savanna_tavern", rotationSteps = 2)
    public static void savannaTavernLoads(GameTestHelper helper) {
        assertStationAndRegionalRoof(helper, Blocks.ACACIA_STAIRS);
    }

    @GameTest(template = "village/bars/snowy_lodge", rotationSteps = 3)
    public static void snowyLodgeLoads(GameTestHelper helper) {
        assertStationAndRegionalRoof(helper, Blocks.SPRUCE_STAIRS);
    }

    @GameTest(template = "village/bars/taiga_alehouse")
    public static void taigaAlehouseLoads(GameTestHelper helper) {
        assertStationAndRegionalRoof(helper, Blocks.DARK_OAK_STAIRS);
    }

    @GameTest(template = "village/bars/oak_taproom", timeoutTicks = 40)
    public static void bartenderIsAnchoredAndStocksEveryAlcohol(GameTestHelper helper) {
        BlockPos absoluteAnchor = helper.absolutePos(ANCHOR);
        BlockPos absoluteStation = helper.absolutePos(STATION);
        Villager bartender = helper.spawn(EntityType.VILLAGER, ANCHOR);
        BartenderService.configure(bartender, absoluteAnchor, absoluteStation);
        bartender.teleportTo(absoluteAnchor.getX() + 3.5D, absoluteAnchor.getY(), absoluteAnchor.getZ() + 0.5D);

        helper.runAfterDelay(5, () -> {
            Vec3 expected = Vec3.atBottomCenterOf(absoluteAnchor);
            helper.assertTrue(bartender.position().distanceToSqr(expected) < 0.05D,
                    "Bartender moved away from the behind-counter anchor");
            helper.assertTrue(bartender.isNoAi(), "Bartender AI must remain disabled so schedules cannot pull it away");
            helper.assertTrue(bartender.getVillagerData().getProfession() == ModVillagers.BARTENDER.get(),
                    "Bartender did not keep the registered bartender profession");
            helper.assertFalse(DealerService.isDealerVillager(bartender), "Bartender was incorrectly converted into a dealer");
            helper.assertFalse(MarketService.isMarketVillager(bartender), "Bartender was incorrectly converted into a broker");
            helper.assertFalse(CustomerService.isCustomerVillager(bartender), "Bartender was incorrectly converted into a customer");

            Set<Item> sold = bartender.getOffers().stream()
                    .map(offer -> offer.getResult().getItem())
                    .collect(Collectors.toSet());
            Set<Item> expectedStock = Set.of(
                    ModItems.BEER_BOTTLE.get(),
                    ModItems.WINE_GLASS_5OZ.get(),
                    ModItems.WINE_GLASS_9OZ.get(),
                    ModItems.WHISKEY_GLASS.get(),
                    ModItems.RUM_GLASS.get(),
                    ModItems.VODKA_GLASS.get()
            );
            helper.assertTrue(sold.containsAll(expectedStock), "Bartender did not stock all six alcohol items");
            helper.assertTrue(bartender.getOffers().size() == expectedStock.size(),
                    "Bartender must sell alcohol and nothing from the dealer catalogue");
            bartender.getOffers().forEach(offer -> {
                int expectedCost = offer.getResult().is(ModItems.WINE_GLASS_9OZ.get()) ? 2 : 1;
                helper.assertTrue(offer.getBaseCostA().is(net.minecraft.world.item.Items.EMERALD)
                                && offer.getBaseCostA().getCount() == expectedCost,
                        "Bartender alcohol price did not match the one-emerald rule");
                helper.assertTrue(offer.getResult().getCount() == 1,
                        "Every bartender trade must dispense exactly one drink");
            });
            helper.assertTrue(DealerRegistries.productsInCategory(MarketRegistries.CATEGORY_ALCOHOL).isEmpty(),
                    "Dealers must not sell any alcohol products");
            helper.succeed();
        });
    }

    private static void assertSingleStation(GameTestHelper helper) {
        helper.assertBlockPresent(ModBlocks.BARTENDER_STATION.get(), STATION);
        int stationCount = 0;
        for (int y = 1; y <= 9; y++) {
            for (int z = 0; z < 14; z++) {
                for (int x = 0; x < 10; x++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.BARTENDER_STATION.get())) {
                        stationCount++;
                    }
                }
            }
        }
        helper.assertTrue(stationCount == 1, "Regional tavern must contain exactly one bartender station");
    }

    private static void assertStationAndRegionalRoof(GameTestHelper helper, Block roofBlock) {
        assertSingleStation(helper);
        helper.assertBlockPresent(Blocks.JUKEBOX, new BlockPos(2, 2, 7));
        helper.assertBlockPresent(Blocks.PEARLESCENT_FROGLIGHT, new BlockPos(3, 5, 4));
        helper.assertBlockPresent(Blocks.VERDANT_FROGLIGHT, new BlockPos(6, 5, 4));
        helper.assertBlockPresent(Blocks.OCHRE_FROGLIGHT, new BlockPos(3, 5, 7));
        helper.assertBlockPresent(Blocks.SEA_LANTERN, new BlockPos(6, 5, 7));
        helper.assertTrue(ModSounds.TAVERN_TRACKS.size() == 19,
                "Every tavern must expose the complete nineteen-track rotation");
        helper.assertTrue(TavernPlaylist.trackCount() == ModSounds.TAVERN_TRACKS.size(),
                "Playlist timing metadata must cover every registered track");
        helper.assertTrue(TavernPlaylist.selection(0L).trackIndex() == 0,
                "Playlist must begin with its first authored track");
        helper.assertTrue(TavernPlaylist.selection(4_362L).trackIndex() == 1,
                "Persistent playlist clock did not advance at the first track boundary");
        helper.assertTrue(TavernPlaylist.selection(TavernPlaylist.totalCycleTicks()).trackIndex() == 0,
                "Persistent playlist clock did not wrap cleanly");
        helper.assertTrue(TavernAudioModel.fadeForOutsideDistance(1.0D) == 1.0F,
                "Tavern audio must remain full volume through one block outside");
        helper.assertTrue(TavernAudioModel.fadeForOutsideDistance(5.0D) > 0.0F
                        && TavernAudioModel.fadeForOutsideDistance(5.0D) < 1.0F,
                "Tavern audio must fade smoothly between one and ten blocks outside");
        helper.assertTrue(TavernAudioModel.fadeForOutsideDistance(10.0D) == 0.0F,
                "Tavern audio must be completely silent ten blocks outside");
        // The custom tavern has a two-wide ridge running through its full
        // 14-block depth at template y=8 (GameTest-relative y=9).
        helper.assertBlockPresent(roofBlock, new BlockPos(4, 9, 0));
        helper.assertBlockPresent(roofBlock, new BlockPos(4, 9, 6));
        helper.assertBlockPresent(roofBlock, new BlockPos(4, 9, 13));
        helper.succeed();
    }
}
