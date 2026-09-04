import { useCallback, useEffect, useState } from 'react'
import { AlertTriangle, CheckCircle2, RefreshCw, ShieldCheck, XCircle } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import { money, plural } from '../../../lib/money.js'
import { METRIC_LABEL } from '../../platform/plans/features.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * School / Plans — what this school is paying for, and what it may use.
 *
 * TWO READS, AND THEY ANSWER DIFFERENT QUESTIONS. `/subscription` is the billing screen — plan,
 * price, when it renews. `/subscription/entitlements` is what the rest of the product asks
 * before letting a school use anything. Both are on one screen because a school looking at its
 * bill wants to know what the bill buys, but they are separate cards because they are separate
 * calls with separate answers.
 *
 * THIS IS DELIBERATELY LESS THAN THE PLATFORM SEES. The platform's read of the same subscription
 * (`Platform › Plans › Subscriptions`) carries the plan's list price, the payment gateway's
 * customer reference and the negotiated limit overrides. None of those are here, and that is the
 * whole reason the API has two response types instead of sharing one:
 *
 *   - a school on a negotiated price would be shown a number it is not paying, which is either a
 *     discount somebody then has to explain or an increase they will ring up about
 *   - the gateway's id for them is ours to hold
 *   - "your limit was negotiated up from 2000" is a commercial conversation, not a bill
 *
 * SO THE SCREEN CHECKS THOSE ABSENCES rather than trusting them. Each field is looked for in the
 * live response and reported if it appears. A privacy rule nobody verifies is a privacy rule
 * until the day it is not.
 *
 * READ `allowed`, NOT `includedInPlan`. The first is whether the school may use it right now —
 * the plan saying yes AND the subscription granting anything. The second is only what the plan
 * says. When they differ the row says so, because that difference is the entire reason no module
 * may read the plan's features directly.
 */

const STATUS_TONE = {
  ACTIVE: 'good', TRIAL: 'warn', PAST_DUE: 'bad', SUSPENDED: 'bad',
  CANCELLED: undefined, EXPIRED: undefined,
}

/**
 * What the platform's read carries and this one must not.
 *
 * Listed so the screen can check rather than claim — see the note above.
 */
const WITHHELD = [
  ['planListPrice', 'a school on a negotiated price would see a number it is not paying'],
  ['billingCustomerReference', "the payment gateway's id for them — ours to hold"],
  ['maxStudentsOverride', 'that a limit was negotiated is a commercial conversation'],
  ['maxUsersOverride', 'the same'],
  ['planCode', 'the internal family key. A school reads the name'],
]

const when = (value) => (value ? new Date(value).toLocaleDateString() : null)

export default function Subscription() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [bill, setBill] = useState(null)
  const [entitlements, setEntitlements] = useState(null)
  const [loading, setLoading] = useState(false)
  const [problem, setProblem] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain) {
      setBill(null)
      setEntitlements(null)
      return
    }
    setLoading(true)
    // In parallel: neither depends on the other, and both are cheap reads.
    const [mine, allowed] = await Promise.all([
      call('get-my-subscription', { label: 'What we are paying for' }),
      call('get-entitlements', { label: 'What we may use' }),
    ])
    setLoading(false)

    if (mine.ok) {
      setBill(mine.bodyJson)
      setProblem(null)
    } else {
      setBill(null)
      setProblem(mine)
    }
    // Guarded on the shape, not just on `ok`: a 200 carrying something unexpected should show
    // the "did not load" panel rather than crash on a missing feature list.
    setEntitlements(
      allowed.ok && Array.isArray(allowed.bodyJson?.features) ? allowed.bodyJson : null,
    )
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingSubdomain, environment.id])

  useEffect(() => {
    load()
  }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="A subscription" />

  if (problem) {
    return (
      <div className="page stack">
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message
              || `Nothing came back for "${actingSubdomain}".`}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
          <div className="toolbar" style={{ justifyContent: 'center', marginTop: 12 }}>
            <EndpointTag id="get-my-subscription" name="What we are paying for" />
          </div>
        </Card>
      </div>
    )
  }

  if (!bill) {
    return <div className="page"><p className="muted">Reading the subscription…</p></div>
  }

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{bill.planName}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span> · what this school is paying for
          </p>
        </div>
        <Badge tone={STATUS_TONE[bill.status]}>{bill.status}</Badge>
        {bill.periodEnded ? <Badge tone="bad">period ended</Badge> : null}
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <EndpointTag id="get-my-subscription" name="Refresh" />
      </div>

      {/* Written for the school to read. The platform's version of this note explains what the
          module cannot do yet, which is true, useful internally, and not for a customer. */}
      {bill.note ? (
        <Card>
          <p className="dl-value">{bill.note}</p>
        </Card>
      ) : null}

      <Card
        title="The bill"
        description={bill.planDescription || 'What the school is on, and when it renews.'}
        action={<EndpointTag id="get-my-subscription" name="The bill" />}
      >
        <div className="stack">
          <dl className="dl">
            <div>
              <span className="dl-term">Number</span>
              <span className="dl-value mono">{bill.subscriptionNo}</span>
            </div>
            <div>
              <span className="dl-term">Plan</span>
              <span className="dl-value">{bill.planName} · version {bill.planVersion}</span>
            </div>
            <div>
              <span className="dl-term">Price</span>
              <span className="dl-value">
                {money(bill.price, bill.currencyCode)}{' '}
                <span className="muted">{bill.billingCycle?.toLowerCase().replace('_', ' ')}</span>
              </span>
            </div>
            <div>
              <span className="dl-term">Period</span>
              <span className="dl-value">{when(bill.currentPeriodStart)} → {when(bill.currentPeriodEnd)}</span>
            </div>
            <div>
              <span className="dl-term">Days left</span>
              <span className="dl-value">
                {/* From the API. Counting days between two instants in a browser is where time
                    zones go wrong, and every screen would do the same sum. */}
                {bill.daysRemaining == null
                  ? '—'
                  : bill.daysRemaining >= 0
                    ? plural(bill.daysRemaining, 'day')
                    : `ended ${plural(Math.abs(bill.daysRemaining), 'day')} ago`}
              </span>
            </div>
            <div>
              <span className="dl-term">Renews automatically</span>
              <span className="dl-value">{bill.autoRenew ? 'Yes' : 'No — it ends'}</span>
            </div>
            {bill.cancelledAt ? (
              <div>
                <span className="dl-term">Cancelled</span>
                <span className="dl-value">{when(bill.cancelledAt)}</span>
              </div>
            ) : null}
          </dl>

          {/* CHECKED, NOT CLAIMED. If one of these ever starts coming through, this panel says
              so in red instead of quietly reassuring. */}
          <div className="withheld">
            <p className="withheld-title">Held back from the school</p>
            <ul className="withheld-list">
              {WITHHELD.map(([field, why]) => {
                const leaked = field in bill
                return (
                  <li key={field} className="withheld-row" data-leaked={leaked || undefined}>
                    {leaked
                      ? <AlertTriangle size={12} className="withheld-bad" />
                      : <CheckCircle2 size={12} className="withheld-ok" />}
                    <span>
                      <code className="mono">{field}</code>
                      {leaked ? ' is being sent — it should not be. ' : ' — '}
                      {why}
                    </span>
                  </li>
                )
              })}
            </ul>
          </div>
        </div>
      </Card>

      <Entitlements entitlements={entitlements} onReload={load} />

      <details className="raw">
        <summary>
          The raw responses
          <span className="toolbar-spacer" />
          <EndpointTag id="get-entitlements" name="And" />
        </summary>
        <pre className="resp-body">
          {JSON.stringify({ subscription: bill, entitlements }, null, 2)}
        </pre>
      </details>
    </div>
  )
}

