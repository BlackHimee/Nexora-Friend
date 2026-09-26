package fr.nexora.friend.database;

import fr.nexora.friend.NexoraFriend;
import fr.nexora.friend.database.dao.BlockDao;
import fr.nexora.friend.database.dao.FriendDao;
import fr.nexora.friend.database.dao.NetworkEventDao;
import fr.nexora.friend.database.dao.ProfileDao;
import fr.nexora.friend.database.dao.RequestDao;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseManager {

    private static final List<String> SCHEMA_STATEMENTS = List.of(
            """
            CREATE TABLE IF NOT EXISTS nf_profiles (
                uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                last_known_name VARCHAR(16) NOT NULL,
                social_status VARCHAR(16) NOT NULL DEFAULT 'ONLINE',
                add_privacy VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
                profile_privacy VARCHAR(20) NOT NULL DEFAULT 'EVERYONE',
                created_at BIGINT NOT NULL,
                last_seen BIGINT NOT NULL,
                notifications_enabled INTEGER NOT NULL DEFAULT 1,
                notify_online INTEGER NOT NULL DEFAULT 1,
                notify_offline INTEGER NOT NULL DEFAULT 1,
                notify_requests INTEGER NOT NULL DEFAULT 1
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS nf_friends (
                player_uuid VARCHAR(36) NOT NULL,
                friend_uuid VARCHAR(36) NOT NULL,
                created_at BIGINT NOT NULL,
                PRIMARY KEY (player_uuid, friend_uuid)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS nf_requests (
                requester_uuid VARCHAR(36) NOT NULL,
                target_uuid VARCHAR(36) NOT NULL,
                created_at BIGINT NOT NULL,
                PRIMARY KEY (requester_uuid, target_uuid)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS nf_blocks (
                player_uuid VARCHAR(36) NOT NULL,
                blocked_uuid VARCHAR(36) NOT NULL,
                created_at BIGINT NOT NULL,
                PRIMARY KEY (player_uuid, blocked_uuid)
            )
            """
    );

    private static final List<String> INDEX_STATEMENTS = List.of(
            "CREATE INDEX idx_nf_friends_player ON nf_friends (player_uuid)",
            "CREATE INDEX idx_nf_requests_target ON nf_requests (target_uuid)",
            "CREATE INDEX idx_nf_requests_requester ON nf_requests (requester_uuid)",
            "CREATE INDEX idx_nf_blocks_player ON nf_blocks (player_uuid)",
            "CREATE INDEX idx_nf_profiles_name ON nf_profiles (last_known_name)"
    );

    private final NexoraFriend plugin;
    private Database database;
    private ExecutorService executor;
    private boolean mysql;

    private FriendDao friendDao;
    private RequestDao requestDao;
    private BlockDao blockDao;
    private ProfileDao profileDao;
    private NetworkEventDao networkEventDao;

    public DatabaseManager(NexoraFriend plugin) {
        this.plugin = plugin;
    }

    public void init() throws Exception {
        FileConfiguration config = plugin.configManager().config();
        String type = config.getString("database.type", "SQLITE").trim().toUpperCase();
        this.mysql = type.equals("MYSQL") || type.equals("MARIADB");

        this.database = mysql ? buildMySQL(config) : buildSQLite(config);

        database.connect();
        createSchema();

        AtomicInteger threadCount = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "NexoraFriend-DB-" + threadCount.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newFixedThreadPool(4, factory);

        this.friendDao = new FriendDao(database.dataSource(), executor);
        this.requestDao = new RequestDao(database.dataSource(), executor);
        this.blockDao = new BlockDao(database.dataSource(), executor);
        this.profileDao = new ProfileDao(database.dataSource(), executor);
        this.networkEventDao = new NetworkEventDao(database.dataSource(), executor);

        plugin.getLogger().info("Database connection established (" + type + ").");
    }

    /** Cross-server sync only makes sense against a shared network database. */
    public boolean supportsNetworkSync() {
        return mysql;
    }

    private Database buildSQLite(FileConfiguration config) {
        String fileName = config.getString("database.sqlite.file", "database.db");
        return new SQLiteDatabase(new File(plugin.getDataFolder(), fileName));
    }

    private Database buildMySQL(FileConfiguration config) {
        return new MySQLDatabase(
                config.getString("database.mysql.host", "localhost"),
                config.getInt("database.mysql.port", 3306),
                config.getString("database.mysql.database", "nexora_friend"),
                config.getString("database.mysql.username", "root"),
                config.getString("database.mysql.password", ""),
                config.getBoolean("database.mysql.useSSL", false),
                config.getInt("database.mysql.pool.maximum-pool-size", 10),
                config.getInt("database.mysql.pool.minimum-idle", 2),
                config.getLong("database.mysql.pool.connection-timeout-ms", 10000),
                config.getLong("database.mysql.pool.idle-timeout-ms", 600000),
                config.getLong("database.mysql.pool.max-lifetime-ms", 1800000)
        );
    }

    private void createSchema() throws SQLException {
        try (Connection connection = database.dataSource().getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : SCHEMA_STATEMENTS) {
                statement.execute(sql);
            }
            for (String sql : INDEX_STATEMENTS) {
                try {
                    statement.execute(sql);
                } catch (SQLException ignored) {
                    // Index already exists - not all supported databases accept "IF NOT EXISTS" here.
                }
            }

            if (mysql) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS nf_network_events (
                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            event_type VARCHAR(32) NOT NULL,
                            origin_server VARCHAR(64) NOT NULL,
                            player_a VARCHAR(36) NOT NULL,
                            player_b VARCHAR(36) NULL,
                            created_at BIGINT NOT NULL
                        )
                        """);
                try {
                    statement.execute("CREATE INDEX idx_nf_network_events_created ON nf_network_events (created_at)");
                } catch (SQLException ignored) {
                    // Index already exists.
                }
            }
        }
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }

    public FriendDao friendDao() {
        return friendDao;
    }

    public RequestDao requestDao() {
        return requestDao;
    }

    public BlockDao blockDao() {
        return blockDao;
    }

    public ProfileDao profileDao() {
        return profileDao;
    }

    public NetworkEventDao networkEventDao() {
        return networkEventDao;
    }

    public ExecutorService executor() {
        return executor;
    }
}
