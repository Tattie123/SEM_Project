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
        try (Connection connection = DatabaseConnectionManager.connect()) {
            // Ask the user for the region and number of countries.
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter a region: ");
            String region = scanner.nextLine().trim();

            System.out.print("Enter the number of countries: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Please enter a whole number.");
                return;
            }

            int n = scanner.nextInt();

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
                ORDER BY city.Population DESC
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

        //stop if the number is 0 or negative
        if (n <= 0) {
            System.out.println("enter a number greater than 0: ");
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
                        System.out.println("Top " + n + " countries in " + region);
                        System.out.println("Code | Name | Continent | Region | Population | Capital");

                        //ensures query contains a country
                        boolean found = false;

                        //row for each country
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

                        //error message if no countries match
                        if (!found) {
                            System.out.println("no countries found for this region");
                        }

                    }
                }
            }
}
