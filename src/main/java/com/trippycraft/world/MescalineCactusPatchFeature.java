package com.trippycraft.world;

import com.trippycraft.block.MescalineCactusBlock;
import com.trippycraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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

/** Modest desert/badlands patches, with a biome check on every individual cluster. */
public final class MescalineCactusPatchFeature extends Feature<NoneFeatureConfiguration> {
    public MescalineCactusPatchFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState cactus = ModBlocks.MESCALINE_CACTUS.get().defaultBlockState();
        for (int anchorAttempt = 0; anchorAttempt < 16; anchorAttempt++) {
            BlockPos anchor = surface(level, origin.getX() + random.nextInt(13) - 6,
                    origin.getZ() + random.nextInt(13) - 6);
            if (!isNaturalHabitat(level, anchor)) {
                continue;
            }
            level.setBlock(anchor, cactus, Block.UPDATE_CLIENTS);
            int placed = 1;
            int desiredSize = 1 + random.nextInt(3);
            for (int clusterAttempt = 0; clusterAttempt < 16 && placed < desiredSize; clusterAttempt++) {
                BlockPos candidate = surface(level, anchor.getX() + random.nextInt(7) - 3,
                        anchor.getZ() + random.nextInt(7) - 3);
                if (Math.abs(candidate.getY() - anchor.getY()) <= 2 && isNaturalHabitat(level, candidate)) {
                    level.setBlock(candidate, cactus, Block.UPDATE_CLIENTS);
                    placed++;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isAllowedBiome(Holder<Biome> biome) {
        return biome.is(Biomes.DESERT)
                || biome.is(Biomes.BADLANDS)
                || biome.is(Biomes.ERODED_BADLANDS)
                || biome.is(Biomes.WOODED_BADLANDS);
    }

    public static boolean isNaturalHabitat(LevelReader level, BlockPos pos) {
        return isAllowedBiome(level.getBiome(pos))
                && level.getBlockState(pos).isAir()
                && level.getFluidState(pos).isEmpty()
                && MescalineCactusBlock.isSuitableSoil(level.getBlockState(pos.below()));
    }

    private static BlockPos surface(WorldGenLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }
}
