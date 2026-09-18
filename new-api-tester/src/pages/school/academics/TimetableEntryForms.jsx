import { useState } from 'react'
import { AlertTriangle, Info } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Button, Field, Input, Modal } from '../../../components/ui/Kit.jsx'

const SLOT_TYPES = ['LESSON', 'BREAK', 'ASSEMBLY', 'ACTIVITY']

/** The response, or the refusal, rendered the same way in both dialogs. */
function Answer({ result, okLabel }) {
  if (!result) return null
  const body = result.bodyJson
  return (
    <div className="resp">
      <div className="resp-head">
        <span className="resp-status" data-ok={result.ok ? 'true' : 'false'}>
          {result.ok ? `${result.status} · ${okLabel}` : (body?.code ?? result.status)}
        </span>
      </div>
      <pre className="resp-body">
        {result.ok
          ? `${body?.periodCode ?? ''} · ${body?.className ?? ''} ${body?.sectionNo ?? ''}`
            + ` · ${body?.startTime ?? ''}–${body?.endTime ?? ''}`
            + `\n${body?.timetableEntryId ?? ''}`
          : (body?.message ?? 'Nothing came back.')}
      </pre>
    </div>
  )
}

/**
 * Adding one period: the #3 dialog of /school-academics/timetable/:date
 *
 * A $PUSH, NOT A RE-SAVE, and the dialog says so — what this sends is one period, and what it
 * cannot do is disturb the others. The day it joins is the one on the screen behind.
 *
 * NOTHING IS PRE-FILLED FROM THE DAY except the class and section lists, because an addition is a
 * new period and not a copy of an old one. The class and section are dropdowns over what the day
 * already holds, plus free text is not needed — a period for a section with nothing yet is added
 * from the grid, which is #1's job.
 *
 * NOTHING IS DISABLED. A duplicate period code, an overlapping time, a subject the section does not
 * study and a break carrying a subject are all still sendable.
 */
export function AddEntryModal({ open, onClose, day, date, onDone }) {
  const { call } = useApi()
  const { actingSubdomain, actingAcademicYear } = useApiState()

  const entries = day?.entries ?? []
  const first = entries[0] ?? {}

  const [form, setForm] = useState({
    periodCode: '',
    classDocsId: first.classDocsId ?? '',
    sectionNo: first.sectionNo ?? '',
    slotType: 'LESSON',
    subjectCode: '',
    teacherDocsId: '',
    slotLabel: '',
    startTime: '',
    endTime: '',
    facilityResourceDocsId: '',
  })
  const [result, setResult] = useState(null)
  const [sending, setSending] = useState(false)

  const set = (field) => (value) =>
    setForm((old) => ({ ...old,
      [field]: typeof value === 'string' ? value : value.target.value }))

  //! THE CLASSES AND SECTIONS THIS DAY HOLDS. Built from the periods rather than read again: the
  //! day is already on screen, and a class it does not use is not what an addition is usually for.
  const classes = []
  for (const entry of entries) {
    if (!classes.some((one) => one.classDocsId === entry.classDocsId)) {
      classes.push({ classDocsId: entry.classDocsId, className: entry.className })
    }
  }
  const sections = []
  for (const entry of entries) {
    if (entry.classDocsId === form.classDocsId && !sections.includes(entry.sectionNo)) {
      sections.push(entry.sectionNo)
    }
  }

  //! AN EMPTY FIELD IS LEFT OUT, never sent as "". The API reads an absent subjectCode as "no
  //! subject", and a blank one on a BREAK would look like a subject it is not allowed to carry.
  const body = {
    periodCode: form.periodCode,
    classDocsId: form.classDocsId,
    sectionNo: form.sectionNo,
    slotType: form.slotType,
    startTime: form.startTime,
    endTime: form.endTime,
  }
  if (form.subjectCode.trim()) body.subjectCode = form.subjectCode.trim()
  if (form.teacherDocsId.trim()) body.teacherDocsId = form.teacherDocsId.trim()
  if (form.slotLabel.trim()) body.slotLabel = form.slotLabel.trim()
  if (form.facilityResourceDocsId.trim()) {
    body.facilityResourceDocsId = form.facilityResourceDocsId.trim()
  }

  const submit = async () => {
    setSending(true)
    const answer = await call('add-timetable-entry', {
      label: 'Add one period',
      pathParams: { year: actingAcademicYear ?? '', date: date ?? '' },
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok && onDone) onDone()
  }

  if (!actingSubdomain) return null

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title="Add one period"
      description="#3 — a $push into the day, never a re-save. Nothing else in the day is touched."
      endpoint={
        <EndpointTag id="add-timetable-entry" name="Add" look="primary"
          pathParams={{ year: actingAcademicYear, date }} />
      }
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={sending} onClick={submit}>Add it</Button>
        </>
      }
    >
      <div className="stack">
        <Answer result={result} okLabel="added" />

        <div className="field-grid">
          <Field label="Period code" required hint="Unique per section per day — a repeat is 409 PERIOD_CODE_TAKEN, and that one is guarded in the write itself.">
            <Input value={form.periodCode} onChange={set('periodCode')} placeholder="P5" />
          </Field>
          <Field label="Class" hint="A class this year does not have is 404 CLASS_NOT_FOUND.">
            <Select label="Class" value={form.classDocsId}
              onChange={(v) => setForm((o) => ({ ...o, classDocsId: v, sectionNo: '' }))}
              options={[{ value: '', label: '— pick a class —' },
                ...classes.map((one) => ({
                  value: one.classDocsId, label: one.className ?? one.classDocsId }))]} />
          </Field>
          <Field label="Section" hint="Case-insensitive — it is stored in the class's own spelling.">
            <Select label="Section" value={form.sectionNo} onChange={set('sectionNo')}
              options={[{ value: '', label: '— pick a section —' },
                ...sections.map((one) => ({ value: one, label: one }))]} />
          </Field>
          <Field label="Slot" hint="A LESSON needs a subject and a teacher; the others must carry no subject.">
            <Select label="Slot" value={form.slotType} onChange={set('slotType')}
              options={SLOT_TYPES} />
          </Field>
          {/* NOT GATED ON slotType: a BREAK carrying a subject is SLOT_FIELDS_NOT_ALLOWED and a
              LESSON without a teacher is SLOT_FIELDS_REQUIRED — both worth triggering. */}
          <Field label="Subject" hint="Must be one that section studies — class-wide, or its own.">
            <Input value={form.subjectCode} onChange={set('subjectCode')} placeholder="MATHS" />
          </Field>
          <Field label="Teacher id" hint="Must be staff of this school. Optional on a non-lesson — somebody supervises lunch.">
            <Input value={form.teacherDocsId} onChange={set('teacherDocsId')} />
          </Field>
          <Field label="Label" hint="What a timetable prints for a non-lesson.">
            <Input value={form.slotLabel} onChange={set('slotLabel')} placeholder="Lunch Break" />
          </Field>
          <Field label="From" required hint="Strictly before 'to' — equal times are refused too.">
            <Input type="time" value={form.startTime} onChange={set('startTime')} />
          </Field>
          <Field label="To" required>
            <Input type="time" value={form.endTime} onChange={set('endTime')} />
          </Field>
          <Field label="Room id" hint="Optional, and never checked against anything — #1 does not validate rooms either.">
            <Input value={form.facilityResourceDocsId}
              onChange={set('facilityResourceDocsId')} />
          </Field>
        </div>

        <p className="muted">
          <Info size={12} /> <b>It is checked against the whole day, not on its own.</b> Whether it
          fits beside the periods already there is the only thing worth checking — so an overlapping
          section, teacher or room is refused here exactly as it would be by #1.
        </p>
      </div>
    </Modal>
  )
}

