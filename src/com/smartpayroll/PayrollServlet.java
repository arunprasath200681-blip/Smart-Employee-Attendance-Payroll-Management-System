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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PayrollServlet.java
 * -------------------
 * HTTP Servlet implementing the Smart Payroll Engine:
 * - GET  /api/payroll?month=6&year=2026   -> doGet()  : Auto-syncs with MySQL attendance and returns payroll
 * - POST /api/payroll/generate           -> doPost() : Explicitly triggers recalculation & sync
 * - POST /api/payroll/update-status      -> doPost() : Updates payment status (Paid / Pending)
 * 
 * Direct Attendance <-> Payroll Connection in MySQL:
 * - Reads attendance records for the selected month and year
 * - Counts:
 *     Absent days = COUNT(status = 'Absent')
 *     Half days   = COUNT(status = 'Half Day') (0.5 absent day)
 * - Effective Absent Days = Absent days + (0.5 * Half days)
 * - Effective Present Days = Total Days in Month - Effective Absent Days
 * - Loss of Pay = Effective Absent Days * (Gross Salary / Total Days in Month)
 * - Net Salary = Gross Salary - Loss of Pay - PF (12%) - Tax (5%)
 */
@WebServlet("/api/payroll/*")
public class PayrollServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        String monthStr = req.getParameter("month");
        String yearStr = req.getParameter("year");

        if (monthStr != null && !monthStr.trim().isEmpty()) {
            try { month = Integer.parseInt(monthStr.trim()); } catch (NumberFormatException ignored) {}
        }
        if (yearStr != null && !yearStr.trim().isEmpty()) {
            try { year = Integer.parseInt(yearStr.trim()); } catch (NumberFormatException ignored) {}
        }

        try (Connection conn = DBConnection.getConnection()) {
            // STEP 1: Automatically calculate and sync payroll directly from attendance table!
            calculateAndSyncPayroll(conn, month, year);

            // STEP 2: Fetch and return the freshly updated payroll records
            String sql = "SELECT p.*, e.emp_code, e.name AS emp_name, e.department, e.designation, e.email " +
                         "FROM payroll p " +
                         "JOIN employees e ON p.emp_id = e.id " +
                         "WHERE p.month = ? AND p.year = ? " +
                         "ORDER BY e.name ASC";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, month);
                ps.setInt(2, year);
                try (ResultSet rs = ps.executeQuery()) {
                    String json = JsonUtil.resultSetToJson(rs);
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write(json);
                }
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
            if (uri.endsWith("/generate")) {
                int month = LocalDate.now().getMonthValue();
                int year = LocalDate.now().getYear();

                if (data.get("month") != null && !data.get("month").isEmpty()) {
                    month = Integer.parseInt(data.get("month"));
                }
                if (data.get("year") != null && !data.get("year").isEmpty()) {
                    year = Integer.parseInt(data.get("year"));
                }

                int count = calculateAndSyncPayroll(conn, month, year);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\":true, \"message\":\"Smart payroll generated and synced with attendance for " + count + " employees!\"}");

            } else if (uri.endsWith("/update-status")) {
                String idStr = data.get("id");
                String status = data.get("status");

                if (idStr == null || status == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\":\"id and status are required\"}");
                    return;
                }

                int id = Integer.parseInt(idStr);
                String sql = "UPDATE payroll SET status = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, status);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{\"success\":true, \"message\":\"Status updated to " + status + "\"}");
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\":\"Unknown payroll endpoint\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * CORE SYNC METHOD: Connects MySQL 'attendance' table directly with 'payroll' table.
     * Computes present days, absent days, deductions, and net salary for all employees.
     */
    public static int calculateAndSyncPayroll(Connection conn, int month, int year) throws SQLException {
        YearMonth ym = YearMonth.of(year, month);
        int totalDaysInMonth = ym.lengthOfMonth();

        // 1. Fetch all active employees
        List<EmployeeSalaryInfo> empList = new ArrayList<>();
        String empSql = "SELECT id, name, basic_salary FROM employees";
        try (PreparedStatement psEmp = conn.prepareStatement(empSql);
             ResultSet rsEmp = psEmp.executeQuery()) {
            while (rsEmp.next()) {
                empList.add(new EmployeeSalaryInfo(
                        rsEmp.getInt("id"),
                        rsEmp.getString("name"),
                        rsEmp.getDouble("basic_salary")
                ));
            }
        }

        // 2. SQL to aggregate attendance for an employee in given month & year
        String attSql = "SELECT " +
                        "COUNT(CASE WHEN status = 'Absent' THEN 1 END) AS absent_cnt, " +
                        "COUNT(CASE WHEN status = 'Half Day' THEN 1 END) AS half_day_cnt, " +
                        "COUNT(CASE WHEN status = 'Present' THEN 1 END) AS present_cnt, " +
                        "COUNT(CASE WHEN status = 'Leave' THEN 1 END) AS leave_cnt " +
                        "FROM attendance " +
                        "WHERE emp_id = ? AND MONTH(date) = ? AND YEAR(date) = ?";

        // 3. Upsert statement into payroll table
        String upsertSql = "INSERT INTO payroll (emp_id, month, year, total_working_days, present_days, absent_days, basic_salary, hra, da, pf_deduction, tax_deduction, net_salary, status, generated_date) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Paid', CURDATE()) " +
                           "ON DUPLICATE KEY UPDATE " +
                           "total_working_days=VALUES(total_working_days), " +
                           "present_days=VALUES(present_days), " +
                           "absent_days=VALUES(absent_days), " +
                           "basic_salary=VALUES(basic_salary), " +
                           "hra=VALUES(hra), " +
                           "da=VALUES(da), " +
                           "pf_deduction=VALUES(pf_deduction), " +
                           "tax_deduction=VALUES(tax_deduction), " +
                           "net_salary=VALUES(net_salary), " +
                           "generated_date=CURDATE()";

        int syncedCount = 0;

        for (EmployeeSalaryInfo emp : empList) {
            double absentCnt = 0;
            double halfDayCnt = 0;

            try (PreparedStatement psAtt = conn.prepareStatement(attSql)) {
                psAtt.setInt(1, emp.id);
                psAtt.setInt(2, month);
                psAtt.setInt(3, year);
                try (ResultSet rsAtt = psAtt.executeQuery()) {
                    if (rsAtt.next()) {
                        absentCnt = rsAtt.getDouble("absent_cnt");
                        halfDayCnt = rsAtt.getDouble("half_day_cnt");
                    }
                }
            }

            // Attendance Deduction Rule:
            // Effective Absent Days = (Marked Absent Days) + (0.5 * Half Days)
            // Effective Present Days = Total Days in Month - Effective Absent Days
            double effectiveAbsentDays = absentCnt + (halfDayCnt * 0.5);
            double effectivePresentDays = totalDaysInMonth - effectiveAbsentDays;
            if (effectivePresentDays < 0) effectivePresentDays = 0;

            // Salary Calculation:
            double basic = emp.basicSalary;
            double hra = Math.round(basic * 0.20 * 100.0) / 100.0; // 20% HRA
            double da = Math.round(basic * 0.10 * 100.0) / 100.0;  // 10% DA
            double grossSalary = basic + hra + da;

            // Per-day rate for absent deduction
            double perDayRate = grossSalary / totalDaysInMonth;
            double absentDeduction = Math.round(effectiveAbsentDays * perDayRate * 100.0) / 100.0;

            // Statutory Deductions:
            // PF = 12% of Basic
            double pf = Math.round(basic * 0.12 * 100.0) / 100.0;
            // Income Tax = 5% of Gross if Gross > 30000
            double tax = grossSalary > 30000 ? Math.round(grossSalary * 0.05 * 100.0) / 100.0 : 0.0;

            // Net Take-Home Salary:
            double netSalary = grossSalary - absentDeduction - pf - tax;
            if (netSalary < 0) netSalary = 0;
            netSalary = Math.round(netSalary * 100.0) / 100.0;

            // Update database
            try (PreparedStatement psUp = conn.prepareStatement(upsertSql)) {
                psUp.setInt(1, emp.id);
                psUp.setInt(2, month);
                psUp.setInt(3, year);
                psUp.setInt(4, totalDaysInMonth);
                psUp.setDouble(5, effectivePresentDays);
                psUp.setDouble(6, effectiveAbsentDays);
                psUp.setDouble(7, basic);
                psUp.setDouble(8, hra);
                psUp.setDouble(9, da);
                psUp.setDouble(10, pf);
                psUp.setDouble(11, tax);
                psUp.setDouble(12, netSalary);
                psUp.executeUpdate();
                syncedCount++;
            }
        }
        return syncedCount;
    }

    private static class EmployeeSalaryInfo {
        int id;
        String name;
        double basicSalary;

        EmployeeSalaryInfo(int id, String name, double basicSalary) {
            this.id = id;
            this.name = name;
            this.basicSalary = basicSalary;
        }
    }
}
