package com.trippycraft.substance.dmt;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.ActiveDoseState;
import com.trippycraft.substance.SubstanceType;
import com.trippycraft.substance.psilocybin.PsilocybinEnvironmentSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class DmtSession {
    private static final int ENVIRONMENT_UPDATE_INTERVAL = 12;

    private double activeDoseUnits;
    private double targetDoseUnits;
    private double displayedDoseUnits;
    private int ageTicks;
    private int durationTicks;
    private int environmentTicks;
    private int lastAnnouncedTier;
    private double memoryVoid;
    private double memoryLight;
    private double memoryMotion;
    private double memoryShelter;
    private DmtTripSignature signature;
    private DmtEncounterDirector encounterDirector;
    private PsilocybinEnvironmentSnapshot environment = PsilocybinEnvironmentSnapshot.empty();

    private DmtSession(double doseUnits, int durationTicks, DmtTripSignature signature) {
        this.activeDoseUnits = doseUnits;
        this.targetDoseUnits = doseUnits;
        this.displayedDoseUnits = doseUnits;
        this.durationTicks = durationTicks;
        this.signature = signature;
        this.encounterDirector = DmtEncounterDirector.start(signature.seed(), doseUnits, signature, environment);
    }

    public static DmtSession start(ServerPlayer player, double doseUnits) {
        long seed = player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17)
                ^ player.level().getGameTime()
                ^ Double.doubleToLongBits(doseUnits)
                ^ 0xD1712EAD5EEDEL;
        DmtSession session = new DmtSession(doseUnits, DmtDoseCurve.baseDurationTicks(doseUnits), DmtTripSignature.create(seed));
        session.environment = PsilocybinEnvironmentSnapshot.capture(player);
        session.encounterDirector = DmtEncounterDirector.start(seed, doseUnits, session.signature, session.environment);
        return session;
    }

    public void stackDose(double addedDoseUnits) {
        targetDoseUnits += Math.max(0.0D, addedDoseUnits);
        int extension = (int) Math.round(DmtDoseCurve.baseDurationTicks(addedDoseUnits) * 0.42D);
        durationTicks = Math.max(durationTicks, ageTicks + extension);
        durationTicks += (int) Math.round(extension * DmtDoseCurve.clamp(targetDoseUnits / 4.0D, 0.0D, 0.30D));
    }

    public void tick(ServerPlayer player) {
        ageTicks++;
        double doseDelta = Math.abs(targetDoseUnits - activeDoseUnits);
        double onsetSpeed = Math.max(0.1D, TrippyConfig.dmtOnsetSpeed);
        double ramp = ((targetDoseUnits > activeDoseUnits ? 0.018D : 0.006D)
                + DmtDoseCurve.smoothstep(0.20D, 2.5D, doseDelta) * 0.020D) * onsetSpeed;
        activeDoseUnits += (targetDoseUnits - activeDoseUnits) * ramp;
        displayedDoseUnits += (targetDoseUnits - displayedDoseUnits) * 0.030D;
        if (Math.abs(targetDoseUnits - activeDoseUnits) < 0.002D) {
            activeDoseUnits = targetDoseUnits;
        }
        if (Math.abs(targetDoseUnits - displayedDoseUnits) < 0.002D) {
            displayedDoseUnits = targetDoseUnits;
        }

        environmentTicks--;
        if (environmentTicks <= 0) {
            PsilocybinEnvironmentSnapshot next = PsilocybinEnvironmentSnapshot.capture(player);
            environment = environment.blend(next, 0.30D + signature.portalBias() * 0.10D + signature.meltBias() * 0.05D);
            environmentTicks = ENVIRONMENT_UPDATE_INTERVAL;
        }

        memoryVoid = memory(memoryVoid, environment.end() + environment.cave() * 0.34D + environment.night() * 0.20D);
        memoryLight = memory(memoryLight, environment.light() + environment.sunriseSunset() * 0.28D + environment.nether() * 0.18D);
        memoryMotion = memory(memoryMotion, environment.moving() + environment.entities() * 0.30D);
        memoryShelter = memory(memoryShelter, environment.artificialBlocks() + environment.village() * 0.22D + environment.stillness() * 0.18D);
        announceTierChanges(player);
    }

    public DmtEffectState effectState() {
        double progress = progress();
        double safeVisual = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
        double doseIntensity = Math.min(DmtDoseCurve.peakIntensity(activeDoseUnits), safeVisual);
        int tier = DmtDoseCurve.tier(activeDoseUnits);
        ActiveDoseState doseState = new ActiveDoseState(
                SubstanceType.DMT,
                true,
                activeDoseUnits,
                targetDoseUnits,
                ageTicks,
                durationTicks,
                remainingTicks(),
                progress,
                tier,
                doseIntensity,
                Math.min(DmtDoseCurve.visualIntensity(activeDoseUnits), safeVisual)
        );
        DmtPipelineResult pipeline = DmtEffectPipeline.resolve(doseState, signature, environment, encounterDirector);
        DmtPhase phase = pipeline.phase();
        DmtStage stage = pipeline.stage();
        DmtLayerProfile layers = pipeline.layers();
        DmtEncounterState encounter = pipeline.encounter();
        double phaseEnvelope = DmtDoseCurve.phaseEnvelope(phase, progress);
        double tierRamp = DmtDoseCurve.clamp(tier / 5.0D, 0.0D, 1.0D);
        double entry = DmtDoseCurve.smoothstep(0.0D, 0.10D, progress);
        double peak = DmtDoseCurve.smoothstep(0.16D, 0.34D, progress) * (1.0D - DmtDoseCurve.smoothstep(0.64D, 0.82D, progress));
        double pulse = Math.sin(ageTicks * (0.145D + signature.tunnelSpeed() * 0.045D) + (signature.seed() & 2047L) * 0.006D) * 0.045D
                + Math.sin(ageTicks * (0.052D + signature.symmetry() * 0.018D) + signature.paletteStyle()) * (0.020D + tierRamp * 0.040D)
                + Math.sin(ageTicks * 0.270D + signature.hyperspaceStyle()) * DmtDoseCurve.smoothstep(2.2D, 4.0D, activeDoseUnits) * 0.025D;
        double living = DmtDoseCurve.clamp(1.0D + pulse, 0.86D, 1.18D);
        double intensity = DmtDoseCurve.clamp(doseIntensity * phaseEnvelope * living, 0.0D, safeVisual);
        double visual = Math.min(DmtDoseCurve.visualIntensity(activeDoseUnits) * phaseEnvelope * living, safeVisual);

        double tier1 = DmtDoseCurve.smoothstep(0.20D, 0.55D, activeDoseUnits);
        double tier2 = DmtDoseCurve.smoothstep(0.70D, 1.05D, activeDoseUnits);
        double tier3 = DmtDoseCurve.smoothstep(1.20D, 1.85D, activeDoseUnits);
        double tier4 = DmtDoseCurve.smoothstep(2.00D, 2.85D, activeDoseUnits);
        double tier5 = DmtDoseCurve.smoothstep(3.20D, 4.60D, activeDoseUnits);
        double transitionStage = stage == DmtStage.BREAKTHROUGH_TRANSITION ? 1.0D : 0.0D;
        double postStage = stage == DmtStage.POST_BREAKTHROUGH ? 1.0D : 0.0D;
        double returnStage = stage == DmtStage.RETURN ? 1.0D : 0.0D;
        double extremeDose = DmtDoseCurve.smoothstep(TrippyConfig.dmtTier5Units * 0.95D, TrippyConfig.dmtTier5Units * 1.55D, activeDoseUnits);
        double layerSplit = DmtDoseCurve.clamp(layers.spatialInstability() * 0.42D + layers.veilThinness() * 0.58D, 0.0D, 1.35D);
        double constructedRealm = DmtDoseCurve.clamp(layers.impossibleArchitecture() * 0.50D
                + layers.symbolLanguage() * 0.26D
                + layers.realityReplacement() * 0.58D
                + encounter.themeStrength() * 0.22D, 0.0D, 1.65D);
        double fractalAuthority = DmtDoseCurve.clamp(1.0D - postStage * 0.46D - extremeDose * 0.22D + transitionStage * 0.12D, 0.30D, 1.0D);
        double voidEnv = DmtDoseCurve.clamp(environment.end() * 0.58D + environment.cave() * 0.24D + memoryVoid * 0.22D, 0.0D, 1.35D);
        double lightEnv = DmtDoseCurve.clamp(environment.light() * 0.36D + environment.sunriseSunset() * 0.26D + memoryLight * 0.20D, 0.0D, 1.35D);
        double motionEnv = DmtDoseCurve.clamp(environment.moving() * 0.34D + environment.entities() * 0.24D + memoryMotion * 0.16D, 0.0D, 1.35D);

        double chrysanthemum = intensity * tier1 * (0.62D + signature.mandalaDensity() * 0.66D + lightEnv * 0.14D + layers.alienShimmer() * 0.18D)
                * entry
                * DmtDoseCurve.clamp(1.0D - postStage * 0.30D - extremeDose * 0.16D, 0.58D, 1.0D);
        double tunnelDepth = intensity * (tier1 * 0.30D + tier2 * 0.46D + tier3 * 0.40D + tier4 * 0.40D + tier5 * 0.12D)
                * (0.58D + signature.tunnelSpeed() * 0.70D + voidEnv * 0.20D + layers.veilThinness() * 0.18D + layers.breakthroughTransition() * 0.16D);
        double hyperGeometry = intensity * (tier2 * 0.34D + tier3 * 0.52D + tier4 * 0.62D + tier5 * 0.58D)
                * (0.50D + signature.symmetry() * 0.66D + memoryShelter * 0.12D + layerSplit * 0.18D + constructedRealm * 0.26D);
        double fractalOverload = intensity * (tier3 * 0.24D + tier4 * 0.52D + tier5 * 0.52D)
                * (0.44D + signature.glyphDensity() * 0.42D + peak * 0.20D + layers.symbolLanguage() * 0.08D)
                * fractalAuthority;
        double worldDissolve = intensity * (tier2 * 0.18D + tier3 * 0.38D + tier4 * 0.68D + tier5 * 0.82D)
                * (0.42D + signature.portalBias() * 0.56D + voidEnv * 0.22D + layers.realityReplacement() * 0.34D + constructedRealm * 0.14D);
        double chromaticBurst = intensity * tier1 * (0.40D + tierRamp * 0.54D) * (0.48D + signature.colorStrobe() * 0.54D + lightEnv * 0.16D + layers.edgeFlicker() * 0.16D)
                * DmtDoseCurve.clamp(1.0D - postStage * 0.18D - extremeDose * 0.10D, 0.66D, 1.0D);
        double entityPresence = intensity * (tier3 * 0.16D + tier4 * 0.38D + tier5 * 0.54D)
                * (0.32D + signature.entityHint() * 0.74D + motionEnv * 0.18D + layers.entityLayer() * 0.34D + encounter.entityFocus() * 0.28D + postStage * 0.16D);
        double egoLoss = intensity * (tier2 * 0.18D + tier3 * 0.38D + tier4 * 0.66D + tier5 * 0.78D)
                * (0.42D + signature.portalBias() * 0.34D + layers.realityReplacement() * 0.28D + postStage * 0.08D);
        double cameraPull = Math.min(0.92D, intensity * (0.030D + tierRamp * 0.040D + signature.tunnelSpeed() * 0.028D + layers.breakthroughTransition() * 0.022D + layers.realityReplacement() * 0.010D)
                * (0.38D + peak * 0.74D + transitionStage * 0.18D + returnStage * 0.06D));

        if (TrippyConfig.dmtAccessibilityMode) {
            chrysanthemum *= 0.62D;
            tunnelDepth *= 0.58D;
            hyperGeometry *= 0.54D;
            fractalOverload *= 0.48D;
            worldDissolve *= 0.52D;
            chromaticBurst *= 0.56D;
            entityPresence *= 0.45D;
            egoLoss *= 0.70D;
            cameraPull *= 0.20D;
            visual *= 0.62D;
        }

        DmtFractalScaffold fractalScaffold = resolveFractalScaffold(stage, phase, progress, intensity, visual, tier, layers);
        return new DmtEffectState(
                true,
                displayedDoseUnits,
                activeDoseUnits,
                targetDoseUnits,
                ageTicks,
                durationTicks,
                remainingTicks(),
                phase,
                stage,
                progress,
                tier,
                intensity,
                visual,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                fractalScaffold,
                encounter,
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

    public DmtPhase phase() {
        return DmtDoseCurve.phase(ageTicks, durationTicks, activeDoseUnits);
    }

    public double progress() {
        if (durationTicks <= 0) {
            return 1.0D;
        }
        return DmtDoseCurve.clamp(ageTicks / (double) durationTicks, 0.0D, 1.0D);
    }

    private DmtFractalScaffold resolveFractalScaffold(
            DmtStage stage,
            DmtPhase phase,
            double progress,
            double intensity,
            double visual,
            int tier,
            DmtLayerProfile layers
    ) {
        double safe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
        double perceptual = DmtDoseCurve.clamp((intensity * 0.54D + visual * 0.46D) / safe, 0.0D, 1.0D);
        double preDose = DmtDoseCurve.smoothstep(
                Math.max(0.12D, TrippyConfig.dmtPreBreakthroughThresholdUnits * 0.42D),
                TrippyConfig.dmtPreBreakthroughThresholdUnits,
                activeDoseUnits
        );
        double breakthroughDose = DmtDoseCurve.smoothstep(
                TrippyConfig.dmtBreakthroughThresholdUnits * 0.82D,
                Math.max(TrippyConfig.dmtBreakthroughThresholdUnits + 0.20D, TrippyConfig.dmtTier5Units),
                activeDoseUnits
        );
        double phaseEnvelope = DmtDoseCurve.phaseEnvelope(phase, progress);
        double arrivalEnvelope = DmtDoseCurve.smoothstep(0.0D, 0.11D, progress)
                * (1.0D - DmtDoseCurve.smoothstep(0.94D, 1.0D, progress));
        double tunnelEnvelope = DmtDoseCurve.smoothstep(0.045D, 0.18D, progress)
                * (1.0D - DmtDoseCurve.smoothstep(0.94D, 1.0D, progress));
        double breakthroughEnvelope = DmtDoseCurve.smoothstep(0.16D, 0.34D, progress)
                * (1.0D - DmtDoseCurve.smoothstep(0.92D, 1.0D, progress));
        double returnEnvelope = DmtDoseCurve.smoothstep(0.62D, 0.82D, progress)
                * (1.0D - DmtDoseCurve.smoothstep(0.97D, 1.0D, progress));
        double stackedEnvelope = DmtDoseCurve.clamp(
                Math.max(phaseEnvelope * 0.82D, arrivalEnvelope * (0.70D + tunnelEnvelope * 0.24D + breakthroughEnvelope * 0.34D + returnEnvelope * 0.12D)),
                0.0D,
                1.24D
        );
        double stageDepth = switch (stage) {
            case LOW_THRESHOLD -> 0.28D;
            case PRE_BREAKTHROUGH -> 0.56D;
            case BREAKTHROUGH_TRANSITION -> 0.86D;
            case POST_BREAKTHROUGH -> 1.0D;
            case RETURN -> 0.68D;
            case AFTERGLOW -> 0.24D;
            case SOBER -> 0.0D;
        };
        double phaseMotion = switch (phase) {
            case RUSH -> 0.72D;
            case TUNNEL -> 0.90D;
            case BREAKTHROUGH -> 1.05D;
            case RETURN -> 0.68D;
            case AFTERGLOW -> mix(0.68D, 0.24D, afterglowEase(progress));
            case SOBER -> 0.0D;
        };
        if (stage == DmtStage.AFTERGLOW) {
            stageDepth = mix(0.68D, stageDepth, afterglowEase(progress));
        }
        double environmentMotion = DmtDoseCurve.clamp(
                environment.moving() * 0.30D
                        + environment.entities() * 0.22D
                        + environment.light() * 0.08D
                        + signature.tunnelSpeed() * 0.18D,
                0.0D,
                1.0D
        );
        double depthCue = DmtDoseCurve.clamp(
                environment.depth() * 0.26D
                        + environment.cave() * 0.18D
                        + environment.end() * 0.22D
                        + signature.portalBias() * 0.18D,
                0.0D,
                1.0D
        );

        double fractal3dPresence = DmtDoseCurve.clamp(
                perceptual * stackedEnvelope * (0.40D + stageDepth * 0.70D)
                        * (0.30D + preDose * 0.48D + breakthroughDose * 0.62D + tunnelEnvelope * 0.10D + layers.impossibleArchitecture() * 0.10D),
                0.0D,
                1.0D
        );
        double animatedFractalPresence = DmtDoseCurve.clamp(
                perceptual * stackedEnvelope * (0.38D + phaseMotion * 0.72D)
                        * (0.26D + preDose * 0.42D + breakthroughDose * 0.68D + tunnelEnvelope * 0.10D + environmentMotion * 0.20D),
                0.0D,
                1.0D
        );
        double staticAnimatedCoupling = DmtDoseCurve.clamp(
                Math.sqrt(fractal3dPresence * animatedFractalPresence) * (0.52D + preDose * 0.22D + breakthroughDose * 0.42D + tunnelEnvelope * 0.12D),
                0.0D,
                1.0D
        );
        double alternateWorldReadiness = DmtDoseCurve.clamp(
                stackedEnvelope * (preDose * 0.18D + breakthroughDose * 0.66D + breakthroughEnvelope * 0.16D + returnEnvelope * 0.08D)
                        * (0.58D + layers.realityReplacement() * 0.20D + layers.dominance() * 0.24D),
                0.0D,
                1.0D
        );
        double dimensionalDepth = DmtDoseCurve.clamp(
                arrivalEnvelope * 0.16D
                        + tunnelEnvelope * 0.24D
                        + preDose * 0.36D
                        + breakthroughDose * 0.52D
                        + depthCue * 0.22D
                        + layers.veilThinness() * 0.08D
                        + layers.impossibleArchitecture() * 0.08D,
                0.0D,
                1.0D
        );
        double temporalFlow = DmtDoseCurve.clamp(
                0.14D
                        + signature.tunnelSpeed() * 0.34D
                        + phaseMotion * 0.38D
                        + preDose * 0.12D
                        + breakthroughDose * 0.28D
                        + returnEnvelope * 0.10D
                        + tier * 0.026D,
                0.0D,
                1.0D
        );
        double morphRate = DmtDoseCurve.clamp(0.022D + temporalFlow * 0.105D + breakthroughDose * 0.055D + staticAnimatedCoupling * 0.022D, 0.0D, 0.24D);
        double seedPhase = ((signature.seed() >>> 16) & 1023L) / 1023.0D;
        double fractalPhase = (ageTicks * morphRate + seedPhase) % 1.0D;
        long structureSeed = signature.seed() ^ 0x3D3D3D1FL ^ ((long) tier << 48);
        long animationSeed = Long.rotateLeft(signature.seed(), 23) ^ 0xA71D00DL ^ Double.doubleToLongBits(targetDoseUnits);

        return new DmtFractalScaffold(
                fractal3dPresence,
                animatedFractalPresence,
                staticAnimatedCoupling,
                alternateWorldReadiness,
                dimensionalDepth,
                temporalFlow,
                morphRate,
                fractalPhase,
                structureSeed,
                animationSeed
        );
    }

    public String debugSummary() {
        DmtEffectState state = effectState();
        return String.format(Locale.ROOT,
                "dmt dose=%.2fx tier=%d phase=%s stage=%s intensity=%.2f remaining=%ds signature=%s",
                state.displayDoseUnits(),
                state.tier(),
                state.phase().name().toLowerCase(Locale.ROOT),
                state.stage().name().toLowerCase(Locale.ROOT),
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
        tag.putDouble("memoryVoid", memoryVoid);
        tag.putDouble("memoryLight", memoryLight);
        tag.putDouble("memoryMotion", memoryMotion);
        tag.putDouble("memoryShelter", memoryShelter);
        tag.put("signature", signature.save());
        if (encounterDirector != null) {
            tag.put("encounterDirector", encounterDirector.save());
        }
        tag.put("environment", environment.save());
        return tag;
    }

    public static DmtSession load(CompoundTag tag) {
        DmtSession session = new DmtSession(
                NbtCompat.getDoubleOr(tag, "activeDoseUnits", 0.0D),
                NbtCompat.getIntOr(tag, "durationTicks", 0),
                DmtTripSignature.load(NbtCompat.getCompoundOrEmpty(tag, "signature"))
        );
        session.targetDoseUnits = NbtCompat.getDoubleOr(tag, "targetDoseUnits", session.activeDoseUnits);
        session.displayedDoseUnits = NbtCompat.getDoubleOr(tag, "displayedDoseUnits", session.activeDoseUnits);
        session.ageTicks = NbtCompat.getIntOr(tag, "ageTicks", 0);
        session.environmentTicks = NbtCompat.getIntOr(tag, "environmentTicks", 0);
        session.lastAnnouncedTier = NbtCompat.getIntOr(tag, "lastAnnouncedTier", DmtDoseCurve.tier(session.activeDoseUnits));
        session.memoryVoid = NbtCompat.getDoubleOr(tag, "memoryVoid", 0.0D);
        session.memoryLight = NbtCompat.getDoubleOr(tag, "memoryLight", 0.0D);
        session.memoryMotion = NbtCompat.getDoubleOr(tag, "memoryMotion", 0.0D);
        session.memoryShelter = NbtCompat.getDoubleOr(tag, "memoryShelter", 0.0D);
        session.environment = PsilocybinEnvironmentSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "environment"));
        if (tag.contains("encounterDirector")) {
            session.encounterDirector = DmtEncounterDirector.load(NbtCompat.getCompoundOrEmpty(tag, "encounterDirector"));
        } else {
            session.encounterDirector = DmtEncounterDirector.start(session.signature.seed(), session.activeDoseUnits, session.signature, session.environment);
        }
        return session;
    }

    private static double memory(double current, double observed) {
        return DmtDoseCurve.clamp(current + (observed - current) * 0.010D, 0.0D, 1.0D);
    }

    private static double afterglowEase(double progress) {
        double start = DmtDoseCurve.clamp(TrippyConfig.dmtAfterglowStartProgress, 0.0D, 0.98D);
        return DmtDoseCurve.smoothstep(start, Math.min(0.995D, start + 0.10D), progress);
    }

    private static double mix(double a, double b, double amount) {
        double t = DmtDoseCurve.clamp(amount, 0.0D, 1.0D);
        return a + (b - a) * t;
    }

    private void announceTierChanges(ServerPlayer player) {
        int tier = DmtDoseCurve.tier(activeDoseUnits);
        if (tier <= lastAnnouncedTier || tier <= 0 || ageTicks < 4) {
            return;
        }
        lastAnnouncedTier = tier;
        String key = switch (tier) {
            case 1 -> "message.trippycraft.dmt_tier_1";
            case 2 -> "message.trippycraft.dmt_tier_2";
            case 3 -> "message.trippycraft.dmt_tier_3";
            case 4 -> "message.trippycraft.dmt_tier_4";
            default -> "message.trippycraft.dmt_tier_5";
        };
        player.displayClientMessage(Component.translatable(key), true);
    }
}
