package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.SetSettingSnapshot;
import com.trippycraft.substance.SubstanceEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared fixtures for the substance-session GameTests in the {@code trippycraft_sessions} namespace.
 *
 * <p>Sessions are advanced by rewriting their saved {@code ageTicks}, the same technique the LSD and
 * psilocybin contracts use, so a full trip can be checked in milliseconds. Every helper restores the
 * static gameplay config it touches.
 */
final class SessionTestSupport {
    static final String NAMESPACE = "trippycraft_sessions";
    static final String TEMPLATE = "empty";

    private SessionTestSupport() { }

    /** An invulnerable player, matching the existing GameTests. Damage and death calls are ignored. */
    static FakePlayer fakePlayer(GameTestHelper helper, String name) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        place(helper, player);
        return player;
    }

    /** A player that can be hurt and records how it died, for withdrawal, hangover and overdose contracts. */
    static TestPlayer damageablePlayer(GameTestHelper helper, String name) {
        TestPlayer player = new TestPlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        place(helper, player);
        return player;
    }

    private static void place(GameTestHelper helper, ServerPlayer player) {
        BlockPos location = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(location.getX() + 0.5D, location.getY(), location.getZ() + 0.5D);
    }

    /** Returns a copy of a saved session with its age replaced. */
    static CompoundTag aged(CompoundTag session, int ageTicks) {
        CompoundTag copy = session.copy();
        copy.putInt("ageTicks", ageTicks);
        return copy;
    }

    static int ageAt(int durationTicks, double progress) {
        return (int) Math.floor(durationTicks * progress);
    }

    /** Builds an experience holding one saved session under its experience key, e.g. {@code "alcohol"}. */
    static PsychoactiveExperience experienceWith(String key, CompoundTag session) {
        CompoundTag saved = new CompoundTag();
        saved.put(key, session);
        return PsychoactiveExperience.load(saved);
    }

    static void tick(ServerPlayer player, PsychoactiveExperience experience, int ticks) {
        SetSettingSnapshot setting = SetSettingSnapshot.capture(player);
        for (int i = 0; i < ticks; i++) experience.tick(player, setting);
    }

    /** Runs the assertions with default gameplay config, restores it, then passes the test. */
    static void run(GameTestHelper helper, Runnable assertions) {
        ConfigSnapshot snapshot = ConfigSnapshot.captureAndReset();
        List<UUID> engineEntries = new ArrayList<>();
        try {
            ENGINE_ENTRIES.set(engineEntries);
            assertions.run();
            helper.succeed();
        } finally {
            ENGINE_ENTRIES.remove();
            engineEntries.forEach(SubstanceEngine::clear);
            snapshot.restore();
        }
    }

    /**
     * Waits for the next 40-tick boundary, when {@code VanillaEffectBridge} applies status effects, then
     * behaves like {@link #run}.
     */
    static void runAtBridgeTick(GameTestHelper helper, Runnable assertions) {
        boolean[] done = {false};
        helper.onEachTick(() -> {
            if (done[0] || helper.getLevel().getGameTime() % 40L != 0L) return;
            done[0] = true;
            run(helper, assertions);
        });
    }

    private static final ThreadLocal<List<UUID>> ENGINE_ENTRIES = new ThreadLocal<>();

    /** Registers a player whose global SubstanceEngine state must be cleared when the test ends. */
    static <T extends ServerPlayer> T tracked(T player) {
        List<UUID> entries = ENGINE_ENTRIES.get();
        if (entries != null) entries.add(player.getUUID());
        return player;
    }

    static PsychoactiveExperience engineExperience(ServerPlayer player) {
        return SubstanceEngine.getExperience(player.getUUID()).orElseThrow(
                () -> new AssertionError("SubstanceEngine has no experience for " + player.getGameProfile().getName()));
    }

    static void assertClose(GameTestHelper helper, double expected, double actual, double tolerance, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= tolerance,
                message + " (expected " + expected + ", was " + actual + ")");
    }

    /** A FakePlayer that takes damage and records its death instead of running vanilla death handling. */
    static final class TestPlayer extends FakePlayer {
        DamageSource lastDamage;
        DamageSource deathSource;
        float damageTaken;
        int hits;

        TestPlayer(ServerLevel level, GameProfile profile) {
            super(level, profile);
            // New players are briefly immune; that timer only counts down in tick(), which FakePlayer skips.
            ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, this, 0, "spawnInvulnerableTime");
        }

        @Override
        public boolean isInvulnerableTo(DamageSource source) {
            return false;
        }

        @Override
        public boolean hurt(DamageSource source, float amount) {
            // FakePlayer skips tick(), so hurt cooldowns would otherwise never expire between calls.
            invulnerableTime = 0;
            float before = getHealth();
            boolean hurt = super.hurt(source, amount);
            if (hurt) {
                lastDamage = source;
                damageTaken += Math.max(0.0F, before - getHealth());
                hits++;
            }
            return hurt;
        }

        @Override
        public void die(DamageSource source) {
            deathSource = source;
        }

        void restoreHealth() {
            setHealth(getMaxHealth());
        }

        boolean diedFrom(String damageTypePath) {
            return deathSource != null && deathSource.typeHolder().unwrapKey()
                    .map(key -> key.location().getPath().equals(damageTypePath)).orElse(false);
        }

        boolean lastDamageWas(String damageTypePath) {
            return lastDamage != null && lastDamage.typeHolder().unwrapKey()
                    .map(key -> key.location().getPath().equals(damageTypePath)).orElse(false);
        }
    }

    private record ConfigSnapshot(boolean vanillaBridge, double timeScale, double globalIntensity,
                                  boolean riskMechanics, double riskSeverity, double recoveryRate,
                                  boolean alcoholEffects) {
        static ConfigSnapshot captureAndReset() {
            ConfigSnapshot snapshot = new ConfigSnapshot(TrippyConfig.enableVanillaBridge, TrippyConfig.experienceTimeScale,
                    TrippyConfig.globalIntensity, TrippyConfig.enableRiskMechanics, TrippyConfig.riskSeverity,
                    TrippyConfig.recoveryRate, TrippyConfig.enableAlcoholEffects);
            TrippyConfig.enableVanillaBridge = true;
            TrippyConfig.experienceTimeScale = 1.0D;
            TrippyConfig.globalIntensity = 1.0D;
            TrippyConfig.enableRiskMechanics = true;
            TrippyConfig.riskSeverity = 1.0D;
            TrippyConfig.recoveryRate = 1.0D;
            TrippyConfig.enableAlcoholEffects = true;
            return snapshot;
        }

        void restore() {
            TrippyConfig.enableVanillaBridge = vanillaBridge;
            TrippyConfig.experienceTimeScale = timeScale;
            TrippyConfig.globalIntensity = globalIntensity;
            TrippyConfig.enableRiskMechanics = riskMechanics;
            TrippyConfig.riskSeverity = riskSeverity;
            TrippyConfig.recoveryRate = recoveryRate;
            TrippyConfig.enableAlcoholEffects = alcoholEffects;
        }
    }
}
