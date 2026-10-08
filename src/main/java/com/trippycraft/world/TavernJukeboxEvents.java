package com.trippycraft.world;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Treats every jukebox inside a registered tavern as that tavern's playback control. */
public final class TavernJukeboxEvents {
    private TavernJukeboxEvents() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !level.getBlockState(event.getPos()).is(Blocks.JUKEBOX)) {
            return;
        }

        VillageBarData bars = level.getDataStorage().computeIfAbsent(VillageBarData.FACTORY, VillageBarData.NAME);
        Optional<VillageBarData.BarPlacement> tavern = TavernAmbienceService.findContainingBar(bars, event.getPos());
        if (tavern.isEmpty()) {
            return;
        }

        TavernPlaybackData playback = level.getDataStorage()
                .computeIfAbsent(TavernPlaybackData.FACTORY, TavernPlaybackData.NAME);
        playback.skipToNext(level.getGameTime(), tavern.get());
        TavernAmbienceService.refreshPlayers(level);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
