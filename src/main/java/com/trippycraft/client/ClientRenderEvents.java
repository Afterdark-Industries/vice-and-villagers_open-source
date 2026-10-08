package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.SubstanceFamily;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholRecoveryTracker.RecoveryState;
import com.trippycraft.substance.cannabis.CannabisEffectState;
import com.trippycraft.substance.cocaine.CocaineEffectState;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.dmt.DmtPhase;
import com.trippycraft.substance.heroin.HeroinEffectState;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import com.trippycraft.substance.ketamine.KetamineMasking;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.mdma.MdmaEffectState;
import com.trippycraft.substance.mescaline.MescalineEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import com.trippycraft.substance.salvia.SalviaDoseCurve;
import com.trippycraft.substance.salvia.SalviaEffectState;
import com.trippycraft.substance.salvia.SalviaPhase;
import com.trippycraft.substance.twocb.TwoCbEffectState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.lang.reflect.Method;

public final class ClientRenderEvents {
    private static double smoothedDmtFovFactor = 1.0D;
    private static long smoothedDmtFovSeed = Long.MIN_VALUE;
    private static double cannabisYawLag;
    private static double cannabisPitchLag;
    private static float lastCannabisYaw = Float.NaN;
    private static float lastCannabisPitch = Float.NaN;
    private static float lastCocaineYaw = Float.NaN;
    private static float lastCocainePitch = Float.NaN;
    private static double ketamineYawLag;
    private static double ketaminePitchLag;
    private static float lastKetamineYaw = Float.NaN;
    private static float lastKetaminePitch = Float.NaN;
    private static long lastKetamineFovNanos = -1L;
    private static double smoothedKetamineFovFactor = 1.0D;
    private static double ketamineCameraPull;
    private static double ketamineRenderYaw;
    private static double ketamineRenderPitch;
    private static double ketamineSwayYaw;
    private static double ketamineSwayPitch;
    private static double ketamineSwayRoll;
    private static double ketaminePresentationSway;
    private static boolean ketamineRenderLagInitialized;
    private static CameraType previousKetamineCameraType;
    private static boolean ketamineForcedThirdPerson;
    private static Method cameraSetPositionMethod;
    private static boolean cameraSetPositionLookupAttempted;
    private static long lastCannabisFovNanos = -1L;
    private static double smoothedCannabisFovFactor = 1.0D;
    private static double cannabisRenderYaw;
    private static double cannabisRenderPitch;
    private static boolean cannabisRenderLagInitialized;
    private static double heroinYawLag;
    private static double heroinPitchLag;
    private static double heroinRenderYaw;
    private static double heroinRenderPitch;
    private static double heroinSwayYaw;
    private static double heroinSwayPitch;
    private static double heroinSwayRoll;
    private static float lastHeroinYaw = Float.NaN;
    private static float lastHeroinPitch = Float.NaN;
    private static boolean heroinRenderLagInitialized;
    private static int heroinStillTicks;
    private static double heroinNodAmount;
    private static double heroinWakeShake;
    private static double heroinWakeRequirement;
    private static int heroinWakeCooldownTicks;
    private static double heroinBlackoutAmount;
    private static int heroinWakeDirection;
    private static boolean wasVrCamera;
    private static VrCameraMotion vrCameraMotion = VrCameraMotion.IDENTITY;

    private ClientRenderEvents() {
    }

    public static double heroinNodAmount() {
        return clamp(heroinNodAmount);
    }

    public static double heroinBlackoutAmount() {
        return clamp(heroinBlackoutAmount);
    }

    public static double heroinInteractionYawOffset() {
        return VivecraftCompat.isVrActive() ? 0.0D : heroinSwayYaw;
    }

    public static double heroinInteractionPitchOffset() {
        return VivecraftCompat.isVrActive() ? 0.0D : heroinSwayPitch;
    }

    public static void registerHeroinLookInput(double yawDelta, double pitchDelta, HeroinEffectState heroin) {
        if (heroin == null || !heroin.active()) {
            heroinWakeShake = 0.0D;
            heroinWakeRequirement = 0.0D;
            heroinWakeCooldownTicks = 0;
            heroinWakeDirection = 0;
            return;
        }

        if (heroinWakeCooldownTicks > 0) {
            heroinWakeCooldownTicks--;
        }

        double dose = Math.max(heroin.activeDoseMilligrams(), heroin.targetDoseMilligrams());
        double doseLoad = smoothstep(25.0D, 500.0D, dose);
        double nodDrive = clamp((heroin.nodding() * 0.48D + heroin.microSleep() * 0.72D + heroin.sedation() * 0.18D) / 7.4D
                + doseLoad * 0.20D);
        double shake = Math.abs(yawDelta) * 0.92D + Math.abs(pitchDelta) * 0.76D;
        double dominantDelta = Math.abs(yawDelta) >= Math.abs(pitchDelta) * 0.90D ? yawDelta : pitchDelta * 0.90D;
        double strokeThreshold = mix(0.90D, 0.24D, clamp(nodDrive + heroinNodAmount * 0.44D));
        if (heroinNodAmount > 0.06D) {
            heroinWakeRequirement = Mth.clamp(mix(5.0D, 9.5D, doseLoad * 0.42D + nodDrive * 0.70D + heroinNodAmount * 0.24D), 5.0D, 9.5D);
            heroinWakeShake *= 0.94D;
            if (shake >= strokeThreshold) {
                int direction = dominantDelta >= 0.0D ? 1 : -1;
                double stroke = Math.min(1.75D, 0.72D + shake * 0.22D);
                if (direction != heroinWakeDirection) {
                    heroinWakeShake += stroke;
                    heroinWakeDirection = direction;
                } else {
                    heroinWakeShake += Math.min(0.24D, shake * 0.05D);
                }
            } else if (shake < strokeThreshold * 0.55D) {
                heroinWakeDirection = 0;
            }
        } else {
            heroinWakeRequirement = Mth.clamp(mix(5.0D, 9.5D, doseLoad * 0.42D + nodDrive * 0.70D), 5.0D, 9.5D);
            heroinWakeShake *= 0.74D;
            heroinWakeDirection = 0;
        }
    }

    public static void updateVrPresentation(Camera camera, float partialTick) {
        if (!VivecraftCompat.isVrWorldPass() || !VivecraftCompat.isFirstWorldPass()) {
            return;
        }
        // XRCamera bypasses NeoForge's ComputeCameraAngles callback. Reuse the
        // presentation calculation locally so nod/blackout still progress. This
        // event is never posted, and the VR path below never applies its angles.
        onCameraAngles(new ViewportEvent.ComputeCameraAngles(
                camera, partialTick, camera.getYRot(), camera.getXRot(), 0.0F));
        ViewportEvent.ComputeFov fov = new ViewportEvent.ComputeFov(
                Minecraft.getInstance().gameRenderer, camera, partialTick, 90.0D, true);
        computeFov(fov);
        vrCameraMotion = new VrCameraMotion(vrCameraMotion.yawDegrees(), vrCameraMotion.pitchDegrees(),
                vrCameraMotion.rollDegrees(), (float) (fov.getFOV() / 90.0D), vrCameraMotion.translationWorld());
    }

    /** Shared artistic view motion; the stereo composite applies it once, without changing tracked poses. */
    public static VrCameraMotion vrCameraMotion() {
        return VivecraftCompat.isVrActive() ? vrCameraMotion : VrCameraMotion.IDENTITY;
    }

