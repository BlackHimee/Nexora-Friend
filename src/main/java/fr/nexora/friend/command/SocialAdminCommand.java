package fr.nexora.friend.command;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.model.SocialProfile;
import fr.nexora.friend.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SocialAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "info", "friends", "remove", "clear", "debug");

    private final NexoraFriend plugin;

    public SocialAdminCommand(NexoraFriend plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("nexora.friend.admin")) {
            plugin.messageUtils().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(MessageUtils.color("&eUsage: /socialadmin <reload|info|friends|remove|clear|debug>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "info" -> handleInfo(sender, args);
            case "friends" -> handleFriends(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "clear" -> handleClear(sender, args);
            case "debug" -> handleDebug(sender);
            default -> sender.sendMessage(MessageUtils.color("&cSous-commande inconnue."));
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("nexora.friend.admin.reload")) {
            plugin.messageUtils().send(sender, "no-permission");
            return;
        }
        plugin.configManager().reload();
        plugin.messageUtils().send(sender, "admin-reload-success");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nexora.friend.admin.info")) {
            plugin.messageUtils().send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&eUsage: /socialadmin info <joueur>"));
            return;
        }

        resolveUuid(args[1]).thenAccept(uuidOpt -> plugin.scheduler().runSync(() -> {
            if (uuidOpt.isEmpty()) {
                plugin.messageUtils().send(sender, "admin-player-not-found");
                return;
            }
            UUID uuid = uuidOpt.get();
            SocialProfile profile = plugin.profileManager().getCached(uuid);
            int friendCount = plugin.friendManager().getFriendCount(uuid);
            int incoming = plugin.requestManager().getIncoming(uuid).size();
            int outgoing = plugin.requestManager().getOutgoing(uuid).size();
            int blocked = plugin.blockManager().getBlocked(uuid).size();

            sender.sendMessage(MessageUtils.color("&8&m----&r &bInfos sociales: &f" + args[1] + " &8&m----"));
            sender.sendMessage(MessageUtils.color("&7UUID: &f" + uuid));
            sender.sendMessage(MessageUtils.color("&7Statut: &f" + (profile != null ? profile.status() : "?")));
            sender.sendMessage(MessageUtils.color("&7Amis: &f" + friendCount));
            sender.sendMessage(MessageUtils.color("&7Demandes reçues: &f" + incoming));
            sender.sendMessage(MessageUtils.color("&7Demandes envoyées: &f" + outgoing));
            sender.sendMessage(MessageUtils.color("&7Joueurs bloqués: &f" + blocked));
        }));
    }

    private void handleFriends(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&eUsage: /socialadmin friends <joueur>"));
            return;
        }

        resolveUuid(args[1]).thenAccept(uuidOpt -> plugin.scheduler().runSync(() -> {
            if (uuidOpt.isEmpty()) {
                plugin.messageUtils().send(sender, "admin-player-not-found");
                return;
            }
            List<UUID> friends = List.copyOf(plugin.friendManager().getFriends(uuidOpt.get()));
            String names = friends.stream()
                    .map(uuid -> {
                        Player online = Bukkit.getPlayer(uuid);
                        if (online != null) {
                            return online.getName();
                        }
                        return Bukkit.getOfflinePlayer(uuid).getName();
                    })
                    .collect(Collectors.joining(", "));
            sender.sendMessage(MessageUtils.color("&b" + args[1] + " &7(&f" + friends.size() + "&7): &f" + names));
        }));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nexora.friend.admin.remove")) {
            plugin.messageUtils().send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(MessageUtils.color("&eUsage: /socialadmin remove <joueur> <ami>"));
            return;
        }

        resolveUuid(args[1]).thenCombine(resolveUuid(args[2]), Map::entry).thenAccept(pair -> plugin.scheduler().runSync(() -> {
            if (pair.getKey().isEmpty() || pair.getValue().isEmpty()) {
                plugin.messageUtils().send(sender, "admin-player-not-found");
                return;
            }
            plugin.friendManager().forceRemoveFriendship(pair.getKey().get(), pair.getValue().get())
                    .whenComplete((v, throwable) -> plugin.scheduler().runSync(() -> {
                        if (throwable != null) {
                            plugin.messageUtils().send(sender, "generic-error");
                            return;
                        }
                        plugin.messageUtils().send(sender, "admin-friend-removed", Map.of("player", args[1], "friend", args[2]));
                    }));
        }));
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("nexora.friend.admin.clear")) {
            plugin.messageUtils().send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.color("&eUsage: /socialadmin clear <joueur>"));
            return;
        }

        resolveUuid(args[1]).thenAccept(uuidOpt -> {
            if (uuidOpt.isEmpty()) {
                plugin.scheduler().runSync(() -> plugin.messageUtils().send(sender, "admin-player-not-found"));
                return;
            }
            plugin.friendManager().clearAllFriends(uuidOpt.get()).whenComplete((count, throwable) ->
                    plugin.scheduler().runSync(() -> {
                        if (throwable != null) {
                            plugin.messageUtils().send(sender, "generic-error");
                            return;
                        }
                        plugin.messageUtils().send(sender, "admin-friends-cleared",
                                Map.of("player", args[1], "count", String.valueOf(count)));
                    }));
        });
    }

    private void handleDebug(CommandSender sender) {
        sender.sendMessage(MessageUtils.color("&8&m----&r &bNexora-Friend Debug &8&m----"));
        sender.sendMessage(MessageUtils.color("&7Database: &f" + plugin.configManager().config().getString("database.type")));
        sender.sendMessage(MessageUtils.color("&7Debug mode: &f" + plugin.configManager().isDebug()));
        sender.sendMessage(MessageUtils.color("&7LuckPerms hook: &f" + (plugin.luckPermsHook() != null)));
        sender.sendMessage(MessageUtils.color("&7PlaceholderAPI hook: &f" + plugin.placeholderApiEnabled()));
        sender.sendMessage(MessageUtils.color("&7Online players tracked: &f" + Bukkit.getOnlinePlayers().size()));
        sender.sendMessage(MessageUtils.color("&7Network sync: &f" + plugin.networkManager().isEnabled()
                + (plugin.networkManager().isEnabled() ? " &7(server-id: &f" + plugin.networkManager().serverId() + "&7)" : "")));
    }

    private java.util.concurrent.CompletableFuture<Optional<UUID>> resolveUuid(String name) {
        return plugin.profileManager().resolveByName(name);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 || args.length == 3) {
            String prefix = args[args.length - 1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
