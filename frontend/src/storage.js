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
  initialAnnouncements,
  initialBooks,
  initialVehicles,
  initialDrivers,
  initialTransportAllocations,
  initialReviewCycles,
  initialTeacherReviews,
  initialStudentReviews,
  initialTeacherPerformanceReviews,
  initialAlumniProfiles,
  initialAlumniEvents,
  initialAlumniDonations,
  initialMentorshipPrograms,
  initialJobPostings,
  initialDocumentTemplates,
  initialGeneratedDocuments,
  initialIdCards,
  initialDocumentApprovals,
  initialDocumentSignatures
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
  BOOKS: "erp_books",
  TRANSPORT_VEHICLES: "erp_transport_vehicles_v2",
  DRIVERS: "erp_drivers_v2",
  TRANSPORT_ALLOCATIONS: "erp_transport_allocations_v2",
  TRANSPORT_ATTENDANCE: "erp_transport_attendance_v2",
  REVIEW_CYCLES: "erp_review_cycles_v2",
  TEACHER_REVIEWS: "erp_teacher_reviews_v2",
  STUDENT_REVIEWS: "erp_student_reviews_v2",
  TEACHER_PERFORMANCE_REVIEWS: "erp_teacher_performance_reviews_v2",
  ALUMNI_PROFILES: "erp_alumni_profiles_v2",
  ALUMNI_EVENTS: "erp_alumni_events_v2",
  ALUMNI_DONATIONS: "erp_alumni_donations_v2",
  MENTORSHIP_PROGRAMS: "erp_mentorship_programs_v2",
  JOB_POSTINGS: "erp_job_postings_v2",
  DOCUMENT_TEMPLATES: "erp_document_templates_v2",
  GENERATED_DOCUMENTS: "erp_generated_documents_v2",
  ID_CARDS: "erp_id_cards_v2",
  DOCUMENT_APPROVALS: "erp_document_approvals_v2",
  DOCUMENT_SIGNATURES: "erp_document_signatures_v2",
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
  if (!localStorage.getItem(KEYS.BOOKS)) {
    localStorage.setItem(KEYS.BOOKS, JSON.stringify(initialBooks));
  }
  if (!localStorage.getItem(KEYS.TRANSPORT_VEHICLES)) {
    localStorage.setItem(KEYS.TRANSPORT_VEHICLES, JSON.stringify(initialVehicles));
  }
  if (!localStorage.getItem(KEYS.DRIVERS)) {
    localStorage.setItem(KEYS.DRIVERS, JSON.stringify(initialDrivers));
  }
  if (!localStorage.getItem(KEYS.TRANSPORT_ALLOCATIONS)) {
    localStorage.setItem(KEYS.TRANSPORT_ALLOCATIONS, JSON.stringify(initialTransportAllocations));
  }
  if (!localStorage.getItem(KEYS.TRANSPORT_ATTENDANCE)) {
    localStorage.setItem(KEYS.TRANSPORT_ATTENDANCE, JSON.stringify([]));
  }
  if (!localStorage.getItem(KEYS.REVIEW_CYCLES)) {
    localStorage.setItem(KEYS.REVIEW_CYCLES, JSON.stringify(initialReviewCycles));
  }
  if (!localStorage.getItem(KEYS.TEACHER_REVIEWS)) {
    localStorage.setItem(KEYS.TEACHER_REVIEWS, JSON.stringify(initialTeacherReviews));
  }
  if (!localStorage.getItem(KEYS.STUDENT_REVIEWS)) {
    localStorage.setItem(KEYS.STUDENT_REVIEWS, JSON.stringify(initialStudentReviews));
  }
  if (!localStorage.getItem(KEYS.TEACHER_PERFORMANCE_REVIEWS)) {
    localStorage.setItem(KEYS.TEACHER_PERFORMANCE_REVIEWS, JSON.stringify(initialTeacherPerformanceReviews));
  }
  if (!localStorage.getItem(KEYS.ALUMNI_PROFILES)) {
    localStorage.setItem(KEYS.ALUMNI_PROFILES, JSON.stringify(initialAlumniProfiles));
  }
  if (!localStorage.getItem(KEYS.ALUMNI_EVENTS)) {
    localStorage.setItem(KEYS.ALUMNI_EVENTS, JSON.stringify(initialAlumniEvents));
  }
  if (!localStorage.getItem(KEYS.ALUMNI_DONATIONS)) {
    localStorage.setItem(KEYS.ALUMNI_DONATIONS, JSON.stringify(initialAlumniDonations));
  }
  if (!localStorage.getItem(KEYS.MENTORSHIP_PROGRAMS)) {
    localStorage.setItem(KEYS.MENTORSHIP_PROGRAMS, JSON.stringify(initialMentorshipPrograms));
  }
  if (!localStorage.getItem(KEYS.JOB_POSTINGS)) {
    localStorage.setItem(KEYS.JOB_POSTINGS, JSON.stringify(initialJobPostings));
  }
  if (!localStorage.getItem(KEYS.DOCUMENT_TEMPLATES)) {
    localStorage.setItem(KEYS.DOCUMENT_TEMPLATES, JSON.stringify(initialDocumentTemplates));
  }
  if (!localStorage.getItem(KEYS.GENERATED_DOCUMENTS)) {
    localStorage.setItem(KEYS.GENERATED_DOCUMENTS, JSON.stringify(initialGeneratedDocuments));
  }
  if (!localStorage.getItem(KEYS.ID_CARDS)) {
    localStorage.setItem(KEYS.ID_CARDS, JSON.stringify(initialIdCards));
  }
  if (!localStorage.getItem(KEYS.DOCUMENT_APPROVALS)) {
    localStorage.setItem(KEYS.DOCUMENT_APPROVALS, JSON.stringify(initialDocumentApprovals));
  }
  if (!localStorage.getItem(KEYS.DOCUMENT_SIGNATURES)) {
    localStorage.setItem(KEYS.DOCUMENT_SIGNATURES, JSON.stringify(initialDocumentSignatures));
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

export function getBooks() {
  return JSON.parse(localStorage.getItem(KEYS.BOOKS) || "[]");
}

export function saveBooks(books) {
  localStorage.setItem(KEYS.BOOKS, JSON.stringify(books));
}

export function getTransportVehicles() {
  return JSON.parse(localStorage.getItem(KEYS.TRANSPORT_VEHICLES) || "[]");
}

export function saveTransportVehicles(vehicles) {
  localStorage.setItem(KEYS.TRANSPORT_VEHICLES, JSON.stringify(vehicles));
}

export function getDrivers() {
  return JSON.parse(localStorage.getItem(KEYS.DRIVERS) || "[]");
}

export function saveDrivers(drivers) {
  localStorage.setItem(KEYS.DRIVERS, JSON.stringify(drivers));
}

export function getTransportAllocations() {
  return JSON.parse(localStorage.getItem(KEYS.TRANSPORT_ALLOCATIONS) || "[]");
}

export function saveTransportAllocations(allocations) {
  localStorage.setItem(KEYS.TRANSPORT_ALLOCATIONS, JSON.stringify(allocations));
}

export function getTransportAttendance() {
  return JSON.parse(localStorage.getItem(KEYS.TRANSPORT_ATTENDANCE) || "[]");
}

export function saveTransportAttendance(records) {
  localStorage.setItem(KEYS.TRANSPORT_ATTENDANCE, JSON.stringify(records));
}

export function getReviewCycles() {
  return JSON.parse(localStorage.getItem(KEYS.REVIEW_CYCLES) || "[]");
}

export function saveReviewCycles(cycles) {
  localStorage.setItem(KEYS.REVIEW_CYCLES, JSON.stringify(cycles));
}

export function getTeacherReviews() {
  return JSON.parse(localStorage.getItem(KEYS.TEACHER_REVIEWS) || "[]");
}

export function saveTeacherReviews(reviews) {
  localStorage.setItem(KEYS.TEACHER_REVIEWS, JSON.stringify(reviews));
}

export function getStudentReviews() {
  return JSON.parse(localStorage.getItem(KEYS.STUDENT_REVIEWS) || "[]");
}

export function saveStudentReviews(reviews) {
  localStorage.setItem(KEYS.STUDENT_REVIEWS, JSON.stringify(reviews));
}

export function getTeacherPerformanceReviews() {
  return JSON.parse(localStorage.getItem(KEYS.TEACHER_PERFORMANCE_REVIEWS) || "[]");
}

export function saveTeacherPerformanceReviews(reviews) {
  localStorage.setItem(KEYS.TEACHER_PERFORMANCE_REVIEWS, JSON.stringify(reviews));
}

export function getAlumniProfiles() {
  return JSON.parse(localStorage.getItem(KEYS.ALUMNI_PROFILES) || "[]");
}

export function saveAlumniProfiles(profiles) {
  localStorage.setItem(KEYS.ALUMNI_PROFILES, JSON.stringify(profiles));
}

export function getAlumniEvents() {
  return JSON.parse(localStorage.getItem(KEYS.ALUMNI_EVENTS) || "[]");
}

export function saveAlumniEvents(events) {
  localStorage.setItem(KEYS.ALUMNI_EVENTS, JSON.stringify(events));
}

export function getAlumniDonations() {
  return JSON.parse(localStorage.getItem(KEYS.ALUMNI_DONATIONS) || "[]");
}

export function saveAlumniDonations(donations) {
  localStorage.setItem(KEYS.ALUMNI_DONATIONS, JSON.stringify(donations));
}

export function getMentorshipPrograms() {
  return JSON.parse(localStorage.getItem(KEYS.MENTORSHIP_PROGRAMS) || "[]");
}

export function saveMentorshipPrograms(programs) {
  localStorage.setItem(KEYS.MENTORSHIP_PROGRAMS, JSON.stringify(programs));
}

export function getJobPostings() {
  return JSON.parse(localStorage.getItem(KEYS.JOB_POSTINGS) || "[]");
}

export function saveJobPostings(postings) {
  localStorage.setItem(KEYS.JOB_POSTINGS, JSON.stringify(postings));
}

export function getDocumentTemplates() {
  return JSON.parse(localStorage.getItem(KEYS.DOCUMENT_TEMPLATES) || "[]");
}

export function saveDocumentTemplates(templates) {
  localStorage.setItem(KEYS.DOCUMENT_TEMPLATES, JSON.stringify(templates));
}

export function getGeneratedDocuments() {
  return JSON.parse(localStorage.getItem(KEYS.GENERATED_DOCUMENTS) || "[]");
}

export function saveGeneratedDocuments(docs) {
  localStorage.setItem(KEYS.GENERATED_DOCUMENTS, JSON.stringify(docs));
}

export function getIdCards() {
  return JSON.parse(localStorage.getItem(KEYS.ID_CARDS) || "[]");
}

export function saveIdCards(cards) {
  localStorage.setItem(KEYS.ID_CARDS, JSON.stringify(cards));
}

export function getDocumentApprovals() {
  return JSON.parse(localStorage.getItem(KEYS.DOCUMENT_APPROVALS) || "[]");
}

export function saveDocumentApprovals(approvals) {
  localStorage.setItem(KEYS.DOCUMENT_APPROVALS, JSON.stringify(approvals));
}

export function getDocumentSignatures() {
  return JSON.parse(localStorage.getItem(KEYS.DOCUMENT_SIGNATURES) || "[]");
}

export function saveDocumentSignatures(sigs) {
  localStorage.setItem(KEYS.DOCUMENT_SIGNATURES, JSON.stringify(sigs));
}



