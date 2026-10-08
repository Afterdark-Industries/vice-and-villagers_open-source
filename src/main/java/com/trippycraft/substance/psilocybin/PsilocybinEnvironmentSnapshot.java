package com.trippycraft.substance.psilocybin;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

public record PsilocybinEnvironmentSnapshot(
        double forest,
        double cave,
        double rain,
        double night,
        double sunriseSunset,
        double snow,
        double desert,
        double water,
        double village,
        double nether,
        double end,
        double organicBlocks,
        double artificialBlocks,
        double entities,
        double hostileEntities,
        double moving,
        double stillness,
        double lowHealth,
        double hunger,
        double light,
        double depth
) {
    public static PsilocybinEnvironmentSnapshot capture(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        double light = PsilocybinDoseCurve.clamp(level.getMaxLocalRawBrightness(pos) / 15.0D, 0.0D, 1.0D);
        double depth = PsilocybinDoseCurve.clamp((62.0D - pos.getY()) / 70.0D, 0.0D, 1.0D);
        double cave = PsilocybinDoseCurve.clamp(depth * 0.65D + (1.0D - light) * 0.45D, 0.0D, 1.0D);
        double rain = level.isRainingAt(pos) ? 1.0D : 0.0D;
        double snow = level.getBiome(pos).value().getPrecipitationAt(pos) == net.minecraft.world.level.biome.Biome.Precipitation.SNOW ? 1.0D : 0.0D;
        double night = captureNight(level);
        double sunriseSunset = captureSunriseSunset(level);
        double water = player.isInWaterOrBubble() || level.getFluidState(pos).is(Fluids.WATER) ? 1.0D : 0.0D;
        double nether = level.dimension() == Level.NETHER ? 1.0D : 0.0D;
        double end = level.dimension() == Level.END ? 1.0D : 0.0D;
        BlockSample blockSample = sampleBlocks(level, pos);
        EntitySample entitySample = sampleEntities(player);
        double forest = PsilocybinDoseCurve.clamp(blockSample.organic() * 0.90D + entitySample.passive() * 0.12D, 0.0D, 1.0D);
        double desert = PsilocybinDoseCurve.clamp(Math.max(0.0D, level.getBiome(pos).value().getBaseTemperature() - 1.2D) / 0.9D, 0.0D, 1.0D);
        double village = level instanceof ServerLevel serverLevel && serverLevel.isCloseToVillage(pos, 2) ? 1.0D : 0.0D;
        double movement = PsilocybinDoseCurve.clamp(player.getDeltaMovement().horizontalDistance() / 0.42D, 0.0D, 1.0D);
        double healthRatio = player.getMaxHealth() <= 0.0F ? 1.0D : player.getHealth() / player.getMaxHealth();
        double hunger = 1.0D - PsilocybinDoseCurve.clamp(player.getFoodData().getFoodLevel() / 20.0D, 0.0D, 1.0D);

        return new PsilocybinEnvironmentSnapshot(
                forest,
                cave,
                rain,
                night,
                sunriseSunset,
                snow,
                desert,
                water,
                village,
                nether,
                end,
                blockSample.organic(),
                blockSample.artificial(),
                entitySample.total(),
                entitySample.hostile(),
                movement,
                1.0D - movement,
                1.0D - PsilocybinDoseCurve.clamp(healthRatio, 0.0D, 1.0D),
                hunger,
                light,
                depth
        );
    }

    public PsilocybinEnvironmentSnapshot blend(PsilocybinEnvironmentSnapshot next, double alpha) {
        double a = PsilocybinDoseCurve.clamp(alpha, 0.0D, 1.0D);
        return new PsilocybinEnvironmentSnapshot(
                lerp(forest, next.forest, a),
                lerp(cave, next.cave, a),
                lerp(rain, next.rain, a),
                lerp(night, next.night, a),
                lerp(sunriseSunset, next.sunriseSunset, a),
                lerp(snow, next.snow, a),
                lerp(desert, next.desert, a),
                lerp(water, next.water, a),
                lerp(village, next.village, a),
                lerp(nether, next.nether, a),
                lerp(end, next.end, a),
                lerp(organicBlocks, next.organicBlocks, a),
                lerp(artificialBlocks, next.artificialBlocks, a),
                lerp(entities, next.entities, a),
                lerp(hostileEntities, next.hostileEntities, a),
                lerp(moving, next.moving, a),
                lerp(stillness, next.stillness, a),
                lerp(lowHealth, next.lowHealth, a),
                lerp(hunger, next.hunger, a),
                lerp(light, next.light, a),
                lerp(depth, next.depth, a)
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("forest", forest);
        tag.putDouble("cave", cave);
        tag.putDouble("rain", rain);
        tag.putDouble("night", night);
        tag.putDouble("sunriseSunset", sunriseSunset);
        tag.putDouble("snow", snow);
        tag.putDouble("desert", desert);
        tag.putDouble("water", water);
        tag.putDouble("village", village);
        tag.putDouble("nether", nether);
        tag.putDouble("end", end);
        tag.putDouble("organicBlocks", organicBlocks);
        tag.putDouble("artificialBlocks", artificialBlocks);
        tag.putDouble("entities", entities);
        tag.putDouble("hostileEntities", hostileEntities);
        tag.putDouble("moving", moving);
        tag.putDouble("stillness", stillness);
        tag.putDouble("lowHealth", lowHealth);
        tag.putDouble("hunger", hunger);
        tag.putDouble("light", light);
        tag.putDouble("depth", depth);
        return tag;
    }

    public static PsilocybinEnvironmentSnapshot load(CompoundTag tag) {
        return new PsilocybinEnvironmentSnapshot(
                NbtCompat.getDoubleOr(tag, "forest", 0.0D),
                NbtCompat.getDoubleOr(tag, "cave", 0.0D),
                NbtCompat.getDoubleOr(tag, "rain", 0.0D),
                NbtCompat.getDoubleOr(tag, "night", 0.0D),
                NbtCompat.getDoubleOr(tag, "sunriseSunset", 0.0D),
                NbtCompat.getDoubleOr(tag, "snow", 0.0D),
                NbtCompat.getDoubleOr(tag, "desert", 0.0D),
                NbtCompat.getDoubleOr(tag, "water", 0.0D),
                NbtCompat.getDoubleOr(tag, "village", 0.0D),
                NbtCompat.getDoubleOr(tag, "nether", 0.0D),
                NbtCompat.getDoubleOr(tag, "end", 0.0D),
                NbtCompat.getDoubleOr(tag, "organicBlocks", 0.0D),
                NbtCompat.getDoubleOr(tag, "artificialBlocks", 0.0D),
                NbtCompat.getDoubleOr(tag, "entities", 0.0D),
                NbtCompat.getDoubleOr(tag, "hostileEntities", 0.0D),
                NbtCompat.getDoubleOr(tag, "moving", 0.0D),
                NbtCompat.getDoubleOr(tag, "stillness", 1.0D),
                NbtCompat.getDoubleOr(tag, "lowHealth", 0.0D),
                NbtCompat.getDoubleOr(tag, "hunger", 0.0D),
                NbtCompat.getDoubleOr(tag, "light", 1.0D),
                NbtCompat.getDoubleOr(tag, "depth", 0.0D)
        );
    }

    public static PsilocybinEnvironmentSnapshot empty() {
        return new PsilocybinEnvironmentSnapshot(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0);
    }

    private static double captureNight(Level level) {
        long dayTime = level.getDayTime() % 24000L;
        if (dayTime < 12000L) {
            return 0.0D;
        }
        return PsilocybinDoseCurve.clamp(1.0D - Math.abs(dayTime - 18000L) / 6000.0D, 0.0D, 1.0D);
    }

    private static double captureSunriseSunset(Level level) {
        long dayTime = level.getDayTime() % 24000L;
        double sunrise = 1.0D - Math.abs(dayTime - 23000L) / 1800.0D;
        double sunset = 1.0D - Math.abs(dayTime - 12000L) / 1800.0D;
        return PsilocybinDoseCurve.clamp(Math.max(sunrise, sunset), 0.0D, 1.0D);
    }

    private static BlockSample sampleBlocks(Level level, BlockPos origin) {
        int organic = 0;
        int artificial = 0;
        int samples = 0;
        for (int i = 0; i < 36; i++) {
            int x = ((i * 7) % 9) - 4;
            int y = ((i * 5) % 5) - 2;
            int z = ((i * 11) % 9) - 4;
            BlockPos probe = origin.offset(x, y, z);
            if (!level.isLoaded(probe)) {
                continue;
            }
            samples++;
            BlockState state = level.getBlockState(probe);
            if (isOrganic(state)) {
                organic++;
            } else if (!state.isAir()) {
                artificial++;
            }
        }
        if (samples == 0) {
            return new BlockSample(0.0D, 0.0D);
        }
        return new BlockSample(
                PsilocybinDoseCurve.clamp(organic / 16.0D, 0.0D, 1.0D),
                PsilocybinDoseCurve.clamp(artificial / 24.0D, 0.0D, 1.0D)
        );
    }

    private static boolean isOrganic(BlockState state) {
        String blockId = String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        return state.getBlock() instanceof LeavesBlock
                || state.getBlock() instanceof MushroomBlock
                || state.getBlock() instanceof BushBlock
                || state.getBlock() instanceof VineBlock
                || state.getBlock() instanceof BedBlock
                || blockId.contains("log")
                || blockId.contains("grass")
                || blockId.contains("mushroom")
                || blockId.contains("flower")
                || blockId.contains("moss")
                || blockId.contains("mycelium");
    }

    private static EntitySample sampleEntities(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(16.0D);
        int total = 0;
        int hostile = 0;
        int passive = 0;
        for (Entity entity : player.level().getEntities(player, box, Entity::isAlive)) {
            if (entity instanceof ItemEntity) {
                total++;
                continue;
            }
            if (entity instanceof Enemy) {
                hostile++;
            }
            if (entity instanceof Animal) {
                passive++;
            }
            total++;
        }
        return new EntitySample(
                PsilocybinDoseCurve.clamp(total / 12.0D, 0.0D, 1.0D),
                PsilocybinDoseCurve.clamp(hostile / 5.0D, 0.0D, 1.0D),
                PsilocybinDoseCurve.clamp(passive / 8.0D, 0.0D, 1.0D)
        );
    }

    private static double lerp(double from, double to, double alpha) {
        return from + (to - from) * alpha;
    }

    private record BlockSample(double organic, double artificial) {
    }

    private record EntitySample(double total, double hostile, double passive) {
    }
}
