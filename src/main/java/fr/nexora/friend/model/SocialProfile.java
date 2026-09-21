package fr.nexora.friend.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Mutable in-memory representation of a player's persisted social profile.
 * Instances live inside the cache and are flushed to the database by the managers.
 */
public class SocialProfile {

    private final UUID uuid;
    private volatile String lastKnownName;
    private volatile SocialStatus status;
    private volatile AddPrivacy addPrivacy;
    private volatile ProfilePrivacy profilePrivacy;
    private volatile Instant createdAt;
    private volatile Instant lastSeen;
    private volatile boolean notificationsEnabled;
    private volatile boolean notifyOnline;
    private volatile boolean notifyOffline;
    private volatile boolean notifyRequests;

    public SocialProfile(UUID uuid, String lastKnownName, SocialStatus status,
                          AddPrivacy addPrivacy, ProfilePrivacy profilePrivacy,
                          Instant createdAt, Instant lastSeen,
                          boolean notificationsEnabled, boolean notifyOnline,
                          boolean notifyOffline, boolean notifyRequests) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.status = status;
        this.addPrivacy = addPrivacy;
        this.profilePrivacy = profilePrivacy;
        this.createdAt = createdAt;
        this.lastSeen = lastSeen;
        this.notificationsEnabled = notificationsEnabled;
        this.notifyOnline = notifyOnline;
        this.notifyOffline = notifyOffline;
        this.notifyRequests = notifyRequests;
    }

    public UUID uuid() {
        return uuid;
    }

    public String lastKnownName() {
        return lastKnownName;
    }

    public void lastKnownName(String name) {
        this.lastKnownName = name;
    }

    public SocialStatus status() {
        return status;
    }

    public void status(SocialStatus status) {
        this.status = status;
    }

    public AddPrivacy addPrivacy() {
        return addPrivacy;
    }

    public void addPrivacy(AddPrivacy addPrivacy) {
        this.addPrivacy = addPrivacy;
    }

    public ProfilePrivacy profilePrivacy() {
        return profilePrivacy;
    }

    public void profilePrivacy(ProfilePrivacy profilePrivacy) {
        this.profilePrivacy = profilePrivacy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastSeen() {
        return lastSeen;
    }

    public void lastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean notificationsEnabled() {
        return notificationsEnabled;
    }

    public void notificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public boolean notifyOnline() {
        return notifyOnline;
    }

    public void notifyOnline(boolean notifyOnline) {
        this.notifyOnline = notifyOnline;
    }

    public boolean notifyOffline() {
        return notifyOffline;
    }

    public void notifyOffline(boolean notifyOffline) {
        this.notifyOffline = notifyOffline;
    }

    public boolean notifyRequests() {
        return notifyRequests;
    }

    public void notifyRequests(boolean notifyRequests) {
        this.notifyRequests = notifyRequests;
    }
}
