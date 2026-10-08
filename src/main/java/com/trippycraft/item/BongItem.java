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

/** A reusable bong; a loaded bowl carries one salvia serving or one dried cannabis bud. */
public final class BongItem extends Item {
    public enum Contents { EMPTY, SALVIA, CANNABIS }

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
            if (contents == Contents.CANNABIS) {
                SubstanceEngine.addCannabisDose(serverPlayer, doseUnits);
            } else {
                SubstanceEngine.addSalviaDose(serverPlayer, doseUnits);
            }
            // Always empty the bowl, including in creative mode. The glass bong is retained.
            player.setItemInHand(hand, new ItemStack(ModItems.BONG.get()));
            serverPlayer.getCooldowns().addCooldown(ModItems.BONG.get(), 15);
            serverPlayer.getCooldowns().addCooldown(ModItems.BONG_SALVIA_1X.get(), 15);
            serverPlayer.getCooldowns().addCooldown(ModItems.BONG_SALVIA_10X.get(), 15);
            serverPlayer.getCooldowns().addCooldown(ModItems.BONG_SALVIA_25X.get(), 15);
            serverPlayer.getCooldowns().addCooldown(ModItems.BONG_SALVIA_50X.get(), 15);
            serverPlayer.getCooldowns().addCooldown(ModItems.BONG_CANNABIS.get(), 15);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (contents == Contents.EMPTY || doseUnits <= 0.0D) {
            tooltip.add(Component.translatable("tooltip.trippycraft.bong_empty"));
        } else if (contents == Contents.CANNABIS) {
            tooltip.add(Component.translatable("tooltip.trippycraft.abstract_dose"));
            tooltip.add(Component.translatable("tooltip.trippycraft.cannabis_total_mg", CANNABIS_THC_MILLIGRAMS));
            tooltip.add(Component.translatable("tooltip.trippycraft.cannabis_hit_mg", CANNABIS_THC_MILLIGRAMS));
            tooltip.add(Component.translatable("tooltip.trippycraft.bong_loaded"));
            tooltip.add(Component.translatable("tooltip.trippycraft.cannabis_bong_stacks"));
        } else {
            tooltip.add(Component.translatable("tooltip.trippycraft.salvia_gameplay_units", SalviaDoseCurve.formatPotencyLabel(doseUnits)));
            tooltip.add(Component.translatable("tooltip.trippycraft.bong_loaded"));
            tooltip.add(Component.translatable("tooltip.trippycraft.salvia_stacks"));
        }
    }

    public double doseUnits() {
        return doseUnits;
    }

    public Contents contents() {
        return contents;
    }
}
