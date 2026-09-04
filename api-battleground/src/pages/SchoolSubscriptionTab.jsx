/**
 * The school's subscription — what it is on, and how it gets one.
 *
 * FOUR ENDPOINTS, AND THE SCREEN IS ARRANGED AROUND WHO IS ASKING:
 *
 *   - `GET  /platform/schools/{id}/subscription`   what WE see. Decides which half shows.
 *   - `POST /platform/schools/{id}/subscriptions`  create one, when the school has none.
 *   - `GET  /schools/current/subscription`         what the SCHOOL sees. Deliberately less.
 *   - `GET  /schools/current/subscription/entitlements`  what the school may actually use.
 *
 * The first read decides which half of the screen you see, so opening a school that already pays
 * shows the subscription rather than a form that would only be refused.
 *
 * THE TWO SCHOOL-SURFACE READS ARE HERE TO BE COMPARED WITH THE PLATFORM ONE. The point of the
 * "What the school sees" card is what is missing from it: no list price, no gateway customer
 * reference, no negotiated overrides. Those absences are CHECKED against the live response
 * rather than asserted in a comment — see WITHHELD. A privacy rule nobody verifies is a privacy
 * rule until the day it is not.
 *
 * THE FEATURE LIST COMES FROM THE ENTITLEMENTS READ, NOT FROM THE PLAN. The platform read
 * carries the plan's features too, and showing both would put two feature lists on one screen
 * that could disagree — which is the exact thing the entitlements endpoint exists to prevent.
 * So there is one list, and it reads `allowed`: the plan saying yes AND the subscription
 * granting. When those two differ the row says so, because that difference is the whole reason
 * no module may read the plan directly.
 *
 * They go out with `subdomain`, which is what puts the tenant header on the request. The school
 * surface never names a school in the URL, so that header is the only thing saying who is asking.
 *
 * WHAT IS ON SCREEN COMES FROM THE READ, NOT FROM THE CREATE. After creating one the tab
 * re-reads rather than rendering the 201 body, so there is one source of truth for what a school
 * is on — and leaving the tab and coming back shows the same thing, which it could not do before.
 *
 * A 404 IS NOT AN ERROR HERE, IT IS THE OTHER STATE. `SUBSCRIPTION_NOT_FOUND` means "no
 * subscription yet", which is exactly when the create form is the right thing to show.
 * `SCHOOL_NOT_FOUND` is a real error and says so. The API returns two codes precisely so a
 * caller can tell them apart, and this is the caller that needs to.
 *
 * THE PLAN IS PICKED, NOT TYPED. The list comes from `GET /platform/plans`, so the only plans
 * offered are ones that exist, and a plan that cannot be sold today is shown with the reason it
 * cannot rather than left out — "PREMIUM v2 — not published yet" answers the question that an
 * absent row only raises. `planCode` and `planVersion` both come off the chosen row: the API
 * names a plan by code and version the same way every plan URL does.
 */

import { useCallback, useEffect, useState } from 'react';
import {
  CreditCard, RefreshCw, AlertTriangle, CheckCircle2, XCircle, Info, SlidersHorizontal,
} from 'lucide-react';
import { useApi } from '../api/apiContext.js';
import EndpointTag from '../components/EndpointTag.jsx';
import {
  Button, Card, Badge, Detail, Field, TextInput, SelectInput, Toggle, Loading, EmptyState,
} from '../components/ui.jsx';
import { sellableReason, money } from './PlansPage.jsx';
import { METRIC_LABEL, POLICY_LABEL } from './PlanFeaturesTab.jsx';

/**
 * The fields the platform read (#27) carries and the school's read (#33) is meant not to.
 *
 * Listed here so the screen can CHECK rather than claim: each one is looked for in the actual
 * response, and a field that starts coming through is reported as a problem instead of being
 * quietly reassuring. A privacy rule nobody verifies is a privacy rule until the day it is not.
 */
