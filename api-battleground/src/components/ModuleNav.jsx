/**
 * The navbar inside a module: its screens, and where you are in them.
 *
 * The side panel answers one question — which module — and stops there. This answers the next
 * one, and it lives in the content column because that is where there is room for it: a row of
 * labels reads at a glance, where the same list indented three deep in a narrow panel did not.
 *
 * IT IS ALSO THE WAY BACK. When something is open the bar keeps its screen selected and shows
 * what is open after it, so "Catalogue › PREMIUM v2" is both a statement of where you are and a
 * link out of it. A module with one screen still earns the bar for that reason.
 *
 * The screens are not listed here. Each module passes its own, so this cannot offer a screen
 * that does not exist.
 */

import { ChevronRight } from 'lucide-react';

export default function ModuleNav({ moduleLabel, screens, screenId, onScreen, openName }) {
  return (
    <div className="mb-5 flex flex-wrap items-center gap-x-1.5 gap-y-2 border-b border-slate-200 pb-3">
      <span className="mr-1 text-[11px] font-semibold uppercase tracking-widest text-slate-400">
        {moduleLabel}
      </span>

      {screens.map((screen) => {
        const current = screen.id === screenId;
        return (
          <button
            key={screen.id}
            type="button"
            onClick={() => onScreen(screen.id)}
            aria-current={current && !openName ? 'page' : undefined}
            className={`inline-flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-[13px] transition
              focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/40 ${
              current
                ? 'bg-blue-50 font-medium text-blue-900'
                : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
            }`}
          >
            {screen.icon && (
              <screen.icon size={14} className={current ? 'text-blue-600' : 'text-slate-400'} />
            )}
            {screen.label}
          </button>
        );
      })}

      {/* What is open, after the screen it was opened from. Clicking the screen goes back. */}
      {openName && (
        <>
          <ChevronRight size={14} className="text-slate-300" />
          <span
            aria-current="page"
            className="max-w-[22rem] truncate rounded-lg bg-slate-100 px-2.5 py-1.5 text-[13px] font-medium text-slate-800"
            title={openName}
          >
            {openName}
          </span>
        </>
      )}
    </div>
  );
}
