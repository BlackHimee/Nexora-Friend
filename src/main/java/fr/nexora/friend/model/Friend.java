package fr.nexora.friend.model;

import java.time.Instant;
import java.util.UUID;

public record Friend(UUID owner, UUID friend, Instant createdAt) {
}
