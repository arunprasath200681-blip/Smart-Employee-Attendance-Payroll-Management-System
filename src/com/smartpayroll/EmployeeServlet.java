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
import java.util.Map;

/**
 * EmployeeServlet.java
 * --------------------
 * HTTP Servlet handling Employee CRUD operations:
 * - GET    /api/employees       -> doGet()    : Fetch all employees
 * - POST   /api/employees       -> doPost()   : Add or update an employee
 * - DELETE /api/employees?id=1  -> doDelete() : Delete an employee by ID
 * 
 * Beginner Concept:
 * In Java EE / Jakarta EE, an HttpServlet handles HTTP requests through
 * standard lifecycle methods: doGet(), doPost(), and doDelete().
 * Requests are read from HttpServletRequest, and responses are written
 * back using HttpServletResponse.
 */
@WebServlet("/api/employees/*")
public class EmployeeServlet extends HttpServlet {

    /**
     * Handles HTTP GET requests - Retrieves all employees from MySQL as JSON.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String sql = "SELECT id, emp_code, name, email, phone, department, designation, basic_salary, joining_date " +
                     "FROM employees ORDER BY id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            String json = JsonUtil.resultSetToJson(rs);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(json);

        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * Handles HTTP POST requests - Adds a new employee or updates an existing one.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Read request body JSON
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        Map<String, String> data = JsonUtil.parseJson(sb.toString());

        String idStr = data.get("id");
        String empCode = data.get("emp_code");
        String name = data.get("name");
        String email = data.get("email");
        String phone = data.get("phone");
        String department = data.get("department");
        String designation = data.get("designation");
        String basicSalaryStr = data.get("basic_salary");
        String joiningDate = data.get("joining_date");

        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Name and Email are required\"}");
            return;
        }

        double basicSalary = 0;
        try {
            basicSalary = Double.parseDouble(basicSalaryStr != null ? basicSalaryStr : "0");
        } catch (NumberFormatException ignored) {}

        try (Connection conn = DBConnection.getConnection()) {
            if (empCode == null || empCode.trim().isEmpty()) {
                empCode = "EMP" + (System.currentTimeMillis() % 10000);
            }

            if (idStr != null && !idStr.trim().isEmpty() && !idStr.equals("0")) {
                // UPDATE existing employee
                int id = Integer.parseInt(idStr);
                String sql = "UPDATE employees SET emp_code=?, name=?, email=?, phone=?, department=?, designation=?, basic_salary=?, joining_date=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, empCode);
                    ps.setString(2, name);
                    ps.setString(3, email);
                    ps.setString(4, phone);
                    ps.setString(5, department);
                    ps.setString(6, designation);
                    ps.setDouble(7, basicSalary);
                    ps.setString(8, joiningDate);
                    ps.setInt(9, id);
                    ps.executeUpdate();
                }
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\":true, \"message\":\"Employee updated successfully\"}");
            } else {
                // INSERT new employee
                String sql = "INSERT INTO employees (emp_code, name, email, phone, department, designation, basic_salary, joining_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, empCode);
                    ps.setString(2, name);
                    ps.setString(3, email);
                    ps.setString(4, phone);
                    ps.setString(5, department);
                    ps.setString(6, designation);
                    ps.setDouble(7, basicSalary);
                    ps.setString(8, (joiningDate != null && !joiningDate.isEmpty()) ? joiningDate : new java.sql.Date(System.currentTimeMillis()).toString());
                    ps.executeUpdate();
                }
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"success\":true, \"message\":\"Employee added successfully\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * Handles HTTP DELETE requests - Deletes an employee by their ID.
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Missing required query parameter: id\"}");
            return;
        }

        int id = Integer.parseInt(idStr);
        String sql = "DELETE FROM employees WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\":true, \"message\":\"Employee deleted successfully\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\":\"Employee not found\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
