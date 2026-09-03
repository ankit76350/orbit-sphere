/**
 * Renders the app in a real DOM with a stubbed backend, so effects actually run.
 *
 *     node behaviour-test.mjs
 *
 * This is the test that catches what the others cannot: how many calls a screen makes when it
 * loads, and whether what comes back reaches the screen. The render test proves a screen does
 * not throw; this one proves it behaves.
 */
import { writeFileSync, rmSync } from 'node:fs';
import { JSDOM } from 'jsdom';
import * as esbuild from 'esbuild';

const dom = new JSDOM('<!doctype html><html><body><div id="root"></div></body></html>', {
  url: 'http://localhost:1300/',
  pretendToBeVisual: true,
});
globalThis.window = dom.window;
globalThis.document = dom.window.document;
// Node defines navigator as a getter, so it has to be replaced rather than assigned.
Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator, configurable: true });
globalThis.HTMLElement = dom.window.HTMLElement;
globalThis.Node = dom.window.Node;
globalThis.Event = dom.window.Event;
globalThis.MouseEvent = dom.window.MouseEvent;
globalThis.getComputedStyle = dom.window.getComputedStyle;
globalThis.requestAnimationFrame = (fn) => setTimeout(fn, 0);
globalThis.cancelAnimationFrame = clearTimeout;
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

const memory = new Map();
globalThis.localStorage = dom.window.localStorage ?? {
  getItem: (k) => (memory.has(k) ? memory.get(k) : null),
  setItem: (k, v) => memory.set(k, String(v)),
  removeItem: (k) => memory.delete(k),
};

/* ------------------------------------------------------------------ the stub backend */

const calls = [];
let handler = () => ({ status: 200, body: {} });

globalThis.fetch = async (url, options = {}) => {
  calls.push({ url: String(url), method: options.method || 'GET' });
  const { status, body, contentType } = handler(String(url), options);
  const text = body === undefined ? '' : JSON.stringify(body);
  // A dev server whose backend is down answers text/plain with nothing in it — the shape the
  // client has to tell apart from a real server error, so the stub has to be honest about it.
  const type = contentType ?? (text === '' ? 'text/plain' : 'application/json');
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: '',
    headers: { forEach(fn) { fn(type, 'content-type'); }, get: (name) => (/content-type/i.test(name) ? type : null) },
    text: async () => text,
  };
};

const { default: React } = await import('react');
const { createRoot } = await import('react-dom/client');
const { act } = await import('react');

const built = await esbuild.build({
  entryPoints: ['render-entry.js'],
  bundle: true, format: 'esm', write: false, jsx: 'automatic',
  external: ['react', 'react-dom', 'react-dom/client', 'react/jsx-runtime', 'lucide-react'],
  loader: { '.css': 'empty' },
});
const bundlePath = new URL('./.behaviour-bundle.mjs', import.meta.url);
writeFileSync(bundlePath, built.outputFiles[0].text);
const parts = await import(bundlePath.href);
rmSync(bundlePath);

const { App, ApiProvider, SchoolSettings, AcademicYearPage } = parts;

/** Mounts something, lets its effects settle, and hands back the text on screen. */
async function mount(element) {
  calls.length = 0;
  const container = document.createElement('div');
  document.body.appendChild(container);
  const root = createRoot(container);
  await act(async () => {
    root.render(element);
  });
  // Let the debounced list load and any follow-up reads finish.
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 600));
  });
  const text = container.textContent;
  return { text, container, unmount: () => act(async () => root.unmount()) };
}

const wrap = (node) => React.createElement(ApiProvider, null, node);

const school = {
  schoolId: 'sch-1', schoolName: 'Orbit Astra International School', subdomain: 'orbit-astra',
  status: 'ACTIVE', accountHolderName: 'Rohan Shinde', city: 'Pune', countryCode: 'IN',
};

let pass = 0, fail = 0;
const check = (label, ok, detail = '') => {
  ok ? pass++ : fail++;
  console.log(`  ${ok ? 'ok  ' : 'FAIL'} ${label}${ok || !detail ? '' : `\n         ${detail}`}`);
};

/* ------------------------------------------------------------------------ the checks */

console.log('\nThe schools list');
handler = () => ({ status: 200, body: {
  content: [{ schoolId: 'sch-1', schoolName: 'Orbit Astra International School',
    subdomain: 'orbit-astra', status: 'ACTIVE', accountHolderName: 'Rohan Shinde',
    city: 'Pune', countryCode: 'IN', createdAt: '2026-08-01T04:00:00Z' }],
  page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false } });

