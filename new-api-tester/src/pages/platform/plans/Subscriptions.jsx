import { useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowLeftRight, CheckCircle2, CreditCard, Pencil, Plus, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import SchoolPicker from '../../../components/SchoolPicker.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { endOfDay, readableInstant } from '../../../lib/dates.js'
import { money, plural } from '../../../lib/money.js'
import { METRIC_LABEL } from './features.js'
import { sellability } from './planFacts.js'
import { asText, changedFields, patchBody, storedForm } from './subscriptionEdit.js'

/**
 * Platform / Plans — subscriptions. What one school is paying for, and how it comes to be.
 *
 * THE SCHOOL IS AN ARGUMENT HERE, NOT A MODE. These endpoints name the school in the URL, so it
 * is a parameter of the call and belongs to this screen — unlike the school surface, where the
 * tenant is a header and the top bar's School picker is a mode that follows you between screens.
 * The same picker, used for a different thing on purpose.
 *
 * THE READ DECIDES WHICH HALF YOU SEE. A school that already pays gets its subscription; one
 * with none gets the create form. So opening a paying school never shows a form that would only
 * be refused, and `404 SUBSCRIPTION_NOT_FOUND` is not an error here — it is the answer "none
 * yet", which is exactly when the form belongs. `SCHOOL_NOT_FOUND` is a real error and says so.
 *
 * WHAT IS ON SCREEN COMES FROM THE READ, NOT FROM THE CREATE. After creating one this re-reads
 * rather than rendering the 201, so there is one source of truth and the features come with it —
 * the create response does not carry them.
 *
 * THE ONE THING KEPT FROM THE 201 IS `nextStep`, because creating a subscription can also take
 * the SCHOOL from PROVISIONING to ACTIVE, and nothing else on this screen would say so. When it
 * does not — a school whose provisioning is unfinished is left alone — `nextStep` carries the
 * missing piece, and that is the sentence somebody needs to read. So the note is shown, and the
 * school is re-read beside it so the status is the server's answer rather than this screen's
 * guess at it.
 */

const STATUS_TONE = {
  ACTIVE: 'good', TRIAL: 'warn', PAST_DUE: 'bad', SUSPENDED: 'bad',
  CANCELLED: undefined, EXPIRED: undefined,
}

export default function Subscriptions() {
  const { call } = useApi()
  const { environment } = useApiState()

  const [schoolId, setSchoolId] = useState(null)
  const [school, setSchool] = useState(null)
  const [subscription, setSubscription] = useState(null)
  const [reading, setReading] = useState(false)
  const [problem, setProblem] = useState(null)
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState(false)
  const [changingPlan, setChangingPlan] = useState(false)
  // Kept from the 201 only: what creating the subscription did to the school itself.
  const [aftermath, setAftermath] = useState(null)

  const load = useCallback(async () => {
    if (!schoolId) {
      setSubscription(null)
      setProblem(null)
      return
    }
    setReading(true)
    const result = await call('get-subscription', {
      label: 'What this school is on',
      pathParams: { id: schoolId },
    })
    setReading(false)
    if (result.ok) {
      setSubscription(result.bodyJson)
      setProblem(null)
      return
    }
    setSubscription(null)
    // A 404 for no subscription is the other state, not a failure.
    setProblem(
      result.status === 404 && result.bodyJson?.code === 'SUBSCRIPTION_NOT_FOUND'
        ? null
        : result,
    )
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, schoolId, environment.id])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Subscriptions</h1>
          <p className="muted">What one school is paying for. The school is named in the URL.</p>
        </div>
        <span className="toolbar-spacer" />
        <SchoolPicker
          label="School"
          as="id"
          value={schoolId}
          placeholder="pick a school"
          onChange={(id, picked) => {
            setSchoolId(id)
            setSchool(picked ?? null)
            setAftermath(null)
          }}
        />
        {schoolId ? (
          <>
            <Button icon={RefreshCw} onClick={load} busy={reading}>Refresh</Button>
            <EndpointTag id="get-subscription" name="Refresh" pathParams={{ id: schoolId }} />
          </>
        ) : null}
      </div>

      {aftermath ? <WhatTheSaleDid aftermath={aftermath} onDismiss={() => setAftermath(null)} /> : null}

      {!schoolId ? (
        <Card>
          <Empty
            title="No school picked"
            description="These three endpoints all name a school in the URL, so there is nothing to read until you choose one."
            action={<CreditCard size={22} aria-hidden="true" />}
          />
        </Card>
      ) : problem ? (
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message || 'Nothing came back.'}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
          <div className="toolbar" style={{ justifyContent: 'center', marginTop: 12 }}>
            <EndpointTag id="get-subscription" name="What this school is on" pathParams={{ id: schoolId }} />
          </div>
        </Card>
      ) : subscription ? (
        <TheSubscription
          subscription={subscription}
          schoolId={schoolId}
          onEdit={() => setEditing(true)}
          onChangePlan={() => setChangingPlan(true)}
        />
      ) : (
        <Card
          title={`${school?.schoolName ?? 'This school'} has no subscription`}
          description="Which is why activation on the core module complains. Give it one."
          action={<EndpointTag id="get-subscription" name="Read" pathParams={{ id: schoolId }} />}
        >
          <div className="toolbar">
            <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>
              Give it a subscription
            </Button>
            <EndpointTag
              id="create-subscription"
              name="Give it a subscription"
              look="primary"
              pathParams={{ id: schoolId }}
            />
          </div>
        </Card>
      )}

      <ChangePlan
        open={changingPlan}
        schoolId={schoolId}
        subscription={subscription}
        onClose={() => setChangingPlan(false)}
        onSaved={async () => { setChangingPlan(false); await load() }}
        onChanged={async () => { setChangingPlan(false); await load() }}
      />

      <EditSubscription
        open={editing}
        schoolId={schoolId}
        subscription={subscription}
        onClose={() => setEditing(false)}
        onSaved={async () => { setEditing(false); await load() }}
      />

      <NewSubscription
        open={creating}
        schoolId={schoolId}
        onClose={() => setCreating(false)}
        onCreated={async (created) => {
          setCreating(false)
          // The school's status is read back rather than assumed: whether the sale activated it
          // depends on setup this screen cannot see.
          const after = await call('get-school', {
            label: 'What the sale did to the school',
            pathParams: { id: schoolId },
          })
          setAftermath({
            schoolId,
            nextStep: created?.nextStep ?? null,
            status: after.ok ? after.bodyJson?.status : null,
            activatedAt: after.ok ? after.bodyJson?.activatedAt : null,
          })
          if (after.ok) { setSchool(after.bodyJson) }
          await load()
        }}
      />
    </div>
  )
}

