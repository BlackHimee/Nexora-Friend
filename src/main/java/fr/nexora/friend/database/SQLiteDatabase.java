package fr.nexora.friend.database;

import com.zaxxer.hikari.HikariConfig;

import java.io.File;

public class SQLiteDatabase extends AbstractHikariDatabase {

    private final File file;

    public SQLiteDatabase(File file) {
        this.file = file;
    }

    @Override
    protected void configure(HikariConfig config) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        // SQLite only supports a single writer at a time; keep the pool small
        // and let Hikari serialize access instead of hitting SQLITE_BUSY errors.
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setPoolName("NexoraFriend-SQLite");
        config.addDataSourceProperty("foreign_keys", "true");
    }
}
