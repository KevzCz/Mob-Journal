package net.pixeldreamstudios.journal.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.client.JournalClient;

@Mod(value = Journal.MOD_ID, dist = Dist.CLIENT)
public final class JournalNeoForgeClient {

    public JournalNeoForgeClient(IEventBus modBus, ModContainer modContainer) {
        JournalClient.init();
    }
}
