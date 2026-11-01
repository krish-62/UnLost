package com.unlost;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String URL = envOrDefault(
            "UNLOST_DB_URL",
            "jdbc:mysql://localhost:3306/unlost?useSSL=false&serverTimezone=UTC"
    );
    private static final String USER = envOrDefault(
            "UNLOST_DB_USER",
            "unlost_user"
    );

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found.", e);
        }

        return DriverManager.getConnection(URL, USER, "");
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
