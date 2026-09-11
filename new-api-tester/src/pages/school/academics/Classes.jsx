import { useState } from 'react'
import { Info, Layers, Pencil, Plus } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * School / Academics — the classes taught in one academic year. Two of the group's 36 endpoints.
 *
 * #12 AND #13 ARE BUILT, AND THERE IS STILL NO LIST. `GET /classes` is #28 and does not exist yet, which
 * means this screen cannot read back what it made: it keeps what each create returned and says
 * plainly that this is a session record, not the server's answer. Showing a table that looks like
 * a list of the school's classes, built from local state, would be a screen that lies after a
 * refresh.
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
export default function Classes() {
  const { actingSubdomain, actingAcademicYear } = useApiState()
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState(null)
  const [made, setMade] = useState([])

  if (!actingSubdomain) return <NoSchoolChosen what="Classes" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Classes</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {actingAcademicYear
              ? <> · <span className="mono">{actingAcademicYear}</span></>
              : ' · no year picked — the create will answer 404'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>Add a class</Button>
      </div>

      <Card
        title="Made in this session"
        description="#12 is the only built endpoint in this group, so there is nothing to read back."
        action={<EndpointTag id="create-school-class" name="Create" />}
      >
        {made.length === 0 ? (
          <Empty
            title="Nothing created yet"
            description="A class is created empty — sections (#17) and subjects (#22) go on afterwards, and neither is built."
            action={
              <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>
                Create one
              </Button>
            }
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Year</th>
                  <th>Order</th>
                  <th>Sections</th>
                  <th>Subjects</th>
                  <th>Id</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {made.map((row) => (
                  <tr key={row.schoolClassId}>
                    <td>
                      {row.name}
                      {row.active ? <Badge tone="good">active</Badge> : null}
                    </td>
                    <td><span className="mono">{row.academicYear}</span></td>
                    <td>{row.displayOrder ?? <span className="muted">last</span>}</td>
                    {/* Both are always 0 here, and that is the point rather than an oversight:
                        #12 cannot accept sections or subjects. */}
                    <td>{row.sectionCount}</td>
                    <td>{row.subjectCount}</td>
                    {/* The identity. What the Location header returned and what #13 to #16 will
                        address it by — there is no code to show instead. */}
                    <td><span className="mono">{row.schoolClassId}</span></td>
                    <td>
                      {/* #13. The id in the row is exactly what the PATCH addresses — there is
                          no code, so this button is the only way to reach that endpoint. */}
                      <Button icon={Pencil} onClick={() => setEditing(row)}>Edit</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="muted">
          <Info size={12} /> This table is what this browser session created, not a read of the
          server. <span className="mono">GET /classes</span> is #28 and is not built, so a refresh
          empties it.
        </p>
      </Card>

      <EditClass
        row={editing}
        onClose={() => setEditing(null)}
        onSaved={(saved) => {
          // Replaces the row by id, which is stable across a rename. Keying on the name would
          // lose the row the moment #13 did the one thing it exists for.
          setMade((rows) => rows.map((r) => (r.schoolClassId === saved.schoolClassId ? saved : r)))
          setEditing(null)
        }}
      />

      <CreateClass
        open={creating}
        year={actingAcademicYear}
        onClose={() => setCreating(false)}
        onCreated={(row) => {
          setMade((rows) => [row, ...rows])
          setCreating(false)
        }}
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
  const [form, setForm] = useState({ name: '', displayOrder: '', affiliationProgrammeDocsId: '' })
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
    // Empty optional fields are dropped rather than sent as "". displayOrder is a number on the
    // request, and "" would be a type error before the server ever read it.
    const body = { name: form.name }
    if (form.displayOrder !== '') body.displayOrder = Number(form.displayOrder)
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
      setForm({ name: '', displayOrder: '', affiliationProgrammeDocsId: '' })
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
            label="Sort order"
            hint="Optional. Absent sorts last."
            error={errors.displayOrder}
          >
            <Input
              type="number"
              value={form.displayOrder}
              error={errors.displayOrder}
              onChange={set('displayOrder')}
              placeholder="7"
            />
          </Field>
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
    displayOrder: row.displayOrder ?? '',
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
    if (String(form.displayOrder) !== String(row.displayOrder ?? '')) {
      body.displayOrder = form.displayOrder === '' ? null : Number(form.displayOrder)
    }
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
            label="Sort order"
            hint="0 is a real position and sorts first. Clearing the box sends null, which the API leaves alone."
            error={errors.displayOrder}
          >
            <Input
              type="number"
              value={form.displayOrder}
              error={errors.displayOrder}
              onChange={set('displayOrder')}
            />
          </Field>
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
