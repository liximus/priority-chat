package com.listraind.prioritychat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PriorityChatCommands {

    private static final String PREFIX = "§8[§6PC§8] §r";

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
                ClientCommandManager.literal("priorityChat")
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("nickname", StringArgumentType.word())
                                        .suggests(PriorityChatCommands::suggestOnlinePlayers)
                                        .executes(PriorityChatCommands::addPerson)
                                )
                        )
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("nickname", StringArgumentType.word())
                                        .suggests(PriorityChatCommands::suggestFavourites)
                                        .executes(PriorityChatCommands::removePerson)
                                )
                        )
                        .then(ClientCommandManager.literal("list")
                                .executes(PriorityChatCommands::listPersons)
                        )
                        .then(ClientCommandManager.literal("clear")
                                .executes(PriorityChatCommands::clearPersons)
                        )
                        .executes(PriorityChatCommands::showHelp)
        );
    }

    private static int addPerson(CommandContext<FabricClientCommandSource> context) {
        String nickname = StringArgumentType.getString(context, "nickname");
        PriorityChatConfig storage = PriorityChatConfig.getInstance();

        if (storage.getIsPersonFavourite(nickname)) {
            send(context, PREFIX + "§cPlayer §f" + nickname + " §cis already in the list");
            return 0;
        }

        storage.addFavouritePerson(nickname);
        send(context, PREFIX + "§aAdded player §f" + nickname);
        return 1;
    }

    private static int removePerson(CommandContext<FabricClientCommandSource> context) {
        String nickname = StringArgumentType.getString(context, "nickname");
        PriorityChatConfig storage = PriorityChatConfig.getInstance();

        if (!storage.getIsPersonFavourite(nickname)) {
            send(context, PREFIX + "§cPlayer §f" + nickname + " §cnot found");
            return 0;
        }

        storage.removeFavouritePerson(nickname);
        send(context, PREFIX + "§eRemoved player §f" + nickname);
        return 1;
    }

    private static int listPersons(CommandContext<FabricClientCommandSource> context) {
        List<String> favourites = PriorityChatConfig.getInstance().getFavouritePersons();

        if (favourites.isEmpty()) {
            send(context, PREFIX + "§7List is empty");
            return 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX).append("§6Favourite players §7(").append(favourites.size()).append(")§r\n");
        for (int i = 0; i < favourites.size(); i++) {
            sb.append(" §8▸ §f").append(favourites.get(i));
            if (i < favourites.size() - 1) sb.append("\n");
        }

        send(context, sb.toString());
        return favourites.size();
    }

    private static int clearPersons(CommandContext<FabricClientCommandSource> context) {
        PriorityChatConfig storage = PriorityChatConfig.getInstance();
        int count = storage.getFavouritePersons().size();

        if (count == 0) {
            send(context, PREFIX + "§7List is already empty");
            return 0;
        }

        storage.putFavouritePersonsList(List.of());
        send(context, PREFIX + "§eCleared §f" + count + " §eplayers from the list");
        return count;
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        String help = PREFIX + "§6Commands:\n"
                + " §e/priorityChat add §7<nick> §8— §7add player\n"
                + " §e/priorityChat remove §7<nick> §8— §7remove player\n"
                + " §e/priorityChat list §8— §7show all players\n"
                + " §e/priorityChat clear §8— §7clear the list";
        send(context, help);
        return 1;
    }

    private static void send(CommandContext<FabricClientCommandSource> context, String msg) {
        context.getSource().sendFeedback(Component.literal(msg));
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(
            CommandContext<FabricClientCommandSource> context,
            SuggestionsBuilder builder) {
        var client = context.getSource().getClient();
        if (client.getConnection() != null) {
            client.getConnection().getOnlinePlayers().forEach(player -> {
                String name = player.getProfile().getName();
                if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(name);
                }
            });
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestFavourites(
            CommandContext<FabricClientCommandSource> context,
            SuggestionsBuilder builder) {
        List<String> favourites = PriorityChatConfig.getInstance().getFavouritePersons();
        for (String person : favourites) {
            if (person.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(person);
            }
        }
        return builder.buildFuture();
    }
}