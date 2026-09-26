package fr.nexora.friend.listener;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.model.NetworkEventType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerQuitListener implements Listener {

    private final NexoraFriend plugin;

    public PlayerQuitListener(NexoraFriend plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        for (UUID friendUuid : plugin.friendManager().getFriends(uuid)) {
            Player friend = Bukkit.getPlayer(friendUuid);
            if (friend != null) {
                plugin.notificationManager().friendOffline(friend, player.getName());
            }
        }

        plugin.profileManager().markOffline(uuid);
        plugin.guiManager().clear(uuid);
        plugin.cacheManager().unloadPlayer(uuid);

        plugin.cacheManager().setNetworkOnline(uuid, false);
        plugin.networkManager().publish(NetworkEventType.PLAYER_OFFLINE, uuid, null);
    }
}
