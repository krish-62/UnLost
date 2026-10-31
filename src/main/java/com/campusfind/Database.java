package com.campusfind;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens a local development database connection.
 * The dedicated campus_user account is intentionally configured with
 * an empty password for this localhost-only student project.
 */
public final class Database {
    private static final String URL = envOrDefault(
            "CAMPUSFIND_DB_URL",
            "jdbc:mysql://localhost:3306/campusfind?useSSL=false&serverTimezone=UTC"
    );
    private static final String USER = envOrDefault(
            "CAMPUSFIND_DB_USER",
            "campus_user"
    );

    private Database() {
        // Utility class
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found.", e);
        }

        // Empty password: MariaDB must have campus_user@localhost configured
        // with no password. This is intended for local development only.
        return DriverManager.getConnection(URL, USER, "");
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
