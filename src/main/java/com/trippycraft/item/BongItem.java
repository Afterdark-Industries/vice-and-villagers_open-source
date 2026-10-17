package com.trippycraft.item;

import com.trippycraft.registry.ModItems;
import com.trippycraft.substance.SubstanceEngine;
import com.trippycraft.substance.salvia.SalviaDoseCurve;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Reusable smoking bowl; subclasses supply their own vessel and cooldown family. */
public class BongItem extends Item {
    public enum Contents { EMPTY, SALVIA, CANNABIS, DMT }

    public static final int CANNABIS_THC_MILLIGRAMS = 200;

    private final Contents contents;
    private final double doseUnits;

    public BongItem(double doseUnits, Properties properties) {
        this(doseUnits > 0.0D ? Contents.SALVIA : Contents.EMPTY, doseUnits, properties);
    }

    public BongItem(Contents contents, double doseUnits, Properties properties) {
        super(properties);
        this.contents = contents;
        this.doseUnits = doseUnits;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (contents == Contents.EMPTY || doseUnits <= 0.0D) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            switch (contents) {
                case CANNABIS -> SubstanceEngine.addCannabisDose(serverPlayer, doseUnits);
                case DMT -> SubstanceEngine.addDmtDose(serverPlayer, doseUnits);
                case SALVIA -> SubstanceEngine.addSalviaDose(serverPlayer, doseUnits);
                case EMPTY -> { }
            }
            // Empty the loaded vessel even in creative; crafting has no vessel remainder.
            player.setItemInHand(hand, new ItemStack(emptyDevice()));
            for (Item item : cooldownItems()) serverPlayer.getCooldowns().addCooldown(item, 15);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (contents == Contents.EMPTY || doseUnits <= 0.0D) {
            tooltip.add(Component.translatable(emptyTooltipKey()));
        } else if (contents == Contents.CANNABIS) {
            tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
            tooltip.add(Component.translatable("tooltip.trippycraft.cannabis_total_mg", CANNABIS_THC_MILLIGRAMS));
            tooltip.add(Component.translatable("tooltip.trippycraft.cannabis_hit_mg", CANNABIS_THC_MILLIGRAMS));
            tooltip.add(Component.translatable(loadedTooltipKey()));
            tooltip.add(Component.translatable("tooltip.trippycraft.cannabis_bong_stacks"));
        } else if (contents == Contents.DMT) {
            tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
            tooltip.add(Component.translatable("tooltip.trippycraft.dmt_gameplay_units", "1.0x"));
            tooltip.add(Component.translatable(loadedTooltipKey()));
            tooltip.add(Component.translatable("tooltip.trippycraft.dmt_stacks"));
        } else {
            tooltip.add(Component.translatable("tooltip.trippycraft.salvia_gameplay_units", SalviaDoseCurve.formatPotencyLabel(doseUnits)));
            tooltip.add(Component.translatable(loadedTooltipKey()));
            tooltip.add(Component.translatable("tooltip.trippycraft.salvia_stacks"));
        }
    }

    public double doseUnits() {
        return doseUnits;
    }

    public Contents contents() {
        return contents;
    }

    protected Item emptyDevice() {
        return ModItems.BONG.get();
    }

    protected List<Item> cooldownItems() {
        return List.of(ModItems.BONG.get(), ModItems.BONG_SALVIA_1X.get(), ModItems.BONG_SALVIA_10X.get(),
                ModItems.BONG_SALVIA_25X.get(), ModItems.BONG_SALVIA_50X.get(), ModItems.BONG_CANNABIS.get(),
                ModItems.BONG_DMT.get());
    }

    protected String emptyTooltipKey() {
        return "tooltip.trippycraft.bong_empty";
    }

    protected String loadedTooltipKey() {
        return "tooltip.trippycraft.bong_loaded";
    }
}