    public record VrCameraMotion(float yawDegrees, float pitchDegrees, float rollDegrees,
                                float fovFactor, Vec3 translationWorld) {
        private static final VrCameraMotion IDENTITY = new VrCameraMotion(0.0F, 0.0F, 0.0F, 1.0F, Vec3.ZERO);
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        boolean vr = VivecraftCompat.isVrActive();
        if (vr != wasVrCamera) {
            // Switching between a tracked headset and the desktop camera must not
            // carry lag or a forced third-person presentation across modes.
            resetCameraMotion();
            wasVrCamera = vr;
        }
        if (vr && (!VivecraftCompat.isVrWorldPass() || !VivecraftCompat.isFirstWorldPass())) {
            return;
        }
        ClientEffectSample sample = ClientEffectGraph.sample((float) event.getPartialTick());
        CannabisEffectState cannabis = ClientExperienceStore.cannabis();
        CocaineEffectState cocaine = ClientExperienceStore.cocaine();
        KetamineEffectState ketamine = ClientExperienceStore.ketamine();
        HeroinEffectState heroin = ClientExperienceStore.heroin();
        MescalineEffectState mescaline = ClientExperienceStore.mescaline();
        var heroinRecovery = ClientExperienceStore.heroinRecovery();
        double opioidWithdrawalShake = heroinRecovery.withdrawalActive() ? heroinRecovery.withdrawalSeverity() : 0.0D;
        if (!sample.visible() && !cannabis.active() && !cocaine.active() && !ketamine.active() && !heroin.active() && !mescaline.active() && opioidWithdrawalShake <= 0.01D) {
            vrCameraMotion = VrCameraMotion.IDENTITY;
            restoreKetamineCameraType();
            cannabisYawLag = 0.0D;
            cannabisPitchLag = 0.0D;
            ketamineYawLag = 0.0D;
            ketaminePitchLag = 0.0D;
            heroinYawLag = 0.0D;
            heroinPitchLag = 0.0D;
            ketamineCameraPull = 0.0D;
            lastCannabisYaw = Float.NaN;
            lastCannabisPitch = Float.NaN;
            lastCocaineYaw = Float.NaN;
            lastCocainePitch = Float.NaN;
            lastKetamineYaw = Float.NaN;
            lastKetaminePitch = Float.NaN;
            lastHeroinYaw = Float.NaN;
            lastHeroinPitch = Float.NaN;
            lastCannabisFovNanos = -1L;
            smoothedCannabisFovFactor = 1.0D;
            lastKetamineFovNanos = -1L;
            smoothedKetamineFovFactor = 1.0D;
            cannabisRenderLagInitialized = false;
            cannabisRenderYaw = 0.0D;
            cannabisRenderPitch = 0.0D;
            ketamineRenderLagInitialized = false;
            ketamineRenderYaw = 0.0D;
            ketamineRenderPitch = 0.0D;
            ketamineSwayYaw = 0.0D;
            ketamineSwayPitch = 0.0D;
            ketamineSwayRoll = 0.0D;
            ketaminePresentationSway = 0.0D;
            heroinRenderLagInitialized = false;
            heroinRenderYaw = 0.0D;
            heroinRenderPitch = 0.0D;
            heroinSwayYaw = 0.0D;
            heroinSwayPitch = 0.0D;
            heroinSwayRoll = 0.0D;
            heroinStillTicks = 0;
            heroinNodAmount = 0.0D;
            heroinWakeShake = 0.0D;
            heroinWakeRequirement = 0.0D;
            heroinWakeCooldownTicks = 0;
            heroinBlackoutAmount = 0.0D;
            heroinWakeDirection = 0;
            return;
        }

        float time = sample.time();
        ClientRenderSignature signature = sample.signature();
        double meltWave = Math.sin(time * (0.038D + signature.organicFlow() * 0.010D)) * sample.geometryMelt();
        double driftWave = Math.sin(time * (0.064D + signature.motorLooseness() * 0.012D) + 1.7D) * sample.visualDrift();
        double bodyWave = Math.sin(time * (0.024D - signature.narrowing() * 0.004D) + 2.4D) * sample.bodyLoad();
        double motorWave = Math.sin(time * (0.38D + signature.edgeSharpness() * 0.34D)) * sample.motorNoise();
        double focusJitter = Math.sin(time * 0.91D + 0.6D) * sample.stimulation() * signature.edgeSharpness();
        double dissociationWave = Math.sin(time * (0.017D + signature.temporalSlip() * 0.010D) + 4.2D) * sample.dissociation();

        float roll = (float) (meltWave * (1.35D + signature.organicFlow() * 0.45D)
                + driftWave * (0.60D + signature.motorLooseness() * 0.28D)
                + dissociationWave * (1.05D + signature.depthSeparation() * 0.46D)
                + motorWave * (0.20D + signature.edgeSharpness() * 0.34D));
        float yaw = (float) (Math.sin(time * 0.033D) * sample.visualDrift() * (0.30D + signature.motorLooseness() * 0.18D)
                + motorWave * 0.08D
                + focusJitter * 0.12D);
        float pitch = (float) (bodyWave * (0.24D + signature.narrowing() * 0.18D)
                + Math.sin(time * 0.051D + 0.8D) * sample.depthBreathing() * (0.24D + signature.depthSeparation() * 0.12D)
                - focusJitter * 0.08D);
        float genericGain = (float) ClientEffectGraph.genericPresentationGain(sample);
        roll *= genericGain;
        yaw *= genericGain;
        pitch *= genericGain;

        PsilocybinEffectState psilocybin = ClientExperienceStore.psilocybin();
        LsdEffectState lsd = ClientExperienceStore.lsd();
        DmtEffectState dmt = ClientExperienceStore.dmt();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(psilocybin, lsd, dmt, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        boolean psilocybinEnabled = psilocybin.active() && TrippyConfig.enablePsilocybinEffects;
        boolean lsdEnabled = lsd.active() && TrippyConfig.enableLsdEffects;
        boolean dmtEnabled = dmt.active() && dmt.hasAuthoredEffects() && TrippyConfig.enableDmtEffects;
        if (psilocybinEnabled || lsdEnabled || dmtEnabled) {
            double psilocybinMotion = psilocybinEnabled ? psilocybinGenericMotion(psilocybin) : 0.0D;
            double dmtMotion = dmtEnabled ? dmtGenericMotion(dmt) : 0.0D;
            if (blend.blended()) {
                double psilocybinGain = blend.psilocybinMotionGain();
                double lsdGain = blend.lsdMotionGain();
                double dmtGain = blend.dmtMotionGain();
                roll *= (float) (psilocybinMotion * 1.00D * blend.psilocybinWeight() * psilocybinGain + dmtMotion * 0.42D * blend.dmtWeight() * dmtGain);
                yaw *= (float) (psilocybinMotion * 1.16D * blend.psilocybinWeight() * psilocybinGain + dmtMotion * 0.54D * blend.dmtWeight() * dmtGain);
                pitch *= (float) (psilocybinMotion * 1.10D * blend.psilocybinWeight() * psilocybinGain + dmtMotion * 0.48D * blend.dmtWeight() * dmtGain);
            } else if (psilocybinEnabled) {
                roll *= (float) psilocybinMotion;
                yaw *= (float) (psilocybinMotion * 1.16D);
                pitch *= (float) (psilocybinMotion * 1.10D);
            } else if (lsdEnabled) {
                roll = 0.0F;
                yaw = 0.0F;
                pitch = 0.0F;
            } else {
                roll *= (float) (dmtMotion * 0.42D);
                yaw *= (float) (dmtMotion * 0.54D);
                pitch *= (float) (dmtMotion * 0.48D);
            }
        }
        // LSD deformation and continuous colour live on surfaces; the viewpoint stays steady.
        if (dmt.active() && dmt.hasAuthoredEffects() && TrippyConfig.enableDmtEffects && !TrippyConfig.dmtAccessibilityMode) {
            var scaffold = dmt.fractalScaffold();
            double launch = smoothstep(0.0D, 0.18D, dmt.progress()) * (1.0D - smoothstep(0.92D, 1.0D, dmt.progress()));
            double camera = Math.min(1.10D, scaffold.animatedFractalPresence() * 0.24D
                    + scaffold.staticAnimatedCoupling() * 0.34D
                    + scaffold.alternateWorldReadiness() * 0.30D
                    + scaffold.dimensionalDepth() * 0.22D)
                    * launch
                    * blend.dmtMotionGain();
            double seedPhase = dmt.signature().seed() * 0.000009D;
            double tunnel = Math.sin(time * (0.16D + dmt.signature().tunnelSpeed() * 0.055D) + seedPhase);
            double snap = Math.sin(time * (0.52D + dmt.signature().symmetry() * 0.16D) + dmt.signature().hyperspaceStyle());
            double mandala = Math.sin(time * (0.083D + dmt.signature().mandalaDensity() * 0.050D) - seedPhase * 0.7D);
            roll += (float) (tunnel * camera * 0.58D + snap * scaffold.staticAnimatedCoupling() * 0.030D);
            yaw += (float) (mandala * camera * 0.22D + tunnel * scaffold.dimensionalDepth() * 0.055D);
            pitch += (float) (Math.cos(time * 0.14D + seedPhase) * camera * 0.22D - snap * camera * 0.070D);
        }
        if (mescaline.active()) {
            double motion = TrippyClientConfig.motionReduction ? 0.36D : 1.0D;
            double body = clamp(mescaline.bodyWarmth() * 0.78D + mescaline.nauseaWave() * 0.38D);
            double seedPhase = (mescaline.signature().seed() & 4095L) * 0.0013D;
            double longBreath = Math.sin(time * 0.0062D * mescaline.signature().breathingRhythm() + seedPhase);
            double sideBreath = Math.sin(time * 0.0041D + seedPhase * 1.7D + 1.3D);
            double nausea = Math.sin(time * 0.019D + seedPhase) * mescaline.nauseaWave();
            roll += (float) ((longBreath * 0.22D + sideBreath * 0.12D) * body * motion);
            yaw += (float) ((sideBreath * 0.095D + nausea * 0.07D) * body * motion);
            pitch += (float) ((longBreath * 0.075D - nausea * 0.10D) * body * motion);
        }
            SalviaEffectState salvia = ClientExperienceStore.salvia();
            if (salvia.active() && TrippyConfig.enableSalviaEffects && !TrippyConfig.salviaAccessibilityMode) {
                double visual = salviaVisualLoad(salvia);
                double motion = TrippyClientConfig.motionReduction ? 0.52D : 1.0D;
                double camera = (0.10D + visual * 0.28D + salvia.signature().bodyTorque() * 0.10D) * motion;
                double lateral = salvia.signature().bodyTorque() >= 0.5D ? 1.0D : -1.0D;
                double pageTurn = Math.sin(time * (0.060D + salvia.signature().recursionBias() * 0.028D) + salvia.signature().seed() * 0.000009D);
                double slot = Math.sin(time * (0.028D + salvia.signature().anchorBias() * 0.018D) + salvia.signature().seed() * 0.000005D + 1.3D);
                double aftershock = Math.sin(time * (0.12D + salvia.signature().aftershock() * 0.08D) + 0.5D);
                double recursion = salvia.phase() == SalviaPhase.RECURSION ? 1.0D : 0.58D;
                double shakeout = salvia.phase() == SalviaPhase.SHAKEOUT ? 1.0D : 0.0D;
                roll += (float) ((pageTurn * (salvia.anchorSlip() * 0.54D + salvia.loopSeam() * 0.44D) * recursion
                    + aftershock * salvia.reentryConfusion() * 0.24D) * camera * (0.72D + salvia.signature().bodyTorque() * 0.42D));
                yaw += (float) ((lateral * slot * (salvia.bodyPull() * 0.60D + salvia.sceneReplacement() * 0.26D)
                    + aftershock * shakeout * 0.12D) * camera * 0.42D);
                pitch += (float) ((Math.sin(time * 0.034D + salvia.signature().sceneBias() * 2.7D) * (salvia.sceneReplacement() * 0.24D + salvia.observerPressure() * 0.28D)
                    - aftershock * salvia.reentryConfusion() * 0.18D) * camera * 0.36D);
            }
        AlcoholEffectState alcohol = ClientExperienceStore.alcohol();
        MdmaEffectState mdma = ClientExperienceStore.mdma();
        RecoveryState alcoholRecovery = ClientExperienceStore.alcoholRecovery();
        double hangover = alcoholHangoverLoad(alcoholRecovery, alcohol);
        double withdrawal = alcoholWithdrawalLoad(alcoholRecovery);
        if ((alcohol.active() || hangover > 0.01D || withdrawal > 0.01D) && TrippyConfig.enableAlcoholEffects) {
            double gain = Math.max(alcoholMotionGain(alcohol), hangover * 0.38D + withdrawal * 0.52D);
            double heavyHead = smoothstep(12.0D, 18.0D, alcohol.activeStandardDrinks());
            heavyHead = Math.max(heavyHead, hangover * 0.18D + withdrawal * 0.28D);
            if (TrippyConfig.alcoholAccessibilityMode || TrippyClientConfig.motionReduction) {
                heavyHead *= 0.36D;
            }
            double motorLoad = Math.max(alcoholLoad(alcohol, alcohol.motorNoise()), hangover * 0.20D + withdrawal * 0.42D);
            double sedationLoad = Math.max(alcoholLoad(alcohol, alcohol.sedation()), hangover * 0.36D + withdrawal * 0.26D);
            double wobbleSeed = alcohol.active()
                    ? alcohol.activeStandardDrinks() * 7.0D + alcohol.ageTicks() * 0.35D
                    : alcoholRecovery.dependencyPhase() * 0.7D + alcoholRecovery.soberTicks() * 0.008D;
            double wobbleTime = time + wobbleSeed;
            double slowRoll = Math.sin(wobbleTime * 0.028D) * 0.72D + Math.sin(wobbleTime * 0.015D + 1.9D) * 0.46D;
            double correction = Math.sin(wobbleTime * (0.19D + motorLoad * 0.10D) + 0.7D);
            double nod = Math.sin(wobbleTime * 0.021D + 2.6D) * 0.70D + Math.sin(wobbleTime * 0.052D) * 0.18D;
            double visualSlip = Math.sin(wobbleTime * 0.038D + 1.2D);
            double headLoll = Math.sin(wobbleTime * 0.012D + 1.6D) * 0.62D
                    + Math.sin(wobbleTime * 0.019D + 4.1D) * 0.38D;
            double failedCorrectionWindow = Math.pow(Math.max(0.0D, Math.sin(wobbleTime * (0.041D + motorLoad * 0.020D) + 0.9D)), 7.0D) * heavyHead;
            double failedCorrection = Math.sin(wobbleTime * (0.16D + motorLoad * 0.12D) + alcohol.activeStandardDrinks() * 0.31D) * failedCorrectionWindow;
            double chinDrop = heavyHead * sedationLoad * (0.18D + failedCorrectionWindow * 0.18D);
                double irritation = Math.sin(wobbleTime * (0.92D + withdrawal * 0.48D) + 0.3D) * (hangover * 0.045D + withdrawal * 0.070D);
            roll += (float) (slowRoll * gain * 1.45D
                    + correction * motorLoad * gain * 0.34D
                    + headLoll * heavyHead * (1.30D + gain * 2.40D)
                    + failedCorrection * (0.70D + heavyHead * 1.45D)
                    + irritation);
            yaw += (float) (visualSlip * gain * 0.18D
                    + correction * gain * 0.060D
                    + Math.sin(wobbleTime * 0.027D + 0.4D) * heavyHead * gain * (0.22D + motorLoad * 0.48D)
                    + failedCorrection * 0.32D
                    + irritation * 0.38D);
            pitch += (float) (nod * gain * 0.14D
                    - sedationLoad * gain * 0.070D
                    + Math.sin(wobbleTime * 0.016D + 2.0D) * heavyHead * gain * (0.20D + sedationLoad * 0.30D)
                    - chinDrop
                    + failedCorrection * 0.18D
                    - irritation * 0.24D);
        }
        if (cannabis.active()) {
            double motion = cannabisMotionGain(cannabis);
            double thcDose = Math.max(cannabis.activeThcMilligrams(), cannabis.targetThcMilligrams());
            double cameraLagDose = thcDose < 100.0D
                    ? 0.0D
                    : 0.16D + smoothstep(100.0D, 1000.0D, thcDose) * 0.84D;
            double mixedPresence = cannabisMixedPresence(sample.familyBlend());
            double mixedLag = clamp(mixedPresence * (0.34D + motion * 0.44D));
            // Mescaline already supplies slow continuous movement. Suppress THC's
            // frame-lag style response in this pairing so it adds dreaminess and
            // persistence without turning camera motion into visible steps.
            double mescalineBlend = mescaline.active() ? clamp(mescaline.visualPresence()) : 0.0D;
            cameraLagDose *= mix(1.0D, 0.30D, mescalineBlend);
            mixedLag *= mix(1.0D, 0.18D, mescalineBlend);
            double overdrive = smoothstep(12.0D, 24.0D, cannabis.doseScale());
            double temporal = clamp(cannabis.temporalDrag() * 0.18D) * (0.48D + motion * 0.72D + overdrive * 0.34D + mixedLag * 0.34D);
            double sticky = clamp(cannabis.thoughtStickiness() * 0.16D) * (0.46D + motion * 0.78D + overdrive * 0.40D + mixedLag * 0.36D);
            temporal *= mix(1.0D, 0.44D, mescalineBlend);
            sticky *= mix(1.0D, 0.58D, mescalineBlend);
            double breathing = Math.sin(time * (0.0035D + cannabis.breathingPulse() * 0.00022D) + cannabis.doseScale() * 0.10D);
            double microSway = Math.sin(time * 0.0028D + 1.4D);
            float cameraYaw = event.getYaw();
            float cameraPitch = event.getPitch();
            if (!Float.isFinite(lastCannabisYaw) || !Float.isFinite(lastCannabisPitch)) {
                lastCannabisYaw = cameraYaw;
                lastCannabisPitch = cameraPitch;
            }
            if (!cannabisRenderLagInitialized) {
                cannabisRenderLagInitialized = true;
                cannabisRenderYaw = cameraYaw;
                cannabisRenderPitch = cameraPitch;
            }
            double yawDelta = Mth.wrapDegrees(cameraYaw - lastCannabisYaw);
            double pitchDelta = cameraPitch - lastCannabisPitch;
                double lagStrength = cameraLagDose * (0.42D + motion * 0.36D + temporal * 0.54D + sticky * 0.52D + overdrive * 0.48D + mixedLag * 0.80D);
            double settle = TrippyClientConfig.motionReduction ? 0.13D : mix(0.075D, 0.032D, clamp(overdrive + mixedLag * 0.62D));
            settle = Math.max(settle, mescalineBlend * 0.090D);
            cannabisYawLag += (-yawDelta * lagStrength - cannabisYawLag) * settle;
            cannabisPitchLag += (-pitchDelta * (lagStrength * 0.88D) - cannabisPitchLag) * settle;
                double directFollow = TrippyClientConfig.motionReduction
                    ? mix(0.16D, 0.11D, cameraLagDose)
                    : mix(0.060D, 0.014D, clamp(overdrive + mixedLag * 0.72D)) * (1.0D - cameraLagDose * 0.78D);
            directFollow = Math.max(directFollow, mescalineBlend * 0.072D);
                double renderLagBlend = clamp((0.46D + motion * 0.24D + temporal * 0.46D + sticky * 0.42D + overdrive * 0.58D + mixedLag * 0.70D) * cameraLagDose);
            renderLagBlend *= mix(1.0D, 0.56D, mescalineBlend);
            cannabisRenderYaw += Mth.wrapDegrees(cameraYaw - (float) cannabisRenderYaw) * directFollow;
            cannabisRenderPitch += (cameraPitch - cannabisRenderPitch) * (directFollow * 0.92D);
            double delayedYaw = Mth.wrapDegrees((float) (cannabisRenderYaw - cameraYaw)) * renderLagBlend;
            double delayedPitch = (cannabisRenderPitch - cameraPitch) * renderLagBlend;

            roll *= (float) (1.0D - motion * 0.22D);
            yaw *= (float) (1.0D - motion * 0.16D);
            pitch *= (float) (1.0D - motion * 0.20D);
            roll += (float) (breathing * motion * (0.055D + temporal * 0.14D));
            yaw += (float) (cannabisYawLag * (0.52D + sticky * 0.32D + overdrive * 0.26D + mixedLag * 0.36D) + delayedYaw * (0.84D + overdrive * 0.34D + mixedLag * 0.42D) + microSway * motion * 0.020D);
            pitch += (float) (cannabisPitchLag * (0.46D + sticky * 0.28D + overdrive * 0.22D + mixedLag * 0.32D) + delayedPitch * (0.80D + overdrive * 0.30D + mixedLag * 0.38D) + breathing * motion * 0.024D);

            lastCannabisYaw = cameraYaw;
            lastCannabisPitch = cameraPitch;
        } else {
            cannabisYawLag *= 0.82D;
            cannabisPitchLag *= 0.82D;
            lastCannabisYaw = event.getYaw();
            lastCannabisPitch = event.getPitch();
            cannabisRenderLagInitialized = false;
            cannabisRenderYaw = event.getYaw();
            cannabisRenderPitch = event.getPitch();
        }

        if (cocaine.active()) {
            float cameraYaw = event.getYaw();
            float cameraPitch = event.getPitch();
            if (!Float.isFinite(lastCocaineYaw) || !Float.isFinite(lastCocainePitch)) {
                lastCocaineYaw = cameraYaw;
                lastCocainePitch = cameraPitch;
            }
            double yawDelta = Mth.wrapDegrees(cameraYaw - lastCocaineYaw);
            double pitchDelta = cameraPitch - lastCocainePitch;
            double drive = clamp(cocaine.locomotorDrive() * 0.060D + cocaine.inputSharpness() * 0.045D + cocaine.clarityLift() * 0.018D);
            double jitter = clamp(cocaine.jitter() * 0.055D + cocaine.overcorrection() * 0.045D + cocaine.microFlicker() * 0.018D);
            double focus = clamp(cocaine.tunnelFocus() * 0.060D + cocaine.clarityLift() * 0.038D + cocaine.edgeShimmer() * 0.016D);
            double crash = clamp(cocaine.crashDrag() * 0.075D + cocaine.crashDesaturation() * 0.30D);
            double cocaineTime = time + cocaine.ageTicks() * 0.031D + cocaine.activeDoseGrams() * 0.45D;
            double microSnap = Math.sin(cocaineTime * (0.92D + jitter * 0.44D)) * (0.20D + focus * 0.14D);
            double overstep = Math.sin(cocaineTime * (0.38D + drive * 0.18D) + 1.4D) * (0.26D + jitter * 0.18D);
            double sharpRoll = Math.sin(cocaineTime * (1.45D + jitter * 0.52D) + 0.3D) * (0.10D + jitter * 0.10D);
            double sensitivity = clamp(cocaine.inputSharpness() * 0.050D + cocaine.locomotorDrive() * 0.026D + cocaine.clarityLift() * 0.018D - crash * 0.045D);
            roll += (float) (sharpRoll * (0.30D + drive * 0.22D) - crash * 0.08D);
            yaw += (float) (microSnap * (0.18D + focus * 0.20D) + overstep * (0.10D + drive * 0.08D) + yawDelta * sensitivity * (0.28D + focus * 0.18D));
            pitch += (float) (overstep * (0.08D + drive * 0.06D) - focus * 0.06D - crash * 0.04D + pitchDelta * sensitivity * (0.24D + focus * 0.14D));
            lastCocaineYaw = cameraYaw;
            lastCocainePitch = cameraPitch;
        } else {
            lastCocaineYaw = event.getYaw();
            lastCocainePitch = event.getPitch();
        }

        if (ketamine.active()) {
            double motion = ketamineMotionGain(ketamine);
            double ketamineDose = Math.max(ketamine.activeDoseMilligrams(), ketamine.targetDoseMilligrams());
            double dreamDose = smoothstep(50.0D, 400.0D, ketamineDose);
            float cameraYaw = event.getYaw();
            float cameraPitch = event.getPitch();
            if (!Float.isFinite(lastKetamineYaw) || !Float.isFinite(lastKetaminePitch)) {
                lastKetamineYaw = cameraYaw;
                lastKetaminePitch = cameraPitch;
            }
            if (!ketamineRenderLagInitialized) {
                ketamineRenderLagInitialized = true;
                ketamineRenderYaw = cameraYaw;
                ketamineRenderPitch = cameraPitch;
            }
            double yawDelta = Mth.wrapDegrees(cameraYaw - lastKetamineYaw);
            double pitchDelta = cameraPitch - lastKetaminePitch;
            double lag = clamp(ketamine.cameraLag() / 2.8D) * (0.32D + motion * 0.36D);
            double disconnect = clamp(ketamine.motorDisconnect() / 2.6D) * (0.30D + motion * 0.34D);
            double depth = clamp(ketamine.depthMismatch() / 2.4D) * (0.36D + motion * 0.42D);
            double floatiness = clamp(ketamine.bodyFloat() / 2.2D) * (0.38D + motion * 0.40D);
            double drift = clamp(ketamine.gravityDrift() / 2.3D) * (0.32D + motion * 0.36D);
            double slip = clamp(ketamine.timeSlip() / 2.6D) * (0.24D + motion * 0.24D);
            double fade = clamp(ketamine.uiFade() / 2.2D);
            double outOfBody = ketamineOutOfBodyPull(ketamine);
            double settle = TrippyClientConfig.motionReduction ? 0.10D : mix(0.045D, 0.006D, clamp(lag + disconnect * 0.54D + floatiness * 0.48D + drift * 0.42D + dreamDose * 0.24D));
            ketamineYawLag += (-yawDelta * (0.30D + lag * 0.24D + disconnect * 0.12D) - ketamineYawLag) * settle;
            ketaminePitchLag += (-pitchDelta * (0.28D + lag * 0.22D + disconnect * 0.10D) - ketaminePitchLag) * settle;
            double follow = TrippyClientConfig.motionReduction ? 0.13D : mix(0.040D, 0.006D, clamp(lag + disconnect * 0.62D + depth * 0.32D + floatiness * 0.44D + dreamDose * 0.26D));
            ketamineRenderYaw += Mth.wrapDegrees(cameraYaw - (float) ketamineRenderYaw) * follow;
            ketamineRenderPitch += (cameraPitch - ketamineRenderPitch) * (follow * 0.90D);
            double delayedYaw = Mth.wrapDegrees((float) (ketamineRenderYaw - cameraYaw)) * clamp(0.26D + lag * 0.20D + disconnect * 0.28D + depth * 0.22D + floatiness * 0.22D + dreamDose * 0.10D);
            double delayedPitch = (ketamineRenderPitch - cameraPitch) * clamp(0.24D + lag * 0.18D + disconnect * 0.24D + depth * 0.18D + floatiness * 0.18D + dreamDose * 0.08D);
            double ketamineTime = time * 0.18D + ketamine.ageTicks() * 0.0018D + ketamine.activeDoseMilligrams() * 0.0032D;
            double slowDrift = Math.sin(ketamineTime * (0.0032D + drift * 0.0014D) + 1.8D) * (0.16D + drift * 0.34D + floatiness * 0.24D + dreamDose * 0.06D);
            double horizonSlip = Math.sin(ketamineTime * (0.0022D + slip * 0.0010D) + 2.1D) * (0.14D + depth * 0.32D + floatiness * 0.20D + outOfBody * 0.12D);
            double floatRoll = Math.sin(ketamineTime * (0.0020D + floatiness * 0.0012D) + 1.3D) * (0.26D + floatiness * 0.84D + drift * 0.30D + dreamDose * 0.08D);
            double bodyDrift = Math.cos(ketamineTime * (0.0026D + drift * 0.0012D) + 0.6D) * (0.12D + fade * 0.18D + depth * 0.16D + outOfBody * 0.14D);
            roll *= (float) (1.0D - motion * 0.06D);
            yaw *= (float) (1.0D - motion * 0.04D);
            pitch *= (float) (1.0D - motion * 0.05D);
                double dreamSmoothing = TrippyClientConfig.motionReduction
                    ? 0.18D
                    : mix(0.090D, 0.035D, clamp(dreamDose + floatiness * 0.24D + depth * 0.20D));
                double targetRoll = floatRoll + slowDrift * 0.28D + bodyDrift * 0.20D - fade * 0.04D;
                double targetYaw = ketamineYawLag * (0.36D + disconnect * 0.14D + floatiness * 0.22D)
                    + delayedYaw * (0.70D + depth * 0.30D + floatiness * 0.18D)
                    + horizonSlip
                    + slowDrift * 0.14D;
                double targetPitch = ketaminePitchLag * (0.34D + disconnect * 0.12D + floatiness * 0.20D)
                    + delayedPitch * (0.66D + depth * 0.26D + floatiness * 0.16D)
                    + bodyDrift
                    + slowDrift * 0.10D
                    - fade * 0.03D;
                ketamineSwayRoll += (targetRoll - ketamineSwayRoll) * dreamSmoothing;
                ketamineSwayYaw += (targetYaw - ketamineSwayYaw) * dreamSmoothing;
                ketamineSwayPitch += (targetPitch - ketamineSwayPitch) * dreamSmoothing;
                roll += (float) ketamineSwayRoll;
                yaw += (float) ketamineSwayYaw;
                pitch += (float) ketamineSwayPitch;
            lastKetamineYaw = cameraYaw;
            lastKetaminePitch = cameraPitch;
        } else {
            restoreKetamineCameraType();
            ketamineYawLag *= 0.82D;
            ketaminePitchLag *= 0.82D;
            ketamineCameraPull *= 0.78D;
                ketamineSwayYaw *= 0.78D;
                ketamineSwayPitch *= 0.78D;
                ketamineSwayRoll *= 0.78D;
                ketaminePresentationSway *= 0.76D;
            lastKetamineYaw = event.getYaw();
            lastKetaminePitch = event.getPitch();
            ketamineRenderLagInitialized = false;
            ketamineRenderYaw = event.getYaw();
            ketamineRenderPitch = event.getPitch();
        }

        if (heroin.active()) {
            double motion = heroinMotionGain(heroin);
            double dose = Math.max(heroin.activeDoseMilligrams(), heroin.targetDoseMilligrams());
            double doseLoad = smoothstep(25.0D, 500.0D, dose);
            double nodDose = smoothstep(210.0D, 500.0D, dose);
            double shutdown = smoothstep(300.0D, 500.0D, dose);
            float cameraYaw = event.getYaw();
            float cameraPitch = event.getPitch();
            if (!Float.isFinite(lastHeroinYaw) || !Float.isFinite(lastHeroinPitch)) {
                lastHeroinYaw = cameraYaw;
                lastHeroinPitch = cameraPitch;
            }
            if (!heroinRenderLagInitialized) {
                heroinRenderLagInitialized = true;
                heroinRenderYaw = cameraYaw;
                heroinRenderPitch = cameraPitch;
            }

            double yawDelta = Mth.wrapDegrees(cameraYaw - lastHeroinYaw);
            double pitchDelta = cameraPitch - lastHeroinPitch;
        registerHeroinLookInput(yawDelta, pitchDelta, heroin);
        double nodDrive = clamp((heroin.nodding() * 0.54D + heroin.microSleep() * 0.78D + heroin.sedation() * 0.18D) / 7.2D
            + nodDose * 0.24D
            + shutdown * 0.12D);
        double heroinTime = time * 0.12D + heroin.ageTicks() * 0.006D + dose * 0.004D;
        double nodPulse = 0.5D + 0.5D * Math.sin(heroinTime * (0.014D + nodDose * 0.004D) + 0.8D);
        double blackoutPulse = 0.5D + 0.5D * Math.sin(heroinTime * (0.0048D + nodDrive * 0.0018D) + 2.1D);
        double nodLatch = clamp(nodDrive * (0.56D + nodPulse * 0.44D) + shutdown * 0.10D);
        boolean wakeTriggered = heroinNodAmount > 0.10D && heroinWakeRequirement > 0.0D && heroinWakeShake >= heroinWakeRequirement;
        boolean nodLatched = heroinNodAmount > 0.06D;
        if (!nodLatched && heroinWakeCooldownTicks <= 0) {
        double latchThreshold = mix(0.68D, 0.22D, clamp(nodDose * 0.74D + nodDrive * 0.52D));
        nodLatched = nodLatch >= latchThreshold;
        }
        double targetNod = 0.0D;
        if (wakeTriggered) {
        heroinWakeShake = 0.0D;
                heroinWakeDirection = 0;
                heroinWakeCooldownTicks = (int) Math.round(mix(10.0D, 26.0D, clamp(nodDose * 0.64D + nodDrive * 0.40D)));
        nodLatched = false;
        } else if (nodLatched) {
        targetNod = clamp(Math.max(nodLatch, heroinNodAmount * (0.88D + nodDrive * 0.08D)));
        }
            double nodRate = targetNod > heroinNodAmount
            ? mix(0.024D, 0.060D, clamp(nodDrive + doseLoad * 0.34D))
            : wakeTriggered
            ? mix(0.16D, 0.34D, clamp(nodDose + nodDrive * 0.36D))
            : mix(0.020D, 0.055D, clamp(0.26D + heroinWakeCooldownTicks / 62.0D));
            heroinNodAmount += (targetNod - heroinNodAmount) * nodRate;
            heroinNodAmount = clamp(heroinNodAmount);
        double blackoutTarget = heroinNodAmount
            * (0.16D + nodDrive * 0.34D + nodDose * 0.24D + shutdown * 0.22D)
            * (0.40D + blackoutPulse * 0.60D);
        heroinBlackoutAmount += (blackoutTarget - heroinBlackoutAmount) * (TrippyClientConfig.motionReduction ? 0.18D : 0.045D);
        heroinBlackoutAmount = clamp(heroinBlackoutAmount);

            double floatiness = clamp(heroin.softFocus() / 7.4D * 0.32D
                    + heroin.warmth() / 8.2D * 0.18D
                    + heroin.sedation() / 8.2D * 0.18D
                    + doseLoad * 0.16D
                    + motion * 0.22D
            + heroinNodAmount * 0.28D
            + heroinBlackoutAmount * 0.26D);
            double lag = clamp(heroin.motorDrag() / 5.0D * 0.36D
                    + heroin.softFocus() / 7.4D * 0.22D
                    + heroin.sedation() / 8.2D * 0.22D
                    + nodDrive * 0.30D
                    + motion * 0.20D
            + doseLoad * 0.12D
            + heroinBlackoutAmount * 0.22D);
                    double settle = TrippyClientConfig.motionReduction ? 0.10D : mix(0.038D, 0.006D, clamp(lag + heroinNodAmount * 0.62D + heroinBlackoutAmount * 0.48D));
            heroinYawLag += (-yawDelta * (0.30D + lag * 0.42D + heroinNodAmount * 0.44D) - heroinYawLag) * settle;
            heroinPitchLag += (-pitchDelta * (0.28D + lag * 0.36D + heroinNodAmount * 0.50D) - heroinPitchLag) * settle;

                    double follow = TrippyClientConfig.motionReduction ? 0.12D : mix(0.026D, 0.003D, clamp(lag + floatiness * 0.46D + heroinNodAmount * 0.72D + heroinBlackoutAmount * 0.54D));
            heroinRenderYaw += Mth.wrapDegrees(cameraYaw - (float) heroinRenderYaw) * follow;
            heroinRenderPitch += (cameraPitch - heroinRenderPitch) * (follow * 0.88D);
        double delayedYaw = Mth.wrapDegrees((float) (heroinRenderYaw - cameraYaw)) * clamp(0.44D + lag * 0.44D + floatiness * 0.30D + heroinNodAmount * 0.58D + heroinBlackoutAmount * 0.20D);
        double delayedPitch = (heroinRenderPitch - cameraPitch) * clamp(0.40D + lag * 0.38D + floatiness * 0.26D + heroinNodAmount * 0.62D + heroinBlackoutAmount * 0.24D);

            double slowBreath = Math.sin(heroinTime * (0.010D + floatiness * 0.003D) + 0.8D);
            double sideFloat = Math.sin(heroinTime * (0.007D + floatiness * 0.002D) + 2.2D);
            double liftFloat = Math.cos(heroinTime * (0.008D + floatiness * 0.002D) + 1.4D);
        double targetRoll = sideFloat * (0.12D + floatiness * 0.58D + heroinNodAmount * 0.24D)
            + delayedYaw * 0.08D;
        double targetYaw = heroinYawLag * (0.34D + lag * 0.22D + heroinNodAmount * 0.24D)
            + delayedYaw * (0.92D + lag * 0.40D + heroinNodAmount * 0.56D)
            + sideFloat * (0.018D + floatiness * 0.038D);
            double nodDrop = heroinNodAmount * mix(14.0D, 42.0D, clamp(nodDose * 0.64D + nodDrive * 0.58D))
            + heroinBlackoutAmount * 10.0D;
            double targetPitch = heroinPitchLag * (0.40D + lag * 0.30D + heroinNodAmount * 0.42D)
            + delayedPitch * (0.88D + lag * 0.34D + heroinNodAmount * 0.64D)
            + slowBreath * (0.020D + floatiness * 0.070D)
            + liftFloat * (0.014D + floatiness * 0.040D)
                    + nodDrop;
            targetPitch = Math.min(targetPitch, Math.max(0.0D, 82.0D - cameraPitch));
            double smoothing = TrippyClientConfig.motionReduction ? 0.16D : mix(0.060D, 0.014D, clamp(floatiness + lag * 0.40D + heroinNodAmount * 0.52D + heroinBlackoutAmount * 0.30D));
            heroinSwayRoll += (targetRoll - heroinSwayRoll) * smoothing;
            heroinSwayYaw += (targetYaw - heroinSwayYaw) * smoothing;
            heroinSwayPitch += (targetPitch - heroinSwayPitch) * smoothing;
            roll += (float) heroinSwayRoll;
            yaw += (float) heroinSwayYaw;
            pitch += (float) heroinSwayPitch;
            lastHeroinYaw = cameraYaw;
            lastHeroinPitch = cameraPitch;
        } else {
            heroinYawLag *= 0.78D;
            heroinPitchLag *= 0.78D;
            heroinSwayYaw *= 0.76D;
            heroinSwayPitch *= 0.76D;
            heroinSwayRoll *= 0.76D;
            heroinNodAmount *= 0.82D;
            heroinBlackoutAmount *= 0.84D;
            if (heroinNodAmount < 0.001D) {
                heroinNodAmount = 0.0D;
                heroinStillTicks = 0;
            }
            if (heroinBlackoutAmount < 0.001D) {
                heroinBlackoutAmount = 0.0D;
            }
            heroinWakeShake = 0.0D;
            heroinWakeRequirement = 0.0D;
            heroinWakeCooldownTicks = 0;
            heroinWakeDirection = 0;
            lastHeroinYaw = event.getYaw();
            lastHeroinPitch = event.getPitch();
            heroinRenderLagInitialized = false;
            heroinRenderYaw = event.getYaw();
            heroinRenderPitch = event.getPitch();
        }

        if (opioidWithdrawalShake > 0.01D) {
            double shake = smoothstep(0.42D, 0.95D, opioidWithdrawalShake);
            if (TrippyClientConfig.motionReduction) {
                shake *= 0.35D;
            }
            double shakeTime = time + ClientExperienceStore.heroinRecovery().soberTicks() * 0.018D;
            double tremorA = Math.sin(shakeTime * (0.88D + shake * 0.64D));
            double tremorB = Math.sin(shakeTime * (1.46D + shake * 0.92D) + 1.7D);
            double tremorC = Math.cos(shakeTime * (1.18D + shake * 0.72D) + 0.4D);
            roll += (float) ((tremorA * 0.22D + tremorB * 0.16D) * shake);
            yaw += (float) ((tremorB * 0.16D + tremorC * 0.08D) * shake);
            pitch += (float) ((tremorC * 0.14D - tremorA * 0.10D) * shake);
        }

        // Keep the complete authored motion as one common visual transform.
        // Vivecraft still owns the physical eye poses; the final stereo composite
        // reprojects the scene with these offsets instead of rewriting tracking.
        if (vr) {
            Vec3 translation = ketamineVrTranslation(ketamine, event.getYaw() + yaw, event.getPitch() + pitch, time);
            vrCameraMotion = new VrCameraMotion(yaw, pitch, roll, 1.0F, translation);
            return;
        }
        float finalRoll = event.getRoll() + roll;
        float finalYaw = event.getYaw() + yaw;
        float finalPitch = event.getPitch() + pitch;
        applyKetamineCameraPresentation(event, ketamine, finalYaw, finalPitch, time);
        event.setRoll(finalRoll);
        event.setYaw(finalYaw);
        event.setPitch(finalPitch);
    }

    public static void onFov(ViewportEvent.ComputeFov event) {
        if (VivecraftCompat.isVrActive()) {
            // Runtime optics stay intact. updateVrPresentation samples the exact
            // original FOV factor once for the common visual composite instead.
            return;
        }
        computeFov(event);
    }

    private static void computeFov(ViewportEvent.ComputeFov event) {
        ClientEffectSample sample = ClientEffectGraph.sample((float) event.getPartialTick());
        CannabisEffectState cannabis = ClientExperienceStore.cannabis();
        CocaineEffectState cocaine = ClientExperienceStore.cocaine();
        KetamineEffectState ketamine = ClientExperienceStore.ketamine();
        HeroinEffectState heroin = ClientExperienceStore.heroin();
        DmtEffectState dmt = ClientExperienceStore.dmt();
        AlcoholEffectState alcohol = ClientExperienceStore.alcohol();
        MdmaEffectState mdma = ClientExperienceStore.mdma();
        TwoCbEffectState twoCb = ClientExperienceStore.twoCb();
        MescalineEffectState mescaline = ClientExperienceStore.mescaline();
        RecoveryState alcoholRecovery = ClientExperienceStore.alcoholRecovery();
        double hangover = alcoholHangoverLoad(alcoholRecovery, alcohol);
        double withdrawal = alcoholWithdrawalLoad(alcoholRecovery);
        boolean dmtFovVisible = dmtScaffoldVisible(dmt) && TrippyConfig.enableDmtEffects && !TrippyConfig.dmtAccessibilityMode;
        boolean alcoholFovVisible = alcohol.active() || hangover > 0.01D || withdrawal > 0.01D;
        boolean cannabisFovVisible = cannabis.active();
        boolean cocaineFovVisible = cocaine.active();
        boolean ketamineFovVisible = ketamine.active();
        boolean heroinFovVisible = heroin.active();
        boolean mdmaFovVisible = mdma.active();
        boolean twoCbFovVisible = twoCb.active();
        boolean mescalineFovVisible = mescaline.active();
        if (!sample.visible() && !dmtFovVisible && !alcoholFovVisible && !cannabisFovVisible && !cocaineFovVisible && !ketamineFovVisible && !heroinFovVisible && !mdmaFovVisible && !twoCbFovVisible && !mescalineFovVisible) {
            return;
        }

        ClientRenderSignature signature = sample.signature();
        double breath = Math.sin(sample.time() * (0.032D + signature.organicFlow() * 0.012D)) * sample.depthBreathing();
        double stretch = sample.timeDilation() * (0.024D + signature.temporalSlip() * 0.014D)
                + sample.stimulation() * (0.014D + signature.edgeSharpness() * 0.012D)
                - sample.sedation() * (0.014D + signature.narrowing() * 0.026D);
        double pulse = breath * (0.015D + signature.organicFlow() * 0.008D + signature.depthSeparation() * 0.005D) + stretch;
        pulse *= ClientEffectGraph.genericPresentationGain(sample);
        PsilocybinEffectState psilocybin = ClientExperienceStore.psilocybin();
        LsdEffectState lsd = ClientExperienceStore.lsd();
        SalviaEffectState salvia = ClientExperienceStore.salvia();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(psilocybin, lsd, dmt, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        double dmtFovFactor = 1.0D;
        double cannabisFovFactor = 1.0D;
        double cocaineFovFactor = 1.0D;
        double ketamineFovFactor = 1.0D;
        double heroinFovFactor = 1.0D;
        double mdmaFovFactor = 1.0D;
        double twoCbFovFactor = 1.0D;
        double mescalineFovFactor = 1.0D;
        // LSD deformation and continuous colour live on surfaces; the viewpoint stays steady.
        if (dmtFovVisible) {
            var scaffold = dmt.fractalScaffold();
            double dmtGain = blend.dmtMotionGain();
            double launch = smoothstep(0.0D, 0.16D, dmt.progress()) * (1.0D - smoothstep(0.92D, 1.0D, dmt.progress()));
            double tunnelPulse = Math.sin(sample.time() * (0.082D + dmt.signature().tunnelSpeed() * 0.040D) + dmt.signature().seed() * 0.000007D);
            double flowerPulse = Math.sin(sample.time() * (0.048D + dmt.signature().mandalaDensity() * 0.028D) + dmt.signature().paletteStyle());
            pulse += (tunnelPulse * scaffold.temporalFlow() * 0.005D
                    + flowerPulse * scaffold.fractal3dPresence() * 0.003D
                    - scaffold.alternateWorldReadiness() * 0.0015D) * launch * dmtGain;
            dmtFovFactor = dmtTunnelFovFactor(dmt, dmtGain);
        }
        if (salvia.active() && TrippyConfig.enableSalviaEffects) {
            double salviaGain = (TrippyConfig.salviaAccessibilityMode || TrippyClientConfig.motionReduction) ? 0.55D : 1.0D;
            double page = Math.sin(sample.time() * (0.032D + salvia.signature().recursionBias() * 0.016D) + salvia.signature().seed() * 0.000004D);
            double slot = Math.sin(sample.time() * (0.018D + salvia.signature().anchorBias() * 0.010D) + 1.1D);
            pulse += (page * salvia.loopSeam() * 0.005D
                    + slot * salvia.anchorSlip() * 0.004D
                    - salvia.sceneReplacement() * 0.003D
                    - salvia.observerPressure() * 0.002D
                    + Math.sin(sample.time() * 0.061D + 0.7D) * salvia.reentryConfusion() * 0.004D) * salviaGain;
        }
        if (cannabis.active()) {
            double motion = cannabisMotionGain(cannabis);
            double breathing = Math.sin(sample.time() * (0.0032D + cannabis.breathingPulse() * 0.00016D) + cannabis.doseScale() * 0.08D);
            double shapedBreathing = Math.copySign(Math.pow(Math.abs(breathing), 1.18D), breathing);
            double tunnel = clamp(cannabis.focusTunnel() * 0.06D) * (0.45D + motion * 0.55D);
            double temporal = clamp(cannabis.temporalDrag() * 0.05D) * (0.42D + motion * 0.58D);
            double targetCannabisFactor = 1.0D + shapedBreathing * (0.0022D + motion * 0.0044D) - tunnel * 0.014D - temporal * 0.0052D;
            cannabisFovFactor = smoothCannabisFovFactor(targetCannabisFactor, cannabis.active());
        } else {
            cannabisFovFactor = smoothCannabisFovFactor(1.0D, false);
        }
        if (cocaine.active()) {
            double drive = clamp(cocaine.locomotorDrive() * 0.045D + cocaine.clarityLift() * 0.028D + cocaine.inputSharpness() * 0.020D);
            double tunnel = clamp(cocaine.tunnelFocus() * 0.055D + cocaine.lightHarshness() * 0.025D + cocaine.edgeShimmer() * 0.018D);
            double crash = clamp(cocaine.crashDrag() * 0.070D + cocaine.crashDesaturation() * 0.30D);
            double flicker = Math.sin(sample.time() * (0.18D + cocaine.microFlicker() * 0.03D) + cocaine.ageTicks() * 0.011D);
            pulse += flicker * drive * 0.003D - crash * 0.004D;
            cocaineFovFactor = 1.0D + drive * 0.012D - tunnel * 0.030D - crash * 0.018D;
        }
        if (ketamine.active()) {
            double motion = ketamineMotionGain(ketamine);
            double tunnel = clamp(ketamine.uiFade() / 2.2D) * (0.36D + motion * 0.44D);
            double disconnect = clamp(ketamine.depthMismatch() / 2.5D) * (0.28D + motion * 0.38D);
            double floatiness = clamp(ketamine.bodyFloat() / 2.4D) * (0.18D + motion * 0.22D);
            double outOfBody = ketamineOutOfBodyPull(ketamine);
            double slip = Math.sin(sample.time() * (0.0055D + clamp(ketamine.timeSlip() / 2.8D) * 0.0040D) + ketamine.ageTicks() * 0.004D);
            pulse += slip * floatiness * 0.002D - disconnect * 0.006D - outOfBody * 0.004D;
            double targetKetamineFactor = 1.0D - tunnel * 0.095D - disconnect * 0.052D - outOfBody * 0.055D + slip * floatiness * 0.004D;
            ketamineFovFactor = smoothKetamineFovFactor(targetKetamineFactor, true);
        } else {
            ketamineFovFactor = smoothKetamineFovFactor(1.0D, false);
        }
        if (heroin.active()) {
            double heroinLoad = heroinMotionGain(heroin);
            double heroinBreath = Math.sin(sample.time() * (0.006D + heroinLoad * 0.004D) + heroin.activeDoseMilligrams() * 0.016D);
            double nod = clamp(heroinNodAmount + heroin.microSleep() / 8.2D * 0.22D);
            pulse += heroinBreath * heroinLoad * 0.004D - nod * 0.006D;
            heroinFovFactor = 1.0D + heroinBreath * heroinLoad * 0.006D - nod * 0.038D - clamp(heroin.softFocus() / 8.2D) * 0.010D;
        }
        if (mdma.active()) {
            double breathing = Math.sin(sample.time() * 0.032D + mdma.activeDoseMilligrams() * 0.003D);
            double beat = ClientTavernAmbience.bassPulse((float) event.getPartialTick());
            double motion = TrippyClientConfig.motionReduction ? 0.32D : 1.0D;
            double bassBreath = mdma.peakWindow() ? beat * mdma.bassSensitivity() * 0.006D : 0.0D;
            mdmaFovFactor = 1.0D + (breathing * mdma.fovBreathing() * 0.010D + bassBreath) * motion
                    - mdma.comedownFatigue() * 0.006D;
        }
        if (twoCb.active() && TrippyConfig.enableTwoCbEffects) {
            boolean club = ClientTavernAmbience.twoCbSettingMultiplier() > 1.0D;
            double beat = club
                    ? ClientTavernAmbience.bassPulse((float) event.getPartialTick())
                    : Math.pow(Math.max(0.0D, Math.sin(sample.time() * 0.155D)), 10.0D) * 0.52D;
            double motion = TrippyClientConfig.motionReduction || TrippyConfig.twoCbAccessibilityMode ? 0.38D : 1.0D;
            double quietBreath = Math.sin(sample.time() * 0.027D + twoCb.activeDoseMilligrams() * 0.004D);
            twoCbFovFactor = 1.0D + (beat * twoCb.fisheyePulse() * 0.020D
                    + quietBreath * twoCb.fisheyePulse() * 0.0028D) * motion;
        }
        if (mescaline.active()) {
            double motion = TrippyClientConfig.motionReduction ? 0.30D : 1.0D;
            double slowBreath = Math.sin(sample.time() * 0.0062D * mescaline.signature().breathingRhythm()
                    + (mescaline.signature().seed() & 2047L) * 0.002D);
            // Intentionally independent of recursive complexity: even extreme trips stay playable.
            mescalineFovFactor = 1.0D + slowBreath * Math.min(0.0055D,
                    mescaline.surfaceBreathing() * 0.0042D) * motion;
        }
        if ((alcohol.active() || hangover > 0.01D || withdrawal > 0.01D) && TrippyConfig.enableAlcoholEffects) {
            double gain = Math.max(alcoholMotionGain(alcohol), hangover * 0.26D + withdrawal * 0.36D);
            double alcoholBreath = Math.sin(sample.time() * 0.027D + alcohol.activeStandardDrinks() * 0.9D);
            double tunnel = Math.max(alcoholLoad(alcohol, alcohol.peripheralNarrowing()), hangover * 0.18D + withdrawal * 0.12D);
            double focus = Math.max(alcoholLoad(alcohol, alcohol.focusBlur()), hangover * 0.24D + withdrawal * 0.10D);
            pulse += (alcoholBreath * gain * 0.006D
                    - tunnel * (0.010D + gain * 0.012D)
                    - focus * 0.004D
                    + Math.sin(sample.time() * 0.071D) * Math.max(alcoholLoad(alcohol, alcohol.nausea()), hangover * 0.06D + withdrawal * 0.10D) * 0.003D
                    - hangover * 0.010D
                    - withdrawal * 0.014D);
        }
        event.setFOV((float) (event.getFOV() * Math.max(0.25D, (1.0D + pulse) * dmtFovFactor * cannabisFovFactor * cocaineFovFactor * ketamineFovFactor * heroinFovFactor * mdmaFovFactor * twoCbFovFactor * mescalineFovFactor)));
    }

    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        ClientEffectSample sample = ClientEffectGraph.sample((float) event.getPartialTick());
        CannabisEffectState cannabis = ClientExperienceStore.cannabis();
        KetamineEffectState ketamine = ClientExperienceStore.ketamine();
        HeroinEffectState heroin = ClientExperienceStore.heroin();
        AlcoholEffectState alcohol = ClientExperienceStore.alcohol();
        MescalineEffectState mescaline = ClientExperienceStore.mescaline();
        RecoveryState alcoholRecovery = ClientExperienceStore.alcoholRecovery();
        double hangover = alcoholHangoverLoad(alcoholRecovery, alcohol);
        double withdrawal = alcoholWithdrawalLoad(alcoholRecovery);
        if (!sample.visible() && !cannabis.active() && !ketamine.active() && !heroin.active() && !alcohol.active() && !mescaline.active() && hangover <= 0.01D && withdrawal <= 0.01D) {
            return;
        }

        double bloom = sample.colorBloom();
        ClientRenderSignature signature = sample.signature();
        double warmth = sample.euphoria() * 0.055D + signature.warmth() * 0.040D - sample.sedation() * 0.020D;
        double threat = sample.anxiety() * 0.050D + sample.risk() * 0.032D + signature.instability() * 0.022D;
        double distance = sample.dissociation() * 0.045D + signature.coolDistance() * 0.035D;
        double genericGain = ClientEffectGraph.genericPresentationGain(sample);
        bloom *= genericGain;
        warmth *= genericGain;
        threat *= genericGain;
        distance *= genericGain;
        double amberLift = 0.0D;
        double greenLift = 0.0D;
        PsilocybinEffectState psilocybin = ClientExperienceStore.psilocybin();
        LsdEffectState lsd = ClientExperienceStore.lsd();
        DmtEffectState dmt = ClientExperienceStore.dmt();
        SalviaEffectState salvia = ClientExperienceStore.salvia();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(psilocybin, lsd, dmt, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        // Psilocybin colour and clarity are applied to surfaces by its world shader.
        // LSD deformation and continuous colour live on surfaces; the viewpoint stays steady.
        if (dmt.active() && dmt.hasAuthoredEffects() && TrippyConfig.enableDmtEffects) {
            var scaffold = dmt.fractalScaffold();
            double phase = sample.time() * (0.018D + dmt.signature().colorStrobe() * 0.012D) + dmt.signature().seed() * 0.000001D;
            double strobe = Math.sin(phase * 2.7D) * 0.5D + 0.5D;
            double dmtGain = blend.dmtVisualGain();
            double realm = dmtScaffoldPresence(dmt);
            warmth += ((dmt.signature().accentRed() / 255.0D - 0.50D) * realm * 0.032D
                    + strobe * scaffold.staticAnimatedCoupling() * 0.016D) * dmtGain;
            distance += ((dmt.signature().primaryBlue() / 255.0D) * scaffold.dimensionalDepth() * 0.052D
                    + scaffold.alternateWorldReadiness() * 0.042D) * dmtGain;
            threat += Math.max(0.0D, dmt.intensity() - 5.4D) * 0.004D * dmtGain;
            bloom += (scaffold.fractal3dPresence() * 0.050D
                    + scaffold.animatedFractalPresence() * 0.034D
                    + scaffold.alternateWorldReadiness() * 0.046D) * dmtGain;
        }
        if (mescaline.active()) {
            double calm = Math.min(1.0D, mescaline.visualPresence());
            warmth += calm * Math.min(0.065D, mescaline.warmth() * 0.046D);
            amberLift += calm * Math.min(0.055D, mescaline.colorRichness() * 0.040D);
            greenLift += calm * Math.min(0.045D, mescaline.organicFlow() * 0.032D);
            bloom += calm * Math.min(0.035D, (mescaline.colorRichness() + mescaline.afterglow()) * 0.018D);
        }
        if (cannabis.active()) {
            double cannabisLoad = cannabisVisualLoad(cannabis);
            double warmthShift = clamp(cannabis.warmthShift() * 0.060D) * (0.72D + cannabisLoad * 0.38D);
            double saturation = clamp(cannabis.saturationBoost() * 0.052D) * (0.62D + cannabisLoad * 0.52D);
            double greens = clamp(cannabis.greenRichness() * 0.050D) * (0.56D + cannabisLoad * 0.56D);
            warmth += cannabisLoad * 0.045D + warmthShift;
            bloom += saturation * 0.42D;
            distance -= cannabisLoad * 0.014D;
            amberLift += warmthShift * 1.08D + saturation * 0.44D;
            greenLift += greens * 1.04D;
        }
        if (ketamine.active()) {
            double ketamineLoad = ketamineVisualLoad(ketamine);
            double disconnect = clamp(ketamine.depthMismatch() / 2.6D) * (0.56D + ketamineLoad * 0.44D);
            double fade = clamp(ketamine.uiFade() / 2.3D) * (0.44D + ketamineLoad * 0.56D);
            warmth -= ketamineLoad * 0.028D + fade * 0.028D;
            distance += disconnect * 0.084D + ketamineLoad * 0.040D;
            bloom -= fade * 0.046D;
        }
        if (heroin.active()) {
            double heroinLoad = heroinVisualLoad(heroin);
            double heroinWarmth = clamp(heroin.warmth() / 8.2D) * (0.82D + heroinLoad * 0.36D);
            double blanket = clamp((heroin.softFocus() + heroin.euphoria() * 0.42D + heroin.painSuppression() * 0.26D) / 8.2D);
            warmth += heroinLoad * 0.070D + heroinWarmth * 0.140D + blanket * 0.038D;
            bloom += heroinWarmth * 0.050D + blanket * 0.030D;
            distance -= blanket * 0.024D;
            amberLift += heroinWarmth * 1.38D + blanket * 0.32D;
            greenLift -= heroinWarmth * 0.10D;
        }
            if (salvia.active() && TrippyConfig.enableSalviaEffects) {
                double salviaGain = TrippyConfig.salviaAccessibilityMode ? 0.60D : 1.0D;
                double phase = sample.time() * (0.012D + salvia.signature().recursionBias() * 0.010D) + salvia.signature().seed() * 0.000001D;
                double flutter = Math.sin(phase * 2.1D) * 0.5D + 0.5D;
                warmth += (((salvia.signature().secondaryRed() / 255.0D) - 0.44D) * salvia.sceneReplacement() * 0.026D
                    + salvia.environment().village() * salvia.sceneReplacement() * 0.010D) * salviaGain;
                distance += (((salvia.signature().primaryBlue() / 255.0D) * salvia.anchorSlip() * 0.042D)
                    + salvia.observerPressure() * 0.028D
                    + salvia.environment().cave() * 0.016D) * salviaGain;
                threat += (salvia.observerPressure() * 0.034D
                    + salvia.reentryConfusion() * 0.024D
                    + salvia.cutoutPresence() * flutter * 0.016D) * salviaGain;
                bloom += (salvia.sceneReplacement() * 0.030D + salvia.loopSeam() * flutter * 0.018D) * salviaGain;
            }
        if ((alcohol.active() || hangover > 0.01D || withdrawal > 0.01D) && TrippyConfig.enableAlcoholEffects) {
            double load = alcoholLoad(alcohol, alcohol.intensity());
            double focus = Math.max(alcoholLoad(alcohol, alcohol.focusBlur()), hangover * 0.28D + withdrawal * 0.18D);
            double tunnel = Math.max(alcoholLoad(alcohol, alcohol.peripheralNarrowing()), hangover * 0.18D + withdrawal * 0.18D);
            double warmFlush = smoothstep(0.35D, 2.2D, alcohol.activeStandardDrinks()) * (1.0D - smoothstep(4.8D, 7.5D, alcohol.activeStandardDrinks()));
            warmth += warmFlush * load * 0.026D - alcoholLoad(alcohol, alcohol.sedation()) * 0.010D;
            threat += alcoholLoad(alcohol, alcohol.risk()) * 0.020D + alcoholLoad(alcohol, alcohol.nausea()) * 0.012D;
            distance += focus * 0.018D - tunnel * 0.012D;
            bloom -= tunnel * 0.020D;
            bloom += hangover * 0.090D + withdrawal * 0.040D;
            warmth += hangover * 0.055D;
            threat += withdrawal * 0.060D + hangover * 0.022D;
        }

        event.setRed((float) clamp(event.getRed() * (1.0D + bloom * 0.25D + warmth + threat + amberLift * 0.58D + greenLift * 0.10D)));
        event.setGreen((float) clamp(event.getGreen() * (1.0D + bloom * 0.18D + warmth * 0.45D - threat * 0.38D + amberLift * 0.24D + greenLift * 0.80D)));
        event.setBlue((float) clamp(event.getBlue() * (1.0D + bloom * 0.32D + distance - warmth * 0.35D - amberLift * 0.22D - greenLift * 0.14D)));
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        ClientEffectSample sample = ClientEffectGraph.sample((float) event.getPartialTick());
        CannabisEffectState cannabis = ClientExperienceStore.cannabis();
        KetamineEffectState ketamine = ClientExperienceStore.ketamine();
        HeroinEffectState heroin = ClientExperienceStore.heroin();
        AlcoholEffectState alcohol = ClientExperienceStore.alcohol();
        RecoveryState alcoholRecovery = ClientExperienceStore.alcoholRecovery();
        double hangover = alcoholHangoverLoad(alcoholRecovery, alcohol);
        double withdrawal = alcoholWithdrawalLoad(alcoholRecovery);
        if (!sample.visible() && !cannabis.active() && !ketamine.active() && !heroin.active() && !alcohol.active() && hangover <= 0.01D && withdrawal <= 0.01D) {
            return;
        }

        ClientRenderSignature signature = sample.signature();
        float melt = (float) (1.0D
                - sample.geometryMelt() * (0.030D + signature.organicFlow() * 0.020D)
                - sample.sedation() * (0.040D + signature.narrowing() * 0.045D)
                + sample.dissociation() * (0.020D + signature.depthSeparation() * 0.024D));
        float near = (float) (1.0D
                + sample.anxiety() * (0.026D + signature.edgeSharpness() * 0.018D)
                + sample.dissociation() * (0.020D + signature.depthSeparation() * 0.026D)
                + signature.narrowing() * 0.022D);
        float genericGain = (float) ClientEffectGraph.genericPresentationGain(sample);
        melt = 1.0F + (melt - 1.0F) * genericGain;
        near = 1.0F + (near - 1.0F) * genericGain;
        PsilocybinEffectState psilocybin = ClientExperienceStore.psilocybin();
        LsdEffectState lsd = ClientExperienceStore.lsd();
        DmtEffectState dmt = ClientExperienceStore.dmt();
        SalviaEffectState salvia = ClientExperienceStore.salvia();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(psilocybin, lsd, dmt, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        if (psilocybin.active() && TrippyConfig.enablePsilocybinEffects) {
            double extreme = Math.max(0.0D, psilocybin.intensity() - 1.0D);
            double wobble = Math.sin(sample.time() * 0.045D * psilocybin.signature().breathingRhythm()) * psilocybin.organicBreathing();
            double psilocybinGain = blend.psilocybinVisualGain();
            melt -= (float) ((psilocybin.organicBreathing() * 0.080D + psilocybin.surfaceMelting() * 0.090D + extreme * 0.20D + Math.abs(wobble) * 0.05D) * psilocybinGain);
            near += (float) ((psilocybin.environment().cave() * psilocybin.intensity() * 0.060D + extreme * 0.10D) * psilocybinGain);
        }
        // LSD deformation and continuous colour live on surfaces; the viewpoint stays steady.
        if (dmt.active() && dmt.hasAuthoredEffects() && TrippyConfig.enableDmtEffects) {
            var scaffold = dmt.fractalScaffold();
            double breakthrough = smoothstep(0.14D, 0.36D, dmt.progress()) * (1.0D - smoothstep(0.74D, 1.0D, dmt.progress()));
            double pulse = Math.abs(Math.sin(sample.time() * (0.055D + dmt.signature().tunnelSpeed() * 0.030D) + dmt.signature().seed() * 0.000008D));
            double dmtGain = blend.dmtVisualGain();
            melt -= (float) ((scaffold.alternateWorldReadiness() * 0.060D
                    + scaffold.dimensionalDepth() * 0.042D
                    + breakthrough * pulse * 0.060D) * dmtGain);
            near += (float) ((scaffold.staticAnimatedCoupling() * 0.018D
                    + scaffold.alternateWorldReadiness() * 0.024D
                    + breakthrough * 0.030D) * dmtGain);
        }
            if (salvia.active() && TrippyConfig.enableSalviaEffects) {
                double salviaGain = (TrippyConfig.salviaAccessibilityMode || TrippyClientConfig.motionReduction) ? 0.58D : 1.0D;
                melt -= (float) ((salvia.anchorSlip() * 0.028D
                    + salvia.sceneReplacement() * 0.040D
                    + salvia.loopSeam() * 0.026D) * salviaGain);
                near += (float) ((salvia.observerPressure() * 0.022D
                    + salvia.cutoutPresence() * 0.016D
                    + salvia.reentryConfusion() * 0.018D) * salviaGain);
            }
        if (cannabis.active()) {
            double cannabisLoad = cannabisVisualLoad(cannabis);
            double tunnel = clamp(cannabis.focusTunnel() * 0.028D) * (0.60D + cannabisLoad);
            double temporal = clamp(cannabis.temporalDrag() * 0.024D) * (0.56D + cannabisLoad);
            double sticky = clamp(cannabis.thoughtStickiness() * 0.020D) * (0.48D + cannabisLoad);
            melt -= (float) (tunnel * 0.070D + temporal * 0.028D);
            near += (float) (tunnel * 0.050D + sticky * 0.020D);
        }
        if (ketamine.active()) {
            double ketamineLoad = ketamineVisualLoad(ketamine);
            double tunnel = clamp(ketamine.uiFade() / 2.2D) * (0.52D + ketamineLoad * 0.40D);
            double depth = clamp(ketamine.depthMismatch() / 2.4D) * (0.58D + ketamineLoad * 0.34D);
            double floatiness = clamp(ketamine.bodyFloat() / 2.4D) * 0.24D;
            double collapse = clamp((ketamine.collapseRisk() + ketamine.collapseDepth()) / 2.4D);
            melt -= (float) (tunnel * 0.12D + depth * 0.06D + floatiness * 0.04D);
            near += (float) (depth * 0.07D + tunnel * 0.05D + collapse * 0.03D);
        }
        if (heroin.active()) {
            double heroinLoad = heroinVisualLoad(heroin);
            double softness = clamp(heroin.softFocus() / 8.2D) * (0.70D + heroinLoad * 0.44D);
            double nod = clamp(heroinNodAmount + heroin.microSleep() / 8.2D * 0.24D);
            melt -= (float) (softness * 0.036D + nod * 0.030D);
            near += (float) (softness * 0.018D + nod * 0.036D);
        }
        if ((alcohol.active() || hangover > 0.01D || withdrawal > 0.01D) && TrippyConfig.enableAlcoholEffects) {
            double gain = Math.max(alcoholMotionGain(alcohol), hangover * 0.26D + withdrawal * 0.34D);
            melt -= (float) ((alcoholLoad(alcohol, alcohol.focusBlur()) * 0.026D
                    + alcoholLoad(alcohol, alcohol.peripheralNarrowing()) * 0.070D
                + alcoholLoad(alcohol, alcohol.sedation()) * 0.046D
                + hangover * 0.042D
                + withdrawal * 0.050D) * gain);
            near += (float) ((alcoholLoad(alcohol, alcohol.peripheralNarrowing()) * 0.030D
                + alcoholLoad(alcohol, alcohol.nausea()) * 0.018D
                + withdrawal * 0.024D) * gain);
        }
        event.scaleFarPlaneDistance(Math.max(0.40F, melt));
        event.scaleNearPlaneDistance(Math.max(0.85F, near));
    }

    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!com.trippycraft.config.TrippyClientConfig.debugChannels || !ClientExperienceStore.active()) {
            return;
        }
        event.getLeft().addAll(ClientEffectGraph.debugLines(
                ClientEffectGraph.sample(event.getPartialTick().getGameTimeDeltaPartialTick(false))));
            PsilocybinEffectState psilocybin = ClientExperienceStore.psilocybin();
            if (psilocybin.active()) {
                event.getLeft().add("  psilocybin=" + String.format(java.util.Locale.ROOT, "%.2fg %s %.2f %ds",
                        psilocybin.displayDoseGrams(),
                        psilocybin.phase().name().toLowerCase(java.util.Locale.ROOT),
                        psilocybin.intensity(),
                        psilocybin.remainingTicks() / 20));
            }
            LsdEffectState lsd = ClientExperienceStore.lsd();
            if (lsd.active()) {
                event.getLeft().add("  lsd=" + String.format(java.util.Locale.ROOT, "%.0fug t%d %s %.2f %ds",
                        lsd.displayDoseMicrograms(),
                        lsd.tier(),
                        lsd.phase().name().toLowerCase(java.util.Locale.ROOT),
                        lsd.intensity(),
                        lsd.remainingTicks() / 20));
            }
            DmtEffectState dmt = ClientExperienceStore.dmt();
            if (dmt.active()) {
                event.getLeft().add("  dmt=" + String.format(java.util.Locale.ROOT, "%.1fx t%d %s/%s %.2f %ds",
                        dmt.displayDoseUnits(),
                        dmt.tier(),
                        dmt.phase().name().toLowerCase(java.util.Locale.ROOT),
                        dmt.stage().name().toLowerCase(java.util.Locale.ROOT),
                        dmt.intensity(),
                        dmt.remainingTicks() / 20));
            }
            CannabisEffectState cannabis = ClientExperienceStore.cannabis();
            if (cannabis.active()) {
                event.getLeft().add("  cannabis=" + String.format(java.util.Locale.ROOT, "%.0fmg %s %.2f drag=%.2f tunnel=%.2f %ds",
                        cannabis.displayThcMilligrams(),
                        cannabis.phase().name().toLowerCase(java.util.Locale.ROOT),
                        cannabis.intensity(),
                        cannabis.temporalDrag(),
                        cannabis.focusTunnel(),
                        cannabis.remainingTicks() / 20));
            }
            TwoCbEffectState twoCb = ClientExperienceStore.twoCb();
            if (twoCb.active()) {
                event.getLeft().add("  2cb=" + String.format(java.util.Locale.ROOT, "%.0fmg t%d %s %.2f edge=%.2f crystal=%.2f %ds",
                        twoCb.displayDoseMilligrams(),
                        twoCb.tier(),
                        twoCb.phase().name().toLowerCase(java.util.Locale.ROOT),
                        twoCb.intensity(),
                        twoCb.edgeGlow(),
                        twoCb.crystalline(),
                        twoCb.remainingTicks() / 20));
            }
            KetamineEffectState ketamine = ClientExperienceStore.ketamine();
            if (ketamine.active()) {
                event.getLeft().add("  ketamine=" + String.format(java.util.Locale.ROOT, "%.0fmg t%d %s %.2f lag=%.2f depth=%.2f %ds",
                        ketamine.displayDoseMilligrams(),
                        ketamine.tier(),
                        ketamine.phase().name().toLowerCase(java.util.Locale.ROOT),
                        ketamine.intensity(),
                        ketamine.cameraLag(),
                        ketamine.depthMismatch(),
                        ketamine.remainingTicks() / 20));
            }
            HeroinEffectState heroin = ClientExperienceStore.heroin();
            if (heroin.active()) {
                event.getLeft().add("  heroin=" + String.format(java.util.Locale.ROOT, "%.0fmg t%d %s %.2f nod=%.2f relief=%.2f %ds",
                        heroin.displayDoseMilligrams(),
                        heroin.tier(),
                        heroin.phase().name().toLowerCase(java.util.Locale.ROOT),
                        heroin.intensity(),
                        heroin.nodding(),
                        heroin.painSuppression(),
                        heroin.remainingTicks() / 20));
            }
            SalviaEffectState salvia = ClientExperienceStore.salvia();
            if (salvia.active()) {
                event.getLeft().add("  salvia=" + String.format(java.util.Locale.ROOT, "%s t%d %s %.2f slip=%.2f scene=%.2f %ds",
                    SalviaDoseCurve.formatPotencyLabel(salvia.displayDoseUnits()),
                        salvia.tier(),
                        salvia.phase().name().toLowerCase(java.util.Locale.ROOT),
                        salvia.intensity(),
                        salvia.anchorSlip(),
                        salvia.sceneReplacement(),
                        salvia.remainingTicks() / 20));
            }
            AlcoholEffectState alcohol = ClientExperienceStore.alcohol();
            if (alcohol.active()) {
                event.getLeft().add("  alcohol=" + String.format(java.util.Locale.ROOT, "%.2f std t%d %s %.2f diplopia=%.2f blur=%.2f %ds",
                        alcohol.displayStandardDrinks(),
                        alcohol.tier(),
                        alcohol.phase().name().toLowerCase(java.util.Locale.ROOT),
                        alcohol.intensity(),
                        alcohol.diplopia(),
                        alcohol.focusBlur(),
                        alcohol.remainingTicks() / 20));
            }
            RecoveryState alcoholRecovery = ClientExperienceStore.alcoholRecovery();
            if (alcoholRecovery.active()) {
                event.getLeft().add("  alcohol_recovery=" + String.format(java.util.Locale.ROOT, "phase=%d/%d hangover=%.2f withdrawal=%.2f reset=%ds",
                        alcoholRecovery.dependencyPhase(),
                        alcoholRecovery.maxDependencyPhase(),
                        alcoholRecovery.hangoverSeverity(),
                        alcoholRecovery.withdrawalSeverity(),
                        alcoholRecovery.resetTicksRemaining() / 20));
            }
            var heroinRecovery = ClientExperienceStore.heroinRecovery();
            if (heroinRecovery.active()) {
                event.getLeft().add("  opioid_recovery=" + String.format(java.util.Locale.ROOT, "tol=%d/%d dep=%d/%d crash=%.2f withdrawal=%.2f food=%.2f reset=%ds",
                        heroinRecovery.tolerancePhase(),
                        heroinRecovery.maxTolerancePhase(),
                        heroinRecovery.dependencePhase(),
                        heroinRecovery.maxDependencePhase(),
                        heroinRecovery.crashSeverity(),
                        heroinRecovery.withdrawalSeverity(),
                        heroinRecovery.nutritionPenaltySeverity(),
                        heroinRecovery.resetTicksRemaining() / 20));
            }
    }

    private static double psilocybinGenericMotion(PsilocybinEffectState psilocybin) {
        return 0.0D;
    }

    private static double dmtGenericMotion(DmtEffectState dmt) {
        return switch (dmt.phase()) {
            case RUSH -> 0.42D;
            case TUNNEL -> 0.58D;
            case BREAKTHROUGH -> 0.70D;
            case RETURN -> 0.34D;
            case AFTERGLOW -> 0.10D;
            case SOBER -> 0.0D;
        };
    }

    private static double cannabisVisualLoad(CannabisEffectState cannabis) {
        if (cannabis == null || !cannabis.active()) {
            return 0.0D;
        }
        double load = Math.log1p(Math.max(0.0D, cannabis.intensity())) / Math.log1p(18.0D);
        load += smoothstep(1.0D, 20.0D, cannabis.doseScale()) * 0.08D;
        return clamp(load);
    }

    private static double cannabisMotionGain(CannabisEffectState cannabis) {
        double gain = cannabisVisualLoad(cannabis);
        if (TrippyClientConfig.motionReduction) {
            gain *= 0.58D;
        }
        return gain;
    }

    private static double ketamineVisualLoad(KetamineEffectState ketamine) {
        if (ketamine == null || !ketamine.active()) {
            return 0.0D;
        }
        double presence = KetamineMasking.visualPresence(ketamine);
        double realityBend = KetamineMasking.realityBend(ketamine);
        double load = Math.log1p(Math.max(0.0D, ketamine.intensity() * (1.08D + presence * 0.96D + realityBend * 0.34D))) / Math.log1p(6.4D);
        load += presence * 0.34D + realityBend * 0.26D;
        load += clamp((ketamine.depthMismatch() + ketamine.bodyEcho() * 0.60D) / 8.0D) * (0.08D + presence * 0.14D + realityBend * 0.04D);
        return clamp(load);
    }

    private static double heroinVisualLoad(HeroinEffectState heroin) {
        if (heroin == null || !heroin.active()) {
            return 0.0D;
        }
        double load = Math.log1p(Math.max(0.0D, heroin.intensity())) / Math.log1p(8.2D);
        load += clamp((heroin.activeDoseMilligrams() - 25.0D) / 475.0D) * 0.10D;
        load += clamp(heroin.warmth() / 8.2D) * 0.18D;
        load += clamp((heroin.nodding() + heroin.microSleep()) / 8.2D) * 0.08D;
        return clamp(load);
    }

    private static double ketamineMotionGain(KetamineEffectState ketamine) {
        double gain = ketamineVisualLoad(ketamine);
        if (TrippyClientConfig.motionReduction) {
            gain *= 0.60D;
        }
        return gain;
    }

    private static double heroinMotionGain(HeroinEffectState heroin) {
        if (heroin == null || !heroin.active()) {
            return 0.0D;
        }
        double dose = smoothstep(25.0D, 500.0D, Math.max(heroin.activeDoseMilligrams(), heroin.targetDoseMilligrams()));
        double load = clamp(heroin.intensity() / 8.2D * 0.32D
                + heroin.motorDrag() / 5.2D * 0.30D
                + heroin.sedation() / 8.2D * 0.22D
                + (heroin.nodding() + heroin.microSleep()) / 8.2D * 0.24D
                + dose * 0.18D);
        if (TrippyClientConfig.motionReduction) {
            load *= 0.46D;
        }
        return load;
    }

    private static double ketamineOutOfBodyPull(KetamineEffectState ketamine) {
        if (ketamine == null || !ketamine.active()) {
            return 0.0D;
        }
        double dose = smoothstep(250.0D, 350.0D, Math.max(ketamine.activeDoseMilligrams(), ketamine.targetDoseMilligrams()));
        double depth = clamp(ketamine.depthMismatch() / 2.4D);
        double floatiness = clamp(ketamine.bodyFloat() / 2.2D);
        double fade = clamp(ketamine.uiFade() / 2.2D);
        return clamp(dose * (0.42D + depth * 0.24D + floatiness * 0.16D + fade * 0.18D));
    }

    private static void applyKetamineCameraPresentation(ViewportEvent event, KetamineEffectState ketamine, float finalYaw, float finalPitch, float time) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || ketamine == null) {
            restoreKetamineCameraType();
            return;
        }

