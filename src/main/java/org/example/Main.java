package org.example;

import org.example.db.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try (Connection connection = DatabaseConnectionManager.connect()) {
            System.out.println("Successfully connected to MySQL!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    public static List<City> getTopCitiesByDistrict(
            Connection connection, String district, int n) throws SQLException {

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

    public static double[] getChineseSpeakers(Connection connection) throws SQLException {
        String sql = """
                SELECT
                    SUM(country.Population * (countrylanguage.Percentage / 100)) AS Speakers,
                    (SUM(country.Population * (countrylanguage.Percentage / 100))
                     / SUM(country.Population)) * 100 AS WorldPercentage
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

    public static double[] getEnglishSpeakers(Connection connection) throws SQLException {
        String sql = """
                SELECT
                    SUM(country.Population * (countrylanguage.Percentage / 100)) AS Speakers,
                    (SUM(country.Population * (countrylanguage.Percentage / 100))
                     / SUM(country.Population)) * 100 AS WorldPercentage
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
}