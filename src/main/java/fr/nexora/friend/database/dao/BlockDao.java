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

public class BlockDao {

    private final DataSource dataSource;
    private final Executor executor;

    public BlockDao(DataSource dataSource, Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    public CompletableFuture<Void> block(UUID player, UUID blocked) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nf_blocks (player_uuid, blocked_uuid, created_at) VALUES (?, ?, ?)";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.toString());
                statement.setString(2, blocked.toString());
                statement.setLong(3, Instant.now().getEpochSecond());
                statement.executeUpdate();
            } catch (Exception e) {
                throw new DaoException("Failed to block " + blocked + " for " + player, e);
            }
        }, executor);
    }

    public CompletableFuture<Void> unblock(UUID player, UUID blocked) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM nf_blocks WHERE player_uuid = ? AND blocked_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.toString());
                statement.setString(2, blocked.toString());
                statement.executeUpdate();
            } catch (Exception e) {
                throw new DaoException("Failed to unblock " + blocked + " for " + player, e);
            }
        }, executor);
    }

    public CompletableFuture<Boolean> isBlocked(UUID player, UUID blocked) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM nf_blocks WHERE player_uuid = ? AND blocked_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.toString());
                statement.setString(2, blocked.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next();
                }
            } catch (Exception e) {
                throw new DaoException("Failed to check block status " + player + " -> " + blocked, e);
            }
        }, executor);
    }

    public CompletableFuture<List<UUID>> getBlocked(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> blocked = new ArrayList<>();
            String sql = "SELECT blocked_uuid FROM nf_blocks WHERE player_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        blocked.add(UUID.fromString(rs.getString(1)));
                    }
                }
            } catch (Exception e) {
                throw new DaoException("Failed to load blocked players for " + player, e);
            }
            return blocked;
        }, executor);
    }
}
