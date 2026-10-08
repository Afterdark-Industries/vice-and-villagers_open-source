package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import com.trippycraft.loot.VillageHouseChestProcessor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModStructureProcessors {
    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, TrippyCraft.MOD_ID);

    public static final Supplier<StructureProcessorType<VillageHouseChestProcessor>> VILLAGE_HOUSE_CHEST =
            STRUCTURE_PROCESSORS.register("village_house_chest", () -> () -> VillageHouseChestProcessor.CODEC);

    private ModStructureProcessors() {
    }
}
