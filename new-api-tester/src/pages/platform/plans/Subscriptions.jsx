import { useCallback, useEffect, useState } from 'react'
import { CheckCircle2, CreditCard, Play, Plus, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import SchoolPicker from '../../../components/SchoolPicker.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { money, plural } from '../../../lib/money.js'
import { METRIC_LABEL } from './features.js'

/**
 * Platform / Plans — subscriptions. What one school is paying for, and how it comes to be.
 *
 * THE SCHOOL IS AN ARGUMENT HERE, NOT A MODE. These endpoints name the school in the URL, so it
 * is a parameter of the call and belongs to this screen — unlike the school surface, where the
 * tenant is a header and "Acting as" in the top bar is a mode that follows you between screens.
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
  const [busy, setBusy] = useState(false)

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

  const activate = async () => {
    setBusy(true)
    // `current` rather than the number: a subscription number is SUB/2026/09/000001 and the
    // slashes end the path segment, so it cannot be written in a URL. The API takes the word.
    await call('activate-subscription', {
      label: 'Activate the trial',
      pathParams: { id: schoolId, subscriptionNo: 'current' },
    })
    setBusy(false)
    await load()
  }

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
          onChange={(id, picked) => { setSchoolId(id); setSchool(picked ?? null) }}
        />
        {schoolId ? (
          <>
            <Button icon={RefreshCw} onClick={load} busy={reading}>Refresh</Button>
            <EndpointTag id="get-subscription" name="Refresh" pathParams={{ id: schoolId }} />
          </>
        ) : null}
      </div>

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
          busy={busy}
          onActivate={activate}
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

      <NewSubscription
        open={creating}
        schoolId={schoolId}
        onClose={() => setCreating(false)}
        onCreated={async () => { setCreating(false); await load() }}
      />
    </div>
  )
}

/* -------------------------------------------------------------- what the school is on */

function TheSubscription({ subscription, schoolId, busy, onActivate }) {
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
          </dl>

          {s.status === 'TRIAL' ? (
            <div className="toolbar">
              <Button look="primary" icon={Play} busy={busy} onClick={onActivate}>
                Turn the trial into a paying subscription
              </Button>
              <span className="muted">
                Same plan, price and limits. Only the status and the period move.
              </span>
              <span className="toolbar-spacer" />
              <EndpointTag
                id="activate-subscription"
                name="Activate the trial"
                look="primary"
                pathParams={{ id: schoolId, subscriptionNo: 'current' }}
              />
            </div>
          ) : (
            <p className="muted">
              Only a TRIAL can be activated. Changing the plan, extending a trial or cancelling
              are endpoints that are not built.
            </p>
          )}
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

/* -------------------------------------------------------------------- create one */

function NewSubscription({ open, schoolId, onClose, onCreated }) {
  const { call } = useApi()
  const [plans, setPlans] = useState(null)
  const [picked, setPicked] = useState('')
  const [trial, setTrial] = useState(false)
  const [price, setPrice] = useState('')
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

  const submit = async () => {
    setRefused(null)
    setSaving(true)
    const body = { planCode: chosen.planCode, planVersion: chosen.planVersion }
    if (trial) body.trial = true
    // An empty box means "charge the plan's list price". Sending 0 would mean free.
    if (price.trim()) body.contractedPrice = Number(price)

    const result = await call('create-subscription', {
      label: 'Give it a subscription', pathParams: { id: schoolId }, body,
    })
    setSaving(false)
    if (result.ok) {
      setPicked('')
      setPrice('')
      setTrial(false)
      await onCreated()
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
          <Button look="primary" busy={saving} disabled={!chosen} onClick={submit}>
            Create it
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
                  {one.sellable ? '' : ' (not sellable today)'}
                </option>
              ))}
            </select>
          </span>
        </Field>

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
