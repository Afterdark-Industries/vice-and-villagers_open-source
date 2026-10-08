package com.trippycraft.client;

public record ClientEffectSample(
        float partialTick,
        float time,
        ClientFamilyBlend familyBlend,
        ClientRenderSignature signature,
        double visualDrift,
        double geometryMelt,
        double patterning,
        double colorBloom,
        double depthBreathing,
        double timeDilation,
        double egoDissolution,
        double bodyLoad,
        double motorNoise,
        double stimulation,
        double sedation,
        double euphoria,
        double anxiety,
        double dissociation,
        double nausea,
        double craving,
        double risk,
        double visualLoad,
        double motionLoad,
        double overlayAlpha
) {
    public boolean visible() {
        return overlayAlpha > 0.003D || motionLoad > 0.003D || visualLoad > 0.003D;
    }
}
