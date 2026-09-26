package fr.nexora.friend.listener;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.model.NetworkEventType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerJoinListener implements Listener {

    private final NexoraFriend plugin;

    public PlayerJoinListener(NexoraFriend plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        plugin.profileManager().loadOrCreate(uuid, player.getName())
                .thenCompose(profile -> CompletableFuture.allOf(
                        plugin.friendManager().loadForPlayer(uuid),
                        plugin.blockManager().loadForPlayer(uuid),
                        plugin.requestManager().loadForPlayer(uuid)
                ))
                .whenComplete((v, throwable) -> plugin.scheduler().runSync(() -> onDataLoaded(player, uuid, throwable)));
    }

    private void onDataLoaded(Player player, UUID uuid, Throwable throwable) {
        if (throwable != null) {
            plugin.getLogger().log(Level.WARNING, "Failed to load social data for " + player.getName(), throwable);
            return;
        }
        if (!player.isOnline()) {
            return;
        }

        int pendingIncoming = plugin.requestManager().getIncoming(uuid).size();
        plugin.notificationManager().pendingRequestsOnJoin(player, pendingIncoming);

        for (UUID friendUuid : plugin.friendManager().getFriends(uuid)) {
            Player friend = Bukkit.getPlayer(friendUuid);
            if (friend != null) {
                plugin.notificationManager().friendOnline(friend, player.getName());
            }
        }

        plugin.cacheManager().setNetworkOnline(uuid, true);
        plugin.networkManager().publish(NetworkEventType.PLAYER_ONLINE, uuid, null);
    }
}
