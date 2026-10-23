import { useState } from 'react'
import { Info } from 'lucide-react'
import { useApi } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Button, Field, Input, Modal } from '../../../components/ui/Kit.jsx'

/**
 * Assigning a subject — #22. Used by both the class page and a section page.
 *
 * THE ONE RULE THIS ENDPOINT SETTLED: a subject is class-wide OR per-section, never both. Leave
 * the section blank and every section studies it; name a section and only that one does. The same
 * subjectCode cannot do both, because a section studies its own rows AND the class's — it would
 * get the subject twice, with two teachers and nothing saying which wins.
 *
 * THE KEY IS THE PAIR (subjectCode, sectionNo), not the code, so HINDI for A and HINDI for B are
 * two legitimate rows. That is how two sections get different teachers for one subject.
 *
 * THE SECTION IS A TEXT BOX, NOT A DROPDOWN OF REAL SECTIONS. A dropdown could not send a section
 * the class does not have, and SECTION_NOT_FOUND is a refusal worth being able to reach.
 *
 * TEACHER IDS ARE SENT EXACTLY AS TYPED, duplicates included. The API refuses a repeated id with
 * DUPLICATE_TEACHER rather than collapsing it, and silently de-duplicating here would hide the
 * refusal this tool exists to exercise.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD — a class gets its subjects in one sitting. Only the code and
 * the names are cleared; the type, the section and the teachers usually repeat.
 */

const TYPES = ['CORE', 'ELECTIVE', 'LANGUAGE', 'ACTIVITY', 'VOCATIONAL']

const BLANK = {
  subjectCode: '',
  name: '',
  shortName: '',
  subjectType: 'CORE',
  sectionNo: '',
  teacherDocsIds: '',
  gradingSchemeDocsId: '',
}

export default function AddSubject({ open, classId, year, fixedSection, onClose, onAdded }) {
  const { call } = useApi()
  const [form, setForm] = useState(BLANK)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [added, setAdded] = useState(null)

  // Kit's Input hands over the EVENT; Select hands over the VALUE. Two setters, because one
  // that guessed would put "[object Object]" in the body.
  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))
  const setValue = (field) => (value) =>
    setForm((old) => ({ ...old, [field]: value }))

  // A section fixed by the page it was opened from wins over the box.
  const section = fixedSection ?? form.sectionNo

  // An empty optional box sends nothing rather than "", which the API would read as an
  // instruction rather than as "not provided".
  const body = (() => {
    const out = {
      subjectCode: form.subjectCode,
      name: form.name,
      subjectType: form.subjectType,
    }
    if (form.shortName.trim() !== '') out.shortName = form.shortName.trim()
    if (section !== '' && section != null) out.sectionNo = section
    if (form.teacherDocsIds.trim() !== '') {
      out.teacherDocsIds = form.teacherDocsIds.split(',').map((one) => one.trim())
    }
    if (form.gradingSchemeDocsId.trim() !== '') {
      out.gradingSchemeDocsId = form.gradingSchemeDocsId.trim()
    }
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('add-class-subject', {
      label: 'Assign a subject',
      pathParams: { year: year ?? '', id: classId },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setAdded(result.bodyJson)
      setForm((old) => ({ ...old, subjectCode: '', name: '', shortName: '' }))
      onAdded()
      return
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) setRefused(result.bodyJson)
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title={fixedSection ? `Add a subject for section ${fixedSection}` : 'Add a subject'}
      description="Class-wide or for one section, never both — a section studies its own rows and the class's."
      endpoint={<EndpointTag id="add-class-subject" name="Assign" look="primary" />}
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
              <span className="resp-status" data-ok="false">{refused.code}</span>
            </div>
            <pre className="resp-body">{refused.message}</pre>
          </div>
        ) : null}

        {added ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">
                {added.subjectCount} subject{added.subjectCount === 1 ? '' : 's'} ·{' '}
                {added.activeCount} active
              </span>
            </div>
            <pre className="resp-body">{added.changeSummary}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Subject code"
            required
            hint='Uppercased, non-alphanumerics to underscore — "maths-2" is stored MATHS_2. Nothing left is a 409.'
            error={errors.subjectCode}
          >
            <Input value={form.subjectCode} error={errors.subjectCode}
              onChange={set('subjectCode')} placeholder="MATHEMATICS" />
          </Field>
          <Field
            label="Type"
            required
            hint="Five, exhaustive. ELECTIVE is what the class offers, not what a student takes."
            error={errors.subjectType}
          >
            <Select label="Type" value={form.subjectType}
              onChange={setValue('subjectType')} options={TYPES} />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Name"
            required
            hint="The display value. Its existence is what lets the code stay a code."
            error={errors.name}
          >
            <Input value={form.name} error={errors.name}
              onChange={set('name')} placeholder="Mathematics" />
          </Field>
          <Field label="Short name" hint="Optional. For a timetable cell and a report card column."
            error={errors.shortName}>
            <Input value={form.shortName} error={errors.shortName}
              onChange={set('shortName')} placeholder="Maths" />
          </Field>
        </div>

        {fixedSection ? null : (
          <Field
            label="Section"
            hint="Blank means the whole class. Matched case-insensitively and stored the way the class spells it — a section the class does not have is a 404."
            error={errors.sectionNo}
          >
            <Input value={form.sectionNo} error={errors.sectionNo}
              onChange={set('sectionNo')} placeholder="leave blank for every section" />
          </Field>
        )}

        <div className="field-grid">
          <Field
            label="Teacher ids"
            hint="Optional Staff.ids, comma separated. Each checked against this school. Sent as typed — the same id twice is a 400, not a silent merge."
            error={errors.teacherDocsIds}
          >
            <Input value={form.teacherDocsIds} error={errors.teacherDocsIds}
              onChange={set('teacherDocsIds')} placeholder="67aa15d9dc3f7d0011111111, 67aa…2222" />
          </Field>
          <Field
            label="Grading scheme id"
            hint="Optional. Checked against this school — another school's real id is a 404."
            error={errors.gradingSchemeDocsId}
          >
            <Input value={form.gradingSchemeDocsId} error={errors.gradingSchemeDocsId}
              onChange={set('gradingSchemeDocsId')} placeholder="67aa15d9dc3f7d0033333333" />
          </Field>
        </div>

        <p className="muted">
          <Info size={12} /> {fixedSection
            ? `Fixed to section ${fixedSection} by the page you opened this from. If the class `
              + 'already teaches this subject class-wide, this is a 409 — it would be taught twice.'
            : 'Leave the section blank and every section studies it. Name one and only that '
              + 'section does. The same code cannot do both.'}
        </p>
      </div>
    </Modal>
  )
}
