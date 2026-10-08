package org.example;

import org.example.db.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    static void main() {

        //connect to database and get countries to check we can get data
        try (Connection connection = DatabaseConnectionManager.connect();
             Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT COUNT(*) FROM country")) {

            if (results.next()) {
                System.out.println("Number of countries: " + results.getInt(1));
            }
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
        }


    }
}
