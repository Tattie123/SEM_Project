package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class UnitTest {

    // Test 1 - Check the city name
    @Test
    void testCityName() {

        City city = new City();
        city.name = "London";

        assertEquals("London", city.name);
    }


    // Test 2 - Check the city population
    @Test
    void testCityPopulation() {

        City city = new City();
        city.population = 5000;

        assertEquals(5000, city.population);
    }


    // Test 3 - Check that zero cities is rejected
    @Test
    void testZeroCities() {

        assertThrows(IllegalArgumentException.class, () -> {
            Main.getTopCitiesByDistrict(null, "Scotland", 0);
        });
    }


    // Test 4 - Check that negative cities is rejected
    @Test
    void testNegativeCities() {

        assertThrows(IllegalArgumentException.class, () -> {
            Main.getTopCitiesByDistrict(null, "Scotland", -5);
        });
    }


    // Test 5 - Check the region report with zero
    @Test
    void testInvalidRegionLimit() throws Exception {

        ByteArrayOutputStream text = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(text);

        Main.printTopCountriesByRegion(null, "Europe", 0, output);

        assertTrue(text.toString().contains("enter a number greater than 0"));
    }


    // Test 6 - Check the region report with a negative number
    @Test
    void testNegativeRegionLimit() throws Exception {

        ByteArrayOutputStream text = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(text);

        Main.printTopCountriesByRegion(null, "Europe", -1, output);

        assertTrue(text.toString().contains("enter a number greater than 0"));
    }


    // Test 7 - Check the city country
    @Test
    void testCityCountry() {

        City city = new City();
        city.country = "United Kingdom";

        assertEquals("United Kingdom", city.country);
    }


    // Test 8 - Check the city district
    @Test
    void testCityDistrict() {

        City city = new City();
        city.district = "England";

        assertEquals("England", city.district);
    }


    // Test 9 - Check the continent report with zero
    @Test
    void testZeroContinentLimit() throws Exception {

        Main.printTopCountriesByContinent(null, "Europe", 0);
    }


    // Test 10 - Check the continent report with a negative number
    @Test
    void testNegativeContinentLimit() throws Exception {

        Main.printTopCountriesByContinent(null, "Europe", -2);
    }

}
