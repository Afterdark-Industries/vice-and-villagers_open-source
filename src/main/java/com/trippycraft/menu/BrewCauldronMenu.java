package com.trippycraft.menu;

import com.trippycraft.block.entity.BrewCauldronBlockEntity;
import com.trippycraft.registry.ModBlocks;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.trippycraft.registry.ModItems;

public final class BrewCauldronMenu extends AbstractContainerMenu {
    private final BrewCauldronBlockEntity blockEntity;
    private final ContainerData data;

    public BrewCauldronMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, getBlockEntity(inventory, buffer), new SimpleContainerData(4));
    }

    public BrewCauldronMenu(int containerId, Inventory inventory, BrewCauldronBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.BREW_CAULDRON.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new Slot(blockEntity, BrewCauldronBlockEntity.BARK_SLOT, 26, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidBarkInput(stack);
            }
        });
        addSlot(new Slot(blockEntity, BrewCauldronBlockEntity.LEAF_SLOT, 26, 71) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidLeafInput(stack);
            }
        });
        addSlot(new Slot(blockEntity, BrewCauldronBlockEntity.WATER_SLOT, 26, 104) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidWaterInput(stack);
            }
        });
        addSlot(new Slot(blockEntity, BrewCauldronBlockEntity.FUEL_SLOT, 160, 124) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.isValidFuelInput(stack);
            }
        });
        addSlot(new Slot(blockEntity, BrewCauldronBlockEntity.OUTPUT_SLOT, 291, 67) {
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
        return progress * 36 / progressTotal;
    }

    public int getScaledBurn() {
        int burn = data.get(2);
        int burnTotal = data.get(3);
        if (burn <= 0 || burnTotal <= 0) {
            return 0;
        }
        return Math.max(1, burn * 18 / burnTotal);
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

        if (index == BrewCauldronBlockEntity.OUTPUT_SLOT) {
            if (!moveItemStackTo(stack, 5, 41, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, copy);
        } else if (index >= BrewCauldronBlockEntity.BARK_SLOT && index <= BrewCauldronBlockEntity.FUEL_SLOT) {
            if (!moveItemStackTo(stack, 5, 41, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.LYSERGIC_EXTRACT.get())
                && blockEntity.getItem(BrewCauldronBlockEntity.BARK_SLOT).is(ModItems.AMINOETHYL_REAGENT.get())
                && blockEntity.getItem(BrewCauldronBlockEntity.WATER_SLOT).isEmpty()) {
            ItemStack reagent = blockEntity.removeItemNoUpdate(BrewCauldronBlockEntity.BARK_SLOT);
            blockEntity.setItem(BrewCauldronBlockEntity.WATER_SLOT, reagent);
            if (!moveItemStackTo(stack, BrewCauldronBlockEntity.BARK_SLOT, BrewCauldronBlockEntity.BARK_SLOT + 1, false)) {
                blockEntity.setItem(BrewCauldronBlockEntity.WATER_SLOT, ItemStack.EMPTY);
                blockEntity.setItem(BrewCauldronBlockEntity.BARK_SLOT, reagent);
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.AMINOETHYL_REAGENT.get())) {
            boolean preferLiquid = blockEntity.getItem(BrewCauldronBlockEntity.BARK_SLOT).is(ModItems.LYSERGIC_EXTRACT.get())
                    || blockEntity.getItem(BrewCauldronBlockEntity.BARK_SLOT).is(ModItems.DIMETHOXY_INTERMEDIATE.get());
            int preferredSlot = preferLiquid ? BrewCauldronBlockEntity.WATER_SLOT : BrewCauldronBlockEntity.BARK_SLOT;
            int fallbackSlot = preferLiquid ? BrewCauldronBlockEntity.BARK_SLOT : BrewCauldronBlockEntity.WATER_SLOT;
            if (!moveItemStackTo(stack, preferredSlot, preferredSlot + 1, false)
                    && !moveItemStackTo(stack, fallbackSlot, fallbackSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.DIMETHOXY_INTERMEDIATE.get())) {
            int intermediateSlot = blockEntity.getItem(BrewCauldronBlockEntity.BARK_SLOT).is(ModItems.AMINOETHYL_REAGENT.get())
                    ? BrewCauldronBlockEntity.WATER_SLOT : BrewCauldronBlockEntity.BARK_SLOT;
            if (!moveItemStackTo(stack, intermediateSlot, intermediateSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.PHENETHYLAMINE_BASE.get())) {
            int baseSlot = blockEntity.getItem(BrewCauldronBlockEntity.BARK_SLOT).is(ModItems.DISTILLED_SOLVENT.get())
                    ? BrewCauldronBlockEntity.WATER_SLOT : BrewCauldronBlockEntity.BARK_SLOT;
            if (!moveItemStackTo(stack, baseSlot, baseSlot + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.ETHANOL.get())) {
            if (!moveItemStackTo(stack, BrewCauldronBlockEntity.WATER_SLOT, BrewCauldronBlockEntity.WATER_SLOT + 1, false)
                    && !moveItemStackTo(stack, BrewCauldronBlockEntity.BARK_SLOT, BrewCauldronBlockEntity.BARK_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ModItems.DISTILLED_SOLVENT.get())) {
            if (blockEntity.getItem(BrewCauldronBlockEntity.BARK_SLOT).is(ModItems.PHENETHYLAMINE_BASE.get())
                    || blockEntity.getItem(BrewCauldronBlockEntity.WATER_SLOT).is(ModItems.PHENETHYLAMINE_BASE.get())) {
                int solventSlot = blockEntity.getItem(BrewCauldronBlockEntity.BARK_SLOT).is(ModItems.PHENETHYLAMINE_BASE.get())
                        ? BrewCauldronBlockEntity.WATER_SLOT : BrewCauldronBlockEntity.BARK_SLOT;
                if (!moveItemStackTo(stack, solventSlot, solventSlot + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean preferLeaf = blockEntity.getItem(BrewCauldronBlockEntity.BARK_SLOT).is(ModItems.KETAMINE_PRECURSOR.get())
                        || blockEntity.getItem(BrewCauldronBlockEntity.WATER_SLOT).is(Items.GLASS_BOTTLE);
                if (preferLeaf) {
                    if (!moveItemStackTo(stack, BrewCauldronBlockEntity.LEAF_SLOT, BrewCauldronBlockEntity.LEAF_SLOT + 1, false)
                            && !moveItemStackTo(stack, BrewCauldronBlockEntity.WATER_SLOT, BrewCauldronBlockEntity.WATER_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, BrewCauldronBlockEntity.WATER_SLOT, BrewCauldronBlockEntity.WATER_SLOT + 1, false)
                        && !moveItemStackTo(stack, BrewCauldronBlockEntity.LEAF_SLOT, BrewCauldronBlockEntity.LEAF_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (blockEntity.isValidBarkInput(stack)) {
            if (!moveItemStackTo(stack, BrewCauldronBlockEntity.BARK_SLOT, BrewCauldronBlockEntity.BARK_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidLeafInput(stack)) {
            if (!moveItemStackTo(stack, BrewCauldronBlockEntity.LEAF_SLOT, BrewCauldronBlockEntity.LEAF_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidWaterInput(stack)) {
            if (!moveItemStackTo(stack, BrewCauldronBlockEntity.WATER_SLOT, BrewCauldronBlockEntity.WATER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.isValidFuelInput(stack)) {
            if (!moveItemStackTo(stack, BrewCauldronBlockEntity.FUEL_SLOT, BrewCauldronBlockEntity.FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 32) {
            if (!moveItemStackTo(stack, 32, 41, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 5, 32, false)) {
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
        return stillValid(ContainerLevelAccess.create(player.level(), blockEntity.getBlockPos()), player, ModBlocks.BREW_CAULDRON.get());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 26 + column * 18, 170 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 26 + slot * 18, 228));
        }
    }

    private static BrewCauldronBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof BrewCauldronBlockEntity brewCauldron) {
            return brewCauldron;
        }
        throw new IllegalStateException("Missing brew cauldron block entity at " + pos);
    }
}
