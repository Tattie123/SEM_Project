package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationTest {

    @Test
    void cityDataCanBeCreatedAndRead() {
        City city = new City();

        city.name = "Glasgow";
        city.country = "United Kingdom";
        city.district = "Scotland";
        city.population = 635130;

        assertEquals("Glasgow", city.name);
        assertEquals("United Kingdom", city.country);
        assertEquals("Scotland", city.district);
        assertEquals(635130, city.population);
    }
    @Test
    void cityPopulationCanBeUpdated() {
        City city = new City();
        city.population = 1000;

        city.population = 2000;

        assertEquals(2000, city.population);
    }
}