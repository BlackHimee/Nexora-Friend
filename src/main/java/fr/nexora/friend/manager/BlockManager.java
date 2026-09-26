package fr.nexora.friend.manager;

import fr.nexora.friend.database.dao.BlockDao;
import fr.nexora.friend.database.dao.FriendDao;
import fr.nexora.friend.database.dao.RequestDao;
import fr.nexora.friend.model.NetworkEventType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BlockManager {

    private final BlockDao blockDao;
    private final FriendDao friendDao;
    private final RequestDao requestDao;
    private final CacheManager cache;
    private NetworkManager networkManager;

    public BlockManager(BlockDao blockDao, FriendDao friendDao, RequestDao requestDao, CacheManager cache) {
        this.blockDao = blockDao;
        this.friendDao = friendDao;
        this.requestDao = requestDao;
        this.cache = cache;
    }

    /** Late-bound to break the BlockManager <-> NetworkManager construction cycle. */
    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public CompletableFuture<Void> loadForPlayer(UUID uuid) {
        return blockDao.getBlocked(uuid).thenAccept(list -> cache.putBlocked(uuid, new HashSet<>(list)));
    }

    public boolean isBlocked(UUID player, UUID target) {
        return cache.getBlocked(player).contains(target);
    }

    /**
     * Cache-independent block check, straight from the database. Use this
     * (instead of {@link #isBlocked}) whenever {@code player} might not be
     * loaded on this server - e.g. checking whether a request's target has
     * blocked the requester, when the target could be online on a different
     * server in the network (or simply never joined this one).
     */
    public CompletableFuture<Boolean> isBlockedInDatabase(UUID player, UUID target) {
        return blockDao.isBlocked(player, target);
    }

    public Set<UUID> getBlocked(UUID player) {
        return cache.getBlocked(player);
    }

    /**
     * Blocks a player: persists the block, silently removes any existing
     * friendship and clears any pending request between the two players.
     */
    public CompletableFuture<Void> block(UUID player, UUID target) {
        CompletableFuture<Void> blockFuture = blockDao.block(player, target)
                .thenRun(() -> cache.addBlocked(player, target));

        CompletableFuture<Void> unfriendFuture = friendDao.areFriends(player, target).thenCompose(areFriends -> {
            if (Boolean.TRUE.equals(areFriends)) {
                return friendDao.removeFriendship(player, target).thenRun(() -> {
                    cache.removeFriend(player, target);
                    cache.removeFriend(target, player);
                });
            }
            return CompletableFuture.completedFuture(null);
        });

        CompletableFuture<Void> clearRequests = CompletableFuture.allOf(
                requestDao.deleteRequest(player, target),
                requestDao.deleteRequest(target, player)
        ).thenRun(() -> {
            cache.removeRequest(player, target);
            cache.removeRequest(target, player);
        });

        return CompletableFuture.allOf(blockFuture, unfriendFuture, clearRequests).thenRun(() -> {
            if (networkManager != null) {
                networkManager.publish(NetworkEventType.BLOCKED, player, target);
            }
        });
    }

    public CompletableFuture<Void> unblock(UUID player, UUID target) {
        return blockDao.unblock(player, target).thenRun(() -> {
            cache.removeBlocked(player, target);
            if (networkManager != null) {
                networkManager.publish(NetworkEventType.UNBLOCKED, player, target);
            }
        });
    }
}
