package org.example;

// Population of a country split into people living in cities and not in cities
public class CountryPopulation {
    public String name;
    public long totalPopulation;
    public long cityPopulation;

    public long getNonCityPopulation() {
        return totalPopulation - cityPopulation;
    }

    public double getCityPercentage() {
        return percentageOfTotal(cityPopulation);
    }

    public double getNonCityPercentage() {
        return percentageOfTotal(getNonCityPopulation());
    }

    private double percentageOfTotal(long population) {
        if (totalPopulation == 0) {
            return 0.0;
        }
        return population * 100.0 / totalPopulation;
    }
}
