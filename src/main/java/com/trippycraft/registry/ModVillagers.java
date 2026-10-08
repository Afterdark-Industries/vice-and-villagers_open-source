package com.trippycraft.registry;

import com.google.common.collect.ImmutableSet;
import com.trippycraft.TrippyCraft;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the bartender's real job site and villager profession. */
public final class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, TrippyCraft.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, TrippyCraft.MOD_ID);

    public static final ResourceKey<PoiType> BARTENDER_STATION_KEY = ResourceKey.create(
            Registries.POINT_OF_INTEREST_TYPE,
            ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "bartender_station")
    );

    public static final Supplier<PoiType> BARTENDER_STATION = POI_TYPES.register(
            "bartender_station",
            () -> new PoiType(
                    Set.copyOf(ModBlocks.BARTENDER_STATION.get().getStateDefinition().getPossibleStates()),
                    1,
                    1
            )
    );

    public static final Supplier<VillagerProfession> BARTENDER = PROFESSIONS.register(
            "bartender",
            () -> new VillagerProfession(
                    "bartender",
                    ModVillagers::isBartenderStation,
                    ModVillagers::isBartenderStation,
                    ImmutableSet.of(),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_FISHERMAN
            )
    );

    private ModVillagers() {
    }

    private static boolean isBartenderStation(Holder<PoiType> poi) {
        return poi.is(BARTENDER_STATION_KEY);
    }
}
