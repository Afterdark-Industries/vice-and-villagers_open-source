package com.trippycraft.substance.psilocybin;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;

import java.util.EnumMap;

public final class PsilocybinEffectProfile {
    private PsilocybinEffectProfile() {
    }

    public static void contribute(PsilocybinEffectState state, EnumMap<ExperienceChannel, Double> target, EnumMap<SubstanceFamily, Double> familyTarget) {
        if (!state.active() || !TrippyConfig.enablePsilocybinEffects) {
            return;
        }

        double intensity = state.intensity();
        double organic = state.environment().organicBlocks();
        double cave = state.environment().cave();
        double rain = state.environment().rain();
        double night = state.environment().night();
        double water = state.environment().water();

        familyTarget.merge(SubstanceFamily.PSYCHEDELIC, intensity * (1.05D + state.visualIntensity() * 0.42D), Double::sum);
        target.merge(ExperienceChannel.VISUAL_DRIFT, intensity * (0.56D + organic * 0.28D + water * 0.16D), Double::sum);
        target.merge(ExperienceChannel.GEOMETRY_MELT, state.surfaceMelting() * (0.74D + organic * 0.30D), Double::sum);
        target.merge(ExperienceChannel.PATTERNING, state.fungalOverlays() * (0.68D + cave * 0.24D + night * 0.16D), Double::sum);
        target.merge(ExperienceChannel.COLOR_BLOOM, state.visualIntensity() * (0.58D + state.colorWarmth() * 0.22D + rain * 0.12D), Double::sum);
        target.merge(ExperienceChannel.DEPTH_BREATHING, state.organicBreathing() * (0.92D + organic * 0.24D), Double::sum);
        target.merge(ExperienceChannel.TIME_DILATION, intensity * (0.26D + intensity / (1.0D + intensity) * 0.08D + water * 0.12D), Double::sum);
        target.merge(ExperienceChannel.EGO_DISSOLUTION, intensity * PsilocybinDoseCurve.smoothstep(3.0D, 5.0D, state.activeDoseGrams()) * 0.52D, Double::sum);
        target.merge(ExperienceChannel.BODY_LOAD, intensity * (0.12D + cave * 0.05D), Double::sum);
        target.merge(ExperienceChannel.EUPHORIA, intensity * (0.16D + state.signature().emotionalTone() * 0.08D + state.environment().sunriseSunset() * 0.10D), Double::sum);
        target.merge(ExperienceChannel.ANXIETY, intensity * (state.environment().hostileEntities() * 0.12D + state.environment().lowHealth() * 0.10D), Double::sum);
        target.merge(ExperienceChannel.RISK, intensity * PsilocybinDoseCurve.smoothstep(5.0D, 9.0D, state.activeDoseGrams()) * 0.08D, Double::sum);
    }
}
