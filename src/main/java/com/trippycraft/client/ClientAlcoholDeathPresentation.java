package com.trippycraft.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class ClientAlcoholDeathPresentation {
    private static boolean blackoutWakeUp;

    private ClientAlcoholDeathPresentation() {
    }

    public static void setBlackoutWakeUp(boolean value) {
        blackoutWakeUp = value;
    }

    public static void clear() {
        blackoutWakeUp = false;
    }

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!blackoutWakeUp || !(event.getScreen() instanceof DeathScreen)) {
            return;
        }

        String respawnText = Component.translatable("deathScreen.respawn").getString();
        for (var listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget && widget.getMessage().getString().equals(respawnText)) {
                widget.setMessage(Component.translatable("trippycraft.death.blackout.wake_up"));
                return;
            }
        }
    }
}
