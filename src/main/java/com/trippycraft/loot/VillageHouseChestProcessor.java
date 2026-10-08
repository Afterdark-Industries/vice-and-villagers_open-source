package com.trippycraft.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.trippycraft.registry.ModStructureProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VillageHouseChestProcessor extends StructureProcessor {
    public static final MapCodec<VillageHouseChestProcessor> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    StructureProcessorType.LIST_CODEC.fieldOf("base_processors").forGetter(processor -> processor.baseProcessors),
                    ResourceLocation.CODEC.fieldOf("loot_table").forGetter(processor -> processor.lootTable))
                    .apply(instance, VillageHouseChestProcessor::new));

    private static final EmptyBlockGetter EMPTY_BLOCK_GETTER = EmptyBlockGetter.INSTANCE;
    private static final List<Direction> HORIZONTAL_DIRECTIONS = List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

    private final Holder<StructureProcessorList> baseProcessors;
    private final ResourceLocation lootTable;

    public VillageHouseChestProcessor(Holder<StructureProcessorList> baseProcessors, ResourceLocation lootTable) {
        this.baseProcessors = baseProcessors;
        this.lootTable = lootTable;
    }

    @Override
    public @Nullable StructureTemplate.StructureBlockInfo process(
            net.minecraft.world.level.LevelReader level,
            BlockPos seed,
            BlockPos offset,
            StructureTemplate.StructureBlockInfo rawBlock,
            StructureTemplate.StructureBlockInfo block,
            StructurePlaceSettings settings,
            @Nullable StructureTemplate template
    ) {
        StructureTemplate.StructureBlockInfo processed = block;

        for (StructureProcessor baseProcessor : this.baseProcessors.value().list()) {
            if (processed == null) {
                break;
            }

            processed = baseProcessor.process(level, seed, offset, rawBlock, processed, settings, template);
        }

        return processed;
    }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,
            BlockPos seed,
            BlockPos offset,
            List<StructureTemplate.StructureBlockInfo> rawBlocks,
            List<StructureTemplate.StructureBlockInfo> processedBlocks,
            StructurePlaceSettings settings
    ) {
        List<StructureTemplate.StructureBlockInfo> result = processedBlocks;

        for (StructureProcessor baseProcessor : this.baseProcessors.value().list()) {
            result = baseProcessor.finalizeProcessing(level, seed, offset, rawBlocks, result, settings);
        }

        if (containsContainer(result)) {
            return result;
        }

        Optional<ChestPlacement> chestPlacement = findChestPlacement(result);
        if (chestPlacement.isEmpty()) {
            return result;
        }

        List<StructureTemplate.StructureBlockInfo> augmented = new ArrayList<>(result);
        augmented.add(createChestBlockInfo(chestPlacement.get()));
        return augmented;
    }

    @Override
    protected @NotNull StructureProcessorType<?> getType() {
        return ModStructureProcessors.VILLAGE_HOUSE_CHEST.get();
    }

    private boolean containsContainer(List<StructureTemplate.StructureBlockInfo> blocks) {
        for (StructureTemplate.StructureBlockInfo blockInfo : blocks) {
            BlockState state = blockInfo.state();
            if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL)) {
                return true;
            }
        }

        return false;
    }

    private Optional<ChestPlacement> findChestPlacement(List<StructureTemplate.StructureBlockInfo> blocks) {
        if (blocks.isEmpty()) {
            return Optional.empty();
        }

        Map<BlockPos, BlockState> statesByPosition = new HashMap<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (StructureTemplate.StructureBlockInfo blockInfo : blocks) {
            BlockPos pos = blockInfo.pos();
            statesByPosition.put(pos, blockInfo.state());
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        List<ChestPlacement> strongCandidates = new ArrayList<>();
        List<ChestPlacement> fallbackCandidates = new ArrayList<>();
        for (int y = minY + 1; y < maxY; y++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                for (int x = minX + 1; x < maxX; x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = stateAt(statesByPosition, pos);
                    if (!isAirLike(state)) {
                        continue;
                    }

                    BlockState floor = stateAt(statesByPosition, pos.below());
                    if (!isFloor(floor)) {
                        continue;
                    }

                    BlockState above = stateAt(statesByPosition, pos.above());
                    if (!isAirLike(above)) {
                        continue;
                    }

                    if (!hasRoof(statesByPosition, pos, maxY)) {
                        continue;
                    }

                    int openSides = 0;
                    int solidSides = 0;
                    Direction wallBackedFacing = null;
                    Direction firstOpenFacing = null;
                    for (Direction direction : HORIZONTAL_DIRECTIONS) {
                        BlockState neighbor = stateAt(statesByPosition, pos.relative(direction));
                        if (isAirLike(neighbor)) {
                            openSides++;
                            if (firstOpenFacing == null) {
                                firstOpenFacing = direction;
                            }
                            continue;
                        }

                        if (isWall(neighbor)) {
                            solidSides++;
                            if (wallBackedFacing == null && isAirLike(stateAt(statesByPosition, pos.relative(direction.getOpposite())))) {
                                wallBackedFacing = direction.getOpposite();
                            }
                        }
                    }

                    Direction facing = wallBackedFacing != null ? wallBackedFacing : firstOpenFacing;
                    if (facing == null) {
                        continue;
                    }

                    int score = scoreCandidate(pos, minX, minY, minZ, maxX, maxZ, openSides, solidSides);
                    ChestPlacement placement = new ChestPlacement(pos, facing, score);
                    if (solidSides >= 1 && openSides >= 1) {
                        strongCandidates.add(placement);
                    } else {
                        fallbackCandidates.add(placement);
                    }
                }
            }
        }

        List<ChestPlacement> candidates = !strongCandidates.isEmpty() ? strongCandidates : fallbackCandidates;
        return candidates.stream()
                .max(Comparator.comparingInt(ChestPlacement::score)
                        .thenComparingInt(candidate -> -candidate.pos().getY())
                        .thenComparingInt(candidate -> -candidate.pos().getZ())
                        .thenComparingInt(candidate -> -candidate.pos().getX()));
    }

    private int scoreCandidate(BlockPos pos, int minX, int minY, int minZ, int maxX, int maxZ, int openSides, int solidSides) {
        int score = 0;
        score += openSides * 3;
        score += solidSides * 4;
        score += Math.max(0, 4 - (pos.getY() - minY));
        if (pos.getX() > minX + 1 && pos.getX() < maxX - 1) {
            score += 2;
        }
        if (pos.getZ() > minZ + 1 && pos.getZ() < maxZ - 1) {
            score += 2;
        }
        return score;
    }

    private boolean hasRoof(Map<BlockPos, BlockState> statesByPosition, BlockPos pos, int maxY) {
        int upperBound = Math.min(maxY, pos.getY() + 5);
        for (int y = pos.getY() + 2; y <= upperBound; y++) {
            BlockState stateAbove = stateAt(statesByPosition, new BlockPos(pos.getX(), y, pos.getZ()));
            if (!isAirLike(stateAbove)) {
                return true;
            }
        }

        return false;
    }

    private BlockState stateAt(Map<BlockPos, BlockState> statesByPosition, BlockPos pos) {
        return statesByPosition.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    private boolean isAirLike(BlockState state) {
        return state.isAir();
    }

    private boolean isFloor(BlockState state) {
        return state.isFaceSturdy(EMPTY_BLOCK_GETTER, BlockPos.ZERO, Direction.UP);
    }

    private boolean isWall(BlockState state) {
        return !state.isAir() && state.isFaceSturdy(EMPTY_BLOCK_GETTER, BlockPos.ZERO, Direction.UP) && state.blocksMotion();
    }

    private StructureTemplate.StructureBlockInfo createChestBlockInfo(ChestPlacement placement) {
        BlockState chestState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, placement.facing());
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("LootTable", this.lootTable.toString());
        return new StructureTemplate.StructureBlockInfo(placement.pos(), chestState, tag);
    }

    private record ChestPlacement(BlockPos pos, Direction facing, int score) {
    }
}
