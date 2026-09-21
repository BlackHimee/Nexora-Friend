package fr.nexora.friend.manager;

import fr.nexora.friend.database.dao.BlockDao;
import fr.nexora.friend.database.dao.FriendDao;
import fr.nexora.friend.database.dao.RequestDao;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BlockManager {

    private final BlockDao blockDao;
    private final FriendDao friendDao;
    private final RequestDao requestDao;
    private final CacheManager cache;

    public BlockManager(BlockDao blockDao, FriendDao friendDao, RequestDao requestDao, CacheManager cache) {
        this.blockDao = blockDao;
        this.friendDao = friendDao;
        this.requestDao = requestDao;
        this.cache = cache;
    }

    public CompletableFuture<Void> loadForPlayer(UUID uuid) {
        return blockDao.getBlocked(uuid).thenAccept(list -> cache.putBlocked(uuid, new HashSet<>(list)));
    }

    public boolean isBlocked(UUID player, UUID target) {
        return cache.getBlocked(player).contains(target);
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

        return CompletableFuture.allOf(blockFuture, unfriendFuture, clearRequests);
    }

    public CompletableFuture<Void> unblock(UUID player, UUID target) {
        return blockDao.unblock(player, target).thenRun(() -> cache.removeBlocked(player, target));
    }
}
