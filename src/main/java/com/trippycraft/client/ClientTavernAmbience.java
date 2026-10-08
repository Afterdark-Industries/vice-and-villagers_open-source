package com.trippycraft.client;

import com.trippycraft.network.TavernAmbiencePacket;
import com.trippycraft.registry.ModSounds;
import com.trippycraft.world.TavernAudioModel;
import com.trippycraft.world.TavernPlaylist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Plays the current point on a persistent, world-time tavern playlist. */
public final class ClientTavernAmbience {
    private static final int SIGNAL_TIMEOUT_TICKS = 160;

    private static TavernAmbiencePacket signal = TavernAmbiencePacket.inactive();
    private static ClientLevel signalLevel;
    private static TavernMusicSound activeSound;
    private static int activeTrackIndex = -1;
    private static int ticksSinceSignal;

    private ClientTavernAmbience() {
    }

    public static void accept(TavernAmbiencePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean tavernChanged = !sameTavern(signal, packet) || signalLevel != minecraft.level;
        signal = packet;
        signalLevel = minecraft.level;
        ticksSinceSignal = 0;
        if (!packet.active()) {
            stopCurrent(minecraft.getSoundManager());
            return;
        }
        TavernPlaylist.Selection selection = TavernPlaylist.selection(packet.playlistTick());
        if (tavernChanged || activeTrackIndex != selection.trackIndex()) {
            stopCurrent(minecraft.getSoundManager());
        }
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) {
            return;
        }
        if (minecraft.level == null || minecraft.player == null || signalLevel != minecraft.level) {
            clear();
            return;
        }
        if (!signal.active()) {
            return;
        }
        if (++ticksSinceSignal > SIGNAL_TIMEOUT_TICKS) {
            clear();
            return;
        }
        if (minecraft.level.hasChunkAt(signal.jukebox())
                && !minecraft.level.getBlockState(signal.jukebox()).is(Blocks.JUKEBOX)) {
            clear();
            return;
        }

        // MusicManager only owns vanilla background music; the streamed tavern track is managed below.
        // Repeating this while the club is audible also catches a vanilla track that tries to begin mid-song.
        if (customPlaylistAudible()) {
            minecraft.getMusicManager().stopPlaying();
        }

        SoundManager soundManager = minecraft.getSoundManager();
        if (minecraft.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F) {
            stopCurrent(soundManager);
            return;
        }

        TavernPlaylist.Selection desired = TavernPlaylist.selection(signal.playlistTick() + ticksSinceSignal);
        if (activeSound != null && activeTrackIndex != desired.trackIndex()) {
            stopCurrent(soundManager);
        }
        if (activeSound != null && soundManager.isActive(activeSound)) {
            return;
        }
        if (activeSound != null) {
            activeSound = null;
            activeTrackIndex = -1;
        }

