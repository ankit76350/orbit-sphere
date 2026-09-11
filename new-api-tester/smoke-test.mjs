/**
 * Renders every route headlessly and checks the navigation is right.
 *
 *     npm test
 *
 * Not a substitute for opening the app. It catches the things that break silently: a nav item
 * pointing at an address with no screen, a module offering another module's submodules, a route
 * that throws on first paint. Bundled with rolldown, which Vite already ships here, so there is
 * no extra dependency and nothing to install.
 */
import { writeFileSync, rmSync } from 'node:fs'
import React from 'react'
import { renderToString } from 'react-dom/server'
import { StaticRouter } from 'react-router-dom'
import { rolldown } from 'rolldown'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'
import { detailPath } from './src/paths.js'
import { sellability } from './src/pages/platform/plans/planFacts.js'
import { changedFields, patchBody, storedForm } from './src/pages/platform/plans/subscriptionEdit.js'
import { startOfDayInZone } from './src/lib/dates.js'
import { endOfDay, startOfDay, toDateInput } from './src/lib/dates.js'

// The store remembers the chosen environment in the browser, and reads it while the provider
// first renders — so there has to be something to read here.
const memory = new Map()
globalThis.localStorage = {
  getItem: (key) => (memory.has(key) ? memory.get(key) : null),
  setItem: (key, value) => memory.set(key, String(value)),
  removeItem: (key) => memory.delete(key),
}

const bundle = await rolldown({
  // Both, so the screens can be rendered inside the provider they need.
  input: 'smoke-entry.js',
  external: ['react', /^react\//, 'react-dom', /^react-dom\//, 'lucide-react',
             'react-router', 'react-router-dom'],
  // The styles are irrelevant to what renders, and nothing here can parse them.
  plugins: [{
    name: 'ignore-css',
    load: (id) => (id.endsWith('.css') ? { code: 'export default {}' } : null),
  }],
})
const { output } = await bundle.generate({ format: 'esm' })
const tmp = new URL('./.smoke-bundle.mjs', import.meta.url)
writeFileSync(tmp, output[0].code)
const { App, ApiProvider, Modal } = await import(tmp.href)
rmSync(tmp)

/**
 * Renders one address.
 *
 * Wrapped in ApiProvider because the screens call useApi(). Nothing is sent: renderToString does
 * not run effects, so this is first paint only — which is exactly the moment a broken screen
 * throws.
 */
const at = (path) => renderToString(
  React.createElement(ApiProvider, null,
    React.createElement(StaticRouter, { location: path }, React.createElement(App))))

/** Every address the navigation offers, and words that prove the right screen answered. */
const ROUTES = [
  // Schools is built, so it should show its own screen and not the placeholder.
  ['/platform-core/schools', ['Platform', 'Core', 'Schools', 'Add a school', '/platform/schools']],
  ['/platform-plans/catalogue', ['Platform', 'Plans', 'Plan catalogue', 'New draft']],
  ['/platform-plans/subscriptions', ['Subscriptions', 'No school picked']],
  // Built, and with no school chosen it says so rather than firing a call that cannot work.
  ['/school-core/profile', ['School', 'Profile', 'No school chosen']],
  ['/school-core/academic-years', ['Academic years', 'No school chosen']],
  ['/school-plans/subscription', ['Subscription', 'No school chosen']],
  // The third module on the school surface. Its plan has 36 endpoints and exactly one exists.
  ['/school-academics/classes', ['Academics', 'Classes', 'No school chosen']],
  // Opening a row is its own address. First paint is the read, because renderToString does not
  // run effects — which is the point: the page reads the school itself rather than being handed
  // a row from a list that may already be stale.
  ['/platform-core/schools/6a95000000000000000000aa', ['Reading the school']],
  // A year is addressed by its name, which the API guarantees never changes.
  ['/school-core/academic-years/2026-2027', ['No school chosen']],
  ['/nonsense', ['Page not found']],
]

let fail = 0
console.log('Routes')
for (const [path, expected] of ROUTES) {
  let html = ''
  try {
    html = at(path)
  } catch (error) {
    console.log(`  THREW  ${path}: ${error.message}`)
    fail++
    continue
  }
  const missing = expected.filter((word) => !html.includes(word))
  if (missing.length) {
    console.log(`  MISS   ${path} — expected ${missing.join(', ')}`)
    fail++
  } else {
    console.log(`  ok     ${path}`)
  }
}

