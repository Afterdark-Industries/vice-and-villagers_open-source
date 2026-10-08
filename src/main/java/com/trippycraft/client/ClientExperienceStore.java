package com.trippycraft.client;

import com.trippycraft.network.ExperienceSnapshotPacket;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholRecoveryTracker.RecoveryState;
import com.trippycraft.substance.cocaine.CocaineEffectState;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.heroin.HeroinEffectState;
import com.trippycraft.substance.heroin.HeroinRecoveryTracker;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.mdma.MdmaEffectState;
import com.trippycraft.substance.mdma.MdmaRecoveryTracker;
import com.trippycraft.substance.mescaline.MescalineEffectState;
import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.twoce.TwoCeEffectState;
import com.trippycraft.substance.nitrous.NitrousEffectState;
import com.trippycraft.substance.homet.HoMetEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import com.trippycraft.substance.cannabis.CannabisEffectState;
import com.trippycraft.substance.salvia.SalviaEffectState;
import com.trippycraft.substance.twocb.TwoCbEffectState;

public final class ClientExperienceStore {
    private static final ClientEffectState STATE = new ClientEffectState();
    private static PsilocybinEffectState psilocybin = PsilocybinEffectState.inactive();
    private static LsdEffectState lsd = LsdEffectState.inactive();
    private static DmtEffectState dmt = DmtEffectState.inactive();
    private static SalviaEffectState salvia = SalviaEffectState.inactive();
    private static CannabisEffectState cannabis = CannabisEffectState.inactive();
    private static MdmaEffectState mdma = MdmaEffectState.inactive();
    private static TwoCbEffectState twoCb = TwoCbEffectState.inactive();
    private static MescalineEffectState mescaline = MescalineEffectState.inactive();
    private static HoMetEffectState hoMet = HoMetEffectState.inactive();
    private static AlLadEffectState alLad = AlLadEffectState.inactive();
    private static TwoCeEffectState twoCe = TwoCeEffectState.inactive();
    private static NitrousEffectState nitrous = NitrousEffectState.inactive();
    private static MdmaRecoveryTracker.RecoveryState mdmaRecovery = MdmaRecoveryTracker.RecoveryState.inactive();
    private static KetamineEffectState ketamine = KetamineEffectState.inactive();
    private static CocaineEffectState cocaine = CocaineEffectState.inactive();
    private static HeroinEffectState heroin = HeroinEffectState.inactive();
    private static AlcoholEffectState alcohol = AlcoholEffectState.inactive();
    private static RecoveryState alcoholRecovery = RecoveryState.inactive();
    private static HeroinRecoveryTracker.RecoveryState heroinRecovery = HeroinRecoveryTracker.RecoveryState.inactive();
    private static int ticksActive;
    private static boolean active;

    private ClientExperienceStore() {
    }

    public static void accept(ExperienceSnapshotPacket packet) {
        active = packet.active();
        ticksActive = packet.ticksActive();
        if (active) {
            STATE.acceptServerSnapshot(packet.channels(), packet.familyBlend());
            psilocybin = packet.psilocybin();
            lsd = packet.lsd();
            dmt = packet.dmt();
            salvia = packet.salvia();
            alcohol = packet.alcohol();
            cannabis = packet.cannabis();
            mdma = packet.mdma();
            twoCb = packet.twoCb();
            mescaline = packet.mescaline();
            hoMet = packet.hoMet();
            alLad = packet.alLad();
            twoCe = packet.twoCe();
            nitrous = packet.nitrous();
            mdmaRecovery = packet.mdmaRecovery();
            ketamine = packet.ketamine();
            cocaine = packet.cocaine();
            heroin = packet.heroin();
            alcoholRecovery = packet.alcoholRecovery();
            heroinRecovery = packet.heroinRecovery();
        } else {
            clear();
        }
    }

    public static void clear() {
        active = false;
        ticksActive = 0;
        STATE.clear();
        psilocybin = PsilocybinEffectState.inactive();
        lsd = LsdEffectState.inactive();
        dmt = DmtEffectState.inactive();
        salvia = SalviaEffectState.inactive();
        alcohol = AlcoholEffectState.inactive();
        cannabis = CannabisEffectState.inactive();
        mdma = MdmaEffectState.inactive();
        twoCb = TwoCbEffectState.inactive();
        mescaline = MescalineEffectState.inactive();
        hoMet = HoMetEffectState.inactive();
        alLad = AlLadEffectState.inactive();
        twoCe = TwoCeEffectState.inactive();
        nitrous = NitrousEffectState.inactive();
        mdmaRecovery = MdmaRecoveryTracker.RecoveryState.inactive();
        ketamine = KetamineEffectState.inactive();
        cocaine = CocaineEffectState.inactive();
        heroin = HeroinEffectState.inactive();
        alcoholRecovery = RecoveryState.inactive();
        heroinRecovery = HeroinRecoveryTracker.RecoveryState.inactive();
    }

    public static ClientEffectState state() {
        return STATE;
    }

    public static int ticksActive() {
        return ticksActive;
    }

    public static boolean active() {
        return active;
    }

    public static PsilocybinEffectState psilocybin() {
        return psilocybin;
    }

    public static LsdEffectState lsd() {
        return lsd;
    }

    public static DmtEffectState dmt() {
        return dmt;
    }

    public static CannabisEffectState cannabis() {
        return cannabis;
    }

    public static MdmaEffectState mdma() {
        return mdma;
    }

    public static TwoCbEffectState twoCb() {
        return twoCb;
    }

    public static HoMetEffectState hoMet() {
        return hoMet;
    }

    public static AlLadEffectState alLad() {
        return alLad;
    }

    public static TwoCeEffectState twoCe() {
        return twoCe;
    }

    public static NitrousEffectState nitrous() {
        return nitrous;
    }

    public static MescalineEffectState mescaline() {
        return mescaline;
    }

    public static MdmaRecoveryTracker.RecoveryState mdmaRecovery() {
        return mdmaRecovery;
    }

    public static KetamineEffectState ketamine() {
        return ketamine;
    }

    public static CocaineEffectState cocaine() {
        return cocaine;
    }

    public static HeroinEffectState heroin() {
        return heroin;
    }

    public static SalviaEffectState salvia() {
        return salvia;
    }

    public static AlcoholEffectState alcohol() {
        return alcohol;
    }

    public static RecoveryState alcoholRecovery() {
        return alcoholRecovery;
    }

    public static HeroinRecoveryTracker.RecoveryState heroinRecovery() {
        return heroinRecovery;
    }
}
