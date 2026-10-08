package com.trippycraft.world;

import com.trippycraft.TrippyCraft;
import com.trippycraft.compat.NbtCompat;
import com.trippycraft.economy.DealerService;
import com.trippycraft.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Creates a small, bounded tavern crowd during the village night. */
public final class TavernNightlifeService {
    private static final String GUEST_KEY = "tavern_nightlife_guest";
    private static final String DEALER_GUEST_KEY = "tavern_nightlife_dealer";
    private static final String TAVERN_ORIGIN_KEY = "tavern_nightlife_origin";
    private static final long UPDATE_INTERVAL_TICKS = 40L;
    private static final long NIGHT_START = 12_500L;
    private static final long NIGHT_END = 23_000L;
    private static final int REGULAR_PATRON_COUNT = 4;
    private static final BlockPos LOCAL_LOUNGE_CENTER = new BlockPos(4, 1, 6);
    private static final BlockPos LOCAL_DEALER_SPOT = new BlockPos(5, 1, 10);
    private static final BlockPos[] LOCAL_GUEST_SPOTS = {
            new BlockPos(4, 1, 3),
            new BlockPos(5, 1, 3),
            new BlockPos(3, 1, 5),
            new BlockPos(6, 1, 5),
            new BlockPos(4, 1, 7),
            new BlockPos(5, 1, 7),
            new BlockPos(3, 1, 9),
            new BlockPos(6, 1, 9),
            LOCAL_DEALER_SPOT
    };

