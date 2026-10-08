package com.trippycraft.block;

import com.mojang.serialization.MapCodec;
import com.trippycraft.block.entity.OpiumPoppyBlockEntity;
import com.trippycraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nullable;

public final class OpiumPoppyBlock extends BushBlock implements BonemealableBlock, EntityBlock {
    @SuppressWarnings("unchecked")
    public static final MapCodec<BushBlock> CODEC =
            (MapCodec<BushBlock>)(MapCodec<?>)simpleCodec(OpiumPoppyBlock::new);

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final BooleanProperty TAPPED = BooleanProperty.create("tapped");
    public static final int MAX_AGE = 3;

    private static final VoxelShape[] SHAPES = new VoxelShape[] {
            Shapes.or(
                    Block.box(4.0D, 0.0D, 4.0D, 12.0D, 6.0D, 12.0D),
                    Block.box(3.0D, 1.0D, 5.0D, 13.0D, 2.0D, 11.0D),
                    Block.box(5.0D, 1.0D, 3.0D, 11.0D, 2.0D, 13.0D)),
            Shapes.or(
                    Block.box(3.0D, 0.0D, 3.0D, 13.0D, 10.0D, 13.0D),
                    Block.box(2.0D, 2.0D, 5.0D, 14.0D, 3.0D, 11.0D),
                    Block.box(5.0D, 2.0D, 2.0D, 11.0D, 3.0D, 14.0D),
                    Block.box(3.0D, 6.0D, 5.0D, 13.0D, 7.0D, 11.0D),
                    Block.box(5.0D, 6.0D, 3.0D, 11.0D, 7.0D, 13.0D)),
            Shapes.or(
                    Block.box(3.0D, 0.0D, 4.0D, 11.0D, 15.0D, 12.0D),
                    Block.box(2.0D, 2.0D, 5.0D, 12.0D, 3.0D, 11.0D),
                    Block.box(4.0D, 6.0D, 4.0D, 10.0D, 7.0D, 12.0D),
                    Block.box(3.0D, 9.0D, 5.0D, 11.0D, 10.0D, 11.0D),
                    Block.box(4.0D, 12.0D, 5.0D, 10.0D, 15.0D, 11.0D)),
            Shapes.or(
                    Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D),
                    Block.box(1.0D, 2.0D, 4.0D, 15.0D, 3.0D, 12.0D),
                    Block.box(4.0D, 2.0D, 1.0D, 12.0D, 3.0D, 15.0D),
                    Block.box(2.0D, 7.0D, 4.0D, 14.0D, 8.0D, 12.0D),
                    Block.box(4.0D, 7.0D, 2.0D, 12.0D, 8.0D, 14.0D),
                    Block.box(3.0D, 11.0D, 4.0D, 13.0D, 12.0D, 12.0D),
                    Block.box(4.0D, 13.0D, 3.0D, 12.0D, 16.0D, 13.0D))
    };

    public OpiumPoppyBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(TAPPED, false));
    }

    @Override
    public MapCodec<BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, TAPPED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ModItems.OPIUM_POPPY_SEEDS.get().getDefaultInstance();
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
        return this.mayPlaceOn(belowState, level, belowPos) && hasGrowthLight(level, pos);
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
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.is(this) && state.getValue(TAPPED)) {
            long currentDay = Math.floorDiv(level.getDayTime(), 24_000L);
            if (!(level.getBlockEntity(pos) instanceof OpiumPoppyBlockEntity poppy)
                    || poppy.canHarvestOnDay(currentDay)) {
                level.setBlock(pos, state.setValue(TAPPED, false), Block.UPDATE_CLIENTS);
            } else {
                scheduleReset(level, pos);
            }
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return this.tryHarvest(state, level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        InteractionResult result = this.tryHarvest(state, level, pos, player);
        return result.consumesAction()
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OpiumPoppyBlockEntity(pos, state);
    }

    public BlockState getMatureState() {
        return this.defaultBlockState().setValue(AGE, MAX_AGE);
    }

    /** Event-level fallback so tapping survives item-first interaction routing changes. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof OpiumPoppyBlock poppy)) {
            return;
        }
        InteractionResult result = poppy.tryHarvest(state, event.getLevel(), event.getPos(), event.getEntity());
        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private InteractionResult tryHarvest(BlockState state, Level level, BlockPos pos, Player player) {
        if (state.getValue(AGE) < MAX_AGE) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        OpiumPoppyBlockEntity poppy;
        if (level.getBlockEntity(pos) instanceof OpiumPoppyBlockEntity existingPoppy) {
            poppy = existingPoppy;
        } else {
            poppy = new OpiumPoppyBlockEntity(pos, state);
            level.setBlockEntity(poppy);
        }

        long currentDay = Math.floorDiv(level.getDayTime(), 24_000L);
        // The block-state flag is position-local. The block entity is used only to notice a new day.
        if (state.getValue(TAPPED) && !poppy.canHarvestOnDay(currentDay)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.trippycraft.opium_poppy_already_tapped"), true);
            return InteractionResult.CONSUME;
        }

        level.setBlock(pos, state.setValue(TAPPED, true), Block.UPDATE_CLIENTS);
        poppy.markHarvested(currentDay);
        scheduleReset(level, pos);

        ItemStack latexStack = ModItems.OPIUM_LATEX.get().getDefaultInstance();
        if (!player.addItem(latexStack.copy())) {
            popResource(level, pos, latexStack);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.trippycraft.opium_poppy_harvested"), true);

        level.playSound(
                null,
                pos,
                SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.BLOCKS,
                0.85F,
                0.9F + level.random.nextFloat() * 0.2F);
        return InteractionResult.CONSUME;
    }

    private void scheduleReset(Level level, BlockPos pos) {
        long timeWithinDay = Math.floorMod(level.getDayTime(), 24_000L);
        int ticksUntilNextDawn = (int) Math.max(1L, 24_000L - timeWithinDay);
        level.scheduleTick(pos, this, ticksUntilNextDawn);
    }

    private static boolean hasGrowthLight(LevelReader level, BlockPos pos) {
        return level.getRawBrightness(pos, 0) >= 7 || level.canSeeSky(pos);
    }
}
