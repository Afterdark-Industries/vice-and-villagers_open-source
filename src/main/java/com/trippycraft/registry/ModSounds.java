package com.trippycraft.registry;

import com.trippycraft.TrippyCraft;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Streaming tavern tracks, kept separate from collectible music discs. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, TrippyCraft.MOD_ID);

    public static final Supplier<SoundEvent> WE_BECOME_THE_WAVE = track("we_become_the_wave");
    public static final Supplier<SoundEvent> WAREHOUSE_DROP = track("warehouse_drop");
    public static final Supplier<SoundEvent> WAREHOUSE_DROP_1 = track("warehouse_drop_1");
    public static final Supplier<SoundEvent> CHOPPED_GROOVE = track("chopped_groove");
    public static final Supplier<SoundEvent> BOUNCE_HARDER = track("bounce_harder");
    public static final Supplier<SoundEvent> PEAK_HOUR_DROP = track("peak_hour_drop");
    public static final Supplier<SoundEvent> NOCTURNAL_MOTION = track("nocturnal_motion");
    public static final Supplier<SoundEvent> SUMMER_NIGHT_EUPHORIA = track("summer_night_euphoria");
    public static final Supplier<SoundEvent> WAREHOUSE_PRESSURE = track("warehouse_pressure");
    public static final Supplier<SoundEvent> MAINSTAGE_DREAMS = track("mainstage_dreams");
    public static final Supplier<SoundEvent> FINAL_HOUR = track("final_hour");
    public static final Supplier<SoundEvent> EUPHORIC_REPRISE = track("euphoric_reprise");
    public static final Supplier<SoundEvent> LOSE_MYSELF_COME_ALIVE = track("lose_myself_come_alive");
    public static final Supplier<SoundEvent> WHILE_THE_HOUSE_COMES_DOWN = track("while_the_house_comes_down");
    public static final Supplier<SoundEvent> NO_BRAKES_NO_SLEEP = track("no_brakes_no_sleep");
    public static final Supplier<SoundEvent> DONT_LEAVE_NOT_YET = track("dont_leave_not_yet");
    public static final Supplier<SoundEvent> LOSE_CONTROL = track("lose_control");
    public static final Supplier<SoundEvent> STAY_WITH_ME_TONIGHT = track("stay_with_me_tonight");
    public static final Supplier<SoundEvent> SOME_MEMORIES_LEAVE = track("some_memories_leave");

    /** Stable authored order used by every tavern playlist. */
    public static final List<Supplier<SoundEvent>> TAVERN_TRACKS = List.of(
            WE_BECOME_THE_WAVE,
            WAREHOUSE_DROP,
            WAREHOUSE_DROP_1,
            CHOPPED_GROOVE,
            BOUNCE_HARDER,
            PEAK_HOUR_DROP,
            NOCTURNAL_MOTION,
            SUMMER_NIGHT_EUPHORIA,
            WAREHOUSE_PRESSURE,
            MAINSTAGE_DREAMS,
            FINAL_HOUR,
            EUPHORIC_REPRISE,
            LOSE_MYSELF_COME_ALIVE,
            WHILE_THE_HOUSE_COMES_DOWN,
            NO_BRAKES_NO_SLEEP,
            DONT_LEAVE_NOT_YET,
            LOSE_CONTROL,
            STAY_WITH_ME_TONIGHT,
            SOME_MEMORIES_LEAVE
    );

    private ModSounds() {
    }

    private static Supplier<SoundEvent> track(String name) {
        return SOUND_EVENTS.register("tavern." + name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(TrippyCraft.MOD_ID, "tavern." + name)
        ));
    }
}
