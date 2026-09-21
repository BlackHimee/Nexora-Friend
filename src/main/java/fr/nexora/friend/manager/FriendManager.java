package fr.nexora.friend.manager;

import fr.nexora.friend.database.dao.FriendDao;
import fr.nexora.friend.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FriendManager {

    private final FriendDao friendDao;
    private final CacheManager cache;
    private final PermissionManager permissionManager;
    private final NotificationManager notifications;
    private final Scheduler scheduler;

    public FriendManager(FriendDao friendDao, CacheManager cache, PermissionManager permissionManager,
                          NotificationManager notifications, Scheduler scheduler) {
        this.friendDao = friendDao;
        this.cache = cache;
        this.permissionManager = permissionManager;
        this.notifications = notifications;
        this.scheduler = scheduler;
    }

    public CompletableFuture<Void> loadForPlayer(UUID uuid) {
        return friendDao.getFriends(uuid).thenAccept(list -> cache.putFriends(uuid, new HashSet<>(list)));
    }

    public Set<UUID> getFriends(UUID player) {
        return cache.getFriends(player);
    }

    /** Cache-first friend lookup that falls back to the database for players who are not currently loaded. */
    public CompletableFuture<List<UUID>> fetchFriends(UUID uuid) {
        Set<UUID> cached = cache.getFriends(uuid);
        if (!cached.isEmpty()) {
            return CompletableFuture.completedFuture(List.copyOf(cached));
        }
        return friendDao.getFriends(uuid);
    }

    public int getFriendCount(UUID player) {
        return cache.getFriends(player).size();
    }

    public boolean areFriends(UUID a, UUID b) {
        return cache.getFriends(a).contains(b);
    }

    public int getLimit(Player player) {
        return permissionManager.getFriendLimit(player);
    }

    public CompletableFuture<Void> createFriendship(UUID a, UUID b) {
        return friendDao.addFriendship(a, b).thenRun(() -> {
            cache.addFriend(a, b);
            cache.addFriend(b, a);
        });
    }

    public CompletableFuture<Void> removeFriendship(UUID actor, UUID target, String actorName, String targetName) {
        return friendDao.removeFriendship(actor, target).thenRun(() -> {
            cache.removeFriend(actor, target);
            cache.removeFriend(target, actor);
            scheduler.runSync(() -> {
                Player actorPlayer = Bukkit.getPlayer(actor);
                if (actorPlayer != null) {
                    notifications.friendRemovedSelf(actorPlayer, targetName);
                }
                Player targetPlayer = Bukkit.getPlayer(target);
                if (targetPlayer != null) {
                    notifications.friendRemovedByOther(targetPlayer, actorName);
                }
            });
        });
    }

    public CompletableFuture<Void> forceRemoveFriendship(UUID a, UUID b) {
        return friendDao.removeFriendship(a, b).thenRun(() -> {
            cache.removeFriend(a, b);
            cache.removeFriend(b, a);
        });
    }

    public CompletableFuture<Integer> clearAllFriends(UUID player) {
        return friendDao.getFriends(player).thenCompose(friends -> friendDao.removeAllFriendships(player).thenApply(v -> {
            for (UUID friend : friends) {
                cache.removeFriend(friend, player);
            }
            cache.removeFriends(player);
            return friends.size();
        }));
    }
}
