import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft, CheckCircle2, Globe, PauseCircle, PlayCircle, RefreshCw, Wrench,
} from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { screenPath } from '../../../paths.js'

/**
 * One school, at its own address: /platform-core/schools/{id}
 *
 * IT IS A PAGE, NOT A DIALOG. A school is a thing you work on for a while — read it, set it up,
 * take it live, come back to it — and all of that wants an address you can link to, reload and
 * paste to somebody. It also wants room: the record is twenty fields and five actions, which is
 * more than a dialog should hold.
 *
 * THE ID COMES FROM THE URL, so this page always reads the school itself rather than being
 * handed one. Arriving here from a link, a reload or the back button all work the same way, and
 * the record on screen is never a stale copy of a row from a list that has since changed.
 *
 * THE RAW RESPONSE IS STILL HERE, behind a disclosure. This app exists to exercise the API, so
 * the payload matters — but it is what the fields were read FROM, not the thing to lead with.
 */

const STATUS_TONE = {
  ACTIVE: 'good',
  TRIAL: 'warn',
  PROVISIONING: 'warn',
  SUSPENDED: 'bad',
  CLOSED: 'grey',
  DELETED: 'bad',
  DELETION_PENDING: 'bad',
}

/** What can be done next, given where the school is now. */
function actionsFor(status) {
  switch (status) {
    case 'PROVISIONING':
    case 'TRIAL':
      return ['complete', 'activate']
    case 'ACTIVE':
      return ['suspend']
    case 'SUSPENDED':
      return ['reactivate']
    default:
      return []
  }
}

const ACTION = {
  complete: {
    label: 'Finish setting up', endpoint: 'complete-provisioning', icon: Wrench,
    note: 'Creates the numbering sequences and the default roles. Safe to run twice.',
  },
  activate: {
    label: 'Take it live', endpoint: 'activate-school', icon: CheckCircle2, look: 'primary',
    note: 'Refuses unless the setup is complete.',
  },
  suspend: {
    label: 'Suspend', endpoint: 'suspend-school', icon: PauseCircle, look: 'danger',
    asks: 'reason', required: true,
    note: 'The reason is required and is kept on the school.',
  },
  reactivate: {
    label: 'Let it back in', endpoint: 'reactivate-school', icon: PlayCircle,
    asks: 'note',
    note: 'Skips the setup and subscription checks — it was live once already.',
  },
}

const LIST = screenPath('platform', 'core', 'schools')

function Value({ label, children, wide }) {
  const empty = children === null || children === undefined || children === ''
  return (
    <div className={wide ? 'dl-wide' : undefined}>
      <span className="dl-term">{label}</span>
      <span className="dl-value" data-empty={empty ? 'true' : undefined}>
        {empty ? null : children}
      </span>
    </div>
  )
}

const when = (value) => (value ? new Date(value).toLocaleString() : null)

