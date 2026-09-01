/**
 * Every endpoint the battleground knows about.
 *
 * Generated from postman/"Orbit Sphere — API.postman_collection.json" — the bodies, the query
 * parameters, the headers, the notes and every numbered test case come from there, unchanged.
 * What the collection cannot say (which fields are required, what comes back, which variable to
 * remember after a call) was read out of the controllers, the DTOs and the services.
 *
 * When the collection changes, the quickest way to update this file is to regenerate it rather
 * than hand-edit: everything here has a source in the repository.
 *
 * TENANT HEADER: every /schools/current/... endpoint needs X-School-Subdomain, because there is
 * no authentication yet and CurrentSchoolResolver reads the tenant from that header. It is set
 * on those requests already, pointing at {{createdSubdomain}}.
 */

const GROUP_CORE_ACADEMIC_YEAR = {
  id: "core-academic-year",
  module: "Core / Academic Year",
  endpoints: [
    {
      id: "create-academic-year",
      name: "Create Academic Year",
      method: "POST",
      path: "/schools/current/academic-years",
      status: 'live',
      summary: "Makes a year with an empty calendar. The name can never be changed afterwards.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years\` — creates an academic year.

### The rule that outranks everything else

**\`name\` can never be changed.** No rename endpoint exists and none may be added. Other
collections store the year's *name* as a string — \`"2026-2027"\` **is** the join key across
\`FeeInvoice\`, \`TransportTrip\`, \`FeedbackCampaign\` and dozens more. A rename would not fail and
would not cascade; every row would still look valid and you would find out when a report came
back empty.

That is also why the URL is keyed by name, not id.

### Holidays are not accepted here

A year is always created with an **empty calendar**. Holidays are their own resource with their
own endpoints (#20–#23). Sending a \`holidays\` array does nothing — the field is not on the
request, so it is ignored.

### Validated

\`name\` unique per school · \`startDate\` before \`endDate\` · 30–800 days · **no overlap with an
existing year**.

### The eight test cases are in the request body as comments
`,
      bodyNotes: `Needs the X-School-Subdomain header. Run Create School first.

 THE NAME CAN NEVER BE CHANGED. There is no rename endpoint and there must
 never be one. Other collections do not reference a year by id — they store
 this string in their own academicYear field. FeeInvoice, TransportTrip,
 FeedbackCampaign, FacilityInspection and dozens more. "2026-2027" IS the
 join key, so a rename would orphan all of them silently and every row would
 still look valid.

 HOLIDAYS ARE NOT ACCEPTED HERE. A year is always created with an empty
 calendar; the calendar has its own endpoints (#20 to #23). Sending a
 holidays array does nothing — the field is not on the request, so it is
 ignored rather than honoured.`,
      requiredFields: ["name", "startDate", "endDate"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "2026-2027",
  "startDate": "2026-04-01",
  "endDate": "2027-03-31"
}`,
      successStatus: 201,
      successNote: "Also sends a Location header: /schools/current/academic-years/{name}",
      responseFields: ["academicYearId", "name", "startDate", "endDate", "durationDays", "current", "holidayCount", "enrollmentEnabled", "resultsLocked", "nextStep"],
      captures: [
        { variable: "academicYearName", from: "name" },
      ],
      errors: [
        { status: 400, code: "INVALID_DATE_RANGE", when: "Backwards dates" },
        { status: 400, code: "IMPLAUSIBLE_DATE_RANGE", when: "A three-day \"year\"" },
        { status: 400, code: "VALIDATION_FAILED", when: "Missing name or dates" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "ACADEMIC_YEAR_NAME_TAKEN", when: "Duplicate name" },
        { status: 409, code: "ACADEMIC_YEAR_OVERLAP", when: "Overlapping dates" },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "CREATE A YEAR",
          expect: "201 Created",
          notes: `The body above.
    OUT: durationDays: 365, current: true, holidayCount: 0
    Header: Location: /schools/current/academic-years/2026-2027`,
          body: null,
        },
        {
          id: "02",
          name: "DUPLICATE NAME",
          expect: "409 Conflict",
          notes: `Send case 01 again.
    OUT: { "code": "ACADEMIC_YEAR_NAME_TAKEN" }`,
          body: null,
        },
        {
          id: "03",
          name: "OVERLAPPING DATES",
          expect: "409 Conflict",
          notes: `OUT: { "code": "ACADEMIC_YEAR_OVERLAP",
           "message": "These dates overlap '2026-2027' (...)" }

    Two years covering one day would give every "which year is this?" lookup
    two answers — and AcademicYear deliberately has NO current flag, so the
    dates are the only thing that can answer it.`,
          body: `{
  "name": "2027-2028",
  "startDate": "2027-03-01",
  "endDate": "2028-02-28"
}`,
        },
        {
          id: "04",
          name: "ADJACENT, NOT OVERLAPPING",
          expect: "201 Created",
          notes: `Ends 03-31, next starts 04-01. Allowed — the check is "one ends before
    the other starts", not four date comparisons.`,
          body: `{
  "name": "2027-2028",
  "startDate": "2027-04-01",
  "endDate": "2028-03-31"
}`,
        },
        {
          id: "05",
          name: "BACKWARDS DATES",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_DATE_RANGE" }`,
          body: `{
  "name": "bad-1",
  "startDate": "2030-04-01",
  "endDate": "2029-04-01"
}`,
        },
        {
          id: "06",
          name: "A THREE-DAY \"YEAR\"",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "IMPLAUSIBLE_DATE_RANGE" }
    Accepted range is 30 to 800 days. Outside that it is a typo, not a
    calendar.`,
          body: `{
  "name": "bad-2",
  "startDate": "2030-04-01",
  "endDate": "2030-04-03"
}`,
        },
        {
          id: "07",
          name: "MISSING NAME OR DATES",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "VALIDATION_FAILED",
           "fieldErrors": { "name": ["must not be blank"] } }`,
          body: `{
  "startDate": "2030-04-01",
  "endDate": "2031-03-31"
}`,
        },
        {
          id: "08",
          name: "A holidays ARRAY IS IGNORED",
          expect: "201 Created",
          notes: `OUT: holidayCount: 0. The field is not on the DTO, so it is dropped.
         Add holidays through their own endpoints instead.`,
          body: `{
  "name": "2029-2030",
  "startDate": "2029-04-01",
  "endDate": "2030-03-31",
  "holidays": [ { "name": "Diwali", "type": "FESTIVAL", "date": "2029-11-08" } ]
}`,
        },
      ],
    },
    {
      id: "update-academic-year-dates",
      name: "Update Academic Year Dates",
      method: "PATCH",
      path: "/schools/current/academic-years/{name}/dates",
      status: 'live',
      summary: "Moves a boundary. Shrinking past an existing holiday is refused.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/academic-years/{name}/dates\` — moves the boundaries.

Send either date or both; an omitted one is left alone. **\`name\` is not accepted** — it is
absent from the request, not optional.

### Extending is safe, shrinking is not

Pulling a boundary inwards can strand data outside the year that owns it. The service refuses a
range that would leave existing **holidays** outside.

**Only holidays are checked.** Attendance, invoices and trips reference the year by name string,
in collections with no repository yet — so a shrink can still orphan those silently.

### The seven test cases are in the request body as comments
`,
      bodyNotes: `The {name} in the URL is the year's name, e.g. 2026-2027.

 NAME IS NOT ACCEPTED HERE — not optional, ABSENT from the request. A year's
 name is the string every other collection stores to point at it.

 Send either date or both. An omitted date is left alone.`,
      optionalFields: ["startDate", "endDate"],
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "endDate": "2027-05-31"
}`,
      successStatus: 200,
      responseFields: ["academicYearId", "name", "startDate", "endDate", "durationDays", "current", "holidayCount", "nextStep"],
      captures: [
        { variable: "academicYearName", from: "name" },
      ],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "Empty body" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "Unknown year name" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "HOLIDAYS_OUTSIDE_NEW_RANGE", when: "Shrink past an existing holiday" },
        { status: 409, code: "ACADEMIC_YEAR_OVERLAP", when: "Shrink or grow into another year" },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "EXTEND THE END DATE",
          expect: "200 OK",
          notes: `The body above. Extending is usually harmless.
    OUT: durationDays grows; holidayCount unchanged.`,
          body: null,
        },
        {
          id: "02",
          name: "MOVE THE START FORWARD",
          expect: "200 OK",
          notes: `Allowed while every existing holiday still falls inside the new range.`,
          body: `{
  "startDate": "2026-06-01"
}`,
        },
        {
          id: "03",
          name: "MOVE BOTH AT ONCE",
          expect: "200 OK",
          notes: ``,
          body: `{
  "startDate": "2026-04-01",
  "endDate": "2027-03-31"
}`,
        },
        {
          id: "04",
          name: "SHRINK PAST AN EXISTING HOLIDAY",
          expect: "409 Conflict",
          notes: `OUT: { "code": "HOLIDAYS_OUTSIDE_NEW_RANGE",
           "message": "1 holiday(s) would fall outside the new dates,
                       starting with 'Diwali' on 2026-11-08..." }

    SHRINKING IS THE DANGEROUS DIRECTION. Pulling a boundary inwards strands
    data outside the year that owns it.`,
          body: `{
  "startDate": "2026-12-01"
}`,
        },
        {
          id: "05",
          name: "SHRINK OR GROW INTO ANOTHER YEAR",
          expect: "409 Conflict",
          notes: `OUT: { "code": "ACADEMIC_YEAR_OVERLAP" }
    Only if a following year exists — run case 04 of Create first.`,
          body: `{
  "endDate": "2027-06-30"
}`,
        },
        {
          id: "06",
          name: "EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" }`,
          body: `{
}`,
        },
        {
          id: "07",
          name: "UNKNOWN YEAR NAME",
          expect: "404 Not Found",
          notes: `Change the URL to .../1999-2000/dates
    OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }

 NOT CHECKED YET, AND IT SHOULD BE:
 Only HOLIDAYS are checked against the new range. Attendance records,
 invoices and trips reference a year by NAME STRING, in collections with no
 repository yet — so shrinking a year can still orphan those, silently.
 Wire that in when those repositories exist.`,
          body: null,
        },
      ],
    },
    {
      id: "replace-holiday-calendar",
      name: "Replace Holiday Calendar",
      method: "PUT",
      path: "/schools/current/academic-years/{name}/holidays",
      status: 'live',
      summary: "Replaces the whole calendar in one go. The bulk import case.",
      schoolSurface: true,
      docs: `**PUT** \`/schools/current/academic-years/{name}/holidays\` — replaces the whole calendar.

The bulk-import case: a school publishes next year's calendar in one go, from a spreadsheet.
Sending the complete list makes a half-imported calendar impossible, which a sequence of
individual adds cannot promise.

### Flat in, grouped out

You send **one row per reason**. The service groups rows by date, so two rows sharing a date
become one closed day with two reasons — a Sunday that is also Diwali. Sending the same
**type** twice for one date is refused; that is a duplicated row, not a second reason.

### Everything already there is discarded

Generated weekly offs included. That is what replace means, and it is why #21 exists.

### The seven test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain. Run Create School and Create Academic Year first.

 THE REQUEST IS FLAT, STORAGE IS GROUPED. You send one row per REASON, the
 way a spreadsheet holds it. The service groups them by date, so the body
 above — three rows, two of them 2026-11-08 — becomes TWO closed days:

   2026-11-08  ->  [ Weekly Off (WEEKLY_OFF), Diwali (FESTIVAL) ]
   2026-08-15  ->  [ Independence Day (PUBLIC_HOLIDAY) ]

 So two rows sharing a date is NOT a duplicate. That is a Sunday that is
 also Holi, which is the whole reason a day holds an array of reasons.
 What IS refused is the same TYPE twice on one date — see case 03.

 THIS REPLACES EVERYTHING, generated weekly offs included. It is the bulk
 import: a school publishes next year's calendar in one go. Use "Add
 Holiday" (#21) to add a single entry in-year.

 TWO COUNTS COME BACK, and they are different numbers:
   closedDayCount  how many days the school is shut     (2 above)
   eventCount      how many reasons are recorded        (3 above)
 countsByType counts REASONS, so a festival on a Sunday still counts as a
 festival.`,
      requiredFields: ["name", "type", "date"],
      optionalFields: ["description"],
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `[
  { "name": "Weekly Off",       "type": "WEEKLY_OFF",     "date": "2026-11-08" },
  { "name": "Diwali",           "type": "FESTIVAL",       "date": "2026-11-08",
    "description": "Festival of lights" },
  { "name": "Independence Day", "type": "PUBLIC_HOLIDAY", "date": "2026-08-15" }
]`,
      successStatus: 200,
      responseFields: ["academicYearName", "startDate", "endDate", "closedDayCount", "eventCount", "countsByType", "holidays", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "DUPLICATE_HOLIDAY_ENTRY", when: "Same type twice on one date" },
        { status: 400, code: "HOLIDAY_OUTSIDE_YEAR", when: "A date outside the year" },
        { status: 400, code: "MALFORMED_REQUEST", when: "An unknown type" },
        { status: 400, code: "—", when: "A missing name or type" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "Unknown year name" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "REPLACE THE CALENDAR",
          expect: "200 OK",
          notes: `The body above.
    OUT: closedDayCount: 2, eventCount: 3
         countsByType: { WEEKLY_OFF: 1, PUBLIC_HOLIDAY: 1, FESTIVAL: 1 }
         changeSummary: "Replaced the calendar: 0 closed days out, 2 in
                         (3 reasons)."
    Days come back SORTED BY DATE with a derived dayOfWeek on each.`,
          body: null,
        },
        {
          id: "02",
          name: "CLEAR THE WHOLE CALENDAR",
          expect: "200 OK",
          notes: `An empty array is the honest way to empty it. There is no DELETE for
    the whole calendar.`,
          body: `[]`,
        },
        {
          id: "03",
          name: "SAME TYPE TWICE ON ONE DATE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "DUPLICATE_HOLIDAY_ENTRY",
           "message": "Two WEEKLY_OFF entries sent for 2026-11-08. A day can
                       hold several reasons, but not the same one twice." }

    A day genuinely closed for two reasons has two DIFFERENT types. The same
    one twice is a duplicated spreadsheet row.`,
          body: `[
  { "name": "Weekly Off",       "type": "WEEKLY_OFF", "date": "2026-11-08" },
  { "name": "Weekly Off again", "type": "WEEKLY_OFF", "date": "2026-11-08" }
]`,
        },
        {
          id: "04",
          name: "A DATE OUTSIDE THE YEAR",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "HOLIDAY_OUTSIDE_YEAR" }
    A holiday belongs to the year that contains it; one outside would never
    be found by anything looking at that year.`,
          body: `[
  { "name": "New Year", "type": "PUBLIC_HOLIDAY", "date": "2028-01-01" }
]`,
        },
        {
          id: "05",
          name: "AN UNKNOWN TYPE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "MALFORMED_REQUEST" }
    There is no NATIONAL_HOLIDAY. Accepted: WEEKLY_OFF, PUBLIC_HOLIDAY,
    FESTIVAL, RELIGIOUS, SCHOOL_EVENT, VACATION, EXAM_BREAK, OTHER.`,
          body: `[
  { "name": "Sports Day", "type": "NATIONAL_HOLIDAY", "date": "2026-12-01" }
]`,
        },
        {
          id: "06",
          name: "A MISSING NAME OR TYPE",
          expect: "400 Bad Request",
          notes: `OUT: fieldErrors names the row's field. Every reason needs a name.`,
          body: `[
  { "type": "FESTIVAL", "date": "2026-12-01" }
]`,
        },
        {
          id: "07",
          name: "UNKNOWN YEAR NAME",
          expect: "404 Not Found",
          notes: `Change {{academicYearName}} in the URL to 1999-2000.
    OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }`,
          body: null,
        },
      ],
    },
    {
      id: "add-holiday",
      name: "Add Holiday",
      method: "POST",
      path: "/schools/current/academic-years/{name}/holidays",
      status: 'live',
      summary: "Adds one reason to one day. A day that is already closed is not a conflict.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{name}/holidays\` — adds one reason to one day.

The in-year case: a bandh, an unexpected closure, a festival somebody missed.

### An already-closed date is not a conflict

The reason is added **alongside** what is already there, and the day is created if this is its
first reason. The caller never has to know which. A second \`WEEKLY_OFF\` on a Sunday that already
has one is refused — that is a repeat, not a second reason.

### The seven test cases are in the request body as comments
`,
      bodyNotes: `ADDS ONE REASON TO ONE DAY. The in-year case: a bandh, an unexpected
 closure, a festival somebody missed.

 A DATE THAT IS ALREADY CLOSED IS NOT A CONFLICT. The reason joins what is
 already on that day. You do NOT have to know whether the day exists, fetch
 it, append and send it back — send one reason and the service merges it.
 That is how a Sunday becomes a weekly off that is ALSO Holi.

 What is refused is the same TYPE twice on one day (case 03): a second
 WEEKLY_OFF on a Sunday that already has one is a repeat, never a reason.`,
      requiredFields: ["name", "type", "date"],
      optionalFields: ["description"],
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "Diwali",
  "description": "School closed for the festival of lights",
  "type": "FESTIVAL",
  "date": "2026-11-08"
}`,
      successStatus: 200,
      responseFields: ["academicYearName", "closedDayCount", "eventCount", "countsByType", "holidays", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "HOLIDAY_OUTSIDE_YEAR", when: "A date outside the year" },
        { status: 400, code: "—", when: "No name" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "—", when: "Unknown year name" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "HOLIDAY_ENTRY_EXISTS", when: "The same type twice on one day" },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "ADD TO AN EMPTY DATE",
          expect: "200 OK",
          notes: `The body above, on a date with nothing on it.
    OUT: changeSummary: "Added 'Diwali' on 2026-11-08."
         closedDayCount and eventCount both go up by 1.`,
          body: null,
        },
        {
          id: "02",
          name: "ADD A SECOND REASON TO THE SAME DATE",
          expect: "200 OK",
          notes: `OUT: changeSummary: "Added 'Weekly Off' on 2026-11-08 alongside 1
                         existing."
         closedDayCount UNCHANGED — still one closed day.
         eventCount goes up by 1 — now two reasons.
    THIS IS THE CASE THE WHOLE STRUCTURE EXISTS FOR.`,
          body: `{
  "name": "Weekly Off",
  "type": "WEEKLY_OFF",
  "date": "2026-11-08"
}`,
        },
        {
          id: "03",
          name: "THE SAME TYPE TWICE ON ONE DAY",
          expect: "409 Conflict",
          notes: `Send case 02 again.
    OUT: { "code": "HOLIDAY_ENTRY_EXISTS",
           "message": "There is already a WEEKLY_OFF entry on 2026-11-08.
                       Edit or remove it first." }`,
          body: null,
        },
        {
          id: "04",
          name: "A DATE OUTSIDE THE YEAR",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "HOLIDAY_OUTSIDE_YEAR" }`,
          body: `{
  "name": "New Year",
  "type": "PUBLIC_HOLIDAY",
  "date": "2028-01-01"
}`,
        },
        {
          id: "05",
          name: "NO NAME",
          expect: "400 Bad Request",
          notes: `OUT: fieldErrors: { "name": "must not be blank" }`,
          body: `{
  "type": "FESTIVAL",
  "date": "2026-12-01"
}`,
        },
        {
          id: "06",
          name: "NO DATE",
          expect: "400 Bad Request",
          notes: `OUT: fieldErrors: { "date": "must not be null" }
    The date is on the REQUEST even though it is not on the stored reason —
    it is how the service knows which day to merge into.`,
          body: `{
  "name": "Sports Day",
  "type": "SCHOOL_EVENT"
}`,
        },
        {
          id: "07",
          name: "UNKNOWN YEAR NAME",
          expect: "404 Not Found",
          notes: `Change {{academicYearName}} to 1999-2000.`,
          body: null,
        },
      ],
    },
    {
      id: "update-holiday",
      name: "Update Holiday",
      method: "PATCH",
      path: "/schools/current/academic-years/{name}/holidays/{date}",
      status: 'live',
      summary: "Edits one reason on a day. ?type= says which one when the day holds several.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/academic-years/{name}/holidays/{date}?type=\` — edits one reason.

### \`?type=\` picks which reason

A day can hold several. Omit the parameter when the day has one; it is **required** when the day
has more, and the error lists what is on that day. Guessing on the caller's behalf would edit the
wrong entry half the time, silently.

### The date is the key and cannot be changed

Moving a holiday is a DELETE then a POST, which leaves both dates visible rather than one silent
edit.

### Retyping is \`newType\`

The selector is in the query string, the new value in the body. Two different things, two
different names.

### The eleven test cases are in the request body as comments
`,
      bodyNotes: `?type= SAYS WHICH REASON TO EDIT. A day can hold several, so the date alone
 no longer identifies one.
   - OMIT it when the day has exactly ONE reason (the common case).
   - REQUIRED when the day has more. The API asks rather than editing the
     first of two, which would be wrong as often as right and invisible.

 THE DATE CANNOT BE CHANGED. It is the key in the URL. Moving a holiday is a
 DELETE then a POST, which leaves both dates visible in the log instead of
 one silent edit.

 RETYPING IS "newType", NOT "type". The selector lives in the query string
 and the new value in the body; one field named \`type\` meaning "which one"
 in one place and "make it this" in the other is a bug waiting to happen.

 Partial: null/omitted leaves a field alone, "" clears the description.
 name and the type cannot be cleared.`,
      optionalFields: ["name", "description", "newType"],
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
        { name: "date", value: "2026-11-08", description: "The closed day, as YYYY-MM-DD." },
      ],
      queryParams: [
        { key: "type", value: "FESTIVAL", enabled: true },
      ],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "Diwali (day 1)",
  "description": "Lakshmi Puja"
}`,
      successStatus: 200,
      responseFields: ["academicYearName", "closedDayCount", "eventCount", "countsByType", "holidays", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "HOLIDAY_TYPE_REQUIRED", when: "No ?type= on a two-reason day" },
        { status: 400, code: "NOTHING_TO_UPDATE", when: "Empty body" },
        { status: 400, code: "HOLIDAY_NAME_REQUIRED", when: "Blank name" },
        { status: 400, code: "INVALID_PARAMETER", when: "A misspelled type" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "HOLIDAY_ENTRY_NOT_FOUND", when: "A type that is not on that day" },
        { status: 404, code: "HOLIDAY_NOT_FOUND", when: "A date that is not closed" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "HOLIDAY_ENTRY_EXISTS", when: "Retype into a type the day already has" },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "EDIT THE ONLY REASON ON A DAY, NO ?type=",
          expect: "200 OK",
          notes: `The body above, on a date with one reason. Drop ?type= from the URL.
    OUT: changeSummary: "Updated 'Diwali (day 1)' on 2026-11-08."`,
          body: null,
        },
        {
          id: "02",
          name: "NO ?type= ON A TWO-REASON DAY",
          expect: "400 Bad Request",
          notes: `Same, on a date holding both a weekly off and a festival.
    OUT: { "code": "HOLIDAY_TYPE_REQUIRED",
           "message": "2026-11-08 is closed for 2 reasons (Weekly Off
                       (WEEKLY_OFF), Diwali (FESTIVAL)). Add ?type= to say
                       which one you mean." }
    The message LISTS the reasons, so the next request is obvious.`,
          body: null,
        },
        {
          id: "03",
          name: "EDIT ONE REASON OF TWO",
          expect: "200 OK",
          notes: `Keep ?type=FESTIVAL in the URL, body above.
    Only the festival changes; the weekly off on that day is untouched.`,
          body: null,
        },
        {
          id: "04",
          name: "CLEAR A DESCRIPTION",
          expect: "200 OK",
          notes: `"" clears, null/omitted leaves alone. OUT: description: null`,
          body: `{
  "description": ""
}`,
        },
        {
          id: "05",
          name: "RETYPE A REASON",
          expect: "200 OK",
          notes: `With ?type=FESTIVAL — the festival becomes a religious holiday.`,
          body: `{
  "newType": "RELIGIOUS"
}`,
        },
        {
          id: "06",
          name: "RETYPE INTO A TYPE THE DAY ALREADY HAS",
          expect: "409 Conflict",
          notes: `With ?type=FESTIVAL, on a day that already has a weekly off.
    OUT: { "code": "HOLIDAY_ENTRY_EXISTS",
           "message": "There is already a WEEKLY_OFF entry on 2026-11-08, so
                       this one cannot become that." }
    One day cannot hold the same reason twice, whichever door it came in by.`,
          body: `{
  "newType": "WEEKLY_OFF"
}`,
        },
        {
          id: "07",
          name: "A TYPE THAT IS NOT ON THAT DAY",
          expect: "404 Not Found",
          notes: `?type=VACATION on a day closed for Diwali.
    OUT: { "code": "HOLIDAY_ENTRY_NOT_FOUND",
           "message": "No VACATION entry on 2026-11-08. That day is closed
                       for Diwali (FESTIVAL)." }`,
          body: null,
          queryParams: [{ key: "type", value: "VACATION", enabled: true }],
        },
        {
          id: "08",
          name: "EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE",
           "message": "Send at least one of name, description or newType." }`,
          body: `{
}`,
        },
        {
          id: "09",
          name: "BLANK NAME",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "HOLIDAY_NAME_REQUIRED" }
    A name cannot be REMOVED. Send a new one, or omit the field.`,
          body: `{
  "name": "   "
}`,
        },
        {
          id: "10",
          name: "A DATE THAT IS NOT CLOSED",
          expect: "404 Not Found",
          notes: `Change the date in the URL to 2026-07-04.
    OUT: { "code": "HOLIDAY_NOT_FOUND" }`,
          body: null,
        },
        {
          id: "11",
          name: "A MISSPELLED TYPE",
          expect: "400 Bad Request",
          notes: `?type=WEEKLYOFF
    OUT: { "code": "INVALID_PARAMETER",
           "message": "...Accepted values: WEEKLY_OFF, PUBLIC_HOLIDAY, ..." }`,
          body: null,
          queryParams: [{ key: "type", value: "WEEKLYOFF", enabled: true }],
        },
      ],
    },
    {
      id: "remove-holiday",
      name: "Remove Holiday",
      method: "DELETE",
      path: "/schools/current/academic-years/{name}/holidays/{date}",
      status: 'live',
      summary: "Removes one reason, or the whole day when ?type= is left off.",
      schoolSurface: true,
      docs: `**DELETE** \`/schools/current/academic-years/{name}/holidays/{date}?type=\` — removes a reason,
or the whole day.

### With \`?type=\`, one reason goes

A Sunday that was also Holi is still a weekly off afterwards. **Removing the last reason removes
the day** — a closed day with nothing saying why reads as corruption to whoever finds it.

### Without \`?type=\`, the whole day goes

Every reason with it. That is a real correction — "the school is open that day after all" — and
the change summary names what went, so someone who meant to drop one reason can see they dropped
two.

\`type\` is optional here, unlike the bulk delete: the blast radius is one date either way.

### The six test cases are in the description below

Postman sends no body on a DELETE, so they live here rather than in a body block:

\`\`\`
01  REMOVE ONE REASON OF TWO                              -> 200 OK
    ?type=WEEKLY_OFF on a day that is also Holi.
    OUT: "Removed 'Weekly Off' on 2027-03-14, which stays closed for
          Holi (FESTIVAL)."
    closedDayCount UNCHANGED. eventCount down by 1.

02  REMOVE THE LAST REASON ON A DAY                       -> 200 OK
    ?type=FESTIVAL on a day with only that.
    OUT: "Removed 'Holi' on 2027-03-14, which is now a working day."
    The DAY DISAPPEARS. Both counts go down.

03  NO ?type= ON A TWO-REASON DAY                         -> 200 OK
    The whole day goes, both reasons with it.
    OUT: "Removed Weekly Off (WEEKLY_OFF), Diwali (FESTIVAL) on 2026-11-08."
    NOT an error — but read the summary. It names what you actually removed.

04  A TYPE THAT IS NOT ON THAT DAY                   -> 404 Not Found
    ?type=WEEKLY_OFF on a day closed only for Janmashtami.
    OUT: { "code": "HOLIDAY_ENTRY_NOT_FOUND",
           "message": "No WEEKLY_OFF entry on 2026-08-15. That day is closed
                       for Janmashtami (FESTIVAL)." }

05  A DATE THAT IS NOT CLOSED                        -> 404 Not Found
    OUT: { "code": "HOLIDAY_NOT_FOUND" }
    A 404, not a silent 200. "It was already gone" and "you deleted it" are
    different answers and the caller should be able to tell them apart.

06  A MISSPELLED TYPE                              -> 400 Bad Request
    ?type=WEEKLYOFF
    OUT: { "code": "INVALID_PARAMETER" } with the accepted values listed.
\`\`\`
`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
        { name: "date", value: "2026-11-08", description: "The closed day, as YYYY-MM-DD." },
      ],
      queryParams: [
        { key: "type", value: "WEEKLY_OFF", enabled: true },
      ],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["academicYearName", "closedDayCount", "eventCount", "countsByType", "holidays", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_PARAMETER", when: "A misspelled type" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "HOLIDAY_ENTRY_NOT_FOUND", when: "A type that is not on that day" },
        { status: 404, code: "HOLIDAY_NOT_FOUND", when: "A date that is not closed" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "REMOVE ONE REASON OF TWO",
          expect: "200 OK",
          notes: `?type=WEEKLY_OFF on a day that is also Holi.
    OUT: "Removed 'Weekly Off' on 2027-03-14, which stays closed for
          Holi (FESTIVAL)."
    closedDayCount UNCHANGED. eventCount down by 1.`,
          body: null,
          queryParams: [{ key: "type", value: "WEEKLY_OFF", enabled: true }],
        },
        {
          id: "02",
          name: "REMOVE THE LAST REASON ON A DAY",
          expect: "200 OK",
          notes: `?type=FESTIVAL on a day with only that.
    OUT: "Removed 'Holi' on 2027-03-14, which is now a working day."
    The DAY DISAPPEARS. Both counts go down.`,
          body: null,
          queryParams: [{ key: "type", value: "FESTIVAL", enabled: true }],
        },
        {
          id: "03",
          name: "NO ?type= ON A TWO-REASON DAY",
          expect: "200 OK",
          notes: `The whole day goes, both reasons with it.
    OUT: "Removed Weekly Off (WEEKLY_OFF), Diwali (FESTIVAL) on 2026-11-08."
    NOT an error — but read the summary. It names what you actually removed.`,
          body: null,
        },
        {
          id: "04",
          name: "A TYPE THAT IS NOT ON THAT DAY",
          expect: "404 Not Found",
          notes: `?type=WEEKLY_OFF on a day closed only for Janmashtami.
    OUT: { "code": "HOLIDAY_ENTRY_NOT_FOUND",
           "message": "No WEEKLY_OFF entry on 2026-08-15. That day is closed
                       for Janmashtami (FESTIVAL)." }`,
          body: null,
          queryParams: [{ key: "type", value: "WEEKLY_OFF", enabled: true }],
        },
        {
          id: "05",
          name: "A DATE THAT IS NOT CLOSED",
          expect: "404 Not Found",
          notes: `OUT: { "code": "HOLIDAY_NOT_FOUND" }
    A 404, not a silent 200. "It was already gone" and "you deleted it" are
    different answers and the caller should be able to tell them apart.`,
          body: null,
        },
        {
          id: "06",
          name: "A MISSPELLED TYPE",
          expect: "400 Bad Request",
          notes: `?type=WEEKLYOFF
    OUT: { "code": "INVALID_PARAMETER" } with the accepted values listed.`,
          body: null,
          queryParams: [{ key: "type", value: "WEEKLYOFF", enabled: true }],
        },
      ],
    },
    {
      id: "generate-weekly-off",
      name: "Generate Weekly Off",
      method: "POST",
      path: "/schools/current/academic-years/{name}/holidays/generate-weekly-off",
      status: 'live',
      summary: "Makes one dated entry per occurrence of a weekday. Needed because there is no weekly-off field anywhere.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{name}/holidays/generate-weekly-off\`

Required by the model rather than a convenience. **There is no "weekly off day" field anywhere in
this system** — schools here may run on Sunday with the off day on any other weekday — so every
non-working day is a dated entry and a year needs roughly 52 of them.

### A Sunday that is already a festival still gets its weekly off

The day ends up holding **both** reasons. The school was closed for Diwali *and* it was their
weekly off; a report that knows only one of those is wrong about the other. Only a date that
already carries a \`WEEKLY_OFF\` is skipped, and the skipped dates come back in \`skippedDates\`.

### Safe to run twice

The second run generates nothing and reports everything skipped.

### \`dayOfWeek\` is required

There is no default, and there must not be one — defaulting to Sunday is the assumption this
whole design refuses to make.

### The nine test cases are in the request body as comments
`,
      bodyNotes: `THIS IS REQUIRED BY THE MODEL, NOT A CONVENIENCE. There is no "weekly off
 day" field anywhere in this system: schools here may run on Sunday with the
 off day on any other weekday, so EVERY non-working day is a dated entry and
 a year needs roughly 52 of them. Without this, somebody types 52 dates or a
 developer hardcodes Sunday.

 A DATE THAT ALREADY HAS A FESTIVAL STILL GETS ITS WEEKLY OFF. The two
 reasons sit on the same day. The school was closed for Diwali AND it was
 their weekly off, and a report that only knows one of those is wrong about
 the other.

 ONLY AN EXISTING WEEKLY_OFF ON THAT DATE IS SKIPPED, and the skipped dates
 come back in skippedDates. That is also what makes running it twice safe.`,
      requiredFields: ["dayOfWeek"],
      optionalFields: ["fromDate", "toDate", "name"],
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "dayOfWeek": "SUNDAY"
}`,
      successStatus: 200,
      responseFields: ["academicYearName", "dayOfWeek", "fromDate", "toDate", "generated", "skippedAlreadyWeeklyOff", "skippedDates", "closedDayCountAfter", "eventCountAfter", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "HOLIDAY_OUTSIDE_YEAR", when: "A window outside the year" },
        { status: 400, code: "INVALID_DATE_RANGE", when: "Fromdate after todate" },
        { status: 400, code: "—", when: "No dayofweek" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "GENERATE EVERY SUNDAY IN THE YEAR",
          expect: "200 OK",
          notes: `The body above. fromDate/toDate default to the whole year.
    OUT: generated: ~52, skippedAlreadyWeeklyOff: 0
         closedDayCountAfter and eventCountAfter both reported — they differ
         wherever a Sunday also carries a festival.`,
          body: null,
        },
        {
          id: "02",
          name: "RUN IT AGAIN",
          expect: "200 OK",
          notes: `OUT: generated: 0, skippedAlreadyWeeklyOff: ~52
         "Nothing generated — every SUNDAY in that window already had a
          weekly off."
    IDEMPOTENT. Safe to re-run.`,
          body: null,
        },
        {
          id: "03",
          name: "A SUNDAY THAT IS ALREADY A FESTIVAL",
          expect: "200 OK",
          notes: `Add Holi on a Sunday with "Add Holiday" first, then generate.
    THE DAY ENDS UP WITH BOTH. It is NOT skipped and NOT overwritten:
      2027-03-14 -> [ Holi (FESTIVAL), Weekly Off (WEEKLY_OFF) ]
    generated counts it; skippedDates does not list it.
    THIS IS THE CASE THE ARRAY-PER-DATE STRUCTURE EXISTS FOR.`,
          body: null,
        },
        {
          id: "04",
          name: "A CUSTOM NAME",
          expect: "200 OK",
          notes: `Defaults to "Weekly Off" when name is omitted.`,
          body: `{
  "dayOfWeek": "FRIDAY",
  "name": "Jumu'ah"
}`,
        },
        {
          id: "05",
          name: "A WINDOW INSIDE THE YEAR",
          expect: "200 OK",
          notes: `For a school that closes alternate Saturdays only in one term.`,
          body: `{
  "dayOfWeek": "SATURDAY",
  "fromDate": "2026-04-01",
  "toDate": "2026-09-30"
}`,
        },
        {
          id: "06",
          name: "A WINDOW OUTSIDE THE YEAR",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "HOLIDAY_OUTSIDE_YEAR" }`,
          body: `{
  "dayOfWeek": "SUNDAY",
  "toDate": "2028-01-01"
}`,
        },
        {
          id: "07",
          name: "fromDate AFTER toDate",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_DATE_RANGE" }`,
          body: `{
  "dayOfWeek": "SUNDAY",
  "fromDate": "2026-12-01",
  "toDate": "2026-06-01"
}`,
        },
        {
          id: "08",
          name: "NO dayOfWeek",
          expect: "400 Bad Request",
          notes: `OUT: fieldErrors: { "dayOfWeek": "must not be null" }
    THERE IS NO DEFAULT WEEKDAY and there must not be one. Defaulting to
    SUNDAY is exactly the assumption this whole design refuses to make.

09  UNDOING A WRONG WEEKDAY
    Use "Remove Holidays By Type" with ?type=WEEKLY_OFF, then run again.
    Days that had another reason survive, stripped of the weekly off only.`,
          body: `{
}`,
        },
      ],
    },
    {
      id: "remove-holidays-by-type",
      name: "Remove Holidays By Type",
      method: "DELETE",
      path: "/schools/current/academic-years/{name}/holidays",
      status: 'live',
      summary: "Clears every entry of one type across the year. type is required here.",
      schoolSurface: true,
      docs: `**DELETE** \`/schools/current/academic-years/{name}/holidays?type=\` — removes every reason of
one type.

The companion to the generator, because the first thing anybody does with it is pick the wrong
weekday, and undoing that one date at a time across 52 entries is not a thing a person should
have to do.

### Days with other reasons survive

The matching reason is stripped wherever it appears, and only the days left with **none** are
dropped. A Sunday that was also Holi survives as Holi; a plain Sunday goes entirely. The summary
reports both numbers.

### \`type\` is required

A bulk delete that cleared the whole calendar when a query parameter was forgotten would be the
most destructive accident in this package. Omitting it is a \`400\`, never a no-op and never a
wipe.

### The five test cases are in the description below

Postman sends no body on a DELETE, so they live here:

\`\`\`
01  REMOVE EVERY WEEKLY OFF                                -> 200 OK
    ?type=WEEKLY_OFF after generating Sundays.
    OUT: "Removed 52 WEEKLY_OFF entries; 50 days became working days,
          2 stayed closed for other reasons."
    The two survivors are the Sundays that were also festivals.

02  RUN IT AGAIN                                           -> 200 OK
    OUT: "Nothing to remove — no WEEKLY_OFF entries were on this calendar."
    A 200, not a 404. Nothing was asked for that could not be honoured.

03  NO type PARAMETER                                -> 400 Bad Request
    OUT: { "code": "MISSING_PARAMETER",
           "message": "The 'type' query parameter is required." }
    THE GUARD THAT MATTERS MOST HERE.

04  A MISSPELLED TYPE                              -> 400 Bad Request
    ?type=WEEKLYOFF
    OUT: { "code": "INVALID_PARAMETER",
           "message": "'WEEKLYOFF' is not a valid value for 'type'.
                       Accepted values: WEEKLY_OFF, PUBLIC_HOLIDAY, ..." }

05  UNKNOWN YEAR NAME                                -> 404 Not Found
    OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }
\`\`\`
`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [
        { key: "type", value: "WEEKLY_OFF", enabled: true },
      ],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["academicYearName", "closedDayCount", "eventCount", "countsByType", "holidays", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "MISSING_PARAMETER", when: "No type parameter" },
        { status: 400, code: "INVALID_PARAMETER", when: "A misspelled type" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "Unknown year name" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "REMOVE EVERY WEEKLY OFF",
          expect: "200 OK",
          notes: `?type=WEEKLY_OFF after generating Sundays.
    OUT: "Removed 52 WEEKLY_OFF entries; 50 days became working days,
          2 stayed closed for other reasons."
    The two survivors are the Sundays that were also festivals.`,
          body: null,
          queryParams: [{ key: "type", value: "WEEKLY_OFF", enabled: true }],
        },
        {
          id: "02",
          name: "RUN IT AGAIN",
          expect: "200 OK",
          notes: `OUT: "Nothing to remove — no WEEKLY_OFF entries were on this calendar."
    A 200, not a 404. Nothing was asked for that could not be honoured.`,
          body: null,
        },
        {
          id: "03",
          name: "NO type PARAMETER",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "MISSING_PARAMETER",
           "message": "The 'type' query parameter is required." }
    THE GUARD THAT MATTERS MOST HERE.`,
          body: null,
        },
        {
          id: "04",
          name: "A MISSPELLED TYPE",
          expect: "400 Bad Request",
          notes: `?type=WEEKLYOFF
    OUT: { "code": "INVALID_PARAMETER",
           "message": "'WEEKLYOFF' is not a valid value for 'type'.
                       Accepted values: WEEKLY_OFF, PUBLIC_HOLIDAY, ..." }`,
          body: null,
          queryParams: [{ key: "type", value: "WEEKLYOFF", enabled: true }],
        },
        {
          id: "05",
          name: "UNKNOWN YEAR NAME",
          expect: "404 Not Found",
          notes: `OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }`,
          body: null,
        },
      ],
    },
    {
      id: "enable-enrollment",
      name: "Enable Enrollment",
      method: "POST",
      path: "/schools/current/academic-years/{name}/enrollment/enable",
      status: 'live',
      summary: "Opens enrollment for the year. A gate, not a field edit.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{name}/enrollment/enable\` — opens the year to new
enrollments.

A gate on what **other modules** may do: with this on, admissions may assign students to this
year. Idempotent — a year already open comes back \`200\` saying so.

**No authorization is enforced yet.** Every response says so in \`nextStep\`.

### The five test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain. Run Create School and Create Academic Year first.

 NO AUTHORIZATION IS ENFORCED ON ANY OF THESE YET. Anybody who can reach
 them can run them. The permission model does not exist, so there is nothing
 to check against — every response says so in nextStep rather than letting it
 be discovered later. The audit rows ARE written now, because a trail that
 starts the day permissions arrive says nothing about the months before it.

 THESE ARE POST, NOT PATCH. Both fields are booleans, so
 PATCH {"resultsLocked": false} would work mechanically — which is the
 problem. It would make the most sensitive operation in this package look
 identical to the least, and leave nowhere to put a reason.

 NO BODY IS NEEDED. #24, #25 and #26 take none; anything sent is ignored.
 Only #27 takes one, because only #27 requires a reason.`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
}`,
      successStatus: 200,
      responseFields: ["academicYearId", "name", "enrollmentEnabled", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "No tenant header" },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "Unknown year name" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "A suspended school" },
      ],
      examples: [
        {
          id: "01",
          name: "OPEN THE YEAR TO ENROLLMENTS",
          expect: "200 OK",
          notes: `No body needed.
    OUT: enrollmentEnabled: true
         nextStep: "Enrollment enabled for '2026-2027'. No authorization..."`,
          body: null,
        },
        {
          id: "02",
          name: "SEND IT AGAIN",
          expect: "200 OK",
          notes: `IDEMPOTENT, deliberately. The caller asked for a state and that state
    holds. A 409 here would only teach callers to GET first and then race.
    OUT: "Enrollment was already enabled for '2026-2027'."`,
          body: null,
        },
        {
          id: "03",
          name: "UNKNOWN YEAR NAME",
          expect: "404 Not Found",
          notes: `Change {{academicYearName}} to 1999-2000.
    OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }`,
          body: null,
        },
        {
          id: "04",
          name: "NO TENANT HEADER",
          expect: "400 Bad Request",
          notes: `Disable the X-School-Subdomain header.
    OUT: { "code": "TENANT_NOT_RESOLVED" }`,
          body: null,
        },
        {
          id: "05",
          name: "A SUSPENDED SCHOOL",
          expect: "409 Conflict",
          notes: `Suspend the school first.
    OUT: { "code": "SCHOOL_NOT_EDITABLE" }`,
          body: null,
        },
      ],
    },
    {
      id: "disable-enrollment",
      name: "Disable Enrollment",
      method: "POST",
      path: "/schools/current/academic-years/{name}/enrollment/disable",
      status: 'live',
      summary: "Closes enrollment for the year.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{name}/enrollment/disable\` — closes the year to new
enrollments.

**Does not touch students already enrolled.** This is a gate on new writes, not a withdrawal —
anything already in the year stays exactly as it is.

Idempotent, and flips freely in both directions: it is a switch, not a lifecycle.

### The four test cases are in the request body as comments
`,
      bodyNotes: `A GATE ON NEW WRITES, NOT A WITHDRAWAL. Students already enrolled in this
 year are completely unaffected. Nothing is removed and nothing is moved.`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
}`,
      successStatus: 200,
      responseFields: ["academicYearId", "name", "enrollmentEnabled", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "—", when: "Unknown year name" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "CLOSE THE YEAR TO NEW ENROLLMENTS",
          expect: "200 OK",
          notes: `OUT: enrollmentEnabled: false
         "Students already enrolled are unaffected."`,
          body: null,
        },
        {
          id: "02",
          name: "SEND IT AGAIN",
          expect: "200 OK",
          notes: `IDEMPOTENT. "Enrollment was already disabled for '2026-2027'."`,
          body: null,
        },
        {
          id: "03",
          name: "ENABLE, THEN DISABLE, THEN ENABLE",
          expect: "200 OK each",
          notes: `The gate flips freely. It is a switch, not a lifecycle: neither
    direction is destructive and neither needs a reason.
    CONTRAST WITH #27, where the unlock direction is not like this at all.`,
          body: null,
        },
        {
          id: "04",
          name: "UNKNOWN YEAR NAME",
          expect: "404 Not Found",
          notes: ``,
          body: null,
        },
      ],
    },
    {
      id: "lock-results",
      name: "Lock Results",
      method: "POST",
      path: "/schools/current/academic-years/{name}/results/lock",
      status: 'live',
      summary: "Locks results for the year. Routine.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{name}/results/lock\` — locks results against further
change.

Routine — it is what happens when marks are published. Idempotent, takes no body, and is
independent of the enrollment gates.

**No authorization is enforced yet**, and every response says so in \`nextStep\`.

### The five test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain. Run Create School and Create Academic Year first.

 ROUTINE. This is what happens when marks are published: results stop being
 editable. No body needed; anything sent is ignored.

 NO AUTHORIZATION IS ENFORCED YET, like the other three gates. Every
 response says so in nextStep rather than letting it be discovered later.`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
}`,
      successStatus: 200,
      responseFields: ["academicYearId", "name", "resultsLocked", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "No tenant header" },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "Unknown year name" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "LOCK THE RESULTS",
          expect: "200 OK",
          notes: `OUT: resultsLocked: true
         "Results locked for '2026-2027'."`,
          body: null,
        },
        {
          id: "02",
          name: "SEND IT AGAIN",
          expect: "200 OK",
          notes: `IDEMPOTENT. "Results were already locked for '2026-2027'."`,
          body: null,
        },
        {
          id: "03",
          name: "IT DOES NOT TOUCH ENROLLMENT",
          expect: "200 OK",
          notes: `Run "Enable Enrollment" after locking: enrollmentEnabled flips,
    resultsLocked stays true. The two gates are independent.`,
          body: null,
        },
        {
          id: "04",
          name: "UNKNOWN YEAR NAME",
          expect: "404 Not Found",
          notes: `Change {{academicYearName}} to 1999-2000.
    OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }`,
          body: null,
        },
        {
          id: "05",
          name: "NO TENANT HEADER",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "TENANT_NOT_RESOLVED" }`,
          body: null,
        },
      ],
    },
    {
      id: "unlock-results",
      name: "Unlock Results",
      method: "POST",
      path: "/schools/current/academic-years/{name}/results/unlock",
      status: 'live',
      summary: "Unlocking lets somebody change a mark a parent has already seen.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{name}/results/unlock\` — unlocks results so they can
be corrected.

Idempotent, takes no body.

### It records nothing about who unlocked, or why

Unlocking lets somebody change a mark a parent has already seen, and this endpoint leaves no
trace of it having happened. That is a deliberate simplification for now: there is no
authentication, so an audit row could not name who acted anyway.

**Before results are real this needs a required reason and an audit row on every call, refusals
included.** The design is kept in \`controllers/core/README.md\`.

### The four test cases are in the request body as comments
`,
      bodyNotes: `UNLOCKING LETS SOMEBODY CHANGE A MARK A PARENT HAS ALREADY SEEN, and right
 now THIS ENDPOINT RECORDS NOTHING ABOUT IT. No reason is asked for, and no
 trace is left that it happened.

 That is deliberate for now, not an oversight. There is no authentication,
 so an audit row could not name who acted anyway, and a trail whose every
 entry says "unknown" is close to worthless.

 BEFORE RESULTS ARE REAL, this needs a required reason and an audit row on
 every call, refusals included. The full design — including the trap, that a
 refusal recorded inside the transaction that refuses it is never written —
 is kept in controllers/core/README.md.`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
}`,
      successStatus: 200,
      responseFields: ["academicYearId", "name", "resultsLocked", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "—", when: "Unknown year name" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "UNLOCK THE RESULTS",
          expect: "200 OK",
          notes: `Run "Lock Results" first.
    OUT: resultsLocked: false
         "Lock them again as soon as the corrections are in."`,
          body: null,
        },
        {
          id: "02",
          name: "SEND IT AGAIN",
          expect: "200 OK",
          notes: `IDEMPOTENT, like all four gates.
    "Results were already unlocked for '2026-2027'."`,
          body: null,
        },
        {
          id: "03",
          name: "LOCK, UNLOCK, LOCK",
          expect: "200 OK each",
          notes: `The gate flips freely in both directions.`,
          body: null,
        },
        {
          id: "04",
          name: "UNKNOWN YEAR NAME",
          expect: "404 Not Found",
          notes: ``,
          body: null,
        },
      ],
    },
    {
      id: "list-academic-years",
      name: "List Academic Years",
      method: "GET",
      path: "/schools/current/academic-years",
      status: 'live',
      summary: "Every academic year the school has, newest first.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years\` — every year this school has, newest first.

**Sorted on \`startDate\`, not \`createdAt\`.** "Newest" means the year furthest along the calendar, not the row typed most recently — a school setting up enters 2025-2026 after 2026-2027 often enough that the two orders disagree.

**No page envelope.** A school has a handful of years. List Schools stays the only list in this collection that pages.

A school with no years yet is \`200\` with \`[]\`, never a \`404\`.

\`nextStep\` is absent: it is a write field, and nothing just happened.

### Cases

| # | Setup | Expected |
|---|---|---|
| 01 | a fresh school | \`200\` \`[]\` |
| 02 | create 2025-2026, then 2027-2028, then 2026-2027 | \`200\` — returned 2027, 2026, 2025 |
| 03 | header removed | \`400 TENANT_NOT_RESOLVED\` |
`,
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["academicYearId", "name", "startDate", "endDate", "durationDays", "current", "holidayCount", "enrollmentEnabled", "resultsLocked"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [],
    },
    {
      id: "get-current-academic-year",
      name: "Get Current Academic Year",
      method: "GET",
      path: "/schools/current/academic-years/current",
      status: 'live',
      summary: "The year that contains today. A 404 when no year covers it — which is a real answer, not a fault.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/current\` — the year today falls in.

**Worked out from the dates, never stored.** \`AcademicYear\` has no \`current\` flag on purpose — two sources for "which year is it" is two sources that can disagree. The year this returns is always the one List Academic Years marks \`current: true\`.

Only one year can match, because Create Academic Year refuses an overlapping one.

Both ends are inclusive: a year ending **today** is still the current year; one that ended yesterday is not.

### The 404 says which kind of nothing

| Situation | Message |
|---|---|
| no years at all | \`This school has no academic years yet.\` |
| years, none covering today | \`No academic year covers 2026-08-31 in this school.\` |

Both are \`404 NO_CURRENT_ACADEMIC_YEAR\`. The two need different things done about them.

### \`current\` is a reserved year name

This fixed path segment wins over \`/{name}\`, so Create Academic Year refuses a year called \`current\` — otherwise it could be created and then never opened.
`,
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["academicYearId", "name", "startDate", "endDate", "durationDays", "current", "holidayCount"],
      captures: [
        { variable: "academicYearName", from: "name" },
      ],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [],
    },
    {
      id: "get-academic-year",
      name: "Get Academic Year",
      method: "GET",
      path: "/schools/current/academic-years/{name}",
      status: 'live',
      summary: "One year by name.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{name}\` — one year.

**Keyed on the name, not the id**, exactly as the writes are. Every other collection stores \`academicYear\` as this string, so the name is what the whole system already means when it says "which year" — and a URL that cannot change is a reminder that the thing it names cannot either.

The lookup is by school **and** name, so **asking for another school's year is a \`404\`**, not somebody else's data.

### Cases

| # | name | Expected |
|---|---|---|
| 01 | \`{{academicYearName}}\` | \`200\` |
| 02 | \`2099-2100\` | \`404 ACADEMIC_YEAR_NOT_FOUND\` |
| 03 | \`some%20year\` | \`404\` — not a 500 |
| 04 | another school's year name | \`404\` |
| 05 | \`current\` | reaches Get Current Academic Year instead — see that request |
`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["academicYearId", "name", "startDate", "endDate", "durationDays", "current", "holidayCount", "enrollmentEnabled", "resultsLocked"],
      captures: [
        { variable: "academicYearName", from: "name" },
      ],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [],
    },
    {
      id: "get-holiday-calendar",
      name: "Get Holiday Calendar",
      method: "GET",
      path: "/schools/current/academic-years/{name}/holidays",
      status: 'live',
      summary: "The year's whole calendar: every closed day, and why each one is closed.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{name}/holidays\` — the whole calendar.

The same record every calendar write returns, so a screen that adds a holiday and a screen that only reads one use a single shape. \`changeSummary\` is absent — nothing just happened.

Days come back **sorted by date**. A year created by Create Academic Year has an empty calendar, so \`200\` with \`holidays: []\` is the normal first answer.

### Two counts, because a day and a reason are not the same thing

On a year with 52 generated Sundays, Independence Day, and Diwali landing on one of those Sundays:

\`\`\`
closedDayCount 53      the number attendance and fees divide by
eventCount     54      reasons recorded across those days
countsByType   { WEEKLY_OFF: 52, PUBLIC_HOLIDAY: 1, FESTIVAL: 1 }
\`\`\`

\`countsByType\` counts **events**, not days — "how many festivals" must not be reduced by the ones that happened to land on a Sunday.

### No filtering, on purpose

No \`?type=\`, no date range. A full year is about sixty closed days. The questions worth asking about dates are **Get Day Status** and **Count Working Days**, which answer them properly rather than handing you a list to filter.
`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["academicYearName", "startDate", "endDate", "closedDayCount", "eventCount", "countsByType", "holidays"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [],
    },
    {
      id: "get-day-status",
      name: "Get Day Status",
      method: "GET",
      path: "/schools/current/academic-years/{name}/holidays/{date}",
      status: 'live',
      summary: "Is the school closed on this day, and why.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{name}/holidays/{date}\` — is the school closed that day, and why.

**This is the one the rest of the system was waiting for.** Attendance, timetables, transport and fee due dates all ask it. Every one of them should call this rather than reading the calendar and deciding for itself — the moment two places decide what a working day is, they disagree.

### An open day is a \`200\`, not a \`404\`

A working day answers \`200\` with \`closed: false\` and an empty \`events\` list. A \`404\` would make every caller treat "the school is open" and "something went wrong" as the same reply, which is exactly the bug this endpoint exists to prevent. The only \`404\` here is a year that does not exist.

### It never looks at the day of the week

**Schools here may run on Sunday and take the weekly off on another day.** Only a dated entry on the calendar closes a day. On a year with the weekly off generated on Wednesday:

| date | | answer |
|---|---|---|
| 2026-11-08 | Sunday | \`closed: false\` — open |
| 2026-11-11 | Wednesday | \`closed: true\` — Weekly Off **and** Diwali |

\`dayOfWeek\` is on the response for a person to read. **\`dayOfWeek === 'SUNDAY'\` in a caller is the bug.**

### Cases

| # | date | Expected |
|---|---|---|
| 01 | a closed date | \`200\` \`closed: true\`, every reason listed |
| 02 | a working date | \`200\` \`closed: false\`, \`events: []\` |
| 03 | the year's first or last day | \`200\` — both ends are inside the year |
| 04 | a date outside the year | \`400 DATE_OUTSIDE_ACADEMIC_YEAR\` — **not** \`closed: false\` |
| 05 | \`08-11-2026\` | \`400 INVALID_PARAMETER\` — dates are ISO |
`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
        { name: "date", value: "2026-11-08", description: "The closed day, as YYYY-MM-DD." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["academicYearName", "date", "dayOfWeek", "closed", "events"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [],
    },
    {
      id: "count-working-days",
      name: "Count Working Days",
      method: "GET",
      path: "/schools/current/academic-years/{name}/working-days",
      status: 'live',
      summary: "Which days in a range are working days, and how many.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{name}/working-days?from=&to=\` — which days in a range are working days, and how many.

Get Day Status asked about one date; this is the same question in bulk. Attendance percentages and fee proration need the answer for a whole range, not two hundred separate calls — and they need the **same** answer as each other.

### It returns the days, not just the count

\`\`\`json
{
  "totalDayCount": 7, "workingDayCount": 6, "closedDayCount": 1,
  "workingDays": [
    { "date": "2026-11-02", "dayOfWeek": "MONDAY" },
    { "date": "2026-11-03", "dayOfWeek": "TUESDAY" }
  ]
}
\`\`\`

A timetable being laid out or a fee schedule spread over teaching days needs to know *which* days. \`workingDayCount\` is the length of that list, not a separate subtraction, so the number and the list cannot drift apart.

### Leaving the range off means the whole year

\`from\` and \`to\` both default to the year's own dates, so a bare call answers "which days does this year teach on" — and its count is the denominator of every attendance percentage. \`from\` alone runs to the end of the year; \`to\` alone runs from the start.

### It counts days, not reasons

A Sunday that is also Diwali is **one** closed day. Overcounting it would quietly understate attendance on exactly the weeks a school has festivals. That is also why there is no per-type breakdown — ask Get Holiday Calendar if you need the reasons.

### Cases

| # | Query | Expected |
|---|---|---|
| 01 | none | the whole year; \`closedDayCount\` matches the calendar's |
| 02 | \`from\` only | runs to the year's end |
| 03 | \`to\` only | runs from the year's start |
| 04 | one day, closed | \`totalDayCount 1\`, \`workingDayCount 0\` |
| 05 | \`from\` after \`to\` | \`400 INVALID_DATE_RANGE\` |
| 06 | either end outside the year | \`400 DATE_OUTSIDE_ACADEMIC_YEAR\` |
| 07 | \`from=01-05-2026\` | \`400 INVALID_PARAMETER\` — dates are ISO |
`,
      pathParams: [
        { name: "name", value: "{{academicYearName}}", description: "The year name, such as 2026-2027. It is the join key and can never change." },
      ],
      queryParams: [
        { key: "from", value: "2026-11-02", enabled: true },
        { key: "to", value: "2026-11-08", enabled: true },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["academicYearName", "from", "to", "totalDayCount", "workingDayCount", "closedDayCount", "workingDays"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [],
    },
  ],
};

const GROUP_CORE_SCHOOL_PROFILE = {
  id: "core-school-profile",
  module: "Core / School — profile",
  endpoints: [
    {
      id: "update-profile",
      name: "Update Profile",
      method: "PATCH",
      path: "/schools/current/profile",
      status: 'live',
      summary: "The school's own name, account holder and contact details.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/profile\` — the school's own name and contact details.

School surface: the tenant comes from \`X-School-Subdomain\`, never from the URL.

### PATCH semantics

| Sent | Effect |
|---|---|
| omitted / \`null\` | left alone |
| \`""\` | cleared |
| a value | replaced |

\`schoolName\` cannot be cleared — \`""\` there is a \`400\`.

### Responses

| Case | Status | Code |
|---|---|---|
| One or more fields | \`200\` | — |
| Empty body | \`400\` | \`NOTHING_TO_UPDATE\` |
| Blank \`schoolName\` | \`400\` | \`SCHOOL_NAME_REQUIRED\` |
| Bad email | \`400\` | \`EMAIL_INVALID\` |
| No tenant header | \`400\` | \`TENANT_NOT_RESOLVED\` |
| Unknown tenant | \`404\` | \`SCHOOL_NOT_FOUND\` |

### The eight test cases are in the request body as comments
`,
      bodyNotes: `accountHolderName MOVED HERE on 2026-08-31 from its own platform endpoint
 (#11), which was dropped. It is a plain label — nothing links it to a
 UserAccount and nothing is granted by it — so a platform-only endpoint for
 one unreferenced string was ceremony. Like schoolName it CANNOT BE CLEARED:
 "" is a 400, not a deletion.

 Needs the X-School-Subdomain header. Run Create School first.

 HOW PATCH BEHAVES HERE — a record cannot tell "omitted" from "null", so:
    field omitted / null  -> leave it exactly as it is
    field is ""           -> clear it (null in the database)
    field has a value     -> replace it`,
      optionalFields: ["schoolName", "accountHolderName", "phoneNumber", "emailAddress"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "schoolName": "Orbit Astra International School",
  "accountHolderName": "Ankit Kumar"
}`,
      successStatus: 200,
      responseFields: ["schoolId", "subdomain", "status", "schoolName", "accountHolderName", "phoneNumber", "emailAddress", "logoUrl", "defaultLocale", "defaultTimeZone", "addressLine", "city", "stateOrProvince", "postalCode", "countryCode"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "Empty body" },
        { status: 400, code: "SCHOOL_NAME_REQUIRED", when: "Try to clear the name" },
        { status: 400, code: "EMAIL_INVALID", when: "Bad email" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "No tenant header — delete x-school-subdomain" },
        { status: 400, code: "ACCOUNT_HOLDER_NAME_REQUIRED", when: "Try to clear the account holder" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "Unknown tenant — set the header to \"nope\"" },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "RENAME ONLY — phone and email untouched",
          expect: "200 OK",
          notes: `The body above. Whitespace is trimmed.`,
          body: null,
        },
        {
          id: "02",
          name: "CLEAR THE PHONE with \"\"",
          expect: "200 OK",
          notes: `OUT: phoneNumber: null, emailAddress unchanged.`,
          body: `{
  "phoneNumber": ""
}`,
        },
        {
          id: "03",
          name: "SET PHONE AND EMAIL TOGETHER",
          expect: "200 OK",
          notes: `OUT: emailAddress is lowercased on the way in.`,
          body: `{
  "phoneNumber": "+919876543210",
  "emailAddress": "Office@Orbit-School.EDU"
}`,
        },
        {
          id: "04",
          name: "EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" }
    A PATCH that asks for nothing is a client bug. 200 would hide it.`,
          body: `{
}`,
        },
        {
          id: "05",
          name: "TRY TO CLEAR THE NAME",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "SCHOOL_NAME_REQUIRED" }
    schoolName is @NotBlank on the model, so "" is a 400, not a deletion.`,
          body: `{
  "schoolName": "   "
}`,
        },
        {
          id: "06",
          name: "BAD EMAIL",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "EMAIL_INVALID" }`,
          body: `{
  "emailAddress": "not-an-email"
}`,
        },
        {
          id: "07",
          name: "NO TENANT HEADER — delete X-School-Subdomain",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "TENANT_NOT_RESOLVED" }`,
          body: null,
        },
        {
          id: "08",
          name: "UNKNOWN TENANT — set the header to \"nope\"",
          expect: "404 Not Found",
          notes: `OUT: { "code": "SCHOOL_NOT_FOUND" }`,
          body: null,
        },
        {
          id: "09",
          name: "CHANGE THE ACCOUNT HOLDER",
          expect: "200 OK",
          notes: `OUT: accountHolderName replaced. Editable here since 2026-08-31; it used
    to be platform-only (#11), which was dropped.`,
          body: `{
  "accountHolderName": "Ankit Kumar"
}`,
        },
        {
          id: "10",
          name: "TRY TO CLEAR THE ACCOUNT HOLDER",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "ACCOUNT_HOLDER_NAME_REQUIRED" }
    @NotBlank on the model, same as schoolName. "" is a 400, not a deletion.`,
          body: `{
  "accountHolderName": "   "
}`,
        },
      ],
    },
    {
      id: "replace-address",
      name: "Replace Address",
      method: "PUT",
      path: "/schools/current/address",
      status: 'live',
      summary: "Replaces the whole address. A PUT, because patching city without state gives a place that does not exist.",
      schoolSurface: true,
      docs: `**PUT** \`/schools/current/address\` — replaces the whole postal address.

**An omitted field is cleared.** \`PUT {"city":"Mumbai"}\` wipes \`addressLine\`,
\`stateOrProvince\` and \`postalCode\`. That is correct replace semantics and the opposite of
\`PATCH /profile\` — worth knowing before writing a client.

\`countryCode\` is **not** on this request and cannot be changed here.

### The four test cases are in the request body as comments
`,
      bodyNotes: `A PUT, NOT A PATCH. An omitted field is CLEARED, not left alone. That is
 what replace means, and it is the opposite of Update Profile above.

 An address is all-or-nothing: patching city without stateOrProvince gives
 you a real-looking address for a place that does not exist.`,
      optionalFields: ["addressLine", "city", "stateOrProvince", "postalCode"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "addressLine": "12, MG Road",
  "city": "Pune",
  "stateOrProvince": "Maharashtra",
  "postalCode": "411001"
}`,
      successStatus: 200,
      responseFields: ["schoolId", "subdomain", "addressLine", "city", "stateOrProvince", "postalCode", "countryCode"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "FULL ADDRESS",
          expect: "200 OK",
          notes: `The body above.`,
          body: null,
        },
        {
          id: "02",
          name: "PARTIAL PUT — WATCH WHAT HAPPENS",
          expect: "200 OK",
          notes: `OUT: city: "Mumbai"
         addressLine: null      <- CLEARED
         stateOrProvince: null  <- CLEARED
         postalCode: null       <- CLEARED
         countryCode: unchanged

    Not a bug. Send the whole address every time.`,
          body: `{
  "city": "Mumbai"
}`,
        },
        {
          id: "03",
          name: "EMPTY BODY — removes the address entirely",
          expect: "200 OK",
          notes: `A legitimate thing to want, and the same rule as case 02.

04  countryCode IS NOT ACCEPTED HERE
{
  "city": "Dubai",
  "countryCode": "AE"
}
    OUT: 200, and countryCode is UNCHANGED. The field is not on the DTO,
         so it is ignored rather than honoured.

    Changing a school's country changes which tax rules and identity
    documents apply — GovernmentIdentityType holds AADHAAR and APAAR;
    FeeHead.taxRatePercent means GST. Schools do not move countries. A
    mistyped one is a platform correction while still PROVISIONING.`,
          body: `{
}`,
        },
      ],
    },
    {
      id: "update-localization",
      name: "Update Localization",
      method: "PATCH",
      path: "/schools/current/localization",
      status: 'live',
      summary: "Language and time zone. Changing the zone reinterprets every school-local date already recorded.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/localization\` — language and time zone.

### \`defaultTimeZone\` has two guards

1. \`confirmTimeZoneChange: true\` must be sent, or \`409\`.
2. If an academic year covers today, the change is **refused outright**.

The flag alone would be theatre. The year check is what protects the attendance register:
every \`Instant\` is UTC, so changing the zone rewrites nothing and silently reinterprets which
calendar date every existing record falls on.

The locale stays editable at all times — only the zone is dangerous.

### The six test cases are in the request body as comments
`,
      optionalFields: ["defaultLocale", "defaultTimeZone", "confirmTimeZoneChange"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "defaultLocale": "en-IN"
}`,
      successStatus: 200,
      responseFields: ["schoolId", "subdomain", "defaultLocale", "defaultTimeZone"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "Empty body" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "TIME_ZONE_CHANGE_NOT_CONFIRMED", when: "Time zone without confirmation" },
        { status: 409, code: "ACADEMIC_YEAR_IN_PROGRESS", when: "Time zone while a year is running" },
        { status: 409, code: "TIME_ZONE_INVALID", when: "Unknown zone" },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "LOCALE ONLY",
          expect: "200 OK",
          notes: `The body above. Safe at any time, including mid-year.`,
          body: null,
        },
        {
          id: "02",
          name: "TIME ZONE WITHOUT CONFIRMATION",
          expect: "409 Conflict",
          notes: `OUT: { "code": "TIME_ZONE_CHANGE_NOT_CONFIRMED" }`,
          body: `{
  "defaultTimeZone": "Asia/Dubai"
}`,
        },
        {
          id: "03",
          name: "TIME ZONE WITH CONFIRMATION, no academic year",
          expect: "200 OK",
          notes: `Works only while the school has NO year covering today.`,
          body: `{
  "defaultTimeZone": "Asia/Dubai",
  "confirmTimeZoneChange": true
}`,
        },
        {
          id: "04",
          name: "TIME ZONE WHILE A YEAR IS RUNNING",
          expect: "409 Conflict",
          notes: `Create an academic year covering today first (see the Academic Year
    folder), then send case 03 again.
    OUT: { "code": "ACADEMIC_YEAR_IN_PROGRESS" }

    THIS IS THE GUARD THAT MATTERS. Every Instant is stored in UTC, so
    changing the zone rewrites nothing — it silently reinterprets which
    calendar DATE every existing attendance record, holiday and trip falls
    on. A school moving Asia/Kolkata to Asia/Dubai mid-year has a register
    that shifts under it, with no error anywhere.

    The confirmation flag alone would be theatre; people tick boxes. This is
    what actually protects the data.`,
          body: null,
        },
        {
          id: "05",
          name: "UNKNOWN ZONE",
          expect: "409 Conflict",
          notes: `OUT: { "code": "TIME_ZONE_INVALID" }
    Checked against the JVM's IANA set. No regex can do this, and the list
    changes as zones are added and renamed.`,
          body: `{
  "defaultTimeZone": "Asia/Pune",
  "confirmTimeZoneChange": true
}`,
        },
        {
          id: "06",
          name: "EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" }`,
          body: `{
}`,
        },
      ],
    },
    {
      id: "replace-logo",
      name: "Replace Logo",
      method: "PUT",
      path: "/schools/current/logo",
      status: 'live',
      summary: "Replaces the logo. The URL must be https and on an allowed host.",
      schoolSurface: true,
      docs: `**PUT** \`/schools/current/logo\` — replaces the logo, or removes it.

\`https\` only, and the host must be on the service's allow-list. \`logoUrl: ""\` removes it, which
is why there is no separate \`DELETE\`.

A file upload would be better — a school-supplied URL can rot or be changed after approval —
but there is no storage service yet.

### The four test cases are in the request body as comments
`,
      optionalFields: ["logoUrl"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "logoUrl": "https://cdn.example.com/schools/orbit/logo.png"
}`,
      successStatus: 200,
      responseFields: ["schoolId", "subdomain", "logoUrl"],
      captures: [],
      errors: [
        { status: 400, code: "LOGO_HOST_NOT_ALLOWED", when: "Any other host" },
        { status: 400, code: "LOGO_URL_NOT_HTTPS", when: "Http instead of https" },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "ALLOW-LISTED HOST",
          expect: "200 OK",
          notes: `The body above.
    Allowed: cdn.example.com, res.cloudinary.com, s3.amazonaws.com,
             storage.googleapis.com`,
          body: null,
        },
        {
          id: "02",
          name: "REMOVE THE LOGO",
          expect: "200 OK",
          notes: `OUT: logoUrl: null. This is why there is no separate DELETE.`,
          body: `{
  "logoUrl": ""
}`,
        },
        {
          id: "03",
          name: "ANY OTHER HOST",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "LOGO_HOST_NOT_ALLOWED" }

    A school-supplied URL is loaded on pages PARENTS open. An arbitrary host
    is somebody else's server deciding what parents see, and a tracker there
    is invisible to us.`,
          body: `{
  "logoUrl": "https://evil.example.net/logo.png"
}`,
        },
        {
          id: "04",
          name: "http INSTEAD OF https",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "LOGO_URL_NOT_HTTPS" }

 NOTE — a file upload would be better than a URL, and the plan says so. A
 school-supplied URL can rot, or be changed to something unwanted after it
 was approved. There is no storage service yet.`,
          body: `{
  "logoUrl": "http://cdn.example.com/logo.png"
}`,
        },
      ],
    },
    {
      id: "get-profile",
      name: "Get Profile",
      method: "GET",
      path: "/schools/current",
      status: 'live',
      summary: "The school's own profile — the read behind the four settings forms.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current\` — the school reading its own details.

The read behind Update Profile, Replace Address, Update Localization and Replace Logo. It returns the **identical** record those four return, so a settings screen loads the form and saves it with one shape rather than two that drift apart.

\`status\` is here, because a school being told it is \`SUSPENDED\` is how its own screens explain why editing stopped working. \`statusReason\`, \`activatedAt\` and \`suspendedAt\` are **not** — those belong to the operator, on Get School.

Needs \`X-School-Subdomain\`. Resolved with \`require\`, not \`requireUsable\`, so **a suspended school can still read this** — being blocked from editing is not being blocked from looking.

### Cases

| # | Header | Expected |
|---|---|---|
| 01 | \`{{createdSubdomain}}\` | \`200\` |
| 02 | header removed | \`400 TENANT_NOT_RESOLVED\` |
| 03 | \`no-such-school\` | \`404 SCHOOL_NOT_FOUND\` |
| 04 | after Suspend School | \`200\` — the read still works |
`,
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["schoolId", "subdomain", "status", "schoolName", "accountHolderName", "phoneNumber", "emailAddress", "logoUrl", "defaultLocale", "defaultTimeZone", "addressLine", "city", "stateOrProvince", "postalCode", "countryCode"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited." },
      ],
      examples: [],
    },
  ],
};

