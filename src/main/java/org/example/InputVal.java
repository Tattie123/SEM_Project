package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

// Input validation: read and check what the user types.
// Each method gives 3 tries and returns null if the user fails.
public class InputVal {

    private static final int TRIES = 3;

    // Reads text that is not empty
    public static String readText(Scanner scanner, String prompt) {
        for (int i = 0; i < TRIES; i++) {
            System.out.print(prompt);
            String text = scanner.nextLine().trim();

            if (text.isEmpty()) {
                System.out.println("Input cannot be empty.");
            } else if (text.length() > 100) {
                System.out.println("Input is too long.");
            } else {
                return text;
            }
        }
        return null;
    }

    // Reads a whole number greater than 0
    public static Integer readPositiveInt(Scanner scanner, String prompt) {
        for (int i = 0; i < TRIES; i++) {
            System.out.print(prompt);
            String text = scanner.nextLine().trim();

            try {
                int number = Integer.parseInt(text);
                if (number > 0) {
                    return number;
                }
                System.out.println("Enter a number greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number.");
            }
        }
        return null;
    }

    public static String readContinent(Scanner scanner, Connection connection) {
        return readExisting(scanner, connection, "continent",
                "SELECT Continent FROM country WHERE Continent = ? LIMIT 1");
    }

    public static String readRegion(Scanner scanner, Connection connection) {
        return readExisting(scanner, connection, "region",
                "SELECT Region FROM country WHERE Region = ? LIMIT 1");
    }

    public static String readCountry(Scanner scanner, Connection connection) {
        return readExisting(scanner, connection, "country",
                "SELECT Name FROM country WHERE Name = ? LIMIT 1");
    }

    public static String readDistrict(Scanner scanner, Connection connection) {
        return readExisting(scanner, connection, "district",
                "SELECT District FROM city WHERE District = ? LIMIT 1");
    }

    // Asks for a name and checks it is in the database
    private static String readExisting(Scanner scanner, Connection connection,
                                       String label, String sql) {
        for (int i = 0; i < TRIES; i++) {
            System.out.print("Enter a " + label + ": ");
            String text = scanner.nextLine().trim();
            if (text.isEmpty()) {
                System.out.println("Input cannot be empty.");
                continue;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, text);
                try (ResultSet results = statement.executeQuery()) {
                    if (results.next()) {
                        return results.getString(1);
                    }
                }
                System.out.println("No " + label + " called \"" + text + "\" found.");
            } catch (SQLException e) {
                ErrorHandler.handleSQLException(e, "checking the " + label);
                return null;
            }
        }
        return null;
    }
}
