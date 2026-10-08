package com.trippycraft.client;

import com.trippycraft.config.TrippyClientConfig;
import com.trippycraft.substance.mdma.MdmaEffectState;
import com.trippycraft.substance.twocb.TwoCbEffectState;
import com.trippycraft.world.TavernPlaylist;
import com.trippycraft.world.TavernAudioModel;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Streamed tavern music with a building-aware fade. The jukebox remains the
 * positional source, while volume stays full inside the footprint, begins
 * fading one block outside it, and reaches silence ten blocks away.
 */
public final class TavernMusicSound extends AbstractTickableSoundInstance {
    private final double minimumX;
    private final double minimumY;
    private final double minimumZ;
    private final double maximumX;
    private final double maximumY;
    private final double maximumZ;
    private final int startOffsetTicks;
    private volatile float mdmaBassEnhancement;
    private volatile float mdmaWarmthEnhancement;
    private volatile float mdmaMuffleEnhancement;
    private volatile float mdmaBeatEnhancement;
    private volatile float nitrousMuffleEnhancement;
    private volatile float nitrousPressureEnhancement;
    private volatile float nitrousWhooshEnhancement;
    private volatile float analyzedBass;
    private volatile float analyzedHarmony;
    private volatile float analyzedMelody;
    private volatile float analyzedSustain;
    private volatile float analyzedSax;

