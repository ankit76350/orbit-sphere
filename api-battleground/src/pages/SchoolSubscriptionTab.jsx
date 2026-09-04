/**
 * The school's subscription — the screen that makes a school a paying customer.
 *
 * One endpoint lives here: `POST /platform/schools/{id}/subscriptions`. It is the piece `core`
 * has been complaining about — `activateSchool` was written to require a subscription, found
 * nothing could create one, and settled for a soft check that announces the gap in every
 * response. Create one here and that response stops apologising.
 *
 * THIS SCREEN DOES NOT READ A SUBSCRIPTION BACK YET. `GET /platform/schools/{id}/subscription`
 * (#27) exists now, but this tab does not call it — so it can still only show what it created,
 * and only until the page is reloaded. That is stated on the screen rather than worked around,
 * because a blank panel that means "nothing to show" and a blank panel that means "nothing asked"
 * are different problems, and guessing which one you are looking at is how somebody creates a
 * second subscription for a school that already has one. Wiring #27 up is what removes the note.
 *
 * THE PLAN IS PICKED, NOT TYPED. The list comes from `GET /platform/plans`, so the only plans
 * offered are ones that exist, and a plan that cannot be sold today is shown with the reason it
 * cannot rather than left out — "PREMIUM v2 — not published yet" answers the question that an
 * absent row only raises. `planCode` and `planVersion` both come off the chosen row: the API
 * names a plan by code and version the same way every plan URL does.
 */

import { useCallback, useEffect, useState } from 'react';
import {
  CreditCard, RefreshCw, AlertTriangle, CheckCircle2, Info, SlidersHorizontal,
} from 'lucide-react';
import { useApi } from '../api/apiContext.js';
import EndpointTag from '../components/EndpointTag.jsx';
import {
  Button, Card, Badge, Detail, Field, TextInput, SelectInput, Toggle, Loading, EmptyState,
} from '../components/ui.jsx';
import { sellableReason, money } from './PlansPage.jsx';

/** Only the plans a school could actually be put on today, newest version of each first. */
const PLAN_QUERY = { status: ['ACTIVE'], size: 100 };

/**
 * A `datetime-local` value as the instant the API wants.
 *
 * The input gives back "2026-04-01T09:30" with no zone, which the browser means in local time.
 * `new Date(...)` reads it that way too, so `toISOString()` converts rather than relabels — the
 * school's 9:30 stays 9:30 wherever the person filling this in happens to be sitting.
 */
function asInstant(local) {
  if (!local) return undefined;
  const at = new Date(local);
  return Number.isNaN(at.getTime()) ? undefined : at.toISOString();
}

/** A number the API will accept, or nothing at all. Empty must not become 0. */
function asNumber(raw) {
  const text = String(raw ?? '').trim();
  if (!text) return undefined;
  const value = Number(text);
  return Number.isFinite(value) ? value : undefined;
}

