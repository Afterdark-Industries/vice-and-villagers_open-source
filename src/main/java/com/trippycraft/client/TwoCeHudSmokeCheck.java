package com.trippycraft.client;

import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.twoce.TwoCeEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Actual item/network/HUD check used only by the isolated opt-in development smoke world. */
public final class TwoCeHudSmokeCheck {
    private static final String SCREENSHOT = "two-ce-250mg-hud.png";
    private static final String EXPECTED_LABEL = "2C-E 250mg";
    private static final AtomicReference<Throwable> FAILURE = new AtomicReference<>();
    private static Phase phase = Phase.IDLE;
    private static long startedNanos;
    private static int renderTicks;
    private static UUID playerId;
    private static ItemStack savedMainHand = ItemStack.EMPTY;
    private static boolean savedCreative;
    private static volatile boolean playerStateSaved;
    private static volatile boolean serverDoseApplied;
    private static volatile boolean screenshotSaved;
    private static volatile boolean cleanupQueued;
    private static volatile boolean serverRestored;

    private TwoCeHudSmokeCheck() {
    }

    /** Returns false while pending and true once the original hand/abilities and sober client are restored. */
    public static boolean tick(Minecraft mc, Consumer<String> log) {
        try {
            return advance(mc, log);
        } catch (RuntimeException exception) {
            restorePlayer(mc);
            throw exception;
        }
    }

    private static boolean advance(Minecraft mc, Consumer<String> log) {
        if (phase == Phase.DONE) return true;
        Throwable failure = FAILURE.get();
        if (failure != null) {
            restorePlayer(mc);
            throw new IllegalStateException("2C-E HUD smoke failed", failure);
        }
        if (phase != Phase.IDLE && (System.nanoTime() - startedNanos) / 1_000_000_000.0D > 35.0D) {
            restorePlayer(mc);
            throw new IllegalStateException("2C-E HUD smoke timed out at " + phase);
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return false;

        switch (phase) {
            case IDLE -> {
                startedNanos = System.nanoTime();
                playerId = mc.player.getUUID();
                mc.setScreen(null);
                require(!mc.options.hideGui, "HUD is hidden in the smoke world");
                phase = Phase.WAIT_DOSE;
                applyDose(mc);
            }
            case WAIT_DOSE -> {
                if (!serverDoseApplied || !ClientExperienceStore.twoCe().active()) return false;
                requireExpectedState(ClientExperienceStore.twoCe());
                require(TripStatusBarOverlay.shouldRender(), "Synced 2C-E state did not enable the trip HUD");
                renderTicks = 0;
                phase = Phase.WAIT_RENDER;
            }
            case WAIT_RENDER -> {
                requireExpectedState(ClientExperienceStore.twoCe());
                require(mc.screen == null && !mc.options.hideGui && TripStatusBarOverlay.shouldRender(),
                        "2C-E HUD stopped being visible before capture");
                // Give the ordinary level/HUD renderer several client ticks to present the received snapshot.
                if (++renderTicks < 6) return false;
                Path screenshot = mc.gameDirectory.toPath().resolve("screenshots").resolve(SCREENSHOT);
                require(!Files.exists(screenshot), "HUD screenshot already exists in the fresh smoke directory");
                phase = Phase.WAIT_SCREENSHOT;
                Screenshot.grab(mc.gameDirectory, SCREENSHOT, mc.getMainRenderTarget(), message -> {
                    try {
                        require(Files.isRegularFile(screenshot) && Files.size(screenshot) > 0,
                                "2C-E HUD screenshot was not written: " + message.getString());
                        screenshotSaved = true;
                    } catch (Exception exception) {
                        FAILURE.compareAndSet(null, exception);
                    }
                });
            }
            case WAIT_SCREENSHOT -> {
                requireExpectedState(ClientExperienceStore.twoCe());
                if (!screenshotSaved) return false;
                phase = Phase.WAIT_CLEANUP;
                restorePlayer(mc);
            }
            case WAIT_CLEANUP -> {
                if (!serverRestored || ClientExperienceStore.twoCe().active()
                        || !ItemStack.matches(savedMainHand, mc.player.getMainHandItem())
                        || mc.player.getAbilities().instabuild != savedCreative) return false;
                phase = Phase.DONE;
                log.accept("PASS: Actual 2C-E 250mg pill consumed; server/client snapshot preserves 250mg physical content "
                        + "at 40 effect units; HUD label '" + EXPECTED_LABEL + "' captured in screenshots/" + SCREENSHOT
                        + "; experience reset and original hand/creative flag restored.");
                return true;
            }
            case DONE -> {
                return true;
            }
        }
        return false;
    }

    private static void applyDose(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                require(player != null, "Integrated player disappeared before applying the 2C-E pill");
                savedMainHand = player.getMainHandItem().copy();
                savedCreative = player.getAbilities().instabuild;
                playerStateSaved = true;
                SubstanceEngine.reset(player);
                player.getAbilities().instabuild = false;
                player.onUpdateAbilities();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.TWO_CE_250MG.get()));
                ModItems.TWO_CE_250MG.get().use(player.level(), player, InteractionHand.MAIN_HAND);
                require(player.getMainHandItem().isEmpty(), "Survival 2C-E pill was not consumed");
                requireExpectedState(SubstanceEngine.getExperience(player.getUUID()).orElseThrow().twoCeState());
                player.containerMenu.broadcastChanges();
                serverDoseApplied = true;
            } catch (Exception exception) {
                FAILURE.compareAndSet(null, exception);
            }
        });
    }

    private static void restorePlayer(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (cleanupQueued || server == null || playerId == null) return;
        cleanupQueued = true;
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                require(player != null, "Integrated player disappeared during 2C-E HUD cleanup");
                SubstanceEngine.reset(player);
                if (playerStateSaved) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, savedMainHand.copy());
                    player.getAbilities().instabuild = savedCreative;
                    player.onUpdateAbilities();
                    player.containerMenu.broadcastChanges();
                }
                serverRestored = true;
            } catch (Exception exception) {
                FAILURE.compareAndSet(null, exception);
            }
        });
    }

    private static void requireExpectedState(TwoCeEffectState state) {
        require(state.active() && state.targetContentMilligrams() == 250.0D && state.targetDoseMilligrams() == 40.0D,
                "2C-E snapshot did not preserve the pill's 250mg content and 40 effect units");
        require(EXPECTED_LABEL.equals(TripStatusBarOverlay.twoCeDose(state, false)),
                "2C-E trip HUD did not label the physical 250mg contents");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private enum Phase {
        IDLE, WAIT_DOSE, WAIT_RENDER, WAIT_SCREENSHOT, WAIT_CLEANUP, DONE
    }
}
