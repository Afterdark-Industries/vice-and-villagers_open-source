package com.trippycraft.client;

import com.trippycraft.substance.SubstanceFamily;

public record ClientRenderSignature(
        double psychedelic,
        double cannabinoid,
        double stimulant,
        double opioid,
        double empathogen,
        double dissociative,
        double depressant,
        double deliriant,
        double organicFlow,
        double warmth,
        double coolDistance,
        double edgeSharpness,
        double narrowing,
        double depthSeparation,
        double temporalSlip,
        double instability,
        double motorLooseness,
        double patternOrder
) {
    public static ClientRenderSignature from(ClientFamilyBlend blend) {
        double intensity = blend.intensity();
        double psychedelic = blend.weight(SubstanceFamily.PSYCHEDELIC) * intensity;
        double cannabinoid = blend.weight(SubstanceFamily.CANNABINOID) * intensity;
        double stimulant = blend.weight(SubstanceFamily.STIMULANT) * intensity;
        double opioid = blend.weight(SubstanceFamily.OPIOID) * intensity;
        double empathogen = blend.weight(SubstanceFamily.EMPATHOGEN) * intensity;
        double dissociative = blend.weight(SubstanceFamily.DISSOCIATIVE) * intensity;
        double depressant = blend.weight(SubstanceFamily.DEPRESSANT) * intensity;
        double deliriant = blend.weight(SubstanceFamily.DELIRIANT) * intensity;

        return new ClientRenderSignature(
                psychedelic,
                cannabinoid,
                stimulant,
                opioid,
                empathogen,
                dissociative,
                depressant,
                deliriant,
                saturate(psychedelic * 0.72D + cannabinoid * 0.24D + empathogen * 0.22D),
                saturate(cannabinoid * 0.48D + empathogen * 0.44D + opioid * 0.24D + psychedelic * 0.18D),
                saturate(dissociative * 0.62D + depressant * 0.28D + deliriant * 0.34D),
                saturate(stimulant * 0.76D + empathogen * 0.18D + deliriant * 0.18D),
                saturate(opioid * 0.74D + depressant * 0.54D + cannabinoid * 0.20D),
                saturate(dissociative * 0.86D + psychedelic * 0.24D + deliriant * 0.40D),
                saturate(dissociative * 0.60D + psychedelic * 0.38D + cannabinoid * 0.25D + deliriant * 0.54D),
                saturate(stimulant * 0.48D + deliriant * 0.58D + opioid * 0.20D),
                saturate(cannabinoid * 0.52D + opioid * 0.42D + depressant * 0.40D + dissociative * 0.36D),
                saturate(psychedelic * 0.85D + deliriant * 0.38D + empathogen * 0.18D)
        );
    }

    private static double saturate(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
