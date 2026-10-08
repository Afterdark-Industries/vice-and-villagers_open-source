package com.trippycraft.substance;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.alcohol.AlcoholEffectState;
import com.trippycraft.substance.alcohol.AlcoholPhase;
import com.trippycraft.substance.alcohol.AlcoholRecoveryTracker.RecoveryState;
import com.trippycraft.substance.cannabis.CannabisEffectState;
import com.trippycraft.substance.cocaine.CocaineEffectState;
import com.trippycraft.substance.dmt.DmtEffectState;
import com.trippycraft.substance.heroin.HeroinEffectState;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import com.trippycraft.substance.ketamine.KetaminePhase;
import com.trippycraft.substance.lsd.LsdEffectState;
import com.trippycraft.substance.mdma.MdmaEffectState;
import com.trippycraft.substance.psilocybin.PsilocybinEffectState;
import com.trippycraft.substance.allad.AlLadEffectState;
import com.trippycraft.substance.allad.AlLadPhase;
import com.trippycraft.substance.twoce.TwoCeEffectState;
import com.trippycraft.substance.twoce.TwoCePhase;
import com.trippycraft.substance.nitrous.NitrousEffectState;
import com.trippycraft.substance.nitrous.NitrousPhase;
import com.trippycraft.substance.twocb.TwoCbEffectState;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class VanillaEffectBridge {
    private VanillaEffectBridge() {
    }

    public static void applyTick(ServerPlayer player, PsychoactiveExperience experience) {
        if (!TrippyConfig.enableVanillaBridge || player.level().getGameTime() % 40L != 0L) {
            return;
        }

        AlcoholEffectState alcohol = experience.alcoholState();
        CannabisEffectState cannabis = experience.cannabisState();
        MdmaEffectState mdma = experience.mdmaState();
        TwoCbEffectState twoCb = experience.twoCbState();
        CocaineEffectState cocaine = experience.cocaineState();
        HeroinEffectState heroin = experience.heroinState();
        RecoveryState alcoholRecovery = experience.alcoholRecoveryState();
        var heroinRecovery = experience.heroinRecoveryState();
        double visual = experience.vanillaNauseaVisualLoad();
        double nausea = experience.channel(ExperienceChannel.NAUSEA);
        double stimulation = experience.channel(ExperienceChannel.STIMULATION);
        double sedation = experience.channel(ExperienceChannel.SEDATION);
        double motorNoise = experience.channel(ExperienceChannel.MOTOR_NOISE);
        double risk = experience.channel(ExperienceChannel.RISK);
        double strain = Math.max(experience.strain(), experience.mixedRisk());
        double craving = experience.channel(ExperienceChannel.CRAVING);
        PsilocybinEffectState psilocybin = experience.psilocybinState();
        LsdEffectState lsd = experience.lsdState();
        DmtEffectState dmt = experience.dmtState();
        KetamineEffectState ketamine = experience.ketamineState();
        double otherEffectScale = experience.ketamineOtherEffectMultiplier();
        double ketamineTherapeutic = ketamine.active() ? ketamineTherapeuticLoad(ketamine) : 0.0D;
        double stimulantBuffScale = positiveBuffCannabisScale(experience, SubstanceFamily.STIMULANT);
        double dissociativeBuffScale = positiveBuffCannabisScale(experience, SubstanceFamily.DISSOCIATIVE);
        double depressantBuffScale = positiveBuffCannabisScale(experience, SubstanceFamily.DEPRESSANT);
        double nonAlcoholRisk = alcoholOnlyWeaknessSource(experience, alcohol)
            ? Math.max(0.0D, risk - alcoholRiskContribution(alcohol))
            : risk;
        double nonAlcoholStrain = alcoholOnlyWeaknessSource(experience, alcohol)
            ? Math.max(0.0D, strain - alcoholStrainContribution(alcohol))
            : strain;
        boolean cocaineCrashWindow = cocaine.active() && (cocaine.phase() == com.trippycraft.substance.cocaine.CocainePhase.CRASH || cocaine.phase() == com.trippycraft.substance.cocaine.CocainePhase.AFTERGLOW);
        boolean cocaineAftermath = experience.cocaineAftermathTicks() > 0;
        boolean cocaineTreatmentWindow = cocaine.active() && !cocaineCrashWindow;

        if (ketamine.active() && otherEffectScale <= 0.35D) {
            suppressNonKetamineBridgeEffects(player);
        }

        visual *= otherEffectScale;
        nausea *= otherEffectScale;
        stimulation *= otherEffectScale;
        sedation *= otherEffectScale;
        motorNoise *= otherEffectScale;
        nonAlcoholRisk *= otherEffectScale;
        nonAlcoholStrain *= otherEffectScale;
        craving *= otherEffectScale;
        double stimulantBuffLoad = stimulation * stimulantBuffScale;

        if (visual > 0.72D || nausea > 0.28D) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 90, amplifier(Math.max(visual, nausea)), true, false, false));
        }
        if (!cocaineCrashWindow && !cocaineAftermath && stimulantBuffLoad > 0.34D && stimulantBuffLoad > sedation) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 90, amplifier(stimulantBuffLoad), true, false, false));
        }
        if (sedation > 0.32D || motorNoise > 0.46D) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, amplifier(Math.max(sedation, motorNoise)), true, false, false));
        }
        if (nonAlcoholRisk > 0.58D) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, amplifier(nonAlcoholRisk), true, false, false));
        }
        if (nonAlcoholStrain > 0.46D) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, amplifier(nonAlcoholStrain), true, false, false));
        }
        if (craving > 0.50D && motorNoise > 0.25D) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0, true, false, false));
        }
        if (cannabis.active()) {
            double cannabisLoad = cannabisStatusLoad(cannabis) * otherEffectScale;
            double slow = clamp(cannabis.thoughtStickiness() * 0.040D + cannabis.temporalDrag() * 0.030D + cannabisLoad * 0.36D);
            double hunger = clamp(cannabisLoad * 0.42D + cannabis.immersion() * 0.032D);
            double floatiness = clamp(cannabis.focusTunnel() * 0.020D + cannabis.breathingPulse() * 0.018D + cannabisLoad * 0.18D);
            double thcDose = Math.max(cannabis.activeThcMilligrams(), cannabis.targetThcMilligrams());
            if (slow > 0.12D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 110, cannabisSlownessAmplifier(slow), true, false, true));
            }
            if (hunger > 0.14D) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 130, cannabisHungerAmplifier(hunger), true, false, true));
            }
            if (thcDose >= 1000.0D && floatiness > 0.18D) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 130, 0, true, false, true));
            }
        }
        if (mdma.active()) {
            if (mdma.peakWindow() && mdma.movementLift() > 0.08D && mdma.comedownFatigue() < 0.10D) {
                int speedAmplifier = mdma.tier() >= 4 && mdma.movementLift() > 0.72D ? 1 : 0;
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, speedAmplifier, true, false, true));
            }
            if (mdma.regeneration() > 0.045D && mdma.peakWindow()) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
            }
            if (mdma.hungerSuppression() > 0.04D && mdma.peakWindow()) {
                replenishMdmaSaturation(player, mdma.hungerSuppression());
            }
            if (mdma.dreaminess() > 0.30D && mdma.peakWindow()) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 90, 0, true, false, true));
            }
            if (mdma.coordinationNoise() > 0.16D) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 90, mdma.coordinationNoise() > 0.70D ? 1 : 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, mdma.coordinationNoise() > 0.78D ? 1 : 0, true, false, true));
            }
            if (mdma.comedownFatigue() > 0.14D) {
                int fatigueAmplifier = mdma.comedownFatigue() > 0.74D ? 1 : 0;
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, fatigueAmplifier, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, fatigueAmplifier, true, false, true));
                if (mdma.comedownFatigue() > 0.48D) {
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, mdma.comedownFatigue() > 0.82D ? 1 : 0, true, false, true));
                }
            }
        }
        if (twoCb.active()) {
            double dose = Math.max(twoCb.activeDoseMilligrams(), twoCb.targetDoseMilligrams());
            if (twoCb.saturation() > 0.20D) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
            }
            if (twoCb.stimulation() > 0.16D) {
                int speedAmplifier = dose > 1_000.0D ? 2 : (dose > 500.0D ? 1 : 0);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, speedAmplifier, true, false, true));
            }
            if (twoCb.peakWindow() && twoCb.edgeGlow() > 0.42D) {
                int hasteAmplifier = dose > 1_000.0D ? 2 : (dose > 500.0D ? 1 : 0);
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 100, hasteAmplifier, true, false, true));
            }
        }
        AlLadEffectState alLad = experience.alLadState();
        if (alLad.active()) {
            double dose = Math.max(alLad.activeDoseMicrograms(), alLad.targetDoseMicrograms());
            if (alLad.visualPresence() > 0.16D) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, true));
            }
            if (alLad.stimulation() > 0.18D && dose >= 140.0D) {
                int speedAmplifier = dose >= 420.0D ? 1 : 0;
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, speedAmplifier, true, false, true));
            }
            if ((alLad.phase() == AlLadPhase.PEAK || alLad.phase() == AlLadPhase.PLATEAU) && alLad.jewelColor() > 0.28D) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, dose >= 300.0D ? 1 : 0, true, false, true));
            }
            if (alLad.bodyLoad() > 0.28D && dose >= 280.0D) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, alLad.bodyLoad() > 0.48D ? 1 : 0, true, false, true));
            }
            if (dose >= 450.0D && alLad.prismDepth() > 0.55D && alLad.phase() != AlLadPhase.AFTERGLOW) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, true));
            }
            if ((alLad.phase() == AlLadPhase.DESCENT || alLad.phase() == AlLadPhase.AFTERGLOW) && alLad.bodyLoad() > 0.22D) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, true, false, true));
            }
        }
        TwoCeEffectState twoCe = experience.twoCeState();
        if (twoCe.active()) {
            if (twoCe.visualPresence() > 0.16D) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, true));
            }
            if (twoCe.bodyLoad() > 0.18D) {
                int slow = twoCe.bodyLoad() > 0.42D ? 1 : 0;
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, slow, true, false, true));
            }
            if ((twoCe.phase() == TwoCePhase.DESCENT || twoCe.phase() == TwoCePhase.AFTERGLOW) && twoCe.bodyLoad() > 0.20D) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, true, false, true));
            }
        }
        NitrousEffectState nitrous = experience.nitrousState();
        if (nitrous.active()) {
            if (nitrous.euphoria() > 0.20D) {
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 80, 0, true, false, true));
            }
            if (nitrous.euphoria() > 0.28D) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, true, false, true));
            }
            if (nitrous.motorNoise() > 0.16D) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 0, true, false, true));
            }
            if (nitrous.motorNoise() > 0.18D || nitrous.phase() == NitrousPhase.FADE) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, true, false, true));
            }
        }
        if (cocaine.active()) {
            double cocaineLoad = cocaineStatusLoad(cocaine) * otherEffectScale;
            double rush = clamp(cocaine.stimulation() * 0.080D + cocaine.locomotorDrive() * 0.055D + cocaineLoad * 0.18D);
            double push = clamp(cocaine.euphoria() * 0.045D + cocaine.compulsiveMomentum() * 0.060D + cocaine.inputSharpness() * 0.035D);
            double crash = clamp(cocaine.crashDrag() * 0.090D + cocaine.crashDesaturation() * 0.36D);
            double cocaineSpeedBuff = Math.min(1.0D, (rush + cocaineLoad * 0.16D) * stimulantBuffScale);
            double cocaineStrengthBuff = Math.min(1.0D, push * stimulantBuffScale);
            if (!cocaineCrashWindow && cocaineSpeedBuff > 0.14D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 90, amplifier(cocaineSpeedBuff), true, false, true));
            }
            if (cocaineStrengthBuff > 0.22D && crash < 0.58D) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 90, amplifier(cocaineStrengthBuff), true, false, true));
            }
            if (crash > 0.22D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, amplifier(Math.min(1.0D, crash)), true, false, true));
            }
        }
        if (ketamine.active()) {
            double ketamineLoad = ketamineStatusLoad(ketamine);
            boolean kHole = ketamine.phase() == KetaminePhase.K_HOLE;
            double dose = Math.max(ketamine.activeDoseMilligrams(), ketamine.targetDoseMilligrams());
            double movementLoad = clamp(
                    clamp(ketamine.motorDisconnect() / 2.6D) * 0.46D
                            + clamp(ketamine.inputLatency() / 2.3D) * 0.26D
                            + clamp(ketamine.bodyFloat() / 2.4D) * 0.12D
                            + ketamineLoad * 0.22D
                            + (kHole ? 0.18D : 0.0D)
                    - ketamineTherapeutic * 0.22D
            );
            double combatLoad = clamp(
                    clamp(ketamine.motorDisconnect() / 2.4D) * 0.42D
                            + clamp(ketamine.depthMismatch() / 2.6D) * 0.16D
                            + clamp(ketamine.uiFade() / 2.0D) * 0.12D
                            + ketamineLoad * 0.20D
                            + (kHole ? 0.14D : 0.0D)
                    - ketamineTherapeutic * 0.28D
            );
            double miningLoad = clamp(
                    clamp(ketamine.motorDisconnect() / 2.2D) * 0.46D
                            + clamp(ketamine.inputLatency() / 2.2D) * 0.18D
                            + clamp(ketamine.timeSlip() / 2.4D) * 0.14D
                            + ketamineLoad * 0.18D
                            + (kHole ? 0.20D : 0.0D)
                    - ketamineTherapeutic * 0.18D
            );
            double floatiness = clamp(
                    clamp(ketamine.bodyFloat() / 2.2D) * 0.34D
                            + clamp(ketamine.gravityDrift() / 2.4D) * 0.42D
                            + ketamineLoad * 0.12D
            );
            double painShield = clamp(
                    clamp(ketamine.painSuppression() / 2.8D) * 0.64D
                            + ketamineLoad * 0.16D
                            + (kHole ? 0.08D : 0.0D)
            );
                    double regeneration = clamp(ketamineTherapeutic * 0.72D + smoothstep(80.0D, 150.0D, dose) * 0.22D) * dissociativeBuffScale;
                    double totalResistance = Math.max(painShield, ketamineTherapeutic * 0.78D) * dissociativeBuffScale;
            double collapse = clamp(
                    clamp(ketamine.collapseRisk() / 2.0D) * 0.52D
                            + clamp(ketamine.collapseDepth() / 1.6D) * 0.66D
                            + (kHole ? 0.12D : 0.0D)
            );
            double blackout = clamp(
                    collapse
                            + clamp(ketamine.uiFade() / 2.2D) * 0.20D
                            + clamp(ketamine.audioMuffle() / 2.6D) * 0.08D
            );

            if (movementLoad > 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 110, amplifier(Math.min(1.0D, movementLoad + collapse * 0.18D)), true, false, true));
            }
            if (combatLoad > 0.10D) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 110, amplifier(Math.min(1.0D, combatLoad + collapse * 0.20D)), true, false, true));
            }
            if (miningLoad > 0.08D) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 110, ketamineMiningFatigueAmplifier(miningLoad, kHole), true, false, true));
            }
            if (floatiness > 0.18D) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false, true));
            }
            if (totalResistance > 0.12D) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 110, ketamineResistanceAmplifier(totalResistance, collapse, ketamineTherapeutic), true, false, true));
            }
            if (regeneration > 0.16D) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, ketamineRegenerationAmplifier(regeneration), true, false, true));
            }
            if (blackout > 0.22D) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, ketamineCollapseAmplifier(blackout), true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, Math.max(1, amplifier(blackout)), true, false, true));
                if (blackout > 0.78D) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 70, 0, true, false, true));
                }
            }
        }
        if (cocaineAftermath) {
            double aftermathLoad = smoothstep(60.0D, 8.0D * 60.0D, experience.cocaineSystemDoseGrams() * 60.0D) * otherEffectScale;
            int amplifier = experience.cocaineSystemDoseGrams() >= 5.0D ? 1 : 0;
            if (aftermathLoad > 0.02D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, aftermathLoad > 0.82D ? Math.max(amplifier, 1) : amplifier, true, false, true));
            }
        }
        if (alcohol.active() && TrippyConfig.enableAlcoholEffects) {
            double safe = Math.max(1.0D, TrippyConfig.alcoholMaxVisualIntensity);
            double alcoholMotor = clamp(alcohol.motorNoise() / safe) * otherEffectScale;
            double alcoholSedation = clamp(alcohol.sedation() / safe) * otherEffectScale;
            double alcoholBlur = clamp(Math.max(alcohol.diplopia(), alcohol.focusBlur()) / safe) * otherEffectScale;
            double alcoholNausea = clamp(alcohol.nausea() / safe) * otherEffectScale;
            double heavyBalance = smoothstep(12.0D, 18.0D, alcohol.activeStandardDrinks());
            double balance = Math.max(alcoholMotor + heavyBalance * 0.26D, alcoholSedation * 0.74D + heavyBalance * 0.18D);
            if (balance > 0.18D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, amplifier(Math.min(1.0D, balance + Math.max(0, alcohol.tier() - 3) * 0.08D + heavyBalance * 0.16D)), true, false, true));
            }
            if (alcoholBlur > 0.24D || alcoholNausea > 0.16D) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 90, amplifier(Math.max(alcoholBlur, alcoholNausea)), true, false, true));
            }
            double strengthWindow = smoothstep(1.0D, 1.8D, alcohol.activeStandardDrinks())
                    * (1.0D - smoothstep(15.0D, 16.0D, alcohol.activeStandardDrinks()));
            strengthWindow *= otherEffectScale * depressantBuffScale;
            if (strengthWindow > 0.02D) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 90, alcoholStrengthAmplifier(alcohol.activeStandardDrinks()), true, false, true));
            }

            double alcoholProtection = alcoholProtection(alcohol, alcoholSedation) * otherEffectScale * depressantBuffScale;
            double alcoholRegeneration = alcoholRegeneration(alcohol, alcoholProtection) * otherEffectScale * depressantBuffScale;
            if (alcoholProtection > 0.16D) {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, alcoholAbsorptionAmplifier(alcoholProtection), true, false, true));
            }
            if (alcoholRegeneration > 0.18D) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, alcoholRegenerationAmplifier(alcoholRegeneration), true, false, true));
            }
            replenishAlcoholHunger(player, alcohol, alcoholProtection);
        }

        double hangover = effectiveHangover(alcoholRecovery, alcohol);
        double withdrawalRecovery = clamp(alcoholRecovery.withdrawalSeverity());
    hangover *= otherEffectScale;
    withdrawalRecovery *= otherEffectScale;
        double grogginess = Math.max(hangover * 0.78D, withdrawalRecovery * 0.92D);
        double recoveryHunger = Math.max(hangover * 0.40D, withdrawalRecovery * 0.86D);
        if (grogginess > 0.16D) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, grogginess > 0.86D ? 2 : grogginess > 0.52D ? 1 : 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, grogginess > 0.70D ? 1 : 0, true, false, true));
        }
        if (recoveryHunger > 0.20D) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, recoveryHunger > 0.75D ? 2 : recoveryHunger > 0.45D ? 1 : 0, true, false, true));
        }
        double hangoverNausea = hangoverNausea(alcoholRecovery, alcohol) * otherEffectScale;
        if (hangoverNausea > 0.18D) {
            int nauseaDuration = hangoverNausea > 0.82D ? 180 : hangoverNausea > 0.52D ? 140 : 100;
            int nauseaAmplifier = hangoverNausea > 0.88D ? 2 : hangoverNausea > 0.60D ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nauseaDuration, nauseaAmplifier, true, false, true));
        } else if (Math.max(hangover, withdrawalRecovery) > 0.72D) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, true, false, true));
        }

        double heroinRelief = heroin.active() ? heroinReliefLoad(heroin) : 0.0D;
        double opioidCrash = heroinRecovery.crashActive() ? heroinRecovery.crashSeverity() * (1.0D - heroinRelief) : 0.0D;
        double opioidWithdrawal = heroinRecovery.withdrawalActive() ? heroinRecovery.withdrawalSeverity() * (1.0D - heroinRelief) : 0.0D;
        double opioidDiscomfort = Math.max(opioidCrash, opioidWithdrawal);
        if (opioidDiscomfort > 0.12D) {
            int fatigueAmplifier = opioidDiscomfort > 0.84D ? 3 : opioidDiscomfort > 0.60D ? 2 : opioidDiscomfort > 0.34D ? 1 : 0;
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 130, fatigueAmplifier, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 130, opioidDiscomfort > 0.82D ? 2 : opioidDiscomfort > 0.54D ? 1 : 0, true, false, true));
            if (opioidWithdrawal > 0.28D || opioidCrash > 0.45D) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 130, opioidDiscomfort > 0.86D ? 3 : opioidDiscomfort > 0.66D ? 2 : opioidDiscomfort > 0.42D ? 1 : 0, true, false, true));
            }
            if (opioidWithdrawal > 0.24D || opioidCrash > 0.54D) {
                int nauseaAmplifier = opioidDiscomfort > 0.90D ? 2 : opioidDiscomfort > 0.64D ? 1 : 0;
                int nauseaDuration = opioidDiscomfort > 0.82D ? 170 : 130;
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nauseaDuration, nauseaAmplifier, true, false, true));
            }
            if (opioidWithdrawal > 0.56D) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 130, opioidWithdrawal > 0.88D ? 2 : opioidWithdrawal > 0.68D ? 1 : 0, true, false, true));
            }
        }

        if (heroin.active()) {
            applyHeroinActiveEffects(player, heroin);
        }

        if (cannabis.active() && player.hasEffect(MobEffects.CONFUSION)) {
            player.removeEffect(MobEffects.CONFUSION);
        }
        if (ketamineTherapeutic > 0.08D) {
            int nauseaReliefAmplifier = ketamineNauseaReliefAmplifier(ketamineTherapeutic);
            if (nauseaReliefAmplifier >= 0) {
                clearEffectUpToAmplifier(player, MobEffects.CONFUSION, nauseaReliefAmplifier);
            }
            replenishKetamineSaturation(player, ketamineTherapeutic);
        }
        if (cocaineTreatmentWindow && !ketamine.active()) {
            player.removeEffect(MobEffects.WEAKNESS);
            int treatmentAmplifier = cocaineTreatmentAmplifier(cocaine);
            clearEffectUpToAmplifier(player, MobEffects.MOVEMENT_SLOWDOWN, treatmentAmplifier);
            clearEffectUpToAmplifier(player, MobEffects.CONFUSION, treatmentAmplifier);
            clearEffectUpToAmplifier(player, MobEffects.DIG_SLOWDOWN, treatmentAmplifier);
        }

        double activePsychedelicLuck = activePsychedelicLuckLoad(psilocybin, lsd, dmt) * otherEffectScale;
        int activeLuckAmplifier = activePsychedelicLuckAmplifier(activePsychedelicLuck);
        int postTripLuckAmplifier = experience.psychedelicLuckTicks() > 0 && otherEffectScale > 0.40D
                ? experience.psychedelicLuckAmplifier()
                : -1;
        int luckAmplifier = Math.max(activeLuckAmplifier, postTripLuckAmplifier);
        if (luckAmplifier >= 0) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 120, luckAmplifier, true, false, true));
        }
        if (experience.dmtResistanceTicks() > 0 && otherEffectScale > 0.40D) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, experience.dmtResistanceAmplifier(), true, false, true));
        }
    }

    private static void applyHeroinActiveEffects(ServerPlayer player, HeroinEffectState heroin) {
        double load = heroinStatusLoad(heroin);
        double relief = heroinReliefLoad(heroin);
        double dose = Math.max(heroin.activeDoseMilligrams(), heroin.targetDoseMilligrams());
        if (relief > 0.08D) {
            int reliefAmplifier = heroinReliefAmplifier(relief, dose);
            clearEffectUpToAmplifier(player, MobEffects.CONFUSION, reliefAmplifier);
            clearEffectUpToAmplifier(player, MobEffects.HUNGER, reliefAmplifier);
            if (dose >= 100.0D) {
                clearEffectUpToAmplifier(player, MobEffects.POISON, reliefAmplifier);
            }
            if (dose >= 250.0D) {
                clearEffectUpToAmplifier(player, MobEffects.WITHER, Math.max(0, reliefAmplifier - 1));
                clearEffectUpToAmplifier(player, MobEffects.WEAKNESS, Math.max(0, reliefAmplifier - 1));
            }
        }

        double movementLoad = clamp(
                clamp(heroin.motorDrag() / 4.2D) * 0.54D
                        + clamp(heroin.nodding() / 4.4D) * 0.38D
                        + clamp(heroin.microSleep() / 3.8D) * 0.36D
                        + load * 0.18D
        );
        double miningLoad = clamp(
                clamp(heroin.miningDrag() / 4.0D) * 0.56D
                        + clamp(heroin.nodding() / 4.2D) * 0.30D
                        + clamp(heroin.microSleep() / 3.6D) * 0.34D
        );
        double regeneration = clamp(heroin.regeneration() / 4.4D);
        double resistance = clamp(heroin.resistance() / 4.4D + heroin.painSuppression() / 8.2D * 0.42D);
        double absorption = clamp(heroin.absorption() / 4.4D);
        double shutdown = clamp(heroin.respiratorySuppression() / 4.4D + heroin.microSleep() / 3.8D * 0.34D);
        double appetite = clamp(heroin.hungerSuppression() / 4.4D + heroin.appetiteSuppression() / 4.8D * 0.36D);
        double floatiness = clamp(heroin.softFocus() / 6.2D * 0.34D + heroin.sedation() / 8.2D * 0.24D + load * 0.18D);

        if (movementLoad > 0.10D) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, heroinSlownessAmplifier(movementLoad, shutdown), false, false, true));
        }
        if (miningLoad > 0.08D) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, heroinMiningFatigueAmplifier(miningLoad, shutdown), false, false, true));
        }
        if (regeneration > 0.020D) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 110, heroinRegenerationAmplifier(regeneration), false, false, true));
        }
        if (resistance > 0.015D) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, heroinResistanceAmplifier(resistance, shutdown), false, false, true));
        }
        if (absorption > 0.014D) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 140, heroinAbsorptionAmplifier(absorption), false, false, true));
        }
        if (floatiness > 0.20D) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false, true));
        }
        if (appetite > 0.10D) {
            player.removeEffect(MobEffects.HUNGER);
            replenishHeroinHungerSuppression(player, appetite);
        }
        if (shutdown > 0.20D) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, heroinShutdownAmplifier(shutdown), false, false, true));
            if (shutdown > 0.72D) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 1, false, false, true));
            }
        }
    }

    private static double heroinStatusLoad(HeroinEffectState heroin) {
        double intensity = clamp(heroin.intensity() / 8.2D);
        double dose = smoothstep(25.0D, 500.0D, Math.max(heroin.activeDoseMilligrams(), heroin.targetDoseMilligrams()));
        double nod = clamp((heroin.nodding() + heroin.microSleep()) / 8.2D);
        return clamp(intensity * 0.54D + dose * 0.24D + nod * 0.34D);
    }

    private static double heroinReliefLoad(HeroinEffectState heroin) {
        if (heroin == null || !heroin.active()) {
            return 0.0D;
        }
        double dose = Math.max(heroin.activeDoseMilligrams(), heroin.targetDoseMilligrams());
        return clamp(heroin.nauseaRelief() / 5.2D * 0.48D
                + heroin.painSuppression() / 8.2D * 0.40D
                + smoothstep(25.0D, 250.0D, dose) * 0.32D);
    }

    private static int heroinReliefAmplifier(double relief, double dose) {
        if (relief > 0.82D || dose >= 250.0D) {
            return 2;
        }
        if (relief > 0.46D || dose >= 100.0D) {
            return 1;
        }
        return 0;
    }

    private static int heroinSlownessAmplifier(double movementLoad, double shutdown) {
        if (shutdown > 0.82D || movementLoad > 0.86D) {
            return 3;
        }
        if (shutdown > 0.58D || movementLoad > 0.62D) {
            return 2;
        }
        if (movementLoad > 0.30D) {
            return 1;
        }
        return 0;
    }

    private static int heroinMiningFatigueAmplifier(double miningLoad, double shutdown) {
        if (shutdown > 0.70D || miningLoad > 0.82D) {
            return 2;
        }
        if (miningLoad > 0.44D) {
            return 1;
        }
        return 0;
    }

    private static int heroinRegenerationAmplifier(double regeneration) {
        if (regeneration > 0.90D) {
            return 2;
        }
        if (regeneration > 0.56D) {
            return 1;
        }
        return 0;
    }

    private static int heroinResistanceAmplifier(double resistance, double shutdown) {
        if (resistance > 0.88D && shutdown < 0.72D) {
            return 1;
        }
        return 0;
    }

    private static int heroinAbsorptionAmplifier(double absorption) {
        if (absorption > 0.86D) {
            return 2;
        }
        if (absorption > 0.54D) {
            return 1;
        }
        return 0;
    }

    private static int heroinShutdownAmplifier(double shutdown) {
        if (shutdown > 0.86D) {
            return 3;
        }
        if (shutdown > 0.62D) {
            return 2;
        }
        return 1;
    }

    private static double activePsychedelicLuckLoad(PsilocybinEffectState psilocybin, LsdEffectState lsd, DmtEffectState dmt) {
        return clamp(Math.max(psilocybinActiveLuck(psilocybin), Math.max(lsdActiveLuck(lsd), dmtActiveLuck(dmt))));
    }

    private static double psilocybinActiveLuck(PsilocybinEffectState psilocybin) {
        if (psilocybin == null || !psilocybin.active()) {
            return 0.0D;
        }
        double safe = Math.max(1.0D, TrippyConfig.psilocybinMaxVisualIntensity);
        double opening = smoothstep(0.08D, 0.28D, psilocybin.progress()) * (1.0D - smoothstep(0.90D, 1.0D, psilocybin.progress()));
        double dose = smoothstep(0.5D, 5.0D, Math.max(psilocybin.activeDoseGrams(), psilocybin.targetDoseGrams()));
        return clamp(psilocybin.visualIntensity() / safe * 0.26D
                + psilocybin.colorDepth() / safe * 0.22D
                + psilocybin.organicBreathing() / safe * 0.16D
                + opening * 0.20D
                + dose * 0.18D);
    }

    private static double lsdActiveLuck(LsdEffectState lsd) {
        if (lsd == null || !lsd.active()) {
            return 0.0D;
        }
        double safe = Math.max(1.0D, TrippyConfig.lsdMaxVisualIntensity);
        double opening = smoothstep(0.06D, 0.24D, lsd.progress()) * (1.0D - smoothstep(0.90D, 1.0D, lsd.progress()));
        double dose = smoothstep(50.0D, 500.0D, Math.max(lsd.activeDoseMicrograms(), lsd.targetDoseMicrograms()));
        return clamp(lsd.visualIntensity() / safe * 0.24D
                + lsd.realityFold() / safe * 0.24D
                + lsd.electricBloom() / safe * 0.18D
                + opening * 0.18D
                + dose * 0.22D);
    }

    private static double dmtActiveLuck(DmtEffectState dmt) {
        if (dmt == null || !dmt.active()) {
            return 0.0D;
        }
        double safe = Math.max(1.0D, TrippyConfig.dmtMaxVisualIntensity);
        double opening = smoothstep(0.02D, 0.12D, dmt.progress()) * (1.0D - smoothstep(0.94D, 1.0D, dmt.progress()));
        double dose = smoothstep(0.25D, 5.0D, Math.max(dmt.activeDoseUnits(), dmt.targetDoseUnits()));
        return clamp(dmt.visualIntensity() / safe * 0.20D
                + dmt.environmentalConsciousness() * 0.22D
                + dmt.dominance() * 0.16D
                + dmt.symbolLanguage() * 0.12D
                + opening * 0.18D
                + dose * 0.18D);
    }

    private static int activePsychedelicLuckAmplifier(double load) {
        if (load > 0.88D) {
            return 2;
        }
        if (load > 0.46D) {
            return 1;
        }
        if (load > 0.10D) {
            return 0;
        }
        return -1;
    }

    private static void replenishHeroinHungerSuppression(ServerPlayer player, double appetite) {
        int cap = appetite > 0.72D ? 18 : appetite > 0.36D ? 16 : 14;
        int currentFood = player.getFoodData().getFoodLevel();
        if (currentFood < cap) {
            player.getFoodData().setFoodLevel(Math.min(cap, currentFood + 1));
        }
        float currentSaturation = player.getFoodData().getSaturationLevel();
        float target = Math.min(player.getFoodData().getFoodLevel(), 1.5F + (float) (appetite * 3.0D));
        if (currentSaturation < target) {
            player.getFoodData().setSaturation(Math.min(target, currentSaturation + 0.35F + (float) appetite * 0.45F));
        }
    }

    private static void replenishMdmaSaturation(ServerPlayer player, double suppression) {
        float current = player.getFoodData().getSaturationLevel();
        float cap = player.getFoodData().getFoodLevel();
        float bonus = 0.12F + (float) clamp(suppression) * 0.34F;
        player.getFoodData().setSaturation(Math.min(cap, current + bonus));
    }

    private static boolean alcoholOnlyWeaknessSource(PsychoactiveExperience experience, AlcoholEffectState alcohol) {
        if (alcohol == null || !alcohol.active()) {
            return false;
        }
        return experience.familyBlend().entrySet().stream()
                .filter(entry -> entry.getKey() != SubstanceFamily.DEPRESSANT)
                .noneMatch(entry -> entry.getValue() > 0.08D);
    }

    private static double alcoholRiskContribution(AlcoholEffectState alcohol) {
        return clamp(alcohol.risk() / 6.0D) * 0.58D;
    }

    private static double alcoholStrainContribution(AlcoholEffectState alcohol) {
        double safe = 6.0D;
        double sedation = clamp(alcohol.sedation() / safe);
        double motor = clamp(alcohol.motorNoise() / safe);
        double risk = clamp(alcohol.risk() / safe);
        return clamp(sedation * 0.42D + motor * 0.30D + risk * 0.24D);
    }

    private static int alcoholStrengthAmplifier(double standardDrinks) {
        if (standardDrinks >= 10.0D) {
            return 2;
        }
        if (standardDrinks >= 5.0D) {
            return 1;
        }
        return 0;
    }

    private static double positiveBuffCannabisScale(PsychoactiveExperience experience, SubstanceFamily family) {
        if (experience == null) {
            return 1.0D;
        }
        return 1.0D / Math.max(1.0D, experience.cannabisPotentiationFactor(family));
    }

    private static double cannabisStatusLoad(CannabisEffectState cannabis) {
        double load = Math.log1p(Math.max(0.0D, cannabis.intensity())) / Math.log1p(18.0D);
        load += smoothstep(1.0D, 20.0D, cannabis.doseScale()) * 0.08D;
        return clamp(load);
    }

    private static int cannabisSlownessAmplifier(double load) {
        if (load > 0.92D) {
            return 2;
        }
        if (load > 0.46D) {
            return 1;
        }
        return 0;
    }

    private static int cannabisHungerAmplifier(double load) {
        if (load > 0.88D) {
            return 2;
        }
        if (load > 0.44D) {
            return 1;
        }
        return 0;
    }

    private static double cocaineStatusLoad(CocaineEffectState cocaine) {
        double load = Math.log1p(Math.max(0.0D, cocaine.intensity())) / Math.log1p(10.0D);
        load += smoothstep(1.0D, 6.0D, cocaine.activeDoseGrams()) * 0.10D;
        return clamp(load);
    }

    private static int cocaineTreatmentAmplifier(CocaineEffectState cocaine) {
        double dose = Math.max(cocaine.activeDoseGrams(), cocaine.targetDoseGrams());
        if (dose >= 5.0D) {
            return 2;
        }
        if (dose >= 3.0D) {
            return 1;
        }
        if (dose >= 1.0D) {
            return 0;
        }
        return -1;
    }

    private static double ketamineStatusLoad(KetamineEffectState ketamine) {
        double intensity = clamp(ketamine.intensity() / 4.8D);
        double disconnect = clamp(ketamine.motorDisconnect() / 2.8D);
        double latency = clamp(ketamine.inputLatency() / 2.6D);
        double dose = smoothstep(50.0D, 500.0D, Math.max(ketamine.activeDoseMilligrams(), ketamine.targetDoseMilligrams()));
        return clamp(intensity * 0.44D + disconnect * 0.22D + latency * 0.16D + dose * 0.18D);
    }

    private static double ketamineTherapeuticLoad(KetamineEffectState ketamine) {
        double dose = Math.max(ketamine.activeDoseMilligrams(), ketamine.targetDoseMilligrams());
        return clamp(smoothstep(25.0D, 150.0D, dose) * (1.0D - smoothstep(150.0D, 190.0D, dose)));
    }

    private static int ketamineMiningFatigueAmplifier(double miningLoad, boolean kHole) {
        if (kHole || miningLoad > 0.84D) {
            return 2;
        }
        if (miningLoad > 0.40D) {
            return 1;
        }
        return 0;
    }

    private static int ketamineResistanceAmplifier(double resistance, double collapse, double therapeutic) {
        if (resistance > 0.86D || collapse > 0.70D || therapeutic > 0.88D) {
            return 1;
        }
        return 0;
    }

    private static int ketamineRegenerationAmplifier(double regeneration) {
        if (regeneration > 0.86D) {
            return 1;
        }
        return 0;
    }

    private static int ketamineNauseaReliefAmplifier(double therapeutic) {
        if (therapeutic > 0.72D) {
            return 1;
        }
        if (therapeutic > 0.16D) {
            return 0;
        }
        return -1;
    }

    private static int ketamineCollapseAmplifier(double blackout) {
        if (blackout > 0.86D) {
            return 3;
        }
        if (blackout > 0.62D) {
            return 2;
        }
        if (blackout > 0.34D) {
            return 1;
        }
        return 0;
    }

    private static void replenishKetamineSaturation(ServerPlayer player, double therapeutic) {
        float current = player.getFoodData().getSaturationLevel();
        float capped = player.getFoodData().getFoodLevel();
        float bonus = (float) (0.16D + therapeutic * 0.70D);
        player.getFoodData().setSaturation(Math.min(capped, current + bonus));
    }

    private static void clearEffectUpToAmplifier(ServerPlayer player, Holder<MobEffect> effect, int maxAmplifier) {
        if (maxAmplifier < 0) {
            return;
        }
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.getAmplifier() <= maxAmplifier) {
            player.removeEffect(effect);
        }
    }

    private static void suppressNonKetamineBridgeEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.ABSORPTION);
        player.removeEffect(MobEffects.REGENERATION);
    }

    private static double alcoholProtection(AlcoholEffectState alcohol, double alcoholSedation) {
        double numbing = clamp(alcoholSedation * 0.18D
                + clamp(alcohol.euphoria() / 4.0D) * 0.14D
                + smoothstep(2.0D, 10.0D, alcohol.activeStandardDrinks()) * 0.68D);
        if (alcohol.phase() == AlcoholPhase.HEAVY) {
            return clamp(numbing + 0.08D);
        }
        if (alcohol.phase() == AlcoholPhase.COMEDOWN) {
            return clamp(numbing * 0.72D);
        }
        return numbing;
    }

    private static double alcoholRegeneration(AlcoholEffectState alcohol, double alcoholProtection) {
        double recovery = clamp(smoothstep(2.0D, 10.0D, alcohol.activeStandardDrinks()) * 0.84D + alcoholProtection * 0.16D);
        if (alcohol.phase() == AlcoholPhase.COMEDOWN || alcohol.phase() == AlcoholPhase.AFTERGLOW) {
            recovery *= 0.60D;
        }
        return recovery;
    }

    private static int alcoholAbsorptionAmplifier(double protection) {
        if (protection > 0.90D) {
            return 2;
        }
        if (protection > 0.62D) {
            return 1;
        }
        return 0;
    }

    private static int alcoholRegenerationAmplifier(double regeneration) {
        if (regeneration > 0.94D) {
            return 3;
        }
        if (regeneration > 0.74D) {
            return 2;
        }
        if (regeneration > 0.45D) {
            return 1;
        }
        return 0;
    }

    private static void replenishAlcoholHunger(ServerPlayer player, AlcoholEffectState alcohol, double alcoholProtection) {
        if (!isDrunkPhase(alcohol.phase())) {
            return;
        }
        int nutrition = alcohol.activeStandardDrinks() >= 8.0D ? 3 : alcohol.activeStandardDrinks() >= 4.0D ? 2 : 1;
        if (nutrition <= 0) {
            return;
        }
        float saturation = 0.20F + (float) (alcoholProtection * 0.45D);
        player.getFoodData().eat(nutrition, saturation);
    }

    private static boolean isDrunkPhase(AlcoholPhase phase) {
        return phase == AlcoholPhase.BUZZ
                || phase == AlcoholPhase.INTOXICATED
                || phase == AlcoholPhase.HEAVY
                || phase == AlcoholPhase.COMEDOWN;
    }

    private static double effectiveHangover(RecoveryState recovery, AlcoholEffectState alcohol) {
        if (recovery == null || !recovery.hangoverActive()) {
            return 0.0D;
        }
        double activeDrinks = alcohol.active() ? alcohol.activeStandardDrinks() : 0.0D;
        double mask = 1.0D - smoothstep(0.20D, 1.25D, activeDrinks);
        double remaining = recovery.hangoverDurationTicks() <= 0 ? 0.0D : recovery.hangoverTicks() / (double) recovery.hangoverDurationTicks();
        return clamp(recovery.hangoverSeverity() * Math.sqrt(Math.max(0.0D, remaining)) * mask);
    }

    private static double hangoverNausea(RecoveryState recovery, AlcoholEffectState alcohol) {
        if (recovery == null || !recovery.hangoverNauseaActive()) {
            return 0.0D;
        }
        double hangover = effectiveHangover(recovery, alcohol);
        double drinks = smoothstep(6.0D, 16.0D, recovery.hangoverPeakDrinks());
        double dependency = smoothstep(4.0D, 12.0D, recovery.dependencyPhase());
        return clamp(hangover * (0.52D + drinks * 0.36D + dependency * 0.24D));
    }

    private static int amplifier(double intensity) {
        if (intensity > 0.88D) {
            return 2;
        }
        if (intensity > 0.62D) {
            return 1;
        }
        return 0;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        if (edge1 <= edge0) {
            return value >= edge1 ? 1.0D : 0.0D;
        }
        double t = Math.max(0.0D, Math.min(1.0D, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }
}
