package com.trippycraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.trippycraft.TrippyCraft;
import com.trippycraft.config.TrippyClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ModListWidget;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleSupplier;

/** Opt-in native config/input/persistence regression in a disposable client directory. */
public final class ConfigClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("trippycraft.configSmokeTest") && !FMLEnvironment.production;
    private static final String WORLD = "config-navigation-smoke";
    private static final String MODE = "trippycraft.configuration.dmtFractalWorld";
    private static final String INTENSITY = "trippycraft.configuration.dmtFractalIntensity";
    private static final String QUALITY = "trippycraft.configuration.dmtFractalQuality";
    private static final List<String> REPORT = new ArrayList<>();
    private static final AtomicInteger CAPTURES = new AtomicInteger();
    private static final KeyMapping[] MAPPINGS = {ClientKeyBindings.TOGGLE_DMT_FRACTAL_WORLD,
            ClientKeyBindings.CYCLE_DMT_FRACTAL_INTENSITY, ClientKeyBindings.CYCLE_DMT_FRACTAL_QUALITY};
    private static final InputConstants.Key[] ORIGINAL_KEYS = new InputConstants.Key[MAPPINGS.length];
    private static Phase phase = Phase.START;
    private static long started;
    private static long renderedFrames, requiredRender;
    private static volatile String failure;

    private ConfigClientSmokeTest() { }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (ENABLED && phase != Phase.DONE) renderedFrames++;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || phase == Phase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (failure != null) throw new IllegalStateException(failure);
            if (started != 0 && (System.nanoTime() - started) / 1e9 > 150)
                throw new IllegalStateException("Config smoke timed out at " + phase);
            // Several catch-up ticks can occur before a single frame during world loading.
            // OptionsList places row widgets while rendering, so ticks alone are insufficient.
            if (renderedFrames < requiredRender) return;
            switch (phase) {
                case START -> {
                    if (!(mc.screen instanceof TitleScreen) && !(mc.screen instanceof AccessibilityOnboardingScreen)) return;
                    if (mc.getOverlay() != null) return;
                    require(!Files.exists(mc.gameDirectory.toPath().resolve("saves").resolve(WORLD))
                                    && !Files.exists(mc.gameDirectory.toPath().resolve("config-smoke-result.txt")),
                            "Use a fresh configSmokeDir; refusing to reuse a previous result or world");
                    started = System.nanoTime();
                    mc.options.pauseOnLostFocus = false;
                    mc.options.onboardAccessibility = false;
                    mc.options.tutorialStep = TutorialSteps.NONE;
                    mc.options.renderDistance().set(3);
                    mc.options.simulationDistance().set(5);
                    mc.options.guiScale().set(2);
                    mc.resizeDisplay();
                    persisted(mc, false, .75, 1);
                    openThroughMods(mc, new TitleScreen());
                    advance(Phase.DEFAULTS);
                }
                case DEFAULTS -> {
                    requireSettings(mc);
                    require(widget(mc.screen, MODE).getMessage().getString().contains(Component.translatable("options.off").getString())
                                    && widget(mc.screen, INTENSITY).getMessage().getString().contains(
                                            Component.translatable("options.trippycraft.dmt_fractal_intensity.recommended").getString())
                                    && widget(mc.screen, QUALITY).getMessage().getString().contains(
                                            Component.translatable("options.trippycraft.dmt_fractal_quality.balanced").getString()),
                            "Fresh menu does not show the disabled realm and recommended 75% intensity");
                    AbstractWidget notice = widget(mc.screen, "trippycraft.settings.dmt_experimental");
                    require(notice.visible && notice.getY() >= 0 && notice.getBottom() <= mc.screen.height
                                    && mc.font.width(notice.getMessage()) <= notice.getWidth(),
                            "Experimental warning is hidden or overflows its native row");
                    String modeHelp = tooltip(mc, widget(mc.screen, MODE)).toLowerCase(java.util.Locale.ROOT);
                    String intensityHelp = tooltip(mc, widget(mc.screen, INTENSITY));
                    require(modeHelp.contains("experimental") && modeHelp.contains("performance")
                                    && intensityHelp.contains("75%") && intensityHelp.contains("50%") && intensityHelp.contains("25%"),
                            "Native setting tooltips omit experimental/performance guidance or player recommendations");
                    log("PASS: fresh client defaults are realm Off, intensity 75%, quality Balanced in live values, saved TOML and native widgets");
                    log("PASS: experimental warning fits its native row; 75%/50%/25% recommendations remain in setting tooltips");
                    capture(mc, "config-title.png");
                    TrippyClientConfig.setDmtFractalIntensity(Double.NaN);
                    persisted(mc, false, .75, 1);
                    TrippyClientConfig.setDmtFractalWorld(false);
                    TrippyClientConfig.setDmtFractalQuality(1);
                    TrippyClientConfig.setDmtFractalIntensity(.63);
                    closeAndReopen(mc);
                    advance(Phase.TITLE);
                }
                case TITLE -> {
                    requireSettings(mc);
                    for (String key : new String[]{MODE, INTENSITY, QUALITY}) {
                        String label = Component.translatable(key).getString();
                        String displayed = widget(mc.screen, key).getMessage().getString();
                        require(displayed.indexOf(label) == displayed.lastIndexOf(label),
                                "Option caption is repeated: " + displayed);
                    }
                    require(close(TrippyClientConfig.dmtFractalIntensity, .63)
                                    && widget(mc.screen, INTENSITY).getMessage().getString().contains("63%"),
                            "Opening the menu changed or concealed a custom 63% intensity");
                    persisted(mc, false, .63, 1);
                    require(!widget(mc.screen, "trippycraft.settings.trip_scrubber").active,
                            "Trip Scrubber should be disabled without an active supported trip");
                    click(mc.screen, MODE);
                    persisted(mc, true, .63, 1);
                    click(mc.screen, MODE);
                    persisted(mc, false, .63, 1);
                    click(mc.screen, MODE);
                    for (double target : new double[]{.25, .5, .75, 1, 0}) {
                        select(mc.screen, INTENSITY, () -> TrippyClientConfig.dmtFractalIntensity, target, 8);
                        persisted(mc, true, target, TrippyClientConfig.dmtFractalQuality);
                    }
                    // Zero remains a valid saved custom value even though the key cycles nonzero presets.
                    closeAndReopen(mc);
                    advance(Phase.QUALITY);
                }
                case QUALITY -> {
                    requireSettings(mc);
                    require(close(TrippyClientConfig.dmtFractalIntensity, 0)
                                    && widget(mc.screen, INTENSITY).getMessage().getString().contains("0%"),
                            "Reopening changed or concealed a zero intensity");
                    for (int quality : new int[]{0, 1, 2}) {
                        select(mc.screen, QUALITY, () -> TrippyClientConfig.dmtFractalQuality, quality, 4);
                        persisted(mc, true, 0, quality);
                    }
                    select(mc.screen, INTENSITY, () -> TrippyClientConfig.dmtFractalIntensity, .75, 8);
                    log("PASS: Mods Config entry, immediate toggles, all intensity/quality presets, custom 63%, zero and TOML persistence");
                    mc.options.guiScale().set(3);
                    mc.getWindow().setWindowed(960, 720);
                    mc.resizeDisplay();
                    advance(Phase.SMALL);
                }
                case SMALL -> {
                    requireSettings(mc);
                    require(mc.screen.width <= 427 && mc.screen.height <= 240,
                            "Small-GUI fixture did not resize the native screen: " + mc.screen.width + "x" + mc.screen.height);
                    for (String key : new String[]{MODE, INTENSITY, QUALITY, "gui.done"}) {
                        AbstractWidget option = widget(mc.screen, key);
                        require(option.getX() >= 0 && option.getY() >= 0
                                        && option.getRight() <= mc.screen.width && option.getBottom() <= mc.screen.height,
                                "Setting extends outside the small GUI: " + key);
                    }
                    select(mc.screen, QUALITY, () -> TrippyClientConfig.dmtFractalQuality, 1, 4);
                    select(mc.screen, QUALITY, () -> TrippyClientConfig.dmtFractalQuality, 2, 4);
                    persisted(mc, true, .75, 2);
                    capture(mc, "config-small-gui.png");
                    click(mc.screen, "gui.done");
                    require(mc.screen instanceof ModListScreen, "Done failed to return to Mods");
                    mc.screen.onClose();
                    require(mc.screen instanceof TitleScreen, "Mods failed to return to the title screen");
                    log("PASS: native small-GUI layout, displayed click targets and title return navigation");
                    mc.options.guiScale().set(2);
                    mc.getWindow().setWindowed(1280, 720);
                    mc.resizeDisplay();
                    phase = Phase.WORLD;
                    mc.createWorldOpenFlows().createFreshLevel(WORLD,
                            new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                    new GameRules(), WorldDataConfiguration.DEFAULT),
                            new WorldOptions(0x434F4E464947L, false, false),
                            registry -> registry.registryOrThrow(Registries.WORLD_PRESET)
                                    .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
                }
                case WORLD -> {
                    if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null
                            || mc.screen != null || mc.getOverlay() != null) return;
                    openThroughMods(mc, new PauseScreen(true));
                    advance(Phase.WORLD_CONFIG);
                }
                case WORLD_CONFIG -> {
                    requireSettings(mc);
                    click(mc.screen, "trippycraft.settings.key_binds");
                    require(mc.screen instanceof KeyBindsScreen, "Key Binds action did not open vanilla controls");
                    mc.screen.onClose();
                    requireSettings(mc);
                    Screen settings = mc.screen;
                    mc.setScreen(new TripScrubScreen(settings));
                    mc.screen.onClose();
                    require(mc.screen == settings, "Trip Scrubber did not return to its config parent");
                    capture(mc, "config-in-world.png");
                    click(mc.screen, "gui.done");
                    require(mc.screen instanceof ModListScreen, "In-world Done did not return to Mods");
                    mc.screen.onClose();
                    require(mc.screen instanceof PauseScreen, "In-world Mods did not return to pause menu");
                    mc.screen.onClose();
                    require(mc.screen == null && mc.player != null, "Pause return did not resume the existing player");
                    for (int i = 0; i < MAPPINGS.length; i++) {
                        ORIGINAL_KEYS[i] = MAPPINGS[i].getKey();
                        MAPPINGS[i].setKey(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_F13 + i));
                    }
                    KeyMapping.resetMapping();
                    for (KeyMapping mapping : MAPPINGS) KeyMapping.click(mapping.getKey());
                    advance(Phase.KEYS);
                }
                case KEYS -> {
                    persisted(mc, false, 1, 0);
                    openThroughMods(mc, new PauseScreen(true));
                    requireSettings(mc);
                    require(widget(mc.screen, INTENSITY).getMessage().getString().contains("100%")
                                    && widget(mc.screen, QUALITY).getMessage().getString().contains(Component.translatable("options.trippycraft.dmt_fractal_quality.low").getString())
                                    && widget(mc.screen, MODE).getMessage().getString().contains(Component.translatable("options.off").getString()),
                            "Reopened widgets did not reflect key-binding changes");
                    log("PASS: in-world config/controls/scrubber parent navigation; real key-mapping clicks update cache, TOML and reopened widgets");
                    advance(Phase.REOPEN);
                }
                case REOPEN -> {
                    capture(mc, "config-after-keybinds.png");
                    advance(Phase.CAPTURES);
                }
                case CAPTURES -> {
                    if (CAPTURES.get() < 4) return;
                    log("PASS: Config client smoke; screenshots=" + CAPTURES.get());
                    finish(mc);
                }
                default -> { }
            }
        } catch (Exception exception) {
            log("FAIL: " + exception);
            TrippyCraft.LOGGER.error("[Config smoke] Native config validation failed", exception);
            finish(mc);
        }
    }

    private static void openThroughMods(Minecraft mc, Screen parent) {
        ModListScreen mods = new ModListScreen(parent);
        mc.setScreen(mods);
        ModListWidget list = mods.children().stream().filter(ModListWidget.class::isInstance)
                .map(ModListWidget.class::cast).findFirst().orElseThrow();
        ModListWidget.ModEntry entry = list.children().stream()
                .filter(candidate -> candidate.getInfo().getModId().equals(TrippyCraft.MOD_ID)).findFirst().orElseThrow();
        entry.mouseClicked(0, 0, 0);
        require(widget(mods, "fml.menu.mods.config").active, "Registered Mods Config action is disabled");
        click(mods, "fml.menu.mods.config");
        requireSettings(mc);
    }

    private static void closeAndReopen(Minecraft mc) {
        click(mc.screen, "gui.done");
        require(mc.screen instanceof ModListScreen, "Done lost the Mods parent");
        click(mc.screen, "fml.menu.mods.config");
        requireSettings(mc);
    }

    private static void select(Screen screen, String key, DoubleSupplier value, double target, int limit) {
        for (int i = 0; i < limit && !close(value.getAsDouble(), target); i++) click(screen, key);
        require(close(value.getAsDouble(), target), "Displayed cycle could not select " + key + "=" + target);
    }

    private static void click(Screen screen, String key) {
        AbstractWidget target = widget(screen, key);
        require(target.active && target.visible, "Unavailable click target: " + key);
        double x = target.getX() + target.getWidth() * .5, y = target.getY() + target.getHeight() * .5;
        require(screen.mouseClicked(x, y, 0), "Native screen did not accept displayed click: " + key
                + " at " + x + "," + y + " on " + screen.width + "x" + screen.height);
        screen.mouseReleased(x, y, 0);
    }

    private static AbstractWidget widget(Screen screen, String key) {
        String label = Component.translatable(key).getString();
        return widgets(screen).stream().filter(widget -> widget.getMessage().getString().startsWith(label))
                .findFirst().orElseThrow(() -> new IllegalStateException("Missing widget " + key + ": "
                        + widgets(screen).stream().map(widget -> widget.getMessage().getString()).toList()));
    }

    private static String tooltip(Minecraft mc, AbstractWidget widget) {
        require(widget.getTooltip() != null, "Missing native setting tooltip");
        StringBuilder text = new StringBuilder();
        for (var line : widget.getTooltip().toCharSequence(mc)) {
            line.accept((index, style, codePoint) -> { text.appendCodePoint(codePoint); return true; });
            text.append(' ');
        }
        return text.toString();
    }

    private static List<AbstractWidget> widgets(ContainerEventHandler container) {
        List<AbstractWidget> widgets = new ArrayList<>();
        for (GuiEventListener child : container.children()) {
            if (child instanceof AbstractWidget widget) widgets.add(widget);
            if (child instanceof ContainerEventHandler nested) widgets.addAll(widgets(nested));
        }
        return widgets;
    }

    private static void persisted(Minecraft mc, boolean mode, double intensity, int quality) throws Exception {
        require(TrippyClientConfig.dmtFractalWorld == mode && close(TrippyClientConfig.dmtFractalIntensity, intensity)
                        && TrippyClientConfig.dmtFractalQuality == quality, "Live client values did not match displayed selection");
        var path = mc.gameDirectory.toPath().resolve("config").resolve(TrippyCraft.MOD_ID + "-client.toml");
        String toml = Files.readString(path);
        require(Boolean.parseBoolean(setting(toml, "dmtFractalWorld")) == mode
                        && close(Double.parseDouble(setting(toml, "dmtFractalIntensity")), intensity)
                        && Integer.parseInt(setting(toml, "dmtFractalQuality")) == quality,
                "Saved client TOML did not match live selection");
    }

    private static String setting(String toml, String name) {
        return toml.lines().map(String::trim).filter(line -> line.startsWith(name + " ="))
                .map(line -> line.substring(line.indexOf('=') + 1).trim()).findFirst().orElseThrow();
    }

    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {
            if (Files.exists(mc.gameDirectory.toPath().resolve("screenshots").resolve(name))) CAPTURES.incrementAndGet();
            else failure = "Screenshot failed: " + message.getString();
        });
    }

    private static void requireSettings(Minecraft mc) {
        require(mc.screen instanceof ModSettingsScreen, "Registered Config did not open ModSettingsScreen");
    }

    private static boolean close(double a, double b) { return Math.abs(a - b) < 1e-9; }
    private static void advance(Phase next) {
        phase = next;
        requiredRender = renderedFrames + (Minecraft.getInstance().screen == null ? 0 : 1);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    private static void log(String message) { REPORT.add(message); TrippyCraft.LOGGER.info("[Config smoke] {}", message); }

    private static void finish(Minecraft mc) {
        phase = Phase.DONE;
        for (int i = 0; i < MAPPINGS.length; i++) if (ORIGINAL_KEYS[i] != null) MAPPINGS[i].setKey(ORIGINAL_KEYS[i]);
        KeyMapping.resetMapping();
        try { Files.write(mc.gameDirectory.toPath().resolve("config-smoke-result.txt"), REPORT); }
        catch (Exception exception) { TrippyCraft.LOGGER.error("Cannot write config smoke report", exception); }
        mc.stop();
    }

    private enum Phase { START, DEFAULTS, TITLE, QUALITY, SMALL, WORLD, WORLD_CONFIG, KEYS, REOPEN, CAPTURES, DONE }
}
