import { useCallback, useEffect, useMemo, useState } from 'react'
import { Info, Plus, RefreshCw } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath } from '../../../paths.js'

/**
 * Admission applications: /school-crm/applications
 *
 * TWO ENDPOINTS HERE — #24 lists the pipeline and #17 starts a form. The table is #24's answer, so
 * it shows what the school HOLDS rather than what this page happened to create. A row opens #25 at
 * its own address, which is where the guardians, the answers and the history are.
 *
 * THE FILTERS ARE THE INDEX, IN ITS ORDER: cycle, class, status, officer. That is the worklist an
 * admission officer opens, and school_cycle_class_status_idx exists for exactly it.
 *
 * THE OFFICER FILTER RETURNS NOTHING, always, because #22 is not built and nothing assigns one.
 * The screen says so rather than leaving somebody to conclude the filter is broken.
 *
 * THE CYCLE PICKER OFFERS EVERY ROUND, labelled with its status — in the filter and in the form.
 * A cycle that is not open is CYCLE_NOT_OPEN, this module's replacement for gate 4, and a picker
 * that only offered open ones would make the single most important refusal here unreachable.
 *
 * THE CLASS IS A PICKER AND A BOX, and it needs both. The picker offers exactly the classes the
 * chosen cycle has seats for — its seat table, straight off #6, which already carries each
 * className — so the working case takes one click instead of a pasted id.
 *
 * BUT THE BOX IS WHAT GETS SENT, and it stays typeable, because the two refusals that live on this
 * field are both ids the picker cannot offer: a class of the cycle's year with no seats in it is
 * CLASS_NOT_IN_CAPACITY, and a class of a different year is CLASS_NOT_IN_CYCLE_YEAR. Telling those
 * two apart is most of what this endpoint does, so a picker alone would hide the whole point of it.
 *
 * The box said "a plain box, not a picker" until 2026-09-23. That was right about the refusals and
 * wrong to conclude the picker had to go — the two together lose nothing.
 *
 * NOTHING IS DISABLED.
 */
