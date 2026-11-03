package org.example;

import org.example.db.DatabaseConnectionManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

public class UnitTest {

    // Check city name
    @Test
    void testCityName() {
        City city = new City();
        city.name = "London";
        assertEquals("London", city.name);
    }

    // Check city population
    @Test
    void testCityPopulation() {
        City city = new City();
        city.population = 5000;
        assertEquals(5000, city.population);
    }

    // Check zero cities
    @Test
    void testZeroCities() {
        assertThrows(IllegalArgumentException.class, () -> {
            Main.getTopCitiesByDistrict(null, "Scotland", 0);
        });
    }

    // Check negative cities
    @Test
    void testNegativeCities() {
        assertThrows(IllegalArgumentException.class, () -> {
            Main.getTopCitiesByDistrict(null, "Scotland", -5);
        });
    }

    // Check zero region limit
    @Test
    void testInvalidRegionLimit() throws Exception {
        ByteArrayOutputStream text = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(text);

        Main.printTopCountriesByRegion(null, "Europe", 0, output);

        assertTrue(text.toString().contains("enter a number greater than 0"));
    }

    // Check negative region limit
    @Test
    void testNegativeRegionLimit() throws Exception {
        ByteArrayOutputStream text = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(text);

        Main.printTopCountriesByRegion(null, "Europe", -1, output);

        assertTrue(text.toString().contains("enter a number greater than 0"));
    }

    // Check city country
    @Test
    void testCityCountry() {
        City city = new City();
        city.country = "United Kingdom";
        assertEquals("United Kingdom", city.country);
    }

    // Check city district
    @Test
    void testCityDistrict() {
        City city = new City();
        city.district = "England";
        assertEquals("England", city.district);
    }

    // Check zero continent limit
    @Test
    void testZeroContinentLimit() throws Exception {
        Main.printTopCountriesByContinent(null, "Europe", 0);
    }

    // Check negative continent limit
    @Test
    void testNegativeContinentLimit() throws Exception {
        Main.printTopCountriesByContinent(null, "Europe", -2);
    }

    // Check worldwide countries
    @Test
    void testAllCountriesWorldwide() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            PrintStream oldOutput = System.out;

            try {
                System.setOut(new PrintStream(text));
                Main.printAllCountriesWorldwide(connection);
            } finally {
                System.setOut(oldOutput);
            }

            String result = text.toString();

            assertTrue(result.contains("All Countries Worldwide"));
            assertTrue(result.contains("CHN | China"));
            assertTrue(result.contains("IND | India"));
            assertTrue(result.contains("USA | United States"));
        }
    }

    // Check country columns
    @Test
    void testCountryColumns() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            PrintStream oldOutput = System.out;

            try {
                System.setOut(new PrintStream(text));
                Main.printAllCountriesWorldwide(connection);
            } finally {
                System.setOut(oldOutput);
            }

            assertTrue(text.toString().contains(
                    "Code | Name | Continent | Region | Population | Capital"));
        }
    }

    // Check country order
    @Test
    void testWorldwideCountryOrder() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            PrintStream oldOutput = System.out;

            try {
                System.setOut(new PrintStream(text));
                Main.printAllCountriesWorldwide(connection);
            } finally {
                System.setOut(oldOutput);
            }

            String result = text.toString();

            assertTrue(result.indexOf("CHN | China") <
                    result.indexOf("IND | India"));

            assertTrue(result.indexOf("IND | India") <
                    result.indexOf("USA | United States"));
        }
    }

    // Check Europe countries
    @Test
    void testCountriesByContinent() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            PrintStream oldOutput = System.out;

            try {
                System.setOut(new PrintStream(text));
                Main.printAllCountriesByContinent(connection, "Europe");
            } finally {
                System.setOut(oldOutput);
            }

            String result = text.toString();

            assertTrue(result.contains("Countries in Europe"));
            assertTrue(result.contains("DEU | Germany"));
            assertTrue(result.contains("FRA | France"));
            assertFalse(result.contains("CHN | China"));
        }
    }

    // Check continent order
    @Test
    void testContinentCountryOrder() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            PrintStream oldOutput = System.out;

            try {
                System.setOut(new PrintStream(text));
                Main.printAllCountriesByContinent(connection, "Europe");
            } finally {
                System.setOut(oldOutput);
            }

            String result = text.toString();

            assertTrue(result.indexOf("RUS | Russian Federation") <
                    result.indexOf("DEU | Germany"));
        }
    }

    // Check Western Europe
    @Test
    void testCountriesByRegion() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            PrintStream oldOutput = System.out;

            try {
                System.setOut(new PrintStream(text));
                Main.printAllCountriesByRegion(connection, "Western Europe");
            } finally {
                System.setOut(oldOutput);
            }

            String result = text.toString();

            assertTrue(result.contains("Countries in Western Europe"));
            assertTrue(result.contains("DEU | Germany"));
            assertTrue(result.contains("FRA | France"));
            assertFalse(result.contains("GBR | United Kingdom"));
        }
    }

    // Check region order
    @Test
    void testRegionCountryOrder() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            PrintStream oldOutput = System.out;

            try {
                System.setOut(new PrintStream(text));
                Main.printAllCountriesByRegion(connection, "Western Europe");
            } finally {
                System.setOut(oldOutput);
            }

            String result = text.toString();

            assertTrue(result.indexOf("DEU | Germany") <
                    result.indexOf("FRA | France"));
        }
    }

    // Check unknown continent
    @Test
    void testUnknownCountryContinent() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            PrintStream oldOutput = System.out;

            try {
                System.setOut(new PrintStream(text));
                Main.printAllCountriesByContinent(connection, "Unknown");
            } finally {
                System.setOut(oldOutput);
            }

            assertTrue(text.toString().contains("Countries in Unknown"));
            assertFalse(text.toString().contains("DEU | Germany"));
        }
    }

    // Check unknown region
    @Test
    void testUnknownCountryRegion() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            ByteArrayOutputStream text = new ByteArrayOutputStream();
            PrintStream oldOutput = System.out;

            try {
                System.setOut(new PrintStream(text));
                Main.printAllCountriesByRegion(connection, "Unknown");
            } finally {
                System.setOut(oldOutput);
            }

            assertTrue(text.toString().contains("Countries in Unknown"));
            assertFalse(text.toString().contains("DEU | Germany"));
        }
    }

    // Check Japan cities
    @Test
    void testCitiesByCountry() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            List<City> cities = Main.getCitiesByCountry(connection, "Japan");

            assertFalse(cities.isEmpty());
            assertEquals("Tokyo", cities.get(0).name);
            assertEquals("Japan", cities.get(0).country);
        }
    }

    // Check unknown country cities
    @Test
    void testUnknownCityCountry() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            List<City> cities = Main.getCitiesByCountry(connection, "Unknown");

            assertTrue(cities.isEmpty());
        }
    }

    // Check Scotland cities
    @Test
    void testCitiesByDistrict() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            List<City> cities = Main.getCitiesByDistrict(connection, "Scotland");

            assertFalse(cities.isEmpty());
            assertEquals("Glasgow", cities.get(0).name);
            assertEquals("Scotland", cities.get(0).district);
        }
    }

    // Check region cities
    @Test
    void testCitiesByRegion() throws Exception {
        try (Connection connection = DatabaseConnectionManager.connect()) {

            List<City> cities = Main.getCitiesByRegion(connection, "Western Europe");

            assertFalse(cities.isEmpty());
            assertEquals("Berlin", cities.get(0).name);
            assertEquals("Germany", cities.get(0).country);
        }
    }

}
