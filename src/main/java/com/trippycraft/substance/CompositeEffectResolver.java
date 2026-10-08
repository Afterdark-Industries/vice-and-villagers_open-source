package com.trippycraft.substance;

import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.twoce.TwoCeEffectState;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;

public final class CompositeEffectResolver {
    private CompositeEffectResolver() {
    }

    public static PsychedelicBlendState resolve(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet, AlLadEffectState alLad, TwoCeEffectState twoCe) {
        return PsychedelicBlendState.from(psilocybin, lsd, dmt, hoMet, alLad, twoCe);
    }

    public static PsychedelicBlendState resolve(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet, AlLadEffectState alLad) {
        return PsychedelicBlendState.from(psilocybin, lsd, dmt, hoMet, alLad);
    }

    public static PsychedelicBlendState resolve(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, HoMetEffectState hoMet) {
        return PsychedelicBlendState.from(psilocybin, lsd, dmt, hoMet);
    }

    public static PsychedelicBlendState resolve(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt) {
        return PsychedelicBlendState.from(psilocybin, lsd, dmt);
    }
}
