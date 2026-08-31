/**
 * The small pieces used in several places: method badges, status pills, the key/value table
 * behind the Params and Headers tabs, and a plain tab strip.
 */

import { Plus, Trash2 } from 'lucide-react';

/** One colour per HTTP method, the same everywhere so the sidebar and the URL bar agree. */
export const METHOD_STYLE = {
  GET: 'text-emerald-300 bg-emerald-500/10 border-emerald-500/30',
  POST: 'text-amber-300 bg-amber-500/10 border-amber-500/30',
  PUT: 'text-sky-300 bg-sky-500/10 border-sky-500/30',
  PATCH: 'text-violet-300 bg-violet-500/10 border-violet-500/30',
  DELETE: 'text-rose-300 bg-rose-500/10 border-rose-500/30',
};

export const HTTP_METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];

export function MethodBadge({ method, className = '' }) {
  return (
    <span
      className={`inline-flex shrink-0 items-center justify-center rounded border px-1.5 py-0.5 font-mono text-[10px] font-bold tracking-wide ${
        METHOD_STYLE[method] || 'text-slate-300 bg-slate-500/10 border-slate-500/30'
      } ${className}`}
    >
      {method}
    </span>
  );
}

const TONE_STYLE = {
  emerald: 'text-emerald-300 bg-emerald-500/10 border-emerald-500/30',
  amber: 'text-amber-300 bg-amber-500/10 border-amber-500/30',
  rose: 'text-rose-300 bg-rose-500/10 border-rose-500/30',
  sky: 'text-sky-300 bg-sky-500/10 border-sky-500/30',
  violet: 'text-violet-300 bg-violet-500/10 border-violet-500/30',
  slate: 'text-slate-300 bg-slate-500/10 border-slate-500/30',
};

export function Pill({ tone = 'slate', children, className = '', title }) {
  return (
    <span
      title={title}
      className={`inline-flex items-center gap-1 rounded-md border px-2 py-0.5 text-[11px] font-medium ${
        TONE_STYLE[tone] || TONE_STYLE.slate
      } ${className}`}
    >
      {children}
    </span>
  );
}

/** A plain tab strip. Controlled, so the panels decide which tab is showing. */
export function TabBar({ tabs, active, onChange, className = '' }) {
  return (
    <div className={`flex flex-wrap items-center gap-1 border-b border-slate-700/70 ${className}`}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          onClick={() => onChange(tab.id)}
          className={`-mb-px border-b-2 px-3 py-2 text-xs font-medium transition ${
            active === tab.id
              ? 'border-sky-400 text-sky-200'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          {tab.label}
          {tab.count != null && tab.count > 0 && (
            <span className="ml-1.5 rounded bg-slate-700/80 px-1.5 py-0.5 font-mono text-[10px] text-slate-300">
              {tab.count}
            </span>
          )}
          {tab.dot && <span className="ml-1.5 inline-block h-1.5 w-1.5 rounded-full bg-amber-400" />}
        </button>
      ))}
    </div>
  );
}

/**
 * The editable table behind the Params and Headers tabs.
 * A row can be switched off rather than deleted, which is how you try a call with and without
 * one header without retyping it.
 */
export function KeyValueEditor({ rows, onChange, keyPlaceholder = 'Key', valuePlaceholder = 'Value', emptyHint }) {
  const update = (index, patch) => onChange(rows.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  const add = () => onChange([...rows, { key: '', value: '', enabled: true }]);
  const remove = (index) => onChange(rows.filter((_, i) => i !== index));

  return (
    <div className="space-y-2">
      {rows.length === 0 && emptyHint && (
        <p className="rounded-md border border-dashed border-slate-700 px-3 py-4 text-center text-xs text-slate-500">
          {emptyHint}
        </p>
      )}
      {rows.length > 0 && (
        <div className="overflow-hidden rounded-lg border border-slate-700/70">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-800/60 text-[10px] uppercase tracking-wide text-slate-500">
              <tr>
                <th className="w-9 px-2 py-1.5" />
                <th className="px-2 py-1.5 font-medium">Key</th>
                <th className="px-2 py-1.5 font-medium">Value</th>
                <th className="w-9 px-2 py-1.5" />
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => (
                <tr key={index} className="border-t border-slate-700/50">
                  <td className="px-2 py-1">
                    <input
                      type="checkbox"
                      checked={row.enabled !== false}
                      onChange={(event) => update(index, { enabled: event.target.checked })}
                      className="h-3.5 w-3.5 accent-sky-500"
                      title="Include this row in the request"
                    />
                  </td>
                  <td className="px-2 py-1">
                    <input
                      value={row.key}
                      onChange={(event) => update(index, { key: event.target.value })}
                      placeholder={keyPlaceholder}
                      className="w-full bg-transparent font-mono text-[11px] text-slate-200 placeholder:text-slate-600 focus:outline-none"
                    />
                  </td>
                  <td className="px-2 py-1">
                    <input
                      value={row.value}
                      onChange={(event) => update(index, { value: event.target.value })}
                      placeholder={valuePlaceholder}
                      className="w-full bg-transparent font-mono text-[11px] text-slate-200 placeholder:text-slate-600 focus:outline-none"
                    />
                  </td>
                  <td className="px-2 py-1">
                    <button
                      type="button"
                      onClick={() => remove(index)}
                      className="rounded p-1 text-slate-500 hover:bg-rose-500/10 hover:text-rose-300"
                      title="Remove this row"
                    >
                      <Trash2 size={12} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <button
        type="button"
        onClick={add}
        className="inline-flex items-center gap-1.5 rounded-md border border-slate-700 px-2.5 py-1 text-[11px] text-slate-400 hover:border-slate-600 hover:text-slate-200"
      >
        <Plus size={12} /> Add row
      </button>
    </div>
  );
}

/** A labelled field, used in the settings and auth panels. */
export function Field({ label, hint, children }) {
  return (
    <label className="block space-y-1">
      <span className="block text-[11px] font-medium uppercase tracking-wide text-slate-500">{label}</span>
      {children}
      {hint && <span className="block text-[11px] text-slate-500">{hint}</span>}
    </label>
  );
}

export const inputClass =
  'w-full rounded-md border border-slate-700 bg-slate-800/70 px-2.5 py-1.5 text-xs text-slate-200 placeholder:text-slate-600 focus:border-sky-500 focus:outline-none';
