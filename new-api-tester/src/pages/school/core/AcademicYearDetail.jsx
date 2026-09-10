import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  ArrowLeft, CalendarCheck, CalendarX, Check, Lock, Plus, RefreshCw, Trash2, Unlock,
} from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import { screenPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One academic year, at its own address: /school-core/academic-years/{name}
 *
 * FIFTEEN OF THE GROUP'S EIGHTEEN ENDPOINTS ARE HERE, because fourteen of them act on one year
 * and the fifteenth reads it. They are grouped by the question they answer rather than by verb:
 * the dates, the two gates, the holiday calendar, and two reads that take an argument.
 *
 * THE HOLIDAY CALENDAR IS DATED ENTRIES, EACH WITH A LIST OF EVENTS. One date can be a festival
 * AND a school event, so the API stores events per date rather than one reason per day. Adding
 * an event to a date that already has one appends; it does not replace. The screen follows that
 * shape rather than flattening it, because flattening is what loses the second reason.
 *
 * TWO WRITES ARE DELIBERATELY DESTRUCTIVE and say so: PUT replaces the whole calendar, and
 * DELETE by type removes every entry of that kind across the year. Both are marked danger and
 * neither is the default action of anything.
 */

const HOLIDAY_TYPES = [
  'WEEKLY_OFF', 'PUBLIC_HOLIDAY', 'FESTIVAL', 'RELIGIOUS',
  'SCHOOL_EVENT', 'VACATION', 'EXAM_BREAK', 'OTHER',
]

const WEEKDAYS = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
]

const LIST = screenPath('school', 'core', 'academic-years')

export default function AcademicYearDetail() {
  const { name } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [year, setYear] = useState(null)
  const [calendar, setCalendar] = useState(null)
  const [loading, setLoading] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const [one, holidays] = await Promise.all([
      call('get-academic-year', { label: 'The year', pathParams: { name } }),
      call('get-holiday-calendar', { label: 'The calendar', pathParams: { name } }),
    ])
    setLoading(false)
    if (one.ok) {
      setYear(one.bodyJson)
      setProblem(null)
    } else {
      setYear(null)
      setProblem(one)
    }
    setCalendar(holidays.ok ? holidays.bodyJson : null)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, name, actingSubdomain, environment.id])

  useEffect(() => {
    load()
  }, [load])

  const run = async (key, endpoint, options) => {
    setBusy(key)
    await call(endpoint, { pathParams: { name }, ...options })
    setBusy(null)
    await load()
  }

  if (!actingSubdomain) return <NoSchoolChosen what="An academic year" />

  if (problem) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All years</Link>
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message || `No year called "${name}" in this school.`}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
          <div className="toolbar" style={{ justifyContent: 'center', marginTop: 12 }}>
            <EndpointTag id="get-academic-year" name="The year" pathParams={{ name }} />
          </div>
        </Card>
      </div>
    )
  }

  // GUARD ON THE DATA, NOT ON `loading`. The effect that starts the read runs after the first
  // render, so there is always one pass with no year yet — and `loading && !year` was false on
  // exactly that pass, which is how this page used to crash on `year.name`.
  if (!year) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All years</Link>
        <p className="muted">Reading <span className="mono">{name}</span>…</p>
      </div>
    )
  }

  const days = calendar?.holidays ?? []

  return (
    <div className="page stack">
      <div>
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All years</Link>
        <div className="toolbar">
          <div>
            <h1 className="page-title mono">{year.name}</h1>
            <p className="muted">{year.startDate} → {year.endDate}</p>
          </div>
          <Badge tone={year.enrollmentEnabled ? 'good' : undefined}>
            enrollment {year.enrollmentEnabled ? 'open' : 'closed'}
          </Badge>
          {/* `current` is DERIVED from the dates: today is inside them. */}
          {year.current ? <Badge tone="brand">current</Badge> : null}
          {/*
            `isThisYearRunning` is STORED, and sits next to `current` on purpose — the two
            answer different questions and can disagree in either direction. Same three states
            as the list table: null is its own case, because every year written before the field
            existed reads that way and calling it "not running" would state something the record
            does not say.
          */}
          <Badge
            tone={year.isThisYearRunning ? 'good'
              : year.isThisYearRunning === false ? undefined : 'warn'}
            title={year.isThisYearRunning == null
              ? 'This year predates the field, so the record does not say either way.'
              : year.isThisYearRunning === year.current
                ? 'Agrees with the dates.'
                : year.isThisYearRunning
                  ? 'Marked as running, but today is outside its dates.'
                  : 'Today is inside its dates, but the school is not using it — End the year does this.'}
          >
            {year.isThisYearRunning ? 'running'
              : year.isThisYearRunning === false ? 'not running' : 'running not set'}
          </Badge>
          <Badge tone={year.resultsLocked ? 'bad' : 'good'}>
            results {year.resultsLocked ? 'locked' : 'unlocked'}
          </Badge>
          <span className="toolbar-spacer" />
          <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
          <EndpointTag id="get-academic-year" name="Refresh" pathParams={{ name }} />
        </div>
      </div>

      <Dates year={year} name={name} onSaved={load} />

      <Gates year={year} name={name} busy={busy} onRun={run} />

      <EndTheYear year={year} name={name} busy={busy} onRun={run} />

      <Holidays
        name={name}
        calendar={calendar}
        days={days}
        busy={busy}
        onRun={run}
        onReload={load}
      />

      <Asks name={name} />

      <details className="raw">
        <summary>
          The raw year
          <span className="toolbar-spacer" />
          <EndpointTag id="get-academic-year" name="Read from" pathParams={{ name }} />
        </summary>
        <pre className="resp-body">{JSON.stringify(year, null, 2)}</pre>
      </details>
    </div>
  )
}

