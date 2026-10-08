package com.trippycraft.substance.mdma;

import com.trippycraft.TrippyCraft;
import com.trippycraft.compat.NbtCompat;
import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.world.TavernNightlifeService;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/** Fictional social feedback shared by villagers and players near an active MDMA roll. */
public final class MdmaSocialEffects {
    private static final String LAST_TOUCH_KEY = "mdma_last_touch_tick";

    private MdmaSocialEffects() {
    }

    public static void tick(ServerPlayer player, MdmaEffectState state) {
        if (!state.active() || state.socialAura() < 0.08D || player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        double radius = 5.0D + Math.min(7.0D, state.socialAura() * 5.0D);
        List<Villager> nearby = level.getEntitiesOfClass(
                Villager.class,
                player.getBoundingBox().inflate(radius),
                Villager::isAlive
        );
        int particleBudget = Math.min(8, 2 + (int) Math.round(state.socialAura() * 3.0D));
        for (int index = 0; index < nearby.size(); index++) {
            Villager villager = nearby.get(index);
            if (index < particleBudget && (player.tickCount + villager.getId()) % 40 == 0) {
                level.sendParticles(ParticleTypes.HEART,
                        villager.getX(), villager.getY() + villager.getBbHeight() + 0.25D, villager.getZ(),
                        1, 0.22D, 0.18D, 0.22D, 0.0D);
                level.sendParticles(ParticleTypes.NOTE,
                        villager.getX(), villager.getY() + villager.getBbHeight() + 0.55D, villager.getZ(),
                        1, 0.28D, 0.16D, 0.28D, 0.0D);
            }
            if (state.peakWindow() && TavernNightlifeService.isNightlifeGuest(villager)) {
                villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                        new WalkTarget(player.blockPosition(), 0.48F, 2));
            }
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()) {
            return;
        }
        MdmaEffectState state = SubstanceEngine.getExperience(player.getUUID())
                .map(experience -> experience.mdmaState())
                .orElse(MdmaEffectState.inactive());
        if (!state.active() || !state.peakWindow() || state.touchAffinity() < 0.10D) {
            return;
        }
        long gameTime = player.level().getGameTime();
        CompoundTag data = NbtCompat.getOrCreateCompound(player.getPersistentData(), TrippyCraft.MOD_ID);
        long previous = data.contains(LAST_TOUCH_KEY) ? data.getLong(LAST_TOUCH_KEY) : Long.MIN_VALUE / 2L;
        if (gameTime - previous < 40L) {
            return;
        }
        data.putLong(LAST_TOUCH_KEY, gameTime);

        Entity target = event.getTarget();
        ServerLevel level = player.serverLevel();
        burst(level, player);
        burst(level, target);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, true, false, true));
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 0, true, false, true));
        }
    }

    private static void burst(ServerLevel level, Entity entity) {
        level.sendParticles(ParticleTypes.HEART,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.72D, entity.getZ(),
                7, 0.42D, 0.42D, 0.42D, 0.04D);
        level.sendParticles(ParticleTypes.NOTE,
                entity.getX(), entity.getY() + entity.getBbHeight() + 0.18D, entity.getZ(),
                4, 0.38D, 0.28D, 0.38D, 0.0D);
    }
}
