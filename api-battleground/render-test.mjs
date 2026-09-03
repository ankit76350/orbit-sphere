/**
 * Renders every screen headlessly, to catch anything that throws on first paint.
 *
 *     node render-test.mjs
 *
 * Not a substitute for opening the app, but it fails loudly on a typo in a component that only
 * shows up two clicks in.
 */
import { writeFileSync, rmSync } from 'node:fs';
import * as esbuild from 'esbuild';
import { renderToString } from 'react-dom/server';
import React from 'react';

const memory = new Map();
globalThis.localStorage = {
  getItem: (k) => (memory.has(k) ? memory.get(k) : null),
  setItem: (k, v) => memory.set(k, String(v)),
  removeItem: (k) => memory.delete(k),
};
globalThis.window = globalThis;
globalThis.navigator ??= { clipboard: { writeText: async () => {} } };

const built = await esbuild.build({
  entryPoints: ['render-entry.js'],
  bundle: true,
  format: 'esm',
  write: false,
  jsx: 'automatic',
  external: ['react', 'react-dom', 'react/jsx-runtime', 'lucide-react'],
  loader: { '.css': 'empty' },
});
const bundlePath = new URL('./.render-bundle.mjs', import.meta.url);
writeFileSync(bundlePath, built.outputFiles[0].text);
const parts = await import(bundlePath.href);
rmSync(bundlePath);

const {
  App, ApiProvider, SchoolDetail, SchoolSettings, AcademicYearPage, ApiDetailsModal, NewSchoolModal,
  ActivityModal, ModulePanel, PlansPage, PlanDetail, PlanFeaturesTab, NewPlanModal,
} = parts;

const school = {
  schoolId: '6a95000000000000000000aa',
  schoolName: 'Orbit Astra International School',
  subdomain: 'orbit-astra',
  status: 'ACTIVE',
  statusReason: 'Cleared on 27 August.',
  accountHolderName: 'Rohan Shinde',
  emailAddress: 'office@orbit-school.edu',
  phoneNumber: '+919876543210',
  city: 'Pune',
  countryCode: 'IN',
  defaultLocale: 'en-IN',
  defaultTimeZone: 'Asia/Kolkata',
  addressLine: '12, MG Road',
  stateOrProvince: 'Maharashtra',
  postalCode: '411001',
  createdAt: '2026-08-01T04:00:00Z',
  activatedAt: '2026-08-02T04:00:00Z',
  suspendedAt: null,
};

const entry = {
  id: '1', at: new Date().toISOString(), action: 'Take the school live', method: 'POST',
  path: '/platform/schools/abc/activate', ok: true, status: 200, durationMs: 245, sizeBytes: 512,
  endpoint: { name: 'Activate School', docs: '**POST** `/x` — notes\n\n### A heading\n\n| a | b |\n|---|---|\n| 1 | 2 |' },
  result: {
    ok: true, status: 200, statusText: 'OK', durationMs: 245, sizeBytes: 512,
    startedAtIso: new Date().toISOString(), finishedAtIso: new Date().toISOString(), timeoutMs: 30000,
    headers: { 'content-type': 'application/json', location: '/platform/schools/abc' },
    bodyText: '{"status":"ACTIVE"}', bodyJson: { status: 'ACTIVE', nextStep: 'The school is live.' },
    jsonParseError: null, error: null,
    request: { method: 'POST', url: 'http://localhost:3456/platform/schools/abc/activate',
      path: '/platform/schools/abc/activate', headers: { 'Content-Type': 'application/json' },
      body: '{"reason":"x"}', pathParams: { id: 'abc' }, authType: 'none' },
  },
};

const failedEntry = {
  ...entry, id: '2', ok: false, status: 409, action: 'Suspend the school',
  result: {
    ...entry.result, ok: false, status: 409, statusText: 'Conflict',
    bodyJson: { code: 'SCHOOL_NOT_SUSPENDABLE', message: 'A school at status TRIAL cannot be suspended.',
      fieldErrors: { reason: ['must not be blank'] } },
  },
};

