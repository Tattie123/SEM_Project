package org.example;

/** Holds the city details returned by the population queries. */
public class City {
    /** City's name as stored in the database. */
    public String name;
    /** Name of the country that contains the city. */
    public String country;
    /** Administrative district associated with the city. */
    public String district;
    /** Recorded city population. */
    public int population;
}