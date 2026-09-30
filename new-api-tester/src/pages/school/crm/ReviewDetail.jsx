import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowLeft, Ban, CheckCircle2, ClipboardCheck, Info, Play, Plus, RefreshCw, ThumbsUp } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from './admissionDates.js'
import { detailPath } from '../../../paths.js'

/**
 * One review: /school-crm/applications/{applicationId}/reviews/{reviewId}
 *
 * A CHILD ADDRESS, not a top-level one, and that is forced by the API rather than chosen. There is
 * no `GET /reviews/{id}` in this module and the plan never had one — #28 is a list and #27 is a
 * write. So the only way to read ONE review is to read the application that owns it, which #25
 * returns with every review in full. The address carries both ids for exactly that reason, and it
 * is what `childPath` exists for: a row inside a row, at its own address.
 *
 * WHICH MEANS A RELOAD STILL WORKS. The page does not depend on being navigated to with state; it
 * reads #25 from the URL like any other screen.
 *
 * RECORDING IS A MODAL ON THIS PAGE, like every other write on this surface. The page shows what
 * the review IS; the modal is where it changes — and the modal is what carries the WHAT WILL BE
 * SENT preview, so the body is visible before it goes.
 *
 * OPENING A PENDING REVIEW STARTS IT. #27b fires on its own the first time this page sees a review
 * that nobody has picked up, because that is what opening it MEANS — a reviewer reading the form
 * has started. It fires ONCE per review, guarded by the id rather than by a boolean, so navigating
 * between two reviews starts each of them and re-rendering starts neither again.
 *
 * AND THE ANSWER IS PATCHED IN, not re-read. #27b returns the whole review, so the row on screen is
 * updated from it directly — no second call, and nothing to refresh by hand.
 *
 * THE BUTTON STAYS, and always sends. The automatic call only fires on PENDING, which would put
 * every one of #27b's refusals out of reach — and "somebody already started it" is the one this
 * endpoint exists to be able to say.
 *
 * FOUR WAYS TO MOVE IT, and they are not four ways to do the same thing. #27 records what was found
 * as the reviewer goes; #27b, #27c and #27d are the three EVENTS — picked it up, finished it, called
 * it off. A verb can ask for what its move needs and nothing else, which is why Finish it insists on
 * a recommendation and Call it off insists on a reason.
 *
 * NOTHING IS DISABLED. Recording on a COMPLETED review is REVIEW_ALREADY_COMPLETED and going back
 * to PENDING is INVALID_REVIEW_TRANSITION; the form says which will refuse and sends anyway.
 */

