package fr.nexora.friend.manager;

import fr.nexora.friend.database.dao.RequestDao;
import fr.nexora.friend.model.AddPrivacy;
import fr.nexora.friend.model.FriendRequest;
import fr.nexora.friend.model.RelationState;
import fr.nexora.friend.model.RequestResult;
import fr.nexora.friend.model.SocialProfile;
import fr.nexora.friend.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RequestManager {

    private final RequestDao requestDao;
    private final CacheManager cache;
    private final FriendManager friendManager;
    private final BlockManager blockManager;
    private final ProfileManager profileManager;
    private final NotificationManager notifications;
    private final ConfigManager configManager;
    private final Scheduler scheduler;

    public RequestManager(RequestDao requestDao, CacheManager cache, FriendManager friendManager,
                           BlockManager blockManager, ProfileManager profileManager,
                           NotificationManager notifications, ConfigManager configManager, Scheduler scheduler) {
        this.requestDao = requestDao;
        this.cache = cache;
        this.friendManager = friendManager;
        this.blockManager = blockManager;
        this.profileManager = profileManager;
        this.notifications = notifications;
        this.configManager = configManager;
        this.scheduler = scheduler;
    }

    public CompletableFuture<Void> loadForPlayer(UUID uuid) {
        CompletableFuture<Void> incoming = requestDao.getIncoming(uuid).thenAccept(list -> cache.putIncoming(uuid, list));
        CompletableFuture<Void> outgoing = requestDao.getOutgoing(uuid).thenAccept(list -> cache.putOutgoing(uuid, list));
        return CompletableFuture.allOf(incoming, outgoing);
    }

    public List<FriendRequest> getIncoming(UUID uuid) {
        return cache.getIncoming(uuid);
    }

    public List<FriendRequest> getOutgoing(UUID uuid) {
        return cache.getOutgoing(uuid);
    }

    public RelationState getRelation(UUID viewer, UUID target) {
        if (viewer.equals(target)) {
            return RelationState.NONE;
        }
        if (blockManager.isBlocked(viewer, target)) {
            return RelationState.BLOCKED;
        }
        if (blockManager.isBlocked(target, viewer)) {
            return RelationState.BLOCKED_BY;
        }
        if (friendManager.areFriends(viewer, target)) {
            return RelationState.FRIENDS;
        }
        if (cache.getOutgoing(viewer).stream().anyMatch(r -> r.target().equals(target))) {
            return RelationState.PENDING_OUTGOING;
        }
        if (cache.getIncoming(viewer).stream().anyMatch(r -> r.requester().equals(target))) {
            return RelationState.PENDING_INCOMING;
        }
        return RelationState.NONE;
    }

    public CompletableFuture<RequestResult> sendRequest(Player requester, UUID targetUuid) {
        UUID requesterUuid = requester.getUniqueId();

        if (requesterUuid.equals(targetUuid)) {
            return CompletableFuture.completedFuture(RequestResult.SELF);
        }

        long cooldownMs = configManager.config().getLong("requests.request-cooldown-seconds", 5) * 1000L;
        long lastRequest = cache.getLastRequestTime(requesterUuid);
        if (cooldownMs > 0 && System.currentTimeMillis() - lastRequest < cooldownMs) {
            return CompletableFuture.completedFuture(RequestResult.COOLDOWN);
        }

        if (blockManager.isBlocked(requesterUuid, targetUuid)) {
            return CompletableFuture.completedFuture(RequestResult.YOU_BLOCKED_TARGET);
        }
        if (blockManager.isBlocked(targetUuid, requesterUuid)) {
            return CompletableFuture.completedFuture(RequestResult.TARGET_BLOCKED_YOU);
        }
        if (friendManager.areFriends(requesterUuid, targetUuid)) {
            return CompletableFuture.completedFuture(RequestResult.ALREADY_FRIENDS);
        }
        if (cache.getOutgoing(requesterUuid).stream().anyMatch(r -> r.target().equals(targetUuid))) {
            return CompletableFuture.completedFuture(RequestResult.ALREADY_PENDING);
        }
        if (cache.getIncoming(requesterUuid).stream().anyMatch(r -> r.requester().equals(targetUuid))) {
            return CompletableFuture.completedFuture(RequestResult.INCOMING_PENDING);
        }

        int limit = friendManager.getLimit(requester);
        if (friendManager.getFriendCount(requesterUuid) >= limit) {
            return CompletableFuture.completedFuture(RequestResult.LIMIT_REACHED);
        }

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer != null && friendManager.getFriendCount(targetUuid) >= friendManager.getLimit(targetPlayer)) {
            return CompletableFuture.completedFuture(RequestResult.TARGET_LIMIT_REACHED);
        }

        return profileManager.fetch(targetUuid).thenCompose(targetProfileOpt -> {
            AddPrivacy privacy = targetProfileOpt.map(SocialProfile::addPrivacy).orElse(AddPrivacy.EVERYONE);

            if (privacy == AddPrivacy.NOBODY) {
                return CompletableFuture.completedFuture(RequestResult.PRIVACY_DENIED);
            }

            if (privacy == AddPrivacy.FRIENDS_OF_FRIENDS) {
                return checkSharedFriend(requesterUuid, targetUuid).thenCompose(shared -> {
                    if (!shared) {
                        return CompletableFuture.completedFuture(RequestResult.PRIVACY_DENIED);
                    }
                    return createAndSendRequest(requester, targetUuid, targetPlayer);
                });
            }

            return createAndSendRequest(requester, targetUuid, targetPlayer);
        });
    }

    private CompletableFuture<Boolean> checkSharedFriend(UUID requesterUuid, UUID targetUuid) {
        return friendManager.fetchFriends(requesterUuid).thenCombine(friendManager.fetchFriends(targetUuid),
                (requesterFriends, targetFriends) -> {
                    Set<UUID> targetSet = new HashSet<>(targetFriends);
                    return requesterFriends.stream().anyMatch(targetSet::contains);
                });
    }

    private CompletableFuture<RequestResult> createAndSendRequest(Player requester, UUID targetUuid, Player targetPlayer) {
        UUID requesterUuid = requester.getUniqueId();
        FriendRequest request = new FriendRequest(requesterUuid, targetUuid, Instant.now());

        return requestDao.createRequest(requesterUuid, targetUuid).thenApply(v -> {
            cache.addRequest(request);
            cache.markRequestSent(requesterUuid);

            scheduler.runSync(() -> {
                String targetName = targetPlayer != null ? targetPlayer.getName() : resolveOfflineName(targetUuid);
                notifications.requestSent(requester, targetName);
                if (targetPlayer != null) {
                    notifications.requestReceived(targetPlayer, requester.getName());
                }
            });

            return RequestResult.SUCCESS;
        });
    }

    public CompletableFuture<RequestResult> acceptRequest(Player target, UUID requesterUuid) {
        UUID targetUuid = target.getUniqueId();
        FriendRequest pending = cache.getIncoming(targetUuid).stream()
                .filter(r -> r.requester().equals(requesterUuid)).findFirst().orElse(null);
        if (pending == null) {
            return CompletableFuture.completedFuture(RequestResult.NOT_FOUND);
        }

        int limit = friendManager.getLimit(target);
        if (friendManager.getFriendCount(targetUuid) >= limit) {
            return CompletableFuture.completedFuture(RequestResult.LIMIT_REACHED);
        }

        // Removed synchronously so an interleaved second click on the same request is rejected
        // immediately as NOT_FOUND instead of racing this one to the database.
        cache.removeRequest(requesterUuid, targetUuid);

        return requestDao.deleteRequest(requesterUuid, targetUuid)
                .thenCompose(v -> friendManager.createFriendship(requesterUuid, targetUuid))
                .exceptionallyCompose(ex -> {
                    cache.addRequest(pending);
                    return CompletableFuture.failedFuture(ex);
                })
                .thenApply(v -> {
                    scheduler.runSync(() -> {
                        String requesterName = resolveOfflineName(requesterUuid);
                        Player requesterPlayer = Bukkit.getPlayer(requesterUuid);
                        if (requesterPlayer != null) {
                            requesterName = requesterPlayer.getName();
                            notifications.requestAcceptedOther(requesterPlayer, target.getName());
                        }
                        notifications.friendAdded(target, requesterName);
                    });
                    return RequestResult.SUCCESS;
                });
    }

    public CompletableFuture<RequestResult> denyRequest(Player target, UUID requesterUuid) {
        UUID targetUuid = target.getUniqueId();
        FriendRequest pending = cache.getIncoming(targetUuid).stream()
                .filter(r -> r.requester().equals(requesterUuid)).findFirst().orElse(null);
        if (pending == null) {
            return CompletableFuture.completedFuture(RequestResult.NOT_FOUND);
        }

        cache.removeRequest(requesterUuid, targetUuid);

        return requestDao.deleteRequest(requesterUuid, targetUuid)
                .exceptionallyCompose(ex -> {
                    cache.addRequest(pending);
                    return CompletableFuture.failedFuture(ex);
                })
                .thenApply(v -> {
            scheduler.runSync(() -> {
                String requesterName = resolveOfflineName(requesterUuid);
                Player requesterPlayer = Bukkit.getPlayer(requesterUuid);
                if (requesterPlayer != null) {
                    requesterName = requesterPlayer.getName();
                    notifications.requestDeniedOther(requesterPlayer, target.getName());
                }
                notifications.requestDeniedSelf(target, requesterName);
            });
            return RequestResult.SUCCESS;
        });
    }

    public CompletableFuture<RequestResult> cancelRequest(Player requester, UUID targetUuid) {
        UUID requesterUuid = requester.getUniqueId();
        FriendRequest pending = cache.getOutgoing(requesterUuid).stream()
                .filter(r -> r.target().equals(targetUuid)).findFirst().orElse(null);
        if (pending == null) {
            return CompletableFuture.completedFuture(RequestResult.NOT_FOUND);
        }

        cache.removeRequest(requesterUuid, targetUuid);

        return requestDao.deleteRequest(requesterUuid, targetUuid)
                .exceptionallyCompose(ex -> {
                    cache.addRequest(pending);
                    return CompletableFuture.failedFuture(ex);
                })
                .thenApply(v -> {
            scheduler.runSync(() -> {
                String targetName = resolveOfflineName(targetUuid);
                Player targetPlayer = Bukkit.getPlayer(targetUuid);
                if (targetPlayer != null) {
                    targetName = targetPlayer.getName();
                }
                notifications.requestCancelled(requester, targetName);
            });
            return RequestResult.SUCCESS;
        });
    }

    public CompletableFuture<Void> clearAllInvolving(UUID player) {
        return requestDao.deleteAllInvolving(player).thenRun(() -> cache.clearRequests(player));
    }

    private String resolveOfflineName(UUID uuid) {
        SocialProfile profile = profileManager.getCached(uuid);
        if (profile != null) {
            return profile.lastKnownName();
        }
        return Bukkit.getOfflinePlayer(uuid).getName() != null ? Bukkit.getOfflinePlayer(uuid).getName() : "?";
    }
}
