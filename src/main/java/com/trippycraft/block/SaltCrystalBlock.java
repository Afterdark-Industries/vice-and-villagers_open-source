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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SaltCrystalBlock extends Block {
    public static final MapCodec<SaltCrystalBlock> CODEC = simpleCodec(SaltCrystalBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D),
            Block.box(2.0D, 12.0D, 2.0D, 5.0D, 15.0D, 5.0D),
            Block.box(5.0D, 12.0D, 7.0D, 8.0D, 16.0D, 10.0D),
            Block.box(8.0D, 12.0D, 5.0D, 11.0D, 16.0D, 8.0D),
            Block.box(11.0D, 12.0D, 3.0D, 14.0D, 15.0D, 6.0D),
            Block.box(10.0D, 12.0D, 10.0D, 13.0D, 16.0D, 13.0D),
            Block.box(4.0D, 12.0D, 10.0D, 7.0D, 14.0D, 13.0D),
            Block.box(7.0D, 12.0D, 12.0D, 9.0D, 15.0D, 14.0D),
            Block.box(3.0D, 12.0D, 6.0D, 5.0D, 14.0D, 8.0D),
            Block.box(12.0D, 12.0D, 7.0D, 14.0D, 14.0D, 9.0D));

    public SaltCrystalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SaltCrystalBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        return belowState.is(BlockTags.BASE_STONE_OVERWORLD)
                || belowState.is(Blocks.DEEPSLATE)
                || belowState.is(Blocks.TUFF)
                || belowState.is(Blocks.CALCITE)
                || belowState.is(Blocks.DRIPSTONE_BLOCK)
                || belowState.is(Blocks.PACKED_ICE)
                || belowState.is(Blocks.BLUE_ICE)
                || belowState.is(Blocks.ICE)
                || belowState.is(Blocks.SNOW_BLOCK)
                || belowState.is(Blocks.GRAVEL);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
