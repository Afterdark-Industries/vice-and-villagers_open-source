package com.trippycraft.economy;

import com.trippycraft.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class DealerRegistries {
    private static final Map<String, DealerProduct> PRODUCTS = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_DOSE_PRODUCTS = Map.ofEntries(
            Map.entry("four_ho_met_5mg", "four_ho_met_25mg"),
            Map.entry("four_ho_met_10mg", "four_ho_met_100mg"),
            Map.entry("four_ho_met_20mg", "four_ho_met_250mg"),
            Map.entry("four_ho_met_40mg", "four_ho_met_500mg"),
            Map.entry("al_lad_75ug", "al_lad_25mg"),
            Map.entry("al_lad_150ug", "al_lad_100mg"),
            Map.entry("al_lad_300ug", "al_lad_250mg"),
            Map.entry("al_lad_450ug", "al_lad_500mg"),
            Map.entry("two_ce_10mg", "two_ce_25mg"),
            Map.entry("two_ce_20mg", "two_ce_100mg"),
            Map.entry("two_ce_40mg", "two_ce_250mg"),
            Map.entry("two_ce_80mg", "two_ce_500mg"));
    private static final List<String> CATEGORIES = new ArrayList<>();
    private static final CustomerAddictionProfile NON_ADDICTIVE = new CustomerAddictionProfile("non_addictive", 1, 0, 0, 8);
    private static final CustomerAddictionProfile LOW_NON_COMPULSIVE = new CustomerAddictionProfile("low_non_compulsive", 2, 0, 0, 12);
    private static final CustomerAddictionProfile CANNABIS_MILD = new CustomerAddictionProfile("cannabis_mild", 4, 1, 1, 42);
    private static final CustomerAddictionProfile CANNABIS_STRONG = new CustomerAddictionProfile("cannabis_strong", 5, 2, 1, 54);
    private static final CustomerAddictionProfile COCAINE = new CustomerAddictionProfile("cocaine", 12, 5, 2, 88);
    private static final CustomerAddictionProfile CRACK_COCAINE = new CustomerAddictionProfile("crack_cocaine", 16, 7, 3, 100);
    private static final CustomerAddictionProfile HEROIN = new CustomerAddictionProfile("heroin", 18, 8, 3, 100);
    private static final CustomerAddictionProfile KETAMINE = new CustomerAddictionProfile("ketamine", 8, 3, 2, 72);

    static {
        product("cannabis_joint", ModItems.CANNABIS_JOINT::get, MarketRegistries.CATEGORY_CANNABIS, 12, 0);
        product("cannabis_gummies", ModItems.CANNABIS_GUMMIES::get, MarketRegistries.CATEGORY_CANNABIS, 32, 1);
        product("cannabis_chocolate_bar", ModItems.CANNABIS_CHOCOLATE_BAR::get, MarketRegistries.CATEGORY_CANNABIS, 28, 1);
        product("cannabis_dab_pen", ModItems.CANNABIS_DAB_PEN::get, MarketRegistries.CATEGORY_CANNABIS, 44, 2);

        product("psilocybin_capsule_0_5g", ModItems.PSILOCYBIN_CAPSULE_05G::get, MarketRegistries.CATEGORY_PSILOCYBIN, 9, 0);
        product("psilocybin_capsule_1g", ModItems.PSILOCYBIN_CAPSULE_1G::get, MarketRegistries.CATEGORY_PSILOCYBIN, 16, 0);
        product("psilocybin_capsule_3g", ModItems.PSILOCYBIN_CAPSULE_3G::get, MarketRegistries.CATEGORY_PSILOCYBIN, 32, 1);
        product("psilocybin_capsule_5g", ModItems.PSILOCYBIN_CAPSULE_5G::get, MarketRegistries.CATEGORY_PSILOCYBIN, 48, 2);

        product("cocaine_powder", ModItems.COCAINE_POWDER::get, MarketRegistries.CATEGORY_COCAINE, 32, 1);
        product("crack_cocaine", ModItems.CRACK_COCAINE::get, MarketRegistries.CATEGORY_COCAINE, 46, 2);

        product("heroin_25mg", ModItems.HEROIN_25MG::get, MarketRegistries.CATEGORY_HEROIN, 16, 0);
        product("heroin_100mg", ModItems.HEROIN_100MG::get, MarketRegistries.CATEGORY_HEROIN, 34, 1);
        product("heroin_250mg", ModItems.HEROIN_250MG::get, MarketRegistries.CATEGORY_HEROIN, 58, 2);
        product("heroin_500mg", ModItems.HEROIN_500MG::get, MarketRegistries.CATEGORY_HEROIN, 88, 3);

        product("lsd_blotter_50ug", ModItems.LSD_BLOTTER_50UG::get, MarketRegistries.CATEGORY_LSD, 12, 0);
        product("lsd_blotter_100ug", ModItems.LSD_BLOTTER_100UG::get, MarketRegistries.CATEGORY_LSD, 24, 0);
        product("lsd_blotter_250ug", ModItems.LSD_BLOTTER_250UG::get, MarketRegistries.CATEGORY_LSD, 48, 1);
        product("lsd_blotter_500ug", ModItems.LSD_BLOTTER_500UG::get, MarketRegistries.CATEGORY_LSD, 78, 2);

        product("dmt_crystal", ModItems.DMT::get, MarketRegistries.CATEGORY_DMT, 34, 1);
        product("dmt_crystal_5x", ModItems.DMT_5X::get, MarketRegistries.CATEGORY_DMT, 92, 3);

        product("ketamine_vial", ModItems.KETAMINE::get, MarketRegistries.CATEGORY_KETAMINE, 56, 2);
        product("ketamine_25mg", ModItems.KETAMINE_25MG::get, MarketRegistries.CATEGORY_KETAMINE, 14, 0);
        product("ketamine_100mg", ModItems.KETAMINE_100MG::get, MarketRegistries.CATEGORY_KETAMINE, 28, 1);
        product("ketamine_250mg", ModItems.KETAMINE_250MG::get, MarketRegistries.CATEGORY_KETAMINE, 48, 2);
        product("ketamine_500mg", ModItems.KETAMINE_500MG::get, MarketRegistries.CATEGORY_KETAMINE, 74, 3);

        product("mdma_25mg", ModItems.MDMA_25MG::get, MarketRegistries.CATEGORY_MDMA, 12, 0);
        product("mdma_100mg", ModItems.MDMA_100MG::get, MarketRegistries.CATEGORY_MDMA, 26, 1);
        product("mdma_250mg", ModItems.MDMA_250MG::get, MarketRegistries.CATEGORY_MDMA, 48, 2);
        product("mdma_500mg", ModItems.MDMA_500MG::get, MarketRegistries.CATEGORY_MDMA, 76, 3);

        product("two_cb_25mg", ModItems.TWO_CB_25MG::get, MarketRegistries.CATEGORY_TWO_CB, 14, 0);
        product("two_cb_100mg", ModItems.TWO_CB_100MG::get, MarketRegistries.CATEGORY_TWO_CB, 30, 1);
        product("two_cb_250mg", ModItems.TWO_CB_250MG::get, MarketRegistries.CATEGORY_TWO_CB, 55, 2);
        product("two_cb_500mg", ModItems.TWO_CB_500MG::get, MarketRegistries.CATEGORY_TWO_CB, 86, 3);

        product("four_ho_met_25mg", ModItems.HO_MET_25MG::get, MarketRegistries.CATEGORY_HO_MET, 14, 0);
        product("four_ho_met_100mg", ModItems.HO_MET_100MG::get, MarketRegistries.CATEGORY_HO_MET, 26, 1);
        product("four_ho_met_250mg", ModItems.HO_MET_250MG::get, MarketRegistries.CATEGORY_HO_MET, 48, 2);
        product("four_ho_met_500mg", ModItems.HO_MET_500MG::get, MarketRegistries.CATEGORY_HO_MET, 78, 3);

        product("al_lad_25mg", ModItems.AL_LAD_25MG::get, MarketRegistries.CATEGORY_AL_LAD, 16, 0);
        product("al_lad_100mg", ModItems.AL_LAD_100MG::get, MarketRegistries.CATEGORY_AL_LAD, 30, 1);
        product("al_lad_250mg", ModItems.AL_LAD_250MG::get, MarketRegistries.CATEGORY_AL_LAD, 54, 2);
        product("al_lad_500mg", ModItems.AL_LAD_500MG::get, MarketRegistries.CATEGORY_AL_LAD, 82, 3);

        product("two_ce_25mg", ModItems.TWO_CE_25MG::get, MarketRegistries.CATEGORY_TWO_CE, 16, 0);
        product("two_ce_100mg", ModItems.TWO_CE_100MG::get, MarketRegistries.CATEGORY_TWO_CE, 32, 1);
        product("two_ce_250mg", ModItems.TWO_CE_250MG::get, MarketRegistries.CATEGORY_TWO_CE, 58, 2);
        product("two_ce_500mg", ModItems.TWO_CE_500MG::get, MarketRegistries.CATEGORY_TWO_CE, 88, 3);

        product("mescaline_25mg", ModItems.MESCALINE_25MG::get, MarketRegistries.CATEGORY_MESCALINE, 14, 0);
        product("mescaline_100mg", ModItems.MESCALINE_100MG::get, MarketRegistries.CATEGORY_MESCALINE, 28, 1);
        product("mescaline_250mg", ModItems.MESCALINE_250MG::get, MarketRegistries.CATEGORY_MESCALINE, 50, 2);
        product("mescaline_500mg", ModItems.MESCALINE_500MG::get, MarketRegistries.CATEGORY_MESCALINE, 80, 3);

        product("salvia_leaf", ModItems.SALVIA_1X::get, MarketRegistries.CATEGORY_SALVIA, 8, 0);
        product("salvia_leaf_10x", ModItems.SALVIA_10X::get, MarketRegistries.CATEGORY_SALVIA, 24, 1);
        product("salvia_leaf_25x", ModItems.SALVIA_25X::get, MarketRegistries.CATEGORY_SALVIA, 46, 2);
        product("salvia_leaf_50x", ModItems.SALVIA_50X::get, MarketRegistries.CATEGORY_SALVIA, 76, 3);

        product("nitrous_canister", ModItems.NITROUS::get, MarketRegistries.CATEGORY_NITROUS, 18, 0);

    }

    private DealerRegistries() {
    }

    public static List<DealerProduct> products() {
        return List.copyOf(PRODUCTS.values());
    }

    public static Optional<DealerProduct> product(String id) {
        return Optional.ofNullable(PRODUCTS.get(LEGACY_DOSE_PRODUCTS.getOrDefault(id, id)));
    }

    public static Optional<DealerProduct> productFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId.getNamespace().equals("trippycraft") && LEGACY_DOSE_PRODUCTS.containsKey(itemId.getPath())) {
            return product(itemId.getPath());
        }
        return PRODUCTS.values().stream()
                .filter(product -> stack.is(product.item().get()))
                .findFirst();
    }

    public static boolean matchesProductItem(ItemStack stack, Item item) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(item)) {
            return true;
        }
        var sourceId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!sourceId.getNamespace().equals("trippycraft")) {
            return false;
        }
        String canonicalId = LEGACY_DOSE_PRODUCTS.get(sourceId.getPath());
        var expectedId = BuiltInRegistries.ITEM.getKey(item);
        return canonicalId != null && expectedId.getNamespace().equals("trippycraft")
                && expectedId.getPath().equals(canonicalId);
    }

    public static List<DealerProduct> productsInCategory(String category) {
        return PRODUCTS.values().stream()
                .filter(product -> product.category().equals(category))
                .toList();
    }

    public static List<String> categories() {
        return List.copyOf(CATEGORIES);
    }

    private static void product(String id,
                                Supplier<net.minecraft.world.item.Item> item,
                                String category,
                                int emeraldPricePerUnit,
                                int unlockLevel) {
        PRODUCTS.put(id, new DealerProduct(
                id,
                item,
                category,
                Math.max(1, emeraldPricePerUnit),
                Math.max(0, unlockLevel),
                addictionProfileFor(id, category)
        ));
        if (!CATEGORIES.contains(category)) {
            CATEGORIES.add(category);
        }
    }

    private static CustomerAddictionProfile addictionProfileFor(String id, String category) {
        return switch (category) {
            case MarketRegistries.CATEGORY_CANNABIS -> id.equals("cannabis_dab_pen") ? CANNABIS_STRONG : CANNABIS_MILD;
            case MarketRegistries.CATEGORY_PSILOCYBIN -> LOW_NON_COMPULSIVE;
            case MarketRegistries.CATEGORY_COCAINE -> id.equals("crack_cocaine") ? CRACK_COCAINE : COCAINE;
            case MarketRegistries.CATEGORY_HEROIN -> HEROIN;
            case MarketRegistries.CATEGORY_LSD -> NON_ADDICTIVE;
            case MarketRegistries.CATEGORY_DMT -> NON_ADDICTIVE;
            case MarketRegistries.CATEGORY_KETAMINE -> KETAMINE;
            case MarketRegistries.CATEGORY_MDMA -> LOW_NON_COMPULSIVE;
            case MarketRegistries.CATEGORY_TWO_CB -> NON_ADDICTIVE;
            case MarketRegistries.CATEGORY_HO_MET, MarketRegistries.CATEGORY_MESCALINE -> NON_ADDICTIVE;
            case MarketRegistries.CATEGORY_SALVIA -> NON_ADDICTIVE;
            default -> NON_ADDICTIVE;
        };
    }
}
