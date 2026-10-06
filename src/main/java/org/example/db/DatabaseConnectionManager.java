package org.example.db;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionManager {

    public static Connection connect() throws SQLException {

        String url = "jdbc:mysql://localhost:3306/software_engineering_methods";

        return DriverManager.getConnection(url, "software_app", "MyAppPassword2026!");
    }
}