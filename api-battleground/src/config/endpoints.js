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

### The rows go under a \`holidays\` key

Not a bare array, so a bad row is reported with the same \`fieldErrors\` shape as every other
endpoint. \`{ "holidays": [] }\` clears the calendar; \`{}\` is refused.

### Flat in, grouped out

You send **one row per reason**. The service groups rows by date, so two rows sharing a date
become one closed day with two reasons — a Sunday that is also Diwali. Sending the same
**type** twice for one date is refused; that is a duplicated row, not a second reason.

### Everything already there is discarded

Generated weekly offs included. That is what replace means, and it is why #21 exists.

### The seven test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain. Run Create School and Create Academic Year first.

 THE ROWS GO UNDER A "holidays" KEY, not as a bare array. Changed on
 2026-09-03: a bare array made Spring report a bad row as a method
 signature and an error count, instead of naming the field. As an object it
 validates like every other endpoint — [1].name must not be blank.

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
      body: `{
  "holidays": [
    { "name": "Weekly Off",       "type": "WEEKLY_OFF",     "date": "2026-11-08" },
    { "name": "Diwali",           "type": "FESTIVAL",       "date": "2026-11-08",
      "description": "Festival of lights" },
    { "name": "Independence Day", "type": "PUBLIC_HOLIDAY", "date": "2026-08-15" }
  ]
}`,
      successStatus: 200,
      responseFields: ["academicYearName", "startDate", "endDate", "closedDayCount", "eventCount", "countsByType", "holidays", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "DUPLICATE_HOLIDAY_ENTRY", when: "Same type twice on one date" },
        { status: 400, code: "HOLIDAY_OUTSIDE_YEAR", when: "A date outside the year" },
        { status: 400, code: "MALFORMED_REQUEST", when: "An unknown type" },
        { status: 400, code: "VALIDATION_FAILED", when: "A missing name or type" },
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
          body: `{
  "holidays": []
}`,
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
          body: `{
  "holidays": [
    { "name": "Weekly Off",       "type": "WEEKLY_OFF", "date": "2026-11-08" },
    { "name": "Weekly Off again", "type": "WEEKLY_OFF", "date": "2026-11-08" }
  ]
}`,
        },
        {
          id: "04",
          name: "A DATE OUTSIDE THE YEAR",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "HOLIDAY_OUTSIDE_YEAR" }
    A holiday belongs to the year that contains it; one outside would never
    be found by anything looking at that year.`,
          body: `{
  "holidays": [{ "name": "New Year", "type": "PUBLIC_HOLIDAY", "date": "2028-01-01" }]
}`,
        },
        {
          id: "05",
          name: "AN UNKNOWN TYPE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "MALFORMED_REQUEST" }
    There is no NATIONAL_HOLIDAY. Accepted: WEEKLY_OFF, PUBLIC_HOLIDAY,
    FESTIVAL, RELIGIOUS, SCHOOL_EVENT, VACATION, EXAM_BREAK, OTHER.`,
          body: `{
  "holidays": [{ "name": "Sports Day", "type": "NATIONAL_HOLIDAY", "date": "2026-12-01" }]
}`,
        },
        {
          id: "06",
          name: "A MISSING NAME OR TYPE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "VALIDATION_FAILED",
           "fieldErrors": { "holidays[1].name": ["must not be blank"] } }
    THE ROW IS NAMED. Against a spreadsheet of sixty rows, which one matters
    more than what. This only works because the body is an object — see the
    note at the top.

06b NO holidays KEY AT ALL                         -> 400 Bad Request
{
}
    OUT: fieldErrors: { "holidays": ["must not be null"] }
    REFUSED, not treated as "clear it". Wiping a year of closures should not
    be what happens when a field is forgotten.`,
          body: `{
  "holidays": [
    { "name": "Fine", "type": "FESTIVAL", "date": "2026-12-01" },
    { "type": "FESTIVAL", "date": "2026-12-02" }
  ]
}`,
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

const GROUP_PLANS_PLAN_CATALOGUE = {
  id: "plans-plan-catalogue",
  module: "Plans / Plan catalogue",
  endpoints: [
    {
      id: "create-plan-draft",
      name: "Create Plan Draft",
      method: "POST",
      path: "/platform/plans/drafts",
      status: 'live',
      summary: "Makes a plan as a DRAFT at version 1, not publicly available. Nobody can buy it yet.",
      schoolSurface: false,
      docs: `**POST** \`/platform/plans/drafts\` — makes a new plan, as a draft.

The platform's own price list: what a school pays **us** for Orbit Sphere. Not student fees —
\`models/finance\` is money a parent pays a school, and the two never meet.

### You do not send a plan code

It is derived from the name — "Premium Plus" becomes \`PREMIUM_PLUS\`. Send one explicitly only
when the derived code will not do.

The code exists because it is the **family key**: the only thing joining version 1, 2 and 3 of
one plan. An editable \`name\` cannot do that job, because a key that can change is not a key.

### It always makes a draft

\`status\` (always \`DRAFT\`), \`planVersion\` (always 1) and \`publiclyAvailable\` (always false) are
**not on the request**. A plan that could be created \`ACTIVE\` would be on sale before it was
priced. Later versions come from #5.

### Features are not accepted here

The plan starts with an empty feature list; #3 sets the whole list in one go — the same shape
academic years use for holidays.

### Normalized on the way in

\`planCode\` derived from the name, \`currencyCode\` uppercased, \`listPrice\` forced to exactly two
decimal places, blank text becoming null.

### Refused, not rounded or guessed

A price with three decimal places, a currency code that is not ISO 4217, a limit of zero, a
selling window that runs backwards, and a \`planCode\` that already exists.

### The fifteen test cases are in the request body as comments
`,
      bodyNotes: `Platform surface. No tenant header: a PlanDefinition has no schoolId.

 THIS IS THE PLATFORM'S OWN PRICE LIST, NOT SCHOOL FEES. Money a school pays
 us for Orbit Sphere. models/finance is the other thing entirely — money a
 parent pays a school — and nothing here may touch a FeeInvoice.

 YOU DO NOT SEND planCode. It is worked out from the name — "Premium Plus"
 becomes PREMIUM_PLUS — so a create form asks for one thing instead of
 making somebody type the same words twice in two shapes. Send one only when
 the derived code will not do (case 03).

 WHY THE CODE EXISTS AT ALL, given the name is right there: it is the FAMILY
 KEY, the only thing joining version 1, 2 and 3 of one plan. A subscription
 stores a document id and a version number, so without it "version 2" means
 version 2 of nothing, and #5 (copy a published plan into a new version) has
 no way to say which family the copy joins. The name cannot do that job,
 because a name is display text somebody will want to change — and a key
 that can change is not a key.

 IT ALWAYS MAKES A DRAFT. status, planVersion and publiclyAvailable are NOT
 on the request:
   status             always DRAFT   — nobody can buy it while we are still
                                       deciding the price
   planVersion        always 1       — later versions come from #5, which
                                       copies a published one
   publiclyAvailable  always false   — #7 decides if it shows publicly
 Send them anyway and they are ignored, not half-honoured (case 10).

 FEATURES ARE NOT ACCEPTED HERE. The plan starts with an empty feature list
 and #3 sets the whole list in one go — the same shape academic years use
 for holidays. A create that can fail on either a bad price or a bad feature
 leaves you working out which, and a half-filled feature list is the "plan
 nobody can price" that #3 exists to prevent.

 WHY /drafts IS IN THE PATH: so nobody reads POST /platform/plans and thinks
 they are putting a plan on sale. Every endpoint after this addresses the
 plan by code and version — /platform/plans/PREMIUM/versions/1 — because
 from then on draft-ness is a status on a plan that exists.`,
      requiredFields: ["name", "billingCycle", "listPrice", "currencyCode", "maxStudents", "maxUsers"],
      optionalFields: ["planCode", "description", "effectiveFrom", "effectiveUntil"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "Premium",
  "description": "Advanced ERP modules and AI capabilities for growing schools.",
  "billingCycle": "YEARLY",
  "listPrice": 49999,
  "currencyCode": "INR",
  "maxStudents": 2000,
  "maxUsers": 250
}`,
      successStatus: 201,
      successNote: "Also sends a Location header: /platform/plans/{planCode}/versions/1",
      responseFields: ["planId", "planCode", "planVersion", "name", "status", "billingCycle", "listPrice", "currencyCode", "maxStudents", "maxUsers", "publiclyAvailable", "featureCount", "sellable", "nextStep"],
      captures: [
        { variable: "planCode", from: "planCode" },
        { variable: "planVersion", from: "planVersion" },
      ],
      errors: [
        { status: 400, code: "PRICE_NEGATIVE", when: "A negative price" },
        { status: 400, code: "PRICE_TOO_PRECISE", when: "More than two decimal places" },
        { status: 400, code: "LIMIT_TOO_LOW", when: "A limit of zero" },
        { status: 400, code: "INVALID_SELLING_WINDOW", when: "A selling window that runs backwards" },
        { status: 400, code: "MALFORMED_REQUEST", when: "A billing cycle that does not exist" },
        { status: 400, code: "VALIDATION_FAILED", when: "Nothing at all" },
        { status: 409, code: "PLAN_CODE_TAKEN", when: "A name that derives a code somebody has" },
        { status: 409, code: "CURRENCY_INVALID", when: "A currency that does not exist" },
        { status: 409, code: "PLAN_CODE_INVALID", when: "An explicit code that is not a code" },
      ],
      examples: [
        {
          id: "01",
          name: "CREATE A DRAFT PLAN",
          expect: "201 Created",
          notes: `The body above.
    OUT: status: "DRAFT", planVersion: 1, publiclyAvailable: false,
         featureCount: 0, sellable: false
    Header: Location: /platform/plans/PREMIUM/versions/1

    sellable is DERIVED, never stored: published AND public AND inside the
    selling window. Three separate facts, so every screen does not combine
    them slightly differently.`,
          body: null,
        },
        {
          id: "02",
          name: "THE CODE COMES FROM THE NAME",
          expect: "201 Created",
          notes: `OUT: planCode "PREMIUM_PLUS"  <- derived; nothing was sent
         name      "Premium Plus" <- trimmed
         description null         <- "   " is nothing, so it is nothing
         listPrice 79999.00       <- always two decimal places
         currencyCode "INR"       <- uppercased

    Anything that is not a letter or digit becomes one underscore, and the
    ends are trimmed:
       "Premium Plus"     -> PREMIUM_PLUS
       "Starter (2026)"   -> STARTER_2026
       "Schools & Trusts" -> SCHOOLS_TRUSTS

02b AN EXPLICIT CODE STILL WINS                          -> 201 Created
{
  "name": "Anything",
  "planCode": "enterprise",
  "billingCycle": "YEARLY",
  "listPrice": 9,
  "currencyCode": "INR",
  "maxStudents": 10,
  "maxUsers": 10
}
    OUT: planCode "ENTERPRISE" — uppercased, hyphens become underscores.
    For when the derived code is taken, or has to match something outside
    this system.

02c A NAME THAT CANNOT PRODUCE A CODE               -> 409 Conflict
{
  "name": "★★★", "billingCycle": "YEARLY", "listPrice": 1,
  "currencyCode": "INR", "maxStudents": 1, "maxUsers": 1
}
    OUT: { "code": "PLAN_CODE_INVALID",
           "message": "No plan code could be worked out from the name '★★★'.
                       Send a planCode of letters, digits and inner
                       underscores." }
    The message names the NAME, not a code the caller never sent.`,
          body: `{
  "name": "   Premium Plus   ",
  "description": "   ",
  "billingCycle": "YEARLY",
  "listPrice": 79999,
  "currencyCode": "inr",
  "maxStudents": 5000,
  "maxUsers": 500
}`,
        },
        {
          id: "03",
          name: "A NAME THAT DERIVES A CODE SOMEBODY HAS",
          expect: "409 Conflict",
          notes: `Send case 01 twice. Two plans both called "Premium" derive the same
    PREMIUM code, and casing does not help — "premium" normalizes to it too.
    OUT: { "code": "PLAN_CODE_TAKEN",
           "message": "A plan called 'PREMIUM' already exists. To change its
                       price, make a new version of it instead of a new
                       plan." }

    Refused even though the unique index is on planCode AND planVersion, so
    a second PREMIUM v1 would technically fit. planCode is the plan's
    permanent identity and SchoolSubscription stores it — two plans sharing
    it could never be told apart, and "which PREMIUM" would have no answer.`,
          body: null,
        },
        {
          id: "04",
          name: "A FREE PLAN",
          expect: "201 Created",
          notes: `ZERO IS ALLOWED. A free tier is a real plan.`,
          body: `{
  "planCode": "FREE",
  "name": "Free",
  "billingCycle": "MONTHLY",
  "listPrice": 0,
  "currencyCode": "INR",
  "maxStudents": 50,
  "maxUsers": 5
}`,
        },
        {
          id: "05",
          name: "A NEGATIVE PRICE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "PRICE_NEGATIVE" }
    A plan we pay the school to be on is not a thing.`,
          body: `{
  "planCode": "ODD", "name": "Odd", "billingCycle": "MONTHLY",
  "listPrice": -5, "currencyCode": "INR", "maxStudents": 10, "maxUsers": 5
}`,
        },
        {
          id: "06",
          name: "MORE THAN TWO DECIMAL PLACES",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "PRICE_TOO_PRECISE" }
    REFUSED, NOT ROUNDED. Rounding somebody's price for them is how 1999.999
    quietly becomes 2000.00 on every invoice for a year.`,
          body: `{
  "planCode": "ODD2", "name": "Odd", "billingCycle": "MONTHLY",
  "listPrice": 1999.999, "currencyCode": "INR", "maxStudents": 10,
  "maxUsers": 5
}`,
        },
        {
          id: "07",
          name: "A CURRENCY THAT DOES NOT EXIST",
          expect: "409 Conflict",
          notes: `OUT: { "code": "CURRENCY_INVALID",
           "message": "'RUP' is not an ISO 4217 currency code. Example:
                       INR." }
    Checked against the JDK's ISO 4217 list, not a hand-written one — for
    the same reason time zones are. RUP and INS look plausible and do not
    exist, and nobody notices until an invoice is issued in one.

    409 rather than 400: the request is well formed and still refused.`,
          body: `{
  "planCode": "ODD3", "name": "Odd", "billingCycle": "MONTHLY",
  "listPrice": 1, "currencyCode": "RUP", "maxStudents": 10, "maxUsers": 5
}`,
        },
        {
          id: "08",
          name: "A LIMIT OF ZERO",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "LIMIT_TOO_LOW" }
    A plan capped at zero students blocks the first thing the school tries
    to do, which reads as a broken platform rather than as the plan it was
    sold.`,
          body: `{
  "planCode": "ODD4", "name": "Odd", "billingCycle": "MONTHLY",
  "listPrice": 1, "currencyCode": "INR", "maxStudents": 0, "maxUsers": 5
}`,
        },
        {
          id: "09",
          name: "A SELLING WINDOW THAT RUNS BACKWARDS",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_SELLING_WINDOW" }
    Both dates are OPTIONAL, and a draft usually has neither — #4 stamps
    effectiveFrom when it publishes. Only the pair together can be wrong.`,
          body: `{
  "planCode": "ODD5", "name": "Odd", "billingCycle": "YEARLY",
  "listPrice": 1, "currencyCode": "INR", "maxStudents": 10, "maxUsers": 5,
  "effectiveFrom": "2027-04-01T00:00:00Z",
  "effectiveUntil": "2026-04-01T00:00:00Z"
}`,
        },
        {
          id: "10",
          name: "FIELDS THAT ARE NOT OURS TO SET",
          expect: "201 Created",
          notes: `OUT: status "DRAFT", planVersion 1, publiclyAvailable false,
         featureCount 0 — every one of those four was ignored.
    They are not on the request record, so Jackson drops them. A plan that
    could be created ACTIVE would be on sale before it was priced.`,
          body: `{
  "planCode": "IGNORED",
  "name": "Try to cheat",
  "billingCycle": "YEARLY",
  "listPrice": 1,
  "currencyCode": "INR",
  "maxStudents": 10,
  "maxUsers": 5,
  "status": "ACTIVE",
  "planVersion": 9,
  "publiclyAvailable": true,
  "features": [{ "featureCode": "EVERYTHING" }]
}`,
        },
        {
          id: "11",
          name: "AN EXPLICIT CODE THAT IS NOT A CODE",
          expect: "409 Conflict",
          notes: `OUT: { "code": "PLAN_CODE_INVALID" }
    Letters, digits and INNER underscores. No leading or trailing one — and
    a derived code never has one, because the ends are trimmed.`,
          body: `{
  "planCode": "_bad_", "name": "X", "billingCycle": "MONTHLY",
  "listPrice": 1, "currencyCode": "INR", "maxStudents": 10, "maxUsers": 5
}`,
        },
        {
          id: "12",
          name: "A BILLING CYCLE THAT DOES NOT EXIST",
          expect: "400 Bad Request",
          notes: `"billingCycle": "WEEKLY"
    OUT: { "code": "MALFORMED_REQUEST" }
    Accepted: MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, CUSTOM.`,
          body: null,
        },
        {
          id: "13",
          name: "NOTHING AT ALL",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "VALIDATION_FAILED" } with fieldErrors naming name,
    billingCycle, listPrice, currencyCode, maxStudents, maxUsers.
    NOT planCode — it is optional, and with no name there is nothing to
    derive it from either.`,
          body: `{
}`,
        },
      ],
    },
    {
      id: "update-plan-draft",
      name: "Update Plan Draft",
      method: "PATCH",
      path: "/platform/plans/{code}/versions/{version}",
      status: 'live',
      summary: "Edits a draft — name, price, limits, selling window. Refused once the plan is published.",
      schoolSurface: false,
      docs: `**PATCH** \`/platform/plans/{code}/versions/{version}\` — fixes the details of a draft.

### Only a draft can be edited

Once a plan is published a school can be on it, and changing the price then would change what
they agreed to pay without anybody agreeing to it. Editing an \`ACTIVE\` or \`RETIRED\` plan is a
\`409\`; #5 copies it into a new draft version instead.

### Partial, with the project's PATCH convention

Omitted or null leaves a field alone, \`""\` clears the description, a value replaces. \`name\`
cannot be cleared.

### The selling window is replaced as a pair

The two dates are only meaningful next to each other, so they are nested. Omit \`sellingWindow\` to
leave it alone; send it with nulls inside to clear it.

### Not on this request

\`planCode\`, \`planVersion\`, \`status\`, \`publiclyAvailable\`, \`features\` — each has its own endpoint
or is the plan's identity.

### The thirteen test cases are in the request body as comments
`,
      bodyNotes: `Platform surface. Run Create Plan Draft first — it saves {{planCode}}.

 ONLY A DRAFT CAN BE EDITED. That is the rule the whole catalogue is built
 on. The moment a plan is published a school can be on it, and changing the
 price then would change what they agreed to pay — retroactively, with no
 record that it happened. #5 copies a published version into a new draft
 instead, and the schools on the old version stay where they are.

 PARTIAL, the same way core's PATCHes are:
    omitted or null -> leave it exactly as it is
    ""              -> clear it (description only)
    a value         -> replace it
 name cannot be cleared: "" is a 400, not a deletion.

 THE SELLING WINDOW IS REPLACED AS A PAIR, not as two loose fields. The two
 dates are only meaningful next to each other — an effectiveUntil moved
 earlier than the existing effectiveFrom is a plan that can never be sold —
 so changing one alone could create a window nobody asked for. Same
 reasoning that puts the school's address behind a PUT.

 NOT ON THIS REQUEST: planCode and planVersion (they are the identity, in
 the URL), status (publish is #4, retire is #6), publiclyAvailable (#7) and
 features (#3). Each is a decision with its own rules; a PATCH that could
 set them all would make "put this on sale" look like "fix a typo".`,
      optionalFields: ["name", "description", "billingCycle", "listPrice", "currencyCode", "maxStudents", "maxUsers", "sellingWindow"],
      pathParams: [
        { name: "code", value: "{{planCode}}", description: "The plan's permanent family code. Create Plan Draft fills this in." },
        { name: "version", value: "{{planVersion}}", description: "Which version of that plan. Versions are immutable once published." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "Premium Plus",
  "listPrice": 44999,
  "maxStudents": 3000
}`,
      successStatus: 200,
      responseFields: ["planCode", "planVersion", "name", "status", "listPrice", "currencyCode", "maxStudents", "maxUsers", "featureCount", "sellable", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "An empty body" },
        { status: 400, code: "PLAN_NAME_REQUIRED", when: "Try to clear the name" },
        { status: 400, code: "—", when: "A bad price, currency or limit" },
        { status: 400, code: "INVALID_SELLING_WINDOW", when: "A window that runs backwards" },
        { status: 404, code: "PLAN_NOT_FOUND", when: "A plan or version that does not exist" },
        { status: 409, code: "PLAN_NOT_EDITABLE", when: "Edit a published plan" },
      ],
      examples: [
        {
          id: "01",
          name: "CHANGE A FEW FIELDS",
          expect: "200 OK",
          notes: `The body above. Everything not mentioned is untouched.
    OUT: status still "DRAFT", planVersion still 1.`,
          body: null,
        },
        {
          id: "02",
          name: "CHANGE ONE FIELD ONLY",
          expect: "200 OK",
          notes: `Trimmed on the way in. Price, currency, cycle and limits unchanged.`,
          body: `{
  "name": "   Renamed   "
}`,
        },
        {
          id: "03",
          name: "CLEAR THE DESCRIPTION",
          expect: "200 OK",
          notes: `OUT: description null. Omitting it instead would have left it alone.`,
          body: `{
  "description": ""
}`,
        },
        {
          id: "04",
          name: "REPLACE THE SELLING WINDOW",
          expect: "200 OK",
          notes: ``,
          body: `{
  "sellingWindow": {
    "effectiveFrom": "2026-06-01T00:00:00Z",
    "effectiveUntil": "2027-05-31T00:00:00Z"
  }
}`,
        },
        {
          id: "05",
          name: "CLEAR THE SELLING WINDOW",
          expect: "200 OK",
          notes: `Nulls INSIDE the pair mean "no date". This is the only way to clear a
    date here — "" cannot mean anything to an instant.
    OMITTING sellingWindow leaves the window alone. The two are different.`,
          body: `{
  "sellingWindow": { "effectiveFrom": null, "effectiveUntil": null }
}`,
        },
        {
          id: "06",
          name: "EDIT A PUBLISHED PLAN",
          expect: "409 Conflict",
          notes: `Publish it with #4 first, then send anything.
    OUT: { "code": "PLAN_NOT_EDITABLE",
           "message": "'PREMIUM' version 1 is ACTIVE and cannot be edited.
                       Schools may already be on it. Make a new version of
                       it instead." }
    A RETIRED plan is refused the same way — schools may still be on it.
    THIS IS THE POINT OF THE ENDPOINT. Nothing is changed by a refusal.`,
          body: null,
        },
        {
          id: "07",
          name: "AN EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" }
    Checked BEFORE the plan is looked up, so an empty PATCH on a plan that
    does not exist says the body is empty rather than sending you hunting
    for a missing plan.`,
          body: `{
}`,
        },
        {
          id: "08",
          name: "TRY TO CLEAR THE NAME",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "PLAN_NAME_REQUIRED" }`,
          body: `{
  "name": "   "
}`,
        },
        {
          id: "09",
          name: "A BAD PRICE, CURRENCY OR LIMIT",
          expect: "400 / 409",
          notes: `{ "listPrice": -1 }      -> 400 PRICE_NEGATIVE
    { "listPrice": 9.999 }   -> 400 PRICE_TOO_PRECISE
    { "currencyCode": "RUP" }-> 409 CURRENCY_INVALID
    { "maxUsers": 0 }        -> 400 LIMIT_TOO_LOW
    The same checks #1 makes, from the same validator — one set of rules,
    not two that drift.`,
          body: null,
        },
        {
          id: "10",
          name: "A WINDOW THAT RUNS BACKWARDS",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_SELLING_WINDOW" }`,
          body: `{
  "sellingWindow": {
    "effectiveFrom": "2027-01-01T00:00:00Z",
    "effectiveUntil": "2026-01-01T00:00:00Z"
  }
}`,
        },
        {
          id: "11",
          name: "A PLAN OR VERSION THAT DOES NOT EXIST",
          expect: "404 Not Found",
          notes: `Change the URL to /platform/plans/NOPE/versions/1, or ask for version 9.
    OUT: { "code": "PLAN_NOT_FOUND",
           "message": "No plan 'NOPE' version 1 exists." }`,
          body: null,
        },
        {
          id: "12",
          name: "A LOWERCASE CODE IN THE URL",
          expect: "200 OK",
          notes: `/platform/plans/premium_plus/versions/1 finds PREMIUM_PLUS.
    A code arrives in a URL where a person may have typed it, so it is
    normalized the same way it was when the plan was created. A code of the
    wrong shape simply matches nothing — that is a 404, not a complaint
    about its shape.`,
          body: null,
        },
        {
          id: "13",
          name: "FIELDS THAT ARE NOT OURS TO SET",
          expect: "200 OK",
          notes: `OUT: planCode, planVersion, status and publiclyAvailable all unchanged.
    They are not on the request record, so Jackson drops them.`,
          body: `{
  "name": "Fine",
  "planCode": "HACKED",
  "planVersion": 9,
  "status": "ACTIVE",
  "publiclyAvailable": true
}`,
        },
      ],
    },
    {
      id: "set-plan-features",
      name: "Set Plan Features",
      method: "PUT",
      path: "/platform/plans/{code}/versions/{version}/features",
      status: 'live',
      summary: "Replaces the whole feature list of a draft. featureCode is one of 24 fixed values.",
      schoolSurface: false,
      docs: `**PUT** \`/platform/plans/{code}/versions/{version}/features\` — sets the whole feature list of a
draft.

### \`featureCode\` is a fixed list of 24, not free text

A feature code points at behaviour in our code, so the set is closed. A misspelling is a \`400\`
naming the row and listing every accepted value — it used to be a \`String\`, which accepted
\`STUDNET_MANAGEMENT\` and silently locked the school out of what they paid for.

### You do not send \`usageMetric\`

Each feature declares what it is measured in — \`TRANSPORT\` in \`VEHICLES\`, \`STUDENT_MANAGEMENT\` in
\`ACTIVE_STUDENTS\` — so the metric is copied from the feature. It is **stored**, not looked up on
read, because a published plan must keep meaning what it meant when it was sold.

Features with nothing to count (\`ATTENDANCE\`, \`EXAMINATIONS\`, …) refuse a \`usageLimit\` outright.

### The whole list, not one feature at a time

A feature list is priced as a set. Send \`{ "features": [] }\` to empty it; there is no separate
delete.

### Only a draft

Features are what a school is buying — \`409\` on a published plan, same as #2.

### The twelve test cases are in the request body as comments
`,
      bodyNotes: `Platform surface. Run Create Plan Draft first — it saves {{planCode}}.

 featureCode IS A FIXED LIST, not free text. Changed on 2026-09-03: it was a
 String, which accepted "STUDNET_MANAGEMENT" with a 200 — the plan looked
 perfect on every screen while the entitlement service, asking for
 STUDENT_MANAGEMENT, found nothing and locked the school out of what they
 had paid for. One transposed letter, discovered when they rang up.

 A feature code points at behaviour in our code, not at anything a user
 invents, so the set is closed. An unknown value is now a 400 that lists
 every accepted one (case 05).

 THE 24 FEATURES:
   Teaching   STUDENT_MANAGEMENT · ACADEMICS · ATTENDANCE · TIMETABLE
              EXAMINATIONS · HOMEWORK
   Money      FEE_MANAGEMENT · PAYROLL
   People     STAFF_MANAGEMENT · ADMISSIONS_CRM
   Daily      TRANSPORT · LIBRARY · HOSTEL · MESS · HEALTH · FRONT_OFFICE
   Premises   INVENTORY · PROCUREMENT · FACILITIES
   Comms      NOTIFICATIONS · DOCUMENTS · GALLERY · FEEDBACK · STUDENT_LIFE

 YOU DO NOT SEND usageMetric. Each feature declares what it is measured in,
 so the metric is copied from the feature. TRANSPORT is counted in VEHICLES,
 STUDENT_MANAGEMENT in ACTIVE_STUDENTS. "Student management limited to 2000
 gigabytes" is not refused — it cannot be written down (case 08).

 SOME FEATURES HAVE NOTHING TO COUNT. ATTENDANCE is included or it is not,
 so a usageLimit on it is refused (case 06). Every response says which
 metric applies, or null.

 THE WHOLE LIST, NOT ONE FEATURE AT A TIME. A feature list is priced as a
 set — "2000 students and examinations for this much" is one offer — and
 there is no moment at which half of it is a plan.

 ONLY A DRAFT. Features are what a school is buying; changing them on a
 published plan changes what somebody already bought.

 DEFAULTS: enabled true, overagePolicy BLOCK.`,
      requiredFields: ["features"],
      pathParams: [
        { name: "code", value: "{{planCode}}", description: "The plan's permanent family code. Create Plan Draft fills this in." },
        { name: "version", value: "{{planVersion}}", description: "Which version of that plan. Versions are immutable once published." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "features": [
    { "featureCode": "STUDENT_MANAGEMENT", "usageLimit": 2000, "overagePolicy": "WARN" },
    { "featureCode": "ATTENDANCE" },
    { "featureCode": "EXAMINATIONS" },
    { "featureCode": "TRANSPORT", "usageLimit": 12 },
    { "featureCode": "HOSTEL", "enabled": false }
  ]
}`,
      successStatus: 200,
      responseFields: ["planCode", "planVersion", "status", "featureCount", "features", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "DUPLICATE_FEATURE", when: "The same feature twice" },
        { status: 400, code: "INVALID_VALUE", when: "A misspelled feature" },
        { status: 400, code: "FEATURE_NOT_MEASURABLE", when: "A limit on something with nothing to count" },
        { status: 400, code: "FEATURE_LIMIT_ZERO", when: "Enabled with a limit of zero" },
        { status: 400, code: "—", when: "No featurecode at all" },
        { status: 404, code: "PLAN_NOT_FOUND", when: "A plan that does not exist" },
        { status: 409, code: "PLAN_NOT_EDITABLE", when: "On a published plan" },
      ],
      examples: [
        {
          id: "01",
          name: "SET FIVE FEATURES",
          expect: "200 OK",
          notes: `The body above.
    OUT: featureCount 5, changeSummary "0 out, 5 in", and every row carries
         its label and description from the enum:
           STUDENT_MANAGEMENT "Student management"
             limit 2000 ACTIVE_STUDENTS, policy WARN
           ATTENDANCE         "Attendance"        limit null, metric null
           TRANSPORT          "Transport"         limit 12 VEHICLES
           HOSTEL             "Hostel"            enabled false

    label and description come from FeatureCode, the only place they are
    written — so the pricing page, the comparison table and the "your plan
    does not include this" message all say the same words.`,
          body: null,
        },
        {
          id: "02",
          name: "REPLACE WITH A SHORTER LIST",
          expect: "200 OK",
          notes: `The four not listed are gone. That is what replace means.`,
          body: `{
  "features": [
    { "featureCode": "STUDENT_MANAGEMENT", "usageLimit": 500 }
  ]
}`,
        },
        {
          id: "03",
          name: "EMPTY THE LIST",
          expect: "200 OK",
          notes: `The honest way to clear it, and why there is no separate delete.
    A body of {} is a 400 — features is required, and forgetting a field
    should not wipe a plan's entitlements.`,
          body: `{
  "features": []
}`,
        },
        {
          id: "04",
          name: "THE SAME FEATURE TWICE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "DUPLICATE_FEATURE" }
    Two rows for one feature is not a bigger entitlement, it is a question:
    which of the two limits applies?`,
          body: `{
  "features": [{ "featureCode": "LIBRARY" }, { "featureCode": "LIBRARY" }]
}`,
        },
        {
          id: "05",
          name: "A MISSPELLED FEATURE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_VALUE",
           "message": "'STUDNET_MANAGEMENT' is not a valid value for
                       'features[1].featureCode'. Accepted values:
                       STUDENT_MANAGEMENT, ACADEMICS, ATTENDANCE, ..." }
    THE ROW IS NAMED AND THE OPTIONS ARE LISTED. This is the whole reason
    featureCode stopped being a String. Nothing is written.`,
          body: `{
  "features": [
    { "featureCode": "STUDENT_MANAGEMENT" },
    { "featureCode": "STUDNET_MANAGEMENT" }
  ]
}`,
        },
        {
          id: "06",
          name: "A LIMIT ON SOMETHING WITH NOTHING TO COUNT",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "FEATURE_NOT_MEASURABLE",
           "message": "'ATTENDANCE' has no limit to set — it is either
                       included or it is not. Drop usageLimit, or use
                       \\"enabled\\": false to exclude it." }
    A plan that reads as capped and behaves as unlimited is worse than one
    with no cap at all.

    MEASURABLE:   STUDENT_MANAGEMENT (ACTIVE_STUDENTS) · PAYROLL and
    STAFF_MANAGEMENT (ACTIVE_STAFF) · TRANSPORT (VEHICLES) · LIBRARY
    (LIBRARY_TITLES) · HOSTEL (HOSTEL_BEDS) · NOTIFICATIONS (SMS_MESSAGES) ·
    DOCUMENTS and GALLERY (STORAGE_MEGABYTES)
    NOT MEASURABLE: everything else — included or not.`,
          body: `{
  "features": [{ "featureCode": "ATTENDANCE", "usageLimit": 500 }]
}`,
        },
        {
          id: "07",
          name: "ENABLED WITH A LIMIT OF ZERO",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "FEATURE_LIMIT_ZERO" }
    "Included, but you may use none of it" is the same outcome as switching
    it off, by a route that leaves it listed as available. Send
    "enabled": false — which IS allowed with a limit of 0.
    A negative limit is FEATURE_LIMIT_NEGATIVE.`,
          body: `{
  "features": [{ "featureCode": "TRANSPORT", "usageLimit": 0 }]
}`,
        },
        {
          id: "08",
          name: "TRY TO CHOOSE THE METRIC",
          expect: "200 OK",
          notes: `OUT: usageMetric "ACTIVE_STUDENTS" — the field is not on the request, so
    it is ignored and the feature's own metric is used.

    IT IS STORED, NOT LOOKED UP ON READ. A plan version is immutable once
    published: if TRANSPORT were ever changed from VEHICLES to ROUTES, a
    plan sold last year must keep meaning 12 vehicles.`,
          body: `{
  "features": [
    { "featureCode": "STUDENT_MANAGEMENT", "usageLimit": 2000,
      "usageMetric": "STORAGE_MEGABYTES" }
  ]
}`,
        },
        {
          id: "09",
          name: "A BAD OVERAGE POLICY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_VALUE",
           "message": "'SHRUG' is not a valid value for
                       'features[0].overagePolicy'. Accepted values: BLOCK,
                       WARN, ALLOW, CHARGE." }`,
          body: `{
  "features": [{ "featureCode": "LIBRARY", "overagePolicy": "SHRUG" }]
}`,
        },
        {
          id: "10",
          name: "NO featureCode AT ALL",
          expect: "400 Bad Request",
          notes: `OUT: fieldErrors: { "features[0].featureCode": ["must not be null"] }`,
          body: `{
  "features": [{ "usageLimit": 5 }]
}`,
        },
        {
          id: "11",
          name: "ON A PUBLISHED PLAN",
          expect: "409 Conflict",
          notes: `Publish it with #4 first.
    OUT: { "code": "PLAN_NOT_EDITABLE",
           "message": "... is ACTIVE and its features cannot be changed." }
    Not reachable from Postman yet — #4 is not built.`,
          body: null,
        },
        {
          id: "12",
          name: "A PLAN THAT DOES NOT EXIST",
          expect: "404 Not Found",
          notes: `OUT: { "code": "PLAN_NOT_FOUND" }`,
          body: null,
        },
      ],
    },
    {
      id: "publish-plan",
      name: "Publish Plan",
      method: "POST",
      path: "/platform/plans/{code}/versions/{version}/publish",
      status: 'live',
      summary: "Turns a draft into a plan schools can buy. One-way: it can never be edited again.",
      schoolSurface: false,
      docs: `**POST** \`/platform/plans/{code}/versions/{version}/publish\` — turns a draft into a plan schools
can buy.

### A one-way door

From here the version can never be edited: #2 and #3 both refuse anything that is not a draft,
and there is no unpublish. To change the price, make a new version with #5 — the schools on this
one keep what they bought.

### Checked, not trusted

Because it cannot be undone, two things are refused here rather than discovered by a school: a
plan with **no features** (they would pay and get nothing) and a plan whose **selling window has
already closed** (it could never be bought).

### Publishing is not the same as listing publicly

That is #7. Straight after publishing, \`publiclyAvailable\` is still false and \`sellable\` is still
false — the plan is real and can be offered privately in a quote.

### \`effectiveFrom\`

Filled with now if it was empty. A future date chosen while the plan was a draft is kept, so a
scheduled launch still works.

### The eight test cases are in the request body as comments
`,
      bodyNotes: `Platform surface. No body needed; anything sent is ignored.

 THIS IS A ONE-WAY DOOR. From here the version can NEVER be edited again:
 #2 (details) and #3 (features) both refuse anything that is not a DRAFT,
 and there is no unpublish. A school can be on it from the moment it goes
 live, and changing what they bought after they bought it is the thing this
 whole group is arranged to prevent.

 To change the price afterwards: #5, a new version. The schools on this
 version stay exactly where they are.

 BECAUSE IT CANNOT BE UNDONE, IT IS CHECKED RATHER THAN TRUSTED. Two things
 are refused here rather than discovered by a school:
   - a plan with no features would take their money and grant nothing
   - a plan whose selling window has already closed could never be bought

 PUBLISHING DOES NOT PUT IT ON THE PUBLIC LIST. That is #7. A published plan
 is real and can be offered privately in a quote; whether it shows on the
 pricing page is a separate decision, so it is a separate endpoint. Expect
 sellable: false straight after publishing.`,
      pathParams: [
        { name: "code", value: "{{planCode}}", description: "The plan's permanent family code. Create Plan Draft fills this in." },
        { name: "version", value: "{{planVersion}}", description: "Which version of that plan. Versions are immutable once published." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
}`,
      successStatus: 200,
      responseFields: ["planCode", "planVersion", "status", "effectiveFrom", "publiclyAvailable", "sellable", "nextStep"],
      captures: [],
      errors: [
        { status: 404, code: "PLAN_NOT_FOUND", when: "A plan or version that does not exist" },
        { status: 409, code: "PLAN_HAS_NO_FEATURES", when: "A draft with no features" },
        { status: 409, code: "PLAN_ALREADY_PUBLISHED", when: "Publish it again" },
        { status: 409, code: "PLAN_WINDOW_ALREADY_CLOSED", when: "A selling window that has already closed" },
        { status: 409, code: "—", when: "Afterwards, it is frozen" },
        { status: 409, code: "PLAN_NOT_EDITABLE", when: "A retired plan" },
      ],
      examples: [
        {
          id: "01",
          name: "PUBLISH A COMPLETE DRAFT",
          expect: "200 OK",
          notes: `Run Create Plan Draft, then Set Plan Features, then this.
    OUT: status "ACTIVE", effectiveFrom stamped with now,
         publiclyAvailable false, sellable false
         nextStep: "Published, and now permanent: this version can never be
                    edited again. It is NOT on the public list yet ..."`,
          body: null,
        },
        {
          id: "02",
          name: "A DRAFT WITH NO FEATURES",
          expect: "409 Conflict",
          notes: `Create a draft and publish it without running Set Plan Features.
    OUT: { "code": "PLAN_HAS_NO_FEATURES",
           "message": "... has no features, so a school buying it would get
                       nothing. Set its features first." }`,
          body: null,
        },
        {
          id: "03",
          name: "PUBLISH IT AGAIN",
          expect: "409 Conflict",
          notes: `OUT: { "code": "PLAN_ALREADY_PUBLISHED",
           "message": "... is already published. To change it, make a new
                       version." }
    NOT an idempotent 200. "It was already published" and "you just
    published it" are different facts, and a caller who cannot tell them
    apart will assume the wrong one — on the one action that cannot be
    undone.`,
          body: null,
        },
        {
          id: "04",
          name: "A SELLING WINDOW THAT HAS ALREADY CLOSED",
          expect: "409 Conflict",
          notes: `On a draft, set the window in the past with #2:
      { "sellingWindow": { "effectiveFrom": "2020-01-01T00:00:00Z",
                           "effectiveUntil": "2021-01-01T00:00:00Z" } }
    then publish.
    OUT: { "code": "PLAN_WINDOW_ALREADY_CLOSED",
           "message": "... stops being sold on 2021-01-01T00:00:00Z, which
                       has passed." }`,
          body: null,
        },
        {
          id: "05",
          name: "A LAUNCH DATE IN THE FUTURE",
          expect: "200 OK",
          notes: `On a draft, set effectiveFrom to a future date with #2, then publish.
    OUT: status "ACTIVE", effectiveFrom UNCHANGED — the date chosen while it
         was a draft still stands; publishing only fills an empty one.
         sellable false, because the window has not opened.
         nextStep says "It goes on sale on 2027-04-01T00:00:00Z."`,
          body: null,
        },
        {
          id: "06",
          name: "AFTERWARDS, IT IS FROZEN",
          expect: "409 Conflict",
          notes: `PATCH the details:  409 PLAN_NOT_EDITABLE
      "... is ACTIVE and cannot be edited."
    PUT the features:   409 PLAN_NOT_EDITABLE
      "... is ACTIVE and its features cannot be changed."
    THIS IS THE POINT OF THE ENDPOINT. Check both after case 01.`,
          body: null,
        },
        {
          id: "07",
          name: "A RETIRED PLAN",
          expect: "409 Conflict",
          notes: `Retire it with #6 first (not built yet).
    OUT: { "code": "PLAN_NOT_EDITABLE" } — "... is RETIRED and cannot be
    published." Retiring is not a way back to draft.`,
          body: null,
        },
        {
          id: "08",
          name: "A PLAN OR VERSION THAT DOES NOT EXIST",
          expect: "404 Not Found",
          notes: `OUT: { "code": "PLAN_NOT_FOUND" }`,
          body: null,
        },
      ],
    },
    {
      id: "set-plan-availability",
      name: "Set Plan Availability",
      method: "PATCH",
      path: "/platform/plans/{code}/versions/{version}/availability",
      status: 'live',
      summary: "Public list, or private quote only. The last of the three things that make a plan sellable.",
      schoolSurface: false,
      docs: `**PATCH** \`/platform/plans/{code}/versions/{version}/availability\` — public list, or private
quote only.

The difference between a plan a school can find and pick for itself, and one that only exists in
a quote you send them. A bespoke price for one large trust is published, sellable and
deliberately off the pricing page.

### On its own it makes nothing buyable

\`sellable\` is three facts: \`ACTIVE\` (#4), public (this), and inside the selling window. Every
response says which of the other two is still missing, so a public plan that is not on sale
explains itself.

### Idempotent, unlike #4 and #6

Those are one-way doors. This is a switch that flips back, so setting it to what it already is
comes back \`200\` saying so.

### A retired plan cannot be listed

\`409\` — advertising it would put something on the pricing page that every purchase would refuse.
Taking a retired plan **off** the list is allowed; that direction is only tidying up.

### The ten test cases are in the request body as comments
`,
      bodyNotes: `Platform surface.

 PUBLIC LIST, OR PRIVATE QUOTE. The difference between a plan a school can
 find and pick for itself, and one that only exists in a quote somebody
 sends them. A bespoke price for one large trust is a real plan —
 published, sellable, and deliberately not on the pricing page.

 ON ITS OWN IT MAKES NOTHING BUYABLE. A plan is sellable when THREE things
 are true:
     status is ACTIVE          (#4 publish)
     publiclyAvailable is true (this endpoint)
     today is inside the selling window
 This endpoint owns one of them. Every response says which of the other two
 is still missing, so a public plan that is not on sale explains itself.

 IT IS IDEMPOTENT, unlike #4 and #6. Those are one-way doors, so "it was
 already done" is a fact the caller needs. This is a switch that can be
 flipped back in one call, so a repeat costs nothing and refusing it would
 only teach callers to read first and then race.

 publiclyAvailable IS REQUIRED and boxed. An omitted boolean would arrive as
 false — indistinguishable from deliberately hiding the plan — so a
 forgotten field would pull a plan off the pricing page and report success.`,
      requiredFields: ["publiclyAvailable"],
      pathParams: [
        { name: "code", value: "{{planCode}}", description: "The plan's permanent family code. Create Plan Draft fills this in." },
        { name: "version", value: "{{planVersion}}", description: "Which version of that plan. Versions are immutable once published." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "publiclyAvailable": true
}`,
      successStatus: 200,
      responseFields: ["planCode", "planVersion", "status", "publiclyAvailable", "sellable", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "—", when: "An empty body" },
        { status: 400, code: "MALFORMED_REQUEST", when: "Not a boolean" },
        { status: 404, code: "PLAN_NOT_FOUND", when: "A plan or version that does not exist" },
        { status: 409, code: "PLAN_RETIRED", when: "Listing a retired plan" },
      ],
      examples: [
        {
          id: "01",
          name: "PUT A PUBLISHED PLAN ON THE PUBLIC LIST",
          expect: "200 OK",
          notes: `Run Create Plan Draft, Set Plan Features, Publish Plan, then this.
    OUT: publiclyAvailable true, sellable TRUE
         nextStep: "Now on the public list. Schools can now pick it."
    THIS IS THE CALL THAT FINALLY MAKES A PLAN BUYABLE.`,
          body: null,
        },
        {
          id: "02",
          name: "SEND IT AGAIN",
          expect: "200 OK",
          notes: `IDEMPOTENT. "It was already on the public list. Schools can now pick
    it."`,
          body: null,
        },
        {
          id: "03",
          name: "TAKE IT OFF THE LIST",
          expect: "200 OK",
          notes: `OUT: sellable false
         "Taken off the public list. It can still be offered privately in a
          quote. It is not sellable: a plan has to be on the public list to
          be picked."
    The plan is still ACTIVE and still real — just not advertised.`,
          body: `{
  "publiclyAvailable": false
}`,
        },
        {
          id: "04",
          name: "ON A DRAFT",
          expect: "200 OK",
          notes: `Set it on a draft before publishing. ALLOWED, so the decision can be
    made before the plan goes live.
    OUT: publiclyAvailable true, sellable FALSE
         "Now on the public list. It is NOT sellable yet — it is still a
          DRAFT. Publish it to put it on sale."`,
          body: null,
        },
        {
          id: "05",
          name: "ON A PLAN WITH A FUTURE LAUNCH DATE",
          expect: "200 OK",
          notes: `Set effectiveFrom to 2030 with #2, publish, then this.
    OUT: ACTIVE, public true, sellable FALSE
         "Now on the public list. It is not sellable yet: it goes on sale on
          2030-01-01T00:00:00Z."
    All three facts reported separately, so nothing looks broken.`,
          body: null,
        },
        {
          id: "06",
          name: "LISTING A RETIRED PLAN",
          expect: "409 Conflict",
          notes: `Retire it with #6 first, then send true.
    OUT: { "code": "PLAN_RETIRED",
           "message": "... is retired, so nobody can buy it. Listing it
                       publicly would advertise a plan every purchase would
                       refuse." }`,
          body: null,
        },
        {
          id: "07",
          name: "UNLISTING A RETIRED PLAN",
          expect: "200 OK",
          notes: `ALLOWED. Only the "on" direction is refused — taking a retired plan off
    the list is tidying up, and never wrong.
    OUT: "Taken off the public list. It is not sellable, and cannot become
          sellable: it is retired."
    Note it does NOT say "can still be offered privately" here: a retired
    plan cannot be sold at all.`,
          body: `{
  "publiclyAvailable": false
}`,
        },
        {
          id: "08",
          name: "AN EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: fieldErrors: { "publiclyAvailable": ["must not be null"] }
    The one field is required — there is no partial case for a PATCH whose
    only field is the thing being set.`,
          body: `{
}`,
        },
        {
          id: "09",
          name: "NOT A BOOLEAN",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "MALFORMED_REQUEST" }`,
          body: `{
  "publiclyAvailable": "yes please"
}`,
        },
        {
          id: "10",
          name: "A PLAN OR VERSION THAT DOES NOT EXIST",
          expect: "404 Not Found",
          notes: `OUT: { "code": "PLAN_NOT_FOUND" }`,
          body: null,
        },
      ],
    },
    {
      id: "retire-plan",
      name: "Retire Plan",
      method: "POST",
      path: "/platform/plans/{code}/versions/{version}/retire",
      status: 'live',
      summary: "Stops a plan being sold. Schools already on it keep it — their subscription does not change.",
      schoolSurface: false,
      docs: `**POST** \`/platform/plans/{code}/versions/{version}/retire\` — stops a plan being sold.

### It is about the catalogue, not about anybody's subscription

Schools already on the plan keep it, at the price and with the features they were sold. Nothing
about their subscription changes. Cancelling one school is #19, one school at a time.

That distinction matters: retiring a popular plan is a routine decision, and if it touched
subscriptions it would cut off every school on it at once.

### A draft can be retired too

It is the only way to withdraw one — no endpoint here deletes anything. The response says which
case it was. The \`planCode\` stays taken either way.

### Terminal

There is no un-retire, and #2, #3 and #4 all refuse a retired plan afterwards.

### \`effectiveUntil\`

Set to now, unless it is already in the past — then it is kept, because that is when the plan
actually stopped being sold.

### The seven test cases are in the request body as comments
`,
      bodyNotes: `Platform surface. No body needed.

 RETIRING IS ABOUT THE MENU, NOT ABOUT ANYBODY'S SUBSCRIPTION. The plan
 stops being something a school can pick. Schools ALREADY on it keep it —
 same price, same features — and nothing about their subscription changes.

 THAT DISTINCTION IS THE WHOLE POINT. Retiring a popular plan is a routine
 commercial decision. If it touched subscriptions it would cut off every
 school on it at once. Cancelling one school is #19, deliberately, one
 school at a time.

 A DRAFT CAN BE RETIRED TOO, and it is the only way to withdraw one: no
 endpoint in this module deletes anything. Nobody is on a draft, so it costs
 nothing. The response says which of the two happened — "withdrawn before it
 was ever sold" and "stopped being sold" are different facts.

 TERMINAL. There is no un-retire, and #2, #3 and #4 all refuse a retired
 plan afterwards.

 publiclyAvailable IS LEFT ALONE. It belongs to #7, and it makes no
 difference anyway: every list of buyable plans filters on ACTIVE first, so
 a retired plan is off the pricing page whatever that flag says.`,
      pathParams: [
        { name: "code", value: "{{planCode}}", description: "The plan's permanent family code. Create Plan Draft fills this in." },
        { name: "version", value: "{{planVersion}}", description: "Which version of that plan. Versions are immutable once published." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
}`,
      successStatus: 200,
      responseFields: ["planCode", "planVersion", "status", "effectiveUntil", "sellable", "nextStep"],
      captures: [],
      errors: [
        { status: 404, code: "PLAN_NOT_FOUND", when: "A plan or version that does not exist" },
        { status: 409, code: "PLAN_ALREADY_RETIRED", when: "Retire it again" },
        { status: 409, code: "—", when: "Afterwards, everything is refused" },
      ],
      examples: [
        {
          id: "01",
          name: "RETIRE A PUBLISHED PLAN",
          expect: "200 OK",
          notes: `Run Create Plan Draft, Set Plan Features, Publish Plan, then this.
    OUT: status "RETIRED", effectiveUntil stamped with now, sellable false
         nextStep: "Retired, and no longer on the menu ... Schools ALREADY
                    on it keep it, at the price and features they were sold,
                    and nothing about their subscription has changed."`,
          body: null,
        },
        {
          id: "02",
          name: "WITHDRAW A DRAFT",
          expect: "200 OK",
          notes: `Create a draft and retire it without publishing.
    OUT: status "RETIRED"
         nextStep: "Withdrawn. It was still a draft, so it was never sold to
                    anybody and nothing else is affected. Its plan code
                    stays taken."
    NOTE the last sentence: the code is NOT released. Nothing here deletes.`,
          body: null,
        },
        {
          id: "03",
          name: "RETIRE IT AGAIN",
          expect: "409 Conflict",
          notes: `OUT: { "code": "PLAN_ALREADY_RETIRED" }
    Not an idempotent 200: retiring is terminal, and a caller who cannot
    tell "it was already retired" from "you just retired it" will assume the
    wrong one.`,
          body: null,
        },
        {
          id: "04",
          name: "AFTERWARDS, EVERYTHING IS REFUSED",
          expect: "409 Conflict",
          notes: `PATCH the details:  PLAN_NOT_EDITABLE  "... is RETIRED and cannot be
                                            edited."
    PUT the features:   PLAN_NOT_EDITABLE  "... its features cannot be
                                            changed."
    POST publish:       PLAN_NOT_EDITABLE  "... cannot be published."
    Retiring is not a way back to draft.

    All three messages end "Make a new version of it instead" — which is #5,
    and #5 is deferred. The advice cannot be followed yet.`,
          body: null,
        },
        {
          id: "05",
          name: "effectiveUntil ALREADY IN THE PAST",
          expect: "200 OK",
          notes: `Set the window in the past with #2, then retire.
    OUT: effectiveUntil UNCHANGED. It stopped being sold then; moving the
    date forward to now would rewrite that.`,
          body: null,
        },
        {
          id: "06",
          name: "effectiveUntil IN THE FUTURE",
          expect: "200 OK",
          notes: `Set effectiveUntil to 2030 with #2, publish, then retire.
    OUT: effectiveUntil BROUGHT FORWARD to now — it stops being sold now,
    not in 2030.`,
          body: null,
        },
        {
          id: "07",
          name: "A PLAN OR VERSION THAT DOES NOT EXIST",
          expect: "404 Not Found",
          notes: `OUT: { "code": "PLAN_NOT_FOUND" }`,
          body: null,
        },
      ],
    },
    {
      id: "list-plans",
      name: "List Plans",
      method: "GET",
      path: "/platform/plans",
      status: 'live',
      summary: "The catalogue, filtered and paged. One row per plan VERSION, newest version of each first.",
      schoolSurface: false,
      docs: `**GET** \`/platform/plans\` — the operator's list of every plan.

Every parameter is optional. A bare call returns the first page of the whole catalogue.

| Parameter | Meaning |
|---|---|
| \`status\` | repeatable — \`?status=DRAFT&status=ACTIVE\` means either |
| \`planCode\` | **exact**, case-insensitive, normalized — \`premium-plus\` finds \`PREMIUM_PLUS\` |
| \`name\` | **partial**, case-insensitive |
| \`publiclyAvailable\` | \`true\` or \`false\` |
| \`search\` | partial, across **code or name** — the one box on a screen |
| \`page\`, \`size\` | zero-based; size defaults to 20, max 100 |
| \`sort\` | \`field,direction\` — \`name\`, \`planCode\`, \`planVersion\`, \`status\`, \`listPrice\`, \`createdAt\`, \`updatedAt\` |

Filters combine with AND; only \`status\` is OR within itself.

### One row per plan *version*

\`PREMIUM\` v1 and v2 are two documents with two prices, and a school is on exactly one of them.
The default order groups them: by code, newest version of each first — the catalogue read as a
menu.

### \`sellable\` is the field the list is for

An operator scanning the catalogue is usually asking which of these a school can buy right now.
It is computed from the same three facts everywhere — \`ACTIVE\`, public, in window — so the list
and #10 can never disagree.

### The fifteen test cases are in the description below

Postman sends no body on a GET, so they live here:

\`\`\`
01  BARE LIST                                             -> 200 OK
    GET /platform/plans
    First 20, by code with the newest version of each first.
    content + page, size, totalElements, totalPages, hasNext, hasPrevious.

02  FILTER BY STATUS                                      -> 200 OK
    ?status=DRAFT           only drafts
    ?status=DRAFT&status=ACTIVE   either — repeat the parameter

03  EVERY VERSION OF ONE PLAN                             -> 200 OK
    ?planCode=PREMIUM
    EXACT match, and normalized: ?planCode=premium-plus finds PREMIUM_PLUS.
    A code of the wrong shape simply matches nothing — that is an empty list,
    not an error.

04  FIND A PLAN BY PART OF ITS NAME                       -> 200 OK
    ?name=premium     matches "Premium" and "Premium Plus"
    PARTIAL, because nobody types a plan's full display name to find it.

05  THE ONE SEARCH BOX                                    -> 200 OK
    ?search=prem
    Partial across the code OR the name. planCode and name are there for when
    you know which of the two you are looking at.

06  WHAT A SCHOOL COULD PICK TODAY                        -> 200 OK
    ?status=ACTIVE&publiclyAvailable=true
    Close to the public pricing page. Note it does not check the selling
    window — read \`sellable\` on each row for that.

07  SORT                                                  -> 200 OK
    ?sort=name,asc      ?sort=listPrice,desc      ?sort=createdAt,desc
    Case-insensitive: sort=CreatedAt works.
    EVERY SORT ENDS WITH planCode ASC, planVersion DESC. That pair is unique,
    so paging is deterministic — without a tiebreaker, paging a hundred plans
    that are all ACTIVE can show one twice and miss another, on page two, in
    production, and never in a small test.

08  PAGINATE                                              -> 200 OK
    ?sort=name,asc&page=0&size=1   then   &page=1
    hasPrevious flips to true on page 1.

09  ALL OF IT AT ONCE                                     -> 200 OK
    ?status=DRAFT&search=premium&page=0&size=10&sort=listPrice,desc

10  NO MATCHES                                            -> 200 OK
    ?search=zzz-nothing
    OUT: content [], totalElements 0, totalPages 0
    A 200 with an empty list, NOT a 404. "No plan matches" is a successful
    answer to the question asked.

11  size=0  or  size=5000                            -> 400 Bad Request
    OUT: { "code": "INVALID_PAGE_SIZE",
           "message": "size must be between 1 and 100. Received: 5000" }
    REFUSED, NOT CLAMPED. Silently returning 100 rows for size=5000 looks like
    the whole catalogue.

12  page=-1                                          -> 400 Bad Request
    OUT: { "code": "INVALID_PAGE" }

13  SORT BY SOMETHING NOT ON THE ALLOW-LIST          -> 400 Bad Request
    ?sort=encryptionKeyReference,asc
    OUT: { "code": "INVALID_SORT_FIELD", "message": "... Allowed: name,
           planCode, planVersion, status, listPrice, createdAt, updatedAt." }
    An allow-list, not a pass-through: an arbitrary field means a collection
    scan per request, and the ORDER of a field can leak it even when the value
    is never returned.

    ?sort=name,sideways -> 400 INVALID_SORT_DIRECTION

14  A MISSPELLED STATUS                              -> 400 Bad Request
    ?status=NOPE
    OUT: { "code": "INVALID_PARAMETER",
           "message": "'NOPE' is not a valid value for 'status'. Accepted
                       values: DRAFT, ACTIVE, RETIRED." }

15  REGEX INJECTION IS NOT POSSIBLE                       -> 200 OK
    ?search=.*
    OUT: totalElements 0 — the term is escaped and matched literally.
    Unescaped, \`.*\` would return every plan, and a nested-quantifier pattern
    could hold a database thread on very little input.
\`\`\`
`,
      pathParams: [],
      queryParams: [
        { key: "page", value: "0", enabled: true },
        { key: "size", value: "20", enabled: true },
        { key: "sort", value: "name,asc", enabled: false },
        { key: "status", value: "ACTIVE", enabled: false },
        { key: "planCode", value: "{{planCode}}", enabled: false },
        { key: "name", value: "premium", enabled: false },
        { key: "publiclyAvailable", value: "true", enabled: false },
        { key: "search", value: "prem", enabled: false },
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
          notes: `GET /platform/plans
    First 20, by code with the newest version of each first.
    content + page, size, totalElements, totalPages, hasNext, hasPrevious.`,
          body: null,
        },
        {
          id: "02",
          name: "FILTER BY STATUS",
          expect: "200 OK",
          notes: `?status=DRAFT           only drafts
    ?status=DRAFT&status=ACTIVE   either — repeat the parameter`,
          body: null,
          queryParams: [{ key: "status", value: "DRAFT", enabled: true }],
        },
        {
          id: "03",
          name: "EVERY VERSION OF ONE PLAN",
          expect: "200 OK",
          notes: `?planCode=PREMIUM
    EXACT match, and normalized: ?planCode=premium-plus finds PREMIUM_PLUS.
    A code of the wrong shape simply matches nothing — that is an empty list,
    not an error.`,
          body: null,
          queryParams: [{ key: "planCode", value: "PREMIUM", enabled: true }],
        },
        {
          id: "04",
          name: "FIND A PLAN BY PART OF ITS NAME",
          expect: "200 OK",
          notes: `?name=premium     matches "Premium" and "Premium Plus"
    PARTIAL, because nobody types a plan's full display name to find it.`,
          body: null,
          queryParams: [{ key: "name", value: "premium", enabled: true }],
        },
        {
          id: "05",
          name: "THE ONE SEARCH BOX",
          expect: "200 OK",
          notes: `?search=prem
    Partial across the code OR the name. planCode and name are there for when
    you know which of the two you are looking at.`,
          body: null,
          queryParams: [{ key: "search", value: "prem", enabled: true }],
        },
        {
          id: "06",
          name: "WHAT A SCHOOL COULD PICK TODAY",
          expect: "200 OK",
          notes: `?status=ACTIVE&publiclyAvailable=true
    Close to the public pricing page. Note it does not check the selling
    window — read \`sellable\` on each row for that.`,
          body: null,
          queryParams: [{ key: "status", value: "ACTIVE", enabled: true }, { key: "publiclyAvailable", value: "true", enabled: true }],
        },
        {
          id: "07",
          name: "SORT",
          expect: "200 OK",
          notes: `?sort=name,asc      ?sort=listPrice,desc      ?sort=createdAt,desc
    Case-insensitive: sort=CreatedAt works.
    EVERY SORT ENDS WITH planCode ASC, planVersion DESC. That pair is unique,
    so paging is deterministic — without a tiebreaker, paging a hundred plans
    that are all ACTIVE can show one twice and miss another, on page two, in
    production, and never in a small test.`,
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
          notes: `?status=DRAFT&search=premium&page=0&size=10&sort=listPrice,desc`,
          body: null,
          queryParams: [{ key: "status", value: "DRAFT", enabled: true }, { key: "search", value: "premium", enabled: true }, { key: "page", value: "0", enabled: true }, { key: "size", value: "10", enabled: true }, { key: "sort", value: "listPrice,desc", enabled: true }],
        },
        {
          id: "10",
          name: "NO MATCHES",
          expect: "200 OK",
          notes: `?search=zzz-nothing
    OUT: content [], totalElements 0, totalPages 0
    A 200 with an empty list, NOT a 404. "No plan matches" is a successful
    answer to the question asked.`,
          body: null,
          queryParams: [{ key: "search", value: "zzz-nothing", enabled: true }],
        },
        {
          id: "11",
          name: "size=0  or  size=5000",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_PAGE_SIZE",
           "message": "size must be between 1 and 100. Received: 5000" }
    REFUSED, NOT CLAMPED. Silently returning 100 rows for size=5000 looks like
    the whole catalogue.`,
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
    OUT: { "code": "INVALID_SORT_FIELD", "message": "... Allowed: name,
           planCode, planVersion, status, listPrice, createdAt, updatedAt." }
    An allow-list, not a pass-through: an arbitrary field means a collection
    scan per request, and the ORDER of a field can leak it even when the value
    is never returned.

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
           "message": "'NOPE' is not a valid value for 'status'. Accepted
                       values: DRAFT, ACTIVE, RETIRED." }`,
          body: null,
          queryParams: [{ key: "status", value: "NOPE", enabled: true }],
        },
        {
          id: "15",
          name: "REGEX INJECTION IS NOT POSSIBLE",
          expect: "200 OK",
          notes: `?search=.*
    OUT: totalElements 0 — the term is escaped and matched literally.
    Unescaped, \`.*\` would return every plan, and a nested-quantifier pattern
    could hold a database thread on very little input.`,
          body: null,
          queryParams: [{ key: "search", value: ".*", enabled: true }],
        },
      ],
    },
    {
      id: "list-plan-versions",
      name: "List Plan Versions",
      method: "GET",
      path: "/platform/plans/{code}/versions",
      status: 'live',
      summary: "Every version of one plan, newest first, with the price change and who is on each.",
      schoolSurface: false,
      docs: `**GET** \`/platform/plans/{code}/versions\` — every version of one plan, newest first.

### It answers two questions #8 cannot

**How did the price move?** Each row carries \`priceChangeFromPrevious\` — the subtraction done for
you rather than by eye down a column.

**Can the old versions be forgotten?** Each row carries \`schoolsOnThisVersion\`.

### Not paged

A price does not change fifty times. A history read in pages is not a history, so the whole
thing comes back in one answer.

### \`priceChangeFromPrevious\` is null in two cases

On the **oldest** version, which has nothing to compare against; and across a **currency change**,
because 49999 INR to 699 USD is not a difference of −49300 and pretending otherwise is worse than
saying nothing.

### \`schoolsOnThisVersion\` is 0 everywhere today

Nothing creates subscriptions yet — that is #13, not built. The response's \`note\` says so, so a
column of zeroes is not read as "this plan has no customers". Read them as *unknown*.

### The eight test cases are in the description below

Postman sends no body on a GET, so they live here:

\`\`\`
01  ONE PLAN'S HISTORY                                    -> 200 OK
    GET /platform/plans/{{planCode}}/versions
    OUT: planCode, name (the NEWEST version's name), versionCount, versions[],
         note
    Rows are newest first. Each has planVersion, name, status, listPrice,
    currencyCode, priceChangeFromPrevious, billingCycle, maxStudents,
    maxUsers, publiclyAvailable, sellable, featureCount,
    schoolsOnThisVersion, effectiveFrom, effectiveUntil, createdAt.

02  A PLAN WITH ONLY ONE VERSION                          -> 200 OK
    versionCount 1, and priceChangeFromPrevious null — there is nothing
    before it.

03  READING THE PRICE HISTORY                             -> 200 OK
    With three versions at 10000, 12500 and 11000 INR:
      v3  11000  change -1500
      v2  12500  change +2500
      v1  10000  change null
    Newest first, so each row is compared with the row BELOW it.

04  A CURRENCY CHANGE                                     -> 200 OK
    If one version is in USD and its neighbours in INR, the change is null on
    both sides of it. A number there would be arithmetic on two different
    currencies.

05  name IS THE NEWEST VERSION'S NAME                     -> 200 OK
    A plan can be renamed between versions. The top-level name is what
    somebody means today; each row also carries its own, so a rename is
    visible rather than hidden.

06  A LOWERCASE OR HYPHENATED CODE                        -> 200 OK
    /platform/plans/premium-plus/versions finds PREMIUM_PLUS. Normalized the
    same way it was when the plan was created.

07  A CODE THAT DOES NOT EXIST                       -> 404 Not Found
    OUT: { "code": "PLAN_NOT_FOUND", "message": "No plan 'NOPE' exists." }
    A 404 rather than an empty list: you asked about a specific plan, and it
    is not there. Contrast with #8, where no matches is a 200 and [].

08  CREATING A SECOND VERSION
    You cannot, from the API: #5 (new version) is deferred. Every plan here
    has exactly one version until it is built. To see a real history, insert
    one directly:
      db.plan_definitions.insertOne(
        Object.assign({}, db.plan_definitions.findOne({planCode:'PREMIUM'}),
                      {_id: undefined, planVersion: 2,
                       listPrice: NumberDecimal('12500.00')}))
\`\`\`
`,
      pathParams: [
        { name: "code", value: "{{planCode}}", description: "The plan's permanent family code. Create Plan Draft fills this in." },
      ],
      queryParams: [],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["planCode", "name", "versionCount", "versions", "note"],
      captures: [],
      errors: [
        { status: 404, code: "PLAN_NOT_FOUND", when: "A code that does not exist" },
      ],
      examples: [
        {
          id: "01",
          name: "ONE PLAN'S HISTORY",
          expect: "200 OK",
          notes: `GET /platform/plans/{{planCode}}/versions
    OUT: planCode, name (the NEWEST version's name), versionCount, versions[],
         note
    Rows are newest first. Each has planVersion, name, status, listPrice,
    currencyCode, priceChangeFromPrevious, billingCycle, maxStudents,
    maxUsers, publiclyAvailable, sellable, featureCount,
    schoolsOnThisVersion, effectiveFrom, effectiveUntil, createdAt.`,
          body: null,
        },
        {
          id: "02",
          name: "A PLAN WITH ONLY ONE VERSION",
          expect: "200 OK",
          notes: `versionCount 1, and priceChangeFromPrevious null — there is nothing
    before it.`,
          body: null,
        },
        {
          id: "03",
          name: "READING THE PRICE HISTORY",
          expect: "200 OK",
          notes: `With three versions at 10000, 12500 and 11000 INR:
      v3  11000  change -1500
      v2  12500  change +2500
      v1  10000  change null
    Newest first, so each row is compared with the row BELOW it.`,
          body: null,
        },
        {
          id: "04",
          name: "A CURRENCY CHANGE",
          expect: "200 OK",
          notes: `If one version is in USD and its neighbours in INR, the change is null on
    both sides of it. A number there would be arithmetic on two different
    currencies.`,
          body: null,
        },
        {
          id: "05",
          name: "name IS THE NEWEST VERSION'S NAME",
          expect: "200 OK",
          notes: `A plan can be renamed between versions. The top-level name is what
    somebody means today; each row also carries its own, so a rename is
    visible rather than hidden.`,
          body: null,
        },
        {
          id: "06",
          name: "A LOWERCASE OR HYPHENATED CODE",
          expect: "200 OK",
          notes: `/platform/plans/premium-plus/versions finds PREMIUM_PLUS. Normalized the
    same way it was when the plan was created.`,
          body: null,
        },
        {
          id: "07",
          name: "A CODE THAT DOES NOT EXIST",
          expect: "404 Not Found",
          notes: `OUT: { "code": "PLAN_NOT_FOUND", "message": "No plan 'NOPE' exists." }
    A 404 rather than an empty list: you asked about a specific plan, and it
    is not there. Contrast with #8, where no matches is a 200 and [].

08  CREATING A SECOND VERSION
    You cannot, from the API: #5 (new version) is deferred. Every plan here
    has exactly one version until it is built. To see a real history, insert
    one directly:
      db.plan_definitions.insertOne(
        Object.assign({}, db.plan_definitions.findOne({planCode:'PREMIUM'}),
                      {_id: undefined, planVersion: 2,
                       listPrice: NumberDecimal('12500.00')}))`,
          body: null,
        },
      ],
    },
    {
      id: "get-plan-version",
      name: "Get Plan Version",
      method: "GET",
      path: "/platform/plans/{code}/versions/{version}",
      status: 'live',
      summary: "One plan version in full, with all its features and their labels.",
      schoolSurface: false,
      docs: `**GET** \`/platform/plans/{code}/versions/{version}\` — one plan version, everything about it.

What you open after picking a row out of #8 or #9. The list endpoints report a feature **count**
so a page of rows stays readable; this is where the features themselves are.

### Every feature comes with its wording

\`label\` and \`description\` come from the \`FeatureCode\` enum — the only place they are written — so
a "what this plan includes" screen does not keep its own copy of the wording for 24 features.

### No \`nextStep\`

Nothing happened. That field belongs to the writes, which use a different record for exactly this
reason.

### \`schoolsOnThisVersion\`

Not in the endpoint's field list, and here because it is the question somebody looking at one
version actually has: *can this be retired, or is somebody on it?* It is 0 everywhere until #13
exists, and the \`note\` says so.

### The seven test cases are in the description below

Postman sends no body on a GET, so they live here:

\`\`\`
01  ONE VERSION IN FULL                                   -> 200 OK
    GET /platform/plans/{{planCode}}/versions/{{planVersion}}
    OUT: every field of the plan, plus features[] in full and
         schoolsOnThisVersion.
    Each feature row: featureCode, label, description, enabled, usageLimit,
    usageMetric, overagePolicy.

02  THE SAME SHAPE #3 RETURNS                             -> 200 OK
    Compare a feature row here with one from Set Plan Features. Identical —
    both are PlanFeatureView, so a client that reads one reads the other.

03  A FEATURE WITH NO LIMIT                               -> 200 OK
    ATTENDANCE comes back with usageLimit null AND usageMetric null. It has
    nothing to count; it is included or it is not.

04  A DISABLED FEATURE IS STILL LISTED                    -> 200 OK
    HOSTEL with "enabled": false appears in the list. That is the point of the
    flag: a comparison table can show it with a cross rather than omitting it.

05  A PLAN WITH NO FEATURES                               -> 200 OK
    featureCount 0 and features []. A valid state for a draft; #4 refuses to
    publish it.

06  A VERSION THAT DOES NOT EXIST                    -> 404 Not Found
    Change the version to 9.
    OUT: { "code": "PLAN_NOT_FOUND",
           "message": "No plan 'PREMIUM' version 9 exists." }
    Same for a code that does not exist.

07  A LOWERCASE OR HYPHENATED CODE                        -> 200 OK
    /platform/plans/premium-plus/versions/1 finds PREMIUM_PLUS, normalized the
    same way it was when the plan was created.
\`\`\`
`,
      pathParams: [
        { name: "code", value: "{{planCode}}", description: "The plan's permanent family code. Create Plan Draft fills this in." },
        { name: "version", value: "{{planVersion}}", description: "Which version of that plan. Versions are immutable once published." },
      ],
      queryParams: [],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["planCode", "planVersion", "name", "status", "listPrice", "currencyCode", "publiclyAvailable", "sellable", "featureCount", "features", "schoolsOnThisVersion", "note"],
      captures: [],
      errors: [
        { status: 404, code: "PLAN_NOT_FOUND", when: "A version that does not exist" },
      ],
      examples: [
        {
          id: "01",
          name: "ONE VERSION IN FULL",
          expect: "200 OK",
          notes: `GET /platform/plans/{{planCode}}/versions/{{planVersion}}
    OUT: every field of the plan, plus features[] in full and
         schoolsOnThisVersion.
    Each feature row: featureCode, label, description, enabled, usageLimit,
    usageMetric, overagePolicy.`,
          body: null,
        },
        {
          id: "02",
          name: "THE SAME SHAPE #3 RETURNS",
          expect: "200 OK",
          notes: `Compare a feature row here with one from Set Plan Features. Identical —
    both are PlanFeatureView, so a client that reads one reads the other.`,
          body: null,
        },
        {
          id: "03",
          name: "A FEATURE WITH NO LIMIT",
          expect: "200 OK",
          notes: `ATTENDANCE comes back with usageLimit null AND usageMetric null. It has
    nothing to count; it is included or it is not.`,
          body: null,
        },
        {
          id: "04",
          name: "A DISABLED FEATURE IS STILL LISTED",
          expect: "200 OK",
          notes: `HOSTEL with "enabled": false appears in the list. That is the point of the
    flag: a comparison table can show it with a cross rather than omitting it.`,
          body: null,
        },
        {
          id: "05",
          name: "A PLAN WITH NO FEATURES",
          expect: "200 OK",
          notes: `featureCount 0 and features []. A valid state for a draft; #4 refuses to
    publish it.`,
          body: null,
        },
        {
          id: "06",
          name: "A VERSION THAT DOES NOT EXIST",
          expect: "404 Not Found",
          notes: `Change the version to 9.
    OUT: { "code": "PLAN_NOT_FOUND",
           "message": "No plan 'PREMIUM' version 9 exists." }
    Same for a code that does not exist.`,
          body: null,
        },
        {
          id: "07",
          name: "A LOWERCASE OR HYPHENATED CODE",
          expect: "200 OK",
          notes: `/platform/plans/premium-plus/versions/1 finds PREMIUM_PLUS, normalized the
    same way it was when the plan was created.`,
          body: null,
        },
      ],
    },
  ],
};

