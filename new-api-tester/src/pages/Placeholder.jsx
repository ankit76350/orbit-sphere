import { Construction } from 'lucide-react'

/**
 * What every route shows until its screen is built.
 *
 * It says which screen is missing and how many endpoints are waiting for it, rather than a bare
 * "coming soon" — the number is the useful part when deciding what to build next.
 */
export default function Placeholder({ title, endpoints }) {
  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">{title}</h1>
      </div>
      <section className="card placeholder">
        <Construction size={26} aria-hidden="true" />
        <h2 className="card-title">{title} is not built yet</h2>
        <p className="placeholder-text">
          {endpoints
            ? `${endpoints} endpoint${endpoints === 1 ? '' : 's'} are built on the backend and `
              + 'have no controls here yet. This route exists so the navigation, active states '
              + 'and layout work end to end while the screens are filled in.'
            : 'No screen answers this address.'}
        </p>
      </section>
    </div>
  )
}
