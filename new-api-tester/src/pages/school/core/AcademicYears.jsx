import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, ChevronRight, Plus, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { detailPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * School / Core — academic years, the list. Three of the group's eighteen endpoints.
 *
 * A YEAR IS ADDRESSED BY ITS NAME, not by an id — `/academic-years/2026-2027`. That is the API's
 * choice and a deliberate one: the name is the natural key other collections reference, so it
 * cannot be renamed. Which means it is safe to put in a URL, unlike a subscription number.
 *
 * "CURRENT" IS ITS OWN ENDPOINT, not a flag on the list. It answers "which year is today in",
 * which the list cannot tell you without the client doing date arithmetic in the browser's zone
 * rather than the school's. So it is read separately and shown as a badge on the row it names.
 */
export default function AcademicYears() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [years, setYears] = useState(null)
  const [current, setCurrent] = useState(null)
  const [asking, setAsking] = useState(false)
  const [loading, setLoading] = useState(false)
  const [problem, setProblem] = useState(null)
  const [creating, setCreating] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    // In parallel: neither depends on the other, and the current year is only a badge.
    const list = await call('list-academic-years', { label: 'The years' })
    setLoading(false)
    if (list.ok) {
      setYears(list.bodyJson ?? [])
      setProblem(null)
    } else {
      setYears(null)
      setProblem(list)
    }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, actingSubdomain, environment.id])

  useEffect(() => {
    load()
  }, [load])

  // A school with no year at all answers 404 here, and that is the answer rather than a failure.
  const askCurrent = async () => {
    setAsking(true)
    const result = await call('get-current-academic-year', { label: 'Which year is today in' })
    setAsking(false)
    setCurrent(result.ok ? result.bodyJson : { none: result.bodyJson?.code || 'none' })
  }

  if (!actingSubdomain) return <NoSchoolChosen what="The academic years" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Academic years</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {years ? ` · ${years.length} year${years.length === 1 ? '' : 's'}` : ''}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <EndpointTag id="list-academic-years" name="Refresh" />
        <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>Add a year</Button>
      </div>

      <Card
        title="The years"
        description="Newest first, as the API returns them."
        action={
          <div className="btn-row">
            {/* `current` is on every row, so the badge does not need this call. It stays as its
                own control because "which year is today in" is a different question from "list
                the years", and it answers 404 when the answer is none — which the list cannot
                say. */}
            <Button busy={asking} onClick={askCurrent}>Which is current?</Button>
            {/* The answer, including "none" — which is a 404 from this endpoint and a real
                answer rather than a failure. */}
            {current ? (
              <Badge tone={current.none ? 'warn' : 'brand'}>
                {current.none ? 'no current year' : current.name}
              </Badge>
            ) : null}
            <EndpointTag id="get-current-academic-year" name="Which is current?" />
          </div>
        }
      >
        {problem ? (
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message || 'Nothing came back.'}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        ) : (years ?? []).length === 0 && !loading ? (
          <Empty
            title="No academic years yet"
            description="A school needs one before anything can be enrolled or examined."
            action={<Button look="primary" icon={Plus} onClick={() => setCreating(true)}>Add the first</Button>}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Year</th>
                  <th>Runs</th>
                  <th>Enrollment</th>
                  <th>Results</th>
                  <th>Holidays</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {(years ?? []).map((year) => (
                  <tr key={year.name}>
                    <td>
                      <span className="mono">{year.name}</span>
                      {year.current ? <Badge tone="brand">current</Badge> : null}
                    </td>
                    <td className="muted">{year.startDate} → {year.endDate}</td>
                    <td>
                      <Badge tone={year.enrollmentEnabled ? 'good' : undefined}>
                        {year.enrollmentEnabled ? 'open' : 'closed'}
                      </Badge>
                    </td>
                    <td>
                      <Badge tone={year.resultsLocked ? 'bad' : 'good'}>
                        {year.resultsLocked ? 'locked' : 'unlocked'}
                      </Badge>
                    </td>
                    <td className="muted">{year.holidayCount ?? '—'}</td>
                    <td>
                      <Link
                        className="btn"
                        to={detailPath('school', 'core', 'academic-years', year.name)}
                      >
                        Open <ChevronRight size={13} />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <NewYear
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={async () => { setCreating(false); await load() }}
      />
    </div>
  )
}

/* ------------------------------------------------------------------------- create a year */

function NewYear({ open, onClose, onCreated }) {
  const { call } = useApi()
  const [form, setForm] = useState({ name: '', startDate: '', endDate: '' })
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value })

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-academic-year', { label: 'Add a year', body: form })
    setSaving(false)
    if (result.ok) {
      setForm({ name: '', startDate: '', endDate: '' })
      await onCreated()
      return
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) setRefused(result.bodyJson)
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Add an academic year"
      description="The name is the key other records point at, so it cannot be changed later."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button look="primary" busy={saving} onClick={submit}>Create</Button>
          <EndpointTag id="create-academic-year" name="Create" look="primary" />
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

        <Field
          label="Name"
          required
          hint="Immutable once created — other collections reference it."
          error={errors.name}
        >
          <Input value={form.name} error={errors.name} onChange={set('name')} placeholder="2026-2027" />
        </Field>
        <div className="field-grid">
          <Field label="Starts" required error={errors.startDate}>
            <Input type="date" value={form.startDate} error={errors.startDate} onChange={set('startDate')} />
          </Field>
          <Field label="Ends" required error={errors.endDate}>
            <Input type="date" value={form.endDate} error={errors.endDate} onChange={set('endDate')} />
          </Field>
        </div>
        <p className="muted">
          <CalendarDays size={12} /> Two years may not overlap, and the API refuses it if they do.
        </p>
      </div>
    </Modal>
  )
}
