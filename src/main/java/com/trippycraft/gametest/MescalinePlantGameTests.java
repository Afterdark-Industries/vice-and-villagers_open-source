package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import com.trippycraft.world.MescalineCactusPatchFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
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
public final class MescalinePlantGameTests {
    private MescalinePlantGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void harvestReturnsExactlyOneReplantableCactus(GameTestHelper helper) {
        BlockState cactus = ModBlocks.MESCALINE_CACTUS.get().defaultBlockState();
        BlockPos position = helper.absolutePos(new BlockPos(3, 3, 3));
        List<ItemStack> drops = Block.getDrops(cactus, helper.getLevel(), position, null);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(ModItems.MESCALINE_CACTUS.get())
                        && drops.getFirst().getCount() == 1,
                "A clustered Mescaline cactus must drop exactly one cactus, with no direct dose or extra drops");
        helper.assertTrue(cactus.getBlock().getCloneItemStack(helper.getLevel(), position, cactus)
                .is(ModItems.MESCALINE_CACTUS.get()), "Pick-block must return the replantable cactus item");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void harvestedCactusReplantsOnDrySoilsButNotStoneOrFarmland(GameTestHelper helper) {
        BlockPos soil = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos position = soil.above();
        for (Block ground : List.of(Blocks.SAND, Blocks.RED_SAND, Blocks.TERRACOTTA, Blocks.RED_TERRACOTTA,
                Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.GRASS_BLOCK, Blocks.ROOTED_DIRT)) {
            helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.getLevel().setBlock(soil, ground.defaultBlockState(), Block.UPDATE_CLIENTS);
            ItemStack item = new ItemStack(ModItems.MESCALINE_CACTUS.get(), 2);
            helper.assertTrue(item.useOn(plantingContext(helper, soil, item)).consumesAction(),
                    "Mescaline cactus could not replant on " + ground);
            helper.assertTrue(helper.getLevel().getBlockState(position).is(ModBlocks.MESCALINE_CACTUS.get())
                            && item.getCount() == 1,
                    "Replanting must place the clustered cactus and consume exactly one item");
        }
        for (Block ground : List.of(Blocks.STONE, Blocks.FARMLAND, Blocks.MUD, Blocks.GLASS)) {
            helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.getLevel().setBlock(soil, ground.defaultBlockState(), Block.UPDATE_CLIENTS);
            ItemStack item = new ItemStack(ModItems.MESCALINE_CACTUS.get(), 2);
            helper.assertFalse(item.useOn(plantingContext(helper, soil, item)).consumesAction(),
                    "Mescaline cactus planted on unsuitable soil " + ground);
            helper.assertTrue(item.getCount() == 2 && helper.getLevel().getBlockState(position).isAir(),
                    "Failed cactus planting consumed an item or placed a block");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void nearbyClustersSurviveButRemovingTheirSupportBreaksThem(GameTestHelper helper) {
        BlockPos position = helper.absolutePos(new BlockPos(3, 3, 3));
        BlockState cactus = ModBlocks.MESCALINE_CACTUS.get().defaultBlockState();
        helper.getLevel().setBlock(position.below(), Blocks.TERRACOTTA.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(position.east().below(), Blocks.RED_SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(position, cactus, Block.UPDATE_ALL);
        helper.getLevel().setBlock(position.east(), cactus, Block.UPDATE_ALL);
        helper.assertTrue(cactus.canSurvive(helper.getLevel(), position)
                        && cactus.canSurvive(helper.getLevel(), position.east()),
                "Mescaline cactus clusters must allow neighbouring cactus blocks");
        helper.assertFalse(cactus.canSurvive(helper.getLevel(), position.above()),
                "Mescaline cactus must not stack into vanilla cactus towers");
        helper.assertFalse(cactus.getCollisionShape(helper.getLevel(), position).isEmpty(),
                "The cactus cluster unexpectedly has no solid collision shape");
        helper.getLevel().setBlock(position.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockState(position).isAir(),
                "Removing support left a floating Mescaline cactus");
        helper.assertTrue(helper.getLevel().getBlockState(position.east()).is(ModBlocks.MESCALINE_CACTUS.get()),
                "Removing a neighbouring cactus also destroyed a supported cluster");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void naturalHabitatOnlyAllowsDesertAndTheBadlandsFamily(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (var biome : List.of(Biomes.DESERT, Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS)) {
            helper.assertTrue(MescalineCactusPatchFeature.isAllowedBiome(biomes.getHolderOrThrow(biome)),
                    "Requested cactus biome was rejected: " + biome);
        }
        for (var biome : List.of(Biomes.PLAINS, Biomes.SAVANNA, Biomes.JUNGLE, Biomes.BEACH, Biomes.STONY_PEAKS)) {
            helper.assertFalse(MescalineCactusPatchFeature.isAllowedBiome(biomes.getHolderOrThrow(biome)),
                    "Natural Mescaline cactus leaked into biome " + biome);
        }
        BlockPos position = helper.absolutePos(new BlockPos(3, 3, 3));
        helper.getLevel().setBlock(position.below(), Blocks.RED_TERRACOTTA.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.setBiome(Biomes.BADLANDS);
        helper.assertTrue(MescalineCactusPatchFeature.isNaturalHabitat(helper.getLevel(), position),
                "A dry Badlands terracotta habitat was rejected");
        helper.getLevel().setBlock(position, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertFalse(MescalineCactusPatchFeature.isNaturalHabitat(helper.getLevel(), position),
                "A flooded habitat admitted a natural cactus");
        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.setBiome(Biomes.PLAINS);
        helper.assertFalse(MescalineCactusPatchFeature.isNaturalHabitat(helper.getLevel(), position),
                "A Plains terracotta platform admitted natural cactus");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void registeredFeaturePlacesSmallClustersOnBadlandsTerracotta(GameTestHelper helper) {
        var configuredKey = ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "mescaline_cactus_patch"));
        var placedKey = ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "mescaline_cactus_drylands"));
        var configured = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(configuredKey);
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(placedKey) != null,
                "The natural Mescaline cactus placed feature did not load");
        clearDryFixture(helper);
        helper.setBiome(Biomes.BADLANDS);
        BlockPos origin = helper.absolutePos(new BlockPos(4, 3, 4));
        helper.assertTrue(configured.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
                        RandomSource.create(1234L), origin),
                "The registered cactus feature could not place a cluster on Badlands terracotta");
        int count = fixtureCacti(helper);
        helper.assertTrue(count >= 1 && count <= 3, "Natural patches must contain one to three clustered cactus blocks");
        clearDryFixture(helper);
        helper.setBiome(Biomes.PLAINS);
        helper.assertFalse(configured.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
                        RandomSource.create(1234L), origin),
                "The registered feature generated cactus in a disallowed biome");
        helper.assertTrue(fixtureCacti(helper) == 0, "A failed cactus feature wrote blocks into the habitat");
        helper.succeed();
    }

    private static UseOnContext plantingContext(GameTestHelper helper, BlockPos soil, ItemStack item) {
        return new UseOnContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, item,
                new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false));
    }

    private static void clearDryFixture(GameTestHelper helper) {
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, 2, z)),
                        Blocks.TERRACOTTA.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int y = 3; y <= 20; y++) {
                    helper.getLevel().setBlock(helper.absolutePos(new BlockPos(x, y, z)),
                            Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static int fixtureCacti(GameTestHelper helper) {
        int count = 0;
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                if (helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x, 3, z)))
                        .is(ModBlocks.MESCALINE_CACTUS.get())) {
                    count++;
                }
            }
        }
        return count;
    }
}
