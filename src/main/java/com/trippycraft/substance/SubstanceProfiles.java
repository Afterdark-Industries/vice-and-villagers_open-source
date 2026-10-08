package com.trippycraft.substance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

public final class SubstanceProfiles {
    public static final SubstanceProfile PSILOCYBIN = new SubstanceProfile(
            "psilocybin",
            "substance.trippycraft.psilocybin",
            SubstanceFamily.PSYCHEDELIC,
            new DoseCurve(0.15D, 1.0D, 2.4D, 900, 4200, 12000),
            1.0D,
            0.62D,
            0.20D,
            Map.ofEntries(
                    entry(ExperienceChannel.VISUAL_DRIFT, 0.75D),
                    entry(ExperienceChannel.GEOMETRY_MELT, 0.58D),
                    entry(ExperienceChannel.PATTERNING, 0.74D),
                    entry(ExperienceChannel.COLOR_BLOOM, 0.62D),
                    entry(ExperienceChannel.DEPTH_BREATHING, 0.56D),
                    entry(ExperienceChannel.TIME_DILATION, 0.36D),
                    entry(ExperienceChannel.EGO_DISSOLUTION, 0.42D),
                    entry(ExperienceChannel.BODY_LOAD, 0.28D),
                    entry(ExperienceChannel.ANXIETY, 0.18D),
                    entry(ExperienceChannel.EUPHORIA, 0.26D)
            ));

    public static final SubstanceProfile LSD = new SubstanceProfile(
            "lsd",
            "substance.trippycraft.lsd",
            SubstanceFamily.PSYCHEDELIC,
            new DoseCurve(0.10D, 1.0D, 2.6D, 1200, 6000, 18000),
            1.0D,
            0.72D,
            0.22D,
            Map.ofEntries(
                    entry(ExperienceChannel.VISUAL_DRIFT, 0.82D),
                    entry(ExperienceChannel.GEOMETRY_MELT, 0.70D),
                    entry(ExperienceChannel.PATTERNING, 0.86D),
                    entry(ExperienceChannel.COLOR_BLOOM, 0.66D),
                    entry(ExperienceChannel.DEPTH_BREATHING, 0.48D),
                    entry(ExperienceChannel.TIME_DILATION, 0.52D),
                    entry(ExperienceChannel.EGO_DISSOLUTION, 0.46D),
                    entry(ExperienceChannel.STIMULATION, 0.24D),
                    entry(ExperienceChannel.ANXIETY, 0.24D),
                    entry(ExperienceChannel.EUPHORIA, 0.22D)
            ));

    public static final SubstanceProfile TWO_CB = new SubstanceProfile(
            "two_cb",
            "substance.trippycraft.two_cb",
            SubstanceFamily.PSYCHEDELIC,
            new DoseCurve(0.10D, 1.0D, 2.5D, 600, 3200, 9800),
            1.0D,
            0.68D,
            0.26D,
            Map.ofEntries(
                    entry(ExperienceChannel.PATTERNING, 0.92D),
                    entry(ExperienceChannel.COLOR_BLOOM, 0.82D),
                    entry(ExperienceChannel.VISUAL_DRIFT, 0.24D),
                    entry(ExperienceChannel.GEOMETRY_MELT, 0.08D),
                    entry(ExperienceChannel.DEPTH_BREATHING, 0.28D),
                    entry(ExperienceChannel.STIMULATION, 0.40D),
                    entry(ExperienceChannel.EUPHORIA, 0.42D),
                    entry(ExperienceChannel.TIME_DILATION, 0.18D),
                    entry(ExperienceChannel.ANXIETY, 0.22D),
                    entry(ExperienceChannel.RISK, 0.24D)
            ));

    public static final SubstanceProfile HO_MET = new SubstanceProfile(
            "four_ho_met", "substance.trippycraft.four_ho_met", SubstanceFamily.PSYCHEDELIC,
            new DoseCurve(0.10D, 1.0D, 3.0D, 1800, 5200, 12000), 1.0D, 0.58D, 0.0D,
            Map.ofEntries(
                    entry(ExperienceChannel.DEPTH_BREATHING, 0.90D),
                    entry(ExperienceChannel.GEOMETRY_MELT, 0.74D),
                    entry(ExperienceChannel.VISUAL_DRIFT, 0.70D),
                    entry(ExperienceChannel.PATTERNING, 0.86D),
                    entry(ExperienceChannel.COLOR_BLOOM, 0.74D),
                    entry(ExperienceChannel.EUPHORIA, 0.20D)
            ));

