/**
 * What a plan includes. Endpoint #3.
 *
 * THE WHOLE LIST GOES AT ONCE, which is why this is a set of tick boxes and not an add/remove
 * list. A feature list is priced as a set — "2000 students and examinations, for this much" is
 * one offer — so there is no moment at which half of it is a plan. Ticking and saving replaces
 * the lot.
 *
 * The 24 features are a fixed list in the backend, each with its own label, description, and the
 * counter it is measured in. That last part is why the limit box only appears on some of them:
 * "attendance" is included or it is not, so a number on it would be a number nothing reads.
 */

import { useEffect, useState } from 'react';
import { Save, Lock, Gauge } from 'lucide-react';
import { useApi } from '../api/apiContext.js';
import { Card, Button, Badge, TextInput, SelectInput, EmptyState } from '../components/ui.jsx';
import EndpointTag from '../components/EndpointTag.jsx';

/**
 * The features, as the backend defines them.
 *
 * Mirrored here because there is no endpoint that lists them — they are a Java enum, and a
 * screen cannot ask for its contents. `metric` is null where a feature has nothing to count,
 * which is what decides whether a limit can be set at all. If the enum gains a constant, this
 * list needs the same one.
 */
const FEATURES = [
  ['STUDENT_MANAGEMENT', 'Student management', 'Records, guardians, enrolment, transfers', 'ACTIVE_STUDENTS'],
  ['ACADEMICS', 'Academic structure', 'Classes, sections, subjects, curriculum', null],
  ['ATTENDANCE', 'Attendance', 'Daily and period attendance, registers', null],
  ['TIMETABLE', 'Timetable', 'Period scheduling and substitutions', null],
  ['EXAMINATIONS', 'Examinations', 'Schedules, marks, grading, report cards', null],
  ['HOMEWORK', 'Homework', 'Assignments, submissions, marking', null],
  ['FEE_MANAGEMENT', 'Fee management', 'Fee structures, invoices, receipts, dues', null],
  ['PAYROLL', 'Payroll', 'Salary structures, payslips, deductions', 'ACTIVE_STAFF'],
  ['STAFF_MANAGEMENT', 'Staff management', 'Staff records, leave, reviews', 'ACTIVE_STAFF'],
  ['ADMISSIONS_CRM', 'Admissions', 'Enquiries, follow-ups, the pipeline', null],
  ['TRANSPORT', 'Transport', 'Routes, stops, vehicles, trips', 'VEHICLES'],
  ['LIBRARY', 'Library', 'Catalogue, issues, returns, fines', 'LIBRARY_TITLES'],
  ['HOSTEL', 'Hostel', 'Blocks, rooms, beds, attendance', 'HOSTEL_BEDS'],
  ['MESS', 'Mess', 'Menus, meal plans, mess attendance', null],
  ['HEALTH', 'Health', 'Health profiles, clinic visits, alerts', null],
  ['FRONT_OFFICE', 'Front office', 'Visitors, gate passes, the call log', null],
  ['INVENTORY', 'Inventory', 'Stock items, issues, returns', null],
  ['PROCUREMENT', 'Procurement', 'Vendors, orders, goods received', null],
  ['FACILITIES', 'Facilities', 'Rooms, bookings, inspections, maintenance', null],
  ['NOTIFICATIONS', 'Notifications', 'Announcements and alerts', 'SMS_MESSAGES'],
  ['DOCUMENTS', 'Documents', 'Storage, templates, certificates', 'STORAGE_MEGABYTES'],
  ['GALLERY', 'Gallery', 'Photo albums shared with guardians', 'STORAGE_MEGABYTES'],
  ['FEEDBACK', 'Feedback', 'Campaigns, and reports to the principal', null],
  ['STUDENT_LIFE', 'Student life', 'Houses, clubs, achievements, discipline', null],
];

const POLICIES = [
  ['BLOCK', 'Block them'],
  ['WARN', 'Let them through, warn'],
  ['ALLOW', 'Ignore it'],
  ['CHARGE', 'Bill the extra'],
];

const METRIC_LABEL = {
  ACTIVE_STUDENTS: 'students',
  ACTIVE_STAFF: 'staff',
  VEHICLES: 'vehicles',
  LIBRARY_TITLES: 'titles',
  HOSTEL_BEDS: 'beds',
  SMS_MESSAGES: 'messages',
  STORAGE_MEGABYTES: 'MB',
};

