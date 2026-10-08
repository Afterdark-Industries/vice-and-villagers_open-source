package com.trippycraft.world;

import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.trippycraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

public final class ErgotInfectionEvents {
    private static final long REQUIRED_SHADED_TICKS = Level.TICKS_PER_DAY;
    private static final long TRACKED_CHECK_INTERVAL_TICKS = 100L;
    private static final long DISCOVERY_INTERVAL_TICKS = 200L;
    private static final int DISCOVERY_HORIZONTAL_RADIUS = 24;
    private static final int DISCOVERY_VERTICAL_RADIUS = 8;

    private ErgotInfectionEvents() {
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();
        ErgotInfectionData data = level.getDataStorage().computeIfAbsent(ErgotInfectionData.FACTORY, ErgotInfectionData.NAME);
        if (gameTime % TRACKED_CHECK_INTERVAL_TICKS == 0L) {
            tickTrackedWheat(level, data, gameTime);
        }
        if (gameTime % DISCOVERY_INTERVAL_TICKS == 0L) {
            discoverAroundPlayers(level, data, gameTime);
        }
    }

    public static void onCropGrowPost(CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockState state = event.getState();
        if (isMatureWheat(state)) {
            trackIfEligible(level, event.getPos(), state, level.getDataStorage().computeIfAbsent(ErgotInfectionData.FACTORY, ErgotInfectionData.NAME), level.getGameTime());
        }
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        scanColumnBelowPlacedBlock(
                level,
                event.getPos(),
                event.getPlacedBlock().getLightBlock(level, event.getPos()) > 0,
                level.getDataStorage().computeIfAbsent(ErgotInfectionData.FACTORY, ErgotInfectionData.NAME),
                level.getGameTime());
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ErgotInfectionData data = level.getDataStorage().get(ErgotInfectionData.FACTORY, ErgotInfectionData.NAME);
        if (data != null && event.getState().is(Blocks.WHEAT)) {
            data.remove(event.getPos());
        }
    }

    private static void tickTrackedWheat(ServerLevel level, ErgotInfectionData data, long gameTime) {
        for (ErgotInfectionData.TrackedWheat entry : data.snapshot()) {
            BlockPos pos = entry.pos();
            if (!isChunkLoaded(level, pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!isMatureWheat(state) || level.canSeeSky(pos)) {
                data.remove(pos);
                continue;
            }

            if (gameTime - entry.shadedSince() >= REQUIRED_SHADED_TICKS) {
                level.setBlock(pos, ModBlocks.ERGOT_INFECTED_WHEAT.get().defaultBlockState(), Block.UPDATE_ALL);
                data.remove(pos);
            }
        }
    }

    private static void discoverAroundPlayers(ServerLevel level, ErgotInfectionData data, long gameTime) {
        if (level.players().isEmpty()) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int minLevelY = level.getMinBuildHeight();
        int maxLevelY = level.getMaxBuildHeight() - 1;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive()) {
                continue;
            }

            BlockPos center = player.blockPosition();
            int minY = Math.max(minLevelY, center.getY() - DISCOVERY_VERTICAL_RADIUS);
            int maxY = Math.min(maxLevelY, center.getY() + DISCOVERY_VERTICAL_RADIUS);
            for (int x = center.getX() - DISCOVERY_HORIZONTAL_RADIUS; x <= center.getX() + DISCOVERY_HORIZONTAL_RADIUS; x++) {
                for (int z = center.getZ() - DISCOVERY_HORIZONTAL_RADIUS; z <= center.getZ() + DISCOVERY_HORIZONTAL_RADIUS; z++) {
                    if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
                        continue;
                    }

                    for (int y = minY; y <= maxY; y++) {
                        cursor.set(x, y, z);
                        BlockState state = level.getBlockState(cursor);
                        if (isMatureWheat(state)) {
                            trackIfEligible(level, cursor, state, data, gameTime);
                        }
                    }
                }
            }
        }
    }

    private static void scanColumnBelowPlacedBlock(ServerLevel level, BlockPos placedPos, boolean placedBlockBlocksSky, ErgotInfectionData data, long gameTime) {
        if (!level.hasChunk(SectionPos.blockToSectionCoord(placedPos.getX()), SectionPos.blockToSectionCoord(placedPos.getZ()))) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(placedPos.getX(), placedPos.getY() - 1, placedPos.getZ());
        for (int y = placedPos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (isMatureWheat(state) && (placedBlockBlocksSky || !level.canSeeSky(cursor))) {
                data.track(cursor, gameTime);
            }
        }
    }

    private static void trackIfEligible(ServerLevel level, BlockPos pos, BlockState state, ErgotInfectionData data, long gameTime) {
        if (!isMatureWheat(state) || level.canSeeSky(pos)) {
            data.remove(pos);
            return;
        }

        data.track(pos, gameTime);
    }

    private static boolean isMatureWheat(BlockState state) {
        return state.is(Blocks.WHEAT)
                && state.hasProperty(CropBlock.AGE)
                && state.getValue(CropBlock.AGE) >= CropBlock.MAX_AGE;
    }

    private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
        return level.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }
}
