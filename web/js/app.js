/**
 * app.js
 * ------
 * Smart Employee Attendance & Payroll Management System
 * Pure Vanilla JavaScript (Zero Frameworks, No Libraries!)
 * 
 * Beginner Concept:
 * Uses modern standard browser APIs:
 * - `fetch()` for HTTP GET/POST/DELETE to communicate with Java backend
 * - `document.getElementById` and `document.querySelector` to update HTML
 * - Event listeners for user clicks and form submissions
 */

// Global State
let allEmployees = [];
let allAttendance = [];
let allPayroll = [];

// ========================================================
// INITIALIZATION
// ========================================================
document.addEventListener("DOMContentLoaded", () => {
    // Start live clock
    updateClock();
    setInterval(updateClock, 1000);

    // Set today's date in attendance date picker
    const today = new Date().toISOString().split("T")[0];
    const datePicker = document.getElementById("attendanceDatePicker");
    if (datePicker) datePicker.value = today;

    // Set default month & year in payroll filters
    const now = new Date();
    const currentMonth = (now.getMonth() + 1).toString();
    const currentYear = now.getFullYear().toString();
    
    const monthSelect = document.getElementById("payrollMonthSelect");
    const yearSelect = document.getElementById("payrollYearSelect");
    if (monthSelect) monthSelect.value = currentMonth;
    if (yearSelect) yearSelect.value = currentYear;

    // Set today's date for dashboard snapshot
    const dDate = document.getElementById("dashboardDateText");
    if (dDate) dDate.textContent = today;

    // Initial data load
    loadDashboard();
    loadEmployees();
    loadAttendance();
    loadPayroll();
});

// Live Clock
function updateClock() {
    const el = document.getElementById("liveClock");
    if (el) {
        const now = new Date();
        el.textContent = now.toLocaleDateString("en-IN", {
            weekday: "short",
            day: "numeric",
            month: "short",
            year: "numeric"
        }) + " | " + now.toLocaleTimeString();
    }
}

// ========================================================
// TAB SWITCHING (Single Page Application feel)
// ========================================================
function switchTab(tabName) {
    // Deactivate all tab buttons and tab contents
    document.querySelectorAll(".nav-btn").forEach(btn => btn.classList.remove("active"));
    document.querySelectorAll(".tab-content").forEach(content => content.classList.remove("active"));

    // Activate the targeted tab
    const targetContent = document.getElementById("tab-" + tabName);
    if (targetContent) {
        targetContent.classList.add("active");
    }

    // Highlight the active navigation button
    const buttons = document.querySelectorAll(".nav-btn");
    buttons.forEach(btn => {
        if (btn.getAttribute("onclick") && btn.getAttribute("onclick").includes(tabName)) {
            btn.classList.add("active");
        }
    });

    // Refresh data depending on tab
    if (tabName === "dashboard") loadDashboard();
    if (tabName === "employees") loadEmployees();
    if (tabName === "attendance") loadAttendance();
    if (tabName === "payroll") loadPayroll();
}

// ========================================================
// 1. DASHBOARD MODULE
// ========================================================
async function loadDashboard() {
    try {
        const res = await fetch("/api/dashboard");
        if (!res.ok) throw new Error("Failed to load dashboard metrics");
        const data = await res.json();

        // Update KPI values
        document.getElementById("statTotalEmployees").textContent = data.totalEmployees || 0;
        document.getElementById("statPresentToday").textContent = data.presentToday || 0;
        document.getElementById("statAbsentToday").textContent = data.absentToday || 0;
        document.getElementById("statLeaveToday").textContent = data.leaveToday || 0;
        document.getElementById("statMonthlyPayroll").textContent = "₹" + Number(data.monthlyPayroll || 0).toLocaleString("en-IN", { minimumFractionDigits: 2 });

        if (data.totalEmployees > 0) {
            const pct = Math.round((data.presentToday / data.totalEmployees) * 100);
            document.getElementById("statPresentRate").textContent = pct + "% attendance today";
        }

        // Load today's attendance snapshot into dashboard table
        const today = new Date().toISOString().split("T")[0];
        const attRes = await fetch("/api/attendance?date=" + today);
        if (attRes.ok) {
            const attData = await attRes.json();
            renderDashboardAttendance(attData);
        }
    } catch (err) {
        console.error("Dashboard error:", err);
    }
}