let view = await mount(React.createElement(App));
const listCalls = calls.filter((one) => one.url.includes('/platform/schools'));
// THE LOOP. React's development mode mounts twice, so two is expected and three is a bug.
check(`loads the list once, not over and over (${listCalls.length} call${listCalls.length === 1 ? '' : 's'})`,
  listCalls.length > 0 && listCalls.length <= 2, `made ${listCalls.length} calls`);
check('draws the school that came back', view.text.includes('Orbit Astra International School'));
check('draws its status in words', view.text.includes('Live'));
check('draws the count', view.text.includes('1 school on the platform'), view.text.slice(0, 200));
await view.unmount();

console.log('\nThe schools list when the backend is down');
handler = () => ({ status: 500, body: undefined });
view = await mount(React.createElement(App));
check('says the backend is not running', view.text.includes('The backend is not running'));
check('does not claim the backend threw', !view.text.includes('Check the server log'));
const downCalls = calls.filter((one) => one.url.includes('/platform/schools')).length;
check(`does not retry in a loop while it is down (${downCalls})`, downCalls <= 2, `made ${downCalls} calls`);
await view.unmount();

console.log('\nSettings');
handler = () => ({ status: 200, body: {
  schoolId: 'sch-1', subdomain: 'orbit-astra', status: 'ACTIVE',
  schoolName: 'Orbit Astra International School', accountHolderName: 'Rohan Shinde',
  emailAddress: 'office@orbit.edu', phoneNumber: '+919876543210',
  addressLine: '12, MG Road', city: 'Pune', stateOrProvince: 'Maharashtra', postalCode: '411001',
  defaultLocale: 'en-IN', defaultTimeZone: 'Asia/Kolkata', logoUrl: null, countryCode: 'IN' } });
view = await mount(wrap(React.createElement(SchoolSettings, { school, onChanged() {} })));
check('reads the profile from the school surface',
  calls.some((one) => one.url.endsWith('/schools/current') && one.method === 'GET'),
  calls.map((c) => `${c.method} ${c.url}`).join(', '));
check('shows the four forms', ['School details', 'Address', 'Language and time', 'Logo'].every((t) => view.text.includes(t)));
await view.unmount();

console.log('\nThe academic year');
handler = (url) => {
  if (url.endsWith('/academic-years')) return { status: 200, body: [
    { academicYearId: 'y1', name: '2026-2027', startDate: '2026-04-01', endDate: '2027-03-31',
      durationDays: 365, current: true, holidayCount: 2, enrollmentEnabled: true, resultsLocked: false }] };
  if (url.endsWith('/holidays')) return { status: 200, body: {
    academicYearName: '2026-2027', closedDayCount: 2, eventCount: 3,
    countsByType: { WEEKLY_OFF: 1, PUBLIC_HOLIDAY: 1, FESTIVAL: 1 },
    holidays: [
      { date: '2026-08-15', dayOfWeek: 'SATURDAY', events: [{ name: 'Independence Day', description: null, type: 'PUBLIC_HOLIDAY' }] },
      { date: '2026-11-08', dayOfWeek: 'SUNDAY', events: [
        { name: 'Weekly Off', description: null, type: 'WEEKLY_OFF' },
        { name: 'Diwali', description: 'Festival of lights', type: 'FESTIVAL' }] }] } };
  return { status: 200, body: {
    academicYearId: 'y1', name: '2026-2027', startDate: '2026-04-01', endDate: '2027-03-31',
    durationDays: 365, current: true, holidayCount: 2, enrollmentEnabled: true, resultsLocked: false } };
};
view = await mount(wrap(React.createElement(AcademicYearPage, { school })));
check('lists the years', calls.some((one) => one.url.endsWith('/academic-years')));
check('reads the calendar', calls.some((one) => one.url.endsWith('/holidays')));
check('no longer says there is no read endpoint', !view.text.includes('no endpoint that reads'));
check('draws the year', view.text.includes('2026-2027') && view.text.includes('365 days'));
check('draws the closed days', view.text.includes('Independence Day') && view.text.includes('Diwali'));
check('shows a day closed for two reasons', view.text.includes('Weekly Off') && view.text.includes('Diwali'));
check('offers the working-day count', view.text.includes('How many working days?'));
check('offers the single-day check', view.text.includes('Is the school open?'));
await view.unmount();

console.log('\nThe academic year, when there is not one yet');
handler = (url) => (url.endsWith('/academic-years') ? { status: 200, body: [] } : { status: 404, body: { code: 'ACADEMIC_YEAR_NOT_FOUND' } });
view = await mount(wrap(React.createElement(AcademicYearPage, { school })));
check('asks for the first year to be created', view.text.includes('No academic year yet'));
await view.unmount();

