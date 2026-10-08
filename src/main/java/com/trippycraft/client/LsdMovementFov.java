package com.trippycraft.client;

import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.SubstanceFamily;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

import java.util.HashSet;
import java.util.Set;

/** Keep LSD's bridge movement buffs from becoming a separate oscillating lens. */
final class LsdMovementFov {
    private static ClientLevel level;
    private static Player trackedPlayer;
    private static final Set<ResourceLocation> bridgeModifiers = new HashSet<>();
    private static String lastDiagnostic = "not sampled";
    private static final boolean TRACE = Boolean.getBoolean("trippycraft.lsdSmokeTest");

    private LsdMovementFov() { }

    static void onModifier(ComputeFovModifierEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = event.getPlayer();
        if (player != minecraft.player) return;
        if (level != minecraft.level || trackedPlayer != player) {
            level = minecraft.level;
            trackedPlayer = player;
            bridgeModifiers.clear();
        }
        if (!TrippyConfig.enableLsdEffects || !TrippyConfig.enableVanillaBridge) {
            bridgeModifiers.clear();
            return;
        }
        ClientEffectSample sample = ClientEffectGraph.sample(minecraft.getTimer().getGameTimeDeltaPartialTick(false));
        boolean anotherFamily = false;
        for (SubstanceFamily family : SubstanceFamily.values()) {
            if (family != SubstanceFamily.PSYCHEDELIC && sample.familyBlend().strength(family) > 0.000001D) {
                anotherFamily = true;
                break;
            }
        }
        boolean otherPsychedelic = ClientExperienceStore.psilocybin().active() || ClientExperienceStore.dmt().active()
                || ClientExperienceStore.hoMet().active() || ClientExperienceStore.alLad().active()
                || ClientExperienceStore.twoCe().active() || ClientExperienceStore.mescaline().active()
                || ClientExperienceStore.twoCb().active();
        if (anotherFamily || otherPsychedelic) {
            if (TRACE) lastDiagnostic = "mixed-family skip";
            // A merged vanilla effect has no reliable per-drug attribution.
            // Preserve other substances rather than erasing their movement optics.
            bridgeModifiers.clear();
            return;
        }
        boolean ownsLsd = ClientExperienceStore.lsd().active() || ClientEffectGraph.reservesLsdPresentation();
        Set<ResourceLocation> marked = new HashSet<>();
        collectMarkedModifiers(player, MobEffects.MOVEMENT_SPEED, ownsLsd, marked);
        collectMarkedModifiers(player, MobEffects.MOVEMENT_SLOWDOWN, ownsLsd, marked);
        if (ownsLsd) bridgeModifiers.addAll(marked);
        // Retain ownership until a final 90-tick bridge effect expires, even if
        // the experience snapshot has already gone sober. An external visible
        // potion replacing it immediately stops matching this marker.
        bridgeModifiers.retainAll(marked);
        if (bridgeModifiers.isEmpty()) {
            if (TRACE) lastDiagnostic = "no excluded modifiers; " + potionDescription(player);
            return;
        }

        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        double walking = player.getAbilities().getWalkingSpeed();
        if (movement == null || walking <= 0.0D) return;
        // Match AbstractClientPlayer's float arithmetic before removing its
        // factor, so the neutral target remains exactly 1.0 at ordinary speed.
        float walkingSpeed = player.getAbilities().getWalkingSpeed();
        double actualMovementFactor = ((float) movement.getValue() / walkingSpeed + 1.0F) * 0.5F;
        double restoredMovementFactor = ((float) withoutModifiers(movement, bridgeModifiers) / walkingSpeed + 1.0F) * 0.5F;
        if (actualMovementFactor <= 0.0D || !Double.isFinite(actualMovementFactor)) return;
        // The original modifier includes flight, sprint, bow and other vanilla
        // inputs. Replace only its movement-attribute factor before Minecraft
        // interpolates the target. Keep the user's FOV-effects scale and changes
        // made by another event listener; never mutate gameplay attributes.
        double original = event.getFovModifier();
        double corrected = original * restoredMovementFactor / actualMovementFactor;
        double scale = minecraft.options.fovEffectScale().get();
        float result = (float) (event.getNewFovModifier() + (corrected - original) * scale);
        if (Float.isFinite(result)) event.setNewFovModifier(result);
        if (TRACE) lastDiagnostic = "owned=" + ownsLsd + "; originals=" + original + "; target=" + result
                + "; attribute=" + movement.getValue() + "; excluded=" + bridgeModifiers + "; " + potionDescription(player);
    }

    static String currentDiagnostic() { return lastDiagnostic; }

    private static String potionDescription(Player player) {
        return "speed=" + describe(player.getEffect(MobEffects.MOVEMENT_SPEED))
                + "; slow=" + describe(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN));
    }

    private static String describe(MobEffectInstance effect) {
        return effect == null ? "absent" : "amp" + effect.getAmplifier() + ":" + effect.getDuration()
                + ":ambient=" + effect.isAmbient() + ":visible=" + effect.isVisible() + ":icon=" + effect.showIcon();
    }

    private static void collectMarkedModifiers(Player player, Holder<MobEffect> holder, boolean ownsLsd, Set<ResourceLocation> result) {
        MobEffectInstance effect = player.getEffect(holder);
        // This exact hidden/ambient combination is emitted by the generic
        // VanillaEffectBridge. Ordinary external potions keep their own FOV.
        if (effect != null && (!effect.isAmbient() || effect.isVisible() || effect.showIcon())) return;
        AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement == null) return;
        holder.value().createModifiers(effect == null ? 0 : effect.getAmplifier(), (attribute, expected) -> {
            if (!attribute.equals(Attributes.MOVEMENT_SPEED)) return;
            // Attributes can also precede the first potion packet or outlive its
            // removal by a tick. Keep a pure LSD/previously owned ID neutral in
            // that gap; an observable visible external potion releases it above.
            if (effect == null && !ownsLsd && !bridgeModifiers.contains(expected.id())) return;
            AttributeModifier actual = movement.getModifier(expected.id());
            // Potion and attribute packets may deliver a changed amplifier on
            // different ticks. Ownership comes from the hidden bridge marker
            // and stable vanilla modifier ID, never their momentary amounts.
            if (actual != null && actual.operation() == expected.operation()) result.add(expected.id());
        });
    }

    static double withoutModifiers(AttributeInstance attribute, Set<ResourceLocation> excluded) {
        double base = attribute.getBaseValue();
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (!excluded.contains(modifier.id()) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE)
                base += modifier.amount();
        }
        double value = base;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (!excluded.contains(modifier.id()) && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                value += base * modifier.amount();
        }
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (!excluded.contains(modifier.id()) && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                value *= 1.0D + modifier.amount();
        }
        return attribute.getAttribute().value().sanitizeValue(value);
    }
}
