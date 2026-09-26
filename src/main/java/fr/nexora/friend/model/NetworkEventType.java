package fr.nexora.friend.model;

/** Cross-server events broadcast through {@code nf_network_events} (MySQL/MariaDB only). */
public enum NetworkEventType {
    REQUEST_SENT,
    REQUEST_ACCEPTED,
    REQUEST_DENIED,
    REQUEST_CANCELLED,
    FRIEND_REMOVED,
    BLOCKED,
    UNBLOCKED,
    PLAYER_ONLINE,
    PLAYER_OFFLINE
}
