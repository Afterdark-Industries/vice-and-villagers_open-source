package com.trippycraft.substance.cocaine;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;

import java.util.Locale;

public final class CocaineSession {
    private double activeDoseGrams;
    private double targetDoseGrams;
    private double displayedDoseGrams;
    private double activeRouteSpeed;
    private double targetRouteSpeed;
    private double activeRouteIntensity;
    private double targetRouteIntensity;
    private double targetDurationScale;
    private int ageTicks;
    private int durationTicks;
    private int plannedAftermathTicks;
    private int aftermathTicks;

    private CocaineSession(double doseGrams, CocaineRoute route, int durationTicks) {
        this.activeDoseGrams = doseGrams;
        this.targetDoseGrams = doseGrams;
        this.displayedDoseGrams = doseGrams;
        this.activeRouteSpeed = route.speedMultiplier();
        this.targetRouteSpeed = route.speedMultiplier();
        this.activeRouteIntensity = route.intensityBoost();
        this.targetRouteIntensity = route.intensityBoost();
        this.targetDurationScale = route.durationScale();
        this.durationTicks = durationTicks;
        this.plannedAftermathTicks = doseAftermathTicks(doseGrams);
    }

    public static CocaineSession start(double doseGrams, CocaineRoute route) {
        return new CocaineSession(doseGrams, route, CocaineDoseCurve.baseDurationTicks(doseGrams, route.durationScale()));
    }

    public void stackDose(double addedDoseGrams, CocaineRoute route) {
        double added = Math.max(0.0D, addedDoseGrams);
        if (added <= 0.0D) {
            return;
        }
        if (!hasActiveEffects()) {
            restart(added, route);
            return;
        }
        double previousTarget = targetDoseGrams;
        targetDoseGrams += added;
        plannedAftermathTicks += doseAftermathTicks(added);
        double weight = added / Math.max(0.0001D, targetDoseGrams);
        targetRouteSpeed = blend(targetRouteSpeed, route.speedMultiplier(), weight);
        targetRouteIntensity = blend(targetRouteIntensity, route.intensityBoost(), weight);
        targetDurationScale = blend(targetDurationScale, route.durationScale(), weight);

        int targetDuration = CocaineDoseCurve.baseDurationTicks(targetDoseGrams, targetDurationScale);
        int extension = CocaineDoseCurve.stackExtensionTicks(added, Math.max(previousTarget, targetDoseGrams), route.extensionScale(), targetDurationScale);
        durationTicks = Math.max(durationTicks + extension, Math.max(targetDuration, ageTicks + extension));
    }

    public void restart(double doseGrams, CocaineRoute route) {
        int carryOverAftermath = aftermathTicks;
        activeDoseGrams = doseGrams;
        targetDoseGrams = doseGrams;
        displayedDoseGrams = doseGrams;
        activeRouteSpeed = route.speedMultiplier();
        targetRouteSpeed = route.speedMultiplier();
        activeRouteIntensity = route.intensityBoost();
        targetRouteIntensity = route.intensityBoost();
        targetDurationScale = route.durationScale();
        ageTicks = 0;
        durationTicks = CocaineDoseCurve.baseDurationTicks(doseGrams, route.durationScale());
        plannedAftermathTicks = carryOverAftermath + doseAftermathTicks(doseGrams);
        aftermathTicks = 0;
    }

