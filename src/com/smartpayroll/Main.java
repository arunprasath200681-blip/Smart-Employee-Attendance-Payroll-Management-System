package com.smartpayroll;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

/**
 * Main.java
 * ---------
 * The main entry point for the Smart Employee Attendance & Payroll Management System.
 * 
 * Beginner Concept:
 * Runs Apache Tomcat (Embedded) to host Jakarta EE HttpServlets.
 * Tomcat automatically reads "web/WEB-INF/web.xml" to register and track all HttpServlets:
 *   - EmployeeServlet   (/api/employees/*)
 *   - AttendanceServlet (/api/attendance/*)
 *   - PayrollServlet    (/api/payroll/*)
 *   - DashboardServlet  (/api/dashboard/*)
 * 
 * Zero HttpServer. Pure HttpServlets tracked via web.xml running on Tomcat.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        System.out.println("==============================================================");
        System.out.println(" Smart Employee Attendance & Payroll Management System");
        System.out.println(" Technologies: Java (Jakarta EE HttpServlets + JDBC) + MySQL");
        System.out.println("               HTML5 + CSS3 + JavaScript");
        System.out.println(" Deployment Descriptor: web.xml (Tracking & Mapping Servlets)");
        System.out.println(" Server: Apache Tomcat");
        System.out.println("==============================================================");

        // Verify MySQL database connection via JDBC
        System.out.println("[Step 1/2] Checking MySQL connection via JDBC...");
        if (DBConnection.testConnection()) {
            System.out.println("[SUCCESS] Connected to MySQL database 'smart_payroll_db'!");
        } else {
            System.err.println("[WARNING] Could not connect to MySQL database.");
            System.err.println("          Please ensure the MySQL service is running.");
        }

        // Initialize Embedded Tomcat Server
        System.out.println("[Step 2/2] Starting Apache Tomcat on port " + port + "...");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // Initialize the default HTTP connector

        // Set the web application root directory to "web/" folder
        // Tomcat automatically parses "web/WEB-INF/web.xml" to load all HttpServlets
        String webDir = new File("web").getAbsolutePath();
        Context ctx = tomcat.addWebapp("", webDir);

        // Start Tomcat
        tomcat.start();

        System.out.println("==============================================================");
        System.out.println(" Application is LIVE with HttpServlets on Tomcat!");
        System.out.println(" Open your browser and navigate to:");
        System.out.println(" --> http://localhost:" + port + "/");
        System.out.println(" Press Ctrl+C in this terminal to stop the server.");
        System.out.println("==============================================================");

        // Keep the server running until Ctrl+C
        tomcat.getServer().await();
    }
}
