package fr.nexora.friend.hook;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.model.SocialProfile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final NexoraFriend plugin;

    public PlaceholderAPIHook(NexoraFriend plugin) {
        this.plugin = plugin;
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nexorafriend";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nexora";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        UUID uuid = player.getUniqueId();

        return switch (params.toLowerCase()) {
            case "count" -> String.valueOf(plugin.friendManager().getFriendCount(uuid));
            case "limit" -> player.getPlayer() != null ? String.valueOf(plugin.friendManager().getLimit(player.getPlayer())) : "";
            case "online" -> String.valueOf(plugin.friendManager().getFriends(uuid).stream()
                    .filter(friend -> plugin.profileManager().isOnline(friend))
                    .count());
            case "requests" -> String.valueOf(plugin.requestManager().getIncoming(uuid).size());
            case "status" -> {
                SocialProfile profile = plugin.profileManager().getCached(uuid);
                yield profile != null ? profile.status().name() : "OFFLINE";
            }
            default -> null;
        };
    }
}
