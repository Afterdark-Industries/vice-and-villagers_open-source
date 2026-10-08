package com.trippycraft.block.entity;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.menu.ChemistryTableMenu;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;

public final class ChemistryTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int AROMATIC_REFINED_OIL = 0;
    public static final int AROMATIC_PHENETHYLAMINE_BASE = 1;
    private static final int SLOT_COUNT = 3;
    private static final int DISTILL_TIME = 200;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> distillProgress;
                case 1 -> distillProgressTotal;
                case 2 -> burnTime;
                case 3 -> burnTimeTotal;
                case 4 -> aromaticRecipe;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> distillProgress = value;
                case 1 -> distillProgressTotal = value;
                case 2 -> burnTime = value;
                case 3 -> burnTimeTotal = value;
                case 4 -> {
                    if (value == AROMATIC_REFINED_OIL || value == AROMATIC_PHENETHYLAMINE_BASE) {
                        aromaticRecipe = value;
                    }
                }
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    private int distillProgress;
    private int distillProgressTotal = DISTILL_TIME;
    private int burnTime;
    private int burnTimeTotal;
    private int aromaticRecipe = AROMATIC_REFINED_OIL;

    public ChemistryTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHEMISTRY_TABLE.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChemistryTableBlockEntity chemistryTable) {
        boolean changed = false;
        boolean wasBurning = chemistryTable.isBurning();

        if (chemistryTable.burnTime > 0) {
            chemistryTable.burnTime--;
            changed = true;
        }

        if (chemistryTable.shouldConsumeFuel()
                && chemistryTable.burnTime <= 0
                && chemistryTable.hasDistillIngredients()
                && chemistryTable.canOutput(chemistryTable.getResultFor(chemistryTable.items.get(INPUT_SLOT)))) {
            changed |= chemistryTable.consumeFuel();
        }

        if (chemistryTable.canDistill()) {
            chemistryTable.distillProgress++;
            changed = true;
            if (chemistryTable.distillProgress >= chemistryTable.distillProgressTotal) {
                chemistryTable.finishDistilling();
                changed = true;
            }
        } else if (chemistryTable.distillProgress != 0) {
            chemistryTable.distillProgress = 0;
            changed = true;
        }

        boolean isBurning = chemistryTable.isBurning();
        if (state.hasProperty(BlockStateProperties.LIT) && wasBurning != isBurning) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, isBurning), Block.UPDATE_CLIENTS);
            state = level.getBlockState(pos);
            changed = true;
        }

        if (changed) {
            setChanged(level, pos, state);
        }
    }

    public boolean isValidIngredientInput(ItemStack stack) {
        return stack.is(ModItems.MORPHINE_BASE.get())
                || stack.is(ModItems.ETHANOL.get())
                || stack.is(ModItems.AROMATIC_EXTRACT.get())
                || stack.is(ModItems.MEDICINAL_REAGENT.get())
                || stack.is(ModItems.ACACIA_BARK.get())
                || stack.is(ModItems.MET_INTERMEDIATE.get())
                || stack.is(ModItems.DIMETHOXY_INTERMEDIATE.get())
                || stack.is(ModItems.TWO_CE_PRECURSOR.get())
                || stack.is(ModItems.ALLYL_LYSERGIC_INTERMEDIATE.get());
    }

    public int aromaticRecipeId() {
        return aromaticRecipe;
    }

    public boolean selectAromaticRecipeByButton(int buttonId) {
        if ((buttonId != AROMATIC_REFINED_OIL && buttonId != AROMATIC_PHENETHYLAMINE_BASE)
                || !items.get(INPUT_SLOT).is(ModItems.AROMATIC_EXTRACT.get())) {
            return false;
        }
        if (aromaticRecipe != buttonId) {
            aromaticRecipe = buttonId;
            distillProgress = 0;
            setChanged();
        }
        return true;
    }

    public boolean isValidFuelInput(ItemStack stack) {
        return level != null && !stack.isEmpty() && stack.getBurnTime(null) > 0;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    private boolean canDistill() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty() || !isValidIngredientInput(input)) {
            return false;
        }

        ItemStack result = getResultFor(input);
        return !result.isEmpty() && canOutput(result) && burnTime > 0;
    }

    private boolean hasDistillIngredients() {
        return getCurrentRecipe() != null;
    }

    private boolean shouldConsumeFuel() {
        return isValidIngredientInput(items.get(INPUT_SLOT));
    }

    private ItemStack getResultFor(ItemStack input) {
        DistillRecipe recipe = getCurrentRecipe();
        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        return switch (recipe) {
            case HEROIN_BASE -> new ItemStack(ModItems.HEROIN_BASE.get());
            case DISTILLED_SOLVENT -> new ItemStack(ModItems.DISTILLED_SOLVENT.get());
            case KETAMINE_PRECURSOR -> new ItemStack(ModItems.KETAMINE_PRECURSOR.get());
            case REFINED_AROMATIC_OIL -> new ItemStack(ModItems.REFINED_AROMATIC_OIL.get());
            case AMINOETHYL_REAGENT -> new ItemStack(ModItems.AMINOETHYL_REAGENT.get());
            case HO_MET_FREEBASE -> new ItemStack(ModItems.HO_MET_FREEBASE.get());
            case AL_LAD_CONCENTRATE -> new ItemStack(ModItems.AL_LAD_CONCENTRATE.get());
            case PHENETHYLAMINE_BASE -> new ItemStack(ModItems.PHENETHYLAMINE_BASE.get());
            case TWO_CB_FREEBASE -> new ItemStack(ModItems.TWO_CB_FREEBASE.get());
            case TWO_CE_FREEBASE -> new ItemStack(ModItems.TWO_CE_FREEBASE.get());
        };
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
        int burnDuration = getFuelBurnTime(fuel);
        if (burnDuration <= 0) {
            return false;
        }

        burnTime = burnDuration;
        burnTimeTotal = burnDuration;

        ItemStack remainder = fuel.getCraftingRemainingItem();
        if (!remainder.isEmpty()) {
            items.set(FUEL_SLOT, remainder.copy());
        } else {
            fuel.shrink(1);
            if (fuel.isEmpty()) {
                items.set(FUEL_SLOT, ItemStack.EMPTY);
            }
        }
        return true;
    }

    private int getFuelBurnTime(ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return 0;
        }
        return stack.getBurnTime(null);
    }

    private void finishDistilling() {
        DistillRecipe recipe = getCurrentRecipe();
        if (recipe == null) {
            distillProgress = 0;
            return;
        }

        ItemStack result = getResultFor(items.get(INPUT_SLOT));
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        if (recipe == DistillRecipe.DISTILLED_SOLVENT || recipe == DistillRecipe.REFINED_AROMATIC_OIL || recipe == DistillRecipe.HO_MET_FREEBASE
                || recipe == DistillRecipe.AL_LAD_CONCENTRATE || recipe == DistillRecipe.PHENETHYLAMINE_BASE
                || recipe == DistillRecipe.TWO_CB_FREEBASE || recipe == DistillRecipe.TWO_CE_FREEBASE) {
            // These bottled inputs become the bottled result rather than returning an extra bottle.
            consumeInputIngredientWithoutRemainder();
        } else {
            consumeInputIngredient();
        }

        distillProgress = 0;
        setChanged();
    }

    @Nullable
    private DistillRecipe getCurrentRecipe() {
        ItemStack input = items.get(INPUT_SLOT);
        if (matchesRecipe(input, ModItems.MORPHINE_BASE.get())) {
            return DistillRecipe.HEROIN_BASE;
        }
        if (matchesRecipe(input, ModItems.ETHANOL.get())) {
            return DistillRecipe.DISTILLED_SOLVENT;
        }
        if (matchesRecipe(input, ModItems.MEDICINAL_REAGENT.get())) {
            return DistillRecipe.KETAMINE_PRECURSOR;
        }
        if (matchesRecipe(input, ModItems.AROMATIC_EXTRACT.get())) {
            return aromaticRecipe == AROMATIC_PHENETHYLAMINE_BASE
                    ? DistillRecipe.PHENETHYLAMINE_BASE : DistillRecipe.REFINED_AROMATIC_OIL;
        }
        if (matchesRecipe(input, ModItems.ACACIA_BARK.get())) {
            return DistillRecipe.AMINOETHYL_REAGENT;
        }
        if (matchesRecipe(input, ModItems.MET_INTERMEDIATE.get())) {
            return DistillRecipe.HO_MET_FREEBASE;
        }
        if (matchesRecipe(input, ModItems.ALLYL_LYSERGIC_INTERMEDIATE.get())) {
            return DistillRecipe.AL_LAD_CONCENTRATE;
        }
        if (matchesRecipe(input, ModItems.DIMETHOXY_INTERMEDIATE.get())) {
            return DistillRecipe.TWO_CB_FREEBASE;
        }
        if (matchesRecipe(input, ModItems.TWO_CE_PRECURSOR.get())) {
            return DistillRecipe.TWO_CE_FREEBASE;
        }
        return null;
    }

    private boolean matchesRecipe(ItemStack input, Item ingredient) {
        return input.is(ingredient) && input.getCount() >= 1;
    }

    private void consumeInputIngredient() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }

        ItemStack remainder = input.getCraftingRemainingItem();
        input.shrink(1);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }

        if (remainder.isEmpty()) {
            return;
        }

        ItemStack updatedInput = items.get(INPUT_SLOT);
        if (updatedInput.isEmpty()) {
            items.set(INPUT_SLOT, remainder.copy());
            return;
        }

        if (ItemStack.isSameItemSameComponents(updatedInput, remainder)
                && updatedInput.getCount() + remainder.getCount() <= updatedInput.getMaxStackSize()) {
            updatedInput.grow(remainder.getCount());
            return;
        }

        if (level != null) {
            Block.popResource(level, worldPosition, remainder.copy());
        }
    }

    private void consumeInputIngredientWithoutRemainder() {
        ItemStack input = items.get(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        input.shrink(1);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private enum DistillRecipe {
        HEROIN_BASE,
        DISTILLED_SOLVENT,
        KETAMINE_PRECURSOR,
        REFINED_AROMATIC_OIL,
        AMINOETHYL_REAGENT,
        HO_MET_FREEBASE,
        AL_LAD_CONCENTRATE,
        PHENETHYLAMINE_BASE,
        TWO_CB_FREEBASE,
        TWO_CE_FREEBASE
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
            distillProgress = 0;
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
        return Component.translatable("container.trippycraft.chemistry_table");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ChemistryTableMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("DistillProgress", distillProgress);
        tag.putInt("DistillProgressTotal", distillProgressTotal);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
        tag.putInt("AromaticRecipe", aromaticRecipe);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        distillProgress = NbtCompat.getIntOr(tag, "DistillProgress", 0);
        distillProgressTotal = NbtCompat.getIntOr(tag, "DistillProgressTotal", DISTILL_TIME);
        burnTime = NbtCompat.getIntOr(tag, "BurnTime", 0);
        burnTimeTotal = NbtCompat.getIntOr(tag, "BurnTimeTotal", 0);
        aromaticRecipe = NbtCompat.getIntOr(tag, "AromaticRecipe", AROMATIC_REFINED_OIL) == AROMATIC_PHENETHYLAMINE_BASE
                ? AROMATIC_PHENETHYLAMINE_BASE : AROMATIC_REFINED_OIL;
    }
}
