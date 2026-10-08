package com.trippycraft.economy;

import com.trippycraft.TrippyCraft;
import com.trippycraft.compat.NbtCompat;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import com.trippycraft.registry.ModVillagers;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Keeps every bartender stocked and permanently stationed behind its counter. */
public final class BartenderService {
    private static final String BARTENDER_KEY = "bartender";
    private static final String ANCHOR_KEY = "bartender_anchor";
    private static final String STATION_KEY = "bartender_station";
    private static final double SNAP_DISTANCE_SQUARED = 0.04D;
    private static final int OFFER_MAX_USES = 9_999;

    private static final List<AlcoholOffer> ALCOHOL_OFFERS = List.of(
            new AlcoholOffer(ModItems.BEER_BOTTLE::get, 1, 1),
            new AlcoholOffer(ModItems.WINE_GLASS_5OZ::get, 1, 1),
            new AlcoholOffer(ModItems.WINE_GLASS_9OZ::get, 2, 1),
            new AlcoholOffer(ModItems.WHISKEY_GLASS::get, 1, 1),
            new AlcoholOffer(ModItems.RUM_GLASS::get, 1, 1),
            new AlcoholOffer(ModItems.VODKA_GLASS::get, 1, 1)
    );

    private BartenderService() {
    }

    public static boolean isBartender(Villager villager) {
        return !villager.isBaby()
                && (villager.getVillagerData().getProfession() == ModVillagers.BARTENDER.get()
                || NbtCompat.getBooleanOr(data(villager), BARTENDER_KEY, false));
    }

    public static void configure(Villager villager, BlockPos anchor, BlockPos station) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return;
        }

        villager.setVillagerData(villager.getVillagerData()
                .setType(VillagerType.byBiome(level.getBiome(anchor)))
                .setProfession(ModVillagers.BARTENDER.get())
                .setLevel(1));
        villager.setPersistenceRequired();
        villager.setNoAi(true);
        villager.setCustomName(Component.translatable("entity.trippycraft.bartender"));
        villager.setCustomNameVisible(false);

        CompoundTag data = data(villager);
        data.putBoolean(BARTENDER_KEY, true);
        data.putLong(ANCHOR_KEY, anchor.asLong());
        data.putLong(STATION_KEY, station.asLong());
        level.getPoiManager().take(
                poi -> poi.is(ModVillagers.BARTENDER_STATION_KEY),
                (poi, pos) -> pos.equals(station),
                station,
                1
        );
        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), station));
        ensureOffers(villager);
        snapToAnchor(villager, anchor);
    }

    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Villager villager)
                || villager.level().isClientSide()
                || !isBartender(villager)) {
            return;
        }

        CompoundTag data = data(villager);
        BlockPos anchor;
        if (data.contains(ANCHOR_KEY)) {
            anchor = BlockPos.of(data.getLong(ANCHOR_KEY));
        } else {
            BlockPos station = findNearestStation(villager);
            anchor = chooseAnchor(villager, station);
            data.putBoolean(BARTENDER_KEY, true);
            data.putLong(ANCHOR_KEY, anchor.asLong());
            data.putLong(STATION_KEY, station.asLong());
        }

        villager.setNoAi(true);
        villager.setPersistenceRequired();
        ensureOffers(villager);
        snapToAnchor(villager, anchor);
    }

    public static BlockPos storedStation(Villager villager) {
        CompoundTag data = data(villager);
        return data.contains(STATION_KEY) ? BlockPos.of(data.getLong(STATION_KEY)) : villager.blockPosition();
    }

    private static BlockPos findNearestStation(Villager villager) {
        BlockPos origin = villager.blockPosition();
        BlockPos nearest = origin;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(origin.offset(-5, -3, -5), origin.offset(5, 3, 5))) {
            if (!villager.level().getBlockState(candidate).is(ModBlocks.BARTENDER_STATION.get())) {
                continue;
            }
            double distance = candidate.distSqr(origin);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate.immutable();
            }
        }
        return nearest;
    }

    private static BlockPos chooseAnchor(Villager villager, BlockPos station) {
        BlockPos current = villager.blockPosition();
        if (current.distManhattan(station) <= 2 && isStandable(villager, current)) {
            return current;
        }

        BlockPos best = current;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : List.of(station.north(), station.south(), station.east(), station.west())) {
            if (!isStandable(villager, candidate)) {
                continue;
            }
            double distance = candidate.distSqr(current);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isStandable(Villager villager, BlockPos pos) {
        return villager.level().getBlockState(pos).getCollisionShape(villager.level(), pos).isEmpty()
                && villager.level().getBlockState(pos.above()).getCollisionShape(villager.level(), pos.above()).isEmpty()
                && !villager.level().getBlockState(pos.below()).getCollisionShape(villager.level(), pos.below()).isEmpty();
    }

    private static void snapToAnchor(Villager villager, BlockPos anchor) {
        Vec3 target = new Vec3(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
        if (villager.position().distanceToSqr(target) > SNAP_DISTANCE_SQUARED) {
            villager.teleportTo(target.x, target.y, target.z);
        }
        villager.setDeltaMovement(Vec3.ZERO);
        villager.fallDistance = 0.0F;
    }

    private static void ensureOffers(Villager villager) {
        MerchantOffers current = villager.getOffers();
        boolean complete = current.size() == ALCOHOL_OFFERS.size()
                && ALCOHOL_OFFERS.stream().allMatch(expected -> current.stream()
                .anyMatch(offer -> offerMatches(offer, expected)));
        if (complete) {
            return;
        }

        MerchantOffers stocked = new MerchantOffers();
        for (AlcoholOffer offer : ALCOHOL_OFFERS) {
            stocked.add(new MerchantOffer(
                    new ItemCost(Items.EMERALD, offer.emeraldCost()),
                    new ItemStack(offer.item().get(), offer.outputCount()),
                    OFFER_MAX_USES,
                    0,
                    0.0F
            ));
        }
        villager.setOffers(stocked);
    }

    private static boolean offerMatches(MerchantOffer offer, AlcoholOffer expected) {
        ItemStack cost = offer.getBaseCostA();
        ItemStack result = offer.getResult();
        return cost.is(Items.EMERALD)
                && cost.getCount() == expected.emeraldCost()
                && result.is(expected.item().get())
                && result.getCount() == expected.outputCount();
    }

    private static CompoundTag data(Villager villager) {
        return NbtCompat.getOrCreateCompound(villager.getPersistentData(), TrippyCraft.MOD_ID);
    }

    private record AlcoholOffer(java.util.function.Supplier<Item> item, int emeraldCost, int outputCount) {
    }
}