/** Real payloads, copied from the backend, so the summary is proved against real shapes. */
const withBody = (bodyJson, action) => ({
  ...entry, id: action, action,
  result: { ...entry.result, bodyJson, bodyText: JSON.stringify(bodyJson) },
});

const setupDone = withBody({
  schoolId: 'abc', subdomain: 'orbit-astra', status: 'PROVISIONING',
  numberSequencesCreated: 47, numberSequencesAlreadyPresent: 0,
  rolesCreated: 3, rolesAlreadyPresent: 0,
  roleKeys: ['GUARDIAN', 'SCHOOL_ADMIN', 'TEACHER'],
  readyToActivate: true, nextStep: 'Create the first administrator account, then activate the school.',
}, 'Finish setting up');

const calendar = withBody({
  academicYearName: '2026-2027', startDate: '2026-04-01', endDate: '2027-03-31',
  closedDayCount: 2, eventCount: 3,
  countsByType: { WEEKLY_OFF: 1, PUBLIC_HOLIDAY: 1, FESTIVAL: 1 },
  holidays: [
    { date: '2026-08-15', dayOfWeek: 'SATURDAY', events: [{ name: 'Independence Day', description: null, type: 'PUBLIC_HOLIDAY' }] },
    { date: '2026-11-08', dayOfWeek: 'SUNDAY', events: [
      { name: 'Weekly Off', description: null, type: 'WEEKLY_OFF' },
      { name: 'Diwali', description: 'Festival of lights', type: 'FESTIVAL' }] },
  ],
  changeSummary: 'Replaced the calendar: 0 closed days out, 2 in (3 reasons).',
}, 'Replace the calendar');

const schoolList = withBody({
  content: [{ schoolId: 'a', schoolName: 'Orbit Astra', subdomain: 'orbit-astra', status: 'ACTIVE',
    city: 'Pune', countryCode: 'IN', createdAt: '2026-08-01T04:00:00Z' }],
  page: 0, size: 20, totalElements: 63, totalPages: 4, hasNext: true, hasPrevious: false,
}, 'Load schools');

/** What a dead backend behind the dev proxy actually looks like: 500, text/plain, no body. */
const backendDown = {
  ...entry, id: 'down', ok: false, action: 'Suspend the school',
  result: {
    ...entry.result, ok: false, status: 500, statusText: 'Internal Server Error',
    headers: { 'content-type': 'text/plain' }, bodyText: '', bodyJson: null, sizeBytes: 0,
    error: {
      kind: 'backend-unreachable',
      title: 'The backend is not running',
      message: 'The dev server could not reach it, so the request never got as far as the application. Nothing was read and nothing was changed.',
      hint: 'Start it with:  cd backend && ./mvnw spring-boot:run  — it listens on port 3456 and takes about a minute.',
    },
  },
};

const wrap = (node) => React.createElement(ApiProvider, null, node);

