import { Construction } from 'lucide-react'

export default function Placeholder({ title }) {
  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">{title}</h1>
      </div>
      <section className="card placeholder">
        <Construction size={26} aria-hidden="true" />
        <h2 className="card-title">{title} is not built yet</h2>
        <p className="placeholder-text">
          The Loans dashboard is the screen implemented in this app. This route exists so the
          navigation, active states and layout can be exercised end to end.
        </p>
      </section>
    </div>
  )
}
