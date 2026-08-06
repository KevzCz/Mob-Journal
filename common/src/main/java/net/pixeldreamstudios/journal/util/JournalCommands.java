package net.pixeldreamstudios.journal.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.journal.config.JournalConfig;
import net.pixeldreamstudios.journal.data.JournalData;
import net.pixeldreamstudios.journal.data.JournalStorage;
import net.pixeldreamstudios.journal.network.NetworkManager;
import net.pixeldreamstudios.journal.network.OpenBlacklistScreenPayload;
import net.pixeldreamstudios.journal.network.SyncJournalPayload;

import java.util.Collections;

public class JournalCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        dispatcher.register(Commands.literal("journal")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlock_all")
                        .executes(JournalCommands::unlockAll))
                .then(Commands.literal("clear_all")
                        .executes(JournalCommands::clearAll))
                .then(Commands.literal("remove")
                        .then(Commands.argument("mob", ResourceLocationArgument.id())
                                .suggests(SUGGEST_DISCOVERED_MOBS)
                                .executes(JournalCommands::removeMob)))
                .then(Commands.literal("blacklist")
                        .executes(JournalCommands::openBlacklist))
        );
    }

    private static int unlockAll(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        JournalData journal = JournalStorage.getJournal(player);

        int unlocked = 0;
        long timestamp = player.getServer().overworld().getGameTime();

        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            if (!type.canSummon()) continue;

            if (SafeEntityFactory.createLiving(type, player.level()) == null) continue;

            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (JournalConfig.isBlacklisted(id)) continue;

            if (journal.unlockMob(id, timestamp)) {
                unlocked++;
            }
        }

        NetworkManager.sendToClient(player, new SyncJournalPayload(journal.getDiscovered()));

        int finalUnlocked = unlocked;
        context.getSource().sendSuccess(() ->
                Component.literal("Unlocked " + finalUnlocked + " mobs in the journal."), false);

        return unlocked;
    }

    private static int clearAll(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        JournalData journal = JournalStorage.getJournal(player);
        journal.clearDiscovered();

        NetworkManager.sendToClient(player, new SyncJournalPayload(Collections.emptyMap()));

        context.getSource().sendSuccess(() ->
                Component.literal("Cleared all discovered mobs in the journal."), false);
        return 1;
    }

    private static int removeMob(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        JournalData journal = JournalStorage.getJournal(player);
        ResourceLocation id = ResourceLocationArgument.getId(context, "mob");

        if (journal.removeMob(id)) {
            context.getSource().sendSuccess(() ->
                    Component.literal("Removed mob from journal: " + id), false);

            NetworkManager.sendToClient(player, new SyncJournalPayload(journal.getDiscovered()));

            return 1;
        } else {
            context.getSource().sendSuccess(() ->
                    Component.literal("Mob not found in journal: " + id), false);
            return 0;
        }
    }

    private static int openBlacklist(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        NetworkManager.sendToClient(player, OpenBlacklistScreenPayload.INSTANCE);
        return 1;
    }

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_DISCOVERED_MOBS = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayerOrException();
        JournalData journal = JournalStorage.getJournal(player);

        return SharedSuggestionProvider.suggestResource(
                journal.getDiscovered().keySet().stream(), builder
        );
    };
}
