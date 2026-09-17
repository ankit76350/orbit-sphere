import { useState } from 'react'
import { AlertTriangle, CopyPlus, Info } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Field, Input } from '../../../components/ui/Kit.jsx'

const EVERY_CLASS = ''
const EVERY_SECTION = ''

/**
 * Copying a day: the #6 half of /school-academics/timetable/:date
 *
 * ONE ENDPOINT — #6, and it is what a school actually does. Monday is typed once; Tuesday through
 * Friday are copied from it and then corrected. Without it, a week of a 400-period school is 2,000
 * periods typed by hand.
 *
 * THE DAY ON SCREEN IS THE SOURCE, and the form asks only where it is going. The API takes the
 * target in the path and the source in the body — the opposite arrangement — because the day being
 * BUILT is what the endpoint acts on. Here the day being READ is what the reader has in hand, so
 * the form fills the source in and asks for the target.
 *
 * THE FILTERS ARE BUILT FROM THIS DAY'S OWN PERIODS, so a class or section that is not in it cannot
 * be picked by accident — but both are dropdowns over what is here, never a gate: NOTHING_TO_COPY
 * stays reachable by picking a class and then a section it does not use.
 *
 * MERGE IS OFF BY DEFAULT, which is what the API assumes too. It is the destructive-adjacent
 * option: it writes into a day somebody else may have built, and every conflict check then runs
 * against the combined list.
 *
 * NOTHING IS DISABLED. A target on a holiday, a target that already exists, a filter that matches
 * nothing, and the day itself as its own target are all still sendable.
 */
export default function TimetableCopy({ day, date }) {
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

  const body = () => {
    const sent = { sourceDate: date }
    if (classDocsId) sent.classDocsId = classDocsId
    if (sectionNo) sent.sectionNo = sectionNo
    if (merge) sent.merge = true
    return sent
  }

  const submit = async () => {
    setSending(true)
    const answer = await call('copy-timetable', {
      label: 'Build a day from this one',
      pathParams: { year: actingAcademicYear ?? '', date: target },
      body: body(),
    })
    setSending(false)
    setResult(answer)
  }

  if (!actingSubdomain) return null

  const answered = result?.ok ? result.bodyJson : null

  return (
    <>
      <Card
        title="Build another day from this one"
        description="#6 — what a school actually does. Monday is typed once; the rest of the week is copied from it and then corrected."
        action={
          <div className="btn-row">
            <EndpointTag id="copy-timetable" name="Copy"
              pathParams={{ year: actingAcademicYear, date: target }} />
            <Badge>{wouldCopy} of {entries.length} periods</Badge>
            <Button look="primary" icon={CopyPlus} busy={sending} onClick={submit}>
              Copy this day
            </Button>
          </div>
        }
      >
        <div className="field-grid">
          <Field
            label="To this date"
            hint="Required. Must be a working day inside the year — a holiday is 409 NOT_A_WORKING_DAY, and this day itself is 400 SOURCE_IS_TARGET."
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
        </div>

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
      </Card>

      {result ? (
        <Card
          title={answered
            ? (answered.merged ? 'Merged into that day' : 'That day was built')
            : (result.bodyJson?.code ?? `The server answered ${result.status}`)}
          description={answered
            ? 'Both dates, because a copy is about two days — and what was copied against what was already there.'
            : 'The refusal exactly as it came. Nothing on this page predicted it.'}
          action={<Badge>{result.status}</Badge>}
        >
          {answered ? (
            <>
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr><th>Built</th><th>From</th><th>Copied</th><th>Already there</th>
                      <th>Periods now</th><th>Version</th></tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td><b>{answered.date}</b></td>
                      <td>{answered.sourceDate}</td>
                      <td>{answered.copiedCount}</td>
                      <td>{answered.keptCount}</td>
                      <td>{answered.timetable?.entryCount}</td>
                      <td><span className="mono">{answered.version}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              {/* 201 BUILT vs 200 MERGED — the status says whether something came into being. */}
              <p className="muted">
                <Info size={12} /> <b>{result.status === 201 ? '201 — a day came into being.'
                  : '200 — nothing came into being; these periods were added.'}</b>
              </p>
              <p className="muted">{answered.nextStep}</p>
            </>
          ) : (
            <div className="resp">
              <div className="resp-head">
                <span className="resp-status" data-ok="false">
                  {result.bodyJson?.code ?? result.status}
                </span>
              </div>
              <p>{result.bodyJson?.message ?? 'Nothing came back.'}</p>
            </div>
          )}
        </Card>
      ) : null}
    </>
  )
}
