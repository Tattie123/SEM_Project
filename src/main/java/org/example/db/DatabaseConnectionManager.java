package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens connections to the world database
 */
public class DatabaseConnectionManager {

    /**
     * Opens a database connection using environment settings
     * <br>
     * Uses local defaults when DB_URL, DB_USER or DB_PASSWORD is missing
     * @return an open connection for the caller to close
     * @throws SQLException if the connection fails
     */
    public static Connection connect() throws SQLException {

        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/world");
        String user = System.getenv().getOrDefault("DB_USER", "software_app");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "MyAppPassword2026!");
        return DriverManager.getConnection(url, user, password);
    }
}
