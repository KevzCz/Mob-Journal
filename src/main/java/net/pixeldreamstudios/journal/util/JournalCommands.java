package net.pixeldreamstudios.journal.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.data.JournalComponents;
import net.pixeldreamstudios.journal.network.SyncJournalPayload;
import net.minecraft.registry.Registries;
import net.minecraft.entity.LivingEntity;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.Collections;
import java.util.List;

public class JournalCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        dispatcher.register(CommandManager.literal("journal")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("unlock_all")
                        .executes(JournalCommands::unlockAll))
                .then(CommandManager.literal("clear_all")
                        .executes(JournalCommands::clearAll))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("mob", IdentifierArgumentType.identifier())
                                .suggests(SUGGEST_DISCOVERED_MOBS)
                                .executes(JournalCommands::removeMob)))
        );
    }

    private static int unlockAll(CommandContext<ServerCommandSource> context) {
        var player = context.getSource().getPlayer();
        var journal = JournalComponents.JOURNAL.get(player);
        int count = 0;

        for (var entry : Registries.ENTITY_TYPE.getIds()) {
            var type = Registries.ENTITY_TYPE.get(entry);
            if (type == null || !type.isSummonable()) continue;

            var entity = type.create(player.getWorld());
            if (!(entity instanceof LivingEntity)) continue;

            if (JournalConfig.isBlacklisted(entry)) continue;

            if (journal.unlockMob(entry)) {
                count++;
            }
        }

        int finalCount = count;
        context.getSource().sendFeedback(() ->
                Text.literal("✅ Unlocked " + finalCount + " mobs in the journal."), false);
        return count;
    }

    private static int clearAll(CommandContext<ServerCommandSource> context) {
        var player = context.getSource().getPlayer();
        var journal = JournalComponents.JOURNAL.get(player);
        journal.clearDiscovered();

        // now send an empty map instead of an empty list
        ServerPlayNetworking.send(player, new SyncJournalPayload(Collections.emptyMap()));
        context.getSource().sendFeedback(() ->
                Text.literal("Cleared all discovered mobs in the journal."), false);
        return 1;
    }

    private static int removeMob(CommandContext<ServerCommandSource> context) {
        var player = context.getSource().getPlayer();
        var journal = JournalComponents.JOURNAL.get(player);
        var id = IdentifierArgumentType.getIdentifier(context, "mob");

        if (journal.removeMob(id)) {
            context.getSource().sendFeedback(() ->
                    Text.literal("❌ Removed mob from journal: " + id), false);

            // now send the full id→timestamp map
            ServerPlayNetworking.send(player,
                    new SyncJournalPayload(journal.getDiscovered()));

            return 1;
        } else {
            context.getSource().sendFeedback(() ->
                    Text.literal("⚠️ Mob not found in journal: " + id), false);
            return 0;
        }
    }

    // 🧠 Suggestions for `/journal remove` autocomplete
    private static final SuggestionProvider<ServerCommandSource> SUGGEST_DISCOVERED_MOBS = (context, builder) -> {
        ServerPlayerEntity player = context.getSource().getPlayer();
        var journal = JournalComponents.JOURNAL.get(player);

        // switch from the old Set to your Map's keySet()
        return net.minecraft.command.CommandSource.suggestIdentifiers(
                journal.getDiscovered().keySet().stream(), builder
        );
    };
}
