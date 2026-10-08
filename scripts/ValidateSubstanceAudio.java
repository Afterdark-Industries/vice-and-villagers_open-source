package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.ExperienceChannel;
import com.trippycraft.substance.SubstanceFamily;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

import javax.sound.sampled.AudioFormat;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

/** Isolated profile/routing/PCM checks; does not launch Minecraft or play audio. */
public final class ValidateSubstanceAudio {
    private static final SoundEvent SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("minecraft", "block.stone.step"));

    public static void main(String[] args) throws Exception {
        TrippyClientConfig.audioEnabled = true;
        TrippyClientConfig.audioIntensity = 0.65;
        for (String name : List.of("psilocybin", "lsd", "dmt", "salvia", "cannabis", "mdma", "twoCb",
                "mescaline", "hoMet", "alLad", "twoCe", "nitrous", "ketamine", "cocaine", "heroin", "alcohol")) {
            ClientExperienceStore.clear();
            set("active", true);
            Field field = field(name);
            Object state = activeState(field.get(null));
            field.set(null, state);
            var channels = new EnumMap<ExperienceChannel, Double>(ExperienceChannel.class);
            var families = new EnumMap<SubstanceFamily, Double>(SubstanceFamily.class);
            Class.forName(state.getClass().getName().replace("EffectState", "EffectProfile"))
                    .getMethod("contribute", state.getClass(), EnumMap.class, EnumMap.class)
                    .invoke(null, state, channels, families);
            ClientEffectSample sample = sample(channels, families);
            for (SoundSource source : List.of(SoundSource.PLAYERS, SoundSource.AMBIENT, SoundSource.MUSIC, SoundSource.RECORDS)) {
                SoundInstance sound = new SimpleSoundInstance(SOUND, source, 0.5f, 1,
                        SoundInstance.createUnseededRandom(), BlockPos.ZERO);
                var factors = ClientAudioEvents.factors(sound, sample);
                require(!factors.equals(ClientAudioEvents.AudioFactors.NEUTRAL), name + " missing " + source + " audio");
                require(Float.isFinite(factors.pitch()) && Float.isFinite(factors.volume()), name + " non-finite factors");
                PlaySoundEvent event = new PlaySoundEvent(null, sound);
                ClientAudioEvents.onPlaySound(event);
                require(event.getSound() == sound, "Sound identity changed");
                TrippyClientConfig.rendererEnabled = false;
                require(factors.equals(ClientAudioEvents.factors(sound, sample)), "Visual toggle changed audio");
                TrippyClientConfig.rendererEnabled = true;
                TrippyClientConfig.audioEnabled = false;
                require(ClientAudioEvents.factors(sound, sample).equals(ClientAudioEvents.AudioFactors.NEUTRAL), "Audio toggle ignored");
                TrippyClientConfig.audioEnabled = true;
                TrippyClientConfig.audioIntensity = 0;
                require(ClientAudioEvents.factors(sound, sample).equals(ClientAudioEvents.AudioFactors.NEUTRAL), "Zero audio intensity ignored");
                TrippyClientConfig.audioIntensity = 0.65;
                set("active", false);
                require(ClientAudioEvents.factors(sound, sample).equals(ClientAudioEvents.AudioFactors.NEUTRAL), "Sober sound stayed shaped");
                set("active", true);
            }
            TavernMusicSound tavern = tavern();
            var musicFactors = ClientAudioEvents.factors(tavern, sample);
            require(!musicFactors.equals(ClientAudioEvents.AudioFactors.NEUTRAL), name + " missing tavern route");
            PlaySoundEvent event = new PlaySoundEvent(null, tavern);
            ClientAudioEvents.onPlaySound(event);
            require(event.getSound() == tavern, "Playlist identity changed");
            System.out.println("PASS: " + name + " world/ambient/music/record/tavern routing; toggles, sober reset, identity");
        }
        ClientExperienceStore.clear();
        for (String enhancement : List.of("mdmaBassEnhancement", "mdmaWarmthEnhancement", "mdmaMuffleEnhancement",
                "mdmaBeatEnhancement", "nitrousMuffleEnhancement", "nitrousPressureEnhancement", "nitrousWhooshEnhancement")) {
            TavernMusicSound tavern = tavern();
            Field field = TavernMusicSound.class.getDeclaredField(enhancement);
            field.setAccessible(true);
            field.setFloat(tavern, 1);
            Class<?> streamClass = Class.forName(TavernMusicSound.class.getName() + "$OffsetAudioStream");
            var constructor = streamClass.getDeclaredConstructor(TavernMusicSound.class, AudioStream.class, int.class);
            constructor.setAccessible(true);
            AudioStream stream = (AudioStream) constructor.newInstance(tavern, new TestStream(), 0);
            ByteBuffer output = stream.read(4096);
            require(output.remaining() == 4096, "PCM size changed");
            require(!output.equals(new TestStream().read(4096)), enhancement + " did not process read-only PCM");
            stream.close();
        }
        System.out.println("PASS: all seven existing MDMA/2C-B/nitrous PCM controls process read-only stereo buffers");
    }

    private static TavernMusicSound tavern() {
        return new TavernMusicSound(SOUND, BlockPos.ZERO, Rotation.NONE, BlockPos.ZERO, 0);
    }

    private static Field field(String name) throws Exception {
        Field field = ClientExperienceStore.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void set(String name, Object value) throws Exception { field(name).set(null, value); }

    private static Object activeState(Object inactive) throws Exception {
        RecordComponent[] components = inactive.getClass().getRecordComponents();
        Object[] values = new Object[components.length];
        for (int i = 0; i < values.length; i++) {
            RecordComponent c = components[i];
            values[i] = c.getAccessor().invoke(inactive);
            if (c.getType() == boolean.class) values[i] = true;
            if (c.getType() == double.class) values[i] = c.getName().equals("progress") ? 0.4 : 1.5;
            if (c.getType() == int.class) values[i] = 100;
            if (c.getType().isEnum()) {
                for (Object value : c.getType().getEnumConstants()) if (value.toString().equals("PEAK")) values[i] = value;
            }
        }
        return inactive.getClass().getDeclaredConstructor(Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new)).newInstance(values);
    }

    private static ClientEffectSample sample(EnumMap<ExperienceChannel, Double> channels,
                                             EnumMap<SubstanceFamily, Double> families) throws Exception {
        ClientFamilyBlend blend = ClientFamilyBlend.from(families);
        RecordComponent[] components = ClientEffectSample.class.getRecordComponents();
        Object[] values = new Object[components.length];
        for (int i = 0; i < values.length; i++) {
            RecordComponent c = components[i];
            if (c.getType() == float.class) values[i] = c.getName().equals("time") ? 123.0f : 1.0f;
            else if (c.getType() == ClientFamilyBlend.class) values[i] = blend;
            else if (c.getType() == ClientRenderSignature.class) values[i] = ClientRenderSignature.from(blend);
            else {
                String channel = c.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(java.util.Locale.ROOT);
                try { values[i] = Math.clamp(channels.getOrDefault(ExperienceChannel.valueOf(channel), 0.0), 0, 1); }
                catch (IllegalArgumentException ignored) { values[i] = 0.0; }
            }
        }
        return (ClientEffectSample) ClientEffectSample.class.getDeclaredConstructor(Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new)).newInstance(values);
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static final class TestStream implements AudioStream {
        public AudioFormat getFormat() { return new AudioFormat(44100, 16, 2, true, false); }
        public ByteBuffer read(int bytes) {
            ByteBuffer data = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < bytes / 2; i++) data.putShort((short)(Math.sin(i * 0.27) * 12000));
            return data.flip().asReadOnlyBuffer();
        }
        public void close() { }
    }
}
