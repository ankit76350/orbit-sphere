import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, ClipboardCheck, Plus, RefreshCw } from 'lucide-react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath, grandchildPath } from '../../../paths.js'

/**
 * Attendance, step 3: /school-academics/attendance/{classId}/sections/{sectionNo}?date=
 *
 * ONE SECTION ON ONE DATE. Its registers (#7) are listed; when there are none, the page says so and
 * offers "Create a register" (#1). Open goes to the register's own page (#6).
 *
 * THE DATE IS IN THE ADDRESS, so a reload — or coming back from a register — lands on the same day.
 *
 * NOTHING IS DISABLED — creating a register on a holiday, or a second one, are documented refusals.
 */

/** Today in the browser's own calendar, as YYYY-MM-DD. */
const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}


export default function AttendanceSection() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id, sectionNo } = useParams()
  const [search, setSearch] = useSearchParams()
  const year = actingAcademicYear ?? ''
  const date = search.get('date') ?? today()

  const [list, setList] = useState(null)
  const [listProblem, setListProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [creating, setCreating] = useState(false)

  //! A REGISTER OPENS ON ITS OWN PAGE.
  const openRegister = (sessionId) => navigate(grandchildPath('school', 'academics', 'attendance',
    id ?? '', 'sections', sectionNo ?? '', 'registers', sessionId))

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

  useEffect(() => { loadList() }, [loadList])

  if (!actingSubdomain) return <NoSchoolChosen what="Attendance" />

  const sessions = list?.content ?? []
  const className = sessions[0]?.className

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{className ? `${className} ${sectionNo}` : `Section ${sectionNo}`}</h1>
          <p className="muted">Attendance registers in <b>{year || 'no year selected'}</b>.</p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(detailPath('school', 'academics', 'attendance', id ?? ''))}>Sections</Button>
        <Button icon={RefreshCw} onClick={loadList} busy={loading}>Refresh</Button>
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
                <tr><th>Kind</th><th>Teacher</th><th>Status</th><th className="num">Marked</th><th></th></tr>
              </thead>
              <tbody>
                {sessions.map((s) => (
                  <tr key={s.attendanceSessionDocsId}>
                    <td><b>{s.sessionType}</b>{s.periodCode ? ` · ${s.periodCode}` : ''}</td>
                    <td>{s.teacherName ?? <span className="muted">—</span>}</td>
                    <td><Badge tone={s.status === 'OPEN' ? 'good' : undefined}>{s.status}</Badge></td>
                    <td className="num">{s.markedCount}</td>
                    <td>
                      <Button icon={ClipboardCheck}
                        onClick={() => openRegister(s.attendanceSessionDocsId)}>Open</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {creating ? (
        <CreateRegister
          year={year}
          classDocsId={id ?? ''}
          sectionNo={sectionNo ?? ''}
          date={date}
          onClose={() => setCreating(false)}
          onCreated={(newId) => { setCreating(false); openRegister(newId) }}
        />
      ) : null}
    </div>
  )
}

/**
 * #1 — create a register for this section on this date. DAILY, or one period picked from that day's
 * timetable for the section (or typed). Opens the new register on success.
 *
 * PICKING A PERIOD FILLS TWO BOXES from the day's timetable: the timetable id (the whole day's
 * document) and that period's teacher. Both stay editable, so a wrong timetable id
 * (TIMETABLE_NOT_FOR_DATE) or a substitute teacher can be sent on purpose.
 *
 * THE TEACHER IS PICKED FROM ALL STAFF, with the box beside it — the box is what is sent.
 */
function CreateRegister({ year, classDocsId, sectionNo, date, onClose, onCreated }) {
  const { call } = useApi()
  const [sessionType, setSessionType] = useState('DAILY')
  const [periodCode, setPeriodCode] = useState('')
  const [timetableDocsId, setTimetableDocsId] = useState('')
  const [teacherDocsId, setTeacherDocsId] = useState('')
  const [day, setDay] = useState(null)
  const [staff, setStaff] = useState([])
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! THE SECTION'S DAY — its periods for the dropdown, and the day's timetable id. A day with no
  //! timetable leaves both empty, and the boxes still send.
  useEffect(() => {
    let live = true
    call('get-section-day', {
      label: 'Periods of the section that day',
      pathParams: { year, date, classDocsId, sectionNo },
    }).then((answer) => { if (live) setDay(answer.ok ? answer.bodyJson : null) })
    return () => { live = false }
  }, [call, year, date, classDocsId, sectionNo])

  //! EVERY STAFF MEMBER, for the teacher dropdown.
  useEffect(() => {
    let live = true
    call('list-staff', { label: 'Staff to pick a teacher from', query: { size: '100' } })
      .then((answer) => { if (live) setStaff(answer.ok ? (answer.bodyJson?.content ?? []) : []) })
    return () => { live = false }
  }, [call])

  const periods = day?.entries ?? []

  //! PICKING A PERIOD fills the day's timetable id and that period's teacher. Clearing it clears
  //! the timetable id; the teacher is left as it is.
  const pickPeriod = (code) => {
    setPeriodCode(code)
    const entry = periods.find((one) => one.periodCode === code)
    if (entry) {
      setTimetableDocsId(day?.dailyTimetableDocsId ?? '')
      setTeacherDocsId(entry.teacherDocsId ?? '')
    } else if (!code) {
      setTimetableDocsId('')
    }
  }

  //! SWITCHING TO DAILY empties the two period-only boxes, since a DAILY register takes neither.
  //! They can be typed back in to reach PERIOD_CODE_NOT_ALLOWED or TIMETABLE_DOCS_ID_NOT_ALLOWED.
  const pickType = (type) => {
    setSessionType(type)
    if (type === 'DAILY') { setPeriodCode(''); setTimetableDocsId('') }
  }

  const body = {
    attendanceDate: date,
    classDocsId,
    sectionNo,
    sessionType,
    ...(periodCode ? { periodCode } : {}),
    ...(timetableDocsId ? { timetableDocsId } : {}),
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
      description="DAILY is once for the day. PERIOD is one timetable period — picking it fills the day's timetable id and its teacher."
      endpoint={<EndpointTag id="open-attendance-session" name="Create" look="primary" pathParams={{ year }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Create</Button>}
    >
      <div className="field-grid">
        <Field label="Kind" required hint="Switching to DAILY empties the period and timetable boxes.">
          <Select value={sessionType} options={['DAILY', 'PERIOD']} onChange={pickType} />
        </Field>
        <Field label="Period" hint="For PERIOD. Picked from the section's timetable that day, or typed. Sending one with DAILY is 400 PERIOD_CODE_NOT_ALLOWED.">
          <div className="picker-pair">
            <Select value={periods.some((p) => p.periodCode === periodCode) ? periodCode : ''}
              options={['', ...periods.map((p) => ({ value: p.periodCode, label: `${p.periodCode} · ${p.startTime}–${p.endTime} · ${p.subjectName ?? p.slotType}` }))]}
              onChange={pickPeriod} />
            <Input value={periodCode} placeholder="P1" onChange={(e) => setPeriodCode(e.target.value)} />
          </div>
        </Field>
        <Field label="Timetable id" hint="The day's timetable — filled when a period is picked. Another day's id is 400 TIMETABLE_NOT_FOR_DATE; any id on DAILY is 400 TIMETABLE_DOCS_ID_NOT_ALLOWED. Empty: the server finds it.">
          <Input value={timetableDocsId} placeholder={day?.dailyTimetableDocsId ?? 'no timetable for this day'}
            onChange={(e) => setTimetableDocsId(e.target.value)} />
        </Field>
        <Field label="Teacher" hint="Filled with the period's teacher when a period is picked. Pick anyone on staff for a substitute. Empty: the period's teacher, or the class teacher for DAILY. An id that is not staff is 404 TEACHER_NOT_FOUND.">
          <div className="picker-pair">
            <Select value={staff.some((one) => one.staffDocsId === teacherDocsId) ? teacherDocsId : ''}
              options={['', ...staff.map((one) => ({ value: one.staffDocsId, label: one.fullName }))]}
              onChange={setTeacherDocsId} />
            <Input value={teacherDocsId} placeholder="defaults from the timetable"
              onChange={(e) => setTeacherDocsId(e.target.value)} />
          </div>
        </Field>
      </div>
      <p className="muted">
        <EndpointTag id="get-section-day" name="Periods" pathParams={{ year, date, classDocsId, sectionNo }} />{' '}
        <EndpointTag id="list-staff" name="Staff" query={{ size: '100' }} />
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
