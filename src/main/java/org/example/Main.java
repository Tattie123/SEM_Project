package org.example;

import org.example.db.DatabaseConnectionManager;

import java.sql.Connection;

public class Main {
    static void main() {

        try (Connection connection = DatabaseConnectionManager.connect()) {
            System.out.println("Successfully connected to MySQL!");
        } catch (Exception e) {

            e.printStackTrace();
        }

    }
}
