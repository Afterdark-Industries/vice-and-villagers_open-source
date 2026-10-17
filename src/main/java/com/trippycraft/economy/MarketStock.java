package com.trippycraft.economy;

import com.trippycraft.item.AlLadConcentrateItem;
import com.trippycraft.item.CannabisDoseItem;
import com.trippycraft.item.EthanolItem;
import com.trippycraft.item.HoMetCrystalItem;
import com.trippycraft.item.MdmaCrystalItem;
import com.trippycraft.item.StackableDoseItemSupport;
import com.trippycraft.item.TwoCbCrystalItem;
import com.trippycraft.item.TwoCeCrystalItem;
import com.trippycraft.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The broker's displayed unit price and contracts require one full, unused product. */
public final class MarketStock {
    private MarketStock() { }

    /** Shared by client counts and authoritative server inventory removal. */
    public static boolean matches(ItemStack stack, Item productItem) {
        if (stack.isEmpty() || !matchesItem(stack, productItem)) return false;

        Item actualItem = stack.getItem();
        if (actualItem instanceof CannabisDoseItem cannabis) {
            int fullUses = Math.max(1, cannabis.totalThcMilligrams() / Math.max(1, cannabis.thcMilligramsPerHit()));
            return StackableDoseItemSupport.remainingUses(stack, fullUses) == fullUses;
        }
        if (actualItem instanceof EthanolItem) {
            return StackableDoseItemSupport.remainingUses(stack, EthanolItem.TOTAL_USES) == EthanolItem.TOTAL_USES;
        }
        if (actualItem instanceof MdmaCrystalItem) {
            return MdmaCrystalItem.remainingMilligrams(stack) == MdmaCrystalItem.CAPACITY_MILLIGRAMS;
        }
        if (actualItem instanceof HoMetCrystalItem) {
            return HoMetCrystalItem.remainingMilligrams(stack) == HoMetCrystalItem.CAPACITY_MILLIGRAMS;
        }
        if (actualItem instanceof TwoCbCrystalItem) {
            return TwoCbCrystalItem.remainingMilligrams(stack) == TwoCbCrystalItem.TOTAL_MILLIGRAMS;
        }
        if (actualItem instanceof TwoCeCrystalItem) {
            return TwoCeCrystalItem.remainingMilligrams(stack) == TwoCeCrystalItem.TOTAL_MILLIGRAMS;
        }
        if (actualItem instanceof AlLadConcentrateItem) {
            return AlLadConcentrateItem.remainingMilligrams(stack) == AlLadConcentrateItem.CAPACITY_MILLIGRAMS;
        }
        return true;
    }

    private static boolean matchesItem(ItemStack stack, Item productItem) {
        return DealerRegistries.matchesProductItem(stack, productItem)
                || (stack.is(ModItems.HEROIN_TAR_LEGACY.get()) && productItem == ModItems.HEROIN_100MG.get())
                || (stack.is(ModItems.MESCALINE_BUTTON_LEGACY.get()) && productItem == ModItems.MESCALINE_100MG.get());
    }
}
