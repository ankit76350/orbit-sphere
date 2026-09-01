/**
 * Walks the same path a person would click through the screens, using the app's own request
 * builder against a real backend.
 *
 *     node e2e.mjs            (needs the backend on 3456)
 */
import { buildCall } from './src/api/buildCall.js';
import { sendRequest } from './src/lib/httpClient.js';

const ENV = { baseUrl: 'http://localhost:3456' };
let pass = 0, fail = 0;

async function act(label, endpointId, options = {}, expected = 200) {
  const { prepared } = buildCall(endpointId, options, ENV);
  const result = await sendRequest(prepared, { timeoutMs: 30000 });
  const ok = result.status === expected;
  ok ? pass++ : fail++;
  console.log(
    `  ${ok ? 'ok  ' : 'FAIL'} ${String(result.status ?? 'ERR').padEnd(4)} ${label}` +
    (ok ? '' : `\n        expected ${expected} · ${(result.bodyText || result.error?.message || '').slice(0, 200)}`),
  );
  return result;
}

const stamp = Date.now();
console.log('\nSCREEN: Schools — the list loads');
const list = await act('Load schools (bare)', 'list-schools', { query: { page: 0, size: 20, sort: 'createdAt,desc' } });
console.log(`        ${list.bodyJson.content.length} rows of ${list.bodyJson.totalElements}, page ${list.bodyJson.page + 1}/${list.bodyJson.totalPages}`);
await act('Filter chip: Live', 'list-schools', { query: { status: ['ACTIVE'], page: 0, size: 20 } });
await act('Two filter chips at once', 'list-schools', { query: { status: ['ACTIVE', 'TRIAL'], page: 0, size: 20 } });
await act('Search box', 'list-schools', { query: { search: 'orbit', page: 0, size: 20 } });
await act('Sort: Name A–Z', 'list-schools', { query: { sort: 'name,asc', page: 0, size: 10 } });
await act('Next page', 'list-schools', { query: { page: 1, size: 10, sort: 'createdAt,desc' } });

console.log('\nSCREEN: Add a school');
const created = await act('Create the school', 'create-school', {
  body: {
    schoolName: 'Riverside Public School', accountHolderName: 'Rohan Shinde',
    subdomain: `riverside-${stamp}`, emailAddress: 'office@riverside.edu',
    phoneNumber: '+919876543210', defaultLocale: 'en-IN', defaultTimeZone: 'Asia/Kolkata',
    countryCode: 'IN', addressLine: '12, MG Road', city: 'Pune',
    stateOrProvince: 'Maharashtra', postalCode: '411001', trial: false,
  },
}, 201);
const id = created.bodyJson.schoolId;
let subdomain = created.bodyJson.subdomain;
console.log(`        created ${subdomain} (${id}); Location: ${created.headers.location}`);

await act('A name that is already taken', 'create-school', {
  body: { schoolName: 'Duplicate', accountHolderName: 'A', subdomain,
    defaultLocale: 'en-IN', defaultTimeZone: 'Asia/Kolkata', countryCode: 'IN' },
}, 409);
await act('Form with missing fields', 'create-school', {
  body: { schoolName: '', subdomain: `bad-${stamp}`, emailAddress: 'nope',
    countryCode: 'ZZZ', defaultLocale: 'en-IN', defaultTimeZone: 'Asia/Kolkata' },
}, 400);

console.log('\nSCREEN: School detail — reads the full record');
const full = await act('Load the school', 'get-school', { pathParams: { id } });
console.log(`        ${full.bodyJson.schoolName} · ${full.bodyJson.status} · created ${full.bodyJson.createdAt}`);
await act('A school id that does not exist', 'get-school', { pathParams: { id: '6a90000000000000000000aa' } }, 404);

console.log('\nSCREEN: School detail — the lifecycle buttons');
const setup = await act('“Finish setting up”', 'complete-provisioning', { pathParams: { id } });
console.log(`        ${setup.bodyJson.numberSequencesCreated} sequences, ${setup.bodyJson.rolesCreated} roles, ready=${setup.bodyJson.readyToActivate}`);
await act('“Take it live”', 'activate-school', { pathParams: { id } });
await act('“Suspend” with a reason', 'suspend-school', { pathParams: { id }, body: { reason: 'Non-payment. Third invoice unpaid past 60 days.' } });
await act('“Suspend” with an empty reason', 'suspend-school', { pathParams: { id }, body: { reason: '  ' } }, 400);
await act('“Let it back in” with a note', 'reactivate-school', { pathParams: { id }, body: { note: 'Cleared on 27 August.' } });
await act('“Let it back in” again', 'reactivate-school', { pathParams: { id } }, 409);

