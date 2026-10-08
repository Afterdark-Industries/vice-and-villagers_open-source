package com.trippycraft.substance;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.trippycraft.TrippyCraft;
import com.trippycraft.compat.NbtCompat;
import com.trippycraft.guide.CursedRecipeCodex;
import com.trippycraft.network.ModNetwork;
import com.trippycraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SubstanceEvents {
    private static final String PLAYER_DATA_SUFFIX = "trippycraft";
    private static final String STARTER_CODEX_KEY = "received_cursed_recipe_codex";
    private static final Set<UUID> SLEEPING_PLAYERS = ConcurrentHashMap.newKeySet();

    private SubstanceEvents() {
    }

    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UUID playerId = serverPlayer.getUUID();
            if (serverPlayer.isSleeping()) {
                SLEEPING_PLAYERS.add(playerId);
            } else if (SLEEPING_PLAYERS.remove(playerId)) {
                SubstanceEngine.endTripFromSleep(serverPlayer);
            }
            SubstanceEngine.tick(serverPlayer);
        }
    }

    public static void onPlayerLoadFromFile(PlayerEvent.LoadFromFile event) {
        SubstancePersistence.load(event.getPlayerFile(PLAYER_DATA_SUFFIX)).ifPresentOrElse(
                experience -> SubstanceEngine.setExperience(event.getEntity().getUUID(), experience),
                () -> SubstanceEngine.clear(event.getEntity().getUUID())
        );
    }

    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        SubstancePersistence.save(event.getPlayerFile(PLAYER_DATA_SUFFIX), SubstanceEngine.getExperience(event.getEntity().getUUID()));
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag originalData = NbtCompat.getCompoundOrEmpty(event.getOriginal().getPersistentData(), TrippyCraft.MOD_ID);
        if (!originalData.isEmpty()) {
            event.getEntity().getPersistentData().put(TrippyCraft.MOD_ID, originalData.copy());
        }

        SubstanceEngine.getExperience(event.getOriginal().getUUID())
                .ifPresent(experience -> {
                    if (event.isWasDeath()) {
                        experience.clearAlcoholAfterDeath();
                    }
                    SubstanceEngine.setExperience(event.getEntity().getUUID(), experience);
                });
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendAlcoholDeathPresentation(serverPlayer, false);
            SubstanceEngine.sync(serverPlayer);
            giveStarterCodexIfNeeded(serverPlayer);
            CursedRecipeCodex.refreshPlayerCodices(serverPlayer);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendAlcoholDeathPresentation(serverPlayer, false);
            SubstanceEngine.sync(serverPlayer);
        }
    }

    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SubstanceEngine.sync(serverPlayer);
        }
    }

    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SLEEPING_PLAYERS.remove(serverPlayer.getUUID());
            SubstanceEngine.endTripFromSleep(serverPlayer);
        }
    }

    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        ServerPlayer serverPlayer = event.getEntity();
        SubstanceEngine.getExperience(serverPlayer.getUUID()).ifPresent(experience -> {
            boolean mdmaBlock = experience.mdmaBlocksSleep();
            boolean twoCbBlock = experience.twoCbBlocksSleep();
            if ((mdmaBlock || twoCbBlock) && experience.heroinOverridesMdmaOrTwoCbSleepBlock()) {
                event.setProblem(null);
                serverPlayer.displayClientMessage(Component.translatable("message.trippycraft.heroin_sleep_override"), true);
            } else if (mdmaBlock) {
                SLEEPING_PLAYERS.remove(serverPlayer.getUUID());
                event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
                serverPlayer.displayClientMessage(Component.translatable("message.trippycraft.mdma_no_sleep"), true);
            } else if (twoCbBlock) {
                SLEEPING_PLAYERS.remove(serverPlayer.getUUID());
                event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
                serverPlayer.displayClientMessage(Component.translatable("message.trippycraft.two_cb_no_sleep"), true);
            } else if (experience.heroinAllowsSleep()) {
                event.setProblem(null);
            } else if (experience.cocaineBlocksSleep()) {
                SLEEPING_PLAYERS.remove(serverPlayer.getUUID());
                event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
                serverPlayer.displayClientMessage(Component.translatable("message.trippycraft.cocaine_no_sleep"), true);
            } else if (experience.heroinDisruptsSleep()) {
                SLEEPING_PLAYERS.remove(serverPlayer.getUUID());
                event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);
                serverPlayer.displayClientMessage(Component.translatable("message.trippycraft.opioid_no_sleep"), true);
            }
        });
    }

    public static void onLivingEntityUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        FoodProperties food = event.getItem().get(DataComponents.FOOD);
        if (food == null) {
            return;
        }
        SubstanceEngine.getExperience(serverPlayer.getUUID())
                .ifPresent(experience -> experience.applyHeroinNutritionPenalty(serverPlayer, food));
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        SLEEPING_PLAYERS.clear();
        SubstanceEngine.clearAll();
    }

    private static void giveStarterCodexIfNeeded(ServerPlayer player) {
        CompoundTag playerData = getTrippycraftData(player);
        if (NbtCompat.getBooleanOr(playerData, STARTER_CODEX_KEY, false)) {
            return;
        }

        var codex = CursedRecipeCodex.createStack(ModItems.CURSED_RECIPE_CODEX.get(), player);
        if (!player.addItem(codex)) {
            player.drop(codex, false);
        }
        playerData.putBoolean(STARTER_CODEX_KEY, true);
    }

    private static CompoundTag getTrippycraftData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return NbtCompat.getOrCreateCompound(persistentData, TrippyCraft.MOD_ID);
    }
}
