package net.pixeldreamstudios.journal;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.data.JournalComponent;
import net.pixeldreamstudios.journal.data.JournalComponents;
import net.pixeldreamstudios.journal.data.MobDescriptionLoader;
import net.pixeldreamstudios.journal.events.MobStatEventHandler;
import net.pixeldreamstudios.journal.item.JournalItem;
import net.pixeldreamstudios.journal.network.*;
import net.pixeldreamstudios.journal.util.MobLootUtil;

import java.util.Set;

public class Journal implements ModInitializer {
	public static final String MOD_ID = "journal";
	public static final Item JOURNAL_ITEM = Registry.register(
			Registries.ITEM,
			Identifier.of(MOD_ID, "journal"),
			new JournalItem(new Item.Settings().maxCount(1))
	);
	public static final ItemGroup JOURNAL_TAB = FabricItemGroup.builder()
			.icon(() -> new ItemStack(JOURNAL_ITEM))
			.displayName(Text.translatable("itemGroup.journal.tab"))
			.entries((context, entries) -> {
				entries.add(JOURNAL_ITEM);
			})
			.build();
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
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			var journal = JournalComponents.JOURNAL.get(player);

			if (!journal.hasReceivedJournal()) {
				player.giveItemStack(new ItemStack(JOURNAL_ITEM));
				journal.setReceivedJournal(true);

				}
		});
		Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "tab"), JOURNAL_TAB);
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.add(JOURNAL_ITEM);
		});
	}
}