function renderDashboardAttendance(list) {
    const tbody = document.getElementById("dashboardAttendanceTable");
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted">No attendance data recorded for today yet. Click 'Mark All Present Today' above to quickly mark attendance.</td></tr>`;
        return;
    }

    tbody.innerHTML = list.map(item => `
        <tr>
            <td><strong>${escapeHtml(item.emp_code)}</strong></td>
            <td>${escapeHtml(item.name)}</td>
            <td>${escapeHtml(item.department)}</td>
            <td>${getStatusBadge(item.status)}</td>
            <td>${escapeHtml(item.check_in || "-")}</td>
            <td>${escapeHtml(item.check_out || "-")}</td>
            <td>${escapeHtml(item.remarks || "-")}</td>
        </tr>
    `).join("");
}

// ========================================================
// 2. EMPLOYEE MANAGEMENT MODULE
// ========================================================
async function loadEmployees() {
    try {
        const res = await fetch("/api/employees");
        if (!res.ok) throw new Error("Could not fetch employees");
        allEmployees = await res.json();
        renderEmployeesTable(allEmployees);
    } catch (err) {
        showToast(err.message, "error");
    }
}

function renderEmployeesTable(list) {
    const tbody = document.getElementById("employeesTableBody");
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center">No employees found. Click '+ Add New Employee' to get started.</td></tr>`;
        return;
    }

    tbody.innerHTML = list.map(emp => `
        <tr>
            <td><strong>${escapeHtml(emp.emp_code)}</strong></td>
            <td><strong>${escapeHtml(emp.name)}</strong></td>
            <td>${escapeHtml(emp.email)}</td>
            <td>${escapeHtml(emp.phone || "-")}</td>
            <td><span class="badge" style="background:#e0f2fe; color:#0369a1;">${escapeHtml(emp.department)}</span></td>
            <td>${escapeHtml(emp.designation)}</td>
            <td><strong>₹${Number(emp.basic_salary).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</strong></td>
            <td>${escapeHtml(emp.joining_date)}</td>
            <td class="text-center">
                <button class="btn btn-sm btn-outline" onclick='editEmployee(${JSON.stringify(emp).replace(/'/g, "&apos;")})'>✏️ Edit</button>
                <button class="btn btn-sm btn-outline" style="color:var(--danger); border-color:#fca5a5;" onclick="deleteEmployee(${emp.id}, '${escapeHtml(emp.name)}')">🗑️ Delete</button>
            </td>
        </tr>
    `).join("");
}

function filterEmployees() {
    const term = (document.getElementById("employeeSearchInput").value || "").toLowerCase().trim();
    if (!term) {
        renderEmployeesTable(allEmployees);
        return;
    }
    const filtered = allEmployees.filter(e => 
        (e.name && e.name.toLowerCase().includes(term)) ||
        (e.emp_code && e.emp_code.toLowerCase().includes(term)) ||
        (e.department && e.department.toLowerCase().includes(term)) ||
        (e.designation && e.designation.toLowerCase().includes(term))
    );
    renderEmployeesTable(filtered);
}

function openAddEmployeeModal() {
    document.getElementById("modalEmployeeTitle").textContent = "Add New Employee";
    document.getElementById("empFormId").value = "0";
    document.getElementById("employeeForm").reset();
    document.getElementById("empFormJoinDate").value = new Date().toISOString().split("T")[0];
    document.getElementById("employeeModal").classList.add("show");
}

