package fr.nexora.friend.database;

import javax.sql.DataSource;

public interface Database {

    void connect() throws Exception;

    void close();

    DataSource dataSource();
}
