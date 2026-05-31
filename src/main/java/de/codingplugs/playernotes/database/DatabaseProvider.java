package de.codingplugs.playernotes.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;

public interface DatabaseProvider {

    void initialize() throws SQLException;

    void shutdown();

    Connection connection() throws SQLException;

    ExecutorService executor();
}
