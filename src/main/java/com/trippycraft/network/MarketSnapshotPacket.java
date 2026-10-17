package com.trippycraft.network;

import com.trippycraft.TrippyCraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public final class MarketSnapshotPacket implements CustomPacketPayload {
    public static final Type<MarketSnapshotPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "market_snapshot"));
    public static final StreamCodec<FriendlyByteBuf, MarketSnapshotPacket> STREAM_CODEC = StreamCodec.ofMember(MarketSnapshotPacket::encode, MarketSnapshotPacket::decode);

    @Override
    public Type<MarketSnapshotPacket> type() {
        return TYPE;
    }
    private final boolean openScreen;
    private final int reputation;
    private final int level;
    private final int totalUnitsSold;
    private final int completedContracts;
    // Legacy wire field retained for compatibility; the server sends zero for persistent contracts.
    private final long ticksUntilContractRefresh;
    private final List<DemandInfo> demand;
    private final List<ContractInfo> contracts;

    public MarketSnapshotPacket(boolean openScreen,
                                int reputation,
                                int level,
                                int totalUnitsSold,
                                int completedContracts,
                                long ticksUntilContractRefresh,
                                List<DemandInfo> demand,
                                List<ContractInfo> contracts) {
        this.openScreen = openScreen;
        this.reputation = Math.max(0, reputation);
        this.level = Math.max(0, level);
        this.totalUnitsSold = Math.max(0, totalUnitsSold);
        this.completedContracts = Math.max(0, completedContracts);
        this.ticksUntilContractRefresh = Math.max(0L, ticksUntilContractRefresh);
        this.demand = List.copyOf(demand);
        this.contracts = List.copyOf(contracts);
    }

    public static void encode(MarketSnapshotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.openScreen);
        buffer.writeVarInt(packet.reputation);
        buffer.writeVarInt(packet.level);
        buffer.writeVarInt(packet.totalUnitsSold);
        buffer.writeVarInt(packet.completedContracts);
        buffer.writeLong(packet.ticksUntilContractRefresh);
        buffer.writeVarInt(packet.demand.size());
        for (DemandInfo demandInfo : packet.demand) {
            buffer.writeUtf(demandInfo.productId());
            buffer.writeVarInt(demandInfo.multiplier());
        }
        buffer.writeVarInt(packet.contracts.size());
        for (ContractInfo contract : packet.contracts) {
            buffer.writeUtf(contract.id());
            buffer.writeUtf(contract.productId());
            buffer.writeUtf(contract.type());
            buffer.writeVarInt(contract.quantity());
            buffer.writeVarInt(contract.rewardEmeralds());
            buffer.writeVarInt(contract.reputationReward());
            buffer.writeVarInt(contract.bonusMultiplier());
        }
    }

    public static MarketSnapshotPacket decode(FriendlyByteBuf buffer) {
        boolean openScreen = buffer.readBoolean();
        int reputation = buffer.readVarInt();
        int level = buffer.readVarInt();
        int totalUnitsSold = buffer.readVarInt();
        int completedContracts = buffer.readVarInt();
        long ticksUntilContractRefresh = buffer.readLong();

        int demandCount = buffer.readVarInt();
        List<DemandInfo> demand = new ArrayList<>();
        for (int i = 0; i < demandCount; i++) {
            demand.add(new DemandInfo(buffer.readUtf(), buffer.readVarInt()));
        }

        int contractCount = buffer.readVarInt();
        List<ContractInfo> contracts = new ArrayList<>();
        for (int i = 0; i < contractCount; i++) {
            contracts.add(new ContractInfo(
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readUtf(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            ));
        }

        return new MarketSnapshotPacket(
                openScreen,
                reputation,
                level,
                totalUnitsSold,
                completedContracts,
                ticksUntilContractRefresh,
                demand,
                contracts
        );
    }

    public static void handle(MarketSnapshotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.trippycraft.client.ClientPacketHandlers.handleMarketSnapshot(packet));
    }

    public boolean openScreen() {
        return openScreen;
    }

    public int reputation() {
        return reputation;
    }

    public int level() {
        return level;
    }

    public int totalUnitsSold() {
        return totalUnitsSold;
    }

    public int completedContracts() {
        return completedContracts;
    }

    public long ticksUntilContractRefresh() {
        return ticksUntilContractRefresh;
    }

    public List<DemandInfo> demand() {
        return demand;
    }

    public List<ContractInfo> contracts() {
        return contracts;
    }

    public int demandMultiplier(String productId) {
        for (DemandInfo demandInfo : demand) {
            if (demandInfo.productId().equals(productId)) {
                return demandInfo.multiplier();
            }
        }
        return 100;
    }

    public record DemandInfo(String productId, int multiplier) {
    }

    public record ContractInfo(
            String id,
            String productId,
            String type,
            int quantity,
            int rewardEmeralds,
            int reputationReward,
            int bonusMultiplier
    ) {
    }
}
