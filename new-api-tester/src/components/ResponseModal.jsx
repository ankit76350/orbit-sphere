import { useApi, useApiState } from '../api/apiContext.js'
import { Badge, Modal } from './ui/Kit.jsx'

/** Pretty-printed if it is JSON, and left exactly as it came if it is not. */
function body(text) {
  if (!text) return '(empty)'
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

/**
 * The request and the response of one call, in full.
 *
 * It opens by itself after anything that changes something — a POST, PUT, PATCH or DELETE —
 * because the answer to "did that work" should not need a second click. Reads stay quiet: they
 * run on page load, and the screen shows its own message when one fails.
 *
 * Lives in the shell rather than on each screen so every call can be inspected from wherever it
 * was made, and no screen has to remember to render it.
 */
export default function ResponseModal() {
  const { inspect } = useApi()
  const { inspecting } = useApiState()
  if (!inspecting) return null

  const { result, method, path, status, ok, durationMs, action, endpointName } = inspecting

  return (
    <Modal
      open
      onClose={() => inspect(null)}
      title={action || endpointName || `${method} ${path}`}
      /* The URL that was actually sent, base address and query string included — not the
         template. It is the one thing here that cannot be worked out from anything else on
         screen, so it goes in the heading rather than under the request body where it was. */
      endpoint={
        <>
          <span className="endpoint-tag-method" data-method={method}>{method}</span>
          <span className="endpoint-tag-path">{result?.request?.url || path}</span>
        </>
      }
      previewLabel="Response body"
      /* The reply on the right, opposite the request that produced it. Same geometry as the
         form-and-payload view this modal replaces on screen, so the request stays in the place
         the eye already learnt — only the left pane changes, from the form to what it sent. */
      preview={
        <>
          <div className="resp-head" style={{ marginBottom: 10 }}>
            <span className="resp-status" data-ok={ok ? 'true' : 'false'}>
              {status ? `${status}` : 'no reply'}
            </span>
            <span>{durationMs} ms</span>
            {result?.sizeBytes ? <span>{result.sizeBytes} bytes</span> : null}
            <span className="toolbar-spacer" />
            <Badge tone={ok ? 'good' : 'bad'}>{ok ? 'ok' : 'failed'}</Badge>
          </div>
          <pre className="modal-json">{body(result?.bodyText)}</pre>
        </>
      }
    >
      <p className="modal-pane-label">Request body</p>
      <pre
        className="modal-json"
        data-empty={result?.request?.body ? 'false' : 'true'}
      >
        {result?.request?.body ? body(result.request.body) : 'This request sent no body.'}
      </pre>
    </Modal>
  )
}
