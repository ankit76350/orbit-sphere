/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */
import {
  mockSchools,
  generateStaff,
  generateStudents,
  generateHostels,
  initialInquiries,
  generateWalletTransactions,
  generateFeeInvoices,
  generateDisciplineViolations,
  mockHomework,
  generateAcademicResults,
  generateAttendanceHistory,
  initialInventory,
  initialRoutes,
  initialVisitors,
  initialOutPasses,
  initialAnnouncements
} from "./mockData";
const KEYS = {
  SCHOOLS: "erp_schools",
  STUDENTS: "erp_students",
  STAFF: "erp_staff",
  HOSTELS: "erp_hostels",
  CRM: "erp_crm",
  TRANSACTIONS: "erp_transactions",
  FEES: "erp_fees",
  DISCIPLINE: "erp_discipline",
  ATTENDANCE: "erp_attendance",
  INVENTORY: "erp_inventory",
  ROUTES: "erp_routes",
  VISITORS: "erp_visitors",
  OUTPASSES: "erp_outpasses",
  ANNOUNCEMENTS: "erp_announcements",
  ACTIONS_LOG: "erp_audit_logs",
  AUTH_USER: "erp_auth_user"
};
export function logAction(userId, userName, role, action, details) {
  const logs = getLogs();
  const newLog = {
    id: `log-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
    userId,
    userName,
    userRole: role,
    action,
    timestamp: (/* @__PURE__ */ new Date()).toISOString().replace("T", " ").substring(0, 19),
    details
  };
  logs.unshift(newLog);
  localStorage.setItem(KEYS.ACTIONS_LOG, JSON.stringify(logs.slice(0, 100)));
}
export function initializeStorage() {
  if (!localStorage.getItem(KEYS.SCHOOLS)) {
    localStorage.setItem(KEYS.SCHOOLS, JSON.stringify(mockSchools));
  }
  const staff = generateStaff();
  if (!localStorage.getItem(KEYS.STAFF)) {
    localStorage.setItem(KEYS.STAFF, JSON.stringify(staff));
  }
  const students = generateStudents();
  if (!localStorage.getItem(KEYS.STUDENTS)) {
    localStorage.setItem(KEYS.STUDENTS, JSON.stringify(students));
  }
  const studentList = JSON.parse(localStorage.getItem(KEYS.STUDENTS));
  const staffList = JSON.parse(localStorage.getItem(KEYS.STAFF));
  if (!localStorage.getItem(KEYS.HOSTELS)) {
    localStorage.setItem(KEYS.HOSTELS, JSON.stringify(generateHostels(studentList)));
  }
  if (!localStorage.getItem(KEYS.CRM)) {
    localStorage.setItem(KEYS.CRM, JSON.stringify(initialInquiries));
  }
  if (!localStorage.getItem(KEYS.TRANSACTIONS)) {
    localStorage.setItem(KEYS.TRANSACTIONS, JSON.stringify(generateWalletTransactions(studentList)));
  }
  if (!localStorage.getItem(KEYS.FEES)) {
    localStorage.setItem(KEYS.FEES, JSON.stringify(generateFeeInvoices(studentList)));
  }
  if (!localStorage.getItem(KEYS.DISCIPLINE)) {
    localStorage.setItem(KEYS.DISCIPLINE, JSON.stringify(generateDisciplineViolations(studentList)));
  }
  if (!localStorage.getItem(KEYS.INVENTORY)) {
    localStorage.setItem(KEYS.INVENTORY, JSON.stringify(initialInventory));
  }
  if (!localStorage.getItem(KEYS.ROUTES)) {
    localStorage.setItem(KEYS.ROUTES, JSON.stringify(initialRoutes));
  }
  if (!localStorage.getItem(KEYS.VISITORS)) {
    localStorage.setItem(KEYS.VISITORS, JSON.stringify(initialVisitors));
  }
  if (!localStorage.getItem(KEYS.OUTPASSES)) {
    localStorage.setItem(KEYS.OUTPASSES, JSON.stringify(initialOutPasses));
  }
  if (!localStorage.getItem(KEYS.ANNOUNCEMENTS)) {
    localStorage.setItem(KEYS.ANNOUNCEMENTS, JSON.stringify(initialAnnouncements));
  }
  if (!localStorage.getItem(KEYS.ATTENDANCE)) {
    localStorage.setItem(KEYS.ATTENDANCE, JSON.stringify(generateAttendanceHistory(studentList, staffList)));
  }
  if (!localStorage.getItem("erp_homework")) {
    localStorage.setItem("erp_homework", JSON.stringify(mockHomework));
  }
  if (!localStorage.getItem("erp_results")) {
    localStorage.setItem("erp_results", JSON.stringify(generateAcademicResults(studentList)));
  }
  if (!localStorage.getItem(KEYS.ACTIONS_LOG)) {
    const defaultLogs = [
      {
        id: "log-seed-1",
        userId: "admin-seed",
        userName: "System Architect",
        userRole: "Super Admin",
        action: "System Booted",
        timestamp: "2026-05-30 06:48:00",
        details: "Initial Boarding School ERP LocalStorage databases successfully synchronized"
      }
    ];
    localStorage.setItem(KEYS.ACTIONS_LOG, JSON.stringify(defaultLogs));
  }
}
export function getSchools() {
  return JSON.parse(localStorage.getItem(KEYS.SCHOOLS) || "[]");
}
export function saveSchools(schools) {
  localStorage.setItem(KEYS.SCHOOLS, JSON.stringify(schools));
}
export function getStudents() {
  return JSON.parse(localStorage.getItem(KEYS.STUDENTS) || "[]");
}
export function saveStudents(students) {
  localStorage.setItem(KEYS.STUDENTS, JSON.stringify(students));
}
export function getStaff() {
  return JSON.parse(localStorage.getItem(KEYS.STAFF) || "[]");
}
export function saveStaff(staff) {
  localStorage.setItem(KEYS.STAFF, JSON.stringify(staff));
}
export function getHostels() {
  return JSON.parse(localStorage.getItem(KEYS.HOSTELS) || "[]");
}
export function saveHostels(hostels) {
  localStorage.setItem(KEYS.HOSTELS, JSON.stringify(hostels));
}
export function getInquiries() {
  return JSON.parse(localStorage.getItem(KEYS.CRM) || "[]");
}
export function saveInquiries(inquiries) {
  localStorage.setItem(KEYS.CRM, JSON.stringify(inquiries));
}
export function getTransactions() {
  return JSON.parse(localStorage.getItem(KEYS.TRANSACTIONS) || "[]");
}
export function saveTransactions(txs) {
  localStorage.setItem(KEYS.TRANSACTIONS, JSON.stringify(txs));
}
export function getFees() {
  return JSON.parse(localStorage.getItem(KEYS.FEES) || "[]");
}
export function saveFees(fees) {
  localStorage.setItem(KEYS.FEES, JSON.stringify(fees));
}
export function getDiscipline() {
  return JSON.parse(localStorage.getItem(KEYS.DISCIPLINE) || "[]");
}
export function saveDiscipline(violations) {
  localStorage.setItem(KEYS.DISCIPLINE, JSON.stringify(violations));
}
export function getInventory() {
  return JSON.parse(localStorage.getItem(KEYS.INVENTORY) || "[]");
}
export function saveInventory(items) {
  localStorage.setItem(KEYS.INVENTORY, JSON.stringify(items));
}
export function getRoutes() {
  return JSON.parse(localStorage.getItem(KEYS.ROUTES) || "[]");
}
export function saveRoutes(routes) {
  localStorage.setItem(KEYS.ROUTES, JSON.stringify(routes));
}
export function getVisitors() {
  return JSON.parse(localStorage.getItem(KEYS.VISITORS) || "[]");
}
export function saveVisitors(visitors) {
  localStorage.setItem(KEYS.VISITORS, JSON.stringify(visitors));
}
export function getOutPasses() {
  return JSON.parse(localStorage.getItem(KEYS.OUTPASSES) || "[]");
}
export function saveOutPasses(outpasses) {
  localStorage.setItem(KEYS.OUTPASSES, JSON.stringify(outpasses));
}
export function getAnnouncements() {
  return JSON.parse(localStorage.getItem(KEYS.ANNOUNCEMENTS) || "[]");
}
export function saveAnnouncements(ann) {
  localStorage.setItem(KEYS.ANNOUNCEMENTS, JSON.stringify(ann));
}
export function getAttendance() {
  return JSON.parse(localStorage.getItem(KEYS.ATTENDANCE) || "[]");
}
export function saveAttendance(records) {
  localStorage.setItem(KEYS.ATTENDANCE, JSON.stringify(records));
}
export function getHomework() {
  return JSON.parse(localStorage.getItem("erp_homework") || "[]");
}
export function saveHomework(hw) {
  localStorage.setItem("erp_homework", JSON.stringify(hw));
}
export function getResults() {
  return JSON.parse(localStorage.getItem("erp_results") || "[]");
}
export function saveResults(res) {
  localStorage.setItem("erp_results", JSON.stringify(res));
}
export function getVehicles() {
  return JSON.parse(localStorage.getItem("erp_vehicles") || JSON.stringify(initialVisitors.slice(0, 3).map((v, i) => ({
    id: `veh-${i}`,
    driverName: v.visitorName,
    plateNumber: `TX-80${i}-CHIP`,
    vehicleDesc: "Delivery Sprinter Van",
    purpose: v.purpose,
    entryTime: v.entryTime
  }))));
}
export function saveVehicles(vehicles) {
  localStorage.setItem("erp_vehicles", JSON.stringify(vehicles));
}
export function getLogs() {
  return JSON.parse(localStorage.getItem(KEYS.ACTIONS_LOG) || "[]");
}
export function getAuditLogs() {
  return getLogs();
}
export function getMessMenu() {
  return JSON.parse(localStorage.getItem("erp_mess_menu") || "[]");
}
export function saveMessMenu(menu) {
  localStorage.setItem("erp_mess_menu", JSON.stringify(menu));
}
export function getInfirmary() {
  return JSON.parse(localStorage.getItem("erp_infirmary") || "[]");
}
export function saveInfirmary(records) {
  localStorage.setItem("erp_infirmary", JSON.stringify(records));
}
export function logoutUser() {
  localStorage.removeItem(KEYS.AUTH_USER);
}
export function getAuthUser() {
  const data = localStorage.getItem(KEYS.AUTH_USER);
  if (!data) return null;
  return JSON.parse(data);
}
export function setAuthUser(user) {
  if (user) {
    localStorage.setItem(KEYS.AUTH_USER, JSON.stringify(user));
  } else {
    localStorage.removeItem(KEYS.AUTH_USER);
  }
}
export function deductWallet(studentId, amount, category, remarks, operName, operRole) {
  const students = getStudents();
  const index = students.findIndex((st) => st.id === studentId);
  if (index === -1) return false;
  if (students[index].walletBalance < amount && category !== "Fine Deduction") {
    return false;
  }
  students[index].walletBalance -= amount;
  saveStudents(students);
  const txs = getTransactions();
  const tx = {
    id: `tx-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
    studentId,
    studentName: students[index].name,
    type: "Debit",
    category,
    amount,
    date: (/* @__PURE__ */ new Date()).toISOString().split("T")[0],
    remarks
  };
  txs.unshift(tx);
  saveTransactions(txs);
  logAction(operName, operName, operRole, "Wallet Debit Triggered", `Deducted $${amount} from student ${students[index].name} (${category}). Remarks: ${remarks}`);
  return true;
}
export function creditWallet(studentId, amount, remarks, operName, operRole) {
  const students = getStudents();
  const index = students.findIndex((st) => st.id === studentId);
  if (index === -1) return false;
  students[index].walletBalance += amount;
  saveStudents(students);
  const txs = getTransactions();
  const tx = {
    id: `tx-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
    studentId,
    studentName: students[index].name,
    type: "Credit",
    category: "Parent Topup",
    amount,
    date: (/* @__PURE__ */ new Date()).toISOString().split("T")[0],
    remarks
  };
  txs.unshift(tx);
  saveTransactions(txs);
  logAction(operName, operName, operRole, "Wallet Credit Triggered", `Credited $${amount} to student ${students[index].name}. Remarks: ${remarks}`);
  return true;
}
export function payInvoice(invoiceId, paymentAmount, method, operName, operRole) {
  const invoices = getFees();
  const idx = invoices.findIndex((inv) => inv.id === invoiceId);
  if (idx === -1) return false;
  const invoice = invoices[idx];
  const remaining = invoice.amount - invoice.paidAmount;
  const pay = Math.min(paymentAmount, remaining);
  if (pay <= 0) return true;
  invoice.paidAmount += pay;
  if (invoice.paidAmount >= invoice.amount) {
    invoice.status = "Paid";
  } else {
    invoice.status = "Partially Paid";
  }
  if (!invoice.paymentHistory) invoice.paymentHistory = [];
  invoice.paymentHistory.push({
    date: (/* @__PURE__ */ new Date()).toISOString().split("T")[0],
    amount: pay,
    method,
    receiptNumber: `REC-${Math.floor(1e5 + Math.random() * 9e5)}`
  });
  invoices[idx] = invoice;
  saveFees(invoices);
  logAction(operName, operName, operRole, "Fee Invoice Paid", `Registered payment of $${pay} on Invoice #${invoiceId} under ${invoice.studentName}`);
  return true;
}