function editEmployee(emp) {
    document.getElementById("modalEmployeeTitle").textContent = "Edit Employee (" + emp.emp_code + ")";
    document.getElementById("empFormId").value = emp.id;
    document.getElementById("empFormCode").value = emp.emp_code;
    document.getElementById("empFormName").value = emp.name;
    document.getElementById("empFormEmail").value = emp.email;
    document.getElementById("empFormPhone").value = emp.phone || "";
    document.getElementById("empFormDept").value = emp.department;
    document.getElementById("empFormDesignation").value = emp.designation;
    document.getElementById("empFormSalary").value = emp.basic_salary;
    document.getElementById("empFormJoinDate").value = emp.joining_date;
    document.getElementById("employeeModal").classList.add("show");
}

function closeEmployeeModal() {
    document.getElementById("employeeModal").classList.remove("show");
}

async function handleEmployeeSubmit(e) {
    e.preventDefault();

    const empData = {
        id: document.getElementById("empFormId").value,
        emp_code: document.getElementById("empFormCode").value.trim(),
        name: document.getElementById("empFormName").value.trim(),
        email: document.getElementById("empFormEmail").value.trim(),
        phone: document.getElementById("empFormPhone").value.trim(),
        department: document.getElementById("empFormDept").value,
        designation: document.getElementById("empFormDesignation").value.trim(),
        basic_salary: document.getElementById("empFormSalary").value,
        joining_date: document.getElementById("empFormJoinDate").value
    };

    try {
        const res = await fetch("/api/employees", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(empData)
        });

        const result = await res.json();
        if (!res.ok) throw new Error(result.error || "Failed to save employee");

        showToast(result.message || "Employee saved successfully!", "success");
        closeEmployeeModal();
        loadEmployees();
        loadDashboard();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function deleteEmployee(id, name) {
    if (!confirm(`Are you sure you want to delete employee "${name}"?\n(This will also remove their attendance and payroll records)`)) {
        return;
    }

    try {
        const res = await fetch(`/api/employees?id=${id}`, { method: "DELETE" });
        const result = await res.json();
        if (!res.ok) throw new Error(result.error || "Failed to delete employee");

        showToast(`Employee "${name}" deleted.`, "info");
        loadEmployees();
        loadDashboard();
        loadAttendance();
        loadPayroll();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ========================================================
// 3. SMART ATTENDANCE MODULE
// ========================================================
async function loadAttendance() {
    const dateInput = document.getElementById("attendanceDatePicker");
    const date = dateInput ? dateInput.value : new Date().toISOString().split("T")[0];

    try {
        const res = await fetch("/api/attendance?date=" + date);
        if (!res.ok) throw new Error("Could not fetch attendance");
        allAttendance = await res.json();
        renderAttendanceTable(allAttendance, date);
    } catch (err) {
        showToast(err.message, "error");
    }
}

function renderAttendanceTable(list, selectedDate) {
    const tbody = document.getElementById("attendanceTableBody");
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center">No employee records found. Add employees first.</td></tr>`;
        return;
    }

    tbody.innerHTML = list.map(item => `
        <tr id="attRow_${item.emp_id}">
            <td><strong>${escapeHtml(item.emp_code)}</strong></td>
            <td><strong>${escapeHtml(item.name)}</strong></td>
            <td>${escapeHtml(item.department)}</td>
            <td>
                <select id="status_${item.emp_id}" class="att-status-select" style="font-weight:600;">
                    <option value="Present" ${item.status === "Present" ? "selected" : ""}>✅ Present</option>
                    <option value="Absent" ${item.status === "Absent" ? "selected" : ""}>❌ Absent</option>
                    <option value="Half Day" ${item.status === "Half Day" ? "selected" : ""}>🌓 Half Day</option>
                    <option value="Leave" ${item.status === "Leave" ? "selected" : ""}>🏖️ Leave</option>
                    ${item.status === "Not Marked" ? '<option value="Not Marked" selected disabled>⚠️ Not Marked</option>' : ''}
                </select>
            </td>
            <td>
                <input type="text" id="in_${item.emp_id}" value="${item.check_in !== '-' ? escapeHtml(item.check_in) : '09:00 AM'}" style="width: 105px;">
            </td>
            <td>
                <input type="text" id="out_${item.emp_id}" value="${item.check_out !== '-' ? escapeHtml(item.check_out) : '05:00 PM'}" style="width: 105px;">
            </td>
            <td>
                <input type="text" id="rem_${item.emp_id}" value="${escapeHtml(item.remarks || 'Regular')}" placeholder="Remarks" style="width: 130px;">
            </td>
            <td class="text-center">
                <button class="btn btn-sm btn-primary" onclick="saveAttendanceRow(${item.emp_id}, '${selectedDate}')">Save</button>
            </td>
        </tr>
    `).join("");
}

async function saveAttendanceRow(empId, date) {
    const statusSelect = document.getElementById(`status_${empId}`);
    let status = statusSelect ? statusSelect.value : "Present";
    if (status === "Not Marked") status = "Present";

    const checkIn = document.getElementById(`in_${empId}`).value;
    const checkOut = document.getElementById(`out_${empId}`).value;
    const remarks = document.getElementById(`rem_${empId}`).value;

    const payload = {
        emp_id: empId.toString(),
        date: date,
        status: status,
        check_in: checkIn,
        check_out: checkOut,
        remarks: remarks
    };

    try {
        const res = await fetch("/api/attendance", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const result = await res.json();
        if (!res.ok) throw new Error(result.error || "Failed to save attendance");

        showToast("Attendance updated!", "success");
        loadDashboard();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// 1-Click Smart Action: Mark All Selected Date Present
async function markAllSelectedDatePresent() {
    const dateInput = document.getElementById("attendanceDatePicker");
    const date = dateInput ? dateInput.value : new Date().toISOString().split("T")[0];

    try {
        const res = await fetch("/api/attendance/mark-all", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ date: date })
        });
        const result = await res.json();
        if (!res.ok) throw new Error(result.error || "Failed to mark all present");

        showToast(result.message || "Marked all employees present!", "success");
        loadAttendance();
        loadDashboard();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// Quick action from dashboard
async function quickMarkAllToday() {
    const today = new Date().toISOString().split("T")[0];
    try {
        const res = await fetch("/api/attendance/mark-all", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ date: today })
        });
        const result = await res.json();
        if (!res.ok) throw new Error(result.error);

        showToast("Quick Attendance: All employees marked present today!", "success");
        loadDashboard();
        loadAttendance();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ========================================================
// 4. SMART PAYROLL MODULE
// ========================================================
async function loadPayroll() {
    const month = document.getElementById("payrollMonthSelect").value;
    const year = document.getElementById("payrollYearSelect").value;

    try {
        const res = await fetch(`/api/payroll?month=${month}&year=${year}`);
        if (!res.ok) throw new Error("Could not fetch payroll records");
        allPayroll = await res.json();
        renderPayrollTable(allPayroll);
    } catch (err) {
        showToast(err.message, "error");
    }
}

function renderPayrollTable(list) {
    const tbody = document.getElementById("payrollTableBody");
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="10" class="text-center">No payroll records generated for this month yet.<br><button class="btn btn-sm btn-primary mt-3" onclick="generateSmartPayroll()">⚡ Click to Auto-Generate Payroll</button></td></tr>`;
        return;
    }

    tbody.innerHTML = list.map(item => {
        const totalAllowances = Number(item.hra || 0) + Number(item.da || 0);
        const totalDeductions = (Number(item.absent_days || 0) * (Number(item.basic_salary) / Number(item.total_working_days))) + Number(item.pf_deduction || 0) + Number(item.tax_deduction || 0);

        return `
            <tr>
                <td><strong>${escapeHtml(item.emp_code)}</strong></td>
                <td><strong>${escapeHtml(item.emp_name)}</strong></td>
                <td>${escapeHtml(item.department)}</td>
                <td>₹${Number(item.basic_salary).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</td>
                <td><span style="color:var(--success);">+₹${totalAllowances.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span></td>
                <td><strong>${item.present_days}</strong> / ${item.total_working_days}</td>
                <td><span style="color:var(--danger);">-₹${Math.round(totalDeductions).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span></td>
                <td><strong style="font-size:1rem; color:#15803d;">₹${Number(item.net_salary).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</strong></td>
                <td>
                    <span class="badge ${item.status === 'Paid' ? 'badge-paid' : 'badge-pending'}" 
                          title="Click to toggle Paid/Pending"
                          onclick="togglePayrollStatus(${item.id}, '${item.status}')">
                        ${item.status === 'Paid' ? '✓ Paid' : '⏳ Pending'}
                    </span>
                </td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline" onclick='openPayslipModal(${JSON.stringify(item).replace(/'/g, "&apos;")})'>📄 Payslip</button>
                </td>
            </tr>
        `;
    }).join("");
}

async function generateSmartPayroll() {
    const month = document.getElementById("payrollMonthSelect").value;
    const year = document.getElementById("payrollYearSelect").value;
    const monthName = document.getElementById("payrollMonthSelect").options[document.getElementById("payrollMonthSelect").selectedIndex].text;

    if (!confirm(`Run Smart Payroll calculation for ${monthName} ${year}?\nSalaries will be automatically computed based on recorded attendance, allowances, and statutory deductions.`)) {
        return;
    }

    try {
        showToast("Generating payroll... please wait", "info");
        const res = await fetch("/api/payroll/generate", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ month: month, year: year })
        });
        const result = await res.json();
        if (!res.ok) throw new Error(result.error || "Failed to generate payroll");

        showToast(result.message || "Payroll calculated successfully!", "success");
        loadPayroll();
        loadDashboard();
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function togglePayrollStatus(id, currentStatus) {
    const newStatus = currentStatus === "Paid" ? "Pending" : "Paid";
    try {
        const res = await fetch("/api/payroll/update-status", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ id: id.toString(), status: newStatus })
        });
        const result = await res.json();
        if (!res.ok) throw new Error(result.error);

        showToast(`Payroll status changed to ${newStatus}`, "info");
        loadPayroll();
    } catch (err) {
        showToast(err.message, "error");
    }
}

// ========================================================
// 5. PAYSLIP MODAL & PRINTING
// ========================================================
function openPayslipModal(item) {
    const monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
    const monthName = monthNames[item.month - 1] || item.month;

    const grossSalary = Number(item.basic_salary) + Number(item.hra) + Number(item.da);
    const absentLoss = Math.round(Number(item.absent_days) * (grossSalary / Number(item.total_working_days)));
    const totalDeductions = absentLoss + Number(item.pf_deduction) + Number(item.tax_deduction);

    const html = `
        <div class="payslip-card">
            <div class="payslip-header">
                <div class="payslip-company">SMART ENTERPRISES PVT. LTD.</div>
                <div class="subtitle">123 Technology Park, Silicon Corridor, Chennai, India</div>
                <div class="payslip-title">Salary Slip for ${monthName} ${item.year}</div>
            </div>

            <div class="payslip-meta-grid">
                <div>
                    <p><strong>Employee Name:</strong> ${escapeHtml(item.emp_name)}</p>
                    <p><strong>Employee Code:</strong> ${escapeHtml(item.emp_code)}</p>
                    <p><strong>Department:</strong> ${escapeHtml(item.department)}</p>
                </div>
                <div>
                    <p><strong>Designation:</strong> ${escapeHtml(item.designation)}</p>
                    <p><strong>Total Working Days:</strong> ${item.total_working_days} Days</p>
                    <p><strong>Effective Present:</strong> ${item.present_days} Days (Absent: ${item.absent_days} Days)</p>
                </div>
            </div>

            <div class="payslip-tables-grid">
                <!-- Earnings Column -->
                <div class="payslip-box">
                    <div class="payslip-box-title">EARNINGS (ALLOWANCES)</div>
                    <div class="payslip-row">
                        <span>Basic Salary</span>
                        <strong>₹${Number(item.basic_salary).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</strong>
                    </div>
                    <div class="payslip-row">
                        <span>House Rent Allowance (HRA 20%)</span>
                        <strong>₹${Number(item.hra).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</strong>
                    </div>
                    <div class="payslip-row">
                        <span>Dearness Allowance (DA 10%)</span>
                        <strong>₹${Number(item.da).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</strong>
                    </div>
                    <div class="payslip-row" style="background:#fafafa; font-weight:700;">
                        <span>Total Gross Salary</span>
                        <span>₹${grossSalary.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span>
                    </div>
                </div>

                <!-- Deductions Column -->
                <div class="payslip-box">
                    <div class="payslip-box-title">DEDUCTIONS</div>
                    <div class="payslip-row">
                        <span>Loss of Pay (Absenteeism)</span>
                        <strong>₹${absentLoss.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</strong>
                    </div>
                    <div class="payslip-row">
                        <span>Provident Fund (PF 12%)</span>
                        <strong>₹${Number(item.pf_deduction).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</strong>
                    </div>
                    <div class="payslip-row">
                        <span>Income Tax Deduction</span>
                        <strong>₹${Number(item.tax_deduction).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</strong>
                    </div>
                    <div class="payslip-row" style="background:#fafafa; font-weight:700;">
                        <span>Total Deductions</span>
                        <span style="color:var(--danger);">-₹${totalDeductions.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span>
                    </div>
                </div>
            </div>

            <div class="payslip-net">
                <div>
                    <span class="payslip-net-label">NET TAKE-HOME SALARY</span>
                    <p class="subtitle" style="margin-top:2px;">Disbursement Status: <strong>${item.status}</strong></p>
                </div>
                <div class="payslip-net-amount">₹${Number(item.net_salary).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</div>
            </div>

            <div style="margin-top: 1.5rem; text-align: center; font-size: 0.75rem; color: var(--text-muted);">
                This is a system-generated payslip computed by the Smart Employee Attendance & Payroll Management System.
            </div>
        </div>
    `;

    document.getElementById("payslipContent").innerHTML = html;
    document.getElementById("payslipModal").classList.add("show");
}

function closePayslipModal() {
    document.getElementById("payslipModal").classList.remove("show");
}

// ========================================================
// UTILITY FUNCTIONS (Beginner Helpers)
// ========================================================
function getStatusBadge(status) {
    if (!status) return `<span class="badge badge-unmarked">Not Marked</span>`;
    const s = status.toLowerCase();
    if (s.includes("present")) return `<span class="badge badge-present">Present</span>`;
    if (s.includes("absent")) return `<span class="badge badge-absent">Absent</span>`;
    if (s.includes("half")) return `<span class="badge badge-halfday">Half Day</span>`;
    if (s.includes("leave")) return `<span class="badge badge-leave">Leave</span>`;
    return `<span class="badge badge-unmarked">${escapeHtml(status)}</span>`;
}

function showToast(message, type = "info") {
    const container = document.getElementById("toastContainer");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    
    let icon = "ℹ️";
    if (type === "success") icon = "✅";
    if (type === "error") icon = "❌";

    toast.innerHTML = `<span>${icon}</span> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transform = "translateY(10px)";
        toast.style.transition = "all 0.3s ease";
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function escapeHtml(str) {
    if (str === null || str === undefined) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
