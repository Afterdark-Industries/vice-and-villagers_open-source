package com.trippycraft.world;

import com.trippycraft.TrippyCraft;
import com.trippycraft.economy.BartenderService;
import com.trippycraft.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Adds exactly one or two persistent, rotated bars to every non-zombie village
 * once the village and a safe perimeter site are loaded.
 */
public final class VillageBarEvents {
    private static final long SCAN_INTERVAL_TICKS = 100L;
    private static final long FAILURE_LOG_INTERVAL_TICKS = 1_200L;
    private static final int SCAN_RADIUS_CHUNKS = 6;
    private static final int MAX_TERRAIN_CUT = 12;
    private static final int MAX_FOUNDATION_DROP = 3;
    private static final int LOCAL_FRONT_X = 4;
    private static final BlockPos LOCAL_ANCHOR = new BlockPos(4, 1, 9);
    private static final BlockPos LOCAL_STATION = new BlockPos(4, 1, 11);
    private static final BlockPos[] LOCAL_RAVE_FIXTURES = {
            new BlockPos(3, 4, 4),
            new BlockPos(6, 4, 4),
            new BlockPos(3, 4, 7),
            new BlockPos(6, 4, 7)
    };
    private static final Block[] RAVE_FIXTURE_BLOCKS = {
            Blocks.PEARLESCENT_FROGLIGHT,
            Blocks.VERDANT_FROGLIGHT,
            Blocks.OCHRE_FROGLIGHT,
            Blocks.SEA_LANTERN
    };
    private static final int LEGACY_TEMPLATE_WIDTH = 13;
    private static final int LEGACY_TEMPLATE_HEIGHT = 10;
    private static final int LEGACY_TEMPLATE_DEPTH = 11;
    private static final int[] LATERAL_OFFSETS = {
            0, 12, -12, 24, -24, 36, -36, 48, -48, 64, -64, 80, -80, 96, -96
    };
    private static final int[] OUTWARD_OFFSETS = {4, 8, 12, 20, 28, 40, 56, 72, 96};
    private static final Map<Long, Long> LAST_FAILURE_LOG_TICKS = new HashMap<>();

    private VillageBarEvents() {
    }

    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.players().isEmpty()
                || level.getGameTime() % SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        Map<Long, StructureStart> starts = findLoadedVillageStarts(level);
        if (starts.isEmpty()) {
            return;
        }

