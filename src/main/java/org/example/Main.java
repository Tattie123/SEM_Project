package org.example;

import org.example.db.DatabaseConnectionManager;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--all-cities"))) {
            System.err.println("Usage: java -jar app.jar [--all-cities]");
            System.exit(2);
        }
        try (Connection connection = DatabaseConnectionManager.connect()) {
            int status = runReports(connection, new Scanner(System.in), System.out,
                    args.length == 1);
            if (status != 0) {
                System.exit(status);
            }
        } catch (SQLException e) {
            System.err.println("Unable to run reports: " + e.getMessage());
            System.exit(1);
        }
    }

    public static int runReports(Connection connection, Scanner scanner,
                                 java.io.PrintStream output, boolean citiesOnly)
            throws SQLException {
        printAllCitiesWorldwide(connection, output);
        if (citiesOnly) {
            return 0;
        }
        output.printf("World population: %d%n", getWorldPopulation(connection));
        output.print("Enter a continent: ");
        if (!scanner.hasNextLine()) {
            output.println("Please enter a continent.");
            return 2;
        }
        String continent = scanner.nextLine().trim();
        if (continent.isEmpty()) {
            output.println("Please enter a continent.");
            return 2;
        }
        output.printf("Population of %s: %d%n",
                continent, getContinentPopulation(connection, continent));
        List<City> cities = getCitiesByContinent(connection, continent);
        if (cities.isEmpty()) {
            output.println("No cities found for continent: " + continent);
        } else {
            output.println("Cities in " + continent + " (population descending):");
            output.println("Name | Country | District | Population");
            for (City city : cities) {
                output.printf("%s | %s | %s | %d%n",
                        city.name, city.country, city.district, city.population);
            }
        }
        output.print("Enter a region: ");
        if (!scanner.hasNextLine()) {
            output.println("Please enter a region.");
            return 2;
        }
        String region = scanner.nextLine().trim();
        if (region.isEmpty()) {
            output.println("Please enter a region.");
            return 2;
        }
        output.print("Enter the number of countries: ");
        if (!scanner.hasNextInt()) {
            output.println("Please enter a whole number.");
            return 2;
        }
        int n = scanner.nextInt();
        if (n <= 0) {
            output.println("Please enter a number greater than zero.");
            return 2;
        }
        printTopCountriesByRegion(connection, region, n, output);
        double[] chinese = getChineseSpeakers(connection);
        double[] english = getEnglishSpeakers(connection);
        output.printf("Chinese: %.0f speakers | %.2f%% of world population%n",
                chinese[0], chinese[1]);
        output.printf("English: %.0f speakers | %.2f%% of world population%n",
                english[0], english[1]);
        return 0;
    }

    public static void printAllCitiesWorldwide(Connection connection,
                                               java.io.PrintStream output)
            throws SQLException {
        List<City> cities = getAllCitiesWorldwide(connection);
        output.println("All cities worldwide");
        output.println("Name | Country | District | Population");
        if (cities.isEmpty()) {
            output.println("No cities found.");
        }
        for (City city : cities) {
            output.printf("%s | %s | %s | %d%n",
                    city.name, city.country, city.district, city.population);
        }
    }
    public static long getWorldPopulation(Connection connection)
            throws SQLException {

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

    public static long getContinentPopulation(
            Connection connection, String continent) throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(Population), 0) AS ContinentPopulation
                FROM country
                WHERE Continent = ?
                """;

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
                ORDER BY city.Population DESC, city.ID
                """;

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

    public static List<City> getTopCitiesByDistrict(
            Connection connection, String district, int n) throws SQLException {

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
                ORDER BY city.Population DESC, city.ID
                LIMIT ?
                """;

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


    public static List<City> getCitiesByContinent(
            Connection connection, String continent) throws SQLException {

        List<City> cities = new ArrayList<>();

        String sql = """
            SELECT city.Name,
                   country.Name AS Country,
                   city.District,
                   city.Population
            FROM city
            JOIN country ON city.CountryCode = country.Code
            WHERE country.Continent = ?
            ORDER BY city.Population DESC, city.ID
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, continent);

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

    public static double[] getChineseSpeakers(Connection connection)
            throws SQLException {

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

    public static double[] getEnglishSpeakers(Connection connection)
            throws SQLException {

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

    // To show the top N countries in a selected region
    public static void printTopCountriesByRegion(
            Connection connection, String region, int n) throws SQLException {

        printTopCountriesByRegion(connection, region, n, System.out);
    }

    public static void printTopCountriesByRegion(
            Connection connection, String region, int n, java.io.PrintStream output)
            throws SQLException {
        //stop if the number is 0 or negative
        if (n <= 0) {
            output.println("enter a number greater than 0: ");
            return;
        }

        //retrieves the information from the database, ordered by population
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
                    //user choice
                    statement.setString(1, region);
                    statement.setInt(2, n);

                    //run query / read the results
                    try(ResultSet results = statement.executeQuery()) {
                        output.println("Top " + n + " countries in " + region);
                        output.println("Code | Name | Continent | Region | Population | Capital");

                        //ensures query contains a country
                        boolean found = false;

                        //row for each country
                        while (results.next()) {
                            found = true;

                            output.println(
                                    results.getString("Code") + " | " +
                                    results.getString("Name") + " | " +
                                    results.getString("Continent") + " | " +
                                    results.getString("Region") + " | " +
                                    results.getString("Population") + " | " +
                                    results.getString("Capital"));
                        }

                        //error message if no countries match
                        if (!found) {
                            output.println("no countries found for this region");
                        }

                    }
                }
            }
}
