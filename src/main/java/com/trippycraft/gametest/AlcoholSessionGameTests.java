package com.trippycraft.gametest;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.PsychoactiveExperience;
import com.trippycraft.substance.VanillaEffectBridge;
import com.trippycraft.substance.alcohol.AlcoholDoseCurve;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholPhase;
import com.trippycraft.substance.alcohol.AlcoholRecoveryTracker;
import com.trippycraft.substance.alcohol.AlcoholSession;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.trippycraft.gametest.SessionTestSupport.TEMPLATE;
import static com.trippycraft.gametest.SessionTestSupport.TestPlayer;
import static com.trippycraft.gametest.SessionTestSupport.ageAt;
import static com.trippycraft.gametest.SessionTestSupport.aged;
import static com.trippycraft.gametest.SessionTestSupport.assertClose;
import static com.trippycraft.gametest.SessionTestSupport.damageablePlayer;
import static com.trippycraft.gametest.SessionTestSupport.engineExperience;
import static com.trippycraft.gametest.SessionTestSupport.experienceWith;
import static com.trippycraft.gametest.SessionTestSupport.fakePlayer;
import static com.trippycraft.gametest.SessionTestSupport.run;
import static com.trippycraft.gametest.SessionTestSupport.runAtBridgeTick;
import static com.trippycraft.gametest.SessionTestSupport.tick;
import static com.trippycraft.gametest.SessionTestSupport.tracked;

