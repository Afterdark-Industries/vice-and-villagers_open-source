package com.trippycraft.client;

import com.trippycraft.block.entity.ChemistryTableBlockEntity;
import com.trippycraft.menu.ChemistryTableMenu;
import com.trippycraft.registry.ModBlocks;
import com.trippycraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** Native menu/network/UI check used only by the isolated opt-in development smoke world. */
public final class TwoCbChemistrySmokeCheck {
    private static final BlockPos FIXTURE_POS = new BlockPos(-2, 65, 1);
    private static final String SCREENSHOT = "two-cb-chemistry-selector.png";
    private static final AtomicReference<Throwable> FAILURE = new AtomicReference<>();
    private static Phase phase = Phase.IDLE;
    private static long startedNanos;
    private static int containerId;
    private static int renderedFrames;
    private static volatile boolean checkQueued;
    private static volatile boolean fixturePlaced;
    private static volatile boolean serverSelected;
    private static volatile boolean screenshotSaved;
    private static volatile boolean fixtureRemoved;

    private TwoCbChemistrySmokeCheck() {
    }

    /** Returns false while the native check is pending and true after its fixture is removed. */
    public static boolean tick(Minecraft mc, Consumer<String> log) {
        if (phase == Phase.DONE) return true;
        Throwable failure = FAILURE.get();
        if (failure != null) throw new IllegalStateException("2C-B Chemistry selector smoke failed", failure);
        if (phase != Phase.IDLE && (System.nanoTime() - startedNanos) / 1_000_000_000.0D > 35.0D) {
            throw new IllegalStateException("2C-B Chemistry selector smoke timed out at " + phase);
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return false;

        switch (phase) {
            case IDLE -> {
                startedNanos = System.nanoTime();
                NeoForge.EVENT_BUS.addListener(TwoCbChemistrySmokeCheck::onScreenRendered);
                phase = Phase.WAIT_FIXTURE;
                placeFixture(mc);
            }
            case WAIT_FIXTURE -> {
                // Opening a menu requires the new block entity to have reached the client first.
                if (!fixturePlaced || !(mc.level.getBlockEntity(FIXTURE_POS) instanceof ChemistryTableBlockEntity)) {
                    return false;
                }
                phase = Phase.WAIT_MENU;
                openFixtureMenu(mc);
            }
            case WAIT_MENU -> {
                if (!(mc.screen instanceof ChemistryTableScreen screen)
                        || !(mc.player.containerMenu instanceof ChemistryTableMenu menu)
                        || screen.getMenu() != menu || !menu.hasAromaticInput()) return false;
                require(menu.getSelectedAromaticRecipeId() == ChemistryTableBlockEntity.AROMATIC_REFINED_OIL,
                        "Fresh Chemistry menu did not default to MDMA");
                require(mc.gameMode != null, "Client game mode is missing");
                containerId = menu.containerId;
                phase = Phase.WAIT_SELECTION;
                mc.gameMode.handleInventoryButtonClick(containerId,
                        ChemistryTableBlockEntity.AROMATIC_PHENETHYLAMINE_BASE);
            }
            case WAIT_SELECTION -> {
                ChemistryTableMenu menu = requireOpenMenu(mc);
                if (menu.getSelectedAromaticRecipeId() != ChemistryTableBlockEntity.AROMATIC_PHENETHYLAMINE_BASE) {
                    return false;
                }
                if (!serverSelected) {
                    checkServerSelection(mc);
                    return false;
                }
                renderedFrames = 0;
                phase = Phase.WAIT_RENDER;
            }
            case WAIT_RENDER -> {
                ChemistryTableMenu menu = requireOpenMenu(mc);
                require(menu.getSelectedAromaticRecipeId() == ChemistryTableBlockEntity.AROMATIC_PHENETHYLAMINE_BASE,
                        "Client lost the synchronized 2C-B selection before capture");
                if (renderedFrames < 6) return false;
                Path screenshot = mc.gameDirectory.toPath().resolve("screenshots").resolve(SCREENSHOT);
                require(!Files.exists(screenshot), "Selector screenshot already exists in the fresh smoke directory");
                phase = Phase.WAIT_SCREENSHOT;
                Screenshot.grab(mc.gameDirectory, SCREENSHOT, mc.getMainRenderTarget(), message -> {
                    try {
                        require(Files.isRegularFile(screenshot) && Files.size(screenshot) > 0,
                                "Selector screenshot was not written: " + message.getString());
                        screenshotSaved = true;
                    } catch (Exception exception) {
                        FAILURE.compareAndSet(null, exception);
                    }
                });
            }
            case WAIT_SCREENSHOT -> {
                requireOpenMenu(mc);
                if (!screenshotSaved) return false;
                mc.player.closeContainer();
                mc.setScreen(null);
                phase = Phase.WAIT_CLEANUP;
                removeFixture(mc);
            }
            case WAIT_CLEANUP -> {
                if (!fixtureRemoved || mc.screen != null || mc.player.containerMenu != mc.player.inventoryMenu
                        || !mc.level.getBlockState(FIXTURE_POS).isAir()) return false;
                phase = Phase.DONE;
                log.accept("PASS: Native Chemistry selector default MDMA -> 2C-B via inventory-button packet; "
                        + "server/client selection synchronized; UI captured in screenshots/" + SCREENSHOT + "; fixture removed.");
                return true;
            }
            case DONE -> {
                return true;
            }
        }
        return false;
    }

    private static void placeFixture(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        UUID playerId = mc.player.getUUID();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                require(player != null, "Integrated player disappeared before opening the selector");
                var level = player.serverLevel();
                require(level.getBlockState(FIXTURE_POS).isAir(), "Chemistry fixture position is occupied");
                require(level.setBlock(FIXTURE_POS, ModBlocks.CHEMISTRY_TABLE.get().defaultBlockState(), 3),
                        "Could not place the Chemistry fixture");
                require(level.getBlockEntity(FIXTURE_POS) instanceof ChemistryTableBlockEntity,
                        "Chemistry fixture has no block entity");
                ChemistryTableBlockEntity chemistry = (ChemistryTableBlockEntity) level.getBlockEntity(FIXTURE_POS);
                chemistry.setItem(ChemistryTableBlockEntity.INPUT_SLOT, new ItemStack(ModItems.AROMATIC_EXTRACT.get()));
                require(chemistry.aromaticRecipeId() == ChemistryTableBlockEntity.AROMATIC_REFINED_OIL,
                        "Fresh server Chemistry fixture did not default to MDMA");
                require(chemistry.getItem(ChemistryTableBlockEntity.FUEL_SLOT).isEmpty(), "Fixture unexpectedly contains fuel");
                fixturePlaced = true;
            } catch (Exception exception) {
                FAILURE.compareAndSet(null, exception);
            }
        });
    }

    private static void openFixtureMenu(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        UUID playerId = mc.player.getUUID();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                require(player != null, "Integrated player disappeared before opening the selector");
                require(player.serverLevel().getBlockEntity(FIXTURE_POS) instanceof ChemistryTableBlockEntity,
                        "Server Chemistry fixture disappeared before menu opening");
                ChemistryTableBlockEntity chemistry = (ChemistryTableBlockEntity) player.serverLevel().getBlockEntity(FIXTURE_POS);
                player.openMenu(chemistry, buffer -> buffer.writeBlockPos(FIXTURE_POS));
                require(player.containerMenu instanceof ChemistryTableMenu, "Server did not open the actual Chemistry menu");
            } catch (Exception exception) {
                FAILURE.compareAndSet(null, exception);
            }
        });
    }

    private static void checkServerSelection(Minecraft mc) {
        if (checkQueued) return;
        checkQueued = true;
        var server = mc.getSingleplayerServer();
        UUID playerId = mc.player.getUUID();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                require(player != null, "Integrated player disappeared while checking selection");
                require(player.containerMenu instanceof ChemistryTableMenu && player.containerMenu.containerId == containerId,
                        "Server Chemistry container changed during the network check");
                require(player.serverLevel().getBlockEntity(FIXTURE_POS) instanceof ChemistryTableBlockEntity,
                        "Server Chemistry fixture disappeared");
                ChemistryTableBlockEntity chemistry = (ChemistryTableBlockEntity) player.serverLevel().getBlockEntity(FIXTURE_POS);
                ChemistryTableMenu menu = (ChemistryTableMenu) player.containerMenu;
                if (chemistry.aromaticRecipeId() == ChemistryTableBlockEntity.AROMATIC_PHENETHYLAMINE_BASE
                        && menu.getSelectedAromaticRecipeId() == ChemistryTableBlockEntity.AROMATIC_PHENETHYLAMINE_BASE) {
                    require(chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).is(ModItems.AROMATIC_EXTRACT.get())
                                    && chemistry.getItem(ChemistryTableBlockEntity.INPUT_SLOT).getCount() == 1
                                    && chemistry.getItem(ChemistryTableBlockEntity.FUEL_SLOT).isEmpty()
                                    && chemistry.getItem(ChemistryTableBlockEntity.OUTPUT_SLOT).isEmpty(),
                            "Selector fixture consumed ingredients without fuel");
                    serverSelected = true;
                }
            } catch (Exception exception) {
                FAILURE.compareAndSet(null, exception);
            } finally {
                checkQueued = false;
            }
        });
    }

    private static void removeFixture(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        UUID playerId = mc.player.getUUID();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                require(player != null, "Integrated player disappeared during fixture cleanup");
                player.closeContainer();
                var level = player.serverLevel();
                require(level.getBlockEntity(FIXTURE_POS) instanceof ChemistryTableBlockEntity,
                        "Chemistry fixture disappeared before cleanup");
                ((ChemistryTableBlockEntity) level.getBlockEntity(FIXTURE_POS)).clearContent();
                require(level.setBlock(FIXTURE_POS, Blocks.AIR.defaultBlockState(), 3), "Could not remove Chemistry fixture");
                fixtureRemoved = true;
            } catch (Exception exception) {
                FAILURE.compareAndSet(null, exception);
            }
        });
    }

    private static ChemistryTableMenu requireOpenMenu(Minecraft mc) {
        require(mc.screen instanceof ChemistryTableScreen && mc.player.containerMenu instanceof ChemistryTableMenu
                        && mc.player.containerMenu.containerId == containerId,
                "Native Chemistry screen or menu closed before the check completed");
        ChemistryTableMenu menu = (ChemistryTableMenu) mc.player.containerMenu;
        require(menu.hasAromaticInput(), "Client Chemistry screen lost the Aromatic Extract selector");
        return menu;
    }

    private static void onScreenRendered(ScreenEvent.Render.Post event) {
        if (phase == Phase.WAIT_RENDER && event.getScreen() instanceof ChemistryTableScreen screen
                && screen.getMenu().containerId == containerId
                && screen.getMenu().getSelectedAromaticRecipeId() == ChemistryTableBlockEntity.AROMATIC_PHENETHYLAMINE_BASE) {
            renderedFrames++;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private enum Phase {
        IDLE, WAIT_FIXTURE, WAIT_MENU, WAIT_SELECTION, WAIT_RENDER, WAIT_SCREENSHOT, WAIT_CLEANUP, DONE
    }
}
