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
 * Hosts standard HttpServlets (EmployeeServlet, AttendanceServlet, PayrollServlet,
 * DashboardServlet) inside a lightweight standalone server with zero external setup needed.
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
        System.out.println(" Technologies: Java (HttpServlets) + MySQL + HTML + CSS + JS");
        System.out.println("==============================================================");

        // Verify MySQL database connection
        System.out.println("[Step 1/2] Checking MySQL connection...");
        if (DBConnection.testConnection()) {
            System.out.println("[SUCCESS] Successfully connected to MySQL database 'smart_payroll_db'!");
        } else {
            System.err.println("[WARNING] Could not connect to MySQL database.");
            System.err.println("          Please ensure MySQL is running and check 'config.properties'.");
        }

        // Initialize server hosting HttpServlets
        try {
            System.out.println("[Step 2/2] Starting server with HttpServlets on port " + port + "...");
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Register Servlets (HttpServlet instances)
            server.createContext("/api/dashboard", new ServletBridge(new DashboardServlet()));
            server.createContext("/api/employees", new ServletBridge(new EmployeeServlet()));
            server.createContext("/api/attendance", new ServletBridge(new AttendanceServlet()));
            server.createContext("/api/payroll", new ServletBridge(new PayrollServlet()));

            // Static Web Handler for serving HTML, CSS, JavaScript files
            server.createContext("/", new StaticFileHandler("web"));

            // Multi-threaded executor
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("==============================================================");
            System.out.println(" Application is LIVE with HttpServlets!");
            System.out.println(" Open your browser and navigate to:");
            System.out.println(" --> http://localhost:" + port + "/");
            System.out.println(" Press Ctrl+C in this terminal to stop the server.");
            System.out.println("==============================================================");

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
