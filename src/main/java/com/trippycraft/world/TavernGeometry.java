package com.trippycraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Shared dimensions and transforms for the player-authored 10 x 9 x 14 tavern. */
public final class TavernGeometry {
    public static final int WIDTH = 10;
    public static final int HEIGHT = 9;
    public static final int DEPTH = 14;

    private TavernGeometry() {
    }

    public static BoundingBox bounds(VillageBarData.BarPlacement bar) {
        BlockPos first = transform(bar, BlockPos.ZERO);
        BlockPos second = transform(bar, new BlockPos(WIDTH - 1, HEIGHT - 1, DEPTH - 1));
        return BoundingBox.fromCorners(first, second);
    }

    public static BlockPos transform(VillageBarData.BarPlacement bar, BlockPos local) {
        return StructureTemplate.transform(local, Mirror.NONE, bar.rotation(), BlockPos.ZERO)
                .offset(bar.origin());
    }

    public static boolean contains(VillageBarData.BarPlacement bar, BlockPos position) {
        return bounds(bar).isInside(position);
    }
}
