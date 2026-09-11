import { useCallback, useEffect, useMemo, useState } from 'react'
import { Eye, Info, Layers, Pencil, Plus, RefreshCw, Search, Users } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * School / Academics — the classes taught in one academic year. Two of the group's 36 endpoints.
 *
 * SIX ENDPOINTS: #12 create, #13 edit, #17 add a section, #28 list, #29 read one, #30 read its
 * sections.
 *
 * THE SECTIONS PANEL NOW READS BEFORE IT WRITES. While #30 was unbuilt it could only show what
 * the last add returned, so a reopened panel looked empty on a class that had four sections. It
 * loads #30 on open, which is also how ?active= is reachable.
 *
 * #17 IS THE ONE THAT UNBLOCKED ANOTHER MODULE. A student record stores sectionNo, so nothing
 * could be placed in a class until a section existed. The Sections button on each row is how it
 * is reached, and the count in the row is how you see it worked. The table is now a real read of the server
 * rather than the session record it was while #28 was unbuilt — so a refresh keeps it, and a
 * class created in another tab appears.
 *
 * EVERY FILTER IS ON SCREEN, INCLUDING THE ONES THAT WILL BE REFUSED. The page-size list carries
 * 101 and 0, and the sort list carries a field off the allow-list, so their 400s can be seen.
 * Nothing is disabled — this is an API testing tool.
 *
 * SEARCH IS SERVER-SIDE AND HAS ITS OWN BUTTON. #28 filters by name in the database, so the box
 * does not filter the page in the browser: it re-asks. A box that narrowed what was already
 * fetched would silently disagree with `totalElements` and would never find a class on page 3.
 *
 * A CLASS IS ADDRESSED BY ITS ID. Twelve other documents store `classDocsId`, and not one stores
 * a class code — so the id is what comes back, what the Location header carries, and what #13 to
 * #16 will take in their URLs. `sectionNo` and `subjectCode` stay codes because they are embedded
 * and have no id to be referenced by.
 *
 * THE YEAR IS A PATH PARAMETER, so this screen needs one chosen. It uses the year already picked
 * in the top bar rather than asking again — the same year every other school screen is acting on.
 * WITH NO YEAR PICKED THE BUTTON STILL WORKS: the API tester must be able to send the request
 * that fails, so the year field is left empty and the 404 comes from the server.
 */
/**
 * What #28 may sort on, plus one it refuses on purpose.
 *
 * `sections` is off the allow-list — it is here so the 400 can be seen rather than only read
 * about.
 */
const SORTS = [
  '', 'name,asc', 'name,desc', 'createdAt,desc', 'updatedAt,desc',
  'sections',
]

/** 101 is over the cap and 0 is under it. Both are refused, never clamped. */
const SIZES = ['5', '20', '100', '101', '0']

/** Three-state: '' means the filter is not sent at all, which is not the same as false. */
const TRISTATE = ['', 'true', 'false']

