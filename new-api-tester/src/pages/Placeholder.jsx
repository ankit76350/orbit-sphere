import { Construction } from 'lucide-react'

/**
 * What every screen shows until it is built.
 *
 * It names the endpoint group waiting for it and how many endpoints that is, because that is the
 * useful part when choosing what to build next — and the group name is the exact string to look
 * up in the generated catalogue.
 */
export default function Placeholder({ title, surface, module, group, endpoints }) {
  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">{title}</h1>
      </div>
      <section className="card placeholder">
        <Construction size={26} aria-hidden="true" />
        <h2 className="card-title">{title} is not built yet</h2>
        <p className="placeholder-text">
          {group
            ? `${endpoints} endpoint${endpoints === 1 ? '' : 's'} are built on the backend under `
              + `"${group}" and have no controls here yet. This screen acts as the `
              + `${surface.toLowerCase()} surface of ${module}.`
            : 'No screen answers this address.'}
        </p>
      </section>
    </div>
  )
}
