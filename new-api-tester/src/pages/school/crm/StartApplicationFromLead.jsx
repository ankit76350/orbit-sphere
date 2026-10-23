import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Info, Plus, RefreshCw, Send } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath, screenPath } from '../../../paths.js'

/**
 * A lead becoming an application — #14, then #6, then #17, as one page.
 *
 * WHY A PAGE AND NOT A MODAL. It asks two things in order: which round, and then a whole form
 * built out of the answer. A modal would lose both on a refresh and could not be linked to; this
 * has an address of its own, so a half-finished conversion survives a reload and a reviewer can be
 * sent straight to it.
 *
 * WHAT IS CARRIED OVER, and what is not. The child's name, date of birth and gender come straight
 * off the lead; so do the guardians, which are the same shape on both sides. The class does NOT:
 * a lead's `interestedClassDocsId` is what a family said on the phone, and the round may not have
 * seats for it — so it is offered as a suggestion and checked against the cycle's seat table.
 *
 * THE INQUIRY LINK IS THE POINT. `inquiryDocsId` is what makes this a conversion rather than a
 * second record of the same child, and it is why the lead moves to APPLICATION_STARTED as a side
 * effect. One inquiry gets one application per cycle — a second is APPLICATION_ALREADY_EXISTS.
 *
 * NOTHING IS DISABLED. The cycle picker offers every round with its status, because CYCLE_NOT_OPEN
 * is this module's replacement for gate 4 and the most important refusal on the page. A guardian
 * with no name or no relation is left in and sent, because that is a documented 400.
 */