const GROUP_CORE_SCHOOL_PLATFORM = {
  id: "core-school-platform",
  module: "Core / School — platform",
  endpoints: [
    {
      id: "create-school",
      name: "Create School",
      method: "POST",
      path: "/platform/schools",
      status: 'live',
      summary: "Makes the school row at PROVISIONING or TRIAL. That is all it does.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools\` — provision a new tenant.

Creates the \`School\` row at \`PROVISIONING\` (or \`TRIAL\`). It does **not** create a user, so the
school cannot be logged into yet — that is what \`nextStep\` in the response says.

### Required — 6 fields

| Field | Rule |
|---|---|
| \`schoolName\` | not blank, max 200 |
| \`accountHolderName\` | not blank, max 150 |
| \`subdomain\` | not blank, max 63, normalised, globally unique, not reserved |
| \`defaultLocale\` | IETF tag — \`en-IN\`, \`hi-IN\` |
| \`defaultTimeZone\` | real IANA id — \`Asia/Kolkata\` |
| \`countryCode\` | exactly 2 letters — \`IN\` |

### Optional

\`phoneNumber\` (30) · \`emailAddress\` (valid, 254) · \`addressLine\` (200) · \`city\` (100) ·
\`stateOrProvince\` (100) · \`postalCode\` (20) · \`trial\` (bool)

### Refused if sent

\`status\`, \`encryptionKeyReference\`, \`activatedAt\`, \`suspendedAt\` — the DTO has no such fields,
so they are ignored rather than honoured. Each would hand the caller something the document
defends.

### Test cases — all 10 are in the request body as comments

| # | Case | Result |
|---|---|---|
| 01 | Full payload | 201 |
| 02 | Minimum payload | 201 |
| 03 | Trial tenant | 201, \`status: TRIAL\` |
| 04 | Subdomain normalisation | 201, \`Norm_Check 12\` → \`norm-check-12\` |
| 05 | Duplicate subdomain | 409 \`SUBDOMAIN_TAKEN\` |
| 06 | Reserved subdomain | 409 \`SUBDOMAIN_RESERVED\` |
| 07 | Malformed subdomain | 409 \`SUBDOMAIN_INVALID\` |
| 08 | Unknown time zone | 409 \`TIME_ZONE_INVALID\` |
| 09 | Missing/invalid fields | 400 \`VALIDATION_FAILED\` + \`fieldErrors\` |
| 10 | Malformed JSON | 400 \`MALFORMED_REQUEST\` |
`,
      requiredFields: ["schoolName", "accountHolderName", "subdomain", "defaultLocale", "defaultTimeZone", "countryCode"],
      optionalFields: ["phoneNumber", "emailAddress", "addressLine", "city", "stateOrProvince", "postalCode", "trial"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "schoolName": "Orbit Astra International School",
  "accountHolderName": "Rohan Shinde",
  "subdomain": "orbit-astra-{{$timestamp}}",
  "phoneNumber": "+919876543210",
  "emailAddress": "admin@orbit-school.edu",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "countryCode": "IN",
  "addressLine": "12, MG Road",
  "city": "Pune",
  "stateOrProvince": "Maharashtra",
  "postalCode": "411001",
  "trial": false
}`,
      successStatus: 201,
      successNote: "Also sends a Location header: /platform/schools/{schoolId}",
      responseFields: ["schoolId", "schoolName", "subdomain", "status", "createdAt", "nextStep"],
      captures: [
        { variable: "schoolId", from: "schoolId" },
        { variable: "createdSubdomain", from: "subdomain" },
      ],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "Missing and invalid fields" },
        { status: 400, code: "MALFORMED_REQUEST", when: "Malformed json" },
        { status: 409, code: "SUBDOMAIN_TAKEN", when: "Duplicate subdomain" },
        { status: 409, code: "SUBDOMAIN_RESERVED", when: "Reserved subdomain" },
        { status: 409, code: "SUBDOMAIN_INVALID", when: "Malformed subdomain" },
        { status: 409, code: "TIME_ZONE_INVALID", when: "Unknown time zone" },
      ],
      examples: [
        {
          id: "01",
          name: "FULL PAYLOAD",
          expect: "201 Created",
          notes: `The body above. Every field the endpoint accepts.
    OUT: { schoolId, schoolName, subdomain, status:"PROVISIONING",
           createdAt, nextStep }
    Header: Location: /platform/schools/{schoolId}`,
          body: null,
        },
        {
          id: "02",
          name: "MINIMUM PAYLOAD — only the 6 required fields",
          expect: "201 Created",
          notes: `Everything omitted is stored as null, not "".`,
          body: `{
  "schoolName": "Minimum Fields School",
  "accountHolderName": "Ankit Kumar",
  "subdomain": "minimum-{{$timestamp}}",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "countryCode": "IN"
}`,
        },
        {
          id: "03",
          name: "TRIAL TENANT",
          expect: "201 Created",
          notes: `OUT: status is "TRIAL" instead of "PROVISIONING".
    Those are the only two legal starting states; a caller cannot ask
    for ACTIVE, because that would skip the subscription check.`,
          body: `{
  "schoolName": "Trial School",
  "accountHolderName": "Ankit Kumar",
  "subdomain": "trial-{{$timestamp}}",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "countryCode": "IN",
  "trial": true
}`,
        },
        {
          id: "04",
          name: "SUBDOMAIN IS NORMALISED BEFORE STORING",
          expect: "201 Created",
          notes: `IN : "  Norm_Check 123  "
    OUT: "norm-check-123"   (trimmed, lowercased, [space _] -> -)
    The subdomain in the RESPONSE is the one to use afterwards.`,
          body: `{
  "schoolName": "Normalisation Check",
  "accountHolderName": "Ankit Kumar",
  "subdomain": "  Norm_Check {{$timestamp}}  ",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "countryCode": "IN"
}`,
        },
        {
          id: "05",
          name: "DUPLICATE SUBDOMAIN",
          expect: "409 Conflict",
          notes: `Run case 01 first: it saves {{createdSubdomain}}.
    OUT: { "code": "SUBDOMAIN_TAKEN", "message": "...already in use." }
    409 not 400 — the request is well formed, the name is simply taken.`,
          body: `{
  "schoolName": "Duplicate Attempt",
  "accountHolderName": "Ankit Kumar",
  "subdomain": "{{createdSubdomain}}",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "countryCode": "IN"
}`,
        },
        {
          id: "06",
          name: "RESERVED SUBDOMAIN",
          expect: "409 Conflict",
          notes: `OUT: { "code": "SUBDOMAIN_RESERVED" }
    Also reserved: www admin login auth app cdn mail api status support
    docs blog test staging dev billing webhooks ... (~40)
    A school owning "login" would receive credentials meant for the platform.`,
          body: `{
  "schoolName": "Reserved Attempt",
  "accountHolderName": "Ankit Kumar",
  "subdomain": "api",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "countryCode": "IN"
}`,
        },
        {
          id: "07",
          name: "MALFORMED SUBDOMAIN",
          expect: "409 Conflict",
          notes: `OUT: { "code": "SUBDOMAIN_INVALID" }
    Leading/trailing hyphens are not a valid DNS label.
    Also rejected: "" , 64+ chars, anything outside [a-z0-9-]`,
          body: `{
  "schoolName": "Bad Shape",
  "accountHolderName": "Ankit Kumar",
  "subdomain": "-bad-",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata",
  "countryCode": "IN"
}`,
        },
        {
          id: "08",
          name: "UNKNOWN TIME ZONE",
          expect: "409 Conflict",
          notes: `IN : "Asia/Pune"  — looks reasonable, does not exist
    OUT: { "code": "TIME_ZONE_INVALID" }
    Checked against the JVM's IANA zone set. No regex can do this, and
    the zone decides which calendar DATE an attendance record falls on.`,
          body: `{
  "schoolName": "Bad Zone",
  "accountHolderName": "Ankit Kumar",
  "subdomain": "bad-zone-{{$timestamp}}",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Pune",
  "countryCode": "IN"
}`,
        },
        {
          id: "09",
          name: "MISSING AND INVALID FIELDS",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "VALIDATION_FAILED",
            "fieldErrors": { "schoolName":        ["must not be blank"],
                             "accountHolderName": ["must not be blank"],
                             "countryCode":       ["must be a two-letter..."],
                             "emailAddress":      ["must be a well-formed..."] } }
    Caught by Jakarta annotations before the controller is entered,
    which is why the errors are per-field.`,
          body: `{
  "schoolName": "",
  "subdomain": "validation-{{$timestamp}}",
  "emailAddress": "not-an-email",
  "countryCode": "ZZZ",
  "defaultLocale": "en-IN",
  "defaultTimeZone": "Asia/Kolkata"
}`,
        },
        {
          id: "10",
          name: "MALFORMED JSON",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "MALFORMED_REQUEST" }  and NO stack trace in the body.
    Send exactly this, comments removed:      {"schoolName": }`,
          body: null,
        },
      ],
    },
    {
      id: "complete-provisioning",
      name: "Complete Provisioning",
      method: "POST",
      path: "/platform/schools/{id}/complete-provisioning",
      status: 'live',
      summary: "Finishes the setup: seeds 47 number sequences and the starting roles. Safe to run twice.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/complete-provisioning\` — finishes a tenant's setup.

Creating a school leaves it with **no number sequences and no roles**. Neither absence is
visible at creation; both show up later, to whoever tries to use the school:

| Missing | Fails when |
|---|---|
| \`NumberSequence\` rows | the first student admission asks for a number and finds no counter |
| \`Role\` rows | the first \`UserAccount\` is created and has nothing to point \`roleDocsIds\` at |

This closes both gaps, and must run before the school can be activated.

### Request

No body. \`{{schoolId}}\` in the path — saved automatically by **Create School**.

### Idempotent

Reads what exists and inserts only the gaps. Safe to send repeatedly; safe to send when you
do not know what state the school is in. That is the point of it.

### Responses

| Case | Status | Code |
|---|---|---|
| Fresh school | \`200\` | — 47 sequences, 3 roles created |
| Sent again | \`200\` | — 0 created, everything already present |
| Partial repair | \`200\` | — only the gaps created |
| Unknown id | \`404\` | \`SCHOOL_NOT_FOUND\` |
| Offboarding / closed / deleted | \`409\` | \`SCHOOL_NOT_PROVISIONABLE\` |

\`readyToActivate\` answers the operator's real question: every sequence type has a row and
\`SCHOOL_ADMIN\` exists.

### The five test cases are in the request body as comments
`,
      bodyNotes: `This endpoint takes NO BODY. It is a POST because it performs an action,
 not because it sends data. The {id} comes from the URL.

 So the cases below are about which {{schoolId}} you point it at, and what
 state that school is in. Set the variable, then Send.

 Safe to send as-is: the controller has no @RequestBody, so Spring ignores
 whatever is here even if your Postman does not strip comments.`,
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["schoolId", "subdomain", "status", "numberSequencesCreated", "numberSequencesAlreadyPresent", "rolesCreated", "rolesAlreadyPresent", "roleKeys", "readyToActivate", "nextStep"],
      captures: [
        { variable: "schoolId", from: "schoolId" },
        { variable: "createdSubdomain", from: "subdomain" },
      ],
      errors: [
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "Unknown school id" },
        { status: 409, code: "SCHOOL_NOT_PROVISIONABLE", when: "Shut-down tenant" },
      ],
      examples: [
        {
          id: "01",
          name: "FIRST CALL on a freshly created school",
          expect: "200 OK",
          notes: `Run "Create School" first — it saves {{schoolId}}.
    OUT: numberSequencesCreated: 47,  numberSequencesAlreadyPresent: 0
         rolesCreated: 3,             rolesAlreadyPresent: 0
         roleKeys: ["GUARDIAN","SCHOOL_ADMIN","TEACHER"]
         readyToActivate: true`,
          body: null,
        },
        {
          id: "02",
          name: "SEND IT AGAIN — idempotency",
          expect: "200 OK",
          notes: `Same {{schoolId}}, no changes needed. Nothing is written.
    OUT: numberSequencesCreated: 0,  numberSequencesAlreadyPresent: 47
         rolesCreated: 0,            rolesAlreadyPresent: 3
         readyToActivate: true

    Still 200, not 201. A call that created nothing because everything was
    already there is a success — 201 Created would be a lie.`,
          body: null,
        },
        {
          id: "03",
          name: "PARTIAL REPAIR — the case this endpoint exists for",
          expect: "200 OK",
          notes: `Delete some rows in MongoDB first, then send again:

      db.number_sequences.deleteMany({schoolId: "<id>", sequenceType: "FEE_INVOICE"})
      db.roles.deleteOne({schoolId: "<id>", roleKey: "TEACHER"})

    OUT: numberSequencesCreated: 1,  numberSequencesAlreadyPresent: 46
         rolesCreated: 1,            rolesAlreadyPresent: 2

    It fills only the gaps. An existing role is skipped, never overwritten —
    so a school that edited SCHOOL_ADMIN's permissions keeps them.`,
          body: null,
        },
        {
          id: "04",
          name: "UNKNOWN SCHOOL ID",
          expect: "404 Not Found",
          notes: `Set {{schoolId}} to 6a90000000000000000000aa
    OUT: { "code": "SCHOOL_NOT_FOUND",
           "message": "No school found with id '...'." }`,
          body: null,
        },
        {
          id: "05",
          name: "SHUT-DOWN TENANT",
          expect: "409 Conflict",
          notes: `Set the school to an end-of-life status first:

      db.schools.updateOne({_id: ObjectId("<id>")}, {$set: {status: "CLOSED"}})

    OUT: { "code": "SCHOOL_NOT_PROVISIONABLE",
           "message": "A school at status CLOSED cannot be provisioned." }

    Refused for OFFBOARDING, CLOSED, DELETION_PENDING and DELETED. Seeding
    one would quietly bring rows back to a school somebody deliberately shut
    down. Every other status is allowed, including ACTIVE — so a school found
    to be missing a role after go-live is fixed here, not by hand in the
    database.`,
          body: null,
        },
      ],
    },
    {
      id: "activate-school",
      name: "Activate School",
      method: "POST",
      path: "/platform/schools/{id}/activate",
      status: 'live',
      summary: "Takes the school live. PROVISIONING or TRIAL to ACTIVE. Refuses a second call.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/activate\` — takes the school live.

\`PROVISIONING\` or \`TRIAL\` → \`ACTIVE\`. Anything else is a \`409\`.

### Request

No body. \`{{schoolId}}\` in the path — saved by **Create School**.

### Order matters

**Run Complete Provisioning first.** Activation refuses a school that has no \`SCHOOL_ADMIN\`
role or is missing any number sequence, because either one produces a live school that fails
on first use.

### Not idempotent

Unlike **Complete Provisioning**, sending this twice is a \`409\`. \`activatedAt\` is stamped once
and never rewritten, so a school suspended and brought back keeps its original go-live date —
\`firstActivation\` tells you which happened. Bringing a suspended school back is endpoint #5
\`reactivate\`.

### Responses

| Case | Status | Code |
|---|---|---|
| Provisioned school, or TRIAL | \`200\` | — \`ACTIVE\`, \`activatedAt\` set |
| No SCHOOL_ADMIN role | \`409\` | \`SETUP_INCOMPLETE\` |
| Missing number sequences | \`409\` | \`SETUP_INCOMPLETE\` (with the count) |
| Already ACTIVE, or SUSPENDED | \`409\` | \`SCHOOL_NOT_ACTIVATABLE\` |
| Subscription CANCELLED / EXPIRED | \`409\` | \`SUBSCRIPTION_NOT_ACTIVE\` |
| No subscription at all | \`200\` | — allowed, and \`subscriptionNote\` says why |
| Unknown id | \`404\` | \`SCHOOL_NOT_FOUND\` |

### The eight test cases are in the request body as comments
`,
      bodyNotes: `No body. The {id} comes from the URL, so the cases below are about which
 {{schoolId}} you point it at and what state that school is in.

 Safe to send as-is: the controller has no @RequestBody, so Spring ignores
 whatever is here.`,
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["schoolId", "subdomain", "status", "activatedAt", "firstActivation", "subscriptionStatus", "subscriptionNote", "nextStep"],
      captures: [
        { variable: "schoolId", from: "schoolId" },
        { variable: "createdSubdomain", from: "subdomain" },
      ],
      errors: [
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "Unknown school id" },
        { status: 409, code: "SETUP_INCOMPLETE", when: "Too early — no roles yet" },
        { status: 409, code: "SCHOOL_NOT_ACTIVATABLE", when: "Send it again — already live" },
        { status: 409, code: "SUBSCRIPTION_NOT_ACTIVE", when: "Cancelled subscription" },
      ],
      examples: [
        {
          id: "01",
          name: "HAPPY PATH — setup done first",
          expect: "200 OK",
          notes: `Run  Create School  ->  Complete Provisioning  ->  this.
    OUT: status: "ACTIVE"
         activatedAt: <now>
         firstActivation: true
         subscriptionStatus: "NONE"`,
          body: null,
        },
        {
          id: "02",
          name: "TOO EARLY — no roles yet",
          expect: "409 Conflict",
          notes: `Create a school and send this WITHOUT Complete Provisioning.
    OUT: { "code": "SETUP_INCOMPLETE",
           "message": "This school has no SCHOOL_ADMIN role. Run
                       complete-provisioning first." }

    Refused because a school with no SCHOOL_ADMIN role has nothing to attach
    a first administrator to. Activating it produces a live school nobody
    can log into.`,
          body: null,
        },
        {
          id: "03",
          name: "TOO EARLY — a sequence is missing",
          expect: "409 Conflict",
          notes: `Complete provisioning, then remove one row:

      db.number_sequences.deleteOne({schoolId: "<id>", sequenceType: "FEE_INVOICE"})

    OUT: { "code": "SETUP_INCOMPLETE",
           "message": "This school has 46 of 47 number sequences. Run
                       complete-provisioning first." }

    The count is in the message on purpose — "incomplete" without a number
    leaves you guessing what is missing.`,
          body: null,
        },
        {
          id: "04",
          name: "SEND IT AGAIN — already live",
          expect: "409 Conflict",
          notes: `OUT: { "code": "SCHOOL_NOT_ACTIVATABLE",
           "message": "A school at status ACTIVE cannot be activated. Only
                       PROVISIONING and TRIAL can. A suspended school is
                       reactivated, not activated." }

    NOT idempotent, unlike Complete Provisioning. This one refuses, because
    the caller believes they changed something and they did not. Bringing a
    suspended school back is endpoint #5 reactivate — a different operation.`,
          body: null,
        },
        {
          id: "05",
          name: "TRIAL SCHOOL",
          expect: "200 OK",
          notes: `Create with "trial": true, complete provisioning, then send this.
    TRIAL and PROVISIONING are both allowed starting states.
    OUT: status: "ACTIVE", firstActivation: true`,
          body: null,
        },
        {
          id: "06",
          name: "CANCELLED SUBSCRIPTION",
          expect: "409 Conflict",
          notes: `Give the school a dead subscription first:

      db.school_subscriptions.insertOne({
        schoolId: "<id>", subscriptionNo: "SUB/T/1",
        planDefinitionDocsId: "x", planVersion: 1,
        status: "CANCELLED", billingCycle: "YEARLY",
        autoRenew: false, current: true, recordState: "ACTIVE" })

    OUT: { "code": "SUBSCRIPTION_NOT_ACTIVE",
           "message": "The school's subscription is CANCELLED. It cannot be
                       activated." }

    Blocked for CANCELLED and EXPIRED only. ACTIVE, TRIAL, PAST_DUE and
    SUSPENDED all pass — a school behind on payment is not a school that
    should be shut out mid-term.

    billingCycle must be MONTHLY | QUARTERLY | HALF_YEARLY | YEARLY | CUSTOM.
    An invalid value written straight to MongoDB comes back as a 500, because
    nothing validates enums on read.`,
          body: null,
        },
        {
          id: "07",
          name: "NO SUBSCRIPTION AT ALL",
          expect: "200 OK",
          notes: `The ordinary case today. Activation is ALLOWED and says so:

    OUT: subscriptionStatus: "NONE"
         subscriptionNote: "No subscription exists for this school.
                            Activation was allowed anyway because nothing
                            creates subscriptions yet — this check must
                            become a hard requirement once it does."

    The plan says activation requires an active subscription. Nothing creates
    one yet, so enforcing it strictly would make this endpoint unusable. The
    response announces the gap rather than hiding it.`,
          body: null,
        },
        {
          id: "08",
          name: "UNKNOWN SCHOOL ID",
          expect: "404 Not Found",
          notes: `Set {{schoolId}} to 6a90000000000000000000aa
    OUT: { "code": "SCHOOL_NOT_FOUND" }`,
          body: null,
        },
      ],
    },
    {
      id: "suspend-school",
      name: "Suspend School",
      method: "POST",
      path: "/platform/schools/{id}/suspend",
      status: 'live',
      summary: "Blocks a live school. ACTIVE to SUSPENDED. A reason is required.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/suspend\` — blocks a school.

\`ACTIVE\` → \`SUSPENDED\`. Anything else is a \`409\`.

### Request

\`\`\`json
{ "reason": "Non-payment. Third invoice unpaid past 60 days." }
\`\`\`

\`reason\` is **required**, max 500 chars. Stored on \`School.statusReason\` and kept after
reactivation, so "this was suspended in August for non-payment" survives being brought back.

### Responses

| Case | Status | Code |
|---|---|---|
| Active school, reason given | \`200\` | — \`SUSPENDED\`, \`suspendedAt\` + \`statusReason\` set |
| Missing or blank reason | \`400\` | \`VALIDATION_FAILED\` |
| Not ACTIVE | \`409\` | \`SCHOOL_NOT_SUSPENDABLE\` |
| Already suspended | \`409\` | \`SCHOOL_NOT_SUSPENDABLE\` |
| Unknown id | \`404\` | \`SCHOOL_NOT_FOUND\` |

### Known gap

Does **not** revoke live sessions or stop scheduled jobs — neither service exists yet. A
suspended school's users stay logged in until their tokens expire.

### The six test cases are in the request body as comments
`,
      bodyNotes: `{{schoolId}} must be an ACTIVE school — run Activate School first.`,
      requiredFields: ["reason"],
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "reason": "Non-payment. Third invoice unpaid past 60 days."
}`,
      successStatus: 200,
      responseFields: ["schoolId", "subdomain", "status", "activatedAt", "suspendedAt", "statusReason", "nextStep"],
      captures: [
        { variable: "schoolId", from: "schoolId" },
      ],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "No reason" },
        { status: 400, code: "—", when: "Blank reason — same as missing" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "Unknown school id" },
        { status: 409, code: "SCHOOL_NOT_SUSPENDABLE", when: "School not active yet" },
      ],
      examples: [
        {
          id: "01",
          name: "SUSPEND AN ACTIVE SCHOOL",
          expect: "200 OK",
          notes: `The body above.
    OUT: status: "SUSPENDED"
         suspendedAt: <now>
         statusReason: "Non-payment. Third invoice unpaid past 60 days."
         activatedAt: <unchanged — still the original go-live date>`,
          body: null,
        },
        {
          id: "02",
          name: "NO REASON",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "VALIDATION_FAILED",
           "fieldErrors": { "reason": ["must not be blank"] } }

    The reason is REQUIRED, unlike on reactivate. Suspension stops a whole
    school, and one with no reason written down gets switched back on by the
    next person who is asked about it.`,
          body: `{
}`,
        },
        {
          id: "03",
          name: "BLANK REASON — same as missing",
          expect: "400 Bad Request",
          notes: `@NotBlank, not @NotNull. Whitespace does not count as an answer.`,
          body: `{
  "reason": "   "
}`,
        },
        {
          id: "04",
          name: "SCHOOL NOT ACTIVE YET",
          expect: "409 Conflict",
          notes: `Point {{schoolId}} at a school you have NOT activated.
    OUT: { "code": "SCHOOL_NOT_SUSPENDABLE",
           "message": "A school at status PROVISIONING cannot be suspended.
                       Only ACTIVE can." }

    Suspending a school that was never usable makes no sense.`,
          body: null,
        },
        {
          id: "05",
          name: "SUSPEND TWICE",
          expect: "409 Conflict",
          notes: `Send case 01 again.
    OUT: { "code": "SCHOOL_NOT_SUSPENDABLE",
           "message": "A school at status SUSPENDED cannot be suspended..." }

    Not idempotent on purpose: the caller believes they changed something.`,
          body: null,
        },
        {
          id: "06",
          name: "UNKNOWN SCHOOL ID",
          expect: "404 Not Found",
          notes: `Set {{schoolId}} to 6a90000000000000000000aa
    OUT: { "code": "SCHOOL_NOT_FOUND" }

 NOT DONE YET — worth knowing before you rely on this
 Suspension does NOT revoke live sessions or stop scheduled jobs. Neither
 service exists. A suspended school's users stay logged in until their
 tokens expire, so this is a flag rather than a lock.`,
          body: null,
        },
      ],
    },
    {
      id: "reactivate-school",
      name: "Reactivate School",
      method: "POST",
      path: "/platform/schools/{id}/reactivate",
      status: 'live',
      summary: "Lets a suspended school back in. SUSPENDED to ACTIVE. The body is optional.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/reactivate\` — brings a suspended school back.

\`SUSPENDED\` → \`ACTIVE\`. Anything else is a \`409\`.

### Request — optional

\`\`\`json
{ "note": "Outstanding invoices cleared on 31 August." }
\`\`\`

The body may be **omitted entirely**. When given, \`note\` replaces \`School.statusReason\`; when
omitted, the suspension reason stays.

### What it deliberately does not reset

| Field | Why |
|---|---|
| \`activatedAt\` | the original go-live date, not a status flag |
| \`suspendedAt\` | the *most recent* suspension — how you see it has happened before |
| \`statusReason\` | kept unless a note replaces it |

### Not the same as activate

Reactivate skips the setup and subscription checks. A suspended school was already live once,
so it passed them — re-running them would mean a school suspended for non-payment could never
be let back in as a goodwill gesture.

### Responses

| Case | Status | Code |
|---|---|---|
| Suspended school | \`200\` | — \`ACTIVE\` |
| Already active | \`409\` | \`SCHOOL_NOT_REACTIVATABLE\` |
| Never went live | \`409\` | \`SCHOOL_NOT_REACTIVATABLE\` |
| Unknown id | \`404\` | \`SCHOOL_NOT_FOUND\` |

### The six test cases are in the request body as comments
`,
      bodyNotes: `{{schoolId}} must be a SUSPENDED school.`,
      optionalFields: ["note"],
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "note": "Outstanding invoices cleared on 31 August."
}`,
      successStatus: 200,
      responseFields: ["schoolId", "subdomain", "status", "activatedAt", "suspendedAt", "statusReason", "nextStep"],
      captures: [
        { variable: "schoolId", from: "schoolId" },
      ],
      errors: [
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "Unknown school id" },
        { status: 409, code: "SCHOOL_NOT_REACTIVATABLE", when: "School is already active" },
      ],
      examples: [
        {
          id: "01",
          name: "REACTIVATE WITH A NOTE",
          expect: "200 OK",
          notes: `The body above. The note REPLACES the stored statusReason.
    OUT: status: "ACTIVE"
         statusReason: "Outstanding invoices cleared on 31 August."`,
          body: null,
        },
        {
          id: "02",
          name: "REACTIVATE WITH NO BODY AT ALL",
          expect: "200 OK",
          notes: `Delete the whole body. The endpoint accepts an absent body.
    OUT: status: "ACTIVE"
         statusReason: <the suspension reason, KEPT>
         suspendedAt:  <KEPT — the most recent suspension>
         activatedAt:  <unchanged — the original go-live date>

    The note is OPTIONAL here, unlike the reason on suspend. Letting a school
    back in usually means the problem was settled; forcing a sentence there
    produces "resolved" typed a hundred times, which looks like a record and
    is not.`,
          body: null,
        },
        {
          id: "03",
          name: "WHAT IS DELIBERATELY *NOT* RESET",
          expect: "200 OK",
          notes: `Run case 02 and read the response carefully:

      activatedAt  NOT re-stamped  — a school suspended in June and brought
                                     back in July keeps its April go-live date
      suspendedAt  NOT cleared     — it is the MOST RECENT suspension, and
                                     keeping it is how you see this school has
                                     been suspended before
      statusReason NOT cleared     — unless a note is sent

    All three are history, not current state.`,
          body: null,
        },
        {
          id: "04",
          name: "SCHOOL IS ALREADY ACTIVE",
          expect: "409 Conflict",
          notes: `OUT: { "code": "SCHOOL_NOT_REACTIVATABLE",
           "message": "A school at status ACTIVE cannot be reactivated. Only
                       SUSPENDED can. A school that has never gone live is
                       activated, not reactivated." }`,
          body: null,
        },
        {
          id: "05",
          name: "SCHOOL NEVER WENT LIVE",
          expect: "409 Conflict",
          notes: `Point {{schoolId}} at a PROVISIONING school.
    OUT: { "code": "SCHOOL_NOT_REACTIVATABLE" }

    Use  Activate School  for a school that has never been live. Reactivate is
    only for bringing a suspended one back.`,
          body: null,
        },
        {
          id: "06",
          name: "UNKNOWN SCHOOL ID",
          expect: "404 Not Found",
          notes: `Set {{schoolId}} to 6a90000000000000000000aa
    OUT: { "code": "SCHOOL_NOT_FOUND" }

 NOTE — activate vs reactivate
 Reactivate does NOT re-run the setup and subscription checks that guard
 Activate School. A suspended school was already live once, so it passed
 them. Re-running them would mean a school suspended for non-payment could
 never be let back in as a goodwill gesture, which is what this is for.`,
          body: null,
        },
      ],
    },
    {
      id: "change-subdomain",
      name: "Change Subdomain",
      method: "PATCH",
      path: "/platform/schools/{id}/subdomain",
      status: 'live',
      summary: "Changes the key that finds the tenant. Breaks every saved link, so it asks for the old value back.",
      schoolSurface: false,
      docs: `**PATCH** \`/platform/schools/{id}/subdomain\` — changes the label a school answers to.

The subdomain is the **globally unique key that resolves a request to a tenant**, so this is not
a profile edit — it moves the school's address. #6 deliberately has no field for it.

### The body confirms the current subdomain

\`currentSubdomain\` must match what the school answers to today. Nothing reads it; it exists so
the one endpoint that can take a tenant off the air cannot be aimed at the wrong one by a
mis-pasted id.

### The old label is released, not reserved

Nothing redirects and nobody is told. Every link using the old label is dead, and the next school
to ask can claim it. The response says so in \`nextStep\`.

### The nine test cases are in the request body as comments
`,
      bodyNotes: `Platform surface. Needs {{schoolId}} — run Create School first.

 THE SUBDOMAIN IS THE KEY THAT RESOLVES A REQUEST TO A TENANT. This is not a
 profile edit; it moves the school's address. That is why it is here and not
 on #6, which has no field for it.

 currentSubdomain MUST MATCH what the school answers to today. Nothing reads
 the value — it exists so the one endpoint that can take a tenant off the
 air cannot be aimed at the wrong one by a mis-pasted id. It is the only
 confirmation field in this package.

 THE OLD LABEL IS RELEASED IMMEDIATELY. Nothing reserves it, nothing
 redirects, and the school is NOT told. Every bookmark, saved link and
 stored login pointing at the old label is dead the moment this returns, and
 the next school to ask can claim it.

 AFTER RUNNING THIS, {{createdSubdomain}} IS STALE. The tests below update
 it, so the school-surface requests keep working. If you run this by hand,
 fix the variable or every /schools/current request starts 404ing.`,
      requiredFields: ["currentSubdomain", "newSubdomain"],
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "currentSubdomain": "{{createdSubdomain}}",
  "newSubdomain": "orbit-astra-renamed-{{$timestamp}}"
}`,
      successStatus: 200,
      responseFields: ["schoolId", "schoolName", "previousSubdomain", "subdomain", "nextStep"],
      captures: [
        { variable: "createdSubdomain", from: "subdomain" },
      ],
      errors: [
        { status: 400, code: "—", when: "No confirmation field" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "Unknown school id" },
        { status: 409, code: "SUBDOMAIN_CONFIRMATION_MISMATCH", when: "Wrong confirmation" },
        { status: 409, code: "SUBDOMAIN_UNCHANGED", when: "Same as the current one" },
        { status: 409, code: "SUBDOMAIN_RESERVED", when: "A reserved word" },
        { status: 409, code: "SUBDOMAIN_INVALID", when: "A bad shape" },
        { status: 409, code: "SUBDOMAIN_TAKEN", when: "Already in use" },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "A school being deleted" },
      ],
      examples: [
        {
          id: "01",
          name: "CHANGE THE SUBDOMAIN",
          expect: "200 OK",
          notes: `The body above.
    OUT: previousSubdomain: the old one, subdomain: the new one
         nextStep: "Every link, bookmark and saved login using '...' is now
                    dead — nothing redirects, and the school has NOT been
                    told. '...' is now free for any school to claim."`,
          body: null,
        },
        {
          id: "02",
          name: "WRONG CONFIRMATION",
          expect: "409 Conflict",
          notes: `OUT: { "code": "SUBDOMAIN_CONFIRMATION_MISMATCH",
           "message": "This school answers to '...', not '...'." }
    THE GUARD THAT MATTERS MOST HERE.`,
          body: `{
  "currentSubdomain": "not-this-school",
  "newSubdomain": "anything-else"
}`,
        },
        {
          id: "03",
          name: "SAME AS THE CURRENT ONE",
          expect: "409 Conflict",
          notes: `Set newSubdomain equal to currentSubdomain.
    OUT: { "code": "SUBDOMAIN_UNCHANGED" }`,
          body: null,
        },
        {
          id: "04",
          name: "A RESERVED WORD",
          expect: "409 Conflict",
          notes: `OUT: { "code": "SUBDOMAIN_RESERVED" }
    A school on 'login' or 'api' would receive traffic and credentials meant
    for the platform. Same list #1 uses — one list, not two to keep in step.`,
          body: `{
  "currentSubdomain": "{{createdSubdomain}}",
  "newSubdomain": "login"
}`,
        },
        {
          id: "05",
          name: "A BAD SHAPE",
          expect: "409 Conflict",
          notes: `OUT: { "code": "SUBDOMAIN_INVALID" }
    No leading or trailing hyphen; lowercase letters, digits, inner hyphens.`,
          body: `{
  "currentSubdomain": "{{createdSubdomain}}",
  "newSubdomain": "-bad-"
}`,
        },
        {
          id: "06",
          name: "ALREADY IN USE",
          expect: "409 Conflict",
          notes: `Create a second school, then send its subdomain here.
    OUT: { "code": "SUBDOMAIN_TAKEN" }`,
          body: null,
        },
        {
          id: "07",
          name: "NO CONFIRMATION FIELD",
          expect: "400 Bad Request",
          notes: `OUT: fieldErrors: { "currentSubdomain": "must not be blank" }
    400, not 409 — the request is malformed, not refused.`,
          body: `{
  "newSubdomain": "orbit-astra-2"
}`,
        },
        {
          id: "08",
          name: "UNKNOWN SCHOOL ID",
          expect: "404 Not Found",
          notes: `OUT: { "code": "SCHOOL_NOT_FOUND" }`,
          body: null,
        },
        {
          id: "09",
          name: "A SCHOOL BEING DELETED",
          expect: "409 Conflict",
          notes: `Only reachable once #15 to #17 exist.
    OUT: { "code": "SCHOOL_NOT_EDITABLE" }`,
          body: null,
        },
      ],
    },
    {
      id: "list-schools",
      name: "List Schools",
      method: "GET",
      path: "/platform/schools",
      status: 'live',
      summary: "The operator's school list: filtered, searched, sorted and paged. Every parameter is optional.",
      schoolSurface: false,
      docs: `**GET** \`/platform/schools\` — the operator's school list: filtered, searched, sorted, paged.

Every parameter is optional. A bare call returns the newest twenty.

| Parameter | Meaning |
|---|---|
| \`status\` | repeatable — \`?status=ACTIVE&status=TRIAL\` means either |
| \`search\` | partial, case-insensitive, on **school name or subdomain** |
| \`countryCode\`, \`city\` | exact, case-insensitive |
| \`createdFrom\`, \`createdTo\` | ISO instants, inclusive |
| \`page\`, \`size\` | zero-based; size defaults to 20, max 100 |
| \`sort\` | \`field,direction\` — \`name\`, \`schoolName\`, \`subdomain\`, \`status\`, \`createdAt\`, \`updatedAt\` |

Filters combine with AND; only \`status\` is OR within itself.

### Everything happens in the database

Filtering, searching, sorting and paging are all on the query, so one page of documents is read
however many tenants exist.

### The fifteen test cases are in the description below

Postman sends no body on a GET, so they live here:

\`\`\`
01  BARE LIST                                             -> 200 OK
    GET /platform/schools
    Newest first, twenty rows. content + page, size,
    totalElements, totalPages, hasNext, hasPrevious.

02  SEARCH, CASE-INSENSITIVE AND PARTIAL                  -> 200 OK
    ?search=ORBIT   matches "Orbit Astra International School"
    ?search=orbit-astra-17  matches by SUBDOMAIN too.
    Name and subdomain only — searching the address as well would make
    ?search=pune return every school in the city.

03  FILTER BY STATUS                                      -> 200 OK
    ?status=ACTIVE

04  SEVERAL STATUSES — repeat the parameter               -> 200 OK
    ?status=ACTIVE&status=TRIAL
    OR within the field: "show me the live ones" is one question.

05  FILTER BY COUNTRY AND CITY                            -> 200 OK
    ?countryCode=in&city=pune        both case-insensitive, both exact

06  FILTER BY CREATION DATE                               -> 200 OK
    ?createdFrom=2026-01-01T00:00:00Z&createdTo=2026-12-31T23:59:59Z

07  SORT                                                  -> 200 OK
    ?sort=name,asc      ?sort=name,desc      ?sort=createdAt,desc
    sort=name is an alias for schoolName. Case-insensitive: CreatedAt works.
    EVERY SORT ENDS WITH id. Without a tiebreaker, paging over rows with
    equal sort keys can show one twice and miss another — a bug that only
    appears in production, only on page two.

08  PAGINATE                                              -> 200 OK
    ?sort=name,asc&page=0&size=1   then   &page=1
    hasPrevious flips to true on page 1.

09  ALL OF IT AT ONCE                                     -> 200 OK
    ?status=ACTIVE&search=orbit&countryCode=IN&page=0&size=20&sort=name,asc

10  NO MATCHES                                            -> 200 OK
    ?search=zzz-nothing
    OUT: content: [], totalElements: 0, totalPages: 0
    A 200 with an empty list, NOT a 404. "No school matches" is a
    successful answer to the question asked.

11  size=0  or  size=5000                            -> 400 Bad Request
    OUT: { "code": "INVALID_PAGE_SIZE",
           "message": "size must be between 1 and 100. Received: 5000" }
    REFUSED, NOT CLAMPED. Quietly returning 100 rows for size=5000 looks
    like the whole result, which is how somebody comes to believe they
    have seen every school.

12  page=-1                                          -> 400 Bad Request
    OUT: { "code": "INVALID_PAGE" }

13  SORT BY SOMETHING NOT ON THE ALLOW-LIST          -> 400 Bad Request
    ?sort=encryptionKeyReference,asc
    OUT: { "code": "INVALID_SORT_FIELD",
           "message": "... Allowed: name, schoolName, subdomain, status,
                       createdAt, updatedAt." }
    An allow-list, not a pass-through. Sorting by an arbitrary field means
    a collection scan per request, and the ORDER of a field can leak it —
    sorting by the key reference tells you which schools share a key
    without the value ever being returned.

    ?sort=name,sideways -> 400 INVALID_SORT_DIRECTION

14  A MISSPELLED STATUS                              -> 400 Bad Request
    ?status=NOPE
    OUT: { "code": "INVALID_PARAMETER",
           "message": "'NOPE' is not a valid value for 'status'.
                       Accepted values: TRIAL, PROVISIONING, ACTIVE, ..." }

15  REGEX INJECTION IS NOT POSSIBLE                       -> 200 OK
    ?search=.*
    OUT: totalElements: 0 — the term is escaped and matched literally.
    Unescaped, \`.*\` would return every school, and a nested-quantifier
    pattern could pin a database thread on very little input.

NEVER RETURNED: encryptionKeyReference is absent from every row, as it is
from every other response in this package.
\`\`\`
`,
      pathParams: [],
      queryParams: [
        { key: "page", value: "0", enabled: true },
        { key: "size", value: "20", enabled: true },
        { key: "sort", value: "createdAt,desc", enabled: true },
        { key: "status", value: "ACTIVE", enabled: false },
        { key: "search", value: "orbit", enabled: false },
        { key: "countryCode", value: "IN", enabled: false },
        { key: "city", value: "Pune", enabled: false },
        { key: "createdFrom", value: "2026-01-01T00:00:00Z", enabled: false },
        { key: "createdTo", value: "2026-12-31T23:59:59Z", enabled: false },
      ],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_PAGE_SIZE", when: "Size=0  or  size=5000" },
        { status: 400, code: "INVALID_PAGE", when: "Page=-1" },
        { status: 400, code: "INVALID_SORT_FIELD", when: "Sort by something not on the allow-list" },
        { status: 400, code: "INVALID_PARAMETER", when: "A misspelled status" },
      ],
      examples: [
        {
          id: "01",
          name: "BARE LIST",
          expect: "200 OK",
          notes: `GET /platform/schools
    Newest first, twenty rows. content + page, size,
    totalElements, totalPages, hasNext, hasPrevious.`,
          body: null,
        },
        {
          id: "02",
          name: "SEARCH, CASE-INSENSITIVE AND PARTIAL",
          expect: "200 OK",
          notes: `?search=ORBIT   matches "Orbit Astra International School"
    ?search=orbit-astra-17  matches by SUBDOMAIN too.
    Name and subdomain only — searching the address as well would make
    ?search=pune return every school in the city.`,
          body: null,
          queryParams: [{ key: "search", value: "ORBIT", enabled: true }],
        },
        {
          id: "03",
          name: "FILTER BY STATUS",
          expect: "200 OK",
          notes: `?status=ACTIVE`,
          body: null,
          queryParams: [{ key: "status", value: "ACTIVE", enabled: true }],
        },
        {
          id: "04",
          name: "SEVERAL STATUSES — repeat the parameter",
          expect: "200 OK",
          notes: `?status=ACTIVE&status=TRIAL
    OR within the field: "show me the live ones" is one question.`,
          body: null,
          queryParams: [{ key: "status", value: "ACTIVE", enabled: true }, { key: "status", value: "TRIAL", enabled: true }],
        },
        {
          id: "05",
          name: "FILTER BY COUNTRY AND CITY",
          expect: "200 OK",
          notes: `?countryCode=in&city=pune        both case-insensitive, both exact`,
          body: null,
          queryParams: [{ key: "countryCode", value: "in", enabled: true }, { key: "city", value: "pune", enabled: true }],
        },
        {
          id: "06",
          name: "FILTER BY CREATION DATE",
          expect: "200 OK",
          notes: `?createdFrom=2026-01-01T00:00:00Z&createdTo=2026-12-31T23:59:59Z`,
          body: null,
          queryParams: [{ key: "createdFrom", value: "2026-01-01T00:00:00Z", enabled: true }, { key: "createdTo", value: "2026-12-31T23:59:59Z", enabled: true }],
        },
        {
          id: "07",
          name: "SORT",
          expect: "200 OK",
          notes: `?sort=name,asc      ?sort=name,desc      ?sort=createdAt,desc
    sort=name is an alias for schoolName. Case-insensitive: CreatedAt works.
    EVERY SORT ENDS WITH id. Without a tiebreaker, paging over rows with
    equal sort keys can show one twice and miss another — a bug that only
    appears in production, only on page two.`,
          body: null,
          queryParams: [{ key: "sort", value: "name,asc", enabled: true }],
        },
        {
          id: "08",
          name: "PAGINATE",
          expect: "200 OK",
          notes: `?sort=name,asc&page=0&size=1   then   &page=1
    hasPrevious flips to true on page 1.`,
          body: null,
          queryParams: [{ key: "sort", value: "name,asc", enabled: true }, { key: "page", value: "0", enabled: true }, { key: "size", value: "1", enabled: true }],
        },
        {
          id: "09",
          name: "ALL OF IT AT ONCE",
          expect: "200 OK",
          notes: `?status=ACTIVE&search=orbit&countryCode=IN&page=0&size=20&sort=name,asc`,
          body: null,
          queryParams: [{ key: "status", value: "ACTIVE", enabled: true }, { key: "search", value: "orbit", enabled: true }, { key: "countryCode", value: "IN", enabled: true }, { key: "page", value: "0", enabled: true }, { key: "size", value: "20", enabled: true }, { key: "sort", value: "name,asc", enabled: true }],
        },
        {
          id: "10",
          name: "NO MATCHES",
          expect: "200 OK",
          notes: `?search=zzz-nothing
    OUT: content: [], totalElements: 0, totalPages: 0
    A 200 with an empty list, NOT a 404. "No school matches" is a
    successful answer to the question asked.`,
          body: null,
          queryParams: [{ key: "search", value: "zzz-nothing", enabled: true }],
        },
        {
          id: "11",
          name: "size=0  or  size=5000",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_PAGE_SIZE",
           "message": "size must be between 1 and 100. Received: 5000" }
    REFUSED, NOT CLAMPED. Quietly returning 100 rows for size=5000 looks
    like the whole result, which is how somebody comes to believe they
    have seen every school.`,
          body: null,
        },
        {
          id: "12",
          name: "page=-1",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_PAGE" }`,
          body: null,
        },
        {
          id: "13",
          name: "SORT BY SOMETHING NOT ON THE ALLOW-LIST",
          expect: "400 Bad Request",
          notes: `?sort=encryptionKeyReference,asc
    OUT: { "code": "INVALID_SORT_FIELD",
           "message": "... Allowed: name, schoolName, subdomain, status,
                       createdAt, updatedAt." }
    An allow-list, not a pass-through. Sorting by an arbitrary field means
    a collection scan per request, and the ORDER of a field can leak it —
    sorting by the key reference tells you which schools share a key
    without the value ever being returned.

    ?sort=name,sideways -> 400 INVALID_SORT_DIRECTION`,
          body: null,
          queryParams: [{ key: "sort", value: "encryptionKeyReference,asc", enabled: true }],
        },
        {
          id: "14",
          name: "A MISSPELLED STATUS",
          expect: "400 Bad Request",
          notes: `?status=NOPE
    OUT: { "code": "INVALID_PARAMETER",
           "message": "'NOPE' is not a valid value for 'status'.
                       Accepted values: TRIAL, PROVISIONING, ACTIVE, ..." }`,
          body: null,
          queryParams: [{ key: "status", value: "NOPE", enabled: true }],
        },
        {
          id: "15",
          name: "REGEX INJECTION IS NOT POSSIBLE",
          expect: "200 OK",
          notes: `?search=.*
    OUT: totalElements: 0 — the term is escaped and matched literally.
    Unescaped, \`.*\` would return every school, and a nested-quantifier
    pattern could pin a database thread on very little input.

NEVER RETURNED: encryptionKeyReference is absent from every row, as it is
from every other response in this package.`,
          body: null,
          queryParams: [{ key: "search", value: ".*", enabled: true }],
        },
      ],
    },
    {
      id: "get-school",
      name: "Get School",
      method: "GET",
      path: "/platform/schools/{id}",
      status: 'live',
      summary: "One school in full, including its lifecycle timestamps and the reason for its current status.",
      schoolSurface: false,
      docs: `**GET** \`/platform/schools/{id}\` — one school in full, for the operator.

The row picked out of List Schools, opened. Everything on the school plus the three lifecycle fields the school itself never sees: \`activatedAt\`, \`suspendedAt\` and \`statusReason\`.

\`statusReason\` is written **for the operator** — "Non-payment. Third invoice unpaid past 60 days." — and is not a message to show the school. It is on this endpoint and not on Get Profile for that reason.

A school at **any** status comes back, closed and deleted included: the console is exactly where somebody needs to look at a school that is no longer running and find out why.

\`encryptionKeyReference\` is never returned, on either surface.

### Cases

| # | id | Expected |
|---|---|---|
| 01 | \`{{schoolId}}\` | \`200\` |
| 02 | \`000000000000000000000000\` | \`404 SCHOOL_NOT_FOUND\` |
| 03 | \`not-an-objectid\` | \`404 SCHOOL_NOT_FOUND\` — not a 500 |
`,
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["schoolId", "schoolName", "accountHolderName", "subdomain", "logoUrl", "phoneNumber", "emailAddress", "defaultLocale", "defaultTimeZone", "addressLine", "city", "stateOrProvince", "postalCode", "countryCode", "status", "statusReason", "activatedAt", "suspendedAt", "createdAt", "updatedAt"],
      captures: [
        { variable: "schoolId", from: "schoolId" },
        { variable: "createdSubdomain", from: "subdomain" },
      ],
      errors: [],
      examples: [],
    },
  ],
};

export const API_CATALOG = [
  GROUP_CORE_ACADEMIC_YEAR,
  GROUP_CORE_SCHOOL_PROFILE,
  GROUP_CORE_SCHOOL_PLATFORM,
];

/** Flat list, handy for searching and for finding an endpoint by id from the history. */
export const ALL_ENDPOINTS = API_CATALOG.flatMap((group) =>
  group.endpoints.map((endpoint) => ({ ...endpoint, module: group.module })),
);

export function findEndpoint(id) {
  return ALL_ENDPOINTS.find((endpoint) => endpoint.id === id) || null;
}

export const LIVE_COUNT = ALL_ENDPOINTS.length;

/** How many worked examples the collection carries, shown in the sidebar footer. */
export const CASE_COUNT = ALL_ENDPOINTS.reduce(
  (total, endpoint) => total + endpoint.examples.length,
  0,
);
