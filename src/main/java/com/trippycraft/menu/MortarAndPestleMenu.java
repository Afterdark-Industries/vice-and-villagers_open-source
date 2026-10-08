package com.trippycraft.menu;

import com.trippycraft.block.entity.MortarAndPestleBlockEntity;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MortarAndPestleMenu extends AbstractContainerMenu {
    private final MortarAndPestleBlockEntity blockEntity;
    private final ContainerData data;

    public MortarAndPestleMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, getBlockEntity(inventory, buffer), new SimpleContainerData(2));
    }

    public MortarAndPestleMenu(int containerId, Inventory inventory, MortarAndPestleBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MORTAR_AND_PESTLE.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new Slot(blockEntity, MortarAndPestleBlockEntity.INPUT_SLOT, 38, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidInput(stack);
            }
        });
        addSlot(new Slot(blockEntity, MortarAndPestleBlockEntity.OUTPUT_SLOT, 120, 34) {
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
        return progress * 42 / progressTotal;
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

        if (index == MortarAndPestleBlockEntity.OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index == MortarAndPestleBlockEntity.INPUT_SLOT) {
            if (!moveItemStackTo(stack, 2, 38, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidInput(stack)) {
            if (!moveItemStackTo(stack, MortarAndPestleBlockEntity.INPUT_SLOT, MortarAndPestleBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 29) {
            if (!moveItemStackTo(stack, 29, 38, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 2, 29, false)) {
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
        return stillValid(ContainerLevelAccess.create(player.level(), blockEntity.getBlockPos()), player, ModBlocks.MORTAR_AND_PESTLE.get());
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

    private static MortarAndPestleBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MortarAndPestleBlockEntity mortar) {
            return mortar;
        }
        throw new IllegalStateException("Missing mortar and pestle block entity at " + pos);
    }
}