/* --------------------------------------------------------------------------- the dates */

function Dates({ year, name, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState({ startDate: year.startDate, endDate: year.endDate })
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  const dirty = form.startDate !== year.startDate || form.endDate !== year.endDate

  const save = async () => {
    setRefused(null)
    setSaving(true)
    // Only what moved. Sending both when one changed is harmless here, but the endpoint is a
    // PATCH and reading it as one keeps the request honest about the intent.
    const body = {}
    if (form.startDate !== year.startDate) body.startDate = form.startDate
    if (form.endDate !== year.endDate) body.endDate = form.endDate
    const result = await call('update-academic-year-dates', {
      label: 'Move the dates', pathParams: { name }, body,
    })
    setSaving(false)
    if (result.ok) await onSaved()
    else setRefused(result.bodyJson || { message: `The server answered ${result.status}.` })
  }

  return (
    <Card
      title="When it runs"
      description="Shrinking past a day that already has holidays on it is refused."
      action={<EndpointTag id="update-academic-year-dates" name="Move the dates" pathParams={{ name }} />}
    >
      <div className="stack">
        {refused ? <Refusal refused={refused} /> : null}
        <div className="field-grid">
          <Field label="Starts">
            <Input
              type="date"
              value={form.startDate ?? ''}
              onChange={(event) => setForm({ ...form, startDate: event.target.value })}
            />
          </Field>
          <Field label="Ends">
            <Input
              type="date"
              value={form.endDate ?? ''}
              onChange={(event) => setForm({ ...form, endDate: event.target.value })}
            />
          </Field>
        </div>
        <div className="toolbar">
          <Button look="primary" icon={Check} busy={saving} onClick={save}>
            Move the dates
          </Button>
          <span className="muted">
            {dirty ? 'Only the date you changed is sent.' : 'Nothing changed yet.'}
          </span>
        </div>
      </div>
    </Card>
  )
}

/* --------------------------------------------------------------------------- the gates */

/**
 * The two switches, and they are switches rather than one-way doors — which is why both
 * directions are offered and neither asks for confirmation.
 */
/**
 * Ending the year. Its own card, deliberately not one of the gates.
 *
 * A GATE IS A SWITCH; THIS IS NOT. Enrollment and results have both directions, so the Gates
 * card offers whichever one is not the current state. Ending a year has no opposite: there is no
 * un-end, and the only way back is to move the dates with the editor above. Putting it beside two
 * reversible switches would suggest it is one.
 *
 * IT SHOWS WHAT WOULD HAPPEN BEFORE IT HAPPENS. Today's date, how many days would be cut, and
 * which of the three refusals the year is currently heading for. That is the difference between a
 * button somebody can reason about and one they press to find out.
 *
 * NOTHING IS DISABLED. Every refusal is reachable on purpose — that is what this tool is for — so
 * the warnings say what will happen rather than preventing it.
 */
function EndTheYear({ year, name, busy, onRun }) {
  // The school's own today is what the API uses. The browser's is the closest thing this screen
  // has to it, and the two agree unless the viewer is in a different zone from the school — which
  // is why the note says "as your browser sees it" rather than claiming to know.
  const today = new Date().toISOString().slice(0, 10)
  const notStarted = today <= year.startDate
  const alreadyEnded = year.endDate < today
  const daysCut = alreadyEnded || notStarted
    ? 0
    : Math.round((Date.parse(year.endDate) - Date.parse(today)) / 86400000)

  // Refusals, in the order the API checks them, so the first one shown is the one that will fire.
  const willRefuse = notStarted
    ? { code: 'ACADEMIC_YEAR_NOT_STARTED', why: `It starts on ${year.startDate}, so today is not inside it. A year cannot end before it begins.` }
    : alreadyEnded
      ? { code: 'ACADEMIC_YEAR_ALREADY_ENDED', why: `It already finished on ${year.endDate}. Ending it today would move that date FORWARD, which is the opposite of ending it — so the API refuses.` }
      : null

  return (
    <Card
      title="End the year"
      description="An event, not an edit: the year is over as of today. Sets isThisYearRunning false and writes endDate = today, in the school's own timezone."
    >
      <div className="stack">
        <div className="toolbar">
          <Button
            look="danger"
            icon={CalendarX}
            busy={busy === 'end'}
            onClick={() => onRun('end', 'end-academic-year', { label: 'End the year today' })}
          >
            End it today
          </Button>
          <span className="muted">
            {willRefuse
              ? `This will be refused — see below.`
              : daysCut > 0
                ? `Its last day becomes ${today}, ${daysCut} day(s) earlier than planned.`
                : `Its last day is already ${today}. Nothing would change.`}
          </span>
          <span className="toolbar-spacer" />
          <EndpointTag
            id="end-academic-year"
            name="End the year today"
            look="danger"
            pathParams={{ name }}
          />
        </div>

        {/* The API writes this; before it was returned, the endpoint's main effect was invisible. */}
        <div className="toolbar">
          <Badge tone={year.isThisYearRunning ? 'good' : undefined}>
            {year.isThisYearRunning ? 'the school is running this year' : 'not marked as running'}
          </Badge>
          <span className="muted">
            {year.isThisYearRunning === null || year.isThisYearRunning === undefined
              ? 'null — this year predates the field, which reads as not running.'
              : 'isThisYearRunning. Read it beside `current` above, which is derived from the dates: a finished year can still read true here if nobody ended it.'}
          </span>
        </div>

        {willRefuse ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{willRefuse.code}</span>
            </div>
            <pre className="resp-body">{willRefuse.why}</pre>
          </div>
        ) : null}

        {/* Closed days after today are refused rather than deleted — the same policy as the date
            editor. Counted here so the refusal is not a surprise. */}
        <p className="muted">
          A closed day dated after today is refused as{' '}
          <span className="mono">HOLIDAYS_OUTSIDE_NEW_RANGE</span>, not deleted — remove it first.
          One already behind today is kept, because it is still inside the shortened year.
        </p>
        <p className="muted">
          A year cut very short is allowed here and refused by the date editor above, which
          rejects anything under 30 days as an implausible range. Enrollment and result locking
          are left alone.
        </p>
      </div>
    </Card>
  )
}

