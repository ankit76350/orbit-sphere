/**
 * Every endpoint the battleground knows about.
 *
 * This is read out of the backend, not made up:
 *   - the five live ones come from
 *     backend/src/main/java/com/orbitastra/backend/controllers/core/SchoolController.java
 *     and the DTOs next to it,
 *   - the test cases come from postman/"Orbit Sphere — API.postman_collection.json",
 *   - the planned ones come from controllers/core/README.md, which lists all 28 writes.
 *
 * Planned endpoints are marked so and shown greyed out. They are still sendable, because a
 * 404 from a path that is not built yet is a real answer — but nobody should have to guess
 * which of these exist today.
 */

/** The body the Postman collection uses for the happy path. */
const CREATE_SCHOOL_BODY = `{
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
}`;

const JSON_HEADER = { key: 'Content-Type', value: 'application/json', enabled: true };

const SCHOOL_ID_PARAM = {
  name: 'id',
  value: '{{schoolId}}',
  description: "The school's MongoDB id. Create School fills this in for you.",
};

/** The live endpoints, in the order you would actually call them. */
const CORE_SCHOOL = {
  id: 'core-school',
  module: 'Core / School',
  surface: 'Platform surface — the caller is outside the tenant.',
  endpoints: [
    {
      id: 'create-school',
      name: 'Create School',
      method: 'POST',
      path: '/platform/schools',
      status: 'live',
      phase: 1,
      summary: 'Makes the school row at PROVISIONING or TRIAL. That is all it does.',
      docs:
        'Creates the tenant and nothing else — no roles, no number sequences, no staff, no ' +
        'user account. A school at PROVISIONING exists but cannot be used yet, so run ' +
        'Complete Provisioning next.\n\n' +
        'Refuses status, encryptionKeyReference, activatedAt and suspendedAt outright. Each ' +
        'one, if a caller could set it, would hand over something the document is meant to ' +
        'defend.',
      requiredFields: [
        'schoolName',
        'accountHolderName',
        'subdomain',
        'defaultLocale',
        'defaultTimeZone',
        'countryCode',
      ],
      optionalFields: [
        'phoneNumber',
        'emailAddress',
        'addressLine',
        'city',
        'stateOrProvince',
        'postalCode',
        'trial',
      ],
      pathParams: [],
      queryParams: [],
      headers: [JSON_HEADER],
      bodyAllowed: true,
      body: CREATE_SCHOOL_BODY,
      successStatus: 201,
      successNote: 'Also sends a Location header: /platform/schools/{schoolId}',
      responseFields: ['schoolId', 'schoolName', 'subdomain', 'status', 'createdAt', 'nextStep'],
      // After a create that works, remember the new id so the other four endpoints work
      // without copying it by hand.
      captures: [
        { variable: 'schoolId', from: 'schoolId' },
        { variable: 'createdSubdomain', from: 'subdomain' },
      ],
      errors: [
        { status: 409, code: 'SUBDOMAIN_TAKEN', when: 'Another school already has that subdomain.' },
        { status: 409, code: 'SUBDOMAIN_RESERVED', when: 'Names like api, www and login are kept for the platform.' },
        { status: 409, code: 'SUBDOMAIN_INVALID', when: 'Not a valid DNS label — a leading or trailing hyphen, wrong characters, too long.' },
        { status: 409, code: 'TIME_ZONE_INVALID', when: 'Not a real IANA zone. "Asia/Pune" looks fine and does not exist.' },
        { status: 400, code: 'VALIDATION_FAILED', when: 'A required field is blank or a value is the wrong shape. Comes back with fieldErrors.' },
        { status: 400, code: 'MALFORMED_REQUEST', when: 'The body is not valid JSON.' },
      ],
      examples: [
        {
          id: '01',
          name: 'FULL PAYLOAD',
          expect: '201 Created',
          notes: 'Every field the endpoint accepts. Also sets the Location header.',
          body: CREATE_SCHOOL_BODY,
        },
        {
          id: '02',
          name: 'MINIMUM PAYLOAD',
          expect: '201 Created',
          notes: 'Only the 6 required fields. Everything left out is stored as null, not "".',
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
          id: '03',
          name: 'TRIAL TENANT',
          expect: '201 Created',
          notes: 'status comes back TRIAL instead of PROVISIONING. Those are the only two legal starting states.',
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
          id: '04',
          name: 'SUBDOMAIN IS TIDIED UP BEFORE SAVING',
          expect: '201 Created',
          notes: 'Send "  Norm_Check 123  " and get back "norm-check-123". Use the subdomain from the response afterwards.',
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
          id: '05',
          name: 'DUPLICATE SUBDOMAIN',
          expect: '409 Conflict',
          notes: 'Run case 01 first — it saves {{createdSubdomain}}. 409 not 400: the request is fine, the name is taken.',
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
          id: '06',
          name: 'RESERVED SUBDOMAIN',
          expect: '409 Conflict',
          notes: 'A school owning "login" or "api" would receive traffic meant for the platform.',
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
          id: '07',
          name: 'MALFORMED SUBDOMAIN',
          expect: '409 Conflict',
          notes: 'Leading and trailing hyphens are not a valid DNS label.',
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
          id: '08',
          name: 'UNKNOWN TIME ZONE',
          expect: '409 Conflict',
          notes: '"Asia/Pune" looks reasonable and does not exist. The zone decides which calendar date an attendance record falls on.',
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
          id: '09',
          name: 'MISSING AND INVALID FIELDS',
          expect: '400 Bad Request',
          notes: 'VALIDATION_FAILED with one entry per bad field, caught before the controller runs.',
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
          id: '10',
          name: 'MALFORMED JSON',
          expect: '400 Bad Request',
          notes: 'MALFORMED_REQUEST, and no stack trace in the body. The editor warns you the JSON is broken before you send it.',
          body: `{"schoolName": }`,
        },
      ],
    },

    {
      id: 'complete-provisioning',
      name: 'Complete Provisioning',
      method: 'POST',
      path: '/platform/schools/{id}/complete-provisioning',
      status: 'live',
      phase: 1,
      summary: 'Finishes the setup: seeds 47 number sequences and the starting roles.',
      docs:
        'Takes no body. It is a POST because it does something, not because it sends data.\n\n' +
        'Safe to run twice. It checks what is already there and fills only the gaps, so a ' +
        'school that has edited SCHOOL_ADMIN keeps its changes.',
      pathParams: [SCHOOL_ID_PARAM],
      queryParams: [],
      headers: [],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      responseFields: [
        'schoolId',
        'subdomain',
        'status',
        'numberSequencesCreated',
        'numberSequencesAlreadyPresent',
        'rolesCreated',
        'rolesAlreadyPresent',
        'roleKeys',
        'readyToActivate',
        'nextStep',
      ],
      captures: [{ variable: 'schoolId', from: 'schoolId' }],
      errors: [
        { status: 404, code: 'SCHOOL_NOT_FOUND', when: 'No school with that id.' },
        { status: 409, code: 'SCHOOL_NOT_PROVISIONABLE', when: 'The school is OFFBOARDING, CLOSED, DELETION_PENDING or DELETED.' },
      ],
      examples: [
        {
          id: '01',
          name: 'FIRST CALL on a fresh school',
          expect: '200 OK',
          notes: 'Run Create School first. Expect 47 sequences created, 3 roles created, readyToActivate true.',
          body: null,
        },
        {
          id: '02',
          name: 'SEND IT AGAIN',
          expect: '200 OK',
          notes: 'Nothing is written. The created counts go to 0 and alreadyPresent goes to 47 and 3. Still 200 — a call that created nothing because everything was there is a success.',
          body: null,
        },
        {
          id: '03',
          name: 'PARTIAL REPAIR',
          expect: '200 OK',
          notes: 'Delete a few rows in MongoDB, send again, and only the gaps are filled.',
          body: null,
        },
        {
          id: '04',
          name: 'UNKNOWN SCHOOL ID',
          expect: '404 Not Found',
          notes: 'Set the id to 6a90000000000000000000aa.',
          body: null,
        },
        {
          id: '05',
          name: 'SHUT-DOWN TENANT',
          expect: '409 Conflict',
          notes: 'Seeding a school somebody deliberately closed would quietly bring its rows back.',
          body: null,
        },
      ],
    },

    {
      id: 'activate-school',
      name: 'Activate School',
      method: 'POST',
      path: '/platform/schools/{id}/activate',
      status: 'live',
      phase: 2,
      summary: 'Takes the school live. PROVISIONING or TRIAL to ACTIVE.',
      docs:
        'Refuses any other starting status, and refuses a school whose setup is not finished — ' +
        'activating a school with no SCHOOL_ADMIN role gives you a live school nobody can log ' +
        'into.\n\n' +
        'Not safe to run twice, unlike Complete Provisioning. A second call is refused, because ' +
        'the caller thinks they changed something and they did not.',
      pathParams: [SCHOOL_ID_PARAM],
      queryParams: [],
      headers: [],
      bodyAllowed: false,
      body: null,
      successStatus: 200,
      responseFields: [
        'schoolId',
        'subdomain',
        'status',
        'activatedAt',
        'firstActivation',
        'subscriptionStatus',
        'subscriptionNote',
        'nextStep',
      ],
      captures: [{ variable: 'schoolId', from: 'schoolId' }],
      errors: [
        { status: 409, code: 'SETUP_INCOMPLETE', when: 'Complete Provisioning has not been run, or a sequence is missing.' },
        { status: 409, code: 'SCHOOL_NOT_ACTIVATABLE', when: 'The school is not PROVISIONING or TRIAL. A suspended school is reactivated, not activated.' },
        { status: 409, code: 'SUBSCRIPTION_NOT_ACTIVE', when: 'The subscription is CANCELLED or EXPIRED.' },
        { status: 404, code: 'SCHOOL_NOT_FOUND', when: 'No school with that id.' },
      ],
      examples: [
        { id: '01', name: 'HAPPY PATH', expect: '200 OK', notes: 'Create School, then Complete Provisioning, then this. firstActivation comes back true.', body: null },
        { id: '02', name: 'TOO EARLY — no roles yet', expect: '409 Conflict', notes: 'Send this without running Complete Provisioning.', body: null },
        { id: '03', name: 'TOO EARLY — a sequence is missing', expect: '409 Conflict', notes: 'The message says 46 of 47 on purpose. "Incomplete" with no number leaves you guessing.', body: null },
        { id: '04', name: 'SEND IT AGAIN — already live', expect: '409 Conflict', notes: 'SCHOOL_NOT_ACTIVATABLE.', body: null },
        { id: '05', name: 'TRIAL SCHOOL', expect: '200 OK', notes: 'Create with trial true, complete provisioning, then send this.', body: null },
        { id: '06', name: 'CANCELLED SUBSCRIPTION', expect: '409 Conflict', notes: 'Blocked for CANCELLED and EXPIRED only. PAST_DUE still passes — a school behind on payment is not shut out mid-term.', body: null },
        { id: '07', name: 'NO SUBSCRIPTION AT ALL', expect: '200 OK', notes: 'The ordinary case today. subscriptionStatus comes back NONE and the response says why it was allowed.', body: null },
        { id: '08', name: 'UNKNOWN SCHOOL ID', expect: '404 Not Found', notes: 'Set the id to 6a90000000000000000000aa.', body: null },
      ],
    },

    {
      id: 'suspend-school',
      name: 'Suspend School',
      method: 'POST',
      path: '/platform/schools/{id}/suspend',
      status: 'live',
      phase: 2,
      summary: 'Blocks a live school. ACTIVE to SUSPENDED. A reason is required.',
      docs:
        'The reason is the only field and it is required. A suspension with nothing written ' +
        'down gets switched back on by the next person who is asked about it.\n\n' +
        'Right now this is a flag, not a lock: nothing kills the school\'s live sessions or ' +
        'stops its scheduled jobs, because those services do not exist yet.',
      requiredFields: ['reason'],
      pathParams: [SCHOOL_ID_PARAM],
      queryParams: [],
      headers: [JSON_HEADER],
      bodyAllowed: true,
      body: `{
  "reason": "Non-payment. Third invoice unpaid past 60 days."
}`,
      successStatus: 200,
      responseFields: ['schoolId', 'subdomain', 'status', 'activatedAt', 'suspendedAt', 'statusReason', 'nextStep'],
      captures: [{ variable: 'schoolId', from: 'schoolId' }],
      errors: [
        { status: 409, code: 'SCHOOL_NOT_SUSPENDABLE', when: 'Only an ACTIVE school can be suspended.' },
        { status: 400, code: 'VALIDATION_FAILED', when: 'The reason is missing or blank, or longer than 500 characters.' },
        { status: 404, code: 'SCHOOL_NOT_FOUND', when: 'No school with that id.' },
      ],
      examples: [
        {
          id: '01',
          name: 'HAPPY PATH',
          expect: '200 OK',
          notes: 'The school must be ACTIVE. status comes back SUSPENDED with suspendedAt stamped now.',
          body: `{
  "reason": "Non-payment. Third invoice unpaid past 60 days."
}`,
        },
        { id: '02', name: 'MISSING REASON', expect: '400 Bad Request', notes: 'VALIDATION_FAILED with fieldErrors.reason.', body: `{}` },
        {
          id: '03',
          name: 'BLANK REASON',
          expect: '400 Bad Request',
          notes: 'Spaces do not count as a reason.',
          body: `{
  "reason": "   "
}`,
        },
        {
          id: '04',
          name: 'SCHOOL IS NOT ACTIVE',
          expect: '409 Conflict',
          notes: 'Try it on a school still at PROVISIONING, or on one already suspended.',
          body: `{
  "reason": "Trying to suspend a school that is not live."
}`,
        },
        {
          id: '05',
          name: 'UNKNOWN SCHOOL ID',
          expect: '404 Not Found',
          notes: 'Set the id to 6a90000000000000000000aa. The school is looked up before the reason is used.',
          body: `{
  "reason": "Any reason."
}`,
        },
      ],
    },

    {
      id: 'reactivate-school',
      name: 'Reactivate School',
      method: 'POST',
      path: '/platform/schools/{id}/reactivate',
      status: 'live',
      phase: 2,
      summary: 'Lets a suspended school back in. SUSPENDED to ACTIVE. The body is optional.',
      docs:
        'Does not re-stamp activatedAt and does not clear suspendedAt. Both are the school\'s ' +
        'history, not its current state, so a school that comes back still shows when it was ' +
        'last suspended and why.\n\n' +
        'The note is optional, unlike the reason on suspend. Send one to replace the stored ' +
        'reason; leave it out and the suspension reason stays. You can send no body at all.',
      optionalFields: ['note'],
      pathParams: [SCHOOL_ID_PARAM],
      queryParams: [],
      headers: [JSON_HEADER],
      bodyAllowed: true,
      body: `{
  "note": "Outstanding invoices cleared on 27 August."
}`,
      successStatus: 200,
      responseFields: ['schoolId', 'subdomain', 'status', 'activatedAt', 'suspendedAt', 'statusReason', 'nextStep'],
      captures: [{ variable: 'schoolId', from: 'schoolId' }],
      errors: [
        { status: 409, code: 'SCHOOL_NOT_REACTIVATABLE', when: 'Only a SUSPENDED school can be reactivated. One that never went live is activated instead.' },
        { status: 400, code: 'VALIDATION_FAILED', when: 'The note is longer than 500 characters.' },
        { status: 404, code: 'SCHOOL_NOT_FOUND', when: 'No school with that id.' },
      ],
      examples: [
        {
          id: '01',
          name: 'HAPPY PATH with a note',
          expect: '200 OK',
          notes: 'statusReason is replaced by the note. suspendedAt is left alone.',
          body: `{
  "note": "Outstanding invoices cleared on 27 August."
}`,
        },
        {
          id: '02',
          name: 'NO BODY AT ALL',
          expect: '200 OK',
          notes: 'Allowed on purpose. The old suspension reason stays, which is the useful default. This case clears the body box for you.',
          body: '',
        },
        {
          id: '03',
          name: 'SCHOOL IS NOT SUSPENDED',
          expect: '409 Conflict',
          notes: 'SCHOOL_NOT_REACTIVATABLE.',
          body: `{
  "note": "Trying to reactivate a school that was never suspended."
}`,
        },
        { id: '04', name: 'UNKNOWN SCHOOL ID', expect: '404 Not Found', notes: 'Set the id to 6a90000000000000000000aa.', body: `{}` },
      ],
    },
  ],
};

