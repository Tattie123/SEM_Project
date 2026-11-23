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

/** Integration checks for the population queries using the World database. */
public class IntegrationTest {

    // Answers "no" to the optional reports that follow the continent capital report.
    private static final String DECLINE_LATER_REPORTS = "no\nno\nno\nno\nno\n";

    // Verify the aggregate uses a long value for the total population.
    @Test
    void worldPopulationIncludesAllCountriesWithoutIntegerOverflow()
            throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertEquals(6078749450L, Main.getWorldPopulation(connection));
        }
    }

    // Check known totals across continents, including the empty Antarctica total.
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

    // Unknown input should return zero and SQL-like input must remain a value.
    @ParameterizedTest
    @ValueSource(strings = {"ContinentThatDoesNotExist", "Asia' OR '1'='1"})
    void unknownContinentReturnsZero(String continent) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertEquals(0L, Main.getContinentPopulation(connection, continent));
        }
    }

    // Check returned city details and descending population order.
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
                    new java.util.Scanner("Antarctica\nJapan\nMoscow (City)\nWestern Europe\n2\nno\nno\n"
                            + DECLINE_LATER_REPORTS),
                    new java.io.PrintStream(bytes), false));
            String output = bytes.toString().replace("\r\n", "\n");
            assertTrue(output.contains("""
                    Population of Antarctica: 0
                    Living in cities: 0 (0.00%)
                    Living outside cities: 0 (0.00%)
                    No cities found for continent: Antarctica
                    """));
        }
    }

    // Confirm both the requested limit and the expected top two cities.
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

    // A limit larger than the matching set returns all available cities.
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

    // A district without matching rows should produce an empty list.
    @Test
    void unknownDistrictReturnsNoCities() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> cities = Main.getTopCitiesByDistrict(
                    connection, "DistrictThatDoesNotExist", 5);

            assertTrue(cities.isEmpty());
        }
    }

    // Validate both the estimated count and its share of the whole population.
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

    // The percentage denominator must include every country's population.
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

            String input = String.join("\n",
                    "Europe",
                    "Japan",
                    "Moscow (City)",
                    "Western Europe",
                    "2",
                    "yes",
                    "yes",
                    "South America") + "\n" + DECLINE_LATER_REPORTS;

            int status = Main.runReports(
                    connection,
                    new java.util.Scanner(input),
                    new java.io.PrintStream(bytes),
                    false);

            String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(0, status, () -> "runReports output:\n" + output);

            assertTrue(output.contains("World population: 6078749450"));
            assertTrue(output.contains("Population of Europe: 730074600"));

            String continentReport = output.substring(
                    output.indexOf("Cities in Europe"),
                    output.indexOf("Enter a region:"));

            assertTrue(continentReport.contains("Name | Country | District | Population"));
            assertTrue(continentReport.contains(
                    "Moscow | Russian Federation | Moscow (City) | 8389200"));
            assertFalse(continentReport.contains("Mumbai (Bombay)"));

            assertTrue(output.contains("DEU | Germany | Europe | Western Europe | 82164700 | Berlin"));
            assertTrue(output.contains("FRA | France | Europe | Western Europe | 59225700 | Paris"));
            assertFalse(output.contains("AUT | Austria"));

            assertTrue(output.contains("Capital cities worldwide"));
            assertTrue(output.contains("France | Paris"));
            assertFalse(output.contains("Antarctica |"));

            String regionalCapitals = output.substring(
                    output.indexOf("Capital cities in South America"),
                    output.indexOf("Would you like to see capital cities by region?"));

            assertTrue(regionalCapitals.contains("Brazil |"));
            assertFalse(regionalCapitals.contains("France |"));

            String[] capitalRows = regionalCapitals.lines().skip(2)
                    .filter(line -> !line.isBlank()).toArray(String[]::new);
            int previousPopulation = Integer.MAX_VALUE;

            for (String row : capitalRows) {
                int population = Integer.parseInt(
                        row.substring(row.lastIndexOf('|') + 1).trim());
                assertTrue(population <= previousPopulation,
                        "Continent capitals must be ordered by population descending");
                previousPopulation = population;
            }

            assertTrue(output.contains("Chinese:"));
            assertTrue(output.contains("English:"));
        }
    }

    @Test
    void capitalReportIsSkippedWhenUserDeclines() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            int status = Main.runReports(connection,
                    new java.util.Scanner("Europe\nJapan\nMoscow (City)\nWestern Europe\n2\nno\nno\n"
                            + DECLINE_LATER_REPORTS),
                    new java.io.PrintStream(bytes), false);
            String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);

            assertEquals(0, status);
            assertTrue(output.contains("Would you like to see all capital cities?"));
            assertTrue(output.contains("Would you like to see capital cities by continent?"));
            assertFalse(output.contains("Capital cities worldwide"));
            assertFalse(output.contains("Capital cities in "));
        }
    }

    @Test
    void continentCapitalsCanBeRequestedWithoutWorldwideReport() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            int status = Main.runReports(connection,
                    new java.util.Scanner(
                            "Europe\nJapan\nMoscow (City)\nWestern Europe\n2\nno\nyes\nSouth America\n"
                                    + DECLINE_LATER_REPORTS),
                    new java.io.PrintStream(bytes), false);
            String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);

            assertEquals(0, status);
            assertFalse(output.contains("Capital cities worldwide"));
            String regionalCapitals = output.substring(
                    output.indexOf("Capital cities in South America"));
            assertTrue(regionalCapitals.contains("Brazil |"));
            assertFalse(regionalCapitals.contains("France |"));
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
    void emptyCityDataDisplaysAnExplicitMessage() {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        Main.printCitiesWorldwide(List.of(), new java.io.PrintStream(bytes));

        String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(output.contains("No cities found."));
        assertEquals(3, output.lines().count());
    }

    // Input that answers every existing prompt and declines both earlier capital reports.
    private static final String BASE_INPUT =
            "Europe\nJapan\nMoscow (City)\nWestern Europe\n2\nno\nno\n";

    @Test
    void capitalCitiesInBritishIslandsAreReturnedInPopulationOrder()
            throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> capitals =
                    Main.getCapitalCitiesByRegion(connection, "British Islands");

            assertEquals(2, capitals.size());
            assertEquals("London", capitals.get(0).name);
            assertEquals("United Kingdom", capitals.get(0).country);
            assertEquals(7285000, capitals.get(0).population);
            assertEquals("Dublin", capitals.get(1).name);
            assertEquals("Ireland", capitals.get(1).country);
            assertEquals(481854, capitals.get(1).population);
        }
    }

    @Test
    void capitalCitiesByRegionAreSortedByPopulationDescending()
            throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> capitals =
                    Main.getCapitalCitiesByRegion(connection, "Western Europe");

            assertEquals(9, capitals.size());
            assertEquals("Berlin", capitals.get(0).name);
            for (int i = 1; i < capitals.size(); i++) {
                assertTrue(capitals.get(i - 1).population >= capitals.get(i).population,
                        "Capital cities must be sorted by population descending");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"RegionThatDoesNotExist", "Western Europe' OR '1'='1"})
    void unknownRegionReturnsNoCapitalCities(String region) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertTrue(Main.getCapitalCitiesByRegion(connection, region).isEmpty());
        }
    }

    @Test
    void topThreeCapitalCitiesWorldwideAreReturned() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> capitals = Main.getTopCapitalCitiesWorldwide(connection, 3);

            assertEquals(3, capitals.size());
            assertEquals("Seoul", capitals.get(0).name);
            assertEquals("South Korea", capitals.get(0).country);
            assertEquals(9981619, capitals.get(0).population);
            assertEquals("Jakarta", capitals.get(1).name);
            assertEquals(9604900, capitals.get(1).population);
            assertEquals("Mexico", capitals.get(2).country);
            assertEquals(8591309, capitals.get(2).population);
        }
    }

    @Test
    void requestingMoreCapitalsThanExistReturnsAllWorldwide() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> capitals = Main.getTopCapitalCitiesWorldwide(connection, 1000);

            assertEquals(232, capitals.size());
            for (int i = 1; i < capitals.size(); i++) {
                assertTrue(capitals.get(i - 1).population >= capitals.get(i).population,
                        "Capital cities must be sorted by population descending");
            }
        }
    }

    @Test
    void topThreeCapitalCitiesInEuropeAreReturned() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> capitals =
                    Main.getTopCapitalCitiesByContinent(connection, "Europe", 3);

            assertEquals(3, capitals.size());
            assertEquals("Moscow", capitals.get(0).name);
            assertEquals("Russian Federation", capitals.get(0).country);
            assertEquals(8389200, capitals.get(0).population);
            assertEquals("London", capitals.get(1).name);
            assertEquals(7285000, capitals.get(1).population);
            assertEquals("Berlin", capitals.get(2).name);
            assertEquals(3386667, capitals.get(2).population);
        }
    }

    @Test
    void requestingMoreCapitalsThanExistReturnsAllInContinent() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> capitals =
                    Main.getTopCapitalCitiesByContinent(connection, "Oceania", 100);

            assertEquals(27, capitals.size());
            for (int i = 1; i < capitals.size(); i++) {
                assertTrue(capitals.get(i - 1).population >= capitals.get(i).population,
                        "Capital cities must be sorted by population descending");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Antarctica", "ContinentThatDoesNotExist", "Europe' OR '1'='1"})
    void continentWithNoCapitalsReturnsEmptyList(String continent) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertTrue(Main.getTopCapitalCitiesByContinent(connection, continent, 5)
                    .isEmpty());
        }
    }

    @Test
    void topThreeCapitalCitiesInWesternEuropeAreReturned() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> capitals =
                    Main.getTopCapitalCitiesByRegion(connection, "Western Europe", 3);

            assertEquals(3, capitals.size());
            assertEquals("Berlin", capitals.get(0).name);
            assertEquals("Germany", capitals.get(0).country);
            assertEquals(3386667, capitals.get(0).population);
            assertEquals("Paris", capitals.get(1).name);
            assertEquals(2125246, capitals.get(1).population);
            assertEquals("Wien", capitals.get(2).name);
            assertEquals(1608144, capitals.get(2).population);
        }
    }

    @Test
    void requestingMoreCapitalsThanExistReturnsAllInRegion() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            List<City> capitals =
                    Main.getTopCapitalCitiesByRegion(connection, "Caribbean", 100);

            assertEquals(24, capitals.size());
            for (int i = 1; i < capitals.size(); i++) {
                assertTrue(capitals.get(i - 1).population >= capitals.get(i).population,
                        "Capital cities must be sorted by population descending");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"RegionThatDoesNotExist", "Caribbean' OR '1'='1"})
    void unknownRegionReturnsNoTopCapitalCities(String region) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertTrue(Main.getTopCapitalCitiesByRegion(connection, region, 5).isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void nonPositiveCapitalLimitIsRejected(int n) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertThrows(IllegalArgumentException.class,
                    () -> Main.getTopCapitalCitiesWorldwide(connection, n));
            assertThrows(IllegalArgumentException.class,
                    () -> Main.getTopCapitalCitiesByContinent(connection, "Europe", n));
            assertThrows(IllegalArgumentException.class,
                    () -> Main.getTopCapitalCitiesByRegion(connection, "Caribbean", n));
        }
    }

    @Test
    void unitedKingdomPopulationIsSplitIntoCityAndNonCity() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            CountryPopulation result =
                    Main.getCountryPopulation(connection, "United Kingdom");

            assertNotNull(result);
            assertEquals("United Kingdom", result.name);
            assertEquals(59623400L, result.totalPopulation);
            assertEquals(22436673L, result.cityPopulation);
            assertEquals(37186727L, result.getNonCityPopulation());
            assertEquals(37.63, result.getCityPercentage(), 0.01);
            assertEquals(62.37, result.getNonCityPercentage(), 0.01);
            assertEquals(100.0,
                    result.getCityPercentage() + result.getNonCityPercentage(), 0.0001);
        }
    }

    @Test
    void countryWithZeroPopulationHasZeroPercentages() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            CountryPopulation result = Main.getCountryPopulation(connection, "Antarctica");

            assertNotNull(result);
            assertEquals(0L, result.totalPopulation);
            assertEquals(0L, result.cityPopulation);
            assertEquals(0.0, result.getCityPercentage());
            assertEquals(0.0, result.getNonCityPercentage());
        }
    }

    @Test
    void cityPopulationLargerThanCountryIsReportedAsIs() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            CountryPopulation result = Main.getCountryPopulation(connection, "Singapore");

            assertNotNull(result);
            assertEquals(3567000L, result.totalPopulation);
            assertEquals(4017733L, result.cityPopulation);
            assertTrue(result.getNonCityPopulation() < 0);

            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            Main.printCountryPopulation(result, "Singapore", new java.io.PrintStream(bytes));
            assertTrue(bytes.toString().contains("Note: the database's city populations"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"CountryThatDoesNotExist", "France' OR '1'='1"})
    void unknownCountryReturnsNull(String country) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            assertNull(Main.getCountryPopulation(connection, country));
        }
    }

    @Test
    void newCapitalAndPopulationReportsRunWithUserInput() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            String input = BASE_INPUT
                    + "yes\nBritish Islands\n"
                    + "yes\n3\n"
                    + "yes\nEurope\n3\n"
                    + "yes\nWestern Europe\n3\n"
                    + "yes\nUnited Kingdom\n";

            int status = Main.runReports(connection, new java.util.Scanner(input),
                    new java.io.PrintStream(bytes), false);
            String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(0, status, () -> "runReports output:\n" + output);

            assertTrue(output.contains("Capital cities in region British Islands"));
            assertTrue(output.contains("London | United Kingdom | 7285000"));
            assertTrue(output.contains("Dublin | Ireland | 481854"));

            assertTrue(output.contains("Top 3 capital cities worldwide"));
            assertTrue(output.contains("Seoul | South Korea | 9981619"));

            assertTrue(output.contains("Top 3 capital cities in Europe"));
            assertTrue(output.contains("Moscow | Russian Federation | 8389200"));

            assertTrue(output.contains("Top 3 capital cities in Western Europe"));
            assertTrue(output.contains("Wien | Austria | 1608144"));
            assertFalse(output.contains("Amsterdam | Netherlands | 731200"));

            assertTrue(output.contains("Population of United Kingdom"));
            assertTrue(output.contains("Total population | 59623400"));
            assertTrue(output.contains("Living in cities | 22436673 | 37.63%"));
            assertTrue(output.contains("Not living in cities | 37186727 | 62.37%"));
        }
    }

    @Test
    void unknownCountryDisplaysNoResultsMessage() throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            int status = Main.runReports(connection,
                    new java.util.Scanner(BASE_INPUT + "no\nno\nno\nno\nyes\nAtlantis\n"),
                    new java.io.PrintStream(bytes), false);

            assertEquals(0, status);
            assertTrue(bytes.toString().contains("No country found with name: Atlantis"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"maybe\n", "no\nyes\n0\n", "no\nyes\nabc\n",
            "no\nno\nyes\nEurope\n-1\n", "no\nno\nno\nyes\n\n", "no\nno\nno\nno\nyes\n"})
    void invalidInputForNewReportsReturnsFailure(String laterInput) throws SQLException {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            assertEquals(2, Main.runReports(connection,
                    new java.util.Scanner(BASE_INPUT + laterInput),
                    new java.io.PrintStream(bytes), false));
            assertTrue(bytes.toString().contains("Please enter"));
        }
    }

    @Test
    void reportDoesNotSilentlyIgnoreDatabaseFailure() throws SQLException {
        Connection connection = DatabaseConnectionManager.connect();
        connection.close();
        assertThrows(SQLException.class, () -> Main.getAllCitiesWorldwide(connection));
    }
    // Read the independent database total used to verify language percentages.
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