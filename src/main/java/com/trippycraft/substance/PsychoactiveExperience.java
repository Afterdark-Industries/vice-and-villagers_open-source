package com.trippycraft.substance;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.alcohol.AlcoholEffectProfile;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholRecoveryTracker;
import com.trippycraft.substance.alcohol.AlcoholSession;
import com.trippycraft.substance.cannabis.CannabisEffectProfile;
import com.trippycraft.substance.cannabis.CannabisEffectState;
import com.trippycraft.substance.cannabis.CannabisSession;
import com.trippycraft.substance.cocaine.CocaineEffectProfile;
import com.trippycraft.substance.cocaine.CocaineEffectState;
import com.trippycraft.substance.cocaine.CocaineRoute;
import com.trippycraft.substance.cocaine.CocaineSession;
import com.trippycraft.substance.heroin.HeroinEffectProfile;
import com.trippycraft.substance.heroin.HeroinEffectState;
import com.trippycraft.substance.heroin.HeroinRecoveryTracker;
import com.trippycraft.substance.heroin.HeroinSession;
import com.trippycraft.substance.ketamine.KetamineEffectProfile;
import com.trippycraft.substance.ketamine.KetamineMasking;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import com.trippycraft.substance.ketamine.KetamineSession;
import com.trippycraft.substance.dmt.DmtEncounterState;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.dmt.DmtEffectProfile;
import com.trippycraft.substance.dmt.DmtFractalScaffold;
import com.trippycraft.substance.dmt.DmtPhase;
import com.trippycraft.substance.dmt.DmtSession;
import com.trippycraft.substance.lsd.LsdEffectProfile;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.lsd.LsdPhase;
import com.trippycraft.substance.lsd.LsdSession;
import com.trippycraft.substance.mdma.MdmaEffectProfile;
import com.trippycraft.substance.mdma.MdmaEffectState;
import com.trippycraft.substance.mdma.MdmaPhase;
import com.trippycraft.substance.mdma.MdmaRecoveryTracker;
import com.trippycraft.substance.mdma.MdmaSession;
import com.trippycraft.substance.mdma.MdmaSocialEffects;
import com.trippycraft.substance.mescaline.MescalineDoseCurve;
import com.trippycraft.substance.allad.AlLadDoseCurve;
import com.trippycraft.substance.allad.AlLadEffectProfile;
import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.allad.AlLadSession;
import com.trippycraft.substance.twoce.TwoCeDoseCurve;
import com.trippycraft.substance.twoce.TwoCeEffectProfile;
import com.trippycraft.substance.twoce.TwoCeEffectState;
import com.trippycraft.substance.twoce.TwoCeSession;
import com.trippycraft.substance.nitrous.NitrousDoseCurve;
import com.trippycraft.substance.nitrous.NitrousEffectProfile;
import com.trippycraft.substance.nitrous.NitrousEffectState;
import com.trippycraft.substance.nitrous.NitrousSession;
import com.trippycraft.substance.homet.HoMetDoseCurve;
import com.trippycraft.substance.homet.HoMetEffectProfile;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.homet.HoMetSession;
import com.trippycraft.substance.mescaline.MescalineEffectProfile;
import com.trippycraft.substance.mescaline.MescalineEffectState;
import com.trippycraft.substance.mescaline.MescalineSession;
import com.trippycraft.substance.psilocybin.PsilocybinEffectProfile;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinPhase;
import com.trippycraft.substance.psilocybin.PsilocybinSession;
import com.trippycraft.substance.salvia.SalviaEffectProfile;
import com.trippycraft.substance.salvia.SalviaEffectState;
import com.trippycraft.substance.salvia.SalviaSession;
import com.trippycraft.substance.twocb.TwoCbEffectProfile;
import com.trippycraft.substance.twocb.TwoCbEffectState;
import com.trippycraft.substance.twocb.TwoCbSession;
import com.trippycraft.world.TavernAmbienceService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PsychoactiveExperience {
    private static final int MIN_DMT_RESISTANCE_TICKS = 60 * 20;
    private static final int MAX_DMT_RESISTANCE_TICKS = 10 * 60 * 20;
    private static final double CLIENT_VISUAL_THRESHOLD = 0.003D;

    private final List<SubstanceExposure> exposures = new ArrayList<>();
    private final EnumMap<ExperienceChannel, Double> channels = new EnumMap<>(ExperienceChannel.class);
    private final EnumMap<SubstanceFamily, Double> families = new EnumMap<>(SubstanceFamily.class);
    private final EnumMap<SubstanceFamily, Double> tolerance = new EnumMap<>(SubstanceFamily.class);
    private final EnumMap<SubstanceFamily, Double> afterglow = new EnumMap<>(SubstanceFamily.class);
    private final EnumMap<SubstanceFamily, Double> comedown = new EnumMap<>(SubstanceFamily.class);
    private final EnumMap<SubstanceFamily, Double> craving = new EnumMap<>(SubstanceFamily.class);
    private final EnumMap<SubstanceFamily, Double> withdrawal = new EnumMap<>(SubstanceFamily.class);
    private PsilocybinSession psilocybinSession;
    private LsdSession lsdSession;
    private DmtSession dmtSession;
    private CannabisSession cannabisSession;
    private MdmaSession mdmaSession;
    private TwoCbSession twoCbSession;
    private MescalineSession mescalineSession;
    private HoMetSession hoMetSession;
    private AlLadSession alLadSession;
    private TwoCeSession twoCeSession;
    private NitrousSession nitrousSession;
    private KetamineSession ketamineSession;
    private CocaineSession cocaineSession;
    private HeroinSession heroinSession;
    private SalviaSession salviaSession;
    private AlcoholSession alcoholSession;
    private AlcoholRecoveryTracker alcoholRecovery = new AlcoholRecoveryTracker();
    private HeroinRecoveryTracker heroinRecovery = new HeroinRecoveryTracker();
    private MdmaRecoveryTracker mdmaRecovery = new MdmaRecoveryTracker();
    private int ticksActive;
    private double mixedRisk;
    private double strain;
    private int psychedelicLuckTicks;
    private double psychedelicLuckStrength;
    private int dmtResistanceTicks;
    private double dmtResistanceStrength;
    private boolean psilocybinAfterglowRewardApplied;
    private boolean lsdAfterglowRewardApplied;
    private boolean dmtAfterglowRewardApplied;
    private int psilocybinAlcoholResetStage;
    private int lsdAlcoholResetStage;
    private int dmtAlcoholResetStage;
    private double pendingPsilocybinAlcoholResetDoseGrams;
    private double pendingLsdAlcoholResetDoseMicrograms;
    private double pendingDmtAlcoholResetDoseUnits;
    private double vanillaNauseaVisualDrift;
    private double vanillaNauseaGeometryMelt;

    public PsychoactiveExperience() {
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            channels.put(channel, 0.0D);
        }
        for (SubstanceFamily family : SubstanceFamily.values()) {
            families.put(family, 0.0D);
            tolerance.put(family, 0.0D);
            afterglow.put(family, 0.0D);
            comedown.put(family, 0.0D);
            craving.put(family, 0.0D);
            withdrawal.put(family, 0.0D);
        }
    }

    public void addExposure(SubstanceProfile profile, double abstractUnits, SetSettingSnapshot setting) {
        if (profile == SubstanceProfiles.HO_MET || "four_ho_met".equals(profile.id())) {
            addHoMetDose(null, abstractUnits * 20.0D);
            return;
        }
        if (profile == SubstanceProfiles.AL_LAD || "al_lad".equals(profile.id())) {
            addAlLadDose(null, abstractUnits * 150.0D);
            return;
        }
        if (profile == SubstanceProfiles.TWO_CE || "two_ce".equals(profile.id())) {
            addTwoCeDose(null, abstractUnits * 40.0D);
            return;
        }
        if (profile == SubstanceProfiles.NITROUS || "nitrous".equals(profile.id())) {
            addNitrousDose(null, Math.max(1.0D, abstractUnits));
            return;
        }
        if (isCocaineProfile(profile)) {
            addCocaineDose(resolveCocaineRoute(profile), abstractUnits);
            return;
        }
        if (isKetamineProfile(profile)) {
            addKetamineDose(abstractUnits * 50.0D);
            return;
        }
        if (isHeroinProfile(profile)) {
            addHeroinDose(null, abstractUnits * 100.0D);
            return;
        }
        if (isMdmaProfile(profile)) {
            addMdmaDose(abstractUnits * 100.0D);
            return;
        }
        if (isTwoCbProfile(profile)) {
            addTwoCbDose(abstractUnits * 100.0D);
            return;
        }
        if (!profile.hasGameplayEffects()) {
            return;
        }
        exposures.add(new SubstanceExposure(profile, abstractUnits, setting));
        if (profile.riskPressure() > 0.25D) {
            SubstanceFamily family = profile.family();
            craving.put(family, Math.max(0.0D, craving.getOrDefault(family, 0.0D) - 0.08D));
        }
    }

    public void addPsilocybinDose(ServerPlayer player, double doseGrams) {
        if (!TrippyConfig.enablePsilocybinEffects) {
            addExposure(SubstanceProfiles.PSILOCYBIN, doseGrams, SetSettingSnapshot.capture(player));
            return;
        }
        if (psilocybinSession == null || !psilocybinSession.isAlive()) {
            psilocybinSession = PsilocybinSession.start(player, doseGrams);
            psilocybinAfterglowRewardApplied = false;
            psilocybinAlcoholResetStage = 0;
        } else {
            psilocybinSession.stackDose(doseGrams);
        }
        queuePsilocybinAlcoholReset(psilocybinSession.targetDoseGrams());
    }

    public void addLsdDose(ServerPlayer player, double doseMicrograms) {
        if (!TrippyConfig.enableLsdEffects) {
            addExposure(SubstanceProfiles.LSD, doseMicrograms / 100.0D, SetSettingSnapshot.capture(player));
            return;
        }
        if (lsdSession == null || !lsdSession.isAlive()) {
            lsdSession = LsdSession.start(player, doseMicrograms);
            lsdAfterglowRewardApplied = false;
            lsdAlcoholResetStage = 0;
        } else {
            lsdSession.stackDose(doseMicrograms);
        }
        queueLsdAlcoholReset(lsdSession.targetDoseMicrograms());
    }

    public void addDmtDose(ServerPlayer player, double doseUnits) {
        if (!TrippyConfig.enableDmtEffects) {
            return;
        }
        if (dmtSession == null || !dmtSession.isAlive()) {
            dmtSession = DmtSession.start(player, doseUnits);
            dmtAfterglowRewardApplied = false;
            dmtAlcoholResetStage = 0;
        } else {
            dmtSession.stackDose(doseUnits);
        }
        queueDmtAlcoholReset(dmtSession.targetDoseUnits());
    }

    public void addCannabisDose(ServerPlayer player, double thcMilligrams) {
        double added = Math.max(0.0D, thcMilligrams);
        if (cannabisSession == null || !cannabisSession.isAlive()) {
            cannabisSession = CannabisSession.start(player, added);
        } else {
            cannabisSession.stackDose(added);
        }
    }

    public void addKetamineDose(double doseMilligrams) {
        double added = Math.max(0.0D, doseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        if (ketamineSession == null || !ketamineSession.isAlive()) {
            ketamineSession = KetamineSession.start(added);
        } else {
            ketamineSession.stackDose(added);
        }
    }

    public void addMdmaDose(double doseMilligrams) {
        addMdmaDose(doseMilligrams, Long.MIN_VALUE);
    }

    public void addMdmaDose(double doseMilligrams, long gameTime) {
        double added = Math.max(0.0D, doseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        if (mdmaSession == null || !mdmaSession.isAlive()) {
            mdmaSession = MdmaSession.start(added);
        } else {
            mdmaSession.stackDose(added);
        }
        mdmaRecovery.onDoseAdded(added, gameTime);
    }

    public void addTwoCbDose(double doseMilligrams) {
        if (!TrippyConfig.enableTwoCbEffects) {
            return;
        }
        double added = Math.max(0.0D, doseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        if (twoCbSession == null || !twoCbSession.isAlive()) {
            twoCbSession = TwoCbSession.start(added);
        } else {
            twoCbSession.stackDose(added);
        }
    }

    public void addHoMetDose(ServerPlayer player, double doseMilligrams) {
        addHoMetDose(player, doseMilligrams, HoMetSession.contentMilligramsForEffect(doseMilligrams));
    }

    public void addHoMetDose(ServerPlayer player, double doseMilligrams, double contentMilligrams) {
        double added = HoMetDoseCurve.sanitizeDose(doseMilligrams);
        if (added <= 0.0D) return;
        if (hoMetSession == null || !hoMetSession.isAlive()) hoMetSession = HoMetSession.start(player, added, contentMilligrams);
        else hoMetSession.stackDose(added, contentMilligrams);
    }

    public void addNitrousDose(ServerPlayer player, double hits) {
        double added = NitrousDoseCurve.sanitizeHits(hits);
        if (added <= 0.0D) {
            return;
        }
        if (nitrousSession == null || !nitrousSession.isAlive()) {
            nitrousSession = NitrousSession.start(player, added);
        } else {
            nitrousSession.stackDose(added);
        }
    }

    public void addTwoCeDose(ServerPlayer player, double doseMilligrams) {
        addTwoCeDose(player, doseMilligrams, TwoCeSession.contentMilligramsForEffect(doseMilligrams));
    }

    public void addTwoCeDose(ServerPlayer player, double doseMilligrams, double contentMilligrams) {
        double added = TwoCeDoseCurve.sanitizeDose(doseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        if (twoCeSession == null || !twoCeSession.isAlive()) {
            twoCeSession = TwoCeSession.start(player, added, contentMilligrams);
        } else {
            twoCeSession.stackDose(added, contentMilligrams);
        }
    }

    public void addAlLadDose(ServerPlayer player, double doseMicrograms) {
        addAlLadDose(player, doseMicrograms, AlLadSession.contentMilligramsForEffect(doseMicrograms));
    }

    public void addAlLadDose(ServerPlayer player, double doseMicrograms, double contentMilligrams) {
        double added = AlLadDoseCurve.sanitizeDose(doseMicrograms);
        if (added <= 0.0D) {
            return;
        }
        if (alLadSession == null || !alLadSession.isAlive()) {
            alLadSession = AlLadSession.start(player, added, contentMilligrams);
        } else {
            alLadSession.stackDose(added, contentMilligrams);
        }
    }

    public void addMescalineDose(ServerPlayer player, double doseMilligrams) {
        double added = MescalineDoseCurve.sanitizeDose(doseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        if (mescalineSession == null || !mescalineSession.isAlive()) {
            mescalineSession = MescalineSession.start(player, added);
        } else {
            mescalineSession.stackDose(added);
        }
    }

    public void addCocaineDose(CocaineRoute route, double doseGrams) {
        double added = Math.max(0.0D, doseGrams);
        if (added <= 0.0D) {
            return;
        }
        if (cocaineSession == null || !cocaineSession.isAlive()) {
            cocaineSession = CocaineSession.start(added, route);
        } else if (!cocaineSession.hasActiveEffects()) {
            cocaineSession.restart(added, route);
        } else {
            cocaineSession.stackDose(added, route);
        }
    }

    public void addHeroinDose(ServerPlayer player, double doseMilligrams) {
        double added = Math.max(0.0D, doseMilligrams);
        if (added <= 0.0D) {
            return;
        }
        heroinRecovery.onDoseAdded(added);
        double tolerance = heroinRecovery.durationToleranceProgress();
        double dependence = heroinRecovery.dependenceProgress();
        if (heroinSession == null || !heroinSession.isAlive()) {
            heroinSession = HeroinSession.start(added, tolerance, dependence);
        } else {
            heroinSession.stackDose(added, tolerance, dependence);
        }
    }

    public void addSalviaDose(ServerPlayer player, double doseUnits) {
        if (salviaSession == null || !salviaSession.isAlive()) {
            salviaSession = SalviaSession.start(player, doseUnits);
        } else {
            salviaSession.stackDose(doseUnits);
        }
    }

    public void addAlcoholDose(double standardDrinks) {
        if (!TrippyConfig.enableAlcoholEffects) {
            return;
        }
        alcoholRecovery.onDoseAdded(standardDrinks);
        if (alcoholSession == null || !alcoholSession.isAlive()) {
            alcoholSession = AlcoholSession.start(standardDrinks);
        } else {
            alcoholSession.stackDose(standardDrinks);
        }
    }

    public void tick(ServerPlayer player, SetSettingSnapshot setting) {
        ticksActive++;
        tickRewardTimers();
        EnumMap<ExperienceChannel, Double> target = zeroChannels();
        EnumMap<ExperienceChannel, Double> excludedVisuals = zeroChannels();
        EnumMap<SubstanceFamily, Double> familyTarget = zeroFamilies();
        EnumMap<SubstanceFamily, Double> rawFamilyTarget = zeroFamilies();
        CannabisEffectState cannabisModerator = cannabisState();
        KetamineEffectState ketamineModerator = rawKetamineState();

        Iterator<SubstanceExposure> iterator = exposures.iterator();
        while (iterator.hasNext()) {
            SubstanceExposure exposure = iterator.next();
            double potentiation = substanceMixMultiplier(exposure.profile().family(), cannabisModerator, ketamineModerator);
            double raw = exposure.rawContribution(setting) * potentiation;
            rawFamilyTarget.merge(exposure.profile().family(), raw, Double::sum);
            double beforeDrift = target.getOrDefault(ExperienceChannel.VISUAL_DRIFT, 0.0D);
            double beforeMelt = target.getOrDefault(ExperienceChannel.GEOMETRY_MELT, 0.0D);
            exposure.contribute(setting, target, familyTarget, responseMultiplier(exposure.profile().family()) * potentiation);
            if ("psilocybin".equals(exposure.profile().id()) || "lsd".equals(exposure.profile().id())) {
                excludedVisuals.merge(ExperienceChannel.VISUAL_DRIFT,
                        target.getOrDefault(ExperienceChannel.VISUAL_DRIFT, 0.0D) - beforeDrift, Double::sum);
                excludedVisuals.merge(ExperienceChannel.GEOMETRY_MELT,
                        target.getOrDefault(ExperienceChannel.GEOMETRY_MELT, 0.0D) - beforeMelt, Double::sum);
            }
            exposure.tick();
            if (exposure.isExpired()) {
                iterator.remove();
            }
        }

        tickSpecializedPsychedelics(player, target, familyTarget, rawFamilyTarget, cannabisModerator, ketamineModerator, excludedVisuals);
        tickMescalineSession(player, target, familyTarget, rawFamilyTarget, cannabisModerator, ketamineModerator);
        tickCannabisSession(player, target, familyTarget, rawFamilyTarget, ketamineModerator);
        tickKetamineSession(player, target, familyTarget, rawFamilyTarget, cannabisModerator);
        tickNitrousSession(player, target, familyTarget, rawFamilyTarget, cannabisModerator);
        tickCocaineSession(player, target, familyTarget, rawFamilyTarget, cannabisModerator, ketamineModerator);
        tickHeroinSession(player, target, familyTarget, rawFamilyTarget, cannabisModerator, ketamineModerator);
        tickSalviaSession(player, target, familyTarget, rawFamilyTarget, cannabisModerator, ketamineModerator);
        tickAlcoholSession(player, target, familyTarget, rawFamilyTarget, cannabisModerator, ketamineModerator, cocaineState());
        tickMdmaSession(player, target, familyTarget, rawFamilyTarget);
        tickTwoCbSession(player, target, familyTarget, rawFamilyTarget);
        applyDeferredAlcoholTherapy();

        updateDepthState(setting, rawFamilyTarget, familyTarget);
        applyDepthChannels(target);
        smoothVanillaNauseaVisuals(target, excludedVisuals);
        applySynergy(target, familyTarget);
        smoothChannels(target);
        smoothFamilies(familyTarget);
    }

    private void tickSpecializedPsychedelics(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget,
            CannabisEffectState cannabisModerator,
            KetamineEffectState ketamineModerator,
            EnumMap<ExperienceChannel, Double> excludedVisuals
    ) {
        PsilocybinEffectState psilocybin = tickPsilocybinSession(player);
        LsdEffectState lsd = tickLsdSession(player);
        DmtEffectState dmt = tickDmtSession(player);
        HoMetEffectState hoMet = tickHoMetSession(player);
        AlLadEffectState alLad = tickAlLadSession(player);
        TwoCeEffectState twoCe = tickTwoCeSession(player);
        if (psilocybin == null && lsd == null && dmt == null && hoMet == null && alLad == null && twoCe == null) {
            return;
        }

        EnumMap<ExperienceChannel, Double> psilocybinChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> psilocybinFamilies = zeroFamilies();
        EnumMap<ExperienceChannel, Double> lsdChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> lsdFamilies = zeroFamilies();
        EnumMap<ExperienceChannel, Double> hoMetChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> hoMetFamilies = zeroFamilies();
        EnumMap<ExperienceChannel, Double> alLadChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> alLadFamilies = zeroFamilies();
        EnumMap<ExperienceChannel, Double> twoCeChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> twoCeFamilies = zeroFamilies();
        EnumMap<ExperienceChannel, Double> dmtChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> dmtFamilies = zeroFamilies();
        double potentiation = substanceMixMultiplier(SubstanceFamily.PSYCHEDELIC, cannabisModerator, ketamineModerator);
        double toleranceResponse = responseMultiplier(SubstanceFamily.PSYCHEDELIC);

        if (psilocybin != null) {
            PsilocybinEffectProfile.contribute(psilocybin, psilocybinChannels, psilocybinFamilies);
            rawFamilyTarget.merge(SubstanceFamily.PSYCHEDELIC, psilocybin.intensity() * potentiation, Double::sum);
        }
        if (lsd != null) {
            LsdEffectProfile.contribute(lsd, lsdChannels, lsdFamilies);
            rawFamilyTarget.merge(SubstanceFamily.PSYCHEDELIC, lsd.intensity() * potentiation, Double::sum);
        }
        if (dmt != null) {
            DmtEffectProfile.contribute(dmt, dmtChannels, dmtFamilies);
            rawFamilyTarget.merge(SubstanceFamily.PSYCHEDELIC, dmt.intensity() * potentiation, Double::sum);
        }
        if (hoMet != null) {
            HoMetEffectProfile.contribute(hoMet, hoMetChannels, hoMetFamilies);
            rawFamilyTarget.merge(SubstanceFamily.PSYCHEDELIC, HoMetEffectProfile.rawFamilyContribution(hoMet) * potentiation, Double::sum);
        }
        if (alLad != null) {
            AlLadEffectProfile.contribute(alLad, alLadChannels, alLadFamilies);
            rawFamilyTarget.merge(SubstanceFamily.PSYCHEDELIC, AlLadEffectProfile.rawFamilyContribution(alLad) * potentiation, Double::sum);
        }
        if (twoCe != null) {
            TwoCeEffectProfile.contribute(twoCe, twoCeChannels, twoCeFamilies);
            rawFamilyTarget.merge(SubstanceFamily.PSYCHEDELIC, TwoCeEffectProfile.rawFamilyContribution(twoCe) * potentiation, Double::sum);
        }
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(
                psilocybin == null ? PsilocybinEffectState.inactive() : psilocybin,
                lsd == null ? LsdEffectState.inactive() : lsd,
                dmt == null ? DmtEffectState.inactive() : dmt,
                hoMet == null ? HoMetEffectState.inactive() : hoMet,
                alLad == null ? AlLadEffectState.inactive() : alLad,
                twoCe == null ? TwoCeEffectState.inactive() : twoCe
        );
        mergeScaled(target, psilocybinChannels, blend.psilocybinContributionGain() * potentiation * toleranceResponse);
        mergeScaled(excludedVisuals, psilocybinChannels, blend.psilocybinContributionGain() * potentiation * toleranceResponse);
        mergeScaled(target, lsdChannels, blend.lsdContributionGain() * potentiation * toleranceResponse);
        mergeScaled(excludedVisuals, lsdChannels, blend.lsdContributionGain() * potentiation * toleranceResponse);
        mergeScaled(target, dmtChannels, blend.dmtContributionGain() * potentiation * (0.65D + toleranceResponse * 0.35D));
        mergeScaled(familyTarget, psilocybinFamilies, blend.psilocybinContributionGain() * potentiation * toleranceResponse);
        mergeScaled(familyTarget, lsdFamilies, blend.lsdContributionGain() * potentiation * toleranceResponse);
        mergeScaled(familyTarget, dmtFamilies, blend.dmtContributionGain() * potentiation * (0.65D + toleranceResponse * 0.35D));
        mergeScaled(target, hoMetChannels, blend.hoMetContributionGain() * potentiation * toleranceResponse);
        mergeScaled(familyTarget, hoMetFamilies, blend.hoMetContributionGain() * potentiation * toleranceResponse);
        mergeScaled(target, alLadChannels, blend.alLadContributionGain() * potentiation * toleranceResponse);
        mergeScaled(familyTarget, alLadFamilies, blend.alLadContributionGain() * potentiation * toleranceResponse);
        mergeScaled(target, twoCeChannels, blend.twoCeContributionGain() * potentiation * toleranceResponse);
        mergeScaled(familyTarget, twoCeFamilies, blend.twoCeContributionGain() * potentiation * toleranceResponse);
        applyPsychedelicBridge(target, familyTarget, psilocybin, lsd, dmt, blend, potentiation, excludedVisuals);
    }

    private HoMetEffectState tickHoMetSession(ServerPlayer player) {
        if (hoMetSession == null) return null;
        hoMetSession.tick(player);
        if (!hoMetSession.isAlive()) {
            hoMetSession = null;
            return null;
        }
        return hoMetSession.effectState();
    }

    private AlLadEffectState tickAlLadSession(ServerPlayer player) {
        if (alLadSession == null) {
            return null;
        }
        alLadSession.tick(player);
        if (!alLadSession.isAlive()) {
            alLadSession = null;
            return null;
        }
        return alLadSession.effectState();
    }

    private TwoCeEffectState tickTwoCeSession(ServerPlayer player) {
        if (twoCeSession == null) {
            return null;
        }
        twoCeSession.tick(player);
        if (!twoCeSession.isAlive()) {
            twoCeSession = null;
            return null;
        }
        return twoCeSession.effectState();
    }

    private void tickMescalineSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget,
            CannabisEffectState cannabisModerator,
            KetamineEffectState ketamineModerator
    ) {
        if (mescalineSession == null) {
            return;
        }
        mescalineSession.tick(player);
        if (!mescalineSession.isAlive()) {
            mescalineSession = null;
            return;
        }
        MescalineEffectState raw = mescalineSession.effectState();
        double potentiation = substanceMixMultiplier(SubstanceFamily.PSYCHEDELIC, cannabisModerator, ketamineModerator);
        MescalineEffectState perceived = raw.scaled(responseMultiplier(SubstanceFamily.PSYCHEDELIC));
        EnumMap<ExperienceChannel, Double> mescalineChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> mescalineFamilies = zeroFamilies();
        MescalineEffectProfile.contribute(perceived, mescalineChannels, mescalineFamilies);
        mergeScaled(target, mescalineChannels, potentiation);
        mergeScaled(familyTarget, mescalineFamilies, potentiation);
        rawFamilyTarget.merge(SubstanceFamily.PSYCHEDELIC, MescalineEffectProfile.rawFamilyContribution(raw), Double::sum);
    }

    private void tickSalviaSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget,
            CannabisEffectState cannabisModerator,
            KetamineEffectState ketamineModerator
    ) {
        if (salviaSession == null) {
            return;
        }
        salviaSession.tick(player);
        if (!salviaSession.isAlive()) {
            applySalviaWakeTeleport(player, salviaSession);
            salviaSession = null;
            return;
        }
        SalviaEffectState state = salviaSession.effectState();
        EnumMap<ExperienceChannel, Double> salviaChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> salviaFamilies = zeroFamilies();
        double potentiation = substanceMixMultiplier(SubstanceFamily.DELIRIANT, cannabisModerator, ketamineModerator);
        SalviaEffectProfile.contribute(state, salviaChannels, salviaFamilies);
        mergeScaled(target, salviaChannels, potentiation);
        mergeScaled(familyTarget, salviaFamilies, potentiation);
        rawFamilyTarget.merge(SubstanceFamily.DELIRIANT, state.intensity() * potentiation, Double::sum);
    }

    private void tickCannabisSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget,
            KetamineEffectState ketamineModerator
    ) {
        if (cannabisSession == null) {
            return;
        }
        cannabisSession.tick();
        if (!cannabisSession.isAlive()) {
            cannabisSession = null;
            return;
        }

        CannabisEffectState state = cannabisSession.effectState();
        EnumMap<ExperienceChannel, Double> cannabisChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> cannabisFamilies = zeroFamilies();
        double modulation = ketamineAttenuationMultiplier(SubstanceFamily.CANNABINOID, ketamineModerator);
        CannabisEffectProfile.contribute(state, cannabisChannels, cannabisFamilies);
        mergeScaled(target, cannabisChannels, modulation);
        mergeScaled(familyTarget, cannabisFamilies, modulation);
        rawFamilyTarget.merge(SubstanceFamily.CANNABINOID, CannabisEffectProfile.rawFamilyContribution(state) * modulation, Double::sum);
    }

    private void tickCocaineSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget,
            CannabisEffectState cannabisModerator,
            KetamineEffectState ketamineModerator
    ) {
        if (cocaineSession == null) {
            return;
        }
        cocaineSession.tick();
        if (!cocaineSession.isAlive()) {
            cocaineSession = null;
            return;
        }

        CocaineEffectState state = cocaineSession.effectState();
        if (!state.active()) {
            return;
        }
        EnumMap<ExperienceChannel, Double> cocaineChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> cocaineFamilies = zeroFamilies();
        double potentiation = cannabisCocaineMultiplier(cannabisModerator) * ketamineAttenuationMultiplier(SubstanceFamily.STIMULANT, ketamineModerator);
        CocaineEffectProfile.contribute(state, cocaineChannels, cocaineFamilies);
        mergeScaled(target, cocaineChannels, potentiation);
        mergeScaled(familyTarget, cocaineFamilies, potentiation);
        rawFamilyTarget.merge(SubstanceFamily.STIMULANT, state.intensity() * potentiation, Double::sum);
    }

    private void tickHeroinSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget,
            CannabisEffectState cannabisModerator,
            KetamineEffectState ketamineModerator
    ) {
        if (heroinSession == null) {
            heroinRecovery.tick(player, HeroinEffectState.inactive(), cannabisModerator.active());
            return;
        }
        heroinSession.tick();
        if (!heroinSession.isAlive()) {
            heroinSession = null;
            heroinRecovery.tick(player, HeroinEffectState.inactive(), cannabisModerator.active());
            return;
        }

        HeroinEffectState state = heroinSession.effectState();
        heroinRecovery.tick(player, state, cannabisModerator.active());
        if (!state.active()) {
            return;
        }
        EnumMap<ExperienceChannel, Double> heroinChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> heroinFamilies = zeroFamilies();
        double potentiation = substanceMixMultiplier(SubstanceFamily.OPIOID, cannabisModerator, ketamineModerator);
        HeroinEffectProfile.contribute(state, heroinChannels, heroinFamilies);
        mergeScaled(target, heroinChannels, potentiation);
        mergeScaled(familyTarget, heroinFamilies, potentiation);
        rawFamilyTarget.merge(SubstanceFamily.OPIOID, HeroinEffectProfile.rawFamilyContribution(state) * potentiation, Double::sum);
    }

    private void tickKetamineSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget,
            CannabisEffectState cannabisModerator
    ) {
        if (ketamineSession == null) {
            return;
        }
        ketamineSession.tick();
        if (!ketamineSession.isAlive()) {
            ketamineSession = null;
            return;
        }

        KetamineEffectState state = ketamineSession.effectState();
        if (!state.active()) {
            return;
        }
        EnumMap<ExperienceChannel, Double> ketamineChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> ketamineFamilies = zeroFamilies();
        double potentiation = cannabisPotentiationMultiplier(SubstanceFamily.DISSOCIATIVE, cannabisModerator);
        KetamineEffectProfile.contribute(state, ketamineChannels, ketamineFamilies);
        mergeScaled(target, ketamineChannels, potentiation);
        mergeScaled(familyTarget, ketamineFamilies, potentiation);
        rawFamilyTarget.merge(SubstanceFamily.DISSOCIATIVE, state.intensity() * potentiation, Double::sum);
    }

    private void tickNitrousSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget,
            CannabisEffectState cannabisModerator
    ) {
        if (nitrousSession == null) {
            return;
        }
        nitrousSession.tick(player);
        if (!nitrousSession.isAlive()) {
            nitrousSession = null;
            return;
        }
        NitrousEffectState state = nitrousSession.effectState();
        if (!state.active()) {
            return;
        }
        EnumMap<ExperienceChannel, Double> nitrousChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> nitrousFamilies = zeroFamilies();
        double potentiation = cannabisPotentiationMultiplier(SubstanceFamily.DISSOCIATIVE, cannabisModerator);
        NitrousEffectProfile.contribute(state, nitrousChannels, nitrousFamilies);
        mergeScaled(target, nitrousChannels, potentiation);
        mergeScaled(familyTarget, nitrousFamilies, potentiation);
        rawFamilyTarget.merge(SubstanceFamily.DISSOCIATIVE, NitrousEffectProfile.rawFamilyContribution(state) * potentiation, Double::sum);
    }

    private void applySalviaWakeTeleport(ServerPlayer player, SalviaSession session) {
        BedAnchor anchor = salviaBedAnchor(player, session);
        BlockPos target = findSalviaWakeTarget(anchor);
        if (target == null) {
            return;
        }

        float yaw = player.getYRot() + (player.getRandom().nextFloat() - 0.5F) * 65.0F;
        if (anchor.level() == player.level()) {
            player.teleportTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D);
        } else {
            player.teleportTo(anchor.level(), target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, Set.<RelativeMovement>of(), yaw, player.getXRot());
        }
        player.setYRot(yaw);
        player.setXRot(player.getXRot());
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.resetFallDistance();
        player.displayClientMessage(Component.translatable("message.trippycraft.salvia_wake_shift"), true);
    }

    private BedAnchor salviaBedAnchor(ServerPlayer player, SalviaSession session) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos fallback = new BlockPos(
                (int) Math.floor(session.homeX()),
                (int) Math.floor(session.homeY()),
                (int) Math.floor(session.homeZ())
        );
        BlockPos respawnPosition = player.getRespawnPosition();
        if (respawnPosition != null && player.getServer() != null) {
            ServerLevel respawnLevel = player.getServer().getLevel(player.getRespawnDimension());
            if (respawnLevel != null) {
                return new BedAnchor(respawnLevel, respawnPosition);
            }
        }

        return new BedAnchor(level, fallback);
    }

    private BlockPos findSalviaWakeTarget(BedAnchor anchor) {
        ServerLevel level = anchor.level();
        RandomSource random = level.random;
        int centerX = anchor.origin().getX();
        int centerZ = anchor.origin().getZ();
        BlockPos fallback = safeSalviaSurface(level, centerX, centerZ);
        for (int attempt = 0; attempt < 36; attempt++) {
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            double radius = 10.0D + Math.sqrt(random.nextDouble()) * 90.0D;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int z = centerZ + (int) Math.round(Math.sin(angle) * radius);
            BlockPos candidate = safeSalviaSurface(level, x, z);
            if (candidate != null) {
                return candidate;
            }
        }
        return fallback;
    }

    private BlockPos safeSalviaSurface(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= 1) {
            return null;
        }

        BlockPos feet = new BlockPos(x, y, z);
        BlockPos ground = feet.below();
        if (!level.getFluidState(ground).isEmpty()) {
            return null;
        }
        if (!level.getBlockState(ground).blocksMotion()) {
            return null;
        }
        if (level.getBlockState(feet).blocksMotion() || level.getBlockState(feet.above()).blocksMotion()) {
            return null;
        }
        return feet;
    }

    private record BedAnchor(ServerLevel level, BlockPos origin) {
    }

    private PsilocybinEffectState tickPsilocybinSession(ServerPlayer player) {
        if (psilocybinSession == null) {
            return null;
        }
        psilocybinSession.tick(player);
        if (!psilocybinSession.isAlive()) {
            psilocybinSession = null;
            psilocybinAfterglowRewardApplied = false;
            return null;
        }
        PsilocybinEffectState state = psilocybinSession.effectState();
        if (state.phase() == PsilocybinPhase.AFTERGLOW) {
            if (!psilocybinAfterglowRewardApplied) {
                queuePsychedelicLuck(psilocybinLuckDurationTicks(state), psilocybinPostTripStrength(state));
                psilocybinAfterglowRewardApplied = true;
            }
        } else {
            psilocybinAfterglowRewardApplied = false;
        }
        return state;
    }

    private LsdEffectState tickLsdSession(ServerPlayer player) {
        if (lsdSession == null) {
            return null;
        }
        lsdSession.tick(player);
        if (!lsdSession.isAlive()) {
            lsdSession = null;
            lsdAfterglowRewardApplied = false;
            return null;
        }
        LsdEffectState state = lsdSession.effectState();
        if (state.phase() == LsdPhase.AFTERGLOW) {
            if (!lsdAfterglowRewardApplied) {
                queuePsychedelicLuck(lsdLuckDurationTicks(state), lsdPostTripStrength(state));
                lsdAfterglowRewardApplied = true;
            }
        } else {
            lsdAfterglowRewardApplied = false;
        }
        return state;
    }

    private DmtEffectState tickDmtSession(ServerPlayer player) {
        if (dmtSession == null) {
            return null;
        }
        dmtSession.tick(player);
        if (!dmtSession.isAlive()) {
            dmtSession = null;
            dmtAfterglowRewardApplied = false;
            return null;
        }
        DmtEffectState state = dmtSession.effectState();
        if (state.phase() == DmtPhase.AFTERGLOW) {
            if (!dmtAfterglowRewardApplied) {
                queuePsychedelicLuck(dmtLuckDurationTicks(state), dmtLuckPostTripStrength(state));
                queueDmtResistance(dmtResistancePostTripStrength(state));
                dmtAfterglowRewardApplied = true;
            }
        } else {
            dmtAfterglowRewardApplied = false;
        }
        return state;
    }

    private void tickAlcoholSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget,
            CannabisEffectState cannabisModerator,
            KetamineEffectState ketamineModerator,
            CocaineEffectState cocaineModerator
    ) {
        if (alcoholSession == null) {
            alcoholRecovery.tick(player, AlcoholEffectState.inactive());
            return;
        }
        alcoholSession.tick();
        if (!alcoholSession.isAlive()) {
            alcoholSession = null;
            alcoholRecovery.tick(player, AlcoholEffectState.inactive());
            return;
        }
        AlcoholEffectState alcohol = alcoholSession.effectState();
        AlcoholEffectState experiencedAlcohol = maskAlcoholState(alcohol, cocaineModerator);
        EnumMap<ExperienceChannel, Double> alcoholChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> alcoholFamilies = zeroFamilies();
        double potentiation = substanceMixMultiplier(SubstanceFamily.DEPRESSANT, cannabisModerator, ketamineModerator);
        AlcoholEffectProfile.contribute(experiencedAlcohol, alcoholChannels, alcoholFamilies);
        mergeScaled(target, alcoholChannels, potentiation);
        mergeScaled(familyTarget, alcoholFamilies, potentiation);
        rawFamilyTarget.merge(SubstanceFamily.DEPRESSANT, experiencedAlcohol.intensity() * potentiation, Double::sum);
        alcoholRecovery.tick(player, alcohol);
    }

    private void tickMdmaSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget
    ) {
        if (mdmaSession == null) {
            mdmaRecovery.tick(player, MdmaEffectState.inactive());
            return;
        }
        mdmaSession.setSettingMultiplier(TavernAmbienceService.mdmaIntensityMultiplier(player));
        mdmaSession.tick();
        if (!mdmaSession.isAlive()) {
            mdmaSession = null;
            mdmaRecovery.tick(player, MdmaEffectState.inactive());
            return;
        }
        mdmaRecovery.tick(player, mdmaSession.effectState());
        MdmaEffectState state = mdmaState();
        EnumMap<ExperienceChannel, Double> mdmaChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> mdmaFamilies = zeroFamilies();
        MdmaEffectProfile.contribute(state, mdmaChannels, mdmaFamilies);
        mergeScaled(target, mdmaChannels, 1.0D);
        mergeScaled(familyTarget, mdmaFamilies, 1.0D);
        rawFamilyTarget.merge(SubstanceFamily.EMPATHOGEN, MdmaEffectProfile.rawFamilyContribution(state), Double::sum);
        MdmaSocialEffects.tick(player, state);
    }

    private void tickTwoCbSession(
            ServerPlayer player,
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            EnumMap<SubstanceFamily, Double> rawFamilyTarget
    ) {
        if (twoCbSession == null) {
            return;
        }
        twoCbSession.setSettingMultiplier(TavernAmbienceService.twoCbIntensityMultiplier(player));
        twoCbSession.tick();
        if (!twoCbSession.isAlive()) {
            twoCbSession = null;
            return;
        }
        TwoCbEffectState state = twoCbState();
        EnumMap<ExperienceChannel, Double> twoCbChannels = zeroChannels();
        EnumMap<SubstanceFamily, Double> twoCbFamilies = zeroFamilies();
        TwoCbEffectProfile.contribute(state, twoCbChannels, twoCbFamilies);
        double blendWeight = Math.max(0.0D, TrippyConfig.twoCbBlendWeight);
        mergeScaled(target, twoCbChannels, blendWeight);
        mergeScaled(familyTarget, twoCbFamilies, blendWeight);
        rawFamilyTarget.merge(SubstanceFamily.PSYCHEDELIC,
                TwoCbEffectProfile.rawFamilyContribution(twoCbSession.effectState()) * blendWeight, Double::sum);
    }

    private static void applyPsychedelicBridge(
            EnumMap<ExperienceChannel, Double> target,
            EnumMap<SubstanceFamily, Double> familyTarget,
            PsilocybinEffectState psilocybin,
            LsdEffectState lsd,
            DmtEffectState dmt,
            PsychedelicBlendState blend,
            double potentiation,
            EnumMap<ExperienceChannel, Double> excludedVisuals
    ) {
        if (blend.bridgeStrength() <= 0.0D) {
            return;
        }

        if (psilocybin != null && lsd != null) {
            double bridge = blend.pairBridgeStrength(blend.psilocybinLoad(), blend.lsdLoad()) * potentiation;
            double psilocybinSafe = Math.max(1.0D, TrippyConfig.psilocybinMaxVisualIntensity);
            double lsdSafe = Math.max(1.0D, TrippyConfig.lsdMaxVisualIntensity);
            double organicGlow = psilocybin.colorWarmth() / psilocybinSafe + psilocybin.organicBreathing() / psilocybinSafe;
            double electricGeometry = lsd.electricBloom() / lsdSafe + lsd.prismSplit() / lsdSafe + lsd.realityFold() / lsdSafe;

            target.merge(ExperienceChannel.COLOR_BLOOM, bridge * (0.030D + organicGlow * 0.034D + electricGeometry * 0.030D), Double::sum);
            target.merge(ExperienceChannel.PATTERNING, bridge * (0.026D + psilocybin.fungalOverlays() / psilocybinSafe * 0.030D + lsd.prismSplit() / lsdSafe * 0.035D), Double::sum);
            target.merge(ExperienceChannel.TIME_DILATION, bridge * (0.024D + lsd.progress() * 0.016D + psilocybin.progress() * 0.010D), Double::sum);
            target.merge(ExperienceChannel.EGO_DISSOLUTION, bridge * (0.020D + Math.min(psilocybin.intensity() / psilocybinSafe, lsd.intensity() / lsdSafe) * 0.028D), Double::sum);
            familyTarget.merge(SubstanceFamily.PSYCHEDELIC, bridge * 0.090D, Double::sum);
        }

        if (dmt != null && psilocybin != null) {
            double bridge = blend.pairBridgeStrength(blend.dmtLoad(), blend.psilocybinLoad()) * potentiation;
            double dmtSafe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
            double psilocybinSafe = Math.max(1.0D, TrippyConfig.psilocybinMaxVisualIntensity);
            double organic = psilocybin.organicBreathing() / psilocybinSafe + psilocybin.surfaceMelting() / psilocybinSafe;
            double hyperspace = dmtHyperspaceSignal(dmt, dmtSafe);
            double melt = bridge * (0.035D + organic * 0.026D + hyperspace * 0.038D);
            target.merge(ExperienceChannel.GEOMETRY_MELT, melt, Double::sum);
            excludedVisuals.merge(ExperienceChannel.GEOMETRY_MELT, melt, Double::sum);
            target.merge(ExperienceChannel.PATTERNING, bridge * (0.034D + psilocybin.fungalOverlays() / psilocybinSafe * 0.028D + dmtPatternSignal(dmt, dmtSafe) * 0.046D), Double::sum);
            target.merge(ExperienceChannel.COLOR_BLOOM, bridge * (0.032D + psilocybin.colorWarmth() / psilocybinSafe * 0.030D + dmtBloomSignal(dmt, dmtSafe) * 0.048D), Double::sum);
            target.merge(ExperienceChannel.EGO_DISSOLUTION, bridge * (0.028D + dmtDissolveSignal(dmt, dmtSafe) * 0.052D), Double::sum);
            familyTarget.merge(SubstanceFamily.PSYCHEDELIC, bridge * 0.110D, Double::sum);
        }

        if (dmt != null && lsd != null) {
            double bridge = blend.pairBridgeStrength(blend.dmtLoad(), blend.lsdLoad()) * potentiation;
            double dmtSafe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
            double lsdSafe = Math.max(1.0D, TrippyConfig.lsdMaxVisualIntensity);
            double electric = lsd.prismSplit() / lsdSafe + lsd.realityFold() / lsdSafe + lsd.skyVortex() / lsdSafe;
            double hyperspace = dmtHyperspaceSignal(dmt, dmtSafe);
            target.merge(ExperienceChannel.PATTERNING, bridge * (0.038D + electric * 0.042D + hyperspace * 0.050D), Double::sum);
            target.merge(ExperienceChannel.COLOR_BLOOM, bridge * (0.036D + lsd.electricBloom() / lsdSafe * 0.040D + dmtBloomSignal(dmt, dmtSafe) * 0.048D), Double::sum);
            target.merge(ExperienceChannel.TIME_DILATION, bridge * (0.030D + lsd.progress() * 0.012D + dmtTemporalSignal(dmt, dmtSafe) * 0.034D), Double::sum);
            target.merge(ExperienceChannel.DISSOCIATION, bridge * (0.024D + dmtDissolveSignal(dmt, dmtSafe) * 0.045D + lsd.entityTrails() / lsdSafe * 0.022D), Double::sum);
            familyTarget.merge(SubstanceFamily.PSYCHEDELIC, bridge * 0.120D, Double::sum);
        }
    }

    private static double dmtHyperspaceSignal(DmtEffectState dmt, double safe) {
        var scaffold = dmt.fractalScaffold();
        return saturate(dmt.visualIntensity() / safe * 0.36D
                + scaffold.fractal3dPresence() * 0.24D
                + scaffold.animatedFractalPresence() * 0.22D
                + scaffold.staticAnimatedCoupling() * 0.20D
                + scaffold.alternateWorldReadiness() * 0.28D
                + scaffold.dimensionalDepth() * 0.16D);
    }

    private static double dmtPatternSignal(DmtEffectState dmt, double safe) {
        var scaffold = dmt.fractalScaffold();
        return saturate(dmt.visualIntensity() / safe * 0.30D
                + scaffold.fractal3dPresence() * 0.34D
                + scaffold.animatedFractalPresence() * 0.28D
                + scaffold.staticAnimatedCoupling() * 0.22D
                + dmt.signature().mandalaDensity() * 0.10D);
    }

    private static double dmtBloomSignal(DmtEffectState dmt, double safe) {
        var scaffold = dmt.fractalScaffold();
        return saturate(dmt.intensity() / safe * 0.28D
                + scaffold.staticAnimatedCoupling() * 0.24D
                + scaffold.alternateWorldReadiness() * 0.24D
                + scaffold.temporalFlow() * 0.18D
                + dmt.signature().colorStrobe() * 0.08D);
    }

    private static double dmtTemporalSignal(DmtEffectState dmt, double safe) {
        var scaffold = dmt.fractalScaffold();
        return saturate(dmt.intensity() / safe * 0.24D
                + scaffold.temporalFlow() * 0.36D
                + scaffold.dimensionalDepth() * 0.22D
                + dmt.signature().tunnelSpeed() * 0.14D);
    }

    private static double dmtDissolveSignal(DmtEffectState dmt, double safe) {
        var scaffold = dmt.fractalScaffold();
        return saturate(dmt.visualIntensity() / safe * 0.30D
                + scaffold.alternateWorldReadiness() * 0.38D
                + scaffold.dimensionalDepth() * 0.24D
                + scaffold.staticAnimatedCoupling() * 0.16D);
    }

    private void tickRewardTimers() {
        if (psychedelicLuckTicks > 0) {
            psychedelicLuckTicks--;
            if (psychedelicLuckTicks == 0) {
                psychedelicLuckStrength = 0.0D;
            }
        }
        if (dmtResistanceTicks > 0) {
            dmtResistanceTicks--;
            if (dmtResistanceTicks == 0) {
                dmtResistanceStrength = 0.0D;
            }
        }
    }

    private void queuePsychedelicLuck(int durationTicks, double strength) {
        if (strength <= 0.0D) {
            return;
        }
        psychedelicLuckStrength = Math.max(psychedelicLuckStrength, strength);
        psychedelicLuckTicks = Math.max(psychedelicLuckTicks, Math.max(20, durationTicks));
    }

    private void queueDmtResistance(double strength) {
        if (strength <= 0.0D) {
            return;
        }
        dmtResistanceStrength = Math.max(dmtResistanceStrength, strength);
        dmtResistanceTicks = Math.max(dmtResistanceTicks, scaledDurationTicks(MIN_DMT_RESISTANCE_TICKS, MAX_DMT_RESISTANCE_TICKS, strength));
    }

    private static double psilocybinPostTripStrength(PsilocybinEffectState state) {
        double doseFactor = smoothstep(0.55D, 10.0D, state.activeDoseGrams());
        double durationFactor = smoothstep(90.0D * 20.0D, 420.0D * 20.0D, state.durationTicks());
        double intensityFactor = saturate(state.intensity() / Math.max(1.0D, TrippyConfig.psilocybinMaxVisualIntensity));
        return saturate(doseFactor * 0.56D + durationFactor * 0.24D + intensityFactor * 0.20D);
    }

    private static int psilocybinLuckDurationTicks(PsilocybinEffectState state) {
        return minutesToTicks(state.targetDoseGrams());
    }

    private static double lsdPostTripStrength(LsdEffectState state) {
        double doseFactor = smoothstep(TrippyConfig.lsdTier1Micrograms, TrippyConfig.lsdTier5Micrograms, state.activeDoseMicrograms());
        double durationFactor = smoothstep(TrippyConfig.lsd50ugDurationSeconds * 20.0D, TrippyConfig.lsdStackedDurationSeconds * 20.0D, state.durationTicks());
        double intensityFactor = saturate(state.intensity() / Math.max(1.0D, TrippyConfig.lsdMaxVisualIntensity));
        return saturate(doseFactor * 0.54D + durationFactor * 0.26D + intensityFactor * 0.20D);
    }

    private static int lsdLuckDurationTicks(LsdEffectState state) {
        return minutesToTicks(state.targetDoseMicrograms() / 50.0D);
    }

    private static double dmtLuckPostTripStrength(DmtEffectState state) {
        double doseFactor = smoothstep(TrippyConfig.dmtTier1Units, TrippyConfig.dmtTier5Units, state.activeDoseUnits());
        double durationFactor = smoothstep(TrippyConfig.dmtLightDurationSeconds * 20.0D, TrippyConfig.dmtStackedDurationSeconds * 20.0D, state.durationTicks());
        double intensityFactor = saturate(state.intensity() / Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity));
        return saturate(doseFactor * 0.58D + durationFactor * 0.20D + intensityFactor * 0.22D);
    }

    private static int dmtLuckDurationTicks(DmtEffectState state) {
        return minutesToTicks(state.targetDoseUnits() * 2.5D);
    }

    private static double dmtResistancePostTripStrength(DmtEffectState state) {
        double doseFactor = smoothstep(TrippyConfig.dmtPreBreakthroughThresholdUnits, TrippyConfig.dmtTier5Units, state.activeDoseUnits());
        double tierFactor = saturate((state.tier() - 1.0D) / 4.0D);
        double intensityFactor = saturate(state.intensity() / Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity));
        return saturate(doseFactor * 0.55D + tierFactor * 0.25D + intensityFactor * 0.20D);
    }

    private static int scaledDurationTicks(int minTicks, int maxTicks, double strength) {
        return (int) Math.round(minTicks + (maxTicks - minTicks) * saturate(strength));
    }

    private static int minutesToTicks(double minutes) {
        return Math.max(20, (int) Math.round(Math.max(0.0D, minutes) * 60.0D * 20.0D));
    }

    private static int postTripLuckAmplifier(double strength) {
        return strength >= 0.92D ? 1 : 0;
    }

    private static int postTripResistanceAmplifier(double strength) {
        if (strength >= 0.92D) {
            return 2;
        }
        if (strength >= 0.62D) {
            return 1;
        }
        return 0;
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = saturate((value - edge0) / (edge1 - edge0));
        return t * t * (3.0D - 2.0D * t);
    }

    private void updateDepthState(SetSettingSnapshot setting, EnumMap<SubstanceFamily, Double> rawFamilyTarget, EnumMap<SubstanceFamily, Double> familyTarget) {
        double recovery = setting.recoverySupport() * TrippyConfig.recoveryRate;
        double recoveryDecay = 0.000025D + recovery * 0.000075D;
        double idleDecay = 0.000018D + recovery * 0.000060D;

        for (SubstanceFamily family : SubstanceFamily.values()) {
            double raw = rawFamilyTarget.getOrDefault(family, 0.0D);
            double active = compress(familyTarget.getOrDefault(family, 0.0D));
            double previous = families.getOrDefault(family, 0.0D);
            double falling = Math.max(0.0D, previous - active);

            double nextTolerance = tolerance.getOrDefault(family, 0.0D);
            nextTolerance += raw * toleranceBuildRate(family) * (1.0D - nextTolerance);
            nextTolerance -= recoveryDecay * (0.45D + recoveryPotential(family));
            tolerance.put(family, saturate(nextTolerance));

            double nextAfterglow = afterglow.getOrDefault(family, 0.0D);
            nextAfterglow = Math.max(nextAfterglow, active * afterglowPotential(family) * 0.30D);
            nextAfterglow -= idleDecay * (0.55D + recovery);
            afterglow.put(family, saturate(nextAfterglow));

            double nextComedown = comedown.getOrDefault(family, 0.0D);
            nextComedown = Math.max(nextComedown, falling * comedownPotential(family) * 0.65D);
            nextComedown += active * comedownPotential(family) * 0.000030D;
            nextComedown -= recoveryDecay * (0.70D + recovery);
            comedown.put(family, saturate(nextComedown));

            double nextWithdrawal = withdrawal.getOrDefault(family, 0.0D);
            double tolerancePressure = tolerance.getOrDefault(family, 0.0D) * withdrawalPotential(family);
            if (active < 0.10D && tolerancePressure > 0.16D) {
                nextWithdrawal += (tolerancePressure - active) * 0.00018D * TrippyConfig.riskSeverity;
            }
            nextWithdrawal -= recoveryDecay * (0.60D + recovery);
            withdrawal.put(family, saturate(nextWithdrawal));

            double nextCraving = craving.getOrDefault(family, 0.0D);
            double cravingPressure = (nextComedown * 0.38D + nextWithdrawal * 0.62D + tolerance.getOrDefault(family, 0.0D) * 0.18D) * cravingPotential(family);
            nextCraving += cravingPressure * 0.00032D * TrippyConfig.riskSeverity;
            nextCraving -= recoveryDecay * (0.58D + recovery);
            craving.put(family, saturate(nextCraving));
        }

        double mixedTarget = TrippyConfig.enableRiskMechanics ? mixedRiskTarget(familyTarget) * TrippyConfig.riskSeverity : 0.0D;
        mixedRisk += (saturate(mixedTarget) - mixedRisk) * 0.055D;

        double strainTarget = TrippyConfig.enableRiskMechanics ? strainTarget(setting) * TrippyConfig.riskSeverity : 0.0D;
        strain += (saturate(strainTarget) - strain) * 0.045D;
        strain = Math.max(0.0D, strain - recovery * 0.00010D);
    }

    private void applyDepthChannels(EnumMap<ExperienceChannel, Double> target) {
        double afterglowLoad = weighted(afterglow, 0.60D, 0.34D, 0.12D, 0.08D, 0.48D, 0.18D, 0.08D, 0.02D);
        double comedownLoad = weighted(comedown, 0.22D, 0.20D, 0.48D, 0.68D, 0.30D, 0.42D, 0.44D, 0.62D);
        double cravingLoad = weighted(craving, 0.10D, 0.32D, 0.78D, 0.86D, 0.36D, 0.20D, 0.38D, 0.30D);
        double withdrawalLoad = weighted(withdrawal, 0.10D, 0.20D, 0.56D, 0.74D, 0.24D, 0.46D, 0.52D, 0.58D);

        if (afterglowLoad > 0.0D) {
            target.merge(ExperienceChannel.EUPHORIA, afterglowLoad * 0.26D, Double::sum);
            target.merge(ExperienceChannel.COLOR_BLOOM, afterglowLoad * 0.14D, Double::sum);
            target.merge(ExperienceChannel.DEPTH_BREATHING, afterglowLoad * 0.08D, Double::sum);
        }

        if (comedownLoad > 0.0D) {
            target.merge(ExperienceChannel.BODY_LOAD, comedownLoad * 0.30D, Double::sum);
            target.merge(ExperienceChannel.SEDATION, comedownLoad * 0.20D, Double::sum);
            target.merge(ExperienceChannel.MOTOR_NOISE, comedownLoad * 0.13D, Double::sum);
            target.merge(ExperienceChannel.ANXIETY, comedownLoad * 0.11D, Double::sum);
        }

        if (withdrawalLoad > 0.0D) {
            target.merge(ExperienceChannel.BODY_LOAD, withdrawalLoad * 0.34D, Double::sum);
            target.merge(ExperienceChannel.MOTOR_NOISE, withdrawalLoad * 0.20D, Double::sum);
            target.merge(ExperienceChannel.ANXIETY, withdrawalLoad * 0.22D, Double::sum);
            target.merge(ExperienceChannel.SEDATION, withdrawalLoad * 0.12D, Double::sum);
        }

        if (cravingLoad > 0.0D) {
            target.merge(ExperienceChannel.CRAVING, cravingLoad * 0.54D, Double::sum);
            target.merge(ExperienceChannel.ANXIETY, cravingLoad * 0.10D, Double::sum);
        }

        if (TrippyConfig.enableRiskMechanics) {
            target.merge(ExperienceChannel.RISK, mixedRisk * 0.52D + strain * 0.58D + cravingLoad * 0.24D + withdrawalLoad * 0.30D, Double::sum);
            target.merge(ExperienceChannel.BODY_LOAD, strain * 0.18D, Double::sum);
            target.merge(ExperienceChannel.MOTOR_NOISE, mixedRisk * 0.12D, Double::sum);
        }
    }

    private void applySynergy(EnumMap<ExperienceChannel, Double> target, EnumMap<SubstanceFamily, Double> familyTarget) {
        double psychedelicLoad = target.getOrDefault(ExperienceChannel.PATTERNING, 0.0D)
                + target.getOrDefault(ExperienceChannel.GEOMETRY_MELT, 0.0D);
        double dissociation = target.getOrDefault(ExperienceChannel.DISSOCIATION, 0.0D);
        double stimulation = target.getOrDefault(ExperienceChannel.STIMULATION, 0.0D);
        double sedation = target.getOrDefault(ExperienceChannel.SEDATION, 0.0D);

        if (psychedelicLoad > 0.0D && dissociation > 0.0D) {
            target.merge(ExperienceChannel.TIME_DILATION, psychedelicLoad * dissociation * 0.12D, Double::sum);
            target.merge(ExperienceChannel.EGO_DISSOLUTION, psychedelicLoad * dissociation * 0.10D, Double::sum);
        }
        if (stimulation > 0.0D && sedation > 0.0D) {
            target.merge(ExperienceChannel.RISK, Math.min(stimulation, sedation) * 0.24D * TrippyConfig.riskSeverity, Double::sum);
            target.merge(ExperienceChannel.MOTOR_NOISE, Math.min(stimulation, sedation) * 0.18D, Double::sum);
        }

        applyMdmaVisualAmplifier(target);

        applyCannabisModerator(target, familyTarget);
    }

    /**
     * MDMA is rendered after the other world-warp passes, so boosting the shared channels here lets its
     * dose-dependent colour grade bind the whole mixture together without turning it into another psychedelic.
     */
    private void applyMdmaVisualAmplifier(EnumMap<ExperienceChannel, Double> target) {
        MdmaEffectState mdma = mdmaState();
        if (!mdma.tripActive()) {
            return;
        }
        double dose = smoothstep(25.0D, 500.0D,
                Math.max(mdma.activeDoseMilligrams(), mdma.targetDoseMilligrams()));
        double colour = saturate((mdma.saturation() + mdma.bloom()) * 0.42D);
        double visualGain = dose * (0.22D + colour * 0.62D);
        multiplyChannel(target, ExperienceChannel.COLOR_BLOOM, 1.0D + visualGain);
        multiplyChannel(target, ExperienceChannel.VISUAL_DRIFT, 1.0D + visualGain * 0.18D);
        multiplyChannel(target, ExperienceChannel.DEPTH_BREATHING, 1.0D + visualGain * 0.16D);
        multiplyChannel(target, ExperienceChannel.PATTERNING, 1.0D + visualGain * 0.08D);
        multiplyChannel(target, ExperienceChannel.GEOMETRY_MELT, 1.0D + visualGain * 0.06D);
        multiplyChannel(target, ExperienceChannel.EUPHORIA, 1.0D + dose * 0.20D);
    }

    private static void multiplyChannel(EnumMap<ExperienceChannel, Double> target,
                                        ExperienceChannel channel,
                                        double multiplier) {
        double value = target.getOrDefault(channel, 0.0D);
        if (value > 0.0D) {
            target.put(channel, value * multiplier);
        }
    }

    private void applyCannabisModerator(EnumMap<ExperienceChannel, Double> target, EnumMap<SubstanceFamily, Double> familyTarget) {
        CannabisEffectState cannabis = cannabisState();
        if (!cannabis.active()) {
            return;
        }
        applyCannabisAntiemetic(target, cannabis);
    }

    private void applyCannabisAntiemetic(EnumMap<ExperienceChannel, Double> target, CannabisEffectState cannabis) {
        double nausea = target.getOrDefault(ExperienceChannel.NAUSEA, 0.0D);
        if (nausea <= 0.0D) {
            return;
        }
        double antiemetic = smoothstep(0.04D, 0.42D, cannabisModeratorLoad(cannabis))
                * (0.42D + smoothstep(1.0D, 20.0D, cannabis.doseScale()) * 0.30D);
        target.put(ExperienceChannel.NAUSEA, Math.max(0.0D, nausea * (1.0D - antiemetic)));
    }

    private void smoothVanillaNauseaVisuals(EnumMap<ExperienceChannel, Double> target,
                                          EnumMap<ExperienceChannel, Double> excludedVisuals) {
        // Keep the full visual channels for rendering. Only the vanilla Nausea
        // bridge excludes psilocybin and LSD, including their legacy fallbacks
        // and blends. Actual nausea and other substances retain their sources.
        EnumMap<ExperienceChannel, Double> bridge = new EnumMap<>(target);
        for (ExperienceChannel channel : new ExperienceChannel[] {ExperienceChannel.VISUAL_DRIFT, ExperienceChannel.GEOMETRY_MELT}) {
            bridge.put(channel, Math.max(0.0D, target.getOrDefault(channel, 0.0D)
                    - excludedVisuals.getOrDefault(channel, 0.0D)));
        }
        applyMdmaVisualAmplifier(bridge);
        vanillaNauseaVisualDrift = smoothChannel(vanillaNauseaVisualDrift, bridge.getOrDefault(ExperienceChannel.VISUAL_DRIFT, 0.0D));
        vanillaNauseaGeometryMelt = smoothChannel(vanillaNauseaGeometryMelt, bridge.getOrDefault(ExperienceChannel.GEOMETRY_MELT, 0.0D));
    }

    private static double smoothChannel(double current, double target) {
        double next = compress(target);
        double alpha = next > current ? 0.08D : 0.025D;
        return current + (next - current) * alpha;
    }

    private void smoothChannels(EnumMap<ExperienceChannel, Double> target) {
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            double current = channels.getOrDefault(channel, 0.0D);
            channels.put(channel, smoothChannel(current, target.getOrDefault(channel, 0.0D)));
        }
    }

    private void smoothFamilies(EnumMap<SubstanceFamily, Double> familyTarget) {
        for (SubstanceFamily family : SubstanceFamily.values()) {
            double current = families.getOrDefault(family, 0.0D);
            double next = compress(familyTarget.getOrDefault(family, 0.0D));
            double alpha = next > current ? 0.10D : 0.030D;
            families.put(family, current + (next - current) * alpha);
        }
    }

    private double responseMultiplier(SubstanceFamily family) {
        double direct = tolerance.getOrDefault(family, 0.0D);
        double cross = crossTolerancePressure(family);
        return Math.max(0.35D, 1.0D - direct * 0.44D - cross * 0.24D);
    }

    private double crossTolerancePressure(SubstanceFamily family) {
        return switch (family) {
            case PSYCHEDELIC -> tolerance.getOrDefault(SubstanceFamily.DISSOCIATIVE, 0.0D) * 0.18D
                    + tolerance.getOrDefault(SubstanceFamily.DELIRIANT, 0.0D) * 0.16D;
            case CANNABINOID -> tolerance.getOrDefault(SubstanceFamily.DEPRESSANT, 0.0D) * 0.12D
                    + tolerance.getOrDefault(SubstanceFamily.OPIOID, 0.0D) * 0.10D;
            case STIMULANT -> tolerance.getOrDefault(SubstanceFamily.EMPATHOGEN, 0.0D) * 0.28D;
            case OPIOID -> tolerance.getOrDefault(SubstanceFamily.DEPRESSANT, 0.0D) * 0.32D;
            case EMPATHOGEN -> tolerance.getOrDefault(SubstanceFamily.STIMULANT, 0.0D) * 0.24D
                    + tolerance.getOrDefault(SubstanceFamily.PSYCHEDELIC, 0.0D) * 0.10D;
            case DISSOCIATIVE -> tolerance.getOrDefault(SubstanceFamily.PSYCHEDELIC, 0.0D) * 0.12D
                    + tolerance.getOrDefault(SubstanceFamily.DEPRESSANT, 0.0D) * 0.14D;
            case DEPRESSANT -> tolerance.getOrDefault(SubstanceFamily.OPIOID, 0.0D) * 0.36D
                    + tolerance.getOrDefault(SubstanceFamily.DISSOCIATIVE, 0.0D) * 0.12D;
            case DELIRIANT -> tolerance.getOrDefault(SubstanceFamily.PSYCHEDELIC, 0.0D) * 0.10D
                    + tolerance.getOrDefault(SubstanceFamily.DISSOCIATIVE, 0.0D) * 0.18D;
        };
    }

    private double mixedRiskTarget(EnumMap<SubstanceFamily, Double> familyTarget) {
        double psychedelic = activeFamily(familyTarget, SubstanceFamily.PSYCHEDELIC);
        double cannabinoid = activeFamily(familyTarget, SubstanceFamily.CANNABINOID);
        double stimulant = activeFamily(familyTarget, SubstanceFamily.STIMULANT);
        double opioid = activeFamily(familyTarget, SubstanceFamily.OPIOID);
        double empathogen = activeFamily(familyTarget, SubstanceFamily.EMPATHOGEN);
        double dissociative = activeFamily(familyTarget, SubstanceFamily.DISSOCIATIVE);
        double depressant = activeFamily(familyTarget, SubstanceFamily.DEPRESSANT);
        double deliriant = activeFamily(familyTarget, SubstanceFamily.DELIRIANT);

        double upper = stimulant + empathogen * 0.55D;
        double lower = opioid + depressant + dissociative * 0.35D + cannabinoid * 0.20D;
        double perceptual = psychedelic + dissociative + deliriant;
        return saturate(upper * lower * 0.72D
                + opioid * depressant * 0.82D
                + psychedelic * dissociative * 0.24D
                + stimulant * psychedelic * 0.20D
                + deliriant * perceptual * 0.44D);
    }

    private double strainTarget(SetSettingSnapshot setting) {
        double cravingLoad = weighted(craving, 0.10D, 0.32D, 0.78D, 0.86D, 0.36D, 0.20D, 0.38D, 0.30D);
        double withdrawalLoad = weighted(withdrawal, 0.10D, 0.20D, 0.56D, 0.74D, 0.24D, 0.46D, 0.52D, 0.58D);
        double activeRisk = channel(ExperienceChannel.RISK);
        return saturate(activeRisk * 0.42D
                + mixedRisk * 0.40D
                + cravingLoad * 0.24D
                + withdrawalLoad * 0.34D
                + setting.threat() * 0.18D
                - setting.recoverySupport() * 0.22D);
    }

    private static double activeFamily(EnumMap<SubstanceFamily, Double> familyTarget, SubstanceFamily family) {
        return compress(familyTarget.getOrDefault(family, 0.0D));
    }

    private static double cannabisPotentiationMultiplier(SubstanceFamily family, CannabisEffectState cannabis) {
        if (cannabis == null || !cannabis.active() || family == SubstanceFamily.CANNABINOID) {
            return 1.0D;
        }
        double phaseStrength = switch (cannabis.phase()) {
            case ONSET -> 0.88D;
            case LIFT, CRUISE -> 1.0D;
            case SINK -> 0.55D;
            case AFTERGLOW -> 0.18D;
            case SOBER -> 0.0D;
        };
        if (phaseStrength <= 0.0D) {
            return 1.0D;
        }
        double activeThc = Math.max(0.0D, cannabis.activeThcMilligrams());
        double fullStrengthMultiplier;
        if (activeThc <= 100.0D) {
            double ratio = saturate(activeThc / 100.0D);
            fullStrengthMultiplier = 1.0D + ratio * (2.0D - ratio);
        } else {
            fullStrengthMultiplier = 2.0D + Math.max(0.0D, activeThc - 100.0D) * 0.0075D;
        }
        return 1.0D + (fullStrengthMultiplier - 1.0D) * phaseStrength;
    }

    private static double substanceMixMultiplier(SubstanceFamily family, CannabisEffectState cannabis, KetamineEffectState ketamine) {
        return cannabisPotentiationMultiplier(family, cannabis) * ketamineAttenuationMultiplier(family, ketamine);
    }

    private static double cannabisCocaineMultiplier(CannabisEffectState cannabis) {
        if (cannabis == null || !cannabis.active()) {
            return 1.0D;
        }
        double phaseStrength = switch (cannabis.phase()) {
            case ONSET -> 0.88D;
            case LIFT, CRUISE -> 1.0D;
            case SINK -> 0.55D;
            case AFTERGLOW -> 0.18D;
            case SOBER -> 0.0D;
        };
        if (phaseStrength <= 0.0D) {
            return 1.0D;
        }
        double activeThc = Math.max(0.0D, cannabis.activeThcMilligrams());
        double suppression = 0.45D * smoothstep(0.0D, 100.0D, activeThc)
                + 0.40D * smoothstep(100.0D, 500.0D, activeThc);
        return Math.max(0.12D, 1.0D - suppression * phaseStrength);
    }

    private static double ketamineAttenuationMultiplier(SubstanceFamily family, KetamineEffectState ketamine) {
        if (ketamine == null || !ketamine.active() || family == SubstanceFamily.DISSOCIATIVE) {
            return 1.0D;
        }
        return KetamineMasking.otherSubstanceMultiplier(ketamine);
    }

    private static double cannabisModeratorLoad(CannabisEffectState cannabis) {
        double load = saturate(Math.log1p(Math.max(0.0D, cannabis.intensity())) / Math.log1p(18.0D));
        load += smoothstep(2.0D, 20.0D, cannabis.doseScale()) * 0.12D;
        load += saturate(cannabis.focusTunnel() * 0.04D + cannabis.temporalDrag() * 0.04D + cannabis.immersion() * 0.03D);
        return saturate(load);
    }

    private static CannabisEffectState potentiateCannabisState(CannabisEffectState state, double multiplier) {
        if (!state.active() || !hasMeaningfulMultiplier(multiplier)) {
            return state;
        }
        return new CannabisEffectState(
                state.active(),
                state.displayThcMilligrams(),
                amplifyMetric(state.activeThcMilligrams(), multiplier),
                amplifyMetric(state.targetThcMilligrams(), multiplier),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.progress(),
                amplifyMetric(state.doseScale(), multiplier),
                amplifyMetric(state.intensity(), multiplier),
                amplifyMetric(state.immersion(), multiplier),
                amplifyMetric(state.temporalDrag(), multiplier),
                amplifyMetric(state.breathingPulse(), multiplier),
                amplifyMetric(state.focusTunnel(), multiplier),
                amplifyMetric(state.audioGlue(), multiplier),
                amplifyMetric(state.thoughtStickiness(), multiplier),
                amplifyMetric(state.warmthShift(), multiplier),
                amplifyMetric(state.saturationBoost(), multiplier),
                amplifyMetric(state.greenRichness(), multiplier)
        );
    }

    private static PsilocybinEffectState potentiatePsilocybinState(PsilocybinEffectState state, double multiplier) {
        if (!state.active() || !hasMeaningfulMultiplier(multiplier)) {
            return state;
        }
        return new PsilocybinEffectState(
                state.active(),
                state.displayDoseGrams(),
                amplifyMetric(state.activeDoseGrams(), multiplier),
                amplifyMetric(state.targetDoseGrams(), multiplier),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.progress(),
                amplifyMetric(state.intensity(), multiplier),
                amplifyMetric(state.visualIntensity(), multiplier),
                amplifyMetric(state.organicBreathing(), multiplier),
                amplifyMetric(state.surfaceMelting(), multiplier),
                amplifyMetric(state.tracers(), multiplier),
                amplifyMetric(state.spores(), multiplier),
                amplifyMetric(state.fungalOverlays(), multiplier),
                amplifyMetric(state.colorWarmth(), multiplier),
                amplifyMetric(state.colorDepth(), multiplier),
                amplifyMetric(state.audioDepth(), multiplier),
                amplifyMetric(state.cameraBreathing(), multiplier),
                state.signature(),
                state.environment()
        );
    }

    private static LsdEffectState potentiateLsdState(LsdEffectState state, double multiplier) {
        if (!state.active() || !hasMeaningfulMultiplier(multiplier)) {
            return state;
        }
        return new LsdEffectState(
                state.active(),
                state.displayDoseMicrograms(),
                amplifyMetric(state.activeDoseMicrograms(), multiplier),
                amplifyMetric(state.targetDoseMicrograms(), multiplier),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.progress(),
                state.tier(),
                amplifyMetric(state.intensity(), multiplier),
                amplifyMetric(state.visualIntensity(), multiplier),
                amplifyMetric(state.edgeHum(), multiplier),
                amplifyMetric(state.geometryMelt(), multiplier),
                amplifyMetric(state.realityFold(), multiplier),
                amplifyMetric(state.blockAutonomy(), multiplier),
                amplifyMetric(state.liquidFlow(), multiplier),
                amplifyMetric(state.skyVortex(), multiplier),
                amplifyMetric(state.entityTrails(), multiplier),
                amplifyMetric(state.prismSplit(), multiplier),
                amplifyMetric(state.electricBloom(), multiplier),
                state.signature(),
                state.environment()
        );
    }

    private static DmtEffectState potentiateDmtState(DmtEffectState state, double multiplier) {
        if (!state.active() || !hasMeaningfulMultiplier(multiplier)) {
            return state;
        }
        return new DmtEffectState(
                state.active(),
                state.displayDoseUnits(),
                amplifyMetric(state.activeDoseUnits(), multiplier),
                amplifyMetric(state.targetDoseUnits(), multiplier),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.stage(),
                state.progress(),
                state.tier(),
                amplifyMetric(state.intensity(), multiplier),
                amplifyMetric(state.visualIntensity(), multiplier),
                amplifyMetric(state.chrysanthemum(), multiplier),
                amplifyMetric(state.tunnelDepth(), multiplier),
                amplifyMetric(state.hyperGeometry(), multiplier),
                amplifyMetric(state.fractalOverload(), multiplier),
                amplifyMetric(state.worldDissolve(), multiplier),
                amplifyMetric(state.chromaticBurst(), multiplier),
                amplifyMetric(state.entityPresence(), multiplier),
                amplifyMetric(state.egoLoss(), multiplier),
                amplifyMetric(state.cameraPull(), multiplier),
                amplifyMetric(state.alienShimmer(), multiplier),
                amplifyMetric(state.spatialInstability(), multiplier),
                amplifyMetric(state.edgeFlicker(), multiplier),
                amplifyMetric(state.symbolPressure(), multiplier),
                amplifyMetric(state.veilThinness(), multiplier),
                amplifyMetric(state.breakthroughTransition(), multiplier),
                amplifyMetric(state.impossibleArchitecture(), multiplier),
                amplifyMetric(state.symbolLanguage(), multiplier),
                amplifyMetric(state.environmentalConsciousness(), multiplier),
                amplifyMetric(state.realityReplacement(), multiplier),
                amplifyMetric(state.dominance(), multiplier),
                potentiateDmtFractalScaffold(state.fractalScaffold(), multiplier),
                potentiateDmtEncounterState(state.encounter(), multiplier),
                state.signature(),
                state.environment()
        );
    }

    private static SalviaEffectState potentiateSalviaState(SalviaEffectState state, double multiplier) {
        if (!state.active() || !hasMeaningfulMultiplier(multiplier)) {
            return state;
        }
        return new SalviaEffectState(
                state.active(),
                state.displayDoseUnits(),
                amplifyMetric(state.activeDoseUnits(), multiplier),
                amplifyMetric(state.targetDoseUnits(), multiplier),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.progress(),
                state.tier(),
                amplifyMetric(state.intensity(), multiplier),
                amplifyMetric(state.visualIntensity(), multiplier),
                amplifyMetric(state.anchorSlip(), multiplier),
                amplifyMetric(state.bodyPull(), multiplier),
                amplifyMetric(state.sceneReplacement(), multiplier),
                amplifyMetric(state.loopSeam(), multiplier),
                amplifyMetric(state.cutoutPresence(), multiplier),
                amplifyMetric(state.observerPressure(), multiplier),
                amplifyMetric(state.reentryConfusion(), multiplier),
                amplifyMetric(state.megaPresence(), multiplier),
                amplifyMetric(state.wheelConveyor(), multiplier),
                amplifyMetric(state.zipperFold(), multiplier),
                amplifyMetric(state.flattening(), multiplier),
                amplifyMetric(state.objecthood(), multiplier),
                amplifyMetric(state.entityContact(), multiplier),
                amplifyMetric(state.realityStack(), multiplier),
                amplifyMetric(state.kaleidoscope(), multiplier),
                state.signature(),
                state.environment()
        );
    }

    private static AlcoholEffectState potentiateAlcoholState(AlcoholEffectState state, double multiplier) {
        if (!state.active() || !hasMeaningfulMultiplier(multiplier)) {
            return state;
        }
        return new AlcoholEffectState(
                state.active(),
                state.displayStandardDrinks(),
                amplifyMetric(state.activeStandardDrinks(), multiplier),
                amplifyMetric(state.targetStandardDrinks(), multiplier),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.progress(),
                state.tier(),
                amplifyMetric(state.intensity(), multiplier),
                amplifyMetric(state.sedation(), multiplier),
                amplifyMetric(state.motorNoise(), multiplier),
                amplifyMetric(state.diplopia(), multiplier),
                amplifyMetric(state.focusBlur(), multiplier),
                amplifyMetric(state.vestibularSway(), multiplier),
                amplifyMetric(state.peripheralNarrowing(), multiplier),
                amplifyMetric(state.euphoria(), multiplier),
                amplifyMetric(state.nausea(), multiplier),
                amplifyMetric(state.disinhibition(), multiplier),
                amplifyMetric(state.risk(), multiplier)
        );
    }

    private static CocaineEffectState potentiateCocaineState(CocaineEffectState state, double multiplier) {
        if (!state.active() || !hasMeaningfulMultiplier(multiplier)) {
            return state;
        }
        return new CocaineEffectState(
                state.active(),
                state.displayDoseGrams(),
                amplifyMetric(state.activeDoseGrams(), multiplier),
                amplifyMetric(state.targetDoseGrams(), multiplier),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.progress(),
                state.tier(),
                amplifyMetric(state.intensity(), multiplier),
                amplifyMetric(state.stimulation(), multiplier),
                amplifyMetric(state.locomotorDrive(), multiplier),
                amplifyMetric(state.inputSharpness(), multiplier),
                amplifyMetric(state.jitter(), multiplier),
                amplifyMetric(state.overcorrection(), multiplier),
                amplifyMetric(state.compulsiveMomentum(), multiplier),
                amplifyMetric(state.tunnelFocus(), multiplier),
                amplifyMetric(state.clarityLift(), multiplier),
                amplifyMetric(state.edgeShimmer(), multiplier),
                amplifyMetric(state.lightHarshness(), multiplier),
                amplifyMetric(state.microFlicker(), multiplier),
                amplifyMetric(state.anxiety(), multiplier),
                amplifyMetric(state.euphoria(), multiplier),
                amplifyMetric(state.craving(), multiplier),
                amplifyMetric(state.risk(), multiplier),
                amplifyMetric(state.crashDrag(), multiplier),
                amplifyMetric(state.crashDesaturation(), multiplier),
                saturate(state.alcoholMask() * (0.88D + (multiplier - 1.0D) * 0.42D))
        );
    }

    private static HeroinEffectState potentiateHeroinState(HeroinEffectState state, double multiplier, double recoveryMultiplier) {
        double totalMultiplier = multiplier * recoveryMultiplier;
        if (!state.active() || !hasMeaningfulMultiplier(totalMultiplier)) {
            return state;
        }
        return new HeroinEffectState(
                state.active(),
                state.displayDoseMilligrams(),
                amplifyMetric(state.activeDoseMilligrams(), multiplier),
                amplifyMetric(state.targetDoseMilligrams(), multiplier),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.progress(),
                state.tier(),
                amplifyMetric(state.intensity(), totalMultiplier),
                amplifyMetric(state.warmth(), totalMultiplier),
                amplifyMetric(state.softFocus(), totalMultiplier),
                amplifyMetric(state.euphoria(), totalMultiplier),
                amplifyMetric(state.sedation(), multiplier),
                amplifyMetric(state.painSuppression(), totalMultiplier),
                amplifyMetric(state.hungerSuppression(), totalMultiplier),
                amplifyMetric(state.regeneration(), totalMultiplier),
                amplifyMetric(state.resistance(), totalMultiplier),
                amplifyMetric(state.absorption(), totalMultiplier),
                amplifyMetric(state.motorDrag(), multiplier),
                amplifyMetric(state.miningDrag(), multiplier),
                amplifyMetric(state.nodding(), multiplier),
                amplifyMetric(state.microSleep(), multiplier),
                amplifyMetric(state.respiratorySuppression(), multiplier),
                amplifyMetric(state.nauseaRelief(), totalMultiplier),
                amplifyMetric(state.appetiteSuppression(), totalMultiplier)
        );
    }

    private static KetamineEffectState potentiateKetamineState(KetamineEffectState state, double multiplier) {
        if (!state.active() || !hasMeaningfulMultiplier(multiplier)) {
            return state;
        }
        return new KetamineEffectState(
                state.active(),
                state.displayDoseMilligrams(),
                amplifyMetric(state.activeDoseMilligrams(), multiplier),
                amplifyMetric(state.targetDoseMilligrams(), multiplier),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.progress(),
                state.tier(),
                amplifyMetric(state.intensity(), multiplier),
                amplifyMetric(state.dissociation(), multiplier),
                amplifyMetric(state.timeSlip(), multiplier),
                amplifyMetric(state.motorDisconnect(), multiplier),
                amplifyMetric(state.bodyFloat(), multiplier),
                amplifyMetric(state.cameraLag(), multiplier),
                amplifyMetric(state.depthMismatch(), multiplier),
                amplifyMetric(state.audioMuffle(), multiplier),
                amplifyMetric(state.gravityDrift(), multiplier),
                amplifyMetric(state.inputLatency(), multiplier),
                amplifyMetric(state.painSuppression(), multiplier),
                amplifyMetric(state.uiFade(), multiplier),
                amplifyMetric(state.bodyEcho(), multiplier),
                amplifyMetric(state.collapseRisk(), multiplier),
                amplifyMetric(state.collapseDepth(), multiplier)
        );
    }

    private static AlcoholEffectState maskAlcoholState(AlcoholEffectState state, CocaineEffectState cocaine) {
        if (state == null || !state.active() || cocaine == null || !cocaine.active()) {
            return state;
        }
        double mask = saturate(cocaine.alcoholMask());
        if (mask <= 0.01D) {
            return state;
        }
        double intensityFactor = 1.0D - mask * 0.86D;
        double bodyFactor = 1.0D - mask * 0.96D;
        double moodFactor = 1.0D - mask * 0.55D;
        double riskFactor = 1.0D - mask * 0.18D;
        return new AlcoholEffectState(
                state.active(),
                state.displayStandardDrinks(),
                state.activeStandardDrinks(),
                state.targetStandardDrinks(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.phase(),
                state.progress(),
                state.tier(),
                state.intensity() * intensityFactor,
                state.sedation() * bodyFactor,
                state.motorNoise() * bodyFactor,
                state.diplopia() * bodyFactor,
                state.focusBlur() * bodyFactor,
                state.vestibularSway() * bodyFactor,
                state.peripheralNarrowing() * bodyFactor,
                state.euphoria() * moodFactor,
                state.nausea() * bodyFactor,
                state.disinhibition() * moodFactor,
                state.risk() * riskFactor
        );
    }

    private static DmtFractalScaffold potentiateDmtFractalScaffold(DmtFractalScaffold scaffold, double multiplier) {
        return new DmtFractalScaffold(
                amplifyMetric(scaffold.fractal3dPresence(), multiplier),
                amplifyMetric(scaffold.animatedFractalPresence(), multiplier),
                amplifyMetric(scaffold.staticAnimatedCoupling(), multiplier),
                amplifyMetric(scaffold.alternateWorldReadiness(), multiplier),
                amplifyMetric(scaffold.dimensionalDepth(), multiplier),
                amplifyMetric(scaffold.temporalFlow(), multiplier),
                amplifyMetric(scaffold.morphRate(), multiplier),
                scaffold.phase(),
                scaffold.structureSeed(),
                scaffold.animationSeed()
        );
    }

    private static boolean hasMeaningfulMultiplier(double multiplier) {
        return Double.isFinite(multiplier) && multiplier > 0.0D && Math.abs(multiplier - 1.0D) > 0.0001D;
    }

    private static DmtEncounterState potentiateDmtEncounterState(DmtEncounterState encounter, double multiplier) {
        return new DmtEncounterState(
                encounter.active(),
                encounter.theme(),
                encounter.eventType(),
                amplifyMetric(encounter.themeStrength(), multiplier),
                amplifyMetric(encounter.eventStrength(), multiplier),
                encounter.eventProgress(),
                encounter.eventSeed(),
                amplifyMetric(encounter.architectureFocus(), multiplier),
                amplifyMetric(encounter.entityFocus(), multiplier),
                amplifyMetric(encounter.symbolFocus(), multiplier),
                amplifyMetric(encounter.consciousnessFocus(), multiplier)
        );
    }

    private static double amplifyMetric(double value, double multiplier) {
        if (!Double.isFinite(value) || value <= 0.0D || !hasMeaningfulMultiplier(multiplier)) {
            return value;
        }
        return value * multiplier;
    }

    private static double toleranceBuildRate(SubstanceFamily family) {
        return switch (family) {
            case PSYCHEDELIC -> 0.000095D;
            case CANNABINOID -> 0.000080D;
            case STIMULANT -> 0.000125D;
            case OPIOID -> 0.000135D;
            case EMPATHOGEN -> 0.000115D;
            case DISSOCIATIVE -> 0.000105D;
            case DEPRESSANT -> 0.000120D;
            case DELIRIANT -> 0.000090D;
        };
    }

    private static double afterglowPotential(SubstanceFamily family) {
        return switch (family) {
            case PSYCHEDELIC -> 0.70D;
            case CANNABINOID -> 0.34D;
            case STIMULANT -> 0.08D;
            case OPIOID -> 0.05D;
            case EMPATHOGEN -> 0.62D;
            case DISSOCIATIVE -> 0.22D;
            case DEPRESSANT -> 0.06D;
            case DELIRIANT -> 0.03D;
        };
    }

    private static double comedownPotential(SubstanceFamily family) {
        return switch (family) {
            case PSYCHEDELIC -> 0.28D;
            case CANNABINOID -> 0.24D;
            case STIMULANT -> 0.74D;
            case OPIOID -> 0.70D;
            case EMPATHOGEN -> 0.52D;
            case DISSOCIATIVE -> 0.44D;
            case DEPRESSANT -> 0.50D;
            case DELIRIANT -> 0.66D;
        };
    }

    private static double cravingPotential(SubstanceFamily family) {
        return switch (family) {
            case PSYCHEDELIC -> 0.0D;
            case CANNABINOID -> 0.24D;
            case STIMULANT -> 0.82D;
            case OPIOID -> 0.88D;
            case EMPATHOGEN -> 0.34D;
            case DISSOCIATIVE -> 0.16D;
            case DEPRESSANT -> 0.46D;
            case DELIRIANT -> 0.18D;
        };
    }

    private static double withdrawalPotential(SubstanceFamily family) {
        return switch (family) {
            case PSYCHEDELIC -> 0.0D;
            case CANNABINOID -> 0.18D;
            case STIMULANT -> 0.50D;
            case OPIOID -> 0.78D;
            case EMPATHOGEN -> 0.28D;
            case DISSOCIATIVE -> 0.26D;
            case DEPRESSANT -> 0.62D;
            case DELIRIANT -> 0.20D;
        };
    }

    private static double recoveryPotential(SubstanceFamily family) {
        return switch (family) {
            case PSYCHEDELIC, EMPATHOGEN -> 0.72D;
            case CANNABINOID -> 0.60D;
            case STIMULANT, DISSOCIATIVE -> 0.48D;
            case OPIOID, DEPRESSANT -> 0.38D;
            case DELIRIANT -> 0.34D;
        };
    }

    private static double weighted(EnumMap<SubstanceFamily, Double> values, double psychedelic, double cannabinoid, double stimulant, double opioid, double empathogen, double dissociative, double depressant, double deliriant) {
        return saturate(values.getOrDefault(SubstanceFamily.PSYCHEDELIC, 0.0D) * psychedelic
                + values.getOrDefault(SubstanceFamily.CANNABINOID, 0.0D) * cannabinoid
                + values.getOrDefault(SubstanceFamily.STIMULANT, 0.0D) * stimulant
                + values.getOrDefault(SubstanceFamily.OPIOID, 0.0D) * opioid
                + values.getOrDefault(SubstanceFamily.EMPATHOGEN, 0.0D) * empathogen
                + values.getOrDefault(SubstanceFamily.DISSOCIATIVE, 0.0D) * dissociative
                + values.getOrDefault(SubstanceFamily.DEPRESSANT, 0.0D) * depressant
                + values.getOrDefault(SubstanceFamily.DELIRIANT, 0.0D) * deliriant);
    }

    private static EnumMap<ExperienceChannel, Double> zeroChannels() {
        EnumMap<ExperienceChannel, Double> values = new EnumMap<>(ExperienceChannel.class);
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            values.put(channel, 0.0D);
        }
        return values;
    }

    private static EnumMap<SubstanceFamily, Double> zeroFamilies() {
        EnumMap<SubstanceFamily, Double> values = new EnumMap<>(SubstanceFamily.class);
        for (SubstanceFamily family : SubstanceFamily.values()) {
            values.put(family, 0.0D);
        }
        return values;
    }

    private static <T extends Enum<T>> void mergeScaled(EnumMap<T, Double> target, EnumMap<T, Double> source, double scale) {
        if (scale <= 0.0D) {
            return;
        }
        for (Map.Entry<T, Double> entry : source.entrySet()) {
            double value = entry.getValue() * scale;
            if (value > 0.0D) {
                target.merge(entry.getKey(), value, Double::sum);
            }
        }
    }

    private static double compress(double value) {
        if (value <= 0.0D) {
            return 0.0D;
        }
        return 1.0D - Math.exp(-value);
    }

    private static double saturate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    public double channel(ExperienceChannel channel) {
        return channels.getOrDefault(channel, 0.0D);
    }

    public double vanillaNauseaVisualLoad() {
        return Math.max(vanillaNauseaVisualDrift, vanillaNauseaGeometryMelt);
    }

    public Map<ExperienceChannel, Double> channels() {
        return Map.copyOf(channels);
    }

    public double family(SubstanceFamily family) {
        return families.getOrDefault(family, 0.0D);
    }

    public Map<SubstanceFamily, Double> familyBlend() {
        return Map.copyOf(families);
    }

    public double tolerance(SubstanceFamily family) {
        return tolerance.getOrDefault(family, 0.0D);
    }

    public double afterglow(SubstanceFamily family) {
        return afterglow.getOrDefault(family, 0.0D);
    }

    public double comedown(SubstanceFamily family) {
        return comedown.getOrDefault(family, 0.0D);
    }

    public double craving(SubstanceFamily family) {
        return craving.getOrDefault(family, 0.0D);
    }

    public double withdrawal(SubstanceFamily family) {
        return withdrawal.getOrDefault(family, 0.0D);
    }

    public double mixedRisk() {
        return mixedRisk;
    }

    public double strain() {
        return strain;
    }

    public String depthSummary() {
        StringBuilder builder = new StringBuilder();
        appendFamilyMetric(builder, "tol", tolerance);
        appendFamilyMetric(builder, "after", afterglow);
        appendFamilyMetric(builder, "down", comedown);
        appendFamilyMetric(builder, "crave", craving);
        appendFamilyMetric(builder, "withdraw", withdrawal);
        if (mixedRisk >= 0.04D) {
            builder.append(", mixed_risk=").append(fmt(mixedRisk));
        }
        if (strain >= 0.04D) {
            builder.append(", strain=").append(fmt(strain));
        }
        return builder.length() == 0 ? "depth=quiet" : builder.substring(2);
    }

    public PsilocybinEffectState psilocybinState() {
        if (psilocybinSession == null || !psilocybinSession.isAlive()) {
            return PsilocybinEffectState.inactive();
        }
        return potentiatePsilocybinState(psilocybinSession.effectState(), substanceMixMultiplier(SubstanceFamily.PSYCHEDELIC, cannabisState(), rawKetamineState()) * responseMultiplier(SubstanceFamily.PSYCHEDELIC));
    }

    public LsdEffectState lsdState() {
        if (lsdSession == null || !lsdSession.isAlive()) {
            return LsdEffectState.inactive();
        }
        return potentiateLsdState(lsdSession.effectState(), substanceMixMultiplier(SubstanceFamily.PSYCHEDELIC, cannabisState(), rawKetamineState()) * responseMultiplier(SubstanceFamily.PSYCHEDELIC));
    }

    public DmtEffectState dmtState() {
        if (dmtSession == null || !dmtSession.isAlive()) {
            return DmtEffectState.inactive();
        }
        return potentiateDmtState(dmtSession.effectState(), substanceMixMultiplier(SubstanceFamily.PSYCHEDELIC, cannabisState(), rawKetamineState()));
    }

    public HoMetEffectState hoMetState() {
        if (hoMetSession == null || !hoMetSession.isAlive()) return HoMetEffectState.inactive();
        return hoMetSession.effectState().scaled(responseMultiplier(SubstanceFamily.PSYCHEDELIC)
                * substanceMixMultiplier(SubstanceFamily.PSYCHEDELIC, cannabisState(), rawKetamineState()));
    }

    public AlLadEffectState alLadState() {
        if (alLadSession == null || !alLadSession.isAlive()) {
            return AlLadEffectState.inactive();
        }
        return alLadSession.effectState().scaled(responseMultiplier(SubstanceFamily.PSYCHEDELIC)
                * substanceMixMultiplier(SubstanceFamily.PSYCHEDELIC, cannabisState(), rawKetamineState()));
    }

    public TwoCeEffectState twoCeState() {
        if (twoCeSession == null || !twoCeSession.isAlive()) {
            return TwoCeEffectState.inactive();
        }
        return twoCeSession.effectState().scaled(responseMultiplier(SubstanceFamily.PSYCHEDELIC)
                * substanceMixMultiplier(SubstanceFamily.PSYCHEDELIC, cannabisState(), rawKetamineState()));
    }

    public NitrousEffectState nitrousState() {
        if (nitrousSession == null || !nitrousSession.isAlive()) {
            return NitrousEffectState.inactive();
        }
        return nitrousSession.effectState();
    }

    public MescalineEffectState mescalineState() {
        if (mescalineSession == null || !mescalineSession.isAlive()) {
            return MescalineEffectState.inactive();
        }
        return mescalineSession.effectState().scaled(responseMultiplier(SubstanceFamily.PSYCHEDELIC));
    }

    public CannabisEffectState cannabisState() {
        CannabisEffectState cannabis = rawCannabisState();
        if (!cannabis.active()) {
            return cannabis;
        }
        return potentiateCannabisState(cannabis, ketamineAttenuationMultiplier(SubstanceFamily.CANNABINOID, rawKetamineState()));
    }

    public MdmaEffectState mdmaState() {
        MdmaEffectState state = mdmaSession == null || !mdmaSession.isAlive()
                ? MdmaEffectState.inactive()
                : mdmaSession.effectState();
        state = attenuateMdmaForTolerance(state, mdmaRecovery.activeEffectMultiplier());
        state = mixMdmaState(state, cannabisState(), alcoholState(), rawKetamineState(), cocaineState());
        state = applyMdmaRecoveryState(state, mdmaRecovery.snapshot());
        double settingMultiplier = mdmaSession == null ? 1.0D : mdmaSession.settingMultiplier();
        return amplifyMdmaState(state, settingMultiplier);
    }

    public TwoCbEffectState twoCbState() {
        if (twoCbSession == null || !twoCbSession.isAlive()) {
            return TwoCbEffectState.inactive();
        }
        return mixTwoCbState(
                twoCbSession.effectState(),
                psilocybinState(), lsdState(), dmtState(), cannabisState(), mdmaState(),
                rawKetamineState(), cocaineState(), heroinState(), salviaState(), alcoholState()
        ).scaled(responseMultiplier(SubstanceFamily.PSYCHEDELIC));
    }

    public KetamineEffectState ketamineState() {
        KetamineEffectState ketamine = rawKetamineState();
        if (!ketamine.active()) {
            return ketamine;
        }
        return potentiateKetamineState(ketamine, cannabisPotentiationMultiplier(SubstanceFamily.DISSOCIATIVE, cannabisState()));
    }

    public CocaineEffectState cocaineState() {
        if (cocaineSession == null || !cocaineSession.isAlive()) {
            return CocaineEffectState.inactive();
        }
        return potentiateCocaineState(cocaineSession.effectState(), cannabisCocaineMultiplier(cannabisState()) * ketamineAttenuationMultiplier(SubstanceFamily.STIMULANT, rawKetamineState()));
    }

    public HeroinEffectState heroinState() {
        if (heroinSession == null || !heroinSession.isAlive()) {
            return HeroinEffectState.inactive();
        }
        return potentiateHeroinState(
                heroinSession.effectState(),
                substanceMixMultiplier(SubstanceFamily.OPIOID, cannabisState(), rawKetamineState()),
                heroinRecovery.activeEffectMultiplier()
        );
    }

    public SalviaEffectState salviaState() {
        if (salviaSession == null || !salviaSession.isAlive()) {
            return SalviaEffectState.inactive();
        }
        return potentiateSalviaState(salviaSession.effectState(), substanceMixMultiplier(SubstanceFamily.DELIRIANT, cannabisState(), rawKetamineState()));
    }

    public AlcoholEffectState alcoholState() {
        if (alcoholSession == null || !alcoholSession.isAlive()) {
            return AlcoholEffectState.inactive();
        }
        AlcoholEffectState alcohol = potentiateAlcoholState(alcoholSession.effectState(), substanceMixMultiplier(SubstanceFamily.DEPRESSANT, cannabisState(), rawKetamineState()));
        return maskAlcoholState(alcohol, cocaineState());
    }

    private static MdmaEffectState mixMdmaState(MdmaEffectState state,
                                                 CannabisEffectState cannabis,
                                                 AlcoholEffectState alcohol,
                                                 KetamineEffectState ketamine,
                                                 CocaineEffectState cocaine) {
        if (!state.active()) {
            return state;
        }
        double cannabisLoad = cannabis != null && cannabis.active()
                ? saturate(Math.log1p(Math.max(0.0D, cannabis.intensity())) / Math.log1p(18.0D))
                : 0.0D;
        double alcoholLoad = alcohol != null && alcohol.active()
                ? saturate(alcohol.activeStandardDrinks() / 8.0D)
                : 0.0D;
        double ketamineMask = ketamine != null && ketamine.active()
                ? KetamineMasking.otherSubstanceMask(ketamine)
                : 0.0D;
        double cocaineGrams = cocaine != null && cocaine.active()
                ? Math.max(cocaine.activeDoseGrams(), cocaine.targetDoseGrams())
                : 0.0D;
        double ketamineBlend = 1.0D - ketamineMask * 0.42D;
        double cocaineSynergy = state.tripActive() ? 1.0D + Math.min(1.25D, cocaineGrams * 0.18D) : 1.0D;
        double activeBlend = state.tripActive() ? ketamineBlend * cocaineSynergy : 1.0D;
        double dreaminess = cannabisLoad * (0.34D + saturate(state.intensity() / 1.6D) * 0.42D);
        dreaminess += ketamineMask * 0.12D;
        double coordinationNoise = alcoholLoad * (0.30D + saturate(state.intensity() / 1.6D) * 0.48D)
                + Math.min(0.42D, cocaineGrams * 0.035D);
        return new MdmaEffectState(
                state.active(),
                state.displayDoseMilligrams(),
                state.activeDoseMilligrams(),
                state.targetDoseMilligrams(),
                state.ageTicks(),
                state.durationTicks(),
                state.remainingTicks(),
                state.tripActive(),
                state.recoveryTicks(),
                state.recoveryDurationTicks(),
                state.phase(),
                state.progress(),
                state.tier(),
                state.repeatedDoses(),
                state.intensity() * activeBlend,
                state.warmth() * (1.0D + cannabisLoad * 0.10D) * activeBlend,
                state.saturation() * (1.0D + cannabisLoad * 0.22D) * activeBlend,
                (state.bloom() * (1.0D + cannabisLoad * 0.28D) + coordinationNoise * 0.10D) * activeBlend,
                state.bassSensitivity() * (1.0D + cannabisLoad * 0.12D) * activeBlend,
                state.fovBreathing() * (1.0D + cannabisLoad * 0.24D) * activeBlend,
                state.movementLift() * (1.0D - alcoholLoad * 0.22D) * activeBlend,
                state.hungerSuppression() * activeBlend,
                state.regeneration() * (1.0D - alcoholLoad * 0.32D) * activeBlend,
                state.socialAura() * activeBlend,
                state.touchAffinity() * activeBlend,
                state.audioFullness() * (1.0D + cannabisLoad * 0.10D) * activeBlend,
                state.euphoria() * activeBlend,
                state.comedownDesaturation() + alcoholLoad * 0.08D,
                state.comedownFatigue() + alcoholLoad * 0.12D,
                state.audioMuffle() + alcoholLoad * 0.08D,
                coordinationNoise,
                dreaminess,
                state.repeatedUsePenalty()
        );
    }

    private static TwoCbEffectState mixTwoCbState(TwoCbEffectState state,
                                                   PsilocybinEffectState psilocybin,
                                                   LsdEffectState lsd,
                                                   DmtEffectState dmt,
                                                   CannabisEffectState cannabis,
                                                   MdmaEffectState mdma,
                                                   KetamineEffectState ketamine,
                                                   CocaineEffectState cocaine,
                                                   HeroinEffectState heroin,
                                                   SalviaEffectState salvia,
                                                   AlcoholEffectState alcohol) {
        if (!state.active()) {
            return state;
        }
        double psilocybinLoad = psilocybin.active() ? saturate(psilocybin.intensity() / 3.2D) : 0.0D;
        double lsdLoad = lsd.active() ? saturate(lsd.intensity() / 3.4D) : 0.0D;
        double dmtLoad = dmt.active() ? saturate(dmt.dominance() / 3.0D + dmt.intensity() / 8.0D) : 0.0D;
        double cannabisLoad = cannabis.active() ? saturate(Math.log1p(cannabis.intensity()) / Math.log1p(18.0D)) : 0.0D;
        double mdmaLoad = mdma.active() && mdma.tripActive() ? saturate(mdma.intensity() / 2.2D) : 0.0D;
        double ketamineMask = ketamine.active() ? KetamineMasking.otherSubstanceMask(ketamine) : 0.0D;
        double cocaineSynergy = cocaine.active() ? saturate(
                cocaine.stimulation() / 3.4D * 0.42D
                        + cocaine.inputSharpness() / 2.8D * 0.33D
                        + cocaine.edgeShimmer() / 2.2D * 0.25D) : 0.0D;
        double heroinLoad = heroin.active() ? saturate(heroin.sedation() / 5.0D) : 0.0D;
        double salviaLoad = salvia.active() ? saturate(salvia.sceneReplacement() / 3.2D + salvia.intensity() / 8.0D) : 0.0D;
        double alcoholLoad = alcohol.active() ? saturate(alcohol.activeStandardDrinks() / 8.0D) : 0.0D;

        // Dissociatives and reality-replacing trips reduce readable 2C-B geometry; the later renderer passes remain dominant.
        double coherence = Math.max(0.34D, 1.0D - ketamineMask * 0.46D - dmtLoad * 0.34D - salviaLoad * 0.27D);
        double psychedelicBlend = 1.0D + cannabisLoad * 0.18D + lsdLoad * 0.14D + psilocybinLoad * 0.08D;
        double depressantDulling = Math.max(0.45D, 1.0D - alcoholLoad * 0.24D - heroinLoad * 0.30D);
        double stimulantDrive = 1.0D + mdmaLoad * 0.14D + cocaineSynergy * 0.30D;

        return new TwoCbEffectState(
                state.active(), state.displayDoseMilligrams(), state.activeDoseMilligrams(), state.targetDoseMilligrams(),
                state.ageTicks(), state.durationTicks(), state.remainingTicks(), state.phase(), state.progress(), state.tier(),
                state.repeatedDoses(), state.intensity() * coherence * psychedelicBlend * stimulantDrive,
                state.saturation() * coherence * depressantDulling * (1.0D + cannabisLoad * 0.16D + mdmaLoad * 0.26D + cocaineSynergy * 0.22D),
                state.edgeGlow() * coherence * depressantDulling * (1.0D + lsdLoad * 0.24D + cocaineSynergy * 0.52D),
                state.surfaceGeometry() * coherence * (1.0D + psilocybinLoad * 0.12D + lsdLoad * 0.24D + cannabisLoad * 0.18D + cocaineSynergy * 0.28D),
                state.tracerStrength() * coherence * (1.0D + lsdLoad * 0.20D + mdmaLoad * 0.14D + cocaineSynergy * 0.62D),
                state.bassSensitivity() * coherence * (1.0D + mdmaLoad * 0.34D + cannabisLoad * 0.10D + cocaineSynergy * 0.30D),
                state.fisheyePulse() * coherence * (1.0D + mdmaLoad * 0.24D + ketamineMask * 0.10D + cocaineSynergy * 0.18D),
                state.symmetry() * coherence * (1.0D + lsdLoad * 0.28D + cannabisLoad * 0.14D + cocaineSynergy * 0.24D),
                state.crystalline() * coherence * (1.0D + lsdLoad * 0.24D - psilocybinLoad * 0.10D + cocaineSynergy * 0.30D),
                state.sensuality() * depressantDulling * (1.0D + mdmaLoad * 0.38D + cannabisLoad * 0.20D + cocaineSynergy * 0.14D),
                state.stimulation() * Math.max(0.40D, depressantDulling) * (1.0D + mdmaLoad * 0.20D + cocaineSynergy * 0.70D),
                state.anxiety() + cocaineSynergy * 0.34D + dmtLoad * 0.20D + salviaLoad * 0.18D + alcoholLoad * 0.10D,
                state.settingMultiplier()
        );
    }

    private static MdmaEffectState attenuateMdmaForTolerance(MdmaEffectState state, double multiplier) {
        if (!state.active() || !state.tripActive()) {
            return state;
        }
        double gain = Math.max(0.30D, Math.min(1.0D, multiplier));
        return new MdmaEffectState(
                state.active(), state.displayDoseMilligrams(), state.activeDoseMilligrams(), state.targetDoseMilligrams(),
                state.ageTicks(), state.durationTicks(), state.remainingTicks(), state.tripActive(),
                state.recoveryTicks(), state.recoveryDurationTicks(), state.phase(), state.progress(), state.tier(),
                state.repeatedDoses(), state.intensity() * gain, state.warmth() * gain, state.saturation() * gain,
                state.bloom() * gain, state.bassSensitivity() * gain, state.fovBreathing() * gain,
                state.movementLift() * gain, state.hungerSuppression() * gain, state.regeneration() * gain,
                state.socialAura() * gain, state.touchAffinity() * gain, state.audioFullness() * gain,
                state.euphoria() * gain, state.comedownDesaturation(), state.comedownFatigue(), state.audioMuffle(),
                state.coordinationNoise(), state.dreaminess(), state.repeatedUsePenalty()
        );
    }

    private static MdmaEffectState applyMdmaRecoveryState(MdmaEffectState state,
                                                           MdmaRecoveryTracker.RecoveryState recovery) {
        if (state.tripActive() || recovery.suppressed()) {
            return state;
        }
        double withdrawal = recovery.withdrawalSeverity();
        double depletion = recovery.depletionProgress();
        if (!state.active() && withdrawal <= 0.002D) {
            return state;
        }
        double dose = state.active() ? state.activeDoseMilligrams() : recovery.recoveryPeakMilligrams();
        double desaturation = state.comedownDesaturation() + withdrawal * (0.28D + depletion * 0.20D);
        double fatigue = state.comedownFatigue() + withdrawal * (0.38D + depletion * 0.28D);
        double muffle = state.audioMuffle() + withdrawal * (0.24D + depletion * 0.20D);
        return new MdmaEffectState(
                true, dose, dose, dose,
                state.ageTicks(), state.durationTicks(), 0, false,
                state.recoveryTicks(), state.recoveryDurationTicks(),
                state.active() ? state.phase() : MdmaPhase.HANGOVER,
                1.0D, state.active() ? state.tier() : com.trippycraft.substance.mdma.MdmaDoseCurve.tier(dose),
                state.repeatedDoses(), 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                desaturation, fatigue, muffle, state.coordinationNoise(), state.dreaminess(),
                Math.max(state.repeatedUsePenalty(), depletion * 0.72D)
        );
    }

    private static MdmaEffectState amplifyMdmaState(MdmaEffectState state, double multiplier) {
        if (!state.active() || multiplier <= 1.0D) {
            return state;
        }
        double gain = Math.min(1.5D, multiplier);
        return new MdmaEffectState(
                state.active(), state.displayDoseMilligrams(), state.activeDoseMilligrams(), state.targetDoseMilligrams(),
                state.ageTicks(), state.durationTicks(), state.remainingTicks(), state.tripActive(),
                state.recoveryTicks(), state.recoveryDurationTicks(), state.phase(), state.progress(), state.tier(),
                state.repeatedDoses(), state.intensity() * gain, state.warmth() * gain, state.saturation() * gain,
                state.bloom() * gain, state.bassSensitivity() * gain, state.fovBreathing() * gain,
                state.movementLift() * gain, state.hungerSuppression() * gain, state.regeneration() * gain,
                state.socialAura() * gain, state.touchAffinity() * gain, state.audioFullness() * gain,
                state.euphoria() * gain, state.comedownDesaturation() * gain, state.comedownFatigue() * gain,
                state.audioMuffle() * gain, state.coordinationNoise() * gain, state.dreaminess() * gain,
                state.repeatedUsePenalty()
        );
    }

    public double ketamineOtherEffectMultiplier() {
        return KetamineMasking.otherSubstanceMultiplier(rawKetamineState());
    }

    public double cannabisPotentiationFactor(SubstanceFamily family) {
        return cannabisPotentiationMultiplier(family, cannabisState());
    }

    private CannabisEffectState rawCannabisState() {
        if (cannabisSession == null || !cannabisSession.isAlive()) {
            return CannabisEffectState.inactive();
        }
        return cannabisSession.effectState();
    }

    private KetamineEffectState rawKetamineState() {
        if (ketamineSession == null || !ketamineSession.isAlive()) {
            return KetamineEffectState.inactive();
        }
        return ketamineSession.effectState();
    }

    public AlcoholRecoveryTracker.RecoveryState alcoholRecoveryState() {
        return alcoholRecovery.snapshot();
    }

    public HeroinRecoveryTracker.RecoveryState heroinRecoveryState() {
        return heroinRecovery.snapshot();
    }

    public MdmaRecoveryTracker.RecoveryState mdmaRecoveryState() {
        return mdmaRecovery.snapshot();
    }

    public boolean mdmaBlocksSleep() {
        return mdmaSession != null && mdmaSession.isAlive() && mdmaSession.tripActive();
    }

    public boolean twoCbBlocksSleep() {
        return twoCbSession != null && twoCbSession.isAlive();
    }

    public boolean cocaineBlocksSleep() {
        return cocaineSession != null && cocaineSession.blocksSleep() && !ketamineOverridesCocaineSleepBlock();
    }

    public boolean heroinAllowsSleep() {
        HeroinEffectState heroin = heroinState();
        if (!heroin.active()) {
            return false;
        }
        double load = Math.max(heroin.sedation(), heroin.painSuppression()) / 6.8D;
        return load >= 0.10D || heroin.activeDoseMilligrams() >= 25.0D;
    }

    public boolean heroinOverridesMdmaOrTwoCbSleepBlock() {
        // Once heroin is in its sleep-permitting state, MDMA and 2C-B may still
        // contribute their other effects but may no longer reject the bed action.
        return heroinAllowsSleep();
    }

    public boolean heroinDisruptsSleep() {
        return !heroinAllowsSleep() && heroinRecovery.sleepDisrupted();
    }

    private boolean ketamineOverridesCocaineSleepBlock() {
        KetamineEffectState ketamine = rawKetamineState();
        if (!ketamine.active()) {
            return false;
        }
        return Math.max(ketamine.activeDoseMilligrams(), ketamine.targetDoseMilligrams()) >= 100.0D;
    }

    public int cocaineAftermathTicks() {
        return cocaineSession == null ? 0 : cocaineSession.aftermathTicks();
    }

    public double cocaineSystemDoseGrams() {
        return cocaineSession == null ? 0.0D : cocaineSession.sessionDoseGrams();
    }

    public double heroinSystemDoseMilligrams() {
        return heroinSession == null ? 0.0D : Math.max(heroinSession.activeDoseMilligrams(), heroinSession.targetDoseMilligrams());
    }

    public double heroinToleranceProgress() {
        return heroinRecovery.toleranceProgress();
    }

    public double mdmaSystemDoseMilligrams() {
        return mdmaSession == null || !mdmaSession.tripActive()
                ? 0.0D
                : Math.max(mdmaSession.effectState().activeDoseMilligrams(), mdmaSession.effectState().targetDoseMilligrams());
    }

    public double twoCbSystemDoseMilligrams() {
        return twoCbSession == null || !twoCbSession.isAlive()
                ? 0.0D
                : Math.max(twoCbSession.effectState().activeDoseMilligrams(), twoCbSession.effectState().targetDoseMilligrams());
    }

    public double mdmaToleranceProgress() {
        return mdmaRecovery.overdoseToleranceProgress();
    }

    public int heroinOverdoseChecksUpToMilligrams() {
        return heroinRecovery.overdoseChecksUpToMilligrams();
    }

    public void setHeroinOverdoseChecksUpToMilligrams(int value) {
        heroinRecovery.setOverdoseChecksUpToMilligrams(value);
    }

    public void applyHeroinNutritionPenalty(ServerPlayer player, FoodProperties food) {
        if (food == null || food.nutrition() <= 0) {
            return;
        }
        double penalty = heroinRecovery.nutritionPenalty(cannabisState().active());
        if (penalty <= 0.02D) {
            return;
        }
        int foodLoss = (int) Math.floor(food.nutrition() * penalty + 0.35D);
        if (foodLoss > 0) {
            player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - foodLoss));
        }
        float saturationLoss = (float) (food.saturation() * penalty);
        if (saturationLoss > 0.0F) {
            player.getFoodData().setSaturation(Math.max(0.0F, player.getFoodData().getSaturationLevel() - saturationLoss));
        }
    }

    public int psychedelicLuckTicks() {
        return psychedelicLuckTicks;
    }

    public int psychedelicLuckAmplifier() {
        return postTripLuckAmplifier(psychedelicLuckStrength);
    }

    public int dmtResistanceTicks() {
        return dmtResistanceTicks;
    }

    public int dmtResistanceAmplifier() {
        return postTripResistanceAmplifier(dmtResistanceStrength);
    }

    public boolean scrubPsilocybin(int newAgeTicks) {
        if (psilocybinSession != null && psilocybinSession.isAlive()) {
            psilocybinSession.setAgeTicks(newAgeTicks);
            return true;
        }
        if (hoMetSession != null && hoMetSession.isAlive()) {
            hoMetSession.setAgeTicks(newAgeTicks);
            return true;
        }
        if (alLadSession != null && alLadSession.isAlive()) {
            alLadSession.setAgeTicks(newAgeTicks);
            return true;
        }
        if (twoCeSession != null && twoCeSession.isAlive()) {
            twoCeSession.setAgeTicks(newAgeTicks);
            return true;
        }
        if (nitrousSession == null || !nitrousSession.isAlive()) {
            return false;
        }
        nitrousSession.setAgeTicks(newAgeTicks);
        return true;
    }

    public boolean endActiveTripFromSleep() {
        boolean changed = psilocybinSession != null
                || lsdSession != null
                || dmtSession != null
                || cannabisSession != null
                || mdmaSession != null
                || twoCbSession != null
                || mescalineSession != null
                || hoMetSession != null
                || alLadSession != null
                || twoCeSession != null
                || nitrousSession != null
                || ketamineSession != null
                || cocaineSession != null
                || heroinSession != null
                || salviaSession != null
                || alcoholSession != null
                || !exposures.isEmpty()
                || channels.values().stream().anyMatch(value -> value > 0.001D)
                || families.values().stream().anyMatch(value -> value > 0.001D)
                || tolerance.values().stream().anyMatch(value -> value > 0.001D)
                || afterglow.values().stream().anyMatch(value -> value > 0.001D)
                || comedown.values().stream().anyMatch(value -> value > 0.001D)
                || craving.values().stream().anyMatch(value -> value > 0.001D)
                || withdrawal.values().stream().anyMatch(value -> value > 0.001D)
                || mixedRisk > 0.001D
                || strain > 0.001D
                || psychedelicLuckTicks > 0
                || dmtResistanceTicks > 0
                || heroinRecovery.isActive()
                || alcoholRecovery.isActive()
                || mdmaRecovery.isActive();
        if (!changed) {
            return false;
        }

        exposures.clear();
        psilocybinSession = null;
        lsdSession = null;
        dmtSession = null;
        cannabisSession = null;
        twoCbSession = null;
        mescalineSession = null;
        hoMetSession = null;
        alLadSession = null;
        twoCeSession = null;
        nitrousSession = null;
        if (mdmaSession != null) {
            if (mdmaSession.tripActive()) {
                mdmaRecovery.forceEndSession(mdmaSession.effectState());
            }
            mdmaSession.onSleep();
            if (!mdmaSession.isAlive()) {
                mdmaSession = null;
            }
        }
        ketamineSession = null;
        cocaineSession = null;
        if (heroinSession != null) {
            heroinRecovery.forceEndSession(heroinSession.effectState());
        }
        heroinSession = null;
        salviaSession = null;
        if (alcoholSession != null) {
            alcoholRecovery.forceEndSession(alcoholSession.effectState());
        }
        alcoholSession = null;
        alcoholRecovery.onSleep();
        heroinRecovery.onSleep();
        mdmaRecovery.onSleep();
        mixedRisk = 0.0D;
        strain = 0.0D;
        psychedelicLuckTicks = 0;
        psychedelicLuckStrength = 0.0D;
        dmtResistanceTicks = 0;
        dmtResistanceStrength = 0.0D;
        psilocybinAfterglowRewardApplied = false;
        lsdAfterglowRewardApplied = false;
        dmtAfterglowRewardApplied = false;
        psilocybinAlcoholResetStage = 0;
        lsdAlcoholResetStage = 0;
        dmtAlcoholResetStage = 0;
        clearMap(channels);
        vanillaNauseaVisualDrift = 0.0D;
        vanillaNauseaGeometryMelt = 0.0D;
        clearMap(families);
        clearMap(tolerance);
        clearMap(afterglow);
        clearMap(comedown);
        clearMap(craving);
        clearMap(withdrawal);
        return true;
    }

    public void clearAlcoholAfterDeath() {
        alcoholSession = null;
        alcoholRecovery.resetAfterDeath();
        heroinSession = null;
        heroinRecovery.resetAfterDeath();
        mdmaSession = null;
        mdmaRecovery.resetAfterDeath();
        twoCbSession = null;
        mescalineSession = null;
        hoMetSession = null;
        alLadSession = null;
        twoCeSession = null;
        nitrousSession = null;
    }

    public String psilocybinSummary() {
        if (psilocybinSession == null || !psilocybinSession.isAlive()) {
            return "psilocybin=inactive";
        }
        return psilocybinSession.debugSummary();
    }

    public String lsdSummary() {
        if (lsdSession == null || !lsdSession.isAlive()) {
            return "lsd=inactive";
        }
        return lsdSession.debugSummary();
    }

    public String dmtSummary() {
        if (dmtSession == null || !dmtSession.isAlive()) {
            return "dmt=inactive";
        }
        return dmtSession.debugSummary();
    }

    public String cannabisSummary() {
        if (cannabisSession == null || !cannabisSession.isAlive()) {
            return "cannabis=inactive";
        }
        return cannabisSession.debugSummary();
    }

    public String mdmaSummary() {
        if (mdmaSession == null || !mdmaSession.isAlive()) {
            return mdmaRecovery.isActive() ? mdmaRecovery.debugSummary() : "mdma=inactive";
        }
        return mdmaSession.debugSummary() + ", " + mdmaRecovery.debugSummary();
    }

    public String twoCbSummary() {
        if (twoCbSession == null || !twoCbSession.isAlive()) {
            return "2cb=inactive";
        }
        return twoCbSession.debugSummary();
    }

    public String hoMetSummary() {
        return hoMetSession == null || !hoMetSession.isAlive() ? "4-ho-met=inactive" : hoMetSession.debugSummary();
    }

    public String alLadSummary() {
        return alLadSession == null || !alLadSession.isAlive() ? "al-lad=inactive" : alLadSession.debugSummary();
    }

    public String twoCeSummary() {
        return twoCeSession == null || !twoCeSession.isAlive() ? "2c-e=inactive" : twoCeSession.debugSummary();
    }

    public String nitrousSummary() {
        return nitrousSession == null || !nitrousSession.isAlive() ? "nitrous=inactive" : nitrousSession.debugSummary();
    }

    public String mescalineSummary() {
        if (mescalineSession == null || !mescalineSession.isAlive()) {
            return "mescaline=inactive";
        }
        return mescalineSession.debugSummary();
    }

    public String ketamineSummary() {
        if (ketamineSession == null || !ketamineSession.isAlive()) {
            return "ketamine=inactive";
        }
        return ketamineSession.debugSummary();
    }

    public String salviaSummary() {
        if (salviaSession == null || !salviaSession.isAlive()) {
            return "salvia=inactive";
        }
        return salviaSession.debugSummary();
    }

    public String cocaineSummary() {
        if (cocaineSession == null || !cocaineSession.isAlive()) {
            return "cocaine=inactive";
        }
        return cocaineSession.debugSummary();
    }

    public String heroinSummary() {
        if (heroinSession == null || !heroinSession.isAlive()) {
            return heroinRecovery.isActive() ? heroinRecovery.debugSummary() : "heroin=inactive";
        }
        return heroinSession.debugSummary() + ", " + heroinRecovery.debugSummary();
    }

    public String alcoholSummary() {
        if (alcoholSession == null || !alcoholSession.isAlive()) {
            return alcoholRecovery.isActive() ? alcoholRecovery.debugSummary() : "alcohol=inactive";
        }
        return alcoholSession.debugSummary() + ", " + alcoholRecovery.debugSummary();
    }

    private int applyPsilocybinAlcoholReset(double targetDoseGrams, int currentStage) {
        if (currentStage < 1 && targetDoseGrams >= 5.0D) {
            alcoholRecovery.applyPsilocybinTherapy(targetDoseGrams, currentStage);
            currentStage = 1;
        }
        if (currentStage < 2 && targetDoseGrams >= 10.0D) {
            alcoholRecovery.applyPsilocybinTherapy(targetDoseGrams, currentStage);
            currentStage = 2;
        }
        return currentStage;
    }

    private int applyLsdAlcoholReset(double targetDoseMicrograms, int currentStage) {
        if (currentStage < 1 && targetDoseMicrograms >= 250.0D) {
            alcoholRecovery.applyLsdTherapy(targetDoseMicrograms, currentStage);
            currentStage = 1;
        }
        if (currentStage < 2 && targetDoseMicrograms >= 500.0D) {
            alcoholRecovery.applyLsdTherapy(targetDoseMicrograms, currentStage);
            currentStage = 2;
        }
        return currentStage;
    }

    private int applyDmtAlcoholReset(double targetDoseUnits, int currentStage) {
        if (currentStage < 1 && targetDoseUnits >= 2.5D) {
            alcoholRecovery.applyDmtTherapy(targetDoseUnits, currentStage);
            currentStage = 1;
        }
        if (currentStage < 2 && targetDoseUnits >= 5.0D) {
            alcoholRecovery.applyDmtTherapy(targetDoseUnits, currentStage);
            currentStage = 2;
        }
        return currentStage;
    }

    private void queuePsilocybinAlcoholReset(double targetDoseGrams) {
        pendingPsilocybinAlcoholResetDoseGrams = Math.max(pendingPsilocybinAlcoholResetDoseGrams, targetDoseGrams);
        applyDeferredAlcoholTherapy();
    }

    private void queueLsdAlcoholReset(double targetDoseMicrograms) {
        pendingLsdAlcoholResetDoseMicrograms = Math.max(pendingLsdAlcoholResetDoseMicrograms, targetDoseMicrograms);
        applyDeferredAlcoholTherapy();
    }

    private void queueDmtAlcoholReset(double targetDoseUnits) {
        pendingDmtAlcoholResetDoseUnits = Math.max(pendingDmtAlcoholResetDoseUnits, targetDoseUnits);
        applyDeferredAlcoholTherapy();
    }

    private void applyDeferredAlcoholTherapy() {
        if (alcoholSession != null && alcoholSession.isAlive()) {
            return;
        }
        if (pendingPsilocybinAlcoholResetDoseGrams > 0.0D) {
            psilocybinAlcoholResetStage = applyPsilocybinAlcoholReset(pendingPsilocybinAlcoholResetDoseGrams, psilocybinAlcoholResetStage);
            pendingPsilocybinAlcoholResetDoseGrams = 0.0D;
        }
        if (pendingLsdAlcoholResetDoseMicrograms > 0.0D) {
            lsdAlcoholResetStage = applyLsdAlcoholReset(pendingLsdAlcoholResetDoseMicrograms, lsdAlcoholResetStage);
            pendingLsdAlcoholResetDoseMicrograms = 0.0D;
        }
        if (pendingDmtAlcoholResetDoseUnits > 0.0D) {
            dmtAlcoholResetStage = applyDmtAlcoholReset(pendingDmtAlcoholResetDoseUnits, dmtAlcoholResetStage);
            pendingDmtAlcoholResetDoseUnits = 0.0D;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ticksActive", ticksActive);
        tag.putDouble("mixedRisk", mixedRisk);
        tag.putDouble("strain", strain);
        tag.putInt("psychedelicLuckTicks", psychedelicLuckTicks);
        tag.putDouble("psychedelicLuckStrength", psychedelicLuckStrength);
        tag.putInt("dmtResistanceTicks", dmtResistanceTicks);
        tag.putDouble("dmtResistanceStrength", dmtResistanceStrength);
        tag.putBoolean("psilocybinAfterglowRewardApplied", psilocybinAfterglowRewardApplied);
        tag.putBoolean("lsdAfterglowRewardApplied", lsdAfterglowRewardApplied);
        tag.putBoolean("dmtAfterglowRewardApplied", dmtAfterglowRewardApplied);
        tag.putInt("psilocybinAlcoholResetStage", psilocybinAlcoholResetStage);
        tag.putInt("lsdAlcoholResetStage", lsdAlcoholResetStage);
        tag.putInt("dmtAlcoholResetStage", dmtAlcoholResetStage);
        tag.putDouble("pendingPsilocybinAlcoholResetDoseGrams", pendingPsilocybinAlcoholResetDoseGrams);
        tag.putDouble("pendingLsdAlcoholResetDoseMicrograms", pendingLsdAlcoholResetDoseMicrograms);
        tag.putDouble("pendingDmtAlcoholResetDoseUnits", pendingDmtAlcoholResetDoseUnits);
        tag.put("alcoholRecovery", alcoholRecovery.save());
        tag.put("heroinRecovery", heroinRecovery.save());
        tag.put("mdmaRecovery", mdmaRecovery.save());
        tag.put("channels", saveChannels());
        tag.putDouble("vanillaNauseaVisualDrift", vanillaNauseaVisualDrift);
        tag.putDouble("vanillaNauseaGeometryMelt", vanillaNauseaGeometryMelt);
        tag.putBoolean("vanillaNauseaExcludesLsd", true);
        tag.put("families", saveFamilies(families));
        tag.put("tolerance", saveFamilies(tolerance));
        tag.put("afterglow", saveFamilies(afterglow));
        tag.put("comedown", saveFamilies(comedown));
        tag.put("craving", saveFamilies(craving));
        tag.put("withdrawal", saveFamilies(withdrawal));
        if (psilocybinSession != null && psilocybinSession.isAlive()) {
            tag.put("psilocybin", psilocybinSession.save());
        }
        if (lsdSession != null && lsdSession.isAlive()) {
            tag.put("lsd", lsdSession.save());
        }
        if (dmtSession != null && dmtSession.isAlive()) {
            tag.put("dmt", dmtSession.save());
        }
        if (cannabisSession != null && cannabisSession.isAlive()) {
            tag.put("cannabis", cannabisSession.save());
        }
        if (mdmaSession != null && mdmaSession.isAlive()) {
            tag.put("mdma", mdmaSession.save());
        }
        if (twoCbSession != null && twoCbSession.isAlive()) {
            tag.put("twoCb", twoCbSession.save());
        }
        if (mescalineSession != null && mescalineSession.isAlive()) {
            tag.put("mescaline", mescalineSession.save());
        }
        if (hoMetSession != null && hoMetSession.isAlive()) {
            tag.put("four_ho_met", hoMetSession.save());
        }
        if (alLadSession != null && alLadSession.isAlive()) {
            tag.put("al_lad", alLadSession.save());
        }
        if (twoCeSession != null && twoCeSession.isAlive()) {
            tag.put("two_ce", twoCeSession.save());
        }
        if (nitrousSession != null && nitrousSession.isAlive()) {
            tag.put("nitrous", nitrousSession.save());
        }
        if (ketamineSession != null && ketamineSession.isAlive()) {
            tag.put("ketamine", ketamineSession.save());
        }
        if (cocaineSession != null && cocaineSession.isAlive()) {
            tag.put("cocaine", cocaineSession.save());
        }
        if (heroinSession != null && heroinSession.isAlive()) {
            tag.put("heroin", heroinSession.save());
        }
        if (salviaSession != null && salviaSession.isAlive()) {
            tag.put("salvia", salviaSession.save());
        }
        if (alcoholSession != null && alcoholSession.isAlive()) {
            tag.put("alcohol", alcoholSession.save());
        }
        ListTag exposureList = new ListTag();
        for (SubstanceExposure exposure : exposures) {
            exposureList.add(exposure.save());
        }
        tag.put("exposures", exposureList);
        return tag;
    }

    public static PsychoactiveExperience load(CompoundTag tag) {
        PsychoactiveExperience experience = new PsychoactiveExperience();
        experience.ticksActive = NbtCompat.getIntOr(tag, "ticksActive", 0);
        experience.mixedRisk = NbtCompat.getDoubleOr(tag, "mixedRisk", 0.0D);
        experience.strain = NbtCompat.getDoubleOr(tag, "strain", 0.0D);
        experience.psychedelicLuckTicks = NbtCompat.getIntOr(tag, "psychedelicLuckTicks", 0);
        experience.psychedelicLuckStrength = NbtCompat.getDoubleOr(tag, "psychedelicLuckStrength", 0.0D);
        experience.dmtResistanceTicks = NbtCompat.getIntOr(tag, "dmtResistanceTicks", 0);
        experience.dmtResistanceStrength = NbtCompat.getDoubleOr(tag, "dmtResistanceStrength", 0.0D);
        experience.psilocybinAfterglowRewardApplied = NbtCompat.getBooleanOr(tag, "psilocybinAfterglowRewardApplied", false);
        experience.lsdAfterglowRewardApplied = NbtCompat.getBooleanOr(tag, "lsdAfterglowRewardApplied", false);
        experience.dmtAfterglowRewardApplied = NbtCompat.getBooleanOr(tag, "dmtAfterglowRewardApplied", false);
        experience.psilocybinAlcoholResetStage = NbtCompat.getIntOr(tag, "psilocybinAlcoholResetStage", 0);
        experience.lsdAlcoholResetStage = NbtCompat.getIntOr(tag, "lsdAlcoholResetStage", 0);
        experience.dmtAlcoholResetStage = NbtCompat.getIntOr(tag, "dmtAlcoholResetStage", 0);
        experience.pendingPsilocybinAlcoholResetDoseGrams = NbtCompat.getDoubleOr(tag, "pendingPsilocybinAlcoholResetDoseGrams", 0.0D);
        experience.pendingLsdAlcoholResetDoseMicrograms = NbtCompat.getDoubleOr(tag, "pendingLsdAlcoholResetDoseMicrograms", 0.0D);
        experience.pendingDmtAlcoholResetDoseUnits = NbtCompat.getDoubleOr(tag, "pendingDmtAlcoholResetDoseUnits", 0.0D);
        experience.alcoholRecovery = AlcoholRecoveryTracker.load(NbtCompat.getCompoundOrEmpty(tag, "alcoholRecovery"));
        experience.heroinRecovery = HeroinRecoveryTracker.load(NbtCompat.getCompoundOrEmpty(tag, "heroinRecovery"));
        experience.mdmaRecovery = MdmaRecoveryTracker.load(NbtCompat.getCompoundOrEmpty(tag, "mdmaRecovery"));

        CompoundTag channelTag = NbtCompat.getCompoundOrEmpty(tag, "channels");
        experience.vanillaNauseaVisualDrift = NbtCompat.getDoubleOr(tag, "vanillaNauseaVisualDrift", 0.0D);
        experience.vanillaNauseaGeometryMelt = NbtCompat.getDoubleOr(tag, "vanillaNauseaGeometryMelt", 0.0D);
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            experience.channels.put(channel, NbtCompat.getDoubleOr(channelTag, channel.name(), 0.0D));
        }

        loadFamilies(NbtCompat.getCompoundOrEmpty(tag, "families"), experience.families);
        loadFamilies(NbtCompat.getCompoundOrEmpty(tag, "tolerance"), experience.tolerance);
        loadFamilies(NbtCompat.getCompoundOrEmpty(tag, "afterglow"), experience.afterglow);
        loadFamilies(NbtCompat.getCompoundOrEmpty(tag, "comedown"), experience.comedown);
        loadFamilies(NbtCompat.getCompoundOrEmpty(tag, "craving"), experience.craving);
        loadFamilies(NbtCompat.getCompoundOrEmpty(tag, "withdrawal"), experience.withdrawal);
        if (tag.contains("psilocybin")) {
            experience.psilocybinSession = PsilocybinSession.load(NbtCompat.getCompoundOrEmpty(tag, "psilocybin"));
        }
        if (tag.contains("lsd")) {
            experience.lsdSession = LsdSession.load(NbtCompat.getCompoundOrEmpty(tag, "lsd"));
        }
        if (tag.contains("dmt")) {
            experience.dmtSession = DmtSession.load(NbtCompat.getCompoundOrEmpty(tag, "dmt"));
        }
        if (tag.contains("cannabis")) {
            experience.cannabisSession = CannabisSession.load(NbtCompat.getCompoundOrEmpty(tag, "cannabis"));
        }
        if (tag.contains("mdma")) {
            experience.mdmaSession = MdmaSession.load(NbtCompat.getCompoundOrEmpty(tag, "mdma"));
        }
        if (tag.contains("twoCb")) {
            experience.twoCbSession = TwoCbSession.load(NbtCompat.getCompoundOrEmpty(tag, "twoCb"));
        }
        if (tag.contains("four_ho_met")) {
            experience.hoMetSession = HoMetSession.load(NbtCompat.getCompoundOrEmpty(tag, "four_ho_met"));
        }
        if (tag.contains("al_lad")) {
            experience.alLadSession = AlLadSession.load(NbtCompat.getCompoundOrEmpty(tag, "al_lad"));
        }
        if (tag.contains("two_ce")) {
            experience.twoCeSession = TwoCeSession.load(NbtCompat.getCompoundOrEmpty(tag, "two_ce"));
        }
        if (tag.contains("nitrous")) {
            experience.nitrousSession = NitrousSession.load(NbtCompat.getCompoundOrEmpty(tag, "nitrous"));
        }
        if (tag.contains("mescaline")) {
            experience.mescalineSession = MescalineSession.load(NbtCompat.getCompoundOrEmpty(tag, "mescaline"));
        }
        if (tag.contains("ketamine")) {
            experience.ketamineSession = KetamineSession.load(NbtCompat.getCompoundOrEmpty(tag, "ketamine"));
        }
        if (tag.contains("cocaine")) {
            experience.cocaineSession = CocaineSession.load(NbtCompat.getCompoundOrEmpty(tag, "cocaine"));
        }
        if (tag.contains("heroin")) {
            experience.heroinSession = HeroinSession.load(NbtCompat.getCompoundOrEmpty(tag, "heroin"));
        }
        if (tag.contains("salvia")) {
            experience.salviaSession = SalviaSession.load(NbtCompat.getCompoundOrEmpty(tag, "salvia"));
        }
        if (tag.contains("alcohol")) {
            experience.alcoholSession = AlcoholSession.load(NbtCompat.getCompoundOrEmpty(tag, "alcohol"));
        }
        ListTag exposureList = NbtCompat.getListOrEmpty(tag, "exposures");
        for (Tag entry : exposureList) {
            if (entry instanceof CompoundTag exposureTag) {
                SubstanceExposure.load(exposureTag).ifPresent(exposure -> {
                    if (isCocaineProfile(exposure.profile())) {
                        experience.addCocaineDose(resolveCocaineRoute(exposure.profile()), exposure.abstractUnits());
                    } else if (isKetamineProfile(exposure.profile())) {
                        experience.addKetamineDose(exposure.abstractUnits() * 50.0D);
                    } else if (isHeroinProfile(exposure.profile())) {
                        experience.addHeroinDose(null, exposure.abstractUnits() * 100.0D);
                    } else if (isMdmaProfile(exposure.profile())) {
                        experience.addMdmaDose(exposure.abstractUnits() * 100.0D);
                    } else if (exposure.profile() == SubstanceProfiles.HO_MET || "four_ho_met".equals(exposure.profile().id())) {
                        experience.addHoMetDose(null, exposure.abstractUnits() * 20.0D);
                    } else if (exposure.profile() == SubstanceProfiles.AL_LAD || "al_lad".equals(exposure.profile().id())) {
                        experience.addAlLadDose(null, exposure.abstractUnits() * 150.0D);
                    } else if (exposure.profile() == SubstanceProfiles.TWO_CE || "two_ce".equals(exposure.profile().id())) {
                        experience.addTwoCeDose(null, exposure.abstractUnits() * 40.0D);
                    } else if (exposure.profile() == SubstanceProfiles.NITROUS || "nitrous".equals(exposure.profile().id())) {
                        experience.addNitrousDose(null, Math.max(1.0D, exposure.abstractUnits()));
                    } else if (isTwoCbProfile(exposure.profile())) {
                        experience.addTwoCbDose(exposure.abstractUnits() * 100.0D);
                    } else {
                        experience.exposures.add(exposure);
                    }
                });
            }
        }
        // Old saves have only the combined visual channels. Reuse those for
        // other substances; a mushroom or LSD save must rebuild the separate bridge
        // signal instead of treating its stored surface visuals as Nausea.
        boolean savedExcludedPsychedelic = experience.psilocybinSession != null || experience.lsdSession != null
                || experience.exposures.stream().anyMatch(exposure -> "psilocybin".equals(exposure.profile().id())
                || "lsd".equals(exposure.profile().id()));
        if (!savedExcludedPsychedelic) {
            if (!tag.contains("vanillaNauseaVisualDrift")) {
                experience.vanillaNauseaVisualDrift = experience.channel(ExperienceChannel.VISUAL_DRIFT);
            }
            if (!tag.contains("vanillaNauseaGeometryMelt")) {
                experience.vanillaNauseaGeometryMelt = experience.channel(ExperienceChannel.GEOMETRY_MELT);
            }
        }
        // The previous bridge saved LSD's visual contribution as vanilla nausea.
        // Discard that stale value for pure LSD/mushroom saves while preserving
        // stored sources from other substances and independently applied debuffs.
        boolean savedLsd = experience.lsdSession != null
                || experience.exposures.stream().anyMatch(exposure -> "lsd".equals(exposure.profile().id()));
        boolean hasOtherVisualSource = experience.exposures.stream().anyMatch(exposure ->
                !"psilocybin".equals(exposure.profile().id()) && !"lsd".equals(exposure.profile().id()))
                || experience.dmtSession != null || experience.hoMetSession != null || experience.alLadSession != null
                || experience.twoCeSession != null || experience.twoCbSession != null || experience.alcoholSession != null
                || experience.ketamineSession != null || experience.salviaSession != null || experience.mescalineSession != null
                || experience.mdmaSession != null || experience.nitrousSession != null || experience.cocaineSession != null;
        if (savedLsd && !hasOtherVisualSource && !NbtCompat.getBooleanOr(tag, "vanillaNauseaExcludesLsd", false)) {
            experience.vanillaNauseaVisualDrift = 0.0D;
            experience.vanillaNauseaGeometryMelt = 0.0D;
        }
        return experience;
    }

    public boolean isAlive() {
        return !exposures.isEmpty()
                || channels.values().stream().anyMatch(value -> value > 0.01D)
                || families.values().stream().anyMatch(value -> value > 0.01D)
                || tolerance.values().stream().anyMatch(value -> value > 0.025D)
                || afterglow.values().stream().anyMatch(value -> value > 0.015D)
                || comedown.values().stream().anyMatch(value -> value > 0.015D)
                || craving.values().stream().anyMatch(value -> value > 0.015D)
                || withdrawal.values().stream().anyMatch(value -> value > 0.015D)
                || (psilocybinSession != null && psilocybinSession.isAlive())
                || (lsdSession != null && lsdSession.isAlive())
                || (dmtSession != null && dmtSession.isAlive())
                || (cannabisSession != null && cannabisSession.isAlive())
                || (mdmaSession != null && mdmaSession.isAlive())
                || (twoCbSession != null && twoCbSession.isAlive())
                || (mescalineSession != null && mescalineSession.isAlive())
                || (hoMetSession != null && hoMetSession.isAlive())
                || (alLadSession != null && alLadSession.isAlive())
                || (twoCeSession != null && twoCeSession.isAlive())
                || (nitrousSession != null && nitrousSession.isAlive())
                || (ketamineSession != null && ketamineSession.isAlive())
                || (cocaineSession != null && cocaineSession.isAlive())
                || (heroinSession != null && heroinSession.isAlive())
                || (salviaSession != null && salviaSession.isAlive())
                || (alcoholSession != null && alcoholSession.isAlive())
                || psychedelicLuckTicks > 0
                || dmtResistanceTicks > 0
                || heroinRecovery.isActive()
                || alcoholRecovery.isActive()
                || mdmaRecovery.isActive()
                || mixedRisk > 0.015D
                || strain > 0.015D;
    }

    public boolean hasClientVisualState() {
        return !exposures.isEmpty()
                || channels.values().stream().anyMatch(value -> value > CLIENT_VISUAL_THRESHOLD)
                || families.values().stream().anyMatch(value -> value > CLIENT_VISUAL_THRESHOLD)
                || tolerance.values().stream().anyMatch(value -> value > CLIENT_VISUAL_THRESHOLD)
                || afterglow.values().stream().anyMatch(value -> value > CLIENT_VISUAL_THRESHOLD)
                || comedown.values().stream().anyMatch(value -> value > CLIENT_VISUAL_THRESHOLD)
                || craving.values().stream().anyMatch(value -> value > CLIENT_VISUAL_THRESHOLD)
                || withdrawal.values().stream().anyMatch(value -> value > CLIENT_VISUAL_THRESHOLD)
                || (psilocybinSession != null && psilocybinSession.isAlive())
                || (lsdSession != null && lsdSession.isAlive())
                || (dmtSession != null && dmtSession.isAlive())
                || (cannabisSession != null && cannabisSession.isAlive())
                || (mdmaSession != null && mdmaSession.isAlive())
                || (twoCbSession != null && twoCbSession.isAlive())
                || (mescalineSession != null && mescalineSession.isAlive())
                || (hoMetSession != null && hoMetSession.isAlive())
                || (alLadSession != null && alLadSession.isAlive())
                || (twoCeSession != null && twoCeSession.isAlive())
                || (nitrousSession != null && nitrousSession.isAlive())
                || (ketamineSession != null && ketamineSession.isAlive())
                || (cocaineSession != null && cocaineSession.hasActiveEffects())
                || (heroinSession != null && heroinSession.isAlive())
                || (salviaSession != null && salviaSession.isAlive())
                || (alcoholSession != null && alcoholSession.isAlive())
                || heroinRecovery.isActive()
                || alcoholRecovery.isActive()
                || mdmaRecovery.isActive()
                || mixedRisk > CLIENT_VISUAL_THRESHOLD
                || strain > CLIENT_VISUAL_THRESHOLD;
    }

    public int ticksActive() {
        return ticksActive;
    }

    public int exposureCount() {
        return exposures.size();
    }

    private CompoundTag saveChannels() {
        CompoundTag tag = new CompoundTag();
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            tag.putDouble(channel.name(), channels.getOrDefault(channel, 0.0D));
        }
        return tag;
    }

    private static CompoundTag saveFamilies(EnumMap<SubstanceFamily, Double> values) {
        CompoundTag tag = new CompoundTag();
        for (SubstanceFamily family : SubstanceFamily.values()) {
            tag.putDouble(family.name(), values.getOrDefault(family, 0.0D));
        }
        return tag;
    }

    private static void loadFamilies(CompoundTag tag, EnumMap<SubstanceFamily, Double> target) {
        for (SubstanceFamily family : SubstanceFamily.values()) {
            target.put(family, NbtCompat.getDoubleOr(tag, family.name(), 0.0D));
        }
    }

    private static <T extends Enum<T>> void clearMap(EnumMap<T, Double> values) {
        for (T key : values.keySet()) {
            values.put(key, 0.0D);
        }
    }

    private static void appendFamilyMetric(StringBuilder builder, String label, EnumMap<SubstanceFamily, Double> values) {
        for (SubstanceFamily family : SubstanceFamily.values()) {
            double value = values.getOrDefault(family, 0.0D);
            if (value >= 0.05D) {
                builder.append(", ")
                        .append(label)
                        .append("_")
                        .append(family.name().toLowerCase(Locale.ROOT))
                        .append("=")
                        .append(fmt(value));
            }
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static boolean isCocaineProfile(SubstanceProfile profile) {
        return profile == SubstanceProfiles.COCAINE
                || profile == SubstanceProfiles.COCAINE_POWDER
                || profile == SubstanceProfiles.CRACK_COCAINE;
    }

    private static boolean isKetamineProfile(SubstanceProfile profile) {
        return profile == SubstanceProfiles.KETAMINE;
    }

    private static boolean isHeroinProfile(SubstanceProfile profile) {
        return profile == SubstanceProfiles.HEROIN;
    }

    private static boolean isMdmaProfile(SubstanceProfile profile) {
        return profile == SubstanceProfiles.MDMA;
    }

    private static boolean isTwoCbProfile(SubstanceProfile profile) {
        return profile == SubstanceProfiles.TWO_CB;
    }

    private static CocaineRoute resolveCocaineRoute(SubstanceProfile profile) {
        return profile == SubstanceProfiles.CRACK_COCAINE ? CocaineRoute.CRACK : CocaineRoute.POWDER;
    }
}