console.log('\nThe Schools screen')
const schools = at('/platform-core/schools')
const screenChecks = [
  ['it is the real screen, not the placeholder', !schools.includes('is not built yet')],
  // The point of this app over a product UI: every control says what it sends.
  ['the list says which endpoint it calls', schools.includes('/platform/schools')],
  ['and which method', schools.includes('>GET<')],
  // A tag names the control it belongs to as well as the request. Read on its own — in the
  // response panel, in the log — the method and path say what was sent but not by what.
  ['and which control sends it', schools.includes('endpoint-tag-name')],
  // The name is a chip like the method, not loose text beside it.
  ['the name is a chip, like the method',
    /<span class="endpoint-tag-name"[^>]*>The list, as filtered<\/span>/.test(schools)],
  // A read is nobody's primary action, so its chip carries no tone.
  ['a plain read is not toned', /class="endpoint-tag-name">The list/.test(schools)],
  ['the name, method and path are all in one tag',
    /endpoint-tag-name[^]*?The list, as filtered[\s\S]{0,200}?GET[\s\S]{0,200}?\/platform\/schools/.test(schools)],
  ['the paging query is in the tag', schools.includes('page=0') && schools.includes('size=20')],
  ['the status filters are offered', schools.includes('Being set up') && schools.includes('Suspended')],
  // Until an endpoint has been called there is nothing to open, so the tag must not look like
  // a button that does nothing.
  ['an uncalled endpoint tag is not a button',
    !/<button[^>]*class="endpoint-tag"/.test(schools)],
  // A row's link cannot be checked here: renderToString runs no effects, so the list has no
  // rows yet. What CAN be wrong is the address it would build, so that is what is asserted.
  ["a row's address is the list's plus the id",
    detailPath('platform', 'core', 'schools', 'abc') === '/platform-core/schools/abc'],
  // The lifecycle actions moved to the detail page; the list must not still offer them.
  ['the list no longer offers the lifecycle actions',
    !schools.includes('Take it live') && !schools.includes('Finish setting up')],
]
for (const [label, ok] of screenChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// A detail address is one segment deeper, and the side panel and body navbar both read the
// module off the FIRST segment — so opening a row must not un-highlight anything.
// A tag's colour has to match the button it belongs to, and the only way that stays true is if
// both read the SAME value. The detail page's lifecycle rows cannot be rendered here — first
// paint is the read — so this checks the thing that would break: the pair drifting apart in the
// source, one hard-coded and the other not.
// The school surface reads its tenant from a header, so it needs somebody to have said which
// school. Every screen under School has to cope with that not having happened yet.
console.log('\nThe school surface without a school')
const profile = at('/school-core/profile')
const surfaceChecks = [
  ['it says no school is chosen', profile.includes('No school chosen')],
  ['it says where to set one', profile.includes('top bar')],
  // Sending anyway would render 400 TENANT_NOT_RESOLVED, which reads as a broken screen.
  ['it does not pretend to have read a profile', !profile.includes('what this school can change')],
  ['the top bar offers the picker', profile.includes('picker-trigger')],
  ['the trigger says none is chosen yet', profile.includes('none chosen')],
  // Closed until asked for: the school list is a request, and drawing the popover on every
  // page would mean making that request on every page.
  ['the dropdown is shut until it is opened', !profile.includes('picker-pop')],
  // Every school-surface screen has to cope with it, not just the first one.
  ['the years list copes too', at('/school-core/academic-years').includes('No school chosen')],
  ["and so does one year's own address",
    at('/school-core/academic-years/2026-2027').includes('No school chosen')],
]
for (const [label, ok] of surfaceChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// WITH A SCHOOL CHOSEN, which is the path the checks above never took — they all stopped at
// "No school chosen" and returned early. That is why a crash on the school-surface detail pages
// went unnoticed: `loading && !data` was false on the very first render, because the effect that
// sets `loading` runs after it, so the page fell through its guards and read `.name` off null.
//
// Every screen renders twice before it has data — once before the effect, once after it starts —
// and the first of those is the pass that has to be safe.
console.log('\nFirst paint with a school chosen')
memory.set('orbit.tester.actingSubdomain', JSON.stringify('lapse-1788507811'))
const withSchool = [
  '/school-core/profile',
  '/school-core/academic-years',
  '/school-core/academic-years/2026-2027',
  '/school-plans/subscription',
  '/platform-core/schools',
  '/platform-core/schools/6a95000000000000000000aa',
  '/platform-plans/catalogue',
  '/platform-plans/catalogue/PREMIUM@2',
  '/platform-plans/subscriptions',
]
for (const path of withSchool) {
  try {
    const html = at(path)
    const empty = !html.includes('page-title') && !html.includes('muted')
    console.log(empty ? `  MISS   ${path} rendered nothing` : `  ok     ${path}`)
    if (empty) fail++
  } catch (error) {
    console.log(`  THREW  ${path}: ${error.message}`)
    fail++
  }
}
memory.delete('orbit.tester.actingSubdomain')

console.log('\nA tag matches its button')
const detailSource = readFileSync('src/pages/platform/core/SchoolDetail.jsx', 'utf8')
const shared = (detailSource.match(/look=\{action\.look\}/g) || []).length
// Every control an action drives, and every tag naming it, takes its colour from the action
// itself. Counting was standing in for that and broke the moment a third place wanted it — what
// matters is that no such tag hardcodes a colour, so a red action never reads as routine.
const actionTags = (detailSource.match(/<EndpointTag\s+id=\{action\.endpoint\}[\s\S]{0,200}?\/>/g)
  || [])
const pairChecks = [
  ['the action button and its tag read one value', shared >= 2],
  [`every tag for an action takes the action's colour (${actionTags.length} of them)`,
    actionTags.length > 0 && actionTags.every((tag) => tag.includes('look={action.look}'))],
  // Suspend is the destructive one; if its look were dropped the tag would look routine.
  ['the destructive action is marked danger', /look: 'danger'/.test(detailSource)],
]
for (const [label, ok] of pairChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nA row\'s own address')
const detail = at('/platform-core/schools/6a95000000000000000000aa')
const detailChecks = [
  ['the module stays active in the side panel', detail.includes('nav-item is-active')],
  ['the body navbar is still there', detail.includes('module-nav-surface')],
  ['and still says Platform', detail.includes('Platform')],
  ['there is a way back to the list',
    /<a[^>]*class="back"[^>]*href="\/platform-core\/schools"/.test(detail)
      || /href="\/platform-core\/schools"[^>]*class="back"/.test(detail)],
]
for (const [label, ok] of detailChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// Every endpoint in the generated catalogue should be reachable from some screen. A tag or a
// call naming one that no screen holds is dead weight; an endpoint no screen names is untested.
// The tenant header is a school-surface thing: no platform endpoint reads it, they all name
// their school in the URL. So "Acting as" on a platform screen was a control that changed
// nothing — and worse, one that implied the screen was scoped to it.
// Publishing is a one-way door, so the selling window it goes out with is permanent. The dialog
// has to ask for both dates rather than letting somebody publish a window nobody looked at.
console.log('\nPublishing asks for the selling window')
const planSource = readFileSync('src/pages/platform/plans/PlanDetail.jsx', 'utf8')
const publishChecks = [
  ['publish asks, retire does not',
    (planSource.match(/asksWindow: true/g) || []).length === 1],
  ['it asks for both dates',
    planSource.includes('effectiveFrom: startOfDay(from)')
      && planSource.includes('effectiveUntil: endOfDay(until)')],
  // "Stops being sold on the 31st" has to include the 31st. Midnight would cut it a day early.
  // Checked against the helper itself, not a string in this screen: the rule moved to lib/dates
  // when a second form needed it, and a check that reads the wrong file passes for the wrong
  // reason. Both of these did, until they were pointed here.
  ['the end date is the END of that day', endOfDay('2027-03-31') === '2027-03-31T23:59:59Z'],
  ['the start date is the start of it', startOfDay('2026-04-01') === '2026-04-01T00:00:00Z'],
  ['and blank stays blank rather than becoming a date',
    endOfDay('') === null && startOfDay('') === null],
  ['an instant comes back as the day it falls on',
    toDateInput('2027-03-31T23:59:59Z') === '2027-03-31' && toDateInput(null) === ''],
  // The form's blank has to mean what the API's absent means, or the two disagree.
  ['the start defaults to today, as the API does', planSource.includes('todayInput()')],
]
for (const [label, ok] of publishChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// THE BUG THIS CATCHES. Confirm used to render always and bail out after its hooks; an edit lost
// that early return and every plan page crashed on `action.title`. First paint never reaches the
// dialog, so no render check here could see it — what CAN be checked is the shape that makes the
// mistake impossible: mounted only when there is something to confirm.
console.log('\nDialogs are mounted, not guarded')
const guardChecks = [
  ['the confirm dialog is mounted conditionally',
    /\{confirming \? \(\s*<Confirm/.test(planSource)],
  ['so it never reads a null action', !planSource.includes('action?.title')],
  // Same shape, same reason, in the other screen that had this bug.
  ['the holiday editor is mounted conditionally too',
    /\{editing \? \(\s*<EditHoliday/.test(
      readFileSync('src/pages/school/core/AcademicYearDetail.jsx', 'utf8'))],
]
for (const [label, ok] of guardChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// A screen that throws should cost one screen, not the whole app. Three crashes so far took the
// shell down with them and left only a console message.
console.log('\nA screen that throws is contained')
const layoutSource = readFileSync('src/components/Layout.jsx', 'utf8')
const boundaryChecks = [
  ['the routed page sits inside a boundary', layoutSource.includes('<ScreenBoundary')],
  // Keyed on the route, or one throw leaves the panel up for every screen visited afterwards.
  ['the boundary is keyed on the route', layoutSource.includes('<ScreenBoundary key={pathname}>')],
  // The shell has to stay outside it, or a broken screen takes the navigation with it.
  ['the side panel and navbar stay outside it',
    layoutSource.indexOf('<Sidebar') < layoutSource.indexOf('<ScreenBoundary')
      && layoutSource.indexOf('<ModuleNav') < layoutSource.indexOf('<ScreenBoundary')],
  ['it reports the error rather than swallowing it',
    readFileSync('src/components/ScreenBoundary.jsx', 'utf8').includes('componentDidCatch')],
]
for (const [label, ok] of boundaryChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// `sellable` on the response is NOT whether this endpoint will sell it, and the subscription
// dialog is the one place that difference costs something. It requires publiclyAvailable, but
// creating a subscription deliberately does not check that field — so a private plan reads
// sellable:false and sells perfectly well. Verified against the backend: publiclyAvailable
// false, sellable false, subscription created.
// The profile answers "who is this school"; the subscription answers "what are they on". Both
// are what you want when you open a school, so the profile reads the subscription too — one
// extra call, in parallel, summarised rather than duplicating the subscription screen.
// A school has no trial state. It had one, and nothing in the codebase ever treated it
// differently from PROVISIONING — the same three checks accepted both, so it was a second word
// for one state. A trial belongs to the SUBSCRIPTION, where it has a plan and a period behind it.
console.log('\nA school has no trial state')
const schoolsSource = readFileSync('src/pages/platform/core/Schools.jsx', 'utf8')
const schoolDetailSource = readFileSync('src/pages/platform/core/SchoolDetail.jsx', 'utf8')
const trialChecks = [
  ['the school list has no trial filter', !schoolsSource.includes("statuses: ['TRIAL']")],
  ['nor a tone for a trial school', !schoolsSource.includes("TRIAL: 'warn'")],
  ['the school page does not offer trial actions', !schoolDetailSource.includes("case 'TRIAL'")],
  ['and PROVISIONING is the only pre-live state',
    /case 'PROVISIONING':\s*\n\s*return \['complete', 'activate'\]/.test(schoolDetailSource)],
  // The subscription's own trial is a different enum and has to survive all of this.
  ['a subscription can still be a trial',
    readFileSync('src/pages/platform/plans/Subscriptions.jsx', 'utf8').includes("s.status === 'TRIAL'")],
  ['and the school surface still shows one',
    readFileSync('src/pages/school/plans/Subscription.jsx', 'utf8').includes("TRIAL: 'warn'")],
]
for (const [label, ok] of trialChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// Creating a subscription can be the last thing a PROVISIONING school needs, so the sale takes it
// to ACTIVE. That happens on the server, and nothing else on the screen would mention it — the
// screen re-reads rather than rendering the 201, and the school is not part of the re-read. So the
// 201's nextStep is the one thing kept, and the school is fetched beside it.
// Editing a subscription sends only what moved, because the endpoint reads an absent field as
// "leave it alone" — a form that posted every box would send twelve fields to change one, and the
// history row it writes would then say twelve fields were edited. This is that rule as a table:
// what the boxes hold against what is stored, and the body it comes to.
// A form of thirteen boxes with no order to it is a form where somebody fills in the cancellation
// date of a subscription they were only trying to reprice. So it is sectioned, the boxes that
// only apply sometimes only appear then, and the dates are calendars rather than typed instants.
// The school surface is a different response type from the platform one, and the fields left out
// are the reason there are two. The screen checks the live response against its own list and
// reports a leak in red — so the list has to actually contain everything that is withheld, or the
// check passes while the leak goes unmentioned.
console.log('\nThe school is not shown what is ours')
const schoolBillSource = readFileSync('src/pages/school/plans/Subscription.jsx', 'utf8')
const withheldChecks = [
  ['the negotiated price is withheld', schoolBillSource.includes("'planListPrice'")],
  ['the gateway id is withheld', schoolBillSource.includes("'billingCustomerReference'")],
  ['both overrides are withheld',
    schoolBillSource.includes("'maxStudentsOverride'") && schoolBillSource.includes("'maxUsersOverride'")],
  ['the internal plan code is withheld', schoolBillSource.includes("'planCode'")],
  // Added with the field: #15 writes an operator's note here, and handing it to the school turns
  // an internal note into a statement to a customer.
  ["and so is the operator's note on the last edit",
    schoolBillSource.includes("'reasonForChanges'")],
  // The field it used to show is gone from the model entirely.
  ['nothing reads the removed cancellation fields',
    !schoolBillSource.includes('cancelledAt') && !schoolBillSource.includes('cancellationReason')],
]
for (const [label, ok] of withheldChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe edit form says what to fill in, and when')
const subsSourceFull = readFileSync('src/pages/platform/plans/Subscriptions.jsx', 'utf8')
// EVERY FILE THIS TEST READS, in one place. Declaring each beside the section that first wanted
// it meant every section added afterwards hit "cannot access before initialization".
// THE SUBSCRIPTION CODE, NOW IN THREE FILES. The service holds the endpoints; the helpers it
// used to carry live in utils/, and what the whole plans module shares lives in helper/ — see
// memory/backend/code-writing-rules/service. Most checks below are about the behaviour rather
// than which file it sits in, so they read the three together and go on passing across a move.
const javaEndpoints = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/services/plans/PlatformSubscriptionService.java',
  'utf8')
const javaSubUtils = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/services/plans/utils/PlatformSubscriptionServiceUtils.java',
  'utf8')
const javaPlansHelper = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/services/plans/helper/PlansHelper.java',
  'utf8')
const javaService = javaEndpoints + '\n' + javaSubUtils + '\n' + javaPlansHelper
const schoolSubUtils = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/services/plans/utils/SchoolSubscriptionServiceUtils.java',
  'utf8')
const createRequestSource = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/dto/plans/subscription/request/SubscriptionCreateRequest.java',
  'utf8')
const editBodySource = readFileSync('src/pages/platform/plans/subscriptionEdit.js', 'utf8')
const css = readFileSync('src/styles/components.css', 'utf8')
const plansReadme = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/controllers/plans/README.md', 'utf8')
// THE GATES. The controller is where they are called and ActionGate is where they are decided,
// so a check that only read one of the two would pass while the other was gutted.
const yearController = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/controllers/core/AcademicYearController.java',
  'utf8')
const actionGateSrc = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/common/access/ActionGate.java', 'utf8')
const coreReadme = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/controllers/core/README.md', 'utf8')
const statusEnum = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/models/plans/enums/SubscriptionStatus.java',
  'utf8')
const editRequestSource = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/dto/plans/subscription/request/SubscriptionUpdateRequest.java',
  'utf8')
const changeRequestSource = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/dto/plans/subscription/request/SubscriptionPlanChangeRequest.java',
  'utf8')
const renewRequestSource = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/dto/plans/subscription/request/SubscriptionRenewRequest.java',
  'utf8')
// The generated catalogue both apps read. It is documentation the user acts on, so a promise it
// makes that the API no longer keeps is a real defect.
const endpointsSource = readFileSync('src/config/endpoints.js', 'utf8')
const datesUtil = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/common/time/Dates.java', 'utf8')
// Every model, concatenated, so the date-typed getter names can be read off them rather than
// guessed at from a list somebody has to remember to update.
// Every backend source, so an import naming a package that no longer exists cannot hide.
const javaSources = (function walkJava(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const full = join(dir, e.name)
    return e.isDirectory() ? walkJava(full)
      : (e.name.endsWith('.java') ? [readFileSync(full, 'utf8')] : [])
  })
})('../backend/src/main/java/com/orbitastra/backend')
const modelSources = (function readModels(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) return readModels(full)
    return entry.name.endsWith('.java') ? [readFileSync(full, 'utf8')] : []
  })
})('../backend/src/main/java/com/orbitastra/backend/models').join('\n')
// Every service that puts a date in a message, so none of them can quietly go back to
// concatenating an instant.
const messageSources = [
  ['PlatformSubscriptionService', javaEndpoints],
  ['PlatformSubscriptionServiceUtils', javaSubUtils],
  ['PlansHelper', javaPlansHelper],
  ['SchoolSubscriptionService', readFileSync(
    '../backend/src/main/java/com/orbitastra/backend/services/plans/SchoolSubscriptionService.java',
    'utf8')],
  ['PlanCatalogueService', readFileSync(
    '../backend/src/main/java/com/orbitastra/backend/services/plans/PlanDefinitionService.java',
    'utf8')],
  ['AcademicYearService', readFileSync(
    '../backend/src/main/java/com/orbitastra/backend/services/core/AcademicYearService.java',
    'utf8')],
  ['AcademicYearServiceUtils', readFileSync(
    '../backend/src/main/java/com/orbitastra/backend/services/core/utils/AcademicYearServiceUtils.java',
    'utf8')],
  ['CoreValidator', readFileSync(
    '../backend/src/main/java/com/orbitastra/backend/services/core/helper/CoreHelper.java',
    'utf8')],
]
// Scoped to the edit form, because two other modals in the same file have date boxes of their own
// and a file-wide count cannot tell them apart. The slice ends at whichever function comes next,
// so adding another modal cannot silently widen it.
const editFormStart = subsSourceFull.indexOf('function EditForm(')
const editFormEnd = Math.min(...[...subsSourceFull.matchAll(/^function \w+\(/gm)]
  .map((m) => m.index)
  .filter((i) => i > editFormStart))
const editSource = subsSourceFull.slice(editFormStart, editFormEnd)
// NewSubscription is the last function in the file, so its slice runs to the end. Declared here
// rather than beside the first section that used it, because two later sections read from it too.
const newStart = subsSourceFull.indexOf('function NewSubscription(')
const newSource = subsSourceFull.slice(newStart)
const changeStart = subsSourceFull.indexOf('function ChangePlan(')
const changeEnd = Math.min(...[...subsSourceFull.matchAll(/^function \w+\(/gm)]
  .map((m) => m.index)
  .filter((i) => i > changeStart))
const changeSource = subsSourceFull.slice(changeStart, changeEnd)
const javaUpdate = javaService.slice(
  javaService.indexOf('public SubscriptionDetailResponse updateSubscription('),
  javaService.indexOf('//! Endpoint 16'))
const javaChangePlan = javaService.slice(
  javaService.indexOf('public SubscriptionDetailResponse changePlan('),
  javaService.indexOf('//! Endpoint 17'))
const javaRenew = javaService.slice(
  javaService.indexOf('public SubscriptionDetailResponse renewSubscription('),
  javaService.indexOf('//! Endpoint 27'))
const mirrorSource = subsSourceFull.slice(subsSourceFull.indexOf('function whyRenewWouldRefuse('),
  subsSourceFull.indexOf('edit the terms */'))
const renewDialogStart = subsSourceFull.indexOf('function RenewCustomPeriod(')
const renewDialogSource = subsSourceFull.slice(renewDialogStart,
  Math.min(...[...subsSourceFull.matchAll(/^function \w+\(/gm)]
    .map((m) => m.index).filter((i) => i > renewDialogStart)))

// A write is worth seeing whole: the form and the exact payload side by side before it goes, and
// the payload beside the reply after. So every modal takes the screen and prints the body it will
// send, recomputed as the fields are typed — reading it afterwards tells you what you sent, not
// what you are about to send.
console.log('\nEvery modal shows the request body it will send')
const kitSource = readFileSync('src/components/ui/Kit.jsx', 'utf8')
const responseSource = readFileSync('src/components/ResponseModal.jsx', 'utf8')
const modalFiles = sourceFiles('src/pages').map((f) => [f, readFileSync(f, 'utf8')])
const modalsWithout = []
for (const [f, src] of modalFiles) {
  for (const m of src.matchAll(/<Modal\b/g)) {
    // the props of that one element, up to the first `>` that closes the opening tag
    const props = src.slice(m.index, src.indexOf('\n    >', m.index) + 6)
    if (!props.includes('preview=')) modalsWithout.push(f.replace('src/pages/', ''))
  }
}
const splitChecks = [
  [`every modal previews its body (${modalsWithout.length ? modalsWithout.join(', ') : 'all do'})`,
    modalsWithout.length === 0],
  // The pane only tracks the fields if the body is built during render. A body assembled inside
  // submit() can only be shown after it has gone, which is the thing this replaces.
  ['the bodies are built at render, not inside submit',
    !/const submit = async \(\) => \{[\s\S]{0,400}const body = \{/.test(subsSourceFull)],
  ['Modal goes full-screen when it has a preview',
    kitSource.includes("data-split={preview === undefined ? 'false' : 'true'}")
      && kitSource.includes('className="modal-split"')],
  ['and the split is two columns that scroll separately',
    css.includes(".modal[data-split='true']")
      && /\.modal-split \{[^}]*grid-template-columns: minmax\(0, 1fr\) minmax\(0, 1fr\)/.test(css)
      && /\.modal-split > \* \{[^}]*overflow-y: auto/.test(css)],
  // An empty body is information too: a pane that vanished would read as a bug.
  ['an empty body still shows, marked empty',
    kitSource.includes('function isEmptyBody(')
      && kitSource.includes("data-empty={isEmptyBody(preview) ? 'true' : 'false'}")],
  // After the call: the request that went, beside the reply it got.
  ['the response modal is split the same way',
    responseSource.includes('previewLabel="Response body"')
      && responseSource.includes('<p className="modal-pane-label">Request body</p>')],
  ['it says so plainly when nothing was sent',
    responseSource.includes('This request sent no body.')],
  // DELETE has no body, and that pairing is exactly what the user needs to see.
  ['and it renders for every method, body or not',
    !/method === 'DELETE'/.test(responseSource)],
  ['it stacks to one column on a narrow screen',
    /@media \(max-width: 900px\)[\s\S]{0,200}\.modal-split \{ grid-template-columns: minmax\(0, 1fr\)/
      .test(css)],
]
// Rendered, not just matched. The patterns above prove the source says the right thing; these
// prove the browser is handed two panes, with the form in one and the JSON in the other.
const asModal = (props) => renderToString(
  React.createElement(Modal, { open: true, title: 'A write', onClose: () => {}, ...props },
    React.createElement('p', null, 'the form')))
const split = asModal({ preview: { reason: 'because', immediate: true } })
const plain = asModal({})
splitChecks.push(
  ['rendered: a preview really does open full-screen', split.includes('data-split="true"')],
  ['rendered: two panes, form on the left and the label on the right',
    split.includes('modal-split') && split.includes('the form')
      && split.includes('modal-pane-label')],
  ['rendered: the pane holds the body that will be sent',
    split.includes('&quot;reason&quot;: &quot;because&quot;')
      && split.includes('&quot;immediate&quot;: true')],
  // A read-only modal is still the old single-pane one. Splitting those would be noise.
  ['rendered: no preview leaves the plain modal alone',
    plain.includes('data-split="false"') && plain.includes('modal-body')
      && !plain.includes('modal-split')],
  ['rendered: an empty body shows an empty pane rather than none',
    asModal({ preview: {} }).includes('data-empty="true"')],
)
// WHICH REQUEST IS THIS? The title says what the modal is for; the heading strip says what it
// sends. In a tool for exercising an API that is the first thing worth knowing, and no modal may
// leave it out.
/** The body of every `endpoint={...}` prop in the screens, brace-matched so nesting is safe. */
const endpointProps = modalFiles.flatMap(([file, src]) => {
  const found = []
  for (const m of src.matchAll(/\bendpoint=\{/g)) {
    let depth = 0
    let i = m.index + m[0].length - 1
    for (; i < src.length; i++) {
      if (src[i] === '{') depth++
      else if (src[i] === '}' && --depth === 0) break
    }
    found.push({ file, body: src.slice(m.index, i + 1) })
  }
  return found
})
const headed = asModal({ endpoint: React.createElement('span', null, 'POST /platform/x') })
const modalsWithoutEndpoint = []
for (const [f, src] of modalFiles) {
  for (const m of src.matchAll(/<Modal\b/g)) {
    const props = src.slice(m.index, src.indexOf('\n    >', m.index) + 6)
    if (!props.includes('endpoint=')) modalsWithoutEndpoint.push(f.replace('src/pages/', ''))
  }
}
splitChecks.push(
  [`every modal names its endpoint (${modalsWithoutEndpoint.length
    ? modalsWithoutEndpoint.join(', ') : 'all do'})`, modalsWithoutEndpoint.length === 0],
  // The tags resolve {id} themselves, so the strip reads as the URL that will be sent rather
  // than as a template. A heading that spells its own path out can disagree with the call it
  // describes, and would be believed — so every one has to come from the registry.
  //
  // This asks whether the prop CONTAINS an EndpointTag, which is the whole invariant. Its first
  // version looked for a quoted "/platform/…" instead and did not bite when a hand-typed path
  // went in: JSX text carries no quotes.
  [`every heading comes from the registry (${endpointProps.length} props)`,
    endpointProps.length >= 15
      && endpointProps.every(({ body }) => body.includes('<EndpointTag'))],
  ['rendered: the heading strip is there', headed.includes('modal-endpoint')
    && headed.includes('POST /platform/x')],
  ['the response modal heading carries the URL that was really called',
    responseSource.includes('result?.request?.url || path')
      && responseSource.includes('className="endpoint-tag-method"')],
  // It was under the request body; two copies of one URL is one too many.
  ['and no longer repeats it under the request body',
    !/className="muted"[\s\S]{0,80}request\?\.url/.test(responseSource)],
)

// FULL SCREEN, LESS A MARGIN, AND ACTUALLY SCROLLING. The row has to be pinned to the container:
// an `auto` row grows to its content, so `height: 100%` measures the grown row, nothing overflows,
// nothing scrolls, and a long response runs off the bottom taking the Submit button with it.
splitChecks.push(
  ['the card is held to the screen, so the panes scroll instead of running off it',
    /\.modal\[data-split='true'\] \{[^}]*grid-template-rows: minmax\(0, 1fr\)/.test(css)
      && /\.modal\[data-split='true'\] \.modal-card \{[^}]*max-height: 100%/.test(css)],
  ['it keeps a margin off the edges of the display',
    /\.modal\[data-split='true'\] \{[^}]*padding: 16px/.test(css)],
  ['the heading and the actions never scroll away',
    /\.modal-head \{[^}]*flex: 0 0 auto/.test(css) && /\.modal-foot \{[^}]*flex: 0 0 auto/.test(css)],
  // A 400-line response must not carry the word telling you what you are reading off the top.
  ['the response body scrolls under a label that stays put',
    /\.modal-pane-preview > \.modal-json \{[^}]*overflow-y: auto/.test(css)
      && /\.modal-pane-preview > \.modal-pane-label,\s*\n\.modal-pane-preview > \.resp-head \{[^}]*flex: 0 0 auto/
        .test(css)],
  ['rendered: the preview pane is the one that pins its label', split.includes('modal-pane-preview')],
  // Every token these rules name has to exist, or the pane silently loses its background.
  ['every colour the modal asks for is a real token', (() => {
    const defined = new Set([...readFileSync('src/styles/tokens.css', 'utf8')
      .matchAll(/^\s*(--[a-z0-9-]+):/gm)].map((m) => m[1]))
    const asked = [...css.matchAll(/\.modal[^{]*\{([^}]*)\}/g)]
      .flatMap((block) => [...block[1].matchAll(/var\((--[a-z0-9-]+)(,|\))/g)]
        .filter((v) => v[2] === ')').map((v) => v[1]))
    const unknown = [...new Set(asked)].filter((v) => !defined.has(v))
    if (unknown.length) console.log(`         undefined: ${unknown.join(', ')}`)
    return unknown.length === 0
  })()],
)

for (const [label, ok] of splitChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// THIS IS AN API TESTING TOOL, so every request has to be reachable — including the ones the API
// will refuse. A disabled button is the one thing that makes a refusal untestable, so nothing in
// src may gate a button or lock an input on state. The screens still work out and SHOW every
// condition; they just do not act on it.
console.log('\nNothing is disabled: every request stays reachable')
const uiFiles = sourceFiles('src')
const gated = uiFiles
  .filter((f) => !f.endsWith('Kit.jsx'))
  .map((f) => [f, readFileSync(f, 'utf8')])
  .filter(([, body]) => /disabled=|readOnly/.test(body))
  .map(([f]) => f.replace('src/', ''))
const kit = readFileSync('src/components/ui/Kit.jsx', 'utf8')
const reachableChecks = [
  [`no screen gates a button or input (${gated.length ? gated.join(', ') : 'none do'})`,
    gated.length === 0],
  // The Button component keeps its own in-flight guard: that stops a double-click firing the
  // same write twice, which is an accident rather than a test.
  ['the Button component keeps only its in-flight guard',
    kit.includes('disabled={disabled || busy}')],
  // The value of the refusal mirrors is the explanation, not the blocking — so they must survive.
  ['the refusal mirrors are all still there',
    ['whyEditWouldRefuse', 'whyRenewWouldRefuse', 'whyEndWouldRefuse', 'suspendOrResumeAction']
      .every((fn) => subsSourceFull.includes(`function ${fn}(`))],
  ['and each still reaches the screen',
    ['editRefusal', 'renewRefusal', 'pauseAction.refusal']
      .every((v) => subsSourceFull.includes(`{${v}`))
      && subsSourceFull.includes('whyEndWouldRefuse(s)')],
]
for (const [label, ok] of reachableChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

const formChecks = [
  ['the boxes are grouped under headings',
    (editSource.match(/className="field-split"/g) || []).length >= 4],
  // cancelledAt and cancellationReason left the model on 2026-09-07. Nothing should offer to
  // edit them, and nothing should read them back.
  ['there is no cancellation to edit any more',
    !editSource.includes('showCancellation') && !editSource.includes('cancelledAt')
      && !editSource.includes('cancellationReason')],
  // The reason is stored on the subscription now, not only logged, and the API requires it.
  ['the reason says it is stored', editSource.includes('Saved as reasonForChanges')],
  ['the reason box is marked required', /label="Reason for these changes"\s*\n\s*required/.test(editSource)],
  // Asked for only once there is something to explain — a reason demanded before any box has
  // moved reads as a nag.
  ['it is only demanded once something has changed',
    editSource.includes('const reasonMissing = !nothingChanged && !form.reason.trim()')],
  ['and the button says so rather than just refusing', editSource.includes("'Say why first'")],
  // Two dates, two calendars: the period's two ends. Counted inside the edit form only.
  ['every date is a calendar, not a typed instant',
    (editSource.match(/type="date"/g) || []).length === 2
      && !editSource.includes('An ISO instant')],
  // Declared at module level, so read from the whole file rather than the EditForm slice.
  ['each status says what choosing it means', subsSourceFull.includes('const STATUS_MEANS')],
  // The plan and the money have their own endpoints (#16, #25, #26). No boxes for them, and the
  // form says so rather than leaving it to be noticed.
  ['there is no box for the plan or the money',
    !editSource.includes("set('contractedPrice')") && !editSource.includes("set('currencyCode')")
      && !editSource.includes("set('billingCustomerReference')")
      && !editSource.includes("set('planKey')")],
  ['and the form says where they went',
    editSource.includes('The plan and the money are not editable here')],
  ['the box marks the one it is on now', editSource.includes("' — as it stands'")],
  // Both refusals are worked out from the boxes: a refused request loses the other twelve
  // boxes somebody just filled in.
  ['a backwards period is caught before it is sent', editSource.includes('periodBackwards')],
  // Zero is legal now — it is how an override is removed — so only a negative is a refusal.
  ['a negative override is caught, and zero is not',
    editSource.includes('negativeOverride') && !editSource.includes('zeroOverride')
      && editSource.includes("min=\"0\"")],
  // The send button is NEVER disabled — every request has to be reachable — so what matters is
  // that each condition is still WORKED OUT and shown, rather than silently gating the button.
  ['the four conditions are still worked out and shown',
    ['nothingChanged', 'reasonMissing', 'periodBackwards', 'negativeOverride']
      .every((guard) => editSource.includes(guard))],
]
for (const [label, ok] of formChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// Moving a CANCELLED or EXPIRED subscription onto a plan is how a school comes back. #16 used to
// refuse it, on the grounds that there was "nothing to move" — which mistook what it does: it
// retires the row it is given and opens a new one.
console.log('\nEvery status may change plan, and the new row is always ACTIVE')
const reviveChecks = [
  ['no status is refused any more',
    !javaService.includes('SUBSCRIPTION_NOT_CHANGEABLE')],
  ['a finished row is still recognised, for the note alone',
    javaChangePlan.includes('boolean revivingFinished = subscription.getStatus() == SubscriptionStatus.CANCELLED')],
  // EVERY status comes out ACTIVE, not just the revivals: a plan change is somebody buying this
  // school a plan, so the row it lands on has to be one the school can use.
  ['the new row always starts ACTIVE, whatever the old one was',
    javaChangePlan.includes('.status(SubscriptionStatus.ACTIVE)')
      && !javaChangePlan.includes('.status(subscription.getStatus())')
      && !javaChangePlan.includes('revivingFinished ? SubscriptionStatus.ACTIVE')],
  ['and it agrees with what the endpoint does to the school',
    javaChangePlan.includes('school.setStatus(SchoolStatus.ACTIVE)')],
  // autoRenew and the closed row's dates still turn on whether the old row had FINISHED — both
  // are about undoing what a cancellation did, not about what the new row is.
  // autoRenew is carried across untouched now, with no revival exception — so a revived
  // subscription inherits the false #21 set, and the note has to say so rather than claim
  // otherwise. That note was left claiming the opposite when the exception was removed.
  ['autoRenew is carried across untouched, with no exception',
    javaChangePlan.includes('.autoRenew(request.autoRenew() == null')
      && !javaChangePlan.includes('revivingFinished ? Boolean.TRUE')],
  ['and the note reports it rather than claiming it was turned back on',
    javaChangePlan.includes('autoRenew is still off')
      && !javaChangePlan.includes('autoRenew is back on')],
  // The closed row really did stop when it was cancelled.
  // A CLOSED PERIOD ONLY EVER SHRINKS, and the dates decide that — not the status. Keying on
  // the status got the middle case wrong: a scheduled cancellation is CANCELLED with an end
  // still in the future, so it really was serving until the handover and does need trimming.
  // Skipping it left two rows claiming the same days.
  ['the closed row is never stretched forward',
    javaChangePlan.includes('if (previousPeriodEnd == null || previousPeriodEnd.isAfter(periodStart)) {')
      && javaChangePlan.includes('subscription.setCurrentPeriodEnd(periodStart);')],
  ['and that decision no longer keys on the status',
    !/revivingFinished\) \{\s*\n\s*subscription\.setCurrentPeriodEnd/.test(javaChangePlan)],
  // The history row is the only place a plan change moves the status.
  ['the history row records the status move',
    javaChangePlan.includes('.previousStatus(previousSubscriptionStatus)')
      && !javaChangePlan.includes('.previousStatus(saved.getStatus())')],
  ['the note names the gap the school was on nothing for',
    javaService.includes('is time this school was on nothing')],
]
for (const [label, ok] of reviveChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// #14 may only edit a TRIAL or ACTIVE subscription. The other four each have an endpoint that
// owns the way out of them, and editing a field would bypass it — the history row would say
// "edited" where the real event was "resumed" or "revived".
console.log('\nOnly a TRIAL or ACTIVE subscription may be edited')
const editGateChecks = [
  ['the service refuses the other four',
    javaUpdate.includes('previousStatus != SubscriptionStatus.TRIAL')
      && javaUpdate.includes('previousStatus != SubscriptionStatus.ACTIVE')
      && javaUpdate.includes('SUBSCRIPTION_NOT_EDITABLE')],
  // A refusal that does not say where to go instead is a dead end.
  ['and names the way out of each',
    ['case SUSPENDED ->', 'case PAST_DUE ->', 'case CANCELLED, EXPIRED ->']
      .every((arm) => javaUpdate.includes(arm))],
  ['it refuses before anything is applied',
    javaUpdate.indexOf('SUBSCRIPTION_NOT_EDITABLE')
      < javaUpdate.indexOf('applySubscriptionEdits(subscription, request)')],
  // The override still has to be able to SET those statuses, or PAST_DUE and EXPIRED become
  // unreachable while no job exists.
  ['but the status field still accepts all six',
    !javaUpdate.includes('request.status() != SubscriptionStatus')
      && editRequestSource.includes('Any of the six')],
  ['the card says why, without blocking the attempt',
    subsSourceFull.includes('function whyEditWouldRefuse(')
      && subsSourceFull.includes('{editRefusal')],
  ['and it names the same four statuses',
    ['PAST_DUE:', 'SUSPENDED:', 'CANCELLED:', 'EXPIRED:']
      .every((k) => new RegExp(`whyEditWouldRefuse[\\s\\S]{0,700}${k}`).test(subsSourceFull))],
]
for (const [label, ok] of editGateChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// #18 and #22 were both dropped because moving INTO PAST_DUE and EXPIRED is the passage of time
// noticing something, not a decision — so a job will do them. The docs must not promise an
// endpoint that is never coming, and the enum is where somebody looks first.
console.log('\nPAST_DUE and EXPIRED are job territory, not endpoints')
const jobChecks = [
  ['the README marks #22 not being built',
    /<a id="t22"><\/a>\[~~22~~\]\(#e22\) \*\*not being built\*\*/.test(plansReadme)],
  ['and says a job will do it',
    /id="e22"[\s\S]{0,900}A job will do it/.test(plansReadme)],
  ['it names both statuses as job territory',
    /id="e22"[\s\S]{0,2500}`PAST_DUE`[\s\S]{0,400}`EXPIRED`/.test(plansReadme)],
  // The enum is where somebody looks to find out how a status is reached.
  // One javadoc each, naming the endpoint that was dropped for it — which is what tells somebody
  // reading the enum that the gap is deliberate rather than unfinished.
  ['the enum says a job sets PAST_DUE, citing #18',
    statusEnum.includes('#18 (mark-past-due)')
      && statusEnum.slice(0, statusEnum.indexOf('PAST_DUE,'))
        .includes('A JOB will move a subscription into this')],
  ['and a job sets EXPIRED, citing #22',
    statusEnum.includes('#22 (expire)')
      && (statusEnum.match(/A JOB will move a subscription into this/g) || []).length === 2],
  ['the enum still says PAST_DUE grants everything',
    statusEnum.includes('It still <b>grants</b> everything meanwhile')],
  // Nothing should still promise #22.
  ['nothing claims #22 is coming',
    !javaService.includes("#22's job") && !javaService.includes('#22 is not built')
      && !editRequestSource.includes('#22 is not built')],
  ['and the honest gap is still stated where it matters',
    javaService.includes('A job will close these')
      && javaService.includes('a date arriving rather than a decision')],
]
for (const [label, ok] of jobChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// #21 ends the subscription. The point of the design is that no field was added for "cancelled
// but still running": the status says cancelled, and the period says how long the access lasts.
console.log('\nEnding a subscription')
const endChecks = [
  ['no field was added to the model for it',
    !javaService.includes('cancelAtPeriodEnd')
      && !readFileSync('../backend/src/main/java/com/orbitastra/backend/models/plans/SchoolSubscription.java',
        'utf8').includes('cancelAtPeriodEnd')],
  ['the status goes CANCELLED either way',
    /cancelSubscription[\s\S]{0,6000}subscription\.setStatus\(SubscriptionStatus\.CANCELLED\);/
      .test(javaService)],
  // Both facts, without a regex spanning the comment between them — a `//` inside a regex
  // literal ends the literal, which is how a first version of this check failed to parse.
  ['and only the immediate shape trims the period',
    javaService.includes('subscription.setCurrentPeriodEnd(Instant.now());')
      && (javaService.match(/setCurrentPeriodEnd\(Instant\.now\(\)\)/g) || []).length === 1
      && javaService.includes('if (request.isImmediate()) {')],
  // The gate is what makes "keeps working" true, and it is in a different service.
  ['a cancelled subscription still grants until its period ends',
    /status == SubscriptionStatus\.CANCELLED\) \{[\s\S]{0,400}cancelledEnd\.isAfter\(Instant\.now\(\)\)/
      .test(schoolSubUtils)],
  ['autoRenew is turned off with it',
    /cancelSubscription[\s\S]{0,6000}setAutoRenew\(Boolean\.FALSE\)/.test(javaService)],
  ['the school is not touched',
    !/cancelSubscription[\s\S]{0,6000}school\.setStatus\(/.test(javaService)],
  ['a CLOSED school can still cancel; only a deleted one cannot',
    /cancelSubscription[\s\S]{0,2000}SchoolStatus\.DELETED[\s\S]{0,200}DELETION_PENDING/
      .test(javaService)
      && !/cancelSubscription[\s\S]{0,2000}SchoolStatus\.CLOSED/.test(javaService)],
  ['escalating a scheduled cancellation is allowed',
    javaService.includes('CANCELLATION_ALREADY_SCHEDULED')
      && /if \(!request\.isImmediate\(\)\) \{[\s\S]{0,300}CANCELLATION_ALREADY_SCHEDULED/
        .test(javaService)],
  // The immediate shape trims currentPeriodEnd to now, so the date the school had actually paid
  // for survives only in the history row's reason. Asserted on the reason carrying paidUntil at
  // all, not on how it is concatenated — it used to pin `" + paidUntil`, which broke the moment
  // the date started going through the readable-date helper.
  ['the history row keeps the date the immediate shape overwrites',
    javaService.includes('The period paid ')
      && /\.reason\(\(request\.isImmediate\(\)[\s\S]{0,400}paidUntil/.test(javaService)],
  // The screen side.
  ['the card offers it on its own row',
    subsSourceFull.includes('function whyEndWouldRefuse(')
      && subsSourceFull.includes('id="cancel-subscription"')],
  ['it refuses only what is genuinely over',
    /whyEndWouldRefuse[\s\S]{0,500}s\.status === 'EXPIRED'/.test(subsSourceFull)
      && /whyEndWouldRefuse[\s\S]{0,600}s\.status === 'CANCELLED' && s\.periodEnded/
        .test(subsSourceFull)],
  ['the dialog offers both shapes',
    subsSourceFull.includes('function EndSubscription(')
      && subsSourceFull.includes('Stop the access today')],
  ['and only escalation for one already ending',
    subsSourceFull.includes("const escalatingOnly = subscription.status === 'CANCELLED'")
      && subsSourceFull.includes('{escalatingOnly ? null : (')],
  ['it says the status turning CANCELLED is not a mistake',
    subsSourceFull.includes('that is not a mistake')],
  ['and that nothing undoes it',
    subsSourceFull.includes('Nothing undoes this')],
]
for (const [label, ok] of endChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// #19 and #20 are one transition each, with a required reason. A subscription is either
// suspendable or resumable and never both, so the card shows ONE button and the dialog serves
// both endpoints — two nearly identical dialogs would differ only in their heading.
console.log('\nCutting a school off, and switching it back on')
const pauseChecks = [
  ['the service moves both documents on suspend',
    /suspendSubscription[\s\S]{0,4000}setStatus\(SubscriptionStatus\.SUSPENDED\)/.test(javaService)
      && /suspendSubscription[\s\S]{0,4000}school\.setStatus\(SchoolStatus\.SUSPENDED\)/
        .test(javaService)],
  ['and both back on resume',
    /resumeSubscription[\s\S]{0,4000}setStatus\(SubscriptionStatus\.ACTIVE\)/.test(javaService)
      && /resumeSubscription[\s\S]{0,4000}school\.setStatus\(SchoolStatus\.ACTIVE\)/
        .test(javaService)],
  ['suspend takes ACTIVE and PAST_DUE only',
    /previousStatus != SubscriptionStatus\.ACTIVE\s*\n\s*&& previousStatus != SubscriptionStatus\.PAST_DUE/
      .test(javaService)],
  ['resume takes SUSPENDED only',
    javaService.includes('previousStatus != SubscriptionStatus.SUSPENDED')],
  // Refusing a trial is what lets resume go straight to ACTIVE with no lookup.
  ['a trial is refused with its own explanation',
    /case TRIAL -> "A trial has no unpaid bill behind it/.test(javaService)],
  ['neither writes a second row',
    !/suspendSubscription[\s\S]{0,4000}SchoolSubscription\.builder\(\)/.test(javaService)
      && !/resumeSubscription[\s\S]{0,4000}SchoolSubscription\.builder\(\)/.test(javaService)],
  ['resume leaves suspendedAt standing',
    !/resumeSubscription[\s\S]{0,4000}setSuspendedAt/.test(javaService)],
  ['and does not touch either period date',
    !/resumeSubscription[\s\S]{0,4000}setCurrentPeriod/.test(javaService)],
  // The screen side.
  ['the card shows one button, not two',
    subsSourceFull.includes('function suspendOrResumeAction(')
      && subsSourceFull.includes('const pauseAction = suspendOrResumeAction(s)')],
  ['it picks resume only for a SUSPENDED subscription',
    /suspendOrResumeAction[\s\S]{0,400}if \(s\.status === 'SUSPENDED'\)[\s\S]{0,200}resume-subscription/
      .test(subsSourceFull)],
  ['and explains why it cannot when neither applies',
    /Cannot cut off a trial/.test(subsSourceFull)
      && /Nothing to cut off/.test(subsSourceFull)],
  ['the dialog serves both endpoints',
    subsSourceFull.includes('function SuspendOrResume(')
      && /onSend\(action\.endpoint, reason\)/.test(subsSourceFull)],
  ['it still says a reason is required',
    subsSourceFull.includes("'Say why first'")],
  ['it warns that suspending blocks the tenant',
    subsSourceFull.includes('This stops the school working')
      && subsSourceFull.includes('SCHOOL_NOT_EDITABLE')],
  ['and that resuming does not extend the period',
    subsSourceFull.includes('The period is not extended')],
  // #14 can still write the status; it must not be presented as the same thing.
  ['#14 says writing SUSPENDED there is not #19',
    editRequestSource.includes('is <b>not</b> #19')],
]
for (const [label, ok] of pauseChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// The subscription card's actions: one row per action, each with the endpoint it calls beside it.
// They used to share a row with all three tags collected at the right, which wrapped onto a
// second line and left the reader pairing buttons to endpoints by position.
console.log('\nEach action sits on its own row with its endpoint')
const actionRows = [...subsSourceFull.matchAll(/<div className="toolbar">([\s\S]*?)<\/div>/g)]
  .map((m) => m[1])
  .filter((row) => /<EndpointTag/.test(row) && /<Button/.test(row))
const cardRows = actionRows.filter((row) => /(onEdit|onChangePlan|onRenew)/.test(row))
const rowChecks = [
  ['there are three action rows', cardRows.length === 3],
  ['each holds exactly one button',
    cardRows.every((row) => (row.match(/<Button/g) || []).length === 1)],
  ['and exactly one endpoint tag',
    cardRows.every((row) => (row.match(/<EndpointTag/g) || []).length === 1)],
  ['the pairing is right in each',
    /onEdit[\s\S]{0,500}id="edit-subscription"/.test(subsSourceFull)
      && /onChangePlan[\s\S]{0,500}id="change-plan"/.test(subsSourceFull)
      && /onRenew[\s\S]{0,700}id="renew-subscription"/.test(subsSourceFull)],
  ['the tag is pushed to the right of its own row',
    cardRows.every((row) => /toolbar-spacer[\s\S]*<EndpointTag/.test(row))],
  // Each row says what its action does, rather than one hint serving all three.
  ['each row says what its action does',
    cardRows.every((row) => /className="muted"/.test(row))],
]
for (const [label, ok] of rowChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// #16 got the same three controls as #13 and #14: a cadence from the new plan, a required start,
// and an end that is only the caller's to give on CUSTOM.
console.log('\nThe plan-change form names its period too')
const changePeriodChecks = [
  ['#16 requires a start, as a validation constraint',
    /@NotNull Instant currentPeriodStart/.test(changeRequestSource)],
  ['and takes a cadence, absent meaning the new plan\'s',
    changeRequestSource.includes('BillingCycle billingCycle')
      && javaService.includes('BillingCycle billingCycle = request.billingCycle() == null')],
  // Asserted on the ARGUMENTS rather than the line-wrapping: pinning the wrap broke as soon as
  // the helper gained its zone parameter, and the wrap was never the point.
  ['the service derives the period from the cadence being moved onto',
    /calculateSubscriptionPeriodEnd\(request\.currentPeriodEnd\(\),\s*\n?\s*periodStart,\s*billingCycle,/
      .test(javaService)],
  // ONE CADENCE RULE, IN ONE PLACE. #16 used to carry its own CUSTOM check beside the call; the
  // helper now decides for #13, #14, #16 and #17 alike, so the inline one was dead code that
  // read as live. What matters is that both halves of the rule live in the helper.
  ['the helper requires an end on CUSTOM and refuses one on every other cadence',
    /if \(cycle == BillingCycle\.CUSTOM\) \{[\s\S]{0,400}BILLING_PERIOD_END_REQUIRED/
      .test(javaService)
      && /if \(requested != null\) \{\s*\n\s*throw ApiException\.badRequest\("BILLING_PERIOD_END_NOT_ALLOWED"/
        .test(javaService)],
  // A fixed cadence must never be asked for a length it does not have, and CUSTOM has none.
  ['and CUSTOM never reaches the day table',
    /case CUSTOM -> throw new IllegalStateException/.test(javaService)],
  // The row being left ends where the new one begins, in both directions.
  ['the closed row ends where the new period starts',
    javaService.includes('subscription.setCurrentPeriodEnd(periodStart)')],
  ['the modal offers the cadence, filled from the plan',
    changeSource.includes('label="Billing cycle"')
      && /const choosePlan[\s\S]{0,900}setCycle\(plan \? plan\.billingCycle : ''\)/
        .test(changeSource)],
  ['it reads the cadence from the box, not the plan',
    changeSource.includes("const soldCycle = cycle || chosen?.billingCycle || ''")
      && changeSource.includes("const needsPeriodEnd = soldCycle === 'CUSTOM'")
      && !changeSource.includes("const needsPeriodEnd = chosen?.billingCycle === 'CUSTOM'")],
  ['the end is editable on every cadence',
    changeSource.includes('value={periodEnd}') && !/readOnly|disabled=/.test(changeSource)],
  ['the start is sent in the school\'s zone, and can be changed',
    changeSource.includes('out.currentPeriodStart = startOfDayInZone(startsOn, timeZone)')
      && changeSource.includes('const startsOn = startDay || todayInZone(timeZone)')],
  ['and the cadence only when it differs from the plan',
    changeSource.includes("if (cycle && chosen && cycle !== chosen.billingCycle) out.billingCycle = cycle")],
]
for (const [label, ok] of changePeriodChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// A day begins at a different instant in every zone, and #13 now needs that instant rather than
// a date. Behaviour, not source: these are the conversions the sale form depends on.
console.log('\nA day begins when the school says it does')
const zoneCases = [
  ['Asia/Kolkata is +05:30', startOfDayInZone('2026-09-08', 'Asia/Kolkata'), '2026-09-07T18:30:00Z'],
  ['UTC is midnight', startOfDayInZone('2026-09-08', 'UTC'), '2026-09-08T00:00:00Z'],
  // The case a naive `${day}T00:00:00Z` gets refused for: 00:00Z is still the 7th in New York.
  ['America/New_York in summer is -04:00',
    startOfDayInZone('2026-09-08', 'America/New_York'), '2026-09-08T04:00:00Z'],
  ['and -05:00 in winter, so DST is handled',
    startOfDayInZone('2026-01-15', 'America/New_York'), '2026-01-15T05:00:00Z'],
  ['Australia/Sydney is +10:00', startOfDayInZone('2026-09-08', 'Australia/Sydney'),
    '2026-09-07T14:00:00Z'],
  ['Pacific/Kiritimati is +14:00, the furthest ahead',
    startOfDayInZone('2026-09-08', 'Pacific/Kiritimati'), '2026-09-07T10:00:00Z'],
  ['no zone falls back to UTC midnight',
    startOfDayInZone('2026-09-08', undefined), '2026-09-08T00:00:00Z'],
  ['and an unusable one does too, rather than throwing',
    startOfDayInZone('2026-09-08', 'Not/AZone'), '2026-09-08T00:00:00Z'],
]
for (const [label, got, want] of zoneCases) {
  const ok = got === want
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label} — ${got} != ${want}`)
  if (!ok) fail++
}

// The cadence names the dates it needs. #13 requires currentPeriodStart on every cycle, and both
// endpoints require currentPeriodEnd whenever the cadence is CUSTOM.
console.log('\nThe cadence names the dates it needs')
const requiredDateChecks = [
  ['#13 requires a start, as a validation constraint',
    /@NotNull Instant currentPeriodStart/.test(createRequestSource)],
  // Named a helper that no longer exists, so it could not fail. What actually holds the line is
  // the field being @NotNull and the service reading it straight through.
  ['and the service no longer defaults it to today',
    javaService.includes('Instant periodStart = request.currentPeriodStart();')
      && !/request\.currentPeriodStart\(\) == null\s*\n?\s*\?/.test(javaService)],
  ['#14 requires a start whenever a cadence is sent',
    /if \(request\.billingCycle\(\) != null\) \{[\s\S]{0,400}PERIOD_START_REQUIRED/
      .test(javaService)],
  ['keyed on the cycle being sent, not on it changing',
    javaService.includes('if (request.billingCycle() != null) {')
      && /Keyed on the cycle being SENT rather than on it changing/.test(javaService)],
  // The four fixed cadences derive their end, so only CUSTOM needs one.
  ['only CUSTOM also requires an end',
    (javaService.match(/BILLING_PERIOD_END_REQUIRED/g) || []).length >= 2
      && javaService.includes('request.billingCycle() == BillingCycle.CUSTOM')],
  // The sale form has to send the start now, and send the right instant for the school's zone.
  ['the sale form sends the start it is now required to',
    newSource.includes('out.currentPeriodStart = startOfDayInZone(startDay || todayInZone(timeZone), timeZone)')],
  ['it uses the school\'s zone, not UTC midnight',
    newSource.includes('todayInZone(timeZone)')
      && !newSource.includes('body.currentPeriodStart = startOfDay(')],
  ['and the page loads the school to get that zone',
    subsSourceFull.includes("call('get-school', {")
      && subsSourceFull.includes('timeZone={school?.defaultTimeZone}')],
]
// ONLY A CUSTOM CADENCE TAKES AN END DATE, and the four fixed ones refuse rather than honour
// one. Those four ARE their length: a date sent with one either agrees with the derivation, in
// which case it said nothing, or disagrees with it — and then the record contradicts itself.
// Refused rather than quietly dropped, because answering 200 with a different date than the one
// sent is ignoring the caller without telling them.
console.log('\nOnly CUSTOM takes an end date')
const notAllowedChecks = [
  ['the helper refuses one on a fixed cadence',
    /if \(requested != null\) \{\s*\n\s*throw ApiException\.badRequest\("BILLING_PERIOD_END_NOT_ALLOWED"/
      .test(javaService)],
  // The message has to name the field, the cadence and the way round it, or the refusal is a
  // dead end for whoever hits it. EVERY one of them, not just the first: there are two throws —
  // the helper's and #14's — and a window anchored on the first found the helper's text and
  // passed while #14's message had been gutted.
  [(() => {
    const thrown = [...javaService.matchAll(/BILLING_PERIOD_END_NOT_ALLOWED"/g)]
    const vague = thrown.filter((m) => !javaService.slice(m.index, m.index + 700)
      .includes('currentPeriodStart')).length
    return `each refusal names the cadence and what to send instead (${thrown.length} throws, `
      + `${vague} vague)`
  })(), (() => {
    const thrown = [...javaService.matchAll(/BILLING_PERIOD_END_NOT_ALLOWED"/g)]
    return thrown.length === 2 && thrown.every((m) => {
      const window = javaService.slice(m.index, m.index + 700)
      return window.includes('currentPeriodStart') && window.includes('CUSTOM')
    })
  })()],
  // #14 writes fields straight onto the document, so it needs its own guard before it does.
  ['#14 guards it against the cadence AFTER the edit, not the stored one',
    javaService.includes('BillingCycle cadenceAfterEdit = request.billingCycle() == null')
      && /cadenceAfterEdit != BillingCycle\.CUSTOM/.test(javaService)],
  ['and not only when a cadence was sent, so a bare end date is refused too',
    /if \(request\.currentPeriodEnd\(\) != null && cadenceAfterEdit != BillingCycle\.CUSTOM\)/
      .test(javaService)],
  // Every DTO that takes the field has to say the rule, or a caller reads the old promise.
  ['every request DTO says only CUSTOM takes one', [
    ['create', createRequestSource], ['update', editRequestSource],
    ['change-plan', changeRequestSource], ['renew', renewRequestSource],
  ].every(([, src]) => src.includes('BILLING_PERIOD_END_NOT_ALLOWED')
    || src.includes('ONLY A CUSTOM CADENCE TAKES AN END DATE'))],
  // The old promise was the opposite, so its words must be gone everywhere.
  ['and none of them still promises an override', ![
    createRequestSource, editRequestSource, changeRequestSource, renewRequestSource,
    javaService,
  ].some((src) => /always wins|optional override|overrides the derived/.test(src))],
  // The catalogue both apps read is documentation the user acts on.
  ['the endpoint catalogue lists the new refusal',
    (endpointsSource.match(/BILLING_PERIOD_END_NOT_ALLOWED/g) || []).length >= 4],
  ['and no longer says an explicit end wins',
    !/always wins|an optional override/.test(endpointsSource)],
  // The box stays typeable on every cadence — that is how the refusal gets tested — but it must
  // start empty, or an ordinary fixed-cadence sale would 400 on its own.
  ['the sale form starts the end box empty, so nothing trips by accident',
    (newSource.match(/const \[periodEnd, setPeriodEnd\] = useState\(''\)/g) || []).length === 1],
  ['and the edit form only sends it when it was actually changed',
    editBodySource.includes('form.currentPeriodEnd !== stored.currentPeriodEnd')],
]
for (const [label, ok] of notAllowedChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

for (const [label, ok] of requiredDateChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// A billing period starts today or later, on both endpoints. Nothing here can invoice a period
// that has already run, so a backdated start would sit in the record as a figure no process
// could act on.
console.log('\nA billing period cannot start in the past')
// THE HELPER CONVENTION, ENFORCED RATHER THAN REMEMBERED. Helpers in this service are flat: the
// main endpoint methods call them, and no helper calls another. A chain of helpers is what turns
// "what does this endpoint do" into a trail to follow, and each one's `Used by:` block stops being
// the whole answer.
//
// Checked because I have broken it twice: once by extracting logic that had a single caller, and
// once by pulling a day table out of calculateSubscriptionPeriodEnd into a second helper it then
// called. Both read fine in isolation; neither survives the rule.
// EVERY DTO LIVES IN request/ OR response/. One folder held both, which reads fine at five files
// and stops reading at thirteen: the thing somebody wants when they open a DTO folder is "what
// does this endpoint accept" or "what does it answer", and a flat alphabetical list interleaves
// the two. The split is packaging only — no endpoint's behaviour depends on it — so the guard is
// that the layout holds, and that no file's package line disagrees with the folder it sits in.
console.log('\nEvery DTO is filed as a request or a response')
const dtoRoot = '../backend/src/main/java/com/orbitastra/backend/dto'
const dtoFiles = (function walk(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const full = join(dir, e.name)
    return e.isDirectory() ? walk(full) : (e.name.endsWith('.java') ? [full] : [])
  })
})(dtoRoot)
// A DTO sitting directly in a module folder rather than in request/ or response/.
const unfiled = dtoFiles
  .filter((f) => !/\/(request|response)\/[^/]+\.java$/.test(f))
  .map((f) => f.replace(dtoRoot + '/', ''))
// The package line has to name the folder, or the move compiled by luck rather than correctness.
const mispackaged = dtoFiles.filter((f) => {
  const want = 'package com.orbitastra.backend.dto.'
    + f.replace(dtoRoot + '/', '').split('/').slice(0, -1).join('.') + ';'
  return !readFileSync(f, 'utf8').startsWith(want)
}).map((f) => f.replace(dtoRoot + '/', ''))
// Named *Request in response/, or *Response in request/ — filed on the wrong side.
const misfiled = dtoFiles.filter((f) => {
  const side = f.includes('/request/') ? 'request' : 'response'
  const name = f.split('/').pop().replace('.java', '')
  if (name.endsWith('Request')) return side !== 'request'
  if (name.endsWith('Response')) return side !== 'response'
  return false
}).map((f) => f.replace(dtoRoot + '/', ''))
const dtoLayoutChecks = [
  [`all ${dtoFiles.length} DTOs are in request/ or response/${unfiled.length ? ': ' + unfiled.join(', ') : ''}`,
    dtoFiles.length > 40 && unfiled.length === 0],
  [`every package line names its folder${mispackaged.length ? ': ' + mispackaged.join(', ') : ''}`,
    mispackaged.length === 0],
  [`nothing is filed on the wrong side${misfiled.length ? ': ' + misfiled.join(', ') : ''}`,
    misfiled.length === 0],
  // Both surfaces of every module, so a module that split only its requests is caught.
  [(() => {
    const mods = [...new Set(dtoFiles.map((f) => f.replace(dtoRoot + '/', '')
      .split('/').slice(0, -2).join('/')))].sort()
    return `both folders exist in every module (${mods.join(', ')})`
  })(), (() => {
    const mods = [...new Set(dtoFiles.map((f) => f.replace(dtoRoot + '/', '')
      .split('/').slice(0, -2).join('/')))]
    return mods.length >= 5 && mods.every((m) =>
      dtoFiles.some((f) => f.includes(`/${m}/request/`))
        && dtoFiles.some((f) => f.includes(`/${m}/response/`)))
  })()],
  // Nothing may still name a flat DTO package: that would compile only while a duplicate class
  // survived somewhere, and would break the moment it did not.
  [(() => {
    const stale = javaSources.filter((src) =>
      /import com\.orbitastra\.backend\.dto\.(?:core\.(?:academicyear|platform|profile)|plans\.(?:catalogue|subscription))\.[A-Z]/
        .test(src)).length
    return `no import names a flat DTO package (${stale} do)`
  })(), javaSources.every((src) =>
    !/import com\.orbitastra\.backend\.dto\.(?:core\.(?:academicyear|platform|profile)|plans\.(?:catalogue|subscription))\.[A-Z]/
      .test(src))],
]
for (const [label, ok] of dtoLayoutChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// A DATE IN A MESSAGE IS READ BY A PERSON. Fields stay ISO-8601 because a program parses them;
// messages spell the date out, because "2027-10-08T23:59:59Z" in the middle of a sentence is
// something nobody reads. One helper renders all of them.
console.log('\nDates in messages are spelled out')
// Any date-typed value concatenated straight into a string, anywhere in the services that build
// messages. This is the check that actually holds the line: a new message can be written without
// going through the helper, and nothing else would notice.
//
// IT SCANS STATEMENTS, NOT LINES, and both of those words were learnt the hard way. The first
// version looked only for GETTERS preceded by `+ "` on ONE line, and three separate mutations
// walked through the gaps:
//   - locals   `+ previousPeriodEnd`  — most of these dates are locals, not getters
//   - wrapping `+ previousPeriodEnd`  — alone on its continuation line, with no quote on it
//   - ternary  `: "sold on " + x`     — a string with no `+` in front of it
// Rebuilding it around `;`-delimited statements with the whitespace flattened found eighteen
// live sites the line version had missed, and four more after that.
const dateGetters = [...modelSources.matchAll(/private (?:Instant|LocalDate|LocalDateTime) (\w+);/g)]
  .map((m) => 'get' + m[1][0].toUpperCase() + m[1].slice(1))
const rawDates = messageSources.flatMap(([name, src]) => {
  const locals = [...src.matchAll(/\b(?:Instant|LocalDate|LocalDateTime)\s+(\w+)\s*[=,);]/g)]
    .map((m) => m[1])
  const idents = [...new Set([...locals, ...dateGetters])].sort((a, b) => b.length - a.length)
  const found = []
  let at = 0
  for (const stmt of src.split(';')) {
    const startedAt = at
    at += stmt.length + 1
    if (!stmt.includes('"')) continue
    // anything already inside Dates.readable(...) is exactly what we want to see
    const flat = stmt.replace(/Dates\.readable\([^;]*?\)/g, 'OK').replace(/\s+/g, ' ')
    for (const id of idents) {
      const e = '(?<![\\w.])(?:\\w+\\.)?' + id + '(?:\\(\\))?(?![\\w])'
      if (new RegExp('" *\\+ *' + e + ' *(?:\\+|\\)|,|$)').test(flat)
        || new RegExp(e + ' *\\+ *"').test(flat)) {
        const line = src.slice(0, startedAt).split('\n').length
        found.push(`${name}:~${line} [${id}]`)
        break
      }
    }
  }
  return found
})
const dateChecks = [
  [`no service concatenates a raw date${rawDates.length ? ': ' + rawDates.join(' | ') : ''}`,
    rawDates.length === 0],
  ['the helper exists, in common rather than in a service',
    datesUtil.includes('public final class Dates')
      && datesUtil.includes('package com.orbitastra.backend.common.time;')],
  // The format the user asked for: weekday, day, month, year, then the time.
  ['it renders "Friday 8 October 2027 10:01PM"',
    datesUtil.includes('"EEEE d MMMM yyyy h:mma"')],
  // Locale.ENGLISH is not optional: en_IN renders "10:01pm" in lower case, and a JVM started
  // elsewhere would render the month in another language. An API message is part of the contract.
  ['the locale is pinned, not taken from the JVM',
    (datesUtil.match(/Locale\.ENGLISH/g) || []).length >= 2
      && !/DateTimeFormatter\.ofPattern\("[^"]+"\)/.test(datesUtil)],
  // A LocalDate is already a day; inventing a time for it would invent information.
  ['a LocalDate keeps its own form, with no invented time',
    datesUtil.includes('"EEEE d MMMM yyyy"')
      && /public static String readable\(LocalDate date\)/.test(datesUtil)],
  ['and a null date reads as text rather than the word null',
    datesUtil.includes('NOT_SET = "(not set)"')],
  // THE ZONE DECIDES THE CALENDAR DAY: midnight in Asia/Kolkata is 18:30Z the day before, so a
  // school-owned date rendered in UTC names the wrong day.
  // The zone became a SchoolTimeZone on 2026-09-10. The behaviour is identical; the type is not,
  // and this guard was matching the old String declaration.
  ['a school-owned date is rendered in the school\'s zone',
    javaService.includes('SchoolTimeZone zone = school.getDefaultTimeZone();')
      && (javaService.match(/Dates\.readable\([^)]*, zone\)/g) || []).length >= 10],
  ['and the zone it threads through is the enum, not a String',
    !/String zone = school\.getDefaultTimeZone\(\)/.test(javaService)],
  ['the school surface passes its own school\'s zone too',
    messageSources.find(([n]) => n === 'SchoolSubscriptionService')[1]
      .includes('whyNotActive(subscription, school.getDefaultTimeZone())')],
  // A plan's selling window belongs to the platform, so UTC is the honest rendering there.
  ['a plan window uses the no-zone form, because no school owns it',
    /Dates\.readable\(plan\.getEffective(?:Until|From)\(\)\)/.test(javaService)
      && /Dates\.readable\(savedPlan\.getEffective(?:Until|From)\(\)\)/
        .test(messageSources.find(([n]) => n === 'PlanCatalogueService')[1])],
  // The fields themselves must NOT change: a program parses those.
  ['the response fields are still ISO instants',
    !/private String currentPeriodEnd/.test(javaService)],
  // Documented examples are what a reader trusts before calling anything.
  ['the documented examples show the spelled-out form',
    plansReadme.includes('## Dates in messages')
      && !/"(?:note|nextStep|reason|message)": .*20\d\d-\d\d-\d\dT/.test(plansReadme)],
]
for (const [label, ok] of dateChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nHelpers stay flat: no helper calls another')
// The helpers are PUBLIC methods on the utils and helper files now — the service has none left.
// AUDITED PER FILE, which matters: concatenating them makes the method boundaries meaningless,
// because the last method of one file looks like it contains the whole of the next. What rule 2
// forbids is one method calling another in the SAME utils file, and rule 3 the same in the helper.
const helperFiles = [
  ['PlatformSubscriptionServiceUtils', javaSubUtils],
  ['SchoolSubscriptionServiceUtils', schoolSubUtils],
  ['PlansHelper', javaPlansHelper],
]
const chained = []
let helperCount = 0
const misIndented = []
for (const [label, src] of helperFiles) {
  const decls = [...src.matchAll(/\n    public (?:static )?[^\s(]+ (\w+)\(/g)]
    .map((m) => [m[1], m.index])
  const names = new Set(decls.map(([n]) => n))
  helperCount += names.size
  decls.forEach(([name, at], i) => {
    const until = i + 1 < decls.length ? decls[i + 1][1] : src.length
    const code = src.slice(at, until).split('\n')
      .filter((line) => !/^\s*(\*|\/\/|\/\*)/.test(line)).join('\n')
    for (const other of names) {
      if (other === name) continue
      if (new RegExp('\\b' + other + '\\s*\\(').test(code)) {
        chained.push(`${label}: ${name}() -> ${other}()`)
      }
    }
  })
  misIndented.push(...[...src.matchAll(/\n( {5,})public (?:static )?[^\s(]+ (\w+)\(/g)]
    .map((m) => `${label}: ${m[2]}`))
}
const helperNames = { size: helperCount }
const flatChecks = [
  [`${helperNames.size} helpers, none calling another${chained.length ? ': ' + chained.join(', ') : ''}`,
    helperNames.size > 0 && chained.length === 0],
  // THE INDENT IS LOAD-BEARING for the check above: startOfTodayInSchoolZone sat at eight
  // spaces, so the scan skipped it and reported a clean file while that helper was being called
  // from inside another one. Same shape as getSubscription, which hid from the endpoint-marker
  // audit for the same reason.
  [`every helper is declared at one indent${misIndented.length ? ': ' + misIndented.join(', ') : ''}`,
    misIndented.length === 0],
]
for (const [label, ok] of flatChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// The helper's own body, from its signature to the blank line after its closing brace.
const startHelperAt = javaSubUtils.indexOf('public void validatePeriodStartIsTodayOrLater(')
const startHelper = javaSubUtils.slice(startHelperAt,
  javaSubUtils.indexOf('\n    }', startHelperAt))
const startChecks = [
  ['the service refuses one on the sale, the edit and the plan change',
    javaSubUtils.includes('public void validatePeriodStartIsTodayOrLater(')
      && (javaEndpoints.match(/utils\.validatePeriodStartIsTodayOrLater\(request\.currentPeriodStart\(\), school\)/g)
        || []).length === 3],
  // WHAT it does, not WHERE the code sits. The zone resolution has been inline in this helper
  // and in a second helper it delegated to, and a check pinned to either shape goes red on a
  // correct refactor rather than on a regression. Two facts hold in both: this helper reads the
  // SCHOOL's zone, and it never falls back to Instant.now(), which is what a UTC comparison
  // would look like.
  ['it compares against the school\'s own timezone, not UTC',
    startHelper.includes('school.getDefaultTimeZone()')
      && !startHelper.includes('Instant.now()')
      // The zone-to-midnight step is Dates.startOfTodayIn now: two utils methods needed it, so
      // it went to common rather than one utils method calling the other.
      && startHelper.includes('Dates.startOfTodayIn(school.getDefaultTimeZone())')
      && datesUtil.includes('LocalDate.now(resolved).atStartOfDay(resolved).toInstant()')],
  // Asserted on the CATCH BLOCK, not on the words appearing somewhere nearby: ZoneOffset.UTC is
  // also the unset-zone branch of the ternary above it, so a looser check passed while the catch
  // had been changed to rethrow.
  // THIS GUARD USED TO ASSERT A try/catch THAT NO LONGER EXISTS, and removing it was the point.
  // `zoneOrUtc` parsed a String and caught DateTimeException because the field could hold
  // anything; SchoolTimeZone is generated from ZoneId.getAvailableZoneIds(), so a malformed zone
  // is unrepresentable and there is nothing left to catch. What still has to hold is the null
  // case, which the platform surface really does pass for dates no school owns.
  ['a missing zone still means UTC, and there is no parse to fail any more',
    /return zone == null \? ZoneOffset\.UTC : zone\.toZoneId\(\);/.test(datesUtil)
      && !/catch \(DateTimeException/.test(datesUtil)],
  ['and the enum is what makes a malformed zone impossible',
    /Generated from \{@code ZoneId\.getAvailableZoneIds\(\)\}/
      .test(readFileSync('../backend/src/main/java/com/orbitastra/backend/models/common/enums/SchoolTimeZone.java', 'utf8'))],
  ['null still means today',
    /if \(requestedStart == null\) \{\s*\n\s*return;/.test(startHelper)],
  ['it refuses only a start strictly before that',
    startHelper.includes('requestedStart.isBefore(startOfToday)')],
  // The stored start of a running subscription is in the past by definition.
  ['it never checks what is already stored',
    !/validatePeriodStartIsTodayOrLater\([^)]*subscription/.test(javaService)],
  ['the helper says which methods use it',
    /Used by:\n     \* - changePlan\(\)\n     \* - createSubscription\(\)\n     \* - updateSubscription\(\)\n     \*\/\n    public void validatePeriodStartIsTodayOrLater/
      .test(javaSubUtils)],
  // The screen side: only a CHANGED value is flagged, or every running subscription would open
  // with an error on the form.
  ['the edit form flags only a changed start',
    editSource.includes('const startChanged = form.currentPeriodStart !== stored.currentPeriodStart')
      && /const startInPast = startChanged[\s\S]{0,160}< todayInput\(\)/.test(editSource)],
  ['the picker\'s min appears only once it is changed',
    editSource.includes('min={startChanged ? todayInput() : undefined}')],
  // Not disabled — a backdated start has to be sendable so the 400 can be seen — so the guard
  // is that the condition is worked out and shown.
  ['it is flagged without blocking the send',
    editSource.includes('const startInPast = startChanged')
      && editSource.includes('{startInPast ? (')],
  ['and it says the stored one being in the past is fine',
    editSource.includes('A billing period starts today or later')
      && editSource.includes('it is only a')],
  // The sale form now HAS to send the field, so the guard is that nobody can type a wrong value
  // into it: the box is read-only and the instant is computed, not picked.
  // It IS editable now: the API takes any start from today onwards, and a locked box put that
  // whole class of request out of reach of a tool whose job is to make them.
  ['the sale form lets the start be typed',
    /label="Period starts on"[\s\S]{0,600}onChange=\{\(event\) => setStartDay/.test(newSource)],
]
for (const [label, ok] of startChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// The cycle decides how long a period runs, on the edit form as on the sale form: #14 now
// recalculates currentPeriodEnd when billingCycle or currentPeriodStart moves, so the end box is
// the caller's to fill only on a CUSTOM cadence.
console.log('\nOn the edit form the cadence decides the period too')
const editCycleChecks = [
  ['the end date is editable on every cadence',
    editSource.includes("const editIsCustomCycle = form.billingCycle === 'CUSTOM'")
      && editSource.includes('value={form.currentPeriodEnd}')],
  ['it shows the date the API will derive',
    editSource.includes('const editDerivedEnd =')
      && editSource.includes('DAYS_PER_CYCLE[form.billingCycle]')],
  // All three conditions, not just the constant's name: it has to be the cadence being CUSTOM,
  // the cadence having actually changed, and no date sent. A version stubbed to a constant
  // passed the earlier "the name exists" form of this check.
  ['moving to CUSTOM with an empty box is flagged, not blocked',
    editSource.includes('const customNeedsEnd = editIsCustomCycle && cycleChanged && !form.currentPeriodEnd')
      && editSource.includes('{customNeedsEnd ? (')],
  // The API requires currentPeriodEnd on that transition, so the diff-only body has to break its
  // own rule there — otherwise somebody happy with the date already shown gets a 400 for
  // changing nothing.
  ['and the body sends the end on that transition even unchanged',
    editBodySource.includes("const movingToCustom = form.billingCycle === 'CUSTOM'")
      && /movingToCustom \|\| form\.currentPeriodEnd !== stored\.currentPeriodEnd/
        .test(editBodySource)],
  // The bug this replaced: periodBackwards compared the STORED end against the new start, so a
  // perfectly good derived end read as "runs backwards" and disabled the button.
  ['the period is judged on the end that will be in force',
    editSource.includes("const effectiveEnd = editIsCustomCycle ? form.currentPeriodEnd : editDerivedEnd")
      && /const periodBackwards = editIsCustomCycle[\s\S]{0,200}effectiveEnd <= form\.currentPeriodStart/
        .test(editSource)],
  ['a derived end is never called backwards',
    !/periodBackwards[\s\S]{0,200}stored\.currentPeriodEnd/.test(editSource)],
  ['and says why, naming the cadence being left',
    editSource.includes('A CUSTOM cadence needs an end date with it')
      && editSource.includes('{stored.billingCycle} cadence this subscription is leaving')],
  ['a period end about to move on its own is announced',
    editSource.includes('The period end moves with this')],
  // The hint used to promise the opposite. It must not still say that.
  ['the cycle hint no longer says the dates are untouched',
    !editSource.includes('are NOT recalculated from it')],
  // The service side of the same rule.
  ['the service derives the end when the cadence or start moves',
    javaService.includes('boolean cycleMoved = changed.contains("billingCycle")')
      && javaService.includes('boolean startMoved = changed.contains("currentPeriodStart")')],
  ['an explicit end still wins',
    javaService.includes('if (request.currentPeriodEnd() == null && (cycleMoved || startMoved)')],
  ['a derived end is reported as a changed field, not applied silently',
    /setCurrentPeriodEnd\(derivedEnd\);[\s\S]{0,80}changed\.add\("currentPeriodEnd"\)/
      .test(javaService)],
  // The requirement moved: it is now keyed on the cycle SENT rather than on it having changed,
  // and lives beside the start requirement in step 4.
  ['a CUSTOM cadence is refused without an end',
    /request\.billingCycle\(\) == BillingCycle\.CUSTOM\s*\n\s*&& request\.currentPeriodEnd\(\) == null[\s\S]{0,300}BILLING_PERIOD_END_REQUIRED/
      .test(javaService)],
  // A CUSTOM subscription's end is an agreed date, not a derivation, so a moved start leaves it.
  ['but a CUSTOM subscription keeps its agreed end when only the start moves',
    javaService.includes('&& subscription.getBillingCycle() != BillingCycle.CUSTOM')],
]
for (const [label, ok] of editCycleChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// #13 writes a capacity figure onto every subscription now, copying the plan's when the sale
// named none. So "a figure is set" stopped meaning "negotiated", and the form has three states to
// tell apart rather than two — a box showing the plan's own number is not a negotiated ceiling.
console.log('\nA copied limit is not a negotiated one')
const limitChecks = [
  ['the form tells the three states apart', editSource.includes('const limitHint =')],
  ['it leans on hasLimitOverrides, not on a null check',
    /const limitHint[\s\S]{0,400}subscription\.hasLimitOverrides/.test(editSource)],
  ['and says when a figure was copied rather than agreed',
    editSource.includes('copied from the plan when this was sold')],
  // The badge on the card has to mean the same thing.
  ['the card badges only a real negotiation',
    subsSourceFull.includes('{s.hasLimitOverrides ? <Badge tone="brand">negotiated</Badge>')],
]
for (const [label, ok] of limitChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// Choosing a plan on the change-plan modal fills the three negotiable boxes with that plan's
// figures, rather than showing them as placeholders nobody can read back or edit. Scoped to
// ChangePlan the same way the edit form is, so the other modals' boxes cannot answer for it.
console.log('\nChoosing a plan fills in what that plan charges')
const autofillChecks = [
  ['the select fills the boxes rather than only recording the choice',
    changeSource.includes('onChange={(event) => choosePlan(event.target.value)}')
      && !changeSource.includes('onChange={(event) => setPicked(event.target.value)}')],
  ['it fills all three negotiable figures',
    /const choosePlan[\s\S]{0,600}setPrice\(plan \? String\(plan\.listPrice\)/.test(changeSource)
      && /const choosePlan[\s\S]{0,600}setMaxStudents\(plan \? String\(plan\.maxStudents\)/
        .test(changeSource)
      && /const choosePlan[\s\S]{0,600}setMaxUsers\(plan \? String\(plan\.maxUsers\)/
        .test(changeSource)],
  ['clearing the plan empties them again',
    (changeSource.match(/plan \? String\([^)]+\) : ''/g) || []).length === 3],
  // The figures are the plan's, so they must be re-read when the plan changes. A handler that
  // only filled an empty box would leave the old plan's price sitting under a new plan's name.
  ['switching plan re-fills them', !/if \(!price\)|price === '' \?/.test(changeSource)],
  // The placeholders stay, but they are now the fallback for a box somebody emptied rather than
  // the only way to see the figure — so each box has to bind its value to the state choosePlan
  // writes, or the fill would be invisible and the placeholder would be back to being the display.
  ['each box shows the filled figure as its value',
    changeSource.includes('value={price}') && changeSource.includes('value={maxStudents}')
      && changeSource.includes('value={maxUsers}')],
  // The hints used to say "blank takes the plan's" — true then, wrong now the box is filled.
  ['the hints describe a filled box',
    !changeSource.includes("Blank takes the plan's")
      && changeSource.includes('filled from the plan')],
  // The warning for a negotiated school keyed on the boxes being blank, which they never are
  // now. It has to key on their still holding the plan's figures instead, or it never shows.
  ['the negotiated-ceiling warning still fires',
    changeSource.includes('const untouchedFromPlan')
      && changeSource.includes('untouchedFromPlan && subscription.hasLimitOverrides')
      && !changeSource.includes("subscription.hasLimitOverrides && !maxStudents.trim()")],
  ['a negotiated price is warned about too',
    changeSource.includes('untouchedFromPlan && paysNegotiatedPrice')],
  // hasDiscount is not a field on SubscriptionResponse; the two prices are.
  ['the price warning compares the two prices the API does send',
    !changeSource.includes('subscription.hasDiscount')
      && /paysNegotiatedPrice[\s\S]{0,300}subscription\.planListPrice/.test(changeSource)],
]
for (const [label, ok] of autofillChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// The sale form got the same treatment, and the two ceiling boxes it never offered at all —
// #13 has accepted maxStudentsOverride and maxUsersOverride since the sale started copying the
// plan's limits onto the subscription, but there was no way to negotiate one at the point of sale.
console.log('\nThe sale form fills in from the plan too')
const saleFillChecks = [
  ['the select fills the boxes',
    newSource.includes('onChange={(event) => choosePlan(event.target.value)}')],
  ['all three figures come from the plan',
    /const choosePlan[\s\S]{0,600}setPrice\(plan \? String\(plan\.listPrice\)/.test(newSource)
      && /const choosePlan[\s\S]{0,600}setMaxStudents\(plan \? String\(plan\.maxStudents\)/
        .test(newSource)
      && /const choosePlan[\s\S]{0,600}setMaxUsers\(plan \? String\(plan\.maxUsers\)/
        .test(newSource)],
  ['a ceiling can now be negotiated at the point of sale',
    newSource.includes('out.maxStudentsOverride = Number(maxStudents)')
      && newSource.includes('out.maxUsersOverride = Number(maxUsers)')],
  ['an emptied box still means "copy the plan\'s"',
    newSource.includes('if (maxStudents.trim())') && newSource.includes('if (maxUsers.trim())')],
  ['a fresh sale starts from a clean form',
    /setPicked\(''\)[\s\S]{0,120}setMaxStudents\(''\)[\s\S]{0,60}setMaxUsers\(''\)/
      .test(newSource)],
  // The activate endpoint was removed on 2026-09-07: a trial that starts paying is #16.
  ['the trial checkbox no longer promises an activate call',
    !newSource.includes('activating it later') && newSource.includes('there is no activate endpoint')],
]
for (const [label, ok] of saleFillChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// #17 takes no request body, so it is a button rather than a modal — and the refusals it would
// give are worked out on the screen so they show on the button instead of arriving as a 409.
// That mirror can drift, so the renewable statuses are read back out of the Java.
console.log('\nRenewing is a button, and it says when it would be refused')
// Comments stripped, for the checks that ask "does this CALL x" rather than "does it mention x" —
// the method's own prose explains why it deliberately does not use loadSellablePlan, and a
// substring test cannot tell that apart from calling it.
const javaRenewCode = javaRenew.split('\n')
  .filter((line) => !line.trim().startsWith('//') && !line.trim().startsWith('*'))
  .join('\n')
const ALL_STATUSES = ['TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED', 'CANCELLED', 'EXPIRED']
// The service's allow-list, lifted from renewSubscription's own condition.
const renewableInJava = ALL_STATUSES.filter((st) =>
  new RegExp(`renewable[\\s\\S]{0,240}SubscriptionStatus\\.${st}\\b`).test(javaRenew))
const refusedInJava = ALL_STATUSES.filter((st) => !renewableInJava.includes(st))
// The CUSTOM dialog on its own, ending at whichever function follows it, so the sale form's
// four boxes below cannot answer for "this dialog has one field".

const renewChecks = [
  ['the screen has a refusal mirror at all', mirrorSource.length > 0],
  [`the Java renews exactly ACTIVE, PAST_DUE, EXPIRED (got ${renewableInJava.join(', ')})`,
    renewableInJava.join(',') === 'ACTIVE,PAST_DUE,EXPIRED'],
  [`and the screen refuses exactly the other three (${refusedInJava.join(', ')})`,
    refusedInJava.every((st) => mirrorSource.includes(st))
      && renewableInJava.every((st) => !mirrorSource.includes(`'${st}'`))],
  // autoRenew is NOT a refusal: #17 does not check it, so neither should the screen. Guarded
  // the other way round, because a mirror that refuses more than the API does is just as wrong.
  ['it does not invent an autoRenew refusal', !mirrorSource.includes('autoRenew')],
  ['and the service does not check autoRenew either',
    !javaRenewCode.includes('AUTO_RENEW_OFF')],
  // A CUSTOM cycle is NOT a refusal any more — it is a question. The mirror must not treat it as
  // one, or the button would block a renewal the API would happily do once given a date.
  ['a CUSTOM cycle is not in the refusal mirror', !mirrorSource.includes('CUSTOM')],
  ['the service asks for the date rather than refusing',
    javaRenew.includes('BILLING_PERIOD_END_REQUIRED')
      && !javaService.includes('CUSTOM_CYCLE_NOT_RENEWABLE')],
  ['it mirrors the period-not-ended refusal',
    mirrorSource.includes('new Date(s.currentPeriodEnd) > new Date()')],
  // A renewal re-commits the school to the SAME plan, so the plan has to still be current. The
  // response reports planStatus and planRetired, which makes this one predictable on the screen.
  ['it mirrors the retired-plan refusal',
    mirrorSource.includes('s.planRetired') && mirrorSource.includes("s.planStatus === 'DRAFT'")],
  ['and points at changing the plan rather than just blocking',
    /planRetired[\s\S]{0,300}change the plan instead/.test(mirrorSource)],
  ['the service refuses a plan that is no longer current',
    javaRenew.includes('PLAN_NOT_RENEWABLE')
      && javaRenew.includes('PlanStatus.RETIRED') && javaRenew.includes('PlanStatus.DRAFT')
      && javaRenew.includes('getEffectiveUntil()') && javaRenew.includes('getEffectiveFrom()')],
  ['and does not reuse the sale-time refusal for it',
    !javaRenewCode.includes('loadSellablePlan')
      && !javaRenewCode.includes('PLAN_NOT_SELLABLE')],
  ['the refusal tells the caller to change the plan',
    /PLAN_NOT_RENEWABLE[\s\S]{0,700}change-plan/.test(javaRenew)],
  // A private plan is a negotiated quote, not an invalid state.
  ['neither refuses a private plan',
    !mirrorSource.includes('publiclyAvailable')
      && !javaRenewCode.includes('PubliclyAvailable')],
  // A dialog exists now, but only for CUSTOM: an ordinary renewal has nothing to fill in, so it
  // must still go straight out rather than opening a form with nothing in it.
  ['an ordinary renewal opens no dialog',
    /else renew\(\)/.test(subsSourceFull) && !/setRenewingCustom\(true\)\s*\n\s*renew\(/.test(subsSourceFull)],
  ['the mirror drives the label, not a disabled button',
    subsSourceFull.includes("{renewRefusal ? 'Cannot renew yet'")],
  ['the reason is shown rather than only blocking',
    subsSourceFull.includes('{renewRefusal') && subsSourceFull.includes('Cannot renew yet')],
  // The one field #17 ever asks for, and only for the one cycle that needs it.
  ['a CUSTOM cycle gets asked for the end date',
    subsSourceFull.includes('function renewNeedsEndDate')
      && /renewNeedsEndDate[\s\S]{0,120}billingCycle === 'CUSTOM'/.test(subsSourceFull)],
  ['and only a CUSTOM cycle opens the dialog',
    subsSourceFull.includes('if (renewNeedsEndDate(subscription)) setRenewingCustom(true)')
      && subsSourceFull.includes('else renew()')],
  ['the dialog asks for nothing but the date',
    (renewDialogSource.match(/<Field/g) || []).length === 1
      && (renewDialogSource.match(/<Input/g) || []).length === 1
      && renewDialogSource.includes('type="date"')],
  // No date means no body at all — buildCall omits the body and the Content-Type when it is
  // undefined, which is what the endpoint expects for the four derivable cycles.
  ['an ordinary renewal still sends no body',
    subsSourceFull.includes('body: endDate ? { currentPeriodEnd: endOfDay(endDate) } : undefined')],
  ['the picker will not offer a day before the period starts',
    subsSourceFull.includes('min={startsOn || undefined}')],
  ['and a too-early date is flagged, still sendable',
    subsSourceFull.includes('const tooEarly =') && subsSourceFull.includes('endDate <= startsOn')
      && subsSourceFull.includes('{tooEarly ? (')],
  ['the dialog says a renewal is not a renegotiation',
    subsSourceFull.includes('a renewal is not a renegotiation')],
  ['the endpoint is tagged on the screen',
    subsSourceFull.includes('id="renew-subscription"')],
  // The two things easiest to assume the wrong way round about a renewal.
  ['it says no money moves and no invoice is raised',
    subsSourceFull.includes('Renewing takes no money and raises no invoice')],
  ['it says a second row is written',
    /second row<\/strong>/.test(subsSourceFull)],
  ['and that warning is hidden when renewing is refused anyway',
    subsSourceFull.includes('{renewRefusal ? null : (')],
  // #16 has been built since this banner was written.
  ['the trial banner no longer calls #16 unbuilt',
    !subsSourceFull.includes('not built yet')],
]
for (const [label, ok] of renewChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// The sale form now sells the CYCLE too, not just the plan. #13 takes billingCycle, absent
// meaning the plan's own — so the box is filled from the plan and editable, and everything about
// the period follows the box rather than the plan.
console.log('\nThe sale form sells the cycle, and the period follows it')
const cycleChecks = [
  ['the form offers a cycle, filled from the plan',
    newSource.includes('label="Billing cycle"')
      && /const choosePlan[\s\S]{0,900}setCycle\(plan \? plan\.billingCycle : ''\)/
        .test(newSource)],
  // Two separate facts rather than one wide window between them: the component has a cycle
  // select, and it is driven by the shared BILLING_CYCLES list so a value added to the enum
  // cannot be missing from only this form.
  ['every cycle the model has is offered',
    (newSource.match(/BILLING_CYCLES\.map/g) || []).length === 1
      && newSource.includes('label="Billing cycle"')],
  ['and that list matches the model\'s enum',
    (() => {
      const inJs = (subsSourceFull.match(/const BILLING_CYCLES = \[([^\]]+)\]/) || [])[1]
      const inJava = readFileSync(
        '../backend/src/main/java/com/orbitastra/backend/models/plans/enums/BillingCycle.java',
        'utf8').match(/^\s+([A-Z_]{4,}),?$/gm)?.map((l) => l.trim().replace(',', ''))
      return Boolean(inJs) && Boolean(inJava)
        && inJava.every((v) => inJs.includes(`'${v}'`))
        && inJava.length === (inJs.match(/'/g) || []).length / 2
    })()],
  // The whole point: the period follows the cycle being SOLD, not the plan's listed one.
  ['the period follows the cycle being sold, not the plan\'s',
    newSource.includes("const soldCycle = cycle || chosen?.billingCycle || ''")
      && newSource.includes("const needsPeriodEnd = soldCycle === 'CUSTOM'")
      && newSource.includes('const cycleDays = DAYS_PER_CYCLE[soldCycle]')],
  ['the end date is editable on every cycle',
    newSource.includes('value={periodEnd}') && !/readOnly/.test(newSource)],
  ['and the derived date is named in the hint instead',
    newSource.includes('the API derives')],
  ['the start date defaults to today and can be changed',
    /label="Period starts on"[\s\S]{0,600}value=\{startDay \|\| todayInZone\(timeZone\)\}/
      .test(newSource)],
  ['changing the cycle clears a date typed against the old one',
    /setCycle\(event\.target\.value\)[\s\S]{0,300}setPeriodEnd\(''\)/.test(newSource)],
  // Sent only when it differs, so an ordinary sale does not restate what the plan already says.
  ['the cycle is sent only when it differs from the plan',
    newSource.includes("if (cycle && chosen && cycle !== chosen.billingCycle) out.billingCycle = cycle")],
  ['selling off-cadence is called out before the sale goes',
    newSource.includes('This is not the cadence the plan is listed on')],
  // The service must read the request's cycle, not the plan's, or the form is lying.
  ['the service bills on the requested cycle',
    javaService.includes('BillingCycle billingCycle = request.billingCycle() == null')
      && javaService.includes('.billingCycle(billingCycle)')],
  ['and derives the period from it, not from the plan',
    /calculateSubscriptionPeriodEnd\(request\.currentPeriodEnd\(\),\s*\n?\s*periodStart,\s*billingCycle,/
      .test(javaService)],
]
for (const [label, ok] of cycleChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// A CUSTOM cycle has no length, so the API cannot derive a period end and refuses the sale
// without one. The form has to ask, or the only way to find out is a 400 with a filled-in form.
//
// The form also says what period a sale WOULD produce, which means it mirrors the service's day
// counts — so this reads those numbers back out of the Java and fails if the two ever disagree.
console.log('\nA CUSTOM cycle is asked for its end date')
const cycleDays = Object.fromEntries([...subsSourceFull.matchAll(
  /(MONTHLY|QUARTERLY|HALF_YEARLY|YEARLY):\s*(\d+)/g)].map((m) => [m[1], Number(m[2])]))
const javaDays = Object.fromEntries([...javaService.matchAll(
  /case (MONTHLY|QUARTERLY|HALF_YEARLY|YEARLY) -> (\d+);/g)].map((m) => [m[1], Number(m[2])]))

const customChecks = [
  // Scoped to the sale form: ChangePlan asks the same question and used to answer it with the
  // same expression, which is why a file-wide match kept passing for the wrong component.
  ['the form asks for an end date, and only for CUSTOM',
    newSource.includes("const needsPeriodEnd = soldCycle === 'CUSTOM'")
      && /\{needsPeriodEnd \? \(/.test(newSource)],
  // Matched as one clause of the button's guard rather than as the whole expression, so adding
  // another reason to refuse the sale does not read as removing this one.
  // Both still WORKED OUT and shown; neither blocks the send, so both refusals are testable.
  ['it says an end date is needed without blocking the sale',
    subsSourceFull.includes("'Set the end date first'")],
  ['and flags a ceiling of nothing the same way',
    subsSourceFull.includes('zeroCeiling') && subsSourceFull.includes('{zeroCeiling ? (')],
  ['the chosen day is sent as the end of it', subsSourceFull.includes('endOfDay(periodEnd)')],
  // Whatever is typed is sent, on any cycle: the API takes an explicit end as an override on the
  // four fixed cadences, so this tool must be able to make that request.
  ['whatever is typed is sent, on any cycle',
    (subsSourceFull.match(/if \(periodEnd\) out\.currentPeriodEnd/g) || []).length === 2
      && !subsSourceFull.includes('if (needsPeriodEnd) out.currentPeriodEnd')],
  // The mirror, checked against the source it mirrors.
  ['the form found four cycle lengths', Object.keys(cycleDays).length === 4],
  ['the service still states the same four', Object.keys(javaDays).length === 4],
  ['and they agree', JSON.stringify(cycleDays) === JSON.stringify(javaDays)],
  // CUSTOM must be absent from the mirror, or the form would derive a length it does not have.
  ['CUSTOM has no length in either', !('CUSTOM' in cycleDays) && !('CUSTOM' in javaDays)],
]
for (const [label, ok] of customChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nAn edit sends only what moved')
const STORED_SUB = {
  subscriptionNo: 'SUB/2026/09/000001', planCode: 'PREMIUM', planVersion: 1,
  status: 'ACTIVE', billingCycle: 'YEARLY',
  currentPeriodStart: '2026-04-01T00:00:00Z', currentPeriodEnd: '2027-03-31T23:59:59Z',
  // Left on the fixture on purpose: the endpoint cannot edit these, so they have to survive a
  // round through storedForm/patchBody untouched rather than merely be missing from the test.
  autoRenew: true, contractedPrice: 49999.0, currencyCode: 'INR',
  billingCustomerReference: 'cus_Qx7B2mR9', maxStudentsOverride: 2500, maxUsersOverride: 300,
  reasonForChanges: 'Renegotiated at renewal.',
}
const baseline = storedForm(STORED_SUB)
const edited = (changes) => patchBody({ ...baseline, reason: '', ...changes }, baseline)
// Key order is not part of a JSON body's meaning, so it must not be part of the comparison —
// expecting the same fields in a different order would fail for a reason nobody cares about.
const sorted = (value) => (value === null || typeof value !== 'object' || Array.isArray(value)
  ? value
  : Object.fromEntries(Object.keys(value).sort().map((key) => [key, sorted(value[key])])))
const same = (a, b) => JSON.stringify(sorted(a)) === JSON.stringify(sorted(b))

const editCases = [
  ['nothing touched sends nothing', same(edited({}), {})],
  // The reason explains an edit; alone it is not one, and sending it alone would be a 400.
  ['a reason alone sends nothing', same(edited({ reason: 'because' }), {})],
  ['a reason rides along with a real change',
    same(edited({ autoRenew: false, reason: 'because' }), { autoRenew: false, reason: 'because' })],
  ['one field sends one field', same(edited({ autoRenew: false }), { autoRenew: false })],
  // THE TWO CASES THAT WERE BROKEN. The screen showed "that period runs backwards" for a
  // perfectly good derived end, and sent no end at all when moving to CUSTOM.
  //
  // On a fixed cadence the API derives the end, so the body must NOT carry one — sending one
  // would override a date the cadence already decides.
  ['a cadence and start change sends no end on a fixed cycle',
    same(edited({ billingCycle: 'QUARTERLY', currentPeriodStart: '2026-11-20', reason: 'r' }),
      { billingCycle: 'QUARTERLY', currentPeriodStart: '2026-11-20T00:00:00Z', reason: 'r' })],
  ['and a start change alone sends only the start',
    same(edited({ currentPeriodStart: '2026-11-20', reason: 'r' }),
      { currentPeriodStart: '2026-11-20T00:00:00Z', reason: 'r' })],
  // Moving to CUSTOM requires the end on the same request, so the unchanged box value goes with
  // it. Without this the API answers 400 BILLING_PERIOD_END_REQUIRED for changing nothing.
  ['moving to CUSTOM carries the end even unchanged',
    same(edited({ billingCycle: 'CUSTOM', reason: 'r' }),
      { billingCycle: 'CUSTOM', currentPeriodEnd: '2027-03-31T23:59:59Z', reason: 'r' })],
  ['moving to CUSTOM with a new start carries it too',
    same(edited({ billingCycle: 'CUSTOM', currentPeriodStart: '2026-11-20', reason: 'r' }),
      { billingCycle: 'CUSTOM', currentPeriodStart: '2026-11-20T00:00:00Z',
        currentPeriodEnd: '2027-03-31T23:59:59Z', reason: 'r' })],
  // Already CUSTOM: the end is an agreed date the API keeps, so an unchanged one stays absent.
  ['a start change on an existing CUSTOM sends no end',
    (() => {
      const custom = storedForm({ ...STORED_SUB, billingCycle: 'CUSTOM' })
      return same(patchBody({ ...custom, currentPeriodStart: '2026-11-20', reason: 'r' }, custom),
        { currentPeriodStart: '2026-11-20T00:00:00Z', reason: 'r' })
    })()],
  ['and a changed end on an existing CUSTOM does send it',
    (() => {
      const custom = storedForm({ ...STORED_SUB, billingCycle: 'CUSTOM' })
      return same(patchBody({ ...custom, currentPeriodEnd: '2027-06-30', reason: 'r' }, custom),
        { currentPeriodEnd: '2027-06-30T23:59:59Z', reason: 'r' })
    })()],
  // The money and the plan are #25, #26 and #16. Nothing in the form can reach them, so nothing
  // in the body can either — a box that reappeared by accident would show up here.
  ['no edit can reach the price, the currency, the reference or the plan',
    ['contractedPrice', 'currencyCode', 'billingCustomerReference', 'plan'].every((field) =>
      !Object.keys(edited({ status: 'SUSPENDED', billingCycle: 'MONTHLY', autoRenew: false,
        currentPeriodStart: '2026-05-01', currentPeriodEnd: '2027-05-01',
        maxStudentsOverride: '9', maxUsersOverride: '9' })).includes(field))],
  ['and the form has no box for any of them',
    ['contractedPrice', 'currencyCode', 'billingCustomerReference'].every((field) =>
      !baseline[field] && !Object.keys(baseline).includes(field))],
  // The period dates are @NotNull on the model: an emptied box is an unfinished one, not a
  // request to clear them, and sending "" would be a 400.
  ['emptying a period date sends nothing', same(edited({ currentPeriodEnd: '' }), {})],
  // A picked day, not an instant: the box is a calendar, and the day it gives is widened to the
  // whole of it — the start from its first second, the end to its last.
  ['a picked end day is sent as the END of that day',
    same(edited({ currentPeriodEnd: '2027-12-31' }),
         { currentPeriodEnd: '2027-12-31T23:59:59Z' })],
  ['a picked start day is sent as the start of it',
    same(edited({ currentPeriodStart: '2026-05-01' }),
         { currentPeriodStart: '2026-05-01T00:00:00Z' })],
  ['an untouched date sends nothing, whatever time it is stored at',
    same(edited({}), {}) && storedForm({ ...STORED_SUB,
      currentPeriodEnd: '2027-03-31T07:29:29.533Z' }).currentPeriodEnd === '2027-03-31'],
  // The overrides are flat, and each goes on its own — so raising one leaves the other alone.
  ['changing one override sends only that one',
    same(edited({ maxStudentsOverride: '4000' }), { maxStudentsOverride: 4000 })],
  // Emptying a box is how an override is removed, and the API spells that 0: an omitted field
  // and an explicit null are the same value to Jackson, so zero carries the removal.
  ['emptying one override sends 0 for it, and nothing for the other',
    same(edited({ maxStudentsOverride: '' }), { maxStudentsOverride: 0 })],
  ['both at once send two fields, not a block',
    same(edited({ maxStudentsOverride: '200', maxUsersOverride: '400' }),
         { maxStudentsOverride: 200, maxUsersOverride: 400 })],
  // Nothing should reconstruct the shape that was flattened away.
  ['no edit sends a limitOverrides block any more',
    !Object.keys(edited({ maxStudentsOverride: '1', maxUsersOverride: '2' })).includes('limitOverrides')],
  // The reason is stored by the API as reasonForChanges, but it is not itself a change: alone it
  // would be a 400, and on a no-op edit there is nothing to explain.
  ['a reason alone is never sent', same(edited({ reason: 'because' }), {})],
  ['no edit can send a cancellation any more',
    ['cancellation', 'cancelledAt', 'cancellationReason'].every((field) =>
      !Object.keys(edited({ status: 'CANCELLED', autoRenew: false })).includes(field))],
  ['several fields at once go together',
    same(edited({ status: 'PAST_DUE', autoRenew: false, billingCycle: 'MONTHLY' }),
         { status: 'PAST_DUE', autoRenew: false, billingCycle: 'MONTHLY' })],
  // What the button counts, and what the preview lists.
  ['the reason is not counted as a change',
    changedFields({ autoRenew: false, reason: 'because' }).length === 1],
  // Nothing here may reach the three fields the endpoint refuses to edit.
  ['no edit can reach subscriptionNo, current or schoolId',
    ['subscriptionNo', 'current', 'schoolId'].every((field) => !Object.keys(
      edited({ status: 'SUSPENDED', autoRenew: false, billingCycle: 'MONTHLY',
               maxStudentsOverride: '9', maxUsersOverride: '9',
               currentPeriodStart: '2026-05-01',
               currentPeriodEnd: '2027-05-01' })).includes(field))],
]
for (const [label, ok] of editCases) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nA sale can take the school live, and the screen says so')
const subsSource = readFileSync('src/pages/platform/plans/Subscriptions.jsx', 'utf8')
const saleChecks = [
  ['the create response is handed back, not dropped', subsSource.includes('await onCreated(result.bodyJson)')],
  ['its nextStep is kept', subsSource.includes('nextStep: created?.nextStep')],
  // Whether the sale activated the school depends on setup this screen cannot see, so the status
  // has to be the server's answer.
  ['the school status is re-read, not assumed', subsSource.includes("call('get-school'")],
  ['the note distinguishes live from still-provisioning',
    subsSource.includes("aftermath.status === 'ACTIVE'") && subsSource.includes("aftermath.status === 'PROVISIONING'")],
  // The note is about one sale; carrying it to the next school would be a lie about that school.
  ['and it is cleared when another school is picked',
    /onChange=\{\([^)]*\) => \{[\s\S]{0,200}setAftermath\(null\)/.test(subsSource)],
]
for (const [label, ok] of saleChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe profile also reads the subscription')
const profileSource = readFileSync('src/pages/school/core/Profile.jsx', 'utf8')
const bottomChecks = [
  ['it reads the subscription as well', profileSource.includes("call('get-my-subscription'")],
  // Serially would double the time to first paint for no reason: neither read needs the other.
  ['both reads go out in parallel', profileSource.includes('Promise.all')],
  ['and it is a summary, not a second copy',
    profileSource.includes("screenPath('school', 'plans', 'subscription')")
      // The feature access belong to the subscription screen; repeating them here is two screens
      // to keep in step for one answer.
      && !profileSource.includes("'get-feature-access'")],
  // A school with no subscription is a normal state, not a failed read.
  ['no subscription is an answer, not an error',
    profileSource.includes("=== 'SUBSCRIPTION_NOT_FOUND'")],
]
for (const [label, ok] of bottomChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nPrivate is not unsellable')
const sell = [
  ['on the public list: nothing to say',
    sellability({ status: 'ACTIVE', publiclyAvailable: true }).canSell === true
      && sellability({ status: 'ACTIVE', publiclyAvailable: true }).label === null],
  // The one this fixes: it used to read "not sellable today".
  ['off the list: sellable, and called a quote',
    sellability({ status: 'ACTIVE', publiclyAvailable: false }).canSell === true
      && /quote only/.test(sellability({ status: 'ACTIVE', publiclyAvailable: false }).label)],
  ['a draft: genuinely refused',
    sellability({ status: 'DRAFT', publiclyAvailable: true }).canSell === false],
  ['retired: genuinely refused',
    sellability({ status: 'RETIRED', publiclyAvailable: true }).canSell === false],
  ['before its window: refused, with the date',
    sellability({ status: 'ACTIVE', publiclyAvailable: true, effectiveFrom: '2030-01-01T00:00:00Z' })
      .label.includes('2030-01-01')],
  ['after its window: refused, with the date',
    sellability({ status: 'ACTIVE', publiclyAvailable: true, effectiveUntil: '2020-01-01T00:00:00Z' })
      .label.includes('2020-01-01')],
  // The dialog must not go back to reading the flag it cannot trust.
  ['the dialog no longer labels from `sellable`',
    !readFileSync('src/pages/platform/plans/Subscriptions.jsx', 'utf8')
      .includes("one.sellable ? '' :")],
]
for (const [label, ok] of sell) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe top bar')
const platform = at('/platform-core/schools')
const school = at('/school-core/profile')
const barChecks = [
  // Labelled "School", not "Acting as": the latter is our word for it, not one anybody reads
  // off a screen and understands.
  ['the picker is offered on the school surface', school.includes('picker-trigger')],
  ['it is labelled with a school word, not jargon',
    school.includes('>School<') && !school.includes('Acting as')],
  ['and it is absent on the platform surface', !platform.includes('picker-trigger')],
  // The platform's subscription screen keeps its own picker: there the school is an argument to
  // the call, not a mode, so removing it would break the screen.
  ['the platform subscription screen keeps its own picker',
    at('/platform-plans/subscriptions').includes('picker-trigger')],
  // The header search searched nothing. The two screens that do have one keep it next to the
  // list it filters.
  ['the header has no dead search box', !platform.includes('Search endpoints')],
  ['but the lists that filter still have theirs',
    platform.includes('Search name or subdomain')
      && at('/platform-plans/catalogue').includes('Search name or code')],
]
for (const [label, ok] of barChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// #28 IS ON THE SCREEN, AND SO IS EVERY REFUSAL IT CAN GIVE. This is a testing tool, so a
// filter that would be rejected has to be reachable: `size` offers 101 and 0 (both refused, not
// clamped), and the sort list carries a field the API's allow-list does not.
console.log('\nThe subscription history (#28) is wired to the screen')
// From the feature's own constants, not from the function: HISTORY_SORTS and HISTORY_STATUSES
// sit above it, and a slice starting at the function misses the very lists being asserted on.
const historyStart = subsSourceFull.indexOf('const HISTORY_SORTS')
// ...to the function that follows the component. Measured from the component, not from the
// constants: the first `function` after the constants is the component itself.
const historyFn = subsSourceFull.indexOf('function SubscriptionHistory(')
const historyEnd = Math.min(...[...subsSourceFull.matchAll(/^function \w+\(/gm)]
  .map((m) => m.index).filter((i) => i > historyFn).concat([subsSourceFull.length]))
const history = subsSourceFull.slice(historyStart, historyEnd)
const { findEndpoint: lookupEndpoint } = await import('./src/config/endpoints.js')
const e28 = lookupEndpoint('list-school-subscriptions')
const historyChecks = [
  ['the endpoint is in the catalogue, as a GET with the school in the path',
    Boolean(e28) && e28.method === 'GET'
      && e28.path === '/platform/schools/{id}/subscriptions'],
  ['a screen calls it', history.includes("call('list-school-subscriptions'")],
  ['it is mounted on the subscriptions page',
    subsSourceFull.includes('<SubscriptionHistory schoolId={schoolId} />')],
  // Shown even when the school has no current subscription: a cancelled row is exactly what
  // somebody comes here to find, and #27 answering 404 says nothing about the history.
  ['and mounted outside the has-a-subscription branch',
    subsSourceFull.indexOf('<SubscriptionHistory') > subsSourceFull.indexOf('<RenewCustomPeriod')],
  ['the tag shows the URL it will really send, filters included',
    /<EndpointTag[\s\S]{0,200}id="list-school-subscriptions"[\s\S]{0,200}query=\{query\}/
      .test(history)],
  // The query is built at render, so the tag tracks the filters as they change.
  ['the query is built at render, not inside the call',
    /const query = useMemo\(\(\) => \{/.test(history)
      && history.indexOf('const query = useMemo') < history.indexOf("call('list-school-subscriptions'")],
  ['an empty filter box sends nothing rather than an empty parameter',
    history.includes('if (planCode.trim()) out.planCode = planCode.trim()')],
  // Every filter the API supports, and no filter it does not.
  [(() => {
    const supported = ['status', 'billingCycle', 'planCode', 'planVersion', 'autoRenew',
      'current', 'startDateFrom', 'startDateTo', 'endDateFrom', 'endDateTo', 'page', 'size',
      'sort']
    const absent = supported.filter((f) => !history.includes('out.' + f)
      && !new RegExp('\\{ *' + f + '[,} ]').test(history))
    return `every filter the API takes is on the screen${absent.length ? ': missing ' + absent.join(', ') : ''}`
  })(), (() => {
    const supported = ['status', 'billingCycle', 'planCode', 'planVersion', 'autoRenew',
      'current', 'startDateFrom', 'startDateTo', 'endDateFrom', 'endDateTo', 'page', 'size',
      'sort']
    return supported.every((f) => history.includes('out.' + f)
      || new RegExp('\\{ *' + f + '[,} ]').test(history))
  })()],
  // There is no `trial` field on the document, so there must be no `trial` filter either — the
  // status row carries TRIAL and that is the whole answer.
  ['there is no invented trial filter',
    !history.includes('out.trial') && history.includes("'TRIAL'")],
  ['the refusals are reachable: a size over the cap and a sort off the allow-list',
    history.includes("'101'") && history.includes("'contractedPrice,desc'")],
  ['nothing in it is disabled',
    !/disabled=|readOnly/.test(history)],
  // periodEnded is computed by the API precisely because status cannot be trusted alone.
  ['a row shows current and periodEnded, not just the status',
    history.includes('row.current') && history.includes('row.periodEnded')
      && history.includes('row.status')],
  ['a row whose plan was deleted says so rather than showing a blank',
    history.includes('row.planCode === null')],
  ['an empty page reads as "never had one", not as an error',
    history.includes('never had one')],
  ['the catalogue entry documents every refusal the endpoint gives',
    ['INVALID_PAGE', 'INVALID_PAGE_SIZE', 'INVALID_SORT_FIELD', 'INVALID_SORT_DIRECTION',
      'INVALID_DATE_RANGE', 'SCHOOL_NOT_FOUND']
      .every((code) => e28.errors.some((e) => e.code === code))],
]
for (const [label, ok] of historyChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe audit trail (#29) is wired to the screen')
// From the feature's own constants down to the function AFTER the component — measured from the
// component itself, because the first `function` below the constants is the component.
const trailStart = subsSourceFull.indexOf('const TRAIL_SORTS')
const trailFn = subsSourceFull.indexOf('function SubscriptionTrail(')
const trailEnd = Math.min(...[...subsSourceFull.matchAll(/^function \w+\(/gm)]
  .map((m) => m.index).filter((i) => i > trailFn).concat([subsSourceFull.length]))
const trail = subsSourceFull.slice(trailStart, trailEnd)
const e29 = lookupEndpoint('get-subscription-history')
const datesLib = readFileSync('src/lib/dates.js', 'utf8')
const trailChecks = [
  ['the endpoint is in the catalogue, as a GET with both path parts',
    Boolean(e29) && e29.method === 'GET'
      && e29.path === '/platform/schools/{id}/subscriptions/{subscriptionNo}/history'],
  ['a screen calls it', trail.includes("call('get-subscription-history'")],
  ['it is mounted on the subscriptions page',
    subsSourceFull.includes('<SubscriptionTrail schoolId={schoolId} />')],
  // Beside #28 and outside the has-a-subscription branch: a cancelled subscription's trail is
  // exactly what somebody comes here for, and #27 answering 404 says nothing about it.
  ['and mounted outside the has-a-subscription branch',
    subsSourceFull.indexOf('<SubscriptionTrail') > subsSourceFull.indexOf('<RenewCustomPeriod')],
  ['it names the subscription in the path, not just the school',
    /<EndpointTag[\s\S]{0,300}id="get-subscription-history"[\s\S]{0,300}subscriptionNo: subject/
      .test(trail)],
  ['the tag shows the URL it will really send, filters included',
    /<EndpointTag[\s\S]{0,300}id="get-subscription-history"[\s\S]{0,300}query=\{query\}/
      .test(trail)],
  ['the query is built at render, not inside the call',
    /const query = useMemo\(\(\) => \{/.test(trail)
      && trail.indexOf('const query = useMemo') < trail.indexOf("call('get-subscription-history'")],
  ['an empty filter box sends nothing rather than an empty parameter',
    trail.includes('if (reason.trim()) out.reason = reason.trim()')],
  // Every filter the API supports, and no filter it does not.
  [(() => {
    const supported = ['eventType', 'status', 'previousStatus', 'source', 'performedByDocsId',
      'sourceEventId', 'reason', 'effectiveFrom', 'effectiveTo', 'recordedFrom', 'recordedTo',
      'page', 'size', 'sort']
    const absent = supported.filter((f) => !trail.includes('out.' + f)
      && !new RegExp('\\{ *' + f + '[,} ]').test(trail))
    return `every filter the API takes is on the screen${absent.length ? ': missing ' + absent.join(', ') : ''}`
  })(), (() => {
    const supported = ['eventType', 'status', 'previousStatus', 'source', 'performedByDocsId',
      'sourceEventId', 'reason', 'effectiveFrom', 'effectiveTo', 'recordedFrom', 'recordedTo',
      'page', 'size', 'sort']
    return supported.every((f) => trail.includes('out.' + f)
      || new RegExp('\\{ *' + f + '[,} ]').test(trail))
  })()],
  // There is no plan-code filter on #29: a row stores TWO plan links, so the filter would have
  // to guess whether the caller means moved-off or moved-to.
  ['no plan-code filter is invented, unlike #28',
    !trail.includes('out.planCode')],
  // The two dates are different questions and both windows must be present and named apart.
  ['both date windows are on the screen, named after what they mean',
    trail.includes('out.effectiveFrom') && trail.includes('out.recordedFrom')
      && trail.includes('Took effect from') && trail.includes('Recorded from')],
  ['a row shows BOTH dates, because they are not the same date',
    trail.includes('row.effectiveAt') && trail.includes('row.recordedAt')],
  // Both ends of a transition, which no single filter can express.
  ['both ends of a transition are filterable',
    trail.includes('out.status') && trail.includes('out.previousStatus')
      && trail.includes('Moved TO this status') && trail.includes('Moved FROM this status')],
  ['every event type the enum holds is on the screen',
    ['CREATED', 'TRIAL_STARTED', 'ACTIVATED', 'PLAN_CHANGED', 'TERMS_CHANGED', 'RENEWED',
      'PAYMENT_PAST_DUE', 'SUSPENDED', 'RESUMED', 'CANCELLED', 'EXPIRED']
      .every((one) => trail.includes(`'${one}'`))],
  // A subscription number cannot go in a URL, so the two forms that work must both be offered.
  ['the path segment offers `current` and takes a pasted id',
    trail.includes("useState('current')") && /subscriptionId from the list above/.test(trail)],
  ['the two 404s are reachable from the screen',
    trail.includes("'SUB-nonsense'") && trail.includes("'6aa10000000000000000beef'")],
  ['the refusals are reachable: a size over the cap and a sort off the allow-list',
    trail.includes("'101'") && trail.includes("'reason,desc'")],
  ['nothing in it is disabled', !/disabled=|readOnly/.test(trail)],
  // The honest state of two filters that will always come back empty.
  ['the two filters that match nothing today say so rather than being hidden',
    trail.includes('out.performedByDocsId') && trail.includes('out.sourceEventId')
      && /nothing populates performedByDocsId yet/.test(trail)],
  ['a row says who is not recorded rather than showing a blank',
    /not recorded/.test(trail) && trail.includes('row.performedByDocsId')],
  ['a row whose plan was deleted says so rather than showing a blank',
    trail.includes('row.previousPlanCode === null')],
  ['a row with no reason says so rather than showing empty quotes',
    trail.includes('no reason was given')],
  ['an empty page reads as "nothing has happened", not as an error',
    trail.includes('Nothing has happened to this subscription yet')],
  ['the catalogue entry documents every refusal the endpoint gives',
    ['INVALID_PAGE', 'INVALID_PAGE_SIZE', 'INVALID_SORT_FIELD', 'INVALID_SORT_DIRECTION',
      'INVALID_DATE_RANGE', 'SCHOOL_NOT_FOUND', 'SUBSCRIPTION_NOT_FOUND']
      .every((code) => e29.errors.some((e) => e.code === code))],
  ['the catalogue offers every query parameter the API takes',
    ['page', 'size', 'sort', 'eventType', 'status', 'previousStatus', 'source',
      'performedByDocsId', 'sourceEventId', 'reason', 'effectiveFrom', 'effectiveTo',
      'recordedFrom', 'recordedTo']
      .every((key) => e29.queryParams.some((q) => q.key === key))],
  ['and names both path parameters',
    ['id', 'subscriptionNo'].every((n) => e29.pathParams.some((p) => p.name === n))],
]
for (const [label, ok] of trailChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe platform-wide list (#30) is wired to the screen')
// Its own file, so the whole source is the slice — no boundary hunting needed.
const allSubs = readFileSync('src/pages/platform/plans/AllSubscriptions.jsx', 'utf8')
const screensSource = readFileSync('src/screens.js', 'utf8')
const e30 = lookupEndpoint('list-all-subscriptions')
const allChecks = [
  ['the endpoint is in the catalogue, as a GET with no school in the path',
    Boolean(e30) && e30.method === 'GET' && e30.path === '/platform/subscriptions'
      && e30.pathParams.length === 0],
  ['a screen calls it', allSubs.includes("call('list-all-subscriptions'")],
  // A screen of its own: Subscriptions.jsx is school-scoped and opens with a School picker, and
  // #30 has no school at all.
  ['it is a screen of its own, not a card on the school-scoped page',
    screensSource.includes('screen: AllSubscriptions')
      && screensSource.includes("id: 'all-subscriptions'")
      && !subsSourceFull.includes('list-all-subscriptions')],
  ['and it is reachable from the navigation',
    /label: 'All subscriptions'/.test(screensSource)],
  ['it takes no school, so it asks for none',
    !/SchoolPicker/.test(allSubs) && !/schoolId:/.test(allSubs)],
  ['the tag shows the URL it will really send, filters included',
    /<EndpointTag[\s\S]{0,200}id="list-all-subscriptions"[\s\S]{0,200}query=\{query\}/
      .test(allSubs)],
  ['the query is built at render, not inside the call',
    /const query = useMemo\(\(\) => \{/.test(allSubs)
      && allSubs.indexOf('const query = useMemo') < allSubs.indexOf("call('list-all-subscriptions'")],
  ['an empty filter box sends nothing rather than an empty parameter',
    allSubs.includes('if (planCode.trim()) out.planCode = planCode.trim()')],
  [(() => {
    const supported = ['status', 'billingCycle', 'planCode', 'planVersion', 'autoRenew',
      'current', 'startDateFrom', 'startDateTo', 'endDateFrom', 'endDateTo', 'page', 'size',
      'sort']
    const absent = supported.filter((f) => !allSubs.includes('out.' + f)
      && !new RegExp('\\{ *' + f + '[,} ]').test(allSubs))
    return `every filter the API takes is on the screen${absent.length ? ': missing ' + absent.join(', ') : ''}`
  })(), (() => {
    const supported = ['status', 'billingCycle', 'planCode', 'planVersion', 'autoRenew',
      'current', 'startDateFrom', 'startDateTo', 'endDateFrom', 'endDateTo', 'page', 'size',
      'sort']
    return supported.every((f) => allSubs.includes('out.' + f)
      || new RegExp('\\{ *' + f + '[,} ]').test(allSubs))
  })()],
  // There is no schoolId filter on #30: naming one school IS #28.
  ['no schoolId filter is invented — that is #28',
    !allSubs.includes('out.schoolId')],
  ['there is no invented trial filter',
    !allSubs.includes('out.trial') && allSubs.includes("'TRIAL'")],
  // Rows are periods, not schools, and the screen must not quietly narrow.
  ['current=true is offered as a filter, not applied by default',
    allSubs.includes("out.current = current") && allSubs.includes("useState('')")
      && !/const \[current, setCurrent\] = useState\('true'\)/.test(allSubs)],
  ['the screen says rows are periods rather than schools',
    /billing periods, not schools/.test(allSubs)],
  // The refusal that separates #30 from #28.
  ['the sort list carries subscriptionNo, which this endpoint refuses and #28 allows',
    allSubs.includes("'subscriptionNo,asc'")],
  ['and the catalogue says why it is refused, and what the tiebreaker is instead',
    /generated \*\*per school\*\*/.test(e30.docs)
      && /neither unique nor a meaningful order/.test(e30.docs)
      && /tiebreaker is the \*\*row id\*\*/.test(e30.docs)],
  ['the refusals are reachable: a size over the cap and a sort off the allow-list',
    allSubs.includes("'101'") && allSubs.includes("'schoolName,asc'")],
  ['nothing in it is disabled', !/disabled=|readOnly/.test(allSubs)],
  // Every row must name its school, which is the whole difference from #28's row.
  ['a row names the school: name, subdomain and id',
    allSubs.includes('row.schoolName') && allSubs.includes('row.subdomain')
      && allSubs.includes('row.schoolId')],
  ['a row shows the SCHOOL\'s status as well as the subscription\'s',
    allSubs.includes('row.schoolStatus') && allSubs.includes('row.status')],
  ['a row shows current and periodEnded, not just the status',
    allSubs.includes('row.current') && allSubs.includes('row.periodEnded')],
  ['a row whose school was deleted says so rather than showing a blank',
    /the school document has gone/.test(allSubs)],
  ['a row whose plan was deleted says so too',
    allSubs.includes('row.planCode === null')],
  ['dates go through the readable helper, not raw',
    allSubs.includes('readableDateTime(row.currentPeriodStart)')
      && !/\{row\.currentPeriodEnd\}/.test(allSubs)],
  ['an empty page reads as "nobody has one", not as an error',
    /No school on the platform has one yet/.test(allSubs)],
  // Corrupt date values: the screen must name the cause rather than blame itself. The ten rows
  // that prompted this were repaired on 2026-09-09, so the panel is dormant — but the failure
  // mode is silent and one bad mongosh insert brings it back, so the diagnostic stays guarded.
  ['a 500 from the corrupt rows is explained, not shown as INTERNAL_ERROR',
    allSubs.includes('problem?.status === 500') && /not the endpoint/.test(allSubs)
      && /\$type: 'object'/.test(allSubs)],
  ['and there is a one-click way past it',
    allSubs.includes('Step around them')
      && /CORRUPT_ROW_WORKAROUND = '2099-01-01'/.test(allSubs)],
  ['the catalogue entry documents every refusal the endpoint gives',
    ['INVALID_PAGE', 'INVALID_PAGE_SIZE', 'INVALID_SORT_FIELD', 'INVALID_SORT_DIRECTION',
      'INVALID_DATE_RANGE'].every((code) => e30.errors.some((e) => e.code === code))],
  // No school is looked up, so there is nothing to 404 on.
  ['and documents that there is no 404',
    !e30.errors.some((e) => e.status === 404)],
  ['the catalogue offers every query parameter the API takes',
    ['page', 'size', 'sort', 'status', 'billingCycle', 'planCode', 'planVersion', 'autoRenew',
      'current', 'startDateFrom', 'startDateTo', 'endDateFrom', 'endDateTo']
      .every((key) => e30.queryParams.some((q) => q.key === key))],
]
for (const [label, ok] of allChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe platform controller was rebased without moving a URL')
const controller = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/controllers/plans/PlatformSubscriptionController.java',
  'utf8')
// #30 has no school in its URL, so the class cannot be pinned to one — Spring appends a method's
// path to the class's. The school segment moved onto each of the other ten methods, and every
// URL has to be exactly what it was.
const classPath = (controller.match(/@RequestMapping\("([^"]*)"\)/) || [, ''])[1]
const mapped = [...controller.matchAll(/@(Get|Post|Patch|Put|Delete)Mapping\("([^"]*)"\)/g)]
  .map((m) => `${m[1].toUpperCase()} ${classPath}${m[2]}`)
const expected = [
  'POST /platform/schools/{schoolId}/subscriptions',
  'PATCH /platform/schools/{schoolId}/subscriptions/{subscriptionNo}',
  'POST /platform/schools/{schoolId}/subscriptions/{subscriptionNo}/change-plan',
  'POST /platform/schools/{schoolId}/subscriptions/{subscriptionNo}/renew',
  'POST /platform/schools/{schoolId}/subscriptions/{subscriptionNo}/suspend',
  'POST /platform/schools/{schoolId}/subscriptions/{subscriptionNo}/resume',
  'POST /platform/schools/{schoolId}/subscriptions/{subscriptionNo}/cancel',
  'GET /platform/schools/{schoolId}/subscription',
  'GET /platform/schools/{schoolId}/subscriptions',
  'GET /platform/schools/{schoolId}/subscriptions/{subscriptionNo}/history',
  'GET /platform/subscriptions',
]
const rebaseChecks = [
  ['the class is mapped at /platform, not at one school', classPath === '/platform'],
  ['every method carries its own path', mapped.length === 11],
  [(() => {
    const missing = expected.filter((one) => !mapped.includes(one))
    return `all eleven URLs are exactly what they were${missing.length ? ': lost ' + missing.join(' | ') : ''}`
  })(), expected.every((one) => mapped.includes(one))],
  [(() => {
    const extra = mapped.filter((one) => !expected.includes(one))
    return `and no URL was invented${extra.length ? ': ' + extra.join(' | ') : ''}`
  })(), mapped.every((one) => expected.includes(one))],
  ['the rebase says why it was needed',
    /cannot host #30/.test(controller)],
  // The one endpoint here with no tenant. The repository keeps it a separate method rather than a
  // nullable school id, so the boundary cannot be forgotten by accident.
  ['#30 uses a separate repository method, not a nullable school id',
    readFileSync('../backend/src/main/java/com/orbitastra/backend/services/plans/PlatformSubscriptionService.java', 'utf8')
      .includes('schoolSubscription.searchAcrossSchools(request, planIds')],
  ['and the tiebreaker is the row id, because subscriptionNo is per-school',
    /PLATFORM_SUBSCRIPTION_ORDER = Sort\.by\(\s*Sort\.Order\.asc\("currentPeriodEnd"\),\s*Sort\.Order\.asc\("id"\)\)/
      .test(readFileSync('../backend/src/main/java/com/orbitastra/backend/services/plans/PlatformSubscriptionService.java', 'utf8'))],
]
for (const [label, ok] of rebaseChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nDates on screen are readable, per the project rule')
const dateRuleChecks = [
  ['there is one helper for it, in lib/dates.js',
    /export function readableDateTime\(/.test(datesLib)],
  // "Friday 8 October 2027 11:59 PM" — weekday, day, month, year, 12-hour clock, AM/PM.
  ['it renders a weekday, a full month and a 12-hour clock',
    /weekday: 'long'/.test(datesLib) && /month: 'long'/.test(datesLib)
      && /hour12: true/.test(datesLib)],
  // Scoped to readableDateTime's OWN body: startOfDayInZone also builds an Intl formatter, and
  // a whole-file check passed happily while this function was switched to the browser's locale.
  ['the locale is pinned, not taken from the browser',
    (() => {
      const from = datesLib.indexOf('export function readableDateTime(')
      const to = datesLib.indexOf('\nexport function', from + 1)
      const body = datesLib.slice(from, to === -1 ? undefined : to)
      return /Intl\.DateTimeFormat\('en-GB'/.test(body)
        && !/DateTimeFormat\(undefined/.test(body)
        && !/toLocaleString|toLocaleDateString|toLocaleTimeString/.test(body)
    })()],
  ['AM and PM are upper case, with a space before them',
    /dayPeriod \|\| ''\)\.toUpperCase\(\)/.test(datesLib)
      && /\$\{at_\.minute\} \$\{/.test(datesLib)],
  ['a null date reads as a dash rather than the word null',
    /if \(!instant\) return '—'/.test(datesLib)],
  ['the trail renders its dates through it, not raw',
    trail.includes('readableDateTime(row.effectiveAt)')
      && trail.includes('readableDateTime(row.recordedAt)')
      && !/\{row\.effectiveAt\}/.test(trail)],
  // Both tables sit on one screen, so they must not disagree about the format.
  ['#28 renders its period dates through it too',
    history.includes('readableDateTime(row.currentPeriodStart)')],
  // The raw instant is still available on purpose: this is an API testing tool, and next to a
  // date picker the exact value on the wire is the thing somebody needs.
  ['the exact stored instant is still available for "stored as" hints',
    /export function readableInstant\(/.test(datesLib)
      && /API testing tool/.test(datesLib)],
]
for (const [label, ok] of dateRuleChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe year detail header shows the stored running flag')
const yearHeaderSource = readFileSync('src/pages/school/core/AcademicYearDetail.jsx', 'utf8')
const header = yearHeaderSource.slice(yearHeaderSource.indexOf('<h1 className="page-title mono">'),
  yearHeaderSource.indexOf('<span className="toolbar-spacer" />'))
const headerChecks = [
  ['the header badges include the running flag', header.includes('year.isThisYearRunning')],
  // Beside `current`, which is derived from the dates, because the two can disagree.
  ['and it sits between `current` and `results`',
    header.indexOf('year.current ?') < header.indexOf('year.isThisYearRunning')
      && header.indexOf('year.isThisYearRunning') < header.indexOf('year.resultsLocked')],
  ['true, false and null each read differently',
    /'running'/.test(header) && /'not running'/.test(header) && /'running not set'/.test(header)],
  ['null is told apart from false, not folded into it',
    /year\.isThisYearRunning === false \?/.test(header)],
  ['the hover says which way a disagreement runs',
    /Marked as running, but today is outside its dates/.test(header)
      && /the school is not using it/.test(header)],
  ['and says so when the record simply does not know',
    /predates the field/.test(header)],
  // The same three states as the list table, so one screen never contradicts the other.
  ['the list table and the header agree on the three labels',
    ['running', 'not running'].every((word) =>
      header.includes(`'${word}'`)
        && readFileSync('src/pages/school/core/AcademicYears.jsx', 'utf8').includes(`'${word}'`))],
]
for (const [label, ok] of headerChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nEnding the academic year is wired to the screen')
const yearDetail = readFileSync('src/pages/school/core/AcademicYearDetail.jsx', 'utf8')
const eEnd = lookupEndpoint('end-academic-year')
// From the card's own COMMENT block to the function after it. Starting at the function itself
// would leave the javadoc above it outside the slice, and that comment is where the card records
// why it is not one of the gates — which is exactly what the check below reads.
const endFn = yearDetail.indexOf('function EndTheYear(')
const endStart = yearDetail.lastIndexOf('/**', endFn)
const endStop = Math.min(...[...yearDetail.matchAll(/^function \w+\(/gm)]
  .map((m) => m.index).filter((i) => i > endFn).concat([yearDetail.length]))
const endCard = yearDetail.slice(endStart, endStop)
const yearEndChecks = [
  ['the endpoint is in the catalogue, as a POST on the named year',
    Boolean(eEnd) && eEnd.method === 'POST'
      && eEnd.path === '/schools/current/academic-years/{name}/end'],
  ['it is a school-surface endpoint and sends the tenant header',
    eEnd.schoolSurface === true
      && eEnd.headers.some((h) => h.key === 'X-School-Subdomain' && h.enabled)],
  ['a screen calls it', endCard.includes("'end-academic-year'")],
  ['it is mounted on the year detail page',
    yearDetail.includes('<EndTheYear year={year} name={name} busy={busy} onRun={run} />')],
  // A gate is a switch and this is not: there is no un-end, so it must not sit in the Gates card
  // pretending to be reversible.
  ['it is its own card, not one of the two-way gates',
    endCard.includes('function EndTheYear') && !/openEndpoint: 'end-academic-year'/.test(yearDetail)
      && /not one of the gates/i.test(endCard)],
  ['the tag names the year in the path',
    /<EndpointTag[\s\S]{0,200}id="end-academic-year"[\s\S]{0,200}pathParams=\{\{ name \}\}/
      .test(endCard)],
  // It says what will happen before it happens, rather than being a button you press to find out.
  ['it shows how many days would be cut before you press it',
    endCard.includes('daysCut') && /day\(s\) earlier than planned/.test(endCard)],
  ['it names the refusal the year is currently heading for',
    endCard.includes("'ACADEMIC_YEAR_ALREADY_ENDED'")
      && endCard.includes("'ACADEMIC_YEAR_NOT_STARTED'")],
  ['and explains the already-ended one as an EXTENSION, which is the real trap',
    /move that date FORWARD/.test(endCard)],
  ['it says the holiday refusal does not delete anything',
    /HOLIDAYS_OUTSIDE_NEW_RANGE/.test(endCard) && /not deleted/.test(endCard)],
  ['it says a short year is allowed here and refused by the date editor',
    /implausible range/.test(endCard)],
  ['it says enrollment and result locking are untouched',
    /Enrollment and result locking\s*\n?\s*are left alone/.test(endCard.replace(/\s+/g, ' '))
      || /left alone/.test(endCard)],
  ['nothing in it is disabled', !/disabled=|readOnly/.test(endCard)],
  // The flag is the endpoint's main effect. Until it was returned, nothing could observe it.
  ['the screen shows isThisYearRunning', endCard.includes('year.isThisYearRunning')],
  ['and says null means not running, for years predating the field',
    /predates the field/.test(endCard)],
  ['and tells the reader to read it beside `current`, not instead of it',
    /beside `current`/.test(endCard)],
  ['the catalogue documents every refusal the endpoint gives',
    ['TENANT_NOT_RESOLVED', 'ACADEMIC_YEAR_NOT_FOUND', 'ACADEMIC_YEAR_NOT_STARTED',
      'ACADEMIC_YEAR_ALREADY_ENDED', 'HOLIDAYS_OUTSIDE_NEW_RANGE']
      .every((code) => eEnd.errors.some((e) => e.code === code))],
  ['and carries the worked cases, including the already-ended trap',
    eEnd.examples.length >= 9
      && eEnd.examples.some((x) => /ALREADY FINISHED/.test(x.name))],
  ['the response fields the screen reads are the ones the catalogue lists',
    ['endDate', 'durationDays', 'isThisYearRunning', 'nextStep']
      .every((f) => eEnd.responseFields.includes(f))],
]
for (const [label, ok] of yearEndChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe response exposes what the write changes')
const yearResponse = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/dto/core/academicyear/response/AcademicYearResponse.java',
  'utf8')
const exposeChecks = [
  // A field the API writes but never returns cannot be checked from here, which is what this
  // whole app is for. It was added for that reason and the DTO says so.
  ['AcademicYearResponse returns isThisYearRunning',
    /Boolean isThisYearRunning,/.test(yearResponse)
      && /year\.getIsThisYearRunning\(\)/.test(yearResponse)],
  ['and records why it had to be added',
    /had no way to be checked/.test(yearResponse)],
  ['`current` is still derived, not stored — the two answer different questions',
    /derived here, never stored/.test(yearResponse)
      && /A finished year can still read true here/.test(yearResponse)],
  // The default changed to true, so several years can read true at once. The docs must say that
  // rather than claiming the flag narrows anything today.
  ['the field table says the default is true, not false',
    /`isThisYearRunning` \| Boolean, required \| \*\*`true`\*\* at create/
      .test(readFileSync('../backend/src/main/java/com/orbitastra/backend/controllers/core/README.md', 'utf8'))],
  ['and warns that several years can read true at once',
    /several\s+years can read true at once/.test(
      readFileSync('../backend/src/main/java/com/orbitastra/backend/controllers/core/README.md', 'utf8')
        .replace(/\s+/g, ' '))],
]
for (const [label, ok] of exposeChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nEvery capped dropdown can search past its cap')
const planOptions = readFileSync('src/pages/platform/plans/planOptions.js', 'utf8')
const planRow = readFileSync('src/pages/platform/plans/PlanSearchRow.jsx', 'utf8')
const yearPickerSource = readFileSync('src/components/AcademicYearPicker.jsx', 'utf8')
const everyDropdownChecks = [
  // The two subscription modals asked the same question with the same copied call. One shared
  // hook means the cap gets fixed in one place, not two.
  ['the plan list and its search are shared, not copied into both modals',
    /export function usePlanOptions/.test(planOptions)
      && (subsSourceFull.match(/usePlanOptions\(open\)/g) || []).length === 2],
  ['and neither modal still has its own list-plans call',
    !/call\('list-plans'/.test(subsSourceFull)],
  ['the shared loader is still capped at 100',
    /status: 'ACTIVE', page: 0, size: 100 \}/.test(planOptions)],
  ['and there is a second call that sends ?search=',
    /size: 100, search: needle/.test(planOptions)],
  ['both modals render the search row',
    (subsSourceFull.match(/<PlanSearchRow picker=\{picker\} \/>/g) || []).length === 2],
  ['the row has a button and Enter runs it',
    /Search all plans/.test(planRow)
      && /event\.key === 'Enter'[\s\S]{0,80}runSearch\(\)/.test(planRow)],
  // A native select cannot hold a search box, so typing does nothing until the button is
  // pressed. Saying so beats leaving somebody typing into a box that looks broken.
  ['it says typing filters nothing, because a native select cannot hold a search',
    /Typing filters nothing here/.test(planRow) && /cannot hold a search/i.test(planRow)],
  ['editing the box drops the previous answer',
    /if \(found\) setFound\(null\)/.test(planOptions)],
  ['a server answer replaces the loaded page rather than being re-filtered',
    /found \?\? loaded/.test(planOptions)],
  ['you can get back to the loaded page',
    /Back to the first hundred/.test(planRow)],
  ['the note says which list it is describing',
    /found across every published plan/.test(planRow)
      && /Showing the first \$\{loadedCount\} published plans/.test(planRow)],
  ['nothing in the row is disabled', !/disabled=|readOnly/.test(planRow)],
  // The year picker is the one dropdown that needs none of this, and the reason is worth
  // pinning: list-academic-years returns every year for the school, with no page and no cap.
  ['the year picker sends no page or size, because that endpoint returns them all',
    /call\('list-academic-years', \{ label: 'Years to choose from' \}\)/.test(yearPickerSource)
      && !/size: \d+/.test(yearPickerSource)],
  ['and the backend really does return every year, uncapped',
    /findBySchoolIdOrderByStartDateDesc\(school\.getId\(\)\)/.test(
      readFileSync('../backend/src/main/java/com/orbitastra/backend/services/core/AcademicYearService.java', 'utf8'))],
]
for (const [label, ok] of everyDropdownChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe school picker can search past the hundred it loads')
const schoolPicker = readFileSync('src/components/SchoolPicker.jsx', 'utf8')
const pickerCss = readFileSync('src/styles/components.css', 'utf8')
const searchChecks = [
  // The list endpoint caps a page at 100. On a platform with 3091 schools that is 3% of them,
  // so a name outside the page looks exactly like a name that does not exist.
  ['the loaded page is still capped at 100',
    /size: 100, sort: 'name,asc' \},/.test(schoolPicker)],
  ['there is a second call that sends ?search=',
    /search: needle/.test(schoolPicker)
      && /call\('list-schools'[\s\S]{0,200}search: needle/.test(schoolPicker)],
  ['and a button that runs it',
    /onClick=\{runSearch\}/.test(schoolPicker) && /Search all schools/.test(schoolPicker)],
  ['Enter runs the server search too',
    /event\.key === 'Enter'[\s\S]{0,160}runSearch\(\)/.test(schoolPicker)],
  // A server answer is already filtered by name AND subdomain; filtering it again locally would
  // drop rows the API deliberately matched.
  ['a server answer is not re-filtered locally',
    /if \(found\) return found/.test(schoolPicker)],
  // Results that no longer match the box are worse than none.
  ['editing the box drops the previous answer',
    /const retype = \(next\) => \{[\s\S]{0,140}if \(found\) setFound\(null\)/.test(schoolPicker)],
  ['and the input goes through it', /onChange=\{\(event\) => retype\(event\.target\.value\)\}/
    .test(schoolPicker)],
  ['you can get back to the loaded list',
    /Back to the loaded list/.test(schoolPicker)],
  // The two empty states mean different things and must not share wording.
  ['a local miss says the list only holds the first hundred',
    /only holds the first hundred/.test(schoolPicker.replace(/\s+/g, ' '))],
  ['a server miss says it checked every school',
    /That is the API&rsquo;s answer\s+for every school/.test(schoolPicker.replace(/\s+/g, ' '))
      || /answer for every school/.test(schoolPicker.replace(/\s+/g, ' '))],
  ['the footer says which list it is counting',
    /found across every school/.test(schoolPicker)
      && /of \$\{schools\.length\} loaded/.test(schoolPicker)],
  ['and warns when the loaded page is full',
    /search to reach the rest/.test(schoolPicker)],
  // The typed-subdomain escape hatch survived Enter being repurposed.
  ['the typed-subdomain escape hatch is still reachable, as a click',
    /use &ldquo;\{query\.trim\(\)\}&rdquo; as the subdomain anyway/.test(schoolPicker)],
  ['the trigger can name a school that came from a search, not just the loaded page',
    /\[\.\.\.\(found \?\? \[\]\), \.\.\.\(schools \?\? \[\]\)\]/.test(schoolPicker)],
  ['nothing in the picker is disabled', !/disabled=|readOnly/.test(schoolPicker)],
  ['the search row has a style of its own',
    /\.picker-search-actions \{/.test(pickerCss)],
]
for (const [label, ok] of searchChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe year list labels the stored running flag')
const yearsList = readFileSync('src/pages/school/core/AcademicYears.jsx', 'utf8')
const listHead = yearsList.slice(yearsList.indexOf('<thead>'), yearsList.indexOf('</thead>'))
const listBody = yearsList.slice(yearsList.indexOf('<tbody>'), yearsList.indexOf('</tbody>'))
// Strip JSX comments before counting cells: a <td> mentioned inside one is not a column.
const listBodyCode = listBody.replace(/\{\/\*[\s\S]*?\*\/\}/g, '')
const listChecks = [
  ['the table has a Running column', /<th>Running<\/th>/.test(listHead)],
  // Next to `current`, because the whole point is that the two can disagree.
  ['and it sits beside the Year column, where the `current` badge is',
    listHead.indexOf('<th>Year</th>') < listHead.indexOf('<th>Running</th>')
      && listHead.indexOf('<th>Running</th>') < listHead.indexOf('<th>Runs</th>')],
  [(() => {
    const th = (listHead.match(/<th[ />]/g) || []).length
    const td = (listBodyCode.match(/<td[ >]/g) || []).length
    return `every header cell has a body cell (${th} vs ${td})`
  })(), (listHead.match(/<th[ />]/g) || []).length
    === (listBodyCode.match(/<td[ >]/g) || []).length],
  ['the cell reads the stored flag', listBodyCode.includes('year.isThisYearRunning')],
  ['true, false and null each read differently',
    /'running'/.test(listBodyCode) && /'not running'/.test(listBodyCode)
      && /'not set'/.test(listBodyCode)],
  // Null is not false. Every year written before the field existed reads null, and calling that
  // "not running" would state something the record does not say.
  ['null is told apart from false, not folded into it',
    /year\.isThisYearRunning === false \? /.test(listBodyCode)],
  // The disagreement is the state somebody opens this table to check.
  ['a row where the flag and the dates disagree says so',
    /year\.isThisYearRunning !== year\.current/.test(listBodyCode)
      && /≠ dates/.test(listBodyCode)],
  ['and the hover explains which way round it is',
    /Marked as running, but today is outside its dates/.test(listBodyCode)
      && /the school is not using it/.test(listBodyCode)],
  ['a null flag is not reported as a disagreement',
    /year\.isThisYearRunning != null/.test(listBodyCode)],
  ['`current` is still shown, and still derived',
    /year\.current \? <Badge tone="brand">current<\/Badge>/.test(listBodyCode)],
]
for (const [label, ok] of listChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nThe academic-year picker is a mode beside the school')
const yearPicker = readFileSync('src/components/AcademicYearPicker.jsx', 'utf8')
const topbar = readFileSync('src/components/Topbar.jsx', 'utf8')
const provider = readFileSync('src/api/ApiProvider.jsx', 'utf8')
const storeSource = readFileSync('src/lib/store.js', 'utf8')
const pickerChecks = [
  ['it is in the top bar, next to the school',
    topbar.includes('<AcademicYearPicker />')
      && topbar.indexOf('<ActingAs />') < topbar.indexOf('<AcademicYearPicker />')],
  // No platform endpoint reads the tenant header and none names a year, so up there it would be
  // a control that changes nothing.
  ['and only on the school surface',
    /onSchoolSurface \? <AcademicYearPicker \/> : null/.test(topbar)],
  ['it reads the years from the school-surface list endpoint',
    yearPicker.includes("call('list-academic-years'")],
  // The list is one request most screens never use, and on a platform screen it would 400.
  ['the list loads when the popover first opens, not on mount',
    /onClick=\{\(\) => \{ setOpen\(!open\); if \(!open\) load\(false\) \}\}/.test(yearPicker)],
  ['the chosen year lives in the provider, not in a screen',
    /const \[actingAcademicYear, setActingAcademicYear\]/.test(provider)
      && /actingAcademicYear \}\)/.test(provider.replace(/\s+/g, ' '))],
  ['and is remembered between reloads',
    /loadActingAcademicYear\(\)/.test(storeSource)
      && /saveActingAcademicYear\(name\)/.test(storeSource)],
  // THE ONE THAT MATTERS. A year is identified by NAME, and a name is unique only within a
  // school — "2026-2027" is a different document per tenant. Keeping it across a school switch
  // would point every year-scoped call at a year the new school may not have.
  ['choosing a school CLEARS the year',
    /chooseSchool = useCallback[\s\S]{0,900}setActingAcademicYear\(null\)[\s\S]{0,120}saveActingAcademicYear\(null\)/
      .test(provider)],
  // Flatten the comment markers as well as the whitespace: a wrapped `//` line leaves a `//`
  // in the middle of the sentence, which is what made the first version of this check miss.
  ['and the code says why a name cannot survive a school change',
    /a name is unique only within a school/
      .test(provider.replace(/^\s*\/\/ ?/gm, '').replace(/\s+/g, ' '))],
  // Loaded years belong to the school that was current when they were read.
  ['a stale list is not shown after a school switch',
    /cache\.subdomain === actingSubdomain \? cache\.years : null/.test(yearPicker)],
  ['and that staleness is derived, not reset in an effect',
    !/useEffect\(\(\) => \{ setYears/.test(yearPicker)
      && /worked out during render rather than reset in an effect/
        .test(yearPicker.replace(/^\s*\/\/ ?/gm, '').replace(/\s+/g, ' '))],
  ['with no school it explains itself rather than disappearing',
    /Choose a school first/.test(yearPicker)],
  ['a school with no years says so',
    /has no academic years yet/.test(yearPicker)],
  ['nothing in it is disabled', !/disabled=|readOnly/.test(yearPicker)],
  // `current` is derived from the dates and isThisYearRunning is stored; they can disagree, and
  // a picker that showed only one would hide that.
  ['a row shows both `today` and `running`, because the two can disagree',
    yearPicker.includes('year.current') && yearPicker.includes('year.isThisYearRunning')],
  ['it shows each year\'s dates, not just its name',
    yearPicker.includes('year.startDate') && yearPicker.includes('year.endDate')],
  ['it can be cleared', /onClick=\{\(\) => pick\(null\)\}/.test(yearPicker)],
]
for (const [label, ok] of pickerChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nEndpoint coverage')
const catalogue = readFileSync('src/config/endpoints.js', 'utf8')
const allIds = [...catalogue.matchAll(/\bid:\s*"([a-z][a-z0-9-]+)",\s*\n\s*name:/g)].map((m) => m[1])
/** Every .js and .jsx under src, except the generated catalogue itself. */
function sourceFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name)
    if (entry.isDirectory()) return sourceFiles(path)
    if (!/\.jsx?$/.test(entry.name) || path.includes('config/endpoints')) return []
    return [path]
  })
}

// An id is counted as reached if it appears as a quoted string anywhere — which covers a literal
// `call('x')`, an `id="x"` on a tag, and the lookup tables that hold `endpoint: 'x'` and are
// called with a variable. Under-counting those was how a first version of this check reported
// eleven false misses.
const sources = sourceFiles('src').map((f) => readFileSync(f, 'utf8')).join('\n')
const unreached = allIds.filter(
  (id) => !sources.includes(`'${id}'`) && !sources.includes(`"${id}"`),
)
console.log(unreached.length === 0
  ? `  ok     all ${allIds.length} endpoints are reachable from a screen`
  : `  MISS   ${unreached.length} unreachable: ${unreached.join(', ')}`)
if (unreached.length) fail++

console.log('\nThe gates on the academic-year writes')
// Each write is checked by the text of its own method body rather than by counting calls across
// the file: a count of 33 stays 33 if one endpoint loses its gate and another gains it twice.
const gateWrites = [
  ['updateDates', 'PATCH  /{name}/dates'],
  ['replaceCalendar', 'PUT    /{name}/holidays'],
  ['addHoliday', 'POST   /{name}/holidays'],
  ['updateHoliday', 'PATCH  /{name}/holidays/{date}'],
  ['removeHoliday', 'DELETE /{name}/holidays/{date}'],
  ['removeHolidaysByType', 'DELETE /{name}/holidays?type='],
  ['generateWeeklyOff', 'POST   /{name}/holidays/generate-weekly-off'],
  ['enableEnrollment', 'POST   /{name}/enrollment/enable'],
  ['disableEnrollment', 'POST   /{name}/enrollment/disable'],
  ['lockResults', 'POST   /{name}/results/lock'],
  ['unlockResults', 'POST   /{name}/results/unlock'],
]
// The reads must stay ungated: looking at a calendar is not an action on it, and a school that
// has stopped paying still has to be able to read its own records.
const yearReads = ['list', 'current', 'getOne', 'getHolidays', 'getDay', 'workingDays']

// Comments are stripped before anything is checked. `includes('gate.requireActiveSchool')`
// matches a COMMENTED-OUT call exactly as happily as a live one, so without this every check
// below passes on a gate somebody disabled with two slashes. Found by mutation: prefixing
// `// X ` to one gate call left the whole suite green.
const bodyOf = (name) => {
  const at = yearController.indexOf(` ${name}(`)
  if (at < 0) return ''
  const open = yearController.indexOf('{', at)
  return yearController.slice(open, yearController.indexOf('\n    }', open))
    .split('\n').map((line) => line.replace(/\/\/.*$/, '')).join('\n')
}

const gateChecks = []
for (const [method, route] of gateWrites) {
  const body = bodyOf(method)
  gateChecks.push(
    [`${route} runs gate 1`, body.includes('gate.requireActiveSchool(school)')],
    [`${route} runs gate 2`, body.includes('gate.requireUsableSubscription(school)')],
    [`${route} runs gate 4`, body.includes('gate.requireYearMarkedAsRunning(school, name)')],
    // Gate 3 adds "today is inside the year's dates" to gate 4. It would refuse the ordinary
    // use of every one of these: next year's holiday calendar is built in February, admissions
    // open months ahead, and marks are published after the last school day. See the README.
    [`${route} does NOT run gate 3`, !body.includes('requireRunningAcademicYear')],
    // School, then subscription, then year. A closed school's billing state is nobody's
    // business, so the cheaper and more fundamental refusal has to come first.
    [`${route} asks the school before the subscription`,
      body.indexOf('requireActiveSchool') < body.indexOf('requireUsableSubscription')],
    [`${route} asks the subscription before the year`,
      body.indexOf('requireUsableSubscription') < body.indexOf('requireYearMarkedAsRunning')],
  )
}
for (const read of yearReads) {
  gateChecks.push([`GET ${read} stays ungated`, !bodyOf(read).includes('gate.require')])
}
gateChecks.push(
  // #18 has no year to check yet, so it runs 1 and 2 and stops there.
  ['POST /academic-years runs gates 1 and 2',
    bodyOf('create').includes('gate.requireActiveSchool(school)')
    && bodyOf('create').includes('gate.requireUsableSubscription(school)')],
  ['and not gate 4, because no year exists yet',
    !bodyOf('create').includes('requireYearMarkedAsRunning')],
  // The overload the controller needs: it has the name from the URL and does not load documents.
  ['ActionGate can gate a year by name',
    actionGateSrc.includes('requireYearMarkedAsRunning(School school, String name)')],
  ['and it scopes the lookup to the school, not the name alone',
    actionGateSrc.includes('findBySchoolIdAndName(school.getId()')],
  ['an unknown year is a 404, not a not-running 409',
    actionGateSrc.includes("ApiException.notFound(\"ACADEMIC_YEAR_NOT_FOUND\"")],
  // Gate 3 delegates to gate 4 so the flag rule has one home. If that ever stops being true,
  // "does NOT run gate 3" above would no longer mean the flag is unchecked.
  ['gate 3 still delegates its flag check to gate 4',
    actionGateSrc.includes('requireYearMarkedAsRunning(year);')],
  ['a null flag is a refusal, so the gate fails closed',
    actionGateSrc.includes('!Boolean.TRUE.equals(year.getIsThisYearRunning())')],
  // The README is the only place that records WHY gate 3 is absent. Without it the next reader
  // sees eleven endpoints missing a gate that exists.
  ['the README says which gates run where', coreReadme.includes('Every write runs gates 1, 2 and 4')],
  ['and why gate 3 is on none of them', coreReadme.includes('Gate 4 and not gate 3')],
  ['and warns to lock results before ending the year',
    coreReadme.includes('Lock results before ending the year')],
  ['and records the two-codes-for-one-condition overlap',
    coreReadme.includes('SCHOOL_NOT_EDITABLE') && coreReadme.includes('SCHOOL_NOT_ACTIVE')],
  // Fixed 2026-09-10. The README claimed this was still broken until then.
  ['the README no longer says the server clock decides "today"',
    !coreReadme.includes('### The server clock decides what "today" is')],
)
for (const [label, ok] of gateChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nAcademics — classes (#12)')
const classesScreen = readFileSync('src/pages/school/academics/Classes.jsx', 'utf8')
const classCatalogue = catalogue.slice(catalogue.indexOf('GROUP_ACADEMICS_CLASSES'))
const classEntry = classCatalogue.slice(0, classCatalogue.indexOf('export const API_CATALOG'))
const classChecks = [
  // A class is addressed by its document id. Twelve other documents store classDocsId and not
  // one stores a code, so a code anywhere here would be reintroducing the field that was removed.
  ['no classCode survives in the catalogue entry', !/classCode["':]/.test(classEntry)],
  ['nor on the screen', !/classCode["':]/.test(classesScreen)],
  ['the response carries the id', classEntry.includes('"schoolClassId"')],
  ['and the screen shows it, since there is no code to show instead',
    classesScreen.includes('row.schoolClassId')],
  ['the id is captured for later calls', classEntry.includes('variable: "schoolClassId"')],

  // The year is a path parameter. Putting it in the body would be two places to say one thing.
  ['the year is a path parameter', classEntry.includes('name: "year"')],
  ['and the screen sends it as one', classesScreen.includes('pathParams: { year: sendYear }')],
  ['the body does not carry a year', !/"academicYear":/.test(classEntry)],

  // The class is created empty. Both fields are absent from the request on purpose.
  ['the catalogue says sections and subjects are not accepted',
    classEntry.includes('SECTIONS AND SUBJECTS ARE NOT ACCEPTED HERE')],
  ['and proves it with a case that sends them anyway',
    classEntry.includes('SECTIONS AND SUBJECTS ARE IGNORED')],
  ['the screen says the list returns counts, not the embedded rows',
    classesScreen.includes('The list returns COUNTS, never the')],

  // The refusals a tester needs to be able to reach.
  ['CLASS_NAME_TAKEN is documented', classEntry.includes('CLASS_NAME_TAKEN')],
  ['and the per-year scoping it implies', classEntry.includes('THE SAME NAME IN ANOTHER YEAR')],
  ["another school's programme is a documented case",
    classEntry.includes("ANOTHER SCHOOL'S AFFILIATION PROGRAMME")],
  ['all three gates are listed', ['SCHOOL_NOT_ACTIVE', 'SUBSCRIPTION_NOT_USABLE',
    'ACADEMIC_YEAR_NOT_RUNNING'].every((code) => classEntry.includes(code))],
  ['the index bug is recorded, since it would have hit the second class ever created',
    classEntry.includes('one class per')],

  // #28 is built, so the table is a real read now - and the screen must no longer claim
  // otherwise. It said "not a read of the server" while #28 was a plan.
  ['the table is a real read, and no longer claims to be session-only',
    !classesScreen.includes('not a read of the')],
  ['it loads from #28', classesScreen.includes("call('list-school-classes'")],
  ['it still names #29 as what would give one class in full', classesScreen.includes('#29')],

  // The year box starts from the top bar but must stay typeable - clearing it is how the 404
  // is tested, and a locked field would make that refusal unreachable.
  ['the year box can be typed over',
    /onChange=\{\(event\) => setYearOverride\(event\.target\.value\)\}/.test(classesScreen)],
  ['and falls back to the top bar', classesScreen.includes('yearOverride ?? year ?? ')],
]

// #13 — the edit. Everything here exists because a class is addressed by id.
const editEntry = classCatalogue.slice(classCatalogue.indexOf('update-school-class'),
  classCatalogue.indexOf('list-school-classes'))
classChecks.push(
  ['#13 is a PATCH on the id', /path: "\/schools\/current\/academic-years\/{year}\/classes\/{id}"/.test(editEntry)],
  ['it takes both the year and the id as path parameters',
    editEntry.includes('name: "year"') && editEntry.includes('name: "id"')],
  ['no field is required — every one is optional', editEntry.includes('requiredFields: [],')],
  ['an empty body is documented as a refusal, not a no-op',
    editEntry.includes('NOTHING_TO_UPDATE')],
  ['a blank name is documented as refused rather than a clear',
    editEntry.includes('CLASS_NAME_REQUIRED')],
  ['the clearing rule is written down — only the programme can be cleared',
    editEntry.includes('clears it') && editEntry.includes('CLASS_NAME_REQUIRED')],
  ['a real id under the wrong year is a documented case',
    editEntry.includes('A REAL ID UNDER THE WRONG YEAR')],
  ['and resending your own name is too', editEntry.includes('RESEND THE SAME NAME')],

  // The form must send only what changed, or "detach the programme" and "leave it alone" become
  // the same request.
  ['the edit form sends only what changed', classesScreen.includes('const changed = ()')],
  // After an edit the screen RE-READS rather than patching its local row. With #28 built the
  // server is the cheaper source of truth, and a local patch would drift from the filters.
  ['an edit re-reads the list rather than patching the row locally',
    classesScreen.includes('onSaved={() => { setEditing(null); load() }}')],
  ['and a create does the same', classesScreen.includes('onCreated={() => { setCreating(false); load() }}')],
  ['it remounts per class rather than copying the row in an effect',
    classesScreen.includes('key={row.schoolClassId}')],
  ['an unchanged form still submits, so the 400 stays reachable',
    classesScreen.includes('sends {} on purpose')],
  // Both halves of the URL stay typeable: two of #13's refusals are only reachable that way.
  ['the year it aims at can be retyped', classesScreen.includes("aim('year')")],
  ['and so can the class id', classesScreen.includes("aim('id')")],
  ['and both are what the request uses',
    classesScreen.includes('pathParams: { year: target.year, id: target.id }')],
)


// #28 — the list. Production shape: filters, paging, sorting, and the traps each one carries.
// The three entries sit in the order they were added: create, update, list. So the list entry
// runs from its own id to the end of the group, not to another id that precedes it.
const listEntry = classCatalogue.slice(
  classCatalogue.indexOf('list-school-classes'),
  classCatalogue.indexOf('export const API_CATALOG'))
classChecks.push(
  ['#28 is a GET on the year', /method: "GET"/.test(listEntry)],
  ['it takes no body', listEntry.includes('bodyAllowed: false')],
  ['it declares all five filters',
    ['active', 'search', 'affiliationProgrammeDocsId', 'hasSections', 'hasSubjects']
      .every((f) => listEntry.includes(`key: "${f}"`))],
  ['and the three paging parameters',
    ['page', 'size', 'sort'].every((f) => listEntry.includes(`key: "${f}"`))],
  ['the envelope is documented, not Spring\'s Page',
    ['content', 'totalElements', 'totalPages', 'hasNext', 'hasPrevious']
      .every((f) => listEntry.includes(`"${f}"`))],
  ['size is refused rather than clamped', listEntry.includes('INVALID_PAGE_SIZE')
    && listEntry.includes('never clamp')],
  ['the sort allow-list is named in the refusal case', listEntry.includes('INVALID_SORT_FIELD')],
  ['an unknown year is documented as a 404, not an empty page',
    listEntry.includes('AN UNKNOWN YEAR') && listEntry.includes('ACADEMIC_YEAR_NOT_FOUND')],
  ['the setup checklist is a worked case', listEntry.includes('THE SETUP CHECKLIST')],
  ['the regex-quoting case is there', listEntry.includes('A SEARCH WITH REGEX CHARACTERS')],
  ['and the read-vs-write asymmetry is a case too',
    listEntry.includes('A SUSPENDED SCHOOL CAN STILL READ')],
  // Four places in this repo said "sorts last". Mongo puts a missing field FIRST.
  ['nothing in src still has an ordering field at all',
    !/displayOrder["':]/.test(sources)],

  // The screen
  ['the screen offers a server-side search with its own button',
    classesScreen.includes('const runSearch = ()') && classesScreen.includes('icon={Search}')],
  ['searching resets to page 0, since it is a new question',
    classesScreen.includes('setPage(0); setSearch(typed)')],
  ['the filters build the query at render, so the tag shows what will be sent',
    classesScreen.includes('const query = useMemo(')],
  ['an empty filter box sends no parameter at all',
    classesScreen.includes('if (active) out.active = active')],
  ['the page-size list carries values the API refuses',
    classesScreen.includes("'101'") && classesScreen.includes("'0'")],
  ['the sort list carries a field off the allow-list',
    classesScreen.includes("'sections',")],
  ['the pager is always usable — nothing is disabled',
    classesScreen.includes('setPage((p) => p - 1)') && classesScreen.includes('setPage((p) => p + 1)')],
  // displayOrder was REMOVED on 2026-09-11, so nothing may offer it, sort by it or refuse on
  // it. It had been guarded three different ways in one day - optional, then required and
  // unique, now gone - so these check the absence rather than any of those.
  ['no screen field offers an order', !classesScreen.includes('Sort order')],
  ['the table has no order column', !classesScreen.includes('<th>Order</th>')],
  ['no sort option names it', !/'displayOrder/.test(classesScreen)],
  ['the list says alphabetical is all there is',
    classesScreen.includes('Grade 10 comes before Grade 2')],
  ['no catalogue entry refuses on it',
    !classEntry.includes('DISPLAY_ORDER') && !editEntry.includes('DISPLAY_ORDER')
    && !listEntry.includes('DISPLAY_ORDER')],
  ['#28 says what ordering by name costs', listEntry.includes('a downgrade worth knowing')],
)


// #17 — the section add. The endpoint another module was blocked on.
const sectionEntry = classCatalogue.slice(classCatalogue.indexOf('add-class-section'),
  classCatalogue.indexOf('export const API_CATALOG'))
classChecks.push(
  ['#17 posts to the class\'s sections',
    /path: "\/schools\/current\/academic-years\/{year}\/classes\/{id}\/sections"/.test(sectionEntry)],
  ['sectionNo is the only required field',
    sectionEntry.includes('requiredFields: ["sectionNo"]')],
  ['the response is the class\'s whole list, not the one row',
    sectionEntry.includes('"sectionCount"') && sectionEntry.includes('"activeCount"')
    && sectionEntry.includes('"sections"')],
  ['it says sectionNo can never change', sectionEntry.includes('can never change')],
  ['and why — eight collections store it as a string',
    sectionEntry.includes('Eight collections store it')],
  ['a duplicate is documented', sectionEntry.includes('SECTION_ALREADY_EXISTS')],
  ['and so is the lower-case one, which is the same section',
    sectionEntry.includes('THE SAME NUMBER IN LOWER CASE')],
  ['it says the service check is the only guard',
    sectionEntry.includes('only guard there is')],
  ['a colour is a documented section name', sectionEntry.includes('A SECTION NAMED BY COLOUR')],
  ['capacity 0 is a documented refusal', sectionEntry.includes('A CAPACITY OF ZERO')],
  ['and capacity is called a plan, not a limit', sectionEntry.includes('a plan, not a limit')],
  ["another school's teacher is a documented 404",
    sectionEntry.includes("ANOTHER SCHOOL'S TEACHER") && sectionEntry.includes('STAFF_NOT_FOUND')],
  ['the student module is named as what it unblocks',
    sectionEntry.includes('StudentAcademicRecord')],

  // The screen
  ['each row can reach its sections', classesScreen.includes('setSectioning(row)')],
  ['the add form sends the section body',
    classesScreen.includes("call('add-class-section'")],
  ['it aims at a typeable year and class id',
    classesScreen.includes('pathParams: { year: target.year, id: target.id }')],
  ['it shows the class list the response came back with',
    classesScreen.includes('list.sectionCount') && classesScreen.includes('list.activeCount')],
  ['it stays open after an add, so a class can get A, B, C and D',
    classesScreen.includes('A, B, C and D in one sitting')],
  ['and clears only the number, not the capacity or teacher',
    classesScreen.includes("setForm((old) => ({ ...old, sectionNo: '' }))")],
  ['the list reloads so the row count moves', classesScreen.includes('onAdded={() => load()}')],
)

for (const [label, ok] of classChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nEvery onChange unwraps the event')
// THE TRAP IS THAT THE TWO KIT COMPONENTS DISAGREE, and both are correct:
//
//   Input / TextArea  pass everything through to a plain <input>, so onChange gets the EVENT
//   Select            already does onChange(e.target.value), so its handler gets the VALUE
//
// So `onChange={setSort}` on a Select is right and `onChange={setYear}` on an Input is a bug
// that stores the whole React event object in state and posts it as the field. That is what
// happened on the Classes screen, and nothing caught it: the static checks read source text and
// the screen replays build their own request bodies, so neither renders a form and types in it.
//
// Which is why this check has to know the tag, not just the handler - an earlier version flagged
// five correct Select call sites.
const RAW_EVENT_TAGS = /^(Input|TextArea|input|textarea)$/
const eventChecks = []
for (const file of sourceFiles('src')) {
  if (file.endsWith('Kit.jsx') || file.endsWith('Select.jsx')) continue
  const body = readFileSync(file, 'utf8')
  const where = file.replace('src/', '')

  // A curried field setter must read .target.value off its own argument.
  for (const m of body.matchAll(/const \w+ = \(\w+\) => \((\w+)\) =>([^\n]*)/g)) {
    const [, arg, rest] = m
    if (/setForm|setState|set[A-Z]/.test(rest) && !rest.includes(`${arg}.target`)) {
      eventChecks.push([`${where}: a field setter ignores the event`, false])
    }
  }

  // A no-op handler is readOnly by another name, and it slips past both the disabled check
  // above and the unwrapping check below. Caught once on the Classes edit form.
  for (const m of body.matchAll(/onChange=\{\(\) => \{\}\}/g)) {
    eventChecks.push([`${where}: onChange={() => {}} is readOnly by another name`, false])
  }

  // onChange={setSomething} is only wrong on a tag that passes the raw event through.
  for (const m of body.matchAll(/onChange=\{(set[A-Z]\w*)\}/g)) {
    const before = body.slice(0, m.index)
    const tag = [...before.matchAll(/<([A-Za-z][\w.]*)/g)].pop()?.[1] ?? ''
    if (RAW_EVENT_TAGS.test(tag)) {
      eventChecks.push([`${where}: <${tag} onChange={${m[1]}}> stores the event, not the value`,
        false])
    }
  }
}
if (eventChecks.length === 0) {
  eventChecks.push(['no Input or TextArea hands a raw event to a state setter', true])
}
for (const [label, ok] of eventChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log('\nNavigation')
const html = at('/platform-plans/subscriptions')
const checks = [
  // The surface is the biggest question in this API, so both are always on offer.
  ['both surfaces are in the side panel', html.includes('>Platform<') && html.includes('>School<')],
  ['the current module is marked active', html.includes('nav-item is-active')],
  // The body navbar must show this module's submodules and only this module's.
  ["the navbar offers this module's submodules",
    html.includes('Plan catalogue') && html.includes('Subscriptions')],
  ["and not another module's", !html.includes('Academic years')],
  // Platform › Plans and School › Plans are different endpoints. The line saying which is the
  // only thing telling the two screens apart.
  ['the navbar names the surface it acts as', html.includes('module-nav-surface')],
]
for (const [label, ok] of checks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

console.log(fail ? `\n${fail} problem(s)` : '\nEvery route resolves and the navigation is correct')
process.exit(fail ? 1 : 0)
