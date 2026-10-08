package com.trippycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class StrangeMushroomBlock extends BushBlock {
    @SuppressWarnings("unchecked")
    public static final MapCodec<BushBlock> CODEC = (MapCodec<BushBlock>)(MapCodec<?>)simpleCodec(StrangeMushroomBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(6.0D, 0.0D, 6.0D, 10.0D, 6.0D, 10.0D),
            Block.box(2.0D, 6.0D, 2.0D, 14.0D, 10.0D, 14.0D),
            Block.box(1.0D, 0.0D, 2.0D, 5.0D, 5.0D, 6.0D));

    public StrangeMushroomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.MUSHROOM_GROW_BLOCK)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        boolean richFloor = belowState.is(BlockTags.MUSHROOM_GROW_BLOCK)
                || belowState.is(Blocks.MOSS_BLOCK)
                || belowState.is(Blocks.ROOTED_DIRT)
                || belowState.is(Blocks.MUD);
        return richFloor || level.getRawBrightness(pos, 0) < 13 && this.mayPlaceOn(belowState, level, belowPos);
    }
}
