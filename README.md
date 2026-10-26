# 💼 Smart Employee Attendance & Payroll Management System

A beginner-friendly, full-stack application built **strictly using only**:
- **HTML5** (Structure, Tables & Forms)
- **CSS3** (Styling, Responsive UI, & Printable Payslip)
- **JavaScript (Vanilla JS)** (Client-side interactivity & `fetch` API)
- **Java (Jakarta EE HttpServlets + JDBC)** (Backend business logic using standard HttpServlets and direct JDBC — **Zero Frameworks! No Spring Boot, No Maven, No Property Files**)
- **XML (`web.xml`)** (Standard Jakarta EE Deployment Descriptor for tracking and mapping all servlets)
- **MySQL** (Relational Database with standard JDBC)
- **Server:** Apache Tomcat (Runs and hosts the HttpServlets mapped in `web.xml`)

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
├── lib\                       # External JAR libraries (MySQL JDBC + Tomcat + Servlet API)
│   ├── mysql-connector-j.jar  # Official MySQL JDBC Driver jar
│   ├── servlet-api.jar        # Official Jakarta Servlet API jar
│   └── *.jar                  # Apache Tomcat embedded runtime JARs
├── src\
│   └── com\
│       └── smartpayroll\
│           ├── Main.java              # Server launcher running Apache Tomcat
│           ├── DBConnection.java      # Standard JDBC connection (URL, user, password)
│           ├── JsonUtil.java          # Helper for JSON serialization
│           ├── EmployeeServlet.java   # HttpServlet: /api/employees/* (doGet, doPost, doDelete)
│           ├── AttendanceServlet.java # HttpServlet: /api/attendance/* (doGet, doPost)
│           ├── PayrollServlet.java    # HttpServlet: /api/payroll/* (doGet, doPost)
│           └── DashboardServlet.java  # HttpServlet: /api/dashboard/* (doGet)
└── web\
    ├── index.html             # Single-Page Application (SPA) user interface
    ├── css\
    │   └── style.css          # Clean modern styling & printable payslip CSS
    ├── js\
    │   └── app.js             # Pure JavaScript with fetch() calls to HttpServlets
    └── WEB-INF\
        └── web.xml            # Standard Jakarta EE XML file for tracking & mapping servlets
```

---

## 🚀 How to Run in VS Code

### Step 1: Open VS Code Terminal
In VS Code, open the terminal (`Terminal -> New Terminal`).

### Step 2: Compile & Run in One Command
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
  - Tracking / Deployment Descriptor: XML (`web.xml`).
  - Database: MySQL.
  - Web Server: Apache Tomcat.
  - No frameworks (no Spring Boot, no Maven, no Hibernate).

- **Q: How are the Servlets mapped and tracked?**  
  *Answer:* Through the standard Jakarta EE XML file `web/WEB-INF/web.xml`. It declares each servlet name and maps its URL route pattern (`/api/employees/*`, `/api/attendance/*`, `/api/payroll/*`, `/api/dashboard/*`) to the respective `HttpServlet` class.

- **Q: Which server is running the HttpServlets?**  
  *Answer:* Apache Tomcat. When `Main.java` is launched, Tomcat reads `web/WEB-INF/web.xml`, registers the servlets, and serves the application on port 8080.
