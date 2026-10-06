import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, FileInput, Info, Pencil, PhoneCall, Plus, RefreshCw }
  from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from './admissionDates.js'
import { actionPath, detailPath, screenPath } from '../../../paths.js'

/**
 * One lead: /school-crm/inquiries/{id}
 *
 * FOUR ENDPOINTS — #14 reads one lead, #9 corrects it, #10 logs a call against it, which is also
 * how its status moves since #12 was removed on 2026-10-01, and #16 says what the lead became.
 * The page exists because three things are on it that a worklist row cannot carry: the notes,
 * where the lead came from, and the timeline itself rather than a count of it. Correcting belongs
 * here for the same reason it belongs on the application's page: what you are editing is what this
 * page shows.
 *
 * #16 IS A SECOND READ, AND IT IS ITS OWN CALL. The lead and its applications are two endpoints
 * and the page shows both answers separately, so a refusal from one is not mistaken for an empty
 * answer from the other — which is the exact mistake #16 reads the lead first to avoid.
 *
 * THE CORRECT BUTTON IS NEVER SWITCHED OFF BY STATUS, and that is not this screen being lax — #9
 * has no status gate at all. A lead is the school's own notes about a phone call, not a
 * declaration the family signed, so a LOST one can still have a misspelt name put right. The
 * difference from #18 is worth reading on the page rather than discovering by pressing.
 *
 * THE TIMELINE IS THE REASON THIS PAGE EXISTS, and today it is almost always empty — #10 logs a
 * follow-up and is not built, so every lead reads back with none. The page says that rather than
 * showing a bare empty state, because "no calls yet" and "no endpoint to Add a FollowUp" look
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
//! EVERY STATUS IS OFFERED, INCLUDING THE FOUR THAT WILL BE REFUSED. LOST, APPLICATION_STARTED
//! and APPLICATION_SUBMITTED each answer a different refusal worth reading, and NEW is a move
//! backwards from everywhere. The page says which will answer what and then sends them.
const STATUSES = ['', 'NEW', 'CONTACTED', 'COUNSELLING', 'VISIT_SCHEDULED', 'VISITED',
  'APPLICATION_STARTED', 'APPLICATION_SUBMITTED', 'LOST', 'CLOSED']
/** The two a lead ends on. Mirrors FINISHED in InquiryService, which is where it decides things. */
const FINISHED = ['LOST', 'CLOSED']

/**
 * Does this row describe a move out of where the lead is now.
 *
 * TWO KINDS OF `from` COLUMN. Most rows name statuses, separated by ` · ` when a move has several
 * starting points. One names none of them — `any non-terminal`, which is the LOST edge — and it
 * applies to everything except the two a lead ends on. Matching that row by string would have
 * missed it on every lead, which is how the summary above the table came to count three moves out
 * of NEW when there are four.
 */
const BEFORE_A_FORM = ['NEW', 'CONTACTED', 'COUNSELLING', 'VISIT_SCHEDULED', 'VISITED']

function movesFrom(from, status) {
  if (!status) return false
  if (from === 'any non-terminal') return !FINISHED.includes(status)
  if (from === 'any status before a form exists') return BEFORE_A_FORM.includes(status)
  return from.split(' · ').includes(status)
}
const RELATIONS = ['', 'FATHER', 'MOTHER', 'GRANDFATHER', 'GRANDMOTHER', 'UNCLE', 'AUNT',
  'LEGAL_GUARDIAN', 'SIBLING', 'OTHER']
const BLANK_GUARDIAN = { fullName: '', relation: '', phoneNumber: '', emailAddress: '' }

/**
 * The lead's status graph — the same one `controllers/crm/README.md` specifies, drawn DOWN the
 * page instead of across it.
 *
 * TURNED VERTICAL SO EVERY STATUS OWNS A LINE, for the reason the application's version records:
 * the marker is appended to the END of a line, and on a horizontal drawing half the statuses
 * share line one — so a NEW lead, which is most of them, would get marked after CLOSED.
 *
 * THE LOST EDGE IS A NOTE RATHER THAN NINE ARROWS. Every non-terminal status reaches LOST, and
 * drawing that is eight lines crossing the picture to say one sentence.
 *
 * SO ARE THE TWO SKIP-FORWARD EDGES, for the same reason and after trying it the other way. The
 * early half can jump straight to VISIT_SCHEDULED or VISITED, which is seven arrows between five
 * nodes; drawn on the chain they crossed each other and needed duplicate labels to land, and the
 * picture said less than the two lines underneath it do.
 *
 * KEPT AS ONE STRING rather than generated from the MOVES table below: the box-drawing alignment
 * is the value. The smoke test asserts every status in the enum appears here.
 */
