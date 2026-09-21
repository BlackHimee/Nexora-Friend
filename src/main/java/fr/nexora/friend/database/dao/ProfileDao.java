package fr.nexora.friend.database.dao;

import fr.nexora.friend.model.AddPrivacy;
import fr.nexora.friend.model.ProfilePrivacy;
import fr.nexora.friend.model.SocialProfile;
import fr.nexora.friend.model.SocialStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ProfileDao {

    private final DataSource dataSource;
    private final Executor executor;

    public ProfileDao(DataSource dataSource, Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    public CompletableFuture<Optional<SocialProfile>> find(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM nf_profiles WHERE uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(map(rs));
                    }
                    return Optional.<SocialProfile>empty();
                }
            } catch (Exception e) {
                throw new DaoException("Failed to load profile for " + uuid, e);
            }
        }, executor);
    }

    public CompletableFuture<Optional<UUID>> findUuidByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT uuid FROM nf_profiles WHERE LOWER(last_known_name) = LOWER(?)";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(UUID.fromString(rs.getString(1)));
                    }
                    return Optional.<UUID>empty();
                }
            } catch (Exception e) {
                throw new DaoException("Failed to resolve uuid for name " + name, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> upsert(SocialProfile profile) {
        return CompletableFuture.runAsync(() -> {
            String selectSql = "SELECT 1 FROM nf_profiles WHERE uuid = ?";
            String updateSql = """
                    UPDATE nf_profiles SET last_known_name = ?, social_status = ?, add_privacy = ?,
                    profile_privacy = ?, last_seen = ?, notifications_enabled = ?, notify_online = ?,
                    notify_offline = ?, notify_requests = ? WHERE uuid = ?
                    """;
            String insertSql = """
                    INSERT INTO nf_profiles (uuid, last_known_name, social_status, add_privacy, profile_privacy,
                    created_at, last_seen, notifications_enabled, notify_online, notify_offline, notify_requests)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (Connection connection = dataSource.getConnection()) {
                boolean exists;
                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setString(1, profile.uuid().toString());
                    try (ResultSet rs = select.executeQuery()) {
                        exists = rs.next();
                    }
                }

                if (exists) {
                    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                        update.setString(1, profile.lastKnownName());
                        update.setString(2, profile.status().name());
                        update.setString(3, profile.addPrivacy().name());
                        update.setString(4, profile.profilePrivacy().name());
                        update.setLong(5, profile.lastSeen().getEpochSecond());
                        update.setInt(6, profile.notificationsEnabled() ? 1 : 0);
                        update.setInt(7, profile.notifyOnline() ? 1 : 0);
                        update.setInt(8, profile.notifyOffline() ? 1 : 0);
                        update.setInt(9, profile.notifyRequests() ? 1 : 0);
                        update.setString(10, profile.uuid().toString());
                        update.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                        insert.setString(1, profile.uuid().toString());
                        insert.setString(2, profile.lastKnownName());
                        insert.setString(3, profile.status().name());
                        insert.setString(4, profile.addPrivacy().name());
                        insert.setString(5, profile.profilePrivacy().name());
                        insert.setLong(6, profile.createdAt().getEpochSecond());
                        insert.setLong(7, profile.lastSeen().getEpochSecond());
                        insert.setInt(8, profile.notificationsEnabled() ? 1 : 0);
                        insert.setInt(9, profile.notifyOnline() ? 1 : 0);
                        insert.setInt(10, profile.notifyOffline() ? 1 : 0);
                        insert.setInt(11, profile.notifyRequests() ? 1 : 0);
                        insert.executeUpdate();
                    }
                }
            } catch (Exception e) {
                throw new DaoException("Failed to save profile for " + profile.uuid(), e);
            }
        }, executor);
    }

    private SocialProfile map(ResultSet rs) throws Exception {
        return new SocialProfile(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("last_known_name"),
                SocialStatus.valueOf(rs.getString("social_status")),
                AddPrivacy.valueOf(rs.getString("add_privacy")),
                ProfilePrivacy.valueOf(rs.getString("profile_privacy")),
                Instant.ofEpochSecond(rs.getLong("created_at")),
                Instant.ofEpochSecond(rs.getLong("last_seen")),
                rs.getInt("notifications_enabled") != 0,
                rs.getInt("notify_online") != 0,
                rs.getInt("notify_offline") != 0,
                rs.getInt("notify_requests") != 0
        );
    }
}
