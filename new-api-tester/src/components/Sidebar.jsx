import { ChevronLeft, ChevronRight } from 'lucide-react'
import { NavLink, useLocation } from 'react-router-dom'
import { MODULE_LINKS, SURFACES } from '../screens.js'

/**
 * The side panel: which surface, and which module inside it.
 *
 * TWO SECTIONS, BECAUSE THE SURFACE IS THE BIGGEST QUESTION. `Platform` is an operator from
 * outside the tenant; `School` is a school acting on itself. It decides what a caller is allowed
 * to see — the school's own subscription read deliberately withholds the plan's list price and
 * the gateway's customer reference — so mixing the two in one list would put the endpoint that
 * must not leak next to the one it must not leak from.
 *
 * A MODULE'S SUBMODULES ARE NOT HERE. They are a navbar in the body, where a row of labels reads
 * at a glance; the same list indented a third level in a narrow column does not. See ModuleNav.
 *
 * The active row is matched on the module, not the exact path, because the address carries a
 * submodule the panel does not know or care about.
 */
export default function Sidebar({ collapsed, onToggle }) {
  const { pathname } = useLocation()
  const currentSlug = pathname.split('/')[1]

  return (
    <aside className="sidebar" data-collapsed={collapsed || undefined}>
      <div className="sidebar-inner">
        <div className="sidebar-head">
          <a className="brand" href="/" aria-label="Orbit Sphere home">
            <span className="brand-mark" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="16" height="16">
                <circle cx="12" cy="12" r="4" fill="currentColor" />
                <ellipse cx="12" cy="12" rx="10" ry="4.5" fill="none" stroke="currentColor"
                         strokeWidth="1.6" transform="rotate(-20 12 12)" />
              </svg>
            </span>
            <span className="brand-name">Orbit Sphere</span>
          </a>
          <button
            type="button"
            className="collapse-btn"
            onClick={onToggle}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            aria-expanded={!collapsed}
          >
            {collapsed ? <ChevronRight size={15} /> : <ChevronLeft size={15} />}
          </button>
        </div>

        <nav className="sidebar-nav" aria-label="Main">
          {SURFACES.map((surface) => (
            <div className="nav-section" key={surface.id}>
              <p className="nav-section-title" title={surface.note}>{surface.label}</p>
              <ul>
                {MODULE_LINKS.filter((one) => one.surfaceId === surface.id).map((one) => (
                  <li key={one.slug}>
                    <NavLink
                      to={one.to}
                      className={`nav-item${one.slug === currentSlug ? ' is-active' : ''}`}
                      title={collapsed ? one.label : undefined}
                    >
                      <one.icon size={17} strokeWidth={1.9} className="nav-icon" aria-hidden="true" />
                      <span className="nav-label">{one.label}</span>
                      <span className="nav-badge">{one.endpoints}</span>
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
      </div>
    </aside>
  )
}
