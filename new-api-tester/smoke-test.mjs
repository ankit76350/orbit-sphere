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

const bundle = await rolldown({
  input: 'src/App.jsx',
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
const { default: App } = await import(tmp.href)
rmSync(tmp)

const at = (path) => renderToString(
  React.createElement(StaticRouter, { location: path }, React.createElement(App)))

/** Every address the navigation offers, and words that prove the right screen answered. */
const ROUTES = [
  ['/platform-core/schools', ['Platform', 'Core', 'Schools', '8 endpoints']],
  ['/platform-plans/catalogue', ['Platform', 'Plans', 'Plan catalogue', '9 endpoints']],
  ['/platform-plans/subscriptions', ['Subscriptions', '3 endpoints']],
  ['/school-core/profile', ['School', 'Profile', '5 endpoints']],
  ['/school-core/academic-years', ['Academic years', '18 endpoints']],
  ['/school-plans/subscription', ['Subscription', '2 endpoint']],
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
