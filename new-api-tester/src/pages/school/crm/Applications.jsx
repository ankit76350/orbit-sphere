import { useCallback, useEffect, useState } from 'react'
import { Info, Plus, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * Admission applications: /school-crm/applications
 *
 * ONE ENDPOINT — #17, which starts one. There is no list: #24 and #25 are not built, so an
 * application goes in and cannot be read back out.
 *
 * WHICH IS WHY WHAT WAS CREATED STAYS ON SCREEN. It is the only record of these anywhere, and the
 * card says so. Reloading loses it, which is honest — nothing can fetch them back.
 *
 * THE CYCLE IS A PICKER OF OPEN CYCLES, loaded from #5 with ?status=OPEN. That is not gating: a
 * cycle that is not open is CYCLE_NOT_OPEN, and the picker offers those too, labelled with their
 * status. Somebody testing this module needs to reach that refusal, and it is the module's
 * replacement for gate 4 — the single most important thing to be able to see fail.
 *
 * THE CLASS IS A PLAIN BOX, not a picker. Two different refusals live here —
 * CLASS_NOT_IN_CYCLE_YEAR and CLASS_NOT_IN_CAPACITY — and telling them apart is most of what this
 * endpoint does. A picker that only offered classes in the seat table would make both unreachable.
 *
 * NOTHING IS DISABLED.
 */
export default function Applications() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [open, setOpen] = useState(false)
  const [cycles, setCycles] = useState([])
  const [loading, setLoading] = useState(false)
  // Every application made in this session, newest first. The only record there is.
  const [made, setMade] = useState([])

  const loadCycles = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    //! EVERY cycle, not only the open ones. The picker labels each with its status so a
    //! CYCLE_NOT_OPEN refusal is one selection away.
    const result = await call('list-admission-cycles', {
      label: 'Cycles to apply into',
      query: { page: 0, size: 100, sort: 'name' },
    })
    setLoading(false)
    setCycles(result.ok ? (result.bodyJson?.content ?? []) : [])
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain])

  useEffect(() => { loadCycles() }, [loadCycles])

  if (!actingSubdomain) return <NoSchoolChosen what="Admission applications" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Applications</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {' · a form can only go into an OPEN cycle'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={loadCycles} busy={loading}>Refresh cycles</Button>
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Start an application</Button>
      </div>

      <Card
        title="Started in this session"
        description="There is no list endpoint — #24 and #25 are not built — so this is the only place these appear. Reloading the page loses it, because nothing can fetch them back."
        action={<EndpointTag id="create-admission-application" name="Start" look="primary" />}
      >
        {made.length === 0 ? (
          <Empty
            title="Nothing started yet"
            description={
              'Open a cycle with #3 and set its seats with #4 first — an application needs a cycle '
              + 'that is OPEN and a class that has seats in it.'
            }
            action={
              <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Start one</Button>
            }
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Application no</th>
                  <th>Applicant</th>
                  <th>Class</th>
                  <th>Status</th>
                  <th>From a lead</th>
                  <th>Id</th>
                </tr>
              </thead>
              <tbody>
                {made.map((one) => (
                  <tr key={one.admissionApplicationId}>
                    <td><span className="mono">{one.applicationNo}</span></td>
                    <td>{one.applicantName}</td>
                    <td>{one.appliedClassName ?? one.appliedClassDocsId}</td>
                    <td><Badge>{one.status}</Badge></td>
                    <td>
                      {one.inquiryDocsId
                        ? <span className="mono muted">{one.inquiryDocsId}</span>
                        : <span className="muted">walked in</span>}
                    </td>
                    <td><span className="mono muted">{one.admissionApplicationId}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <StartApplication
        open={open}
        cycles={cycles}
        onClose={() => setOpen(false)}
        onStarted={(one) => setMade((old) => [one, ...old])}
      />
    </div>
  )
}

/**
 * Starting an application — #17.
 *
 * THE GUARDIAN LIST IS EDITABLE AND CAN BE EMPTIED, because an empty list is a documented 400 and
 * an application requiring at least one guardian is the thing that distinguishes it from an
 * inquiry. A form that always kept one row would make that untestable.
 *
 * THE INQUIRY IS A PLAIN BOX with a warning, not a picker. #8 is not built, so there is nothing to
 * pick from — and the two refusals that live on this field (INQUIRY_NOT_FOUND and
 * APPLICATION_ALREADY_EXISTS) both need a typed id anyway.
 */
const BLANK_GUARDIAN = {
  fullName: '', relation: 'FATHER', phoneNumber: '', emailAddress: '', primaryContact: true,
}
const RELATIONS = ['FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT',
  'LEGAL_GUARDIAN', 'SIBLING', 'OTHER']

function StartApplication({ open, cycles, onClose, onStarted }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    admissionCycleDocsId: '', inquiryDocsId: '', appliedClassDocsId: '',
    applicantName: '', dateOfBirth: '', gender: 'MALE', formAnswers: '',
  })
  const [guardians, setGuardians] = useState([{ ...BLANK_GUARDIAN }])
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open) return
    const firstOpen = cycles.find((one) => one.status === 'OPEN')
    setForm({
      admissionCycleDocsId: firstOpen?.admissionCycleId ?? cycles[0]?.admissionCycleId ?? '',
      inquiryDocsId: '', appliedClassDocsId: '',
      applicantName: '', dateOfBirth: '', gender: 'MALE', formAnswers: '',
    })
    setGuardians([{ ...BLANK_GUARDIAN }])
    setErrors({})
    setRefused(null)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))
  const setGuardian = (index, field, value) => setGuardians((old) =>
    old.map((row, n) => (n === index ? { ...row, [field]: value } : row)))

  //! ANSWERS ARE TYPED AS JSON, because the shape is whatever the school asks for and nothing
  //! validates it. Unparseable text is shown as such rather than silently dropped.
  let answers = null
  let answersProblem = null
  if (form.formAnswers.trim() !== '') {
    try {
      answers = JSON.parse(form.formAnswers)
    } catch (error) {
      answersProblem = error.message
    }
  }

  const body = {
    admissionCycleDocsId: form.admissionCycleDocsId,
    appliedClassDocsId: form.appliedClassDocsId,
    applicantName: form.applicantName,
    dateOfBirth: form.dateOfBirth,
    gender: form.gender,
    guardians: guardians.map((one) => ({
      fullName: one.fullName,
      relation: one.relation,
      ...(one.phoneNumber.trim() ? { phoneNumber: one.phoneNumber.trim() } : {}),
      ...(one.emailAddress.trim() ? { emailAddress: one.emailAddress.trim() } : {}),
      primaryContact: one.primaryContact,
    })),
    ...(form.inquiryDocsId.trim() ? { inquiryDocsId: form.inquiryDocsId.trim() } : {}),
    ...(answers ? { formAnswers: answers } : {}),
  }

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-admission-application', {
      label: 'Start an application', body,
    })
    setSaving(false)
    if (result.ok) {
      onStarted(result.bodyJson)
      onClose()
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

  const chosen = cycles.find((one) => one.admissionCycleId === form.admissionCycleDocsId)

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title="Start an application"
      description="Creates it as a DRAFT. Submitting is #19, which is not built, so it cannot move past DRAFT yet."
      endpoint={<EndpointTag id="create-admission-application" name="Start" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Start it</Button>
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
          label="Admission cycle"
          required
          hint="Every cycle is offered, with its status. Only an OPEN one is accepted — anything else is 409 CYCLE_NOT_OPEN, which is this module's replacement for gate 4 and the thing most worth seeing fail."
          error={errors.admissionCycleDocsId}
        >
          <Select
            value={form.admissionCycleDocsId}
            options={[
              { value: '', label: '— choose a cycle —' },
              ...cycles.map((one) => ({
                value: one.admissionCycleId,
                label: `${one.name} · ${one.status}${one.status === 'OPEN' ? '' : ' — will be refused'}`,
              })),
            ]}
            label="Admission cycle"
            onChange={(value) => setForm((old) => ({ ...old, admissionCycleDocsId: value }))}
          />
        </Field>

        {chosen && chosen.status !== 'OPEN' ? (
          <p className="muted">
            <Info size={12} /> <b>{chosen.name} is {chosen.status}</b>, so this will answer{' '}
            <span className="mono">409 CYCLE_NOT_OPEN</span>. Open it with #3 — which will itself
            refuse if the cycle has no seats.
          </p>
        ) : null}

        <Field
          label="Applied class id"
          required
          hint="A plain box on purpose: two refusals live here. A class of another year is CLASS_NOT_IN_CYCLE_YEAR; a class of the right year with no seats in this cycle is CLASS_NOT_IN_CAPACITY. A picker would make both unreachable."
          error={errors.appliedClassDocsId}
        >
          <Input value={form.appliedClassDocsId} error={errors.appliedClassDocsId}
            onChange={set('appliedClassDocsId')} placeholder="67aa15d9dc3f7d0011111111" />
        </Field>

        <div className="field-grid">
          <Field label="Applicant name" required error={errors.applicantName}
            hint="The child.">
            <Input value={form.applicantName} error={errors.applicantName}
              onChange={set('applicantName')} placeholder="Aarav Sharma" />
          </Field>
          <Field label="Date of birth" required error={errors.dateOfBirth}
            hint="Required here, unlike on an inquiry. Must be in the past.">
            <Input type="date" value={form.dateOfBirth} error={errors.dateOfBirth}
              onChange={set('dateOfBirth')} />
          </Field>
        </div>

        <div className="field-grid">
          <Field label="Gender" required error={errors.gender}
            hint="Required here, unlike on an inquiry.">
            <Select value={form.gender} options={['MALE', 'FEMALE', 'OTHER']} label="Gender"
              onChange={(value) => setForm((old) => ({ ...old, gender: value }))} />
          </Field>
          <Field
            label="Inquiry id"
            hint="Optional — the family that walks in never enquired. #8 is not built, so nothing can create one through the API. A wrong id is INQUIRY_NOT_FOUND; the same id twice in one cycle is APPLICATION_ALREADY_EXISTS."
            error={errors.inquiryDocsId}
          >
            <Input value={form.inquiryDocsId} error={errors.inquiryDocsId}
              onChange={set('inquiryDocsId')} placeholder="blank means a walk-in" />
          </Field>
        </div>

        <div className="stack">
          <p className="muted">
            <b>Guardians</b> — at least one is required, which is what makes an application a
            formal document where an inquiry was not. Removing them all is a documented{' '}
            <span className="mono">400</span>.
          </p>
          {guardians.map((one, index) => (
            <div className="field-grid" key={index}>
              <Field label={`Guardian ${index + 1} name`}>
                <Input value={one.fullName}
                  onChange={(e) => setGuardian(index, 'fullName', e.target.value)}
                  placeholder="Rohan Sharma" />
              </Field>
              <Field label="Relation">
                <Select value={one.relation} options={RELATIONS} label="Relation"
                  onChange={(value) => setGuardian(index, 'relation', value)} />
              </Field>
              <Field label="Phone">
                <Input value={one.phoneNumber}
                  onChange={(e) => setGuardian(index, 'phoneNumber', e.target.value)}
                  placeholder="+919876543210" />
              </Field>
              <Field label="Email">
                <Input value={one.emailAddress}
                  onChange={(e) => setGuardian(index, 'emailAddress', e.target.value)}
                  placeholder="rohan@example.com" />
              </Field>
            </div>
          ))}
          <div className="toolbar">
            <Button icon={Plus}
              onClick={() => setGuardians((old) => [...old, { ...BLANK_GUARDIAN }])}>
              Add a guardian
            </Button>
            <Button onClick={() => setGuardians((old) => old.slice(0, -1))}>
              Remove the last
            </Button>
            <span className="toolbar-spacer" />
            <Badge tone={guardians.length === 0 ? 'bad' : undefined}>
              {guardians.length} guardian{guardians.length === 1 ? '' : 's'}
            </Badge>
          </div>
        </div>

        <Field
          label="Form answers (JSON)"
          hint="Optional, and NOTHING validates it — there is no form definition to check against. Over 200 keys is 400 TOO_MANY_FORM_ANSWERS."
          error={answersProblem}
        >
          <Input value={form.formAnswers} error={answersProblem}
            onChange={set('formAnswers')}
            placeholder={'{"previousSchool": "ABC School"}'} />
        </Field>
        {answersProblem ? (
          <p className="muted">
            <Info size={12} /> That is not valid JSON, so it is left out of the request entirely
            rather than sent as a string. The body on the right shows what will actually go.
          </p>
        ) : null}
      </div>
    </Modal>
  )
}
