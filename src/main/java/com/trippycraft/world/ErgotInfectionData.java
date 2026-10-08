package com.trippycraft.world;

import com.trippycraft.TrippyCraft;
import com.trippycraft.compat.NbtCompat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

public final class ErgotInfectionData extends SavedData {
    public static final String NAME = TrippyCraft.MOD_ID + "_ergot_infection";
    public static final SavedData.Factory<ErgotInfectionData> FACTORY =
            new SavedData.Factory<>(ErgotInfectionData::new, ErgotInfectionData::load);
    private static final String ENTRIES_KEY = "tracked_wheat";

    private final Map<Long, Long> shadedSinceByPos = new HashMap<>();

    public void track(BlockPos pos, long gameTime) {
        long key = pos.asLong();
        if (!shadedSinceByPos.containsKey(key)) {
            shadedSinceByPos.put(key, gameTime);
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (shadedSinceByPos.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    public List<TrackedWheat> snapshot() {
        return shadedSinceByPos.entrySet().stream()
                .map(entry -> new TrackedWheat(BlockPos.of(entry.getKey()), entry.getValue()))
                .toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (TrackedWheat entry : snapshot()) {
            CompoundTag stored = new CompoundTag();
            stored.putLong("pos", entry.pos().asLong());
            stored.putLong("shaded_since", entry.shadedSince());
            entries.add(stored);
        }
        tag.put(ENTRIES_KEY, entries);
        return tag;
    }

    private static ErgotInfectionData load(CompoundTag tag, HolderLookup.Provider registries) {
        ErgotInfectionData data = new ErgotInfectionData();
        ListTag entries = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (Tag rawEntry : entries) {
            CompoundTag stored = (CompoundTag) rawEntry;
            long pos = NbtCompat.getLongOr(stored, "pos", Long.MIN_VALUE);
            if (pos != Long.MIN_VALUE) {
                data.shadedSinceByPos.put(pos, NbtCompat.getLongOr(stored, "shaded_since", 0L));
            }
        }
        return data;
    }

    public record TrackedWheat(BlockPos pos, long shadedSince) {
    }
}
