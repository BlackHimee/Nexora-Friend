package fr.nexora.friend.database.dao;

import fr.nexora.friend.model.NetworkEvent;
import fr.nexora.friend.model.NetworkEventType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * MySQL/MariaDB-only event queue used to propagate friend/request/block/presence
 * changes across every server sharing the same network database. Each server
 * polls {@link #fetchAfter} on an interval and ignores rows it produced itself.
 */
public class NetworkEventDao {

    private final DataSource dataSource;
    private final Executor executor;

    public NetworkEventDao(DataSource dataSource, Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor;
    }

    public CompletableFuture<Void> publish(NetworkEventType type, String originServer, UUID playerA, UUID playerB) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO nf_network_events (event_type, origin_server, player_a, player_b, created_at) VALUES (?, ?, ?, ?, ?)";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, type.name());
                statement.setString(2, originServer);
                statement.setString(3, playerA.toString());
                statement.setString(4, playerB != null ? playerB.toString() : null);
                statement.setLong(5, Instant.now().toEpochMilli());
                statement.executeUpdate();
            } catch (Exception e) {
                throw new DaoException("Failed to publish network event " + type, e);
            }
        }, executor);
    }

    public CompletableFuture<Long> maxId() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT MAX(id) FROM nf_network_events";
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery(sql)) {
                if (rs.next()) {
                    long value = rs.getLong(1);
                    return rs.wasNull() ? 0L : value;
                }
                return 0L;
            } catch (Exception e) {
                throw new DaoException("Failed to read latest network event id", e);
            }
        }, executor);
    }

    public CompletableFuture<List<NetworkEvent>> fetchAfter(long lastId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<NetworkEvent> events = new ArrayList<>();
            String sql = "SELECT id, event_type, origin_server, player_a, player_b, created_at "
                    + "FROM nf_network_events WHERE id > ? ORDER BY id ASC LIMIT ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, lastId);
                statement.setInt(2, limit);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        String playerBRaw = rs.getString("player_b");
                        events.add(new NetworkEvent(
                                rs.getLong("id"),
                                NetworkEventType.valueOf(rs.getString("event_type")),
                                rs.getString("origin_server"),
                                UUID.fromString(rs.getString("player_a")),
                                playerBRaw != null ? UUID.fromString(playerBRaw) : null,
                                rs.getLong("created_at")
                        ));
                    }
                }
            } catch (Exception e) {
                throw new DaoException("Failed to fetch network events after " + lastId, e);
            }
            return events;
        }, executor);
    }

    public CompletableFuture<Void> purgeOlderThan(long epochMillis) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM nf_network_events WHERE created_at < ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, epochMillis);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new DaoException("Failed to purge old network events", e);
            }
        }, executor);
    }
}
