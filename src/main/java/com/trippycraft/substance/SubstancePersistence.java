package com.trippycraft.substance;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.TrippyCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public final class SubstancePersistence {
    private static final int SCHEMA_VERSION = 1;

    private SubstancePersistence() {
    }

    public static Optional<PsychoactiveExperience> load(File file) {
        if (!file.isFile()) {
            return Optional.empty();
        }

        try {
            CompoundTag root = NbtIo.read(file.toPath());
            if (root == null || NbtCompat.getIntOr(root, "schema", 0) != SCHEMA_VERSION) {
                return Optional.empty();
            }
            PsychoactiveExperience experience = PsychoactiveExperience.load(NbtCompat.getCompoundOrEmpty(root, "experience"));
            return experience.isAlive() ? Optional.of(experience) : Optional.empty();
        } catch (IOException exception) {
            TrippyCraft.LOGGER.warn("Failed to load Vice and Villagers player experience from {}", file, exception);
            return Optional.empty();
        }
    }

    public static void save(File file, Optional<PsychoactiveExperience> maybeExperience) {
        try {
            if (maybeExperience.isEmpty() || !maybeExperience.get().isAlive()) {
                Files.deleteIfExists(file.toPath());
                return;
            }

            CompoundTag root = new CompoundTag();
            root.putInt("schema", SCHEMA_VERSION);
            root.put("experience", maybeExperience.get().save());
            NbtIo.write(root, file.toPath());
        } catch (IOException exception) {
            TrippyCraft.LOGGER.warn("Failed to save Vice and Villagers player experience to {}", file, exception);
        }
    }
}
