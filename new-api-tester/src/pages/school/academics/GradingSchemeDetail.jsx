import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Info, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { screenPath } from '../../../paths.js'

const LIST = screenPath('school', 'academics', 'grading')

/**
 * One grading scheme: /school-academics/grading/:id
 *
 * ONE ENDPOINT — #7, and it is the only one that returns the BANDS. #6 trims them to a count,
 * because a page of full band tables is hundreds of values nobody reads, so this page exists for
 * the one question a list cannot answer: what are this scheme's actual boundaries.
 *
 * THE BANDS ARE IN THE ORDER THEY WERE WRITTEN, never re-sorted — so this table is the thing a
 * school checks against the paper it copied them from. That is the whole point of the endpoint,
 * and it is why the table is not sorted here either.
 *
 * THE WARNING IS RECOMPUTED ON EVERY READ, so it is rendered as a live property of the scheme
 * rather than a one-off notice from the create. Fix the bands and it stops appearing.
 *
 * IT ANSWERS FOR A RETIRED SCHEME, which is why the page never treats inactive as an error.
 */
export default function GradingSchemeDetail() {
  const { id } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-grading-scheme', {
      label: 'One grading scheme',
      pathParams: { id: id ?? '' },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="Grading schemes" />

  const bands = data?.gradeBands ?? []
  // DESCRIPTOR schemes carry no ceiling, so the bound columns have nothing to show.
  const measured = data?.scaleType !== 'DESCRIPTOR'

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">{data?.name ?? 'Grading scheme'}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {data ? <> · <span className="mono">v{data.schemeVersion}</span></> : null}
            {' · '}<span className="mono">{id}</span>
          </p>
        </div>
        <span className="toolbar-spacer" />
        {/* A Link, not a Button — Button renders a <button>, which cannot be an address. */}
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All schemes</Link>
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {problem ? (
        <Card
          title="Not found"
          description="A 404 rather than a 500, even for an id that is not an id — the query matches nothing rather than failing to parse. Another school's REAL id answers the same way."
          action={<EndpointTag id="get-grading-scheme" name="Get" pathParams={{ id }} />}
        >
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? 'Request failed.'}</pre>
          </div>
        </Card>
      ) : null}

      {data ? (
        <>
          <Card
            title="The rulebook"
            description="name + schemeVersion is the key, and neither can be changed. Moving a boundary means a new version — editing in place would rewrite every report card ever issued."
            action={<EndpointTag id="get-grading-scheme" name="Get" pathParams={{ id }} />}
          >
            <div className="toolbar">
              <Badge>{data.scaleType}</Badge>
              {/* Absent on a DESCRIPTOR scheme — refused rather than optional. */}
              {data.maximumValue != null
                ? <Badge>out of {data.maximumValue}</Badge>
                : <Badge>not measured</Badge>}
              <Badge>{data.bandCount} bands</Badge>
              <Badge tone={data.active ? 'good' : undefined}>
                {data.active ? 'active' : 'retired'}
              </Badge>
            </div>

            {/* A retired scheme still resolves every card issued under it, so this is a note
                rather than a warning: active governs what is offered for NEW work only. */}
            {data.active === false ? (
              <p className="muted">
                <Info size={12} /> Retired, and still readable on purpose. <b>A report card issued
                under this version reprints through these rules</b> however long ago it was — so
                #7, #8 and #9 all answer for an inactive scheme. What <span className="mono">
                active</span> governs is whether it is offered for new work.
              </p>
            ) : null}

            {/* Recomputed on every read, so it is a live property rather than a create notice. */}
            {data.warning ? (
              <div className="resp">
                <div className="resp-head">
                  <span className="resp-status" data-ok="true">gap</span>
                </div>
                <pre className="resp-body">⚠ {data.warning}</pre>
              </div>
            ) : null}
          </Card>

          <Card
            title={`Bands · ${bands.length}`}
            description="In the order they were WRITTEN, never re-sorted — so this table can be checked line by line against the paper they were copied from."
          >
            {bands.length === 0 ? (
              <Empty
                title="No bands"
                description="Which #1 refuses, so this should be unreachable — a scheme that grades nothing is not a scheme."
              />
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Code</th>
                      {measured ? <th>From</th> : null}
                      {measured ? <th>To</th> : null}
                      <th>Point</th>
                      <th>Description</th>
                      <th>Pass</th>
                    </tr>
                  </thead>
                  <tbody>
                    {bands.map((band) => (
                      <tr key={band.gradeCode}>
                        <td><span className="mono">{band.gradeCode}</span></td>
                        {measured ? (
                          <td><span className="mono">{band.minimumValue}</span></td>
                        ) : null}
                        {measured ? (
                          <td><span className="mono">{band.maximumValue}</span></td>
                        ) : null}
                        {/* An OUTPUT — what the band is worth in a CGPA once awarded. Nothing
                            resolves a band by it. */}
                        <td>{band.gradePoint != null
                          ? <span className="mono">{band.gradePoint}</span>
                          : <span className="muted">none</span>}</td>
                        <td>{band.description ?? <span className="muted">—</span>}</td>
                        <td>
                          <Badge tone={band.passed ? 'good' : undefined}>
                            {band.passed ? 'pass' : 'fail'}
                          </Badge>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <p className="muted">
              <Info size={12} /> <b>The gap warning is recomputed here, never stored.</b> A school
              that ignored it on create still sees it every time it looks — and a stored sentence
              would outlive the problem it described, so a scheme whose bands were fixed would keep
              being warned about a hole that is gone.
            </p>
            <p className="muted">
              <Info size={12} /> <b>Point is an output.</b> It is what a band is worth in a CGPA
              once the grade has been awarded — nothing resolves a band <i>by</i> it. That is why
              the scale is called <span className="mono">MARKS</span> and not{' '}
              <span className="mono">POINT</span>.
            </p>
          </Card>
        </>
      ) : null}
    </div>
  )
}