/* ------------------------------------------------ what creating the subscription did to the school */

/**
 * The school half of a sale. `nextStep` is the server's own sentence, so it is shown as written
 * rather than reworded here — it says which of the three things happened, and when the school was
 * left alone it says what is missing.
 */
function WhatTheSaleDid({ aftermath, onDismiss }) {
  const wentLive = aftermath.status === 'ACTIVE'
  const stuck = aftermath.status === 'PROVISIONING'
  return (
    <Card
      title={
        wentLive ? 'Sold, and the school is live'
          : stuck ? 'Sold, but the school is still PROVISIONING'
            : 'Sold'
      }
      description="Creating a subscription can be the last thing a school needs to go live."
      action={<EndpointTag id="get-school" name="What the sale did to the school" pathParams={{ id: aftermath.schoolId }} />}
    >
      <div className="stack">
        <div className="toolbar">
          {aftermath.status ? (
            <Badge tone={wentLive ? 'good' : stuck ? 'warn' : undefined}>
              school {aftermath.status}
            </Badge>
          ) : null}
          {aftermath.activatedAt ? (
            <span className="muted">live since {new Date(aftermath.activatedAt).toLocaleString()}</span>
          ) : null}
          <span className="toolbar-spacer" />
          <Button onClick={onDismiss}>Dismiss</Button>
        </div>
        {aftermath.nextStep ? <p className="muted">{aftermath.nextStep}</p> : null}
      </div>
    </Card>
  )
}

/* -------------------------------------------------------------- what the school is on */

function TheSubscription({ subscription, schoolId, onEdit, onChangePlan }) {
  const s = subscription
  return (
    <>
      <Card
        title="The subscription"
        description={`${s.planName} — ${s.planCode} v${s.planVersion}`}
        action={<EndpointTag id="get-subscription" name="The subscription" pathParams={{ id: schoolId }} />}
      >
        <div className="stack">
          {/* The API's own sentence about anything odd — a lapsed period, a retired plan. Shown
              as written so the screen cannot disagree with it. */}
          {s.note ? (
            <p className="resp-body" style={{ borderRadius: 8 }}>{s.note}</p>
          ) : null}

          <dl className="dl">
            <div>
              <span className="dl-term">Number</span>
              <span className="dl-value mono">{s.subscriptionNo}</span>
            </div>
            <div>
              <span className="dl-term">Status</span>
              <span className="dl-value">
                <Badge tone={STATUS_TONE[s.status]}>{s.status}</Badge>
                {s.periodEnded ? <Badge tone="bad">period ended</Badge> : null}
              </span>
            </div>
            <div>
              <span className="dl-term">Plan</span>
              <span className="dl-value">
                <span className="mono">{s.planCode}</span> v{s.planVersion}
                {s.planRetired ? <Badge>retired</Badge> : null}
              </span>
            </div>
            <div>
              <span className="dl-term">Price</span>
              <span className="dl-value">
                {money(s.contractedPrice, s.currencyCode)}
                {/* The gap between the two is a discount, and a discount is what somebody rings
                    up about — so both are shown whenever they differ. */}
                {s.hasDiscount ? (
                  <span className="muted"> list {money(s.planListPrice, s.currencyCode)}</span>
                ) : null}
              </span>
            </div>
            <div>
              <span className="dl-term">Billing cycle</span>
              <span className="dl-value">{s.billingCycle?.toLowerCase().replace('_', ' ')}</span>
            </div>
            <div>
              <span className="dl-term">Renews automatically</span>
              <span className="dl-value">{s.autoRenew ? 'Yes' : 'No'}</span>
            </div>
            <div>
              <span className="dl-term">Period</span>
              <span className="dl-value">
                {s.currentPeriodStart ? new Date(s.currentPeriodStart).toLocaleDateString() : '—'}
                {' → '}
                {s.currentPeriodEnd ? new Date(s.currentPeriodEnd).toLocaleDateString() : '—'}
              </span>
            </div>
            <div>
              <span className="dl-term">Days left</span>
              <span className="dl-value">
                {/* From the API, not worked out here: counting days between two instants in a
                    browser is where time zones go wrong. */}
                {s.daysRemaining == null
                  ? '—'
                  : s.daysRemaining >= 0
                    ? plural(s.daysRemaining, 'day')
                    : `ended ${plural(Math.abs(s.daysRemaining), 'day')} ago`}
              </span>
            </div>
            <div>
              <span className="dl-term">Limits</span>
              <span className="dl-value">
                {s.maxStudents} students · {s.maxUsers} users
                {s.hasLimitOverrides ? <Badge tone="brand">negotiated</Badge> : null}
              </span>
            </div>
            {/* Why it looks the way it does, from the last edit. Only shown when there is one:
                an empty row would suggest the field means "never edited", when in fact an edit
                with no reason given clears it. */}
            {s.reasonForChanges ? (
              <div>
                <span className="dl-term">Last changed because</span>
                <span className="dl-value">{s.reasonForChanges}</span>
              </div>
            ) : null}
          </dl>

          {/* There was an Activate endpoint for exactly this, and it was withdrawn: whether a
              subscription is a trial is decided when it is sold, and moving it on is an edit
              like any other. Said here so the absence of a button reads as a decision. */}
          {s.status === 'TRIAL' ? (
            <p className="banner" data-tone="warn">
              <strong>This is a trial.</strong> It becomes a paying subscription by editing its
              status to <code className="mono">ACTIVE</code> below. If the school is buying a
              different plan from the one it tried, that is a new subscription rather than an
              edit — <code className="mono">#16</code>, not built yet.
            </p>
          ) : null}

          {/* Editing is not a trial-only action, and not a lifecycle one either: it is how a
              subscription that is already wrong gets corrected. */}
          <div className="toolbar">
            <Button icon={Pencil} onClick={onEdit}>Edit the terms</Button>
            <Button icon={ArrowLeftRight} onClick={onChangePlan}>Change the plan</Button>
            <span className="muted">
              Status, plan, price, dates, limits, auto-renewal, the cancellation. One call.
            </span>
            <span className="toolbar-spacer" />
            <EndpointTag
              id="edit-subscription"
              name="Edit the terms"
              pathParams={{ id: schoolId, subscriptionNo: 'current' }}
            />
            <EndpointTag
              id="change-plan"
              name="Change the plan"
              pathParams={{ id: schoolId, subscriptionNo: 'current' }}
            />
          </div>
        </div>
      </Card>

      <Card
        title="What this school may use"
        description={`${plural(s.featureCount, 'feature')} on the plan it is on.`}
      >
        {(s.features ?? []).length === 0 ? (
          <Empty
            title="No features on this plan"
            description="It was published with an empty list, so this school pays for nothing it can use."
          />
        ) : (
          <ul className="divide">
            {s.features.map((feature) => (
              <li key={feature.featureCode} className="feature-row">
                <CheckCircle2
                  size={15}
                  className={feature.enabled ? 'feature-tick' : 'feature-cross'}
                />
                <span className="feature-main">
                  <span className="feature-name">
                    {feature.label}
                    {feature.usageLimit != null ? (
                      <span className="muted">
                        {' '}up to {feature.usageLimit} {METRIC_LABEL[feature.usageMetric] ?? ''}
                      </span>
                    ) : null}
                  </span>
                  <span className="feature-desc">{feature.description}</span>
                </span>
                {feature.usageLimit != null ? <Badge>{feature.overagePolicy}</Badge> : null}
              </li>
            ))}
          </ul>
        )}
      </Card>

      <details className="raw">
        <summary>
          The raw subscription
          <span className="toolbar-spacer" />
          <EndpointTag id="get-subscription" name="Read from" pathParams={{ id: schoolId }} />
        </summary>
        <pre className="resp-body">{JSON.stringify(s, null, 2)}</pre>
      </details>
    </>
  )
}

