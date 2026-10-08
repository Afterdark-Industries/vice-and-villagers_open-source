package com.trippycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.dmt.DmtStage;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

public final class ClientEntityHallucinations {
    private ClientEntityHallucinations() {
    }

    public static void onPre(RenderLivingEvent.Pre<?, ?> event) {
        PsilocybinEffectState state = ClientExperienceStore.psilocybin();
        boolean psilocybinActive = state.active() && TrippyConfig.enablePsilocybinEffects && state.intensity() > 0.0D;
        LsdEffectState lsd = ClientExperienceStore.lsd();
        boolean lsdActive = lsd.active() && TrippyConfig.enableLsdEffects && TrippyConfig.lsdEntityEffects && lsd.entityTrails() + lsd.blockAutonomy() >= 0.08D;
        DmtEffectState dmt = ClientExperienceStore.dmt();
        boolean dmtActive = dmt.active() && dmt.hasAuthoredEffects() && TrippyConfig.enableDmtEffects
                && dmt.entityPresence() + dmt.encounter().entityFocus() + dmt.environmentalConsciousness()
                + dmt.alienShimmer() * 0.28D + dmt.spatialInstability() * 0.34D >= 0.10D;
        if (!psilocybinActive && !lsdActive && !dmtActive) {
            return;
        }
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(state, lsd, dmt, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        float time = gameTime;
        long entityHash = System.identityHashCode(event.getEntity()) & 0xFFFFL;
        float phase = (float) (entityHash / (double) 0xFFFFL * Math.PI * 2.0D);

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        if (psilocybinActive) {
            double gain = blend.psilocybinVisualGain();
            double breath = compressedPsilocybinStrength(state.organicBreathing() * gain);
            double melt = compressedPsilocybinStrength(state.surfaceMelting() * gain);
            double flowTime = PsilocybinPresentationClock.timeTicks(event.getPartialTick());
            double breathe = Math.sin(flowTime * 0.0055D * state.signature().breathingRhythm() + phase);
            double drift = Math.sin(flowTime * 0.0037D * state.signature().waveRhythm() + phase * 1.7D);
            // Living surfaces gently change proportions. No spin, levitation, rapid
            // jitter or negative scales, even after an oversized stacked amount.
            float width = (float) (1.0D + breathe * 0.055D * breath + drift * 0.015D * melt);
            float height = (float) (1.0D - breathe * 0.045D * breath + drift * 0.025D * melt);
            pose.scale(width, height, width);
        }
        if (lsdActive) {
            double gain = blend.lsdVisualGain();
            double energy = Math.log1p(Math.max(0.0D, lsd.blockAutonomy() * gain + lsd.realityFold() * gain * 0.45D));
            double safeScale = energy / (1.0D + energy);
            double flowTime = LsdPresentationClock.timeTicks(event.getPartialTick());
            double wave = Math.sin(flowTime * 0.0042D + phase * 1.6D);
            double fold = Math.cos(flowTime * 0.0031D + phase * 2.3D);
            // Electric, fluid proportions replace frame-fast rotations and snaps.
            // The entity stays where it belongs, with positive smooth scales.
            float sx = (float) (1.0D + wave * 0.075D * safeScale);
            float sy = (float) (1.0D - wave * 0.045D * safeScale + fold * 0.025D * safeScale);
            float sz = (float) (1.0D + fold * 0.055D * safeScale);
            pose.scale(sx, sy, sz);
        }
        if (dmtActive) {
            double gain = blend.dmtMotionGain();
            double safe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
            double presence = Math.min(1.45D, (dmt.entityPresence() / safe + dmt.encounter().entityFocus() * 0.74D + dmt.environmentalConsciousness() * 0.18D) * gain);
            double architecture = Math.min(1.15D, (dmt.impossibleArchitecture() + dmt.realityReplacement() * 0.42D) * gain);
            double symbol = Math.min(1.20D, (dmt.symbolLanguage() + dmt.symbolPressure() * 0.35D) * gain);
            double threshold = Math.min(0.72D, (dmt.alienShimmer() + dmt.spatialInstability() * 0.82D + dmt.edgeFlicker() * 0.46D) / safe * gain);
            double crossing = dmt.stage() == DmtStage.BREAKTHROUGH_TRANSITION ? Math.min(1.0D, dmt.breakthroughTransition() * gain) : 0.0D;
            double realm = dmt.stage() == DmtStage.POST_BREAKTHROUGH || dmt.stage() == DmtStage.RETURN
                    ? Math.min(1.0D, (dmt.realityReplacement() + dmt.impossibleArchitecture() * 0.34D) * gain)
                    : 0.0D;
            float gaze = (float) Math.sin(time * (0.060D + dmt.signature().entityHint() * 0.030D) + phase + dmt.signature().seed() * 0.000002D);
            float inspect = (float) Math.sin(time * 0.115D + phase * 2.2D + dmt.encounter().eventSeed() * 0.000001D);
            float unfold = (float) Math.sin(time * (0.095D + dmt.signature().symmetry() * 0.040D) + phase * 1.6D);
            float lift = (float) ((0.016D + threshold * 0.032D + presence * 0.050D + realm * 0.026D) * gaze);
            float yaw = (float) (gaze * (2.5D + threshold * 7.0D + presence * 12.0D + architecture * 8.5D + crossing * 9.0D));
            float roll = (float) (inspect * (2.0D + symbol * 7.0D + architecture * 5.5D + realm * 4.0D));
            float pitch = (float) (unfold * (threshold * 2.8D + crossing * 5.0D + realm * 3.0D));
            float sx = (float) (1.0D + threshold * 0.010D + architecture * 0.020D + realm * 0.020D + inspect * symbol * 0.018D);
            float sy = (float) (1.0D + presence * 0.034D + threshold * 0.012D + gaze * architecture * 0.024D + crossing * 0.018D);
            float sz = (float) (1.0D - architecture * 0.012D - realm * 0.018D + inspect * presence * 0.012D);
            pose.translate(0.0F, lift, 0.0F);
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.ZP.rotationDegrees(roll));
            pose.scale(sx, sy, sz);
        }
    }

    public static void onPost(RenderLivingEvent.Post<?, ?> event) {
        PsilocybinEffectState state = ClientExperienceStore.psilocybin();
        LsdEffectState lsd = ClientExperienceStore.lsd();
        boolean psilocybinActive = state.active() && TrippyConfig.enablePsilocybinEffects && state.intensity() > 0.0D;
        boolean lsdActive = lsd.active() && TrippyConfig.enableLsdEffects && TrippyConfig.lsdEntityEffects && lsd.entityTrails() + lsd.blockAutonomy() >= 0.08D;
        DmtEffectState dmt = ClientExperienceStore.dmt();
        boolean dmtActive = dmt.active() && dmt.hasAuthoredEffects() && TrippyConfig.enableDmtEffects
                && dmt.entityPresence() + dmt.encounter().entityFocus() + dmt.environmentalConsciousness()
                + dmt.alienShimmer() * 0.28D + dmt.spatialInstability() * 0.34D >= 0.10D;
        if (!psilocybinActive && !lsdActive && !dmtActive) {
            return;
        }
        event.getPoseStack().popPose();
    }

    private static double compressedPsilocybinStrength(double amplitude) {
        double strength = Math.log1p(Math.max(0.0D, amplitude));
        return strength / (1.0D + strength);
    }
}
