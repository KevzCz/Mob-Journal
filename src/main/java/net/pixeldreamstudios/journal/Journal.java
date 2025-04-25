package net.pixeldreamstudios.journal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.pixeldreamstudios.journal.item.JournalItems;
import net.pixeldreamstudios.journal.network.JournalEvents;
import net.pixeldreamstudios.journal.network.JournalNetwork;
import net.pixeldreamstudios.journal.util.JournalCommands;

public class Journal implements ModInitializer {
	public static final String MOD_ID = "journal";
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			JournalCommands.register(dispatcher, registryAccess);
		});
		JournalItems.init();
		JournalNetwork.init();
		JournalEvents.init();
	}
}