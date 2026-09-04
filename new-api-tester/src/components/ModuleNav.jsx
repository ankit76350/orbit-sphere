import { NavLink, useLocation } from 'react-router-dom'
import { moduleForPath } from '../screens.js'
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
 */
export default function ModuleNav() {
  const { pathname } = useLocation()
  const here = moduleForPath(pathname)
  if (!here) return null

  const { surface, module } = here

  return (
    <div className="module-nav">
      <p className="module-nav-where">
        <span className="module-nav-surface">{surface.label}</span>
        <span aria-hidden="true">›</span>
        <span>{module.label}</span>
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