/* ------------------------------------------------------------------------ edit the terms */

/**
 * How long a period runs, per cycle — mirrored from `resolvePeriodEnd` in
 * PlatformSubscriptionService so the form can say what a sale will produce before making it.
 *
 * A mirror can drift, so `npm test` reads those numbers back out of the Java and fails if the two
 * disagree. `CUSTOM` is absent on purpose: it has no length, which is the whole reason the form
 * has to ask for a date.
 */
const DAYS_PER_CYCLE = { MONTHLY: 30, QUARTERLY: 90, HALF_YEARLY: 180, YEARLY: 365 }

const SUBSCRIPTION_STATUSES = ['TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED', 'CANCELLED', 'EXPIRED']
const BILLING_CYCLES = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', 'CUSTOM']

/**
 * What choosing each one means, under the box that chooses it.
 *
 * The six names are not self-explanatory — PAST_DUE and SUSPENDED are a warning and a
 * cut-off, and which one somebody wants depends on whether the school should still be able to
 * work today. Saying so here is cheaper than finding out from a school that cannot log in.
 */
const STATUS_MEANS = {
  TRIAL: 'Evaluating. The period end is the trial end, and activating it is what turns it into a paying subscription.',
  ACTIVE: 'Paying, and the product is available.',
  PAST_DUE: 'A bill is overdue. The school keeps working — this is the warning stage, not the cut-off.',
  SUSPENDED: 'Access blocked over an unpaid bill. This one stops the school working.',
  CANCELLED: 'Will not renew. When it happened is the history row this writes — no date is stored on the subscription.',
  EXPIRED: 'The last paid period ended and nothing renewed it.',
}

/**
 * Editing what a school is contracted to — the one endpoint that used to be five.
 *
 * <p>PREFILLED, AND ONLY THE DIFFERENCE IS SENT. The endpoint reads an absent field as "leave it
 * alone", so a form that posted every box would send twelve fields to change one, and the history
 * row would say twelve fields were edited. So the form starts from what is stored and each box is
 * compared against it — which is also what makes the body preview below worth reading.
 *
 * <p>THE BODY IS SHOWN BEFORE IT IS SENT. This is an API testing environment, and the interesting
 * part of this endpoint is which fields a given edit does and does not include: emptying the
 * billing reference sends `""`, emptying one limit sends a block with a null in it, and touching
 * nothing sends nothing at all.
 */
function EditSubscription({ open, schoolId, subscription, onClose, onSaved }) {
  // Nothing to edit until there is a subscription. Returning null rather than an empty modal
  // also means the form below MOUNTS on open — which is what lets its state start from the
  // stored values instead of being filled in by an effect a render later.
  //
  // No plan list is loaded either: the plan cannot be changed here (#16), so there is nothing to
  // choose from and nothing to fetch.
  if (!open || !subscription) return null

  return (
    <EditForm
      schoolId={schoolId}
      subscription={subscription}
      onClose={onClose}
      onSaved={onSaved}
    />
  )
}

