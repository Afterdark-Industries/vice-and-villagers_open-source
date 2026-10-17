package com.trippycraft.item;

import com.trippycraft.registry.ModItems;
import net.minecraft.world.item.Item;

import java.util.List;

/** Same one-serving loading and use rules as the bong, retaining the wooden pipe. */
public final class AncientPipeItem extends BongItem {
    public AncientPipeItem(Contents contents, double doseUnits, Properties properties) {
        super(contents, doseUnits, properties);
    }

    @Override
    protected Item emptyDevice() {
        return ModItems.ANCIENT_PIPE.get();
    }

    @Override
    protected List<Item> cooldownItems() {
        return List.of(ModItems.ANCIENT_PIPE.get(), ModItems.ANCIENT_PIPE_SALVIA_1X.get(),
                ModItems.ANCIENT_PIPE_SALVIA_10X.get(), ModItems.ANCIENT_PIPE_SALVIA_25X.get(),
                ModItems.ANCIENT_PIPE_SALVIA_50X.get(), ModItems.ANCIENT_PIPE_CANNABIS.get(),
                ModItems.ANCIENT_PIPE_DMT.get());
    }

    @Override
    protected String emptyTooltipKey() {
        return "tooltip.trippycraft.ancient_pipe_empty";
    }

    @Override
    protected String loadedTooltipKey() {
        return "tooltip.trippycraft.ancient_pipe_loaded";
    }
}
