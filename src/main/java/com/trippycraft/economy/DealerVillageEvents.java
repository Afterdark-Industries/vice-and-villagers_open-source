package com.trippycraft.economy;

import net.neoforged.neoforge.event.tick.LevelTickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import com.trippycraft.world.TavernNightlifeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DealerVillageEvents {
    private static final long ECONOMY_SCAN_INTERVAL_TICKS = 200L;
    private static final int SCAN_RADIUS = 128;
    private static final double VILLAGE_CLUSTER_LINK_DISTANCE = 96.0D;
    private static final int LARGE_DEALER_VILLAGE_MIN_ADULTS = 10;
    private static final int LARGE_BROKER_VILLAGE_MIN_ADULTS = 12;
    private static final int MAX_CUSTOMERS_PER_VILLAGE = 7;

    private DealerVillageEvents() {
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % ECONOMY_SCAN_INTERVAL_TICKS != 0L || level.players().isEmpty()) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (player.isAlive()) {
                ensureDealersAround(level, player.blockPosition());
            }
        }
    }

    private static void ensureDealersAround(ServerLevel level, BlockPos center) {
        AABB searchArea = new AABB(center).inflate(SCAN_RADIUS, 32.0D, SCAN_RADIUS);
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, searchArea, villager ->
                villager.isAlive()
                        && !villager.isBaby()
                        && !TavernNightlifeService.isNightlifeGuest(villager)
                        && isLoaded(level, villager.blockPosition())
                        && level.isVillage(villager.blockPosition()));
        if (villagers.isEmpty()) {
            return;
        }

        Map<UUID, Villager> remaining = new HashMap<>();
        for (Villager villager : villagers) {
            remaining.put(villager.getUUID(), villager);
        }

        while (!remaining.isEmpty()) {
            Villager anchor = remaining.values().iterator().next();
            List<Villager> cluster = connectedCluster(anchor, remaining);
            if (cluster.isEmpty()) {
                continue;
            }

            int targetDealers = targetDealerCount(cluster);
            int targetBrokers = targetBrokerCount(cluster);
            int targetCustomers = targetCustomerCount(cluster, targetDealers, targetBrokers);
            addMissingDealers(cluster, targetDealers);
            addMissingBrokers(cluster, targetBrokers);
            addMissingCustomers(cluster, targetCustomers);
        }
    }

    private static List<Villager> connectedCluster(Villager anchor, Map<UUID, Villager> remaining) {
        List<Villager> cluster = new ArrayList<>();
        List<Villager> frontier = new ArrayList<>();
        frontier.add(anchor);
        remaining.remove(anchor.getUUID());

        for (int index = 0; index < frontier.size(); index++) {
            Villager current = frontier.get(index);
            cluster.add(current);

            List<UUID> linked = new ArrayList<>();
            for (Map.Entry<UUID, Villager> entry : remaining.entrySet()) {
                if (current.distanceToSqr(entry.getValue()) <= VILLAGE_CLUSTER_LINK_DISTANCE * VILLAGE_CLUSTER_LINK_DISTANCE) {
                    linked.add(entry.getKey());
                }
            }

            for (UUID linkedId : linked) {
                Villager linkedVillager = remaining.remove(linkedId);
                if (linkedVillager != null) {
                    frontier.add(linkedVillager);
                }
            }
        }

        return cluster;
    }

    private static int targetDealerCount(List<Villager> cluster) {
        return cluster.size() >= LARGE_DEALER_VILLAGE_MIN_ADULTS ? 2 : 1;
    }

    private static int targetBrokerCount(List<Villager> cluster) {
        return cluster.size() >= LARGE_BROKER_VILLAGE_MIN_ADULTS ? 2 : 1;
    }

    private static int targetCustomerCount(List<Villager> cluster, int targetDealers, int targetBrokers) {
        int adultCount = cluster.size();
        int desired = adultCount < 5
                ? Math.max(0, adultCount - targetDealers - targetBrokers)
                : Math.min(MAX_CUSTOMERS_PER_VILLAGE, Math.max(2, (int) Math.round(adultCount * 0.35D)));
        int reservedNormalVillagers = adultCount >= 5 ? Math.max(1, adultCount / 4) : 0;
        int maxCustomers = Math.max(0, adultCount - targetDealers - targetBrokers - reservedNormalVillagers);
        return Math.min(desired, maxCustomers);
    }

    private static void addMissingDealers(List<Villager> cluster, int targetDealers) {
        long dealerCount = cluster.stream().filter(DealerService::isDealerVillager).count();
        int missingDealers = Math.max(0, targetDealers - (int) dealerCount);
        if (missingDealers <= 0) {
            return;
        }

        List<Villager> candidates = cluster.stream()
                .filter(DealerService::isAutomaticDealerCandidate)
                .sorted(DealerService.dealerCandidateComparator())
                .toList();
        for (int i = 0; i < Math.min(missingDealers, candidates.size()); i++) {
            DealerService.markDealer(candidates.get(i));
        }
    }

    private static void addMissingBrokers(List<Villager> cluster, int targetBrokers) {
        long brokerCount = cluster.stream().filter(MarketService::isMarketVillager).count();
        int missingBrokers = Math.max(0, targetBrokers - (int) brokerCount);
        if (missingBrokers <= 0) {
            return;
        }

        List<Villager> candidates = cluster.stream()
                .filter(MarketService::isAutomaticBrokerCandidate)
                .sorted(MarketService.brokerCandidateComparator())
                .toList();
        for (int i = 0; i < Math.min(missingBrokers, candidates.size()); i++) {
            MarketService.markBroker(candidates.get(i));
        }
    }

    private static void addMissingCustomers(List<Villager> cluster, int targetCustomers) {
        long customerCount = cluster.stream().filter(CustomerService::isCustomerVillager).count();
        int missingCustomers = Math.max(0, targetCustomers - (int) customerCount);
        if (missingCustomers <= 0) {
            return;
        }

        List<Villager> candidates = cluster.stream()
                .filter(CustomerService::isAutomaticCustomerCandidate)
                .sorted(CustomerService.customerCandidateComparator())
                .toList();
        for (int i = 0; i < Math.min(missingCustomers, candidates.size()); i++) {
            CustomerService.markCustomer(candidates.get(i));
        }
    }

    private static boolean isLoaded(ServerLevel level, BlockPos pos) {
        return level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }
}
