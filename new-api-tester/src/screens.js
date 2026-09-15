import { Building2, CreditCard, GraduationCap, Package, Settings2, Users } from 'lucide-react'
import Catalogue from './pages/platform/plans/Catalogue.jsx'
import PlanDetail from './pages/platform/plans/PlanDetail.jsx'
import AllSubscriptions from './pages/platform/plans/AllSubscriptions.jsx'
import Subscriptions from './pages/platform/plans/Subscriptions.jsx'
import MySubscription from './pages/school/plans/Subscription.jsx'
import SchoolDetail from './pages/platform/core/SchoolDetail.jsx'
import Schools from './pages/platform/core/Schools.jsx'
import AcademicYearDetail from './pages/school/core/AcademicYearDetail.jsx'
import AcademicYears from './pages/school/core/AcademicYears.jsx'
import Profile from './pages/school/core/Profile.jsx'
import Terms from './pages/school/academics/Terms.jsx'
import Departments from './pages/school/people/Departments.jsx'
import DepartmentDetail from './pages/school/people/DepartmentDetail.jsx'
import StaffList from './pages/school/people/StaffList.jsx'
import Classes from './pages/school/academics/Classes.jsx'
import ClassDetail from './pages/school/academics/ClassDetail.jsx'
import SectionDetail from './pages/school/academics/SectionDetail.jsx'
import GradingSchemes from './pages/school/academics/GradingSchemes.jsx'
import GradingSchemeDetail from './pages/school/academics/GradingSchemeDetail.jsx'
import { moduleSlug, screenPath } from './paths.js'

/**
 * Where a module's plan is readable, for the link every screen carries.
 *
 * ONE CONSTANT, AND IT NAMES A BRANCH. Every README path below is repo-relative, so this is the
 * only line to change when the work merges — leave it and every link on every screen silently
 * points at a branch that no longer exists.
 *
 * THE PLAN IS THE POINT. These screens exist to exercise endpoints whose reasoning lives in those
 * files: what each refusal is for, what was deliberately left out, what is still owed. A tester
 * that cannot reach the reasoning is a set of buttons.
 */
export const REPO_README_BASE = 'https://github.com/ankit76350/orbit-sphere/blob/new-api/'

