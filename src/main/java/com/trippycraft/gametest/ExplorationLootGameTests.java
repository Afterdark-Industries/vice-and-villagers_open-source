package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.item.BongItem;
import com.trippycraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExplorationLootGameTests {
    private static final int SAMPLES_PER_TABLE = 512;

    private ExplorationLootGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void villageHousesOfferStarterDosesAndEmptyBongs(GameTestHelper helper) {
        Set<Item> starters = Set.of(ModItems.MDMA_25MG.get(), ModItems.TWO_CB_25MG.get(), ModItems.BONG.get());
        for (String house : List.of("desert", "plains", "savanna", "taiga", "snowy")) {
            assertChestFinds(helper, "village/village_" + house + "_house", starters, Set.of());
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void researchAndAncientChestsOfferCanonicalPsychedelicDoses(GameTestHelper helper) {
        assertChestFinds(helper, "village/village_cartographer", Set.of(
                ModItems.HO_MET_25MG.get(), ModItems.HO_MET_100MG.get(),
                ModItems.AL_LAD_25MG.get(), ModItems.AL_LAD_100MG.get()), Set.of());
        assertChestFinds(helper, "stronghold_library", Set.of(
                ModItems.HO_MET_100MG.get(), ModItems.HO_MET_250MG.get(),
                ModItems.AL_LAD_100MG.get(), ModItems.AL_LAD_250MG.get(),
                ModItems.TWO_CE_25MG.get(), ModItems.TWO_CE_100MG.get()), Set.of());
        assertChestFinds(helper, "ancient_city", Set.of(
                ModItems.TWO_CE_100MG.get(), ModItems.TWO_CE_250MG.get(),
                ModItems.HO_MET_250MG.get(), ModItems.AL_LAD_250MG.get()), Set.of());
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void mansionWorkshopsAndSupplyChestsOfferPartySupplies(GameTestHelper helper) {
        assertChestFinds(helper, "woodland_mansion", Set.of(
                ModItems.MDMA_100MG.get(), ModItems.MDMA_250MG.get(),
                ModItems.TWO_CB_100MG.get(), ModItems.TWO_CB_250MG.get(),
                ModItems.NITROUS.get(), ModItems.BONG.get()), Set.of());
        for (String workshop : List.of("armorer", "toolsmith", "weaponsmith")) {
            assertChestFinds(helper, "village/village_" + workshop, Set.of(ModItems.NITROUS.get()), Set.of());
        }
        assertChestFinds(helper, "shipwreck_supply", Set.of(ModItems.NITROUS.get()), Set.of());
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void biomeStructuresOfferMescalineAndPreserveSalviaStrengths(GameTestHelper helper) {
        assertChestFinds(helper, "desert_pyramid", Set.of(
                ModItems.MESCALINE_25MG.get(), ModItems.MESCALINE_100MG.get()), Set.of());
        assertChestFinds(helper, "jungle_temple", Set.of(ModItems.BONG.get()), Set.of(
                ModItems.SALVIA_1X.get(), ModItems.SALVIA_10X.get(), ModItems.SALVIA_25X.get()));
        assertChestFinds(helper, "village/village_temple", Set.of(), Set.of(
                ModItems.SALVIA_1X.get(), ModItems.SALVIA_10X.get(),
                ModItems.SALVIA_25X.get(), ModItems.SALVIA_50X.get()));
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void unmappedVanillaChestsReceiveNoModItems(GameTestHelper helper) {
        LootParams params = chestParams(helper);
        for (String chest : List.of("nether_bridge", "end_city_treasure")) {
            LootTable table = loadedChest(helper, chest);
            boolean foundVanillaItem = false;
            for (int sample = 0; sample < SAMPLES_PER_TABLE; sample++) {
                for (ItemStack stack : table.getRandomItems(params, sampleSeed(chest, sample))) {
                    String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
                    helper.assertFalse(namespace.equals(TrippyCraft.MOD_ID),
                            "Substance loot leaked into unmapped minecraft:chests/" + chest);
                    foundVanillaItem |= namespace.equals("minecraft");
                }
            }
            helper.assertTrue(foundVanillaItem, "Unmapped vanilla chest produced no vanilla loot: " + chest);
        }
        helper.succeed();
    }

    private static void assertChestFinds(GameTestHelper helper, String chest,
                                         Set<Item> expectedBonusItems, Set<Item> expectedExistingItems) {
        LootTable table = loadedChest(helper, chest);
        LootParams params = chestParams(helper);
        Set<Item> bonusItems = bonusItems();
        Set<Item> observed = new HashSet<>();
        int samplesWithBonus = 0;
        boolean foundVanillaItem = false;
        for (int sample = 0; sample < SAMPLES_PER_TABLE; sample++) {
            int bonusUnits = 0;
            // This public generation path applies NeoForge global modifiers using the vanilla table ID.
            for (ItemStack stack : table.getRandomItems(params, sampleSeed(chest, sample))) {
                if (stack.isEmpty()) {
                    continue;
                }
                Item item = stack.getItem();
                observed.add(item);
                foundVanillaItem |= BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("minecraft");
                if (bonusItems.contains(item)) {
                    helper.assertTrue(expectedBonusItems.contains(item),
                            "Unexpected bonus item in minecraft:chests/" + chest + ": " + BuiltInRegistries.ITEM.getKey(item));
                    bonusUnits += stack.getCount();
                }
                if (item instanceof BongItem bong) {
                    helper.assertTrue(stack.is(ModItems.BONG.get()) && bong.doseUnits() == 0.0D,
                            "Exploration loot supplied a loaded bong instead of an empty reusable bong");
                }
            }
            helper.assertTrue(bonusUnits <= 1,
                    "Rare bonus loot exceeded one item in minecraft:chests/" + chest + " at sample " + sample);
            if (bonusUnits > 0) {
                samplesWithBonus++;
            }
        }
        helper.assertTrue(foundVanillaItem, "Substance injection displaced all vanilla loot: " + chest);
        for (Item item : expectedBonusItems) {
            assertObserved(helper, chest, observed, item);
        }
        for (Item item : expectedExistingItems) {
            assertObserved(helper, chest, observed, item);
        }
        if (!expectedBonusItems.isEmpty()) {
            helper.assertTrue(samplesWithBonus > 0 && samplesWithBonus < SAMPLES_PER_TABLE,
                    "Bonus loot must be an occasional exploration find in " + chest);
        }
        TrippyCraft.LOGGER.info("Exploration loot checked minecraft:chests/{}: {} seeded samples, {} with bonus items",
                chest, SAMPLES_PER_TABLE, samplesWithBonus);
    }

    private static void assertObserved(GameTestHelper helper, String chest, Set<Item> observed, Item item) {
        helper.assertTrue(observed.contains(item),
                "Loaded minecraft:chests/" + chest + " never generated " + BuiltInRegistries.ITEM.getKey(item)
                        + " across " + SAMPLES_PER_TABLE + " deterministic samples");
    }

    private static LootTable loadedChest(GameTestHelper helper, String chest) {
        ResourceLocation id = ResourceLocation.withDefaultNamespace("chests/" + chest);
        LootTable table = helper.getLevel().getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
        helper.assertTrue(table != LootTable.EMPTY && id.equals(table.getLootTableId()),
                "Vanilla chest loot table was not loaded with its actual ID: " + id);
        return table;
    }

    private static LootParams chestParams(GameTestHelper helper) {
        return new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3))))
                .create(LootContextParamSets.CHEST);
    }

    private static long sampleSeed(String chest, int sample) {
        // Nonzero seeds bypass mutable world random sequences, keeping the sampled item choices reproducible.
        return 0x6A09E667F3BCC909L ^ ((long) chest.hashCode() << 32) ^ (sample + 1L);
    }

    private static Set<Item> bonusItems() {
        return Set.of(ModItems.MDMA_25MG.get(), ModItems.MDMA_100MG.get(), ModItems.MDMA_250MG.get(), ModItems.MDMA_500MG.get(),
                ModItems.TWO_CB_25MG.get(), ModItems.TWO_CB_100MG.get(), ModItems.TWO_CB_250MG.get(), ModItems.TWO_CB_500MG.get(),
                ModItems.HO_MET_25MG.get(), ModItems.HO_MET_100MG.get(), ModItems.HO_MET_250MG.get(), ModItems.HO_MET_500MG.get(),
                ModItems.AL_LAD_25MG.get(), ModItems.AL_LAD_100MG.get(), ModItems.AL_LAD_250MG.get(), ModItems.AL_LAD_500MG.get(),
                ModItems.TWO_CE_25MG.get(), ModItems.TWO_CE_100MG.get(), ModItems.TWO_CE_250MG.get(), ModItems.TWO_CE_500MG.get(),
                ModItems.MESCALINE_25MG.get(), ModItems.MESCALINE_100MG.get(), ModItems.MESCALINE_250MG.get(), ModItems.MESCALINE_500MG.get(),
                ModItems.NITROUS.get(), ModItems.BONG.get());
    }
}
