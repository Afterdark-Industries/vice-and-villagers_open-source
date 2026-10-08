package com.trippycraft.substance;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.twoce.TwoCeEffectState;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;

public record PsychedelicBlendState(
        boolean psilocybinActive,
        boolean lsdActive,
        boolean dmtActive,
        boolean hoMetActive,
        boolean alLadActive,
        boolean twoCeActive,
        double psilocybinLoad,
        double lsdLoad,
        double dmtLoad,
        double hoMetLoad,
        double alLadLoad,
        double twoCeLoad,
        double psilocybinWeight,
        double lsdWeight,
        double dmtWeight,
        double hoMetWeight,
        double alLadWeight,
        double twoCeWeight,
        double coPresence,
        int activeCount
) {
    private static final double ACTIVE_WEIGHT_FLOOR = 0.035D;

    public static PsychedelicBlendState from(PsilocybinEffectState psilocybin, LsdEffectState lsd) {
        return from(psilocybin, lsd, DmtEffectState.inactive());
    }

    public static PsychedelicBlendState from(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt) {
        return from(psilocybin, lsd, dmt, HoMetEffectState.inactive());
    }

    public static PsychedelicBlendState from(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet) {
        return from(psilocybin, lsd, dmt, hoMet, AlLadEffectState.inactive());
    }

    public static PsychedelicBlendState from(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet, AlLadEffectState alLad) {
        return from(psilocybin, lsd, dmt, hoMet, alLad, TwoCeEffectState.inactive());
    }

    public static PsychedelicBlendState from(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet, AlLadEffectState alLad, TwoCeEffectState twoCe) {
        boolean psilocybinActive = psilocybin != null && psilocybin.active() && TrippyConfig.enablePsilocybinEffects;
        boolean lsdActive = lsd != null && lsd.active() && TrippyConfig.enableLsdEffects;
        boolean dmtActive = dmt != null && dmt.active() && TrippyConfig.enableDmtEffects;
        boolean hoMetActive = hoMet != null && hoMet.active();
        boolean alLadActive = alLad != null && alLad.active();
        boolean twoCeActive = twoCe != null && twoCe.active();
        double hoMetLoad = hoMetActive ? saturate(hoMet.visualPresence() * 0.65D + hoMet.spatialDepth() * 0.35D) : 0.0D;
        double alLadLoad = alLadActive ? saturate(alLad.visualPresence() * 0.62D + alLad.stainedGlass() * 0.38D) : 0.0D;
        double twoCeLoad = twoCeActive ? saturate(twoCe.visualPresence() * 0.60D + twoCe.tessellation() * 0.40D) : 0.0D;
        double psilocybinLoad = psilocybinActive ? psilocybinLoad(psilocybin) : 0.0D;
        double lsdLoad = lsdActive ? lsdLoad(lsd) : 0.0D;
        double dmtLoad = dmtActive ? dmtLoad(dmt) : 0.0D;

        double psilocybinWeightLoad = psilocybinActive ? Math.max(psilocybinLoad, ACTIVE_WEIGHT_FLOOR) : 0.0D;
        double lsdWeightLoad = lsdActive ? Math.max(lsdLoad, ACTIVE_WEIGHT_FLOOR) : 0.0D;
        double dmtWeightLoad = dmtActive ? Math.max(dmtLoad, ACTIVE_WEIGHT_FLOOR) : 0.0D;
        double hoMetWeightLoad = hoMetActive ? hoMetLoad : 0.0D;
        double alLadWeightLoad = alLadActive ? alLadLoad : 0.0D;
        double twoCeWeightLoad = twoCeActive ? twoCeLoad : 0.0D;
        double totalWeightLoad = psilocybinWeightLoad + lsdWeightLoad + dmtWeightLoad + hoMetWeightLoad + alLadWeightLoad + twoCeWeightLoad;
        double psilocybinWeight = totalWeightLoad <= 0.0D ? 0.0D : psilocybinWeightLoad / totalWeightLoad;
        double lsdWeight = totalWeightLoad <= 0.0D ? 0.0D : lsdWeightLoad / totalWeightLoad;
        double dmtWeight = totalWeightLoad <= 0.0D ? 0.0D : dmtWeightLoad / totalWeightLoad;
        double hoMetWeight = totalWeightLoad <= 0.0D ? 0.0D : hoMetWeightLoad / totalWeightLoad;
        double alLadWeight = totalWeightLoad <= 0.0D ? 0.0D : alLadWeightLoad / totalWeightLoad;
        double twoCeWeight = totalWeightLoad <= 0.0D ? 0.0D : twoCeWeightLoad / totalWeightLoad;
        int activeCount = (psilocybinActive ? 1 : 0) + (lsdActive ? 1 : 0) + (dmtActive ? 1 : 0) + (hoMetActive ? 1 : 0) + (alLadActive ? 1 : 0) + (twoCeActive ? 1 : 0);
        double coPresence = 0.0D;
        if (activeCount >= 2) {
            coPresence += pairCoPresence(psilocybinActive, lsdActive, psilocybinLoad, lsdLoad);
            coPresence += pairCoPresence(psilocybinActive, dmtActive, psilocybinLoad, dmtLoad);
            coPresence += pairCoPresence(lsdActive, dmtActive, lsdLoad, dmtLoad);
            coPresence += pairCoPresence(psilocybinActive, hoMetActive, psilocybinLoad, hoMetLoad);
            coPresence += pairCoPresence(lsdActive, hoMetActive, lsdLoad, hoMetLoad);
            coPresence += pairCoPresence(dmtActive, hoMetActive, dmtLoad, hoMetLoad);
            coPresence += pairCoPresence(psilocybinActive, alLadActive, psilocybinLoad, alLadLoad);
            coPresence += pairCoPresence(lsdActive, alLadActive, lsdLoad, alLadLoad);
            coPresence += pairCoPresence(dmtActive, alLadActive, dmtLoad, alLadLoad);
            coPresence += pairCoPresence(hoMetActive, alLadActive, hoMetLoad, alLadLoad);
            coPresence += pairCoPresence(psilocybinActive, twoCeActive, psilocybinLoad, twoCeLoad);
            coPresence += pairCoPresence(lsdActive, twoCeActive, lsdLoad, twoCeLoad);
            coPresence += pairCoPresence(dmtActive, twoCeActive, dmtLoad, twoCeLoad);
            coPresence += pairCoPresence(hoMetActive, twoCeActive, hoMetLoad, twoCeLoad);
            coPresence += pairCoPresence(alLadActive, twoCeActive, alLadLoad, twoCeLoad);
            int extra = (hoMetActive ? 1 : 0) + (alLadActive ? 1 : 0) + (twoCeActive ? 1 : 0);
            int originalCount = activeCount - extra;
            coPresence /= Math.max(1.0D, originalCount - 1.0D + smoothstep(0.0D, 0.32D, hoMetLoad + alLadLoad + twoCeLoad));
        }

        return new PsychedelicBlendState(
                psilocybinActive,
                lsdActive,
                dmtActive,
                hoMetActive,
                alLadActive,
                twoCeActive,
                psilocybinLoad,
                lsdLoad,
                dmtLoad,
                hoMetLoad,
                alLadLoad,
                twoCeLoad,
                psilocybinWeight,
                lsdWeight,
                dmtWeight,
                hoMetWeight,
                alLadWeight,
                twoCeWeight,
                saturate(coPresence),
                activeCount
        );
    }

    public boolean blended() {
        return activeCount >= 2;
    }

    public double totalLoad() {
        return saturate(psilocybinLoad + lsdLoad + dmtLoad + hoMetLoad + alLadLoad + twoCeLoad);
    }

    public double combinedProgress(PsilocybinEffectState psilocybin, LsdEffectState lsd) {
        return combinedProgress(psilocybin, lsd, DmtEffectState.inactive());
    }

    public double combinedProgress(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt) {
        return combinedProgress(psilocybin, lsd, dmt, HoMetEffectState.inactive());
    }

    public double combinedProgress(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet) {
        return combinedProgress(psilocybin, lsd, dmt, hoMet, AlLadEffectState.inactive());
    }

    public double combinedProgress(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet, AlLadEffectState alLad) {
        return combinedProgress(psilocybin, lsd, dmt, hoMet, alLad, TwoCeEffectState.inactive());
    }

    public double combinedProgress(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet, AlLadEffectState alLad, TwoCeEffectState twoCe) {
        double total = 0.0D;
        double progress = 0.0D;
        if (psilocybinActive && psilocybin != null) {
            double weight = Math.max(ACTIVE_WEIGHT_FLOOR, psilocybinLoad) * (0.70D + psilocybinWeight * 0.30D);
            progress += psilocybin.progress() * weight;
            total += weight;
        }
        if (lsdActive && lsd != null) {
            double weight = Math.max(ACTIVE_WEIGHT_FLOOR, lsdLoad) * (0.70D + lsdWeight * 0.30D);
            progress += lsd.progress() * weight;
            total += weight;
        }
        if (dmtActive && dmt != null) {
            double weight = Math.max(ACTIVE_WEIGHT_FLOOR, dmtLoad) * (0.70D + dmtWeight * 0.30D);
            progress += dmt.progress() * weight;
            total += weight;
        }
        if (hoMetActive && hoMet != null && hoMet.active()) {
            double weight = Math.max(ACTIVE_WEIGHT_FLOOR, hoMetLoad) * (0.70D + hoMetWeight * 0.30D);
            progress += hoMet.progress() * weight;
            total += weight;
        }
        if (alLadActive && alLad != null && alLad.active()) {
            double weight = Math.max(ACTIVE_WEIGHT_FLOOR, alLadLoad) * (0.70D + alLadWeight * 0.30D);
            progress += alLad.progress() * weight;
            total += weight;
        }
        if (twoCeActive && twoCe != null && twoCe.active()) {
            double weight = Math.max(ACTIVE_WEIGHT_FLOOR, twoCeLoad) * (0.70D + twoCeWeight * 0.30D);
            progress += twoCe.progress() * weight;
            total += weight;
        }
        return total <= 0.0D ? 1.0D : saturate(progress / total);
    }

    public int combinedRemainingTicks(PsilocybinEffectState psilocybin, LsdEffectState lsd) {
        return combinedRemainingTicks(psilocybin, lsd, DmtEffectState.inactive());
    }

    public int combinedRemainingTicks(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt) {
        return combinedRemainingTicks(psilocybin, lsd, dmt, HoMetEffectState.inactive());
    }

    public int combinedRemainingTicks(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet) {
        return combinedRemainingTicks(psilocybin, lsd, dmt, hoMet, AlLadEffectState.inactive());
    }

    public int combinedRemainingTicks(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet, AlLadEffectState alLad) {
        return combinedRemainingTicks(psilocybin, lsd, dmt, hoMet, alLad, TwoCeEffectState.inactive());
    }

    public int combinedRemainingTicks(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet, AlLadEffectState alLad, TwoCeEffectState twoCe) {
        int remaining = 0;
        if (psilocybinActive && psilocybin != null) {
            remaining = Math.max(remaining, psilocybin.remainingTicks());
        }
        if (lsdActive && lsd != null) {
            remaining = Math.max(remaining, lsd.remainingTicks());
        }
        if (dmtActive && dmt != null) {
            remaining = Math.max(remaining, dmt.remainingTicks());
        }
        if (hoMetActive && hoMet != null && hoMet.active()) remaining = Math.max(remaining, hoMet.remainingTicks());
        if (alLadActive && alLad != null && alLad.active()) remaining = Math.max(remaining, alLad.remainingTicks());
        if (twoCeActive && twoCe != null && twoCe.active()) remaining = Math.max(remaining, twoCe.remainingTicks());
        return remaining;
    }

    public double hoMetContributionGain() { return contributionGain(hoMetActive, hoMetWeight); }
    public double alLadContributionGain() { return contributionGain(alLadActive, alLadWeight); }
    public double twoCeContributionGain() { return contributionGain(twoCeActive, twoCeWeight); }
    public double twoCeShaderGain() { return displayGain(twoCeActive, twoCeWeight, 0.14D, 0.14D, 0.68D); }
    public double alLadVisualGain() { return displayGain(alLadActive, alLadWeight, 0.16D, 0.12D, 0.66D); }
    public double alLadShaderGain() { return displayGain(alLadActive, alLadWeight, 0.14D, 0.14D, 0.68D); }
    public double alLadMotionGain() { return displayGain(alLadActive, alLadWeight, 0.10D, 0.13D, 0.72D); }
    public double alLadAudioGain() { return displayGain(alLadActive, alLadWeight, 0.08D, 0.10D, 0.76D); }
    public double alLadParticleGain() { return displayGain(alLadActive, alLadWeight, 0.10D, 0.12D, 0.72D); }
    public double hoMetVisualGain() { return displayGain(hoMetActive, hoMetWeight, 0.16D, 0.12D, 0.66D); }
    public double hoMetShaderGain() { return displayGain(hoMetActive, hoMetWeight, 0.14D, 0.14D, 0.68D); }
    public double hoMetMotionGain() { return displayGain(hoMetActive, hoMetWeight, 0.10D, 0.13D, 0.72D); }
    public double hoMetAudioGain() { return displayGain(hoMetActive, hoMetWeight, 0.08D, 0.10D, 0.76D); }
    public double hoMetParticleGain() { return displayGain(hoMetActive, hoMetWeight, 0.10D, 0.12D, 0.72D); }

    public double psilocybinContributionGain() {
        return contributionGain(psilocybinActive, psilocybinWeight);
    }

    public double lsdContributionGain() {
        return contributionGain(lsdActive, lsdWeight);
    }

    public double dmtContributionGain() {
        return contributionGain(dmtActive, dmtWeight);
    }

    public double psilocybinVisualGain() {
        return displayGain(psilocybinActive, psilocybinWeight, 0.16D, 0.12D, 0.66D);
    }

    public double lsdVisualGain() {
        return displayGain(lsdActive, lsdWeight, 0.16D, 0.12D, 0.66D);
    }

    public double dmtVisualGain() {
        return dmtDisplayGain(0.18D, 0.16D, 0.62D);
    }

    public double psilocybinShaderGain() {
        return displayGain(psilocybinActive, psilocybinWeight, 0.14D, 0.14D, 0.68D);
    }

    public double lsdShaderGain() {
        return displayGain(lsdActive, lsdWeight, 0.14D, 0.14D, 0.68D);
    }

    public double dmtShaderGain() {
        return dmtDisplayGain(0.18D, 0.18D, 0.60D);
    }

    public double psilocybinMotionGain() {
        return displayGain(psilocybinActive, psilocybinWeight, 0.10D, 0.13D, 0.72D);
    }

    public double lsdMotionGain() {
        return displayGain(lsdActive, lsdWeight, 0.10D, 0.13D, 0.72D);
    }

    public double dmtMotionGain() {
        return dmtDisplayGain(0.16D, 0.16D, 0.58D);
    }

    public double psilocybinAudioGain() {
        return displayGain(psilocybinActive, psilocybinWeight, 0.08D, 0.10D, 0.76D);
    }

    public double lsdAudioGain() {
        return displayGain(lsdActive, lsdWeight, 0.08D, 0.10D, 0.76D);
    }

    public double dmtAudioGain() {
        return dmtDisplayGain(0.12D, 0.14D, 0.66D);
    }

    public double psilocybinParticleGain() {
        return displayGain(psilocybinActive, psilocybinWeight, 0.10D, 0.12D, 0.72D);
    }

    public double lsdParticleGain() {
        return displayGain(lsdActive, lsdWeight, 0.10D, 0.12D, 0.72D);
    }

    public double dmtParticleGain() {
        return dmtDisplayGain(0.14D, 0.16D, 0.62D);
    }

    public double dmtDominance() {
        if (!dmtActive) {
            return 0.0D;
        }
        double loadGate = smoothstep(0.46D, 0.82D, dmtLoad);
        return saturate(loadGate * TrippyConfig.dmtBreakthroughDominanceWeight);
    }

    public double bridgeStrength() {
        if (!blended()) {
            return 0.0D;
        }
        int extras = (hoMetActive ? 1 : 0) + (alLadActive ? 1 : 0) + (twoCeActive ? 1 : 0);
        return saturate(coPresence * Math.sqrt(Math.max(0.0D, psilocybinLoad + lsdLoad + dmtLoad + hoMetLoad + alLadLoad + twoCeLoad) / (3.0D + extras)));
    }

    public double pairBridgeStrength(double firstLoad, double secondLoad) {
        if (firstLoad <= 0.0D || secondLoad <= 0.0D) {
            return 0.0D;
        }
        return saturate(pairCoPresence(true, true, firstLoad, secondLoad) * Math.sqrt(Math.max(0.0D, firstLoad) * Math.max(0.0D, secondLoad)));
    }

    private static double psilocybinLoad(PsilocybinEffectState state) {
        double safe = Math.max(1.0D, TrippyConfig.psilocybinMaxVisualIntensity);
        double intensity = saturate((state.intensity() * 0.58D + state.visualIntensity() * 0.42D) / safe);
        double dose = smoothstep(0.35D, 5.0D, state.activeDoseGrams());
        double trip = tripPresence(state.progress());
        return saturate(intensity * (0.34D + dose * 0.66D) * trip * TrippyConfig.psilocybinBlendWeight);
    }

    private static double lsdLoad(LsdEffectState state) {
        double safe = Math.max(1.0D, TrippyConfig.lsdMaxVisualIntensity);
        double intensity = saturate((state.intensity() * 0.52D + state.visualIntensity() * 0.48D) / safe);
        double dose = smoothstep(35.0D, 250.0D, state.activeDoseMicrograms());
        double tier = saturate(state.tier() / 5.0D);
        double trip = tripPresence(state.progress());
        return saturate(intensity * (0.30D + dose * 0.52D + tier * 0.18D) * trip * TrippyConfig.lsdBlendWeight);
    }

    private static double dmtLoad(DmtEffectState state) {
        double safe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
        double intensity = saturate((state.intensity() * 0.54D + state.visualIntensity() * 0.46D) / safe);
        double dose = smoothstep(0.25D, 3.4D, state.activeDoseUnits());
        double tier = saturate(state.tier() / 5.0D);
        double trip = tripPresence(state.progress());
        return saturate(intensity * (0.28D + dose * 0.46D + tier * 0.26D) * trip * TrippyConfig.dmtBlendWeight);
    }

    private static double tripPresence(double progress) {
        double wake = smoothstep(0.0D, 0.14D, progress);
        double tail = 1.0D - smoothstep(0.96D, 1.0D, progress) * 0.42D;
        return saturate(wake * tail);
    }

    private double contributionGain(boolean active, double weight) {
        if (!active) {
            return 0.0D;
        }
        if (!blended()) {
            return 1.0D;
        }
        return clamp(1.0D - coPresence * (0.040D + (1.0D - weight) * 0.100D), 0.84D, 1.0D);
    }

    private double displayGain(boolean active, double weight, double sharedBlend, double weakerBlend, double floor) {
        if (!active) {
            return 0.0D;
        }
        if (!blended()) {
            return 1.0D;
        }
        return clamp(1.0D - coPresence * (sharedBlend + (1.0D - weight) * weakerBlend), floor, 1.0D);
    }

    private double dmtDisplayGain(double sharedBlend, double weakerBlend, double floor) {
        if (!dmtActive) {
            return 0.0D;
        }
        double base = displayGain(true, dmtWeight, sharedBlend, weakerBlend, floor);
        if (!blended()) {
            return base;
        }
        return clamp(base + dmtDominance() * 0.18D, floor, 1.0D);
    }

    private static double pairCoPresence(boolean firstActive, boolean secondActive, double firstLoad, double secondLoad) {
        if (!firstActive || !secondActive) {
            return 0.0D;
        }
        double smaller = Math.min(firstLoad, secondLoad);
        double larger = Math.max(firstLoad, secondLoad);
        return smoothstep(0.025D, 0.32D, smaller) * (0.62D + smoothstep(0.12D, 0.72D, larger) * 0.38D);
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    private static double saturate(double value) {
        return clamp(value, 0.0D, 1.0D);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