/**
 * The rest of the write plan from controllers/core/README.md. None of these are built. They
 * are here so the whole shape of the API is visible in one place, and so that the day one of
 * them lands it only has to be moved up into the live list.
 */
const plannedSchoolEndpoints = [
  ['offboard-school', 13, 'POST', '/platform/schools/{id}/offboard', 7, 'Starts the data export and begins winding the tenant down.', '{\n  "reason": "Contract not renewed."\n}'],
  ['close-school', 14, 'POST', '/platform/schools/{id}/close', 7, 'The tenant is no longer reachable. Needs the export to be finished.', null],
  ['request-deletion', 15, 'POST', '/platform/schools/{id}/request-deletion', 7, 'Starts the retention clock. Needs an explicit confirmation.', '{\n  "confirmSubdomain": "{{createdSubdomain}}"\n}'],
  ['cancel-deletion', 16, 'POST', '/platform/schools/{id}/cancel-deletion', 7, 'Back to CLOSED, before the clock runs out.', null],
  ['confirm-deletion', 17, 'POST', '/platform/schools/{id}/confirm-deletion', 7, 'Irreversible. Needs a second confirmation.', '{\n  "confirmSubdomain": "{{createdSubdomain}}"\n}'],
  ['patch-profile', 6, 'PATCH', '/schools/current/profile', 3, 'The school edits its own name, phone and email.', '{\n  "schoolName": "Orbit Astra International School",\n  "phoneNumber": "+919876543210",\n  "emailAddress": "office@orbit-school.edu"\n}'],
  ['put-address', 7, 'PUT', '/schools/current/address', 3, 'Replaces the whole address. A PUT because patching city without state gives a place that does not exist.', '{\n  "addressLine": "12, MG Road",\n  "city": "Pune",\n  "stateOrProvince": "Maharashtra",\n  "postalCode": "411001"\n}'],
  ['patch-localization', 8, 'PATCH', '/schools/current/localization', 3, 'Language and time zone. Changing the zone is the most dangerous edit in this package.', '{\n  "defaultLocale": "en-IN",\n  "defaultTimeZone": "Asia/Kolkata"\n}'],
  ['put-logo', 9, 'PUT', '/schools/current/logo', 3, 'Replaces the logo.', '{\n  "logoUrl": "https://cdn.example.com/logos/orbit-astra.png"\n}'],
  ['patch-subdomain', 10, 'PATCH', '/platform/schools/{id}/subdomain', 6, 'Its own endpoint because it is the key that finds the tenant. Changing it breaks every saved link.', '{\n  "currentSubdomain": "{{createdSubdomain}}",\n  "newSubdomain": "orbit-astra"\n}'],
  ['patch-account-holder', 11, 'PATCH', '/platform/schools/{id}/account-holder', 6, 'The name on the contract.', '{\n  "accountHolderName": "Ankit Kumar"\n}'],
  ['patch-encryption-key', 12, 'PATCH', '/platform/schools/{id}/encryption-key', 6, 'Platform only, and must never appear on the school surface.', '{\n  "encryptionKeyReference": "kms://orbit/tenant/new-key"\n}'],
];

