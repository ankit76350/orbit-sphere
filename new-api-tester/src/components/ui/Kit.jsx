import { useEffect } from 'react'
import { Loader2, X } from 'lucide-react'

/**
 * The everyday pieces the screens are built from.
 *
 * One file rather than one each: they are a handful of lines apiece and always arrive together,
 * and a directory of nine two-line files is harder to read than this.
 */

export function Button({ look, busy, disabled, icon: Icon, children, ...rest }) {
  return (
    <button
      type="button"
      className="btn"
      data-look={look || undefined}
      disabled={disabled || busy}
      {...rest}
    >
      {busy ? <Loader2 size={13} className="spin" /> : Icon ? <Icon size={13} /> : null}
      {children}
    </button>
  )
}

export function Badge({ tone, children, title }) {
  return <span className="badge" data-tone={tone || undefined} title={title}>{children}</span>
}

export function Field({ label, required, hint, error, children }) {
  return (
    <label className="field">
      <span className="field-label">
        {label}
        {required ? <span> *</span> : null}
      </span>
      {children}
      {error
        ? <span className="field-error">{error}</span>
        : hint ? <span className="field-hint">{hint}</span> : null}
    </label>
  )
}

export function Input({ error, ...rest }) {
  return <input className="input" aria-invalid={error ? 'true' : undefined} {...rest} />
}

export function TextArea({ error, ...rest }) {
  return <textarea className="textarea" aria-invalid={error ? 'true' : undefined} rows={3} {...rest} />
}

export function Card({ title, description, action, children }) {
  return (
    <section className="card">
      {(title || action) && (
        <div className="card-head">
          <div>
            {title && <h2 className="card-title">{title}</h2>}
            {description && <p className="muted">{description}</p>}
          </div>
          {action && <div className="card-head-tools">{action}</div>}
        </div>
      )}
      {children}
    </section>
  )
}

export function Empty({ title, description, action }) {
  return (
    <div className="placeholder">
      <h2 className="card-title">{title}</h2>
      {description && <p className="placeholder-text">{description}</p>}
      {action}
    </div>
  )
}

/**
 * A dialog. Closes on Escape, and the scrim is a real button so a click anywhere outside is a
 * close rather than something only a mouse user can discover.
 */
export function Modal({ open, onClose, title, description, footer, children }) {
  useEffect(() => {
    if (!open) return undefined
    const onKey = (event) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
      <button type="button" className="modal-scrim" aria-label="Close" onClick={onClose} />
      <div className="modal-card">
        <div className="modal-head">
          <div>
            <h2 className="card-title">{title}</h2>
            {description && <p className="muted">{description}</p>}
          </div>
          <Button look="quiet" onClick={onClose} aria-label="Close"><X size={15} /></Button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  )
}
