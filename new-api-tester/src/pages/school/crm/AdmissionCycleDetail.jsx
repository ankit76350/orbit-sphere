import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, Pencil, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
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
                  <Badge tone={STATUS_TONE[cycle.status]}>{cycle.status}</Badge>
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
            description="What the school configured. NOT how the seats are doing — offered, accepted and free are counted from the applications, and that is #7."
          >
            {seats.length === 0 ? (
              <Empty
                title="No seats set up"
                description={
                  'Which is normal, and not something you can fix here yet: #4 — PUT '
                  + '/admission-cycles/{id}/capacities — is the endpoint that sets the seat table, '
                  + 'and it is not built. Every cycle reads back this way today.'
                }
                action={<Info size={22} aria-hidden="true" />}
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
        </>
      ) : null}

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
 * EVERY CLEARABLE FIELD HAS ITS OWN CLEAR CONTROL, because "" and unchanged look alike in a form
 * and there is no such thing as an empty instant. The checkbox is what puts a field in `clear`,
 * which is the only way the API can be told to remove a date. Same call the timetable screen made.
 *
 * THE BODY IS SHOWN LIVE beside the form, so what is actually being sent is never a guess — which
 * matters more here than anywhere else in this module, because "absent" and "cleared" and
 * "unchanged" are three different things that look the same on screen.
 *
 * NOTHING IS DISABLED. Save always sends, including when nothing moved — NOTHING_TO_UPDATE is a
 * documented answer and greying the button out would make it unreachable.
 */
function EditCycle({ open, cycle, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState({})
  const [cleared, setCleared] = useState([])
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
      setCleared([])
      setWithVersion(false)
      setErrors({})
      setRefused(null)
    }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, cycle?.admissionCycleId])

  const set = (field, value) => setForm((old) => ({ ...old, [field]: value }))
  const toggleClear = (field) => setCleared((old) =>
    old.includes(field) ? old.filter((each) => each !== field) : [...old, field])

  const DATES = ['inquiryOpenAt', 'applicationOpenAt', 'applicationCloseAt', 'enrollmentDeadlineAt']

  //! THE DIFF IS THE REQUEST. A field equal to what is stored is left out entirely, which is what
  //! makes this a correction rather than a replacement.
  const body = (() => {
    const out = {}
    if (!cycle) return out
    if (form.name !== undefined && form.name !== (cycle.name ?? '')) out.name = form.name
    for (const field of DATES) {
      if (cleared.includes(field)) continue
      const stored = cycle[field] ?? ''
      if (form[field] && form[field] !== stored) out[field] = form[field]
    }
    if (!cleared.includes('notes') && (form.notes ?? '') !== (cycle.notes ?? '')) {
      out.notes = form.notes
    }
    if (cleared.length) out.clear = cleared
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
      description="Only what you change is sent. A field you clear goes in the clear list — there is no such thing as an empty instant, so that is the only way to remove a date."
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
          hint="Cannot be blanked — it is the only thing telling two rounds of one year apart. An empty name is 400 BLANK_CYCLE_NAME, not a clear."
          error={errors.name}
        >
          <Input value={form.name ?? ''} error={errors.name}
            onChange={(e) => set('name', e.target.value)} />
        </Field>

        {DATES.map((field) => (
          <Field
            key={field}
            label={field}
            hint={cleared.includes(field)
              ? 'Will be REMOVED. The picker is ignored while this is ticked.'
              : 'Leave it alone and it is not sent at all. Tick clear to remove it.'}
            error={errors[field]}
          >
            <div className="stack">
              <Input
                type="datetime-local"
                step="1"
                value={toLocalInput(form[field] ?? '')}
                error={errors[field]}
                onChange={(e) => set(field, toInstant(e.target.value))}
              />
              <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <input type="checkbox" checked={cleared.includes(field)}
                  onChange={() => toggleClear(field)} />
                clear this date
              </label>
            </div>
          </Field>
        ))}

        <Field label="Notes" hint={'Clearable two ways — "" or the tick. Both mean the same thing.'}
          error={errors.notes}>
          <div className="stack">
            <Input value={form.notes ?? ''} error={errors.notes}
              onChange={(e) => set('notes', e.target.value)} />
            <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <input type="checkbox" checked={cleared.includes('notes')}
                onChange={() => toggleClear('notes')} />
              clear the notes
            </label>
          </div>
        </Field>

        <label className="muted" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" checked={withVersion}
            onChange={(e) => setWithVersion(e.target.checked)} />
          Send the version I read ({cycle?.version ?? 0}) — a cycle somebody else changed since
          then answers 409 CONCURRENT_MODIFICATION instead of my change landing on top of theirs.
        </label>

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
