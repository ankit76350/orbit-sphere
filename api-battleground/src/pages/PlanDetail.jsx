/**
 * One plan version: what it costs, what it includes, and what can still be done to it.
 *
 * The buttons that show depend on where the plan is in its life, the same way a school's do.
 * A draft can be edited, given features and published; a published plan can be listed publicly
 * or retired; a retired one can only be read.
 *
 * THE LIFECYCLE IS THE POINT OF THIS SCREEN. Publish and retire are one-way — nothing undoes
 * either, and there is no endpoint that returns a plan to draft — so both ask first and say what
 * will not be possible afterwards.
 */

import { useCallback, useEffect, useState } from 'react';
import {
  ArrowLeft, RefreshCw, Rocket, Archive, Globe, EyeOff, Info, Gauge, History, Pencil,
  ShoppingCart, Lock, AlertTriangle, Users,
} from 'lucide-react';
import { useApi } from '../api/apiContext.js';
import {
  Button, Card, Badge, Detail, Modal, Field, TextInput, SelectInput, Loading, EmptyState,
} from '../components/ui.jsx';
import PlanFeaturesTab from './PlanFeaturesTab.jsx';
import { PlanStatusBadge, sellableReason, money } from './PlansPage.jsx';

export const PLAN_TABS = [
  { id: 'overview', label: 'Overview', icon: Info },
  { id: 'features', label: 'Features', icon: Gauge },
  { id: 'versions', label: 'Versions', icon: History },
];

/** What can be done to a plan, given where it is. */
function actionsFor(status) {
  switch (status) {
    case 'DRAFT': return ['edit', 'publish', 'retire'];
    case 'ACTIVE': return ['availability', 'retire'];
    default: return [];
  }
}

const CYCLES = ['YEARLY', 'HALF_YEARLY', 'QUARTERLY', 'MONTHLY', 'CUSTOM'];

