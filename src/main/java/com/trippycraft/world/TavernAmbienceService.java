package com.trippycraft.world;

import com.trippycraft.network.ModNetwork;
import com.trippycraft.network.TavernAmbiencePacket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Sends only the closest verified tavern to each client for spatial music and lighting. */
public final class TavernAmbienceService {
    public static final BlockPos LOCAL_JUKEBOX = new BlockPos(2, 1, 7);
    private static final long UPDATE_INTERVAL_TICKS = 20L;
    private static final long HEARTBEAT_TICKS = 100L;
    private static final double SIGNAL_RADIUS_SQUARED = 72.0D * 72.0D;
    private static final double MDMA_INTENSIFIER_RADIUS_SQUARED = 10.0D * 10.0D;
    private static final Map<UUID, SentSignal> LAST_SIGNALS = new HashMap<>();

    private TavernAmbienceService() {
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.players().isEmpty()
                || level.getGameTime() % UPDATE_INTERVAL_TICKS != 0L) {
            return;
        }

        VillageBarData data = level.getDataStorage().computeIfAbsent(VillageBarData.FACTORY, VillageBarData.NAME);
        TavernPlaybackData playback = level.getDataStorage()
                .computeIfAbsent(TavernPlaybackData.FACTORY, TavernPlaybackData.NAME);
        for (ServerPlayer player : level.players()) {
            sendCurrentSignal(level, player, data, playback, false);
        }
    }

    public static void refreshPlayers(ServerLevel level) {
        VillageBarData data = level.getDataStorage().computeIfAbsent(VillageBarData.FACTORY, VillageBarData.NAME);
        TavernPlaybackData playback = level.getDataStorage()
                .computeIfAbsent(TavernPlaybackData.FACTORY, TavernPlaybackData.NAME);
        for (ServerPlayer player : level.players()) {
            sendCurrentSignal(level, player, data, playback, true);
        }
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SIGNALS.remove(event.getEntity().getUUID());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        LAST_SIGNALS.clear();
    }

    private static TavernAmbiencePacket nearestSignal(ServerLevel level,
                                                       ServerPlayer player,
                                                       VillageBarData data,
                                                       TavernPlaybackData playback) {
        VillageBarData.BarPlacement closest = null;
        BlockPos closestJukebox = BlockPos.ZERO;
        double closestDistance = SIGNAL_RADIUS_SQUARED;
        Vec3 playerPosition = player.position();
        for (VillageBarData.BarPlacement bar : data.allBars()) {
            if (distanceToBoundsSqr(playerPosition, TavernGeometry.bounds(bar)) > closestDistance) {
                continue;
            }
            Optional<BlockPos> foundJukebox = findJukebox(level, bar);
            if (foundJukebox.isEmpty()) {
                continue;
            }
            BlockPos jukebox = foundJukebox.get();
            double distance = playerPosition.distanceToSqr(Vec3.atCenterOf(jukebox));
            if (distance <= closestDistance) {
                closest = bar;
                closestJukebox = jukebox;
                closestDistance = distance;
            }
        }
        if (closest == null) {
            return TavernAmbiencePacket.inactive();
        }
        return new TavernAmbiencePacket(
                true,
                closest.origin(),
                closest.rotation(),
                closestJukebox,
                playback.timelineTick(level.getGameTime(), closest)
        );
    }

    public static BlockPos jukeboxPosition(VillageBarData.BarPlacement bar) {
        return TavernGeometry.transform(bar, LOCAL_JUKEBOX);
    }

    public static Optional<BlockPos> findJukebox(ServerLevel level, VillageBarData.BarPlacement bar) {
        BoundingBox bounds = TavernGeometry.bounds(bar);
        BlockPos preferred = jukeboxPosition(bar);
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (BlockPos candidate : BlockPos.betweenClosed(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            if (!level.hasChunkAt(candidate) || !level.getBlockState(candidate).is(Blocks.JUKEBOX)) {
                continue;
            }
            double distance = candidate.distSqr(preferred);
            if (distance < closestDistance
                    || (distance == closestDistance && (closest == null || candidate.asLong() < closest.asLong()))) {
                closest = candidate.immutable();
                closestDistance = distance;
            }
        }
        return Optional.ofNullable(closest);
    }

    public static Optional<VillageBarData.BarPlacement> findContainingBar(VillageBarData data, BlockPos position) {
        return data.allBars().stream()
                .filter(bar -> TavernGeometry.contains(bar, position))
                .findFirst();
    }

    /** The recognized nightclub footprint and its ten-block exterior halo intensify music-responsive substances. */
    public static double mdmaIntensityMultiplier(ServerPlayer player) {
        VillageBarData data = player.serverLevel().getDataStorage()
                .computeIfAbsent(VillageBarData.FACTORY, VillageBarData.NAME);
        Vec3 position = player.position();
        for (VillageBarData.BarPlacement bar : data.allBars()) {
            if (distanceToBoundsSqr(position, TavernGeometry.bounds(bar)) <= MDMA_INTENSIFIER_RADIUS_SQUARED) {
                return 1.5D;
            }
        }
        return 1.0D;
    }

    public static double twoCbIntensityMultiplier(ServerPlayer player) {
        return mdmaIntensityMultiplier(player);
    }

    private static double distanceToBoundsSqr(Vec3 position, BoundingBox bounds) {
        double x = Math.max(bounds.minX() - position.x, Math.max(0.0D, position.x - (bounds.maxX() + 1.0D)));
        double y = Math.max(bounds.minY() - position.y, Math.max(0.0D, position.y - (bounds.maxY() + 1.0D)));
        double z = Math.max(bounds.minZ() - position.z, Math.max(0.0D, position.z - (bounds.maxZ() + 1.0D)));
        return x * x + y * y + z * z;
    }

    private static void sendCurrentSignal(ServerLevel level,
                                          ServerPlayer player,
                                          VillageBarData data,
                                          TavernPlaybackData playback,
                                          boolean force) {
        TavernAmbiencePacket signal = nearestSignal(level, player, data, playback);
        SentSignal previous = LAST_SIGNALS.get(player.getUUID());
        boolean dimensionChanged = previous == null || !previous.dimension().equals(level.dimension());
        boolean signalChanged = previous == null || !samePlayback(previous.packet(), signal);
        boolean heartbeatDue = previous == null || level.getGameTime() - previous.sentAtTick() >= HEARTBEAT_TICKS;
        if (force || dimensionChanged || signalChanged || heartbeatDue) {
            ModNetwork.sendTavernAmbience(player, signal);
            LAST_SIGNALS.put(player.getUUID(), new SentSignal(level.dimension(), signal, level.getGameTime()));
        }
    }

    private static boolean samePlayback(TavernAmbiencePacket first, TavernAmbiencePacket second) {
        if (first.active() != second.active()) {
            return false;
        }
        if (!first.active()) {
            return true;
        }
        return first.origin().equals(second.origin())
                && first.rotation() == second.rotation()
                && first.jukebox().equals(second.jukebox())
                && TavernPlaylist.selection(first.playlistTick()).trackIndex()
                == TavernPlaylist.selection(second.playlistTick()).trackIndex();
    }

    private record SentSignal(ResourceKey<Level> dimension, TavernAmbiencePacket packet, long sentAtTick) {
    }
}
