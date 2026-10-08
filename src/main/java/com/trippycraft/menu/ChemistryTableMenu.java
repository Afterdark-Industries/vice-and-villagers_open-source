package com.trippycraft.menu;

import com.trippycraft.block.entity.ChemistryTableBlockEntity;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import com.trippycraft.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ChemistryTableMenu extends AbstractContainerMenu {
    private final ChemistryTableBlockEntity blockEntity;
    private final ContainerData data;

    public ChemistryTableMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, getBlockEntity(inventory, buffer), new SimpleContainerData(5));
    }

    public ChemistryTableMenu(int containerId, Inventory inventory, ChemistryTableBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.CHEMISTRY_TABLE.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new Slot(blockEntity, ChemistryTableBlockEntity.INPUT_SLOT, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidIngredientInput(stack);
            }
        });
        addSlot(new Slot(blockEntity, ChemistryTableBlockEntity.FUEL_SLOT, 26, 59) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidFuelInput(stack);
            }
        });
        addSlot(new Slot(blockEntity, ChemistryTableBlockEntity.OUTPUT_SLOT, 134, 47) {
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
        return progress * 46 / progressTotal;
    }

    public int getScaledBurn() {
        int burn = data.get(2);
        int burnTotal = data.get(3);
        if (burn <= 0 || burnTotal <= 0) {
            return 0;
        }
        return Math.max(1, burn * 14 / burnTotal);
    }

    public boolean hasAromaticInput() {
        return blockEntity.getItem(ChemistryTableBlockEntity.INPUT_SLOT).is(ModItems.AROMATIC_EXTRACT.get());
    }

    public int getSelectedAromaticRecipeId() {
        return data.get(4);
    }

    public Component getSelectedAromaticRecipeLabel() {
        return getAromaticRecipeLabel(getSelectedAromaticRecipeId());
    }

    public Component getAromaticRecipeLabel(int recipeId) {
        return Component.translatable(recipeId == ChemistryTableBlockEntity.AROMATIC_PHENETHYLAMINE_BASE
                ? ModItems.PHENETHYLAMINE_BASE.get().getDescriptionId() : ModItems.REFINED_AROMATIC_OIL.get().getDescriptionId());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return stillValid(player) && blockEntity.selectAromaticRecipeByButton(buttonId);
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

        if (index == ChemistryTableBlockEntity.OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index == ChemistryTableBlockEntity.INPUT_SLOT || index == ChemistryTableBlockEntity.FUEL_SLOT) {
            if (!moveItemStackTo(stack, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidIngredientInput(stack)) {
            if (!moveItemStackTo(stack, ChemistryTableBlockEntity.INPUT_SLOT, ChemistryTableBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidFuelInput(stack)) {
            if (!moveItemStackTo(stack, ChemistryTableBlockEntity.FUEL_SLOT, ChemistryTableBlockEntity.FUEL_SLOT + 1, false)) {
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
        return stillValid(ContainerLevelAccess.create(player.level(), blockEntity.getBlockPos()), player, ModBlocks.CHEMISTRY_TABLE.get());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 120 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 8 + slot * 18, 178));
        }
    }

    private static ChemistryTableBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof ChemistryTableBlockEntity chemistryTable) {
            return chemistryTable;
        }
        throw new IllegalStateException("Missing chemistry table block entity at " + pos);
    }
}
