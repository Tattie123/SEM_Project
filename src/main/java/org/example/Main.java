package org.example;

import org.example.db.DatabaseConnectionManager;

import java.io.PrintStream;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Runs the population reports against the configured World database. */
public class Main {

    /**
     * Checks args and connects to the database
     * <br>
     * Starts {@link #runReports(Connection, Scanner, PrintStream, boolean)}
     * @param args optional --all-cities flag
     */
    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--all-cities"))) {
            System.err.println("Usage: java -jar app.jar [--all-cities]");
            System.exit(2);
        }
        // Keep one connection open while the interactive reports are generated.
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

    /**
     * Prints reports using the user's choices
     * <br>
     * Stops after the city report when citiesOnly is true
     * @param connection open database connection
     * @param scanner user input
     * @param output report output
     * @param citiesOnly whether to print only the worldwide city report
     * @return 0 on success, or 2 for missing or invalid input
     * @throws SQLException if a database query fails
     */
    public static int runReports(Connection connection, Scanner scanner,
                                 java.io.PrintStream output, boolean citiesOnly)
            throws SQLException {
        printAllCitiesWorldwide(connection, output);
        if (citiesOnly) {
            return 0;
        }
        output.printf("World population: %d%n", getWorldPopulation(connection));
        // Read the continent report selection.
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
        // Read and validate the inputs for the regional country report.
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
        scanner.nextLine();
        if (n <= 0) {
            output.println("Please enter a number greater than zero.");
            return 2;
        }
        // Print the selected region's countries and worldwide language totals.
        printTopCountriesByRegion(connection, region, n, output);
        double[] chinese = getChineseSpeakers(connection);
        double[] english = getEnglishSpeakers(connection);
        output.printf("Chinese: %.0f speakers | %.2f%% of world population%n",
                chinese[0], chinese[1]);
        output.printf("English: %.0f speakers | %.2f%% of world population%n",
                english[0], english[1]);
        output.print("Would you like to see all capital cities? (yes/no): ");
        if (!scanner.hasNextLine()) {
            output.println("Please enter yes or no.");
            return 2;
        }

        //Shows all the capital cities in the world
        String showCapitals = scanner.nextLine().trim();
        if (showCapitals.equalsIgnoreCase("yes") || showCapitals.equalsIgnoreCase("y")) {
            allCapitals(connection, output);
        } else if (!showCapitals.equalsIgnoreCase("no")
                && !showCapitals.equalsIgnoreCase("n")) {
            output.println("Please answer yes or no!");
            return 2;
        }

        // Shows the capitals by the continent selected
        output.print("\n\nWould you like to see capital cities by continent? (yes/no): ");
        if (!scanner.hasNextLine()) {
            output.println("Please enter yes or no.");
            return 2;
        }
        String showCapitalsWithContinents = scanner.nextLine().trim();
        if (showCapitalsWithContinents.equalsIgnoreCase("yes")
                || showCapitalsWithContinents.equalsIgnoreCase("y")) {
            output.print("Enter a continent for the capital report: ");
            if (!scanner.hasNextLine()) {
                output.println("Please enter a continent.");
                return 2;
            }
            String capitalContinent = scanner.nextLine().trim();
            if (capitalContinent.isEmpty()) {
                output.println("Please enter a continent.");
                return 2;
            }
            capitalsByContinent(connection, capitalContinent, output);
        } else if (!showCapitalsWithContinents.equalsIgnoreCase("no")
                && !showCapitalsWithContinents.equalsIgnoreCase("n")) {
            output.println("Please enter yes or no.");
            return 2;
        }
        return 0;
    }

    /**
     * Prints all cities from largest to smallest population
     * @param connection open database connection
     * @param output report output
     * @throws SQLException if a database query fails
     */
    public static void printAllCitiesWorldwide(Connection connection,
                                               java.io.PrintStream output)
            throws SQLException {
        printCitiesWorldwide(getAllCitiesWorldwide(connection), output);
    }

    static void printCitiesWorldwide(List<City> cities, PrintStream output) {
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

    /**
     * Returns the sum of the population values for all countries.
     * @param connection open database connection
     * @return world population, or zero if there are no countries
     * @throws SQLException if a database query fails
     */
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

    /**
     * Returns the population total for the selected continent.
     * @param connection open database connection
     * @param continent continent name
     * @return continent population, or zero if no countries match
     * @throws SQLException if a database query fails
     */
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

    /**
     * Loads every city with its country name, ordered from largest to smallest.
     * @param connection open database connection
     * @return cities with their country names
     * @throws SQLException if a database query fails
     */
    public static List<City> getAllCitiesWorldwide(Connection connection)
            throws SQLException {

        List<City> cities = new ArrayList<>();

        // City ID keeps the order consistent when populations match.
        String sql = """
                SELECT city.Name,
                       country.Name AS Country,
                       city.District,
                       city.Population
                FROM city
                JOIN country ON city.CountryCode = country.Code
                ORDER BY city.Population DESC, city.ID
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

    /**
     * Returns at most {@code n} cities in the requested district.
     * @param connection open database connection
     * @param district district name
     * @param n maximum number of cities
     * @return matching cities from largest to smallest population
     * @throws IllegalArgumentException if n is zero or negative
     * @throws SQLException if a database query fails
     */
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
                ORDER BY city.Population DESC, city.ID
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


    /**
     * Gets cities in a continent from largest to smallest population
     * @param connection open database connection
     * @param continent continent name
     * @return matching cities with their country names
     * @throws SQLException if a database query fails
     */
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

    /**
     * Returns estimated Chinese speakers and their percentage of world population.
     * @param connection open database connection
     * @return speaker count at index 0 and world percentage at index 1
     * @throws SQLException if a database query fails
     */
    public static double[] getChineseSpeakers(Connection connection)
            throws SQLException {

        // Weight each country's population by the language's reported percentage.
        // NULLIF avoids division by zero
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

    /**
     * Returns estimated English speakers and their percentage of world population.
     * @param connection open database connection
     * @return speaker count at index 0 and world percentage at index 1
     * @throws SQLException if a database query fails
     */
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

    /**
     * Prints up to {@code n} countries in a region, ordered by population.
     * @param connection open database connection
     * @param region region name
     * @param n maximum number of countries
     * @throws SQLException if a database query fails
     */
    public static void printTopCountriesByRegion(
            Connection connection, String region, int n) throws SQLException {

        printTopCountriesByRegion(connection, region, n, System.out);
    }

    /**
     * Prints up to {@code n} countries in a region, ordered by population.
     * <br>
     * Prints a message if n is invalid or no countries match
     * @param connection open database connection
     * @param region region name
     * @param n maximum number of countries
     * @param output report output
     * @throws SQLException if a database query fails
     */
    public static void printTopCountriesByRegion(
            Connection connection, String region, int n, java.io.PrintStream output)
            throws SQLException {
        // A non-positive limit cannot produce a meaningful top-N report.
        if (n <= 0) {
            output.println("enter a number greater than 0: ");
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
                        output.println("Top " + n + " countries in " + region);
                        output.println("Code | Name | Continent | Region | Population | Capital");

                        // Track whether the query returned a matching country.
                        boolean found = false;

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

                        // Give a clear message for a region with no matching rows.
                        if (!found) {
                            output.println("no countries found for this region");
                        }

                    }
                }
            }
    public static void allCapitals(Connection connection, PrintStream output)
            throws SQLException {
        String sql = """
                SELECT country.Name AS Country, city.Name AS Capital
                FROM country
                JOIN city ON country.Capital = city.ID
                ORDER BY city.Population DESC, country.Name, country.Code;
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            output.println("Capital cities worldwide");
            output.println("Country | Capital");

            boolean found = false;
            while (results.next()) {
                found = true;
                String capital = results.getString("Capital");
                output.printf("%s | %s%n", results.getString("Country"),
                        capital == null ? "No capital recorded" : capital);
            }
            if (!found) {
                output.println("No countries found.");
            }
        }
    }

    public static void capitalsByContinent(Connection connection, String continent,
                                           PrintStream output) throws SQLException {
        String sql = """
                SELECT country.Name AS Country,
                       city.Name AS Capital,
                       city.Population
                FROM country
                JOIN city ON country.Capital = city.ID
                WHERE country.Continent = ?
                ORDER BY city.Population DESC, country.Name, country.Code
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, continent);
            try (ResultSet results = statement.executeQuery()) {
                output.println("Capital cities in " + continent + " (population descending)");
                output.println("Country | Capital | Population");

                boolean found = false;
                while (results.next()) {
                    found = true;
                    output.printf("%s | %s | %d%n",
                            results.getString("Country"),
                            results.getString("Capital"),
                            results.getInt("Population"));
                }
                if (!found) {
                    output.println("No capitals found for continent: " + continent);
                }
            }
        }
    }
}
