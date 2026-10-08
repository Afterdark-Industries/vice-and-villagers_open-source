package com.trippycraft.world;

import com.trippycraft.TrippyCraft;
import com.trippycraft.economy.BartenderService;
import com.trippycraft.economy.DealerService;
import com.trippycraft.registry.ModBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Integration coverage for exact-count, idempotent village bar placement. */
@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VillageBarGameTests {
    private VillageBarGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom", timeoutTicks = 200)
    public static void villagePlacementCreatesExactPersistentCount(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure village = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getOrThrow(BuiltinStructures.VILLAGE_PLAINS);
        BlockPos generationPoint = helper.absolutePos(new BlockPos(48, 0, 48));
        ChunkPos villageChunk = new ChunkPos(generationPoint);
        StructureStart start = village.generate(
                level.registryAccess(),
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGenerator().getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getStructureManager(),
                level.getSeed(),
                villageChunk,
                0,
                level,
                ignored -> true
        );
        helper.assertTrue(start.isValid(), "Could not generate the synthetic plains village start");

        BoundingBox loadedArea = start.getBoundingBox().inflatedBy(64);
        for (int chunkZ = Math.floorDiv(loadedArea.minZ(), 16); chunkZ <= Math.floorDiv(loadedArea.maxZ(), 16); chunkZ++) {
            for (int chunkX = Math.floorDiv(loadedArea.minX(), 16); chunkX <= Math.floorDiv(loadedArea.maxX(), 16); chunkX++) {
                level.getChunk(chunkX, chunkZ);
            }
        }

        long villageKey = villageChunk.toLong() ^ 0x715A4E2BL;
        VillageBarData data = level.getDataStorage().computeIfAbsent(VillageBarData.FACTORY, VillageBarData.NAME);
        VillageBarData.VillageRecord firstPass = VillageBarEvents.processVillageStart(level, data, villageKey, start);
        helper.assertTrue(firstPass.bars().size() == firstPass.targetCount(),
                "Village placement did not reach its deterministic one-or-two target");
        helper.assertTrue(firstPass.targetCount() >= 1 && firstPass.targetCount() <= 2,
                "Village target escaped the requested one-or-two range");

        int firstCount = firstPass.bars().size();
        for (VillageBarData.BarPlacement bar : firstPass.bars()) {
            helper.assertTrue(level.getBlockState(bar.station()).is(ModBlocks.BARTENDER_STATION.get()),
                    "A placed bar is missing its bartender station");
            long bartenders = level.getEntitiesOfClass(
                    Villager.class,
                    new net.minecraft.world.phys.AABB(bar.anchor()).inflate(4.0D),
                    villagerEntity -> BartenderService.isBartender(villagerEntity)
                            && BartenderService.storedStation(villagerEntity).equals(bar.station())
            ).size();
            helper.assertTrue(bartenders == 1L, "A placed bar must have exactly one anchored bartender");
        }

        VillageBarData.VillageRecord secondPass = VillageBarEvents.processVillageStart(level, data, villageKey, start);
        helper.assertTrue(secondPass.bars().size() == firstCount,
                "Repeated village processing created duplicate bars");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom", timeoutTicks = 200)
    public static void legacyBarMigratesToCustomTavernFootprint(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure village = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getOrThrow(BuiltinStructures.VILLAGE_PLAINS);
        BlockPos generationPoint = helper.absolutePos(new BlockPos(64, 0, 64));
        ChunkPos villageChunk = new ChunkPos(generationPoint);
        StructureStart start = village.generate(
                level.registryAccess(),
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGenerator().getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getStructureManager(),
                level.getSeed(),
                villageChunk,
                0,
                level,
                ignored -> true
        );
        helper.assertTrue(start.isValid(), "Could not generate the synthetic plains village start");

        BoundingBox loadedArea = start.getBoundingBox().inflatedBy(64);
        for (int chunkZ = Math.floorDiv(loadedArea.minZ(), 16); chunkZ <= Math.floorDiv(loadedArea.maxZ(), 16); chunkZ++) {
            for (int chunkX = Math.floorDiv(loadedArea.minX(), 16); chunkX <= Math.floorDiv(loadedArea.maxX(), 16); chunkX++) {
                level.getChunk(chunkX, chunkZ);
            }
        }

        BlockPos oldOrigin = helper.absolutePos(new BlockPos(24, 1, 24));
        BlockPos oldStation = oldOrigin.offset(6, 1, 6);
        BlockPos oldAnchor = oldOrigin.offset(6, 1, 7);
        BlockPos legacyEdgeMarker = oldOrigin.offset(12, 5, 10);
        level.setBlock(oldStation, ModBlocks.BARTENDER_STATION.get().defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(legacyEdgeMarker, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);

        long villageKey = villageChunk.toLong() ^ 0x43A71B9DL;
        VillageBarData data = level.getDataStorage().computeIfAbsent(VillageBarData.FACTORY, VillageBarData.NAME);
        data.getOrCreate(villageKey, 1);
        data.addBar(villageKey, new VillageBarData.BarPlacement(
                "oak_taproom",
                oldOrigin,
                Rotation.NONE,
                oldAnchor,
                oldStation,
                2
        ));

        VillageBarData.VillageRecord migrated = VillageBarEvents.processVillageStart(level, data, villageKey, start);
        VillageBarData.BarPlacement bar = migrated.bars().getFirst();
        BlockPos expectedStation = bar.origin().offset(4, 1, 11);
        helper.assertTrue(bar.revision() == VillageBarData.CURRENT_BAR_REVISION,
                "Legacy bar did not advance to the current custom-tavern revision");
        helper.assertTrue(bar.station().equals(expectedStation),
                "Migrated bartender station did not follow the custom tavern layout");
        helper.assertTrue(level.getBlockState(expectedStation).is(ModBlocks.BARTENDER_STATION.get()),
                "Migrated custom tavern is missing its bartender station");
        helper.assertTrue(level.getBlockState(legacyEdgeMarker).isAir(),
                "Legacy 13x10x11 footprint was not fully cleared");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom", timeoutTicks = 80)
    public static void existingCustomTavernGetsLightsWithoutErasingDecoration(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos station = origin.offset(4, 1, 11);
        BlockPos anchor = origin.offset(4, 1, 9);
        BlockPos decoratedFixture = origin.offset(3, 4, 4);
        level.setBlock(decoratedFixture, Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(origin.offset(6, 4, 4), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(origin.offset(3, 4, 7), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(origin.offset(6, 4, 7), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        long villageKey = origin.asLong() ^ 0x524156454C494748L;
        VillageBarData data = level.getDataStorage().computeIfAbsent(VillageBarData.FACTORY, VillageBarData.NAME);
        data.getOrCreate(villageKey, 1);
        data.addBar(villageKey, new VillageBarData.BarPlacement(
                "oak_taproom",
                origin,
                Rotation.NONE,
                anchor,
                station,
                3
        ));

        VillageBarData.VillageRecord migrated = VillageBarEvents.processVillageStart(
                level,
                data,
                villageKey,
                StructureStart.INVALID_START
        );
        VillageBarData.BarPlacement bar = migrated.bars().getFirst();
        helper.assertTrue(bar.revision() == VillageBarData.CURRENT_BAR_REVISION,
                "Existing custom tavern did not advance to the rave-light revision");
        helper.assertTrue(level.getBlockState(decoratedFixture).is(Blocks.DIAMOND_BLOCK),
                "Lighting migration overwrote a player decoration");
        helper.assertTrue(level.getBlockState(origin.offset(6, 4, 4)).is(Blocks.VERDANT_FROGLIGHT),
                "Lighting migration missed the verdant fixture");
        helper.assertTrue(level.getBlockState(origin.offset(3, 4, 7)).is(Blocks.OCHRE_FROGLIGHT),
                "Lighting migration missed the ochre fixture");
        helper.assertTrue(level.getBlockState(origin.offset(6, 4, 7)).is(Blocks.SEA_LANTERN),
                "Lighting migration missed the sea-lantern fixture");
        helper.assertTrue(TavernAmbienceService.jukeboxPosition(bar).equals(origin.offset(2, 1, 7)),
                "Tavern ambience did not transform the jukebox source correctly");

        BlockPos originalJukebox = TavernAmbienceService.jukeboxPosition(bar);
        BlockPos replacementJukebox = TavernGeometry.transform(bar, new BlockPos(7, 1, 5));
        helper.assertTrue(TavernAmbienceService.findJukebox(level, bar).orElseThrow().equals(originalJukebox),
                "Tavern ambience did not discover the authored jukebox");
        level.setBlock(originalJukebox, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(replacementJukebox, Blocks.JUKEBOX.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(TavernAmbienceService.findJukebox(level, bar).orElseThrow().equals(replacementJukebox),
                "A replacement jukebox elsewhere inside the tavern did not restore playback");
        level.setBlock(replacementJukebox, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(TavernAmbienceService.findJukebox(level, bar).isEmpty(),
                "Tavern playback remained active after its jukebox was destroyed");

        TavernPlaybackData playback = new TavernPlaybackData();
        TavernPlaylist.Selection beforeSkip = TavernPlaylist.selection(playback.timelineTick(12_345L, bar));
        TavernPlaylist.Selection afterSkip = TavernPlaylist.selection(playback.skipToNext(12_345L, bar));
        helper.assertTrue(afterSkip.trackIndex() == (beforeSkip.trackIndex() + 1) % TavernPlaylist.trackCount()
                        && afterSkip.elapsedTicks() == 0,
                "Right-click skip state did not advance exactly to the next tavern song");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom", timeoutTicks = 80)
    public static void tavernNightlifeHasFourPatronsAndOneDealer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        VillageBarData.BarPlacement bar = new VillageBarData.BarPlacement(
                "oak_taproom",
                origin,
                Rotation.NONE,
                origin.offset(4, 1, 9),
                origin.offset(4, 1, 11)
        );
        AABB searchArea = new AABB(origin).inflate(20.0D, 10.0D, 20.0D);

        TavernNightlifeService.updateBar(level, bar, true);
        List<Villager> firstNight = level.getEntitiesOfClass(
                Villager.class,
                searchArea,
                TavernNightlifeService::isNightlifeGuest
        );
        helper.assertTrue(firstNight.size() == 5,
                "Nighttime tavern must contain four patrons and one dealer");
        helper.assertTrue(firstNight.stream().filter(TavernNightlifeService::isNightlifeDealer).count() == 1L,
                "Nighttime tavern must contain exactly one dedicated dealer");
        helper.assertTrue(firstNight.stream().filter(DealerService::isDealerVillager).count() == 1L,
                "Only the dedicated nightlife guest may have the dealer role");

        TavernNightlifeService.updateBar(level, bar, true);
        List<Villager> secondPass = level.getEntitiesOfClass(
                Villager.class,
                searchArea,
                TavernNightlifeService::isNightlifeGuest
        );
        helper.assertTrue(secondPass.size() == 5,
                "Repeated nightlife maintenance duplicated tavern guests");

        TavernNightlifeService.updateBar(level, bar, false);
        List<Villager> morning = level.getEntitiesOfClass(
                Villager.class,
                searchArea,
                TavernNightlifeService::isNightlifeGuest
        );
        helper.assertTrue(morning.isEmpty(), "Nighttime tavern guests remained after dawn");
        helper.succeed();
    }
}
