import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Info, RefreshCw, ShieldAlert } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import { detailPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One seat and who is in it: /school-people/departments/{id}/positions/{positionDocsId}
 *
 * A SEAT IS A ROW INSIDE A UNIT, which is why this is the third address level rather than a page
 * of its own — the same shape a class's section has. A position cannot exist outside a department
 * (#13 requires one, #14 refuses to move it), so an address naming a seat without naming its unit
 * would describe something this product cannot store.
 *
 * THE COUNT AND THE LIST COME FROM ONE READ, so this page cannot show "3 filled" above two names.
 * #53 makes filledHeadcount the size of holders rather than a second count query, because two
 * reads of the same collection a moment apart can disagree.
 *
 * CURRENT HOLDERS ONLY — a seat is not a history. A closed record names somebody who USED TO hold
 * this; one person's history is #19, reached by clicking through to them.
 *
 * LONGEST-SERVING FIRST, the opposite of #19's order. A history is read newest-first because the
 * current row is the interesting one; a roster is read oldest-first because seniority is what
 * distinguishes otherwise identical rows.
 *
 * A HOLDER WHOSE PERSON IS MISSING IS SHOWN, NOT HIDDEN. #53 returns the posting with its name
 * fields absent and a note; dropping it would make the seat look less filled than it is, and then
 * the count and the list would disagree.
 *
 * EVERY ROW OPENS THE PERSON, because "which three" is what this page answers and "who is that" is
 * the next question. #8 carries the fuller answer, and will carry the permission check when there
 * is one.
 *
 * NOTHING HERE IS DISABLED. A retired seat, an over-filled one and an empty one render the same
 * controls, because this is an API tester and every condition has to be reachable.
 */

export default function PositionDetail() {
  const { id, positionDocsId } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-position', {
      label: 'One seat and who is in it',
      pathParams: { id: positionDocsId },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, positionDocsId])

  useEffect(() => { load() }, [load])

  const unit = detailPath('school', 'people', 'departments', id)

  if (!actingSubdomain) return <NoSchoolChosen what="This position" />

  if (problem) {
    return (
      <div className="page stack">
        <Link className="back" to={unit}><ArrowLeft size={13} /> Back to the department</Link>
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={
              problem.bodyJson?.code === 'POSITION_NOT_FOUND'
                ? 'No position with this id in this school. A seat belongs to one school, so '
                  + "another school's real id is a 404 rather than a look at their org chart."
                : problem.bodyJson?.message || 'Nothing came back.'
            }
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        </Card>
      </div>
    )
  }

  const holders = data?.holders ?? []
  const broken = holders.find((one) => one.note)

  return (
    <div className="page stack">
      <Link className="back" to={unit}><ArrowLeft size={13} /> Back to the department</Link>

      <div className="toolbar">
        <div>
          <h1 className="page-title">{data?.title ?? 'Reading the position'}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {data?.departmentName ? <> · {data.departmentName}</> : null}
            {/* RESOLVED BY #53, not by a second request — a detail view is the one place that
                turns an id into a name. A seat reporting to nobody carries neither field. */}
            {data?.reportsToPositionTitle
              ? <> · reports to {data.reportsToPositionTitle}</>
              : <> · reports to nobody</>}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {/* THE NUMBERS THIS PAGE EXISTS FOR. approvedHeadcount is stored on the seat; filledHeadcount
          is COUNTED from current employment records and never stored, because a counter on the
          position document drifts the first time a writer forgets it. vacancies is floored at
          zero — a negative would read as a seat owing people — and the overflow is overFilled. */}
      <Card
        title="The headcount"
        description="Approved is stored on the seat. Filled is counted from current employment records on every call, and never stored."
        action={
          <div className="btn-row">
            <EndpointTag id="get-position" />
            <Badge>{data?.approvedHeadcount ?? '—'} approved</Badge>
            <Badge tone={data?.filledHeadcount ? 'brand' : undefined}>
              {data?.filledHeadcount ?? 0} filled
            </Badge>
            <Badge tone={data?.vacancies ? 'good' : undefined}>
              {data?.vacancies ?? 0} vacant
            </Badge>
            <Badge tone={data?.active ? 'good' : undefined}>
              {data?.active ? 'active' : 'retired'}
            </Badge>
            {data?.teachingPosition ? <Badge tone="brand">teaching</Badge> : null}
          </div>
        }
      >
        {/* #16 WARNS RATHER THAN REFUSING when a school over-hires, so this state is reachable on
            purpose: a twelfth teacher in eleven approved seats is a budget conversation, not a
            data error. */}
        {data?.overFilled ? (
          <p className="muted">
            <ShieldAlert size={12} /> <b>More people hold this seat than were approved.</b>{' '}
            Reachable on purpose — #16 warns rather than refusing, because over-hiring is a budget
            conversation and not a data error. Vacancies reads 0 rather than a negative, and this
            is the flag that tells the two apart.
          </p>
        ) : null}

        {/* #14 DOES NOT CHECK THE FILLED COUNT BEFORE RETIRING — POSITION_STILL_FILLED is a
            refusal it still owes — so a retired seat with people in it is a state this product
            can currently reach, and this page is how somebody notices. */}
        {data?.active === false && holders.length > 0 ? (
          <p className="muted">
            <ShieldAlert size={12} /> <b>This seat is retired and {holders.length}{' '}
            {holders.length === 1 ? 'person still holds' : 'people still hold'} it.</b>{' '}
            #14 does not check the filled count before retiring, so this is reachable rather than
            impossible. POSITION_STILL_FILLED is the refusal it owes.
          </p>
        ) : null}

        {!data?.overFilled && !(data?.active === false && holders.length > 0) ? (
          <p className="muted">
            <Info size={12} /> Nothing on this seat contradicts itself: the filled count is at or
            under the approved one, and the seat is in the state its holders imply.
          </p>
        ) : null}
      </Card>

      <Card
        title="Who is in it now"
        description="Current records only — a seat is not a history. Longest-serving first, which is the opposite of #19's order and deliberately so."
        action={
          <div className="btn-row">
            <EndpointTag id="get-position" />
            <Badge tone={holders.length ? 'brand' : undefined}>
              {holders.length} {holders.length === 1 ? 'holder' : 'holders'}
            </Badge>
          </div>
        }
      >
        {holders.length === 0 ? (
          <Empty
            title="Nobody currently holds this position"
            /* AN EMPTY LIST WITH NO EXPLANATION READS AS SOMETHING HAVING GONE WRONG, so the API
               sends the words and the page shows them rather than inventing its own. */
            description={data?.holdersNote
              || 'The seat is approved and unfilled. That is a real state, not a missing record.'}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Employee no</th>
                  <th>Status</th>
                  <th>Type</th>
                  <th>Since</th>
                  <th>Manager</th>
                  <th>Record id</th>
                </tr>
              </thead>
              <tbody>
                {holders.map((one) => (
                  <tr
                    key={one.employmentDocsId}
                    data-opens
                    /* #8 IS THE FULLER ANSWER, and the endpoint that will carry the permission
                       check when there is one. */
                    onClick={() => navigate(
                      detailPath('school', 'people', 'staff', one.staffDocsId))}
                  >
                    <td>
                      {one.fullName
                        ? one.fullName
                        /* MARKED, NEVER DROPPED. #53 returns a posting whose person does not
                           resolve, because hiding it would make the seat look less filled than it
                           is — and then the count and the list would disagree. */
                        : <span className="muted">a posting with no person</span>}
                    </td>
                    <td>{one.employeeNo
                      ? <span className="mono">{one.employeeNo}</span>
                      : <span className="muted">—</span>}</td>
                    <td>
                      <Badge tone={one.status === 'ACTIVE' ? 'good' : undefined}>
                        {one.status}
                      </Badge>
                    </td>
                    <td>{one.employmentType ?? <span className="muted">—</span>}</td>
                    <td>{one.effectiveFrom}</td>
                    <td>{one.managerDocsId
                      ? <span className="mono">{one.managerDocsId}</span>
                      : <span className="muted">nobody</span>}</td>
                    {/* THE POSTING'S OWN ID, which is what #18 is addressed by — a person has
                        several records and the URL has to say which. */}
                    <td><span className="muted mono">{one.employmentDocsId}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* A BROKEN REFERENCE IS EXPLAINED IN THE API'S OWN WORDS, once, under the table. */}
        {broken ? (
          <p className="muted"><Info size={12} /> {broken.note}</p>
        ) : null}
      </Card>

      <Card title="Before this ships">
        <p className="muted">
          <ShieldAlert size={12} /> <b>Nothing checks who is asking.</b> Anybody who can reach the
          API with a school subdomain can read who holds every seat in this school. The API repeats
          it on every response: <span className="mono">{data?.note ?? '—'}</span>
        </p>
        <p className="muted">
          <Info size={12} /> <b>The filled count is computed, never stored.</b> A counter on the
          position document would drift the first time a writer forgot it — the same objection that
          keeps a weight total off a term and a gap warning off a grading scheme.
        </p>
        <p className="muted">
          <Info size={12} /> <b>This is current holders, not the seat&apos;s history.</b> Nothing
          in the API answers &ldquo;who has ever held this seat&rdquo;. One person&apos;s history is
          #19, reached by opening them.
        </p>
        <p className="muted">
          <Info size={12} /> <b>#14 still owes two checks</b> —{' '}
          <span className="mono">POSITION_STILL_FILLED</span> when a filled seat is retired, and{' '}
          <span className="mono">HEADCOUNT_BELOW_FILLED</span> when the approved number is lowered
          under the filled one. Both can be computed now that this count exists; neither is
          enforced.
        </p>
      </Card>
    </div>
  )
}
