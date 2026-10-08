package com.trippycraft.substance.salvia;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class SalviaSession {
    private static final int ENVIRONMENT_UPDATE_INTERVAL = 16;
    private static final String EMPTY_DIMENSION = "";

    private double activeDoseUnits;
    private double targetDoseUnits;
    private double displayedDoseUnits;
    private int ageTicks;
    private int durationTicks;
    private int environmentTicks;
    private int lastAnnouncedTier;
    private double homeX;
    private double homeY;
    private double homeZ;
    private String homeDimensionKey = EMPTY_DIMENSION;
    private SalviaPhase lastObservedPhase = SalviaPhase.SOBER;
    private SalviaPhase lastTeleportedPhase = SalviaPhase.SOBER;
    private double memoryArtificial;
    private double memoryStill;
    private double memoryMotion;
    private double memorySocial;
    private double memoryThreat;
    private SalviaTripSignature signature;
    private PsilocybinEnvironmentSnapshot environment = PsilocybinEnvironmentSnapshot.empty();

    private SalviaSession(double doseUnits, int durationTicks, SalviaTripSignature signature) {
        this.activeDoseUnits = doseUnits;
        this.targetDoseUnits = doseUnits;
        this.displayedDoseUnits = doseUnits;
        this.durationTicks = durationTicks;
        this.signature = signature;
    }

    public static SalviaSession start(ServerPlayer player, double doseUnits) {
        long seed = player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 9)
                ^ player.level().getGameTime()
                ^ Double.doubleToLongBits(doseUnits)
                ^ 0x5A171A55EEDL;
        SalviaSession session = new SalviaSession(doseUnits, SalviaDoseCurve.baseDurationTicks(doseUnits), SalviaTripSignature.create(seed));
        session.environment = PsilocybinEnvironmentSnapshot.capture(player);
        session.homeX = player.getX();
        session.homeY = player.getY();
        session.homeZ = player.getZ();
        session.homeDimensionKey = player.level().dimension().location().toString();
        session.lastObservedPhase = session.phase();
        return session;
    }

    public void stackDose(double addedDoseUnits) {
        targetDoseUnits += Math.max(0.0D, addedDoseUnits);
        int totalDuration = SalviaDoseCurve.baseDurationTicks(targetDoseUnits);
        int absorptionTail = Math.max(60, (int) Math.round(SalviaDoseCurve.baseDurationTicks(addedDoseUnits) * 0.25D));
        durationTicks = Math.max(durationTicks, Math.max(totalDuration, ageTicks + absorptionTail));
    }

    public void tick(ServerPlayer player) {
        ensureHomeAnchor(player);
        ageTicks++;
        double doseDelta = Math.abs(targetDoseUnits - activeDoseUnits);
        double ramp = (targetDoseUnits > activeDoseUnits ? 0.0048D : 0.0018D)
                + SalviaDoseCurve.smoothstep(0.18D, 2.0D, doseDelta) * 0.0034D;
        activeDoseUnits += (targetDoseUnits - activeDoseUnits) * ramp;
        displayedDoseUnits += (targetDoseUnits - displayedDoseUnits) * 0.010D;
        if (Math.abs(targetDoseUnits - activeDoseUnits) < 0.002D) {
            activeDoseUnits = targetDoseUnits;
        }
        if (Math.abs(targetDoseUnits - displayedDoseUnits) < 0.002D) {
            displayedDoseUnits = targetDoseUnits;
        }

        environmentTicks--;
        if (environmentTicks <= 0) {
            PsilocybinEnvironmentSnapshot next = PsilocybinEnvironmentSnapshot.capture(player);
            environment = environment.blend(next, 0.26D + signature.sceneBias() * 0.08D + signature.observerBias() * 0.04D);
            environmentTicks = ENVIRONMENT_UPDATE_INTERVAL;
        }

        memoryArtificial = memory(memoryArtificial, environment.artificialBlocks() + environment.village() * 0.35D);
        memoryStill = memory(memoryStill, environment.stillness() + environment.depth() * 0.18D);
        memoryMotion = memory(memoryMotion, environment.moving() + environment.entities() * 0.18D);
        memorySocial = memory(memorySocial, environment.entities() + environment.village() * 0.24D);
        memoryThreat = memory(memoryThreat, environment.hostileEntities() + environment.lowHealth() * 0.22D + environment.night() * 0.14D);
        announceTierChanges(player);
    }

    public SalviaEffectState effectState() {
        SalviaPhase phase = phase();
        double progress = progress();
        double safeVisual = SalviaDoseCurve.safeVisualIntensity();
        double doseIntensity = SalviaDoseCurve.peakIntensity(activeDoseUnits);
        double phaseEnvelope = SalviaDoseCurve.phaseEnvelope(phase, progress);
        int tier = SalviaDoseCurve.tier(activeDoseUnits);
        double tierRamp = SalviaDoseCurve.clamp(tier / 5.0D, 0.0D, 1.0D);
        double unsettle = Math.sin(ageTicks * 0.014D + (signature.seed() & 2047L) * 0.003D) * 0.038D
                + Math.sin(ageTicks * (0.043D + signature.bodyTorque() * 0.018D) + signature.anchorBias() * 4.0D) * (0.016D + tierRamp * 0.030D)
                + Math.sin(ageTicks * (0.071D + signature.recursionBias() * 0.022D) + signature.observerBias() * 5.0D) * (0.010D + tierRamp * 0.020D);
        double living = SalviaDoseCurve.clamp(1.0D + unsettle, 0.86D, 1.18D);
        double intensity = Math.max(0.0D, doseIntensity * phaseEnvelope * living);
        double visual = Math.max(0.0D, SalviaDoseCurve.visualIntensity(activeDoseUnits) * phaseEnvelope * living);

        double tier1 = SalviaDoseCurve.smoothstep(0.25D, 0.45D, activeDoseUnits);
        double tier2 = SalviaDoseCurve.smoothstep(0.45D, 0.85D, activeDoseUnits);
        double tier3 = SalviaDoseCurve.smoothstep(0.85D, 1.35D, activeDoseUnits);
        double tier4 = SalviaDoseCurve.smoothstep(1.35D, 2.10D, activeDoseUnits);
        double tier5 = SalviaDoseCurve.smoothstep(2.10D, 3.40D, activeDoseUnits);
        double sceneEnv = SalviaDoseCurve.clamp(environment.artificialBlocks() * 0.42D + environment.village() * 0.30D + memoryArtificial * 0.24D + environment.cave() * 0.16D, 0.0D, 1.35D);
        double stillEnv = SalviaDoseCurve.clamp(environment.stillness() * 0.44D + memoryStill * 0.30D + environment.depth() * 0.18D, 0.0D, 1.35D);
        double motionEnv = SalviaDoseCurve.clamp(environment.moving() * 0.38D + memoryMotion * 0.22D + environment.entities() * 0.12D, 0.0D, 1.35D);
        double socialEnv = SalviaDoseCurve.clamp(environment.entities() * 0.28D + environment.village() * 0.24D + memorySocial * 0.24D, 0.0D, 1.35D);
        double threatEnv = SalviaDoseCurve.clamp(environment.hostileEntities() * 0.30D + environment.lowHealth() * 0.24D + memoryThreat * 0.26D + environment.night() * 0.14D, 0.0D, 1.35D);
        double recursionWindow = SalviaDoseCurve.smoothstep(0.48D, 0.62D, progress) * (1.0D - SalviaDoseCurve.smoothstep(0.92D, 0.99D, progress));
        double shakeoutWindow = SalviaDoseCurve.smoothstep(0.82D, 0.94D, progress);

        double anchorSlip = intensity * (tier2 * 0.18D + tier3 * 0.40D + tier4 * 0.58D + tier5 * 0.76D) * (0.36D + signature.anchorBias() * 0.52D + stillEnv * 0.18D + sceneEnv * 0.14D);
        double bodyPull = intensity * (tier1 * 0.22D + tier2 * 0.34D + tier3 * 0.44D + tier4 * 0.42D + tier5 * 0.38D) * (0.34D + signature.bodyTorque() * 0.54D + motionEnv * 0.26D);
        double sceneReplacement = intensity * (tier1 * 0.12D + tier2 * 0.28D + tier3 * 0.50D + tier4 * 0.62D + tier5 * 0.70D) * (0.34D + signature.sceneBias() * 0.54D + sceneEnv * 0.24D);
        double loopSeam = intensity * (tier2 * 0.18D + tier3 * 0.40D + tier4 * 0.54D + tier5 * 0.60D) * (0.32D + signature.recursionBias() * 0.56D + recursionWindow * 0.22D + stillEnv * 0.12D);
        double cutoutPresence = intensity * (tier2 * 0.12D + tier3 * 0.28D + tier4 * 0.44D + tier5 * 0.52D) * (0.30D + signature.cutoutBias() * 0.56D + socialEnv * 0.22D);
        double observerPressure = intensity * (tier1 * 0.08D + tier2 * 0.18D + tier3 * 0.34D + tier4 * 0.54D + tier5 * 0.72D) * (0.36D + signature.observerBias() * 0.56D + threatEnv * 0.28D);
        double reentryConfusion = intensity * shakeoutWindow * (0.24D + tier3 * 0.26D + tier4 * 0.38D + tier5 * 0.44D) * (0.40D + signature.aftershock() * 0.50D + motionEnv * 0.16D);
        double megaPresence = SalviaDoseCurve.megaPresence(activeDoseUnits);
        double highDoseDrive = SalviaDoseCurve.smoothstep(6.0D, 10.0D, activeDoseUnits);
        double realityBreak = SalviaDoseCurve.smoothstep(10.0D, 15.0D, activeDoseUnits);
        double beyondBreak = SalviaDoseCurve.smoothstep(15.0D, 20.0D, activeDoseUnits);
        double deepBreak = SalviaDoseCurve.smoothstep(20.0D, 28.0D, activeDoseUnits);
        double dislocationWindow = SalviaDoseCurve.smoothstep(0.22D, 0.36D, progress) * (1.0D - SalviaDoseCurve.smoothstep(0.80D, 0.92D, progress));
        double peakWindow = Math.max(dislocationWindow, recursionWindow);
        double megaWindow = megaPresence * SalviaDoseCurve.clamp(
            0.24D + phaseEnvelope * 0.56D + peakWindow * 0.76D + highDoseDrive * 0.34D + realityBreak * 0.42D + beyondBreak * 0.34D + deepBreak * 0.40D,
            0.0D,
            2.45D
        );
        double slipNorm = SalviaDoseCurve.clamp(anchorSlip / Math.max(0.75D, safeVisual), 0.0D, 1.35D);
        double pullNorm = SalviaDoseCurve.clamp(bodyPull / Math.max(0.75D, safeVisual), 0.0D, 1.35D);
        double sceneNorm = SalviaDoseCurve.clamp(sceneReplacement / Math.max(0.75D, safeVisual), 0.0D, 1.35D);
        double loopNorm = SalviaDoseCurve.clamp(loopSeam / Math.max(0.75D, safeVisual), 0.0D, 1.35D);
        double cutoutNorm = SalviaDoseCurve.clamp(cutoutPresence / Math.max(0.75D, safeVisual), 0.0D, 1.35D);
        double observerNorm = SalviaDoseCurve.clamp(observerPressure / Math.max(0.75D, safeVisual), 0.0D, 1.35D);
        double pullWindow = SalviaDoseCurve.smoothstep(0.14D, 0.30D, progress) * (1.0D - SalviaDoseCurve.smoothstep(0.56D, 0.74D, progress));
        double wheelConveyor = SalviaDoseCurve.clamp(
            megaWindow * (0.34D + pullNorm * 0.48D + slipNorm * 0.28D + sceneNorm * 0.18D + highDoseDrive * 0.18D + beyondBreak * 0.20D + deepBreak * 0.28D)
                * (0.22D + pullWindow * 0.34D + peakWindow * 0.54D + realityBreak * 0.24D + beyondBreak * 0.18D + deepBreak * 0.24D),
            0.0D,
            3.60D
        );
        double zipperFold = SalviaDoseCurve.clamp(
            megaWindow * (0.28D + sceneNorm * 0.40D + loopNorm * 0.32D + slipNorm * 0.22D + highDoseDrive * 0.16D + beyondBreak * 0.18D + deepBreak * 0.26D)
                * (0.22D + dislocationWindow * 0.34D + recursionWindow * 0.50D + realityBreak * 0.18D + beyondBreak * 0.16D + deepBreak * 0.22D),
            0.0D,
            3.50D
        );
        double flattening = SalviaDoseCurve.clamp(
            megaWindow * (0.20D + stillEnv * 0.38D + sceneNorm * 0.34D + slipNorm * 0.24D + highDoseDrive * 0.20D + beyondBreak * 0.20D + deepBreak * 0.26D)
                * (0.18D + dislocationWindow * 0.24D + recursionWindow * 0.62D + realityBreak * 0.22D + beyondBreak * 0.18D + deepBreak * 0.26D),
            0.0D,
            3.40D
        );
        double objecthood = SalviaDoseCurve.clamp(
            megaWindow * (0.18D + sceneEnv * 0.38D + stillEnv * 0.28D + cutoutNorm * 0.20D + pullNorm * 0.20D + highDoseDrive * 0.14D + beyondBreak * 0.18D + deepBreak * 0.24D)
                * (0.16D + dislocationWindow * 0.30D + recursionWindow * 0.40D + realityBreak * 0.20D + beyondBreak * 0.14D + deepBreak * 0.22D),
            0.0D,
            3.10D
        );
        double entityContact = SalviaDoseCurve.clamp(
            megaWindow * (0.18D + observerNorm * 0.42D + socialEnv * 0.30D + threatEnv * 0.24D + cutoutNorm * 0.16D + realityBreak * 0.14D + beyondBreak * 0.16D + deepBreak * 0.24D)
                * (0.18D + peakWindow * 0.20D + recursionWindow * 0.42D + shakeoutWindow * 0.14D + realityBreak * 0.22D + beyondBreak * 0.12D + deepBreak * 0.18D),
            0.0D,
            3.20D
        );
        double realityStack = SalviaDoseCurve.clamp(
            megaWindow * (0.32D + loopNorm * 0.44D + sceneNorm * 0.36D + slipNorm * 0.20D + observerNorm * 0.16D + highDoseDrive * 0.22D + realityBreak * 0.28D + beyondBreak * 0.20D + deepBreak * 0.32D)
                * (0.22D + dislocationWindow * 0.28D + recursionWindow * 0.58D + realityBreak * 0.30D + beyondBreak * 0.18D + deepBreak * 0.28D),
            0.0D,
            4.10D
        );
        double kaleidoscope = SalviaDoseCurve.clamp(
            megaWindow * (0.24D + sceneNorm * 0.36D + cutoutNorm * 0.28D + loopNorm * 0.24D + observerNorm * 0.16D + highDoseDrive * 0.18D + realityBreak * 0.22D + beyondBreak * 0.18D + deepBreak * 0.28D)
                * (0.20D + dislocationWindow * 0.22D + recursionWindow * 0.44D + peakWindow * 0.22D + realityBreak * 0.26D + beyondBreak * 0.16D + deepBreak * 0.24D),
            0.0D,
            3.80D
        );

        return new SalviaEffectState(
                true,
                displayedDoseUnits,
                activeDoseUnits,
                targetDoseUnits,
                ageTicks,
                durationTicks,
                remainingTicks(),
                phase,
                progress,
                tier,
                intensity,
                visual,
                anchorSlip,
                bodyPull,
                sceneReplacement,
                loopSeam,
                cutoutPresence,
                observerPressure,
                reentryConfusion,
                megaPresence,
                wheelConveyor,
                zipperFold,
                flattening,
                objecthood,
                entityContact,
                realityStack,
                kaleidoscope,
                signature,
                environment
        );
    }

    public boolean isAlive() {
        return ageTicks < durationTicks;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public double targetDoseUnits() {
        return targetDoseUnits;
    }

    public double homeX() {
        return homeX;
    }

    public double homeY() {
        return homeY;
    }

    public double homeZ() {
        return homeZ;
    }

    public String homeDimensionKey() {
        return homeDimensionKey;
    }

    public SalviaPhase phase() {
        return SalviaDoseCurve.phase(ageTicks, durationTicks);
    }

    public double progress() {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return SalviaDoseCurve.clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    public SalviaPhase consumePhaseTeleport(SalviaEffectState state) {
        if (!state.active()) {
            return null;
        }

        SalviaPhase phase = state.phase();
        boolean phaseChanged = phase != lastObservedPhase;
        lastObservedPhase = phase;
        if (!phaseChanged) {
            return null;
        }
        if (phase == SalviaPhase.SOBER || phase == SalviaPhase.LOAD_IN) {
            return null;
        }
        if (phase == lastTeleportedPhase) {
            return null;
        }

        lastTeleportedPhase = phase;
        return phase;
    }

    public String debugSummary() {
        SalviaEffectState state = effectState();
        return String.format(Locale.ROOT,
            "salvia dose=%s phase=%s intensity=%.2f remaining=%ds signature=%s",
            SalviaDoseCurve.formatPotencyLabel(state.displayDoseUnits()),
                state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(),
                state.remainingTicks() / 20,
                signature.debugName());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseUnits", activeDoseUnits);
        tag.putDouble("targetDoseUnits", targetDoseUnits);
        tag.putDouble("displayedDoseUnits", displayedDoseUnits);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        tag.putInt("environmentTicks", environmentTicks);
        tag.putInt("lastAnnouncedTier", lastAnnouncedTier);
        tag.putDouble("homeX", homeX);
        tag.putDouble("homeY", homeY);
        tag.putDouble("homeZ", homeZ);
        tag.putString("homeDimensionKey", homeDimensionKey);
        tag.putInt("lastObservedPhase", lastObservedPhase.ordinal());
        tag.putInt("lastTeleportedPhase", lastTeleportedPhase.ordinal());
        tag.putDouble("memoryArtificial", memoryArtificial);
        tag.putDouble("memoryStill", memoryStill);
        tag.putDouble("memoryMotion", memoryMotion);
        tag.putDouble("memorySocial", memorySocial);
        tag.putDouble("memoryThreat", memoryThreat);
        tag.put("signature", signature.save());
        tag.put("environment", environment.save());
        return tag;
    }

    public static SalviaSession load(CompoundTag tag) {
        SalviaSession session = new SalviaSession(
                NbtCompat.getDoubleOr(tag, "activeDoseUnits", 0.0D),
                NbtCompat.getIntOr(tag, "durationTicks", 0),
                SalviaTripSignature.load(NbtCompat.getCompoundOrEmpty(tag, "signature"))
        );
        session.targetDoseUnits = NbtCompat.getDoubleOr(tag, "targetDoseUnits", session.activeDoseUnits);
        session.displayedDoseUnits = NbtCompat.getDoubleOr(tag, "displayedDoseUnits", session.activeDoseUnits);
        session.ageTicks = NbtCompat.getIntOr(tag, "ageTicks", 0);
        session.environmentTicks = NbtCompat.getIntOr(tag, "environmentTicks", 0);
        session.lastAnnouncedTier = NbtCompat.getIntOr(tag, "lastAnnouncedTier", SalviaDoseCurve.tier(session.activeDoseUnits));
        session.homeX = NbtCompat.getDoubleOr(tag, "homeX", 0.0D);
        session.homeY = NbtCompat.getDoubleOr(tag, "homeY", 0.0D);
        session.homeZ = NbtCompat.getDoubleOr(tag, "homeZ", 0.0D);
        session.homeDimensionKey = NbtCompat.getStringOr(tag, "homeDimensionKey", EMPTY_DIMENSION);
        SalviaPhase currentPhase = session.phase();
        session.lastObservedPhase = phaseFromOrdinal(NbtCompat.getIntOr(tag, "lastObservedPhase", currentPhase.ordinal()), currentPhase);
        session.lastTeleportedPhase = phaseFromOrdinal(NbtCompat.getIntOr(tag, "lastTeleportedPhase", currentPhase.ordinal()), currentPhase);
        session.memoryArtificial = NbtCompat.getDoubleOr(tag, "memoryArtificial", 0.0D);
        session.memoryStill = NbtCompat.getDoubleOr(tag, "memoryStill", 0.0D);
        session.memoryMotion = NbtCompat.getDoubleOr(tag, "memoryMotion", 0.0D);
        session.memorySocial = NbtCompat.getDoubleOr(tag, "memorySocial", 0.0D);
        session.memoryThreat = NbtCompat.getDoubleOr(tag, "memoryThreat", 0.0D);
        session.environment = PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "environment"));
        return session;
    }

    private static double memory(double current, double observed) {
        return SalviaDoseCurve.clamp(current + (observed - current) * 0.0048D, 0.0D, 1.0D);
    }

    private void ensureHomeAnchor(ServerPlayer player) {
        if (!homeDimensionKey.isEmpty()) {
            return;
        }
        homeX = player.getX();
        homeY = player.getY();
        homeZ = player.getZ();
        homeDimensionKey = player.level().dimension().location().toString();
        lastObservedPhase = phase();
    }

    private static SalviaPhase phaseFromOrdinal(int ordinal, SalviaPhase fallback) {
        if (ordinal < 0 || ordinal >= SalviaPhase.values().length) {
            return fallback;
        }
        return SalviaPhase.values()[ordinal];
    }

    private void announceTierChanges(ServerPlayer player) {
        int tier = SalviaDoseCurve.tier(activeDoseUnits);
        if (tier <= lastAnnouncedTier || tier <= 0 || ageTicks < 6) {
            return;
        }
        lastAnnouncedTier = tier;
        String key = switch (tier) {
            case 1 -> "message.trippycraft.salvia_tier_1";
            case 2 -> "message.trippycraft.salvia_tier_2";
            case 3 -> "message.trippycraft.salvia_tier_3";
            case 4 -> "message.trippycraft.salvia_tier_4";
            default -> "message.trippycraft.salvia_tier_5";
        };
        player.displayClientMessage(Component.translatable(key), true);
    }
}