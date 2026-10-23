import { useState } from 'react'
import { AlertTriangle, Info } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Button, Field, Input, Modal } from '../../../components/ui/Kit.jsx'

const EVERY_CLASS = ''
const EVERY_SECTION = ''

/**
 * Copying a day: the #6 dialog of /school-academics/timetable/:date
 *
 * ONE ENDPOINT — #6, and it is what a school actually does. Monday is typed once; Tuesday through
 * Friday are copied from it and then corrected. Without it, a week of a 400-period school is 2,000
 * periods typed by hand.
 *
 * A MODAL, NOT A CARD BELOW THE DAY. The fields are on the left and the REQUEST BODY THEY BUILD is
 * on the right, updating as they are typed — which is the point of this tool: what is about to be
 * sent is visible before it is sent, rather than reconstructed from the response afterwards.
 *
 * THE DAY ON SCREEN IS THE SOURCE, and the form asks only where it is going. The API takes the
 * target in the path and the source in the body — the opposite arrangement — because the day being
 * BUILT is what the endpoint acts on. Here the day being READ is what the reader has in hand.
 *
 * THE FILTERS ARE BUILT FROM THIS DAY'S OWN PERIODS, so a class or section this day does not hold
 * cannot be picked by accident — but neither is a gate: NOTHING_TO_COPY stays reachable by picking
 * a class and then a section it does not use.
 *
 * MERGE IS OFF BY DEFAULT, which is what the API assumes too. It writes into a day somebody else
 * may have built, and every conflict check then runs against the combined list.
 *
 * NOTHING IS DISABLED. A target on a holiday, a target that already exists, a filter that matches
 * nothing, and the day itself as its own target are all still sendable.
 */
