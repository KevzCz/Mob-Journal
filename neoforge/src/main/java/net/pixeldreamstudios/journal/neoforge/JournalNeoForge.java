package net.pixeldreamstudios.journal.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.network.neoforge.NetworkManagerImpl;

@Mod(Journal.MOD_ID)
public final class JournalNeoForge {

    public JournalNeoForge(IEventBus modBus, ModContainer modContainer) {
        JournalAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(this::registerPayloads);

        Journal.init();
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        NetworkManagerImpl.registerPayloads(event);
    }
}
