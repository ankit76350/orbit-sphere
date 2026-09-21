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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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

### The nine test cases are in the request body as comments
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
      id: "end-academic-year",
      name: "End Academic Year",
      method: "POST",
      path: "/schools/current/academic-years/{name}/end",
      status: 'live',
      summary: "Ends the year today: stops it running and closes its dates on today.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{name}/end\` — the year is over as of today.

Sets \`isThisYearRunning\` to false and writes \`endDate\` = **today in the school's own timezone**.
Takes no body.

### An event, not a re-plan — which is why it is not Update Dates

\`PATCH .../dates\` moves a year's boundaries because somebody **decided** they should be
different. This records that the year **is over**: a term finished early, a school closing, a
calendar superseded. They are allowed to refuse different things:

| | \`PATCH .../dates\` | \`POST .../end\` |
|---|---|---|
| A 6-day year | \`400 IMPLAUSIBLE_DATE_RANGE\` | **allowed** |
| Moving \`endDate\` later | allowed — that is the point | \`409 ACADEMIC_YEAR_ALREADY_ENDED\` |
| Which date it writes | whatever you send | today, always |
| \`isThisYearRunning\` | untouched | set false |

**It deliberately skips the plausibility check.** \`validateAcademicYearRange\` rejects any range
under 30 days as "almost certainly a typo" — right when *planning* a year, wrong here: a school
that shut two weeks into term really did have a two-week year, and refusing to record it would
leave the calendar claiming a year that is still running.

### The trap it exists to avoid

\`ACADEMIC_YEAR_ALREADY_ENDED\`. Writing today's date onto a year that closed last March would
push its end **forward** by months — the opposite of ending it — and it would look like it
worked.

### Today is the SCHOOL's today

\`Dates.todayIn(school.defaultTimeZone)\`, not the server's date. At 23:00 in Asia/Kolkata it is
still the previous day in UTC, and closing a year a day early loses a day of the school's work.

### Stranded closed days are refused, never deleted

Same code and policy as Update Dates, so the two cannot answer differently. Deleting a school's
calendar as a side effect of a different action is not something this should do quietly. A closed
day already **behind** today is kept — it is still inside the shortened year.

### Idempotent

Ending an already-ended year is a \`200\` saying *"Nothing changed"*, like the enrollment and
results flags. The note reports the two halves separately — whether it stopped running, and
whether the dates moved — because they move independently.

### What it does NOT touch

\`enrollmentEnabled\` and \`resultsLocked\` are left alone, and the response says so. Both are
arguably implied by a year ending; neither was asked for.

**No authorization is enforced yet.**

### The nine test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain. Run Create School and Create Academic Year first.

 AN EVENT, NOT AN EDIT. Sets isThisYearRunning false and endDate = today, in
 the SCHOOL's timezone. No body needed; anything sent is ignored.

 THE TRAP: a year that already finished is REFUSED, because writing today
 would move its end date FORWARD. That is the opposite of ending it.

 A very short year is allowed here and refused by Update Dates, which
 rejects anything under 30 days as an implausible range.`,
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
      responseFields: ["academicYearId", "name", "startDate", "endDate", "durationDays", "current", "holidayCount", "enrollmentEnabled", "resultsLocked", "isThisYearRunning", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "No tenant header" },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "Unknown year name" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_STARTED", when: "Today is on or before its first day — it would end before it began." },
        { status: 409, code: "ACADEMIC_YEAR_ALREADY_ENDED", when: "It finished in the past, so writing today would move that date FORWARD." },
        { status: 409, code: "HOLIDAYS_OUTSIDE_NEW_RANGE", when: "Closed days after today would be stranded outside the shortened year." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
      ],
      examples: [
        {
          id: "01",
          name: "END A RUNNING YEAR",
          expect: "200 OK",
          notes: `OUT: endDate becomes today, durationDays shrinks.
         "...it is no longer the year this school is running, and its
          last day is now Wednesday 9 September 2026, 233 day(s)
          earlier than planned."`,
          body: null,
        },
        {
          id: "02",
          name: "SEND IT AGAIN",
          expect: "200 OK",
          notes: `IDEMPOTENT. "Nothing changed: '2026-2027' was already closed on
    Wednesday 9 September 2026 and was not marked as running."`,
          body: null,
        },
        {
          id: "03",
          name: "A YEAR THAT ALREADY FINISHED",
          expect: "409 Conflict",
          notes: `Make a year whose endDate is in the past, then end it.
    OUT: { "code": "ACADEMIC_YEAR_ALREADY_ENDED" }
         "...already finished on ... Ending it today would move that
          date forward."
    THIS IS THE POINT OF THE ENDPOINT'S MAIN GUARD.`,
          body: null,
        },
        {
          id: "04",
          name: "A YEAR THAT HAS NOT STARTED",
          expect: "409 Conflict",
          notes: `Make a year starting next month, then end it.
    OUT: { "code": "ACADEMIC_YEAR_NOT_STARTED" }
         "...starts on Friday 9 October 2026, so it cannot be ended today."`,
          body: null,
        },
        {
          id: "05",
          name: "A CLOSED DAY AFTER TODAY",
          expect: "409 Conflict",
          notes: `Add a holiday dated next week, then end the year.
    OUT: { "code": "HOLIDAYS_OUTSIDE_NEW_RANGE" }
         "1 closed day(s) fall after today and would end up outside the
          year, starting with ... Remove them first."
    REFUSED, NOT DELETED. Remove the holiday, then end it.`,
          body: null,
        },
        {
          id: "06",
          name: "A CLOSED DAY BEFORE TODAY IS KEPT",
          expect: "200 OK",
          notes: `Add a holiday dated last week, then end the year.
    It succeeds and holidayCount stays as it was: a closed day already
    behind us is still inside the shortened year.`,
          body: null,
        },
        {
          id: "07",
          name: "A YEAR CUT VERY SHORT",
          expect: "200 OK",
          notes: `Make a year that started 5 days ago. Then:
      PATCH .../dates {"endDate": "<today>"}  -> 400 IMPLAUSIBLE_DATE_RANGE
      POST  .../end                          -> 200 OK
    A school that shut in week one really had a 6-day year.`,
          body: null,
        },
        {
          id: "08",
          name: "UNKNOWN YEAR NAME",
          expect: "404 Not Found",
          notes: `Change {{academicYearName}} to 1999-2000.
    OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }`,
          body: null,
        },
        {
          id: "09",
          name: "NO TENANT HEADER",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "TENANT_NOT_RESOLVED" }`,
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
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
      summary: "Makes the school row at PROVISIONING. That is all it does.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools\` — provision a new tenant.

Creates the \`School\` row at \`PROVISIONING\` — the only starting state there is. It does **not** create a user, so the
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
\`stateOrProvince\` (100) · \`postalCode\` (20)

### Refused if sent

\`status\`, \`encryptionKeyReference\`, \`activatedAt\`, \`suspendedAt\` — the DTO has no such fields,
so they are ignored rather than honoured. Each would hand the caller something the document
defends.

### Test cases — all 10 are in the request body as comments

| # | Case | Result |
|---|---|---|
| 01 | Full payload | 201 |
| 02 | Minimum payload | 201 |
| 03 | No starting-state choice | 201, \`status: PROVISIONING\` |
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
  "postalCode": "411001"
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
          name: "THERE IS NO STARTING-STATE CHOICE",
          expect: "201 Created",
          notes: `Every school starts at PROVISIONING. A caller cannot ask for ACTIVE,
    because that would skip the subscription check, and there is no TRIAL
    any more — a trial belongs to the SUBSCRIPTION, where it has a plan
    and a period behind it. Send "trial": true here and it is ignored
    like any other unknown field.`,
          body: null,
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
| \`number_sequences\` counters | the first student admission asks for a number and finds no counter |
| \`roles\` entries | the first \`UserAccount\` is created and has nothing to point \`roleKeys\` at |

This closes both gaps, and must run before the school can be activated.

### One document each, not fifty-one

Restructured 2026-09-05. A provisioned school used to hold 48 \`number_sequences\` documents and
3 \`roles\` documents. It now holds **one of each**: a \`number_sequences\` document with a
\`counters\` array of 48 entries, and a \`roles\` document with a \`roles\` array of 3. The response
counts below are unchanged — they count entries, not documents.

### Request

No body. \`{{schoolId}}\` in the path — saved automatically by **Create School**.

### Idempotent

Reads what exists and adds only the gaps, with a \`$push\` rather than saving the document back —
so a counter already part-way through its numbering keeps its \`nextValue\`, and a role whose
permissions the school has edited is never overwritten by our defaults. Safe to send repeatedly;
safe to send when you do not know what state the school is in. That is the point of it.

### Responses

| Case | Status | Code |
|---|---|---|
| Fresh school | \`200\` | — 48 sequences, 3 roles created |
| Sent again | \`200\` | — 0 created, everything already present |
| Partial repair | \`200\` | — only the gaps created |
| Unknown id | \`404\` | \`SCHOOL_NOT_FOUND\` |
| Offboarding / closed / deleted | \`409\` | \`SCHOOL_NOT_PROVISIONABLE\` |

\`readyToActivate\` answers the operator's real question: every sequence type has an entry and
\`SCHOOL_ADMIN\` is in the roles array.

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
      summary: "Takes the school live. PROVISIONING to ACTIVE. Refuses a second call. A provisioned school is activated by its subscription too.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/activate\` — takes the school live.

\`PROVISIONING\` → \`ACTIVE\`. Anything else is a \`409\`.

**A school does not always need this call.** Create Subscription activates a \`PROVISIONING\`
school whose provisioning is finished, because a subscription is the last thing such a school is
waiting for. This endpoint stays for the school that is activated before it is sold to.

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
| Provisioned school | \`200\` | — \`ACTIVE\`, \`activatedAt\` set |
| No SCHOOL_ADMIN role | \`409\` | \`SETUP_INCOMPLETE\` |
| Missing number sequences | \`409\` | \`SETUP_INCOMPLETE\` (with the count of \`counters\` entries, not documents) |
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
                       PROVISIONING can. A suspended school is
                       reactivated, not activated." }

    NOT idempotent, unlike Complete Provisioning. This one refuses, because
    the caller believes they changed something and they did not. Bringing a
    suspended school back is endpoint #5 reactivate — a different operation.`,
          body: null,
        },
        {
          id: "05",
          name: "A SCHOOL ITS SUBSCRIPTION ALREADY ACTIVATED",
          expect: "409 Conflict",
          notes: `Create School, Complete Provisioning, then Create Subscription — and
    skip this endpoint. The school is ACTIVE already: a provisioned school
    waiting only on a subscription is activated by the sale.
    OUT: { "code": "SCHOOL_NOT_ACTIVATABLE" }, same as case 04.

    There is no TRIAL starting state. A trial belongs to the SUBSCRIPTION,
    where it has a plan and a period behind it.`,
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
| \`status\` | repeatable — \`?status=ACTIVE&status=SUSPENDED\` means either |
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
    ?status=ACTIVE&status=SUSPENDED
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
                       Accepted values: PROVISIONING, ACTIVE, SUSPENDED, ..." }

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
          notes: `?status=ACTIVE&status=SUSPENDED
    OR within the field: "show me the live ones" is one question.`,
          body: null,
          queryParams: [{ key: "status", value: "ACTIVE", enabled: true }, { key: "status", value: "SUSPENDED", enabled: true }],
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
                       Accepted values: PROVISIONING, ACTIVE, SUSPENDED, ..." }`,
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
 perfect on every screen while the feature access service, asking for
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
    should not wipe a plan's feature access.`,
          body: `{
  "features": []
}`,
        },
        {
          id: "04",
          name: "THE SAME FEATURE TWICE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "DUPLICATE_FEATURE" }
    Two rows for one feature is not a bigger feature access, it is a question:
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
           "message": "... stops being sold on Friday 1 January 2021
                       12:00AM, which has passed." }
    EVERY DATE IN A MESSAGE IS SPELLED OUT — "Friday 1 January 2021 12:00AM",
    not 2021-01-01T00:00:00Z. A plan's selling window belongs to the platform
    rather than to a school, so it reads in UTC; a date that belongs to a
    school reads in that school's own timezone.`,
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
         nextStep says "It goes on sale on Thursday 1 April 2027 12:00AM."`,
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

### The nine test cases are in the request body as comments
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
    both are PlanFeatureResponse, so a client that reads one reads the other.

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
    both are PlanFeatureResponse, so a client that reads one reads the other.`,
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
      summary: "Makes a school a paying customer, and takes a fully provisioned school from PROVISIONING to ACTIVE.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/subscriptions\` — gives a school its first subscription.

### currentPeriodStart is required, on every cycle

It used to default to today and no longer does: the start is the anchor the end is measured from,
and on a \`YEARLY\` sale it fixes which day the school is billed on for as long as it stays. So it
is somebody's decision rather than a convenience — sending today explicitly says the same thing the
default did, and says it on purpose. Omitting it is \`400 VALIDATION_FAILED\` naming the field.

It has to be **today or later** (\`400 PERIOD_START_IN_PAST\`), and "today" is the start of today in
the **school's own timezone** — for a school west of UTC, \`{day}T00:00:00Z\` is still the previous
day there and gets refused. \`CUSTOM\` needs \`currentPeriodEnd\` alongside it; the other four
cadences derive theirs.

What makes a school a paying customer, and the piece \`core\` has been complaining about:
\`activateSchool\` was written to require a subscription, found nothing could create one, and
settled for a soft check that announces the gap in every response. Create one first and
\`subscriptionStatus\` reports \`ACTIVE\` instead of \`NONE\`.

### The period, and the capacity, come from the plan

The period starts at **midnight today in the school's own zone** and runs for a **fixed count of
days** taken from the plan's cycle — 30, 90, 180, 365. \`CUSTOM\` has no length, so the caller must
send \`currentPeriodEnd\` there.

\`maxStudentsOverride\` and \`maxUsersOverride\` are **written on every sale**, copied from the plan
when the caller named no figures. So the subscription says what the school may use without
anybody reading the plan behind it, and a school already sold keeps what it bought when the
plan's next version moves its ceiling.

### Two fields is the ordinary request

The plan already knows the price, the currency, the cycle and therefore when the first period
ends. Everything else exists for a negotiated deal — a discount, a raised limit, a trial.

### Three writes, one transaction

The subscription, its first \`subscription_history\` row, and an \`$inc\` on the school's
\`number_sequences\` document to take \`subscriptionNo\` from its \`SUBSCRIPTION\` counter. A
subscription with no history row is a customer nobody can explain.

The counter is an entry in one document per school since 2026-09-05, so the allocation is a
\`findAndModify\` with \`$inc\` through the positional operator — still one atomic step, still
returning the value before the increment.

### The plan must be sellable

\`ACTIVE\` and inside its selling window. A plan that is published but **not** publicly available
is allowed — that is exactly a private quote.

### One current subscription per school

A second is a \`409\` telling you to change the plan on the existing one.

### The nine test cases are in the request body as comments
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

 IT ALSO WORKS THE OTHER WAY ROUND. A school still PROVISIONING with
 everything else already in place is waiting only on a subscription, so
 this endpoint activates it — see cases 12 to 14. The setup gates are not
 skipped: no SCHOOL_ADMIN role or missing number sequences leaves the
 school PROVISIONING, and the subscription is still created.

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
        { status: 400, code: "PERIOD_START_IN_PAST", when: "currentPeriodStart is before today in the school's zone" },
        { status: 400, code: "BILLING_PERIOD_END_REQUIRED", when: "A custom billing cycle" },
        { status: 400, code: "BILLING_PERIOD_END_NOT_ALLOWED", when: "An end date on a fixed cycle" },
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
         billingCycle    = the plan's cycle, unless the request named one
         currentPeriodStart = MIDNIGHT TODAY in the school's own zone, not
                              the moment the request arrived
         currentPeriodEnd   = start + the cycle's days (30/90/180/365),
                              and it may NOT be sent on those four cycles
         maxStudentsOverride / maxUsersOverride = COPIED FROM THE PLAN,
                              since the sale named no figures of its own
         hasLimitOverrides  = false, because they match the plan
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
          id: "05b",
          name: "SELL IT ON A DIFFERENT CADENCE",
          expect: "201 Created",
          notes: `billingCycle is optional and absent means the plan's own — this sells
    the same plan on different terms. It is stored on the SUBSCRIPTION, so
    the plan itself is unchanged and no other school on it is affected.

    On a YEARLY plan, send { "billingCycle": "QUARTERLY" }:
      OUT: billingCycle QUARTERLY, and currentPeriodEnd is start + 90 days
           rather than + 365. The period follows the cycle being SOLD.

    The refusal follows it too, which is the part worth testing both ways:
      a YEARLY plan sold "CUSTOM" with no date  -> 400
                                                   BILLING_PERIOD_END_REQUIRED
      a CUSTOM plan sold "MONTHLY" with no date -> 201, 30 days

    An unknown value is a validation error, not a silent default:
      { "billingCycle": "WEEKLY" } -> 400`,
          body: `{
  "planCode": "PREMIUM",
  "planVersion": 1,
  "billingCycle": "QUARTERLY"
}`,
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
          notes: `A FIXED COUNT OF DAYS, from the plan's cycle:
      MONTHLY 30 | QUARTERLY 90 | HALF_YEARLY 180 | YEARLY 365
    So a YEARLY plan starting 2026-04-01 ends 2027-04-01, and a MONTHLY one
    starting 31 January ends 2 March.

    EQUAL PERIODS RATHER THAN EQUAL DATES, and the drift is real: twelve
    30-day months come to 360 days, and a 365-day year through 29 February
    ends a day early. Adding calendar months instead makes every period a
    different length and lands the end on a different day of the month
    depending on where it started. Evenness was chosen.

    SEND NOTHING and the period starts at MIDNIGHT TODAY in the school's
    own zone — 06:00 in Kolkata is still yesterday in UTC, so the zone is
    what decides which day "today" is.`,
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
    Zero is refused HERE, unlike on #14 where it removes an override —
    on create there is nothing to remove, so it is a mistake like any
    other. Omitting the field copies the plan's figure instead.
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
        -> maxStudentsOverride and maxUsersOverride HOLD THE PLAN'S
           FIGURES, not null: the subscription says what the school may use
           without anybody reading the plan behind it, and a school already
           sold keeps what it bought when the plan's next version moves its
           ceiling. \`hasLimitOverrides\` in the response compares against
           the plan rather than checking for null, so it still means "this
           school's ceiling is not its plan's".
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
        {
          id: "12",
          name: "A PROVISIONING SCHOOL GOES LIVE",
          expect: "201 Created",
          notes: `A school that has had complete-provisioning run on it, so it has its
    SCHOOL_ADMIN role and all its number sequences, but was never
    activated. Send case 01 against it.
    OUT: the subscription as usual, and nextStep ends with
         "The school is now ACTIVE — a subscription was the last thing it
          needed."
    Then GET /platform/schools/{{schoolId}}:
         status "ACTIVE", activatedAt stamped.

    activatedAt is set only the FIRST time, so a school that is suspended
    and later resubscribes keeps the date it originally went live.`,
          body: null,
        },
        {
          id: "13",
          name: "A SCHOOL WHOSE SETUP IS NOT FINISHED",
          expect: "201 Created",
          notes: `A school created by POST /platform/schools with complete-provisioning
    NOT yet run. Send case 01 against it.
    OUT: the subscription IS created — activation never fails the sale —
         and nextStep carries the actual reason:
         "The school is still PROVISIONING: This school has no SCHOOL_ADMIN
          role. Run complete-provisioning first."
    A school missing sequences instead reads "This school has 3 of 12
    number sequences. Run complete-provisioning first."
    Then GET /platform/schools/{{schoolId}}: still PROVISIONING,
    activatedAt null.

    The check is the same one #3 in core uses — whyNotReadyToActivate in
    SchoolPlatformService — so the two endpoints cannot disagree about what
    "ready" means. Activating a school without these produces a live school
    that fails on first use.`,
          body: null,
        },
        {
          id: "14",
          name: "A SCHOOL THAT IS ALREADY LIVE, OR SUSPENDED",
          expect: "201 Created",
          notes: `Anything other than PROVISIONING is left alone.
    OUT: nextStep says "The school itself is SUSPENDED, which a
         subscription does not change."
    Reinstating a suspended school is #5 in core; buying something is not
    an appeal.`,
          body: null,
        },
      ],
    },
    {
      id: "edit-subscription",
      name: "Edit Subscription",
      method: "PATCH",
      path: "/platform/schools/{id}/subscriptions/current",
      status: 'live',
      summary: "",
      schoolSurface: false,
      docs: `**PATCH** \`/platform/schools/{id}/subscriptions/{no}\` — edits the terms of one subscription.

**When it runs, what state it is in, how much of the product it may use.** Seven fields:
\`status\`, \`billingCycle\`, \`currentPeriodStart\`, \`currentPeriodEnd\`, \`autoRenew\`,
\`maxStudentsOverride\`, \`maxUsersOverride\` — three endpoints' worth of single-column edits in one
request. extend-trial moved \`currentPeriodEnd\`, #23 moved \`autoRenew\`, #24 the two overrides;
three sets of rules to keep in step, and a correction touching two of them was two writes and two
history rows for one decision.

### \`reason\` is REQUIRED, and stored — not just logged

**The only field on this request that must be sent.** Every other field here changes something a
school is paying for — its status, its dates, its capacity — so an unexplained change is one
nobody can answer for months later. Blank counts as missing: \`"  "\` is refused too.

It goes onto the subscription as **\`reasonForChanges\`** — why it looks the way it does, readable
without a second query — and onto the history row beside the list of fields that moved. The
document keeps the latest; history keeps all of them.

**Every edit overwrites it.** A reason left standing from an earlier edit would explain the wrong
change. Since the field is required, an edited subscription always carries one; \`reasonForChanges\`
being null means nothing has ever edited it.

\`cancelledAt\` and \`cancellationReason\` are **gone from the model** (2026-09-07). When a
subscription was cancelled is the \`effectiveAt\` of its \`CANCELLED\` history row — a second copy on
the document could only come to disagree with it.

### Nothing about the money

\`contractedPrice\` and \`currencyCode\` are **#25**, \`billingCustomerReference\` is **#26**, the plan
is **#16**. Changing a price changes what gets invoiced, and a currency changes what money it is
invoiced in — commercial decisions with a paper trail of their own. Behind the same request as
"push the trial out a fortnight", repricing a school would look like an administrative tidy-up.

A consequence worth knowing: **nothing built can change a price after Create Subscription set
it.** #25 is not built and this will not do it.

### Use \`current\` as the number

Same reason as everywhere else here: a real number is \`SUB/2026/09/000001\`, the slashes end the
path segment, and \`%2F\` is refused by Tomcat before Spring sees it.

### It applies no transition rules, deliberately

### Only a TRIAL or ACTIVE subscription may be edited

The other four are \`409 SUBSCRIPTION_NOT_EDITABLE\`. Each was somebody's decision or a date
arriving, and each has an endpoint that owns the way out — undoing one by writing a field here
would bypass that endpoint, and the history row would say *edited* where the real event was
*resumed* or *revived*.

| Status | Editable | The way out |
|---|---|---|
| \`TRIAL\`, \`ACTIVE\` | **yes** | — |
| \`PAST_DUE\` | no | #17 renew once the bill is settled, or #16 change plan |
| \`SUSPENDED\` | no | #20 resume |
| \`CANCELLED\`, \`EXPIRED\` | no | #16 change plan, which opens a new period at \`ACTIVE\` |

The refusal names the way out for the status it refused, so it is a signpost rather than a dead
end, and nothing is written when it refuses.

**It is still a one-way door the other way**, deliberately: from \`TRIAL\` or \`ACTIVE\` this can set
any of the six — which is how \`PAST_DUE\` and \`EXPIRED\` get set by hand while no job exists — and
the next PATCH is then refused.

### It applies no transition rules

The lifecycle endpoints each know one transition and what it implies: #17 renews, #19 suspends,
#20 resumes, #21 ends. **Nothing pushes a subscription into \`PAST_DUE\` or \`EXPIRED\`** — both are
the passage of time noticing something rather than a decision, so a **job** will do them (#18 and
#22 were dropped for that reason). Until then this endpoint is the only way to set either by hand.

This writes what it is told — which is what is needed when a subscription is already wrong and no ordinary
transition describes the fix. It is not how a subscription should ordinarily be renewed or
cancelled.

Two things are refused: a missing or blank \`reason\` (\`400 VALIDATION_FAILED\`) and a period left
running backwards (\`400 INVALID_BILLING_PERIOD\`).

### Absent, null and cleared

| Sent | Means |
|---|---|
| omitted, or \`null\` | leave it exactly as it is |
| a value | replace it |
| \`0\`, on either override | remove it, and fall back to the plan's own limit |

Every field is flat. Jackson cannot tell an omitted field from one sent as \`null\`, which costs
nothing for the five \`@NotNull\` fields — they cannot be cleared at all — but would collapse "leave
this alone" and "take this away" for the two nullable overrides. So **zero is the removal**: a
school permitted no students is not a ceiling anybody negotiated, which is why Create Subscription
refuses zero. A negative override is a \`400 LIMIT_TOO_LOW\`.

### Nothing changed is not an error, nothing asked for is

A request that restates what is already stored answers \`200\` saying so, writes no history row and
stores no new reason. An **empty** request is now \`400 VALIDATION_FAILED\` naming \`reason\`, since
bean validation runs before the service — \`NO_CHANGES_REQUESTED\` is what you get when a reason
*was* given and no editable field was.

### The twelve test cases are in the request body as comments
`,
      bodyNotes: `\`reason\` IS REQUIRED on every one of these. Drop it and the request is
refused before the service sees it.

 Platform surface. Needs {{schoolId}} and a subscription — run Create
 Subscription first.

 EVERY FIELD IS OPTIONAL and absent means unchanged. What each field's
 absence means is in the description above; the cases below are what the
 endpoint actually does with them.

 THE EDITABLE FIELDS ARE SEVEN, all flat: status, billingCycle,
 currentPeriodStart, currentPeriodEnd, autoRenew, maxStudentsOverride,
 maxUsersOverride — plus \`reason\`, which is REQUIRED, is stored on the
 subscription as reasonForChanges AND written to the history row.

 NOT EDITABLE HERE: contractedPrice and currencyCode (#25),
 billingCustomerReference (#26), the plan (#16), subscriptionNo, current
 and schoolId. Sending them is ignored like any other unknown field.

 cancelledAt AND cancellationReason NO LONGER EXIST on the model. When a
 subscription was cancelled is the effectiveAt of its CANCELLED history
 row; reasonForChanges holds why it was last changed, whatever the change.

 ANSWERS THE WHOLE SUBSCRIPTION BACK, the same shape as Get Subscription,
 so there is no second call to see what it now says. \`note\` says what moved.`,
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "currentPeriodEnd": "2027-12-31T23:59:59Z",
  "reason": "Trial extended three months."
}`,
      successStatus: 200,
      responseFields: [],
      captures: [],
      errors: [
        { status: 409, code: "SUBSCRIPTION_NOT_EDITABLE", when: "The subscription is not TRIAL or ACTIVE" },
        { status: 400, code: "PERIOD_START_REQUIRED", when: "billingCycle sent with no currentPeriodStart" },
        { status: 400, code: "PERIOD_START_IN_PAST", when: "currentPeriodStart is before today in the school's zone" },
        { status: 400, code: "VALIDATION_FAILED", when: "Ask for nothing" },
        { status: 400, code: "INVALID_BILLING_PERIOD", when: "A period that runs backwards" },
        { status: 400, code: "NO_CHANGES_REQUESTED", when: "A reason on its own" },
      ],
      examples: [
        {
          id: "01",
          name: "PUSH A TRIAL'S END DATE OUT",
          expect: "200 OK",
          notes: `The body above. THIS IS WHAT EXTEND-TRIAL DID, and nothing else moves.
    OUT: currentPeriodEnd 2027-12-31, daysRemaining recounted from it.
         note: "Edited currentPeriodEnd. ..."`,
          body: null,
        },
        {
          id: "02",
          name: "ASK FOR NOTHING",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "VALIDATION_FAILED",
           "fieldErrors": { "reason": ["must not be blank"] } }
    Bean validation runs before the service, so the missing reason is what
    answers first.

02b AN EDIT WITH NO REASON                            -> 400 Bad Request
{
  "autoRenew": false
}
    OUT: the same VALIDATION_FAILED on \`reason\`. So is "reason": "   " —
    it is @NotBlank, not @NotNull.
    EVERY FIELD HERE IS SOMETHING A SCHOOL IS PAYING FOR. An unexplained
    change to any of it is one nobody can answer for months later.`,
          body: `{}`,
        },
        {
          id: "03",
          name: "SEND WHAT IS ALREADY STORED",
          expect: "200 OK",
          notes: `On a subscription whose autoRenew is already true.
    OUT: note: "Nothing changed: every field sent already held that value.
                No history row was written."
         reasonForChanges KEEPS whatever it had — an explanation for an
         edit that did not happen is not worth storing.
    "Changed" means different from what was stored, not "was mentioned in
    the request".`,
          body: `{
  "autoRenew": true,
  "reason": "This will not be stored."
}`,
        },
        {
          id: "04",
          name: "A PERIOD THAT RUNS BACKWARDS",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_BILLING_PERIOD",
           "message": "currentPeriodEnd (...) must be after
                       currentPeriodStart (...). Editing one end of a
                       period is checked against the other." }
    Checked against whichever end did NOT move, so one end cannot be
    edited past the other.`,
          body: `{
  "currentPeriodEnd": "2020-01-01T00:00:00Z"
}`,
        },
        {
          id: "05",
          name: "AN END DATE ON A FIXED CADENCE",
          expect: "400 Bad Request",
          notes: `ONLY A CUSTOM CADENCE TAKES AN END DATE. The four fixed cycles ARE
    their length, so one sent with them either agrees with the derivation —
    saying nothing — or disagrees with it, and then the record contradicts
    itself: MONTHLY beside a six-month period bills the school for half a
    year while the document says it pays monthly.

    OUT: { "code": "BILLING_PERIOD_END_NOT_ALLOWED" }

    REFUSED RATHER THAN QUIETLY DROPPED: a 200 carrying a different date
    than the one sent ignores the caller without telling them. Nothing is
    written.

    TO MOVE THE END, MOVE WHAT IT IS MEASURED FROM — send currentPeriodStart
    on its own and the end is re-derived. See the next case.

    A BARE END DATE IS REFUSED TOO, which is the case most callers try:
      { "currentPeriodEnd": "2027-03-31T23:59:59Z", "reason": "..." }

    READ AGAINST THE CADENCE AFTER THE EDIT, so (CUSTOM -> MONTHLY plus an
    end date) is refused as one request rather than accepted because CUSTOM
    was true when it arrived.`,
          body: `{
  "currentPeriodStart": "2026-04-01T00:00:00Z",
  "currentPeriodEnd": "2027-03-31T23:59:59Z",
  "reason": "Contract re-dated."
}`,
        },
        {
          id: "05b",
          name: "THE CADENCE DECIDES THE PERIOD",
          expect: "200 OK",
          notes: `THE CYCLE MOVES THE END WITH IT. An end derived as "start + 365" is not
    the end of a MONTHLY period, so leaving it would bill the school for a
    year while the document said it paid monthly.

    On a MONTHLY subscription, send { "billingCycle": "YEARLY", "reason": … }:
      OUT: billingCycle YEARLY, currentPeriodEnd = start + 365 days, and the
           note says "Edited billingCycle, currentPeriodEnd" — a derived
           change is reported, never silent.

    MOVING THE START does the same on the four fixed cycles:
      { "currentPeriodStart": "2026-10-01T00:00:00Z", "reason": … }
        -> the end becomes 2026-10-31 on a MONTHLY cadence.

    MOVING TO CUSTOM NEEDS THE DATE WITH IT:
      { "billingCycle": "CUSTOM", "reason": … }
        -> 400 BILLING_PERIOD_END_REQUIRED. CUSTOM has no length, and the
           date on record belongs to the cadence being left.
      { "billingCycle": "CUSTOM", "currentPeriodEnd": "2028-01-31T23:59:59Z" }
        -> 200.

    MOVING AWAY FROM CUSTOM needs nothing extra — the new cycle's length
    settles it.

    A CUSTOM subscription whose START moves KEEPS its end: that date was
    somebody's decision, not a derivation.

    An edit touching neither the cycle nor the start derives nothing at all.

    This reverses what this endpoint used to do, which was to leave the dates
    alone on a cadence change. Reporting the derived field in the changed
    list is what answers the original worry about a silent re-bill.`,
          body: `{
  "billingCycle": "YEARLY",
  "reason": "Moved to annual billing."
}`,
        },
        {
          id: "06",
          name: "RAISE THE NEGOTIATED LIMITS",
          expect: "200 OK",
          notes: `OUT: maxStudents 2500, hasLimitOverrides true. The response reports the
         limit IN FORCE, so no caller works out which one applies.`,
          body: `{
  "maxStudentsOverride": 2500,
  "maxUsersOverride": 300,
  "reason": "Negotiated up at renewal."
}`,
        },
        {
          id: "07",
          name: "RAISE ONE, LEAVE THE OTHER",
          expect: "200 OK",
          notes: `OUT: the students override moves; the users override is untouched.
    Omitting a field leaves it. There is no way to say "leave it" and
    "clear it" with the same absent value, which is what case 08 is about.`,
          body: `{
  "maxStudentsOverride": 4000,
  "reason": "More intake than expected."
}`,
        },
        {
          id: "08",
          name: "TAKE ONE OVERRIDE AWAY",
          expect: "200 OK",
          notes: `ZERO IS THE REMOVAL. OUT: maxStudentsOverride null, and maxStudents
    back to the PLAN's own limit. The users override is untouched.

    Why zero: Jackson hands the server null both for a field that was
    omitted and for one sent as null, so "leave this alone" and "take this
    away" arrive identical. Zero is free to mean the second because it
    cannot mean anything else — a school permitted no students is not a
    ceiling anybody negotiated, which is why Create Subscription refuses 0.

    A NEGATIVE OVERRIDE IS STILL REFUSED:
      { "maxUsersOverride": -5 }  ->  400 LIMIT_TOO_LOW
    That is a typo, not an instruction.`,
          body: `{
  "maxStudentsOverride": 0,
  "reason": "Back to the plan's own limit."
}`,
        },
        {
          id: "09",
          name: "CANCEL IT",
          expect: "200 OK",
          notes: `OUT: status CANCELLED, reasonForChanges "School closed mid-year."
         NO DATE IS STAMPED on the document — when it happened is the
         effectiveAt of the history row this same request writes.
    History: eventType CANCELLED, previousStatus ACTIVE.

    NO TRANSITION RULES APPLY. All six statuses are accepted from any
    other, because this is the override for a subscription that is already
    wrong. Suspending properly is #19, resuming is #20.`,
          body: `{
  "status": "CANCELLED",
  "reason": "School closed mid-year."
}`,
        },
        {
          id: "10",
          name: "THE NEXT EDIT REPLACES THE STORED REASON",
          expect: "200 OK",
          notes: `Straight after case 09.
    OUT: reasonForChanges is the NEW sentence, not "School closed
         mid-year." Each edit explains itself; the trail of all of them is
         the history.`,
          body: `{
  "autoRenew": false,
  "reason": "Auto-renewal switched off at the school's request."
}`,
        },
        {
          id: "11",
          name: "A REASON ON ITS OWN",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NO_CHANGES_REQUESTED" }
    A reason is not a change. This is the ONLY way to see that code now:
    an empty body fails validation on \`reason\` first.`,
          body: `{
  "reason": "Just making a note."
}`,
        },
        {
          id: "12",
          name: "ALL SEVEN AT ONCE, ONE HISTORY ROW",
          expect: "200 OK",
          notes: `OUT: ONE history row for the whole edit, because it was one decision.
         note names the cycle now disagreeing with the plan's — reported,
         not corrected: billing a school monthly on a yearly plan is a real
         arrangement, and rewriting it would undo a deliberate change.

    From mongosh afterwards:
      db.subscription_history.find({schoolSubscriptionDocsId: "..."})
                             .sort({createdAt: 1})
        -> TERMS_CHANGED  TRIAL     -> TRIAL     | Edited currentPeriodEnd.
                                                   Trial extended three
                                                   months.
           CANCELLED      TRIAL     -> CANCELLED | Edited status. School
                                                   closed mid-year.
           RESUMED        SUSPENDED -> ACTIVE    | Edited status.

    reasonForChanges IS NOT IN THE FIELD LIST, though it moves on every
    edit — it would otherwise sit in every row next to the reason itself.

    TERMS_CHANGED was added for this endpoint — the enum had only status
    moves, so an edit either went unrecorded or borrowed a type that says
    something untrue. A status move made here writes the type that names
    it, so a suspension recorded through this endpoint and one recorded
    through #19 read identically. PLAN_CHANGED is never written here: this
    endpoint cannot move the plan.

    THE reason ON THE ROW IS THE FIELD LIST PLUS THE CALLER'S WORDS,
    always. Months later "the dates changed" is the question and "which
    fields moved" is the answer; "renegotiated" alone does not say what.

    NO ROW AT ALL when nothing changed (case 03).`,
          body: `{
  "status": "PAST_DUE",
  "billingCycle": "MONTHLY",
  "currentPeriodStart": "2026-04-01T00:00:00Z",
  "currentPeriodEnd": "2027-03-31T23:59:59Z",
  "autoRenew": false,
  "maxStudentsOverride": 4000,
  "maxUsersOverride": 400,
  "reason": "Contract renegotiated, invoice overdue."
}`,
        },
      ],
    },
    {
      id: "change-plan",
      name: "Change Plan",
      method: "POST",
      path: "/platform/schools/{id}/subscriptions/current/change-plan",
      status: 'live',
      summary: "Moves a school onto a different plan or version. Immediate, and the period restarts with it.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/subscriptions/{no}/change-plan\` — moves a school onto a different plan.

**What #14 cannot do.** #14 edits the terms of the plan a school is already on; this changes which
plan that is, and with it the feature access, the price and the billing cycle. Two endpoints, because
"push the trial out a fortnight" and "move them to Enterprise" are not the same request.

### It writes two rows, and does not edit one

The row the school is leaving is **closed** — \`current\` becomes false and its \`currentPeriodEnd\`
is trimmed to the day of the change, because that is the period it actually served. Its status is
NOT touched: it was superseded, not expired and not cancelled.

A **new row is inserted** for the plan it moves onto, with a \`subscriptionNo\` of its own from the
number sequence. So \`school_subscriptions\` holds one row per plan period, and exactly one has
\`current = true\` — which is the row every read answers with.

The old row is closed *before* the new one is inserted, because the unique partial index on
\`{schoolId, current}\` permits one current row per school.

### The change is immediate

There is no timing field. A subscription holds **one** plan, not a current one and a pending one,
so a change scheduled for a future period would have nowhere to live — and moving the pointer now
while calling it "next period" would hand the school its new feature access early. So the plan
changes when you send this, and the billing period restarts with it, on the **new** plan's cycle.

### The cadence and the period

\`currentPeriodStart\` is **required** and has to be **today or later**. It used to be derived as
"today" and is now stated, because it decides two dates rather than one: the anchor the new
period's end is measured from, and the instant the row being left stops serving. The closed row's
end is set to it, so the two periods meet — trimming the old one ordinarily, extending it when the
start is dated later.

\`billingCycle\` is optional and absent takes the **new plan's** cadence. Send one to move the
school on at a different cadence, stored on the subscription rather than the plan. Whichever
applies decides the period, and therefore whether \`currentPeriodEnd\` is required: a \`CUSTOM\`
plan needs an end date, so does a \`YEARLY\` plan billed \`CUSTOM\`, and a \`CUSTOM\` plan billed
\`MONTHLY\` needs none.

A future \`currentPeriodStart\` does **not** delay the plan change — the pointer still moves now.
It moves when the new billing period opens.

### Every status may change plan, and the new row is always ACTIVE

**No status is refused, and none is carried forward.** A plan change is somebody buying this school
a plan, so the row it lands on has to be one the school can use — carrying \`TRIAL\`, \`SUSPENDED\`,
\`PAST_DUE\` or \`CANCELLED\` onto a plan just bought would sell it something it cannot reach.

| The old row was | The new row is |
|---|---|
| \`ACTIVE\` | \`ACTIVE\` |
| \`TRIAL\` | \`ACTIVE\` — **the conversion path**; a trial that starts paying is a plan change, which is why #15 was withdrawn |
| \`PAST_DUE\`, \`SUSPENDED\` | \`ACTIVE\` |
| \`CANCELLED\`, \`EXPIRED\` | \`ACTIVE\` — a finished subscription is how a school comes back |

\`CANCELLED\` and \`EXPIRED\` used to be \`409 SUBSCRIPTION_NOT_CHANGEABLE\` for having "nothing to
move", which mistook what this endpoint does: it never edits the row it is given, it retires that
row and opens a new one, so the state the old row ended in does not constrain the new one.

It also **agrees with what this endpoint does to the school**, which it takes \`ACTIVE\` either way.
This used to leave a \`SUSPENDED\` subscription suspended while doing that — a state nothing could
act on sensibly.

\`autoRenew\` is **carried across untouched** on every plan change, revival included: it is the
school's standing instruction, and absent means "leave it as it is".

One consequence: #21 turns the flag off on its way out, so a revived subscription inherits
\`autoRenew: false\` unless the request names it. Nothing acts on the flag, but the school's own
billing view (#33) reads it and will say a subscription just bought does not renew. Send
\`autoRenew: true\` with the revival if it should say otherwise — the \`note\` points that out when
the flag comes across off.

### A closed period only ever shrinks

The retired row's \`currentPeriodEnd\` moves to the handover **only when that is earlier than the
end it already has**. One rule, and the DATES decide it rather than the status:

| The closed row's stored end | What happens |
|---|---|
| after the handover — still serving | **trimmed** to the handover |
| at or before it — already stopped | **left alone**; stretching it forward would claim it covered a gap the school was on nothing for |

Keying on the status got the middle case wrong: a *scheduled* cancellation is \`CANCELLED\` with an
end still in the future, so it really was serving until the handover and does need trimming.

One edge: \`currentPeriodStart\` may be earlier than when the old row stopped — midnight today
against a cancellation at 09:35. The closed row is then shortened to the handover, because the
alternative is two overlapping periods.

The history row records the move: \`previousStatus\` is whatever the old row was and \`newStatus\` is
\`ACTIVE\`. They are equal only when the school was already \`ACTIVE\`.

### It asks nothing about the money, and moves none

The school is part-way through a period it has paid for, and this endpoint **charges, credits and
refunds nothing** — because nothing in this codebase raises an invoice: \`subscription_invoices\` has
no writer and #17 is not built.

It does not ask what *should* happen to that money either. That is a commercial decision belonging
to whatever eventually raises the invoice, and the module README still carries it as an open
question. The response says outright that no money moved, so a plan change is never read as a
payment.

### What follows the plan, and what survives it

| | |
|---|---|
| from the new plan | \`billingCycle\` and \`currencyCode\`, always |
| from the new plan | the price and both capacity ceilings, **unless you send them** |
| kept as it is | \`autoRenew\`, **unless you send it** |
| kept as it is | \`status\`, \`billingCustomerReference\`, \`subscriptionNo\` |

**Two kinds of absence.** A price or ceiling you leave out takes the **new plan's** figure. An
\`autoRenew\` you leave out keeps **the school's** setting — a plan has no opinion about renewal, so
defaulting it to \`true\` would switch it back on for the one school that asked for it off.

**Nothing negotiable is carried across on its own.** A discount and a raised ceiling are agreed
against a *particular* plan, so a move means those terms are agreed again — send
\`contractedPrice\`, \`maxStudentsOverride\` and \`maxUsersOverride\` to continue them.

### Use \`current\` as the number

Same reason as everywhere else here: a real number is \`SUB/2026/09/000001\`, the slashes end the
path segment, and \`%2F\` is refused by Tomcat before Spring sees it.

### The nine test cases are in the request body as comments
`,
      bodyNotes: `Platform surface. Needs {{schoolId}} on a subscription — run Create
 Subscription first, ideally on STARTER_PLAN so there is somewhere to go.

 REQUIRED: planCode, planVersion, reason.
 OPTIONAL: contractedPrice, maxStudentsOverride, maxUsersOverride,
           autoRenew, currentPeriodEnd.

 THERE IS NO TIMING FIELD. The change is immediate and the period restarts
 with it. A subscription holds one plan, not a current one and a pending
 one, so a scheduled change would have nowhere to live.

 THERE IS NO MONEY FIELD EITHER. Nothing raises invoices, so nothing is
 charged, credited or refunded for the period already paid for — and what
 SHOULD happen to it is a commercial question left open on purpose.`,
      requiredFields: ["planCode", "planVersion", "reason"],
      optionalFields: ["contractedPrice", "maxStudentsOverride", "maxUsersOverride", "autoRenew", "currentPeriodEnd"],
      pathParams: [
        { name: "id", value: "{{schoolId}}", description: "The school's MongoDB id. Create School fills this in." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "planCode": "PREMIUM",
  "planVersion": 1,
  "reason": "Outgrew Starter's 500 students."
}`,
      successStatus: 200,
      responseFields: ["subscriptionId", "subscriptionNo", "schoolId", "planDefinitionDocsId", "planCode", "planVersion", "planName", "planStatus", "planRetired", "status", "billingCycle", "currentPeriodStart", "currentPeriodEnd", "daysRemaining", "periodEnded", "autoRenew", "current", "contractedPrice", "planListPrice", "currencyCode", "hasDiscount", "maxStudents", "maxUsers", "maxStudentsOverride", "maxUsersOverride", "hasLimitOverrides", "featureCount", "features", "reasonForChanges", "billingCustomerReference", "note"],
      captures: [],
      errors: [
        { status: 400, code: "PERIOD_START_IN_PAST", when: "currentPeriodStart is before today in the school's zone" },
        { status: 400, code: "BILLING_PERIOD_END_REQUIRED", when: "A CUSTOM cadence with no currentPeriodEnd" },
        { status: 400, code: "BILLING_PERIOD_END_NOT_ALLOWED", when: "An end date on a fixed cadence" },
        { status: 400, code: "VALIDATION_FAILED", when: "A reason is required" },
        { status: 400, code: "BILLING_PERIOD_END_REQUIRED", when: "A custom target with no end date" },
        { status: 400, code: "BILLING_PERIOD_END_NOT_ALLOWED", when: "An end date on a fixed target cadence" },
        { status: 409, code: "PLAN_UNCHANGED", when: "The plan it is already on" },
        { status: 409, code: "PLAN_NOT_SELLABLE", when: "A plan that cannot be sold" },
      ],
      examples: [
        {
          id: "01",
          name: "THE ORDINARY UPGRADE",
          expect: "200 OK",
          notes: `The body above, on a school currently on STARTER_PLAN.
    OUT: A NEW subscriptionNo — the response is the row the school moved
         ONTO, not the one it left. The old row is still in the
         collection with current=false.
         planCode PREMIUM, contractedPrice 49999 (the new plan's list),
         billingCycle and currencyCode from the new plan,
         maxStudents/maxUsers 2000/250 — PREMIUM's own, because the
         request named no ceilings,
         currentPeriodStart MIDNIGHT TODAY, currentPeriodEnd + 365 days,
         reasonForChanges = your reason.
         note: "Upgraded from 'STARTER_PLAN' version 1 to 'PREMIUM'
                version 1. ... NO money has moved for the period the
                school had already paid for ... What should happen to it
                is still an open question."`,
          body: null,
        },
        {
          id: "02",
          name: "A REASON IS REQUIRED",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "VALIDATION_FAILED",
           "fieldErrors": { "reason": ["must not be blank"] } }
    @NotBlank, so "  " is refused too. A plan change moves what a school
    is entitled to and what it pays; an unexplained one is the hardest
    record to answer questions about later.`,
          body: `{
  "planCode": "PREMIUM", "planVersion": 1
}`,
        },
        {
          id: "03",
          name: "NAME THE CEILINGS, OR GET THE NEW PLAN'S",
          expect: "200 OK",
          notes: `OUT: maxStudents 4000 / maxUsers 400, against STARTER_PLAN's own
         500/50. hasLimitOverrides true.

    A NEGOTIATED CEILING DOES NOT SURVIVE A MOVE ON ITS OWN. Raise one
    first with Edit Subscription:
      PATCH .../subscriptions/current
      { "maxStudentsOverride": 9000, "reason": "Negotiated up." }
    then move WITHOUT naming ceilings, and the school lands on the new
    plan's 500/50 — the 9000 is gone. Same rule as Create Subscription:
    a ceiling is agreed against a plan, and this is a different plan.

    Send only one and the other still comes from the plan: 3000 students
    with no maxUsersOverride gives 3000/250 on PREMIUM.

    ZERO IS REFUSED HERE — 400 LIMIT_TOO_LOW. There is nothing to remove
    on a plan change; removing an override is Edit Subscription, where 0
    means exactly that.`,
          body: `{
  "planCode": "STARTER_PLAN",
  "planVersion": 1,
  "maxStudentsOverride": 4000,
  "maxUsersOverride": 400,
  "reason": "Downsized plan, negotiated headcount."
}`,
        },
        {
          id: "04",
          name: "CARRY A DISCOUNT ACROSS",
          expect: "200 OK",
          notes: `OUT: contractedPrice 39999.50 next to planListPrice 49999.00,
         hasDiscount true.
    A DISCOUNT IS NOT CARRIED OVER AUTOMATICALLY. It was agreed against a
    plan at a price, and this is a different plan at a different price, so
    continuing it silently would invent a deal nobody made.

04b AUTO-RENEWAL IS LEFT ALONE UNLESS YOU SAY            -> 200 OK
    Turn it off first with Edit Subscription:
      PATCH .../subscriptions/current
      { "autoRenew": false, "reason": "School asked." }
    then move plan WITHOUT the field:
{
  "planCode": "PREMIUM", "planVersion": 1,
  "reason": "Upgrade, renewal untouched."
}
    OUT: autoRenew STILL false. This is the one field whose absence means
    "leave it alone" rather than "take the new plan's" — a plan has no
    opinion about renewal, and defaulting to true the way Create
    Subscription does would switch it back on for the one school that had
    asked for it off.

    Send "autoRenew": true or false to set it as part of the move.`,
          body: `{
  "planCode": "PREMIUM",
  "planVersion": 1,
  "contractedPrice": 39999.50,
  "reason": "Partner discount continues on the new plan."
}`,
        },
        {
          id: "05",
          name: "THE PLAN IT IS ALREADY ON",
          expect: "409 Conflict",
          notes: `OUT: { "code": "PLAN_UNCHANGED",
           "message": "... is the plan this subscription is already on. To
                       change its terms rather than its plan, use the edit
                       endpoint." }
    A NEWER VERSION of the same code IS allowed — that is how a school
    moves to v2 of what it is on.`,
          body: `{
  "planCode": "PREMIUM", "planVersion": 1, "reason": "x"
}`,
        },
        {
          id: "06",
          name: "A PLAN THAT CANNOT BE SOLD",
          expect: "409 Conflict",
          notes: `A DRAFT or RETIRED target.
    OUT: { "code": "PLAN_NOT_SELLABLE" }
    A published plan that is NOT publicly available is allowed: that is a
    private quote, and moving a school onto one is exactly the use.`,
          body: null,
        },
        {
          id: "07",
          name: "A CUSTOM TARGET WITH NO END DATE",
          expect: "400 Bad Request",
          notes: `Moving to a plan whose billingCycle is CUSTOM:
    OUT: { "code": "BILLING_PERIOD_END_REQUIRED" }
    Send one, and it is used as-is:
    IT IS THE NEW PLAN'S CYCLE THAT DECIDES, not the old one. A school
    moving from a yearly plan to a monthly one gets 30 days from today.`,
          body: `{
  "planCode": "{{planCode}}", "planVersion": 1,
  "currentPeriodEnd": "2027-06-30T23:59:59Z",
  "reason": "Bespoke term."
}`,
        },
        {
          id: "08",
          name: "A FINISHED SUBSCRIPTION COMES BACK",
          expect: "200 OK",
          notes: `On a CANCELLED or EXPIRED subscription. This used to be
    409 SUBSCRIPTION_NOT_CHANGEABLE, which mistook what the endpoint does: it
    never edits the row it is given, it retires that row and opens a new one,
    so the state the old row ended in does not constrain the new one.

    Cancel a subscription (immediate: true), then send this.
    OUT: 200. The new row is ACTIVE — true of EVERY plan change, not just a
         revival: TRIAL, SUSPENDED, PAST_DUE and CANCELLED all come out
         ACTIVE, because a plan change is somebody buying this school a plan.

         Two things are specific to reviving a FINISHED row:
           autoRenew   true, unless the request names it — #21 turned it off
                       on its way out, and carrying that forward would apply
                       half of a decision this request reverses
           the closed row's currentPeriodEnd  LEFT ALONE, not trimmed to the
                       new start: it really did stop when it was cancelled,
                       and moving it forward would claim it covered a gap the
                       school was on nothing for

    CHECK: db.subscription_history.findOne({ eventType: "PLAN_CHANGED" })
             -> previousStatus CANCELLED, newStatus ACTIVE. The only place a
                plan change moves the status.
           GET /schools/current/subscription/feature-access
             -> active false before, TRUE after.

    An EXPIRED one comes back the same way. Every other status still carries
    over untouched: TRIAL stays TRIAL, SUSPENDED stays SUSPENDED.

09  WHAT LANDS IN THE DATABASE
    TWO ROWS PER CHANGE. From mongosh, after a sale and two changes:
      db.school_subscriptions.find({schoolId: "..."}).sort({createdAt:1})
        -> SUB/2026/09/000001 STARTER_PLAN current false  (closed)
           SUB/2026/09/000002 PREMIUM      current false  (closed)
           SUB/2026/09/000003 STARTER_PLAN current TRUE   (live)
      Exactly one current=true, always. A closed row keeps its status:
      superseded is not expired and not cancelled.

    From mongosh:
      db.subscription_history.find({schoolSubscriptionDocsId: "..."})
                             .sort({createdAt: 1})
        -> PLAN_CHANGED, with BOTH plan ids on the row:
           previousPlanDefinitionDocsId and newPlanDefinitionDocsId, and
           previousStatus == newStatus, because a plan change is not a
           status move.
           reason: "Moved from 'STARTER_PLAN' version 1 to 'PREMIUM'
                    version 1, immediately. Outgrew Starter's 500
                    students."

    db.subscription_invoices IS NOT TOUCHED. Nothing writes it yet, which
    is why there is no money field on the request.`,
          body: null,
        },
      ],
    },
    {
      id: "renew-subscription",
      name: "Renew Subscription",
      method: "POST",
      path: "/platform/schools/{id}/subscriptions/current/renew",
      status: 'live',
      summary: "Starts the next billing period on identical terms. No request body.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/subscriptions/current/renew\` — starts the next billing period.

Normally the nightly job would call this; an operator calls it by hand when something went wrong.
**Nothing calls it on a schedule yet**, so today it is only ever called by hand.

### The ordinary renewal sends no body

A renewal is the same plan at the same price for the next period. The plan, the version, the price,
the currency, both capacity ceilings, the cycle, \`autoRenew\` and the billing customer reference
all carry across **untouched** — including negotiated ones, because nobody agreed to renegotiate
anything by renewing. That is the opposite of #16, where a different plan means different terms.

Anything that could change one of those would make it a change rather than a renewal, and changes
have their own endpoints: **#14** for the terms, **#16** for the plan.

**One field exists, for the one case that cannot be derived.** A \`CUSTOM\` cycle has no length, so
\`currentPeriodEnd\` says when the next period ends — **required** there, and **refused** on the
four fixed cycles (\`400 BILLING_PERIOD_END_NOT_ALLOWED\`), which decide their own length. So
omitting the body is not merely the ordinary renewal; on those four it is the only call.

### It writes two rows, the same way #16 does

The period that just ended is **closed** — \`current\` becomes false — and a new row is inserted
with a \`subscriptionNo\` of its own. So \`school_subscriptions\` holds one document per billing
period rather than one per school.

The closed row's **dates are left alone**, which is the difference from #16: it ran its full course,
where a plan change trims the old row's end to the day the school left. Its status is not touched
either — it was renewed, not expired and not cancelled.

### The periods are contiguous, and each call advances exactly one

The new period starts at the **old period's end**, not today — so there is no day the school was
live but unbilled, and none it was billed twice for.

A subscription several periods behind catches up **one call at a time**:

\`\`\`
SUB/…/000001   ran to 2026-08-01           closed
SUB/…/000002   2026-08-01 → 2026-08-31     closed by the next call
SUB/…/000003   2026-08-31 → 2026-09-30     current, period still running
                                            -> a further call is 409 PERIOD_NOT_ENDED
\`\`\`

### The plan has to still be current

A renewal commits the school to the **same plan** for another period, so the plan it is on has to
be one a school can still be put on. Retired, back to \`DRAFT\`, or outside its
\`effectiveFrom\`/\`effectiveUntil\` window is \`409 PLAN_NOT_RENEWABLE\`, and the message points at
**#16** — the school is already on this plan, so the only fix is moving it to one that is current.

A **private** plan (\`publiclyAvailable: false\`) renews like any other: that is a negotiated quote,
not an invalid state.

This is deliberately not the check \`loadSellablePlan\` does for a sale. That one advises "publish
it first" or "pick one still on the menu", which is the wrong answer for a school already on the
plan.

### What renews

| Status | Renew | Result |
|---|---|---|
| \`ACTIVE\` | yes | stays \`ACTIVE\` |
| \`PAST_DUE\` | yes | becomes \`ACTIVE\` — but the outstanding payment is NOT settled |
| \`EXPIRED\` | yes | becomes \`ACTIVE\`; the case this endpoint exists to repair |
| \`TRIAL\` | \`409\` | no agreed next-period price. Extend with #14, convert with #16 |
| \`SUSPENDED\` | \`409\` | would bill for a period the school cannot use |
| \`CANCELLED\` | \`409\` | deliberately ended |

**\`autoRenew\` is not checked, and refuses nothing.** Nothing calls this endpoint on a schedule, so
every renewal is an operator deciding to renew this school now — refusing that over a flag would
only mean editing the flag first to get past the endpoint. It is still carried onto the new row,
and #33 still tells a school its subscription does not renew automatically.

A \`CUSTOM\` cycle is **asked when the next period ends**, not refused: it has no length, so send
\`currentPeriodEnd\` or get \`400 BILLING_PERIOD_END_REQUIRED\`. The date is never guessed — a
fallback would put a date nobody signed off into a billing record — and it has to be after the new
period's start, or \`400 INVALID_BILLING_PERIOD\`.

**On the other four cadences the field is refused, not overridden.** They are their own length, so
a renewal running to a date the cadence disagrees with would bill a school for a period its own
record denies: \`400 BILLING_PERIOD_END_NOT_ALLOWED\`.

### No invoice is raised, and no money is taken

\`subscription_invoices\` has no repository and no writer anywhere in the codebase, and its fields
— \`subTotal\`, \`taxAmount\`, \`dueDate\`, the invoice number — are commercial decisions rather
than something this endpoint can derive from a plan's price. So this **moves the billing period and
records the renewal; it does not charge for it**, and the \`note\` says so on every response.

It also does **not** touch the school's own status, unlike #16: a renewal continues an arrangement
rather than starting one, so there is nothing about it that should take a school live.`,
      pathParams: [
        { name: "id", value: "{{createdSchoolId}}", note: "The school's id." },
      ],
      requestFields: [
        { name: "currentPeriodEnd", required: "on a CUSTOM cycle, and refused on every other",
          note: "When the next period ends. Required on CUSTOM, which has no length to derive from. On the four fixed cycles the cycle decides — 30, 90, 180 or 365 days from where the last period ended — and sending one is 400 BILLING_PERIOD_END_NOT_ALLOWED, so there the ordinary renewal needs no body at all and no other call is possible." },
      ],
      responseFields: ["subscriptionNo", "planCode", "planVersion", "status", "billingCycle",
        "currentPeriodStart", "currentPeriodEnd", "contractedPrice", "planListPrice",
        "currencyCode", "maxStudents", "maxUsers", "hasLimitOverrides", "reasonForChanges",
        "note"],
      errors: [
        { status: 409, code: "PERIOD_NOT_ENDED", when: "The period is still running" },
        { status: 409, code: "SUBSCRIPTION_NOT_RENEWABLE", when: "TRIAL, SUSPENDED or CANCELLED" },
        { status: 409, code: "PLAN_NOT_RENEWABLE", when: "The plan is retired, DRAFT, or outside its window" },
        { status: 400, code: "BILLING_PERIOD_END_REQUIRED", when: "A CUSTOM cycle, no date sent" },
        { status: 400, code: "BILLING_PERIOD_END_NOT_ALLOWED", when: "A date sent on a fixed cycle" },
        { status: 400, code: "INVALID_BILLING_PERIOD", when: "A date not after the period's start" },
        { status: 409, code: "SCHOOL_NOT_RENEWABLE", when: "The school is being wound down" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No such school" },
        { status: 404, code: "SUBSCRIPTION_NOT_FOUND", when: "The school has no subscription" },
      ],
      examples: [
        {
          name: "1 — the happy path, on a period that has ended",
          notes: `There is no body. The only setup needed is a subscription whose period is in the
    past, because a live period is refused.

    From mongosh:
      db.school_subscriptions.updateOne(
        { schoolId: "<id>", current: true },
        { $set: { currentPeriodEnd: new Date("2026-08-01T00:00:00Z") } })

    NOTE: use new Date(...), not { $date: ... } — extended JSON inside a
    mongosh --eval is stored as a literal subdocument and then fails to map
    back to Instant, which shows up as a 500 on the next read.

    Then send this with no body.

    -> 200, and a NEW subscriptionNo. Check the collection:
       db.school_subscriptions.find({ schoolId: "<id>" })
         -> two rows, exactly one with current: true
         -> the closed row keeps its own currentPeriodEnd and its status
         -> the new row's currentPeriodStart == the closed row's
            currentPeriodEnd, exactly`,
          body: null,
        },
        {
          name: "2 — the terms carry across untouched",
          notes: `Sell a subscription with negotiated terms first, so there is something to
    carry: contractedPrice, maxStudentsOverride and maxUsersOverride all
    different from the plan's own figures.

    Renew it, and compare the response with what Get Subscription said
    before:

      planCode, planVersion, billingCycle, contractedPrice, currencyCode,
      maxStudents, maxUsers, maxStudentsOverride, maxUsersOverride,
      hasLimitOverrides, autoRenew
        -> all identical

      subscriptionNo, currentPeriodStart, currentPeriodEnd
        -> the only three that move

    A renewal is NOT a renegotiation: hasLimitOverrides stays true, and a
    discount stays a discount. #16 is the endpoint where terms are agreed
    again, because a different plan means different terms.`,
          body: null,
        },
        {
          name: "3 — a period still running is refused",
          notes: `Send it against a freshly sold subscription, with no setup at all.

    -> 409 PERIOD_NOT_ENDED, naming the date it runs to.

    Renewing early would insert a current row whose period starts in the
    future, and every read of "what is this school on" would then have to
    explain a subscription that has not begun. To move that date, use
    Edit Subscription.`,
          body: null,
        },
        {
          name: "4 — TRIAL, SUSPENDED and CANCELLED are refused; PAST_DUE and EXPIRED are not",
          notes: `For each, set the status and put the period in the past:
      db.school_subscriptions.updateOne(
        { schoolId: "<id>", current: true },
        { $set: { status: "TRIAL",
                  currentPeriodEnd: new Date("2026-08-01T00:00:00Z") } })

    TRIAL      -> 409 SUBSCRIPTION_NOT_RENEWABLE
    SUSPENDED  -> 409 SUBSCRIPTION_NOT_RENEWABLE
    CANCELLED  -> 409 SUBSCRIPTION_NOT_RENEWABLE

    PAST_DUE   -> 200, and the new row's status is ACTIVE. The note says the
                  outstanding payment is NOT settled by this.
    EXPIRED    -> 200, and the new row's status is ACTIVE. This is the case
                  the endpoint exists to repair.

    All three renewable statuses come out ACTIVE: an EXPIRED row whose new
    period has just started would contradict its own dates.`,
          body: null,
        },
        {
          name: "5 — a CUSTOM cycle is asked for the date",
          notes: `Sell a plan whose billingCycle is CUSTOM (Create Subscription requires
    currentPeriodEnd for one), then put that end date in the past.

    Send with NO body:
      -> 400 BILLING_PERIOD_END_REQUIRED, naming currentPeriodEnd. An empty
         body and an explicit null do the same — Jackson cannot tell them
         apart, and all three mean "no date was given".

    Then send the body on the right, with a date AFTER the day the previous
    period ended:
      -> 200. currentPeriodStart is the old period's end, currentPeriodEnd is
         exactly the date you sent, and billingCycle is still CUSTOM.

    A date on or before the start:
      -> 400 INVALID_BILLING_PERIOD, and nothing is written. A period that
         ended before it began is one no school was ever on.

    The date is never guessed. Falling back to a year, or to the length of the
    last period, would put a date nobody signed off into a billing record.`,
          body: { currentPeriodEnd: "2027-03-31T23:59:59Z" },
        },
        {
          name: "6 — a plan that is no longer current is refused",
          notes: `Publish a plan, sell it, put the period in the past, then retire the
    plan (Retire Plan) and renew.

    -> 409 PLAN_NOT_RENEWABLE, and the message names the change-plan endpoint:
       "'X' version 1 has been retired, so SUB/... cannot be renewed onto it
        for another period. Move this school to a current plan with the
        change-plan endpoint instead."

    Same for a plan put back to DRAFT, one past effectiveUntil, and one whose
    effectiveFrom is still in the future. Nothing is written on any of them:
      db.school_subscriptions.countDocuments({ schoolId: "<id>" })  -> still 1

    NOT refused: publiclyAvailable false. A private plan is a negotiated
    quote, not an invalid state, and a school on one renews like any other.

    A renewal re-commits the school to the SAME plan, which is why this is
    checked here and not on #16 — there, the plan being left can be anything,
    because the school is leaving it.`,
          body: null,
        },
        {
          name: "7 — a school being wound down is refused",
          notes: `db.schools.updateOne({ _id: ObjectId("<id>") },
                           { $set: { status: "CLOSED" } })

    -> 409 SCHOOL_NOT_RENEWABLE

    Same for OFFBOARDING, DELETION_PENDING and DELETED. The same allow-list
    Change Plan uses — PROVISIONING, ACTIVE, SUSPENDED — for a plainer
    reason: do not start a new billing period for a customer who is leaving.

    Check nothing was written:
      db.school_subscriptions.countDocuments({ schoolId: "<id>" })
        -> still 1`,
          body: null,
        },
        {
          name: "8 — catching up several periods, one call at a time",
          notes: `Put the period end far enough back that two whole cycles have passed, then
    renew repeatedly.

    Each call advances exactly ONE period, and each new row is a real record
    of a real period rather than one row pretending to cover the whole gap.
    Keep going and the chain stops on its own:

      -> 409 PERIOD_NOT_ENDED, once the current period reaches the future

      db.school_subscriptions.countDocuments({ schoolId: "<id>" })
        -> one row per period, exactly one with current: true

    And the history:
      db.subscription_history.find({ schoolId: "<id>", eventType: "RENEWED" })
        -> one row per renewal, each with previousPlanDefinitionDocsId ==
           newPlanDefinitionDocsId. That sameness is what tells a renewal
           from a plan change in a list of history rows.

    db.subscription_invoices IS NOT TOUCHED, and there is no repository for
    it. The note on every response says no invoice was raised.`,
          body: null,
        },
      ],
    },
    {
      id: "suspend-subscription",
      name: "Suspend Subscription",
      method: "POST",
      path: "/platform/schools/{id}/subscriptions/current/suspend",
      status: 'live',
      summary: "Cuts a school off for non-payment. Moves the subscription AND the school.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/subscriptions/current/suspend\` — cuts a school off.

### Why this is not #14 writing a status

#14 *can* put \`SUSPENDED\` in the status field, and that is the problem: cutting a school off stops
its staff working, and it should not be reachable by the same request that pushes a date out. This
knows one transition, refuses everything else, and carries the school's own access with it.

\`{"status": "SUSPENDED"}\` on #14 moves the field and nothing else — no school status, no
\`SUSPENDED\` history event, none of the refusals below. Use that to correct a record, not to cut a
school off.

### It moves two documents, because one would stop nothing

| | What it does |
|---|---|
| \`school_subscriptions.status\` = \`SUSPENDED\` | turns **every feature** off — #34 reads it and answers \`allowed: false\` on all of them |
| \`schools.status\` = \`SUSPENDED\` | blocks **the tenant** — \`requireUsable()\` reads it, so school-surface writes answer \`409 SCHOOL_NOT_EDITABLE\` |

Only an \`ACTIVE\` school's status moves. A \`PROVISIONING\` one was never usable, and one already
\`SUSPENDED\` keeps the \`suspendedAt\` and reason it has. The \`note\` says which happened.

### What it refuses

| Status | Suspend |
|---|---|
| \`ACTIVE\`, \`PAST_DUE\` | allowed |
| \`SUSPENDED\` | \`409\` — already suspended |
| \`TRIAL\` | \`409\` — no unpaid bill behind a trial |
| \`CANCELLED\`, \`EXPIRED\` | \`409\` — ended rather than paused |

Refusing a trial is what keeps **#20** simple: resume can go straight to \`ACTIVE\` without looking
up what the status used to be.

### What it does NOT stop

Nothing kills the school's live sessions and nothing halts its scheduled jobs — neither exists yet.
A user already signed in is refused at the next request that checks, not thrown out mid-page.

**No date lands on the subscription.** When it happened is the history row's \`effectiveAt\`. The
*school* gets \`suspendedAt\`, because that field already exists and core's own suspend maintains it.

**The period is not paused either**, so the school loses time it has paid for. Crediting that is a
money decision nothing here can make.`,
      pathParams: [
        { name: "id", value: "{{createdSchoolId}}", note: "The school's id." },
      ],
      requestFields: [
        { name: "reason", required: "yes",
          note: "Max 500, not blank. Stored in three places — on the subscription as reasonForChanges, on the school as statusReason so whoever finds it locked can see why, and on the history row. 'The bill is unpaid' is not enough on its own: which bill, and how far past the grace period." },
      ],
      responseFields: ["subscriptionNo", "status", "reasonForChanges", "note"],
      errors: [
        { status: 409, code: "SUBSCRIPTION_NOT_SUSPENDABLE", when: "Not ACTIVE or PAST_DUE" },
        { status: 409, code: "SCHOOL_NOT_SUSPENDABLE", when: "The school is being wound down" },
        { status: 400, code: "VALIDATION_FAILED", when: "No reason, or a blank one" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No such school" },
        { status: 404, code: "SUBSCRIPTION_NOT_FOUND", when: "The school has no subscription" },
      ],
      examples: [
        {
          name: "1 — cut a school off",
          notes: `The school has to be ACTIVE for its own status to move — a freshly sold
    school is often still PROVISIONING, and then only the subscription
    changes and the note says so:

      db.schools.updateOne({ _id: ObjectId("<id>") },
                           { $set: { status: "ACTIVE" } })

    -> 200. status SUSPENDED, and check BOTH documents:
       db.school_subscriptions.findOne({ schoolId: "<id>", current: true })
         -> status SUSPENDED, reasonForChanges = your reason
       db.schools.findOne({ _id: ObjectId("<id>") })
         -> status SUSPENDED, suspendedAt stamped, statusReason = your reason
       db.subscription_history.find({ schoolId: "<id>",
                                      eventType: "SUSPENDED" })
         -> one row, previousStatus ACTIVE

    THEN PROVE IT ACTUALLY BLOCKS ANYTHING:
      PATCH /schools/current/profile with X-School-Subdomain
        -> 200 before, 409 SCHOOL_NOT_EDITABLE while suspended, 200 after
           Resume Subscription.
      GET /schools/current/subscription/feature-access
        -> active false, and allowed:false on every feature.`,
          body: `{
  "reason": "Invoice INV/2026/08/000412 unpaid 30 days past the grace period."
}`,
        },
        {
          name: "2 — the statuses that cannot be suspended",
          notes: `Set the status and try each:
      db.school_subscriptions.updateOne(
        { schoolId: "<id>", current: true },
        { $set: { status: "TRIAL" } })

    TRIAL      -> 409 SUBSCRIPTION_NOT_SUSPENDABLE. No unpaid bill behind a
                  trial, so this is not that decision — and refusing it is
                  what lets #20 resume to ACTIVE without a lookup.
    SUSPENDED  -> 409. Already suspended.
    CANCELLED  -> 409. Ended rather than paused.
    EXPIRED    -> 409.

    PAST_DUE   -> 200. The ordinary case, the bill having gone unpaid.`,
          body: `{
  "reason": "Invoice unpaid past the grace period."
}`,
        },
        {
          name: "3 — a reason is required",
          notes: `An empty body      -> 400 VALIDATION_FAILED naming reason.
    { "reason": "   " } -> 400. Blank counts as missing.

    Everything else in this module takes a reason because it changed a
    figure. This one takes a reason because it stopped a school working.`,
          body: `{}`,
        },
        {
          name: "4 — a school being wound down",
          notes: `db.schools.updateOne({ _id: ObjectId("<id>") },
                           { $set: { status: "CLOSED" } })

    -> 409 SCHOOL_NOT_SUSPENDABLE. Same for OFFBOARDING, DELETION_PENDING
       and DELETED: a school already closing is not suspended, it is going.

    CHECK the subscription did not move:
      db.school_subscriptions.findOne(...).status  -> still ACTIVE`,
          body: `{
  "reason": "Unpaid."
}`,
        },
      ],
    },
    {
      id: "resume-subscription",
      name: "Resume Subscription",
      method: "POST",
      path: "/platform/schools/{id}/subscriptions/current/resume",
      status: 'live',
      summary: "Switches a school back on after it pays. The period is NOT extended.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/subscriptions/current/resume\` — switches a school back on.

The exact reverse of **#19** and only that: the subscription returns to \`ACTIVE\` and the school
with it. The plan, the price, the ceilings and both period dates are untouched — a suspension
pauses access, and lifting it renegotiates nothing.

### It resumes to ACTIVE without looking anything up

#19 only ever suspends an \`ACTIVE\` or a \`PAST_DUE\` subscription, and a school that has paid is not
\`PAST_DUE\` any more — so \`ACTIVE\` is the only sensible answer. Refusing to suspend a trial is what
buys that simplicity.

\`suspendedAt\` on the school is deliberately **left standing**: it is when the suspension began, and
a resumed school's history is worth keeping. Core's own reactivate leaves it too.

### The period is not extended, and that is deliberate

A school suspended for three weeks comes back to the same \`currentPeriodEnd\`, having paid for time
it could not use. Crediting that is a **money** decision: nothing here raises or credits an invoice,
so moving the end date would be this endpoint inventing a refund. The \`note\` says so instead. If a
credit was agreed, **#14** is where the date moves — deliberately, with a reason recorded.

### What it refuses

| Status | Resume |
|---|---|
| \`SUSPENDED\` | allowed |
| \`ACTIVE\` | \`409\` — nothing to resume |
| \`TRIAL\`, \`PAST_DUE\` | \`409\` — never suspended, so not paused |
| \`CANCELLED\`, \`EXPIRED\` | \`409\` — ended rather than paused; reopening one would be selling a period, which is #13 or #16 |`,
      pathParams: [
        { name: "id", value: "{{createdSchoolId}}", note: "The school's id." },
      ],
      requestFields: [
        { name: "reason", required: "yes",
          note: "Max 500, not blank. Required for the same reason #19's is: a record that says exactly why a school was cut off but only 'resumed' for why it came back answers half the question. Naming the payment closes it." },
      ],
      responseFields: ["subscriptionNo", "status", "reasonForChanges", "note"],
      errors: [
        { status: 409, code: "SUBSCRIPTION_NOT_RESUMABLE", when: "Not SUSPENDED" },
        { status: 409, code: "SCHOOL_NOT_RESUMABLE", when: "The school is being wound down" },
        { status: 400, code: "VALIDATION_FAILED", when: "No reason, or a blank one" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No such school" },
        { status: 404, code: "SUBSCRIPTION_NOT_FOUND", when: "The school has none" },
      ],
      examples: [
        {
          name: "1 — switch it back on",
          notes: `Suspend it first, then send this.

    -> 200. status ACTIVE, and both documents move back:
       db.schools.findOne(...)  -> status ACTIVE, statusReason replaced,
                                   suspendedAt STILL SET (kept on purpose)
       db.subscription_history.find({ eventType: "RESUMED" })  -> one row

    COMPARE WITH WHAT GET SUBSCRIPTION SAID BEFORE THE SUSPENSION:
      planCode, planVersion, billingCycle, contractedPrice, currencyCode,
      maxStudents, maxUsers, currentPeriodStart, currentPeriodEnd,
      subscriptionNo  -> all IDENTICAL.

    And the collection still holds ONE row: unlike #16 and #17, this pair
    writes no second document.

    THE PERIOD IS NOT EXTENDED. currentPeriodEnd is exactly what it was, so
    the school paid for the days it was locked out of. Crediting that is a
    money decision nothing here can make — #14 is where a date moves if a
    credit was agreed.`,
          body: `{
  "reason": "Invoice INV/2026/08/000412 paid in full on 2026-09-08."
}`,
        },
        {
          name: "2 — the statuses that cannot be resumed",
          notes: `ACTIVE     -> 409 SUBSCRIPTION_NOT_RESUMABLE. Nothing to resume.
    TRIAL      -> 409. Never suspended, so not paused.
    PAST_DUE   -> 409.
    CANCELLED  -> 409, and the message says why: it ended rather than
                  paused, so reopening it would be selling a period without
                  saying so. That is #13 or #16.
    EXPIRED    -> 409.`,
          body: `{
  "reason": "Paid."
}`,
        },
        {
          name: "3 — a reason is required",
          notes: `An empty body -> 400 VALIDATION_FAILED naming reason.

    A suspension and its lifting are a pair. A record that explains the
    cut-off but not the restoration answers half the question.`,
          body: `{}`,
        },
      ],
    },
    {
      id: "cancel-subscription",
      name: "Cancel Subscription",
      method: "POST",
      path: "/platform/schools/{id}/subscriptions/current/cancel",
      status: 'live',
      summary: "Ends the subscription. The school keeps working until its paid period runs out.",
      schoolSurface: false,
      docs: `**POST** \`/platform/schools/{id}/subscriptions/current/cancel\` — ends the subscription.

**The school usually keeps working until the period it already paid for runs out.** A school
cancelling mid-month has bought that month, and cutting it off the same afternoon would be keeping
its money and taking the product away.

### The status says cancelled; the period says how long for

No field was added for "cancelled but still running". The status goes \`CANCELLED\` either way —
the contract is over either way — and what decides the access is \`currentPeriodEnd\`, because
\`whyNotActive\` lets a cancelled subscription grant until that date passes.

| | \`status\` | \`currentPeriodEnd\` | Access |
|---|---|---|---|
| the default | \`CANCELLED\` | left alone | until the period runs out |
| \`immediate: true\` | \`CANCELLED\` | **trimmed to now** | stops at once |

The same division of labour **#16** uses when it closes the row a school leaves: the dates record
the period actually served. The immediate shape overwrites what the school had paid for, so the
history row's reason keeps the original end date.

**Nothing quietly undoes it, and no new check was needed.** #17 already refuses to renew a cancelled
subscription and #20 refuses to resume one. Bringing the school back means selling it something new.

### What it refuses

| Subscription | Cancel |
|---|---|
| \`ACTIVE\`, \`PAST_DUE\`, \`TRIAL\`, \`SUSPENDED\` | allowed |
| \`CANCELLED\`, period still running | \`409 CANCELLATION_ALREADY_SCHEDULED\` — but \`immediate: true\` **is** allowed |
| \`CANCELLED\` lapsed, or \`EXPIRED\` | \`409 SUBSCRIPTION_ALREADY_ENDED\` |

Almost everything can be cancelled, the opposite of #19 and #20 — and a \`CLOSED\` or \`OFFBOARDING\`
school is allowed too, because cancelling is *part of* winding a school down. Only a deleted school
is refused.

### It does not touch the school, and no money moves

Unlike #19, this is a commercial end and not a lock-out: the school stays as it is. And nothing
here raises, credits or refunds an invoice, so an immediate cancellation keeps whatever was paid for
the part of the period being given up.

**Nothing marks a lapsed subscription \`EXPIRED\`** either, so a cancellation that has served out its
period reads \`CANCELLED\` with \`periodEnded: true\` — correct in every field, and still not tidied
away. **A job will close these** — there is no endpoint for it, because a period end passing is a
date arriving rather than a decision anybody makes. #22 was dropped on 2026-09-08 for that reason,
the same conclusion #18 reached about \`PAST_DUE\`.

Read \`periodEnded\` alongside \`status\` until the job exists: the record is never wrong, only
untidied.`,
      pathParams: [
        { name: "id", value: "{{createdSchoolId}}", note: "The school's id." },
      ],
      requestFields: [
        { name: "reason", required: "yes",
          note: "Max 500, not blank. Stored as reasonForChanges and on the history row. This is the one transition nothing undoes, so an unexplained end is the one nobody can answer for." },
        { name: "immediate", required: "no",
          note: "Absent or false is the ordinary cancellation — the school keeps the product for the time it paid for. true trims currentPeriodEnd to now, which is what stops the access. It refunds nothing." },
      ],
      responseFields: ["subscriptionNo", "status", "autoRenew", "currentPeriodEnd", "periodEnded",
        "reasonForChanges", "note"],
      errors: [
        { status: 409, code: "SUBSCRIPTION_ALREADY_ENDED", when: "EXPIRED, or cancelled and lapsed" },
        { status: 409, code: "CANCELLATION_ALREADY_SCHEDULED", when: "Cancelled, period still running" },
        { status: 409, code: "SUBSCRIPTION_NOT_CANCELLABLE", when: "The school is deleted" },
        { status: 400, code: "VALIDATION_FAILED", when: "No reason, or a blank one" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No such school" },
        { status: 404, code: "SUBSCRIPTION_NOT_FOUND", when: "The school has none" },
      ],
      examples: [
        {
          name: "1 — the ordinary cancellation",
          notes: `-> 200. status CANCELLED, autoRenew false, and currentPeriodEnd
       UNTOUCHED with periodEnded false — the school is still working.

    CHECK IT REALLY IS:
      GET /schools/current/subscription/feature-access
        -> active TRUE, every feature still allowed. That is the point.
      db.schools.findOne({ _id: ObjectId("<id>") })
        -> status ACTIVE, untouched. This is not #19.
      db.subscription_history.find({ eventType: "CANCELLED" })
        -> one row, and effectiveAt is the PERIOD END, not now — the only
           place in this service where that field is deliberately future.

    IT WILL NOT BE RENEWED OR RESUMED:
      Renew Subscription  -> 409 SUBSCRIPTION_NOT_RENEWABLE
      Resume Subscription -> 409 SUBSCRIPTION_NOT_RESUMABLE`,
          body: `{
  "reason": "School closing at the end of the academic year."
}`,
        },
        {
          name: "2 — end it now instead",
          notes: `-> 200. status CANCELLED and currentPeriodEnd TRIMMED TO NOW, which is
       what stops the access.

      GET /schools/current/subscription/feature-access
        -> active FALSE now. Cancelled AND the period is over.

    The date the school had paid for is overwritten on the document, so the
    history row's reason keeps it: "The period paid for ran to <date> and was
    trimmed to the cancellation."

    NO REFUND. Nothing here raises or credits an invoice.`,
          body: `{
  "reason": "Contract terminated for cause.",
  "immediate": true
}`,
        },
        {
          name: "3 — cancelling twice",
          notes: `Cancel once, then send the same body again:
      -> 409 CANCELLATION_ALREADY_SCHEDULED, naming the date it runs out.
         A repeat changes nothing, so answering 200 would tell you a new
         reason was recorded when the first one still stands.

    But ESCALATING is a real decision and IS allowed:
      { "reason": "...", "immediate": true }  -> 200, period trimmed.

    Once the period has lapsed:
      db.school_subscriptions.updateOne(
        { schoolId: "<id>", current: true },
        { $set: { currentPeriodEnd: new Date("2026-08-01T00:00:00Z") } })
      -> 409 SUBSCRIPTION_ALREADY_ENDED. Now there is nothing left.`,
          body: `{
  "reason": "School closing at the end of the academic year."
}`,
        },
        {
          name: "4 — what it allows that #19 and #20 do not",
          notes: `TRIAL      -> 200. A trial that did not convert ends here. #19 refuses
                  a trial, because there is no unpaid bill behind one.
    SUSPENDED  -> 200. A school that never paid.
    PAST_DUE   -> 200.

    AND THE SCHOOL'S OWN STATUS:
      CLOSED / OFFBOARDING -> 200. Cancelling is PART of winding a school
        down; refusing it would leave a closed school with a live
        subscription nobody could end. #19 and #20 refuse these.
      DELETED / DELETION_PENDING -> 409 SUBSCRIPTION_NOT_CANCELLABLE.

    EXPIRED    -> 409 SUBSCRIPTION_ALREADY_ENDED.`,
          body: `{
  "reason": "Trial ended without converting."
}`,
        },
        {
          name: "5 — a reason is required",
          notes: `{} -> 400 VALIDATION_FAILED naming reason.

    This is the one transition nothing undoes: #17 will not renew it and #20
    will not resume it. A record of the end that does not say why is the one
    nobody can answer for.`,
          body: `{}`,
        },
      ],
    },
    {
      id: "list-school-subscriptions",
      name: "Get School Subscriptions",
      method: "GET",
      path: "/platform/schools/{id}/subscriptions",
      status: 'live',
      summary: "Every subscription this school has ever had — live, lapsed, cancelled and superseded.",
      schoolSurface: false,
      docs: `**GET** \`/platform/schools/{id}/subscriptions\` — the school's whole subscription history.

**Not the same question as #27.** That returns the one row the school is on now; this returns
every row it has ever had — the trial it started on, the plan it left, the period that lapsed, the
cancellation from two years ago. A school on its fourth plan has four documents, and exactly one
of them is \`current\`.

**Every status is included by default**, and that is the point: a history that hid the cancelled
ones would hide what somebody opened it to find.

A school with **no** subscriptions gets an **empty page**, not a 404. A school that does not exist
gets \`404 SCHOOL_NOT_FOUND\` — different problems, different answers.

| Parameter | Meaning |
|---|---|
| \`status\` | repeatable — \`?status=CANCELLED&status=EXPIRED\` means either. **Also how you ask for trials**: \`?status=TRIAL\` |
| \`billingCycle\` | repeatable, ORs within itself |
| \`planCode\` | **exact**, case-insensitive, normalized — \`premium-plus\` finds \`PREMIUM_PLUS\`. A code matching no plan gives an empty page, not an error |
| \`planVersion\` | one version; only meaningful beside \`planCode\` |
| \`autoRenew\` | \`true\` or \`false\` |
| \`current\` | \`true\` is at most one row — the one #27 returns. \`false\` is the history without it |
| \`startDateFrom\`, \`startDateTo\` | instants, **inclusive** both ends, on \`currentPeriodStart\` |
| \`endDateFrom\`, \`endDateTo\` | instants, **inclusive** both ends, on \`currentPeriodEnd\` |
| \`page\`, \`size\` | zero-based; size defaults to 20, max 100 — **refused above it, not clamped** |
| \`sort\` | \`field,direction\` — \`currentPeriodStart\`, \`currentPeriodEnd\`, \`subscriptionNo\`, \`status\`, \`createdAt\`, \`updatedAt\` |

Filters combine with AND; only \`status\` and \`billingCycle\` OR within themselves.

### There is no \`trial\` filter, deliberately

There is no \`trial\` field on the document — a trial is a **status**. #13 takes a \`trial\` flag on
the way in and it lands on \`status\`. A boolean here would be a second way to ask one question,
and two ways to ask one question eventually disagree.

### \`periodEnded\` is computed, and is NOT derivable from \`status\`

Nothing marks a lapsed subscription \`EXPIRED\` yet, so a row can read \`ACTIVE\` with a period that
finished months ago. It is worked out once for the whole page from a single \`now\`, so no two rows
can disagree about it.

### Pagination is stable

The default order is \`currentPeriodStart\` descending, tie-broken on \`subscriptionNo\`, which is
unique within a school — so no two rows compare equal. Without that, two subscriptions sharing a
start (which #16 produces whenever a plan changes on the day a period begins) could appear on
page one *and* page two while another was never seen.

> With a stable tiebreaker, \`desc\` is **not** the exact reverse of \`asc\`: tied rows keep their
> relative order either way. That is the stability working, not a bug.

### What a row does not carry

\`planDefinitionDocsId\` (an internal id — the plan is named instead), \`schoolId\` (it is in the
URL), \`billingCustomerReference\` (a payment-gateway id, wrong thing to spray across twenty rows)
and the plan's features (#27 returns those in full).

### The test cases

\`\`\`
01  BARE LIST                                          -> 200 OK
    GET /platform/schools/{id}/subscriptions
    First 20, newest period first. Every status included.

02  A SCHOOL WITH NO SUBSCRIPTIONS                     -> 200 OK
    An EMPTY page: content [], totalElements 0, totalPages 0.
    NOT a 404 — the school exists, and "nothing yet" is the honest answer.

03  A SCHOOL THAT DOES NOT EXIST                       -> 404 Not Found
    { "code": "SCHOOL_NOT_FOUND" }

04  ONE STATUS, THEN TWO                               -> 200 OK
    ?status=CANCELLED
    ?status=CANCELLED&status=EXPIRED     both, ORed
    ?status=TRIAL                        this is the "trial filter"

05  THE LIVE ROW, AND THE HISTORY WITHOUT IT           -> 200 OK
    ?current=true      at most one row
    ?current=false     everything else

06  ONE PLAN, ONE VERSION                              -> 200 OK
    ?planCode=PREMIUM
    ?planCode=premium-plus     normalized, finds PREMIUM_PLUS
    ?planCode=NO_SUCH          EMPTY page, not an error
    ?planCode=PREMIUM&planVersion=2

07  THE PERIOD WINDOWS                                 -> 200 OK
    ?startDateFrom=2026-04-01T00:00:00Z&startDateTo=2027-03-31T23:59:59Z
    Both ends apply. Inclusive, so a period starting on the from date is in.

08  PAGING                                             -> 200 OK
    ?page=0&size=1   then page=1, page=2 ...
    ?page=99         an empty page, not an error
    ?size=100        the maximum

09  SORTING                                            -> 200 OK
    ?sort=currentPeriodEnd,asc
    ?sort=subscriptionNo,desc
    ?sort=CurrentPeriodStart,DESC     case-insensitive

10  BAD PAGING                                    -> 400 Bad Request
    ?page=-1     INVALID_PAGE
    ?size=0      INVALID_PAGE_SIZE
    ?size=101    INVALID_PAGE_SIZE — refused, NOT clamped to 100

11  BAD SORTING                                   -> 400 Bad Request
    ?sort=contractedPrice,desc   INVALID_SORT_FIELD, listing what is allowed
    ?sort=currentPeriodStart,sideways   INVALID_SORT_DIRECTION

12  A WINDOW THAT RUNS BACKWARDS                  -> 400 Bad Request
    ?startDateFrom=2027-01-01T00:00:00Z&startDateTo=2026-01-01T00:00:00Z
    { "code": "INVALID_DATE_RANGE" }
    A 400 rather than zero rows: zero rows would read as "nothing in that
    range" instead of "you sent from and to the wrong way round".

13  AN INVALID ENUM OR DATE                       -> 400 Bad Request
    ?status=NOT_A_STATUS      through the type-mismatch handler
    ?startDateFrom=not-a-date

14  PARAMETERS ARE CHECKED BEFORE THE SCHOOL IS READ
    A bad page on a school that does not exist answers 400, not 404 —
    so a malformed request costs no database round trip.
\`\`\``,
      pathParams: [
        { name: "id", value: "{{createdSchoolId}}", note: "The school's id." },
      ],
      queryParams: [
        { key: "page", value: "0", enabled: true },
        { key: "size", value: "20", enabled: true },
        { key: "sort", value: "currentPeriodStart,desc", enabled: false },
        { key: "status", value: "CANCELLED", enabled: false },
        { key: "billingCycle", value: "MONTHLY", enabled: false },
        { key: "planCode", value: "{{planCode}}", enabled: false },
        { key: "planVersion", value: "1", enabled: false },
        { key: "autoRenew", value: "true", enabled: false },
        { key: "current", value: "false", enabled: false },
        { key: "startDateFrom", value: "2026-04-01T00:00:00Z", enabled: false },
        { key: "startDateTo", value: "2027-03-31T23:59:59Z", enabled: false },
        { key: "endDateFrom", value: "2026-04-01T00:00:00Z", enabled: false },
        { key: "endDateTo", value: "2027-03-31T23:59:59Z", enabled: false },
      ],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_PAGE", when: "page is negative" },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "size outside 1-100 — refused, not clamped" },
        { status: 400, code: "INVALID_SORT_FIELD", when: "A field off the allow-list" },
        { status: 400, code: "INVALID_SORT_DIRECTION", when: "Anything but asc or desc" },
        { status: 400, code: "INVALID_DATE_RANGE", when: "from is after to, on either window" },
        { status: 400, code: "VALIDATION_FAILED", when: "An unknown enum value, or a date that is not an instant" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No such school" },
      ],
      // A GET sends no body, so the fourteen cases are in `docs` above rather than here.
      examples: [],
    },
    {
      id: "get-subscription-history",
      name: "Get Subscription History",
      method: "GET",
      path: "/platform/schools/{id}/subscriptions/{subscriptionNo}/history",
      status: 'live',
      summary: "The audit trail of one subscription: what changed, when, who changed it and why.",
      schoolSurface: false,
      docs: `**GET** \`/platform/schools/{id}/subscriptions/{subscriptionNo}/history\` — one subscription's audit trail.

**The answer to "why did this school get suspended"**, months after whoever did it has forgotten.
Every endpoint that moves a subscription writes a history row *in the same transaction as the
change*, so the trail cannot be missing the one event that explains the state.

Read-only, and the collection is append-only. A correction is a new row written by whichever
endpoint made the change; nothing here can alter one.

### Naming the subscription in the path

A subscription number looks like \`SUB/2026/09/000002\` and **cannot be written in a URL** — the
slashes are path separators, and Tomcat rejects them encoded. Three forms are accepted:

| In the path | What it means |
|---|---|
| \`current\` | the subscription the school is on now |
| \`6aa100755e32971b99de6109\` | its **id** — what #28 returns as \`subscriptionId\` |
| \`SUB-with-no-slashes\` | the number itself, for a caller that can send it |

The school id is in all three lookups, so another school's subscription is a **404**, not its
audit trail.

| Parameter | Meaning |
|---|---|
| \`eventType\` | repeatable — \`?eventType=SUSPENDED&eventType=RESUMED\` means either. This is the "action" filter |
| \`status\` | repeatable. The status the change moved the subscription **to** — "when was it suspended" |
| \`previousStatus\` | repeatable. The status it moved **from** — "when did the trial end". Matches nothing on a first row |
| \`source\` | **exact**, case-insensitive. \`ADMIN_PORTAL\` is the only value written today |
| \`performedByDocsId\` | the acting identity. **Matches nothing today** — see below |
| \`sourceEventId\` | the external event that caused it, for tracing a row back to a webhook or job run |
| \`reason\` | free-text **substring**, case-insensitive. Escaped, so \`.*\` searches for those two characters |
| \`effectiveFrom\`, \`effectiveTo\` | instants, **inclusive** both ends, on \`effectiveAt\` |
| \`recordedFrom\`, \`recordedTo\` | instants, **inclusive** both ends, on \`createdAt\` |
| \`page\`, \`size\` | zero-based; size defaults to 20, max 100 — **refused above it, not clamped** |
| \`sort\` | \`field,direction\` — \`effectiveAt\`, \`createdAt\`, \`eventType\`, \`newStatus\`, \`previousStatus\` |

Filters combine with AND; only \`eventType\`, \`status\` and \`previousStatus\` OR within themselves.

### \`effectiveAt\` and \`createdAt\` are different dates

\`effectiveAt\` is when the change **took effect**; \`createdAt\` is when the row was **written**. A
cancellation agreed today for the end of the period is effective at the end of the period and
recorded today. Filtering the wrong one silently answers a different question, so each window is
named after what it means, and the response returns both — \`effectiveAt\` and \`recordedAt\`.

The default order is newest **effective** first, so a future-dated cancellation sits above events
that already happened. Use \`?sort=createdAt,desc\` for write order.

### There is no plan-code filter, unlike #28

A history row stores **two** plan links, so "rows involving PREMIUM" would have to guess whether
you mean moved-off or moved-to. \`?eventType=PLAN_CHANGED\` is the question that actually gets
asked, and the response names both plans so you can see which.

### A gap is left as a gap

An audit trail is read to settle what actually happened, so a field the endpoint cannot answer is
\`null\` rather than filled in with something plausible:

- \`previousStatus\` is null on a subscription's first row — there was no previous status. It does
  **not** mean "unknown".
- **\`performedByDocsId\` is null on every row that exists today.** Nothing populates it yet — #13
  does not resolve the acting account — so \`source\` is the only answer to "who" the record holds.
  The filter for it is implemented and matches nothing; that is the honest state, not a bug.
- \`reason\` is null when whoever made the change gave none.
- A plan whose document has since been deleted leaves that side's three plan fields null rather
  than failing the page. A history is exactly where a deleted plan turns up.

An event where the plan did **not** move shows the same plan on both sides, because that is what
#19/#20/#21 write — recording which plan was in force when the school was cut off is the point.

### Pagination is stable even when two rows share an instant

The default order ends in the **row id**, the only unique key. Two rows really can share both
dates — #13 and #16 can write in the same millisecond — and tied rows may come back in either
order, so one could appear on page one *and* page two while another was never seen. An audit
trail that loses a row when you page through it is worse than useless.

### What a row does not carry

\`schoolSubscriptionDocsId\` — you named the subscription in the URL, so echoing its internal id
back on all twenty rows says nothing. \`subscriptionNo\` is there instead, so a row copied out of a
page still says what it belongs to.

### The test cases

\`\`\`
01  BARE TRAIL                                         -> 200 OK
    GET /platform/schools/{id}/subscriptions/current/history
    First 20, newest EFFECTIVE change first.

02  BY ID INSTEAD OF \`current\`                         -> 200 OK
    .../subscriptions/{subscriptionId}/history
    The subscriptionId from #28. Same trail.

03  A SUBSCRIPTION WITH ONE CHANGE                     -> 200 OK
    A freshly created subscription has exactly one row: CREATED.
    No subscription can have an EMPTY trail — #13 writes CREATED in the
    same transaction as the subscription itself.

04  A SCHOOL THAT DOES NOT EXIST                       -> 404 Not Found
    { "code": "SCHOOL_NOT_FOUND" }

05  A SCHOOL WITH NO SUBSCRIPTION                      -> 404 Not Found
    { "code": "SUBSCRIPTION_NOT_FOUND" }  "...has no subscription yet."

06  ANOTHER SCHOOL'S SUBSCRIPTION ID                   -> 404 Not Found
    { "code": "SUBSCRIPTION_NOT_FOUND" }
    A 404 and not a 403: confirming somebody else's subscription exists
    is itself a disclosure about the other school.

07  A NUMBER WITH SLASHES IN IT                        -> 404 Not Found
    .../subscriptions/SUB-nonsense/history
    The message explains that a number cannot go in a URL — use
    \`current\`, or the subscriptionId.

08  THE ACTION FILTER                                  -> 200 OK
    ?eventType=SUSPENDED
    ?eventType=SUSPENDED&eventType=RESUMED    both, ORed
    ?eventType=RENEWED                        empty page, not an error

09  BOTH ENDS OF A TRANSITION                          -> 200 OK
    ?status=SUSPENDED           moved TO suspended
    ?previousStatus=TRIAL       moved FROM trial — when the trial ended
    ?status=CANCELLED&previousStatus=ACTIVE   one specific move

10  SOURCE, EXACT AND CASE-INSENSITIVE                 -> 200 OK
    ?source=ADMIN_PORTAL     every row today
    ?source=admin_portal     same rows
    ?source=PORTAL           NONE — it is exact, not a substring

11  FREE-TEXT REASON                                   -> 200 OK
    ?reason=non-payment      matches anywhere in the reason
    ?reason=NON-PAYMENT      case-insensitive
    ?reason=.*               ZERO rows — the text is escaped, so this
                             searches for a dot and a star

12  THE FILTERS THAT MATCH NOTHING TODAY               -> 200 OK
    ?performedByDocsId=6aa10000000000000000abcd
    ?sourceEventId=billing_event_00004519
    Both implemented, both empty: nothing populates those fields yet.

13  THE TWO DATE WINDOWS                               -> 200 OK
    ?effectiveFrom=2026-09-01T00:00:00Z&effectiveTo=2026-09-30T00:00:00Z
    ?recordedFrom=2026-01-01T00:00:00Z&recordedTo=2027-01-01T00:00:00Z
    Both ends apply, inclusive. They are DIFFERENT dates — a cancellation
    can be recorded in September and effective in October.

14  PAGING                                             -> 200 OK
    ?page=0&size=1   then page=1, page=2 ...
    ?page=99         an empty page, not an error
    ?size=100        the maximum

15  SORTING                                            -> 200 OK
    ?sort=effectiveAt,asc      oldest change first
    ?sort=createdAt,desc       write order rather than effective order
    ?sort=eventType,asc
    ?sort=newStatus,desc
    ?sort=previousStatus,asc

16  BAD PAGING                                    -> 400 Bad Request
    ?page=-1     INVALID_PAGE
    ?size=0      INVALID_PAGE_SIZE
    ?size=101    INVALID_PAGE_SIZE — refused, NOT clamped to 100

17  BAD SORTING                                   -> 400 Bad Request
    ?sort=reason,desc          INVALID_SORT_FIELD, listing what is allowed
    ?sort=source,asc           INVALID_SORT_FIELD — off the allow-list
    ?sort=effectiveAt,sideways INVALID_SORT_DIRECTION

18  A WINDOW THAT RUNS BACKWARDS                  -> 400 Bad Request
    ?effectiveFrom=2027-01-01T00:00:00Z&effectiveTo=2026-01-01T00:00:00Z
    { "code": "INVALID_DATE_RANGE" }, with both dates spelled out
    Also on the recorded window. A 400 rather than zero rows, which on an
    audit trail is the difference between a clean record and a lost one.

19  AN INVALID ENUM OR DATE                       -> 400 Bad Request
    ?eventType=NOT_A_THING
    ?status=NOT_A_STATUS
    ?effectiveFrom=not-a-date

20  PARAMETERS ARE CHECKED BEFORE ANYTHING IS READ
    A bad page on a school that does not exist answers 400, not 404 —
    so a malformed request costs no database round trip.

21  READING NEVER WRITES                          -> 405 / 404
    POST, PUT, PATCH and DELETE on this URL are not mapped.
    Read the trail repeatedly: it is byte-identical every time.
\`\`\``,
      pathParams: [
        { name: "id", value: "{{createdSchoolId}}", note: "The school's id." },
        { name: "subscriptionNo", value: "current", note: "`current`, or the subscriptionId from #28. A subscription number has slashes and cannot go in a URL." },
      ],
      queryParams: [
        { key: "page", value: "0", enabled: true },
        { key: "size", value: "20", enabled: true },
        { key: "sort", value: "effectiveAt,desc", enabled: false },
        { key: "eventType", value: "SUSPENDED", enabled: false },
        { key: "status", value: "SUSPENDED", enabled: false },
        { key: "previousStatus", value: "ACTIVE", enabled: false },
        { key: "source", value: "ADMIN_PORTAL", enabled: false },
        { key: "performedByDocsId", value: "", enabled: false },
        { key: "sourceEventId", value: "", enabled: false },
        { key: "reason", value: "non-payment", enabled: false },
        { key: "effectiveFrom", value: "2026-04-01T00:00:00Z", enabled: false },
        { key: "effectiveTo", value: "2027-03-31T23:59:59Z", enabled: false },
        { key: "recordedFrom", value: "2026-04-01T00:00:00Z", enabled: false },
        { key: "recordedTo", value: "2027-03-31T23:59:59Z", enabled: false },
      ],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_PAGE", when: "page is negative" },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "size outside 1-100 — refused, not clamped" },
        { status: 400, code: "INVALID_SORT_FIELD", when: "A field off the allow-list" },
        { status: 400, code: "INVALID_SORT_DIRECTION", when: "Anything but asc or desc" },
        { status: 400, code: "INVALID_DATE_RANGE", when: "from is after to, on either window" },
        { status: 400, code: "VALIDATION_FAILED", when: "An unknown enum value, or a date that is not an instant" },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No such school" },
        { status: 404, code: "SUBSCRIPTION_NOT_FOUND", when: "No such subscription in this school — including another school's" },
      ],
      // A GET sends no body, so the twenty-one cases are in `docs` above rather than here.
      examples: [],
    },
    {
      id: "list-all-subscriptions",
      name: "Get All Subscriptions",
      method: "GET",
      path: "/platform/subscriptions",
      status: 'live',
      summary: "Every school's subscription in one list. The operator's main screen.",
      schoolSurface: false,
      docs: `**GET** \`/platform/subscriptions\` — every school's subscription, in one list.

**The operator's main screen.** Who is on what, who is suspended, whose period is about to lapse
— the questions somebody asks when they are looking after the *platform* rather than one school.
#28 is this same list narrowed to one school by its URL.

**No school in the path.** It is the only endpoint on this controller without one, which is why
the controller is mapped at \`/platform\` with \`/schools/{schoolId}\` on each of its other ten
methods. There is no 404 here: no school is looked up, so an empty platform is an empty page.

### Rows are periods, not schools

The collection holds **one document per billing period**, so a school on its fourth plan appears
four times and exactly one of those rows is \`current\`. A bare list is therefore periods, and the
same school shows up several times.

**\`?current=true\` is the one-row-per-school view**, and it is what a "who is on what" screen
wants. It is deliberately **not** the default: a list endpoint that quietly filtered would report
a total that does not match what it returned.

| Parameter | Meaning |
|---|---|
| \`status\` | repeatable — \`?status=SUSPENDED&status=PAST_DUE\` means either. The headline filter. **Also how you ask for trials**: \`?status=TRIAL\` |
| \`billingCycle\` | repeatable, ORs within itself |
| \`planCode\` | **exact**, case-insensitive, normalized — \`premium-plus\` finds \`PREMIUM_PLUS\`. A code matching no plan gives an empty page, not an error |
| \`planVersion\` | one version; only meaningful beside \`planCode\` |
| \`autoRenew\` | \`true\` or \`false\` |
| \`current\` | \`true\` is one row per school. See above — not defaulted |
| \`startDateFrom\`, \`startDateTo\` | instants, **inclusive** both ends, on \`currentPeriodStart\` |
| \`endDateFrom\`, \`endDateTo\` | instants, **inclusive** both ends, on \`currentPeriodEnd\` |
| \`page\`, \`size\` | zero-based; size defaults to 20, max 100 — **refused above it, not clamped** |
| \`sort\` | \`field,direction\` — \`currentPeriodEnd\`, \`currentPeriodStart\`, \`status\`, \`contractedPrice\`, \`createdAt\`, \`updatedAt\` |

Filters combine with AND; only \`status\` and \`billingCycle\` OR within themselves. The names and
defaults are #28's minus the tenant, on purpose.

**There is no \`schoolId\` filter.** Naming one school *is* #28.

### \`sort\` refuses \`subscriptionNo\`, unlike #28

A subscription number is generated **per school**, so two schools both have a
\`SUB/2026/09/000001\`. Across the platform it is neither unique nor a meaningful order, so it is
off the allow-list — \`?sort=subscriptionNo,asc\` is a \`400 INVALID_SORT_FIELD\`.

That is also why the pagination tiebreaker is the **row id** instead. Period ends tie constantly
here, because schools onboarded together get the same one, and without a unique key a row could
appear on page one *and* page two while another was never seen.

### Every row names its school

\`schoolId\`, \`schoolName\`, \`subdomain\` and \`schoolStatus\`. #28 withholds \`schoolId\` because the
school is in its URL; here the opposite is true, and a row that did not say whose it was would be
unreadable. Paste the \`schoolId\` into #28's URL to drill in.

**\`schoolStatus\` is not padding.** A school that is \`SUSPENDED\` or \`CLOSED\` with an \`ACTIVE\`
subscription is exactly the row this screen exists to surface, and the two statuses move
independently.

### \`periodEnded\` matters more here than anywhere

Nothing marks a lapsed subscription \`EXPIRED\` yet, so a row can read \`ACTIVE\` with a period that
finished months ago. Across the whole platform that is the difference between "paying" and
"nobody has noticed". Worked out once per page from a single \`now\`.

### What a row does not carry

\`planDefinitionDocsId\` (internal — the plan is named instead), \`billingCustomerReference\` (a
payment-gateway id; a list of every school is the worst place for it) and the plan's features
(#27 returns those in full).

### A row with no period end sorts to the top

Mongo puts missing and null values first in an ascending sort, and the default order is
\`currentPeriodEnd\` ascending. \`currentPeriodEnd\` is \`@NotNull\` on the model, so a row without
one exists only from a migration or a hand-written insert — and an operator's screen ordered by
"what needs attention" is the right place for it to surface. One row is in that state today.

### FIXED 2026-09-09: ten documents used to make the bare list a 500

Ten rows had \`currentPeriodEnd\` stored as \`{"$date": "..."}\` — a nested object instead of a BSON
date — so Spring Data threw converting them. It was never this endpoint's bug (#27 and #28
already answered 500 on those ten schools); #30's default request was simply the first to touch
all of them, which is how it was found. **All ten are repaired**, and every date field on
\`school_subscriptions\`, \`schools\`, \`subscription_history\` and \`plan_definitions\` was scanned —
those were the only malformed values.

If it ever recurs, the screen names the pattern rather than blaming the endpoint, and the repair
is:

\`\`\`js
db.school_subscriptions.find({currentPeriodEnd: {$type: 'object'}}).forEach(d =>
  db.school_subscriptions.updateOne({_id: d._id},
    {$set: {currentPeriodEnd: new Date(d.currentPeriodEnd['$date'])}}))
\`\`\`

### The test cases

\`\`\`
01  BARE LIST                                          -> 200 OK
    GET /platform/subscriptions
    First 20, soonest to end first. Several schools in one page.
    A row with no currentPeriodEnd sorts to the top — see above.

02  ONE ROW PER SCHOOL                                 -> 200 OK
    ?current=true
    At most one row per school. This is the "who is on what" view.

03  THE HEADLINE FILTER                                -> 200 OK
    ?status=SUSPENDED
    ?status=SUSPENDED&status=PAST_DUE     both, ORed — and the counts add up
    ?status=TRIAL                         this is the "trial filter"

04  WHO NEEDS CHASING                                  -> 200 OK
    ?status=ACTIVE&current=true&autoRenew=false
    Live, and nobody has agreed a renewal.

05  ONE PLAN, ONE VERSION                              -> 200 OK
    ?planCode=PREMIUM
    ?planCode=premium-plus     normalized, finds PREMIUM_PLUS
    ?planCode=NO_SUCH          EMPTY page, not an error
    ?planCode=PREMIUM&planVersion=2

06  BY CADENCE                                         -> 200 OK
    ?billingCycle=YEARLY
    ?billingCycle=MONTHLY&billingCycle=YEARLY

07  THE PERIOD WINDOWS                                 -> 200 OK
    ?endDateFrom=2026-10-01T00:00:00Z&endDateTo=2026-12-31T23:59:59Z
    Both ends apply, inclusive. This pair is the renewal question.

08  PAGING                                             -> 200 OK
    ?page=0&size=1   then page=1, page=2 ...
    ?page=99999      an empty page, not an error
    ?size=100        the maximum

09  SORTING                                            -> 200 OK
    ?sort=currentPeriodEnd,desc
    ?sort=contractedPrice,desc     biggest contracts first
    ?sort=status,asc
    ?sort=CreatedAt,DESC           case-insensitive

10  A SCHOOL THAT DOES NOT MATCH ITS SUBSCRIPTION      -> 200 OK
    Look for schoolStatus SUSPENDED or CLOSED beside status ACTIVE.
    Nothing keeps the two in step; this screen is where you see it.

11  BAD PAGING                                    -> 400 Bad Request
    ?page=-1     INVALID_PAGE
    ?size=0      INVALID_PAGE_SIZE
    ?size=101    INVALID_PAGE_SIZE — refused, NOT clamped to 100

12  BAD SORTING                                   -> 400 Bad Request
    ?sort=subscriptionNo,asc   INVALID_SORT_FIELD — unique only per school
    ?sort=schoolName,asc       INVALID_SORT_FIELD — not on the subscription
    ?sort=currentPeriodEnd,sideways   INVALID_SORT_DIRECTION

13  A WINDOW THAT RUNS BACKWARDS                  -> 400 Bad Request
    ?endDateFrom=2027-01-01T00:00:00Z&endDateTo=2026-01-01T00:00:00Z
    { "code": "INVALID_DATE_RANGE" }, with both dates spelled out

14  AN INVALID ENUM OR DATE                       -> 400 Bad Request
    ?status=NOT_A_STATUS
    ?billingCycle=NOT_A_CYCLE
    ?endDateFrom=not-a-date

15  PARAMETERS ARE CHECKED BEFORE ANYTHING IS READ
    ?page=-1&planCode=X answers INVALID_PAGE, so a malformed request
    costs no database round trip.

16  READING NEVER WRITES                          -> 405 / 404
    POST, PUT, PATCH and DELETE on this URL are not mapped.
\`\`\``,
      pathParams: [],
      queryParams: [
        { key: "page", value: "0", enabled: true },
        { key: "size", value: "20", enabled: true },
        { key: "sort", value: "currentPeriodEnd,asc", enabled: false },
        { key: "status", value: "SUSPENDED", enabled: false },
        { key: "billingCycle", value: "MONTHLY", enabled: false },
        { key: "planCode", value: "{{planCode}}", enabled: false },
        { key: "planVersion", value: "1", enabled: false },
        { key: "autoRenew", value: "false", enabled: false },
        { key: "current", value: "true", enabled: false },
        { key: "startDateFrom", value: "2026-04-01T00:00:00Z", enabled: false },
        { key: "startDateTo", value: "2027-03-31T23:59:59Z", enabled: false },
        { key: "endDateFrom", value: "2026-04-01T00:00:00Z", enabled: false },
        { key: "endDateTo", value: "2027-03-31T23:59:59Z", enabled: false },
      ],
      headers: [],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_PAGE", when: "page is negative" },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "size outside 1-100 — refused, not clamped" },
        { status: 400, code: "INVALID_SORT_FIELD", when: "A field off the allow-list — subscriptionNo included" },
        { status: 400, code: "INVALID_SORT_DIRECTION", when: "Anything but asc or desc" },
        { status: 400, code: "INVALID_DATE_RANGE", when: "from is after to, on either window" },
        { status: 400, code: "VALIDATION_FAILED", when: "An unknown enum value, or a date that is not an instant" },
      ],
      // A GET sends no body, so the sixteen cases are in `docs` above rather than here.
      examples: [],
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
      responseFields: ["subscriptionId", "subscriptionNo", "schoolId", "planDefinitionDocsId", "planCode", "planVersion", "planName", "planStatus", "planRetired", "status", "billingCycle", "currentPeriodStart", "currentPeriodEnd", "daysRemaining", "periodEnded", "autoRenew", "current", "contractedPrice", "planListPrice", "currencyCode", "hasDiscount", "maxStudents", "maxUsers", "maxStudentsOverride", "maxUsersOverride", "hasLimitOverrides", "featureCount", "features", "reasonForChanges", "billingCustomerReference", "note"],
      captures: [
        { variable: "subscriptionNo", from: "subscriptionNo" },
      ],
      errors: [],
      examples: [],
    },
  ],
};

const GROUP_PLANS_SUBSCRIPTION_THE_SCHOOL_S_OWN_VIEW = {
  id: "plans-subscription-the-school-s-own-view",
  module: "Plans / Subscription — the school's own view",
  endpoints: [
    {
      id: "get-my-subscription",
      name: "Get My Subscription",
      method: "GET",
      path: "/schools/current/subscription",
      status: 'live',
      summary: "The school's own billing screen. Deliberately less than the platform sees.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/subscription\` — the school's own billing screen.

What plan it is on, what it costs, when the period ends, whether it renews.

### There is no \`{id}\` in the path, and that is the point

The tenant comes from \`CurrentSchoolResolver\` — today the \`X-School-Subdomain\` header, tomorrow
the session. A caller cannot ask about a school it does not belong to, because it never names one.

### It is not #27 with a different URL

#27 is the platform read and shows everything. This one leaves out four things on purpose:

- \`planListPrice\` — a school on a negotiated price would see a number it is not paying.
- \`billingCustomerReference\` — the gateway's id for them. Ours, not theirs.
- the limit overrides — "your limit is 2500" is useful; "negotiated up from 2000" is a
  commercial conversation.
- \`planCode\` — the internal family key. A school reads the name.

\`contractedPrice\` comes back as \`price\`: from where the school sits there is only one price.

### \`note\` is written for the school

Every branch says what it means for the person paying, and none of them mention what the module
cannot do yet. That is deliberately different from #27's note.

### The four test cases

**01  A SCHOOL WITH A SUBSCRIPTION  -> 200 OK**
\`status\`, \`planName\`, \`price\`, \`currencyCode\`, the period, and \`daysRemaining\` counting down.
Confirm \`planListPrice\` and \`billingCustomerReference\` are **absent** — that is the whole design.

**02  NO TENANT HEADER  -> 400 Bad Request**
\`{ "code": "TENANT_NOT_RESOLVED" }\`. Disable the header to see it.

**03  A SUBDOMAIN THAT MATCHES NO SCHOOL  -> 404 Not Found**
\`{ "code": "SCHOOL_NOT_FOUND" }\`.

**04  A SCHOOL WITH NO SUBSCRIPTION  -> 404 Not Found**
\`{ "code": "SUBSCRIPTION_NOT_FOUND", "message": "This school has no subscription." }\`
Note it uses \`require()\`, not \`requireUsable()\` — a suspended school can still read its own
billing screen, which is exactly when somebody needs to.
`,
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["subscriptionNo", "status", "planName", "planDescription", "planVersion", "billingCycle", "price", "currencyCode", "currentPeriodStart", "currentPeriodEnd", "daysRemaining", "periodEnded", "autoRenew", "note"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
      ],
      examples: [],
    },
    {
      id: "get-feature-access",
      name: "Get Feature access",
      method: "GET",
      path: "/schools/current/subscription/feature-access",
      status: 'live',
      summary: "What this school is allowed to use. The one every other module has to ask.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/subscription/feature-access\` — what this school is allowed to use.

**The one the rest of the product asks.** No module may read \`plan_definitions.features\` and
decide for itself whether a school can use something: two places working that out disagree, and
they disagree in the direction of letting a school use what it has not paid for.

In-process callers use \`Feature accessService\` directly. This endpoint is the same method with a URL
in front of it.

### Read \`allowed\`, not \`includedInPlan\`

- \`includedInPlan\` — what the plan says.
- \`allowed\` — whether it may be used **right now**: the plan saying yes *and* the subscription
  granting anything at all.

\`allowed\` is false on every feature when the subscription grants nothing, so a caller that reads
the rows and forgets the top-level \`active\` flag still gets the right answer.

### What grants nothing

\`SUSPENDED\`, \`CANCELLED\`, \`EXPIRED\`, or a period end in the past whatever the status says.
\`PAST_DUE\` **does** still grant — an unpaid invoice is a conversation to have, not a reason to
lock a school out of its attendance register mid-morning. \`reason\` says which it is.

### No usage counts

This is the ceiling, not how much is gone. Counting students and users is #35, separate because a
gate check runs on every request that touches a feature.

### The five test cases

**01  A SCHOOL ON A LIVE SUBSCRIPTION  -> 200 OK**
\`active: true\`, \`reason: null\`, and every included feature \`allowed: true\`. \`maxStudents\` and
\`maxUsers\` are the ceilings in force — the school's override where it has one.

**02  A SCHOOL ON A TRIAL  -> 200 OK**
\`active: true\`. A trial grants everything the plan does; it is a paying question, not an access
question.

**03  A LAPSED PERIOD  -> 200 OK, granting nothing**
Create a subscription with \`currentPeriodEnd\` in the past. \`active: false\`,
\`reason: "The subscription period ended on …"\`, and **every** feature \`allowed: false\` while
\`includedInPlan\` stays true. This is the case a module reading the plan directly would get wrong.

**04  A SCHOOL WITH NO SUBSCRIPTION  -> 404 Not Found**
\`{ "code": "SUBSCRIPTION_NOT_FOUND", "message": "This school has no subscription, so it is not
entitled to anything." }\`
A 404 rather than an empty allowance: entitled to nothing either way, but an empty feature list
would be indistinguishable from a plan published with no features.

**05  NO TENANT HEADER / UNKNOWN SUBDOMAIN  -> 400 / 404**
\`TENANT_NOT_RESOLVED\` and \`SCHOOL_NOT_FOUND\`.
`,
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: ``,
      successStatus: 200,
      responseFields: ["active", "reason", "subscriptionNo", "status", "planName", "planVersion", "currentPeriodEnd", "maxStudents", "maxUsers", "featureCount", "features"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SCHOOL_NOT_EDITABLE", when: "The school is past PROVISIONING or ACTIVE and cannot be edited." },
      ],
      examples: [],
    },
  ],
};

const GROUP_ACADEMICS_TERMS = {
  id: "academics-terms",
  module: "Academics / Terms",
  endpoints: [
    {
      id: "create-academic-term",
      name: "Create Term",
      method: "POST",
      path: "/schools/current/academic-years/{year}/terms",
      status: 'live',
      summary: "Add one reporting period to the year.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{year}/terms\` — endpoint #1.

### A term is a document, unlike a section or a subject

Six documents across three modules reference one by \`termDocsId\` — \`Exam\`, \`ReportCard\`,
\`HolisticProgressCard\`, \`FeedbackCampaign\`, \`FeeInstallment\` and \`FeeInvoice\`. That is why
a term has an id, and why it can be **renamed safely** where a \`sectionNo\` can never be.

### Six rules MongoDB cannot express, in this order

1. the range is not inverted — \`400 INVALID_TERM_RANGE\`
2. it falls inside the year — \`409 TERM_OUTSIDE_ACADEMIC_YEAR\`
3. the code is free — \`409 TERM_CODE_TAKEN\`
4. the sequence is free — \`409 TERM_SEQUENCE_TAKEN\`
5. the name is free — \`409 TERM_NAME_TAKEN\` (case-insensitive)
6. the dates are free — \`409 TERMS_OVERLAP\`

**The order matters.** An inverted range checked last would be reported as "outside the year",
which is true and says the wrong thing about what is wrong.

All of them answer from **one** read of the year's terms, so they cannot disagree with each other.

### A retired term keeps its code, its sequence and its name — but releases its dates

None of the three unique indexes filters on \`active\`, so a check that skipped retired rows would
accept a write the database then refuses. The **dates** are different: nothing is taught in a retired term,
and \`school_year_term_active_dates_idx\` is indexed on \`active\` for exactly that query.

Three rules, two different answers to "does a retired term still count", and the difference is
deliberate.

### termCode is given, not derived

**Changed 2026-09-12.** It used to be derived from \`name\`, which tied two fields that do not move
together: a school renaming "Term 1" to "First Term" would have been offered a code of
\`FIRST_TERM\` on a term six documents across three modules already reference as \`TERM1\`. The
code is the stable half, the name is the display half, so the code is stated outright.

The shape is fixed and **tighter than the derivation's was** — \`^[A-Z0-9]+$\`, 40 characters:
uppercase letters and digits, no underscore. \`TextHelper.toCode\` emitted underscores because it
had to put something where a space had been; a code stated outright has no such gap to fill.

It is **validated, not normalized**: \`term 1\` is a \`400\` naming the field, never a silent
rewrite into something the caller never typed and will not recognise coming back.

### Weights: a shortfall is reported, an excess is refused

\`weightPercent\` null means **this school does not weight the annual result**, which is a normal
way to run a school. What is refused is the *mixture* — one active term weighted and another not —
because it computes to nothing and no sequence of edits passes through it legitimately.

A total **below** 100 comes back as a **\`warning\` on the response**, not a refusal: 20/80 →
30/70 passes through 110, and refusing that would make the values impossible to change. It is also
where every year sits while its terms are still being entered.

A total **above** 100 is a \`409 TERM_WEIGHTS_EXCEED_100\`. The transient-invalid argument is about
*editing*, and an insert is not an edit — a create only ever adds to the sum, so nothing has to
pass through "over 100" to reach a valid set. **The ceiling only, never equality**: requiring
exactly 100 would make the first weighted term of a year impossible to create.

### Adjacency is not overlap

A term ending 30 September and one starting 1 October are fine. The predicate is
\`Dates.overlaps\`, **shared with core's academic-year check** so the two cannot come to disagree
about touching endpoints — which is a question a school hits every April.

### The gates

Same three as every write in this module — **1** school ACTIVE · **2** subscription usable ·
**4** the year is running.

### The fifteen test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain and a year that is marked as running.

 A TERM IS A DOCUMENT, not an embedded row. Six documents across three
 modules store termDocsId, which is why it has an id — and why a term can
 be renamed where a sectionNo never can.

 termCode IS GIVEN, NOT DERIVED (changed 2026-09-12). Uppercase letters and
 digits only: ^[A-Z0-9]+$, 40 max. No underscore. VALIDATED, NOT NORMALISED,
 so "term 1" is a 400 naming the field, not a silent rewrite.

 A RETIRED TERM KEEPS ITS CODE AND SEQUENCE, because neither unique index
 filters on active. It RELEASES ITS DATES, because nothing is taught in it.

 THE MIXTURE IS REFUSED. One term weighted and another not computes to
 nothing. A SHORTFALL is only a warning, because 20/80 -> 30/70 passes
 through 110, and because a year being entered sits under 100 until the last
 term arrives. AN EXCESS IS REFUSED: a create only ever adds to the sum.

 ADJACENCY IS NOT OVERLAP. Ending 30 Sep and starting 1 Oct is fine.`,
      requiredFields: ["name", "termCode", "sequence", "startDate", "endDate"],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the term belongs to. It must exist, and gate 4 requires it to be the running one." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "Term 1",
  "termCode": "TERM1",
  "sequence": 1,
  "startDate": "2026-04-01",
  "endDate": "2026-09-30"
}`,
      successStatus: 201,
      successNote: "Also sends a Location header pointing at the term by its termCode.",
      responseFields: ["termDocsId", "academicYear", "termCode", "name", "sequence", "startDate", "endDate", "resultsLocked", "active"],
      captures: [],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "A missing name, termCode, sequence, startDate or endDate; a termCode that is not uppercase letters and digits; a sequence below 1; a weightPercent outside 0–100." },
        { status: 400, code: "INVALID_TERM_RANGE", when: "endDate is before startDate. Equal dates are legal — a one-day term is odd, not wrong." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 409, code: "TERM_CODE_TAKEN", when: "That year already has that termCode — retired terms included." },
        { status: 409, code: "TERM_NAME_TAKEN", when: "That year already has a term with that name, case-insensitively — retired terms included. A report card names the term, not its code." },
        { status: 409, code: "TERM_SEQUENCE_TAKEN", when: "Another term in the year holds that sequence — retired terms included." },
        { status: 409, code: "TERMS_OVERLAP", when: "The dates cover a day an ACTIVE term already covers. Retired terms do not block." },
        { status: 409, code: "TERM_OUTSIDE_ACADEMIC_YEAR", when: "The dates fall outside the year's own range." },
        { status: 409, code: "TERM_WEIGHT_MIXED", when: "One active term carries a weight and another does not." },
        { status: 409, code: "TERM_WEIGHTS_EXCEED_100", when: "This weight would take the year's active terms past 100%. A shortfall is only a warning; an excess is not." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — the year was ended by POST .../end, or was never marked running." },
      ],
      examples: [
        {
          id: "01",
          name: "THE FIRST TERM",
          expect: "201 Created",
          notes: `The body above.
    OUT: termDocsId, termCode TERM1 as sent, resultsLocked false and
    active true. No weightPercent field, because none was sent.`,
          body: null,
        },
        {
          id: "02",
          name: "THE SECOND, STARTING THE DAY AFTER",
          expect: "201 Created",
          notes: `Adjacency is not overlap — the same rule core uses for years.`,
          body: `{
  "name": "Term 2",
  "termCode": "TERM2",
  "sequence": 2,
  "startDate": "2026-10-01",
  "endDate": "2027-03-31"
}`,
        },
        {
          id: "03",
          name: "THE CODE NEED NOT MATCH THE NAME",
          expect: "201 Created",
          notes: `OUT: termCode SEM2 beside a name of "Semester 2". Nothing derives
    one from the other — which is the point: the name can be changed later
    and the code, which records reference, cannot.`,
          body: `{
  "name": "Semester 2",
  "termCode": "SEM2",
  "sequence": 3,
  "startDate": "2027-01-01",
  "endDate": "2027-01-31"
}`,
        },
        {
          id: "04",
          name: "A CODE OF THE WRONG SHAPE",
          expect: "400 Bad Request",
          notes: `OUT: fieldErrors on termCode. @NotBlank passes — it was not blank.
    Lowercase, spaces and underscores are all refused
    rather than normalised: a silent rewrite hands back a code nobody typed.`,
          body: `{
  "name": "Term 4",
  "termCode": "term 4",
  "sequence": 4,
  "startDate": "2027-02-01",
  "endDate": "2027-02-28"
}`,
        },
        {
          id: "05",
          name: "THE SAME CODE AGAIN",
          expect: "409 Conflict",
          notes: `Send case 01 twice.
    OUT: { "code": "TERM_CODE_TAKEN" }, naming the term that holds it —
    even a retired one. The name may repeat; the code may not.`,
          body: null,
        },
        {
          id: "06",
          name: "A SEQUENCE ALREADY IN USE",
          expect: "409 Conflict",
          notes: `OUT: { "code": "TERM_SEQUENCE_TAKEN" }, naming the term that holds it.`,
          body: `{
  "name": "Extra",
  "termCode": "EXTRA",
  "sequence": 1,
  "startDate": "2027-02-01",
  "endDate": "2027-02-28"
}`,
        },
        {
          id: "07",
          name: "OVERLAPPING DATES",
          expect: "409 Conflict",
          notes: `A single shared day is enough.
    OUT: { "code": "TERMS_OVERLAP" }, naming the term it clashes with.`,
          body: `{
  "name": "Overlapping",
  "termCode": "OVERLAP",
  "sequence": 5,
  "startDate": "2026-09-30",
  "endDate": "2026-10-05"
}`,
        },
        {
          id: "08",
          name: "A RETIRED TERM KEEPS ITS CODE",
          expect: "409 Conflict",
          notes: `Set active:false on a term in Mongo (#7 is not built), then send its
    name again. OUT: TERM_CODE_TAKEN, and the message says "retired" —
    the unique index does not filter on active, so the code stays taken.`,
          body: null,
        },
        {
          id: "09",
          name: "BUT IT RELEASES ITS DATES",
          expect: "201 Created",
          notes: `Same retired term, but reuse its DATE RANGE under a new name.
    Accepted: nothing is taught in a retired term, so the days are free.
    This is the one rule where retired terms are treated differently.`,
          body: null,
        },
        {
          id: "10",
          name: "OUTSIDE THE YEAR",
          expect: "409 Conflict",
          notes: `A term starting before the year begins or ending after it finishes.
    OUT: { "code": "TERM_OUTSIDE_ACADEMIC_YEAR" }, naming what the year covers.`,
          body: `{
  "name": "Too early",
  "termCode": "TOOEARLY",
  "sequence": 8,
  "startDate": "2026-03-31",
  "endDate": "2026-04-02"
}`,
        },
        {
          id: "11",
          name: "AN INVERTED RANGE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_TERM_RANGE" } — NOT "outside the year",
    which would be true and would say the wrong thing.`,
          body: `{
  "name": "Backwards",
  "termCode": "BACKWARDS",
  "sequence": 9,
  "startDate": "2026-12-01",
  "endDate": "2026-11-01"
}`,
        },
        {
          id: "12",
          name: "A WEIGHTED TERM, ALONE",
          expect: "201 Created",
          notes: `In a fresh year. OUT: weightPercent 20, and a WARNING saying the
    active weights total 20%, not 100%. Reported, not refused.`,
          body: `{
  "name": "W1",
  "termCode": "W1",
  "sequence": 1,
  "startDate": "2026-04-01",
  "endDate": "2026-09-30",
  "weightPercent": 20
}`,
        },
        {
          id: "13",
          name: "AN UNWEIGHTED TERM BESIDE IT",
          expect: "409 Conflict",
          notes: `After case 12. OUT: { "code": "TERM_WEIGHT_MIXED" }
    Refused, unlike a wrong total: a mixture computes to nothing, and no
    sequence of edits legitimately passes through it. Refused both ways
    round — a weighted term beside unweighted ones is the same 409.`,
          body: `{
  "name": "W2",
  "termCode": "W2",
  "sequence": 2,
  "startDate": "2026-10-01",
  "endDate": "2027-03-31"
}`,
        },
        {
          id: "14",
          name: "AND THE ONE THAT COMPLETES THE 100",
          expect: "201 Created",
          notes: `weightPercent 80 after case 12's 20.
    OUT: no warning at all, because 20 + 80 = 100.`,
          body: `{
  "name": "W2",
  "termCode": "W2",
  "sequence": 2,
  "startDate": "2026-10-01",
  "endDate": "2027-03-31",
  "weightPercent": 80
}`,
        },
        {
          id: "15",
          name: "ONE PERCENT TOO MANY",
          expect: "409 Conflict",
          notes: `After case 14, the year is at 100. OUT:
    { "code": "TERM_WEIGHTS_EXCEED_100" }, naming what is already used and
    what is left. Refused, unlike a shortfall: a create only ever ADDS to
    the sum, so nothing has to pass through "over 100" on the way to a
    valid set. Lower a term with #3 first, then add this one.`,
          body: `{
  "name": "W3",
  "termCode": "W3",
  "sequence": 3,
  "startDate": "2027-01-01",
  "endDate": "2027-01-31",
  "weightPercent": 1
}`,
        },
      ],
    },
    {
      id: "update-academic-term",
      name: "Update Term",
      method: "PATCH",
      path: "/schools/current/academic-years/{year}/terms/{termId}",
      status: 'live',
      summary: "Fix one term's name, dates or weight. Not its code, not its order.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/academic-years/{year}/terms/{termId}\` — endpoint #3.

### Addressed by the document id, not by termCode

Settled 2026-09-10, and the rule is not "prefer codes" — it is *use whatever other collections
already store*. Six documents across three modules store \`termDocsId\`, so the id is what
identifies a term in a URL. The controller javadoc said the opposite until 2026-09-12, and #1
built its \`Location\` header from the code to match — a URL no route answered.

### Three fields, and the two it refuses are the interesting ones

- **Never \`termCode\`.** A code edit orphans nothing — which is what makes it dangerous.
  Nothing fails, nothing cascades, and every school-facing report, export and saved filter
  naming the old code quietly stops matching.
- **Never \`sequence\` — that is #4.** It is unique within the year, so swapping two terms one
  PATCH at a time hits \`school_year_term_sequence_uniq\` halfway through: term 1 becomes 2
  while term 2 is still 2.
- **Never \`active\` or \`resultsLocked\`** — #5 to #8. Events, not fields.

### The dates are a pair, even when you send one

Sending \`startDate\` alone keeps the stored \`endDate\`, and the two are then checked
**together**. A start moved past an untouched end is \`400 INVALID_TERM_RANGE\` — checking only
the field that arrived would miss it entirely.

### weightPercent cannot be cleared here

A year weights every active term or none, so removing one weight is only legal as part of
removing them all — which is #2. A clear here would be refused by \`TERM_WEIGHT_MIXED\` in
every year with more than one active term, so the field is not offered.

### The weight sum is REPORTED here, never refused

This is the endpoint that proves the rule has to be per-endpoint. 20/80 becomes 30/70 in two
calls and the first one sits at **110**; refusing it would make the values impossible to
change. So a broken total comes back as \`warning\` on a **successful** response.

| endpoint | what it can say about the sum | why |
|---|---|---|
| #1 create | refuses an **excess** | a create only ever adds to the sum |
| #2 replace | requires **exactly 100** | it is the only one that sees every row |
| #3 patch | **reports** only | it can be legitimately mid-edit |

### name is unique within the year

Case-insensitively, and retired terms hold theirs — \`409 TERM_NAME_TAKEN\`. A term may keep the
name it already has: the check excludes it by id rather than comparing names alone.

**This is what a rename now reads the year's terms for.** The year *document* is still loaded only
when dates are sent, because only a date has to fall inside it — but the year's terms are always
loaded, so a rename can carry a weight \`warning\` where it used to carry none.

### The gates

Same three as every write in this module — **1** school ACTIVE · **2** subscription usable ·
**4** the year is running.

### The nine test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain and a year that is marked as running.

 ADDRESSED BY THE DOCUMENT ID, not by termCode. Six documents across three
 modules store termDocsId, so the id is what identifies a term in a URL.

 EVERY FIELD IS OPTIONAL. Absent means leave it alone. An empty body is a
 400 NOTHING_TO_UPDATE, not a 200 - a client with a broken form finds out.

 NEVER termCode (a report naming the old code stops matching silently) and
 NEVER sequence (unique per year - a reorder is #4, a bulk write).

 THE DATES ARE A PAIR. One sent alone keeps the other, and the two are then
 checked together: a start moved past an untouched end is a 400.

 weightPercent CANNOT BE CLEARED. Removing one weight is only legal as part
 of removing them all, which is #2.

 THE WEIGHT SUM IS REPORTED, NEVER REFUSED. 20/80 -> 30/70 sits at 110 after
 the first call. #1 refuses an excess, #2 requires 100, #3 can do neither.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the term belongs to. Gate 4 requires it to be the running one." },
        { name: "termId", value: "{{termDocsId}}", description: "The term's document id — termDocsId on any term response. Not termCode." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "First Term"
}`,
      successStatus: 200,
      successNote: "Returns the term as it now is. A warning rides on it when the year's active weights no longer total 100.",
      responseFields: ["termDocsId", "academicYear", "termCode", "name", "sequence", "startDate", "endDate", "weightPercent", "resultsLocked", "active", "warning", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "A body that asks for nothing. Checked before anything is read." },
        { status: 400, code: "TERM_NAME_REQUIRED", when: "\"name\": \"\" — a name can be replaced, never removed." },
        { status: 400, code: "VALIDATION_FAILED", when: "A name over 120 characters, or a weightPercent outside 0–100." },
        { status: 400, code: "INVALID_TERM_RANGE", when: "endDate before startDate — including a startDate moved past an endDate that was not sent." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "TERM_NOT_FOUND", when: "No term with that id in that year. A real id from another year is a 404 here, not a silent edit." },
        { status: 409, code: "TERM_NAME_TAKEN", when: "Another term in the year already has that name, case-insensitively. The term may keep its own — the check excludes it by id." },
        { status: 409, code: "TERM_OUTSIDE_ACADEMIC_YEAR", when: "The new dates fall outside the year's own range." },
        { status: 409, code: "TERMS_OVERLAP", when: "The new dates cover a day another ACTIVE term already covers. The term never overlaps itself." },
        { status: 409, code: "TERM_WEIGHT_MIXED", when: "A weight sent into a year whose other active terms carry none." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — the year was ended by POST .../end, or was never marked running." },
      ],
      examples: [
        {
          id: "01",
          name: "A RENAME, AND NOTHING ELSE",
          expect: "200 OK",
          notes: `The body above. OUT: the term with its new name, termCode
    untouched, and NO warning — nothing else was read, so there is no
    honest total to report.`,
          body: null,
        },
        {
          id: "02",
          name: "AN EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" }. A PATCH that changes nothing
    and answers 200 lets a broken form look healthy.`,
          body: `{}`,
        },
        {
          id: "03",
          name: "A NAME CLEARED RATHER THAN REPLACED",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "TERM_NAME_REQUIRED" }. @NotBlank would not catch
    it — the field is optional here, so "" has to be refused by hand.`,
          body: `{
  "name": ""
}`,
        },
        {
          id: "04",
          name: "termCode IS IGNORED, NOT REFUSED",
          expect: "200 OK",
          notes: `The record has no termCode field, so Jackson drops it. OUT: the
    name changed and termCode exactly as it was. Nothing renames a code.`,
          body: `{
  "name": "Renamed",
  "termCode": "NEWCODE",
  "sequence": 9
}`,
        },
        {
          id: "05",
          name: "ONE DATE, WHICH MOVES PAST THE OTHER",
          expect: "400 Bad Request",
          notes: `Send only a startDate later than the term's stored endDate.
    OUT: { "code": "INVALID_TERM_RANGE" }. This is the case a per-field
    check would wave through — the field that arrived is fine on its own.`,
          body: `{
  "startDate": "2027-03-01"
}`,
        },
        {
          id: "06",
          name: "DATES OUTSIDE THE YEAR",
          expect: "409 Conflict",
          notes: `OUT: { "code": "TERM_OUTSIDE_ACADEMIC_YEAR" }, naming what the
    year covers. The year is read HERE and only here — a rename never
    loads it.`,
          body: `{
  "startDate": "2020-01-01",
  "endDate": "2020-02-01"
}`,
        },
        {
          id: "07",
          name: "ONTO ANOTHER TERM'S DAYS",
          expect: "409 Conflict",
          notes: `A single shared day is enough. OUT: { "code": "TERMS_OVERLAP" },
    naming the term it clashes with. The term being edited is excluded,
    or it would overlap the version of itself still in the database.`,
          body: null,
        },
        {
          id: "08",
          name: "A WEIGHT THAT BREAKS THE TOTAL",
          expect: "200 OK",
          notes: `In a year at 20/80, send 30 to the first term. ACCEPTED, with a
    warning saying the active weights total 110%. This is the whole
    reason #3 cannot refuse: 30/70 is only reachable through 110.`,
          body: `{
  "weightPercent": 30
}`,
        },
        {
          id: "09",
          name: "A WEIGHT INTO AN UNWEIGHTED YEAR",
          expect: "409 Conflict",
          notes: `OUT: { "code": "TERM_WEIGHT_MIXED" }. Refused where a broken
    total is not: a mixture computes to nothing, and no sequence of edits
    legitimately passes through it.`,
          body: `{
  "weightPercent": 40
}`,
        },
      ],
    },
    {
      id: "lock-term-results",
      name: "Lock Term Results",
      method: "POST",
      path: "/schools/current/academic-years/{year}/terms/{termId}/results/lock",
      status: 'live',
      summary: "Freeze one term's results while another is still being marked.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{year}/terms/{termId}/results/lock\` — endpoint #5.

### The narrow control, and the whole point of the field

\`AcademicYear.resultsLocked\` freezes everything at once, which is what a school wants when the
year is finished — **not** when Term 1's report cards have gone out and Term 2 is still being
taught. A term-level lock is what lets those two states exist at the same time.

### It does not touch the year's flag, in either direction

The year-wide one is the stronger control and overrides this. A narrow endpoint that quietly
widened its own effect is the worst kind of surprise, so this writes \`resultsLocked\` on one term
and nothing else — not \`active\`, not the dates, not the year.

### It locks a retired term without complaint

Deliberate. Retiring a term does not unpublish the report cards issued for it, so freezing its
marks is still a sensible thing to ask for.

### Idempotent, and that is a design choice rather than a shortcut

Asking for a state it is already in is a **200 saying so**, never a 409. A refusal would turn
"make sure this is locked" — the thing a caller actually wants — into a request it has to read
the state before daring to send.

### Gate 4 makes this unreachable on an ended year

Which is the freeze \`controllers/core\` warns about under *lock results before ending the year*.
After \`POST .../end\`, neither of these endpoints answers. Lock first, end second.

### The gates

Same three as every write in this module — **1** school ACTIVE · **2** subscription usable ·
**4** the year is running.

### Nothing reads this flag yet

The endpoint that has to honour it is **mark entry, in \`examination\`**, and it does not exist. So
today this sets a field that no write consults — correct, and inert.

### The four test cases are in the notes below
`,
      bodyNotes: `A POST with NO BODY. Needs X-School-Subdomain and a running year.

 THE NARROW CONTROL. AcademicYear.resultsLocked freezes the whole year; this
 freezes one term, so Term 1 can be final while Term 2 is still being taught.

 IT NEVER TOUCHES THE YEAR'S FLAG, in either direction. The year-wide one is
 stronger and overrides this.

 IDEMPOTENT. Already locked is a 200 saying so, never a 409.

 A RETIRED TERM LOCKS FINE. Retiring does not unpublish its report cards.

 NOTHING READS THIS FLAG YET. Mark entry lives in examination and does not
 exist, so today this sets a field no write consults.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the term belongs to. Gate 4 requires it to be the running one." },
        { name: "termId", value: "{{termDocsId}}", description: "The term's document id — termDocsId on any term response. Not termCode." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "Returns the term with resultsLocked true. No warning field — this endpoint does not touch weights.",
      responseFields: ["termDocsId", "academicYear", "termCode", "name", "sequence", "startDate", "endDate", "weightPercent", "resultsLocked", "active", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "TERM_NOT_FOUND", when: "No term with that id in that year. A real id from another year is a 404 here, not a silent write." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — the year was ended by POST .../end. Lock before ending, not after." },
      ],
      examples: [
        {
          id: "01",
          name: "LOCK ONE TERM",
          expect: "200 OK",
          notes: `No body. OUT: resultsLocked true, and a nextStep saying the other
    terms of the year are unaffected and the year's own flag is untouched.`,
          body: null,
        },
        {
          id: "02",
          name: "LOCK IT AGAIN",
          expect: "200 OK",
          notes: `Send case 01 twice. OUT: still 200, and nextStep says "were already
    locked. Nothing changed." NOT a 409 — the caller asked for a state, not
    for a transition, and it is in that state.`,
          body: null,
        },
        {
          id: "03",
          name: "THE OTHER TERMS ARE UNTOUCHED",
          expect: "200 OK",
          notes: `After case 01, run List Terms. Exactly one row reads locked. That is
    the whole reason this field exists beside the year-wide one.`,
          body: null,
        },
        {
          id: "04",
          name: "A RETIRED TERM LOCKS FINE",
          expect: "200 OK",
          notes: `Set active:false in Mongo (#7 is not built), then lock it. Accepted:
    retiring a term does not unpublish the cards issued for it.`,
          body: null,
        },
      ],
    },
    {
      id: "unlock-term-results",
      name: "Unlock Term Results",
      method: "POST",
      path: "/schools/current/academic-years/{year}/terms/{termId}/results/unlock",
      status: 'live',
      summary: "Reopen one term's results to correct a mark. Records nothing about who or why.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{year}/terms/{termId}/results/unlock\` — endpoint #6.

### It records nothing about who unlocked, or why

The same hole core's #27 carries, and the same answer: it wants a **reason on the request** and an
\`AuditEvent\` row, which wants an audit writer this project does not have.

**Do not run this against real published results until it does.** An unlock that leaves no trace
is indistinguishable from marks that were never locked. The response says so in capitals, which is
the most this endpoint can do about it.

### Unlocking here does not make results writable

\`AcademicYear.resultsLocked\` is the stronger control, so a term unlocked inside a locked year
stays frozen. Two flags, and a result write has to satisfy **both**.

### Idempotent, and that is a design choice rather than a shortcut

Asking for a state it is already in is a **200 saying so**, never a 409. A refusal would turn
"make sure this is locked" — the thing a caller actually wants — into a request it has to read
the state before daring to send.

### Gate 4 makes this unreachable on an ended year

Which is the freeze \`controllers/core\` warns about under *lock results before ending the year*.
After \`POST .../end\`, neither of these endpoints answers. Lock first, end second.

### The gates

Same three as every write in this module — **1** school ACTIVE · **2** subscription usable ·
**4** the year is running.

### The four test cases are in the notes below
`,
      bodyNotes: `A POST with NO BODY. Needs X-School-Subdomain and a running year.

 IT RECORDS NOTHING ABOUT WHO OR WHY. Same hole as core's #27, same answer: a
 reason on the request plus an AuditEvent, which needs an audit writer. DO NOT
 RUN THIS ON REAL PUBLISHED RESULTS UNTIL THEN - an unlock that leaves no trace
 looks the same as never having locked.

 UNLOCKING HERE DOES NOT MAKE RESULTS WRITABLE. The year's flag is the stronger
 control, so a term unlocked inside a locked year stays frozen.

 IDEMPOTENT, the same as #5.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the term belongs to. Gate 4 requires it to be the running one." },
        { name: "termId", value: "{{termDocsId}}", description: "The term's document id — termDocsId on any term response. Not termCode." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "Returns the term with resultsLocked false, and a nextStep warning that nothing recorded who did it.",
      responseFields: ["termDocsId", "academicYear", "termCode", "name", "sequence", "startDate", "endDate", "weightPercent", "resultsLocked", "active", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "TERM_NOT_FOUND", when: "No term with that id in that year. A real id from another year is a 404 here, not a silent write." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — the year was ended by POST .../end. Lock before ending, not after." },
      ],
      examples: [
        {
          id: "01",
          name: "UNLOCK ONE TERM",
          expect: "200 OK",
          notes: `No body. OUT: resultsLocked false, and a nextStep that SHOUTS that
    nothing recorded who did this or why.`,
          body: null,
        },
        {
          id: "02",
          name: "UNLOCK IT AGAIN",
          expect: "200 OK",
          notes: `OUT: still 200, nextStep says "were already unlocked. Nothing
    changed." Idempotent, the same as #5.`,
          body: null,
        },
        {
          id: "03",
          name: "UNLOCK ONE THAT WAS NEVER LOCKED",
          expect: "200 OK",
          notes: `A freshly created term has resultsLocked false. Same 200, same
    "nothing changed" — there is no distinction between never-locked and
    unlocked-again, which is exactly what the missing audit row would fix.`,
          body: null,
        },
        {
          id: "04",
          name: "AFTER THE YEAR HAS ENDED",
          expect: "409 Conflict",
          notes: `Run POST /academic-years/{year}/end first. OUT:
    { "code": "ACADEMIC_YEAR_NOT_RUNNING" }. Corrections to a finished year
    need a way to reopen it, and there is none — see controllers/core.`,
          body: null,
        },
      ],
    },
    {
      id: "list-academic-terms",
      name: "List Terms",
      method: "GET",
      path: "/schools/current/academic-years/{year}/terms",
      status: 'live',
      summary: "The year's terms in sequence order, filtered and paged.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{year}/terms\` — endpoint #9.

### Paged — and the plan said not to be

The plan's reasoning was that a year holds two to four terms, and a page cursor on a four-row list
is machinery nobody uses. **Nothing enforces two to four**: a school running monthly reporting
periods has twelve. The cost is one shared record and one shared factory that already existed for
#28, and a client that handles every list in this API the same way is worth more than four rows
saved. Revisited 2026-09-11.

### Five filters, all optional, all AND-ed

- **\`?active=\`** — terms in use, or retired. Absent returns **both**, which is not the same as
  \`false\`.
- **\`?search=\`** — matches \`name\` **or** \`termCode\`, case-insensitive, anywhere in either.
  A person types whichever they remember.
- **\`?resultsLocked=\`** — the frozen ones. What #5 and #6 write.
- **\`?weighted=\`** — asked with \`exists\`, not \`ne: null\`, because a term written before the
  field existed has no key at all and must read as unweighted.
- **\`?coversDate=\`** — the term covering one date, both ends inclusive.

### coversDate is the question #10 asks only about today

#10 answers "the term covering **today**, in the school's own time zone" — and getting "today"
right is the whole of its job. This asks about a date the caller names, so no zone is involved and
no "today" has to be agreed on.

### Sorted by sequence — which is also the tiebreaker on every other sort

\`?sort=name\` is really \`name, sequence\`. That is **not** this endpoint's doing:
\`PageResponse.pageableOf\` appends the fallback order to whatever the caller named, minus any key
they already used — and #28 gets the same from its own \`name\` fallback.

**So the choice of fallback is the decision that matters.** \`sequence\` is unique within a year,
so every sort ends in a total order and paging cannot put one row on two pages while another
appears on none. A term \`name\` is unique too since 2026-09-12 and could now have served,
but \`sequence\` is what a year is ordered by — a fallback that reshuffled the page on a rename
would be worse for being equally valid. Before that, only \`termCode\` and \`sequence\` were
unique.

\`?sort=\` is an **allowlist**: \`sequence\`, \`name\`, \`startDate\`, \`endDate\`, \`createdAt\`,
\`updatedAt\`. Anything else is \`400\`, because an arbitrary field name reaching a Mongo sort is
how a caller makes the database read every row to answer.

### An unknown year is a 404, not an empty page

No gate runs on a read, so the year check is what answers it. Without it an unknown year would
return an empty page — which reads as "this year has no terms", and those are different facts.

### No gates

A suspended or closed school still reads its own calendar.

### The sixteen test cases are in the notes below
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain and a year.

 PAGED, THOUGH THE PLAN SAID NOT TO BE. Nothing caps a year at four terms;
 a school running monthly reporting periods has twelve.

 FIVE FILTERS, ALL AND-ed: active, search, resultsLocked, weighted,
 coversDate. Absent is never the same as false.

 search MATCHES name OR termCode — a person types whichever they remember.
 The needle is regex-quoted, so a stray "(" is an empty result, not a 500.

 weighted ASKS exists, not ne:null. A term written before the field existed
 has no key at all and must read as unweighted.

 coversDate IS #10'S QUESTION ABOUT ANY DATE. Both ends inclusive, and no
 time zone, because the caller names the date.

 EVERY SORT ENDS IN sequence, because PageResponse appends the fallback
 order to whatever you name. sequence is unique in the year, so that makes
 every sort a total order — a term name would not have, unlike a class name.

 AN UNKNOWN YEAR IS A 404, not an empty page — those are different facts.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year. Unknown is a 404, not an empty page." },
      ],
      queryParams: [
        { key: "active", value: "", enabled: false, description: "true for terms in use, false for retired. Absent returns both." },
        { key: "search", value: "", enabled: false, description: "Matches name OR termCode, case-insensitive, anywhere. Blank is treated as absent." },
        { key: "resultsLocked", value: "", enabled: false, description: "true for terms whose results are frozen — what #5 and #6 write." },
        { key: "weighted", value: "", enabled: false, description: "true for terms carrying a weightPercent. Asked with exists, so a missing field reads as unweighted." },
        { key: "coversDate", value: "", enabled: false, description: "The term covering this date, both ends inclusive. #10 asks the same of today, in the school's zone." },
        { key: "page", value: "", enabled: false, description: "0-based. Negative is a 400." },
        { key: "size", value: "", enabled: false, description: "1 to 100, default 20." },
        { key: "sort", value: "", enabled: false, description: "sequence | name | startDate | endDate | createdAt | updatedAt, with ,desc. Anything else is a 400." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "A page envelope: content, page, size, totalElements, totalPages, hasNext, hasPrevious.",
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_PAGE", when: "page is negative." },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "size is below 1 or above 100." },
        { status: 400, code: "INVALID_SORT_FIELD", when: "A sort field not on the allowlist. The message lists what is." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school — not an empty page." },
      ],
      examples: [
        { id: "01", name: "EVERY TERM IN THE YEAR", expect: "200 OK",
          notes: `No parameters.\n    OUT: the terms in sequence order, inside a page envelope.\n    Default size 20, so one page for any realistic year.`, body: null },
        { id: "02", name: "A YEAR WITH NO TERMS", expect: "200 OK",
          notes: `OUT: content: [], totalElements: 0. An empty page, never a 404.`, body: null },
        { id: "03", name: "AN UNKNOWN YEAR", expect: "404 Not Found",
          notes: `OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }\n    NOT an empty page — "no such year" and "no terms" are different facts.`, body: null },
        { id: "04", name: "ONLY THE ACTIVE ONES", expect: "200 OK",
          notes: `?active=true\n    Then ?active=false for the retired ones, and leave it off for BOTH.\n    Absent is not the same as false.`, body: null },
        { id: "05", name: "SEARCH BY CODE", expect: "200 OK",
          notes: `?search=TERM1 — matches the termCode.`, body: null },
        { id: "06", name: "SEARCH BY NAME", expect: "200 OK",
          notes: `?search=semester — case-insensitive, matches anywhere in the name.\n    One parameter, two fields, because a person types whichever they recall.`, body: null },
        { id: "07", name: "A STRAY REGEX CHARACTER", expect: "200 OK",
          notes: `?search=Term (1\n    An empty result, not a 500 — the needle is Pattern.quote'd.`, body: null },
        { id: "08", name: "THE FROZEN TERMS", expect: "200 OK",
          notes: `?resultsLocked=true — what #5 has locked. #6 unlocks.`, body: null },
        { id: "09", name: "WEIGHTED OR NOT", expect: "200 OK",
          notes: `?weighted=false finds terms with NO weightPercent field at all,\n    including any written before the field existed.`, body: null },
        { id: "10", name: "WHICH TERM COVERS A DATE", expect: "200 OK",
          notes: `?coversDate=2026-08-15\n    Both ends inclusive, so a term's last day is inside it. This is #10's\n    question asked about any date rather than today.`, body: null },
        { id: "11", name: "FILTERS COMBINE", expect: "200 OK",
          notes: `?active=true&weighted=true — AND-ed, like every combination here.\n    A combination nothing matches is an empty page, not a 404.`, body: null },
        { id: "12", name: "ONE PAGE AT A TIME", expect: "200 OK",
          notes: `?page=0&size=2, then ?page=1&size=2.\n    totalElements counts every match, not the page.`, body: null },
        { id: "13", name: "A PAGE PAST THE END", expect: "200 OK",
          notes: `?page=99 — empty content, and the true total beside it. Not an error.`, body: null },
        { id: "14", name: "A BAD PAGE OR SIZE", expect: "400 Bad Request",
          notes: `?page=-1 is INVALID_PAGE; ?size=0 and ?size=101 are INVALID_PAGE_SIZE.\n    Checked BEFORE the year is read, so a bad page costs no query.`, body: null },
        { id: "15", name: "SORTING", expect: "200 OK",
          notes: `?sort=name, ?sort=sequence,desc, ?sort=startDate,desc.\n    ?sort=weightPercent is a 400 — the allowlist is the point.`, body: null },
        { id: "16", name: "THE TIEBREAKER", expect: "200 OK",
          notes: `Give two terms the same name, then ?sort=name&size=2 through the pages.\n    Every row appears exactly once: PageResponse appends the fallback order,\n    and sequence is unique in the year so the result is a total order.`, body: null },
      ],
    },
  ],
};

const GROUP_ACADEMICS_CLASSES = {
  id: "academics-classes",
  module: "Academics / Classes",
  endpoints: [
    {
      id: "create-school-class",
      name: "Create Class",
      method: "POST",
      path: "/schools/current/academic-years/{year}/classes",
      status: 'live',
      summary: "Makes a class for one year, with no sections and no subjects.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{year}/classes\` — endpoint #12 of
\`controllers/academics/structure\`.

### A class is addressed by its id, not by a code

**Twelve** other documents reference a class as \`classDocsId\`, and not one stores a class code —
so the id is what the URL uses, and the \`Location\` header returns it. \`sectionNo\` and
\`subjectCode\` are codes only because they are *embedded* and have no id to be referenced by.

**Which is why the name is editable** (#13, not built). A year *is* its name to every other
collection and can never be renamed; a class is its id, so a rename joins nothing. The name only
has to stay unique inside the year.

### It fixed a bug that would have hit the second class ever created

\`school_year_class_code_uniq\` indexed a \`classCode\` the model never declared, so every document
indexed a **missing** value, they all collided, and a school could hold exactly **one class per
academic year**. The index is now \`school_year_class_name_uniq\` on \`name\`.

### Sections and subjects are not accepted here

A class is always created **empty**. Both are their own resources — #17 and #22, neither built.
Sending them does nothing: the fields are not on the request.

### The year comes from the path

Not the body. A class belongs to one year, and having the year in two places is two places that
can disagree.

### The gates

Writes run **1** (school is ACTIVE) · **2** (subscription usable) · **4** (the year is the
school's working year). Gate 3 is deliberately not used: next year's classes are built in
February, before that year starts.
`,
      bodyNotes: `Needs the X-School-Subdomain header, and a year that exists.
 Run Create School and Create Academic Year first.

 SECTIONS AND SUBJECTS ARE NOT ACCEPTED HERE. The class is created empty and
 both go on afterwards through #17 and #22, neither of which is built. A
 create that could fail on either a bad class name or a stray subject leaves
 the caller working out which, and a half-written subject list is worse than
 an empty one. Same shape as an academic year created with no holidays.

 THERE IS NO CODE FIELD. A class is addressed and referenced by its document
 id. An earlier draft of this endpoint had a classCode derived from the name;
 it was removed the same day, because twelve documents already store
 classDocsId and none stores a code.

 THE NAME IS UNIQUE PER YEAR, NOT PER SCHOOL. "Grade 7" in 2026-2027 and
 "Grade 7" in 2027-2028 are two different classes, and both are allowed.`,
      requiredFields: ["name"],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year this class belongs to, such as 2026-2027. It must already exist." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "Grade 7"
}`,
      successStatus: 201,
      successNote: "Also sends a Location header: /schools/current/academic-years/{year}/classes/{id}",
      responseFields: ["schoolClassId", "academicYear", "name", "affiliationProgrammeDocsId", "sectionCount", "subjectCount", "active", "nextStep"],
      captures: [
        { variable: "schoolClassId", from: "schoolClassId" },
      ],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "No name, a blank name, or one over 120 characters." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "AFFILIATION_PROGRAMME_NOT_FOUND", when: "No such programme in this school — including a real id belonging to another school." },
        { status: 409, code: "CLASS_NAME_TAKEN", when: "That year already has a class with that name." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SCHOOL_NOT_READY", when: "Gate 1 — the school is still PROVISIONING." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
        { status: 404, code: "SUBSCRIPTION_NOT_FOUND", when: "Gate 2 — the school has never had a subscription." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — the year was ended by POST .../end." },
      ],
      examples: [
        {
          id: "01",
          name: "CREATE A CLASS",
          expect: "201 Created",
          notes: `The body above.
    OUT: schoolClassId, sectionCount: 0, subjectCount: 0, active: true
    Header: Location: /schools/current/academic-years/2026-2027/classes/{id}`,
          body: null,
        },
        {
          id: "02",
          name: "DUPLICATE NAME IN THE SAME YEAR",
          expect: "409 Conflict",
          notes: `Send case 01 again.
    OUT: { "code": "CLASS_NAME_TAKEN",
           "message": "'2026-2027' already has a class called 'Grade 7'." }`,
          body: null,
        },
        {
          id: "03",
          name: "THE SAME NAME IN ANOTHER YEAR",
          expect: "201 Created",
          notes: `Create 2027-2028 first, then send case 01 against it.
    The name is unique per YEAR, not per school, so this is allowed and is a
    different document with a different id.`,
          body: null,
        },
        {
          id: "04",
          name: "MANY CLASSES IN ONE YEAR",
          expect: "201 Created, every time",
          notes: `Grade 8, Grade 9, Nursery, XII Science.
    This is the case the old index broke: it indexed a classCode no model
    declared, so every document indexed a MISSING value and the SECOND class
    was a duplicate-key error.`,
          body: `{
  "name": "Grade 8"
}`,
        },
        {
          id: "05",
          name: "SECTIONS AND SUBJECTS ARE IGNORED",
          expect: "201 Created",
          notes: `Neither field is on the request, so both are dropped rather than honoured.
    OUT: sectionCount: 0, subjectCount: 0`,
          body: `{
  "name": "Ignored extras",
  "sections": [{ "sectionNo": "A" }],
  "subjects": [{ "subjectCode": "MATHEMATICS" }]
}`,
        },
        {
          id: "06",
          name: "NO NAME",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "VALIDATION_FAILED", fieldErrors: { name: ... } }`,
          body: `{
  "name": "Grade 7"
}`,
        },
        {
          id: "07",
          name: "AN UNKNOWN YEAR",
          expect: "404 Not Found",
          notes: `Set the year path parameter to 2099-2100.
    OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }
    Gate 4 answers this before the service does — both give the same code.`,
          body: null,
        },
        {
          id: "08",
          name: "ANOTHER SCHOOL'S AFFILIATION PROGRAMME",
          expect: "404 Not Found",
          notes: `A REAL programme id, belonging to a different school.
    OUT: { "code": "AFFILIATION_PROGRAMME_NOT_FOUND" }
    The lookup is findByIdAndSchoolId — the id existing is not enough.`,
          body: `{
  "name": "Borrowed programme",
  "affiliationProgrammeDocsId": "6aa2a107c7cc53f3111217bf"
}`,
        },
        {
          id: "09",
          name: "A YEAR THAT HAS BEEN ENDED",
          expect: "409 Conflict",
          notes: `Run POST /academic-years/{name}/end first.
    OUT: { "code": "ACADEMIC_YEAR_NOT_RUNNING" }
    Gate 4. Classes cannot be added to a year the school has closed.`,
          body: null,
        },
      ],
    },
    {
      id: "update-school-class",
      name: "Update Class",
      method: "PATCH",
      path: "/schools/current/academic-years/{year}/classes/{id}",
      status: 'live',
      summary: "Edits a class's name, sort order, or affiliation programme. Nothing structural.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/academic-years/{year}/classes/{id}\` — endpoint #13.

Three fields, all optional. A body that sends none of them is a **400**, not a silent success.

### The class is named by its MongoDB document id

The id is globally unique, so \`{year}\` is not needed to *find* the class — it is in the path so
that an id pasted from last year's URL answers **404** instead of quietly editing last year's
structure. A real id under the wrong year, or from another school, is a \`CLASS_NOT_FOUND\`.

### The name is editable — an academic year's is not

A year **is** its name to every other collection, so it can never be renamed. A class is its
**id** — twelve documents store \`classDocsId\` — so nothing joins on the name and a rename
cascades nowhere. It only has to stay unique inside the year.

**And a class may keep its own name.** The check compares ids, not names, so sending an unchanged
name beside a new sort order is not a conflict with itself. Using the cheaper "does this name
exist" check would have refused it.

### What can be cleared is not symmetric

    "affiliationProgrammeDocsId": ""     clears it
    "affiliationProgrammeDocsId": null   leaves it       (same as absent)
    "name": ""                           400 CLASS_NAME_REQUIRED

**There is no ordering field.** \`displayOrder\` was removed on 2026-09-11, so a class carries no
school-defined position and there is nothing here to reorder.

### Nothing structural is reachable

\`sections\`, \`subjects\` and \`active\` are not on the request, so sending them does nothing. An
edit that could replace forty embedded rows while looking like a rename is what this avoids;
\`active\` is #15 and #16, neither built.

### The gates

Same three as #12 — **1** school ACTIVE · **2** subscription usable · **4** the year is running.

### The nine test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain, a year, and a class id from Create Class.

 EVERY FIELD IS OPTIONAL AND ABSENT MEANS "LEAVE IT ALONE". Sending {} is a
 400 NOTHING_TO_UPDATE rather than a no-op 200, so a client with a broken
 form finds out.

 A BLANK NAME IS REFUSED, NOT A CLEAR. "name": "" answers
 400 CLASS_NAME_REQUIRED — the same rule HOLIDAY_NAME_REQUIRED follows in the
 core module. Silently keeping the old value would hide the client bug.

 THERE IS NO ORDERING FIELD. displayOrder was removed on 2026-09-11. A class
 has no school-defined position, and #28 lists a year by name.

 NOTHING STRUCTURAL IS HERE. No section, no subject, no active flag. Send
 them and they are dropped.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the class belongs to. A real class id under the wrong year is a 404." },
        { name: "id", value: "{{schoolClassId}}", description: "The class's MongoDB document id — what Create Class returned, and what twelve other documents store as classDocsId." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "Grade Seven"
}`,
      successStatus: 200,
      responseFields: ["schoolClassId", "academicYear", "name", "affiliationProgrammeDocsId", "sectionCount", "subjectCount", "active", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "The body sends none of the three fields." },
        { status: 400, code: "CLASS_NAME_REQUIRED", when: "\"name\": \"\" — a name cannot be removed, only replaced." },
        { status: 400, code: "VALIDATION_FAILED", when: "A name over 120 characters." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "No class with that id in that year — including a real id under the wrong year, or another school's." },
        { status: 404, code: "AFFILIATION_PROGRAMME_NOT_FOUND", when: "No such programme in this school, including another school's real id." },
        { status: 409, code: "CLASS_NAME_TAKEN", when: "Another class in that year already has that name." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — the year was ended by POST .../end." },
      ],
      examples: [
        {
          id: "01",
          name: "RENAME IT",
          expect: "200 OK",
          notes: `The body above.
    OUT: name: "Grade Seven", and the SAME schoolClassId.`,
          body: null,
        },
        {
          id: "02",
          name: "RESEND THE SAME NAME",
          expect: "200 OK",
          notes: `Sending an unchanged name is NOT a conflict with itself — the check
    compares ids, not names. A cheaper "does this name exist" check would
    have refused it.`,
          body: `{
  "name": "Grade 7 Renamed"
}`,
        },
        {
          id: "03",
          name: "ATTACH A PROGRAMME",
          expect: "200 OK",
          notes: `Checked against THIS school. Another school's real id is a 404.`,
          body: `{
  "affiliationProgrammeDocsId": "6aa2a107c7cc53f3111217bf"
}`,
        },
        {
          id: "04",
          name: "DETACH IT",
          expect: "200 OK",
          notes: `"" clears it; null would leave it alone.
    OUT: the field drops out of the response.`,
          body: `{
  "affiliationProgrammeDocsId": ""
}`,
        },
        {
          id: "05",
          name: "AN EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" }
    A PATCH that changes nothing must not report success.`,
          body: `{
}`,
        },
        {
          id: "06",
          name: "A BLANK NAME",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "CLASS_NAME_REQUIRED" }
    Refused, not treated as a clear.`,
          body: `{
  "name": ""
}`,
        },
        {
          id: "07",
          name: "A NAME ANOTHER CLASS HAS",
          expect: "409 Conflict",
          notes: `Create two classes, then rename one to the other's name.
    OUT: { "code": "CLASS_NAME_TAKEN" }`,
          body: null,
        },
        {
          id: "08",
          name: "A REAL ID UNDER THE WRONG YEAR",
          expect: "404 Not Found",
          notes: `Set year to another year this school has, keeping the same class id.
    OUT: { "code": "CLASS_NOT_FOUND" } — the year scopes the lookup.`,
          body: null,
        },
        {
          id: "09",
          name: "SECTIONS AND SUBJECTS ARE IGNORED",
          expect: "200 OK",
          notes: `Neither field is on the request.
    OUT: sectionCount: 0, subjectCount: 0, active still true`,
          body: `{
  "name": "Grade Seven",
  "sections": [{ "sectionNo": "A" }],
  "active": false
}`,
        },
      ],
    },
    {
      id: "list-school-classes",
      name: "List Classes",
      method: "GET",
      path: "/schools/current/academic-years/{year}/classes",
      status: 'live',
      summary: "One year's classes by name, filtered, searched and paged.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{year}/classes\` — endpoint #28.

The screen a school opens to see its own structure. Every filter is optional; a bare call is the
first page in \`name\` order.

### The filters, all AND-ed

    ?active=true                        only the classes in use
    ?search=grade                       name contains "grade", case-insensitive
    ?affiliationProgrammeDocsId=67aa…   one board's classes
    ?hasSections=false                  THE SETUP CHECKLIST
    ?hasSubjects=false                  nothing taught in these yet

**\`?hasSections=false\` is the one worth knowing.** A class with no section cannot hold a
student — \`StudentAcademicRecord\` stores \`sectionNo\` — so that is how a school finds what it
has not finished setting up. Asked of \`sections.0\`, not a stored count, so there is no second
field to keep in step with the list.

### Ordered by name, and that is a downgrade worth knowing

\`displayOrder\` was removed on 2026-09-11, so a class has no school-defined position and this
endpoint defaults to \`name\` ascending. Alphabetical is **not** the order a school reads its
classes in: "Grade 10" sorts before "Grade 2", and "Nursery, LKG, UKG, 1, 2, 3" cannot be
expressed at all.

What it buys is a sort that needs no tiebreaker — \`name\` is unique within the year, so it is a
total order — served by \`school_year_class_name_uniq\` rather than a blocking in-memory pass.

### Rows carry counts, not the embedded lists

A twelve-class year with four sections and ten subjects each is 168 embedded rows nobody reads.
#29 is for one class in full, and it is not built.

### Paging is refused, never clamped

\`?size=101\` is a **400**. A caller who asked for 5000 rows and silently got 100 has been handed
a page they will read as the whole answer.

### No gates on a read

A suspended school can still read its structure and cannot write to it. Which makes the year
check the only thing that answers \`404 ACADEMIC_YEAR_NOT_FOUND\` here — on #12 and #13 gate 4
answers it first.

**A year with no classes is an empty page. An unknown year is a 404.** Different answers.

### The eleven test cases are in the request body as comments
`,
      bodyNotes: `A GET, so there is no body. Everything is a query parameter.

 THE DEFAULT ORDER IS name, NOT A SCHOOL-DEFINED ONE. displayOrder was
 removed on 2026-09-11, so alphabetical is all there is: "Grade 10" comes
 before "Grade 2". The sort needs no tiebreaker, name being unique within
 the year, and school_year_class_name_uniq serves it.

 SIZE IS REFUSED, NEVER CLAMPED. 101 is a 400. Try it.

 AN UNKNOWN YEAR IS A 404, A YEAR WITH NO CLASSES IS AN EMPTY PAGE.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year whose classes to list. Must exist — an unknown year is a 404, not an empty page." },
      ],
      queryParams: [
        { key: "active", value: "", enabled: false, description: "true for classes in use, false for retired. Absent returns both." },
        { key: "search", value: "", enabled: false, description: "Case-insensitive, matches anywhere in the name. Blank is treated as absent." },
        { key: "affiliationProgrammeDocsId", value: "", enabled: false, description: "Only classes under one board programme. An unknown id is an empty page." },
        { key: "hasSections", value: "", enabled: false, description: "false is the setup checklist — classes nothing can be placed in yet." },
        { key: "hasSubjects", value: "", enabled: false, description: "false is 'nothing is taught in this class yet'." },
        { key: "page", value: "0", enabled: false, description: "Zero-based. Negative is a 400." },
        { key: "size", value: "20", enabled: false, description: "Defaults to 20, capped at 100. Above that is a 400, never a clamp." },
        { key: "sort", value: "", enabled: false, description: "field,direction. name, createdAt, updatedAt only." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "A PageResponse envelope: content, page, size, totalElements, totalPages, hasNext, hasPrevious.",
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_PAGE", when: "page is negative." },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "size is below 1 or above 100 — refused, never clamped." },
        { status: 400, code: "INVALID_SORT_FIELD", when: "sort names something off the allow-list." },
        { status: 400, code: "INVALID_SORT_DIRECTION", when: "The direction is neither asc nor desc." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
      ],
      examples: [
        {
          id: "01",
          name: "THE FIRST PAGE",
          expect: "200 OK",
          notes: `No parameters.
    OUT: content[], page: 0, size: 20, totalElements, totalPages,
         hasNext, hasPrevious — rows in name order.`,
          body: null,
        },
        {
          id: "02",
          name: "THE SETUP CHECKLIST",
          expect: "200 OK",
          notes: `?hasSections=false
    Classes nothing can be placed in yet, because a student record stores
    sectionNo. While #17 is unbuilt this is every class.`,
          body: null,
        },
        {
          id: "03",
          name: "SEARCH BY NAME",
          expect: "200 OK",
          notes: `?search=grade
    Case-insensitive and matches anywhere: finds "Grade 1" and "Upper Grade".`,
          body: null,
        },
        {
          id: "04",
          name: "A SEARCH WITH REGEX CHARACTERS",
          expect: "200 OK, zero rows",
          notes: `?search=Grade (1)
    The needle is quoted before it is compiled, so this searches for those
    seven characters literally — it does NOT match "Grade 1" as a regex group
    would, and a lone "(" does not become a 500.`,
          body: null,
        },
        {
          id: "05",
          name: "TWO FILTERS AT ONCE",
          expect: "200 OK",
          notes: `?search=grade&active=true
    The filters AND together. Nothing ORs within itself here, unlike #30.`,
          body: null,
        },
        {
          id: "06",
          name: "SORT BY NAME, DESCENDING",
          expect: "200 OK",
          notes: `?sort=name,desc
    Really descending. A fallback appended under a caller's field used to
    produce {name: -1, name: 1}, which sorts ASCENDING — the duplicate-key
    trap PageResponse.sortOf now filters out.`,
          body: null,
        },
        {
          id: "07",
          name: "A FIELD OFF THE ALLOW-LIST",
          expect: "400 Bad Request",
          notes: `?sort=sections
    OUT: { "code": "INVALID_SORT_FIELD",
           "message": "... Allowed: name, createdAt, updatedAt." }`,
          body: null,
        },
        {
          id: "08",
          name: "SIZE OVER THE CAP",
          expect: "400 Bad Request",
          notes: `?size=101
    OUT: { "code": "INVALID_PAGE_SIZE" } — refused, not clamped to 100.`,
          body: null,
        },
        {
          id: "09",
          name: "A PAGE PAST THE END",
          expect: "200 OK, zero rows",
          notes: `?page=99
    An empty page, not a 404. totalElements still reports the real count.`,
          body: null,
        },
        {
          id: "10",
          name: "AN UNKNOWN YEAR",
          expect: "404 Not Found",
          notes: `Set the year path parameter to 2099-2100.
    OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }
    No gate runs on a read, so the service's own check is what answers this.`,
          body: null,
        },
        {
          id: "11",
          name: "A SUSPENDED SCHOOL CAN STILL READ",
          expect: "200 OK",
          notes: `Suspend the school, then send case 01 again.
    It still lists. Then try Create Class: 409 SCHOOL_NOT_ACTIVE.
    Reads run no gates; writes run three. That asymmetry is deliberate.`,
          body: null,
        },
      ],
    },
    {
      id: "add-class-section",
      name: "Add Section",
      method: "POST",
      path: "/schools/current/academic-years/{year}/classes/{id}/sections",
      status: 'live',
      summary: "Adds one section to a class. The endpoint the student module was waiting for.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{year}/classes/{id}/sections\` — endpoint #17.

### The endpoint another module was blocked on

\`StudentAcademicRecord\` stores \`sectionNo\` as a plain string, so **no student could be placed
anywhere until a section existed.** Six documents in \`models/academics\` were behind the same
wall — attendance sessions, exam schedules, homework, report cards.

### A section is embedded, so the document written is the class

No section collection, no section id, no \`schoolId\` of its own — it inherits all three from its
class. The response is therefore the class's **whole section list**, not the one row added: the
same shape every calendar endpoint in \`core\` returns for a holiday.

### sectionNo can never change

**Eight collections store it as a plain string**, and a section has no id for them to reference
instead. A rename would not fail and would not cascade — every one of those strings would name a
section that no longer answers to it, and every row would still look valid.

### Unique in the class, case-insensitively — but stored as typed

"A" and "a" in one class is a typo every time, not two sections, and the two would be
indistinguishable on screen. So the check folds case. What a school typed is what is kept, and no
shape is imposed: "Blue" and "Red" are legitimate section names.

**This check is the only guard there is.** Mongo cannot enforce uniqueness *inside* an array, so
unlike a class name there is no index behind it.

### capacity is a plan, not a limit

Nothing enforces it — this module cannot count students, and the refusal for the 41st belongs to
the student module. \`0\` is a **400**: a section nobody can be placed in is not a section.
Absent means no plan was recorded, which is not the same as a plan of zero.

### The gates

Same three as every write here — **1** school ACTIVE · **2** subscription usable · **4** the year
is running.

### The eleven test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain, a year, and a class id from Create Class.

 sectionNo CAN NEVER BE CHANGED. There is no rename endpoint and there must
 not be one: eight collections store it as a plain string, and a section is
 EMBEDDED so it has no id for them to reference instead.

 IT IS BOTH THE REFERENCE AND THE DISPLAY VALUE, which is why ClassSection
 has no separate name field. Uniqueness folds case; storage does not.

 THE DUPLICATE CHECK IS THE ONLY GUARD. Mongo cannot make an array's
 contents unique, so there is no index to fall back on.

 capacity IS A PLAN, NOT A LIMIT. Nothing enforces it. 0 is refused.

 classTeacherDocsId IS CHECKED AGAINST THIS SCHOOL. Another school's real
 staff id is a 404, not an accepted teacher.`,
      requiredFields: ["sectionNo"],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the class belongs to. A real class id under the wrong year is a 404." },
        { name: "id", value: "{{schoolClassId}}", description: "The class's MongoDB document id, from Create Class." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "sectionNo": "A",
  "capacity": 40
}`,
      successStatus: 201,
      successNote: "Also sends a Location header pointing at the class's section list.",
      responseFields: ["schoolClassId", "className", "academicYear", "sectionCount", "activeCount", "sections", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "No sectionNo, a blank one, one over 20 characters, or a capacity below 1." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "No class with that id in that year — including a real id under the wrong year, or another school's." },
        { status: 404, code: "STAFF_NOT_FOUND", when: "No such staff in this school, including another school's real id." },
        { status: 409, code: "SECTION_ALREADY_EXISTS", when: "That class already has a section with that number, case folded. A retired one counts." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — the year was ended by POST .../end." },
      ],
      examples: [
        {
          id: "01",
          name: "ADD A SECTION",
          expect: "201 Created",
          notes: `The body above.
    OUT: the class's WHOLE section list, with sectionCount and activeCount.
    A student can be placed in it as soon as the student module exists.`,
          body: null,
        },
        {
          id: "02",
          name: "JUST A NUMBER",
          expect: "201 Created",
          notes: `capacity and classTeacherDocsId are both optional. A section with no
    class teacher is a real state, not an incomplete one.`,
          body: `{
  "sectionNo": "B"
}`,
        },
        {
          id: "03",
          name: "A DUPLICATE",
          expect: "409 Conflict",
          notes: `Send case 01 again.
    OUT: { "code": "SECTION_ALREADY_EXISTS" }
    Checked in the service — Mongo cannot make an array unique.`,
          body: null,
        },
        {
          id: "04",
          name: "THE SAME NUMBER IN LOWER CASE",
          expect: "409 Conflict",
          notes: `"a" is not a second section. Uniqueness folds case; storage does not.`,
          body: `{
  "sectionNo": "a"
}`,
        },
        {
          id: "05",
          name: "A SECTION NAMED BY COLOUR",
          expect: "201 Created",
          notes: `No shape is imposed. sectionNo is the display value as well as the
    reference, so it is stored exactly as typed — trimmed, nothing else.`,
          body: `{
  "sectionNo": "Blue",
  "capacity": 35
}`,
        },
        {
          id: "06",
          name: "A CAPACITY OF ZERO",
          expect: "400 Bad Request",
          notes: `@Min(1). A section nobody can be placed in is not a section.
    Absent is fine and means no plan was recorded.`,
          body: `{
  "sectionNo": "Z",
  "capacity": 0
}`,
        },
        {
          id: "07",
          name: "A CLASS TEACHER",
          expect: "201 Created",
          notes: `A real Staff.id belonging to THIS school. There is no API that creates
    staff yet — the people module has none — so insert one directly to try it.`,
          body: `{
  "sectionNo": "C",
  "classTeacherDocsId": "67aa15d9dc3f7d0011111111"
}`,
        },
        {
          id: "08",
          name: "ANOTHER SCHOOL'S TEACHER",
          expect: "404 Not Found",
          notes: `A REAL staff id belonging to a different school.
    OUT: { "code": "STAFF_NOT_FOUND" }
    The lookup is findByIdAndSchoolId — existing is not enough.`,
          body: null,
        },
        {
          id: "09",
          name: "NO SECTION NUMBER",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "VALIDATION_FAILED", fieldErrors: { sectionNo: ... } }`,
          body: `{
  "capacity": 40
}`,
        },
        {
          id: "10",
          name: "A REAL CLASS ID UNDER THE WRONG YEAR",
          expect: "404 Not Found",
          notes: `Keep the class id, change the year to another this school has.
    OUT: { "code": "CLASS_NOT_FOUND" } — the year scopes the lookup.`,
          body: null,
        },
        {
          id: "11",
          name: "A YEAR THAT HAS BEEN ENDED",
          expect: "409 Conflict",
          notes: `Run POST /academic-years/{name}/end first.
    OUT: { "code": "ACADEMIC_YEAR_NOT_RUNNING" } — gate 4.
    Then list the classes: a suspended or closed year still READS fine.`,
          body: null,
        },
      ],
    },
    {
      id: "get-school-class",
      name: "Get Class",
      method: "GET",
      path: "/schools/current/academic-years/{year}/classes/{id}",
      status: 'live',
      summary: "The class's own facts, and how much is inside it.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{year}/classes/{id}\` — endpoint #29.

### The counts are here; the rows are not

**Trimmed 2026-09-11.** This used to return \`sections[]\` and \`subjects[]\` in full. It no
longer does, because each now has an endpoint that owns it:

- **#30** \`/sections\` — the section list, with \`?active=\`
- **#37** \`/sections/{sectionNo}\` — one section
- **#31** \`/subjects?sectionNo=\` — the subjects, unioned for a section

Three responses returning the same embedded rows is three places to keep in step, and the first
client to read a section from here would depend on a shape this endpoint has no claim on.

### Why the counts stayed

"3 sections, 3 subjects" is what a class row and a page header show, and making a caller fetch
two lists to count them is the call this endpoint exists to save. They are also the only thing
here that is **derived** — everything else is a column on the document.

### Four counts, not two

Active and total differ for both. A retired section keeps its \`sectionNo\` and still counts,
because records reference it — but it is not one a student can be placed in. A class with four
sections and none active would otherwise look ready.

### One document, one query, no joins

Still true, and still the reason sections and subjects are **embedded** rather than collections
of their own — it is what lets #30, #31 and #37 each be a single lookup rather than a join.

### Nothing is resolved to a name

\`affiliationProgrammeDocsId\` comes back as a **raw id**, and it is the one field here no other
endpoint returns. Resolving it is possible and deliberately not done: one place should decide how
a programme is presented, and it is not this response.

### No gates

A suspended school can read its own class and cannot change it. Which makes the year check the
only thing that answers \`404 ACADEMIC_YEAR_NOT_FOUND\` here.

### The seven test cases are in the request body as comments
`,
      bodyNotes: `A GET, so there is no body. Needs X-School-Subdomain and a class id.

 THE COUNTS ARE HERE; THE ROWS ARE NOT. Trimmed 2026-09-11. Sections come
 from #30 and #37, subjects from #31 — three responses carrying the same
 embedded rows is three places to keep in step.

 THE COUNTS STAYED because "3 sections, 3 subjects" is what a page header
 shows, and they are the only DERIVED thing here. Everything else is a
 column on the document.

 affiliationProgrammeDocsId IS THE FIELD NO OTHER ENDPOINT RETURNS, and it
 comes back as a raw id. One place should decide how a programme is shown.

 A CLASS WITH NOTHING IN IT IS A 200 WITH ZERO COUNTS, never a 404.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the class belongs to. A real class id under the wrong year is a 404." },
        { name: "id", value: "{{schoolClassId}}", description: "The class's MongoDB document id, from Create Class." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      responseFields: ["schoolClassId", "academicYear", "name", "affiliationProgrammeDocsId", "active", "sectionCount", "activeSectionCount", "subjectCount", "activeSubjectCount"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "No class with that id in that year — including a real id under the wrong year, or another school's." },
      ],
      examples: [
        { id: "01", name: "ONE CLASS IN FULL", expect: "200 OK",
          notes: `No parameters.\n    OUT: the class's own fields and four counts — and NO sections[] or\n    subjects[] arrays. Those are #30/#37 and #31.`, body: null },
        { id: "02", name: "A CLASS WITH NOTHING IN IT", expect: "200 OK",
          notes: `Create a class and read it without adding anything.\n    OUT: all four counts 0, and no row arrays at all. Not a 404.`, body: null },
        { id: "03", name: "THE SECTIONS MATCH #30", expect: "200 OK",
          notes: `Run #30 on the same class. The section rows are byte-identical —\n    they share SectionResponse, so a section has one shape everywhere.`, body: null },
        { id: "04", name: "NOTHING IS RESOLVED", expect: "200 OK",
          notes: `classTeacherDocsId comes back as a raw ObjectId string, never a name.\n    Same for subject teachers and grading schemes.`, body: null },
        { id: "05", name: "A REAL CLASS ID UNDER THE WRONG YEAR", expect: "404 Not Found",
          notes: `Keep the class id, change the year to another this school has.\n    OUT: { "code": "CLASS_NOT_FOUND" } — the year scopes the lookup.`, body: null },
        { id: "06", name: "AN UNKNOWN YEAR", expect: "404 Not Found",
          notes: `Set the year to 2099-2100.\n    OUT: { "code": "ACADEMIC_YEAR_NOT_FOUND" }\n    No gate runs on a read, so the service's own check answers this.`, body: null },
        { id: "07", name: "A SUSPENDED SCHOOL CAN STILL READ", expect: "200 OK",
          notes: `Suspend the school, then send case 01 again. It still reads.\n    Then try Add Section: 409 SCHOOL_NOT_ACTIVE.`, body: null },
      ],
    },
    {
      id: "list-class-sections",
      name: "List Sections",
      method: "GET",
      path: "/schools/current/academic-years/{year}/classes/{id}/sections",
      status: 'live',
      summary: "Just the sections, with capacity and class teacher. What a dropdown reads.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{year}/classes/{id}/sections\` — endpoint #30.

### The document read is identical to #29's

A section is embedded, so there is nothing cheaper to fetch. What this saves is the **response**,
which is a tenth of the size — and that is the part that crosses the network. A "move this
student" dropdown wants four fields per section, not a class with every subject assignment behind
it.

### ?active=true is what that dropdown sends

    ?active=true    only the sections a student can be placed in
    ?active=false   only the retired ones
    (absent)        every section, retired included

A retired section still holds its \`sectionNo\` and still appears unfiltered, because records
reference it — but nobody should be placed in one. **Absent is not the same as \`false\`.**

### The counts describe the whole class, not the filtered view

\`sectionCount\` answers "how many does this class have", which does not change because a caller
asked to see some of them. A filtered count would make \`?active=true\` on a class with two
retired sections report two sections and two active — a lie in both halves.

### It shares SectionResponse with #29 and #17

So a section has one shape across every endpoint that returns one. It was nested inside
\`SectionListResponse\` until #29 needed it too; a second copy would have been two shapes for one
thing.

### No gates

Same as #29. A class with no sections is an **empty list**, never a 404.

### The six test cases are in the request body as comments
`,
      bodyNotes: `A GET, so there is no body. Everything is a query parameter.

 THE COUNTS ARE NOT FILTERED. sectionCount and activeCount describe the whole
 class however you filter the rows. Compare them against sections.length on
 an ?active=true call — they deliberately disagree.

 ABSENT IS NOT false. Leaving ?active off returns every section; sending
 false returns only the retired ones.

 A RETIRED SECTION STILL APPEARS unfiltered, because records reference its
 sectionNo. Nothing can retire one yet — #20 is unbuilt — so to see it,
 flip sections.$.active directly in Mongo.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the class belongs to." },
        { name: "id", value: "{{schoolClassId}}", description: "The class's MongoDB document id." },
      ],
      queryParams: [
        { key: "active", value: "", enabled: false, description: "true for sections a student can be placed in, false for retired. Absent returns both." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      responseFields: ["schoolClassId", "className", "academicYear", "sectionCount", "activeCount", "sections"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "No class with that id in that year — including another school's." },
      ],
      examples: [
        { id: "01", name: "EVERY SECTION", expect: "200 OK",
          notes: `No parameters.\n    OUT: schoolClassId, className, sectionCount, activeCount, sections[].\n    No subjects — that is the whole point beside #29.`, body: null },
        { id: "02", name: "WHAT THE DROPDOWN SENDS", expect: "200 OK",
          notes: `?active=true\n    Only the sections a student can be placed in.`, body: null },
        { id: "03", name: "THE RETIRED ONES", expect: "200 OK",
          notes: `?active=false\n    Not the same as leaving it off, which returns both.`, body: null },
        { id: "04", name: "THE COUNTS DO NOT FOLLOW THE FILTER", expect: "200 OK",
          notes: `Retire a section in Mongo, then send ?active=true.\n    sections.length is 2 and sectionCount is still 3. Deliberate:\n    "how many does this class have" is not "how many did you ask to see".`, body: null },
        { id: "05", name: "A CLASS WITH NO SECTIONS", expect: "200 OK",
          notes: `OUT: sections: [], sectionCount: 0. An empty list, never a 404.`, body: null },
        { id: "06", name: "ANOTHER SCHOOL'S CLASS", expect: "404 Not Found",
          notes: `A REAL class id, read with a different school's subdomain.\n    OUT: { "code": "CLASS_NOT_FOUND" } — the tenant scopes the lookup.`, body: null },
      ],
    },
    {
      id: "add-class-subject",
      name: "Add Subject",
      method: "POST",
      path: "/schools/current/academic-years/{year}/classes/{id}/subjects",
      status: 'live',
      summary: "Assigns a subject to the whole class, or to one section of it.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{year}/classes/{id}/subjects\` — endpoint #22.

### Class-wide OR per-section, never both

This is the rule the module plan left open and this endpoint settled. \`MATHEMATICS\` with no
\`sectionNo\` teaches the whole class. \`MATHEMATICS\` with \`sectionNo: "A"\` teaches section A.
**A subject cannot have one of each** — \`409 SUBJECT_ASSIGNMENT_CONFLICT\`, in both directions.

The reason: a section studies **its own rows plus the class's**. Allow both and section A gets
MATHEMATICS twice, with two teachers and two grading schemes and nothing saying which wins. When
sections need different teachers, repeat the code with *each* section — never one section
alongside a class-wide row.

### The key is the pair, not the code

\`(subjectCode, sectionNo)\` must be free. So HINDI for section A and HINDI for section B are two
legitimate rows, and a second HINDI for section A is \`409 SUBJECT_ALREADY_ASSIGNED\`.

**Both checks live in the service.** Mongo cannot enforce uniqueness inside an array, so unlike a
class name there is no index behind either of them.

### subjectCode is normalised; sectionNo is not

\`subjectCode\` is uppercased and every run of non-alphanumerics becomes one underscore, so
\`"maths-2"\` is stored \`MATHS_2\` and \`"Maths (Advanced)"\` becomes \`MATHS_ADVANCED\`. A code
of pure punctuation has nothing left and is \`409 SUBJECT_CODE_INVALID\`.

It can afford to be a code because \`name\` carries the display value. \`sectionNo\` cannot — it is
the reference *and* what appears on screen, which is why #17 stores it exactly as typed.

**\`sectionNo\` in this body is matched case-insensitively and stored the way the class spells
it.** Send \`"a"\` for a section called \`"A"\` and the row reads \`"A"\`; two spellings of one
section would read as two sections.

### Every referenced id is checked with the tenant in the query

\`teacherDocsIds\` against \`staff\`, \`gradingSchemeDocsId\` against \`grading_schemes\` —
\`GradingSchemeRepository\` was built for this endpoint, closing the last of the module's three
missing repositories. **Another school's real id is a 404**, not an accepted reference.

A repeated teacher is \`400 DUPLICATE_TEACHER\` rather than being quietly collapsed. \`[]\` is
legitimate: a subject with no teacher assigned yet.

### The gates

Same three as every write here — **1** school ACTIVE · **2** subscription usable · **4** the year
is running.

### The twelve test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain, a year, and a class id from Create Class.

 CLASS-WIDE OR PER-SECTION, NEVER BOTH. Leave sectionNo out and the whole
 class studies it. Name a section and only that section does. The same
 subjectCode cannot do both — 409, because a section studies its own rows
 AND the class's, so it would get the subject twice.

 THE KEY IS THE PAIR (subjectCode, sectionNo). HINDI for A and HINDI for B
 are two rows. A second HINDI for A is a 409.

 subjectCode IS NORMALISED: uppercased, non-alphanumerics to underscore.
 "maths-2" is stored MATHS_2. sectionNo is NOT — it is matched case-
 insensitively and stored the way the class spells it.

 TEACHERS AND THE GRADING SCHEME ARE CHECKED AGAINST THIS SCHOOL. Another
 school's real id is a 404. The same teacher twice is a 400, not collapsed.`,
      requiredFields: ["subjectCode", "name", "subjectType"],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the class belongs to. A real class id under the wrong year is a 404." },
        { name: "id", value: "{{schoolClassId}}", description: "The class's MongoDB document id, from Create Class." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "subjectCode": "MATHEMATICS",
  "name": "Mathematics",
  "shortName": "Maths",
  "subjectType": "CORE"
}`,
      successStatus: 201,
      successNote: "Also sends a Location header pointing at the class's subject list.",
      responseFields: ["schoolClassId", "className", "academicYear", "subjectCount", "activeCount", "subjects", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "No subjectCode, name or subjectType, one over its length, or a subjectType outside the five." },
        { status: 400, code: "DUPLICATE_TEACHER", when: "The same staff id appears twice in teacherDocsIds." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "No class with that id in that year — including a real id under the wrong year, or another school's." },
        { status: 404, code: "SECTION_NOT_FOUND", when: "The sectionNo sent is not a section of this class. Leave it out for a class-wide subject." },
        { status: 404, code: "STAFF_NOT_FOUND", when: "No such staff in this school, including another school's real id." },
        { status: 404, code: "GRADING_SCHEME_NOT_FOUND", when: "No such grading scheme in this school, including another school's real id." },
        { status: 409, code: "SUBJECT_CODE_INVALID", when: "The code has no letter or digit left after normalising — \"!!!\" and the like." },
        { status: 409, code: "SUBJECT_ALREADY_ASSIGNED", when: "That exact (subjectCode, sectionNo) pair is already on the class." },
        { status: 409, code: "SUBJECT_ASSIGNMENT_CONFLICT", when: "The subject is already assigned the other way round — class-wide when you asked for a section, or per-section when you asked for the class." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — the year was ended by POST .../end." },
      ],
      examples: [
        {
          id: "01",
          name: "A SUBJECT FOR THE WHOLE CLASS",
          expect: "201 Created",
          notes: `The body above — no sectionNo, so every section studies it.
    OUT: the class's WHOLE subject list, with subjectCount and activeCount.
    The row comes back with no sectionNo field at all, not a null one.`,
          body: null,
        },
        {
          id: "02",
          name: "THE CODE IS NORMALISED",
          expect: "201 Created",
          notes: `OUT: subjectCode is MATHS_2 — uppercased, the hyphen an underscore.
    name carries the display value, which is why the code can be a code.`,
          body: `{
  "subjectCode": "maths-2",
  "name": "Maths 2",
  "subjectType": "ELECTIVE"
}`,
        },
        {
          id: "03",
          name: "A CODE WITH NOTHING IN IT",
          expect: "409 Conflict",
          notes: `OUT: { "code": "SUBJECT_CODE_INVALID" }
    Normalising leaves an empty string. @NotBlank passes — it was not blank.`,
          body: `{
  "subjectCode": "!!!",
  "name": "Nonsense",
  "subjectType": "CORE"
}`,
        },
        {
          id: "04",
          name: "A SUBJECT FOR ONE SECTION",
          expect: "201 Created",
          notes: `Needs a section from Add Section first.
    OUT: the row carries sectionNo: "A" and two teachers in the order sent.`,
          body: `{
  "subjectCode": "HINDI",
  "name": "Hindi",
  "subjectType": "LANGUAGE",
  "sectionNo": "A"
}`,
        },
        {
          id: "05",
          name: "THE SAME SUBJECT FOR ANOTHER SECTION",
          expect: "201 Created",
          notes: `Allowed — the key is the PAIR, not the code. This is how two
    sections get different teachers for one subject.`,
          body: `{
  "subjectCode": "HINDI",
  "name": "Hindi",
  "subjectType": "LANGUAGE",
  "sectionNo": "B"
}`,
        },
        {
          id: "06",
          name: "THE SAME PAIR AGAIN, IN LOWER CASE",
          expect: "409 Conflict",
          notes: `Send case 04 with sectionNo "a".
    OUT: { "code": "SUBJECT_ALREADY_ASSIGNED" } — "a" is section A.`,
          body: `{
  "subjectCode": "HINDI",
  "name": "Hindi",
  "subjectType": "LANGUAGE",
  "sectionNo": "a"
}`,
        },
        {
          id: "07",
          name: "PER-SECTION WHEN IT IS ALREADY CLASS-WIDE",
          expect: "409 Conflict",
          notes: `After case 01. OUT: { "code": "SUBJECT_ASSIGNMENT_CONFLICT" }
    Section A already studies it through the class-wide row.`,
          body: `{
  "subjectCode": "MATHEMATICS",
  "name": "Mathematics",
  "subjectType": "CORE",
  "sectionNo": "A"
}`,
        },
        {
          id: "08",
          name: "CLASS-WIDE WHEN IT IS ALREADY PER-SECTION",
          expect: "409 Conflict",
          notes: `After case 04 — the same refusal the other way round.
    OUT: { "code": "SUBJECT_ASSIGNMENT_CONFLICT" }
    Both directions, because either one gives a section the subject twice.`,
          body: `{
  "subjectCode": "HINDI",
  "name": "Hindi",
  "subjectType": "LANGUAGE"
}`,
        },
        {
          id: "09",
          name: "A SECTION THE CLASS DOES NOT HAVE",
          expect: "404 Not Found",
          notes: `OUT: { "code": "SECTION_NOT_FOUND" }
    A typo here would otherwise sit unnoticed until a timetable was built
    and the subject turned out to be taught to nobody.`,
          body: `{
  "subjectCode": "SCIENCE",
  "name": "Science",
  "subjectType": "CORE",
  "sectionNo": "Z"
}`,
        },
        {
          id: "10",
          name: "ANOTHER SCHOOL'S TEACHER",
          expect: "404 Not Found",
          notes: `A REAL staff id belonging to a different school.
    OUT: { "code": "STAFF_NOT_FOUND" } — the lookup carries schoolId.
    Same shape for gradingSchemeDocsId: GRADING_SCHEME_NOT_FOUND.`,
          body: `{
  "subjectCode": "ART",
  "name": "Art",
  "subjectType": "ACTIVITY",
  "teacherDocsIds": ["67aa15d9dc3f7d0011111111"]
}`,
        },
        {
          id: "11",
          name: "THE SAME TEACHER TWICE",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "DUPLICATE_TEACHER" }
    Refused rather than collapsed — a list that quietly loses an entry is
    one nobody notices.`,
          body: `{
  "subjectCode": "ART",
  "name": "Art",
  "subjectType": "ACTIVITY",
  "teacherDocsIds": ["67aa15d9dc3f7d0011111111", "67aa15d9dc3f7d0011111111"]
}`,
        },
        {
          id: "12",
          name: "A YEAR THAT HAS BEEN ENDED",
          expect: "409 Conflict",
          notes: `Run POST /academic-years/{name}/end first.
    OUT: { "code": "ACADEMIC_YEAR_NOT_RUNNING" } — gate 4.
    Then read the class: a suspended or ended year still READS fine.`,
          body: null,
        },
      ],
    },
    {
      id: "update-class-subject",
      name: "Update Subject",
      method: "PATCH",
      path: "/schools/current/academic-years/{year}/classes/{id}/subjects/{subjectCode}",
      status: 'live',
      summary: "Edits one assignment's name, short name, type or grading scheme.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/academic-years/{year}/classes/{id}/subjects/{subjectCode}?sectionNo=\` — endpoint #24.

### The pair is the address, and only one half is in the path

\`subjectCode\` is a path segment; \`sectionNo\` is a query parameter. They are split because one
is usually absent — a class-wide subject has no section, and the ordinary case should not have to
send an empty path segment. **\`?sectionNo=\` blank reads the same as leaving it off.**

Both halves are matched case-insensitively, and the code is normalised the way #22 stored it, so
the URL that created \`maths-2\` finds \`MATHS_2\`. An endpoint that refused the spelling its own
create call accepted would be a trap.

### A 404 tells you which way round the subject actually is

#22 forbids a subject being class-wide *and* per-section, so at most one of the two exists.
Asking for the wrong one answers \`404 SUBJECT_NOT_FOUND\` **with the fix in the message** — "drop
\`?sectionNo=\`" or "use \`?sectionNo=A\`". A bare not-found would be true and useless: the caller
would think the assignment had been deleted.

### Four fields, and the ones missing are the point

\`name\`, \`shortName\`, \`subjectType\`, \`gradingSchemeDocsId\`. Everything else is deliberately
absent from the request record:

- **Not \`subjectCode\` and not \`sectionNo\`** — together they are the key. Seven collections store
  the code as a plain string and a subject row has no id, so a rename would not fail, would not
  cascade, and would leave every stored string naming an assignment that no longer answers to it.
  **Moving an assignment between sections is #26 then #22**, which reads correctly as history.
- **Not \`teacherDocsIds\`** — that is #25. Two endpoints writing one array is how a duplicate id
  gets in.
- **Not \`active\`** — that is #26 and #27. Retiring a subject is an event, not a flag to toggle.

Sending any of them is **ignored, not refused** — the ordinary shape for a \`PATCH\` body.

### What "" clears, and what it refuses

\`shortName: ""\` and \`gradingSchemeDocsId: ""\` clear. \`name: ""\` is
\`400 SUBJECT_NAME_REQUIRED\` — the model requires a name and it is the only thing on the row a
person reads. Clearing the scheme is **not "no grading"**: the resolution order falls through to
\`Exam.gradingSchemeDocsId\`, so it hands the decision back to the exam.

**An empty body is \`400 NOTHING_TO_UPDATE\`**, never a 200 — same rule as #13, so a client with a
broken form finds out.

### The gates

Same three as every write here — **1** school ACTIVE · **2** subscription usable · **4** the year
is running.

### The thirteen test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain, a year, a class id, and a subject from Add Subject.

 THE PAIR IS THE ADDRESS. subjectCode in the path, sectionNo in the query.
 Leave sectionNo off for the class-wide row; blank reads the same as off.
 The code is normalised, so the URL that created "maths-2" finds MATHS_2.

 A 404 SAYS WHICH WAY ROUND IT IS. #22 forbids both at once, so asking the
 wrong way gets "drop ?sectionNo=" or "use ?sectionNo=A" in the message.

 FOUR FIELDS ONLY. Not subjectCode or sectionNo — they are the key, and a
 move is #26 then #22. Not teacherDocsIds — that is #25. Not active — that
 is #26 and #27. Sending them is IGNORED, not refused.

 "" CLEARS shortName AND gradingSchemeDocsId. "" ON name IS A 400.
 Clearing the scheme falls back to the exam's, which is not "no grading".

 AN EMPTY BODY IS A 400, not a no-op 200.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the class belongs to. A real class id under the wrong year is a 404." },
        { name: "id", value: "{{schoolClassId}}", description: "The class's MongoDB document id, from Create Class." },
        { name: "subjectCode", value: "MATHEMATICS", description: "Normalised the way #22 stored it — maths-2 finds MATHS_2." },
      ],
      queryParams: [
        { key: "sectionNo", value: "", enabled: false, description: "Which row: absent or blank is the class-wide one, a value is that section's. Matched case-insensitively." },
      ],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "Mathematics (Core)"
}`,
      successStatus: 200,
      successNote: "Returns the class's whole subject list, the same shape Add Subject answers with.",
      responseFields: ["schoolClassId", "className", "academicYear", "subjectCount", "activeCount", "subjects", "changeSummary"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "The body is empty, or every field in it is null." },
        { status: 400, code: "SUBJECT_NAME_REQUIRED", when: 'name is "" or only spaces. A name cannot be removed, only replaced.' },
        { status: 400, code: "VALIDATION_FAILED", when: "A name over 120, a short name over 40, or a subjectType outside the five." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "No class with that id in that year — including a real id under the wrong year, or another school's." },
        { status: 404, code: "SUBJECT_NOT_FOUND", when: "No row for that (subjectCode, sectionNo) pair. The message says which way round the subject IS, when it is on the class the other way." },
        { status: 404, code: "GRADING_SCHEME_NOT_FOUND", when: "No such grading scheme in this school, including another school's real id." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — the year was ended by POST .../end." },
      ],
      examples: [
        {
          id: "01",
          name: "RENAME THE CLASS-WIDE ROW",
          expect: "200 OK",
          notes: `The body above, with no sectionNo.
    OUT: the class's WHOLE subject list, like #22 answers.
    The short name, type, teachers, scheme and active flag are untouched.`,
          body: null,
        },
        {
          id: "02",
          name: "ALL FOUR FIELDS AT ONCE",
          expect: "200 OK",
          notes: `Everything this endpoint can change, in one call.`,
          body: `{
  "name": "Mathematics",
  "shortName": "Mth",
  "subjectType": "ELECTIVE",
  "gradingSchemeDocsId": "67aa15d9dc3f7d0033333333"
}`,
        },
        {
          id: "03",
          name: "ONE SECTION'S ROW",
          expect: "200 OK",
          notes: `?sectionNo=A with a subject assigned to section A.
    OUT: only that row changed — section B's row for the same code is
    untouched, which is what the pair being the key means.`,
          body: `{
  "name": "Hindi (Section A)"
}`,
        },
        {
          id: "04",
          name: "THE SECTION IN LOWER CASE",
          expect: "200 OK",
          notes: `?sectionNo=a finds section A's row. Matched case-insensitively,
    and the stored sectionNo never changes.`,
          body: null,
        },
        {
          id: "05",
          name: "A BLANK SECTION PARAMETER",
          expect: "200 OK",
          notes: `?sectionNo= with nothing after it reaches the CLASS-WIDE row —
    the same as leaving the parameter off entirely.`,
          body: null,
        },
        {
          id: "06",
          name: "THE CODE AS IT WAS TYPED INTO #22",
          expect: "200 OK",
          notes: `Add a subject with subjectCode "maths-2", then PATCH .../maths-2.
    It is normalised to MATHS_2 and found. The create URL is a valid edit URL.`,
          body: null,
        },
        {
          id: "07",
          name: "ASKING FOR A SECTION WHEN IT IS CLASS-WIDE",
          expect: "404 Not Found",
          notes: `?sectionNo=A on a subject assigned to the whole class.
    OUT: { "code": "SUBJECT_NOT_FOUND" }
    The message says "drop ?sectionNo=" — #22 forbids both at once, so the
    row it is asking about cannot exist.`,
          body: null,
        },
        {
          id: "08",
          name: "ASKING CLASS-WIDE WHEN IT IS PER-SECTION",
          expect: "404 Not Found",
          notes: `No sectionNo, on a subject assigned to section A.
    OUT: the message names the section to use — "use ?sectionNo=A".`,
          body: null,
        },
        {
          id: "09",
          name: "CLEARING THE OPTIONAL FIELDS",
          expect: "200 OK",
          notes: `"" removes both. Clearing the scheme is not "no grading" — the
    resolution order falls through to Exam.gradingSchemeDocsId.`,
          body: `{
  "shortName": "",
  "gradingSchemeDocsId": ""
}`,
        },
        {
          id: "10",
          name: "CLEARING THE NAME",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "SUBJECT_NAME_REQUIRED" }
    Refused rather than cleared — the model requires a name, and it is the
    only thing on the row a person reads. Only spaces is the same refusal.`,
          body: `{
  "name": ""
}`,
        },
        {
          id: "11",
          name: "AN EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" }
    A PATCH that changes nothing and answers 200 lets a client with a broken
    form look healthy. An all-null body is the same refusal.`,
          body: `{}`,
        },
        {
          id: "12",
          name: "THE FIELDS THIS ENDPOINT WILL NOT CHANGE",
          expect: "200 OK",
          notes: `Sent alongside a real edit. All four are IGNORED, not refused —
    the code and section are the key, teachers are #25, active is #26/#27.
    OUT: only name changed; no RENAMED row appeared, the teachers survived.`,
          body: `{
  "subjectCode": "RENAMED",
  "sectionNo": "A",
  "teacherDocsIds": [],
  "active": false,
  "name": "Only this one lands"
}`,
        },
        {
          id: "13",
          name: "ANOTHER SCHOOL'S GRADING SCHEME",
          expect: "404 Not Found",
          notes: `A REAL grading scheme id belonging to a different school.
    OUT: { "code": "GRADING_SCHEME_NOT_FOUND" } — the lookup carries schoolId.
    A refused scheme leaves the row exactly as it was.`,
          body: `{
  "gradingSchemeDocsId": "67aa15d9dc3f7d0044444444"
}`,
        },
      ],
    },
    {
      id: "list-class-subjects",
      name: "List Subjects",
      method: "GET",
      path: "/schools/current/academic-years/{year}/classes/{id}/subjects",
      status: 'live',
      summary: "The class's subjects, or the ones one section studies.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{year}/classes/{id}/subjects?sectionNo=\` — endpoint #31.

### ?sectionNo= names an audience, not a row

**Leave it off** and the answer is every assignment the class holds. **Give it** and the answer is
what that section is taught — **its own rows *and* the class-wide ones**, because a row with no
\`sectionNo\` applies to every section.

Blank reads the same as absent.

### Why the union, and not a strict match

In a class where only the languages are split by section, a strict \`sectionNo == "A"\` answer
hides maths, science and everything else section A actually studies. On the test fixture, strict
returns **2 of the 4** rows section A is taught.

A class-wide row is still identifiable — it comes back with **no \`sectionNo\` field at all** — so
a caller that genuinely wants the strict set can filter in one line. The union is the default
because it is the answer to the question being asked.

### This is the one place ?sectionNo= differs from #24

On **#24** \`?sectionNo=A\` is half of a row's *key* — which row to edit. Here it is *taught to
whom*. Same word, two jobs. It is documented at both ends rather than renamed, because "which
section" is the honest reading of both.

### An unknown section is a 404, not an empty-ish 200

Without that check a typo answers with just the class-wide rows — **which looks exactly like a
real section that has nothing of its own**, and there are real sections like that.
\`?sectionNo=Z\` must not be indistinguishable from \`?sectionNo=C\`.

### The counts describe the class, not the slice

\`subjectCount\` stays 5 while 2 rows come back. The same rule #30 follows: "how many does this
class teach" is not "how many did you ask to see". A screen showing 3 of 11 needs both numbers.

### It reads the same document as #29

Same \`findById\`, same rows — asserted directly in the test suite. That is not a defect, it is
why the response shape is shared with #22 and #24. What this endpoint owns is **the question**,
not the query: it is the one place that knows what a section studies, so every caller does not
reimplement the union.

### No gate runs on it

A read, so a suspended or closed school still reads its own structure.

### The eleven test cases are in the notes below
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain, a year, and a class id.

 ?sectionNo= NAMES AN AUDIENCE, NOT A ROW. Off or blank = every assignment
 the class holds. Given = what that section is taught: ITS OWN ROWS PLUS
 THE CLASS-WIDE ONES, because a row with no sectionNo applies to every one.

 A STRICT MATCH WOULD HIDE MOST OF IT. On the fixture, strict returns 2 of
 the 4 rows section A studies. A class-wide row has NO sectionNo field, so
 a caller wanting the strict set can still filter in one line.

 THIS IS THE ONE PLACE ?sectionNo= DIFFERS FROM #24, where it is half of a
 row's key. Here it asks "taught to whom".

 AN UNKNOWN SECTION IS A 404, not an empty-ish 200 — a typo would otherwise
 look exactly like a real section with nothing of its own.

 THE COUNTS DESCRIBE THE CLASS, NOT THE SLICE. subjectCount stays 5 while
 2 rows come back.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the class belongs to. A real class id under the wrong year is a 404." },
        { name: "id", value: "{{schoolClassId}}", description: "The class's MongoDB document id, from Create Class." },
      ],
      queryParams: [
        { key: "sectionNo", value: "", enabled: false, description: "Whose subjects: absent or blank is the whole class, a value is what that section studies — its own rows plus the class-wide ones. Matched case-insensitively. An unknown section is a 404." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "Same shape Add Subject and Update Subject answer with, minus the changeSummary.",
      responseFields: ["schoolClassId", "className", "academicYear", "subjectCount", "activeCount", "subjects"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "No class with that id in that year — including a real id under the wrong year, or another school's." },
        { status: 404, code: "SECTION_NOT_FOUND", when: "The sectionNo sent is not a section of this class. Leave it out to read every subject the class teaches." },
      ],
      examples: [
        {
          id: "01",
          name: "EVERY SUBJECT THE CLASS TEACHES",
          expect: "200 OK",
          notes: `No parameters.
    OUT: every row — class-wide and per-section alike, with subjectCount
    and activeCount. No sections[]; that is #30's job.`,
          body: null,
        },
        {
          id: "02",
          name: "WHAT ONE SECTION STUDIES",
          expect: "200 OK",
          notes: `?sectionNo=A
    OUT: section A's own rows AND every class-wide row. This is the whole
    point of the endpoint — a strict match would hide most of it.`,
          body: null,
        },
        {
          id: "03",
          name: "ANOTHER SECTION DOES NOT SEE THE FIRST'S",
          expect: "200 OK",
          notes: `?sectionNo=B with a subject assigned only to A.
    OUT: B's own rows and the class-wide ones. A's are absent.`,
          body: null,
        },
        {
          id: "04",
          name: "A SECTION WITH NOTHING OF ITS OWN",
          expect: "200 OK",
          notes: `A section that has no per-section subject.
    OUT: the class-wide rows, NOT an empty list. It studies them like any
    other section — which is why an unknown section must be a 404 instead.`,
          body: null,
        },
        {
          id: "05",
          name: "THE CLASS-WIDE ROWS ARE STILL IDENTIFIABLE",
          expect: "200 OK",
          notes: `?sectionNo=A, then look at the rows.
    Those with no sectionNo field are the class's; those carrying "A" are
    the section's. One line of filtering gets the strict set if you want it.`,
          body: null,
        },
        {
          id: "06",
          name: "THE COUNTS DO NOT FOLLOW THE FILTER",
          expect: "200 OK",
          notes: `?sectionNo= a section with fewer rows than the class has.
    subjects.length is 2 and subjectCount is still 5. Deliberate: "how many
    does this class teach" is not "how many did you ask to see".`,
          body: null,
        },
        {
          id: "07",
          name: "THE SECTION IN LOWER CASE",
          expect: "200 OK",
          notes: `?sectionNo=a finds section A. Matched case-insensitively, and
    padding is trimmed.`,
          body: null,
        },
        {
          id: "08",
          name: "A BLANK PARAMETER",
          expect: "200 OK",
          notes: `?sectionNo= with nothing after it returns the WHOLE list —
    the same as leaving the parameter off.`,
          body: null,
        },
        {
          id: "09",
          name: "A SECTION THE CLASS DOES NOT HAVE",
          expect: "404 Not Found",
          notes: `?sectionNo=Z
    OUT: { "code": "SECTION_NOT_FOUND" }
    Refused rather than answered, because the class-wide rows alone look
    exactly like a real section that has nothing of its own.`,
          body: null,
        },
        {
          id: "10",
          name: "A CLASS WITH NO SUBJECTS",
          expect: "200 OK",
          notes: `OUT: subjects: [], subjectCount: 0. An empty list, never a 404 —
    a class created by #12 and not yet filled by #22.`,
          body: null,
        },
        {
          id: "11",
          name: "A SUSPENDED SCHOOL",
          expect: "200 OK",
          notes: `Suspend the school, then read.
    No gate runs on a read: a school still sees its own structure when it
    cannot change it.`,
          body: null,
        },
      ],
    },
    {
      id: "get-class-section",
      name: "Get Section",
      method: "GET",
      path: "/schools/current/academic-years/{year}/classes/{id}/sections/{sectionNo}",
      status: 'live',
      summary: "One section of one class, by its number.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{year}/classes/{id}/sections/{sectionNo}\` — endpoint #37.

### The only read in this group that is not a list

#30 answers "which sections does this class have". This answers "**this** section" — the
difference between a dropdown and a page. Without it, a caller wanting one section reads all of
them and picks in its own code, which is the duplication #31 was built to end for subjects.

### sectionNo is a path segment here, and that is deliberate

Three endpoints use the word for three different jobs, each in the position that matches:

- **#30** \`/sections\` — the list *is* the resource, no parameter at all
- **#31** \`/subjects?sectionNo=A\` — a **query** parameter, narrowing an audience
- **#37** \`/sections/A\` — a **path** segment, naming the thing being fetched

### A section is embedded, so the class comes with it

\`className\`, \`academicYear\` and \`schoolClassId\` are in the response. A section has no
identity away from its class — no collection, no document id, no \`schoolId\` of its own — so a
response holding only \`sectionNo\` would name something that means nothing on its own.

That is also why the number can never be changed: eight collections store it as a plain string.

### The counts describe the class, not the section

\`sectionCount\` and \`activeCount\` are the class's, the same rule #30 and #31 follow. "One of
three" is what a page shows beside a section's name.

### The subjects are not here

That is **#31 with \`?sectionNo=\`**, which applies the class-wide union — its own rows plus the
class's. Restating that rule in this response would mean two places to keep it right.

### Matched case-insensitively, and trimmed

"a" and "A" are one section everywhere else in this module, and a URL is not where that should
start to differ.

### No gate runs on it

A read, so a suspended or closed school still reads its own structure.

### The eight test cases are in the notes below
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain, a year, a class id and a section number.

 THE ONLY READ HERE THAT IS NOT A LIST. #30 answers "which sections are
 there"; this answers "this one".

 sectionNo IS A PATH SEGMENT, not a query parameter — it names the thing
 being fetched. #31 uses ?sectionNo= to narrow an audience instead; three
 endpoints, three jobs for one word.

 THE CLASS COMES WITH IT. A section is embedded and has no identity away
 from its class, so className and academicYear are in the response.

 THE COUNTS DESCRIBE THE CLASS, not the section — "one of three".

 THE SUBJECTS ARE NOT HERE. That is #31 with ?sectionNo=, which applies the
 class-wide union.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the class belongs to. A real class id under the wrong year is a 404." },
        { name: "id", value: "{{schoolClassId}}", description: "The class's MongoDB document id, from Create Class." },
        { name: "sectionNo", value: "A", description: "The section's number, as #17 stored it. Matched case-insensitively and trimmed." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "One section object, with the class around it and the class's counts.",
      responseFields: ["schoolClassId", "className", "academicYear", "sectionCount", "activeCount", "section"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "The {year} in the path is not a year of this school." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "No class with that id in that year — including a real id under the wrong year, or another school's." },
        { status: 404, code: "SECTION_NOT_FOUND", when: "That class has no section with that number." },
      ],
      examples: [
        {
          id: "01",
          name: "ONE SECTION",
          expect: "200 OK",
          notes: `sectionNo A on a class that has it.
    OUT: section is a single OBJECT, not a list — sectionNo, capacity,
    classTeacherDocsId and active, with the class around it.`,
          body: null,
        },
        {
          id: "02",
          name: "IT IS THE ROW #30 RETURNS",
          expect: "200 OK",
          notes: `Read #30, then this. The section object is field-for-field the
    same row. The shape is shared on purpose; what differs is the question.`,
          body: null,
        },
        {
          id: "03",
          name: "THE COUNTS ARE THE CLASS'S",
          expect: "200 OK",
          notes: `sectionCount is 3 on a class with three sections, even though one
    section came back. "One of three" is what a page shows.`,
          body: null,
        },
        {
          id: "04",
          name: "NO SUBJECTS IN THE RESPONSE",
          expect: "200 OK",
          notes: `Deliberate. What a section studies is #31 with ?sectionNo=, which
    applies the class-wide union — one place for that rule, not two.`,
          body: null,
        },
        {
          id: "05",
          name: "THE NUMBER IN LOWER CASE",
          expect: "200 OK",
          notes: `sectionNo a finds section A. Matched case-insensitively and
    trimmed, as everywhere else in this module.`,
          body: null,
        },
        {
          id: "06",
          name: "A DIFFERENT SECTION",
          expect: "200 OK",
          notes: `sectionNo B returns B — the check that the number is actually read
    rather than the first section being handed back.`,
          body: null,
        },
        {
          id: "07",
          name: "A SECTION THE CLASS DOES NOT HAVE",
          expect: "404 Not Found",
          notes: `OUT: { "code": "SECTION_NOT_FOUND" }, naming the class it looked in.`,
          body: null,
        },
        {
          id: "08",
          name: "A SUSPENDED SCHOOL",
          expect: "200 OK",
          notes: `Suspend the school, then read. No gate runs on a read.`,
          body: null,
        },
      ],
    },
  ],
};

const GROUP_ACADEMICS_GRADING = {
  id: "academics-grading",
  module: "Academics / Grading",
  endpoints: [
    {
      id: "create-grading-scheme",
      name: "Create Grading Scheme",
      method: "POST",
      path: "/schools/current/grading-schemes",
      status: 'live',
      summary: "Define a rulebook and its bands in one write.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/grading-schemes\` — endpoint #1.

### The school's rulebook for turning a mark into a grade

A teacher enters **94**. Whether that is an \`A1\`, a \`7\`, or "Outstanding" is not something the
number says — it is something the school decides once, here, and every exam and report card then
reads.

### No {year} in the path, and no gate 4

**A rulebook outlives a year.** The same scheme grades 2026-2027 and 2027-2028, and a report card
from either must reprint identically five years later. \`schemeVersion\` is what moves when the
*rules* move, not the calendar.

Which is why only **two** gates run: gate 4 asks whether a named academic year is the school's
working one, and there is no year here to ask it about. **This is the one academics surface that
still answers after \`POST /academic-years/{name}/end\`** — and it has to, because correcting a 2026
report card means reading the 2026 scheme.

### The scale decides the shape of everything under it

| scaleType | maximumValue | bands |
|---|---|---|
| \`PERCENTAGE\` | **required** — 100 | bounded, ordered, no overlaps |
| \`MARKS\` | **required** — the paper total: 50, 25, 80 | bounded, ordered, no overlaps |
| \`DESCRIPTOR\` | **refused** | **unbounded** — a grade chosen, not computed |

**The scale names what a teacher ENTERS, never what comes out.** The awarded grade is a band's
\`gradeCode\` and its weight in an aggregate is \`gradePoint\` — both mapped *from* a value on one of
these scales. \`POINT\` was renamed to \`MARKS\` on 2026-09-14 because it read as a grade point, and a
scheme keyed on grade points would be mapping grade points onto grade points.

**An IB scheme is \`PERCENTAGE\`, not a "1–7 scale"** — the 1–7 is what IB *awards*, so the codes
are \`"7"\` to \`"1"\` and a raw score resolves the band. IB also sets boundaries per subject per
session, so each session is a new \`schemeVersion\`.

The scale is read **first**, before a single band is looked at. Asking about a band's bounds before
knowing the scale gives the right refusal for the wrong reason: *"this band needs a minimum"* when
the truth is *"this scheme measures nothing"*.

\`DESCRIPTOR\` was impossible to store until 2026-09-13 — \`GradeBand\`'s bounds were \`@NotNull\`, so a
descriptor scheme had to invent numbers for fields nothing reads. They are nullable now, and the
rule moved to the service where the scale is visible.

### An overlap is refused; a gap is only reported

**And the asymmetry is the whole point.** A gap means one mark has *no* grade — visible, and
fixable by whoever reads the warning. An overlap means one mark has *two*, and which wins depends
on the order the bands happen to be stored in — silently, differently, per scheme.

**Bounds are inclusive at both ends, so bands must not touch**: \`81..90\` beside \`90..100\` both
claim 90 and is a \`409\`.

### Which means a scale can never be tiled, and gaps are counted in whole marks

\`81..90\` beside \`91..100\` does not cover \`90.5\`. Closing that is an overlap. **There is no third
option** — every percentage scheme ever written has slivers, including the CBSE one.

So a gap is reported only when **a whole mark can land in it**:

| bands | gap | reported? |
|---|---|---|
| \`81..90\`, \`91..100\` | 90 → 91 | **no** — no whole mark fits |
| \`0..32\`, \`81..90\` | 32 → 81 | **yes** — 33 through 80 |
| \`0..99\` out of 100 | 99 → 100 | **yes** — 100 itself has no grade |
| \`1..100\` | 0 → 1 | **yes** — 0 itself has no grade |

The last two rows are why the three edges differ: the scale is \`[0, max]\` with **both ends
gradeable**, so a leading gap is closed at 0, a middle gap is open at both ends, and a trailing gap
is closed at the ceiling.

**The cost is fractional marks.** A school awarding 90.5 gets no warning that it has no grade — #8
answers \`404\` for it, which is where it is discoverable.

### name + schemeVersion is the key, so there is no rename

Unique together per school. **Neither can be changed afterwards**, and there is deliberately no
\`PATCH\` at all: every field is either half the key, a reinterpretation of every band beneath it
(\`scaleType\`, \`maximumValue\`), the history itself (\`gradeBands\`), or an event with its own
endpoints (\`active\`). An endpoint with no legal field is not an endpoint.

Moving a boundary means **a new version** (#2) — editing in place would rewrite every report card
ever issued under the old one.

The index named a \`schemeCode\` field that never existed anywhere in the project until 2026-09-13.
MongoDB indexes a missing field as null, so it read \`{schoolId, null, schemeVersion}\` — one version
string per school, making "CBSE Percentage" 2026.1 and "IB Points" 2026.1 mutually exclusive.

### Bands are stored in the order given

Never re-sorted. A school listing \`A1\` first means A1 first, and silently reordering makes a typo
hard to spot against the paper it was copied from. The *checks* sort a copy, because overlap is a
question about the set rather than about the list.

### The gates

**1** school ACTIVE · **2** subscription usable. No gate 4 — see above.

### The ten test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain. NO academic year — a rulebook outlives one.

 THE SCALE IS READ FIRST. PERCENTAGE and MARKS require maximumValue and
 bounded bands; DESCRIPTOR refuses both. Asking about a band before knowing
 the scale gives the right refusal for the wrong reason.

 IT NAMES WHAT A TEACHER ENTERS, never what comes out. gradeCode is the
 awarded grade and gradePoint is what it is worth - both mapped FROM a value
 on one of these scales. An IB scheme is PERCENTAGE with codes "7" to "1".

 AN OVERLAP IS REFUSED, A GAP IS ONLY REPORTED. A gap means one mark has no
 grade - visible and fixable. An overlap means one mark has two, and which
 wins depends on storage order.

 BOUNDS ARE INCLUSIVE AT BOTH ENDS, so bands must not touch: 81..90 beside
 90..100 both claim 90.

 WHICH MEANS A SCALE CANNOT BE TILED. 81..90 beside 91..100 misses 90.5, and
 closing it is an overlap. So gaps are counted in WHOLE MARKS: 90 -> 91 is
 silent, 32 -> 81 is not.

 name + schemeVersion IS THE KEY. Neither changes, and there is no PATCH at
 all. Moving a boundary means a new version (#2).

 NO GATE 4. There is no year in this path to ask it about, so this is the one
 academics surface that still answers after the year has been ended.`,
      requiredFields: ["name", "schemeVersion", "scaleType", "gradeBands"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "CBSE Percentage Grading",
  "schemeVersion": "2026.1",
  "scaleType": "PERCENTAGE",
  "maximumValue": 100,
  "gradeBands": [
    { "gradeCode": "A1", "minimumValue": 91, "maximumValue": 100, "gradePoint": 10, "description": "Outstanding" },
    { "gradeCode": "A2", "minimumValue": 81, "maximumValue": 90, "gradePoint": 9, "description": "Excellent" },
    { "gradeCode": "B1", "minimumValue": 71, "maximumValue": 80, "gradePoint": 8, "description": "Very Good" },
    { "gradeCode": "B2", "minimumValue": 61, "maximumValue": 70, "gradePoint": 7, "description": "Good" },
    { "gradeCode": "C1", "minimumValue": 51, "maximumValue": 60, "gradePoint": 6, "description": "Fair" },
    { "gradeCode": "C2", "minimumValue": 41, "maximumValue": 50, "gradePoint": 5, "description": "Average" },
    { "gradeCode": "D", "minimumValue": 33, "maximumValue": 40, "gradePoint": 4, "description": "Below Average" },
    { "gradeCode": "E", "minimumValue": 0, "maximumValue": 32, "gradePoint": 0, "description": "Needs Improvement", "passed": false }
  ]
}`,
      successStatus: 201,
      successNote: "Also sends a Location header pointing at the scheme by its id. A warning rides on the 201 when the bands leave a whole mark ungraded.",
      responseFields: ["gradingSchemeDocsId", "name", "schemeVersion", "scaleType", "maximumValue", "bandCount", "gradeBands", "active", "warning", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "A missing name, schemeVersion, scaleType or gradeBands; an empty band list; a band with no gradeCode; more than 40 bands." },
        { status: 400, code: "SCALE_MAXIMUM_REQUIRED", when: "A PERCENTAGE or MARKS scheme with no maximumValue, or one that is zero or below." },
        { status: 400, code: "GRADE_BAND_BOUNDS_REQUIRED", when: "A PERCENTAGE or MARKS band missing a bound. One bound alone is not a range." },
        { status: 400, code: "GRADE_BAND_BOUNDS_NOT_ALLOWED", when: "A DESCRIPTOR band carrying bounds, or a DESCRIPTOR scheme carrying maximumValue." },
        { status: 400, code: "INVALID_GRADE_BAND_RANGE", when: "One band's maximumValue is below its minimumValue. Equal bounds are legal — a one-value band is odd, not wrong." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "GRADE_BAND_CODE_TAKEN", when: "Two bands share a gradeCode, case-insensitively." },
        { status: 409, code: "GRADE_BAND_OUTSIDE_SCALE", when: "A band reaches past maximumValue or below zero — a grade no mark could ever reach." },
        { status: 409, code: "GRADE_BANDS_OVERLAP", when: "Two bands cover the same value. Bounds are inclusive at both ends, so touching counts." },
        { status: 409, code: "SCHEME_VERSION_TAKEN", when: "This school already has that name at that version." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
      ],
      examples: [
        {
          id: "01",
          name: "THE CBSE SCALE",
          expect: "201 Created",
          notes: `The body above — eight bands, fully covering 0 to 100.
    OUT: gradingSchemeDocsId, bandCount 8, active true, and NO warning:
    every whole mark from 0 to 100 has a grade.`,
          body: null,
        },
        {
          id: "02",
          name: "AN IB SCHEME — 1 TO 7 AS THE AWARDED GRADE",
          expect: "201 Created",
          notes: `IB's 1-7 is the grade AWARDED, not the number a teacher enters. So the
    codes are "7" to "1" and the scale is PERCENTAGE: a raw score resolves the
    band, and gradePoint carries the 1-7 for aggregation.

    THE BOUNDARIES MOVE EVERY SESSION. IB sets them per subject after the
    papers are marked, so a 7 is roughly 75-85% and a raw 72% can be a 7 one
    session and a 6 the next. That is what schemeVersion is for - one version
    per session, and the name says which.`,
          body: `{
  "name": "IB Diploma HL — May 2026 boundaries",
  "schemeVersion": "2026.5",
  "scaleType": "PERCENTAGE",
  "maximumValue": 100,
  "gradeBands": [
    { "gradeCode": "7", "minimumValue": 84, "maximumValue": 100, "gradePoint": 7, "description": "Excellent" },
    { "gradeCode": "6", "minimumValue": 72, "maximumValue": 83, "gradePoint": 6, "description": "Very good" },
    { "gradeCode": "5", "minimumValue": 59, "maximumValue": 71, "gradePoint": 5, "description": "Good" },
    { "gradeCode": "4", "minimumValue": 46, "maximumValue": 58, "gradePoint": 4, "description": "Satisfactory" },
    { "gradeCode": "3", "minimumValue": 33, "maximumValue": 45, "gradePoint": 3, "description": "Mediocre" },
    { "gradeCode": "2", "minimumValue": 20, "maximumValue": 32, "gradePoint": 2, "description": "Poor" },
    { "gradeCode": "1", "minimumValue": 0, "maximumValue": 19, "gradePoint": 1, "description": "Very poor", "passed": false }
  ]
}`,
        },
        {
          id: "02b",
          name: "A PAPER MARKED OUT OF 50",
          expect: "201 Created",
          notes: `This is what MARKS is for: a raw total that is not 100. Mechanically
    the same walk as PERCENTAGE — the difference is the reported figure, since
    "43 / 50" is not "86%", which is why ReportCardSubjectResult carries
    maximumMarks and percentage as two fields.`,
          body: `{
  "name": "Unit Test — out of 50",
  "schemeVersion": "2026.1",
  "scaleType": "MARKS",
  "maximumValue": 50,
  "gradeBands": [
    { "gradeCode": "A", "minimumValue": 40, "maximumValue": 50, "gradePoint": 10 },
    { "gradeCode": "B", "minimumValue": 30, "maximumValue": 39, "gradePoint": 8 },
    { "gradeCode": "C", "minimumValue": 17, "maximumValue": 29, "gradePoint": 6 },
    { "gradeCode": "D", "minimumValue": 0, "maximumValue": 16, "gradePoint": 0, "passed": false }
  ]
}`,
        },
        {
          id: "03",
          name: "A DESCRIPTOR SCALE, WITH NO NUMBERS AT ALL",
          expect: "201 Created",
          notes: `No maximumValue, and no bounds on any band. This was impossible to
    store until 2026-09-13, when GradeBand's bounds stopped being @NotNull.
    OUT: no warning — there is no scale to leave holes in.`,
          body: `{
  "name": "Early Years Descriptors",
  "schemeVersion": "2026.1",
  "scaleType": "DESCRIPTOR",
  "gradeBands": [
    { "gradeCode": "SECURE", "description": "Secure" },
    { "gradeCode": "DEVELOPING", "description": "Developing" },
    { "gradeCode": "BEGINNING", "description": "Beginning", "passed": false }
  ]
}`,
        },
        {
          id: "04",
          name: "A DESCRIPTOR SCHEME CARRYING A CEILING",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "GRADE_BAND_BOUNDS_NOT_ALLOWED" }. Send case 03 with
    "maximumValue": 100 added. The scale is checked BEFORE any band, so this
    is what you hear about first.`,
          body: null,
        },
        {
          id: "05",
          name: "TWO BANDS THAT TOUCH",
          expect: "409 Conflict",
          notes: `OUT: { "code": "GRADE_BANDS_OVERLAP" }, naming both and the value
    they share. Bounds are inclusive at BOTH ends, so 90 belongs to each —
    unlike term dates, where the 30th and the 1st share no day.`,
          body: `{
  "name": "Touching",
  "schemeVersion": "1",
  "scaleType": "PERCENTAGE",
  "maximumValue": 100,
  "gradeBands": [
    { "gradeCode": "A2", "minimumValue": 81, "maximumValue": 90 },
    { "gradeCode": "A1", "minimumValue": 90, "maximumValue": 100 }
  ]
}`,
        },
        {
          id: "06",
          name: "A HOLE A WHOLE MARK FITS IN",
          expect: "201 Created",
          notes: `ACCEPTED, with a warning naming "32 to 81". Marks 33 to 80 resolve
    to nothing. Reported rather than refused: a gap a school left on purpose
    is indistinguishable from one it did not mean.`,
          body: `{
  "name": "Gappy",
  "schemeVersion": "1",
  "scaleType": "PERCENTAGE",
  "maximumValue": 100,
  "gradeBands": [
    { "gradeCode": "E", "minimumValue": 0, "maximumValue": 32, "passed": false },
    { "gradeCode": "A2", "minimumValue": 81, "maximumValue": 90 },
    { "gradeCode": "A1", "minimumValue": 91, "maximumValue": 100 }
  ]
}`,
        },
        {
          id: "07",
          name: "AN UNGRADED CEILING",
          expect: "201 Created",
          notes: `ACCEPTED, with a warning naming "99 to 100". The trailing edge is
    CLOSED at the ceiling — 100 itself has no grade, and no other edge would
    notice. One mark, and still worth saying.`,
          body: `{
  "name": "Short",
  "schemeVersion": "1",
  "scaleType": "PERCENTAGE",
  "maximumValue": 100,
  "gradeBands": [
    { "gradeCode": "ALL", "minimumValue": 0, "maximumValue": 99 }
  ]
}`,
        },
        {
          id: "08",
          name: "A BAND PAST THE CEILING",
          expect: "409 Conflict",
          notes: `OUT: { "code": "GRADE_BAND_OUTSIDE_SCALE" }. A grade no mark could
    ever reach is a row a school would stare at without working out why it
    never appears.`,
          body: `{
  "name": "Overshoot",
  "schemeVersion": "1",
  "scaleType": "MARKS",
  "maximumValue": 7,
  "gradeBands": [
    { "gradeCode": "EIGHT", "minimumValue": 7.5, "maximumValue": 8 }
  ]
}`,
        },
        {
          id: "09",
          name: "THE SAME NAME AND VERSION AGAIN",
          expect: "409 Conflict",
          notes: `Send case 01 twice. OUT: { "code": "SCHEME_VERSION_TAKEN" }.
    The pair is the key — the same NAME at a different version is fine, and
    is what #2 exists to create.`,
          body: null,
        },
      ],
    },
    {
      id: "update-grading-scheme",
      name: "Update Grading Scheme",
      method: "PATCH",
      path: "/schools/current/grading-schemes/{id}",
      status: 'live',
      summary: "Change any field of a scheme nothing has used. Only active once something has.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/grading-schemes/{id}\` — endpoint #3.

### Every field is editable, and mostly this endpoint cannot be used

The plan for this module said there would be **no \`PATCH\` at all**, reasoning field by field: the
key cannot move, the scale reinterprets every band, the bands are the history. Every one of those
is true of a scheme **something has used** — and none is true of one nothing has. A school that
mistypes a boundary during setup should not have to publish version 2 to fix version 1.

**So the rule moved from the field to the state:**

| | |
|---|---|
| nothing references this scheme | every field below is editable |
| something references it | only \`active\` is, and the rest is \`409\` |

### active is the exception, and the only one that could be

It is the single field that does not change what a **printed grade** means. Retiring a scheme
everything uses is exactly what a school does when it publishes the next version — the old cards
still resolve through it, they are simply not offered for new work.

**This is why #4 and #5 were dropped.** They existed to set \`active\`, and #3 sets it both ways on
exactly the schemes they would have.

### Every rule #1 applies is re-applied to the RESULTING scheme

Not to the body — which is what makes a half-change safe to send:

- **Lowering \`maximumValue\`** is checked against the bands you did *not* send →
  \`409 GRADE_BAND_OUTSIDE_SCALE\`
- **Switching to \`DESCRIPTOR\`** is checked against stored bands that still carry bounds →
  \`400 GRADE_BAND_BOUNDS_NOT_ALLOWED\`. Send the new bands in the same request; the two fields are
  one change.

### maximumValue is DERIVED on a descriptor scale

The one place absent does **not** mean "leave it alone". A PATCH has no way to send *"remove this
number"*, so keeping the stored ceiling would make \`PERCENTAGE → DESCRIPTOR\` **impossible** —
every such request would \`400\` with nothing the caller could do about it.

It is not a guess: there is exactly one legal value, absent. The reverse needs no rule, because
\`SCALE_MAXIMUM_REQUIRED\` says what to send.

### The band set is replaced whole, never patched

Send \`gradeBands\` and every band changes; leave it out and none do. There is no per-band edit,
because adding or moving one always risks an overlap or a gap with its neighbours and the checks
that catch those read the entire set.

An empty list is \`400 GRADE_BANDS_REQUIRED\` — clearing the bands is not a way to retire a scheme.

### The reference check is incomplete, and the message says so

Only \`school_classes\` is reachable: \`exams\` and \`report_cards\` store the same
\`gradingSchemeDocsId\` and have no repository, because neither has an endpoint to write a row. The
refusal names what was actually checked, so a pass is never read as a guarantee.

### Nothing is written until every check has passed

A refusal leaves the scheme exactly as it was.

### The gates

**1** school ACTIVE · **2** subscription usable. No gate 4 — there is no academic year in this
path to ask it about.

### The nine test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain. NO academic year. Every field is optional.

 EVERY FIELD IS EDITABLE - while NOTHING references the scheme. The moment a
 subject, exam or report card points at it, only \`active\` is, and everything
 else is 409 SCHEME_STILL_REFERENCED. The rule is about the STATE, not the
 field: a boundary moved under a printed card rewrites that card silently.

 active IS THE EXCEPTION because it is the one field that does not change
 what a printed grade means. Retiring a scheme everything uses is exactly
 what a school does when it publishes the next version.

 EVERY RULE #1 APPLIES IS RE-APPLIED TO THE RESULTING SCHEME, not the body.
 Lowering maximumValue is checked against bands you did NOT send; switching
 to DESCRIPTOR is checked against stored bands that still carry bounds.

 maximumValue IS DERIVED ON A DESCRIPTOR SCALE - the one place absent does
 not mean "leave it alone". Without it, PERCENTAGE -> DESCRIPTOR would be
 impossible: there is no way to send "remove this number".

 THE BAND SET IS REPLACED WHOLE OR NOT AT ALL. An empty list is a 400 -
 clearing the bands is not a way to retire a scheme.

 NOTHING IS WRITTEN until every check passes, so a refusal changes nothing.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{gradingSchemeDocsId}}", description: "The scheme's document id — gradingSchemeDocsId." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "name": "CBSE Percentage Grading (corrected)"
}`,
      successStatus: 200,
      successNote: "The scheme as it now is. A warning rides on it when the bands leave a whole mark ungraded — recomputed from what was just stored.",
      responseFields: ["gradingSchemeDocsId", "name", "schemeVersion", "scaleType", "maximumValue", "bandCount", "gradeBands", "active", "warning", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "A body that asks for nothing. Checked before anything is read." },
        { status: 400, code: "SCHEME_KEY_REQUIRED", when: "\"name\": \"\" or \"schemeVersion\": \"\" — the key can be replaced, never removed." },
        { status: 400, code: "GRADE_BANDS_REQUIRED", when: "An empty gradeBands list. Clearing the bands is not a way to retire a scheme — send active false." },
        { status: 400, code: "SCALE_MAXIMUM_REQUIRED", when: "Switching AWAY from DESCRIPTOR without sending a ceiling." },
        { status: 400, code: "GRADE_BAND_BOUNDS_REQUIRED", when: "A PERCENTAGE or MARKS band missing a bound." },
        { status: 400, code: "GRADE_BAND_BOUNDS_NOT_ALLOWED", when: "Switching to DESCRIPTOR while the stored bands still carry bounds. Send the new bands in the same request." },
        { status: 400, code: "INVALID_GRADE_BAND_RANGE", when: "A band whose maximumValue is below its minimumValue." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "GRADING_SCHEME_NOT_FOUND", when: "No scheme with that id in this school." },
        { status: 409, code: "SCHEME_STILL_REFERENCED", when: "A subject already grades by this scheme. Everything else is #2; retiring it is still allowed, through #4." },
        { status: 409, code: "SCHEME_VERSION_TAKEN", when: "The new name + version pair is already this school's." },
        { status: 409, code: "GRADE_BAND_CODE_TAKEN", when: "Two bands in the new set share a gradeCode." },
        { status: 409, code: "GRADE_BAND_OUTSIDE_SCALE", when: "A band reaches past the ceiling — including a ceiling you lowered under bands you did not send." },
        { status: 409, code: "GRADE_BANDS_OVERLAP", when: "Two bands in the resulting set cover the same value." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
      ],
      examples: [
        {
          id: "01",
          name: "A RENAME, AND NOTHING ELSE",
          expect: "200 OK",
          notes: `The body above. Everything not mentioned is untouched — the bands, the
    scale and the ceiling all stay exactly as they were.`,
          body: null,
        },
        {
          id: "02",
          name: "AN EMPTY BODY",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" }. Checked before the scheme is
    even read, so an empty PATCH on a scheme that does not exist says the
    body is empty rather than sending you hunting for a missing scheme.`,
          body: `{}`,
        },
        {
          id: "03",
          name: "LOWER THE CEILING UNDER A BAND YOU DID NOT SEND",
          expect: "409 Conflict",
          notes: `On the CBSE scale, send maximumValue 50. OUT:
    { "code": "GRADE_BAND_OUTSIDE_SCALE" } — A1 still reaches 100.
    THIS IS THE POINT OF CHECKING THE RESULTING SCHEME rather than the body:
    the bands you left alone are still checked against the ceiling you sent.`,
          body: `{
  "maximumValue": 50
}`,
        },
        {
          id: "04",
          name: "SWITCH TO DESCRIPTOR WITHOUT NEW BANDS",
          expect: "400 Bad Request",
          notes: `OUT: { "code": "GRADE_BAND_BOUNDS_NOT_ALLOWED" }. The stored bands
    still carry bounds, and a descriptor band must have none. The scale and
    the bands are ONE change — send them together, as case 05 does.`,
          body: `{
  "scaleType": "DESCRIPTOR"
}`,
        },
        {
          id: "05",
          name: "SWITCH TO DESCRIPTOR, PROPERLY",
          expect: "200 OK",
          notes: `OUT: scaleType DESCRIPTOR and maximumValue GONE — derived, not sent.
    A PATCH cannot express "remove this number", so without that rule this
    switch would be impossible.`,
          body: `{
  "scaleType": "DESCRIPTOR",
  "gradeBands": [
    { "gradeCode": "SECURE", "description": "Secure" },
    { "gradeCode": "DEVELOPING", "description": "Developing" },
    { "gradeCode": "BEGINNING", "description": "Beginning", "passed": false }
  ]
}`,
        },
        {
          id: "06",
          name: "AND BACK AGAIN, WITHOUT A CEILING",
          expect: "400 Bad Request",
          notes: `After case 05, send scaleType PERCENTAGE with bands but no
    maximumValue. OUT: { "code": "SCALE_MAXIMUM_REQUIRED" }, which says what
    to send. The reverse switch needs no derivation rule for that reason.`,
          body: `{
  "scaleType": "PERCENTAGE",
  "gradeBands": [
    { "gradeCode": "A", "minimumValue": 0, "maximumValue": 100 }
  ]
}`,
        },
        {
          id: "07",
          name: "REPLACE THE WHOLE BAND SET",
          expect: "200 OK",
          notes: `Whole or not at all — there is no per-band edit, because adding or
    moving one band always risks an overlap or a gap with its neighbours.`,
          body: `{
  "gradeBands": [
    { "gradeCode": "P", "minimumValue": 33, "maximumValue": 100, "gradePoint": 10 },
    { "gradeCode": "F", "minimumValue": 0, "maximumValue": 32, "gradePoint": 0, "passed": false }
  ]
}`,
        },
        {
          id: "08",
          name: "ONCE A SUBJECT USES IT",
          expect: "409 Conflict",
          notes: `Attach the scheme to a subject with Add Subject, then send ANY of the
    cases above. OUT: { "code": "SCHEME_STILL_REFERENCED" }, naming the
    scheme and saying which collections were actually checked.
    THIS IS THE ENDPOINT'S WHOLE GUARD.`,
          body: null,
        },
        {
          id: "09",
          name: "EXCEPT active, WHICH STILL WORKS",
          expect: "200 OK",
          notes: `Same referenced scheme as case 08. Retiring one everything uses is
    exactly what a school does when it publishes the next version — the old
    report cards still resolve through it. Send true to put it back.`,
          body: `{
  "active": false
}`,
        },
      ],
    },
    {
      id: "deactivate-grading-scheme",
      name: "Deactivate Grading Scheme",
      method: "POST",
      path: "/schools/current/grading-schemes/{id}/deactivate",
      status: 'live',
      summary: "Stop offering this scheme for new work. Old cards still resolve through it.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/grading-schemes/{id}/deactivate\` — endpoint #4.

### Retiring does not stop a scheme resolving

\`active\` governs what is **offered for new work**, and nothing else. #7 and #8 answer for a retired
version and must: a report card issued in 2026 reprints through the 2026 rules long after the
school moved to 2027's.

### No reference check — unlike #3

Retiring a scheme everything uses is exactly what a school does when it publishes the next version.
**So the one state #3 refuses outright is the state this endpoint is for.**

### A POST, not a field on #3

**Every lifecycle flag in this project is a named POST event** — \`/results/lock\` on a term,
\`/enrollment/enable\` on a year, eight across two modules — and \`AcademicTermUpdateRequest\` refuses
\`active\` in these exact words: *"events with meanings, not fields to toggle in passing"*.

\`active\` briefly lived on #3's PATCH on 2026-09-14 and moved back here the same day. A grading
scheme is not the one document that should differ.

### Idempotent

Asking for a state it is already in is a **200 saying so**, never a 409. A refusal would turn
"make sure this is retired" — the thing a caller actually wants — into a request it has to read
the state before daring to send.

### It writes \`active\` and nothing else

Not the bands, not the key, not the scale. Those are #3, and #3 refuses them outright on a scheme
anything references.

### The gates

**1** school ACTIVE · **2** subscription usable. No gate 4 — there is no academic year here.

### The four test cases are in the notes below
`,
      bodyNotes: `A POST with NO BODY. Needs X-School-Subdomain. NO academic year.

 RETIRING DOES NOT STOP THE SCHEME RESOLVING. active governs what is OFFERED
 for new work. #7 and #8 answer for a retired version - a card issued in 2026
 reprints through the 2026 rules forever.

 NO REFERENCE CHECK, unlike #3. Retiring a scheme everything uses is exactly
 what a school does when publishing the next version, so the one state #3
 refuses outright is the state this endpoint is FOR.

 IDEMPOTENT. Already retired is a 200 saying so, never a 409.

 A POST, NOT A FIELD ON #3. Every lifecycle flag in this project is a named
 POST event - eight across core and terms. active lived on #3's PATCH for one
 day and moved back here.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{gradingSchemeDocsId}}", description: "The scheme's document id." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "Returns the scheme with active false. It still reads and resolves — only #6\u2019s ?active=true filter hides it.",
      responseFields: ["gradingSchemeDocsId", "name", "schemeVersion", "scaleType", "maximumValue", "bandCount", "gradeBands", "active", "warning", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "GRADING_SCHEME_NOT_FOUND", when: "No scheme with that id in this school." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
      ],
      examples: [
        {
          id: "01",
          name: "RETIRE A SCHEME",
          expect: "200 OK",
          notes: `No body. OUT: active false, and a nextStep saying it still resolves
    every report card issued under it — retiring never changes a grade
    already printed.`,
          body: null,
        },
        {
          id: "02",
          name: "RETIRE IT AGAIN",
          expect: "200 OK",
          notes: `OUT: still 200, nextStep says "was already retired. Nothing changed."
    NOT a 409 — the caller asked for a state, not a transition.`,
          body: null,
        },
        {
          id: "03",
          name: "IT STILL READS AND RESOLVES",
          expect: "200 OK",
          notes: `Run Get Grading Scheme on it afterwards. Still 200, bands and all.
    #6 hides it from ?active=true; nothing else changes.`,
          body: null,
        },
        {
          id: "04",
          name: "ON A SCHEME #3 REFUSES ENTIRELY",
          expect: "200 OK",
          notes: `Attach the scheme to a subject, then try PATCH — 409
    SCHEME_STILL_REFERENCED. Then send this: 200. THE ONE STATE #3 REFUSES
    IS THE STATE THIS ENDPOINT IS FOR.`,
          body: null,
        },
      ],
    },
    {
      id: "reactivate-grading-scheme",
      name: "Reactivate Grading Scheme",
      method: "POST",
      path: "/schools/current/grading-schemes/{id}/reactivate",
      status: 'live',
      summary: "Offer this scheme for new work again. There is no DELETE, so this is the way back.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/grading-schemes/{id}/reactivate\` — endpoint #5.

### It exists because there is no DELETE

A school that retired the wrong version needs a way back that is not a third version. Nothing in
this module deletes anything — three places store \`gradingSchemeDocsId\` and none of those
references is a foreign key, so a deleted scheme would leave every one of them pointing at nothing
while everything still looked valid.

### A POST, not a field on #3

**Every lifecycle flag in this project is a named POST event** — \`/results/lock\` on a term,
\`/enrollment/enable\` on a year, eight across two modules — and \`AcademicTermUpdateRequest\` refuses
\`active\` in these exact words: *"events with meanings, not fields to toggle in passing"*.

\`active\` briefly lived on #3's PATCH on 2026-09-14 and moved back here the same day. A grading
scheme is not the one document that should differ.

### Idempotent

Asking for a state it is already in is a **200 saying so**, never a 409. A refusal would turn
"make sure this is retired" — the thing a caller actually wants — into a request it has to read
the state before daring to send.

### It writes \`active\` and nothing else

Not the bands, not the key, not the scale. Those are #3, and #3 refuses them outright on a scheme
anything references.

### The gates

**1** school ACTIVE · **2** subscription usable. No gate 4 — there is no academic year here.

### The three test cases are in the notes below
`,
      bodyNotes: `A POST with NO BODY. Needs X-School-Subdomain. NO academic year.

 IT EXISTS BECAUSE THERE IS NO DELETE. Three places store
 gradingSchemeDocsId and none of those references is a foreign key, so a
 deleted scheme would leave them all pointing at nothing while everything
 still looked valid.

 IDEMPOTENT. Already active is a 200 saying so.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{gradingSchemeDocsId}}", description: "The scheme's document id." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "Returns the scheme with active true.",
      responseFields: ["gradingSchemeDocsId", "name", "schemeVersion", "scaleType", "maximumValue", "bandCount", "gradeBands", "active", "warning", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "GRADING_SCHEME_NOT_FOUND", when: "No scheme with that id in this school." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
      ],
      examples: [
        {
          id: "01",
          name: "PUT IT BACK",
          expect: "200 OK",
          notes: `After #4. OUT: active true, offered for new work again.`,
          body: null,
        },
        {
          id: "02",
          name: "AGAIN",
          expect: "200 OK",
          notes: `OUT: "was already active. Nothing changed." Idempotent, same as #4.`,
          body: null,
        },
        {
          id: "03",
          name: "ON A REFERENCED SCHEME",
          expect: "200 OK",
          notes: `Works the same. Neither #4 nor #5 runs a reference check — the flag
    changes nothing a report card already resolved.`,
          body: null,
        },
      ],
    },
    {
      id: "get-grading-scheme",
      name: "Get Grading Scheme",
      method: "GET",
      path: "/schools/current/grading-schemes/{id}",
      status: 'live',
      summary: "One scheme with every band, in stored order.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/grading-schemes/{id}\` — endpoint #7.

### The only endpoint that returns the bands

#6 trims them to a \`bandCount\`, because a page of full band tables is hundreds of values nobody
reads. This is where a school goes to check its own boundaries.

### Bands come back in the order they were written

**Never re-sorted.** A school listing \`A1\` first means A1 first, and a response that silently
reordered them would make a typo hard to spot against the paper they were copied from. #1's checks
sort a *copy*, because overlap is a question about the set rather than about the list.

So a scheme entered \`E, A2, A1\` reads back \`E, A2, A1\`.

### The gap warning is recomputed, never stored

A school that ignored the warning on create should still see it **every time it looks** — and a
stored sentence would outlive the problem it described, so a scheme whose bands were later fixed
would keep being warned about a hole that is no longer there.

That is why \`GradingHelper.gapWarning\` takes **stored bands** rather than a request — the one check
in that class which does. One signature instead of an overload per caller, and #1 and #7 compute
the warning from exactly the same input: the document.

### It answers for a retired scheme

\`active\` governs what is offered for **new work** and nothing else. A report card issued in 2026
has to reprint through the 2026 rules long after the school moved to 2027's, so #7, #8 and #9 all
answer for an inactive version.

### Addressed by the document id

Which is what three places already store as \`gradingSchemeDocsId\` — \`ClassSubject\`, \`Exam\` and
\`ReportCard\`. A caller holding one of those has exactly what this endpoint needs.

**Scoped by \`schoolId\` anyway**, even though a MongoDB id is globally unique: another school's id
is a real id. Unlike a term there is no second key to scope by, so this cannot 404 for "wrong
year", only for "not yours".

**A malformed id is a \`404\`, not a \`500\`** — the query matches nothing rather than failing to
parse.

### No gates

A suspended or closed school still reads its own grading rules.

### The five test cases are in the notes below
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain. NO academic year.

 THE ONLY ENDPOINT THAT RETURNS THE BANDS. #6 trims them to a count.

 BANDS COME BACK IN THE ORDER THEY WERE WRITTEN, never re-sorted. A scheme
 entered E, A2, A1 reads back E, A2, A1.

 THE GAP WARNING IS RECOMPUTED, never stored - so a scheme whose bands were
 fixed stops being warned about, and one that was never fixed still is.

 IT ANSWERS FOR A RETIRED SCHEME. active governs what is offered for NEW
 work; an old report card still reprints through the rules it was issued
 under.

 A MALFORMED ID IS A 404, not a 500 - the query matches nothing rather than
 failing to parse.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{gradingSchemeDocsId}}", description: "The scheme's document id — gradingSchemeDocsId, which ClassSubject, Exam and ReportCard all store." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "The scheme with every band. A warning rides on it when the bands leave a whole mark ungraded — recomputed on every read, so it disappears once the bands are fixed.",
      responseFields: ["gradingSchemeDocsId", "name", "schemeVersion", "scaleType", "maximumValue", "bandCount", "gradeBands", "active", "warning", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "GRADING_SCHEME_NOT_FOUND", when: "No scheme with that id in this school — including a malformed id, and including another school's real id." },
      ],
      examples: [
        {
          id: "01",
          name: "READ A SCHEME BACK",
          expect: "200 OK",
          notes: `Create the CBSE scale, then open it. OUT: all eight bands with
    their bounds, points and descriptions — and NO warning, because every
    whole mark from 0 to 100 has a grade.`,
          body: null,
        },
        {
          id: "02",
          name: "THE BANDS ARE IN THE ORDER THEY WERE WRITTEN",
          expect: "200 OK",
          notes: `Create a scheme listing E first, then A2, then A1. It reads back
    E, A2, A1 — never sorted. #1's checks sort a COPY, because overlap is a
    question about the set rather than about the list.`,
          body: null,
        },
        {
          id: "03",
          name: "A GAP IS REPORTED EVERY TIME, NOT JUST ON CREATE",
          expect: "200 OK",
          notes: `Open the "Gappy" scheme from Create case 06. The SAME warning comes
    back — "32 to 81" — because it is recomputed here rather than stored.
    A stored sentence would outlive the problem it described.`,
          body: null,
        },
        {
          id: "04",
          name: "A DESCRIPTOR SCHEME HAS NO NUMBERS AND NO WARNING",
          expect: "200 OK",
          notes: `OUT: bands with a gradeCode and a description, no minimumValue or
    maximumValue keys at all, no maximumValue on the scheme, and no warning —
    there is no scale to leave holes in.`,
          body: null,
        },
        {
          id: "05",
          name: "AN ID THAT IS NOT AN ID",
          expect: "404 Not Found",
          notes: `/grading-schemes/not-an-id
    OUT: { "code": "GRADING_SCHEME_NOT_FOUND" }. A 404 rather than a 500 —
    the query matches nothing rather than failing to parse. Another school's
    REAL id answers the same way, which is the tenant boundary doing its job.`,
          body: null,
        },
      ],
    },
    {
      id: "list-grading-schemes",
      name: "List Grading Schemes",
      method: "GET",
      path: "/schools/current/grading-schemes",
      status: 'live',
      summary: "The school's schemes, filtered and paged. Bands are not included.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/grading-schemes\` — endpoint #6.

### The dropdown behind every "how is this graded" field

Which is why it filters by \`scaleType\`. A caller attaching a scheme to an exam marked out of 100
wants the schemes that can resolve a number — and \`DESCRIPTOR\` cannot, because a descriptor grade
is chosen rather than computed.

### Three filters, all optional, all AND-ed

- **\`?active=\`** — offered for new work, or retired. Absent returns **both**, which is not the
  same as \`false\`. A retired scheme still resolves every report card that used it.
- **\`?scaleType=\`** — \`PERCENTAGE\` · \`MARKS\` · \`DESCRIPTOR\`.
- **\`?search=\`** — case-insensitive, matches anywhere in \`name\`.

**\`?search=\` matches \`name\` only**, unlike the term list which also matches a code. There is no
code on a scheme to match — the visible cost of keying on the name, and an argument for the
\`schemeCode\` that open item 3 keeps on the table.

The needle is **\`Pattern.quote\`d**, so \`?search=(\` is an empty result rather than a 500 from
\`PatternSyntaxException\`.

### Bands are not returned, only bandCount

The CBSE scale alone is eight bands of six fields, so a twelve-row page carrying full band tables
is roughly six hundred values to render a list of twelve names. **#7 is one call away** for the
caller that wants them.

\`bandCount\` earns the place they lost: a scheme with zero bands cannot exist — #1 refuses it — so
the count is never a way of saying "empty". It is how a person recognises a scale they know.

**No \`warning\` either.** The gap note is recomputed per read rather than stored, so putting it on
a row would mean walking every band of every scheme on the page — and showing a warning on a row
that cannot act on it.

### Sorted by name, THEN schemeVersion

**This is the first list in the project whose stable order needs two fields.** The pair is unique
within a school — \`school_grading_name_version_uniq\` declares it and #1 enforces it — so every
sort ends in a total order and paging cannot put one row on two pages while another is never seen.

**Neither field alone would have served**: a school holds one name at several versions, and one
version string across several names. Contrast the term list, where \`sequence\` alone is unique
within a year.

Grouping the versions of one rulebook together is the useful side effect, not the reason.

\`?sort=\` is an **allowlist**: \`name\`, \`schemeVersion\`, \`scaleType\`, \`createdAt\`, \`updatedAt\`.
Anything else is \`400 INVALID_SORT_FIELD\`, because an arbitrary field name reaching a Mongo sort
is how a caller makes the database read every document to answer one page.

**\`gradeBands\` is deliberately not on it.** Sorting by an array sorts by its first element in
Mongo, which would order schemes by whichever band happened to be entered first — a result that
looks deliberate and means nothing.

### Nothing here can 404

Unlike the term list, which resolves a \`{year}\` from the path and answers \`404\` when it is not this
school's, there is **no parent to resolve**. So an empty page means "this school has no schemes",
which is a fact rather than an ambiguity.

### No gates

A suspended or closed school still reads its own grading rules — and has to, because correcting an
old report card means reading the scheme it was issued under.

### The eight test cases are in the notes below
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain. NO academic year.

 THREE FILTERS, ALL AND-ed: active, scaleType, search. Absent is never the
 same as false — ?active= left off returns retired schemes too.

 search MATCHES name ONLY. A scheme has no code to match, unlike a term. The
 needle is regex-quoted, so a stray "(" is an empty result, not a 500.

 BANDS ARE NOT RETURNED, only bandCount. Eight bands of six fields per row
 would be ~600 values to render twelve names. #7 is one call away.

 SORTED BY name THEN schemeVersion — the first list here whose stable order
 needs TWO fields. Neither alone is unique: one name has many versions, one
 version string spans many names.

 ?sort= IS AN ALLOWLIST: name, schemeVersion, scaleType, createdAt,
 updatedAt. gradeBands is excluded on purpose — Mongo sorts an array by its
 first element, which would mean whichever band was typed first.

 NOTHING HERE CAN 404. There is no parent to resolve, so an empty page means
 "no schemes" rather than "no such year".

 NO GATES. A suspended school still reads its own grading rules.`,
      requiredFields: [],
      pathParams: [],
      queryParams: [
        { key: "active", value: "", disabled: true, description: "true for schemes offered for new work, false for retired. Absent returns both." },
        { key: "scaleType", value: "", disabled: true, description: "PERCENTAGE, MARKS or DESCRIPTOR. The filter that answers 'what can grade a number'." },
        { key: "search", value: "", disabled: true, description: "Matches name, case-insensitive, anywhere. Regex-quoted." },
        { key: "page", value: "0", disabled: true, description: "0-based." },
        { key: "size", value: "20", disabled: true, description: "1 to 100." },
        { key: "sort", value: "", disabled: true, description: "name, schemeVersion, scaleType, createdAt, updatedAt — optionally ,desc." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "A page of summaries — no gradeBands and no warning on any row. Both are on #7.",
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "first", "last"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_SORT_FIELD", when: "A ?sort= field outside the allowlist. The message lists what is allowed." },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "size below 1 or above 100." },
        { status: 400, code: "INVALID_PAGE", when: "A negative page." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain. The only 404 this endpoint has — there is no parent to resolve." },
      ],
      examples: [
        {
          id: "01",
          name: "EVERY SCHEME",
          expect: "200 OK",
          notes: `No filters. OUT: a page ordered by name, then schemeVersion — so the
    versions of one rulebook sit together. No gradeBands on any row.`,
          body: null,
        },
        {
          id: "02",
          name: "ONLY WHAT CAN GRADE A NUMBER",
          expect: "200 OK",
          notes: `?scaleType=PERCENTAGE, then ?scaleType=MARKS, then DESCRIPTOR.
    The first two can be resolved by value; the third cannot, which is the
    real reason this filter exists.`,
          body: null,
        },
        {
          id: "03",
          name: "ACTIVE, RETIRED, THEN BOTH",
          expect: "200 OK",
          notes: `?active=true, then ?active=false, then leave it off.
    Absent is NOT the same as false — a retired scheme still resolves every
    report card that used it.`,
          body: null,
        },
        {
          id: "04",
          name: "SEARCH BY NAME",
          expect: "200 OK",
          notes: `?search=cbse — case-insensitive, matches anywhere. Try ?search=CBSE
    and ?search=grading too: same rows.`,
          body: null,
        },
        {
          id: "05",
          name: "A STRAY BRACKET",
          expect: "200 OK",
          notes: `?search=( — an EMPTY PAGE, not a 500. The needle is Pattern.quote'd
    before it is compiled, so it searches for the character.`,
          body: null,
        },
        {
          id: "06",
          name: "THE VERSIONS OF ONE RULEBOOK",
          expect: "200 OK",
          notes: `?search=CBSE after creating 2026.1, 2026.2 and 2027.1.
    OUT: three rows in version order. This is what the second sort field is
    for — name alone leaves their order undefined.`,
          body: null,
        },
        {
          id: "07",
          name: "PAGING IS STABLE",
          expect: "200 OK",
          notes: `?size=2&page=0, then page=1, then page=2. No row appears twice and
    none is skipped, because name+schemeVersion is unique within a school.`,
          body: null,
        },
        {
          id: "08",
          name: "SORTING BY THE BANDS",
          expect: "400 Bad Request",
          notes: `?sort=gradeBands
    OUT: { "code": "INVALID_SORT_FIELD" }, listing what is allowed. Excluded
    on purpose: Mongo sorts an array by its FIRST element, so this would
    order schemes by whichever band happened to be typed first.`,
          body: null,
        },
      ],
    },
    {
      id: "resolve-grade",
      name: "Resolve A Mark",
      method: "GET",
      path: "/schools/current/grading-schemes/{id}/resolve",
      status: 'live',
      summary: "Turn a mark into a grade.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/grading-schemes/{id}/resolve?value=\` — endpoint #8.

### This is what the whole module is for

Every rule #1 enforces on write was **unexercised on read** until this existed. A band set that
*validates* but *resolves* wrongly is the failure mode a create-only module cannot detect, which is
why the plan puts #8 in phase 1 beside the write rather than after it.

**It writes nothing.** Arithmetic over one document — which is why it is a \`GET\` with the value
in the query string. A resolved grade is stored by whatever records the mark, not by asking.

### Three refusals, and the ORDER is the answer

1. \`409 SCHEME_NOT_RESOLVABLE_BY_VALUE\` — a **DESCRIPTOR** scheme has no arithmetic to do; a
   teacher picks "Developing" directly.
2. \`400 VALUE_OUTSIDE_SCALE\` — the value is one this scheme could never produce.
3. \`404 GRADE_NOT_RESOLVABLE\` — the scale has a hole there.

Asking them in any other order gives a true answer to the wrong question: a descriptor scheme asked
about 90 would be told "outside the scale" when it has no scale.

### A gap is a 404, not a 409

The caller asked for the grade at this value and there is none — a thing not found. **The message
names the two bands it fell between**, because the school is the only one who can close the hole
and "no grade for 90.5" does not say where to look.

### Both bounds are inclusive

A value equal to a boundary resolves to the band that declares it. That is unambiguous **only**
because an overlap is refused at write — the two rules are halves of one decision.

### The whole band comes back

\`gradePoint\` feeds a CGPA and \`passed\` decides whether a subject is cleared. Returning only
\`gradeCode\` would guarantee a second call per mark, and a report card resolves one per subject
per term. The scheme's name and version come too: **the same mark resolves differently under
another version**, which is the entire reason versioning exists.

**No gate runs on a read, and a retired scheme resolves** — reprinting a 2026 report card means
reading the 2026 rules.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{gradingSchemeDocsId}}", description: "The scheme's MongoDB document id, from Create Grading Scheme." },
      ],
      queryParams: [
        { key: "value", value: "95", enabled: true, description: "The mark to resolve. Required. Must be between 0 and the scheme's maximumValue." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "The whole band the value falls in, plus the scheme that answered.",
      responseFields: ["gradingSchemeDocsId", "schemeName", "schemeVersion", "scaleType", "maximumValue", "value", "gradeCode", "gradePoint", "description", "passed", "bandMinimumValue", "bandMaximumValue", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "VALUE_OUTSIDE_SCALE", when: "?value= is below 0 or above the scheme's maximumValue — a mark this scheme could never produce." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "GRADING_SCHEME_NOT_FOUND", when: "No scheme with that id in this school — including another school's real id." },
        { status: 404, code: "GRADE_NOT_RESOLVABLE", when: "No band covers the value. The message names the bands on either side of the hole." },
        { status: 409, code: "SCHEME_NOT_RESOLVABLE_BY_VALUE", when: "The scheme is DESCRIPTOR — a grade is chosen, not computed." },
      ],
      examples: [
        { id: "01", name: "A MARK RESOLVES", expect: "200 OK",
          notes: `?value=95 on a CBSE-shaped scheme.\n    OUT: the WHOLE band — gradeCode A1, gradePoint 10, description,\n    passed, and the band's own bounds so a caller can show WHY.`, body: null },
        { id: "02", name: "BOTH BOUNDS ARE INCLUSIVE", expect: "200 OK",
          notes: `A1 is 91-100. ?value=91 and ?value=100 BOTH resolve to A1;\n    ?value=90 resolves to A2. Unambiguous only because an overlap is\n    refused at write.`, body: null },
        { id: "03", name: "A HOLE IN THE SCALE", expect: "404 Not Found",
          notes: `Bands 0-32 and 81-100, then ?value=50.\n    OUT: { "code": "GRADE_NOT_RESOLVABLE" }, and the message NAMES the\n    band below and the band above. A 404 rather than a 409: the grade\n    asked for does not exist.`, body: null },
        { id: "04", name: "ABOVE THE CEILING", expect: "400 Bad Request",
          notes: `?value=101 on a scheme out of 100.\n    OUT: { "code": "VALUE_OUTSIDE_SCALE" }. Checked BEFORE the bands —\n    "no band covers 101" is true and hides the real problem.`, body: null },
        { id: "05", name: "BELOW ZERO", expect: "400 Bad Request",
          notes: `?value=-1. Nothing is graded below zero on any scale here.`, body: null },
        { id: "06", name: "A DESCRIPTOR SCHEME", expect: "409 Conflict",
          notes: `OUT: { "code": "SCHEME_NOT_RESOLVABLE_BY_VALUE" } — and it says so\n    even for ?value=9999, because the scale question does not apply to a\n    scheme that has no scale.`, body: null },
        { id: "07", name: "A MARKS SCHEME OUT OF 50", expect: "200 OK",
          notes: `?value=50 resolves; ?value=51 is VALUE_OUTSIDE_SCALE. The same walk\n    against a different ceiling.`, body: null },
        { id: "08", name: "A FRACTIONAL MARK IN A SLIVER", expect: "404 Not Found",
          notes: `Bands 0-19 and 20-50, then ?value=19.5.\n    OUT: GRADE_NOT_RESOLVABLE. This is the cost #1's gap warning names:\n    it measures in whole marks, so it never warned about this sliver.`, body: null },
        { id: "09", name: "A RETIRED SCHEME STILL RESOLVES", expect: "200 OK",
          notes: `Deactivate it first. active governs what is OFFERED for new work;\n    a 2026 report card reprints through the 2026 rules forever.`, body: null },
        { id: "10", name: "NO ?value= AT ALL", expect: "400 Bad Request",
          notes: `And a non-numeric ?value=abc is a 400 too, never a 500.`, body: null },
        { id: "11", name: "ANOTHER SCHOOL'S SCHEME", expect: "404 Not Found",
          notes: `A REAL scheme id belonging to a different school.\n    OUT: { "code": "GRADING_SCHEME_NOT_FOUND" } — the lookup carries\n    schoolId, so their boundaries never leak.`, body: null },
      ],
    },
  ],
};

const GROUP_ACADEMICS_TIMETABLE = {
  id: "academics-timetable",
  module: "Academics / Timetable",
  endpoints: [
    {
      id: "create-timetable",
      name: "Create Timetable",
      method: "POST",
      path: "/schools/current/academic-years/{year}/timetables",
      status: 'live',
      summary: "Write a day's periods across one date or a range.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{year}/timetables\` — endpoint #1.

### A range, because a school builds a pattern and not a Tuesday

\`startDate\` alone writes **one day**. Adding \`endDate\` writes **every date between them
inclusive**, each carrying the same set of periods.

**The dates are in the body; the YEAR is in the path.** The plan had \`POST /timetables/{date}\`
with the year derived from the date — reversed on **2026-09-17**, and this is the change that
matters most:

> A school holding two academic years whose ranges both covered a September date had its timetable
> written into the year it had **not** picked, and the refusal it eventually saw named a year it had
> never mentioned. Every date resolves to *something*, so "you asked for the wrong year" was not a
> sentence this endpoint could say.

Now the caller states the year and a date outside it is \`409 DATE_OUTSIDE_ACADEMIC_YEAR\`, naming
that year's own range. The dates stay in the body because #1 writes a **range**, and a date in the
path beside a range in the body would be two sources for one fact.

**It also removed a deviation.** With the year named in the URL, **gate 4 runs in the controller**
like everywhere else, instead of the per-date refusal the service used to carry — and the service
lost its per-date year lookup with it.

### A holiday is skipped; a taken date refuses everything

Those look inconsistent and are not:

- **A holiday inside a range is expected** — any range longer than about five days contains a
  weekly off — so it is skipped and **named in the response** rather than refusing the whole
  request. A range where *every* date is a holiday writes nothing and is a \`409\`.
- **A date that already has a timetable is not expected.** It means the caller is rebuilding
  something, and half a range would leave a school unable to tell which days came from which
  request. All of it or none, with every colliding date named.

**A weekly off is a dated holiday, never a weekday.** Nothing here looks at the day of the week. A
school that runs on Sunday and closes on Friday is a normal school, and only
\`AcademicYear.holidays\` knows which.

### A LESSON may only name a subject that section actually studies

This is the rule to get right, and it is wrong in two different directions:

- A subject created **without** a \`sectionNo\` is **class-wide** — every section takes it. Maths.
- A subject created **with** a \`sectionNo\` belongs to **that section alone**. 10-C does German;
  10-A does not.

Matching only the section's own subjects would refuse Maths for every section in the school.
Ignoring \`sectionNo\` would let 10-A be timetabled for German it does not take, and nobody would
notice until a child sat an exam in it. Refused as \`409 SUBJECT_NOT_IN_SECTION\`.

**A retired subject cannot be scheduled** — unlike a retired grading scheme, which must keep
resolving report cards already issued. Nothing is being reprinted; the day has not happened yet.

### Touching periods do not clash

09:00–09:45 beside 09:45–10:30 is a normal day. That is the **opposite** call from a grade band,
whose bounds are inclusive at both ends and whose neighbours must not touch — and the difference is
real: a band covers the value 90, a period does not occupy the instant it ends.

### Entry ids are generated per date

MongoDB does not generate \`_id\` for embedded documents, and **each date gets its own** — two
dates sharing an id would make \`AttendanceSession.timetableEntryId\` ambiguous.

**Gates 1 and 2 run; gate 4 does not.** The year comes from a date in the body and a range may span
two of them, so the equivalent check runs per date in the service. The one place in the project
where that rule bends.`,
      requiredFields: ["startDate", "entries"],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year this timetable belongs to. Every date in the range must fall inside it." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: {
        startDate: "2026-08-03",
        endDate: "2026-08-07",
        entries: [
          {
            periodCode: "P1",
            classDocsId: "{{schoolClassId}}",
            sectionNo: "A",
            slotType: "LESSON",
            subjectCode: "MATHEMATICS",
            teacherDocsId: "{{staffDocsId}}",
            startTime: "09:00:00",
            endTime: "09:45:00",
          },
          {
            periodCode: "B1",
            classDocsId: "{{schoolClassId}}",
            sectionNo: "A",
            slotType: "BREAK",
            slotLabel: "Lunch Break",
            startTime: "11:00:00",
            endTime: "11:30:00",
          },
        ],
      },
      successStatus: 201,
      successNote: "Every working day in the range now has a timetable. Skipped holidays are named.",
      responseFields: ["startDate", "endDate", "createdCount", "entriesPerDay", "createdDates", "skippedDates", "timetable", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "INVALID_DATE_RANGE", when: "endDate is before startDate." },
        { status: 400, code: "DATE_RANGE_TOO_LONG", when: "The range covers more than 120 days — about a term of working days." },
        { status: 400, code: "INVALID_PERIOD_TIMES", when: "A period's startTime is not before its endTime. Equal times are refused too: a period from 09:00 to 09:00 is nothing happening." },
        { status: 400, code: "SLOT_FIELDS_REQUIRED", when: "A LESSON without a subjectCode or a teacherDocsId." },
        { status: 400, code: "SLOT_FIELDS_NOT_ALLOWED", when: "A BREAK, ASSEMBLY or ACTIVITY carrying a subjectCode. A teacherDocsId on one is FINE since 2026-09-17 — somebody supervises the break, runs the assembly, takes the activity — and it still counts against that teacher’s day." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "A classDocsId that is not this school's in that academic year." },
        { status: 404, code: "TEACHER_NOT_FOUND", when: "A teacherDocsId that is not staff of this school." },
        { status: 409, code: "TIMETABLE_ALREADY_EXISTS", when: "Any date in the range already has a timetable. Nothing is written, and the message names every colliding date." },
        { status: 409, code: "NOT_A_WORKING_DAY", when: "Every date in the range is a holiday or weekly off, so nothing was written." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "No academic year of that name in this school." },
        { status: 409, code: "DATE_OUTSIDE_ACADEMIC_YEAR", when: "A date in the range falls outside the year named in the path. The refusal the old date-derived version had no way to give — it wrote into whichever year happened to contain the date." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "The year named is not the one the school is running. Gate 4, in the controller." },
        { status: 409, code: "SECTION_NOT_IN_CLASS", when: "The sectionNo is not an active section of that class." },
        { status: 409, code: "SUBJECT_NOT_IN_SECTION", when: "That section does not study the subject — it is neither class-wide nor its own, or it has been retired." },
        { status: 409, code: "SECTION_PERIOD_OVERLAP", when: "One section has two periods covering the same minute." },
        { status: 409, code: "TEACHER_PERIOD_OVERLAP", when: "One teacher is in two places at once." },
        { status: 409, code: "ROOM_PERIOD_OVERLAP", when: "Two sections are sent to the same room at once." },
        { status: 409, code: "PERIOD_CODE_TAKEN", when: "One section names a period code twice in a day, case-folded." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended or closed." },
      ],
      examples: [
        { id: "01", name: "ONE DAY", expect: "201 Created",
          notes: `startDate only, no endDate.\n    OUT: createdCount 1, and the whole day in \`timetable\` — present only\n    when exactly one date was written, because re-reading it would be a\n    round trip for what the server just had in its hand.`, body: null },
        { id: "02", name: "A RANGE", expect: "201 Created",
          notes: `startDate + endDate.\n    OUT: one timetable per date, createdDates listing them, and NO\n    \`timetable\` — a fortnight of a 400-period school is 4,000 entries the\n    caller already holds.`, body: null },
        { id: "03", name: "A HOLIDAY INSIDE THE RANGE", expect: "201 Created",
          notes: `Add a holiday to the year first.\n    OUT: that date appears in skippedDates with its NAME, and every other\n    date is written. Skipped, not refused — any range longer than about\n    five days contains a weekly off.`, body: null },
        { id: "04", name: "EVERY DATE IS A HOLIDAY", expect: "409 Conflict",
          notes: `OUT: { "code": "NOT_A_WORKING_DAY" }. Nothing was written, so there is\n    no partial success to report.`, body: null },
        { id: "05", name: "A DATE ALREADY HAS ONE", expect: "409 Conflict",
          notes: `OUT: { "code": "TIMETABLE_ALREADY_EXISTS" }, naming every colliding\n    date. NOTHING is written — not even the dates that were free.`, body: null },
        { id: "06", name: "A CLASS-WIDE SUBJECT", expect: "201 Created",
          notes: `Maths was created with NO sectionNo, so section A and section C may\n    both be timetabled for it.`, body: null },
        { id: "07", name: "A SECTION-SPECIFIC SUBJECT IN ITS SECTION", expect: "201 Created",
          notes: `German was created with sectionNo C. 10-C may take it.`, body: null },
        { id: "08", name: "THE SAME SUBJECT IN ANOTHER SECTION", expect: "409 Conflict",
          notes: `10-A timetabled for German.\n    OUT: { "code": "SUBJECT_NOT_IN_SECTION" } — it belongs to C alone.\n    This is the case that makes the rule worth having.`, body: null },
        { id: "09", name: "BACK-TO-BACK PERIODS", expect: "201 Created",
          notes: `09:00-09:45 then 09:45-10:30 for one section.\n    Touching is NOT overlapping — the opposite call from a grade band.`, body: null },
        { id: "10", name: "ONE TEACHER, TWO SECTIONS, ONE TIME", expect: "409 Conflict",
          notes: `OUT: { "code": "TEACHER_PERIOD_OVERLAP" }. Two SECTIONS sharing a time\n    is fine; the same teacher in both is not.`, body: null },
        { id: "11", name: "TWO SECTIONS, ONE ROOM", expect: "409 Conflict",
          notes: `Both carrying the same facilityResourceDocsId.\n    OUT: { "code": "ROOM_PERIOD_OVERLAP" }. Without a room, two sections\n    at one time is the normal case.`, body: null },
        { id: "12", name: "A BREAK WITH A TEACHER", expect: "201 Created",
          notes: `ALLOWED since 2026-09-17. Somebody supervises lunch, and recording\n    who is what makes their day add up — a teacher on a break still\n    clashes with a lesson at the same time.\n    A SUBJECT on a break is still 400 SLOT_FIELDS_NOT_ALLOWED: a break\n    teaches nothing.`, body: null },
        { id: "13", name: "AN UNLISTED SUNDAY", expect: "201 Created",
          notes: `A Sunday the school did NOT list as a holiday is a working day.\n    Nothing infers a weekend from the calendar.`, body: null },
        { id: "15", name: "A DATE BELONGING TO ANOTHER YEAR", expect: "409 Conflict",
          notes: `Two years, one covering September and one November. Name the\n    November year and send a September date.\n    OUT: { "code": "DATE_OUTSIDE_ACADEMIC_YEAR" }, naming the year's own\n    range. THE CASE THIS REWORK EXISTS FOR — the old version wrote it\n    into the September year without saying so.`, body: null },
        { id: "16", name: "ANOTHER YEAR'S CLASS", expect: "404 Not Found",
          notes: `A classDocsId from a different year, with a date inside THIS one.\n    OUT: { "code": "CLASS_NOT_FOUND" } — the class is resolved in the\n    year named in the path, not in whichever year the date falls in.`, body: null },
        { id: "14", name: "A SUSPENDED SCHOOL", expect: "409 Conflict",
          notes: `OUT: { "code": "SCHOOL_NOT_ACTIVE" } — gate 1, not the service.`, body: null },
      ],
    },
    {
      id: "list-timetables",
      name: "List Timetables",
      method: "GET",
      path: "/schools/current/academic-years/{year}/timetables",
      status: 'live',
      summary: "A year's days, date-wise, as counts rather than periods.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{year}/timetables?from=&to=&…\` — endpoint #10.

### A list carries what a list needs

A full school day measures about **120 KB**. A page of twenty carrying its periods would be two
and a half megabytes shipped to render twenty dates and five numbers — so the counts are worked
out by an **aggregation in the database** and the \`entries\` array never leaves it.

That is the same call #6 of grading makes about bands and #7 of positions about holders: **one
call away is the endpoint that carries the rest.** Here that is #7, \`GET /timetables/{date}\`.

### The five counts

| Field | What it says |
|---|---|
| \`entryCount\` | every period of every section that day |
| \`lessonCount\` | how many are taught — the rest are breaks, assemblies, activities |
| \`classCount\` | distinct classes with anything scheduled |
| \`sectionCount\` | distinct **class-and-section pairs** — "A" of one class and "A" of another are two |
| \`teacherCount\` | distinct staff named anywhere, a break's supervisor included |

**None of them is sortable.** They do not exist on the document — the aggregation computes them
*after* the page is chosen, so ordering by one would mean counting the whole year first. "The
busiest day" is a different endpoint, not a sort.

### ?from= and ?to= are independent

Either alone is meaningful — "everything from here on", "everything up to here" — and neither is
required. A window with nothing in it is an **empty page, never a 404**.

### The other filters reach inside the periods

\`?teacherDocsId=\` gives one person's working days (a break they supervise counts),
\`?facilityResourceDocsId=\` one room's, and **\`?classDocsId=\` with \`?sectionNo=\` are matched
as ONE period** rather than as two conditions.

> That pairing is the subtle one. A day holds every class's periods, so two separate conditions
> would match a day where one entry belongs to class X and an entirely unrelated entry belongs to
> some other class's section B — which is nearly every day. It is an \`$elemMatch\`.

**No gate runs on a read**, and a year the school has **ended still answers** — reading last
year's Tuesday is how an attendance record taken against it gets explained.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year whose days to list. Must exist; need not be the running one." },
      ],
      queryParams: [
        { key: "from", value: "", enabled: false, description: "First date to include. Absent starts at the beginning of the year." },
        { key: "to", value: "", enabled: false, description: "Last date, inclusive. Absent runs to the end of the year." },
        { key: "classDocsId", value: "", enabled: false, description: "Days this class is taught at all. Paired with sectionNo when both are sent." },
        { key: "sectionNo", value: "", enabled: false, description: "Matched as ONE period with classDocsId — not as a separate condition." },
        { key: "teacherDocsId", value: "", enabled: false, description: "One person's working days. A break they supervise counts." },
        { key: "facilityResourceDocsId", value: "", enabled: false, description: "One room's days." },
        { key: "page", value: "0", enabled: false, description: "Zero-based." },
        { key: "size", value: "20", enabled: false, description: "Default 20, max 100. Refused outside that, never clamped." },
        { key: "sort", value: "date", enabled: false, description: "date, createdAt, updatedAt. NOT a count — those are computed after the page is chosen." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "One page of days, each as five counts. The periods stay in the database.",
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "INVALID_SORT_FIELD", when: "?sort= named a field off the allowlist — a count included, because it is computed rather than stored." },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "?size= is above 100 or below 1." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "No academic year of that name in this school." },
      ],
      examples: [
        { id: "01", name: "A YEAR'S DAYS", expect: "200 OK",
          notes: `No filters.\n    OUT: every day that has a timetable, oldest first, each as five\n    counts. NO row carries its periods — that is #7.`, body: null },
        { id: "02", name: "A DATE WINDOW", expect: "200 OK",
          notes: `?from=&to= together. Either alone also works: ?from= means\n    "everything after", ?to= means "everything before".`, body: null },
        { id: "03", name: "A WINDOW WITH NOTHING IN IT", expect: "200 OK",
          notes: `OUT: an empty page, never a 404. A school simply has no timetable\n    written for those dates yet.`, body: null },
        { id: "04", name: "ONE TEACHER'S WORKING DAYS", expect: "200 OK",
          notes: `?teacherDocsId=\n    A day where they ONLY supervise a break still counts — which is the\n    point of recording a teacher on a non-lesson.`, body: null },
        { id: "05", name: "ONE ROOM'S DAYS", expect: "200 OK",
          notes: `?facilityResourceDocsId= — served by school_timetable_room_idx,\n    which is partial on the field existing.`, body: null },
        { id: "06", name: "A CLASS AND A SECTION, PAIRED", expect: "200 OK",
          notes: `?classDocsId=&sectionNo= together.\n    A day holding class X in section A and class Y in section B does NOT\n    match "X + B" — both halves are present but never in one period.\n    THE CASE THAT MAKES $elemMatch necessary.`, body: null },
        { id: "07", name: "AN UNSUPERVISED BREAK", expect: "200 OK",
          notes: `A day with one lesson and one break nobody supervises.\n    OUT: teacherCount 1, not 2 — the absence of a teacher is not a\n    person. The field is ABSENT rather than null, and in an aggregation\n    $ne null would have counted it.`, body: null },
        { id: "08", name: "SORTING BY A COUNT", expect: "400 Bad Request",
          notes: `?sort=entryCount\n    OUT: { "code": "INVALID_SORT_FIELD" }. A count is computed after the\n    page is chosen.`, body: null },
        { id: "09", name: "A YEAR THAT HAS ENDED", expect: "200 OK",
          notes: `Still answers. No gate runs on a read, and reading last year's\n    Tuesday is how an attendance record taken against it is explained.`, body: null },
        { id: "10", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `Also answers.`, body: null },
      ],
    },
    {
      id: "add-timetable-entry",
      name: "Add Timetable Entry",
      method: "POST",
      path: "/schools/current/academic-years/{year}/timetables/{date}/entries",
      status: 'live',
      summary: "Add one period. A $push, never a re-save.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{year}/timetables/{date}/entries\` — endpoint #3.

### A $push, never a re-save

A day is about **120 KB**. Rewriting all of it to add one period would make every addition a race
with every other edit of that morning, and would overwrite periods the caller never saw. The write
touches the array and nothing else — every period already there keeps its id.

### Checked against the whole day, not on its own

Whether the new period fits **beside the ones already there** is the only thing worth checking, so
the overlap, slot and structure rules run over the combined list. A section already busy at that
hour, a teacher already teaching, a room already in use — all refused here exactly as #1 would.

### The period code is guarded in the update itself

\`$push\` matched on "this section has no period with this code". That is the one conflict rule
expressible **without comparing times**, and it is the one most likely to be raced, because a
period code is what a person types twice.

**Why only that one:** a stored \`LocalTime\` is a BSON date carrying *the day the document was
written* — \`09:00\` became \`2026-09-17T03:30:00Z\` — so two days written a day apart cannot have
their times compared in a query at all. Overlap is therefore checked in Java against the day just
read, which leaves a narrow race: two clerks adding *overlapping* periods with *different* codes in
the same instant would both be accepted. Open item 8 of the plan is about fixing that properly.

### The day must already exist

\`404 TIMETABLE_NOT_FOUND\`. Creating one is #1.

**All three gates run.**`,
      requiredFields: ["periodCode", "classDocsId", "sectionNo", "slotType", "startTime", "endTime"],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "Must be the RUNNING year — gate 4." },
        { name: "date", value: "{{timetableDate}}", description: "The day to add to. It must already have a timetable." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: null,
      successStatus: 201,
      successNote: "The period as it was stored, with its generated id and the names behind its ids.",
      responseFields: ["timetableEntryId", "periodCode", "classDocsId", "className", "sectionNo", "slotType", "subjectCode", "subjectName", "teacherDocsId", "teacherName", "startTime", "endTime"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "INVALID_PERIOD_TIMES", when: "startTime is not before endTime." },
        { status: 400, code: "SLOT_FIELDS_REQUIRED", when: "A LESSON with no subjectCode or teacherDocsId." },
        { status: 400, code: "SLOT_FIELDS_NOT_ALLOWED", when: "A non-lesson carrying a subject. A teacher on one is fine." },
        { status: 404, code: "TIMETABLE_NOT_FOUND", when: "That date has no timetable — create it with #1." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "Not this school's class in that year." },
        { status: 404, code: "TEACHER_NOT_FOUND", when: "Not staff of this school." },
        { status: 404, code: "NOT_A_WORKING_DAY", when: "That date is a holiday or weekly off." },
        { status: 409, code: "PERIOD_CODE_TAKEN", when: "That section already has a period with that code." },
        { status: 409, code: "SECTION_NOT_IN_CLASS", when: "Not an active section of that class." },
        { status: 409, code: "SUBJECT_NOT_IN_SECTION", when: "That section does not study it." },
        { status: 409, code: "SECTION_PERIOD_OVERLAP", when: "The section is already busy at that hour." },
        { status: 409, code: "TEACHER_PERIOD_OVERLAP", when: "The teacher is already teaching then." },
        { status: 409, code: "ROOM_PERIOD_OVERLAP", when: "The room is already in use then." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1." },
      ],
      examples: [
        { id: "01", name: "ONE PERIOD INTO A DAY", expect: "201 Created",
          notes: `OUT: the period with a generated timetableEntryId.\n    Read the day back with #7: every period that was there keeps the\n    id it had — this is a $push, not a re-save.`, body: null },
        { id: "02", name: "THE DAY'S VERSION MOVES", expect: "201 Created",
          notes: `Compare #7's version before and after. A targeted write that left\n    the version standing still would let #2 replace a day that had\n    gained a period.`, body: null },
        { id: "03", name: "A REPEATED PERIOD CODE", expect: "409 Conflict",
          notes: `OUT: { "code": "PERIOD_CODE_TAKEN" }. Guarded in the update itself,\n    not only checked before it.`, body: null },
        { id: "04", name: "THE SAME CODE ELSEWHERE", expect: "201 Created",
          notes: `Codes are per SECTION, so the same code in another section is fine.`, body: null },
        { id: "05", name: "OVERLAPPING ITS OWN SECTION", expect: "409 Conflict",
          notes: `OUT: { "code": "SECTION_PERIOD_OVERLAP" } — checked against the\n    whole day, which is the only thing worth checking.`, body: null },
        { id: "06", name: "A TEACHER ALREADY TEACHING", expect: "409 Conflict",
          notes: `OUT: { "code": "TEACHER_PERIOD_OVERLAP" }.`, body: null },
        { id: "07", name: "A ROOM ALREADY IN USE", expect: "409 Conflict",
          notes: `OUT: { "code": "ROOM_PERIOD_OVERLAP" }.`, body: null },
        { id: "08", name: "A LOWER-CASE SECTION", expect: "201 Created",
          notes: `Accepted, and stored in the class's own spelling — the same\n    normalisation every write in this module runs.`, body: null },
        { id: "09", name: "A DAY WITH NO TIMETABLE", expect: "404 Not Found",
          notes: `OUT: { "code": "TIMETABLE_NOT_FOUND" }. This adds to a day; #1\n    creates one.`, body: null },
        { id: "10", name: "A SUSPENDED SCHOOL", expect: "409 Conflict",
          notes: `OUT: { "code": "SCHOOL_NOT_ACTIVE" } — gate 1.`, body: null },
      ],
    },
    {
      id: "patch-timetable-entry",
      name: "Patch Timetable Entry",
      method: "PATCH",
      path: "/schools/current/academic-years/{year}/timetables/{date}/entries/{entryId}",
      status: 'live',
      summary: "Correct one period \u2014 the substitution.",
      schoolSurface: true,
      docs: `**PATCH** \`/…/timetables/{date}/entries/{entryId}\` — endpoint #4.

### The write this module exists for

A teacher calls in sick at **07:40** and six periods need covering before **08:00**. Each is one
field of one period, so this is one targeted \`$set entries.$[entry].<field>\` through an array
filter — not a re-save of 120 KB, and not a replacement of the day.

### Editable, and not

**Editable**: \`teacherDocsId\`, \`subjectCode\`, \`startTime\`, \`endTime\`, \`slotLabel\`,
\`facilityResourceDocsId\`, \`periodCode\`.

**Not editable**: \`classDocsId\` and \`sectionNo\` — moving a period to another section is deleting
one and adding another, and pretending otherwise keeps an attendance session pointing at a period
that changed identity underneath it. Nor \`slotType\`, which decides which other fields are legal:
turning a \`LESSON\` into a \`BREAK\` in place would leave a subject on a break.

Sending them is not an error — they are simply not in the request shape, and the response shows
what actually changed.

### \`""\` clears; an absent key leaves the field alone

A room is removed by sending \`facilityResourceDocsId: ""\`, and there is no other way to say it.
Treating an absent key as a clear would empty a field every time somebody patched a different one.
It does not apply to \`periodCode\`, \`startTime\` and \`endTime\`, which a period cannot be without.

**A patch that changes nothing is \`400 NOTHING_TO_UPDATE\`** — a correction has to say what it
corrects.

### Exactly one document must MATCH — and matched is not modified

The model contract's rule 2, and the one thing to get right. Zero matched means the entry is gone or
the version moved: \`404\` or \`409\`, never a silent success.

**Zero *modified* means nothing of the sort.** A \`$set\` writing the value a field already holds
changes nothing and is a perfectly good no-op — patching a teacher to the teacher already there is
a **200**, not a 404.

### The corrected period is checked beside the others

Which is the whole point of a substitution: **the covering teacher must not already be somewhere
else at that hour.**

### version is optional here, unlike #2

A targeted write cannot lose somebody else's edit to a *different* period, so requiring it would
refuse two clerks working on two sections — open item 1's complaint. Send it when the correction was
decided from a screen that might be stale, and a moved day is \`409 CONCURRENT_MODIFICATION\`.

**All three gates run.**`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "Must be the RUNNING year — gate 4." },
        { name: "date", value: "{{timetableDate}}", description: "The day the period is on." },
        { name: "entryId", value: "{{timetableEntryId}}", description: "The period's own id. Not in this day is 404 — a malformed one too, never a 500." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: null,
      successStatus: 200,
      successNote: "The period as it now stands, with the names behind its ids.",
      responseFields: ["timetableEntryId", "periodCode", "classDocsId", "className", "sectionNo", "slotType", "subjectCode", "subjectName", "teacherDocsId", "teacherName", "slotLabel", "startTime", "endTime", "facilityResourceDocsId"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "NOTHING_TO_UPDATE", when: "No field was sent to change." },
        { status: 400, code: "INVALID_PERIOD_TIMES", when: "The corrected times are not start-before-end." },
        { status: 400, code: "SLOT_FIELDS_REQUIRED", when: "Clearing the subject or teacher of a LESSON." },
        { status: 400, code: "SLOT_FIELDS_NOT_ALLOWED", when: "Giving a subject to a non-lesson." },
        { status: 404, code: "TIMETABLE_NOT_FOUND", when: "That date has no timetable." },
        { status: 404, code: "TIMETABLE_ENTRY_NOT_FOUND", when: "No period with that id in this day — a malformed id answers the same way." },
        { status: 404, code: "TEACHER_NOT_FOUND", when: "The new teacher is not staff of this school." },
        { status: 404, code: "NOT_A_WORKING_DAY", when: "That date is a holiday or weekly off." },
        { status: 409, code: "CONCURRENT_MODIFICATION", when: "A version was sent and the day has moved past it." },
        { status: 409, code: "PERIOD_CODE_TAKEN", when: "The new code is one that section already uses." },
        { status: 409, code: "SUBJECT_NOT_IN_SECTION", when: "That section does not study the new subject." },
        { status: 409, code: "SECTION_PERIOD_OVERLAP", when: "The new times collide with the section's own next period." },
        { status: 409, code: "TEACHER_PERIOD_OVERLAP", when: "The covering teacher is already somewhere else at that hour." },
        { status: 409, code: "ROOM_PERIOD_OVERLAP", when: "The new room is already in use then." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1." },
      ],
      examples: [
        { id: "01", name: "THE SUBSTITUTION", expect: "200 OK",
          notes: `{ "teacherDocsId": "…" } and nothing else.\n    OUT: the period with the new teacher. Read the day back with #7:\n    NOTHING ELSE MOVED, and the id is the same one.`, body: null },
        { id: "02", name: "A NO-OP PATCH", expect: "200 OK",
          notes: `Send the teacher it already has.\n    200, NOT 404 — the write counts documents MATCHED, not modified.\n    THE TRAP THIS ENDPOINT IS BUILT AROUND.`, body: null },
        { id: "03", name: "NOTHING AT ALL", expect: "400 Bad Request",
          notes: `{ }\n    OUT: { "code": "NOTHING_TO_UPDATE" }.`, body: null },
        { id: "04", name: "CLEARING THE ROOM", expect: "200 OK",
          notes: `{ "facilityResourceDocsId": "" }\n    The field is ABSENT afterwards. An absent key would have left it\n    alone — those are two different requests.`, body: null },
        { id: "05", name: "CLEARING A LESSON'S TEACHER", expect: "400 Bad Request",
          notes: `OUT: { "code": "SLOT_FIELDS_REQUIRED" } — a lesson needs one.`, body: null },
        { id: "06", name: "A TEACHER ALREADY BUSY", expect: "409 Conflict",
          notes: `OUT: { "code": "TEACHER_PERIOD_OVERLAP" }. The corrected period is\n    checked BESIDE THE OTHERS, which is the whole point.`, body: null },
        { id: "07", name: "TIMES THAT NOW COLLIDE", expect: "409 Conflict",
          notes: `OUT: { "code": "SECTION_PERIOD_OVERLAP" }.`, body: null },
        { id: "08", name: "A STALE VERSION", expect: "409 Conflict",
          notes: `Send a version, patch, then send the same version again.\n    OUT: { "code": "CONCURRENT_MODIFICATION" } naming both.`, body: null },
        { id: "09", name: "NO VERSION AT ALL", expect: "200 OK",
          notes: `Optional here, unlike #2 — a targeted write cannot lose an edit to\n    a DIFFERENT period.`, body: null },
        { id: "10", name: "CLASS, SECTION AND SLOT TYPE", expect: "200 OK",
          notes: `Send them alongside a real change: they are ignored. Moving a\n    period to another section is delete-and-add, and a slot type\n    decides which other fields are legal.`, body: null },
        { id: "11", name: "A MALFORMED ENTRY ID", expect: "404 Not Found",
          notes: `OUT: { "code": "TIMETABLE_ENTRY_NOT_FOUND" }, never a 500 — the\n    presence check runs before anything parses it as an ObjectId.`, body: null },
      ],
    },
    {
      id: "remove-timetable-entry",
      name: "Remove Timetable Entry",
      method: "DELETE",
      path: "/schools/current/academic-years/{year}/timetables/{date}/entries/{entryId}",
      status: 'live',
      summary: "Remove one period. A $pull by id.",
      schoolSurface: true,
      docs: `**DELETE** \`/…/timetables/{date}/entries/{entryId}\` — endpoint #5.

### A $pull by id

One period leaves; every other keeps its id and its place. The day's **version still moves**, so #2
can tell that it changed.

### A 204, and a 404 when it was not there

**Not an idempotent 204 either way.** A caller deleting a period that has already gone has a stale
screen, and telling them it worked would leave them believing they removed something somebody else
had already dealt with.

**Here the modified count is the right signal**, unlike #4: a \`$pull\` that removes nothing modifies
nothing.

### Refused when attendance names the period

\`409 ENTRY_STILL_REFERENCED\`. \`AttendanceSession.timetableEntryId\` is an optional link with no
foreign key behind it, so the removal would leave any session naming that period pointing at
nothing and **nothing would fail**. Open item 4 of the plan proposed refusing instead, and that is
what this does — one query per removal, scoped by school so another tenant's session cannot block
a removal this school is entitled to make.

**Nothing writes that collection yet**, so the refusal cannot fire through the API today. It becomes
live the moment the attendance module is built, rather than needing to be remembered then.

### No version

A \`$pull\` by id is position-independent and cannot lose a concurrent edit to another period —
open item 1's table says so.

**All three gates run.**`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "Must be the RUNNING year — gate 4." },
        { name: "date", value: "{{timetableDate}}", description: "The day the period is on." },
        { name: "entryId", value: "{{timetableEntryId}}", description: "The period's own id. Not in this day is 404 — a malformed one too, never a 500." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 204,
      successNote: "No content. The period is gone and the day's version has moved.",
      responseFields: [],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "TIMETABLE_NOT_FOUND", when: "That date has no timetable." },
        { status: 404, code: "TIMETABLE_ENTRY_NOT_FOUND", when: "No period with that id in this day — including one already removed." },
        { status: 404, code: "NOT_A_WORKING_DAY", when: "That date is a holiday or weekly off." },
        { status: 409, code: "ENTRY_STILL_REFERENCED", when: "An attendance session of this school names the period." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1." },
      ],
      examples: [
        { id: "01", name: "REMOVE ONE PERIOD", expect: "204 No Content",
          notes: `Read the day back with #7: it is gone, EVERY OTHER PERIOD KEPT ITS\n    ID, and the version moved.`, body: null },
        { id: "02", name: "REMOVE IT AGAIN", expect: "404 Not Found",
          notes: `OUT: { "code": "TIMETABLE_ENTRY_NOT_FOUND" }. NOT a second 204 — a\n    caller with a stale screen should know.`, body: null },
        { id: "03", name: "A MALFORMED ENTRY ID", expect: "404 Not Found",
          notes: `Same 404, never a 500 — the presence check runs before anything\n    parses it as an ObjectId.`, body: null },
        { id: "04", name: "A PERIOD ATTENDANCE NAMES", expect: "409 Conflict",
          notes: `OUT: { "code": "ENTRY_STILL_REFERENCED" }, and the period is STILL\n    THERE. Nothing writes attendance_sessions through the API yet, so\n    this needs a session inserted directly to reach.`, body: null },
        { id: "05", name: "ANOTHER SCHOOL'S SESSION", expect: "204 No Content",
          notes: `Does NOT block it — the check is scoped by school, like every\n    query in this project.`, body: null },
        { id: "06", name: "A SUSPENDED SCHOOL", expect: "409 Conflict",
          notes: `OUT: { "code": "SCHOOL_NOT_ACTIVE" } — gate 1.`, body: null },
      ],
    },
    {
      id: "get-section-day",
      name: "Get Section Day",
      method: "GET",
      path: "/schools/current/academic-years/{year}/timetables/{date}/sections/{classDocsId}/{sectionNo}",
      status: 'live',
      summary: "One section's day \u2014 what a child's parent opens.",
      schoolSurface: true,
      docs: `**GET** \`/…/timetables/{date}/sections/{classDocsId}/{sectionNo}\` — endpoint #8.

### What a child's parent opens

One section, one date, in the order the day happens. Nobody outside the office wants the whole
school's four hundred periods — they want the eight their child sits through.

### Earliest first, where #7 is in stored order

#7 returns the whole school's day **as stored**, and says why: periods of different sections run at
the same hour, so "by time" there is not an order at all — it is a tie with a hidden second key.

**That objection does not apply to one section.** A section cannot be in two places at once —
\`SECTION_PERIOD_OVERLAP\` is refused on every write — so within one section \`startTime\` is a
**total** order. Sorting here is meaningful where sorting there would have been a guess.

### An empty answer is a 200

The date has a timetable and this section has nothing in it: a fact about the section, not a missing
document. **The three 404s belong to the day**, and they are the same three #7 gives — a holiday
names itself, a working day with nothing written says so, and a date outside the year is a 409.

### "Nothing scheduled" and "wrong section" never look alike

Returning an empty list for a mistyped \`classDocsId\` would leave a parent's app unable to tell
"no school today" from "I asked for the wrong child". So the class is resolved in the year
(\`404 CLASS_NOT_FOUND\`) and the section has to be one the class holds
(\`409 SECTION_NOT_IN_CLASS\`).

**A retired section still answers.** This is a read, and no gate runs on one — a section retired in
March must not make February's Tuesday unreadable, because attendance taken against it has to stay
explicable. That is why this does *not* use the active-section check every write does.

### The names come resolved

\`className\`, \`subjectName\` and \`teacherName\` beside the ids, in two queries — and the subject
name follows the same class-wide rule #1 enforces, so two sections under one \`subjectCode\` get two
different names.

**No gate.** Last year's Tuesday still answers.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year. Must exist; need not be the running one." },
        { name: "date", value: "", description: "ISO date, 2026-11-02." },
        { name: "classDocsId", value: "{{schoolClassId}}", description: "Must be a class of this school in that year." },
        { name: "sectionNo", value: "{{sectionNo}}", description: "Case-insensitive. Answered in the class's own spelling." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "One section's periods, earliest first, with the names behind their ids.",
      responseFields: ["date", "academicYear", "dailyTimetableDocsId", "classDocsId", "className", "sectionNo", "entryCount", "lessonCount", "teacherCount", "entries", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "BAD_REQUEST", when: "{date} is not an ISO date." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "No academic year of that name in this school." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "That classDocsId is not this school's class in that year." },
        { status: 404, code: "NOT_A_WORKING_DAY", when: "That date is a holiday or weekly off. The message names it." },
        { status: 404, code: "TIMETABLE_NOT_FOUND", when: "A working day with no timetable written for it yet." },
        { status: 409, code: "SECTION_NOT_IN_CLASS", when: "The class has no section of that name." },
        { status: 409, code: "DATE_OUTSIDE_ACADEMIC_YEAR", when: "The stored day belongs to a different year than {year}." },
      ],
      examples: [
        { id: "01", name: "A SECTION'S DAY", expect: "200 OK",
          notes: `OUT: only that section's periods, EARLIEST FIRST — which is a real\n    order here, because a section cannot be in two places at once.`, body: null },
        { id: "02", name: "NOT AS STORED", expect: "200 OK",
          notes: `Compare with #7 for the same date. #7 gives the whole school in the\n    order it was written; this sorts, and it is allowed to.`, body: null },
        { id: "03", name: "THE SECTION FOLDS CASE", expect: "200 OK",
          notes: `Ask for 'a'.\n    OUT: the same day, and sectionNo answers as 'A' — the class's own\n    spelling.`, body: null },
        { id: "04", name: "ONE CODE, TWO SECTIONS", expect: "200 OK",
          notes: `A subjectCode used twice in one class with a different sectionNo\n    resolves to a DIFFERENT subjectName per section.`, body: null },
        { id: "05", name: "A SECTION WITH NOTHING ON", expect: "200 OK",
          notes: `entryCount 0 and an empty list — NOT a 404. The day exists; this\n    section is simply free.`, body: null },
        { id: "06", name: "A SECTION THE CLASS LACKS", expect: "409 Conflict",
          notes: `OUT: { "code": "SECTION_NOT_IN_CLASS" }. THIS is why 05 is a 200:\n    "nothing scheduled" and "wrong section" must never look alike.`, body: null },
        { id: "07", name: "A CLASS FROM ANOTHER YEAR", expect: "404 Not Found",
          notes: `OUT: { "code": "CLASS_NOT_FOUND" } — the class is resolved in the\n    year in the path.`, body: null },
        { id: "08", name: "A HOLIDAY", expect: "404 Not Found",
          notes: `OUT: { "code": "NOT_A_WORKING_DAY" }, naming it. The three day-level\n    refusals are the same three #7 gives.`, body: null },
        { id: "09", name: "A DAY WITH NO TIMETABLE", expect: "404 Not Found",
          notes: `OUT: { "code": "TIMETABLE_NOT_FOUND" }.`, body: null },
        { id: "10", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `Answers. No gate runs on a read.`, body: null },
      ],
    },
    {
      id: "get-teacher-day",
      name: "Get Teacher Day",
      method: "GET",
      path: "/schools/current/academic-years/{year}/timetables/{date}/teachers/{teacherDocsId}",
      status: 'live',
      summary: "One teacher's day \u2014 what a teacher's app opens.",
      schoolSurface: true,
      docs: `**GET** \`/…/timetables/{date}/teachers/{teacherDocsId}\` — endpoint #9.

### What a teacher's app opens

One person, one date, in the order their day happens.

### A break they supervise is part of their day

Since 2026-09-17 a non-lesson may carry a \`teacherDocsId\` — somebody supervises lunch, runs the
assembly, takes the activity. **Those periods are in this list**, and they are why \`lessonCount\`
and \`entryCount\` differ: a teacher with no lessons can still have a working day, and a teacher
named on a break cannot also be teaching period 4.

### Earliest first, for the reason #8 is

A teacher cannot be in two places at once — \`TEACHER_PERIOD_OVERLAP\` is refused on every write —
so within one person's day \`startTime\` is a **total** order. #7 returns the whole school in stored
order because there it is not.

### An unknown id is a 404, not a free day

An app that could not tell those apart would show an empty morning to somebody whose id it had got
wrong. \`404 TEACHER_NOT_FOUND\`.

**A teacher who genuinely has nothing that day is a 200** with an empty list, no
\`firstStartTime\` and no \`lastEndTime\` — absent, not null, because there is no first period to
report a time for.

### It is not "who is free"

\`firstStartTime\` and \`lastEndTime\` are the ends of what this person is **committed to**. The gaps
between periods are not computed here. **#12 is the endpoint that answers coverage** — against every
member of staff rather than one — and it is not built.

**No gate.** Last year's Tuesday still answers.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year. Must exist; need not be the running one." },
        { name: "date", value: "", description: "ISO date, 2026-11-02." },
        { name: "teacherDocsId", value: "{{staffDocsId}}", description: "Must be staff of this school. An unknown id is a 404, not an empty day." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "One teacher's periods, earliest first, with the ends of their day.",
      responseFields: ["date", "academicYear", "dailyTimetableDocsId", "teacherDocsId", "teacherName", "entryCount", "lessonCount", "sectionCount", "firstStartTime", "lastEndTime", "entries", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "BAD_REQUEST", when: "{date} is not an ISO date." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "No academic year of that name in this school." },
        { status: 404, code: "TEACHER_NOT_FOUND", when: "That id is not staff of this school." },
        { status: 404, code: "NOT_A_WORKING_DAY", when: "That date is a holiday or weekly off. The message names it." },
        { status: 404, code: "TIMETABLE_NOT_FOUND", when: "A working day with no timetable written for it yet." },
        { status: 409, code: "DATE_OUTSIDE_ACADEMIC_YEAR", when: "The stored day belongs to a different year than {year}." },
      ],
      examples: [
        { id: "01", name: "A TEACHER'S DAY", expect: "200 OK",
          notes: `OUT: their periods, EARLIEST FIRST, each naming the class and\n    section it is for.`, body: null },
        { id: "02", name: "A BREAK THEY SUPERVISE", expect: "200 OK",
          notes: `It is IN the list and counts against their day. Somebody who ONLY\n    supervises a break has entryCount 1 and lessonCount 0.`, body: null },
        { id: "03", name: "AN UNSUPERVISED BREAK", expect: "200 OK",
          notes: `Belongs to nobody's day — it appears in no teacher's list.`, body: null },
        { id: "04", name: "THE ENDS OF THE DAY", expect: "200 OK",
          notes: `firstStartTime and lastEndTime. The LAST PERIOD'S END, not the\n    last start.`, body: null },
        { id: "05", name: "SECTIONS ARE PAIRED", expect: "200 OK",
          notes: `sectionCount pairs class WITH section — "A" of one class and "A" of\n    another are two.`, body: null },
        { id: "06", name: "A FREE DAY", expect: "200 OK",
          notes: `entryCount 0, an empty list, and NO firstStartTime or lastEndTime —\n    absent, not null. There is no first period to report a time for.`, body: null },
        { id: "07", name: "AN UNKNOWN ID", expect: "404 Not Found",
          notes: `OUT: { "code": "TEACHER_NOT_FOUND" }. THIS is why 06 is a 200: an\n    app must not show a free morning for a wrong id.`, body: null },
        { id: "08", name: "A HOLIDAY", expect: "404 Not Found",
          notes: `OUT: { "code": "NOT_A_WORKING_DAY" }, naming it.`, body: null },
        { id: "09", name: "A DAY UNDER THE WRONG YEAR", expect: "409 Conflict",
          notes: `OUT: { "code": "DATE_OUTSIDE_ACADEMIC_YEAR" }. Staff are not\n    year-scoped, so this endpoint reaches the day check where #8 stops\n    at the class.`, body: null },
        { id: "10", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `Answers. No gate runs on a read.`, body: null },
      ],
    },
    {
      id: "copy-timetable",
      name: "Copy Timetable",
      method: "POST",
      path: "/schools/current/academic-years/{year}/timetables/{date}/copy-from",
      status: 'live',
      summary: "Build this day from another day \u2014 what schools actually do.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/academic-years/{year}/timetables/{date}/copy-from\` — endpoint #6.

### What a school actually does

Nobody types five days. **Monday is built once, and Tuesday through Friday are copied from it and
then corrected.** Without this, a week of a 400-period school is 2,000 periods typed by hand — and
the typing is where the mistakes come from.

**The target is in the path; the source is in the body.** The day being *built* is what this
endpoint acts on, so it is the address — the same reading that puts \`{date}\` in the path on #2
and #7.

### New ids for every copied period, always

They are different periods on a different date. Two days sharing an entry id would make
\`AttendanceSession.timetableEntryId\` ambiguous, which is the single thing generated ids exist to
prevent — so a copy **generates** rather than reuses, even though it is copying. Everything else is
carried across field for field, in the order the source held it.

### The target is validated the way #1 builds a day

Its own year, its own holiday check. **Copying Monday onto a festival is refused** —
\`409 NOT_A_WORKING_DAY\`, naming the holiday.

That is where it differs from #1, which *skips* a holiday: #1 takes a **range** and any range
longer than about five days contains a weekly off, so skipping is the only way ranges stay usable.
A copy names **one** date, and a caller who named a festival meant a different day.

**The source must be in the same academic year.** A class belongs to exactly one, so last year's
Monday names \`classDocsId\`s this year does not have.

**A day cannot be built from itself** — \`400 SOURCE_IS_TARGET\`. Without a merge it is a no-op
dressed as a write; with one it would duplicate every period onto itself, and every duplicate would
clash with the original it came from.

### The filters copy part of a day

\`classDocsId\` copies one class. With \`sectionNo\` it copies one section — which is exactly what a
school **adding a section mid-term** wants.

**\`sectionNo\` alone is \`400 SECTION_WITHOUT_CLASS\`**: "section A" is not one thing across a
school, and a filter that silently matched every class's A would copy three classes where the caller
meant one. Same pairing rule #10 applies with its \`$elemMatch\`.

A filter that matches **nothing** is \`409 NOTHING_TO_COPY\`, not an empty success — a 201 saying
"0 copied" reads as though something worked.

### Merging re-runs every check against the COMBINED list

Without \`merge\`, a target that already has a timetable is \`409 TIMETABLE_ALREADY_EXISTS\`. With
it, the copied periods are **added** to what is there.

**That is the whole risk of merging:** a teacher free in Monday and free in Tuesday can be in two
places once Monday's periods are added to Tuesday's. So the overlap, period-code and structure
checks all run on the combined list, never on what arrived. Periods already in the target **keep
their ids**; merging adds, it never rewrites.

**No \`version\` is required, unlike #2** — a merge cannot erase a period the caller never saw. The
save still carries \`@Version\`, so a writer that got in between is \`409 CONCURRENT_MODIFICATION\`
rather than a lost edit.

### 201 or 200

**201** when it built a day, **200** when it merged into one that already existed. The status says
whether something came into being.

**All three gates run.**`,
      requiredFields: ["sourceDate"],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The year BOTH days belong to. Must be the RUNNING one — gate 4." },
        { name: "date", value: "", description: "The day being BUILT. ISO date. Must be a working day inside the year." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: null,
      successStatus: 201,
      successNote: "The new day in full, both dates, and how many periods were copied against how many were already there.",
      responseFields: ["date", "sourceDate", "dailyTimetableDocsId", "version", "merged", "copiedCount", "keptCount", "timetable", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "VALIDATION_FAILED", when: "sourceDate is absent." },
        { status: 400, code: "SOURCE_IS_TARGET", when: "sourceDate is the date in the path." },
        { status: 400, code: "SECTION_WITHOUT_CLASS", when: "sectionNo was sent with no classDocsId beside it." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "No academic year of that name in this school." },
        { status: 404, code: "TIMETABLE_NOT_FOUND", when: "The source date has no timetable to copy from." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "A copied period names a class this year does not have." },
        { status: 404, code: "TEACHER_NOT_FOUND", when: "A copied period names somebody who is no longer staff." },
        { status: 409, code: "NOTHING_TO_COPY", when: "The class/section filter matched no period of the source day." },
        { status: 409, code: "TIMETABLE_ALREADY_EXISTS", when: "The target already has a timetable and merge was not asked for." },
        { status: 409, code: "NOT_A_WORKING_DAY", when: "The target date is a holiday or weekly off. The message names it." },
        { status: 409, code: "DATE_OUTSIDE_ACADEMIC_YEAR", when: "The target is outside {year}, or the source belongs to another year." },
        { status: 409, code: "SECTION_NOT_IN_CLASS", when: "A copied section is no longer active in its class." },
        { status: 409, code: "SUBJECT_NOT_IN_SECTION", when: "A copied subject is no longer one that section studies." },
        { status: 409, code: "SECTION_PERIOD_OVERLAP", when: "Merging puts one section in two places at once." },
        { status: 409, code: "TEACHER_PERIOD_OVERLAP", when: "Merging puts one teacher in two places at once." },
        { status: 409, code: "ROOM_PERIOD_OVERLAP", when: "Merging sends two sections to one room at once." },
        { status: 409, code: "PERIOD_CODE_TAKEN", when: "Merging gives one section the same period code twice." },
        { status: 409, code: "CONCURRENT_MODIFICATION", when: "Another write changed the target between the read and the merge." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1." },
      ],
      examples: [
        { id: "01", name: "TUESDAY FROM MONDAY", expect: "201 Created",
          notes: `{ "sourceDate": "2026-09-07" }\n    OUT: every period carried across, in the source's order, with NEW\n    timetableEntryIds. copiedCount n, keptCount 0, merged false.`, body: null },
        { id: "02", name: "THE IDS ARE NEW", expect: "201 Created",
          notes: `Compare the target's entry ids with the source's — NOT ONE IS\n    SHARED. Two days sharing an id would make an attendance session's\n    link ambiguous.`, body: null },
        { id: "03", name: "THE SOURCE IS UNTOUCHED", expect: "201 Created",
          notes: `Read the source back after copying. A copy reads one day and\n    writes another.`, body: null },
        { id: "04", name: "ONE CLASS ONLY", expect: "201 Created",
          notes: `+ "classDocsId". Nothing of the other classes comes with it.`, body: null },
        { id: "05", name: "ONE SECTION ONLY", expect: "201 Created",
          notes: `+ "classDocsId" and "sectionNo". WHAT A SCHOOL ADDING A SECTION\n    MID-TERM WANTS. sectionNo folds case, like every other reading of\n    one in this module.`, body: null },
        { id: "06", name: "A SECTION WITH NO CLASS", expect: "400 Bad Request",
          notes: `"sectionNo" alone.\n    OUT: { "code": "SECTION_WITHOUT_CLASS" }. "Section A" is not one\n    thing across a school.`, body: null },
        { id: "07", name: "A FILTER THAT MATCHES NOTHING", expect: "409 Conflict",
          notes: `OUT: { "code": "NOTHING_TO_COPY" }, and NO day is created. A 201\n    saying "0 copied" would read as though something worked.`, body: null },
        { id: "08", name: "A TARGET THAT ALREADY EXISTS", expect: "409 Conflict",
          notes: `OUT: { "code": "TIMETABLE_ALREADY_EXISTS" }, offering merge and #2.`, body: null },
        { id: "09", name: "MERGING INTO IT", expect: "200 OK",
          notes: `+ "merge": true.\n    200, not 201 — nothing came into being. keptCount is what was\n    there and keeps its ids; copiedCount is what arrived with new ones.`, body: null },
        { id: "10", name: "A MERGE THAT CLASHES", expect: "409 Conflict",
          notes: `Two days each valid ALONE, whose teacher is shared at one hour.\n    OUT: { "code": "TEACHER_PERIOD_OVERLAP" } — the checks run on the\n    COMBINED list. Nothing is written.\n    THE WHOLE RISK OF MERGING.`, body: null },
        { id: "11", name: "ONTO A HOLIDAY", expect: "409 Conflict",
          notes: `OUT: { "code": "NOT_A_WORKING_DAY" }, NAMING the holiday.\n    #1 SKIPS a holiday in a range; #6 refuses one. A range is expected\n    to contain a weekly off; a copy names one date.`, body: null },
        { id: "12", name: "A DAY FROM ITSELF", expect: "400 Bad Request",
          notes: `OUT: { "code": "SOURCE_IS_TARGET" }.`, body: null },
        { id: "13", name: "A SOURCE THAT DOES NOT EXIST", expect: "404 Not Found",
          notes: `OUT: { "code": "TIMETABLE_NOT_FOUND" }.`, body: null },
        { id: "14", name: "A SOURCE FROM ANOTHER YEAR", expect: "409 Conflict",
          notes: `OUT: { "code": "DATE_OUTSIDE_ACADEMIC_YEAR" }. A class belongs to\n    ONE year, so last year's Monday names classes this year lacks.`, body: null },
        { id: "15", name: "A YEAR THAT IS NOT RUNNING", expect: "409 Conflict",
          notes: `OUT: { "code": "ACADEMIC_YEAR_NOT_RUNNING" } — gate 4.`, body: null },
        { id: "16", name: "A SUSPENDED SCHOOL", expect: "409 Conflict",
          notes: `OUT: { "code": "SCHOOL_NOT_ACTIVE" } — gate 1, not the service.`, body: null },
      ],
    },
    {
      id: "replace-timetable",
      name: "Replace Timetable",
      method: "PUT",
      path: "/schools/current/academic-years/{year}/timetables/{date}",
      status: 'live',
      summary: "Replace every period of one day, against the version it was read at.",
      schoolSurface: true,
      docs: `**PUT** \`/schools/current/academic-years/{year}/timetables/{date}\` — endpoint #2.

### The one full-document write, and the one to reach for last

Every other write in this module exists so that this one is not needed: #3 adds a period, #4
corrects one — the substitution the module exists for — #5 removes one, #6 copies a day. **This one
overwrites everything**, and a period left out of the list is gone. The module's own plan puts it
last and calls it "a footgun with an audit trail". It is built because a school that has typed a
day wrongly in forty places wants one call rather than forty — and three things blunt it.

### 1. \`version\` is required

A targeted update touches one embedded entry and cannot lose somebody else's edit to another. A
replace can lose all of them. So the caller states which version of the day it is replacing, and a
day that moved on is **409 \`CONCURRENT_MODIFICATION\`** naming *both* versions — not a silent
overwrite of the other clerk's morning.

**It comes from #7, or from #1's echo.** Both now return \`version\` for exactly this reason: a
required field with no way to obtain it would be a refusal nobody could satisfy.

It is checked **twice**. Once against the document just read, which is what produces that message;
and once by \`@Version\` on the save itself, which closes the window between that read and the
write. The first is for the person, the second is for the race.

### 2. Entry ids are kept where they are sent

Send a period's \`timetableEntryId\` back and it **keeps its identity**, so an \`AttendanceSession\`
pointing at it still points at it. Leave it off — or send it blank — for a period being added, and
one is generated.

An id that is not in **this** day is **404 \`TIMETABLE_ENTRY_NOT_FOUND\`**, *including a real id
belonging to another date*: one id on two days would make \`timetableEntryId\` ambiguous, which is
the single thing generated ids exist to prevent. An id sent twice is
**400 \`DUPLICATE_TIMETABLE_ENTRY_ID\`**.

### 3. The response names every removed id

Not a count — the ids. This is the destructive half of the endpoint, and those ids are what an
attendance session may still be pointing at. **The endpoint does not refuse over it** (open item 4
of the plan is unsettled and there is no attendance repository yet). Visibility, not enforcement.

### It replaces; it does not create

A date with no timetable is **404 \`TIMETABLE_NOT_FOUND\`**, pointing at #1. The pair is what keeps
both honest: #1 refuses a taken date, #2 refuses a free one.

### Every rule #1 applies, this applies

The section normalised to the class's own spelling, a subject that section actually studies, the
three overlap checks, the period-code rule, every teacher being this school's. They are literally
the same code — the structure step and the teacher check moved into \`utils\` when this endpoint
needed exactly them.

**A date that has since become a holiday can still be corrected**, and that is deliberate: #1 skips
holidays because it *chooses* its dates; this endpoint is handed one that already has a document,
and refusing would trap a school that cannot delete the day either.

**All three gates run**, unlike the reads. This is the most destructive write in the module.`,
      requiredFields: ["version", "entries"],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The year the day belongs to. Must be the RUNNING one — gate 4." },
        { name: "date", value: "", description: "ISO date, 2026-11-02. The day to replace. It must already exist." },
      ],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: null,
      successStatus: 200,
      successNote: "The day as it now stands, plus what it cost: kept, added, and every removed id by name.",
      responseFields: ["dailyTimetableDocsId", "version", "timetable", "keptCount", "addedCount", "removedEntryIds", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "VALIDATION_FAILED", when: "version is absent, or entries is empty or over 4000." },
        { status: 400, code: "DUPLICATE_TIMETABLE_ENTRY_ID", when: "One timetableEntryId appears twice in the request." },
        { status: 400, code: "INVALID_PERIOD_TIMES", when: "startTime is not before endTime." },
        { status: 400, code: "SLOT_FIELDS_REQUIRED", when: "A LESSON with no subjectCode or teacherDocsId." },
        { status: 400, code: "SLOT_FIELDS_NOT_ALLOWED", when: "A non-lesson carrying a subject. A teacher on one is fine." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "No academic year of that name in this school." },
        { status: 404, code: "TIMETABLE_NOT_FOUND", when: "That date has no timetable — create it with #1. This is not an upsert." },
        { status: 404, code: "TIMETABLE_ENTRY_NOT_FOUND", when: "A sent timetableEntryId is not in THIS day — another date's real id included." },
        { status: 404, code: "CLASS_NOT_FOUND", when: "A classDocsId is not this school's class in that year." },
        { status: 404, code: "TEACHER_NOT_FOUND", when: "A teacherDocsId is not staff of this school." },
        { status: 409, code: "CONCURRENT_MODIFICATION", when: "The version sent is not the day's current version." },
        { status: 409, code: "DATE_OUTSIDE_ACADEMIC_YEAR", when: "The stored day belongs to a different year than {year}." },
        { status: 409, code: "SECTION_NOT_IN_CLASS", when: "A sectionNo is not an active section of that class." },
        { status: 409, code: "SUBJECT_NOT_IN_SECTION", when: "That section does not study the subject." },
        { status: 409, code: "SECTION_PERIOD_OVERLAP", when: "One section in two places at once." },
        { status: 409, code: "TEACHER_PERIOD_OVERLAP", when: "One teacher in two places at once." },
        { status: 409, code: "ROOM_PERIOD_OVERLAP", when: "Two sections sent to one room at once." },
        { status: 409, code: "PERIOD_CODE_TAKEN", when: "One section names a period code twice." },
        { status: 409, code: "ACADEMIC_YEAR_NOT_RUNNING", when: "Gate 4 — that year is not the one the school is running." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1." },
      ],
      examples: [
        { id: "01", name: "KEEP EVERY PERIOD", expect: "200 OK",
          notes: `Send every timetableEntryId back, change one field.\n    OUT: keptCount n, addedCount 0, removedEntryIds []. Every id is the\n    one it came in with — identity survives, and so does the\n    attendance pointing at it.`, body: null },
        { id: "02", name: "DROP A PERIOD", expect: "200 OK",
          notes: `Leave one out of the list.\n    OUT: its id is in removedEntryIds, BY NAME. That list is the\n    closest thing this endpoint has to an undo.`, body: null },
        { id: "03", name: "ADD A PERIOD", expect: "200 OK",
          notes: `One entry with NO timetableEntryId.\n    OUT: addedCount 1, and a fresh id nobody had.`, body: null },
        { id: "04", name: "A BLANK ID IS \"NEW\"", expect: "200 OK",
          notes: `timetableEntryId: "" is the same as leaving it off — a stored id\n    is never empty, so blank cannot be a lookup.`, body: null },
        { id: "05", name: "A STALE VERSION", expect: "409 Conflict",
          notes: `Replace twice with the same version.\n    OUT: { "code": "CONCURRENT_MODIFICATION" } naming BOTH versions.\n    THE CASE THE WHOLE ENDPOINT RESTS ON.`, body: null },
        { id: "06", name: "NO VERSION AT ALL", expect: "400 Bad Request",
          notes: `Required, unlike on every other write here.`, body: null },
        { id: "07", name: "ANOTHER DAY'S REAL ENTRY ID", expect: "404 Not Found",
          notes: `A genuine id, from a genuine day, that is not THIS day's.\n    OUT: { "code": "TIMETABLE_ENTRY_NOT_FOUND" }. Accepting it would\n    put one id on two dates and make AttendanceSession ambiguous.`, body: null },
        { id: "08", name: "ONE ID SENT TWICE", expect: "400 Bad Request",
          notes: `OUT: { "code": "DUPLICATE_TIMETABLE_ENTRY_ID" }.`, body: null },
        { id: "09", name: "A DATE WITH NO TIMETABLE", expect: "404 Not Found",
          notes: `OUT: { "code": "TIMETABLE_NOT_FOUND" }, pointing at #1.\n    NOT an upsert — #1 refuses a taken date, #2 refuses a free one.`, body: null },
        { id: "10", name: "AN EMPTY DAY", expect: "400 Bad Request",
          notes: `entries: []. There is no way to empty a day here, deliberately.`, body: null },
        { id: "11", name: "\"A\" AND \"a\" AT ONE TIME", expect: "409 Conflict",
          notes: `OUT: { "code": "SECTION_PERIOD_OVERLAP" }. The section is\n    normalised to the class's own spelling FIRST — the same shared\n    utils step #1 runs.`, body: null },
        { id: "12", name: "A DAY UNDER THE WRONG YEAR", expect: "409 Conflict",
          notes: `OUT: { "code": "DATE_OUTSIDE_ACADEMIC_YEAR" }. The stored\n    academicYear is the authority.`, body: null },
        { id: "13", name: "A DATE THAT BECAME A HOLIDAY", expect: "200 OK",
          notes: `Declare the day a holiday AFTER writing it, then replace.\n    ALLOWED, deliberately: #1 skips holidays because it chooses its\n    dates; this one is handed a date that already has a document, and\n    refusing would trap a school that cannot delete the day either.`, body: null },
        { id: "14", name: "A YEAR THAT IS NOT RUNNING", expect: "409 Conflict",
          notes: `OUT: { "code": "ACADEMIC_YEAR_NOT_RUNNING" } — gate 4.\n    #7 still reads the same day: no gate runs on a read.`, body: null },
        { id: "15", name: "A SUSPENDED SCHOOL", expect: "409 Conflict",
          notes: `OUT: { "code": "SCHOOL_NOT_ACTIVE" } — gate 1, not the service.`, body: null },
      ],
    },
    {
      id: "get-timetable",
      name: "Get Timetable",
      method: "GET",
      path: "/schools/current/academic-years/{year}/timetables/{date}",
      status: 'live',
      summary: "One school day in full, with the names behind its ids.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/academic-years/{year}/timetables/{date}\` — endpoint #7.

### Addressed by the date, not by a document id

Every other detail read in this project takes a MongoDB id. This one takes an **ISO date**, and
that is right for one reason: **a caller always knows the date and never knows the id.** A
teacher's app asks what is on today; nothing asks what is on in document \`67aa15…\`.
\`school_timetable_date_uniq\` — unique on \`schoolId + date\` — is what makes the date enough.

### The periods come back with names attached

A period stores \`classDocsId\`, \`subjectCode\` and \`teacherDocsId\`, none of which a person can
read. Each entry therefore also carries \`className\`, \`subjectName\` and \`teacherName\`, resolved
in **two extra queries** rather than one request per id — about seventy round trips for a day of
four hundred periods across twelve classes and sixty staff.

**A name that cannot be found is left out and the id stays.** A class deleted, a subject retired
or a staff member removed *after* the day was written must not stop last Tuesday from answering.

\`facilityResourceDocsId\` is **not** resolved. #1 does not check that a room exists when it writes
one, so a stored id may name nothing — and a blank name would hide that.

### Entries come back in stored order, never re-sorted

Sorting by time is the obvious choice and the wrong one: periods of different **sections** run at
the same hour, so "by time" is not an order, it is a tie with a hidden second key. Which grouping a
screen wants — by section, by teacher, by hour — is the screen's question, and #8, #9 and #11 are
the endpoints that answer it for one of each.

### Three refusals, and they say different things

| | |
|---|---|
| **409 \`DATE_OUTSIDE_ACADEMIC_YEAR\`** | The caller's year and date disagree. A mistake in the question, not an absence in the answer — the same refusal #1 gives. |
| **404 \`NOT_A_WORKING_DAY\`** | The school was closed, and the message **names the holiday**. Nothing is missing. |
| **404 \`TIMETABLE_NOT_FOUND\`** | A working day with nothing written. **This is the one a school acts on.** |

A screen that could not tell the last two apart would be reporting a gap on Independence Day.

### The same five counts as a row of #10

Repeated rather than assumed to be in hand: a caller reaching a day by a **link** — a bookmark, an
attendance record pointing back at the date it was taken on — never saw the list. They cost
nothing here, because the entries are already loaded.

**No gate runs on a read.** A suspended school still reads its own timetable, and last year's
Tuesday still answers.`,
      requiredFields: [],
      pathParams: [
        { name: "year", value: "{{academicYearName}}", description: "The academic year the day belongs to. Must exist; need not be the running one." },
        { name: "date", value: "", description: "ISO date, 2026-11-02. The business key — unique per school." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "The whole school's day, entries in stored order, each with the names behind its ids.",
      responseFields: ["dailyTimetableDocsId", "date", "academicYear", "entryCount", "lessonCount", "classCount", "sectionCount", "teacherCount", "entries", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "BAD_REQUEST", when: "{date} is not an ISO date — 02-11-2026, or 2026-13-01." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "No academic year of that name in this school." },
        { status: 404, code: "NOT_A_WORKING_DAY", when: "That date is a holiday or weekly off. The message names it." },
        { status: 404, code: "TIMETABLE_NOT_FOUND", when: "A working day with no timetable written for it yet." },
        { status: 409, code: "DATE_OUTSIDE_ACADEMIC_YEAR", when: "The date falls outside {year}'s own range, or the stored day belongs to a different year." },
      ],
      examples: [
        { id: "01", name: "A DAY THAT EXISTS", expect: "200 OK",
          notes: `A date #1 wrote.\n    OUT: every period of every section, in stored order, each carrying\n    className, subjectName and teacherName beside its ids.`, body: null },
        { id: "02", name: "THE NAMES ARE RESOLVED", expect: "200 OK",
          notes: `Compare against #1's echo, which has ids only.\n    className, subjectName, teacherName — two extra queries, not one\n    per id.`, body: null },
        { id: "03", name: "A BREAK NOBODY SUPERVISES", expect: "200 OK",
          notes: `teacherDocsId and teacherName both ABSENT, not null.\n    A break with a supervisor DOES carry both — allowed since\n    2026-09-17.`, body: null },
        { id: "04", name: "A ROOM IS NOT RESOLVED", expect: "200 OK",
          notes: `facilityResourceDocsId comes back as an id and there is no room\n    name. #1 never checked the room exists, so a name would be a\n    guess. Open item 3 of the plan.`, body: null },
        { id: "05", name: "A HOLIDAY", expect: "404 Not Found",
          notes: `A date the school listed in AcademicYear.holidays.\n    OUT: { "code": "NOT_A_WORKING_DAY" }, NAMING the holiday.\n    Nothing is missing — this is not the one to act on.`, body: null },
        { id: "06", name: "A WORKING DAY WITH NOTHING ON IT", expect: "404 Not Found",
          notes: `OUT: { "code": "TIMETABLE_NOT_FOUND" }.\n    THE ONE A SCHOOL ACTS ON — and the whole reason 05 is a separate\n    code rather than the same 404.`, body: null },
        { id: "07", name: "A DATE OUTSIDE THE YEAR", expect: "409 Conflict",
          notes: `A date before the year starts or after it ends.\n    OUT: { "code": "DATE_OUTSIDE_ACADEMIC_YEAR" }, naming the year's\n    own range. 409, not 404: the question is wrong, not the answer\n    empty.`, body: null },
        { id: "08", name: "TWO YEARS COVERING ONE DATE", expect: "409 Conflict",
          notes: `Two years whose ranges overlap. Ask for the day under the year it\n    was NOT written into.\n    OUT: { "code": "DATE_OUTSIDE_ACADEMIC_YEAR" } naming the year it\n    DOES belong to. The stored academicYear is the authority, not the\n    range check.`, body: null },
        { id: "09", name: "A MALFORMED DATE", expect: "400 Bad Request",
          notes: `/timetables/02-11-2026\n    ISO only. The path is a date, not a document id, so this is the\n    shape error that replaces "not found".`, body: null },
        { id: "10", name: "A YEAR THAT HAS ENDED", expect: "200 OK",
          notes: `Still answers. No gate runs on a read, and reading last year's\n    Tuesday is how an attendance record taken against it is explained.`, body: null },
        { id: "11", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `Also answers.`, body: null },
      ],
    },
  ],
};

const GROUP_LOCAL_USER = {
  id: "local-user",
  module: "Local user",
  endpoints: [
    {
      id: "store-local-user",
      name: "Store Local User",
      method: "POST",
      path: "/local-user",
      status: 'live',
      summary: "Issue a signed id token and store it in the idtoken cookie.",
      schoolSurface: false,
      docs: `**POST** \`/local-user\`

### What it is for

The three pickers in the top bar — school, year, staff — are remembered in \`localStorage\` so the
tester keeps them across reloads. This puts the same three in a **cookie**, so that anything which
reads cookies sees them too.

**The tester calls it for you.** \`ApiProvider\` re-sends it whenever the school, the year or the
staff member changes, which is why it appears in the log without anybody pressing anything.

### The signature does not make it a credential

Since 2026-09-19 the cookie holds a **signed** token rather than plain JSON. That changes exactly
one thing: it can no longer be **edited** in developer tools without the signature failing.

**It changes nothing about who may ask for one.** Nothing authenticates the caller, so anybody can
POST any \`schoolId\` and receive a validly signed token asserting it. The signature proves *this
server issued the token*, not that its claims are true — **tamper-evident, not trustworthy**. Every
response repeats that in a \`warning\` field.

The signing secret is \`app.local-user.jwt-secret\`. Unset, it uses a built-in development default,
the application warns at startup, and the response says \`signedWithDefaultSecret: true\` rather than
implying a guarantee it is not making.

The specific line that must not be crossed is \`CurrentSchoolResolver\` reading it — the moment it
does, every \`schoolId\` check in the repositories is satisfied by a text field the caller controls.
The tenant still comes from \`X-School-Subdomain\`.

### The cookie holds a signed JWT

\`idtoken\`, holding an **HS256 JWT**. Its claims are \`schoolId\`, \`staffDocsId\` and
\`academicYear\` — plus any extras you send — and the server adds \`iss\`, \`iat\` and \`exp\`. The
token's expiry and the cookie's \`Max-Age\` are **the same number**, because two lifetimes for one
thing is how a browser ends up holding a cookie whose token expired an hour ago.

\`Path=/\`, \`HttpOnly\` (so the page cannot read it back — the response hands the token over in the
body instead), \`SameSite=Lax\`, and **\`Secure\` only when the request itself was HTTPS**: a Secure
cookie is discarded by the browser on an \`http://\` page, so setting it unconditionally would mean
it silently never arrived in local development.

**\`maxAgeSeconds: 0\` mints no token at all** — one already expired as it is signed helps nobody —
and expires the cookie instead. That is how a context is cleared.

### There is no JWT library

The build has no JOSE dependency and this needs one thing: HMAC-SHA256 over
\`base64url(header).base64url(claims)\`, which the JDK's \`Mac\` already does. Adding a dependency for
a dev-convenience endpoint would change the build for everybody.

**That stops being true the moment anything VERIFIES one of these.** Parsing an attacker-controlled
JWT is where the well-known vulnerabilities live — \`alg: none\`, algorithm confusion, claim type
coercion — and is exactly where a library earns its place. The signing lives in
\`LocalUserController\` and deliberately only signs.

### Two things that had to be true for this to work at all

**It must go through the dev proxy.** \`DevCorsConfig\` sets \`allowCredentials(false)\`, and without
credentials a browser **ignores \`Set-Cookie\` on a cross-origin XHR** — silently, with a 200 in the
network tab. Calling \`http://localhost:3456\` straight from a page would appear to work and store
nothing.

**\`/local-user\` had to be added to the Vite proxy**, which previously forwarded only \`/platform\`
and \`/schools\`. Without that rule the dev server answers with its own 404 and the request never
reaches Spring.

### Everything is optional

An empty body stores an empty context — a real thing to want. \`maxAgeSeconds: 0\` expires the
cookie, which is how a context is cleared without a second endpoint. A blank value is left out
rather than stored as \`""\`, because a staff member whose id is the empty string is not the same
fact as no staff member.

**No gates.** It touches no school and no subscription.`,
      requiredFields: [],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: null,
      successStatus: 200,
      successNote: "The cookie is set, and the body repeats exactly what went into it.",
      responseFields: ["cookieName", "idToken", "claims", "tokenLength", "maxAgeSeconds", "expiresAt", "secure", "signedWithDefaultSecret", "warning"],
      captures: [],
      errors: [
        { status: 400, code: "EXTRA_KEY_RESERVED", when: "An 'extra' key collides with staffDocsId, schoolId or academicYear." },
        { status: 400, code: "BLANK_EXTRA_KEY", when: "An entry in 'extra' has a blank name." },
        { status: 400, code: "INVALID_MAX_AGE", when: "maxAgeSeconds is negative. Send 0 to expire it now." },
        { status: 400, code: "CONTEXT_TOO_LARGE", when: "The encoded cookie would exceed 3500 characters — a browser drops an oversized cookie without reporting it." },
        { status: 400, code: "VALIDATION_FAILED", when: "More than 20 extras, or a field over its length cap." },
      ],
      examples: [
        { id: "01", name: "THE THREE PICKERS", expect: "200 OK",
          notes: `{ "staffDocsId": "...", "schoolId": "...", "academicYear": "2026-2027" }\n    OUT: Set-Cookie: idtoken=<jwt>, and the same token in the body.\n    Paste it into a decoder: the three are claims, beside iss/iat/exp.\n    THIS IS WHAT THE SIGN IN BUTTON SENDS.`, body: null },
        { id: "02", name: "THE SIGNATURE HOLDS", expect: "200 OK",
          notes: `Verify the token with the signing secret — it passes. Edit one\n    character of the payload and it does not. That is the ONLY thing\n    signing buys: it cannot be edited after issue. Anybody can still\n    ASK for one saying anything.`, body: null },
        { id: "02", name: "AN EMPTY BODY", expect: "200 OK",
          notes: `{ }\n    Stores an empty context and still sets a cookie. Sending no body at\n    all does the same.`, body: null },
        { id: "03", name: "BLANK IS ABSENT", expect: "200 OK",
          notes: `{ "staffDocsId": "  ", "schoolId": "S1" }\n    OUT: stored holds only schoolId. A stored "" would read back as an\n    id that is the empty string.`, body: null },
        { id: "04", name: "EXTRAS", expect: "200 OK",
          notes: `+ "extra": { "role": "COUNSELLOR" }\n    Stored beside the named fields.`, body: null },
        { id: "05", name: "AN EXTRA THAT COLLIDES", expect: "400 Bad Request",
          notes: `"extra": { "schoolId": "other" } alongside a named schoolId.\n    OUT: { "code": "EXTRA_KEY_RESERVED" } — one request setting one\n    value twice would depend on map order.`, body: null },
        { id: "06", name: "CLEARING IT", expect: "200 OK",
          notes: `"maxAgeSeconds": 0 — expires the cookie and mints NO token. One\n    already expired as it is signed helps nobody.`, body: null },
        { id: "07", name: "TOO BIG", expect: "400 Bad Request",
          notes: `OUT: { "code": "CONTEXT_TOO_LARGE" }. Refused rather than stored\n    and lost — a browser drops an oversized cookie silently. A JWT is\n    bigger than its claims, so this bites sooner than raw JSON did.`, body: null },
        { id: "08", name: "NO TENANT HEADER", expect: "200 OK",
          notes: `It needs none. There is no school to resolve and no gate to run.`, body: null },
      ],
    },
  ],
};

const GROUP_CRM_ADMISSION_CYCLES = {
  id: "crm-admission-cycles",
  module: "CRM / Admission cycles",
  endpoints: [
    {
      id: "create-admission-cycle",
      name: "Create Admission Cycle",
      method: "POST",
      path: "/schools/current/admission-cycles",
      status: 'live',
      summary: "Open a year for admissions. The first call in the module.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/admission-cycles\` — endpoint #1, and the only one built.

### Nothing else in CRM works until one exists

Every admission application has to name a cycle, so this is the first call anybody makes. The
other thirty-three endpoints are planned in the module's README; none of them is built, so an
admission cycle currently goes in and cannot be read back out.

### THE YEAR DOES NOT HAVE TO BE THE RUNNING ONE — this is the point of the module

Every other module refuses a write against a year the school is not running. **Gate 4 is not run
here at all.** A school sets up its 2027-2028 admissions in the middle of 2026-2027, and often
works both at once: late admissions into the running year while next year's cycle is open. A gate
4 here would refuse the module's normal case.

Try it: the same year that \`Create Class\` refuses with \`ACADEMIC_YEAR_NOT_RUNNING\` is accepted
here. That difference is the module's reason to exist.

**What takes its place** is the cycle's own status — an application can only be submitted into a
cycle that is \`OPEN\`. That is a check on the cycle, not on the year, and it lives on #17, which
is not built.

### No {year} in the path either

Unlike classes, terms and timetables, where the year IS the scope. Here it is a **property**: a
school routinely has two cycles live for two different years, and a path segment would make "show
me both" unaskable. So \`academicYear\` is a required field in the body.

### It is created empty and not started

Status \`DRAFT\`, and \`capacities: []\`. Seats are #4 and opening it is #3, neither built. A
school names and dates a round before it has worked out how many seats each class gets — and a
create that could fail on either a duplicate name or a bad seat row leaves the caller working out
which.

### The four dates, and what each one is

| Field | What it is |
|---|---|
| \`inquiryOpenAt\` | The first day the front desk logs a parent's enquiry against this round. A school gathers interest for weeks before it takes any forms. |
| \`applicationOpenAt\` | The first moment a family can actually submit a form. |
| \`applicationCloseAt\` | The last moment a form is taken. |
| \`enrollmentDeadlineAt\` | The last moment a family who was **offered** a seat can take it and become a student. After it the seat goes to somebody on the waitlist. |

All four are optional and **only the ones sent are compared**, in that order. A school often creates
the cycle before its calendar is settled, so sending just the application window is normal. A gap in
the middle is fine; what is checked is that the ones present run forwards, **including across a
gap**.

**\`18:29:59Z\` in the examples is one second to midnight in India.** An Instant is UTC, so a
school's own end of day is 5½ hours earlier than it looks — \`23:59:59Z\` would hand an Indian
school most of the next day as well.

### Nothing enforces any of the four

They are stored, given back, and read by nothing else. The endpoints that would obey them are #8,
#17 and #33, and none is built. Even once they are, what decides whether an application can be
taken is the cycle's own **status** being \`OPEN\` (#3), not the date. **These four are the
school's published calendar; the status is the switch.**

### The name is unique per YEAR, not per school

"Main intake" in 2026-2027 and "Main intake" in 2027-2028 are two different cycles and both are
allowed. A school runs more than one round for a year — a general intake and a scholarship round —
and the name is the only thing staff have to tell them apart.`,
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "academicYear": "{{academicYearName}}",
  "name": "Main intake"
}`,
      successStatus: 201,
      successNote: "Also sends a Location header pointing at the new cycle by its document id.",
      responseFields: ["admissionCycleId", "academicYear", "name", "status", "inquiryOpenAt", "applicationOpenAt", "applicationCloseAt", "enrollmentDeadlineAt", "capacityCount", "notes", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "A missing or blank academicYear or name; a name over 120 characters; notes over 2000." },
        { status: 400, code: "CYCLE_DATES_OUT_OF_ORDER", when: "Two of the dates that were sent run backwards. The message names both in words and shows them the way a person reads a date." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "No idtoken cookie. Press Sign in — the tenant no longer comes from a header." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "The cookie names a school that is not there." },
        { status: 404, code: "ACADEMIC_YEAR_NOT_FOUND", when: "No year with that name in this school. The year must exist — it just need not be running." },
        { status: 409, code: "CYCLE_NAME_TAKEN", when: "That year already has a cycle with that name. A different year may reuse it." },
        { status: 409, code: "SCHOOL_NOT_READY", when: "Gate 1 — the school is still PROVISIONING." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
      ],
      examples: [
        {
          id: "01",
          name: "OPEN A YEAR FOR ADMISSIONS",
          expect: "201 Created",
          notes: `The body above.
    OUT: admissionCycleId, status "DRAFT", capacityCount 0, and a nextStep
    saying to set the seats and then open it.`,
          body: null,
        },
        {
          id: "02",
          name: "A YEAR THE SCHOOL IS NOT RUNNING",
          expect: "201 Created",
          notes: `THE ONE WORTH RUNNING. Pick a year whose isThisYearRunning is false.
    Create Class against that same year answers 409 ACADEMIC_YEAR_NOT_RUNNING;
    this answers 201, because gate 4 is not run in this module.`,
          body: `{
  "academicYear": "2027-2028",
  "name": "Next year main intake"
}`,
        },
        {
          id: "03",
          name: "WITH ALL FOUR DATES",
          expect: "201 Created",
          notes: `They have to run forwards: enquiries open, applications open,
    applications close, then the enrollment deadline.`,
          body: `{
  "academicYear": "{{academicYearName}}",
  "name": "Dated intake",
  "inquiryOpenAt": "2026-10-01T00:00:00Z",
  "applicationOpenAt": "2026-11-01T00:00:00Z",
  "applicationCloseAt": "2027-01-31T18:29:59Z",
  "enrollmentDeadlineAt": "2027-03-15T18:29:59Z",
  "notes": "Board intake for the main campus."
}`,
        },
        {
          id: "04",
          name: "ONLY SOME OF THE DATES",
          expect: "201 Created",
          notes: `All four are optional and only the ones sent are compared, so a
    school can give the application window now and fill the rest in later.`,
          body: `{
  "academicYear": "{{academicYearName}}",
  "name": "Half dated intake",
  "applicationOpenAt": "2026-11-01T00:00:00Z",
  "applicationCloseAt": "2027-01-31T18:29:59Z"
}`,
        },
        {
          id: "05",
          name: "DATES IN THE WRONG ORDER",
          expect: "400 CYCLE_DATES_OUT_OF_ORDER",
          notes: `Applications close before they open.
    OUT: a message naming both dates in words, not ISO.`,
          body: `{
  "academicYear": "{{academicYearName}}",
  "name": "Backwards intake",
  "applicationOpenAt": "2027-02-01T00:00:00Z",
  "applicationCloseAt": "2027-01-01T00:00:00Z"
}`,
        },
        {
          id: "06",
          name: "A BREAK ACROSS A GAP",
          expect: "400 CYCLE_DATES_OUT_OF_ORDER",
          notes: `The first and last dates only, running backwards. A missing
    middle does not stop the two that ARE present being compared.`,
          body: `{
  "academicYear": "{{academicYearName}}",
  "name": "Gapped backwards intake",
  "inquiryOpenAt": "2027-06-01T00:00:00Z",
  "enrollmentDeadlineAt": "2027-03-15T00:00:00Z"
}`,
        },
        {
          id: "07",
          name: "THE SAME NAME TWICE IN ONE YEAR",
          expect: "409 CYCLE_NAME_TAKEN",
          notes: `Send case 01 again.
    OUT: a message naming the year and the cycle that already holds it.`,
          body: null,
        },
        {
          id: "08",
          name: "THE SAME NAME IN A DIFFERENT YEAR",
          expect: "201 Created",
          notes: `Allowed. The name only has to be free inside its own year.`,
          body: `{
  "academicYear": "2027-2028",
  "name": "Main intake"
}`,
        },
        {
          id: "09",
          name: "A YEAR THAT DOES NOT EXIST",
          expect: "404 ACADEMIC_YEAR_NOT_FOUND",
          body: `{
  "academicYear": "1999-2000",
  "name": "Ghost intake"
}`,
        },
        {
          id: "10",
          name: "NO NAME",
          expect: "400 VALIDATION_FAILED",
          body: `{
  "academicYear": "{{academicYearName}}"
}`,
        },
        {
          id: "11",
          name: "WITHOUT SIGNING IN",
          expect: "400 TENANT_NOT_RESOLVED",
          notes: `Clear the idtoken cookie, then send case 01. The school comes
    from that cookie now; the X-School-Subdomain header is no longer read.`,
          body: null,
        },
      ],
    },
  ],
};

const GROUP_PEOPLE_DEPARTMENT = {
  id: "people-department",
  module: "People / Department",
  endpoints: [
    {
      id: "create-department",
      name: "Create Department",
      method: "POST",
      path: "/schools/current/departments",
      status: 'live',
      summary: "Create an org unit, optionally under another.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/departments\` — endpoint #9.

### The first call anyone makes against the people module

\`POST /staff\` looks like it should be, but the write that actually employs somebody needs a
\`positionDocsId\`, and a position needs a department. **This is where a school's people data
starts.**

### departmentCode is given, never derived

The rule this project settled for \`termCode\`: deriving a code from a name ties two fields that
do not move together. A school renaming "Academics" to "Teaching & Learning" must not be offered a
new code for a unit twenty positions reference — the **name** is what a person reads, the **code**
is what a filter and an export are written against.

It is stored **trimmed and upper-cased**, so \`"  admin  "\` and \`"ADMIN"\` are one code. A person
typing a filter should not have to know which case the school used that day.

### Two units may share a name — only the code is unique

\`school_department_code_uniq\` names \`departmentCode\` alone. A school with two units both called
"Science" under different parents is a real org chart, not a mistake.

### The head is validated to exist, and NOT to be employed

During setup a school enters its org chart before its employment records. Refusing a head with no
employment record would force it to work backwards, so the check is existence and tenant only.

### active is not accepted

It starts \`true\`. Retiring is #11 — an event with its own endpoint, the way every lifecycle flag
in this project works. Sending it is **ignored, not refused**: the ordinary shape for a field a
request record does not declare.

### Two gates, not three

Gate 4 asks whether a named academic year is the school's working one, and **no path here carries
a year to ask it about**. An org chart outlives any year — a department exists before the first
year opens and after the last one ends, and this endpoint still answers after every year has been
ended.

### No cycle check here

A brand-new department has no children, so it cannot be its own ancestor whatever parent it names.
The walk belongs to **#10**, which can move an existing unit under its own descendant.

### The twelve test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain. No year anywhere — an org chart outlives them.

 THIS IS THE FIRST CALL of the people module. A position needs a department;
 the write that employs somebody needs a position.

 departmentCode IS GIVEN, NEVER DERIVED — the termCode rule. Stored trimmed
 and UPPER-CASED, so "admin" and "ADMIN" are one code.

 TWO UNITS MAY SHARE A NAME. Only the code is unique.

 THE HEAD IS VALIDATED TO EXIST, NOT TO BE EMPLOYED. A school enters its org
 chart before its employment records.

 active IS NOT ACCEPTED. It starts true; retiring is #11. Sending it is
 ignored, not refused.

 NO GATE 4. There is no year in this path to ask it about.`,
      requiredFields: ["departmentCode", "name"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "departmentCode": "ACADEMICS",
  "name": "Academic Department",
  "description": "Curriculum and teaching operations."
}`,
      successStatus: 201,
      successNote: "Also sends a Location header pointing at the unit by its document id, not its code.",
      responseFields: ["departmentDocsId", "departmentCode", "name", "active"],
      captures: [
        { from: "departmentDocsId", into: "departmentDocsId", description: "Positions reference this, and #10 to #12 address the unit by it." },
      ],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "No departmentCode or name, either blank, a code over 40, or a name over 120." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "DEPARTMENT_NOT_FOUND", when: "The parentDepartmentDocsId is not a department of this school — including another school's real id." },
        { status: 404, code: "STAFF_NOT_FOUND", when: "The headStaffDocsId is not a staff member of this school, including another school's real id." },
        { status: 409, code: "DEPARTMENT_CODE_TAKEN", when: "This school already uses that code. Case-folded, so ADMIN and admin collide." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
      ],
      examples: [
        {
          id: "01",
          name: "A TOP-LEVEL UNIT",
          expect: "201 Created",
          notes: `The body above.
    OUT: departmentDocsId, active true, and NO parentDepartmentDocsId field
    at all — absent, not null.`,
          body: null,
        },
        {
          id: "02",
          name: "THE CODE IS NORMALISED",
          expect: "201 Created",
          notes: `OUT: departmentCode is ADMIN — trimmed and upper-cased.
    The NAME does not decide the code; they move independently.`,
          body: `{
  "departmentCode": "  admin  ",
  "name": "Administration"
}`,
        },
        {
          id: "03",
          name: "THE SAME CODE AGAIN",
          expect: "409 Conflict",
          notes: `OUT: { "code": "DEPARTMENT_CODE_TAKEN" }
    Case-folded, so sending "admin" after "ADMIN" is the same refusal.`,
          body: `{
  "departmentCode": "ADMIN",
  "name": "A different unit entirely"
}`,
        },
        {
          id: "04",
          name: "TWO UNITS SHARING A NAME",
          expect: "201 Created",
          notes: `Allowed — only the code is unique. Two "Science" units under
    different parents is a real org chart.`,
          body: `{
  "departmentCode": "ACADEMICS_2",
  "name": "Academic Department"
}`,
        },
        {
          id: "05",
          name: "NESTED UNDER ANOTHER",
          expect: "201 Created",
          notes: `Put a real departmentDocsId in parentDepartmentDocsId.
    OUT: the child names its parent.`,
          body: `{
  "departmentCode": "SCIENCE",
  "name": "Science",
  "parentDepartmentDocsId": "{{departmentDocsId}}"
}`,
        },
        {
          id: "06",
          name: "AN UNKNOWN PARENT",
          expect: "404 Not Found",
          notes: `OUT: { "code": "DEPARTMENT_NOT_FOUND" }
    Another school's REAL department id is the same 404 — the lookup carries
    schoolId, so a real id belonging elsewhere is not an accepted parent.`,
          body: `{
  "departmentCode": "ORPHAN",
  "name": "Orphan",
  "parentDepartmentDocsId": "deadbeefdeadbeefdeadbeef"
}`,
        },
        {
          id: "07",
          name: "A BLANK PARENT",
          expect: "201 Created",
          notes: `"   " is treated as none, not as a lookup that fails.`,
          body: `{
  "departmentCode": "BLANKPARENT",
  "name": "Blank parent",
  "parentDepartmentDocsId": "   "
}`,
        },
        {
          id: "08",
          name: "A HEAD WITH NO EMPLOYMENT RECORD",
          expect: "201 Created",
          notes: `A real Staff.id of this school. There is no POST /staff yet, so
    insert one directly to try it.
    ACCEPTED ON PURPOSE: a school enters its org chart before its employment
    records, and refusing this would make it work backwards.`,
          body: `{
  "departmentCode": "HEADED",
  "name": "Headed unit",
  "headStaffDocsId": "67aa15d9dc3f7d0011111111"
}`,
        },
        {
          id: "09",
          name: "AN UNKNOWN HEAD",
          expect: "404 Not Found",
          notes: `OUT: { "code": "STAFF_NOT_FOUND" }
    Another school's real staff id is the same 404.`,
          body: null,
        },
        {
          id: "10",
          name: "THE FIELDS THIS ENDPOINT WILL NOT TAKE",
          expect: "201 Created",
          notes: `active and schoolId are IGNORED, not refused.
    OUT: active is true anyway, and the unit belongs to the calling school.
    Retiring is #11; the tenant comes from the header.`,
          body: `{
  "departmentCode": "IGNORED",
  "name": "Ignored flags",
  "active": false,
  "schoolId": "67aa15d9dc3f7d0099999999"
}`,
        },
        {
          id: "11",
          name: "A SUSPENDED SCHOOL",
          expect: "409 Conflict",
          notes: `Suspend the school, then send case 01.
    OUT: { "code": "SCHOOL_NOT_ACTIVE" } — gate 1.`,
          body: null,
        },
        {
          id: "12",
          name: "AFTER EVERY YEAR HAS ENDED",
          expect: "201 Created",
          notes: `Run POST /academic-years/{name}/end on every year, then create one.
    STILL WORKS: gate 4 does not run here. An org chart outlives any year,
    and there is no year in this path to ask about.`,
          body: null,
        },
      ],
    },
    {
      id: "create-position",
      name: "Create Position",
      method: "POST",
      path: "/schools/current/positions",
      status: 'live',
      summary: "Create an approved position inside a department.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/positions\` — endpoint #13.

### A position has no code

\`positionCode\` was removed on **2026-09-15**. A position is addressed by its document id — which is
what \`EmploymentRecord.positionDocsId\` already stored — so **\`title\` is what names a position**, and
it carries the uniqueness the code used to.

Its unique index went with it, necessarily: an index naming a field the model no longer declares
is not inert. Every document then indexes a *missing* value, so the key is identical for all of
them and the collection accepts exactly **one** row per school.

### title is unique within its department

\`409 POSITION_TITLE_TAKEN\`, scoped to \`{schoolId, departmentDocsId, title}\`. The same title in
a **different** department is fine — two "Mathematics Teacher" positions in Academics and in
Continuing Education are different positions.

**The check folds case; the index does not.** Mongo compares a unique key case-sensitively, so
"Mathematics Teacher" and "mathematics teacher" are two keys to the index and one title to the
service. The service is the stricter of the two, and therefore the enforcement in practice.

**A retired position keeps its title**, because the index does not filter on \`active\` — consistent
with a retired term keeping its code.

### The department must be ACTIVE, not merely present

\`409 DEPARTMENT_NOT_ACTIVE\`. A position nobody may be hired into, inside a unit that no longer
exists, is two problems rather than one.

### The reporting line crosses departments on purpose

\`reportsToPositionDocsId\` is checked to be **this school's** and nothing else. A school with one
Head of Safeguarding that every unit reports to on that line is a real structure — the org tree
and the reporting line answer different questions and are deliberately not kept consistent.

**No cycle check here:** a brand-new position has nothing reporting to it, so it cannot be its own
ancestor. That walk belongs to #14.

### approvedHeadcount follows the model, not the plan

The plan says "null means uncapped". The **model** declares it \`@NotNull\` with a default of
**1**, so uncapped is not a state a stored position can be in. Absent or null becomes 1; zero and
negative are \`400\`. Making uncapped real means dropping \`@NotNull\` from \`Position\`.

### The teaching warning

\`teachingPosition\` defaults to \`false\` and **should almost always be sent** — it is what a
teacher picker filters on. When a department's positions are *all* non-teaching the response carries a
\`warning\`, because that is legitimate for Finance and is also exactly what an empty picker looks
like. It is **per department**, so creating Facilities does not warn a school whose Academics
positions are correctly flagged.

### The fifteen test cases are in the request body as comments
`,
      bodyNotes: `Needs X-School-Subdomain and an ACTIVE department id from Create Department.

 A POSITION HAS NO CODE. positionCode was removed 2026-09-15; a position is
 addressed by its document id, which EmploymentRecord already stored.

 title IS UNIQUE WITHIN ITS DEPARTMENT — the job the code used to do. The
 service folds case, the index does not. A RETIRED POSITION KEEPS ITS TITLE.

 THE DEPARTMENT MUST BE ACTIVE, not merely present.

 THE REPORTING LINE MAY CROSS DEPARTMENTS, on purpose. It is checked to be
 this school's and nothing else.

 approvedHeadcount FOLLOWS THE MODEL: absent or null becomes 1, and
 "uncapped" is not storable because Position declares it @NotNull.

 teachingPosition DEFAULTS FALSE and should almost always be sent. A
 department with no teaching position gets a WARNING, not a refusal.`,
      requiredFields: ["title", "departmentDocsId"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "Content-Type", value: "application/json", enabled: true },
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: true,
      body: `{
  "title": "Mathematics Teacher",
  "departmentDocsId": "{{departmentDocsId}}",
  "approvedHeadcount": 4,
  "teachingPosition": true
}`,
      successStatus: 201,
      successNote: "Also sends a Location header pointing at the position by its document id. There is no code.",
      responseFields: ["positionDocsId", "title", "departmentDocsId", "approvedHeadcount", "teachingPosition", "active"],
      captures: [
        { from: "positionDocsId", into: "positionDocsId", description: "What #16 needs to employ somebody, and what EmploymentRecord stores." },
      ],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "No title or departmentDocsId, either blank, a title over 120, or an approvedHeadcount below 1." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "DEPARTMENT_NOT_FOUND", when: "The departmentDocsId is not a department of this school, including another school's real id." },
        { status: 404, code: "POSITION_NOT_FOUND", when: "The reportsToPositionDocsId is not a position of this school, including another school's real id." },
        { status: 409, code: "DEPARTMENT_NOT_ACTIVE", when: "The department exists but is retired. A position cannot be created in it." },
        { status: 409, code: "POSITION_TITLE_TAKEN", when: "That department already has a position with that title, case-folded. Retired positions count." },
        { status: 409, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended, closed or deleted." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — expired, suspended, or the period has ended." },
      ],
      examples: [
        { id: "01", name: "A TEACHING POSITION", expect: "201 Created",
          notes: `The body above.\n    OUT: positionDocsId, active true, and NO positionCode field — there is\n    no such field any more. No warning, because this position teaches.`, body: null },
        { id: "02", name: "THE DEFAULTS", expect: "201 Created",
          notes: `Only title and departmentDocsId.\n    OUT: approvedHeadcount 1, teachingPosition false.`,
          body: `{
  "title": "Lab Assistant",
  "departmentDocsId": "{{departmentDocsId}}"
}` },
        { id: "03", name: "AN EXPLICIT NULL HEADCOUNT", expect: "201 Created",
          notes: `OUT: approvedHeadcount is 1, NOT uncapped. The model declares it\n    @NotNull, so uncapped is not a storable state.`,
          body: `{
  "title": "Explicit null",
  "departmentDocsId": "{{departmentDocsId}}",
  "approvedHeadcount": null
}` },
        { id: "04", name: "A HEADCOUNT OF ZERO", expect: "400 Bad Request",
          notes: `@Min(1). A position nobody may be hired into is not a position.`, body: null },
        { id: "05", name: "THE SAME TITLE AGAIN", expect: "409 Conflict",
          notes: `Send case 01 twice.\n    OUT: { "code": "POSITION_TITLE_TAKEN" }, naming the department.`, body: null },
        { id: "06", name: "THE SAME TITLE IN ANOTHER CASE", expect: "409 Conflict",
          notes: `"mathematics teacher" after "Mathematics Teacher". The service folds\n    case even though the index would not — it is the stricter of the two.`, body: null },
        { id: "07", name: "THE SAME TITLE IN ANOTHER DEPARTMENT", expect: "201 Created",
          notes: `Accepted. Uniqueness is scoped to the department, not the school.`, body: null },
        { id: "08", name: "A RETIRED POSITION KEEPS ITS TITLE", expect: "409 Conflict",
          notes: `Set active:false on a position in Mongo (#14 is not built), then send its\n    title again. OUT: POSITION_TITLE_TAKEN — the index does not filter\n    on active, so neither does the check.`, body: null },
        { id: "09", name: "A RETIRED DEPARTMENT", expect: "409 Conflict",
          notes: `Set active:false on the department, then create a position in it.\n    OUT: { "code": "DEPARTMENT_NOT_ACTIVE" } — not NOT_FOUND. It exists;\n    it is closed.`, body: null },
        { id: "10", name: "AN UNKNOWN DEPARTMENT", expect: "404 Not Found",
          notes: `OUT: { "code": "DEPARTMENT_NOT_FOUND" }\n    Another school's REAL department id is the same 404.`, body: null },
        { id: "11", name: "A REPORTING LINE", expect: "201 Created",
          notes: `Put a real positionDocsId in reportsToPositionDocsId.`,
          body: `{
  "title": "Junior Maths Teacher",
  "departmentDocsId": "{{departmentDocsId}}",
  "reportsToPositionDocsId": "{{positionDocsId}}"
}` },
        { id: "12", name: "REPORTING ACROSS DEPARTMENTS", expect: "201 Created",
          notes: `A Finance position reporting to an Academics one. ACCEPTED ON PURPOSE:\n    the org tree and the reporting line answer different questions, and a\n    single Head of Safeguarding everyone reports to is a real structure.`, body: null },
        { id: "13", name: "AN UNKNOWN REPORTING LINE", expect: "404 Not Found",
          notes: `OUT: { "code": "POSITION_NOT_FOUND" }. Another school's real position\n    id is the same 404.`, body: null },
        { id: "14", name: "A DEPARTMENT WITH NO TEACHING POSITION", expect: "201 Created",
          notes: `Create a non-teaching position in a fresh department.\n    OUT: 201 WITH A WARNING — legitimate for Finance, and also exactly\n    what an empty teacher picker looks like. Add one teaching position and\n    later non-teaching positions stop warning.`, body: null },
        { id: "15", name: "AFTER EVERY YEAR HAS ENDED", expect: "201 Created",
          notes: `STILL WORKS: gate 4 does not run here. An org chart outlives them.`, body: null },
      ],
    },
    {
      id: "update-position",
      name: "Update Position",
      method: "PATCH",
      path: "/schools/current/positions/{id}",
      status: 'live',
      summary: "Retitle a position, move its headcount, change its line, retire it.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/positions/{id}\` — endpoint #14.

### Absent means "leave it alone", and an empty body is refused

A request that sends nothing at all is a \`400 NOTHING_TO_UPDATE\` rather than a no-op \`200\`.
Sending **only** \`departmentDocsId\` is the same 400 — it is not on the request record, so it is
ignored on the way in and the request is still empty.

### Never departmentDocsId — a position cannot move department

Editing it in place would **rewrite where every past holder worked**, and every employment record
under the position would silently change department too. A position in another unit is a new position.

It is also what keeps \`school_department_title_uniq\` meaningful: a title is unique *within* a
unit, and a position that could move would carry its title across that boundary.

The same call #10 makes about a department's parent, for a different reason: **a department's
parent is structure, a position's department is history.**

### This is the endpoint that can write a reporting cycle

#13 needs no cycle walk — a brand-new position has nothing reporting to it. This one can move an
existing position under its own subordinate, which is exactly the case the module plan's open item 2
describes: a chain that closes on itself is a stack overflow in whatever first walks it, months
later and in a different module. \`409 POSITION_CYCLE\`.

**Reporting to itself is the one-step case** of the same walk, named separately only because the
message can be clearer.

**The walk carries a visited set**, and that is not habit either: a cycle already in the collection
— hand-written, restored from a backup, left by a future writer — would make the walk itself loop
forever. It stops and reports rather than hanging the request.

**One read per level, not one read of the collection.** A reporting chain is a handful of positions
deep, where a department tree is read whole by #12 anyway.

### The title carries the uniqueness the code used to

\`positionCode\` was removed on 2026-09-15, so \`title\` is what names a position — unique within the
department, **retired positions included**, because \`school_department_title_uniq\` does not filter on
\`active\`.

**The duplicate check skips a title that only changed case**, because that is the same position and
\`existsBy…\` cannot exclude it. \`"mathematics teacher"\` → \`"Mathematics Teacher"\` is a
correction, not a collision.

### Turning teachingPosition OFF is the interesting direction

It is how a department that had one teaching position stops having any — the same empty teacher picker
#13 warns about, arriving by a different route. So the warning is computed here on the way **out**
as well. A warning rides on a \`200\`; the position is saved.

### Two checks the plan specifies and this does NOT implement

Both are recorded here rather than quietly skipped, because both are owed the moment #16 lands:

- **\`HEADCOUNT_BELOW_FILLED\`** — lowering the approved count below the filled one is to be a
  *warning*, not a refusal.
- **\`409 POSITION_STILL_FILLED\`** — retiring a position somebody currently holds is to be refused.

**Neither can fire yet**: there is no \`EmploymentRecordRepository\` and no endpoint writes one, so
the filled count could only ever be zero. A check that can never fail is not a check — the same
call #13 made about its cycle walk.

### The department is not checked, and that asymmetry is on purpose

#13 refuses a position in a retired unit — \`409 DEPARTMENT_NOT_ACTIVE\` — because a position nobody may be
hired into, inside a unit that no longer exists, is two problems. #14 does not: a position that already
exists in a unit since retired still needs correcting, and refusing to edit it would strand it.

### The fifteen test cases are in the notes below
`,
      bodyNotes: `Every field optional. Needs X-School-Subdomain and a position id.

 ABSENT MEANS LEAVE IT ALONE. An empty body is 400 NOTHING_TO_UPDATE.

 NEVER departmentDocsId — a position cannot move department. Editing it would
 rewrite where every past holder worked. A position elsewhere is a NEW position.

 THIS IS WHERE A REPORTING CYCLE CAN BE WRITTEN, so this is where the walk is.
 409 POSITION_CYCLE. #13 needs none: a new position has nothing reporting to it.

 THE TITLE IS UNIQUE WITHIN THE DEPARTMENT, retired positions included. Its own
 title in another case is a correction, not a collision.

 TURNING teachingPosition OFF can empty a unit's teacher picker — the same
 warning #13 gives, on the way out. A warning rides on a 200.

 "" CLEARS reportsToPositionDocsId. "" on title is refused. Headcount is >= 1.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{positionDocsId}}", description: "The position's MongoDB document id, from Create Position." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: {
        title: "Senior Mathematics Teacher",
      },
      successStatus: 200,
      successNote: "The position as it now stands, with a nextStep and any warning.",
      responseFields: ["positionDocsId", "title", "departmentDocsId", "reportsToPositionDocsId", "approvedHeadcount", "teachingPosition", "active", "warning", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "No editable field was sent — including a body of only departmentDocsId." },
        { status: 400, code: "POSITION_TITLE_REQUIRED", when: "title was sent blank. A title is replaced, never removed." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 403, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended or closed. Reads still work." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "POSITION_NOT_FOUND", when: "No position with that id in this school — the position itself, or the supervisor named." },
        { status: 409, code: "POSITION_TITLE_TAKEN", when: "Another position in the same department holds that title, retired ones included." },
        { status: 409, code: "POSITION_CYCLE", when: "The proposed supervisor reports to this position, directly or up the chain. Includes reporting to itself." },
      ],
      examples: [
        { id: "01", name: "A RETITLE", expect: "200 OK",
          notes: `The ordinary case. OUT: the new title, the department untouched.`,
          body: { title: "Senior Mathematics Teacher" } },
        { id: "02", name: "NOTHING AT ALL", expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" } — not a quiet 200.`, body: {} },
        { id: "03", name: "ONLY THE DEPARTMENT", expect: "400 Bad Request",
          notes: `The same 400: departmentDocsId is not on the request record, so\n    the request is empty. The position does NOT move.`,
          body: { departmentDocsId: "{{departmentDocsId}}" } },
        { id: "04", name: "THE TITLE CANNOT BE REMOVED", expect: "400 Bad Request",
          notes: `OUT: { "code": "POSITION_TITLE_REQUIRED" }.`, body: { title: "   " } },
        { id: "05", name: "A TITLE ANOTHER POSITION HOLDS", expect: "409 Conflict",
          notes: `Create two positions in one unit, then rename one to the other.\n    OUT: { "code": "POSITION_TITLE_TAKEN" } — and the check folds\n    case, so "MATHEMATICS teacher" collides too.`,
          body: { title: "Academics Administrator" } },
        { id: "06", name: "ITS OWN TITLE IN ANOTHER CASE", expect: "200 OK",
          notes: `Allowed: that is the same position, and a case correction is not a\n    collision.`, body: { title: "mathematics teacher" } },
        { id: "07", name: "A RETIRED POSITION STILL HOLDS ITS TITLE", expect: "409 Conflict",
          notes: `Retire a position in Mongo, then rename another to its title.\n    OUT: 409 — school_department_title_uniq does not filter on active,\n    so a check that skipped retired rows would accept a write the\n    index then refuses.`, body: { title: "Retired Position" } },
        { id: "08", name: "CLEARING THE REPORTING LINE", expect: "200 OK",
          notes: `OUT: no reportsToPositionDocsId key at all — the position reports to\n    nobody.`, body: { reportsToPositionDocsId: "" } },
        { id: "09", name: "A SUPERVISOR IN ANOTHER DEPARTMENT", expect: "200 OK",
          notes: `Allowed, deliberately. A school with one Head of Safeguarding\n    every unit reports to is a real structure — the org tree and the\n    reporting line answer different questions.`,
          body: { reportsToPositionDocsId: "{{positionDocsId}}" } },
        { id: "10", name: "REPORTING TO ITSELF", expect: "409 Conflict",
          notes: `OUT: { "code": "POSITION_CYCLE" } — the one-step case of the walk.`,
          body: { reportsToPositionDocsId: "{{positionDocsId}}" } },
        { id: "11", name: "A LOOP UP THE CHAIN", expect: "409 Conflict",
          notes: `A reports to B reports to C. Make C report to A.\n    OUT: 409 POSITION_CYCLE — the walk goes up the WHOLE chain, not\n    one level. Skipping a level upwards is fine and is not a cycle.`,
          body: { reportsToPositionDocsId: "{{positionDocsId}}" } },
        { id: "12", name: "THE HEADCOUNT", expect: "200 OK / 400",
          notes: `8 raises it, 1 lowers it — nothing refuses lowering. 0 and -3 are\n    a 400: the model is @NotNull with a default of 1, so null is NOT\n    "uncapped" and never was.`, body: { approvedHeadcount: 8 } },
        { id: "13", name: "TURNING OFF THE LAST TEACHING POSITION", expect: "200 OK",
          notes: `OUT: 200 WITH a warning — the unit now has no teaching position, which\n    is what an empty teacher picker looks like. A warning is not a\n    refusal: the position is saved.`, body: { teachingPosition: false } },
        { id: "14", name: "RETIRING", expect: "200 OK",
          notes: `OUT: active false, and a nextStep saying it keeps its title —\n    records made against it still name it. Restoring is the same call\n    with true. Editing a RETIRED position still works.`, body: { active: false } },
        { id: "15", name: "ANOTHER SCHOOL'S POSITION", expect: "404 Not Found",
          notes: `A REAL position id belonging to a different school — as the position\n    itself AND as the supervisor named.\n    OUT: { "code": "POSITION_NOT_FOUND" } both ways.`,
          body: { title: "Stolen" } },
      ],
    },
    {
      id: "list-departments",
      name: "List Departments",
      method: "GET",
      path: "/schools/current/departments",
      status: 'live',
      summary: "The tree, or one flat filtered page.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/departments?tree=&active=&search=&…\` — endpoint #12.

### One endpoint, two response shapes

\`?tree=true\` returns **nested roots**; anything else returns a **page envelope**. The caller
chooses, so it always knows which it is reading — and they are two records rather than one with a
sometimes-populated \`subDepartments\`, because a field present on some responses and absent on
others
is a field every client has to guard.

### The tree cannot be paged, and asking is refused

\`400 TREE_CANNOT_BE_PAGED\`. A page boundary in a tree **cuts children off their parents** — page
2 is not part of a chart, it is a broken one that still looks like a chart. Silently dropping the
parameter would hide that.

The flat side **is** paged, which the plan originally said not to do. Nothing caps a school's
department count, and this endpoint's own title says "one flat filtered **page**".

### Built from one flat read

A query per level would be a storm for a structure that fits in memory. A school has tens of
departments.

### The orphan case — the interesting one

\`?tree=true&active=true\` excludes a **retired parent** whose children are still active. Those
children are real units the caller asked to see, so they are **lifted to the top** and marked
\`liftedToTop\`, not dropped with the parent. The response carries the count.

Dropping them was the alternative, and it would have hidden active departments. Leaving a hole
where the parent was is not a tree either.

### The builder carries a visited set

Nothing can write a cycle today — #9 cannot, because a new unit has no children — but **#10 will
be able to**, and a cycle written then is a crash in whatever first draws the chart. This is that
thing, so the guard belongs here.

### Five filters, all AND-ed

\`?active=\` · \`?search=\` (name **or** code) · \`?parentDepartmentDocsId=\` ·
\`?headStaffDocsId=\` · \`?topLevelOnly=\`

\`?topLevelOnly=\` asks \`exists\` on the parent rather than comparing to null, so a document
written before the field existed reads as top-level.

### Sorted by name, tiebroken by departmentCode

**Two units may share a name** — that is a real org chart, two "Science" units under different
parents. So \`name\` alone ties, and a tie with no tiebreaker puts one row on two pages while
another appears on none. \`departmentCode\` is unique per school and settles it.

### No gates

A suspended or closed school still reads its own org chart.

### The seventeen test cases are in the notes below
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain.

 TWO SHAPES. ?tree=true gives nested roots; anything else gives a page
 envelope. The caller picks, so it always knows which it is reading.

 THE TREE CANNOT BE PAGED — 400, not a silently dropped parameter. A page
 boundary cuts children off their parents.

 THE ORPHAN CASE IS THE INTERESTING ONE. ?tree=true&active=true drops a
 retired parent; its ACTIVE children are lifted to the top and marked
 liftedToTop rather than vanishing with it.

 FIVE FILTERS, ALL AND-ed: active, search (name OR code),
 parentDepartmentDocsId, headStaffDocsId, topLevelOnly.

 SORTED BY name, TIEBROKEN BY departmentCode — two units may share a name,
 so name alone would put one row on two pages.`,
      requiredFields: [],
      pathParams: [],
      queryParams: [
        { key: "tree", value: "", enabled: false, description: "true returns nested roots instead of a page. Changes the response SHAPE." },
        { key: "active", value: "", enabled: false, description: "true for units in use, false for retired. Absent returns both — and a retired unit keeps its place in the tree." },
        { key: "search", value: "", enabled: false, description: "Matches name OR departmentCode, case-insensitive, anywhere. Regex-quoted." },
        { key: "parentDepartmentDocsId", value: "", enabled: false, description: "The units directly under one unit." },
        { key: "headStaffDocsId", value: "", enabled: false, description: "What one person runs." },
        { key: "topLevelOnly", value: "", enabled: false, description: "true for roots, false for everything nested. Asked with exists, so a missing key reads as top-level." },
        { key: "page", value: "", enabled: false, description: "0-based. Refused entirely when tree=true." },
        { key: "size", value: "", enabled: false, description: "1 to 100, default 20. Refused entirely when tree=true." },
        { key: "sort", value: "", enabled: false, description: "name | departmentCode | createdAt | updatedAt, with ,desc. Anything else is a 400." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "A page envelope, or — with tree=true — roots, totalElements, liftedToTop and depth.",
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious"],
      captures: [],
      errors: [
        { status: 400, code: "TREE_CANNOT_BE_PAGED", when: "tree=true sent together with page or size. Refused rather than ignored." },
        { status: 400, code: "INVALID_PAGE", when: "page is negative." },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "size is below 1 or above 100." },
        { status: 400, code: "INVALID_SORT_FIELD", when: "A sort field not on the allowlist. The message lists what is." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
      ],
      examples: [
        { id: "01", name: "EVERY DEPARTMENT, FLAT", expect: "200 OK",
          notes: `No parameters.\n    OUT: a page envelope, ordered by name then departmentCode.\n    A flat row has NO subDepartments key — that belongs to the tree shape.`, body: null },
        { id: "02", name: "THE TREE", expect: "200 OK",
          notes: `?tree=true\n    OUT: roots, totalElements (every unit at any depth, not the roots),\n    liftedToTop and depth. A leaf still has subDepartments: [].`, body: null },
        { id: "03", name: "A TREE CANNOT BE PAGED", expect: "400 Bad Request",
          notes: `?tree=true&page=0, and ?tree=true&size=10.\n    OUT: { "code": "TREE_CANNOT_BE_PAGED" } — refused, not ignored.`, body: null },
        { id: "04", name: "THE ORPHAN CASE", expect: "200 OK",
          notes: `Retire a PARENT whose child is still active, then ?tree=true&active=true.\n    The parent is gone; the child is at the TOP with liftedToTop: true and\n    still names the parent it belongs to. It did not vanish.`, body: null },
        { id: "05", name: "A RETIRED UNIT KEEPS ITS PLACE", expect: "200 OK",
          notes: `?tree=true with no active filter. The retired unit is still in the\n    tree, marked active:false — its children are there, and a tree with a\n    hole in the middle is not a tree.`, body: null },
        { id: "06", name: "SEARCH BY CODE", expect: "200 OK",
          notes: `?search=PHYSICS — matches departmentCode.`, body: null },
        { id: "07", name: "SEARCH BY NAME", expect: "200 OK",
          notes: `?search=scien — case-insensitive, matches anywhere in the name.`, body: null },
        { id: "08", name: "A STRAY REGEX CHARACTER", expect: "200 OK",
          notes: `?search=Sci( — an empty page, not a 500.`, body: null },
        { id: "09", name: "THE CHILDREN OF ONE UNIT", expect: "200 OK",
          notes: `?parentDepartmentDocsId={{departmentDocsId}}`, body: null },
        { id: "10", name: "WHAT ONE PERSON RUNS", expect: "200 OK",
          notes: `?headStaffDocsId= a real Staff.id.`, body: null },
        { id: "11", name: "THE ROOTS ONLY", expect: "200 OK",
          notes: `?topLevelOnly=true, then false for everything nested.\n    Asked with exists, so a unit written before the field existed reads as\n    top-level rather than disappearing.`, body: null },
        { id: "12", name: "ACTIVE OR RETIRED", expect: "200 OK",
          notes: `?active=true, then false, then leave it off for BOTH.\n    Absent is not the same as false.`, body: null },
        { id: "13", name: "FILTERS COMBINE", expect: "200 OK",
          notes: `?active=true&topLevelOnly=true — AND-ed, like every combination here.`, body: null },
        { id: "14", name: "PAGING THE FLAT LIST", expect: "200 OK",
          notes: `?page=0&size=2, then page 1, then a page past the end (empty, not\n    an error). totalElements counts every match, not the page.`, body: null },
        { id: "15", name: "THE TIEBREAKER", expect: "200 OK",
          notes: `Create two units both named "Science", then ?sort=name&size=1 through\n    the pages. Each appears exactly once, because departmentCode settles\n    the tie — a department name is NOT unique.`, body: null },
        { id: "16", name: "A SCHOOL WITH NOTHING", expect: "200 OK",
          notes: `?tree=true on an empty school: roots: [], depth: 0. Never a 404.`, body: null },
        { id: "17", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `Suspend the school, then read. No gate runs on a read.`, body: null },
      ],
    },
    {
      id: "update-department",
      name: "Update Department",
      method: "PATCH",
      path: "/schools/current/departments/{id}",
      status: 'live',
      summary: "Rename it, describe it, name its head, retire it or restore it.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/departments/{id}\` — endpoint #10.

### Absent means "leave it alone", and an empty body is refused

Every field is optional. A request that sends nothing at all is a \`400 NOTHING_TO_UPDATE\` rather
than a no-op \`200\` — a PATCH that changes nothing and answers success lets a client with a broken
form look healthy.

Sending **only** uneditable fields is the same 400: \`departmentCode\` and
\`parentDepartmentDocsId\` are not on the request record, so they are ignored on the way in and the
request is still empty.

### Never departmentCode

**Nothing joins on it, which is exactly what makes editing it dangerous.** No query would break,
and every export, filter and report naming the old code would quietly stop matching. The
\`termCode\` reasoning, unchanged.

### Never the parent — a unit cannot be moved

The plan had #10 moving one and refusing a cycle at \`409 DEPARTMENT_CYCLE\`. **Dropped
2026-09-15.** Where a unit sits is decided when it is created, under the parent whose page created
it; a move is the one edit here that changes what every *other* unit's page shows.

The consequence is worth stating: **no endpoint in this product can write a cycle into the
department chart.** #9 cannot, because a new unit has no children; #10 cannot, because it does not
accept a parent. #12's tree builder still carries its visited set — that is what makes the
sentence true of the *data* rather than of the code's current shape.

### active is here, and #11 is why that needs saying

The plan gave \`active\` its own endpoint pair — \`POST /departments/{id}/deactivate\` and
\`/reactivate\`, #11 — the shape every lifecycle flag in this project uses. Putting the field here
instead is a real departure, **and it carries #11's refusal with it**: retiring a unit that still
holds active positions is a \`409 DEPARTMENT_NOT_EMPTY\` naming how many. The rule belongs to the
transition, not to whichever endpoint performs it, and a field reaching that state without the
check would be a back door around a decision this module already made.

**Sub-departments do not block it**, and that asymmetry is deliberate: #12 already answers for a
retired parent whose children are still active by lifting them to the top and marking
\`liftedToTop\`. That state is designed for. A live position has no such answer.

**Restoring has no check and needs none** — putting a unit back cannot invalidate anything.

### What clears, and what cannot

\`\`\`
"description": ""       clears it
"headStaffDocsId": ""   clears it — the unit has no named head
"name": ""              400 DEPARTMENT_NAME_REQUIRED
any field: null         leaves it            (same as absent)
\`\`\`

### The head is checked to EXIST, not to be employed

The same rule #9 follows: during setup a school enters its org chart before its employment
records, and refusing this would force it to work backwards. Another school's real staff id is a
\`404\`, not a cross-tenant head.

### A refused write lands nothing

The head is validated before anything is saved, so a request carrying a good name and a bad head
changes neither.

### The twelve test cases are in the notes below
`,
      bodyNotes: `Every field optional. Needs X-School-Subdomain and a department id.

 ABSENT MEANS LEAVE IT ALONE. An empty body is 400 NOTHING_TO_UPDATE, not a
 no-op 200 — a PATCH that changes nothing and says success hides a broken form.

 NEVER departmentCode. Nothing joins on it, which is what makes editing it
 dangerous: no query breaks and every export naming the old code stops matching.

 NEVER the parent — a unit cannot be moved. Dropped from the plan 2026-09-15.
 Nothing in this API can write a cycle any more.

 active IS HERE, carrying #11's refusal: retiring a unit that still holds
 ACTIVE positions is 409 DEPARTMENT_NOT_EMPTY, naming how many. Sub-departments do
 NOT block it — #12 lifts them. Restoring has no check.

 "" CLEARS description and headStaffDocsId. "" on name is refused.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{departmentDocsId}}", description: "The unit's MongoDB document id, from Create Department." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: {
        name: "Teaching & Learning",
      },
      successStatus: 200,
      successNote: "The unit as it now stands, with a nextStep saying what happened.",
      responseFields: ["departmentDocsId", "departmentCode", "name", "description", "headStaffDocsId", "active", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "No editable field was sent — including a body of only departmentCode or parentDepartmentDocsId." },
        { status: 400, code: "DEPARTMENT_NAME_REQUIRED", when: "name was sent blank. A name cannot be removed, only replaced." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 403, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended or closed. Reads still work." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "DEPARTMENT_NOT_FOUND", when: "No department with that id in this school — including another school's real id." },
        { status: 404, code: "STAFF_NOT_FOUND", when: "headStaffDocsId names nobody in this school. The whole write is rejected." },
        { status: 409, code: "DEPARTMENT_NOT_EMPTY", when: "active:false while positions are still active. The message names how many." },
      ],
      examples: [
        { id: "01", name: "A RENAME", expect: "200 OK",
          notes: `The ordinary case. OUT: the new name, the code untouched.`,
          body: { name: "Teaching & Learning" } },
        { id: "02", name: "NOTHING AT ALL", expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" } — not a quiet 200.`, body: {} },
        { id: "03", name: "ONLY UNEDITABLE FIELDS", expect: "400 Bad Request",
          notes: `The same 400: neither field is on the request record, so the\n    request is empty. OUT: the code and parent are unchanged.`,
          body: { departmentCode: "NEWCODE", parentDepartmentDocsId: "6aa15d9dc3f7d0011111111" } },
        { id: "04", name: "THE NAME CANNOT BE REMOVED", expect: "400 Bad Request",
          notes: `OUT: { "code": "DEPARTMENT_NAME_REQUIRED" }.`, body: { name: "   " } },
        { id: "05", name: "CLEARING THE DESCRIPTION", expect: "200 OK",
          notes: `OUT: no description key at all — absent, not null or "".`, body: { description: "" } },
        { id: "06", name: "CLEARING THE HEAD", expect: "200 OK",
          notes: `OUT: no headStaffDocsId key. The unit has no named head.`, body: { headStaffDocsId: "" } },
        { id: "07", name: "AN UNKNOWN HEAD", expect: "404 Not Found",
          notes: `OUT: { "code": "STAFF_NOT_FOUND" }. Send a good name with it —\n    the whole write is rejected, so the name does not land either.`,
          body: { name: "Renamed", headStaffDocsId: "deadbeefdeadbeefdeadbeef" } },
        { id: "08", name: "RETIRING AN EMPTY UNIT", expect: "200 OK",
          notes: `OUT: active false, and a nextStep saying it keeps its place in\n    the tree.`, body: { active: false } },
        { id: "09", name: "RETIRING ONE THAT STILL HOLDS POSITIONS", expect: "409 Conflict",
          notes: `Create two positions in it first.\n    OUT: { "code": "DEPARTMENT_NOT_EMPTY" } and "2 positions are still\n    active" — the count is in the message because "retire the two\n    positions first" is actionable and "not empty" is not.`,
          body: { active: false } },
        { id: "10", name: "RETIRING A PARENT WHOSE CHILDREN ARE ACTIVE", expect: "200 OK",
          notes: `Allowed, and not an oversight: #12 lifts those children to the\n    top and marks liftedToTop. That state is designed for.`,
          body: { active: false } },
        { id: "11", name: "RESTORING", expect: "200 OK",
          notes: `No check at all, even with active positions under it — putting a\n    unit back cannot invalidate anything.`, body: { active: true } },
        { id: "12", name: "ANOTHER SCHOOL'S UNIT", expect: "404 Not Found",
          notes: `A REAL department id belonging to a different school.\n    OUT: { "code": "DEPARTMENT_NOT_FOUND" } — the lookup carries schoolId.`,
          body: { name: "Stolen" } },
      ],
    },
    {
      id: "get-department",
      name: "Get Department",
      method: "GET",
      path: "/schools/current/departments/{id}",
      status: 'live',
      summary: "One unit and everything it is made of.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/departments/{id}\` — endpoint #52.

### Not in the original plan

The plan has a list (#12) and a tree, and no "tell me about this one" — rendering a department's
page meant four requests. **Added 2026-09-15**, numbered on the end because these numbers are
referenced from this catalogue and the Postman collection, and none is ever reused.

### The one endpoint in this package that resolves an id to a name

Everywhere else \`parentDepartmentDocsId\` and \`headStaffDocsId\` come back **raw**, because one
place should decide how a unit and a person are presented and a write's response is not it. A
detail view **is** that place.

### The head resolves to a name and nothing else

\`{ staffDocsId, fullName }\`. A \`Staff\` document carries an address, a date of birth and a
national identity number — returning the record would leak the most sensitive data this product
holds through an endpoint nobody would think to check, and this module has **no authorization
yet**.

### A dangling reference leaves the page readable

A parent or a head deleted out from under the unit is **omitted, not a 404**. The department the
caller asked for still exists, and refusing to describe it because something it points at is gone
would make the damage worse.

### Direct sub-departments only

The whole nesting is **#12 with \`?tree=true\`**, built from one flat read. Repeating that walk
here would be a second implementation of it.

### Retired positions are included and marked

A retired position is still part of what a unit is made of — records made against it still name it.
\`positionCount\` counts them all; \`activePositionCount\` does not.

### The positions are here, and that is a boundary

This answers "what is this unit made of". **#15** — \`GET /positions\`, not built — will answer
"find positions across the school", filtered and paged. When it arrives the two return the same rows
in two shapes, which is the situation #29 of academics ended up in and had to be trimmed out of.
**If it becomes a problem, #15 wins and this trims.**

### No gates

A suspended or closed school still reads its own org chart.

### The eleven test cases are in the notes below
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain and a department id.

 NOT IN THE ORIGINAL PLAN. Added 2026-09-15; the plan had a list and a tree
 but no "tell me about this one".

 THE ONE ENDPOINT HERE THAT RESOLVES AN ID TO A NAME. Everywhere else the
 parent and the head come back raw.

 THE HEAD RESOLVES TO A NAME AND NOTHING ELSE. A Staff record carries an
 address and a date of birth, and this module has no authorization yet.

 A DANGLING PARENT OR HEAD IS OMITTED, NOT A 404. The unit still exists.

 DIRECT SUB-DEPARTMENTS ONLY — the subtree is #12 with ?tree=true.

 RETIRED POSITIONS ARE INCLUDED AND MARKED. positionCount counts them all;
 activePositionCount does not.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{departmentDocsId}}", description: "The unit's MongoDB document id, from Create Department." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "The unit, its parentDepartment and head resolved, its subDepartments, its positions, and four counts.",
      responseFields: ["departmentDocsId", "departmentCode", "name", "active", "parentDepartment", "subDepartments", "positions", "subDepartmentCount", "positionCount", "activePositionCount", "teachingPositionCount"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "DEPARTMENT_NOT_FOUND", when: "No department with that id in this school — including another school's real id." },
      ],
      examples: [
        { id: "01", name: "ONE UNIT IN FULL", expect: "200 OK",
          notes: `OUT: the unit, its parentDepartment and head RESOLVED to objects, its\n    subDepartments, every position in it, and four counts.`, body: null },
        { id: "02", name: "THE HEAD IS A NAME, NOT A RECORD", expect: "200 OK",
          notes: `OUT: headStaff is { staffDocsId, fullName } and NOTHING else — no\n    address, no date of birth, no identity number.`, body: null },
        { id: "03", name: "A TOP-LEVEL UNIT", expect: "200 OK",
          notes: `OUT: no parentDepartment key at all — absent, not null.`, body: null },
        { id: "04", name: "DIRECT SUB-DEPARTMENTS ONLY", expect: "200 OK",
          notes: `A unit two deep. OUT: only the direct subDepartments appear.\n    The subtree is #12 with ?tree=true.`, body: null },
        { id: "05", name: "A LEAF", expect: "200 OK",
          notes: `OUT: subDepartments: [] and subDepartmentCount 0 — empty, never null.`, body: null },
        { id: "06", name: "RETIRED POSITIONS ARE INCLUDED", expect: "200 OK",
          notes: `Set active:false on a position in Mongo (#14 is not built).\n    OUT: it still appears, marked. positionCount counts it;\n    activePositionCount does not.`, body: null },
        { id: "07", name: "THE TEACHING COUNT", expect: "200 OK",
          notes: `teachingPositionCount counts only positions flagged teachingPosition.\n    Zero is legitimate for Finance — and is what an empty teacher picker\n    looks like, which #13 warns about on the way in.`, body: null },
        { id: "08", name: "A DELETED HEAD", expect: "200 OK",
          notes: `Delete the head's staff row in Mongo, then read the unit.\n    STILL 200, with headStaff simply omitted. The department exists;\n    refusing to describe it would make the damage worse.`, body: null },
        { id: "09", name: "A DELETED PARENT", expect: "200 OK",
          notes: `Same, for the unit above it. STILL 200, parentDepartment omitted.`, body: null },
        { id: "10", name: "ANOTHER SCHOOL'S UNIT", expect: "404 Not Found",
          notes: `A REAL department id belonging to a different school.\n    OUT: { "code": "DEPARTMENT_NOT_FOUND" } — the lookup carries schoolId.`, body: null },
        { id: "11", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `No gate runs on a read.`, body: null },
      ],
    },
    {
      id: "list-positions",
      name: "List Positions",
      method: "GET",
      path: "/schools/current/positions",
      status: 'live',
      summary: "Every seat, with the count of who currently holds it.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/positions?departmentDocsId=&active=&teaching=&vacant=&search=&…\` — endpoint #15.

### The filled count IS the endpoint

Everything else on the row is already on #52. What #15 adds is \`filledHeadcount\`, and it is
**computed from current employment records on every call — never stored**. A counter on the
position document drifts the first time a writer forgets it, which is the same objection that
keeps a weight total off \`AcademicTerm\` and a gap warning off \`GradingScheme\`.

**One grouped count for the whole page, not one per row.** Twenty seats answered by twenty
\`countBy…\` calls is the N+1 that makes a list slower the more it returns.

### vacancies never goes below zero

A school may hire a twelfth teacher into eleven approved seats — #16 **warns** rather than
refusing, because that is a budget conversation and not a data error. So \`filledHeadcount\` can
exceed \`approvedHeadcount\`, and a negative \`vacancies\` would read as a seat owing people. The
overflow is reported as \`overFilled: true\` instead.

### ?vacant= costs more than the other three filters

Vacancy is **not a field**. It is \`filledHeadcount < approvedHeadcount\`, and the left side lives
in another collection — so it cannot go into the query that pages, and applying it *after* paging
returns **short pages**: ask for twenty and get the eleven of those twenty that were vacant, with
no way to tell that from "there are only eleven".

So \`?vacant=\` takes a different path: every matching seat is read, counted in one aggregation,
filtered, and only then paged in memory. The plan's "counted for the page, not the collection"
holds for every call **except** this one — a trade made deliberately rather than a line quietly
crossed. \`?vacant=true\` is the query a school runs at the start of a hiring round, and a hiring
round is not a hot path.

**\`?vacant=false\` is "not vacant", not "full"** — it includes an over-filled seat, because that
state exists and has to fall on one side.

### filledHeadcount cannot be sorted on

It is not a field. Sorting by it would mean counting the whole collection before choosing a page.
\`?sort=\` accepts \`title\`, \`approvedHeadcount\`, \`createdAt\`, \`updatedAt\` and refuses the
rest — an allowlist, because an arbitrary field name reaching a Mongo sort is how a caller sorts
on something unindexed and makes the database read every row.

**No gate runs on a read.** A suspended school still reads its own org chart.`,
      requiredFields: [],
      pathParams: [],
      queryParams: [
        { key: "departmentDocsId", value: "", enabled: false, description: "The seats of one unit. Absent returns every unit's." },
        { key: "active", value: "true", enabled: false, description: "In use, or retired. ABSENT MEANS BOTH, not false." },
        { key: "teaching", value: "true", enabled: false, description: "Teaching seats, or the rest. Absent returns both." },
        { key: "vacant", value: "true", enabled: false, description: "Approved headcount left to fill. Computed — see the notes on what it costs." },
        { key: "search", value: "", enabled: false, description: "Case-insensitive, matches anywhere in title. Quoted, so a stray bracket is an empty result and not a 500." },
        { key: "page", value: "0", enabled: false, description: "Zero-based." },
        { key: "size", value: "20", enabled: false, description: "Default 20, max 100." },
        { key: "sort", value: "title", enabled: false, description: "title, approvedHeadcount, createdAt, updatedAt. NOT filledHeadcount — it is computed." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "One page of seats, each with filledHeadcount, vacancies and overFilled computed from current employment records.",
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "first", "last"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 400, code: "INVALID_SORT_FIELD", when: "?sort= named a field that is not on the allowlist — filledHeadcount included, because it is computed rather than stored." },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "?size= is above 100 or below 1." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
      ],
      examples: [
        { id: "01", name: "TWO OF THREE FILLED", expect: "200 OK",
          notes: `A seat with approvedHeadcount 3 and two people employed into it.\n    OUT: filledHeadcount 2, vacancies 1, overFilled false.`, body: null },
        { id: "02", name: "A SEAT NOBODY HOLDS", expect: "200 OK",
          notes: `OUT: filledHeadcount 0 — PRESENT, not omitted. The aggregation cannot\n    produce a bucket for a seat nobody holds, so the service fills the\n    zero rather than making every client write ?? 0.`, body: null },
        { id: "03", name: "OVER-FILLED", expect: "200 OK",
          notes: `approvedHeadcount 1 with two people in it — reachable, because #16\n    WARNS rather than refusing.\n    OUT: filledHeadcount 2, vacancies 0 (floored, never negative),\n    overFilled true.`, body: null },
        { id: "04", name: "EXACTLY FULL IS NOT OVER-FILLED", expect: "200 OK",
          notes: `approvedHeadcount 1, one person. OUT: vacancies 0, overFilled FALSE.\n    The boundary > and >= disagree on, and the only place they do.`, body: null },
        { id: "05", name: "ONLY CURRENT RECORDS COUNT", expect: "200 OK",
          notes: `Employ somebody, then employ them into a different seat — which closes\n    the first record. OUT: the seat they LEFT drops back to 0 and the one\n    they moved into goes to 1.`, body: null },
        { id: "06", name: "?vacant=true", expect: "200 OK",
          notes: `OUT: only seats with room. A full seat is absent, and so is an\n    over-filled one.`, body: null },
        { id: "07", name: "?vacant=false IS NOT 'FULL'", expect: "200 OK",
          notes: `OUT: the full seat AND the over-filled one. "Not vacant" covers both;\n    that state exists because #16 warns rather than refusing.`, body: null },
        { id: "08", name: "A VACANT PAGE IS FULL, NOT SHORT", expect: "200 OK",
          notes: `?vacant=true&size=2 with more than two vacant seats.\n    OUT: exactly 2 rows, and totalElements counts the VACANT set rather\n    than the whole collection. Filtering after paging is the bug this\n    guards — it returns short pages indistinguishable from a small result.`, body: null },
        { id: "09", name: "A PAGE PAST THE END", expect: "200 OK",
          notes: `?vacant=true&page=99. OUT: content [] — empty, not a 500 from a\n    sublist past the end.`, body: null },
        { id: "10", name: "A SEAT WITH NO teachingPosition KEY", expect: "200 OK",
          notes: `$unset the field in Mongo. OUT: it answers to ?teaching=false and is\n    absent from ?teaching=true. Asked with ne(true) rather than is(false),\n    because is(false) would drop it from BOTH — a row that disappears no\n    matter what you filter by.`, body: null },
        { id: "11", name: "AN UNBALANCED BRACKET", expect: "200 OK",
          notes: `?search=Teacher( — a valid search string and an INVALID regex.\n    OUT: an empty result. Unquoted it is a 500 from\n    PatternSyntaxException.`, body: null },
        { id: "12", name: "SORTING ON filledHeadcount", expect: "400 Bad Request",
          notes: `OUT: { "code": "INVALID_SORT_FIELD" }. It is computed after the page\n    is chosen, so sorting by it would mean counting the whole collection.`, body: null },
        { id: "13", name: "ANOTHER SCHOOL'S SEATS", expect: "200 OK",
          notes: `OUT: not on our page, and ours not on theirs. The count is scoped too —\n    an employment record of another school naming OUR seat id does not\n    inflate our number.`, body: null },
        { id: "14", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `No gate runs on a read, and the counts are intact.`, body: null },
      ],
    },
    {
      id: "get-position",
      name: "Get Position",
      method: "GET",
      path: "/schools/current/positions/{id}",
      status: 'live',
      summary: "One seat and who is currently in it.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/positions/{id}\` — endpoint #53.

### Not in the original plan, the way #52 was not

#15 answers "3 of 5 filled" and a school immediately asks **which three**. Answering that from the
endpoints the plan numbered means #7, which has no employment filter yet — so the same call #52
made for a department is made here for a seat. **Added 2026-09-16**, numbered on the end.

### The count and the list come from ONE read

\`filledHeadcount\` is \`holders.length\`, not a second count query. Two reads of the same
collection a moment apart can disagree — somebody is hired between them — and a page showing
"3 filled" above two names is a bug report nobody can reproduce.

#15 counts **without** listing, because a page of twenty seats should not fetch every holder of
every one. This lists, so it counts by listing.

### Current holders only — a seat is not a history

A closed record names somebody who *used to* hold this. One person's history is #19, and it is
addressed by the person because that is who a history belongs to.

### A holder whose person is missing is MARKED, never dropped

An employment record naming a \`staffDocsId\` that does not resolve comes back with its name
fields absent and a \`note\`. Dropping it would make the seat look less filled than it is, and the
count and the list would disagree — the one thing a page like this must not do.

### The person, and what they do here — and nothing else

The same boundary #52 draws for a department head. A staff document carries a date of birth, two
addresses and an emergency contact; a page asking "who is in this seat" needs a name, a number to
look them up by, and the terms of the posting. **There is no authorization yet**, and the module
plan calls that the open item that matters most here.

**No gate runs on a read.**`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{positionDocsId}}", description: "The seat's MongoDB document id, from Create Position or List Positions." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "The seat, its unit and reporting line resolved to names, its counts, and everybody currently in it — longest-serving first.",
      responseFields: ["positionDocsId", "title", "departmentDocsId", "departmentName", "reportsToPositionTitle", "approvedHeadcount", "filledHeadcount", "vacancies", "overFilled", "teachingPosition", "active", "holders", "holdersNote", "note"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "POSITION_NOT_FOUND", when: "No position with that id in this school — including another school's real id." },
      ],
      examples: [
        { id: "01", name: "A SEAT WITH TWO PEOPLE IN IT", expect: "200 OK",
          notes: `OUT: holders is two rows, LONGEST-SERVING FIRST (effectiveFrom\n    ascending) — the opposite of #19's order, and deliberately: a history\n    is read newest-first because the current row is the interesting one,\n    a roster oldest-first because seniority is what distinguishes rows.`, body: null },
        { id: "02", name: "THE COUNT EQUALS THE LIST", expect: "200 OK",
          notes: `filledHeadcount === holders.length, ALWAYS. It is the list's size, not\n    a second query — two reads a moment apart can disagree.`, body: null },
        { id: "03", name: "A HOLDER CARRIES BOTH IDS", expect: "200 OK",
          notes: `staffDocsId addresses the PERSON (#2, #8); employmentDocsId addresses\n    the POSTING (#18). A row with only one makes the wrong edit\n    reachable from this page.`, body: null },
        { id: "04", name: "AND NOTHING SENSITIVE", expect: "200 OK",
          notes: `OUT: no dateOfBirth, no address, no emergency contact. The boundary\n    #52 draws for a department head, for the same reason: there is no\n    authorization on this endpoint yet.`, body: null },
        { id: "05", name: "A SEAT NOBODY HOLDS", expect: "200 OK",
          notes: `OUT: holders [], filledHeadcount 0, and a holdersNote saying that is a\n    REAL state — an empty list with no explanation reads as something\n    having gone wrong. A filled seat carries no such note.`, body: null },
        { id: "06", name: "CURRENT HOLDERS ONLY", expect: "200 OK",
          notes: `Employ somebody, then employ them elsewhere. OUT: the seat they left\n    has no holders; the one they moved into has them.`, body: null },
        { id: "07", name: "A RETIRED SEAT STILL SHOWS ITS PEOPLE", expect: "200 OK",
          notes: `#14 does not check the filled count before retiring —\n    POSITION_STILL_FILLED is owed — so a retired seat with people in it is\n    reachable, and this page is how somebody notices.`, body: null },
        { id: "08", name: "A HOLDER WHOSE PERSON WAS DELETED", expect: "200 OK",
          notes: `Delete the staff row in Mongo. OUT: the posting is STILL returned,\n    with no fullName and a note saying the reference is broken. Dropping\n    it would make the count and the list disagree.`, body: null },
        { id: "09", name: "A RECORD NAMING ANOTHER SCHOOL'S PERSON", expect: "200 OK",
          notes: `OUT: counted as a posting, but their NAME is not returned — the staff\n    lookup carries schoolId, so a cross-tenant id reads as a broken\n    reference rather than as that person.`, body: null },
        { id: "10", name: "THE UNIT AND LINE ARE RESOLVED", expect: "200 OK",
          notes: `OUT: departmentName and reportsToPositionTitle, so the page needs no\n    second call. A seat reporting to nobody carries NEITHER field —\n    absent, not null.`, body: null },
        { id: "11", name: "AN UNKNOWN SEAT", expect: "404 Not Found",
          notes: `OUT: { "code": "POSITION_NOT_FOUND" }.`, body: null },
        { id: "12", name: "ANOTHER SCHOOL'S SEAT", expect: "404 Not Found",
          notes: `A REAL position id belonging to a different school.\n    OUT: { "code": "POSITION_NOT_FOUND" } — the lookup carries schoolId.`, body: null },
        { id: "13", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `No gate runs on a read.`, body: null },
      ],
    },
  ],
};

const GROUP_PEOPLE_STAFF = {
  id: "people-staff",
  module: "People / Staff",
  endpoints: [
    {
      id: "create-staff",
      name: "Create Staff",
      method: "POST",
      path: "/schools/current/staff",
      status: 'live',
      summary: "Create a person. employeeNo generated, never sent.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/staff\` — endpoint #1.

### The write five other modules are waiting for

Payroll, leave, reviews, development and every teacher picker need a \`staffDocsId\`, and **nothing
else in this product produces one.** Together with #9 and #13 this is the whole of the people
module's phase 1.

### employeeNo is generated, never accepted

\`NumberSequenceService.next(schoolId, EMPLOYEE_NUMBER, "EMP/{YYYY}/{MM}/")\` — the same allocator
school creation and subscriptions use. **Atomic**, so two simultaneous creates cannot be handed the
same number, and **per school**, so two schools both hold \`EMP/2026/09/000001\` without colliding.

A caller-supplied number would let two conventions collide inside one tenant, and nobody should
pick their own staff number. Sending one is **ignored, not refused** — the ordinary shape for a
field the request record does not declare.

The counter is created when the school is provisioned, with a padding width of 6 and no template;
the first caller's template is written onto it, so every later number in that school's life reads
the same shape.

**Which means changing this line does not restyle a school already numbering**, and must not — a
number somebody has written down does not change under them. A school that started on an older
template keeps it until its stored template is cleared.

### Five required fields, and the plan said one

The module plan says \`fullName\` is the only one: *"a school entering two hundred people at the
start of term has a name and nothing else on day one."* Two separate decisions overruled it.

**\`dateOfBirth\` and \`gender\`, because the model requires them** — 2026-09-15, the same way
\`approvedHeadcount\` followed the model over the plan at #13. Nothing validates a document on
save, so the plan's version would have stored rows violating their own declared constraints,
invisible until something read them expecting a date.

**\`phoneNumber\` and \`emailAddress\`, because the school asked for them** — 2026-09-15. The
model leaves both optional and this endpoint does not: a staff record with no way to contact the
person is one the office has to chase later, and *"we will fill it in afterwards"* is what leaves
765 rows without a phone number.

**Two things follow, and neither is obvious.** Both fields are unique per school, so a duplicate
is now reachable on **every** create rather than only when somebody happened to supply one. And
\`?hasEmail=false\` on #7 — *"who are we missing contact details for"* — can no longer match
anybody created here; it still matches the rows written before this rule, which is exactly who
that query is for.

**\`"---"\` is refused**, with \`400 STAFF_PHONE_REQUIRED\`. It passes the blank check and then
normalises away to nothing, which would be a blank number wearing a valid-looking request.

### This creates a person, not an employee

**No \`active\`, no status, no department, no designation, no joining date** — there are none on the
document. Every field on \`Staff\` is a fact about a *human being*; everything about the job lives
on \`EmploymentRecord\`, which #16 writes.

Somebody created here and not yet hired is **a real state**, not a half-finished one — it is a
person the school has entered but not employed, and it is the state this endpoint leaves them in.

### Closed sets, not free strings

\`nationalityCode\` is a **\`CountryCode\`** and \`preferredLanguage\` a **\`SchoolLocale\`** — the
same enums \`School.countryCode\` and \`School.defaultLocale\` already use, so a school and its
staff cannot disagree about what a country is. The address's \`countryCode\` is the same enum.

**They were free strings until 2026-09-15**, which accepted \`"12"\` as a nationality and
\`"adads"\` as a language — four stored rows had to be cleaned up when they changed. An unknown
value is now a \`400\` naming it, on **both** boundaries: Jackson reads the body through the
enum's own factory, and a converter does the same for \`?nationalityCode=\`, which otherwise
matched the constant name exactly and refused \`in\` while the body accepted it.

**An empty string is not a country.** Omit the key instead — \`"countryCode": ""\` is a 400.

### Normalisation

\`\`\`
emailAddress   trimmed and lower-cased      "  Anita@X.COM " -> "anita@x.com"
phoneNumber    spacing characters stripped  "+91 98765-43210" -> "+919876543210"
country codes  case-insensitive in          "in" -> IN            (the enum does this)
locales        matched on the tag           "en-IN" -> EN_IN
\`\`\`

**No country code is invented.** "Normalised to international format" would mean guessing \`+91\`
for a bare \`9876543210\` from the school's country — that needs a dialling-code table this project
does not have, and a wrong guess writes a number that looks right and cannot be called. A number
given without a \`+\` is stored as given.

**The email is trimmed in the request record itself**, which is the one piece of normalisation that
does not live in the service. \`@Email\` runs on the constructed record, so without it a paste from
a spreadsheet is refused as malformed — and a paste from a spreadsheet is exactly how a school
enters two hundred people.

### A phone number and an email address each identify one person

Both are refused as duplicates within a school — \`409 STAFF_PHONE_TAKEN\` and
\`409 STAFF_EMAIL_TAKEN\`. **The module plan said the opposite about email** ("two staff genuinely
may share a family address"); that was overruled on 2026-09-15.

**Checked AFTER normalising, or the check is a lie.** \`"+91 98765-43210"\` collides with
\`"+919876543210"\`, and \`"Anita@X.com"\` with \`"anita@x.com"\`. Comparing the raw strings would
pass both, store both, and leave a duplicate the index would have refused.

**School-scoped.** Another school may hold the same number and the same address.

### Both indexes are PARTIAL, and that is the whole design

\`school_staff_phone_uniq\` and \`school_staff_email_uniq\` carry
\`partialFilterExpression: { field: { $type: "string" } }\` — the same shape \`UserAccount\` uses.

Neither field is required, and **a plain unique index treats every missing value as null**: one
school could then hold exactly ONE person with no phone, and the second would be rejected by the
database with no explanation. Measured 2026-09-15: **74 schools already hold more than one person
with no phone.** That is the \`school_year_class_code_uniq\` defect this project already shipped
once, and it cost a 659-document migration.

**The service check is the enforcement, not the index.** Indexes are built on demand
(\`app.mongo.sync-indexes\`), so a database that has never synced carries no constraint at all —
where they are built, the check turns a duplicate-key 500 into a 409 naming the field.

### An empty address is no address

An address or emergency contact whose every field is blank is stored as **absent**, not as an
object of nulls. They are the same fact, and a reader should not have to tell them apart. A
*partly* filled one is kept — a city and nothing else is real.

### The fourteen test cases are in the notes below
`,
      bodyNotes: `Needs X-School-Subdomain. fullName, dateOfBirth and gender are required.

 THE WRITE FIVE OTHER MODULES ARE WAITING FOR. Payroll, leave, reviews,
 development and every teacher picker need the staffDocsId this hands back.

 employeeNo IS GENERATED, NEVER SENT. Atomic and per school, so two schools
 both hold EMP/2026/09/000001 and no two people share one. Sending it is IGNORED.

 THREE REQUIRED FIELDS, AND THE PLAN SAID ONE. Staff.java declares dateOfBirth
 and gender @NotNull; the model won, the way approvedHeadcount did at #13.

 THIS CREATES A PERSON, NOT AN EMPLOYEE. No status, no department, no joining
 date — none are on the document. #16 writes the job.

 NO COUNTRY CODE IS INVENTED. A number without a + is stored as given.

 A PHONE AND AN EMAIL EACH IDENTIFY ONE PERSON, school-scoped, checked AFTER
 normalising — "+91 98765-43210" collides with "+919876543210". Both indexes
 are PARTIAL, so any number of people may have neither.`,
      requiredFields: ["fullName", "dateOfBirth", "gender", "phoneNumber", "emailAddress"],
      pathParams: [],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: {
        fullName: "Anita Sharma",
        dateOfBirth: "1990-08-14",
        gender: "FEMALE",
        phoneNumber: "+919876543210",
        emailAddress: "anita.sharma@example.com",
      },
      successStatus: 201,
      successNote: "The person, led by the employeeNo the school writes down.",
      responseFields: ["employeeNo", "staffDocsId", "fullName", "dateOfBirth", "gender", "phoneNumber", "emailAddress", "currentAddress", "permanentAddress", "emergencyContact", "nextStep"],
      captures: [
        { variable: "staffDocsId", from: "staffDocsId", note: "What #16, leave, payroll and every review store." },
        { variable: "employeeNo", from: "employeeNo", note: "What the school writes down." },
      ],
      errors: [
        { status: 400, code: "VALIDATION_FAILED", when: "fullName, phoneNumber or emailAddress blank; dateOfBirth absent or in the future; gender absent or not one of MALE / FEMALE / OTHER; a malformed email." },
        { status: 400, code: "STAFF_PHONE_REQUIRED", when: "The phone has no digits — '---' passes the blank check then normalises to nothing." },
        { status: 400, code: "INVALID_VALUE", when: "nationalityCode is not an ISO 3166-1 alpha-2 code, or preferredLanguage is not a locale this product supports. The message names the value." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 403, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended or closed." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 409, code: "SUBSCRIPTION_NOT_USABLE", when: "Gate 2 — the school is not paying." },
        { status: 409, code: "STAFF_PHONE_TAKEN", when: "Somebody in this school already holds that phone number, compared after normalising." },
        { status: 409, code: "STAFF_EMAIL_TAKEN", when: "Somebody in this school already holds that email address, compared after lower-casing." },
      ],
      examples: [
        { id: "01", name: "A NAME, A BIRTHDAY AND A GENDER", expect: "201 Created",
          notes: `The ordinary case, and the smallest legal body.\n    OUT: employeeNo EMP/{year}/{month}/000001, and a nextStep saying this\n    person is not employed yet.`,
          body: { fullName: "Anita Sharma", dateOfBirth: "1990-08-14", gender: "FEMALE" } },
        { id: "02", name: "THE NUMBER INCREMENTS", expect: "201 Created",
          notes: `Send it twice. OUT: 000001 then 000002 — allocated atomically, so\n    two simultaneous creates cannot share one.`,
          body: { fullName: "Bhavna Rao", dateOfBirth: "1988-02-02", gender: "FEMALE" } },
        { id: "03", name: "A SECOND SCHOOL STARTS AT 1 AGAIN", expect: "201 Created",
          notes: `Same body, a different X-School-Subdomain.\n    OUT: EMP/{year}/000001 again — the sequence is per school, which is\n    the whole reason a caller may not supply one.`,
          body: { fullName: "Anita Sharma", dateOfBirth: "1990-08-14", gender: "FEMALE" } },
        { id: "04", name: "SENDING AN employeeNo", expect: "201 Created",
          notes: `IGNORED, not refused — it is not on the request record.\n    OUT: the generated number, not the one sent.`,
          body: { fullName: "Chetan Iyer", dateOfBirth: "1985-05-05", gender: "MALE", employeeNo: "EMP/1900/000999" } },
        { id: "05", name: "A NAME ALONE", expect: "400 Bad Request",
          notes: `What the PLAN said should work. OUT: fieldErrors naming dateOfBirth,\n    gender, phoneNumber AND emailAddress — four fields the plan called\n    optional, overruled by the model and by the school.`,
          body: { fullName: "No Birthday" } },
        { id: "05b", name: "A PHONE WITH NO DIGITS", expect: "400 Bad Request",
          notes: `"---" passes @NotBlank and then normalises to nothing.\n    OUT: { "code": "STAFF_PHONE_REQUIRED" } rather than a blank number\n    stored behind a request that looked valid.`,
          body: { fullName: "No Digits", dateOfBirth: "1990-01-01", gender: "MALE", phoneNumber: "---", emailAddress: "nodigits@example.com" } },
        { id: "06", name: "BORN TOMORROW", expect: "400 Bad Request",
          notes: `OUT: fieldErrors naming dateOfBirth. @Past — a person born in the\n    future is a typo, and one stored is a payroll problem later.`,
          body: { fullName: "Time Traveller", dateOfBirth: "2999-01-01", gender: "OTHER" } },
        { id: "07", name: "A GENDER OUTSIDE THE ENUM", expect: "400 Bad Request",
          notes: `MALE, FEMALE, OTHER — the shared Gender, not a people-specific one.`,
          body: { fullName: "Bad Gender", dateOfBirth: "1990-01-01", gender: "ROBOT" } },
        { id: "08", name: "JOB FIELDS IN THE BODY", expect: "201 Created",
          notes: `IGNORED, every one — none is on the document. A person is not\n    employed by existing. OUT: no status, no department, no joining date.`,
          body: { fullName: "Not An Employee", dateOfBirth: "1990-01-01", gender: "MALE", active: false, status: "ACTIVE", departmentDocsId: "x", joiningDate: "2020-01-01" } },
        { id: "09", name: "AN EMAIL PASTED WITH WHITESPACE", expect: "201 Created",
          notes: `OUT: "anita.sharma@example.com" — trimmed in the request record so\n    @Email judges the value that would be stored, then lower-cased.`,
          body: { fullName: "Pasted Email", dateOfBirth: "1990-01-01", gender: "FEMALE", emailAddress: "  Anita.Sharma@Example.COM  " } },
        { id: "10", name: "A DUPLICATE EMAIL", expect: "409 Conflict",
          notes: `Send 09 twice. OUT: { "code": "STAFF_EMAIL_TAKEN" }. Send it in a\n    DIFFERENT CASE and it still collides — the check runs after\n    lower-casing. Another SCHOOL may hold the same address.`,
          body: { fullName: "Same Address", dateOfBirth: "1992-01-01", gender: "MALE", emailAddress: "ANITA.Sharma@Example.COM" } },
        { id: "10b", name: "A DUPLICATE PHONE", expect: "409 Conflict",
          notes: `OUT: { "code": "STAFF_PHONE_TAKEN" }. Retype the same number with\n    different spacing and it still collides — the check runs after\n    stripping. Many people with NO phone are always fine: the index is\n    partial, which is what stops one school holding exactly one of them.`,
          body: { fullName: "Same Phone", dateOfBirth: "1992-01-01", gender: "MALE", phoneNumber: "+91 98765 43210" } },
        { id: "11", name: "PHONE NUMBERS", expect: "201 Created",
          notes: `"+91 98765-43210" -> "+919876543210". "(022) 2345.6789" ->\n    "02223456789". A bare "9876543210" is stored AS GIVEN — no country\n    code is invented, because a wrong guess looks right and cannot be\n    called.`,
          body: { fullName: "Phone Person", dateOfBirth: "1990-01-01", gender: "MALE", phoneNumber: "+91 98765-43210" } },
        { id: "12", name: "AN ALL-BLANK ADDRESS", expect: "201 Created",
          notes: `OUT: no currentAddress key at all — an empty address is no address,\n    not an object of six nulls.`,
          body: { fullName: "Blank Address", dateOfBirth: "1990-01-01", gender: "MALE", currentAddress: { city: "   ", countryCode: "" } } },
        { id: "13", name: "A PARTLY FILLED ADDRESS", expect: "201 Created",
          notes: `KEPT — a city and nothing else is real, and holding the profile back\n    until somebody chases a postal code helps nobody.`,
          body: { fullName: "Part Address", dateOfBirth: "1990-01-01", gender: "FEMALE", currentAddress: { city: "Pune", countryCode: "in" } } },
        { id: "14", name: "EVERYTHING AT ONCE", expect: "201 Created",
          notes: `Both addresses and the emergency contact. OUT: the contact's phone\n    normalised the same way the person's own is.`,
          body: { fullName: "Full Profile", dateOfBirth: "1990-08-14", gender: "FEMALE", nationalityCode: "in", preferredLanguage: "en-IN", phoneNumber: "+91 98765 43210", emailAddress: "full@example.com", currentAddress: { addressLine1: "12 Park Road", city: "Pune", postalCode: "411001", countryCode: "IN" }, permanentAddress: { addressLine1: "Village Road", city: "Nashik", countryCode: "IN" }, emergencyContact: { fullName: "Rakesh Sharma", relationship: "Spouse", phoneNumber: "+91 98765 11111" } } },
      ],
    },
    {
      id: "list-staff",
      name: "List Staff",
      method: "GET",
      path: "/schools/current/staff",
      status: 'live',
      summary: "One page of the school's people, searched and filtered.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/staff?search=&gender=&hasEmail=&…\` — endpoint #7.

### The row is deliberately thin, and that is the security decision here

Name, employee number, gender, phone, email. **No date of birth, no address, no emergency
contact** — those are #8, one call away.

A list endpoint returning them puts every employee's personal data into the network tab of every
dropdown that reads it, and **this module has no authorization yet**: "only the staff screen calls
it" is not a control, it is a hope.

**The phone and email are here on purpose**, and that is a line rather than an inconsistency. A
staff list is a contact list — the office reading it is looking for somebody to ring. A date of
birth is never what a picker needs.

### The sort allowlist is part of that, not paperwork

\`fullName\` · \`employeeNo\` · \`createdAt\` · \`updatedAt\`, and nothing else. A sort field taken
straight from the query string orders by **anything on the document** — including the fields the
row deliberately withholds, which leaks their values through the ordering. \`?sort=dateOfBirth\`
is a \`400\`.

### The four filters a teacher picker actually wants are NOT here yet

\`?employed=\` · \`?departmentDocsId=\` · \`?positionDocsId=\` · \`?employmentType=\`

**Every one of them lives on \`EmploymentRecord\`**, which #16 writes — and as of 2026-09-15 there
is no repository, no endpoint and no \`employment_records\` collection in the database at all.

Accepting them now would be a parameter that silently matches nothing, which is worse than one
documented as absent: a filter that looks like it works and returns an empty page reads as "there
are no teachers here". They are **ignored rather than refused**, like every field not on a request
record.

When #16 lands, the domain plan's open item 3 has already settled how they arrive: **page on
\`employment_records\`, then read \`staff\` by id** — because every narrowing filter lives on the
first collection, and paging the second gives pages that shrink after filtering.

### What IS here

\`?search=\` matches **\`fullName\` OR \`employeeNo\`**, case-insensitive, anywhere, regex-quoted —
an office looks somebody up by name and a payroll run looks them up by number, and which one the
caller has is not this endpoint's to decide. A stray \`(\` is an empty page, not a \`500\`.

\`?gender=\` · \`?nationalityCode=\` (case-insensitive, matching how #1 stores it) ·
\`?hasEmail=\` · \`?hasPhone=\`.

**\`?hasEmail=false\` is the interesting one** — *"who are we missing contact details for"* is a
real question a school asks at the start of term, and this is the only way to ask it. Asked with
\`exists\`, not a null comparison, because #1 stores an absent email as **no key at all**.

### Sorted by name, tiebroken by employeeNo

**Two people genuinely share a name** — the most ordinary thing in a school roll. So \`fullName\`
alone ties, and a tie with no tiebreaker puts one row on two pages while another appears on none.
\`employeeNo\` is unique per school by index and settles it.

### No gates

A suspended or closed school still reads its own staff list.

### The test cases are in the notes below
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain.

 THE ROW IS THIN, AND THAT IS THE SECURITY DECISION. Name, number, gender,
 phone, email — no date of birth, no address, no emergency contact. Those are
 #8. This module has no authorization yet.

 THE SORT ALLOWLIST IS PART OF THAT: a pass-through would order by fields the
 row withholds, which leaks them. ?sort=dateOfBirth is a 400.

 THE FOUR FILTERS A PICKER WANTS ARE NOT HERE YET — ?employed=,
 ?departmentDocsId=, ?positionDocsId=, ?employmentType= all live on
 EmploymentRecord, which #16 writes and which does not exist. IGNORED, not
 refused. Accepting them would be a filter that silently matches nothing.

 ?search= matches fullName OR employeeNo, regex-quoted. ?hasEmail=false is
 "who are we missing contact details for", asked with exists.

 SORTED BY name, TIEBROKEN BY employeeNo — two people share a name.`,
      requiredFields: [],
      pathParams: [],
      queryParams: [
        { key: "search", value: "", enabled: false, description: "Matches fullName OR employeeNo, case-insensitive, anywhere. Regex-quoted." },
        { key: "gender", value: "", enabled: false, description: "MALE | FEMALE | OTHER. Anything else is a 400." },
        { key: "nationalityCode", value: "", enabled: false, description: "ISO 3166-1 alpha-2, case-insensitive." },
        { key: "hasEmail", value: "", enabled: false, description: "false finds who is MISSING one. Absent returns both." },
        { key: "hasPhone", value: "", enabled: false, description: "The same, for a phone number." },
        { key: "page", value: "", enabled: false, description: "0-based. Negative is a 400." },
        { key: "size", value: "", enabled: false, description: "1 to 100, default 20. 0 and 101 are refused, never clamped." },
        { key: "sort", value: "", enabled: false, description: "fullName | employeeNo | createdAt | updatedAt, with ,desc. Anything else is a 400." },
      ],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "A page envelope. The counts describe every match, not this page.",
      responseFields: ["content", "page", "size", "totalElements", "totalPages", "hasNext", "hasPrevious"],
      captures: [],
      errors: [
        { status: 400, code: "INVALID_PAGE", when: "page is negative." },
        { status: 400, code: "INVALID_PAGE_SIZE", when: "size is below 1 or above 100." },
        { status: 400, code: "INVALID_SORT_FIELD", when: "A sort field not on the allowlist — including one the row deliberately withholds." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
      ],
      examples: [
        { id: "01", name: "EVERYBODY", expect: "200 OK",
          notes: `No parameters. OUT: a page envelope ordered by name then\n    employeeNo. NO dateOfBirth, address or emergency contact on any row.`, body: null },
        { id: "02", name: "SEARCH BY NAME", expect: "200 OK",
          notes: `?search=anita — case-insensitive, matches anywhere.`, body: null },
        { id: "03", name: "SEARCH BY EMPLOYEE NUMBER", expect: "200 OK",
          notes: `?search=000001 — the same box. Payroll looks people up that way.`, body: null },
        { id: "04", name: "A STRAY REGEX CHARACTER", expect: "200 OK",
          notes: `?search=Sharma( — an empty page, not a 500.`, body: null },
        { id: "05", name: "WHO IS MISSING AN EMAIL", expect: "200 OK",
          notes: `?hasEmail=false — the question a school actually asks at the start\n    of term. Asked with exists, because #1 stores an absent email as no\n    key at all. Absent returns BOTH.`, body: null },
        { id: "06", name: "GENDER", expect: "200 OK / 400",
          notes: `?gender=MALE narrows. ?gender=ROBOT is a 400, not an empty page.`, body: null },
        { id: "07", name: "NATIONALITY IS CASE-INSENSITIVE", expect: "200 OK",
          notes: `?nationalityCode=in matches IN — #1 upper-cases on the way in, and\n    a caller should not have to know that.`, body: null },
        { id: "08", name: "FILTERS COMBINE", expect: "200 OK",
          notes: `?hasEmail=true&hasPhone=true — AND-ed, as is ?search= with any\n    filter.`, body: null },
        { id: "09", name: "AN EMPLOYMENT FILTER", expect: "200 OK",
          notes: `?employed=true&departmentDocsId=x — IGNORED, not refused, and it\n    narrows NOTHING. That is why it is not accepted as a real filter:\n    one that looks like it works and returns an empty page reads as\n    "there are no teachers here". It arrives with #16.`, body: null },
        { id: "10", name: "PAGING", expect: "200 OK",
          notes: `?page=0&size=2, then page 1, then a page past the end (empty, not\n    an error). totalElements counts every match, not the page.`, body: null },
        { id: "11", name: "PAGE AND SIZE ARE REFUSED, NEVER CLAMPED", expect: "400 Bad Request",
          notes: `?page=-1, ?size=0 and ?size=101 are each a 400. ?size=100 is fine.`, body: null },
        { id: "12", name: "THE TIEBREAKER", expect: "200 OK",
          notes: `Create two people both called "Anita Sharma", then walk the list\n    one row at a time with ?size=1. Everybody appears exactly once —\n    fullName alone would tie, and a tie puts one row on two pages.`, body: null },
        { id: "13", name: "A HIDDEN FIELD CANNOT BE SORTED ON", expect: "400 Bad Request",
          notes: `?sort=dateOfBirth and ?sort=phoneNumber are each a 400. A\n    pass-through allowlist would let a caller order by a field the row\n    withholds, which leaks it.`, body: null },
        { id: "14", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `No gate runs on a read — it still sees its own people. Writing one\n    is still refused.`, body: null },
      ],
    },
    {
      id: "update-staff",
      name: "Update Staff",
      method: "PATCH",
      path: "/schools/current/staff/{id}",
      status: 'live',
      summary: "Correct anything on a person — name, contact, addresses, photo.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/staff/{id}\` — endpoint #2.

### It edits the whole profile, which absorbs #3, #4 and #5

The plan split it across four endpoints: this one for the scalar fields, \`PUT /addresses\` (#3),
\`PUT /emergency-contact\` (#4) and \`PUT /photo\` (#5). **Asked for as one on 2026-09-15**, the same
call that folded #11 into #10.

**The reasoning behind the split is kept, not discarded.** #3 existed because *"same as current"*
is a real answer and two independent PATCHes leave a window where the pair disagree; #4 because a
contact with a new name beside an old number is worse than no contact, since somebody will trust
it in the one situation where it matters. So:

### An address and the emergency contact are REPLACED WHOLE, never merged

Send the object and it **becomes** the object. Send the fields you want, not the fields that
changed — anything you leave out is gone, not kept.

Merging would reintroduce exactly the half-updated address those separate endpoints were designed
to prevent, which is the one thing this endpoint has to get right.

### What clears, and what cannot

\`\`\`
"currentAddress": {}      clears it     — an empty object is no address
"emergencyContact": {}    clears it
"profileImageDocsId": ""  clears it
"fullName": ""            400 STAFF_NAME_REQUIRED
"phoneNumber": ""         400 — required since 2026-09-15
"emailAddress": ""        400 — required since 2026-09-15
any field: null           leaves it     (same as absent)
\`\`\`

**\`nationalityCode\` and \`preferredLanguage\` can be corrected but not removed**, and that is a
limitation rather than a decision. They are enums, so \`""\` is not a value they take, and \`null\`
already means "leave it alone" — with no way to tell an absent field from an explicit null, there
is nothing left to mean "clear". Making them clearable needs \`JsonNullable\`, which this project
does not depend on.

### Never employeeNo, and nothing about the job

It is generated and **printed on things** — an identity card, a payslip, a register signed at the
gate. A rename leaves a paper trail pointing at nobody, and unlike a department code there is not
even a second key to find them by.

There is no status, department or joining date on this document to edit. That is
\`EmploymentRecord\`, and #16 writes it.

### The phone and email stay unique, and their own value is not a collision

\`409 STAFF_PHONE_TAKEN\` / \`409 STAFF_EMAIL_TAKEN\`. Re-sending somebody the number they already
have is a \`200\` — the check is skipped when the normalised value already belongs to them, the
same shape #14 uses for a position's title. Both are compared **after** normalising.

### It answers with #8's shape

An edit and a read are one thing to a caller, employment folded in and all.

### The test cases are in the notes below
`,
      bodyNotes: `Every field optional. Needs X-School-Subdomain and a staff id.

 IT EDITS THE WHOLE PROFILE — #3 (addresses), #4 (emergency contact) and #5
 (photo) are folded in. Asked for as one endpoint 2026-09-15.

 AN ADDRESS AND THE EMERGENCY CONTACT REPLACE WHOLE, NEVER MERGE. Send the
 object and it becomes the object; what you leave out is GONE. That is the
 reasoning those separate endpoints existed for, kept.

 {} CLEARS an address or the contact. "" clears the photo. "" on the name,
 phone or email is REFUSED — all three are required.

 nationalityCode and preferredLanguage are correctable, NOT clearable: they
 are enums, "" is not one of their values, and null already means "leave it".

 NEVER employeeNo. Nothing about the job either — that is #16.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{staffDocsId}}", description: "The person's document id, from Create Staff." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: { fullName: "Anita Verma" },
      successStatus: 200,
      successNote: "The person in full, in #8's shape — an edit and a read are one thing.",
      responseFields: ["staffDocsId", "employeeNo", "fullName", "dateOfBirth", "gender", "phoneNumber", "emailAddress", "currentAddress", "permanentAddress", "emergencyContact", "employment", "note"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "No editable field was sent — including a body of only employeeNo." },
        { status: 400, code: "STAFF_NAME_REQUIRED", when: "fullName sent blank. A name is replaced, never removed." },
        { status: 400, code: "STAFF_PHONE_REQUIRED", when: "The phone was blank or had no digits. It cannot be cleared." },
        { status: 400, code: "STAFF_EMAIL_REQUIRED", when: "The email was blank. It cannot be cleared." },
        { status: 400, code: "INVALID_VALUE", when: "nationalityCode is not a country, or preferredLanguage is not a supported locale." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 403, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended or closed. Reads still work." },
        { status: 404, code: "STAFF_NOT_FOUND", when: "No staff member with that id in this school." },
        { status: 409, code: "STAFF_PHONE_TAKEN", when: "Somebody ELSE in this school holds that number. Their own is fine." },
        { status: 409, code: "STAFF_EMAIL_TAKEN", when: "Somebody ELSE in this school holds that address." },
      ],
      examples: [
        { id: "01", name: "A RENAME", expect: "200 OK",
          notes: `The ordinary case. OUT: the whole person in #8's shape, employeeNo\n    untouched.`, body: { fullName: "Anita Verma" } },
        { id: "02", name: "NOTHING AT ALL", expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" } — not a quiet 200. A body of\n    only employeeNo is the same 400.`, body: {} },
        { id: "03", name: "THE NAME CANNOT BE REMOVED", expect: "400 Bad Request",
          notes: `OUT: { "code": "STAFF_NAME_REQUIRED" }.`, body: { fullName: "   " } },
        { id: "04", name: "AN ADDRESS REPLACES WHOLE", expect: "200 OK",
          notes: `Set a full address first, then send only a city.\n    OUT: the city and NOTHING ELSE — addressLine1 and countryCode are\n    gone, not kept. That is the point: half a changed address is a\n    delivery to the wrong place.`,
          body: { currentAddress: { city: "Pune" } } },
        { id: "05", name: "AN EMPTY OBJECT CLEARS IT", expect: "200 OK",
          notes: `OUT: no currentAddress key at all.`, body: { currentAddress: {} } },
        { id: "06", name: "THE EMERGENCY CONTACT, WHOLE", expect: "200 OK",
          notes: `Same rule. A new name beside an old number is worse than no\n    contact, so it is never merged.`,
          body: { emergencyContact: { fullName: "Sunita", relationship: "Sister", phoneNumber: "+91 99999 00002" } } },
        { id: "07", name: "THE PHOTO", expect: "200 OK",
          notes: `Which the plan gave to #5. "" removes it.`, body: { profileImageDocsId: "67aa15d9dc3f7d0012345678" } },
        { id: "08", name: "THEIR OWN NUMBER IS NOT A DUPLICATE", expect: "200 OK",
          notes: `Send back the number they already have. 200, not 409 — the check\n    skips the value that already belongs to them.`,
          body: { phoneNumber: "+919876543210" } },
        { id: "09", name: "SOMEBODY ELSE'S NUMBER", expect: "409 Conflict",
          notes: `OUT: { "code": "STAFF_PHONE_TAKEN" }. Send a good name with it —\n    the whole write is rejected, so the name does not land either.`,
          body: { fullName: "Should Not Land", phoneNumber: "+911111111111" } },
        { id: "10", name: "A NATIONALITY THAT IS NOT A COUNTRY", expect: "400 Bad Request",
          notes: `OUT: INVALID_VALUE naming the value. It is a CountryCode enum.`,
          body: { nationalityCode: "12" } },
        { id: "11", name: "employeeNo IS IGNORED", expect: "200 OK",
          notes: `Not on the request record, so ignored rather than refused — and\n    the number does not move.`, body: { employeeNo: "EMP/1900/000001", fullName: "Anita Sharma" } },
        { id: "12", name: "ANOTHER SCHOOL'S PERSON", expect: "404 Not Found",
          notes: `A REAL staff id belonging to a different school.`, body: { fullName: "Stolen" } },
      ],
    },
    {
      id: "get-staff",
      name: "Get Staff",
      method: "GET",
      path: "/schools/current/staff/{id}",
      status: 'live',
      summary: "One person in full, with their employment folded in.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/staff/{id}\` — endpoint #8.

### The fullest thing this product returns about a human being

A date of birth, two addresses, an emergency contact. **#7's row carries none of it precisely so
that this one can**: a list is read by every dropdown, and this is read by one page.

**Which is why the module plan calls authorization the open item that matters most here**, and why
there is none. Every field below is readable by anybody who can reach the API with a school
subdomain, and \`note\` says so on every response rather than leaving it to a README nobody opens.

### The employment block is absent, and says why

The plan folds the person's \`current = true\` employment record in here, because *"who is this and
what do they do"* is one question and every caller would make the second call anyway.

**#16 writes that record and is not built** — there is no \`employment_records\` collection at all.
So the key is **absent**, not \`null\` and not \`{}\`: three ways of saying "nothing here" is three
cases a client has to handle. \`employmentNote\` explains it, because an absence like that is the
kind of thing a caller otherwise reads as a bug in their own code.

**That shape is not scaffolding.** A person with no employment record stays a real state once #16
exists — somebody the school has entered and not yet hired, which is what #1 leaves them in. This
is what that person will always look like.

### Scoped by schoolId, never by id alone

Another school's real id is a real id. On this endpoint an unscoped lookup does not leak a name —
it leaks **a date of birth, a home address and an emergency contact**. The refusal says nothing
about the person either.

### No gates

A suspended or closed school still reads its own people.

### The test cases are in the notes below
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain and a staff id.

 THE FULLEST THING THIS PRODUCT RETURNS ABOUT A PERSON. Date of birth, two
 addresses, an emergency contact — #7's row hides all of it so this can show
 it. Nothing checks who is asking. That is the module's biggest open item.

 THE EMPLOYMENT BLOCK IS ABSENT, NOT NULL OR {}. #16 writes the record and is
 not built; the collection does not exist. employmentNote says so.

 A PERSON WITH NO EMPLOYMENT RECORD IS A REAL STATE, not a broken row — it is
 what #1 leaves them in, and stays true once #16 exists.

 SCOPED BY schoolId. Another school's real id is a 404 that says nothing about
 the person.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{staffDocsId}}", description: "The person's MongoDB document id, from Create Staff." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "The person in full, with an employmentNote where the job will go.",
      responseFields: ["staffDocsId", "employeeNo", "fullName", "dateOfBirth", "gender", "phoneNumber", "emailAddress", "currentAddress", "permanentAddress", "emergencyContact", "employmentNote", "note"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "STAFF_NOT_FOUND", when: "No staff member with that id in this school — including another school's real id." },
      ],
      examples: [
        { id: "01", name: "ONE PERSON IN FULL", expect: "200 OK",
          notes: `OUT: everything #1 stored — date of birth, both addresses, the\n    emergency contact — plus an employmentNote where the job will go.`, body: null },
        { id: "02", name: "WHAT #7 HIDES AND #8 SHOWS", expect: "200 OK",
          notes: `Read the same person from GET /staff and compare. The LIST row has\n    no dateOfBirth, no address and no emergency contact; this has all\n    three. The two are meant to disagree.`, body: null },
        { id: "03", name: "SOMEBODY WITH ONLY THE REQUIRED FIELDS", expect: "200 OK",
          notes: `OUT: no phoneNumber, emailAddress, address or contact keys AT ALL —\n    absent, never null. fullName, dateOfBirth and gender are always there.`, body: null },
        { id: "04", name: "THE EMPLOYMENT BLOCK", expect: "200 OK",
          notes: `There is no employment key, and employmentNote says why: #16 writes\n    it and is not built. Not null, not {} — absent.`, body: null },
        { id: "05", name: "ANOTHER SCHOOL'S PERSON", expect: "404 Not Found",
          notes: `A REAL staff id belonging to a different school.\n    OUT: { "code": "STAFF_NOT_FOUND" } — and the message says nothing\n    about them. An unscoped lookup here leaks a home address.`, body: null },
        { id: "06", name: "A PADDED ID", expect: "200 OK",
          notes: `Leading and trailing spaces are trimmed before the lookup.`, body: null },
        { id: "07", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `No gate runs on a read — it still sees its own people, in full.\n    Creating one is still refused.`, body: null },
      ],
    },
    {
      id: "update-employment",
      name: "Update Employment",
      method: "PATCH",
      path: "/schools/current/employment/{id}",
      status: 'live',
      summary: "Correct a date or a manager on a record already written.",
      schoolSurface: true,
      docs: `**PATCH** \`/schools/current/employment/{id}\` — endpoint #18.

### A correction, not an event

That difference decides everything that is not here. A promotion, a transfer and a resignation are
**events** — they close one record and open another, or close one and set a terminal status. They
are #16 and #17. This is for what was **typed wrong** on a record that already describes the right
thing.

### Addressed by the RECORD's id, not the person's

Somebody has several, and the URL has to say which. It is also the only place the tenant can be
checked on this path — which is why the lookup is scoped by \`schoolId\` and another school's real
record id is a \`404\`.

### Never current, never positionDocsId

**\`current\`** — #16 and #17 move two records together. A PATCH that could set this is exactly how
two records end up current, **or none do** — and the second is worse, because the person then
reads as unemployed. The partial unique index catches the first; nothing catches the second.

**\`positionDocsId\`** — moving somebody to another position is a **transfer**, which is a new
record, which is #16. Editing it in place would rewrite where they worked last year. The same
objection that keeps a position from changing department at #14.

**\`staffDocsId\`** either: a record belongs to the person it was written for.

### What it edits

\`effectiveFrom\` · \`effectiveUntil\` · \`managerDocsId\` · \`probationUntil\` · \`status\` ·
**\`employmentType\`**

The last one is **not in the plan's list, added 2026-09-15**: full-time typed where part-time was
meant is a typo like any other, and no event endpoint owns it — leaving it out would mean the only
fix was deleting the record, which this module has no way to do.

\`"managerDocsId": ""\` clears it. **\`effectiveUntil\` and \`probationUntil\` cannot be cleared**,
for the reason #2's enums cannot: \`null\` already means "leave it alone".

### A current record cannot be ended or terminated here

\`EmploymentRecord\` says \`effectiveUntil\` is *"null while this employment record remains
current"*, and \`status: TERMINATED\` on a current record is the contradiction the module plan's
open item 2 warns about. Both are refused — **ending an employment is #17**, which sets \`current\`
and a terminal status **together**, and that pairing is the whole point of it.

A **closed** record can be given either, because correcting a past record is what this endpoint is
for.

### Moving a date is the dangerous edit, and no index covers it

\`school_staff_employment_start_uniq\` forbids two records **starting** on the same day. **Nothing
in the database compares a start to the previous record's end** — so moving a date is how somebody
comes to hold two overlapping jobs that #19 would print as a contradiction.

So this reads the person's whole history and checks the neighbours itself:
\`409 EMPLOYMENT_ALREADY_STARTS_THEN\` and \`409 EMPLOYMENT_OVERLAPS_PREVIOUS\`, the second naming
the record it would collide with.

**A gap is allowed.** Being unemployed for a while between two jobs is real; an overlap is not.

### The dates are judged as the record would END UP

All three are resolved before any check runs, so a change to one is judged against the two already
stored rather than half-applied.

### The test cases are in the notes below
`,
      bodyNotes: `Every field optional. Needs X-School-Subdomain and an EMPLOYMENT record id
 — not a staff id. Somebody has several records; the URL says which.

 A CORRECTION, NOT AN EVENT. A promotion or transfer is #16, a resignation is
 #17; both move two things at once. This fixes what was typed wrong.

 NEVER current — a PATCH that could set it is how two records end up current,
 or NONE do, and none is worse: the person reads as unemployed.

 NEVER positionDocsId — a transfer is a NEW record. Editing it in place would
 rewrite where they worked last year.

 A CURRENT RECORD CANNOT BE ENDED OR TERMINATED HERE. Both are #17, which sets
 current and a terminal status together. A CLOSED record can have either.

 MOVING A DATE IS CHECKED AGAINST THE NEIGHBOURS, which no index does: nothing
 in the database compares a start to the previous record's end. A gap is
 allowed; an overlap is not.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{employmentDocsId}}", description: "The EMPLOYMENT record's id, from Employ Staff — not the staff id." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: { managerDocsId: "{{staffDocsId}}" },
      successStatus: 200,
      successNote: "The record as it now stands.",
      responseFields: ["employmentDocsId", "staffDocsId", "positionDocsId", "managerDocsId", "status", "employmentType", "effectiveFrom", "effectiveUntil", "probationUntil", "current"],
      captures: [],
      errors: [
        { status: 400, code: "NOTHING_TO_UPDATE", when: "No editable field was sent — including a body of only current or positionDocsId." },
        { status: 400, code: "EMPLOYMENT_STATUS_TERMINAL", when: "status TERMINATED on a record that is still current. That is #17." },
        { status: 400, code: "EMPLOYMENT_CURRENT_CANNOT_END", when: "effectiveUntil on a record that is still current. That is #17." },
        { status: 400, code: "EMPLOYMENT_ENDS_BEFORE_IT_STARTS", when: "The resolved dates would run backwards." },
        { status: 400, code: "PROBATION_BEFORE_START", when: "Probation would end before the employment begins." },
        { status: 400, code: "MANAGER_IS_SELF", when: "managerDocsId is the person the record belongs to." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 403, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended or closed." },
        { status: 404, code: "EMPLOYMENT_NOT_FOUND", when: "No record with that id in this school — including another school's real id." },
        { status: 404, code: "MANAGER_NOT_FOUND", when: "managerDocsId names nobody in this school." },
        { status: 409, code: "EMPLOYMENT_ALREADY_STARTS_THEN", when: "Another record for this person already begins on that day." },
        { status: 409, code: "EMPLOYMENT_OVERLAPS_PREVIOUS", when: "The new start falls on or before an earlier record's end. The message names it." },
      ],
      examples: [
        { id: "01", name: "A MANAGER", expect: "200 OK",
          notes: `The ordinary case. "" clears it — they report to nobody.`,
          body: { managerDocsId: "{{staffDocsId}}" } },
        { id: "02", name: "A STATUS AND A CONTRACT TYPE", expect: "200 OK",
          notes: `employmentType is not in the plan's list — added because a typo\n    there has no other fix. The record stays CURRENT: a correction\n    moves nobody.`,
          body: { status: "PROBATION", employmentType: "PART_TIME", probationUntil: "2026-09-30" } },
        { id: "03", name: "NOTHING AT ALL", expect: "400 Bad Request",
          notes: `OUT: { "code": "NOTHING_TO_UPDATE" }. A body of only current or\n    positionDocsId is the same 400 — neither is on the request record.`, body: {} },
        { id: "04", name: "current IS IGNORED", expect: "200 OK",
          notes: `Send { "current": false } with a real field. IGNORED, not refused,\n    and the record is still current — this is how two records end up\n    current, or none do.`,
          body: { current: false, status: "ACTIVE" } },
        { id: "05", name: "positionDocsId IS IGNORED TOO", expect: "200 OK",
          notes: `A transfer is a NEW record, which is #16. The position does not\n    move.`, body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE" } },
        { id: "06", name: "TERMINATING A CURRENT RECORD", expect: "400 Bad Request",
          notes: `OUT: { "code": "EMPLOYMENT_STATUS_TERMINAL" } — current and\n    finished at once. #17 sets current and a terminal status together.\n    A CLOSED record may be marked TERMINATED here.`,
          body: { status: "TERMINATED" } },
        { id: "07", name: "ENDING A CURRENT RECORD", expect: "400 Bad Request",
          notes: `OUT: { "code": "EMPLOYMENT_CURRENT_CANNOT_END" }. Same reasoning,\n    the other field. A closed record's end IS correctable.`,
          body: { effectiveUntil: "2027-03-31" } },
        { id: "08", name: "DATES THAT RUN BACKWARDS", expect: "400 Bad Request",
          notes: `On a CLOSED record, an end before its own start.\n    OUT: EMPLOYMENT_ENDS_BEFORE_IT_STARTS. Probation before the start\n    is PROBATION_BEFORE_START.`,
          body: { effectiveUntil: "2020-01-01" } },
        { id: "09", name: "THE SAME START AS ANOTHER RECORD", expect: "409 Conflict",
          notes: `OUT: EMPLOYMENT_ALREADY_STARTS_THEN — the check in front of\n    school_staff_employment_start_uniq.`,
          body: { effectiveFrom: "2024-04-01" } },
        { id: "10", name: "MOVED BACK ONTO THE PREVIOUS RECORD", expect: "409 Conflict",
          notes: `OUT: EMPLOYMENT_OVERLAPS_PREVIOUS, naming the record it would\n    collide with. NO INDEX CHECKS THIS — nothing in the database\n    compares a start to a previous end.`,
          body: { effectiveFrom: "2024-06-01" } },
        { id: "11", name: "A GAP IS ALLOWED", expect: "200 OK",
          notes: `A start well after the previous record ends. Being unemployed for\n    a while between two jobs is real; an overlap is not.`,
          body: { effectiveFrom: "2026-06-01" } },
        { id: "12", name: "ANOTHER SCHOOL'S RECORD", expect: "404 Not Found",
          notes: `A REAL employment id belonging to a different school.\n    OUT: { "code": "EMPLOYMENT_NOT_FOUND" }.`,
          body: { status: "ON_LEAVE" } },
      ],
    },
    {
      id: "employment-status",
      name: "Change Employment Status",
      method: "POST",
      path: "/schools/current/employment/{id}/status",
      status: 'live',
      summary: "Change a status, with the reason — and end the employment if it is terminal.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/employment/{id}/status\` — endpoint 18b.

### Why this is not a field on #18

#18 corrects what was **typed wrong** on a record. A status change is not a correction — it is
something that *happened* to somebody: they went on leave, were suspended, retired.

**What a school needs six months later is the reason, not the new value.** A field on a general
PATCH cannot demand one; an endpoint can. So \`status\` came off #18 on 2026-09-16 and lives here.

### Five of the seven statuses require a reason, and the enum decides

\`\`\`
PROBATION      no reason needed   a normal start
ACTIVE         no reason needed   the ordinary state
ON_LEAVE       REQUIRED           how long, backfill, paid?
SUSPENDED      REQUIRED
NOTICE_PERIOD  REQUIRED           whose decision was it
TERMINATED     REQUIRED           and ends the employment
RETIRED        REQUIRED           and ends the employment
\`\`\`

The rule lives on \`EmploymentStatus.requiresReason()\`, not in a list in the service — **a status
added later brings its own answer** and nothing has to remember to update this. Missing it is
\`400 EMPLOYMENT_STATUS_REASON_REQUIRED\`.

**A reason is kept where it is not required, too.** "Passed probation" beside \`ACTIVE\` is worth
having, and refusing it would make a school throw away the one sentence explaining the row above.

### The set changed on 2026-09-16

**\`OFFERED\` was removed** — it meant "accepted an offer, has not started", and that is now said
with a **future \`effectiveFrom\`**. **\`RETIRED\` was added**, kept apart from \`TERMINATED\`
because a school reads them differently.

**What that owes:** the module plan defined \`?employed=true\` as *not in {OFFERED, TERMINATED}*.
It is now *not terminal* — and the not-yet-started case has no status of its own, so #7's filters
will have to read a future \`effectiveFrom\` as "not employed yet" when they are built.

### A terminal status ends the employment, in the same write

\`TERMINATED\` and \`RETIRED\` set \`current = false\` and \`effectiveUntil\` as they are applied.
**They have to** — terminal and current at once is the contradiction open item 2 describes and
nothing in the model prevents. \`effectiveUntil\` defaults to today; an end before the record began
is a \`400\`.

**Which means this absorbed #17**, \`POST /staff/{id}/separate\`. Ending an employment is one
status change among seven, and two endpoints that both close a record are two chances to close it
differently.

### Two refusals that look like fussiness and are not

**\`409 EMPLOYMENT_STATUS_UNCHANGED\`** — a \`200\` for a write that did nothing hides a client
sending the wrong id, and the reason attached to it would explain something that never happened.

**\`409 EMPLOYMENT_NOT_CURRENT\`** — a closed record's story is over. What happens next is a new
record, which is #16.

### The test cases are in the notes below
`,
      bodyNotes: `Needs X-School-Subdomain and an EMPLOYMENT record id. status is required.

 A STATUS CHANGE IS AN EVENT, NOT A CORRECTION — that is why it left #18 on
 2026-09-16. What a school needs later is the REASON, not the new value, and a
 PATCH field cannot demand one.

 FIVE OF SEVEN REQUIRE A REASON, and the ENUM decides: ON_LEAVE, SUSPENDED,
 NOTICE_PERIOD, TERMINATED, RETIRED. PROBATION and ACTIVE need none. A reason
 is KEPT on those two as well.

 OFFERED IS GONE from the enum; a future effectiveFrom says "not started yet".
 RETIRED was added.

 A TERMINAL STATUS ENDS THE EMPLOYMENT in the same write — current:false plus
 effectiveUntil, defaulting to today. It has to: terminal AND current at once
 is a contradiction nothing in the model prevents. This absorbed #17.

 409 on a no-op change, and on a record that has already closed.`,
      requiredFields: ["status"],
      pathParams: [
        { name: "id", value: "{{employmentDocsId}}", description: "The EMPLOYMENT record's id, from Employ Staff." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: { status: "ON_LEAVE", reason: "Maternity leave until March" },
      successStatus: 200,
      successNote: "The record at its new status, with the reason on it.",
      responseFields: ["employmentDocsId", "status", "statusReason", "separationReason", "effectiveUntil", "current"],
      captures: [],
      errors: [
        { status: 400, code: "EMPLOYMENT_STATUS_REASON_REQUIRED", when: "The status requires a reason and none was sent. A blank one counts as none." },
        { status: 400, code: "EMPLOYMENT_ENDS_BEFORE_IT_STARTS", when: "A terminal status with an effectiveUntil before the record began." },
        { status: 400, code: "VALIDATION_FAILED", when: "status absent, or not one of the seven — OFFERED was removed on 2026-09-16." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 403, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended or closed." },
        { status: 404, code: "EMPLOYMENT_NOT_FOUND", when: "No record with that id in this school — including another school's real id." },
        { status: 409, code: "EMPLOYMENT_STATUS_UNCHANGED", when: "It is already at that status. Refused rather than a quiet 200." },
        { status: 409, code: "EMPLOYMENT_NOT_CURRENT", when: "The record has already closed. What happens next is a new record — #16." },
      ],
      examples: [
        { id: "01", name: "ON LEAVE, WITH A REASON", expect: "200 OK",
          notes: `OUT: the status and statusReason. The record stays CURRENT — only a\n    terminal status closes one.`,
          body: { status: "ON_LEAVE", reason: "Maternity leave until March" } },
        { id: "02", name: "ON LEAVE, WITHOUT ONE", expect: "400 Bad Request",
          notes: `OUT: { "code": "EMPLOYMENT_STATUS_REASON_REQUIRED" }. A BLANK reason\n    counts as none. Try SUSPENDED, NOTICE_PERIOD, TERMINATED and\n    RETIRED the same way — all five require it.`,
          body: { status: "ON_LEAVE" } },
        { id: "03", name: "BACK TO ACTIVE, NO REASON NEEDED", expect: "200 OK",
          notes: `PROBATION and ACTIVE are somebody working, so neither needs\n    explaining.`, body: { status: "ACTIVE" } },
        { id: "04", name: "A REASON WHERE NONE IS REQUIRED", expect: "200 OK",
          notes: `KEPT, not discarded — "Passed probation" beside ACTIVE is worth\n    having. A later change with no reason clears it, because it\n    explained a state that is gone.`,
          body: { status: "ACTIVE", reason: "Passed probation" } },
        { id: "05", name: "RETIRED", expect: "200 OK",
          notes: `OUT: current false, effectiveUntil set, and the reason on\n    separationReason as well — it is what ended the employment.\n    TERMINAL STATUSES CLOSE THE RECORD, because terminal and current at\n    once is a contradiction nothing else prevents.`,
          body: { status: "RETIRED", reason: "Reached retirement age", effectiveUntil: "2026-03-31" } },
        { id: "06", name: "TERMINATED WITH NO DATE", expect: "200 OK",
          notes: `effectiveUntil defaults to TODAY. A school recording a resignation\n    a week late can send the real date instead.`,
          body: { status: "TERMINATED", reason: "Gross misconduct" } },
        { id: "07", name: "ENDING BEFORE IT BEGAN", expect: "400 Bad Request",
          notes: `OUT: EMPLOYMENT_ENDS_BEFORE_IT_STARTS, and the record is untouched.`,
          body: { status: "TERMINATED", reason: "x", effectiveUntil: "2000-01-01" } },
        { id: "08", name: "THE STATUS IT ALREADY HAS", expect: "409 Conflict",
          notes: `OUT: EMPLOYMENT_STATUS_UNCHANGED — a 200 for a write that did\n    nothing hides a client sending the wrong id.`, body: { status: "ACTIVE" } },
        { id: "09", name: "A CLOSED RECORD", expect: "409 Conflict",
          notes: `Retire somebody first, then try again.\n    OUT: EMPLOYMENT_NOT_CURRENT — its story is over, and what happens\n    next is a new record, which is #16.`, body: { status: "ACTIVE", reason: "Rehired" } },
        { id: "10", name: "OFFERED", expect: "400 Bad Request",
          notes: `Removed from the enum on 2026-09-16. "Accepted an offer, has not\n    started" is now a record with a FUTURE effectiveFrom.`,
          body: { status: "OFFERED" } },
        { id: "11", name: "ANOTHER SCHOOL'S RECORD", expect: "404 Not Found",
          notes: `A REAL employment id belonging to a different school.`,
          body: { status: "SUSPENDED", reason: "x" } },
      ],
    },
    {
      id: "employment-history",
      name: "Employment History",
      method: "GET",
      path: "/schools/current/staff/{id}/employment",
      status: 'live',
      summary: "One person's history, newest first.",
      schoolSurface: true,
      docs: `**GET** \`/schools/current/staff/{id}/employment\` — endpoint #19.

### An unknown person is a 404, not an empty list

**This is the distinction the endpoint is built on.** Without reading the staff record first, an id
that belongs to nobody would answer with \`[]\` — and *"this person was never employed"* and
*"there is no such person"* would be indistinguishable. One is a normal state; the other is a bug
in whatever built the URL.

So the person is read, scoped to the school, before the history is.

### Newest first

*"What do they do now"* is the common question and *"what did they do in 2019"* is the rare one, so
the answer to the first should not be at the bottom of the page.

**The current record is marked on the row as well**, so nothing has to be inferred from position —
and at most one can be, by \`school_staff_current_employment_uniq\`.

### An envelope, not a bare array

A top-level JSON array cannot grow a field without breaking every caller, and this one has two
worth having: \`currentlyEmployed\` and \`totalRecords\`. Both are questions a caller would
otherwise answer by scanning, and the first is the question the list is usually opened to ask.

### Not paged

Nobody has a hundred employment records. A cursor on a five-row list is machinery nobody uses —
the argument the term list eventually lost, and this one wins.

### An empty history is a real answer, and says so

Somebody entered and never employed has none — the state #1 leaves them in. The response carries a
\`note\` explaining it, because an empty list otherwise reads as something having gone wrong.

### What the history shows

Each record carries its own dates, and #16 closes the previous one the day before the next begins
— so a promotion chain reads with **no gap and no overlap**. A record closed by 18b carries its
\`separationReason\` too.

### No gates

A suspended or closed school still reads its own history.
`,
      bodyNotes: `A GET — no body. Needs X-School-Subdomain and a STAFF id.

 AN UNKNOWN PERSON IS A 404, NOT AN EMPTY LIST. That is what the endpoint is
 built on: "never employed" and "no such person" are different facts, and a
 bare [] would make them identical. The staff record is read first for it.

 NEWEST FIRST — "what do they do now" is the common question. The current
 record is marked on the row as well, so nothing is inferred from position.

 AN ENVELOPE, NOT AN ARRAY: currentlyEmployed and totalRecords are questions a
 caller would otherwise answer by scanning.

 NOT PAGED. Nobody has a hundred employment records.

 AN EMPTY HISTORY IS A REAL ANSWER and carries a note saying so.`,
      requiredFields: [],
      pathParams: [
        { name: "id", value: "{{staffDocsId}}", description: "The person's document id — not an employment record id." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
      ],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      successNote: "The whole history, newest first, with the current record marked.",
      responseFields: ["staffDocsId", "records", "totalRecords", "currentlyEmployed", "note"],
      captures: [],
      errors: [
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 404, code: "SCHOOL_NOT_FOUND", when: "No school has that subdomain." },
        { status: 404, code: "STAFF_NOT_FOUND", when: "No staff member with that id in this school. NOT an empty list — that is the point." },
      ],
      examples: [
        { id: "01", name: "A WHOLE CAREER", expect: "200 OK",
          notes: `Employ somebody, then promote them twice with #16.\n    OUT: three records, NEWEST FIRST, exactly one marked current, and\n    each closed one ending the day before the next began — no gap and\n    no overlap.`, body: null },
        { id: "02", name: "SOMEBODY NEVER EMPLOYED", expect: "200 OK",
          notes: `Create a person with #1 and read this straight away.\n    OUT: records [], totalRecords 0, currentlyEmployed false, and a\n    note saying it is a real state rather than a missing record.`, body: null },
        { id: "03", name: "AN UNKNOWN PERSON", expect: "404 Not Found",
          notes: `OUT: { "code": "STAFF_NOT_FOUND" } — NOT an empty history.\n    Compare against 02: those two answers must differ, or a caller\n    cannot tell a never-hired person from a bad id.`, body: null },
        { id: "04", name: "AFTER A RETIREMENT", expect: "200 OK",
          notes: `Retire somebody with 18b, then read this.\n    OUT: the record is still there, marked current:false, carrying its\n    separationReason — and currentlyEmployed is false.`, body: null },
        { id: "05", name: "ANOTHER SCHOOL'S PERSON", expect: "404 Not Found",
          notes: `A REAL staff id belonging to a different school. Reading ours from\n    their subdomain is a 404 as well.`, body: null },
        { id: "06", name: "A SUSPENDED SCHOOL", expect: "200 OK",
          notes: `No gate runs on a read — it still sees its own history, in full.`, body: null },
      ],
    },
    {
      id: "employ-staff",
      name: "Employ Staff",
      method: "POST",
      path: "/schools/current/staff/{id}/employment",
      status: 'live',
      summary: "Hire, promote or transfer — one write, because it is one event.",
      schoolSurface: true,
      docs: `**POST** \`/schools/current/staff/{id}/employment\` — endpoint #16.

### The write the whole product was waiting on

#9 and #13 built the position, #1 built the person, and **nothing joined them** — so nobody was
employed anywhere, #7 had no employment filters, #8 returned no employment block, and #14 owed two
checks that could only ever count zero.

### One endpoint, because it is one event

Hiring, promoting and transferring are the same write: **close the record that was current, open a
new one.** Three endpoints doing that would be three chances to leave two records current — or
none, which is worse, because the person then reads as unemployed.

The response carries **both halves**: the new record and the one it closed. \`closed\` is absent on
a first hire, which is how a caller tells a hire from a promotion without comparing dates.

### Two documents move together, and there IS a transaction

The module plan says this project configures no Mongo transaction manager and that the close
should therefore ride on the previous record's \`version\`. **That is out of date** —
\`MongoTransactionConfig\` registers a \`MongoTransactionManager\` and Atlas is a replica set, so
\`@Transactional\` does what it says. Both writes commit or neither does.

**The order is forced by the index.** \`school_staff_current_employment_uniq\` is unique and partial
on \`current: true\`, so a new current record cannot be inserted while the old one still is. Close
first, insert second, both inside the transaction — and without it, a failure between them would
leave the person with **no** current record at all.

### The end date is computed, never sent

The closed record ends **the day before** the new one starts. Two people typing two dates is how a
gap or an overlap gets in, and there is no \`effectiveUntil\` on this request at all.

### What this refuses, and what it only warns about

\`\`\`
POSITION_NOT_ACTIVE               409   a retired position
EMPLOYMENT_STATUS_TERMINAL        400   current and TERMINATED at once
EMPLOYMENT_ALREADY_STARTS_THEN    409   two records starting the same day
EMPLOYMENT_STARTS_BEFORE_CURRENT  409   the close would end a record before it began
MANAGER_IS_SELF                   400   nobody manages themselves
PROBATION_BEFORE_START            400
POSITION_FULL                     warning, on a 201
\`\`\`

**\`POSITION_FULL\` is a warning and not a refusal.** A school hiring a twelfth teacher into eleven
approved positions is recording something that has **already happened**, and refusing it stops the
system describing the truth. The same call #14 makes about lowering an approved headcount.

**\`TERMINATED\` is refused** because a record that is \`current\` and terminal at once is the
contradiction the module plan's open item 2 warns about — nothing in the model stops it, so this
endpoint does. \`OFFERED\` is allowed and is the case that matters: somebody who accepted an offer
but has not started is a real row that must not appear in a teacher picker.

### Overlap with non-current records is NOT checked

Open item 1 settles on allowing it: a part-time music teacher who also runs the choir on a separate
contract is real. \`current\` then means **the post the school considers primary**, which is what
the unique index already enforces.

### The filled headcount is counted, never stored

A stored \`filledHeadcount\` on \`Position\` drifts the first time a writer forgets it — the
objection that also keeps a weight total off \`AcademicTerm\`.

### The test cases are in the notes below
`,
      bodyNotes: `Needs X-School-Subdomain and a staff id. positionDocsId, status,
 employmentType and effectiveFrom are required.

 ONE ENDPOINT BECAUSE IT IS ONE EVENT. Hire, promote and transfer all close
 the current record and open a new one. Both halves come back.

 @Transactional, AND THE MANAGER EXISTS. The plan says it does not — that is
 out of date. Close then insert, inside one transaction; the unique partial
 index on current:true forces that order.

 THE END DATE IS COMPUTED, never sent: the day before the new one starts.
 There is no effectiveUntil on this request.

 POSITION_FULL IS A WARNING ON A 201. A twelfth teacher in eleven positions has
 already happened. TERMINATED is REFUSED — current and terminal at once is a
 contradiction nothing downstream can read. OFFERED is allowed.

 current, effectiveUntil and separationReason are IGNORED if sent.`,
      requiredFields: ["positionDocsId", "status", "employmentType", "effectiveFrom"],
      pathParams: [
        { name: "id", value: "{{staffDocsId}}", description: "The person's document id, from Create Staff." },
      ],
      queryParams: [],
      headers: [
        { key: "X-School-Subdomain", value: "{{createdSubdomain}}", enabled: true },
        { key: "Content-Type", value: "application/json", enabled: true },
      ],
      bodyAllowed: true,
      body: {
        positionDocsId: "{{positionDocsId}}",
        status: "ACTIVE",
        employmentType: "FULL_TIME",
        effectiveFrom: "2026-04-01",
      },
      successStatus: 201,
      successNote: "The new employment, and whatever it closed.",
      responseFields: ["employment", "closed", "warning", "nextStep"],
      captures: [],
      errors: [
        { status: 400, code: "EMPLOYMENT_STATUS_TERMINAL", when: "status TERMINATED — a record cannot be current and finished at once. Leaving is #17." },
        { status: 400, code: "MANAGER_IS_SELF", when: "managerDocsId is the person being employed." },
        { status: 400, code: "PROBATION_BEFORE_START", when: "probationUntil is before effectiveFrom." },
        { status: 400, code: "TENANT_NOT_RESOLVED", when: "The X-School-Subdomain header is missing or blank." },
        { status: 403, code: "SCHOOL_NOT_ACTIVE", when: "Gate 1 — the school is suspended or closed." },
        { status: 404, code: "STAFF_NOT_FOUND", when: "No staff member with that id in this school." },
        { status: 404, code: "POSITION_NOT_FOUND", when: "No position with that id in this school." },
        { status: 404, code: "MANAGER_NOT_FOUND", when: "managerDocsId names nobody in this school." },
        { status: 409, code: "POSITION_NOT_ACTIVE", when: "The position is retired — nobody can be employed into it." },
        { status: 409, code: "EMPLOYMENT_ALREADY_STARTS_THEN", when: "This person already has a record beginning on that day, closed ones included." },
        { status: 409, code: "EMPLOYMENT_STARTS_BEFORE_CURRENT", when: "The new record would start on or before the current one, so closing it would end it before it began." },
      ],
      examples: [
        { id: "01", name: "THE HIRE", expect: "201 Created",
          notes: `OUT: the new record, current:true, with NO effectiveUntil and NO\n    "closed" key — there was nothing to close. Read the person with #8\n    afterwards: the employment is now folded in.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE", employmentType: "FULL_TIME", effectiveFrom: "2026-04-01" } },
        { id: "02", name: "THE PROMOTION", expect: "201 Created",
          notes: `Send 01, then this with a later date and another position.\n    OUT: both halves — the new record current, and "closed" carrying the\n    old one with current:false and effectiveUntil the DAY BEFORE this\n    one starts. Computed, never sent.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE", employmentType: "FULL_TIME", effectiveFrom: "2027-04-01", managerDocsId: "{{staffDocsId}}" } },
        { id: "03", name: "EXACTLY ONE IS EVER CURRENT", expect: "200 OK",
          notes: `After 02, read employment_records in Mongo: two rows, exactly one\n    with current:true. The unique partial index forbids a second, which\n    is also why the close must happen before the insert.`, body: null },
        { id: "04", name: "A RECORD CANNOT BE BORN TERMINATED", expect: "400 Bad Request",
          notes: `OUT: { "code": "EMPLOYMENT_STATUS_TERMINAL" } — current and finished\n    at once is a contradiction nothing downstream can read. OFFERED is\n    allowed and IS the interesting case.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "TERMINATED", employmentType: "FULL_TIME", effectiveFrom: "2026-04-01" } },
        { id: "05", name: "A RETIRED POSITION", expect: "409 Conflict",
          notes: `Retire the position with #14 first.\n    OUT: { "code": "POSITION_NOT_ACTIVE" }.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE", employmentType: "FULL_TIME", effectiveFrom: "2026-04-01" } },
        { id: "06", name: "MANAGING YOURSELF", expect: "400 Bad Request",
          notes: `managerDocsId equal to the path id.\n    OUT: { "code": "MANAGER_IS_SELF" }. Another school's person is a 404\n    MANAGER_NOT_FOUND.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE", employmentType: "FULL_TIME", effectiveFrom: "2026-04-01", managerDocsId: "{{staffDocsId}}" } },
        { id: "07", name: "TWO RECORDS STARTING THE SAME DAY", expect: "409 Conflict",
          notes: `OUT: { "code": "EMPLOYMENT_ALREADY_STARTS_THEN" } — the check in\n    front of school_staff_employment_start_uniq. It counts CLOSED\n    records too.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE", employmentType: "FULL_TIME", effectiveFrom: "2026-04-01" } },
        { id: "08", name: "STARTING BEFORE THE CURRENT RECORD", expect: "409 Conflict",
          notes: `OUT: { "code": "EMPLOYMENT_STARTS_BEFORE_CURRENT" } — closing the\n    old one would set an end date before its own start. The current\n    record is left untouched, which the suite checks.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE", employmentType: "FULL_TIME", effectiveFrom: "2020-01-01" } },
        { id: "09", name: "PROBATION BEFORE THE START", expect: "400 Bad Request",
          notes: `OUT: { "code": "PROBATION_BEFORE_START" }.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "PROBATION", employmentType: "FULL_TIME", effectiveFrom: "2026-04-01", probationUntil: "2026-01-01" } },
        { id: "10", name: "OVERFILLING A POSITION", expect: "201 Created",
          notes: `Employ three people into a position with approvedHeadcount 2.\n    OUT: 201 WITH a warning naming both numbers. A warning is NOT a\n    refusal — it has already happened, and refusing it would stop the\n    system recording the truth. #14 raises the headcount.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE", employmentType: "FULL_TIME", effectiveFrom: "2026-04-01" } },
        { id: "11", name: "FIELDS THAT ARE IGNORED", expect: "201 Created",
          notes: `current, effectiveUntil and separationReason are not on the request\n    record. OUT: current is true anyway and there is no end date.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE", employmentType: "FULL_TIME", effectiveFrom: "2028-01-01", current: false, effectiveUntil: "2028-06-01", separationReason: "quit" } },
        { id: "12", name: "A SUSPENDED SCHOOL", expect: "403 Forbidden",
          notes: `Gate 1. OUT: { "code": "SCHOOL_NOT_ACTIVE" } — and reading the\n    person with #8 still works.`,
          body: { positionDocsId: "{{positionDocsId}}", status: "ACTIVE", employmentType: "FULL_TIME", effectiveFrom: "2026-04-01" } },
      ],
    },
  ],
};

export const API_CATALOG = [
  GROUP_CORE_ACADEMIC_YEAR,
  GROUP_CORE_SCHOOL_PROFILE,
  GROUP_CORE_SCHOOL_PLATFORM,
  GROUP_PLANS_PLAN_CATALOGUE,
  GROUP_PLANS_SUBSCRIPTIONS,
  GROUP_PLANS_SUBSCRIPTION_THE_SCHOOL_S_OWN_VIEW,
  GROUP_ACADEMICS_TERMS,
  GROUP_ACADEMICS_CLASSES,
  GROUP_ACADEMICS_GRADING,
  GROUP_ACADEMICS_TIMETABLE,
  GROUP_PEOPLE_DEPARTMENT,
  GROUP_PEOPLE_STAFF,
  GROUP_CRM_ADMISSION_CYCLES,
  GROUP_LOCAL_USER,
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
