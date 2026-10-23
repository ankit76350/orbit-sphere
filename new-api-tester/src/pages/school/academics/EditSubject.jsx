import { useEffect, useState } from 'react'
import { Info } from 'lucide-react'
import { useApi } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Field, Input, Modal } from '../../../components/ui/Kit.jsx'

/**
 * Editing one subject assignment — #24. Used by the class page and a section page.
 *
 * THE PAIR IS THE ADDRESS, and this modal never lets it be edited. `subjectCode` goes in the path
 * and the row's own `sectionNo` in the query; neither has a box, because together they are the
 * key. Seven collections store the code as a plain string and a subject row has no id, so a
 * rename would not fail and would not cascade — it would leave every stored string naming an
 * assignment that no longer answers to it. Moving an assignment between sections is #26 then #22.
 *
 * TEACHERS AND THE ACTIVE FLAG ARE NOT HERE EITHER — #25 and #26/#27. Shown read-only, so the
 * screen says what exists rather than pretending the fields do not.
 *
 * IT SENDS ONLY WHAT CHANGED, which is what makes every refusal reachable: touch nothing and the
 * body is `{}`, which is the 400 NOTHING_TO_UPDATE this endpoint is supposed to give. Clear the
 * name and it sends `""`, which is SUBJECT_NAME_REQUIRED. A form that always sent all four fields
 * could reach neither.
 */

const TYPES = ['CORE', 'ELECTIVE', 'LANGUAGE', 'ACTIVITY', 'VOCATIONAL']

export default function EditSubject({ open, classId, year, subject, onClose, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(null)

  // The row as it was when the modal opened. Everything sent is a diff against this.
  const initial = {
    name: subject?.name ?? '',
    shortName: subject?.shortName ?? '',
    subjectType: subject?.subjectType ?? 'CORE',
    gradingSchemeDocsId: subject?.gradingSchemeDocsId ?? '',
  }

  useEffect(() => {
    if (open) { setForm(initial); setErrors({}); setRefused(null); setSaved(null) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, subject?.subjectCode, subject?.sectionNo])

  const current = form ?? initial
  const set = (field) => (event) =>
    setForm((old) => ({ ...(old ?? initial), [field]: event.target.value }))
  const setValue = (field) => (value) =>
    setForm((old) => ({ ...(old ?? initial), [field]: value }))

  // Only the fields that differ from what was read. An untouched form sends {} on purpose.
  const body = Object.fromEntries(
    Object.entries(current).filter(([field, value]) => value !== initial[field]),
  )

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('update-class-subject', {
      label: 'Edit a subject',
      pathParams: {
        year: year ?? '',
        id: classId,
        subjectCode: subject?.subjectCode ?? '',
      },
      // Absent for a class-wide row, which is exactly how the endpoint reads "the whole class".
      query: subject?.sectionNo ? { sectionNo: subject.sectionNo } : {},
      body,
    })
    setSaving(false)
    if (result.ok) { setSaved(result.bodyJson); onSaved(); return }
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
      title={subject ? `Edit ${subject.subjectCode}` : 'Edit a subject'}
      description="The code and the section are the key and cannot be changed here — a move is #26 then #22."
      endpoint={<EndpointTag id="update-class-subject" name="Save" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Save</Button>
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

        {saved ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">
                {saved.subjectCount} subject{saved.subjectCount === 1 ? '' : 's'} ·{' '}
                {saved.activeCount} active
              </span>
            </div>
            <pre className="resp-body">{saved.changeSummary}</pre>
          </div>
        ) : null}

        {/* The key, shown and not editable. */}
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Subject code</td>
                <td><span className="mono">{subject?.subjectCode}</span></td></tr>
              <tr><td className="muted">Applies to</td>
                <td>{subject?.sectionNo
                  ? <>section <span className="mono">{subject.sectionNo}</span></>
                  : <span className="muted">all sections</span>}</td></tr>
              <tr><td className="muted">Teachers</td>
                <td>
                  {(subject?.teacherDocsIds ?? []).length === 0
                    ? <span className="muted">none</span>
                    : (subject?.teacherDocsIds ?? []).map((one) => (
                        <span key={one} className="mono">{one} </span>
                      ))}
                  <Badge>#25 changes these</Badge>
                </td></tr>
              <tr><td className="muted">Status</td>
                <td>
                  <Badge tone={subject?.active ? 'good' : undefined}>
                    {subject?.active ? 'active' : 'retired'}
                  </Badge>
                  <Badge>#26 and #27 change this</Badge>
                </td></tr>
            </tbody>
          </table>
        </div>

        <div className="field-grid">
          <Field
            label="Name"
            hint="Blank is refused, not a clear — the model requires a name."
            error={errors.name}
          >
            <Input value={current.name} error={errors.name} onChange={set('name')} />
          </Field>
          <Field label="Short name" hint='Blank clears it.' error={errors.shortName}>
            <Input value={current.shortName} error={errors.shortName}
              onChange={set('shortName')} />
          </Field>
        </div>

        <div className="field-grid">
          <Field label="Type" hint="Five, exhaustive. It cannot be cleared." error={errors.subjectType}>
            <Select label="Type" value={current.subjectType}
              onChange={setValue('subjectType')} options={TYPES} />
          </Field>
          <Field
            label="Grading scheme id"
            hint="Blank clears it, falling back to the exam's scheme — which is not 'no grading'."
            error={errors.gradingSchemeDocsId}
          >
            <Input value={current.gradingSchemeDocsId} error={errors.gradingSchemeDocsId}
              onChange={set('gradingSchemeDocsId')} />
          </Field>
        </div>

        <p className="muted">
          <Info size={12} /> Only the boxes you change are sent — the panel above is the whole
          body. Change nothing and it sends <span className="mono">{'{}'}</span>, which is a
          400 <span className="mono">NOTHING_TO_UPDATE</span> rather than a quiet success.
        </p>
      </div>
    </Modal>
  )
}
