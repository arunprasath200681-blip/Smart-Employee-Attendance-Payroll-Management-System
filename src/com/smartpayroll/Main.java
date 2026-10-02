package com.smartpayroll;

import com.sun.net.httpserver.HttpServer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * Main.java
 * ---------
 * The main entry point for the Smart Employee Attendance & Payroll Management System.
 * 
 * Beginner Concept:
 * In pure Java (without frameworks like Spring Boot or external servlet containers),
 * the built-in 'com.sun.net.httpserver.HttpServer' creates a lightweight, high-performance
 * web server with zero external libraries.
 */
public class Main {

    public static void main(String[] args) {
        int port = 8080;

        // Load configured port from config.properties if specified
        try (InputStream is = new FileInputStream("config.properties")) {
            Properties props = new Properties();
            props.load(is);
            String portStr = props.getProperty("server.port");
            if (portStr != null && !portStr.trim().isEmpty()) {
                port = Integer.parseInt(portStr.trim());
            }
        } catch (Exception ignored) {
            // Use default port 8080
        }

        System.out.println("==============================================================");
        System.out.println(" Smart Employee Attendance & Payroll Management System");
        System.out.println(" Technologies: Java + MySQL + HTML + CSS + JavaScript");
        System.out.println(" (Zero Frameworks - Pure Standard Java & Web Technologies)");
        System.out.println("==============================================================");

        // Verify MySQL database connection
        System.out.println("[Step 1/2] Checking MySQL connection...");
        if (DBConnection.testConnection()) {
            System.out.println("[SUCCESS] Successfully connected to MySQL database 'smart_payroll_db'!");
        } else {
            System.err.println("[WARNING] Could not connect to MySQL database.");
            System.err.println("          Please ensure MySQL is running and check 'config.properties'.");
        }

        // Initialize Java built-in HTTP server
        try {
            System.out.println("[Step 2/2] Starting built-in HTTP server on port " + port + "...");
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Register API handlers (REST endpoints)
            server.createContext("/api/dashboard", new DashboardHandler());
            server.createContext("/api/employees", new EmployeeHandler());
            server.createContext("/api/attendance", new AttendanceHandler());
            server.createContext("/api/payroll", new PayrollHandler());

            // Register Static Web Handler for serving HTML, CSS, JavaScript files
            server.createContext("/", new StaticFileHandler("web"));

            // Multi-threaded executor for handling concurrent browser requests
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("==============================================================");
            System.out.println(" Application is LIVE and ready!");
            System.out.println(" Open your browser and navigate to:");
            System.out.println(" --> http://localhost:" + port + "/");
            System.out.println(" Press Ctrl+C in this terminal to stop the server.");
            System.out.println("==============================================================");

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