export default function StartApplicationFromLead() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [lead, setLead] = useState(null)
  const [cycles, setCycles] = useState([])
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  //! STEP ONE IS A CHOICE, STEP TWO IS THE FORM. Held as one state rather than a wizard object:
  //! choosing a cycle is what reveals the form, and clearing it takes you back.
  const [admissionCycleDocsId, setCycle] = useState('')

  //! THE CHOSEN ROUND IN FULL — its seat table and its questions. #5's row carries neither.
  const [round, setRound] = useState(null)
  const [loadingRound, setLoadingRound] = useState(false)

  const [form, setForm] = useState(null)
  const [guardians, setGuardians] = useState([])
  const [answerRows, setAnswerRows] = useState([])
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const [one, rounds] = await Promise.all([
      call('get-inquiry', { label: 'The lead being converted', pathParams: { inquiryId: id ?? '' } }),
      call('list-admission-cycles', { label: 'Which round to apply into', query: { size: '100' } }),
    ])
    setLoading(false)
    setCycles(rounds.ok ? (rounds.bodyJson?.content ?? []) : [])
    if (one.ok) { setLead(one.bodyJson); setProblem(null) } else { setProblem(one) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  //! THE FORM IS BUILT WHEN A CYCLE IS CHOSEN, not before — the questions belong to the round, and
  //! so does the seat table the class is picked from. Choosing a different round rebuilds it,
  //! because the other round asks different questions and has different seats.
  useEffect(() => {
    if (!admissionCycleDocsId || !lead) { setRound(null); setForm(null); return }
    let cancelled = false
    const load = async () => {
      setLoadingRound(true)
      const full = await call('get-admission-cycle', {
        label: "The round's seats and questions",
        pathParams: { admissionCycleId: admissionCycleDocsId },
      })
      if (cancelled) return
      setLoadingRound(false)
      const cycle = full.ok ? full.bodyJson : null
      setRound(cycle)

      //! EVERYTHING THE LEAD ALREADY KNOWS. A conversion that made somebody retype the child's
      //! name is a conversion nobody would use.
      setForm({
        applicantName: lead.prospectiveStudentName ?? '',
        dateOfBirth: lead.dateOfBirth ?? '',
        gender: lead.gender ?? 'MALE',
        //! THE CLASS IS A SUGGESTION, NOT A CARRY-OVER. What a family said on the phone is not
        //! necessarily a class this round has seats for, and #17 checks it against the seat table.
        appliedClassDocsId: lead.interestedClassDocsId ?? '',
      })
      setGuardians((lead.guardians ?? []).map((one) => ({
        fullName: one.fullName ?? '',
        relation: one.relation ?? '',
        phoneNumber: one.phoneNumber ?? '',
        emailAddress: one.emailAddress ?? '',
        primaryContact: one.primaryContact === true,
      })))
      setAnswerRows((cycle?.questions ?? []).map((q) => ({ question: q.question, answer: '' })))
      setErrors({})
      setRefused(null)
    }
    load()
    return () => { cancelled = true }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [admissionCycleDocsId, lead?.inquiryId])

  const back = () => navigate(detailPath('school', 'crm', 'inquiries', id))

  if (!actingSubdomain) return <NoSchoolChosen what="A lead" />

  const seated = round?.capacities ?? []
  const chosen = cycles.find((one) => one.admissionCycleId === admissionCycleDocsId)

  //! THE ROWS FOLDED INTO THE MAP #17 TAKES. A row with no question is left out — an empty key
  //! matches no question — and an empty ANSWER is kept, because a question asked and not answered
  //! is a real thing to record.
  const answerPairs = answerRows.filter((row) => row.question.trim() !== '')
  const answers = answerPairs.length
    ? Object.fromEntries(answerPairs.map((row) => [row.question.trim(), row.answer]))
    : null

  const body = form ? {
    admissionCycleDocsId,
    //! WHAT MAKES THIS A CONVERSION. Without it #17 writes a second record of the same child and
    //! the lead never moves to APPLICATION_STARTED.
    inquiryDocsId: lead?.inquiryId ?? '',
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
    ...(answers ? { formAnswers: answers } : {}),
  } : {}

  //! WHAT WILL BE REFUSED, said before the button is pressed. Each of these is a documented 400 or
  //! 409 and each stays SENDABLE — the point is to know which one is coming, not to be stopped.
  const willRefuse = []
  if (chosen && chosen.status !== 'OPEN') {
    willRefuse.push(`409 CYCLE_NOT_OPEN — this round is ${chosen.status}`)
  }
  if (guardians.length === 0) willRefuse.push('400 VALIDATION_FAILED — an application needs a guardian')
  if (guardians.some((one) => !one.fullName.trim() || !one.relation)) {
    willRefuse.push('400 VALIDATION_FAILED — a guardian with no name or no relation')
  }
  if (form && !form.appliedClassDocsId) willRefuse.push('400 VALIDATION_FAILED — no applied class')
  if (form?.appliedClassDocsId
    && seated.length > 0
    && !seated.some((seat) => seat.classDocsId === form.appliedClassDocsId)) {
    willRefuse.push('409 CLASS_NOT_IN_CAPACITY — that class has no seats in this round')
  }

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-admission-application', {
      label: `Start an application for ${lead?.prospectiveStudentName ?? 'this lead'}`,
      body,
    })
    setSaving(false)
    if (result.ok) {
      navigate(detailPath('school', 'crm', 'applications', result.bodyJson.admissionApplicationId))
      return
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code) setRefused(result.bodyJson)
  }

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Start an application</h1>
          <p className="muted">
            from <span className="mono">{lead?.inquiryNo ?? id}</span>
            {lead ? ` · ${lead.prospectiveStudentName}` : ' · reading the lead…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={back}>Back to the lead</Button>
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {/* THE THREE READS THIS PAGE MAKES, named where they are made. #14 is the lead, #5 is the
          list of rounds, and #6 is the chosen round in full — its seat table and its questions,
          neither of which a #5 row carries. */}
      <p className="muted">
        <EndpointTag id="get-inquiry" name="The lead being converted"
          pathParams={{ inquiryId: id ?? '' }} />
        {' '}
        <EndpointTag id="get-admission-cycle" name="The round's seats and questions"
          pathParams={{ admissionCycleId: admissionCycleDocsId }} />
      </p>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      {lead ? (
        <>
          <Card
            title="1 · Which round"
            description="Every cycle is offered, with its status. Only an OPEN one is accepted — anything else is 409 CYCLE_NOT_OPEN, which is this module's replacement for gate 4 and the thing most worth seeing fail."
            action={<EndpointTag id="list-admission-cycles" name="Rounds" query={{ size: '100' }} />}
          >
            <Field label="Admission cycle" required>
              <Select
                value={admissionCycleDocsId}
                options={[
                  { value: '', label: '— choose a cycle —' },
                  ...cycles.map((one) => ({
                    value: one.admissionCycleId,
                    label: `${one.name} · ${one.academicYear} · ${one.status}`,
                  })),
                ]}
                label="Admission cycle"
                onChange={setCycle}
              />
            </Field>

            {lead.academicYear && chosen && chosen.academicYear !== lead.academicYear ? (
              <p className="muted">
                <Info size={12} /> <b>This round admits into {chosen.academicYear}, and the lead
                asked about {lead.academicYear}.</b> Nothing refuses that — the year on an
                application comes from the <b>cycle</b>, not from the lead — but it is worth
                knowing which one the family will end up in.
              </p>
            ) : null}
          </Card>

          {!admissionCycleDocsId ? (
            <Card title="2 · The form">
              <Empty
                title="Choose a round first"
                description="The questions and the seat table both belong to a round, so there is no form to fill in until one is chosen."
              />
            </Card>
          ) : null}

          {admissionCycleDocsId && form ? (
            <Card
              title="2 · The form, filled in from the lead"
              description="The child's details and the guardians are carried over. Correct anything that was misheard on the phone — this is the last point before the form becomes the family's declaration."
              action={<EndpointTag id="create-admission-application" name="Start" look="primary" />}
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

                {willRefuse.length ? (
                  <p className="muted">
                    <Info size={12} /> <b>As it stands this will be refused:</b>{' '}
                    {willRefuse.join(' · ')}. <b>Send it anyway to read the refusal</b> — nothing
                    here is switched off.
                  </p>
                ) : null}

                <div className="field-grid">
                  <Field label="Applicant name" required error={errors.applicantName}>
                    <Input value={form.applicantName} error={errors.applicantName}
                      onChange={(e) => setForm((old) => ({ ...old, applicantName: e.target.value }))} />
                  </Field>
                  <Field label="Date of birth" required error={errors.dateOfBirth}
                    hint="Required here, unlike on a lead. Must be in the past.">
                    <Input type="date" value={form.dateOfBirth} error={errors.dateOfBirth}
                      onChange={(e) => setForm((old) => ({ ...old, dateOfBirth: e.target.value }))} />
                  </Field>
                  <Field label="Gender" required error={errors.gender}
                    hint="Required here, unlike on a lead.">
                    <Select
                      value={form.gender}
                      options={['MALE', 'FEMALE', 'OTHER'].map((one) => ({ value: one, label: one }))}
                      label="Gender"
                      onChange={(value) => setForm((old) => ({ ...old, gender: value }))}
                    />
                  </Field>
                </div>

                {/* THE CLASS IS THE ONE THING NOT CARRIED OVER AS FACT. The picker is the round's
                    seat table — a class outside it is CLASS_NOT_IN_CAPACITY — and the box below is
                    how a class of ANOTHER year is sent, which is CLASS_NOT_IN_CYCLE_YEAR. */}
                <Field
                  label="Applied class"
                  required
                  hint="The classes THIS round has seats for. The lead's interested class is pre-selected when the round has seats for it — what a family said on the phone is not always a class the round is taking forms for."
                  error={errors.appliedClassDocsId}
                >
                  <Select
                    value={form.appliedClassDocsId}
                    options={[
                      { value: '', label: loadingRound
                        ? 'reading the seat table…'
                        : (seated.length
                          ? `${seated.length} seated class${seated.length === 1 ? '' : 'es'} — pick one`
                          : 'this round has NO seats set up — #4 sets them') },
                      ...seated.map((seat) => ({
                        value: seat.classDocsId,
                        label: `${seat.className ?? seat.classDocsId}`
                          + (seat.classDocsId === lead.interestedClassDocsId
                            ? ' — what the family asked about' : ''),
                      })),
                    ]}
                    label="Applied class"
                    onChange={(value) => setForm((old) => ({ ...old, appliedClassDocsId: value }))}
                  />
                </Field>
                <Field
                  label="…or the class id, typed"
                  hint="What is actually sent. A class of the cycle's year with no seats in it is 409 CLASS_NOT_IN_CAPACITY; a class of a DIFFERENT year is 409 CLASS_NOT_IN_CYCLE_YEAR. Neither is something the picker can offer."
                >
                  <Input value={form.appliedClassDocsId}
                    onChange={(e) => setForm((old) => ({ ...old, appliedClassDocsId: e.target.value }))}
                    placeholder="67aa15d9dc3f7d0011111111" />
                </Field>

                {/* THE GUARDIANS CAME OFF THE LEAD, where every field is optional — and an
                    application needs a name and a relation on each. A row that is missing one is
                    left exactly as it is and sent, because that refusal is a documented 400. */}
                <Field
                  label={`Guardians — ${guardians.length}`}
                  hint="Carried over from the lead. An application needs AT LEAST ONE, and each needs a name and a relation — a lead needs neither, so a row may arrive here half-filled. Removing them all is a documented 400."
                  error={errors.guardians}
                >
                  {guardians.length === 0 ? (
                    <p className="muted">
                      This lead has no guardians. An application needs one —{' '}
                      <b>add a row, or send it empty to read 400 VALIDATION_FAILED</b>.
                    </p>
                  ) : (
                    <div className="stack">
                      {guardians.map((one, at) => (
                        <div key={at} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <span className="muted mono">{at + 1}</span>
                          <Input
                            value={one.fullName}
                            placeholder="Name — required on an application"
                            onChange={(e) => setGuardians((old) => old.map((g, i) =>
                              i === at ? { ...g, fullName: e.target.value } : g))}
                          />
                          <Select
                            value={one.relation}
                            options={[
                              { value: '', label: 'no relation — a documented 400' },
                              ...['FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT',
                                'LEGAL_GUARDIAN', 'SIBLING', 'OTHER']
                                .map((r) => ({ value: r, label: r })),
                            ]}
                            label="Relation"
                            onChange={(value) => setGuardians((old) => old.map((g, i) =>
                              i === at ? { ...g, relation: value } : g))}
                          />
                          <Input
                            value={one.phoneNumber}
                            placeholder="Phone"
                            onChange={(e) => setGuardians((old) => old.map((g, i) =>
                              i === at ? { ...g, phoneNumber: e.target.value } : g))}
                          />
                          <label className="muted" style={{
                            display: 'flex', alignItems: 'center', gap: 6, whiteSpace: 'nowrap' }}>
                            <input type="checkbox" checked={one.primaryContact}
                              onChange={(e) => setGuardians((old) => old.map((g, i) =>
                                i === at ? { ...g, primaryContact: e.target.checked } : g))} />
                            primary
                          </label>
                          <Button onClick={() => setGuardians((old) =>
                            old.filter((_, i) => i !== at))}>
                            Remove
                          </Button>
                        </div>
                      ))}
                    </div>
                  )}
                  <div style={{ marginTop: 8 }}>
                    <Button icon={Plus} onClick={() => setGuardians((old) => [...old, {
                      fullName: '', relation: 'FATHER', phoneNumber: '', emailAddress: '',
                      primaryContact: old.length === 0,
                    }])}>
                      Add a guardian
                    </Button>
                  </div>
                </Field>

                {/* THE ROUND'S OWN QUESTIONS, one row each with the answer beside it. Keyed by the
                    wording, which is what #17 stores. */}
                <Field
                  label={`Form answers — ${answerPairs.length}`}
                  hint="The questions THIS round asks, with the answer beside each. The map that is sent is built from these rows and keyed by the question itself. Nothing checks the keys, and a required question can still be left unanswered — that check belongs to #19."
                >
                  {answerRows.length === 0 ? (
                    <p className="muted">
                      This round asks nothing beyond the fixed fields. Add a row to send an answer
                      anyway — nothing checks the keys against the round.
                    </p>
                  ) : (
                    <div className="stack">
                      {answerRows.map((row, at) => {
                        const asked = (round?.questions ?? [])
                          .find((q) => q.question === row.question.trim())
                        return (
                          <div key={at} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <span className="muted mono">{at + 1}</span>
                            <Input
                              value={row.question}
                              placeholder="The question, which is the key"
                              onChange={(e) => setAnswerRows((old) => old.map((one, i) =>
                                i === at ? { ...one, question: e.target.value } : one))}
                            />
                            <Input
                              value={row.answer}
                              placeholder="The answer, which is the value"
                              onChange={(e) => setAnswerRows((old) => old.map((one, i) =>
                                i === at ? { ...one, answer: e.target.value } : one))}
                            />
                            <span className="muted" style={{ whiteSpace: 'nowrap' }}>
                              {asked
                                ? (asked.required ? 'required' : 'optional')
                                : 'not asked by this round'}
                            </span>
                            <Button onClick={() => setAnswerRows((old) =>
                              old.filter((_, i) => i !== at))}>
                              Remove
                            </Button>
                          </div>
                        )
                      })}
                    </div>
                  )}
                  <div style={{ marginTop: 8 }}>
                    <Button icon={Plus} onClick={() => setAnswerRows((old) =>
                      [...old, { question: '', answer: '' }])}>
                      Add a question and answer
                    </Button>
                  </div>
                </Field>

                <p className="muted">
                  <Info size={12} /> <b>The lead is linked, not copied.</b>{' '}
                  <span className="mono">inquiryDocsId</span> goes with the form, which is what
                  moves this lead to <span className="mono">APPLICATION_STARTED</span> as a side
                  effect — nothing asked it to. <b>One inquiry gets one application per round</b>:
                  a second is <span className="mono">409 APPLICATION_ALREADY_EXISTS</span>, and the
                  same lead in a DIFFERENT round is allowed.
                </p>

                <p className="muted">
                  <Info size={12} /> <b>It is created as a DRAFT.</b> What the family declared is
                  not frozen until <b>#19</b> submits it, so everything here can still be corrected
                  with <b>#18</b> afterwards — and nothing can be corrected once it is sent.
                </p>

                <div className="toolbar">
                  <span className="toolbar-spacer" />
                  <Button look="primary" icon={Send} busy={saving} onClick={submit}>
                    Create the application
                  </Button>
                </div>
              </div>
            </Card>
          ) : null}

          <Card title="What is carried over" description="So it is clear what came from where.">
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr><th>On the application</th><th>Comes from</th></tr>
                </thead>
                <tbody>
                  <tr><td>applicantName</td><td>the lead&rsquo;s prospectiveStudentName</td></tr>
                  <tr><td>dateOfBirth · gender</td><td>the lead, where it has them</td></tr>
                  <tr><td>guardians</td><td>the lead — same shape, but a name and a relation become required</td></tr>
                  <tr><td>appliedClassDocsId</td><td><b>the round&rsquo;s seat table</b>, with the lead&rsquo;s interested class pre-selected when it is in there</td></tr>
                  <tr><td>formAnswers</td><td><b>the round&rsquo;s questions</b> — the lead has none</td></tr>
                  <tr><td>inquiryDocsId</td><td>this lead, which is what makes it a conversion</td></tr>
                  <tr><td>academicYear</td><td><b>the cycle</b>, never the lead</td></tr>
                  <tr><td>status</td><td>always <span className="mono">DRAFT</span> — not accepted on the request</td></tr>
                </tbody>
              </table>
            </div>
          </Card>
        </>
      ) : null}

      {!lead && !problem && !loading ? (
        <Card title="No lead">
          <Empty
            title="That lead could not be read"
            description="#14 answered nothing. Go back to the worklist and open one from there."
            action={<Button onClick={() => navigate(screenPath('school', 'crm', 'inquiries'))}>
              The worklist
            </Button>}
          />
        </Card>
      ) : null}
    </div>
  )
}
