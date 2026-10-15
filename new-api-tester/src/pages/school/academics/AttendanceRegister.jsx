import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, Lock, LockOpen, RefreshCw, Save } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { childPath } from '../../../paths.js'

/**
 * One attendance register, on its own page:
 * /school-academics/attendance/{classId}/sections/{sectionNo}/registers/{sessionId}
 *
 * TWO ENDPOINTS — #6 reads the register: every child who was in the section on its date, each with
 * their mark or "not marked". #2 saves marks: change a child's status (and reason, unless PRESENT),
 * then Save. Only the children changed on this page are sent. #3 locks the register — refused
 * while any child is unmarked. #4 unlocks it, with a reason that is saved on the register's
 * unlock history, shown below the children. Both buttons are at the top and open a modal: the
 * boxes on one side, the request object on the other.
 *
 * NOTHING IS DISABLED — saving with nothing changed is 400 NOTHING_TO_MARK, an absence with no
 * reason is 400 REASON_REQUIRED, and a locked register is 409 SESSION_NOT_OPEN.
 */

const STATUSES = ['', 'PRESENT', 'LATE', 'AUTHORISED_ABSENCE', 'UNAUTHORISED_ABSENCE']

const MARK_TONE = { PRESENT: 'good', LATE: 'warn', AUTHORISED_ABSENCE: undefined, UNAUTHORISED_ABSENCE: 'bad' }

