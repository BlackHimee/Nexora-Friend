package fr.nexora.friend.manager;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.database.dao.NetworkEventDao;
import fr.nexora.friend.gui.GuiManager;
import fr.nexora.friend.model.FriendRequest;
import fr.nexora.friend.model.NetworkEvent;
import fr.nexora.friend.model.NetworkEventType;
import fr.nexora.friend.model.SocialProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Cross-server sync (MySQL/MariaDB only): every server publishes a row to
 * {@code nf_network_events} whenever it performs a friend/request/block/presence
 * mutation, and polls that same table on an interval for rows other servers
 * published. This keeps every server's in-memory cache - and any open GUI -
 * correct in near-real-time without needing Redis or a proxy plugin channel.
 * <p>
 * Disabled automatically (with a warning) if {@code network.enabled} is true
 * but the active database is SQLite, since a single local file can't be
 * shared safely between multiple server processes.
 */
public class NetworkManager {

    private final NexoraFriend plugin;
    private final NetworkEventDao dao;
    private final CacheManager cache;
    private final FriendManager friendManager;
    private final NotificationManager notifications;
    private final ProfileManager profileManager;
    private final GuiManager guiManager;

    private final boolean enabled;
    private final String serverId;
    private final long pollIntervalTicks;
    private final long retentionMillis;

    private volatile long lastEventId;
    private BukkitTask pollTask;
    private BukkitTask cleanupTask;

    public NetworkManager(NexoraFriend plugin, NetworkEventDao dao, CacheManager cache, FriendManager friendManager,
                           NotificationManager notifications, ProfileManager profileManager, GuiManager guiManager) {
        this.plugin = plugin;
        this.dao = dao;
        this.cache = cache;
        this.friendManager = friendManager;
        this.notifications = notifications;
        this.profileManager = profileManager;
        this.guiManager = guiManager;

        var config = plugin.configManager().config();
        boolean requested = config.getBoolean("network.enabled", false);
        boolean supported = plugin.databaseManager().supportsNetworkSync();
        if (requested && !supported) {
            plugin.getLogger().warning("network.enabled is true but database.type isn't MYSQL/MARIADB - "
                    + "cross-server sync needs a shared network database, staying disabled.");
        }
        this.enabled = requested && supported;
        this.serverId = config.getString("network.server-id", "server1");
        this.pollIntervalTicks = Math.max(1, config.getLong("network.poll-interval-ticks", 20));
        this.retentionMillis = Math.max(1, config.getLong("network.event-retention-hours", 24)) * 3_600_000L;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String serverId() {
        return serverId;
    }

    public void start() {
        if (!enabled) {
            return;
        }

        dao.maxId().whenComplete((max, throwable) -> lastEventId = (throwable == null && max != null) ? max : 0L);

        pollTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::poll, pollIntervalTicks, pollIntervalTicks);
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanup, 20L * 60, 20L * 60 * 30);

