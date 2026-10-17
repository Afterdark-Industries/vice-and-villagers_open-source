package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.economy.DealerRegistries;
import com.trippycraft.economy.EmeraldPayoutLedger;
import com.trippycraft.economy.EmeraldWallet;
import com.trippycraft.economy.MarketContract;
import com.trippycraft.economy.MarketDataStore;
import com.trippycraft.economy.MarketPlayerData;
import com.trippycraft.economy.MarketRegistries;
import com.trippycraft.economy.MarketService;
import com.trippycraft.economy.MarketStock;
import com.trippycraft.economy.SellableMarketProduct;
import com.trippycraft.item.AlLadConcentrateItem;
import com.trippycraft.item.HoMetCrystalItem;
import com.trippycraft.item.MdmaCrystalItem;
import com.trippycraft.item.StackableDoseItemSupport;
import com.trippycraft.item.TwoCbCrystalItem;
import com.trippycraft.item.TwoCeCrystalItem;
import com.trippycraft.network.MarketActionPacket;
import com.trippycraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Focused broker catalog, actual action/settlement and saved-stock regression tests. */
@GameTestHolder("trippycraft_market")
@PrefixGameTestTemplate(false)
public final class MarketBrokerGameTests {
    private static final Set<String> FAMILIES = Set.of("Cannabis", "Psilocybin", "Cocaine", "Heroin", "LSD", "DMT",
            "Ketamine", "MDMA", "2C-B", "4-HO-MET", "AL-LAD", "2C-E", "Mescaline", "Salvia", "Nitrous", "Alcohol");
    private static final List<String> FINISHED_FORMS = List.of("dmt_powder", "mdma_crystal", "mdma_caps_25mg", "mdma_caps_100mg",
            "mdma_caps_250mg", "mdma_caps_500mg", "two_cb_crystal", "two_cb_powder", "four_ho_met_crystal", "four_ho_met_powder",
            "two_ce_crystal", "two_ce_powder", "al_lad_concentrate", "lsd_solution", "ethanol", "heroin_base",
            "bong_cannabis", "bong_dmt", "bong_salvia_1x", "bong_salvia_10x", "bong_salvia_25x", "bong_salvia_50x",
            "ancient_pipe_cannabis", "ancient_pipe_dmt", "ancient_pipe_salvia_1x", "ancient_pipe_salvia_10x", "ancient_pipe_salvia_25x", "ancient_pipe_salvia_50x",
            "beer_bottle", "wine_glass_5oz", "wine_glass_9oz", "whiskey_glass", "rum_glass", "vodka_glass");
    private static final List<Alias> ALIASES = List.of(
            new Alias("four_ho_met_5mg", "four_ho_met_25mg", "four_ho_met_100mg"),
            new Alias("four_ho_met_10mg", "four_ho_met_100mg", "four_ho_met_250mg"),
            new Alias("four_ho_met_20mg", "four_ho_met_250mg", "four_ho_met_500mg"),
            new Alias("four_ho_met_40mg", "four_ho_met_500mg", "four_ho_met_25mg"),
            new Alias("al_lad_75ug", "al_lad_25mg", "al_lad_100mg"), new Alias("al_lad_150ug", "al_lad_100mg", "al_lad_250mg"),
            new Alias("al_lad_300ug", "al_lad_250mg", "al_lad_500mg"), new Alias("al_lad_450ug", "al_lad_500mg", "al_lad_25mg"),
            new Alias("two_ce_10mg", "two_ce_25mg", "two_ce_100mg"), new Alias("two_ce_20mg", "two_ce_100mg", "two_ce_250mg"),
            new Alias("two_ce_40mg", "two_ce_250mg", "two_ce_500mg"), new Alias("two_ce_80mg", "two_ce_500mg", "two_ce_25mg"),
            new Alias("heroin_tar", "heroin_100mg", "heroin_500mg"), new Alias("mescaline_button", "mescaline_100mg", "mescaline_500mg"));

    private MarketBrokerGameTests() { }

