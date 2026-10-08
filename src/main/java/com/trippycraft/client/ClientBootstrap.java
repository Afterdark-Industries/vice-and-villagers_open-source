package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.trippycraft.TrippyCraft;
import com.trippycraft.registry.ModMenuTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientBootstrap {
    private static final ResourceLocation EXPERIENCE_LAYER =
            ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "experience_layer");
    private static final ResourceLocation TRIP_BAR_LAYER =
            ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "trip_status_bar");

    private ClientBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ClientBootstrap::addGuiLayers);
        modEventBus.addListener(ClientBootstrap::registerMenuScreens);
        modEventBus.addListener(ClientKeyBindings::onRegister);
        modEventBus.addListener(WorldWarpPostProcessor::onRegisterReloadListeners);
        modEventBus.addListener(HoMetTrailRenderer::onRegisterReloadListeners);

        IEventBus gameEventBus = NeoForge.EVENT_BUS;
        gameEventBus.addListener(ClientRenderEvents::onCameraAngles);
        gameEventBus.addListener(EventPriority.HIGHEST, PsilocybinClientSmokeTest::beforeCameraAngles);
        gameEventBus.addListener(EventPriority.LOWEST, PsilocybinClientSmokeTest::onCameraAngles);
        gameEventBus.addListener(EventPriority.HIGHEST, LsdClientSmokeTest::beforeCameraAngles);
        gameEventBus.addListener(EventPriority.LOWEST, LsdClientSmokeTest::onCameraAngles);
        gameEventBus.addListener(ClientRenderEvents::onFov);
        gameEventBus.addListener(EventPriority.HIGHEST, LsdMovementFov::onModifier);
        gameEventBus.addListener(EventPriority.HIGHEST, PsilocybinClientSmokeTest::beforeFov);
        gameEventBus.addListener(EventPriority.LOWEST, PsilocybinClientSmokeTest::onFov);
        gameEventBus.addListener(EventPriority.HIGHEST, LsdClientSmokeTest::beforeFov);
        gameEventBus.addListener(EventPriority.LOWEST, LsdClientSmokeTest::onFov);
        gameEventBus.addListener(ClientRenderEvents::onFogColor);
        gameEventBus.addListener(ClientRenderEvents::onRenderFog);
        gameEventBus.addListener(WorldWarpPostProcessor::onRenderLevelStage);
        gameEventBus.addListener(EventPriority.HIGHEST, PsilocybinClientSmokeTest::beforeWorldEffects);
        gameEventBus.addListener(EventPriority.LOWEST, PsilocybinClientSmokeTest::afterWorldEffects);
        gameEventBus.addListener(EventPriority.HIGHEST, LsdClientSmokeTest::beforeWorldEffects);
        gameEventBus.addListener(EventPriority.LOWEST, LsdClientSmokeTest::afterWorldEffects);
        gameEventBus.addListener(TavernRaveRenderer::onRenderLevelStage);
        gameEventBus.addListener(HoMetTrailRenderer::onRenderLevelStage);
        gameEventBus.addListener(HoMetTrailRenderer::onLivingPost);
        gameEventBus.addListener(HoMetTrailRenderer::onRenderHand);
        gameEventBus.addListener(ClientBlockIllusions::onBlockHighlight);
        gameEventBus.addListener(DmtWorldGeometryRenderer::onBlockHighlight);
        gameEventBus.addListener(ClientBlockIllusions::onRenderHand);
        gameEventBus.addListener(ClientBlockIllusions::onVrRenderLevelStage);
        gameEventBus.addListener(DmtWorldGeometryRenderer::onRenderHand);
        gameEventBus.addListener(DmtWorldGeometryRenderer::onVrRenderLevelStage);
        gameEventBus.addListener(ClientEntityHallucinations::onPre);
        gameEventBus.addListener(ClientEntityHallucinations::onPost);
        gameEventBus.addListener(ClientRenderEvents::onDebugText);
        gameEventBus.addListener(ClientAudioEvents::onPlaySound);
        gameEventBus.addListener(ClientWorldSessionEvents::onLoggingOut);
        gameEventBus.addListener(ClientAlcoholDeathPresentation::onScreenInit);
        gameEventBus.addListener(AlcoholDependencyScreenOverlay::onRenderPost);
        gameEventBus.addListener(OpioidDependencyScreenOverlay::onRenderPost);
        gameEventBus.addListener(MdmaDepletionScreenOverlay::onRenderPost);
        gameEventBus.addListener(ClientKeyBindings::onClientTick);
        gameEventBus.addListener(WorldWarpPostProcessor::onClientTick);
        gameEventBus.addListener(ClientWorldHallucinations::onClientTick);
        gameEventBus.addListener(HoMetTrailRenderer::onClientTick);
        gameEventBus.addListener(HoMetClientSmokeTest::onClientTick);
        gameEventBus.addListener(GuiSmokeCheck::onClientTick);
        gameEventBus.addListener(TwoCeClientSmokeTest::onClientTick);
        gameEventBus.addListener(AlLadClientSmokeTest::onClientTick);
        gameEventBus.addListener(SalviaClientSmokeTest::onClientTick);
        gameEventBus.addListener(MescalineClientSmokeTest::onClientTick);
        gameEventBus.addListener(PsilocybinClientSmokeTest::onClientTick);
        gameEventBus.addListener(LsdClientSmokeTest::onClientTick);
        gameEventBus.addListener(LsdClientSmokeTest::onServerTick);
        gameEventBus.addListener(ClientAlcoholMovementEvents::onClientTick);
        gameEventBus.addListener(ClientCannabisMovementEvents::onClientTick);
        gameEventBus.addListener(ClientCocaineMovementEvents::onClientTick);
        gameEventBus.addListener(ClientKetamineMovementEvents::onClientTick);
        gameEventBus.addListener(ClientHeroinMovementEvents::onClientTick);
        gameEventBus.addListener(ClientTavernAmbience::onClientTick);
        gameEventBus.addListener(ClientAudioEvents::onClientTick);
        gameEventBus.addListener(ClientHeroinInteractionEvents::onInteractionKey);
    }

    private static void addGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, EXPERIENCE_LAYER, (graphics, deltaTracker) -> {
            if (ClientExperienceStore.active() && !VivecraftCompat.isVrActive()) {
                PsychedelicOverlayRenderer.render(graphics, deltaTracker);
            }
        });
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, TRIP_BAR_LAYER, (graphics, deltaTracker) -> {
            if (TripStatusBarOverlay.shouldRender()) {
                TripStatusBarOverlay.render(graphics, deltaTracker);
            }
        });
    }

    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.MORTAR_AND_PESTLE.get(), MortarAndPestleScreen::new);
        event.register(ModMenuTypes.CANNABIS_PRESS.get(), CannabisPressScreen::new);
        event.register(ModMenuTypes.CHEMISTRY_TABLE.get(), ChemistryTableScreen::new);
        event.register(ModMenuTypes.ADVANCED_CHEMISTRY_TABLE.get(), AdvancedChemistryTableScreen::new);
        event.register(ModMenuTypes.BREW_CAULDRON.get(), BrewCauldronScreen::new);
    }
}
