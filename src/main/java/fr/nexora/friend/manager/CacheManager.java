package fr.nexora.friend.manager;

import fr.nexora.friend.model.FriendRequest;
import fr.nexora.friend.model.SocialProfile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Central in-memory cache. Populated on join, invalidated on quit and on
 * every mutation so GUIs never have to hit the database on click.
 */
public class CacheManager {

    private final Map<UUID, SocialProfile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> friends = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> blocked = new ConcurrentHashMap<>();
    private final Map<UUID, List<FriendRequest>> incomingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, List<FriendRequest>> outgoingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> requestCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> networkPresence = new ConcurrentHashMap<>();

    // ---- profiles ----

    public SocialProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    public void putProfile(SocialProfile profile) {
        profiles.put(profile.uuid(), profile);
    }

    public void removeProfile(UUID uuid) {
        profiles.remove(uuid);
    }

    // ---- friends ----

    public Set<UUID> getFriends(UUID uuid) {
        return friends.getOrDefault(uuid, Set.of());
    }

    public void putFriends(UUID uuid, Set<UUID> friendSet) {
        friends.put(uuid, new CopyOnWriteArraySet<>(friendSet));
    }

    public void addFriend(UUID uuid, UUID friend) {
        friends.computeIfAbsent(uuid, k -> new CopyOnWriteArraySet<>()).add(friend);
    }

    public void removeFriend(UUID uuid, UUID friend) {
        Set<UUID> set = friends.get(uuid);
        if (set != null) {
            set.remove(friend);
        }
    }

    public void removeFriends(UUID uuid) {
        friends.remove(uuid);
    }

    // ---- blocked ----

    public Set<UUID> getBlocked(UUID uuid) {
        return blocked.getOrDefault(uuid, Set.of());
    }

    public void putBlocked(UUID uuid, Set<UUID> blockedSet) {
        blocked.put(uuid, new CopyOnWriteArraySet<>(blockedSet));
    }

    public void addBlocked(UUID uuid, UUID target) {
        blocked.computeIfAbsent(uuid, k -> new CopyOnWriteArraySet<>()).add(target);
    }

    public void removeBlocked(UUID uuid, UUID target) {
        Set<UUID> set = blocked.get(uuid);
        if (set != null) {
            set.remove(target);
        }
    }

    public void clearBlocked(UUID uuid) {
        blocked.remove(uuid);
    }

    // ---- requests ----

    public List<FriendRequest> getIncoming(UUID uuid) {
        return incomingRequests.getOrDefault(uuid, List.of());
    }

    public void putIncoming(UUID uuid, List<FriendRequest> requests) {
        incomingRequests.put(uuid, new CopyOnWriteArrayList<>(requests));
    }

    public List<FriendRequest> getOutgoing(UUID uuid) {
        return outgoingRequests.getOrDefault(uuid, List.of());
    }

    public void putOutgoing(UUID uuid, List<FriendRequest> requests) {
        outgoingRequests.put(uuid, new CopyOnWriteArrayList<>(requests));
    }

    public void addRequest(FriendRequest request) {
        incomingRequests.computeIfAbsent(request.target(), k -> new CopyOnWriteArrayList<>()).add(0, request);
        outgoingRequests.computeIfAbsent(request.requester(), k -> new CopyOnWriteArrayList<>()).add(0, request);
    }

    public void removeRequest(UUID requester, UUID target) {
        List<FriendRequest> incoming = incomingRequests.get(target);
        if (incoming != null) {
            incoming.removeIf(r -> r.requester().equals(requester));
        }
        List<FriendRequest> outgoing = outgoingRequests.get(requester);
        if (outgoing != null) {
            outgoing.removeIf(r -> r.target().equals(target));
        }
    }

    public void clearRequests(UUID uuid) {
        incomingRequests.remove(uuid);
        outgoingRequests.remove(uuid);
    }

    // ---- request cooldowns (anti-spam) ----

    public long getLastRequestTime(UUID uuid) {
        return requestCooldowns.getOrDefault(uuid, 0L);
    }

    public void markRequestSent(UUID uuid) {
        requestCooldowns.put(uuid, System.currentTimeMillis());
    }

    // ---- network-wide presence (updated by local join/quit and by remote PLAYER_ONLINE/OFFLINE events) ----

    public boolean isNetworkOnline(UUID uuid) {
        return networkPresence.getOrDefault(uuid, Boolean.FALSE);
    }

    public void setNetworkOnline(UUID uuid, boolean online) {
        networkPresence.put(uuid, online);
    }

    // ---- session lifecycle ----

    public void unloadPlayer(UUID uuid) {
        profiles.remove(uuid);
        friends.remove(uuid);
        blocked.remove(uuid);
        incomingRequests.remove(uuid);
        outgoingRequests.remove(uuid);
        requestCooldowns.remove(uuid);
    }
}
