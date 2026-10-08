package com.trippycraft.block.entity;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.menu.BrewCauldronMenu;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class BrewCauldronBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int BARK_SLOT = 0;
    public static final int LEAF_SLOT = 1;
    public static final int WATER_SLOT = 2;
    public static final int FUEL_SLOT = 3;
    public static final int OUTPUT_SLOT = 4;
    private static final int SLOT_COUNT = 5;
    private static final int BREW_TIME = 220;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> brewProgress;
                case 1 -> brewProgressTotal;
                case 2 -> burnTime;
                case 3 -> burnTimeTotal;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> brewProgress = value;
                case 1 -> brewProgressTotal = value;
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

    private int brewProgress;
    private int brewProgressTotal = BREW_TIME;
    private int burnTime;
    private int burnTimeTotal;

    public BrewCauldronBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BREW_CAULDRON.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BrewCauldronBlockEntity brewCauldron) {
        boolean changed = false;

        if (brewCauldron.burnTime > 0) {
            brewCauldron.burnTime--;
            changed = true;
        }

        if (brewCauldron.shouldConsumeFuel()
                && brewCauldron.burnTime <= 0
                && brewCauldron.hasBrewIngredients()
                && brewCauldron.canOutput(brewCauldron.getResultForCurrentInputs())) {
            changed |= brewCauldron.consumeFuel();
        }

        if (brewCauldron.canBrew()) {
            brewCauldron.brewProgress++;
            changed = true;
            if (brewCauldron.brewProgress >= brewCauldron.brewProgressTotal) {
                brewCauldron.finishBrewing();
                changed = true;
            }
        } else if (brewCauldron.brewProgress != 0) {
            brewCauldron.brewProgress = 0;
            changed = true;
        }

        if (changed) {
            setChanged(level, pos, state);
        }
    }

    public boolean isValidBarkInput(ItemStack stack) {
        return stack.is(ModItems.ACACIA_BARK.get())
                || stack.is(ModItems.ERGOT_CLUMP.get())
                || stack.is(ModItems.LYSERGIC_EXTRACT.get())
                || stack.is(ModItems.ETHANOL.get())
                || stack.is(ModItems.BUNDLED_PLANT_EXTRACT.get())
                || stack.is(ModItems.REFINED_AROMATIC_OIL.get())
                || stack.is(ModItems.KETAMINE_PRECURSOR.get())
                || stack.is(ModItems.AMINOETHYL_REAGENT.get())
                || stack.is(ModItems.PHENETHYLAMINE_BASE.get())
                || stack.is(ModItems.DIMETHOXY_INTERMEDIATE.get())
                || stack.is(ModItems.DISTILLED_SOLVENT.get());
    }

    public boolean isValidLeafInput(ItemStack stack) {
        return stack.is(ModItems.ACACIA_LEAF.get())
                || stack.is(ModItems.DISTILLED_SOLVENT.get())
                || stack.is(ModItems.ETHANOL.get());
    }

    public boolean isValidWaterInput(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET)
                || stack.is(ModItems.ETHANOL.get())
                || stack.is(ModItems.DISTILLED_SOLVENT.get())
                || stack.is(ModItems.MEDICINAL_REAGENT.get())
                || stack.is(ModItems.AMINOETHYL_REAGENT.get())
                || stack.is(ModItems.PHENETHYLAMINE_BASE.get())
                || stack.is(ModItems.DIMETHOXY_INTERMEDIATE.get())
                || stack.is(Items.GLASS_BOTTLE);
    }

    public boolean isValidFuelInput(ItemStack stack) {
        return level != null && !stack.isEmpty() && stack.getBurnTime(null) > 0;
    }

    private boolean canBrew() {
        return hasBrewIngredients()
                && !getResultForCurrentInputs().isEmpty()
                && canOutput(getResultForCurrentInputs())
                && burnTime > 0;
    }

    private boolean hasBrewIngredients() {
        return getCurrentRecipe() != null;
    }

    private boolean shouldConsumeFuel() {
        return hasBrewIngredients();
    }

    private ItemStack getResultForCurrentInputs() {
        BrewRecipe recipe = getCurrentRecipe();
        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        return switch (recipe) {
            case PLANT_EXTRACT -> new ItemStack(ModItems.PLANT_EXTRACT.get());
            case LYSERGIC_EXTRACT -> new ItemStack(ModItems.LYSERGIC_EXTRACT.get());
            case LSD_SOLUTION -> new ItemStack(ModItems.LSD_SOLUTION.get());
            case MEDICINAL_REAGENT -> new ItemStack(ModItems.MEDICINAL_REAGENT.get());
            case KETAMINE_BOTTLE -> new ItemStack(ModItems.KETAMINE.get());
            case AROMATIC_EXTRACT -> new ItemStack(ModItems.AROMATIC_EXTRACT.get());
            case CRUDE_EMPATHOGEN_BASE -> new ItemStack(ModItems.CRUDE_EMPATHOGEN_BASE.get());
            case MET_INTERMEDIATE -> new ItemStack(ModItems.MET_INTERMEDIATE.get());
            case ALLYL_LYSERGIC_INTERMEDIATE -> new ItemStack(ModItems.ALLYL_LYSERGIC_INTERMEDIATE.get());
            case DIMETHOXY_INTERMEDIATE -> new ItemStack(ModItems.DIMETHOXY_INTERMEDIATE.get());
            case TWO_CE_PRECURSOR -> new ItemStack(ModItems.TWO_CE_PRECURSOR.get());
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

    private void finishBrewing() {
        BrewRecipe recipe = getCurrentRecipe();
        if (recipe == null) {
            brewProgress = 0;
            return;
        }

        ItemStack result = getResultForCurrentInputs();
        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        if (recipe == BrewRecipe.DIMETHOXY_INTERMEDIATE || recipe == BrewRecipe.TWO_CE_PRECURSOR) {
            // The base/intermediate bottle becomes the product; return only the other reagent's extra bottle.
            Item retainedBottle = recipe == BrewRecipe.DIMETHOXY_INTERMEDIATE
                    ? ModItems.PHENETHYLAMINE_BASE.get() : ModItems.DIMETHOXY_INTERMEDIATE.get();
            int baseSlot = items.get(BARK_SLOT).is(retainedBottle) ? BARK_SLOT : WATER_SLOT;
            consumeIngredientWithoutRemainder(baseSlot);
            consumeIngredient(baseSlot == BARK_SLOT ? WATER_SLOT : BARK_SLOT);
            consumeIngredient(LEAF_SLOT);
        } else {
            if (recipe == BrewRecipe.CRUDE_EMPATHOGEN_BASE || recipe == BrewRecipe.MET_INTERMEDIATE) {
                // The primary bottle becomes the product bottle; the other bottled reagent is returned empty.
                consumeIngredientWithoutRemainder(BARK_SLOT);
            } else {
                consumeIngredient(BARK_SLOT);
            }
            consumeIngredient(LEAF_SLOT);
            consumeIngredient(WATER_SLOT);
        }
        brewProgress = 0;
        setChanged();
    }

    @Nullable
    private BrewRecipe getCurrentRecipe() {
        if (matchesRecipe(ModItems.ACACIA_BARK.get(), ModItems.ACACIA_LEAF.get(), Items.WATER_BUCKET)) {
            return BrewRecipe.PLANT_EXTRACT;
        }
        if (matchesRecipe(ModItems.ERGOT_CLUMP.get(), null, ModItems.ETHANOL.get())) {
            return BrewRecipe.LYSERGIC_EXTRACT;
        }
        if (matchesRecipe(ModItems.LYSERGIC_EXTRACT.get(), null, ModItems.DISTILLED_SOLVENT.get())) {
            return BrewRecipe.LSD_SOLUTION;
        }
        if (matchesRecipe(ModItems.ETHANOL.get(), ModItems.DISTILLED_SOLVENT.get(), Items.GLASS_BOTTLE)) {
            return BrewRecipe.MEDICINAL_REAGENT;
        }
        if (matchesRecipe(ModItems.KETAMINE_PRECURSOR.get(), ModItems.DISTILLED_SOLVENT.get(), Items.GLASS_BOTTLE)) {
            return BrewRecipe.KETAMINE_BOTTLE;
        }
        if (matchesRecipe(ModItems.BUNDLED_PLANT_EXTRACT.get(), ModItems.ETHANOL.get(), Items.GLASS_BOTTLE)) {
            return BrewRecipe.AROMATIC_EXTRACT;
        }
        if (matchesRecipe(ModItems.REFINED_AROMATIC_OIL.get(), null, ModItems.MEDICINAL_REAGENT.get())) {
            return BrewRecipe.CRUDE_EMPATHOGEN_BASE;
        }
        if (matchesRecipe(ModItems.AMINOETHYL_REAGENT.get(), null, ModItems.ETHANOL.get())) {
            return BrewRecipe.MET_INTERMEDIATE;
        }
        if (matchesRecipe(ModItems.LYSERGIC_EXTRACT.get(), null, ModItems.AMINOETHYL_REAGENT.get())) {
            return BrewRecipe.ALLYL_LYSERGIC_INTERMEDIATE;
        }
        if (matchesRecipe(ModItems.PHENETHYLAMINE_BASE.get(), null, ModItems.DISTILLED_SOLVENT.get())
                || matchesRecipe(ModItems.DISTILLED_SOLVENT.get(), null, ModItems.PHENETHYLAMINE_BASE.get())) {
            return BrewRecipe.DIMETHOXY_INTERMEDIATE;
        }
        if (matchesRecipe(ModItems.DIMETHOXY_INTERMEDIATE.get(), null, ModItems.AMINOETHYL_REAGENT.get())
                || matchesRecipe(ModItems.AMINOETHYL_REAGENT.get(), null, ModItems.DIMETHOXY_INTERMEDIATE.get())) {
            return BrewRecipe.TWO_CE_PRECURSOR;
        }
        return null;
    }

    private boolean matchesRecipe(Item barkItem, @Nullable Item leafItem, Item liquidItem) {
        ItemStack bark = items.get(BARK_SLOT);
        ItemStack leaf = items.get(LEAF_SLOT);
        ItemStack liquid = items.get(WATER_SLOT);
        return bark.is(barkItem)
                && bark.getCount() >= 1
                && matchesOptionalIngredient(leaf, leafItem)
                && liquid.is(liquidItem)
                && liquid.getCount() >= 1;
    }

    private boolean matchesOptionalIngredient(ItemStack stack, @Nullable Item item) {
        if (item == null) {
            return stack.isEmpty();
        }
        return stack.is(item) && stack.getCount() >= 1;
    }

    private void consumeIngredient(int slot) {
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remainder = stack.getCraftingRemainingItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }

        if (remainder.isEmpty()) {
            return;
        }

        ItemStack updatedStack = items.get(slot);
        if (updatedStack.isEmpty()) {
            items.set(slot, remainder.copy());
            return;
        }

        if (ItemStack.isSameItemSameComponents(updatedStack, remainder)
                && updatedStack.getCount() + remainder.getCount() <= updatedStack.getMaxStackSize()) {
            updatedStack.grow(remainder.getCount());
            return;
        }

        if (level != null) {
            Block.popResource(level, worldPosition, remainder.copy());
        }
    }

    private void consumeIngredientWithoutRemainder(int slot) {
        ItemStack stack = items.get(slot);
        if (stack.isEmpty()) {
            return;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
    }

    private enum BrewRecipe {
        PLANT_EXTRACT,
        LYSERGIC_EXTRACT,
        LSD_SOLUTION,
        MEDICINAL_REAGENT,
        KETAMINE_BOTTLE,
        AROMATIC_EXTRACT,
        CRUDE_EMPATHOGEN_BASE,
        MET_INTERMEDIATE,
        ALLYL_LYSERGIC_INTERMEDIATE,
        DIMETHOXY_INTERMEDIATE,
        TWO_CE_PRECURSOR
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
        if (slot == BARK_SLOT && !isValidBarkInput(stack) && !stack.isEmpty() && !stack.is(Items.GLASS_BOTTLE)) {
            // A reversed bottled recipe can return its extra empty bottle to this slot on client updates.
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == LEAF_SLOT && !isValidLeafInput(stack) && !stack.isEmpty() && !stack.is(Items.GLASS_BOTTLE)) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == WATER_SLOT && !isValidWaterInput(stack) && !stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == FUEL_SLOT && !isValidFuelInput(stack) && !stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == BARK_SLOT || slot == LEAF_SLOT || slot == WATER_SLOT || slot == FUEL_SLOT) {
            brewProgress = 0;
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
            case BARK_SLOT -> isValidBarkInput(stack);
            case LEAF_SLOT -> isValidLeafInput(stack);
            case WATER_SLOT -> isValidWaterInput(stack);
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
        return Component.translatable("container.trippycraft.brew_cauldron");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BrewCauldronMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("BrewProgress", brewProgress);
        tag.putInt("BrewProgressTotal", brewProgressTotal);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        brewProgress = NbtCompat.getIntOr(tag, "BrewProgress", 0);
        brewProgressTotal = NbtCompat.getIntOr(tag, "BrewProgressTotal", BREW_TIME);
        burnTime = NbtCompat.getIntOr(tag, "BurnTime", 0);
        burnTimeTotal = NbtCompat.getIntOr(tag, "BurnTimeTotal", 0);
    }
}
