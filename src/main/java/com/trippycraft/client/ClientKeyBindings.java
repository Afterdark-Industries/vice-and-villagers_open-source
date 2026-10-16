package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.mojang.blaze3d.platform.InputConstants;
import com.trippycraft.config.TrippyClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ClientKeyBindings {
    public static final KeyMapping OPEN_TRIP_SCRUB = new KeyMapping(
            "key.trippycraft.open_trip_scrub",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.trippycraft"
    );

    public static final KeyMapping TOGGLE_DMT_FRACTAL_WORLD = new KeyMapping(
            "key.trippycraft.toggle_dmt_fractal_world",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.trippycraft"
    );

    private ClientKeyBindings() {
    }

    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TRIP_SCRUB);
        event.register(TOGGLE_DMT_FRACTAL_WORLD);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (TOGGLE_DMT_FRACTAL_WORLD.consumeClick()) {
            boolean enabled = !TrippyClientConfig.dmtFractalWorld;
            TrippyClientConfig.setDmtFractalWorld(enabled);
            mc.player.displayClientMessage(Component.translatable("message.trippycraft.dmt_fractal_world",
                    Component.translatable(enabled ? "options.on" : "options.off")), true);
        }
        while (OPEN_TRIP_SCRUB.consumeClick()) {
            if (ClientExperienceStore.psilocybin().active() || ClientExperienceStore.hoMet().active()
                    || ClientExperienceStore.alLad().active() || ClientExperienceStore.twoCe().active()
                    || ClientExperienceStore.nitrous().active()) {
                mc.setScreen(new TripScrubScreen());
            }
        }
    }
}