export default function Classes() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()

  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState(null)
  const [sectioning, setSectioning] = useState(null)
  const [viewing, setViewing] = useState(null)

  const [active, setActive] = useState('')
  const [hasSections, setHasSections] = useState('')
  const [hasSubjects, setHasSubjects] = useState('')
  const [programme, setProgramme] = useState('')
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
    if (hasSections) out.hasSections = hasSections
    if (hasSubjects) out.hasSubjects = hasSubjects
    if (programme.trim()) out.affiliationProgrammeDocsId = programme.trim()
    if (search.trim()) out.search = search.trim()
    if (sort) out.sort = sort
    return out
  }, [page, size, active, hasSections, hasSubjects, programme, search, sort])

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-school-classes', {
      label: "The year's classes",
      // Empty when no year is picked, so the request still goes and the server answers 404.
      pathParams: { year: actingAcademicYear ?? '' },
      query,
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, actingAcademicYear, query])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Classes" />

  const rows = data?.content ?? []
  /** Re-ask with what the box holds. Server-side, so it is a new request and page 0. */
  const runSearch = () => { setPage(0); setSearch(typed) }

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Classes</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {actingAcademicYear
              ? <> · <span className="mono">{actingAcademicYear}</span></>
              : ' · no year picked — the list will answer 404'}
            {data ? ` · ${data.totalElements} in this year · page ${data.page + 1} of ${Math.max(data.totalPages, 1)}` : ''}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>Add a class</Button>
      </div>

      <Card
        title="Filters"
        description="Every one is optional and they AND together. Blank means the parameter is not sent at all, which is not the same as false."
        action={<EndpointTag id="list-school-classes" name="As filtered" query={query} />}
      >
        <div className="stack">
          <div className="toolbar">
            <Field label="Search by name" hint="Server-side and case-insensitive; matches anywhere. Press the button — it re-asks rather than filtering this page.">
              <Input
                value={typed}
                onChange={(event) => setTyped(event.target.value)}
                onKeyDown={(event) => { if (event.key === 'Enter') runSearch() }}
                placeholder="grade"
              />
            </Field>
            <Button icon={Search} onClick={runSearch}>Search</Button>
            {search ? (
              <Button onClick={() => { setSearch(''); setTyped(''); setPage(0) }}>Clear</Button>
            ) : null}
          </div>

          <div className="field-grid">
            <Field label="Active" hint="Blank returns both.">
              <Select label="Active" value={active}
                onChange={(value) => { setActive(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Has sections" hint="false is THE SETUP CHECKLIST — a class with no section cannot hold a student.">
              <Select label="Has sections" value={hasSections}
                onChange={(value) => { setHasSections(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Has subjects" hint="false is 'nothing is taught in this class yet'.">
              <Select label="Has subjects" value={hasSubjects}
                onChange={(value) => { setHasSubjects(value); setPage(0) }} options={TRISTATE} />
            </Field>
          </div>

          <div className="field-grid">
            <Field label="Affiliation programme id" hint="An unknown id is an empty page, not a 404.">
              <Input value={programme}
                onChange={(event) => { setProgramme(event.target.value); setPage(0) }}
                placeholder="6aa2a107c7cc53f3111217bf" />
            </Field>
            <Field label="Sort" hint="`sections` is off the allow-list on purpose — it answers 400 INVALID_SORT_FIELD.">
              <Select label="Sort" value={sort}
                onChange={(value) => { setSort(value); setPage(0) }} options={SORTS} />
            </Field>
          </div>

          <div className="toolbar">
            <Field label="Page size" hint="Defaults to 20, capped at 100. 101 and 0 are refused, never clamped.">
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
        title="The year's classes"
        description="In name order, as the API returns them. A class carries no school-defined position, so alphabetical is all there is: Grade 10 comes before Grade 2."
      >
        {problem ? (
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message || 'Nothing came back.'}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        ) : rows.length === 0 && !loading ? (
          <Empty
            title={Object.keys(query).length > 2 ? 'Nothing matches those filters' : 'No classes yet'}
            description={Object.keys(query).length > 2
              ? 'An empty page, not a 404 — the year exists and the filters matched nothing.'
              : 'A class is created empty. Sections (#17) and subjects (#22) go on afterwards, and neither is built.'}
            action={<Button look="primary" icon={Plus} onClick={() => setCreating(true)}>Create one</Button>}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Sections</th>
                  <th>Subjects</th>
                  <th>Programme</th>
                  <th>Id</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.schoolClassId}>
                    <td>
                      {row.name}
                      {row.active ? <Badge tone="good">active</Badge> : <Badge>retired</Badge>}
                    </td>
                    {/* Both are 0 until #17 and #22 exist. The list returns COUNTS, never the
                        embedded rows: 168 of them for a twelve-class year. */}
                    <td>{row.sectionCount}</td>
                    <td>{row.subjectCount}</td>
                    <td>
                      {row.affiliationProgrammeDocsId
                        ? <span className="mono">{row.affiliationProgrammeDocsId.slice(0, 8)}…</span>
                        : <span className="muted">none</span>}
                    </td>
                    {/* The identity. What twelve other documents store as classDocsId, and what
                        #13 addresses — there is no code to show instead. */}
                    <td><span className="mono">{row.schoolClassId}</span></td>
                    <td>
                      <div className="btn-row">
                        <Button icon={Pencil} onClick={() => setEditing(row)}>Edit</Button>
                        {/* #17. The section count beside it is how you see the add worked —
                            there is no section read yet, #30 being unbuilt. */}
                        {/* #29 — one class in full, which is the only way to see its subjects. */}
                        <Button icon={Eye} onClick={() => setViewing(row)}>View</Button>
                        <Button icon={Users} onClick={() => setSectioning(row)}>Sections</Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="muted">
          <Info size={12} /> Rows carry counts, not the embedded lists — <span className="mono">GET
          /classes/{'{id}'}</span> is #29 and is not built. A suspended school can still read this
          list and cannot add to it.
        </p>
      </Card>

      <ViewClass
        row={viewing}
        onClose={() => setViewing(null)}
      />

      <AddSection
        row={sectioning}
        onClose={() => setSectioning(null)}
        onAdded={() => load()}
      />

      <EditClass
        row={editing}
        onClose={() => setEditing(null)}
        onSaved={() => { setEditing(null); load() }}
      />

      <CreateClass
        open={creating}
        year={actingAcademicYear}
        onClose={() => setCreating(false)}
        onCreated={() => { setCreating(false); load() }}
      />
    </div>
  )
}

/**
 * The create form.
 *
 * THE BODY CARRIES THREE FIELDS AND NO CODE. An earlier draft of this endpoint derived a
 * `classCode` from the name; it was removed the same day it was added, once a count showed twelve
 * documents already referencing a class by id and none by a code.
 */
function CreateClass({ open, year, onClose, onCreated }) {
  const { call } = useApi()
  const [form, setForm] = useState({ name: '', affiliationProgrammeDocsId: '' })
  // null means "whatever the top bar is on". Typing takes over, and clearing the box sends an
  // empty year on purpose — that is how the 404 is tested without changing the top bar.
  // Derived during render rather than copied in an effect, so there is no stale first paint.
  const [yearOverride, setYearOverride] = useState(null)
  const sendYear = yearOverride ?? year ?? ''
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  // Kit's Input is a passthrough to a plain <input>, so onChange hands over the EVENT, not the
  // value. Unwrapping here is the idiom every other screen in this app uses; taking `value`
  // directly stored the whole React event object as the field and sent it as the name.
  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    // An empty optional field is dropped rather than sent as "", which the API would read as
    // an instruction to clear rather than as "not provided".
    const body = { name: form.name }
    if (form.affiliationProgrammeDocsId.trim() !== '') {
      body.affiliationProgrammeDocsId = form.affiliationProgrammeDocsId.trim()
    }
    const result = await call('create-school-class', {
      label: 'Add a class',
      // Empty when no year is picked, so the request still goes and the server answers 404.
      // Blocking it here would hide the refusal this tool exists to show.
      pathParams: { year: sendYear },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setForm({ name: '', affiliationProgrammeDocsId: '' })
      // The year is NOT reset: making several classes in one year is the normal next action.
      onCreated(result.bodyJson)
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
      preview={form}
      title="Add a class"
      description="Created empty. Sections and subjects go on afterwards, through endpoints that are not built yet."
      endpoint={<EndpointTag id="create-school-class" name="Create" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button look="primary" busy={saving} onClick={submit}>Create</Button>
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
          label="Year"
          hint="A path parameter, not a body field. Starts from the top bar; type over it to aim at another year, or clear it to see the 404."
        >
          <Input
            value={sendYear}
            onChange={(event) => setYearOverride(event.target.value)}
            placeholder="no year picked — this will 404"
          />
        </Field>

        <Field
          label="Name"
          required
          hint="Unique within the year, and editable later — nothing joins on it."
          error={errors.name}
        >
          <Input value={form.name} error={errors.name} onChange={set('name')} placeholder="Grade 7" />
        </Field>

        <div className="field-grid">
          <Field
            label="Affiliation programme id"
            hint="Optional. Checked against this school — another school's real id is a 404."
            error={errors.affiliationProgrammeDocsId}
          >
            <Input
              value={form.affiliationProgrammeDocsId}
              error={errors.affiliationProgrammeDocsId}
              onChange={set('affiliationProgrammeDocsId')}
              placeholder="6aa2a107c7cc53f3111217bf"
            />
          </Field>
        </div>

        <p className="muted">
          <Layers size={12} /> The name is unique per <em>year</em>, not per school — "Grade 7" can
          exist in 2026-2027 and 2027-2028 at once.
        </p>
      </div>
    </Modal>
  )
}

/**
 * The edit form — #13.
 *
 * THE ROW IS PASSED IN AND THE FORM STARTS FROM IT, so the fields show what the class currently
 * is rather than empty boxes that would blank the class if submitted. It is keyed on the class id
 * below, which remounts the form when a different row is opened — the alternative is copying the
 * row into state in an effect, which paints the previous class for one frame.
 *
 * ONLY WHAT CHANGED IS SENT. Every field is optional on the request and absent means "leave it
 * alone", so sending all three every time would make "clear the programme" indistinguishable from
 * "do not touch it". The diff is the whole reason this screen can offer a detach at all.
 */
function EditClass({ row, onClose, onSaved }) {
  if (!row) return null
  return (
    <EditForm key={row.schoolClassId} row={row} onClose={onClose} onSaved={onSaved} />
  )
}

function EditForm({ row, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    name: row.name ?? '',
    affiliationProgrammeDocsId: row.affiliationProgrammeDocsId ?? '',
  })
  // What the PATCH is AIMED at, as opposed to what it sends. Both start from the row and both
  // stay typeable: a bogus id is how CLASS_NOT_FOUND is reached, and another year is how case 10
  // — a real id under the wrong year — is reached. Locking either would put two of this
  // endpoint's documented refusals out of the screen's reach.
  const [target, setTarget] = useState({ year: row.academicYear, id: row.schoolClassId })
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  // Kit's Input passes the EVENT through to a plain <input>, so it is unwrapped here.
  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))
  const aim = (field) => (event) =>
    setTarget((old) => ({ ...old, [field]: event.target.value }))

  // Only what the user actually changed. `""` on the programme is a real value — it is how the
  // API is told to detach one — so it is sent when it differs from what the row had.
  const changed = () => {
    const body = {}
    if (form.name !== (row.name ?? '')) body.name = form.name
    if (form.affiliationProgrammeDocsId !== (row.affiliationProgrammeDocsId ?? '')) {
      body.affiliationProgrammeDocsId = form.affiliationProgrammeDocsId.trim()
    }
    return body
  }

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    // An unchanged form sends {} on purpose, so the 400 NOTHING_TO_UPDATE stays reachable
    // rather than being hidden behind a disabled button.
    const result = await call('update-school-class', {
      label: 'Edit a class',
      pathParams: { year: target.year, id: target.id },
      body: changed(),
    })
    setSaving(false)
    if (result.ok) {
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

  const body = changed()

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      title={`Edit ${row.name}`}
      description="Only the fields you change are sent. Nothing structural is reachable from here."
      endpoint={<EndpointTag id="update-school-class" name="Update" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
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

        <div className="field-grid">
          <Field
            label="Year"
            hint="Scopes the lookup. Point it at another year and the same id answers 404 — the class belongs to one year."
          >
            <Input value={target.year} onChange={aim('year')} />
          </Field>
          <Field
            label="Class id"
            hint="What the PATCH addresses. There is no code — this is the identity. Change it to reach CLASS_NOT_FOUND."
          >
            <Input value={target.id} onChange={aim('id')} />
          </Field>
        </div>

        <Field
          label="Name"
          hint="Editable, unlike an academic year's. It only has to stay unique inside the year — and it may stay exactly as it is."
          error={errors.name}
        >
          <Input value={form.name} error={errors.name} onChange={set('name')} />
        </Field>

        <div className="field-grid">
          <Field
            label="Affiliation programme id"
            hint='Empty it to detach — that sends "", which is the only clear this endpoint has.'
            error={errors.affiliationProgrammeDocsId}
          >
            <Input
              value={form.affiliationProgrammeDocsId}
              error={errors.affiliationProgrammeDocsId}
              onChange={set('affiliationProgrammeDocsId')}
            />
          </Field>
        </div>

        {Object.keys(body).length === 0 ? (
          <p className="muted">
            <Info size={12} /> Nothing has changed, so Save sends <span className="mono">{'{}'}</span>
            {' '}and the API answers <span className="mono">400 NOTHING_TO_UPDATE</span>. That is
            left reachable on purpose.
          </p>
        ) : null}
      </div>
    </Modal>
  )
}

/**
 * Adding a section — #17.
 *
 * KEYED ON THE CLASS so it remounts per row rather than copying the row into state in an effect,
 * which would paint the previous class for one frame.
 *
 * THE MODAL STAYS OPEN AFTER A SUCCESSFUL ADD, and that is deliberate: a class gets A, B, C and D
 * in one sitting, and closing after each one would mean reopening three times. The response is
 * the class's whole section list, so the panel shows what the class now has.
 */
function AddSection({ row, onClose, onAdded }) {
  if (!row) return null
  return <AddSectionForm key={row.schoolClassId} row={row} onClose={onClose} onAdded={onAdded} />
}

function AddSectionForm({ row, onClose, onAdded }) {
  const { call } = useApi()
  const [form, setForm] = useState({ sectionNo: '', capacity: '', classTeacherDocsId: '' })
  // Both halves of the URL stay typeable, so CLASS_NOT_FOUND and the wrong-year 404 are
  // reachable from the screen rather than only from the docs.
  const [target, setTarget] = useState({ year: row.academicYear, id: row.schoolClassId })
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [refusedRead, setRefusedRead] = useState(null)
  const [saving, setSaving] = useState(false)
  const [list, setList] = useState(null)
  // The filter #30 takes. '' means the parameter is not sent, which is not the same as false.
  const [active, setActive] = useState('')

  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))
  const aim = (field) => (event) =>
    setTarget((old) => ({ ...old, [field]: event.target.value }))

  // #30. Loaded on open and after every add, so the panel shows the server's answer rather than
  // only what the last write happened to return.
  const read = useCallback(async () => {
    const result = await call('list-class-sections', {
      label: "The class's sections",
      pathParams: { year: target.year, id: target.id },
      query: active ? { active } : {},
    })
    if (result.ok) setList(result.bodyJson)
    else setRefusedRead(result.bodyJson ?? { code: `HTTP ${result.status}` })
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, target.year, target.id, active])

  useEffect(() => { read() }, [read])

  const body = (() => {
    const out = { sectionNo: form.sectionNo }
    if (form.capacity !== '') out.capacity = Number(form.capacity)
    if (form.classTeacherDocsId.trim() !== '') {
      out.classTeacherDocsId = form.classTeacherDocsId.trim()
    }
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('add-class-section', {
      label: 'Add a section',
      pathParams: { year: target.year, id: target.id },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setList(result.bodyJson)
      // Only the number is cleared: capacity and the teacher usually repeat across a class's
      // sections, so retyping them for B, C and D would be the wrong default.
      setForm((old) => ({ ...old, sectionNo: '' }))
      onAdded()
      // Re-read, so the panel reflects the filter rather than the write's unfiltered answer.
      read()
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
      title={`Sections of ${row.name}`}
      description="A section is embedded in its class, so the response is the class's whole list. sectionNo can never be changed once records reference it."
      endpoint={<EndpointTag id="add-class-section" name="Add" look="primary" />}
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

        {refusedRead ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refusedRead.code}</span>
            </div>
            <pre className="resp-body">{refusedRead.message}</pre>
          </div>
        ) : null}

        {/* #30's answer. The counts describe the WHOLE class however the rows are filtered. */}
        {list ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">
                {list.sectionCount} section{list.sectionCount === 1 ? '' : 's'} ·{' '}
                {list.activeCount} active
                {active ? ` · showing ${(list.sections ?? []).length}` : ''}
              </span>
            </div>
            <pre className="resp-body">
              {(list.sections ?? []).map((one) => [
                one.sectionNo,
                one.capacity ? `cap ${one.capacity}` : 'no capacity',
                one.classTeacherDocsId ? 'has a class teacher' : 'no class teacher',
                one.active ? 'active' : 'retired',
              ].join('  ·  ')).join('\n')}
            </pre>
          </div>
        ) : null}

        <div className="toolbar">
          <Field label="Show" hint="#30's ?active=. Blank sends no parameter, which is not the same as false.">
            <Select label="Show" value={active} onChange={setActive} options={TRISTATE} />
          </Field>
          <span className="toolbar-spacer" />
          <Button icon={RefreshCw} onClick={read}>Re-read</Button>
          <EndpointTag id="list-class-sections" name="Read" />
        </div>

        <div className="field-grid">
          <Field label="Year" hint="Scopes the lookup. Point it elsewhere and the same class id is a 404.">
            <Input value={target.year} onChange={aim('year')} />
          </Field>
          <Field label="Class id" hint="Change it to reach CLASS_NOT_FOUND.">
            <Input value={target.id} onChange={aim('id')} />
          </Field>
        </div>

        <Field
          label="Section number"
          required
          hint="Stored exactly as typed — A, Blue, Alpha. Unique in the class, checked case-insensitively, and never changeable afterwards."
          error={errors.sectionNo}
        >
          <Input value={form.sectionNo} error={errors.sectionNo}
            onChange={set('sectionNo')} placeholder="A" />
        </Field>

        <div className="field-grid">
          <Field
            label="Capacity"
            hint="Optional, at least 1. A plan, not a limit — nothing enforces it, and 0 is refused."
            error={errors.capacity}
          >
            <Input type="number" value={form.capacity} error={errors.capacity}
              onChange={set('capacity')} placeholder="40" />
          </Field>
          <Field
            label="Class teacher id"
            hint="Optional Staff.id. Checked against this school — another school's real id is a 404."
            error={errors.classTeacherDocsId}
          >
            <Input value={form.classTeacherDocsId} error={errors.classTeacherDocsId}
              onChange={set('classTeacherDocsId')} placeholder="67aa15d9dc3f7d0011111111" />
          </Field>
        </div>

        <p className="muted">
          <Users size={12} /> The number stays unique only within this class — another class may
          have its own "A". Add stays open so a class can get A, B, C and D in one sitting. The
          counts above describe the whole class even when the rows are filtered.
        </p>
      </div>
    </Modal>
  )
}

/**
 * One class in full — #29.
 *
 * THE ONLY WAY TO SEE A CLASS'S SUBJECTS. #28 returns counts and #30 returns sections, so this is
 * the endpoint that shows what is actually taught — and while #22 is unbuilt, it shows that the
 * answer is nothing, which is itself worth seeing.
 *
 * NOTHING IS RESOLVED TO A NAME. Every teacher and grading scheme is a raw id, and the panel
 * prints them as such rather than pretending otherwise.
 */
function ViewClass({ row, onClose }) {
  if (!row) return null
  return <ViewClassPanel key={row.schoolClassId} row={row} onClose={onClose} />
}

function ViewClassPanel({ row, onClose }) {
  const { call } = useApi()
  const [target, setTarget] = useState({ year: row.academicYear, id: row.schoolClassId })
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)

  const aim = (field) => (event) =>
    setTarget((old) => ({ ...old, [field]: event.target.value }))

  const read = useCallback(async () => {
    const result = await call('get-school-class', {
      label: 'One class in full',
      pathParams: { year: target.year, id: target.id },
    })
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, target.year, target.id])

  useEffect(() => { read() }, [read])

  return (
    <Modal
      open
      onClose={onClose}
      title={row.name}
      description="One document, one query — which is the whole reason sections and subjects are embedded."
      endpoint={<EndpointTag id="get-school-class" name="Read" />}
      footer={
        <>
          <Button icon={RefreshCw} onClick={read}>Re-read</Button>
          <Button onClick={onClose}>Close</Button>
        </>
      }
    >
      <div className="stack">
        {problem ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code || `HTTP ${problem.status}`}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message}</pre>
          </div>
        ) : null}

        {/* Both halves of the URL stay typeable, so the two 404s are reachable from here. */}
        <div className="field-grid">
          <Field label="Year" hint="A real class id under the wrong year is a 404.">
            <Input value={target.year} onChange={aim('year')} />
          </Field>
          <Field label="Class id" hint="Change it to reach CLASS_NOT_FOUND.">
            <Input value={target.id} onChange={aim('id')} />
          </Field>
        </div>

        {data ? (
          <>
            <div className="resp">
              <div className="resp-head">
                <span className="resp-status" data-ok="true">
                  {data.sectionCount} section{data.sectionCount === 1 ? '' : 's'} ·{' '}
                  {data.activeSectionCount} active · {data.subjectCount} subject
                  {data.subjectCount === 1 ? '' : 's'} · {data.activeSubjectCount} active
                </span>
              </div>
              <pre className="resp-body">
                {[
                  `name        ${data.name}`,
                  `year        ${data.academicYear}`,
                  `active      ${data.active}`,
                  `programme   ${data.affiliationProgrammeDocsId ?? 'none'}`,
                  `id          ${data.schoolClassId}`,
                ].join('\n')}
              </pre>
            </div>

            <div className="resp">
              <div className="resp-head"><span className="resp-status">sections</span></div>
              <pre className="resp-body">
                {(data.sections ?? []).length === 0
                  ? 'none — add one with #17, or nothing can be placed in this class'
                  : data.sections.map((one) => [
                      one.sectionNo,
                      one.capacity ? `cap ${one.capacity}` : 'no capacity',
                      // The raw id, not a name. See the note on the record.
                      one.classTeacherDocsId ?? 'no class teacher',
                      one.active ? 'active' : 'retired',
                    ].join('  ·  ')).join('\n')}
              </pre>
            </div>

            <div className="resp">
              <div className="resp-head"><span className="resp-status">subjects</span></div>
              <pre className="resp-body">
                {(data.subjects ?? []).length === 0
                  ? 'none — #22 is not built, so every class reads this way'
                  : data.subjects.map((one) => [
                      one.subjectCode,
                      one.name,
                      one.subjectType,
                      // Absent means the whole class, which is the ordinary case.
                      one.sectionNo ? `section ${one.sectionNo}` : 'all sections',
                      `${(one.teacherDocsIds ?? []).length} teacher(s)`,
                      one.active ? 'active' : 'retired',
                    ].join('  ·  ')).join('\n')}
              </pre>
            </div>
          </>
        ) : null}

        <p className="muted">
          <Info size={12} /> Teachers and grading schemes are raw ids on purpose — one place
          should decide how a teacher is presented, and it is not two response records.
        </p>
      </div>
    </Modal>
  )
}
