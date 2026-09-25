import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, Pencil, Plus, RefreshCw, Trash2 } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { compact, readable, toInstant, toLocalInput, zoneLabel } from './admissionDates.js'
import { screenPath } from '../../../paths.js'

/**
 * One admission cycle: /school-crm/admission-cycles/{id}
 *
 * TWO ENDPOINTS — #6 reads one cycle and #2 corrects it. The page exists because two things are
 * on #6 that a list row cannot carry: the notes, and the seat table itself rather than a count of
 * it. Correcting belongs here for the same reason: what you are editing is what this page shows.
 *
 * THE SEAT TABLE IS THE REASON THIS PAGE EXISTS, and today it is almost always empty — #4 sets
 * the seats and is not built, so every cycle reads back with none. The page says that rather than
 * showing a bare empty state, because "no seats" and "no endpoint to add seats" look identical
 * and only one of them is something the person can act on.
 *
 * A SEAT ROW WITH NO CLASS NAME IS SHOWN LOUDLY. #6 resolves the names and leaves one absent when
 * the class is gone; the row stays, and this page marks it. That is a cycle holding seats for a
 * class the school no longer has — a real problem, and hiding it is what a tidier screen would do.
 *
 * THE COUNTS COME FROM THE SERVER, not from counting the rows here. capacityCount and totalSeats
 * are both on #6. Recomputing them in the browser would mean two answers that can disagree, and
 * the one on screen would be the one nobody could check.
 *
 * NOTHING IS DISABLED. Refresh always sends, and an id that is not this school's is a documented
 * 404 worth being able to reach by editing the address bar.
 */

const STATUS_TONE = {
  OPEN: 'good',
  SCHEDULED: 'warn',
  CANCELLED: 'bad',
}

