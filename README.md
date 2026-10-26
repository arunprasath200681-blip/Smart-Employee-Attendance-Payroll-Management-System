# 💼 Smart Employee Attendance & Payroll Management System

A beginner-friendly, full-stack application built **strictly using only**:
- **HTML5** (Structure, Tables & Forms)
- **CSS3** (Styling, Responsive UI, & Printable Payslip)
- **JavaScript (Vanilla JS)** (Client-side interactivity & `fetch` API)
- **Java (Jakarta EE Servlets + JDBC)** (Backend business logic using standard HTTP Servlets and direct JDBC — **Zero Frameworks! No Spring Boot, No Maven, No Property Files**)
- **XML (`web.xml`)** (Standard Jakarta EE Deployment Descriptor for tracking and mapping servlets)
- **MySQL** (Relational Database with standard JDBC)

---

## 🌟 Key Features

1. **📊 Interactive Dashboard:**
   - Real-time KPI summary cards: Total Employees, Today's Present, Today's Absent, Today's Leaves, and Monthly Salary Outflow.
   - Live digital clock and today's attendance snapshot.

2. **👥 Employee Management:**
   - Add new employees with code, name, email, phone, department, designation, basic salary, and joining date.
   - Live instant search filter by name, department, or employee code.
   - Edit employee details or delete records.

3. **📅 Smart Attendance Tracking:**
   - Date picker to view or record attendance for any calendar date.
   - Mark status: **Present**, **Absent**, **Half Day**, or **Leave**.
   - Record custom check-in and check-out times with optional remarks.
   - **⚡ 1-Click Smart Action:** "Mark All Present" instantly marks all active staff.

4. **💰 Smart Payroll & Automated Salary Calculation:**
   - Automatic monthly salary calculation directly connected to MySQL attendance:
     - **House Rent Allowance (HRA):** 20% of Basic Salary
     - **Dearness Allowance (DA):** 10% of Basic Salary
     - **Gross Salary:** Basic + HRA + DA
     - **Loss of Pay (Absenteeism):** Deducted per absent day and half day
     - **Provident Fund (PF):** 12% deduction of basic salary
     - **Income Tax:** 5% deduction for higher brackets
     - **Net Take-Home Salary:** Gross Salary − Deductions
   - Toggle payment status between **Paid** and **Pending** with one click.
   - **📄 Printable Payslip:** Generates an official company salary slip with earnings/deductions breakdown and browser print button (`window.print()`).

---

## 📁 Project Directory Structure

```text
d:\project3\
├── database.sql               # MySQL script to create database, tables & sample data
├── README.md                  # Project documentation & viva guide
├── .gitignore                 # Ignores bin/, *.class, and temp files
├── .classpath                 # Eclipse/VS Code Java classpath configuration
├── .project                   # Eclipse/VS Code Java project definition
├── lib\
│   ├── mysql-connector-j.jar  # Official MySQL JDBC Driver jar
│   └── servlet-api.jar        # Official Jakarta Servlet API jar
├── src\
│   └── com\
│       └── smartpayroll\
│           ├── Main.java              # Server startup hosting HttpServlets
│           ├── DBConnection.java      # Standard JDBC connection (URL, user, password)
│           ├── JsonUtil.java          # Helper for JSON serialization
│           ├── ServletBridge.java     # Bridge connecting HttpServlets to standalone runner
│           ├── EmployeeServlet.java   # HttpServlet: /api/employees/* (doGet, doPost, doDelete)
│           ├── AttendanceServlet.java # HttpServlet: /api/attendance/* (doGet, doPost)
│           ├── PayrollServlet.java    # HttpServlet: /api/payroll/* (doGet, doPost)
│           ├── DashboardServlet.java  # HttpServlet: /api/dashboard/* (doGet)
│           └── StaticFileHandler.java # Serves HTML, CSS, JS files to browser
└── web\
    ├── index.html             # Single-Page Application (SPA) user interface
    ├── css\
    │   └── style.css          # Clean modern styling & printable payslip CSS
    ├── js\
    │   └── app.js             # Pure JavaScript with fetch() calls to Servlets
    └── WEB-INF\
        └── web.xml            # Standard Jakarta EE XML file for tracking & mapping servlets
```

---

## 🚀 How to Run in VS Code

### Step 1: Open VS Code Terminal
In VS Code, open the terminal (`Terminal -> New Terminal`).

### Step 2: Compile & Run in One Step
Paste and execute this single command:
```powershell
javac -cp "lib/*" -d bin src/com/smartpayroll/*.java ; java -cp "bin;lib/*" com.smartpayroll.Main
```

### Step 3: Open Browser
Navigate to:
```
http://localhost:8080/
```

---

## 🎓 Viva & Interview Q&A

- **Q: Which technologies are used in this project?**  
  *Answer:* 
  - Frontend: HTML5, CSS3, Vanilla JavaScript.
  - Backend: Java (Jakarta EE HttpServlets + standard JDBC).
  - Tracking / Descriptor: XML (`web.xml`).
  - Database: MySQL.
  - No frameworks (no Spring Boot, no Maven, no Hibernate).

- **Q: Why is the XML file (`web.xml`) used?**  
  *Answer:* `web.xml` is the standard Jakarta EE Deployment Descriptor. It defines servlet names and maps each URL route pattern (`/api/employees/*`, `/api/attendance/*`, `/api/payroll/*`, `/api/dashboard/*`) to its respective `HttpServlet` class.

- **Q: How does JDBC connect to MySQL?**  
  *Answer:* Inside `DBConnection.java`, we use `Class.forName("com.mysql.cj.jdbc.Driver")` and `DriverManager.getConnection(url, user, password)` with the MySQL Connector/J driver. No external configuration or properties files are needed.
