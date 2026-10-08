package com.trippycraft.menu;

import com.trippycraft.block.entity.AdvancedChemistryTableBlockEntity;
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

public final class AdvancedChemistryTableMenu extends AbstractContainerMenu {
    private final AdvancedChemistryTableBlockEntity blockEntity;
    private final ContainerData data;

    public AdvancedChemistryTableMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, getBlockEntity(inventory, buffer), new SimpleContainerData(3));
    }

    public AdvancedChemistryTableMenu(int containerId, Inventory inventory, AdvancedChemistryTableBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.ADVANCED_CHEMISTRY_TABLE.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new Slot(blockEntity, AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, 22, 55) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidHeroinBaseInput(stack);
            }

            @Override
            public int getMaxStackSize(ItemStack stack) {
                return stack.is(ModItems.MDMA_CRYSTAL.get()) || stack.is(ModItems.AL_LAD_CONCENTRATE.get())
                        ? 1 : super.getMaxStackSize(stack);
            }
        });
        addSlot(new Slot(blockEntity, AdvancedChemistryTableBlockEntity.GLASS_SLOT, 22, 79) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidGlassInput(stack);
            }
        });
        addSlot(new Slot(blockEntity, AdvancedChemistryTableBlockEntity.OUTPUT_SLOT, 138, 67) {
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
        return progress * 48 / progressTotal;
    }

    public int getSelectedProductId() {
        return data.get(2);
    }

    public AdvancedChemistryTableBlockEntity.RefinedProduct getSelectedProduct() {
        return AdvancedChemistryTableBlockEntity.RefinedProduct.bySavedId(getSelectedProductId());
    }

    public String getPreviewOutputSummary() {
        return blockEntity.previewOutputSummary(getSelectedProduct());
    }

    public String getPreviewRequirementSummary() {
        return blockEntity.previewRequirementSummary(getSelectedProduct());
    }

    public String getPreviewOutputSummary(int buttonId) {
        return blockEntity.previewOutputSummary(AdvancedChemistryTableBlockEntity.RefinedProduct.bySavedId(buttonId));
    }

    public String getPreviewRequirementSummary(int buttonId) {
        return blockEntity.previewRequirementSummary(AdvancedChemistryTableBlockEntity.RefinedProduct.bySavedId(buttonId));
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        return blockEntity.selectProductByButton(buttonId);
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

        if (index == AdvancedChemistryTableBlockEntity.OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index == AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT || index == AdvancedChemistryTableBlockEntity.GLASS_SLOT) {
            if (!moveItemStackTo(stack, 3, 39, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidHeroinBaseInput(stack)) {
            if (!moveItemStackTo(stack, AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT, AdvancedChemistryTableBlockEntity.HEROIN_BASE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidGlassInput(stack)) {
            if (!moveItemStackTo(stack, AdvancedChemistryTableBlockEntity.GLASS_SLOT, AdvancedChemistryTableBlockEntity.GLASS_SLOT + 1, false)) {
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
        return stillValid(ContainerLevelAccess.create(player.level(), blockEntity.getBlockPos()), player, ModBlocks.ADVANCED_CHEMISTRY_TABLE.get());
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

    private static AdvancedChemistryTableBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof AdvancedChemistryTableBlockEntity advancedTable) {
            return advancedTable;
        }
        throw new IllegalStateException("Missing advanced chemistry table block entity at " + pos);
    }
}
