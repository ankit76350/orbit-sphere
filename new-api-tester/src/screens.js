import { Building2, CreditCard, Package, Settings2 } from 'lucide-react'

/**
 * Every screen this app has, in the shape the navigation reads it: surface, then module, then
 * submodule.
 *
 * THE THREE LEVELS ARE THE API'S OWN, NOT INVENTED.
 *
 *   1. SURFACE — who is calling. `platform` is an operator from outside the tenant, naming the
 *      school in the URL; `school` is a school acting on itself, naming nothing because the
 *      tenant comes from the header. This is the most important split in the whole API and it is
 *      the one that decides what a caller may see, so it is the outermost level here.
 *   2. MODULE — `core` is schools and their academic years, `plans` is what we sell.
 *   3. SUBMODULE — the six groups the built endpoints actually fall into.
 *
 * The counts add up to the 45 endpoints in `api-battleground/src/config/endpoints.js`, which is
 * generated from the Postman collection. Recount them there rather than trusting these badges.
 */
export const SURFACES = [
  {
    id: 'platform',
    label: 'Platform',
    note: 'An operator, from outside the tenant. The school is named in the URL.',
    modules: [
      {
        id: 'core',
        label: 'Core',
        icon: Building2,
        submodules: [
          { id: 'schools', label: 'Schools', endpoints: 8, group: 'Core / School — platform' },
        ],
      },
      {
        id: 'plans',
        label: 'Plans',
        icon: Package,
        submodules: [
          { id: 'catalogue', label: 'Plan catalogue', endpoints: 9, group: 'Plans / Plan catalogue' },
          { id: 'subscriptions', label: 'Subscriptions', endpoints: 3, group: 'Plans / Subscriptions' },
        ],
      },
    ],
  },
  {
    id: 'school',
    label: 'School',
    note: 'A school acting on itself. It never names a school — the tenant header does.',
    modules: [
      {
        id: 'core',
        label: 'Core',
        icon: Settings2,
        submodules: [
          { id: 'profile', label: 'Profile', endpoints: 5, group: 'Core / School — profile' },
          { id: 'academic-years', label: 'Academic years', endpoints: 18, group: 'Core / Academic Year' },
        ],
      },
      {
        id: 'plans',
        label: 'Plans',
        icon: CreditCard,
        submodules: [
          {
            id: 'subscription',
            label: 'Subscription',
            endpoints: 2,
            group: "Plans / Subscription — the school's own view",
          },
        ],
      },
    ],
  },
]

/* ------------------------------------------------------------------------ addresses */

/**
 * A module's address is ONE segment, `surface-module`, and the submodule is the second.
 *
 * That is not a style choice. The dev server proxies `^/platform($|/)` and `^/schools($|/)` to
 * the backend, so a route like `/platform/plans/catalogue` would never reach the router — the
 * proxy would send it to the API and the browser would get a 404 from Spring. Joining the two
 * with a hyphen sidesteps it: `/platform-plans/catalogue` cannot match either anchor, now or
 * when a new API group appears.
 *
 * It also happens to map one segment per level of navigation: the side panel picks the first,
 * the navbar in the body picks the second.
 */
export const moduleSlug = (surfaceId, moduleId) => `${surfaceId}-${moduleId}`
export const screenPath = (surfaceId, moduleId, submoduleId) =>
  `/${moduleSlug(surfaceId, moduleId)}/${submoduleId}`

/** Every module, flattened, for the side panel. Each one knows where its first screen is. */
export const MODULE_LINKS = SURFACES.flatMap((surface) =>
  surface.modules.map((module) => ({
    surfaceId: surface.id,
    surfaceLabel: surface.label,
    moduleId: module.id,
    label: module.label,
    icon: module.icon,
    slug: moduleSlug(surface.id, module.id),
    to: screenPath(surface.id, module.id, module.submodules[0].id),
    endpoints: module.submodules.reduce((sum, one) => sum + one.endpoints, 0),
  })),
)

/** Every screen, flattened, for the router. */
export const ROUTES = SURFACES.flatMap((surface) =>
  surface.modules.flatMap((module) =>
    module.submodules.map((submodule) => ({
      path: screenPath(surface.id, module.id, submodule.id),
      surface,
      module,
      submodule,
    })),
  ),
)

/** The module a path belongs to, so the body navbar knows which submodules to offer. */
export function moduleForPath(pathname) {
  const slug = pathname.split('/')[1]
  for (const surface of SURFACES) {
    for (const module of surface.modules) {
      if (moduleSlug(surface.id, module.id) === slug) return { surface, module }
    }
  }
  return null
}
