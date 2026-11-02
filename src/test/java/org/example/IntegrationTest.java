package org.example;

import org.example.db.DatabaseConnectionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    @Test
    void worldPopulationIncludesAllCountriesWithoutIntegerOverflow()
            throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertEquals(6078749450L, Main.getWorldPopulation(connection));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "Africa, 784475000",
            "Antarctica, 0",
            "Asia, 3705025700",
            "Europe, 730074600",
            "North America, 482993000",
            "Oceania, 30401150",
            "South America, 345780000"
    })
    void continentPopulationIncludesOnlyTheSelectedContinent(
            String continent, long expectedPopulation) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertEquals(expectedPopulation,
                    Main.getContinentPopulation(connection, continent));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ContinentThatDoesNotExist", "Asia' OR '1'='1"})
    void unknownContinentReturnsZero(String continent) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertEquals(0L, Main.getContinentPopulation(connection, continent));
        }
    }

    @Test
    void allCitiesAreReturnedInPopulationOrder() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> cities = Main.getAllCitiesWorldwide(connection);

            assertEquals(4079, cities.size());

            for (City city : cities) {
                assertNotNull(city.name);
                assertNotNull(city.country);
                assertNotNull(city.district);
                assertTrue(city.population >= 0);
            }

            for (int i = 1; i < cities.size(); i++) {
                assertTrue(
                        cities.get(i - 1).population >= cities.get(i).population,
                        "Cities must be sorted by population descending");
            }
        }
    }


    @Test
    void citiesByContinentAreReturnedInPopulationOrder() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> cities =
                    Main.getCitiesByContinent(connection, "Europe");

            assertFalse(cities.isEmpty());
            assertEquals("Moscow", cities.get(0).name);
            assertEquals("Russian Federation", cities.get(0).country);
            assertEquals(8389200, cities.get(0).population);

            java.util.Set<String> europeanCountries = new java.util.HashSet<>();
            try (Statement statement = connection.createStatement();
                 ResultSet countries = statement.executeQuery(
                         "SELECT Name FROM country WHERE Continent = 'Europe'")) {
                while (countries.next()) {
                    europeanCountries.add(countries.getString("Name"));
                }
            }

            for (City city : cities) {
                assertNotNull(city.name);
                assertNotNull(city.country);
                assertNotNull(city.district);
                assertTrue(city.population >= 0);
                assertTrue(europeanCountries.contains(city.country),
                        "Continent report must contain only European countries");
            }

            for (int i = 1; i < cities.size(); i++) {
                assertTrue(
                        cities.get(i - 1).population >= cities.get(i).population,
                        "Cities in Europe must be sorted by population descending"
                );
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Antarctica", "ContinentThatDoesNotExist", "Asia' OR '1'='1"})
    void emptyOrUnknownContinentReturnsNoCities(String continent) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertTrue(Main.getCitiesByContinent(connection, continent).isEmpty());
        }
    }

    @Test
    void continentWithNoCitiesDisplaysAnExplicitMessage() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            assertEquals(0, Main.runReports(connection,
                    new java.util.Scanner("Antarctica\nJapann\nMoscow (City)\nWestern Europe\n2\n"),
                    new java.io.PrintStream(bytes), false));
            assertTrue(bytes.toString().contains("No cities found for continent: Antarctica"));
        }
    }

    @Test
    void topTwoCitiesInScotlandAreReturned() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> cities =
                    Main.getTopCitiesByDistrict(connection, "Scotland", 2);

            assertEquals(2, cities.size());

            assertEquals("Glasgow", cities.get(0).name);
            assertEquals(619680, cities.get(0).population);

            assertEquals("Edinburgh", cities.get(1).name);
            assertEquals(450180, cities.get(1).population);

            for (City city : cities) {
                assertEquals("Scotland", city.district);
                assertEquals("United Kingdom", city.country);
            }
        }
    }

    @Test
    void requestingFiveReturnsFourWhenOnlyFourExist() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> cities =
                    Main.getTopCitiesByDistrict(connection, "Scotland", 5);

            assertEquals(4, cities.size());
            assertEquals("Aberdeen", cities.get(2).name);
            assertEquals("Dundee", cities.get(3).name);
        }
    }

    @Test
    void unknownDistrictReturnsNoCities() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> cities = Main.getTopCitiesByDistrict(
                    connection, "DistrictThatDoesNotExist", 5);

            assertTrue(cities.isEmpty());
        }
    }

    @Test
    void chineseSpeakersUseTheWholeWorldPopulation() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            double[] result = Main.getChineseSpeakers(connection);

            assertEquals(2, result.length);
            assertEquals(1191843539.0, result[0], 1.0);

            double expectedPercentage =
                    result[0] / getWorldPopulation(connection) * 100.0;

            assertEquals(expectedPercentage, result[1], 0.0001);
            assertEquals(19.61, result[1], 0.01);
        }
    }

    @Test
    void englishSpeakersUseTheWholeWorldPopulation() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            double[] result = Main.getEnglishSpeakers(connection);

            assertEquals(2, result.length);
            assertEquals(347077867.0, result[0], 1.0);

            double expectedPercentage =
                    result[0] / getWorldPopulation(connection) * 100.0;

            assertEquals(expectedPercentage, result[1], 0.0001);
            assertEquals(5.71, result[1], 0.01);
        }
    }

    @Test
    void worldwideReportDisplaysEveryCityWithAllFields() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            Main.printAllCitiesWorldwide(connection, new java.io.PrintStream(bytes));
            String[] lines = bytes.toString(java.nio.charset.StandardCharsets.UTF_8).lines()
                    .toArray(String[]::new);
            assertEquals(4081, lines.length);
            assertEquals("Name | Country | District | Population", lines[1]);
            assertEquals("Mumbai (Bombay) | India | Maharashtra | 10500000", lines[2]);
            assertTrue(lines[4080].startsWith("Adamstown | Pitcairn | "));
            assertTrue(lines[4080].endsWith(" | 42"));
        }
    }

    @Test
    void reportsRunTogetherWithUserInput() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            int status = Main.runReports(connection,
                    new java.util.Scanner("Europe\nJapan\nMoscow (City)\nWestern Europe\n2\n"),
                    new java.io.PrintStream(bytes), false);
            String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(0, status);
            assertTrue(output.contains("World population: 6078749450"));
            assertTrue(output.contains("Population of Europe: 730074600"));
            String continentReport = output.substring(output.indexOf("Cities in Europe"),
                    output.indexOf("Enter a region:"));
            assertTrue(continentReport.contains("Name | Country | District | Population"));
            assertTrue(continentReport.contains(
                    "Moscow | Russian Federation | Moscow (City) | 8389200"));
            assertFalse(continentReport.contains("Mumbai (Bombay)"));
            assertTrue(output.contains("DEU | Germany | Europe | Western Europe | 82164700 | Berlin"));
            assertTrue(output.contains("FRA | France | Europe | Western Europe | 59225700 | Paris"));
            assertFalse(output.contains("AUT | Austria"));
            assertTrue(output.contains("Chinese:"));
            assertTrue(output.contains("English:"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Europe\n", "Europe\n\n", "Europe\nWestern Europe\n",
            "Europe\nWestern Europe\nabc\n", "Europe\nWestern Europe\n0\n",
            "Europe\nWestern Europe\n-2\n"})
    void invalidOrMissingUserInputReturnsFailure(String input) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            assertEquals(2, Main.runReports(connection, new java.util.Scanner(input),
                    new java.io.PrintStream(bytes), false));
            assertTrue(bytes.toString().contains("Please enter"));
        }
    }

    @Test
    void cityOnlyModeNeedsNoInteractiveInput() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            assertEquals(0, Main.runReports(connection, new java.util.Scanner(""),
                    new java.io.PrintStream(bytes), true));
            assertTrue(bytes.toString().contains("All cities worldwide"));
            assertFalse(bytes.toString().contains("Enter a continent"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void nonPositiveCityLimitIsRejected(int n) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertThrows(IllegalArgumentException.class,
                    () -> Main.getTopCitiesByDistrict(connection, "Scotland", n));
        }
    }

    @Test
    void districtInputCannotChangeTheQuery() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertTrue(Main.getTopCitiesByDistrict(connection, "Scotland' OR '1'='1", 5)
                    .isEmpty());
        }
    }

    @Test
    void unknownRegionDisplaysNoResultsMessage() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            Main.printTopCountriesByRegion(connection, "Unknown' OR '1'='1", 5,
                    new java.io.PrintStream(bytes));
            assertTrue(bytes.toString().contains("no countries found"));
        }
    }
    @Test
    void emptyCityDataDisplaysAnExplicitMessage() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect();
             Statement statement = connection.createStatement()) {
            // Connection uses the empty temporary table instead of the real city table
            statement.execute("""
                    CREATE TEMPORARY TABLE city (
                        ID INT, Name VARCHAR(100), CountryCode CHAR(3),
                        District VARCHAR(100), Population INT)
                    """);
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            Main.printAllCitiesWorldwide(connection, new java.io.PrintStream(bytes));
            assertTrue(bytes.toString().contains("No cities found."));
            assertEquals(3, bytes.toString().lines().count());
        }
    }

    @Test
    void reportDoesNotSilentlyIgnoreDatabaseFailure() throws SQLException {
        Connection connection = DatabaseConnectionManager.connect();
        connection.close();
        assertThrows(SQLException.class, () -> Main.getAllCitiesWorldwide(connection));
    }
    private double getWorldPopulation(Connection connection)
            throws SQLException {

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT SUM(Population) FROM country")) {

            assertTrue(resultSet.next());

            double population = resultSet.getDouble(1);
            assertTrue(population > 0);

            return population;
        }
    }
}
