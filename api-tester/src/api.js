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
    throw new Error(msg);
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
  createStudent: (payload) => call('POST', '/api/students', payload),
  updateStudent: (id, payload) => call('PATCH', `/api/students/${id}`, payload),
  deleteStudent: (id) => call('DELETE', `/api/students/${id}`),
  getStudentAcademicHistory: (id) => listOr(`/api/students/${id}/academic-history`),
  assignAcademicRecord: (studentId, payload) => call('POST', `/api/students/${studentId}/academic-records`, payload),
  promoteStudent: (id, payload) => call('POST', `/api/students/${id}/promote`, payload),
  getStudentSiblings: (id) => listOr(`/api/students/${id}/siblings`),
  getStudentByAdmissionNo: (admissionNo) => call('GET', `/api/students/admission/${admissionNo}`),

  // ----- parents -----
  parents: (schoolId) => listOr(`/api/parents/school/${schoolId}`),
  createParent: (payload) => call('POST', '/api/parents', payload),
  updateParent: (id, payload) => call('PATCH', `/api/parents/${id}`, payload),
  deleteParent: (id) => call('DELETE', `/api/parents/${id}`),
  getParentById: (id) => call('GET', `/api/parents/${id}`),
  getParentByEmail: (email) => call('GET', `/api/parents/email/${email}`),
};