    public TavernMusicSound(SoundEvent soundEvent,
                            BlockPos origin,
                            Rotation rotation,
                            BlockPos jukebox,
                            int startOffsetTicks) {
        super(soundEvent, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.x = jukebox.getX() + 0.5D;
        this.y = jukebox.getY() + 0.5D;
        this.z = jukebox.getZ() + 0.5D;
        this.looping = false;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = false;
        this.startOffsetTicks = Math.max(0, startOffsetTicks);

        BlockPos[] corners = {
                transform(origin, rotation, new BlockPos(0, 0, 0)),
                transform(origin, rotation, new BlockPos(9, 0, 0)),
                transform(origin, rotation, new BlockPos(0, 0, 13)),
                transform(origin, rotation, new BlockPos(9, 0, 13))
        };
        int minimumBlockX = Integer.MAX_VALUE;
        int minimumBlockZ = Integer.MAX_VALUE;
        int maximumBlockX = Integer.MIN_VALUE;
        int maximumBlockZ = Integer.MIN_VALUE;
        for (BlockPos corner : corners) {
            minimumBlockX = Math.min(minimumBlockX, corner.getX());
            minimumBlockZ = Math.min(minimumBlockZ, corner.getZ());
            maximumBlockX = Math.max(maximumBlockX, corner.getX());
            maximumBlockZ = Math.max(maximumBlockZ, corner.getZ());
        }
        this.minimumX = minimumBlockX;
        this.minimumY = origin.getY();
        this.minimumZ = minimumBlockZ;
        this.maximumX = maximumBlockX + 1.0D;
        this.maximumY = origin.getY() + 9.0D;
        this.maximumZ = maximumBlockZ + 1.0D;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            stop();
            return;
        }
        double distance = distanceOutside(
                minecraft.player.getX(),
                minecraft.player.getY(),
                minecraft.player.getZ()
        );
        MdmaEffectState mdma = ClientExperienceStore.mdma();
        TwoCbEffectState twoCb = ClientExperienceStore.twoCb();
        float ambience = TavernAudioModel.fadeForOutsideDistance(distance);
        if (!TrippyClientConfig.audioEnabled || TrippyClientConfig.audioIntensity <= 0.0D) {
            mdmaBassEnhancement = mdmaWarmthEnhancement = mdmaMuffleEnhancement = mdmaBeatEnhancement = 0.0F;
            nitrousMuffleEnhancement = nitrousPressureEnhancement = nitrousWhooshEnhancement = 0.0F;
            volume = ambience;
            pitch = 1.0F;
            return;
        }
        double twoCbAudio = twoCb.active() ? twoCb.sensuality() * 0.30D + twoCb.saturation() * 0.10D : 0.0D;
        double fullness = (mdma.active() ? mdma.audioFullness() : 0.0D) + twoCbAudio;
        double muffle = mdma.active() ? mdma.audioMuffle() : 0.0D;
        double pulse = ClientTavernAmbience.bassPulse(0.0F);
        double beat = (mdma.active() && mdma.peakWindow() ? pulse * mdma.bassSensitivity() : 0.0D)
                + (twoCb.active() ? pulse * twoCb.bassSensitivity() * 0.78D : 0.0D);
        this.mdmaBassEnhancement = (float) Math.clamp((fullness + (twoCb.active() ? twoCb.bassSensitivity() * 0.16D : 0.0D)) / 1.55D, 0.0D, 1.45D);
        this.mdmaWarmthEnhancement = (float) Math.clamp((fullness + mdma.warmth() * 0.45D + (twoCb.active() ? twoCb.sensuality() * 0.22D : 0.0D)) / 1.70D, 0.0D, 1.35D);
        this.mdmaMuffleEnhancement = (float) Math.clamp(muffle, 0.0D, 1.25D);
        this.mdmaBeatEnhancement = (float) Math.clamp(beat / 1.50D, 0.0D, 1.35D);
        var nitrous = ClientExperienceStore.nitrous();
        double nitrousLove = nitrous.active() ? nitrous.musicAppreciation() : 0.0D;
        double nitrousMuffle = nitrous.active() ? nitrous.audioMuffle() : 0.0D;
        double nitrousCharge = nitrous.active() ? nitrous.visualPresence() + nitrous.tunnel() * 0.35D : 0.0D;
        this.nitrousMuffleEnhancement = (float) Math.clamp(nitrousMuffle * 0.85D + nitrousCharge * 0.18D, 0.0D, 1.6D);
        this.nitrousPressureEnhancement = (float) Math.clamp(nitrousCharge * 0.55D + nitrousLove * 0.35D, 0.0D, 1.8D);
        this.nitrousWhooshEnhancement = (float) Math.clamp(nitrous.active() ? 0.35D + nitrous.stretch() * 0.25D : 0.0D, 0.0D, 1.4D);
        this.volume = (float) Math.clamp(ambience * (1.0D + Math.min(2.0D, fullness) * 0.28D)
                * (1.0D - muffle * 0.30D)
                * (1.0D + Math.min(1.8D, nitrousLove) * 0.42D), 0.0D, 2.10D);
        this.pitch = (float) Math.clamp(1.0D - muffle * 0.022D + mdma.dreaminess() * 0.008D
                + (twoCb.active() ? pulse * twoCb.edgeGlow() * 0.0018D : 0.0D)
                - nitrousMuffle * 0.10D - nitrousCharge * 0.04D
                + Math.sin(minecraft.level.getGameTime() * 0.11D) * nitrousWhooshEnhancement * 0.03D,
                nitrous.active() ? 0.72D : 0.92D, nitrous.active() ? 1.08D : 1.05D);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return soundBuffers.getStream(sound.getPath(), false).thenApply(stream -> {
            try {
                return new OffsetAudioStream(stream, startOffsetTicks);
            } catch (IOException exception) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
                throw new CompletionException(exception);
            }
        });
    }

    public MusicAnalysis musicAnalysis() {
        return new MusicAnalysis(analyzedBass, analyzedHarmony, analyzedMelody, analyzedSustain, analyzedSax);
    }

    public record MusicAnalysis(float bass, float harmony, float melody, float sustain, float saxLike) {
        public static MusicAnalysis silent() {
            return new MusicAnalysis(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    private double distanceOutside(double x, double y, double z) {
        double deltaX = axisDistance(x, minimumX, maximumX);
        double deltaY = axisDistance(y, minimumY, maximumY);
        double deltaZ = axisDistance(z, minimumZ, maximumZ);
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    private static double axisDistance(double value, double minimum, double maximum) {
        if (value < minimum) {
            return minimum - value;
        }
        if (value > maximum) {
            return value - maximum;
        }
        return 0.0D;
    }

    private static BlockPos transform(BlockPos origin, Rotation rotation, BlockPos local) {
        return StructureTemplate.transform(local, Mirror.NONE, rotation, BlockPos.ZERO).offset(origin);
    }

    private final class OffsetAudioStream implements AudioStream {
        private final AudioStream delegate;
        private final float[] bassHistory;
        private final float[] muffleHistory;
        private final float[] analysisBassHistory;
        private final float[] analysisBodyHistory;
        private final float[] analysisMelodyHistory;
        private final double bassAlpha;
        private final double muffleAlpha;
        private final double analysisBodyAlpha;
        private final double analysisMelodyAlpha;
        private final boolean supportsEnhancement;
        private ByteBuffer pending;

        private OffsetAudioStream(AudioStream delegate, int offsetTicks) throws IOException {
            this.delegate = delegate;
            AudioFormat format = delegate.getFormat();
            int channels = Math.max(1, format.getChannels());
            this.bassHistory = new float[channels];
            this.muffleHistory = new float[channels];
            this.analysisBassHistory = new float[channels];
            this.analysisBodyHistory = new float[channels];
            this.analysisMelodyHistory = new float[channels];
            this.supportsEnhancement = AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding())
                    && format.getSampleSizeInBits() == 16
                    && !format.isBigEndian();
            double sampleRate = Math.max(8_000.0D, format.getSampleRate());
            this.bassAlpha = 1.0D - Math.exp(-2.0D * Math.PI * 180.0D / sampleRate);
            this.muffleAlpha = 1.0D - Math.exp(-2.0D * Math.PI * 1_650.0D / sampleRate);
            this.analysisBodyAlpha = 1.0D - Math.exp(-2.0D * Math.PI * 520.0D / sampleRate);
            this.analysisMelodyAlpha = 1.0D - Math.exp(-2.0D * Math.PI * 2_400.0D / sampleRate);
            skipTo(offsetTicks);
        }

        @Override
        public AudioFormat getFormat() {
            return delegate.getFormat();
        }

        @Override
        public ByteBuffer read(int requestedBytes) throws IOException {
            if (pending != null && pending.hasRemaining()) {
                ByteBuffer result = pending;
                pending = null;
                return enhance(result);
            }
            return enhance(delegate.read(requestedBytes));
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }

        private void skipTo(int offsetTicks) throws IOException {
            AudioFormat format = delegate.getFormat();
            long frames = Math.round(offsetTicks / (double) TavernPlaylist.TICKS_PER_SECOND * format.getFrameRate());
            long bytesRemaining = frames * format.getFrameSize();
            while (bytesRemaining > 0L) {
                int request = (int) Math.min(32_768L, bytesRemaining);
                ByteBuffer decoded = delegate.read(request);
                if (!decoded.hasRemaining()) {
                    break;
                }
                int available = decoded.remaining();
                if (available > bytesRemaining) {
                    decoded.position(decoded.position() + (int) bytesRemaining);
                    pending = decoded.slice();
                    break;
                }
                bytesRemaining -= available;
            }
        }

        private ByteBuffer enhance(ByteBuffer input) {
            if (!supportsEnhancement || input == null || !input.hasRemaining()) {
                return input;
            }
            analyze(input.duplicate().order(ByteOrder.LITTLE_ENDIAN));
            float bass = mdmaBassEnhancement;
            float warmth = mdmaWarmthEnhancement;
            float muffle = mdmaMuffleEnhancement;
            float beat = mdmaBeatEnhancement;
            float nitrousMuffle = nitrousMuffleEnhancement;
            float nitrousPressure = nitrousPressureEnhancement;
            float nitrousWhoosh = nitrousWhooshEnhancement;
            if (bass <= 0.001F && warmth <= 0.001F && muffle <= 0.001F && beat <= 0.001F
                    && nitrousMuffle <= 0.001F && nitrousPressure <= 0.001F && nitrousWhoosh <= 0.001F) {
                return input;
            }

            ByteBuffer output = input;
            if (input.isReadOnly()) {
                output = ByteBuffer.allocateDirect(input.remaining());
                output.put(input.duplicate());
                output.flip();
            }
            ByteBuffer samples = output.duplicate().order(ByteOrder.LITTLE_ENDIAN);
            int start = samples.position();
            int end = samples.limit() - Math.floorMod(samples.limit() - start, Short.BYTES);
            int channelCount = bassHistory.length;
            int sampleIndex = 0;
            double bassGain = bass * 0.62D + beat * 0.24D + nitrousPressure * 0.48D;
            double muffleMix = Math.clamp(muffle * 0.68D + nitrousMuffle * 0.72D, 0.0D, 0.92D);
            double drive = 1.0D + warmth * 0.25D + bass * 0.08D + nitrousPressure * 0.18D;
            double driveNorm = Math.tanh(drive);
            double whoosh = nitrousWhoosh * 0.12D;
            for (int offset = start; offset < end; offset += Short.BYTES) {
                int channel = sampleIndex++ % channelCount;
                double sample = samples.getShort(offset) / 32768.0D;
                bassHistory[channel] += (float) ((sample - bassHistory[channel]) * bassAlpha);
                muffleHistory[channel] += (float) ((sample - muffleHistory[channel]) * muffleAlpha);
                double shaped = sample + bassHistory[channel] * bassGain;
                shaped *= 1.0D + warmth * 0.075D;
                shaped += (sample - bassHistory[channel]) * whoosh;
                shaped = shaped * (1.0D - muffleMix) + muffleHistory[channel] * muffleMix;
                shaped = Math.tanh(shaped * drive) / Math.max(0.0001D, driveNorm);
                int encoded = (int) Math.round(Math.clamp(shaped, -1.0D, 0.999969D) * 32768.0D);
                samples.putShort(offset, (short) encoded);
            }
            return output;
        }

        private void analyze(ByteBuffer samples) {
            int start = samples.position();
            int end = samples.limit() - Math.floorMod(samples.limit() - start, Short.BYTES);
            int channelCount = analysisBassHistory.length;
            int sampleIndex = 0;
            double bassSquare = 0.0D;
            double harmonySquare = 0.0D;
            double melodySquare = 0.0D;
            double totalSquare = 0.0D;
            for (int offset = start; offset < end; offset += Short.BYTES) {
                int channel = sampleIndex++ % channelCount;
                double sample = samples.getShort(offset) / 32768.0D;
                analysisBassHistory[channel] += (float) ((sample - analysisBassHistory[channel]) * bassAlpha);
                analysisBodyHistory[channel] += (float) ((sample - analysisBodyHistory[channel]) * analysisBodyAlpha);
                analysisMelodyHistory[channel] += (float) ((sample - analysisMelodyHistory[channel]) * analysisMelodyAlpha);
                double low = analysisBassHistory[channel];
                double harmonic = analysisBodyHistory[channel] - low;
                double melodic = analysisMelodyHistory[channel] - analysisBodyHistory[channel];
                bassSquare += low * low;
                harmonySquare += harmonic * harmonic;
                melodySquare += melodic * melodic;
                totalSquare += sample * sample;
            }
            if (sampleIndex <= 0) return;
            double bass = Math.clamp(Math.sqrt(bassSquare / sampleIndex) * 5.8D, 0.0D, 1.0D);
            double harmony = Math.clamp(Math.sqrt(harmonySquare / sampleIndex) * 5.2D, 0.0D, 1.0D);
            double melody = Math.clamp(Math.sqrt(melodySquare / sampleIndex) * 5.6D, 0.0D, 1.0D);
            double total = Math.sqrt(totalSquare / sampleIndex);
            double sustainedTarget = Math.clamp((harmony * 0.52D + melody * 0.48D) * Math.min(1.0D, total * 8.0D), 0.0D, 1.0D);
            double saxTarget = Math.clamp(sustainedTarget * (melody * 0.62D + harmony * 0.58D) * (1.10D - bass * 0.28D), 0.0D, 1.0D);
            analyzedBass = smoothEnvelope(analyzedBass, (float) bass, 0.16F, 0.035F);
            analyzedHarmony = smoothEnvelope(analyzedHarmony, (float) harmony, 0.075F, 0.018F);
            analyzedMelody = smoothEnvelope(analyzedMelody, (float) melody, 0.09F, 0.022F);
            analyzedSustain = smoothEnvelope(analyzedSustain, (float) sustainedTarget, 0.035F, 0.009F);
            analyzedSax = smoothEnvelope(analyzedSax, (float) saxTarget, 0.028F, 0.007F);
        }

        private float smoothEnvelope(float current, float target, float attack, float release) {
            float alpha = target >= current ? attack : release;
            return current + (target - current) * alpha;
        }
    }
}
