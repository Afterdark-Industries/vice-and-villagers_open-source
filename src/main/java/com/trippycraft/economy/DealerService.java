package com.trippycraft.economy;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.TrippyCraft;
import com.trippycraft.network.DealerActionPacket;
import com.trippycraft.network.DealerSnapshotPacket;
import com.trippycraft.network.ModNetwork;
import com.trippycraft.world.TavernNightlifeService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

public final class DealerService {
    private static final String DEALER_KEY = "drug_dealer";
    private static final String AUTO_DEALER_KEY = "auto_drug_dealer";
    private static final double DEALER_USE_DISTANCE = 8.0D;

    private DealerService() {
    }

    public static boolean isDealerVillager(Villager villager) {
        if (villager.isBaby() || BartenderService.isBartender(villager)) {
            return false;
        }
        return NbtCompat.getBooleanOr(getTrippycraftData(villager), DEALER_KEY, false)
                || villager.hasCustomName()
                && villager.getCustomName() != null
                && isDealerName(villager.getCustomName().getString());
    }

    public static boolean isAutomaticDealerCandidate(Villager villager) {
        return villager.isAlive()
                && !villager.isBaby()
                && !BartenderService.isBartender(villager)
                && !TavernNightlifeService.isNightlifeGuest(villager)
                && !isDealerVillager(villager)
                && villager.getVillagerData().getProfession() != VillagerProfession.NITWIT
                && !hasReservedMarketName(villager);
    }

    public static void markDealer(Villager villager) {
        CompoundTag data = getTrippycraftData(villager);
        data.putBoolean(DEALER_KEY, true);
        data.putBoolean(AUTO_DEALER_KEY, true);
        if (!villager.hasCustomName()) {
            villager.setCustomName(Component.literal("Dealer"));
            villager.setCustomNameVisible(false);
        }
    }

    public static boolean isAutoDealer(Villager villager) {
        CompoundTag data = getTrippycraftData(villager);
        if (NbtCompat.getBooleanOr(data, AUTO_DEALER_KEY, false)) {
            return true;
        }
        return NbtCompat.getBooleanOr(data, DEALER_KEY, false)
                && villager.hasCustomName()
                && villager.getCustomName() != null
                && villager.getCustomName().getString().equals("Dealer");
    }

    public static void unmarkAutoDealer(Villager villager) {
        if (!isAutoDealer(villager)) {
            return;
        }
        CompoundTag data = getTrippycraftData(villager);
        data.remove(DEALER_KEY);
        data.remove(AUTO_DEALER_KEY);
        if (villager.hasCustomName()
                && villager.getCustomName() != null
                && villager.getCustomName().getString().equals("Dealer")) {
            villager.setCustomName(null);
        }
    }

    public static boolean hasDealerNearby(ServerPlayer player) {
        AABB searchArea = player.getBoundingBox().inflate(DEALER_USE_DISTANCE);
        return !player.level().getEntitiesOfClass(Villager.class, searchArea, DealerService::isDealerVillager).isEmpty();
    }

    public static void openDealer(ServerPlayer player) {
        MarketPlayerData data = MarketDataStore.getOrCreate(player);
        ModNetwork.sendDealerSnapshot(player, createSnapshot(player, data, true));
    }

    public static void handleDealerAction(ServerPlayer player, DealerActionPacket packet) {
        if (!hasDealerNearby(player)) {
            player.displayClientMessage(Component.translatable("message.trippycraft.dealer.too_far"), true);
            return;
        }

        MarketPlayerData data = MarketDataStore.getOrCreate(player);
        Optional<DealerProduct> maybeProduct = DealerRegistries.product(packet.productId());
        if (maybeProduct.isEmpty()) {
            return;
        }

        DealerProduct product = maybeProduct.get();
        int maxStack = product.stack(1).getMaxStackSize();
        int amount = Math.clamp(packet.amount(), 1, maxStack);
        int discountPercent = com.trippycraft.substance.SubstanceEngine.mdmaSocialDiscount(player);
        int cost = discountedUnitPrice(product, discountPercent) * amount;
        int emeralds = EmeraldWallet.value(player);
        if (emeralds < cost) {
            player.displayClientMessage(Component.translatable("message.trippycraft.market.no_emeralds", cost), true);
            ModNetwork.sendDealerSnapshot(player, createSnapshot(player, data, false));
            return;
        }

        EmeraldWallet.spend(player, cost);
        giveItem(player, product.stack(amount));
        player.displayClientMessage(Component.translatable("message.trippycraft.dealer.bought", amount, cost), true);
        ModNetwork.sendDealerSnapshot(player, createSnapshot(player, data, false));
    }

    public static DealerSnapshotPacket createSnapshot(ServerPlayer player, MarketPlayerData data, boolean openScreen) {
        return new DealerSnapshotPacket(openScreen, data.reputation(), data.level(),
                com.trippycraft.substance.SubstanceEngine.mdmaSocialDiscount(player));
    }

    private static int discountedUnitPrice(DealerProduct product, int discountPercent) {
        return Math.max(1, (int) Math.ceil(product.emeraldPricePerUnit() * (100 - discountPercent) / 100.0D));
    }

    public static Comparator<Villager> dealerCandidateComparator() {
        return Comparator
                .comparing((Villager villager) -> villager.getVillagerData().getProfession() == VillagerProfession.NITWIT)
                .thenComparing(Villager::hasCustomName)
                .thenComparing(Villager::getUUID);
    }

    private static boolean isDealerName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.contains("dealer")
                || normalized.contains("plug")
                || normalized.contains("street seller");
    }

    private static boolean hasReservedMarketName(Villager villager) {
        if (!villager.hasCustomName() || villager.getCustomName() == null) {
            return false;
        }
        String normalized = villager.getCustomName().getString().toLowerCase(Locale.ROOT);
        return normalized.contains("broker")
                || normalized.contains("fixer")
                || normalized.contains("supplier")
                || normalized.contains("market");
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

    private static CompoundTag getTrippycraftData(Villager villager) {
        CompoundTag persistentData = villager.getPersistentData();
        return NbtCompat.getOrCreateCompound(persistentData, TrippyCraft.MOD_ID);
    }
}