        VillageBarData data = level.getDataStorage().computeIfAbsent(VillageBarData.FACTORY, VillageBarData.NAME);
        for (Map.Entry<Long, StructureStart> entry : starts.entrySet()) {
            StructureStart start = entry.getValue();
            if (isZombieVillage(start)) {
                continue;
            }
            processVillageStart(level, data, entry.getKey(), start);
        }
    }

    static VillageBarData.VillageRecord processVillageStart(ServerLevel level,
                                                             VillageBarData data,
                                                             long villageKey,
                                                             StructureStart start) {
        int targetCount = targetCount(level, villageKey);
        VillageBarData.VillageRecord record = data.getOrCreate(villageKey, targetCount);
        migrateLegacyBars(level, data, villageKey, record);
        ensureBartenders(level, record);
        while (record.bars().size() < record.targetCount()) {
            Optional<VillageBarData.BarPlacement> placed = placeNextBar(
                    level,
                    start,
                    record,
                    villageKey,
                    record.bars().size()
            );
            if (placed.isEmpty()) {
                break;
            }
            data.addBar(villageKey, placed.get());
        }
        return record;
    }

    private static void migrateLegacyBars(ServerLevel level,
                                          VillageBarData data,
                                          long villageKey,
                                          VillageBarData.VillageRecord record) {
        for (int index = 0; index < record.bars().size(); index++) {
            VillageBarData.BarPlacement bar = record.bars().get(index);
            if (bar.revision() >= VillageBarData.CURRENT_BAR_REVISION || !level.hasChunkAt(bar.station())) {
                continue;
            }

            // A missing station represents an intentional player edit. Advance
            // the data revision without rebuilding anything the player removed.
            if (!level.getBlockState(bar.station()).is(ModBlocks.BARTENDER_STATION.get())) {
                data.replaceBar(villageKey, index, revisedPlacement(bar, bar.origin(), bar.anchor(), bar.station()));
                continue;
            }

            // Revision 3 is already the player-authored tavern. Install only
            // the new empty-space light fixtures instead of rebuilding it and
            // erasing any other player decoration.
            if (bar.revision() == 3) {
                boolean fixturesLoaded = true;
                for (BlockPos local : LOCAL_RAVE_FIXTURES) {
                    if (!level.hasChunkAt(transformLocal(bar.origin(), bar.rotation(), local))) {
                        fixturesLoaded = false;
                        break;
                    }
                }
                if (!fixturesLoaded) {
                    continue;
                }
                installRaveFixtures(level, bar);
                data.replaceBar(villageKey, index, revisedPlacement(bar, bar.origin(), bar.anchor(), bar.station()));
                TrippyCraft.LOGGER.info("Installed rave lighting in existing {} at {}", bar.template(), bar.origin());
                continue;
            }

            Optional<BarVariant> maybeVariant = BarVariant.byId(bar.template());
            Optional<StructureTemplate> maybeTemplate = level.getStructureManager().get(
                    ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "village/bars/" + bar.template())
            );
            if (maybeVariant.isEmpty() || maybeTemplate.isEmpty()) {
                TrippyCraft.LOGGER.error("Cannot migrate unknown village bar template {}", bar.template());
                continue;
            }

            StructureTemplate template = maybeTemplate.get();
            StructurePlaceSettings settings = new StructurePlaceSettings()
                    .setRotation(bar.rotation())
                    .setIgnoreEntities(true)
                    .setKnownShape(false);
            BoundingBox oldBox = legacyBarBox(bar.origin(), bar.rotation());
            if (!level.hasChunksAt(oldBox.minX(), oldBox.minZ(), oldBox.maxX(), oldBox.maxZ())) {
                continue;
            }

            int targetFloorY = entranceGroundFloor(level, bar);
            targetFloorY = Math.min(bar.origin().getY(), targetFloorY);
            targetFloorY = Math.max(bar.origin().getY() - MAX_TERRAIN_CUT, targetFloorY);
            BlockPos newOrigin = new BlockPos(bar.origin().getX(), targetFloorY, bar.origin().getZ());

            removeLegacyBartender(level, oldBox, bar.station());
            clearLegacyStructure(level, oldBox, targetFloorY + 1);

            RandomSource random = RandomSource.create(level.getSeed() ^ newOrigin.asLong());
            settings.setRandom(random);
            if (!template.placeInWorld(level, newOrigin, newOrigin, settings, random, Block.UPDATE_ALL)) {
                TrippyCraft.LOGGER.error("Could not rebuild legacy village bar {} at {}", bar.template(), newOrigin);
                continue;
            }

            BoundingBox newBox = template.getBoundingBox(settings, newOrigin);
            BarVariant variant = maybeVariant.get();
            supportFoundation(level, newBox, targetFloorY, variant.foundation());
            BlockPos newAnchor = transformLocal(newOrigin, bar.rotation(), LOCAL_ANCHOR);
            BlockPos newStation = transformLocal(newOrigin, bar.rotation(), LOCAL_STATION);
            if (!level.getBlockState(newStation).is(ModBlocks.BARTENDER_STATION.get())) {
                TrippyCraft.LOGGER.error("Migrated bar {} is missing its station at {}", bar.template(), newStation);
                continue;
            }

            spawnBartender(level, newAnchor, newStation);
            BlockPos front = transformLocal(newOrigin, bar.rotation(), new BlockPos(LOCAL_FRONT_X, 0, 0));
            connectPath(level, front, SiteSide.forRotation(bar.rotation()), targetFloorY, variant.pathBlock());
            data.replaceBar(villageKey, index, revisedPlacement(bar, newOrigin, newAnchor, newStation));
            TrippyCraft.LOGGER.info(
                    "Replaced legacy {} with the regional custom tavern and moved its floor from y={} to ground level y={} at {}",
                    variant.displayName(),
                    bar.origin().getY(),
                    targetFloorY,
                    newOrigin
            );
        }
    }

    private static BoundingBox legacyBarBox(BlockPos origin, Rotation rotation) {
        BlockPos[] corners = {
                transformLocal(origin, rotation, BlockPos.ZERO),
                transformLocal(origin, rotation, new BlockPos(LEGACY_TEMPLATE_WIDTH - 1, 0, 0)),
                transformLocal(origin, rotation, new BlockPos(0, 0, LEGACY_TEMPLATE_DEPTH - 1)),
                transformLocal(origin, rotation, new BlockPos(LEGACY_TEMPLATE_WIDTH - 1, 0, LEGACY_TEMPLATE_DEPTH - 1))
        };
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos corner : corners) {
            minX = Math.min(minX, corner.getX());
            minZ = Math.min(minZ, corner.getZ());
            maxX = Math.max(maxX, corner.getX());
            maxZ = Math.max(maxZ, corner.getZ());
        }
        return new BoundingBox(
                minX,
                origin.getY(),
                minZ,
                maxX,
                origin.getY() + LEGACY_TEMPLATE_HEIGHT - 1,
                maxZ
        );
    }

    private static VillageBarData.BarPlacement revisedPlacement(VillageBarData.BarPlacement old,
                                                                 BlockPos origin,
                                                                 BlockPos anchor,
                                                                 BlockPos station) {
        return new VillageBarData.BarPlacement(
                old.template(),
                origin,
                old.rotation(),
                anchor,
                station,
                VillageBarData.CURRENT_BAR_REVISION
        );
    }

    private static void installRaveFixtures(ServerLevel level, VillageBarData.BarPlacement bar) {
        for (int index = 0; index < LOCAL_RAVE_FIXTURES.length; index++) {
            BlockPos position = transformLocal(bar.origin(), bar.rotation(), LOCAL_RAVE_FIXTURES[index]);
            if (level.getBlockState(position).isAir()) {
                level.setBlock(position, RAVE_FIXTURE_BLOCKS[index].defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static int entranceGroundFloor(ServerLevel level, VillageBarData.BarPlacement bar) {
        List<Integer> surfaceHeights = new ArrayList<>();
        for (int localZ = -3; localZ <= -1; localZ++) {
            for (int localX = 4; localX <= 8; localX++) {
                BlockPos sample = transformLocal(
                        new BlockPos(bar.origin().getX(), 0, bar.origin().getZ()),
                        bar.rotation(),
                        new BlockPos(localX, 0, localZ)
                );
                if (!level.hasChunkAt(sample)) {
                    continue;
                }
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sample.getX(), sample.getZ());
                BlockPos ground = new BlockPos(sample.getX(), surface - 1, sample.getZ());
                if (level.getFluidState(ground).isEmpty()) {
                    surfaceHeights.add(surface);
                }
            }
        }
        if (surfaceHeights.isEmpty()) {
            return bar.origin().getY();
        }
        surfaceHeights.sort(Integer::compareTo);
        return surfaceHeights.get(surfaceHeights.size() / 2) - 1;
    }

    private static void removeLegacyBartender(ServerLevel level, BoundingBox oldBox, BlockPos oldStation) {
        for (Villager villager : level.getEntitiesOfClass(Villager.class, box(oldBox).inflate(4.0D),
                candidate -> BartenderService.isBartender(candidate)
                        && BartenderService.storedStation(candidate).equals(oldStation))) {
            villager.discard();
        }
    }

    private static void clearLegacyStructure(ServerLevel level, BoundingBox oldBox, int clearFromY) {
        for (int y = oldBox.maxY(); y >= Math.min(clearFromY, oldBox.maxY()); y--) {
            for (int z = oldBox.minZ(); z <= oldBox.maxZ(); z++) {
                for (int x = oldBox.minX(); x <= oldBox.maxX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockEntity(pos) == null) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static Map<Long, StructureStart> findLoadedVillageStarts(ServerLevel level) {
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Predicate<Structure> isVillage = structure -> structures.wrapAsHolder(structure).is(StructureTags.VILLAGE);
        Set<Long> scannedChunks = new HashSet<>();
        Map<Long, StructureStart> starts = new HashMap<>();

        for (ServerPlayer player : level.players()) {
            ChunkPos playerChunk = player.chunkPosition();
            for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
                for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
                    int chunkX = playerChunk.x + dx;
                    int chunkZ = playerChunk.z + dz;
                    long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
                    if (!scannedChunks.add(chunkKey) || !level.hasChunk(chunkX, chunkZ)) {
                        continue;
                    }
                    for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(chunkX, chunkZ), isVillage)) {
                        starts.put(start.getChunkPos().toLong(), start);
                    }
                }
            }
        }
        return starts;
    }

    private static boolean isZombieVillage(StructureStart start) {
        return start.getPieces().stream()
                .filter(PoolElementStructurePiece.class::isInstance)
                .map(PoolElementStructurePiece.class::cast)
                .map(piece -> piece.getElement().toString())
                .anyMatch(description -> description.contains("/zombie/"));
    }

    private static int targetCount(ServerLevel level, long villageKey) {
        long mixed = mix64(level.getSeed() ^ villageKey ^ 0x5B8D80A71F3C2E19L);
        return 1 + (int) (mixed & 1L);
    }

    private static Optional<VillageBarData.BarPlacement> placeNextBar(ServerLevel level,
                                                                       StructureStart village,
                                                                       VillageBarData.VillageRecord record,
                                                                       long villageKey,
                                                                       int barIndex) {
        BarVariant variant = variantFor(level, village);
        Optional<StructureTemplate> maybeTemplate = level.getStructureManager().get(variant.location());
        if (maybeTemplate.isEmpty()) {
            TrippyCraft.LOGGER.error("Missing village bar template {}", variant.location());
            return Optional.empty();
        }

        StructureTemplate template = maybeTemplate.get();
        BoundingBox villageBox = village.getBoundingBox();
        RandomSource random = RandomSource.create(mix64(level.getSeed() ^ villageKey ^ barIndex));
        List<SiteSide> sides = rotatedSides(random.nextInt(4) + barIndex);
        EnumMap<SiteFailure, Integer> failures = new EnumMap<>(SiteFailure.class);
        long failureLogKey = mix64(
                level.getSeed()
                        ^ villageKey
                        ^ ((long) barIndex << 48)
                        ^ level.dimension().location().hashCode()
        );

        for (int outward : OUTWARD_OFFSETS) {
            for (int lateral : LATERAL_OFFSETS) {
                for (SiteSide side : sides) {
                    Candidate candidate = makeCandidate(template, villageBox, side, outward, lateral);
                    Optional<VillageBarData.BarPlacement> placement = tryPlaceAt(
                            level,
                            villageBox,
                            record,
                            variant,
                            template,
                            candidate,
                            random,
                            failures
                    );
                    if (placement.isPresent()) {
                        LAST_FAILURE_LOG_TICKS.remove(failureLogKey);
                        TrippyCraft.LOGGER.info(
                                "Placed {} {} at {} for village start {} (bar {}/{})",
                                variant.displayName(),
                                candidate.rotation(),
                                placement.get().origin(),
                                village.getChunkPos(),
                                barIndex + 1,
                                record.targetCount()
                        );
                        return placement;
                    }
                }
            }
        }
        long now = level.getGameTime();
        Long lastFailureLog = LAST_FAILURE_LOG_TICKS.get(failureLogKey);
        if (lastFailureLog == null || now - lastFailureLog >= FAILURE_LOG_INTERVAL_TICKS) {
            LAST_FAILURE_LOG_TICKS.put(failureLogKey, now);
            TrippyCraft.LOGGER.warn(
                    "Could not place {} for village start {} after {} candidates; rejection summary: {}",
                    variant.displayName(),
                    village.getChunkPos(),
                    failures.values().stream().mapToInt(Integer::intValue).sum(),
                    failures
            );
        }
        return Optional.empty();
    }

    private static Candidate makeCandidate(StructureTemplate template,
                                           BoundingBox villageBox,
                                           SiteSide side,
                                           int outward,
                                           int lateral) {
        int centerX = (villageBox.minX() + villageBox.maxX()) / 2;
        int centerZ = (villageBox.minZ() + villageBox.maxZ()) / 2;
        int frontX;
        int frontZ;

        switch (side) {
            case NORTH -> {
                frontX = centerX + lateral;
                frontZ = villageBox.minZ() - outward;
            }
            case SOUTH -> {
                frontX = centerX + lateral;
                frontZ = villageBox.maxZ() + outward;
            }
            case WEST -> {
                frontX = villageBox.minX() - outward;
                frontZ = centerZ + lateral;
            }
            case EAST -> {
                frontX = villageBox.maxX() + outward;
                frontZ = centerZ + lateral;
            }
            default -> throw new IllegalStateException("Unexpected side " + side);
        }

        Rotation rotation = side.rotation();
        BlockPos rotatedFront = StructureTemplate.transform(
                new BlockPos(LOCAL_FRONT_X, 0, 0),
                Mirror.NONE,
                rotation,
                BlockPos.ZERO
        );
        BlockPos origin = new BlockPos(frontX - rotatedFront.getX(), 0, frontZ - rotatedFront.getZ());
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        return new Candidate(origin, rotation, template.getBoundingBox(settings, origin), new BlockPos(frontX, 0, frontZ), side);
    }

    private static Optional<VillageBarData.BarPlacement> tryPlaceAt(ServerLevel level,
                                                                     BoundingBox villageBox,
                                                                     VillageBarData.VillageRecord record,
                                                                     BarVariant variant,
                                                                     StructureTemplate template,
                                                                     Candidate candidate,
                                                                     RandomSource random,
                                                                     EnumMap<SiteFailure, Integer> failures) {
        BoundingBox horizontalBox = candidate.boxAtZero();
        if (intersectsHorizontally(horizontalBox, villageBox.inflatedBy(2))) {
            reject(failures, SiteFailure.VILLAGE_OVERLAP);
            return Optional.empty();
        }
        if (!level.hasChunksAt(horizontalBox.minX(), horizontalBox.minZ(), horizontalBox.maxX(), horizontalBox.maxZ())) {
            reject(failures, SiteFailure.CHUNKS_UNLOADED);
            return Optional.empty();
        }

        for (VillageBarData.BarPlacement existing : record.bars()) {
            if (existing.origin().distSqr(candidate.originAtZero()) < 28.0D * 28.0D) {
                reject(failures, SiteFailure.EXISTING_BAR_NEARBY);
                return Optional.empty();
            }
        }

        int minSurface = Integer.MAX_VALUE;
        int maxSurface = Integer.MIN_VALUE;
        for (int z = horizontalBox.minZ(); z <= horizontalBox.maxZ(); z++) {
            for (int x = horizontalBox.minX(); x <= horizontalBox.maxX(); x++) {
                int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos ground = new BlockPos(x, surface - 1, z);
                if (!level.getFluidState(ground).isEmpty()) {
                    reject(failures, SiteFailure.FLUID);
                    return Optional.empty();
                }
                minSurface = Math.min(minSurface, surface);
                maxSurface = Math.max(maxSurface, surface);
            }
        }
        int frontSurface = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                candidate.frontAtZero().getX(),
                candidate.frontAtZero().getZ()
        );
        int cutHeight = maxSurface - frontSurface;
        int foundationDrop = frontSurface - minSurface;
        if (cutHeight > MAX_TERRAIN_CUT || foundationDrop > MAX_FOUNDATION_DROP) {
            reject(failures, SiteFailure.SLOPE);
            return Optional.empty();
        }

        // Put the porch/door at the actual approach height. Higher terrain is
        // cut into; candidates that would need a tall downhill base are rejected.
        int originY = frontSurface - 1;
        BlockPos origin = new BlockPos(candidate.originAtZero().getX(), originY, candidate.originAtZero().getZ());
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(candidate.rotation())
                .setIgnoreEntities(true)
                .setKnownShape(false)
                .setRandom(random);
        BoundingBox placedBox = template.getBoundingBox(settings, origin);
        if (!isSafeSite(level, placedBox, minSurface - 1, maxSurface - 1)) {
            reject(failures, SiteFailure.OBSTRUCTED);
            return Optional.empty();
        }
        if (!level.getEntities(null, box(placedBox)).isEmpty()) {
            reject(failures, SiteFailure.ENTITY);
            return Optional.empty();
        }

        clearOverburdenAboveTemplate(level, placedBox, originY + template.getSize().getY(), maxSurface - 1);
        if (!template.placeInWorld(level, origin, origin, settings, random, Block.UPDATE_ALL)) {
            reject(failures, SiteFailure.TEMPLATE_FAILED);
            return Optional.empty();
        }

        supportFoundation(level, placedBox, originY, variant.foundation());
        BlockPos anchor = transformLocal(origin, candidate.rotation(), LOCAL_ANCHOR);
        BlockPos station = transformLocal(origin, candidate.rotation(), LOCAL_STATION);
        if (!level.getBlockState(station).is(ModBlocks.BARTENDER_STATION.get())) {
            reject(failures, SiteFailure.STATION_MISSING);
            TrippyCraft.LOGGER.error("Bar {} placed without its bartender station at {}", variant.location(), station);
            return Optional.empty();
        }

        spawnBartender(level, anchor, station);
        connectPath(level, candidate.frontAtZero(), candidate.side(), originY, variant.pathBlock());
        return Optional.of(new VillageBarData.BarPlacement(
                variant.id(),
                origin,
                candidate.rotation(),
                anchor,
                station
        ));
    }

    private static void reject(EnumMap<SiteFailure, Integer> failures, SiteFailure failure) {
        failures.merge(failure, 1, Integer::sum);
    }

    private static boolean intersectsHorizontally(BoundingBox first, BoundingBox second) {
        return first.maxX() >= second.minX()
                && first.minX() <= second.maxX()
                && first.maxZ() >= second.minZ()
                && first.minZ() <= second.maxZ();
    }

    private static boolean isSafeSite(ServerLevel level,
                                      BoundingBox box,
                                      int minimumGroundY,
                                      int highestTerrainY) {
        int scanTop = Math.max(box.maxY(), highestTerrainY);
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int y = Math.max(minimumGroundY, box.minY()); y <= scanTop; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockEntity(pos) != null) {
                        return false;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.is(ModBlocks.BARTENDER_STATION.get())) {
                        return false;
                    }
                    if (y > minimumGroundY + 1 && !isNaturalOrReplaceable(state)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void clearOverburdenAboveTemplate(ServerLevel level,
                                                      BoundingBox box,
                                                      int clearFromY,
                                                      int clearToY) {
        if (clearFromY > clearToY) {
            return;
        }
        for (int y = clearToY; y >= clearFromY; y--) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isNaturalOrReplaceable(level.getBlockState(pos))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static boolean isNaturalOrReplaceable(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(BlockTags.ICE)
                || state.is(BlockTags.SNOW)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.MUD)
                || state.is(Blocks.DIRT_PATH);
    }

    private static void supportFoundation(ServerLevel level, BoundingBox box, int floorY, BlockState foundation) {
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                BlockPos floor = new BlockPos(x, floorY, z);
                if (level.getBlockState(floor).isAir()) {
                    continue;
                }
                for (int depth = 1; depth <= MAX_FOUNDATION_DROP + 1; depth++) {
                    BlockPos support = floor.below(depth);
                    BlockState state = level.getBlockState(support);
                    if (!state.isAir() && !state.canBeReplaced()) {
                        break;
                    }
                    level.setBlock(support, foundation, Block.UPDATE_ALL);
                }
            }
        }
    }

    private static void connectPath(ServerLevel level, BlockPos frontAtZero, SiteSide side, int referenceY, BlockState pathBlock) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(frontAtZero.getX(), referenceY, frontAtZero.getZ());
        for (int step = 1; step <= 8; step++) {
            cursor.move(side.towardVillage());
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cursor.getX(), cursor.getZ());
            BlockPos ground = new BlockPos(cursor.getX(), surface - 1, cursor.getZ());
            BlockState state = level.getBlockState(ground);
            if (!level.getFluidState(ground).isEmpty() || !isNaturalOrReplaceable(state)) {
                continue;
            }
            level.setBlock(ground, pathBlock, Block.UPDATE_ALL);
            BlockPos above = ground.above();
            if (level.getBlockState(above).canBeReplaced()) {
                level.removeBlock(above, false);
            }
        }
    }

    private static void ensureBartenders(ServerLevel level, VillageBarData.VillageRecord record) {
        for (VillageBarData.BarPlacement bar : record.bars()) {
            if (!level.hasChunkAt(bar.station()) || !level.getBlockState(bar.station()).is(ModBlocks.BARTENDER_STATION.get())) {
                continue;
            }
            List<Villager> bartenders = level.getEntitiesOfClass(
                    Villager.class,
                    new net.minecraft.world.phys.AABB(bar.anchor()).inflate(4.0D),
                    villager -> BartenderService.isBartender(villager)
                            && BartenderService.storedStation(villager).equals(bar.station())
            );
            if (bartenders.isEmpty()) {
                spawnBartender(level, bar.anchor(), bar.station());
            }
        }
    }

    private static void spawnBartender(ServerLevel level, BlockPos anchor, BlockPos station) {
        Villager bartender = EntityType.VILLAGER.create(level);
        if (bartender == null) {
            TrippyCraft.LOGGER.error("Could not create bartender villager at {}", anchor);
            return;
        }
        bartender.moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 0.0F, 0.0F);
        BartenderService.configure(bartender, anchor, station);
        level.addFreshEntity(bartender);
    }

    private static BlockPos transformLocal(BlockPos origin, Rotation rotation, BlockPos local) {
        return StructureTemplate.transform(local, Mirror.NONE, rotation, BlockPos.ZERO).offset(origin);
    }

    private static net.minecraft.world.phys.AABB box(BoundingBox box) {
        return new net.minecraft.world.phys.AABB(
                box.minX(), box.minY(), box.minZ(),
                box.maxX() + 1.0D, box.maxY() + 1.0D, box.maxZ() + 1.0D
        );
    }

    private static BarVariant variantFor(ServerLevel level, StructureStart village) {
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceLocation key = structures.getKey(village.getStructure());
        String path = key == null ? "plains" : key.getPath();
        if (path.contains("desert")) {
            return BarVariant.DESERT_CANTINA;
        }
        if (path.contains("savanna")) {
            return BarVariant.SAVANNA_TAVERN;
        }
        if (path.contains("snowy")) {
            return BarVariant.SNOWY_LODGE;
        }
        if (path.contains("taiga")) {
            return BarVariant.TAIGA_ALEHOUSE;
        }
        return BarVariant.OAK_TAPROOM;
    }

    private static List<SiteSide> rotatedSides(int offset) {
        List<SiteSide> sides = new ArrayList<>(List.of(SiteSide.values()));
        sides.sort(Comparator.comparingInt(side -> Math.floorMod(side.ordinal() - offset, sides.size())));
        return sides;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private enum SiteSide {
        NORTH(Rotation.CLOCKWISE_180, net.minecraft.core.Direction.SOUTH),
        EAST(Rotation.COUNTERCLOCKWISE_90, net.minecraft.core.Direction.WEST),
        SOUTH(Rotation.NONE, net.minecraft.core.Direction.NORTH),
        WEST(Rotation.CLOCKWISE_90, net.minecraft.core.Direction.EAST);

        private final Rotation rotation;
        private final net.minecraft.core.Direction towardVillage;

        SiteSide(Rotation rotation, net.minecraft.core.Direction towardVillage) {
            this.rotation = rotation;
            this.towardVillage = towardVillage;
        }

        public Rotation rotation() {
            return rotation;
        }

        public net.minecraft.core.Direction towardVillage() {
            return towardVillage;
        }

        public static SiteSide forRotation(Rotation rotation) {
            for (SiteSide side : values()) {
                if (side.rotation == rotation) {
                    return side;
                }
            }
            throw new IllegalArgumentException("Unsupported village bar rotation " + rotation);
        }
    }

    private enum BarVariant {
        OAK_TAPROOM("oak_taproom", "Oak Taproom", Blocks.COBBLESTONE.defaultBlockState(), Blocks.DIRT_PATH.defaultBlockState()),
        DESERT_CANTINA("desert_cantina", "Desert Cantina", Blocks.SMOOTH_SANDSTONE.defaultBlockState(), Blocks.SMOOTH_SANDSTONE.defaultBlockState()),
        SAVANNA_TAVERN("savanna_tavern", "Savanna Tavern", Blocks.MUD_BRICKS.defaultBlockState(), Blocks.DIRT_PATH.defaultBlockState()),
        SNOWY_LODGE("snowy_lodge", "Snowy Lodge", Blocks.STONE_BRICKS.defaultBlockState(), Blocks.DIRT_PATH.defaultBlockState()),
        TAIGA_ALEHOUSE("taiga_alehouse", "Taiga Alehouse", Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.DIRT_PATH.defaultBlockState());

        private final String id;
        private final String displayName;
        private final BlockState foundation;
        private final BlockState pathBlock;

        BarVariant(String id, String displayName, BlockState foundation, BlockState pathBlock) {
            this.id = id;
            this.displayName = displayName;
            this.foundation = foundation;
            this.pathBlock = pathBlock;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public ResourceLocation location() {
            return ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "village/bars/" + id);
        }

        public BlockState foundation() {
            return foundation;
        }

        public BlockState pathBlock() {
            return pathBlock;
        }

        public static Optional<BarVariant> byId(String id) {
            for (BarVariant variant : values()) {
                if (variant.id.equals(id)) {
                    return Optional.of(variant);
                }
            }
            return Optional.empty();
        }
    }

    private record Candidate(BlockPos originAtZero,
                             Rotation rotation,
                             BoundingBox boxAtZero,
                             BlockPos frontAtZero,
                             SiteSide side) {
    }

    private enum SiteFailure {
        VILLAGE_OVERLAP,
        CHUNKS_UNLOADED,
        EXISTING_BAR_NEARBY,
        FLUID,
        SLOPE,
        OBSTRUCTED,
        ENTITY,
        TEMPLATE_FAILED,
        STATION_MISSING
    }
}
