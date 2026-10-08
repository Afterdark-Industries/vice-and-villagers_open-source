package com.trippycraft.substance;

import com.trippycraft.compat.NbtCompat;

import com.trippycraft.config.TrippyConfig;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Optional;

public final class SubstanceExposure {
    private final SubstanceProfile profile;
    private final double abstractUnits;
    private final SetSettingSnapshot initialSetting;
    private int ageTicks;

    public SubstanceExposure(SubstanceProfile profile, double abstractUnits, SetSettingSnapshot initialSetting) {
        this(profile, abstractUnits, initialSetting, 0);
    }

    private SubstanceExposure(SubstanceProfile profile, double abstractUnits, SetSettingSnapshot initialSetting, int ageTicks) {
        this.profile = profile;
        this.abstractUnits = abstractUnits;
        this.initialSetting = initialSetting;
        this.ageTicks = Math.max(0, ageTicks);
    }

    public void contribute(SetSettingSnapshot currentSetting, EnumMap<ExperienceChannel, Double> target, EnumMap<SubstanceFamily, Double> families, double responseMultiplier) {
        if (!profile.hasGameplayEffects()) {
            return;
        }
        double base = contributionBase(currentSetting) * Math.max(0.0D, responseMultiplier);
        if (base > 0.0D) {
            families.merge(profile.family(), base, Double::sum);
        }
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            double weighted = base * profile.channelWeight(channel);
            if (weighted > 0.0D) {
                target.merge(channel, weighted, Double::sum);
            }
        }

        double phase = profile.curve().phaseIntensity(ageTicks, TrippyConfig.experienceTimeScale);
        double anxietyBias = currentSetting.anxietyBias(profile) * phase;
        if (anxietyBias > 0.0D) {
            target.merge(ExperienceChannel.ANXIETY, anxietyBias * profile.settingSensitivity() * 0.22D, Double::sum);
        }
        if (TrippyConfig.enableRiskMechanics && profile.riskPressure() > 0.0D) {
            target.merge(ExperienceChannel.RISK, base * profile.riskPressure(), Double::sum);
        }
    }

    private double contributionBase(SetSettingSnapshot currentSetting) {
        if (!profile.hasGameplayEffects()) {
            return 0.0D;
        }
        double dose = profile.curve().normalizedDose(abstractUnits);
        double phase = profile.curve().phaseIntensity(ageTicks, TrippyConfig.experienceTimeScale);
        double setting = (initialSetting.intensityMultiplier(profile) * 0.35D) + (currentSetting.intensityMultiplier(profile) * 0.65D);
        return dose * phase * setting * TrippyConfig.globalIntensity;
    }

    public double rawContribution(SetSettingSnapshot currentSetting) {
        return contributionBase(currentSetting);
    }

    public void tick() {
        ageTicks++;
    }

    public boolean isExpired() {
        return profile.curve().isExpired(ageTicks, TrippyConfig.experienceTimeScale);
    }

    public SubstanceProfile profile() {
        return profile;
    }

    public double abstractUnits() {
        return abstractUnits;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("profile", profile.id());
        tag.putDouble("units", abstractUnits);
        tag.putInt("ageTicks", ageTicks);
        tag.put("initialSetting", initialSetting.save());
        return tag;
    }

    public static Optional<SubstanceExposure> load(CompoundTag tag) {
        String profileId = NbtCompat.getStringOr(tag, "profile", "");
        double units = NbtCompat.getDoubleOr(tag, "units", 0.0D);
        int age = NbtCompat.getIntOr(tag, "ageTicks", 0);
        SetSettingSnapshot setting = SetSettingSnapshot.load(NbtCompat.getCompoundOrEmpty(tag, "initialSetting"));
        return SubstanceProfiles.byId(profileId)
                .map(profile -> new SubstanceExposure(profile, units, setting, age))
                .filter(exposure -> exposure.profile().hasGameplayEffects())
                .filter(exposure -> !exposure.isExpired());
    }
}
