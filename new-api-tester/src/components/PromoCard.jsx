import { ArrowRight } from 'lucide-react'

function SwapArt() {
  return (
    <svg viewBox="0 0 190 130" className="promo-art" aria-hidden="true">
      <circle cx="34" cy="96" r="17" fill="#f6b64b" />
      <circle cx="70" cy="112" r="12" fill="#f6b64b" opacity="0.85" />
      <circle cx="150" cy="104" r="14" fill="#f6b64b" opacity="0.9" />
      <circle cx="24" cy="62" r="9" fill="#f6b64b" opacity="0.7" />
      <rect x="66" y="14" width="66" height="92" rx="12" fill="#dfe3ff" />
      <rect x="74" y="22" width="50" height="76" rx="8" fill="#eef0ff" />
      <circle cx="128" cy="70" r="11" fill="#f6b64b" />
    </svg>
  )
}

function EarnArt() {
  return (
    <svg viewBox="0 0 190 130" className="promo-art" aria-hidden="true">
      <circle cx="52" cy="46" r="26" fill="#7fd6b0" />
      <circle cx="118" cy="34" r="18" fill="#7fd6b0" opacity="0.85" />
      <circle cx="150" cy="76" r="22" fill="#7fd6b0" opacity="0.9" />
      <circle cx="96" cy="86" r="14" fill="#7fd6b0" opacity="0.75" />
      <circle cx="168" cy="26" r="10" fill="#f6b64b" />
      <circle cx="30" cy="96" r="12" fill="#f6b64b" opacity="0.9" />
      <circle cx="104" cy="118" r="9" fill="#f6b64b" opacity="0.8" />
      <circle cx="176" cy="112" r="7" fill="#f6b64b" opacity="0.7" />
    </svg>
  )
}

const ART = { swap: SwapArt, earn: EarnArt }

export default function PromoCard({ art, title, highlight, highlightTone, cta, href = '#' }) {
  const Art = ART[art]
  const [before, after] = title.split('%s')

  return (
    <section className="card promo">
      <div className="promo-body">
        <h2 className="promo-title">
          {before}
          <span className="promo-highlight" data-tone={highlightTone}>{highlight}</span>
          {after}
        </h2>
        <a className="promo-cta" href={href}>
          {cta}
          <ArrowRight size={15} aria-hidden="true" />
        </a>
      </div>
      <Art />
    </section>
  )
}
