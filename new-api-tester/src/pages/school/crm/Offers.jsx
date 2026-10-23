import { useCallback, useEffect, useState } from 'react'
import { Info, RefreshCw } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from './admissionDates.js'
import { detailPath } from '../../../paths.js'

/**
 * #32 — the chase list. /school-crm/offers
 *
 * WHY THIS HAS A SCREEN WHEN #28'S QUEUE DID NOT. The review queue was removed because it showed
 * what an application's own page already showed. This does not: it crosses every application at
 * once and answers "what runs out this week", which no single form can.
 *
 * EXPIRED IS THE INTERESTING FILTER, and it is not "has a past date". An offer a family accepted
 * last month has one of those too. So it is past its date AND still ISSUED — and the screen says
 * so, because the difference is the whole reason the filter exists.
 *
 * NOTHING WRITES EXPIRED. A lapsed offer still reads ISSUED and only the clock knows, which is why
 * a row can say ISSUED and be shown as lapsed at the same time.
 *
 * A ROW OPENS THE APPLICATION, not the offer. There is no GET /offers/{id} and the plan never had
 * one — an offer is answered and withdrawn from the form it belongs to, where its one letter lives.
 */
const STATUSES = ['', 'ISSUED', 'ACCEPTED', 'DECLINED', 'WITHDRAWN', 'EXPIRED', 'SUPERSEDED',
  'DRAFT']
const TONE = { ACCEPTED: 'good', ISSUED: 'warn', DECLINED: 'bad', WITHDRAWN: 'bad' }

export default function Offers() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [filters, setFilters] = useState({ status: '', expired: '', expiringBefore: '' })

  const query = {
    page, size: 20,
    ...(filters.status ? { status: filters.status } : {}),
    ...(filters.expired ? { expired: filters.expired } : {}),
    ...(filters.expiringBefore
      ? { expiringBefore: new Date(filters.expiringBefore).toISOString() }
      : {}),
  }

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-admission-offers', { label: 'The chase list', query })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, page, filters.status, filters.expired,
    filters.expiringBefore])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Offers" />

  const rows = data?.content ?? []
  const lapsed = (one) => one.status === 'ISSUED' && one.expiresAt
    && new Date(one.expiresAt) < new Date()

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Offers</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {' · what is expiring, soonest first'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="list-admission-offers" name="List" query={query} />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card title={problem.bodyJson?.code ?? `The server answered ${problem.status}`}>
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
        </Card>
      ) : null}

      <Card
        title="Narrow it"
        description="status and expiringBefore are the two keys of school_offer_status_expiry_idx, in its order — together they are this week's phone calls."
        action={<Badge>{data?.totalElements ?? 0} offers</Badge>}
      >
        <div className="field-grid">
          <Field label="Status" hint="EXPIRED and SUPERSEDED and DRAFT are offered and will match nothing: nothing writes them. That is worth seeing rather than being told.">
            <Select
              value={filters.status}
              options={STATUSES.map((one) => ({ value: one, label: one === '' ? 'any' : one }))}
              label="Status"
              onChange={(v) => { setPage(0); setFilters((f) => ({ ...f, status: v })) }}
            />
          </Field>
          <Field label="Lapsed" hint="NOT 'has a past date'. Past its date AND still ISSUED — an offer a family accepted last month has a past date too, and nobody needs chasing about it.">
            <Select
              value={filters.expired}
              options={[
                { value: '', label: 'either' },
                { value: 'true', label: 'lapsed — past its date and still out' },
                { value: 'false', label: 'not lapsed — answered ones included' },
              ]}
              label="Lapsed"
              onChange={(v) => { setPage(0); setFilters((f) => ({ ...f, expired: v })) }}
            />
          </Field>
          <Field label="Lapsing before" hint="The window: who do I ring this week. It says nothing about status, so it returns answered offers too unless you narrow that as well.">
            <Input type="datetime-local" value={filters.expiringBefore}
              onChange={(e) => {
                setPage(0)
                setFilters((f) => ({ ...f, expiringBefore: e.target.value }))
              }} />
          </Field>
        </div>
      </Card>

      <Card
        title="What is out there"
        description="Soonest to lapse first, then by id — the id is the tiebreaker because an offer number is only unique per school, and a fallback order has to be total or a row is shown twice while another is never shown."
      >
        {rows.length === 0 ? (
          <Empty
            title="Nothing matches"
            description="An offer is created by #29 against an approved application. With one letter per admission there is at most one row per form."
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Offer no</th>
                  <th>Applicant</th>
                  <th>Class</th>
                  <th>Status</th>
                  <th>Lapses</th>
                  <th>Answered</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  <tr key={one.admissionOfferId}
                    onClick={() => navigate(detailPath('school', 'crm', 'applications',
                      one.admissionApplicationDocsId))}>
                    <td className="mono">{one.offerNo}</td>
                    <td>
                      {one.applicantName ?? <span className="muted">form is gone</span>}
                      <br />
                      <span className="mono muted">{one.applicationNo}</span>
                    </td>
                    <td>{one.offeredClassName ?? <span className="muted">class is gone</span>}</td>
                    <td>
                      <Badge tone={TONE[one.status]}>{one.status}</Badge>
                      {lapsed(one) ? <> <Badge tone="bad">lapsed</Badge></> : null}
                    </td>
                    <td title={one.expiresAt}>
                      {one.expiresAt
                        ? readable(one.expiresAt)
                        : <span className="muted">no date</span>}
                    </td>
                    <td title={one.respondedAt}>
                      {one.respondedAt
                        ? `${one.response} · ${readable(one.respondedAt)}`
                        : <span className="muted">not yet</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="toolbar">
          <Button onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</Button>
          <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          <span className="toolbar-spacer" />
          <Badge>page {(data?.page ?? 0) + 1} of {Math.max(1, data?.totalPages ?? 1)}</Badge>
        </div>

        <p className="muted">
          <Info size={12} /> <b>A row marked <span className="mono">ISSUED</span> can still be
          lapsed.</b> Nothing writes <span className="mono">EXPIRED</span> — a date in the past is
          what it means — so the stored status says <span className="mono">ISSUED</span> and only
          the clock knows. #30 refuses one anyway, which is what this list exists to prevent.
        </p>
      </Card>
    </div>
  )
}
