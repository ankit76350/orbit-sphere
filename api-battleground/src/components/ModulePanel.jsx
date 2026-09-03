/**
 * The side panel: which module you are in, and which of its screens.
 *
 * TWO MODULES. `core` is schools and their academic years; `plans` is the catalogue — what we
 * sell and at what price. A module that has no screens yet is listed and disabled rather than
 * hidden, because a panel showing only what is finished suggests that is all there is.
 *
 * The screens are not described here twice. A school's sections come from SCHOOL_TABS and a
 * plan's from PLAN_TABS — the same arrays the detail screens render — so the panel cannot offer
 * a tab that does not exist.
 */

import { Boxes, School as SchoolIcon, Package, ChevronRight, Lock } from 'lucide-react';
import { SCHOOL_TABS } from '../pages/SchoolDetail.jsx';
import { PLAN_TABS } from '../pages/PlanDetail.jsx';
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
    description: 'What we sell, and at what price',
    ready: true,
  },
];

/**
 * One module's screens: a list, and — when something is open — its sections.
 *
 * Both modules have the same shape, so they share this rather than each growing their own copy:
 * a list you can always reach, and the thing you have opened with its tabs underneath.
 */
function ModuleScreens({ listLabel, listIcon: ListIcon, onList, openName, tabs, tab, onTab }) {
  return (
    <ul className="mt-1 space-y-0.5 pl-1">
      <li>
        <button
          type="button"
          onClick={onList}
          className={`flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-left text-xs transition ${
            openName ? 'text-slate-700 hover:bg-slate-50' : 'bg-blue-50 text-slate-900 ring-1 ring-blue-200'
          }`}
        >
          <ListIcon size={13} className="shrink-0 text-slate-400" />
          <span className="flex-1">{listLabel}</span>
          {openName && <ChevronRight size={13} className="text-slate-300" />}
        </button>
      </li>

      {/* The sections of whatever is open. They need something to be about, so a link to
          Features with no plan chosen is not offered at all. */}
      {openName && (
        <li className="pl-2">
          <p className="truncate px-2 pb-1 pt-1.5 text-[11px] font-medium text-slate-500" title={openName}>
            {openName}
          </p>
          <ul className="space-y-0.5">
            {tabs.map((one) => (
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
  );
}

export default function ModulePanel({
  moduleId, onModule, school, tab, onTab, onSchools, plan, planTab, onPlanTab, onPlans,
}) {
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

              {/* Only the module in view lists its screens. */}
              {active && module.id === 'core' && (
                <ModuleScreens
                  listLabel="Schools"
                  listIcon={SchoolIcon}
                  onList={onSchools}
                  openName={school?.schoolName}
                  tabs={SCHOOL_TABS}
                  tab={tab}
                  onTab={onTab}
                />
              )}

              {active && module.id === 'plans' && (
                <ModuleScreens
                  listLabel="Catalogue"
                  listIcon={Package}
                  onList={onPlans}
                  openName={plan && `${plan.planCode} v${plan.planVersion}`}
                  tabs={PLAN_TABS}
                  tab={planTab}
                  onTab={onPlanTab}
                />
              )}

            </div>
          );
        })}
      </div>

      <p className="border-t border-slate-100 px-3 py-2.5 text-[11px] leading-relaxed text-slate-400">
        Subscriptions are created from a school, under Core.
      </p>
    </nav>
  );
}
