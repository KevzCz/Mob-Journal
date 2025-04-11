package net.pixeldreamstudios.journal.events;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class JournalSounds {
    public static final Identifier WRITING_ID = Identifier.of("journal", "writing");
    public static final SoundEvent WRITING = SoundEvent.of(WRITING_ID);

    public static void register() {
        Registry.register(Registries.SOUND_EVENT, WRITING_ID, WRITING);
    }
}
