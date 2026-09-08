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
const { App, ApiProvider } = await import(tmp.href)
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
const pairChecks = [
  ['the action button and its tag read one value', shared === 2],
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
const javaService = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/services/plans/PlatformSubscriptionService.java',
  'utf8')
const createRequestSource = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/dto/plans/subscription/SubscriptionCreateRequest.java',
  'utf8')
const editBodySource = readFileSync('src/pages/platform/plans/subscriptionEdit.js', 'utf8')
const editRequestSource = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/dto/plans/subscription/SubscriptionUpdateRequest.java',
  'utf8')
const changeRequestSource = readFileSync(
  '../backend/src/main/java/com/orbitastra/backend/dto/plans/subscription/SubscriptionPlanChangeRequest.java',
  'utf8')
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
const javaRenew = javaService.slice(
  javaService.indexOf('public SubscriptionDetailResponse renewSubscription('),
  javaService.indexOf('//! Endpoint 27'))
const mirrorSource = subsSourceFull.slice(subsSourceFull.indexOf('function whyRenewWouldRefuse('),
  subsSourceFull.indexOf('edit the terms */'))
const renewDialogStart = subsSourceFull.indexOf('function RenewCustomPeriod(')
const renewDialogSource = subsSourceFull.slice(renewDialogStart,
  Math.min(...[...subsSourceFull.matchAll(/^function \w+\(/gm)]
    .map((m) => m.index).filter((i) => i > renewDialogStart)))
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
  // Each clause on its own, not the whole expression: adding a reason to refuse must not read
  // as removing the others. This is the third guard that had to be loosened this way.
  ['and the send button refuses all three',
    ['nothingChanged', 'reasonMissing', 'periodBackwards', 'negativeOverride']
      .every((guard) => new RegExp(`disabled=\\{[^}]*${guard}`).test(editSource))],
]
for (const [label, ok] of formChecks) {
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
  ['it will not send without a reason',
    subsSourceFull.includes("disabled={!reason.trim()}")
      && subsSourceFull.includes("'Say why first'")],
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
  ['the service derives the period from the cadence being moved onto',
    /calculateSubscriptionPeriodEnd\(request\.currentPeriodEnd\(\),\s*\n\s*periodStart, billingCycle\)/
      .test(javaService)],
  ['and refuses CUSTOM without an end',
    /billingCycle == BillingCycle\.CUSTOM && request\.currentPeriodEnd\(\) == null/
      .test(javaService)],
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
  ['the end is disabled unless the cadence is CUSTOM',
    changeSource.includes('disabled={!needsPeriodEnd}')
      && changeSource.includes('readOnly={!needsPeriodEnd}')],
  ['the start is sent, in the school\'s zone',
    changeSource.includes('body.currentPeriodStart = startOfDayInZone(startsOn, timeZone)')
      && changeSource.includes('const startsOn = todayInZone(timeZone)')],
  ['and the cadence only when it differs from the plan',
    changeSource.includes("if (cycle && chosen && cycle !== chosen.billingCycle) body.billingCycle = cycle")],
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
  ['and the service no longer defaults it to today',
    javaService.includes('Instant periodStart = request.currentPeriodStart();')
      && !javaService.includes('request.currentPeriodStart() == null\n                ? startOfTodayInSchoolZone')],
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
    newSource.includes('body.currentPeriodStart = startOfDayInZone(todayInZone(timeZone), timeZone)')],
  ['it uses the school\'s zone, not UTC midnight',
    newSource.includes('todayInZone(timeZone)')
      && !newSource.includes('body.currentPeriodStart = startOfDay(')],
  ['and the page loads the school to get that zone',
    subsSourceFull.includes("call('get-school', {")
      && subsSourceFull.includes('timeZone={school?.defaultTimeZone}')],
]
for (const [label, ok] of requiredDateChecks) {
  console.log(ok ? `  ok     ${label}` : `  MISS   ${label}`)
  if (!ok) fail++
}

// A billing period starts today or later, on both endpoints. Nothing here can invoice a period
// that has already run, so a backdated start would sit in the record as a figure no process
// could act on.
console.log('\nA billing period cannot start in the past')
// The helper's own body, from its signature to the blank line after its closing brace.
const startHelperAt = javaService.indexOf('private void validatePeriodStartIsTodayOrLater(')
const startHelper = javaService.slice(startHelperAt,
  javaService.indexOf('\n    }', startHelperAt))
