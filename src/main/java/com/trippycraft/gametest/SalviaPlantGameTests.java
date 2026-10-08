package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.block.CannabisPlantBlock;
import com.trippycraft.block.SalviaDivinorumPlantBlock;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import com.trippycraft.world.SalviaPatchFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SalviaPlantGameTests {
    private SalviaPlantGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void matureHarvestReturnsTwoSeedsAndFourFreshLeaves(GameTestHelper helper) {
        SalviaDivinorumPlantBlock plant = (SalviaDivinorumPlantBlock) ModBlocks.SALVIA_DIVINORUM_PLANT.get();
        BlockPos position = helper.absolutePos(new BlockPos(3, 2, 3));
        List<ItemStack> matureDrops = Block.getDrops(plant.getMatureState(), helper.getLevel(), position, null);
        helper.assertTrue(count(matureDrops, ModItems.SALVIA_SEEDS.get()) == 2, "Mature Salvia must return exactly two seeds");
        helper.assertTrue(count(matureDrops, ModItems.FRESH_SALVIA_LEAF.get()) == 4, "Mature Salvia must return exactly four fresh leaves");
        helper.assertTrue(matureDrops.stream().mapToInt(ItemStack::getCount).sum() == 6,
                "Mature Salvia returned extract or another unexpected item");
        for (int age = 0; age < SalviaDivinorumPlantBlock.MAX_AGE; age++) {
            List<ItemStack> drops = Block.getDrops(plant.defaultBlockState().setValue(SalviaDivinorumPlantBlock.AGE, age),
                    helper.getLevel(), position, null);
            helper.assertTrue(count(drops, ModItems.SALVIA_SEEDS.get()) == 1
                            && drops.stream().mapToInt(ItemStack::getCount).sum() == 1,
                    "An immature Salvia plant must return only its replanting seed");
        }
        helper.assertTrue(plant.getCloneItemStack(helper.getLevel(), position, plant.getMatureState()).is(ModItems.SALVIA_SEEDS.get()),
                "Pick-block did not return Salvia seeds");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void salviaSeedsReplantOneYoungPlantOnFarmland(GameTestHelper helper) {
        BlockPos soil = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos position = soil.above();
        helper.getLevel().setBlock(soil, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        ItemStack seeds = new ItemStack(ModItems.SALVIA_SEEDS.get(), 2);
        UseOnContext context = new UseOnContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, seeds,
                new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false));
        helper.assertTrue(seeds.getItem().useOn(context).consumesAction(), "Salvia seeds could not replant on farmland");
        BlockState planted = helper.getLevel().getBlockState(position);
        helper.assertTrue(planted.is(ModBlocks.SALVIA_DIVINORUM_PLANT.get()) && planted.getValue(SalviaDivinorumPlantBlock.AGE) == 0,
                "Replanting Salvia skipped its first growth stage");
        helper.assertTrue(seeds.getCount() == 1, "Replanting must consume exactly one seed");
        helper.assertFalse(seeds.getItem().useOn(context).consumesAction(), "Replanting overwrote an occupied crop block");
        helper.assertTrue(seeds.getCount() == 1, "A failed replant consumed a seed");
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(soil, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertFalse(seeds.getItem().useOn(context).consumesAction(), "Salvia seeds planted on untilled dirt");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void growthStagesAndTimingMatchCannabis(GameTestHelper helper) {
        BlockPos salviaPosition = helper.absolutePos(new BlockPos(3, 8, 3));
        BlockPos cannabisPosition = salviaPosition.east();
        for (int height = 0; height <= 32; height++) {
            helper.getLevel().setBlock(salviaPosition.above(height), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.getLevel().setBlock(cannabisPosition.above(height), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        helper.assertTrue(SalviaDivinorumPlantBlock.MAX_AGE == CannabisPlantBlock.MAX_AGE, "Salvia has a different stage count than cannabis");
        for (Block soil : List.of(Blocks.FARMLAND, Blocks.DIRT)) {
            helper.getLevel().setBlock(salviaPosition.below(), soil.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.getLevel().setBlock(cannabisPosition.below(), soil.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.getLevel().setBlock(salviaPosition, ModBlocks.SALVIA_DIVINORUM_PLANT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.getLevel().setBlock(cannabisPosition, ModBlocks.CANNABIS_PLANT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            RandomSource salviaRandom = RandomSource.create(37021L);
            RandomSource cannabisRandom = RandomSource.create(37021L);
            for (int tick = 0; tick < 100; tick++) {
                helper.getLevel().getBlockState(salviaPosition).randomTick(helper.getLevel(), salviaPosition, salviaRandom);
                helper.getLevel().getBlockState(cannabisPosition).randomTick(helper.getLevel(), cannabisPosition, cannabisRandom);
                helper.assertTrue(helper.getLevel().getBlockState(salviaPosition).getValue(SalviaDivinorumPlantBlock.AGE)
                                .equals(helper.getLevel().getBlockState(cannabisPosition).getValue(CannabisPlantBlock.AGE)),
                        "Salvia advanced at a different rate than cannabis on " + soil);
            }
            helper.assertTrue(helper.getLevel().getBlockState(salviaPosition).getValue(SalviaDivinorumPlantBlock.AGE) == 3,
                    "Salvia never matured under matching random ticks");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void naturalHabitatRequiresJungleCanopyAndNearbyWater(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        helper.assertTrue(SalviaPatchFeature.isAllowedBiome(biomes.getHolderOrThrow(Biomes.JUNGLE)), "Jungle habitat was rejected");
        helper.assertTrue(SalviaPatchFeature.isAllowedBiome(biomes.getHolderOrThrow(Biomes.SPARSE_JUNGLE)), "Sparse Jungle habitat was rejected");
        for (var biome : List.of(Biomes.BAMBOO_JUNGLE, Biomes.FOREST, Biomes.SWAMP, Biomes.PLAINS)) {
            helper.assertFalse(SalviaPatchFeature.isAllowedBiome(biomes.getHolderOrThrow(biome)), "Salvia leaked into disallowed biome " + biome);
        }
        BlockPos position = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos water = position.offset(2, -1, 0);
        BlockPos canopy = position.above(3);
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(canopy, Blocks.OAK_LEAVES.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(water, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertTrue(SalviaPatchFeature.hasCanopy(helper.getLevel(), position), "Overhead leaves did not count as canopy");
        helper.assertTrue(SalviaPatchFeature.hasNearbyWater(helper.getLevel(), position), "A waterside habitat was rejected");
        helper.setBiome(Biomes.JUNGLE);
        helper.assertTrue(SalviaPatchFeature.isNaturalHabitat(helper.getLevel(), position), "A shaded Jungle water bank was rejected");
        helper.setBiome(Biomes.PLAINS);
        helper.assertFalse(SalviaPatchFeature.isNaturalHabitat(helper.getLevel(), position), "A shaded Plains bank admitted wild Salvia");
        helper.setBiome(Biomes.JUNGLE);
        helper.getLevel().setBlock(water, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertFalse(SalviaPatchFeature.hasNearbyWater(helper.getLevel(), position), "A dry habitat admitted wild Salvia");
        helper.assertFalse(SalviaPatchFeature.isNaturalHabitat(helper.getLevel(), position), "A dry Jungle admitted wild Salvia");
        helper.getLevel().setBlock(water, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(canopy, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertFalse(SalviaPatchFeature.hasCanopy(helper.getLevel(), position), "Open sun was treated as leaf canopy");
        helper.assertFalse(SalviaPatchFeature.isNaturalHabitat(helper.getLevel(), position), "An open Jungle bank admitted wild Salvia");
        helper.succeed();
    }

    private static int count(List<ItemStack> drops, Item item) {
        return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