export default function Applications() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()

  const [open, setOpen] = useState(false)
  const [cycles, setCycles] = useState([])
  const [loading, setLoading] = useState(false)

  const [cycleFilter, setCycleFilter] = useState('')
  const [classFilter, setClassFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [officerFilter, setOfficerFilter] = useState('')
  const [fromInquiry, setFromInquiry] = useState('')
  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState('20')
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)

  const query = useMemo(() => {
    const out = { page, size: Number(size) }
    if (cycleFilter) out.admissionCycleDocsId = cycleFilter
    if (classFilter.trim()) out.appliedClassDocsId = classFilter.trim()
    if (statusFilter) out.status = statusFilter
    if (officerFilter.trim()) out.assignedAdmissionOfficerDocsId = officerFilter.trim()
    if (fromInquiry) out.fromInquiry = fromInquiry
    if (search.trim()) out.search = search.trim()
    if (sort) out.sort = sort
    return out
  }, [page, size, cycleFilter, classFilter, statusFilter, officerFilter, fromInquiry, search, sort])

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    const result = await call('list-admission-applications', {
      label: 'The pipeline', query,
    })
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, query])

  useEffect(() => { load() }, [load])
  const runSearch = () => { setPage(0); setSearch(typed) }
  const rows = data?.content ?? []

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
        title="Find an application"
        description="Every filter is optional. The first four are the order the index is built in — this round, this class, at this stage, whose desk."
        action={<EndpointTag id="list-admission-applications" name="List" />}
      >
        <div className="stack">
          <div className="field-grid">
            <Field
              label="Admission cycle"
              hint="Blank returns every round. A school works two at once during admissions, so that is the useful default."
            >
              <Select
                value={cycleFilter}
                options={[
                  { value: '', label: '— every round —' },
                  ...cycles.map((one) => ({
                    value: one.admissionCycleId, label: `${one.name} · ${one.status}`,
                  })),
                ]}
                label="Cycle filter"
                onChange={(v) => { setPage(0); setCycleFilter(v) }}
              />
            </Field>
            <Field
              label="Status"
              hint="Blank returns every stage, DRAFT and WITHDRAWN included. Everything is DRAFT today, because #19 is not built."
            >
              <Select value={statusFilter} options={STATUSES} label="Status filter"
                onChange={(v) => { setPage(0); setStatusFilter(v) }} />
            </Field>
          </div>

          <div className="field-grid">
            <Field
              label="Applied class id"
              hint="A plain box: the classes on screen depend on which cycle, and an id from another year is a legitimate thing to search for and find nothing."
            >
              <Input value={classFilter} placeholder="67aa15d9dc3f7d0011111111"
                onChange={(e) => { setPage(0); setClassFilter(e.target.value) }} />
            </Field>
            <Field
              label="Assigned officer id"
              hint="Returns NOTHING for any id — #22 assigns an officer and is not built. That is the truth, not a broken filter."
            >
              <Input value={officerFilter} placeholder="nothing assigns one yet"
                onChange={(e) => { setPage(0); setOfficerFilter(e.target.value) }} />
            </Field>
          </div>

          <div className="field-grid">
            <Field
              label="Search"
              hint="The applicant's NAME or the application NUMBER — a parent gives a name on the phone, the file carries a number. Regex-quoted, so APP/2026/09 searches for those characters."
            >
              <Input value={typed} onChange={(e) => setTyped(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') runSearch() }}
                placeholder="aarav, or APP/2026/09" />
            </Field>
            <Field
              label="Sort"
              hint="An allowlist. dateOfBirth is in this list on purpose and is NOT allowed — ordering is a read, and a child's birthday is not data to order by."
            >
              <Select value={sort} options={SORTS} label="Sort"
                onChange={(v) => { setPage(0); setSort(v) }} />
            </Field>
          </div>

          <Field
            label="Came from a lead"
            hint="true finds the forms that name an inquiry, false finds the walk-ins. Blank returns both."
          >
            <Select value={fromInquiry} options={['', 'true', 'false']} label="From inquiry"
              onChange={(v) => { setPage(0); setFromInquiry(v) }} />
          </Field>

          <div className="toolbar">
            <Button onClick={runSearch}>Search</Button>
            <span className="toolbar-spacer" />
            <Select value={size} options={SIZES} label="Page size"
              onChange={(v) => { setPage(0); setSize(v) }} />
            <Button onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <Badge>page {(data?.page ?? 0) + 1} of {data?.totalPages ?? 1}</Badge>
            <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </div>
      </Card>

      <Card
        title={data ? `${data.totalElements} application${data.totalElements === 1 ? '' : 's'}` : 'Applications'}
        description="A row is thinner than what Start gives back: no guardians, no form answers, no evidence. All three are on #25, which is not built — which is also why a row does not open anything."
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
            description="An empty page, never a 404. Open a cycle with #3 and set its seats with #4, then start a form — an application needs a cycle that is OPEN and a class that has seats."
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
                  <th>Status</th>
                  <th>From a lead</th>
                  <th>Class id</th>
                  <th>Id</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  // Opening a row is its OWN address, so it can be linked and reloaded — and #25
                  // is the only endpoint that returns the guardians, the answers and the history.
                  <tr
                    key={one.admissionApplicationId}
                    data-opens
                    onClick={() => navigate(detailPath('school', 'crm', 'applications',
                      one.admissionApplicationId))}
                  >
                    <td><span className="mono">{one.applicationNo}</span></td>
                    <td>{one.applicantName}</td>
                    <td><Badge>{one.status}</Badge></td>
                    <td>
                      {one.inquiryDocsId
                        ? <span className="mono muted">{one.inquiryDocsId}</span>
                        : <span className="muted">walked in</span>}
                    </td>
                    <td><span className="mono muted">{one.appliedClassDocsId}</span></td>
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
        onStarted={() => { setPage(0); load() }}
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
const STATUSES = ['', 'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'ADDITIONAL_INFORMATION_REQUIRED',
  'APPROVED', 'REJECTED', 'WAITLISTED', 'OFFER_ISSUED', 'OFFER_ACCEPTED', 'OFFER_DECLINED',
  'ENROLLED', 'WITHDRAWN']
const SORTS = ['', 'applicantName', 'applicantName,desc', 'applicationNo', 'status',
  'submittedAt,desc', 'createdAt,desc', 'updatedAt,desc',
  // NOT on the allowlist, on purpose — ordering is a read, and a child's birthday is not data to
  // order by. Picking it is how the 400 is reached.
  'dateOfBirth', 'guardians']
const SIZES = ['5', '20', '100']

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

  //! THE CYCLE'S SEAT TABLE, and nothing else. #6 returns it with each class's NAME already
  //! resolved, so this is one read rather than two — the year's full class list was fetched here
  //! for a while and was the wrong list: a class with no seats in this round is not something to
  //! offer, it is a refusal to reach by typing.
  const [seated, setSeated] = useState([])
  const [loadingClasses, setLoadingClasses] = useState(false)

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

  const cycle = cycles.find((one) => one.admissionCycleId === form.admissionCycleDocsId)
  const cycleYear = cycle?.academicYear ?? ''

  //! RE-READ WHEN THE CYCLE CHANGES. Every round has its own seat table, so the list of classes
  //! worth offering changes with it — and a table set by #4 after this modal opened would
  //! otherwise still show the old one.
  useEffect(() => {
    if (!open || !form.admissionCycleDocsId) { setSeated([]); return }
    let cancelled = false
    const load = async () => {
      setLoadingClasses(true)
      const full = await call('get-admission-cycle', {
        label: "The cycle's seat table",
        pathParams: { admissionCycleId: form.admissionCycleDocsId },
      })
      if (cancelled) return
      setLoadingClasses(false)
      setSeated(full.ok ? (full.bodyJson?.capacities ?? []) : [])
    }
    load()
    return () => { cancelled = true }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, form.admissionCycleDocsId])

  const hasSeats = (id) => seated.some((seat) => seat.classDocsId === id)

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

        {cycle && cycle.status !== 'OPEN' ? (
          <p className="muted">
            <Info size={12} /> <b>{cycle.name} is {cycle.status}</b>, so this will answer{' '}
            <span className="mono">409 CYCLE_NOT_OPEN</span>. Open it with #3 — which will itself
            refuse if the cycle has no seats.
          </p>
        ) : null}

        <Field
          label="Applied class"
          required
          hint={form.admissionCycleDocsId
            ? `Only the classes THIS cycle has seats for — its seat table, set by #4. A class of ${cycleYear || 'the cycle\u2019s year'} that is not in it would be refused, so it is not offered here; reach that with the box below.`
            : 'Choose a cycle first — a seat table belongs to a round, not to the school.'}
        >
          <Select
            value={form.appliedClassDocsId}
            options={[
              { value: '', label: loadingClasses
                ? 'reading the seat table…'
                : (seated.length
                  ? `${seated.length} class${seated.length === 1 ? '' : 'es'} with seats — pick one`
                  : 'this cycle has NO seats set up — #4 is what sets them') },
              //! STRAIGHT FROM #6, whose capacities already carry the resolved className. Nothing
              //! else needs reading: the ids this endpoint will accept are exactly these.
              ...seated.map((seat) => ({
                value: seat.classDocsId,
                label: `${seat.className ?? seat.classDocsId} — ${seat.totalSeats} seat`
                  + `${seat.totalSeats === 1 ? '' : 's'}`
                  + (seat.reservedSeats ? `, ${seat.reservedSeats} reserved` : ''),
              })),
            ]}
            label="Applied class"
            onChange={(value) => setForm((old) => ({ ...old, appliedClassDocsId: value }))}
          />
        </Field>

        <Field
          label="…or the class id, typed"
          required
          hint="The box is what gets sent, and it is how both of this field's refusals are reached — neither is something the picker can offer. A class of the cycle's year with no seats in it is CLASS_NOT_IN_CAPACITY; a class of a DIFFERENT year is CLASS_NOT_IN_CYCLE_YEAR."
          error={errors.appliedClassDocsId}
        >
          <Input value={form.appliedClassDocsId} error={errors.appliedClassDocsId}
            onChange={set('appliedClassDocsId')} placeholder="67aa15d9dc3f7d0011111111" />
        </Field>

        {form.appliedClassDocsId && !hasSeats(form.appliedClassDocsId) ? (
          <p className="muted">
            <Info size={12} /> <b>That id is not in this cycle&rsquo;s seat table</b>, so it will
            be refused — <span className="mono">409 CLASS_NOT_IN_CAPACITY</span> if it is a class
            of {cycleYear || 'the cycle\u2019s year'}, or{' '}
            <span className="mono">409 CLASS_NOT_IN_CYCLE_YEAR</span> if it belongs to another
            year. Which one it is, is the thing worth finding out.
          </p>
        ) : null}

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
