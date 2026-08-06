package net.pixeldreamstudios.journal;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.pixeldreamstudios.journal.compat.JournalAccess;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.events.JournalSounds;
import net.pixeldreamstudios.journal.item.JournalItems;
import net.pixeldreamstudios.journal.network.ServerNetworkHandler;
import net.pixeldreamstudios.journal.util.JournalCommands;

public final class Journal {
    public static final String MOD_ID = "journal";

    public static void init() {
        JournalConfig.load();
        JournalItems.init();
        JournalSounds.register();
        JournalAccess.registerAccessoryItem();

        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                JournalCommands.register(dispatcher, registryAccess));

        ServerNetworkHandler.init();
    }
}
