package fr.nexora.friend.model;

import java.time.Instant;
import java.util.UUID;

public record FriendRequest(UUID requester, UUID target, Instant createdAt) {
}
