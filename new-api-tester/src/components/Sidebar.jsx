import { NavLink } from 'react-router-dom'
import {
  BarChart3, ChevronLeft, ChevronRight, CircleDollarSign, CreditCard, FileText,
  LayoutList, LifeBuoy, LineChart, PieChart, Repeat, Settings, ShieldCheck,
  SlidersHorizontal, SquareStack, Table2, Waves, Wallet,
} from 'lucide-react'

const SECTIONS = [
  {
    title: 'Pages',
    items: [
      { label: 'Markets', to: '/markets', icon: PieChart, badge: 1 },
      { label: 'Trading', to: '/trading', icon: LineChart, badge: 1 },
      { label: 'Wallet', to: '/wallet', icon: Wallet, badge: 1 },
      { label: 'Loans', to: '/', icon: CircleDollarSign, badge: 1 },
      { label: 'Vaults', to: '/vaults', icon: ShieldCheck, badge: 1 },
      { label: 'Portfolio', to: '/portfolio', icon: BarChart3, badge: 1 },
      { label: 'Liquidity pools', to: '/liquidity-pools', icon: Waves, badge: 1 },
      { label: 'Swap', to: '/swap', icon: Repeat, badge: 1 },
    ],
  },
  {
    title: 'UI Elements',
    items: [
      { label: 'Menu Styles', to: '/menu-styles', icon: LayoutList },
      { label: 'Tables', to: '/tables', icon: Table2 },
      { label: 'Charts', to: '/charts', icon: BarChart3 },
      { label: 'Forms', to: '/forms', icon: SlidersHorizontal },
      { label: 'Pricing', to: '/pricing', icon: CreditCard },
      { label: 'Settings', to: '/settings', icon: Settings },
      { label: 'Modals/Pop-Ups', to: '/modals', icon: SquareStack },
    ],
  },
  {
    title: 'Documentation & Support',
    items: [
      { label: 'Documentation', to: '/documentation', icon: FileText, chevron: true },
      { label: 'Support', to: '/support', icon: LifeBuoy, chevron: true },
    ],
  },
]

export default function Sidebar({ collapsed, onToggle }) {
  return (
    <aside className="sidebar" data-collapsed={collapsed || undefined}>
      <div className="sidebar-inner">
        <div className="sidebar-head">
          <a className="brand" href="/" aria-label="Extej home">
            <span className="brand-mark" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="16" height="16">
                <path d="M12 5l7.5 14.5h-15L12 5z" fill="currentColor" />
              </svg>
            </span>
            <span className="brand-name">Extej</span>
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
          {SECTIONS.map((section) => (
            <div className="nav-section" key={section.title}>
              <p className="nav-section-title">{section.title}</p>
              <ul>
                {section.items.map(({ label, to, icon: Icon, badge, chevron }) => (
                  <li key={label}>
                    <NavLink
                      to={to}
                      end={to === '/'}
                      className={({ isActive }) => `nav-item${isActive ? ' is-active' : ''}`}
                      title={collapsed ? label : undefined}
                    >
                      <Icon size={17} strokeWidth={1.9} className="nav-icon" aria-hidden="true" />
                      <span className="nav-label">{label}</span>
                      {badge ? <span className="nav-badge">{badge}</span> : null}
                      {badge || chevron ? (
                        <ChevronRight size={14} className="nav-chevron" aria-hidden="true" />
                      ) : null}
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
