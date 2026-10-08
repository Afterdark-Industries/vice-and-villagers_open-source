package com.trippycraft.substance.nitrous;

import com.trippycraft.compat.NbtCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class NitrousSession {
    private double activeHits;
    private double effectiveTargetHits;
    private double targetHits;
    private double displayedHits;
    private double visualEnvelope;
    private int ageTicks;
    private int durationTicks;
    private int repeatedHits;

    private NitrousSession(double hits, int duration) {
        activeHits = hits;
        effectiveTargetHits = hits;
        targetHits = hits;
        displayedHits = hits;
        durationTicks = duration;
    }

    public static NitrousSession start(ServerPlayer player, double hits) {
        double dose = NitrousDoseCurve.sanitizeHits(hits);
        return new NitrousSession(dose, NitrousDoseCurve.baseDurationTicks(dose));
    }

    public void stackDose(double addedHits) {
        double added = NitrousDoseCurve.sanitizeHits(addedHits);
        if (added <= 0.0D) {
            return;
        }
        repeatedHits = Math.min(Integer.MAX_VALUE - 1, repeatedHits) + 1;
        targetHits = NitrousDoseCurve.safeAdd(targetHits, added);
        effectiveTargetHits = NitrousDoseCurve.safeAdd(effectiveTargetHits, added);
        int extension = NitrousDoseCurve.baseDurationTicks(added);
        durationTicks = (int) Math.min(NitrousDoseCurve.maximumDurationTicks(),
                Math.max((long) durationTicks + extension, (long) ageTicks + extension));
    }

    public void tick(ServerPlayer player) {
        if (!isAlive()) {
            return;
        }
        ageTicks++;
        activeHits = approach(activeHits, effectiveTargetHits, 0.12D);
        displayedHits = approach(displayedHits, targetHits, 0.18D);
        double desired = NitrousDoseCurve.phaseEnvelope(phase(), progress());
        visualEnvelope += (desired - visualEnvelope) * 0.18D;
    }

    public NitrousEffectState effectState() {
        if (!isAlive()) {
            return NitrousEffectState.inactive();
        }
        double envelope = visualEnvelope * NitrousDoseCurve.smoothstep(0.0D, 12.0D, remainingTicks());
        double intensity = NitrousDoseCurve.peakIntensity(activeHits) * envelope;
        double presence = NitrousDoseCurve.visualPresence(intensity);
        double amplitude = NitrousDoseCurve.visualAmplitude(activeHits) * envelope;
        return new NitrousEffectState(true, displayedHits, activeHits, targetHits, ageTicks, durationTicks,
                remainingTicks(), phase(), progress(), NitrousDoseCurve.tier(activeHits), repeatedHits, intensity,
                presence,
                amplitude * 1.15D,
                amplitude * 1.22D,
                amplitude * 1.05D,
                amplitude * 1.18D,
                amplitude * 1.10D,
                amplitude * 1.20D,
                amplitude * 0.95D,
                amplitude * 0.55D,
                amplitude * 0.72D,
                amplitude * 0.80D,
                amplitude * 0.42D,
                amplitude * 0.70D);
    }

    public boolean isAlive() {
        return durationTicks > 0 && ageTicks < durationTicks;
    }

    public int remainingTicks() {
        return Math.max(0, durationTicks - ageTicks);
    }

    public double targetHits() {
        return targetHits;
    }

    public NitrousPhase phase() {
        return NitrousDoseCurve.phase(ageTicks, durationTicks);
    }

    public double progress() {
        return NitrousDoseCurve.progress(ageTicks, durationTicks);
    }

    public void setAgeTicks(int newAgeTicks) {
        ageTicks = Math.max(0, Math.min(durationTicks, newAgeTicks));
        visualEnvelope = NitrousDoseCurve.phaseEnvelope(phase(), progress());
    }

    public String debugSummary() {
        NitrousEffectState state = effectState();
        return state.active() ? String.format(Locale.ROOT,
                "nitrous hits=%.0f phase=%s intensity=%.2f remaining=%ds",
                state.displayHits(), state.phase().name().toLowerCase(Locale.ROOT),
                state.intensity(), state.remainingTicks() / 20) : "nitrous=inactive";
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("activeHits", activeHits);
        tag.putDouble("effectiveTargetHits", effectiveTargetHits);
        tag.putDouble("targetHits", targetHits);
        tag.putDouble("displayedHits", displayedHits);
        tag.putDouble("visualEnvelope", visualEnvelope);
        tag.putInt("ageTicks", ageTicks);
        tag.putInt("durationTicks", durationTicks);
        tag.putInt("repeatedHits", repeatedHits);
        return tag;
    }

    public static NitrousSession load(CompoundTag tag) {
        double active = NitrousDoseCurve.sanitizeHits(NbtCompat.getDoubleOr(tag, "activeHits", 0.0D));
        NitrousSession session = new NitrousSession(active,
                Math.max(0, Math.min(NitrousDoseCurve.maximumDurationTicks(), NbtCompat.getIntOr(tag, "durationTicks", 0))));
        session.effectiveTargetHits = Math.max(active, NitrousDoseCurve.sanitizeHits(
                NbtCompat.getDoubleOr(tag, "effectiveTargetHits", active)));
        session.targetHits = Math.max(session.effectiveTargetHits,
                NitrousDoseCurve.sanitizeHits(NbtCompat.getDoubleOr(tag, "targetHits", active)));
        session.displayedHits = NitrousDoseCurve.sanitizeHits(NbtCompat.getDoubleOr(tag, "displayedHits", active));
        session.ageTicks = Math.max(0, NbtCompat.getIntOr(tag, "ageTicks", 0));
        session.visualEnvelope = NitrousDoseCurve.clamp(NbtCompat.getDoubleOr(tag, "visualEnvelope",
                NitrousDoseCurve.phaseEnvelope(session.phase(), session.progress())), 0.0D, 1.0D);
        session.repeatedHits = Math.max(0, NbtCompat.getIntOr(tag, "repeatedHits", 0));
        return session;
    }

    private static double approach(double current, double target, double rate) {
        return current * (1.0D - rate) + target * rate;
    }
}
