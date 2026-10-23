import { useEffect, useState } from 'react'
import { Info } from 'lucide-react'
import { useApi } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Button, Field, Input, Modal } from '../../../components/ui/Kit.jsx'

const BLANK = {
  departmentCode: '',
  name: '',
  description: '',
  parentDepartmentDocsId: '',
  headStaffDocsId: '',
}

/**
 * Adding a department — #9. Two callers, and the parent is what separates them.
 *
 * NO PARENT PROP MEANS A TOP-LEVEL UNIT, and the box is not drawn at all. That is the department
 * list asking, where nesting has no id to nest under — a caller would have to paste one, and a
 * field whose only correct value is empty is a field worth removing.
 *
 * A PARENT PROP MEANS A SUB-DEPARTMENT, and the box IS drawn, pre-filled with the unit whose page
 * asked. Pre-filled, not fixed: an id that is another school's, or nonsense, is how
 * DEPARTMENT_NOT_FOUND is reached, and blank is how a unit is made top-level from here — so every
 * refusal this endpoint documents stays reachable from one screen or the other. No control on
 * either screen is ever greyed out; the box is drawn or it is not.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD, because a school enters its whole org chart in one sitting.
 * The code and name are cleared; the parent is kept, because the next unit is usually a sibling.
 */
export default function AddDepartment({ open, parent, onClose, onAdded }) {
  const { call } = useApi()
  const initial = { ...BLANK, parentDepartmentDocsId: parent?.departmentDocsId ?? '' }
  const [form, setForm] = useState(initial)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  useEffect(() => {
    if (open) { setForm(initial); setErrors({}); setRefused(null); setMade(null) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [open, parent?.departmentDocsId])

  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))

  // An empty optional box sends nothing rather than "", which the API would read as a value.
  const body = (() => {
    const out = { departmentCode: form.departmentCode, name: form.name }
    if (form.description.trim() !== '') out.description = form.description.trim()
    if (form.parentDepartmentDocsId.trim() !== '') {
      out.parentDepartmentDocsId = form.parentDepartmentDocsId.trim()
    }
    if (form.headStaffDocsId.trim() !== '') out.headStaffDocsId = form.headStaffDocsId.trim()
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-department', { label: 'Add a department', body })
    setSaving(false)
    if (result.ok) {
      setMade(result.bodyJson)
      onAdded(result.bodyJson)
      setForm((old) => ({ ...old, departmentCode: '', name: '', description: '' }))
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
      title={parent?.name ? `Add a sub-department under ${parent.name}` : 'Add a department'}
      description="The code is given, never derived from the name — they move independently, and twenty positions may reference the code."
      endpoint={<EndpointTag id="create-department" name="Add" look="primary" />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Add</Button>
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

        {made ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">{made.departmentCode}</span>
            </div>
            <pre className="resp-body">{made.nextStep}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Department code"
            required
            hint="Given, never derived. Stored trimmed and UPPER-CASED, so 'admin' and 'ADMIN' are one code."
            error={errors.departmentCode}
          >
            <Input value={form.departmentCode} error={errors.departmentCode}
              onChange={set('departmentCode')} placeholder="ACADEMICS" />
          </Field>
          <Field
            label="Name"
            required
            hint="What a person reads. Two units may share a name — only the code is unique."
            error={errors.name}
          >
            <Input value={form.name} error={errors.name}
              onChange={set('name')} placeholder="Academic Department" />
          </Field>
        </div>

        <Field label="Description" hint="Optional free text." error={errors.description}>
          <Input value={form.description} error={errors.description}
            onChange={set('description')} placeholder="Curriculum and teaching operations." />
        </Field>

        <div className="field-grid">
          {/* DRAWN ONLY WHEN A UNIT ASKED. From the list there is nothing to nest under, so the
              only correct value is empty — and an empty box is a question nobody can answer. */}
          {parent ? (
            <Field
              label="Parent department id"
              hint="Pre-filled with the unit you opened this from. A department of THIS school — another school's real id is a 404, and blank makes this one top-level."
              error={errors.parentDepartmentDocsId}
            >
              <Input value={form.parentDepartmentDocsId} error={errors.parentDepartmentDocsId}
                onChange={set('parentDepartmentDocsId')} placeholder="blank makes it top level" />
            </Field>
          ) : null}
          <Field
            label="Head staff id"
            hint="Optional Staff.id. Checked to EXIST, not to be employed — a school enters its org chart before its employment records."
            error={errors.headStaffDocsId}
          >
            <Input value={form.headStaffDocsId} error={errors.headStaffDocsId}
              onChange={set('headStaffDocsId')} placeholder="67aa15d9dc3f7d0011111111" />
          </Field>
        </div>

        <p className="muted">
          <Info size={12} /> <span className="mono">active</span> is not accepted — a unit starts
          active and retiring is #11. Sending it is ignored rather than refused, which is the
          ordinary shape for a field the request record does not declare.
        </p>
      </div>
    </Modal>
  )
}
