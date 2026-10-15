import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, ClipboardCheck, Info, Plus, RefreshCw } from 'lucide-react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath } from '../../../paths.js'

/**
 * Attendance, step 3 of 3: /school-academics/attendance/{classId}/sections/{sectionNo}?date=&session=
 *
 * ONE SECTION ON ONE DATE. Its registers (#7) are listed; when there are none, the page says so and
 * offers "Create a register" (#1). Clicking a register opens it below (#6): every child who was in
 * the section that day, with their mark or "not marked".
 *
 * THE DATE AND THE OPEN REGISTER ARE IN THE ADDRESS, so a reload lands on the same register.
 *
 * NOTHING IS DISABLED — creating a register on a holiday, or a second one, are documented refusals.
 */

/** Today in the browser's own calendar, as YYYY-MM-DD. */
const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const MARK_TONE = { PRESENT: 'good', LATE: 'warn', AUTHORISED_ABSENCE: undefined, UNAUTHORISED_ABSENCE: 'bad' }

export default function AttendanceSection() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id, sectionNo } = useParams()
  const [search, setSearch] = useSearchParams()
  const year = actingAcademicYear ?? ''
  const date = search.get('date') ?? today()
  const sessionId = search.get('session') ?? ''

  const [list, setList] = useState(null)
  const [listProblem, setListProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [register, setRegister] = useState(null)
  const [registerProblem, setRegisterProblem] = useState(null)
  const [creating, setCreating] = useState(false)

  const setQuery = (next) => setSearch({ date, ...(sessionId ? { session: sessionId } : {}), ...next }, { replace: true })

  //! #7 — THIS SECTION'S REGISTERS ON THIS DATE.
  const loadList = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('list-attendance-sessions', {
      label: `Registers of ${sectionNo} on ${date}`,
      pathParams: { year },
      query: { date, classDocsId: id ?? '', sectionNo: sectionNo ?? '', size: '50' },
    })
    setLoading(false)
    if (answer.ok) { setList(answer.bodyJson); setListProblem(null) } else { setList(null); setListProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, id, sectionNo, date])

  //! #6 — THE OPEN REGISTER AND ITS CHILDREN.
  const loadRegister = useCallback(async () => {
    if (!actingSubdomain || !year || !sessionId) { setRegister(null); return }
    const answer = await call('get-attendance-register', {
      label: 'One register',
      pathParams: { year, attendanceSessionDocsId: sessionId },
    })
    if (answer.ok) { setRegister(answer.bodyJson); setRegisterProblem(null) } else { setRegister(null); setRegisterProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, sessionId])

  useEffect(() => { loadList() }, [loadList])
  useEffect(() => { loadRegister() }, [loadRegister])

  if (!actingSubdomain) return <NoSchoolChosen what="Attendance" />

  const sessions = list?.content ?? []
  const className = sessions[0]?.className ?? register?.session?.className

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{className ? `${className} ${sectionNo}` : `Section ${sectionNo}`}</h1>
          <p className="muted">Attendance registers in <b>{year || 'no year selected'}</b>.</p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(detailPath('school', 'academics', 'attendance', id ?? ''))}>Sections</Button>
        <Button icon={RefreshCw} onClick={() => { loadList(); loadRegister() }} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>Create a register</Button>
      </div>

      <Card
        title="Registers"
        description="This section's registers on the chosen date."
        action={<EndpointTag id="list-attendance-sessions" name="List" pathParams={{ year }}
          query={{ date, classDocsId: id, sectionNo }} />}
      >
        <div className="field-grid">
          <Field label="Date" hint="Defaults to today.">
            <Input type="date" value={date} onChange={(e) => setSearch({ date: e.target.value }, { replace: true })} />
          </Field>
        </div>

        {listProblem ? (
          <pre className="resp-body">{listProblem.bodyJson?.message ?? listProblem.bodyText}</pre>
        ) : sessions.length === 0 ? (
          <Empty
            title="No register for this date"
            description="Nobody has opened attendance for this section on this date yet."
            action={<Button look="primary" icon={Plus} onClick={() => setCreating(true)}>Create a register</Button>}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr><th>Kind</th><th>Subject</th><th>Teacher</th><th>Status</th><th className="num">Marked</th><th></th></tr>
              </thead>
              <tbody>
                {sessions.map((s) => (
                  <tr key={s.attendanceSessionDocsId}>
                    <td><b>{s.sessionType}</b>{s.periodCode ? ` · ${s.periodCode}` : ''}</td>
                    <td>{s.subjectCode ?? <span className="muted">—</span>}</td>
                    <td>{s.teacherName ?? <span className="muted">—</span>}</td>
                    <td><Badge tone={s.status === 'OPEN' ? 'good' : undefined}>{s.status}</Badge></td>
                    <td className="num">{s.markedCount}</td>
                    <td>
                      <Button look={s.attendanceSessionDocsId === sessionId ? 'primary' : undefined}
                        icon={ClipboardCheck}
                        onClick={() => setQuery({ session: s.attendanceSessionDocsId })}>Open</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {registerProblem ? (
        <Card title={registerProblem.bodyJson?.code ?? `The server answered ${registerProblem.status}`}>
          <pre className="resp-body">{registerProblem.bodyJson?.message ?? registerProblem.bodyText}</pre>
        </Card>
      ) : null}

      {register ? (
        <Card
          title={`${register.session.sessionType}${register.session.periodCode ? ` · ${register.session.periodCode}` : ''} · ${register.session.attendanceDate}`}
          description={`${register.studentCount} children · ${register.markedCount} marked · ${register.unmarkedCount} not marked`}
          action={
            <div className="btn-row">
              <EndpointTag id="get-attendance-register" name="Register"
                pathParams={{ year, attendanceSessionDocsId: sessionId }} />
              <Badge tone={register.session.status === 'OPEN' ? 'good' : undefined}>{register.session.status}</Badge>
            </div>
          }
        >
          {register.students.length === 0 ? (
            <Empty title="Nobody in this section on this date"
              description="No child's placement covered this date. Place children under Students › The roll." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Roll</th><th>Child</th><th>Admission no</th><th>Mark</th><th>Reason</th></tr></thead>
                <tbody>
                  {register.students.map((r) => (
                    <tr key={r.studentDocsId}>
                      <td className="mono">{r.rollNo ?? '—'}</td>
                      <td>
                        {r.fullName ?? <span className="mono muted">{r.studentDocsId}</span>}
                        {r.onRoster ? null : <> <Badge tone="warn">no longer in this section</Badge></>}
                      </td>
                      <td className="mono">{r.admissionNo ?? '—'}</td>
                      <td>{r.mark ? <Badge tone={MARK_TONE[r.mark.status]}>{r.mark.status}</Badge> : <span className="muted">not marked</span>}</td>
                      <td>{r.mark?.reason ?? <span className="muted">—</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <p className="muted">
            <Info size={12} /> <b>Everyone who was in the section on this date</b> — not only who is
            in it today, so a child who moved later still appears here. Marking is the next
            endpoint (#2), not built yet.
          </p>
        </Card>
      ) : null}

      {creating ? (
        <CreateRegister
          year={year}
          classDocsId={id ?? ''}
          sectionNo={sectionNo ?? ''}
          date={date}
          onClose={() => setCreating(false)}
          onCreated={(newId) => { setCreating(false); loadList(); setQuery({ session: newId }) }}
        />
      ) : null}
    </div>
  )
}

/**
 * #1 — create a register for this section on this date. DAILY, or one period picked from that day's
 * timetable for the section (or typed). Opens the new register on success.
 */
function CreateRegister({ year, classDocsId, sectionNo, date, onClose, onCreated }) {
  const { call } = useApi()
  const [sessionType, setSessionType] = useState('DAILY')
  const [periodCode, setPeriodCode] = useState('')
  const [teacherDocsId, setTeacherDocsId] = useState('')
  const [periods, setPeriods] = useState([])
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! THE SECTION'S PERIODS THAT DAY, for the PERIOD dropdown. A day with no timetable leaves it
  //! empty, and the box still sends.
  useEffect(() => {
    let live = true
    call('get-section-day', {
      label: 'Periods of the section that day',
      pathParams: { year, date, classDocsId, sectionNo },
    }).then((answer) => { if (live) setPeriods(answer.ok ? (answer.bodyJson?.entries ?? []) : []) })
    return () => { live = false }
  }, [call, year, date, classDocsId, sectionNo])

  const body = {
    attendanceDate: date,
    classDocsId,
    sectionNo,
    sessionType,
    ...(periodCode ? { periodCode } : {}),
    ...(teacherDocsId ? { teacherDocsId } : {}),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('open-attendance-session', { label: 'Create a register', pathParams: { year }, body })
    setSending(false)
    setResult(answer)
    if (answer.ok) onCreated(answer.bodyJson.attendanceSessionDocsId)
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={`Create a register — ${sectionNo}, ${date}`}
      description="DAILY is once for the day. PERIOD is one timetable period; its subject and teacher come from the timetable."
      endpoint={<EndpointTag id="open-attendance-session" name="Create" look="primary" pathParams={{ year }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Create</Button>}
    >
      <div className="field-grid">
        <Field label="Kind" required>
          <Select value={sessionType} options={['DAILY', 'PERIOD']} onChange={setSessionType} />
        </Field>
        <Field label="Period" hint="For PERIOD. Picked from the section's timetable that day, or typed. Sending one with DAILY is 400 PERIOD_CODE_NOT_ALLOWED.">
          <div className="picker-pair">
            <Select value={periods.some((p) => p.periodCode === periodCode) ? periodCode : ''}
              options={['', ...periods.map((p) => ({ value: p.periodCode, label: `${p.periodCode} · ${p.startTime}–${p.endTime} · ${p.subjectName ?? p.slotType}` }))]}
              onChange={setPeriodCode} />
            <Input value={periodCode} placeholder="P1" onChange={(e) => setPeriodCode(e.target.value)} />
          </div>
        </Field>
        <Field label="Teacher id" hint="Optional. Empty: the period's teacher, or the class teacher for DAILY.">
          <Input value={teacherDocsId} placeholder="defaults from the timetable" onChange={(e) => setTeacherDocsId(e.target.value)} />
        </Field>
      </div>
      <p className="muted">
        <EndpointTag id="get-section-day" name="Periods" pathParams={{ year, date, classDocsId, sectionNo }} />
      </p>
      {result && !result.ok ? (
        <div className="resp">
          <div className="resp-head"><span className="resp-status" data-ok="false">{result.bodyJson?.code ?? result.status}</span></div>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </div>
      ) : null}
    </Modal>
  )
}