export default function SchoolSubscriptionTab({ school }) {
  const { call } = useApi();

  const [plans, setPlans] = useState(null);
  const [loadingPlans, setLoadingPlans] = useState(true);
  const [planProblem, setPlanProblem] = useState(null);

  const [picked, setPicked] = useState('');
  const [trial, setTrial] = useState(false);
  const [autoRenew, setAutoRenew] = useState(true);
  const [showDeal, setShowDeal] = useState(false);
  const [deal, setDeal] = useState({
    currentPeriodStart: '',
    currentPeriodEnd: '',
    contractedPrice: '',
    maxStudentsOverride: '',
    maxUsersOverride: '',
    billingCustomerReference: '',
    reason: '',
  });

  const [saving, setSaving] = useState(false);
  const [created, setCreated] = useState(null);
  const [refused, setRefused] = useState(null);
  const [errors, setErrors] = useState({});

  const loadPlans = useCallback(async () => {
    setLoadingPlans(true);
    const result = await call('list-plans', { label: 'Load the sellable plans', query: PLAN_QUERY });
    setLoadingPlans(false);
    if (result.ok) {
      setPlans(result.bodyJson?.content ?? []);
      setPlanProblem(null);
    } else {
      setPlanProblem(result);
    }
  }, [call]);

  useEffect(() => {
    loadPlans();
  }, [loadPlans]);

  const chosen = (plans ?? []).find(
    (one) => `${one.planCode}@${one.planVersion}` === picked,
  );

  // A CUSTOM cycle has no length of its own, so the API cannot work out where the first period
  // ends and refuses with BILLING_PERIOD_END_REQUIRED. Better to ask for it up front than to let
  // somebody find out by being refused.
  const needsEnd = chosen?.billingCycle === 'CUSTOM';

  const submit = async () => {
    setErrors({});
    setRefused(null);

    if (!chosen) {
      setErrors({ plan: 'Choose a plan first.' });
      return;
    }
    if (needsEnd && !deal.currentPeriodEnd) {
      setShowDeal(true);
      setErrors({ currentPeriodEnd: 'A custom billing cycle has to say when the period ends.' });
      return;
    }

    // Only what was actually filled in. An empty box means "use the default", and sending it as
    // null or 0 would overrule a default the API is better placed to pick than this form is.
    // `undefined` is what makes that work: JSON.stringify drops those keys, where null would be
    // sent and read as an explicit "no value".
    const body = {
      planCode: chosen.planCode,
      planVersion: chosen.planVersion,
      trial: trial || undefined,
      autoRenew: autoRenew ? undefined : false,
      currentPeriodStart: asInstant(deal.currentPeriodStart),
      currentPeriodEnd: asInstant(deal.currentPeriodEnd),
      contractedPrice: asNumber(deal.contractedPrice),
      maxStudentsOverride: asNumber(deal.maxStudentsOverride),
      maxUsersOverride: asNumber(deal.maxUsersOverride),
      billingCustomerReference: deal.billingCustomerReference.trim() || undefined,
      reason: deal.reason.trim() || undefined,
    };

    setSaving(true);
    const result = await call('create-subscription', {
      label: 'Create the subscription',
      pathParams: { id: school.schoolId },
      body,
    });
    setSaving(false);

    if (result.ok) {
      setCreated(result.bodyJson);
      return;
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(
        Object.fromEntries(
          Object.entries(result.bodyJson.fieldErrors).map(([field, messages]) => [
            field, [].concat(messages)[0],
          ]),
        ),
      );
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) {
      setRefused(result.bodyJson);
    }
  };

  /* ------------------------------------------------------------------ what was created */

  if (created) {
    return (
      <div className="space-y-5">
        <Card
          title="Subscription created"
          description="This school is now a paying customer."
          action={
            <Button onClick={() => { setCreated(null); setPicked(''); }}>
              Create another
            </Button>
          }
        >
          <dl className="grid gap-x-4 gap-y-4 sm:grid-cols-3">
            <Detail label="Number">
              <span className="font-mono text-xs">{created.subscriptionNo}</span>
            </Detail>
            <Detail label="Status">
              <Badge look={created.status === 'TRIAL' ? 'amber' : 'green'}>{created.status}</Badge>
            </Detail>
            <Detail label="Plan">
              {created.planName} — {created.planCode} v{created.planVersion}
            </Detail>
            <Detail label="Billing cycle">{created.billingCycle}</Detail>
            <Detail label="Price">
              {money(created.contractedPrice, created.currencyCode)}
              {created.contractedPrice !== created.planListPrice && (
                <span className="ml-1.5 text-xs text-slate-500">
                  list {money(created.planListPrice, created.currencyCode)}
                </span>
              )}
            </Detail>
            <Detail label="Renews automatically">{created.autoRenew ? 'Yes' : 'No'}</Detail>
            <Detail label="Period start">
              {created.currentPeriodStart ? new Date(created.currentPeriodStart).toLocaleString() : null}
            </Detail>
            <Detail label="Period end">
              {created.currentPeriodEnd ? new Date(created.currentPeriodEnd).toLocaleString() : null}
            </Detail>
            <Detail label="Limits">
              {created.maxStudents} students · {created.maxUsers} users
              {created.hasLimitOverrides && (
                <Badge look="violet" className="ml-1.5">negotiated</Badge>
              )}
            </Detail>
          </dl>

          {created.nextStep && (
            <p className="mt-4 flex items-start gap-2 rounded-lg bg-blue-50 px-3 py-2.5 text-xs text-blue-900">
              <Info size={14} className="mt-px shrink-0 text-blue-600" />
              {created.nextStep}
            </p>
          )}
        </Card>

        <p className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs text-amber-900">
          <AlertTriangle size={14} className="mt-px shrink-0 text-amber-600" />
          Shown from the response to the call just made. This tab does not read the subscription
          back yet, so leaving it loses it from the screen. The subscription itself is saved, and
          <code className="font-mono">GET /platform/schools/{'{id}'}/subscription</code> can fetch
          it once this screen is wired to call it.
        </p>
      </div>
    );
  }

  /* ------------------------------------------------------------------------- the form */

  return (
    <div className="space-y-5">
      <Card
        title="Give this school a subscription"
        description="Pick the plan it is buying. Everything else has a sensible default."
        action={
          <div className="flex flex-col items-end gap-1">
            <Button icon={RefreshCw} onClick={loadPlans} busy={loadingPlans}>
              Reload plans
            </Button>
            <EndpointTag id="list-plans" query={PLAN_QUERY} showPath={false} />
          </div>
        }
      >
        {loadingPlans && !plans ? (
          <Loading label="Loading the plans…" />
        ) : planProblem ? (
          <EmptyState
            icon={AlertTriangle}
            title="The plans could not be loaded"
            description={planProblem.bodyJson?.message || `The catalogue answered ${planProblem.status}.`}
            action={<Button icon={RefreshCw} onClick={loadPlans}>Try again</Button>}
          />
        ) : (plans ?? []).length === 0 ? (
          <EmptyState
            icon={CreditCard}
            title="No published plans"
            description="A school can only be put on a plan that has been published. Publish one in Plans first."
          />
        ) : (
          <div className="space-y-4">
            <Field
              label="Plan"
              required
              error={errors.plan || errors.planCode || errors.planVersion}
              hint={chosen ? `Billed ${chosen.billingCycle.toLowerCase()} at ${money(chosen.listPrice, chosen.currencyCode)}. ${chosen.maxStudents} students, ${chosen.maxUsers} users.` : 'Only published plans appear here.'}
            >
              <SelectInput
                value={picked}
                error={Boolean(errors.plan)}
                onChange={(event) => setPicked(event.target.value)}
              >
                <option value="">Choose a plan…</option>
                {(plans ?? []).map((one) => {
                  const why = sellableReason(one);
                  return (
                    <option
                      key={`${one.planCode}@${one.planVersion}`}
                      value={`${one.planCode}@${one.planVersion}`}
                    >
                      {one.name} — {one.planCode} v{one.planVersion}
                      {why ? ` (${why})` : ''}
                    </option>
                  );
                })}
              </SelectInput>
            </Field>

            {/* A plan the catalogue says cannot be sold is still offered, because the refusal is
                the API's to make and seeing it is the point of this app — but it is flagged, so
                nobody is surprised by the 409. */}
            {chosen && sellableReason(chosen) && (
              <p className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs text-amber-900">
                <AlertTriangle size={14} className="mt-px shrink-0 text-amber-600" />
                <span>
                  <strong>{chosen.planCode} v{chosen.planVersion}</strong> cannot be sold today —{' '}
                  {sellableReason(chosen).toLowerCase()}. Sending it anyway is a{' '}
                  <code className="font-mono">409 PLAN_NOT_SELLABLE</code>.
                </span>
              </p>
            )}

            <div className="divide-y divide-slate-100 border-y border-slate-100">
              <Toggle
                checked={trial}
                onChange={setTrial}
                label="Start it as a trial"
                description="The subscription opens TRIAL instead of ACTIVE. Everything else is the same."
              />
              <Toggle
                checked={autoRenew}
                onChange={setAutoRenew}
                label="Renew automatically"
                description="Off means the subscription simply ends when the period does."
              />
            </div>

            <button
              type="button"
              onClick={() => setShowDeal((was) => !was)}
              className="flex items-center gap-1.5 text-xs font-medium text-slate-600 transition hover:text-slate-900"
            >
              <SlidersHorizontal size={13} className="text-slate-400" />
              {showDeal ? 'Hide' : 'Set'} the negotiated terms
              <span className="text-slate-400">— price, period and limit overrides</span>
            </button>

            {showDeal && (
              <div className="grid gap-4 rounded-lg bg-slate-50 p-4 sm:grid-cols-2">
                <Field
                  label="Period starts"
                  hint="Blank means now."
                  error={errors.currentPeriodStart}
                >
                  <TextInput
                    type="datetime-local"
                    value={deal.currentPeriodStart}
                    onChange={(event) => setDeal({ ...deal, currentPeriodStart: event.target.value })}
                  />
                </Field>

                <Field
                  label="Period ends"
                  required={needsEnd}
                  hint={needsEnd
                    ? 'Required: a CUSTOM cycle has no length of its own.'
                    : 'Blank means one billing cycle after the start.'}
                  error={errors.currentPeriodEnd}
                >
                  <TextInput
                    type="datetime-local"
                    value={deal.currentPeriodEnd}
                    error={Boolean(errors.currentPeriodEnd)}
                    onChange={(event) => setDeal({ ...deal, currentPeriodEnd: event.target.value })}
                  />
                </Field>

                <Field
                  label="Agreed price"
                  hint={chosen
                    ? `Blank charges the list price, ${money(chosen.listPrice, chosen.currencyCode)}.`
                    : 'Blank charges the plan’s list price.'}
                  error={errors.contractedPrice}
                >
                  <TextInput
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder={chosen ? String(chosen.listPrice) : ''}
                    value={deal.contractedPrice}
                    error={Boolean(errors.contractedPrice)}
                    onChange={(event) => setDeal({ ...deal, contractedPrice: event.target.value })}
                  />
                </Field>

                <Field
                  label="Billing customer reference"
                  hint="The payment gateway’s own id, if there is one."
                  error={errors.billingCustomerReference}
                >
                  <TextInput
                    placeholder="customer_Qx7B2mR9"
                    value={deal.billingCustomerReference}
                    onChange={(event) => setDeal({ ...deal, billingCustomerReference: event.target.value })}
                  />
                </Field>

                <Field
                  label="Student limit"
                  hint={chosen ? `Blank uses the plan’s ${chosen.maxStudents}.` : 'Blank uses the plan’s.'}
                  error={errors.maxStudentsOverride}
                >
                  <TextInput
                    type="number"
                    min="1"
                    placeholder={chosen ? String(chosen.maxStudents) : ''}
                    value={deal.maxStudentsOverride}
                    error={Boolean(errors.maxStudentsOverride)}
                    onChange={(event) => setDeal({ ...deal, maxStudentsOverride: event.target.value })}
                  />
                </Field>

                <Field
                  label="User limit"
                  hint={chosen ? `Blank uses the plan’s ${chosen.maxUsers}.` : 'Blank uses the plan’s.'}
                  error={errors.maxUsersOverride}
                >
                  <TextInput
                    type="number"
                    min="1"
                    placeholder={chosen ? String(chosen.maxUsers) : ''}
                    value={deal.maxUsersOverride}
                    error={Boolean(errors.maxUsersOverride)}
                    onChange={(event) => setDeal({ ...deal, maxUsersOverride: event.target.value })}
                  />
                </Field>

                <Field
                  label="Why"
                  className="sm:col-span-2"
                  hint="Goes on the first subscription_history row, so the deal has a note against it."
                  error={errors.reason}
                >
                  <TextInput
                    placeholder="Negotiated at 10% off for the first year."
                    value={deal.reason}
                    onChange={(event) => setDeal({ ...deal, reason: event.target.value })}
                  />
                </Field>
              </div>
            )}

            {/* A refusal that is not about one field has nowhere to sit next to an input. */}
            {refused && (
              <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2.5">
                <AlertTriangle size={14} className="mt-px shrink-0 text-red-600" />
                <div className="min-w-0 text-xs text-red-900">
                  <p className="font-semibold">{refused.code}</p>
                  <p className="mt-0.5">{refused.message}</p>
                  {refused.code === 'SUBSCRIPTION_ALREADY_EXISTS' && (
                    <p className="mt-1.5 text-red-800/80">
                      A school gets one current subscription, enforced by a unique partial index.
                      Changing it is #18–20, none of which are built.
                    </p>
                  )}
                </div>
              </div>
            )}

            <div className="flex flex-col items-start gap-1.5 pt-1">
              <Button
                look="primary"
                icon={CheckCircle2}
                onClick={submit}
                busy={saving}
                disabled={!picked}
              >
                Create the subscription
              </Button>
              <EndpointTag id="create-subscription" pathParams={{ id: school.schoolId }} />
            </div>
          </div>
        )}
      </Card>

      <p className="flex items-start gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-xs text-slate-500 shadow-sm">
        <Info size={14} className="mt-px shrink-0 text-slate-400" />
        <span>
          Creating is all this tab does today. The read exists —{' '}
          <code className="font-mono">GET /platform/schools/{'{id}'}/subscription</code> — but this
          screen does not call it yet, so there is nothing shown for a school that already has one.
          Until it is wired up, a second attempt is how you find out:{' '}
          <code className="font-mono">409 SUBSCRIPTION_ALREADY_EXISTS</code>.
        </span>
      </p>
    </div>
  );
}
