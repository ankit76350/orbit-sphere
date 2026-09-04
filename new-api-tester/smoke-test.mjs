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