const WITHHELD = [
  ['planListPrice', 'a school on a negotiated price would see a number it is not paying'],
  ['billingCustomerReference', "the payment gateway's id for them — ours to hold"],
  ['maxStudentsOverride', 'that a limit was negotiated is a commercial conversation'],
  ['maxUsersOverride', 'the same'],
  ['planCode', 'the internal family key. A school reads the name'],
];

/** One colour per status, so a lapsed or cancelled subscription does not look healthy. */
const STATUS_LOOK = {
  ACTIVE: 'green',
  TRIAL: 'amber',
  PAST_DUE: 'red',
  SUSPENDED: 'red',
  CANCELLED: 'grey',
  EXPIRED: 'grey',
};

/** An instant as something a person reads, or nothing when there is none. */
function when(instant) {
  return instant ? new Date(instant).toLocaleString() : null;
}

/**
 * How long is left, said in words.
 *
 * `daysRemaining` comes from the API and goes negative once the period has passed, so the sign
 * is the whole answer: "12 days" or "ended 3 days ago". Working this out in the browser instead
 * is how the count ends up a day out across a time zone.
 */
function daysLeft(subscription) {
  const days = subscription.daysRemaining;
  if (days == null) return null;
  if (days > 1) return `${days} days`;
  if (days === 1) return '1 day';
  if (days === 0) return 'Ends today';
  return `Ended ${Math.abs(days)} day${Math.abs(days) === 1 ? '' : 's'} ago`;
}