        plugin.getLogger().info("Nexora-Friend network sync enabled (server-id=" + serverId + ").");
    }

    public void stop() {
        if (pollTask != null) {
            pollTask.cancel();
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
    }

    /** Fire-and-forget: publishes nothing when sync is disabled. */
    public void publish(NetworkEventType type, UUID playerA, UUID playerB) {
        if (!enabled) {
            return;
        }
        dao.publish(type, serverId, playerA, playerB).exceptionally(throwable -> {
            plugin.getLogger().log(Level.WARNING, "Failed to publish network event " + type, throwable);
            return null;
        });
    }

    private void poll() {
        dao.fetchAfter(lastEventId, 200).whenComplete((events, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().log(Level.WARNING, "Failed to poll network events", throwable);
                return;
            }
            for (NetworkEvent event : events) {
                lastEventId = Math.max(lastEventId, event.id());
                if (event.originServer().equals(serverId)) {
                    continue; // already applied locally when this server performed the action
                }
                Bukkit.getScheduler().runTask(plugin, () -> handle(event));
            }
        });
    }

    private void cleanup() {
        dao.purgeOlderThan(System.currentTimeMillis() - retentionMillis);
    }

    private void handle(NetworkEvent event) {
        switch (event.type()) {
            case REQUEST_SENT -> onRequestSent(event.playerA(), event.playerB());
            case REQUEST_ACCEPTED -> onRequestAccepted(event.playerA(), event.playerB());
            case REQUEST_DENIED -> onRequestDenied(event.playerA(), event.playerB());
            case REQUEST_CANCELLED -> onRequestCancelled(event.playerA(), event.playerB());
            case FRIEND_REMOVED -> onFriendRemoved(event.playerA(), event.playerB());
            case BLOCKED -> onBlocked(event.playerA(), event.playerB());
            case UNBLOCKED -> {
                // Nothing locally cached needs correcting for either side.
            }
            case PLAYER_ONLINE -> onPresence(event.playerA(), true);
            case PLAYER_OFFLINE -> onPresence(event.playerA(), false);
        }
    }

    private void onRequestSent(UUID requester, UUID target) {
        cache.addRequest(new FriendRequest(requester, target, Instant.now()));

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            notifications.requestReceived(targetPlayer, nameOf(requester));
            guiManager.refreshOpen(targetPlayer);
        }
        refreshIfOnline(requester);
    }

    private void onRequestAccepted(UUID requester, UUID target) {
        cache.removeRequest(requester, target);
        cache.addFriend(requester, target);
        cache.addFriend(target, requester);

        Player requesterPlayer = Bukkit.getPlayer(requester);
        if (requesterPlayer != null) {
            notifications.requestAcceptedOther(requesterPlayer, nameOf(target));
            guiManager.refreshOpen(requesterPlayer);
        }
        refreshIfOnline(target);
    }

    private void onRequestDenied(UUID requester, UUID target) {
        cache.removeRequest(requester, target);

        Player requesterPlayer = Bukkit.getPlayer(requester);
        if (requesterPlayer != null) {
            notifications.requestDeniedOther(requesterPlayer, nameOf(target));
            guiManager.refreshOpen(requesterPlayer);
        }
        refreshIfOnline(target);
    }

    private void onRequestCancelled(UUID requester, UUID target) {
        cache.removeRequest(requester, target);
        refreshIfOnline(target);
        refreshIfOnline(requester);
    }

    private void onFriendRemoved(UUID actor, UUID target) {
        cache.removeFriend(actor, target);
        cache.removeFriend(target, actor);

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            notifications.friendRemovedByOther(targetPlayer, nameOf(actor));
            guiManager.refreshOpen(targetPlayer);
        }
        refreshIfOnline(actor);
    }

    private void onBlocked(UUID player, UUID target) {
        // Blocks are silent to the blocked party by design - just correct any
        // friendship/pending-request state either side had cached locally.
        cache.removeFriend(player, target);
        cache.removeFriend(target, player);
        cache.removeRequest(player, target);
        cache.removeRequest(target, player);
        refreshIfOnline(target);
        refreshIfOnline(player);
    }

    private void onPresence(UUID uuid, boolean online) {
        cache.setNetworkOnline(uuid, online);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!friendManager.areFriends(onlinePlayer.getUniqueId(), uuid)) {
                continue;
            }
            if (online) {
                notifications.friendOnline(onlinePlayer, nameOf(uuid));
            } else {
                notifications.friendOffline(onlinePlayer, nameOf(uuid));
            }
            guiManager.refreshOpen(onlinePlayer);
        }
    }

    private void refreshIfOnline(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            guiManager.refreshOpen(player);
        }
    }

    private String nameOf(UUID uuid) {
        SocialProfile profile = profileManager.getCached(uuid);
        if (profile != null) {
            return profile.lastKnownName();
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        String offlineName = Bukkit.getOfflinePlayer(uuid).getName();
        return offlineName != null ? offlineName : "?";
    }
}
