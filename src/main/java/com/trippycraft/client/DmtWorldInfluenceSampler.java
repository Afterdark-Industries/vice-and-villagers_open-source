package com.trippycraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import java.util.UUID;

/** Samples loaded terrain once per second; never requests or generates chunks. */
public final class DmtWorldInfluenceSampler {
    private static final int[][] OFFSETS = {{-3, -3}, {0, -4}, {3, -3}, {4, 0},
            {3, 3}, {0, 4}, {-3, 3}, {-4, 0}};
    private static final int[][] WALL_DIRECTIONS = {{-1, -1}, {0, -1}, {1, -1}, {1, 0},
            {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
    private static final int[][] OVERHEAD_OFFSETS = {{0, 0}, {-2, 0}, {2, 0}, {0, -2}, {0, 2}};
    private static final Sample EMPTY = new Sample(0.35D, 0.40D, 0.45D, 0.0D);
    private static Object lastLevel;
    private static BlockPos lastPosition;
    private static long lastTicks = Long.MIN_VALUE;
    private static Sample cached = EMPTY;
    private static UUID trackedEntityId;

    private DmtWorldInfluenceSampler() {
    }

    public static Sample sample(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null) return EMPTY;
        var level = minecraft.level;
        BlockPos center = minecraft.player.blockPosition();
        BlockPos eye = BlockPos.containing(minecraft.player.getX(), minecraft.player.getEyeY(), minecraft.player.getZ());
        long ticks = level.getGameTime();
        if (lastLevel == level && lastPosition != null && center.distSqr(lastPosition) < 64.0D
                && ticks >= lastTicks && ticks - lastTicks < 20L) return cached;
        if (lastLevel != level) trackedEntityId = null;
        lastLevel = level;
        lastPosition = center.immutable();
        lastTicks = ticks;
        // At most 168 block reads: 8x12 supporting, 8x4 wall, 5x8 overhead.
        Evidence evidence = new Evidence();
        for (int[] offset : OFFSETS) {
            BlockPos column = center.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(column)) continue;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            int y = Math.min(surface - 1, center.getY() - 1);
            for (int step = 0; step < 12 && y - step >= level.getMinBuildHeight(); step++) {
                BlockPos point = new BlockPos(column.getX(), y - step, column.getZ());
                if (evidence.add(level, point, eye, 1.0D)) break;
            }
        }
        for (int[] direction : WALL_DIRECTIONS) {
            for (int distance = 1; distance <= 4; distance++) {
                BlockPos point = eye.offset(direction[0] * distance, 0, direction[1] * distance);
                if (!level.hasChunkAt(point)) break;
                if (evidence.add(level, point, eye, 0.85D)) break;
            }
        }
        for (int[] offset : OVERHEAD_OFFSETS) {
            for (int height = 1; height <= 8 && eye.getY() + height < level.getMaxBuildHeight(); height++) {
                BlockPos point = eye.offset(offset[0], height, offset[1]);
                if (!level.hasChunkAt(point)) break;
                if (evidence.add(level, point, eye, 1.20D)) break;
            }
        }
        double red = EMPTY.red(), green = EMPTY.green(), blue = EMPTY.blue();
        double relief = 0.0D, gradientX = 0.0D, gradientY = 0.0D, gradientZ = 0.0D, gradientStrength = 0.0D;
        if (evidence.weight > 0.0D) {
            double variance = Math.max(0.0D, evidence.heightsSquared / evidence.weight
                    - Math.pow(evidence.heights / evidence.weight, 2.0D));
            relief = Math.min(1.0D, Math.sqrt(variance) / 8.0D + evidence.solidWeight / evidence.weight * 0.22D);
            red = evidence.red / evidence.weight;
            green = evidence.green / evidence.weight;
            blue = evidence.blue / evidence.weight;
            double luminance = red * 0.2126D + green * 0.7152D + blue * 0.0722D;
            double contrast = Math.sqrt(Math.max(0.0D, evidence.colorSquares / evidence.weight - luminance * luminance));
            gradientX = evidence.directionX / evidence.weight;
            gradientY = evidence.directionY / evidence.weight;
            gradientZ = evidence.directionZ / evidence.weight;
            double directionalPresence = Math.sqrt(gradientX * gradientX + gradientY * gradientY + gradientZ * gradientZ);
            gradientStrength = Math.min(1.0D, relief * 0.60D + contrast * 2.0D + directionalPresence * 0.35D);
        }
        // The spatial query visits only existing entity sections. Keep the
        // cached point in world coordinates so head movement cannot drag it.
        LivingEntity nearest = null;
        LivingEntity tracked = null;
        double nearestDistanceSquared = 24.0D * 24.0D;
        for (var entity : level.getEntities(minecraft.player, minecraft.player.getBoundingBox().inflate(24.0D),
                candidate -> candidate instanceof LivingEntity && candidate.isAlive())) {
            double distanceSquared = entity.distanceToSqr(minecraft.player);
            if (entity.getUUID().equals(trackedEntityId) && distanceSquared < 24.0D * 24.0D)
                tracked = (LivingEntity) entity;
            if (distanceSquared < nearestDistanceSquared) {
                nearest = (LivingEntity) entity;
                nearestDistanceSquared = distanceSquared;
            }
        }
        // Keep an existing identity through small distance-order changes. A new
        // entity must be at least 20% closer, or the old target must leave/die.
        if (tracked != null && (nearest == null
                || nearestDistanceSquared >= tracked.distanceToSqr(minecraft.player) * 0.64D)) {
            nearest = tracked;
            nearestDistanceSquared = tracked.distanceToSqr(minecraft.player);
        }
        trackedEntityId = nearest == null ? null : nearest.getUUID();
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
        trackedEntityId = null;
    }

    private static final class Evidence {
        private double weight, red, green, blue, heights, heightsSquared, solidWeight, colorSquares;
        private double directionX, directionY, directionZ;

        private boolean add(ClientLevel level, BlockPos point, BlockPos eye, double sampleWeight) {
            var state = level.getBlockState(point);
            if (state.isAir()) return false;
            MapColor map = state.getMapColor(level, point);
            if (map == MapColor.NONE) return false;
            double r = ((map.col >>> 16) & 255) / 255.0D;
            double g = ((map.col >>> 8) & 255) / 255.0D;
            double b = (map.col & 255) / 255.0D;
            double luminance = r * 0.2126D + g * 0.7152D + b * 0.0722D;
            boolean solid = state.isSolidRender(level, point);
            double dx = point.getX() - eye.getX(), dy = point.getY() - eye.getY(), dz = point.getZ() - eye.getZ();
            double distance = Math.max(1.0D, Math.sqrt(dx * dx + dy * dy + dz * dz));
            double salience = 0.30D + luminance * 0.45D + (solid ? 0.25D : 0.0D);
            weight += sampleWeight;
            red += r * sampleWeight;
            green += g * sampleWeight;
            blue += b * sampleWeight;
            heights += dy * sampleWeight;
            heightsSquared += dy * dy * sampleWeight;
            colorSquares += luminance * luminance * sampleWeight;
            if (solid) solidWeight += sampleWeight;
            directionX += dx / distance * salience * sampleWeight;
            directionY += dy / distance * salience * sampleWeight;
            directionZ += dz / distance * salience * sampleWeight;
            return true;
        }
    }

    public record Sample(double red, double green, double blue, double relief,
                         double directionX, double directionY, double directionZ, double directionStrength,
                         double attractorX, double attractorY, double attractorZ, double attractorStrength) {
        public Sample(double red, double green, double blue, double relief) {
            this(red, green, blue, relief, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
