package com.trippycraft.client;

import com.trippycraft.substance.heroin.HeroinEffectState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;

public final class ClientHeroinInteractionEvents {
    private ClientHeroinInteractionEvents() {
    }

    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        // Vivecraft's hit result follows the tracked controller, whereas the
        // desktop nod retarget below is based on player yaw/pitch.
        if (VivecraftCompat.isVrActive()) {
            return;
        }
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (minecraft.level == null || player == null || minecraft.gameMode == null) {
            return;
        }

        HeroinEffectState heroin = ClientExperienceStore.heroin();
        if (heroin == null || !heroin.active()) {
            return;
        }

        double nod = ClientRenderEvents.heroinNodAmount();
        double blackout = ClientRenderEvents.heroinBlackoutAmount();
        if (nod < 0.32D && blackout < 0.18D) {
            return;
        }

        BlockHitResult retargeted = retargetedBlockHit(player);
        if (retargeted.getType() != HitResult.Type.BLOCK || !player.canInteractWithBlock(retargeted.getBlockPos(), 0.0D)) {
            return;
        }

        minecraft.hitResult = retargeted;
    }

    private static BlockHitResult retargetedBlockHit(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        double reach = Math.max(1.0D, player.blockInteractionRange());
        float yaw = (float) (player.getYRot() + ClientRenderEvents.heroinInteractionYawOffset());
        float pitch = (float) (player.getXRot() + ClientRenderEvents.heroinInteractionPitchOffset());
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);
        Vec3 end = eye.add(look.scale(reach));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }
}
