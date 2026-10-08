package com.trippycraft.economy;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.TrippyCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public final class MarketPersistence {
    private static final int SCHEMA_VERSION = 1;

    private MarketPersistence() {
    }

    public static Optional<MarketPlayerData> load(File file) {
        if (!file.isFile()) {
            return Optional.empty();
        }

        try {
            CompoundTag root = NbtIo.read(file.toPath());
            if (root == null || NbtCompat.getIntOr(root, "schema", 0) != SCHEMA_VERSION) {
                return Optional.empty();
            }
            return Optional.of(MarketPlayerData.load(NbtCompat.getCompoundOrEmpty(root, "market")));
        } catch (IOException exception) {
            TrippyCraft.LOGGER.warn("Failed to load Vice and Villagers market data from {}", file, exception);
            return Optional.empty();
        }
    }

    public static void save(File file, MarketPlayerData data) {
        try {
            if (file.toPath().getParent() != null) {
                Files.createDirectories(file.toPath().getParent());
            }
            CompoundTag root = new CompoundTag();
            root.putInt("schema", SCHEMA_VERSION);
            root.put("market", data.save());
            NbtIo.write(root, file.toPath());
        } catch (IOException exception) {
            TrippyCraft.LOGGER.warn("Failed to save Vice and Villagers market data to {}", file, exception);
        }
    }
}
