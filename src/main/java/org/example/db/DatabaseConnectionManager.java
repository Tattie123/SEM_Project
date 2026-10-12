package org.example.db;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Creates JDBC connections for the application's configured MySQL database. */
public class DatabaseConnectionManager {

    /** Opens a connection to the local World database. */
    public static Connection connect() throws SQLException {

        // Keep the database name in the URL so all queries use the same schema.
        String url = "jdbc:mysql://localhost:3306/world";

        return DriverManager.getConnection(url, "software_app", "MyAppPassword2026!");
    }
}