    public static final SubstanceProfile TWO_CE = new SubstanceProfile(
            "two_ce", "substance.trippycraft.two_ce", SubstanceFamily.PSYCHEDELIC,
            new DoseCurve(0.12D, 1.0D, 2.8D, 1600, 5600, 16000), 1.0D, 0.70D, 0.18D,
            Map.ofEntries(
                    entry(ExperienceChannel.PATTERNING, 0.92D),
                    entry(ExperienceChannel.GEOMETRY_MELT, 0.68D),
                    entry(ExperienceChannel.DEPTH_BREATHING, 0.86D),
                    entry(ExperienceChannel.VISUAL_DRIFT, 0.40D),
                    entry(ExperienceChannel.TIME_DILATION, 0.48D),
                    entry(ExperienceChannel.EGO_DISSOLUTION, 0.36D),
                    entry(ExperienceChannel.BODY_LOAD, 0.28D),
                    entry(ExperienceChannel.ANXIETY, 0.22D),
                    entry(ExperienceChannel.COLOR_BLOOM, 0.24D)
            ));

    public static final SubstanceProfile AL_LAD = new SubstanceProfile(
            "al_lad", "substance.trippycraft.al_lad", SubstanceFamily.PSYCHEDELIC,
            new DoseCurve(0.10D, 1.0D, 2.7D, 1400, 4800, 14000), 1.0D, 0.64D, 0.12D,
            Map.ofEntries(
                    entry(ExperienceChannel.PATTERNING, 0.94D),
                    entry(ExperienceChannel.COLOR_BLOOM, 0.88D),
                    entry(ExperienceChannel.DEPTH_BREATHING, 0.72D),
                    entry(ExperienceChannel.GEOMETRY_MELT, 0.42D),
                    entry(ExperienceChannel.VISUAL_DRIFT, 0.36D),
                    entry(ExperienceChannel.TIME_DILATION, 0.40D),
                    entry(ExperienceChannel.EUPHORIA, 0.38D),
                    entry(ExperienceChannel.STIMULATION, 0.22D),
                    entry(ExperienceChannel.BODY_LOAD, 0.16D),
                    entry(ExperienceChannel.ANXIETY, 0.10D)
            ));

    public static final SubstanceProfile DMT = new SubstanceProfile(
            "dmt",
            "substance.trippycraft.dmt",
            SubstanceFamily.PSYCHEDELIC,
            new DoseCurve(0.12D, 1.0D, 2.2D, 80, 360, 1800),
            1.0D,
            0.0D,
            0.0D,
            Map.of());

    public static final SubstanceProfile CANNABIS = new SubstanceProfile(
            "cannabis",
            "substance.trippycraft.cannabis",
            SubstanceFamily.CANNABINOID,
            new DoseCurve(0.12D, 1.0D, 2.5D, 260, 1800, 7200),
            1.0D,
            0.42D,
            0.12D,
            Map.ofEntries(
                    entry(ExperienceChannel.COLOR_BLOOM, 0.25D),
                    entry(ExperienceChannel.TIME_DILATION, 0.38D),
                    entry(ExperienceChannel.BODY_LOAD, 0.32D),
                    entry(ExperienceChannel.MOTOR_NOISE, 0.22D),
                    entry(ExperienceChannel.SEDATION, 0.36D),
                    entry(ExperienceChannel.EUPHORIA, 0.30D),
                    entry(ExperienceChannel.ANXIETY, 0.20D),
                    entry(ExperienceChannel.CRAVING, 0.18D)
            ));