/**
 * The form. Separate from the shell above so that opening the modal mounts it, and its state can
 * be initialised from the subscription at first render.
 *
 * <p>An effect that copies props into state is a render where the boxes are empty, and it was
 * exactly that render this app has crashed on three times. Here it would also have meant the
 * body preview briefly claiming the edit was empty.
 */
function EditForm({ schoolId, subscription, onClose, onSaved }) {
  const { call } = useApi()
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  // What the boxes start at, and what the diff is taken against.
  const stored = useMemo(() => storedForm(subscription), [subscription])

  const [form, setForm] = useState(() => ({ ...stored, reason: '' }))

  const set = (field) => (event) => {
    const target = event.target
    setForm((current) => ({
      ...current,
      [field]: target.type === 'checkbox' ? target.checked : target.value,
    }))
  }

  const body = useMemo(() => patchBody(form, stored), [form, stored])

  const changed = changedFields(body)
  const nothingChanged = changed.length === 0

  // Every refusal the API would answer with, worked out from the boxes so the answer arrives
  // before the round trip that would empty the form.
  const periodBackwards = Boolean(form.currentPeriodStart) && Boolean(form.currentPeriodEnd)
    && form.currentPeriodEnd < form.currentPeriodStart
  // Three states, not two. #13 writes a figure onto every subscription, copying the plan's when
  // the sale named none, so "a figure is set" no longer means "negotiated" — hasLimitOverrides is
  // what says the school's ceiling differs from its plan's.
  const limitHint = (override, inForce) => {
    if (override == null) return `Blank, so the plan's ${inForce} applies.`
    if (!subscription.hasLimitOverrides) {
      return `${override}, copied from the plan when this was sold. Empty the box to make it `
        + 'follow the plan instead — sent as 0.'
    }
    return `Negotiated away from what the plan lists. Empty the box to remove it — sent as 0, `
      + "which is how the API says \u201cuse the plan's own limit\u201d."
  }

  // Zero is legal now — it is how an override is removed — so only a negative is a refusal.
  const negativeOverride = [form.maxStudentsOverride, form.maxUsersOverride]
    .some((value) => value.trim() !== '' && Number(value) < 0)
  // The API requires it — @NotBlank, so whitespace does not count. Asked for only once there is
  // something to explain: a reason demanded before any box has moved reads as a nag.
  const reasonMissing = !nothingChanged && !form.reason.trim()

  const submit = async () => {
    setRefused(null)
    setSaving(true)
    const result = await call('edit-subscription', {
      label: 'Edit the terms',
      pathParams: { id: schoolId, subscriptionNo: 'current' },
      body,
    })
    setSaving(false)
    if (result.ok) {
      await onSaved(result.bodyJson)
      return
    }
    setRefused(result.bodyJson || { message: `The server answered ${result.status}.` })
  }

  return (
    <Modal
      open
      onClose={onClose}
      title="Edit the terms"
      description="When it runs, what state it is in, how much it may use. Only what you change is sent — the endpoint reads an absent field as “leave it alone”."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button
            look="primary"
            busy={saving}
            disabled={nothingChanged || reasonMissing || periodBackwards || negativeOverride}
            onClick={submit}
          >
            {nothingChanged
              ? 'Nothing changed yet'
              : reasonMissing
                ? 'Say why first'
                : `Send ${plural(changed.length, 'change')}`}
          </Button>
          <EndpointTag
            id="edit-subscription"
            name="Send the changes"
            look="primary"
            pathParams={{ id: schoolId, subscriptionNo: 'current' }}
          />
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code || 'Refused'}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        {/* Every section says what it is for, and the two that only apply sometimes say when.
            A form of thirteen boxes with no order to it is a form where somebody fills in the
            cancellation date of a subscription they were only trying to reprice. */}
        <div className="field-split">Status</div>

        <Field
          label="Status"
          hint={STATUS_MEANS[form.status]}
        >
          <span className="select" style={{ width: '100%' }}>
            <select className="select-input" style={{ width: '100%' }}
              value={form.status} onChange={set('status')}>
              {SUBSCRIPTION_STATUSES.map((one) => (
                <option key={one} value={one}>
                  {one}{one === stored.status ? ' — as it stands' : ''}
                </option>
              ))}
            </select>
          </span>
        </Field>

        {/* What this endpoint cannot touch, said once rather than left to be discovered by a
            box that is not there. Each has its own endpoint because it moves money. */}
        <p className="banner">
          <span>
            <strong>The plan and the money are not editable here.</strong>{' '}
            {subscription.planCode} v{subscription.planVersion} at{' '}
            {money(subscription.contractedPrice, subscription.currencyCode)}
            {subscription.hasDiscount
              ? ` (the plan lists ${money(subscription.planListPrice, subscription.currencyCode)})`
              : ''}
            . Repricing is <code className="mono">#25</code>, the billing customer{' '}
            <code className="mono">#26</code>, changing the plan{' '}
            <code className="mono">#16</code> — none of them built yet.
          </span>
        </p>

        <div className="field-split">The period they have paid for</div>

        <div className="field-grid">
          <Field
            label="Period starts on"
            hint={`Stored as ${readableInstant(subscription.currentPeriodStart)}. A picked day is sent from its start.`}
          >
            <Input type="date" value={form.currentPeriodStart}
              onChange={set('currentPeriodStart')} />
          </Field>
          <Field
            label="Period ends on"
            hint="Push this out to extend a trial — that is all extend-trial ever did. The chosen day is included."
          >
            <Input type="date" value={form.currentPeriodEnd} onChange={set('currentPeriodEnd')} />
          </Field>
          <Field
            label="Billing cycle"
            wide
            hint="Only the cadence. The dates above are NOT recalculated from it — an edit that moved the period end would change what the school is billed for while looking like a change of cadence."
          >
            <span className="select" style={{ width: '100%' }}>
              <select className="select-input" style={{ width: '100%' }}
                value={form.billingCycle} onChange={set('billingCycle')}>
                {BILLING_CYCLES.map((one) => (
                  <option key={one} value={one}>
                    {one}{one === stored.billingCycle ? ' — as it stands' : ''}
                  </option>
                ))}
              </select>
            </span>
          </Field>
        </div>

        {/* Caught here as well as by the API, because a refused request loses the other twelve
            boxes somebody has just filled in. */}
        {periodBackwards ? (
          <p className="banner" data-tone="bad">
            <strong>That period runs backwards.</strong> The end has to come after the start, so
            this would be refused with <code className="mono">400 INVALID_BILLING_PERIOD</code>.
          </p>
        ) : null}

        <label className="feature-row" style={{ cursor: 'pointer' }}>
          <input type="checkbox" className="feature-check"
            checked={form.autoRenew} onChange={set('autoRenew')} />
          <span className="feature-main">
            <span className="feature-name">Renew it automatically at the end of the period</span>
            <span className="feature-desc">
              Nothing renews a subscription yet, so today this records the intention and no more.
            </span>
          </span>
        </label>

        <div className="field-split">Negotiated limits — blank means the plan's own</div>

        <div className="field-grid">
          <Field
            label="Student limit"
            hint={limitHint(subscription.maxStudentsOverride, subscription.maxStudents)}
          >
            <Input type="number" min="0" value={form.maxStudentsOverride}
              onChange={set('maxStudentsOverride')}
              placeholder={asText(subscription.maxStudents)} />
          </Field>
          <Field
            label="User limit"
            hint={limitHint(subscription.maxUsersOverride, subscription.maxUsers)}
          >
            <Input type="number" min="0" value={form.maxUsersOverride}
              onChange={set('maxUsersOverride')}
              placeholder={asText(subscription.maxUsers)} />
          </Field>
        </div>

        {/* Said here so it is not a round trip to find out. */}
        {negativeOverride ? (
          <p className="banner" data-tone="bad">
            <strong>A negative override is refused.</strong> Empty the box to remove the override
            and fall back to the plan's own limit — that is sent as <code className="mono">0</code>,
            which is how this endpoint says it.
          </p>
        ) : null}

        <div className="field-split">Why — required, and stored on the subscription</div>

        <Field
          label="Reason for these changes"
          required
          error={reasonMissing ? 'The API requires this. Whitespace does not count.' : undefined}
          hint={subscription.reasonForChanges
            ? `Saved as reasonForChanges, replacing “${subscription.reasonForChanges}”, and written to the history row beside the fields that moved.`
            : 'Saved as reasonForChanges, and written to the history row beside the fields that moved.'}
        >
          <Input
            value={form.reason}
            onChange={set('reason')}
            error={reasonMissing || undefined}
            placeholder="Renegotiated at renewal — 20% partner discount."
          />
        </Field>

        {/* Why it is required, said where somebody is being made to type. */}
        {reasonMissing ? (
          <p className="banner" data-tone="warn">
            <strong>Every field here is something the school is paying for.</strong> An
            unexplained change to its status, dates or capacity is one nobody can answer for
            months later — so the API refuses an edit with no reason, and each edit replaces the
            last reason rather than adding to it. The full trail stays in the history.
          </p>
        ) : null}

        {/* The point of the screen: which fields this edit actually includes. */}
        <details className="raw" open>
          <summary>
            What will be sent
            <span className="toolbar-spacer" />
            <span className="muted">
              {nothingChanged ? 'nothing yet' : plural(changed.length, 'field')}
            </span>
          </summary>
          <pre className="resp-body">
            {nothingChanged
              ? 'Nothing to send until a box changes. An empty PATCH is refused for the missing '
                + 'reason first (400 VALIDATION_FAILED); a reason on its own is 400 '
                + 'NO_CHANGES_REQUESTED; and resending a value it already holds is a 200 that '
                + 'says nothing changed.'
              // Shown as it stands, incomplete: the body genuinely has no `reason` yet, and the
              // API would refuse it. Filling one in adds it to what is printed here.
              : JSON.stringify(body, null, 2)
                + (reasonMissing ? '\n\n// plus "reason", which is required and not filled in yet' : '')}
          </pre>
        </details>
      </div>
    </Modal>
  )
}

