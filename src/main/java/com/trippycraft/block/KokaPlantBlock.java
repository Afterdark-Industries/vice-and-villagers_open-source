package com.trippycraft.block;

import com.mojang.serialization.MapCodec;
import com.trippycraft.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class KokaPlantBlock extends BushBlock implements BonemealableBlock {
    @SuppressWarnings("unchecked")
    public static final MapCodec<BushBlock> CODEC =
            (MapCodec<BushBlock>)(MapCodec<?>)simpleCodec(KokaPlantBlock::new);

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final int MAX_AGE = 3;

    private static final VoxelShape[] SHAPES = new VoxelShape[] {
            Shapes.or(
                    Block.box(6.0D, 0.0D, 6.0D, 10.0D, 5.0D, 10.0D),
                    Block.box(4.0D, 1.0D, 7.0D, 12.0D, 2.0D, 9.0D),
                    Block.box(7.0D, 1.0D, 4.0D, 9.0D, 2.0D, 12.0D)),
            Shapes.or(
                    Block.box(4.0D, 0.0D, 4.0D, 12.0D, 9.0D, 12.0D),
                    Block.box(2.0D, 2.0D, 6.0D, 14.0D, 5.0D, 10.0D),
                    Block.box(6.0D, 2.0D, 2.0D, 10.0D, 5.0D, 14.0D),
                    Block.box(5.0D, 6.0D, 5.0D, 11.0D, 9.0D, 11.0D)),
            Shapes.or(
                    Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D),
                    Block.box(1.0D, 2.0D, 5.0D, 15.0D, 6.0D, 11.0D),
                    Block.box(5.0D, 2.0D, 1.0D, 11.0D, 6.0D, 15.0D),
                    Block.box(2.0D, 7.0D, 5.0D, 14.0D, 10.0D, 11.0D),
                    Block.box(5.0D, 7.0D, 2.0D, 11.0D, 10.0D, 14.0D),
                    Block.box(4.0D, 10.0D, 4.0D, 12.0D, 13.0D, 12.0D)),
            Shapes.or(
                    Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D),
                    Block.box(0.0D, 2.0D, 4.0D, 16.0D, 6.0D, 12.0D),
                    Block.box(4.0D, 2.0D, 0.0D, 12.0D, 6.0D, 16.0D),
                    Block.box(2.0D, 6.0D, 3.0D, 15.0D, 10.0D, 13.0D),
                    Block.box(3.0D, 6.0D, 2.0D, 13.0D, 10.0D, 15.0D),
                    Block.box(2.0D, 10.0D, 4.0D, 14.0D, 13.0D, 12.0D),
                    Block.box(4.0D, 10.0D, 2.0D, 12.0D, 13.0D, 14.0D),
                    Block.box(5.0D, 13.0D, 5.0D, 11.0D, 16.0D, 11.0D))
    };

    public KokaPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ModItems.KOKA_SEEDS.get().getDefaultInstance();
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.FARMLAND)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MUD)
                || state.is(BlockTags.DIRT);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        return this.mayPlaceOn(belowState, level, belowPos)
                && hasGrowthLight(level, pos);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= MAX_AGE || !hasGrowthLight(level, pos)) {
            return;
        }

        int growthChance = level.getBlockState(pos.below()).is(Blocks.FARMLAND) ? 3 : 5;
        if (random.nextInt(growthChance) == 0) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int increase = Mth.nextInt(random, 1, 2);
        int nextAge = Math.min(MAX_AGE, state.getValue(AGE) + increase);
        level.setBlock(pos, state.setValue(AGE, nextAge), Block.UPDATE_CLIENTS);
    }

    public BlockState getMatureState() {
        return this.defaultBlockState().setValue(AGE, MAX_AGE);
    }

    private static boolean hasGrowthLight(LevelReader level, BlockPos pos) {
        return level.getRawBrightness(pos, 0) >= 7 || level.canSeeSky(pos);
    }
}
