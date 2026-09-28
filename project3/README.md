# 💼 Smart Employee Attendance & Payroll Management System

A beginner-friendly, full-stack application built **strictly using only**:
- **HTML5** (Structure & Forms)
- **CSS3** (Styling, Responsive UI, & Printable Payslip)
- **JavaScript (Vanilla JS)** (Client-side interactivity & `fetch` API)
- **Java (Standard Edition)** (HTTP Server & Business Logic - **Zero Frameworks! No Spring Boot, No Maven**)
- **MySQL** (Relational Database with JDBC)

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
   - Automatic monthly salary calculation based on attendance:
     - **House Rent Allowance (HRA):** 20% of Basic Salary
     - **Dearness Allowance (DA):** 10% of Basic Salary
     - **Gross Salary:** Basic + HRA + DA
     - **Loss of Pay (Absenteeism):** Deducted per absent day based on total days in the month
     - **Provident Fund (PF):** 12% deduction of basic salary
     - **Income Tax:** 5% deduction for higher brackets
     - **Net Take-Home Salary:** Gross Salary − Deductions
   - Toggle payment status between **Paid** and **Pending** with one click.
   - **📄 Printable Payslip:** Generates an official company salary slip with earnings/deductions breakdown and browser print button (`window.print()`).

---

## 📁 Project Directory Structure

```text
d:\project3\
├── config.properties          # MySQL credentials and server port
├── database.sql               # MySQL script to create database, tables & sample data
├── run.bat                    # One-click Windows runner (compiles and starts browser)
├── setup_database.bat         # One-click script to initialize database
├── lib\
│   └── mysql-connector-j.jar  # Official MySQL JDBC Driver jar
├── src\
│   └── com\
│       └── smartpayroll\
│           ├── Main.java              # Java server entry point & route registration
│           ├── DBConnection.java      # MySQL JDBC connection manager
│           ├── JsonUtil.java          # Helper for JSON serialization & HTTP responses
│           ├── EmployeeHandler.java   # /api/employees (GET, POST, DELETE)
│           ├── AttendanceHandler.java # /api/attendance (GET, POST, mark-all)
│           ├── PayrollHandler.java    # /api/payroll (GET, calculate & generate)
│           ├── DashboardHandler.java  # /api/dashboard (KPI metrics)
│           └── StaticFileHandler.java # Serves HTML, CSS, JS files to browser
└── web\
    ├── index.html             # Single-Page Application (SPA) user interface
    ├── css\
    │   └── style.css          # Clean modern styling & printable payslip CSS
    └── js\
        └── app.js             # Pure JavaScript with fetch() calls to Java backend
```

---

## 🚀 How to Run the Project (Beginner Steps)

### Step 1: Verify MySQL Credentials
Open `config.properties` and verify your MySQL password:
```properties
db.url=jdbc:mysql://localhost:3306/smart_payroll_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=Arun@2006
server.port=8080
```
*(If your password is different, simply update `db.password`)*

### Step 2: One-Click Run (Easiest Method)
Simply **double-click** the file `run.bat` in the project folder.
It will:
1. Compile the Java files automatically using `javac`.
2. Start the Java HTTP server.
3. Automatically open your web browser to `http://localhost:8080/`.

---

## 💻 Manual Terminal Commands (For Viva / Demo)

If you want to run or explain the commands manually in PowerShell or Command Prompt:

### 1. Initialize the Database (Already done, but can re-run anytime):
```powershell
Get-Content "d:\project3\database.sql" | & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p"Arun@2006"
```

### 2. Compile Java Source Files:
```powershell
javac -cp "lib/*" -d bin src/com/smartpayroll/*.java
```

### 3. Run Java Application:
```powershell
java -cp "bin;lib/*" com.smartpayroll.Main
```

### 4. Open in Browser:
Open any web browser (Chrome, Edge, Firefox) and go to:
```
http://localhost:8080/
```

---

## 🎓 Technology Breakdown (How each part works)

| Technology | Role in this Project | Why It's Beginner Friendly |
| :--- | :--- | :--- |
| **Java** | Backend web server (`HttpServer`) and business logic | Uses standard Java SE without Spring Boot or Maven complexity. Easy to trace flow line-by-line. |
| **MySQL** | Relational Database Management System | Clean SQL schema with 3 tables (`employees`, `attendance`, `payroll`) connected by foreign keys. |
| **HTML5** | UI Layout & Semantics | Clear semantic tags (`<header>`, `<nav>`, `<section>`, `<table>`, `<form>`). |
| **CSS3** | Modern visual design | Flexbox and Grid layouts, clean card components, and `@media print` for printing payslips. |
| **JavaScript** | Client-Side Interactivity | Vanilla JavaScript using standard `fetch()` API and DOM manipulation without any heavy JS frameworks. |

---

## 💡 Quick Tips for Project Presentation / Viva
- **Q: Why are there no frameworks like Spring Boot or Maven?**  
  *Answer:* Standard Java SE includes `com.sun.net.httpserver.HttpServer` and `java.sql` (JDBC). Using pure Java demonstrates fundamental understanding of networking, HTTP protocol, REST endpoints, and relational database connectivity without hiding details behind framework annotations.
- **Q: How does the Smart Payroll calculate salary?**  
  *Answer:* When you click "Auto-Generate Payroll", the Java backend (`PayrollHandler.java`) queries the `attendance` table for the selected month to count present days and absent days. It calculates per-day salary, applies standard allowances (HRA 20%, DA 10%), deducts absenteeism loss of pay and statutory PF (12%), and generates the Net Take-Home Salary.