const screens = [
  ['Schools list (the whole app)', React.createElement(App), ['Schools', 'Add a school']],
  ['Module panel — the plans module', wrap(React.createElement(ModulePanel,
    { moduleId: 'plans', onModule() {}, school: null, tab: 'overview', onTab() {}, onSchools() {},
      plan: { planCode: 'PREMIUM', planVersion: 2 }, planTab: 'features', onPlanTab() {}, onPlans() {} })),
    ['Plans', 'Catalogue', 'PREMIUM v2', 'Features', 'Versions']],
  ['Plans list — first paint', wrap(React.createElement(PlansPage, { onOpenPlan() {} })),
    ['Plans', 'New plan', 'Search by name or code']],
  ['Plan detail — first paint', wrap(React.createElement(PlanDetail,
    { plan: { planCode: 'PREMIUM', planVersion: 1 }, onBack() {} })),
    ['Loading PREMIUM']],
  ['Plan features — a draft', wrap(React.createElement(PlanFeaturesTab,
    { plan: { planCode: 'PREMIUM', planVersion: 1, status: 'DRAFT', featureCount: 0, features: [] },
      onChanged() {} })),
    // "Show as excluded" is not here: it belongs to a ticked feature, and the tick state comes
    // from an effect this test does not run.
    ['What this plan includes', 'Student management', 'Save the list', 'priced as a set']],
  ['Plan features — published, so frozen', wrap(React.createElement(PlanFeaturesTab,
    { plan: { planCode: 'PREMIUM', planVersion: 1, status: 'ACTIVE', featureCount: 1,
      features: [{ featureCode: 'TRANSPORT', label: 'Transport', enabled: true, usageLimit: 12,
        usageMetric: 'VEHICLES', overagePolicy: 'BLOCK' }] }, onChanged() {} })),
    // Published: read-only, and the rows come from an effect, so the first paint is the header.
    // Assert nothing that spans two JSX expressions — React SSR puts a <!-- --> between them, so
    // "1 included" is "1<!-- --> included" in the markup this test greps.
    ['cannot be changed', 'Make a new version']],
  ['New plan form', wrap(React.createElement(NewPlanModal,
    { open: true, onClose() {}, onCreated() {} })),
    ['New plan', 'Billing cycle', 'Students included', 'Features are set after this']],
  ['Module panel — nothing open', wrap(React.createElement(ModulePanel,
    { moduleId: 'core', onModule() {}, school: null, tab: 'overview', onTab() {}, onSchools() {} })),
    ['Modules', 'Core', 'Schools', 'Plans']],
  ['Module panel — a school open', wrap(React.createElement(ModulePanel,
    { moduleId: 'core', onModule() {}, school, tab: 'settings', onTab() {}, onSchools() {} })),
    ['Orbit Astra International School', 'Overview', 'Settings', 'Academic year']],
  ['School detail', wrap(React.createElement(SchoolDetail, { school, onBack() {}, onChanged() {} })),
    ['Orbit Astra International School', 'Suspend', 'Change web address', 'Academic year']],
  ['School detail — being set up',
    wrap(React.createElement(SchoolDetail, { school: { ...school, status: 'PROVISIONING' }, onBack() {}, onChanged() {} })),
    ['Finish setting up', 'Take it live', 'not usable yet']],
  // These three read on mount, so their first paint is a loading state. What they look like
  // once the answer is in is covered by behaviour-test.mjs, which runs the effects.
  ['Settings — first paint', wrap(React.createElement(SchoolSettings, { school, onChanged() {} })),
    // No apostrophe in the assertion: React escapes it to &#x27; in the rendered HTML.
    ['Loading the', 'settings']],
  ['Settings — closed school',
    wrap(React.createElement(SchoolSettings, { school: { ...school, status: 'CLOSED' }, onChanged() {} })),
    ['cannot be changed']],
  ['Academic year — first paint', wrap(React.createElement(AcademicYearPage, { school })),
    ['Loading the academic years']],
  ['Add a school form', wrap(React.createElement(NewSchoolModal, { open: true, onClose() {}, onCreated() {} })),
    ['Add a school', 'Web address', 'Time zone', 'Start this school on a trial']],
  ['Technical details — success', wrap(React.createElement(ApiDetailsModal, { entry, onClose() {} })),
    ['Take the school live', 'What happened', 'What we sent', 'The raw answer', '200 OK', 'The school is live.']],
  ['Technical details — failure', wrap(React.createElement(ApiDetailsModal, { entry: failedEntry, onClose() {} })),
    ['SCHOOL_NOT_SUSPENDABLE', 'cannot be suspended', 'must not be blank']],
  ['Summary — setup counts', wrap(React.createElement(ApiDetailsModal, { entry: setupDone, onClose() {} })),
    ['Number sequences created', '47', 'School admin', 'Ready to activate', 'Yes']],
  ['Summary — holiday calendar', wrap(React.createElement(ApiDetailsModal, { entry: calendar, onClose() {} })),
    ['Replaced the calendar', 'Closed day count', 'Weekly Off', 'Diwali', 'Independence Day', 'Weekly off · 1']],
  ['Summary — the school list', wrap(React.createElement(ApiDetailsModal, { entry: schoolList, onClose() {} })),
    ['Total elements', '63', 'Orbit Astra', 'Live', 'Has next']],
  ['Backend down — says so, not "500"',
    wrap(React.createElement(ApiDetailsModal, { entry: backendDown, onClose() {} })),
    ['The backend is not running', './mvnw spring-boot:run', 'never got as far as the application']],
  ['Activity list', wrap(React.createElement(ActivityModal, { open: true, onClose() {}, log: [entry, failedEntry], onInspect() {}, onClear() {} })),
    ['Activity', 'Take the school live', 'Suspend the school']],
];

