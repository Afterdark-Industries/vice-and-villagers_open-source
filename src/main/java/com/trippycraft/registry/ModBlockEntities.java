package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import com.trippycraft.block.entity.AdvancedChemistryTableBlockEntity;
import com.trippycraft.block.entity.BrewCauldronBlockEntity;
import com.trippycraft.block.entity.CannabisPressBlockEntity;
import com.trippycraft.block.entity.ChemistryTableBlockEntity;
import com.trippycraft.block.entity.MortarAndPestleBlockEntity;
import com.trippycraft.block.entity.OpiumPoppyBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, TrippyCraft.MOD_ID);

    public static final Supplier<BlockEntityType<MortarAndPestleBlockEntity>> MORTAR_AND_PESTLE =
            BLOCK_ENTITIES.register("mortar_and_pestle",
                    () -> BlockEntityType.Builder.of(MortarAndPestleBlockEntity::new, ModBlocks.MORTAR_AND_PESTLE.get()).build(null));

    public static final Supplier<BlockEntityType<CannabisPressBlockEntity>> CANNABIS_PRESS =
            BLOCK_ENTITIES.register("cannabis_press",
                    () -> BlockEntityType.Builder.of(CannabisPressBlockEntity::new, ModBlocks.CANNABIS_PRESS.get()).build(null));

    public static final Supplier<BlockEntityType<ChemistryTableBlockEntity>> CHEMISTRY_TABLE =
            BLOCK_ENTITIES.register("chemistry_table",
                    () -> BlockEntityType.Builder.of(ChemistryTableBlockEntity::new, ModBlocks.CHEMISTRY_TABLE.get()).build(null));

    public static final Supplier<BlockEntityType<AdvancedChemistryTableBlockEntity>> ADVANCED_CHEMISTRY_TABLE =
            BLOCK_ENTITIES.register("advanced_chemistry_table",
                    () -> BlockEntityType.Builder.of(AdvancedChemistryTableBlockEntity::new, ModBlocks.ADVANCED_CHEMISTRY_TABLE.get()).build(null));

    public static final Supplier<BlockEntityType<BrewCauldronBlockEntity>> BREW_CAULDRON =
            BLOCK_ENTITIES.register("brew_cauldron",
                    () -> BlockEntityType.Builder.of(BrewCauldronBlockEntity::new, ModBlocks.BREW_CAULDRON.get()).build(null));

    public static final Supplier<BlockEntityType<OpiumPoppyBlockEntity>> OPIUM_POPPY =
            BLOCK_ENTITIES.register("opium_poppy",
                    () -> BlockEntityType.Builder.of(OpiumPoppyBlockEntity::new, ModBlocks.OPIUM_POPPY.get()).build(null));

    private ModBlockEntities() {
    }
}