/* ------------------------------------------------------------------------ entitlements */

function Entitlements({ entitlements, onReload }) {
  if (!entitlements) {
    return (
      <Card title="What this school may use">
        <Empty
          title="The entitlements did not load"
          description="This read needs the tenant header, which comes from the school chosen in the top bar."
          action={<Button icon={RefreshCw} onClick={onReload}>Try again</Button>}
        />
        <div className="toolbar" style={{ justifyContent: 'center', marginTop: 12 }}>
          <EndpointTag id="get-entitlements" name="What we may use" />
        </div>
      </Card>
    )
  }

  const e = entitlements
  const usable = e.features.filter((one) => one.allowed).length

  return (
    <Card
      title="What this school may use"
      description="The read every other module asks before letting a school use anything."
      action={<EndpointTag id="get-entitlements" name="What we may use" />}
    >
      <div className="stack">
        {/* The one thing every caller has to honour. When this is red, nothing on the plan may
            be used — whatever the plan says. */}
        {e.active ? (
          <p className="banner" data-tone="good">
            <ShieldCheck size={14} />
            This subscription is granting what the plan includes.
          </p>
        ) : (
          <p className="banner" data-tone="bad">
            <AlertTriangle size={14} />
            <span><strong>Nothing is allowed.</strong> {e.reason}</span>
          </p>
        )}

        <dl className="dl">
          <div>
            <span className="dl-term">Students</span>
            <span className="dl-value">{e.maxStudents}</span>
          </div>
          <div>
            <span className="dl-term">Users</span>
            <span className="dl-value">{e.maxUsers}</span>
          </div>
          <div>
            <span className="dl-term">Features available</span>
            <span className="dl-value">{usable} of {e.featureCount}</span>
          </div>
        </dl>

        {e.featureCount === 0 ? (
          <Empty
            title="No features on this plan"
            description="It was published with an empty list, so this school pays for nothing it can use."
          />
        ) : (
          <ul className="divide">
            {e.features.map((feature) => (
              <li key={feature.featureCode} className="feature-row">
                {feature.allowed
                  ? <CheckCircle2 size={15} className="feature-tick" />
                  : <XCircle size={15} className="feature-cross" />}
                <span className="feature-main">
                  <span className="feature-name">
                    {feature.label}
                    {feature.usageLimit != null ? (
                      <span className="muted">
                        {' '}up to {feature.usageLimit} {METRIC_LABEL[feature.usageMetric] ?? ''}
                      </span>
                    ) : null}
                  </span>
                  {/* The two disagreeing is the whole point of this endpoint: the plan grants it,
                      the subscription does not, and a module reading the plan on its own would
                      have let it through. */}
                  {feature.includedInPlan && !feature.allowed ? (
                    <span className="feature-desc warn-text">
                      In the plan, but not available while the subscription is not granting.
                    </span>
                  ) : !feature.includedInPlan ? (
                    <span className="feature-desc">Not part of this plan.</span>
                  ) : null}
                </span>
                {feature.usageLimit != null ? <Badge>{feature.overagePolicy}</Badge> : null}
              </li>
            ))}
          </ul>
        )}
      </div>
    </Card>
  )
}
