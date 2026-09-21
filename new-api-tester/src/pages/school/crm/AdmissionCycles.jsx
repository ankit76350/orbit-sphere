import { useEffect, useState } from 'react'
import { Info, Plus } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable, toInstant, toLocalInput } from './admissionDates.js'

/**
 * Admission cycles: /school-crm/admission-cycles
 *
 * ONE ENDPOINT — #1, which opens a year for admissions. Thirty-three others are planned and none
 * is built, so a cycle goes in and cannot be read back out.
 *
 * CREATING IS A MODAL, the same as every other screen here. The form used to sit open on the page
 * because there is no list endpoint to put behind it — which was true and still read wrong: a
 * permanently open form makes the page look like a form rather than like a screen about admission
 * cycles, and it does not match Grading, Departments or Classes, where creating is the occasional
 * act behind a button.
 *
 * THE PAGE ITSELF IS WHAT WAS MADE IN THIS SESSION, and that is the only record of it anywhere.
 * #5 and #6 would list these; neither is built, so the response body is the only way to see what
 * was stored. Reloading loses it, which is honest — nothing can fetch it back.
 *
 * NOTHING IS DISABLED. Sending with no year, no name or an empty form is how VALIDATION_FAILED and
 * ACADEMIC_YEAR_NOT_FOUND are reached, and both are documented refusals worth being able to hit.
 * The year box is pre-filled from the year picker and is then free text — typing a year the school
 * does not have is the only way to see the 404.
 *
 * THE YEAR IS A FIELD, NOT PART OF THE PATH, unlike every academics screen. That is the module's
 * own decision: a school runs next year's admissions during this one, and often two cycles at
 * once, so the year is a property of the cycle rather than the scope it sits in.
 *
 * AND IT NEED NOT BE THE RUNNING YEAR. This is the one school-surface write in the product where
 * gate 4 does not run, so the modal says so — somebody who has used the academics screens will
 * expect a year that is not running to be refused here too.
 */

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
  const { actingSubdomain } = useApiState()
  const [open, setOpen] = useState(false)
  // Every cycle made in this session, newest first. The only record of them that exists.
  const [made, setMade] = useState([])

  if (!actingSubdomain) return <NoSchoolChosen what="Admission cycles" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <span className="toolbar-spacer" />
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Create cycle</Button>
      </div>

      <Card
        title="Admission cycles"
        description="A cycle is one round of admissions for one academic year. Every application has to name one, so this is the first call in the module."
        action={<EndpointTag id="create-admission-cycle" name="Create" look="primary" />}
      >
        {made.length === 0 ? (
          <Empty
            title="Nothing created yet"
            description={
              'There is no list endpoint — #5 and #6 are not built — so this screen can only show '
              + 'what you create while it is open. Press Create cycle to open a year for admissions.'
            }
            action={
              <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Create one</Button>
            }
          />
        ) : (
          <div className="stack">
            <p className="muted">
              <Info size={12} /> Created in this session. <b>There is no list endpoint yet</b> — #5
              and #6 are not built — so this is the only place these appear. Reloading the page
              loses it, because nothing can fetch them back.
            </p>
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Year</th>
                    <th>Status</th>
                    <th>Seats</th>
                    <th>Id</th>
                  </tr>
                </thead>
                <tbody>
                  {made.map((cycle) => (
                    <tr key={cycle.admissionCycleId}>
                      <td>{cycle.name}</td>
                      <td>{cycle.academicYear}</td>
                      <td><span className="mono">{cycle.status}</span></td>
                      <td>{cycle.capacityCount}</td>
                      <td className="mono">{cycle.admissionCycleId}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </Card>

      <CreateCycle
        open={open}
        onClose={() => setOpen(false)}
        onAdded={(cycle) => setMade((old) => [cycle, ...old])}
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
function DateField({ label, hint, raw, value, error, onChange }) {
  return (
    <Field label={label} hint={hint} error={error}>
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
            raw={raw} value={form.inquiryOpenAt} error={errors.inquiryOpenAt}
            onChange={(v) => setForm((old) => ({ ...old, inquiryOpenAt: v }))}
          />
          <DateField
            label="Applications open"
            hint="The first moment a family can actually submit a form. Between this and the date beside it, the school is gathering interest but taking no applications."
            raw={raw} value={form.applicationOpenAt} error={errors.applicationOpenAt}
            onChange={(v) => setForm((old) => ({ ...old, applicationOpenAt: v }))}
          />
        </div>

        <div className="field-grid">
          <DateField
            label="Applications close"
            hint="The last moment a form is taken. Pick 11:59:59 pm and the instant below shows what that really is in UTC — for an Indian school, 18:29:59Z."
            raw={raw} value={form.applicationCloseAt} error={errors.applicationCloseAt}
            onChange={(v) => setForm((old) => ({ ...old, applicationCloseAt: v }))}
          />
          <DateField
            label="Enrollment deadline"
            hint="The last moment a family who was OFFERED a seat can take it and become a student. After it the school gives that seat to somebody on the waitlist."
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
