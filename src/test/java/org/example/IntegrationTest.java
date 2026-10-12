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
