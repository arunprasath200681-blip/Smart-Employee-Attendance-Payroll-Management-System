package com.smartpayroll;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection.java
 * -----------------
 * This class handles the database connection to MySQL using JDBC.
 * It reads configuration values (URL, username, password) from 'config.properties'.
 * 
 * Beginner Concept:
 * JDBC (Java Database Connectivity) is a standard Java API that interacts with
 * relational databases like MySQL.
 */
public class DBConnection {

    private static String dbUrl = "jdbc:mysql://localhost:3306/smart_payroll_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String dbUser = "root";
    private static String dbPassword = "root";

    static {
        // Load settings from config.properties if available
        Properties props = new Properties();
        try (InputStream is = new FileInputStream("config.properties")) {
            props.load(is);
            if (props.getProperty("db.url") != null) dbUrl = props.getProperty("db.url");
            if (props.getProperty("db.user") != null) dbUser = props.getProperty("db.user");
            if (props.getProperty("db.password") != null) dbPassword = props.getProperty("db.password");
            System.out.println("[DBConnection] Loaded configuration from config.properties");
        } catch (IOException e) {
            System.out.println("[DBConnection] config.properties not found or error loading, using default settings.");
        }

        // Register MySQL JDBC Driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[DBConnection] MySQL JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] ERROR: MySQL JDBC Driver not found in classpath!");
            e.printStackTrace();
        }
    }

    /**
     * Obtains a new connection to the MySQL database.
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Quick test method to verify connectivity
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[DBConnection] Connection failed: " + e.getMessage());
            return false;
        }
    }
}
