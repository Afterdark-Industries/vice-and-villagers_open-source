package com.trippycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A compact, harvestable cluster rather than a vertically growing vanilla cactus. */
public final class MescalineCactusBlock extends Block {
    public static final MapCodec<MescalineCactusBlock> CODEC = simpleCodec(MescalineCactusBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5.7D, 0.0D, 1.2D, 10.3D, 15.6D, 5.8D),
            Block.box(1.3D, 0.0D, 3.9D, 5.7D, 12.8D, 8.3D),
            Block.box(10.3D, 0.0D, 3.9D, 14.7D, 12.8D, 8.3D),
            Block.box(5.9D, 0.0D, 5.9D, 10.1D, 11.1D, 10.1D),
            Block.box(1.3D, 0.0D, 8.7D, 5.7D, 11.1D, 13.1D),
            Block.box(10.3D, 0.0D, 8.7D, 14.7D, 11.1D, 13.1D),
            Block.box(5.7D, 0.0D, 10.5D, 10.3D, 10.4D, 15.1D));

    public MescalineCactusBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MescalineCactusBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static boolean isSuitableSoil(BlockState soil) {
        return soil.is(BlockTags.SAND)
                || soil.is(BlockTags.TERRACOTTA)
                || soil.is(Blocks.DIRT)
                || soil.is(Blocks.COARSE_DIRT)
                || soil.is(Blocks.GRASS_BLOCK)
                || soil.is(Blocks.ROOTED_DIRT);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Horizontal neighbours are intentionally allowed: the plant itself is already a cluster.
        return isSuitableSoil(level.getBlockState(pos.below())) && level.getFluidState(pos).isEmpty();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
