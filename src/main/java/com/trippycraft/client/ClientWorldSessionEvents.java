package com.trippycraft.client;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

public final class ClientWorldSessionEvents {
    private ClientWorldSessionEvents() {
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientExperienceStore.clear();
        ClientAlcoholDeathPresentation.clear();
        ClientTavernAmbience.clear();
        HoMetTrailRenderer.clear();
    }
}
