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

  // ----- academic year & holidays -----
  createAcademicYear: (payload) => call('POST', '/api/academic-years', payload),
  updateAcademicYear: (id, payload) => call('PUT', `/api/academic-years/${id}`, payload),
  deleteAcademicYear: (id) => call('DELETE', `/api/academic-years/${id}`),
  addHolidays: (id, arr) => call('POST', `/api/academic-years/${id}/holidays`, arr),
  addWeeklyOff: (id, dayOfWeek) => call('POST', `/api/academic-years/${id}/weekly-offs?dayOfWeek=${dayOfWeek}`),
  removeHoliday: (id, date, name) =>
    call('DELETE', `/api/academic-years/${id}/holidays?date=${date}${name ? '&name=' + encodeURIComponent(name) : ''}`),
  removeWeeklyOff: (id, dayOfWeek) => call('DELETE', `/api/academic-years/${id}/weekly-offs?dayOfWeek=${dayOfWeek}`),

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
};
