package com.trippycraft.economy;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.TrippyCraft;
import com.trippycraft.network.CustomerActionPacket;
import com.trippycraft.network.CustomerSnapshotPacket;
import com.trippycraft.network.ModNetwork;
import com.trippycraft.world.TavernNightlifeService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class CustomerService {
    private static final String CUSTOMER_KEY = "market_customer";
    private static final String AUTO_CUSTOMER_KEY = "auto_market_customer";
    private static final String HABITS_KEY = "customer_product_habits";
    private static final String LAST_SAMPLE_TICK_KEY = "customer_last_sample_tick";
    private static final String OFFER_DAY_KEY = "customer_offer_day";
    private static final String DAILY_OFFERS_KEY = "customer_daily_offers";
    private static final double CUSTOMER_USE_DISTANCE = 8.0D;
    private static final int OFFER_COUNT = 4;
    private static final int MAX_HABIT_LEVEL = 100;
    private static final int SAMPLE_COOLDOWN_TICKS = 40;

    private CustomerService() {
    }

    public static boolean isCustomerVillager(Villager villager) {
        if (villager.isBaby() || BartenderService.isBartender(villager) || DealerService.isDealerVillager(villager)) {
            return false;
        }
        return NbtCompat.getBooleanOr(getTrippycraftData(villager), CUSTOMER_KEY, false)
                || villager.hasCustomName()
                && villager.getCustomName() != null
                && isCustomerName(villager.getCustomName().getString());
    }

    public static boolean isAutomaticCustomerCandidate(Villager villager) {
        return villager.isAlive()
                && !villager.isBaby()
                && !BartenderService.isBartender(villager)
                && !TavernNightlifeService.isNightlifeGuest(villager)
                && !isCustomerVillager(villager)
                && !DealerService.isDealerVillager(villager)
                && !MarketService.isMarketVillager(villager);
    }

    public static void markCustomer(Villager villager) {
        CompoundTag data = getTrippycraftData(villager);
        data.putBoolean(CUSTOMER_KEY, true);
        data.putBoolean(AUTO_CUSTOMER_KEY, true);
    }

    public static boolean isAutoCustomer(Villager villager) {
        CompoundTag data = getTrippycraftData(villager);
        return NbtCompat.getBooleanOr(data, AUTO_CUSTOMER_KEY, false)
                || NbtCompat.getBooleanOr(data, CUSTOMER_KEY, false)
                && (!villager.hasCustomName()
                || villager.getCustomName() == null
                || !isCustomerName(villager.getCustomName().getString()));
    }

    public static void unmarkAutoCustomer(Villager villager) {
        if (!isAutoCustomer(villager)) {
            return;
        }
        CompoundTag data = getTrippycraftData(villager);
        data.remove(CUSTOMER_KEY);
        data.remove(AUTO_CUSTOMER_KEY);
    }

    public static Comparator<Villager> customerCandidateComparator() {
        return Comparator
                .comparing(Villager::hasCustomName)
                .thenComparing(Villager::getUUID);
    }

    public static void openCustomer(ServerPlayer player, Villager customer) {
        EmeraldPayoutLedger.flush(player);
        MarketPlayerData data = MarketDataStore.getOrCreate(player);
        ModNetwork.sendCustomerSnapshot(player, createSnapshot(player, customer, data, true));
    }

    public static void handleCustomerAction(ServerPlayer player, CustomerActionPacket packet) {
        if (packet.isCloseRequest()) {
            EmeraldPayoutLedger.flush(player);
            return;
        }

        Optional<Villager> maybeCustomer = findNearbyCustomer(player, packet.customerId());
        if (maybeCustomer.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.trippycraft.customer.too_far"), true);
            return;
        }

        MarketPlayerData data = MarketDataStore.getOrCreate(player);
        Villager customer = maybeCustomer.get();
        Optional<CustomerOffer> maybeOffer = offersFor(player, customer, data).stream()
                .filter(offer -> offer.id().equals(packet.offerId()))
                .findFirst();
        if (maybeOffer.isEmpty()) {
            ModNetwork.sendCustomerSnapshot(player, createSnapshot(player, customer, data, false));
            return;
        }

        CustomerOffer offer = maybeOffer.get();
        Optional<DealerProduct> maybeProduct = DealerRegistries.product(offer.productId());
        if (maybeProduct.isEmpty()) {
            return;
        }

        DealerProduct product = maybeProduct.get();
        int available = countItem(player, product.item().get());
        if (available <= 0) {
            player.displayClientMessage(Component.translatable("message.trippycraft.customer.missing", offer.quantity()), true);
            ModNetwork.sendCustomerSnapshot(player, createSnapshot(player, customer, data, false));
            return;
        }

        int amountToSell = Math.min(available, offer.quantity());
        int removed = removeItem(player, product.item().get(), amountToSell);
        if (removed <= 0) {
            return;
        }

        boolean completedNeed = removed >= offer.quantity();
        int completionTip = completedNeed ? completionTip(offer) : 0;
        int reward = completedNeed ? offer.rewardEmeralds() + completionTip : Math.max(1, offer.unitPrice() * removed);
        int reputationReward = reputationRewardForSale(offer, removed, completedNeed);
        EmeraldPayoutLedger.queue(player, reward);
        updateCachedOfferAfterSale(customer, offer, removed);
        data.addSoldUnits(removed);
        data.addReputation(reputationReward);
        MarketRegistries.sellableProduct(offer.productId())
                .ifPresent(sellable -> data.setDemandMultiplier(sellable.id(), data.demandMultiplier(sellable.id()) - 1));
        recordPurchase(customer, product, removed, player.level().getGameTime());
        if (completionTip > 0) {
            player.displayClientMessage(Component.translatable("message.trippycraft.customer.sold_tip", removed, reward, completionTip), true);
        } else {
            player.displayClientMessage(Component.translatable("message.trippycraft.customer.sold", removed, reward), true);
        }
        ModNetwork.sendCustomerSnapshot(player, createSnapshot(player, customer, data, false));
    }

    public static boolean canAcceptSample(Villager customer, long gameTime) {
        if (!isCustomerVillager(customer)) {
            return false;
        }
        long lastSampleTick = NbtCompat.getLongOr(getTrippycraftData(customer), LAST_SAMPLE_TICK_KEY, Long.MIN_VALUE);
        return lastSampleTick == Long.MIN_VALUE || gameTime - lastSampleTick >= SAMPLE_COOLDOWN_TICKS;
    }

    public static boolean acceptSample(Villager customer, DealerProduct product, long gameTime) {
        if (!canAcceptSample(customer, gameTime)) {
            return false;
        }

        recordExposure(customer, product, 1, gameTime, true);
        getTrippycraftData(customer).putLong(LAST_SAMPLE_TICK_KEY, gameTime);
        return true;
    }

    public static void recordPurchase(Villager customer, DealerProduct product, int quantity, long gameTime) {
        if (!isCustomerVillager(customer) || quantity <= 0) {
            return;
        }
        recordExposure(customer, product, quantity, gameTime, false);
    }

    public static int sampleCooldownTicks() {
        return SAMPLE_COOLDOWN_TICKS;
    }

    public static CustomerSnapshotPacket createSnapshot(ServerPlayer player, Villager customer, MarketPlayerData data, boolean openScreen) {
        List<CustomerSnapshotPacket.OfferInfo> offerInfos = offersFor(player, customer, data).stream()
                .map(offer -> new CustomerSnapshotPacket.OfferInfo(
                        offer.id(),
                        offer.productId(),
                        offer.requestType(),
                        offer.quantity(),
                        offer.rewardEmeralds(),
                        offer.reputationReward(),
                        offer.unitPrice(),
                        offer.habitLevel()
                ))
                .toList();
        long gameDay = Math.max(0L, player.level().getGameTime() / 24_000L);
        return new CustomerSnapshotPacket(openScreen, customer.getUUID(), data.reputation(), data.level(), gameDay, offerInfos);
    }

    public static List<CustomerOffer> offersFor(ServerPlayer player, Villager customer, MarketPlayerData data) {
        long gameDay = gameDay(player);
        ensureDailyOffers(player, customer, data, gameDay);
        return cachedOffers(customer);
    }

    private static List<CustomerOffer> generateDailyOffers(ServerPlayer player, Villager customer, MarketPlayerData data, long gameDay) {
        int level = Math.max(0, data.level());
        List<WeightedProduct> weightedProducts = DealerRegistries.products().stream()
                .map(product -> weightedProduct(customer, product, level))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        if (weightedProducts.isEmpty()) {
            return List.of();
        }

        long seed = customer.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(customer.getUUID().getLeastSignificantBits(), 17)
                ^ gameDay * 341_873_128_712L
                ^ data.level() * 13_249L
                ^ habitSeed(customer);
        RandomSource random = RandomSource.create(seed);

        List<WeightedProduct> pool = new ArrayList<>(weightedProducts);
        List<CustomerOffer> offers = new ArrayList<>();
        for (int i = 0; i < OFFER_COUNT && !pool.isEmpty(); i++) {
            DealerProduct product = removeWeighted(random, pool).product();
            int habitLevel = habitInfluence(customer, product);
            int quantity = quantityFor(random, product, data.level(), habitLevel);
            int unitPrice = customerUnitPrice(random, product, data, habitLevel);
            String type = requestType(random, quantity, habitLevel);
            int reward = Math.max(1, unitPrice * quantity);
            int reputationReward = Math.max(1, reward / 5 + habitLevel / 30);
            offers.add(new CustomerOffer("offer_" + i, product.id(), type, quantity, reward, reputationReward, unitPrice, habitLevel));
        }
        return offers;
    }

    private static void ensureDailyOffers(ServerPlayer player, Villager customer, MarketPlayerData data, long gameDay) {
        CompoundTag customerData = getTrippycraftData(customer);
        if (NbtCompat.getLongOr(customerData, OFFER_DAY_KEY, Long.MIN_VALUE) == gameDay
                && NbtCompat.hasCompound(customerData, DAILY_OFFERS_KEY)) {
            return;
        }

        CompoundTag offersTag = new CompoundTag();
        for (CustomerOffer offer : generateDailyOffers(player, customer, data, gameDay)) {
            offersTag.put(offer.id(), saveOffer(offer));
        }
        customerData.putLong(OFFER_DAY_KEY, gameDay);
        customerData.put(DAILY_OFFERS_KEY, offersTag);
    }

    private static List<CustomerOffer> cachedOffers(Villager customer) {
        Optional<CompoundTag> maybeOffers = NbtCompat.getOptionalCompound(getTrippycraftData(customer), DAILY_OFFERS_KEY);
        if (maybeOffers.isEmpty()) {
            return List.of();
        }

        CompoundTag offersTag = maybeOffers.get();
        List<CustomerOffer> offers = new ArrayList<>();
        for (int i = 0; i < OFFER_COUNT; i++) {
            String id = "offer_" + i;
            NbtCompat.getOptionalCompound(offersTag, id)
                    .flatMap(tag -> loadOffer(id, tag))
                    .filter(offer -> offer.quantity() > 0)
                    .ifPresent(offers::add);
        }
        return offers;
    }

    private static CompoundTag saveOffer(CustomerOffer offer) {
        CompoundTag tag = new CompoundTag();
        tag.putString("productId", offer.productId());
        tag.putString("type", offer.requestType());
        tag.putInt("quantity", Math.max(0, offer.quantity()));
        tag.putInt("rewardEmeralds", Math.max(0, offer.rewardEmeralds()));
        tag.putInt("reputationReward", Math.max(0, offer.reputationReward()));
        tag.putInt("unitPrice", Math.max(1, offer.unitPrice()));
        tag.putInt("habitLevel", Math.max(0, Math.min(MAX_HABIT_LEVEL, offer.habitLevel())));
        return tag;
    }

    private static Optional<CustomerOffer> loadOffer(String id, CompoundTag tag) {
        String productId = NbtCompat.getStringOr(tag, "productId", "");
        if (DealerRegistries.product(productId).isEmpty()) {
            return Optional.empty();
        }

        int quantity = Math.max(0, NbtCompat.getIntOr(tag, "quantity", 0));
        if (quantity <= 0) {
            return Optional.empty();
        }

        int unitPrice = Math.max(1, NbtCompat.getIntOr(tag, "unitPrice", 1));
        int reward = Math.max(1, NbtCompat.getIntOr(tag, "rewardEmeralds", Math.max(1, unitPrice * quantity)));
        int reputationReward = Math.max(1, NbtCompat.getIntOr(tag, "reputationReward", Math.max(1, reward / 5)));
        return Optional.of(new CustomerOffer(
                id,
                productId,
                NbtCompat.getStringOr(tag, "type", "Private Order"),
                quantity,
                reward,
                reputationReward,
                unitPrice,
                Math.max(0, Math.min(MAX_HABIT_LEVEL, NbtCompat.getIntOr(tag, "habitLevel", 0)))
        ));
    }

    private static void updateCachedOfferAfterSale(Villager customer, CustomerOffer offer, int sold) {
        Optional<CompoundTag> maybeOffers = NbtCompat.getOptionalCompound(getTrippycraftData(customer), DAILY_OFFERS_KEY);
        if (maybeOffers.isEmpty()) {
            return;
        }

        CompoundTag offersTag = maybeOffers.get();
        int remaining = Math.max(0, offer.quantity() - Math.max(0, sold));
        if (remaining <= 0) {
            offersTag.remove(offer.id());
            return;
        }

        int remainingReward = Math.max(1, offer.unitPrice() * remaining);
        int remainingReputation = Math.max(1, offer.reputationReward() * remaining / Math.max(1, offer.quantity()));
        CustomerOffer updated = new CustomerOffer(
                offer.id(),
                offer.productId(),
                offer.requestType(),
                remaining,
                remainingReward,
                remainingReputation,
                offer.unitPrice(),
                offer.habitLevel()
        );
        offersTag.put(offer.id(), saveOffer(updated));
    }

    private static Optional<Villager> findNearbyCustomer(ServerPlayer player, UUID customerId) {
        AABB searchArea = player.getBoundingBox().inflate(CUSTOMER_USE_DISTANCE);
        return player.level().getEntitiesOfClass(Villager.class, searchArea, CustomerService::isCustomerVillager)
                .stream()
                .filter(customer -> customer.getUUID().equals(customerId))
                .findFirst();
    }

    private static Optional<WeightedProduct> weightedProduct(Villager customer, DealerProduct product, int level) {
        int exactHabit = habitLevel(customer, product.id());
        int categoryHabit = categoryHabitLevel(customer, product.category());
        int habitLevel = Math.max(exactHabit, categoryHabit);
        int unlockReach = level + unlockBonus(habitLevel);
        if (product.unlockLevel() > unlockReach) {
            return Optional.empty();
        }

        int weight = 12 + Math.max(0, level - product.unlockLevel()) * 2;
        if (exactHabit > 0) {
            weight += exactHabit * 4 + Math.min(exposureCount(customer, product.id()), Math.max(1, exactHabit / 2));
        }
        if (categoryHabit > 0) {
            weight += categoryHabit * (product.unlockLevel() > level ? 4 : 2);
        }
        if (product.unlockLevel() > level) {
            weight += 8 + categoryHabit * 2;
        }
        return Optional.of(new WeightedProduct(product, Math.max(1, Math.min(600, weight))));
    }

    private static WeightedProduct removeWeighted(RandomSource random, List<WeightedProduct> pool) {
        int totalWeight = pool.stream().mapToInt(WeightedProduct::weight).sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        for (int i = 0; i < pool.size(); i++) {
            WeightedProduct candidate = pool.get(i);
            roll -= candidate.weight();
            if (roll < 0) {
                return pool.remove(i);
            }
        }
        return pool.remove(pool.size() - 1);
    }

    private static int quantityFor(RandomSource random, DealerProduct product, int level, int habitLevel) {
        int maxStack = product.stack(1).getMaxStackSize();
        int[] quantities = habitLevel >= 80 || level >= 5 ? new int[]{1, 2, 4, 8, 16, 32, 64}
                : habitLevel >= 55 || level >= 4 ? new int[]{1, 2, 4, 8, 16, 32}
                : habitLevel >= 25 || level >= 2 ? new int[]{1, 2, 4, 8, 16}
                : new int[]{1, 2, 4};
        int quantity = quantities[random.nextInt(quantities.length)];
        if (habitLevel >= 55 && quantities.length > 3 && random.nextInt(100) < habitLevel) {
            quantity = Math.max(quantity, quantities[quantities.length - 1 - random.nextInt(2)]);
        }
        return Math.max(1, Math.min(quantity, maxStack));
    }

    private static int customerUnitPrice(RandomSource random, DealerProduct dealerProduct, MarketPlayerData data, int habitLevel) {
        int dealerPrice = dealerProduct.emeraldPricePerUnit();
        int brokerPrice = MarketRegistries.sellableProduct(dealerProduct.id())
                .map(product -> MarketMath.emeraldsFor(MarketService.sellPriceHundredths(data, product), 1))
                .orElse(Math.max(1, dealerPrice * 2 / 3));
        int premium = 118 + random.nextInt(28) + Math.min(24, habitLevel / 4);
        int target = Math.max(brokerPrice + 1, brokerPrice * premium / 100);
        int noArbitrageCap = Math.max(1, dealerPrice - 1);
        return Math.max(1, Math.min(target, noArbitrageCap));
    }

    private static String requestType(RandomSource random, int quantity, int habitLevel) {
        if (habitLevel >= 80) {
            return quantity >= 16 ? "Compulsive Bulk Order" : "Compulsive Craving";
        }
        if (habitLevel >= 55) {
            return quantity >= 8 ? "Habit Order" : "Strong Craving";
        }
        if (habitLevel >= 25) {
            return random.nextBoolean() ? "Craving" : "Repeat Buyer";
        }
        if (quantity >= 8) {
            return random.nextBoolean() ? "Party Request" : "Regular Buyer";
        }
        return random.nextBoolean() ? "Quick Sale" : "Private Order";
    }

    private static int completionTip(CustomerOffer offer) {
        return Math.max(1, offer.rewardEmeralds() / 8);
    }

    private static int reputationRewardForSale(CustomerOffer offer, int sold, boolean completedNeed) {
        if (completedNeed) {
            return offer.reputationReward();
        }
        return Math.max(1, offer.reputationReward() * sold / Math.max(1, offer.quantity()));
    }

    private static int unlockBonus(int habitLevel) {
        if (habitLevel >= 80) {
            return 3;
        }
        if (habitLevel >= 55) {
            return 2;
        }
        if (habitLevel >= 25) {
            return 1;
        }
        return 0;
    }

    private static int habitInfluence(Villager customer, DealerProduct product) {
        return Math.max(habitLevel(customer, product.id()), categoryHabitLevel(customer, product.category()));
    }

    private static void recordExposure(Villager customer, DealerProduct product, int quantity, long gameTime, boolean sample) {
        CompoundTag data = getTrippycraftData(customer);
        CompoundTag habit = getOrCreateHabit(data, product.id());
        CustomerAddictionProfile profile = product.addictionProfile();
        int oldLevel = profile.clampLevel(NbtCompat.getIntOr(habit, "level", 0));
        int growth;
        if (sample) {
            int samples = safeAddInt(NbtCompat.getIntOr(habit, "samples", 0), 1);
            habit.putInt("samples", samples);
            habit.putLong("lastSampleGameTime", gameTime);
            habit.putLong("lastSampleDay", Math.max(0L, gameTime / 24_000L));
            growth = profile.sampleGrowth(oldLevel, samples);
        } else {
            int purchases = safeAddInt(NbtCompat.getIntOr(habit, "purchases", 0), 1);
            int purchasedUnits = safeAddInt(NbtCompat.getIntOr(habit, "purchasedUnits", 0), Math.max(1, quantity));
            habit.putInt("purchases", purchases);
            habit.putInt("purchasedUnits", purchasedUnits);
            habit.putLong("lastPurchaseGameTime", gameTime);
            habit.putLong("lastPurchaseDay", Math.max(0L, gameTime / 24_000L));
            growth = profile.purchaseGrowth(oldLevel, purchases, Math.max(1, quantity));
        }

        habit.putInt("level", profile.clampLevel(oldLevel + growth));
        habit.putString("category", product.category());
        habit.putString("profile", profile.id());
        habit.putInt("profileMax", profile.maxHabitLevel());
    }

    private static int habitLevel(Villager customer, String productId) {
        return getHabit(customer, productId)
                .map(tag -> DealerRegistries.product(productId)
                        .map(product -> product.addictionProfile().clampLevel(NbtCompat.getIntOr(tag, "level", 0)))
                        .orElse(Math.max(0, Math.min(MAX_HABIT_LEVEL, NbtCompat.getIntOr(tag, "level", 0)))))
                .orElse(0);
    }

    private static int exposureCount(Villager customer, String productId) {
        return getHabit(customer, productId)
                .map(tag -> safeAddInt(Math.max(0, NbtCompat.getIntOr(tag, "samples", 0)), Math.max(0, NbtCompat.getIntOr(tag, "purchases", 0))))
                .orElse(0);
    }

    private static int categoryHabitLevel(Villager customer, String category) {
        return DealerRegistries.productsInCategory(category).stream()
                .mapToInt(product -> habitLevel(customer, product.id()))
                .max()
                .orElse(0);
    }

    private static long habitSeed(Villager customer) {
        long seed = 0L;
        for (DealerProduct product : DealerRegistries.products()) {
            int level = habitLevel(customer, product.id());
            if (level > 0) {
                seed ^= (long) product.id().hashCode() * 31L + level * 17L + exposureCount(customer, product.id());
            }
        }
        return seed;
    }

    private static Optional<CompoundTag> getHabit(Villager customer, String productId) {
        return NbtCompat.getOptionalCompound(getTrippycraftData(customer), HABITS_KEY)
                .flatMap(habits -> NbtCompat.getOptionalCompound(habits, productId));
    }

    private static CompoundTag getOrCreateHabit(CompoundTag data, String productId) {
        CompoundTag habits = NbtCompat.getOrCreateCompound(data, HABITS_KEY);
        return NbtCompat.getOrCreateCompound(habits, productId);
    }

    private record WeightedProduct(DealerProduct product, int weight) {
    }

    private static int safeAddInt(int left, int right) {
        if (right <= 0) {
            return Math.max(0, left);
        }
        return left >= Integer.MAX_VALUE - right ? Integer.MAX_VALUE : Math.max(0, left + right);
    }

    private static boolean isCustomerName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("customer")
                || normalized.contains("buyer")
                || normalized.contains("regular");
    }

    private static long gameDay(ServerPlayer player) {
        return Math.max(0L, player.level().getGameTime() / 24_000L);
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (DealerRegistries.matchesProductItem(stack, item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int removeItem(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!DealerRegistries.matchesProductItem(stack, item)) {
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

    private static CompoundTag getTrippycraftData(Villager villager) {
        CompoundTag persistentData = villager.getPersistentData();
        return NbtCompat.getOrCreateCompound(persistentData, TrippyCraft.MOD_ID);
    }
}
