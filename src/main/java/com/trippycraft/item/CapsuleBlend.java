package com.trippycraft.item;

import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.SubstanceProfile;
import com.trippycraft.substance.SubstanceProfiles;
import com.trippycraft.substance.cocaine.CocaineRoute;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CapsuleBlend {
    private static final String ROOT_KEY = "TrippyCraftCapsuleBlend";
    private static final String ENTRIES_KEY = "Entries";
    private static final String KIND_KEY = "Kind";
    private static final String AMOUNT_KEY = "Amount";
    private static final String EFFECT_AMOUNT_KEY = "EffectAmount";
    private static final String PROFILE_KEY = "Profile";

    private static final String KIND_PSILOCYBIN_GRAMS = "psilocybin_grams";
    private static final String KIND_LSD_MICROGRAMS = "lsd_micrograms";
    private static final String KIND_DMT_UNITS = "dmt_units";
    private static final String KIND_CANNABIS_MILLIGRAMS = "cannabis_milligrams";
    private static final String KIND_MDMA_MILLIGRAMS = "mdma_milligrams";
    private static final String KIND_TWO_CB_MILLIGRAMS = "two_cb_milligrams";
    private static final String KIND_HO_MET_MILLIGRAMS = "four_ho_met_milligrams";
    private static final String KIND_AL_LAD_MICROGRAMS = "al_lad_micrograms";
    private static final String KIND_AL_LAD_MILLIGRAMS = "al_lad_milligrams";
    private static final String KIND_TWO_CE_MILLIGRAMS = "two_ce_milligrams";
    private static final String KIND_NITROUS_HITS = "nitrous_hits";
    private static final String KIND_MESCALINE_MILLIGRAMS = "mescaline_milligrams";
    private static final String KIND_KOKA_GRAMS = "koka_grams";
    private static final String KIND_COCAINE_POWDER_GRAMS = "cocaine_powder_grams";
    private static final String KIND_CRACK_COCAINE_GRAMS = "crack_cocaine_grams";
    private static final String KIND_HEROIN_MILLIGRAMS = "heroin_milligrams";
    private static final String KIND_KETAMINE_MILLIGRAMS = "ketamine_milligrams";
    private static final String KIND_SALVIA_UNITS = "salvia_units";
    private static final String KIND_ALCOHOL_STANDARD_DRINKS = "alcohol_standard_drinks";
    private static final String KIND_PROFILE_UNITS = "profile_units";

    private CapsuleBlend() {
    }

    public static Optional<Entry> entryFromDoseItem(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ModItems.BLEND_CAPSULE.get()) || stack.is(ModItems.EMPTY_CAPSULE.get())) {
            return Optional.empty();
        }

        Item item = stack.getItem();
        if (item instanceof PsilocybinCapsuleItem capsule) {
            return entry(KIND_PSILOCYBIN_GRAMS, capsule.doseGrams());
        }
        if (item instanceof LsdBlotterItem blotter) {
            return entry(KIND_LSD_MICROGRAMS, blotter.doseMicrograms());
        }
        if (item instanceof LsdSolutionItem solution) {
            return entry(KIND_LSD_MICROGRAMS, solution.doseMicrograms());
        }
        if (item instanceof DmtItem dmt) {
            return entry(KIND_DMT_UNITS, dmt.doseUnits());
        }
        if (item instanceof CannabisDoseItem cannabis) {
            int totalUses = Math.max(1, cannabis.totalThcMilligrams() / Math.max(1, cannabis.thcMilligramsPerHit()));
            int remainingUses = StackableDoseItemSupport.remainingUses(stack, totalUses);
            return entry(KIND_CANNABIS_MILLIGRAMS, remainingUses * (double)cannabis.thcMilligramsPerHit());
        }
        if (item instanceof CannabisEdibleItem edible) {
            return entry(KIND_CANNABIS_MILLIGRAMS, edible.thcMilligrams());
        }
        if (item instanceof MdmaDoseItem mdma) {
            return entry(KIND_MDMA_MILLIGRAMS, mdma.doseMilligrams());
        }
        if (item instanceof TwoCbDoseItem twoCb) {
            return entry(KIND_TWO_CB_MILLIGRAMS, twoCb.doseMilligrams());
        }
        if (item instanceof HoMetDoseItem hoMet) {
            return Optional.of(new Entry(KIND_HO_MET_MILLIGRAMS, hoMet.amountMilligrams(), "", hoMet.doseMilligrams()));
        }
        if (item instanceof AlLadDoseItem alLad) {
            return Optional.of(new Entry(KIND_AL_LAD_MILLIGRAMS, alLad.amountMilligrams(), "", alLad.doseMicrograms()));
        }
        if (item instanceof TwoCeDoseItem twoCe) {
            return Optional.of(new Entry(KIND_TWO_CE_MILLIGRAMS, twoCe.contentMilligrams(), "", twoCe.doseMilligrams()));
        }
        if (item instanceof NitrousDoseItem nitrous) {
            return entry(KIND_NITROUS_HITS, nitrous.hits());
        }
        if (item instanceof MescalineDoseItem mescaline) {
            return entry(KIND_MESCALINE_MILLIGRAMS, mescaline.doseMilligrams());
        }
        if (item instanceof KokaDoseItem koka) {
            return entry(KIND_KOKA_GRAMS, koka.doseGrams());
        }
        if (item instanceof CocaineItem cocaine) {
            return entry(cocaine.route() == CocaineRoute.CRACK ? KIND_CRACK_COCAINE_GRAMS : KIND_COCAINE_POWDER_GRAMS, cocaine.doseGrams());
        }
        if (item instanceof HeroinItem heroin) {
            return entry(KIND_HEROIN_MILLIGRAMS, heroin.doseMilligrams());
        }
        if (item instanceof KetamineDoseItem ketamine) {
            return entry(KIND_KETAMINE_MILLIGRAMS, ketamine.doseMilligrams());
        }
        if (item instanceof KetamineBottleItem bottle) {
            int totalUses = Math.max(1, bottle.totalMilligrams() / Math.max(1, bottle.milligramsPerUse()));
            int remainingUses = StackableDoseItemSupport.remainingUses(stack, totalUses);
            return entry(KIND_KETAMINE_MILLIGRAMS, remainingUses * (double)bottle.milligramsPerUse());
        }
        if (item instanceof AlcoholItem alcohol) {
            return entry(KIND_ALCOHOL_STANDARD_DRINKS, alcohol.standardDrinks());
        }
        if (item instanceof PsychoactiveItem psychoactive) {
            SubstanceProfile profile = psychoactive.profile();
            return entry(KIND_PROFILE_UNITS, profile.defaultAbstractDoseUnits(), profile.id());
        }

        return Optional.empty();
    }

    public static ItemStack createStack(List<Entry> entries) {
        List<Entry> merged = merge(entries);
        if (merged.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(ModItems.BLEND_CAPSULE.get());
        CompoundTag root = new CompoundTag();
        ListTag storedEntries = new ListTag();
        for (Entry entry : merged) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString(KIND_KEY, entry.kind());
            entryTag.putDouble(AMOUNT_KEY, entry.amount());
            if (entry.kind().equals(KIND_HO_MET_MILLIGRAMS) || entry.kind().equals(KIND_AL_LAD_MILLIGRAMS)
                    || entry.kind().equals(KIND_TWO_CE_MILLIGRAMS)) {
                entryTag.putDouble(EFFECT_AMOUNT_KEY, entry.effectAmount());
            }
            if (!entry.profileId().isEmpty()) {
                entryTag.putString(PROFILE_KEY, entry.profileId());
            }
            storedEntries.add(entryTag);
        }
        root.put(ENTRIES_KEY, storedEntries);

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(ROOT_KEY, root));
        return stack;
    }

    public static List<Entry> read(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) {
            return List.of();
        }

        CompoundTag customTag = customData.copyTag();
        if (!com.trippycraft.compat.NbtCompat.hasCompound(customTag, ROOT_KEY)) {
            return List.of();
        }
        CompoundTag root = customTag.getCompound(ROOT_KEY);

        ListTag storedEntries = com.trippycraft.compat.NbtCompat.getListOrEmpty(root, ENTRIES_KEY);
        List<Entry> entries = new ArrayList<>();
        for (Tag tag : storedEntries) {
            if (!(tag instanceof CompoundTag entryTag)) {
                continue;
            }
            String kind = com.trippycraft.compat.NbtCompat.getStringOr(entryTag, KIND_KEY, "");
            double amount = com.trippycraft.compat.NbtCompat.getDoubleOr(entryTag, AMOUNT_KEY, 0.0D);
            // Capsules saved before the format change already contain effect units.
            double effectAmount = (kind.equals(KIND_HO_MET_MILLIGRAMS) || kind.equals(KIND_AL_LAD_MILLIGRAMS)
                    || kind.equals(KIND_TWO_CE_MILLIGRAMS))
                    ? com.trippycraft.compat.NbtCompat.getDoubleOr(entryTag, EFFECT_AMOUNT_KEY, amount) : amount;
            if (kind.equals(KIND_TWO_CE_MILLIGRAMS) && !entryTag.contains(EFFECT_AMOUNT_KEY)) {
                amount = com.trippycraft.substance.twoce.TwoCeSession.contentMilligramsForEffect(effectAmount);
            }
            String profileId = com.trippycraft.compat.NbtCompat.getStringOr(entryTag, PROFILE_KEY, "");
            if (!kind.isEmpty() && amount > 0.0D) {
                entries.add(new Entry(kind, amount, profileId, effectAmount));
            }
        }
        return merge(entries);
    }

    public static boolean hasBlend(ItemStack stack) {
        return !read(stack).isEmpty();
    }

    public static int apply(ItemStack stack, ServerPlayer player) {
        int applied = 0;
        for (Entry entry : read(stack)) {
            if (!player.isAlive()) {
                break;
            }
            if (entry.apply(player)) {
                applied++;
            }
        }
        return applied;
    }

    public static Component displayName(Entry entry) {
        return switch (entry.kind()) {
            case KIND_PSILOCYBIN_GRAMS -> Component.translatable(SubstanceProfiles.PSILOCYBIN.translationKey());
            case KIND_LSD_MICROGRAMS -> Component.translatable(SubstanceProfiles.LSD.translationKey());
            case KIND_DMT_UNITS -> Component.translatable(SubstanceProfiles.DMT.translationKey());
            case KIND_CANNABIS_MILLIGRAMS -> Component.translatable(SubstanceProfiles.CANNABIS.translationKey());
            case KIND_MDMA_MILLIGRAMS -> Component.translatable(SubstanceProfiles.MDMA.translationKey());
            case KIND_TWO_CB_MILLIGRAMS -> Component.translatable(SubstanceProfiles.TWO_CB.translationKey());
            case KIND_HO_MET_MILLIGRAMS -> Component.translatable(SubstanceProfiles.HO_MET.translationKey());
            case KIND_AL_LAD_MICROGRAMS, KIND_AL_LAD_MILLIGRAMS -> Component.translatable(SubstanceProfiles.AL_LAD.translationKey());
            case KIND_TWO_CE_MILLIGRAMS -> Component.translatable(SubstanceProfiles.TWO_CE.translationKey());
            case KIND_NITROUS_HITS -> Component.translatable(SubstanceProfiles.NITROUS.translationKey());
            case KIND_MESCALINE_MILLIGRAMS -> Component.translatable(SubstanceProfiles.MESCALINE.translationKey());
            case KIND_KOKA_GRAMS -> Component.translatable("item.trippycraft.koka_leaves");
            case KIND_COCAINE_POWDER_GRAMS -> Component.translatable(SubstanceProfiles.COCAINE_POWDER.translationKey());
            case KIND_CRACK_COCAINE_GRAMS -> Component.translatable(SubstanceProfiles.CRACK_COCAINE.translationKey());
            case KIND_HEROIN_MILLIGRAMS -> Component.translatable(SubstanceProfiles.HEROIN.translationKey());
            case KIND_KETAMINE_MILLIGRAMS -> Component.translatable(SubstanceProfiles.KETAMINE.translationKey());
            case KIND_SALVIA_UNITS -> Component.translatable(SubstanceProfiles.SALVIA.translationKey());
            case KIND_ALCOHOL_STANDARD_DRINKS -> Component.translatable(SubstanceProfiles.ALCOHOL.translationKey());
            case KIND_PROFILE_UNITS -> SubstanceProfiles.byId(entry.profileId())
                    .map(profile -> Component.translatable(profile.translationKey()))
                    .orElseGet(() -> Component.literal(entry.profileId()));
            default -> Component.literal(entry.kind());
        };
    }

    public static String doseLabel(Entry entry) {
        return switch (entry.kind()) {
            case KIND_PSILOCYBIN_GRAMS, KIND_KOKA_GRAMS, KIND_COCAINE_POWDER_GRAMS, KIND_CRACK_COCAINE_GRAMS ->
                    String.format(Locale.ROOT, "%.2fg", entry.amount());
            case KIND_LSD_MICROGRAMS, KIND_AL_LAD_MICROGRAMS -> String.format(Locale.ROOT, "%.0fug", entry.amount());
            case KIND_DMT_UNITS -> String.format(Locale.ROOT, "%.1fx", entry.amount());
            case KIND_CANNABIS_MILLIGRAMS, KIND_MDMA_MILLIGRAMS, KIND_TWO_CB_MILLIGRAMS, KIND_TWO_CE_MILLIGRAMS, KIND_HO_MET_MILLIGRAMS, KIND_AL_LAD_MILLIGRAMS, KIND_MESCALINE_MILLIGRAMS, KIND_HEROIN_MILLIGRAMS, KIND_KETAMINE_MILLIGRAMS ->
                    String.format(Locale.ROOT, "%.0fmg", entry.amount());
            case KIND_SALVIA_UNITS, KIND_NITROUS_HITS -> String.format(Locale.ROOT, "%.1fx", entry.amount());
            case KIND_ALCOHOL_STANDARD_DRINKS -> String.format(Locale.ROOT, "%.2f standard drink units", entry.amount());
            case KIND_PROFILE_UNITS -> String.format(Locale.ROOT, "%.2f abstract units", entry.amount());
            default -> String.format(Locale.ROOT, "%.2f", entry.amount());
        };
    }

    private static Optional<Entry> entry(String kind, double amount) {
        return entry(kind, amount, "");
    }

    private static Optional<Entry> entry(String kind, double amount, String profileId) {
        if (amount <= 0.0D) {
            return Optional.empty();
        }
        return Optional.of(new Entry(kind, amount, profileId));
    }

    private static List<Entry> merge(List<Entry> entries) {
        Map<EntryKey, Entry> amounts = new LinkedHashMap<>();
        for (Entry entry : entries) {
            EntryKey key = new EntryKey(entry.kind(), entry.profileId());
            amounts.merge(key, entry, (first, second) -> new Entry(key.kind(),
                    first.amount() + second.amount(), key.profileId(), first.effectAmount() + second.effectAmount()));
        }

        List<Entry> merged = new ArrayList<>();
        amounts.forEach((key, entry) -> {
            if (entry.amount() > 0.0D) {
                merged.add(entry);
            }
        });
        return List.copyOf(merged);
    }

    public record Entry(String kind, double amount, String profileId, double effectAmount) {
        public Entry(String kind, double amount, String profileId) {
            this(kind, amount, profileId, amount);
        }

        private boolean apply(ServerPlayer player) {
            switch (kind) {
                case KIND_PSILOCYBIN_GRAMS -> SubstanceEngine.addPsilocybinDose(player, amount);
                case KIND_LSD_MICROGRAMS -> SubstanceEngine.addLsdDose(player, amount);
                case KIND_DMT_UNITS -> SubstanceEngine.addDmtDose(player, amount);
                case KIND_CANNABIS_MILLIGRAMS -> SubstanceEngine.addCannabisDose(player, amount);
                case KIND_MDMA_MILLIGRAMS -> SubstanceEngine.addMdmaDose(player, amount);
                case KIND_TWO_CB_MILLIGRAMS -> SubstanceEngine.addTwoCbDose(player, amount);
                case KIND_HO_MET_MILLIGRAMS -> SubstanceEngine.addHoMetDose(player, effectAmount, amount);
                case KIND_AL_LAD_MICROGRAMS -> SubstanceEngine.addAlLadDose(player, amount);
                case KIND_AL_LAD_MILLIGRAMS -> SubstanceEngine.addAlLadDose(player, effectAmount, amount);
                case KIND_TWO_CE_MILLIGRAMS -> SubstanceEngine.addTwoCeDose(player, effectAmount, amount);
                case KIND_NITROUS_HITS -> SubstanceEngine.addNitrousDose(player, amount);
                case KIND_MESCALINE_MILLIGRAMS -> SubstanceEngine.addMescalineDose(player, amount);
                case KIND_KOKA_GRAMS -> SubstanceEngine.addKokaDose(player, amount);
                case KIND_COCAINE_POWDER_GRAMS -> SubstanceEngine.addCocaineDose(player, CocaineRoute.POWDER, amount);
                case KIND_CRACK_COCAINE_GRAMS -> SubstanceEngine.addCocaineDose(player, CocaineRoute.CRACK, amount);
                case KIND_HEROIN_MILLIGRAMS -> SubstanceEngine.addHeroinDose(player, amount);
                case KIND_KETAMINE_MILLIGRAMS -> SubstanceEngine.addKetamineDose(player, amount);
                // Legacy capsules must not bypass the bong-only salvia route.
                case KIND_SALVIA_UNITS -> {
                    return false;
                }
                case KIND_ALCOHOL_STANDARD_DRINKS -> SubstanceEngine.addAlcoholDose(player, amount);
                case KIND_PROFILE_UNITS -> {
                    Optional<SubstanceProfile> profile = SubstanceProfiles.byId(profileId);
                    if (profile.isEmpty() || profile.get() == SubstanceProfiles.SALVIA) {
                        return false;
                    }
                    SubstanceEngine.addExposure(player, profile.get(), amount);
                }
                default -> {
                    return false;
                }
            }
            return true;
        }
    }

    private record EntryKey(String kind, String profileId) {
    }
}