console.log('\nThe endpoint tags');
handler = () => ({ status: 200, body: {
  content: [{ schoolId: 'sch-1', schoolName: 'Orbit Astra International School',
    subdomain: 'orbit-astra', status: 'PROVISIONING', createdAt: '2026-08-01T04:00:00Z' }],
  page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false } });
view = await mount(React.createElement(App));
// The point of the tags: the screen says which endpoint each control calls, and the one under
// the heading shows the request that was actually sent — paging and sorting included.
check('names the method', view.text.includes('GET') && view.text.includes('POST'));
check('shows the live query, not a description of it',
  view.text.includes('/platform/schools?page=0&size=20&sort=createdAt,desc'),
  view.text.slice(0, 300));
// A tag for an endpoint that has been called is a button that reopens the last call. One that
// has not been called says so instead of looking like a dead control.
const tagTitles = [...view.container.querySelectorAll('[title*="/platform/schools"]')]
  .map((one) => one.getAttribute('title'));
check('a called endpoint offers its last call', tagTitles.some((one) => one.includes('click to see')),
  tagTitles.join(' | ').slice(0, 300));
check('an uncalled endpoint says so rather than looking dead',
  tagTitles.some((one) => one.includes('not called yet')), tagTitles.join(' | ').slice(0, 300));
await view.unmount();

console.log('\nThe endpoint tags follow the filters');
view = await mount(React.createElement(App));
const searchBox = view.container.querySelector('input[placeholder="Search by name or subdomain"]');
await act(async () => {
  // React tracks its own value, so the setter has to be called the way React would see it.
  const setter = Object.getOwnPropertyDescriptor(dom.window.HTMLInputElement.prototype, 'value').set;
  setter.call(searchBox, 'orbit');
  searchBox.dispatchEvent(new dom.window.Event('input', { bubbles: true }));
});
check('the tag picks up what was typed', view.container.textContent.includes('search=orbit'),
  view.container.textContent.slice(0, 300));
await view.unmount();

console.log('\nThe side panel');
handler = () => ({ status: 200, body: {
  content: [{ schoolId: 'sch-1', schoolName: 'Orbit Astra International School',
    subdomain: 'orbit-astra', status: 'ACTIVE', createdAt: '2026-08-01T04:00:00Z' }],
  page: 0, size: 20, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false } });
view = await mount(React.createElement(App));
check('the panel lists the modules', view.text.includes('Modules') && view.text.includes('Core'));
check('Plans is shown but not enterable', view.text.includes('Plans')
  && view.text.includes('no screens yet'));
const plansButton = [...view.container.querySelectorAll('button')]
  .find((b) => b.textContent.includes('Plans'));
check('and it is actually disabled', plansButton?.disabled === true);
check('Core lists the school list', view.text.includes('Schools'));
check('a school\'s own screens are not offered until one is open',
  !view.text.includes('Academic year'), view.text.slice(0, 200));
check(`opening the panel loads nothing extra (${calls.length})`, calls.length <= 2,
  `made ${calls.length} calls`);
await view.unmount();

console.log('\nThe side panel with a school open');
view = await mount(React.createElement(App));
const row = [...view.container.querySelectorAll('tr')]
  .find((r) => r.textContent.includes('Orbit Astra International School'));
await act(async () => { row.click(); });
await act(async () => { await new Promise((r) => setTimeout(r, 250)); });
// A school being open is what makes its screens meaningful, so that is when the panel offers
// them — a Settings link with no school chosen would go nowhere.
check('the panel now names the school', view.container.textContent.includes('Orbit Astra'));
check('and offers its screens', ['Overview', 'Settings', 'Academic year']
  .every((label) => view.container.textContent.includes(label)));
const settingsLink = [...view.container.querySelectorAll('button')]
  .filter((b) => b.textContent.trim() === 'Settings');
check(`Settings is reachable from the panel (${settingsLink.length} way(s))`,
  settingsLink.length >= 1);
await act(async () => { settingsLink[0].click(); });
await act(async () => { await new Promise((r) => setTimeout(r, 250)); });
check('choosing it from the panel changes the screen',
  view.container.textContent.includes('School details')
  || view.container.textContent.includes('Loading the'),
  view.container.textContent.slice(0, 240));
await view.unmount();

console.log(`\nPASS=${pass} FAIL=${fail}`);
process.exit(fail ? 1 : 0);
