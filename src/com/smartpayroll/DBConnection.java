package com.smartpayroll;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection.java
 * -----------------
 * Pure standard JDBC Database Connection.
 * 
 * Beginner Concept:
 * Uses standard java.sql.DriverManager with MySQL JDBC URL, username, and password.
 * No external properties files or frameworks are needed.
 */
public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/smart_payroll_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "Arun@2006";

    static {
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
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Quick test method to verify database connectivity.
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
