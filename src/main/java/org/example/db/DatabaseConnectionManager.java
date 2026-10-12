package org.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionManager {

    public static Connection connect() throws SQLException {

        String url = System.getenv().getOrDefault("DB_URL",
                "jdbc:mysql://localhost:3306/world");
        String user = System.getenv().getOrDefault("DB_USER", "software_app");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "MyAppPassword2026!");
        return DriverManager.getConnection(url, user, password);
    }
}
