package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.SubstanceFamily;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholRecoveryTracker.RecoveryState;
import com.trippycraft.substance.cannabis.CannabisEffectState;
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
import com.trippycraft.substance.salvia.SalviaEffectState;
import com.trippycraft.substance.twocb.TwoCbEffectState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.EnumMap;
import java.util.Map;

public final class ExperienceSnapshotPacket implements CustomPacketPayload {
    public static final Type<ExperienceSnapshotPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "experience_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, ExperienceSnapshotPacket> STREAM_CODEC = StreamCodec.ofMember(ExperienceSnapshotPacket::encode, ExperienceSnapshotPacket::decode);

    @Override
    public Type<ExperienceSnapshotPacket> type() {
        return TYPE;
    }
    private final int ticksActive;
    private final boolean active;
    private final EnumMap<ExperienceChannel, Double> channels;
    private final EnumMap<SubstanceFamily, Double> familyBlend;
    private final PsilocybinEffectState psilocybin;
    private final LsdEffectState lsd;
    private final DmtEffectState dmt;
    private final CannabisEffectState cannabis;
    private final MdmaEffectState mdma;
    private final TwoCbEffectState twoCb;
    private final MescalineEffectState mescaline;
    private final HoMetEffectState hoMet;
    private final AlLadEffectState alLad;
    private final TwoCeEffectState twoCe;
    private final NitrousEffectState nitrous;
    private final MdmaRecoveryTracker.RecoveryState mdmaRecovery;
    private final KetamineEffectState ketamine;
    private final CocaineEffectState cocaine;
    private final HeroinEffectState heroin;
    private final SalviaEffectState salvia;
    private final AlcoholEffectState alcohol;
    private final RecoveryState alcoholRecovery;
    private final HeroinRecoveryTracker.RecoveryState heroinRecovery;

