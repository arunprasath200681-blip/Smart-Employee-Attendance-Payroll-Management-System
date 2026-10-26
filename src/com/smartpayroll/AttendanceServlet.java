package com.smartpayroll;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

/**
 * AttendanceServlet.java
 * ----------------------
 * HTTP Servlet handling Smart Attendance Tracking:
 * - GET  /api/attendance?date=YYYY-MM-DD -> doGet()  : Fetch attendance records for all employees on date
 * - POST /api/attendance                -> doPost() : Mark/update single employee attendance
 * - POST /api/attendance/mark-all       -> doPost() : 1-Click Smart Action to mark all employees Present
 * 
 * Synchronized with Payroll:
 * Whenever an attendance status is saved or updated, the servlet automatically triggers
 * PayrollServlet.calculateAndSyncPayroll() to keep the MySQL payroll table live and accurate.
 */
@WebServlet("/api/attendance/*")
public class AttendanceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String targetDate = req.getParameter("date");
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
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(json);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String uri = req.getRequestURI();
        if (uri == null) uri = "";

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        Map<String, String> data = JsonUtil.parseJson(sb.toString());

        try (Connection conn = DBConnection.getConnection()) {
            if (uri.endsWith("/mark-all")) {
                // 1-Click Feature: Mark all present
                String dateStr = data.get("date");
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    dateStr = LocalDate.now().toString();
                }

                String sql = "INSERT INTO attendance (emp_id, date, status, check_in, check_out, remarks) " +
                             "SELECT id, ?, 'Present', '09:00 AM', '05:00 PM', 'Auto Marked' FROM employees " +
                             "ON DUPLICATE KEY UPDATE status='Present', check_in='09:00 AM', check_out='05:00 PM', remarks='Auto Marked'";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, dateStr);
                    int rows = ps.executeUpdate();

                    // Real-Time Sync: Instantly recalculate & sync payroll in MySQL
                    try {
                        LocalDate d = LocalDate.parse(dateStr);
                        PayrollServlet.calculateAndSyncPayroll(conn, d.getMonthValue(), d.getYear());
                    } catch (Exception ignored) {}

                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{\"success\":true, \"message\":\"Marked all present & updated payroll (" + rows + " records)\"}");
                }
            } else {
                // Single employee attendance save/update
                String empIdStr = data.get("emp_id");
                String dateStr = data.get("date");
                String status = data.get("status");
                String checkIn = data.get("check_in");
                String checkOut = data.get("check_out");
                String remarks = data.get("remarks");

                if (empIdStr == null || dateStr == null || status == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\":\"emp_id, date, and status are required\"}");
                    return;
                }

                int empId = Integer.parseInt(empIdStr);
                if (checkIn == null || checkIn.trim().isEmpty()) checkIn = "09:00 AM";
                if (checkOut == null || checkOut.trim().isEmpty()) checkOut = "05:00 PM";
                if (remarks == null) remarks = "Regular";

                String sql = "INSERT INTO attendance (emp_id, date, status, check_in, check_out, remarks) " +
                             "VALUES (?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE status=VALUES(status), check_in=VALUES(check_in), check_out=VALUES(check_out), remarks=VALUES(remarks)";

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, empId);
                    ps.setString(2, dateStr);
                    ps.setString(3, status);
                    ps.setString(4, checkIn);
                    ps.setString(5, checkOut);
                    ps.setString(6, remarks);
                    ps.executeUpdate();

                    // Real-Time Sync: Instantly recalculate & sync payroll in MySQL
                    try {
                        LocalDate d = LocalDate.parse(dateStr);
                        PayrollServlet.calculateAndSyncPayroll(conn, d.getMonthValue(), d.getYear());
                    } catch (Exception ignored) {}

                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{\"success\":true, \"message\":\"Attendance saved & payroll updated!\"}");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
