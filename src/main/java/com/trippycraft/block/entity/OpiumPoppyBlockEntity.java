package com.trippycraft.block.entity;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class OpiumPoppyBlockEntity extends BlockEntity {
    private long lastHarvestDay = Long.MIN_VALUE;

    public OpiumPoppyBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.OPIUM_POPPY.get(), pos, blockState);
    }

    public boolean canHarvestOnDay(long currentDay) {
        return currentDay > lastHarvestDay;
    }

    public void markHarvested(long currentDay) {
        lastHarvestDay = currentDay;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("LastHarvestDay", lastHarvestDay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastHarvestDay = NbtCompat.getLongOr(tag, "LastHarvestDay", Long.MIN_VALUE);
    }
}