    public void tick() {
        if (!hasActiveEffects()) {
            if (aftermathTicks > 0) {
                aftermathTicks--;
            }
            return;
        }

        ageTicks++;
        double doseDelta = Math.abs(targetDoseGrams - activeDoseGrams);
        double routeUrgency = Math.max(0.70D, activeRouteSpeed);
        double ramp = (targetDoseGrams > activeDoseGrams ? 0.010D : 0.0042D)
                + CocaineDoseCurve.smoothstep(0.20D, 4.0D, doseDelta) * 0.0080D;
        activeDoseGrams += (targetDoseGrams - activeDoseGrams) * ramp * routeUrgency;
        displayedDoseGrams += (targetDoseGrams - displayedDoseGrams) * 0.018D * routeUrgency;
        activeRouteSpeed += (targetRouteSpeed - activeRouteSpeed) * 0.055D;
        activeRouteIntensity += (targetRouteIntensity - activeRouteIntensity) * 0.045D;
        if (Math.abs(targetDoseGrams - activeDoseGrams) < 0.002D) {
            activeDoseGrams = targetDoseGrams;
        }
        if (Math.abs(targetDoseGrams - displayedDoseGrams) < 0.002D) {
            displayedDoseGrams = targetDoseGrams;
        }
        if (Math.abs(targetRouteSpeed - activeRouteSpeed) < 0.0005D) {
            activeRouteSpeed = targetRouteSpeed;
        }
        if (Math.abs(targetRouteIntensity - activeRouteIntensity) < 0.0005D) {
            activeRouteIntensity = targetRouteIntensity;
        }
        if (!hasActiveEffects()) {
            aftermathTicks = Math.max(aftermathTicks, plannedAftermathTicks);
        }
    }

