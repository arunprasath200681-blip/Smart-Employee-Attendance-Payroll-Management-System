package com.smartpayroll;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * DashboardHandler.java
 * --------------------
 * Provides summary metrics for the interactive dashboard:
 * - Total employee count
 * - Today's attendance counts (Present, Absent, Leave, Half Day)
 * - Monthly payroll total outflow
 * - Department distribution
 * 
 * Beginner Concept:
 * Aggregations in SQL (COUNT, SUM) allow quick real-time analytics
 * that feed directly into visual cards and status badges on the UI.
 */
public class DashboardHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();

        if ("OPTIONS".equals(method)) {
            JsonUtil.sendJsonResponse(exchange, 200, "{\"status\":\"ok\"}");
            return;
        }

        if (!"GET".equals(method)) {
            JsonUtil.sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try {
            getDashboardStats(exchange);
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(exchange, 500, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    private void getDashboardStats(HttpExchange exchange) throws SQLException, IOException {
        int totalEmployees = 0;
        int presentToday = 0;
        int absentToday = 0;
        int leaveToday = 0;
        double monthlyPayrollOutflow = 0.0;

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Total employees
            String sql1 = "SELECT COUNT(*) FROM employees";
            try (PreparedStatement ps = conn.prepareStatement(sql1);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalEmployees = rs.getInt(1);
            }

            // 2. Today's attendance
            String sql2 = "SELECT status, COUNT(*) FROM attendance WHERE date = CURDATE() GROUP BY status";
            try (PreparedStatement ps = conn.prepareStatement(sql2);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String st = rs.getString(1);
                    int count = rs.getInt(2);
                    if ("Present".equalsIgnoreCase(st)) presentToday += count;
                    else if ("Absent".equalsIgnoreCase(st)) absentToday += count;
                    else if ("Leave".equalsIgnoreCase(st) || "Half Day".equalsIgnoreCase(st)) leaveToday += count;
                }
            }

            // 3. Current month payroll outflow
            String sql3 = "SELECT COALESCE(SUM(net_salary), 0) FROM payroll WHERE month = ? AND year = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql3)) {
                ps.setInt(1, currentMonth);
                ps.setInt(2, currentYear);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) monthlyPayrollOutflow = rs.getDouble(1);
                }
            }

            // Build JSON response
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"totalEmployees\":").append(totalEmployees).append(",");
            json.append("\"presentToday\":").append(presentToday).append(",");
            json.append("\"absentToday\":").append(absentToday).append(",");
            json.append("\"leaveToday\":").append(leaveToday).append(",");
            json.append("\"monthlyPayroll\":").append(monthlyPayrollOutflow).append(",");
            json.append("\"currentMonth\":").append(currentMonth).append(",");
            json.append("\"currentYear\":").append(currentYear);
            json.append("}");

            JsonUtil.sendJsonResponse(exchange, 200, json.toString());
        }
    }
}