const STATUS_TONE = { COMPLETED: 'good', IN_PROGRESS: 'warn', CANCELLED: 'bad' }
export default function ReviewDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id, reviewId } = useParams()

  const [application, setApplication] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const [recording, setRecording] = useState(false)
  const [recommending, setRecommending] = useState(false)
  const [completing, setCompleting] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [starting, setStarting] = useState(false)
  const [started, setStarted] = useState(null)

  //! WHICH REVIEW HAS ALREADY BEEN STARTED FROM HERE, by id rather than by a flag. A flag would
  //! stop the second review being started after navigating to it, and would also let a re-render
  //! start the first one twice.
  const autoStarted = useRef(null)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-admission-application', {
      label: 'The form this review is of',
      pathParams: { admissionApplicationId: id ?? '' },
    })
    setLoading(false)
    if (result.ok) { setApplication(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  const review = (application?.reviews ?? [])
    .find((one) => one.admissionReviewId === reviewId)

  //! PATCHES THE ANSWER IN rather than re-reading. #27b returns the whole review, so the page can
  //! show the new status, the new version and the new nextStep without a second call — which is
  //! also what makes the automatic start invisible rather than a flash of stale data.
  const applyStarted = (fresh) => setApplication((old) => (old === null ? old : {
    ...old,
    reviews: (old.reviews ?? []).map((one) =>
      (one.admissionReviewId === fresh.admissionReviewId ? { ...one, ...fresh } : one)),
  }))

  const startIt = async (automatic) => {
    setStarting(true)
    const result = await call('start-admission-review', {
      label: automatic ? 'Opened it, so it is started' : 'Start it',
      pathParams: { admissionReviewId: reviewId ?? '' },
    })
    setStarting(false)
    setStarted(result)
    if (result.ok && result.bodyJson) applyStarted(result.bodyJson)
  }

  //! FIRES ONCE, ON A PENDING REVIEW, the first time this page sees it. Not on IN_PROGRESS —
  //! that is a documented 409 and firing it on every visit would answer it every time.
  useEffect(() => {
    if (!review || review.status !== 'PENDING') return
    if (autoStarted.current === review.admissionReviewId) return
    autoStarted.current = review.admissionReviewId
    startIt(true)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [review?.admissionReviewId, review?.status])

  if (!actingSubdomain) return <NoSchoolChosen what="A review" />

  const back = () => navigate(detailPath('school', 'crm', 'applications', id))

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">
            {review ? `Round ${review.reviewRound}` : 'Review'}
            {review?.reviewerName ? ` — ${review.reviewerName}` : ''}
          </h1>
          <p className="muted">
            <span className="mono">{reviewId}</span>
            {application ? ` · on ${application.applicantName}` : ' · reading the form…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={back}>Back to the form</Button>
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button icon={Play} onClick={() => startIt(false)} busy={starting}>Start it</Button>
      </div>

      {problem ? (
        <Card title="Could not read the form" action={<EndpointTag id="get-admission-application" name="Get" />}>
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
        </Card>
      ) : null}

      {started ? (
        <Card
          title={started.ok ? 'Started' : 'Not started'}
          action={<EndpointTag id="start-admission-review" name="Start" />}
        >
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok={String(Boolean(started.ok))}>
                {started.bodyJson?.code ?? started.status}
              </span>
            </div>
            <pre className="resp-body">
              {started.ok
                ? (started.bodyJson?.nextStep ?? 'It is IN_PROGRESS.')
                : (started.bodyJson?.message ?? started.bodyText)}
            </pre>
          </div>
          <p className="muted">
            <Info size={12} /> {started.ok
              ? 'Opening a PENDING review starts it — that is what opening it means. The review'
                + ' below is updated straight from this answer rather than re-read.'
              : 'A refusal changes nothing. This one fires by hand; the automatic start only runs'
                + ' on a PENDING review, which would put every refusal here out of reach.'}
          </p>
        </Card>
      ) : null}

      {application && !review ? (
        <Card title="That review is not on this form">
          <p className="muted">
            <Info size={12} /> <b>This page reads the APPLICATION, not the review.</b> There is no{' '}
            <span className="mono">GET /reviews/&#123;id&#125;</span> in this module — #28 is a list
            and #27 is a write — so one review is read by reading the form that owns it, which #25
            returns in full. A review id that is not among this form&rsquo;s {application.reviewCount}{' '}
            is simply not here.
          </p>
        </Card>
      ) : null}

      {review ? (
        <>
          <Card
            title="The review"
            description="Everything on it. A row of the queue leaves off the criteria and the notes; this is where they are."
            action={<EndpointTag id="get-admission-application" name="From #25" />}
          >
            <div className="stack">
              <div className="field-grid">
                <div>
                  <p className="muted">Status</p>
                  <Badge tone={STATUS_TONE[review.status]}>{review.status}</Badge>
                </div>
                <div>
                  <p className="muted">Round</p>
                  <p className="mono">{review.reviewRound}</p>
                </div>
                <div>
                  <p className="muted">Reviewer</p>
                  <p>{review.reviewerName ?? <span className="muted">not staff any more</span>}</p>
                  <p className="mono muted">{review.reviewerDocsId}</p>
                </div>
                <div>
                  <p className="muted">Acting as</p>
                  <p>{review.reviewerRole}</p>
                </div>
                <div>
                  <p className="muted">Due</p>
                  {review.dueAt
                    ? <p title={review.dueAt}>{readable(review.dueAt)}</p>
                    : <p className="muted">No due date — which also means it is never overdue.</p>}
                </div>
                <div>
                  <p className="muted">Completed</p>
                  {review.completedAt
                    ? <p title={review.completedAt}>{readable(review.completedAt)}</p>
                    : <p className="muted">Not yet. Stamped only on the way into COMPLETED.</p>}
                </div>
                <div>
                  <p className="muted">Score</p>
                  <p className="mono">{review.score ?? <span className="muted">none yet</span>}</p>
                </div>
                <div>
                  <p className="muted">Recommends</p>
                  <p>{review.recommendation ?? <span className="muted">nothing yet</span>}</p>
                </div>
              </div>
            </div>
          </Card>

          <Card
            title="Record what was found"
            description="The marks, and only the marks — a score and the criteria behind it. Only what you send moves, and a body carrying neither is 400 NOTHING_TO_UPDATE."
            action={
              <Button look="primary" icon={ClipboardCheck} onClick={() => setRecording(true)}>
                Add review
              </Button>
            }
          >
            {/* WHAT IS ALREADY RECORDED, BESIDE THE BUTTON THAT CHANGES IT. These two are exactly
                what this endpoint writes — with the score in the summary above — so reading them
                anywhere else meant scrolling away from the thing you were about to edit. */}
            <div className="stack">
              <div>
                <p className="muted">Criterion scores</p>
                {review.criterionScores
                  ? (
                    <div className="table-scroll">
                      <table className="data-table">
                        {/* NOT RIGHT-ALIGNED, and no longer called a score: the values became
                            strings on 2026-09-30, so "Pass" and "B+" sit here as readily as
                            42.50 and a numeric column would misalign every one of them. */}
                        <thead><tr><th>Part</th><th>Result</th></tr></thead>
                        <tbody>
                          {Object.entries(review.criterionScores).map(([part, mark]) => (
                            <tr key={part}>
                              <td className="mono">{part}</td>
                              <td>{mark}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )
                  : <p className="muted">None. Left off the response entirely rather than sent as
                    an empty map.</p>}
              </div>


              <p className="muted">
                <Info size={12} /> Opens with a <b>WHAT WILL BE SENT</b> panel, so the body is
                visible before it goes — the same as every other write on this surface.
                {review.status === 'COMPLETED' || review.status === 'CANCELLED'
                  ? ' This review is finished, so everything in it will be refused; that refusal is'
                    + ' worth reading once.'
                  : ''}
              </p>
            </div>
          </Card>

 


          <Card
            title="What the reviewer concludes"
            description="The verdict, and the reasoning behind it. It does NOT finish the review and does not stamp completedAt — saying what you think is not saying you are done."
            action={
              <Button look="primary" icon={ThumbsUp} onClick={() => setRecommending(true)}>
                Recommend
              </Button>
            }
          >
            <div className="stack">
              <div>
                <p className="muted">Notes</p>
                {review.notes ? <p>{review.notes}</p> : <p className="muted">None.</p>}
              </div>

              <p className="muted">
                <Info size={12} /> <b>Both of these were fields on Add review</b> — the verdict
                moved to its own call on 2026-09-30, and the notes followed it. A score is a
                measurement; what the reviewer makes of it, and why, is the thing a review exists
                to produce. Read the answer back: the{' '}
                <span className="mono">status</span> is exactly what it was. Recording a second
                verdict replaces the first, until the review ends.
                {review.recommendation
                  ? ` It currently recommends ${review.recommendation}.`
                  : ' It recommends nothing yet.'}
              </p>
            </div>
          </Card>

              {/* MOUNTED ONLY WHILE OPEN, unlike the modals on the sibling screens. The version box
              is seeded from the review, and a component that stays mounted would keep the value
              it was first given — so a send elsewhere, or a Refresh behind this, would leave the
              box one behind and the next write stale for no reason. Mounting on demand seeds it
              on every open and needs no effect to do it. */}
          <Card
            title="Finish it"
            description="COMPLETED, once Recommend has said what the review concludes. This is the end of the review — and it stamps completedAt, which nothing else in the module does."
            action={
              <Button look="primary" icon={CheckCircle2} onClick={() => setCompleting(true)}>
                Complete review
              </Button>
            }
          >
            <p className="muted">
              <Info size={12} /> <b>Finishing is an event rather than a field being set</b>, which
              is why it is a call of its own and can insist on what the move needs. Its body is the
              version and nothing else — the findings belong to Add review and the verdict to
              Recommend.
              {review.recommendation === 'REQUEST_MORE_INFORMATION'
                ? ' It recommends REQUEST_MORE_INFORMATION, which is a reviewer asking rather than'
                  + ' a verdict — 409 RECOMMENDATION_NOT_FINAL until one of the other three'
                  + ' replaces it.'
                : review.recommendation
                  ? ` It recommends ${review.recommendation}, so an empty body completes it.`
                  : ' It recommends nothing yet, so this answers 400 RECOMMENDATION_REQUIRED until'
                    + ' Recommend has run.'}
            </p>
          </Card>

          <Card
            title="Call it off"
            description="CANCELLED, with a reason. This is what a DELETE would have been — the review stays, and says it was called off and why."
            action={
              <Button icon={Ban} onClick={() => setCancelling(true)}>Cancel review</Button>
            }
          >
            <p className="muted">
              <Info size={12} /> The reviewer left, the round was assigned by mistake, the family
              withdrew. <b>A PENDING row nobody is ever going to work is worse than a cancelled
              one</b> — it sits in #28&rsquo;s queue for ever and makes the backlog a lie. It does
              not stamp <span className="mono">completedAt</span>, and whatever was recorded so far
              stays untouched.
            </p>
          </Card>

          {recording ? (
            <RecordResult
              review={review}
              onClose={() => setRecording(false)}
              onRecorded={load}
            />
          ) : null}

          {/* MOUNTED ON DEMAND, for the same reason as the one above: the version box is seeded
              from the review, and a component that stayed mounted would keep the value it was
              first given. */}
          {completing ? (
            <CompleteReview
              review={review}
              onClose={() => setCompleting(false)}
              onDone={load}
            />
          ) : null}

          {recommending ? (
            <RecommendOnReview
              review={review}
              onClose={() => setRecommending(false)}
              onDone={load}
            />
          ) : null}

          {cancelling ? (
            <CancelReview
              review={review}
              onClose={() => setCancelling(false)}
              onDone={load}
            />
          ) : null}
        </>
      ) : null}
    </div>
  )
}

/**
 * #27 — what the reviewer found.
 *
 * EVERY FIELD IS OPTIONAL and the modal makes none of them required. Completing without a
 * recommendation is RECOMMENDATION_REQUIRED, cancelling without a note is
 * CANCELLATION_NOTE_REQUIRED, and an empty body is NOTHING_TO_UPDATE — three documented refusals
 * somebody testing this needs to be able to send. The hints say which will refuse; they do not
 * refuse for the server.
 *
 * EVERY STATUS IS OFFERED, including the ones the graph will not take from where the review is.
 * Both ends are terminal, so a COMPLETED review refuses all of them — and that refusal, with its
 * "cancel and assign another" message, is the most interesting answer this endpoint gives.
 *
 * THE CRITERIA BOX IS JSON, because the keys are whatever the school calls its parts and nothing
 * on the server validates them. A built form would have to invent a criterion list the API does
 * not have.
 */
const REVIEW_STATUSES = ['', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'PENDING']
const RECOMMENDATIONS = ['', 'APPROVE', 'REJECT', 'WAITLIST', 'REQUEST_MORE_INFORMATION']
const BLANK_PAIR = { name: '', score: '' }

/**
 * The two parts the model's own example names:
 * `{ "INTERVIEW": 42.50, "ENTRANCE_TEST": 44.00 }`.
 *
 * PRE-FILLED AS NAMES ONLY, with no scores. They are a school's commonest two and typing them out
 * every time is friction — but nothing validates the keys, so they are a starting point rather
 * than a list. Rename them, remove them, add others with +.
 *
 * A ROW WITH NO SCORE IS SKIPPED, which is why a blank score is safe to ship as the default: the
 * body stays `{}` until somebody actually marks something.
 */
const DEFAULT_PAIRS = [{ name: 'INTERVIEW', score: '' }, { name: 'ENTRANCE_TEST', score: '' }]

/**
 * The pairs, as the body will carry them.
 *
 * BOTH HALVES OR NEITHER. A part with a name and nothing against it is not a result — and it
 * matters more than it looks, because the two default rows arrive named and empty. Counting a name
 * alone would make the opening state send `{"INTERVIEW": ""}` before anybody typed.
 *
 * EVERY VALUE IS SENT AS A STRING, since 2026-09-30. The server holds these as strings now, because
 * a criterion result is not always a figure — A/B/C, Pass, "42/50". Coercing "42.5" to a number
 * here would send 42.5 where the school typed "42.50" and quietly lose the trailing zero they
 * wrote.
 */
function criteriaFrom(pairs) {
  const named = pairs.filter((one) => one.name.trim())
  const filled = named.filter((one) => one.score.trim())

  const assembled = Object.fromEntries(
    filled.map((one) => [one.name.trim(), one.score.trim()]))

  return { filled, unscored: named.length - filled.length, assembled }
}

/**
 * The criterion editor, shared by #27 and #27c.
 *
 * ONE EDITOR, BECAUSE IT IS ONE FIELD. `criterionScores` behaves identically on both endpoints —
 * absent leaves the map alone, `{}` clears it, anything else replaces it whole — and two copies of
 * a three-state control is two places for that sentence to drift.
 */
function CriterionFields({ mode, setMode, pairs, setPairs }) {
  const { filled, unscored } = criteriaFrom(pairs)

  const setPair = (index, field, value) => setPairs((old) =>
    old.map((row, n) => (n === index ? { ...row, [field]: value } : row)))

  return (
    <>
      <Field
        label="Criterion scores"
        hint="Three things the API can be told, and they are not the same: leave them alone, clear them, or replace them. SENDING REPLACES THE WHOLE MAP — merging would leave no way to remove a criterion recorded by mistake."
      >
        <Select
          value={mode}
          options={[
            { value: '', label: 'leave them alone — the field is not sent' },
            { value: 'replace', label: 'replace them with the pairs below' },
            { value: 'clear', label: 'clear them all — sends {}' },
          ]}
          label="Criterion scores"
          onChange={setMode}
        />
      </Field>

      {mode === 'replace' ? (
        <div className="stack">
          {pairs.map((one, index) => (
            <div className="field-grid" key={index}>
              <Field label={`Part ${index + 1}`} hint="What the school calls it. Nothing validates the keys — there is no criterion-definition model, so a school names its own parts.">
                <Input value={one.name} placeholder="INTERVIEW"
                  onChange={(e) => setPair(index, 'name', e.target.value)} />
              </Field>
              {/* A TEXT BOX, NOT type="number", since 2026-09-30. The server holds these as
                  strings, so "B+" and "Pass" are as valid as 42.50 — a number field would make
                  two thirds of what a school can write unreachable. */}
              <Field label="Result" hint="Anything up to 40 characters — 42.50, B+, Pass, 42/50. The server holds it as typed and checks nothing beyond the length. A part with nothing against it is skipped rather than sent empty.">
                <Input value={one.score} placeholder="42.50"
                  onChange={(e) => setPair(index, 'score', e.target.value)} />
              </Field>
            </div>
          ))}
          <div className="toolbar">
            <Button icon={Plus} onClick={() => setPairs((old) => [...old, { ...BLANK_PAIR }])}>
              Add a part
            </Button>
            <Button onClick={() => setPairs((old) => old.slice(0, -1))}>
              Remove the last
            </Button>
            <span className="toolbar-spacer" />
            <Badge tone={filled.length === 0 ? 'warn' : undefined}>
              {filled.length} part{filled.length === 1 ? '' : 's'}
            </Badge>
          </div>
          {unscored > 0 ? (
            <p className="muted">
              <Info size={12} /> <b>{unscored} part{unscored === 1 ? ' has' : 's have'} no
              result</b>, so {unscored === 1 ? 'it is' : 'they are'} left out — a part with a name
              and nothing against it is not a result. Give {unscored === 1 ? 'it' : 'them'} one,
              or remove the row.
            </p>
          ) : null}
          {filled.length === 0 ? (
            <p className="muted">
              <Info size={12} /> Nothing has a result yet, so this sends{' '}
              <span className="mono">{'{}'}</span> — the same as clearing them.
            </p>
          ) : null}
          {filled.length !== new Set(filled.map((one) => one.name.trim())).size ? (
            <p className="muted">
              <Info size={12} /> <b>Two parts share a name.</b> A map has one value per key, so
              the last one wins and the other is lost before the request is even sent.
            </p>
          ) : null}
        </div>
      ) : null}
    </>
  )
}

function RecordResult({ review, onClose, onRecorded }) {
  const { call } = useApi()
  const [score, setScore] = useState('')
  //! THE CRITERIA ARE TYPED AS PAIRS, not as JSON. A reviewer fills in what the part was called
  //! and what it scored; the body is assembled from that. Typing `{"INTERVIEW": 42.5}` by hand is
  //! a brace away from a 400 that says nothing about admissions.
  const [criteriaMode, setCriteriaMode] = useState('')
  const [pairs, setPairs] = useState(DEFAULT_PAIRS.map((one) => ({ ...one })))
  //! SEEDED FROM THE REVIEW, because a version nobody can read is a parameter nobody can use.
  //! #25 carries it on every review as of 2026-09-23; before that the only way to find it was to
  //! read the document out of Mongo.
  const [version, setVersion] = useState(String(review.version ?? ''))
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  //! THE THREE STATES THE API HAS, said out loud rather than inferred from an empty box:
  //! absent leaves the map alone, `{}` clears it, and anything else replaces it whole. An
  //! editor that only had "rows" could not express the middle one.
  const { assembled } = criteriaFrom(pairs)
  const parsedCriteria = criteriaMode === 'clear' ? {}
    : (criteriaMode === 'replace' ? assembled : undefined)

  //! THREE FIELDS AND A VERSION, since 2026-09-30. The status and the recommendation left this
  //! endpoint — ending a review is a verb, and the verdict has #27e — so a box for either here
  //! would send a key the API ignores and teach the wrong shape.
  const body = {
    ...(score === '' ? {} : { score: Number(score) }),
    ...(parsedCriteria === undefined ? {} : { criterionScores: parsedCriteria }),
    ...(version === '' ? {} : { version: Number(version) }),
  }

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('record-admission-review', {
      label: `Record on round ${review.reviewRound}`,
      pathParams: { admissionReviewId: review.admissionReviewId },
      body,
    })
    setSaving(false)
    //! A REFUSAL CHANGES NOTHING, so the modal stays open with the message and the page behind it
    //! is left alone. A success closes and reloads, which is how the page proves what happened.
    if (result.ok) { onRecorded(); onClose() } else { setRefused(result.bodyJson ?? {}) }
  }

  const finished = review.status === 'COMPLETED' || review.status === 'CANCELLED'

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={`Record on round ${review.reviewRound}`}
      description="Only what you send moves, so a score saved now survives a recommendation added later. A body carrying nothing is 400 NOTHING_TO_UPDATE."
      endpoint={<EndpointTag id="record-admission-review" name="Record" look="primary" />}
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

        {finished ? (
          <p className="muted">
            <Info size={12} /> <b>This review is {review.status}</b>, so everything here will be
            refused — <span className="mono">
              {review.status === 'COMPLETED' ? 'REVIEW_ALREADY_COMPLETED' : 'REVIEW_CANCELLED'}
            </span>. Both ends are terminal on purpose: a score recorded wrongly is corrected by
            cancelling this review and assigning another, which leaves both in the history rather
            than overwriting one. Send something to read the refusal.
          </p>
        ) : null}



        <CriterionFields
          mode={criteriaMode} setMode={setCriteriaMode} pairs={pairs} setPairs={setPairs} />

        <Field
          label="Score"
          hint="No upper bound — out of 100, out of 50, out of 5 is the school's business, not the API's. Negative is refused, because that is a typo rather than a scale."
        >
          <Input type="number" step="0.01" value={score}
            onChange={(e) => setScore(e.target.value)} placeholder="86.50" />
        </Field>
        {/* NO NOTES BOX. They moved to Recommend on 2026-09-30 — what a reviewer writes is their
            reasoning, and reasoning belongs beside the verdict it justifies. */}
        <p className="muted">
          <Info size={12} /> <b>The notes are on Recommend now.</b> This call carries the marks;
          what the reviewer makes of them, and why, goes with the verdict.
        </p>

        <Field
          label="Version"
          hint={`Filled in from what this page last read${review.version === undefined ? '' : ` — version ${review.version}`}. Change it for 409 CONCURRENT_MODIFICATION, or clear it and last write wins. On its own it is still 400 NOTHING_TO_UPDATE — the request's own shape is checked before the state of the world.`}
        >
          <Input type="number" value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}

/**
 * #27c — the reviewer is finished.
 *
 * THE RECOMMENDATION IS NOT REQUIRED BY THIS FORM, and that is on purpose twice over. It is not
 * enforced here because 400 RECOMMENDATION_REQUIRED is a refusal somebody testing this needs to be
 * able to send — and it is not always needed, because the rule is that the REVIEW has one by the
 * time it is done, not that this body carries one. A recommendation saved earlier with #27 completes
 * it with an empty body.
 *
 * NO STATUS BOX. The status is the endpoint. A body naming one would be a second way to say the
 * same thing, and a way to disagree with the path.
 *
 * THE SAME CRITERION EDITOR #27 USES, because it is the same field with the same three states.
 */
function CompleteReview({ review, onClose, onDone }) {
  const { call } = useApi()
  const [version, setVersion] = useState(String(review.version ?? ''))
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  //! THE VERSION, AND THAT IS THE WHOLE BODY, since 2026-09-30. The score, the criteria, the notes
  //! and finally the verdict all left this endpoint — a box for any of them would send a key the
  //! API ignores and teach a shape that no longer exists.
  const body = {
    ...(version === '' ? {} : { version: Number(version) }),
  }

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('complete-admission-review', {
      label: `Finish round ${review.reviewRound}`,
      pathParams: { admissionReviewId: review.admissionReviewId },
      body,
    })
    setSaving(false)
    if (result.ok) { onDone(); onClose() } else { setRefused(result.bodyJson ?? {}) }
  }

  const finished = review.status === 'COMPLETED' || review.status === 'CANCELLED'

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={`Finish round ${review.reviewRound}`}
      description="COMPLETED. Both ends are terminal — this is where the review stops. It needs a verdict already recorded, and carries nothing but the version."
      endpoint={<EndpointTag id="complete-admission-review" name="Complete" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Finish it</Button>
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
            <Info size={12} /> <b>This review is {review.status}</b>, so this will be refused —{' '}
            <span className="mono">
              {review.status === 'COMPLETED' ? 'REVIEW_ALREADY_COMPLETED' : 'REVIEW_CANCELLED'}
            </span>. A result recorded wrongly is corrected by cancelling this review and assigning
            another, which leaves both in the history rather than overwriting one. Send it to read
            the refusal.
          </p>
        ) : null}

        <p className="muted">
          <Info size={12} /> <b>There is no status box.</b> The status is the endpoint — a body
          naming one would be a second way to say the same thing, and a way to disagree with the
          path. It is {review.status} now, and {review.status === 'PENDING'
            ? 'PENDING completes straight to COMPLETED: most reviews are done in one sitting, and'
              + ' forcing a start first would be ceremony nobody would keep up.'
            : 'this takes it to COMPLETED.'}
        </p>

        {/* NO VERDICT BOX. The recommendation left this call on 2026-09-30 and the rule is now
            read off the REVIEW — a Select here would send a key the API ignores and suggest you
            could finish a review by naming one, which is the thing that changed. */}
        <p className="muted">
          <Info size={12} /> <b>The body is the version, and that is all.</b> The score, the
          criterion results, the notes and finally the verdict all left this call — Add review and
          Recommend are where they are written, and whatever is on the review survives this
          untouched.
        </p>

        {review.recommendation === 'REQUEST_MORE_INFORMATION'
          ? (
            <p className="muted">
              <Info size={12} /> <b>It recommends REQUEST_MORE_INFORMATION</b>, so this will
              answer <span className="mono">409 RECOMMENDATION_NOT_FINAL</span>. That is a reviewer
              asking for something rather than a verdict — only APPROVE, REJECT and WAITLIST can be
              a review&rsquo;s last word. When the answer arrives, record one of those with{' '}
              <b>Recommend</b> and finish it then.
            </p>
          )
          : review.recommendation
          ? (
            <p className="muted">
              <Info size={12} /> It recommends <b>{review.recommendation}</b>, so this will finish
              it.
            </p>
          )
          : (
            <p className="muted">
              <Info size={12} /> <b>It recommends nothing yet</b>, so this will answer{' '}
              <span className="mono">400 RECOMMENDATION_REQUIRED</span> — worth seeing once. Record
              a verdict with <b>Recommend</b> first; sending one in this body does not get round
              it, because the rule reads the review rather than the request.
            </p>
          )}

        <Field
          label="Version"
          hint={`Filled in from what this page last read${review.version === undefined ? '' : ` — version ${review.version}`}. Change it for 409 CONCURRENT_MODIFICATION, or clear it and last write wins. Unlike #27 there is no NOTHING_TO_UPDATE here: an empty body still asks for the move the path names.`}
        >
          <Input type="number" value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}

/**
 * #27e — what the reviewer concludes.
 *
 * ITS OWN CALL SINCE 2026-09-30. It was a Select on Add review, in among the score and the notes;
 * a score is a measurement and a note is a remark, and this is the one thing a review exists to
 * produce.
 *
 * THE BOX STARTS EMPTY EVEN WHEN THE REVIEW ALREADY RECOMMENDS SOMETHING, unlike the score on
 * Complete review. Seeding it would make "send it again unchanged" the default gesture, and this
 * endpoint's whole shape is that recording a second verdict REPLACES the first — the one moment
 * worth making somebody type the answer is the one where they are changing their mind.
 *
 * AND THE EMPTY OPTION IS OFFERED, which sends `{}` and reaches 400 VALIDATION_FAILED. The field is
 * required by the API and a form that made that unreachable would hide a documented refusal.
 */
function RecommendOnReview({ review, onClose, onDone }) {
  const { call } = useApi()
  const [recommendation, setRecommendation] = useState('')
  //! NOT SEEDED FROM THE REVIEW, for the same reason the verdict is not: leaving it empty keeps
  //! what is written, so pre-filling would make "send it back unchanged" the default gesture.
  const [notes, setNotes] = useState('')
  const [version, setVersion] = useState(String(review.version ?? ''))
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  const body = {
    ...(recommendation ? { recommendation } : {}),
    ...(notes === '' ? {} : { notes }),
    ...(version === '' ? {} : { version: Number(version) }),
  }

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('recommend-admission-review', {
      label: `Recommend on round ${review.reviewRound}`,
      pathParams: { admissionReviewId: review.admissionReviewId },
      body,
    })
    setSaving(false)
    if (result.ok) { onDone(); onClose() } else { setRefused(result.bodyJson ?? {}) }
  }

  const finished = review.status === 'COMPLETED' || review.status === 'CANCELLED'

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={`Recommend on round ${review.reviewRound}`}
      description="The verdict, and nothing else. It does NOT finish the review and does not stamp completedAt."
      endpoint={<EndpointTag id="recommend-admission-review" name="Recommend" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Recommend</Button>
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
            <Info size={12} /> <b>This review is {review.status}</b>, so this will be refused —{' '}
            <span className="mono">
              {review.status === 'COMPLETED' ? 'REVIEW_ALREADY_COMPLETED' : 'REVIEW_CANCELLED'}
            </span>. A finished review is a record, not a draft. Send it to read the refusal.
          </p>
        ) : null}

        <Field
          label="Recommends"
          hint={review.recommendation
            ? `It already recommends ${review.recommendation}. Sending another REPLACES it — until the review ends this is a working answer, not a record. Leave it empty to see 400 VALIDATION_FAILED.`
            : 'What THIS person suggests. Not a decision: #20 is what the school does, and it may decide something no reviewer recommended. Leave it empty to see 400 VALIDATION_FAILED.'}
        >
          <Select
            value={recommendation}
            options={RECOMMENDATIONS.map((one) => ({
              value: one, label: one === '' ? 'send nothing — 400 VALIDATION_FAILED' : one,
            }))}
            label="Recommendation"
            onChange={setRecommendation}
          />
        </Field>

        {/* THE REASONING, BESIDE THE VERDICT IT JUSTIFIES. It moved here from Add review on
            2026-09-30. Left empty the field is not sent at all, which KEEPS what is written —
            typing a single space is how you reach the "" that clears it. */}
        <Field
          label="Notes"
          hint={review.notes
            ? `It currently says "${review.notes}". Leave this empty and that stands — the field is not sent at all. Type "" worth of spaces to clear it, which is the project's convention for a String.`
            : 'Why. Optional, up to 2000 characters. Left empty the field is not sent, so nothing is overwritten. Cancel review writes this same field as the reason a review was called off — one notes, two things worth saying in it.'}
        >
          <Input value={notes} onChange={(e) => setNotes(e.target.value)}
            placeholder="Strong in the interaction, comfortably above the mark." />
        </Field>

        <Field
          label="Version"
          hint="OPTIONAL — leave it empty and the last write wins. Type an OLDER number to make 409 CONCURRENT_MODIFICATION happen on purpose; what you would be overwriting is somebody else's verdict."
        >
          <Input value={version} onChange={(e) => setVersion(e.target.value)}
            placeholder={String(review.version ?? 0)} />
        </Field>

        <p className="muted">
          <Info size={12} /> <b>It does not finish the review.</b> Read the answer back — the{' '}
          <span className="mono">status</span> is exactly what it was, and{' '}
          <span className="mono">completedAt</span> is still empty. Complete review is what ends it,
          and it takes a verdict of its own for the reviewer who decides and finishes at once.
        </p>
      </div>
    </Modal>
  )
}

