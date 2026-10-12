package org.example;

import org.example.db.DatabaseConnectionManager;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Runs the population reports against the configured World database. */
public class Main {
    public static void main(String[] args) {
        // Keep one connection open while the interactive reports are generated.
        try (Connection connection = DatabaseConnectionManager.connect()) {
            System.out.printf("World population: %d%n", getWorldPopulation(connection));

            Scanner scanner = new Scanner(System.in);

            // Read the continent report selection.
            System.out.print("Enter a continent: ");
            String continent = scanner.nextLine().trim();
            System.out.printf("Population of %s: %d%n",
                    continent, getContinentPopulation(connection, continent));

            // Read and validate the inputs for the regional country report.
            System.out.print("Enter a region: ");
            String region = scanner.nextLine().trim();

            System.out.print("Enter the number of countries: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a whole number.");
                return;
            }

            int n = scanner.nextInt();

            // Print the selected region's countries and worldwide language totals.
            printTopCountriesByRegion(connection, region, n);
            double[] chinese = getChineseSpeakers(connection);
            double[] english = getEnglishSpeakers(connection);

            System.out.printf(
                    "Chinese: %.0f speakers | %.2f%% of world population%n",
                    chinese[0], chinese[1]);

            System.out.printf(
                    "English: %.0f speakers | %.2f%% of world population%n",
                    english[0], english[1]);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Returns the sum of the population values for all countries. */
    public static long getWorldPopulation(Connection connection)
            throws SQLException {

        // COALESCE returns zero if the table has no population values.
        String sql = """
                SELECT COALESCE(SUM(Population), 0) AS WorldPopulation
                FROM country
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getLong("WorldPopulation");
            }
        }

        return 0L;
    }

    /** Returns the population total for the selected continent. */
    public static long getContinentPopulation(
            Connection connection, String continent) throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(Population), 0) AS ContinentPopulation
                FROM country
                WHERE Continent = ?
                """;

        // Bind the user's selection rather than inserting it into the SQL text.
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, continent);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("ContinentPopulation");
                }
            }
        }

        return 0L;
    }

    /** Loads every city with its country name, ordered from largest to smallest. */
    public static List<City> getAllCitiesWorldwide(Connection connection)
            throws SQLException {

        List<City> cities = new ArrayList<>();

        String sql = """
                SELECT city.Name,
                       country.Name AS Country,
                       city.District,
                       city.Population
                FROM city
                JOIN country ON city.CountryCode = country.Code
                ORDER BY city.Population DESC
                """;

        // Map each result row to a City object; try-with-resources closes JDBC objects.
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                City city = new City();

                city.name = resultSet.getString("Name");
                city.country = resultSet.getString("Country");
                city.district = resultSet.getString("District");
                city.population = resultSet.getInt("Population");

                cities.add(city);
            }
        }

        return cities;
    }

    /** Returns at most {@code n} cities in the requested district. */
    public static List<City> getTopCitiesByDistrict(
            Connection connection, String district, int n) throws SQLException {

        // Reject invalid limits before sending a query to the database.
        if (n <= 0) {
            throw new IllegalArgumentException("N must be greater than zero.");
        }

        List<City> cities = new ArrayList<>();

        String sql = """
                SELECT city.Name,
                       country.Name AS Country,
                       city.District,
                       city.Population
                FROM city
                JOIN country ON city.CountryCode = country.Code
                WHERE city.District = ?
                ORDER BY city.Population DESC
                LIMIT ?
                """;

        // Both the district filter and result limit are supplied as parameters.
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, district);
            statement.setInt(2, n);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    City city = new City();

                    city.name = resultSet.getString("Name");
                    city.country = resultSet.getString("Country");
                    city.district = resultSet.getString("District");
                    city.population = resultSet.getInt("Population");

                    cities.add(city);
                }
            }
        }

        return cities;
    }

    /** Returns estimated Chinese speakers and their percentage of world population. */
    public static double[] getChineseSpeakers(Connection connection)
            throws SQLException {

        // Weight each country's population by the language's reported percentage.
        String sql = """
                SELECT
                    COALESCE(SUM(country.Population *
                        (countrylanguage.Percentage / 100.0)), 0) AS Speakers,
                    (COALESCE(SUM(country.Population *
                        (countrylanguage.Percentage / 100.0)), 0)
                     / NULLIF((SELECT SUM(Population) FROM country), 0))
                     * 100.0 AS WorldPercentage
                FROM countrylanguage
                JOIN country ON countrylanguage.CountryCode = country.Code
                WHERE countrylanguage.Language = 'Chinese'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                double speakers = resultSet.getDouble("Speakers");
                double percentage = resultSet.getDouble("WorldPercentage");

                return new double[]{speakers, percentage};
            }
        }

        return new double[]{0, 0};
    }

    /** Returns estimated English speakers and their percentage of world population. */
    public static double[] getEnglishSpeakers(Connection connection)
            throws SQLException {

        // The subquery denominator is the population of all countries, not just
        // countries where English is spoken.
        String sql = """
                SELECT
                    COALESCE(SUM(country.Population *
                        (countrylanguage.Percentage / 100.0)), 0) AS Speakers,
                    (COALESCE(SUM(country.Population *
                        (countrylanguage.Percentage / 100.0)), 0)
                     / NULLIF((SELECT SUM(Population) FROM country), 0))
                     * 100.0 AS WorldPercentage
                FROM countrylanguage
                JOIN country ON countrylanguage.CountryCode = country.Code
                WHERE countrylanguage.Language = 'English'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                double speakers = resultSet.getDouble("Speakers");
                double percentage = resultSet.getDouble("WorldPercentage");

                return new double[]{speakers, percentage};
            }

        }

        return new double[]{0, 0};
    }

    /** Prints up to {@code n} countries in a region, ordered by population. */
    public static void printTopCountriesByRegion(
            Connection connection, String region, int n) throws SQLException {

        // A non-positive limit cannot produce a meaningful top-N report.
        if (n <= 0) {
            System.out.println("enter a number greater than 0: ");
            return;
        }

        // LEFT JOIN keeps countries in the report even when no capital is listed.
        String sql = """
                SELECT country.Code, country.Name, country.Continent,
                            country.region, country.population,
                            city.Name AS Capital
                        FROM country
                        LEFT JOIN city ON country.Capital = city.ID
                        WHERE country.Region = ?
                        ORDER BY country.Population DESC, country.Code
                        LIMIT ?
                """;
                try(PreparedStatement statement = connection.prepareStatement(sql)) {
                    // Bind the chosen region and maximum number of rows.
                    statement.setString(1, region);
                    statement.setInt(2, n);

                    try(ResultSet results = statement.executeQuery()) {
                        System.out.println("Top " + n + " countries in " + region);
                        System.out.println("Code | Name | Continent | Region | Population | Capital");

                        // Track whether the query returned a matching country.
                        boolean found = false;

                        while (results.next()) {
                            found = true;

                            System.out.println(
                                    results.getString("Code") + " | " +
                                    results.getString("Name") + " | " +
                                    results.getString("Continent") + " | " +
                                    results.getString("Region") + " | " +
                                    results.getString("Population") + " | " +
                                    results.getString("Capital"));
                        }

                        // Give a clear message for a region with no matching rows.
                        if (!found) {
                            System.out.println("no countries found for this region");
                        }

                    }
                }
            }
}