export default function SchoolDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { call } = useApi()
  const { environment } = useApiState()

  const [school, setSchool] = useState(null)
  const [loading, setLoading] = useState(true)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(null)
  const [asking, setAsking] = useState(null)
  const [answer, setAnswer] = useState('')
  const [renaming, setRenaming] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    const result = await call('get-school', {
      label: 'Read one school',
      pathParams: { id },
    })
    setLoading(false)
    if (result.ok) {
      setSchool(result.bodyJson)
      setProblem(null)
    } else {
      setSchool(null)
      setProblem(result)
    }
    // `environment.id` is not read above and is not a mistake: call() reaches the environment
    // through a ref, so this dependency is the only thing that re-reads when somebody switches
    // backend.
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, id, environment.id])

  useEffect(() => {
    load()
  }, [load])

  const run = async (key, extra) => {
    const action = ACTION[key]
    setBusy(key)
    const result = await call(action.endpoint, {
      label: action.label,
      pathParams: { id },
      ...(extra ? { body: extra } : {}),
    })
    setBusy(null)
    setAsking(null)
    setAnswer('')
    // Re-read either way. On success the status has moved; on a refusal the record is still
    // worth refreshing, because the reason it was refused is usually visible in it.
    await load()
    return result
  }

  // The way back and the address stay on screen while the read is in flight. A loading state
  // that drops both leaves somebody stuck on a blank page if the read is slow or never answers.
  if (loading && !school) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All schools</Link>
        <p className="muted">Reading the school <span className="mono">{id}</span>…</p>
      </div>
    )
  }

  if (problem) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All schools</Link>
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message
              || `Nothing came back for id ${id}. Is the backend running on 3456?`}
            action={
              <div className="btn-row" style={{ justifyContent: 'center' }}>
                <Button icon={RefreshCw} onClick={load}>Try again</Button>
                <Button onClick={() => navigate(LIST)}>Back to the list</Button>
              </div>
            }
          />
          <div className="toolbar" style={{ justifyContent: 'center', marginTop: 12 }}>
            <EndpointTag id="get-school" name="The record" pathParams={{ id }} />
          </div>
        </Card>
      </div>
    )
  }

  const actions = actionsFor(school.status)
  const address = [school.addressLine, school.city, school.stateOrProvince, school.postalCode]
    .filter(Boolean).join(', ')

  return (
    <div className="page stack">
      <div>
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All schools</Link>
        <div className="toolbar">
          <div>
            <h1 className="page-title">{school.schoolName}</h1>
            <p className="muted">
              <span className="mono">{school.subdomain}</span>
            </p>
          </div>
          <Badge tone={STATUS_TONE[school.status]}>{school.status}</Badge>
          <span className="toolbar-spacer" />
          <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
          <EndpointTag id="get-school" name="Refresh" pathParams={{ id }} />
        </div>
      </div>

      {school.statusReason ? (
        <Card>
          <span className="dl-term">Why it is {school.status}</span>
          <p className="dl-value">{school.statusReason}</p>
        </Card>
      ) : null}

      <Card title="The record" description="Every field the platform read returns.">
        <dl className="dl">
          <Value label="Account holder">{school.accountHolderName}</Value>
          <Value label="Email">{school.emailAddress}</Value>
          <Value label="Phone">{school.phoneNumber}</Value>
          <Value label="Address" wide>{address}</Value>
          <Value label="Country">{school.countryCode}</Value>
          <Value label="Language">{school.defaultLocale}</Value>
          <Value label="Time zone">{school.defaultTimeZone}</Value>
          <Value label="Added">{when(school.createdAt)}</Value>
          <Value label="Went live">{when(school.activatedAt)}</Value>
          <Value label="Last suspended">{when(school.suspendedAt)}</Value>
          <Value label="Last changed">{when(school.updatedAt)}</Value>
          <Value label="Logo">{school.logoUrl}</Value>
          <Value label="Id"><span className="mono">{school.schoolId}</span></Value>
        </dl>
      </Card>

      <Card
        title="What can be done"
        description={
          actions.length === 0
            ? `Nothing on this surface, while the school is ${school.status}.`
            : 'Only what this status allows — the others would answer 409.'
        }
      >
        <div className="stack">
          {actions.map((key) => {
            const action = ACTION[key]
            return (
              <div key={key} className="toolbar">
                <Button
                  look={action.look}
                  icon={action.icon}
                  busy={busy === key}
                  onClick={() => (action.asks ? setAsking(key) : run(key))}
                >
                  {action.label}
                </Button>
                <span className="muted">{action.note}</span>
                <span className="toolbar-spacer" />
                <EndpointTag id={action.endpoint} name={action.label} pathParams={{ id }} />
              </div>
            )
          })}

          {/* Available at every status, unlike the lifecycle actions: a web address can be
              wrong on a school that is being set up just as easily as on a live one. */}
          <div className="toolbar">
            <Button icon={Globe} onClick={() => setRenaming(true)}>Change web address</Button>
            <span className="muted">Old links stop working immediately.</span>
            <span className="toolbar-spacer" />
            <EndpointTag id="change-subdomain" name="Change web address" pathParams={{ id }} />
          </div>
        </div>
      </Card>

      {/* The payload the fields above were read from. Behind a disclosure because it is the
          evidence, not the headline. */}
      <details className="raw">
        <summary>
          The raw response
          <span className="toolbar-spacer" />
          <EndpointTag id="get-school" name="Read from" pathParams={{ id }} />
        </summary>
        <pre className="resp-body">{JSON.stringify(school, null, 2)}</pre>
      </details>

      <AskFirst
        action={asking ? ACTION[asking] : null}
        value={answer}
        onChange={setAnswer}
        busy={Boolean(busy)}
        onCancel={() => { setAsking(null); setAnswer('') }}
        onConfirm={() => run(asking, { [ACTION[asking].asks]: answer.trim() || undefined })}
      />

      <ChangeSubdomain
        open={renaming}
        school={school}
        onClose={() => setRenaming(false)}
        onDone={async () => { setRenaming(false); await load() }}
      />
    </div>
  )
}

