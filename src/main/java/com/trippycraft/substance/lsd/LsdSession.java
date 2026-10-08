package com.trippycraft.substance.lsd;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class LsdSession {
    private static final int ENVIRONMENT_UPDATE_INTERVAL = 24;

    private double activeDoseMicrograms;
    private double targetDoseMicrograms;
    private double displayedDoseMicrograms;
    private double visualEnvelope;
    private int ageTicks;
    private int durationTicks;
    private int environmentTicks;
    private int lastAnnouncedTier;
    private double memoryWater;
    private double memoryFire;
    private double memorySky;
    private double memoryArtificial;
    private LsdTripSignature signature;
    private PsilocybinEnvironmentSnapshot environment = PsilocybinEnvironmentSnapshot.empty();
    private PsilocybinEnvironmentSnapshot targetEnvironment = PsilocybinEnvironmentSnapshot.empty();

    private LsdSession(double doseMicrograms, int durationTicks, LsdTripSignature signature) {
        this.activeDoseMicrograms = doseMicrograms;
        this.targetDoseMicrograms = doseMicrograms;
        this.displayedDoseMicrograms = doseMicrograms;
        this.durationTicks = durationTicks;
        this.signature = signature;
    }

    public static LsdSession start(ServerPlayer player, double doseMicrograms) {
        double dose = LsdDoseCurve.sanitizeDose(doseMicrograms);
        long seed = (player == null ? 0L : player.getUUID().getLeastSignificantBits() ^ player.level().getGameTime())
                ^ Double.doubleToLongBits(dose) ^ 0x15D15D15D15D15DL;
        LsdSession session = new LsdSession(dose, LsdDoseCurve.baseDurationTicks(dose), LsdTripSignature.create(seed));
        if (player != null) session.environment = PsilocybinEnvironmentSnapshot.capture(player);
        session.targetEnvironment = session.environment;
        return session;
    }

    public void stackDose(double addedDoseMicrograms) {
        double added = LsdDoseCurve.sanitizeDose(addedDoseMicrograms);
        if (added <= 0.0D) return;
        targetDoseMicrograms = LsdDoseCurve.safeAdd(targetDoseMicrograms, added);
        int extension = (int) Math.round(LsdDoseCurve.baseDurationTicks(added) * 0.48D);
        long extended = Math.max((long) durationTicks, (long) ageTicks + extension)
                + Math.round(extension * LsdDoseCurve.clamp(targetDoseMicrograms / 500.0D, 0.0D, 0.36D));
        durationTicks = (int) Math.min(Integer.MAX_VALUE, extended);
        // Preserve the absorbed amount, signature, and current envelope on ingestion.
        // Both the added amount and extended timeline ease in on subsequent ticks.
    }

    public void tick(ServerPlayer player) {
        if (!isAlive()) return;
        ageTicks++;
        double doseDelta = Math.abs(targetDoseMicrograms - activeDoseMicrograms);
        double ramp = (targetDoseMicrograms > activeDoseMicrograms ? 0.0032D : 0.0012D)
                + LsdDoseCurve.smoothstep(25.0D, 250.0D, doseDelta) * 0.0026D;
        activeDoseMicrograms += (targetDoseMicrograms - activeDoseMicrograms) * ramp;
        displayedDoseMicrograms += (targetDoseMicrograms - displayedDoseMicrograms) * 0.012D;
        double desiredEnvelope = LsdDoseCurve.phaseEnvelope(phase(), progress());
        double envelopeRate = Math.max(0.012D, 1.0D / Math.max(1, remainingTicks()));
        visualEnvelope += (desiredEnvelope - visualEnvelope) * envelopeRate;

        environmentTicks--;
        if (player != null && environmentTicks <= 0) {
            targetEnvironment = PsilocybinEnvironmentSnapshot.capture(player);
            environmentTicks = ENVIRONMENT_UPDATE_INTERVAL;
        }
        // Environment observations arrive discretely; geometry and palette influence
        // interpolate every tick instead of changing once per observation.
        environment = environment.blend(targetEnvironment, 0.010D + signature.liquidBias() * 0.004D);

        memoryWater = memory(memoryWater, environment.water() + environment.rain() * 0.35D);
        memoryFire = memory(memoryFire, environment.nether() + environment.light() * 0.18D);
        memorySky = memory(memorySky, environment.end() + environment.night() * 0.22D + environment.sunriseSunset() * 0.30D);
        memoryArtificial = memory(memoryArtificial, environment.artificialBlocks() + environment.village() * 0.45D);
        if (player != null) announceTierChanges(player);
    }

    public LsdEffectState effectState() {
        if (!isAlive()) return LsdEffectState.inactive();
        LsdPhase phase = phase();
        double progress = progress();
        double tier1 = LsdDoseCurve.smoothstep(35.0D, 60.0D, activeDoseMicrograms);
        double tier2 = LsdDoseCurve.smoothstep(80.0D, 120.0D, activeDoseMicrograms);
        double tier3 = LsdDoseCurve.smoothstep(135.0D, 210.0D, activeDoseMicrograms);
        double tier4 = LsdDoseCurve.smoothstep(225.0D, 340.0D, activeDoseMicrograms);
        double tier5 = LsdDoseCurve.smoothstep(800.0D, 1000.0D, activeDoseMicrograms);
        // Tier labels must not switch continuous colors or motion when a redose
        // crosses a threshold. These smooth gates introduce visual characteristics.
        double tierRamp = (tier1 + tier2 + tier3 + tier4 + tier5) * 0.20D;
        double visualGain = TrippyConfig.lsdMaxVisualIntensity <= 0.0D ? 0.0D
                : LsdDoseCurve.clamp(TrippyConfig.lsdVisualGain, 0.0D, 2.0D);
        double doseIntensity = LsdDoseCurve.peakIntensity(activeDoseMicrograms) * visualGain;
        double wave = Math.sin(ageTicks * 0.0034D + (signature.seed() & 2047L) * 0.004D) * 0.028D
                + Math.sin(ageTicks * 0.0016D + signature.geometricPrecision() * 4.0D) * (0.012D + tierRamp * 0.014D);
        double living = 1.0D + wave;
        double intensity = doseIntensity * visualEnvelope * living;
        double visual = LsdDoseCurve.visualIntensity(activeDoseMicrograms) * visualGain * visualEnvelope * living;
        int tier = LsdDoseCurve.tier(activeDoseMicrograms);
        double envLiquid = LsdDoseCurve.clamp(environment.water() * 0.62D + environment.rain() * 0.28D + environment.nether() * 0.36D + memoryWater * 0.20D, 0.0D, 1.35D);
        double envSky = LsdDoseCurve.clamp(environment.end() * 0.65D + environment.night() * 0.36D + environment.sunriseSunset() * 0.24D + memorySky * 0.20D, 0.0D, 1.35D);
        double envMelt = LsdDoseCurve.clamp(environment.nether() * 0.42D + environment.light() * 0.16D + memoryFire * 0.22D, 0.0D, 1.30D);
        double envBlocks = LsdDoseCurve.clamp(environment.forest() * 0.24D + environment.cave() * 0.28D + environment.village() * 0.24D + memoryArtificial * 0.16D, 0.0D, 1.35D);

        double edgeHum = intensity * tier1 * (0.56D + signature.electricSharpness() * 0.78D);
        double geometryMelt = intensity * (tier2 * 0.38D + tier3 * 0.35D + tier4 * 0.35D) * (0.40D + signature.meltFrequency() * 0.68D + envMelt * 0.18D);
        double realityFold = intensity * (tier3 * 0.32D + tier4 * 0.48D + tier5 * 0.46D) * (0.44D + signature.geometricPrecision() * 0.52D + signature.skyVortexBias() * 0.26D);
        double blockAutonomy = intensity * (tier2 * 0.20D + tier3 * 0.42D + tier4 * 0.48D + tier5 * 0.40D) * (0.42D + signature.blockAutonomyBias() * 0.68D + envBlocks * 0.16D);
        double liquidFlow = intensity * (tier2 * 0.26D + tier3 * 0.42D + tier4 * 0.38D + tier5 * 0.28D) * (0.42D + signature.liquidBias() * 0.70D + envLiquid * 0.30D);
        double skyVortex = intensity * (tier1 * 0.10D + tier2 * 0.24D + tier3 * 0.38D + tier4 * 0.56D + tier5 * 0.54D) * (0.38D + signature.skyVortexBias() * 0.72D + envSky * 0.30D);
        double entityTrails = intensity * (tier1 * 0.18D + tier2 * 0.30D + tier3 * 0.42D + tier4 * 0.46D + tier5 * 0.40D) * (0.44D + signature.entityEchoStrength() * 0.70D + environment.entities() * 0.18D);
        double prismSplit = intensity * tier1 * (0.40D + tierRamp * 0.74D) * (0.50D + signature.prismSplit() * 0.72D);
        double electricBloom = intensity * tier1 * (0.38D + tierRamp * 0.52D) * (0.44D + signature.electricSharpness() * 0.54D + environment.light() * 0.16D);

        if (TrippyConfig.lsdAccessibilityMode) {
            edgeHum *= 0.66D;
            geometryMelt *= 0.56D;
            realityFold *= 0.52D;
            blockAutonomy *= 0.55D;
            liquidFlow *= 0.62D;
            skyVortex *= 0.50D;
            entityTrails *= 0.52D;
            prismSplit *= 0.58D;
            electricBloom *= 0.58D;
            visual *= 0.68D;
        }

        return new LsdEffectState(
                true,
                displayedDoseMicrograms,
                activeDoseMicrograms,
                targetDoseMicrograms,
                ageTicks,
                durationTicks,
                remainingTicks(),
                phase,
                progress,
                tier,
                intensity,
                visual,
                edgeHum,
                geometryMelt,
                realityFold,
                blockAutonomy,
                liquidFlow,
                skyVortex,
                entityTrails,
                prismSplit,
                electricBloom,
                signature,
                environment
        );
    }

    public boolean isAlive() {
        return durationTicks > 0 && ageTicks < durationTicks;
    }

    public int ageTicks() {
        return ageTicks;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public void setAgeTicks(int newAgeTicks) {
        if (durationTicks <= 0) return;
        int clamped = Math.max(0, Math.min(durationTicks, newAgeTicks));
        if (clamped == ageTicks) return;
        ageTicks = clamped;
        environmentTicks = 0;
        visualEnvelope = LsdDoseCurve.phaseEnvelope(phase(), progress());
        activeDoseMicrograms = targetDoseMicrograms;
        displayedDoseMicrograms = targetDoseMicrograms;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public double targetDoseMicrograms() {
        return targetDoseMicrograms;
    }

    public LsdPhase phase() {
        return LsdDoseCurve.phase(ageTicks, durationTicks);
    }

    public double progress() {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return LsdDoseCurve.clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    public String debugSummary() {
        LsdEffectState state = effectState();
        return String.format(Locale.ROOT,
                "lsd dose=%.0fug phase=%s intensity=%.2f remaining=%ds signature=%s",
                state.displayDoseMicrograms(),
                state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(),
                state.remainingTicks() / 20,
                signature.debugName());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseMicrograms", activeDoseMicrograms);
        tag.putDouble("targetDoseMicrograms", targetDoseMicrograms);
        tag.putDouble("displayedDoseMicrograms", displayedDoseMicrograms);
        tag.putDouble("visualEnvelope", visualEnvelope);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        tag.putInt("environmentTicks", environmentTicks);
        tag.putInt("lastAnnouncedTier", lastAnnouncedTier);
        tag.putDouble("memoryWater", memoryWater);
        tag.putDouble("memoryFire", memoryFire);
        tag.putDouble("memorySky", memorySky);
        tag.putDouble("memoryArtificial", memoryArtificial);
        tag.put("signature", signature.save());
        tag.put("environment", environment.save());
        tag.put("targetEnvironment", targetEnvironment.save());
        return tag;
    }

    public static LsdSession load(CompoundTag tag) {
        LsdSession session = new LsdSession(
                LsdDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "activeDoseMicrograms", 0.0D)),
                Math.max(0, NbtCompat.getIntOr(tag, "durationTicks", 0)),
                LsdTripSignature.load(NbtCompat.getCompoundOrEmpty(tag, "signature"))
        );
        session.targetDoseMicrograms = Math.max(session.activeDoseMicrograms,
                LsdDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "targetDoseMicrograms", session.activeDoseMicrograms)));
        session.displayedDoseMicrograms = LsdDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "displayedDoseMicrograms", session.activeDoseMicrograms));
        session.ageTicks = Math.max(0, NbtCompat.getIntOr(tag, "ageTicks", 0));
        session.visualEnvelope = LsdDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "visualEnvelope",
                LsdDoseCurve.phaseEnvelope(session.phase(), session.progress())), 0.0D, 1.0D);
        session.environmentTicks = NbtCompat.getIntOr(tag, "environmentTicks", 0);
        session.lastAnnouncedTier = NbtCompat.getIntOr(tag, "lastAnnouncedTier", LsdDoseCurve.tier(session.activeDoseMicrograms));
        session.memoryWater = NbtCompat.getDoubleOr(tag, "memoryWater", 0.0D);
        session.memoryFire = NbtCompat.getDoubleOr(tag, "memoryFire", 0.0D);
        session.memorySky = NbtCompat.getDoubleOr(tag, "memorySky", 0.0D);
        session.memoryArtificial = NbtCompat.getDoubleOr(tag, "memoryArtificial", 0.0D);
        session.environment = PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "environment"));
        session.targetEnvironment = tag.contains("targetEnvironment")
                ? PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "targetEnvironment"))
                : session.environment;
        return session;
    }

    private static double memory(double current, double observed) {
        return LsdDoseCurve.clamp(current + (observed - current) * 0.0042D, 0.0D, 1.0D);
    }

    private void announceTierChanges(ServerPlayer player) {
        int tier = LsdDoseCurve.tier(activeDoseMicrograms);
        if (tier <= lastAnnouncedTier || tier <= 0 || ageTicks < 6) {
            return;
        }
        lastAnnouncedTier = tier;
        String key = switch (tier) {
            case 1 -> "message.trippycraft.lsd_tier_1";
            case 2 -> "message.trippycraft.lsd_tier_2";
            case 3 -> "message.trippycraft.lsd_tier_3";
            case 4 -> "message.trippycraft.lsd_tier_4";
            default -> "message.trippycraft.lsd_tier_5";
        };
        player.displayClientMessage(Component.translatable(key), true);
    }
}
