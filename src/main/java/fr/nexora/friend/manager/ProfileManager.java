package fr.nexora.friend.manager;

import fr.nexora.friend.database.dao.ProfileDao;
import fr.nexora.friend.model.AddPrivacy;
import fr.nexora.friend.model.ProfilePrivacy;
import fr.nexora.friend.model.SocialProfile;
import fr.nexora.friend.model.SocialStatus;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProfileManager {

    private final ProfileDao profileDao;
    private final CacheManager cache;
    private final ConfigManager configManager;

    public ProfileManager(ProfileDao profileDao, CacheManager cache, ConfigManager configManager) {
        this.profileDao = profileDao;
        this.cache = cache;
        this.configManager = configManager;
    }

    public CompletableFuture<SocialProfile> loadOrCreate(UUID uuid, String name) {
        return profileDao.find(uuid).thenCompose(existing -> {
            SocialProfile profile = existing.orElseGet(() -> newDefaultProfile(uuid, name));
            profile.lastKnownName(name);
            profile.status(SocialStatus.ONLINE);
            profile.lastSeen(Instant.now());
            cache.putProfile(profile);
            return profileDao.upsert(profile).thenApply(v -> profile);
        });
    }

    private SocialProfile newDefaultProfile(UUID uuid, String name) {
        AddPrivacy addPrivacy = parseEnum(AddPrivacy.class,
                configManager.config().getString("privacy.default-add-privacy"), AddPrivacy.EVERYONE);
        ProfilePrivacy profilePrivacy = parseEnum(ProfilePrivacy.class,
                configManager.config().getString("privacy.default-profile-privacy"), ProfilePrivacy.EVERYONE);
        Instant now = Instant.now();
        return new SocialProfile(uuid, name, SocialStatus.ONLINE, addPrivacy, profilePrivacy, now, now,
                true, true, true, true);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public void markOffline(UUID uuid) {
        SocialProfile profile = cache.getProfile(uuid);
        if (profile != null) {
            profile.status(SocialStatus.OFFLINE);
            profile.lastSeen(Instant.now());
            profileDao.upsert(profile);
        }
    }

    public SocialProfile getCached(UUID uuid) {
        return cache.getProfile(uuid);
    }

    public CompletableFuture<Optional<SocialProfile>> fetch(UUID uuid) {
        SocialProfile cached = cache.getProfile(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return profileDao.find(uuid);
    }

    public CompletableFuture<Optional<UUID>> resolveByName(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return CompletableFuture.completedFuture(Optional.of(online.getUniqueId()));
        }
        OfflinePlayer cachedOffline = Bukkit.getOfflinePlayerIfCached(name);
        if (cachedOffline != null && cachedOffline.hasPlayedBefore()) {
            return CompletableFuture.completedFuture(Optional.of(cachedOffline.getUniqueId()));
        }
        return profileDao.findUuidByName(name);
    }

    public void save(SocialProfile profile) {
        profileDao.upsert(profile);
    }
}
