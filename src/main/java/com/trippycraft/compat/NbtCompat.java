package com.trippycraft.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Backports the default-valued CompoundTag accessors introduced after Minecraft 1.21.1.
 */
public final class NbtCompat {
    private NbtCompat() {
    }

    public static int getIntOr(CompoundTag tag, String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    public static long getLongOr(CompoundTag tag, String key, long fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : fallback;
    }

    public static double getDoubleOr(CompoundTag tag, String key, double fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getDouble(key) : fallback;
    }

    public static boolean getBooleanOr(CompoundTag tag, String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback;
    }

    public static String getStringOr(CompoundTag tag, String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : new CompoundTag();
    }

    public static boolean hasCompound(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND);
    }

    public static Optional<CompoundTag> getOptionalCompound(CompoundTag tag, String key) {
        return hasCompound(tag, key) ? Optional.of(tag.getCompound(key)) : Optional.empty();
    }

    public static CompoundTag getOrCreateCompound(CompoundTag tag, String key) {
        if (hasCompound(tag, key)) {
            return tag.getCompound(key);
        }
        CompoundTag created = new CompoundTag();
        tag.put(key, created);
        return created;
    }

    public static ListTag getListOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LIST) ? tag.getList(key, Tag.TAG_COMPOUND) : new ListTag();
    }

    public static Stream<CompoundTag> compoundStream(ListTag list) {
        return list.stream()
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast);
    }
}
