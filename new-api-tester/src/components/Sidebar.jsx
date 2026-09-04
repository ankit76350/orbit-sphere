import { NavLink } from 'react-router-dom'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { GROUPS } from '../screens.js'

export default function Sidebar({ collapsed, onToggle }) {
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
          {GROUPS.map((group) => (
            <div className="nav-section" key={group.title}>
              <p className="nav-section-title">{group.title}</p>
              <ul>
                {group.items.map(({ label, to, icon: Icon, endpoints }) => (
                  <li key={to}>
                    <NavLink
                      to={to}
                      className={({ isActive }) => `nav-item${isActive ? ' is-active' : ''}`}
                      title={collapsed ? label : undefined}
                    >
                      <Icon size={17} strokeWidth={1.9} className="nav-icon" aria-hidden="true" />
                      <span className="nav-label">{label}</span>
                      {endpoints ? <span className="nav-badge">{endpoints}</span> : null}
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
