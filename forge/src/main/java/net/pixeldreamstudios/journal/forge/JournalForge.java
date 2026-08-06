package net.pixeldreamstudios.journal.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.pixeldreamstudios.journal.Journal;
import net.pixeldreamstudios.journal.client.JournalClient;
import net.pixeldreamstudios.journal.network.forge.NetworkManagerImpl;

@Mod(Journal.MOD_ID)
public final class JournalForge {

    public JournalForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Journal.MOD_ID, modBus);

        NetworkManagerImpl.registerCommonPayloads();
        Journal.init();

        EnvExecutor.runInEnv(Env.CLIENT, () -> JournalClient::init);
    }
}