const STATUS_GRAPH = `NEW
  │    #10  the first call gets through
  v
CONTACTED
  │    #10  a counsellor starts talking them through it
  v
COUNSELLING
  │    #10  a visit is booked
  v
VISIT_SCHEDULED
  │    #10  they came
  v
VISITED

              APPLICATION_STARTED
                        │    #19  the form is submitted
                        v
              APPLICATION_SUBMITTED
                        │    #10  the school closes the file
                        v
                     CLOSED

the early half may skip forward, because a walk-in did not book anything:
  NEW · CONTACTED · COUNSELLING                    ──> VISIT_SCHEDULED    #10
  NEW · CONTACTED · COUNSELLING · VISIT_SCHEDULED  ──> VISITED            #10

any status before a form exists ──> APPLICATION_STARTED   #17 only
any non-terminal                ──> LOST                  #10, and the note says why`

/**
 * Where this lead is, marked on the picture without disturbing it.
 *
 * APPENDED AT THE END OF A LINE, never inserted into one: every other way of highlighting a node
 * shifts the characters after it and breaks the arrows.
 *
 * THE FIRST MATCH IS THE RIGHT ONE. `CONTACTED` is a substring of nothing else, but
 * `APPLICATION_STARTED` appears inside no other status either — the one to watch is `LOST`, which
 * only appears on the final note, and that is where a LOST lead should be marked.
 */
function markCurrent(status) {
  if (!status) return STATUS_GRAPH
  const lines = STATUS_GRAPH.split('\n')
  const at = lines.findIndex((line) => line.includes(status))
  if (at < 0) return STATUS_GRAPH
  lines[at] = `${lines[at]}   ◀── this lead`
  return lines.join('\n')
}

/**
 * Which endpoint owns each move, and whether it exists.
 *
 * THE POINT OF THE TABLE IS THE LAST TWO COLUMNS. The graph reads as a set of moves somebody
 * could try, and four of these are not moves this half can make: two belong to the APPLICATION
 * half and one needs an endpoint that is not built.
 *
 * THE "WHO MAY" COLUMN IS THE ONE THIS TABLE HAS AND THE APPLICATION'S DOES NOT, and it exists
 * because #10 refuses three destinations that are ON the table. APPLICATION_STARTED and
 * APPLICATION_SUBMITTED are legal moves owned by #17 and #19 — a counsellor typing either would
 * let a lead claim a form that does not exist. LOST is a legal move that needs a reason #10 has
 * nowhere to put. Saying "it cannot go there" would be a lie, so the endpoint refuses each with
 * its own code and this column is why.
 *
 * NOTHING HERE IS SET BY BEING TOLD TO, except through #10 — and that is not a "set the status"
 * call either: it logs a call that happened to move the lead. #12 was the one that moved a lead on
 * its own, and it was removed on 2026-10-01 as a second way to do one thing.
 */
const MOVES = [
  ['NEW', 'CONTACTED', '#10 — the first call gets through', '#10', true],
  ['CONTACTED', 'COUNSELLING', '#10 — a counsellor starts talking them through it', '#10', true],
  // THE EARLY HALF SKIPS FORWARD. A parent rings, asks to come and see the place, and a date is
  // agreed — there was no separate counselling step, and inventing one would put a fiction in
  // the timeline. So VISIT_SCHEDULED is reachable from all three of the statuses before it.
  ['NEW · CONTACTED · COUNSELLING', 'VISIT_SCHEDULED', '#10 — a visit is booked', '#10', true],
  // AND A WALK-IN BOOKED NOTHING AT ALL. They visited; the lead was NEW that morning. Forcing the
  // desk through CONTACTED and VISIT_SCHEDULED first would be three calls logged that never
  // happened.
  ['NEW · CONTACTED · COUNSELLING · VISIT_SCHEDULED', 'VISITED',
    '#10 — they came, booked or not', '#10', true],
  // THE TWO THE APPLICATION HALF OWNS. Both are on the transition table and both are refused by
  // #10 with INQUIRY_STATUS_NOT_BY_HAND — a lead's application state is a fact about the form.
  // FROM EVERY PRE-APPLICATION STATUS, and the row used to say COUNSELLING · VISITED. That was a
  // route #17 has never taken: it does not consult the table at all, and sets the status the
  // moment a form naming the lead is saved. A family can fill a form in on the first call.
  ['any status before a form exists', 'APPLICATION_STARTED',
    '#17 — a form is started naming this lead', '#17 only', true],
  ['APPLICATION_STARTED', 'APPLICATION_SUBMITTED',
    '#19 — that form is submitted', '#19 only', true],
  ['APPLICATION_SUBMITTED', 'CLOSED', '#10 — the school closes the file', '#10', true],
  // LOST IS #10's SINCE 2026-10-01. It was refused here while a loss needed a reason and a
  // follow-up had nowhere to put one; the note on the entry that loses the lead IS the reason,
  // and the separate lostReason field went with #12.
  ['any non-terminal', 'LOST', '#10 — the note on the call is the reason', '#10', true],
]

