import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Info, RefreshCw } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { compact, readable, zoneLabel } from './admissionDates.js'
import { screenPath } from '../../../paths.js'

/**
 * One admission cycle: /school-crm/admission-cycles/{id}
 *
 * ONE ENDPOINT — #6. It exists because two things are on it that a list row cannot carry: the
 * notes, and the seat table itself rather than a count of it.
 *
 * THE SEAT TABLE IS THE REASON THIS PAGE EXISTS, and today it is almost always empty — #4 sets
 * the seats and is not built, so every cycle reads back with none. The page says that rather than
 * showing a bare empty state, because "no seats" and "no endpoint to add seats" look identical
 * and only one of them is something the person can act on.
 *
 * A SEAT ROW WITH NO CLASS NAME IS SHOWN LOUDLY. #6 resolves the names and leaves one absent when
 * the class is gone; the row stays, and this page marks it. That is a cycle holding seats for a
 * class the school no longer has — a real problem, and hiding it is what a tidier screen would do.
 *
 * THE COUNTS COME FROM THE SERVER, not from counting the rows here. capacityCount and totalSeats
 * are both on #6. Recomputing them in the browser would mean two answers that can disagree, and
 * the one on screen would be the one nobody could check.
 *
 * NOTHING IS DISABLED. Refresh always sends, and an id that is not this school's is a documented
 * 404 worth being able to reach by editing the address bar.
 */

const STATUS_TONE = {
  OPEN: 'good',
  SCHEDULED: 'warn',
  CANCELLED: 'bad',
}

export default function AdmissionCycleDetail() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const { id } = useParams()

  const [cycle, setCycle] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-admission-cycle', {
      label: 'One admission cycle in full',
      pathParams: { admissionCycleId: id ?? '' },
    })
    setLoading(false)
    if (result.ok) { setCycle(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  const back = () => navigate(screenPath('school', 'crm', 'admission-cycles'))

  if (!actingSubdomain) return <NoSchoolChosen what="An admission cycle" />

  const seats = cycle?.capacities ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{cycle?.name ?? 'Admission cycle'}</h1>
          <p className="muted">
            <span className="mono">{id}</span>
            {cycle ? ` · ${cycle.academicYear}` : ' · reading the cycle…'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={ArrowLeft} onClick={back}>All rounds</Button>
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card
          title="Could not read it"
          action={<EndpointTag id="get-admission-cycle" name="Get" />}
        >
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
          <p className="muted">
            <Info size={12} /> A cycle id belongs to one school. <b>Another school&rsquo;s real id
            answers 404 here</b>, not the cycle — the school is part of the lookup rather than
            something checked afterwards.
          </p>
        </Card>
      ) : null}

      {cycle ? (
        <>
          <Card
            title="The round"
            description="What the school set up. Everything here except notes is also on a list row."
            action={<EndpointTag id="get-admission-cycle" name="Get" />}
          >
            <div className="stack">
              <div className="field-grid">
                <div>
                  <p className="muted">Status</p>
                  <Badge tone={STATUS_TONE[cycle.status]}>{cycle.status}</Badge>
                </div>
                <div>
                  <p className="muted">Academic year</p>
                  <p className="mono">{cycle.academicYear}</p>
                </div>
              </div>

              <p className="muted">
                Times shown in <b>{zoneLabel()}</b>. Hover for the exact instant that is stored.
              </p>
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Enquiries open</th>
                      <th>Applications open</th>
                      <th>Applications close</th>
                      <th>Enrollment deadline</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      {['inquiryOpenAt', 'applicationOpenAt', 'applicationCloseAt',
                        'enrollmentDeadlineAt'].map((field) => (
                          <td key={field} title={cycle[field] ?? 'not set'}>
                            {cycle[field]
                              ? compact(cycle[field])
                              : <span className="muted">not set</span>}
                          </td>
                        ))}
                    </tr>
                  </tbody>
                </table>
              </div>

              <div>
                <p className="muted">Notes</p>
                {cycle.notes
                  ? <p>{cycle.notes}</p>
                  : <p className="muted">None. Left out of the response entirely rather than
                    sent as an empty string.</p>}
              </div>

              <p className="muted">
                <Info size={12} /> Created {readable(cycle.createdAt)}, last changed{' '}
                {readable(cycle.updatedAt)}.
              </p>
            </div>
          </Card>

          <Card
            title={`Seats — ${cycle.capacityCount} class${cycle.capacityCount === 1 ? '' : 'es'}, ${cycle.totalSeats} in total`}
            description="What the school configured. NOT how the seats are doing — offered, accepted and free are counted from the applications, and that is #7."
          >
            {seats.length === 0 ? (
              <Empty
                title="No seats set up"
                description={
                  'Which is normal, and not something you can fix here yet: #4 — PUT '
                  + '/admission-cycles/{id}/capacities — is the endpoint that sets the seat table, '
                  + 'and it is not built. Every cycle reads back this way today.'
                }
                action={<Info size={22} aria-hidden="true" />}
              />
            ) : (
              <>
                <div className="table-scroll">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Class</th>
                        <th className="num">Seats</th>
                        <th className="num">Reserved</th>
                        <th>Class id</th>
                      </tr>
                    </thead>
                    <tbody>
                      {seats.map((seat) => (
                        <tr key={seat.classDocsId}>
                          {/* A MISSING NAME IS MARKED, NOT TIDIED AWAY. #6 leaves it absent when
                              the class is gone, and that is a cycle holding seats for a class the
                              school no longer has — worth seeing, not worth hiding. */}
                          <td>
                            {seat.className
                              ? seat.className
                              : <Badge tone="bad" title="#6 could not resolve this class in the cycle's year. It may have been deleted.">
                                no such class
                              </Badge>}
                          </td>
                          <td className="num">{seat.totalSeats}</td>
                          <td className="num">{seat.reservedSeats}</td>
                          <td><span className="mono muted">{seat.classDocsId}</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <p className="muted">
                  <Info size={12} /> <b>The totals come from the server</b>, not from adding these
                  rows up here — two places counting one thing is two answers that can disagree.
                  A row reading <span className="mono">no such class</span> is not a display
                  problem: the cycle really does hold seats for a class this year does not have.
                </p>
              </>
            )}
          </Card>
        </>
      ) : null}
    </div>
  )
}
