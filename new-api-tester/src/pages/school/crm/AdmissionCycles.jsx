import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Info, Plus, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath } from '../../../paths.js'
import { compact, readable, toInstant, toLocalInput, zoneLabel } from './admissionDates.js'

/**
 * Admission cycles: /school-crm/admission-cycles
 *
 * TWO ENDPOINTS — #5 lists the school's rounds and #1 adds one. The table is #5's answer, so it
 * shows what the school HOLDS rather than what this page happened to create.
 *
 * NO YEAR FILTER BY DEFAULT, which is the opposite of every academics screen. A school works on
 * two years at once during admissions — late admissions into the running year while next year's
 * round is open — so "show me both" is the starting point rather than something to ask for.
 *
 * THE TABLE CARRIES NO NOTES AND NO SEAT TABLE, because #5 does not return them: notes can be two
 * thousand characters and nothing on a list reads them. capacityCount is what survives, and it
 * answers the only question a list needs to — have the seats been set up at all.
 *
 * NOTHING HERE CAN 404. There is no {year} in the path to resolve, so an empty table means "this
 * school has no rounds matching" rather than "no such year" — the same shape grading has.
 *
 * NOTHING IS DISABLED. Every filter can be set to something that returns nothing, the sort can be
 * set to a field the allowlist refuses, and the page can be walked past the end. All three are
 * documented answers worth being able to reach.
 */

const STATUSES = ['', 'DRAFT', 'SCHEDULED', 'OPEN', 'CLOSED', 'COMPLETED', 'CANCELLED']
const SORTS = ['', 'name', 'name,desc', 'academicYear', 'academicYear,desc', 'status',
  'applicationOpenAt', 'applicationCloseAt', 'createdAt,desc', 'updatedAt,desc',
  // NOT on the allowlist — kept so the 400 stays one click away. Ordering is a read.
  'schoolId', 'notes']
const SIZES = ['5', '20', '100']

/**
 * What each status looks like. DRAFT and the two finished ones are deliberately plain: a table
 * where every row is coloured is a table where no colour means anything.
 */
const STATUS_TONE = {
  OPEN: 'good',
  SCHEDULED: 'warn',
  CANCELLED: 'bad',
}

const BLANK = {
  academicYear: '',
  name: '',
  inquiryOpenAt: '',
  applicationOpenAt: '',
  applicationCloseAt: '',
  enrollmentDeadlineAt: '',
  notes: '',
}

