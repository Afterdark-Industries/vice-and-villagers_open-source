package com.trippycraft.economy;

import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MarketDataStore {
    public static final String PLAYER_DATA_SUFFIX = "trippycraft_market";

    private static final Map<UUID, MarketPlayerData> PLAYER_DATA = new ConcurrentHashMap<>();

    private MarketDataStore() {
    }

    public static MarketPlayerData getOrCreate(ServerPlayer player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), ignored -> new MarketPlayerData());
    }

    public static Optional<MarketPlayerData> get(UUID playerId) {
        return Optional.ofNullable(PLAYER_DATA.get(playerId));
    }

    public static void set(UUID playerId, MarketPlayerData data) {
        PLAYER_DATA.put(playerId, data);
    }

    public static void load(UUID playerId, File file) {
        MarketPersistence.load(file).ifPresentOrElse(
                data -> set(playerId, data),
                () -> set(playerId, new MarketPlayerData())
        );
    }

    public static void save(UUID playerId, File file) {
        MarketPersistence.save(file, PLAYER_DATA.getOrDefault(playerId, new MarketPlayerData()));
    }

    public static void clone(UUID originalPlayerId, UUID newPlayerId) {
        MarketPlayerData original = PLAYER_DATA.get(originalPlayerId);
        if (original != null) {
            PLAYER_DATA.put(newPlayerId, original.copy());
        }
    }

    public static void clearPlayer(UUID playerId) {
        PLAYER_DATA.remove(playerId);
    }

    public static void clearAll() {
        PLAYER_DATA.clear();
    }
}
