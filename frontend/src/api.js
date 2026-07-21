/**
 * API integration client for Spring Boot REST endpoints.
 * Handles HTTP requests with standard error handling and automatic response parsing.
 */

async function call(method, path, body) {
  const opts = { method, headers: {} };
  if (body != null) {
    opts.headers['Content-Type'] = 'application/json';
    opts.body = JSON.stringify(body);
  }
  try {
    const res = await fetch(path, opts);
    const text = await res.text();
    let data;
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
    if (!res.ok) {
      const msg = data && data.message ? data.message : `${res.status} ${res.statusText}`;
      const err = new Error(msg);
      err.status = res.status;
      err.data = data;
      throw err;
    }
    return data;
  } catch (err) {
    console.warn(`API call failed [${method} ${path}]:`, err.message);
    throw err;
  }
}

const listOr = (path) => call('GET', path).catch(() => []);

export const api = {
  // ----- Core: Schools -----
  getSchools: () => listOr('/api/schools'),
  getActiveSchools: () => listOr('/api/schools/active'),
  getSchoolById: (id) => call('GET', `/api/schools/${id}`),
  getSchoolBySubdomain: (subdomain) => call('GET', `/api/schools/subdomain/${subdomain}`),
  createSchool: (payload) => call('POST', '/api/schools', payload),
  updateSchool: (id, payload) => call('PATCH', `/api/schools/${id}`, payload),
  deleteSchool: (id) => call('DELETE', `/api/schools/${id}`),

  // ----- Core: Academic Years -----
  getAcademicYears: (schoolId = 'SCH-001') => listOr(`/api/academic-years/school/${schoolId}`),
  getAcademicYearByName: (schoolId, name) => call('GET', `/api/academic-years/school/${schoolId}/name/${encodeURIComponent(name)}`),
  createAcademicYear: (payload) => call('POST', '/api/academic-years', payload),
  updateAcademicYear: (id, payload) => call('PUT', `/api/academic-years/${id}`, payload),
  deleteAcademicYear: (id) => call('DELETE', `/api/academic-years/${id}`),

  // ----- Core: Announcements & Notifications -----
  getAnnouncements: () => listOr('/api/announcements'),
  getAnnouncementsBySchool: (schoolId = 'SCH-001') => listOr(`/api/announcements/school/${schoolId}`),
  getAnnouncementsByTarget: (schoolId = 'SCH-001', target) => listOr(`/api/announcements/school/${schoolId}/target/${encodeURIComponent(target)}`),
  createAnnouncement: (payload) => call('POST', '/api/announcements', payload),
  updateAnnouncement: (id, payload) => call('PUT', `/api/announcements/${id}`, payload),
  deleteAnnouncement: (id) => call('DELETE', `/api/announcements/${id}`),

  getNotifications: () => listOr('/api/notifications'),
  getNotificationsByRecipient: (recipientId) => listOr(`/api/notifications/recipient/${recipientId}`),
  getNotificationsBySchool: (schoolId = 'SCH-001') => listOr(`/api/notifications/school/${schoolId}`),
  createNotification: (payload) => call('POST', '/api/notifications', payload),
  markNotificationSent: (id) => call('PUT', `/api/notifications/${id}/mark-sent`),
  deleteNotification: (id) => call('DELETE', `/api/notifications/${id}`),

  // ----- Staff -----
  getStaff: (schoolId = 'SCH-001') => listOr(`/api/staff/school/${schoolId}`),
  getStaffById: (id) => call('GET', `/api/staff/${id}`),
  getStaffByEmployeeId: (empId) => call('GET', `/api/staff/employee/${empId}`),
  createStaff: (payload) => call('POST', '/api/staff', payload),
  updateStaff: (id, payload) => call('PATCH', `/api/staff/${id}`, payload),
  deleteStaff: (id) => call('DELETE', `/api/staff/${id}`),

  // ----- Students & Guardians -----
  getStudents: (schoolId = 'SCH-001') => listOr(`/api/students/school/${schoolId}`),
  getStudentsByYear: (schoolId = 'SCH-001', year) => listOr(`/api/students/school/${schoolId}/academic-year/${encodeURIComponent(year)}`),
  getStudentByAdmissionNo: (admissionNo) => call('GET', `/api/students/admission/${admissionNo}`),
  createStudent: (rawPayload) => {
    const payload = {
      schoolId: rawPayload.schoolId || "6a474d2517e9c40cf971ccc2",
      name: rawPayload.name || rawPayload.studentName || "Student",
      admissionNo: rawPayload.admissionNo || rawPayload.admissionNumber || `ADM-2026-${Math.floor(1000 + Math.random() * 9000)}`,
      dob: rawPayload.dob || "2015-06-19",
      gender: (rawPayload.gender || "MALE").toUpperCase(),
      bloodGroup: rawPayload.bloodGroup || rawPayload.medicalBloodGroup || "AB+",
      photoUrl: rawPayload.photoUrl || "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d",
      walletId: rawPayload.walletId || `wallet-${Date.now()}`,
      medicalRecordId: rawPayload.medicalRecordId || `medical-${Date.now()}`,
      status: (rawPayload.status || "ACTIVE").toUpperCase(),
      admissionDate: rawPayload.admissionDate || rawPayload.joinedDate || new Date().toISOString().split("T")[0],
      guardians: Array.isArray(rawPayload.guardians) && rawPayload.guardians.length > 0 ? rawPayload.guardians : [
        {
          name: rawPayload.parentName || "Parent",
          relation: (rawPayload.parentRelation || "MOTHER").toUpperCase(),
          phone: rawPayload.parentPhone || "+61-400-555-666",
          email: rawPayload.parentEmail || "parent@example.com",
          address: rawPayload.address || "9 Oak Ave",
          occupation: rawPayload.parentOccupation || "Parent",
          primary: true,
          emergencyContact: true,
          pickupApproved: true,
          portalAccess: true
        }
      ],
      currentAcademicRecord: rawPayload.currentAcademicRecord || {
        academicYear: rawPayload.academicYear || "2026-2027",
        studentNo: rawPayload.studentNo || `STD-${Math.floor(100 + Math.random() * 900)}`,
        rollNo: rawPayload.rollNo || "9C-01",
        classDocId: rawPayload.classDocId || rawPayload.grade || "6a4ca1b1ccce8dc055ebd633",
        sectionNo: rawPayload.sectionNo || rawPayload.sectionId || "section-a",
        hostelRoomNo: rawPayload.hostelRoomNo || rawPayload.hostelRoomId || (rawPayload.hostelRoomNumber ? `room-${rawPayload.hostelRoomNumber}` : undefined),
        status: "ACTIVE"
      }
    };
    return call('POST', '/api/students', payload);
  },
  createStudentFromAdmission: (payload) => call('POST', '/api/students/from-admission', payload),
  updateStudent: (id, payload) => call('PATCH', `/api/students/${id}`, payload),
  promoteStudent: (id, payload) => call('POST', `/api/students/${id}/promote`, payload),
  deleteStudent: (id) => call('DELETE', `/api/students/${id}`),

  getGuardians: (schoolId = 'SCH-001') => listOr(`/api/guardians/school/${schoolId}`),
  getGuardianById: (id) => call('GET', `/api/guardians/${id}`),
  createGuardian: (payload) => call('POST', '/api/guardians', payload),
  updateGuardian: (id, payload) => call('PATCH', `/api/guardians/${id}`, payload),
  deleteGuardian: (id) => call('DELETE', `/api/guardians/${id}`),
  addGuardianLink: (studentId, payload) => call('POST', `/api/students/${studentId}/guardians`, payload),
  removeGuardianLink: (studentId, guardianId) => call('DELETE', `/api/students/${studentId}/guardians/${guardianId}`),

  // ----- Academics: Classes, Attendance, Homework, Results, Discipline, Timetables, Medical -----
  getClasses: (schoolId = 'SCH-001') => listOr(`/api/classes/school/${schoolId}`),
  createClass: (payload) => call('POST', '/api/classes', payload),
  addSectionToClass: (classId, sections) => call('POST', `/api/classes/${classId}/sections`, { sections }),
  deleteClass: (classId) => call('DELETE', `/api/classes/${classId}`),

  getAttendance: (schoolId = 'SCH-001') => listOr(`/api/attendance/school/${schoolId}`),
  createAttendance: (payload) => call('POST', '/api/attendance', payload),
  updateAttendance: (id, payload) => call('PATCH', `/api/attendance/${id}`, payload),
  deleteAttendance: (id) => call('DELETE', `/api/attendance/${id}`),

  getHomework: (schoolId = 'SCH-001') => listOr(`/api/homework/school/${schoolId}`),
  createHomework: (payload) => call('POST', '/api/homework', payload),
  assignHomework: (id, scope, studentAssignments) => call('POST', `/api/homework/${id}/assign`, { assignmentScope: scope, studentAssignments }),
  submitHomework: (id, studentId, text, fileUrl) => call('POST', `/api/homework/${id}/submit/${studentId}`, { submissionText: text, submissionFileUrl: fileUrl }),
  gradeHomework: (id, studentId, obtainedMarks, feedback) => call('POST', `/api/homework/${id}/grade/${studentId}`, { obtainedMarks, feedback }),
  updateHomework: (id, payload) => call('PATCH', `/api/homework/${id}`, payload),
  deleteHomework: (id) => call('DELETE', `/api/homework/${id}`),

  getAcademicResults: (schoolId = 'SCH-001') => listOr(`/api/academic-results/school/${schoolId}`),
  createAcademicResult: (payload) => call('POST', '/api/academic-results', payload),
  updateAcademicResult: (id, payload) => call('PATCH', `/api/academic-results/${id}`, payload),
  deleteAcademicResult: (id) => call('DELETE', `/api/academic-results/${id}`),

  getDisciplineLogs: (schoolId = 'SCH-001') => listOr(`/api/discipline-logs/school/${schoolId}`),
  createDisciplineLog: (payload) => call('POST', '/api/discipline-logs', payload),
  updateDisciplineLog: (id, payload) => call('PATCH', `/api/discipline-logs/${id}`, payload),
  deleteDisciplineLog: (id) => call('DELETE', `/api/discipline-logs/${id}`),

  getTimetables: (schoolId = 'SCH-001', start, end) => listOr(`/api/timetables/school/${schoolId}/range?startDate=${start}&endDate=${end}`),
  createTimetable: (payload) => call('POST', '/api/timetables', payload),

  getMedicalRecords: (schoolId = 'SCH-001') => listOr(`/api/medical-records/school/${schoolId}`),
  createMedicalRecord: (payload) => call('POST', '/api/medical-records', payload),
  updateMedicalRecord: (id, payload) => call('PATCH', `/api/medical-records/${id}`, payload),
  deleteMedicalRecord: (id) => call('DELETE', `/api/medical-records/${id}`),

  // ----- Finance: Fees & Wallets -----
  getFees: (schoolId = 'SCH-001') => listOr(`/api/fees/school/${schoolId}`),
  createFee: (payload) => call('POST', '/api/fees', payload),
  updateFee: (id, payload) => call('PATCH', `/api/fees/${id}`, payload),
  deleteFee: (id) => call('DELETE', `/api/fees/${id}`),
  recordFeePayment: (feeId, { amount, paymentMode, remarks, collectedBy }) =>
    call('POST', `/api/fees/${feeId}/payments`, { amount, paymentMode, remarks, collectedBy }),
  getFeePayments: (feeId) => listOr(`/api/fees/${feeId}/payments`),
  getFeePaymentsByStudent: (studentId) => listOr(`/api/fees/payments/student/${studentId}`),

  getWallet: (studentId) => call('GET', `/api/wallets/student/${studentId}`),
  creditWallet: (studentId, amount, remarks) => call('POST', `/api/wallets/student/${studentId}/credit`, { amount, remarks }),
  debitWallet: (studentId, amount, remarks) => call('POST', `/api/wallets/student/${studentId}/debit`, { amount, remarks }),
  getWalletTransactions: (studentId) => listOr(`/api/wallets/student/${studentId}/transactions`),

  // ----- CRM: Inquiries & Admissions -----
  getInquiries: (schoolId = 'SCH-001') => listOr(`/api/inquiries/school/${schoolId}`),
  getInquiryById: (id) => call('GET', `/api/inquiries/${id}`),
  createInquiry: (payload) => call('POST', '/api/inquiries', payload),
  updateInquiry: (id, payload) => call('PATCH', `/api/inquiries/${id}`, payload),
  recordInquiryFollowUp: (id, entry) => call('POST', `/api/inquiries/${id}/follow-ups`, entry),
  deleteInquiry: (id) => call('DELETE', `/api/inquiries/${id}`),

  getAdmissions: (schoolId = 'SCH-001') => listOr(`/api/admissions/school/${schoolId}`),
  getAdmissionById: (id) => call('GET', `/api/admissions/${id}`),
  createAdmission: (payload) => call('POST', '/api/admissions', payload),
  updateAdmission: (id, payload) => call('PATCH', `/api/admissions/${id}`, payload),
  convertAdmissionToStudent: (id, studentPayload) => call('POST', `/api/admissions/${id}/convert`, studentPayload),
  deleteAdmission: (id) => call('DELETE', `/api/admissions/${id}`),
};
