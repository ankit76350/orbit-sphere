/**
 * Every call made in this browser, newest first. Inspect opens what actually happened;
 * Resend runs the same request again.
 */

import { useMemo, useState } from 'react';
import { Search, Trash2, RotateCw, Eye, WifiOff } from 'lucide-react';
import { MethodBadge, Pill } from './ui.jsx';
import { formatBytes, formatDateTime, formatDuration, statusTone, timeAgo } from '../lib/format.js';

const TONE_TEXT = {
  emerald: 'text-emerald-300',
  amber: 'text-amber-300',
  rose: 'text-rose-300',
  sky: 'text-sky-300',
};

export default function HistoryPanel({ history, onInspect, onResend, onClear, activeId }) {
  const [query, setQuery] = useState('');
  const [failuresOnly, setFailuresOnly] = useState(false);

  const rows = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return history.filter((entry) => {
      if (failuresOnly && entry.ok) return false;
      if (!needle) return true;
      return (
        entry.endpointName.toLowerCase().includes(needle) ||
        entry.path.toLowerCase().includes(needle) ||
        String(entry.status || '').includes(needle) ||
        entry.method.toLowerCase().includes(needle)
      );
    });
  }, [history, query, failuresOnly]);

  const failures = history.filter((entry) => !entry.ok).length;

  return (
    <aside className="flex h-full min-h-0 w-80 shrink-0 flex-col border-l border-slate-800 bg-slate-900/60">
      <div className="space-y-2 border-b border-slate-800 p-3">
        <div className="flex items-center justify-between">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400">History</h3>
          <button
            type="button"
            onClick={onClear}
            disabled={history.length === 0}
            className="inline-flex items-center gap-1 rounded px-1.5 py-1 text-[11px] text-slate-500 hover:text-rose-300 disabled:opacity-40 disabled:hover:text-slate-500"
          >
            <Trash2 size={11} /> Clear
          </button>
        </div>
        <div className="relative">
          <Search size={12} className="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Filter by path, status or method"
            className="w-full rounded-md border border-slate-700 bg-slate-800/70 py-1.5 pl-7 pr-2 text-[11px] text-slate-200 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none"
          />
        </div>
        <div className="flex items-center justify-between">
          <button
            type="button"
            onClick={() => setFailuresOnly((was) => !was)}
            className={`rounded-md border px-2 py-0.5 text-[11px] transition ${
              failuresOnly
                ? 'border-rose-500/40 bg-rose-500/10 text-rose-300'
                : 'border-slate-700 text-slate-400 hover:text-slate-200'
            }`}
          >
            Failures only
          </button>
          <span className="text-[11px] text-slate-500">
            {history.length} call{history.length === 1 ? '' : 's'}
            {failures > 0 && `, ${failures} failed`}
          </span>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto">
        {rows.length === 0 && (
          <p className="px-4 py-10 text-center text-xs text-slate-500">
            {history.length === 0
              ? 'Nothing sent yet. Every call you make gets recorded here.'
              : 'Nothing matches that filter.'}
          </p>
        )}
        <ul>
          {rows.map((entry) => (
            <li key={entry.id}>
              <div
                className={`group border-l-2 px-3 py-2 transition ${
                  activeId === entry.id ? 'border-sky-400 bg-sky-500/10' : 'border-transparent hover:bg-slate-800/50'
                }`}
              >
                <div className="flex items-center gap-2">
                  <MethodBadge method={entry.method} />
                  <span className={`font-mono text-xs font-bold ${TONE_TEXT[statusTone(entry.status)]}`}>
                    {entry.status ?? 'ERR'}
                  </span>
                  <span className="ml-auto font-mono text-[10px] text-slate-500">
                    {formatDuration(entry.durationMs)}
                  </span>
                </div>
                <p className="mt-1 truncate font-mono text-[11px] text-slate-300" title={entry.path}>
                  {entry.path}
                </p>
                <div className="mt-1 flex items-center gap-2 text-[10px] text-slate-500">
                  <span title={formatDateTime(entry.at)}>{timeAgo(entry.at)}</span>
                  <span>·</span>
                  <span>{formatBytes(entry.sizeBytes)}</span>
                  {entry.environmentName && (
                    <>
                      <span>·</span>
                      <span className="truncate">{entry.environmentName}</span>
                    </>
                  )}
                </div>
                {entry.errorTitle && (
                  <Pill tone="rose" className="mt-1.5">
                    <WifiOff size={9} /> {entry.errorTitle}
                  </Pill>
                )}
                <div className="mt-1.5 flex gap-1 opacity-0 transition group-hover:opacity-100">
                  <button
                    type="button"
                    onClick={() => onInspect(entry)}
                    className="inline-flex items-center gap-1 rounded border border-slate-700 px-1.5 py-0.5 text-[10px] text-slate-400 hover:border-sky-500/50 hover:text-sky-200"
                  >
                    <Eye size={10} /> Inspect
                  </button>
                  <button
                    type="button"
                    onClick={() => onResend(entry)}
                    className="inline-flex items-center gap-1 rounded border border-slate-700 px-1.5 py-0.5 text-[10px] text-slate-400 hover:border-sky-500/50 hover:text-sky-200"
                  >
                    <RotateCw size={10} /> Resend
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </aside>
  );
}
