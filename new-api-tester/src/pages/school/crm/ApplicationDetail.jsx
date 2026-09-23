import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Gavel, Info, RefreshCw, Send, UserPlus } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { compact, readable } from './admissionDates.js'
import { screenPath } from '../../../paths.js'

/**
 * One admission application: /school-crm/applications/{id}
 *
 * FOUR ENDPOINTS — #25 reads the form, #19 submits it, #26 puts it on somebody's desk and
 * #20 records what the school decided. Submitting belongs here because what it
 * freezes is exactly what this page shows: press the button and the guardians, the answers and the
 * applicant's details stop being editable.
 *
 * THE SUBMIT BUTTON IS ALWAYS ENABLED, including on a form that is already SUBMITTED and on one
 * whose round has closed. Both are documented refusals — INVALID_APPLICATION_TRANSITION and
 * CYCLE_NOT_OPEN — and a greyed-out button would make the two most interesting answers this
 * endpoint gives unreachable. The page says what will happen instead of preventing it.
 *
 * #25 FILLS THE REST OF THE PAGE, and it exists because six things are on it that a #24 row cannot
 * carry:
 * the guardians, the form answers, the evidence ids, the resolved names, everything about
 * withdrawal and enrollment, and the reviews and offers — which are not fields on the application
 * at all but rows in two other collections.
 *
 * THE REVIEWS AND OFFERS ARE EMPTY, and the page says WHY rather than showing a bare empty state.
 * #26 and #27 create a review, #29 to #31 create an offer, and none are built. "No reviews yet"
 * and "no endpoint that could make a review" look identical on screen and only one of them is
 * something the person can act on — the same call AdmissionCycleDetail makes about seats.
 *
 * A FORM WHOSE ROUND IS GONE IS SHOWN LOUDLY. #25 leaves the cycle name off when the cycle is
 * missing, and leaves the class name off with it, because classes are stored per year and the
 * year comes from the cycle. The page marks that rather than rendering three quiet blanks: an
 * application pointing at a round the school no longer has is a real problem.
 *
 * THE PEOPLE ARE IDS, ON PURPOSE. A reviewer and an assigned officer come back as document ids
 * because #22 and #26 are what fill those fields and neither is built — so there is no name to
 * resolve yet. The page shows the id rather than pretending the field is empty.
 *
 * NOTHING IS DISABLED. Refresh always sends, and an id that is not this school's is a documented
 * 404 worth being able to reach by editing the address bar.
 */

/**
 * The status graph — the same one `controllers/crm/README.md` specifies, drawn DOWN the page
 * instead of across it.
 *
 * TURNED VERTICAL SO EVERY STATUS OWNS A LINE, and that is the whole reason it differs from the
 * README's version. The marker below is appended to the END of a line, and on the horizontal
 * drawing the first six statuses all share line one — so a DRAFT form, which is most of them, got
 * marked after ENROLLED. A picture that says the wrong thing about the commonest case is worse
 * than no picture.
 *
 * KEPT AS ONE STRING rather than drawn from the MOVES table below: the box-drawing alignment is
 * the value, and generating it would mean maintaining a layout engine to reproduce something
 * somebody can read in the plan. The smoke test asserts every status in the enum appears here.
 */
const STATUS_GRAPH = `DRAFT
  │    #19  the family sends it
  v
SUBMITTED
  │    #26  a reviewer is assigned...
  │    ...or #20 decides it outright, with no review at all
  v
UNDER_REVIEW <────────────────────────┐
  │                                   │   #20  ask for UNDER_REVIEW when
  ├──> ADDITIONAL_INFORMATION_REQUIRED┘        what was asked for arrives
  │            #20
  ├──> REJECTED                             #20  (needs a reason)
  │
  ├──> WAITLISTED ──┐                   #20
  │                 │
  └──> APPROVED <───┘                   #20
         │    #29  an offer is issued
         v
      OFFERED
         │    #30  the family accepts
         v
   OFFER_ACCEPTED
         │    #33  they become a student
         v
      ENROLLED

anything before ENROLLED ──> WITHDRAWN      #21, and it needs a reason`

