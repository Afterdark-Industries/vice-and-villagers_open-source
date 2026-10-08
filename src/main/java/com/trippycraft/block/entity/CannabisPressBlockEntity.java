package com.trippycraft.block.entity;

import com.trippycraft.compat.NbtCompat;
import com.trippycraft.item.MdmaCrystalItem;

import com.trippycraft.menu.CannabisPressMenu;
import com.trippycraft.registry.ModBlockEntities;
import com.trippycraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class CannabisPressBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int SLOT_COUNT = 3;
    private static final int PRESS_TIME = 200;
    private static final int COAL_BURN_TIME = 1600;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> pressProgress;
                case 1 -> pressProgressTotal;
                case 2 -> burnTime;
                case 3 -> burnTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> pressProgress = value;
                case 1 -> pressProgressTotal = value;
                case 2 -> burnTime = value;
                case 3 -> burnTimeTotal = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    private int pressProgress;
    private int pressProgressTotal = PRESS_TIME;
    private int burnTime;
    private int burnTimeTotal;

    public CannabisPressBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CANNABIS_PRESS.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CannabisPressBlockEntity press) {
        boolean changed = false;
        if (press.burnTime > 0) {
            press.burnTime--;
            changed = true;
        }

        if (press.shouldConsumeFuel() && press.burnTime <= 0 && press.hasPressIngredients() && press.canOutput(press.getResultFor(press.items.get(INPUT_SLOT)))) {
            changed |= press.consumeFuel();
        }

        if (press.canPress()) {
            press.pressProgress++;
            changed = true;
            if (press.pressProgress >= press.pressProgressTotal) {
                press.finishPressing();
                changed = true;
            }
        } else if (press.pressProgress != 0) {
            press.pressProgress = 0;
            changed = true;
        }

        if (changed) {
            setChanged(level, pos, state);
            return;
        }
    }

    public boolean isValidIngredientInput(ItemStack stack) {
        return stack.is(ModItems.CANNABIS_BUD.get())
                || stack.is(ModItems.THC_EXTRACT.get())
                || stack.is(Items.SUGAR);
    }

    public boolean isValidFuelInput(ItemStack stack) {
        return stack.is(Items.COAL)
                || stack.is(Items.CHARCOAL)
                || stack.is(ModItems.MDMA_CRYSTAL.get());
    }

    private boolean canPress() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty() || !isValidIngredientInput(input)) {
            return false;
        }

        ItemStack result = getResultFor(input);
        if (result.isEmpty()) {
            return false;
        }

        if (!canOutput(result)) {
            return false;
        }

        if (!requiresFuel(input)) {
            return true;
        }

        return burnTime > 0;
    }

    private boolean hasPressIngredients() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.is(ModItems.CANNABIS_BUD.get())) {
            return input.getCount() >= 2;
        }
        if (input.is(ModItems.THC_EXTRACT.get())) {
            return input.getCount() >= 10;
        }
        if (input.is(Items.SUGAR)) {
            return input.getCount() >= 1
                    && items.get(FUEL_SLOT).getCount() == 1
                    && MdmaCrystalItem.remainingMilligrams(items.get(FUEL_SLOT)) >= 500;
        }
        return false;
    }

    private boolean requiresFuel(ItemStack input) {
        return input.is(ModItems.CANNABIS_BUD.get());
    }

    private boolean shouldConsumeFuel() {
        return requiresFuel(items.get(INPUT_SLOT));
    }

    private ItemStack getResultFor(ItemStack input) {
        if (input.is(ModItems.CANNABIS_BUD.get()) && input.getCount() >= 2) {
            return new ItemStack(ModItems.THC_EXTRACT.get());
        }
        if (input.is(ModItems.THC_EXTRACT.get()) && input.getCount() >= 10) {
            return new ItemStack(ModItems.PACKED_THC_EXTRACT.get());
        }
        if (input.is(Items.SUGAR)
                && input.getCount() >= 1
                && items.get(FUEL_SLOT).getCount() == 1
                && MdmaCrystalItem.remainingMilligrams(items.get(FUEL_SLOT)) >= 500) {
            return new ItemStack(ModItems.MDMA_500MG.get());
        }
        return ItemStack.EMPTY;
    }

    private boolean canOutput(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }

        return output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), getMaxStackSize());
    }

    private boolean consumeFuel() {
        ItemStack fuel = items.get(FUEL_SLOT);
        if (!isValidFuelInput(fuel)) {
            return false;
        }

        burnTime = getFuelBurnTime(fuel);
        burnTimeTotal = burnTime;
        fuel.shrink(1);
        if (fuel.isEmpty()) {
            items.set(FUEL_SLOT, ItemStack.EMPTY);
        }
        return burnTime > 0;
    }

    private int getFuelBurnTime(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? COAL_BURN_TIME : 0;
    }

    private void finishPressing() {
        ItemStack input = items.get(INPUT_SLOT);
        ItemStack result = getResultFor(input);
        if (result.isEmpty()) {
            pressProgress = 0;
            return;
        }

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        if (input.is(ModItems.CANNABIS_BUD.get())) {
            input.shrink(2);
        } else if (input.is(Items.SUGAR)) {
            input.shrink(1);
            ItemStack crystal = items.get(FUEL_SLOT);
            MdmaCrystalItem.consumeMilligrams(crystal, 500);
            if (crystal.isEmpty()) {
                items.set(FUEL_SLOT, ItemStack.EMPTY);
            }
        } else {
            input.shrink(10);
        }

        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }

        pressProgress = 0;
        setChanged();
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        if (slot == INPUT_SLOT && !isValidIngredientInput(stack) && !stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == FUEL_SLOT && !isValidFuelInput(stack) && !stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == INPUT_SLOT || slot == FUEL_SLOT) {
            pressProgress = 0;
            if (slot == INPUT_SLOT && !requiresFuel(stack)) {
                burnTime = 0;
                burnTimeTotal = 0;
            }
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getCenter()) <= 64.0D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> isValidIngredientInput(stack);
            case FUEL_SLOT -> isValidFuelInput(stack);
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.trippycraft.cannabis_press");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CannabisPressMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("PressProgress", pressProgress);
        tag.putInt("PressProgressTotal", pressProgressTotal);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        pressProgress = NbtCompat.getIntOr(tag, "PressProgress", 0);
        pressProgressTotal = NbtCompat.getIntOr(tag, "PressProgressTotal", PRESS_TIME);
        burnTime = NbtCompat.getIntOr(tag, "BurnTime", 0);
        burnTimeTotal = NbtCompat.getIntOr(tag, "BurnTimeTotal", 0);
    }
}
