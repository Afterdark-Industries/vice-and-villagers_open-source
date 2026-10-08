package com.trippycraft.world;

import com.trippycraft.block.SalviaDivinorumPlantBlock;
import com.trippycraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/** Small waterside patches whose individual plants stay beneath jungle leaves. */
public final class SalviaPatchFeature extends Feature<NoneFeatureConfiguration> {
    public static final int WATER_RADIUS = 6;
    public static final int CANOPY_HEIGHT = 32;

    public SalviaPatchFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState mature = ((SalviaDivinorumPlantBlock) ModBlocks.SALVIA_DIVINORUM_PLANT.get()).getMatureState();

        // Search a small part of this chunk for a suitable bank instead of requiring a lucky
        // exact waterside origin. Rarity is applied once per patch in the placed feature.
        for (int anchorAttempt = 0; anchorAttempt < 24; anchorAttempt++) {
            BlockPos anchor = surface(level, origin.getX() + random.nextInt(13) - 6,
                    origin.getZ() + random.nextInt(13) - 6);
            if (!isNaturalHabitat(level, anchor) || !mature.canSurvive(level, anchor)) {
                continue;
            }

            int desiredSize = 2 + random.nextInt(4);
            List<BlockPos> patch = new ArrayList<>();
            patch.add(anchor);
            for (int clusterAttempt = 0; clusterAttempt < 32 && patch.size() < desiredSize; clusterAttempt++) {
                BlockPos candidate = surface(level, anchor.getX() + random.nextInt(7) - 3,
                        anchor.getZ() + random.nextInt(7) - 3);
                if (!patch.contains(candidate) && Math.abs(candidate.getY() - anchor.getY()) <= 2
                        && isNaturalHabitat(level, candidate) && mature.canSurvive(level, candidate)) {
                    patch.add(candidate);
                }
            }
            if (patch.size() < 2) {
                continue;
            }
            patch.forEach(pos -> level.setBlock(pos, mature, Block.UPDATE_CLIENTS));
            return true;
        }
        return false;
    }

    public static boolean isAllowedBiome(Holder<Biome> biome) {
        return biome.is(Biomes.JUNGLE) || biome.is(Biomes.SPARSE_JUNGLE);
    }

    public static boolean isNaturalHabitat(LevelReader level, BlockPos pos) {
        return isAllowedBiome(level.getBiome(pos))
                && level.getBlockState(pos).isAir()
                && level.getFluidState(pos).isEmpty()
                && hasCanopy(level, pos)
                && hasNearbyWater(level, pos);
    }

    public static boolean hasCanopy(LevelReader level, BlockPos pos) {
        // Direct overhead foliage excludes open sun, roofs, caves and nearby-but-unshaded trees.
        for (int height = 2; height <= CANOPY_HEIGHT; height++) {
            if (level.getBlockState(pos.above(height)).is(BlockTags.LEAVES)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasNearbyWater(LevelReader level, BlockPos pos) {
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (int dx = -WATER_RADIUS; dx <= WATER_RADIUS; dx++) {
            for (int dz = -WATER_RADIUS; dz <= WATER_RADIUS; dz++) {
                if (dx * dx + dz * dz > WATER_RADIUS * WATER_RADIUS) {
                    continue;
                }
                for (int dy = -3; dy <= 0; dy++) {
                    probe.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (level.getFluidState(probe).is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static BlockPos surface(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }
}
