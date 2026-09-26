package fr.nexora.friend.model;

import java.util.UUID;

/**
 * A single row from {@code nf_network_events}. {@code playerB} is null for
 * events that only concern one player (presence changes).
 */
public record NetworkEvent(long id, NetworkEventType type, String originServer, UUID playerA, UUID playerB, long createdAt) {
}
