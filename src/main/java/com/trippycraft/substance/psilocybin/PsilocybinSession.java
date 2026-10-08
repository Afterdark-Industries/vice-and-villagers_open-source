package com.trippycraft.substance.psilocybin;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.config.TrippyConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class PsilocybinSession {
    private static final int ENVIRONMENT_UPDATE_INTERVAL = 30;

    private double activeDoseGrams;
    private double targetDoseGrams;
    private double displayedDoseGrams;
    private double visualEnvelope;
    private int ageTicks;
    private int durationTicks;
    private int environmentTicks;
    private double tripMemoryForest;
    private double tripMemoryCave;
    private double tripMemoryRain;
    private double tripMemoryFast;
    private double tripMemoryStill;
    private double tripMemoryEntities;
    private PsilocybinTripSignature signature;
    private PsilocybinEnvironmentSnapshot environment = PsilocybinEnvironmentSnapshot.empty();
    private PsilocybinEnvironmentSnapshot targetEnvironment = PsilocybinEnvironmentSnapshot.empty();

    private PsilocybinSession(double doseGrams, int durationTicks, PsilocybinTripSignature signature) {
        this.activeDoseGrams = doseGrams;
        this.targetDoseGrams = doseGrams;
        this.displayedDoseGrams = doseGrams;
        this.durationTicks = durationTicks;
        this.signature = signature;
    }

    public static PsilocybinSession start(ServerPlayer player, double doseGrams) {
        double dose = PsilocybinDoseCurve.sanitizeDose(doseGrams);
        long seed = player == null ? Double.doubleToLongBits(dose) ^ 0x51F2EEDL
                : player.getUUID().getMostSignificantBits() ^ player.level().getGameTime() ^ Double.doubleToLongBits(dose);
        PsilocybinSession session = new PsilocybinSession(dose, PsilocybinDoseCurve.baseDurationTicks(dose), PsilocybinTripSignature.create(seed));
        if (player != null) session.environment = PsilocybinEnvironmentSnapshot.capture(player);
        session.targetEnvironment = session.environment;
        return session;
    }

    public void stackDose(double addedDoseGrams) {
        double added = PsilocybinDoseCurve.sanitizeDose(addedDoseGrams);
        if (added <= 0.0D) return;
        targetDoseGrams = PsilocybinDoseCurve.safeAdd(targetDoseGrams, added);
        int extension = (int) Math.round(PsilocybinDoseCurve.baseDurationTicks(added) * 0.45D);
        long extended = Math.max((long) durationTicks, (long) ageTicks + extension)
                + Math.round(extension * PsilocybinDoseCurve.clamp(targetDoseGrams / 8.0D, 0.0D, 0.35D));
        durationTicks = (int) Math.min(Integer.MAX_VALUE, extended);
        // Keep the currently rendered envelope and absorbed amount. The new amount and
        // extended timeline enter gradually on subsequent ticks, never on right-click.
    }

    public void tick(ServerPlayer player) {
        if (!isAlive()) return;
        ageTicks++;
        double doseDelta = Math.abs(targetDoseGrams - activeDoseGrams);
        double ramp = (targetDoseGrams > activeDoseGrams ? 0.0038D : 0.0015D)
                + PsilocybinDoseCurve.smoothstep(0.25D, 4.0D, doseDelta) * 0.0030D;
        activeDoseGrams = approach(activeDoseGrams, targetDoseGrams, ramp);
        displayedDoseGrams = approach(displayedDoseGrams, targetDoseGrams, 0.010D);
        double desiredEnvelope = PsilocybinDoseCurve.phaseEnvelope(phase(), progress());
        // Close the interpolation lag gently during the final two seconds. Keeping the
        // complete envelope here also preserves an almost-finished trip when redosed.
        double envelopeRate = Math.max(0.012D, 1.0D / Math.max(1, remainingTicks()));
        visualEnvelope += (desiredEnvelope - visualEnvelope) * envelopeRate;

        environmentTicks--;
        if (player != null && environmentTicks <= 0) {
            targetEnvironment = PsilocybinEnvironmentSnapshot.capture(player);
            environmentTicks = ENVIRONMENT_UPDATE_INTERVAL;
        }
        // Environment scans are discrete; their color/geometry influence should not be.
        environment = environment.blend(targetEnvironment, 0.010D + signature.environmentalSensitivity() * 0.004D);

        updateTripMemory();
    }

    public PsilocybinEffectState effectState() {
        if (!isAlive()) return PsilocybinEffectState.inactive();
        PsilocybinPhase phase = phase();
        double progress = progress();
        // Older installations used 1.25 as a ceiling. Interpreting that value as
        // 1.25 / 6 gain erased most of the reworked effects; keep it as compatibility
        // metadata and give the new gain its own neutral default.
        double visualGain = TrippyConfig.psilocybinMaxVisualIntensity <= 0.0D ? 0.0D
                : PsilocybinDoseCurve.clamp(TrippyConfig.psilocybinVisualGain, 0.0D, 2.0D);
        double doseIntensity = PsilocybinDoseCurve.peakIntensity(activeDoseGrams) * visualGain;
        double mediumDose = PsilocybinDoseCurve.smoothstep(1.0D, 3.0D, activeDoseGrams);
        double highDose = PsilocybinDoseCurve.smoothstep(3.0D, 5.0D, activeDoseGrams);
        double extreme = PsilocybinDoseCurve.smoothstep(5.0D, 10.0D, activeDoseGrams);
        double overload = PsilocybinDoseCurve.smoothstep(10.0D, 14.0D, activeDoseGrams);
        double phaseEnvelope = visualEnvelope;
        double aliveDrift = Math.sin(ageTicks * 0.0036D * signature.waveRhythm() + (signature.seed() & 1023L) * 0.01D) * 0.035D
                + Math.sin(ageTicks * 0.0017D * signature.breathingRhythm() + 1.7D) * (0.014D + mediumDose * 0.010D + highDose * 0.012D);
        double slowWave = 1.0D + aliveDrift;
        double intensity = doseIntensity * phaseEnvelope * slowWave;
        double visual = PsilocybinDoseCurve.visualIntensity(activeDoseGrams) * visualGain * phaseEnvelope * slowWave;
        double amplifier = 1.0D + mediumDose * 0.55D + highDose * 0.95D + extreme * 1.30D + overload * 1.10D;
        double breath = intensity * amplifier * (0.55D + signature.geometrySoftness() * 0.55D + environment.organicBlocks() * 0.30D + tripMemoryForest * 0.12D);
        double melt = intensity * amplifier * PsilocybinDoseCurve.smoothstep(1.1D, 3.6D, activeDoseGrams) * (0.55D + signature.geometrySoftness() * 0.50D);
        double tracers = intensity * amplifier * (0.30D + signature.tracerPersistence() * 0.50D + environment.moving() * 0.40D + tripMemoryFast * 0.16D);
        double spores = intensity * amplifier * (0.30D + environment.forest() * 0.32D + environment.cave() * 0.26D + environment.rain() * 0.18D) * TrippyConfig.psilocybinParticleMultiplier;
        double fungal = intensity * amplifier * PsilocybinDoseCurve.smoothstep(0.9D, 3.4D, activeDoseGrams) * (0.40D + signature.hallucinationTendency() * 0.55D + tripMemoryStill * 0.18D);
        double colorWarmth = intensity * amplifier * (0.40D + environment.forest() * 0.20D + environment.sunriseSunset() * 0.30D + environment.nether() * 0.22D);
        double colorDepth = intensity * amplifier * (0.32D + environment.cave() * 0.28D + environment.night() * 0.22D + environment.end() * 0.30D);
        double audioDepth = intensity * (0.25D + environment.cave() * 0.30D + environment.water() * 0.22D + signature.emotionalTone() * 0.14D);
        if (TrippyConfig.psilocybinAccessibilityMode) {
            fungal *= 0.55D;
            tracers *= 0.62D;
            spores *= 0.70D;
            visual *= 0.72D;
            melt *= 0.55D;
            breath *= 0.60D;
        }

        return new PsilocybinEffectState(
                true,
                displayedDoseGrams,
                activeDoseGrams,
                targetDoseGrams,
                ageTicks,
                durationTicks,
                remainingTicks(),
                phase,
                progress,
                intensity,
                visual,
                breath,
                melt,
                tracers,
                spores,
                fungal,
                colorWarmth,
                colorDepth,
                audioDepth,
                0.0D,
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
        if (durationTicks <= 0) {
            return;
        }
        int clamped = Math.max(0, Math.min(durationTicks, newAgeTicks));
        if (clamped == ageTicks) {
            return;
        }
        ageTicks = clamped;
        environmentTicks = 0;
        visualEnvelope = PsilocybinDoseCurve.phaseEnvelope(phase(), progress());
        activeDoseGrams = targetDoseGrams;
        displayedDoseGrams = targetDoseGrams;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public double targetDoseGrams() {
        return targetDoseGrams;
    }

    public PsilocybinPhase phase() {
        return PsilocybinDoseCurve.phase(ageTicks, durationTicks);
    }

    public double progress() {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return PsilocybinDoseCurve.clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    public String debugSummary() {
        PsilocybinEffectState state = effectState();
        return String.format(Locale.ROOT,
                "psilocybin dose=%.2fg phase=%s intensity=%.2f remaining=%ds signature=%s",
                state.displayDoseGrams(),
                state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(),
                state.remainingTicks() / 20,
                signature.debugName());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseGrams", activeDoseGrams);
        tag.putDouble("targetDoseGrams", targetDoseGrams);
        tag.putDouble("displayedDoseGrams", displayedDoseGrams);
        tag.putDouble("visualEnvelope", visualEnvelope);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        tag.putInt("environmentTicks", environmentTicks);
        tag.putDouble("tripMemoryForest", tripMemoryForest);
        tag.putDouble("tripMemoryCave", tripMemoryCave);
        tag.putDouble("tripMemoryRain", tripMemoryRain);
        tag.putDouble("tripMemoryFast", tripMemoryFast);
        tag.putDouble("tripMemoryStill", tripMemoryStill);
        tag.putDouble("tripMemoryEntities", tripMemoryEntities);
        tag.put("signature", signature.save());
        tag.put("environment", environment.save());
        tag.put("targetEnvironment", targetEnvironment.save());
        return tag;
    }

    public static PsilocybinSession load(CompoundTag tag) {
        PsilocybinSession session = new PsilocybinSession(
                PsilocybinDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "activeDoseGrams", 0.0D)),
                Math.max(0, NbtCompat.getIntOr(tag, "durationTicks", 0)),
                PsilocybinTripSignature.load(NbtCompat.getCompoundOrEmpty(tag, "signature"))
        );
        session.targetDoseGrams = Math.max(session.activeDoseGrams, PsilocybinDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "targetDoseGrams", session.activeDoseGrams)));
        session.displayedDoseGrams = PsilocybinDoseCurve.sanitizeDose(NbtCompat.getDoubleOr(tag, "displayedDoseGrams", session.activeDoseGrams));
        session.ageTicks = Math.max(0, NbtCompat.getIntOr(tag, "ageTicks", 0));
        session.visualEnvelope = PsilocybinDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "visualEnvelope",
                PsilocybinDoseCurve.phaseEnvelope(session.phase(), session.progress())), 0.0D, 1.0D);
        session.environmentTicks = NbtCompat.getIntOr(tag, "environmentTicks", 0);
        session.tripMemoryForest = NbtCompat.getDoubleOr(tag, "tripMemoryForest", 0.0D);
        session.tripMemoryCave = NbtCompat.getDoubleOr(tag, "tripMemoryCave", 0.0D);
        session.tripMemoryRain = NbtCompat.getDoubleOr(tag, "tripMemoryRain", 0.0D);
        session.tripMemoryFast = NbtCompat.getDoubleOr(tag, "tripMemoryFast", 0.0D);
        session.tripMemoryStill = NbtCompat.getDoubleOr(tag, "tripMemoryStill", 0.0D);
        session.tripMemoryEntities = NbtCompat.getDoubleOr(tag, "tripMemoryEntities", 0.0D);
        session.environment = PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "environment"));
        session.targetEnvironment = tag.contains("targetEnvironment")
                ? PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "targetEnvironment"))
                : session.environment;
        return session;
    }

    private void updateTripMemory() {
        tripMemoryForest = memory(tripMemoryForest, environment.forest());
        tripMemoryCave = memory(tripMemoryCave, environment.cave());
        tripMemoryRain = memory(tripMemoryRain, environment.rain());
        tripMemoryFast = memory(tripMemoryFast, environment.moving());
        tripMemoryStill = memory(tripMemoryStill, environment.stillness());
        tripMemoryEntities = memory(tripMemoryEntities, environment.entities());
    }

    private static double memory(double current, double observed) {
        return PsilocybinDoseCurve.clamp(current + (observed - current) * 0.0035D, 0.0D, 1.0D);
    }

    private static double approach(double current, double target, double rate) {
        // Dose values are non-negative, so their difference cannot overflow. This form
        // also leaves an already absorbed dose exactly unchanged between ticks.
        return current + (target - current) * rate;
    }
}