const startChecks = [
  ['the service refuses one on the sale, the edit and the plan change',
    javaService.includes('private void validatePeriodStartIsTodayOrLater(')
      && (javaService.match(/validatePeriodStartIsTodayOrLater\(request\.currentPeriodStart\(\), school\)/g)
        || []).length === 3],
  // Scoped to the helper's BODY. A window measured from the name matched a call site instead —
  // in createSubscription, startOfTodayInSchoolZone sits three lines under the call.
  ['it compares against the school\'s own timezone, not UTC',
    startHelper.includes('startOfTodayInSchoolZone(school.getDefaultTimeZone())')
      && !startHelper.includes('Instant.now()')],
  ['null still means today',
    /if \(requestedStart == null\) \{\s*\n\s*return;/.test(startHelper)],
  ['it refuses only a start strictly before that',
    startHelper.includes('requestedStart.isBefore(startOfToday)')],
  // The stored start of a running subscription is in the past by definition.
  ['it never checks what is already stored',
    !/validatePeriodStartIsTodayOrLater\([^)]*subscription/.test(javaService)],
  ['the helper says which methods use it',
    /Used by:[\s\S]{0,200}createSubscription\(\)[\s\S]{0,60}updateSubscription\(\)[\s\S]{0,60}changePlan\(\)[\s\S]{0,200}private void validatePeriodStartIsTodayOrLater/
      .test(javaService)],
  // The screen side: only a CHANGED value is flagged, or every running subscription would open
  // with an error on the form.
  ['the edit form flags only a changed start',
    editSource.includes('const startChanged = form.currentPeriodStart !== stored.currentPeriodStart')
      && /const startInPast = startChanged[\s\S]{0,160}< todayInput\(\)/.test(editSource)],
  ['the picker\'s min appears only once it is changed',
    editSource.includes('min={startChanged ? todayInput() : undefined}')],
  ['the send button refuses it',
    /disabled=\{[^}]*startInPast/.test(editSource)],
  ['and it says the stored one being in the past is fine',
    editSource.includes('A billing period starts today or later')
      && editSource.includes('it is only a')],
  // The sale form now HAS to send the field, so the guard is that nobody can type a wrong value
  // into it: the box is read-only and the instant is computed, not picked.
  ['the sale form offers no editable start to get wrong',
    !newSource.includes("set('currentPeriodStart')")
      && /label="Period starts on"[\s\S]{0,600}readOnly/.test(newSource)],
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
  ['the end date is disabled unless the cadence is CUSTOM',
    editSource.includes("const editIsCustomCycle = form.billingCycle === 'CUSTOM'")
      && editSource.includes('disabled={!editIsCustomCycle}')
      && editSource.includes('readOnly={!editIsCustomCycle}')],
  ['it shows the date the API will derive',
    editSource.includes('const editDerivedEnd =')
      && editSource.includes('DAYS_PER_CYCLE[form.billingCycle]')],
  // All three conditions, not just the constant's name: it has to be the cadence being CUSTOM,
  // the cadence having actually changed, and no date sent. A version stubbed to a constant
  // passed the earlier "the name exists" form of this check.
  ['moving to CUSTOM with an empty box is caught before sending',
    editSource.includes('const customNeedsEnd = editIsCustomCycle && cycleChanged && !form.currentPeriodEnd')
      && /disabled=\{[^}]*customNeedsEnd/.test(editSource)],
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
    newSource.includes('body.maxStudentsOverride = Number(maxStudents)')
      && newSource.includes('body.maxUsersOverride = Number(maxUsers)')],
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
  ['the button is disabled by the mirror, not by a bare status test',
    subsSourceFull.includes('disabled={Boolean(renewRefusal)}')],
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
  ['and a too-early date is caught before it is sent',
    subsSourceFull.includes('const tooEarly =') && subsSourceFull.includes('endDate <= startsOn')
      && subsSourceFull.includes('disabled={!endDate || tooEarly}')],
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
  ['the end date is disabled unless the cycle is CUSTOM',
    newSource.includes('disabled={!needsPeriodEnd}')
      && newSource.includes('readOnly={!needsPeriodEnd}')],
  ['and shows the derived date while it is disabled',
    newSource.includes("value={needsPeriodEnd ? periodEnd : (derivedEnd ?? '')}")],
  ['the start date is shown as today and not editable',
    /label="Period starts on"[\s\S]{0,600}value=\{todayInZone\(timeZone\)\}[\s\S]{0,80}disabled/
      .test(newSource)],
  ['changing the cycle clears a date typed against the old one',
    /setCycle\(event\.target\.value\)[\s\S]{0,300}setPeriodEnd\(''\)/.test(newSource)],
  // Sent only when it differs, so an ordinary sale does not restate what the plan already says.
  ['the cycle is sent only when it differs from the plan',
    newSource.includes("if (cycle && chosen && cycle !== chosen.billingCycle) body.billingCycle = cycle")],
  ['selling off-cadence is called out before the sale goes',
    newSource.includes('This is not the cadence the plan is listed on')],
  // The service must read the request's cycle, not the plan's, or the form is lying.
  ['the service bills on the requested cycle',
    javaService.includes('BillingCycle billingCycle = request.billingCycle() == null')
      && javaService.includes('.billingCycle(billingCycle)')],
  ['and derives the period from it, not from the plan',
    /calculateSubscriptionPeriodEnd\(request\.currentPeriodEnd\(\), periodStart,\s*\n\s*billingCycle\)/
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
  ['it will not let the sale go without one',
    /disabled=\{[^}]*needsPeriodEnd && !periodEnd/.test(subsSourceFull)
      && subsSourceFull.includes("'Set the end date first'")],
  ['and not with a ceiling of nothing',
    /disabled=\{[^}]*zeroCeiling/.test(subsSourceFull)],
  ['the chosen day is sent as the end of it', subsSourceFull.includes('endOfDay(periodEnd)')],
  ['and only for CUSTOM — no other cycle gets an end sent',
    subsSourceFull.includes('if (needsPeriodEnd) body.currentPeriodEnd')],
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
      // The entitlements belong to the subscription screen; repeating them here is two screens
      // to keep in step for one answer.
      && !profileSource.includes("'get-entitlements'")],
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
