import { NavLink, useLocation } from 'react-router-dom'
import { BookOpen } from 'lucide-react'
import { moduleForPath, readmeUrl } from '../screens.js'
import { screenPath } from '../paths.js'

/**
 * The navbar in the body: the submodules of whichever module the side panel is on.
 *
 * IT LIVES HERE, NOT IN THE SIDE PANEL, because this is where there is room. A row of labels
 * across the content column reads at a glance; the same list as a third level of indent in a
 * 15rem column does not, and it makes two things look selected at once.
 *
 * It also names where you are: the surface and module above the row, so a screen is never
 * ambiguous about which caller it is acting as. That matters more here than in most apps —
 * `Platform › Plans › Subscriptions` and `School › Plans › Subscription` are different endpoints
 * with deliberately different answers, and the only thing telling them apart is this line.
 *
 * A module with one submodule still gets the bar, so the heading is in the same place on every
 * screen and the surface is always stated.
 *
 * IT CARRIES THE LINK TO THE PLAN, and here rather than on each page for the reason the bar itself
 * is here: one place that every screen passes through. Twenty pages each rendering their own link
 * is twenty places for one to go stale, and the pages that have not been written yet would have
 * none at all.
 *
 * THE LINK IS PER SUBMODULE, NOT PER MODULE, because that is how the plans are filed:
 * `academics` has one README for structure and another for grading, and `people` has one per
 * package. The active pill decides which — so the link always names the plan for the screen you
 * are looking at, and follows you when you switch tabs.
 */
export default function ModuleNav() {
  const { pathname } = useLocation()
  const here = moduleForPath(pathname)
  if (!here) return null

  const { surface, module } = here

  // The submodule whose screen is showing. Matched on the path rather than on NavLink's own
  // state, because the link below is rendered outside the list and cannot ask a NavLink.
  const active = module.submodules.find(
    (one) => pathname.startsWith(screenPath(surface.id, module.id, one.id)),
  ) ?? module.submodules[0]
  const plan = readmeUrl(active)

  return (
    <div className="module-nav">
      <p className="module-nav-where">
        <span className="module-nav-surface">{surface.label}</span>
        <span aria-hidden="true">›</span>
        <span>{module.label}</span>
        {plan ? (
          <>
            <span aria-hidden="true">·</span>
            {/* NEW TAB, always. This is a testing tool with unsaved forms open on nearly every
                screen, and navigating away from one to read a plan would lose it. */}
            <a className="module-nav-plan" href={plan} target="_blank" rel="noreferrer">
              <BookOpen size={12} />
              {active.label} plan
            </a>
          </>
        ) : null}
      </p>

      <div className="segmented" role="tablist" aria-label={`${module.label} screens`}>
        {module.submodules.map((submodule) => (
          <NavLink
            key={submodule.id}
            to={screenPath(surface.id, module.id, submodule.id)}
            role="tab"
            /* The active class comes from NavLink's own match, so the pill and the route agree
               by construction rather than by a second path comparison that could disagree. */
            className={({ isActive }) => `segmented-item${isActive ? ' is-on' : ''}`}
          >
            {submodule.label}
            <span className="module-nav-count">{submodule.endpoints}</span>
          </NavLink>
        ))}
      </div>
    </div>
  )
}
