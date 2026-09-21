package fr.nexora.friend.model;

/** Relationship between a viewing player and a target player. */
public enum RelationState {
    NONE,
    PENDING_OUTGOING,
    PENDING_INCOMING,
    FRIENDS,
    BLOCKED,
    BLOCKED_BY
}