export default function AttendanceRegister() {
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()
  const navigate = useNavigate()
  const { id, sectionNo, sessionId } = useParams()
  const year = actingAcademicYear ?? ''

  const [register, setRegister] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  //! WHAT HAS BEEN CHANGED ON THIS PAGE, by child: { status, reason }. Only these are sent.
  const [draft, setDraft] = useState({})
  const [saving, setSaving] = useState(false)
  const [saveResult, setSaveResult] = useState(null)
  //! WHICH MODAL IS OPEN — 'lock', 'unlock' or none. Mounted only while open, so its version box
  //! is seeded from the register as last read.
  const [modal, setModal] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain || !year) return
    setLoading(true)
    const answer = await call('get-attendance-register', {
      label: 'One register',
      pathParams: { year, attendanceSessionDocsId: sessionId ?? '' },
    })
    setLoading(false)
    if (answer.ok) { setRegister(answer.bodyJson); setProblem(null); setDraft({}) } else { setRegister(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, year, sessionId])

  //! THE MARKS TO SEND — every child changed here, with the status shown and the reason typed.
  const changedMarks = Object.entries(draft).map(([studentDocsId, one]) => ({
    studentDocsId,
    ...(one.status ? { status: one.status } : {}),
    ...(one.reason ? { reason: one.reason } : {}),
  }))

  const save = async () => {
    setSaving(true)
    const answer = await call('mark-attendance-register', {
      label: 'Save marks',
      pathParams: { year, attendanceSessionDocsId: sessionId ?? '' },
      body: { marks: changedMarks },
    })
    setSaving(false)
    setSaveResult(answer)
    //! THE ANSWER IS THE REGISTER AS IT NOW STANDS, so it replaces the page's copy.
    if (answer.ok) { setRegister(answer.bodyJson); setDraft({}) }
  }

  const statusOf = (row) => draft[row.studentDocsId]?.status ?? row.mark?.status ?? ''
  const reasonOf = (row) => draft[row.studentDocsId]?.reason ?? row.mark?.reason ?? ''
  const change = (row, key, value) => setDraft({
    ...draft,
    [row.studentDocsId]: { status: statusOf(row), reason: reasonOf(row), [key]: value },
  })

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="An attendance register" />

  const session = register?.session
  //! BACK TO THE SECTION, ON THE REGISTER'S OWN DATE — not today's, which may show nothing.
  const back = () => navigate(childPath('school', 'academics', 'attendance', id ?? '', 'sections', sectionNo ?? '')
    + (session?.attendanceDate ? `?date=${session.attendanceDate}` : ''))

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">
            {session
              ? `${session.className ?? 'Class'} ${session.sectionNo} · ${session.sessionType}${session.periodCode ? ` ${session.periodCode}` : ''}`
              : 'Attendance register'}
          </h1>
          <p className="muted">
            {session ? <>{session.attendanceDate} · {session.teacherName ?? 'no teacher'}</> : 'reading the register…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={back}>Registers</Button>
        <EndpointTag id="get-attendance-register" name="Register"
          pathParams={{ year, attendanceSessionDocsId: sessionId }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        {/* NEVER GATED ON THE STATUS: locking a locked register is 409 SESSION_NOT_OPEN, and
            unlocking an open one is 409 SESSION_NOT_LOCKED — both worth being able to send. */}
        <Button icon={Lock} onClick={() => setModal('lock')}>Lock</Button>
        <Button icon={LockOpen} onClick={() => setModal('unlock')}>Unlock</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      {register ? (
        <Card
          title={`${register.studentCount} children`}
          description={`${register.markedCount} marked · ${register.unmarkedCount} not marked`}
          action={
            <div className="btn-row">
              <EndpointTag id="mark-attendance-register" name="Mark"
                pathParams={{ year, attendanceSessionDocsId: sessionId }} />
              <EndpointTag id="lock-attendance-register" name="Lock"
                pathParams={{ year, attendanceSessionDocsId: sessionId }} />
              <EndpointTag id="unlock-attendance-register" name="Unlock"
                pathParams={{ year, attendanceSessionDocsId: sessionId }} />
              <Badge tone={session.status === 'OPEN' ? 'good' : undefined}>{session.status}</Badge>
            </div>
          }
        >
          {register.students.length === 0 ? (
            <Empty title="Nobody in this section on this date"
              description="No child's placement covered this date. Place children under Students › The roll." />
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Roll</th><th>Child</th><th>Admission no</th><th>Saved</th><th>Mark</th><th>Reason</th></tr></thead>
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
                      <td style={{ minWidth: 190 }}>
                        <Select value={statusOf(r)} options={STATUSES}
                          onChange={(v) => change(r, 'status', v)} />
                        {draft[r.studentDocsId] ? <div className="muted">changed</div> : null}
                      </td>
                      <td>
                        <Input value={reasonOf(r)}
                          placeholder={statusOf(r) && statusOf(r) !== 'PRESENT' ? 'required' : 'optional'}
                          onChange={(e) => change(r, 'reason', e.target.value)} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="toolbar">
            <Button look="primary" icon={Save} busy={saving} onClick={save}>
              Save marks{changedMarks.length ? ` (${changedMarks.length})` : ''}
            </Button>
          </div>

          {saveResult && !saveResult.ok ? (
            <div className="resp">
              <div className="resp-head">
                <span className="resp-status" data-ok="false">{saveResult.bodyJson?.code ?? saveResult.status}</span>
              </div>
              <pre className="resp-body">{saveResult.bodyJson?.message ?? saveResult.bodyText}</pre>
            </div>
          ) : saveResult?.ok ? (
            <p className="muted"><Badge tone="good">saved</Badge> {saveResult.bodyJson?.nextStep}</p>
          ) : null}

          {(session.unlocks ?? []).length ? (
            <div className="table-scroll">
              <table className="data-table">
                <thead><tr><th>Unlocked</th><th>Reason</th><th>Undid the lock of</th></tr></thead>
                <tbody>
                  {session.unlocks.map((u) => (
                    <tr key={u.unlockedAt}>
                      <td>{u.unlockedAt}{u.unlockedByDocsId ? <div className="mono muted">{u.unlockedByDocsId}</div> : null}</td>
                      <td>{u.reason}</td>
                      <td>{u.previousLockedAt ?? <span className="muted">—</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}

          <p className="muted">
            <Info size={12} /> <b>Everyone who was in the section on this date</b> — not only who is
            in it today. <b>Only the children changed here are sent</b>; re-sending a child corrects
            their mark. Every mark except PRESENT needs a reason. <b>Locking</b> makes the register final — every child
            must be marked first, and unsaved changes here are not sent with it. <b>Unlocking</b> needs
            a reason, which is kept on the register.
          </p>
        </Card>
      ) : null}

      {modal && register ? (
        <LockModal
          kind={modal}
          year={year}
          sessionId={sessionId ?? ''}
          session={register.session}
          onClose={() => setModal(null)}
          onDone={(next) => { setModal(null); setRegister(next); setDraft({}) }}
        />
      ) : null}
    </div>
  )
}

/**
 * #3 and #4 in one modal — Lock, or Unlock with its reason. The boxes on the left, the request
 * object on the right.
 *
 * THE VERSION IS TYPED AND ALWAYS SENT, seeded from the register as last read: an older number is
 * 409 CONCURRENT_MODIFICATION, an empty one 400 VALIDATION_FAILED. The reason is sent as typed —
 * empty reaches 400 REASON_REQUIRED.
 */
function LockModal({ kind, year, sessionId, session, onClose, onDone }) {
  const { call } = useApi()
  const unlocking = kind === 'unlock'
  const [version, setVersion] = useState(String(session?.version ?? ''))
  const [reason, setReason] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  const endpointId = unlocking ? 'unlock-attendance-register' : 'lock-attendance-register'
  const body = {
    ...(unlocking && reason ? { reason } : {}),
    version: version === '' ? undefined : Number(version),
  }

  const send = async () => {
    setSending(true)
    const answer = await call(endpointId, {
      label: unlocking ? 'Unlock the register' : 'Lock the register',
      pathParams: { year, attendanceSessionDocsId: sessionId },
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok) onDone(answer.bodyJson)
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={unlocking ? 'Unlock the register' : 'Lock the register'}
      description={unlocking
        ? 'Opens it again to correct it. The reason is saved on the register, with when and by whom.'
        : 'Makes it final. Every child must be marked first.'}
      endpoint={<EndpointTag id={endpointId} name={unlocking ? 'Unlock' : 'Lock'} look="primary"
        pathParams={{ year, attendanceSessionDocsId: sessionId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" icon={unlocking ? LockOpen : Lock} onClick={send} busy={sending}>
        {unlocking ? 'Unlock' : 'Lock'}
      </Button>}
    >
      <p className="muted">Now <Badge>{session?.status}</Badge></p>
      <div className="field-grid">
        {unlocking ? (
          <Field label="Reason" required hint="Why it is being opened again. Saved on the register. Empty is 400 REASON_REQUIRED.">
            <Input value={reason} placeholder="Ayan was marked absent by mistake"
              onChange={(e) => setReason(e.target.value)} />
          </Field>
        ) : null}
        <Field label="The register's version" required
          hint="Seeded from the last read. An older number is 409 CONCURRENT_MODIFICATION; empty is 400 VALIDATION_FAILED.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>
      {result && !result.ok ? (
        <div className="resp">
          <div className="resp-head"><span className="resp-status" data-ok="false">{result.bodyJson?.code ?? result.status}</span></div>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </div>
      ) : null}
    </Modal>
  )
}
