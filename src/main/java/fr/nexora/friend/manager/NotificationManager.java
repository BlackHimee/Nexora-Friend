package fr.nexora.friend.manager;

import fr.nexora.friend.model.RequestResult;
import fr.nexora.friend.model.SocialProfile;
import fr.nexora.friend.util.MessageUtils;
import fr.nexora.friend.util.SoundUtil;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class NotificationManager {

    private final ConfigManager configManager;
    private final MessageUtils messages;
    private final SoundUtil sounds;
    private final CacheManager cache;

    public NotificationManager(ConfigManager configManager, MessageUtils messages, SoundUtil sounds, CacheManager cache) {
        this.configManager = configManager;
        this.messages = messages;
        this.sounds = sounds;
        this.cache = cache;
    }

    private boolean globallyEnabled(String key) {
        return configManager.config().getBoolean("notifications." + key, true);
    }

    private boolean playerAllows(UUID uuid, Predicate<SocialProfile> check) {
        SocialProfile profile = cache.getProfile(uuid);
        if (profile == null) {
            return true;
        }
        if (!profile.notificationsEnabled()) {
            return false;
        }
        return check.test(profile);
    }

    public void friendAdded(Player player, String otherName) {
        messages.send(player, "friend-added", Map.of("player", otherName));
        sounds.play(player, "request-accepted");
    }

    public void requestReceived(Player target, String requesterName) {
        if (!globallyEnabled("friend-request-received") || !playerAllows(target.getUniqueId(), SocialProfile::notifyRequests)) {
            return;
        }
        messages.send(target, "request-received", Map.of("player", requesterName));
        sounds.play(target, "request-received");
    }

    public void requestSent(Player requester, String targetName) {
        messages.send(requester, "request-sent", Map.of("player", targetName));
        sounds.play(requester, "request-sent");
    }

    public void requestAcceptedOther(Player requester, String accepterName) {
        if (!globallyEnabled("friend-request-accepted") || !playerAllows(requester.getUniqueId(), SocialProfile::notifyRequests)) {
            return;
        }
        messages.send(requester, "request-accepted-target", Map.of("player", accepterName));
        sounds.play(requester, "request-accepted");
    }

    public void requestDeniedSelf(Player denier, String requesterName) {
        messages.send(denier, "request-denied", Map.of("player", requesterName));
        sounds.play(denier, "request-denied");
    }

    public void requestDeniedOther(Player requester, String denierName) {
        if (!globallyEnabled("friend-request-denied") || !playerAllows(requester.getUniqueId(), SocialProfile::notifyRequests)) {
            return;
        }
        messages.send(requester, "request-denied-target", Map.of("player", denierName));
    }

    public void requestCancelled(Player requester, String targetName) {
        messages.send(requester, "request-cancelled", Map.of("player", targetName));
    }

    public void friendOnline(Player friendOfPlayer, String onlinePlayerName) {
        if (!globallyEnabled("friend-online") || !playerAllows(friendOfPlayer.getUniqueId(), SocialProfile::notifyOnline)) {
            return;
        }
        messages.send(friendOfPlayer, "friend-connected", Map.of("player", onlinePlayerName));
    }

    public void friendOffline(Player friendOfPlayer, String offlinePlayerName) {
        if (!globallyEnabled("friend-offline") || !playerAllows(friendOfPlayer.getUniqueId(), SocialProfile::notifyOffline)) {
            return;
        }
        messages.send(friendOfPlayer, "friend-disconnected", Map.of("player", offlinePlayerName));
    }

    public void friendRemovedByOther(Player player, String otherName) {
        if (!globallyEnabled("friend-removed")) {
            return;
        }
        messages.send(player, "friend-removed-by-other", Map.of("player", otherName));
        sounds.play(player, "friend-removed");
    }

    public void friendRemovedSelf(Player player, String otherName) {
        messages.send(player, "friend-removed", Map.of("player", otherName));
        sounds.play(player, "friend-removed");
    }

    public void pendingRequestsOnJoin(Player player, int count) {
        if (count <= 0 || !configManager.config().getBoolean("requests.notify-pending-on-join", true)) {
            return;
        }
        messages.send(player, "pending-requests-on-join", Map.of("count", String.valueOf(count)));
    }

    public void error(Player player) {
        messages.send(player, "generic-error");
        sounds.play(player, "error");
    }

    /** Reports why a {@code sendRequest} call did not succeed. SUCCESS is handled elsewhere. */
    public void sendRequestFailure(Player player, RequestResult result, String targetName, int limit) {
        sounds.play(player, "error");
        Map<String, String> placeholders = Map.of("player", targetName, "limit", String.valueOf(limit));
        switch (result) {
            case SELF -> messages.send(player, "cannot-add-yourself");
            case ALREADY_FRIENDS -> messages.send(player, "already-friends", placeholders);
            case ALREADY_PENDING, INCOMING_PENDING -> messages.send(player, "request-already-sent");
            case TARGET_BLOCKED_YOU -> messages.send(player, "blocked-you");
            case YOU_BLOCKED_TARGET -> messages.send(player, "you-blocked-them");
            case LIMIT_REACHED -> messages.send(player, "limit-reached", placeholders);
            case TARGET_LIMIT_REACHED -> messages.send(player, "target-limit-reached", placeholders);
            case COOLDOWN -> messages.send(player, "request-cooldown");
            case PRIVACY_DENIED -> messages.send(player, "privacy-denied", placeholders);
            case NOT_FOUND -> messages.send(player, "player-not-found");
            default -> messages.send(player, "generic-error");
        }
    }
}
