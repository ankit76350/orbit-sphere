/**
 * The everyday pieces the screens are built from — buttons, form fields, cards, tables and
 * dialogs. Ordinary application parts, named after what they are on screen.
 */

import { useEffect } from 'react';
import { X, Loader2, Search, Inbox, AlertCircle } from 'lucide-react';

/* ---------------------------------------------------------------- buttons */

const BUTTON_LOOK = {
  primary: 'bg-blue-600 text-white hover:bg-blue-700 disabled:bg-blue-300 shadow-sm',
  secondary: 'bg-white text-slate-700 border border-slate-300 hover:bg-slate-50 disabled:text-slate-400',
  danger: 'bg-white text-red-600 border border-red-300 hover:bg-red-50 disabled:text-red-300',
  solidDanger: 'bg-red-600 text-white hover:bg-red-700 disabled:bg-red-300 shadow-sm',
  quiet: 'text-slate-600 hover:bg-slate-100 disabled:text-slate-300',
};

const BUTTON_SIZE = {
  sm: 'px-2.5 py-1.5 text-xs gap-1.5',
  md: 'px-3.5 py-2 text-sm gap-2',
  lg: 'px-5 py-2.5 text-sm gap-2',
};

export function Button({
  look = 'secondary',
  size = 'md',
  icon: Icon,
  busy = false,
  children,
  className = '',
  ...rest
}) {
  return (
    <button
      type="button"
      {...rest}
      disabled={rest.disabled || busy}
      className={`inline-flex items-center justify-center rounded-lg font-medium transition disabled:cursor-not-allowed ${
        BUTTON_LOOK[look]
      } ${BUTTON_SIZE[size]} ${className}`}
    >
      {busy ? <Loader2 size={15} className="animate-spin" /> : Icon ? <Icon size={15} /> : null}
      {children}
    </button>
  );
}

/* ------------------------------------------------------------------ cards */

export function Card({ title, description, action, children, className = '' }) {
  return (
    <section className={`rounded-xl border border-slate-200 bg-white shadow-sm ${className}`}>
      {(title || action) && (
        <header className="flex items-start justify-between gap-4 border-b border-slate-100 px-5 py-4">
          <div>
            {title && <h2 className="text-sm font-semibold text-slate-900">{title}</h2>}
            {description && <p className="mt-0.5 text-xs text-slate-500">{description}</p>}
          </div>
          {action}
        </header>
      )}
      <div className="p-5">{children}</div>
    </section>
  );
}

/* ----------------------------------------------------------------- badges */

const BADGE_LOOK = {
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  amber: 'bg-amber-50 text-amber-800 ring-amber-600/20',
  red: 'bg-red-50 text-red-700 ring-red-600/20',
  blue: 'bg-blue-50 text-blue-700 ring-blue-600/20',
  violet: 'bg-violet-50 text-violet-700 ring-violet-600/20',
  grey: 'bg-slate-100 text-slate-600 ring-slate-500/20',
};