let failures = 0;
for (const [name, element, musts] of screens) {
  let html;
  try {
    html = renderToString(element);
  } catch (error) {
    failures++;
    console.log(`  CRASH ${name}: ${error.message}`);
    continue;
  }
  const missing = musts.filter((text) => !html.includes(text));
  if (missing.length) {
    failures++;
    console.log(`  MISS  ${name} — did not render: ${missing.join(', ')}`);
  } else {
    console.log(`  ok    ${name.padEnd(34)} ${String(html.length).padStart(6)} chars`);
  }
}

/* --------------------------------------------------------------------------------------
 * Two static checks, for the two mistakes that got past the tests above.
 * ------------------------------------------------------------------------------------ */

const { readdirSync, statSync } = await import('node:fs');
const { readFileSync } = await import('node:fs');
const { join } = await import('node:path');

function sourceFiles(dir) {
  return readdirSync(dir).flatMap((entry) => {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) return sourceFiles(full);
    return /\.(jsx?|mjs)$/.test(entry) ? [full] : [];
  });
}
const files = sourceFiles('src');

console.log('\nChecks');

// 1. A dependency array holding the whole api object. When the context carried the activity
//    log, this made a screen reload itself forever. The context is split now, but a future
//    merge would bring the loop straight back.
let loopHazards = 0;
for (const file of files) {
  const text = readFileSync(file, 'utf8');
  text.split('\n').forEach((line, index) => {
    if (/^\s*\}, \[[^\]]*\bapi\b[^\]]*\]\);/.test(line)) {
      loopHazards++;
      console.log(`  HAZARD ${file}:${index + 1} depends on the whole api object — ${line.trim()}`);
    }
  });
}
if (loopHazards === 0) console.log('  ok    no screen depends on the whole api object');
failures += loopHazards;

// 2. Every endpoint a screen asks for actually exists. buildCall throws on a wrong name, but
//    only when the button is pressed — which is how a rename broke three handlers silently.
const { ALL_ENDPOINTS } = await import('./src/config/endpoints.js');
const known = new Set(ALL_ENDPOINTS.map((one) => one.id));
let unknown = 0;
let checked = 0;
for (const file of files) {
  const text = readFileSync(file, 'utf8');
  const asked = [
    ...text.matchAll(/\bcall\(\s*'([^']+)'/g),          // call('activate-school', …)
    // The screens each wrap call() in a small helper — act(), run(), save() — that takes the
    // endpoint name third. Check those too; a rename broke three of them silently.
    ...text.matchAll(/\b(?:act|run|save)\([^,]+,[^,]+,\s*'([^']+)'/g),
  ].map((match) => match[1]);
  for (const id of asked) {
    checked++;
    if (!known.has(id)) {
      unknown++;
      console.log(`  UNKNOWN ${file} asks for "${id}", which is not in the catalog`);
    }
  }
}
if (unknown === 0) console.log(`  ok    all ${checked} endpoint names used by the screens exist`);
failures += unknown;

console.log(failures ? `\n${failures} problem(s)` : '\nEvery screen renders, every check passes');
process.exit(failures ? 1 : 0);