export default function InquiryDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [lead, setLead] = useState(null)
  //! #16's answer, kept SEPARATE from the lead rather than folded into it. A read that fails is a
  //! read that failed: showing "no applications" because the second call 404'd would be the one
  //! lie this endpoint was built to stop telling.
  const [became, setBecame] = useState(null)
  const [becameProblem, setBecameProblem] = useState(null)
  const [becameLoading, setBecameLoading] = useState(false)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [correcting, setCorrecting] = useState(false)
  const [logging, setLogging] = useState(false)

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

  //! ITS OWN CALL, not part of the one above. #14 and #16 are two endpoints and the page shows
  //! both answers; a single loader would hide which of the two refused.
  const loadBecame = useCallback(async () => {
    if (!actingSubdomain) return
    setBecameLoading(true)
    const result = await call('list-inquiry-applications', {
      label: 'What the lead became',
      pathParams: { inquiryId: id ?? '' },
    })
    setBecameLoading(false)
    if (result.ok) { setBecame(result.bodyJson ?? []); setBecameProblem(null) }
    else { setBecame(null); setBecameProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])
  useEffect(() => { loadBecame() }, [loadBecame])

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
            /* #9 SITS ON THE CARD IT EDITS — moved off the page toolbar 2026-10-01, the same as
               #2 on a cycle and #18 on an application. Everything this button changes is shown
               below it; the toolbar keeps what belongs to no single card. */
            action={
              <>
                <Badge tone={TONE[lead.status]}>{lead.status}</Badge>
                {lead.overdue ? <> <Badge tone="bad">late</Badge></> : null}
                <EndpointTag id="update-inquiry" name="Correct" look="primary"
                  pathParams={{ inquiryId: id ?? '' }} />
                <Button icon={Pencil} onClick={() => setCorrecting(true)}>Correct it</Button>
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
            </div>

            <p className="muted">
              <Info size={12} /> <b>Overdue is two conditions, not one.</b> Past its chase date{' '}
              <i>and</i> not <span className="mono">LOST</span> or{' '}
              <span className="mono">CLOSED</span> — a lead somebody gave up on last month has a
              past date too, and nobody owes it a phone call. A lead with no date at all is never
              late: <span className="mono">$lt</span> does not match a field that is not there.
            </p>

              {/* THE GUARDIANS ARE PART OF THE LEAD, not a neighbour of it — merged 2026-10-01,
                  the same as an application's guardians. Who the school should ring is the point
                  of a lead, so reading it apart from the name and the chase date asked the page
                  to be held in two places at once. */}
              <div>
                <h3 className="card-title">Guardians — {guardians.length}</h3>
                <p className="muted">
                  Every field on a lead&rsquo;s guardian is optional, unlike an
                  application&rsquo;s. The front desk writes down a first name and a phone
                  number, and a record that refused that would refuse the call.
                </p>
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
              </div>
          </Card>

          <Card
            title="Where it can go from here"
            description="The graph in controllers/crm/README.md, which is the specification. It only goes forwards, and LOST and CLOSED are the ends of it."
          >
            <div className="stack">
              <pre className="resp-body">{markCurrent(lead.status)}</pre>

              <p className="muted">
                <Info size={12} /> <b>The chain down the left is #10&rsquo;s</b> — each step is a
                call that happened to move the lead, logged with the note that moved it.{' '}
                <b>The early half may skip forward</b>, which is what the two lines under the
                picture are for: a walk-in <i>visited</i> and nobody booked anything, and a parent
                who rings to arrange a visit never had a separate counselling step. Forcing either
                through the full chain would mean logging calls that never happened.{' '}
                <b>The two below it are not this half&rsquo;s to make</b>: a lead reaches{' '}
                <span className="mono">APPLICATION_STARTED</span> because #17 started a form
                naming it, and <span className="mono">APPLICATION_SUBMITTED</span> because #19 sent
                that form. #10 refuses both with{' '}
                <span className="mono">409 INQUIRY_STATUS_NOT_BY_HAND</span>, and they are still on
                the table because they are legal <i>moves</i> — just not ones a counsellor may
                type. <b>#17 does not consult this table at all</b>, which is why its row starts
                from <i>any</i> status before a form exists rather than from two of them: a family
                can fill a form in on the first call.
              </p>

              {/* WHERE IT IS NOW, SAID ABOVE THE TABLE and not only marked in it. A highlighted
                  row is easy to miss once the table scrolls, and on a finished lead there is no
                  highlighted row to find at all — a sentence always has something to say.

                  IT COUNTS ROWS AND SAYS NOTHING ELSE. The first version worked out how many
                  moves could be made "here", and got it wrong three ways: it missed the LOST row
                  because that one names no status, it said "can be made here" about an endpoint
                  that was not built, and it read "the one move out of it belong to". A summary
                  that has to be right about four things will be wrong about one. */}
              <div className="toolbar">
                <span className="muted">This lead is</span>
                <Badge tone={TONE[lead.status]}>{lead.status}</Badge>
                <span className="toolbar-spacer" />
                <span className="muted">
                  {(() => {
                    const out = MOVES.filter(([from]) => movesFrom(from, lead.status))
                    if (out.length === 0) {
                      return FINISHED.includes(lead.status)
                        ? 'nothing follows it — this is where a lead ends'
                        : 'no row below starts from it'
                    }
                    return out.length === 1
                      ? 'the one row below that starts from it is marked'
                      : `the ${out.length} rows below that start from it are marked`
                  })()}
                </span>
              </div>

              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>From</th>
                      <th>To</th>
                      <th>What does it</th>
                      <th>Who may</th>
                      <th>Built</th>
                    </tr>
                  </thead>
                  <tbody>
                    {MOVES.map(([from, to, owner, who, built]) => (
                      // The row for the move OUT of where this lead is now, highlighted — that is
                      // the one somebody reading this page actually wants.
                      <tr key={`${from}-${to}`}
                        data-now={movesFrom(from, lead.status) || undefined}>
                        <td className="mono">{from}</td>
                        <td className="mono">{to}</td>
                        <td>{owner}</td>
                        <td className="mono">{who}</td>
                        <td>
                          {built
                            ? <Badge tone="good">yes</Badge>
                            : <span className="muted">not yet</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <p className="muted">
                <Info size={12} /> <b>Every row on this table is reachable, and{' '}
                <span className="mono">#10</span> walks all but two of them.</b> Losing a lead
                needs a reason — <i>why</i> is the only question anybody asks of one six months
                later — and <b>the note on the call that loses it is that reason</b>. It was
                refused here until 2026-10-01, when the separate{' '}
                <span className="mono">lostReason</span> field went with #12 and the rule it
                existed for went with it. <b>Every row on this table is now
                reachable.</b>
              </p>

              <p className="muted">
                <Info size={12} /> <b>Sending the status it already has is a no-move, not a
                refusal.</b> A second call about a lead that is still{' '}
                <span className="mono">CONTACTED</span> should not have to leave the field out —
                and the timeline entry then records no move at all, which reads as &ldquo;left as
                it was&rdquo;. Anything else off this table is{' '}
                <span className="mono">409 INQUIRY_TRANSITION_NOT_ALLOWED</span>, and the refusal
                lists what it <i>can</i> go to, or <span className="mono">nothing</span> when it is
                finished. <b>What the table still refuses is going backwards</b> — a lead that has
                visited cannot return to <span className="mono">NEW</span>.
              </p>
            </div>
          </Card>

          <Card
            title={`The timeline — ${lead.followUpCount}`}
            description="Oldest first, because a conversation reads forwards. Sorted by the server rather than trusted: a $push is not a promise about order once anything else touches the array."
            action={
              <>
                <Badge>{lead.followUpCount}</Badge>{' '}
                <Button icon={PhoneCall} onClick={() => setLogging(true)}>Add a FollowUp</Button>
              </>
            }
          >
            {timeline.length === 0 ? (
              <Empty
                title="Nothing has been logged"
                description="Add a FollowUp and it lands here, oldest first — and the chase date it sets is what puts this lead on the worklist."
                action={
                  <Button look="primary" icon={PhoneCall} onClick={() => setLogging(true)}>
                    Add a FollowUp
                  </Button>
                }
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

          {/* WHAT CAME OF IT, read by #16 — the other end of inquiryDocsId. #17 has written that
              field since the application block was built and NOTHING READ IT BACK, so until now
              this page could say the family had enquired and not that they had applied.

              IT SITS ABOVE "Start an application" on purpose: what already happened, then the
              thing that makes it happen again. A lead with a row here and the button below it is
              the 409 APPLICATION_ALREADY_EXISTS story told in one screenful. */}
          <Card
            title="What the lead became"
            description="#16 — every application this lead turned into, newest first. One per admission round is all the database allows."
            action={
              <>
                <EndpointTag id="list-inquiry-applications" name="Read"
                  pathParams={{ inquiryId: id ?? '' }} />
                <Button icon={RefreshCw} onClick={loadBecame} busy={becameLoading}>
                  Refresh
                </Button>
              </>
            }
          >
            {becameProblem ? (
              <div className="resp">
                <div className="resp-head">
                  <span className="resp-status" data-ok="false">
                    {becameProblem.bodyJson?.code ?? becameProblem.status}
                  </span>
                </div>
                <pre className="resp-body">
                  {becameProblem.bodyJson?.message ?? becameProblem.bodyText}
                </pre>
              </div>
            ) : (became?.length ?? 0) === 0 ? (
              <p className="muted">
                <Info size={12} /> <b>An empty list, not a 404.</b> This family never applied —
                which is a different fact from the lead not existing, and the reason #16 reads the
                lead first. Start one below and it appears here.
              </p>
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Application no</th>
                      <th>Applicant on the form</th>
                      <th>Status</th>
                      <th>Round</th>
                      <th>Class id</th>
                      <th>Started</th>
                      <th>How it ended</th>
                    </tr>
                  </thead>
                  <tbody>
                    {became.map((one) => (
                      //! THE ROW OPENS THE APPLICATION, because #16 answers with ids and no names:
                      //! it reads ONE collection, and resolving a class or a cycle name would mean
                      //! two more queries to draw three rows. #25 is where the form is.
                      <tr
                        key={one.admissionApplicationId}
                        data-opens
                        onClick={() => navigate(detailPath('school', 'crm', 'applications',
                          one.admissionApplicationId))}
                      >
                        <td><span className="mono">{one.applicationNo}</span></td>
                        <td>
                          {one.applicantName}
                          {lead.prospectiveStudentName
                            && one.applicantName !== lead.prospectiveStudentName ? (
                              <>
                                {' '}
                                <Badge tone="warn">not the lead&rsquo;s name</Badge>
                              </>
                            ) : null}
                        </td>
                        <td><Badge>{one.status}</Badge></td>
                        <td><span className="mono muted">{one.admissionCycleDocsId}</span></td>
                        <td><span className="mono muted">{one.appliedClassDocsId}</span></td>
                        <td title={one.createdAt}>{readable(one.createdAt)}</td>
                        <td>
                          {one.withdrawnAt ? (
                            <span title={one.withdrawnAt}>
                              <b>withdrawn</b> — {one.withdrawalReason ?? 'no reason given'}
                            </span>
                          ) : one.decidedAt ? (
                            <span title={one.decidedAt}>
                              <b>decided</b> {readable(one.decidedAt)}
                              {one.decisionNote ? ` — ${one.decisionNote}` : ''}
                            </span>
                          ) : one.submittedAt ? (
                            <span className="muted" title={one.submittedAt}>
                              sent {readable(one.submittedAt)}, waiting on the school
                            </span>
                          ) : (
                            <span className="muted">still a draft</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <p className="muted">
              <Info size={12} /> <b>Four fields here are not on #24&rsquo;s worklist row</b> —{' '}
              <span className="mono">decidedAt</span>, <span className="mono">decisionNote</span>,{' '}
              <span className="mono">withdrawnAt</span> and{' '}
              <span className="mono">withdrawalReason</span> — because the question being asked of
              a lead is how it ended, not where it sits in a queue. The fifth,{' '}
              <span className="mono">resultingStudentDocsId</span>, is what would make the title
              literally true and is absent on every row today: <b>#33 writes it, and #33 is
              blocked.</b>
            </p>
          </Card>

          {/* LAST ON THE PAGE, because it is the last thing that happens to a lead — and the only
              action here that LEAVES it. Converting is a page rather than a modal: it asks for a
              round and then builds a whole form out of the answer, and a modal would lose both on
              a refresh. See StartApplicationFromLead.

              NOT GATED ON THE STATUS. A lead that has already become an application answers
              409 APPLICATION_ALREADY_EXISTS for the same round, and that is worth reading — so the
              button is offered whatever state the lead is in, and says what it will get. */}
          <Card
            title="Start an application"
            description="The lead becomes a formal application: its name, date of birth, gender and guardians are carried over, the class and the questions come from the round you pick, and the two are linked so this lead moves to APPLICATION_STARTED."
            action={
              <Button look="primary" icon={FileInput}
                onClick={() => navigate(
                  actionPath('school', 'crm', 'inquiries', id, 'start-application'))}>
                Start an application
              </Button>
            }
          >
            {lead.status === 'APPLICATION_STARTED' || lead.status === 'APPLICATION_SUBMITTED' ? (
              <p className="muted">
                <Info size={12} /> <b>This lead is already{' '}
                <span className="mono">{lead.status}</span>.</b> One inquiry gets one application
                per round, so starting another in the SAME cycle is{' '}
                <span className="mono">409 APPLICATION_ALREADY_EXISTS</span> — and the same lead in
                a DIFFERENT round is allowed. The button still goes, because which of the two you
                get is worth seeing.
              </p>
            ) : (
              <p className="muted">
                <Info size={12} /> <b>Nothing is copied twice.</b> The application carries{' '}
                <span className="mono">inquiryDocsId</span>, so this lead is linked rather than
                duplicated — and it moves to{' '}
                <span className="mono">APPLICATION_STARTED</span> as a side effect, which nothing
                asked it to do.
              </p>
            )}
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

      {logging && lead ? (
        <LogFollowUp
          lead={lead}
          onClose={() => setLogging(false)}
          onLogged={load}
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

        {/* WHAT THE LEAD IS NOW, including the parts this form cannot change. A correction is
            made against what is already there, so the whole of it is worth seeing without closing
            the modal to go and look. */}
        <div className="field-grid">
          <div>
            <p className="muted">Inquiry no</p>
            <p className="mono">{stored.inquiryNo}</p>
          </div>
          <div>
            <p className="muted">Status</p>
            <Badge tone={TONE[stored.status]}>{stored.status}</Badge>
          </div>
          <div>
            <p className="muted">Chase by</p>
            {stored.nextFollowUpAt
              ? <p>{readable(stored.nextFollowUpAt)}</p>
              : <p className="muted">nobody promised a date</p>}
          </div>
          <div>
            <p className="muted">Captured</p>
            <p>{readable(stored.createdAt)}</p>
          </div>
        </div>

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

        {/* THE ONES ON THE LEAD NOW, shown while the switch says "leave them alone". Without this
            the only way to see who is on a lead was to choose REPLACE, which is the one option
            that rewrites them — a reader had to arm the thing they were trying to avoid. */}
        {guardiansMode === '' ? (
          (stored.guardians ?? []).length === 0 ? (
            <p className="muted">
              <Info size={12} /> This lead has <b>no guardians</b> — the walk-in who gave a
              child&rsquo;s name and left. Nothing is sent while the switch is here.
            </p>
          ) : (
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Name</th><th>Relation</th><th>Phone</th><th>Email</th><th>Primary</th>
                  </tr>
                </thead>
                <tbody>
                  {(stored.guardians ?? []).map((one, at) => (
                    <tr key={at}>
                      <td>{one.fullName ?? <span className="muted">—</span>}</td>
                      <td>{one.relation ?? <span className="muted">—</span>}</td>
                      <td>{one.phoneNumber ?? <span className="muted">—</span>}</td>
                      <td>{one.emailAddress ?? <span className="muted">—</span>}</td>
                      <td>{one.primaryContact ? 'yes' : <span className="muted">—</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )
        ) : null}

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
          <Info size={12} /> <b>What another endpoint owns is not on this form.</b> The{' '}
          <span className="mono">status</span> and the chase date are <b>#10&rsquo;s</b> — a lead
          moves because somebody rang the family, and logging that call is what records it.{' '}
          <span className="mono">lostReason</span> is written by <b>nothing</b>: #12 owned it and
          was removed on 2026-10-01, so <span className="mono">LOST</span> is unreachable. #11, the
          counsellor assignment, was removed rather than built. Send any of them by hand and they
          are <b>ignored, not refused</b>: this module gives events verbs and field edits a PATCH,
          and an edit that could set a status would be a way round the transition table.
        </p>
      </div>
    </Modal>
  )
}

/**
 * #10's modal.
 *
 * IT STAYS OPEN AFTER A SUCCESS, like the lead capturer. A counsellor works a list — three calls
 * in a morning — and closing after each one would make the common case the slow one. The lead
 * behind it reloads, so the timeline grows while the modal is up.
 *
 * THE CHASE-DATE BOX IS THE ONE WORTH EXPLAINING. Leaving it empty does not mean "leave the date
 * alone", it means "there is no next call due" — and that CLEARS the date the lead had. It is the
 * one place #10 departs from "only what was sent", and it is the difference between a worklist
 * that is right and one that shows a family as overdue on the day somebody rang them. The box says
 * so, and says what the lead's date is now.
 *
 * THE STATUS SELECT OFFERS EVERY STATUS, INCLUDING THE FOUR THAT WILL BE REFUSED. That is the
 * never-disable rule doing its job: LOST, APPLICATION_STARTED and APPLICATION_SUBMITTED are all
 * things a caller will try, and each has a different refusal worth reading. The page says which
 * ones will answer what, and then sends them.
 */
function LogFollowUp({ lead, onClose, onLogged }) {
  const { call } = useApi()
  //! WHO THE TOP BAR IS ACTING AS. The nearest thing this project has to "who is asking" until
  //! real sessions arrive, and the same claim the idtoken cookie carries — so it is the right
  //! default for a field that exists to say who made the call.
  const { actingStaffDocsId } = useApiState()
  const stored = lead

  const [note, setNote] = useState('')
  const [communicationChannel, setChannel] = useState('')
  const [status, setStatus] = useState('')
  const [nextFollowUpAt, setNext] = useState('')
  //! DEFAULTED TO WHOEVER THE TOP BAR IS ACTING AS. The field is optional only because nothing
  //! here knows who is asking; the staff picker beside the school and the year IS that answer, so
  //! leaving it empty asked the person to retype what they had already chosen.
  //!
  //! STILL CLEARABLE AND STILL EDITABLE: an empty box logs the call against nobody, and another
  //! school's id pasted in is 404 STAFF_NOT_FOUND. Neither is gated.
  const [counselorDocsId, setCounselor] = useState(actingStaffDocsId ?? '')
  //! THIS SCHOOL'S STAFF, for the picker. Read when the modal opens rather than held by the page:
  //! the list is only wanted while somebody is logging a call, and a staff member added since the
  //! page loaded should be offered without a refresh.
  const [staff, setStaff] = useState([])
  //! SEEDED FROM WHAT THIS PAGE READ — version became REQUIRED on every write 2026-09-30, so a
  //! box that started empty would make each save 400 VALIDATION_FAILED. Still typed and still
  //! clearable: an older number is how 409 CONCURRENT_MODIFICATION is reached on purpose, and
  //! an empty one is how the new refusal is.
  const [version, setVersion] = useState(String(stored.version ?? ''))
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)
  const [done, setDone] = useState(null)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      const people = await call('list-staff', {
        label: 'Who could have logged it',
        query: { size: '100', sort: 'fullName' },
      })
      if (!cancelled) setStaff(people.ok ? (people.bodyJson?.content ?? []) : [])
    }
    load()
    return () => { cancelled = true }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const body = {
    ...(note ? { note } : {}),
    ...(communicationChannel ? { communicationChannel } : {}),
    ...(status ? { status } : {}),
    ...(nextFollowUpAt ? { nextFollowUpAt: new Date(nextFollowUpAt).toISOString() } : {}),
    ...(counselorDocsId ? { counselorDocsId } : {}),
    ...(version === '' ? {} : { version: Number(version) }),
  }

  //! WHAT THE CALL WILL DO, so the warnings can say it before it happens rather than after.
  const clearsTheDate = !nextFollowUpAt && stored.nextFollowUpAt
  //! LOST CAME OFF THIS LIST ON 2026-10-01. It was refused because a loss needs a reason and a
  //! follow-up had nowhere to put one; a follow-up is nothing BUT somewhere to write what
  //! happened, so the note on the entry that loses the lead is the reason.
  const ownedElsewhere = (status === 'APPLICATION_STARTED' || status === 'APPLICATION_SUBMITTED')
    ? '409 INQUIRY_STATUS_NOT_BY_HAND' : null

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('log-inquiry-follow-up', {
      label: `Add a FollowUp on ${stored.inquiryNo}`,
      pathParams: { inquiryId: stored.inquiryId },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setDone(result.bodyJson)
      onLogged()
      //! ONLY THE NOTE IS CLEARED. The channel, the counsellor and the date are the same across a
      //! sitting, and re-typing them is the friction that stops a worklist being worked.
      setNote('')
    } else {
      setRefused(result.bodyJson ?? {})
    }
  }

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={`Add a FollowUp on ${stored.inquiryNo}`}
      description="One interaction, pushed onto the timeline. It sets the lead's chase date too — which is the field the worklist sorts on."
      endpoint={<EndpointTag id="log-inquiry-follow-up" name="Log" look="primary"
        pathParams={{ inquiryId: stored.inquiryId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Log it</Button>
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

        {done ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">
                {done.followUpCount} on the timeline
              </span>
            </div>
            <pre className="resp-body">{done.nextStep}</pre>
          </div>
        ) : null}

        <Field label="What happened" required hint="THE ONLY REQUIRED FIELD. An entry that says nothing is a row that makes a lead look worked when nobody did anything — everything else about a call can be missing and the entry is still worth having.">
          <Input value={note} onChange={(e) => setNote(e.target.value)}
            placeholder="Rang the mother, she asked for the fee structure." />
        </Field>

        <div className="field-grid">
          <Field label="How" hint="Free text — PHONE, WHATSAPP, VISIT. Nothing validates it, because a school names its own channels. 'Caught the father in the corridor' is a real interaction with no channel worth naming.">
            <Input value={communicationChannel} onChange={(e) => setChannel(e.target.value)}
              placeholder="PHONE" />
          </Field>
          {/* THE READ THAT FILLS THE PICKER BELOW. */}
          <p className="muted">
            <EndpointTag id="list-staff" name="Who could have logged it"
              query={{ size: '100', sort: 'fullName' }} />
          </p>

          <Field
            label="Who logged it"
            hint="FILLED IN FROM THE STAFF PICKER IN THE TOP BAR — whoever this browser is acting as. The field is OPTIONAL and should not be: the point of a timeline is who said what, and it is optional only because nothing here knows who is asking yet. Pick somebody else, or clear it to log the call against nobody. The box below is what gets sent."
          >
            <Select
              value={counselorDocsId}
              options={[
                { value: '', label: staff.length
                  ? `nobody named — ${staff.length} to choose from`
                  : 'nobody named' },
                //! AN ACTING STAFF WHO IS NOT ON THE LIST still has to show as chosen — a staff
                //! member on page 2 of a hundred, or one who has since left. Without this the
                //! Select would fall back to "nobody named" while the box below said otherwise.
                ...(actingStaffDocsId
                  && !staff.some((one) => one.staffDocsId === actingStaffDocsId)
                  ? [{ value: actingStaffDocsId, label: `${actingStaffDocsId} — acting staff` }]
                  : []),
                ...staff.map((one) => ({
                  value: one.staffDocsId,
                  label: `${one.fullName}${one.employeeNo ? ` · ${one.employeeNo}` : ''}`,
                })),
              ]}
              label="Who logged it"
              onChange={setCounselor}
            />
          </Field>
          {/* THE BOX IS WHAT GETS SENT, and it is what makes 404 STAFF_NOT_FOUND reachable: the
              picker only ever offers THIS school's staff, so another school's id can be pasted
              but never selected. Clearing it logs the call against nobody, which is the ordinary
              case while nothing knows who is asking. */}
          <Field
            label="Counsellor id"
            hint="What is actually sent, and the picker fills it. A staff id of ANOTHER school is 404 STAFF_NOT_FOUND — paste one here to see it, because the picker cannot offer one. Clear it and the call is logged against nobody."
          >
            <Input value={counselorDocsId} onChange={(e) => setCounselor(e.target.value)}
              placeholder="67aa15d9dc3f7d0011111111" />
          </Field>
        </div>

        <Field
          label="Next call due"
          hint="LEAVING THIS EMPTY DOES NOT MEAN 'LEAVE IT ALONE'. It means there is no next call due, and it CLEARS the date the lead had — which is the difference between a worklist that is right and one that shows a family as overdue on the day somebody rang them."
        >
          <Input type="datetime-local" value={nextFollowUpAt}
            onChange={(e) => setNext(e.target.value)} />
        </Field>

        {clearsTheDate ? (
          <p className="muted">
            <Info size={12} /> <b>This will clear the chase date.</b> The lead is currently due{' '}
            <span className="mono">{readable(stored.nextFollowUpAt)}</span>, and sending no date
            means there is no next call due. <b>The entry you are writing keeps whatever it
            promises</b>, so the history is not lost — it is the <i>lead&rsquo;s</i> field that is
            rewritten, every time, because that is the one the worklist sorts on.
          </p>
        ) : null}

        <Field
          label="Did it move the lead"
          hint="Most calls move nothing — a counsellor rings, nobody answers. Leave this alone and the entry records no move, which reads as 'left as it was' on the timeline. Sending the status it already has is a no-move, not a refusal."
        >
          <Select
            value={status}
            options={[
              { value: '', label: 'no — it is still ' + stored.status },
              ...STATUSES.filter((one) => one !== '').map((one) => ({
                value: one, label: one,
              })),
            ]}
            label="Moved to"
            onChange={setStatus}
          />
        </Field>

        {ownedElsewhere ? (
          <p className="muted">
            {/* ONE BRANCH SINCE 2026-10-01. This read `status === 'LOST' ? … : …` while LOST was
                refused here too; LOST is now #10's, so the two application statuses are the only
                ones left that answer anything. */}
            <Info size={12} /> <b>That one answers <span className="mono">{ownedElsewhere}</span>.</b>{' '}
            <span className="mono">{status}</span> is a fact about an <i>application</i>. #17 sets
            it when a form is started and #19 when it is submitted — otherwise a lead could claim a
            form that does not exist.{' '}
            <b>Send it anyway to read the refusal.</b>
          </p>
        ) : null}

        <Field
          label="Version"
          hint="Empty by default, because a counsellor logging a call is not editing a field anybody else is holding. Fill it in and a call somebody else logged since is 409 CONCURRENT_MODIFICATION — it is guarded in the query as well as checked, so two people logging in the same instant cannot both win."
        >
          <Input value={version} onChange={(e) => setVersion(e.target.value)}
            placeholder={String(stored.version ?? '')} />
        </Field>

        <p className="muted">
          <Info size={12} /> <b>It is a <span className="mono">$push</span>, not a save.</b>{' '}
          Reading the lead, adding to its list and saving the whole document back would overwrite
          every entry anybody else logged in between — and a timeline is exactly the kind of list
          two counsellors write to at once.
        </p>
        <p className="muted">
          <Info size={12} /> <b>A finished lead can be logged against but not moved.</b> Somebody
          ringing back a family that gave up is exactly the call worth recording;{' '}
          <span className="mono">LOST</span> and <span className="mono">CLOSED</span> simply lead
          nowhere on the table, and the refusal says <span className="mono">nothing</span> rather
          than trailing off.
        </p>
      </div>
    </Modal>
  )
}