        activeTrackIndex = desired.trackIndex();
        activeSound = new TavernMusicSound(
                ModSounds.TAVERN_TRACKS.get(activeTrackIndex).get(),
                signal.origin(),
                signal.rotation(),
                signal.jukebox(),
                desired.elapsedTicks()
        );
        soundManager.play(activeSound);
    }

    public static TavernAmbiencePacket currentSignal() {
        return signal;
    }

    /** True only within the same spatial fade where the custom nightclub playlist has audible volume. */
    public static boolean customPlaylistAudible() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!signal.active()
                || ticksSinceSignal > SIGNAL_TIMEOUT_TICKS
                || minecraft.player == null
                || signalLevel != minecraft.level
                || minecraft.options.getSoundSourceVolume(SoundSource.MUSIC) <= 0.0F) {
            return false;
        }
        return TavernAudioModel.fadeForOutsideDistance(distanceOutsideTavern(minecraft.player.position())) > 0.0F;
    }

    /** Returns the playlist-clock pulse shared by music-responsive overlays and the physical disco lights. */
    public static double bassPulse(float partialTick) {
        if (!signal.active() || ticksSinceSignal > SIGNAL_TIMEOUT_TICKS) {
            return 0.0D;
        }
        return TavernPlaylist.bassPulse(signal.playlistTick() + ticksSinceSignal, partialTick);
    }

    /** Long-form musical energy used by mescaline ribbons, palette drift and spatial breathing. */
    public static MusicStructure musicStructure(float partialTick) {
        if (!signal.active() || ticksSinceSignal > SIGNAL_TIMEOUT_TICKS || !customPlaylistAudible()) {
            return MusicStructure.silent();
        }
        long tick = signal.playlistTick() + ticksSinceSignal;
        TavernPlaylist.Structure authored = TavernPlaylist.structure(tick, partialTick);
        TavernMusicSound.MusicAnalysis live = activeSound == null
                ? TavernMusicSound.MusicAnalysis.silent()
                : activeSound.musicAnalysis();
        double bass = clamp01(authored.bassExpansion() * 0.34D + live.bass() * 0.82D);
        double harmony = clamp01(authored.harmonicShift() * 0.42D + live.harmony() * 0.72D);
        double melody = clamp01(authored.melodicFlow() * 0.38D + live.melody() * 0.78D);
        double sustain = clamp01(authored.sustain() * 0.30D + live.sustain() * 0.88D);
        double sax = clamp01(live.saxLike() * 0.92D + authored.sustain() * live.melody() * 0.24D);
        return new MusicStructure(bass, harmony, melody, sustain, sax, authored.phrasePhase(), true);
    }

    public record MusicStructure(double bassExpansion, double harmonicShift, double melodicFlow,
                                 double sustainedRibbon, double saxRibbon, double phrasePhase,
                                 boolean tavern) {
        public static MusicStructure silent() {
            return new MusicStructure(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, false);
        }
    }

    /** Mirrors the server's nightclub footprint plus ten-block audiovisual intensifier halo. */
    public static double mdmaSettingMultiplier() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!signal.active() || minecraft.player == null || signalLevel != minecraft.level) {
            return 1.0D;
        }
        return distanceOutsideTavern(minecraft.player.position()) <= 10.0D ? 1.5D : 1.0D;
    }

    public static double twoCbSettingMultiplier() {
        return mdmaSettingMultiplier();
    }

    private static double distanceOutsideTavern(Vec3 position) {
        BlockPos[] corners = {
                transformLocal(new BlockPos(0, 0, 0)),
                transformLocal(new BlockPos(9, 0, 0)),
                transformLocal(new BlockPos(0, 0, 13)),
                transformLocal(new BlockPos(9, 0, 13))
        };
        double minimumX = Double.POSITIVE_INFINITY;
        double minimumZ = Double.POSITIVE_INFINITY;
        double maximumX = Double.NEGATIVE_INFINITY;
        double maximumZ = Double.NEGATIVE_INFINITY;
        for (BlockPos corner : corners) {
            minimumX = Math.min(minimumX, corner.getX());
            minimumZ = Math.min(minimumZ, corner.getZ());
            maximumX = Math.max(maximumX, corner.getX() + 1.0D);
            maximumZ = Math.max(maximumZ, corner.getZ() + 1.0D);
        }
        double dx = axisDistance(position.x, minimumX, maximumX);
        double dy = axisDistance(position.y, signal.origin().getY(), signal.origin().getY() + 9.0D);
        double dz = axisDistance(position.z, minimumZ, maximumZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static void clear() {
        stopCurrent(Minecraft.getInstance().getSoundManager());
        signal = TavernAmbiencePacket.inactive();
        signalLevel = null;
        ticksSinceSignal = 0;
    }

    private static boolean sameTavern(TavernAmbiencePacket first, TavernAmbiencePacket second) {
        if (first.active() != second.active()) {
            return false;
        }
        if (!first.active()) {
            return true;
        }
        return first.origin().equals(second.origin())
                && first.rotation() == second.rotation()
                && first.jukebox().equals(second.jukebox());
    }

    private static BlockPos transformLocal(BlockPos local) {
        return StructureTemplate.transform(local, Mirror.NONE, signal.rotation(), BlockPos.ZERO).offset(signal.origin());
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

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static void stopCurrent(SoundManager soundManager) {
        if (activeSound != null) {
            soundManager.stop(activeSound);
            activeSound = null;
        }
        activeTrackIndex = -1;
    }
}