/**
 * Correcting one period: the #4 dialog — <b>the substitution</b>.
 *
 * ONE FIELD OF ONE PERIOD. A teacher calls in sick at 07:40 and six periods need covering before
 * 08:00, so the form starts from what the period IS and sends only what moved.
 *
 * "" CLEARS AND ABSENT LEAVES ALONE, and the dialog makes that operable rather than explaining it:
 * every clearable field has its own "clear" toggle, because a text box cannot distinguish "I left
 * this alone" from "I want it empty" once it has been emptied.
 *
 * WHAT IT WILL NOT CHANGE IS SHOWN, NOT HIDDEN. The class, section and slot type are displayed
 * read-only with the reason — moving a period to another section is deleting one and adding
 * another, and a slot type decides which other fields are legal.
 *
 * THE VERSION IS OPTIONAL AND EDITABLE. Sending a stale one on purpose is how
 * CONCURRENT_MODIFICATION gets triggered without a second browser.
 */
export function PatchEntryModal({ open, onClose, day, date, entry, onDone }) {
  const { call } = useApi()
  const { actingSubdomain, actingAcademicYear } = useApiState()

  const [form, setForm] = useState({
    periodCode: entry?.periodCode ?? '',
    subjectCode: entry?.subjectCode ?? '',
    teacherDocsId: entry?.teacherDocsId ?? '',
    slotLabel: entry?.slotLabel ?? '',
    startTime: entry?.startTime ?? '',
    endTime: entry?.endTime ?? '',
    facilityResourceDocsId: entry?.facilityResourceDocsId ?? '',
  })
  //! WHICH FIELDS ARE BEING CLEARED, kept apart from their values. "" and "unchanged" are the same
  //! empty box, and the API reads them as opposite instructions — so the instruction is its own
  //! control rather than something inferred from an empty string.
  const [clearing, setClearing] = useState({})
  const [version, setVersion] = useState('')
  const [result, setResult] = useState(null)
  const [sending, setSending] = useState(false)

  const set = (field) => (value) =>
    setForm((old) => ({ ...old,
      [field]: typeof value === 'string' ? value : value.target.value }))

  const toggleClear = (field) => () =>
    setClearing((old) => ({ ...old, [field]: !old[field] }))

  //! ONLY WHAT MOVED. A field equal to what the period already holds is left out, so the body is
  //! the correction rather than the period — which is what makes NOTHING_TO_UPDATE reachable by
  //! simply pressing the button without touching anything.
  const body = {}
  if (version.trim() !== '') body.version = Number(version)
  for (const field of ['periodCode', 'subjectCode', 'teacherDocsId', 'slotLabel',
    'startTime', 'endTime', 'facilityResourceDocsId']) {
    if (clearing[field]) {
      body[field] = ''
    } else if (form[field] !== (entry?.[field] ?? '')) {
      body[field] = form[field]
    }
  }

  const submit = async () => {
    setSending(true)
    const answer = await call('patch-timetable-entry', {
      label: 'Correct one period',
      pathParams: {
        year: actingAcademicYear ?? '',
        date: date ?? '',
        entryId: entry?.timetableEntryId ?? '',
      },
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok && onDone) onDone()
  }

  if (!actingSubdomain || !entry) return null

  const clearable = (field, label, hint, type) => (
    <Field label={label} hint={hint}>
      <Input type={type} value={clearing[field] ? '' : form[field]} onChange={set(field)} />
      <label className="check">
        <input type="checkbox" checked={!!clearing[field]} onChange={toggleClear(field)} />
        <span>clear it — sends <span className="mono">&quot;&quot;</span></span>
      </label>
    </Field>
  )

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title={`Correct ${entry.periodCode}`}
      description="#4 — the substitution. One targeted $set through an array filter; nothing else in the day is touched."
      endpoint={
        <EndpointTag id="patch-timetable-entry" name="Correct" look="primary"
          pathParams={{ year: actingAcademicYear, date, entryId: entry.timetableEntryId }} />
      }
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={sending} onClick={submit}>Correct it</Button>
        </>
      }
    >
      <div className="stack">
        <Answer result={result} okLabel="corrected" />

        {/* WHAT IT WILL NOT CHANGE, shown rather than hidden. */}
        <p className="muted">
          <AlertTriangle size={12} /> <b>Not editable:</b>{' '}
          <span className="mono">{entry.className ?? entry.classDocsId}</span> ·{' '}
          <span className="mono">section {entry.sectionNo}</span> ·{' '}
          <span className="mono">{entry.slotType}</span>. Moving a period to another section is
          deleting one and adding another, and the slot type decides which other fields are legal —
          so both mean delete and add, not a correction.
        </p>

        <div className="field-grid">
          <Field label="Version" hint="Optional, unlike on #2. Send it when this was decided from a screen that might be stale; type an older number to trigger CONCURRENT_MODIFICATION on purpose.">
            <Input value={version} onChange={(e) => setVersion(e.target.value)}
              placeholder={`day is at ${day?.version ?? '?'}`} />
          </Field>
          <Field label="Period code" hint="Cannot be cleared — a period needs one.">
            <Input value={form.periodCode} onChange={set('periodCode')} />
          </Field>
          {clearable('teacherDocsId', 'Teacher id',
            'THE SUBSTITUTION. Clearing it on a LESSON is 400 SLOT_FIELDS_REQUIRED.')}
          {clearable('subjectCode', 'Subject',
            'Still has to be one that section studies — the rule does not relax for an edit.')}
          {clearable('slotLabel', 'Label', 'What a timetable prints for a non-lesson.')}
          <Field label="From" hint="Cannot be cleared. Still strictly before 'to' afterwards.">
            <Input type="time" value={form.startTime} onChange={set('startTime')} />
          </Field>
          <Field label="To" hint="Cannot be cleared.">
            <Input type="time" value={form.endTime} onChange={set('endTime')} />
          </Field>
          {clearable('facilityResourceDocsId', 'Room id',
            'How a practical moved out of the lab is recorded — clearing sends "".')}
        </div>

        <p className="muted">
          <Info size={12} /> <b>Only what moved is sent.</b> The body on the right is the
          correction, not the period — press the button without changing anything and the answer is{' '}
          <span className="mono">400 NOTHING_TO_UPDATE</span>, because a correction has to say what
          it corrects.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Patching a field to the value it already has is a 200.</b> The write
          counts documents <em>matched</em>, not modified — reading a no-op as &ldquo;the period is
          gone&rdquo; would turn it into a 404.
        </p>
        <p className="muted">
          <Info size={12} /> <b>The corrected period is checked beside the others.</b> That is the
          whole point of a substitution: the covering teacher must not already be somewhere else at
          that hour.
        </p>
      </div>
    </Modal>
  )
}
