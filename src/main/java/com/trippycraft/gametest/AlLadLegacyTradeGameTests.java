package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.TrippyCraft;
import com.trippycraft.economy.CustomerService;
import com.trippycraft.economy.DealerProduct;
import com.trippycraft.economy.DealerRegistries;
import com.trippycraft.economy.MarketRegistries;
import com.trippycraft.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AlLadLegacyTradeGameTests {
    private AlLadLegacyTradeGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void customerInventoryCountsAndRemovesLegacyAndCurrentTabs(GameTestHelper helper) {
        ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "allad-trade-test"));
        var inventory = player.getInventory();
        inventory.clearContent();
        inventory.setItem(0, new ItemStack(ModItems.AL_LAD_75UG.get(), 3));
        inventory.setItem(1, new ItemStack(ModItems.AL_LAD_25MG.get(), 2));
        inventory.setItem(2, new ItemStack(ModItems.AL_LAD_150UG.get(), 7));
        inventory.setItem(3, new ItemStack(Items.EMERALD, 9));
        inventory.setItem(4, new ItemStack(ModItems.AL_LAD_100MG.get(), 4));
        inventory.setItem(40, new ItemStack(ModItems.AL_LAD_75UG.get()));

        Item requested = DealerRegistries.product("al_lad_75ug").orElseThrow().item().get();
        helper.assertTrue(requested == ModItems.AL_LAD_25MG.get(), "A saved legacy customer product did not resolve to its current tab format");
        helper.assertTrue(count(player, requested) == 6, "The customer must count matching legacy and current tabs, including the offhand");
        helper.assertTrue(remove(player, requested, 5) == 5, "The customer failed to remove the requested quantity from mixed legacy/current stock");
        helper.assertTrue(inventory.getItem(0).isEmpty() && inventory.getItem(1).isEmpty(), "The matching inventory stacks were not consumed");
        helper.assertTrue(inventory.getItem(40).is(ModItems.AL_LAD_75UG.get()) && inventory.getItem(40).getCount() == 1,
                "The sale consumed the offhand tab after already reaching its requested quantity");
        helper.assertTrue(count(player, requested) == 1, "A sale removed more than requested or missed the remaining offhand tab");
        helper.assertTrue(remove(player, requested, 8) == 1 && count(player, requested) == 0,
                "A partial sale must remove only available tabs and report its actual quantity");
        helper.assertTrue(inventory.getItem(40).isEmpty(), "The partial sale did not consume the remaining offhand tab");
        helper.assertTrue(inventory.getItem(2).is(ModItems.AL_LAD_150UG.get()) && inventory.getItem(2).getCount() == 7,
                "A customer sale consumed the wrong legacy tab tier");
        helper.assertTrue(inventory.getItem(4).is(ModItems.AL_LAD_100MG.get()) && inventory.getItem(4).getCount() == 4,
                "A customer sale consumed the wrong current tab tier");
        helper.assertTrue(inventory.getItem(3).is(Items.EMERALD) && inventory.getItem(3).getCount() == 9,
                "A customer sale consumed unrelated inventory");
        helper.assertTrue(count(player, Items.EMERALD) == 9 && remove(player, Items.EMERALD, 4) == 4
                && inventory.getItem(3).getCount() == 5, "Unrelated exact-item inventory matching changed");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void legacyTradeAliasesAndSamplesKeepAllFourTabTiersSeparate(GameTestHelper helper) {
        List<TierCase> tiers = tiers();
        for (TierCase tier : tiers) {
            DealerProduct currentProduct = DealerRegistries.product(tier.currentId()).orElseThrow();
            DealerProduct legacyProduct = DealerRegistries.product(tier.legacyId()).orElseThrow();
            helper.assertTrue(legacyProduct == currentProduct && legacyProduct.id().equals(tier.currentId()),
                    "The saved legacy product ID did not resolve to its canonical current product: " + tier.legacyId());
            helper.assertTrue(legacyProduct.item().get() == tier.current(), "The legacy alias resolved to the wrong tab tier");

            ItemStack oldTab = new ItemStack(tier.legacy());
            ItemStack currentTab = new ItemStack(tier.current());
            helper.assertTrue(DealerRegistries.productFor(oldTab).orElseThrow() == currentProduct,
                    "Legacy samples and saved customer products resolved to different tiers");
            helper.assertTrue(DealerRegistries.productFor(currentTab).orElseThrow() == currentProduct,
                    "Current samples resolved to the wrong product");
            helper.assertTrue(DealerRegistries.matchesProductItem(oldTab, tier.current()), "A corresponding legacy tab was rejected");
            helper.assertTrue(DealerRegistries.matchesProductItem(currentTab, tier.current()), "Current tab identity matching changed");
            helper.assertFalse(DealerRegistries.matchesProductItem(currentTab, tier.legacy()),
                    "Compatibility must only map old inventory to current requested products");
            for (TierCase other : tiers) {
                if (other != tier) {
                    helper.assertFalse(DealerRegistries.matchesProductItem(oldTab, other.current()), "A legacy tab matched another current strength tier");
                    helper.assertFalse(DealerRegistries.matchesProductItem(currentTab, other.current()), "A current tab matched another current strength tier");
                }
            }
        }
        helper.assertFalse(DealerRegistries.matchesProductItem(ItemStack.EMPTY, Items.AIR), "Empty inventory matched a product");
        helper.assertTrue(DealerRegistries.productFor(ItemStack.EMPTY).isEmpty(), "An empty sample resolved to a dealer product");
        helper.assertFalse(DealerRegistries.matchesProductItem(new ItemStack(ModItems.AL_LAD_CONCENTRATE.get()), ModItems.AL_LAD_25MG.get()),
                "Concentrate was incorrectly accepted as a finished tab");
        helper.assertFalse(DealerRegistries.matchesProductItem(new ItemStack(ModItems.ALLYL_LYSERGIC_INTERMEDIATE.get()), ModItems.AL_LAD_25MG.get()),
                "An intermediate was incorrectly accepted as a finished tab");
        helper.assertFalse(DealerRegistries.matchesProductItem(new ItemStack(ModItems.HO_MET_25MG.get()), ModItems.AL_LAD_25MG.get()),
                "A different substance was accepted as an AL-LAD tab");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void dealerCatalogContainsOnlyCurrentTabsAtPreservedPricesAndRanks(GameTestHelper helper) {
        List<TierCase> tiers = tiers();
        List<DealerProduct> products = DealerRegistries.productsInCategory(MarketRegistries.CATEGORY_AL_LAD);
        helper.assertTrue(products.stream().map(DealerProduct::id).toList().equals(tiers.stream().map(TierCase::currentId).toList()),
                "The AL-LAD dealer catalog must contain exactly the four current tab IDs in tier order");
        for (int rank = 0; rank < tiers.size(); rank++) {
            TierCase tier = tiers.get(rank);
            DealerProduct product = products.get(rank);
            helper.assertTrue(product.item().get() == tier.current() && product.category().equals(MarketRegistries.CATEGORY_AL_LAD),
                    "The AL-LAD dealer catalog contains an incorrect item or category");
            helper.assertTrue(product.emeraldPricePerUnit() == tier.price() && product.unlockLevel() == rank,
                    "The price or unlock rank changed for " + tier.currentId());
            helper.assertTrue(DealerRegistries.product(tier.legacyId()).orElseThrow().emeraldPricePerUnit() == tier.price(),
                    "A saved legacy customer offer resolved to a different price");
        }
        helper.succeed();
    }

    private static List<TierCase> tiers() {
        return List.of(
                new TierCase("al_lad_75ug", ModItems.AL_LAD_75UG.get(), "al_lad_25mg", ModItems.AL_LAD_25MG.get(), 16),
                new TierCase("al_lad_150ug", ModItems.AL_LAD_150UG.get(), "al_lad_100mg", ModItems.AL_LAD_100MG.get(), 30),
                new TierCase("al_lad_300ug", ModItems.AL_LAD_300UG.get(), "al_lad_250mg", ModItems.AL_LAD_250MG.get(), 54),
                new TierCase("al_lad_450ug", ModItems.AL_LAD_450UG.get(), "al_lad_500mg", ModItems.AL_LAD_500MG.get(), 82));
    }

    private static int count(ServerPlayer player, Item item) {
        return invokeInventoryMethod("countItem", new Class<?>[]{ServerPlayer.class, Item.class}, player, item);
    }

    private static int remove(ServerPlayer player, Item item, int amount) {
        return invokeInventoryMethod("removeItem", new Class<?>[]{ServerPlayer.class, Item.class, int.class}, player, item, amount);
    }

    private static int invokeInventoryMethod(String name, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = CustomerService.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return (int) method.invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not exercise the real customer inventory method " + name, exception);
        }
    }

    private record TierCase(String legacyId, Item legacy, String currentId, Item current, int price) {
    }
}
