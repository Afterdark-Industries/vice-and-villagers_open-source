package com.trippycraft.economy;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MarketPlayerData {
    private int reputation;
    private int totalUnitsSold;
    private int completedContracts;
    private int nextContractSerial;
    private long lastDemandRefreshGameTime;
    // Retained for old save compatibility only; contracts no longer expire or refresh on a timer.
    private long lastContractRefreshGameTime;
    private final Map<String, Integer> demandMultipliers = new LinkedHashMap<>();
    private final List<MarketContract> activeContracts = new ArrayList<>();

    public int reputation() {
        return reputation;
    }

    public int level() {
        return MarketMath.reputationLevel(reputation);
    }

    public int totalUnitsSold() {
        return totalUnitsSold;
    }

    public int completedContracts() {
        return completedContracts;
    }

    public long lastDemandRefreshGameTime() {
        return lastDemandRefreshGameTime;
    }

    public void setLastDemandRefreshGameTime(long lastDemandRefreshGameTime) {
        this.lastDemandRefreshGameTime = lastDemandRefreshGameTime;
    }

    public long lastContractRefreshGameTime() {
        return lastContractRefreshGameTime;
    }

    public void setLastContractRefreshGameTime(long lastContractRefreshGameTime) {
        this.lastContractRefreshGameTime = lastContractRefreshGameTime;
    }

    public List<MarketContract> activeContracts() {
        return activeContracts;
    }

    public int demandMultiplier(String productId) {
        return demandMultipliers.getOrDefault(productId, 100);
    }

    public void setDemandMultiplier(String productId, int multiplier) {
        demandMultipliers.put(productId, Math.clamp(multiplier, MarketMath.MIN_DEMAND, MarketMath.MAX_DEMAND));
    }

    public Map<String, Integer> demandMultipliers() {
        return demandMultipliers;
    }

    public void addReputation(int amount) {
        reputation = Math.max(0, reputation + Math.max(0, amount));
    }

    public void addSoldUnits(int amount) {
        totalUnitsSold = Math.max(0, totalUnitsSold + Math.max(0, amount));
    }

    public void addCompletedContract() {
        completedContracts = Math.max(0, completedContracts + 1);
    }

    public String nextContractId() {
        nextContractSerial++;
        return "contract_" + nextContractSerial;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("reputation", reputation);
        tag.putInt("totalUnitsSold", totalUnitsSold);
        tag.putInt("completedContracts", completedContracts);
        tag.putInt("nextContractSerial", nextContractSerial);
        tag.putLong("lastDemandRefreshGameTime", lastDemandRefreshGameTime);
        tag.putLong("lastContractRefreshGameTime", lastContractRefreshGameTime);

        ListTag demandList = new ListTag();
        demandMultipliers.forEach((productId, multiplier) -> {
            CompoundTag demandTag = new CompoundTag();
            demandTag.putString("productId", productId);
            demandTag.putInt("multiplier", multiplier);
            demandList.add(demandTag);
        });
        tag.put("demand", demandList);

        ListTag contractList = new ListTag();
        for (MarketContract contract : activeContracts) {
            contractList.add(contract.save());
        }
        tag.put("contracts", contractList);
        return tag;
    }

    public static MarketPlayerData load(CompoundTag tag) {
        MarketPlayerData data = new MarketPlayerData();
        data.reputation = Math.max(0, NbtCompat.getIntOr(tag, "reputation", 0));
        data.totalUnitsSold = Math.max(0, NbtCompat.getIntOr(tag, "totalUnitsSold", 0));
        data.completedContracts = Math.max(0, NbtCompat.getIntOr(tag, "completedContracts", 0));
        data.nextContractSerial = Math.max(0, NbtCompat.getIntOr(tag, "nextContractSerial", 0));
        data.lastDemandRefreshGameTime = Math.max(0L, NbtCompat.getLongOr(tag, "lastDemandRefreshGameTime", 0L));
        data.lastContractRefreshGameTime = Math.max(0L, NbtCompat.getLongOr(tag, "lastContractRefreshGameTime", 0L));

        NbtCompat.compoundStream(NbtCompat.getListOrEmpty(tag, "demand")).forEach(demandTag -> {
            String productId = NbtCompat.getStringOr(demandTag, "productId", "");
            if (!productId.isBlank()) {
                data.setDemandMultiplier(productId, NbtCompat.getIntOr(demandTag, "multiplier", 100));
            }
        });

        NbtCompat.compoundStream(NbtCompat.getListOrEmpty(tag, "contracts"))
                .map(MarketContract::load)
                .filter(contract -> MarketRegistries.sellableProduct(contract.productId()).isPresent())
                .forEach(data.activeContracts::add);
        return data;
    }

    public MarketPlayerData copy() {
        MarketPlayerData copy = new MarketPlayerData();
        copy.reputation = reputation;
        copy.totalUnitsSold = totalUnitsSold;
        copy.completedContracts = completedContracts;
        copy.nextContractSerial = nextContractSerial;
        copy.lastDemandRefreshGameTime = lastDemandRefreshGameTime;
        copy.lastContractRefreshGameTime = lastContractRefreshGameTime;
        copy.demandMultipliers.putAll(demandMultipliers);
        copy.activeContracts.addAll(activeContracts);
        return copy;
    }
}
