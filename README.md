# 💼 Smart Employee Attendance & Payroll Management System

A beginner-friendly, full-stack application built **strictly using only**:
- **HTML5** (Structure, Tables & Forms)
- **CSS3** (Styling, Responsive UI, & Printable Payslip)
- **JavaScript (Vanilla JS)** (Client-side interactivity & `fetch` API)
- **Java Servlets (`HttpServlet`)** (Backend business logic using standard HTTP Servlets — **Zero Frameworks! No Spring Boot, No Maven**)
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
├── config.properties          # MySQL credentials and server port
├── database.sql               # MySQL script to create database, tables & sample data
├── setup_database.bat         # One-click script to initialize database
├── lib\
│   ├── mysql-connector-j.jar  # Official MySQL JDBC Driver jar
│   └── servlet-api.jar        # Official Jakarta Servlet API jar
├── src\
│   └── com\
│       └── smartpayroll\
│           ├── Main.java              # Server startup hosting HttpServlets
│           ├── DBConnection.java      # MySQL JDBC connection manager
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
        └── web.xml            # Standard Servlet deployment descriptor
```

---

## 🚀 How to Run in VS Code

### Step 1: Verify MySQL Credentials
Open `config.properties` and verify your MySQL password:
```properties
db.url=jdbc:mysql://localhost:3306/smart_payroll_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=Arun@2006
server.port=8080
```

### Step 2: Compile Java Servlets
In the VS Code terminal (`Terminal -> New Terminal`):
```powershell
javac -cp "lib/*" -d bin src/com/smartpayroll/*.java
```

### Step 3: Run the Application
```powershell
java -cp "bin;lib/*" com.smartpayroll.Main
```

### Step 4: Open Browser
Open your browser and navigate to:
```
http://localhost:8080/
```

---

## 🎓 How to Explain HttpServlets in Viva / Interviews

- **Q: What are Servlets?**  
  *Answer:* A Servlet (`HttpServlet`) is a Java class that handles HTTP requests and generates dynamic responses. It follows standard lifecycle methods:
  - `doGet(HttpServletRequest req, HttpServletResponse resp)`: Handles GET requests (retrieving data from MySQL).
  - `doPost(HttpServletRequest req, HttpServletResponse resp)`: Handles POST requests (inserting/updating records).
  - `doDelete(HttpServletRequest req, HttpServletResponse resp)`: Handles DELETE requests (deleting records).

- **Q: Where is `web.xml` located?**  
  *Answer:* Inside `web/WEB-INF/web.xml`. It declares each servlet (`<servlet>`) and maps its URL route pattern (`<servlet-mapping>`).

- **Q: How does this project run standalone without Tomcat installed?**  
  *Answer:* All business logic classes (`EmployeeServlet`, `AttendanceServlet`, `PayrollServlet`, `DashboardServlet`) extend `HttpServlet`. To make running easy for development and demonstration, `Main.java` includes a lightweight `ServletBridge` that routes incoming HTTP requests directly into `servlet.service(req, resp)`. At the same time, because these are genuine `HttpServlet` classes and `web.xml` is present, the project can also be deployed directly to Apache Tomcat.
