package com.trippycraft.block.entity;

import com.trippycraft.compat.NbtCompat;
import com.trippycraft.item.HoMetCrystalItem;
import com.trippycraft.item.TwoCbCrystalItem;
import com.trippycraft.item.TwoCeCrystalItem;

import com.trippycraft.menu.MortarAndPestleMenu;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class MortarAndPestleBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int SLOT_COUNT = 2;
    private static final int GRIND_TIME = 40;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> grindProgress;
                case 1 -> grindProgressTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> grindProgress = value;
                case 1 -> grindProgressTotal = value;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    private int grindProgress;
    private int grindProgressTotal = GRIND_TIME;

    public MortarAndPestleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MORTAR_AND_PESTLE.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MortarAndPestleBlockEntity mortar) {
        if (mortar.canGrind()) {
            mortar.grindProgress++;
            if (mortar.grindProgress >= mortar.grindProgressTotal) {
                mortar.finishGrinding();
            }
            setChanged(level, pos, state);
            return;
        }

        if (mortar.grindProgress != 0) {
            mortar.grindProgress = 0;
            setChanged(level, pos, state);
        }
    }

    public ItemStack createDropStack() {
        return new ItemStack(ModItems.MORTAR_AND_PESTLE.get());
    }

    public boolean isValidInput(ItemStack stack) {
        return stack.is(ModItems.DRIED_STRANGE_MUSHROOM.get())
                || stack.is(ModItems.DRIED_KOKA_LEAVES.get())
                || stack.is(ModItems.HO_MET_CRYSTAL.get())
                || stack.is(ModItems.TWO_CB_CRYSTAL.get())
                || stack.is(ModItems.TWO_CE_CRYSTAL.get());
    }

    private boolean canGrind() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty() || !isValidInput(input)) {
            return false;
        }

        ItemStack result = getResultFor(input);
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

    private ItemStack getResultFor(ItemStack input) {
        if (input.is(ModItems.DRIED_STRANGE_MUSHROOM.get())) {
            return new ItemStack(ModItems.PSILOCYBIN_POWDER.get());
        }
        if (input.is(ModItems.DRIED_KOKA_LEAVES.get())) {
            return new ItemStack(ModItems.GROUND_KOKA_LEAVES.get());
        }
        if (input.is(ModItems.HO_MET_CRYSTAL.get())) {
            int remaining = HoMetCrystalItem.remainingMilligrams(input);
            // Each powder item has a fixed mass. Never discard a fractional powder remainder.
            if (remaining >= HoMetCrystalItem.POWDER_MILLIGRAMS
                    && remaining % HoMetCrystalItem.POWDER_MILLIGRAMS == 0) {
                return new ItemStack(ModItems.HO_MET_POWDER.get(), remaining / HoMetCrystalItem.POWDER_MILLIGRAMS);
            }
        }
        if (input.is(ModItems.TWO_CB_CRYSTAL.get())) {
            int remaining = TwoCbCrystalItem.remainingMilligrams(input);
            if (remaining >= TwoCbCrystalItem.POWDER_MILLIGRAMS
                    && remaining % TwoCbCrystalItem.POWDER_MILLIGRAMS == 0) {
                return new ItemStack(ModItems.TWO_CB_POWDER.get(), remaining / TwoCbCrystalItem.POWDER_MILLIGRAMS);
            }
        }
        if (input.is(ModItems.TWO_CE_CRYSTAL.get())) {
            int remaining = TwoCeCrystalItem.remainingMilligrams(input);
            if (remaining >= TwoCeCrystalItem.POWDER_MILLIGRAMS
                    && remaining % TwoCeCrystalItem.POWDER_MILLIGRAMS == 0) {
                return new ItemStack(ModItems.TWO_CE_POWDER.get(), remaining / TwoCeCrystalItem.POWDER_MILLIGRAMS);
            }
        }
        return ItemStack.EMPTY;
    }

    private void finishGrinding() {
        if (!canGrind()) {
            grindProgress = 0;
            return;
        }

        ItemStack input = items.get(INPUT_SLOT);
        ItemStack output = items.get(OUTPUT_SLOT);
        ItemStack result = getResultFor(input);

        input.shrink(1);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }

        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        grindProgress = 0;
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
        if (slot == INPUT_SLOT && !isValidInput(stack) && !stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == INPUT_SLOT) {
            grindProgress = 0;
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
        return slot == INPUT_SLOT && isValidInput(stack);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.trippycraft.mortar_and_pestle");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MortarAndPestleMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("GrindProgress", grindProgress);
        tag.putInt("GrindProgressTotal", grindProgressTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        grindProgress = NbtCompat.getIntOr(tag, "GrindProgress", 0);
        grindProgressTotal = NbtCompat.getIntOr(tag, "GrindProgressTotal", GRIND_TIME);
    }
}
