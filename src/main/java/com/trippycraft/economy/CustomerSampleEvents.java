package com.trippycraft.economy;

import net.neoforged.neoforge.event.tick.LevelTickEvent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class CustomerSampleEvents {
    private static final long SAMPLE_SCAN_INTERVAL_TICKS = 10L;
    private static final int SCAN_RADIUS = 96;
    private static final double CUSTOMER_SAMPLE_REACH = 2.35D;

    private CustomerSampleEvents() {
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime % SAMPLE_SCAN_INTERVAL_TICKS != 0L || level.players().isEmpty()) {
            return;
        }

        Set<UUID> scannedCustomers = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive()) {
                scanCustomersNear(level, player, gameTime, scannedCustomers);
            }
        }
    }

    private static void scanCustomersNear(ServerLevel level, ServerPlayer player, long gameTime, Set<UUID> scannedCustomers) {
        AABB searchArea = player.getBoundingBox().inflate(SCAN_RADIUS, 32.0D, SCAN_RADIUS);
        List<Villager> customers = level.getEntitiesOfClass(Villager.class, searchArea, CustomerService::isCustomerVillager);
        for (Villager customer : customers) {
            if (scannedCustomers.add(customer.getUUID())) {
                tryAcceptNearbySample(level, customer, gameTime);
            }
        }
    }

    private static void tryAcceptNearbySample(ServerLevel level, Villager customer, long gameTime) {
        if (!CustomerService.canAcceptSample(customer, gameTime)) {
            return;
        }

        AABB pickupArea = customer.getBoundingBox().inflate(CUSTOMER_SAMPLE_REACH, 1.15D, CUSTOMER_SAMPLE_REACH);
        Optional<ItemEntity> maybeSample = level.getEntitiesOfClass(ItemEntity.class, pickupArea, itemEntity ->
                        itemEntity.isAlive()
                                && !itemEntity.getItem().isEmpty()
                                && DealerRegistries.productFor(itemEntity.getItem()).isPresent())
                .stream()
                .min(Comparator.comparingDouble(customer::distanceToSqr));
        if (maybeSample.isEmpty()) {
            return;
        }

        ItemEntity itemEntity = maybeSample.get();
        Optional<DealerProduct> maybeProduct = DealerRegistries.productFor(itemEntity.getItem());
        if (maybeProduct.isEmpty() || !CustomerService.acceptSample(customer, maybeProduct.get(), gameTime)) {
            return;
        }

        consumeOne(itemEntity);
        playSampleFeedback(level, customer);
    }

    private static void consumeOne(ItemEntity itemEntity) {
        ItemStack remaining = itemEntity.getItem().copy();
        remaining.shrink(1);
        if (remaining.isEmpty()) {
            itemEntity.discard();
            return;
        }
        itemEntity.setItem(remaining);
    }

    private static void playSampleFeedback(ServerLevel level, Villager customer) {
        customer.playSound(SoundEvents.ITEM_PICKUP, 0.45F, 1.25F);
        customer.playSound(SoundEvents.VILLAGER_YES, 0.38F, 0.95F + level.random.nextFloat() * 0.18F);
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                customer.getX(),
                customer.getY() + 1.45D,
                customer.getZ(),
                4,
                0.25D,
                0.25D,
                0.25D,
                0.02D
        );
    }
}