/** Full-session contracts for alcohol: phases, stacking, saves, items, status effects and the recovery tracker. */
@GameTestHolder(SessionTestSupport.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class AlcoholSessionGameTests {
    private AlcoholSessionGameTests() { }

    // ---- Session lifecycle -------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void alcoholPhasesRunInOrderThenTheSessionEndsSober(GameTestHelper helper) {
        run(helper, () -> {
            AlcoholSession session = AlcoholSession.start(6.0D);
            CompoundTag saved = session.save();
            int duration = saved.getInt("durationTicks");
            List<AlcoholPhase> seen = new ArrayList<>();
            for (int age = 0; age < duration; age += Math.max(1, duration / 400)) {
                AlcoholPhase phase = AlcoholSession.load(aged(saved, age)).phase();
                if (seen.isEmpty() || seen.get(seen.size() - 1) != phase) seen.add(phase);
            }
            helper.assertTrue(seen.equals(List.of(AlcoholPhase.ONSET, AlcoholPhase.BUZZ, AlcoholPhase.INTOXICATED,
                            AlcoholPhase.HEAVY, AlcoholPhase.COMEDOWN, AlcoholPhase.AFTERGLOW)),
                    "Alcohol phases ran out of order: " + seen);
            AlcoholSession ended = AlcoholSession.load(aged(saved, duration));
            helper.assertTrue(!ended.isAlive() && ended.phase() == AlcoholPhase.SOBER && ended.remainingTicks() == 0,
                    "An alcohol session at its full duration was still active");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void moreDrinksLastLongerAndHitHarderUpToTheirCaps(GameTestHelper helper) {
        run(helper, () -> {
            double[] drinks = {0.5D, 1.0D, 3.0D, 6.0D, 10.0D};
            for (int i = 1; i < drinks.length; i++) {
                helper.assertTrue(AlcoholDoseCurve.baseDurationTicks(drinks[i]) > AlcoholDoseCurve.baseDurationTicks(drinks[i - 1]),
                        "Duration did not grow from " + drinks[i - 1] + " to " + drinks[i] + " drinks");
            }
            helper.assertTrue(AlcoholDoseCurve.baseDurationTicks(10.0D) == AlcoholDoseCurve.baseDurationTicks(24.0D),
                    "Base duration should stop growing beyond ten standard drinks");
            double[] intensityDrinks = {0.5D, 1.5D, 3.0D, 6.0D, 10.0D, 16.0D};
            for (int i = 1; i < intensityDrinks.length; i++) {
                helper.assertTrue(AlcoholDoseCurve.peakIntensity(intensityDrinks[i]) > AlcoholDoseCurve.peakIntensity(intensityDrinks[i - 1]),
                        "Peak intensity did not grow from " + intensityDrinks[i - 1] + " to " + intensityDrinks[i] + " drinks");
            }
            helper.assertTrue(AlcoholDoseCurve.peakIntensity(40.0D) <= 6.0D, "Peak intensity exceeded its 6.0 ceiling");
            int[] expectedTiers = {0, 1, 2, 3, 4, 5};
            double[] tierDrinks = {0.0D, 0.5D, 1.0D, 2.0D, 4.0D, 6.0D};
            for (int i = 0; i < tierDrinks.length; i++) {
                helper.assertTrue(AlcoholDoseCurve.tier(tierDrinks[i]) == expectedTiers[i],
                        tierDrinks[i] + " drinks should be tier " + expectedTiers[i]);
            }
            assertClose(helper, 1.0D, AlcoholDoseCurve.standardDrinks(12.0D, 0.05D), 1.0E-9,
                    "A 12 oz 5% beer should be one standard drink");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void stackingRaisesTheTargetGraduallyAndNeverShortensTheSession(GameTestHelper helper) {
        run(helper, () -> {
            AlcoholSession session = AlcoholSession.start(2.0D);
            for (int i = 0; i < 100; i++) session.tick();
            int remainingBefore = session.remainingTicks();
            session.stackDose(3.0D);
            AlcoholEffectState stacked = session.effectState();
            assertClose(helper, 5.0D, stacked.targetStandardDrinks(), 1.0E-9, "Stacked drinks did not add to the target");
            helper.assertTrue(stacked.activeStandardDrinks() < 2.5D, "A new drink took effect instantly instead of absorbing");
            helper.assertTrue(session.remainingTicks() >= remainingBefore, "Another drink shortened the session");

            for (int i = 0; i < 1500; i++) session.tick();
            assertClose(helper, 5.0D, session.effectState().activeStandardDrinks(), 1.0E-9,
                    "Absorbed drinks never reached the stacked target");

            double target = session.effectState().targetStandardDrinks();
            session.stackDose(-4.0D);
            session.stackDose(0.0D);
            assertClose(helper, target, session.effectState().targetStandardDrinks(), 1.0E-9,
                    "Zero or negative drinks changed the target");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void experienceDrinksJoinOneSession(GameTestHelper helper) {
        run(helper, () -> {
            ServerPlayer player = fakePlayer(helper, "alcohol-stack");
            PsychoactiveExperience experience = new PsychoactiveExperience();
            experience.addAlcoholDose(1.0D);
            tick(player, experience, 40);
            int ageBefore = experience.alcoholState().ageTicks();
            experience.addAlcoholDose(1.5D);
            AlcoholEffectState state = experience.alcoholState();
            assertClose(helper, 2.5D, state.targetStandardDrinks(), 1.0E-9, "Second drink did not join the first session");
            helper.assertTrue(state.ageTicks() == ageBefore, "A second drink restarted the session clock");
        });
    }

    // ---- Saves --------------------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void alcoholSaveAndReloadKeepsMidSessionState(GameTestHelper helper) {
        run(helper, () -> {
            AlcoholSession session = AlcoholSession.start(4.0D);
            for (int i = 0; i < 300; i++) session.tick();
            session.stackDose(2.0D);
            for (int i = 0; i < 200; i++) session.tick();
            AlcoholEffectState before = session.effectState();
            AlcoholEffectState after = AlcoholSession.load(session.save()).effectState();
            helper.assertTrue(before.equals(after), "Alcohol state changed across save/load:\n" + before + "\n" + after);

            ServerPlayer player = fakePlayer(helper, "alcohol-save");
            PsychoactiveExperience experience = experienceWith("alcohol", session.save());
            PsychoactiveExperience reloaded = PsychoactiveExperience.load(experience.save());
            tick(player, experience, 1);
            tick(player, reloaded, 1);
            helper.assertTrue(experience.alcoholState().ageTicks() == reloaded.alcoholState().ageTicks()
                            && experience.alcoholState().activeStandardDrinks() == reloaded.alcoholState().activeStandardDrinks(),
                    "Alcohol session diverged after an experience save/load");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void alcoholLoadRepairsOutOfRangeDurationsAndMissingAmounts(GameTestHelper helper) {
        run(helper, () -> {
            double drinks = 5.0D;
            int base = AlcoholDoseCurve.baseDurationTicks(drinks);

            CompoundTag tooShort = AlcoholSession.start(drinks).save();
            tooShort.putInt("durationTicks", 10);
            helper.assertTrue(AlcoholSession.load(tooShort).effectState().durationTicks() == base,
                    "A save shorter than the dose curve was not repaired");

            CompoundTag tooLong = aged(AlcoholSession.start(drinks).save(), 100);
            tooLong.putInt("durationTicks", 10_000_000);
            int cap = base + (int) Math.round((360.0D + drinks * 18.0D) * 20.0D);
            helper.assertTrue(AlcoholSession.load(tooLong).effectState().durationTicks() == cap,
                    "An inflated save was not capped to the stacking limit");

            CompoundTag legacy = new CompoundTag();
            legacy.putDouble("activeStandardDrinks", 3.0D);
            legacy.putInt("durationTicks", AlcoholDoseCurve.baseDurationTicks(3.0D));
            AlcoholEffectState repaired = AlcoholSession.load(legacy).effectState();
            helper.assertTrue(repaired.targetStandardDrinks() == 3.0D && repaired.displayStandardDrinks() == 3.0D,
                    "Missing target/displayed amounts did not default to the active amount");
        });
    }

    // ---- Items and config ---------------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void drinkingABeerStartsASessionAndReturnsTheBottle(GameTestHelper helper) {
        run(helper, () -> {
            ServerPlayer player = tracked(fakePlayer(helper, "alcohol-beer"));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.BEER_BOTTLE.get()));
            ModItems.BEER_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "Drinking a beer did not return its glass bottle");
            AlcoholEffectState state = engineExperience(player).alcoholState();
            helper.assertTrue(state.active(), "Drinking a beer did not start an alcohol session");
            assertClose(helper, 1.0D, state.targetStandardDrinks(), 1.0E-9, "A beer should be one standard drink");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WHISKEY_GLASS.get()));
            ModItems.WHISKEY_GLASS.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            assertClose(helper, 2.0D, engineExperience(player).alcoholState().targetStandardDrinks(), 1.0E-9,
                    "A whiskey after a beer should total two standard drinks in one session");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void disablingAlcoholEffectsIgnoresDrinks(GameTestHelper helper) {
        run(helper, () -> {
            TrippyConfig.enableAlcoholEffects = false;
            ServerPlayer player = tracked(fakePlayer(helper, "alcohol-disabled"));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.VODKA_GLASS.get()));
            ModItems.VODKA_GLASS.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(engineExperience(player).alcoholState().equals(AlcoholEffectState.inactive()),
                    "A drink started a session while alcohol effects were disabled");
        });
    }

    // ---- Vanilla status effects ---------------------------------------------------------------

    @GameTest(template = TEMPLATE)
    public static void intoxicationSlowsAndConfusesWhileOneDrinkOnlyStrengthens(GameTestHelper helper) {
        runAtBridgeTick(helper, () -> {
            ServerPlayer drunk = fakePlayer(helper, "alcohol-drunk");
            CompoundTag heavy = AlcoholSession.start(8.0D).save();
            PsychoactiveExperience drunkExperience = experienceWith("alcohol",
                    aged(heavy, ageAt(heavy.getInt("durationTicks"), 0.45D)));
            tick(drunk, drunkExperience, 1);
            VanillaEffectBridge.applyTick(drunk, drunkExperience);
            helper.assertTrue(drunkExperience.alcoholState().phase() == AlcoholPhase.INTOXICATED,
                    "Fixture was not in the intoxicated phase");
            helper.assertTrue(drunk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Eight drinks did not slow the player");
            helper.assertTrue(drunk.hasEffect(MobEffects.CONFUSION), "Eight drinks did not apply Nausea");

            ServerPlayer tipsy = fakePlayer(helper, "alcohol-tipsy");
            CompoundTag light = AlcoholSession.start(1.5D).save();
            PsychoactiveExperience tipsyExperience = experienceWith("alcohol",
                    aged(light, ageAt(light.getInt("durationTicks"), 0.25D)));
            tick(tipsy, tipsyExperience, 1);
            VanillaEffectBridge.applyTick(tipsy, tipsyExperience);
            helper.assertTrue(tipsy.hasEffect(MobEffects.DAMAGE_BOOST), "One and a half drinks did not grant Strength");
            helper.assertTrue(!tipsy.hasEffect(MobEffects.CONFUSION) && !tipsy.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                    "One and a half drinks already caused Nausea or Slowness");
        });
    }

    // ---- Recovery tracker: hangover, dependence, poisoning and withdrawal ---------------------

    /** Guards the "no damage" contracts below from passing because the test player cannot be hurt. */
    @GameTest(template = TEMPLATE)
    public static void theDamageableTestPlayerReallyTakesDamage(GameTestHelper helper) {
        run(helper, () -> {
            TestPlayer player = damageablePlayer(helper, "alcohol-harness");
            player.hurt(player.damageSources().magic(), 1.0F);
            player.hurt(player.damageSources().magic(), 1.0F);
            helper.assertTrue(player.hits == 2 && player.damageTaken == 2.0F && player.lastDamageWas("magic"),
                    "The damageable test player ignored damage: hits=" + player.hits + " taken=" + player.damageTaken);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void sessionPeakDecidesHangoverAndDependenceGain(GameTestHelper helper) {
        run(helper, () -> {
            ServerPlayer player = fakePlayer(helper, "alcohol-peaks");
            double[] peaks = {2.5D, 3.5D, 5.0D, 9.0D, 18.0D};
            int[] dependence = {0, 1, 1, 3, 5};
            boolean[] queued = {false, false, true, true, true};
            boolean[] felt = {false, false, false, true, true};
            for (int i = 0; i < peaks.length; i++) {
                AlcoholRecoveryTracker.RecoveryState after = finishSession(player, new AlcoholRecoveryTracker(), peaks[i]);
                helper.assertTrue(after.dependencyPhase() == dependence[i],
                        peaks[i] + " drinks should add " + dependence[i] + " dependence, added " + after.dependencyPhase());
                helper.assertTrue((after.hangoverTicks() > 0) == queued[i],
                        peaks[i] + " drinks hangover queued expected " + queued[i]);
                // Just past the four-drink threshold the hangover is queued but too faint to count as active.
                helper.assertTrue(after.hangoverActive() == felt[i],
                        peaks[i] + " drinks hangover felt expected " + felt[i] + ", severity " + after.hangoverSeverity());
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void poisoningCannotKillBelowTwentyDrinksAndAlwaysKillsAtThirty(GameTestHelper helper) {
        run(helper, () -> {
            TestPlayer survivor = damageablePlayer(helper, "alcohol-19");
            AlcoholRecoveryTracker safe = new AlcoholRecoveryTracker();
            safe.tick(survivor, AlcoholSession.start(19.9D).effectState());
            helper.assertTrue(survivor.deathSource == null, "19.9 drinks caused a blackout death");

            TestPlayer victim = damageablePlayer(helper, "alcohol-30");
            AlcoholRecoveryTracker lethal = new AlcoholRecoveryTracker();
            lethal.tick(victim, AlcoholSession.start(30.0D).effectState());
            helper.assertTrue(victim.diedFrom("blackout"), "Thirty drinks did not cause a blackout death");
            // The tracker clears dependence and hangover at once; the session itself is cleared when the
            // player respawns (clearAlcoholAfterDeath on clone).
            AlcoholRecoveryTracker.RecoveryState reset = lethal.snapshot();
            helper.assertTrue(reset.dependencyPhase() == 0 && reset.hangoverTicks() == 0 && reset.withdrawalSeverity() == 0.0D,
                    "Blackout death did not reset dependence, hangover and withdrawal: " + reset);
        });
    }

    @GameTest(template = TEMPLATE)
    public static void withdrawalIsHarmlessAtPhaseFiveHurtsAtNineAndKillsAtFifteen(GameTestHelper helper) {
        run(helper, () -> {
            Withdrawal mild = sitOutWithdrawal(helper, 5, 12_000);
            helper.assertTrue(mild.player.hits == 0, "Dependence phase 5 withdrawal dealt damage");

            Withdrawal moderate = sitOutWithdrawal(helper, 9, 12_000);
            helper.assertTrue(moderate.player.hits > 0 && moderate.player.lastDamageWas("withdrawal"),
                    "Dependence phase 9 withdrawal never dealt withdrawal damage");
            helper.assertTrue(moderate.player.deathSource == null, "Phase 9 withdrawal was lethal");

            Withdrawal late = sitOutWithdrawal(helper, 15, 24_000);
            helper.assertTrue(late.player.diedFrom("withdrawal"), "Phase 15 withdrawal never reached its lethal check");
            helper.assertTrue(late.tracker.snapshot().dependencyPhase() == 0, "Withdrawal death did not reset dependence");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void aDrinkAboveTheThresholdHoldsWithdrawalOff(GameTestHelper helper) {
        run(helper, () -> {
            TestPlayer player = damageablePlayer(helper, "alcohol-suppressed");
            AlcoholRecoveryTracker tracker = trackerAtDependence(15);
            AlcoholSession session = AlcoholSession.start(3.0D);
            helper.assertTrue(tracker.snapshot().suppressionThresholdDrinks() < 3.0D, "Fixture drink was below the threshold");
            for (int i = 0; i < 6_000 && session.isAlive(); i++) {
                session.tick();
                tracker.tick(player, session.effectState());
                player.restoreHealth();
            }
            helper.assertTrue(tracker.withdrawalSeverity() == 0.0D && player.hits == 0,
                    "Drinking above the suppression threshold still produced withdrawal");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void hangoverOnlyHurtsWhenDependenceMakesItSevere(GameTestHelper helper) {
        run(helper, () -> {
            TestPlayer firstTimer = damageablePlayer(helper, "alcohol-hangover-0");
            AlcoholRecoveryTracker fresh = new AlcoholRecoveryTracker();
            finishSession(firstTimer, fresh, 16.0D);
            for (int i = 0; i < 600; i++) {
                fresh.tick(firstTimer, AlcoholEffectState.inactive());
                firstTimer.restoreHealth();
            }
            helper.assertTrue(fresh.snapshot().hangoverActive() && firstTimer.hits == 0,
                    "A first sixteen-drink hangover should be active without dealing damage");

            TestPlayer dependent = damageablePlayer(helper, "alcohol-hangover-15");
            AlcoholRecoveryTracker heavy = trackerAtDependence(15);
            finishSession(dependent, heavy, 16.0D);
            for (int i = 0; i < 400; i++) {
                heavy.tick(dependent, AlcoholEffectState.inactive());
                dependent.restoreHealth();
            }
            helper.assertTrue(dependent.hits > 0 && dependent.lastDamageWas("magic"),
                    "A sixteen-drink hangover at full dependence did not deal hangover damage");
            helper.assertTrue(heavy.withdrawalSeverity() < 0.34D, "Withdrawal damage leaked into the hangover window");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void sleepingEndsTheSessionAndTakesHalfADayOffTheHangover(GameTestHelper helper) {
        run(helper, () -> {
            ServerPlayer player = fakePlayer(helper, "alcohol-sleep");
            CompoundTag session = AlcoholSession.start(6.0D).save();
            CompoundTag midSession = aged(session, ageAt(session.getInt("durationTicks"), 0.4D));

            PsychoactiveExperience slept = experienceWith("alcohol", midSession);
            helper.assertTrue(slept.endActiveTripFromSleep(), "Sleep did not report ending an active alcohol session");
            helper.assertTrue(!slept.alcoholState().active(), "Sleep left the alcohol session running");
            AlcoholRecoveryTracker.RecoveryState afterSleep = slept.alcoholRecoveryState();

            AlcoholRecoveryTracker control = new AlcoholRecoveryTracker();
            control.forceEndSession(AlcoholSession.load(midSession).effectState());
            AlcoholRecoveryTracker.RecoveryState awake = control.snapshot();

            helper.assertTrue(awake.hangoverTicks() > 12_000, "Fixture hangover was too short to observe the sleep reduction");
            helper.assertTrue(afterSleep.hangoverTicks() == awake.hangoverTicks() - 12_000,
                    "Sleep should remove exactly half a Minecraft day of hangover");
            helper.assertTrue(afterSleep.dependencyPhase() == 2, "A six-drink session ended by sleep should add two dependence");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void onlyDmtTherapyReducesAlcoholDependence(GameTestHelper helper) {
        run(helper, () -> {
            // The psilocybin and LSD journal pages say strong trips push against alcohol dependence, but the
            // code and the recovery chapter say only DMT does. This pins the current code behavior.
            AlcoholRecoveryTracker tracker = trackerAtDependence(12);
            tracker.applyPsilocybinTherapy(20.0D, 0);
            tracker.applyLsdTherapy(1_000.0D, 0);
            helper.assertTrue(tracker.snapshot().dependencyPhase() == 12, "Psilocybin or LSD changed alcohol dependence");
            tracker.applyDmtTherapy(2.5D, 0);
            helper.assertTrue(tracker.snapshot().dependencyPhase() == 6, "A 2.5-unit DMT trip should halve dependence");
            tracker.applyDmtTherapy(5.0D, 1);
            helper.assertTrue(tracker.snapshot().dependencyPhase() == 1, "A 5-unit DMT trip should cut dependence to phase 1");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void deathClearsTheSessionAndRecoveryState(GameTestHelper helper) {
        run(helper, () -> {
            CompoundTag saved = new CompoundTag();
            saved.put("alcohol", AlcoholSession.start(7.0D).save());
            saved.put("alcoholRecovery", trackerAtDependence(8).save());
            PsychoactiveExperience experience = PsychoactiveExperience.load(saved);
            helper.assertTrue(experience.alcoholState().active() && experience.alcoholRecoveryState().active(),
                    "Fixture lacked an alcohol session and recovery state");
            experience.clearAlcoholAfterDeath();
            helper.assertTrue(!experience.alcoholState().active() && !experience.alcoholRecoveryState().active(),
                    "Death left alcohol or its recovery state behind");
        });
    }

    @GameTest(template = TEMPLATE)
    public static void recoveryStateSurvivesSaveAndReload(GameTestHelper helper) {
        run(helper, () -> {
            ServerPlayer player = fakePlayer(helper, "alcohol-recovery-save");
            AlcoholRecoveryTracker tracker = trackerAtDependence(4);
            finishSession(player, tracker, 9.0D);
            for (int i = 0; i < 500; i++) tracker.tick(player, AlcoholEffectState.inactive());
            CompoundTag saved = new CompoundTag();
            saved.put("alcoholRecovery", tracker.save());
            PsychoactiveExperience reloaded = PsychoactiveExperience.load(PsychoactiveExperience.load(saved).save());
            helper.assertTrue(reloaded.alcoholRecoveryState().equals(tracker.snapshot()),
                    "Alcohol recovery changed across save/load:\n" + tracker.snapshot() + "\n" + reloaded.alcoholRecoveryState());
        });
    }

    // ---- Fixtures -----------------------------------------------------------------------------

    /** Feeds the tracker one active tick at the given peak, then one sober tick so the session finishes. */
    private static AlcoholRecoveryTracker.RecoveryState finishSession(ServerPlayer player, AlcoholRecoveryTracker tracker, double peakDrinks) {
        tracker.tick(player, AlcoholSession.start(peakDrinks).effectState());
        tracker.tick(player, AlcoholEffectState.inactive());
        return tracker.snapshot();
    }

    private static AlcoholRecoveryTracker trackerAtDependence(int phase) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dependencyPhase", phase);
        return AlcoholRecoveryTracker.load(tag);
    }

    private record Withdrawal(TestPlayer player, AlcoholRecoveryTracker tracker) { }

    /** Ticks a sober dependent player, keeping them at full health so only the lethal check can kill. */
    private static Withdrawal sitOutWithdrawal(GameTestHelper helper, int phase, int ticks) {
        TestPlayer player = damageablePlayer(helper, "alcohol-withdrawal-" + phase);
        AlcoholRecoveryTracker tracker = trackerAtDependence(phase);
        for (int i = 0; i < ticks && player.deathSource == null; i++) {
            tracker.tick(player, AlcoholEffectState.inactive());
            player.restoreHealth();
        }
        return new Withdrawal(player, tracker);
    }
}