    public CocaineEffectState effectState() {
        if (!hasActiveEffects()) {
            return CocaineEffectState.inactive();
        }
        CocainePhase phase = phase();
        double progress = progress();
        int tier = CocaineDoseCurve.tier(activeDoseGrams);
        double phaseEnvelope = CocaineDoseCurve.phaseEnvelope(phase, progress);
        double doseIntensity = CocaineDoseCurve.peakIntensity(activeDoseGrams, activeRouteIntensity);
        double urgency = CocaineDoseCurve.clamp(activeRouteSpeed, 0.85D, 1.80D);
        double wave = Math.sin(ageTicks * (0.030D + urgency * 0.010D) + activeDoseGrams * 0.8D) * 0.040D
                + Math.sin(ageTicks * (0.072D + urgency * 0.020D) + targetDoseGrams * 0.5D) * CocaineDoseCurve.smoothstep(1.5D, 5.0D, activeDoseGrams) * 0.032D;
        double living = CocaineDoseCurve.clamp(1.0D + wave, 0.88D, 1.16D);
        double intensity = CocaineDoseCurve.clamp(doseIntensity * phaseEnvelope * living, 0.0D, 5.8D);

        double mild = CocaineDoseCurve.smoothstep(0.30D, 1.00D, activeDoseGrams);
        double moderate = CocaineDoseCurve.smoothstep(1.50D, 3.00D, activeDoseGrams);
        double heavy = CocaineDoseCurve.smoothstep(3.50D, 5.00D, activeDoseGrams);
        double extreme = CocaineDoseCurve.smoothstep(5.50D, 8.00D, activeDoseGrams);
        double onset = CocaineDoseCurve.smoothstep(0.00D, 0.12D, progress);
        double rush = CocaineDoseCurve.smoothstep(0.08D, 0.24D, progress);
        double peak = CocaineDoseCurve.smoothstep(0.18D, 0.55D, progress);
        double edge = CocaineDoseCurve.smoothstep(0.52D, 0.78D, progress);
        double crash = CocaineDoseCurve.smoothstep(0.76D, 0.96D, progress);
        double after = CocaineDoseCurve.smoothstep(0.92D, 1.00D, progress);

        double stimulation = intensity * (0.34D + onset * 0.18D + rush * 0.34D + peak * 0.32D + edge * 0.10D);
        double locomotorDrive = intensity * (0.10D + rush * 0.28D + peak * 0.24D + edge * 0.10D) * (0.62D + mild * 0.18D + moderate * 0.18D);
        double inputSharpness = intensity * (0.12D + onset * 0.18D + rush * 0.22D + peak * 0.14D) * (0.70D + urgency * 0.18D);
        double jitter = intensity * (moderate * 0.16D + heavy * 0.24D + extreme * 0.32D) * (0.10D + peak * 0.18D + edge * 0.42D);
        double overcorrection = intensity * (moderate * 0.14D + heavy * 0.24D + extreme * 0.30D) * (0.08D + peak * 0.18D + edge * 0.46D);
        double compulsiveMomentum = intensity * (heavy * 0.18D + extreme * 0.34D) * (0.10D + rush * 0.14D + peak * 0.22D + edge * 0.34D);
        double tunnelFocus = intensity * (0.12D + rush * 0.18D + peak * 0.30D + edge * 0.22D);
        double clarityLift = intensity * (0.10D + onset * 0.16D + rush * 0.18D + peak * 0.14D);
        double edgeShimmer = intensity * (moderate * 0.12D + heavy * 0.18D + extreme * 0.20D) * (0.10D + peak * 0.22D + edge * 0.18D);
        double lightHarshness = intensity * (moderate * 0.12D + heavy * 0.22D + extreme * 0.32D) * (0.08D + peak * 0.24D + edge * 0.34D);
        double microFlicker = intensity * (heavy * 0.14D + extreme * 0.30D) * (0.06D + peak * 0.14D + edge * 0.42D);
        double euphoria = intensity * (mild * 0.14D + moderate * 0.22D + heavy * 0.16D) * CocaineDoseCurve.clamp(0.36D + rush * 0.20D + peak * 0.16D - crash * 0.18D, 0.0D, 0.82D);
        double anxiety = intensity * (moderate * 0.14D + heavy * 0.22D + extreme * 0.36D) * (0.08D + peak * 0.10D + edge * 0.42D + crash * 0.20D);
        double craving = intensity * (0.08D + peak * 0.14D + edge * 0.36D + crash * 0.28D);
        double risk = intensity * (0.08D + heavy * 0.16D + extreme * 0.30D) * (0.10D + peak * 0.16D + edge * 0.34D + crash * 0.12D);
        double crashDrag = intensity * (0.06D + crash * 0.40D + after * 0.20D) * (0.18D + heavy * 0.16D + extreme * 0.18D);
        double crashDesaturation = intensity * (crash * 0.16D + after * 0.10D);
        double alcoholMask = CocaineDoseCurve.clamp(
                (0.16D + rush * 0.24D + peak * 0.40D + edge * 0.16D)
                        * (0.20D + CocaineDoseCurve.smoothstep(0.5D, 5.0D, activeDoseGrams) * 0.80D)
                        * (0.84D + (urgency - 1.0D) * 0.38D),
                0.0D,
                1.0D
        );

        return new CocaineEffectState(
                true,
                displayedDoseGrams,
                activeDoseGrams,
                targetDoseGrams,
                ageTicks,
                durationTicks,
                remainingTicks(),
                phase,
                progress,
                tier,
                intensity,
                stimulation,
                locomotorDrive,
                inputSharpness,
                jitter,
                overcorrection,
                compulsiveMomentum,
                tunnelFocus,
                clarityLift,
                edgeShimmer,
                lightHarshness,
                microFlicker,
                anxiety,
                euphoria,
                craving,
                risk,
                crashDrag,
                crashDesaturation,
                alcoholMask
        );
    }

    public boolean isAlive() {
        return hasActiveEffects() || aftermathTicks > 0;
    }

    public boolean hasActiveEffects() {
        return ageTicks < durationTicks;
    }

    public boolean hasAftermath() {
        return aftermathTicks > 0;
    }

