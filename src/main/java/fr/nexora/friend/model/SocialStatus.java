package fr.nexora.friend.model;

/**
 * Extensible social presence status. Extra statuses can be appended
 * without touching persistence (stored as plain enum name).
 */
public enum SocialStatus {
    ONLINE,
    AWAY,
    DND,
    INVISIBLE,
    OFFLINE
}
