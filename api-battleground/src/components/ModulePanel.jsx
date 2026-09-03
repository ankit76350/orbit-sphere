/**
 * The side panel: which module, and which of its screens.
 *
 * TWO MODULES. `core` is schools and their academic years; `plans` is the catalogue — what we
 * sell and at what price. A module with no screens yet is listed and disabled rather than hidden,
 * because a panel showing only what is finished suggests that is all there is.
 *
 * ONE LEVEL, DELIBERATELY. It answers one question — which module — and nothing else. An
 * earlier version nested the module's screens and then the open thing's tabs underneath, three
 * levels of indent in a 15rem column, with the module filled near-black and a tab ringed in
 * blue: two things looked "selected" in two different languages and neither read as the current
 * screen.
 *
 * A module's own screens are a navbar inside the module — see ModuleNav — which is where there
 * is room for them.
 */

import { School as SchoolIcon, Package, Lock } from 'lucide-react';

/** What each module is, and whether the frontend has anything to show for it. */
export const MODULES = [
  {
    id: 'core',
    label: 'Core',
    description: 'Schools and academic years',
    icon: SchoolIcon,
    ready: true,
  },
  {
    id: 'plans',
    label: 'Plans',
    description: 'What we sell, and for how much',
    icon: Package,
    ready: true,
  },
];

export default function ModulePanel({ moduleId, onModule }) {
  return (
    <nav aria-label="Modules" className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <p className="px-3 pb-1 pt-3 text-[10px] font-semibold uppercase tracking-widest text-slate-400">
        Modules
      </p>

      <div className="space-y-1 p-2 pt-1">
        {MODULES.map((module) => {
          const here = module.id === moduleId && module.ready;
          const Icon = module.ready ? module.icon : Lock;

          return (
            <section key={module.id}>
              {/* The module itself. A tint and a left bar rather than a filled block: it says
                  "you are in here", which is different from "this is the screen you are on". */}
              <button
                type="button"
                disabled={!module.ready}
                onClick={() => onModule(module.id)}
                title={module.ready ? module.description : 'API built, no screens yet'}
                className={`flex w-full items-start gap-2.5 rounded-lg border-l-2 py-2 pl-2.5 pr-2 text-left transition
                  focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/40 ${
                  !module.ready
                    ? 'cursor-not-allowed border-transparent'
                    : here
                      ? 'border-blue-600 bg-slate-50'
                      : 'border-transparent hover:bg-slate-50'
                }`}
              >
                <span
                  className={`mt-px flex h-6 w-6 shrink-0 items-center justify-center rounded-md ${
                    !module.ready ? 'bg-slate-100' : here ? 'bg-blue-600' : 'bg-slate-100'
                  }`}
                >
                  <Icon
                    size={13}
                    className={!module.ready ? 'text-slate-300' : here ? 'text-white' : 'text-slate-500'}
                  />
                </span>
                <span className="min-w-0 flex-1">
                  <span className={`block text-[13px] font-semibold leading-5 ${
                    !module.ready ? 'text-slate-400' : here ? 'text-slate-900' : 'text-slate-700'
                  }`}>
                    {module.label}
                  </span>
                  <span className="block truncate text-[11px] leading-4 text-slate-400">
                    {module.ready ? module.description : 'No screens yet'}
                  </span>
                </span>
              </button>

            </section>
          );
        })}
      </div>

      <p className="border-t border-slate-100 bg-slate-50/60 px-3 py-2.5 text-[11px] leading-relaxed text-slate-500">
        More modules appear here as the frontend gets screens for them.
      </p>
    </nav>
  );
}
