/**
 * The side panel: which module you are in, and which of its screens.
 *
 * ONE MODULE TODAY. `core` is the only one the frontend has screens for — the school list, and
 * inside a school its overview, settings and academic year. `plans` has a built API and no
 * screens yet, so it is listed and disabled rather than hidden: a panel that showed only Core
 * would suggest Core is all there is, and the next person would go looking for the plans screens
 * in the wrong place.
 *
 * The screens themselves are not described here twice — a school's sections come from
 * SCHOOL_TABS, which is what SchoolDetail actually renders, so the panel cannot list a tab that
 * does not exist.
 */

import { Boxes, School as SchoolIcon, ChevronRight, Lock } from 'lucide-react';
import { SCHOOL_TABS } from '../pages/SchoolDetail.jsx';
import { Badge } from './ui.jsx';

/** What each module is, and whether the frontend has anything to show for it. */
export const MODULES = [
  {
    id: 'core',
    label: 'Core',
    description: 'Schools and their academic years',
    ready: true,
  },
  {
    id: 'plans',
    label: 'Plans',
    description: 'Plan catalogue and subscriptions',
    ready: false,
    note: 'API built, no screens yet',
  },
];

export default function ModulePanel({ moduleId, onModule, school, tab, onTab, onSchools }) {
  return (
    <nav className="rounded-xl border border-slate-200 bg-white shadow-sm">
      <p className="px-3 pt-3 text-[10px] font-semibold uppercase tracking-widest text-slate-400">
        Modules
      </p>

      <div className="p-2">
        {MODULES.map((module) => {
          const active = module.id === moduleId && module.ready;
          return (
            <div key={module.id} className="mb-1">
              <button
                type="button"
                disabled={!module.ready}
                onClick={() => onModule(module.id)}
                title={module.ready ? module.description : module.note}
                className={`flex w-full items-start gap-2 rounded-lg px-2.5 py-2 text-left transition ${
                  active
                    ? 'bg-slate-900 text-white'
                    : module.ready
                      ? 'text-slate-700 hover:bg-slate-100'
                      : 'cursor-not-allowed text-slate-400'
                }`}
              >
                {module.ready
                  ? <Boxes size={14} className={`mt-0.5 shrink-0 ${active ? 'text-white/70' : 'text-slate-400'}`} />
                  : <Lock size={14} className="mt-0.5 shrink-0 text-slate-300" />}
                <span className="min-w-0 flex-1">
                  <span className="block text-xs font-semibold">{module.label}</span>
                  <span className={`block text-[11px] ${active ? 'text-white/60' : 'text-slate-400'}`}>
                    {module.ready ? module.description : module.note}
                  </span>
                </span>
              </button>

              {/* Core's screens. Only the module in view lists them. */}
              {active && module.id === 'core' && (
                <ul className="mt-1 space-y-0.5 pl-1">
                  <li>
                    <button
                      type="button"
                      onClick={onSchools}
                      className={`flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-xs transition ${
                        school ? 'text-slate-700 hover:bg-slate-50' : 'bg-blue-50 text-slate-900 ring-1 ring-blue-200'
                      }`}
                    >
                      <SchoolIcon size={13} className="shrink-0 text-slate-400" />
                      <span className="flex-1">Schools</span>
                      {school && <ChevronRight size={13} className="text-slate-300" />}
                    </button>
                  </li>

                  {/* A school's own screens, but only while one is open — they need a school to
                      be about, and a dead link to Settings with nothing selected is worse than
                      no link. */}
                  {school && (
                    <li className="pl-2">
                      <p className="truncate px-2 pb-1 pt-1.5 text-[11px] font-medium text-slate-500"
                        title={school.schoolName}>
                        {school.schoolName}
                      </p>
                      <ul className="space-y-0.5">
                        {SCHOOL_TABS.map((one) => (
                          <li key={one.id}>
                            <button
                              type="button"
                              onClick={() => onTab(one.id)}
                              className={`flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-xs transition ${
                                tab === one.id
                                  ? 'bg-blue-50 text-slate-900 ring-1 ring-blue-200'
                                  : 'text-slate-600 hover:bg-slate-50'
                              }`}
                            >
                              <one.icon size={13} className="shrink-0 text-slate-400" />
                              {one.label}
                            </button>
                          </li>
                        ))}
                      </ul>
                    </li>
                  )}
                </ul>
              )}
            </div>
          );
        })}
      </div>

      <p className="border-t border-slate-100 px-3 py-2.5 text-[11px] leading-relaxed text-slate-400">
        More modules appear here as the frontend gets screens for them.
      </p>
    </nav>
  );
}
