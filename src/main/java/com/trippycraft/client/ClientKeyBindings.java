package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.mojang.blaze3d.platform.InputConstants;
import com.trippycraft.TrippyCraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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

    private ClientKeyBindings() {
    }

    public static void onRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TRIP_SCRUB);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
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
