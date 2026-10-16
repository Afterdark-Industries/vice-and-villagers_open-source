package com.trippycraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/** Samples loaded terrain once per second; never requests or generates chunks. */
public final class DmtWorldInfluenceSampler {
    private static final int[][] OFFSETS = {{-3, -3}, {0, -4}, {3, -3}, {4, 0},
            {3, 3}, {0, 4}, {-3, 3}, {-4, 0}};
    private static final Sample EMPTY = new Sample(0.35D, 0.40D, 0.45D, 0.0D);
    private static Object lastLevel;
    private static BlockPos lastPosition;
    private static long lastTicks = Long.MIN_VALUE;
    private static Sample cached = EMPTY;

    private DmtWorldInfluenceSampler() {
    }

    public static Sample sample(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) return EMPTY;
        var level = minecraft.level;
        BlockPos center = minecraft.player.blockPosition();
        long ticks = level.getGameTime();
        if (lastLevel == level && lastPosition != null && center.distSqr(lastPosition) < 64.0D
                && ticks >= lastTicks && ticks - lastTicks < 20L) return cached;
        lastLevel = level;
        lastPosition = center.immutable();
        lastTicks = ticks;
        double red = 0.0D, green = 0.0D, blue = 0.0D, heights = 0.0D, heightsSquared = 0.0D;
        double colorSquares = 0.0D, directionX = 0.0D, directionZ = 0.0D;
        int samples = 0, solidSamples = 0;
        for (int[] offset : OFFSETS) {
            BlockPos column = center.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(column)) continue;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            int y = Math.min(surface - 1, center.getY() - 1);
            for (int step = 0; step < 12 && y - step >= level.getMinBuildHeight(); step++) {
                BlockPos point = new BlockPos(column.getX(), y - step, column.getZ());
                var state = level.getBlockState(point);
                if (state.isAir()) continue;
                MapColor map = state.getMapColor(level, point);
                if (map == MapColor.NONE) continue;
                int color = map.col;
                red += ((color >>> 16) & 255) / 255.0D;
                green += ((color >>> 8) & 255) / 255.0D;
                blue += (color & 255) / 255.0D;
                double luminance = (((color >>> 16) & 255) * 0.2126D
                        + ((color >>> 8) & 255) * 0.7152D + (color & 255) * 0.0722D) / 255.0D;
                colorSquares += luminance * luminance;
                double height = point.getY() - center.getY();
                heights += height;
                heightsSquared += height * height;
                boolean solid = state.isSolidRender(level, point);
                if (solid) solidSamples++;
                double structure = Math.max(-1.0D, height / 12.0D) + luminance * 0.65D + (solid ? 0.20D : 0.0D);
                double radius = Math.sqrt(offset[0] * offset[0] + offset[1] * offset[1]);
                directionX += offset[0] / radius * structure;
                directionZ += offset[1] / radius * structure;
                samples++;
                break;
            }
        }
        double relief = 0.0D, gradientX = 0.0D, gradientY = 0.0D, gradientZ = 0.0D, gradientStrength = 0.0D;
        if (samples > 0) {
            double variance = Math.max(0.0D, heightsSquared / samples - Math.pow(heights / samples, 2.0D));
            relief = Math.min(1.0D, Math.sqrt(variance) / 8.0D + solidSamples / (double) samples * 0.22D);
            red /= samples;
            green /= samples;
            blue /= samples;
            double luminance = red * 0.2126D + green * 0.7152D + blue * 0.0722D;
            double contrast = Math.sqrt(Math.max(0.0D, colorSquares / samples - luminance * luminance));
            gradientX = directionX / samples;
            gradientY = Math.max(-1.0D, Math.min(1.0D, heights / samples / 12.0D));
            gradientZ = directionZ / samples;
            gradientStrength = Math.min(1.0D, relief * 0.60D + contrast * 2.0D);
        } else {
            red = EMPTY.red();
            green = EMPTY.green();
            blue = EMPTY.blue();
        }
        // The spatial query visits only existing entity sections. Keep the
        // cached point in world coordinates so head movement cannot drag it.
        LivingEntity nearest = null;
        double nearestDistanceSquared = 24.0D * 24.0D;
        for (var entity : level.getEntities(minecraft.player, minecraft.player.getBoundingBox().inflate(24.0D),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive())) {
            double distanceSquared = entity.distanceToSqr(minecraft.player);
            if (distanceSquared < nearestDistanceSquared) {
                nearest = (LivingEntity) entity;
                nearestDistanceSquared = distanceSquared;
            }
        }
        double entityX = nearest == null ? 0.0D : nearest.getX();
        double entityY = nearest == null ? 0.0D : nearest.getY() + nearest.getBbHeight() * 0.5D;
        double entityZ = nearest == null ? 0.0D : nearest.getZ();
        double entityStrength = nearest == null ? 0.0D : 1.0D - Math.sqrt(nearestDistanceSquared) / 24.0D;
        cached = new Sample(red, green, blue, relief, gradientX, gradientY, gradientZ, gradientStrength,
                entityX, entityY, entityZ, entityStrength);
        return cached;
    }

    public static void reset() {
        lastLevel = null;
        lastPosition = null;
        lastTicks = Long.MIN_VALUE;
        cached = EMPTY;
    }

    public record Sample(double red, double green, double blue, double relief,
                         double directionX, double directionY, double directionZ, double directionStrength,
                         double attractorX, double attractorY, double attractorZ, double attractorStrength) {
        public Sample(double red, double green, double blue, double relief) {
            this(red, green, blue, relief, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
