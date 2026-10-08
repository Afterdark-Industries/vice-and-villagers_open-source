package com.trippycraft.world;

import com.trippycraft.TrippyCraft;
import com.trippycraft.compat.NbtCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-dimension record that makes bar placement deterministic and one-time. */
public final class VillageBarData extends SavedData {
    public static final int CURRENT_BAR_REVISION = 4;
    public static final String NAME = TrippyCraft.MOD_ID + "_village_bars";
    public static final SavedData.Factory<VillageBarData> FACTORY =
            new SavedData.Factory<>(VillageBarData::new, VillageBarData::load);
    private static final String VILLAGES_KEY = "villages";

    private final Map<Long, VillageRecord> villages = new HashMap<>();

    public VillageRecord getOrCreate(long villageStartChunk, int targetCount) {
        VillageRecord record = villages.get(villageStartChunk);
        if (record != null) {
            return record;
        }
        VillageRecord created = new VillageRecord(Math.clamp(targetCount, 1, 2), new ArrayList<>());
        villages.put(villageStartChunk, created);
        setDirty();
        return created;
    }

    public void addBar(long villageStartChunk, BarPlacement placement) {
        VillageRecord record = villages.get(villageStartChunk);
        if (record == null) {
            throw new IllegalStateException("Village record must exist before adding a bar");
        }
        record.bars().add(placement);
        setDirty();
    }

    public void replaceBar(long villageStartChunk, int index, BarPlacement placement) {
        VillageRecord record = villages.get(villageStartChunk);
        if (record == null || index < 0 || index >= record.bars().size()) {
            throw new IllegalArgumentException("Cannot replace an unknown village bar placement");
        }
        record.bars().set(index, placement);
        setDirty();
    }

    public List<BarPlacement> allBars() {
        List<BarPlacement> bars = new ArrayList<>();
        for (VillageRecord village : villages.values()) {
            bars.addAll(village.bars());
        }
        return List.copyOf(bars);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag villageList = new ListTag();
        for (Map.Entry<Long, VillageRecord> entry : villages.entrySet()) {
            CompoundTag storedVillage = new CompoundTag();
            storedVillage.putLong("start_chunk", entry.getKey());
            storedVillage.putInt("target_count", entry.getValue().targetCount());

            ListTag bars = new ListTag();
            for (BarPlacement bar : entry.getValue().bars()) {
                CompoundTag storedBar = new CompoundTag();
                storedBar.putString("template", bar.template());
                storedBar.putLong("origin", bar.origin().asLong());
                storedBar.putString("rotation", bar.rotation().name());
                storedBar.putLong("anchor", bar.anchor().asLong());
                storedBar.putLong("station", bar.station().asLong());
                storedBar.putInt("revision", bar.revision());
                bars.add(storedBar);
            }
            storedVillage.put("bars", bars);
            villageList.add(storedVillage);
        }
        tag.put(VILLAGES_KEY, villageList);
        return tag;
    }

    private static VillageBarData load(CompoundTag tag, HolderLookup.Provider registries) {
        VillageBarData data = new VillageBarData();
        ListTag villageList = tag.getList(VILLAGES_KEY, Tag.TAG_COMPOUND);
        for (Tag rawVillage : villageList) {
            CompoundTag storedVillage = (CompoundTag) rawVillage;
            long startChunk = NbtCompat.getLongOr(storedVillage, "start_chunk", Long.MIN_VALUE);
            if (startChunk == Long.MIN_VALUE) {
                continue;
            }

            List<BarPlacement> bars = new ArrayList<>();
            for (Tag rawBar : storedVillage.getList("bars", Tag.TAG_COMPOUND)) {
                CompoundTag storedBar = (CompoundTag) rawBar;
                try {
                    bars.add(new BarPlacement(
                            NbtCompat.getStringOr(storedBar, "template", "oak_taproom"),
                            BlockPos.of(NbtCompat.getLongOr(storedBar, "origin", BlockPos.ZERO.asLong())),
                            Rotation.valueOf(NbtCompat.getStringOr(storedBar, "rotation", Rotation.NONE.name())),
                            BlockPos.of(NbtCompat.getLongOr(storedBar, "anchor", BlockPos.ZERO.asLong())),
                            BlockPos.of(NbtCompat.getLongOr(storedBar, "station", BlockPos.ZERO.asLong())),
                            NbtCompat.getIntOr(storedBar, "revision", 0)
                    ));
                } catch (IllegalArgumentException ignored) {
                    // Ignore a corrupt individual placement while preserving the rest of the village data.
                }
            }
            data.villages.put(startChunk, new VillageRecord(
                    Math.clamp(NbtCompat.getIntOr(storedVillage, "target_count", 1), 1, 2),
                    bars
            ));
        }
        return data;
    }

    public record VillageRecord(int targetCount, List<BarPlacement> bars) {
    }

    public record BarPlacement(String template,
                               BlockPos origin,
                               Rotation rotation,
                               BlockPos anchor,
                               BlockPos station,
                               int revision) {
        public BarPlacement(String template,
                            BlockPos origin,
                            Rotation rotation,
                            BlockPos anchor,
                            BlockPos station) {
            this(template, origin, rotation, anchor, station, CURRENT_BAR_REVISION);
        }
    }
}
