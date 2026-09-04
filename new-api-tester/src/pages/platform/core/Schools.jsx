import { useCallback, useEffect, useState } from 'react'
import {
  CheckCircle2, Globe, PauseCircle, PlayCircle, Plus, RefreshCw, Wrench,
} from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'

/**
 * Core / School — platform. All eight endpoints of the operator's view of schools.
 *
 * ONE SCREEN PER ENDPOINT GROUP, and the group is a surface of a module — so everything here is
 * something an operator does from outside the tenant, and the school is always named in the URL.
 * The school's own edits are the school surface and are a different screen; nothing here can
 * reach them.
 *
 * THE LIFECYCLE BUTTONS DEPEND ON WHERE THE SCHOOL IS. A school being set up gets "Finish
 * setting up" and "Take it live"; a live one gets "Suspend". Offering all five always would mean
 * four of them answering 409, which tells you nothing you could not have been told first.
 *
 * EVERY CONTROL SAYS WHICH ENDPOINT IT CALLS, and the tag is clickable once that endpoint has
 * been used — that is the whole point of this app over a product UI.
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

const FILTERS = [
  { id: 'all', label: 'All', statuses: [] },
  { id: 'live', label: 'Live', statuses: ['ACTIVE'] },
  { id: 'trial', label: 'Trial', statuses: ['TRIAL'] },
  { id: 'setup', label: 'Being set up', statuses: ['PROVISIONING'] },
  { id: 'suspended', label: 'Suspended', statuses: ['SUSPENDED'] },
]

const SORTS = ['createdAt,desc', 'createdAt,asc', 'name,asc', 'name,desc', 'status,asc']

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
  complete: { label: 'Finish setting up', endpoint: 'complete-provisioning', icon: Wrench },
  activate: { label: 'Take it live', endpoint: 'activate-school', icon: CheckCircle2, look: 'primary' },
  suspend: { label: 'Suspend', endpoint: 'suspend-school', icon: PauseCircle, look: 'danger', asks: 'reason', required: true },
  reactivate: { label: 'Let it back in', endpoint: 'reactivate-school', icon: PlayCircle, asks: 'note' },
}

/**
 * The query the list sends, from what is on screen.
 *
 * A plain function outside the component on purpose: the tag under the heading has to show the
 * same query the call sends, and building the object in the body would put a fresh one in
 * load()'s dependencies every render — so load() would change identity, the effect would fire
 * again, and the page would call the backend in a loop.
 */
function listQuery({ search, filter, page, size, sort }) {
  const statuses = FILTERS.find((one) => one.id === filter)?.statuses ?? []
  return {
    search: search.trim() || undefined,
    status: statuses.length ? statuses : undefined,
    page,
    size,
    sort,
  }
}