/** What a limit counts. Falls back to the raw constant rather than showing nothing. */
function metricWords(metric) {
  return METRIC_LABEL[metric] || metric.toLowerCase().replace(/_/g, ' ');
}

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

  // What the school is on. `null` while we have not asked, and stays null when the answer is
  // "none" — which is the create form's cue, not an error.
  const [subscription, setSubscription] = useState(null);
  const [reading, setReading] = useState(true);
  const [readProblem, setReadProblem] = useState(null);

  // The same subscription as the school itself sees it (#33), and what it is allowed to use
  // (#34). Both are school-surface reads, so they go out with the tenant header rather than the
  // school's id in the path.
  const [schoolView, setSchoolView] = useState(null);
  const [entitlements, setEntitlements] = useState(null);

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
  const [refused, setRefused] = useState(null);
  const [errors, setErrors] = useState({});

  /**
   * Reads what the school is on.
   *
   * A `404 SUBSCRIPTION_NOT_FOUND` is not treated as a failure: it is the answer "none yet", and
   * the create form is what should follow. Anything else — a missing school, a dead backend — is
   * a real problem and gets said out loud.
   */
  const loadSubscription = useCallback(async () => {
    setReading(true);
    const result = await call('get-subscription', {
      label: 'Read the subscription',
      pathParams: { id: school.schoolId },
    });
    setReading(false);

    if (result.ok) {
      setSubscription(result.bodyJson);
      setReadProblem(null);
      return;
    }

    setSubscription(null);
    setReadProblem(
      result.status === 404 && result.bodyJson?.code === 'SUBSCRIPTION_NOT_FOUND'
        ? null
        : result,
    );
  }, [call, school.schoolId]);

  useEffect(() => {
    loadSubscription();
  }, [loadSubscription]);

  /**
   * The school's own two reads, together.
   *
   * In parallel because neither depends on the other and both are cheap. They go out with
   * `subdomain`, which is what puts the tenant header on the request — the school surface never
   * names a school in the URL, so this is the only thing that says which school is asking.
   */
  const loadSchoolView = useCallback(async () => {
    const [mine, allowed] = await Promise.all([
      call('get-my-subscription', {
        label: "Read the school's own view",
        subdomain: school.subdomain,
      }),
      call('get-entitlements', {
        label: 'Read the entitlements',
        subdomain: school.subdomain,
      }),
    ]);
    setSchoolView(mine.ok && mine.bodyJson?.subscriptionNo ? mine.bodyJson : null);
    // Guarded on the shape, not just on `ok`: a 200 carrying something unexpected should show
    // the "did not load" panel, not crash the tab on a missing feature list.
    setEntitlements(
      allowed.ok && Array.isArray(allowed.bodyJson?.features) ? allowed.bodyJson : null,
    );
  }, [call, school.subdomain]);

  // Keyed on the subscription number rather than the object, so this runs when the subscription
  // actually changes and not on every refetch of the same one. Refresh reloads all three.
  const subscriptionNo = subscription?.subscriptionNo;
  useEffect(() => {
    if (subscriptionNo) loadSchoolView();
  }, [subscriptionNo, loadSchoolView]);

  const refreshAll = useCallback(async () => {
    await loadSubscription();
    await loadSchoolView();
  }, [loadSubscription, loadSchoolView]);

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

  // Only worth asking for the plans when a plan has to be chosen. A school that already pays
  // never sees the form, so loading the catalogue for it would be a call nothing reads.
  const needsPlans = !reading && !subscription && !readProblem;
  useEffect(() => {
    if (needsPlans) loadPlans();
  }, [needsPlans, loadPlans]);

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
      // Re-read rather than render the 201. One source of truth for what the school is on, and
      // the read carries things the create response does not — the features, daysRemaining.
      await loadSubscription();
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

  /* ------------------------------------------------------- reading what it is on */

  if (reading && !subscription) {
    return <Loading label="Reading the subscription…" />;
  }

  // A missing school, or a backend that is not answering. Not the same as "no subscription".
  if (readProblem) {
    return (
      <Card title="The subscription could not be read">
        <EmptyState
          icon={AlertTriangle}
          title={readProblem.bodyJson?.code || `The server answered ${readProblem.status}`}
          description={
            readProblem.bodyJson?.message
            || 'Nothing came back. Check the environment in the header.'
          }
          action={<Button icon={RefreshCw} onClick={loadSubscription}>Try again</Button>}
        />
        <div className="mt-3 flex justify-center">
          <EndpointTag id="get-subscription" pathParams={{ id: school.schoolId }} />
        </div>
      </Card>
    );
  }

  /* ------------------------------------------------------------- what it is on */

  if (subscription) {
    const s = subscription;
    return (
      <div className="space-y-5">
        <Card
          title="Subscription"
          description={`${s.planName} — ${s.planCode} v${s.planVersion}`}
          action={
            <div className="flex flex-col items-end gap-1">
              <Button icon={RefreshCw} onClick={refreshAll} busy={reading}>
                Refresh
              </Button>
              <EndpointTag id="get-subscription" pathParams={{ id: school.schoolId }} showPath={false} />
            </div>
          }
        >
          <dl className="grid gap-x-4 gap-y-4 sm:grid-cols-3">
            <Detail label="Number">
              <span className="font-mono text-xs">{s.subscriptionNo}</span>
            </Detail>
            <Detail label="Status">
              <Badge look={STATUS_LOOK[s.status] ?? 'grey'}>{s.status}</Badge>
              {s.periodEnded && <Badge look="red" className="ml-1.5">period ended</Badge>}
            </Detail>
            <Detail label="Plan">
              {s.planCode} v{s.planVersion}
              {s.planRetired && <Badge look="grey" className="ml-1.5">retired</Badge>}
            </Detail>

            <Detail label="Billing cycle">{s.billingCycle}</Detail>
            <Detail label="Price">
              {money(s.contractedPrice, s.currencyCode)}
              {/* The gap between the two is the discount, and the discount is what somebody
                  rings up about — so both are shown whenever they differ. */}
              {s.hasDiscount && (
                <span className="ml-1.5 text-xs text-slate-500">
                  list {money(s.planListPrice, s.currencyCode)}
                </span>
              )}
            </Detail>
            <Detail label="Renews automatically">{s.autoRenew ? 'Yes' : 'No'}</Detail>

            <Detail label="Period start">{when(s.currentPeriodStart)}</Detail>
            <Detail label="Period end">{when(s.currentPeriodEnd)}</Detail>
            {/* daysRemaining comes from the API rather than being worked out here: counting days
                between two instants in a browser is where time zones go wrong. */}
            <Detail label="Days left">{daysLeft(s)}</Detail>

            <Detail label="Students">
              {s.maxStudents}
              {s.maxStudentsOverride != null && (
                <Badge look="violet" className="ml-1.5">negotiated</Badge>
              )}
            </Detail>
            <Detail label="Users">
              {s.maxUsers}
              {s.maxUsersOverride != null && (
                <Badge look="violet" className="ml-1.5">negotiated</Badge>
              )}
            </Detail>
            <Detail label="Billing reference">
              {s.billingCustomerReference
                ? <span className="font-mono text-xs">{s.billingCustomerReference}</span>
                : null}
            </Detail>

            {s.cancelledAt && (
              <>
                <Detail label="Cancelled">{when(s.cancelledAt)}</Detail>
                <Detail label="Why" className="sm:col-span-2">{s.cancellationReason}</Detail>
              </>
            )}
          </dl>

          {/* The API's own sentence about anything odd — a lapsed period, a retired plan. Shown
              as it comes rather than re-derived, so the screen cannot disagree with the API. */}
          {s.note && (
            <p className="mt-4 flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs text-amber-900">
              <AlertTriangle size={14} className="mt-px shrink-0 text-amber-600" />
              {s.note}
            </p>
          )}
        </Card>

        {/* --------------------------------- the same subscription, as the school sees it */}
        <Card
          title="What the school sees"
          description="The school's own billing screen — GET /schools/current/subscription"
          action={<EndpointTag id="get-my-subscription" showPath={false} />}
        >
          {schoolView ? (
            <>
              <dl className="grid gap-x-4 gap-y-4 sm:grid-cols-3">
                <Detail label="Plan">{schoolView.planName}</Detail>
                <Detail label="Price">{money(schoolView.price, schoolView.currencyCode)}</Detail>
                <Detail label="Renews">{schoolView.autoRenew ? 'Automatically' : 'No — it ends'}</Detail>
                <Detail label="Period ends">{when(schoolView.currentPeriodEnd)}</Detail>
                <Detail label="Days left">{daysLeft(schoolView)}</Detail>
                <Detail label="Status">
                  <Badge look={STATUS_LOOK[schoolView.status] ?? 'grey'}>{schoolView.status}</Badge>
                </Detail>
              </dl>

              {/* The school's note is written for them, and says different things from the
                  platform's note above — that is the point of it being a separate field. */}
              {schoolView.note && (
                <p className="mt-4 flex items-start gap-2 rounded-lg bg-slate-50 px-3 py-2.5 text-xs text-slate-600">
                  <Info size={14} className="mt-px shrink-0 text-slate-400" />
                  <span>
                    <span className="font-medium">What the school is told: </span>
                    {schoolView.note}
                  </span>
                </p>
              )}

              {/* CHECKED, NOT CLAIMED. These are the fields the platform read has and this one
                  is meant not to — a school must not see a list price it is not paying, or the
                  gateway's id for it. Read off the actual response, so if one ever starts coming
                  through, this panel says so instead of quietly reassuring. */}
              <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50/60 px-3 py-2.5">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                  Held back from the school
                </p>
                <ul className="mt-1.5 space-y-1">
                  {WITHHELD.map(([field, why]) => {
                    const leaked = field in schoolView;
                    return (
                      <li key={field} className="flex items-start gap-1.5 text-[11.5px]">
                        {leaked
                          ? <AlertTriangle size={12} className="mt-0.5 shrink-0 text-red-600" />
                          : <CheckCircle2 size={12} className="mt-0.5 shrink-0 text-emerald-600" />}
                        <span className={leaked ? 'text-red-800' : 'text-slate-500'}>
                          <code className="font-mono">{field}</code>
                          {leaked ? ' is being sent — it should not be. ' : ' — '}
                          {why}
                        </span>
                      </li>
                    );
                  })}
                </ul>
              </div>
            </>
          ) : (
            <EmptyState
              icon={AlertTriangle}
              title="The school's own view did not load"
              description="This read needs the tenant header, which comes from the school's subdomain."
              action={<Button icon={RefreshCw} onClick={loadSchoolView}>Try again</Button>}
            />
          )}
        </Card>

        {/* -------------------------------------------------- what the school may actually use */}
        <Card
          title="Entitlements"
          description="What this school may use right now — GET /schools/current/subscription/entitlements"
          action={<EndpointTag id="get-entitlements" showPath={false} />}
        >
          {!entitlements ? (
            <EmptyState
              icon={AlertTriangle}
              title="The entitlements did not load"
              description="This read needs the tenant header, which comes from the school's subdomain."
              action={<Button icon={RefreshCw} onClick={loadSchoolView}>Try again</Button>}
            />
          ) : (
            <>
              {/* The one thing every module has to honour. A green line here is not decoration:
                  when it is red, nothing on the plan may be used, whatever the plan says. */}
              {entitlements.active ? (
                <p className="flex items-start gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-xs text-emerald-900">
                  <CheckCircle2 size={14} className="mt-px shrink-0 text-emerald-600" />
                  This subscription is granting what the plan includes.
                </p>
              ) : (
                <p className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2.5 text-xs text-red-900">
                  <AlertTriangle size={14} className="mt-px shrink-0 text-red-600" />
                  <span>
                    <strong>Nothing is allowed.</strong> {entitlements.reason}
                  </span>
                </p>
              )}

              <dl className="mt-4 grid gap-x-4 gap-y-4 sm:grid-cols-3">
                <Detail label="Students">{entitlements.maxStudents}</Detail>
                <Detail label="Users">{entitlements.maxUsers}</Detail>
                <Detail label="Features">
                  {entitlements.features.filter((one) => one.allowed).length} of{' '}
                  {entitlements.featureCount} available
                </Detail>
              </dl>

              {entitlements.featureCount === 0 ? (
                <EmptyState
                  icon={Info}
                  title="No features on this plan"
                  description="The plan was published with an empty list, so this school is paying for nothing it can use."
                />
              ) : (
                <ul className="mt-2 divide-y divide-slate-100 border-t border-slate-100">
                  {entitlements.features.map((feature) => (
                    <li key={feature.featureCode} className="flex items-start gap-3 py-2.5">
                      {feature.allowed
                        ? <CheckCircle2 size={15} className="mt-0.5 shrink-0 text-emerald-600" />
                        : <XCircle size={15} className="mt-0.5 shrink-0 text-slate-300" />}
                      <div className="min-w-0 flex-1">
                        <p className={`text-[13px] font-medium ${feature.allowed ? 'text-slate-800' : 'text-slate-400'}`}>
                          {feature.label}
                          {feature.usageLimit != null && (
                            <span className="ml-1.5 font-normal text-slate-500">
                              up to {feature.usageLimit}
                              {feature.usageMetric ? ` ${metricWords(feature.usageMetric)}` : ''}
                            </span>
                          )}
                        </p>
                        {/* The two disagreeing is the whole reason this endpoint exists: the
                            plan grants it, the subscription does not, and a module reading the
                            plan on its own would have let it through. */}
                        {feature.includedInPlan && !feature.allowed && (
                          <p className="mt-0.5 text-[11px] text-amber-800">
                            In the plan, but not available while the subscription is not granting.
                          </p>
                        )}
                        {!feature.includedInPlan && (
                          <p className="mt-0.5 text-[11px] text-slate-400">Not part of this plan.</p>
                        )}
                      </div>
                      {feature.usageLimit != null && (
                        <Badge look="grey" title={POLICY_LABEL[feature.overagePolicy] || feature.overagePolicy}>
                          {feature.overagePolicy}
                        </Badge>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </Card>
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
          <code className="font-mono">GET /platform/schools/{'{id}'}/subscription</code> answered{' '}
          <code className="font-mono">404 SUBSCRIPTION_NOT_FOUND</code>, which is why this form is
          here rather than a subscription. Creating one re-reads it, so what you see next comes
          from the API and not from this form.
        </span>
      </p>
    </div>
  );
}
