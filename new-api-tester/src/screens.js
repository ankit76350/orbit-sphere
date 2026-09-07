import { Building2, CreditCard, Package, Settings2 } from 'lucide-react'
import Catalogue from './pages/platform/plans/Catalogue.jsx'
import PlanDetail from './pages/platform/plans/PlanDetail.jsx'
import Subscriptions from './pages/platform/plans/Subscriptions.jsx'
import MySubscription from './pages/school/plans/Subscription.jsx'
import SchoolDetail from './pages/platform/core/SchoolDetail.jsx'
import Schools from './pages/platform/core/Schools.jsx'
import AcademicYearDetail from './pages/school/core/AcademicYearDetail.jsx'
import AcademicYears from './pages/school/core/AcademicYears.jsx'
import Profile from './pages/school/core/Profile.jsx'
import { moduleSlug, screenPath } from './paths.js'

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
          {
            id: 'schools',
            label: 'Schools',
            endpoints: 8,
            group: 'Core / School — platform',
            // A submodule with no `screen` falls back to Placeholder, so the nav stays
            // complete while the screens are filled in one at a time.
            screen: Schools,
            // Opening a row is its own address, so it can be linked, reloaded and shared. The
            // list stays selected in both navigations because the module is read off the FIRST
            // path segment, which a detail address does not change.
            detail: { param: 'id', screen: SchoolDetail },
          },
        ],
      },
      {
        id: 'plans',
        label: 'Plans',
        icon: Package,
        submodules: [
          {
            id: 'catalogue',
            label: 'Plan catalogue',
            endpoints: 9,
            group: 'Plans / Plan catalogue',
            screen: Catalogue,
            // The API names a plan by code AND version, so the address carries both,
            // joined with `@` to stay one route parameter.
            detail: { param: 'id', screen: PlanDetail },
          },
          {
            id: 'subscriptions',
            label: 'Subscriptions',
            endpoints: 3,
            group: 'Plans / Subscriptions',
            screen: Subscriptions,
          },
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
          {
            id: 'profile',
            label: 'Profile',
            endpoints: 5,
            group: 'Core / School — profile',
            screen: Profile,
          },
          {
            id: 'academic-years',
            label: 'Academic years',
            endpoints: 18,
            group: 'Core / Academic Year',
            screen: AcademicYears,
            // A year is addressed by its name, which the API guarantees is immutable — so
            // it is safe in a URL in a way an editable field would not be.
            detail: { param: 'name', screen: AcademicYearDetail },
          },
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
            screen: MySubscription,
          },
        ],
      },
    ],
  },
]

/* ------------------------------------------------------------------------ addresses */

/**
 * The builders live in paths.js, not here.
 *
 * This file imports every page, so a page needing to build a link back to its list would import
 * this file and close a cycle — which threw at module load, not at build. Re-exported so callers
 * that already have `screens.js` open do not need a second import.
 */
export { detailPath, moduleSlug, screenPath } from './paths.js'

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

/**
 * Every address, flattened, for the router.
 *
 * A submodule with a `detail` contributes two: the list and one row. Both are built from the
 * same declaration, so a detail address cannot exist without the list it belongs to.
 */
export const ROUTES = SURFACES.flatMap((surface) =>
  surface.modules.flatMap((module) =>
    module.submodules.flatMap((submodule) => {
      const list = {
        path: screenPath(surface.id, module.id, submodule.id),
        surface,
        module,
        submodule,
        screen: submodule.screen,
      }
      if (!submodule.detail) return [list]
      return [
        list,
        {
          path: `${list.path}/:${submodule.detail.param}`,
          surface,
          module,
          submodule,
          screen: submodule.detail.screen,
        },
      ]
    }),
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