console.log('\nSCREEN: Settings — the tenant header is added for us');
const profile = await act('Load the profile', 'get-profile', { subdomain });
console.log(`        ${profile.bodyJson.schoolName} · ${profile.bodyJson.defaultTimeZone}`);
await act('Loading it with no school named', 'get-profile', {}, 400);
await act('Save school details', 'update-profile', { subdomain, body: { schoolName: 'Riverside Public School', accountHolderName: 'Ankit Kumar', emailAddress: 'admin@riverside.edu' } });
await act('Save with nothing filled in', 'update-profile', { subdomain, body: {} }, 400);
await act('Save with a bad email', 'update-profile', { subdomain, body: { emailAddress: 'not-an-email' } }, 400);
await act('Forgetting the school (no header)', 'update-profile', { body: { schoolName: 'x' } }, 400);
await act('Save the address', 'replace-address', { subdomain, body: { addressLine: '44, FC Road', city: 'Pune', stateOrProvince: 'Maharashtra', postalCode: '411004' } });
await act('Save language only', 'update-localization', { subdomain, body: { defaultLocale: 'en-GB' } });
const zone = await act('Change the time zone, unconfirmed', 'update-localization', { subdomain, body: { defaultTimeZone: 'Asia/Dubai' } }, 409);
console.log(`        refused with ${zone.bodyJson.code} — the confirm dialog is for this`);
await act('Change the time zone, confirmed', 'update-localization', { subdomain, body: { defaultTimeZone: 'Asia/Dubai', confirmTimeZoneChange: true } });
await act('Save the logo', 'replace-logo', { subdomain, body: { logoUrl: 'https://cdn.example.com/logos/riverside.png' } });
await act('A logo that is not https', 'replace-logo', { subdomain, body: { logoUrl: 'http://cdn.example.com/x.png' } }, 400);

console.log('\nSCREEN: Academic year');
const year = await act('Create the year', 'create-academic-year', { subdomain, body: { name: '2026-2027', startDate: '2026-04-01', endDate: '2027-03-31' } }, 201);
const name = year.bodyJson.name;
console.log(`        ${name}: ${year.bodyJson.durationDays} days, current=${year.bodyJson.current}`);
await act('A year with the same name', 'create-academic-year', { subdomain, body: { name: '2026-2027', startDate: '2026-04-01', endDate: '2027-03-31' } }, 409);
await act('A three-day “year”', 'create-academic-year', { subdomain, body: { name: 'oops', startDate: '2030-04-01', endDate: '2030-04-03' } }, 400);

await act('Open admissions', 'enable-enrollment', { subdomain, pathParams: { name } });
await act('Close admissions', 'disable-enrollment', { subdomain, pathParams: { name } });
await act('Lock the results', 'lock-results', { subdomain, pathParams: { name } });
await act('Unlock the results', 'unlock-results', { subdomain, pathParams: { name } });

console.log('\nSCREEN: Academic year — the reads behind the screen');
const yearList = await act('List the years', 'list-academic-years', { subdomain });
console.log(`        ${yearList.bodyJson.length} year(s): ${yearList.bodyJson.map((y) => y.name).join(', ')}`);
await act('Read one year', 'get-academic-year', { subdomain, pathParams: { name } });
await act('A year name that does not exist', 'get-academic-year', { subdomain, pathParams: { name: '1999-2000' } }, 404);
const current = await act('The year containing today', 'get-current-academic-year', { subdomain });
console.log(`        today falls in ${current.bodyJson.name}`);