export default function PlanFeaturesTab({ plan, onChanged }) {
  const { call } = useApi();
  const editable = plan.status === 'DRAFT';

  // Keyed by feature code, so ticking one is a lookup rather than a search through a list.
  const [chosen, setChosen] = useState({});
  const [saving, setSaving] = useState(false);
  const [refused, setRefused] = useState(null);

  useEffect(() => {
    const next = {};
    (plan.features || []).forEach((one) => {
      next[one.featureCode] = {
        enabled: one.enabled !== false,
        usageLimit: one.usageLimit == null ? '' : String(one.usageLimit),
        overagePolicy: one.overagePolicy || 'BLOCK',
      };
    });
    setChosen(next);
    setRefused(null);
  }, [plan.planCode, plan.planVersion, plan.featureCount]);

  const toggle = (code) =>
    setChosen((was) => {
      const next = { ...was };
      if (next[code]) delete next[code];
      else next[code] = { enabled: true, usageLimit: '', overagePolicy: 'BLOCK' };
      return next;
    });

  const set = (code, patch) =>
    setChosen((was) => ({ ...was, [code]: { ...was[code], ...patch } }));

  const save = async () => {
    setSaving(true);
    setRefused(null);
    const features = Object.entries(chosen).map(([featureCode, one]) => {
      const row = { featureCode, enabled: one.enabled };
      // An empty box means no limit, not a limit of zero — and a limit of zero on an enabled
      // feature is refused by the backend anyway.
      if (one.usageLimit !== '') row.usageLimit = Number(one.usageLimit);
      if (one.overagePolicy && one.overagePolicy !== 'BLOCK') row.overagePolicy = one.overagePolicy;
      return row;
    });

    const result = await call('set-plan-features', {
      label: 'Set the features',
      pathParams: { code: plan.planCode, version: plan.planVersion },
      body: { features },
    });
    setSaving(false);
    if (result.ok) onChanged();
    else setRefused(result.bodyJson);
  };

  const count = Object.keys(chosen).length;

  return (
    <Card
      title="What this plan includes"
      description={
        editable
          ? 'Tick what is included. Saving replaces the whole list — a plan is priced as a set.'
          : 'A published plan cannot be changed. Make a new version to change what it includes.'
      }
      action={
        editable ? (
          <div className="flex flex-wrap items-center gap-2">
            <EndpointTag id="set-plan-features"
              pathParams={{ code: plan.planCode, version: plan.planVersion }} />
            <Badge look={count ? 'green' : 'amber'}>{count} chosen</Badge>
            <Button look="primary" size="sm" icon={Save} onClick={save} busy={saving}>
              Save the list
            </Button>
          </div>
        ) : (
          <Badge look="grey"><Lock size={11} /> {plan.featureCount} included</Badge>
        )
      }
    >
      {refused && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3">
          <p className="font-mono text-[11px] font-semibold text-red-900">{refused.code}</p>
          <p className="mt-1 text-xs text-red-800">{refused.message}</p>
        </div>
      )}

      {!editable && plan.featureCount === 0 ? (
        <EmptyState icon={Gauge} title="Nothing included"
          description="This plan has no features, which is why it could never be published." />
      ) : (
        <ul className="divide-y divide-slate-100">
          {FEATURES.map(([code, label, description, metric]) => {
            const picked = chosen[code];
            const on = Boolean(picked);
            if (!editable && !on) return null;
            return (
              <li key={code} className="py-2.5">
                <div className="flex items-start gap-3">
                  <input
                    type="checkbox"
                    checked={on}
                    disabled={!editable}
                    onChange={() => toggle(code)}
                    className="mt-0.5 h-4 w-4 shrink-0 rounded border-slate-300"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-slate-800">
                      {label}
                      {on && picked.enabled === false && (
                        <Badge look="grey" className="ml-2">listed as not included</Badge>
                      )}
                    </p>
                    <p className="text-[11px] text-slate-500">{description}</p>

                    {on && (
                      <div className="mt-2 flex flex-wrap items-end gap-3">
                        {metric ? (
                          <>
                            <label className="block">
                              <span className="mb-1 block text-[11px] text-slate-500">
                                Limit ({METRIC_LABEL[metric] || metric.toLowerCase()})
                              </span>
                              <TextInput
                                type="number"
                                min="1"
                                disabled={!editable}
                                value={picked.usageLimit}
                                onChange={(event) => set(code, { usageLimit: event.target.value })}
                                placeholder="no limit"
                                className="w-32 text-xs"
                              />
                            </label>
                            {picked.usageLimit !== '' && (
                              <label className="block">
                                <span className="mb-1 block text-[11px] text-slate-500">Past the limit</span>
                                <SelectInput
                                  disabled={!editable}
                                  value={picked.overagePolicy}
                                  onChange={(event) => set(code, { overagePolicy: event.target.value })}
                                  className="w-auto text-xs"
                                >
                                  {POLICIES.map(([value, text]) => (
                                    <option key={value} value={value}>{text}</option>
                                  ))}
                                </SelectInput>
                              </label>
                            )}
                          </>
                        ) : (
                          <p className="text-[11px] text-slate-400">
                            Nothing to count — it is either included or it is not.
                          </p>
                        )}

                        {editable && (
                          <label className="flex items-center gap-1.5 text-[11px] text-slate-600">
                            <input
                              type="checkbox"
                              checked={picked.enabled === false}
                              onChange={(event) => set(code, { enabled: !event.target.checked })}
                              className="h-3.5 w-3.5 rounded border-slate-300"
                            />
                            Show as excluded rather than included
                          </label>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}
