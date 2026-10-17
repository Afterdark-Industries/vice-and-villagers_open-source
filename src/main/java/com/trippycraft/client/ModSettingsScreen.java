package com.trippycraft.client;

import com.mojang.serialization.Codec;
import com.trippycraft.config.TrippyClientConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Player settings shared with the shortcuts; vanilla's options list supplies scrolling and focus. */
public final class ModSettingsScreen extends OptionsSubScreen {
    private Button timeline;

    public ModSettingsScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("trippycraft.settings.title"));
    }

    @Override
    protected void addOptions() {
        // Add future settings in sections here. Each option owns its caption,
        // tooltip and persisted setter; no custom positioning/input is needed.
        addSection("trippycraft.settings.section.dmt",
                "trippycraft.settings.dmt_experimental",
                OptionInstance.createBoolean("trippycraft.configuration.dmtFractalWorld",
                        OptionInstance.cachedConstantTooltip(Component.translatable("trippycraft.settings.dmt_world.tooltip")),
                        TrippyClientConfig.dmtFractalWorld, TrippyClientConfig::setDmtFractalWorld),
                fractalIntensity(),
                new OptionInstance<>("trippycraft.configuration.dmtFractalQuality",
                        OptionInstance.cachedConstantTooltip(Component.translatable("trippycraft.settings.dmt_quality.tooltip")),
                        (caption, value) -> Component.translatable(switch (value) {
                                    case 0 -> "options.trippycraft.dmt_fractal_quality.low";
                                    case 2 -> "options.trippycraft.dmt_fractal_quality.high";
                                    default -> "options.trippycraft.dmt_fractal_quality.balanced";
                                }),
                        new OptionInstance.Enum<>(List.of(0, 1, 2), Codec.INT),
                        TrippyClientConfig.dmtFractalQuality, TrippyClientConfig::setDmtFractalQuality));

        addNotice(Component.translatable("trippycraft.settings.dmt_recommended"));
        addNotice(Component.translatable("trippycraft.settings.dmt_mid_range"));
        addNotice(Component.translatable("trippycraft.settings.dmt_low_end"));

        addSection("trippycraft.settings.section.controls", null);
        list.addSmall(Button.builder(Component.translatable("trippycraft.settings.key_binds"),
                        button -> minecraft.setScreen(new KeyBindsScreen(this, options)))
                .width(310).tooltip(Tooltip.create(Component.translatable("trippycraft.settings.key_binds.tooltip"))).build(), null);
        timeline = Button.builder(Component.translatable("trippycraft.settings.trip_scrubber"),
                        button -> minecraft.setScreen(new TripScrubScreen(this)))
                .width(310).tooltip(Tooltip.create(Component.translatable("trippycraft.settings.trip_scrubber.tooltip"))).build();
        timeline.active = ClientKeyBindings.canOpenTripScrub();
        list.addSmall(timeline, null);
    }

    @Override
    public void tick() {
        super.tick();
        if (timeline != null) timeline.active = ClientKeyBindings.canOpenTripScrub();
    }

    private OptionInstance<Double> fractalIntensity() {
        double current = TrippyClientConfig.dmtFractalIntensity;
        List<Double> values = new ArrayList<>(List.of(0.0, 0.25, 0.50, 0.75, 1.0));
        // Preserve config-file values such as 63% until the player changes them.
        if (!values.contains(current)) {
            values.add(current);
            values.sort(Comparator.naturalOrder());
        }
        return new OptionInstance<>("trippycraft.configuration.dmtFractalIntensity",
                OptionInstance.cachedConstantTooltip(Component.translatable("trippycraft.settings.dmt_intensity.tooltip")),
                (caption, value) -> {
                    if (value == 0.75D) return Component.translatable("options.trippycraft.dmt_fractal_intensity.recommended");
                    if (value == 0.50D) return Component.translatable("options.trippycraft.dmt_fractal_intensity.mid_range");
                    if (value == 0.25D) return Component.translatable("options.trippycraft.dmt_fractal_intensity.low_end");
                    return Component.literal(Math.round(value * 100.0) + "%");
                },
                new OptionInstance.Enum<>(List.copyOf(values), Codec.DOUBLE), current,
                TrippyClientConfig::setDmtFractalIntensity);
    }

    private void addSection(String titleKey, String noticeKey, OptionInstance<?>... settings) {
        list.addSmall(new StringWidget(0, 0, 310, 20, Component.translatable(titleKey), font), null);
        if (noticeKey != null) addNotice(Component.translatable(noticeKey).withStyle(ChatFormatting.GOLD));
        for (OptionInstance<?> setting : settings) list.addBig(setting);
    }

    private void addNotice(Component message) {
        list.addSmall(new StringWidget(0, 0, 310, 20, message, font), null);
    }
}