/* ------------------------------------------------------------------- move it to another plan */

/**
 * Moving a school onto a different plan, or a newer version of its own.
 *
 * <p>THE TWO THINGS THIS SCREEN HAS TO SAY OUT LOUD, because neither is visible in the fields:
 * the change is immediate and restarts the billing period, and the money decision moves no money
 * because nothing raises invoices yet. A form that collected them silently would leave somebody
 * believing a charge had been raised.
 */
function ChangePlan({ open, schoolId, subscription, onClose, onChanged }) {
  const { call } = useApi()
  const [plans, setPlans] = useState(null)
  const [picked, setPicked] = useState('')
  const [reason, setReason] = useState('')
  const [price, setPrice] = useState('')
  const [maxStudents, setMaxStudents] = useState('')
  const [maxUsers, setMaxUsers] = useState('')
  // Three states, not a checkbox: absent leaves the school's setting, which a boolean cannot say.
  const [renewal, setRenewal] = useState('')
  const [periodEnd, setPeriodEnd] = useState('')
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open || plans) return
    let alive = true
    call('list-plans', {
      label: 'Plans this school could move to',
      query: { status: 'ACTIVE', page: 0, size: 100 },
    }).then((result) => {
      if (alive) setPlans(result.ok ? (result.bodyJson?.content ?? []) : [])
    })
    return () => { alive = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, plans])

  if (!open || !subscription) return null

  const onNow = `${subscription.planCode}@${subscription.planVersion}`
  const chosen = (plans ?? []).find((one) => `${one.planCode}@${one.planVersion}` === picked)
  // A CUSTOM cycle has no length, so the API cannot derive an end and refuses without one.
  const needsPeriodEnd = chosen?.billingCycle === 'CUSTOM'
  const cycleDays = chosen ? DAYS_PER_CYCLE[chosen.billingCycle] : undefined

  /**
   * Choosing a plan fills the three negotiable boxes with that plan's own figures.
   *
   * Pre-filled rather than left blank with a placeholder, because a placeholder cannot be read
   * back: somebody deciding whether to negotiate needs to see the number they are deciding
   * against, and somebody adjusting it needs something to adjust. Switching plan re-fills them,
   * since the figures belong to the plan rather than to the form.
   *
   * The API treats a figure equal to the plan's exactly as it treats an absent one, so filling
   * them in changes nothing about what the request does.
   */
  const choosePlan = (key) => {
    setPicked(key)
    const plan = (plans ?? []).find((one) => `${one.planCode}@${one.planVersion}` === key)
    setPrice(plan ? String(plan.listPrice) : '')
    setMaxStudents(plan ? String(plan.maxStudents) : '')
    setMaxUsers(plan ? String(plan.maxUsers) : '')
  }

  // Whether this school pays something other than its current plan's list price. Worked out
  // here rather than read off the response, which reports the two prices and no verdict on them.
  const paysNegotiatedPrice = subscription.planListPrice != null
    && Number(subscription.contractedPrice) !== Number(subscription.planListPrice)

  // True while the three boxes still hold the plan's own figures — nobody has negotiated yet.
  const untouchedFromPlan = Boolean(chosen)
    && price === String(chosen.listPrice)
    && maxStudents === String(chosen.maxStudents)
    && maxUsers === String(chosen.maxUsers)

  // Zero is refused by the API on a plan change — there is nothing to remove — so it is caught
  // here rather than after a round trip that empties the form.
  const zeroCeiling = [maxStudents, maxUsers]
    .some((value) => value.trim() !== '' && Number(value) < 1)
  const missing = !chosen || !reason.trim() || (needsPeriodEnd && !periodEnd)

  const submit = async () => {
    setRefused(null)
    setSaving(true)
    const body = {
      planCode: chosen.planCode,
      planVersion: chosen.planVersion,
      reason: reason.trim(),
    }
    // Blank means "the new plan's own figure" for all three — the API's own default, so nothing
    // is sent. That is also why a negotiated price or ceiling has to be retyped to carry it.
    if (price.trim()) body.contractedPrice = Number(price)
    if (maxStudents.trim()) body.maxStudentsOverride = Number(maxStudents)
    if (maxUsers.trim()) body.maxUsersOverride = Number(maxUsers)
    // Left blank the field is not sent at all, which is how the school's existing setting is
    // kept — sending false would turn renewal off for a school that had it on.
    if (renewal) body.autoRenew = renewal === 'on'
    if (needsPeriodEnd) body.currentPeriodEnd = endOfDay(periodEnd)

    const result = await call('change-plan', {
      label: 'Move it to this plan',
      pathParams: { id: schoolId, subscriptionNo: 'current' },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setPicked(''); setReason(''); setPrice(''); setMaxStudents(''); setMaxUsers('')
      setRenewal(''); setPeriodEnd('')
      await onChanged(result.bodyJson)
      return
    }
    setRefused(result.bodyJson || { message: `The server answered ${result.status}.` })
  }

  return (
    <Modal
      open
      onClose={onClose}
      title="Move this school to another plan"
      description="It takes effect immediately, and the billing period restarts with it."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button look="primary" busy={saving} disabled={missing || zeroCeiling} onClick={submit}>
            {missing ? 'Choose a plan and say why' : 'Move it to this plan'}
          </Button>
          <EndpointTag
            id="change-plan"
            name="Move it to this plan"
            look="primary"
            pathParams={{ id: schoolId, subscriptionNo: 'current' }}
          />
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code || 'Refused'}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        {/* Said before anything is chosen, because both facts change what somebody decides. */}
        <p className="banner" data-tone="warn">
          <strong>Immediate, and no money moves.</strong> The plan changes when you send this and
          the period restarts today on the new plan&apos;s cycle. The school is part-way through a
          period it paid for and <em>nothing is charged, credited or refunded</em>: nothing raises
          invoices yet.
        </p>

        {/* The shape of the write, because the response comes back with a different
            subscriptionNo and that reads like a bug until you know why. */}
        <p className="banner">
          <span>
            <strong>This closes {subscription.subscriptionNo} and opens a new one.</strong> The row
            the school is on now is kept as history with{' '}
            <code className="mono">current: false</code>, and the plan it moves to gets a row of
            its own with a new number — one row per plan period. Every read still answers with the
            live one.
          </span>
        </p>

        <Field
          label="Move to"
          required
          hint={chosen
            ? `${money(chosen.listPrice, chosen.currencyCode)} ${chosen.billingCycle.toLowerCase()}, `
              + `${chosen.maxStudents} students, ${chosen.maxUsers} users`
              + (cycleDays ? ` · the period would run ${cycleDays} days from today` : '')
            : `On ${subscription.planCode} v${subscription.planVersion} now. Only published plans are offered.`}
        >
          <span className="select" style={{ width: '100%' }}>
            <select className="select-input" style={{ width: '100%' }}
              value={picked} onChange={(event) => choosePlan(event.target.value)}>
              <option value="">{plans ? 'Choose a plan…' : 'Loading the plans…'}</option>
              {(plans ?? [])
                // The version it is already on is left out: sending it is 409 PLAN_UNCHANGED.
                .filter((one) => `${one.planCode}@${one.planVersion}` !== onNow)
                .map((one) => (
                  <option key={`${one.planCode}@${one.planVersion}`}
                    value={`${one.planCode}@${one.planVersion}`}>
                    {one.name} — {one.planCode} v{one.planVersion}
                    {sellability(one).label ? ` (${sellability(one).label})` : ''}
                  </option>
                ))}
            </select>
          </span>
        </Field>

        {/* Which direction this is, worked out from the two list prices, because "upgrade" and
            "downgrade" change what somebody wants to do about the money. */}
        {chosen ? (
          <p className="banner" data-tone={chosen.listPrice > subscription.planListPrice ? undefined : 'warn'}>
            {chosen.listPrice > subscription.planListPrice ? (
              <span>
                <strong>An upgrade.</strong> The plan&apos;s ceilings follow unless this school
                negotiated its own, which are kept.
              </span>
            ) : (
              <span>
                <strong>A downgrade.</strong> Nothing checks whether the school is already above
                the new plan&apos;s limits — nothing counts students yet — so this is not verified
                as safe.
              </span>
            )}
          </p>
        ) : null}

        {needsPeriodEnd ? (
          <Field
            label="New period ends on"
            required
            hint="This plan bills on a CUSTOM cycle, which has no length, so the end date has to be said. The chosen day is included."
          >
            <Input type="date" value={periodEnd}
              onChange={(event) => setPeriodEnd(event.target.value)} />
          </Field>
        ) : null}

        {/* Nothing negotiable is carried across on its own: a price and a ceiling are agreed
            against a particular plan, so a move means they are agreed again. Blank means the new
            plan's own figure, which is why each box shows what that would be. */}
        <div className="field-split">
          Negotiated terms — filled from the plan, change what was agreed
        </div>

        <div className="field-grid">
          <Field
            label="Agreed price"
            hint={chosen
              ? `The plan's list price. Change it to sell at something else; clearing it charges ${money(chosen.listPrice, chosen.currencyCode)} just the same.`
              : 'Choose a plan and this fills in.'}
          >
            <Input type="number" min="0" step="0.01" value={price}
              onChange={(event) => setPrice(event.target.value)}
              placeholder={chosen ? String(chosen.listPrice) : ''} />
          </Field>
          <Field
            label="Student limit"
            hint={chosen ? `The plan's own ceiling. Raise it to negotiate one.` : 'Choose a plan and this fills in.'}
          >
            <Input type="number" min="1" value={maxStudents}
              onChange={(event) => setMaxStudents(event.target.value)}
              placeholder={chosen ? String(chosen.maxStudents) : ''} />
          </Field>
          <Field
            label="User limit"
            hint={chosen ? `The plan's own ceiling. Raise it to negotiate one.` : 'Choose a plan and this fills in.'}
          >
            <Input type="number" min="1" value={maxUsers}
              onChange={(event) => setMaxUsers(event.target.value)}
              placeholder={chosen ? String(chosen.maxUsers) : ''} />
          </Field>
        </div>

        {/* The one thing somebody moving a negotiated school has to be told before they send it.
            Keyed on the boxes still holding the plan's figures, not on their being blank — they
            are filled in now, so "blank" would never be true and the warning never show. */}
        {untouchedFromPlan && subscription.hasLimitOverrides ? (
          <p className="banner" data-tone="warn">
            <strong>This school has negotiated ceilings, and these are the plan&apos;s.</strong>{' '}
            It is on {subscription.maxStudents} students and {subscription.maxUsers} users now;
            sending it as filled in puts it on {chosen.maxStudents} and {chosen.maxUsers}. Change
            the boxes to keep the arrangement.
          </p>
        ) : null}
        {untouchedFromPlan && paysNegotiatedPrice ? (
          <p className="banner" data-tone="warn">
            <strong>This school pays a negotiated price, and this is the plan&apos;s.</strong>{' '}
            It pays {money(subscription.contractedPrice, subscription.currencyCode)} against a list
            price of {money(subscription.planListPrice, subscription.currencyCode)}. Change the
            price box to continue a discount on the new plan.
          </p>
        ) : null}

        <Field
          label="Renew automatically"
          hint={`Leave this alone and the school keeps its current setting — ${subscription.autoRenew ? 'on' : 'off'}. A plan has no opinion about renewal, so nothing is assumed.`}
        >
          <span className="select" style={{ width: '100%' }}>
            <select className="select-input" style={{ width: '100%' }}
              value={renewal} onChange={(event) => setRenewal(event.target.value)}>
              <option value="">
                Leave it as it is — {subscription.autoRenew ? 'on' : 'off'}
              </option>
              <option value="on">Turn it on</option>
              <option value="off">Turn it off</option>
            </select>
          </span>
        </Field>

        {zeroCeiling ? (
          <p className="banner" data-tone="bad">
            <strong>A limit of 0 is refused here.</strong> There is nothing to remove on a plan
            change — leave the box blank to take the plan&apos;s own figure. Removing an override
            is the edit endpoint, where <code className="mono">0</code> means exactly that.
          </p>
        ) : null}

        <Field
          label="Why"
          required
          hint="Stored on the subscription as reasonForChanges and on the history row. A plan change moves what the school is entitled to and what it pays."
        >
          <Input value={reason} onChange={(event) => setReason(event.target.value)}
            placeholder="Outgrew Starter's 500 students." />
        </Field>
      </div>
    </Modal>
  )
}

