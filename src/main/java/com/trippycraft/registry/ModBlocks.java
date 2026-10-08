package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import com.trippycraft.block.AdvancedChemistryTableBlock;
import com.trippycraft.block.BrewCauldronBlock;
import com.trippycraft.block.CannabisPlantBlock;
import com.trippycraft.block.CannabisPressBlock;
import com.trippycraft.block.ChemistryTableBlock;
import com.trippycraft.block.ErgotWheatBlock;
import com.trippycraft.block.KokaPlantBlock;
import com.trippycraft.block.MescalineCactusBlock;
import com.trippycraft.block.MortarAndPestleBlock;
import com.trippycraft.block.OpiumPoppyBlock;
import com.trippycraft.block.SaltCrystalBlock;
import com.trippycraft.block.SalviaDivinorumPlantBlock;
import com.trippycraft.block.StrangeMushroomBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, TrippyCraft.MOD_ID);

    public static final Supplier<Block> MORTAR_AND_PESTLE = BLOCKS.register("mortar_and_pestle",
            () -> new MortarAndPestleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.8F)
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    public static final Supplier<Block> CANNABIS_PRESS = BLOCKS.register("cannabis_press",
            () -> new CannabisPressBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.2F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final Supplier<Block> CHEMISTRY_TABLE = BLOCKS.register("chemistry_table",
            () -> new ChemistryTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.8F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 8 : 0)
                    .noOcclusion()));

    public static final Supplier<Block> ADVANCED_CHEMISTRY_TABLE = BLOCKS.register("advanced_chemistry_table",
            () -> new AdvancedChemistryTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.4F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT) ? 10 : 0)
                    .noOcclusion()));

    public static final Supplier<Block> BREW_CAULDRON = BLOCKS.register("brew_cauldron",
            () -> new BrewCauldronBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 8)
                    .noOcclusion()));

    public static final Supplier<Block> BARTENDER_STATION = BLOCKS.register("bartender_station",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)));

    public static final Supplier<Block> STRANGE_MUSHROOM = BLOCKS.register("strange_mushroom",
            () -> new StrangeMushroomBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)));

    public static final Supplier<Block> CANNABIS_PLANT = BLOCKS.register("cannabis_plant",
            () -> new CannabisPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .randomTicks()
                    .sound(SoundType.CROP)));

    public static final Supplier<Block> KOKA_PLANT = BLOCKS.register("koka_plant",
            () -> new KokaPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .randomTicks()
                    .sound(SoundType.CROP)));

    public static final Supplier<Block> OPIUM_POPPY = BLOCKS.register("opium_poppy",
            () -> new OpiumPoppyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .randomTicks()
                    .sound(SoundType.CROP)));

    public static final Supplier<Block> SALVIA_DIVINORUM_PLANT = BLOCKS.register("salvia_divinorum_plant",
            () -> new SalviaDivinorumPlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .randomTicks()
                    .sound(SoundType.CROP)));

    public static final Supplier<Block> MESCALINE_CACTUS = BLOCKS.register("mescaline_cactus",
            () -> new MescalineCactusBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.4F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final Supplier<Block> ERGOT_INFECTED_WHEAT = BLOCKS.register("ergot_infected_wheat",
            () -> new ErgotWheatBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.CROP)));

    public static final Supplier<Block> SALT_CRYSTAL = BLOCKS.register("salt_crystal",
            () -> new SaltCrystalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(2.0F)
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    private ModBlocks() {
    }
}
