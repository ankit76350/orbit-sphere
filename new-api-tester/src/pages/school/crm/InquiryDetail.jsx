import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from './admissionDates.js'
import { screenPath } from '../../../paths.js'

/**
 * One lead: /school-crm/inquiries/{id}
 *
 * ONE ENDPOINT — #14. The page exists because three things are on it that a worklist row cannot
 * carry: the notes, where the lead came from, and the timeline itself rather than a count of it.
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

export default function InquiryDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [lead, setLead] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

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
              family a call back, so hiding the lead would lose them exactly when it matters.
            </p>
          </Card>
        </>
      ) : null}
    </div>
  )
}