    public ExperienceSnapshotPacket(int ticksActive, boolean active, Map<ExperienceChannel, Double> channels, Map<SubstanceFamily, Double> familyBlend, PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt, CannabisEffectState cannabis, MdmaEffectState mdma, TwoCbEffectState twoCb, MescalineEffectState mescaline, HoMetEffectState hoMet, AlLadEffectState alLad, TwoCeEffectState twoCe, NitrousEffectState nitrous, MdmaRecoveryTracker.RecoveryState mdmaRecovery, KetamineEffectState ketamine, CocaineEffectState cocaine, HeroinEffectState heroin, SalviaEffectState salvia, AlcoholEffectState alcohol, RecoveryState alcoholRecovery, HeroinRecoveryTracker.RecoveryState heroinRecovery) {
        this.ticksActive = Math.max(0, ticksActive);
        this.active = active;
        this.channels = new EnumMap<>(ExperienceChannel.class);
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            this.channels.put(channel, clamp(channels.getOrDefault(channel, 0.0D)));
        }
        this.familyBlend = new EnumMap<>(SubstanceFamily.class);
        for (SubstanceFamily family : SubstanceFamily.values()) {
            this.familyBlend.put(family, clamp(familyBlend.getOrDefault(family, 0.0D)));
        }
        this.psilocybin = psilocybin;
        this.lsd = lsd;
        this.dmt = dmt;
        this.cannabis = cannabis;
        this.mdma = mdma;
        this.twoCb = twoCb;
        this.mescaline = mescaline;
        this.hoMet = hoMet;
        this.alLad = alLad;
        this.twoCe = twoCe;
        this.nitrous = nitrous;
        this.mdmaRecovery = mdmaRecovery;
        this.ketamine = ketamine;
        this.cocaine = cocaine;
        this.heroin = heroin;
        this.salvia = salvia;
        this.alcohol = alcohol;
        this.alcoholRecovery = alcoholRecovery;
        this.heroinRecovery = heroinRecovery;
    }

    public static ExperienceSnapshotPacket from(PsychoactiveExperience experience) {
        return new ExperienceSnapshotPacket(experience.ticksActive(), experience.hasClientVisualState(), experience.channels(), experience.familyBlend(), experience.psilocybinState(), experience.lsdState(), experience.dmtState(), experience.cannabisState(), experience.mdmaState(), experience.twoCbState(), experience.mescalineState(), experience.hoMetState(), experience.alLadState(), experience.twoCeState(), experience.nitrousState(), experience.mdmaRecoveryState(), experience.ketamineState(), experience.cocaineState(), experience.heroinState(), experience.salviaState(), experience.alcoholState(), experience.alcoholRecoveryState(), experience.heroinRecoveryState());
    }

    public static ExperienceSnapshotPacket clear() {
        return new ExperienceSnapshotPacket(0, false, Map.of(), Map.of(), PsilocybinEffectState.inactive(), LsdEffectState.inactive(), DmtEffectState.inactive(), CannabisEffectState.inactive(), MdmaEffectState.inactive(), TwoCbEffectState.inactive(), MescalineEffectState.inactive(), HoMetEffectState.inactive(), AlLadEffectState.inactive(), TwoCeEffectState.inactive(), NitrousEffectState.inactive(), MdmaRecoveryTracker.RecoveryState.inactive(), KetamineEffectState.inactive(), CocaineEffectState.inactive(), HeroinEffectState.inactive(), SalviaEffectState.inactive(), AlcoholEffectState.inactive(), RecoveryState.inactive(), HeroinRecoveryTracker.RecoveryState.inactive());
    }

    public static void encode(ExperienceSnapshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.ticksActive);
        buffer.writeBoolean(packet.active);
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            buffer.writeDouble(packet.channels.getOrDefault(channel, 0.0D));
        }
        for (SubstanceFamily family : SubstanceFamily.values()) {
            buffer.writeDouble(packet.familyBlend.getOrDefault(family, 0.0D));
        }
        packet.psilocybin.encode(buffer);
        packet.lsd.encode(buffer);
        packet.dmt.encode(buffer);
        packet.cannabis.encode(buffer);
        packet.mdma.encode(buffer);
        packet.twoCb.encode(buffer);
        packet.mescaline.encode(buffer);
        packet.hoMet.encode(buffer);
        packet.alLad.encode(buffer);
        packet.twoCe.encode(buffer);
        packet.nitrous.encode(buffer);
        packet.mdmaRecovery.encode(buffer);
        packet.ketamine.encode(buffer);
        packet.cocaine.encode(buffer);
        packet.heroin.encode(buffer);
        packet.salvia.encode(buffer);
        packet.alcohol.encode(buffer);
        packet.alcoholRecovery.encode(buffer);
        packet.heroinRecovery.encode(buffer);
    }

    public static ExperienceSnapshotPacket decode(FriendlyByteBuf buffer) {
        int ticksActive = buffer.readInt();
        boolean active = buffer.readBoolean();
        EnumMap<ExperienceChannel, Double> channels = new EnumMap<>(ExperienceChannel.class);
        for (ExperienceChannel channel : ExperienceChannel.values()) {
            channels.put(channel, clamp(buffer.readDouble()));
        }
        EnumMap<SubstanceFamily, Double> familyBlend = new EnumMap<>(SubstanceFamily.class);
        for (SubstanceFamily family : SubstanceFamily.values()) {
            familyBlend.put(family, clamp(buffer.readDouble()));
        }
        PsilocybinEffectState psilocybin = PsilocybinEffectState.decode(buffer);
        LsdEffectState lsd = LsdEffectState.decode(buffer);
        DmtEffectState dmt = DmtEffectState.decode(buffer);
        CannabisEffectState cannabis = CannabisEffectState.decode(buffer);
        MdmaEffectState mdma = MdmaEffectState.decode(buffer);
        TwoCbEffectState twoCb = TwoCbEffectState.decode(buffer);
        MescalineEffectState mescaline = MescalineEffectState.decode(buffer);
        HoMetEffectState hoMet = HoMetEffectState.decode(buffer);
        AlLadEffectState alLad = AlLadEffectState.decode(buffer);
        TwoCeEffectState twoCe = TwoCeEffectState.decode(buffer);
        NitrousEffectState nitrous = NitrousEffectState.decode(buffer);
        MdmaRecoveryTracker.RecoveryState mdmaRecovery = MdmaRecoveryTracker.RecoveryState.decode(buffer);
        KetamineEffectState ketamine = KetamineEffectState.decode(buffer);
        CocaineEffectState cocaine = CocaineEffectState.decode(buffer);
        HeroinEffectState heroin = HeroinEffectState.decode(buffer);
        SalviaEffectState salvia = SalviaEffectState.decode(buffer);
        AlcoholEffectState alcohol = AlcoholEffectState.decode(buffer);
        RecoveryState alcoholRecovery = RecoveryState.decode(buffer);
        HeroinRecoveryTracker.RecoveryState heroinRecovery = HeroinRecoveryTracker.RecoveryState.decode(buffer);
        return new ExperienceSnapshotPacket(ticksActive, active, channels, familyBlend, psilocybin, lsd, dmt, cannabis, mdma, twoCb, mescaline, hoMet, alLad, twoCe, nitrous, mdmaRecovery, ketamine, cocaine, heroin, salvia, alcohol, alcoholRecovery, heroinRecovery);
    }

    public static void handle(ExperienceSnapshotPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.trippycraft.client.ClientPacketHandlers.handleExperienceSnapshot(packet);
        }
    }

    public int ticksActive() {
        return ticksActive;
    }

    public boolean active() {
        return active;
    }

    public Map<ExperienceChannel, Double> channels() {
        return Map.copyOf(channels);
    }

    public Map<SubstanceFamily, Double> familyBlend() {
        return Map.copyOf(familyBlend);
    }

    public PsilocybinEffectState psilocybin() {
        return psilocybin;
    }

    public LsdEffectState lsd() {
        return lsd;
    }

    public DmtEffectState dmt() {
        return dmt;
    }

    public CannabisEffectState cannabis() {
        return cannabis;
    }

    public MdmaEffectState mdma() {
        return mdma;
    }

    public TwoCbEffectState twoCb() {
        return twoCb;
    }

    public HoMetEffectState hoMet() {
        return hoMet;
    }

    public AlLadEffectState alLad() {
        return alLad;
    }

    public TwoCeEffectState twoCe() {
        return twoCe;
    }

    public NitrousEffectState nitrous() {
        return nitrous;
    }

    public MescalineEffectState mescaline() {
        return mescaline;
    }

    public MdmaRecoveryTracker.RecoveryState mdmaRecovery() {
        return mdmaRecovery;
    }

    public KetamineEffectState ketamine() {
        return ketamine;
    }

    public CocaineEffectState cocaine() {
        return cocaine;
    }

    public HeroinEffectState heroin() {
        return heroin;
    }

    public SalviaEffectState salvia() {
        return salvia;
    }

    public AlcoholEffectState alcohol() {
        return alcohol;
    }

    public RecoveryState alcoholRecovery() {
        return alcoholRecovery;
    }

    public HeroinRecoveryTracker.RecoveryState heroinRecovery() {
        return heroinRecovery;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
