import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowLeft, Ban, CheckCircle2, ClipboardCheck, Info, Play, Plus, RefreshCw } from 'lucide-react'
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
        <Button icon={ClipboardCheck} onClick={() => setRecording(true)}>
          Record what was found
        </Button>
        <Button icon={Ban} onClick={() => setCancelling(true)}>Call it off</Button>
        <Button look="primary" icon={CheckCircle2} onClick={() => setCompleting(true)}>
          Finish it
        </Button>
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

              <div>
                <p className="muted">Criterion scores</p>
                {review.criterionScores
                  ? (
                    <div className="table-scroll">
                      <table className="data-table">
                        <thead><tr><th>Part</th><th className="num">Score</th></tr></thead>
                        <tbody>
                          {Object.entries(review.criterionScores).map(([part, mark]) => (
                            <tr key={part}>
                              <td className="mono">{part}</td>
                              <td className="num">{mark}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )
                  : <p className="muted">None. Left off the response entirely rather than sent as
                    an empty map.</p>}
              </div>

              <div>
                <p className="muted">Notes</p>
                {review.notes ? <p>{review.notes}</p> : <p className="muted">None.</p>}
              </div>
            </div>
          </Card>

          <Card
            title="Record what was found"
            description="Only what you send moves, so a score saved now survives a recommendation added later. A body carrying nothing is 400 NOTHING_TO_UPDATE."
            action={
              <Button look="primary" icon={ClipboardCheck} onClick={() => setRecording(true)}>
                Record
              </Button>
            }
          >
            <p className="muted">
              <Info size={12} /> Opens with a <b>WHAT WILL BE SENT</b> panel, so the body is
              visible before it goes — the same as every other write on this surface.
              {review.status === 'COMPLETED' || review.status === 'CANCELLED'
                ? ' This review is finished, so everything in it will be refused; that refusal is'
                  + ' worth reading once.'
                : ''}
            </p>
          </Card>

          {/* MOUNTED ONLY WHILE OPEN, unlike the modals on the sibling screens. The version box
              is seeded from the review, and a component that stays mounted would keep the value
              it was first given — so a send elsewhere, or a Refresh behind this, would leave the
              box one behind and the next write stale for no reason. Mounting on demand seeds it
              on every open and needs no effect to do it. */}
          <Card
            title="Finish it"
            description="COMPLETED, with the recommendation the review exists to produce. This is the end of the review — and it stamps completedAt, which nothing else in the module does."
            action={
              <Button look="primary" icon={CheckCircle2} onClick={() => setCompleting(true)}>
                Finish it
              </Button>
            }
          >
            <p className="muted">
              <Info size={12} /> #27 can make the same move inside a general edit; this exists
              because <b>finishing is an event rather than a field being set</b>, and it can insist
              on what the move needs. It takes the score and the criteria too, so one call finishes
              the job.
              {review.recommendation
                ? ` It already recommends ${review.recommendation}, so an empty body completes it —`
                  + ' it is the REVIEW that has to say what it recommends, not the request.'
                : ' It recommends nothing yet, so a body without one is 400'
                  + ' RECOMMENDATION_REQUIRED.'}
            </p>
          </Card>

          <Card
            title="Call it off"
            description="CANCELLED, with a reason. This is what a DELETE would have been — the review stays, and says it was called off and why."
            action={
              <Button icon={Ban} onClick={() => setCancelling(true)}>Call it off</Button>
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
 * BOTH HALVES OR NEITHER. A part with a name and no score is not a score — and it matters more than
 * it looks, because the two default rows arrive named and unscored. Counting a name alone would
 * make the opening state send `{"INTERVIEW": ""}` and 400 before anybody typed.
 *
 * A SCORE THAT IS NOT A NUMBER IS STILL SENT AS TYPED, and the branch stays even though the boxes
 * are `type="number"` and will not accept letters. 400 MALFORMED_REQUEST is measured and real — the
 * JSON reader refuses it before the endpoint runs, so it beats even REVIEW_ALREADY_COMPLETED — it is
 * simply no longer reachable from HERE. Postman's Record a Review is where that one is sent from
 * now.
 */
function criteriaFrom(pairs) {
  const named = pairs.filter((one) => one.name.trim())
  const filled = named.filter((one) => one.score.trim())

  const assembled = Object.fromEntries(filled.map((one) => {
    const mark = one.score.trim()
    return [one.name.trim(), Number.isNaN(Number(mark)) ? mark : Number(mark)]
  }))

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
              <Field label="Scored" hint="A decimal — the field is a BigDecimal on the server, so 42.5 and 42.50 are both fine. A part with no score is skipped rather than sent empty.">
                <Input type="number" step="0.01" value={one.score} placeholder="42.50"
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
              score</b>, so {unscored === 1 ? 'it is' : 'they are'} left out — a part with a name
              and nothing against it is not a score. Give {unscored === 1 ? 'it' : 'them'} a
              number, or remove the row.
            </p>
          ) : null}
          {filled.length === 0 ? (
            <p className="muted">
              <Info size={12} /> Nothing is scored yet, so this sends{' '}
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
  const [status, setStatus] = useState('')
  const [score, setScore] = useState('')
  const [recommendation, setRecommendation] = useState('')
  //! THE CRITERIA ARE TYPED AS PAIRS, not as JSON. A reviewer fills in what the part was called
  //! and what it scored; the body is assembled from that. Typing `{"INTERVIEW": 42.5}` by hand is
  //! a brace away from a 400 that says nothing about admissions.
  const [criteriaMode, setCriteriaMode] = useState('')
  const [pairs, setPairs] = useState(DEFAULT_PAIRS.map((one) => ({ ...one })))
  const [notes, setNotes] = useState('')
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

  const body = {
    ...(status ? { status } : {}),
    ...(score === '' ? {} : { score: Number(score) }),
    ...(recommendation ? { recommendation } : {}),
    ...(parsedCriteria === undefined ? {} : { criterionScores: parsedCriteria }),
    ...(notes ? { notes } : {}),
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

        <Field
          label="Move it to"
          hint={`It is ${review.status}. Leave it empty to record a score without declaring yourself done. PENDING is offered and goes nowhere — the graph only runs forwards.`}
        >
          <Select
            value={status}
            options={REVIEW_STATUSES.map((one) => ({
              value: one, label: one === '' ? 'leave it where it is' : one,
            }))}
            label="New status"
            onChange={setStatus}
          />
        </Field>

        <Field
          label="Score"
          hint="No upper bound — out of 100, out of 50, out of 5 is the school's business, not the API's. Negative is refused, because that is a typo rather than a scale."
        >
          <Input type="number" step="0.01" value={score}
            onChange={(e) => setScore(e.target.value)} placeholder="86.50" />
        </Field>

        <Field
          label="Recommends"
          hint={status === 'COMPLETED'
            ? 'REQUIRED to complete. Leave it empty to see 400 RECOMMENDATION_REQUIRED — it is not enforced here.'
            : 'What THIS person suggests. Not a decision: #20 is what the school does, and it may decide something no reviewer recommended.'}
        >
          <Select
            value={recommendation}
            options={RECOMMENDATIONS.map((one) => ({
              value: one, label: one === '' ? 'nothing yet' : one,
            }))}
            label="Recommendation"
            onChange={setRecommendation}
          />
        </Field>

        <CriterionFields
          mode={criteriaMode} setMode={setCriteriaMode} pairs={pairs} setPairs={setPairs} />

        <Field
          label="Notes"
          hint={status === 'CANCELLED'
            ? 'REQUIRED to cancel — leave it empty to see 400 CANCELLATION_NOTE_REQUIRED, which is not enforced here. Work called off with no reason is a gap in the record, the same reading that makes lostReason required on a lost inquiry.'
            : 'Optional, up to 2000 characters.'}
        >
          <Input value={notes} onChange={(e) => setNotes(e.target.value)}
            placeholder="The applicant performed well in the interaction." />
        </Field>

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
  const [recommendation, setRecommendation] = useState(review.recommendation ?? '')
  const [score, setScore] = useState(review.score === undefined ? '' : String(review.score))
  const [criteriaMode, setCriteriaMode] = useState('')
  const [pairs, setPairs] = useState(DEFAULT_PAIRS.map((one) => ({ ...one })))
  const [notes, setNotes] = useState('')
  const [version, setVersion] = useState(String(review.version ?? ''))
  const [saving, setSaving] = useState(false)
  const [refused, setRefused] = useState(null)

  const { assembled } = criteriaFrom(pairs)
  const parsedCriteria = criteriaMode === 'clear' ? {}
    : (criteriaMode === 'replace' ? assembled : undefined)

  const body = {
    ...(recommendation ? { recommendation } : {}),
    ...(score === '' ? {} : { score: Number(score) }),
    ...(parsedCriteria === undefined ? {} : { criterionScores: parsedCriteria }),
    ...(notes ? { notes } : {}),
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
      description="COMPLETED, with a recommendation. Both ends are terminal — this is where the review stops."
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

        <Field
          label="Recommends"
          hint={review.recommendation
            ? `It already recommends ${review.recommendation}. Leave it and that stands — it is the REVIEW that has to say what it recommends, not this request.`
            : 'REQUIRED, because the review has none yet. Leave it empty to see 400 RECOMMENDATION_REQUIRED — it is not enforced here.'}
        >
          <Select
            value={recommendation}
            options={RECOMMENDATIONS.map((one) => ({
              value: one, label: one === '' ? 'send nothing' : one,
            }))}
            label="Recommendation"
            onChange={setRecommendation}
          />
        </Field>

        <Field
          label="Score"
          hint="Optional, and pre-filled from the review so finishing does not blank what was saved earlier. No upper bound — out of 100, out of 50 or out of 5 is the school's business. Negative is 400."
        >
          <Input type="number" step="0.01" value={score}
            onChange={(e) => setScore(e.target.value)} placeholder="86.50" />
        </Field>

        <CriterionFields
          mode={criteriaMode} setMode={setCriteriaMode} pairs={pairs} setPairs={setPairs} />

        <Field
          label="Notes"
          hint="Optional, up to 2000 characters. Sending replaces what is on the review; leaving it empty keeps it."
        >
          <Input value={notes} onChange={(e) => setNotes(e.target.value)}
            placeholder="The applicant performed well in the interaction." />
        </Field>

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
 * #27d — the school called it off.
 *
 * THE REASON IS THE WHOLE BODY, and it is still not enforced here. 400 CANCELLATION_NOTE_REQUIRED is
 * the refusal this endpoint is most worth sending, and a form that would not let it be sent would
 * put it out of reach.
 *
 * IT IS NOT PRE-FILLED from the review's notes, unlike the score on Finish it. Those notes are what
 * the reviewer wrote about a child; a cancellation reason is why the school stopped. Seeding one
 * with the other would invite somebody to send the reviewer's observation back as the reason it was
 * abandoned — and it would overwrite the observation with itself for no gain.
 */
function CancelReview({ review, onClose, onDone }) {
  const { call } = useApi()
  const [notes, setNotes] = useState('')
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
            ? 'The review already carries notes, so an empty body is accepted and they stand as the reason. Sending something REPLACES them.'
            : 'REQUIRED — the review has no notes. Leave it empty for 400 CANCELLATION_NOTE_REQUIRED, which is not enforced here. Work called off with no reason is a gap in the record, the same reading that makes lostReason required on a lost inquiry.'}
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
