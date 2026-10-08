package com.trippycraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public final class KokaSeedsItem extends BlockItem {
    public KokaSeedsItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos soilPos = context.getClickedPos();
        BlockState soilState = level.getBlockState(soilPos);

        if (context.getClickedFace() != Direction.UP || !soilState.is(Blocks.FARMLAND)) {
            return InteractionResult.PASS;
        }

        BlockPos plantPos = soilPos.above();
        if (!level.getBlockState(plantPos).canBeReplaced()) {
            return InteractionResult.PASS;
        }

        BlockState plantState = this.getBlock().defaultBlockState();
        if (!plantState.canSurvive(level, plantPos)) {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (!level.isClientSide) {
            level.setBlock(plantPos, plantState, Block.UPDATE_ALL);
            SoundType sound = plantState.getSoundType(level, plantPos, player);
            level.playSound(
                    null,
                    plantPos,
                    sound.getPlaceSound(),
                    SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, plantPos, GameEvent.Context.of(player, plantState));

            if (player == null || !player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (player != null) {
                player.awardStat(Stats.ITEM_USED.get(this));
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
