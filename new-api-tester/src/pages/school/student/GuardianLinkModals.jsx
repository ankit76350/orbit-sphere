import { useState } from 'react'
import { Info } from 'lucide-react'
import { useApi } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import { RELATIONS } from './relations.js'


const FLAGS = [
  ['primaryContact', 'Primary contact', 'True MOVES it here — the current primary stops being primary. False on the current primary is 409 LAST_PRIMARY_CONTACT.'],
  ['emergencyContact', 'Emergency contact', 'May be contacted in an emergency.'],
  ['pickupAuthorized', 'May collect', 'Allowed to collect the child.'],
  ['portalAccess', 'Portal access', 'May have guardian-portal access.'],
]

/** One answer block, shared by both modals. */
function Answer({ result, okText }) {
  if (!result) return null
  return (
    <div className="resp">
      <div className="resp-head">
        <span className="resp-status" data-ok={result.ok ? 'true' : 'false'}>
          {result.ok ? `${result.status} OK` : (result.bodyJson?.code ?? result.status)}
        </span>
      </div>
      {result.ok
        ? <pre className="resp-body">{result.bodyJson?.nextStep ?? okText}</pre>
        : <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>}
    </div>
  )
}

/**
 * #12 — change how one guardian is linked to this child: the relation and the four flags.
 *
 * SEEDED FROM THE ROW, and only what moved is sent. An untouched form still sends, which is
 * 400 NOTHING_TO_UPDATE — nothing is disabled.
 *
 * THE VERSION IS THE STUDENT'S, because the link lives inside the student.
 */
function ChangeGuardianLink({ child, guardian, onClose, onChanged }) {
  const { call } = useApi()
  const seed = {
    relation: guardian.relation ?? '',
    primaryContact: Boolean(guardian.primaryContact),
    emergencyContact: Boolean(guardian.emergencyContact),
    pickupAuthorized: Boolean(guardian.pickupAuthorized),
    portalAccess: Boolean(guardian.portalAccess),
  }
  const [form, setForm] = useState(seed)
  const [version, setVersion] = useState(String(child.version ?? 0))
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! ONLY WHAT MOVED. A box left as it was is left out of the body.
  const body = {
    ...(form.relation !== seed.relation ? { relation: form.relation } : {}),
    ...Object.fromEntries(FLAGS
      .filter(([key]) => form[key] !== seed[key])
      .map(([key]) => [key, form[key]])),
    version: version === '' ? undefined : Number(version),
  }

  const pathParams = { studentDocsId: child.studentDocsId, guardianDocsId: guardian.guardianDocsId }

  const send = async () => {
    setSending(true)
    const answer = await call('update-student-guardian', {
      label: `Change ${guardian.fullName} on ${child.fullName}`,
      pathParams,
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok) onChanged()
  }

  const moves = Object.keys(body).filter((k) => k !== 'version').length

  return (
    <Modal
      open
      onClose={onClose}
      title={`${guardian.fullName} — for ${child.fullName}`}
      description="#12 — this child's link only. The guardian's own record and their links to other children are not touched."
      endpoint={<EndpointTag id="update-student-guardian" name="Change" look="primary"
        pathParams={pathParams} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Send it</Button>}
    >
      <div className="field-grid">
        <Field label="Relation" hint="What this guardian is to THIS child.">
          <Select value={form.relation} options={RELATIONS}
            onChange={(v) => setForm({ ...form, relation: v })} />
        </Field>
        <Field label="The STUDENT'S version" required
          hint="Seeded from the read behind this modal. An older number reaches 409 CONCURRENT_MODIFICATION.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>

      {FLAGS.map(([key, label, hint]) => (
        <label key={key} className="muted" title={hint}>
          <input type="checkbox" checked={form[key]}
            onChange={(e) => setForm({ ...form, [key]: e.target.checked })} />{' '}
          <b>{label}</b> — {hint}
        </label>
      ))}

      {moves === 0 ? (
        <p className="muted">
          <Info size={12} /> <b>Nothing has been changed.</b> Sending this is{' '}
          <span className="mono">400 NOTHING_TO_UPDATE</span>.
        </p>
      ) : null}

      {seed.primaryContact && !form.primaryContact ? (
        <p className="muted">
          <Info size={12} /> <b>This guardian is the primary contact.</b> Unticking it is{' '}
          <span className="mono">409 LAST_PRIMARY_CONTACT</span> — make another guardian primary
          instead, which moves it.
        </p>
      ) : null}

      <Answer result={result} />
    </Modal>
  )
}

/**
 * #13 — detach one guardian from this child. 204, no body.
 *
 * THE GUARDIAN IS NEVER DELETED — only the link on this child goes. Detaching the primary is
 * 409 LAST_PRIMARY_CONTACT, and the modal says so rather than stopping you sending it.
 */
function DetachGuardian({ child, guardian, onClose, onDetached }) {
  const { call } = useApi()
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  const pathParams = { studentDocsId: child.studentDocsId, guardianDocsId: guardian.guardianDocsId }

  const send = async () => {
    setSending(true)
    const answer = await call('unlink-student-guardian', {
      label: `Detach ${guardian.fullName} from ${child.fullName}`,
      pathParams,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok) onDetached()
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={`Detach ${guardian.fullName}`}
      description={`#13 — take ${guardian.fullName} off ${child.fullName}. The guardian is not deleted.`}
      endpoint={<EndpointTag id="unlink-student-guardian" name="Detach" look="primary"
        pathParams={pathParams} />}
      footer={<Button look="primary" onClick={send} busy={sending}>Detach</Button>}
    >
      <p className="muted">
        {guardian.relation}{' '}
        {guardian.primaryContact ? <Badge tone="good">primary</Badge> : null}{' '}
        <span className="mono muted">{guardian.guardianDocsId}</span>
      </p>
      <p className="muted">
        <Info size={12} /> <b>Only the link goes.</b> {guardian.fullName} stays on the school&rsquo;s
        guardian list and on any other child they are linked to.
      </p>
      {guardian.primaryContact ? (
        <p className="muted">
          <Info size={12} /> <b>This is the primary contact.</b> Detaching is{' '}
          <span className="mono">409 LAST_PRIMARY_CONTACT</span> — make another guardian primary
          with Change first.
        </p>
      ) : null}
      <Answer result={result} okText="204 — detached. The guardian list behind this has been re-read." />
    </Modal>
  )
}

export { ChangeGuardianLink, DetachGuardian }
