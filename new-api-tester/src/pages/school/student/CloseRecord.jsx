import { useState } from 'react'
import { Info } from 'lucide-react'
import { useApi } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'

/**
 * #16 — close one academic record: COMPLETED, TRANSFERRED or CANCELLED.
 *
 * SEEDED FROM THE ROW that was clicked — its year, its id (in the path) and its version.
 *
 * NOTHING IS DISABLED. ACTIVE is offered as a status so 400 INVALID_CLOSE_STATUS can be reached,
 * and a closed row still sends, which is 409 RECORD_NOT_OPEN.
 */
export default function CloseRecord({ child, row, onClose, onClosed }) {
  const { call } = useApi()
  const [year, setYear] = useState(row.academicYear ?? '')
  const [recordId, setRecordId] = useState(row.academicRecordDocsId ?? '')
  const [status, setStatus] = useState('COMPLETED')
  const [version, setVersion] = useState(String(row.version ?? 0))
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! NO END DATE IS SENT — the server sets it to the day the record is closed.
  const body = {
    ...(status ? { status } : {}),
    version: version === '' ? undefined : Number(version),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('close-student-record', {
      label: `Close a record of ${child.fullName}`,
      pathParams: { year, academicRecordDocsId: recordId },
      body,
    })
    setSending(false)
    setResult(answer)
    if (answer.ok) onClosed()
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={`Close a record of ${child.fullName}`}
      description="#16 — end a placement. The server sets the end date to today; the record is kept in the history, and if it was the child's current record, that is cleared."
      endpoint={<EndpointTag id="close-student-record" name="Close" look="primary"
        pathParams={{ year, academicRecordDocsId: recordId }} />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Send it</Button>}
    >
      <div className="table-scroll">
        <table className="data-table">
          <tbody>
            <tr><th>The row you are closing</th>
              <td><b>{row.className ?? 'class deleted'}</b> {row.sectionNo}
                {row.rollNo ? <> · roll <span className="mono">{row.rollNo}</span></> : null}
                {' '}· from {row.effectiveFrom}{' '}
                <Badge tone={row.status === 'ACTIVE' ? 'good' : undefined}>{row.status}</Badge>
                {row.current ? <> <Badge tone="good">current</Badge></> : null}
              </td></tr>
          </tbody>
        </table>
      </div>

      {row.status !== 'ACTIVE' ? (
        <p className="muted">
          <Info size={12} /> <b>This row is already closed ({row.status}).</b> A closed record is
          final — sending this is <span className="mono">409 RECORD_NOT_OPEN</span>.
        </p>
      ) : null}

      <p className="muted">
        <span className="mono">POST /schools/current/academic-years/</span>
        <b className="mono">{year || '{year}'}</b>
        <span className="mono">/student-records/</span>
        <b className="mono">{recordId || '{academicRecordDocsId}'}</b>
        <span className="mono">/close</span>
      </p>

      <div className="field-grid">
        <Field label="Academic year" required
          hint="THE NAME, not an id. Seeded from the row. The record must belong to it, or it is 400 RECORD_NOT_IN_YEAR.">
          <Input value={year} placeholder="2026-2027"
            onChange={(e) => setYear(e.target.value)} />
        </Field>
        <Field label="Academic record id" required
          hint="IN THE PATH — the record to close, seeded from the row.">
          <Input value={recordId} onChange={(e) => setRecordId(e.target.value)} />
        </Field>
        <Field label="How it ended" required
          hint="COMPLETED — the year is over. TRANSFERRED — the child left (a move inside this school is Transfer). CANCELLED — it should never have been made. ACTIVE is 400 INVALID_CLOSE_STATUS; empty is 400 VALIDATION_FAILED.">
          <Select value={status}
            options={['', 'COMPLETED', 'TRANSFERRED', 'CANCELLED', 'ACTIVE']}
            onChange={setStatus} />
        </Field>
        <Field label="The RECORD'S version" required
          hint="Seeded from the row. An older number reaches 409 CONCURRENT_MODIFICATION.">
          <Input value={version} onChange={(e) => setVersion(e.target.value)} />
        </Field>
      </div>

      {result ? (
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok={result.ok ? 'true' : 'false'}>
              {result.ok ? `${result.status} OK` : (result.bodyJson?.code ?? result.status)}
            </span>
          </div>
          {result.ok ? (
            <>
              <p className="muted">
                <b>{result.bodyJson?.className} {result.bodyJson?.sectionNo}</b> is now{' '}
                <b>{result.bodyJson?.status}</b>, ending {result.bodyJson?.effectiveUntil}. The
                table behind this modal has been re-read — the row is still there.
              </p>
              <pre className="resp-body">{result.bodyJson?.nextStep}</pre>
            </>
          ) : (
            <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
          )}
        </div>
      ) : null}
    </Modal>
  )
}