    private TavernNightlifeService() {
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.players().isEmpty()
                || level.getGameTime() % UPDATE_INTERVAL_TICKS != 0L) {
            return;
        }
        boolean night = isNight(level);
        VillageBarData data = level.getDataStorage().computeIfAbsent(VillageBarData.FACTORY, VillageBarData.NAME);
        for (VillageBarData.BarPlacement bar : data.allBars()) {
            if (level.hasChunkAt(bar.station())) {
                updateBar(level, bar, night);
            }
        }
    }

    static void updateBar(ServerLevel level, VillageBarData.BarPlacement bar, boolean night) {
        AABB tavernBox = tavernBox(bar);
        AABB searchBox = tavernBox.inflate(12.0D, 6.0D, 12.0D);
        List<Villager> guests = new ArrayList<>(level.getEntitiesOfClass(
                Villager.class,
                searchBox,
                villager -> isNightlifeGuest(villager) && storedOrigin(villager).equals(bar.origin())
        ));
        if (!night || !level.getBlockState(bar.station()).is(ModBlocks.BARTENDER_STATION.get())) {
            guests.forEach(Villager::discard);
            return;
        }

        guests.sort(Comparator.comparing(Villager::getUUID));
        List<Villager> dealers = guests.stream().filter(TavernNightlifeService::isNightlifeDealer).toList();
        List<Villager> regulars = guests.stream().filter(guest -> !isNightlifeDealer(guest)).toList();
        for (int index = 1; index < dealers.size(); index++) {
            dealers.get(index).discard();
        }
        for (int index = REGULAR_PATRON_COUNT; index < regulars.size(); index++) {
            regulars.get(index).discard();
        }

        Villager dealer = dealers.isEmpty() ? spawnGuest(level, bar, true, 0) : dealers.getFirst();
        List<Villager> activeRegulars = new ArrayList<>(regulars.subList(0, Math.min(REGULAR_PATRON_COUNT, regulars.size())));
        while (activeRegulars.size() < REGULAR_PATRON_COUNT) {
            Villager patron = spawnGuest(level, bar, false, activeRegulars.size());
            if (patron == null) {
                break;
            }
            activeRegulars.add(patron);
        }

        if (dealer != null) {
            maintainGuest(level, bar, tavernBox, dealer, REGULAR_PATRON_COUNT);
        }
        for (int index = 0; index < activeRegulars.size(); index++) {
            maintainGuest(level, bar, tavernBox, activeRegulars.get(index), index);
        }
    }

    public static boolean isNightlifeGuest(Villager villager) {
        return NbtCompat.getBooleanOr(data(villager), GUEST_KEY, false);
    }

    public static boolean isNightlifeDealer(Villager villager) {
        return isNightlifeGuest(villager)
                && NbtCompat.getBooleanOr(data(villager), DEALER_GUEST_KEY, false);
    }

    public static BlockPos storedOrigin(Villager villager) {
        CompoundTag data = data(villager);
        return data.contains(TAVERN_ORIGIN_KEY)
                ? BlockPos.of(data.getLong(TAVERN_ORIGIN_KEY))
                : BlockPos.ZERO;
    }

    private static Villager spawnGuest(ServerLevel level,
                                       VillageBarData.BarPlacement bar,
                                       boolean dealer,
                                       int regularIndex) {
        Villager villager = EntityType.VILLAGER.create(level);
        if (villager == null) {
            TrippyCraft.LOGGER.error("Could not create a nighttime tavern guest at {}", bar.origin());
            return null;
        }
        BlockPos localSpot = dealer
                ? LOCAL_DEALER_SPOT
                : LOCAL_GUEST_SPOTS[Math.floorMod(regularIndex, LOCAL_GUEST_SPOTS.length - 1)];
        BlockPos position = transform(bar, localSpot);
        villager.moveTo(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F,
                0.0F
        );
        villager.setVillagerData(villager.getVillagerData()
                .setType(VillagerType.byBiome(level.getBiome(position)))
                .setProfession(VillagerProfession.NONE)
                .setLevel(1));
        villager.setAge(0);
        villager.setNoAi(false);
        villager.setPersistenceRequired();
        villager.setCanPickUpLoot(false);
        markGuest(villager, bar.origin(), dealer);
        if (dealer) {
            DealerService.markDealer(villager);
        }
        if (!level.addFreshEntity(villager)) {
            return null;
        }
        return villager;
    }

    private static void maintainGuest(ServerLevel level,
                                      VillageBarData.BarPlacement bar,
                                      AABB tavernBox,
                                      Villager guest,
                                      int guestIndex) {
        if (!guest.isAlive()) {
            return;
        }
        guest.setNoAi(false);
        guest.setPersistenceRequired();
        BlockPos loungeCenter = transform(bar, LOCAL_LOUNGE_CENTER);
        guest.restrictTo(loungeCenter, 4);
        guest.getBrain().eraseMemory(MemoryModuleType.HOME);
        guest.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        guest.getBrain().eraseMemory(MemoryModuleType.MEETING_POINT);

        if (!tavernBox.inflate(0.5D).contains(guest.position())) {
            BlockPos rescue = transform(bar, LOCAL_GUEST_SPOTS[Math.floorMod(guestIndex, LOCAL_GUEST_SPOTS.length)]);
            guest.teleportTo(rescue.getX() + 0.5D, rescue.getY(), rescue.getZ() + 0.5D);
        }

        long movementStep = level.getGameTime() / 80L;
        int targetIndex = Math.floorMod(
                Long.hashCode(movementStep + guest.getUUID().getLeastSignificantBits()),
                LOCAL_GUEST_SPOTS.length
        );
        BlockPos target = transform(bar, LOCAL_GUEST_SPOTS[targetIndex]);
        guest.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, 0.42F, 1));
    }

    private static void markGuest(Villager villager, BlockPos origin, boolean dealer) {
        CompoundTag data = data(villager);
        data.putBoolean(GUEST_KEY, true);
        data.putBoolean(DEALER_GUEST_KEY, dealer);
        data.putLong(TAVERN_ORIGIN_KEY, origin.asLong());
    }

    private static boolean isNight(ServerLevel level) {
        long dayTime = Math.floorMod(level.getDayTime(), 24_000L);
        return dayTime >= NIGHT_START && dayTime < NIGHT_END;
    }

    private static AABB tavernBox(VillageBarData.BarPlacement bar) {
        BlockPos[] corners = {
                transform(bar, BlockPos.ZERO),
                transform(bar, new BlockPos(9, 8, 0)),
                transform(bar, new BlockPos(0, 8, 13)),
                transform(bar, new BlockPos(9, 8, 13))
        };
        int minimumX = Integer.MAX_VALUE;
        int minimumY = Integer.MAX_VALUE;
        int minimumZ = Integer.MAX_VALUE;
        int maximumX = Integer.MIN_VALUE;
        int maximumY = Integer.MIN_VALUE;
        int maximumZ = Integer.MIN_VALUE;
        for (BlockPos corner : corners) {
            minimumX = Math.min(minimumX, corner.getX());
            minimumY = Math.min(minimumY, corner.getY());
            minimumZ = Math.min(minimumZ, corner.getZ());
            maximumX = Math.max(maximumX, corner.getX());
            maximumY = Math.max(maximumY, corner.getY());
            maximumZ = Math.max(maximumZ, corner.getZ());
        }
        return new AABB(minimumX, minimumY, minimumZ, maximumX + 1.0D, maximumY + 1.0D, maximumZ + 1.0D);
    }

    private static BlockPos transform(VillageBarData.BarPlacement bar, BlockPos local) {
        return StructureTemplate.transform(local, Mirror.NONE, bar.rotation(), BlockPos.ZERO).offset(bar.origin());
    }

    private static CompoundTag data(Villager villager) {
        return NbtCompat.getOrCreateCompound(villager.getPersistentData(), TrippyCraft.MOD_ID);
    }
}