/**
 * Where this form is, marked on the picture without disturbing it.
 *
 * APPENDED AT THE END OF A LINE, never inserted into one: every other way of highlighting a node
 * — brackets, a caret line, colour spans — either shifts the characters after it and breaks the
 * arrows, or needs the diagram to stop being a single string.
 *
 * EVERY STATUS IS ON ITS OWN LINE in the drawing above, which is what makes "the end of the line"
 * an unambiguous place to put this. `ENROLLED` is the only one that appears twice — on its own
 * line and in the withdrawal note — and the first match is the right one.
 */
function markCurrent(status) {
  if (!status) return STATUS_GRAPH
  const lines = STATUS_GRAPH.split('\n')
  const at = lines.findIndex((line) => line.includes(status))
  if (at < 0) return STATUS_GRAPH
  lines[at] = `${lines[at]}   ◀── this form`
  return lines.join('\n')
}

/**
 * Which endpoint owns each move, and whether it exists.
 *
 * THE POINT OF THE TABLE is the second column. Every arrow above except the first is something
 * nothing can currently do, and a graph with no note saying so reads as a set of moves somebody
 * could try — which is how an afternoon gets spent looking for a route that 404s.
 *
 * NONE OF THESE ARE SET BY BEING TOLD TO. There is no "set the status" endpoint and there will
 * not be one: OFFERED is #29's consequence, OFFER_ACCEPTED is #30's, ENROLLED is #33's. That is
 * why the column names an action rather than a status.
 */
const MOVES = [
  ['DRAFT', 'SUBMITTED', '#19 — the family sends it', true],
  ['SUBMITTED', 'UNDER_REVIEW', '#26 — a reviewer is assigned', true],
  // #20 DOES NOT NEED A REVIEW TO EXIST. Small schools decide in a conversation, and insisting
  // on UNDER_REVIEW would mean assigning a reviewer first — which IS inventing a review row.
  ['SUBMITTED', 'APPROVED · REJECTED · WAITLISTED · ADDITIONAL_INFO…',
    '#20 — decided with no review at all', true],
  ['UNDER_REVIEW', 'APPROVED · REJECTED · WAITLISTED', '#20 — the decision', true],
  ['UNDER_REVIEW', 'ADDITIONAL_INFORMATION_REQUIRED',
    '#20 — asking for more, and it needs a reason', true],
  ['ADDITIONAL_INFORMATION_REQUIRED', 'UNDER_REVIEW',
    '#20 — what was asked for arrived', true],
  ['WAITLISTED', 'APPROVED', '#20 — a seat came free', true],
  ['APPROVED', 'OFFERED', "#29 — issuing an offer, as a side effect", false],
  ['OFFERED', 'OFFER_ACCEPTED', "#30 — the family's answer", false],
  ['OFFER_ACCEPTED', 'ENROLLED', '#33 — the applicant becomes a student', false],
  ['anything before ENROLLED', 'WITHDRAWN', '#21 — needs a reason', false],
]

const STATUS_TONE = {
  APPROVED: 'good',
  OFFER_ACCEPTED: 'good',
  ENROLLED: 'good',
  WAITLISTED: 'warn',
  ADDITIONAL_INFORMATION_REQUIRED: 'warn',
  REJECTED: 'bad',
  WITHDRAWN: 'bad',
}

const OFFER_TONE = { ISSUED: 'good', ACCEPTED: 'good', SUPERSEDED: 'warn', WITHDRAWN: 'bad' }
const REVIEW_TONE = { COMPLETED: 'good', PENDING: 'warn' }

