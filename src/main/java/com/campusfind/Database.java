package com.campusfind;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Creates JDBC connections using credentials supplied by the Tomcat environment. */
public final class Database {
    private static final String URL = envOrDefault(
            "CAMPUSFIND_DB_URL",
            "jdbc:mysql://localhost:3306/campusfind?useSSL=false&serverTimezone=UTC"
    );
    private static final String USER = System.getenv("CAMPUSFIND_DB_USER");
    private static final String PASSWORD = System.getenv("CAMPUSFIND_DB_PASSWORD");

    private Database() {
        // Utility class
    }

    public static Connection getConnection() throws SQLException {
        if (USER == null || USER.isBlank() || PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException(
                    "Database credentials are missing. Set CAMPUSFIND_DB_USER and CAMPUSFIND_DB_PASSWORD."
            );
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found.", e);
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
