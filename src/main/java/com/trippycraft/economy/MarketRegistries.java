package com.trippycraft.economy;

import com.trippycraft.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class MarketRegistries {
    public static final String CATEGORY_CANNABIS = "Cannabis";
    public static final String CATEGORY_PSILOCYBIN = "Psilocybin";
    public static final String CATEGORY_COCAINE = "Cocaine";
    public static final String CATEGORY_HEROIN = "Heroin";
    public static final String CATEGORY_LSD = "LSD";
    public static final String CATEGORY_DMT = "DMT";
    public static final String CATEGORY_KETAMINE = "Ketamine";
    public static final String CATEGORY_MDMA = "MDMA";
    public static final String CATEGORY_TWO_CB = "2C-B";
    public static final String CATEGORY_HO_MET = "4-HO-MET";
    public static final String CATEGORY_AL_LAD = "AL-LAD";
    public static final String CATEGORY_TWO_CE = "2C-E";
    public static final String CATEGORY_MESCALINE = "Mescaline";
    public static final String CATEGORY_SALVIA = "Salvia";
    public static final String CATEGORY_NITROUS = "Nitrous";
    public static final String CATEGORY_ALCOHOL = "Alcohol";

    private static final Map<String, SellableMarketProduct> SELLABLES = new LinkedHashMap<>();
    private static final Map<String, BuyableMarketMaterial> MATERIALS = new LinkedHashMap<>();
    private static final List<String> SELLABLE_CATEGORIES = new ArrayList<>();
    private static final List<String> MATERIAL_CATEGORIES = new ArrayList<>();

    static {
        registerSellables();
        registerMaterials();
    }

    private MarketRegistries() {
    }

    public static List<SellableMarketProduct> sellableProducts() {
        return List.copyOf(SELLABLES.values());
    }

    public static Optional<SellableMarketProduct> sellableProduct(String id) {
        return Optional.ofNullable(SELLABLES.get(id));
    }

    public static List<BuyableMarketMaterial> buyableMaterials() {
        return List.copyOf(MATERIALS.values());
    }

    public static Optional<BuyableMarketMaterial> buyableMaterial(String id) {
        return Optional.ofNullable(MATERIALS.get(id));
    }

    public static List<String> sellableCategories() {
        return List.copyOf(SELLABLE_CATEGORIES);
    }

    public static List<String> materialCategories() {
        return List.copyOf(MATERIAL_CATEGORIES);
    }

    public static List<SellableMarketProduct> contractEligibleProducts(int reputationLevel) {
        return SELLABLES.values().stream()
                .filter(SellableMarketProduct::contractEligible)
                .sorted(Comparator.comparingInt(SellableMarketProduct::demandWeight).reversed())
                .toList();
    }

    private static void registerSellables() {
        product("cannabis_bud", ModItems.CANNABIS_BUD::get, CATEGORY_CANNABIS, 1, true, 95, 0);
        product("dried_cannabis_bud", ModItems.DRIED_CANNABIS_BUD::get, CATEGORY_CANNABIS, 2, true, 90, 0);
        product("thc_extract", ModItems.THC_EXTRACT::get, CATEGORY_CANNABIS, 6, true, 75, 0);
        product("packed_thc_extract", ModItems.PACKED_THC_EXTRACT::get, CATEGORY_CANNABIS, 36, true, 55, 1);
        product("cannabis_joint", ModItems.CANNABIS_JOINT::get, CATEGORY_CANNABIS, 8, true, 80, 0);
        product("cannabis_dab_pen", ModItems.CANNABIS_DAB_PEN::get, CATEGORY_CANNABIS, 28, true, 55, 2);
        product("cannabis_gummies", ModItems.CANNABIS_GUMMIES::get, CATEGORY_CANNABIS, 22, true, 60, 1);
        product("cannabis_chocolate_bar", ModItems.CANNABIS_CHOCOLATE_BAR::get, CATEGORY_CANNABIS, 18, true, 60, 1);
        product("bong_cannabis", ModItems.BONG_CANNABIS::get, CATEGORY_CANNABIS, 4, false, 55, 0);
        product("ancient_pipe_cannabis", ModItems.ANCIENT_PIPE_CANNABIS::get, CATEGORY_CANNABIS, 4, false, 55, 0);

        product("dried_strange_mushroom", ModItems.DRIED_STRANGE_MUSHROOM::get, CATEGORY_PSILOCYBIN, 2, true, 90, 0);
        product("psilocybin_powder", ModItems.PSILOCYBIN_POWDER::get, CATEGORY_PSILOCYBIN, 4, true, 80, 0);
        product("packed_psilocybin_powder", ModItems.PACKED_PSILOCYBIN_POWDER::get, CATEGORY_PSILOCYBIN, 24, true, 55, 1);
        product("psilocybin_capsule_0_5g", ModItems.PSILOCYBIN_CAPSULE_05G::get, CATEGORY_PSILOCYBIN, 5, true, 80, 0);
        product("psilocybin_capsule_1g", ModItems.PSILOCYBIN_CAPSULE_1G::get, CATEGORY_PSILOCYBIN, 9, true, 80, 0);
        product("psilocybin_capsule_3g", ModItems.PSILOCYBIN_CAPSULE_3G::get, CATEGORY_PSILOCYBIN, 18, true, 65, 1);
        product("psilocybin_capsule_5g", ModItems.PSILOCYBIN_CAPSULE_5G::get, CATEGORY_PSILOCYBIN, 28, true, 55, 2);

        product("koka_leaves", ModItems.KOKA_LEAVES::get, CATEGORY_COCAINE, 1, true, 95, 0);
        product("dried_koka_leaves", ModItems.DRIED_KOKA_LEAVES::get, CATEGORY_COCAINE, 1, true, 90, 0);
        product("ground_koka_leaves", ModItems.GROUND_KOKA_LEAVES::get, CATEGORY_COCAINE, 2, true, 85, 0);
        product("koka_paste", ModItems.KOKA_PASTE::get, CATEGORY_COCAINE, 5, true, 75, 0);
        product("hardened_koka_paste", ModItems.HARDENED_KOKA_PASTE::get, CATEGORY_COCAINE, 8, true, 65, 1);
        product("cocaine_powder", ModItems.COCAINE_POWDER::get, CATEGORY_COCAINE, 16, true, 70, 1);
        product("crack_cocaine", ModItems.CRACK_COCAINE::get, CATEGORY_COCAINE, 24, true, 50, 2);

        product("opium_latex", ModItems.OPIUM_LATEX::get, CATEGORY_HEROIN, 2, true, 90, 0);
        product("raw_opium", ModItems.RAW_OPIUM::get, CATEGORY_HEROIN, 5, true, 80, 0);
        product("morphine_base", ModItems.MORPHINE_BASE::get, CATEGORY_HEROIN, 10, true, 70, 1);
        product("heroin_base", ModItems.HEROIN_BASE::get, CATEGORY_HEROIN, 16, true, 65, 1);
        product("heroin_25mg", ModItems.HEROIN_25MG::get, CATEGORY_HEROIN, 8, true, 75, 0);
        product("heroin_100mg", ModItems.HEROIN_100MG::get, CATEGORY_HEROIN, 18, true, 70, 1);
        product("heroin_250mg", ModItems.HEROIN_250MG::get, CATEGORY_HEROIN, 30, true, 55, 2);
        product("heroin_500mg", ModItems.HEROIN_500MG::get, CATEGORY_HEROIN, 46, true, 45, 3);

        product("ergot_clump", ModItems.ERGOT_CLUMP::get, CATEGORY_LSD, 4, true, 85, 0);
        product("lysergic_extract", ModItems.LYSERGIC_EXTRACT::get, CATEGORY_LSD, 10, true, 70, 1);
        product("lsd_solution", ModItems.LSD_SOLUTION::get, CATEGORY_LSD, 32, true, 55, 2);
        product("lsd_blotter_50ug", ModItems.LSD_BLOTTER_50UG::get, CATEGORY_LSD, 6, true, 90, 0);
        product("lsd_blotter_100ug", ModItems.LSD_BLOTTER_100UG::get, CATEGORY_LSD, 12, true, 80, 0);
        product("lsd_blotter_250ug", ModItems.LSD_BLOTTER_250UG::get, CATEGORY_LSD, 25, true, 60, 1);
        product("lsd_blotter_500ug", ModItems.LSD_BLOTTER_500UG::get, CATEGORY_LSD, 42, true, 45, 2);

        product("acacia_bark", ModItems.ACACIA_BARK::get, CATEGORY_DMT, 1, true, 90, 0);
        product("acacia_leaf", ModItems.ACACIA_LEAF::get, CATEGORY_DMT, 1, true, 85, 0);
        product("plant_extract", ModItems.PLANT_EXTRACT::get, CATEGORY_DMT, 5, true, 80, 0);
        product("bundled_plant_extract", ModItems.BUNDLED_PLANT_EXTRACT::get, CATEGORY_DMT, 24, true, 60, 1);
        product("dmt_crystal", ModItems.DMT::get, CATEGORY_DMT, 18, true, 70, 1);
        product("dmt_crystal_5x", ModItems.DMT_5X::get, CATEGORY_DMT, 48, true, 45, 3);
        product("dmt_powder", ModItems.DMT_POWDER::get, CATEGORY_DMT, 9, true, 70, 1);
        product("bong_dmt", ModItems.BONG_DMT::get, CATEGORY_DMT, 11, false, 50, 1);
        product("ancient_pipe_dmt", ModItems.ANCIENT_PIPE_DMT::get, CATEGORY_DMT, 11, false, 50, 1);

        product("medicinal_reagent", ModItems.MEDICINAL_REAGENT::get, CATEGORY_KETAMINE, 7, true, 75, 0);
        product("ketamine_precursor", ModItems.KETAMINE_PRECURSOR::get, CATEGORY_KETAMINE, 12, true, 70, 1);
        product("ketamine_vial", ModItems.KETAMINE::get, CATEGORY_KETAMINE, 30, true, 55, 2);
        product("ketamine_25mg", ModItems.KETAMINE_25MG::get, CATEGORY_KETAMINE, 7, true, 75, 0);
        product("ketamine_100mg", ModItems.KETAMINE_100MG::get, CATEGORY_KETAMINE, 14, true, 70, 1);
        product("ketamine_250mg", ModItems.KETAMINE_250MG::get, CATEGORY_KETAMINE, 24, true, 60, 2);
        product("ketamine_500mg", ModItems.KETAMINE_500MG::get, CATEGORY_KETAMINE, 38, true, 50, 3);

        product("mdma_25mg", ModItems.MDMA_25MG::get, CATEGORY_MDMA, 6, true, 78, 0);
        product("mdma_100mg", ModItems.MDMA_100MG::get, CATEGORY_MDMA, 13, true, 72, 1);
        product("mdma_250mg", ModItems.MDMA_250MG::get, CATEGORY_MDMA, 24, true, 58, 2);
        product("mdma_500mg", ModItems.MDMA_500MG::get, CATEGORY_MDMA, 38, true, 46, 3);
        product("mdma_caps_25mg", ModItems.MDMA_CAPS_25MG::get, CATEGORY_MDMA, 6, true, 78, 0);
        product("mdma_caps_100mg", ModItems.MDMA_CAPS_100MG::get, CATEGORY_MDMA, 13, true, 72, 1);
        product("mdma_caps_250mg", ModItems.MDMA_CAPS_250MG::get, CATEGORY_MDMA, 24, true, 58, 2);
        product("mdma_caps_500mg", ModItems.MDMA_CAPS_500MG::get, CATEGORY_MDMA, 38, true, 46, 3);
        product("mdma_crystal", ModItems.MDMA_CRYSTAL::get, CATEGORY_MDMA, 300, false, 30, 3);

        product("two_cb_25mg", ModItems.TWO_CB_25MG::get, CATEGORY_TWO_CB, 7, true, 78, 0);
        product("two_cb_100mg", ModItems.TWO_CB_100MG::get, CATEGORY_TWO_CB, 15, true, 72, 1);
        product("two_cb_250mg", ModItems.TWO_CB_250MG::get, CATEGORY_TWO_CB, 27, true, 58, 2);
        product("two_cb_500mg", ModItems.TWO_CB_500MG::get, CATEGORY_TWO_CB, 43, true, 46, 3);
        product("two_cb_powder", ModItems.TWO_CB_POWDER::get, CATEGORY_TWO_CB, 12, true, 65, 1);
        product("two_cb_crystal", ModItems.TWO_CB_CRYSTAL::get, CATEGORY_TWO_CB, 300, false, 30, 3);

        product("four_ho_met_25mg", ModItems.HO_MET_25MG::get, CATEGORY_HO_MET, 7, true, 78, 0);
        product("four_ho_met_100mg", ModItems.HO_MET_100MG::get, CATEGORY_HO_MET, 13, true, 72, 1);
        product("four_ho_met_250mg", ModItems.HO_MET_250MG::get, CATEGORY_HO_MET, 24, true, 58, 2);
        product("four_ho_met_500mg", ModItems.HO_MET_500MG::get, CATEGORY_HO_MET, 39, true, 46, 3);
        product("four_ho_met_powder", ModItems.HO_MET_POWDER::get, CATEGORY_HO_MET, 10, true, 65, 1);
        product("four_ho_met_crystal", ModItems.HO_MET_CRYSTAL::get, CATEGORY_HO_MET, 250, false, 30, 3);

        product("al_lad_25mg", ModItems.AL_LAD_25MG::get, CATEGORY_AL_LAD, 8, true, 78, 0);
        product("al_lad_100mg", ModItems.AL_LAD_100MG::get, CATEGORY_AL_LAD, 15, true, 72, 1);
        product("al_lad_250mg", ModItems.AL_LAD_250MG::get, CATEGORY_AL_LAD, 27, true, 58, 2);
        product("al_lad_500mg", ModItems.AL_LAD_500MG::get, CATEGORY_AL_LAD, 41, true, 46, 3);
        product("al_lad_concentrate", ModItems.AL_LAD_CONCENTRATE::get, CATEGORY_AL_LAD, 280, false, 30, 3);

        product("two_ce_25mg", ModItems.TWO_CE_25MG::get, CATEGORY_TWO_CE, 8, true, 78, 0);
        product("two_ce_100mg", ModItems.TWO_CE_100MG::get, CATEGORY_TWO_CE, 16, true, 72, 1);
        product("two_ce_250mg", ModItems.TWO_CE_250MG::get, CATEGORY_TWO_CE, 29, true, 58, 2);
        product("two_ce_500mg", ModItems.TWO_CE_500MG::get, CATEGORY_TWO_CE, 44, true, 46, 3);
        product("two_ce_powder", ModItems.TWO_CE_POWDER::get, CATEGORY_TWO_CE, 13, true, 65, 1);
        product("two_ce_crystal", ModItems.TWO_CE_CRYSTAL::get, CATEGORY_TWO_CE, 300, false, 30, 3);

        product("mescaline_25mg", ModItems.MESCALINE_25MG::get, CATEGORY_MESCALINE, 7, true, 78, 0);
        product("mescaline_100mg", ModItems.MESCALINE_100MG::get, CATEGORY_MESCALINE, 14, true, 72, 1);
        product("mescaline_250mg", ModItems.MESCALINE_250MG::get, CATEGORY_MESCALINE, 25, true, 58, 2);
        product("mescaline_500mg", ModItems.MESCALINE_500MG::get, CATEGORY_MESCALINE, 40, true, 46, 3);

        product("salvia_leaf", ModItems.SALVIA_1X::get, CATEGORY_SALVIA, 4, true, 85, 0);
        product("salvia_leaf_10x", ModItems.SALVIA_10X::get, CATEGORY_SALVIA, 12, true, 70, 1);
        product("salvia_leaf_25x", ModItems.SALVIA_25X::get, CATEGORY_SALVIA, 24, true, 55, 2);
        product("salvia_leaf_50x", ModItems.SALVIA_50X::get, CATEGORY_SALVIA, 40, true, 40, 3);
        product("bong_salvia_1x", ModItems.BONG_SALVIA_1X::get, CATEGORY_SALVIA, 6, false, 60, 0);
        product("bong_salvia_10x", ModItems.BONG_SALVIA_10X::get, CATEGORY_SALVIA, 14, false, 50, 1);
        product("bong_salvia_25x", ModItems.BONG_SALVIA_25X::get, CATEGORY_SALVIA, 26, false, 40, 2);
        product("bong_salvia_50x", ModItems.BONG_SALVIA_50X::get, CATEGORY_SALVIA, 42, false, 30, 3);
        product("ancient_pipe_salvia_1x", ModItems.ANCIENT_PIPE_SALVIA_1X::get, CATEGORY_SALVIA, 6, false, 60, 0);
        product("ancient_pipe_salvia_10x", ModItems.ANCIENT_PIPE_SALVIA_10X::get, CATEGORY_SALVIA, 14, false, 50, 1);
        product("ancient_pipe_salvia_25x", ModItems.ANCIENT_PIPE_SALVIA_25X::get, CATEGORY_SALVIA, 26, false, 40, 2);
        product("ancient_pipe_salvia_50x", ModItems.ANCIENT_PIPE_SALVIA_50X::get, CATEGORY_SALVIA, 42, false, 30, 3);

        product("nitrous_canister", ModItems.NITROUS::get, CATEGORY_NITROUS, 8, true, 80, 0);

        product("beer_bottle", ModItems.BEER_BOTTLE::get, CATEGORY_ALCOHOL, 3, false, 60, 0);
        product("wine_glass_5oz", ModItems.WINE_GLASS_5OZ::get, CATEGORY_ALCOHOL, 5, false, 50, 0);
        product("wine_glass_9oz", ModItems.WINE_GLASS_9OZ::get, CATEGORY_ALCOHOL, 8, false, 45, 1);
        product("whiskey_glass", ModItems.WHISKEY_GLASS::get, CATEGORY_ALCOHOL, 9, false, 45, 1);
        product("rum_glass", ModItems.RUM_GLASS::get, CATEGORY_ALCOHOL, 9, false, 45, 1);
        product("vodka_glass", ModItems.VODKA_GLASS::get, CATEGORY_ALCOHOL, 9, false, 45, 1);
        product("ethanol", ModItems.ETHANOL::get, CATEGORY_ALCOHOL, 12, false, 40, 2);
    }

    private static void registerMaterials() {
        material("oak_log", () -> Items.OAK_LOG, "Wood", 1, 0);
        material("spruce_log", () -> Items.SPRUCE_LOG, "Wood", 1, 0);
        material("birch_log", () -> Items.BIRCH_LOG, "Wood", 1, 0);
        material("jungle_log", () -> Items.JUNGLE_LOG, "Wood", 1, 0);
        material("acacia_log", () -> Items.ACACIA_LOG, "Wood", 1, 0);
        material("dark_oak_log", () -> Items.DARK_OAK_LOG, "Wood", 1, 0);
        material("mangrove_log", () -> Items.MANGROVE_LOG, "Wood", 1, 1);
        material("cherry_log", () -> Items.CHERRY_LOG, "Wood", 1, 1);
        material("bamboo_block", () -> Items.BAMBOO_BLOCK, "Wood", 1, 1);
        material("oak_planks", () -> Items.OAK_PLANKS, "Wood", 1, 0);
        material("spruce_planks", () -> Items.SPRUCE_PLANKS, "Wood", 1, 0);
        material("birch_planks", () -> Items.BIRCH_PLANKS, "Wood", 1, 0);
        material("jungle_planks", () -> Items.JUNGLE_PLANKS, "Wood", 1, 0);
        material("acacia_planks", () -> Items.ACACIA_PLANKS, "Wood", 1, 0);
        material("dark_oak_planks", () -> Items.DARK_OAK_PLANKS, "Wood", 1, 0);

        material("stone", () -> Items.STONE, "Stone", 1, 0);
        material("cobblestone", () -> Items.COBBLESTONE, "Stone", 1, 0);
        material("smooth_stone", () -> Items.SMOOTH_STONE, "Stone", 1, 0);
        material("stone_bricks", () -> Items.STONE_BRICKS, "Stone", 1, 0);
        material("deepslate", () -> Items.DEEPSLATE, "Stone", 1, 0);
        material("cobbled_deepslate", () -> Items.COBBLED_DEEPSLATE, "Stone", 1, 0);
        material("polished_deepslate", () -> Items.POLISHED_DEEPSLATE, "Stone", 1, 1);
        material("tuff", () -> Items.TUFF, "Stone", 1, 0);
        material("calcite", () -> Items.CALCITE, "Stone", 2, 1);
        material("dripstone_block", () -> Items.DRIPSTONE_BLOCK, "Stone", 1, 1);

        material("bricks", () -> Items.BRICKS, "Bricks", 2, 0);
        material("mud_bricks", () -> Items.MUD_BRICKS, "Bricks", 2, 1);
        material("nether_bricks", () -> Items.NETHER_BRICKS, "Bricks", 2, 2);
        material("red_nether_bricks", () -> Items.RED_NETHER_BRICKS, "Bricks", 3, 3);
        material("quartz_block", () -> Items.QUARTZ_BLOCK, "Bricks", 4, 2);
        material("quartz_bricks", () -> Items.QUARTZ_BRICKS, "Bricks", 4, 3);

        material("glass", () -> Items.GLASS, "Glass", 1, 0);
        material("tinted_glass", () -> Items.TINTED_GLASS, "Glass", 3, 2);
        material("white_stained_glass", () -> Items.WHITE_STAINED_GLASS, "Glass", 1, 0);
        material("gray_stained_glass", () -> Items.GRAY_STAINED_GLASS, "Glass", 1, 0);
        material("black_stained_glass", () -> Items.BLACK_STAINED_GLASS, "Glass", 1, 0);
        material("lime_stained_glass", () -> Items.LIME_STAINED_GLASS, "Glass", 1, 1);
        material("purple_stained_glass", () -> Items.PURPLE_STAINED_GLASS, "Glass", 1, 1);

        material("iron_block", () -> Items.IRON_BLOCK, "Metal", 9, 1);
        material("copper_block", () -> Items.COPPER_BLOCK, "Metal", 4, 0);
        material("cut_copper", () -> Items.CUT_COPPER, "Metal", 4, 1);
        material("exposed_copper", () -> Items.EXPOSED_COPPER, "Metal", 4, 2);
        material("weathered_copper", () -> Items.WEATHERED_COPPER, "Metal", 4, 2);
        material("oxidized_copper", () -> Items.OXIDIZED_COPPER, "Metal", 4, 3);
        material("chain", () -> Items.CHAIN, "Metal", 2, 1);

        material("torch", () -> Items.TORCH, "Lighting", 1, 0);
        material("lantern", () -> Items.LANTERN, "Lighting", 2, 0);
        material("soul_lantern", () -> Items.SOUL_LANTERN, "Lighting", 3, 1);
        material("glowstone", () -> Items.GLOWSTONE, "Lighting", 4, 2);
        material("sea_lantern", () -> Items.SEA_LANTERN, "Lighting", 5, 3);
        material("redstone_lamp", () -> Items.REDSTONE_LAMP, "Lighting", 4, 1);
        material("shroomlight", () -> Items.SHROOMLIGHT, "Lighting", 4, 2);

        material("redstone", () -> Items.REDSTONE, "Redstone", 1, 0);
        material("redstone_torch", () -> Items.REDSTONE_TORCH, "Redstone", 1, 0);
        material("repeater", () -> Items.REPEATER, "Redstone", 2, 0);
        material("comparator", () -> Items.COMPARATOR, "Redstone", 3, 1);
        material("piston", () -> Items.PISTON, "Redstone", 3, 1);
        material("sticky_piston", () -> Items.STICKY_PISTON, "Redstone", 4, 2);
        material("observer", () -> Items.OBSERVER, "Redstone", 4, 2);
        material("dispenser", () -> Items.DISPENSER, "Redstone", 3, 1);
        material("dropper", () -> Items.DROPPER, "Redstone", 2, 0);
        material("hopper", () -> Items.HOPPER, "Redstone", 5, 2);

        material("dirt", () -> Items.DIRT, "Farming", 1, 0);
        material("coarse_dirt", () -> Items.COARSE_DIRT, "Farming", 1, 0);
        material("podzol", () -> Items.PODZOL, "Farming", 1, 1);
        material("mud", () -> Items.MUD, "Farming", 1, 1);
        material("sand", () -> Items.SAND, "Farming", 1, 0);
        material("gravel", () -> Items.GRAVEL, "Farming", 1, 0);
        material("bone_meal", () -> Items.BONE_MEAL, "Farming", 1, 0);
        material("water_bucket", () -> Items.WATER_BUCKET, "Farming", 4, 0);
        material("composter", () -> Items.COMPOSTER, "Farming", 2, 0);

        material("barrel", () -> Items.BARREL, "Lab Decor", 2, 0);
        material("chest", () -> Items.CHEST, "Lab Decor", 2, 0);
        material("bookshelf", () -> Items.BOOKSHELF, "Lab Decor", 3, 1);
        material("cauldron", () -> Items.CAULDRON, "Lab Decor", 7, 1);
        material("brewing_stand", () -> Items.BREWING_STAND, "Lab Decor", 8, 2);
        material("iron_bars", () -> Items.IRON_BARS, "Lab Decor", 2, 1);
        material("chain_lab", () -> Items.CHAIN, "Lab Decor", 2, 1);
        material("blackstone", () -> Items.BLACKSTONE, "Lab Decor", 2, 2);
        material("polished_blackstone", () -> Items.POLISHED_BLACKSTONE, "Lab Decor", 2, 2);

        registerVanillaCreativeCatalog();
    }

    private static void registerVanillaCreativeCatalog() {
        ensureMaterialCategory("Vanilla Blocks");
        ensureMaterialCategory("Gear");
        ensureMaterialCategory("Food");
        ensureMaterialCategory("Rare / OP");
        ensureMaterialCategory("Spawn Eggs");
        ensureMaterialCategory("Misc");

        BuiltInRegistries.ITEM.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().location().getPath()))
                .forEach(entry -> {
                    ResourceLocation key = entry.getKey().location();
                    if (!key.getNamespace().equals("minecraft")) {
                        return;
                    }

                    String id = key.getPath();
                    Item item = entry.getValue();
                    if (item == Items.AIR || MATERIALS.containsKey(id)) {
                        return;
                    }

                    material(id, () -> item, categoryForVanillaItem(id, item), priceForVanillaItem(id, item), 0);
                });
    }

    private static void product(String id,
                                Supplier<net.minecraft.world.item.Item> item,
                                String category,
                                int baseEmeraldValue,
                                boolean contractEligible,
                                int demandWeight,
                                int reputationUnlockLevel) {
        SELLABLES.put(id, new SellableMarketProduct(
                id,
                item,
                category,
                Math.max(1, baseEmeraldValue),
                contractEligible,
                Math.max(1, demandWeight),
                Math.max(0, reputationUnlockLevel)
        ));
        if (!SELLABLE_CATEGORIES.contains(category)) {
            SELLABLE_CATEGORIES.add(category);
        }
    }

    private static void material(String id,
                                 Supplier<net.minecraft.world.item.Item> item,
                                 String category,
                                 int emeraldPricePerUnit,
                                 int reputationUnlockLevel) {
        MATERIALS.put(id, new BuyableMarketMaterial(
                id,
                item,
                category,
                Math.max(1, emeraldPricePerUnit),
                Math.max(0, reputationUnlockLevel)
        ));
        ensureMaterialCategory(category);
    }

    private static void ensureMaterialCategory(String category) {
        if (!MATERIAL_CATEGORIES.contains(category)) {
            MATERIAL_CATEGORIES.add(category);
        }
    }

    private static String categoryForVanillaItem(String id, Item item) {
        if (isOpItem(id) || isHighValueItem(id)) {
            return "Rare / OP";
        }
        if (id.endsWith("_spawn_egg")) {
            return "Spawn Eggs";
        }
        if (isGearItem(id)) {
            return "Gear";
        }
        if (isFoodItem(id)) {
            return "Food";
        }
        if (isWoodItem(id)) {
            return "Wood";
        }
        if (isStoneItem(id)) {
            return "Stone";
        }
        if (isBrickItem(id)) {
            return "Bricks";
        }
        if (isGlassItem(id)) {
            return "Glass";
        }
        if (isMetalItem(id)) {
            return "Metal";
        }
        if (isLightingItem(id)) {
            return "Lighting";
        }
        if (isRedstoneItem(id)) {
            return "Redstone";
        }
        if (isFarmingItem(id)) {
            return "Farming";
        }
        if (isDecorItem(id)) {
            return "Lab Decor";
        }
        if (item instanceof BlockItem) {
            return "Vanilla Blocks";
        }
        return "Misc";
    }

    private static int priceForVanillaItem(String id, Item item) {
        if (isOpItem(id)) {
            return 2_048;
        }
        if (id.equals("ender_dragon_spawn_egg")) {
            return 2_048;
        }
        if (id.endsWith("_spawn_egg")) {
            return spawnEggPrice(id);
        }
        if (id.equals("dragon_egg")) {
            return 2_048;
        }
        if (id.equals("elytra")) {
            return 1_024;
        }
        if (id.equals("nether_star") || id.equals("beacon")) {
            return 768;
        }
        if (id.equals("totem_of_undying") || id.equals("enchanted_golden_apple") || id.equals("conduit")) {
            return 512;
        }
        if (id.equals("end_crystal") || id.equals("heavy_core") || id.equals("trident")) {
            return 320;
        }
        if (id.equals("ominous_trial_key")) {
            return 256;
        }
        if (id.equals("trial_key") || id.equals("ominous_bottle") || id.equals("creaking_heart")) {
            return 192;
        }
        if (id.equals("enchanting_table") || id.equals("recovery_compass")) {
            return 128;
        }
        if (id.equals("heart_of_the_sea") || id.equals("breeze_rod") || id.equals("shulker_shell")) {
            return 96;
        }
        if (id.equals("experience_bottle") || id.equals("dragon_breath")) {
            return 64;
        }
        if (id.equals("ender_chest")) {
            return 48;
        }
        if (id.equals("shulker_box") || id.endsWith("_shulker_box")) {
            return 96;
        }
        if (id.equals("netherite_block")) {
            return 1_152;
        }
        if (id.equals("netherite_ingot")) {
            return 144;
        }
        if (id.equals("netherite_scrap")) {
            return 36;
        }
        if (id.startsWith("netherite_")) {
            return 256;
        }
        if (id.equals("diamond_block")) {
            return 288;
        }
        if (id.equals("diamond")) {
            return 32;
        }
        if (id.startsWith("diamond_")) {
            return 64;
        }
        if (id.equals("emerald_block")) {
            return 18;
        }
        if (id.equals("emerald")) {
            return 2;
        }
        if (id.equals("gold_block")) {
            return 54;
        }
        if (id.equals("gold_ingot")) {
            return 6;
        }
        if (id.startsWith("golden_")) {
            return id.equals("golden_apple") ? 64 : 18;
        }
        if (id.equals("iron_ingot")) {
            return 4;
        }
        if (id.startsWith("iron_")) {
            return item instanceof BlockItem ? 9 : 16;
        }
        if (id.equals("lapis_block") || id.equals("redstone_block") || id.equals("coal_block")) {
            return 9;
        }
        if (id.endsWith("_ore") || id.endsWith("_ores")) {
            return id.contains("diamond") || id.contains("emerald") ? 24 : id.contains("ancient_debris") ? 80 : 8;
        }
        if (isGearItem(id)) {
            return gearPrice(id);
        }
        if (isFoodItem(id)) {
            return foodPrice(id);
        }
        if (isRedstoneItem(id)) {
            return redstonePrice(id);
        }
        if (isLightingItem(id)) {
            return lightingPrice(id);
        }
        if (item instanceof BlockItem) {
            return blockPrice(id);
        }
        return miscPrice(id);
    }

    private static boolean isOpItem(String id) {
        return id.equals("bedrock")
                || id.equals("barrier")
                || id.equals("light")
                || id.equals("command_block")
                || id.equals("chain_command_block")
                || id.equals("repeating_command_block")
                || id.equals("structure_block")
                || id.equals("structure_void")
                || id.equals("jigsaw")
                || id.equals("debug_stick")
                || id.equals("knowledge_book")
                || id.equals("trial_spawner")
                || id.equals("vault")
                || id.equals("spawner")
                || id.equals("end_portal_frame")
                || id.equals("command_block_minecart");
    }

    private static boolean isHighValueItem(String id) {
        return id.contains("netherite")
                || id.contains("diamond")
                || id.equals("elytra")
                || id.equals("totem_of_undying")
                || id.equals("beacon")
                || id.equals("nether_star")
                || id.equals("dragon_egg")
                || id.equals("enchanted_golden_apple")
                || id.equals("end_crystal")
                || id.equals("conduit")
                || id.equals("heart_of_the_sea")
                || id.equals("trident")
                || id.equals("heavy_core")
                || id.equals("trial_key")
                || id.equals("ominous_trial_key")
                || id.equals("ominous_bottle")
                || id.equals("experience_bottle")
                || id.equals("dragon_breath")
                || id.equals("enchanting_table")
                || id.equals("recovery_compass")
                || id.equals("creaking_heart");
    }

    private static boolean isGearItem(String id) {
        return id.endsWith("_sword")
                || id.endsWith("_pickaxe")
                || id.endsWith("_axe")
                || id.endsWith("_shovel")
                || id.endsWith("_hoe")
                || id.endsWith("_helmet")
                || id.endsWith("_chestplate")
                || id.endsWith("_leggings")
                || id.endsWith("_boots")
                || id.equals("shield")
                || id.equals("bow")
                || id.equals("crossbow")
                || id.equals("mace")
                || id.equals("trident")
                || id.equals("fishing_rod")
                || id.equals("shears")
                || id.equals("flint_and_steel")
                || id.equals("brush")
                || id.equals("elytra");
    }

    private static boolean isFoodItem(String id) {
        return id.contains("apple")
                || id.contains("beef")
                || id.contains("porkchop")
                || id.contains("chicken")
                || id.contains("mutton")
                || id.contains("rabbit")
                || id.contains("cod")
                || id.contains("salmon")
                || id.contains("cookie")
                || id.contains("melon")
                || id.contains("pumpkin_pie")
                || id.contains("bread")
                || id.contains("potato")
                || id.contains("carrot")
                || id.contains("beetroot")
                || id.contains("berries")
                || id.contains("stew")
                || id.contains("soup")
                || id.equals("cake")
                || id.equals("honey_bottle")
                || id.equals("dried_kelp");
    }

    private static boolean isWoodItem(String id) {
        return id.contains("wood")
                || id.contains("log")
                || id.contains("planks")
                || id.contains("bamboo")
                || id.contains("stem")
                || id.contains("hyphae")
                || id.contains("stripped_")
                || id.contains("sapling")
                || id.contains("leaves")
                || id.contains("sign")
                || id.contains("hanging_sign")
                || id.contains("fence")
                || id.contains("boat");
    }

    private static boolean isStoneItem(String id) {
        return id.contains("stone")
                || id.contains("deepslate")
                || id.contains("granite")
                || id.contains("diorite")
                || id.contains("andesite")
                || id.contains("tuff")
                || id.contains("calcite")
                || id.contains("dripstone")
                || id.contains("slate")
                || id.equals("cobblestone")
                || id.equals("cobbled_deepslate")
                || id.equals("basalt")
                || id.equals("blackstone")
                || id.equals("netherrack")
                || id.equals("end_stone");
    }

    private static boolean isBrickItem(String id) {
        return id.contains("brick") || id.contains("quartz") || id.contains("purpur") || id.contains("prismarine");
    }

    private static boolean isGlassItem(String id) {
        return id.contains("glass");
    }

    private static boolean isMetalItem(String id) {
        return id.contains("iron")
                || id.contains("gold")
                || id.contains("copper")
                || id.contains("chain")
                || id.contains("anvil")
                || id.contains("rail")
                || id.contains("minecart")
                || id.contains("cauldron");
    }

    private static boolean isLightingItem(String id) {
        return id.contains("torch")
                || id.contains("lantern")
                || id.contains("light")
                || id.contains("glow")
                || id.contains("lamp")
                || id.contains("candle")
                || id.contains("froglight")
                || id.equals("sea_lantern")
                || id.equals("end_rod");
    }

    private static boolean isRedstoneItem(String id) {
        return id.contains("redstone")
                || id.contains("piston")
                || id.contains("observer")
                || id.contains("dispenser")
                || id.contains("dropper")
                || id.contains("hopper")
                || id.contains("comparator")
                || id.contains("repeater")
                || id.contains("sculk_sensor")
                || id.contains("crafter")
                || id.contains("daylight_detector")
                || id.contains("target")
                || id.contains("lever")
                || id.contains("button")
                || id.contains("pressure_plate")
                || id.contains("tripwire");
    }

    private static boolean isFarmingItem(String id) {
        return id.contains("seed")
                || id.contains("sapling")
                || id.contains("crop")
                || id.contains("dirt")
                || id.contains("sand")
                || id.contains("gravel")
                || id.contains("mud")
                || id.contains("podzol")
                || id.contains("nylium")
                || id.contains("moss")
                || id.contains("flower")
                || id.contains("mushroom")
                || id.contains("coral")
                || id.contains("kelp")
                || id.contains("vine")
                || id.contains("grass")
                || id.contains("fern")
                || id.contains("azalea")
                || id.contains("cactus")
                || id.contains("sugar_cane")
                || id.contains("bone_meal")
                || id.contains("composter")
                || id.contains("bucket");
    }

    private static boolean isDecorItem(String id) {
        return id.contains("banner")
                || id.contains("bed")
                || id.contains("carpet")
                || id.contains("wool")
                || id.contains("terracotta")
                || id.contains("concrete")
                || id.contains("painting")
                || id.contains("pot")
                || id.contains("sherd")
                || id.contains("head")
                || id.contains("skull")
                || id.contains("armor_stand")
                || id.contains("jukebox")
                || id.contains("note_block")
                || id.contains("bookshelf")
                || id.contains("chest")
                || id.contains("barrel")
                || id.contains("table")
                || id.contains("lectern")
                || id.contains("loom")
                || id.contains("smithing")
                || id.contains("cartography")
                || id.contains("furnace")
                || id.contains("brewing_stand")
                || id.contains("grindstone")
                || id.contains("stonecutter");
    }

    private static int spawnEggPrice(String id) {
        if (id.contains("wither") || id.contains("warden") || id.contains("elder_guardian")) {
            return 1_024;
        }
        if (id.contains("blaze") || id.contains("breeze") || id.contains("ghast") || id.contains("evoker")
                || id.contains("ravager") || id.contains("piglin_brute") || id.contains("guardian")) {
            return 512;
        }
        if (id.contains("villager") || id.contains("iron_golem") || id.contains("happy_ghast")) {
            return 320;
        }
        return 192;
    }

    private static int gearPrice(String id) {
        if (id.contains("diamond")) {
            return 64;
        }
        if (id.contains("golden") || id.contains("gold")) {
            return 18;
        }
        if (id.contains("iron") || id.equals("shield") || id.equals("crossbow")) {
            return 16;
        }
        if (id.contains("chainmail")) {
            return 24;
        }
        if (id.contains("stone")) {
            return 3;
        }
        if (id.contains("wooden")) {
            return 2;
        }
        return 12;
    }

    private static int foodPrice(String id) {
        if (id.equals("golden_apple")) {
            return 64;
        }
        if (id.equals("cake") || id.contains("stew") || id.contains("soup")) {
            return 4;
        }
        if (id.contains("cooked") || id.contains("beef") || id.contains("porkchop") || id.contains("mutton")) {
            return 2;
        }
        return 1;
    }

    private static int redstonePrice(String id) {
        if (id.contains("hopper")) {
            return 5;
        }
        if (id.contains("observer") || id.contains("sticky_piston") || id.contains("crafter")) {
            return 4;
        }
        if (id.contains("piston") || id.contains("dispenser") || id.contains("comparator")) {
            return 3;
        }
        if (id.contains("repeater") || id.contains("dropper")) {
            return 2;
        }
        return 1;
    }

    private static int lightingPrice(String id) {
        if (id.contains("sea_lantern") || id.contains("froglight") || id.contains("glowstone") || id.contains("shroomlight")) {
            return 4;
        }
        if (id.contains("soul") || id.contains("candle") || id.equals("end_rod")) {
            return 2;
        }
        return 1;
    }

    private static int blockPrice(String id) {
        if (id.contains("concrete") || id.contains("terracotta") || id.contains("wool") || id.contains("carpet")) {
            return 1;
        }
        if (id.contains("prismarine") || id.contains("quartz") || id.contains("purpur")) {
            return 4;
        }
        if (id.contains("copper")) {
            return 4;
        }
        if (id.contains("amethyst") || id.contains("sculk") || id.contains("resin")) {
            return 6;
        }
        if (id.contains("ancient_debris")) {
            return 80;
        }
        if (id.contains("ore")) {
            return 8;
        }
        if (id.contains("stairs") || id.contains("slab") || id.contains("wall")) {
            return 1;
        }
        return 2;
    }

    private static int miscPrice(String id) {
        if (id.contains("music_disc")) {
            return 96;
        }
        if (id.contains("smithing_template") || id.contains("armor_trim")) {
            return 128;
        }
        if (id.equals("name_tag") || id.equals("saddle")) {
            return 48;
        }
        if (id.contains("potion") || id.equals("enchanted_book")) {
            return 64;
        }
        if (id.contains("bucket")) {
            return 4;
        }
        if (id.contains("dye")) {
            return 1;
        }
        return 2;
    }
}
