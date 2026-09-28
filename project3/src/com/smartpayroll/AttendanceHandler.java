package com.smartpayroll;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

/**
 * AttendanceHandler.java
 * ----------------------
 * Handles REST API endpoints for Smart Attendance:
 * - GET  /api/attendance?date=YYYY-MM-DD  -> Get attendance for all employees on a date
 * - POST /api/attendance                 -> Mark/update single employee attendance
 * - POST /api/attendance/mark-all        -> Smart 1-click feature to mark all active employees Present
 * 
 * Beginner Concept:
 * We use SQL LEFT JOIN to join the employees table with the attendance table.
 * If an employee does not have an attendance entry for a given day, SQL's COALESCE
 * provides default values like 'Not Marked'.
 */
public class AttendanceHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        // Handle CORS pre-flight
        if ("OPTIONS".equals(method)) {
            JsonUtil.sendJsonResponse(exchange, 200, "{\"status\":\"ok\"}");
            return;
        }

        try {
            if ("GET".equals(method)) {
                getAttendanceByDate(exchange);
            } else if ("POST".equals(method)) {
                if (path.endsWith("/mark-all")) {
                    markAllPresent(exchange);
                } else {
                    saveAttendance(exchange);
                }
            } else {
                JsonUtil.sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(exchange, 500, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * Retrieves attendance for all employees on a specific date.
     * Default date is today's date if not supplied.
     */
    private void getAttendanceByDate(HttpExchange exchange) throws SQLException, IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = JsonUtil.parseQueryParams(query);
        String targetDate = params.get("date");

        if (targetDate == null || targetDate.trim().isEmpty()) {
            targetDate = LocalDate.now().toString();
        }

        String sql = "SELECT e.id AS emp_id, e.emp_code, e.name, e.department, e.designation, " +
                     "COALESCE(a.id, 0) AS attendance_id, " +
                     "COALESCE(a.status, 'Not Marked') AS status, " +
                     "COALESCE(a.check_in, '-') AS check_in, " +
                     "COALESCE(a.check_out, '-') AS check_out, " +
                     "COALESCE(a.remarks, '') AS remarks " +
                     "FROM employees e " +
                     "LEFT JOIN attendance a ON e.id = a.emp_id AND a.date = ? " +
                     "ORDER BY e.id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, targetDate);
            try (ResultSet rs = ps.executeQuery()) {
                String json = JsonUtil.resultSetToJson(rs);
                JsonUtil.sendJsonResponse(exchange, 200, json);
            }
        }
    }

    /**
     * Saves or updates single employee attendance using MySQL ON DUPLICATE KEY UPDATE.
     */
    private void saveAttendance(HttpExchange exchange) throws SQLException, IOException {
        String body = JsonUtil.readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseJson(body);

        String empIdStr = data.get("emp_id");
        String dateStr = data.get("date");
        String status = data.get("status");
        String checkIn = data.get("check_in");
        String checkOut = data.get("check_out");
        String remarks = data.get("remarks");

        if (empIdStr == null || dateStr == null || status == null) {
            JsonUtil.sendJsonResponse(exchange, 400, "{\"error\":\"emp_id, date, and status are required\"}");
            return;
        }

        int empId = Integer.parseInt(empIdStr);
        if (checkIn == null || checkIn.trim().isEmpty()) checkIn = "09:00 AM";
        if (checkOut == null || checkOut.trim().isEmpty()) checkOut = "05:00 PM";
        if (remarks == null) remarks = "Regular";

        String sql = "INSERT INTO attendance (emp_id, date, status, check_in, check_out, remarks) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE status=VALUES(status), check_in=VALUES(check_in), check_out=VALUES(check_out), remarks=VALUES(remarks)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setString(2, dateStr);
            ps.setString(3, status);
            ps.setString(4, checkIn);
            ps.setString(5, checkOut);
            ps.setString(6, remarks);
            ps.executeUpdate();

            JsonUtil.sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Attendance saved successfully\"}");
        }
    }

    /**
     * Smart Feature: Marks all employees as 'Present' for the given date in one click!
     */
    private void markAllPresent(HttpExchange exchange) throws SQLException, IOException {
        String body = JsonUtil.readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseJson(body);

        String dateStr = data.get("date");
        if (dateStr == null || dateStr.trim().isEmpty()) {
            dateStr = LocalDate.now().toString();
        }

        String sql = "INSERT INTO attendance (emp_id, date, status, check_in, check_out, remarks) " +
                     "SELECT id, ?, 'Present', '09:00 AM', '05:00 PM', 'Auto Marked' FROM employees " +
                     "ON DUPLICATE KEY UPDATE status='Present', check_in='09:00 AM', check_out='05:00 PM', remarks='Auto Marked'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dateStr);
            int rowsAffected = ps.executeUpdate();

            JsonUtil.sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Marked all employees present (" + rowsAffected + " records)\"}");
        }
    }
}