    public static final SubstanceProfile COCAINE_POWDER = new SubstanceProfile(
            "cocaine_powder",
            "substance.trippycraft.cocaine_powder",
            SubstanceFamily.STIMULANT,
            new DoseCurve(0.20D, 3.0D, 5.0D, 120, 1100, 4200),
            1.0D,
            0.35D,
            0.58D,
            Map.ofEntries(
                    entry(ExperienceChannel.STIMULATION, 0.92D),
                    entry(ExperienceChannel.EUPHORIA, 0.44D),
                    entry(ExperienceChannel.TIME_DILATION, 0.20D),
                    entry(ExperienceChannel.MOTOR_NOISE, 0.38D),
                    entry(ExperienceChannel.ANXIETY, 0.38D),
                    entry(ExperienceChannel.CRAVING, 0.70D),
                    entry(ExperienceChannel.RISK, 0.52D)
            ));

    public static final SubstanceProfile CRACK_COCAINE = new SubstanceProfile(
            "crack_cocaine",
            "substance.trippycraft.crack_cocaine",
            SubstanceFamily.STIMULANT,
            new DoseCurve(0.20D, 3.0D, 5.0D, 40, 420, 2200),
            5.0D,
            0.46D,
            0.84D,
            Map.ofEntries(
                    entry(ExperienceChannel.STIMULATION, 1.08D),
                    entry(ExperienceChannel.EUPHORIA, 0.58D),
                    entry(ExperienceChannel.TIME_DILATION, 0.26D),
                    entry(ExperienceChannel.MOTOR_NOISE, 0.52D),
                    entry(ExperienceChannel.ANXIETY, 0.54D),
                    entry(ExperienceChannel.CRAVING, 0.88D),
                    entry(ExperienceChannel.RISK, 0.78D)
            ));

    public static final SubstanceProfile COCAINE = COCAINE_POWDER;

    public static final SubstanceProfile HEROIN = new SubstanceProfile(
            "heroin",
            "substance.trippycraft.heroin",
            SubstanceFamily.OPIOID,
            new DoseCurve(0.18D, 1.0D, 2.2D, 220, 1600, 7600),
            1.0D,
            0.30D,
            0.86D,
            Map.ofEntries(
                    entry(ExperienceChannel.SEDATION, 0.94D),
                    entry(ExperienceChannel.EUPHORIA, 0.46D),
                    entry(ExperienceChannel.BODY_LOAD, 0.58D),
                    entry(ExperienceChannel.MOTOR_NOISE, 0.42D),
                    entry(ExperienceChannel.TIME_DILATION, 0.26D),
                    entry(ExperienceChannel.NAUSEA, 0.30D),
                    entry(ExperienceChannel.CRAVING, 0.74D),
                    entry(ExperienceChannel.RISK, 0.82D)
            ));

    public static final SubstanceProfile MDMA = new SubstanceProfile(
            "mdma",
            "substance.trippycraft.mdma",
            SubstanceFamily.EMPATHOGEN,
            new DoseCurve(0.16D, 1.0D, 2.4D, 600, 3200, 9800),
            1.0D,
            0.50D,
            0.35D,
            Map.ofEntries(
                    entry(ExperienceChannel.EUPHORIA, 0.78D),
                    entry(ExperienceChannel.STIMULATION, 0.46D),
                    entry(ExperienceChannel.COLOR_BLOOM, 0.30D),
                    entry(ExperienceChannel.BODY_LOAD, 0.36D),
                    entry(ExperienceChannel.MOTOR_NOISE, 0.22D),
                    entry(ExperienceChannel.ANXIETY, 0.18D),
                    entry(ExperienceChannel.RISK, 0.30D)
            ));

    public static final SubstanceProfile KETAMINE = new SubstanceProfile(
            "ketamine",
            "substance.trippycraft.ketamine",
            SubstanceFamily.DISSOCIATIVE,
            new DoseCurve(0.15D, 1.0D, 2.4D, 180, 1200, 4600),
            1.0D,
            0.54D,
            0.42D,
            Map.ofEntries(
                    entry(ExperienceChannel.DISSOCIATION, 0.92D),
                    entry(ExperienceChannel.TIME_DILATION, 0.62D),
                    entry(ExperienceChannel.GEOMETRY_MELT, 0.34D),
                    entry(ExperienceChannel.DEPTH_BREATHING, 0.56D),
                    entry(ExperienceChannel.MOTOR_NOISE, 0.66D),
                    entry(ExperienceChannel.SEDATION, 0.42D),
                    entry(ExperienceChannel.RISK, 0.34D)
            ));

