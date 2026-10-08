package com.trippycraft.block.entity;

import com.trippycraft.compat.NbtCompat;
import com.trippycraft.item.AlLadConcentrateItem;
import com.trippycraft.item.HoMetCrystalItem;
import com.trippycraft.item.MdmaCrystalItem;
import com.trippycraft.item.TwoCbCrystalItem;
import com.trippycraft.item.TwoCeCrystalItem;

import com.trippycraft.menu.AdvancedChemistryTableMenu;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public final class AdvancedChemistryTableBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int HEROIN_BASE_SLOT = 0;
    public static final int GLASS_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int SLOT_COUNT = 3;
    private static final int REFINEMENT_TIME = 220;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> refineProgress;
                case 1 -> refineProgressTotal;
                case 2 -> selectedProduct.buttonId();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> refineProgress = value;
                case 1 -> refineProgressTotal = value;
                case 2 -> {
                    RefinedProduct product = RefinedProduct.byButtonId(value);
                    if (product != null) {
                        selectedProduct = product;
                    }
                }
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    private int refineProgress;
    private int refineProgressTotal = REFINEMENT_TIME;
    private RefinedProduct selectedProduct = RefinedProduct.ONE_HUNDRED;

    public AdvancedChemistryTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ADVANCED_CHEMISTRY_TABLE.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AdvancedChemistryTableBlockEntity table) {
        boolean changed = false;

        if (table.canRefine()) {
            table.refineProgress++;
            changed = true;
            if (table.refineProgress >= table.refineProgressTotal) {
                table.finishRefining();
                changed = true;
            }
        } else if (table.refineProgress != 0) {
            table.refineProgress = 0;
            changed = true;
        }

        boolean shouldBeLit = table.refineProgress > 0 && table.canRefine();
        if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT) != shouldBeLit) {
            level.setBlock(pos, state.setValue(BlockStateProperties.LIT, shouldBeLit), Block.UPDATE_CLIENTS);
            state = level.getBlockState(pos);
            changed = true;
        }

        if (changed) {
            setChanged(level, pos, state);
        }
    }

    public boolean isValidHeroinBaseInput(ItemStack stack) {
        return stack.is(ModItems.HEROIN_BASE.get())
                || stack.is(ModItems.LSD_SOLUTION.get())
                || stack.is(ModItems.MDMA_CRYSTAL.get())
                || stack.is(ModItems.KETAMINE.get())
                || stack.is(ModItems.HO_MET_POWDER.get())
                || stack.is(ModItems.TWO_CB_POWDER.get())
                || stack.is(ModItems.TWO_CE_POWDER.get())
                || stack.is(ModItems.AL_LAD_CONCENTRATE.get());
    }

    public boolean isValidGlassInput(ItemStack stack) {
        return stack.is(Blocks.GLASS.asItem())
                || stack.is(ModItems.BLOTTER_SHEET.get())
                || stack.is(ModItems.EMPTY_CAPSULE.get())
                || stack.is(Items.GLASS_BOTTLE)
                || stack.is(Items.SUGAR);
    }

    public RefinedProduct selectedProduct() {
        return selectedProduct;
    }

    public boolean selectProductByButton(int buttonId) {
        RefinedProduct product = RefinedProduct.byButtonId(buttonId);
        if (product == null || product == selectedProduct) {
            return product != null;
        }
        selectedProduct = product;
        refineProgress = 0;
        setChanged();
        return true;
    }

    private boolean canRefine() {
        RefinementMode mode = getCurrentMode();
        if (mode == null) {
            return false;
        }

        ItemStack primary = items.get(HEROIN_BASE_SLOT);
        ItemStack secondary = items.get(GLASS_SLOT);
        if (!selectedProduct.hasRequiredInputs(mode, primary, secondary)) {
            return false;
        }

        return canOutput(selectedProduct.createResultStack(mode));
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

    private void finishRefining() {
        RefinementMode mode = getCurrentMode();
        if (mode == null) {
            refineProgress = 0;
            return;
        }

        ItemStack result = selectedProduct.createResultStack(mode);
        if (result.isEmpty() || !canRefine()) {
            refineProgress = 0;
            return;
        }

        if (mode == RefinementMode.AL_LAD_BLOTTER) {
            ItemStack concentrate = items.get(HEROIN_BASE_SLOT);
            if (!AlLadConcentrateItem.consumeMilligrams(concentrate, AlLadConcentrateItem.USE_MILLIGRAMS)) {
                refineProgress = 0;
                return;
            }
            if (concentrate.isEmpty()) {
                // The exhausted bottle fits in the primary slot vacated by its final charge.
                items.set(HEROIN_BASE_SLOT, new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        if (mode == RefinementMode.MDMA_CAPSULE) {
            MdmaCrystalItem.consumeMilligrams(items.get(HEROIN_BASE_SLOT), selectedProduct.mdmaMilligrams());
            if (items.get(HEROIN_BASE_SLOT).isEmpty()) {
                items.set(HEROIN_BASE_SLOT, ItemStack.EMPTY);
            }
        } else if (mode != RefinementMode.AL_LAD_BLOTTER) {
            consumeIngredient(HEROIN_BASE_SLOT, selectedProduct.primaryRequired(mode));
        }
        consumeIngredient(GLASS_SLOT, selectedProduct.secondaryRequired(mode));

        refineProgress = 0;
        setChanged();
    }

    @Nullable
    private RefinementMode getCurrentMode() {
        ItemStack primary = items.get(HEROIN_BASE_SLOT);
        ItemStack secondary = items.get(GLASS_SLOT);
        if (primary.is(ModItems.HEROIN_BASE.get()) && secondary.is(Blocks.GLASS.asItem())) {
            return RefinementMode.HEROIN;
        }
        if (primary.is(ModItems.LSD_SOLUTION.get()) && secondary.is(ModItems.BLOTTER_SHEET.get())) {
            return RefinementMode.LSD_BLOTTER;
        }
        if (primary.is(ModItems.KETAMINE.get()) && secondary.is(Blocks.GLASS.asItem())) {
            return RefinementMode.KETAMINE;
        }
        if (primary.is(ModItems.MDMA_CRYSTAL.get()) && secondary.is(ModItems.EMPTY_CAPSULE.get())) {
            return RefinementMode.MDMA_CAPSULE;
        }
        if (primary.is(ModItems.HO_MET_POWDER.get()) && secondary.is(Items.GLASS_BOTTLE)) {
            return RefinementMode.HO_MET_BOTTLE;
        }
        if (primary.is(ModItems.AL_LAD_CONCENTRATE.get()) && secondary.is(ModItems.BLOTTER_SHEET.get())) {
            return RefinementMode.AL_LAD_BLOTTER;
        }
        if (primary.is(ModItems.TWO_CB_POWDER.get()) && secondary.is(Items.SUGAR)) {
            return RefinementMode.TWO_CB_PILL;
        }
        if (primary.is(ModItems.TWO_CE_POWDER.get()) && secondary.is(Items.SUGAR)) {
            return RefinementMode.TWO_CE_PILL;
        }
        return null;
    }

    @Nullable
    private RefinementMode getDisplayMode() {
        ItemStack primary = items.get(HEROIN_BASE_SLOT);
        ItemStack secondary = items.get(GLASS_SLOT);

        boolean heroinPrimary = primary.is(ModItems.HEROIN_BASE.get());
        boolean heroinSecondary = secondary.is(Blocks.GLASS.asItem());
        boolean lsdPrimary = primary.is(ModItems.LSD_SOLUTION.get());
        boolean lsdSecondary = secondary.is(ModItems.BLOTTER_SHEET.get());
        boolean ketaminePrimary = primary.is(ModItems.KETAMINE.get());
        boolean ketamineSecondary = secondary.is(Blocks.GLASS.asItem());
        boolean mdmaPrimary = primary.is(ModItems.MDMA_CRYSTAL.get());
        boolean mdmaSecondary = secondary.is(ModItems.EMPTY_CAPSULE.get());

        if (primary.is(ModItems.TWO_CE_POWDER.get())) {
            return RefinementMode.TWO_CE_PILL;
        }
        if (primary.is(ModItems.TWO_CB_POWDER.get()) || (primary.isEmpty() && secondary.is(Items.SUGAR))) {
            return RefinementMode.TWO_CB_PILL;
        }
        if (primary.is(ModItems.AL_LAD_CONCENTRATE.get())) {
            return RefinementMode.AL_LAD_BLOTTER;
        }
        if (primary.is(ModItems.HO_MET_POWDER.get()) || (primary.isEmpty() && secondary.is(Items.GLASS_BOTTLE))) {
            return RefinementMode.HO_MET_BOTTLE;
        }
        if (ketaminePrimary) {
            return RefinementMode.KETAMINE;
        }
        if (mdmaPrimary || mdmaSecondary) {
            return RefinementMode.MDMA_CAPSULE;
        }
        if ((heroinPrimary || heroinSecondary) && !(lsdPrimary || lsdSecondary || ketaminePrimary)) {
            return RefinementMode.HEROIN;
        }
        if ((lsdPrimary || lsdSecondary) && !(heroinPrimary || heroinSecondary || ketaminePrimary || ketamineSecondary)) {
            return RefinementMode.LSD_BLOTTER;
        }
        return getCurrentMode();
    }

    public String previewOutputSummary() {
        return previewOutputSummary(selectedProduct);
    }

    public String previewOutputSummary(RefinedProduct product) {
        RefinementMode mode = getDisplayMode();
        if (mode == null) {
            return "Select a recipe";
        }
        return product.outputCount(mode) + "x " + product.label(mode)
                + (mode == RefinementMode.AL_LAD_BLOTTER ? " AL-LAD tabs"
                : mode == RefinementMode.TWO_CB_PILL ? " 2C-B pills"
                : mode == RefinementMode.TWO_CE_PILL ? " 2C-E pills" : "");
    }

    public String previewRequirementSummary() {
        return previewRequirementSummary(selectedProduct);
    }

    public String previewRequirementSummary(RefinedProduct product) {
        RefinementMode mode = getDisplayMode();
        if (mode == null) {
            return "Insert matching materials";
        }
        return product.requirementSummary(mode);
    }

    private void consumeIngredient(int slot, int amount) {
        ItemStack stack = items.get(slot);
        if (stack.isEmpty() || amount <= 0) {
            return;
        }

        ItemStack remainder = stack.getCraftingRemainingItem();
        int consumed = Math.min(amount, stack.getCount());
        stack.shrink(consumed);
        if (stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }

        if (remainder.isEmpty()) {
            return;
        }

        ItemStack totalRemainder = remainder.copy();
        totalRemainder.setCount(remainder.getCount() * consumed);

        ItemStack updatedStack = items.get(slot);
        if (updatedStack.isEmpty()) {
            items.set(slot, totalRemainder);
            return;
        }

        if (ItemStack.isSameItemSameComponents(updatedStack, totalRemainder)
                && updatedStack.getCount() + totalRemainder.getCount() <= updatedStack.getMaxStackSize()) {
            updatedStack.grow(totalRemainder.getCount());
            return;
        }

        if (level != null) {
            Block.popResource(level, worldPosition, totalRemainder);
        }
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
        if (slot == HEROIN_BASE_SLOT && !isValidHeroinBaseInput(stack) && !stack.isEmpty()
                && !stack.is(Items.GLASS_BOTTLE)) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == GLASS_SLOT && !isValidGlassInput(stack) && !stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
        }
        if (slot == HEROIN_BASE_SLOT || slot == GLASS_SLOT) {
            refineProgress = 0;
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
            case HEROIN_BASE_SLOT -> isValidHeroinBaseInput(stack);
            case GLASS_SLOT -> isValidGlassInput(stack);
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.trippycraft.advanced_chemistry_table");
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AdvancedChemistryTableMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("RefineProgress", refineProgress);
        tag.putInt("RefineProgressTotal", refineProgressTotal);
        tag.putInt("SelectedProduct", selectedProduct.buttonId());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        refineProgress = NbtCompat.getIntOr(tag, "RefineProgress", 0);
        refineProgressTotal = NbtCompat.getIntOr(tag, "RefineProgressTotal", REFINEMENT_TIME);
        selectedProduct = RefinedProduct.bySavedId(NbtCompat.getIntOr(tag, "SelectedProduct", RefinedProduct.ONE_HUNDRED.buttonId()));
    }

    public enum RefinedProduct {
        TWENTY_FIVE(0, "25", 1, 1, 4, ModItems.HEROIN_25MG, 40, ModItems.LSD_BLOTTER_50UG, 40, ModItems.KETAMINE_25MG, ModItems.MDMA_CAPS_25MG, ModItems.HO_MET_25MG, ModItems.AL_LAD_25MG, ModItems.TWO_CB_25MG, ModItems.TWO_CE_25MG),
        ONE_HUNDRED(1, "100", 1, 1, 1, ModItems.HEROIN_100MG, 10, ModItems.LSD_BLOTTER_100UG, 10, ModItems.KETAMINE_100MG, ModItems.MDMA_CAPS_100MG, ModItems.HO_MET_100MG, ModItems.AL_LAD_100MG, ModItems.TWO_CB_100MG, ModItems.TWO_CE_100MG),
        TWO_FIFTY(2, "250", 5, 2, 2, ModItems.HEROIN_250MG, 4, ModItems.LSD_BLOTTER_250UG, 4, ModItems.KETAMINE_250MG, ModItems.MDMA_CAPS_250MG, ModItems.HO_MET_250MG, ModItems.AL_LAD_250MG, ModItems.TWO_CB_250MG, ModItems.TWO_CE_250MG),
        FIVE_HUNDRED(3, "500", 5, 1, 1, ModItems.HEROIN_500MG, 2, ModItems.LSD_BLOTTER_500UG, 2, ModItems.KETAMINE_500MG, ModItems.MDMA_CAPS_500MG, ModItems.HO_MET_500MG, ModItems.AL_LAD_500MG, ModItems.TWO_CB_500MG, ModItems.TWO_CE_500MG);

        private final int buttonId;
        private final String amountLabel;
        private final int heroinBaseRequired;
        private final int glassRequired;
        private final int heroinOutputCount;
        private final Supplier<Item> heroinResultItem;
        private final int lsdOutputCount;
        private final Supplier<Item> lsdResultItem;
        private final int ketamineOutputCount;
        private final Supplier<Item> ketamineResultItem;
        private final Supplier<Item> mdmaResultItem;
        private final Supplier<Item> hoMetResultItem;
        private final Supplier<Item> alLadResultItem;
        private final Supplier<Item> twoCbResultItem;
        private final Supplier<Item> twoCeResultItem;

        RefinedProduct(int buttonId,
                       String amountLabel,
                       int heroinBaseRequired,
                       int glassRequired,
                       int heroinOutputCount,
                       Supplier<Item> heroinResultItem,
                       int lsdOutputCount,
                       Supplier<Item> lsdResultItem,
                       int ketamineOutputCount,
                       Supplier<Item> ketamineResultItem,
                       Supplier<Item> mdmaResultItem,
                       Supplier<Item> hoMetResultItem,
                       Supplier<Item> alLadResultItem,
                       Supplier<Item> twoCbResultItem,
                       Supplier<Item> twoCeResultItem) {
            this.buttonId = buttonId;
            this.amountLabel = amountLabel;
            this.heroinBaseRequired = heroinBaseRequired;
            this.glassRequired = glassRequired;
            this.heroinOutputCount = heroinOutputCount;
            this.heroinResultItem = heroinResultItem;
            this.lsdOutputCount = lsdOutputCount;
            this.lsdResultItem = lsdResultItem;
            this.ketamineOutputCount = ketamineOutputCount;
            this.ketamineResultItem = ketamineResultItem;
            this.mdmaResultItem = mdmaResultItem;
            this.hoMetResultItem = hoMetResultItem;
            this.alLadResultItem = alLadResultItem;
            this.twoCbResultItem = twoCbResultItem;
            this.twoCeResultItem = twoCeResultItem;
        }

        public int buttonId() {
            return buttonId;
        }

        public String label(RefinementMode mode) {
            return amountLabel + (mode == RefinementMode.LSD_BLOTTER ? "ug" : "mg");
        }

        public int heroinBaseRequired() {
            return heroinBaseRequired;
        }

        public int glassRequired() {
            return glassRequired;
        }

        public int outputCount(RefinementMode mode) {
            return switch (mode) {
                case HEROIN -> heroinOutputCount;
                case LSD_BLOTTER -> lsdOutputCount;
                case KETAMINE -> ketamineOutputCount;
                case MDMA_CAPSULE -> 1;
                case AL_LAD_BLOTTER -> AlLadConcentrateItem.USE_MILLIGRAMS / amountMilligrams();
                case HO_MET_BOTTLE, TWO_CB_PILL, TWO_CE_PILL -> switch (this) {
                    case TWENTY_FIVE -> 4;
                    case TWO_FIFTY -> 2;
                    default -> 1;
                };
            };
        }

        public int mdmaMilligrams() {
            return amountMilligrams();
        }

        public int amountMilligrams() {
            return Integer.parseInt(amountLabel);
        }

        public int primaryRequired(RefinementMode mode) {
            if (mode == RefinementMode.AL_LAD_BLOTTER) {
                return 0;
            }
            if (mode == RefinementMode.HO_MET_BOTTLE) {
                // Package exact batches so each 100mg powder is fully distributed without rounding.
                return outputCount(mode) * amountMilligrams() / HoMetCrystalItem.POWDER_MILLIGRAMS;
            }
            if (mode == RefinementMode.TWO_CB_PILL) {
                return outputCount(mode) * amountMilligrams() / TwoCbCrystalItem.POWDER_MILLIGRAMS;
            }
            if (mode == RefinementMode.TWO_CE_PILL) {
                return outputCount(mode) * amountMilligrams() / TwoCeCrystalItem.POWDER_MILLIGRAMS;
            }
            return mode == RefinementMode.HEROIN ? heroinBaseRequired : mode == RefinementMode.MDMA_CAPSULE ? 0 : 1;
        }

        public int secondaryRequired(RefinementMode mode) {
            if (mode == RefinementMode.HO_MET_BOTTLE || mode == RefinementMode.TWO_CB_PILL || mode == RefinementMode.TWO_CE_PILL) {
                return outputCount(mode);
            }
            return mode == RefinementMode.HEROIN ? glassRequired : 1;
        }

        public ItemStack createResultStack(RefinementMode mode) {
            Supplier<Item> resultItem = switch (mode) {
                case HEROIN -> heroinResultItem;
                case LSD_BLOTTER -> lsdResultItem;
                case KETAMINE -> ketamineResultItem;
                case MDMA_CAPSULE -> mdmaResultItem;
                case HO_MET_BOTTLE -> hoMetResultItem;
                case AL_LAD_BLOTTER -> alLadResultItem;
                case TWO_CB_PILL -> twoCbResultItem;
                case TWO_CE_PILL -> twoCeResultItem;
            };
            return new ItemStack(resultItem.get(), outputCount(mode));
        }

        public boolean hasRequiredInputs(RefinementMode mode, ItemStack primary, ItemStack secondary) {
            if (mode == RefinementMode.TWO_CE_PILL) {
                return primary.is(ModItems.TWO_CE_POWDER.get())
                        && primary.getCount() >= primaryRequired(mode)
                        && secondary.is(Items.SUGAR)
                        && secondary.getCount() >= secondaryRequired(mode);
            }
            if (mode == RefinementMode.TWO_CB_PILL) {
                return primary.is(ModItems.TWO_CB_POWDER.get())
                        && primary.getCount() >= primaryRequired(mode)
                        && secondary.is(Items.SUGAR)
                        && secondary.getCount() >= secondaryRequired(mode);
            }
            if (mode == RefinementMode.AL_LAD_BLOTTER) {
                return primary.is(ModItems.AL_LAD_CONCENTRATE.get())
                        && primary.getCount() == 1
                        && AlLadConcentrateItem.remainingMilligrams(primary) >= AlLadConcentrateItem.USE_MILLIGRAMS
                        && secondary.is(ModItems.BLOTTER_SHEET.get())
                        && secondary.getCount() >= 1;
            }
            if (mode == RefinementMode.HO_MET_BOTTLE) {
                return primary.is(ModItems.HO_MET_POWDER.get())
                        && primary.getCount() >= primaryRequired(mode)
                        && secondary.is(Items.GLASS_BOTTLE)
                        && secondary.getCount() >= secondaryRequired(mode);
            }
            if (mode == RefinementMode.HEROIN) {
                return primary.is(ModItems.HEROIN_BASE.get())
                        && primary.getCount() >= heroinBaseRequired
                        && secondary.is(Blocks.GLASS.asItem())
                        && secondary.getCount() >= glassRequired;
            }
            if (mode == RefinementMode.KETAMINE) {
                return primary.is(ModItems.KETAMINE.get())
                        && primary.getCount() >= 1
                        && secondary.is(Blocks.GLASS.asItem())
                        && secondary.getCount() >= 1;
            }
            if (mode == RefinementMode.MDMA_CAPSULE) {
                return primary.is(ModItems.MDMA_CRYSTAL.get())
                        && primary.getCount() == 1
                        && MdmaCrystalItem.remainingMilligrams(primary) >= mdmaMilligrams()
                        && secondary.is(ModItems.EMPTY_CAPSULE.get())
                        && secondary.getCount() >= 1;
            }
            return primary.is(ModItems.LSD_SOLUTION.get())
                    && primary.getCount() >= 1
                    && secondary.is(ModItems.BLOTTER_SHEET.get())
                    && secondary.getCount() >= 1;
        }

        public String requirementSummary(RefinementMode mode) {
            if (mode == RefinementMode.TWO_CE_PILL) {
                return primaryRequired(mode) + " powder (" + TwoCeCrystalItem.POWDER_MILLIGRAMS
                        + "mg) + " + secondaryRequired(mode) + " sugar";
            }
            if (mode == RefinementMode.TWO_CB_PILL) {
                return primaryRequired(mode) + " powder (" + TwoCbCrystalItem.POWDER_MILLIGRAMS
                        + "mg) + " + secondaryRequired(mode) + " sugar";
            }
            if (mode == RefinementMode.AL_LAD_BLOTTER) {
                return AlLadConcentrateItem.USE_MILLIGRAMS + "mg concentrate + 1 sheet ("
                        + AlLadConcentrateItem.CAPACITY_MILLIGRAMS / AlLadConcentrateItem.USE_MILLIGRAMS
                        + " sheets per full bottle)";
            }
            if (mode == RefinementMode.HO_MET_BOTTLE) {
                return primaryRequired(mode) + " powder (" + HoMetCrystalItem.POWDER_MILLIGRAMS
                        + "mg) + " + secondaryRequired(mode) + " bottles";
            }
            if (mode == RefinementMode.HEROIN) {
                return heroinBaseRequired + " base + " + glassRequired + " glass";
            }
            if (mode == RefinementMode.KETAMINE) {
                return "1 bottle + 1 glass";
            }
            if (mode == RefinementMode.MDMA_CAPSULE) {
                return mdmaMilligrams() + "mg charge + 1 capsule";
            }
            return "1 solution + 1 sheet";
        }

        @Nullable
        public static RefinedProduct byButtonId(int buttonId) {
            for (RefinedProduct product : values()) {
                if (product.buttonId == buttonId) {
                    return product;
                }
            }
            return null;
        }

        public static RefinedProduct bySavedId(int savedId) {
            RefinedProduct product = byButtonId(savedId);
            return product != null ? product : ONE_HUNDRED;
        }
    }

    public enum RefinementMode {
        HEROIN,
        LSD_BLOTTER,
        KETAMINE,
        MDMA_CAPSULE,
        HO_MET_BOTTLE,
        AL_LAD_BLOTTER,
        TWO_CB_PILL,
        TWO_CE_PILL
    }
}