/**
 * #27d — the school called it off.
 *
 * THE REASON IS THE WHOLE BODY, and it is still not enforced here. 400 CANCELLATION_NOTE_REQUIRED is
 * the refusal this endpoint is most worth sending, and a form that would not let it be sent would
 * put it out of reach.
 *
 * IT IS PRE-FILLED from the review's notes, changed 2026-09-30. It started empty, and the argument
 * for that still holds: this box and those notes are ONE FIELD with two meanings — the reviewer's
 * reasoning for their verdict, and why the school stopped — so sending it back unchanged relabels
 * the first as the second.
 *
 * WHAT WON THE ARGUMENT: the API accepts an empty body here precisely BECAUSE the review's notes
 * stand as the reason, and a box that hid them made that rule impossible to see. Showing the text
 * the rule is about is worth the risk of somebody pressing send without reading it.
 */
function CancelReview({ review, onClose, onDone }) {
  const { call } = useApi()
  //! PRE-FILLED FROM THE REVIEW'S NOTES — changed 2026-09-30, on request. It started empty for a
  //! reason worth keeping in view: this box and those notes are ONE FIELD with two meanings, so
  //! sending it back unchanged relabels the reviewer's reasoning as the reason the school gave
  //! up. Showing it is the argument for: the API accepts an empty body precisely BECAUSE those
  //! notes stand as the reason, and a box that hid them made that rule impossible to see.
  //!
  //! BOTH REFUSAL PATHS STAY REACHABLE. Clearing the box sends no `notes` at all — the body omits
  //! an empty string — so a review WITH notes still reaches the empty-body path, and one WITHOUT
  //! starts empty and still reaches 400 CANCELLATION_NOTE_REQUIRED.
  const [notes, setNotes] = useState(review.notes ?? '')
  const [version, setVersion] = useState(String(review.version ?? ''))
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  const body = {
    ...(notes ? { notes } : {}),
    ...(version === '' ? {} : { version: Number(version) }),
  }

  const submit = async () => {
    setSaving(true); setRefused(null)
    const result = await call('cancel-admission-review', {
      label: `Call off round ${review.reviewRound}`,
      pathParams: { admissionReviewId: review.admissionReviewId },
      body,
    })
    setSaving(false)
    if (result.ok) { onDone(); onClose() } else { setRefused(result.bodyJson ?? {}) }
  }

  const finished = review.status === 'COMPLETED' || review.status === 'CANCELLED'

  return (
    <Modal
      open
      onClose={onClose}
      preview={body}
      previewLabel="WHAT WILL BE SENT"
      title={`Call off round ${review.reviewRound}`}
      description="CANCELLED, with a reason. The review stays and says it was called off — there is no DELETE on this collection."
      endpoint={<EndpointTag id="cancel-admission-review" name="Cancel" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Call it off</Button>
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
            <Info size={12} /> <b>This review is {review.status}</b>, so this will be refused —{' '}
            <span className="mono">
              {review.status === 'COMPLETED' ? 'REVIEW_ALREADY_COMPLETED' : 'REVIEW_CANCELLED'}
            </span>.{' '}
            {review.status === 'COMPLETED'
              ? 'What somebody found is a record: the school undoes it by deciding differently in'
                + ' #20, not by erasing the review.'
              : 'Assign another with #26 if somebody still needs to look.'}
          </p>
        ) : null}

        <p className="muted">
          <Info size={12} /> <b>Nothing is lost.</b> {review.score === undefined
            && review.recommendation === undefined && !review.criterionScores
            ? 'Nothing has been recorded on this review yet.'
            : 'Whatever has been recorded so far stays exactly where it is — that is the history'
              + ' the cancellation is being written into.'}{' '}
          <span className="mono">completedAt</span> is <b>not</b> stamped: a cancelled review was
          not completed, however much of it was filled in.
        </p>

        <Field
          label="Why"
          hint={review.notes
            ? 'PRE-FILLED with what the review already says — which is the REVIEWER\'S reasoning for their verdict, not a cancellation reason. Sending it back unchanged relabels it as why the school gave up; type over it to say something else. CLEAR THE BOX to send an empty body, which is accepted because those notes stand as the reason.'
            : 'REQUIRED — the review has no notes to fall back on. Leave it empty for 400 CANCELLATION_NOTE_REQUIRED, which is not enforced here. Work called off with no reason is a gap in the record, the same reading that makes lostReason required on a lost inquiry.'}
        >
          <Input value={notes} onChange={(e) => setNotes(e.target.value)}
            placeholder="The reviewer has left the school, so this round is being reassigned." />
        </Field>

        <Field
          label="Version"
          hint={`Filled in from what this page last read${review.version === undefined ? '' : ` — version ${review.version}`}. Worth sending on this one: somebody may have just finished the work you are calling off, and a stale version is 409 CONCURRENT_MODIFICATION rather than a cancellation that lands on top of it.`}
        >
          <Input type="number" value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}
