package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.SubstanceFamily;
import com.trippycraft.substance.cannabis.CannabisEffectState;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import com.trippycraft.substance.ketamine.KetamineMasking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClientEffectGraph {
    private static long lastCannabisClockNanos = -1L;
    private static double smoothCannabisTicks;
    private static ClientLevel vrClockLevel;
    private static double vrClockFrame = Double.NaN;
    private static float vrClockTime;
    private static ClientLevel lsdReservationLevel;
    private static boolean lsdPresentationReserved;

    private ClientEffectGraph() {
    }

    public static ClientEffectSample sample(float partialTick) {
        return sample(partialTick, false);
    }

    /** Audio uses the experience channels independently of visual settings. */
    static ClientEffectSample audioSample(float partialTick) {
        return sample(partialTick, true);
    }

    private static ClientEffectSample sample(float partialTick, boolean audio) {
        ClientEffectState state = ClientExperienceStore.state();
        ClientFamilyBlend familyBlend = state.sampleFamilyBlend(partialTick);
        ClientRenderSignature signature = ClientRenderSignature.from(familyBlend);
        CannabisEffectState cannabis = ClientExperienceStore.cannabis();
        KetamineEffectState ketamine = ClientExperienceStore.ketamine();
        double cannabisLoad = cannabisVisualLoad(cannabis);
        double ketaminePresence = KetamineMasking.visualPresence(ketamine);
        double visualScale = audio ? 1.0D : TrippyClientConfig.rendererEnabled ? TrippyClientConfig.visualIntensity : 0.0D;
        double motionScale = audio ? 1.0D : TrippyClientConfig.rendererEnabled ? TrippyClientConfig.motionIntensity : 0.0D;

        double visualDrift = state.sample(ExperienceChannel.VISUAL_DRIFT, partialTick);
        double geometryMelt = state.sample(ExperienceChannel.GEOMETRY_MELT, partialTick);
        double patterning = state.sample(ExperienceChannel.PATTERNING, partialTick);
        double colorBloom = state.sample(ExperienceChannel.COLOR_BLOOM, partialTick);
        double depthBreathing = state.sample(ExperienceChannel.DEPTH_BREATHING, partialTick);
        double timeDilation = state.sample(ExperienceChannel.TIME_DILATION, partialTick);
        double egoDissolution = state.sample(ExperienceChannel.EGO_DISSOLUTION, partialTick);
        double bodyLoad = state.sample(ExperienceChannel.BODY_LOAD, partialTick);
        double motorNoise = state.sample(ExperienceChannel.MOTOR_NOISE, partialTick);
        double stimulation = state.sample(ExperienceChannel.STIMULATION, partialTick);
        double sedation = state.sample(ExperienceChannel.SEDATION, partialTick);
        double euphoria = state.sample(ExperienceChannel.EUPHORIA, partialTick);
        double anxiety = state.sample(ExperienceChannel.ANXIETY, partialTick);
        double dissociation = state.sample(ExperienceChannel.DISSOCIATION, partialTick);
        double nausea = state.sample(ExperienceChannel.NAUSEA, partialTick);
        double craving = state.sample(ExperienceChannel.CRAVING, partialTick);
        double risk = state.sample(ExperienceChannel.RISK, partialTick);

        if (cannabis.active()) {
            double warmth = saturate(cannabis.warmthShift() * 0.18D);
            double saturation = saturate(cannabis.saturationBoost() * 0.14D);
            double greens = saturate(cannabis.greenRichness() * 0.14D);
            double breath = saturate(cannabis.breathingPulse() * 0.22D);
            double temporal = saturate(cannabis.temporalDrag() * 0.14D);
            double tunnel = saturate(cannabis.focusTunnel() * 0.18D);
            double sticky = saturate(cannabis.thoughtStickiness() * 0.16D);
            double immersion = saturate(cannabis.immersion() * 0.16D);

            colorBloom = saturate(colorBloom + cannabisLoad * 0.10D + warmth * 0.22D + saturation * 0.18D + greens * 0.12D);
            depthBreathing = saturate(depthBreathing + breath * 0.34D + cannabisLoad * 0.06D);
            timeDilation = saturate(timeDilation + temporal * 0.48D + sticky * 0.10D);
            bodyLoad = saturate(bodyLoad + immersion * 0.24D + tunnel * 0.10D);
            sedation = saturate(sedation + sticky * 0.18D + temporal * 0.12D + cannabisLoad * 0.06D);
            euphoria = saturate(euphoria + warmth * 0.14D + saturation * 0.08D);
            visualDrift = saturate(visualDrift + tunnel * 0.08D);
            patterning = saturate(patterning + greens * 0.04D);
        }

        if (ketamine.active() && ketaminePresence > 0.0D) {
            double realityBend = KetamineMasking.realityBend(ketamine);
            double dissoc = saturate(ketamine.dissociation() * 0.16D);
            double slip = saturate(ketamine.timeSlip() * 0.16D);
            double depth = saturate(ketamine.depthMismatch() * 0.18D);
            double floatiness = saturate(ketamine.bodyFloat() * 0.16D);
            double echo = saturate(ketamine.bodyEcho() * 0.18D);
            double fade = saturate(ketamine.uiFade() * 0.16D);

            visualDrift = saturate(visualDrift + ketaminePresence * (depth * 0.24D + echo * 0.18D + realityBend * 0.10D));
            geometryMelt = saturate(geometryMelt + ketaminePresence * (depth * 0.34D + realityBend * 0.16D));
            colorBloom = saturate(colorBloom + ketaminePresence * (fade * 0.10D));
            depthBreathing = saturate(depthBreathing + ketaminePresence * (floatiness * 0.24D + depth * 0.18D));
            timeDilation = saturate(timeDilation + ketaminePresence * (slip * 0.32D + fade * 0.08D));
            bodyLoad = saturate(bodyLoad + ketaminePresence * (floatiness * 0.16D + fade * 0.12D));
            dissociation = saturate(dissociation + ketaminePresence * (dissoc * 0.46D + echo * 0.16D + realityBend * 0.08D));
            sedation = saturate(sedation + ketaminePresence * (fade * 0.10D + floatiness * 0.08D));
        }

        visualDrift = saturate(visualDrift * (1.0D + signature.organicFlow() * 0.16D + signature.motorLooseness() * 0.09D + signature.depthSeparation() * 0.06D));
        geometryMelt = saturate(geometryMelt * (1.0D + signature.organicFlow() * 0.20D + signature.depthSeparation() * 0.12D));
        patterning = saturate(patterning * (1.0D + signature.patternOrder() * 0.20D));
        colorBloom = saturate(colorBloom * (1.0D + signature.warmth() * 0.14D + signature.patternOrder() * 0.08D));
        depthBreathing = saturate(depthBreathing * (1.0D + signature.organicFlow() * 0.18D + signature.depthSeparation() * 0.18D));
        timeDilation = saturate(timeDilation * (1.0D + signature.temporalSlip() * 0.22D + signature.motorLooseness() * 0.08D));
        egoDissolution = saturate(egoDissolution * (1.0D + signature.psychedelic() * 0.12D + signature.deliriant() * 0.14D));
        bodyLoad = saturate(bodyLoad * (1.0D + signature.narrowing() * 0.18D + signature.motorLooseness() * 0.12D));
        motorNoise = saturate(motorNoise * (1.0D + signature.edgeSharpness() * 0.26D + signature.motorLooseness() * 0.13D + signature.instability() * 0.10D));
        stimulation = saturate(stimulation * (1.0D + signature.edgeSharpness() * 0.18D));
        sedation = saturate(sedation * (1.0D + signature.narrowing() * 0.20D));
        euphoria = saturate(euphoria * (1.0D + signature.warmth() * 0.10D + signature.empathogen() * 0.12D));
        anxiety = saturate(anxiety * (1.0D + signature.edgeSharpness() * 0.10D + signature.instability() * 0.16D));
        dissociation = saturate(dissociation * (1.0D + signature.depthSeparation() * 0.22D + signature.temporalSlip() * 0.10D));
        nausea = saturate(nausea * (1.0D + signature.motorLooseness() * 0.08D + signature.narrowing() * 0.08D));
        craving = saturate(craving * (1.0D + signature.edgeSharpness() * 0.08D + signature.narrowing() * 0.08D));
        risk = saturate(risk * (1.0D + signature.instability() * 0.10D + signature.narrowing() * 0.12D));

        if (!audio && TrippyClientConfig.motionReduction) {
            visualDrift = Math.min(visualDrift, 0.35D);
            geometryMelt = Math.min(geometryMelt, 0.42D);
            depthBreathing = Math.min(depthBreathing, 0.34D);
            timeDilation = Math.min(timeDilation, 0.44D);
            dissociation = Math.min(dissociation, 0.46D);
            motorNoise = Math.min(motorNoise, 0.45D);
            motionScale *= 0.38D;
        }

        if (!audio && TrippyClientConfig.photosensitivityReduction) {
            patterning = Math.min(patterning, 0.38D);
            colorBloom = Math.min(colorBloom, 0.46D);
            euphoria = Math.min(euphoria, 0.50D);
            anxiety = Math.min(anxiety, 0.52D);
            visualScale *= 0.55D;
        }

        visualDrift = saturate(visualDrift * visualScale);
        geometryMelt = saturate(geometryMelt * visualScale);
        patterning = saturate(patterning * visualScale);
        colorBloom = saturate(colorBloom * visualScale);
        egoDissolution = saturate(egoDissolution * visualScale);
        euphoria = saturate(euphoria * visualScale);
        anxiety = saturate(anxiety * visualScale);
        dissociation = saturate(dissociation * Math.max(visualScale, motionScale));
        risk = saturate(risk * visualScale);

        depthBreathing = saturate(depthBreathing * motionScale);
        timeDilation = saturate(timeDilation * motionScale);
        bodyLoad = saturate(bodyLoad * motionScale);
        motorNoise = saturate(motorNoise * motionScale);
        stimulation = saturate(stimulation * motionScale);
        sedation = saturate(sedation * motionScale);
        nausea = saturate(nausea * motionScale);

        double visualLoad = saturate(max(visualDrift, geometryMelt, patterning, colorBloom, egoDissolution, euphoria, anxiety, dissociation, risk));
        double motionLoad = saturate(max(visualDrift, geometryMelt, depthBreathing, timeDilation, bodyLoad, motorNoise, stimulation, sedation, nausea, dissociation));
        double signaturePresence = saturate(visualLoad * 1.6D + motionLoad * 0.35D);
        double overlayAlpha = saturate(0.10D * colorBloom + 0.08D * patterning + 0.09D * visualDrift + 0.11D * anxiety + 0.07D * sedation + 0.06D * risk
                + (0.035D * signature.warmth() + 0.030D * signature.coolDistance() + 0.025D * signature.narrowing()) * signaturePresence);
        overlayAlpha = saturate(overlayAlpha + cannabisLoad * 0.040D);
        if (TrippyClientConfig.photosensitivityReduction) {
            overlayAlpha = Math.min(overlayAlpha, 0.16D);
        } else {
            overlayAlpha = Math.min(overlayAlpha, 0.26D);
        }

        return new ClientEffectSample(
                partialTick,
            clientTime(partialTick),
                familyBlend,
                signature,
                visualDrift,
                geometryMelt,
                patterning,
                colorBloom,
                depthBreathing,
                timeDilation,
                egoDissolution,
                bodyLoad,
                motorNoise,
                stimulation,
                sedation,
                euphoria,
                anxiety,
                dissociation,
                nausea,
                craving,
                risk,
                visualLoad,
                motionLoad,
                overlayAlpha
        );
    }

    public static List<String> debugLines(ClientEffectSample sample) {
        List<String> lines = new ArrayList<>();
        lines.add("Vice and Villagers visual=" + fmt(sample.visualLoad()) + " motion=" + fmt(sample.motionLoad()) + " age=" + ClientExperienceStore.ticksActive());
        if (sample.familyBlend().intensity() >= 0.04D) {
            lines.add("  family=" + sample.familyBlend().dominantLabel() + " blend=" + fmt(sample.familyBlend().dominantStrength()));
        }
        append(lines, "drift", sample.visualDrift());
        append(lines, "melt", sample.geometryMelt());
        append(lines, "pattern", sample.patterning());
        append(lines, "bloom", sample.colorBloom());
        append(lines, "time", sample.timeDilation());
        append(lines, "dissociation", sample.dissociation());
        append(lines, "anxiety", sample.anxiety());
        append(lines, "risk", sample.risk());
        append(lines, "sharp", sample.signature().edgeSharpness());
        append(lines, "narrow", sample.signature().narrowing());
        append(lines, "depth", sample.signature().depthSeparation());
        var hoMet = ClientExperienceStore.hoMet();
        if (hoMet.active()) {
            lines.add("  4-HO-MET depth=" + fmt(hoMet.spatialDepth()) + " trails=" + fmt(hoMet.trails())
                    + " recursion=" + fmt(hoMet.surfaceRecursion()) + " complexity=" + fmt(hoMet.complexity()));
        }
        var alLad = ClientExperienceStore.alLad();
        if (alLad.active()) {
            lines.add("  AL-LAD glass=" + fmt(alLad.stainedGlass()) + " prism=" + fmt(alLad.prismDepth())
                    + " recursion=" + fmt(alLad.cathedralRecursion()));
        }
        var twoCe = ClientExperienceStore.twoCe();
        if (twoCe.active()) {
            lines.add("  2C-E cavities=" + fmt(twoCe.cavities()) + " rooms=" + fmt(twoCe.nestedRooms())
                    + " recursion=" + fmt(twoCe.recursion()) + " corridor=" + fmt(twoCe.corridorZoom()));
        }
        return lines;
    }

    /** Reserve native world-shader shares for actual surfaces, rather than duplicating them
     * as generic camera sway, FOV pulses, fog tint and a full-screen color wash. */
    static double genericPresentationGain(ClientEffectSample sample) {
        var hoMet = ClientExperienceStore.hoMet();
        var alLad = ClientExperienceStore.alLad();
        var twoCe = ClientExperienceStore.twoCe();
        var psilocybin = ClientExperienceStore.psilocybin();
        var lsd = ClientExperienceStore.lsd();
        ClientLevel level = Minecraft.getInstance().level;
        if (lsdReservationLevel != level) {
            lsdReservationLevel = level;
            lsdPresentationReserved = false;
        }
        if (!TrippyConfig.enableLsdEffects) {
            lsdPresentationReserved = false;
        } else if (lsd.active()) {
            lsdPresentationReserved = true;
        } else if (sample.familyBlend().strength(SubstanceFamily.PSYCHEDELIC) <= 0.0D
                || ClientExperienceStore.ticksActive() == 0) {
            lsdPresentationReserved = false;
        }
        boolean otherPsychedelic = psilocybin.active() || hoMet.active() || alLad.active() || twoCe.active()
                || ClientExperienceStore.dmt().active() || ClientExperienceStore.mescaline().active()
                || ClientExperienceStore.twoCb().active();
        if (!lsd.active() && otherPsychedelic) lsdPresentationReserved = false;
        if (lsdPresentationReserved && !otherPsychedelic) {
            // Family channels can outlive the native session during afterglow.
            // Keep its attribution through that fade rather than suddenly restoring
            // generic camera pulses, fog and filters when active flips to false.
            // No visibility epsilon: tiny residuals must not turn the lens back on.
            double total = 0.0D;
            for (SubstanceFamily family : SubstanceFamily.values()) total += sample.familyBlend().strength(family);
            return total > 0.0D ? 1.0D - sample.familyBlend().strength(SubstanceFamily.PSYCHEDELIC) / total : 0.0D;
        }
        if ((!lsd.active() || !TrippyConfig.enableLsdEffects)
                && (!psilocybin.active() || !TrippyConfig.enablePsilocybinEffects)
                && !hoMet.active() && !alLad.active() && !twoCe.active()) return 1.0D;
        var blend = CompositeEffectResolver.resolve(psilocybin,
                ClientExperienceStore.lsd(), ClientExperienceStore.dmt(), hoMet, alLad, twoCe);
        double spatial = blend.psilocybinLoad() + blend.lsdLoad() + blend.hoMetLoad() + blend.alLadLoad() + blend.twoCeLoad();
        double other = blend.dmtLoad()
                + ClientExperienceStore.mescaline().visualPresence()
                + (ClientExperienceStore.twoCb().active() ? saturate(ClientExperienceStore.twoCb().intensity()) : 0.0D);
        if (spatial + other <= 0.00001D) return 1.0D;
        double share = spatial / (spatial + other) * sample.familyBlend().weight(SubstanceFamily.PSYCHEDELIC);
        return 1.0D - saturate(share);
    }

    static boolean reservesLsdPresentation() { return lsdPresentationReserved; }

    static double saturate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    static float clientTime(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (VivecraftCompat.isVrActive() && minecraft.level != null) {
            // The same artistic clock is sampled by fog, geometry and overlays.
            // Advance it once on the first world pass, then freeze it for every
            // subsequent callback/eye/mirror belonging to this stereo frame.
            double frame = (double) minecraft.level.getGameTime()
                    + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
            if (vrClockLevel == minecraft.level && Double.isFinite(vrClockFrame)
                    && (!VivecraftCompat.isFirstWorldPass() || frame == vrClockFrame)) {
                return vrClockTime;
            }
            vrClockLevel = minecraft.level;
            vrClockFrame = frame;
            vrClockTime = sampleClientTime(minecraft, partialTick);
            return vrClockTime;
        }
        vrClockLevel = null;
        vrClockFrame = Double.NaN;
        return sampleClientTime(minecraft, partialTick);
    }

    private static float sampleClientTime(Minecraft minecraft, float partialTick) {
        if (minecraft.level != null) {
            CannabisEffectState cannabis = ClientExperienceStore.cannabis();
            if (!cannabis.active()) {
                lastCannabisClockNanos = -1L;
                smoothCannabisTicks = minecraft.level.getGameTime() + partialTick;
                return minecraft.level.getGameTime() + partialTick;
            }
            return cannabisTime(minecraft, cannabis, partialTick);
        }
        lastCannabisClockNanos = -1L;
        smoothCannabisTicks = 0.0D;
        return ClientExperienceStore.ticksActive() + partialTick;
    }

    private static float cannabisTime(Minecraft minecraft, CannabisEffectState cannabis, float partialTick) {
        long now = System.nanoTime();
        double worldTicks = minecraft.level.getGameTime() + partialTick;
        if (lastCannabisClockNanos < 0L) {
            lastCannabisClockNanos = now;
            smoothCannabisTicks = worldTicks;
            return (float) smoothCannabisTicks;
        }

        double deltaSeconds = (now - lastCannabisClockNanos) / 1_000_000_000.0D;
        lastCannabisClockNanos = now;
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0D || deltaSeconds > 0.25D) {
            smoothCannabisTicks = worldTicks;
            return (float) smoothCannabisTicks;
        }

        double load = cannabisVisualLoad(cannabis);
        double lag = saturate(cannabis.temporalDrag() * 0.06D + cannabis.thoughtStickiness() * 0.04D + load * 0.12D);
        double responseHz = 8.5D - lag * 6.4D;
        double follow = 1.0D - Math.exp(-Math.max(0.001D, deltaSeconds) * responseHz);
        smoothCannabisTicks += (worldTicks - smoothCannabisTicks) * follow;
        if (smoothCannabisTicks < worldTicks - 10.0D || smoothCannabisTicks > worldTicks + 4.0D) {
            smoothCannabisTicks = worldTicks;
        }
        return (float) smoothCannabisTicks;
    }

    private static double cannabisVisualLoad(CannabisEffectState cannabis) {
        if (cannabis == null || !cannabis.active()) {
            return 0.0D;
        }
        double load = Math.log1p(Math.max(0.0D, cannabis.intensity())) / Math.log1p(18.0D);
        load += saturate((cannabis.doseScale() - 1.0D) / 19.0D) * 0.08D;
        return saturate(load);
    }

    private static double max(double... values) {
        double max = 0.0D;
        for (double value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static void append(List<String> lines, String label, double value) {
        if (value >= 0.04D) {
            lines.add("  " + label + "=" + fmt(value));
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