export default function AdmissionCycles() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()

  const [open, setOpen] = useState(false)
  const [yearFilter, setYearFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  // Typed, then SENT. `search` is what the last request used; `typed` is what the box holds.
  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [openOn, setOpenOn] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState('20')

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const query = useMemo(() => {
    const out = { page, size: Number(size) }
    if (yearFilter.trim()) out.academicYear = yearFilter.trim()
    if (statusFilter) out.status = statusFilter
    if (search.trim()) out.search = search.trim()
    if (openOn) out.openOn = openOn
    if (sort) out.sort = sort
    return out
  }, [page, size, yearFilter, statusFilter, search, openOn, sort])

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-admission-cycles', {
      label: "The school's admission rounds",
      query,
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, query])

  useEffect(() => { load() }, [load])

  const runSearch = () => { setPage(0); setSearch(typed) }
  const rows = data?.content ?? []

  if (!actingSubdomain) return <NoSchoolChosen what="Admission cycles" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Admission cycles</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {' · every year at once — a school runs next year\'s round during this one'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Create cycle</Button>
      </div>

      <Card
        title="Find a round"
        description="Every filter is optional. Send none and you get the school's rounds, newest year first then by name."
        action={<EndpointTag id="list-admission-cycles" name="List" />}
      >
        <div className="stack">
          <div className="field-grid">
            <Field
              label="Academic year"
              hint="Blank returns EVERY year, and that is the normal case here — unlike classes, where a year is always in the path."
            >
              <Input value={yearFilter} placeholder="2026-2027"
                onChange={(e) => { setPage(0); setYearFilter(e.target.value) }} />
            </Field>
            <Field
              label="Status"
              hint="Blank returns every status, cancelled ones included. Everything #1 creates is DRAFT, because #3 is not built."
            >
              <Select value={statusFilter} options={STATUSES} label="Status filter"
                onChange={(v) => { setPage(0); setStatusFilter(v) }} />
            </Field>
          </div>

          <div className="field-grid">
            <Field
              label="Search"
              hint="Matches the name anywhere, ignoring case. A cycle has no code. Regex-quoted, so a stray ( is an empty page rather than a 500."
            >
              <Input value={typed} onChange={(e) => setTyped(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') runSearch() }}
                placeholder="main" />
            </Field>
            <Field
              label="Sort"
              hint="An allowlist. schoolId and notes are in this list on purpose — they are NOT allowed, and picking one is how the 400 is reached."
            >
              <Select value={sort} options={SORTS} label="Sort"
                onChange={(v) => { setPage(0); setSort(v) }} />
            </Field>
          </div>

          <Field
            label="Taking applications on"
            hint="Which rounds had this moment inside their application window. A cycle missing either date NEVER matches — it is not open forever, its calendar was never filled in."
          >
            <Input type="datetime-local" step="1" value={toLocalInput(openOn)}
              onChange={(e) => { setPage(0); setOpenOn(toInstant(e.target.value)) }} />
          </Field>
          {openOn ? (
            <p className="muted mono">sends {openOn} — {readable(openOn)}</p>
          ) : null}

          <div className="toolbar">
            <Button onClick={runSearch}>Search</Button>
            <span className="toolbar-spacer" />
            <Select value={size} options={SIZES} label="Page size"
              onChange={(value) => { setPage(0); setSize(value) }} />
            <Button onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <Badge>page {(data?.page ?? 0) + 1} of {data?.totalPages ?? 1}</Badge>
            <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>

          <p className="muted">
            <Info size={12} /> <b>Previous and Next are never greyed out.</b> A negative page is{' '}
            <span className="mono">400 INVALID_PAGE</span> and a page past the end is an empty
            page, not a 404 — both are answers worth being able to see.
          </p>
        </div>
      </Card>

      <Card
        title={data ? `${data.totalElements} round${data.totalElements === 1 ? '' : 's'}` : 'Rounds'}
        description="A row is thinner than what Create gives back: no notes and no seat table, only how many classes have seats. Both are on #6, which is not built."
      >
        {problem ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
        ) : rows.length === 0 ? (
          <Empty
            title="Nothing matches"
            description="An empty page, never a 404 — there is no year in the path to be wrong about. Clear the filters to see whether the school has any rounds at all."
            action={
              <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Create one</Button>
            }
          />
        ) : (
          <>
            {/* THE ZONE IS SAID ONCE. Every row shares it, so repeating "GMT+5:30" twenty times
                is noise — and the exact instant is on each cell's title. */}
            <p className="muted">
              Times shown in <b>{zoneLabel()}</b>. Hover a date for the exact instant that is
              stored.
            </p>
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Year</th>
                    <th>Status</th>
                    <th>Applications open</th>
                    <th>Applications close</th>
                    <th className="num">Seats</th>
                    <th>Id</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((cycle) => (
                    // Opening a row is its OWN address, so it can be linked and reloaded — and
                    // #6 is the only endpoint that returns the seat table and the notes.
                    <tr
                      key={cycle.admissionCycleId}
                      data-opens
                      onClick={() => navigate(detailPath('school', 'crm', 'admission-cycles',
                        cycle.admissionCycleId))}
                    >
                      <td>{cycle.name}</td>
                      <td><span className="mono">{cycle.academicYear}</span></td>
                      <td>
                        <Badge tone={STATUS_TONE[cycle.status]}>{cycle.status}</Badge>
                      </td>
                      {/* The instant is the truth and lives on the title; the cell shows the
                          reading, which is what a person is actually looking for. An em dash
                          rather than a blank, so "not set" is visibly a value. */}
                      <td title={cycle.applicationOpenAt ?? 'not set'}>
                        {cycle.applicationOpenAt
                          ? compact(cycle.applicationOpenAt)
                          : <span className="muted">not set</span>}
                      </td>
                      <td title={cycle.applicationCloseAt ?? 'not set'}>
                        {cycle.applicationCloseAt
                          ? compact(cycle.applicationCloseAt)
                          : <span className="muted">not set</span>}
                      </td>
                      <td className="num">{cycle.capacityCount}</td>
                      {/* What applications will store as admissionCycleDocsId. */}
                      <td><span className="mono muted">{cycle.admissionCycleId}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <p className="muted">
              <Info size={12} /> <b>A row is not the whole cycle.</b> #5 leaves out{' '}
              <span className="mono">notes</span> and the seat table — notes can be two thousand
              characters and nothing on a list reads them. <b>Open a row</b> for both: that is #6,
              and is what a row opens.
            </p>
          </>
        )}
      </Card>

      <CreateCycle
        open={open}
        onClose={() => setOpen(false)}
        onAdded={() => { setPage(0); load() }}
      />
    </div>
  )
}

/**
 * The create form — #1. Its own component so the modal sits at the top of its own return, which
 * is the shape every other screen here uses.
 *
 * STAYS OPEN AFTER A SUCCESSFUL CREATE, and only the name is cleared. A school sets up its rounds
 * in one sitting — a general intake and a scholarship round — and the next one is for the same
 * year, so the year and the dates are kept.
 */
/**
 * One of the four dates. A picker, with the instant it will actually send shown underneath.
 *
 * THE INSTANT IS ALWAYS VISIBLE because the picker and the field are different things: the picker
 * holds a wall-clock reading in the browser's zone, the API stores a moment in UTC. Hiding the
 * conversion is how somebody sends `23:59:59Z` meaning midnight in India and quietly gets most of
 * the next day.
 *
 * RAW MODE IS A TEXT BOX and nothing is validated in it. A picker cannot express a malformed
 * instant, and this is an API tester — every refusal has to stay reachable, including the ones a
 * well-behaved control would make impossible.
 */
function DateField({ label, hint, required, raw, value, error, onChange }) {
  return (
    <Field label={label} hint={hint} required={required} error={error}>
      {raw ? (
        <Input value={value} error={error} onChange={(e) => onChange(e.target.value)}
          placeholder="2027-01-31T18:29:59Z" />
      ) : (
        <>
          <Input
            type="datetime-local"
            // Seconds matter here: an end-of-day is 23:59:59, and without this the picker
            // rounds to the minute and quietly sends :00.
            step="1"
            value={toLocalInput(value)}
            error={error}
            onChange={(e) => onChange(toInstant(e.target.value))}
          />
          {value ? (
            <p className="muted mono" style={{ marginTop: 4 }}>
              sends {value} — {readable(value)}
            </p>
          ) : null}
        </>
      )}
    </Field>
  )
}

function CreateCycle({ open, onClose, onAdded }) {
  const { call } = useApi()
  const { actingAcademicYear } = useApiState()

  const [form, setForm] = useState(BLANK)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [last, setLast] = useState(null)
  //! PICKERS BY DEFAULT, raw on request. A picker cannot produce a malformed instant, and a
  //! tester has to be able to send one — so the text boxes stay one click away rather than gone.
  const [raw, setRaw] = useState(false)

  //! PRE-FILLED WHEN IT OPENS, then owned by the box. Reading the picker on every render would
  //! fight somebody typing a different year, which is the whole point of it being editable.
  useEffect(() => {
    if (open) {
      setForm({ ...BLANK, academicYear: actingAcademicYear ?? '' })
      setErrors({})
      setRefused(null)
      setLast(null)
      setRaw(false)
    }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))

  // An empty optional box sends nothing rather than "", which the API would read as a value.
  const body = (() => {
    const out = { academicYear: form.academicYear, name: form.name }
    for (const field of ['inquiryOpenAt', 'applicationOpenAt', 'applicationCloseAt',
      'enrollmentDeadlineAt', 'notes']) {
      if (form[field].trim() !== '') out[field] = form[field].trim()
    }
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-admission-cycle', { label: 'Open a year for admissions', body })
    setSaving(false)
    if (result.ok) {
      setLast(result.bodyJson)
      onAdded(result.bodyJson)
      setForm((old) => ({ ...old, name: '' }))
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
      preview={body}
      title="Open a year for admissions"
      description="Created as a DRAFT with no seats. Setting seats is #4 and opening it is #3, neither of which is built."
      endpoint={<EndpointTag id="create-admission-cycle" name="Create" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Create cycle</Button>
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

        {last ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">{last.status}</span>
            </div>
            <pre className="resp-body">{last.nextStep}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Academic year"
            required
            hint="The year's NAME, and it must already exist. It does NOT have to be the year the school is running — see below."
            error={errors.academicYear}
          >
            <Input value={form.academicYear} error={errors.academicYear}
              onChange={set('academicYear')} placeholder="2027-2028" />
          </Field>
          <Field
            label="Name"
            required
            hint="Unique within the year, not within the school. A school runs more than one round — a general intake and a scholarship round."
            error={errors.name}
          >
            <Input value={form.name} error={errors.name}
              onChange={set('name')} placeholder="Main intake" />
          </Field>
        </div>

        <div className="field-grid">
          <DateField
            label="Enquiries open"
            hint="The first day the front desk logs a parent's enquiry against this round. Earliest of the four — a school gathers interest for weeks before it takes any forms."
            required
            raw={raw} value={form.inquiryOpenAt} error={errors.inquiryOpenAt}
            onChange={(v) => setForm((old) => ({ ...old, inquiryOpenAt: v }))}
          />
          <DateField
            label="Applications open"
            hint="The first moment a family can actually submit a form. Between this and the date beside it, the school is gathering interest but taking no applications."
            required
            raw={raw} value={form.applicationOpenAt} error={errors.applicationOpenAt}
            onChange={(v) => setForm((old) => ({ ...old, applicationOpenAt: v }))}
          />
        </div>

        <div className="field-grid">
          <DateField
            label="Applications close"
            hint="The last moment a form is taken. Pick 11:59:59 pm and the instant below shows what that really is in UTC — for an Indian school, 18:29:59Z."
            required
            raw={raw} value={form.applicationCloseAt} error={errors.applicationCloseAt}
            onChange={(v) => setForm((old) => ({ ...old, applicationCloseAt: v }))}
          />
          <DateField
            label="Enrollment deadline"
            hint="The last moment a family who was OFFERED a seat can take it and become a student. After it the school gives that seat to somebody on the waitlist."
            required
            raw={raw} value={form.enrollmentDeadlineAt} error={errors.enrollmentDeadlineAt}
            onChange={(v) => setForm((old) => ({ ...old, enrollmentDeadlineAt: v }))}
          />
        </div>

        <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" checked={raw} onChange={(e) => setRaw(e.target.checked)} />
          Type the instants myself — a picker cannot produce a malformed one, and that refusal
          should still be reachable.
        </label>

        <Field label="Notes" hint="Optional free text." error={errors.notes}>
          <Input value={form.notes} error={errors.notes}
            onChange={set('notes')} placeholder="Board intake for the main campus." />
        </Field>

        <p className="muted">
          <Info size={12} /> All four dates are optional and <b>only the ones you send are
          compared</b>, in this order: enquiries open, applications open, applications close,
          enrollment deadline. A gap in the middle is fine — the two either side of it are still
          checked against each other. Sending them backwards is{' '}
          <span className="mono">400 CYCLE_DATES_OUT_OF_ORDER</span>.
        </p>

        <p className="muted">
          <Info size={12} /> <b>Nothing enforces any of these four yet.</b> They are stored, given
          back, and read by nothing else — the endpoints that would obey them are #8, #17 and #33,
          and none is built. Even once they are, what decides whether an application can be
          submitted is the cycle&rsquo;s <span className="mono">status</span> being{' '}
          <span className="mono">OPEN</span> (#3), not the date. These four are the school&rsquo;s
          published calendar; the status is the switch.
        </p>

        <p className="muted">
          <Info size={12} /> <b>The year does not have to be the running one</b>, and this is the
          only school write in the product where that is true. A school opens its 2027-2028 cycle
          in the middle of 2026-2027. Try the same year on{' '}
          <span className="mono">Create Class</span>: it answers{' '}
          <span className="mono">409 ACADEMIC_YEAR_NOT_RUNNING</span> and this one answers 201.
        </p>

        <p className="muted">
          <Info size={12} /> <span className="mono">status</span> and{' '}
          <span className="mono">capacities</span> are not accepted. Every cycle starts as a{' '}
          <span className="mono">DRAFT</span> with no seats; setting seats is #4 and opening it is
          #3, neither of which is built.
        </p>
      </div>
    </Modal>
  )
}
