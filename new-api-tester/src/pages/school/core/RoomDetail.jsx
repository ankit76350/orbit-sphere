import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { screenPath } from '../../../paths.js'
import { readableDateTime } from '../../../lib/dates.js'

/**
 * One room, on its own page: /school-core/rooms/{roomId} — room #3.
 */
export default function RoomDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [room, setRoom] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const answer = await call('get-room', { label: 'One room', pathParams: { roomDocsId: id ?? '' } })
    setLoading(false)
    if (answer.ok) { setRoom(answer.bodyJson); setProblem(null) } else { setRoom(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Rooms" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{room?.name ?? 'Room'}</h1>
          <p className="muted">{room ? room.resourceType : id}</p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={() => navigate(screenPath('school', 'core', 'rooms'))}>Rooms</Button>
        <EndpointTag id="get-room" name="Room" pathParams={{ roomDocsId: id }} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </Card>
      ) : null}

      {room ? (
        <Card title="The room">
          <div className="table-scroll">
            <table className="data-table">
              <tbody>
                <tr><th>Name</th><td><b>{room.name}</b></td></tr>
                <tr><th>Type</th><td>{room.resourceType}</td></tr>
                <tr><th>Location</th><td>{room.location ?? <span className="muted">—</span>}</td></tr>
                <tr><th>Seats</th><td>{room.capacity ?? <span className="muted">not counted</span>}</td></tr>
                <tr><th>Status</th><td><Badge tone={room.status === 'IN_USE' ? 'good' : 'warn'}>{room.status}</Badge></td></tr>
                <tr><th>Remarks</th><td>{room.remarks ?? <span className="muted">—</span>}</td></tr>
                <tr><th>Added</th><td>{readableDateTime(room.createdAt)}</td></tr>
                <tr><th>Version</th><td className="mono">{room.version}</td></tr>
                <tr><th>Id</th><td className="mono muted">{room.roomDocsId}</td></tr>
              </tbody>
            </table>
          </div>
        </Card>
      ) : null}
    </div>
  )
}
