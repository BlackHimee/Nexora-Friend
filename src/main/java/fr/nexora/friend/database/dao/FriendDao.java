package fr.nexora.friend.database.dao;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FriendDao {

    private final DataSource dataSource;
    private final Executor executor;

    public FriendDao(DataSource dataSource, Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    public CompletableFuture<List<UUID>> getFriends(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> friends = new ArrayList<>();
            String sql = "SELECT friend_uuid FROM nf_friends WHERE player_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        friends.add(UUID.fromString(rs.getString(1)));
                    }
                }
            } catch (Exception e) {
                throw new DaoException("Failed to load friends for " + player, e);
            }
            return friends;
        }, executor);
    }

    public CompletableFuture<Integer> countFriends(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM nf_friends WHERE player_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (Exception e) {
                throw new DaoException("Failed to count friends for " + player, e);
            }
        }, executor);
    }

    public CompletableFuture<Boolean> areFriends(UUID a, UUID b) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM nf_friends WHERE player_uuid = ? AND friend_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, a.toString());
                statement.setString(2, b.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next();
                }
            } catch (Exception e) {
                throw new DaoException("Failed to check friendship between " + a + " and " + b, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> addFriendship(UUID a, UUID b) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nf_friends (player_uuid, friend_uuid, created_at) VALUES (?, ?, ?)";
            long now = Instant.now().getEpochSecond();

            try (Connection connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, a.toString());
                    statement.setString(2, b.toString());
                    statement.setLong(3, now);
                    statement.addBatch();

                    statement.setString(1, b.toString());
                    statement.setString(2, a.toString());
                    statement.setLong(3, now);
                    statement.addBatch();

                    statement.executeBatch();
                    connection.commit();
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (Exception e) {
                throw new DaoException("Failed to create friendship between " + a + " and " + b, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> removeFriendship(UUID a, UUID b) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM nf_friends WHERE (player_uuid = ? AND friend_uuid = ?) OR (player_uuid = ? AND friend_uuid = ?)";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, a.toString());
                statement.setString(2, b.toString());
                statement.setString(3, b.toString());
                statement.setString(4, a.toString());
                statement.executeUpdate();
            } catch (Exception e) {
                throw new DaoException("Failed to remove friendship between " + a + " and " + b, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> removeAllFriendships(UUID player) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM nf_friends WHERE player_uuid = ? OR friend_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.toString());
                statement.setString(2, player.toString());
                statement.executeUpdate();
            } catch (Exception e) {
                throw new DaoException("Failed to clear friendships for " + player, e);
            }
        }, executor);
    }
}
