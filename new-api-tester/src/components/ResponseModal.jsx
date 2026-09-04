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
      description={`${method} ${path}`}
    >
      <div className="stack">
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok={ok ? 'true' : 'false'}>
              {status ? `${status}` : 'no reply'}
            </span>
            <span>{durationMs} ms</span>
            {result?.sizeBytes ? <span>{result.sizeBytes} bytes</span> : null}
            <span className="toolbar-spacer" />
            <Badge tone={ok ? 'good' : 'bad'}>{ok ? 'ok' : 'failed'}</Badge>
          </div>
          <pre className="resp-body">{body(result?.bodyText)}</pre>
        </div>

        {/* The request is second: when something is wrong the answer is usually in the reply,
            and the thing that was sent is what you check next. */}
        {result?.request?.body ? (
          <div className="resp">
            <div className="resp-head"><span>what was sent</span></div>
            <pre className="resp-body">{body(result.request.body)}</pre>
          </div>
        ) : null}

        <p className="muted">{result?.request?.url}</p>
      </div>
    </Modal>
  )
}
