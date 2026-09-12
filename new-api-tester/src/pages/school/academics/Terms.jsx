import { useCallback, useEffect, useMemo, useState } from 'react'
import { Info, Pencil, Plus, RefreshCw, Search } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * The reporting periods of one academic year: /school-academics/terms
 *
 * TWO ENDPOINTS — #9 lists the year's terms and #1 adds one. The table is #9's answer, so it
 * shows what the year HOLDS rather than what this page happened to create.
 *
 * A TERM IS A DOCUMENT, unlike a section or a subject. Six documents across three modules store
 * termDocsId, which is why it has an id — and why a term can be renamed where a sectionNo can
 * never be. The table shows the id for that reason.
 *
 * FIVE FILTERS, AND EVERY ONE IS TRISTATE OR FREE TEXT. Blank sends no parameter at all, which is
 * not the same as sending false — the difference this tool exists to let someone see.
 *
 * AN UNKNOWN YEAR IS A 404, NOT AN EMPTY TABLE, and the page renders it as the refusal it is. An
 * empty table would say "this year has no terms", which is a different fact.
 *
 * THE WARNING IS NOT AN ERROR. A weight total that is not 100 comes back as `warning` and the
 * create still succeeds, because 20/80 to 30/70 passes through 110. The page renders it as a
 * notice beside a successful response, never as a failure.
 */

const BLANK = { name: '', termCode: '', sequence: '', startDate: '', endDate: '', weightPercent: '' }

const TRISTATE = ['', 'true', 'false']
const SORTS = ['', 'sequence', 'sequence,desc', 'name', 'name,desc', 'startDate',
  'startDate,desc', 'endDate', 'createdAt,desc', 'updatedAt,desc']
const SIZES = ['5', '20', '100']

