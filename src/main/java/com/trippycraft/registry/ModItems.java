package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import com.trippycraft.item.AlcoholItem;
import com.trippycraft.item.BlendCapsuleItem;
import com.trippycraft.item.CannabisSeedsItem;
import com.trippycraft.item.CannabisDoseItem;
import com.trippycraft.item.CocaineItem;
import com.trippycraft.item.CursedRecipeCodexItem;
import com.trippycraft.item.DmtItem;
import com.trippycraft.item.DmtPowderItem;
import com.trippycraft.item.AncientPipeItem;
import com.trippycraft.item.EthanolItem;
import com.trippycraft.item.FlavorBlockItem;
import com.trippycraft.item.FlavorItem;
import com.trippycraft.item.HeroinItem;
import com.trippycraft.item.AlLadDoseItem;
import com.trippycraft.item.AlLadConcentrateItem;
import com.trippycraft.item.TwoCeDoseItem;
import com.trippycraft.item.TwoCeCrystalItem;
import com.trippycraft.item.NitrousDoseItem;
import com.trippycraft.item.HoMetDoseItem;
import com.trippycraft.item.HoMetCrystalItem;
import com.trippycraft.item.HoMetPowderItem;
import com.trippycraft.item.KokaSeedsItem;
import com.trippycraft.item.KetamineBottleItem;
import com.trippycraft.item.KetamineDoseItem;
import com.trippycraft.item.KokaDoseItem;
import com.trippycraft.item.LsdBlotterItem;
import com.trippycraft.item.LsdSolutionItem;
import com.trippycraft.item.MdmaDoseItem;
import com.trippycraft.item.MdmaCrystalItem;
import com.trippycraft.item.MescalineDoseItem;
import com.trippycraft.item.MortarAndPestleBlockItem;
import com.trippycraft.item.OpiumPoppySeedsItem;
import com.trippycraft.item.PsychoactiveItem;
import com.trippycraft.item.PsilocybinCapsuleItem;
import com.trippycraft.item.SalviaLeafItem;
import com.trippycraft.item.SalviaSeedsItem;
import com.trippycraft.item.BongItem;
import com.trippycraft.item.TwoCbDoseItem;
import com.trippycraft.item.TwoCbCrystalItem;
import com.trippycraft.substance.SubstanceProfile;
import com.trippycraft.substance.SubstanceProfiles;
import com.trippycraft.substance.cocaine.CocaineRoute;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, TrippyCraft.MOD_ID);

    private static final Map<SubstanceProfile, Supplier<Item>> SUBSTANCE_ITEMS = new LinkedHashMap<>();
    private static final List<Supplier<Item>> CREATIVE_ITEMS = new ArrayList<>();

    public static final Supplier<Item> CURSED_RECIPE_CODEX = registerCodex("cursed_recipe_codex");
    public static final Supplier<Item> STRANGE_MUSHROOM = registerBlockItem("strange_mushroom", ModBlocks.STRANGE_MUSHROOM);
    public static final Supplier<Item> DRIED_STRANGE_MUSHROOM = registerSimpleItem("dried_strange_mushroom");
    public static final Supplier<Item> PSILOCYBIN_POWDER = registerSimpleItem("psilocybin_powder");
    public static final Supplier<Item> PACKED_PSILOCYBIN_POWDER = registerSimpleItem("packed_psilocybin_powder");
    public static final Supplier<Item> EMPTY_CAPSULE = registerSimpleItem("empty_capsule");
    public static final Supplier<Item> BLEND_CAPSULE = registerBlendCapsule("blend_capsule");
    public static final Supplier<Item> MORTAR_AND_PESTLE = registerMortarAndPestleBlockItem();
    public static final Supplier<Item> CANNABIS_SEEDS = registerCannabisSeeds();
    public static final Supplier<Item> CANNABIS_BUD = registerSimpleItem("cannabis_bud");
    public static final Supplier<Item> DRIED_CANNABIS_BUD = registerFlavorItem("dried_cannabis_bud", "tooltip.trippycraft.dried_cannabis_bud", 64);
    public static final Supplier<Item> ACACIA_BARK = registerSimpleItem("acacia_bark");
    public static final Supplier<Item> ACACIA_LEAF = registerSimpleItem("acacia_leaf");
    public static final Supplier<Item> PLANT_EXTRACT = registerSimpleItem("plant_extract");
    public static final Supplier<Item> BUNDLED_PLANT_EXTRACT = registerSimpleItem("bundled_plant_extract");
    public static final Supplier<Item> KOKA_PLANT = registerBlockItem("koka_plant", ModBlocks.KOKA_PLANT, false);
    public static final Supplier<Item> KOKA_SEEDS = registerKokaSeeds();
    public static final Supplier<Item> KOKA_LEAVES = registerKokaDoseItem("koka_leaves", 0.10D, 64);
    public static final Supplier<Item> DRIED_KOKA_LEAVES = registerSimpleItem("dried_koka_leaves");
    public static final Supplier<Item> GROUND_KOKA_LEAVES = registerSimpleItem("ground_koka_leaves");
    public static final Supplier<Item> KOKA_PASTE = registerSimpleItem("koka_paste");
    public static final Supplier<Item> HARDENED_KOKA_PASTE = registerSimpleItem("hardened_koka_paste");
    public static final Supplier<Item> OPIUM_POPPY_SEEDS = registerOpiumPoppySeeds();
    public static final Supplier<Item> OPIUM_LATEX = registerHeroin("opium_latex", 1.0D, true);
    public static final Supplier<Item> RAW_OPIUM = registerSimpleItem("raw_opium");
    public static final Supplier<Item> MORPHINE_BASE = registerHeroin("morphine_base", 5.0D, true);
    public static final Supplier<Item> ERGOT_CLUMP = registerSimpleItem("ergot_clump");
    public static final Supplier<Item> ERGOT_INFECTED_WHEAT = registerBlockItem("ergot_infected_wheat", ModBlocks.ERGOT_INFECTED_WHEAT);
    public static final Supplier<Item> ETHANOL = registerEthanol("ethanol");
    public static final Supplier<Item> DISTILLED_SOLVENT = registerFlavorItem("distilled_solvent", "tooltip.trippycraft.distilled_solvent", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> MEDICINAL_REAGENT = registerFlavorItem("medicinal_reagent", "tooltip.trippycraft.medicinal_reagent", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> AROMATIC_EXTRACT = registerFlavorItem("aromatic_extract", "tooltip.trippycraft.aromatic_extract", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> REFINED_AROMATIC_OIL = registerFlavorItem("refined_aromatic_oil", "tooltip.trippycraft.refined_aromatic_oil", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> CRUDE_EMPATHOGEN_BASE = registerFlavorItem("crude_empathogen_base", "tooltip.trippycraft.crude_empathogen_base", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> KETAMINE_PRECURSOR = registerFlavorItem("ketamine_precursor", "tooltip.trippycraft.ketamine_precursor", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> LYSERGIC_EXTRACT = registerSimpleItem("lysergic_extract");
    public static final Supplier<Item> LSD_SOLUTION = registerLsdSolution("lsd_solution", 1000.0D);
    public static final Supplier<Item> BLOTTER_SHEET = registerSimpleItem("blotter_sheet");
    public static final Supplier<Item> SALT_CRYSTAL = registerBlockItem("salt_crystal", ModBlocks.SALT_CRYSTAL);
    public static final Supplier<Item> FROST_SALT = registerSimpleItem("frost_salt");
    public static final Supplier<Item> THC_EXTRACT = registerSimpleItem("thc_extract");
    public static final Supplier<Item> PACKED_THC_EXTRACT = registerSimpleItem("packed_thc_extract");
    public static final Supplier<Item> CANNABIS_PRESS = registerFlavorBlockItem("cannabis_press", ModBlocks.CANNABIS_PRESS, "tooltip.trippycraft.cannabis_press");
    public static final Supplier<Item> CHEMISTRY_TABLE = registerFlavorBlockItem("chemistry_table", ModBlocks.CHEMISTRY_TABLE, "tooltip.trippycraft.chemistry_table");
    public static final Supplier<Item> ADVANCED_CHEMISTRY_TABLE = registerFlavorBlockItem("advanced_chemistry_table", ModBlocks.ADVANCED_CHEMISTRY_TABLE, "tooltip.trippycraft.advanced_chemistry_table");
    public static final Supplier<Item> BREW_CAULDRON = registerFlavorBlockItem("brew_cauldron", ModBlocks.BREW_CAULDRON, "tooltip.trippycraft.brew_cauldron");
    public static final Supplier<Item> BARTENDER_STATION = registerBlockItem("bartender_station", ModBlocks.BARTENDER_STATION);
    public static final Supplier<Item> PSILOCYBIN_CAPSULE_05G = registerPsilocybinCapsule("psilocybin_capsule_0_5g", 0.5D);
    public static final Supplier<Item> PSILOCYBIN_CAPSULE_1G = registerPsilocybinCapsule("psilocybin_capsule_1g", 1.0D);
    public static final Supplier<Item> PSILOCYBIN_CAPSULE_3G = registerPsilocybinCapsule("psilocybin_capsule_3g", 3.0D);
    public static final Supplier<Item> PSILOCYBIN_CAPSULE_5G = registerPsilocybinCapsule("psilocybin_capsule_5g", 5.0D);
    // Keep the legacy registry id stable so existing worlds and loot tables still resolve this item.
    public static final Supplier<Item> LSD_BLOTTER_50UG = registerLsdBlotter("lsd_blotter_50ug", 25.0D);
    public static final Supplier<Item> LSD_BLOTTER_100UG = registerLsdBlotter("lsd_blotter_100ug", 100.0D);
    public static final Supplier<Item> LSD_BLOTTER_250UG = registerLsdBlotter("lsd_blotter_250ug", 250.0D);
    public static final Supplier<Item> LSD_BLOTTER_500UG = registerLsdBlotter("lsd_blotter_500ug", 500.0D);
    public static final Supplier<Item> LSD = LSD_BLOTTER_100UG;
    public static final Supplier<Item> TWO_CB_25MG = registerTwoCbDose("two_cb_25mg", 25.0D, false);
    public static final Supplier<Item> TWO_CB_100MG = registerTwoCbDose("two_cb_100mg", 100.0D, true);
    public static final Supplier<Item> TWO_CB_250MG = registerTwoCbDose("two_cb_250mg", 250.0D, false);
    public static final Supplier<Item> TWO_CB_500MG = registerTwoCbDose("two_cb_500mg", 500.0D, false);
    public static final Supplier<Item> TWO_CB = TWO_CB_100MG;
    public static final Supplier<Item> PHENETHYLAMINE_BASE = registerFlavorItem("phenethylamine_base", "tooltip.trippycraft.phenethylamine_base", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> DIMETHOXY_INTERMEDIATE = registerFlavorItem("dimethoxy_intermediate", "tooltip.trippycraft.dimethoxy_intermediate", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> TWO_CB_FREEBASE = registerFlavorItem("two_cb_freebase", "tooltip.trippycraft.two_cb_freebase", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> TWO_CB_CRYSTAL = registerTwoCbCrystal();
    public static final Supplier<Item> TWO_CB_POWDER = registerFlavorItem("two_cb_powder", "tooltip.trippycraft.two_cb_powder", 64);
    public static final Supplier<Item> AMINOETHYL_REAGENT = registerFlavorItem("aminoethyl_reagent", "tooltip.trippycraft.aminoethyl_reagent", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> MET_INTERMEDIATE = registerFlavorItem("met_intermediate", "tooltip.trippycraft.met_intermediate", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> HO_MET_FREEBASE = registerFlavorItem("four_ho_met_freebase", "tooltip.trippycraft.four_ho_met_freebase", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> HO_MET_CRYSTAL = registerHoMetCrystal();
    public static final Supplier<Item> HO_MET_POWDER = registerHoMetPowder();
    public static final Supplier<Item> HO_MET_25MG = registerHoMetDose("four_ho_met_25mg", 25, 5.0D, false, true);
    public static final Supplier<Item> HO_MET_100MG = registerHoMetDose("four_ho_met_100mg", 100, 10.0D, false, true);
    public static final Supplier<Item> HO_MET_250MG = registerHoMetDose("four_ho_met_250mg", 250, 20.0D, true, true);
    public static final Supplier<Item> HO_MET_500MG = registerHoMetDose("four_ho_met_500mg", 500, 40.0D, false, true);
    public static final Supplier<Item> HO_MET = HO_MET_250MG;
    // Old world items remain usable at the corresponding format and original effect tier.
    public static final Supplier<Item> HO_MET_5MG = registerHoMetDose("four_ho_met_5mg", 25, 5.0D, false, false);
    public static final Supplier<Item> HO_MET_10MG = registerHoMetDose("four_ho_met_10mg", 100, 10.0D, false, false);
    public static final Supplier<Item> HO_MET_20MG = registerHoMetDose("four_ho_met_20mg", 250, 20.0D, false, false);
    public static final Supplier<Item> HO_MET_40MG = registerHoMetDose("four_ho_met_40mg", 500, 40.0D, false, false);
    public static final Supplier<Item> ALLYL_LYSERGIC_INTERMEDIATE = registerFlavorItem("allyl_lysergic_intermediate", "tooltip.trippycraft.allyl_lysergic_intermediate", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> AL_LAD_CONCENTRATE = registerAlLadConcentrate();
    public static final Supplier<Item> AL_LAD_25MG = registerAlLadDose("al_lad_25mg", 25, 75.0D, false, true);
    public static final Supplier<Item> AL_LAD_100MG = registerAlLadDose("al_lad_100mg", 100, 150.0D, true, true);
    public static final Supplier<Item> AL_LAD_250MG = registerAlLadDose("al_lad_250mg", 250, 300.0D, false, true);
    public static final Supplier<Item> AL_LAD_500MG = registerAlLadDose("al_lad_500mg", 500, 450.0D, false, true);
    public static final Supplier<Item> AL_LAD = AL_LAD_100MG;
    // Saved old-format tabs retain their original effect tier and adopt the matching content label.
    public static final Supplier<Item> AL_LAD_75UG = registerAlLadDose("al_lad_75ug", 25, 75.0D, false, false);
    public static final Supplier<Item> AL_LAD_150UG = registerAlLadDose("al_lad_150ug", 100, 150.0D, false, false);
    public static final Supplier<Item> AL_LAD_300UG = registerAlLadDose("al_lad_300ug", 250, 300.0D, false, false);
    public static final Supplier<Item> AL_LAD_450UG = registerAlLadDose("al_lad_450ug", 500, 450.0D, false, false);
    public static final Supplier<Item> TWO_CE_PRECURSOR = registerFlavorItem("two_ce_precursor", "tooltip.trippycraft.two_ce_precursor", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> TWO_CE_FREEBASE = registerFlavorItem("two_ce_freebase", "tooltip.trippycraft.two_ce_freebase", 64, Items.GLASS_BOTTLE);
    public static final Supplier<Item> TWO_CE_CRYSTAL = registerTwoCeCrystal();
    public static final Supplier<Item> TWO_CE_POWDER = registerFlavorItem("two_ce_powder", "tooltip.trippycraft.two_ce_powder", 64);
    public static final Supplier<Item> TWO_CE_25MG = registerTwoCeDose("two_ce_25mg", 25, 10.0D, false, true);
    public static final Supplier<Item> TWO_CE_100MG = registerTwoCeDose("two_ce_100mg", 100, 20.0D, false, true);
    public static final Supplier<Item> TWO_CE_250MG = registerTwoCeDose("two_ce_250mg", 250, 40.0D, true, true);
    public static final Supplier<Item> TWO_CE_500MG = registerTwoCeDose("two_ce_500mg", 500, 80.0D, false, true);
    public static final Supplier<Item> TWO_CE = TWO_CE_250MG;
    // Retain saved item IDs while adopting the matching content label and original effect tier.
    public static final Supplier<Item> TWO_CE_10MG = registerTwoCeDose("two_ce_10mg", 25, 10.0D, false, false);
    public static final Supplier<Item> TWO_CE_20MG = registerTwoCeDose("two_ce_20mg", 100, 20.0D, false, false);
    public static final Supplier<Item> TWO_CE_40MG = registerTwoCeDose("two_ce_40mg", 250, 40.0D, false, false);
    public static final Supplier<Item> TWO_CE_80MG = registerTwoCeDose("two_ce_80mg", 500, 80.0D, false, false);
    public static final Supplier<Item> DMT = registerDmt("dmt_crystal", 1.0D);
    public static final Supplier<Item> DMT_5X = registerDmt("dmt_crystal_5x", 5.0D);
    public static final Supplier<Item> DMT_POWDER = registerDmtPowder();
    public static final Supplier<Item> CANNABIS_JOINT = registerCannabisDoseItem("cannabis_joint", 200, 50, true);
    public static final Supplier<Item> CANNABIS_DAB_PEN = registerCannabisDoseItem("cannabis_dab_pen", 1000, 50, false);
    public static final Supplier<Item> CANNABIS_GUMMIES = registerCannabisDoseItem(
            "cannabis_gummies",
            1000,
            500,
            false,
            "tooltip.trippycraft.cannabis_half_mg",
            "tooltip.trippycraft.cannabis_gummies_stacks");
        public static final Supplier<Item> CANNABIS_CHOCOLATE_BAR = registerCannabisDoseItem(
            "cannabis_chocolate_bar",
            250,
            125,
            false,
            "tooltip.trippycraft.cannabis_half_mg",
            "tooltip.trippycraft.cannabis_chocolate_stacks");
    public static final Supplier<Item> CANNABIS = CANNABIS_JOINT;
    public static final Supplier<Item> COCAINE_POWDER = registerCocaine("cocaine_powder", CocaineRoute.POWDER, 1.0D, true);
    public static final Supplier<Item> CRACK_COCAINE = registerCocaine("crack_cocaine", CocaineRoute.CRACK, 5.0D, false);
    public static final Supplier<Item> COCAINE = COCAINE_POWDER;
    public static final Supplier<Item> HEROIN_BASE = registerHeroin("heroin_base", 10.0D, true);
    public static final Supplier<Item> HEROIN_25MG = registerHeroin("heroin_25mg", 25.0D, true);
    public static final Supplier<Item> HEROIN_100MG = registerHeroin("heroin_100mg", 100.0D, true);
    public static final Supplier<Item> HEROIN_250MG = registerHeroin("heroin_250mg", 250.0D, true);
    public static final Supplier<Item> HEROIN_500MG = registerHeroin("heroin_500mg", 500.0D, true);
    public static final Supplier<Item> HEROIN = HEROIN_100MG;
    public static final Supplier<Item> HEROIN_TAR_LEGACY = registerHeroin("heroin_tar", 100.0D, false);
    public static final Supplier<Item> MDMA_25MG = registerMdmaDose("mdma_25mg", 25.0D, false);
    public static final Supplier<Item> MDMA_100MG = registerMdmaDose("mdma_100mg", 100.0D, true);
    public static final Supplier<Item> MDMA_250MG = registerMdmaDose("mdma_250mg", 250.0D, false);
    public static final Supplier<Item> MDMA_500MG = registerMdmaDose("mdma_500mg", 500.0D, false);
    public static final Supplier<Item> MDMA_CRYSTAL = registerMdmaCrystal("mdma_crystal");
    public static final Supplier<Item> MDMA_CAPS_25MG = registerMdmaCapsule("mdma_caps_25mg", 25.0D);
    public static final Supplier<Item> MDMA_CAPS_100MG = registerMdmaCapsule("mdma_caps_100mg", 100.0D);
    public static final Supplier<Item> MDMA_CAPS_250MG = registerMdmaCapsule("mdma_caps_250mg", 250.0D);
    public static final Supplier<Item> MDMA_CAPS_500MG = registerMdmaCapsule("mdma_caps_500mg", 500.0D);
    public static final Supplier<Item> MDMA = MDMA_100MG;
    public static final Supplier<Item> KETAMINE = registerKetamineBottle("ketamine_vial", 1000, 1000);
    public static final Supplier<Item> KETAMINE_25MG = registerKetamineDose("ketamine_25mg", 25.0D);
    public static final Supplier<Item> KETAMINE_100MG = registerKetamineDose("ketamine_100mg", 100.0D);
    public static final Supplier<Item> KETAMINE_250MG = registerKetamineDose("ketamine_250mg", 250.0D);
    public static final Supplier<Item> KETAMINE_500MG = registerKetamineDose("ketamine_500mg", 500.0D);
    public static final Supplier<Item> MESCALINE_CACTUS = registerMescalineCactus();
    public static final Supplier<Item> MESCALINE_25MG = registerMescalineDose("mescaline_25mg", 25.0D, false, true);
    public static final Supplier<Item> MESCALINE_100MG = registerMescalineDose("mescaline_100mg", 100.0D, true, true);
    public static final Supplier<Item> MESCALINE_250MG = registerMescalineDose("mescaline_250mg", 250.0D, false, true);
    public static final Supplier<Item> MESCALINE_500MG = registerMescalineDose("mescaline_500mg", 500.0D, false, true);
    public static final Supplier<Item> MESCALINE = MESCALINE_100MG;
    // Retain the old id for existing worlds, but keep it out of the creative tab.
    public static final Supplier<Item> MESCALINE_BUTTON_LEGACY = registerMescalineDose("mescaline_button", 100.0D, false, false);
    public static final Supplier<Item> SALVIA_SEEDS = registerSalviaSeeds();
    public static final Supplier<Item> FRESH_SALVIA_LEAF = registerFlavorItem("fresh_salvia_leaf", "tooltip.trippycraft.fresh_salvia_leaf", 64);
    public static final Supplier<Item> DRIED_SALVIA_LEAF = registerFlavorItem("dried_salvia_leaf", "tooltip.trippycraft.dried_salvia_leaf", 64);
    public static final Supplier<Item> SALVIA_1X = registerSalvia("salvia_leaf", 1.0D);
    public static final Supplier<Item> SALVIA = SALVIA_1X;
    public static final Supplier<Item> SALVIA_10X = registerSalvia("salvia_leaf_10x", 5.0D);
    public static final Supplier<Item> SALVIA_25X = registerSalvia("salvia_leaf_25x", 10.0D);
    public static final Supplier<Item> SALVIA_50X = registerSalvia("salvia_leaf_50x", 15.0D);
    public static final Supplier<Item> BONG = registerBong("bong", 0.0D);
    public static final Supplier<Item> BONG_SALVIA_1X = registerBong("bong_salvia_1x", 1.0D);
    public static final Supplier<Item> BONG_SALVIA_10X = registerBong("bong_salvia_10x", 5.0D);
    public static final Supplier<Item> BONG_SALVIA_25X = registerBong("bong_salvia_25x", 10.0D);
    public static final Supplier<Item> BONG_SALVIA_50X = registerBong("bong_salvia_50x", 15.0D);
    public static final Supplier<Item> BONG_CANNABIS = registerCannabisBong();
    public static final Supplier<Item> BONG_DMT = registerLoadedBong("bong_dmt", BongItem.Contents.DMT, DmtPowderItem.DOSE_UNITS);
    public static final Supplier<Item> ANCIENT_PIPE = registerAncientPipe("ancient_pipe", BongItem.Contents.EMPTY, 0.0D);
    public static final Supplier<Item> ANCIENT_PIPE_SALVIA_1X = registerAncientPipe("ancient_pipe_salvia_1x", BongItem.Contents.SALVIA, 1.0D);
    public static final Supplier<Item> ANCIENT_PIPE_SALVIA_10X = registerAncientPipe("ancient_pipe_salvia_10x", BongItem.Contents.SALVIA, 5.0D);
    public static final Supplier<Item> ANCIENT_PIPE_SALVIA_25X = registerAncientPipe("ancient_pipe_salvia_25x", BongItem.Contents.SALVIA, 10.0D);
    public static final Supplier<Item> ANCIENT_PIPE_SALVIA_50X = registerAncientPipe("ancient_pipe_salvia_50x", BongItem.Contents.SALVIA, 15.0D);
    public static final Supplier<Item> ANCIENT_PIPE_CANNABIS = registerAncientPipe("ancient_pipe_cannabis", BongItem.Contents.CANNABIS, BongItem.CANNABIS_THC_MILLIGRAMS);
    public static final Supplier<Item> ANCIENT_PIPE_DMT = registerAncientPipe("ancient_pipe_dmt", BongItem.Contents.DMT, DmtPowderItem.DOSE_UNITS);
    public static final Supplier<Item> NITROUS = registerNitrousDose("nitrous_canister", 1.0D);
    public static final Supplier<Item> BEER_BOTTLE = registerAlcohol("beer_bottle", 12.0D, 0.05D, true, Items.GLASS_BOTTLE);
    public static final Supplier<Item> WINE_GLASS_5OZ = registerAlcohol("wine_glass_5oz", 5.0D, 0.14D, false);
    public static final Supplier<Item> WINE_GLASS_9OZ = registerAlcohol("wine_glass_9oz", 9.0D, 0.14D, false);
    public static final Supplier<Item> WHISKEY_GLASS = registerAlcohol("whiskey_glass", 1.5D, 0.40D, false);
    public static final Supplier<Item> RUM_GLASS = registerAlcohol("rum_glass", 1.5D, 0.40D, false);
    public static final Supplier<Item> VODKA_GLASS = registerAlcohol("vodka_glass", 1.5D, 0.40D, false);

    private ModItems() {
    }

    public static Map<SubstanceProfile, Supplier<Item>> substanceItems() {
        return SUBSTANCE_ITEMS;
    }

    public static List<Supplier<Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static Supplier<Item> registerSubstance(String itemId, SubstanceProfile profile) {
        return registerSubstance(itemId, profile, true);
    }

    private static Supplier<Item> registerSubstance(String itemId, SubstanceProfile profile, boolean addToCreative) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new PsychoactiveItem(profile, new Item.Properties()
                        .stacksTo(16)
                        ));
        SUBSTANCE_ITEMS.put(profile, item);
        if (addToCreative) {
            CREATIVE_ITEMS.add(item);
        }
        return item;
    }

    private static Supplier<Item> registerSimpleItem(String itemId) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new Item(new Item.Properties()
                        .stacksTo(64)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerEthanol(String itemId) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new EthanolItem(new Item.Properties()
                        .stacksTo(64)
                        .craftRemainder(Items.GLASS_BOTTLE)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerBlendCapsule(String itemId) {
        return ITEMS.register(itemId,
                () -> new BlendCapsuleItem(new Item.Properties()
                        .stacksTo(16)
                        ));
    }

    private static Supplier<Item> registerBlockItem(String itemId, Supplier<Block> block) {
        return registerBlockItem(itemId, block, true);
    }

    private static Supplier<Item> registerBlockItem(String itemId, Supplier<Block> block, boolean addToCreative) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new BlockItem(block.get(), new Item.Properties()
                        .stacksTo(64)));
        if (addToCreative) {
            CREATIVE_ITEMS.add(item);
        }
        return item;
    }

    private static Supplier<Item> registerCodex(String itemId) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new CursedRecipeCodexItem(new Item.Properties()
                        .stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerFlavorBlockItem(String itemId, Supplier<Block> block, String tooltipKey) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new FlavorBlockItem(
                        block.get(),
                        tooltipKey,
                        new Item.Properties()
                                .stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerFlavorItem(String itemId, String tooltipKey, int stackSize) {
        return registerFlavorItem(itemId, tooltipKey, stackSize, null);
    }

    private static Supplier<Item> registerFlavorItem(String itemId, String tooltipKey, int stackSize, Item craftingRemainder) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new FlavorItem(
                        tooltipKey,
                        createFlavorProperties(itemId, stackSize, craftingRemainder)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Item.Properties createFlavorProperties(String itemId, int stackSize, Item craftingRemainder) {
        Item.Properties properties = new Item.Properties()
                .stacksTo(stackSize);
        if (craftingRemainder != null) {
            properties = properties.craftRemainder(craftingRemainder);
        }
        return properties;
    }

    private static Supplier<Item> registerMortarAndPestleBlockItem() {
        Supplier<Item> item = ITEMS.register("mortar_and_pestle",
                () -> new MortarAndPestleBlockItem(
                        ModBlocks.MORTAR_AND_PESTLE.get(),
                        "tooltip.trippycraft.mortar_and_pestle",
                        new Item.Properties()
                                .stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerCannabisSeeds() {
        Supplier<Item> item = ITEMS.register("cannabis_seeds",
                () -> new CannabisSeedsItem(
                        ModBlocks.CANNABIS_PLANT.get(),
                        new Item.Properties()
                                .stacksTo(64)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerKokaSeeds() {
        Supplier<Item> item = ITEMS.register("koka_seeds",
                () -> new KokaSeedsItem(
                        ModBlocks.KOKA_PLANT.get(),
                        new Item.Properties()
                                .stacksTo(64)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerOpiumPoppySeeds() {
        Supplier<Item> item = ITEMS.register("opium_poppy_seeds",
                () -> new OpiumPoppySeedsItem(
                        ModBlocks.OPIUM_POPPY.get(),
                        new Item.Properties()
                                .stacksTo(64)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerCocaine(String itemId, CocaineRoute route, double doseGrams, boolean defaultProfileItem) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new CocaineItem(route, doseGrams, new Item.Properties()
                        .stacksTo(16)
                        ));
        SubstanceProfile profile = route == CocaineRoute.CRACK ? SubstanceProfiles.CRACK_COCAINE : SubstanceProfiles.COCAINE_POWDER;
        SUBSTANCE_ITEMS.put(profile, item);
        if (defaultProfileItem) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.COCAINE, item);
        }
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerKokaDoseItem(String itemId, double doseGrams) {
        return registerKokaDoseItem(itemId, doseGrams, 16);
    }

    private static Supplier<Item> registerKokaDoseItem(String itemId, double doseGrams, int stackSize) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new KokaDoseItem(doseGrams, new Item.Properties()
                        .stacksTo(stackSize)
                        ));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerKetamineBottle(String itemId, int totalMilligrams, int milligramsPerUse) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new KetamineBottleItem(totalMilligrams, milligramsPerUse, new Item.Properties()
                        .stacksTo(16)
                        .craftRemainder(Items.GLASS_BOTTLE)
                        ));
        SUBSTANCE_ITEMS.put(SubstanceProfiles.KETAMINE, item);
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerKetamineDose(String itemId, double doseMilligrams) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new KetamineDoseItem(doseMilligrams, new Item.Properties()
                        .stacksTo(64)
                        ));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerMdmaDose(String itemId, double doseMilligrams, boolean defaultProfileItem) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new MdmaDoseItem(doseMilligrams, new Item.Properties()
                        .stacksTo(16)
                        ));
        if (defaultProfileItem) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.MDMA, item);
        }
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerTwoCbDose(String itemId, double doseMilligrams, boolean defaultProfileItem) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new TwoCbDoseItem(doseMilligrams, new Item.Properties()
                        .stacksTo(16)
                        ));
        if (defaultProfileItem) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.TWO_CB, item);
        }
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerTwoCbCrystal() {
        Supplier<Item> item = ITEMS.register("two_cb_crystal",
                () -> new TwoCbCrystalItem(new Item.Properties().stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerHoMetDose(String itemId, int amountMilligrams, double doseMilligrams, boolean defaultProfileItem, boolean addToCreative) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new HoMetDoseItem(amountMilligrams, doseMilligrams, new Item.Properties()
                        .stacksTo(16)
                        .craftRemainder(Items.GLASS_BOTTLE)));
        if (defaultProfileItem) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.HO_MET, item);
        }
        if (addToCreative) {
            CREATIVE_ITEMS.add(item);
        }
        return item;
    }

    private static Supplier<Item> registerHoMetCrystal() {
        Supplier<Item> item = ITEMS.register("four_ho_met_crystal",
                () -> new HoMetCrystalItem(new Item.Properties().stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerHoMetPowder() {
        Supplier<Item> item = ITEMS.register("four_ho_met_powder",
                () -> new HoMetPowderItem(new Item.Properties().stacksTo(64)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerAlLadDose(String itemId, int amountMilligrams, double doseMicrograms, boolean defaultProfileItem, boolean addToCreative) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new AlLadDoseItem(amountMilligrams, doseMicrograms, new Item.Properties().stacksTo(64)));
        if (defaultProfileItem) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.AL_LAD, item);
        }
        if (addToCreative) {
            CREATIVE_ITEMS.add(item);
        }
        return item;
    }

    private static Supplier<Item> registerAlLadConcentrate() {
        Supplier<Item> item = ITEMS.register("al_lad_concentrate",
                () -> new AlLadConcentrateItem(new Item.Properties().stacksTo(1).craftRemainder(Items.GLASS_BOTTLE)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerTwoCeCrystal() {
        Supplier<Item> item = ITEMS.register("two_ce_crystal",
                () -> new TwoCeCrystalItem(new Item.Properties().stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerTwoCeDose(String itemId, int contentMilligrams, double doseMilligrams, boolean defaultProfileItem, boolean addToCreative) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new TwoCeDoseItem(contentMilligrams, doseMilligrams, new Item.Properties().stacksTo(16)));
        if (defaultProfileItem) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.TWO_CE, item);
        }
        if (addToCreative) {
            CREATIVE_ITEMS.add(item);
        }
        return item;
    }

    private static Supplier<Item> registerNitrousDose(String itemId, double hits) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new NitrousDoseItem(hits, new Item.Properties().stacksTo(16)));
        SUBSTANCE_ITEMS.put(SubstanceProfiles.NITROUS, item);
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerMescalineCactus() {
        Supplier<Item> item = ITEMS.register("mescaline_cactus",
                () -> new FlavorBlockItem(ModBlocks.MESCALINE_CACTUS.get(), "tooltip.trippycraft.mescaline_cactus",
                        new Item.Properties().stacksTo(64)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerMescalineDose(String itemId, double doseMilligrams, boolean defaultProfileItem, boolean addToCreative) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new MescalineDoseItem(doseMilligrams, new Item.Properties().stacksTo(16)));
        if (defaultProfileItem) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.MESCALINE, item);
        }
        if (addToCreative) {
            CREATIVE_ITEMS.add(item);
        }
        return item;
    }

    private static Supplier<Item> registerMdmaCrystal(String itemId) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new MdmaCrystalItem(new Item.Properties()
                        .stacksTo(16)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerMdmaCapsule(String itemId, double doseMilligrams) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new MdmaDoseItem(doseMilligrams, new Item.Properties()
                        .stacksTo(64)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerHeroin(String itemId, double doseMilligrams, boolean addToCreative) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new HeroinItem(doseMilligrams, new Item.Properties()
                        .stacksTo(16)
                        ));
        if (addToCreative && Math.round(doseMilligrams) == 100L) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.HEROIN, item);
        }
        if (addToCreative) {
            CREATIVE_ITEMS.add(item);
        }
        return item;
    }

    private static Supplier<Item> registerPsilocybinCapsule(String itemId, double doseGrams) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new PsilocybinCapsuleItem(doseGrams, new Item.Properties()
                        .stacksTo(16)
                        ));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerCannabisDoseItem(String itemId, int totalThcMilligrams, int thcMilligramsPerHit, boolean defaultProfileItem) {
        return registerCannabisDoseItem(
            itemId,
            totalThcMilligrams,
            thcMilligramsPerHit,
            defaultProfileItem,
            "tooltip.trippycraft.cannabis_hit_mg",
            "tooltip.trippycraft.cannabis_stacks");
        }

        private static Supplier<Item> registerCannabisDoseItem(String itemId,
                                     int totalThcMilligrams,
                                     int thcMilligramsPerHit,
                                     boolean defaultProfileItem,
                                     String perUseTooltipKey,
                                     String stackingTooltipKey) {
        Supplier<Item> item = ITEMS.register(itemId,
            () -> new CannabisDoseItem(totalThcMilligrams, thcMilligramsPerHit, perUseTooltipKey, stackingTooltipKey, new Item.Properties()
                        .stacksTo(16)
                        ));
        if (defaultProfileItem) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.CANNABIS, item);
        }
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerLsdBlotter(String itemId, double doseMicrograms) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new LsdBlotterItem(doseMicrograms, new Item.Properties()
                        .stacksTo(64)
                        ));
        if (Math.round(doseMicrograms) == 100L) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.LSD, item);
        }
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerLsdSolution(String itemId, double doseMicrograms) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new LsdSolutionItem(doseMicrograms, new Item.Properties()
                        .stacksTo(16)
                        .craftRemainder(Items.GLASS_BOTTLE)
                        ));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerDmt(String itemId, double doseUnits) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new DmtItem(doseUnits, new Item.Properties()
                        .stacksTo(16)
                        ));
        SUBSTANCE_ITEMS.putIfAbsent(SubstanceProfiles.DMT, item);
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerSalviaSeeds() {
        Supplier<Item> item = ITEMS.register("salvia_seeds",
                () -> new SalviaSeedsItem(ModBlocks.SALVIA_DIVINORUM_PLANT.get(), new Item.Properties().stacksTo(64)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerDmtPowder() {
        Supplier<Item> item = ITEMS.register("dmt_powder", () -> new DmtPowderItem(new Item.Properties().stacksTo(64)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerBong(String itemId, double doseUnits) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new BongItem(doseUnits, new Item.Properties().stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerCannabisBong() {
        Supplier<Item> item = ITEMS.register("bong_cannabis",
                () -> new BongItem(BongItem.Contents.CANNABIS, BongItem.CANNABIS_THC_MILLIGRAMS,
                        new Item.Properties().stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerLoadedBong(String itemId, BongItem.Contents contents, double doseUnits) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new BongItem(contents, doseUnits, new Item.Properties().stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerAncientPipe(String itemId, BongItem.Contents contents, double doseUnits) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new AncientPipeItem(contents, doseUnits, new Item.Properties().stacksTo(1)));
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerSalvia(String itemId, double doseUnits) {
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new SalviaLeafItem(doseUnits, new Item.Properties()
                        .stacksTo(16)
                        ));
        SUBSTANCE_ITEMS.put(SubstanceProfiles.SALVIA, item);
        CREATIVE_ITEMS.add(item);
        return item;
    }

    private static Supplier<Item> registerAlcohol(String itemId, double servingOunces, double alcoholByVolume, boolean defaultProfileItem) {
        return registerAlcohol(itemId, servingOunces, alcoholByVolume, defaultProfileItem, null);
    }

    private static Supplier<Item> registerAlcohol(String itemId,
                                                        double servingOunces,
                                                        double alcoholByVolume,
                                                        boolean defaultProfileItem,
                                                        @Nullable Item craftingRemainder) {
        Item.Properties properties = new Item.Properties()
                .stacksTo(16)
                ;
        if (craftingRemainder != null) {
            properties = properties.craftRemainder(craftingRemainder);
        }
        final Item.Properties finalProperties = properties;
        Supplier<Item> item = ITEMS.register(itemId,
                () -> new AlcoholItem(servingOunces, alcoholByVolume, finalProperties));
        if (defaultProfileItem) {
            SUBSTANCE_ITEMS.put(SubstanceProfiles.ALCOHOL, item);
        }
        CREATIVE_ITEMS.add(item);
        return item;
    }
}
