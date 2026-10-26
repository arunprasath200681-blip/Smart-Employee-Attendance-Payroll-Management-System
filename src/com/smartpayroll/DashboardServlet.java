package com.smartpayroll;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * DashboardServlet.java
 * --------------------
 * HTTP Servlet providing real-time KPI metrics for the Dashboard:
 * - GET /api/dashboard -> doGet() : Returns employee count, today's attendance stats, and monthly payroll outflow.
 */
@WebServlet("/api/dashboard/*")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

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

            // 2. Today's attendance breakdown
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

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