/** The GitHub URL for a submodule's plan, or null when it has none. */
export function readmeUrl(submodule) {
  return submodule?.readme ? REPO_README_BASE + submodule.readme : null
}

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
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/core/README.md',
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
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/plans/README.md',
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
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/plans/README.md',
            label: 'Subscriptions',
            // Ten: create, edit, change-plan, renew, suspend, resume, cancel, plus the three
            // reads (#27, #28, #29). The badge said 3, from when that was true.
            endpoints: 10,
            group: 'Plans / Subscriptions',
            screen: Subscriptions,
          },
          {
            // A screen of its own rather than a card on Subscriptions: that one is school-scoped
            // and starts with a School picker, and #30 takes no school at all.
            id: 'all-subscriptions',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/plans/README.md',
            label: 'All subscriptions',
            endpoints: 1,
            group: 'Plans / All subscriptions',
            screen: AllSubscriptions,
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
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/core/README.md',
            label: 'Profile',
            endpoints: 5,
            group: 'Core / School — profile',
            screen: Profile,
          },
          {
            id: 'academic-years',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/core/README.md',
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
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/plans/README.md',
            label: 'Subscription',
            endpoints: 2,
            group: "Plans / Subscription — the school's own view",
            screen: MySubscription,
          },
        ],
      },
      {
        // The third module, and the first one whose plan is mostly unbuilt: 37 endpoints are
        // designed in controllers/academics/structure/README.md. The badges count what is BUILT,
        // like every other badge here.
        id: 'academics',
        label: 'Academics',
        icon: GraduationCap,
        submodules: [
          {
            // Terms come first because a year is divided before it is filled — and because the
            // plan numbers them 1 to 11, ahead of the classes.
            id: 'terms',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/academics/structure/README.md',
            label: 'Terms',
            group: 'Academics / Terms',
            // #1, #3, #5, #6 and #9. The badge said 2, from when that was true.
            endpoints: 5,
            screen: Terms,
          },
          {
            // Grading sits beside terms and classes rather than under either. A scheme belongs
            // to the SCHOOL, not to a year — the only submodule here whose paths carry no
            // {year}, which is also why no gate 4 runs above it.
            id: 'grading',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/academics/grading/README.md',
            label: 'Grading',
            group: 'Academics / Grading',
            endpoints: 6,
            screen: GradingSchemes,
            // A scheme is addressed by its document id — what ClassSubject, Exam and ReportCard
            // all store — so that is the address. #7 is what fills the page, and it is the only
            // endpoint that returns the BANDS: #6 trims them to a count.
            detail: { param: 'id', screen: GradingSchemeDetail },
          },
          {
            id: 'classes',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/academics/structure/README.md',
            label: 'Classes',
            group: 'Academics / Classes',
            endpoints: 6,
            screen: Classes,
            // A class is addressed by its document id, so that is the address — and #29 is what
            // fills the page. Opening a row is its own URL, so it can be linked, reloaded and
            // shared; the list stays selected in both navigations because the module is read off
            // the FIRST path segment, which a detail address does not change.
            detail: {
              param: 'id',
              screen: ClassDetail,
              // A section has no id — it is embedded — so it is addressed by its sectionNo,
              // which is the one thing eight other collections store about it.
              child: { segment: 'sections', param: 'sectionNo', screen: SectionDetail },
            },
          },
        ],
      },
      {
        // The fourth module, and the one the product actually starts with: a school hires before
        // it timetables. 51 endpoints are designed in controllers/people/README.md and one
        // exists. The badge counts what is BUILT, like every other badge here.
        id: 'people',
        label: 'People',
        icon: Users,
        submodules: [
          {
            // Organization comes first because nothing else in people can be built without it —
            // employing somebody needs a position, and a position needs a department.
            id: 'departments',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/people/organization/README.md',
            label: 'Organization',
            group: 'People / Organization',
            // Six: #9 and #13 create, #10 and #14 edit, #12 lists, #52 opens one. It said 4,
            // from before the two edits and the detail read.
            endpoints: 6,
            screen: Departments,
            // A unit is addressed by its document id — what positions store as departmentDocsId.
            // Opening a row is its own URL, so it can be linked, reloaded and shared; the list
            // stays selected in both navigations because the module is read off the FIRST path
            // segment, which a detail address does not change.
            detail: {
              param: 'id',
              screen: DepartmentDetail,
            },
          },
          {
            // Second, because a position has to exist before #16 can employ anybody into one —
            // and #1 is the other half of the module's phase 1.
            id: 'staff',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/people/staff/README.md',
            label: 'Staff',
            group: 'People / Staff',
            // One: #1 creates a person. #7 and #8 are the reads and are not built, which is why
            // this screen shows what it created rather than what the school holds.
            endpoints: 1,
            screen: StaffList,
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
 * A submodule with a `detail` contributes two: the list and one row — three when that detail has
 * a `child`, which is a row inside the row. All are built from the same declaration, so a deeper
 * address cannot exist without the one it hangs off.
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

      const detail = {
        path: `${list.path}/:${submodule.detail.param}`,
        surface,
        module,
        submodule,
        screen: submodule.detail.screen,
      }
      if (!submodule.detail.child) return [list, detail]

      // A THIRD LEVEL, for a row inside a detail that has a page of its own — a class's
      // section. It is built from the same declaration as the two above it, so a section
      // address cannot exist without the class address it hangs off, which cannot exist
      // without the list. The nesting stops here on purpose: a section belongs to a class
      // belongs to a year, and the year is the tenant's, which the header carries.
      const child = submodule.detail.child
      return [
        list,
        detail,
        {
          path: `${detail.path}/${child.segment}/:${child.param}`,
          surface,
          module,
          submodule,
          screen: child.screen,
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