export function Badge({ look = 'grey', children, className = '', title }) {
  return (
    <span
      title={title}
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-medium ring-1 ring-inset ${
        BADGE_LOOK[look] || BADGE_LOOK.grey
      } ${className}`}
    >
      {children}
    </span>
  );
}

/** Says a school's lifecycle status in words a person would use. */
const STATUS_LOOK = {
  ACTIVE: ['green', 'Live'],
  TRIAL: ['blue', 'On trial'],
  PROVISIONING: ['amber', 'Being set up'],
  SUSPENDED: ['red', 'Suspended'],
  OFFBOARDING: ['amber', 'Leaving'],
  CLOSED: ['grey', 'Closed'],
  DELETION_PENDING: ['red', 'To be deleted'],
  DELETED: ['grey', 'Deleted'],
};

export function StatusBadge({ status }) {
  const [look, label] = STATUS_LOOK[status] || ['grey', status || 'Unknown'];
  return (
    <Badge look={look} title={status}>
      {label}
    </Badge>
  );
}

/* ------------------------------------------------------------ form fields */

export function Field({ label, hint, error, required, children, className = '' }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1 block text-xs font-medium text-slate-700">
        {label}
        {required && <span className="ml-0.5 text-red-500">*</span>}
      </span>
      {children}
      {error ? (
        <span className="mt-1 flex items-start gap-1 text-xs text-red-600">
          <AlertCircle size={12} className="mt-0.5 shrink-0" />
          {error}
        </span>
      ) : (
        hint && <span className="mt-1 block text-xs text-slate-500">{hint}</span>
      )}
    </label>
  );
}

const FIELD_BASE =
  'w-full rounded-lg border bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition focus:outline-none focus:ring-2 focus:ring-blue-500/30 disabled:bg-slate-50 disabled:text-slate-400';

export function TextInput({ error, className = '', ...rest }) {
  return (
    <input
      {...rest}
      className={`${FIELD_BASE} ${error ? 'border-red-400 focus:border-red-500' : 'border-slate-300 focus:border-blue-500'} ${className}`}
    />
  );
}

export function TextArea({ error, className = '', ...rest }) {
  return (
    <textarea
      {...rest}
      className={`${FIELD_BASE} ${error ? 'border-red-400 focus:border-red-500' : 'border-slate-300 focus:border-blue-500'} ${className}`}
    />
  );
}

export function SelectInput({ error, children, className = '', ...rest }) {
  return (
    <select
      {...rest}
      className={`${FIELD_BASE} ${error ? 'border-red-400' : 'border-slate-300 focus:border-blue-500'} ${className}`}
    >
      {children}
    </select>
  );
}

export function SearchInput({ className = '', ...rest }) {
  return (
    <div className={`relative ${className}`}>
      <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
      <input
        {...rest}
        className={`${FIELD_BASE} border-slate-300 pl-9 focus:border-blue-500`}
      />
    </div>
  );
}

/** An on/off switch, for the settings that really are a switch. */
export function Toggle({ checked, onChange, disabled, label, description, busy }) {
  return (
    <div className="flex items-start justify-between gap-4 py-3">
      <div className="min-w-0">
        <p className="text-sm font-medium text-slate-800">{label}</p>
        {description && <p className="mt-0.5 text-xs text-slate-500">{description}</p>}
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={Boolean(checked)}
        disabled={disabled || busy}
        onClick={() => onChange(!checked)}
        className={`relative mt-0.5 inline-flex h-6 w-11 shrink-0 items-center rounded-full transition disabled:opacity-50 ${
          checked ? 'bg-blue-600' : 'bg-slate-300'
        }`}
      >
        <span
          className={`inline-flex h-5 w-5 items-center justify-center rounded-full bg-white shadow transition ${
            checked ? 'translate-x-5' : 'translate-x-0.5'
          }`}
        >
          {busy && <Loader2 size={11} className="animate-spin text-slate-500" />}
        </span>
      </button>
    </div>
  );
}

/* ----------------------------------------------------------------- states */

export function EmptyState({ icon: Icon = Inbox, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 px-6 py-14 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-slate-100">
        <Icon size={22} className="text-slate-400" />
      </div>
      <div>
        <p className="text-sm font-medium text-slate-800">{title}</p>
        {description && <p className="mx-auto mt-1 max-w-md text-xs text-slate-500">{description}</p>}
      </div>
      {action}
    </div>
  );
}

export function Loading({ label = 'Loading…' }) {
  return (
    <div className="flex items-center justify-center gap-2 px-6 py-14 text-sm text-slate-500">
      <Loader2 size={16} className="animate-spin text-blue-600" />
      {label}
    </div>
  );
}

/** Rows of grey blocks while a table loads, so the page does not jump. */
export function SkeletonRows({ rows = 6, columns = 5 }) {
  return (
    <tbody>
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <tr key={rowIndex} className="border-t border-slate-100">
          {Array.from({ length: columns }).map((_, cellIndex) => (
            <td key={cellIndex} className="px-4 py-3">
              <div className="h-3 animate-pulse rounded bg-slate-100" style={{ width: `${45 + ((rowIndex + cellIndex) % 4) * 15}%` }} />
            </td>
          ))}
        </tr>
      ))}
    </tbody>
  );
}

/* ----------------------------------------------------------------- dialog */

export function Modal({ open, onClose, title, description, children, footer, width = 'max-w-lg' }) {
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (event) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto p-4 sm:p-8">
      <button
        type="button"
        aria-label="Close"
        onClick={onClose}
        className="fixed inset-0 bg-slate-900/40 backdrop-blur-[2px]"
      />
      <div className={`relative my-auto w-full ${width} rounded-2xl bg-white shadow-2xl`}>
        <header className="flex items-start justify-between gap-4 border-b border-slate-100 px-5 py-4">
          <div>
            <h2 className="text-base font-semibold text-slate-900">{title}</h2>
            {description && <p className="mt-0.5 text-xs text-slate-500">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="-mr-1 rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
          >
            <X size={18} />
          </button>
        </header>
        <div className="px-5 py-4">{children}</div>
        {footer && (
          <footer className="flex items-center justify-end gap-2 rounded-b-2xl border-t border-slate-100 bg-slate-50 px-5 py-3">
            {footer}
          </footer>
        )}
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ table */

export function Table({ head, children, className = '' }) {
  return (
    <div className={`overflow-x-auto ${className}`}>
      <table className="w-full text-left text-sm">
        <thead className="bg-slate-50 text-xs font-medium text-slate-500">
          <tr>{head}</tr>
        </thead>
        {children}
      </table>
    </div>
  );
}

export function Th({ children, sortable, active, direction, onClick, className = '' }) {
  if (!sortable) {
    return <th className={`whitespace-nowrap px-4 py-2.5 font-medium ${className}`}>{children}</th>;
  }
  return (
    <th className={`whitespace-nowrap px-4 py-2.5 font-medium ${className}`}>
      <button
        type="button"
        onClick={onClick}
        className={`inline-flex items-center gap-1 transition hover:text-slate-800 ${active ? 'text-slate-900' : ''}`}
      >
        {children}
        <span className={`text-[9px] ${active ? 'opacity-100' : 'opacity-30'}`}>
          {active && direction === 'desc' ? '▼' : '▲'}
        </span>
      </button>
    </th>
  );
}

export function Td({ children, className = '' }) {
  return <td className={`px-4 py-3 align-middle text-slate-700 ${className}`}>{children}</td>;
}

/* ------------------------------------------------------- small text bits */

/** A label and a value, the way a details panel shows one. */
export function Detail({ label, children, className = '' }) {
  return (
    <div className={className}>
      <dt className="text-[11px] font-medium uppercase tracking-wide text-slate-400">{label}</dt>
      <dd className="mt-0.5 break-words text-sm text-slate-800">
        {children === null || children === undefined || children === '' ? (
          <span className="text-slate-400">Not set</span>
        ) : (
          children
        )}
      </dd>
    </div>
  );
}
