package com.trippycraft.world;

import com.trippycraft.TrippyCraft;
import com.trippycraft.compat.NbtCompat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent per-tavern playlist offsets used when a player skips a track. */
public final class TavernPlaybackData extends SavedData {
    public static final String NAME = TrippyCraft.MOD_ID + "_tavern_playback";
    public static final SavedData.Factory<TavernPlaybackData> FACTORY =
            new SavedData.Factory<>(TavernPlaybackData::new, TavernPlaybackData::load);
    private static final String OFFSETS_KEY = "offsets";

    private final Map<Long, Long> offsets = new HashMap<>();

    public long timelineTick(long gameTime, VillageBarData.BarPlacement bar) {
        long base = TavernPlaylist.timelineTick(gameTime, bar.origin(), bar.rotation());
        return Math.floorMod(base + offsets.getOrDefault(bar.origin().asLong(), 0L),
                TavernPlaylist.totalCycleTicks());
    }

    public long skipToNext(long gameTime, VillageBarData.BarPlacement bar) {
        long currentTick = timelineTick(gameTime, bar);
        TavernPlaylist.Selection current = TavernPlaylist.selection(currentTick);
        long advancement = current.durationTicks() - current.elapsedTicks();
        long key = bar.origin().asLong();
        offsets.put(key, Math.floorMod(offsets.getOrDefault(key, 0L) + advancement,
                TavernPlaylist.totalCycleTicks()));
        setDirty();
        return timelineTick(gameTime, bar);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag storedOffsets = new ListTag();
        for (Map.Entry<Long, Long> entry : offsets.entrySet()) {
            CompoundTag storedOffset = new CompoundTag();
            storedOffset.putLong("origin", entry.getKey());
            storedOffset.putLong("ticks", entry.getValue());
            storedOffsets.add(storedOffset);
        }
        tag.put(OFFSETS_KEY, storedOffsets);
        return tag;
    }

    private static TavernPlaybackData load(CompoundTag tag, HolderLookup.Provider registries) {
        TavernPlaybackData data = new TavernPlaybackData();
        for (Tag rawOffset : tag.getList(OFFSETS_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag storedOffset = (CompoundTag) rawOffset;
            if (!storedOffset.contains("origin", Tag.TAG_ANY_NUMERIC)) {
                continue;
            }
            long origin = storedOffset.getLong("origin");
            long ticks = Math.floorMod(NbtCompat.getLongOr(storedOffset, "ticks", 0L),
                    TavernPlaylist.totalCycleTicks());
            data.offsets.put(origin, ticks);
        }
        return data;
    }
}
