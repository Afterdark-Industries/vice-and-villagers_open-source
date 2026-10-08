package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TrippyCraft.MOD_ID);

    public static final Supplier<CreativeModeTab> TRIPPYCRAFT_TAB = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.trippycraft.main"))
                    .withTabsBefore(CreativeModeTabs.INGREDIENTS)
                    .icon(() -> ModItems.PSILOCYBIN_CAPSULE_1G.get().getDefaultInstance())
                    .displayItems((parameters, output) -> ModItems.creativeItems()
                            .forEach(item -> output.accept(item.get())))
                    .build());

    private ModCreativeTabs() {
    }
}