console.log('\nSCREEN: Academic year — the calendar');
await act('Add a closed day', 'add-holiday', { subdomain, pathParams: { name }, body: { name: 'Diwali', description: 'Festival of lights', type: 'FESTIVAL', date: '2026-11-08' } });
const two = await act('A second reason on the same day', 'add-holiday', { subdomain, pathParams: { name }, body: { name: 'Weekly Off', type: 'WEEKLY_OFF', date: '2026-11-08' } });
console.log(`        ${two.bodyJson.closedDayCount} closed day, ${two.bodyJson.eventCount} reasons — "${two.bodyJson.changeSummary}"`);
await act('The same kind twice', 'add-holiday', { subdomain, pathParams: { name }, body: { name: 'Weekly Off again', type: 'WEEKLY_OFF', date: '2026-11-08' } }, 409);
await act('A day outside the year', 'add-holiday', { subdomain, pathParams: { name }, body: { name: 'New Year', type: 'PUBLIC_HOLIDAY', date: '2028-01-01' } }, 400);
await act('Edit one reason', 'update-holiday', { subdomain, pathParams: { name, date: '2026-11-08' }, query: { type: 'FESTIVAL' }, body: { name: 'Diwali (day 1)', description: 'Lakshmi Puja' } });
await act('Remove one reason', 'remove-holiday', { subdomain, pathParams: { name, date: '2026-11-08' }, query: { type: 'WEEKLY_OFF' } });
const reopened = await act('“Reopen” the whole day', 'remove-holiday', { subdomain, pathParams: { name, date: '2026-11-08' } });
console.log(`        "${reopened.bodyJson.changeSummary}"`);
const imported = await act('Import a calendar', 'replace-holiday-calendar', { subdomain, pathParams: { name }, body: [
  { name: 'Weekly Off', type: 'WEEKLY_OFF', date: '2026-11-08' },
  { name: 'Diwali', type: 'FESTIVAL', date: '2026-11-08', description: 'Festival of lights' },
  { name: 'Independence Day', type: 'PUBLIC_HOLIDAY', date: '2026-08-15' },
] });
console.log(`        ${imported.bodyJson.closedDayCount} closed days, ${imported.bodyJson.eventCount} reasons, by kind ${JSON.stringify(imported.bodyJson.countsByType)}`);
console.log(`        first day drawn: ${imported.bodyJson.holidays[0].date} (${imported.bodyJson.holidays[0].dayOfWeek}) — ${imported.bodyJson.holidays[0].events.map(e => e.name).join(' + ')}`);
const weekly = await act('Set the weekly day off', 'generate-weekly-off', { subdomain, pathParams: { name }, body: { dayOfWeek: 'SUNDAY' } });
console.log(`        ${weekly.bodyJson.generated} added, ${weekly.bodyJson.skippedAlreadyWeeklyOff} already there`);
const cleared = await act('Clear the weekly days off', 'remove-holidays-by-type', { subdomain, pathParams: { name }, query: { type: 'WEEKLY_OFF' } });
console.log(`        back to ${cleared.bodyJson.closedDayCount} closed days`);
await act('Extend the year', 'update-academic-year-dates', { subdomain, pathParams: { name }, body: { endDate: '2027-05-31' } });
await act('Shrink past a closed day', 'update-academic-year-dates', { subdomain, pathParams: { name }, body: { startDate: '2026-12-01' } }, 409);

console.log('\nSCREEN: Academic year — the two questions');
const cal = await act('Read the calendar', 'get-holiday-calendar', { subdomain, pathParams: { name } });
console.log(`        ${cal.bodyJson.closedDayCount} closed days, ${cal.bodyJson.eventCount} reasons`);
const closed = await act('“Is the school open?” on a closed day', 'get-day-status', { subdomain, pathParams: { name, date: '2026-11-08' } });
console.log(`        2026-11-08 closed=${closed.bodyJson.closed} because ${closed.bodyJson.events.map((e) => e.name).join(' + ') || '—'}`);
const open = await act('“Is the school open?” on a working day', 'get-day-status', { subdomain, pathParams: { name, date: '2026-09-15' } });
console.log(`        2026-09-15 closed=${open.bodyJson.closed}`);
const week = await act('Count the working days in a week', 'count-working-days', { subdomain, pathParams: { name }, query: { from: '2026-11-02', to: '2026-11-08' } });
console.log(`        ${week.bodyJson.workingDayCount} working of ${week.bodyJson.totalDayCount} days`);
const whole = await act('Count them across the whole year', 'count-working-days', { subdomain, pathParams: { name } });
console.log(`        ${whole.bodyJson.workingDayCount} working, ${whole.bodyJson.closedDayCount} closed, ${whole.bodyJson.totalDayCount} in the year`);

console.log('\nSCREEN: School detail — change the web address');
const renamed = await act('Change it', 'change-subdomain', { pathParams: { id }, body: { currentSubdomain: subdomain, newSubdomain: `${subdomain}-renamed` } });
console.log(`        ${renamed.bodyJson.previousSubdomain} → ${renamed.bodyJson.subdomain}`);
await act('Change it to a reserved name', 'change-subdomain', { pathParams: { id }, body: { currentSubdomain: renamed.bodyJson.subdomain, newSubdomain: 'api' } }, 409);

console.log(`\nPASS=${pass} FAIL=${fail}`);
process.exit(fail ? 1 : 0);
