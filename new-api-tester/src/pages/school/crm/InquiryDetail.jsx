import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, Pencil, Plus, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from './admissionDates.js'
import { screenPath } from '../../../paths.js'

/**
 * One lead: /school-crm/inquiries/{id}
 *
 * TWO ENDPOINTS — #14 reads one lead and #9 corrects it. The page exists because three things are
 * on it that a worklist row cannot carry: the notes, where the lead came from, and the timeline
 * itself rather than a count of it. Correcting belongs here for the same reason it belongs on the
 * application's page: what you are editing is what this page shows.
 *
 * THE CORRECT BUTTON IS NEVER SWITCHED OFF BY STATUS, and that is not this screen being lax — #9
 * has no status gate at all. A lead is the school's own notes about a phone call, not a
 * declaration the family signed, so a LOST one can still have a misspelt name put right. The
 * difference from #18 is worth reading on the page rather than discovering by pressing.
 *
 * THE TIMELINE IS THE REASON THIS PAGE EXISTS, and today it is almost always empty — #10 logs a
 * follow-up and is not built, so every lead reads back with none. The page says that rather than
 * showing a bare empty state, because "no calls yet" and "no endpoint to log a call" look
 * identical and only one of them is something the person can act on.
 *
 * AN ENTRY WITH NO NAME AGAINST IT IS SHOWN LOUDLY, not tidied away. #14 resolves the names and
 * leaves one absent when that person has left the school — the call still happened, and a screen
 * that hid the entry would be hiding a real thing about this family.
 *
 * OLDEST FIRST, because a conversation reads forwards. The question being asked of a timeline is
 * "what have we already told them", and the server sorts it rather than trusting the array.
 *
 * NOTHING IS DISABLED. Refresh always sends, and an id that is not this school's is a documented
 * 404 worth being able to reach by editing the address bar.
 */

const TONE = { APPLICATION_SUBMITTED: 'good', LOST: 'bad', CLOSED: 'bad', NEW: 'warn' }
const GENDERS = ['', 'MALE', 'FEMALE', 'OTHER']
const RELATIONS = ['', 'FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT',
  'LEGAL_GUARDIAN', 'SIBLING', 'OTHER']
const BLANK_GUARDIAN = { fullName: '', relation: '', phoneNumber: '', emailAddress: '' }

