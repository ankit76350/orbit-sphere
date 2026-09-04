/**
 * How an address is built. No dependency on the screen list, and that is the point.
 *
 * These used to live in screens.js, which imports every page — and a page that needs to build a
 * link back to its list would then import screens.js, closing a cycle. It failed exactly the way
 * cycles do: `screens.js` began loading, reached its import of `SchoolDetail`, which ran
 * `screenPath(...)` at module level before `screens.js` had defined it, and threw
 * "Cannot access 'screenPath' before initialization". The bundler was happy; the runtime was not.
 *
 * A MODULE'S ADDRESS IS ONE SEGMENT, `surface-module`, and the submodule is the second.
 *
 * That is not a style choice. The dev server proxies `^/platform($|/)` and `^/schools($|/)` to
 * the backend, so a route like `/platform/plans/catalogue` would never reach the router — the
 * proxy would send it to the API and the browser would get a 404 from Spring. Joining the two
 * with a hyphen sidesteps it: `/platform-plans/catalogue` cannot match either anchor, now or
 * when a new API group appears.
 *
 * It also happens to map one segment per level of navigation: the side panel picks the first,
 * the navbar in the body picks the second, and anything deeper belongs to one row.
 */

export const moduleSlug = (surfaceId, moduleId) => `${surfaceId}-${moduleId}`

export const screenPath = (surfaceId, moduleId, submoduleId) =>
  `/${moduleSlug(surfaceId, moduleId)}/${submoduleId}`

/** One row of a submodule, addressed by its own id. */
export const detailPath = (surfaceId, moduleId, submoduleId, id) =>
  `${screenPath(surfaceId, moduleId, submoduleId)}/${id}`

/**
 * Which surface an address is on, from the address alone.
 *
 * Here rather than in screens.js because that file imports every page, and the top bar needs
 * this — importing screens.js from a component in the shell is how the last cycle formed.
 * The first segment is `surface-module`, so the surface is everything before the hyphen.
 *
 * Returns null for an address that is on neither, like the not-found page.
 */
export function surfaceOf(pathname) {
  const slug = String(pathname).split('/')[1] ?? ''
  const surface = slug.split('-')[0]
  return surface === 'platform' || surface === 'school' ? surface : null
}
