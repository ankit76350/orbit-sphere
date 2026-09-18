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
import PositionDetail from './pages/school/people/PositionDetail.jsx'
import Positions from './pages/school/people/Positions.jsx'
import StaffList from './pages/school/people/StaffList.jsx'
import StaffDetail from './pages/school/people/StaffDetail.jsx'
import Classes from './pages/school/academics/Classes.jsx'
import ClassDetail from './pages/school/academics/ClassDetail.jsx'
import SectionDetail from './pages/school/academics/SectionDetail.jsx'
import GradingSchemes from './pages/school/academics/GradingSchemes.jsx'
import Timetable from './pages/school/academics/Timetable.jsx'
import TimetableDay from './pages/school/academics/TimetableDay.jsx'
import GradingSchemeDetail from './pages/school/academics/GradingSchemeDetail.jsx'
import { moduleSlug, screenPath, tabPath } from './paths.js'

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
            // Seven: #1 creates, #3 edits, #4 and #5 retire and restore, #6 lists, #7 opens one,
            // #8 resolves a mark. #8 closed phase 1 on 2026-09-16.
            endpoints: 7,
            screen: GradingSchemes,
            // A scheme is addressed by its document id — what ClassSubject, Exam and ReportCard
            // all store — so that is the address. #7 is what fills the page, and it is the only
            // endpoint that returns the BANDS: #6 trims them to a count.
            detail: { param: 'id', screen: GradingSchemeDetail },
          },
          {
            // Last of the four, because a timetable needs everything the other three define: a
            // year to derive from the date, a class and a section to schedule, and a subject that
            // section actually studies.
            id: 'timetable',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/academics/timetable/README.md',
            label: 'Timetable',
            group: 'Academics / Timetable',
            // TEN OF TWELVE. #1 writes a day or a range, #2 replaces one whole, #3 adds one
            // period, #4 corrects one — the substitution this module exists for — #5 removes one,
            // #6 builds a day from another, #7 opens one in full, #8 and #9 read one section's and
            // one teacher's day, and #10 lists a year's days as counts. What is left is #11, one
            // room's day, and #12, who is FREE to cover a period.
            endpoints: 10,
            screen: Timetable,
            // TWO JOBS, TWO ADDRESSES. Writing a day and reading the year back were a toggle with
            // no address, so a link could not point at either and a reload always landed on the
            // builder. Each is a named screen now, and the bare submodule address redirects to
            // the fallback — viewing, because reading is what somebody arriving here usually
            // wants.
            //
            // The segments are STATIC, which is what keeps them out of `:date`'s way: React Router
            // ranks a literal segment above a dynamic one, so `view-timetable` can never be read
            // as a date.
            tabs: {
              fallback: 'view-timetable',
              items: [
                { segment: 'create-timetable', label: 'Create timetable', screen: Timetable },
                { segment: 'view-timetable', label: 'View timetable', screen: Timetable },
                // #8 AND #9 ARE ENTRY POINTS, not lenses. "What a parent opens" and "what a
                // teacher's app opens" are where somebody STARTS, not a filter they reach after
                // reading the whole school's Tuesday — so each gets an address of its own.
                { segment: 'section-day', label: 'Section day', screen: Timetable },
                { segment: 'teacher-day', label: 'Teacher day', screen: Timetable },
              ],
            },
            // ADDRESSED BY THE DATE, not by a document id, which is unusual here and is what the
            // API does: a caller always knows the date and never knows the id. So a row of the
            // list opens by the column it was already showing.
            detail: { param: 'date', screen: TimetableDay },
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
            // Six: #9 and #13 create, #10 and #14 edit, #12 lists, #52 opens one. The two
            // position READS are counted on the submodule below, which is where they are run.
            endpoints: 6,
            screen: Departments,
            // A unit is addressed by its document id — what positions store as departmentDocsId.
            // Opening a row is its own URL, so it can be linked, reloaded and shared; the list
            // stays selected in both navigations because the module is read off the FIRST path
            // segment, which a detail address does not change.
            detail: {
              param: 'id',
              screen: DepartmentDetail,
              // A SEAT IS A ROW INSIDE A UNIT, so it gets the third level rather than an address
              // of its own — the same shape a class's section has. A position cannot exist
              // outside a department (#13 requires one and #14 refuses to move it), so an
              // address that could name a seat without naming its unit would be describing
              // something this product cannot store.
              child: { segment: 'positions', param: 'positionDocsId', screen: PositionDetail },
            },
          },
          {
            // SECOND, AND IT IS A READ SCREEN ONLY. #15 answers "find seats across the school",
            // which is a different question from #52's "what is this unit made of" — and its
            // filters (?vacant=, ?teaching=, ?active=, ?search=) had nowhere to be run from
            // until this existed. Creating a seat stays on the department that will own it,
            // because #13 requires a departmentDocsId.
            //
            // NO `detail` OF ITS OWN. A row opens the seat at its one address, under its
            // department — a seat does not get a second address just because a second screen
            // lists it.
            id: 'positions',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/people/organization/README.md',
            label: 'Positions',
            group: 'People / Organization',
            // Two: #15 lists the seats with their filled counts, #53 opens one.
            endpoints: 2,
            screen: Positions,
          },
          {
            // Third, because a position has to exist before #16 can employ anybody into one —
            // and #1 is the other half of the module's phase 1.
            id: 'staff',
            readme: 'backend/src/main/java/com/orbitastra/backend/controllers/people/staff/README.md',
            label: 'Staff',
            group: 'People / Staff',
            // Eight: #1 creates a person, #2 edits one, #7 lists them, #8 opens one, #16
            // employs or promotes, #18 corrects a record, 18b changes its status, #19 reads a
            // whole history. It said 3, from before everything after #8 was built.
            endpoints: 8,
            screen: StaffList,
            // A person is addressed by their document id — what every other collection stores,
            // never the employee number. Opening a row is its own URL, so it can be linked,
            // reloaded and shared.
            detail: {
              param: 'id',
              screen: StaffDetail,
            },
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
export { detailPath, moduleSlug, screenPath, tabPath } from './paths.js'

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
 *
 * A submodule with `tabs` contributes one more per tab — a named screen for a submodule that holds
 * more than one job, like the timetable's builder and its list. The bare submodule address stays
 * registered and redirects to the declared fallback, so a link written before the split still
 * lands somewhere sensible.
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

      // A NAMED SCREEN PER JOB, for a submodule that holds more than one. The bare address above
      // stays registered and redirects to the fallback, so an old link still lands somewhere.
      const tabs = (submodule.tabs?.items ?? []).map((tab) => ({
        path: tabPath(surface.id, module.id, submodule.id, tab.segment),
        surface,
        module,
        submodule,
        screen: tab.screen,
      }))

      if (!submodule.detail) return [list, ...tabs]

      const detail = {
        path: `${list.path}/:${submodule.detail.param}`,
        surface,
        module,
        submodule,
        screen: submodule.detail.screen,
      }
      if (!submodule.detail.child) return [list, ...tabs, detail]

      // A THIRD LEVEL, for a row inside a detail that has a page of its own — a class's
      // section. It is built from the same declaration as the two above it, so a section
      // address cannot exist without the class address it hangs off, which cannot exist
      // without the list. The nesting stops here on purpose: a section belongs to a class
      // belongs to a year, and the year is the tenant's, which the header carries.
      const child = submodule.detail.child
      return [
        list,
        ...tabs,
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