        double dose = Math.max(ketamine.activeDoseMilligrams(), ketamine.targetDoseMilligrams());
        boolean shouldForceThirdPerson = ketamine.active() && dose >= 350.0D;
        CameraType currentType = minecraft.options.getCameraType();
        if (shouldForceThirdPerson) {
            if (!ketamineForcedThirdPerson && currentType.isFirstPerson()) {
                previousKetamineCameraType = currentType;
                ketamineForcedThirdPerson = true;
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                currentType = minecraft.options.getCameraType();
            }
        } else {
            restoreKetamineCameraType();
            currentType = minecraft.options.getCameraType();
        }

        double targetPull = ketamine.active() ? ketamineOutOfBodyPull(ketamine) : 0.0D;
        ketamineCameraPull += (targetPull - ketamineCameraPull) * (targetPull > ketamineCameraPull ? 0.05D : 0.12D);
        if (!ketamine.active() || !currentType.isFirstPerson() || ketamineCameraPull <= 0.001D) {
            return;
        }

        Vec3 base = event.getCamera().getPosition();
        Vec3 look = Vec3.directionFromRotation(finalPitch, finalYaw);
        Vec3 side = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 1.0E-5D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        double floatiness = clamp(ketamine.bodyFloat() / 2.2D);
        double depth = clamp(ketamine.depthMismatch() / 2.4D);
        double drift = clamp(ketamine.gravityDrift() / 2.3D);
        double presentationTime = time * 0.28D + ketamine.ageTicks() * 0.08D + dose * 0.0016D;
        double targetSway = Math.sin(presentationTime * (0.0060D + drift * 0.0024D) + 0.8D)
            * (0.04D + drift * 0.16D + floatiness * 0.10D);
        double swaySmoothing = TrippyClientConfig.motionReduction ? 0.18D : 0.060D;
        ketaminePresentationSway += (targetSway - ketaminePresentationSway) * swaySmoothing;
        double lift = 0.04D + ketamineCameraPull * 0.24D + floatiness * 0.10D;
        double distance = 0.12D + ketamineCameraPull * (1.10D + depth * 0.60D);
        Vec3 offset = look.scale(-distance)
                .add(0.0D, lift, 0.0D)
                .add(side.scale(ketaminePresentationSway));
        setCameraPosition(event.getCamera(), base.add(offset));
    }

    private static Vec3 ketamineVrTranslation(KetamineEffectState ketamine, float yaw, float pitch, float time) {
        double targetPull = ketamine.active() ? ketamineOutOfBodyPull(ketamine) : 0.0D;
        ketamineCameraPull += (targetPull - ketamineCameraPull) * (targetPull > ketamineCameraPull ? 0.05D : 0.12D);
        if (!ketamine.active() || ketamineCameraPull <= 0.001D) {
            return Vec3.ZERO;
        }
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        double dose = Math.max(ketamine.activeDoseMilligrams(), ketamine.targetDoseMilligrams());
        if (dose >= 350.0D) {
            // Desktop selects THIRD_PERSON_BACK (four-block desired distance).
            // Preserve that visual pull without changing Vivecraft's camera mode;
            // the compositor cannot invent occluded surfaces or the hidden avatar.
            return look.scale(-4.0D);
        }
        Vec3 side = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        side = side.lengthSqr() < 1.0E-5D ? new Vec3(1.0D, 0.0D, 0.0D) : side.normalize();
        double floatiness = clamp(ketamine.bodyFloat() / 2.2D);
        double depth = clamp(ketamine.depthMismatch() / 2.4D);
        double drift = clamp(ketamine.gravityDrift() / 2.3D);
        double presentationTime = time * 0.28D + ketamine.ageTicks() * 0.08D + dose * 0.0016D;
        double targetSway = Math.sin(presentationTime * (0.0060D + drift * 0.0024D) + 0.8D)
                * (0.04D + drift * 0.16D + floatiness * 0.10D);
        ketaminePresentationSway += (targetSway - ketaminePresentationSway)
                * (TrippyClientConfig.motionReduction ? 0.18D : 0.060D);
        double lift = 0.04D + ketamineCameraPull * 0.24D + floatiness * 0.10D;
        double distance = 0.12D + ketamineCameraPull * (1.10D + depth * 0.60D);
        return look.scale(-distance).add(0.0D, lift, 0.0D).add(side.scale(ketaminePresentationSway));
    }

    private static void resetCameraMotion() {
        vrCameraMotion = VrCameraMotion.IDENTITY;
        restoreKetamineCameraType();
        cannabisYawLag = cannabisPitchLag = 0.0D;
        ketamineYawLag = ketaminePitchLag = 0.0D;
        heroinYawLag = heroinPitchLag = 0.0D;
        cannabisRenderYaw = cannabisRenderPitch = 0.0D;
        ketamineRenderYaw = ketamineRenderPitch = 0.0D;
        heroinRenderYaw = heroinRenderPitch = 0.0D;
        cannabisRenderLagInitialized = false;
        ketamineRenderLagInitialized = false;
        heroinRenderLagInitialized = false;
        lastCannabisYaw = lastCannabisPitch = Float.NaN;
        lastCocaineYaw = lastCocainePitch = Float.NaN;
        lastKetamineYaw = lastKetaminePitch = Float.NaN;
        lastHeroinYaw = lastHeroinPitch = Float.NaN;
        ketamineCameraPull = ketaminePresentationSway = 0.0D;
        ketamineSwayYaw = ketamineSwayPitch = ketamineSwayRoll = 0.0D;
        heroinSwayYaw = heroinSwayPitch = heroinSwayRoll = 0.0D;
        resetFovMotion();
    }

    private static void resetFovMotion() {
        smoothedDmtFovFactor = 1.0D;
        smoothedDmtFovSeed = Long.MIN_VALUE;
        smoothedCannabisFovFactor = 1.0D;
        smoothedKetamineFovFactor = 1.0D;
        lastCannabisFovNanos = -1L;
        lastKetamineFovNanos = -1L;
    }

    private static void restoreKetamineCameraType() {
        if (!ketamineForcedThirdPerson) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.setCameraType(previousKetamineCameraType != null ? previousKetamineCameraType : CameraType.FIRST_PERSON);
        ketamineForcedThirdPerson = false;
        previousKetamineCameraType = null;
    }

    private static void setCameraPosition(Camera camera, Vec3 position) {
        Method method = ketamineCameraSetPositionMethod();
        if (camera == null || method == null) {
            return;
        }
        try {
            method.invoke(camera, position);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static Method ketamineCameraSetPositionMethod() {
        if (cameraSetPositionLookupAttempted) {
            return cameraSetPositionMethod;
        }
        cameraSetPositionLookupAttempted = true;
        try {
            Method method = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
            method.setAccessible(true);
            cameraSetPositionMethod = method;
        } catch (ReflectiveOperationException ignored) {
            cameraSetPositionMethod = null;
        }
        return cameraSetPositionMethod;
    }

    private static double cannabisMixedPresence(ClientFamilyBlend familyBlend) {
        if (familyBlend == null) {
            return 0.0D;
        }
        double mixed = 0.0D;
        for (SubstanceFamily family : SubstanceFamily.values()) {
            if (family != SubstanceFamily.CANNABINOID) {
                mixed += familyBlend.strength(family);
            }
        }
        return clamp(mixed);
    }

    private static double smoothCannabisFovFactor(double target, boolean active) {
        if (!active) {
            lastCannabisFovNanos = -1L;
            smoothedCannabisFovFactor += (1.0D - smoothedCannabisFovFactor) * 0.30D;
            return smoothedCannabisFovFactor;
        }
        long now = System.nanoTime();
        if (lastCannabisFovNanos < 0L) {
            lastCannabisFovNanos = now;
            smoothedCannabisFovFactor = target;
            return smoothedCannabisFovFactor;
        }
        double deltaSeconds = (now - lastCannabisFovNanos) / 1_000_000_000.0D;
        lastCannabisFovNanos = now;
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0D || deltaSeconds > 0.25D) {
            smoothedCannabisFovFactor = target;
            return smoothedCannabisFovFactor;
        }
        double follow = 1.0D - Math.exp(-Math.max(0.001D, deltaSeconds) * 5.4D);
        smoothedCannabisFovFactor += (target - smoothedCannabisFovFactor) * follow;
        return smoothedCannabisFovFactor;
    }

    private static double smoothKetamineFovFactor(double target, boolean active) {
        if (!active) {
            lastKetamineFovNanos = -1L;
            smoothedKetamineFovFactor += (1.0D - smoothedKetamineFovFactor) * 0.24D;
            return smoothedKetamineFovFactor;
        }
        long now = System.nanoTime();
        if (lastKetamineFovNanos < 0L) {
            lastKetamineFovNanos = now;
            smoothedKetamineFovFactor = target;
            return smoothedKetamineFovFactor;
        }
        double deltaSeconds = (now - lastKetamineFovNanos) / 1_000_000_000.0D;
        lastKetamineFovNanos = now;
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0D || deltaSeconds > 0.25D) {
            smoothedKetamineFovFactor = target;
            return smoothedKetamineFovFactor;
        }
        double follow = 1.0D - Math.exp(-Math.max(0.001D, deltaSeconds) * 4.2D);
        smoothedKetamineFovFactor += (target - smoothedKetamineFovFactor) * follow;
        return smoothedKetamineFovFactor;
    }

    private static double salviaVisualLoad(SalviaEffectState salvia) {
        return clamp(salvia.visualIntensity() / Math.max(1.0D, Math.max(TrippyConfig.salviaMaxVisualIntensity, SalviaDoseCurve.safeVisualIntensity())));
    }

    private static boolean dmtScaffoldVisible(DmtEffectState dmt) {
        if (!dmt.active()) {
            smoothedDmtFovFactor = 1.0D;
            smoothedDmtFovSeed = Long.MIN_VALUE;
            return false;
        }
        return dmtScaffoldPresence(dmt) > 0.004D;
    }

    private static double dmtScaffoldPresence(DmtEffectState dmt) {
        var scaffold = dmt.fractalScaffold();
        return clamp(scaffold.fractal3dPresence() * 0.42D
                + scaffold.animatedFractalPresence() * 0.38D
                + scaffold.staticAnimatedCoupling() * 0.44D
                + scaffold.alternateWorldReadiness() * 0.58D
                + scaffold.dimensionalDepth() * 0.22D);
    }

    private static double dmtTunnelFovFactor(DmtEffectState dmt, double gain) {
        var scaffold = dmt.fractalScaffold();
        double progress = dmt.progress();
        double tunnel = smoothstep(0.035D, 0.24D, progress) * (1.0D - smoothstep(0.76D, 0.96D, progress));
        double breakthrough = smoothstep(0.13D, 0.34D, progress) * (1.0D - smoothstep(0.76D, 0.92D, progress));
        double reverse = smoothstep(0.62D, 0.88D, progress) * (1.0D - smoothstep(0.97D, 1.0D, progress));
        double doseDepth = smoothstep(0.45D, 4.50D, dmt.activeDoseUnits());
        double doseOverdrive = smoothstep(4.00D, 15.00D, dmt.activeDoseUnits());
        double impossible = Math.min(1.0D, (smoothstep(1.70D, 5.20D, dmt.activeDoseUnits()) + doseOverdrive * 0.55D) * breakthrough);
        double drive = clamp((scaffold.animatedFractalPresence() * 0.34D
                + scaffold.staticAnimatedCoupling() * 0.36D
                + scaffold.alternateWorldReadiness() * 0.44D
                + scaffold.dimensionalDepth() * 0.30D)
                * (0.86D + doseDepth * 0.86D + doseOverdrive * 0.76D)
                * Math.max(tunnel, breakthrough * 0.92D)
                * gain);
        double deepest = mix(0.48D, 0.25D, impossible);
        double target = mix(1.0D, deepest, drive);
        target = mix(target, 1.12D + doseDepth * 0.24D + doseOverdrive * 0.24D, reverse * Math.max(drive, 0.35D) * 0.92D);
        long seed = dmt.signature().seed();
        if (smoothedDmtFovSeed != seed || dmt.ageTicks() <= 2) {
            smoothedDmtFovFactor = 1.0D;
            smoothedDmtFovSeed = seed;
        }
        double rate = target < smoothedDmtFovFactor ? 0.34D : 0.18D;
        smoothedDmtFovFactor += (target - smoothedDmtFovFactor) * rate;
        if (!Double.isFinite(smoothedDmtFovFactor)) {
            smoothedDmtFovFactor = 1.0D;
        }
        return Math.max(0.25D, Math.min(1.62D, smoothedDmtFovFactor));
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double alcoholMotionGain(AlcoholEffectState state) {
        if (state == null || !state.active()) {
            return 0.0D;
        }
        double sway = alcoholLoad(state, state.vestibularSway());
        double motor = alcoholLoad(state, state.motorNoise());
        double focus = alcoholLoad(state, state.focusBlur());
        double heavyHead = smoothstep(12.0D, 18.0D, state.activeStandardDrinks());
        double gain = clamp(sway * 0.52D + motor * 0.30D + focus * 0.14D + heavyHead * 0.34D);
        if (TrippyConfig.alcoholAccessibilityMode || TrippyClientConfig.motionReduction) {
            gain *= 0.36D;
        }
        return gain;
    }

    private static double alcoholLoad(AlcoholEffectState state, double value) {
        if (state == null || !state.active()) {
            return 0.0D;
        }
        double safe = Math.max(1.0D, TrippyConfig.alcoholMaxVisualIntensity);
        return clamp(value / safe);
    }

    private static double alcoholHangoverLoad(RecoveryState recovery, AlcoholEffectState alcohol) {
        if (recovery == null || !recovery.hangoverActive()) {
            return 0.0D;
        }
        double activeDrinks = alcohol != null && alcohol.active() ? alcohol.activeStandardDrinks() : 0.0D;
        double mask = 1.0D - smoothstep(0.20D, 1.25D, activeDrinks);
        double remaining = recovery.hangoverDurationTicks() <= 0 ? 0.0D : recovery.hangoverTicks() / (double) recovery.hangoverDurationTicks();
        return clamp(recovery.hangoverSeverity() * Math.sqrt(Math.max(0.0D, remaining)) * mask);
    }

    private static double alcoholWithdrawalLoad(RecoveryState recovery) {
        if (recovery == null) {
            return 0.0D;
        }
        return clamp(recovery.withdrawalSeverity());
    }

    private static double smoothPulse(double value) {
        double shaped = Math.max(0.0D, Math.min(1.0D, (value - 0.72D) / 0.28D));
        shaped = shaped * shaped * (3.0D - 2.0D * shaped);
        return shaped * shaped;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * Math.max(0.0D, Math.min(1.0D, t));
    }
}
