import { ChevronDown } from 'lucide-react'

export default function Select({ value, onChange, options, label }) {
  return (
    <div className="select">
      <label className="sr-only" htmlFor={`select-${label}`}>{label}</label>
      <select
        id={`select-${label}`}
        className="select-input"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      >
        {options.map((o) => (
          <option key={o} value={o}>{o}</option>
        ))}
      </select>
      <ChevronDown size={15} className="select-caret" aria-hidden="true" />
    </div>
  )
}