    public static final SubstanceProfile MESCALINE = new SubstanceProfile(
            "mescaline",
            "substance.trippycraft.mescaline",
            SubstanceFamily.PSYCHEDELIC,
            new DoseCurve(0.16D, 1.0D, 2.4D, 1600, 6500, 19000),
            1.0D,
            0.60D,
            0.0D,
            Map.ofEntries(
                    entry(ExperienceChannel.VISUAL_DRIFT, 0.62D),
                    entry(ExperienceChannel.GEOMETRY_MELT, 0.42D),
                    entry(ExperienceChannel.PATTERNING, 0.64D),
                    entry(ExperienceChannel.COLOR_BLOOM, 0.70D),
                    entry(ExperienceChannel.DEPTH_BREATHING, 0.46D),
                    entry(ExperienceChannel.BODY_LOAD, 0.44D),
                    entry(ExperienceChannel.EUPHORIA, 0.30D),
                    entry(ExperienceChannel.NAUSEA, 0.34D)
            ));

    public static final SubstanceProfile SALVIA = new SubstanceProfile(
            "salvia",
            "substance.trippycraft.salvia",
            SubstanceFamily.DELIRIANT,
            new DoseCurve(0.18D, 1.0D, 2.2D, 40, 280, 1300),
            1.0D,
            0.95D,
            0.46D,
            Map.ofEntries(
                    entry(ExperienceChannel.GEOMETRY_MELT, 0.72D),
                    entry(ExperienceChannel.TIME_DILATION, 0.76D),
                    entry(ExperienceChannel.EGO_DISSOLUTION, 0.70D),
                    entry(ExperienceChannel.DISSOCIATION, 0.82D),
                    entry(ExperienceChannel.MOTOR_NOISE, 0.48D),
                    entry(ExperienceChannel.ANXIETY, 0.42D),
                    entry(ExperienceChannel.RISK, 0.36D)
            ));

    public static final SubstanceProfile NITROUS = new SubstanceProfile(
            "nitrous",
            "substance.trippycraft.nitrous",
            SubstanceFamily.DISSOCIATIVE,
            new DoseCurve(0.14D, 1.0D, 2.1D, 20, 160, 700),
            1.0D,
            0.38D,
            0.28D,
            Map.ofEntries(
                    entry(ExperienceChannel.DISSOCIATION, 0.54D),
                    entry(ExperienceChannel.EUPHORIA, 0.38D),
                    entry(ExperienceChannel.TIME_DILATION, 0.44D),
                    entry(ExperienceChannel.MOTOR_NOISE, 0.36D),
                    entry(ExperienceChannel.SEDATION, 0.24D),
                    entry(ExperienceChannel.RISK, 0.24D)
            ));

    public static final SubstanceProfile ALCOHOL = new SubstanceProfile(
            "alcohol",
            "substance.trippycraft.alcohol",
            SubstanceFamily.DEPRESSANT,
            new DoseCurve(0.18D, 1.0D, 4.0D, 220, 1500, 6200),
            1.0D,
            0.28D,
            0.42D,
            Map.ofEntries(
                    entry(ExperienceChannel.SEDATION, 0.72D),
                    entry(ExperienceChannel.MOTOR_NOISE, 0.62D),
                    entry(ExperienceChannel.EUPHORIA, 0.30D),
                    entry(ExperienceChannel.BODY_LOAD, 0.30D),
                    entry(ExperienceChannel.NAUSEA, 0.24D),
                    entry(ExperienceChannel.RISK, 0.34D)
            ));

    public static final List<SubstanceProfile> ORDERED = List.of(
            PSILOCYBIN, LSD, TWO_CB, TWO_CE, HO_MET, AL_LAD, DMT, CANNABIS, COCAINE_POWDER, CRACK_COCAINE, HEROIN, MDMA, KETAMINE, MESCALINE, SALVIA, NITROUS, ALCOHOL
    );

    private SubstanceProfiles() {
    }

    public static Optional<SubstanceProfile> byId(String id) {
                if ("cocaine".equals(id)) {
                        return Optional.of(COCAINE_POWDER);
                }
        return ORDERED.stream().filter(profile -> profile.id().equals(id)).findFirst();
    }
}