export default function Schools() {
  const { call } = useApi()
  const { environment } = useApiState()

  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('all')
  const [sort, setSort] = useState(SORTS[0])
  const [page, setPage] = useState(0)
  const size = 20

  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [problem, setProblem] = useState(null)

  const [open, setOpen] = useState(null)      // the school being looked at, in full
  const [busy, setBusy] = useState(null)      // which action is running
  const [asking, setAsking] = useState(null)  // the action waiting for a reason
  const [answer, setAnswer] = useState('')
  const [creating, setCreating] = useState(false)

  const query = listQuery({ search, filter, page, size, sort })

  const load = useCallback(async () => {
    setLoading(true)
    const result = await call('list-schools', {
      label: 'List the schools',
      query: listQuery({ search, filter, page, size, sort }),
    })
    setLoading(false)
    if (result.ok) {
      setData(result.bodyJson)
      setProblem(null)
    } else {
      setProblem(result)
    }
    // `environment.id` is not read above and is not a mistake: call() reaches the environment
    // through a ref, so this dependency is the only thing that reloads the list when somebody
    // switches backend. The linter can see it is unused; it cannot see that it is the trigger.
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, search, filter, page, sort])

  useEffect(() => {
    // Debounced, so typing in the search box is one call and not one per keystroke.
    const timer = setTimeout(load, search ? 350 : 0)
    return () => clearTimeout(timer)
  }, [load, search])

  const openSchool = async (schoolId) => {
    // The row is a summary; the whole record is a separate read, which is what #G2 is for.
    const result = await call('get-school', {
      label: 'Read one school',
      pathParams: { id: schoolId },
    })
    if (result.ok) setOpen(result.bodyJson)
  }

  const run = async (school, key, extra) => {
    const action = ACTION[key]
    setBusy(key)
    const result = await call(action.endpoint, {
      label: action.label,
      pathParams: { id: school.schoolId },
      ...(extra ? { body: extra } : {}),
    })
    setBusy(null)
    setAsking(null)
    setAnswer('')
    if (result.ok) {
      await load()
      await openSchool(school.schoolId)
    }
  }

  const rows = data?.content ?? []

  return (
    <div className="page stack">
      <div className="page-head toolbar">
        <div>
          <h1 className="page-title">Schools</h1>
          <p className="muted">
            {data ? `${data.totalElements} on the platform` : 'Every school on the platform'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>Add a school</Button>
      </div>

      <Card>
        <div className="toolbar" style={{ marginBottom: 14 }}>
          <span className="search search-sm">
            <input
              className="search-input"
              value={search}
              onChange={(event) => { setSearch(event.target.value); setPage(0) }}
              placeholder="Search name or subdomain"
              aria-label="Search schools"
            />
          </span>

          <div className="segmented" role="tablist" aria-label="Status">
            {FILTERS.map((one) => (
              <button
                key={one.id}
                type="button"
                role="tab"
                className="segmented-item"
                data-active={filter === one.id || undefined}
                onClick={() => { setFilter(one.id); setPage(0) }}
              >
                {one.label}
              </button>
            ))}
          </div>

          <Select label="Sort" value={sort} onChange={setSort} options={SORTS} />

          <span className="toolbar-spacer" />
          {/* The live request, filters and paging included. Click it for the last response. */}
          <EndpointTag id="list-schools" query={query} />
        </div>

        {problem ? (
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message
              || 'Nothing came back. Is the backend running on 3456?'}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        ) : rows.length === 0 && !loading ? (
          <Empty title="No schools match" description="Change the filter or the search." />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>School</th>
                  <th>Web address</th>
                  <th>Status</th>
                  <th>Added</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((school) => (
                  <tr key={school.schoolId}>
                    <td>{school.schoolName}</td>
                    <td className="mono">{school.subdomain}</td>
                    <td>
                      <Badge tone={STATUS_TONE[school.status]}>{school.status}</Badge>
                    </td>
                    <td className="muted">
                      {school.createdAt ? new Date(school.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td>
                      <Button onClick={() => openSchool(school.schoolId)}>Open</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {data ? (
          <div className="toolbar" style={{ marginTop: 14 }}>
            <span className="muted">
              Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} total
            </span>
            <span className="toolbar-spacer" />
            <div className="pager">
              <Button disabled={!data.hasPrevious} onClick={() => setPage((p) => p - 1)}>Previous</Button>
              <Button disabled={!data.hasNext} onClick={() => setPage((p) => p + 1)}>Next</Button>
            </div>
          </div>
        ) : null}
      </Card>

      <OneSchool
        school={open}
        busy={busy}
        onClose={() => setOpen(null)}
        onAction={(key) => {
          const action = ACTION[key]
          if (action.asks) setAsking(key)
          else run(open, key)
        }}
      />

      <AskFirst
        action={asking ? ACTION[asking] : null}
        value={answer}
        onChange={setAnswer}
        busy={Boolean(busy)}
        onCancel={() => { setAsking(null); setAnswer('') }}
        onConfirm={() => run(open, asking, { [ACTION[asking].asks]: answer.trim() || undefined })}
      />

      <NewSchool
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={async () => { setCreating(false); await load() }}
      />
    </div>
  )
}

/* ------------------------------------------------------------------ one school, in full */

function OneSchool({ school, busy, onClose, onAction }) {
  if (!school) return null
  const actions = actionsFor(school.status)

  return (
    <Modal
      open
      onClose={onClose}
      title={school.schoolName}
      description={`${school.subdomain} · ${school.status}`}
    >
      <div className="stack">
        <div className="resp">
          <div className="resp-head">
            <span>the whole record</span>
            <span className="toolbar-spacer" />
            <EndpointTag id="get-school" pathParams={{ id: school.schoolId }} />
          </div>
          <pre className="resp-body">{JSON.stringify(school, null, 2)}</pre>
        </div>

        {/* Only what this status allows. The other four would answer 409. */}
        {actions.length === 0 ? (
          <p className="muted">
            Nothing can be done to a {school.status} school from this surface.
          </p>
        ) : (
          <div className="stack">
            {actions.map((key) => {
              const action = ACTION[key]
              return (
                <div key={key} className="toolbar">
                  <Button
                    look={action.look}
                    icon={action.icon}
                    busy={busy === key}
                    onClick={() => onAction(key)}
                  >
                    {action.label}
                  </Button>
                  <EndpointTag id={action.endpoint} pathParams={{ id: school.schoolId }} />
                </div>
              )
            })}
          </div>
        )}

        <div className="toolbar">
          <Button icon={Globe} disabled title="Not wired up yet">Change web address</Button>
          <EndpointTag id="change-subdomain" pathParams={{ id: school.schoolId }} />
        </div>
      </div>
    </Modal>
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
          placeholder={action.asks === 'reason' ? 'Unpaid invoices since March.' : 'Cleared on 27 August.'}
        />
      </Field>
    </Modal>
  )
}

/* ------------------------------------------------------------------------ create a school */

const NEW_SCHOOL = {
  schoolName: '', accountHolderName: '', subdomain: '',
  defaultLocale: 'en-IN', defaultTimeZone: 'Asia/Kolkata', countryCode: 'IN',
  emailAddress: '', phoneNumber: '',
}

function NewSchool({ open, onClose, onCreated }) {
  const { call } = useApi()
  const [form, setForm] = useState(NEW_SCHOOL)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value })

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    // Only what was filled in. An empty optional must not be sent as "" — the API would take
    // that as a value and refuse it, where leaving it out means "no value".
    const body = Object.fromEntries(
      Object.entries(form).filter(([, value]) => String(value).trim() !== ''),
    )
    const result = await call('create-school', { label: 'Create a school', body })
    setSaving(false)

    if (result.ok) {
      setForm(NEW_SCHOOL)
      await onCreated()
      return
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    // A refusal that is not about one field — a subdomain already taken — has nowhere to sit
    // beside an input, so it goes at the top of the form.
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) setRefused(result.bodyJson)
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add a school"
      description="Creates the row at PROVISIONING. Setting it up is a separate step."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <div className="stack" style={{ gap: 4, alignItems: 'flex-end' }}>
            <Button look="primary" busy={saving} onClick={submit}>Create</Button>
            <EndpointTag id="create-school" showPath={false} />
          </div>
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field label="Name" required error={errors.schoolName}>
            <Input value={form.schoolName} error={errors.schoolName} onChange={set('schoolName')} />
          </Field>
          <Field label="Account holder" required error={errors.accountHolderName}>
            <Input value={form.accountHolderName} error={errors.accountHolderName} onChange={set('accountHolderName')} />
          </Field>
          <Field label="Web address" required hint="Lowercase, unique across the platform." error={errors.subdomain}>
            <Input value={form.subdomain} error={errors.subdomain} onChange={set('subdomain')} />
          </Field>
          <Field label="Country" required error={errors.countryCode}>
            <Input value={form.countryCode} error={errors.countryCode} onChange={set('countryCode')} />
          </Field>
          <Field label="Language" required error={errors.defaultLocale}>
            <Input value={form.defaultLocale} error={errors.defaultLocale} onChange={set('defaultLocale')} />
          </Field>
          <Field label="Time zone" required error={errors.defaultTimeZone}>
            <Input value={form.defaultTimeZone} error={errors.defaultTimeZone} onChange={set('defaultTimeZone')} />
          </Field>
          <Field label="Email" error={errors.emailAddress}>
            <Input value={form.emailAddress} error={errors.emailAddress} onChange={set('emailAddress')} />
          </Field>
          <Field label="Phone" error={errors.phoneNumber}>
            <Input value={form.phoneNumber} error={errors.phoneNumber} onChange={set('phoneNumber')} />
          </Field>
        </div>
      </div>
    </Modal>
  )
}
