// Friendly API layer for the School Admin console.
// All calls are same-origin; Vite proxies /api to the backend (see vite.config.js).

async function call(method, path, body) {
  const opts = { method, headers: {} };
  if (body != null) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(path, opts);
  const text = await res.text();
  let data;
  try { data = JSON.parse(text); } catch { data = text; }
  if (!res.ok) {
    const msg = data && data.message ? data.message : `${res.status} ${res.statusText}`;
    const err = new Error(msg);
    err.status = res.status;
    err.data = data; // structured error body (e.g. existingGuardianId on a 409)
    throw err;
  }
  return data;
}

const listOr = (p) => call('GET', p).catch(() => []);

export const api = {
  // ----- schools & context -----
  schools: () => listOr('/api/schools'),
  academicYears: (schoolId) => listOr(`/api/academic-years/school/${schoolId}`),
  classes: (schoolId) => listOr(`/api/classes/school/${schoolId}`),
  classesByYear: (schoolId, year) => listOr(`/api/classes/school/${schoolId}/academic-year/${encodeURIComponent(year)}`),
  staff: (schoolId) => listOr(`/api/staff/school/${schoolId}`),
  createStaff: (payload) => call('POST', '/api/staff', payload),
  updateStaff: (id, payload) => call('PATCH', `/api/staff/${id}`, payload),
  deleteStaff: (id) => call('DELETE', `/api/staff/${id}`),
  getStaffById: (id) => call('GET', `/api/staff/${id}`),
  getStaffByEmployeeNo: (employeeNo) =>
    call('GET', `/api/staff/employee/${encodeURIComponent(employeeNo)}`),

  // ----- schools admin -----
  createSchool: (payload) => call('POST', '/api/schools', payload),
  getAllSchools: () => listOr('/api/schools'),
  getActiveSchools: () => listOr('/api/schools/active'),
  getSchoolById: (id) => call('GET', `/api/schools/${id}`),
  getSchoolBySubdomain: (subdomain) => call('GET', `/api/schools/subdomain/${subdomain}`),
  updateSchool: (id, payload) => call('PATCH', `/api/schools/${id}`, payload),
  deleteSchool: (id) => call('DELETE', `/api/schools/${id}`),

  // ----- academic year & holidays -----
  createAcademicYear: (payload) => call('POST', '/api/academic-years', payload),
  updateAcademicYear: (id, payload) => call('PUT', `/api/academic-years/${id}`, payload),
  deleteAcademicYear: (id) => call('DELETE', `/api/academic-years/${id}`),
  addHolidays: (id, arr) => call('POST', `/api/academic-years/${id}/holidays`, arr),
  addWeeklyOff: (id, dayOfWeek) => call('POST', `/api/academic-years/${id}/weekly-offs?dayOfWeek=${dayOfWeek}`),
  removeHoliday: (id, date, name) =>
    call('DELETE', `/api/academic-years/${id}/holidays?date=${date}${name ? '&name=' + encodeURIComponent(name) : ''}`),
  removeWeeklyOff: (id, dayOfWeek) => call('DELETE', `/api/academic-years/${id}/weekly-offs?dayOfWeek=${dayOfWeek}`),
  
  getAcademicYearBySchoolAndName: (schoolId, name) => call('GET', `/api/academic-years/school/${schoolId}/name/${encodeURIComponent(name)}`),
  getAcademicYearForDate: (schoolId, date) => call('GET', `/api/academic-years/school/${schoolId}/for-date/${date}`),
  getHolidaysInRange: (id, start, end) => listOr(`/api/academic-years/${id}/holidays/range?start=${start}&end=${end}`),
  isHoliday: (id, date) => call('GET', `/api/academic-years/${id}/holidays/check?date=${date}`),
  getHolidaysInRangeByName: (schoolId, name, start, end) => listOr(`/api/academic-years/school/${schoolId}/name/${encodeURIComponent(name)}/holidays/range?start=${start}&end=${end}`),
  isHolidayByName: (schoolId, name, date) => call('GET', `/api/academic-years/school/${schoolId}/name/${encodeURIComponent(name)}/holidays/check?date=${date}`),

  // ----- announcements -----
  createAnnouncement: (payload) => call('POST', '/api/announcements', payload),
  getAllAnnouncements: () => listOr('/api/announcements'),
  getAnnouncementById: (id) => call('GET', `/api/announcements/${id}`),
  getAnnouncementsBySchool: (schoolId) => listOr(`/api/announcements/school/${schoolId}`),
  getAnnouncementsBySchoolAndTarget: (schoolId, target) => listOr(`/api/announcements/school/${schoolId}/target/${encodeURIComponent(target)}`),
  updateAnnouncement: (id, payload) => call('PUT', `/api/announcements/${id}`, payload),
  deleteAnnouncement: (id) => call('DELETE', `/api/announcements/${id}`),

  // ----- notifications -----
  createNotification: (payload) => call('POST', '/api/notifications', payload),
  getAllNotifications: () => listOr('/api/notifications'),
  getNotificationById: (id) => call('GET', `/api/notifications/${id}`),
  getNotificationsByRecipient: (recipientId) => listOr(`/api/notifications/recipient/${recipientId}`),
  getUnsentNotificationsByRecipient: (recipientId) => listOr(`/api/notifications/recipient/${recipientId}/unsent`),
  getNotificationsBySchool: (schoolId) => listOr(`/api/notifications/school/${schoolId}`),
  markNotificationAsSent: (id) => call('PUT', `/api/notifications/${id}/mark-sent`),
  deleteNotification: (id) => call('DELETE', `/api/notifications/${id}`),

  // ----- classes -----
  createClass: (payload) => call('POST', '/api/classes', payload),
  addSection: (classId, sections) => call('POST', `/api/classes/${classId}/sections`, { sections }),
  deleteClass: (classId) => call('DELETE', `/api/classes/${classId}`),

  // ----- timetables -----
  createTimetable: (payload) => call('POST', '/api/timetables', payload),
  timetableRange: (schoolId, start, end) =>
    listOr(`/api/timetables/school/${schoolId}/range?startDate=${start}&endDate=${end}`),
  sectionSchedule: (schoolId, classId, section, start, end) =>
    listOr(`/api/timetables/school/${schoolId}/class/${classId}/section/${encodeURIComponent(section)}?startDate=${start}&endDate=${end}`),
  teacherSchedule: (schoolId, teacherId, start, end) =>
    listOr(`/api/timetables/school/${schoolId}/teacher/${teacherId}?startDate=${start}&endDate=${end}`),
  clearSection: (schoolId, classId, section, start, end) =>
    call('DELETE', `/api/timetables/school/${schoolId}/class/${classId}/section/${encodeURIComponent(section)}?startDate=${start}&endDate=${end}`),

  // ----- students & academic records -----
  students: (schoolId) => listOr(`/api/students/school/${schoolId}`),
  studentsByYear: (schoolId, year) => listOr(`/api/students/school/${schoolId}/academic-year/${encodeURIComponent(year)}`),
  createStudent: (payload) => call('POST', '/api/students', payload),
  createStudentFromAdmission: (payload) => call('POST', '/api/students/from-admission', payload),
  updateStudent: (id, payload) => call('PATCH', `/api/students/${id}`, payload),
  deleteStudent: (id) => call('DELETE', `/api/students/${id}`),
  getStudentAcademicHistory: (id) => listOr(`/api/students/${id}/academic-history`),
  assignAcademicRecord: (studentId, payload) => call('POST', `/api/students/${studentId}/academic-records`, payload),
  promoteStudent: (id, payload) => call('POST', `/api/students/${id}/promote`, payload),
  getStudentSiblings: (id) => listOr(`/api/students/${id}/siblings`),
  getStudentByAdmissionNo: (admissionNo) => call('GET', `/api/students/admission/${admissionNo}`),

  // ----- guardians (many-to-many family) -----
  guardians: (schoolId) => listOr(`/api/guardians/school/${schoolId}`),
  getGuardian: (id) => call('GET', `/api/guardians/${id}`),
  createGuardian: (payload) => call('POST', '/api/guardians', payload),
  updateGuardian: (id, payload) => call('PATCH', `/api/guardians/${id}`, payload),
  deleteGuardian: (id) => call('DELETE', `/api/guardians/${id}`),
  studentsByGuardian: (guardianDocsId) => listOr(`/api/students/guardian/${guardianDocsId}`),
  addGuardianLink: (studentId, link) => call('POST', `/api/students/${studentId}/guardians`, link),
  removeGuardianLink: (studentId, guardianDocsId) => call('DELETE', `/api/students/${studentId}/guardians/${guardianDocsId}`),

  // ----- academics: attendance -----
  attendance: (schoolId) => listOr(`/api/attendance/school/${schoolId}`),
  attendanceByYear: (schoolId, year) => listOr(`/api/attendance/school/${schoolId}/academic-year/${encodeURIComponent(year)}`),
  createAttendance: (payload) => call('POST', '/api/attendance', payload),
  updateAttendance: (id, payload) => call('PATCH', `/api/attendance/${id}`, payload),
  deleteAttendance: (id) => call('DELETE', `/api/attendance/${id}`),

  // ----- academics: homework -----
  homework: (schoolId) => listOr(`/api/homework/school/${schoolId}`),
  homeworkByYear: (schoolId, year) => listOr(`/api/homework/school/${schoolId}/academic-year/${encodeURIComponent(year)}`),
  createHomework: (payload) => call('POST', '/api/homework', payload),
  assignHomework: (id, scope, studentAssignments) => call('POST', `/api/homework/${id}/assign`, { assignmentScope: scope, studentAssignments }),
  submitHomework: (id, studentId, text, fileUrl) => call('POST', `/api/homework/${id}/submit/${studentId}`, { submissionText: text, submissionFileUrl: fileUrl }),
  gradeHomework: (id, studentId, obtainedMarks, feedback) => call('POST', `/api/homework/${id}/grade/${studentId}`, { obtainedMarks, feedback }),
  updateHomework: (id, payload) => call('PATCH', `/api/homework/${id}`, payload),
  deleteHomework: (id) => call('DELETE', `/api/homework/${id}`),

  // ----- academics: results -----
  academicResults: (schoolId) => listOr(`/api/academic-results/school/${schoolId}`),
  academicResultsByYear: (schoolId, year) => listOr(`/api/academic-results/school/${schoolId}/academic-year/${encodeURIComponent(year)}`),
  createAcademicResult: (payload) => call('POST', '/api/academic-results', payload),
  updateAcademicResult: (id, payload) => call('PATCH', `/api/academic-results/${id}`, payload),
  deleteAcademicResult: (id) => call('DELETE', `/api/academic-results/${id}`),

  // ----- academics: discipline -----
  disciplineLogs: (schoolId) => listOr(`/api/discipline-logs/school/${schoolId}`),
  disciplineLogsByYear: (schoolId, year) => listOr(`/api/discipline-logs/school/${schoolId}/academic-year/${encodeURIComponent(year)}`),
  createDisciplineLog: (payload) => call('POST', '/api/discipline-logs', payload),
  updateDisciplineLog: (id, payload) => call('PATCH', `/api/discipline-logs/${id}`, payload),
  deleteDisciplineLog: (id) => call('DELETE', `/api/discipline-logs/${id}`),

  // ----- academics: medical records -----
  medicalRecords: (schoolId) => listOr(`/api/medical-records/school/${schoolId}`),
  createMedicalRecord: (payload) => call('POST', '/api/medical-records', payload),
  updateMedicalRecord: (id, payload) => call('PATCH', `/api/medical-records/${id}`, payload),
  deleteMedicalRecord: (id) => call('DELETE', `/api/medical-records/${id}`),

  // ----- finance: fees -----
  fees: (schoolId) => listOr(`/api/fees/school/${schoolId}`),
  feesByYear: (schoolId, year) => listOr(`/api/fees/school/${schoolId}/academic-year/${encodeURIComponent(year)}`),
  createFee: (payload) => call('POST', '/api/fees', payload),
  updateFee: (id, payload) => call('PATCH', `/api/fees/${id}`, payload),
  deleteFee: (id) => call('DELETE', `/api/fees/${id}`),
  // Unified fee collection: mode = CASH | WALLET | ONLINE | CHEQUE
  recordPayment: (id, { amount, paymentMode, remarks, collectedBy }) =>
    call('POST', `/api/fees/${id}/payments`, { amount, paymentMode, remarks, collectedBy }),
  feePayments: (id) => listOr(`/api/fees/${id}/payments`),
  feePaymentsByStudent: (studentId) => listOr(`/api/fees/payments/student/${studentId}`),

  // ----- crm: inquiries -----
  inquiries: (schoolId) => listOr(`/api/inquiries/school/${schoolId}`),
  inquiriesByStatus: (schoolId, status) => listOr(`/api/inquiries/school/${schoolId}/status/${status}`),
  inquiryFollowUps: (schoolId, asOf) => listOr(`/api/inquiries/school/${schoolId}/follow-ups${asOf ? `?asOf=${asOf}` : ''}`),
  getInquiry: (id) => call('GET', `/api/inquiries/${id}`),
  createInquiry: (payload) => call('POST', '/api/inquiries', payload),
  updateInquiry: (id, payload) => call('PATCH', `/api/inquiries/${id}`, payload),
  recordInquiryFollowUp: (id, entry) => call('POST', `/api/inquiries/${id}/follow-ups`, entry),
  deleteInquiry: (id) => call('DELETE', `/api/inquiries/${id}`),

  // ----- crm: admissions -----
  admissions: (schoolId) => listOr(`/api/admissions/school/${schoolId}`),
  admissionsByYear: (schoolId, year) => listOr(`/api/admissions/school/${schoolId}/academic-year/${encodeURIComponent(year)}`),
  admissionsByStatus: (schoolId, status) => listOr(`/api/admissions/school/${schoolId}/status/${status}`),
  admissionsByInquiry: (inquiryId) => listOr(`/api/admissions/inquiry/${inquiryId}`),
  getAdmission: (id) => call('GET', `/api/admissions/${id}`),
  createAdmission: (payload) => call('POST', '/api/admissions', payload),
  updateAdmission: (id, payload) => call('PATCH', `/api/admissions/${id}`, payload),
  convertAdmission: (id, studentPayload) => call('POST', `/api/admissions/${id}/convert`, studentPayload),
  deleteAdmission: (id) => call('DELETE', `/api/admissions/${id}`),

  // ----- finance: student wallets -----
  getWallet: (studentId) => call('GET', `/api/wallets/student/${studentId}`),
  creditWallet: (studentId, amount, remarks) => call('POST', `/api/wallets/student/${studentId}/credit`, { amount, remarks }),
  debitWallet: (studentId, amount, remarks) => call('POST', `/api/wallets/student/${studentId}/debit`, { amount, remarks }),
  walletTransactions: (studentId) => listOr(`/api/wallets/student/${studentId}/transactions`),
};
