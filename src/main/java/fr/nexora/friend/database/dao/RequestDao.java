package fr.nexora.friend.database.dao;

import fr.nexora.friend.model.FriendRequest;

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

public class RequestDao {

    private final DataSource dataSource;
    private final Executor executor;

    public RequestDao(DataSource dataSource, Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    public CompletableFuture<Void> createRequest(UUID requester, UUID target) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nf_requests (requester_uuid, target_uuid, created_at) VALUES (?, ?, ?)";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, requester.toString());
                statement.setString(2, target.toString());
                statement.setLong(3, Instant.now().getEpochSecond());
                statement.executeUpdate();
            } catch (Exception e) {
                throw new DaoException("Failed to create request " + requester + " -> " + target, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> deleteRequest(UUID requester, UUID target) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM nf_requests WHERE requester_uuid = ? AND target_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, requester.toString());
                statement.setString(2, target.toString());
                statement.executeUpdate();
            } catch (Exception e) {
                throw new DaoException("Failed to delete request " + requester + " -> " + target, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> deleteAllInvolving(UUID player) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM nf_requests WHERE requester_uuid = ? OR target_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.toString());
                statement.setString(2, player.toString());
                statement.executeUpdate();
            } catch (Exception e) {
                throw new DaoException("Failed to delete requests for " + player, e);
            }
        }, executor);
    }

    public CompletableFuture<Boolean> exists(UUID requester, UUID target) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM nf_requests WHERE requester_uuid = ? AND target_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, requester.toString());
                statement.setString(2, target.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next();
                }
            } catch (Exception e) {
                throw new DaoException("Failed to check request " + requester + " -> " + target, e);
            }
        }, executor);
    }

    public CompletableFuture<List<FriendRequest>> getIncoming(UUID target) {
        return CompletableFuture.supplyAsync(() -> {
            List<FriendRequest> requests = new ArrayList<>();
            String sql = "SELECT requester_uuid, target_uuid, created_at FROM nf_requests WHERE target_uuid = ? ORDER BY created_at DESC";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, target.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        requests.add(map(rs));
                    }
                }
            } catch (Exception e) {
                throw new DaoException("Failed to load incoming requests for " + target, e);
            }
            return requests;
        }, executor);
    }

    public CompletableFuture<List<FriendRequest>> getOutgoing(UUID requester) {
        return CompletableFuture.supplyAsync(() -> {
            List<FriendRequest> requests = new ArrayList<>();
            String sql = "SELECT requester_uuid, target_uuid, created_at FROM nf_requests WHERE requester_uuid = ? ORDER BY created_at DESC";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, requester.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        requests.add(map(rs));
                    }
                }
            } catch (Exception e) {
                throw new DaoException("Failed to load outgoing requests for " + requester, e);
            }
            return requests;
        }, executor);
    }

    public CompletableFuture<Integer> countIncoming(UUID target) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM nf_requests WHERE target_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, target.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (Exception e) {
                throw new DaoException("Failed to count incoming requests for " + target, e);
            }
        }, executor);
    }

    public CompletableFuture<Integer> countOutgoing(UUID requester) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM nf_requests WHERE requester_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, requester.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (Exception e) {
                throw new DaoException("Failed to count outgoing requests for " + requester, e);
            }
        }, executor);
    }

    private FriendRequest map(ResultSet rs) throws Exception {
        return new FriendRequest(
                UUID.fromString(rs.getString("requester_uuid")),
                UUID.fromString(rs.getString("target_uuid")),
                Instant.ofEpochSecond(rs.getLong("created_at"))
        );
    }
}