export default function TimetableCopy({ open, onClose, day, date }) {
  const { call } = useApi()
  const { actingSubdomain, actingAcademicYear } = useApiState()

  const [target, setTarget] = useState('')
  const [classDocsId, setClassDocsId] = useState(EVERY_CLASS)
  const [sectionNo, setSectionNo] = useState(EVERY_SECTION)
  const [merge, setMerge] = useState(false)
  const [result, setResult] = useState(null)
  const [sending, setSending] = useState(false)

  const entries = day?.entries ?? []

  //! THE CLASSES THIS DAY ACTUALLY HOLDS, in the order they first appear. Built from the periods
  //! rather than read from the structure endpoint: the question is "what is in this day", and a
  //! class the day does not use is not something to offer copying.
  const classes = []
  for (const entry of entries) {
    if (!classes.some((one) => one.classDocsId === entry.classDocsId)) {
      classes.push({ classDocsId: entry.classDocsId, className: entry.className })
    }
  }

  //! THE SECTIONS OF THE CHOSEN CLASS. Empty while no class is chosen, because sectionNo without
  //! classDocsId is 400 SECTION_WITHOUT_CLASS — "section A" is not one thing across a school.
  const sections = []
  for (const entry of entries) {
    if (classDocsId && entry.classDocsId === classDocsId && !sections.includes(entry.sectionNo)) {
      sections.push(entry.sectionNo)
    }
  }

  //! HOW MANY PERIODS THIS FILTER WOULD CARRY, worked out the way the server does. Shown before
  //! the button, so NOTHING_TO_COPY is a thing a tester can aim at rather than stumble into.
  const wouldCopy = entries.filter((entry) =>
    (!classDocsId || entry.classDocsId === classDocsId)
    && (!sectionNo || entry.sectionNo.toLowerCase() === sectionNo.toLowerCase())).length

  //! THE BODY IS DERIVED, NOT ASSEMBLED ON SUBMIT, so the pane on the right is the request itself
  //! rather than a rendering of it. A field left empty is left OUT — the API reads an absent
  //! filter as "every class", and sending "" would be a filter matching nothing.
  const body = { sourceDate: date }
  if (classDocsId) body.classDocsId = classDocsId
  if (sectionNo) body.sectionNo = sectionNo
  if (merge) body.merge = true

  const submit = async () => {
    setSending(true)
    const answer = await call('copy-timetable', {
      label: 'Build a day from this one',
      pathParams: { year: actingAcademicYear ?? '', date: target },
      body,
    })
    setSending(false)
    setResult(answer)
  }

  if (!actingSubdomain) return null

  const answered = result?.ok ? result.bodyJson : null

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title="Build another day from this one"
      description="#6 — what a school actually does. Monday is typed once; the rest of the week is copied from it and then corrected."
      endpoint={
        <EndpointTag id="copy-timetable" name="Copy" look="primary"
          pathParams={{ year: actingAcademicYear, date: target }} />
      }
      footer={
        <>
          <span className="muted">{wouldCopy} of {entries.length} periods</span>
          <span className="toolbar-spacer" />
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={sending} onClick={submit}>Copy this day</Button>
        </>
      }
    >
      <div className="stack">
        {result ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok={answered ? 'true' : 'false'}>
                {answered
                  ? `${result.status} · ${answered.merged ? 'merged into' : 'built'} ${answered.date}`
                  : (result.bodyJson?.code ?? result.status)}
              </span>
            </div>
            {answered ? (
              <pre className="resp-body">
                {answered.copiedCount} copied · {answered.keptCount} already there ·{' '}
                {answered.timetable?.entryCount} periods now · version {answered.version}
                {'\n'}
                {/* 201 BUILT vs 200 MERGED — the status says whether something came into being. */}
                {result.status === 201
                  ? '201 — a day came into being.'
                  : '200 — nothing came into being; these periods were added.'}
              </pre>
            ) : (
              <pre className="resp-body">{result.bodyJson?.message ?? 'Nothing came back.'}</pre>
            )}
          </div>
        ) : null}

        <Field
          label="To this date"
          required
          hint="Must be a working day inside the year — a holiday is 409 NOT_A_WORKING_DAY, and this day itself is 400 SOURCE_IS_TARGET."
        >
          <Input type="date" value={target} onChange={(e) => setTarget(e.target.value)} />
        </Field>

        <Field label="Class" hint="Absent copies every class of this day.">
          <Select
            label="Class"
            value={classDocsId}
            onChange={(value) => { setClassDocsId(value); setSectionNo(EVERY_SECTION) }}
            options={[{ value: EVERY_CLASS, label: '— every class —' },
              ...classes.map((one) => ({
                value: one.classDocsId,
                label: one.className ?? one.classDocsId,
              }))]}
          />
        </Field>

        <Field
          label="Section"
          hint="Needs a class beside it — on its own it is 400 SECTION_WITHOUT_CLASS, because 'A' names one section in every class that has one."
        >
          <Select
            label="Section"
            value={sectionNo}
            onChange={setSectionNo}
            options={[{ value: EVERY_SECTION, label: '— every section —' },
              ...sections.map((one) => ({ value: one, label: one }))]}
          />
        </Field>

        <Field
          label="If that day already has a timetable"
          hint="Off is 409 TIMETABLE_ALREADY_EXISTS. On ADDS these periods to it, and every conflict check then runs against the combined list."
        >
          <label className="check">
            <input type="checkbox" checked={merge}
              onChange={(event) => setMerge(event.target.checked)} />
            <span>merge into it</span>
          </label>
        </Field>

        {wouldCopy === 0 ? (
          <p className="muted">
            <AlertTriangle size={12} /> <b>This filter matches no period of this day</b>, so the
            answer will be <span className="mono">409 NOTHING_TO_COPY</span> — a refusal rather
            than an empty success, because a 201 saying &ldquo;0 copied&rdquo; would read as though
            something worked.
          </p>
        ) : null}

        <p className="muted">
          <Info size={12} /> <b>Every copied period gets a new entry id.</b> They are different
          periods on a different date, and two days sharing an id would make an attendance
          session&apos;s <span className="mono">timetableEntryId</span> ambiguous. Everything else
          is carried across field for field, in the order this day holds it.
        </p>
        <p className="muted">
          <Info size={12} /> <b>A holiday is refused here, where #1 skips it.</b> #1 writes a
          <em> range</em> and any range longer than about five days contains a weekly off, so
          skipping is the only way ranges stay usable. A copy names <em>one</em> date, and a caller
          who named a festival meant a different day.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Merging is the risky half.</b> A teacher free in this day and free
          in the target can still be in two places once the two are put together — which is why the
          checks run on the combined list, and why nothing is written when one fires.
        </p>
      </div>
    </Modal>
  )
}