    public boolean blocksSleep() {
        return hasActiveEffects();
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public CocainePhase phase() {
        return CocaineDoseCurve.phase(ageTicks, durationTicks);
    }

    public double progress() {
        return CocaineDoseCurve.progress(ageTicks, durationTicks);
    }

    public double targetDoseGrams() {
        return targetDoseGrams;
    }

    public int aftermathTicks() {
        return aftermathTicks;
    }

    public double sessionDoseGrams() {
        return Math.max(targetDoseGrams, activeDoseGrams);
    }

    public String debugSummary() {
        if (!hasActiveEffects()) {
            return String.format(Locale.ROOT, "cocaine_aftermath remaining=%ds dose=%.1fg", aftermathTicks / 20, sessionDoseGrams());
        }
        CocaineEffectState state = effectState();
        return String.format(Locale.ROOT,
                "cocaine dose=%.1fg tier=%d phase=%s intensity=%.2f remaining=%ds",
                state.displayDoseGrams(),
                state.tier(),
                state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(),
                state.remainingTicks() / 20);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeDoseGrams", activeDoseGrams);
        tag.putDouble("targetDoseGrams", targetDoseGrams);
        tag.putDouble("displayedDoseGrams", displayedDoseGrams);
        tag.putDouble("activeRouteSpeed", activeRouteSpeed);
        tag.putDouble("targetRouteSpeed", targetRouteSpeed);
        tag.putDouble("activeRouteIntensity", activeRouteIntensity);
        tag.putDouble("targetRouteIntensity", targetRouteIntensity);
        tag.putDouble("targetDurationScale", targetDurationScale);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        tag.putInt("plannedAftermathTicks", plannedAftermathTicks);
        tag.putInt("aftermathTicks", aftermathTicks);
        return tag;
    }

    public static CocaineSession load(CompoundTag tag) {
        CocaineRoute route = NbtCompat.getDoubleOr(tag, "targetDurationScale", 1.0D) < 0.75D ? CocaineRoute.CRACK : CocaineRoute.POWDER;
        CocaineSession session = new CocaineSession(
                NbtCompat.getDoubleOr(tag, "activeDoseGrams", 0.0D),
                route,
                NbtCompat.getIntOr(tag, "durationTicks", 0)
        );
        session.targetDoseGrams = NbtCompat.getDoubleOr(tag, "targetDoseGrams", session.activeDoseGrams);
        session.displayedDoseGrams = NbtCompat.getDoubleOr(tag, "displayedDoseGrams", session.activeDoseGrams);
        session.activeRouteSpeed = NbtCompat.getDoubleOr(tag, "activeRouteSpeed", route.speedMultiplier());
        session.targetRouteSpeed = NbtCompat.getDoubleOr(tag, "targetRouteSpeed", session.activeRouteSpeed);
        session.activeRouteIntensity = NbtCompat.getDoubleOr(tag, "activeRouteIntensity", route.intensityBoost());
        session.targetRouteIntensity = NbtCompat.getDoubleOr(tag, "targetRouteIntensity", session.activeRouteIntensity);
        session.targetDurationScale = NbtCompat.getDoubleOr(tag, "targetDurationScale", route.durationScale());
        session.ageTicks = NbtCompat.getIntOr(tag, "ageTicks", 0);
        session.plannedAftermathTicks = Math.max(doseAftermathTicks(session.targetDoseGrams), NbtCompat.getIntOr(tag, "plannedAftermathTicks", doseAftermathTicks(session.targetDoseGrams)));
        session.aftermathTicks = Math.max(0, NbtCompat.getIntOr(tag, "aftermathTicks", 0));
        int curveDuration = CocaineDoseCurve.baseDurationTicks(session.targetDoseGrams, session.targetDurationScale);
        int stackedDurationCap = curveDuration + CocaineDoseCurve.stackExtensionTicks(session.targetDoseGrams * 0.35D, session.targetDoseGrams, 0.85D, session.targetDurationScale);
        if (session.durationTicks < curveDuration) {
            session.durationTicks = curveDuration;
        } else if (session.durationTicks > stackedDurationCap) {
            session.durationTicks = Math.max(session.ageTicks + 40, stackedDurationCap);
        }
        if (!session.hasActiveEffects() && session.plannedAftermathTicks > 0 && session.aftermathTicks <= 0) {
            session.aftermathTicks = session.plannedAftermathTicks;
        }
        return session;
    }

    private static int doseAftermathTicks(double doseGrams) {
        return Math.max(0, (int) Math.round(Math.max(0.0D, doseGrams) * 60.0D * 20.0D));
    }

    private static double blend(double current, double next, double weight) {
        double clamped = CocaineDoseCurve.clamp(weight, 0.0D, 1.0D);
        return current + (next - current) * clamped;
    }
}