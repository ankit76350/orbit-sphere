/**
 * The list of endpoints down the left. Grouped the way the backend packages are, searchable
 * by name, path or method, and filterable to just the ones that are built.
 */

import { useMemo, useState } from 'react';
import { Search, ChevronDown, CircleDot, CircleDashed } from 'lucide-react';
import { API_CATALOG, LIVE_COUNT, PLANNED_COUNT } from '../config/endpoints.js';
import { MethodBadge } from './ui.jsx';

export default function Sidebar({ selectedId, onSelect }) {
  const [query, setQuery] = useState('');
  const [liveOnly, setLiveOnly] = useState(false);
  const [collapsed, setCollapsed] = useState({});

  const groups = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return API_CATALOG.map((group) => ({
      ...group,
      endpoints: group.endpoints.filter((endpoint) => {
        if (liveOnly && endpoint.status !== 'live') return false;
        if (!needle) return true;
        return (
          endpoint.name.toLowerCase().includes(needle) ||
          endpoint.path.toLowerCase().includes(needle) ||
          endpoint.method.toLowerCase().includes(needle) ||
          (endpoint.summary || '').toLowerCase().includes(needle)
        );
      }),
    })).filter((group) => group.endpoints.length > 0);
  }, [query, liveOnly]);

  const shown = groups.reduce((total, group) => total + group.endpoints.length, 0);

  return (
    <aside className="flex h-full min-h-0 w-72 shrink-0 flex-col border-r border-slate-800 bg-slate-900/60">
      <div className="space-y-2 border-b border-slate-800 p-3">
        <div className="relative">
          <Search size={13} className="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search endpoints"
            className="w-full rounded-md border border-slate-700 bg-slate-800/70 py-1.5 pl-8 pr-2 text-xs text-slate-200 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none"
          />
        </div>
        <div className="flex items-center justify-between">
          <button
            type="button"
            onClick={() => setLiveOnly((was) => !was)}
            className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-[11px] transition ${
              liveOnly
                ? 'border-emerald-500/40 bg-emerald-500/10 text-emerald-300'
                : 'border-slate-700 text-slate-400 hover:text-slate-200'
            }`}
            title="Hide the endpoints that are not built yet"
          >
            {liveOnly ? <CircleDot size={11} /> : <CircleDashed size={11} />}
            Built only
          </button>
          <span className="text-[11px] text-slate-500">
            {shown} shown · {LIVE_COUNT} built, {PLANNED_COUNT} planned
          </span>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto py-2">
        {groups.length === 0 && (
          <p className="px-4 py-8 text-center text-xs text-slate-500">Nothing matches "{query}".</p>
        )}
        {groups.map((group) => {
          const isOpen = !collapsed[group.id];
          return (
            <div key={group.id} className="mb-1">
              <button
                type="button"
                onClick={() => setCollapsed((was) => ({ ...was, [group.id]: isOpen }))}
                className="flex w-full items-center gap-1.5 px-3 py-1.5 text-left text-[11px] font-semibold uppercase tracking-wide text-slate-400 hover:text-slate-200"
              >
                <ChevronDown size={12} className={`transition-transform ${isOpen ? '' : '-rotate-90'}`} />
                <span className="truncate">{group.module}</span>
                <span className="ml-auto font-mono text-[10px] font-normal text-slate-600">
                  {group.endpoints.length}
                </span>
              </button>
              {isOpen && (
                <ul>
                  {group.endpoints.map((endpoint) => {
                    const isSelected = endpoint.id === selectedId;
                    const isPlanned = endpoint.status === 'planned';
                    return (
                      <li key={endpoint.id}>
                        <button
                          type="button"
                          onClick={() => onSelect(endpoint.id)}
                          title={endpoint.summary}
                          className={`flex w-full items-center gap-2 border-l-2 py-1.5 pl-4 pr-3 text-left transition ${
                            isSelected ? 'border-sky-400 bg-sky-500/10' : 'border-transparent hover:bg-slate-800/60'
                          }`}
                        >
                          <MethodBadge method={endpoint.method} className={isPlanned ? 'opacity-50' : ''} />
                          <span
                            className={`truncate text-xs ${
                              isSelected ? 'text-slate-100' : isPlanned ? 'text-slate-500' : 'text-slate-300'
                            }`}
                          >
                            {endpoint.name}
                          </span>
                          {isPlanned && (
                            <span className="ml-auto shrink-0 rounded bg-slate-800 px-1.5 py-0.5 text-[9px] uppercase tracking-wide text-slate-500">
                              plan
                            </span>
                          )}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          );
        })}
      </div>

      <div className="border-t border-slate-800 px-3 py-2 text-[10px] leading-relaxed text-slate-600">
        Read from SchoolController, the core DTOs and controllers/core/README.md. "Plan"
        endpoints are not built — sending one returns 404.
      </div>
    </aside>
  );
}
