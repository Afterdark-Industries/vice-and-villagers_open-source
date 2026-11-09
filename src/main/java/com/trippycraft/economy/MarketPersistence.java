package com.trippycraft.economy;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.TrippyCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class MarketPersistence {
    private static final int SCHEMA_VERSION = 1;
    static final String UNREADABLE_SUFFIX = ".unreadable-";
    private static final String TEMP_SUFFIX = ".tmp";

    private MarketPersistence() {
    }

    /**
     * Loads saved market progress. A file that exists but cannot be used (corrupt, or written by a newer
     * schema) is moved aside rather than left in place, because the next save would otherwise replace it
     * with a fresh profile and the player's progress would be lost for good.
     */
    public static Optional<MarketPlayerData> load(File file) {
        if (!file.isFile()) {
            return Optional.empty();
        }

        try {
            CompoundTag root = NbtIo.read(file.toPath());
            int schema = root == null ? -1 : NbtCompat.getIntOr(root, "schema", 0);
            if (schema == SCHEMA_VERSION) {
                return Optional.of(MarketPlayerData.load(NbtCompat.getCompoundOrEmpty(root, "market")));
            }
            preserveUnreadable(file, "unsupported schema " + schema, null);
        } catch (IOException | RuntimeException exception) {
            preserveUnreadable(file, "unreadable data", exception);
        }
        return Optional.empty();
    }

    /** Writes to a temporary file first so an interrupted save cannot leave a truncated market file. */
    public static void save(File file, MarketPlayerData data) {
        Path target = file.toPath();
        Path temporary = target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            CompoundTag root = new CompoundTag();
            root.putInt("schema", SCHEMA_VERSION);
            root.put("market", data.save());
            NbtIo.write(root, temporary);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            TrippyCraft.LOGGER.warn("Failed to save Vice and Villagers market data to {}", file, exception);
        }
    }

    private static void preserveUnreadable(File file, String reason, Exception cause) {
        Path source = file.toPath();
        Path backup = source.resolveSibling(source.getFileName() + UNREADABLE_SUFFIX + System.currentTimeMillis());
        try {
            Files.move(source, backup);
            TrippyCraft.LOGGER.error("Vice and Villagers market data in {} has {}; moved it to {} and started a new market profile",
                    file, reason, backup, cause);
        } catch (IOException moveFailure) {
            TrippyCraft.LOGGER.error("Vice and Villagers market data in {} has {} and could not be moved aside; "
                    + "it will be replaced on the next save", file, reason, moveFailure);
        }
    }
}