export default function AdmissionCycleDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [cycle, setCycle] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [editing, setEditing] = useState(false)
  const [seating, setSeating] = useState(false)
  const [moving, setMoving] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-admission-cycle', {
      label: 'One admission cycle in full',
      pathParams: { admissionCycleId: id ?? '' },
    })
    setLoading(false)
    if (result.ok) { setCycle(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  const back = () => navigate(screenPath('school', 'crm', 'admission-cycles'))

  if (!actingSubdomain) return <NoSchoolChosen what="An admission cycle" />

  const seats = cycle?.capacities ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{cycle?.name ?? 'Admission cycle'}</h1>
          <p className="muted">
            <span className="mono">{id}</span>
            {cycle ? ` · ${cycle.academicYear}` : ' · reading the cycle…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={back}>All rounds</Button>
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Pencil} onClick={() => setEditing(true)}>Correct it</Button>
      </div>

      {problem ? (
        <Card
          title="Could not read it"
          action={<EndpointTag id="get-admission-cycle" name="Get" />}
        >
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
          <p className="muted">
            <Info size={12} /> A cycle id belongs to one school. <b>Another school&rsquo;s real id
            answers 404 here</b>, not the cycle — the school is part of the lookup rather than
            something checked afterwards.
          </p>
        </Card>
      ) : null}

      {cycle ? (
        <>
          <Card
            title="The round"
            description="What the school set up. Everything here except notes is also on a list row."
            action={<EndpointTag id="get-admission-cycle" name="Get" />}
          >
            <div className="stack">
              <div className="field-grid">
                <div>
                  <p className="muted">Status</p>
                  <div className="toolbar" style={{ gap: 8 }}>
                    <Badge tone={STATUS_TONE[cycle.status]}>{cycle.status}</Badge>
                    <Button icon={Pencil} onClick={() => setMoving(true)}>Move it</Button>
                  </div>
                </div>
                <div>
                  <p className="muted">Academic year</p>
                  <p className="mono">{cycle.academicYear}</p>
                </div>
              </div>

              <p className="muted">
                Times shown in <b>{zoneLabel()}</b>. Hover for the exact instant that is stored.
              </p>
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Enquiries open</th>
                      <th>Applications open</th>
                      <th>Applications close</th>
                      <th>Enrollment deadline</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      {['inquiryOpenAt', 'applicationOpenAt', 'applicationCloseAt',
                        'enrollmentDeadlineAt'].map((field) => (
                          <td key={field} title={cycle[field] ?? 'not set'}>
                            {cycle[field]
                              ? compact(cycle[field])
                              : <span className="muted">not set</span>}
                          </td>
                        ))}
                    </tr>
                  </tbody>
                </table>
              </div>

              <div>
                <p className="muted">Notes</p>
                {cycle.notes
                  ? <p>{cycle.notes}</p>
                  : <p className="muted">None. Left out of the response entirely rather than
                    sent as an empty string.</p>}
              </div>

              <p className="muted">
                <Info size={12} /> Created {readable(cycle.createdAt)}, last changed{' '}
                {readable(cycle.updatedAt)}.
              </p>
            </div>
          </Card>

          <Card
            title={`Seats — ${cycle.capacityCount} class${cycle.capacityCount === 1 ? '' : 'es'}, ${cycle.totalSeats} in total`}
            description="What the school configured. NOT how the seats are DOING — offered, accepted and free are counted from the applications, and that is #7, in the card below."
            action={
              <Button look="primary" icon={Pencil} onClick={() => setSeating(true)}>
                Set the seats
              </Button>
            }
          >
            {seats.length === 0 ? (
              <Empty
                title="No seats set up"
                description={
                  'Normal for a new cycle — every one is created with none. Set the seats to say '
                  + 'how many places each class is offering; that is #4, and it replaces the '
                  + 'whole table each time.'
                }
                action={
                  <Button look="primary" icon={Plus} onClick={() => setSeating(true)}>
                    Set the seats
                  </Button>
                }
              />
            ) : (
              <>
                <div className="table-scroll">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Class</th>
                        <th className="num">Seats</th>
                        <th className="num">Reserved</th>
                        <th>Class id</th>
                      </tr>
                    </thead>
                    <tbody>
                      {seats.map((seat) => (
                        <tr key={seat.classDocsId}>
                          {/* A MISSING NAME IS MARKED, NOT TIDIED AWAY. #6 leaves it absent when
                              the class is gone, and that is a cycle holding seats for a class the
                              school no longer has — worth seeing, not worth hiding. */}
                          <td>
                            {seat.className
                              ? seat.className
                              : <Badge tone="bad" title="#6 could not resolve this class in the cycle's year. It may have been deleted.">
                                no such class
                              </Badge>}
                          </td>
                          <td className="num">{seat.totalSeats}</td>
                          <td className="num">{seat.reservedSeats}</td>
                          <td><span className="mono muted">{seat.classDocsId}</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <p className="muted">
                  <Info size={12} /> <b>The totals come from the server</b>, not from adding these
                  rows up here — two places counting one thing is two answers that can disagree.
                  A row reading <span className="mono">no such class</span> is not a display
                  problem: the cycle really does hold seats for a class this year does not have.
                </p>
              </>
            )}
          </Card>

          <Capacity cycle={cycle} />
        </>
      ) : null}

      <MoveStatus
        open={moving}
        cycle={cycle}
        onClose={() => setMoving(false)}
        onSaved={load}
      />

      <SetSeats
        open={seating}
        cycle={cycle}
        onClose={() => setSeating(false)}
        onSaved={load}
      />

      <EditCycle
        open={editing}
        cycle={cycle}
        onClose={() => setEditing(false)}
        onSaved={load}
      />
    </div>
  )
}

/**
 * Correcting a cycle — #2. Its own component so the modal sits at the top of its own return.
 *
 * ONLY WHAT MOVED IS SENT. The form starts from what is stored and the body is built by comparing
 * the two, so a field nobody touched is not in the request at all. Sending everything back would
 * turn a correction into a replacement — three more chances to get a date wrong — and would make
 * the version check fire for edits that changed nothing.
 *
 * EMPTYING THE NOTES BOX IS WHAT CLEARS THEM, and that is all the clearing this endpoint has. It
 * carried a `clear` list until 2026-09-25 — a checkbox here put the field in it — and the list was
 * removed because "" already did the same job. The four dates cannot be emptied at all: they are
 * required on create, so a correction that blanked one would leave a cycle #1 would not have made.
 *
 * THE BODY IS SHOWN LIVE beside the form, so what is actually being sent is never a guess — which
 * matters here because "absent" and "emptied" and "unchanged" look the same on screen.
 *
 * NOTHING IS DISABLED. Save always sends, including when nothing moved — NOTHING_TO_UPDATE is a
 * documented answer and greying the button out would make it unreachable.
 */
function EditCycle({ open, cycle, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState({})
  const [withVersion, setWithVersion] = useState(false)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  //! STARTS FROM WHAT IS STORED, so the diff below has something to compare against.
  useEffect(() => {
    if (open && cycle) {
      setForm({
        name: cycle.name ?? '',
        inquiryOpenAt: cycle.inquiryOpenAt ?? '',
        applicationOpenAt: cycle.applicationOpenAt ?? '',
        applicationCloseAt: cycle.applicationCloseAt ?? '',
        enrollmentDeadlineAt: cycle.enrollmentDeadlineAt ?? '',
        notes: cycle.notes ?? '',
      })
      setWithVersion(false)
      setErrors({})
      setRefused(null)
    }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, cycle?.admissionCycleId])

  const set = (field, value) => setForm((old) => ({ ...old, [field]: value }))

  const DATES = ['inquiryOpenAt', 'applicationOpenAt', 'applicationCloseAt', 'enrollmentDeadlineAt']

  //! THE DIFF IS THE REQUEST. A field equal to what is stored is left out entirely, which is what
  //! makes this a correction rather than a replacement.
  const body = (() => {
    const out = {}
    if (!cycle) return out
    if (form.name !== undefined && form.name !== (cycle.name ?? '')) out.name = form.name
    for (const field of DATES) {
      const stored = cycle[field] ?? ''
      if (form[field] && form[field] !== stored) out[field] = form[field]
    }
    //! "" IS A REAL VALUE HERE, not an absence — emptying the box is how the notes are cleared,
    //! so the comparison is against what is stored rather than a truthiness test.
    if ((form.notes ?? '') !== (cycle.notes ?? '')) {
      out.notes = form.notes ?? ''
    }
    if (withVersion) out.version = cycle.version ?? 0
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('update-admission-cycle', {
      label: 'Correct the cycle',
      pathParams: { admissionCycleId: cycle?.admissionCycleId ?? '' },
      body,
    })
    setSaving(false)
    if (result.ok) { onSaved(); onClose(); return }
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
      previewLabel="WHAT WILL BE SENT"
      title="Correct this cycle"
      description="Only what you change is sent. Emptying the notes box clears them; the four dates can be moved but never blanked."
      endpoint={<EndpointTag id="update-admission-cycle" name="Correct" look="primary" />}
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

        <Field
          label="Name"
          hint="Cannot be blanked — it is the only thing telling two rounds of one year apart. An empty name is 400 BLANK_CYCLE_NAME."
          error={errors.name}
        >
          <Input value={form.name ?? ''} error={errors.name}
            onChange={(e) => set('name', e.target.value)} />
        </Field>

        {DATES.map((field) => (
          <Field
            key={field}
            label={field}
            hint="Leave it alone and it is not sent at all. It can be MOVED but never emptied — the four dates became required on 2026-09-22, so blanking one would leave a cycle the create endpoint would refuse to make. It must also land INSIDE the academic year."
            error={errors[field]}
          >
            <Input
              type="datetime-local"
              step="1"
              value={toLocalInput(form[field] ?? '')}
              error={errors[field]}
              onChange={(e) => set(field, toInstant(e.target.value))}
            />
          </Field>
        ))}

        <Field label="Notes"
          hint={'Empty the box to clear them — "" is the project\'s one convention, and #9 and #18 use it too. This endpoint also took a `clear` list naming the field until 2026-09-25; it was removed because "" already did the job.'}
          error={errors.notes}>
          <Input value={form.notes ?? ''} error={errors.notes}
            onChange={(e) => set('notes', e.target.value)} />
        </Field>

        <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" checked={withVersion}
            onChange={(e) => setWithVersion(e.target.checked)} />
          Send the version I read ({cycle?.version ?? 0}) — a cycle somebody else changed since
          then answers 409 CONCURRENT_MODIFICATION instead of my change landing on top of theirs.
        </label>

        <p className="muted">
          <Info size={12} /> <b>Only the notes can be emptied.</b> The four dates are required on
          create, so a correction can move one but never blank it — there is no field on this
          request that would ask for that.
        </p>

        <p className="muted">
          <Info size={12} /> <b>The dates are checked as they will end up</b>, merged with what is
          already stored. A close date that is fine on its own can still be wrong against the open
          date the cycle already has — that is{' '}
          <span className="mono">400 CYCLE_DATES_OUT_OF_ORDER</span>.
        </p>

        <p className="muted">
          <Info size={12} /> <span className="mono">academicYear</span>,{' '}
          <span className="mono">status</span> and <span className="mono">capacities</span> are not
          accepted here. The first is a different cycle, the second is #3 and the third is #4 —
          none of which is built.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Setting the seat table — #4. A row editor, because the endpoint replaces the whole table.
 *
 * IT SENDS EVERY ROW ON SCREEN, always. That is not laziness: #4 is a PUT and what you send IS the
 * table. A row removed here is a row removed there, which is why the form starts from what is
 * stored rather than empty — starting empty would make "save" mean "delete everything", and
 * somebody would find that out the hard way.
 *
 * THE CLASS IS A PICKER, not a box to paste an id into. Every class of the cycle's year, loaded
 * once when the dialog opens. A cycle admits into one year and a class from another is
 * CLASS_NOT_IN_CYCLE_YEAR, so offering the wrong ones would be offering a refusal.
 *
 * BUT THE REFUSALS STAY REACHABLE. The picker also offers "— a class from another year —", which
 * puts a real class of a different year in the row, and a row can be duplicated. Both are
 * documented 409s and a screen that made them unreachable would be deciding which requests are
 * worth testing.
 *
 * THE LIVE BODY IS SHOWN, because "what will this actually send" is the question a replacing write
 * has to answer before you press it.
 */
function SetSeats({ open, cycle, onClose, onSaved }) {
  const { call } = useApi()
  const [rows, setRows] = useState([])
  const [classes, setClasses] = useState([])
  const [withVersion, setWithVersion] = useState(false)
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  //! STARTS FROM WHAT IS STORED. A PUT means what is on screen becomes the table, so an empty
  //! start would turn "save" into "delete everything".
  useEffect(() => {
    if (!open || !cycle) return
    setRows((cycle.capacities ?? []).map((seat) => ({
      classDocsId: seat.classDocsId ?? '',
      totalSeats: String(seat.totalSeats ?? 0),
      reservedSeats: String(seat.reservedSeats ?? 0),
    })))
    setWithVersion(false)
    setRefused(null)
    // The classes of the CYCLE'S year, which is the only year #4 accepts.
    call('list-school-classes', {
      label: "Classes of the cycle's year",
      pathParams: { year: cycle.academicYear ?? '' },
      query: { page: 0, size: 100 },
    }).then((result) => setClasses(result.ok ? (result.bodyJson?.content ?? []) : []))
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, cycle?.admissionCycleId])

  const setRow = (index, field, value) => setRows((old) =>
    old.map((row, n) => (n === index ? { ...row, [field]: value } : row)))
  const addRow = () => setRows((old) => [...old,
    { classDocsId: '', totalSeats: '0', reservedSeats: '0' }])
  const removeRow = (index) => setRows((old) => old.filter((_, n) => n !== index))

  //! WHAT IS ON SCREEN IS WHAT IS SENT. A blank number becomes 0 rather than being dropped,
  //! because dropping it would send a row with no totalSeats and read as a validation failure
  //! the person did not cause.
  const body = {
    capacities: rows.map((row) => ({
      classDocsId: row.classDocsId,
      totalSeats: Number(row.totalSeats || 0),
      reservedSeats: Number(row.reservedSeats || 0),
    })),
    ...(withVersion ? { version: cycle?.version ?? 0 } : {}),
  }

  const submit = async () => {
    setRefused(null)
    setSaving(true)
    const result = await call('set-admission-cycle-capacities', {
      label: 'Set the seat table',
      pathParams: { admissionCycleId: cycle?.admissionCycleId ?? '' },
      body,
    })
    setSaving(false)
    if (result.ok) { onSaved(); onClose(); return }
    if (result.bodyJson?.code) setRefused(result.bodyJson)
  }

  const total = body.capacities.reduce((sum, row) => sum + (row.totalSeats || 0), 0)

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title="Set the seat table"
      description="A PUT — what is here becomes the table. A row you remove is removed, and saving with no rows clears it."
      endpoint={<EndpointTag id="set-admission-cycle-capacities" name="Set" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Save the table</Button>
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

        <p className="muted">
          <Info size={12} /> <b>This replaces the whole table.</b> Rows you remove here are removed
          on the server — it is not a merge. Saving with no rows clears it, which is a real thing
          to want and is not the same as forgetting to send the field.
        </p>

        {/* THE READ THAT FILLS THE CLASS PICKERS, fired when this modal opens. The CYCLE'S year,
            which is the only year #4 accepts — so the tag carries it rather than the school's
            current one. */}
        <p className="muted">
          <EndpointTag id="list-school-classes" name="Classes of the cycle's year"
            pathParams={{ year: cycle?.academicYear }} query={{ page: 0, size: 100 }} />
        </p>

        {rows.length === 0 ? (
          <p className="muted">No rows. Saving now would clear the table.</p>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Class</th>
                  <th className="num">Seats</th>
                  <th className="num">Reserved</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => (
                  <tr key={index}>
                    <td>
                      {/* An OBJECT option: the name is shown, the id is sent. The same shape
                          the timetable grid needed for teachers. */}
                      <Select
                        value={row.classDocsId}
                        options={[
                          { value: '', label: '— choose a class —' },
                          ...classes.map((one) => ({
                            value: one.schoolClassId, label: one.name,
                          })),
                          // NOT of this year, on purpose: CLASS_NOT_IN_CYCLE_YEAR is a
                          // documented refusal and a picker that could not reach it would be
                          // deciding which requests are worth testing.
                          { value: '6aa39612224c2e933a1cFFFF', label: '— a class not in this year —' },
                        ]}
                        label="Class"
                        onChange={(value) => setRow(index, 'classDocsId', value)}
                      />
                    </td>
                    <td className="num">
                      <Input type="number" min="0" value={row.totalSeats}
                        onChange={(e) => setRow(index, 'totalSeats', e.target.value)} />
                    </td>
                    <td className="num">
                      <Input type="number" min="0" value={row.reservedSeats}
                        onChange={(e) => setRow(index, 'reservedSeats', e.target.value)} />
                    </td>
                    <td>
                      <Button icon={Trash2} onClick={() => removeRow(index)}>Remove</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="toolbar">
          <Button icon={Plus} onClick={addRow}>Add a class</Button>
          <span className="toolbar-spacer" />
          <Badge>{rows.length} row{rows.length === 1 ? '' : 's'} · {total} seats</Badge>
        </div>

        <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" checked={withVersion}
            onChange={(e) => setWithVersion(e.target.checked)} />
          Send the version I read ({cycle?.version ?? 0}). <b>Worth more here than on a correction</b>
          {' '}— this write replaces, so two people setting intake from stale screens means one of
          them silently loses every row the other added.
        </label>

        <p className="muted">
          <Info size={12} /> A class must belong to <b>{cycle?.academicYear}</b>, the year this
          cycle admits into — the picker only offers those. Leaving a row&rsquo;s class blank, or
          listing one class twice, are both documented refusals worth being able to reach:{' '}
          <span className="mono">CLASS_NOT_IN_CYCLE_YEAR</span> and{' '}
          <span className="mono">DUPLICATE_CAPACITY_CLASS</span>.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Moving a cycle through its lifecycle — #3. The endpoint the rest of the module waited for.
 *
 * EVERY STATUS IS OFFERED, including the ones the graph refuses. That is deliberate and it is the
 * opposite of what a product UI would do: INVALID_CYCLE_TRANSITION is a documented answer with a
 * message that lists what IS reachable, and a picker that only offered legal moves would make it
 * unreachable. The screen SAYS which are legal instead of enforcing it.
 *
 * THE GRAPH IS DRAWN, because "why can I not reopen this" is the question the refusal answers and
 * a diagram answers it before the request is sent.
 *
 * OPENING WITH NO SEATS IS CALLED OUT, because it is the one refusal that is not about the graph
 * at all — #17 will reject an application whose class is not in the seat table, so an OPEN cycle
 * with no seats is a round nobody can apply to.
 */
const REACHABLE = {
  DRAFT: ['SCHEDULED', 'OPEN', 'CANCELLED'],
  SCHEDULED: ['OPEN', 'CANCELLED'],
  OPEN: ['CLOSED', 'CANCELLED'],
  CLOSED: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
}
const ALL_STATUSES = ['DRAFT', 'SCHEDULED', 'OPEN', 'CLOSED', 'COMPLETED', 'CANCELLED']

function MoveStatus({ open, cycle, onClose, onSaved }) {
  const { call } = useApi()
  const [to, setTo] = useState('')
  const [withVersion, setWithVersion] = useState(false)
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open && cycle) {
      setTo((REACHABLE[cycle.status] ?? [])[0] ?? '')
      setWithVersion(false)
      setRefused(null)
    }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, cycle?.admissionCycleId, cycle?.status])

  const body = { status: to, ...(withVersion ? { version: cycle?.version ?? 0 } : {}) }
  const legal = REACHABLE[cycle?.status] ?? []
  const noSeats = (cycle?.capacityCount ?? 0) === 0

  const submit = async () => {
    setRefused(null)
    setSaving(true)
    const result = await call('move-admission-cycle-status', {
      label: `Move the cycle to ${to}`,
      pathParams: { admissionCycleId: cycle?.admissionCycleId ?? '' },
      body,
    })
    setSaving(false)
    if (result.ok) { onSaved(); onClose(); return }
    if (result.bodyJson?.code) setRefused(result.bodyJson)
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={`Move this cycle from ${cycle?.status ?? ''}`}
      description="It only goes forwards, and COMPLETED and CANCELLED are both terminal. A cycle closed by mistake cannot be reopened — the safe undo is a new cycle."
      endpoint={<EndpointTag id="move-admission-cycle-status" name="Move" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Move it</Button>
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

        <pre className="resp-body">{`DRAFT ──> SCHEDULED ──> OPEN ──> CLOSED ──> COMPLETED
  │           │           │         │
  └───────────┴───────────┴─────────┴──> CANCELLED`}</pre>

        <Field
          label="Move it to"
          hint={legal.length
            ? `From ${cycle?.status} the graph allows: ${legal.join(', ')}. The others are offered anyway — INVALID_CYCLE_TRANSITION is a documented answer worth being able to see.`
            : `${cycle?.status} is terminal. Every option here will be refused, which is the thing to check.`}
        >
          <Select
            value={to}
            options={ALL_STATUSES.map((one) => ({
              value: one,
              label: legal.includes(one) ? one : `${one} — the graph refuses this`,
            }))}
            label="New status"
            onChange={setTo}
          />
        </Field>

        {to === 'OPEN' && noSeats ? (
          <p className="muted">
            <Info size={12} /> <b>This cycle has no seats</b>, so opening it will answer{' '}
            <span className="mono">409 CYCLE_HAS_NO_SEATS</span>. That is not the graph: #17 refuses
            an application whose class is not in the seat table, so an open cycle with no seats is a
            round nobody can apply to. Set the seats first — or send it anyway and read the refusal.
          </p>
        ) : null}

        <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" checked={withVersion}
            onChange={(e) => setWithVersion(e.target.checked)} />
          Send the version I read ({cycle?.version ?? 0}) — a cycle somebody else moved since then
          answers 409 CONCURRENT_MODIFICATION rather than being moved twice.
        </label>

        <p className="muted">
          <Info size={12} /> Asking for the status it <b>already has</b> is a refusal too, not a
          silent success — a 200 there would tell you that you opened a cycle you did not.
        </p>
      </div>
    </Modal>
  )
}

/**
 * #7 — seats against reality.
 *
 * WHY IT IS A CARD AND NOT A SCREEN. It is one round's numbers, and the round already has a page.
 * A top-level "capacity" screen would need a cycle picker to answer the same question.
 *
 * THE NUMBER TO LOOK AT IS FREE, AND IT CAN BE NEGATIVE. #29 does not cap offers against the seat
 * table — schools over-offer on purpose, because a fifth of families go elsewhere — so this is the
 * only place that says a round has promised more seats than it has.
 *
 * APPROVED IS NOT COMMITTED, and the card says so, because it is the line people get wrong: a
 * school that approved forty children has decided something, not promised anybody a seat.
 */
function Capacity({ cycle }) {
  const { call } = useApi()
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    const result = await call('get-admission-cycle-capacity', {
      label: 'Seats against reality',
      pathParams: { admissionCycleId: cycle.admissionCycleId },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, cycle.admissionCycleId])

  useEffect(() => { load() }, [load])

  const rows = data?.classes ?? []
  const over = data?.overCommittedClasses ?? 0

  return (
    <Card
      title="How the seats are doing"
      description="Counted from the applications, never stored — keeping these on the cycle would make it a document every application write has to touch."
      action={
        <div className="btn-row">
          <EndpointTag id="get-admission-cycle-capacity" name="Capacity"
            pathParams={{ admissionCycleId: cycle.admissionCycleId }} />
          <Badge tone={over ? 'bad' : 'good'}>
            {over ? `${over} over-committed` : 'within seats'}
          </Badge>
          <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        </div>
      }
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
          title="Nothing to count against"
          description="This round has no seat table, so there are no classes to report on. #4 sets one — and #3 refuses to open a round without it."
        />
      ) : (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Class</th>
                  <th className="num">Seats</th>
                  <th className="num">Held</th>
                  <th className="num">Open</th>
                  <th className="num">Pending</th>
                  <th className="num">Approved</th>
                  <th className="num">Offered</th>
                  <th className="num">Accepted</th>
                  <th className="num">Committed</th>
                  <th className="num">Free</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  <tr key={one.classDocsId}>
                    <td>
                      {one.className ?? <span className="muted">no such class</span>}
                      <br />
                      <span className="mono muted">{one.classDocsId}</span>
                    </td>
                    <td className="num">{one.totalSeats}</td>
                    <td className="num">{one.reservedSeats}</td>
                    <td className="num">{one.openSeats}</td>
                    <td className="num">{one.pending}</td>
                    <td className="num">{one.approved}</td>
                    <td className="num">{one.offered}</td>
                    <td className="num">{one.accepted}</td>
                    <td className="num">{one.committed}</td>
                    <td className="num">
                      {one.overCommitted
                        ? <Badge tone="bad">{one.freeSeats}</Badge>
                        : one.freeSeats}
                    </td>
                  </tr>
                ))}
                {data?.total ? (
                  <tr>
                    <td><b>Total</b></td>
                    <td className="num"><b>{data.total.totalSeats}</b></td>
                    <td className="num"><b>{data.total.reservedSeats}</b></td>
                    <td className="num"><b>{data.total.openSeats}</b></td>
                    <td className="num"><b>{data.total.pending}</b></td>
                    <td className="num"><b>{data.total.approved}</b></td>
                    <td className="num"><b>{data.total.offered}</b></td>
                    <td className="num"><b>{data.total.accepted}</b></td>
                    <td className="num"><b>{data.total.committed}</b></td>
                    <td className="num">
                      {data.total.overCommitted
                        ? <Badge tone="bad">{data.total.freeSeats}</Badge>
                        : <b>{data.total.freeSeats}</b>}
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>

          <p className="muted">
            <Info size={12} /> <b>Approved is not committed.</b> A seat is promised when a letter
            goes out (#29), not when the school decides — so <span className="mono">committed</span>
            {' '}is offered plus accepted plus enrolled, and approvals sit outside it. Counting them
            would make every round look over-subscribed the moment it started deciding.
          </p>

          <p className="muted">
            <Info size={12} /> <b>Free can go negative, and it is not clamped.</b> #29 does not cap
            offers against the seat table — schools offer sixty letters for forty places because a
            fifth of families go elsewhere — so this is the only place that says a round has
            promised more than it has. <b>&ldquo;0 free&rdquo; cannot tell &ldquo;exactly
            full&rdquo; from &ldquo;twenty over&rdquo;</b>, which is why it is a real number.
          </p>

          <p className="muted">
            <Info size={12} /> <b>Two things these numbers get wrong on purpose, for now.</b> A
            family that DECLINES a letter still counts as committed — #30 leaves the application at{' '}
            <span className="mono">OFFERED</span> — so a declined seat is never given back. And a
            child offered a <i>different</i> grade is counted against the class they applied for,
            because the counts group by <span className="mono">appliedClassDocsId</span>. Both are
            written down rather than hidden.
          </p>
        </>
      )}
    </Card>
  )
}