function Gates({ year, name, busy, onRun }) {
  const gates = [
    {
      key: 'enrollment',
      on: year.enrollmentEnabled,
      openLabel: 'Open enrollment',
      shutLabel: 'Close enrollment',
      openEndpoint: 'enable-enrollment',
      shutEndpoint: 'disable-enrollment',
      openIcon: CalendarCheck,
      shutIcon: CalendarX,
      note: 'Whether students can be admitted into this year.',
    },
    {
      key: 'results',
      on: !year.resultsLocked,
      openLabel: 'Unlock results',
      shutLabel: 'Lock results',
      openEndpoint: 'unlock-results',
      shutEndpoint: 'lock-results',
      openIcon: Unlock,
      shutIcon: Lock,
      note: 'Locked means marks cannot be edited. Unlocking is deliberate and audited.',
    },
  ]

  return (
    <Card title="The gates" description="Both are switches, so both directions are offered.">
      <div className="stack">
        {gates.map((gate) => {
          const next = gate.on
            ? { label: gate.shutLabel, endpoint: gate.shutEndpoint, icon: gate.shutIcon, look: 'danger' }
            : { label: gate.openLabel, endpoint: gate.openEndpoint, icon: gate.openIcon, look: 'primary' }
          return (
            <div key={gate.key} className="toolbar">
              <Button
                look={next.look}
                icon={next.icon}
                busy={busy === gate.key}
                onClick={() => onRun(gate.key, next.endpoint, { label: next.label })}
              >
                {next.label}
              </Button>
              <span className="muted">{gate.note}</span>
              <span className="toolbar-spacer" />
              <EndpointTag
                id={next.endpoint}
                name={next.label}
                look={next.look}
                pathParams={{ name }}
              />
            </div>
          )
        })}
      </div>
    </Card>
  )
}

