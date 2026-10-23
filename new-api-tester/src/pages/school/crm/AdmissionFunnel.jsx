import { useCallback, useEffect, useState } from 'react'
import { Info, RefreshCw } from 'lucide-react'
import { useApi } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card } from '../../../components/ui/Kit.jsx'
import Meter from '../../../components/ui/Meter.jsx'

/**
 * #34 — the admissions funnel for one cycle, as a card on the cycle's page.
 *
 * THE BARS ARE AGAINST THE TOP OF THE FUNNEL (leads), so each one reads as "how many of the
 * people who asked got this far". The number is always printed beside the bar.
 *
 * THE THREE STATUS TABLES ARE THE RAW COUNTS behind the stages, every status listed — a stage
 * that looks wrong can be checked against them without a second request.
 */
export default function AdmissionFunnel({ cycle }) {
  const { call } = useApi()
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    const result = await call('get-admission-funnel', {
      label: 'The admissions funnel',
      pathParams: { admissionCycleId: cycle.admissionCycleId },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, cycle.admissionCycleId])

  useEffect(() => { load() }, [load])

  const stages = data?.stages ?? []
  const top = stages[0]?.count ?? 0

  const statusTable = (title, counts) => (
    <div className="table-scroll">
      <table className="data-table">
        <thead><tr><th>{title}</th><th className="num">Count</th></tr></thead>
        <tbody>
          {Object.entries(counts ?? {}).map(([status, count]) => (
            <tr key={status}>
              <td className={count ? undefined : 'muted'}>{status}</td>
              <td className="num">{count ? <b>{count}</b> : <span className="muted">0</span>}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )

  return (
    <Card
      title="The admissions funnel"
      description="Leads, applications, offers and enrolments for this round — three counts, one per collection, never stored."
      action={
        <div className="btn-row">
          <EndpointTag id="get-admission-funnel" name="Funnel"
            pathParams={{ admissionCycleId: cycle.admissionCycleId }} />
          <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        </div>
      }
    >
      {problem ? (
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok="false">
              {problem.bodyJson?.code ?? problem.status}
            </span>
          </div>
          <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
        </div>
      ) : data ? (
        <>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Stage</th>
                  <th className="num">Count</th>
                  <th style={{ minWidth: 160 }}></th>
                </tr>
              </thead>
              <tbody>
                {stages.map((s) => (
                  <tr key={s.key}>
                    <td>{s.label}</td>
                    <td className="num"><b>{s.count}</b></td>
                    <td>{top ? <Meter value={s.count} max={top} /> : null}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <p className="muted">
            <Info size={12} /> <b>Leads are counted by year, not by round.</b> An inquiry names an
            academic year, so every round in <b>{data.academicYear}</b> shows the same leads.{' '}
            <b>The offer steps count applications, not letters</b> — an offer withdrawn and
            re-issued counts once. The Offers table below counts every letter.
          </p>

          <div className="field-grid">
            {statusTable('Inquiries', data.inquiriesByStatus)}
            {statusTable('Applications', data.applicationsByStatus)}
            {statusTable('Offers', data.offersByStatus)}
          </div>

          {data.note ? <p className="muted"><Badge>note</Badge> {data.note}</p> : null}
        </>
      ) : null}
    </Card>
  )
}
