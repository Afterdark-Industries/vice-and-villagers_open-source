package com.trippycraft.guide;

import com.trippycraft.TrippyCraft;
import com.trippycraft.compat.NbtCompat;
import com.trippycraft.network.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashSet;
import java.util.Set;

public final class CursedRecipeCodex {
    private static final String CODEX_DATA_KEY = "cursed_findings";
    private static final String LEGACY_CHAPTER_PSILOCYBIN_KEY = "chapter_psilocybin";
    private static final String LEGACY_CHAPTER_THC_KEY = "chapter_thc";
    private static final String LEGACY_CHAPTER_COCAINE_KEY = "chapter_cocaine";
    private static final String LEGACY_CHAPTER_OPIUM_KEY = "chapter_opium";
    private static final Set<String> ALWAYS_UNLOCKED_FINDINGS = Set.of(
            CursedFindingIds.DEPENDENCE_TOLERANCE,
            CursedFindingIds.ECONOMY_DEALING
    );

    private CursedRecipeCodex() {
    }

    public static ItemStack createStack(Item item) {
        return createStack(item, null);
    }

    public static ItemStack createStack(Item item, Player player) {
        return new ItemStack(item);
    }

    public static void refreshStack(ItemStack stack, Player player) {
        // The journal is now rendered from persistent player data instead of per-item written-book pages.
    }

    public static void refreshPlayerCodices(ServerPlayer player) {
        syncFindings(player, false);
    }

    public static void openJournal(ServerPlayer player) {
        syncFindings(player, true);
    }

    public static boolean unlockFinding(ServerPlayer player, String findingId) {
        if (!CursedFindingsRegistry.contains(findingId)) {
            TrippyCraft.LOGGER.warn("Ignoring unknown Cursed Findings unlock id '{}'", findingId);
            return false;
        }

        CompoundTag codexData = getOrCreateCodexData(player);
        migrateLegacyChapterKeys(codexData);
        if (NbtCompat.getBooleanOr(codexData, findingId, false)) {
            return false;
        }

        codexData.putBoolean(findingId, true);
        syncFindings(player, false);
        return true;
    }

    public static Set<String> unlockedFindings(Player player) {
        if (player == null) {
            return Set.of();
        }

        CompoundTag codexData = getOrCreateCodexData(player);
        migrateLegacyChapterKeys(codexData);

        Set<String> unlocked = new LinkedHashSet<>();
        for (CursedFindingEntry entry : CursedFindingsRegistry.entries()) {
            if (ALWAYS_UNLOCKED_FINDINGS.contains(entry.id()) || NbtCompat.getBooleanOr(codexData, entry.id(), false)) {
                unlocked.add(entry.id());
            }
        }
        return Set.copyOf(unlocked);
    }

    public static boolean isFindingUnlocked(Player player, String findingId) {
        return player != null && unlockedFindings(player).contains(findingId);
    }

    public static void unlockPsilocybin(ServerPlayer player) {
        unlockFinding(player, CursedFindingIds.PSILOCYBIN);
    }

    public static void unlockThc(ServerPlayer player) {
        unlockFinding(player, CursedFindingIds.CANNABIS);
    }

    public static void unlockCocaine(ServerPlayer player) {
        unlockFinding(player, CursedFindingIds.COCAINE);
    }

    public static void unlockOpium(ServerPlayer player) {
        unlockFinding(player, CursedFindingIds.HEROIN);
    }

    private static void syncFindings(ServerPlayer player, boolean openJournal) {
        if (player.connection != null) {
            ModNetwork.sendCursedFindings(player, unlockedFindings(player), openJournal);
        }
    }

    private static void migrateLegacyChapterKeys(CompoundTag codexData) {
        migrateLegacyChapter(codexData, LEGACY_CHAPTER_PSILOCYBIN_KEY, CursedFindingIds.PSILOCYBIN);
        migrateLegacyChapter(codexData, LEGACY_CHAPTER_THC_KEY, CursedFindingIds.CANNABIS);
        migrateLegacyChapter(codexData, LEGACY_CHAPTER_COCAINE_KEY, CursedFindingIds.COCAINE);
        migrateLegacyChapter(codexData, LEGACY_CHAPTER_OPIUM_KEY, CursedFindingIds.HEROIN);
    }

    private static void migrateLegacyChapter(CompoundTag codexData, String legacyKey, String findingId) {
        if (NbtCompat.getBooleanOr(codexData, legacyKey, false) && !NbtCompat.getBooleanOr(codexData, findingId, false)) {
            codexData.putBoolean(findingId, true);
        }
    }

    private static CompoundTag getOrCreateCodexData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag trippycraftData = NbtCompat.getOrCreateCompound(persistentData, TrippyCraft.MOD_ID);
        return NbtCompat.getOrCreateCompound(trippycraftData, CODEX_DATA_KEY);
    }
}
