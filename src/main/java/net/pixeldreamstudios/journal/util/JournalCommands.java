package net.pixeldreamstudios.journal.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.data.JournalComponents;
import net.pixeldreamstudios.journal.network.OpenBlacklistScreenPayload;
import net.pixeldreamstudios.journal.network.SyncJournalPayload;

import java.util.Collections;

public class JournalCommands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        dispatcher.register(CommandManager.literal("journal")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("unlock_all")
                        .executes(JournalCommands::unlockAll))
                .then(CommandManager.literal("clear_all")
                        .executes(JournalCommands:: clearAll))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("mob", IdentifierArgumentType.identifier())
                                .suggests(SUGGEST_DISCOVERED_MOBS)
                                .executes(JournalCommands::removeMob)))
                .then(CommandManager.literal("blacklist")
                        .executes(JournalCommands:: openBlacklist))
        );
    }

    private static int unlockAll(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        var journal = JournalComponents.JOURNAL.get(player);

        int unlocked = 0;

        for (var type :  Registries.ENTITY_TYPE) {
            if (! type.isSummonable()) continue;

            var entity = type.create(player.getWorld());
            if (!(entity instanceof LivingEntity)) continue;

            Identifier id = Registries.ENTITY_TYPE.getId(type);
            if (JournalConfig.isBlacklisted(id)) continue;

            if (journal.unlockMob(id)) {
                unlocked++;
            }
        }

        SyncJournalPayload.sendToClient(player, journal.getDiscovered());

        int finalUnlocked = unlocked;
        context.getSource().sendFeedback(() ->
                Text.literal("Unlocked " + finalUnlocked + " mobs in the journal."), false);

        return unlocked;
    }

    private static int clearAll(CommandContext<ServerCommandSource> context) {
        var player = context.getSource().getPlayer();
        var journal = JournalComponents.JOURNAL.get(player);
        journal.clearDiscovered();

        SyncJournalPayload.sendToClient(player, Collections.emptyMap());

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
                    Text.literal("Removed mob from journal:  " + id), false);

            SyncJournalPayload.sendToClient(player, journal.getDiscovered());

            return 1;
        } else {
            context.getSource().sendFeedback(() ->
                    Text.literal("Mob not found in journal: " + id), false);
            return 0;
        }
    }

    private static int openBlacklist(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        OpenBlacklistScreenPayload.sendToClient(player);
        return 1;
    }

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_DISCOVERED_MOBS = (context, builder) -> {
        ServerPlayerEntity player = context.getSource().getPlayer();
        var journal = JournalComponents.JOURNAL.get(player);

        return net.minecraft.command.CommandSource.suggestIdentifiers(
                journal.getDiscovered().keySet().stream(), builder
        );
    };
}