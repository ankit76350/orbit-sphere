import { useCallback, useEffect, useMemo, useState } from 'react'
import { ChevronRight, Plus, RefreshCw, Search } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath } from '../../../paths.js'

/**
 * The school's rooms: /school-core/rooms
 *
 * TWO ENDPOINTS — room #2 lists them (filters, sort, paging) and #1 adds one. A room belongs to
 * the school, not to a year, so the navbar's year does not matter here. Opening a row is #3.
 *
 * NOTHING IS DISABLED — a name already used (any case) is 409 ROOM_NAME_TAKEN, a capacity below 1
 * is 400 VALIDATION_FAILED, and an unknown sort field is 400 INVALID_SORT_FIELD.
 */

const ROOM_TYPES = ['CLASSROOM', 'LABORATORY', 'HALL', 'LIBRARY', 'PLAYGROUND', 'OFFICE', 'OTHER']
const STATUSES = ['IN_USE', 'UNDER_MAINTENANCE', 'CLOSED']
const SORTS = ['', 'name', 'name,desc', 'capacity', 'capacity,desc', 'createdAt,desc']

export default function Rooms() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()

  const [resourceType, setResourceType] = useState('')
  const [status, setStatus] = useState('')
  const [minCapacity, setMinCapacity] = useState('')
  //! Typed, then SENT. `search` is what the last request used; `typed` is what the box holds.
  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [open, setOpen] = useState(false)

  //! THE QUERY #2 IS SENT. An empty filter sends nothing at all.
  const query = useMemo(() => {
    const out = { page: String(page), size: '20' }
    if (resourceType) out.resourceType = resourceType
    if (status) out.status = status
    if (minCapacity) out.minCapacity = minCapacity
    if (search.trim()) out.search = search.trim()
    if (sort) out.sort = sort
    return out
  }, [page, resourceType, status, minCapacity, search, sort])

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const answer = await call('list-rooms', { label: "The school's rooms", query })
    setLoading(false)
    if (answer.ok) { setData(answer.bodyJson); setProblem(null) } else { setData(null); setProblem(answer) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, query])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Rooms" />

  const rows = data?.content ?? []
  const runSearch = () => { setPage(0); setSearch(typed) }

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Rooms</h1>
          <p className="muted">The school's classrooms, labs, hall and other spaces — where periods and exam papers happen.</p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="list-rooms" name="Rooms" query={query} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add a room</Button>
      </div>

      <Card title="Filters" description="All optional. Empty sends nothing.">
        <div className="field-grid">
          <Field label="Type">
            <Select value={resourceType} options={['', ...ROOM_TYPES]} onChange={(v) => { setPage(0); setResourceType(v) }} />
          </Field>
          <Field label="Status">
            <Select value={status} options={['', ...STATUSES]} onChange={(v) => { setPage(0); setStatus(v) }} />
          </Field>
          <Field label="Seats at least" hint="Rooms that can take this many — for an exam.">
            <Input value={minCapacity} placeholder="60" onChange={(e) => { setPage(0); setMinCapacity(e.target.value) }} />
          </Field>
          <Field label="Name or location contains" hint="Ignoring case.">
            <div className="picker-pair">
              <Input value={typed} placeholder="science" onChange={(e) => setTyped(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') runSearch() }} />
              <Button icon={Search} onClick={runSearch}>Search</Button>
            </div>
          </Field>
          <Field label="Sort" hint="Any other field is 400 INVALID_SORT_FIELD.">
            <Select value={sort} options={SORTS} onChange={(v) => { setPage(0); setSort(v) }} />
          </Field>
        </div>
      </Card>

      <Card title={data ? `${data.totalElements} room${data.totalElements === 1 ? '' : 's'}` : 'Rooms'}>
        {problem ? (
          <>
            <div className="resp-head"><span className="resp-status" data-ok="false">{problem.bodyJson?.code ?? problem.status}</span></div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </>
        ) : rows.length === 0 && data ? (
          <Empty title="No rooms" description="No room matches. Add one with the button above." />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr><th>Name</th><th>Type</th><th>Location</th><th>Seats</th><th>Status</th><th /></tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  <tr key={one.roomDocsId}>
                    <td><b>{one.name}</b><div className="mono muted">{one.roomDocsId}</div></td>
                    <td>{one.resourceType}</td>
                    <td>{one.location ?? <span className="muted">—</span>}</td>
                    <td>{one.capacity ?? <span className="muted">not counted</span>}</td>
                    <td><Badge tone={one.status === 'IN_USE' ? 'good' : 'warn'}>{one.status}</Badge></td>
                    <td>
                      <Button icon={ChevronRight} onClick={() => navigate(detailPath('school', 'core', 'rooms', one.roomDocsId))}>Open</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {data && data.totalPages > 1 ? (
          <div className="toolbar">
            <Button onClick={() => setPage(page - 1)}>Previous</Button>
            <span className="muted">Page {data.page + 1} of {data.totalPages}</span>
            <Button onClick={() => setPage(page + 1)}>Next</Button>
          </div>
        ) : null}
      </Card>

      {open ? <AddRoom onClose={() => setOpen(false)} onAdded={() => { setOpen(false); load() }} /> : null}
    </div>
  )
}

/**
 * Adding one room — #1. The boxes on one side, what will be sent on the other.
 */
function AddRoom({ onClose, onAdded }) {
  const { call } = useApi()
  const [name, setName] = useState('')
  const [resourceType, setResourceType] = useState('CLASSROOM')
  const [location, setLocation] = useState('')
  const [capacity, setCapacity] = useState('')
  const [remarks, setRemarks] = useState('')
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  // An empty optional box sends nothing rather than "".
  const body = {
    name,
    resourceType,
    ...(location ? { location } : {}),
    ...(capacity !== '' ? { capacity: Number(capacity) } : {}),
    ...(remarks ? { remarks } : {}),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('create-room', { label: 'Add a room', body })
    setSending(false)
    setResult(answer)
    if (answer.ok) onAdded(answer.bodyJson)
  }

  return (
    <Modal
      open
      onClose={onClose}
      title="Add a room"
      description="It starts as IN_USE. The name must be free in the school, ignoring case."
      endpoint={<EndpointTag id="create-room" name="Add" look="primary" />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Add</Button>}
    >
      <div className="field-grid">
        <Field label="Name" required hint="Unique in the school, ignoring case. A closed room keeps its name.">
          <Input value={name} placeholder="Room 12" onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Type" required>
          <Select value={resourceType} options={ROOM_TYPES} onChange={setResourceType} />
        </Field>
        <Field label="Location" hint="Optional. Where it is, in words.">
          <Input value={location} placeholder="Science Block, first floor" onChange={(e) => setLocation(e.target.value)} />
        </Field>
        <Field label="Seats" hint="Optional; at least 1. Exam papers are checked against it.">
          <Input value={capacity} placeholder="40" onChange={(e) => setCapacity(e.target.value)} />
        </Field>
        <Field label="Remarks" hint="Optional.">
          <Input value={remarks} placeholder="Projector on the left wall." onChange={(e) => setRemarks(e.target.value)} />
        </Field>
      </div>
      {result && !result.ok ? (
        <div className="resp">
          <div className="resp-head"><span className="resp-status" data-ok="false">{result.bodyJson?.code ?? result.status}</span></div>
          <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
        </div>
      ) : null}
    </Modal>
  )
}
