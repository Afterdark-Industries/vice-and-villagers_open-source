package com.trippycraft.menu;

import com.trippycraft.block.entity.CannabisPressBlockEntity;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import com.trippycraft.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CannabisPressMenu extends AbstractContainerMenu {
    private final CannabisPressBlockEntity blockEntity;
    private final ContainerData data;

    public CannabisPressMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, getBlockEntity(inventory, buffer), new SimpleContainerData(4));
    }

    public CannabisPressMenu(int containerId, Inventory inventory, CannabisPressBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.CANNABIS_PRESS.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new Slot(blockEntity, CannabisPressBlockEntity.INPUT_SLOT, 34, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidIngredientInput(stack);
            }
        });
        addSlot(new Slot(blockEntity, CannabisPressBlockEntity.FUEL_SLOT, 34, 55) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidFuelInput(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return stack.is(ModItems.MDMA_CRYSTAL.get()) ? 1 : super.getMaxStackSize(stack);
            }
        });
        addSlot(new Slot(blockEntity, CannabisPressBlockEntity.OUTPUT_SLOT, 126, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
        addDataSlots(data);
    }

    public int getScaledProgress() {
        int progress = data.get(0);
        int progressTotal = data.get(1);
        if (progress <= 0 || progressTotal <= 0) {
            return 0;
        }
        return progress * 44 / progressTotal;
    }

    public int getScaledBurn() {
        int burn = data.get(2);
        int burnTotal = data.get(3);
        if (burn <= 0 || burnTotal <= 0) {
            return 0;
        }
        return Math.max(1, burn * 12 / burnTotal);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return empty;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == CannabisPressBlockEntity.OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index == CannabisPressBlockEntity.INPUT_SLOT || index == CannabisPressBlockEntity.FUEL_SLOT) {
            if (!moveItemStackTo(stack, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidIngredientInput(stack)) {
            if (!moveItemStackTo(stack, CannabisPressBlockEntity.INPUT_SLOT, CannabisPressBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidFuelInput(stack)) {
            if (!moveItemStackTo(stack, CannabisPressBlockEntity.FUEL_SLOT, CannabisPressBlockEntity.FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 30) {
            if (!moveItemStackTo(stack, 30, 39, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 3, 30, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockEntity.getBlockPos()), player, ModBlocks.CANNABIS_PRESS.get());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 8 + slot * 18, 142));
        }
    }

    private static CannabisPressBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof CannabisPressBlockEntity press) {
            return press;
        }
        throw new IllegalStateException("Missing cannabis press block entity at " + pos);
    }
}