    @GameTest(template = "empty")
    public static void everyDealerProductAndAllPreparedFormsAppearOnceAcrossAllSixteenBrokerFamilies(GameTestHelper helper) {
        List<SellableMarketProduct> products = MarketRegistries.sellableProducts();
        helper.assertTrue(new HashSet<>(MarketRegistries.sellableCategories()).containsAll(FAMILIES), "Broker omits a substance family");
        Set<String> ids = products.stream().map(SellableMarketProduct::id).collect(Collectors.toSet());
        Set<Item> items = products.stream().map(product -> product.item().get()).collect(Collectors.toSet());
        helper.assertTrue(ids.size() == products.size() && items.size() == products.size(), "Broker duplicates a canonical row/item");
        for (var dealer : DealerRegistries.products()) {
            var product = product(dealer.id());
            helper.assertTrue(product.item().get() == dealer.item().get() && product.category().equals(dealer.category()),
                    "Broker omits or misidentifies dealer stock: " + dealer.id());
        }
        for (String id : FINISHED_FORMS) helper.assertTrue(ids.contains(id), "Broker omits a prepared form: " + id);
        for (String excluded : List.of("bong", "ancient_pipe", "empty_capsule", "blend_capsule", "cannabis_seeds",
                "salvia_seeds", "mortar_and_pestle", "two_ce_precursor", "four_ho_met_freebase", "allyl_lysergic_intermediate")) {
            helper.assertTrue(!ids.contains(excluded), "Broker lists an empty device, arbitrary blend or new precursor: " + excluded);
        }
        for (Alias alias : ALIASES) helper.assertTrue(!ids.contains(alias.legacy()), "Legacy alias creates a duplicate broker row");
        for (String id : List.of("mdma_crystal", "two_cb_crystal", "four_ho_met_crystal", "two_ce_crystal", "al_lad_concentrate",
                "ethanol", "bong_dmt", "ancient_pipe_dmt")) helper.assertTrue(!product(id).contractEligible(), "Bulk/device form creates impractical contracts: " + id);
        try (Fixture fixture = fixture(helper)) {
            var snapshot = MarketService.createSnapshot(fixture.player, fixture.data, true);
            Set<String> visible = snapshot.demand().stream().map(entry -> entry.productId()).collect(Collectors.toSet());
            helper.assertTrue(visible.equals(ids), "Server snapshot omitted or duplicated catalog demand rows");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void everyCatalogRowSellsExactFullItemsQueuesItsPriceAndPaysProgressionExactlyOnce(GameTestHelper helper) {
        for (SellableMarketProduct product : MarketRegistries.sellableProducts()) {
            try (Fixture fixture = fixture(helper)) {
                for (int slot : new int[]{0, 9, 40}) fixture.player.getInventory().setItem(slot, product.stack());
                helper.assertTrue(MarketService.countItem(fixture.player, product.item().get()) == 3, "Full stock is unavailable: " + product.id());
                fixture.sell(product.id(), 2);
                int reward = product.baseEmeraldValue() * 2;
                helper.assertTrue(MarketService.countItem(fixture.player, product.item().get()) == 1
                                && fixture.player.getInventory().getItem(40).is(product.item().get()),
                        "Selling did not remove exactly two items across inventory slots: " + product.id());
                helper.assertTrue(fixture.data.totalUnitsSold() == 2 && fixture.data.reputation() == Math.max(1, reward / 4)
                                && fixture.data.demandMultiplier(product.id()) == 99,
                        "Sale changed unit/reputation/demand accounting: " + product.id());
                helper.assertTrue(EmeraldWallet.value(fixture.player) == 0, "Sale paid before ledger settlement");
                fixture.settle();
                helper.assertTrue(EmeraldWallet.value(fixture.player) == reward, "Sale paid the wrong listed value: " + product.id());
                fixture.settle();
                helper.assertTrue(EmeraldWallet.value(fixture.player) == reward, "Repeated close duplicated a payout");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allFourteenLegacyAliasesShareOnlyTheirEquivalentTierWithoutDuplicateRowsOrPayouts(GameTestHelper helper) {
        for (Alias alias : ALIASES) {
            try (Fixture fixture = fixture(helper)) {
                Item legacy = item(alias.legacy()), canonical = item(alias.canonical()), wrong = item(alias.wrongTier());
                fixture.player.getInventory().setItem(0, new ItemStack(legacy, 2));
                fixture.player.getInventory().setItem(9, new ItemStack(canonical));
                fixture.player.getInventory().setItem(10, new ItemStack(wrong));
                helper.assertTrue(MarketStock.matches(new ItemStack(legacy), canonical) && !MarketStock.matches(new ItemStack(legacy), wrong),
                        "Legacy stock was mapped to the wrong dose tier: " + alias.legacy());
                helper.assertTrue(MarketService.countItem(fixture.player, canonical) == 3, "Alias stock count differs from canonical row");
                fixture.sell(alias.canonical(), 0);
                helper.assertTrue(MarketService.countItem(fixture.player, canonical) == 0
                                && fixture.player.getInventory().getItem(10).is(wrong),
                        "Alias sale consumed another tier or retained a sold item");
                fixture.settle();
                int reward = product(alias.canonical()).baseEmeraldValue() * 3;
                helper.assertTrue(EmeraldWallet.value(fixture.player) == reward && fixture.data.totalUnitsSold() == 3, "Alias sale changed the equivalent price/quantity");
                fixture.sell(alias.canonical(), 0);
                fixture.settle();
                helper.assertTrue(EmeraldWallet.value(fixture.player) == reward && fixture.data.totalUnitsSold() == 3, "Replayed alias sale minted emeralds");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void partialReservoirsAndLegacyDamageRemainUntouchedWhileOnlyFreshStockIsPaid(GameTestHelper helper) {
        List<ItemStack> partials = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            Item[] consumables = {ModItems.CANNABIS_JOINT.get(), ModItems.CANNABIS_DAB_PEN.get(), ModItems.CANNABIS_GUMMIES.get(), ModItems.CANNABIS_CHOCOLATE_BAR.get(), ModItems.ETHANOL.get()};
            int[] uses = {4, 20, 2, 2, 2};
            ItemStack partial = new ItemStack(consumables[index]);
            StackableDoseItemSupport.setRemainingUsesForLoot(partial, uses[index], uses[index] - 1);
            partials.add(partial);
        }
        partials.add(partial(ModItems.MDMA_CRYSTAL.get(), stack -> MdmaCrystalItem.consumeMilligrams(stack, 1000)));
        partials.add(partial(ModItems.TWO_CB_CRYSTAL.get(), stack -> TwoCbCrystalItem.consumeMilligrams(stack, 1000)));
        partials.add(partial(ModItems.HO_MET_CRYSTAL.get(), stack -> HoMetCrystalItem.consumeMilligrams(stack, 1000)));
        partials.add(partial(ModItems.TWO_CE_CRYSTAL.get(), stack -> TwoCeCrystalItem.consumeMilligrams(stack, 1000)));
        partials.add(partial(ModItems.AL_LAD_CONCENTRATE.get(), stack -> AlLadConcentrateItem.consumeMilligrams(stack, 1000)));
        ItemStack oldDamage = new ItemStack(ModItems.MDMA_CRYSTAL.get());
        oldDamage.set(DataComponents.DAMAGE, 1000);
        partials.add(oldDamage);
        for (ItemStack partial : partials) {
            try (Fixture fixture = fixture(helper)) {
                String id = BuiltInRegistries.ITEM.getKey(partial.getItem()).getPath();
                fixture.player.getInventory().setItem(0, partial.copy());
                fixture.player.getInventory().setItem(9, new ItemStack(partial.getItem()));
                helper.assertTrue(!MarketStock.matches(partial, partial.getItem()) && MarketService.countItem(fixture.player, partial.getItem()) == 1,
                        "Partly consumed stock is advertised at full price: " + id);
                fixture.sell(id, 0);
                helper.assertTrue(ItemStack.matches(partial, fixture.player.getInventory().getItem(0))
                                && fixture.player.getInventory().getItem(9).isEmpty() && fixture.data.totalUnitsSold() == 1,
                        "Selling consumed a partial reservoir or counted it as full stock: " + id);
                fixture.sell(id, 0);
                fixture.settle();
                helper.assertTrue(EmeraldWallet.value(fixture.player) == product(id).baseEmeraldValue()
                                && fixture.data.totalUnitsSold() == 1 && ItemStack.matches(partial, fixture.player.getInventory().getItem(0)),
                        "Repeated requests paid for or modified a partial reservoir: " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void contractsRequireAllFullUnitsAndCannotReplayButAcceptEquivalentLegacyStock(GameTestHelper helper) {
        try (Fixture fixture = fixture(helper)) {
            fixture.contract("full-stock-order", "cannabis_joint", 2, 27, 9);
            ItemStack partial = new ItemStack(ModItems.CANNABIS_JOINT.get());
            StackableDoseItemSupport.setRemainingUsesForLoot(partial, 4, 3);
            fixture.player.getInventory().setItem(0, partial.copy());
            fixture.player.getInventory().setItem(9, new ItemStack(ModItems.CANNABIS_JOINT.get()));
            fixture.complete("full-stock-order");
            helper.assertTrue(fixture.data.totalUnitsSold() == 0 && fixture.data.completedContracts() == 0 && fixture.data.reputation() == 0
                            && fixture.player.getInventory().getItem(9).getCount() == 1 && ItemStack.matches(partial, fixture.player.getInventory().getItem(0)),
                    "An incomplete contract consumed or paid for partial inventory");
            fixture.player.getInventory().setItem(10, new ItemStack(ModItems.CANNABIS_JOINT.get()));
            fixture.complete("full-stock-order");
            helper.assertTrue(fixture.data.totalUnitsSold() == 2 && fixture.data.completedContracts() == 1 && fixture.data.reputation() == 9
                            && fixture.player.getInventory().getItem(9).isEmpty() && fixture.player.getInventory().getItem(10).isEmpty()
                            && ItemStack.matches(partial, fixture.player.getInventory().getItem(0)), "Contract did not consume exactly its full stock");
            helper.assertTrue(fixture.data.activeContracts().stream().noneMatch(contract -> contract.id().equals("full-stock-order")), "Completed contract remains reusable");
            fixture.complete("full-stock-order");
            fixture.settle();
            fixture.settle();
            helper.assertTrue(EmeraldWallet.value(fixture.player) == 27 && fixture.data.completedContracts() == 1, "Stale contract/close replay duplicated rewards");
        }
        try (Fixture fixture = fixture(helper)) {
            fixture.contract("legacy-order", "four_ho_met_250mg", 2, 37, 6);
            fixture.player.getInventory().setItem(0, new ItemStack(ModItems.HO_MET_20MG.get()));
            fixture.player.getInventory().setItem(9, new ItemStack(ModItems.HO_MET_250MG.get()));
            fixture.player.getInventory().setItem(10, new ItemStack(ModItems.HO_MET_500MG.get()));
            fixture.complete("legacy-order");
            fixture.settle();
            helper.assertTrue(fixture.data.completedContracts() == 1 && fixture.data.totalUnitsSold() == 2 && EmeraldWallet.value(fixture.player) == 37
                            && fixture.player.getInventory().getItem(10).is(ModItems.HO_MET_500MG.get()), "Equivalent legacy contract consumed or valued the wrong tier");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingUnavailableDistantAndReplayedRequestsCannotConsumeExtraStockOrMintPayouts(GameTestHelper helper) {
        try (Fixture fixture = fixture(helper)) {
            fixture.player.getInventory().setItem(0, new ItemStack(ModItems.DMT_POWDER.get(), 2));
            fixture.sell("removed-or-unknown-product", 64);
            fixture.sell("ancient_pipe", 1);
            fixture.complete("stale-contract");
            helper.assertTrue(fixture.data.totalUnitsSold() == 0 && fixture.player.getInventory().getItem(0).getCount() == 2, "Unavailable request mutated inventory/progression");
            BlockPos far = helper.absolutePos(new BlockPos(1000, 1, 1000));
            fixture.player.setPos(far.getX(), far.getY(), far.getZ());
            fixture.sell("dmt_powder", 2);
            helper.assertTrue(fixture.data.totalUnitsSold() == 0 && fixture.player.getInventory().getItem(0).getCount() == 2, "Distant broker request consumed stock");
            fixture.player.setPos(fixture.broker.getX(), fixture.broker.getY(), fixture.broker.getZ());
            fixture.sell("dmt_powder", Integer.MAX_VALUE);
            fixture.sell("dmt_powder", Integer.MAX_VALUE);
            fixture.settle();
            helper.assertTrue(fixture.data.totalUnitsSold() == 2 && fixture.player.getInventory().countItem(ModItems.DMT_POWDER.get()) == 0
                            && EmeraldWallet.value(fixture.player) == product("dmt_powder").baseEmeraldValue() * 2,
                    "Oversized or replayed request paid beyond available stock");
            fixture.player.getInventory().setItem(0, new ItemStack(ModItems.BLEND_CAPSULE.get()));
            fixture.sell("blend_capsule", 0);
            fixture.settle();
            helper.assertTrue(fixture.player.getInventory().getItem(0).is(ModItems.BLEND_CAPSULE.get()) && fixture.data.totalUnitsSold() == 2,
                    "Unpriced variable-content capsule entered fixed-price sales");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void queuedSalesAggregateAndSettleOnOpenOnceWhileExistingMarketDataKeepsNewCatalogDemand(GameTestHelper helper) {
        try (Fixture fixture = fixture(helper)) {
            fixture.player.getInventory().setItem(0, new ItemStack(ModItems.DMT_POWDER.get()));
            fixture.player.getInventory().setItem(9, new ItemStack(ModItems.MDMA_CAPS_25MG.get()));
            fixture.sell("dmt_powder", 1);
            fixture.sell("mdma_caps_25mg", 1);
            int expected = product("dmt_powder").baseEmeraldValue() + product("mdma_caps_25mg").baseEmeraldValue();
            helper.assertTrue(EmeraldWallet.value(fixture.player) == 0, "Aggregated ledger paid early");
            MarketService.openMarket(fixture.player);
            fixture.settle();
            helper.assertTrue(EmeraldWallet.value(fixture.player) == expected && fixture.data.totalUnitsSold() == 2, "Open/close lost or duplicated aggregated payouts");
            MarketPlayerData restored = MarketPlayerData.load(fixture.data.save());
            helper.assertTrue(restored.totalUnitsSold() == 2 && restored.reputation() == fixture.data.reputation()
                            && restored.demandMultiplier("dmt_powder") == 99 && restored.demandMultiplier("mdma_caps_25mg") == 99,
                    "Saving market progression lost newly added products");
            MarketPlayerData old = new MarketPlayerData();
            old.setDemandMultiplier("cannabis_joint", 123);
            old.setLastDemandRefreshGameTime(fixture.player.level().getGameTime());
            old.activeContracts().add(new MarketContract("saved-order", "cannabis_joint", "Saved", 8, 20, 5, 130));
            old.setLastContractRefreshGameTime(fixture.player.level().getGameTime());
            var refreshed = MarketService.createSnapshot(fixture.player, old, false);
            helper.assertTrue(refreshed.demand().size() == MarketRegistries.sellableProducts().size() && old.demandMultiplier("cannabis_joint") == 123
                            && old.activeContracts().stream().anyMatch(contract -> contract.id().equals("saved-order")),
                    "An existing market state did not gain new product demand or lost its saved order");
        }
        helper.succeed();
    }

    private static ItemStack partial(Item item, Consumer<ItemStack> consume) { ItemStack result = new ItemStack(item); consume.accept(result); return result; }
    private static Item item(String id) { ResourceLocation key = ResourceLocation.fromNamespaceAndPath("trippycraft", id); if (!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalStateException("Missing item " + id); return BuiltInRegistries.ITEM.get(key); }
    private static SellableMarketProduct product(String id) { return MarketRegistries.sellableProduct(id).orElseThrow(() -> new IllegalStateException("Missing broker product " + id)); }
    private static Fixture fixture(GameTestHelper helper) { return new Fixture(helper); }

    private static final class Fixture implements AutoCloseable {
        private final ServerPlayer player;
        private final Villager broker;
        private final MarketPlayerData data;
        private Fixture(GameTestHelper helper) {
            player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "market-test"));
            player.getInventory().clearContent();
            BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));
            player.setPos(position.getX() + .5, position.getY(), position.getZ() + .5);
            broker = EntityType.VILLAGER.create(helper.getLevel());
            if (broker == null) throw new IllegalStateException("Cannot create broker fixture");
            broker.setNoAi(true);
            broker.setPos(player.position());
            MarketService.markBroker(broker);
            helper.assertTrue(helper.getLevel().addFreshEntity(broker) && MarketService.hasMarketVillagerNearby(player), "Fixture broker is not usable");
            data = MarketDataStore.getOrCreate(player);
            for (var product : MarketRegistries.sellableProducts()) data.setDemandMultiplier(product.id(), 100);
            data.setLastDemandRefreshGameTime(player.level().getGameTime());
            contract("keep-fixture-order", "cannabis_joint", 8, 20, 5);
        }
        private void contract(String id, String product, int count, int reward, int reputation) {
            data.activeContracts().clear();
            data.activeContracts().add(new MarketContract(id, product, "Fixture", count, reward, reputation, 135));
            data.setLastContractRefreshGameTime(player.level().getGameTime());
        }
        private void sell(String id, int amount) { MarketService.handleMarketAction(player, new MarketActionPacket(MarketActionPacket.Action.SELL_PRODUCT, id, amount)); }
        private void complete(String id) { MarketService.handleMarketAction(player, new MarketActionPacket(MarketActionPacket.Action.COMPLETE_CONTRACT, id, 0)); }
        private void settle() { MarketService.handleMarketAction(player, new MarketActionPacket(MarketActionPacket.Action.CLOSE_MARKET, "", 0)); }
        @Override public void close() { EmeraldPayoutLedger.flush(player); MarketDataStore.clearPlayer(player.getUUID()); broker.discard(); }
    }

    private record Alias(String legacy, String canonical, String wrongTier) { }
}