const plannedYearEndpoints = [
  ['create-year', 18, 'POST', '/schools/current/academic-years', 4, 'Checks the name is unique, the dates are in order, and the range does not overlap another year.', '{\n  "name": "2026-2027",\n  "startDate": "2026-04-01",\n  "endDate": "2027-03-31",\n  "holidays": []\n}'],
  ['patch-year-dates', 19, 'PATCH', '/schools/current/academic-years/{name}/dates', 4, 'Moving a boundary after the year has started can leave data outside the year that owns it.', '{\n  "startDate": "2026-04-01",\n  "endDate": "2027-03-31"\n}'],
  ['put-holidays', 20, 'PUT', '/schools/current/academic-years/{name}/holidays', 5, 'Replaces the whole calendar. The bulk import case.', '{\n  "holidays": [\n    { "date": "2026-08-15", "name": "Independence Day", "type": "PUBLIC_HOLIDAY" }\n  ]\n}'],
  ['post-holiday', 21, 'POST', '/schools/current/academic-years/{name}/holidays', 5, 'Adds one — a bandh, an unexpected closure.', '{\n  "date": "2026-11-14",\n  "name": "Local closure",\n  "type": "SPECIAL_HOLIDAY"\n}'],
  ['patch-holiday', 22, 'PATCH', '/schools/current/academic-years/{name}/holidays/{date}', 5, 'Edits one, for example renaming Diwali to Diwali (day 2).', '{\n  "name": "Diwali (day 2)"\n}'],
  ['delete-holiday', null, 'DELETE', '/schools/current/academic-years/{name}/holidays/{date}', 5, 'Removes one, for a holiday that moved.', null],
  ['generate-weekly-off', 23, 'POST', '/schools/current/academic-years/{name}/holidays/generate-weekly-off', 5, 'Makes one dated entry per occurrence of a weekday. Needed because there is no weekly-off field anywhere.', '{\n  "dayOfWeek": "SUNDAY"\n}'],
  ['delete-weekly-off', null, 'DELETE', '/schools/current/academic-years/{name}/holidays?type=WEEKLY_OFF', 5, 'Clears the generated offs, because the first thing anyone does is pick the wrong weekday.', null],
  ['enrollment-enable', 24, 'POST', '/schools/current/academic-years/{name}/enrollment/enable', 6, 'Opens enrollment. A gate with permission attached, not a field edit.', null],
  ['enrollment-disable', 25, 'POST', '/schools/current/academic-years/{name}/enrollment/disable', 6, 'Closes enrollment.', null],
  ['results-lock', 26, 'POST', '/schools/current/academic-years/{name}/results/lock', 6, 'Locks results. Routine.', null],
  ['results-unlock', 27, 'POST', '/schools/current/academic-years/{name}/results/unlock', 6, 'Unlocking means somebody can change a mark a parent has already seen. Needs a reason and an audit event every time, including failed attempts.', '{\n  "reason": "Grade 9 maths re-evaluation approved by the principal."\n}'],
  ['clone-year', 28, 'POST', '/schools/current/academic-years/{name}/clone', 8, 'Copies last year\'s calendar. Most Indian holidays are on lunar dates and move, so this saves about three dates.', '{\n  "newName": "2027-2028",\n  "startDate": "2027-04-01",\n  "endDate": "2028-03-31"\n}'],
];