/* -------------------------------------------------------------------- create one */

function NewSubscription({ open, schoolId, onClose, onCreated }) {
  const { call } = useApi()
  const [plans, setPlans] = useState(null)
  const [picked, setPicked] = useState('')
  const [trial, setTrial] = useState(false)
  const [price, setPrice] = useState('')
  const [periodEnd, setPeriodEnd] = useState('')
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open || plans) return
    let alive = true
    call('list-plans', {
      label: 'Plans a school could be put on',
      query: { status: 'ACTIVE', page: 0, size: 100 },
    }).then((result) => {
      if (alive) setPlans(result.ok ? (result.bodyJson?.content ?? []) : [])
    })
    return () => { alive = false }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, plans])

  const chosen = (plans ?? []).find((one) => `${one.planCode}@${one.planVersion}` === picked)
  const chosenSellability = chosen ? sellability(chosen) : null

  // A CUSTOM cycle has no length, so there is nothing for the API to derive and it refuses the
  // sale with BILLING_PERIOD_END_REQUIRED. The date is asked for here instead of being found out
  // from a 400.
  const needsPeriodEnd = chosen?.billingCycle === 'CUSTOM'
  const cycleDays = chosen ? DAYS_PER_CYCLE[chosen.billingCycle] : undefined
  // What the sale would produce, for the cycles that have a length. The response is what counts;
  // this is so nobody has to create one to find out.
  const derivedEnd = cycleDays
    ? new Date(Date.now() + cycleDays * 86400000).toISOString().slice(0, 10)
    : null

  const submit = async () => {
    setRefused(null)
    setSaving(true)
    const body = { planCode: chosen.planCode, planVersion: chosen.planVersion }
    if (trial) body.trial = true
    // An empty box means "charge the plan's list price". Sending 0 would mean free.
    if (price.trim()) body.contractedPrice = Number(price)
    // Only for a CUSTOM cycle: every other cycle derives its own end, and sending one would
    // override a length the plan already implies. The chosen day is included, so end of it.
    if (needsPeriodEnd) body.currentPeriodEnd = endOfDay(periodEnd)

    const result = await call('create-subscription', {
      label: 'Give it a subscription', pathParams: { id: schoolId }, body,
    })
    setSaving(false)
    if (result.ok) {
      setPicked('')
      setPrice('')
      setTrial(false)
      setPeriodEnd('')
      await onCreated(result.bodyJson)
      return
    }
    setRefused(result.bodyJson || { message: `The server answered ${result.status}.` })
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Give this school a subscription"
      description="Only a published plan can be sold. The price, currency and cycle come from it."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button
            look="primary"
            busy={saving}
            disabled={!chosen || (needsPeriodEnd && !periodEnd)}
            onClick={submit}
          >
            {needsPeriodEnd && !periodEnd ? 'Set the end date first' : 'Create it'}
          </Button>
          <EndpointTag
            id="create-subscription"
            name="Create it"
            look="primary"
            pathParams={{ id: schoolId }}
          />
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code || 'Refused'}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        <Field
          label="Plan"
          required
          hint={chosen
            ? `${money(chosen.listPrice, chosen.currencyCode)} ${chosen.billingCycle.toLowerCase()}, `
              + `${chosen.maxStudents} students, ${chosen.maxUsers} users`
              + (derivedEnd ? ` · sold today the period runs ${cycleDays} days, to ${derivedEnd}` : '')
            : 'Only published plans are offered — a draft would be refused.'}
        >
          <span className="select" style={{ width: '100%' }}>
            <select
              className="select-input"
              style={{ width: '100%' }}
              value={picked}
              onChange={(event) => setPicked(event.target.value)}
            >
              <option value="">{plans ? 'Choose a plan…' : 'Loading the plans…'}</option>
              {(plans ?? []).map((one) => (
                <option
                  key={`${one.planCode}@${one.planVersion}`}
                  value={`${one.planCode}@${one.planVersion}`}
                >
                  {one.name} — {one.planCode} v{one.planVersion}
                  {/* What the API would actually do, not `sellable` — which is false for a
                      private plan this endpoint sells happily. See sellability(). */}
                  {sellability(one).label ? ` (${sellability(one).label})` : ''}
                </option>
              ))}
            </select>
          </span>
        </Field>

        {/* Said again under the field, because the option text is one line and the difference
            between "private" and "refused" decides whether to bother trying. */}
        {chosenSellability?.label ? (
          <p className="banner" data-tone={chosenSellability.canSell ? 'warn' : 'bad'}>
            {chosenSellability.canSell ? (
              <span>
                <strong>Not on the public list.</strong> This endpoint sells it anyway — that is
                what a private quote is.
              </span>
            ) : (
              <span>
                <strong>This one cannot be sold.</strong> {chosenSellability.label} — sending it
                answers <code className="mono">409 PLAN_NOT_SELLABLE</code>.
              </span>
            )}
          </p>
        ) : null}

        {/* A CUSTOM cycle has no length, so the API cannot derive an end and refuses the sale
            without one. Asked for here rather than found out from a 400. */}
        {needsPeriodEnd ? (
          <>
            <p className="banner" data-tone="warn">
              <strong>This plan bills on a CUSTOM cycle, so it has no length.</strong> Every other
              cycle derives its own end — 30, 90, 180 or 365 days — but a custom contract runs to a
              date somebody agreed, so it has to be said. Without it the sale is refused with
              <code className="mono"> 400 BILLING_PERIOD_END_REQUIRED</code>.
            </p>

            <Field
              label="Period ends on"
              required
              hint="The chosen day is included — it is sent as the last second of it. The period starts today unless the school was sold a backdated contract."
            >
              <Input
                type="date"
                value={periodEnd}
                onChange={(event) => setPeriodEnd(event.target.value)}
              />
            </Field>
          </>
        ) : null}

        <Field
          label="Agreed price"
          hint={chosen
            ? `Blank charges the list price, ${money(chosen.listPrice, chosen.currencyCode)}.`
            : "Blank charges the plan's list price."}
        >
          <Input
            type="number" min="0" step="0.01"
            value={price}
            placeholder={chosen ? String(chosen.listPrice) : ''}
            onChange={(event) => setPrice(event.target.value)}
          />
        </Field>

        <label className="feature-row" style={{ cursor: 'pointer' }}>
          <input
            type="checkbox"
            className="feature-check"
            checked={trial}
            onChange={(event) => setTrial(event.target.checked)}
          />
          <span className="feature-main">
            <span className="feature-name">Start it as a trial</span>
            <span className="feature-desc">
              Opens TRIAL instead of ACTIVE. Everything else is the same, and activating it later
              is one call.
            </span>
          </span>
        </label>
      </div>
    </Modal>
  )
}
