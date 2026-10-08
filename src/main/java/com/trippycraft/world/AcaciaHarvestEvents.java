package com.trippycraft.world;

import com.trippycraft.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class AcaciaHarvestEvents {
    private AcaciaHarvestEvents() {
    }

    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated()
                || event.getItemAbility() != ItemAbilities.AXE_STRIP
                || event.getState().getBlock() != Blocks.ACACIA_LOG
                || !(event.getLevel() instanceof Level level)
                || level.isClientSide()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || player.getAbilities().instabuild) {
            return;
        }

        BlockState strippedState = event.getState().getBlock()
                .getToolModifiedState(event.getState(), event.getContext(), event.getItemAbility(), true);
        if (strippedState == null) {
            return;
        }

        Block.popResource(level, event.getPos(), new ItemStack(ModItems.ACACIA_BARK.get(), 4));
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()
                || event.getState().getBlock() != Blocks.ACACIA_LEAVES
                || !(event.getLevel() instanceof Level level)
                || level.isClientSide()) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || player.getAbilities().instabuild) {
            return;
        }

        Block.popResource(level, event.getPos(), new ItemStack(ModItems.ACACIA_LEAF.get(), 2));
    }
}
