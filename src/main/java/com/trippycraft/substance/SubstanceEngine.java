package com.trippycraft.substance;

import com.trippycraft.guide.CursedFindingIds;
import com.trippycraft.substance.allad.AlLadDoseCurve;
import com.trippycraft.substance.allad.AlLadSession;
import com.trippycraft.substance.twoce.TwoCeDoseCurve;
import com.trippycraft.substance.twoce.TwoCeSession;
import com.trippycraft.guide.CursedRecipeCodex;
import com.trippycraft.network.ModNetwork;
import com.trippycraft.substance.cocaine.CocaineDamageTypes;
import com.trippycraft.substance.cocaine.CocaineRoute;
import com.trippycraft.substance.heroin.HeroinDamageTypes;
import com.trippycraft.substance.homet.HoMetDamageTypes;
import com.trippycraft.substance.homet.HoMetDoseCurve;
import com.trippycraft.substance.homet.HoMetSession;
import com.trippycraft.substance.mdma.MdmaDamageTypes;
import com.trippycraft.substance.salvia.SalviaDoseCurve;
import com.trippycraft.substance.twocb.TwoCbDamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SubstanceEngine {
    private static final Map<UUID, PsychoactiveExperience> EXPERIENCES = new ConcurrentHashMap<>();
    private static final Set<UUID> PENDING_VANILLA_CLEAR = ConcurrentHashMap.newKeySet();

    private SubstanceEngine() {
    }

    public static void addExposure(ServerPlayer player, SubstanceProfile profile, double abstractUnits) {
        if (profile == SubstanceProfiles.HO_MET || "four_ho_met".equals(profile.id())) {
            addHoMetDose(player, abstractUnits * 20.0D);
            return;
        }
        if (profile == SubstanceProfiles.AL_LAD || "al_lad".equals(profile.id())) {
            addAlLadDose(player, abstractUnits * 150.0D);
            return;
        }
        if (profile == SubstanceProfiles.TWO_CE || "two_ce".equals(profile.id())) {
            addTwoCeDose(player, abstractUnits * 40.0D);
            return;
        }
        if (profile == SubstanceProfiles.NITROUS || "nitrous".equals(profile.id())) {
            addNitrousDose(player, Math.max(1.0D, abstractUnits));
            return;
        }
        if (profile == SubstanceProfiles.MESCALINE || "mescaline".equals(profile.id())) {
            addMescalineDose(player, abstractUnits * 100.0D);
            return;
        }
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addExposure(profile, abstractUnits, SetSettingSnapshot.capture(player));
        player.displayClientMessage(Component.translatable("message.trippycraft.exposure", Component.translatable(profile.translationKey())), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addPsilocybinDose(ServerPlayer player, double doseGrams) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addPsilocybinDose(player, doseGrams);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.PSILOCYBIN);
        player.displayClientMessage(Component.translatable("message.trippycraft.psilocybin", String.format(java.util.Locale.ROOT, "%.1fg", doseGrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addLsdDose(ServerPlayer player, double doseMicrograms) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addLsdDose(player, doseMicrograms);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.LSD);
        player.displayClientMessage(Component.translatable("message.trippycraft.lsd", String.format(java.util.Locale.ROOT, "%.0fug", doseMicrograms)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addDmtDose(ServerPlayer player, double doseUnits) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addDmtDose(player, doseUnits);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.DMT);
        player.displayClientMessage(Component.translatable("message.trippycraft.dmt", String.format(java.util.Locale.ROOT, "%.1fx", doseUnits)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addCannabisDose(ServerPlayer player, double thcMilligrams) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addCannabisDose(player, thcMilligrams);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.CANNABIS);
        player.displayClientMessage(Component.translatable("message.trippycraft.cannabis", String.format(java.util.Locale.ROOT, "%.0fmg", thcMilligrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addKetamineDose(ServerPlayer player, double doseMilligrams) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addKetamineDose(doseMilligrams);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.KETAMINE);
        player.displayClientMessage(Component.translatable("message.trippycraft.ketamine", String.format(java.util.Locale.ROOT, "%.0fmg", doseMilligrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addMdmaDose(ServerPlayer player, double doseMilligrams) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addMdmaDose(doseMilligrams, player.level().getGameTime());
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.MDMA);
        double heartAttackChance = mdmaHeartAttackChance(
                experience.mdmaSystemDoseMilligrams(), experience.mdmaToleranceProgress());
        if (heartAttackChance > 0.0D && player.getRandom().nextDouble() < heartAttackChance) {
            clear(player.getUUID());
            ModNetwork.sendClear(player);
            player.hurt(MdmaDamageTypes.heartAttack(player), Float.MAX_VALUE);
            return;
        }
        player.displayClientMessage(Component.translatable("message.trippycraft.mdma", String.format(java.util.Locale.ROOT, "%.0fmg", doseMilligrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addTwoCbDose(ServerPlayer player, double doseMilligrams) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addTwoCbDose(doseMilligrams);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.TWO_CB);
        double overdoseChance = twoCbOverdoseChance(experience.twoCbSystemDoseMilligrams());
        if (overdoseChance > 0.0D && player.getRandom().nextDouble() < overdoseChance) {
            clear(player.getUUID());
            ModNetwork.sendClear(player);
            player.hurt(TwoCbDamageTypes.overdose(player), Float.MAX_VALUE);
            return;
        }
        player.displayClientMessage(Component.translatable("message.trippycraft.two_cb", String.format(java.util.Locale.ROOT, "%.0fmg", doseMilligrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addHoMetDose(ServerPlayer player, double doseMilligrams) {
        addHoMetDose(player, doseMilligrams, HoMetSession.contentMilligramsForEffect(doseMilligrams));
    }

    public static void addHoMetDose(ServerPlayer player, double doseMilligrams, double contentMilligrams) {
        double dose = HoMetDoseCurve.sanitizeDose(doseMilligrams);
        if (dose <= 0.0D) return;
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addHoMetDose(player, dose, contentMilligrams);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.HO_MET);
        double consumedTotal = experience.hoMetState().targetDoseMilligrams();
        if (HoMetDoseCurve.overdoseChanceForDose(dose, consumedTotal) > 0.0D
                && HoMetDoseCurve.overdoseRollIsFatal(dose, consumedTotal, player.getRandom().nextDouble())) {
            clear(player.getUUID());
            ModNetwork.sendClear(player);
            player.hurt(HoMetDamageTypes.overdose(player), Float.MAX_VALUE);
            return;
        }
        player.displayClientMessage(Component.translatable("message.trippycraft.four_ho_met",
                String.format(java.util.Locale.ROOT, "%.0fmg", contentMilligrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addTwoCeDose(ServerPlayer player, double doseMilligrams) {
        addTwoCeDose(player, doseMilligrams, TwoCeSession.contentMilligramsForEffect(doseMilligrams));
    }

    public static void addTwoCeDose(ServerPlayer player, double doseMilligrams, double contentMilligrams) {
        double dose = TwoCeDoseCurve.sanitizeDose(doseMilligrams);
        if (dose <= 0.0D) {
            return;
        }
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        double content = Double.isFinite(contentMilligrams) && contentMilligrams > 0.0D ? contentMilligrams : 0.0D;
        experience.addTwoCeDose(player, dose, content);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.TWO_CE);
        player.displayClientMessage(Component.translatable("message.trippycraft.two_ce",
                String.format(java.util.Locale.ROOT, "%.0fmg", content)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addNitrousDose(ServerPlayer player, double hits) {
        double dose = com.trippycraft.substance.nitrous.NitrousDoseCurve.sanitizeHits(hits);
        if (dose <= 0.0D) {
            return;
        }
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addNitrousDose(player, dose);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.NITROUS);
        player.displayClientMessage(Component.translatable("message.trippycraft.nitrous"), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addAlLadDose(ServerPlayer player, double doseMicrograms) {
        addAlLadDose(player, doseMicrograms, -1.0D);
    }

    public static void addAlLadDose(ServerPlayer player, double doseMicrograms, double contentMilligrams) {
        double dose = AlLadDoseCurve.sanitizeDose(doseMicrograms);
        if (dose <= 0.0D) {
            return;
        }
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addAlLadDose(player, dose, contentMilligrams >= 0.0D
                ? contentMilligrams : AlLadSession.contentMilligramsForEffect(dose));
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.AL_LAD);
        player.displayClientMessage(Component.translatable("message.trippycraft.al_lad",
                contentMilligrams >= 0.0D
                        ? String.format(java.util.Locale.ROOT, "%.0fmg", contentMilligrams)
                        : String.format(java.util.Locale.ROOT, "%.0fug", dose)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addMescalineDose(ServerPlayer player, double doseMilligrams) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addMescalineDose(player, doseMilligrams);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.MESCALINE);
        player.displayClientMessage(Component.translatable("message.trippycraft.mescaline",
                String.format(java.util.Locale.ROOT, "%.0fmg", doseMilligrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addKokaDose(ServerPlayer player, double doseGrams) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addCocaineDose(CocaineRoute.POWDER, doseGrams);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.COCAINE);
        double heartAttackChance = cocaineHeartAttackChance(experience.cocaineSystemDoseGrams());
        if (heartAttackChance > 0.0D && player.getRandom().nextDouble() < heartAttackChance) {
            clear(player.getUUID());
            ModNetwork.sendClear(player);
            player.hurt(CocaineDamageTypes.heartAttack(player), Float.MAX_VALUE);
            return;
        }
        player.displayClientMessage(Component.translatable("message.trippycraft.koka", String.format(java.util.Locale.ROOT, "%.2fg", doseGrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addCocaineDose(ServerPlayer player, CocaineRoute route, double doseGrams) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addCocaineDose(route, doseGrams);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.COCAINE);
        double heartAttackChance = cocaineHeartAttackChance(experience.cocaineSystemDoseGrams());
        if (heartAttackChance > 0.0D && player.getRandom().nextDouble() < heartAttackChance) {
            clear(player.getUUID());
            ModNetwork.sendClear(player);
            player.hurt(CocaineDamageTypes.heartAttack(player), Float.MAX_VALUE);
            return;
        }
        player.displayClientMessage(Component.translatable("message.trippycraft.cocaine", String.format(java.util.Locale.ROOT, "%.1fg", doseGrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addHeroinDose(ServerPlayer player, double doseMilligrams) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        double previousDose = experience.heroinSystemDoseMilligrams();
        double tolerance = experience.heroinToleranceProgress();
        int previousChecks = experience.heroinOverdoseChecksUpToMilligrams();
        experience.addHeroinDose(player, doseMilligrams);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.HEROIN);
        double currentDose = experience.heroinSystemDoseMilligrams();
        int checkedThrough = applyHeroinOverdoseRisk(player, previousDose, currentDose, tolerance, previousChecks);
        experience.setHeroinOverdoseChecksUpToMilligrams(checkedThrough);
        if (!player.isAlive()) {
            clear(player.getUUID());
            ModNetwork.sendClear(player);
            return;
        }
        player.displayClientMessage(Component.translatable("message.trippycraft.heroin", String.format(java.util.Locale.ROOT, "%.0fmg", doseMilligrams)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addSalviaDose(ServerPlayer player, double doseUnits) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addSalviaDose(player, doseUnits);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.SALVIA);
        player.displayClientMessage(Component.translatable("message.trippycraft.salvia", SalviaDoseCurve.formatPotencyLabel(doseUnits)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void addAlcoholDose(ServerPlayer player, double standardDrinks) {
        PsychoactiveExperience experience = EXPERIENCES.computeIfAbsent(player.getUUID(), id -> new PsychoactiveExperience());
        experience.addAlcoholDose(standardDrinks);
        CursedRecipeCodex.unlockFinding(player, CursedFindingIds.ALCOHOL);
        player.displayClientMessage(Component.translatable("message.trippycraft.alcohol", String.format(java.util.Locale.ROOT, "%.2f", standardDrinks)), true);
        ModNetwork.sendSnapshot(player, experience);
    }

    public static void tick(ServerPlayer player) {
        PsychoactiveExperience experience = EXPERIENCES.get(player.getUUID());
        if (experience == null) {
            if (player.level().getGameTime() % 100L == 0L) {
                ModNetwork.sendClear(player);
            }
            return;
        }

        experience.tick(player, SetSettingSnapshot.capture(player));
        VanillaEffectBridge.applyTick(player, experience);
        if (player.level().getGameTime() % 5L == 0L) {
            ModNetwork.sendSnapshot(player, experience);
        }

        if (!experience.isAlive() && !experience.hasClientVisualState()) {
            EXPERIENCES.remove(player.getUUID());
            ModNetwork.sendClear(player);
        }
    }

    public static Optional<PsychoactiveExperience> getExperience(UUID playerId) {
        return Optional.ofNullable(EXPERIENCES.get(playerId));
    }

    public static int mdmaSocialDiscount(ServerPlayer player) {
        PsychoactiveExperience experience = EXPERIENCES.get(player.getUUID());
        if (experience == null) {
            return 0;
        }
        var mdma = experience.mdmaState();
        if (!mdma.active() || !mdma.peakWindow()) {
            return 0;
        }
        double discount = Math.max(0.0D, Math.min(0.25D, mdma.socialAura() * 0.16D));
        return (int) Math.round(discount * 100.0D);
    }

    public static void setExperience(UUID playerId, PsychoactiveExperience experience) {
        if (experience.isAlive() || experience.hasClientVisualState()) {
            EXPERIENCES.put(playerId, experience);
        } else {
            EXPERIENCES.remove(playerId);
        }
    }

    public static void clear(UUID playerId) {
        EXPERIENCES.remove(playerId);
        PENDING_VANILLA_CLEAR.remove(playerId);
    }

    public static void clearAll() {
        EXPERIENCES.clear();
        PENDING_VANILLA_CLEAR.clear();
    }

    public static void reset(ServerPlayer player) {
        clear(player.getUUID());
        clearVanillaFeedback(player);
        if (canSyncPlayer(player)) {
            ModNetwork.sendClear(player);
        }
    }

    public static void endTripFromSleep(ServerPlayer player) {
        boolean canSync = canSyncPlayer(player);
        PsychoactiveExperience experience = EXPERIENCES.get(player.getUUID());
        if (experience == null) {
            clearVanillaFeedback(player);
            if (canSync) {
                ModNetwork.sendClear(player);
            }
            return;
        }
        boolean changed = experience.endActiveTripFromSleep();
        clearVanillaFeedback(player);
        if (!experience.isAlive() && !experience.hasClientVisualState()) {
            EXPERIENCES.remove(player.getUUID());
            if (canSync) {
                ModNetwork.sendClear(player);
            }
        } else if (canSync) {
            ModNetwork.sendSnapshot(player, experience);
        }
        if (changed && canSync) {
            player.displayClientMessage(Component.translatable("message.trippycraft.sleep_trip_killer"), true);
        }
    }

    private static void clearVanillaFeedback(ServerPlayer player) {
        if (!canSyncPlayer(player)) {
            PENDING_VANILLA_CLEAR.add(player.getUUID());
            return;
        }
        PENDING_VANILLA_CLEAR.remove(player.getUUID());
        removeVanillaFeedback(player);
    }

    private static void flushPendingVanillaFeedback(ServerPlayer player) {
        if (canSyncPlayer(player) && PENDING_VANILLA_CLEAR.remove(player.getUUID())) {
            removeVanillaFeedback(player);
        }
    }

    private static void removeVanillaFeedback(ServerPlayer player) {
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.REGENERATION);
        player.removeEffect(MobEffects.ABSORPTION);
        player.removeEffect(MobEffects.LUCK);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
    }

    public static void sync(ServerPlayer player) {
        flushPendingVanillaFeedback(player);
        PsychoactiveExperience experience = EXPERIENCES.get(player.getUUID());
        if (experience == null) {
            ModNetwork.sendClear(player);
        } else {
            ModNetwork.sendSnapshot(player, experience);
        }
    }

    private static boolean canSyncPlayer(ServerPlayer player) {
        return player.connection != null;
    }

    public static boolean scrubPsilocybin(ServerPlayer player, int newAgeTicks) {
        PsychoactiveExperience experience = EXPERIENCES.get(player.getUUID());
        if (experience == null) {
            return false;
        }
        boolean changed = experience.scrubPsilocybin(newAgeTicks);
        if (changed) {
            ModNetwork.sendSnapshot(player, experience);
        }
        return changed;
    }

    public static String describe(ServerPlayer player) {
        PsychoactiveExperience experience = EXPERIENCES.get(player.getUUID());
        if (experience == null) {
            return "No active Vice and Villagers experience.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("ticks=").append(experience.ticksActive())
                .append(", exposures=").append(experience.exposureCount());
        builder.append(", ").append(SetSettingSnapshot.capture(player).debugSummary());
        builder.append(", ").append(experience.depthSummary());
        builder.append(", ").append(experience.psilocybinSummary());
        builder.append(", ").append(experience.lsdSummary());
        builder.append(", ").append(experience.dmtSummary());
        builder.append(", ").append(experience.mdmaSummary());
        builder.append(", ").append(experience.twoCbSummary());
        builder.append(", ").append(experience.mescalineSummary());
        builder.append(", ").append(experience.hoMetSummary());
        builder.append(", ").append(experience.alLadSummary());
        builder.append(", ").append(experience.twoCeSummary());
        builder.append(", ").append(experience.nitrousSummary());
        builder.append(", ").append(experience.ketamineSummary());
        builder.append(", ").append(experience.cocaineSummary());
        builder.append(", ").append(experience.heroinSummary());
        builder.append(", ").append(experience.salviaSummary());
        builder.append(", ").append(experience.alcoholSummary());
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            double value = experience.channel(channel);
            if (value >= 0.05D) {
                builder.append(", ")
                        .append(channel.name().toLowerCase())
                        .append("=")
                        .append(String.format("%.2f", value));
            }
        }
        for (SubstanceFamily family : SubstanceFamily.values()) {
            double value = experience.family(family);
            if (value >= 0.05D) {
                builder.append(", family_")
                        .append(family.name().toLowerCase())
                        .append("=")
                        .append(String.format("%.2f", value));
            }
        }
        return builder.toString();
    }

    private static double cocaineHeartAttackChance(double systemDoseGrams) {
        return Math.max(0.0D, Math.min(1.0D, (systemDoseGrams - 10.0D) * 0.10D));
    }

    private static double mdmaHeartAttackChance(double systemDoseMilligrams, double toleranceProgress) {
        double toleranceThresholdMultiplier = 1.0D + Math.max(0.0D, Math.min(1.0D, toleranceProgress));
        double toleranceAdjustedDose = systemDoseMilligrams / toleranceThresholdMultiplier;
        if (toleranceAdjustedDose > 1_000.0D) {
            return 0.50D;
        }
        if (toleranceAdjustedDose > 500.0D) {
            return 0.20D;
        }
        return 0.0D;
    }

    private static double twoCbOverdoseChance(double systemDoseMilligrams) {
        if (systemDoseMilligrams > 3_000.0D) {
            return 0.50D;
        }
        if (systemDoseMilligrams >= 3_000.0D) {
            return 0.30D;
        }
        if (systemDoseMilligrams >= 2_500.0D) {
            return 0.25D;
        }
        if (systemDoseMilligrams >= 2_000.0D) {
            return 0.20D;
        }
        if (systemDoseMilligrams >= 1_500.0D) {
            return 0.10D;
        }
        if (systemDoseMilligrams >= 1_000.0D) {
            return 0.05D;
        }
        return 0.0D;
    }

    private static int applyHeroinOverdoseRisk(ServerPlayer player, double previousDose, double currentDose, double toleranceProgress, int previousChecks) {
        double tolerance = Math.max(0.0D, Math.min(1.0D, toleranceProgress));
        double threshold = 300.0D + tolerance * 260.0D;
        double perCheckChance = Math.max(0.04D, 0.20D * (1.0D - tolerance * 0.70D));
        int checkedThrough = Math.max(previousChecks, (int) Math.floor(Math.max(previousDose, threshold) / 50.0D) * 50);
        int currentStep = (int) Math.floor(currentDose / 50.0D) * 50;
        for (int check = checkedThrough + 50; check <= currentStep; check += 50) {
            if (check <= threshold) {
                checkedThrough = check;
                continue;
            }
            checkedThrough = check;
            if (player.getRandom().nextDouble() < perCheckChance) {
                player.hurt(HeroinDamageTypes.overdose(player), Float.MAX_VALUE);
                return checkedThrough;
            }
        }
        return checkedThrough;
    }
}
