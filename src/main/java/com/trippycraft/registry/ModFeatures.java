package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import com.trippycraft.world.SalviaPatchFeature;
import com.trippycraft.world.MescalineCactusPatchFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, TrippyCraft.MOD_ID);

    public static final Supplier<SalviaPatchFeature> SALVIA_PATCH =
            FEATURES.register("salvia_patch", SalviaPatchFeature::new);

    public static final Supplier<MescalineCactusPatchFeature> MESCALINE_CACTUS_PATCH =
            FEATURES.register("mescaline_cactus_patch", MescalineCactusPatchFeature::new);

    private ModFeatures() {
    }
}
