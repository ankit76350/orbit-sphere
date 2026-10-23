/**
 * The 24 features a plan can grant, mirrored from `FeatureCode` on the backend.
 *
 * MIRRORED, WHICH MEANS IT CAN DRIFT — and the drift is one-directional and safe: a feature added
 * to the enum and not here is simply not offered on this screen, where a feature here that the
 * enum does not have is a `400 INVALID_VALUE` naming the row and listing what is accepted. So a
 * stale copy fails loudly on the way in rather than quietly granting something.
 *
 * `metric` IS WHAT A LIMIT WOULD COUNT, and its presence is the useful half: a feature with no
 * metric has nothing to count, and sending it a limit is `400 FEATURE_NOT_MEASURABLE`. The
 * number is never sent — the API copies the metric off the feature itself and freezes it on the
 * plan, so that a feature's metric changing later cannot rewrite what a school already bought.
 */
export const FEATURES = [
  ['STUDENT_MANAGEMENT', 'Student management', 'ACTIVE_STUDENTS'],
  ['ACADEMICS', 'Academic structure'],
  ['ATTENDANCE', 'Attendance'],
  ['TIMETABLE', 'Timetable'],
  ['EXAMINATIONS', 'Examinations'],
  ['HOMEWORK', 'Homework'],
  ['FEE_MANAGEMENT', 'Fee management'],
  ['PAYROLL', 'Payroll', 'ACTIVE_STAFF'],
  ['STAFF_MANAGEMENT', 'Staff management', 'ACTIVE_STAFF'],
  ['ADMISSIONS_CRM', 'Admissions'],
  ['TRANSPORT', 'Transport', 'VEHICLES'],
  ['LIBRARY', 'Library', 'LIBRARY_TITLES'],
  ['HOSTEL', 'Hostel', 'HOSTEL_BEDS'],
  ['MESS', 'Mess'],
  ['HEALTH', 'Health'],
  ['FRONT_OFFICE', 'Front office'],
  ['INVENTORY', 'Inventory'],
  ['PROCUREMENT', 'Procurement'],
  ['FACILITIES', 'Facilities'],
  ['NOTIFICATIONS', 'Notifications', 'SMS_MESSAGES'],
  ['DOCUMENTS', 'Documents', 'STORAGE_MEGABYTES'],
  ['GALLERY', 'Gallery', 'STORAGE_MEGABYTES'],
  ['FEEDBACK', 'Feedback'],
  ['STUDENT_LIFE', 'Student life'],
].map(([code, label, metric]) => ({ code, label, metric: metric ?? null }))

/** What a limit counts, in words. */
export const METRIC_LABEL = {
  ACTIVE_STUDENTS: 'students',
  ACTIVE_STAFF: 'staff',
  USER_ACCOUNTS: 'user accounts',
  VEHICLES: 'vehicles',
  HOSTEL_BEDS: 'beds',
  LIBRARY_TITLES: 'titles',
  STORAGE_MEGABYTES: 'MB',
  SMS_MESSAGES: 'SMS messages',
  EMAIL_MESSAGES: 'emails',
}

export const OVERAGE_POLICIES = ['BLOCK', 'WARN', 'ALLOW', 'CHARGE']