export default function ApplicationDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [application, setApplication] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [sent, setSent] = useState(null)
  const [assigning, setAssigning] = useState(false)
  const [deciding, setDeciding] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-admission-application', {
      label: 'One application in full',
      pathParams: { admissionApplicationId: id ?? '' },
    })
    setLoading(false)
    if (result.ok) { setApplication(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  // #19. Always sends, whatever the form's status — see the note at the top of this file.
  const submit = async () => {
    setSubmitting(true)
    const result = await call('submit-admission-application', {
      label: 'Submit the form',
      pathParams: { admissionApplicationId: id ?? '' },
    })
    setSubmitting(false)
    setSent(result)
    // Reload either way. A refusal changes nothing on the server, and re-reading proves it.
    load()
  }

  const back = () => navigate(screenPath('school', 'crm', 'applications'))

  if (!actingSubdomain) return <NoSchoolChosen what="An admission application" />

  const guardians = application?.guardians ?? []
  const reviews = application?.reviews ?? []
  const offers = application?.offers ?? []
  const answers = Object.entries(application?.formAnswers ?? {})
  const evidence = application?.evidenceDocumentDocsIds ?? []
  // The cycle is gone when its name came back absent. #25 leaves the name off rather than
  // refusing, so this is the only signal — and it is worth saying out loud.
  const orphaned = Boolean(application) && !application.admissionCycleName

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{application?.applicantName ?? 'Application'}</h1>
          <p className="muted">
            <span className="mono">{application?.applicationNo ?? id}</span>
            {application ? ` · ${application.status}` : ' · reading the form…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={back}>All applications</Button>
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button icon={Send} onClick={submit} busy={submitting}>Submit it</Button>
        <Button look="primary" icon={Gavel} onClick={() => setDeciding(true)}>Decide it</Button>
      </div>

      {sent ? (
        <Card
          title={sent.ok ? 'Submitted' : 'Not submitted'}
          action={<EndpointTag id="submit-admission-application" name="Submit" />}
        >
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok={String(Boolean(sent.ok))}>
                {sent.bodyJson?.code ?? sent.status}
              </span>
            </div>
            <pre className="resp-body">
              {sent.ok
                ? (sent.bodyJson?.nextStep ?? 'The form is in.')
                : (sent.bodyJson?.message ?? sent.bodyText)}
            </pre>
          </div>
          {!sent.ok ? (
            <p className="muted">
              <Info size={12} /> A refusal changes nothing — the form is still exactly as it was,
              and the page below is a fresh read proving it. <b>INVALID_APPLICATION_TRANSITION</b>
              {' '}means the form is not a DRAFT; <b>CYCLE_NOT_OPEN</b> and{' '}
              <b>APPLICATIONS_CLOSED</b> mean the round stopped taking forms — the first because
              somebody closed it, the second because nobody did and the published date passed.
            </p>
          ) : null}
        </Card>
      ) : null}

      {problem ? (
        <Card
          title="Could not read it"
          action={<EndpointTag id="get-admission-application" name="Get" />}
        >
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
          <p className="muted">
            <Info size={12} /> An application id belongs to one school. <b>Another school&rsquo;s
            real id answers 404 here</b>, and the body carries no name, no date of birth and no
            phone number — the school is part of the lookup rather than something checked after it.
          </p>
        </Card>
      ) : null}

      {application ? (
        <>
          <Card
            title="The form"
            description="What the family declared. Everything below the first row is left off a #24 list row."
            action={<EndpointTag id="get-admission-application" name="Get" />}
          >
            <div className="stack">
              <div className="field-grid">
                <div>
                  <p className="muted">Status</p>
                  <Badge tone={STATUS_TONE[application.status]}>{application.status}</Badge>
                </div>
                <div>
                  <p className="muted">Date of birth</p>
                  <p className="mono">{application.dateOfBirth}</p>
                </div>
                <div>
                  <p className="muted">Gender</p>
                  <p>{application.gender}</p>
                </div>
                <div>
                  <p className="muted">From a lead</p>
                  {application.inquiryDocsId
                    ? <p className="mono">{application.inquiryDocsId}</p>
                    : <p className="muted">Walked in — the field is left off, not null.</p>}
                </div>
              </div>

              {orphaned ? (
                <p className="muted">
                  <Info size={12} /> <b>This form&rsquo;s round is gone.</b> #25 left the cycle
                  name, the year and the class name off rather than refusing the read — classes
                  are stored per academic year, and the year comes from the cycle. The form itself
                  is intact; the link is not.
                </p>
              ) : null}

              <div className="field-grid">
                <div>
                  <p className="muted">Round</p>
                  <p>{application.admissionCycleName ?? <span className="muted">not resolved</span>}</p>
                  <p className="mono muted">{application.admissionCycleDocsId}</p>
                </div>
                <div>
                  <p className="muted">Academic year</p>
                  <p className="mono">
                    {application.academicYear ?? <span className="muted">not resolved</span>}
                  </p>
                </div>
                <div>
                  <p className="muted">Applied class</p>
                  <p>{application.appliedClassName ?? <span className="muted">not resolved</span>}</p>
                  <p className="mono muted">{application.appliedClassDocsId}</p>
                </div>
                <div>
                  <p className="muted">Assigned officer</p>
                  {application.assignedAdmissionOfficerDocsId
                    ? <p className="mono">{application.assignedAdmissionOfficerDocsId}</p>
                    : <p className="muted">Nobody. #22 assigns one and is not built.</p>}
                </div>
              </div>

              {application.nextStep ? (
                <p className="muted"><Info size={12} /> {application.nextStep}</p>
              ) : null}

              {application.status === 'DRAFT' ? (
                <p className="muted">
                  <Info size={12} /> Submitting freezes this form. The round has to be
                  <b> OPEN</b> and inside its published window <i>at the moment you press it</i>,
                  not at the moment the draft was started — a form begun before the deadline and
                  sent after it is a late application.
                </p>
              ) : (
                <p className="muted">
                  <Info size={12} /> This form is past DRAFT, so <b>#19 refuses it</b> —
                  submitting again would overwrite the moment the family sent it. The button
                  still sends, because that refusal is worth being able to see.
                </p>
              )}

              <p className="muted">
                Started {readable(application.createdAt)}, last changed{' '}
                {readable(application.updatedAt)}.
                {application.submittedAt
                  ? ` Submitted ${readable(application.submittedAt)}.`
                  : ' Never submitted — #19 is what submits one, and it is not built.'}
              </p>
            </div>
          </Card>

          <Card
            title="Where it can go from here"
            description="The graph in controllers/crm/README.md, which is the specification — the model README's diagram shows a subset. It only goes forwards, and ENROLLED and REJECTED are the ends of it."
          >
            <div className="stack">
              <pre className="resp-body">{markCurrent(application.status)}</pre>

              <p className="muted">
                <Info size={12} /> <b>Everything down to <span className="mono">APPROVED</span> is
                built.</b> What is not is the offer half — #29, #30 and #33 — so a form can now be
                decided but cannot yet be offered a seat, which is where this module runs out of
                road until <span className="mono">student</span> exists.
              </p>

              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>From</th>
                      <th>To</th>
                      <th>What does it</th>
                      <th>Built</th>
                    </tr>
                  </thead>
                  <tbody>
                    {MOVES.map(([from, to, owner, built]) => (
                      // The row for the move OUT of where this form is now, highlighted — that is
                      // the one somebody reading this page actually wants.
                      <tr key={`${from}-${to}`} data-now={from === application.status || undefined}>
                        <td className="mono">{from}</td>
                        <td className="mono">{to}</td>
                        <td>{owner}</td>
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
                <Info size={12} /> <b>No endpoint sets a status by being told to</b>, and there
                will not be one. <span className="mono">OFFERED</span> is #29&rsquo;s consequence,{' '}
                <span className="mono">OFFER_ACCEPTED</span> is #30&rsquo;s and{' '}
                <span className="mono">ENROLLED</span> is #33&rsquo;s — each move has its own
                preconditions and its own side effects, so a single &ldquo;set the
                status&rdquo; call would be ten endpoints wearing one name.
              </p>
            </div>
          </Card>

          <Card
            title={`Guardians — ${guardians.length}`}
            description="A snapshot taken when the form was filled in, not a link to the inquiry. Editing the lead afterwards must not rewrite a form the school has already acted on."
          >
            {guardians.length === 0 ? (
              <Empty
                title="No guardians on this form"
                description="#17 requires at least one, so a form with none was put in directly rather than through the API."
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
                      <th>Occupation</th>
                      <th>Primary</th>
                    </tr>
                  </thead>
                  <tbody>
                    {guardians.map((one, at) => (
                      <tr key={`${one.fullName}-${at}`}>
                        <td>{one.fullName}</td>
                        <td>{one.relation}</td>
                        {/* An em dash rather than a blank: a field nobody filled in is left off
                            the response entirely, and a blank cell would read as an empty
                            string that was sent. */}
                        <td>{one.phoneNumber ?? <span className="muted">—</span>}</td>
                        <td>{one.emailAddress ?? <span className="muted">—</span>}</td>
                        <td>{one.occupation ?? <span className="muted">—</span>}</td>
                        <td>{one.primaryContact ? 'yes' : <span className="muted">—</span>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card
            title={`Form answers — ${answers.length}`}
            description="Whatever this school asks for beyond the fixed fields. NOTHING VALIDATES THESE: there is no form definition model, so what comes back is exactly what was sent."
          >
            {answers.length === 0 ? (
              <Empty
                title="No extra answers"
                description="The map is left off the response entirely rather than sent as {} — an absent map and an empty one say the same thing, and one of them is noise on every read."
              />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr><th>Question</th><th>Answer</th></tr>
                  </thead>
                  <tbody>
                    {answers.map(([question, answer]) => (
                      <tr key={question}>
                        <td className="mono">{question}</td>
                        {/* Stringified, because the value is whatever was sent — a number, a
                            boolean, a nested object. Rendering it raw is how React throws. */}
                        <td>{typeof answer === 'string' ? answer : JSON.stringify(answer)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card
            title={`Reviews — ${application.reviewCount}`}
            description="Oldest round first, then by when it was created. A round can hold more than one review — an interview and a test — so the round alone is not an order."
            action={
              <Button look="primary" icon={UserPlus} onClick={() => setAssigning(true)}>
                Assign a reviewer
              </Button>
            }
          >
            {reviews.length === 0 ? (
              <Empty
                title="Nobody is reviewing this yet"
                description="#26 puts it on somebody's desk. It assigns the work rather than doing it — the score and the recommendation are #27, which is not built, so a review cannot move past PENDING."
                action={
                  <Button look="primary" icon={UserPlus} onClick={() => setAssigning(true)}>
                    Assign a reviewer
                  </Button>
                }
              />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th className="num">Round</th>
                      <th>Reviewer</th>
                      <th>Role</th>
                      <th>Status</th>
                      <th className="num">Score</th>
                      <th>Recommendation</th>
                      <th>Due</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reviews.map((one) => (
                      <tr key={one.admissionReviewId}>
                        <td className="num">{one.reviewRound}</td>
                        {/* NAMED since #26 arrived — #25 resolves every reviewer on the form
                            in one query. A reviewer who is not this school's staff any more
                            reads back with no name rather than a made-up one. */}
                        <td>
                          {one.reviewerName ?? <span className="muted">not staff any more</span>}
                          <br />
                          <span className="mono muted">{one.reviewerDocsId}</span>
                        </td>
                        <td>{one.reviewerRole}</td>
                        <td><Badge tone={REVIEW_TONE[one.status]}>{one.status}</Badge></td>
                        <td className="num">{one.score ?? <span className="muted">—</span>}</td>
                        <td>{one.recommendation ?? <span className="muted">—</span>}</td>
                        <td title={one.dueAt ?? 'not set'}>
                          {one.dueAt ? compact(one.dueAt) : <span className="muted">—</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card
            title={`Offers — ${application.offerCount}`}
            description="First revision first, superseded ones included. A later offer supersedes the one before it, and showing only the live one would make what the school offered FIRST unanswerable."
          >
            {offers.length === 0 ? (
              <Empty
                title="No offers, and nothing can make one yet"
                description="#29 issues an offer, #30 records the family's answer and #31 withdraws it. None are built. An offer also needs an APPROVED application, and #20 is what approves one — also not built."
              />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th className="num">Rev</th>
                      <th>Offer no</th>
                      <th>Offered class</th>
                      <th>Status</th>
                      <th>Offered</th>
                      <th>Expires</th>
                      <th>Answer</th>
                    </tr>
                  </thead>
                  <tbody>
                    {offers.map((one) => (
                      <tr key={one.admissionOfferId}>
                        <td className="num">{one.revisionNo}</td>
                        <td><span className="mono">{one.offerNo}</span></td>
                        {/* The offered class is NOT always the applied one — a school assesses a
                            child and offers a different grade — so it carries its own name. */}
                        <td>
                          {one.offeredClassName ?? <span className="muted">not resolved</span>}
                          <br />
                          <span className="mono muted">{one.offeredClassDocsId}</span>
                        </td>
                        <td><Badge tone={OFFER_TONE[one.status]}>{one.status}</Badge></td>
                        <td title={one.offeredAt ?? 'not set'}>
                          {one.offeredAt ? compact(one.offeredAt) : <span className="muted">—</span>}
                        </td>
                        <td title={one.expiresAt ?? 'not set'}>
                          {one.expiresAt ? compact(one.expiresAt) : <span className="muted">—</span>}
                        </td>
                        <td>{one.response ?? <span className="muted">no answer yet</span>}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card
            title={`Evidence — ${evidence.length}`}
            description="DocumentRecord ids. This module stores the ids; the documents module owns the files and is what turns one into something downloadable."
          >
            {evidence.length === 0 ? (
              <Empty
                title="Nothing attached"
                description="#23 replaces the whole evidence list and is not built, so every application reads back with an empty one. An empty LIST, note — not an absent field, because a list that is there and empty is a different thing from a field nobody set."
              />
            ) : (
              <ul>
                {evidence.map((one) => <li key={one}><span className="mono">{one}</span></li>)}
              </ul>
            )}
          </Card>

          {application.withdrawnAt || application.resultingStudentDocsId ? (
            <Card title="How it ended">
              <div className="field-grid">
                {application.withdrawnAt ? (
                  <div>
                    <p className="muted">Withdrawn</p>
                    <p>{readable(application.withdrawnAt)}</p>
                    <p className="muted">{application.withdrawalReason ?? 'No reason given.'}</p>
                  </div>
                ) : null}
                {application.resultingStudentDocsId ? (
                  <div>
                    <p className="muted">Became a student</p>
                    <p className="mono">{application.resultingStudentDocsId}</p>
                  </div>
                ) : null}
              </div>
            </Card>
          ) : null}
          <AssignReviewer
            open={assigning}
            application={application}
            onClose={() => setAssigning(false)}
            onAssigned={load}
          />

          <Decide
            open={deciding}
            application={application}
            onClose={() => setDeciding(false)}
            onDecided={load}
          />
        </>
      ) : null}
    </div>
  )
}

/**
 * #26 — put this application on somebody's desk.
 *
 * THE STAFF ID IS A PLAIN BOX, not a picker. Two refusals live on this field —
 * STAFF_NOT_FOUND for an id that is nobody's, and the same code for ANOTHER SCHOOL'S real staff
 * id — and the second is the one worth reaching. A picker of this school's staff would make it
 * unreachable, and it is the only thing proving the lookup is tenant-scoped.
 *
 * THE ROLE IS A PLAIN BOX TOO, because it is a free string on the server. An enum here would
 * invent a closed set the API does not have.
 *
 * THE ROUND IS LEFT EMPTY BY DEFAULT, so the common request is the one that omits it and gets
 * round 1. Sending 0 or 2026 is a documented 400 worth being able to send.
 *
 * NOTHING IS DISABLED. Assigning the same person twice is REVIEWER_ALREADY_ASSIGNED and assigning
 * on a DRAFT is APPLICATION_NOT_REVIEWABLE; both are the interesting answers here.
 */
function AssignReviewer({ open, application, onClose, onAssigned }) {
  const { call } = useApi()
  const [reviewerDocsId, setReviewer] = useState('')
  const [reviewerRole, setRole] = useState('ADMISSION_OFFICER')
  const [reviewRound, setRound] = useState('')
  const [dueAt, setDueAt] = useState('')
  const [notes, setNotes] = useState('')
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  const body = {
    reviewerDocsId,
    reviewerRole,
    ...(reviewRound === '' ? {} : { reviewRound: Number(reviewRound) }),
    ...(dueAt ? { dueAt } : {}),
    ...(notes ? { notes } : {}),
  }

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('assign-admission-reviewer', {
      label: 'Assign a reviewer',
      pathParams: { admissionApplicationId: application.admissionApplicationId },
      body,
    })
    setSaving(false)
    if (result.ok) { onAssigned(); onClose() } else { setRefused(result.bodyJson ?? {}) }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title="Assign a reviewer"
      description="This assigns the work, not the result. The review is created PENDING; the score and the recommendation are #27, which is not built."
      endpoint={<EndpointTag id="assign-admission-reviewer" name="Assign" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Assign</Button>
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

        <Field
          label="Reviewer"
          hint="A staff document id of THIS school. An id that is nobody's is 404 STAFF_NOT_FOUND — and so is another school's real staff id, which is the case worth trying."
        >
          <Input value={reviewerDocsId} onChange={(e) => setReviewer(e.target.value)} placeholder="67aa15d9dc3f7d0088888888" />
        </Field>

        <Field
          label="Acting as"
          hint="A free string, not an enum. Schools run interviews, entrance tests and principal rounds under names of their own, so the API stores whatever you send."
        >
          <Input value={reviewerRole} onChange={(e) => setRole(e.target.value)} placeholder="ADMISSION_OFFICER" />
        </Field>

        <Field
          label="Round"
          hint="Leave it empty for round 1, which is what most applications get. A round can hold more than one reviewer — an interview and a test are both round 1 — so the same round with a different person is allowed and the same person twice is not. 0 and 2026 are both 400s worth sending."
        >
          <Input value={reviewRound} onChange={(e) => setRound(e.target.value)} placeholder="1" />
        </Field>

        <Field
          label="Due by"
          hint="Optional, and an instant. A date in the PAST is accepted on purpose — a school catching up on paperwork records a review that was due last week."
        >
          <Input value={dueAt} onChange={(e) => setDueAt(e.target.value)} placeholder="2027-03-15T17:00:00Z" />
        </Field>

        <Field label="Notes" hint="Optional, up to 2000 characters.">
          <Input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Interview first, then the written test." />
        </Field>

        {application.status === 'DRAFT' ? (
          <p className="muted">
            <Info size={12} /> <b>This form is still a DRAFT</b>, so assigning will answer{' '}
            <span className="mono">409 APPLICATION_NOT_REVIEWABLE</span> — the family has not sent
            it. Submit it with #19 first, or send this anyway and read the refusal.
          </p>
        ) : null}
      </div>
    </Modal>
  )
}

/**
 * #20 — what the school decided.
 *
 * EVERY DECISION IS OFFERED, including the ones the table refuses from where this form is. That is
 * the same call MoveStatus makes on a cycle: INVALID_APPLICATION_TRANSITION is the most
 * interesting answer this endpoint gives, and a picker that hid the illegal ones would make it
 * unreachable. The hint says which are legal; it does not enforce them.
 *
 * THE NOTE IS NOT MADE REQUIRED IN THE BROWSER even for REJECT. DECISION_NOTE_REQUIRED is a
 * documented 400 and somebody testing this needs to be able to send it — the screen says which
 * decisions will refuse without one instead of refusing for the server.
 *
 * THE VERSION IS PRE-FILLED from what #25 last read, because that is the only way to send a
 * correct one — and left editable, because sending a stale one on purpose is how you see
 * CONCURRENT_MODIFICATION.
 */
/**
 * EVERY status, because the request can now name any of them.
 *
 * #20 takes an AdmissionApplicationStatus directly, the way #3 does for a cycle — so the picker
 * offers the whole enum including the ones this endpoint must never set. OFFERED, OFFER_ACCEPTED
 * and ENROLLED belong to #29, #30 and #33; WITHDRAWN is #21's; DRAFT and SUBMITTED are the
 * family's side. Each is a documented refusal and the transition table is what makes it one, which
 * is exactly the thing worth being able to see fail.
 */
const DECISIONS = ['APPROVED', 'REJECTED', 'WAITLISTED', 'ADDITIONAL_INFORMATION_REQUIRED',
  'UNDER_REVIEW', 'OFFERED', 'OFFER_ACCEPTED', 'ENROLLED', 'WITHDRAWN', 'DRAFT', 'SUBMITTED']

/** What each status will actually accept. Mirrors DECISION_MOVES in AdmissionApplicationService. */
const ALLOWED = {
  SUBMITTED: ['APPROVED', 'REJECTED', 'WAITLISTED', 'ADDITIONAL_INFORMATION_REQUIRED'],
  UNDER_REVIEW: ['APPROVED', 'REJECTED', 'WAITLISTED', 'ADDITIONAL_INFORMATION_REQUIRED'],
  ADDITIONAL_INFORMATION_REQUIRED: ['UNDER_REVIEW', 'APPROVED', 'REJECTED', 'WAITLISTED'],
  WAITLISTED: ['APPROVED', 'REJECTED'],
}

const NEEDS_A_NOTE = ['REJECTED', 'ADDITIONAL_INFORMATION_REQUIRED']

function Decide({ open, application, onClose, onDecided }) {
  const { call } = useApi()
  const [decision, setDecision] = useState('APPROVED')
  const [note, setNote] = useState('')
  const [version, setVersion] = useState('')
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  const legal = ALLOWED[application.status] ?? []

  const body = {
    status: decision,
    ...(note ? { note } : {}),
    ...(version === '' ? {} : { version: Number(version) }),
  }

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('decide-admission-application', {
      label: `Decide: ${decision}`,
      pathParams: { admissionApplicationId: application.admissionApplicationId },
      body,
    })
    setSaving(false)
    if (result.ok) { onDecided(); onClose() } else { setRefused(result.bodyJson ?? {}) }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={`Decide from ${application.status}`}
      description="You name the status the form moves to, exactly as #3 does for a cycle — one set of words in and the same set out. The transition table is what refuses ENROLLED, not the shape of the request."
      endpoint={<EndpointTag id="decide-admission-application" name="Decide" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Record it</Button>
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

        <Field
          label="Move it to"
          hint={legal.length
            ? `From ${application.status} the school can move it to: ${legal.join(', ')}. Every other status is offered anyway — INVALID_APPLICATION_TRANSITION is a documented answer worth being able to see, and its message lists what is reachable. OFFERED, OFFER_ACCEPTED and ENROLLED are #29's, #30's and #33's consequences and this endpoint refuses all three.`
            : `${application.status} can be moved nowhere by this endpoint. Every option here will be refused, and the message says WHY rather than just no — which is the thing to read.`}
        >
          <Select
            value={decision}
            options={DECISIONS.map((one) => ({
              value: one,
              label: legal.includes(one) ? one : `${one} — refused from ${application.status}`,
            }))}
            label="New status"
            onChange={setDecision}
          />
        </Field>

        <Field
          label="Why"
          hint={NEEDS_A_NOTE.includes(decision)
            ? `Moving to ${decision} will answer 400 DECISION_NOTE_REQUIRED without one, and a blank counts as none. It is KEPT on the application and reads back on #25 — a refusal with no reason is the part of an admissions record worth the most. Send it empty anyway if you want to see the refusal.`
            : 'Optional for this move. Kept on the application when sent, and the old one is left alone when it is not.'}
        >
          <Input value={note} onChange={(e) => setNote(e.target.value)}
            placeholder="Interview scores below the cut-off for Grade 7" />
        </Field>

        <Field
          label="Version"
          hint={`Optional. ${application.version ?? 'unknown'} is what this page last read. Send it and a form somebody else decided in the meantime answers 409 CONCURRENT_MODIFICATION; change it to see that happen; leave it empty and last write wins.`}
        >
          <Input value={version} onChange={(e) => setVersion(e.target.value)}
            placeholder={String(application.version ?? '')} />
        </Field>

        {decision === 'UNDER_REVIEW' && application.status !== 'ADDITIONAL_INFORMATION_REQUIRED'
          ? (
            <p className="muted">
              <Info size={12} /> <b>Going back to UNDER_REVIEW is legal from exactly one
              status</b> — <span className="mono">ADDITIONAL_INFORMATION_REQUIRED</span>. The graph
              draws that edge and #26 deliberately does not make it: assigning another reviewer is
              not what decides the information turned up.
            </p>
          ) : null}
      </div>
    </Modal>
  )
}
