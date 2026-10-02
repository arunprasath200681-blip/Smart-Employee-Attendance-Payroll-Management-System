-- ========================================================
-- Smart Employee Attendance & Payroll Management System
-- Database Setup Script (MySQL)
-- ========================================================

CREATE DATABASE IF NOT EXISTS smart_payroll_db;
USE smart_payroll_db;

-- 1. Employees Table
CREATE TABLE IF NOT EXISTS employees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    emp_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(50) NOT NULL,
    designation VARCHAR(50) NOT NULL,
    basic_salary DOUBLE NOT NULL,
    joining_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Attendance Table
CREATE TABLE IF NOT EXISTS attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    date DATE NOT NULL,
    status ENUM('Present', 'Absent', 'Half Day', 'Leave') DEFAULT 'Present',
    check_in VARCHAR(10) DEFAULT '09:00 AM',
    check_out VARCHAR(10) DEFAULT '05:00 PM',
    remarks VARCHAR(255) DEFAULT 'Regular',
    UNIQUE KEY unique_emp_date (emp_id, date),
    FOREIGN KEY (emp_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- 3. Payroll Table
CREATE TABLE IF NOT EXISTS payroll (
    id INT AUTO_INCREMENT PRIMARY KEY,
    emp_id INT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    total_working_days INT NOT NULL DEFAULT 30,
    present_days DOUBLE NOT NULL DEFAULT 30,
    absent_days DOUBLE NOT NULL DEFAULT 0,
    basic_salary DOUBLE NOT NULL,
    hra DOUBLE NOT NULL,
    da DOUBLE NOT NULL,
    pf_deduction DOUBLE NOT NULL,
    tax_deduction DOUBLE NOT NULL,
    net_salary DOUBLE NOT NULL,
    status ENUM('Pending', 'Paid') DEFAULT 'Paid',
    generated_date DATE NOT NULL,
    UNIQUE KEY unique_emp_month_year (emp_id, month, year),
    FOREIGN KEY (emp_id) REFERENCES employees(id) ON DELETE CASCADE
);

-- ========================================================
-- Sample Initial Data for Testing & Demonstration
-- ========================================================

-- Insert Sample Employees
INSERT INTO employees (emp_code, name, email, phone, department, designation, basic_salary, joining_date)
VALUES 
('EMP101', 'Arun Kumar', 'arun.kumar@example.com', '9876543210', 'Engineering', 'Software Engineer', 45000.00, '2025-01-15'),
('EMP102', 'Priya Sharma', 'priya.s@example.com', '9876543211', 'Human Resources', 'HR Manager', 42000.00, '2024-11-01'),
('EMP103', 'Rajesh Patel', 'rajesh.p@example.com', '9876543212', 'Finance', 'Financial Analyst', 40000.00, '2024-06-20'),
('EMP104', 'Sneha Reddy', 'sneha.r@example.com', '9876543213', 'Marketing', 'Marketing Lead', 38000.00, '2025-02-10'),
('EMP105', 'Vikram Singh', 'vikram.s@example.com', '9876543214', 'Engineering', 'UI/UX Designer', 35000.00, '2025-03-01')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert Today's Attendance (for demo)
INSERT INTO attendance (emp_id, date, status, check_in, check_out, remarks)
VALUES
(1, CURDATE(), 'Present', '09:05 AM', '05:10 PM', 'On Time'),
(2, CURDATE(), 'Present', '08:55 AM', '05:00 PM', 'On Time'),
(3, CURDATE(), 'Half Day', '09:00 AM', '01:30 PM', 'Doctor Appointment'),
(4, CURDATE(), 'Leave', '-', '-', 'Approved Sick Leave'),
(5, CURDATE(), 'Present', '09:15 AM', '05:15 PM', 'On Time')
ON DUPLICATE KEY UPDATE status=VALUES(status);

-- Insert Sample Payroll Records for Previous Month
INSERT INTO payroll (emp_id, month, year, total_working_days, present_days, absent_days, basic_salary, hra, da, pf_deduction, tax_deduction, net_salary, status, generated_date)
VALUES
(1, 8, 2026, 30, 28.0, 2.0, 45000.00, 9000.00, 4500.00, 5400.00, 2250.00, 47850.00, 'Paid', CURDATE()),
(2, 8, 2026, 30, 30.0, 0.0, 42000.00, 8400.00, 4200.00, 5040.00, 2100.00, 47460.00, 'Paid', CURDATE()),
(3, 8, 2026, 30, 27.5, 2.5, 40000.00, 8000.00, 4000.00, 4800.00, 2000.00, 41866.67, 'Paid', CURDATE())
ON DUPLICATE KEY UPDATE net_salary=VALUES(net_salary);