export default function Terms() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const [open, setOpen] = useState(false)
  // The row #3 is editing, or null. Held here rather than in the table so the modal
  // survives a refresh of the list underneath it.
  const [editing, setEditing] = useState(null)

  const [active, setActive] = useState('')
  const [resultsLocked, setResultsLocked] = useState('')
  const [weighted, setWeighted] = useState('')
  const [coversDate, setCoversDate] = useState('')
  // Typed, then SENT. `search` is what the last request used; `typed` is what the box holds.
  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState('20')

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  // Built at render, so the endpoint tag shows the URL that will actually be sent. An empty box
  // sends nothing rather than an empty parameter — `?active=` is not the same as no filter.
  const query = useMemo(() => {
    const out = { page, size }
    if (active) out.active = active
    if (resultsLocked) out.resultsLocked = resultsLocked
    if (weighted) out.weighted = weighted
    if (coversDate) out.coversDate = coversDate
    if (search.trim()) out.search = search.trim()
    if (sort) out.sort = sort
    return out
  }, [page, size, active, resultsLocked, weighted, coversDate, search, sort])

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-academic-terms', {
      label: "The year's terms",
      // Empty when no year is picked, so the request still goes and the server answers 404.
      pathParams: { year: actingAcademicYear ?? '' },
      query,
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, actingAcademicYear, query])

  useEffect(() => { load() }, [load])

  const runSearch = () => { setPage(0); setSearch(typed) }
  const rows = data?.content ?? []

  if (!actingSubdomain) return <NoSchoolChosen what="Terms" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Terms</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {actingAcademicYear ? <> · <span className="mono">{actingAcademicYear}</span></> : null}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add a term</Button>
      </div>

      <Card
        title="Filters"
        description="All five are AND-ed, and blank sends nothing at all — which is not the same as sending false."
        action={<EndpointTag id="list-academic-terms" name="List"
          pathParams={{ year: actingAcademicYear }} query={query} />}
      >
        <div className="stack">
          <div className="toolbar">
            <Field label="Search" wide
              hint="Matches name OR termCode, case-insensitive, anywhere in either. A stray '(' is an empty result, not a 500.">
              <Input value={typed} onChange={(event) => setTyped(event.target.value)}
                onKeyDown={(event) => { if (event.key === 'Enter') runSearch() }}
                placeholder="TERM1, or Semester" />
            </Field>
            <Button icon={Search} onClick={runSearch}>Search</Button>
            <Button onClick={() => { setSearch(''); setTyped(''); setPage(0) }}>Clear</Button>
          </div>

          <div className="field-grid">
            <Field label="Active" hint="Blank returns BOTH — not the same as false.">
              <Select label="Active" value={active}
                onChange={(value) => { setActive(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Results locked" hint="What #5 freezes and #6 releases.">
              <Select label="Results locked" value={resultsLocked}
                onChange={(value) => { setResultsLocked(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Weighted" hint="Asked with exists, so a term with no weightPercent key at all reads as false.">
              <Select label="Weighted" value={weighted}
                onChange={(value) => { setWeighted(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Covers date"
              hint="The term containing this date, both ends inclusive. #10 asks the same of today, in the school's zone.">
              <Input type="date" value={coversDate}
                onChange={(event) => { setCoversDate(event.target.value); setPage(0) }} />
            </Field>
          </div>

          <div className="toolbar">
            <Field label="Sort"
              hint="sequence is unique in the year, so it is a total order. Every other sort gets it appended — a term name is not unique.">
              <Select label="Sort" value={sort}
                onChange={(value) => { setSort(value); setPage(0) }} options={SORTS} />
            </Field>
            <Field label="Page size" hint="Defaults to 20, capped at 100. 0 and 101 are refused, never clamped.">
              <Select label="Page size" value={size}
                onChange={(value) => { setSize(value); setPage(0) }} options={SIZES} />
            </Field>
            <span className="toolbar-spacer" />
            <Button onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <span className="muted">page {page}</span>
            <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </div>
      </Card>

      <Card
        title="Terms"
        description="From #9, in sequence order. The counts describe every term that matched, not this page."
        action={
          <div className="btn-row">
            <Badge>{data?.totalElements ?? 0} matching</Badge>
            <Badge>page {(data?.page ?? 0) + 1} of {data?.totalPages ?? 0}</Badge>
            <Button icon={Plus} onClick={() => setOpen(true)}>Add</Button>
          </div>
        }
      >
        {problem ? (
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={
              problem.bodyJson?.code === 'ACADEMIC_YEAR_NOT_FOUND'
                ? 'That is not a year of this school. #9 refuses an unknown year rather than '
                  + 'answering with an empty page — "no such year" and "no terms" are different facts.'
                : problem.bodyJson?.message || 'Nothing came back.'
            }
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        ) : rows.length === 0 ? (
          <Empty
            title="No terms match"
            description="An empty page, never a 404. Clear the filters to see whether the year has any at all."
            action={
              <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add one</Button>
            }
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Seq</th>
                  <th>Starts</th>
                  <th>Ends</th>
                  <th>Weight</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  <tr key={one.termDocsId}>
                    {/* The stable key, and what six documents in three modules store. */}
                    <td><span className="mono">{one.termCode}</span></td>
                    <td>{one.name}</td>
                    <td>{one.sequence}</td>
                    <td><span className="mono">{one.startDate}</span></td>
                    <td><span className="mono">{one.endDate}</span></td>
                    {/* Absent means the school does not weight, which is a normal school. */}
                    <td>{one.weightPercent != null
                      ? `${one.weightPercent}%`
                      : <span className="muted">unweighted</span>}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                      {one.resultsLocked ? <Badge>results locked</Badge> : null}
                    </td>
                    <td>
                      {/* #3 is addressed by termDocsId, never by termCode. */}
                      <Button icon={Pencil} onClick={() => setEditing(one)}>Edit</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="muted">
          <Info size={12} /> A term is a <b>document</b>, not an embedded row — six documents
          across three modules reference one by <span className="mono">termDocsId</span>. That is
          why it has an id, and why a term can be renamed where a{' '}
          <span className="mono">sectionNo</span> never can.
        </p>
        <p className="muted">
          <Info size={12} /> Sorted by <span className="mono">sequence</span>, which is unique in
          the year. <b>Every other sort ends in it too</b> — the shared page helper appends the
          fallback order to whatever you name, so <span className="mono">?sort=name</span> is
          really <span className="mono">name, sequence</span>. That is what keeps paging stable
          when two terms share a name.
        </p>
      </Card>

      <AddTerm
        open={open}
        year={actingAcademicYear}
        onClose={() => setOpen(false)}
        onAdded={() => load()}
      />

      {/* KEYED BY THE ROW, so picking a different term remounts with its values rather
          than syncing them in an effect. React's own advice, and it deletes a whole class of
          "the modal is showing the term I clicked before" bugs. */}
      {editing ? (
        <EditTerm
          key={editing.termDocsId}
          term={editing}
          year={actingAcademicYear}
          onClose={() => setEditing(null)}
          onSaved={() => load()}
        />
      ) : null}
    </div>
  )
}

/**
 * Editing one term — #3.
 *
 * ADDRESSED BY termDocsId, not by termCode. Six documents across three modules store the id, so
 * that is what identifies a term in a URL — and it is why the code itself is not editable here.
 *
 * EVERY BOX STARTS AT THE STORED VALUE and every one is optional on the wire: a box left exactly
 * as it was sends nothing, so a rename really is a rename and does not quietly rewrite the dates
 * back to themselves. That is also what makes NOTHING_TO_UPDATE reachable — close and reopen,
 * change nothing, Save.
 *
 * termCode AND sequence ARE SHOWN AND DISABLED rather than hidden, because "why can I not edit
 * this" is the question the endpoint exists to answer.
 */
function EditTerm({ term, year, onClose, onSaved }) {
  const { call } = useApi()
  // Seeded once, at mount. The parent keys this component by termDocsId, so a different
  // row is a different component and gets its own seed — no effect has to keep them in step.
  const [form, setForm] = useState(() => ({
    name: term.name ?? '',
    startDate: term.startDate ?? '',
    endDate: term.endDate ?? '',
    weightPercent: term.weightPercent != null ? String(term.weightPercent) : '',
  }))
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(null)

  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))

  // ONLY WHAT CHANGED. An unchanged box sends nothing, because absent means "leave it alone"
  // and sending every field would make every save a full overwrite.
  const body = (() => {
    const out = {}
    if (form.name !== (term.name ?? '')) out.name = form.name
    if (form.startDate !== (term.startDate ?? '')) out.startDate = form.startDate
    if (form.endDate !== (term.endDate ?? '')) out.endDate = form.endDate
    const storedWeight = term.weightPercent != null ? String(term.weightPercent) : ''
    if (form.weightPercent !== storedWeight) {
      // "" is not a clear — #3 has no clear. It is simply not sent, and the modal says so.
      if (form.weightPercent !== '') out.weightPercent = Number(form.weightPercent)
    }
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('update-academic-term', {
      label: 'Update a term',
      pathParams: { year: year ?? '', termId: term.termDocsId },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setSaved(result.bodyJson)
      onSaved(result.bodyJson)
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
      open
      onClose={onClose}
      preview={body}
      title={`Edit ${term.termCode}`}
      description="Name, dates and weight. Not the code and not the order — a reorder is #4, because sequence is unique per year."
      endpoint={<EndpointTag id="update-academic-term" name="Save" look="primary"
        pathParams={{ year, termId: term.termDocsId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Save</Button>
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

        {saved ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">saved</span>
            </div>
            <pre className="resp-body">{saved.nextStep}</pre>
          </div>
        ) : null}

        {/* A warning rides on a SUCCESSFUL response. #3 reports a broken weight total and
            never refuses it — 20/80 to 30/70 is only reachable through 110. */}
        {saved?.warning ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">saved, with a warning</span>
            </div>
            <pre className="resp-body">{saved.warning}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Code"
            hint="Not editable. A code edit orphans nothing — which is what makes it dangerous: every report naming the old code stops matching, silently."
          >
            <Input value={term.termCode} disabled readOnly />
          </Field>
          <Field
            label="Sequence"
            hint="Not editable here. It is unique per year, so term 1 becoming 2 while term 2 is still 2 hits the index halfway through. That is #4."
          >
            <Input value={String(term.sequence ?? '')} disabled readOnly />
          </Field>
        </div>

        <Field
          label="Name"
          hint="Safe to change: consumers store termDocsId and report cards snapshot the name. Clearing it is a 400, not a removal."
          error={errors.name}
        >
          <Input value={form.name} error={errors.name}
            onChange={set('name')} placeholder="Term 1" />
        </Field>

        <div className="field-grid">
          <Field
            label="Starts"
            hint="Send one date alone and the other is kept — then both are checked together."
            error={errors.startDate}
          >
            <Input type="date" value={form.startDate} error={errors.startDate}
              onChange={set('startDate')} />
          </Field>
          <Field
            label="Ends"
            hint="A start moved past an untouched end is a 400, which a per-field check would wave through."
            error={errors.endDate}
          >
            <Input type="date" value={form.endDate} error={errors.endDate}
              onChange={set('endDate')} />
          </Field>
        </div>

        <Field
          label="Weight percent"
          hint="Cannot be cleared here — emptying the box sends nothing. Removing one weight is only legal as part of removing them all, which is #2."
          error={errors.weightPercent}
        >
          <Input type="number" value={form.weightPercent} error={errors.weightPercent}
            onChange={set('weightPercent')} placeholder="20" />
        </Field>

        <p className="muted">
          <Info size={12} /> A broken weight total comes back as a <b>warning on a successful
          save</b>, never a refusal: 20/80 becomes 30/70 in two calls and the first one sits at
          110. #1 refuses an excess because a create only adds, #2 requires exactly 100 because
          it sees every row, and #3 can do neither.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Adding a term — #1.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD, because a year gets its terms in one sitting. The sequence
 * is bumped and the name, code and dates cleared — only the sequence carries over.
 *
 * EVERY BOX IS FREE TEXT AND NOTHING IS DISABLED, so every refusal stays reachable — an inverted
 * range, a sequence already taken, a weight of 101, dates outside the year.
 */
function AddTerm({ open, year, onClose, onAdded }) {
  const { call } = useApi()
  const [form, setForm] = useState(BLANK)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))

  // An empty optional box sends nothing rather than "", which the API would read as a value.
  const body = (() => {
    const out = {
      name: form.name,
      termCode: form.termCode,
      startDate: form.startDate,
      endDate: form.endDate,
    }
    if (form.sequence !== '') out.sequence = Number(form.sequence)
    if (form.weightPercent !== '') out.weightPercent = Number(form.weightPercent)
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-academic-term', {
      label: 'Add a term',
      pathParams: { year: year ?? '' },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setMade(result.bodyJson)
      onAdded(result.bodyJson)
      setForm((old) => ({
        ...old,
        name: '',
        termCode: '',
        startDate: '',
        endDate: '',
        sequence: old.sequence === '' ? '' : String(Number(old.sequence) + 1),
      }))
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
      title="Add a term"
      description="The code is given, not derived from the name: it is what records reference, so it outlives a rename. A term must fit inside the year without overlapping another."
      endpoint={<EndpointTag id="create-academic-term" name="Add" look="primary"
        pathParams={{ year }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Add</Button>
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

        {made ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">
                {made.termCode} · seq {made.sequence}
              </span>
            </div>
            <pre className="resp-body">{made.nextStep}</pre>
          </div>
        ) : null}

        {/* A warning rides on a SUCCESSFUL response. Rendered apart from the refusal above so it
            never reads as a failure — the term was created. */}
        {made?.warning ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">created, with a warning</span>
            </div>
            <pre className="resp-body">{made.warning}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Name"
            required
            hint="What a report card prints. Renameable later — records hold the code, not this."
            error={errors.name}
          >
            <Input value={form.name} error={errors.name}
              onChange={set('name')} placeholder="Term 1" />
          </Field>
          <Field
            label="Code"
            required
            hint="Uppercase letters and digits only — TERM1, no underscore. Unique in the year, and a retired term still holds its own. Never changes once records reference it."
            error={errors.termCode}
          >
            <Input value={form.termCode} error={errors.termCode}
              onChange={set('termCode')} placeholder="TERM1" />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Starts"
            required
            hint="Inclusive, and must fall inside the academic year."
            error={errors.startDate}
          >
            <Input type="date" value={form.startDate} error={errors.startDate}
              onChange={set('startDate')} />
          </Field>
          <Field
            label="Ends"
            required
            hint="Inclusive — a term ending 30 September includes the 30th. Before the start is a 400."
            error={errors.endDate}
          >
            <Input type="date" value={form.endDate} error={errors.endDate}
              onChange={set('endDate')} />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Sequence"
            required
            hint="Order inside the year, unique within it. A retired term still holds its number."
            error={errors.sequence}
          >
            <Input type="number" value={form.sequence} error={errors.sequence}
              onChange={set('sequence')} placeholder="1" />
          </Field>
          <Field
            label="Weight percent"
            hint="Optional, 0 to 100. Blank means this school does not weight the annual result — a normal school. Weighting one term and not another is refused, and so is a weight that takes the year past 100%. Under 100 is only a warning."
            error={errors.weightPercent}
          >
            <Input type="number" value={form.weightPercent} error={errors.weightPercent}
              onChange={set('weightPercent')} placeholder="blank if you do not weight" />
          </Field>
        </div>

        <p className="muted">
          <Info size={12} /> Terms may not overlap, but <b>adjacency is not overlap</b> — one
          ending 30 September and the next starting 1 October are fine. A retired term keeps its
          code and its sequence and releases its dates.
        </p>
      </div>
    </Modal>
  )
}