/* ------------------------------------------------------------------------ the calendar */

function Holidays({ name, calendar, days, busy, onRun, onReload }) {
  const [adding, setAdding] = useState(false)
  const [editing, setEditing] = useState(null)
  const [weeklyOff, setWeeklyOff] = useState('SUNDAY')
  const [purgeType, setPurgeType] = useState('WEEKLY_OFF')

  // The API reports both counts, so they are read rather than recomputed here — a second
  // opinion on the same number is a second thing that can be wrong.
  const closed = calendar?.closedDayCount ?? days.length
  const events = calendar?.eventCount ?? 0

  return (
    <>
      <Card
        title="Holidays"
        description={`${closed} closed day${closed === 1 ? '' : 's'}, ${events} event${events === 1 ? '' : 's'}. One date can hold more than one reason.`}
        action={<EndpointTag id="get-holiday-calendar" name="The calendar" pathParams={{ name }} />}
      >
        <div className="stack">
          <div className="toolbar">
            <Button look="primary" icon={Plus} onClick={() => setAdding(true)}>Add a holiday</Button>
            <EndpointTag id="add-holiday" name="Add a holiday" look="primary" pathParams={{ name }} />
          </div>

          {days.length === 0 ? (
            <Empty title="No holidays yet" description="Every day in the year counts as working." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr><th>Date</th><th>Day</th><th>Events</th><th /></tr>
                </thead>
                <tbody>
                  {days.map((day) => (
                    <tr key={day.date}>
                      <td className="mono">{day.date}</td>
                      <td className="muted">{day.dayOfWeek?.toLowerCase()}</td>
                      <td>
                        {/* Every event on the date, because the whole reason the API stores a
                            list here is that a date can have more than one reason. */}
                        <div className="stack" style={{ gap: 4 }}>
                          {(day.events ?? []).map((event) => (
                            <span key={`${event.type}-${event.name}`}>
                              <Badge>{event.type}</Badge> {event.name}
                              {event.description
                                ? <span className="muted"> — {event.description}</span>
                                : null}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>
                        <div className="btn-row">
                          {(day.events ?? []).map((event) => (
                            <Button
                              key={`edit-${event.type}`}
                              onClick={() => setEditing({ date: day.date, event })}
                            >
                              Edit {event.type}
                            </Button>
                          ))}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </Card>

      <Card
        title="In bulk"
        description="Two of these change many dates at once, so neither is anything's default."
      >
        <div className="stack">
          <div className="toolbar">
            <Select
              label="Weekly off day"
              value={weeklyOff}
              onChange={setWeeklyOff}
              options={WEEKDAYS}
            />
            <Button
              busy={busy === 'weekly'}
              onClick={() => onRun('weekly', 'generate-weekly-off', {
                label: 'Generate the weekly off', body: { dayOfWeek: weeklyOff },
              })}
            >
              Mark every {weeklyOff.toLowerCase()} off
            </Button>
            <span className="muted">
              A school may run on Sunday, so the day is a choice and not an assumption.
            </span>
            <span className="toolbar-spacer" />
            <EndpointTag id="generate-weekly-off" name="Generate weekly off" pathParams={{ name }} />
          </div>

          <div className="toolbar">
            <Select
              label="Type to remove"
              value={purgeType}
              onChange={setPurgeType}
              options={HOLIDAY_TYPES}
            />
            <Button
              look="danger"
              icon={Trash2}
              busy={busy === 'purge'}
              onClick={() => onRun('purge', 'remove-holidays-by-type', {
                label: `Remove every ${purgeType}`, query: { type: purgeType },
              })}
            >
              Remove every {purgeType}
            </Button>
            <span className="toolbar-spacer" />
            <EndpointTag
              id="remove-holidays-by-type"
              name={`Remove every ${purgeType}`}
              look="danger"
              pathParams={{ name }}
              query={{ type: purgeType }}
            />
          </div>

          <div className="toolbar">
            <ReplaceCalendar name={name} days={days} onDone={onReload} />
          </div>
        </div>
      </Card>

      <AddHoliday
        open={adding}
        name={name}
        onClose={() => setAdding(false)}
        onDone={async () => { setAdding(false); await onReload() }}
      />

      {/* Keyed on the event, so picking a different one remounts the form rather than the
          form having to notice its prop changed. */}
      {editing ? (
        <EditHoliday
          key={`${editing.date}-${editing.event.type}`}
          editing={editing}
          name={name}
          onClose={() => setEditing(null)}
          onDone={async () => { setEditing(null); await onReload() }}
        />
      ) : null}
    </>
  )
}

/* -------------------------------------------------------------------- add one holiday */

function AddHoliday({ open, name, onClose, onDone }) {
  const { call } = useApi()
  const [form, setForm] = useState({ name: '', type: 'FESTIVAL', date: '', description: '' })
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  // Built at render, WHOLE, so the pane beside the form shows exactly what will go — including
  // the optional description, which used to be bolted on inside submit.
  const body = (() => {
    const out = { name: form.name.trim(), type: form.type, date: form.date }
    if (form.description.trim()) out.description = form.description.trim()
    return out
  })()

  const submit = async () => {
    setRefused(null)
    setSaving(true)
    const result = await call('add-holiday', {
      label: 'Add a holiday', pathParams: { name }, body,
    })
    setSaving(false)
    if (result.ok) {
      setForm({ name: '', type: 'FESTIVAL', date: '', description: '' })
      await onDone()
    } else {
      setRefused(result.bodyJson || { message: `The server answered ${result.status}.` })
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title="Add a holiday"
      description="Appends to the date. A date that already has an event keeps it."
      endpoint={
        <EndpointTag id="add-holiday" name="Add it" look="primary" pathParams={{ name }} />
      }
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button
            look="primary"
            busy={saving}
            onClick={submit}
          >
            Add it
          </Button>
        </>
      }
    >
      <div className="stack">
        {refused ? <Refusal refused={refused} /> : null}
        <div className="field-grid">
          <Field label="Date" required>
            <Input
              type="date"
              value={form.date}
              onChange={(event) => setForm({ ...form, date: event.target.value })}
            />
          </Field>
          <Field label="Type" required hint="The same type twice on one date is refused.">
            <Select
              label="Holiday type"
              value={form.type}
              onChange={(type) => setForm({ ...form, type })}
              options={HOLIDAY_TYPES}
            />
          </Field>
        </div>
        <Field label="Name" required>
          <Input
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            placeholder="Diwali (day 1)"
          />
        </Field>
        <Field label="Description">
          <Input
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
            placeholder="Lakshmi Puja"
          />
        </Field>
      </div>
    </Modal>
  )
}

/* ------------------------------------------------- change or remove one event on a date */

/**
 * PATCH and DELETE both take the date in the path AND the type as a query parameter, because a
 * date can hold several events and neither would otherwise know which one is meant.
 */
function EditHoliday({ editing, name, onClose, onDone }) {
  const { call } = useApi()
  const { date, event } = editing
  // Initialised straight from the event, not synced to it in an effect. The parent gives this
  // component a key per event, so opening a different one remounts with the right values in a
  // single render instead of showing the previous event's for a frame.
  const [form, setForm] = useState({
    name: event.name ?? '',
    description: event.description ?? '',
    newType: '',
  })
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(null)
  const query = { type: event.type }

  // Built at render, whole, so the pane beside the form shows what the PATCH will carry. The
  // DELETE half of this dialog sends no body at all.
  const requestBody = (() => {
    const out = { name: form.name.trim(), description: form.description.trim() || null }
    if (form.newType) out.newType = form.newType
    return out
  })()

  const send = async (which) => {
    setRefused(null)
    setSaving(which)
    const options = { pathParams: { name, date }, query }
    if (which === 'save') {
      options.body = requestBody
      options.label = 'Change the holiday'
    } else {
      options.label = 'Remove the holiday'
    }
    const result = await call(which === 'save' ? 'update-holiday' : 'remove-holiday', options)
    setSaving(null)
    if (result.ok) await onDone()
    else setRefused(result.bodyJson || { message: `The server answered ${result.status}.` })
  }

  return (
    <Modal
      open
      onClose={onClose}
      preview={requestBody}
      title={`${event.type} on ${date}`}
      description="The type is sent as a query parameter, because one date can hold several events."
      /* TWO ENDPOINTS, ONE DIALOG: Save patches the event and Remove deletes it, so the heading
         names both rather than picking whichever the eye lands on first. */
      endpoint={
        <>
          <EndpointTag id="update-holiday" name="Save" look="primary"
            pathParams={{ name, date }} query={query} />
          <EndpointTag id="remove-holiday" name="Remove" look="danger"
            pathParams={{ name, date }} query={query} />
        </>
      }
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button
            look="danger"
            icon={Trash2}
            busy={saving === 'remove'}
            onClick={() => send('remove')}
          >
            Remove
          </Button>
          <Button look="primary" busy={saving === 'save'} onClick={() => send('save')}>
            Save
          </Button>
        </>
      }
    >
      <div className="stack">
        {refused ? <Refusal refused={refused} /> : null}
        <Field label="Name">
          <Input
            value={form.name}
            onChange={(field) => setForm({ ...form, name: field.target.value })}
          />
        </Field>
        <Field label="Description">
          <Input
            value={form.description}
            onChange={(field) => setForm({ ...form, description: field.target.value })}
          />
        </Field>
        <Field label="Change the type to" hint="Leave it as is to keep the current type.">
          <Select
            label="New type"
            value={form.newType || event.type}
            onChange={(newType) => setForm({ ...form, newType })}
            options={HOLIDAY_TYPES}
          />
        </Field>
      </div>
    </Modal>
  )
}

/* ------------------------------------------------------------- replace the whole calendar */

/** Whether the box holds something the API could parse. */
function parses(text) {
  if (!text.trim()) return false
  try {
    JSON.parse(text)
    return true
  } catch {
    return false
  }
}

function ReplaceCalendar({ name, days, onDone }) {
  const { call } = useApi()
  const [open, setOpen] = useState(false)
  const [text, setText] = useState('')
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  /** The current calendar, flattened to the shape the request wants. */
  const asRequest = () => JSON.stringify(
    {
      holidays: days.flatMap((day) => (day.events ?? []).map((event) => ({
        name: event.name,
        description: event.description ?? undefined,
        type: event.type,
        date: day.date,
      }))),
    },
    null,
    2,
  )

  const submit = async () => {
    setRefused(null)
    let body
    try {
      body = JSON.parse(text)
    } catch {
      setRefused({ code: 'NOT_JSON', message: 'That is not valid JSON.' })
      return
    }
    setSaving(true)
    const result = await call('replace-holiday-calendar', {
      label: 'Replace the calendar', pathParams: { name }, body,
    })
    setSaving(false)
    if (result.ok) {
      setOpen(false)
      await onDone()
    } else {
      setRefused(result.bodyJson || { message: `The server answered ${result.status}.` })
    }
  }

  return (
    <>
      <Button
        look="danger"
        onClick={() => { setText(asRequest()); setOpen(true) }}
      >
        Replace the whole calendar
      </Button>
      <span className="muted">Everything not in the body is removed.</span>
      <span className="toolbar-spacer" />
      <EndpointTag
        id="replace-holiday-calendar"
        name="Replace the calendar"
        look="danger"
        pathParams={{ name }}
      />

      <Modal
        preview={text.trim() ? text : '{}'}
        previewLabel={parses(text) ? 'Request body' : 'Request body — not valid JSON yet'}
        open={open}
        onClose={() => setOpen(false)}
        title="Replace the whole calendar"
        description="A PUT: whatever is not in this body stops being a holiday."
        endpoint={
          <EndpointTag id="replace-holiday-calendar" name="Replace it" look="danger"
            pathParams={{ name }} />
        }
        footer={
          <>
            <Button onClick={() => setOpen(false)}>Cancel</Button>
            <Button look="danger" busy={saving} onClick={submit}>Replace it</Button>
          </>
        }
      >
        <div className="stack">
          {refused ? <Refusal refused={refused} /> : null}
          <Field
            label="The body"
            hint="Pre-filled with the current calendar, flattened the way the request wants it. Edit and send."
          >
            <textarea
              className="textarea"
              rows={14}
              value={text}
              onChange={(event) => setText(event.target.value)}
              spellCheck={false}
            />
          </Field>
        </div>
      </Modal>
    </>
  )
}

/* ---------------------------------------------------------- the two reads that take input */

function Asks({ name }) {
  const { call } = useApi()
  const [date, setDate] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [day, setDay] = useState(null)
  const [working, setWorking] = useState(null)

  return (
    <Card
      title="Ask about a date"
      description="Two reads that answer a question rather than returning a record."
    >
      <div className="stack">
        <div className="toolbar">
          <Field label="Is this day off?">
            <Input type="date" value={date} onChange={(event) => setDate(event.target.value)} />
          </Field>
          <Button
            onClick={async () => {
              const result = await call('get-day-status', {
                label: 'Day status', pathParams: { name, date },
              })
              setDay(result.ok ? result.bodyJson : result.bodyJson || { message: 'failed' })
            }}
          >
            Ask
          </Button>
          {day ? (
            <Badge tone={day.closed ? 'warn' : 'good'}>
              {day.closed
                ? (day.events?.map((one) => one.type).join(', ') || 'closed')
                : `working ${day.dayOfWeek?.toLowerCase() ?? 'day'}`}
            </Badge>
          ) : null}
          <span className="toolbar-spacer" />
          <EndpointTag id="get-day-status" name="Ask" pathParams={{ name, date: date || '{date}' }} />
        </div>

        <div className="toolbar">
          <Field label="Working days from">
            <Input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
          </Field>
          <Field label="to">
            <Input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
          </Field>
          <Button
            onClick={async () => {
              const result = await call('count-working-days', {
                label: 'Count working days',
                pathParams: { name },
                query: { from: from || undefined, to: to || undefined },
              })
              setWorking(result.ok ? result.bodyJson : null)
            }}
          >
            Count
          </Button>
          {working ? (
            <Badge tone="brand">
              {working.workingDayCount} working of {working.totalDayCount}
              {working.closedDayCount ? `, ${working.closedDayCount} closed` : ''}
            </Badge>
          ) : null}
          <span className="toolbar-spacer" />
          <EndpointTag
            id="count-working-days"
            name="Count"
            pathParams={{ name }}
            query={{ from: from || undefined, to: to || undefined }}
          />
        </div>
      </div>
    </Card>
  )
}

function Refusal({ refused }) {
  return (
    <div className="resp">
      <div className="resp-head">
        <span className="resp-status" data-ok="false">{refused.code || 'Refused'}</span>
      </div>
      <pre className="resp-body">{refused.message}</pre>
    </div>
  )
}
