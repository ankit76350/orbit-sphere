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
  ['/platform-plans/catalogue', ['Platform', 'Plans', 'Plan catalogue', '9 endpoints']],
  ['/platform-plans/subscriptions', ['Subscriptions', '3 endpoints']],
  ['/school-core/profile', ['School', 'Profile', '5 endpoints']],
  ['/school-core/academic-years', ['Academic years', '18 endpoints']],
  ['/school-plans/subscription', ['Subscription', '2 endpoint']],
  // Opening a row is its own address. First paint is the read, because renderToString does not
  // run effects — which is the point: the page reads the school itself rather than being handed
  // a row from a list that may already be stale.
  ['/platform-core/schools/6a95000000000000000000aa', ['Reading the school']],
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