export default function InquiryDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [lead, setLead] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [correcting, setCorrecting] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-inquiry', {
      label: 'One lead with its timeline',
      pathParams: { inquiryId: id ?? '' },
    })
    setLoading(false)
    if (result.ok) { setLead(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  const back = () => navigate(screenPath('school', 'crm', 'inquiries'))

  if (!actingSubdomain) return <NoSchoolChosen what="A lead" />

  const timeline = lead?.followUps ?? []
  const guardians = lead?.guardians ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{lead?.prospectiveStudentName ?? 'Lead'}</h1>
          <p className="muted">
            <span className="mono">{lead?.inquiryNo ?? id}</span>
            {lead ? ` · ${lead.academicYear}` : ' · reading the lead…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={back}>The worklist</Button>
        <EndpointTag id="get-inquiry" name="Read" pathParams={{ inquiryId: id ?? '' }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <EndpointTag id="update-inquiry" name="Correct" look="primary"
          pathParams={{ inquiryId: id ?? '' }} />
        <Button look="primary" icon={Pencil} onClick={() => setCorrecting(true)}>Correct it</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
          <p className="muted">
            <Info size={12} /> <b>Another school&rsquo;s lead is a 404, not a 403.</b> It is a real
            id, and saying which would confirm that another tenant&rsquo;s lead exists. The read is
            scoped by school <i>in the query</i>, never checked after.
          </p>
        </Card>
      ) : null}

      {lead ? (
        <>
          <Card
            title="The lead"
            description="Everything a worklist row leaves off."
            action={
              <>
                <Badge tone={TONE[lead.status]}>{lead.status}</Badge>
                {lead.overdue ? <> <Badge tone="bad">late</Badge></> : null}
              </>
            }
          >
            <div className="dl">
              <div>
                <span className="dl-term">Inquiry no</span>
                <span className="dl-value mono">{lead.inquiryNo}</span>
              </div>
              <div>
                <span className="dl-term">Academic year</span>
                <span className="dl-value mono">{lead.academicYear}</span>
              </div>
              <div>
                <span className="dl-term">Date of birth</span>
                <span className="dl-value" data-empty={!lead.dateOfBirth}>
                  {lead.dateOfBirth}
                </span>
              </div>
              <div>
                <span className="dl-term">Gender</span>
                <span className="dl-value" data-empty={!lead.gender}>{lead.gender}</span>
              </div>
              <div>
                <span className="dl-term">Interested in</span>
                <span className="dl-value">
                  {lead.interestedClassName
                    ?? (lead.interestedClassDocsId
                      ? <span className="muted">that class is gone</span>
                      : <span className="muted">nothing in mind</span>)}
                </span>
              </div>
              <div>
                <span className="dl-term">With</span>
                <span className="dl-value">
                  {lead.assignedCounselorName
                    ?? (lead.assignedCounselorDocsId
                      ? <span className="muted">no longer staff</span>
                      : <span className="muted">nobody yet</span>)}
                </span>
              </div>
              <div>
                <span className="dl-term">Chase by</span>
                <span className="dl-value" title={lead.nextFollowUpAt}>
                  {lead.nextFollowUpAt
                    ? readable(lead.nextFollowUpAt)
                    : <span className="muted">nobody promised a date</span>}
                </span>
              </div>
              <div>
                <span className="dl-term">Source</span>
                <span className="dl-value" data-empty={!lead.source}>{lead.source}</span>
              </div>
              <div>
                <span className="dl-term">Captured</span>
                <span className="dl-value" title={lead.createdAt}>
                  {readable(lead.createdAt)}
                </span>
              </div>
              <div>
                <span className="dl-term">Version</span>
                <span className="dl-value mono">{lead.version}</span>
              </div>
              {lead.sourceDetails ? (
                <div className="dl-wide">
                  <span className="dl-term">Where they heard about the school</span>
                  <span className="dl-value">{lead.sourceDetails}</span>
                </div>
              ) : null}
              {lead.notes ? (
                <div className="dl-wide">
                  <span className="dl-term">Notes</span>
                  <span className="dl-value">{lead.notes}</span>
                </div>
              ) : null}
              {lead.lostReason ? (
                <div className="dl-wide">
                  <span className="dl-term">Why it was lost</span>
                  <span className="dl-value">{lead.lostReason}</span>
                </div>
              ) : null}
            </div>

            <p className="muted">
              <Info size={12} /> <b>Overdue is two conditions, not one.</b> Past its chase date{' '}
              <i>and</i> not <span className="mono">LOST</span> or{' '}
              <span className="mono">CLOSED</span> — a lead somebody gave up on last month has a
              past date too, and nobody owes it a phone call. A lead with no date at all is never
              late: <span className="mono">$lt</span> does not match a field that is not there.
            </p>
          </Card>

          <Card
            title={`Guardians — ${guardians.length}`}
            description="Every field on a lead's guardian is optional, unlike an application's. The front desk writes down a first name and a phone number, and a record that refused that would refuse the call."
            action={<Badge>{guardians.length}</Badge>}
          >
            {guardians.length === 0 ? (
              <Empty
                title="Nobody left their details"
                description="A walk-in who gives a child's name and leaves is a real lead. #15 finds a family again by phone or email, and it can only find the ones who left one."
              />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Relation</th>
                      <th>Phone</th>
                      <th>Email</th>
                      <th>Primary</th>
                    </tr>
                  </thead>
                  <tbody>
                    {guardians.map((one, index) => (
                      <tr key={index}>
                        <td>{one.fullName ?? <span className="muted">not said</span>}</td>
                        <td>{one.relation ?? <span className="muted">not said</span>}</td>
                        <td className="mono">
                          {one.phoneNumber ?? <span className="muted">none</span>}
                        </td>
                        <td className="mono">
                          {one.emailAddress ?? <span className="muted">none</span>}
                        </td>
                        <td>{one.primaryContact ? <Badge tone="good">yes</Badge> : ''}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <p className="muted">
              <Info size={12} /> <b>The worklist shows one of these numbers, not all of them.</b>{' '}
              The primary guardian&rsquo;s, or the first one with a number — a row that showed
              nothing because the first guardian happened to have no phone would be a row nobody
              can use.
            </p>
          </Card>

          <Card
            title={`The timeline — ${lead.followUpCount}`}
            description="Oldest first, because a conversation reads forwards. Sorted by the server rather than trusted: a $push is not a promise about order once anything else touches the array."
            action={<Badge>{lead.followUpCount}</Badge>}
          >
            {timeline.length === 0 ? (
              <Empty
                title="Nothing has been logged"
                description="#10 logs a follow-up and sets the next chase date, and it is not built. So this is empty for every lead in the system — not because nobody has rung, but because nothing can record it yet."
              />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>When</th>
                      <th>Who</th>
                      <th>Channel</th>
                      <th>Moved it to</th>
                      <th>Note</th>
                      <th>Next</th>
                    </tr>
                  </thead>
                  <tbody>
                    {timeline.map((one, index) => (
                      <tr key={index}>
                        <td title={one.recordedAt}>
                          {one.recordedAt
                            ? readable(one.recordedAt)
                            : <span className="muted">no date on it</span>}
                        </td>
                        <td>
                          {one.counselorName
                            ?? (one.counselorDocsId
                              ? <span className="muted">no longer staff</span>
                              : <span className="muted">not recorded</span>)}
                        </td>
                        <td>{one.communicationChannel
                          ?? <span className="muted">not said</span>}</td>
                        <td>{one.status
                          ? <Badge tone={TONE[one.status]}>{one.status}</Badge>
                          : <span className="muted">left as it was</span>}</td>
                        <td>{one.note ?? <span className="muted">nothing written</span>}</td>
                        <td title={one.nextFollowUpAt}>
                          {one.nextFollowUpAt
                            ? readable(one.nextFollowUpAt)
                            : <span className="muted">none set</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <p className="muted">
              <Info size={12} /> <b>An entry whose author has left is kept and marked.</b> The call
              they made still happened; dropping the row or inventing a name would hide that. The
              lookup is school-scoped too, so another school&rsquo;s real staff id is not named
              either — and the whole timeline costs <b>one query</b>, not one per entry.
            </p>
          </Card>

          <Card title="What happens next" description="#14 says it in words.">
            <pre className="resp-body">{lead.nextStep}</pre>
            <p className="muted">
              <Info size={12} /> <b>A read runs no gates.</b> A suspended school still owes this
              family a call back, so hiding the lead would lose them exactly when it matters. #9,
              which corrects it, runs both — it is a write.
            </p>
          </Card>
        </>
      ) : null}

      {correcting && lead ? (
        <CorrectLead
          lead={lead}
          onClose={() => setCorrecting(false)}
          onCorrected={load}
        />
      ) : null}
    </div>
  )
}

/**
 * #9's modal.
 *
 * ONLY WHAT DIFFERS FROM WHAT WAS READ IS SENT, like #18's. That is what makes the preview panel
 * readable: a PATCH whose body carried every field would say nothing about what changed.
 *
 * AND IT IS WHAT MAKES CLEARING WORK BY ITSELF. Empty a box that had something in it and the
 * difference is "", which is exactly what #9 reads as "take it off". #18 needs separate controls
 * for the same job because its lists cannot be cleared at all; here the box IS the control, and
 * the hint says so.
 *
 * THE GUARDIAN ROWS ARE BEHIND A SWITCH, because sending them at all replaces the lot. A screen
 * that sent them on every correction would silently rewrite the family every time somebody fixed
 * a spelling.
 *
 * IT SAYS WHAT THE CALL WILL DO BEFORE IT HAPPENS, in one case: moving the year while leaving a
 * class behind is a refusal about a field the body never mentions, which is the single most
 * surprising thing this endpoint does. NOTHING IS SWITCHED OFF — the warning is a sentence, and
 * the button still sends, because reading the refusal is the point of this app.
 */
function CorrectLead({ lead, onClose, onCorrected }) {
  const { call } = useApi()
  const stored = lead

  const [prospectiveStudentName, setName] = useState(stored.prospectiveStudentName ?? '')
  const [academicYear, setYear] = useState(stored.academicYear ?? '')
  const [dateOfBirth, setDob] = useState(stored.dateOfBirth ?? '')
  const [gender, setGender] = useState(stored.gender ?? '')
  const [interestedClassDocsId, setClass] = useState(stored.interestedClassDocsId ?? '')
  const [source, setSource] = useState(stored.source ?? '')
  const [sourceDetails, setSourceDetails] = useState(stored.sourceDetails ?? '')
  const [notes, setNotes] = useState(stored.notes ?? '')
  const [guardiansMode, setGuardiansMode] = useState('')
  const [guardians, setGuardians] = useState(
    (stored.guardians ?? []).map((one) => ({ ...one })))
  const [version, setVersion] = useState(String(stored.version ?? ''))
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  const differs = (now, was) => now !== (was ?? '')

  const body = {
    ...(differs(prospectiveStudentName, stored.prospectiveStudentName)
      ? { prospectiveStudentName } : {}),
    ...(differs(academicYear, stored.academicYear) ? { academicYear } : {}),
    ...(differs(dateOfBirth, stored.dateOfBirth) ? { dateOfBirth } : {}),
    ...(differs(gender, stored.gender) ? { gender } : {}),
    ...(differs(interestedClassDocsId, stored.interestedClassDocsId)
      ? { interestedClassDocsId } : {}),
    ...(differs(source, stored.source) ? { source } : {}),
    ...(differs(sourceDetails, stored.sourceDetails) ? { sourceDetails } : {}),
    ...(differs(notes, stored.notes) ? { notes } : {}),
    ...(guardiansMode === 'replace' ? { guardians } : {}),
    ...(guardiansMode === 'clear' ? { guardians: [] } : {}),
    ...(version === '' ? {} : { version: Number(version) }),
  }

  //! WHAT THE CALL WILL DO, worked out here so the warning below can say it before it happens.
  const yearMoved = differs(academicYear, stored.academicYear)
  const keepsOldClass = yearMoved && stored.interestedClassDocsId
    && !differs(interestedClassDocsId, stored.interestedClassDocsId)

  const setGuardian = (index, field, value) => setGuardians((old) =>
    old.map((row, n) => (n === index ? { ...row, [field]: value } : row)))

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('update-inquiry', {
      label: `Correct ${stored.inquiryNo}`,
      pathParams: { inquiryId: stored.inquiryId },
      body,
    })
    setSaving(false)
    if (result.ok) { onCorrected(); onClose() } else { setRefused(result.bodyJson ?? {}) }
  }

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={`Correct ${stored.inquiryNo}`}
      description="Only what differs from what was read is sent. Emptying a box sends an empty string, which is how #9 is told to take an optional field off."
      endpoint={<EndpointTag id="update-inquiry" name="Correct" look="primary"
        pathParams={{ inquiryId: stored.inquiryId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Correct it</Button>
        </>
      }
    >
      <div className="stack">
        {refused ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">{refused.code ?? 'refused'}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        <p className="muted">
          <Info size={12} /> <b>There is no status gate, and this lead is{' '}
          <span className="mono">{stored.status}</span>.</b> #18 refuses anything but a{' '}
          <span className="mono">DRAFT</span> application, because #19 freezes what the family
          declared. <b>Nobody declares a lead</b> — somebody took a phone call and wrote down what
          they heard — so every status stays correctable, including{' '}
          <span className="mono">LOST</span>.
        </p>

        {keepsOldClass ? (
          <p className="muted">
            <Info size={12} /> <b>You are moving the year and leaving the class behind.</b> That
            will answer <span className="mono">409 CLASS_NOT_IN_CYCLE_YEAR</span> about a class
            this body never mentions — a lead&rsquo;s class must be a class of its year, and a year
            that moved and left an unrelated one behind would break that silently. Empty the class
            box, or put in one of the new year. <b>Send it anyway to read the refusal.</b>
          </p>
        ) : null}

        <Field label="Child's name" hint="Emptying this is 400 BLANK_STUDENT_NAME — a lead has to be about somebody. Leaving it as it is means the field is not sent at all, and the box knows the difference.">
          <Input value={prospectiveStudentName} onChange={(e) => setName(e.target.value)} />
        </Field>

        <Field label="Academic year" hint="A label on a phone call, and the first thing anybody mishears — which is why this is editable and #18's cycle is not. Emptying it is 400 BLANK_ACADEMIC_YEAR.">
          <Input value={academicYear} onChange={(e) => setYear(e.target.value)} />
        </Field>

        <div className="field-grid">
          <Field label="Date of birth" hint="CANNOT BE CLEARED — it is not a string, so it has no blank to send. It can be corrected to the right one. A date in the future is 400 VALIDATION_FAILED.">
            <Input type="date" value={dateOfBirth} onChange={(e) => setDob(e.target.value)} />
          </Field>
          <Field label="Gender" hint="Cannot be cleared either, and for the same reason.">
            <Select
              value={gender}
              options={GENDERS.map((one) => ({
                value: one, label: one === '' ? 'leave it alone' : one,
              }))}
              label="Gender"
              onChange={setGender}
            />
          </Field>
        </div>

        <Field label="Interested class id" hint="EMPTY IT TO CLEAR IT — that is the family no longer having a class in mind. It has to be a class of the year above, so moving the year re-checks this even when you do not touch it.">
          <Input value={interestedClassDocsId} onChange={(e) => setClass(e.target.value)} />
        </Field>

        <div className="field-grid">
          <Field label="Source" hint="Empty it to clear it.">
            <Input value={source} onChange={(e) => setSource(e.target.value)} />
          </Field>
          <Field label="Source details" hint="Empty it to clear it.">
            <Input value={sourceDetails} onChange={(e) => setSourceDetails(e.target.value)} />
          </Field>
        </div>

        <Field label="Notes" hint="Empty it to clear it. Without that, a note typed by mistake would be permanent.">
          <Input value={notes} onChange={(e) => setNotes(e.target.value)} />
        </Field>

        <Field
          label="Guardians"
          hint="BEHIND A SWITCH, because sending them at all REPLACES the lot — there is no id on a guardian to merge by. A screen that sent them on every correction would rewrite the family every time somebody fixed a spelling."
        >
          <Select
            value={guardiansMode}
            options={[
              { value: '', label: 'leave them alone — the field is not sent' },
              { value: 'replace', label: 'replace them with the rows below' },
              { value: 'clear', label: 'clear them — send an empty list' },
            ]}
            label="Guardians"
            onChange={setGuardiansMode}
          />
        </Field>

        {guardiansMode === 'clear' ? (
          <p className="muted">
            <Info size={12} /> <b>An empty list is allowed here and refused by #18.</b> An
            application with no guardian is not one a school can act on; a <i>lead</i> with none is
            the walk-in who gave a child&rsquo;s name and left, which #8 is built to accept.
          </p>
        ) : null}

        {guardiansMode === 'replace' ? (
          <div className="stack">
            {guardians.map((one, index) => (
              <div key={index} className="stack">
                <div className="field-grid">
                  <Field label={`Guardian ${index + 1}`} hint="Optional, as it is on #8 — the desk often has a first name and nothing more.">
                    <Input value={one.fullName ?? ''}
                      onChange={(e) => setGuardian(index, 'fullName', e.target.value)} />
                  </Field>
                  <Field label="Relation" hint="Optional too.">
                    <Select
                      value={one.relation ?? ''}
                      options={RELATIONS.map((r) => ({
                        value: r, label: r === '' ? 'not said' : r,
                      }))}
                      label="Relation"
                      onChange={(v) => setGuardian(index, 'relation', v)}
                    />
                  </Field>
                </div>
                <div className="field-grid">
                  <Field label="Phone" hint="What the worklist shows, and what #15 will search on.">
                    <Input value={one.phoneNumber ?? ''}
                      onChange={(e) => setGuardian(index, 'phoneNumber', e.target.value)} />
                  </Field>
                  <Field label="Email" hint="The other thing #15 searches on.">
                    <Input value={one.emailAddress ?? ''}
                      onChange={(e) => setGuardian(index, 'emailAddress', e.target.value)} />
                  </Field>
                </div>
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
              <Badge tone={guardians.length > 10 ? 'bad' : undefined}>
                {guardians.length} of 10
              </Badge>
            </div>
          </div>
        ) : null}

        <Field
          label="Version"
          hint="Filled in from what this page last read. Leave it and a correction somebody else made since is 409 CONCURRENT_MODIFICATION; clear it and the check is skipped entirely."
        >
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>

        <p className="muted">
          <Info size={12} /> <b>What another endpoint owns is not on this form.</b>{' '}
          <span className="mono">status</span> and <span className="mono">lostReason</span> are
          #12&rsquo;s, the counsellor is #11&rsquo;s, and the chase date is #10&rsquo;s — none of
          them built. Send them by hand and they are <b>ignored, not refused</b>: this module gives
          events verbs and field edits a PATCH, and an edit that could set a status would be a way
          round the transition table.
        </p>
      </div>
    </Modal>
  )
}
