package org.example;

import org.example.db.DatabaseConnectionManager;
import java.io.PrintStream;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs the world population and city reports
 */
public class Main {
    /**
     * Checks args and connects to the database
     * @param args optional --all-cities flag
     */
    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && !args[0].equals("--all-cities"))) {
            System.err.println("Usage: java -jar app.jar [--all-cities]");
            System.exit(2);
        }
        try (Connection connection = DatabaseConnectionManager.connect()) {
            printAllCitiesWorldwide(connection, System.out);
            if (args.length == 1) {
                return;
            }

            ErrorHandler.runSafely("world population", () ->
                    System.out.printf("World population: %d%n", getWorldPopulation(connection)));

            Scanner scanner = new Scanner(System.in);

            // System.out.print("Enter a continent: ");
            // String continent = scanner.nextLine().trim();
            String continent = InputVal.readContinent(scanner, connection);

            if (continent != null) {
                ErrorHandler.runSafely("continent reports", () -> {
                    System.out.printf("Population of %s: %d%n",
                            continent, getContinentPopulation(connection, continent));
                    // Get and display cities in the selected continent
                    List<City> cities = getCitiesByContinent(connection, continent);

                    if (cities.isEmpty()) {
                        System.out.println("No cities found for continent: " + continent); }
                    else {
                        System.out.println("Cities in " + continent + " (population descending):");
                        System.out.printf("%-25s %-30s %-20s %12s%n", "City", "Country", "District", "Population");

                        for (City city : cities) {
                            System.out.printf("%-25s %-30s %-20s %,12d%n", city.name, city.country, city.district, city.population); }
                    }
                });
            }

            System.out.print("Enter a country: ");

            if (!scanner.hasNextLine()) {
                System.out.println("Please enter a country.");
                System.exit(2);
            }

            String countryName = scanner.nextLine().trim();

            if (countryName.isEmpty()) {
                System.out.println("Please enter a country.");
                System.exit(2);
            }

            List<City> countryCities =
                    getCitiesByCountry(connection, countryName);

            if (countryCities.isEmpty()) {
                System.out.println("No cities found for country: " + countryName);
            } else {
                System.out.println("Cities in " + countryName
                        + " (population descending):");
                System.out.println("Name | Country | District | Population");

                for (City city : countryCities) {
                    System.out.printf("%s | %s | %s | %d%n",
                            city.name, city.country,
                            city.district, city.population);
                }
            }

            System.out.print("Enter a district: ");

            if (!scanner.hasNextLine()) {
                System.out.println("Please enter a district.");
                System.exit(2);
            }

            String district = scanner.nextLine().trim();

            if (district.isEmpty()) {
                System.out.println("Please enter a district.");
                System.exit(2);
            }

            List<City> districtCities =
                    getCitiesByDistrict(connection, district);

            if (districtCities.isEmpty()) {
                System.out.println("No cities found for district: " + district);
            } else {
                System.out.println("Cities in district " + district
                        + " (population descending):");
                System.out.println("Name | Country | District | Population");

                for (City city : districtCities) {
                    System.out.printf("%s | %s | %s | %d%n",
                            city.name, city.country,
                            city.district, city.population);
                }
            }

            // Ask the user for the region and number of countries.
            // System.out.print("Enter a region: ");
            // String region = scanner.nextLine().trim();
            String region = InputVal.readRegion(scanner, connection);

            if (region != null) {
                List<City> regionCities = getCitiesByRegion(connection, region);

                if (regionCities.isEmpty()) {
                    System.out.println("No cities found for region: " + region);
                } else {
                    System.out.println("Cities in " + region
                            + " (population descending):");

                    System.out.printf("%-25s %-30s %-20s %12s%n",
                            "City", "Country", "District", "Population");

                    for (City city : regionCities) {
                        System.out.printf("%-25s %-30s %-20s %,12d%n",
                                city.name, city.country, city.district, city.population);
                    }
                }
            }

            // System.out.print("Enter the number of countries: ");
            //
            // if (!scanner.hasNextInt()) {
            //     System.out.println("Please enter a whole number.");
            //     return;
            // }
            //
            // int n = scanner.nextInt();
            Integer n = InputVal.readPositiveInt(scanner, "Enter the number of countries: ");

            if (region != null && n != null) {
                ErrorHandler.runSafely("top countries", () ->
                        printTopCountriesByRegion(connection, region, n));
            }

            ErrorHandler.runSafely("language report", () -> {
                double[] chinese = getChineseSpeakers(connection);
                double[] english = getEnglishSpeakers(connection);

                System.out.printf(
                        "Chinese: %.0f speakers | %.2f%% of world population%n",
                        chinese[0], chinese[1]);

                System.out.printf(
                        "English: %.0f speakers | %.2f%% of world population%n",
                        english[0], english[1]);

                double[] spanish = getSpanishSpeakers(connection);
                double[] hindi = getHindiSpeakers(connection);

                System.out.printf(
                        "Spanish: %.0f speakers | %.2f%% of world population%n",
                        spanish[0], spanish[1]);

                System.out.printf(
                        "Hindi: %.0f speakers | %.2f%% of world population%n",
                        hindi[0], hindi[1]);

                double[] arabic = getArabicSpeakers(connection);

                System.out.printf(
                        "Arabic: %.0f speakers | %.2f%% of world population%n",
                        arabic[0], arabic[1]);
            });

            ErrorHandler.runSafely("ranked language report", () ->
                    printLanguagesRanked(connection));
        } catch (SQLException e) {
            ErrorHandler.handleSQLException(e, "connecting to the database");
            // e.printStackTrace();
            System.exit(1);
        }
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

    /**
     * Gets the total population of all countries
     * @param connection open database connection
     * @return world population, or zero if there are no countries
     * @throws SQLException if a database query fails
     */
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

    /**
     * Gets the total population of a continent
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
     * Gets all cities from largest to smallest population
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
     * Gets the most populated cities in a district
     * @param connection open database connection
     * @param district district name
     * @param n maximum number of cities
     * @return matching cities from largest to smallest population
     * @throws IllegalArgumentException if n is zero or negative
     * @throws SQLException if a database query fails
     */
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

    public static List<City> getCitiesByCountry(
            Connection connection, String countryName) throws SQLException {

        List<City> cities = new ArrayList<>();

        String sql = """
            SELECT city.Name,
                   country.Name AS Country,
                   city.District,
                   city.Population
            FROM city
            JOIN country ON city.CountryCode = country.Code
            WHERE country.Name = ?
            ORDER BY city.Population DESC
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, countryName);

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


    public static List<City> getCitiesByDistrict(
            Connection connection, String district) throws SQLException {

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
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, district);

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


    public static List<City> getCitiesByRegion(
            Connection connection, String region) throws SQLException {

        List<City> cities = new ArrayList<>();

        String sql = """
            SELECT city.Name,
                   country.Name AS Country,
                   city.District,
                   city.Population
            FROM city
            JOIN country ON city.CountryCode = country.Code
            WHERE country.Region = ?
            ORDER BY city.Population DESC
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, region);

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
     * Estimates Chinese speakers and their share of the world population
     * @param connection open database connection
     * @return speaker count at index 0 and world percentage at index 1
     * @throws SQLException if a database query fails
     */
    public static double[] getChineseSpeakers(Connection connection)
            throws SQLException {

        // Multiply each country's population by its language percentage then add
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
     * Estimates English speakers and their share of the world population
     * @param connection open database connection
     * @return speaker count at index 0 and world percentage at index 1
     * @throws SQLException if a database query fails
     */
    public static double[] getEnglishSpeakers(Connection connection)
            throws SQLException {

        // Use all countries for the world total including those without English speakers
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

    public static double[] getSpanishSpeakers(Connection connection)
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
                WHERE countrylanguage.Language = 'Spanish'
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

    public static double[] getHindiSpeakers(Connection connection)
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
                WHERE countrylanguage.Language = 'Hindi'
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

    public static double[] getArabicSpeakers(Connection connection)
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
                WHERE countrylanguage.Language = 'Arabic'
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

    // Chinese, English, Hindi, Spanish and Arabic ranked by speakers (largest first)
    public static void printLanguagesRanked(Connection connection)
            throws SQLException {

        String[] names = {"Chinese", "English", "Hindi", "Spanish", "Arabic"};
        double[][] results = {
                getChineseSpeakers(connection),
                getEnglishSpeakers(connection),
                getHindiSpeakers(connection),
                getSpanishSpeakers(connection),
                getArabicSpeakers(connection)
        };

        // order[] holds the positions 0-4, sorted by speakers descending
        Integer[] order = {0, 1, 2, 3, 4};
        Arrays.sort(order, (a, b) -> Double.compare(results[b][0], results[a][0]));

        System.out.println("Languages ranked by number of speakers:");
        System.out.printf("%-10s %18s %12s%n", "Language", "Speakers", "% of world");

        for (int i : order) {
            System.out.printf("%-10s %,18.0f %11.2f%%%n",
                    names[i], results[i][0], results[i][1]);
        }
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

    /**
     * Prints the most populated countries in a region
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