const GROUP_PLANS_SUBSCRIPTIONS = {
  id: "plans-subscriptions",
  module: "Plans / Subscriptions",
  endpoints: [
    {
      id: "create-subscription",
      name: "Create Subscription",
      method: "POST",
      path: "/platform/schools/{id}/subscriptions",
      status: 'live',
      summary: "Makes a school a paying customer. Closes the gap core activation complains about.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/subscriptions\` — gives a school its first subscription.

What makes a school a paying customer, and the piece \`core\` has been complaining about:
\`activateSchool\` was written to require a subscription, found nothing could create one, and
settled for a soft check that announces the gap in every response. Create one first and
\`subscriptionStatus\` reports \`ACTIVE\` instead of \`NONE\`.

### Two fields is the ordinary request

The plan already knows the price, the currency, the cycle and therefore when the first period
ends. Everything else exists for a negotiated deal — a discount, a raised limit, a trial.

### Three documents, one transaction

The subscription, its first \`subscription_history\` row, and the \`number_sequences\` row it took
\`subscriptionNo\` from. A subscription with no history row is a customer nobody can explain.

### The plan must be sellable

\`ACTIVE\` and inside its selling window. A plan that is published but **not** publicly available
is allowed — that is exactly a private quote.

### One current subscription per school

A second is a \`409\` telling you to change the plan on the existing one.

### The eleven test cases are in the request body as comments
`,
      bodyNotes: `Platform surface. Needs {{schoolId}} and a PUBLISHED plan.

 THIS IS WHAT MAKES A SCHOOL A PAYING CUSTOMER, and it is the piece core has
 been complaining about. activateSchool (#3 in core) was written to require
 an active subscription, found nothing could create one, and settled for a
 soft check — every activate response carried:

   "subscriptionStatus": "NONE",
   "subscriptionNote": "No subscription exists for this school. Activation
    was allowed anyway because nothing creates subscriptions yet — this
    check must become a hard requirement once it does."

 Create a subscription first and the same call now reports
 "subscriptionStatus": "ACTIVE" with no note.

 TWO FIELDS IS THE ORDINARY REQUEST. The plan already knows the price, the
 currency, the billing cycle and therefore when the first period ends. The
 rest of the fields exist for a negotiated deal.

 THE PLAN IS NAMED BY CODE AND VERSION, not by a Mongo id — the same way
 every plan URL names one.

 THREE DOCUMENTS, ONE TRANSACTION: the subscription, its first
 subscription_history row, and the number_sequences row it took
 subscriptionNo from. A subscription with no history row is a customer
 nobody can explain; a number handed out with no subscription attached is a
 permanent gap in the numbering that looks like a deleted record.`,
      requiredFields: ["planCode", "planVersion"],
      optionalFields: ["trial", "currentPeriodStart", "currentPeriodEnd", "autoRenew", "contractedPrice", "maxStudentsOverride", "maxUsersOverride", "billingCustomerReference", "reason"],
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "planCode": "{{planCode}}",
  "planVersion": 1
}`,
      successStatus: 201,
      responseFields: ["subscriptionId", "subscriptionNo", "schoolId", "planCode", "planVersion", "planName", "status", "billingCycle", "currentPeriodStart", "currentPeriodEnd", "autoRenew", "contractedPrice", "planListPrice", "currencyCode", "maxStudents", "maxUsers", "hasLimitOverrides", "current", "nextStep"],
      captures: [
        { variable: "subscriptionNo", from: "subscriptionNo" },
      ],
      errors: [
        { status: 400, code: "BILLING_PERIOD_END_REQUIRED", when: "A custom billing cycle" },
        { status: 400, code: "INVALID_BILLING_PERIOD", when: "A period that runs backwards" },
        { status: 400, code: "LIMIT_TOO_LOW", when: "An override of zero" },
        { status: 404, code: "—", when: "An unknown school or plan" },
        { status: 409, code: "SUBSCRIPTION_ALREADY_EXISTS", when: "A second subscription for the same school" },
        { status: 409, code: "PLAN_NOT_SELLABLE", when: "A plan that is still a draft" },
        { status: 409, code: "SCHOOL_NOT_SUBSCRIBABLE", when: "A school that cannot be sold to" },
      ],
      examples: [
        {
          id: "01",
          name: "THE ORDINARY REQUEST",
          expect: "201 Created",
          notes: `The body above, on a published plan.
    OUT: subscriptionNo "SUB/2026/09/000001", status "ACTIVE", current true
         contractedPrice = the plan's listPrice
         currencyCode    = the plan's currency (never the caller's)
         billingCycle    = the plan's cycle
         currentPeriodEnd = start + one cycle
         maxStudents / maxUsers = the plan's, since no override was sent
    Header: Location: /platform/schools/{id}/subscriptions/SUB/2026/09/000001`,
          body: null,
        },
        {
          id: "02",
          name: "A NEGOTIATED DEAL",
          expect: "201 Created",
          notes: `OUT: status "TRIAL", hasLimitOverrides true, and the response shows
         contractedPrice 39999.50 NEXT TO planListPrice 49999.00 — the only
         way to notice a school is on a discount.
    maxStudents comes back as 2500: the response reports the limit IN FORCE,
    not the raw override, so no caller has to work out which applies.`,
          body: `{
  "planCode": "{{planCode}}",
  "planVersion": 1,
  "trial": true,
  "contractedPrice": 39999.50,
  "maxStudentsOverride": 2500,
  "maxUsersOverride": 300,
  "autoRenew": false,
  "billingCustomerReference": "cus_Qx7B2mR9",
  "reason": "Pilot, 20% partner discount."
}`,
        },
        {
          id: "03",
          name: "A SECOND SUBSCRIPTION FOR THE SAME SCHOOL",
          expect: "409 Conflict",
          notes: `Send case 01 twice.
    OUT: { "code": "SUBSCRIPTION_ALREADY_EXISTS",
           "message": "... is already on SUB/2026/09/000001. Change the plan on
                       that subscription rather than creating a second
                       one." }
    A unique partial index enforces one current subscription per school, but
    a duplicate-key error tells the caller nothing about what to do instead.`,
          body: null,
        },
        {
          id: "04",
          name: "A PLAN THAT IS STILL A DRAFT",
          expect: "409 Conflict",
          notes: `OUT: { "code": "PLAN_NOT_SELLABLE",
           "message": "... is DRAFT, so no school can be put on it. Publish
                       it first." }

    A RETIRED plan is also refused, with different advice — "A retired plan
    cannot be sold again" — because telling somebody to publish a retired
    plan sends them to an endpoint that will refuse them.

    A plan that is published but NOT publicly available IS allowed: that is
    exactly a private quote, and this is how a private quote gets sold.`,
          body: null,
        },
        {
          id: "05",
          name: "A CUSTOM BILLING CYCLE",
          expect: "400 Bad Request",
          notes: `On a plan whose billingCycle is CUSTOM, with no currentPeriodEnd:
    OUT: { "code": "BILLING_PERIOD_END_REQUIRED",
           "message": "This plan bills on a CUSTOM cycle, which has no set
                       length, so currentPeriodEnd has to be sent." }
    Every other cycle derives it. Guessing a month for CUSTOM would be
    inventing a contract term.`,
          body: null,
        },
        {
          id: "06",
          name: "THE PERIOD END DERIVED FROM THE CYCLE",
          expect: "201 Created",
          notes: `A YEARLY plan gives currentPeriodEnd 2027-04-01.
    A MONTHLY plan starting 31 January gives 28 February — the calendar
    clamps, which is what a person means by "a month later".
    The arithmetic runs in the SCHOOL'S time zone, because a billing period
    is a pair of dates somebody reads.`,
          body: `{
  "planCode": "{{planCode}}", "planVersion": 1,
  "currentPeriodStart": "2026-04-01T00:00:00Z"
}`,
        },
        {
          id: "07",
          name: "A PERIOD THAT RUNS BACKWARDS",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_BILLING_PERIOD" }`,
          body: `{
  "planCode": "{{planCode}}", "planVersion": 1,
  "currentPeriodStart": "2027-01-01T00:00:00Z",
  "currentPeriodEnd": "2026-01-01T00:00:00Z"
}`,
        },
        {
          id: "08",
          name: "AN OVERRIDE OF ZERO",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "LIMIT_TOO_LOW", "message": "... Omit it to use the
           plan's own limit." }
    A negative contractedPrice is PRICE_NEGATIVE, from the same validator #1
    and #2 use.`,
          body: `{
  "planCode": "{{planCode}}", "planVersion": 1, "maxStudentsOverride": 0
}`,
        },
        {
          id: "09",
          name: "A SCHOOL THAT CANNOT BE SOLD TO",
          expect: "409 Conflict",
          notes: `A CLOSED, DELETION_PENDING or DELETED school.
    OUT: { "code": "SCHOOL_NOT_SUBSCRIBABLE" }
    Not reachable yet — #13 to #17 in core are deferred, so no school can
    reach those states.`,
          body: null,
        },
        {
          id: "10",
          name: "AN UNKNOWN SCHOOL OR PLAN",
          expect: "404 Not Found",
          notes: `SCHOOL_NOT_FOUND or PLAN_NOT_FOUND.

11  WHAT LANDS IN THE DATABASE
    From mongosh, after case 01:
      db.school_subscriptions.findOne({schoolId: "..."})
      db.subscription_history.find({schoolSubscriptionDocsId: "..."})
        -> one row: eventType CREATED (or TRIAL_STARTED), previousStatus
           null — there was no status before this — newStatus ACTIVE,
           source ADMIN_PORTAL, performedByDocsId null because nobody is
           signed in yet.
      db.number_sequences.findOne({schoolId: "...",
                                   sequenceType: "SUBSCRIPTION"})
        -> nextValue 2, prefixTemplate "SUB/{YYYY}/{MM}/"

    THE HOUSE FORMAT IS XXX/{YYYY}/{MM}/ + six digits, for every kind of
    number in the system: SUB/2026/09/000001, ADM/2026/09/000123,
    RCPT/2026/09/000045. The trailing slash matters — the number is appended
    straight on, so "SUB/{YYYY}/{MM}" would give SUB/2026/09000001.

    NUMBERING IS PER SCHOOL. Two schools both get SUB/2026/09/000001, which
    is correct: subscriptionNo is unique per school, not globally.`,
          body: null,
        },
      ],
    },
    {
      id: "activate-subscription",
      name: "Activate Subscription",
      method: "POST",
      path: "/platform/schools/{id}/subscriptions/current/activate",
      status: 'live',
      summary: "Turns a trial into a paying subscription. The plan, price and limits do not move.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/subscriptions/{no}/activate\` — turns a trial into a paying subscription.

For when the school has tried it and agreed to buy. Nothing about what they are buying changes:
same plan, same price, same limits, same \`autoRenew\`. All that changes is that they are paying
now, and a fresh paid period starts.

### Use \`current\` as the number

A real subscription number is \`SUB/2026/09/000001\`. The slashes end the path segment, so it
cannot be written in a URL, and \`%2F\` is refused by Tomcat with a 400 before Spring sees it. A
school has exactly one current subscription, so \`current\` names it without ambiguity.

### The body is optional — all of it

Send nothing and the paid period starts now and runs for one billing cycle, read from the
subscription rather than the plan because the cycle was frozen onto it when it was sold.

### Only a TRIAL can be activated

Anything else is a \`409\` that says what to do instead, and the advice differs by status.

### The plan is not re-checked

If the plan was retired mid-trial the school still gets to buy it. Retiring takes a plan off the
menu for new schools and changes nothing for a school already on it.

### The eight test cases are in the request body as comments
`,
      bodyNotes: `Platform surface. Needs {{schoolId}} and a subscription that is on TRIAL.
 Make one with Create Subscription, sending "trial": true.

 THIS IS ONE STATUS CHANGE AND NOTHING ELSE. Same plan, same price, same
 limits. The plan, the price and the limits are deliberately NOT in the
 request body, so they cannot be changed here by accident.

 THE CLOCK STARTS AGAIN. The trial is finished, so a fresh paid period
 begins rather than the trial's dates being kept. It runs for one of
 whatever cycle the subscription was sold on — read from the SUBSCRIPTION,
 not the plan, because the cycle was copied onto it when it was sold and
 that is the contract.

 USE current AS THE NUMBER. A real one is SUB/2026/09/000001 and the
 slashes end the path segment. Encoding them as %2F does not help:
   GET /platform/plans/AB%2FCD/versions/1
   -> 400 Invalid URI: [The encoded slash character is not allowed]
 That is Tomcat, before Spring routes anything.`,
      optionalFields: ["currentPeriodStart", "currentPeriodEnd", "reason"],
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{}`,
      successStatus: 200,
      responseFields: ["subscriptionId", "subscriptionNo", "schoolId", "planCode", "planVersion", "planName", "status", "billingCycle", "currentPeriodStart", "currentPeriodEnd", "autoRenew", "contractedPrice", "planListPrice", "currencyCode", "maxStudents", "maxUsers", "hasLimitOverrides", "current", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_BILLING_PERIOD", when: "A period that runs backwards" },
        { status: 404, code: "—", when: "A school or a number that does not exist" },
        { status: 409, code: "SUBSCRIPTION_NOT_TRIAL", when: "Activating something that is not a trial" },
      ],
      examples: [
        {
          id: "01",
          name: "THE ORDINARY CASE — NO BODY AT ALL",
          expect: "200 OK",
          notes: `Send nothing. Or {} as it stands above.
    OUT: status "ACTIVE", currentPeriodStart now, currentPeriodEnd one
         billing cycle later, nextStep "Now paying, from ... to ...".`,
          body: null,
        },
        {
          id: "02",
          name: "WITH A REASON, FOR THE HISTORY ROW",
          expect: "200 OK",
          notes: `The reason lands on the subscription_history row, not on the
    subscription. See case 08.`,
          body: `{
  "reason": "Signed the annual contract on 3 September."
}`,
        },
        {
          id: "03",
          name: "BACKDATED, BECAUSE THE DEAL WAS AGREED LAST WEEK",
          expect: "200 OK",
          notes: `currentPeriodEnd is worked out from the start, so a yearly plan now
    runs to 2027-09-01.`,
          body: `{
  "currentPeriodStart": "2026-09-01T00:00:00Z",
  "reason": "Backdated to the date they signed."
}`,
        },
        {
          id: "04",
          name: "BOTH DATES SPELLED OUT",
          expect: "200 OK",
          notes: `A short first period, to line the school up with the April billing run.`,
          body: `{
  "currentPeriodStart": "2026-09-01T00:00:00Z",
  "currentPeriodEnd": "2027-03-31T23:59:59Z"
}`,
        },
        {
          id: "05",
          name: "A PERIOD THAT RUNS BACKWARDS",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_BILLING_PERIOD" }`,
          body: `{
  "currentPeriodStart": "2026-09-01T00:00:00Z",
  "currentPeriodEnd": "2026-08-01T00:00:00Z"
}`,
        },
        {
          id: "06",
          name: "ACTIVATING SOMETHING THAT IS NOT A TRIAL",
          expect: "409 Conflict",
          notes: `Run case 01 twice. The second time:
    OUT: { "code": "SUBSCRIPTION_NOT_TRIAL",
           "message": "SUB/2026/09/000001 is ACTIVE, and only a TRIAL can
                       be activated. It is already paying, so there is
                       nothing to do." }
    A 409 rather than a quiet 200 on purpose: the second call is a
    different intention from the first, usually somebody who does not know
    it already happened, and a 200 would hide that.
    The advice changes with the status — PAST_DUE says take the payment,
    SUSPENDED says lift the suspension, CANCELLED and EXPIRED say create a
    new subscription. None of those states are reachable yet: the
    endpoints that set them are #15 to #26 and are not built.`,
          body: null,
        },
        {
          id: "07",
          name: "A SCHOOL OR A NUMBER THAT DOES NOT EXIST",
          expect: "404 Not Found",
          notes: `SCHOOL_NOT_FOUND, or SUBSCRIPTION_NOT_FOUND when the school has no
    subscription at all. Ask for a school that has none and the message
    tells you to create one first.

08  WHAT LANDS IN THE DATABASE
    From mongosh, after case 02:
      db.school_subscriptions.findOne({schoolId: "..."})
        -> status ACTIVE, currentPeriodStart and currentPeriodEnd both
           moved. planDefinitionDocsId, contractedPrice, currencyCode,
           maxStudentsOverride, autoRenew and current all unchanged.
      db.subscription_history.find({schoolSubscriptionDocsId: "..."})
        -> now TWO rows. The first from Create Subscription
           (TRIAL_STARTED, previousStatus null), and a second:
           eventType ACTIVATED, previousStatus TRIAL, newStatus ACTIVE,
           source ADMIN_PORTAL, reason the one you sent,
           performedByDocsId null because nobody is signed in yet,
           effectiveAt the period start — which is when it took effect,
           not when the row was written. createdAt is the write time.

    NO NUMBER IS TAKEN. Unlike Create Subscription this touches no
    number_sequences row: the subscription already has its number.`,
          body: null,
        },
      ],
    },
    {
      id: "get-subscription",
      name: "Get Subscription",
      method: "GET",
      path: "/platform/schools/{id}/subscription",
      status: 'live',
      summary: "What one school is on right now: the plan and its features, the price, the status, the period.",
      schoolSurface: false,
      docs: `**GET** \`/platform/schools/{id}/subscription\` — what this school is on right now.

The whole of it: the plan and its features, the price they actually pay against the plan's list
price, the status, and when the period ends.

### Singular, because a school has one

\`/subscriptions\` is the collection you post to; \`/subscription\` is the one they are on. A unique
partial index makes sure there is only ever one, so there is nothing to page through.

### The features are listed, not counted

The plan list reports a count, because feature rows on every row of a page is noise. This is one
school, and *what has this school paid for* is the question it answers.

### Three things it works out for you

- \`daysRemaining\` — how long is left in the period.
- \`periodEnded\` — the end has passed while the status still says the school is paying. Real
  today: nothing renews or expires a subscription yet, so a period just lapses.
- \`planRetired\` — the plan has been taken off the menu. Allowed; the school keeps it.

\`note\` says any of that in a sentence.

### Two 404s, not one

\`SCHOOL_NOT_FOUND\` and \`SUBSCRIPTION_NOT_FOUND\` are different problems and get different answers.

### The five test cases are in this description — Postman sends no body on a GET

---

**01  A SCHOOL ON A PAID SUBSCRIPTION  -> 200 OK**
Run Create Subscription first. Returns \`status\` ACTIVE, \`daysRemaining\` counting down,
\`periodEnded\` false, \`note\` null, and \`features\` listing what the plan grants.

**02  A SCHOOL ON A TRIAL  -> 200 OK**
Create one with \`"trial": true\`. \`status\` TRIAL, and \`note\` says activating it is what turns it
into a paying subscription. Run Activate Subscription and call this again: \`status\` ACTIVE and
that part of the note is gone.

**03  A NEGOTIATED DEAL  -> 200 OK**
Create with \`contractedPrice\` below the plan's, and \`maxStudentsOverride\` above it.
\`hasDiscount\` true, \`contractedPrice\` and \`planListPrice\` both present and different,
\`maxStudents\` shows the override with \`hasLimitOverrides\` true.

**04  A SCHOOL WITH NO SUBSCRIPTION  -> 404 Not Found**
\`{ "code": "SUBSCRIPTION_NOT_FOUND", "message": "'<school>' has no subscription. Create one
first." }\`
A school id that does not exist gives \`SCHOOL_NOT_FOUND\` instead — that is the whole reason
there are two codes.

**05  A LAPSED PERIOD  -> 200 OK**
Create with \`currentPeriodEnd\` in the past, then call this. \`periodEnded\` true while \`status\`
still says ACTIVE, \`daysRemaining\` negative, and \`note\` explains that nothing marks a
subscription expired yet so it has to be read as lapsed. This is the case a screen trusting
\`status\` alone would get wrong.
`,
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["subscriptionId", "subscriptionNo", "schoolId", "planDefinitionDocsId", "planCode", "planVersion", "planName", "planStatus", "planRetired", "status", "billingCycle", "currentPeriodStart", "currentPeriodEnd", "daysRemaining", "periodEnded", "autoRenew", "current", "contractedPrice", "planListPrice", "currencyCode", "hasDiscount", "maxStudents", "maxUsers", "maxStudentsOverride", "maxUsersOverride", "hasLimitOverrides", "featureCount", "features", "cancelledAt", "cancellationReason", "billingCustomerReference", "note"],
      captures: [
        { variable: "subscriptionNo", from: "subscriptionNo" },
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
  GROUP_PLANS_PLAN_CATALOGUE,
  GROUP_PLANS_SUBSCRIPTIONS,
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
