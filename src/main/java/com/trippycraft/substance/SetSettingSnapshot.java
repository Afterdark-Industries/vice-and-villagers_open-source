package com.trippycraft.substance;

import com.trippycraft.compat.NbtCompat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Locale;

public record SetSettingSnapshot(
        double safety,
        double threat,
        double darkness,
        double isolation,
        double motion,
        double healthStress,
        double novelty,
        double biomeStress,
        double weatherStress,
        double timeStress,
        double mobThreat,
        double hungerStress,
        double fatigueStress,
        double safeCues,
        double socialSupport,
        double soundscapeComfort
) {
    public static SetSettingSnapshot capture(ServerPlayer player) {
        Level level = player.level();
        BlockPos position = player.blockPosition();
        double healthRatio = player.getMaxHealth() <= 0.0F ? 1.0D : player.getHealth() / player.getMaxHealth();
        double healthStress = 1.0D - DoseCurve.saturate(healthRatio);
        double darkness = 1.0D - DoseCurve.saturate(level.getMaxLocalRawBrightness(position) / 15.0D);
        double motion = DoseCurve.saturate(player.getDeltaMovement().length() / 0.75D);
        double dimensionThreat = level.dimension() == Level.NETHER || level.dimension() == Level.END ? 0.35D : 0.0D;
        MinecraftServer server = player.getServer();
        int playerCount = server == null ? 1 : server.getPlayerList().getPlayerCount();
        double socialSupport = captureSocialSupport(player, server);
        double isolation = playerCount <= 1 ? 0.65D : Math.max(0.0D, 0.35D - socialSupport * 0.25D);
        Biome biome = level.getBiome(position).value();
        double biomeStress = captureBiomeStress(biome, position);
        double weatherStress = captureWeatherStress(level, position);
        double timeStress = captureTimeStress(level);
        double mobThreat = captureMobThreat(player);
        double hungerStress = captureHungerStress(player.getFoodData());
        double fatigueStress = captureFatigueStress(player, timeStress, hungerStress, motion);
        double safeCues = captureSafeCues(player);
        double armorComfort = DoseCurve.saturate(player.getArmorValue() / 20.0D);
        double soundscapeComfort = captureSoundscapeComfort(biome);
        double threat = DoseCurve.saturate(dimensionThreat
                + healthStress * 0.42D
                + darkness * 0.18D
                + biomeStress * 0.16D
                + weatherStress * 0.20D
                + timeStress * 0.16D
                + mobThreat * 0.42D
                + hungerStress * 0.18D
                + fatigueStress * 0.12D
                - armorComfort * 0.16D
                - safeCues * 0.24D
                - socialSupport * 0.12D
                - soundscapeComfort * 0.06D);
        double safety = DoseCurve.saturate(1.0D
                - threat * 0.62D
                - isolation * 0.10D
                + safeCues * 0.28D
                + socialSupport * 0.14D
                + armorComfort * 0.08D
                + soundscapeComfort * 0.06D);
        double novelty = player.tickCount < 20 * 60 ? 0.25D : 0.0D;
        return new SetSettingSnapshot(
                safety,
                threat,
                darkness,
                isolation,
                motion,
                healthStress,
                novelty,
                biomeStress,
                weatherStress,
                timeStress,
                mobThreat,
                hungerStress,
                fatigueStress,
                safeCues,
                socialSupport,
                soundscapeComfort
        );
    }

    public double intensityMultiplier(SubstanceProfile profile) {
        double pressure = threat * 0.46D
                + darkness * 0.16D
                + novelty * 0.16D
                + biomeStress * 0.10D
                + weatherStress * 0.12D
                + timeStress * 0.12D
                + mobThreat * 0.22D
                + hungerStress * 0.08D
                + fatigueStress * 0.08D
                - safety * 0.18D
                - safeCues * 0.10D
                - socialSupport * 0.06D
                - soundscapeComfort * 0.04D;
        return Math.max(0.35D, 1.0D + pressure * profile.settingSensitivity());
    }

    public double anxietyBias(SubstanceProfile profile) {
        return DoseCurve.saturate((threat
                + darkness * 0.38D
                + isolation * 0.22D
                + healthStress * 0.70D
                + weatherStress * 0.24D
                + timeStress * 0.20D
                + mobThreat * 0.54D
                + hungerStress * 0.20D
                - safeCues * 0.18D
                - socialSupport * 0.10D) * profile.settingSensitivity());
    }

    public String debugSummary() {
        return String.format(Locale.ROOT,
                "setting safety=%.2f threat=%.2f dark=%.2f biome=%.2f weather=%.2f time=%.2f mobs=%.2f hunger=%.2f fatigue=%.2f safe=%.2f social=%.2f soundscape=%.2f",
                safety,
                threat,
                darkness,
                biomeStress,
                weatherStress,
                timeStress,
                mobThreat,
                hungerStress,
                fatigueStress,
                safeCues,
                socialSupport,
                soundscapeComfort);
    }

    public double recoverySupport() {
        return DoseCurve.saturate(safety * 0.36D
                + safeCues * 0.26D
                + socialSupport * 0.12D
                + soundscapeComfort * 0.08D
                + (1.0D - hungerStress) * 0.10D
                + (1.0D - fatigueStress) * 0.08D
                - threat * 0.18D
                - mobThreat * 0.16D);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("safety", safety);
        tag.putDouble("threat", threat);
        tag.putDouble("darkness", darkness);
        tag.putDouble("isolation", isolation);
        tag.putDouble("motion", motion);
        tag.putDouble("healthStress", healthStress);
        tag.putDouble("novelty", novelty);
        tag.putDouble("biomeStress", biomeStress);
        tag.putDouble("weatherStress", weatherStress);
        tag.putDouble("timeStress", timeStress);
        tag.putDouble("mobThreat", mobThreat);
        tag.putDouble("hungerStress", hungerStress);
        tag.putDouble("fatigueStress", fatigueStress);
        tag.putDouble("safeCues", safeCues);
        tag.putDouble("socialSupport", socialSupport);
        tag.putDouble("soundscapeComfort", soundscapeComfort);
        return tag;
    }

    public static SetSettingSnapshot load(CompoundTag tag) {
        return new SetSettingSnapshot(
                NbtCompat.getDoubleOr(tag, "safety", 1.0D),
                NbtCompat.getDoubleOr(tag, "threat", 0.0D),
                NbtCompat.getDoubleOr(tag, "darkness", 0.0D),
                NbtCompat.getDoubleOr(tag, "isolation", 0.0D),
                NbtCompat.getDoubleOr(tag, "motion", 0.0D),
                NbtCompat.getDoubleOr(tag, "healthStress", 0.0D),
                NbtCompat.getDoubleOr(tag, "novelty", 0.0D),
                NbtCompat.getDoubleOr(tag, "biomeStress", 0.0D),
                NbtCompat.getDoubleOr(tag, "weatherStress", 0.0D),
                NbtCompat.getDoubleOr(tag, "timeStress", 0.0D),
                NbtCompat.getDoubleOr(tag, "mobThreat", 0.0D),
                NbtCompat.getDoubleOr(tag, "hungerStress", 0.0D),
                NbtCompat.getDoubleOr(tag, "fatigueStress", 0.0D),
                NbtCompat.getDoubleOr(tag, "safeCues", 0.0D),
                NbtCompat.getDoubleOr(tag, "socialSupport", 0.0D),
                NbtCompat.getDoubleOr(tag, "soundscapeComfort", 0.0D)
        );
    }

    private static double captureBiomeStress(Biome biome, BlockPos position) {
        double temperature = biome.getBaseTemperature();
        double temperatureStress = DoseCurve.saturate(Math.abs(temperature - 0.65D) / 1.75D);
        double precipitationStress = biome.hasPrecipitation() ? 0.0D : 0.08D;
        double snowStress = biome.coldEnoughToSnow(position) ? 0.10D : 0.0D;
        return DoseCurve.saturate(temperatureStress + precipitationStress + snowStress);
    }

    private static double captureWeatherStress(Level level, BlockPos position) {
        double rain = level.isRainingAt(position) ? 0.26D : 0.0D;
        double thunder = level.isThundering() ? 0.34D : 0.0D;
        return DoseCurve.saturate(rain + thunder);
    }

    private static double captureTimeStress(Level level) {
        long time = level.getDayTime() % 24000L;
        if (time < 12000L) {
            return 0.0D;
        }
        double distanceFromMidnight = Math.abs(time - 18000L) / 6000.0D;
        return DoseCurve.saturate(1.0D - distanceFromMidnight);
    }

    private static double captureMobThreat(ServerPlayer player) {
        List<Entity> nearby = player.level().getEntities(player, player.getBoundingBox().inflate(18.0D),
                entity -> entity instanceof Enemy && entity.isAlive());
        double proximity = 0.0D;
        for (Entity entity : nearby) {
            double distance = Math.sqrt(entity.distanceToSqr(player));
            proximity += Math.max(0.0D, 1.0D - distance / 18.0D);
        }
        double recentCombat = player.hurtTime > 0 || player.getLastHurtByMob() != null ? 0.35D : 0.0D;
        return DoseCurve.saturate(proximity / 3.0D + recentCombat);
    }

    private static double captureHungerStress(FoodData foodData) {
        double food = 1.0D - DoseCurve.saturate(foodData.getFoodLevel() / 20.0D);
        double saturation = 1.0D - DoseCurve.saturate(foodData.getSaturationLevel() / 8.0D);
        return DoseCurve.saturate(food * 0.75D + saturation * 0.25D);
    }

    private static double captureFatigueStress(ServerPlayer player, double timeStress, double hungerStress, double motion) {
        if (player.isSleeping()) {
            return 0.0D;
        }
        return DoseCurve.saturate(timeStress * 0.35D + hungerStress * 0.26D + motion * 0.18D + (player.isSprinting() ? 0.12D : 0.0D));
    }

    private static double captureSafeCues(ServerPlayer player) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        int usefulBlocks = 0;
        int shelterBlocks = 0;

        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos probe = origin.offset(x, y, z);
                    if (!level.isLoaded(probe)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(probe);
                    if (isSafeBlock(state)) {
                        usefulBlocks++;
                    }
                    if (y >= 2 && !state.isAir()) {
                        shelterBlocks++;
                    }
                }
            }
        }

        double blockComfort = DoseCurve.saturate(usefulBlocks / 8.0D);
        double shelterComfort = DoseCurve.saturate(shelterBlocks / 32.0D);
        double friendlyComfort = captureFriendlyCreatures(player);
        double villageComfort = level instanceof net.minecraft.server.level.ServerLevel serverLevel && serverLevel.isCloseToVillage(origin, 2) ? 0.25D : 0.0D;
        return DoseCurve.saturate(blockComfort * 0.38D + shelterComfort * 0.28D + friendlyComfort * 0.24D + villageComfort);
    }

    private static boolean isSafeBlock(BlockState state) {
        return state.getBlock() instanceof BedBlock
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.LANTERN)
                || state.is(Blocks.TORCH)
                || state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.CHEST);
    }

    private static double captureFriendlyCreatures(ServerPlayer player) {
        List<Entity> nearby = player.level().getEntities(player, player.getBoundingBox().inflate(12.0D),
                entity -> entity instanceof Villager || entity instanceof Animal || entity instanceof TamableAnimal);
        int friendlyCount = 0;
        for (Entity entity : nearby) {
            if (entity instanceof TamableAnimal tamable && !tamable.isTame()) {
                continue;
            }
            friendlyCount++;
        }
        return DoseCurve.saturate(friendlyCount / 5.0D);
    }

    private static double captureSocialSupport(ServerPlayer player, MinecraftServer server) {
        if (server == null) {
            return 0.0D;
        }
        int nearbyPlayers = 0;
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other == player || other.level() != player.level()) {
                continue;
            }
            if (other.distanceToSqr(player) <= 24.0D * 24.0D) {
                nearbyPlayers++;
            }
        }
        return DoseCurve.saturate(nearbyPlayers / 3.0D);
    }

    private static double captureSoundscapeComfort(Biome biome) {
        double ambient = biome.getAmbientLoop().isPresent() ? 0.16D : 0.0D;
        double mood = biome.getAmbientMood().isPresent() ? 0.08D : 0.0D;
        double music = biome.getBackgroundMusic().isPresent() ? 0.10D : 0.0D;
        return DoseCurve.saturate(ambient + mood + music);
    }
}