export default function PlanDetail({ plan: initial, tab: controlledTab, onTabChange, onBack }) {
  const { call } = useApi();

  const [ownTab, setOwnTab] = useState('overview');
  const tab = controlledTab ?? ownTab;
  const setTab = (next) => {
    setOwnTab(next);
    onTabChange?.(next);
  };

  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(null);
  const [asking, setAsking] = useState(null);
  const [refused, setRefused] = useState(null);
  const [edit, setEdit] = useState({});

  const code = initial.planCode;
  const version = initial.planVersion;

  const load = useCallback(async () => {
    setLoading(true);
    const result = await call('get-plan-version', {
      label: 'Load the plan',
      pathParams: { code, version },
    });
    setLoading(false);
    if (result.ok) setPlan(result.bodyJson);
  }, [call, code, version]);

  useEffect(() => { load(); }, [load]);

  const [versions, setVersions] = useState(null);
  const loadVersions = useCallback(async () => {
    const result = await call('list-plan-versions', {
      label: 'Load the version history',
      pathParams: { code },
    });
    if (result.ok) setVersions(result.bodyJson);
  }, [call, code]);

  useEffect(() => {
    if (tab === 'versions' && !versions) loadVersions();
  }, [tab, versions, loadVersions]);

  /** One lifecycle action, then reload — the response is the plan, but so is a refusal's cause. */
  const run = async (key, label, endpointId, body) => {
    setBusy(key);
    setRefused(null);
    const result = await call(endpointId, {
      label,
      pathParams: { code, version },
      ...(body === undefined ? {} : { body }),
    });
    setBusy(null);
    setAsking(null);
    if (result.ok) {
      setPlan(result.bodyJson.features ? { ...plan, ...result.bodyJson } : { ...plan, ...result.bodyJson });
      await load();
      setVersions(null);
    } else {
      setRefused(result.bodyJson);
    }
  };

  if (loading && !plan) return <Loading label={`Loading ${code} v${version}…`} />;
  if (!plan) {
    return (
      <EmptyState icon={AlertTriangle} title="That plan could not be loaded"
        description={`Nothing came back for ${code} version ${version}.`}
        action={<Button onClick={onBack}>Back to the catalogue</Button>} />
    );
  }

  const actions = actionsFor(plan.status);
  const reason = sellableReason(plan);

  return (
    <div className="space-y-5">
      <button type="button" onClick={onBack}
        className="inline-flex items-center gap-1.5 text-xs font-medium text-slate-500 transition hover:text-slate-800">
        <ArrowLeft size={14} /> All plans
      </button>

      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2.5">
              <h1 className="text-xl font-semibold text-slate-900">{plan.name}</h1>
              <PlanStatusBadge status={plan.status} />
              {plan.sellable ? (
                <Badge look="green"><ShoppingCart size={11} /> On sale</Badge>
              ) : (
                <Badge look="grey" title="Published, on the public list, and inside its selling window">
                  <Lock size={11} /> {reason}
                </Badge>
              )}
            </div>
            <p className="mt-1 font-mono text-sm text-slate-500">
              {plan.planCode} · version {plan.planVersion}
            </p>
            {plan.description && <p className="mt-2 text-sm text-slate-600">{plan.description}</p>}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {actions.includes('edit') && (
              <Button icon={Pencil} onClick={() => {
                setEdit({
                  name: plan.name,
                  listPrice: String(plan.listPrice ?? ''),
                  billingCycle: plan.billingCycle,
                  maxStudents: String(plan.maxStudents ?? ''),
                  maxUsers: String(plan.maxUsers ?? ''),
                });
                setAsking('edit');
              }}>
                Edit
              </Button>
            )}
            {actions.includes('publish') && (
              <Button look="primary" icon={Rocket} onClick={() => setAsking('publish')}>
                Publish
              </Button>
            )}
            {actions.includes('availability') && (
              plan.publiclyAvailable ? (
                <Button icon={EyeOff} busy={busy === 'availability'}
                  onClick={() => run('availability', 'Take it off the public list',
                    'set-plan-availability', { publiclyAvailable: false })}>
                  Unlist
                </Button>
              ) : (
                <Button look="primary" icon={Globe} busy={busy === 'availability'}
                  onClick={() => run('availability', 'Put it on the public list',
                    'set-plan-availability', { publiclyAvailable: true })}>
                  List publicly
                </Button>
              )
            )}
            {actions.includes('retire') && (
              <Button look="danger" icon={Archive} onClick={() => setAsking('retire')}>
                Retire
              </Button>
            )}
            <Button icon={RefreshCw} busy={loading} onClick={load} title="Read it again" />
          </div>
        </div>

        {refused && (
          <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3">
            <p className="font-mono text-[11px] font-semibold text-red-900">{refused.code}</p>
            <p className="mt-1 text-xs text-red-800">{refused.message}</p>
          </div>
        )}

        {plan.status === 'DRAFT' && (
          <p className="mt-4 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2.5 text-xs text-blue-900">
            This is a draft: nobody can buy it, and everything about it can still be changed. Set
            its <strong>features</strong>, then <strong>publish</strong> it — after which it can
            never be edited again.
          </p>
        )}
        {plan.status === 'ACTIVE' && !plan.publiclyAvailable && (
          <p className="mt-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs text-amber-900">
            Published but <strong>not on the public list</strong>, so a school cannot find it —
            which is exactly a private quote. <strong>List publicly</strong> makes it buyable.
          </p>
        )}
      </div>

      <div className="flex gap-1 border-b border-slate-200">
        {PLAN_TABS.map((one) => (
          <button key={one.id} type="button" onClick={() => setTab(one.id)}
            className={`-mb-px inline-flex items-center gap-1.5 border-b-2 px-3.5 py-2.5 text-sm font-medium transition ${
              tab === one.id
                ? 'border-blue-600 text-blue-700'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}>
            <one.icon size={15} />
            {one.label}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <div className="grid gap-5 lg:grid-cols-2">
          <Card title="Commercial terms">
            <div className="grid gap-4 sm:grid-cols-2">
              <Detail label="Price">
                <span className="text-lg font-semibold text-slate-900">
                  {money(plan.listPrice, plan.currencyCode)}
                </span>
                <span className="ml-1 text-xs text-slate-500">
                  / {(plan.billingCycle || '').toLowerCase().replace('_', ' ')}
                </span>
              </Detail>
              <Detail label="Currency">{plan.currencyCode}</Detail>
              <Detail label="Students included">{plan.maxStudents}</Detail>
              <Detail label="Staff logins included">{plan.maxUsers}</Detail>
              <Detail label="On the public list">{plan.publiclyAvailable ? 'Yes' : 'No'}</Detail>
              <Detail label="Features">{plan.featureCount}</Detail>
            </div>
          </Card>

          <Card title="Selling window"
            description="When this version may be sold. Publishing fills the start if it is empty.">
            <div className="grid gap-4 sm:grid-cols-2">
              <Detail label="On sale from">
                {plan.effectiveFrom ? new Date(plan.effectiveFrom).toLocaleString() : 'Not set'}
              </Detail>
              <Detail label="Until">
                {plan.effectiveUntil ? new Date(plan.effectiveUntil).toLocaleString() : 'No end'}
              </Detail>
            </div>
            <div className="mt-4 border-t border-slate-100 pt-4">
              <Detail label="Schools on this version">
                <span className="inline-flex items-center gap-1.5">
                  <Users size={14} className="text-slate-400" />
                  {plan.schoolsOnThisVersion}
                </span>
              </Detail>
              {plan.note && <p className="mt-1.5 text-[11px] text-slate-500">{plan.note}</p>}
            </div>
          </Card>
        </div>
      )}

      {tab === 'features' && <PlanFeaturesTab plan={plan} onChanged={load} />}

      {tab === 'versions' && (
        <Card title="Every version of this plan"
          description="Newest first, with what changed in the price and who is on each.">
          {!versions ? (
            <Loading label="Loading the history…" />
          ) : (
            <>
              <ul className="divide-y divide-slate-100">
                {versions.versions.map((one) => (
                  <li key={one.planVersion} className="flex flex-wrap items-center gap-3 py-2.5">
                    <Badge look={one.planVersion === plan.planVersion ? 'blue' : 'grey'}
                      className="w-14 justify-center">
                      v{one.planVersion}
                    </Badge>
                    <PlanStatusBadge status={one.status} />
                    <span className="text-sm text-slate-800">{money(one.listPrice, one.currencyCode)}</span>
                    {one.priceChangeFromPrevious != null && (
                      <Badge look={one.priceChangeFromPrevious > 0 ? 'red' : 'green'}>
                        {one.priceChangeFromPrevious > 0 ? '+' : ''}
                        {money(one.priceChangeFromPrevious, one.currencyCode)}
                      </Badge>
                    )}
                    <span className="ml-auto text-[11px] text-slate-500">
                      {one.schoolsOnThisVersion} school{one.schoolsOnThisVersion === 1 ? '' : 's'}
                    </span>
                  </li>
                ))}
              </ul>
              {versions.note && (
                <p className="mt-3 rounded-lg bg-slate-50 px-3 py-2 text-[11px] text-slate-600">
                  {versions.note}
                </p>
              )}
              <p className="mt-3 text-[11px] text-slate-500">
                There is no way to add a version yet — the endpoint that copies a published plan
                into a new draft is not built.
              </p>
            </>
          )}
        </Card>
      )}

      {/* ------------------------------------------------------------ publish */}
      <Modal
        open={asking === 'publish'}
        onClose={() => setAsking(null)}
        title="Publish this plan"
        description="From here it can never be edited again."
        footer={
          <>
            <Button onClick={() => setAsking(null)}>Cancel</Button>
            <Button look="primary" busy={busy === 'publish'}
              onClick={() => run('publish', 'Publish the plan', 'publish-plan')}>
              Publish it
            </Button>
          </>
        }
      >
        <ul className="space-y-2 text-xs text-slate-600">
          <li>• The price, the limits and the features are <strong>frozen</strong>. To change any
            of them afterwards you make a new version, which is not built yet.</li>
          <li>• It will <strong>not</strong> be on the public list — that is a separate step, so a
            school still cannot find it until you list it.</li>
          <li>• A plan with no features cannot be published. This one has{' '}
            <strong>{plan.featureCount}</strong>.</li>
        </ul>
      </Modal>

      {/* ------------------------------------------------------------- retire */}
      <Modal
        open={asking === 'retire'}
        onClose={() => setAsking(null)}
        title="Retire this plan"
        description="It stops being something a school can pick."
        footer={
          <>
            <Button onClick={() => setAsking(null)}>Cancel</Button>
            <Button look="solidDanger" busy={busy === 'retire'}
              onClick={() => run('retire', 'Retire the plan', 'retire-plan')}>
              Retire it
            </Button>
          </>
        }
      >
        <ul className="space-y-2 text-xs text-slate-600">
          <li>• Schools <strong>already on it keep it</strong> — same price, same features.
            Nothing about their subscription changes.</li>
          <li>• There is <strong>no un-retire</strong>. It cannot be edited or published
            afterwards.</li>
          <li>• Its plan code stays taken: retiring is not deleting.</li>
        </ul>
      </Modal>

      {/* --------------------------------------------------------------- edit */}
      <Modal
        open={asking === 'edit'}
        onClose={() => setAsking(null)}
        title="Edit this draft"
        description="Only a draft can be changed. Leave a field alone to keep it as it is."
        footer={
          <>
            <Button onClick={() => setAsking(null)}>Cancel</Button>
            <Button look="primary" busy={busy === 'edit'}
              onClick={() => run('edit', 'Edit the draft', 'update-plan-draft', {
                name: edit.name,
                listPrice: edit.listPrice === '' ? undefined : Number(edit.listPrice),
                billingCycle: edit.billingCycle,
                maxStudents: edit.maxStudents === '' ? undefined : Number(edit.maxStudents),
                maxUsers: edit.maxUsers === '' ? undefined : Number(edit.maxUsers),
              })}>
              Save
            </Button>
          </>
        }
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Name" className="sm:col-span-2">
            <TextInput value={edit.name ?? ''}
              onChange={(event) => setEdit((was) => ({ ...was, name: event.target.value }))} />
          </Field>
          <Field label="Price">
            <TextInput type="number" min="0" step="0.01" value={edit.listPrice ?? ''}
              onChange={(event) => setEdit((was) => ({ ...was, listPrice: event.target.value }))} />
          </Field>
          <Field label="Billing cycle">
            <SelectInput value={edit.billingCycle ?? 'YEARLY'}
              onChange={(event) => setEdit((was) => ({ ...was, billingCycle: event.target.value }))}>
              {CYCLES.map((one) => <option key={one} value={one}>{one}</option>)}
            </SelectInput>
          </Field>
          <Field label="Students included">
            <TextInput type="number" min="1" value={edit.maxStudents ?? ''}
              onChange={(event) => setEdit((was) => ({ ...was, maxStudents: event.target.value }))} />
          </Field>
          <Field label="Staff logins included">
            <TextInput type="number" min="1" value={edit.maxUsers ?? ''}
              onChange={(event) => setEdit((was) => ({ ...was, maxUsers: event.target.value }))} />
          </Field>
        </div>
      </Modal>
    </div>
  );
}
