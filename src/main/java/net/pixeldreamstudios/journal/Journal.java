package net.pixeldreamstudios.journal;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.data.JournalComponent;
import net.pixeldreamstudios.journal.data.JournalComponents;
import net.pixeldreamstudios.journal.data.MobDescriptionLoader;
import net.pixeldreamstudios.journal.events.MobStatEventHandler;
import net.pixeldreamstudios.journal.network.*;
import net.pixeldreamstudios.journal.util.MobLootUtil;

import java.util.Set;

public class Journal implements ModInitializer {
	public static final String MOD_ID = "journal";

	@Override
	public void onInitialize() {
		// 📦 Register C2S payloads
		PayloadTypeRegistry.playC2S().register(UnlockMobPayload.ID, UnlockMobPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(OpenJournalPayload.ID, OpenJournalPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(RequestMobDropsPayload.ID, RequestMobDropsPayload.CODEC);

		// 📊 Register mob stat handler
		MobStatEventHandler.register();


		// 🔓 Unlock mob packet
		ServerPlayNetworking.registerGlobalReceiver(UnlockMobPayload.ID, (payload, context) -> {
			context.player().server.execute(() -> {
				var player = context.player();
				var id = payload.mobId();
				var journal = JournalComponents.JOURNAL.get(player);

				if (journal.unlockMob(id)) {
					ServerPlayNetworking.send(player, new DiscoveredMobToastPayload(id));
					ServerPlayNetworking.send(player, new SyncJournalPayload(journal.getDiscovered().stream().toList()));
				}
			});
		});

		// 📖 Open journal request
		ServerPlayNetworking.registerGlobalReceiver(OpenJournalPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			var component = JournalComponents.JOURNAL.get(player);
			var discovered = component.getDiscovered();
			ServerPlayNetworking.send(player, new SyncJournalPayload(discovered.stream().toList()));
			var mobStats = JournalComponents.MOB_STATS.get(player);
			ServerPlayNetworking.send(player, new SyncMobStatsPayload(mobStats.getAllStats()));
		});

		// 📦 Mob loot preview request (show ALL items from loot table)
		ServerPlayNetworking.registerGlobalReceiver(RequestMobDropsPayload.ID, (payload, context) -> {
			var player = context.player();
			var world = player.getServerWorld();
			var type = net.minecraft.registry.Registries.ENTITY_TYPE.get(payload.mobId());

			if (type != null && type.isSummonable()) {
				var entity = type.create(world);
				if (entity instanceof net.minecraft.entity.LivingEntity mob) {
					var drops = MobLootUtil.getAllPossibleDrops(mob, world); // 🔁 Using new method!
					ServerPlayNetworking.send(player, new SyncMobDropsPayload(drops));
				}
			}
		});
	}
}
