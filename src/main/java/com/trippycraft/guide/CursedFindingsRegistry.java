package com.trippycraft.guide;

import com.trippycraft.registry.ModItems;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class CursedFindingsRegistry {
    private static final List<CursedFindingCategory> CATEGORIES = List.of(CursedFindingCategory.values());
    private static final List<CursedFindingEntry> ENTRIES = buildEntries();
    private static final Map<String, CursedFindingEntry> BY_ID = indexEntries(ENTRIES);

    private CursedFindingsRegistry() {
    }

    public static List<CursedFindingCategory> categories() {
        return CATEGORIES;
    }

    public static List<CursedFindingEntry> entries() {
        return ENTRIES;
    }

    public static List<CursedFindingEntry> entriesFor(CursedFindingCategory category) {
        return ENTRIES.stream()
                .filter(entry -> entry.category() == category)
                .toList();
    }

    public static Optional<CursedFindingEntry> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static boolean contains(String id) {
        return BY_ID.containsKey(id);
    }

    private static Map<String, CursedFindingEntry> indexEntries(List<CursedFindingEntry> entries) {
        Map<String, CursedFindingEntry> indexed = new LinkedHashMap<>();
        for (CursedFindingEntry entry : entries) {
            indexed.put(entry.id(), entry);
        }
        return Map.copyOf(indexed);
    }

    private static List<CursedFindingEntry> buildEntries() {
        List<CursedFindingEntry> entries = new ArrayList<>();
        entries.add(entry(
                CursedFindingIds.CANNABIS,
                "Cannabis / THC",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.CANNABIS_JOINT,
                List.of(
                        "The green finding begins low to the ground, hiding in wet shade and old forest light. It does not announce itself. It waits until your hand knows the leaf.",
                        "Cannabis is a warm and body-heavy discovery. It does not split the sky like LSD or tear open a tunnel like DMT. It settles over the world instead, thickening time, softening color, and making each step arrive a little after you asked for it.",
                        "The first path is simple: find or grow the plant, dry the bud, then roll a joint or load a Bong or Ancient Pipe. One dried bud holds 200mg THC: the Joint divides it into four pulls, while either smoking device gives it as one hit. The deeper path needs the Cannabis Press, where fresh flowers give up THC Extract, and extract becomes packed green concentrate for pens and edibles."
                ),
                List.of(
                        "The first taste opens this page. After that, every joint, Bong or Ancient Pipe hit, pen, gummy, or chocolate bar adds its own weight to the same green weather until it fades.",
                        "A small pull comes on gently. The edges warm, the air breathes, and colors lean toward gold and leaf-green. The trip bar marks the rise as Onset, Lift, Cruise, Sink, and Afterglow.",
                        "Taking more before the feeling passes does not start a second story. It deepens the one already being written, raising the THC mark and stretching the remaining glow.",
                        "At cruising strength, time begins to drag. The body feels heavier, movement gets soft at the edges, and the camera may lag behind your intention like your eyes are remembering the previous second.",
                        "Sound changes too. Footsteps and ambience can feel closer and warmer, while sharper sounds may tuck themselves down a tunnel.",
                        "Strong THC can bring Hunger and Slowness. Very heavy THC can become strangely floaty, enough that the world may briefly spare your fall.",
                        "Cannabis can soften nausea from other discoveries. With psychedelics, dissociatives, opioids, alcohol, and stranger states, it often makes the whole mixture feel larger during the rise and cruise.",
                        "Cocaine fights the green. Cannabis does not sharpen it; it dulls and pulls against the stimulant edge.",
                        "When opioids leave the body in a poor state, active cannabis can make the hunger penalty less cruel."
                ),
                List.of(
                        step(() -> Items.GRASS_BLOCK, "Farmland or living soil", ModItems.CANNABIS_SEEDS, "Cannabis Seeds", ModItems.CANNABIS_BUD, "4x Cannabis Bud + 2x Seeds",
                                "Give the seed light and patient soil. A mature plant gives 4 Cannabis Buds and 2 Cannabis Seeds. If cut too early, it gives only seed and regret."),
                        step(ModItems.CANNABIS_BUD, "Fresh Cannabis Bud", () -> Items.FURNACE, "Furnace, Blast Furnace, Smoker, or Campfire", ModItems.DRIED_CANNABIS_BUD, "Dried Cannabis Bud",
                                "Fresh bud must be dried before it can be rolled or loaded in a Bong or Ancient Pipe. One dried bud contains 200mg THC. A smoker works fastest, a furnace works steadily, and a campfire takes its slow little ritual."),
                        step(ModItems.DRIED_CANNABIS_BUD, "Dried Bud + 2 Paper", () -> Items.CRAFTING_TABLE, "Crafting", ModItems.CANNABIS_JOINT, "Joint",
                                "The quick field method. One dried bud and two papers become a Joint: four small pulls from one rolled finding."),
                        step(() -> Items.GLASS, "4x Glass + Water Bucket", () -> Items.CRAFTING_TABLE, "Shaped crafting", ModItems.BONG, "Bong",
                                "Build the reusable glass vessel in this shape: _ G _ / _ G _ / G W G. G is a Glass block, W is a Water Bucket, and _ is empty. The Water Bucket leaves an empty Bucket behind."),
                        step(ModItems.DRIED_CANNABIS_BUD, "One Dried Bud + Empty Bong", () -> Items.CRAFTING_TABLE, "Crafting table or inventory", ModItems.BONG_CANNABIS, "Loaded Bong: One 200mg Hit",
                                "Craft one dried bud with one empty Bong in either crafting grid. Right-click the loaded Bong to take its whole 200mg THC hit. The same vessel returns empty, ready for another dried bud, DMT Powder, or salvia extract."),
                        ancientPipeProductionStep(),
                        step(ModItems.DRIED_CANNABIS_BUD, "One Dried Bud + Empty Ancient Pipe", () -> Items.CRAFTING_TABLE, "Crafting table or inventory", ModItems.ANCIENT_PIPE_CANNABIS, "Loaded Ancient Pipe: One 200mg Hit",
                                "Load one dried bud beside one empty Ancient Pipe in either crafting grid. Its one 200mg THC hit uses the same cannabis session as a Bong and returns the empty Ancient Pipe."),
                        step(ModItems.CANNABIS_BUD, "2x Fresh Bud + Fuel", ModItems.CANNABIS_PRESS, "Cannabis Press", ModItems.THC_EXTRACT, "THC Extract",
                                "The press squeezes fresh flowers under heat. Two buds become one THC Extract. Coal or charcoal keeps the jaw hot enough for several pressings."),
                        step(ModItems.THC_EXTRACT, "10x THC Extract", ModItems.CANNABIS_PRESS, "Cannabis Press", ModItems.PACKED_THC_EXTRACT, "Packed THC Extract",
                                "Ten loose extracts can be pressed into one dense packet. This packing needs pressure, not heat."),
                        step(ModItems.PACKED_THC_EXTRACT, "Packed Extract", () -> Items.CRAFTING_TABLE, "Crafting", ModItems.CANNABIS_DAB_PEN, "Dab Pen / Gummies",
                                "Packed extract feeds the stronger branch: Dab Pens and Gummies. Chocolate Bars take a smaller road with loose extract, cocoa, sugar, and milk.")
                ),
                List.of(
                        recipe("Cannabis Press", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(
                                        ref(Items.IRON_INGOT, "2x Iron Ingot"),
                                        ref(Items.SMOOTH_STONE, "3x Smooth Stone"),
                                        ref(Items.COPPER_INGOT, "2x Copper Ingot"),
                                        ref(Items.PISTON, "1x Piston"),
                                        ref(Items.REDSTONE, "1x Redstone")
                                ),
                                ref(ModItems.CANNABIS_PRESS, "Cannabis Press"),
                                List.of("Build the iron jaw in this shape: I S I / C P C / S R S. I=Iron, S=Smooth Stone, C=Copper, P=Piston, R=Redstone.")),
                        recipe("Dried Cannabis Bud", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.CANNABIS_BUD, "1x Cannabis Bud")),
                                ref(ModItems.DRIED_CANNABIS_BUD, "1x Dried Cannabis Bud"),
                                List.of("One fresh bud dries into one dried bud. Smoker is fastest, furnace is steady, campfire is slowest.")),
                        recipe("Joint", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.PAPER, "2x Paper"), ref(ModItems.DRIED_CANNABIS_BUD, "1x Dried Cannabis Bud")),
                                ref(ModItems.CANNABIS_JOINT, "1x Joint"),
                                List.of("Shapeless recipe. A Joint carries 200mg THC total, taken as four 50mg pulls.")),
                        recipe("Bong", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.GLASS, "4x Glass Blocks"), ref(Items.WATER_BUCKET, "1x Water Bucket")),
                                ref(ModItems.BONG, "1x Empty Bong"),
                                List.of("Build it in this shape: _ G _ / _ G _ / G W G. G=Glass block, W=Water Bucket, _=Empty. The Water Bucket leaves an empty Bucket behind.")),
                        recipe("Load Bong: Cannabis", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.BONG, "1x Empty Bong"), ref(ModItems.DRIED_CANNABIS_BUD, "1x Dried Cannabis Bud")),
                                ref(ModItems.BONG_CANNABIS, "1x Loaded Bong (200mg THC)"),
                                List.of("Shapeless recipe in the crafting table or 2x2 inventory grid. One dried bud supplies one 200mg THC hit. Right-click to smoke it and recover the same empty Bong.")),
                        ancientPipeRecipe(),
                        loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.DRIED_CANNABIS_BUD, "Dried Cannabis Bud", ModItems.ANCIENT_PIPE_CANNABIS, "Cannabis", "200mg THC"),
                        recipe("THC Extract", CursedFindingRecipeType.CANNABIS_PRESS,
                                List.of(ref(ModItems.CANNABIS_BUD, "2x Cannabis Bud"), ref(Items.COAL, "Coal or Charcoal fuel")),
                                ref(ModItems.THC_EXTRACT, "1x THC Extract"),
                                List.of("Two fresh buds become one THC Extract in the Cannabis Press. Fresh flowers need coal or charcoal heat; one fuel piece can carry eight pressings.")),
                        recipe("Packed THC Extract", CursedFindingRecipeType.CANNABIS_PRESS,
                                List.of(ref(ModItems.THC_EXTRACT, "10x THC Extract")),
                                ref(ModItems.PACKED_THC_EXTRACT, "1x Packed THC Extract"),
                                List.of("Ten THC Extracts become one Packed THC Extract in the Cannabis Press. This step wants pressure, not fuel.")),
                        recipe("Dab Pen", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.GLASS, "1x Glass"), ref(ModItems.PACKED_THC_EXTRACT, "1x Packed THC Extract"), ref(Items.REDSTONE, "1x Redstone"), ref(Items.COPPER_INGOT, "1x Copper Ingot")),
                                ref(ModItems.CANNABIS_DAB_PEN, "1x Dab Pen"),
                                List.of("Shapeless recipe. The Dab Pen carries 1000mg THC total, released in twenty 50mg pulls.")),
                        recipe("Edible Gummies", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.PACKED_THC_EXTRACT, "1x Packed THC Extract"), ref(Items.SUGAR, "5x Sugar"), ref(Items.SLIME_BALL, "1x Slime Ball")),
                                ref(ModItems.CANNABIS_GUMMIES, "1x Edible Gummies"),
                                List.of("Shapeless recipe. Gummies carry 1000mg THC total, split into two heavy 500mg halves.")),
                        recipe("Chocolate Bar", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.THC_EXTRACT, "3x THC Extract"), ref(Items.COCOA_BEANS, "2x Cocoa Beans"), ref(Items.SUGAR, "1x Sugar"), ref(Items.MILK_BUCKET, "1x Milk Bucket")),
                                ref(ModItems.CANNABIS_CHOCOLATE_BAR, "1x Chocolate Bar"),
                                List.of("Shapeless recipe. The Chocolate Bar carries 250mg THC total, split into two 125mg halves. Milk leaves its bucket behind after the work is done."))
                ),
                List.of(
                        "Look for wild Cannabis where green things keep secrets: Forest, Taiga, Old Growth Birch Forest, Old Growth Pine Taiga, Flower Forest, Dark Forest, Swamp, and Mangrove Swamp.",
                        "The plant accepts farmland, grass, dirt, coarse dirt, podzol, moss, rooted dirt, mud, and similar earth. It wants light, or at least a clear line to the sky.",
                        "Farmland pleases it most. Bonemeal can hurry the growth, but the journal has a bias toward patience.",
                        "The Cannabis Press has three mouths: input, fuel, and output. Buds and extract go in; coal or charcoal heats fresh buds; finished resin waits on the far side.",
                        "Pens, gummies, bars, and joints remember how many uses remain. If you use one from a stack, one serving-piece separates from the pile and the rest tries to return to your inventory.",
                        "A Bong or Ancient Pipe has one bowl and one hit. Load exactly one Dried Cannabis Bud beside the empty device in a crafting table or the inventory grid; fresh buds cannot be smoked this way. A loaded bowl cannot be topped up or mixed with another substance. Smoking adds 200mg THC to the active session and returns the same empty device, including in Creative mode.",
                        "Both devices are shared with DMT and Salvia. Once empty, either can hold one dried cannabis bud, one DMT Powder, or one of the four salvia extract strengths. The Utilities / Smoking Devices page lists their crafting and loading recipes.",
                        "Gummies and Chocolate Bars are not ordinary food in this build. They are measured THC items, and their remaining portions show like durability.",
                        "The journal's THC marks are fictional gameplay measures. They are for the world of Vice and Villagers, not the world outside it."
                )
        ));
        entries.add(entry(
                CursedFindingIds.PSILOCYBIN,
                "Psilocybin",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.PSILOCYBIN_CAPSULE_1G,
                List.of(
                        "The mushroom finding does not arrive like lightning. It creeps in with the patience of wet roots, soft moss, and old shade under trees.",
                        "Psilocybin is the forest learning to breathe around you. Blocks keep their place, but their faces begin to soften. Bark looks threaded with hidden veins. Stone remembers water. The air fills with tiny spores of color.",
                        "The path begins with Strange Mushrooms. Find them in damp places, coax them from ordinary red and brown mushrooms, dry them, grind them, and seal the powder into capsules. The capsule is not a meal. It is a door with a measured hinge."
                ),
                List.of(
                        "The first capsule opens this page. After that, every capsule joins the same living trip until the wave has finished passing through you.",
                        "Small amounts are gentle and moss-lit. Color warms, surfaces breathe, and the world feels slightly more alive than it admits when sober.",
                        "Medium strength brings the Come Up and Peak into view: breathing trees, mycelium-like lines, spore motes, soft tracers, and surfaces that seem to sag and recover like wet cloth.",
                        "Large doses invite stranger weather. The journal marks the road as Onset, Come Up, Peak, Wave, and Afterglow. During the Peak, caves, forests, rain, water, snow, the Nether, and the End can all stain the vision in different ways.",
                        "Psilocybin listens closely to where you stand. Forests lean organic and golden, caves glow from cracks, rain turns droplets glassy, water bends light, and night may let shadow-shapes dissolve at the edge of sight.",
                        "Taking more before the wave ends deepens the same living current. The trip grows upward rather than restarting, and the remaining time stretches with it.",
                        "At heavier strengths, the self can feel less nailed to the body. The world still belongs to Minecraft, but for a while it may feel like Minecraft is dreaming you back.",
                        "When psilocybin, LSD, and DMT overlap, the journal reads them as one braided psychedelic weather. Psilocybin usually lends the braid its organic breath, fungal patterns, and environmental sensitivity.",
                        "A strong completed journey can leave a bright tail: a little luck in the afterglow, and, at high enough strength, a chance to push back against alcohol dependence."
                ),
                List.of(
                        step(() -> Items.RED_MUSHROOM, "Red + Brown Mushroom", () -> Items.CRAFTING_TABLE, "Crafting", ModItems.STRANGE_MUSHROOM, "2x Strange Mushroom",
                                "Two ordinary mushrooms can be pressed into two Strange Mushrooms. The shortcut works, but wild specimens still feel like the truer omen."),
                        step(ModItems.STRANGE_MUSHROOM, "Strange Mushroom", () -> Items.FURNACE, "Furnace, Blast Furnace, Smoker, or Campfire", ModItems.DRIED_STRANGE_MUSHROOM, "Dried Strange Mushroom",
                                "Heat drives the wet forest out of the cap. A smoker is swift, a furnace is steady, and a campfire turns drying into a long little vigil."),
                        step(ModItems.DRIED_STRANGE_MUSHROOM, "Dried Strange Mushroom", ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle", ModItems.PSILOCYBIN_POWDER, "Psilocybin Powder",
                                "The dried cap becomes powder under stone. The mortar does the quiet work in a few heartbeats."),
                        step(ModItems.PSILOCYBIN_POWDER, "4x Psilocybin Powder", () -> Items.CRAFTING_TABLE, "Crafting", ModItems.PACKED_PSILOCYBIN_POWDER, "Packed Powder",
                                "Four loose powders can be packed into one square of stronger bookkeeping, then broken back apart when the dose calls for loose powder."),
                        step(ModItems.EMPTY_CAPSULE, "Empty Capsule + Powder", () -> Items.CRAFTING_TABLE, "Capsule Work", ModItems.PSILOCYBIN_CAPSULE_1G, "Measured Capsules",
                                "Paper, sugar, and powder become tidy little thresholds: 0.5g, 1g, 3g, and 5g in the journal's fictional measures.")
                ),
                List.of(
                        recipe("Mortar and Pestle", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.COBBLESTONE, "2x Cobblestone"), ref(Items.STICK, "1x Stick"), ref(Items.BOWL, "1x Bowl")),
                                ref(ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle"),
                                List.of("Build the small grinding shrine in this shape: C C / _ S _ / _ B _. C=Cobblestone, S=Stick, B=Bowl.")),
                        recipe("Strange Mushroom", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.RED_MUSHROOM, "1x Red Mushroom"), ref(Items.BROWN_MUSHROOM, "1x Brown Mushroom")),
                                ref(ModItems.STRANGE_MUSHROOM, "2x Strange Mushroom"),
                                List.of("Shapeless recipe. A practical imitation of the wild finding.")),
                        recipe("Dried Strange Mushroom", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.STRANGE_MUSHROOM, "1x Strange Mushroom")),
                                ref(ModItems.DRIED_STRANGE_MUSHROOM, "1x Dried Strange Mushroom"),
                                List.of("Smoker dries fastest, furnace follows, and campfire takes the longest. Every path yields one dried mushroom.")),
                        recipe("Psilocybin Powder", CursedFindingRecipeType.MORTAR_AND_PESTLE,
                                List.of(ref(ModItems.DRIED_STRANGE_MUSHROOM, "1x Dried Strange Mushroom")),
                                ref(ModItems.PSILOCYBIN_POWDER, "1x Psilocybin Powder"),
                                List.of("The mortar accepts dried Strange Mushrooms and grinds them into powder.")),
                        recipe("Packed Psilocybin Powder", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.PSILOCYBIN_POWDER, "4x Psilocybin Powder")),
                                ref(ModItems.PACKED_PSILOCYBIN_POWDER, "1x Packed Psilocybin Powder"),
                                List.of("Shape four powders as a 2x2 square. One packed powder can be unpacked back into four loose powders.")),
                        recipe("Empty Capsules", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.PAPER, "2x Paper"), ref(Items.SUGAR, "1x Sugar")),
                                ref(ModItems.EMPTY_CAPSULE, "8x Empty Capsule"),
                                List.of("Shapeless recipe. The capsule is a paper-sugar shell for measured discoveries.")),
                        recipe("0.5g Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.PSILOCYBIN_POWDER, "1x Psilocybin Powder")),
                                ref(ModItems.PSILOCYBIN_CAPSULE_05G, "1x Psilocybin Capsule 0.5g"),
                                List.of("A small capsule, enough to wake the page and tint the edges.")),
                        recipe("1g Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.PSILOCYBIN_POWDER, "2x Psilocybin Powder")),
                                ref(ModItems.PSILOCYBIN_CAPSULE_1G, "1x Psilocybin Capsule 1g"),
                                List.of("A steadier capsule. The forest starts speaking in longer sentences.")),
                        recipe("3g Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.PACKED_PSILOCYBIN_POWDER, "1x Packed Powder"), ref(ModItems.PSILOCYBIN_POWDER, "2x Psilocybin Powder")),
                                ref(ModItems.PSILOCYBIN_CAPSULE_3G, "1x Psilocybin Capsule 3g"),
                                List.of("One packed powder and two loose powders make a strong capsule.")),
                        recipe("5g Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.PACKED_PSILOCYBIN_POWDER, "2x Packed Powder"), ref(ModItems.PSILOCYBIN_POWDER, "2x Psilocybin Powder")),
                                ref(ModItems.PSILOCYBIN_CAPSULE_5G, "1x Psilocybin Capsule 5g"),
                                List.of("Two packed powders and two loose powders make the heavy capsule. The journal's margins get crowded here."))
                ),
                List.of(
                        "Wild Strange Mushrooms favor Forest, Taiga, Old Growth Birch Forest, Old Growth Pine Taiga, Dark Forest, Swamp, Mangrove Swamp, Mushroom Fields, Flower Forest, Lush Caves, and Dripstone Caves.",
                        "They can sit on dirt, coarse dirt, podzol, mycelium, moss, rooted dirt, mud, and other mushroom-friendly ground. Rich floors let them survive more openly; ordinary dirt asks for shade.",
                        "Breaking a Strange Mushroom gives the mushroom back. This is a finding to collect, dry, and grind, not a crop that grows by age like wheat.",
                        "The capsule labels are fictional gameplay intensity marks. They are written for Vice and Villagers' dream-logic, not for any world outside the game.",
                        "If the Trip Status bar shows more than one psychedelic at once, psilocybin gives the blend its green breath, slow waves, fungal overlays, and place-sensitive hallucinations."
                )
        ));
        entries.add(entry(
                CursedFindingIds.LSD,
                "LSD",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.LSD_BLOTTER_100UG,
                List.of(
                        "LSD does not grow politely. It begins as wrong wheat: grain left mature and shaded until a black little omen takes hold.",
                        "Where psilocybin breathes, LSD hums. It draws bright angles through the world, makes edges sing, and teaches blocks to pretend they have their own intentions.",
                        "The chain is longer and more deliberate than the mushroom path. You must find ergot, wake it in ethanol, clean the result into solution, then lay it down onto blotter paper with the Advanced Chemistry Table."
                ),
                List.of(
                        "The first blotter or solution opens this page. Every later tab joins the same electric storm, sharpening the dose mark and lengthening the remaining road.",
                        "The early edge is a hum: outlines brighten, color splits at corners, and the world feels like it has been tuned half a note too high.",
                        "At clearer strength, geometry stops being quiet. Block faces can ripple, duplicate, melt, gloss over like liquid, or seem to move when watched too long.",
                        "The journal marks the ride as Onset, Come Up, Peak, Wave, Comedown, and Afterglow. LSD lingers longer than DMT and usually feels more angular than psilocybin.",
                        "Higher tiers bend the sky and time. Entity silhouettes can echo, movement may leave bright trails, and the horizon can pull into prismatic weather.",
                        "Very strong or stacked tabs push toward reality-collapse visuals: the grid is still there, but the eye starts receiving several possible grids at once.",
                        "LSD blends fiercely with psilocybin and DMT. It lends the shared trip electric color, hard-edged patterning, tracers, sky vortexing, and a restless sense that the world is thinking in symbols.",
                        "The afterglow can leave luck in your pocket for a while. Strong LSD journeys can also push against alcohol dependence in the recovery systems of the mod."
                ),
                List.of(
                        step(() -> Items.WHEAT, "Mature shaded wheat", ModItems.ERGOT_INFECTED_WHEAT, "One dark day", ModItems.ERGOT_CLUMP, "Ergot Clump",
                                "Let mature wheat lose the sky for a full day. When the omen takes, the infected stalk drops 1-3 Ergot Clumps."),
                        step(ModItems.ERGOT_CLUMP, "Ergot Clump + Ethanol", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.LYSERGIC_EXTRACT, "Lysergic Extract",
                                "The Brew Cauldron wakes ergot with ethanol and heat. The result is an extract that should not be trusted in the open air."),
                        step(ModItems.ETHANOL, "Ethanol", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.DISTILLED_SOLVENT, "Distilled Solvent",
                                "Ethanol can be cleaned into Distilled Solvent at the Chemistry Table. The table wants fuel and patience."),
                        step(ModItems.LYSERGIC_EXTRACT, "Lysergic Extract + Solvent", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.LSD_SOLUTION, "LSD Solution",
                                "The second brew fixes the extract into solution. The bottle holds a full 1000ug fictional charge if used directly."),
                        step(ModItems.LSD_SOLUTION, "LSD Solution + Blotter Sheet", ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table", ModItems.LSD_BLOTTER_100UG, "Selected Blotters",
                                "The Advanced Chemistry Table divides one solution across one sheet. Choose the dose before the paper drinks.")
                ),
                List.of(
                        recipe("Brew Cauldron", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.CAULDRON, "1x Cauldron"), ref(Items.CAMPFIRE, "1x Campfire"), ref(Items.SMOOTH_STONE, "3x Smooth Stone"), ref(Items.OAK_FENCE, "2x Any Fence")),
                                ref(ModItems.BREW_CAULDRON, "Brew Cauldron"),
                                List.of("Build it in this shape: _ A _ / F C F / S S S. A=Cauldron, F=Fence, C=Campfire, S=Smooth Stone.")),
                        recipe("Chemistry Table", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.GLASS_BOTTLE, "2x Glass Bottle"), ref(Items.REDSTONE, "1x Redstone"), ref(Items.IRON_INGOT, "2x Iron Ingot"), ref(Items.CRAFTING_TABLE, "1x Crafting Table"), ref(Items.OAK_PLANKS, "2x Any Planks"), ref(Items.FURNACE, "1x Furnace")),
                                ref(ModItems.CHEMISTRY_TABLE, "Chemistry Table"),
                                List.of("Build it in this shape: G R G / I C I / W F W. G=Glass Bottle, R=Redstone, I=Iron, C=Crafting Table, W=Any Planks, F=Furnace.")),
                        recipe("Advanced Chemistry Table", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.DIAMOND, "2x Diamond"), ref(Items.BREWING_STAND, "1x Brewing Stand"), ref(Items.GOLD_INGOT, "2x Gold Ingot"), ref(ModItems.CHEMISTRY_TABLE, "1x Chemistry Table"), ref(Items.OBSIDIAN, "2x Obsidian"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table"),
                                List.of("Build it in this shape: D B D / G C G / O L O. D=Diamond, B=Brewing Stand, G=Gold, C=Chemistry Table, O=Obsidian, L=Glass.")),
                        recipe("Ethanol", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.SUGAR, "1x Sugar"), ref(Items.POTION, "1x Water Bottle"), ref(Items.GLASS_BOTTLE, "1x Glass Bottle")),
                                ref(ModItems.ETHANOL, "1x Ethanol"),
                                List.of("Shapeless recipe. The cauldron uses ethanol to wake ergot.")),
                        recipe("Distilled Solvent", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.ETHANOL, "1x Ethanol"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.DISTILLED_SOLVENT, "1x Distilled Solvent"),
                                List.of("Distill ethanol at the Chemistry Table with any valid fuel.")),
                        recipe("Lysergic Extract", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.ERGOT_CLUMP, "1x Ergot Clump"), ref(ModItems.ETHANOL, "1x Ethanol"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.LYSERGIC_EXTRACT, "1x Lysergic Extract"),
                                List.of("Brew Cauldron recipe. Leave the leaf slot empty; the ergot and ethanol do the work.")),
                        recipe("LSD Solution", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.LYSERGIC_EXTRACT, "1x Lysergic Extract"), ref(ModItems.DISTILLED_SOLVENT, "1x Distilled Solvent"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.LSD_SOLUTION, "1x LSD Solution"),
                                List.of("Brew Cauldron recipe. Leave the leaf slot empty again. The solution can be consumed directly as a full 1000ug fictional dose and leaves a glass bottle behind.")),
                        recipe("Blotter Sheet", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.PAPER, "4x Paper")),
                                ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet"),
                                List.of("Shape four paper into a 2x2 square.")),
                        recipe("25ug Blotters", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.LSD_SOLUTION, "1x LSD Solution"), ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet")),
                                ref(ModItems.LSD_BLOTTER_50UG, "40x LSD Blotter 25ug"),
                                List.of("Select 25ug on the Advanced Chemistry Table. One solution and one sheet become forty small tabs.")),
                        recipe("100ug Blotters", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.LSD_SOLUTION, "1x LSD Solution"), ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet")),
                                ref(ModItems.LSD_BLOTTER_100UG, "10x LSD Blotter 100ug"),
                                List.of("Select 100ug. One solution and one sheet become ten measured tabs.")),
                        recipe("250ug Blotters", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.LSD_SOLUTION, "1x LSD Solution"), ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet")),
                                ref(ModItems.LSD_BLOTTER_250UG, "4x LSD Blotter 250ug"),
                                List.of("Select 250ug. One solution and one sheet become four strong tabs.")),
                        recipe("500ug Blotters", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.LSD_SOLUTION, "1x LSD Solution"), ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet")),
                                ref(ModItems.LSD_BLOTTER_500UG, "2x LSD Blotter 500ug"),
                                List.of("Select 500ug. One solution and one sheet become two heavy tabs. The paper looks too innocent for what it carries."))
                ),
                List.of(
                        "Ergot begins with mature wheat denied the sky. The world checks shaded mature wheat over time; after one full Minecraft day in darkness, it may become Ergot-Infected Wheat.",
                        "Breaking Ergot-Infected Wheat drops 1-3 Ergot Clumps. Bonemeal will not hurry this wrong harvest.",
                        "The Brew Cauldron has places for bark/source, leaf, liquid, fuel, and output. For LSD work, the leaf slot stays empty.",
                        "The Advanced Chemistry Table remembers the selected tab size. Check the selector before feeding it solution and paper.",
                        "The so-called microgram numbers are fictional gameplay intensity labels. They are not real-world instructions.",
                        "LSD can also appear as rare finished contraband in old structures and village places, but the journal trusts the ergot chain most."
                )
        ));
        entries.add(entry(
                CursedFindingIds.TWO_CB,
                "2C-B",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.TWO_CB_100MG,
                List.of(
                        "2C-B is the electric finding. Its crafting path shares MDMA's plant-extract beginning, branches at Aromatic Extract, and passes through three bottled intermediates into a 5000mg crystal and 100mg powder. The finished pink tablets retain their mirrored geometric seal and four formats: 25mg, 100mg, 250mg, and 500mg.",
                        "It is not LSD recolored. LSD flows, breathes, and bends; 2C-B cuts the scene into saturated planes, luminous edges, repeating surface geometry, and brief crystalline symmetries.",
                        "The names and milligram labels on this page are fictional game balance. This finding documents Minecraft effects and never offers real-world dosing or mixing guidance."
                ),
                List.of(
                        "The trip bar follows Onset, Lift, Peak, Crystalline, and Descent. Its complete active clock deliberately matches the MDMA active-trip clock, including dose-stacking extensions.",
                        "At 25mg, color gains a clean saturated edge and geometry stays restrained. At 100mg, neon outlines and beat-snapped tracers become unmistakable. At 250mg, repeating facets crawl across surfaces and symmetry arrives in short rhythmic windows. At 500mg, the scene can read as crystalline and patterned rather than melted.",
                        "The complete 2C-B effect curve is twice as strong as the original calibration. Totals above 500mg escalate beyond that doubled baseline; above 1000mg, the renderer begins changing visual languages instead of only turning up one kaleidoscope.",
                        "At extreme totals, the old geometric overlay recedes and reality itself starts disagreeing: horizontal scene slabs slip out of place, scale echoes drift, a false horizon reflects the wrong part of the world, sparse strips swap left and right, and the highest tiers tear open rotated samples of the scene.",
                        "Bright, quiet upper regions such as clouds pick up animated cyan, magenta, and violet textures. Thin club-light sheets and discontinuity seams answer the beat without hiding the scene behind one giant repeating filter; the tavern reorganizes the timing, while the accessibility setting damps the fastest motion.",
                        "Its crisp sensory focus grants Night Vision, plus a dose-scaled Speed lift and peak-window Haste. These fictional gameplay buffs remain subject to the normal mixture system, so depressants and dissociatives can still dull or mask the experience.",
                        "Bass energy drives the quick parts: tracers step outward on the beat and a small fisheye/FOV pulse answers the low end. Between hits, the image holds still enough to remain crisp.",
                        "Inside the tavern or its ten-block halo, every 2C-B visual and audio response rises smoothly to one-and-a-half strength. The playlist's authored bass clock keeps screen pulses aligned with the moving club lights.",
                        "An active 2C-B trip blocks sleep until its bar has ended, unless a sufficiently heavy active heroin dose overpowers that wakefulness through the shared dose-balanced sleep rule.",
                        "Cannabis deepens color and patterned surfaces; MDMA raises saturation, bass response, and sensual warmth. LSD adds harder electric symmetry, while psilocybin adds organic pattern without turning the dedicated layer melty.",
                        "Ketamine masks and softens the readable geometry. DMT and salvia reduce 2C-B's coherence while their reality-replacing passes take visual priority.",
                        "Cocaine now locks strongly into 2C-B: its clarity and stimulant rush amplify saturation, neon edges, surface geometry, beat response, crystalline detail, and especially tracer snap. Their shaders also blend into a sharper cyan-magenta diffraction and laser rhythm, with the expected extra stimulation and anxiety. Alcohol and opioids still dull contrast and drive while the shared mixture system retains depressant strain and coordination risk.",
                        "Mescaline and other psychedelics meet 2C-B through the shared psychedelic blend; nitrous and other dissociatives meet it through the shared masking and mixed-risk channels. Every active substance still keeps its own signature rather than being recolored pink.",
                        "Extreme totals carry an immediate fictional overdose roll whenever a new dose is taken: 1000mg is 5%, 1500mg is 10%, 2000mg is 20%, 2500mg is 25%, exactly 3000mg is 30%, and totals above 3000mg are 50%.",
                        "Right-click a crystal in your hand to consume 1000mg through the same native 2C-B experience as the tablets. A fresh crystal holds five portions. Each portion joins the active session and follows its existing effects, timing, mixtures, and overdose rules."
                ),
                List.of(
                        step(() -> Items.ACACIA_LOG, "Acacia Log + Axe", () -> Items.IRON_AXE, "Strip the bark", ModItems.ACACIA_BARK, "4x Acacia Bark",
                                "Strip an Acacia Log with an axe in survival to collect four pieces of Acacia Bark."),
                        step(() -> Items.ACACIA_LEAVES, "Acacia Leaves", () -> Items.IRON_AXE, "Break the leaves", ModItems.ACACIA_LEAF, "2x Acacia Leaf",
                                "Break Acacia Leaves in survival to collect two Acacia Leaf items."),
                        step(ModItems.ACACIA_BARK, "Bark + Leaf + Water", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.PLANT_EXTRACT, "1x Plant Extract",
                                "Brew one Acacia Bark, one Acacia Leaf, and one Water Bucket with fuel. The water leaves its empty bucket behind."),
                        step(ModItems.PLANT_EXTRACT, "5x Plant Extract", () -> Items.CRAFTING_TABLE, "Shapeless Crafting", ModItems.BUNDLED_PLANT_EXTRACT, "1x Bundled Plant Extract",
                                "Combine five Plant Extract items into one bundle, following the same starting materials as MDMA."),
                        step(ModItems.BUNDLED_PLANT_EXTRACT, "Bundle + Ethanol + Empty Bottle", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.AROMATIC_EXTRACT, "1x Aromatic Extract",
                                "Put the bundle in the source slot, Ethanol in the leaf slot, and an empty Glass Bottle in the liquid slot. Brew with fuel to obtain Aromatic Extract."),
                        step(ModItems.AROMATIC_EXTRACT, "Aromatic Extract + Fuel", ModItems.CHEMISTRY_TABLE, "Chemistry Table: select 2C-B", ModItems.PHENETHYLAMINE_BASE, "1x Phenethylamine Base",
                                "Choose 2C-B on the Chemistry Table before processing Aromatic Extract. The default MDMA selection still produces Refined Aromatic Oil; the 2C-B selection produces Phenethylamine Base."),
                        step(ModItems.ETHANOL, "1x Ethanol + Fuel", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.DISTILLED_SOLVENT, "1x Distilled Solvent",
                                "Distill Ethanol with fuel. The existing Distilled Solvent item supplies the Distilled Ethanol required by this fictional chain."),
                        step(ModItems.PHENETHYLAMINE_BASE, "Base + Distilled Solvent", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.DIMETHOXY_INTERMEDIATE, "1x Dimethoxy Intermediate",
                                "Brew one Phenethylamine Base with one Distilled Solvent and fuel, leaving the leaf slot empty. The base bottle becomes the intermediate bottle, and the extra empty solvent bottle returns."),
                        step(ModItems.DIMETHOXY_INTERMEDIATE, "1x Dimethoxy Intermediate", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.TWO_CB_FREEBASE, "1x 2C-B Freebase",
                                "The fueled Chemistry Table converts one intermediate into one freebase without duplicating empty glass."),
                        step(ModItems.TWO_CB_FREEBASE, "1x 2C-B Freebase", () -> Items.FURNACE, "Furnace, Blast Furnace, Smoker, or Campfire", ModItems.TWO_CB_CRYSTAL, "1x 5000mg 2C-B Crystal",
                                "Any supported heating path turns one freebase into one full 5000mg crystal."),
                        step(ModItems.TWO_CB_CRYSTAL, "1x Full 5000mg Crystal", ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle", ModItems.TWO_CB_POWDER, "50x 100mg 2C-B Powder",
                                "A full crystal yields fifty powders, each holding 100mg. After one 1000mg crystal portion is consumed, the remaining crystal yields forty powders."),
                        step(ModItems.TWO_CB_POWDER, "100mg Powder + Sugar", ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table", ModItems.TWO_CB_100MG, "Selected 25 / 100 / 250 / 500mg Tablets",
                                "Select the tablet size. One powder and four Sugar make four 25mg tablets; one powder and one Sugar make one 100mg tablet; five powders and two Sugar make two 250mg tablets; five powders and one Sugar make one 500mg tablet.")
                ),
                List.of(
                        recipe("Plant Extract", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.ACACIA_BARK, "1x Acacia Bark"), ref(ModItems.ACACIA_LEAF, "1x Acacia Leaf"), ref(Items.WATER_BUCKET, "1x Water Bucket"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.PLANT_EXTRACT, "1x Plant Extract"),
                                List.of("Bark in the source slot, leaf in the leaf slot, and water in the liquid slot. The Water Bucket returns its empty bucket.")),
                        recipe("Bundled Plant Extract", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.PLANT_EXTRACT, "5x Plant Extract")),
                                ref(ModItems.BUNDLED_PLANT_EXTRACT, "1x Bundled Plant Extract"),
                                List.of("Shapeless recipe. Five extracts supply one bundle for the shared MDMA and 2C-B starting path.")),
                        recipe("Ethanol", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.SUGAR, "1x Sugar"), ref(Items.POTION, "1x Water Bottle"), ref(Items.GLASS_BOTTLE, "1x Glass Bottle")),
                                ref(ModItems.ETHANOL, "1x Ethanol"),
                                List.of("Shapeless game recipe. Ethanol is used in the aromatic brew and can be distilled into the solvent for the next branch.")),
                        recipe("Distilled Solvent (Distilled Ethanol)", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.ETHANOL, "1x Ethanol"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.DISTILLED_SOLVENT, "1x Distilled Solvent"),
                                List.of("Distill Ethanol at the Chemistry Table. The existing Distilled Solvent item is the Distilled Ethanol input in this fictional chain.")),
                        recipe("Aromatic Extract", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.BUNDLED_PLANT_EXTRACT, "1x Bundled Plant Extract"), ref(ModItems.ETHANOL, "1x Ethanol"), ref(Items.GLASS_BOTTLE, "1x Empty Bottle"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.AROMATIC_EXTRACT, "1x Aromatic Extract"),
                                List.of("Use bundle/source, Ethanol/leaf, and empty bottle/liquid. This is the existing MDMA starting brew.")),
                        recipe("Phenethylamine Base", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.AROMATIC_EXTRACT, "1x Aromatic Extract"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.PHENETHYLAMINE_BASE, "1x Phenethylamine Base"),
                                List.of("Select 2C-B before processing. The MDMA default remains Refined Aromatic Oil; the 2C-B branch converts the same extract into Phenethylamine Base.")),
                        recipe("Dimethoxy Intermediate", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.PHENETHYLAMINE_BASE, "1x Phenethylamine Base"), ref(ModItems.DISTILLED_SOLVENT, "1x Distilled Solvent"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.DIMETHOXY_INTERMEDIATE, "1x Dimethoxy Intermediate"),
                                List.of("Base in the source slot, solvent in the liquid slot, and leaf slot empty. The base bottle becomes the product; the extra empty solvent bottle returns.")),
                        recipe("2C-B Freebase", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.DIMETHOXY_INTERMEDIATE, "1x Dimethoxy Intermediate"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.TWO_CB_FREEBASE, "1x 2C-B Freebase"),
                                List.of("Convert one intermediate into one freebase with fuel. The input bottle becomes the product bottle.")),
                        recipe("2C-B Crystal", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.TWO_CB_FREEBASE, "1x 2C-B Freebase")),
                                ref(ModItems.TWO_CB_CRYSTAL, "1x 5000mg 2C-B Crystal"),
                                List.of("A Furnace, Blast Furnace, Smoker, or Campfire produces one full crystal from one freebase.")),
                        recipe("2C-B Powder", CursedFindingRecipeType.MORTAR_AND_PESTLE,
                                List.of(ref(ModItems.TWO_CB_CRYSTAL, "1x Full 5000mg Crystal")),
                                ref(ModItems.TWO_CB_POWDER, "50x 100mg 2C-B Powder"),
                                List.of("Only remaining content is ground. Each powder holds 100mg; one direct crystal use leaves enough for forty powders.")),
                        recipe("2C-B 25mg Tablets", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.TWO_CB_POWDER, "1x Powder: 100mg"), ref(Items.SUGAR, "4x Sugar")),
                                ref(ModItems.TWO_CB_25MG, "4x 2C-B 25mg"),
                                List.of("Select 25mg. One complete batch conserves 100mg across four tablets. A full crystal can make two hundred tablets.")),
                        recipe("2C-B 100mg Tablet", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.TWO_CB_POWDER, "1x Powder: 100mg"), ref(Items.SUGAR, "1x Sugar")),
                                ref(ModItems.TWO_CB_100MG, "1x 2C-B 100mg"),
                                List.of("Select 100mg. One powder and one Sugar make one tablet. A full crystal can make fifty tablets.")),
                        recipe("2C-B 250mg Tablets", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.TWO_CB_POWDER, "5x Powder: 500mg"), ref(Items.SUGAR, "2x Sugar")),
                                ref(ModItems.TWO_CB_250MG, "2x 2C-B 250mg"),
                                List.of("Select 250mg. Five powders make two tablets, conserving all 500mg. A full crystal can make twenty tablets.")),
                        recipe("2C-B 500mg Tablet", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.TWO_CB_POWDER, "5x Powder: 500mg"), ref(Items.SUGAR, "1x Sugar")),
                                ref(ModItems.TWO_CB_500MG, "1x 2C-B 500mg"),
                                List.of("Select 500mg. Five powders and one Sugar make one tablet. A full crystal can make ten tablets."))
                ),
                List.of(
                        "All four tablets use the same provided pink-violet-cyan geometric artwork; their item names carry the strength.",
                        "Taking another tablet joins the living session, raises the target dose, and extends the remaining clock instead of starting a disconnected overlay.",
                        "The finding unlocks when a 2C-B tablet, crystal portion, or containing Blend Capsule is consumed.",
                        "Choose 2C-B at the Chemistry Table's Aromatic Extract selector. Leaving it at MDMA preserves the existing Refined Aromatic Oil path.",
                        "Distilled Ethanol uses the existing item named Distilled Solvent. Brew it with Phenethylamine Base; the extra empty Glass Bottle is returned.",
                        "Crystals hold 5000mg and give five 1000mg direct portions. Their saved remaining content controls the mortar yield, with exactly 100mg in each powder.",
                        "The Advanced Chemistry Table uses Sugar for tablets and preserves the full powder content. Missing Sugar, incomplete powder batches, or blocked output leave the ingredients untouched; surplus powder stays in the input.",
                        "Crafted tablets are the existing 25mg, 100mg, 250mg, and 500mg formats. Their balanced effects, artwork, dose-stacking behavior, and dealer availability stay the same."
                )
        ));
        entries.add(entry(
                CursedFindingIds.HO_MET,
                "4-HO-MET",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.HO_MET_100MG,
                List.of(
                        "4-HO-MET is the impossible-depth finding. Its trail begins with Acacia Bark, passes through turquoise reagent and violet intermediate, then becomes a lime-green freebase, crystal, and powder before reaching the familiar finished bottles.",
                        "Finished bottles hold 25mg, 100mg, 250mg, or 500mg and share the same bottle artwork. Every 4-HO-MET dosage now produces twice its former visual effect strength, while the bottle sizes and trip clocks stay the same.",
                        "Its signature lives in the environment: blocks breathe at different depths, connected surfaces carry slow waves, and moving things leave memories suspended behind them. Familiar rooms remain recognizable while their proportions begin to disagree.",
                        "The substance name and milligram labels are game balance. This page describes Minecraft mechanics, not real-world doses or preparation."
                ),
                List.of(
                        "The existing trip bar tracks a gradual rise, an increasingly detailed peak, a long visual plateau, a smooth descent, and a faint afterglow. At the default time scale, a fresh 500mg bottle retains the former highest tier's seven-minute session. Taking another bottle joins that living session instead of starting a second visual layer.",
                        "At low intensity, depth shifts gently and rich color gathers at edges. At medium intensity, block faces breathe, surface geometry develops, and neighboring walls or floors share coherent motion.",
                        "The doubled effect applies throughout the dose range: intensity, spatial depth, deformation, recursion, color, perspective, complexity, and trailing all rise from the stronger baseline. Additional bottles keep increasing them rather than stopping at a visual ceiling.",
                        "Movement draws out the trailing. Staying still lets quieter surface detail unfold; the room keeps moving internally even when the camera rests.",
                        "Other substances meet this finding through the shared mixture. 4-HO-MET contributes depth, recursion, color, and trailing while the other active substances keep their own character.",
                        "A Blend Capsule preserves the effect contribution from every included bottle. Several tiers merge into one stored 4-HO-MET contribution and return through the same trip system on use.",
                        "Right-click a crystal in your hand to consume 1000mg from its 5000mg contents. Each portion has the same combined effect strength as two finished 500mg bottles, and a fresh crystal supplies five portions.",
                        "The existing in-game overdose rule uses accumulated dose calibration rather than printed package contents. Every new dose that pushes the session above its threshold rolls an independent 30% chance of immediate death. Doubling the visual effects leaves this dose accounting and rule unchanged."
                ),
                List.of(
                        step(() -> Items.ACACIA_LOG, "Acacia Log + Axe", () -> Items.IRON_AXE, "Strip the bark", ModItems.ACACIA_BARK, "4x Acacia Bark",
                                "Strip an Acacia Log with an axe in survival to collect four pieces of Acacia Bark."),
                        step(ModItems.ACACIA_BARK, "1x Acacia Bark", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.AMINOETHYL_REAGENT, "1x Aminoethyl Reagent",
                                "One bark becomes one Aminoethyl Reagent, the chain's Tryptamine Base, with fuel at the Chemistry Table."),
                        step(ModItems.AMINOETHYL_REAGENT, "Reagent + Ethanol", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.MET_INTERMEDIATE, "1x MET Intermediate",
                                "Combine one Aminoethyl Reagent with one Ethanol. The brew produces one MET Intermediate and returns the extra empty bottle."),
                        step(ModItems.MET_INTERMEDIATE, "1x MET Intermediate", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.HO_MET_FREEBASE, "1x 4-HO-MET Freebase",
                                "The Chemistry Table converts one intermediate into one freebase with fuel."),
                        step(ModItems.HO_MET_FREEBASE, "1x 4-HO-MET Freebase", () -> Items.FURNACE, "Furnace, Blast Furnace, Smoker, or Campfire", ModItems.HO_MET_CRYSTAL, "1x 5000mg Crystal",
                                "Heat turns one freebase into one full 5000mg crystal. A campfire provides the open-fire path."),
                        step(ModItems.HO_MET_CRYSTAL, "1x Full 5000mg Crystal", ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle", ModItems.HO_MET_POWDER, "50x 100mg Powder",
                                "A full crystal grinds into fifty pieces of powder, each carrying 100mg. After one 1000mg crystal portion is consumed, the remaining crystal yields forty powders."),
                        step(ModItems.HO_MET_POWDER, "100mg Powder + Glass Bottles", ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table", ModItems.HO_MET_100MG, "Selected 25 / 100 / 250 / 500mg Bottles",
                                "Choose the bottle size before processing. One powder fills four 25mg bottles or one 100mg bottle; five powders fill two 250mg bottles or one 500mg bottle. Unmatched powder stays in the input.")
                ),
                List.of(
                        recipe("Aminoethyl Reagent", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.ACACIA_BARK, "1x Acacia Bark"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.AMINOETHYL_REAGENT, "1x Aminoethyl Reagent"),
                                List.of("Chemistry Table recipe. One Acacia Bark becomes one Tryptamine Base reagent.")),
                        recipe("MET Intermediate", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.AMINOETHYL_REAGENT, "1x Aminoethyl Reagent"), ref(ModItems.ETHANOL, "1x Ethanol"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.MET_INTERMEDIATE, "1x MET Intermediate"),
                                List.of("Brew one reagent with one Ethanol. The extra empty Glass Bottle returns after the brew.")),
                        recipe("4-HO-MET Freebase", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.MET_INTERMEDIATE, "1x MET Intermediate"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.HO_MET_FREEBASE, "1x 4-HO-MET Freebase"),
                                List.of("Chemistry Table recipe. One intermediate becomes one freebase.")),
                        recipe("4-HO-MET Crystal", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.HO_MET_FREEBASE, "1x 4-HO-MET Freebase")),
                                ref(ModItems.HO_MET_CRYSTAL, "1x 5000mg 4-HO-MET Crystal"),
                                List.of("Use a Furnace, Blast Furnace, Smoker, or Campfire. Every heating path produces one full 5000mg crystal.")),
                        recipe("4-HO-MET Powder", CursedFindingRecipeType.MORTAR_AND_PESTLE,
                                List.of(ref(ModItems.HO_MET_CRYSTAL, "1x Full 5000mg Crystal")),
                                ref(ModItems.HO_MET_POWDER, "50x 100mg 4-HO-MET Powder"),
                                List.of("A full crystal yields fifty powders. Grinding a partly consumed crystal only packages its remaining contents; one powder holds 100mg. One crystal portion leaves forty powders' worth.")),
                        recipe("4-HO-MET 25mg Bottles", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.HO_MET_POWDER, "1x Powder: 100mg"), ref(Items.GLASS_BOTTLE, "4x Glass Bottle")),
                                ref(ModItems.HO_MET_25MG, "4x 4-HO-MET 25mg"),
                                List.of("Select 25mg. One 100mg powder fills four 25mg bottles in one cycle. A full crystal can fill two hundred bottles.")),
                        recipe("4-HO-MET 100mg Bottle", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.HO_MET_POWDER, "1x Powder: 100mg"), ref(Items.GLASS_BOTTLE, "1x Glass Bottle")),
                                ref(ModItems.HO_MET_100MG, "1x 4-HO-MET 100mg"),
                                List.of("Select 100mg. One powder fills one bottle. A full crystal can fill fifty bottles.")),
                        recipe("4-HO-MET 250mg Bottles", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.HO_MET_POWDER, "5x Powder: 500mg"), ref(Items.GLASS_BOTTLE, "2x Glass Bottle")),
                                ref(ModItems.HO_MET_250MG, "2x 4-HO-MET 250mg"),
                                List.of("Select 250mg. Five powders fill two bottles in one cycle, preserving all 500mg. A full crystal can fill twenty bottles. Fewer than five remaining powders stay in the input for a later batch or another selection.")),
                        recipe("4-HO-MET 500mg Bottle", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.HO_MET_POWDER, "5x Powder: 500mg"), ref(Items.GLASS_BOTTLE, "1x Glass Bottle")),
                                ref(ModItems.HO_MET_500MG, "1x 4-HO-MET 500mg"),
                                List.of("Select 500mg. Five powders fill one bottle. A full crystal can fill ten bottles.")),
                        recipe("4-HO-MET Blend Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.HO_MET_500MG, "1x 4-HO-MET 500mg"), ref(ModItems.HO_MET_100MG, "1x 4-HO-MET 100mg")),
                                ref(ModItems.BLEND_CAPSULE, "1x Blend Capsule"),
                                List.of("Shapeless game recipe. Put each bottle in its own crafting slot; one bottle per occupied slot contributes its preserved effect tier.",
                                        "Add another supported substance item in a free slot to store a mixed capsule. The ingredients' containers return through ordinary crafting remainders."))
                ),
                List.of(
                        "Dealers carry all four bottles alongside their existing products. Their existing price and customer demand tiers remain tied to the corresponding effect tiers.",
                        "Using a bottle returns an empty Glass Bottle. The bottle artwork is shared across formats, with the item name carrying the new size.",
                        "Fresh crystals hold 5000mg. Each right-click consumes 1000mg and leaves the rest in the same crystal; its remaining contents control its mortar yield.",
                        "Each 100mg powder is also a single-hit serving: right-click to consume one powder with the same effect as a 100mg finished bottle. Powder produces no empty bottle; the rest of a held stack remains available for later use or packaging.",
                        "Powder and bottle amounts are conserved at the Advanced Chemistry Table. It waits for enough powder, enough empty bottles, and output space, then produces the selected format.",
                        "The 25mg, 100mg, 250mg, and 500mg bottles keep their existing dose calibration and durations, with twice their former visual effect strength. Crystal portions and containing Blend Capsules receive the same stronger 4-HO-MET effects.",
                        "The finding unlocks after a bottle, crystal portion, or Blend Capsule containing 4-HO-MET is consumed. The discovery stays in the journal through save and load."
                )
        ));
        entries.add(entry(
                CursedFindingIds.AL_LAD,
                "AL-LAD",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.AL_LAD_100MG,
                List.of(
                        "AL-LAD is the stained-glass finding. Four perforated blotters share the supplied cyan, blue, and magenta spiral design and hold 25mg, 100mg, 250mg, or 500mg of fictional gameplay content. Their four effect tiers retain the same strength as before.",
                        "Where LSD cuts the world with electric geometry, AL-LAD rebuilds it from moving cathedral glass: leaded panes, jewel color, prismatic depth, and nested windows that look into rooms that should not be there.",
                        "Its Minecraft crafting path branches from LSD's Lysergic Extract, joins the Acacia Bark reagent path, and passes through Allyl-Lysergic Intermediate into a bottled 5000mg AL-LAD Concentrate. The Advanced Chemistry Table lays measured portions onto Blotter Sheets.",
                        "The names, quantities, and crafting steps on this page describe fictional Minecraft mechanics, not real-world doses, sourcing, or preparation."
                ),
                List.of(
                        "The shared trip bar follows Onset, Come-up, Peak, a long Glass Plateau, Descent, and Afterglow. At the default time scale a fresh 100mg tab retains its four-minute session, a little shorter than the matching LSD clock.",
                        "At the 25mg tier, color blooms and faint leaded seams gather on surfaces. Night Vision arrives as the glass begins to catch light.",
                        "At the 100mg tier the world tessellates into slow-moving stained glass. Prismatic fringes split along pane edges, and a mild Speed lift joins the visual plateau. Peak and plateau can grant Luck.",
                        "At the 250mg tier refraction displaces the reconstructed world. Nested cathedral windows open inside surfaces, and music in a tavern gently travels color through the panes.",
                        "At the 500mg tier and stacked totals, recursive glass depth and kaleidoscopic folds take over. High body load can add Hunger, and the heaviest stacked states can briefly confuse. Descent leaves a short Weakness afterglow.",
                        "Taking another blotter joins the living session, raises the target dose, and extends the remaining clock instead of starting a disconnected overlay.",
                        "AL-LAD meets other substances through the shared psychedelic blend. It keeps its stained-glass signature while LSD, psilocybin, DMT, 4-HO-MET, 2C-B, and mescaline keep theirs. Cannabis warms the jewels; ketamine and DMT can mask or dominate the readable glass.",
                        "A Blend Capsule preserves the content and effect contribution from every included blotter. Mixed capsules still dispatch through the same trip system.",
                        "Right-click AL-LAD Concentrate in your hand to consume 1000mg. Each portion has the combined effect strength of two finished 500mg tabs; a full 5000mg bottle provides five portions and returns its empty Glass Bottle when exhausted.",
                        "J opens the existing timeline editor for an active AL-LAD trip. Psilocybin keeps priority, then 4-HO-MET, then AL-LAD.",
                        "There is no fictional lethal overdose roll. The experience stays in gameplay channels, vanilla buffs/debuffs, and the stained-glass world shader."
                ),
                List.of(
                        step(() -> Items.WHEAT, "Mature shaded wheat", ModItems.ERGOT_INFECTED_WHEAT, "One dark day", ModItems.ERGOT_CLUMP, "Ergot Clump",
                                "Follow LSD's source path: mature wheat kept without sky for a full Minecraft day can become infected and drop 1-3 Ergot Clumps."),
                        step(ModItems.ERGOT_CLUMP, "Ergot Clump + Ethanol", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.LYSERGIC_EXTRACT, "1x Lysergic Extract",
                                "Brew one Ergot Clump with one Ethanol and fuel. Leave the leaf slot empty, just as on the LSD path."),
                        step(ModItems.ACACIA_BARK, "1x Acacia Bark", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.AMINOETHYL_REAGENT, "1x Aminoethyl Reagent",
                                "The shared reagent path begins with bark stripped from Acacia Logs. The fueled Chemistry Table converts one bark into one Aminoethyl Reagent."),
                        step(ModItems.LYSERGIC_EXTRACT, "Lysergic Extract + Aminoethyl Reagent", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.ALLYL_LYSERGIC_INTERMEDIATE, "1x Allyl-Lysergic Intermediate",
                                "Brew one Lysergic Extract with one Aminoethyl Reagent and fuel. The extract is consumed, and the reagent's empty Glass Bottle returns through the usual Brew Cauldron handling."),
                        step(ModItems.ALLYL_LYSERGIC_INTERMEDIATE, "1x Allyl-Lysergic Intermediate", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.AL_LAD_CONCENTRATE, "1x 5000mg AL-LAD Concentrate",
                                "The fueled Chemistry Table converts one intermediate into one full concentrate bottle without duplicating its glass."),
                        step(ModItems.AL_LAD_CONCENTRATE, "Concentrate + Blotter Sheet", ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table", ModItems.AL_LAD_100MG, "Selected 25 / 100 / 250 / 500mg Tabs",
                                "Choose the tab size. Every cycle consumes one Blotter Sheet and 1000mg from the concentrate, producing 40, 10, 4, or 2 tabs. The remaining concentrate stays in the table until all five portions are used.")
                ),
                List.of(
                        recipe("Lysergic Extract", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.ERGOT_CLUMP, "1x Ergot Clump"), ref(ModItems.ETHANOL, "1x Ethanol"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.LYSERGIC_EXTRACT, "1x Lysergic Extract"),
                                List.of("Fictional Minecraft brew shared with LSD. Put Ergot Clump in the source slot, Ethanol in the liquid slot, and leave the leaf slot empty.")),
                        recipe("Aminoethyl Reagent", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.ACACIA_BARK, "1x Acacia Bark"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.AMINOETHYL_REAGENT, "1x Aminoethyl Reagent"),
                                List.of("Chemistry Table recipe shared with 4-HO-MET. One bark becomes one reagent with fuel.")),
                        recipe("Allyl-Lysergic Intermediate", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.LYSERGIC_EXTRACT, "1x Lysergic Extract"), ref(ModItems.AMINOETHYL_REAGENT, "1x Aminoethyl Reagent"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.ALLYL_LYSERGIC_INTERMEDIATE, "1x Allyl-Lysergic Intermediate"),
                                List.of("Fictional Minecraft brew. Lysergic Extract is consumed, and Aminoethyl Reagent returns one empty Glass Bottle.")),
                        recipe("AL-LAD Concentrate", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.ALLYL_LYSERGIC_INTERMEDIATE, "1x Allyl-Lysergic Intermediate"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.AL_LAD_CONCENTRATE, "1x 5000mg AL-LAD Concentrate"),
                                List.of("One intermediate becomes one full concentrate bottle at the Chemistry Table.")),
                        recipe("Blotter Sheet", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.PAPER, "4x Paper")),
                                ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet"),
                                List.of("Shape four Paper into a 2x2 square.")),
                        recipe("AL-LAD 25mg Tabs", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.AL_LAD_CONCENTRATE, "1000mg Concentrate"), ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet")),
                                ref(ModItems.AL_LAD_25MG, "40x AL-LAD 25mg"),
                                List.of("Select 25mg. One sheet and 1000mg from the bottle yield forty tabs. Five sheets package a full bottle into two hundred tabs.")),
                        recipe("AL-LAD 100mg Tabs", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.AL_LAD_CONCENTRATE, "1000mg Concentrate"), ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet")),
                                ref(ModItems.AL_LAD_100MG, "10x AL-LAD 100mg"),
                                List.of("Select 100mg. One sheet and 1000mg yield ten tabs. Five sheets package a full bottle into fifty tabs.")),
                        recipe("AL-LAD 250mg Tabs", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.AL_LAD_CONCENTRATE, "1000mg Concentrate"), ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet")),
                                ref(ModItems.AL_LAD_250MG, "4x AL-LAD 250mg"),
                                List.of("Select 250mg. One sheet and 1000mg yield four tabs. Five sheets package a full bottle into twenty tabs.")),
                        recipe("AL-LAD 500mg Tabs", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.AL_LAD_CONCENTRATE, "1000mg Concentrate"), ref(ModItems.BLOTTER_SHEET, "1x Blotter Sheet")),
                                ref(ModItems.AL_LAD_500MG, "2x AL-LAD 500mg"),
                                List.of("Select 500mg. One sheet and 1000mg yield two tabs. Five sheets package a full bottle into ten tabs.")),
                        recipe("AL-LAD Blend Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.AL_LAD_100MG, "1x AL-LAD 100mg"), ref(ModItems.AL_LAD_25MG, "1x AL-LAD 25mg")),
                                ref(ModItems.BLEND_CAPSULE, "1x Blend Capsule: 125mg AL-LAD"),
                                List.of("Shapeless game recipe. Put each blotter in its own crafting slot; one blotter per occupied slot contributes its content and preserved effect tier.",
                                        "Add another supported substance item in a free slot to store a mixed capsule."))
                ),
                List.of(
                        "Dealers carry all four blotters alongside their existing products. The new content labels retain the corresponding existing price and customer demand tiers.",
                        "All four finished formats use the supplied cyan, blue, and magenta spiral blotter artwork. The 25mg, 100mg, 250mg, and 500mg tabs preserve the former 75ug, 150ug, 300ug, and 450ug effect tiers respectively.",
                        "A full concentrate holds 5000mg. Direct use or a packaging cycle consumes exactly 1000mg; the bottle's remaining contents persist across saves, and its final portion returns an empty Glass Bottle.",
                        "The Advanced Chemistry Table needs one Blotter Sheet per cycle and enough output space for the selected batch. It retains the concentrate between cycles and consumes nothing while blocked.",
                        "The finding unlocks after a tab, concentrate portion, or Blend Capsule containing AL-LAD is consumed. The discovery stays in the journal through save and load.",
                        "Every crafting instruction here belongs to the fictional Minecraft game chain."
                )
        ));
        entries.add(entry(
                CursedFindingIds.TWO_CE,
                "2C-E",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.TWO_CE,
                List.of(
                        "2C-E is the machine-geometry finding. Four tablets share one ominous hex-seal print, in the standard dose formats: 25mg, 100mg, 250mg, and 500mg.",
                        "Where AL-LAD rebuilds the world as stained glass, 2C-E exposes it as a sacred geometry engine: nested cavities in blocks, rooms behind walls, edges locking into impossible angles, and corridors that recede into themselves.",
                        "The milligram labels are game balance. This page describes Minecraft mechanics, not real-world doses, sourcing, or preparation."
                ),
                List.of(
                        "The shared trip bar follows Onset, Lock-in, Peak, a long Machine plateau, Descent, and Afterglow. At the default time scale a fresh 250mg session lasts eight minutes, slower and heavier than 4-HO-MET.",
                        "At 25mg, block edges lock and faint cyan wireframes appear on faces. Night Vision arrives as the machine becomes visible.",
                        "At 100mg walls read as rooms behind rooms. Tessellation hardens. Tight geometric trails follow motion instead of soft liquid ribbons. The body grows heavier (Slowness).",
                        "At 250mg architecture recursively repeats into itself. Gaze opens a slow infinite corridor. Perspective folds inward and outward.",
                        "At 500mg and stacked totals the machine overwhelms the field: brief full-scene symmetry locks, heavier depth, still less colorful than AL-LAD. Descent leaves Weakness.",
                        "Taking another tablet joins the living session and extends the remaining clock.",
                        "2C-E meets other substances through the shared psychedelic blend. It keeps its cyan-violet-crimson-gold machine signature while AL-LAD keeps stained glass. Mixed bars list both names and both progress heads.",
                        "A Blend Capsule stores the exact physical milligram total and each tablet's original effect calibration. J opens the timeline editor; psilocybin then 4-HO-MET then AL-LAD then 2C-E keep priority.",
                        "There is no fictional lethal overdose roll.",
                        "The four formats preserve the original 10/20/40/80 effect calibrations and their timing. A 1000mg crystal portion contributes the same calibration as two 500mg tablets; five portions exhaust a full crystal."
                ),
                List.of(
                        step(() -> Items.ACACIA_LOG, "Acacia Log + Axe", () -> Items.IRON_AXE, "Strip the bark", ModItems.ACACIA_BARK, "4x Acacia Bark",
                                "Strip Acacia Logs in survival to collect four bark. Breaking Acacia Leaves supplies two Acacia Leaf items. This is the shared MDMA and 2C-B starting path."),
                        step(ModItems.ACACIA_BARK, "Bark + Leaf + Water", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.PLANT_EXTRACT, "1x Plant Extract",
                                "Brew one Acacia Bark, one Acacia Leaf, and one Water Bucket with fuel; the empty bucket returns. Combine five extracts in shapeless crafting to obtain one Bundled Plant Extract."),
                        step(ModItems.BUNDLED_PLANT_EXTRACT, "Bundle + Ethanol + Glass Bottle", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.AROMATIC_EXTRACT, "1x Aromatic Extract",
                                "Place the bundle in source, Ethanol in leaf, and an empty Glass Bottle in liquid. Ethanol is crafted from Sugar, a Water Bottle, and a Glass Bottle."),
                        step(ModItems.AROMATIC_EXTRACT, "1x Aromatic Extract", ModItems.CHEMISTRY_TABLE, "Chemistry Table: select 2C-B", ModItems.PHENETHYLAMINE_BASE, "1x Phenethylamine Base",
                                "Choose 2C-B at the Aromatic Extract selector. The MDMA selection still produces Refined Aromatic Oil."),
                        step(ModItems.PHENETHYLAMINE_BASE, "Base + Distilled Solvent", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.DIMETHOXY_INTERMEDIATE, "1x Dimethoxy Intermediate",
                                "Distill Ethanol at the Chemistry Table to obtain Distilled Solvent, the existing distilled ethanol item. Brew one base with one solvent and fuel, leaving leaf empty; the extra empty bottle returns."),
                        step(ModItems.ACACIA_BARK, "1x Acacia Bark", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.AMINOETHYL_REAGENT, "1x Aminoethyl Reagent",
                                "Process one extra bark with fuel to obtain the shared Aminoethyl Reagent."),
                        step(ModItems.DIMETHOXY_INTERMEDIATE, "Intermediate + Aminoethyl Reagent", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.TWO_CE_PRECURSOR, "1x 2C-E Precursor",
                                "Brew one Dimethoxy Intermediate with one Aminoethyl Reagent and fuel, leaving leaf empty. Either source/liquid order works. One bottle becomes the product and the extra empty Glass Bottle returns."),
                        step(ModItems.TWO_CE_PRECURSOR, "1x 2C-E Precursor", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.TWO_CE_FREEBASE, "1x 2C-E Freebase",
                                "Convert one precursor into one freebase with fuel. The precursor bottle becomes the freebase bottle."),
                        step(ModItems.TWO_CE_FREEBASE, "1x 2C-E Freebase", () -> Items.FURNACE, "Furnace, Blast Furnace, Smoker, or Campfire", ModItems.TWO_CE_CRYSTAL, "1x 5000mg 2C-E Crystal",
                                "All supported heating paths produce one full crystal. Right-click it for one of five 1000mg portions."),
                        step(ModItems.TWO_CE_CRYSTAL, "1x Full 5000mg Crystal", ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle", ModItems.TWO_CE_POWDER, "50x 100mg 2C-E Powder",
                                "Grind the remaining crystal content into 100mg powders. A full crystal yields fifty; after one direct portion it yields forty."),
                        step(ModItems.TWO_CE_POWDER, "100mg Powder + Sugar", ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table", ModItems.TWO_CE_100MG, "Selected 25 / 100 / 250 / 500mg Tablets",
                                "Select the tablet size. Complete batches preserve all powder content and use one Sugar per finished tablet.")
                ),
                List.of(
                        recipe("Aminoethyl Reagent", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.ACACIA_BARK, "1x Acacia Bark"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.AMINOETHYL_REAGENT, "1x Aminoethyl Reagent"), List.of("The same reagent used by the 4-HO-MET and AL-LAD chains.")),
                        recipe("2C-E Precursor", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.DIMETHOXY_INTERMEDIATE, "1x Dimethoxy Intermediate"), ref(ModItems.AMINOETHYL_REAGENT, "1x Aminoethyl Reagent"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.TWO_CE_PRECURSOR, "1x 2C-E Precursor"), List.of("Use source and liquid slots in either order, with leaf empty. The extra empty bottle returns.")),
                        recipe("2C-E Freebase", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.TWO_CE_PRECURSOR, "1x 2C-E Precursor"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.TWO_CE_FREEBASE, "1x 2C-E Freebase"), List.of("The input bottle becomes the product bottle; no duplicate glass.")),
                        recipe("2C-E Crystal", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.TWO_CE_FREEBASE, "1x 2C-E Freebase")),
                                ref(ModItems.TWO_CE_CRYSTAL, "1x 5000mg 2C-E Crystal"), List.of("Use a Furnace, Blast Furnace, Smoker, or Campfire.")),
                        recipe("2C-E Powder", CursedFindingRecipeType.MORTAR_AND_PESTLE,
                                List.of(ref(ModItems.TWO_CE_CRYSTAL, "1x Full Crystal")),
                                ref(ModItems.TWO_CE_POWDER, "50x 100mg Powder"), List.of("Only the saved remaining content is ground. Each consumed 1000mg portion reduces yield by ten powders.")),
                        recipe("2C-E 25mg Tablets", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.TWO_CE_POWDER, "1x Powder: 100mg"), ref(Items.SUGAR, "4x Sugar")),
                                ref(ModItems.TWO_CE_25MG, "4x 2C-E 25mg"), List.of("Select 25mg. A full crystal can supply two hundred tablets.")),
                        recipe("2C-E 100mg Tablet", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.TWO_CE_POWDER, "1x Powder: 100mg"), ref(Items.SUGAR, "1x Sugar")),
                                ref(ModItems.TWO_CE_100MG, "1x 2C-E 100mg"), List.of("Select 100mg. A full crystal can supply fifty tablets.")),
                        recipe("2C-E 250mg Tablets", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.TWO_CE_POWDER, "5x Powder: 500mg"), ref(Items.SUGAR, "2x Sugar")),
                                ref(ModItems.TWO_CE_250MG, "2x 2C-E 250mg"), List.of("Select 250mg. Complete pairs conserve the 500mg powder batch; a full crystal supplies twenty tablets.")),
                        recipe("2C-E 500mg Tablet", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.TWO_CE_POWDER, "5x Powder: 500mg"), ref(Items.SUGAR, "1x Sugar")),
                                ref(ModItems.TWO_CE_500MG, "1x 2C-E 500mg"), List.of("Select 500mg. A full crystal can supply ten tablets.")),
                        recipe("2C-E Blend Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.TWO_CE_250MG, "1x 2C-E 250mg"), ref(ModItems.TWO_CE_25MG, "1x 2C-E 25mg")),
                                ref(ModItems.BLEND_CAPSULE, "1x Blend Capsule: 275mg 2C-E"),
                                List.of("Shapeless recipe. One tablet per occupied slot contributes its full physical content and original effect calibration."))
                ),
                List.of(
                        "The chain follows 2C-B through Dimethoxy Intermediate. Chemistry still turns that intermediate into 2C-B Freebase; brewing it with Aminoethyl Reagent creates the 2C-E branch.",
                        "All four finished tablets keep the existing hex-seal artwork and original effects. Their item names and trip bar display 25mg, 100mg, 250mg, or 500mg. Dealers carry those same four formats at their existing prices and demand tiers.",
                        "One 5000mg crystal gives five 1000mg portions, or fifty 100mg powders. Grinding a partially consumed crystal converts only what remains.",
                        "Advanced Chemistry uses complete batches with Sugar. Missing ingredients, incomplete batches, or blocked output consume nothing; surplus powder remains in the input.",
                        "The finding unlocks after a tablet, crystal portion, or containing Blend Capsule is consumed and stays discovered through saves."
                )
        ));
        entries.add(entry(
                CursedFindingIds.MESCALINE,
                "Mescaline",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.MESCALINE_100MG,
                List.of(
                        "The desert finding gathers itself into green columns crowned with pale thorns. Mescaline Cactus grows in modest patches across Desert and the Badlands, Eroded Badlands, and Wooded Badlands. It is common enough for a patient search, but the sand still makes you look.",
                        "Mescaline unfolds slowly: warmth in the colors, soft movement across block faces, and patterns that grow from the world instead of shutting it away. The deeper peak brings flowing geometry, symmetry, and space breathing to a long rhythm.",
                        "Break one cactus block to collect one Mescaline Cactus item. Shapeless crafting turns one, two, three, or four cacti into one Mescaline 25mg, 100mg, 250mg, or 500mg item. Each cactus needs its own crafting slot."
                ),
                List.of(
                        "The first prepared dose opens this page. Right-click a finished Mescaline item to consume one serving. A Blend Capsule containing Mescaline opens the same finding.",
                        "The Trip Status bar follows Come-up, Opening, Plateau, Deep Peak, Long Descent, and Afterglow. Even a strong first dose travels through the quiet opening before its peak appears.",
                        "At 25mg the finding is gentle: warm color, soft surface breathing, and a little extra depth. At the default time scale its session lasts about two minutes.",
                        "At 100mg richer colors, organic flow, and soft trails become more present. The opening takes its time; a fresh session lasts about three and a half minutes at the default time scale.",
                        "At 250mg tessellation and symmetry become clearer, the world gains layered depth, and recursive patterns gather around the deep peak. A fresh session lasts about five minutes at the default time scale.",
                        "At 500mg the same signature deepens: more flowing geometry, stronger symmetry, and more complex repeated patterns. A fresh session lasts about six minutes at the default time scale.",
                        "The body can feel warm and gently heavy, with a small nausea wave during the come-up. Camera sway and subtle changes in sound follow the slow breathing rhythm. Tavern music can feed the flowing colors and ribbons.",
                        "Further servings join the living session. The displayed total rises toward the new target, while acute tolerance softens how much of each repeated dose reaches the active effect. Pattern complexity keeps growing with stacked totals; one continuous session has a twelve-minute duration ceiling.",
                        "Mescaline contributes to the shared psychedelic blend while keeping its own warm, organic signature. Cannabis can strengthen the shared psychedelic channels; ketamine can mask them. Repeated psychedelic activity also builds the shared tolerance described in the tolerance finding.",
                        "The long descent leaves a gentle authored afterglow. There is no Mescaline-specific lethal overdose roll in this game chain."
                ),
                List.of(
                        step(ModItems.MESCALINE_CACTUS, "Desert / Badlands Cactus", () -> Items.IRON_AXE, "Break the cactus block", ModItems.MESCALINE_CACTUS, "1x Mescaline Cactus",
                                "Search Desert, Badlands, Eroded Badlands, and Wooded Badlands for small cactus patches. Each harvested block returns exactly one cactus item."),
                        step(ModItems.MESCALINE_CACTUS, "1 / 2 / 3 / 4 Cacti", () -> Items.CRAFTING_TABLE, "Shapeless crafting", ModItems.MESCALINE_25MG, "25mg / 100mg / 250mg / 500mg",
                                "One cactus makes one 25mg item, two make one 100mg item, three make one 250mg item, and four make one 500mg item. Put each cactus in a separate slot; the 2x2 inventory grid also fits all four recipes."),
                        step(ModItems.MESCALINE_100MG, "Prepared Mescaline + Empty Capsule", () -> Items.CRAFTING_TABLE, "Optional capsule blending", ModItems.BLEND_CAPSULE, "Blend Capsule",
                                "Finished doses can be enclosed in an Empty Capsule, alongside other supported substances if desired. Each occupied dose slot contributes one serving; raw cactus cannot enter the capsule.")
                ),
                List.of(
                        recipe("Mescaline 25mg", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.MESCALINE_CACTUS, "1x Mescaline Cactus")),
                                ref(ModItems.MESCALINE_25MG, "1x Mescaline 25mg"),
                                List.of("Shapeless recipe in a crafting table or inventory grid. One cactus becomes one 25mg serving.")),
                        recipe("Mescaline 100mg", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.MESCALINE_CACTUS, "2x Mescaline Cactus")),
                                ref(ModItems.MESCALINE_100MG, "1x Mescaline 100mg"),
                                List.of("Shapeless recipe. Put two cacti in separate slots to make one 100mg serving.")),
                        recipe("Mescaline 250mg", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.MESCALINE_CACTUS, "3x Mescaline Cactus")),
                                ref(ModItems.MESCALINE_250MG, "1x Mescaline 250mg"),
                                List.of("Shapeless recipe. Put three cacti in separate slots to make one 250mg serving.")),
                        recipe("Mescaline 500mg", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.MESCALINE_CACTUS, "4x Mescaline Cactus")),
                                ref(ModItems.MESCALINE_500MG, "1x Mescaline 500mg"),
                                List.of("Shapeless recipe. Put four cacti in separate slots to make one 500mg serving.")),
                        recipe("Mescaline Blend Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.MESCALINE_25MG, "1x Mescaline 25mg"),
                                        ref(ModItems.MESCALINE_100MG, "1x Mescaline 100mg")),
                                ref(ModItems.BLEND_CAPSULE, "1x Blend Capsule: 125mg Mescaline"),
                                List.of("Shapeless recipe. One 25mg serving and one 100mg serving contribute 125mg Mescaline. Each dose needs its own slot.",
                                        "A capsule also accepts any single finished Mescaline dose or a mixture with other supported substance items."))
                ),
                List.of(
                        "Wild cactus patches appear in newly generated Desert and Badlands terrain. Badlands includes its Eroded and Wooded variants; other biomes do not grow this finding naturally.",
                        "A cactus item can be replanted on Sand, Red Sand, ordinary Terracotta of any color, Dirt, Coarse Dirt, Grass Block, or Rooted Dirt, in dry air. Glazed Terracotta and farmland do not support it. Its compact cluster is one block and one harvested item; it has no crop stages, spreading, or vertical growth.",
                        "The cactus is a crafting ingredient. Right-clicking it in the air does not consume Mescaline, and placing it does not start a trip. The four finished dose formats retain their existing direct-use behavior and effects.",
                        "Each dosage recipe produces exactly one finished item. The number of occupied cactus slots selects the format; a larger stack in one slot still supplies only one cactus per craft. Extra ingredients or five cacti do not make a dose.",
                        "The finding unlocks when a prepared dose or Mescaline Blend Capsule is consumed. Discovery is stored with the player and remains after the trip ends.",
                        "The milligram labels and cactus crafting recipes describe fictional Minecraft gameplay."
                )
        ));
        entries.add(entry(
                CursedFindingIds.DMT,
                "DMT",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.DMT,
                List.of(
                        "DMT is the short bright door. It does not ask for a long walk; it throws the room open and counts quickly.",
                        "Its trail begins in acacia: bark shaved from the log, leaves broken loose, both boiled into Plant Extract, then dried down into crystals. Crystals must be ground in the Mortar and Pestle before use: a 1x crystal yields one DMT Powder, and a 5x crystal yields five. Use a powder directly or load it into a Bong or Ancient Pipe for one fictional dose unit.",
                        "Where LSD bends the world and psilocybin grows through it, DMT replaces the frame. At enough strength, ordinary space can fold into tunnels, impossible rooms, symbol chambers, and the feeling that something is studying the player back."
                ),
                List.of(
                        "The first DMT Powder serving, DMT-containing Blend Capsule, or DMT-loaded Bong or Ancient Pipe hit opens this page. Additional servings or hits join the same launch if the trip is still alive, pushing the target higher and stretching the return.",
                        "Low threshold DMT is pressure behind the eyes, bright edges, sudden geometry, and a sense that the room has gained depth in directions it should not have.",
                        "The Tunnel arrives quickly. Color becomes a corridor, sound narrows, and motion feels pulled toward a center you did not choose.",
                        "At pre-breakthrough strength, the chrysanthemum opens: patterned shutters, kaleidoscopic gates, rotating forms, and a sense of machinery made from light.",
                        "At breakthrough strength, the ordinary world can give way to encounter-space. The journal names its masks as impossible architecture, hyperdimensional chambers, living symbol space, machine cathedral, entity observatory, world behind world, and organic entity realm.",
                        "The phases move fast: Rush, Tunnel, Breakthrough if the dose is high enough, Return, and Afterglow. The whole thing is brief compared with LSD, but the peak can be much more vertical.",
                        "DMT can dominate a blended psychedelic trip. Psilocybin may color the walls, LSD may sharpen the symbols, but DMT often takes the steering wheel once breakthrough begins.",
                        "A completed DMT journey can leave luck behind. Stronger launches can also leave a temporary resistance-like blessing and can push against alcohol dependence in recovery."
                ),
                List.of(
                        step(() -> Items.ACACIA_LOG, "Acacia Log + Axe", () -> Items.IRON_AXE, "Strip the bark", ModItems.ACACIA_BARK, "4x Acacia Bark",
                                "Strip an Acacia Log with an axe. The bark falls away as four pieces if the hand is not in creative silence."),
                        step(() -> Items.ACACIA_LEAVES, "Acacia Leaves", () -> Items.IRON_AXE, "Break the leaves", ModItems.ACACIA_LEAF, "2x Acacia Leaf",
                                "Break Acacia Leaves to collect two leaves. The tree gives both skin and breath to this chain."),
                        step(ModItems.ACACIA_BARK, "Acacia Bark + Leaf + Water", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.PLANT_EXTRACT, "Plant Extract",
                                "Bark, leaf, water, and fuel become Plant Extract in the Brew Cauldron."),
                        step(ModItems.PLANT_EXTRACT, "5x Plant Extract", () -> Items.CRAFTING_TABLE, "Bundling", ModItems.BUNDLED_PLANT_EXTRACT, "Bundled Plant Extract",
                                "Five extracts can be bundled before drying, a compact promise of a stronger crystal."),
                        step(ModItems.PLANT_EXTRACT, "Plant Extract", () -> Items.FURNACE, "Furnace, Smoker, or Campfire", ModItems.DMT, "DMT Crystal 1x",
                                "Heat dries a single extract into a 1x crystal. Smoker is quickest, furnace is steady, campfire is slow."),
                        step(ModItems.BUNDLED_PLANT_EXTRACT, "Bundled Plant Extract", () -> Items.FURNACE, "Furnace, Smoker, or Campfire", ModItems.DMT_5X, "DMT Crystal 5x",
                                "A bundled extract dries into the 5x crystal. It holds five units for grinding; the crystal itself cannot be consumed."),
                        step(ModItems.DMT, "1x DMT Crystal 1x", ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle", ModItems.DMT_POWDER, "1x DMT Powder",
                                "Grind one 1x crystal for one powder. The mortar takes 40 ticks and needs room for its complete output."),
                        step(ModItems.DMT_5X, "1x DMT Crystal 5x", ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle", ModItems.DMT_POWDER, "5x DMT Powder",
                                "Grind one 5x crystal for five separate powders. Every powder carries one fictional DMT dose unit; the original five-unit yield is preserved."),
                        ancientPipeProductionStep(),
                        step(ModItems.DMT_POWDER, "One Powder + Empty Ancient Pipe", () -> Items.CRAFTING_TABLE, "Crafting table or inventory", ModItems.ANCIENT_PIPE_DMT, "Loaded Ancient Pipe: One DMT Unit",
                                "Use one powder directly, or craft it beside one empty Ancient Pipe or Bong. Either loaded device gives one DMT hit and returns its own empty vessel; crystals cannot be loaded.")
                ),
                List.of(
                        recipe("Brew Cauldron", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.CAULDRON, "1x Cauldron"), ref(Items.CAMPFIRE, "1x Campfire"), ref(Items.SMOOTH_STONE, "3x Smooth Stone"), ref(Items.OAK_FENCE, "2x Any Fence")),
                                ref(ModItems.BREW_CAULDRON, "Brew Cauldron"),
                                List.of("Build it in this shape: _ A _ / F C F / S S S. A=Cauldron, F=Fence, C=Campfire, S=Smooth Stone.")),
                        recipe("Plant Extract", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.ACACIA_BARK, "1x Acacia Bark"), ref(ModItems.ACACIA_LEAF, "1x Acacia Leaf"), ref(Items.WATER_BUCKET, "1x Water Bucket"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.PLANT_EXTRACT, "1x Plant Extract"),
                                List.of("Brew Cauldron recipe. Bark in the source slot, leaf in the leaf slot, water in the liquid slot, and any valid fuel below.")),
                        recipe("Bundled Plant Extract", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.PLANT_EXTRACT, "5x Plant Extract")),
                                ref(ModItems.BUNDLED_PLANT_EXTRACT, "1x Bundled Plant Extract"),
                                List.of("Shapeless recipe. Five extracts become one bundled extract.")),
                        recipe("DMT Crystal 1x", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.PLANT_EXTRACT, "1x Plant Extract")),
                                ref(ModItems.DMT, "1x DMT Crystal 1x"),
                                List.of("Smoker dries fastest, furnace follows, campfire takes longer. One plant extract becomes one 1x crystal. This crystal is a processing ingredient: grind it before use.")),
                        recipe("DMT Crystal 5x", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.BUNDLED_PLANT_EXTRACT, "1x Bundled Plant Extract")),
                                ref(ModItems.DMT_5X, "1x DMT Crystal 5x"),
                                List.of("The bundled extract dries the same way. The resulting crystal is processing-only and preserves five fictional dose units as five powders after grinding.")),
                        recipe("Mortar and Pestle", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.COBBLESTONE, "2x Cobblestone"), ref(Items.STICK, "1x Stick"), ref(Items.BOWL, "1x Bowl")),
                                ref(ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle"),
                                List.of("Build it in this shape: C _ C / _ S _ / _ B _. C=Cobblestone, S=Stick, B=Bowl, _=Empty.")),
                        recipe("DMT Powder: 1x Crystal", CursedFindingRecipeType.MORTAR_AND_PESTLE,
                                List.of(ref(ModItems.DMT, "1x DMT Crystal 1x")), ref(ModItems.DMT_POWDER, "1x DMT Powder"),
                                List.of("One 1x crystal becomes one powder after 40 ticks. Each powder can be used directly for one fictional dose unit or loaded into either smoking device.")),
                        recipe("DMT Powder: 5x Crystal", CursedFindingRecipeType.MORTAR_AND_PESTLE,
                                List.of(ref(ModItems.DMT_5X, "1x DMT Crystal 5x")), ref(ModItems.DMT_POWDER, "5x DMT Powder"),
                                List.of("One 5x crystal becomes five powders after 40 ticks. The entire five-powder output must fit before the mortar consumes the crystal.")),
                        bongRecipe(),
                        loadDeviceRecipe("Bong", ModItems.BONG, ModItems.DMT_POWDER, "DMT Powder", ModItems.BONG_DMT, "DMT", "1 DMT unit"),
                        ancientPipeRecipe(),
                        loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.DMT_POWDER, "DMT Powder", ModItems.ANCIENT_PIPE_DMT, "DMT", "1 DMT unit"),
                        recipe("DMT Blend Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.DMT_POWDER, "1x DMT Powder")),
                                ref(ModItems.BLEND_CAPSULE, "1x Blend Capsule (1 DMT unit)"),
                                List.of("Shapeless capsule blending. Each occupied DMT Powder slot contributes one fictional unit; supported prepared substances may share the capsule. Unprocessed DMT crystals are not accepted."))
                ),
                List.of(
                        "Acacia Bark drops when a non-creative player strips an Acacia Log with an axe. Acacia Leaf drops when a non-creative player breaks Acacia Leaves.",
                        "The Brew Cauldron consumes bark, leaf, water, and fuel for each Plant Extract. Water buckets leave their bucket behind when the ritual is done.",
                        "Plant Extract and Bundled Plant Extract both dry through furnace, smoker, or campfire. The smoker is quickest, the furnace is steady, and the campfire is the slow road.",
                        "Crystals cannot be consumed as-is or loaded into a smoking device. Grind them first: 1x crystal to one powder, 5x crystal to five powders. Use one powder directly for one fictional unit, or craft one powder with one empty Bong or Ancient Pipe for an equivalent one-hit serving.",
                        "Either loaded device returns the same empty vessel after use, including in Creative mode. A loaded bowl cannot be topped up or mixed. The Utilities / Smoking Devices page lists both devices and their other supported loads.",
                        "New Blend Capsules accept DMT Powder, not crystals. Each occupied powder slot contributes one unit. Existing already-filled capsules keep their saved contents.",
                        "DMT also appears rarely in old chests and strange bundles, but the acacia chain is the craftable path the journal can retrace.",
                        "If several psychedelics are active, watch the Trip Status bar. DMT's yellow mark can briefly outrank the others when the breakthrough gate opens."
                )
        ));
        entries.add(entry(
                CursedFindingIds.SALVIA,
                "Salvia",
                CursedFindingCategory.PSYCHEDELICS,
                ModItems.SALVIA_1X,
                List.of(
                        "The purple finding grows where the jungle keeps its shade. Look for small, uncommon patches beneath Jungle and Sparse Jungle canopy, close to water and away from open sunlight.",
                        "Salvia does not read like the other psychedelic findings. It does not simply brighten the room. It convinces the room that it was staged wrong, as if the world is still made of the same blocks but no longer agrees on what those blocks were for.",
                        "Harvest the mature plant for seeds and fresh leaves, replant the seeds, then dry the leaves over heat. One, two, three, or four dried leaves become the 1x, 10x, 25x, or 50x extract. The finished extract needs a Bong or Ancient Pipe: load one serving through crafting, then take its single hit.",
                        "Every Bong or Ancient Pipe hit joins one escalating session instead of starting over from the beginning. The Trip Status bar writes the road as Load-in, Pull, Dislocation, Recursion, and Shakeout, while stronger extracts push the same chapter harder and keep it alive longer."
                ),
                List.of(
                        "The first Salvia hit from either device opens this page. Later hits do not begin a second separate trip. They stack into the active salvia state, raise the target potency, and stretch the duration of the same unfolding scene.",
                        "At the low end, the room starts to feel staged. The world is still recognizably Minecraft, but it stops feeling innocent. Surfaces feel placed, corners feel intentional, and the tier messages climb from unease toward outright timeline slippage.",
                        "Pull is where the body starts losing its clean agreement with space. Camera roll, yaw drift, and pitch drag build from body pull, anchor slip, and scene replacement rather than from ordinary vanilla potion effects.",
                        "Dislocation is the real center of the finding. Scene replacement and anchor slip become strong enough that space stops feeling trustworthy, especially in still rooms, artificial spaces, villages, caves, and other places the system remembers as arranged.",
                        "Recursion is where the page-turning logic gets loud. Loop seams, repeated-moment pressure, and the feeling that the same place has happened before but wrong all intensify here.",
                        "At heavier strengths, the chapter starts inventing stranger furniture: cutout presences, observer pressure, flattening, zipper-folds, conveyor-wheel pull, stacked realities, objecthood, and kaleidoscopic breakage.",
                        "Sound bends with it. Pitch can sag, volume can thin out, and stillness itself can make the room feel less trustworthy while the world warp and camera pull are active.",
                        "Cannabis can make the salvia state larger while it is active. Ketamine can do the opposite and bury other substance signals under dissociative masking.",
                        "When the chapter fully lets go, the archive may not place you exactly where you began. The session can resolve by waking you somewhere near your bed or stored home anchor."
                ),
                List.of(
                        step(() -> Items.JUNGLE_LEAVES, "Shaded Jungle Patch", () -> Items.WATER_BUCKET, "Search near water", ModItems.SALVIA_SEEDS, "2x Salvia Seeds + 4x Salvia Leaf",
                                "Wild Salvia grows only in Jungle and Sparse Jungle, in rare little patches beneath canopy near water. Break a fully grown plant to collect two seeds and four fresh leaves."),
                        step(ModItems.SALVIA_SEEDS, "Salvia Seeds", () -> Items.FARMLAND, "Farmland", ModItems.FRESH_SALVIA_LEAF, "4x Salvia Leaf + 2x Seeds",
                                "Replant a seed on farmland. Salvia follows Cannabis through four growth stages at the same pace: cultivated farmland is faster than wild plants on soil, and bonemeal can hurry it. Harvest when the purple flowers are fully grown."),
                        step(ModItems.FRESH_SALVIA_LEAF, "Fresh Salvia Leaf", () -> Items.FURNACE, "Furnace, Smoker, or Campfire", ModItems.DRIED_SALVIA_LEAF, "Dried Salvia Leaf",
                                "One fresh leaf dries into one dried leaf. The smoker works quickly, the furnace steadily, and the campfire takes longer."),
                        step(ModItems.DRIED_SALVIA_LEAF, "1 / 2 / 3 / 4 Dried Leaves", () -> Items.CRAFTING_TABLE, "Shapeless crafting", ModItems.SALVIA_1X, "1x / 10x / 25x / 50x Extract",
                                "Use exactly one dried leaf for 1x, two for 10x, three for 25x, or four for 50x. Each recipe makes one extract serving. Put each leaf in its own crafting slot."),
                        step(() -> Items.GLASS, "4x Glass + Water Bucket", () -> Items.CRAFTING_TABLE, "Shaped crafting", ModItems.BONG, "Bong",
                                "Build the glass vessel in this shape: _ G _ / _ G _ / G W G. G is a Glass block, W is a Water Bucket, and _ is empty. The water leaves an empty bucket behind."),
                        step(ModItems.SALVIA_1X, "One Extract + Empty Bong", () -> Items.CRAFTING_TABLE, "Crafting table or inventory", ModItems.BONG_SALVIA_1X, "Loaded Bong: One Hit",
                                "Craft an empty Bong with one extract of any strength. Right-click the loaded Bong to take that one hit; the vessel returns with an empty bowl, ready to load again."),
                        ancientPipeProductionStep(),
                        step(ModItems.SALVIA_1X, "One Extract + Empty Ancient Pipe", () -> Items.CRAFTING_TABLE, "Crafting table or inventory", ModItems.ANCIENT_PIPE_SALVIA_1X, "Loaded Ancient Pipe: One Hit",
                                "The Ancient Pipe accepts the same four extract strengths as the Bong. Craft one serving beside the empty pipe, take its one hit, and recover the same empty Ancient Pipe.")
                ),
                List.of(
                        recipe("Dried Salvia Leaf", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.FRESH_SALVIA_LEAF, "1x Salvia Leaf")),
                                ref(ModItems.DRIED_SALVIA_LEAF, "1x Dried Salvia Leaf"),
                                List.of("Heat one fresh leaf in a furnace, smoker, or campfire. Smoker is fastest, furnace is steady, campfire is slowest.")),
                        recipe("Salvia Divinorum Extract 1x", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.DRIED_SALVIA_LEAF, "1x Dried Salvia Leaf")),
                                ref(ModItems.SALVIA_1X, "1x Extract Serving (1x)"),
                                List.of("Shapeless recipe. One dried leaf becomes one 1x extract serving.")),
                        recipe("Salvia Divinorum Extract 10x", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.DRIED_SALVIA_LEAF, "2x Dried Salvia Leaf")),
                                ref(ModItems.SALVIA_10X, "1x Extract Serving (10x)"),
                                List.of("Shapeless recipe. Two dried leaves in separate slots become one 10x extract serving.")),
                        recipe("Salvia Divinorum Extract 25x", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.DRIED_SALVIA_LEAF, "3x Dried Salvia Leaf")),
                                ref(ModItems.SALVIA_25X, "1x Extract Serving (25x)"),
                                List.of("Shapeless recipe. Three dried leaves in separate slots become one 25x extract serving.")),
                        recipe("Salvia Divinorum Extract 50x", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.DRIED_SALVIA_LEAF, "4x Dried Salvia Leaf")),
                                ref(ModItems.SALVIA_50X, "1x Extract Serving (50x)"),
                                List.of("Shapeless recipe. Four dried leaves in separate slots become one 50x extract serving.")),
                        recipe("Bong", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.GLASS, "4x Glass Blocks"), ref(Items.WATER_BUCKET, "1x Water Bucket")),
                                ref(ModItems.BONG, "1x Empty Bong"),
                                List.of("Build it in this shape: _ G _ / _ G _ / G W G. G=Glass block, W=Water Bucket, _=Empty. The Water Bucket leaves an empty Bucket behind.")),
                        recipe("Load Bong: Salvia 1x", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.BONG, "1x Empty Bong"), ref(ModItems.SALVIA_1X, "1x Salvia Extract 1x")),
                                ref(ModItems.BONG_SALVIA_1X, "1x Loaded Bong (1x)"),
                                List.of("Shapeless recipe in the crafting table or 2x2 inventory grid. Carries one 1x hit; right-click to consume it and recover the empty Bong.")),
                        recipe("Load Bong: Salvia 10x", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.BONG, "1x Empty Bong"), ref(ModItems.SALVIA_10X, "1x Salvia Extract 10x")),
                                ref(ModItems.BONG_SALVIA_10X, "1x Loaded Bong (10x)"),
                                List.of("Shapeless recipe in the crafting table or 2x2 inventory grid. Carries one 10x hit; right-click to consume it and recover the empty Bong.")),
                        recipe("Load Bong: Salvia 25x", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.BONG, "1x Empty Bong"), ref(ModItems.SALVIA_25X, "1x Salvia Extract 25x")),
                                ref(ModItems.BONG_SALVIA_25X, "1x Loaded Bong (25x)"),
                                List.of("Shapeless recipe in the crafting table or 2x2 inventory grid. Carries one 25x hit; right-click to consume it and recover the empty Bong.")),
                        recipe("Load Bong: Salvia 50x", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.BONG, "1x Empty Bong"), ref(ModItems.SALVIA_50X, "1x Salvia Extract 50x")),
                                ref(ModItems.BONG_SALVIA_50X, "1x Loaded Bong (50x)"),
                                List.of("Shapeless recipe in the crafting table or 2x2 inventory grid. Carries one 50x hit; right-click to consume it and recover the empty Bong.")),
                        ancientPipeRecipe(),
                        loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.SALVIA_1X, "Salvia Extract 1x", ModItems.ANCIENT_PIPE_SALVIA_1X, "Salvia 1x", "Salvia 1x"),
                        loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.SALVIA_10X, "Salvia Extract 10x", ModItems.ANCIENT_PIPE_SALVIA_10X, "Salvia 10x", "Salvia 10x"),
                        loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.SALVIA_25X, "Salvia Extract 25x", ModItems.ANCIENT_PIPE_SALVIA_25X, "Salvia 25x", "Salvia 25x"),
                        loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.SALVIA_50X, "Salvia Extract 50x", ModItems.ANCIENT_PIPE_SALVIA_50X, "Salvia 50x", "Salvia 50x")
                ),
                List.of(
                        "Only Jungle and Sparse Jungle grow wild Salvia. Search the shaded ground near water beneath leaves; open sunlit clearings are the wrong place. A few plants together are a finding worth carrying home.",
                        "A mature harvest gives exactly two Salvia Seeds and four fresh Salvia Leaves. Cutting an immature plant returns one seed and no leaves. Seeds can be replanted on farmland; cultivation follows the same four stages and growth pace as Cannabis.",
                        "Fresh leaves must be dried before extract crafting. Each heat recipe keeps the count simple: one fresh leaf becomes one dried leaf.",
                        "Extract is a Bong or Ancient Pipe ingredient. Right-clicking loose extract cannot consume it, and it cannot be tucked into a blended capsule. Loading uses exactly one empty device and one extract serving; a loaded bowl cannot be filled again until its hit is taken.",
                        "Every loaded Bong or Ancient Pipe holds one hit and returns the same empty device after use, including in Creative mode. The strength belongs to the loaded extract: either device preserves the 1x, 10x, 25x, or 50x serving.",
                        "Both empty devices also accept one Dried Cannabis Bud for 200mg THC or one DMT Powder for one DMT unit. Smoke the existing bowl empty before switching substances. See Utilities / Smoking Devices for all crafting and loading recipes.",
                        "The item labels are abstract gameplay intensity marks. The extracts are presented as 1x, 10x, 25x, and 50x style salvia labels, but the journal treats them as Vice and Villagers shorthand rather than outside-world guidance.",
                        "Each run receives its own trip signature. That signature nudges body torque, recursion bias, observer bias, scene bias, cutout bias, and aftershock, so two equal hits can feel related without feeling identical.",
                        "Environment matters more here than in most chapters. Artificial blocks, villages, caves, stillness, movement, nearby entities, threat, night, and low health all feed the final read.",
                        "This finding has a dedicated accessibility mode. It keeps the slower uncanny layering but reduces camera displacement, observer silhouettes, and seam density.",
                        "Salvia labels in this journal are fictional gameplay notation only."
                )
        ));
        entries.add(entry(
                CursedFindingIds.COCAINE,
                "Cocaine",
                CursedFindingCategory.STIMULANTS,
                ModItems.COCAINE_POWDER,
                List.of(
                        "The stimulant finding begins as a bright leaf in hot country. It looks harmless enough until the journal starts underlining its own sentences.",
                        "Cocaine is not a dream-substance. It is a sharp nail of momentum: fast hands, tight focus, harsh light, and the feeling that the next action should already be finished.",
                        "The path starts with Coca leaves. Leaves can be taken directly for a faint coca load, or dried, ground, mixed into paste, and split into two harder branches: powder for a longer bright rush, or crack for a shorter, fiercer flare."
                ),
                List.of(
                        "The first leaf, powder, or crack use opens this page. Every later dose joins the same wired clock while the session is still alive.",
                        "The journal marks the cocaine ride as Onset, Rush, Peak, Edge, Crash, and Afterglow. It climbs quickly, stays bright for a while, then scrapes its way down.",
                        "Powder is the steadier route: less violent than crack, longer in the body, and easier to read on the Trip Status bar.",
                        "Crack is the struck match. It comes on faster, burns brighter, and ends sooner, leaving less room between the Rush and the Edge.",
                        "During the Rush and Peak, movement can feel pushed forward. Speed and Strength may appear, inputs feel sharper, and the world narrows into tunnel focus.",
                        "At heavier loads, the gift curdles. Jitter, overcorrection, compulsive momentum, harsh light, edge shimmer, and micro-flicker begin to crowd the screen.",
                        "Cocaine blocks sleep while it is active. The body may be exhausted, but the finding keeps knocking from inside the walls.",
                        "The Crash is not decorative. Slowness can follow, color can drain, and the aftermath may keep dragging after the main rush is gone.",
                        "Cannabis pulls against cocaine instead of sharpening it. Ketamine can also dull stimulant force. Alcohol may feel masked while cocaine is loud, but the journal does not call that safety.",
                        "2C-B is the sharp exception: cocaine now reinforces its electric geometry, neon outlines, tracer snap, diffraction, and club-beat response. The blend is deliberately cohesive, but it also raises stimulation and anxiety.",
                        "Very high accumulated cocaine load carries heart-attack risk in this fictional system. Above the danger line, every extra push gives the journal another chance to close the page for good."
                ),
                List.of(
                        step(() -> Items.GRASS_BLOCK, "Warm living soil", ModItems.KOKA_SEEDS, "Coca Seeds", ModItems.KOKA_LEAVES, "4x Coca Leaves + 2x Seeds",
                                "Plant the seeds where there is light or sky. A mature Coca Plant gives 4 leaves and 2 seeds; an immature plant gives only a seed."),
                        step(ModItems.KOKA_LEAVES, "Fresh Coca Leaves", () -> Items.FURNACE, "Furnace, Blast Furnace, Smoker, or Campfire", ModItems.DRIED_KOKA_LEAVES, "Dried Coca Leaves",
                                "Fresh leaves must be dried before the mortar can hear them. Smoker is fastest, furnace is steady, campfire is slow."),
                        step(ModItems.DRIED_KOKA_LEAVES, "Dried Coca Leaves", ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle", ModItems.GROUND_KOKA_LEAVES, "Ground Coca Leaves",
                                "The dried leaves become ground leaf under stone, pale and restless."),
                        step(ModItems.GROUND_KOKA_LEAVES, "4x Ground Leaves + Sugar + Frost Salt", () -> Items.CRAFTING_TABLE, "Paste Work", ModItems.KOKA_PASTE, "Coca Paste",
                                "Four ground leaves, sugar, and Frost Salt become Coca Paste. The paste is the fork in the stimulant road."),
                        step(ModItems.KOKA_PASTE, "Coca Paste + Frost Salt", () -> Items.CRAFTING_TABLE, "Powder Branch", ModItems.COCAINE_POWDER, "4x Cocaine Powder",
                                "Loose paste and Frost Salt split into four powder doses."),
                        step(ModItems.KOKA_PASTE, "Coca Paste", () -> Items.FURNACE, "Heat", ModItems.HARDENED_KOKA_PASTE, "Hardened Coca Paste",
                                "Heating paste hardens the branch. From here the road gets shorter and more violent."),
                        step(ModItems.HARDENED_KOKA_PASTE, "Hardened Paste + Frost Salt", () -> Items.CRAFTING_TABLE, "Crack Branch", ModItems.CRACK_COCAINE, "Crack Cocaine",
                                "Hardened paste and Frost Salt become the crack route: fewer items, stronger fictional impact.")
                ),
                List.of(
                        recipe("Mortar and Pestle", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.COBBLESTONE, "2x Cobblestone"), ref(Items.STICK, "1x Stick"), ref(Items.BOWL, "1x Bowl")),
                                ref(ModItems.MORTAR_AND_PESTLE, "Mortar and Pestle"),
                                List.of("Build it in this shape: C C / _ S _ / _ B _. C=Cobblestone, S=Stick, B=Bowl.")),
                        recipe("Frost Salt", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.REDSTONE, "1x Redstone"), ref(Items.GLOWSTONE_DUST, "1x Glowstone Dust")),
                                ref(ModItems.FROST_SALT, "1x Frost Salt"),
                                List.of("Shape the two dusts as R G. In this world, Frost Salt is a fictional reagent for bright, cold refinements.")),
                        recipe("Dried Coca Leaves", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.KOKA_LEAVES, "1x Coca Leaves")),
                                ref(ModItems.DRIED_KOKA_LEAVES, "1x Dried Coca Leaves"),
                                List.of("Smoker dries fastest, furnace follows, and campfire takes longer.")),
                        recipe("Ground Coca Leaves", CursedFindingRecipeType.MORTAR_AND_PESTLE,
                                List.of(ref(ModItems.DRIED_KOKA_LEAVES, "1x Dried Coca Leaves")),
                                ref(ModItems.GROUND_KOKA_LEAVES, "1x Ground Coca Leaves"),
                                List.of("The mortar accepts dried Coca Leaves and returns ground leaves.")),
                        recipe("Coca Paste", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.GROUND_KOKA_LEAVES, "4x Ground Coca Leaves"), ref(Items.SUGAR, "1x Sugar"), ref(ModItems.FROST_SALT, "1x Frost Salt")),
                                ref(ModItems.KOKA_PASTE, "1x Coca Paste"),
                                List.of("Shapeless recipe. This is the central paste before powder or crack.")),
                        recipe("Cocaine Powder", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.KOKA_PASTE, "1x Coca Paste"), ref(ModItems.FROST_SALT, "1x Frost Salt")),
                                ref(ModItems.COCAINE_POWDER, "4x Cocaine Powder"),
                                List.of("Shapeless recipe. Each powder item carries a 1.0g fictional cocaine load.")),
                        recipe("Hardened Coca Paste", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.KOKA_PASTE, "1x Coca Paste")),
                                ref(ModItems.HARDENED_KOKA_PASTE, "1x Hardened Coca Paste"),
                                List.of("Smoker, furnace, or campfire can harden paste. The smoker is fastest.")),
                        recipe("Crack Cocaine", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.HARDENED_KOKA_PASTE, "1x Hardened Coca Paste"), ref(ModItems.FROST_SALT, "1x Frost Salt")),
                                ref(ModItems.CRACK_COCAINE, "1x Crack Cocaine"),
                                List.of("Shapeless recipe. Crack carries a 5.0g fictional load and uses the faster, shorter, stronger route."))
                ),
                List.of(
                        "Wild Coca Plants favor Jungle, Sparse Jungle, Bamboo Jungle, River, Savanna, Savanna Plateau, and Windswept Savanna.",
                        "The plant accepts farmland, grass, dirt, coarse dirt, podzol, moss, rooted dirt, mud, and similar earth. It wants growth light or a clear line to the sky.",
                        "Farmland speeds the odds. Bonemeal can hurry the plant by one or two stages.",
                        "Coca Leaves can be used directly as a tiny 0.10g coca finding. It is weak, but it is enough for the journal to recognize the stimulant family.",
                        "Cocaine Powder and Crack Cocaine use fictional gram labels. The numbers are Vice and Villagers intensity marks, not outside-world guidance.",
                        "If the rush is active, sleep will refuse you. Wait for the page to stop vibrating.",
                        "The heart-attack check begins only at extreme accumulated cocaine load. That does not make the lower road kind; it only means the journal has not started rolling that particular die yet."
                )
        ));
        entries.add(entry(
                CursedFindingIds.HEROIN,
                "Heroin",
                CursedFindingCategory.OPIOIDS,
                ModItems.HEROIN_100MG,
                List.of(
                        "The opioid finding grows in plain sight, all pale stem and patient flower. The danger is not loud at first. It is warm, heavy, and very good at sounding like mercy.",
                        "Heroin is the amber blanket of the archive: soft focus, slowed hands, pain pushed far away, and a body that starts forgetting why standing mattered.",
                        "The path begins with Opium Poppies. Mature flowers can be tapped once per day for Latex, then the chain passes through heat, Morphine Base, the Chemistry Table, Heroin Base, and finally the Advanced Chemistry Table's measured portions."
                ),
                List.of(
                        "The journal fully opens this page when a true 25mg-or-higher heroin dose is tried. Smaller precursor traces are too faint for the ink to confess the name.",
                        "The road is Onset, Warmth, Bliss, Nod, Comedown, and Afterglow. The Nod appears only when the active dose is heavy enough.",
                        "At low strength, the world softens. Warmth rises, nausea may fall away, and pain feels farther from the player than it has any right to be.",
                        "Moderate heroin brings soft focus, euphoria, hunger suppression, regeneration, resistance, absorption, and a honeyed sense that danger is happening somewhere else.",
                        "Strong heroin turns the blessing into weight. Slowness, mining fatigue, motor drag, nodding, micro-sleep, and slow falling can arrive as the body starts slipping out of command.",
                        "Very heavy heroin carries shutdown pressure. The journal marks this as respiratory suppression, weakness, deep slowness, and overdose risk.",
                        "Overdose checks begin above a threshold that rises with tolerance. Tolerance is not safety; it only moves the line while dependence grows behind it.",
                        "When a qualified session ends, the recovery ledger may add tolerance and dependence. Heavy sessions can leave a crash, withdrawal pressure, food penalty, and damage ticks if the body is abandoned.",
                        "Sleep helps recovery. Cannabis can soften the nutrition penalty. Active heroin can suppress crash and withdrawal for a while, but the ledger keeps counting in the margins.",
                        "A sufficiently heavy active heroin dose can overpower MDMA or 2C-B insomnia. The requirement is dose-balanced: a 25mg sedative floor plus roughly half the combined active MDMA and 2C-B total, with enough actual sedation to account for tolerance and mixture masking. Sleeping still ends the active mixture."
                ),
                List.of(
                        step(() -> Items.GRASS_BLOCK, "Open living soil", ModItems.OPIUM_POPPY_SEEDS, "Opium Poppy Seeds", ModItems.OPIUM_LATEX, "Opium Latex",
                                "Grow the poppy to maturity, then tap it by hand. A mature flower gives one Latex per Minecraft day without breaking the plant."),
                        step(ModItems.OPIUM_LATEX, "Opium Latex", () -> Items.FURNACE, "Furnace, Blast Furnace, Smoker, or Campfire", ModItems.RAW_OPIUM, "Raw Opium",
                                "Heat thickens the latex into Raw Opium. Smoker is fastest, furnace is steady, campfire is slow."),
                        step(ModItems.RAW_OPIUM, "Raw Opium", () -> Items.FURNACE, "Furnace, Blast Furnace, Smoker, or Campfire", ModItems.MORPHINE_BASE, "Morphine Base",
                                "A second heating turns Raw Opium into Morphine Base, the chain's quiet hinge."),
                        step(ModItems.MORPHINE_BASE, "Morphine Base + Fuel", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.HEROIN_BASE, "Heroin Base",
                                "The Chemistry Table refines Morphine Base into Heroin Base with fuel. The result is still unfinished and unstable in the journal's eyes."),
                        step(ModItems.HEROIN_BASE, "Heroin Base + Glass", ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table", ModItems.HEROIN_100MG, "Measured Heroin",
                                "The Advanced Chemistry Table divides base with glass into 25mg, 100mg, 250mg, or 500mg fictional portions.")
                ),
                List.of(
                        recipe("Opium Latex", CursedFindingRecipeType.WORLD_INTERACTION,
                                List.of(ref(ModItems.OPIUM_POPPY_SEEDS, "Crop Source")),
                                ref(ModItems.OPIUM_LATEX, "1x Opium Latex"),
                                List.of("Use a mature Opium Poppy by hand. Each plant can be tapped once per Minecraft day.")),
                        recipe("Raw Opium", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.OPIUM_LATEX, "1x Opium Latex")),
                                ref(ModItems.RAW_OPIUM, "1x Raw Opium"),
                                List.of("Smoker dries fastest, furnace follows, and campfire takes longer.")),
                        recipe("Morphine Base", CursedFindingRecipeType.HEATING,
                                List.of(ref(ModItems.RAW_OPIUM, "1x Raw Opium")),
                                ref(ModItems.MORPHINE_BASE, "1x Morphine Base"),
                                List.of("A second heating prepares the base for the Chemistry Table.")),
                        recipe("Chemistry Table", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.GLASS_BOTTLE, "2x Glass Bottle"), ref(Items.REDSTONE, "1x Redstone"), ref(Items.IRON_INGOT, "2x Iron Ingot"), ref(Items.CRAFTING_TABLE, "1x Crafting Table"), ref(Items.OAK_PLANKS, "2x Any Planks"), ref(Items.FURNACE, "1x Furnace")),
                                ref(ModItems.CHEMISTRY_TABLE, "Chemistry Table"),
                                List.of("Build it in this shape: G R G / I C I / W F W. G=Glass Bottle, R=Redstone, I=Iron, C=Crafting Table, W=Any Planks, F=Furnace.")),
                        recipe("Heroin Base", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.MORPHINE_BASE, "1x Morphine Base"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.HEROIN_BASE, "1x Heroin Base"),
                                List.of("Distill Morphine Base at the Chemistry Table with any valid fuel.")),
                        recipe("Advanced Chemistry Table", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.DIAMOND, "2x Diamond"), ref(Items.BREWING_STAND, "1x Brewing Stand"), ref(Items.GOLD_INGOT, "2x Gold Ingot"), ref(ModItems.CHEMISTRY_TABLE, "1x Chemistry Table"), ref(Items.OBSIDIAN, "2x Obsidian"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table"),
                                List.of("Build it in this shape: D B D / G C G / O L O. D=Diamond, B=Brewing Stand, G=Gold, C=Chemistry Table, O=Obsidian, L=Glass.")),
                        recipe("25mg Heroin", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.HEROIN_BASE, "1x Heroin Base"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.HEROIN_25MG, "4x Heroin 25mg"),
                                List.of("Select 25mg on the Advanced Chemistry Table. One base and one glass become four small portions.")),
                        recipe("100mg Heroin", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.HEROIN_BASE, "1x Heroin Base"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.HEROIN_100MG, "1x Heroin 100mg"),
                                List.of("Select 100mg. One base and one glass become one standard portion.")),
                        recipe("250mg Heroin", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.HEROIN_BASE, "5x Heroin Base"), ref(Items.GLASS, "2x Glass")),
                                ref(ModItems.HEROIN_250MG, "2x Heroin 250mg"),
                                List.of("Select 250mg. Five base and two glass become two strong portions.")),
                        recipe("500mg Heroin", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.HEROIN_BASE, "5x Heroin Base"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.HEROIN_500MG, "1x Heroin 500mg"),
                                List.of("Select 500mg. Five base and one glass become one heavy portion. The journal presses the ink down hard here."))
                ),
                List.of(
                        "Wild Opium Poppies appear in Plains, Sunflower Plains, Meadow, and more rarely Windswept Hills.",
                        "The plant accepts farmland, grass, dirt, coarse dirt, podzol, moss, rooted dirt, mud, and similar earth. It wants growth light or sky.",
                        "Breaking an age-2 poppy drops one seed. Breaking a mature poppy drops two seeds. Latex comes from tapping the living mature plant, not from breaking it.",
                        "Latex, Morphine Base, and Heroin Base have faint fictional opioid loads if used directly, but the chapter only fully names Heroin at 25mg or above.",
                        "Heroin dose labels are fictional gameplay milligrams. They exist for Vice and Villagers balance and UI, not outside-world instruction.",
                        "Tolerance can make the warm part smaller and shorter. Dependence can make the cold part longer and meaner.",
                        "Recovery clears with sober time: three Minecraft days for most states, five days for late dependence. Sleeping pushes the clock forward.",
                        "The old Heroin Tar item remains only as a legacy item. This chapter follows the current base-and-portion chain."
                )
        ));
        entries.add(entry(
                CursedFindingIds.MDMA,
                "MDMA",
                CursedFindingCategory.EMPATHOGENS,
                ModItems.MDMA_100MG,
                List.of(
                        "MDMA is the neon finding: a four-stage aromatic chain that ends in a reusable-looking crystal reservoir holding exactly 5000mg of fictional gameplay charge. That charge can be packaged as sugar-pressed 500mg tablets or measured 25mg, 100mg, 250mg, and 500mg capsules.",
                        "This chapter is built around warmth, music, and company. The come-up lifts color and movement softly; the peak turns every calibrated kick in the tavern playlist into a synchronized pulse through the screen and the club's physical disco lights.",
                        "The names, materials, and milligram labels are deliberately fictional game balance, not chemistry. Vice and Villagers treats MDMA, its production chain, its mixtures, and every interaction described here as an invented experience system."
                ),
                List.of(
                        "Come-up begins with a gentle warm wash, a little saturation and bloom, fuller tavern music, and a lightness in movement. The smallest tablet keeps this subtle; higher strengths climb faster and farther.",
                        "At Peak and Roll, color and glow intensify. The nightclub playlist clock drives a sharp bass pulse shared by the MDMA vignette, FOV breathing, floor glow, and moving disco beams, so the room reacts to the music instead of using an unrelated visual timer.",
                        "The nightclub is an amplifier. Anywhere inside its footprint or within ten blocks outside it, every active MDMA channel runs at one-and-a-half strength: color, bloom, music fullness, movement, social pull, touch, and the later comedown all rise together.",
                        "Peak movement gets a mild Speed lift, hunger drains more slowly, and stronger rolls may briefly grant Regeneration. These are short fictional buffs shaped by dose and phase rather than permanent potion effects.",
                        "Nearby villagers answer the social aura with heart and music-note particles. Nightlife guests are encouraged toward the player, and a strong peak can improve dealer prices by as much as twenty-five percent.",
                        "Interacting with another player or villager during a social peak creates a shared heart-and-note burst. Both sides receive a short positive touch buff, with a cooldown so it remains a moment rather than a spam machine.",
                        "The bright roll tapers smoothly before its trip bar ends. Only after that bar disappears does the separate comedown begin, pulling saturation away, adding fatigue and hunger pressure, and softening the tavern mix.",
                        "The complete active MDMA timeline now runs at one-and-a-half its original length. Initial sessions and every re-dose extension use the same 1.5x timing, while the separately queued hangover clock keeps its own dose-based duration.",
                        "A 500mg session queues roughly half a Minecraft day of recovery. Lower formats fade sooner, while stacked doses preserve and add to the recovery clock; repeated use still gives diminishing euphoria and a heavier hangover tail.",
                        "The inventory ledger shows cyan tolerance above pink 5-HT depletion. Tolerance makes the same format feel weaker, while depletion makes the delayed withdrawal heavier. Recovery now lasts one to three sober Minecraft days: only consuming a daily total above 250mg for three Minecraft days in a row reaches the three-day maximum, and sleep still advances recovery.",
                        "An active roll blocks sleep unless a sufficiently heavy active heroin dose overpowers it through the shared dose-balanced sleep rule. Once the roll ends, MDMA comedown, hangover, and depletion follow ordinary Minecraft bed rules: they no longer allow daytime sleep, though a normal successful night's sleep still advances recovery. Re-dosing suppresses the cold state without erasing it.",
                        "Alcohol makes an MDMA roll less coordinated and cannabis makes its neon dreamier. Ketamine blends into the roll while mildly turning its gain down; cocaine pushes its intensity upward with each active gram.",
                        "MDMA's final color grade sits over the complete mixture, raising dose-dependent saturation and bloom around every other active visual system without replacing their shapes or turning the roll psychedelic.",
                        "This build also gives extreme active totals a fictional heart-attack roll. With no tolerance it begins above 500mg at one chance in five and rises above 1000mg to one chance in two; tolerance raises those gameplay thresholds."
                ),
                List.of(
                        step(ModItems.BUNDLED_PLANT_EXTRACT, "Bundle + Ethanol + Empty Bottle", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.AROMATIC_EXTRACT, "Aromatic Extract",
                                "Place Bundled Plant Extract, Ethanol, and an empty glass bottle into the Brew Cauldron with fuel. One complete brew fills the bottle with Aromatic Extract."),
                        step(ModItems.AROMATIC_EXTRACT, "Aromatic Extract + Fuel", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.REFINED_AROMATIC_OIL, "Refined Aromatic Oil",
                                "Distill the aromatic feedstock at the Chemistry Table. The table consumes ordinary furnace fuel and returns one bottle of Refined Aromatic Oil."),
                        step(ModItems.REFINED_AROMATIC_OIL, "Oil + Medicinal Reagent", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.CRUDE_EMPATHOGEN_BASE, "Crude Empathogen Base",
                                "Return the oil to the Brew Cauldron with Medicinal Reagent and fuel. The finished bottle contains Crude Empathogen Base."),
                        step(ModItems.CRUDE_EMPATHOGEN_BASE, "Base + Frost Salt", () -> Items.CRAFTING_TABLE, "Shapeless Crafting", ModItems.MDMA_CRYSTAL, "MDMA Crystal: 5000mg",
                                "Combine one Crude Empathogen Base with one Frost Salt. The crystal is a 5000mg reservoir, and its durability-style bar records charge already used."),
                        step(ModItems.MDMA_CRYSTAL, "Crystal in Fuel Slot + Sugar Above", ModItems.CANNABIS_PRESS, "Cannabis Press", ModItems.MDMA_500MG, "MDMA 500mg Tablet",
                                "Each press cycle consumes one Sugar and 500mg from the crystal. A fresh crystal therefore makes ten 500mg tablets."),
                        step(ModItems.MDMA_CRYSTAL, "Crystal + Empty Capsules", ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table", ModItems.MDMA_CAPS_100MG, "Selected MDMA Caps",
                                "Choose 25mg, 100mg, 250mg, or 500mg. Each cycle consumes one empty capsule and exactly the selected charge; the partial crystal stays in the table.")
                ),
                List.of(
                        recipe("Aromatic Extract", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.BUNDLED_PLANT_EXTRACT, "1x Bundle of Plant Extract"), ref(ModItems.ETHANOL, "1x Ethanol"), ref(Items.GLASS_BOTTLE, "1x Empty Bottle"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.AROMATIC_EXTRACT, "1x Aromatic Extract"),
                                List.of("Brew the plant bundle and ethanol into the waiting empty bottle. Any valid Brew Cauldron fuel works.")),
                        recipe("Refined Aromatic Oil", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.AROMATIC_EXTRACT, "1x Aromatic Extract"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.REFINED_AROMATIC_OIL, "1x Refined Aromatic Oil"),
                                List.of("Distill at the Chemistry Table with any valid furnace fuel.")),
                        recipe("Crude Empathogen Base", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.REFINED_AROMATIC_OIL, "1x Refined Aromatic Oil"), ref(ModItems.MEDICINAL_REAGENT, "1x Medicinal Reagent"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.CRUDE_EMPATHOGEN_BASE, "1x Crude Empathogen Base"),
                                List.of("Brew the refined oil with Medicinal Reagent. The oil bottle becomes the base bottle; the reagent returns its empty glass bottle.")),
                        recipe("MDMA Crystal", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.CRUDE_EMPATHOGEN_BASE, "1x Crude Empathogen Base"), ref(ModItems.FROST_SALT, "1x Frost Salt")),
                                ref(ModItems.MDMA_CRYSTAL, "1x MDMA Crystal: 5000mg"),
                                List.of("Shapeless recipe. The new crystal begins with a full 5000mg reservoir.")),
                        recipe("Pressed MDMA 500mg", CursedFindingRecipeType.CANNABIS_PRESS,
                                List.of(ref(Items.SUGAR, "1x Sugar in Top Slot"), ref(ModItems.MDMA_CRYSTAL, "500mg Crystal Charge in Fuel Slot")),
                                ref(ModItems.MDMA_500MG, "1x MDMA 500mg"),
                                List.of("One Sugar and 500mg are consumed per cycle. Ten Sugar package one fresh crystal into ten tablets.")),
                        recipe("MDMA Caps 25mg", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.MDMA_CRYSTAL, "25mg Crystal Charge"), ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule")),
                                ref(ModItems.MDMA_CAPS_25MG, "1x MDMA Caps 25mg"),
                                List.of("Select 25mg. One full crystal can fill 200 capsules. Filling fifty consumes 1250mg and leaves 3750mg in the crystal.")),
                        recipe("MDMA Caps 100mg", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.MDMA_CRYSTAL, "100mg Crystal Charge"), ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule")),
                                ref(ModItems.MDMA_CAPS_100MG, "1x MDMA Caps 100mg"),
                                List.of("Select 100mg. One full crystal can fill fifty capsules.")),
                        recipe("MDMA Caps 250mg", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.MDMA_CRYSTAL, "250mg Crystal Charge"), ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule")),
                                ref(ModItems.MDMA_CAPS_250MG, "1x MDMA Caps 250mg"),
                                List.of("Select 250mg. One full crystal can fill twenty capsules.")),
                        recipe("MDMA Caps 500mg", CursedFindingRecipeType.ADVANCED_CHEMISTRY_TABLE,
                                List.of(ref(ModItems.MDMA_CRYSTAL, "500mg Crystal Charge"), ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule")),
                                ref(ModItems.MDMA_CAPS_500MG, "1x MDMA Caps 500mg"),
                                List.of("Select 500mg. One full crystal can fill ten capsules."))
                ),
                List.of(
                        "The four MDMA Caps use the existing Blend Capsule artwork. Their item names identify the selected fictional strength; the pressed tablets keep the shared neon tablet artwork.",
                        "Fresh MDMA Crystals stack to sixteen. The two packaging-machine crystal slots accept one crystal from that stack at a time, allowing its private 5000mg reservoir and item bar to remain exact.",
                        "A fresh MDMA Crystal stores exactly 5000mg. Its item bar shrinks proportionally after either machine packages a dose, so packaging paths may be mixed without losing or creating charge.",
                        "Right-clicking consumes one crystal from the stack and applies all charge still stored in that crystal. A fresh crystal therefore applies the entire 5000mg fictional dose; a partial crystal applies only its displayed remainder.",
                        "A crystal may be split across sessions. The Advanced Chemistry Table refuses a selection larger than the remaining charge, and the Cannabis Press stops when fewer than 500mg remain.",
                        "Any MDMA format can be crafted with an Empty Capsule and other supported dose items. Multiple tablets merge into one stored milligram entry and the Blend Capsule releases it through the normal MDMA system.",
                        "The MDMA finding unlocks when any finished tablet, MDMA Cap, or crystal charge is consumed.",
                        "Bass pulses are derived from per-track low-frequency onset grids and the same persistent world-time playlist position used for audio seeking.",
                        "The lights follow long cinematic arcs and only accelerate briefly with musical energy; they are not meant to sweep or strobe at maximum speed all night.",
                        "Photosensitivity and motion-reduction settings still reduce the pulse and movement components.",
                        "This page documents fictional Minecraft mechanics only and does not provide real-world dosing or mixing guidance."
                )
        ));
        entries.add(entry(
                CursedFindingIds.NITROUS,
                "Nitrous Oxide",
                CursedFindingCategory.DISSOCIATIVES,
                ModItems.NITROUS,
                List.of(
                        "Nitrous Oxide is the short rush. The world breathes, stretches, and feels euphoric for a handful of seconds, then lets go.",
                        "The canister is a single gameplay hit. Another canister while the rush is still alive joins the same session and extends the clock. The milliliter math of the real world is not used here.",
                        "This page documents fictional Minecraft mechanics only."
                ),
                List.of(
                        "The trip bar writes Inhale, Rush, Peak, Fade, and Afterglow. A fresh canister lasts sixteen seconds; peak remaining time sits near twelve.",
                        "Vision tunnels and fisheyes. An airy white-blue haze settles in. Soft bloom and chromatic split color the edges. Recent frames linger as time-echo trails. The room stretches with a slow bass pulse.",
                        "Sound muffles on the highs, bass warps, and music gets louder and sweeter. Voices sit a little farther away, as if heard through a balloon.",
                        "Brief Luck and Resistance cover the euphoria and pain-suppression notes. Mining Fatigue and a touch of Slowness cover the lost precision, slower reactions, and slight instability. Everything is short.",
                        "There is no production chain. Dealers carry the canister, and village workshops, shipwreck supplies, and woodland mansions can hold a rare canister. A Blend Capsule can store the hit count."
                ),
                List.of(),
                List.of(
                        recipe("Nitrous Blend Capsule", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(ModItems.EMPTY_CAPSULE, "1x Empty Capsule"), ref(ModItems.NITROUS, "1x Nitrous Canister")),
                                ref(ModItems.BLEND_CAPSULE, "1x Blend Capsule: 1 nitrous hit"),
                                List.of("Shapeless game recipe. One canister per occupied slot contributes one hit.",
                                        "Add another supported substance item in a free slot to store a mixed capsule."))
                ),
                List.of(
                        "Dealers carry the canister alongside their existing products. Explore village workshops, shipwreck supply chests, and woodland mansions for occasional finds.",
                        "The finding unlocks after the first canister or a Blend Capsule containing nitrous is consumed.",
                        "Finished canisters come from the dealer catalog, rare exploration loot, or creative inventory. This finding has no production chain."
                )
        ));
        entries.add(entry(
                CursedFindingIds.KETAMINE,
                "Ketamine",
                CursedFindingCategory.DISSOCIATIVES,
                ModItems.KETAMINE,
                List.of(
                        "Ketamine is the cold stair in the archive. It does not bloom outward like the psychedelics. It lifts the body a little away from itself and lets the room keep going without asking permission.",
                        "A small measured piece can feel like softened weight, delayed footsteps, and a hush settling over distance. A deep one does something stranger: it unties gravity, muffles the world, and leaves the player watching their own movement arrive a heartbeat late.",
                        "Its chain is all glass, solvent, and pale preparation. Ethanol is cleaned into Distilled Solvent, folded into Medicinal Reagent, refined into Ketamine Precursor, sealed into a full bottle at the Brew Cauldron, then portioned by the Advanced Chemistry Table into doses meant to be read before they are trusted."
                ),
                List.of(
                        "The first ketamine portion or the full bottle opens this page. Later doses do not create a new trip so much as thicken the same dissociative weather and lengthen the way back down.",
                        "The journal names the road Onset, Float, Detachment, Reentry, and Aftermath. At very heavy strength, another chamber opens between them: the K-hole.",
                        "Light ketamine begins as drag and hush. Sound loses a little edge, distance feels wrong in the corners, and the player's steps start feeling borrowed rather than owned.",
                        "Measured 100mg and 250mg work is where the chapter becomes honest. Body float, depth mismatch, camera lag, and motor disconnect start arriving together, so crossing ordinary ground can feel like negotiating with the floor.",
                        "Heavy ketamine pushes further into dissociation: controls answer late, the world tunnels, the HUD can feel far away, and movement may slide sideways like the body forgot which direction it promised.",
                        "At 400mg and above, the journal starts writing in a darker hand. The K-hole threshold can bring collapse pulses, severe detachment, deep muffling, and the feeling that the world is happening somewhere adjacent to the player instead of around them.",
                        "The full Ketamine Bottle is a whole storm in one swallow: a single-use 1000mg fictional gameplay dose that empties the bottle and gives the glass back. The archive strongly prefers measured portions for any deliberate work.",
                        "Cannabis makes the dissociative weather larger and stranger. Ketamine, in return, can smother the voices of other active substances, especially stimulant noise. Quiet does not mean safe; it only means the other signals are harder to hear."
                ),
                List.of(
                        step(ModItems.ETHANOL, "Ethanol", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.DISTILLED_SOLVENT, "Distilled Solvent",
                                "The clean solvent pass comes first. Ethanol goes to the Chemistry Table and returns sharper, colder, and fit for the rest of the chain."),
                        step(ModItems.ETHANOL, "Ethanol + Distilled Solvent + Glass Bottle", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.MEDICINAL_REAGENT, "Medicinal Reagent",
                                "At the Brew Cauldron, ethanol, solvent, and fresh bottle glass become Medicinal Reagent. The bottle enters the process full and returns later only as emptied hardware."),
                        step(ModItems.MEDICINAL_REAGENT, "Medicinal Reagent", ModItems.CHEMISTRY_TABLE, "Chemistry Table", ModItems.KETAMINE_PRECURSOR, "Ketamine Precursor",
                                "The reagent goes back to the table for refinement. What comes out is labeled Precursor, which is the archive's way of saying not yet, but close enough to worry about."),
                        step(ModItems.KETAMINE_PRECURSOR, "Ketamine Precursor + Distilled Solvent + Glass Bottle", ModItems.BREW_CAULDRON, "Brew Cauldron", ModItems.KETAMINE, "Ketamine Bottle",
                                "A second cauldron pass seals the precursor into a full Ketamine Bottle. This is not a serving. It is a reservoir."),
                        step(ModItems.KETAMINE, "Ketamine Bottle + Glass", ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table", ModItems.KETAMINE_100MG, "Selected Portions",
                                "The Advanced Chemistry Table cuts one full bottle into measured 25mg, 100mg, 250mg, or 500mg fictional portions, depending on what the selector is set to."),
                        step(ModItems.KETAMINE, "Full Bottle", () -> Items.GLASS_BOTTLE, "Drink Once", () -> Items.GLASS_BOTTLE, "Empty Glass Bottle",
                                "If the bottle is used directly, the player takes the full 1000mg fictional load at once and keeps only the empty glass.")),
                List.of(
                        recipe("Brew Cauldron", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.CAULDRON, "1x Cauldron"), ref(Items.CAMPFIRE, "1x Campfire"), ref(Items.SMOOTH_STONE, "3x Smooth Stone"), ref(Items.OAK_FENCE, "2x Any Fence")),
                                ref(ModItems.BREW_CAULDRON, "Brew Cauldron"),
                                List.of("Build it in this shape: _ A _ / F C F / S S S. A=Cauldron, F=Fence, C=Campfire, S=Smooth Stone.")),
                        recipe("Chemistry Table", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.GLASS_BOTTLE, "2x Glass Bottle"), ref(Items.REDSTONE, "1x Redstone"), ref(Items.IRON_INGOT, "2x Iron Ingot"), ref(Items.CRAFTING_TABLE, "1x Crafting Table"), ref(Items.OAK_PLANKS, "2x Any Planks"), ref(Items.FURNACE, "1x Furnace")),
                                ref(ModItems.CHEMISTRY_TABLE, "Chemistry Table"),
                                List.of("Build it in this shape: G R G / I C I / W F W. G=Glass Bottle, R=Redstone, I=Iron, C=Crafting Table, W=Any Planks, F=Furnace.")),
                        recipe("Advanced Chemistry Table", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.DIAMOND, "2x Diamond"), ref(Items.BREWING_STAND, "1x Brewing Stand"), ref(Items.GOLD_INGOT, "2x Gold Ingot"), ref(ModItems.CHEMISTRY_TABLE, "1x Chemistry Table"), ref(Items.OBSIDIAN, "2x Obsidian"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.ADVANCED_CHEMISTRY_TABLE, "Advanced Chemistry Table"),
                                List.of("Build it in this shape: D B D / G C G / O L O. D=Diamond, B=Brewing Stand, G=Gold, C=Chemistry Table, O=Obsidian, L=Glass.")),
                        recipe("Ethanol", CursedFindingRecipeType.CRAFTING_TABLE,
                                List.of(ref(Items.SUGAR, "1x Sugar"), ref(Items.POTION, "1x Water Bottle"), ref(Items.GLASS_BOTTLE, "1x Glass Bottle")),
                                ref(ModItems.ETHANOL, "1x Ethanol"),
                                List.of("Shapeless recipe. This is the bottle that starts the ketamine solvent chain.")),
                        recipe("Distilled Solvent", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.ETHANOL, "1x Ethanol"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.DISTILLED_SOLVENT, "1x Distilled Solvent"),
                                List.of("Refine Ethanol at the Chemistry Table with any valid fuel. The empty bottle returns after the distillation.")),
                        recipe("Medicinal Reagent", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.ETHANOL, "1x Ethanol"), ref(ModItems.DISTILLED_SOLVENT, "1x Distilled Solvent"), ref(Items.GLASS_BOTTLE, "1x Glass Bottle"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.MEDICINAL_REAGENT, "1x Medicinal Reagent"),
                                List.of("Brew Cauldron recipe. Place Ethanol in the source slot, Distilled Solvent in the leaf slot, and the Glass Bottle in the liquid slot. The working bottles return as empty glass through the process.")),
                        recipe("Ketamine Precursor", CursedFindingRecipeType.CHEMISTRY_TABLE,
                                List.of(ref(ModItems.MEDICINAL_REAGENT, "1x Medicinal Reagent"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.KETAMINE_PRECURSOR, "1x Ketamine Precursor"),
                                List.of("Chemistry Table recipe. The reagent is refined one stage further, and the emptied bottle is returned.")),
                        recipe("Ketamine Bottle", CursedFindingRecipeType.BREW_CAULDRON,
                                List.of(ref(ModItems.KETAMINE_PRECURSOR, "1x Ketamine Precursor"), ref(ModItems.DISTILLED_SOLVENT, "1x Distilled Solvent"), ref(Items.GLASS_BOTTLE, "1x Glass Bottle"), ref(Items.COAL, "Fuel")),
                                ref(ModItems.KETAMINE, "1x Ketamine Bottle"),
                                List.of("Brew Cauldron recipe. Place the precursor in the source slot, Distilled Solvent in the leaf slot, and the Glass Bottle in the liquid slot. The solvent bottle and the empty container both return as glass.")),
                        recipe("Ketamine 25mg", CursedFindingRecipeType.DOSAGE_SELECTOR,
                                List.of(ref(ModItems.KETAMINE, "1x Ketamine Bottle"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.KETAMINE_25MG, "40x Ketamine 25mg"),
                                List.of("Advanced Chemistry Table recipe. Select 25mg to cut one full bottle into forty light portions.")),
                        recipe("Ketamine 100mg", CursedFindingRecipeType.DOSAGE_SELECTOR,
                                List.of(ref(ModItems.KETAMINE, "1x Ketamine Bottle"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.KETAMINE_100MG, "10x Ketamine 100mg"),
                                List.of("Advanced Chemistry Table recipe. Select 100mg to cut one full bottle into ten measured portions.")),
                        recipe("Ketamine 250mg", CursedFindingRecipeType.DOSAGE_SELECTOR,
                                List.of(ref(ModItems.KETAMINE, "1x Ketamine Bottle"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.KETAMINE_250MG, "4x Ketamine 250mg"),
                                List.of("Advanced Chemistry Table recipe. Select 250mg to cut one full bottle into four heavy portions.")),
                        recipe("Ketamine 500mg", CursedFindingRecipeType.DOSAGE_SELECTOR,
                                List.of(ref(ModItems.KETAMINE, "1x Ketamine Bottle"), ref(Items.GLASS, "1x Glass")),
                                ref(ModItems.KETAMINE_500MG, "2x Ketamine 500mg"),
                                List.of("Advanced Chemistry Table recipe. Select 500mg to cut one full bottle into two brutal portions that flirt with the chapter's darkest state.")),
                        recipe("Direct Bottle Use", CursedFindingRecipeType.DOSAGE_SELECTOR,
                                List.of(ref(ModItems.KETAMINE, "Bottle")),
                                ref(Items.GLASS_BOTTLE, "Returned Glass Bottle"),
                                List.of("Using the bottle directly consumes it in one pull, applies the full 1000mg fictional gameplay dose, and hands the empty glass back to the player."))),
                List.of(
                        "Ketamine dose labels in this journal are fictional gameplay intensity marks. They are for Vice and Villagers balance and readability, not outside-world instruction.",
                        "The chapter properly reveals the moment any ketamine dose is tried, whether it came from a measured portion or the full bottle.",
                        "The full bottle is intentionally reckless compared to the split portions. If the player wants readable control, the Advanced Chemistry Table exists for a reason.",
                        "Strong ketamine can bury stimulant wakefulness deeply enough that sleep becomes possible again. The journal records that as smothering, not curing.",
                        "Bottle-form ingredients in this chain return empty glass when they are consumed by the Chemistry Table, the Brew Cauldron, or the player.",
                        "Ketamine lives under Dissociatives in the archive. It is less about color than about separation: space from body, delay from intention, and the uneasy miracle of coming back together."
                )
        ));
        entries.add(entry(
                CursedFindingIds.ALCOHOL,
                "Alcohol",
                CursedFindingCategory.ALCOHOL,
                ModItems.BEER_BOTTLE,
                List.of(
                        "Alcohol is the old familiar finding in the archive, which makes it easy to underestimate. It does not arrive like a vision. It arrives like permission, blur, warmth, and the body slowly deciding that precision was overrated.",
                        "In Vice and Villagers, alcohol is measured as standard-drink units rather than as one generic bottle effect. Every serving converts its ounces and ABV into the same internal load, then joins one running session instead of living in separate little timers.",
                        "The chapter is built as a depressant with a long tail. It can feel social and forgiving at the front, but the system keeps separate books for the active drunk state, the hangover that follows, and the dependency-withdrawal track that can wake up later."
                ),
                List.of(
                        "The first drink opens this page. Beer, wine, whiskey, rum, vodka, and consumable Ethanol all feed the same active alcohol load, and additional servings stack into the current session instead of restarting it.",
                        "The Trip Status bar writes the road as Onset, Buzz, Intoxicated, Heavy, Comedown, and Afterglow. Small amounts lean toward euphoria and disinhibition first, then blur, sway, sedation, and nausea grow louder as the drink count climbs.",
                        "Buzz is the warm social window. A modest dose can briefly make the player stronger, a little more buoyant, and oddly protected, because the vanilla bridge hands out short Strength, Absorption, Regeneration, and hunger replenishment while the drunk phases are active.",
                        "Intoxicated is where the body starts billing you back. Motor noise, diplopia, focus blur, vestibular sway, peripheral narrowing, and risk all rise together, so the feeling becomes less celebration and more negotiation with your own feet.",
                        "Heavy is where alcohol stops pretending to be subtle. Sedation thickens, balance goes bad, nausea can start chewing through the screen, and the mod begins checking the blackout lane for truly reckless peaks.",
                        "The session does not end when the pleasant part ends. Four or more peak drinks can queue a hangover, six or more can make that hangover nauseous, and dependency can keep a separate withdrawal clock running after the active drunk state is gone.",
                        "A small fresh drink can temporarily suppress hangover pressure while it is active, but the recovery tracker does not confuse suppression with healing.",
                        "Cocaine can mask the felt alcohol state while it is active, dulling the body-side drunken read without removing the underlying danger. Cannabis can make depressant weather feel larger. Ketamine can bury other signals under dissociative masking.",
                        "At the extreme end, the chapter becomes fatal in two different ways. Very high peak drink counts can trigger blackout death during the session, and late-stage dependency can make withdrawal itself lethal if it is allowed to reach the wall."
                ),
                List.of(step(() -> Items.SUGAR, "Sugar + Water Bottle + Empty Bottle", () -> Items.CRAFTING_TABLE, "Shapeless Crafting", ModItems.ETHANOL, "Ethanol: Two Halves",
                        "The solvent bottle is also consumable. Each right-click drinks one half for ten fictional standard-drink units; the second half returns an empty glass bottle.")),
                List.of(recipe("Ethanol", CursedFindingRecipeType.CRAFTING_TABLE,
                        List.of(ref(Items.SUGAR, "1x Sugar"), ref(Items.POTION, "1x Water Bottle"), ref(Items.GLASS_BOTTLE, "1x Empty Bottle")),
                        ref(ModItems.ETHANOL, "1x Ethanol Bottle"),
                        List.of("Shapeless recipe. The bottle has two consumable halves worth ten fictional standard-drink units each, twenty total. It remains usable as a solvent ingredient."))),
                List.of(
                        "Alcohol items in this build show ABV, serving ounces, and a gameplay standard-drink label on their tooltip. The standard drink math is the archive's bookkeeping language for Vice and Villagers.",
                        "Beer Bottle, Wine 5oz, Wine 9oz, Whiskey, Rum, and Vodka all use the same alcohol system. They differ by serving and ABV, not by separate hidden effect families.",
                        "Ethanol is the deliberate outlier: it is both a chemistry ingredient and a two-use alcohol container. A half-used bottle shows a reservoir bar, and finishing it returns the empty glass bottle.",
                        "A session that peaks at 3 or more drinks starts contributing to dependency phase. The long-term reset takes two Minecraft days sober in the early stages and three days in the late stages. Sleeping pushes that recovery clock forward.",
                        "Hangovers start at 4 or more peak drinks, and severe ones can deal damage while they tick down. If the peak was high enough, the hangover can also keep its own nausea flag alive for a while.",
                        "Only DMT therapy currently reduces the alcohol dependency track. Psilocybin and LSD do not clear alcoholism in the current code path.",
                        "Alcohol labels in this journal are fictional gameplay notation only."
                )
        ));
        entries.add(entry(
                CursedFindingIds.DEPENDENCE_TOLERANCE,
                "Dependence & Tolerance",
                CursedFindingCategory.DEPENDENCE_RECOVERY,
                () -> Items.CLOCK,
                List.of(
                        "Not every consequence in Vice and Villagers lives inside the active trip. Beneath the bright part, the archive keeps slower ledgers for tolerance, afterglow, comedown, craving, and withdrawal across every substance family.",
                        "Most of those ledgers are broad family weather rather than one-item punishments. Psychedelics, cannabinoids, stimulants, opioids, dissociatives, depressants, empathogens, and deliriants each leave their own residue, then that residue changes how the next session answers.",
                        "Two branches go further than the shared family maps. Heroin and alcohol each run dedicated long-term trackers with their own phases, reset clocks, suppression thresholds, and death paths, because their cold endings are harsher than the archive's softer generic comedown math."
                ),
                List.of(
                        "Tolerance builds whenever a family is actively present. The shared system raises a family's tolerance from raw exposure, then slowly shaves it back down through recovery. That means repeated use makes later passes answer less strongly unless enough sober time or recovery support has passed.",
                        "The response loss is not isolated. Cross-tolerance is real in this build. Psychedelics lean against dissociatives and deliriants, depressants press on opioids and dissociatives, stimulants lean into empathogens, and several of the families borrow dullness from one another instead of staying politely separate.",
                        "Tolerance is not the only tail. Afterglow can leave extra euphoria, color bloom, and depth breathing behind. Comedown adds body load, sedation, motor noise, and anxiety. If tolerance pressure remains while active load falls low enough, withdrawal begins climbing. Craving then feeds on comedown, withdrawal, and tolerance together.",
                        "Those long-tail states also feed the danger side of the mod. Craving and withdrawal add risk, mixed substances can create extra mixed-risk and strain, and threat-heavy surroundings make the body less forgiving while recovery-heavy surroundings let it bleed off faster.",
                        "Heroin has the hardest dedicated opioid ledger in the book. It tracks separate tolerance and dependence phases, shrinks the warm effect as tolerance rises, queues a crash and nutrition penalty after stronger sessions, and starts withdrawal once active opioid load falls below the moving suppression threshold.",
                        "Alcohol has its own separate recovery machine. Four or more peak drinks can queue hangover, three-or-more drink sessions can start building dependency, a fresh drink can temporarily suppress the cold edge without curing it, and late-stage alcohol withdrawal can become lethal.",
                        "Sleep is not decorative here. Ending the active trip by sleeping clears the running sessions and pushes heroin and alcohol recovery clocks forward by half a Minecraft day. It can shorten crashes, hangovers, and sober reset timers even when it does not make them vanish outright.",
                        "Only DMT therapy currently cuts the alcohol dependency track. Psilocybin and LSD do not clear alcoholism in the current code path."
                ),
                List.of(step(() -> Items.CLOCK, "Sober Time", () -> Items.RED_BED, "Sleep / Recovery", () -> Items.BOOK, "System Ledger",
                        "This chapter documents hidden state rather than a craft chain. Tolerance, craving, withdrawal, and reset timers are tracked in the background while the player lives with what previous sessions left behind.")),
                List.of(recipe("System Page", CursedFindingRecipeType.CRAFTING_TABLE,
                        List.of(ref(Items.BOOK, "Journal page")),
                        ref(Items.CLOCK, "No recipe"),
                        List.of("This chapter is systemic, not craftable. There are no recipe cards here; the tab only marks that dependence and tolerance belong to the simulation layer instead of the workbench."))),
                List.of(
                        "The shared family maps decay through recovery support and the global recovery-rate config. Safer rooms, food, sleep, shelter, social support, and low threat all make the archive forgive faster.",
                        "The shared family maps also respect the global risk-severity config. Higher risk severity makes withdrawal, craving, mixed risk, and strain grow more aggressively.",
                        "Heroin's dedicated reset takes three Minecraft days sober in the earlier stages and five days in late dependence. Alcohol's dedicated reset takes two days early and three days late. Sleeping advances both clocks by half a day.",
                        "Heroin crash usually begins after stronger sessions instead of tiny ones, and cannabis can soften the opioid nutrition penalty while it is active. Alcohol hangover begins at lower thresholds than its true dependency track.",
                        "The archive uses fictional gameplay notation. Tolerance phases, dependence phases, suppression thresholds, and reset windows are Vice and Villagers systems, not outside-world guidance."
                )
        ));
        entries.add(entry(
                CursedFindingIds.ECONOMY_DEALING,
                "Economy / Dealers / Customers / Brokers",
                CursedFindingCategory.ECONOMY_DEALING,
                () -> Items.EMERALD,
                List.of(
                        "Vice and Villagers keeps a second simulation under the altered states: villages can quietly grow their own dealer, customer, and broker network around the player.",
                        "Every 200 ticks, the economy scan looks around living players, gathers nearby adult villagers into linked village clusters, and decides how many dealers, market brokers, and customers that settlement should support.",
                        "The result is not one shop but a loop. Dealers sell finished formats for emeralds, customers create local street demand, and brokers handle the larger market where demand, contracts, raw materials, and reputation all start pushing back."
                ),
                List.of(
                        "Dealers are the quick-access side of the system. Interact with a nearby dealer villager and the catalog opens within eight blocks, selling finished cannabis, psychedelic, stimulant, opioid, dissociative, salvia, and nitrous formats for straight emerald prices. Bartenders handle the served drinks.",
                        "The psychedelic catalog includes 4-HO-MET and Mescaline bottles, plus AL-LAD tabs, at 25mg, 100mg, 250mg, and 500mg. The renamed 4-HO-MET and AL-LAD formats retain their existing effect, price, and customer demand tiers.",
                        "Customers are smaller but smarter buyers. Each customer rolls four offers per Minecraft day, and those offers are weighted by your business level, the product's unlock tier, and that villager's remembered habit history with exact products and whole categories.",
                        "Dropped samples matter. If a valid dealer product item lands close enough to a customer, that villager can consume one unit, gain exposure, and start biasing future requests toward that substance family.",
                        "Habit pressure changes the language of demand. Light interest shows up as Quick Sale, Private Order, or Repeat Buyer. Stronger histories escalate into Craving, Habit Order, Strong Craving, Compulsive Craving, and even Compulsive Bulk Order.",
                        "Market brokers run the structured trade layer. Their product catalog covers all sixteen substance families, including Dealer products, DMT Powder, MDMA capsules, bulk crystals and powders, AL-LAD concentrate, ethanol, and loaded smoking devices. They also sell materials and vanilla goods, track daily demand, and keep four personal contracts for each player.",
                        "Bring full, unused stock for sales and contracts. Partly used joints, pens, edibles, ethanol, crystals, and concentrate stay in your inventory. Equivalent older item IDs count toward the current product without needing to be replaced.",
                        "Demand is alive instead of fixed. Each sellable product keeps its own multiplier between 0.80x and 1.50x, prices are recalculated from base value, demand, and business level, and heavier sales push that product's demand back down.",
                        "Reputation is the ladder under the whole system. Sales and completed contracts add reputation, every 250 reputation becomes another business level, higher levels make broker prices slightly better, and deeper levels widen the range of customer asks and contract templates."
                ),
                List.of(
                        step(() -> Items.BELL, "Village cluster", () -> Items.CLOCK, "Automatic economy scan", () -> Items.VILLAGER_SPAWN_EGG, "Dealer / Broker / Customer roles",
                                "Nearby adult villagers are grouped into linked village clusters. Small settlements aim for one dealer and one broker; larger ones can grow two of each, with a slice of the remaining adults becoming customers."),
                        step(() -> Items.EMERALD, "Emerald bankroll", () -> Items.NAME_TAG, "Dealer catalog", ModItems.CANNABIS_JOINT, "Finished formats",
                                "Dealers sell registered product formats for fixed emerald prices. DMT crystals still require grinding before use. The current dealer path is a cash catalog rather than a crafting gate."),
                        step(ModItems.CANNABIS_JOINT, "Product stock or dropped sample", () -> Items.PAPER, "Customer demand", () -> Items.BOOK, "Daily offers + habit memory",
                                "Customers remember samples and purchases. Their daily offers, quantities, request types, and product reach all react to stored habit data."),
                        step(() -> Items.CHEST, "Full, unused product stock", () -> Items.EMERALD, "Market Broker", () -> Items.WRITABLE_BOOK, "Demand, reputation, contracts",
                                "Sell eligible full stock or complete a listed contract. Partly used items remain yours. Emeralds are queued until the market closes; sales also affect reputation and demand.")
                ),
                List.of(recipe("System Page", CursedFindingRecipeType.CRAFTING_TABLE,
                        List.of(ref(Items.BOOK, "Journal page")),
                        ref(Items.EMERALD, "No recipe"),
                        List.of("This chapter documents the trading simulation, not a craft chain. Dealers, customers, and brokers are generated and managed by the village economy systems rather than by a recipe."))),
                List.of(
                        "The role tags are flexible. Dealers answer to names like Dealer, Plug, or Street Seller; brokers match Broker, Fixer, Supplier, or Market; customers answer to Customer, Buyer, or Regular. Untagged villagers can also be auto-marked during the scan.",
                        "Nitwits lean toward broker duty in the current rules, while dealers are kept away from broker-like reserved names so the roles do not overlap by accident.",
                        "Customer offers are not random noise. Habit level can lift unlock reach, raise quantities toward large stack sales, and add extra premium pressure without ever quite overtaking the dealer's own buy price.",
                        "Broker trades and customer sales queue emerald payouts through a ledger and flush them when the relevant screen opens or closes, so some market money is collected in batches instead of appearing instantly.",
                        "Selling a loaded Bong or Ancient Pipe transfers the whole loaded device. An empty vessel returns when you use a prepared hit, not when you sell it. Empty tools and custom blend capsules are outside the broker's fixed-product catalog.",
                        "The newly added reservoir crystals, AL-LAD concentrate, loaded devices, and ethanol can be sold directly but are not contract targets. Contracts keep their listed product and quantity; only full, unused matching stock counts.",
                        "Your four contracts never expire. Closing and reopening the market, logging out, saving, or passing a Minecraft day leaves them intact. Completing one replaces only that order in its slot; the other three keep their IDs, requirements, and rewards. Daily demand changes do not reprice posted contracts.",
                        "Contract templates include Small Order, Steady Buyer, Bulk Order, Premium Order, and Warehouse Order. Higher business levels make heavier templates available for new orders. An initial or incomplete older board fills only its missing slots.",
                        "The market buy side is broad on purpose. Beyond mod materials, the broker registry also exposes a huge vanilla catalog grouped into categories like Wood, Stone, Gear, Food, Rare / OP, Spawn Eggs, and Misc.",
                        "Dealer products still carry unlock tiers in the registry, but in this build those tiers matter most for customer demand weighting rather than for hard dealer-side locks.",
                        "This economy chapter describes fictional gameplay systems inside Vice and Villagers, not outside-world advice."
                )
        ));
        entries.add(entry(
                CursedFindingIds.SMOKING_DEVICES,
                "Smoking Devices",
                CursedFindingCategory.UTILITIES,
                ModItems.ANCIENT_PIPE,
                List.of(
                        "The Ancient Pipe and Bong are reusable vessels for the same prepared findings. Each holds one serving: a Dried Cannabis Bud, one DMT Powder, or one Salvia Extract of any of the four strengths.",
                        "The Ancient Pipe uses iron, any wooden planks, and Acacia Bark. The Bong keeps its glass-and-water recipe. This utility page is open from the start so their construction does not depend on trying a substance first."
                ),
                List.of(
                        "Craft exactly one empty device beside exactly one supported ingredient in a crafting table or the 2x2 inventory grid. The result holds one hit. A loaded bowl cannot be topped up or mixed with another substance.",
                        "Right-click the loaded item to take its single serving. It returns the same type of empty device, including in Creative mode. An empty device gives no dose.",
                        "One dried bud supplies 200mg THC, one DMT Powder supplies one DMT unit, and Salvia keeps its selected 1x, 10x, 25x, or 50x extract strength. Switching vessels does not multiply the serving."
                ),
                List.of(
                        ancientPipeProductionStep(),
                        step(() -> Items.GLASS, "4x Glass + Water Bucket", () -> Items.CRAFTING_TABLE, "Shaped crafting", ModItems.BONG, "Empty Bong",
                                "Build the Bong in this shape: _ G _ / _ G _ / G W G. G=Glass block, W=Water Bucket, _=Empty. The bucket returns empty."),
                        step(ModItems.DMT_POWDER, "One Prepared Serving + Empty Device", () -> Items.CRAFTING_TABLE, "Shapeless crafting", ModItems.ANCIENT_PIPE_DMT, "Loaded Device: One Hit",
                                "Use DMT Powder, a Dried Cannabis Bud, or Salvia Extract. DMT crystals, fresh buds, and fresh or merely dried Salvia leaves are not device loads.")
                ),
                smokingDeviceRecipes(),
                List.of(
                        "See the DMT finding for the acacia chain and mortar yields: a 1x crystal makes one powder, a 5x crystal makes five powders. Crystals cannot be consumed or loaded as-is; one powder can also be consumed directly for one fictional unit.",
                        "See Cannabis / THC for drying buds and Salvia for the four extract recipes. The same ingredients work in either device.",
                        "Only an empty vessel can be loaded. Use one ingredient item, not two servings or a mixed bowl. A full inventory does not turn a loaded item into an extra serving or destroy the returning vessel.",
                        "Taking a prepared hit unlocks its existing substance finding. This utility page does not change any substance dose, trip progression, or chapter history."
                )
        ));
        entries.sort(Comparator.comparing(entry -> entry.displayName().toLowerCase(java.util.Locale.ROOT)));
        return List.copyOf(entries);
    }

    private static CursedFindingEntry entry(
            String id,
            String displayName,
            CursedFindingCategory category,
            Supplier<? extends ItemLike> icon,
            List<String> overview,
            List<String> effects,
            List<CursedFindingProductionStep> productionChain,
            List<CursedFindingRecipe> recipes,
            List<String> notes
    ) {
        return new CursedFindingEntry(
                id,
                displayName,
                category,
                icon,
                overview,
                effects,
                productionChain,
                recipes,
                notes,
                "Ink refuses the shape. Try the thing, and the finding may let itself be named."
        );
    }

    private static CursedFindingProductionStep ancientPipeProductionStep() {
        return step(() -> Items.IRON_INGOT, "Iron Ingot + 2 Any Planks + Acacia Bark", () -> Items.CRAFTING_TABLE,
                "Shaped crafting", ModItems.ANCIENT_PIPE, "Empty Ancient Pipe",
                "Build it in this shape: _ _ _ / I _ _ / W A W. I=Iron Ingot, W=any plank from minecraft:planks, A=Acacia Bark, _=Empty. The three-column recipe needs a crafting table.");
    }

    private static CursedFindingRecipe ancientPipeRecipe() {
        return recipe("Ancient Pipe", CursedFindingRecipeType.CRAFTING_TABLE,
                List.of(ref(Items.IRON_INGOT, "1x Iron Ingot"), ref(Items.OAK_PLANKS, "2x Any Planks (minecraft:planks)"), ref(ModItems.ACACIA_BARK, "1x Acacia Bark")),
                ref(ModItems.ANCIENT_PIPE, "1x Empty Ancient Pipe"),
                List.of("Build it in this shape: _ _ _ / I _ _ / W A W. I=Iron Ingot, W=any wooden plank, A=Acacia Bark, _=Empty. Requires a crafting table; the plank ingredient accepts the minecraft:planks tag."));
    }

    private static CursedFindingRecipe bongRecipe() {
        return recipe("Bong", CursedFindingRecipeType.CRAFTING_TABLE,
                List.of(ref(Items.GLASS, "4x Glass Blocks"), ref(Items.WATER_BUCKET, "1x Water Bucket")),
                ref(ModItems.BONG, "1x Empty Bong"),
                List.of("Build it in this shape: _ G _ / _ G _ / G W G. G=Glass block, W=Water Bucket, _=Empty. The Water Bucket leaves an empty Bucket behind."));
    }

    private static CursedFindingRecipe loadDeviceRecipe(String device,
            Supplier<? extends ItemLike> empty, Supplier<? extends ItemLike> ingredient, String ingredientLabel,
            Supplier<? extends ItemLike> loaded, String substanceLabel, String servingLabel) {
        return recipe("Load " + device + ": " + substanceLabel, CursedFindingRecipeType.CRAFTING_TABLE,
                List.of(ref(empty, "1x Empty " + device), ref(ingredient, "1x " + ingredientLabel)),
                ref(loaded, "1x Loaded " + device + " (" + servingLabel + ")"),
                List.of("Shapeless recipe in the crafting table or 2x2 inventory grid. Each loaded item carries " + servingLabel
                        + " as one hit. Right-click to take it and recover the same empty " + device + ", including in Creative mode. A loaded bowl cannot be topped up or mixed."));
    }

    private static List<CursedFindingRecipe> smokingDeviceRecipes() {
        return List.of(
                ancientPipeRecipe(), bongRecipe(),
                loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.DMT_POWDER, "DMT Powder", ModItems.ANCIENT_PIPE_DMT, "DMT", "1 DMT unit"),
                loadDeviceRecipe("Bong", ModItems.BONG, ModItems.DMT_POWDER, "DMT Powder", ModItems.BONG_DMT, "DMT", "1 DMT unit"),
                loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.DRIED_CANNABIS_BUD, "Dried Cannabis Bud", ModItems.ANCIENT_PIPE_CANNABIS, "Cannabis", "200mg THC"),
                loadDeviceRecipe("Bong", ModItems.BONG, ModItems.DRIED_CANNABIS_BUD, "Dried Cannabis Bud", ModItems.BONG_CANNABIS, "Cannabis", "200mg THC"),
                loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.SALVIA_1X, "Salvia Extract 1x", ModItems.ANCIENT_PIPE_SALVIA_1X, "Salvia 1x", "Salvia 1x"),
                loadDeviceRecipe("Bong", ModItems.BONG, ModItems.SALVIA_1X, "Salvia Extract 1x", ModItems.BONG_SALVIA_1X, "Salvia 1x", "Salvia 1x"),
                loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.SALVIA_10X, "Salvia Extract 10x", ModItems.ANCIENT_PIPE_SALVIA_10X, "Salvia 10x", "Salvia 10x"),
                loadDeviceRecipe("Bong", ModItems.BONG, ModItems.SALVIA_10X, "Salvia Extract 10x", ModItems.BONG_SALVIA_10X, "Salvia 10x", "Salvia 10x"),
                loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.SALVIA_25X, "Salvia Extract 25x", ModItems.ANCIENT_PIPE_SALVIA_25X, "Salvia 25x", "Salvia 25x"),
                loadDeviceRecipe("Bong", ModItems.BONG, ModItems.SALVIA_25X, "Salvia Extract 25x", ModItems.BONG_SALVIA_25X, "Salvia 25x", "Salvia 25x"),
                loadDeviceRecipe("Ancient Pipe", ModItems.ANCIENT_PIPE, ModItems.SALVIA_50X, "Salvia Extract 50x", ModItems.ANCIENT_PIPE_SALVIA_50X, "Salvia 50x", "Salvia 50x"),
                loadDeviceRecipe("Bong", ModItems.BONG, ModItems.SALVIA_50X, "Salvia Extract 50x", ModItems.BONG_SALVIA_50X, "Salvia 50x", "Salvia 50x")
        );
    }

    private static CursedFindingProductionStep step(
            Supplier<? extends ItemLike> input,
            String inputLabel,
            Supplier<? extends ItemLike> station,
            String stationLabel,
            Supplier<? extends ItemLike> output,
            String outputLabel
    ) {
        return step(input, inputLabel, station, stationLabel, output, outputLabel,
                "Placeholder production step. Exact inputs, station rules, and yields will be populated later.");
    }

    private static CursedFindingProductionStep step(
            Supplier<? extends ItemLike> input,
            String inputLabel,
            Supplier<? extends ItemLike> station,
            String stationLabel,
            Supplier<? extends ItemLike> output,
            String outputLabel,
            String note
    ) {
        return new CursedFindingProductionStep(ref(input, inputLabel), ref(station, stationLabel), ref(output, outputLabel), note);
    }

    private static CursedFindingRecipe recipe(
            String title,
            CursedFindingRecipeType type,
            List<CursedFindingItemRef> inputs,
            CursedFindingItemRef output
    ) {
        return recipe(title, type, inputs, output,
                List.of("Placeholder recipe card. Exact components, station rules, notes, and outputs will be written later."));
    }

    private static CursedFindingRecipe recipe(
            String title,
            CursedFindingRecipeType type,
            List<CursedFindingItemRef> inputs,
            CursedFindingItemRef output,
            List<String> notes
    ) {
        return new CursedFindingRecipe(title, type, inputs, output, notes);
    }

    private static CursedFindingItemRef ref(Supplier<? extends ItemLike> item, String label) {
        return new CursedFindingItemRef(item, label);
    }

    private static CursedFindingItemRef ref(ItemLike item, String label) {
        return new CursedFindingItemRef(() -> item, label);
    }
}
