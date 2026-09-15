import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Briefcase, Info, RefreshCw, ShieldAlert } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty } from '../../../components/ui/Kit.jsx'
import { screenPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One person, at their own address: /school-people/staff/{id}
 *
 * THE EMPLOYMENT CARD COMES FIRST, above the personal details, because "what do they do here" is
 * the question this page is opened to answer. It is empty — #16 is not built — and it says so
 * with the API's own words rather than a blank panel, because an absence like that reads as a bug
 * in the page otherwise.
 *
 * ONE ENDPOINT FILLS IT. #8 returns everything below in one read.
 *
 * THIS IS THE MOST SENSITIVE SCREEN IN THE PRODUCT, and it is the only one that says so on the
 * page. A date of birth, a home address and an emergency contact, with no authorization anywhere
 * behind them — the module plan calls that its biggest open item, and a tester should see it
 * where the data is rather than in a README.
 *
 * #7's ROW AND #8's RECORD ARE MEANT TO DISAGREE. The list carries none of this, because a list
 * is read by every dropdown. Opening one person is a deliberate act.
 */

const LIST = screenPath('school', 'people', 'staff')

export default function StaffDetail() {
  const { id } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-staff', {
      label: 'One person in full',
      pathParams: { id },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="This person" />

  if (problem) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All staff</Link>
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={
              problem.bodyJson?.code === 'STAFF_NOT_FOUND'
                ? 'No staff member with this id in this school. A person belongs to one school, '
                  + "so another school's real id is a 404 rather than somebody else's home "
                  + 'address — and the refusal says nothing about them either.'
                : problem.bodyJson?.message || 'Nothing came back.'
            }
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        </Card>
      </div>
    )
  }

  const address = (one) => {
    if (!one) return <span className="muted">none on file</span>
    const parts = [one.addressLine1, one.addressLine2, one.city, one.stateOrProvince,
      one.postalCode, one.countryCode].filter(Boolean)
    return parts.join(', ')
  }

  return (
    <div className="page stack">
      <Link className="back" to={LIST}><ArrowLeft size={13} /> All staff</Link>

      <div className="toolbar">
        <div>
          <h1 className="page-title">{data?.fullName ?? 'Reading the person'}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {data ? <> · <span className="mono">{data.employeeNo}</span></> : null}
            {data ? ' · entered, not yet employed' : ''}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
      </div>

      {/* FIRST, because it is the question this page is opened to answer. */}
      <Card
        title="Employment"
        description="What they do here — the current record, folded into #8 rather than fetched separately."
        action={<Badge>not built</Badge>}
      >
        <Empty
          title="Nobody is employed anywhere in this product yet"
          description={data?.employmentNote
            ?? 'The employment record is written by #16 POST /staff/{id}/employment, which is not built.'}
        />
        <p className="muted">
          <Briefcase size={12} /> <b>The block is absent, not empty</b> — no{' '}
          <span className="mono">employment</span> key at all, rather than{' '}
          <span className="mono">null</span> or <span className="mono">{'{}'}</span>. Three ways of
          saying &quot;nothing here&quot; is three cases a client has to handle.
        </p>
        <p className="muted">
          <Info size={12} /> <b>This is not scaffolding.</b> A person with no employment record
          stays a real state once #16 exists — somebody the school has entered and not yet hired,
          which is what #1 leaves them in. This card will look like this for them forever.
        </p>
      </Card>

      <Card
        title="The person"
        description="Everything #8 returns, in one read. The list at #7 deliberately carries none of it."
        action={<EndpointTag id="get-staff" name="Read" pathParams={{ id }} />}
      >
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Employee no</td>
                <td><span className="mono">{data?.employeeNo}</span></td></tr>
              <tr><td className="muted">Name</td><td>{data?.fullName}</td></tr>
              <tr><td className="muted">Date of birth</td><td>{data?.dateOfBirth}</td></tr>
              <tr><td className="muted">Gender</td><td>{data?.gender}</td></tr>
              <tr><td className="muted">Phone</td>
                <td>{data?.phoneNumber
                  ? <span className="mono">{data.phoneNumber}</span>
                  : <span className="muted">none on file</span>}</td></tr>
              <tr><td className="muted">Email</td>
                <td>{data?.emailAddress ?? <span className="muted">none on file</span>}</td></tr>
              <tr><td className="muted">Nationality</td>
                <td>{data?.nationalityCode ?? <span className="muted">none on file</span>}</td></tr>
              <tr><td className="muted">Preferred language</td>
                <td>{data?.preferredLanguage ?? <span className="muted">none on file</span>}</td></tr>
              <tr><td className="muted">Staff id</td>
                <td><span className="mono">{data?.staffDocsId}</span>{' '}
                  <span className="muted">what every other collection stores</span></td></tr>
            </tbody>
          </table>
        </div>
      </Card>

      <Card
        title="Addresses"
        description="Current and permanent, replaced as a pair by #3 — which is not built."
      >
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Current</td><td>{address(data?.currentAddress)}</td></tr>
              <tr><td className="muted">Permanent</td><td>{address(data?.permanentAddress)}</td></tr>
            </tbody>
          </table>
        </div>
        <p className="muted">
          <Info size={12} /> <b>An address with nothing in it was stored as no address</b>, so
          &quot;none on file&quot; here means exactly that — not an object of six empty fields.
        </p>
      </Card>

      <Card
        title="Emergency contact"
        description="Replaced whole by #4 — which is not built. A new name beside an old number is worse than nothing."
      >
        {data?.emergencyContact ? (
          <div className="table-scroll">
            <table className="data-table">
              <tbody>
                <tr><td className="muted">Name</td>
                  <td>{data.emergencyContact.fullName ?? <span className="muted">none</span>}</td></tr>
                <tr><td className="muted">Relationship</td>
                  <td>{data.emergencyContact.relationship ?? <span className="muted">none</span>}</td></tr>
                <tr><td className="muted">Phone</td>
                  <td>{data.emergencyContact.phoneNumber
                    ? <span className="mono">{data.emergencyContact.phoneNumber}</span>
                    : <span className="muted">none</span>}</td></tr>
              </tbody>
            </table>
          </div>
        ) : (
          <Empty title="None on file" description="Nobody is recorded for this person." />
        )}
      </Card>

      {/* THE WARNING GOES WHERE THE DATA IS. A README nobody opens is not a control. */}
      <Card title="Before this ships">
        <p className="muted">
          <ShieldAlert size={12} /> <b>This is the fullest thing the product returns about a human
          being, and nothing checks who is asking.</b> A date of birth, a home address and an
          emergency contact come back to anybody who can reach the API with a school subdomain.
          The module plan calls authorization its open item that matters most, and this is the
          endpoint it means.
        </p>
        <p className="muted">
          <Info size={12} /> The API repeats it on every response:{' '}
          <span className="mono">{data?.note ?? '—'}</span>
        </p>
      </Card>
    </div>
  )
}