/* --------------------------------------------- the actions that need something said first */

function AskFirst({ action, value, onChange, busy, onCancel, onConfirm }) {
  if (!action) return null
  const missing = action.required && !value.trim()

  return (
    <Modal
      open
      onClose={onCancel}
      title={action.label}
      description={action.required
        ? 'The API requires a reason, and it is kept on the school.'
        : 'A note is optional here.'}
      footer={
        <>
          <Button onClick={onCancel}>Cancel</Button>
          <Button look={action.look || 'primary'} busy={busy} disabled={missing} onClick={onConfirm}>
            {action.label}
          </Button>
        </>
      }
    >
      <Field
        label={action.asks === 'reason' ? 'Reason' : 'Note'}
        required={action.required}
        hint={missing ? undefined : 'Sent as the request body.'}
        error={missing ? 'The API refuses this without a reason.' : undefined}
      >
        <Input
          value={value}
          error={missing}
          onChange={(event) => onChange(event.target.value)}
          placeholder={action.asks === 'reason'
            ? 'Unpaid invoices since March.'
            : 'Cleared on 27 August.'}
        />
      </Field>
    </Modal>
  )
}

/* ---------------------------------------------------------------- change the web address */

/**
 * The one write here that is not a lifecycle step.
 *
 * The API wants BOTH the current subdomain and the new one. That is not redundancy: sending the
 * current one is the caller saying which school it thinks it is renaming, so a stale page cannot
 * rename a school that has already been renamed by somebody else.
 */
function ChangeSubdomain({ open, school, onClose, onDone }) {
  const { call } = useApi()
  const [next, setNext] = useState('')
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  const submit = async () => {
    setRefused(null)
    setSaving(true)
    const result = await call('change-subdomain', {
      label: 'Change the web address',
      pathParams: { id: school.schoolId },
      body: { currentSubdomain: school.subdomain, newSubdomain: next.trim() },
    })
    setSaving(false)
    if (result.ok) {
      setNext('')
      await onDone()
      return
    }
    setRefused(result.bodyJson || { message: `The server answered ${result.status}.` })
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Change the web address"
      description="Every saved link and bookmark for the old address stops working."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button look="primary" busy={saving} disabled={!next.trim()} onClick={submit}>
            Change it
          </Button>
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

        <Field label="Current" hint="Sent with the request, so a stale page cannot rename the wrong school.">
          <Input value={school.subdomain} readOnly />
        </Field>
        <Field label="New address" required>
          <Input
            value={next}
            onChange={(event) => setNext(event.target.value)}
            placeholder="orbit-astra"
          />
        </Field>
      </div>
    </Modal>
  )
}
