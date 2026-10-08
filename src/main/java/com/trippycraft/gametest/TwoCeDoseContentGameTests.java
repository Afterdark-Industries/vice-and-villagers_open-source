package com.trippycraft.gametest;

import com.trippycraft.TrippyCraft;
import com.trippycraft.economy.DealerProduct;
import com.trippycraft.economy.DealerRegistries;
import com.trippycraft.economy.MarketRegistries;
import com.trippycraft.item.CapsuleBlend;
import com.trippycraft.item.TwoCeCrystalItem;
import com.trippycraft.item.TwoCeDoseItem;
import com.trippycraft.network.ExperienceSnapshotPacket;
import com.trippycraft.crafting.CapsuleBlendRecipe;
import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.twoce.TwoCeEffectState;
import com.trippycraft.substance.twoce.TwoCeSession;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TwoCeDoseContentGameTests {
    private TwoCeDoseContentGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void allFormatsKeepPhysicalContentSeparateFromTheirExistingEffects(GameTestHelper helper) {
        for (Tier tier : tiers()) {
            for (Item item : List.of(tier.current(), tier.legacy())) {
                helper.assertTrue(item instanceof TwoCeDoseItem, "A current or legacy 2C-E format lost its native item behavior");
                TwoCeDoseItem tablet = (TwoCeDoseItem) item;
                helper.assertTrue(tablet.contentMilligrams() == tier.content() && tablet.doseMilligrams() == tier.effect(),
                        "The item format changed physical content or its preserved effect calibration");
            }
            PsychoactiveExperience current = new PsychoactiveExperience();
            PsychoactiveExperience legacy = new PsychoactiveExperience();
            current.addTwoCeDose(null, tier.effect(), tier.content());
            legacy.addTwoCeDose(null, tier.effect());
            assertContent(helper, current.twoCeState(), tier.content(), tier.content(),
                    "Fresh 2C-E format reported its effect calibration instead of physical milligrams");
            helper.assertTrue(current.twoCeState().targetDoseMilligrams() == tier.effect(),
                    "Changing the content format increased the established 2C-E effect tier");
            int age = current.twoCeState().durationTicks() / 2;
            helper.assertTrue(current.scrubPsilocybin(age) && legacy.scrubPsilocybin(age),
                    "Could not compare 2C-E sessions at the same progression");
            helper.assertTrue(current.twoCeState().intensity() > 0, "The comparison never reached visible 2C-E effects");
            assertSameEffects(helper, current.twoCeState(), legacy.twoCeState());
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void equalEffectTotalsKeepDifferentConsumedContentTotals(GameTestHelper helper) {
        PsychoactiveExperience mixed = new PsychoactiveExperience();
        mixed.addTwoCeDose(null, 10, 25);
        mixed.addTwoCeDose(null, 20, 100);
        PsychoactiveExperience small = new PsychoactiveExperience();
        for (int dose = 0; dose < 3; dose++) small.addTwoCeDose(null, 10, 25);
        helper.assertTrue(mixed.twoCeState().targetDoseMilligrams() == 30 && small.twoCeState().targetDoseMilligrams() == 30,
                "The comparison did not preserve equal calibrated effect totals");
        helper.assertTrue(mixed.twoCeState().targetContentMilligrams() == 125 && small.twoCeState().targetContentMilligrams() == 75,
                "2C-E content was inferred from the accumulated effect total instead of consumed item amounts");
        helper.assertTrue(mixed.twoCeState().displayContentMilligrams() == 25,
                "Redosing abruptly replaced the display rather than keeping its existing smooth transition");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void crystalPortionsAccumulateOneThousandMilligramsAtPreservedCalibration(GameTestHelper helper) {
        helper.assertTrue(TwoCeCrystalItem.EFFECT_DOSE_PER_USE == 160,
                "A crystal portion must retain two heavy tablets' combined effect calibration");
        PsychoactiveExperience experience = new PsychoactiveExperience();
        for (int portion = 1; portion <= 5; portion++) {
            experience.addTwoCeDose(null, TwoCeCrystalItem.EFFECT_DOSE_PER_USE, 1000);
            helper.assertTrue(experience.twoCeState().targetContentMilligrams() == portion * 1000
                            && experience.twoCeState().targetDoseMilligrams() == portion * 160,
                    "Crystal portions confused physical content and calibrated effects");
            if (portion == 1) assertContent(helper, experience.twoCeState(), 1000, 1000,
                    "A fresh crystal portion did not display its actual 1000mg content");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void savedSessionsAndSnapshotCodecPreservePhysicalAndCalibratedAmounts(GameTestHelper helper) {
        PsychoactiveExperience experience = new PsychoactiveExperience();
        experience.addTwoCeDose(null, 10, 25);
        experience.addTwoCeDose(null, 20, 100);
        helper.assertTrue(experience.scrubPsilocybin(1500), "Could not progress the 2C-E session before saving");
        experience.addHoMetDose(null, 5, 25);
        experience.addAlLadDose(null, 75, 25);
        experience.addNitrousDose(null, 1);
        PsychoactiveExperience restored = PsychoactiveExperience.load(experience.save());
        helper.assertTrue(restored.twoCeState().equals(experience.twoCeState()),
                "NBT persistence changed 2C-E physical content, calibration, or visual progression");
        ExperienceSnapshotPacket snapshot = ExperienceSnapshotPacket.from(restored);
        ExperienceSnapshotPacket decoded = roundTrip(snapshot, helper);
        helper.assertTrue(decoded.twoCe().equals(snapshot.twoCe()), "Snapshot synchronization changed the 2C-E session state");
        helper.assertTrue(decoded.twoCe().targetContentMilligrams() == 125 && decoded.twoCe().targetDoseMilligrams() == 30,
                "The client snapshot contains the wrong 2C-E content/effect pair");
        helper.assertTrue(decoded.hoMet().equals(snapshot.hoMet()) && decoded.alLad().equals(snapshot.alLad())
                        && decoded.nitrous().equals(snapshot.nitrous()) && decoded.heroin().equals(snapshot.heroin()),
                "Expanded 2C-E fields misaligned preceding or following snapshot states");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void mixtureScalingChangesEffectsWithoutChangingPhysicalContent(GameTestHelper helper) {
        PsychoactiveExperience experience = new PsychoactiveExperience();
        experience.addTwoCeDose(null, 20, 100);
        experience.scrubPsilocybin(experience.twoCeState().durationTicks() / 2);
        TwoCeEffectState state = experience.twoCeState();
        for (double multiplier : new double[]{0, 0.5, 2}) {
            TwoCeEffectState scaled = state.scaled(multiplier);
            assertContent(helper, scaled, state.displayContentMilligrams(), state.targetContentMilligrams(),
                    "Mixture scaling changed recorded or displayed 2C-E content");
            helper.assertTrue(state.intensity() > 0 && scaled.intensity() == state.intensity() * multiplier,
                    "The scaling test did not exercise actual changes in 2C-E perception");
            helper.assertTrue(scaled.targetDoseMilligrams() == state.targetDoseMilligrams()
                            && scaled.durationTicks() == state.durationTicks(),
                    "Mixture scaling changed accumulated calibration or the trip clock");
        }
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void clearingExpiryAndRestartDoNotRetainPreviousPhysicalContent(GameTestHelper helper) {
        PsychoactiveExperience experience = new PsychoactiveExperience();
        assertContent(helper, experience.twoCeState(), 0, 0, "Inactive 2C-E retained physical content");
        experience.addTwoCeDose(null, 160, 1000);
        helper.assertTrue(experience.endActiveTripFromSleep(), "Sleep did not clear the active 2C-E session");
        assertContent(helper, experience.twoCeState(), 0, 0, "Sleep retained the previous trip's content");
        assertContent(helper, roundTrip(ExperienceSnapshotPacket.clear(), helper).twoCe(), 0, 0,
                "The clear snapshot retained old 2C-E content");
        experience.addTwoCeDose(null, 10, 25);
        assertContent(helper, experience.twoCeState(), 25, 25, "A new session carried content from the cleared trip");
        experience.scrubPsilocybin(experience.twoCeState().durationTicks());
        assertContent(helper, experience.twoCeState(), 0, 0, "An expired 2C-E session reported old consumed content");
        experience.addTwoCeDose(null, 20, 100);
        assertContent(helper, experience.twoCeState(), 100, 100, "Restarting an expired session retained the previous amount");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void legacySaveMigrationInfersSingleFormatsWithoutOverwritingRecordedContent(GameTestHelper helper) {
        for (Tier tier : tiers()) {
            PsychoactiveExperience old = new PsychoactiveExperience();
            old.addTwoCeDose(null, tier.effect());
            CompoundTag saved = old.save();
            removeContentFields(saved);
            PsychoactiveExperience restored = PsychoactiveExperience.load(saved);
            assertContent(helper, restored.twoCeState(), tier.content(), tier.content(),
                    "A legacy single-format save did not infer its matching physical content");
            helper.assertTrue(restored.twoCeState().targetDoseMilligrams() == tier.effect(),
                    "Legacy physical-content migration changed the original calibrated dose");
        }
        PsychoactiveExperience mixed = new PsychoactiveExperience();
        mixed.addTwoCeDose(null, 10, 25);
        mixed.addTwoCeDose(null, 20, 100);
        helper.assertTrue(PsychoactiveExperience.load(mixed.save()).twoCeState().targetContentMilligrams() == 125,
                "An exact saved total was replaced with an estimate from effect calibration");
        CompoundTag oldStacked = mixed.save();
        removeContentFields(oldStacked);
        TwoCeEffectState inferred = PsychoactiveExperience.load(oldStacked).twoCeState();
        // Missing ingredient history permits only an approximation of an old accumulated dose.
        helper.assertTrue(inferred.targetContentMilligrams() == 175 && inferred.targetDoseMilligrams() == 30,
                "Legacy stacked-content fallback changed calibration or failed to use its documented approximation");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void invalidContentAndFiniteOverflowNeverPoisonThePhysicalLedger(GameTestHelper helper) {
        for (double invalid : new double[]{0, -10, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            PsychoactiveExperience experience = new PsychoactiveExperience();
            experience.addTwoCeDose(null, 10, invalid);
            assertContent(helper, experience.twoCeState(), 0, 0, "Invalid physical content entered the 2C-E ledger");
            helper.assertTrue(experience.twoCeState().targetDoseMilligrams() == 10,
                    "Sanitizing physical content changed a valid calibrated effect");
            experience.addTwoCeDose(null, 20, 100);
            helper.assertTrue(experience.twoCeState().targetContentMilligrams() == 100,
                    "Invalid content poisoned a subsequent valid dose");
        }
        PsychoactiveExperience ignored = new PsychoactiveExperience();
        ignored.addTwoCeDose(null, 10, 25);
        for (double invalidEffect : new double[]{0, -10, Double.NaN}) ignored.addTwoCeDose(null, invalidEffect, 1000);
        assertContent(helper, ignored.twoCeState(), 25, 25, "A rejected effect dose added phantom physical content");

        TwoCeSession extreme = TwoCeSession.start(null, 10, Double.MAX_VALUE);
        extreme.stackDose(20, Double.MAX_VALUE);
        helper.assertTrue(extreme.effectState().targetContentMilligrams() == Double.MAX_VALUE,
                "Finite physical content overflowed instead of saturating at the largest representable amount");
        for (int tick = 0; tick < 200; tick++) {
            extreme.tick(null);
            helper.assertTrue(Double.isFinite(extreme.effectState().displayContentMilligrams()),
                    "Physical-content display interpolation overflowed");
        }
        helper.assertTrue(extreme.effectState().targetDoseMilligrams() == 30,
                "Huge physical content increased the established effect contribution");
        helper.assertTrue(TwoCeSession.load(extreme.save()).effectState().targetContentMilligrams() == Double.MAX_VALUE,
                "Huge finite content failed persistence");
        helper.assertTrue(TwoCeSession.contentMilligramsForEffect(Double.MAX_VALUE) == Double.MAX_VALUE,
                "Inferring huge legacy effect content overflowed or lost the amount");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void capsulesPersistSeparateContentAndEffectsForNewAndLegacyIngredients(GameTestHelper helper) {
        CapsuleBlendRecipe recipe = new CapsuleBlendRecipe(CraftingBookCategory.MISC);
        CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(ModItems.EMPTY_CAPSULE.get()),
                new ItemStack(ModItems.TWO_CE_25MG.get(), 3), new ItemStack(ModItems.TWO_CE_100MG.get())));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "New 2C-E formats were rejected by capsule crafting");
        ItemStack capsule = recipe.assemble(input, helper.getLevel().registryAccess());
        CapsuleBlend.Entry dose = onlyEntry(helper, capsule);
        helper.assertTrue(dose.kind().equals("two_ce_milligrams") && dose.amount() == 125 && dose.effectAmount() == 30,
                "Capsule crafting changed the 125mg content / 30-unit effect pair");
        helper.assertTrue(CapsuleBlend.doseLabel(dose).equals("125mg"), "The capsule label displayed calibrated effects as content");
        helper.assertTrue(input.getItem(1).getCount() == 3, "Crafting preview consumed or counted the whole ingredient stack");
        helper.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty),
                "2C-E tablets returned an unintended container");
        CompoundTag data = capsule.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag encoded = data.getCompound("TrippyCraftCapsuleBlend").getList("Entries", 10).getCompound(0);
        helper.assertTrue(encoded.getDouble("Amount") == 125 && encoded.getDouble("EffectAmount") == 30,
                "Capsule NBT did not separately serialize physical content and effects");

        CapsuleBlend.Entry legacy = onlyEntry(helper, legacyCapsule(20));
        helper.assertTrue(legacy.amount() == 100 && legacy.effectAmount() == 20,
                "An old single-format capsule lost its original effect or matching inferred content");
        CapsuleBlend.Entry added = CapsuleBlend.entryFromDoseItem(new ItemStack(ModItems.TWO_CE_25MG.get())).orElseThrow();
        ItemStack merged = CapsuleBlend.createStack(List.of(legacy, added));
        CapsuleBlend.Entry mixed = onlyEntry(helper, merged);
        helper.assertTrue(mixed.amount() == 125 && mixed.effectAmount() == 30,
                "Combining migrated legacy and current capsule ingredients changed their content/effect pair");
        CapsuleBlend.Entry ambiguous = onlyEntry(helper, legacyCapsule(30));
        helper.assertTrue(ambiguous.amount() == 175 && ambiguous.effectAmount() == 30,
                "An old accumulated capsule did not preserve effects while approximating missing content");
        helper.succeed();
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void savedDealerAliasesResolveOnlyToMatchingCurrentFormats(GameTestHelper helper) {
        List<Tier> tiers = tiers();
        List<DealerProduct> products = DealerRegistries.productsInCategory(MarketRegistries.CATEGORY_TWO_CE);
        helper.assertTrue(products.stream().map(DealerProduct::id).toList().equals(tiers.stream().map(Tier::currentId).toList()),
                "The dealer catalog did not contain exactly the four current 2C-E formats in tier order");
        for (int rank = 0; rank < tiers.size(); rank++) {
            Tier tier = tiers.get(rank);
            DealerProduct product = DealerRegistries.product(tier.currentId()).orElseThrow();
            helper.assertTrue(DealerRegistries.product(tier.legacyId()).orElseThrow() == product
                            && product.item().get() == tier.current() && product.emeraldPricePerUnit() == tier.price()
                            && product.unlockLevel() == rank,
                    "Legacy saved offers changed the format, price, or demand tier");
            helper.assertTrue(DealerRegistries.productFor(new ItemStack(tier.legacy())).orElseThrow() == product
                            && DealerRegistries.productFor(new ItemStack(tier.current())).orElseThrow() == product,
                    "Legacy and current dealer samples resolved to different products");
            helper.assertTrue(DealerRegistries.matchesProductItem(new ItemStack(tier.legacy()), tier.current()),
                    "A saved legacy tablet was rejected as its matching current product");
            for (Tier other : tiers) {
                if (other != tier) helper.assertFalse(DealerRegistries.matchesProductItem(new ItemStack(tier.legacy()), other.current()),
                        "Legacy 2C-E stock matched the wrong current tier");
            }
        }
        helper.assertTrue(DealerRegistries.productFor(new ItemStack(ModItems.TWO_CE_CRYSTAL.get())).isEmpty(),
                "A production crystal was accepted as a finished dealer tablet");
        helper.succeed();
    }

    private static List<Tier> tiers() {
        return List.of(new Tier("two_ce_25mg", ModItems.TWO_CE_25MG.get(), "two_ce_10mg", ModItems.TWO_CE_10MG.get(), 25, 10, 16),
                new Tier("two_ce_100mg", ModItems.TWO_CE_100MG.get(), "two_ce_20mg", ModItems.TWO_CE_20MG.get(), 100, 20, 32),
                new Tier("two_ce_250mg", ModItems.TWO_CE_250MG.get(), "two_ce_40mg", ModItems.TWO_CE_40MG.get(), 250, 40, 58),
                new Tier("two_ce_500mg", ModItems.TWO_CE_500MG.get(), "two_ce_80mg", ModItems.TWO_CE_80MG.get(), 500, 80, 88));
    }

    private static void assertContent(GameTestHelper helper, TwoCeEffectState state, double display, double target, String message) {
        helper.assertTrue(state.displayContentMilligrams() == display && state.targetContentMilligrams() == target, message);
    }

    private static void assertSameEffects(GameTestHelper helper, TwoCeEffectState current, TwoCeEffectState legacy) {
        helper.assertTrue(current.active() == legacy.active()
                        && current.displayDoseMilligrams() == legacy.displayDoseMilligrams()
                        && current.activeDoseMilligrams() == legacy.activeDoseMilligrams()
                        && current.targetDoseMilligrams() == legacy.targetDoseMilligrams()
                        && current.ageTicks() == legacy.ageTicks() && current.durationTicks() == legacy.durationTicks()
                        && current.remainingTicks() == legacy.remainingTicks() && current.phase() == legacy.phase()
                        && current.progress() == legacy.progress() && current.tier() == legacy.tier()
                        && current.repeatedDoses() == legacy.repeatedDoses() && current.acuteTolerance() == legacy.acuteTolerance()
                        && current.intensity() == legacy.intensity() && current.visualPresence() == legacy.visualPresence()
                        && current.cavities() == legacy.cavities() && current.nestedRooms() == legacy.nestedRooms()
                        && current.tessellation() == legacy.tessellation() && current.edgeLock() == legacy.edgeLock()
                        && current.perspectiveFold() == legacy.perspectiveFold() && current.recursion() == legacy.recursion()
                        && current.corridorZoom() == legacy.corridorZoom() && current.symmetryLock() == legacy.symmetryLock()
                        && current.geometricTrails() == legacy.geometricTrails() && current.afterglow() == legacy.afterglow()
                        && current.bodyLoad() == legacy.bodyLoad() && current.signature().equals(legacy.signature())
                        && current.environment().equals(legacy.environment()),
                "Physical 2C-E content changed preserved timing, calibration, tolerance, or visual output");
    }

    private static ExperienceSnapshotPacket roundTrip(ExperienceSnapshotPacket packet, GameTestHelper helper) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ExperienceSnapshotPacket.encode(packet, buffer);
            ExperienceSnapshotPacket result = ExperienceSnapshotPacket.decode(buffer);
            helper.assertTrue(buffer.readableBytes() == 0, "Expanded 2C-E snapshot left unread or misaligned fields");
            return result;
        } finally {
            buffer.release();
        }
    }

    private static void removeContentFields(CompoundTag saved) {
        CompoundTag session = saved.getCompound("two_ce");
        session.remove("targetContentMilligrams");
        session.remove("displayedContentMilligrams");
        saved.put("two_ce", session);
    }

    private static CapsuleBlend.Entry onlyEntry(GameTestHelper helper, ItemStack capsule) {
        ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(),
                (CompoundTag) capsule.save(helper.getLevel().registryAccess()));
        List<CapsuleBlend.Entry> entries = CapsuleBlend.read(restored);
        helper.assertTrue(entries.size() == 1, "A saved 2C-E capsule failed to retain one merged entry");
        return entries.getFirst();
    }

    private static ItemStack legacyCapsule(double effectAmount) {
        ItemStack result = new ItemStack(ModItems.BLEND_CAPSULE.get());
        CompoundTag entry = new CompoundTag();
        entry.putString("Kind", "two_ce_milligrams");
        entry.putDouble("Amount", effectAmount);
        ListTag entries = new ListTag();
        entries.add(entry);
        CompoundTag root = new CompoundTag();
        root.put("Entries", entries);
        CustomData.update(DataComponents.CUSTOM_DATA, result, tag -> tag.put("TrippyCraftCapsuleBlend", root));
        return result;
    }

    private record Tier(String currentId, Item current, String legacyId, Item legacy, int content, double effect, int price) {
    }
}
