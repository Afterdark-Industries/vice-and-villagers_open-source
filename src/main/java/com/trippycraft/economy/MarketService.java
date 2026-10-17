package com.trippycraft.economy;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.network.MarketActionPacket;
import com.trippycraft.network.MarketSnapshotPacket;
import com.trippycraft.network.ModNetwork;
import com.trippycraft.TrippyCraft;
import com.trippycraft.world.TavernNightlifeService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MarketService {
    private static final String BROKER_KEY = "market_broker";
    private static final String AUTO_BROKER_KEY = "auto_market_broker";
    private static final long DEMAND_REFRESH_TICKS = 24_000L;
    private static final long CONTRACT_REFRESH_TICKS = 24_000L;
    private static final int CONTRACT_TARGET_COUNT = 4;
    private static final double MARKET_USE_DISTANCE = 8.0D;

    private MarketService() {
    }

    public static boolean isMarketVillager(Villager villager) {
        if (villager.isBaby() || BartenderService.isBartender(villager)) {
            return false;
        }
        if (DealerService.isDealerVillager(villager)) {
            return false;
        }
        if (CustomerService.isCustomerVillager(villager)) {
            return false;
        }
        boolean nitwitBroker = villager.getVillagerData().getProfession() == VillagerProfession.NITWIT;
        boolean taggedBroker = NbtCompat.getBooleanOr(getTrippycraftData(villager), BROKER_KEY, false);
        boolean namedBroker = villager.hasCustomName()
                && villager.getCustomName() != null
                && isMarketName(villager.getCustomName().getString());
        return taggedBroker || nitwitBroker || namedBroker;
    }

    public static boolean isAutomaticBrokerCandidate(Villager villager) {
        return villager.isAlive()
                && !villager.isBaby()
                && !BartenderService.isBartender(villager)
                && !TavernNightlifeService.isNightlifeGuest(villager)
                && !isMarketVillager(villager)
                && !DealerService.isDealerVillager(villager)
                && !CustomerService.isCustomerVillager(villager);
    }

    public static void markBroker(Villager villager) {
        CompoundTag data = getTrippycraftData(villager);
        data.putBoolean(BROKER_KEY, true);
        data.putBoolean(AUTO_BROKER_KEY, true);
        if (!villager.hasCustomName()) {
            villager.setCustomName(Component.literal("Market Broker"));
            villager.setCustomNameVisible(false);
        }
    }

    public static boolean isAutoBroker(Villager villager) {
        CompoundTag data = getTrippycraftData(villager);
        if (NbtCompat.getBooleanOr(data, AUTO_BROKER_KEY, false)) {
            return true;
        }
        return NbtCompat.getBooleanOr(data, BROKER_KEY, false)
                && villager.hasCustomName()
                && villager.getCustomName() != null
                && villager.getCustomName().getString().equals("Market Broker");
    }

    public static void unmarkAutoBroker(Villager villager) {
        if (!isAutoBroker(villager)) {
            return;
        }
        CompoundTag data = getTrippycraftData(villager);
        data.remove(BROKER_KEY);
        data.remove(AUTO_BROKER_KEY);
        if (villager.hasCustomName()
                && villager.getCustomName() != null
                && villager.getCustomName().getString().equals("Market Broker")) {
            villager.setCustomName(null);
        }
    }

    public static Comparator<Villager> brokerCandidateComparator() {
        return Comparator
                .comparing((Villager villager) -> villager.getVillagerData().getProfession() != VillagerProfession.NITWIT)
                .thenComparing(Villager::hasCustomName)
                .thenComparing(Villager::getUUID);
    }

    public static boolean hasMarketVillagerNearby(ServerPlayer player) {
        AABB searchArea = player.getBoundingBox().inflate(MARKET_USE_DISTANCE);
        return !player.level().getEntitiesOfClass(Villager.class, searchArea, MarketService::isMarketVillager).isEmpty();
    }

    public static void openMarket(ServerPlayer player) {
        EmeraldPayoutLedger.flush(player);
        MarketPlayerData data = MarketDataStore.getOrCreate(player);
        prepareMarket(player, data);
        ModNetwork.sendMarketSnapshot(player, createSnapshot(player, data, true));
    }

    public static void handleMarketAction(ServerPlayer player, MarketActionPacket packet) {
        if (packet.action() == MarketActionPacket.Action.CLOSE_MARKET) {
            EmeraldPayoutLedger.flush(player);
            return;
        }

        if (!hasMarketVillagerNearby(player)) {
            player.displayClientMessage(Component.translatable("message.trippycraft.market.too_far"), true);
            return;
        }

        MarketPlayerData data = MarketDataStore.getOrCreate(player);
        prepareMarket(player, data);

        switch (packet.action()) {
            case SELL_PRODUCT -> sellProduct(player, data, packet.entryId(), packet.amount());
            case BUY_MATERIAL -> buyMaterial(player, data, packet.entryId(), packet.amount());
            case COMPLETE_CONTRACT -> completeContract(player, data, packet.entryId());
        }

        ModNetwork.sendMarketSnapshot(player, createSnapshot(player, data, false));
    }

    public static MarketSnapshotPacket createSnapshot(ServerPlayer player, MarketPlayerData data, boolean openScreen) {
        prepareMarket(player, data);
        List<MarketSnapshotPacket.DemandInfo> demand = MarketRegistries.sellableProducts().stream()
                .map(product -> new MarketSnapshotPacket.DemandInfo(product.id(), data.demandMultiplier(product.id())))
                .toList();
        List<MarketSnapshotPacket.ContractInfo> contracts = data.activeContracts().stream()
                .filter(contract -> MarketRegistries.sellableProduct(contract.productId()).isPresent())
                .map(contract -> new MarketSnapshotPacket.ContractInfo(
                        contract.id(),
                        contract.productId(),
                        contract.type(),
                        contract.quantity(),
                        contract.rewardEmeralds(),
                        contract.reputationReward(),
                        contract.bonusMultiplier()
                ))
                .toList();
        long now = player.level().getGameTime();
        long nextRefresh = Math.max(0L, CONTRACT_REFRESH_TICKS - (now - data.lastContractRefreshGameTime()));
        return new MarketSnapshotPacket(
                openScreen,
                data.reputation(),
                data.level(),
                data.totalUnitsSold(),
                data.completedContracts(),
                nextRefresh,
                demand,
                contracts
        );
    }

    public static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (MarketStock.matches(stack, item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int sellPriceHundredths(MarketPlayerData data, SellableMarketProduct product) {
        return MarketMath.priceHundredths(product.baseEmeraldValue(), data.demandMultiplier(product.id()), data.level());
    }

    private static boolean isMarketName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("broker")
                || normalized.contains("fixer")
                || normalized.contains("supplier")
                || normalized.contains("market");
    }

    private static CompoundTag getTrippycraftData(Villager villager) {
        CompoundTag persistentData = villager.getPersistentData();
        return NbtCompat.getOrCreateCompound(persistentData, TrippyCraft.MOD_ID);
    }

    private static void prepareMarket(ServerPlayer player, MarketPlayerData data) {
        long now = player.level().getGameTime();
        ensureDemand(player.getRandom(), data, now);
        ensureContracts(player.getRandom(), data, now);
    }

    private static void ensureDemand(RandomSource random, MarketPlayerData data, long now) {
        boolean firstDemand = data.demandMultipliers().isEmpty();
        for (SellableMarketProduct product : MarketRegistries.sellableProducts()) {
            if (!data.demandMultipliers().containsKey(product.id())) {
                data.setDemandMultiplier(product.id(), 90 + random.nextInt(41));
            }
        }

        if (firstDemand || now - data.lastDemandRefreshGameTime() < DEMAND_REFRESH_TICKS) {
            if (data.lastDemandRefreshGameTime() <= 0L) {
                data.setLastDemandRefreshGameTime(now);
            }
            return;
        }

        for (SellableMarketProduct product : MarketRegistries.sellableProducts()) {
            int current = data.demandMultiplier(product.id());
            int pullTowardNormal = current > 112 ? -3 : current < 92 ? 3 : 0;
            int drift = random.nextInt(21) - 10 + pullTowardNormal;
            data.setDemandMultiplier(product.id(), current + drift);
        }
        data.setLastDemandRefreshGameTime(now);
    }

    private static void ensureContracts(RandomSource random, MarketPlayerData data, long now) {
        if (!data.activeContracts().isEmpty() && now - data.lastContractRefreshGameTime() < CONTRACT_REFRESH_TICKS) {
            return;
        }

        data.activeContracts().clear();
        for (int i = 0; i < CONTRACT_TARGET_COUNT; i++) {
            generateContract(random, data).ifPresent(data.activeContracts()::add);
        }
        data.setLastContractRefreshGameTime(now);
    }

    private static Optional<MarketContract> generateContract(RandomSource random, MarketPlayerData data) {
        List<SellableMarketProduct> eligible = new ArrayList<>(MarketRegistries.contractEligibleProducts(data.level()));
        if (eligible.isEmpty()) {
            return Optional.empty();
        }

        SellableMarketProduct product = chooseWeighted(random, eligible);
        ContractTemplate template = chooseTemplate(random, data.level());
        int priceHundredths = sellPriceHundredths(data, product);
        int baseReward = MarketMath.emeraldsFor(priceHundredths, template.quantity());
        int reward = Math.max(1, baseReward * template.multiplier() / 100);
        int reputationReward = Math.max(template.reputationReward(), reward / 2);
        return Optional.of(new MarketContract(
                data.nextContractId(),
                product.id(),
                template.name(),
                template.quantity(),
                reward,
                reputationReward,
                template.multiplier()
        ));
    }

    private static SellableMarketProduct chooseWeighted(RandomSource random, List<SellableMarketProduct> products) {
        int totalWeight = products.stream().mapToInt(SellableMarketProduct::demandWeight).sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        int cursor = 0;
        for (SellableMarketProduct product : products) {
            cursor += product.demandWeight();
            if (roll < cursor) {
                return product;
            }
        }
        return products.getLast();
    }

    private static ContractTemplate chooseTemplate(RandomSource random, int reputationLevel) {
        List<ContractTemplate> templates = new ArrayList<>();
        templates.add(new ContractTemplate("Small Order", 8, 130, 8));
        templates.add(new ContractTemplate("Steady Buyer", 16, 135, 14));
        if (reputationLevel >= 1) {
            templates.add(new ContractTemplate("Bulk Order", 64, 145, 38));
        }
        if (reputationLevel >= 3) {
            templates.add(new ContractTemplate("Premium Order", 32, 165, 52));
        }
        if (reputationLevel >= 5) {
            templates.add(new ContractTemplate("Warehouse Order", 128, 155, 80));
        }
        return templates.get(random.nextInt(templates.size()));
    }

    private static void sellProduct(ServerPlayer player, MarketPlayerData data, String productId, int requestedAmount) {
        Optional<SellableMarketProduct> maybeProduct = MarketRegistries.sellableProduct(productId);
        if (maybeProduct.isEmpty()) {
            return;
        }
        SellableMarketProduct product = maybeProduct.get();

        int available = countItem(player, product.item().get());
        int amount = requestedAmount <= 0 ? available : Math.min(requestedAmount, available);
        if (amount <= 0) {
            player.displayClientMessage(Component.translatable("message.trippycraft.market.no_product"), true);
            return;
        }

        int removed = removeItem(player, product.item().get(), amount);
        if (removed <= 0) {
            return;
        }

        int reward = MarketMath.emeraldsFor(sellPriceHundredths(data, product), removed);
        EmeraldPayoutLedger.queue(player, reward);
        data.addSoldUnits(removed);
        data.addReputation(Math.max(1, reward / 4));
        nudgeDemandAfterSale(data, product.id(), removed);
        player.displayClientMessage(Component.translatable("message.trippycraft.market.sold", removed, reward), true);
    }

    private static void buyMaterial(ServerPlayer player, MarketPlayerData data, String materialId, int requestedAmount) {
        Optional<BuyableMarketMaterial> maybeMaterial = MarketRegistries.buyableMaterial(materialId);
        if (maybeMaterial.isEmpty()) {
            return;
        }
        BuyableMarketMaterial material = maybeMaterial.get();

        int maxStack = Math.max(1, material.stack(1).getMaxStackSize());
        int amount = Math.clamp(requestedAmount, 1, maxStack);
        int cost = material.emeraldPricePerUnit() * amount;
        int emeralds = EmeraldWallet.value(player);
        if (emeralds < cost) {
            player.displayClientMessage(Component.translatable("message.trippycraft.market.no_emeralds", cost), true);
            return;
        }

        EmeraldWallet.spend(player, cost);
        giveItem(player, material.stack(amount));
        player.displayClientMessage(Component.translatable("message.trippycraft.market.bought", amount, cost), true);
    }

    private static void completeContract(ServerPlayer player, MarketPlayerData data, String contractId) {
        Optional<MarketContract> maybeContract = data.activeContracts().stream()
                .filter(contract -> contract.id().equals(contractId))
                .findFirst();
        if (maybeContract.isEmpty()) {
            return;
        }

        MarketContract contract = maybeContract.get();
        Optional<SellableMarketProduct> maybeProduct = MarketRegistries.sellableProduct(contract.productId());
        if (maybeProduct.isEmpty()) {
            data.activeContracts().remove(contract);
            return;
        }

        SellableMarketProduct product = maybeProduct.get();
        int available = countItem(player, product.item().get());
        if (available < contract.quantity()) {
            player.displayClientMessage(Component.translatable("message.trippycraft.market.contract_missing", contract.quantity()), true);
            return;
        }

        int removed = removeItem(player, product.item().get(), contract.quantity());
        if (removed < contract.quantity()) {
            return;
        }

        EmeraldPayoutLedger.queue(player, contract.rewardEmeralds());
        data.addSoldUnits(removed);
        data.addCompletedContract();
        data.addReputation(contract.reputationReward());
        nudgeDemandAfterSale(data, product.id(), removed);
        data.activeContracts().remove(contract);
        generateContract(player.getRandom(), data).ifPresent(data.activeContracts()::add);
        data.activeContracts().sort(Comparator.comparing(MarketContract::type).thenComparing(MarketContract::id));
        player.displayClientMessage(Component.translatable("message.trippycraft.market.contract_complete", contract.rewardEmeralds()), true);
    }

    private static int removeItem(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!MarketStock.matches(stack, item)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        return amount - remaining;
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        while (!stack.isEmpty()) {
            int count = Math.min(stack.getCount(), stack.getMaxStackSize());
            ItemStack split = stack.copyWithCount(count);
            stack.shrink(count);
            if (!player.addItem(split)) {
                player.drop(split, false);
            }
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }

    private static void nudgeDemandAfterSale(MarketPlayerData data, String productId, int quantity) {
        int current = data.demandMultiplier(productId);
        int pressure = quantity >= 64 ? 4 : quantity >= 16 ? 2 : 1;
        data.setDemandMultiplier(productId, current - pressure);
    }

    private record ContractTemplate(String name, int quantity, int multiplier, int reputationReward) {
    }
}
