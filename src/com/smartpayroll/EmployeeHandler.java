package com.smartpayroll;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.*;
import java.util.Map;

/**
 * EmployeeHandler.java
 * -------------------
 * Handles REST API endpoints for Employee operations:
 * - GET    /api/employees       -> Fetch all employees
 * - POST   /api/employees       -> Add or Update an employee
 * - DELETE /api/employees?id=1  -> Delete an employee
 * 
 * Beginner Concept:
 * In pure Java, an HttpHandler is registered on a route. When a browser calls
 * that URL, the handle() method is invoked. We examine the HTTP method (GET, POST, DELETE)
 * and perform SQL operations using JDBC PreparedStatements.
 */
public class EmployeeHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();

        // Handle CORS pre-flight requests
        if ("OPTIONS".equals(method)) {
            JsonUtil.sendJsonResponse(exchange, 200, "{\"status\":\"ok\"}");
            return;
        }

        try {
            switch (method) {
                case "GET":
                    getAllEmployees(exchange);
                    break;
                case "POST":
                    saveEmployee(exchange);
                    break;
                case "DELETE":
                    deleteEmployee(exchange);
                    break;
                default:
                    JsonUtil.sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JsonUtil.sendJsonResponse(exchange, 500, "{\"error\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    /**
     * Retrieves all employees from the database and returns them as a JSON array.
     */
    private void getAllEmployees(HttpExchange exchange) throws SQLException, IOException {
        String sql = "SELECT id, emp_code, name, email, phone, department, designation, basic_salary, joining_date FROM employees ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            String json = JsonUtil.resultSetToJson(rs);
            JsonUtil.sendJsonResponse(exchange, 200, json);
        }
    }

    /**
     * Adds a new employee or updates an existing employee if 'id' is supplied.
     */
    private void saveEmployee(HttpExchange exchange) throws SQLException, IOException {
        String body = JsonUtil.readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseJson(body);

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
            JsonUtil.sendJsonResponse(exchange, 400, "{\"error\":\"Name and Email are required\"}");
            return;
        }

        double basicSalary = 0;
        try {
            basicSalary = Double.parseDouble(basicSalaryStr != null ? basicSalaryStr : "0");
        } catch (NumberFormatException ignored) {}

        try (Connection conn = DBConnection.getConnection()) {
            // Auto generate emp_code if empty
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
                JsonUtil.sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Employee updated successfully\"}");
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
                JsonUtil.sendJsonResponse(exchange, 201, "{\"success\":true, \"message\":\"Employee added successfully\"}");
            }
        }
    }

    /**
     * Deletes an employee by their ID.
     */
    private void deleteEmployee(HttpExchange exchange) throws SQLException, IOException {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = JsonUtil.parseQueryParams(query);
        String idStr = params.get("id");

        if (idStr == null || idStr.trim().isEmpty()) {
            JsonUtil.sendJsonResponse(exchange, 400, "{\"error\":\"Missing required query parameter: id\"}");
            return;
        }

        int id = Integer.parseInt(idStr);
        String sql = "DELETE FROM employees WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                JsonUtil.sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Employee deleted successfully\"}");
            } else {
                JsonUtil.sendJsonResponse(exchange, 404, "{\"error\":\"Employee not found\"}");
            }
        }
    }
}
