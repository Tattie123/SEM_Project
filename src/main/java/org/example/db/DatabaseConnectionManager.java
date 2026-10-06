package org.example.db;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionManager {

    private static final Dotenv dotenv = Dotenv.load();

    public static Connection connect() throws SQLException {

        String host = getRequiredConfiguration("MYSQL_HOST");
        String port = getRequiredConfiguration("MYSQL_PORT");
        String database = getRequiredConfiguration("MYSQL_DATABASE");
        String username = getRequiredConfiguration("MYSQL_USER");
        String password = getRequiredConfiguration("MYSQL_PASSWORD");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        return DriverManager.getConnection(url, username, password);
    }

    private static String getRequiredConfiguration(String name) {

        String value = dotenv.get(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required configuration '" + name + "' is not set."
            );
        }

        return value;
    }
}