/** Turns one row of the planned tables into the same shape as a live endpoint. */
function toPlanned([id, number, method, path, phase, summary, body]) {
  const pathParams = [];
  if (path.includes('{id}')) pathParams.push(SCHOOL_ID_PARAM);
  if (path.includes('{name}')) {
    pathParams.push({
      name: 'name',
      value: '{{academicYear}}',
      description: 'The year name, such as 2026-2027. It is the join key and can never change.',
    });
  }
  if (path.includes('{date}')) {
    pathParams.push({ name: 'date', value: '2026-08-15', description: 'The holiday date, as YYYY-MM-DD.' });
  }

  const [cleanPath, queryString] = path.split('?');
  const queryParams = queryString
    ? queryString.split('&').map((pair) => {
        const [key, value] = pair.split('=');
        return { key, value, enabled: true };
      })
    : [];

  const lastSegment = cleanPath.split('/').filter(Boolean).pop() || 'root';

  return {
    id,
    name: `#${number ?? '—'} ${lastSegment.replace(/[{}]/g, '')}`,
    method,
    path: cleanPath,
    status: 'planned',
    phase,
    planNumber: number,
    summary,
    docs:
      'Not built. This is from the plan in controllers/core/README.md. Sending it now returns ' +
      '404 from Spring, which is the honest answer — the path does not exist yet.',
    pathParams,
    queryParams,
    headers: body ? [JSON_HEADER] : [],
    bodyAllowed: Boolean(body),
    body,
    successStatus: 200,
    responseFields: [],
    captures: [],
    errors: [{ status: 404, code: '—', when: 'The endpoint is not built yet, so Spring has no handler for this path.' }],
    examples: [],
  };
}

export const API_CATALOG = [
  CORE_SCHOOL,
  {
    id: 'core-school-planned',
    module: 'Core / School — planned',
    surface: 'From controllers/core/README.md. Not built.',
    endpoints: plannedSchoolEndpoints.map(toPlanned),
  },
  {
    id: 'core-year-planned',
    module: 'Core / Academic Year — planned',
    surface: 'School surface. Not built. The year name in the URL can never change.',
    endpoints: plannedYearEndpoints.map(toPlanned),
  },
];

/** Flat list, handy for searching and for finding an endpoint by id from the history. */
export const ALL_ENDPOINTS = API_CATALOG.flatMap((group) =>
  group.endpoints.map((endpoint) => ({ ...endpoint, module: group.module })),
);

export function findEndpoint(id) {
  return ALL_ENDPOINTS.find((endpoint) => endpoint.id === id) || null;
}

export const LIVE_COUNT = ALL_ENDPOINTS.filter((e) => e.status === 'live').length;
export const PLANNED_COUNT = ALL_ENDPOINTS.filter((e) => e.status === 'planned').length;
