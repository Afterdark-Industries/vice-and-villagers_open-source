package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.config.TrippyConfig;
import com.trippycraft.substance.PsychedelicBlendState;
import com.trippycraft.substance.CompositeEffectResolver;
import com.trippycraft.substance.cannabis.CannabisEffectState;
import com.trippycraft.substance.heroin.HeroinEffectState;
import com.trippycraft.substance.ketamine.KetamineEffectState;
import com.trippycraft.substance.salvia.SalviaEffectState;
import com.trippycraft.substance.twocb.TwoCbEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

public final class ClientAudioEvents {
    private static boolean wasShaping;

    private ClientAudioEvents() {
    }

    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }
        if (!(sound instanceof TavernMusicSound)
                && sound.getSource() == SoundSource.MUSIC
                && ClientTavernAmbience.customPlaylistAudible()) {
            event.setSound(null);
            return;
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) return;
        boolean enabled = minecraft.level != null && TrippyClientConfig.audioEnabled
                && TrippyClientConfig.audioIntensity > 0.0D && ClientExperienceStore.active();
        if (!enabled && !wasShaping) return;
        ClientEffectSample sample = enabled ? ClientEffectGraph.audioSample(1.0F) : null;
        var engine = minecraft.getSoundManager().soundEngine;
        // Recompute from the original sound each tick: ongoing music responds to
        // new doses, effects fade out, and tickable/streamed sounds retain identity.
        engine.instanceToChannel.forEach((sound, handle) -> {
            if (sourceScale(sound.getSource()) == 0.0D) return;
            AudioFactors factors = enabled ? factors(sound, sample) : AudioFactors.NEUTRAL;
            float pitch = (float) clamp(sound.getPitch() * factors.pitch(), 0.5D, 2.0D);
            float volume = engine.calculateVolume(sound.getVolume() * factors.volume(), sound.getSource());
            // ChannelHandle schedules OpenAL changes on Minecraft's audio thread.
            handle.execute(channel -> {
                channel.setPitch(pitch);
                channel.setVolume(volume);
            });
        });
        // One neutral update restores still-playing sounds when effects are disabled or end.
        wasShaping = enabled;
    }

    static AudioFactors factors(SoundInstance sound, ClientEffectSample sample) {
        if (!TrippyClientConfig.audioEnabled || TrippyClientConfig.audioIntensity <= 0.0D
                || !ClientExperienceStore.active() || sourceScale(sound.getSource()) == 0.0D) {
            return AudioFactors.NEUTRAL;
        }
        boolean tavern = sound instanceof TavernMusicSound;
        CannabisEffectState cannabis = ClientExperienceStore.cannabis();
        KetamineEffectState ketamine = ClientExperienceStore.ketamine();
        HeroinEffectState heroin = ClientExperienceStore.heroin();
        TwoCbEffectState twoCb = ClientExperienceStore.twoCb();
        ClientRenderSignature signature = sample.signature();
        double scale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource());
        double phase = phase(sound.getLocation().toString(), sample.time());
        double wobble = Math.sin(sample.time() * 0.021D + phase) * sample.timeDilation() * signature.temporalSlip();
        double focus = Math.sin(sample.time() * 0.110D + phase * 0.7D) * sample.stimulation() * signature.edgeSharpness();
        double looseness = Math.sin(sample.time() * 0.037D + phase * 1.3D) * signature.motorLooseness();

        double pitchFactor = 1.0D
                + scale * 0.045D * focus
                + scale * 0.030D * looseness
                - scale * 0.040D * sample.sedation() * signature.narrowing()
                - scale * 0.030D * wobble;
        double volumeFactor = 1.0D
                - scale * 0.090D * sample.sedation() * signature.narrowing()
                - scale * 0.055D * sample.dissociation() * signature.coolDistance()
                + scale * 0.030D * sample.anxiety() * signature.edgeSharpness()
                + scale * 0.020D * sample.euphoria() * signature.warmth();
        String soundName = sound.getLocation().toString();
        var psilocybin = ClientExperienceStore.psilocybin();
        var lsd = ClientExperienceStore.lsd();
        var dmt = ClientExperienceStore.dmt();
        SalviaEffectState salvia = ClientExperienceStore.salvia();
        PsychedelicBlendState blend = CompositeEffectResolver.resolve(psilocybin, lsd, dmt, ClientExperienceStore.hoMet(), ClientExperienceStore.alLad(), ClientExperienceStore.twoCe());
        if (psilocybin.active() && TrippyConfig.enablePsilocybinEffects) {
            double psiloScale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource()) * blend.psilocybinAudioGain();
            pitchFactor += Math.sin(sample.time() * 0.018D * psilocybin.signature().waveRhythm() + phase) * psilocybin.audioDepth() * psiloScale * 0.035D;
            volumeFactor += psilocybin.environment().cave() * psilocybin.audioDepth() * psiloScale * 0.025D;
            volumeFactor -= psilocybin.environment().water() * psilocybin.audioDepth() * psiloScale * 0.080D;
            volumeFactor += psilocybin.environment().forest() * psilocybin.intensity() * psiloScale * 0.018D;
        }
        if (lsd.active() && TrippyConfig.enableLsdEffects) {
            double lsdScale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource()) * blend.lsdAudioGain();
            double edge = Math.sin(sample.time() * (0.090D + lsd.signature().electricSharpness() * 0.035D) + phase * 1.9D);
            double prism = Math.sin(sample.time() * 0.031D + lsd.signature().prismSplit() * 5.4D + phase);
            pitchFactor += edge * lsd.edgeHum() * lsdScale * 0.018D;
            pitchFactor += prism * lsd.realityFold() * lsdScale * 0.010D;
            volumeFactor += lsd.electricBloom() * lsdScale * 0.010D;
            volumeFactor -= lsd.skyVortex() * lsdScale * 0.012D;
        }
        if (dmt.active() && dmt.hasAuthoredEffects() && TrippyConfig.enableDmtEffects) {
            double dmtScale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource()) * blend.dmtAudioGain();
            double tunnel = Math.sin(sample.time() * (0.070D + dmt.signature().tunnelSpeed() * 0.030D) + phase * 2.4D);
            double shimmer = Math.sin(sample.time() * (0.180D + dmt.signature().colorStrobe() * 0.080D) + phase * 0.9D);
            pitchFactor += tunnel * dmt.tunnelDepth() * dmtScale * 0.016D;
            pitchFactor += shimmer * dmt.chromaticBurst() * dmtScale * 0.010D;
            volumeFactor -= dmt.egoLoss() * dmtScale * 0.020D;
            volumeFactor += dmt.chrysanthemum() * dmtScale * 0.008D;
        }
        if (!tavern && twoCb.active() && TrippyConfig.enableTwoCbEffects) {
            double twoCbScale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource());
            double edge = clamp(twoCb.edgeGlow() / 3.0D, 0.0D, 1.0D);
            double geometry = clamp(twoCb.surfaceGeometry() / 3.0D, 0.0D, 1.0D);
            double pulse = ClientTavernAmbience.twoCbSettingMultiplier() > 1.0D
                    ? ClientTavernAmbience.bassPulse(1.0F)
                    : Math.pow(Math.max(0.0D, Math.sin(sample.time() * 0.155D)), 10.0D) * 0.52D;
            double snap = Math.sin(sample.time() * 0.31D + phase * 2.2D);
            pitchFactor += (snap * edge * 0.010D + pulse * twoCb.bassSensitivity() * 0.006D) * twoCbScale;
            volumeFactor += (twoCb.sensuality() * 0.010D + pulse * geometry * 0.018D) * twoCbScale;
        }
        var mdma = ClientExperienceStore.mdma();
        if (!tavern && mdma.active()) {
            double mdmaScale = scale;
            pitchFactor += (mdma.dreaminess() * 0.008D - mdma.audioMuffle() * 0.022D) * mdmaScale;
            volumeFactor += (mdma.audioFullness() * 0.028D - mdma.audioMuffle() * 0.080D) * mdmaScale;
        }
        var mescaline = ClientExperienceStore.mescaline();
        if (mescaline.active()) {
            // Mescaline deliberately omits the generic family presentation, so
            // its sound needs an authored route as well as its world shader.
            double flow = Math.sin(sample.time() * 0.018D + phase);
            pitchFactor += flow * mescaline.organicFlow() * scale * 0.014D;
            volumeFactor += (mescaline.bodyWarmth() * 0.012D
                    + mescaline.musicSensitivity() * ClientTavernAmbience.bassPulse(1.0F) * 0.018D) * scale;
        }
        if (salvia.active() && TrippyConfig.enableSalviaEffects) {
            double salviaScale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource()) * (TrippyConfig.salviaAccessibilityMode ? 0.65D : 1.0D);
            double page = Math.sin(sample.time() * (0.040D + salvia.signature().recursionBias() * 0.018D) + phase * 1.4D);
            double pressure = Math.sin(sample.time() * (0.014D + salvia.signature().observerBias() * 0.010D) + phase * 0.4D);
            double aftershock = Math.sin(sample.time() * (0.17D + salvia.signature().aftershock() * 0.08D) + phase * 0.8D);
            pitchFactor += page * salvia.loopSeam() * salviaScale * 0.012D;
            pitchFactor -= salvia.sceneReplacement() * salviaScale * 0.010D;
            pitchFactor -= pressure * salvia.observerPressure() * salviaScale * 0.008D;
            volumeFactor -= salvia.observerPressure() * salviaScale * 0.028D;
            volumeFactor -= salvia.cutoutPresence() * salviaScale * 0.018D;
            volumeFactor -= salvia.environment().stillness() * salvia.sceneReplacement() * salviaScale * 0.012D;
            volumeFactor += aftershock * salvia.reentryConfusion() * salviaScale * 0.012D;
        }
        if (cannabis.active()) {
            double cannabisScale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource()) * cannabisAudioLoad(cannabis);
            double glue = clamp(cannabis.audioGlue() * 0.018D, 0.0D, 1.0D);
            double tunnel = clamp(cannabis.focusTunnel() * 0.012D, 0.0D, 1.0D);
            double drag = clamp(cannabis.temporalDrag() * 0.016D, 0.0D, 1.0D);
            boolean footsteps = isFootstepSound(soundName, sound.getSource());
            boolean ambient = isAmbientSound(soundName, sound.getSource());
            double inhale = Math.sin(sample.time() * (0.008D + cannabis.breathingPulse() * 0.0007D) + phase * 0.35D);
            pitchFactor -= cannabisScale * (0.018D + glue * 0.42D + drag * 0.18D);
            pitchFactor -= inhale * cannabisScale * 0.006D;
            if (footsteps) {
                pitchFactor -= cannabisScale * (0.010D + glue * 0.20D);
                volumeFactor += cannabisScale * 0.010D;
            }
            if (ambient) {
                volumeFactor += cannabisScale * (0.014D + glue * 0.22D);
            } else {
                volumeFactor -= cannabisScale * tunnel * 0.035D;
            }
        }
        var nitrous = ClientExperienceStore.nitrous();
        // TavernMusicSound already applies its dedicated nitrous and 2C-B processing.
        boolean nitrousActive = !tavern && nitrous.active();
        if (nitrousActive) {
            double nitrousScale = Math.max(0.35D, ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity));
            double muffle = Math.max(0.0D, nitrous.audioMuffle());
            double musicLove = Math.max(0.0D, nitrous.musicAppreciation());
            boolean music = sound.getSource() == SoundSource.MUSIC || sound.getSource() == SoundSource.RECORDS;
            double whoosh = Math.sin(sample.time() * 0.14D + phase * 2.1D);
            pitchFactor -= nitrousScale * (0.12D + muffle * 0.10D);
            pitchFactor += whoosh * nitrousScale * 0.045D;
            if (music) {
                volumeFactor += nitrousScale * (0.28D + musicLove * 0.35D);
                pitchFactor -= nitrousScale * 0.06D;
            } else {
                volumeFactor -= nitrousScale * (0.16D + muffle * 0.18D);
                if (isAmbientSound(soundName, sound.getSource()) || isFootstepSound(soundName, sound.getSource())) {
                    volumeFactor -= nitrousScale * 0.10D;
                    pitchFactor -= nitrousScale * 0.05D;
                }
            }
        }
        if (ketamine.active()) {
            double ketamineScale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource()) * ketamineAudioLoad(ketamine);
            double muffle = clamp(ketamine.audioMuffle() * 0.22D, 0.0D, 1.0D);
            double distance = clamp(ketamine.dissociation() * 0.18D + ketamine.depthMismatch() * 0.12D, 0.0D, 1.0D);
            double floatiness = clamp(ketamine.timeSlip() * 0.14D + ketamine.bodyFloat() * 0.12D, 0.0D, 1.0D);
            double fade = clamp(ketamine.uiFade() * 0.14D + ketamine.bodyEcho() * 0.06D, 0.0D, 1.0D);
            boolean footsteps = isFootstepSound(soundName, sound.getSource());
            boolean ambient = isAmbientSound(soundName, sound.getSource());
            double drag = Math.sin(sample.time() * (0.012D + floatiness * 0.010D) + phase * 0.22D);
            pitchFactor -= ketamineScale * (0.018D + muffle * 0.060D + distance * 0.024D + fade * 0.010D);
            pitchFactor -= drag * ketamineScale * floatiness * 0.010D;
            volumeFactor -= ketamineScale * (0.050D + muffle * 0.16D + distance * 0.12D + fade * 0.10D);
            if (footsteps) {
                pitchFactor -= ketamineScale * 0.012D;
                volumeFactor -= ketamineScale * (0.040D + distance * 0.070D);
            }
            if (ambient) {
                volumeFactor -= ketamineScale * (0.014D + muffle * 0.030D + distance * 0.040D);
            } else {
                volumeFactor -= ketamineScale * distance * 0.024D;
            }
        }
        if (heroin.active()) {
            double heroinScale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource()) * heroinAudioLoad(heroin);
            double warmth = clamp(heroin.warmth() * 0.16D + heroin.euphoria() * 0.08D, 0.0D, 1.0D);
            double muffle = clamp(heroin.softFocus() * 0.15D + heroin.sedation() * 0.10D + heroin.nodding() * 0.12D, 0.0D, 1.0D);
            double nod = clamp(heroin.microSleep() * 0.14D + heroin.nodding() * 0.12D + ClientRenderEvents.heroinNodAmount() * 0.38D, 0.0D, 1.0D);
            boolean ambient = isAmbientSound(soundName, sound.getSource());
            boolean footsteps = isFootstepSound(soundName, sound.getSource());
            double blanket = Math.sin(sample.time() * 0.010D + phase * 0.25D);
            pitchFactor -= heroinScale * (0.014D + muffle * 0.052D + nod * 0.034D);
            pitchFactor -= blanket * heroinScale * warmth * 0.008D;
            volumeFactor -= heroinScale * (0.030D + muffle * 0.105D + nod * 0.074D);
            if (ambient) {
                volumeFactor += heroinScale * (0.024D + warmth * 0.060D);
                pitchFactor -= heroinScale * warmth * 0.008D;
            } else if (footsteps) {
                volumeFactor -= heroinScale * (0.036D + nod * 0.074D);
            }
        }
        var alcohol = ClientExperienceStore.alcohol();
        if (alcohol.active() && TrippyConfig.enableAlcoholEffects) {
            double alcoholScale = ClientEffectGraph.saturate(TrippyClientConfig.audioIntensity) * sourceScale(sound.getSource());
            double safe = Math.max(1.0D, TrippyConfig.alcoholMaxVisualIntensity);
            double alcoholFocus = ClientEffectGraph.saturate(alcohol.focusBlur() / safe);
            double sway = ClientEffectGraph.saturate(alcohol.vestibularSway() / safe);
            double sedationLoad = ClientEffectGraph.saturate(alcohol.sedation() / safe);
            double wobbleA = Math.sin(sample.time() * 0.032D + alcohol.activeStandardDrinks() * 1.7D + phase);
            double wobbleB = Math.sin(sample.time() * 0.093D + phase * 0.4D);
            pitchFactor += (wobbleA * sway * 0.018D - wobbleB * alcoholFocus * 0.010D - sedationLoad * 0.018D) * alcoholScale;
            volumeFactor -= (sedationLoad * 0.080D + alcoholFocus * 0.030D) * alcoholScale;
            volumeFactor += Math.sin(sample.time() * 0.019D + phase) * sway * alcoholScale * 0.014D;
        }

        double pitchMin = nitrousActive ? 0.52D : 0.82D;
        double pitchMax = nitrousActive ? 1.28D : 1.18D;
        double volumeMin = nitrousActive ? 0.38D : 0.72D;
        double volumeMax = nitrousActive ? 1.85D : 1.08D;
        return new AudioFactors((float) clamp(pitchFactor, pitchMin, pitchMax),
                (float) clamp(volumeFactor, volumeMin, volumeMax));
    }

    record AudioFactors(float pitch, float volume) {
        static final AudioFactors NEUTRAL = new AudioFactors(1.0F, 1.0F);
    }

    private static double sourceScale(SoundSource source) {
        if (source == SoundSource.MUSIC || source == SoundSource.RECORDS) {
            return 0.32D;
        }
        if (source == SoundSource.MASTER || source == SoundSource.VOICE) {
            return 0.0D;
        }
        if (source == SoundSource.WEATHER || source == SoundSource.AMBIENT) {
            return 0.46D;
        }
        return 0.72D;
    }

    private static double phase(String name, float time) {
        int hash = name.hashCode();
        double base = (hash & 0xFFFF) / 65535.0D;
        return base * Math.PI * 2.0D + time * 0.003D;
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return 1.0D;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static double cannabisAudioLoad(CannabisEffectState cannabis) {
        if (cannabis == null || !cannabis.active()) {
            return 0.0D;
        }
        double load = Math.log1p(Math.max(0.0D, cannabis.audioGlue() + cannabis.temporalDrag() * 0.4D)) / Math.log1p(7.5D);
        return clamp(load, 0.0D, 1.0D);
    }

    private static double ketamineAudioLoad(KetamineEffectState ketamine) {
        if (ketamine == null || !ketamine.active()) {
            return 0.0D;
        }
        double load = Math.log1p(Math.max(0.0D, ketamine.audioMuffle() + ketamine.dissociation() * 0.5D)) / Math.log1p(7.0D);
        return clamp(load, 0.0D, 1.0D);
    }

    private static double heroinAudioLoad(HeroinEffectState heroin) {
        if (heroin == null || !heroin.active()) {
            return 0.0D;
        }
        double load = Math.log1p(Math.max(0.0D, heroin.softFocus() + heroin.sedation() * 0.55D + heroin.warmth() * 0.30D)) / Math.log1p(7.0D);
        load += clamp((heroin.nodding() + heroin.microSleep()) * 0.050D, 0.0D, 0.24D);
        return clamp(load, 0.0D, 1.0D);
    }

    private static boolean isFootstepSound(String soundName, SoundSource source) {
        if (source != SoundSource.PLAYERS && source != SoundSource.BLOCKS) {
            return false;
        }
        return soundName.contains("step")
                || soundName.contains("foot")
                || soundName.contains("walk")
                || soundName.contains("gravel")
                || soundName.contains("wood")
                || soundName.contains("grass");
    }

    private static boolean isAmbientSound(String soundName, SoundSource source) {
        return source == SoundSource.AMBIENT
                || source == SoundSource.WEATHER
                || soundName.contains("ambient")
                || soundName.contains("wind")
                || soundName.contains("cave")
                || soundName.contains("rain")
                || soundName.contains("water");
    }

}
