import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  AlertTriangle, Check, ChevronRight, CreditCard, Image, Languages, MapPin, RefreshCw, User,
} from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import { money, plural } from '../../../lib/money.js'
import { screenPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * School / Core — profile. The five endpoints a school uses to edit itself.
 *
 * FOUR WRITES, AND THE SHAPE OF EACH ONE IS THE POINT. They are not one big form on purpose:
 *
 *   PATCH /profile        a partial edit — send only what changed
 *   PUT   /address        replaced whole, because a patched address can name a city in the
 *                         wrong state and still look fine
 *   PATCH /localization   partial, but the time zone needs confirming (see below)
 *   PUT   /logo           replaced whole; blank removes it
 *
 * So each section sends its own request, and the button says which. Collapsing them into one
 * Save would mean inventing a fifth endpoint on the client and guessing which of the four to
 * call — and would send fields nobody touched.
 *
 * NOTHING HERE CAN REACH `status`, `subdomain` OR THE PLAN. Those are the platform surface: a
 * school does not suspend itself or move itself to another plan. The methods do not exist and
 * the fields are not on the requests.
 *
 * THE TIME ZONE ASKS BEFORE IT MOVES. Once an academic year is running, its attendance and
 * holidays are already anchored to the old zone, and the API refuses the change unless
 * `confirmTimeZoneChange` is sent — so the screen asks rather than retrying blind.
 */

/**
 * What this school is paying for, read at the bottom of its profile.
 *
 * A SUMMARY, NOT A SECOND COPY of the subscription screen. Everything a school can be told about
 * its subscription is on School › Plans › Subscription — the feature access, the feature list, what
 * is withheld from it. Repeating that here would be two screens to keep in step for one answer.
 * What belongs here is the one line somebody opening a profile actually wants: what are they on,
 * and is it live.
 *
 * A 404 IS AN ANSWER, NOT A FAILURE. A school with no subscription is a normal state — every
 * school starts that way — so it says so and points at where to fix it, rather than rendering
 * an error next to a profile that loaded perfectly well.
 */
function CurrentSubscription({ subscription, none, loading }) {
  if (loading && !subscription && !none) {
    return <p className="muted">Reading the subscription…</p>
  }

  if (none) {
    return (
      <Card
        title="What this school is paying for"
        action={<EndpointTag id="get-my-subscription" name="The subscription" />}
      >
        <p className="banner" data-tone="warn">
          <CreditCard size={14} />
          <span>
            <strong>No subscription.</strong> Which is why activation on the core module
            complains. One is created on the platform surface — Platform › Plans ›
            Subscriptions.
          </span>
        </p>
      </Card>
    )
  }

  if (!subscription) {
    return (
      <Card
        title="What this school is paying for"
        action={<EndpointTag id="get-my-subscription" name="The subscription" />}
      >
        <p className="muted">
          The subscription could not be read. The profile above loaded, so this is about that
          endpoint rather than the school.
        </p>
      </Card>
    )
  }

  const s = subscription

  return (
    <Card
      title="What this school is paying for"
      description={`${s.planName} · version ${s.planVersion}`}
      action={
        <div className="btn-row">
          <Link className="btn" to={screenPath('school', 'plans', 'subscription')}>
            The full view <ChevronRight size={13} />
          </Link>
          <EndpointTag id="get-my-subscription" name="The subscription" />
        </div>
      }
    >
      <dl className="dl">
        <div>
          <span className="dl-term">Status</span>
          <span className="dl-value">
            <Badge tone={s.status === 'ACTIVE' ? 'good' : s.status === 'TRIAL' ? 'warn' : 'bad'}>
              {s.status}
            </Badge>
            {s.periodEnded ? <Badge tone="bad">period ended</Badge> : null}
          </span>
        </div>
        <div>
          <span className="dl-term">Price</span>
          <span className="dl-value">
            {money(s.price, s.currencyCode)}{' '}
            <span className="muted">{s.billingCycle?.toLowerCase().replace('_', ' ')}</span>
          </span>
        </div>
        <div>
          <span className="dl-term">Number</span>
          <span className="dl-value mono">{s.subscriptionNo}</span>
        </div>
        <div>
          <span className="dl-term">Period ends</span>
          <span className="dl-value">
            {s.currentPeriodEnd ? new Date(s.currentPeriodEnd).toLocaleDateString() : null}
          </span>
        </div>
        <div>
          <span className="dl-term">Days left</span>
          <span className="dl-value">
            {/* From the API. Counting days between two instants in a browser is where time
                zones go wrong, and every screen would do the same sum. */}
            {s.daysRemaining == null
              ? '—'
              : s.daysRemaining >= 0
                ? plural(s.daysRemaining, 'day')
                : `ended ${plural(Math.abs(s.daysRemaining), 'day')} ago`}
          </span>
        </div>
        <div>
          <span className="dl-term">Renews</span>
          <span className="dl-value">{s.autoRenew ? 'Automatically' : 'No — it ends'}</span>
        </div>
      </dl>

      {/* The API's own sentence, written for the school. Shown as it comes rather than
          re-derived, so this card cannot disagree with the subscription screen. */}
      {s.note ? (
        <p className="banner" data-tone="warn" style={{ marginTop: 14 }}>
          <AlertTriangle size={14} />
          {s.note}
        </p>
      ) : null}
    </Card>
  )
}

/** One editable section: which endpoint it sends, and the fields it owns. */
const SECTIONS = [
  {
    id: 'profile',
    title: 'Who they are',
    description: 'A partial edit — only what you change is sent.',
    endpoint: 'update-profile',
    icon: User,
    fields: [
      ['schoolName', 'Name'],
      ['accountHolderName', 'Account holder'],
      ['emailAddress', 'Email'],
      ['phoneNumber', 'Phone'],
    ],
  },
  {
    id: 'address',
    title: 'Where they are',
    description: 'Replaced whole: a patched address can name a city in the wrong state.',
    endpoint: 'replace-address',
    icon: MapPin,
    whole: true,
    fields: [
      ['addressLine', 'Address'],
      ['city', 'City'],
      ['stateOrProvince', 'State'],
      ['postalCode', 'Postcode'],
    ],
  },
  {
    id: 'localization',
    title: 'Language and time',
    description: 'The time zone needs confirming once a year is running.',
    endpoint: 'update-localization',
    icon: Languages,
    fields: [
      ['defaultLocale', 'Language'],
      ['defaultTimeZone', 'Time zone'],
    ],
  },
  {
    id: 'logo',
    title: 'Logo',
    description: 'Replaced whole. Leave it blank to remove it. https only.',
    endpoint: 'replace-logo',
    icon: Image,
    whole: true,
    fields: [['logoUrl', 'Logo URL']],
  },
]

export default function Profile() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [problem, setProblem] = useState(null)
  const [draft, setDraft] = useState({})
  // What the school is paying for. Read here as well as on School › Plans › Subscription,
  // because "who is this school" and "what are they on" are the two things you want together
  // when you open one — and it is a single extra read, in parallel with the profile.
  const [subscription, setSubscription] = useState(null)
  const [noSubscription, setNoSubscription] = useState(false)
  const [saving, setSaving] = useState(null)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain) {
      setProfile(null)
      setSubscription(null)
      return
    }
    setLoading(true)
    // In parallel: the subscription does not depend on the profile, and waiting for one before
    // starting the other would double the time to first paint for no reason.
    const [result, sub] = await Promise.all([
      call('get-profile', { label: 'Read the profile' }),
      call('get-my-subscription', { label: 'What this school is paying for' }),
    ])
    setLoading(false)

    // A school with no subscription answers 404 here, and that is an answer rather than a
    // failure — so it is kept apart from `problem`, which is what stops the page rendering.
    setSubscription(sub.ok ? sub.bodyJson : null)
    setNoSubscription(!sub.ok && sub.bodyJson?.code === 'SUBSCRIPTION_NOT_FOUND')
    if (result.ok) {
      setProfile(result.bodyJson)
      setDraft(result.bodyJson)
      setProblem(null)
    } else {
      setProfile(null)
      setProblem(result)
    }
    // `environment.id` is the trigger for re-reading when somebody switches backend; call()
    // reaches the environment through a ref, so it is not read above.
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingSubdomain, environment.id])

  useEffect(() => {
    load()
  }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="The profile" />

  if (problem) {
    return (
      <div className="page stack">
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message
              || `Nothing came back for "${actingSubdomain}". Is that a real subdomain?`}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
          <div className="toolbar" style={{ justifyContent: 'center', marginTop: 12 }}>
            <EndpointTag id="get-profile" name="The profile" />
          </div>
        </Card>
      </div>
    )
  }

  if (!profile) {
    return <div className="page"><p className="muted">Reading the profile…</p></div>
  }

  /**
   * Sends one section.
   *
   * A PATCH section sends only the fields that actually changed; a PUT section sends all of its
   * fields, because that is what replacing whole means — leaving one out would blank it.
   */
  const save = async (section, extra) => {
    setErrors({})
    setRefused(null)
    setSaving(section.id)

    const body = {}
    section.fields.forEach(([key]) => {
      const next = String(draft[key] ?? '').trim()
      const before = String(profile[key] ?? '').trim()
      if (section.whole || next !== before) body[key] = next || null
    })

    const result = await call(section.endpoint, {
      label: section.title,
      body: { ...body, ...(extra || {}) },
    })
    setSaving(null)

    if (result.ok) {
      setProfile(result.bodyJson)
      setDraft(result.bodyJson)
      return
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) {
      setRefused({ section: section.id, ...result.bodyJson })
    }
  }

  const changed = (section) => section.fields.some(
    ([key]) => String(draft[key] ?? '') !== String(profile[key] ?? ''),
  )

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{profile.schoolName}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span> · what this school can change about
            itself
          </p>
        </div>
        <Badge tone={profile.status === 'ACTIVE' ? 'good' : 'warn'}>{profile.status}</Badge>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <EndpointTag id="get-profile" name="Refresh" />
      </div>

      {SECTIONS.map((section) => {
        const dirty = changed(section)
        const wrong = refused?.section === section.id ? refused : null
        // The API refuses a time-zone change while a year is running unless it is confirmed.
        const needsConfirm = wrong?.code === 'TIME_ZONE_CHANGE_NOT_CONFIRMED'
          || wrong?.code === 'ACADEMIC_YEAR_IN_PROGRESS'

        return (
          <Card
            key={section.id}
            title={section.title}
            description={section.description}
            action={<EndpointTag id={section.endpoint} name={section.title} />}
          >
            <div className="stack">
              {wrong ? (
                <div className="resp">
                  <div className="resp-head">
                    <span className="resp-status" data-ok="false">{wrong.code || 'Refused'}</span>
                  </div>
                  <pre className="resp-body">{wrong.message}</pre>
                </div>
              ) : null}

              <div className="field-grid">
                {section.fields.map(([key, label]) => (
                  <Field key={key} label={label} error={errors[key]}>
                    <Input
                      value={draft[key] ?? ''}
                      error={errors[key]}
                      onChange={(event) => setDraft({ ...draft, [key]: event.target.value })}
                    />
                  </Field>
                ))}
              </div>

              <div className="toolbar">
                <Button
                  look="primary"
                  icon={Check}
                  busy={saving === section.id}
                  disabled={!dirty && !section.whole}
                  onClick={() => save(section)}
                >
                  {section.whole ? 'Replace' : 'Save changes'}
                </Button>

                {/* Only offered once the API has said it needs confirming — sending the flag
                    every time would defeat the check it exists for. */}
                {needsConfirm ? (
                  <Button
                    look="danger"
                    busy={saving === section.id}
                    onClick={() => save(section, { confirmTimeZoneChange: true })}
                  >
                    Change it anyway
                  </Button>
                ) : null}

                <span className="muted">
                  {section.whole
                    ? 'Every field in this section is sent.'
                    : dirty ? 'Only the changed fields are sent.' : 'Nothing changed yet.'}
                </span>
              </div>
            </div>
          </Card>
        )
      })}

      <details className="raw">
        <summary>
          The raw profile
          <span className="toolbar-spacer" />
          <EndpointTag id="get-profile" name="Read from" />
        </summary>
        <pre className="resp-body">{JSON.stringify(profile, null, 2)}</pre>
      </details>

      <CurrentSubscription subscription={subscription} none={noSubscription} loading={loading} />
    </div>
  )
}
