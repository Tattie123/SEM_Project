package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        City city = new City();

        city.name = "Glasgow";
        city.country = "United Kingdom";
        city.district = "Scotland";
        city.population = 635130;

        System.out.println(city.name);
        System.out.println(city.country);
        System.out.println(city.district);
        System.out.println(city.population);
    }
    public static List<City> getAllCitiesWorldwide(Connection connection) throws SQLException {
        List<City> cities = new ArrayList<>();

        String sql = """
            SELECT city.Name,
                   country.Name AS Country,
                   city.District,
                   city.Population
            FROM city
            JOIN country ON city.CountryCode = country.Code
            ORDER BY city.Population DESC
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
    public static List<City> getTopCitiesByDistrict(Connection connection, String district, int n) throws SQLException {
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
}