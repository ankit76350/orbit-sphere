import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Briefcase, Gavel, Info, RefreshCw, Send, Ticket, UserPlus } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { compact, readable, toInstant, toLocalInput, zoneLabel } from './admissionDates.js'
import { childPath, screenPath } from '../../../paths.js'

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
  const [owning, setOwning] = useState(false)
  const [offering, setOffering] = useState(false)
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
            description="Oldest round first, then by when it was created. A round can hold more than one review — an interview and a test — so the round alone is not an order. Open one to record what was found."
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
                      // Opening a review is its OWN address — a row inside a row, which is what
                      // childPath is for. Recording on it lives there rather than in a modal
                      // here: a review has a history worth seeing before you write to it.
                      <tr
                        key={one.admissionReviewId}
                        data-opens
                        onClick={() => navigate(childPath('school', 'crm', 'applications',
                          application.admissionApplicationId, 'reviews', one.admissionReviewId))}
                      >
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
            description="ONE OFFER LETTER PER ADMISSION. If it expires the school extends it, and if anything else changes the school edits it — there is no second document and no revision history, because a family holds one letter."
            action={
              <Button look="primary" icon={Ticket} onClick={() => setOffering(true)}>
                {offers.length ? 'Offer again — refused' : 'Issue an offer'}
              </Button>
            }
          >
            {offers.length === 0 ? (
              <Empty
                title="Nothing has been offered yet"
                description="#29 issues an offer; #30 records the family's answer and #31 withdraws it, and neither is built. An offer needs an APPROVED or WAITLISTED form and #20 is what approves one — approving is the school saying yes, and an offer is what the family gets to say yes to."
                action={
                  <Button look="primary" icon={Ticket} onClick={() => setOffering(true)}>
                    Issue an offer
                  </Button>
                }
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
          <Card
            title="Whose form this is"
            description="The admission officer OWNS it; a reviewer ASSESSES it. Different jobs — this one chases the missing certificate and makes sure the form does not sit for three weeks."
            action={
              <Button look="warn" icon={Briefcase} onClick={() => setOwning(true)}>
                {application.assignedAdmissionOfficerDocsId ? 'Give it to somebody else' : 'Give it to an officer'}
              </Button>
            }
          >
            {application.assignedAdmissionOfficerDocsId ? (
              <div className="field-grid">
                <div>
                  <p className="muted">Admission officer</p>
                  <p>{application.assignedAdmissionOfficerName
                    ?? <span className="muted">not staff any more</span>}</p>
                  <p className="mono muted">{application.assignedAdmissionOfficerDocsId}</p>
                </div>
              </div>
            ) : (
              <Empty
                title="It belongs to nobody"
                description="#22 puts it on an officer's worklist, which is what #24's officer filter reads. Assigning moves no status and stamps no date — owning a form is not deciding it."
                action={
                  <Button look="warn" icon={Briefcase} onClick={() => setOwning(true)}>
                    Give it to an officer
                  </Button>
                }
              />
            )}
          </Card>

          {/* MOUNTED ONLY WHILE OPEN, unlike the two modals below it. Both of its boxes are
              SEEDED from the application — the officer and the version — and a component that
              stayed mounted would keep the values it was first given, so a send elsewhere or a
              Refresh behind this would leave the version one behind and the next write stale for
              no reason. Mounting on demand seeds them on every open and needs no effect to do it.
              The same call ReviewDetail's record modal makes. */}
          {offering ? (
            <IssueOffer
              application={application}
              onClose={() => setOffering(false)}
              onIssued={load}
            />
          ) : null}

          {owning ? (
            <AssignOfficer
              application={application}
              onClose={() => setOwning(false)}
              onAssigned={load}
            />
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
 * THE REVIEWER IS A PICKER AND A BOX. The picker lists this school's staff, so the working case is
 * one click. The box is what gets sent and stays typeable, because the refusal worth reaching here
 * is ANOTHER SCHOOL'S REAL STAFF ID — a real person, scoped away — and no picker of this school's
 * staff can offer one. An id that is nobody's is the same 404 and proves less.
 *
 * THE ROLE IS A PLAIN BOX, because it is a free string on the server. An enum here would invent a
 * closed set the API does not have.
 *
 * THE ROUND IS A NUMBER BOX WITH NO min OR max. Typed, because the field is an integer — but
 * unbounded, because 0, 2026 and a round with nothing before it are all documented refusals, and
 * an input that blocked them would put the server's own checks out of reach. The hint says which
 * rounds are open and what the next one is; it does not enforce either. Left empty by default, so
 * the common request is the one that omits it and gets round 1.
 *
 * THE DUE DATE IS A datetime-local PICKER, because the field is an Instant and typing one by hand
 * is where the mistake lives — an Indian school's 5 pm is 11:30Z, not 17:00Z. The instant that will
 * be sent is shown under it, and a date in the PAST is deliberately pickable.
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

  //! THIS SCHOOL'S STAFF, read when the modal opens. One page of 100 covers a school's staff list
  //! comfortably; a school with more would need paging here, and would notice.
  const [staff, setStaff] = useState([])
  const [loadingStaff, setLoadingStaff] = useState(false)

  useEffect(() => {
    if (!open) return
    let cancelled = false
    const load = async () => {
      setLoadingStaff(true)
      const result = await call('list-staff', {
        label: "This school's staff",
        query: { size: '100', sort: 'fullName' },
      })
      if (cancelled) return
      setLoadingStaff(false)
      setStaff(result.ok ? (result.bodyJson?.content ?? []) : [])
    }
    load()
    return () => { cancelled = true }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  //! WHICH ROUNDS ALREADY EXIST, straight off the reviews #25 returned — no extra read. The
  //! rounds are the SCHOOL'S stages, so this counts every reviewer's, not one person's.
  const openRounds = [...new Set((application.reviews ?? []).map((one) => one.reviewRound))]
    .filter((one) => typeof one === 'number')
    .sort((a, b) => a - b)
  const nextRound = openRounds.length ? Math.max(...openRounds) + 1 : 1

  const body = {
    reviewerDocsId,
    reviewerRole,
    ...(reviewRound === '' ? {} : { reviewRound: Number(reviewRound) }),
    ...(dueAt ? { dueAt } : {}),
    ...(notes ? { notes } : {}),
  }

  //! THE FORM HOLDS THE INSTANT, never the local reading — the picker is a view over it. So what
  //! is shown beside the box is exactly what will be sent.
  const pickDue = (local) => setDueAt(toInstant(local))

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

        {/* THE READ THAT FILLS THE PICKER, fired when this modal opens. One page of 100, which
            covers a school's staff list comfortably. */}
        <p className="muted">
          <EndpointTag id="list-staff" name="This school's staff"
            query={{ size: '100', sort: 'fullName' }} />
        </p>

        <Field
          label="Reviewer"
          hint="This school's staff. Picking one fills the box below, which is what gets sent."
        >
          <Select
            value={reviewerDocsId}
            options={[
              { value: '', label: loadingStaff
                ? 'reading the staff…'
                : (staff.length
                  ? `${staff.length} staff member${staff.length === 1 ? '' : 's'} — pick one`
                  : 'no staff in this school') },
              ...staff.map((one) => ({
                value: one.staffDocsId,
                label: `${one.fullName}${one.employeeNo ? ` · ${one.employeeNo}` : ''}`,
              })),
            ]}
            label="Reviewer"
            onChange={setReviewer}
          />
        </Field>

        <Field
          label="…or the staff id, typed"
          hint="The box is what gets sent. An id that is nobody's is 404 STAFF_NOT_FOUND — and so is ANOTHER SCHOOL'S REAL staff id, which is the case worth trying and the one the picker above can never offer."
        >
          <Input value={reviewerDocsId} onChange={(e) => setReviewer(e.target.value)}
            placeholder="67aa15d9dc3f7d0088888888" />
        </Field>

        <Field
          label="Acting as"
          hint="A free string, not an enum. Schools run interviews, entrance tests and principal rounds under names of their own, so the API stores whatever you send."
        >
          <Input value={reviewerRole} onChange={(e) => setRole(e.target.value)} placeholder="ADMISSION_OFFICER" />
        </Field>

        <Field
          label="Round"
          hint={`${openRounds.length
            ? `Rounds ${openRounds.join(', ')} are open on this form, so the next one you may start is ${nextRound}.`
            : 'No rounds yet, so round 1 is the only one that can be started.'} Leave it empty for round 1. A round holds more than one reviewer — an interview and a test are both round 1 — so the same round with a DIFFERENT person is allowed and the same person twice is not. Rounds run 1, 2, 3 with no gaps: asking for ${nextRound + 1} is REVIEW_ROUND_OUT_OF_ORDER, and 0 and 2026 are both 400s. All three are worth sending.`}
        >
          <Input type="number" value={reviewRound} onChange={(e) => setRound(e.target.value)}
            placeholder="1" />
        </Field>

        <Field
          label="Due by"
          hint={`Optional. Picked in ${zoneLabel()} and sent as an instant — a school's 5 pm is not 17:00Z. A date in the PAST is accepted on purpose: a school catching up on paperwork records a review that was due last week. Clear it to leave the field off entirely.`}
        >
          <Input type="datetime-local" step="1" value={toLocalInput(dueAt)}
            onChange={(e) => pickDue(e.target.value)} />
        </Field>

        {dueAt ? (
          <p className="muted">
            Sends <span className="mono">{dueAt}</span> — that is{' '}
            {readable(dueAt)}.
          </p>
        ) : null}

        <Field label="Notes" hint="Optional, up to 2000 characters.">
          <Input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Interview first, then the written test." />
        </Field>

        {reviewRound !== '' && Number(reviewRound) > nextRound ? (
          <p className="muted">
            <Info size={12} /> <b>Round {reviewRound} has nothing before it.</b> This will answer{' '}
            <span className="mono">409 REVIEW_ROUND_OUT_OF_ORDER</span> — rounds run 1, 2, 3 with
            no gaps, and {nextRound} is the next one that can be started. Send it anyway to read
            the refusal.
          </p>
        ) : null}

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

/**
 * The review statuses that hold an APPROVAL up. Mirrors STILL_ASSESSING in the service.
 *
 * CANCELLED IS NOT ONE OF THEM, which is the part worth having written down twice: work the school
 * called off is a settled answer, and waiting for it would mean waiting for something that is never
 * going to happen.
 */
const STILL_ASSESSING = ['PENDING', 'IN_PROGRESS']

function Decide({ open, application, onClose, onDecided }) {
  const { call } = useApi()
  const [decision, setDecision] = useState('APPROVED')
  const [note, setNote] = useState('')
  const [version, setVersion] = useState('')
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  const legal = ALLOWED[application.status] ?? []

  //! WHAT WILL BLOCK AN APPROVAL, read off #25 rather than guessed. The page already has every
  //! review in full, so the screen can name the same rounds the refusal will — before it is sent.
  const openReviews = (application.reviews ?? [])
    .filter((one) => STILL_ASSESSING.includes(one.status))

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

        {openReviews.length ? (
          <p className="muted">
            <Info size={12} /> <b>{openReviews.length} review
            {openReviews.length === 1 ? ' is' : 's are'} still open</b> —{' '}
            <span className="mono">
              {openReviews.map((one) => `round ${one.reviewRound} (${one.status})`).join(', ')}
            </span>. <b>APPROVED will answer 409 REVIEWS_STILL_OUTSTANDING</b> while that is true,
            and the message names these same rounds. Everything else goes through: refusing,
            waitlisting and asking for more are all answers a head can give over an incomplete
            picture — and asking for more is often exactly why a review is still open.
            {' '}Finish one with <b>#27c</b> or call it off with <b>#27d</b>; a{' '}
            <span className="mono">CANCELLED</span> review does not hold an approval up. Nothing
            here is switched off — send APPROVED to read the refusal.
          </p>
        ) : null}

        {openReviews.length === 0 && (application.reviews ?? []).length > 0
          ? (
            <p className="muted">
              <Info size={12} /> Every review on this form is settled, so <b>APPROVED is not
              blocked</b>. The check reads the open ones — having none left and never having had
              any are the same answer to it.
            </p>
          ) : null}

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

/**
 * #22 — whose form this is.
 *
 * AN OFFICER OWNS IT; A REVIEWER ASSESSES IT. Two different jobs, two different endpoints, and the
 * screen says so rather than leaving somebody to work out why there are two "assign" buttons. This
 * one moves no status and stamps no date, which is the visible difference from #26.
 *
 * REASSIGNING IS THE NORMAL CASE, so the button says "give it to somebody else" once there is an
 * owner. Assigning the same person twice is a quiet 200 — unlike #27b, which refuses a second
 * start. The two are different intents: "make sure this is on Anita's list" is worth being
 * idempotent; "pick up work nobody has" is a claim two people cannot both make.
 *
 * THERE IS NO UNASSIGN, so the picker cannot be cleared into a send. Sending an empty body is 400
 * VALIDATION_FAILED and it stays reachable — the box below the picker is free text.
 *
 * NOTHING IS SWITCHED OFF. A REJECTED, WITHDRAWN or ENROLLED form answers
 * APPLICATION_NOT_ASSIGNABLE and the screen says so before it is sent.
 */
const WORK_IS_OVER = ['REJECTED', 'WITHDRAWN', 'ENROLLED']

function AssignOfficer({ application, onClose, onAssigned }) {
  const { call } = useApi()
  const [officer, setOfficer] = useState(application.assignedAdmissionOfficerDocsId ?? '')
  //! SEEDED FROM THE FORM, because a version nobody can read is a parameter nobody can use — and
  //! this is the endpoint where sending it matters most: what a stale write would overwrite is
  //! somebody else's decision about who owns the form.
  const [version, setVersion] = useState(String(application.version ?? ''))
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  //! THIS SCHOOL'S STAFF, read when the modal opens — the same call and the same reasoning as the
  //! reviewer picker above. One page of 100 covers a school's staff list comfortably.
  const [staff, setStaff] = useState([])
  const [loadingStaff, setLoadingStaff] = useState(false)

  //! ONCE, ON MOUNT — which IS "when it opens", because this modal is only mounted while open.
  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoadingStaff(true)
      const result = await call('list-staff', {
        label: "This school's staff",
        query: { size: '100', sort: 'fullName' },
      })
      if (cancelled) return
      setLoadingStaff(false)
      setStaff(result.ok ? (result.bodyJson?.content ?? []) : [])
    }
    load()
    return () => { cancelled = true }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const body = {
    ...(officer ? { assignedAdmissionOfficerDocsId: officer } : {}),
    ...(version === '' ? {} : { version: Number(version) }),
  }

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('assign-admission-officer', {
      label: 'Give it to an officer',
      pathParams: { admissionApplicationId: application.admissionApplicationId },
      body,
    })
    setSaving(false)
    if (result.ok) { onAssigned(); onClose() } else { setRefused(result.bodyJson ?? {}) }
  }

  const finished = WORK_IS_OVER.includes(application.status)
  const current = application.assignedAdmissionOfficerDocsId

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={current ? 'Give it to somebody else' : 'Give it to an admission officer'}
      description="The officer owns the form: they chase what is missing and make sure it does not sit. It moves no status and stamps no date — owning a form is not deciding it."
      endpoint={<EndpointTag id="assign-admission-officer" name="Assign" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Give it to them</Button>
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

        {finished ? (
          <p className="muted">
            <Info size={12} /> <b>This form is {application.status}</b>, so this will answer{' '}
            <span className="mono">409 APPLICATION_NOT_ASSIGNABLE</span>. A form is given to
            somebody so they can move it along, and this one has stopped. Send it to read the
            refusal.
          </p>
        ) : null}

        {application.status === 'DRAFT' ? (
          <p className="muted">
            <Info size={12} /> <b>A DRAFT can be given to somebody</b>, unlike a review — #26
            refuses one because there is nothing to assess yet, but keying a paper form in and
            handing it to somebody to chase the family is a real day&rsquo;s work.
          </p>
        ) : null}

        {/* THE READ THAT FILLS THE PICKER, the same call the reviewer modal makes. */}
        <p className="muted">
          <EndpointTag id="list-staff" name="This school's staff"
            query={{ size: '100', sort: 'fullName' }} />
        </p>

        <Field
          label="Admission officer"
          hint="This school's staff. Picking one fills the box below, which is what gets sent."
        >
          <Select
            value={officer}
            options={[
              { value: '', label: loadingStaff
                ? 'reading the staff…'
                : (staff.length
                  ? `${staff.length} staff member${staff.length === 1 ? '' : 's'} — pick one`
                  : 'no staff in this school') },
              ...staff.map((one) => ({
                value: one.staffDocsId,
                label: `${one.fullName}${one.employeeNo ? ` · ${one.employeeNo}` : ''}`,
              })),
            ]}
            label="Admission officer"
            onChange={setOfficer}
          />
        </Field>

        <Field
          label="Staff id"
          hint={current
            ? `It is on ${application.assignedAdmissionOfficerName ?? current} now. Sending the SAME id again is a quiet 200 — reassigning is the normal case here, and there is nothing to refuse. Clear it for 400 VALIDATION_FAILED: there is no unassign.`
            : 'What is actually sent. An id that is not this school\'s staff is 404 STAFF_NOT_FOUND — including another school\'s, which is a real id somewhere. Clear it for 400 VALIDATION_FAILED.'}
        >
          <Input value={officer} onChange={(e) => setOfficer(e.target.value)}
            placeholder="6aa91f16ebf05fbafaa4ce22" />
        </Field>

        <Field
          label="Version"
          hint={`Filled in from what this page last read${application.version === undefined ? '' : ` — version ${application.version}`}. A form somebody else reassigned in the meantime answers 409 CONCURRENT_MODIFICATION; change it to see that happen. Worth keeping here, since what a stale write would overwrite is somebody else's decision about who owns this form. Clear it and last write wins.`}
        >
          <Input type="number" value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>

        <p className="muted">
          <Info size={12} /> Once this is set, <b>#24 can filter the pipeline by it</b> — that
          filter matched nothing for any id until #22 existed, because nothing could fill the
          field. The rows name the officer as well, resolved in one query for the whole page.
        </p>
      </div>
    </Modal>
  )
}

/**
 * #29 — the school offers a seat.
 *
 * ONE OFFER LETTER PER ADMISSION, so this modal is mostly reachable once. The button stays after
 * that and still sends, because which refusal comes back is worth seeing: a form that has been
 * offered is APPLICATION_NOT_APPROVED (issuing moved it to OFFERED), and OFFER_ALREADY_ISSUED is
 * the deeper guard behind it.
 *
 * THE CLASS PICKER IS THE CYCLE'S SEAT TABLE, not the school's class list. A class the round has no
 * seats for is 409 CLASS_NOT_IN_CAPACITY — the same rule #17 applies to the class applied for — so
 * offering one is a refusal rather than a choice. The box below stays free text, so that refusal is
 * still one paste away.
 *
 * AND IT IS NOT LIMITED TO THE APPLIED CLASS. A school assesses a child and offers a different
 * grade; the offer carries a class of its own for exactly that, so every seated class is offered
 * here rather than just the one on the form.
 *
 * NO STATUS BOX AND NO REVISION BOX. Issuing is the endpoint, and the revision is always 1 because
 * there is only ever one offer — it is there to line up with the declared unique index rather than
 * to be chosen.
 *
 * NOTHING IS SWITCHED OFF. A form nobody approved answers APPLICATION_NOT_APPROVED and the screen
 * says so before it is sent.
 */
const OFFERABLE = ['APPROVED', 'WAITLISTED', 'OFFERED']

function IssueOffer({ application, onClose, onIssued }) {
  const { call } = useApi()
  const [offeredClassDocsId, setClass] = useState(application.appliedClassDocsId ?? '')
  const [expiresAt, setExpiresAt] = useState('')
  const [issuedByDocsId, setIssuedBy] = useState('')
  const [depositInvoiceDocsId, setDeposit] = useState('')
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  //! THE SEAT TABLE OF THE ROUND THIS FORM IS IN, read when the modal opens. #25 does not carry
  //! it — it returns the cycle's id and name, not its capacities — so this is the one read.
  const [seated, setSeated] = useState([])
  const [loadingSeats, setLoadingSeats] = useState(false)

  //! THIS SCHOOL'S STAFF, for the issuer. Optional on the request, so the picker is too.
  const [staff, setStaff] = useState([])

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoadingSeats(true)
      const cycle = await call('get-admission-cycle', {
        label: "The round's seat table",
        pathParams: { admissionCycleId: application.admissionCycleDocsId ?? '' },
      })
      const people = await call('list-staff', {
        label: 'Who can issue it',
        query: { size: '100', sort: 'fullName' },
      })
      if (cancelled) return
      setLoadingSeats(false)
      setSeated(cycle.ok ? (cycle.bodyJson?.capacities ?? []) : [])
      setStaff(people.ok ? (people.bodyJson?.content ?? []) : [])
    }
    load()
    return () => { cancelled = true }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const body = {
    ...(offeredClassDocsId ? { offeredClassDocsId } : {}),
    ...(expiresAt ? { expiresAt: new Date(expiresAt).toISOString() } : {}),
    ...(issuedByDocsId ? { issuedByDocsId } : {}),
    ...(depositInvoiceDocsId ? { depositInvoiceDocsId } : {}),
  }

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('issue-admission-offer', {
      label: `Offer a seat to ${application.applicantName}`,
      pathParams: { admissionApplicationId: application.admissionApplicationId },
      body,
    })
    setSaving(false)
    if (result.ok) { onIssued(); onClose() } else { setRefused(result.bodyJson ?? {}) }
  }

  const live = (application.offers ?? []).filter((one) => one.status === 'ISSUED')
  const offerable = OFFERABLE.includes(application.status)

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={live.length ? 'This form already has its offer' : 'Issue an offer'}
      description="The school offers a seat and the family answers with #30. Approving is the school saying yes; this is what the family gets to say yes to."
      endpoint={<EndpointTag id="issue-admission-offer" name="Issue" look="primary"
        pathParams={{ admissionApplicationId: application.admissionApplicationId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Issue it</Button>
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

        {offerable ? null : (
          <p className="muted">
            <Info size={12} /> <b>This form is {application.status}</b>, so this will answer{' '}
            <span className="mono">409 APPLICATION_NOT_APPROVED</span>.{' '}
            {application.status === 'OFFER_ACCEPTED'
              ? 'The family has already accepted an offer — changing it means withdrawing that one'
                + ' with #31 and issuing another, so both stay in the record.'
              : 'An offer follows a decision rather than making one: #20 is what approves or'
                + ' waitlists a form. Send it to read the refusal.'}
          </p>
        )}

        {(application.offers ?? []).length ? (
          <p className="muted">
            <Info size={12} /> <b>This form already has offer{' '}
            <span className="mono">{application.offers[0].offerNo}</span></b>, which is{' '}
            <span className="mono">{application.offers[0].status}</span>. There is <b>one offer
            letter per admission</b>, so this will be refused — extending or correcting it is an
            edit to that letter rather than a second one, and <b>there is no endpoint for that
            yet</b>. Send it to read the refusal.
          </p>
        ) : null}

        {/* THE READS THAT FILL THE TWO PICKERS BELOW. */}
        <p className="muted">
          <EndpointTag id="get-admission-cycle" name="The round's seat table"
            pathParams={{ admissionCycleId: application.admissionCycleDocsId }} />
          {' '}
          <EndpointTag id="list-staff" name="Who can issue it"
            query={{ size: '100', sort: 'fullName' }} />
        </p>

        <Field
          label="Offered class"
          required
          hint="Only the classes THIS round has seats for — a class that is not in its seat table is 409 CLASS_NOT_IN_CAPACITY. It need NOT be the class applied for: a school assesses a child and offers another grade, which is why the offer carries its own."
        >
          <Select
            value={offeredClassDocsId}
            options={[
              { value: '', label: loadingSeats
                ? 'reading the seat table…'
                : (seated.length
                  ? `${seated.length} seated class${seated.length === 1 ? '' : 'es'} — pick one`
                  : 'this round has no seats set up — #4 sets them') },
              ...seated.map((one) => ({
                value: one.classDocsId,
                label: `${one.className ?? one.classDocsId}${one.classDocsId === application.appliedClassDocsId ? ' — the class applied for' : ''}`,
              })),
            ]}
            label="Offered class"
            onChange={setClass}
          />
        </Field>

        <Field
          label="Class id"
          hint="What is actually sent. A class of another year, or another school's, is 404 CLASS_NOT_FOUND — paste one here to see it. Clear it for 400 VALIDATION_FAILED."
        >
          <Input value={offeredClassDocsId} onChange={(e) => setClass(e.target.value)}
            placeholder="6aa39612224c2e933a1c854a" />
        </Field>

        <Field
          label="Expires"
          hint="Optional. Left empty it defaults to the ROUND'S enrollment deadline — the date the school already published — and an offer with no deadline at all is a seat held for ever. A date already past is 400 OFFER_EXPIRY_IN_THE_PAST, including when it is the cycle's own."
        >
          <Input type="datetime-local" value={expiresAt}
            onChange={(e) => setExpiresAt(e.target.value)} />
        </Field>

        <Field
          label="Issued by"
          hint="Optional, because nothing knows who is calling yet. An id that IS sent has to be this school's staff, and the answer names them."
        >
          <Select
            value={issuedByDocsId}
            options={[
              { value: '', label: 'nobody named' },
              ...staff.map((one) => ({
                value: one.staffDocsId,
                label: `${one.fullName}${one.employeeNo ? ` · ${one.employeeNo}` : ''}`,
              })),
            ]}
            label="Issued by"
            onChange={setIssuedBy}
          />
        </Field>

        <Field
          label="Deposit invoice id"
          hint="Optional, CHECKED, and today it refuses everything: a named invoice has to exist in this school, and nothing writes fee_invoices yet — the finance module has models and no service. Send anything for 404 FEE_INVOICE_NOT_FOUND; leave it empty to issue the offer."
        >
          <Input value={depositInvoiceDocsId} onChange={(e) => setDeposit(e.target.value)}
            placeholder="67aa15d9dc3f7d0099999993" />
        </Field>

        <p className="muted">
          <Info size={12} /> <b>It does not cap offers against the seat table.</b> Schools
          deliberately over-offer — sixty offers for forty places, because a fifth of families go
          elsewhere — so a refusal at the seat count would refuse the normal case. What it refuses
          is a class the round has <b>no</b> seats for at all. Counting offers against places
          is #7.
        </p>
      </div>
    </Modal>
  )
}
