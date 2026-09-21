package fr.nexora.friend.model;

import java.time.Instant;
import java.util.UUID;

public record Block(UUID player, UUID blocked, Instant createdAt) {
}
