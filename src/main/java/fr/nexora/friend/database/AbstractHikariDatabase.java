package fr.nexora.friend.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public abstract class AbstractHikariDatabase implements Database {

    protected HikariDataSource dataSource;

    protected abstract void configure(HikariConfig config);

    @Override
    public void connect() {
        HikariConfig config = new HikariConfig();
        configure(config);
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public DataSource dataSource() {
        return dataSource;
    }
}
