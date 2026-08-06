package net.pixeldreamstudios.journal.events;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.pixeldreamstudios.journal.Journal;

public class JournalSounds {
    public static final ResourceLocation WRITING_ID =
            new ResourceLocation(Journal.MOD_ID, "writing");

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Journal.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> WRITING =
            SOUNDS.register("writing", () -> SoundEvent.createVariableRangeEvent(WRITING_ID));

    public static void register() {
        SOUNDS.register();
    }
}
