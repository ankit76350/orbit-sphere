import { useMemo, useState } from 'react'
import { Info, Plus, Trash2, Wand2 } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * The school's grading rulebooks: /school-academics/grading
 *
 * ONE ENDPOINT — #1. There is no list yet (#6 is not built), so the table below is what THIS
 * PAGE created, not what the school holds. It is labelled that way rather than looking like a
 * list, because a table that silently shows less than the truth is worse than no table.
 *
 * THE FORM IS THE PAGE, not a modal. A scheme has eight bands in the ordinary case and a modal
 * that scrolls is a worse place to check eight rows against the paper they were copied from.
 *
 * THE SCALE DRIVES THE FORM. Picking DESCRIPTOR hides the ceiling and the bounds, because those
 * fields are REFUSED on that scale rather than optional — a form that offered them would be
 * offering a 400.
 *
 * THE WARNING IS NOT AN ERROR. Bands that leave a whole mark ungraded come back as `warning` on
 * a 201, and the page renders it beside a success, never as a failure.
 */

const SCALES = ['PERCENTAGE', 'POINT', 'DESCRIPTOR']

const BLANK_BAND = {
  gradeCode: '', minimumValue: '', maximumValue: '', gradePoint: '', description: '', passed: 'true',
}

/** The scale every example in the plan is drawn from — eight bands, nothing ungraded. */
const CBSE = [
  { gradeCode: 'A1', minimumValue: '91', maximumValue: '100', gradePoint: '10', description: 'Outstanding', passed: 'true' },
  { gradeCode: 'A2', minimumValue: '81', maximumValue: '90', gradePoint: '9', description: 'Excellent', passed: 'true' },
  { gradeCode: 'B1', minimumValue: '71', maximumValue: '80', gradePoint: '8', description: 'Very Good', passed: 'true' },
  { gradeCode: 'B2', minimumValue: '61', maximumValue: '70', gradePoint: '7', description: 'Good', passed: 'true' },
  { gradeCode: 'C1', minimumValue: '51', maximumValue: '60', gradePoint: '6', description: 'Fair', passed: 'true' },
  { gradeCode: 'C2', minimumValue: '41', maximumValue: '50', gradePoint: '5', description: 'Average', passed: 'true' },
  { gradeCode: 'D', minimumValue: '33', maximumValue: '40', gradePoint: '4', description: 'Below Average', passed: 'true' },
  { gradeCode: 'E', minimumValue: '0', maximumValue: '32', gradePoint: '0', description: 'Needs Improvement', passed: 'false' },
]

export default function GradingSchemes() {
  const { call } = useApi()
  const { actingSubdomain } = useApiState()

  const [name, setName] = useState('CBSE Percentage Grading')
  const [schemeVersion, setSchemeVersion] = useState('2026.1')
  const [scaleType, setScaleType] = useState('PERCENTAGE')
  const [maximumValue, setMaximumValue] = useState('100')
  const [bands, setBands] = useState(CBSE)

  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  // Everything this page has created, newest first. NOT the school's schemes — #6 is not built,
  // so there is no honest way to show those.
  const [made, setMade] = useState([])

  // DESCRIPTOR refuses a ceiling and refuses bounds, so those fields are not optional on it —
  // they are a 400. The form hides what the API would refuse rather than letting someone send it.
  const measured = scaleType !== 'DESCRIPTOR'

  // Input gives onChange an EVENT; Select gives it the VALUE. Two setters rather than one
  // that guesses, because a setter reading `.target` off a string fails silently at runtime.
  const setBandValue = (index, field) => (value) =>
    setBands((old) => old.map((band, i) =>
      i === index ? { ...band, [field]: value } : band))

  const setBand = (index, field) => (event) => setBandValue(index, field)(event.target.value)

  const addBand = () => setBands((old) => [...old, { ...BLANK_BAND }])
  const removeBand = (index) => setBands((old) => old.filter((_, i) => i !== index))

  // Built at render so the endpoint tag shows what will actually be sent. An empty optional box
  // sends nothing rather than "", which the API would read as a value — and on a DESCRIPTOR
  // scheme the bounds are omitted entirely rather than sent as null.
  const body = useMemo(() => {
    const out = { name, schemeVersion, scaleType }
    if (measured && maximumValue !== '') out.maximumValue = Number(maximumValue)

    out.gradeBands = bands.map((band) => {
      const one = { gradeCode: band.gradeCode }
      if (measured) {
        if (band.minimumValue !== '') one.minimumValue = Number(band.minimumValue)
        if (band.maximumValue !== '') one.maximumValue = Number(band.maximumValue)
      }
      if (band.gradePoint !== '') one.gradePoint = Number(band.gradePoint)
      if (band.description !== '') one.description = band.description
      // Only sent when false: the API defaults it to true, so sending true on seven of eight
      // bands is noise in a body somebody is reading to understand the request.
      if (band.passed === 'false') one.passed = false
      return one
    })
    return out
  }, [name, schemeVersion, scaleType, maximumValue, bands, measured])

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-grading-scheme', {
      label: 'Create a grading scheme',
      body,
    })
    setSaving(false)

    if (result.ok) {
      setMade((old) => [result.bodyJson, ...old])
      // The version is bumped, not the name: the pair is the key, and the next thing anybody
      // does after creating 2026.1 is create 2026.2 — which is what #2 will do properly.
      setSchemeVersion((old) => nextVersion(old))
      return
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(Object.fromEntries(
        Object.entries(result.bodyJson.fieldErrors)
          .map(([field, messages]) => [field, [].concat(messages)[0]]),
      ))
    }
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) setRefused(result.bodyJson)
  }

  if (!actingSubdomain) return <NoSchoolChosen what="Grading schemes" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Grading schemes</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {' · no academic year — a rulebook outlives one'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={Wand2} onClick={() => { setScaleType('PERCENTAGE'); setBands(CBSE) }}>
          Load the CBSE scale
        </Button>
      </div>

      <Card
        title="The rulebook"
        description="name + schemeVersion is the key, and neither can be changed afterwards. There is no rename and no PATCH at all — moving a boundary means a new version."
        action={
          <>
            {/* The tag names the call; the button makes it. EndpointTag is a label — it
                reopens the last response, it does not send. */}
            <EndpointTag id="create-grading-scheme" name="Create" look="primary" />
            <Button look="primary" busy={saving} onClick={submit}>Create</Button>
          </>
        }
      >
        <div className="stack">
          {refused ? (
            <div className="resp">
              <div className="resp-head">
                <span className="resp-status" data-ok="false">{refused.code}</span>
              </div>
              <pre className="resp-body">{refused.message}</pre>
            </div>
          ) : null}

          <div className="field-grid">
            <Field
              label="Name"
              hint="Half the unique key, so it identifies rather than labels. Every version of one rulebook carries it identically."
              error={errors.name}
            >
              <Input value={name} error={errors.name}
                onChange={(e) => setName(e.target.value)}
                placeholder="CBSE Percentage Grading" />
            </Field>
            <Field
              label="Version"
              hint="The other half. It moves when the RULES move, not when the calendar does."
              error={errors.schemeVersion}
            >
              <Input value={schemeVersion} error={errors.schemeVersion}
                onChange={(e) => setSchemeVersion(e.target.value)} placeholder="2026.1" />
            </Field>
          </div>

          <div className="field-grid">
            <Field
              label="Scale"
              hint="Read before any band. It decides whether bands carry bounds at all — and it can never be changed, because it reinterprets every band under it."
              error={errors.scaleType}
            >
              <Select value={scaleType} options={SCALES} label="Scale"
                onChange={setScaleType} />
            </Field>
            {measured ? (
              <Field
                label="Maximum value"
                hint="The ceiling the bands are read against — 100 for a percentage, 7 for IB points. Required on this scale."
                error={errors.maximumValue}
              >
                <Input type="number" value={maximumValue} error={errors.maximumValue}
                  onChange={(e) => setMaximumValue(e.target.value)} placeholder="100" />
              </Field>
            ) : (
              <Field
                label="Maximum value"
                hint="Not sent on a DESCRIPTOR scale — it is REFUSED rather than optional, because there is nothing to measure."
              >
                <Input value="" disabled readOnly placeholder="not applicable" />
              </Field>
            )}
          </div>
        </div>
      </Card>

      <Card
        title={`Bands · ${bands.length}`}
        description={measured
          ? 'Stored in the order given, never re-sorted. Bounds are inclusive at BOTH ends, so two bands must not touch: 81–90 beside 90–100 both claim 90.'
          : 'A descriptor grade is chosen, not computed — so a band here is a code and a description, and sending bounds is a 400.'}
        action={<Button icon={Plus} onClick={addBand}>Add a band</Button>}
      >
        {bands.length === 0 ? (
          <Empty
            title="No bands"
            description="A scheme that grades nothing is not a scheme — an empty list is a 400."
            action={<Button look="primary" icon={Plus} onClick={addBand}>Add one</Button>}
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
                  <th />
                </tr>
              </thead>
              <tbody>
                {bands.map((band, index) => (
                  // Keyed by position, deliberately: a band has no id and its code is the thing
                  // being edited, so keying by code would remount the row on every keystroke.
                  // oxlint-disable-next-line react/no-array-index-key
                  <tr key={index}>
                    <td>
                      <Input value={band.gradeCode} onChange={setBand(index, 'gradeCode')}
                        placeholder="A1" />
                    </td>
                    {measured ? (
                      <td>
                        <Input type="number" value={band.minimumValue}
                          onChange={setBand(index, 'minimumValue')} placeholder="91" />
                      </td>
                    ) : null}
                    {measured ? (
                      <td>
                        <Input type="number" value={band.maximumValue}
                          onChange={setBand(index, 'maximumValue')} placeholder="100" />
                      </td>
                    ) : null}
                    <td>
                      <Input type="number" value={band.gradePoint}
                        onChange={setBand(index, 'gradePoint')} placeholder="10" />
                    </td>
                    <td>
                      <Input value={band.description} onChange={setBand(index, 'description')}
                        placeholder="Outstanding" />
                    </td>
                    <td>
                      <Select value={band.passed} options={['true', 'false']}
                        label={`passed-${index}`}
                        onChange={setBandValue(index, 'passed')} />
                    </td>
                    <td>
                      <Button icon={Trash2} onClick={() => removeBand(index)}>Remove</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <p className="muted">
          <Info size={12} /> <b>An overlap is refused; a gap is only reported.</b> A gap means one
          mark has <i>no</i> grade — visible, and fixable. An overlap means one mark has{' '}
          <i>two</i>, and which wins depends on the order the bands happen to be stored in.
        </p>
        <p className="muted">
          <Info size={12} /> <b>A scale can never be tiled</b>, because bounds are inclusive at
          both ends: 81–90 beside 91–100 misses 90.5, and closing that is an overlap. So gaps are
          counted in <b>whole marks</b> — 90 → 91 is silent, 32 → 81 is not. The cost is that a
          school awarding 90.5 is not warned; #8 will answer 404 for it.
        </p>
      </Card>

      <Card
        title={`Created here · ${made.length}`}
        description="What THIS PAGE created, not what the school holds — #6, the list, is not built. A table that silently showed less than the truth would be worse than none."
      >
        {made.length === 0 ? (
          <Empty
            title="Nothing created yet"
            description="Create a scheme above and it appears here, with its id and any warning."
          />
        ) : (
          <div className="stack">
            {made.map((scheme) => (
              <div className="resp" key={scheme.gradingSchemeDocsId}>
                <div className="resp-head">
                  <span className="resp-status" data-ok="true">201</span>
                  <span className="mono">{scheme.name}</span>
                  <Badge>v{scheme.schemeVersion}</Badge>
                  <Badge>{scheme.scaleType}</Badge>
                  <Badge tone={scheme.active ? 'good' : undefined}>
                    {scheme.bandCount} bands
                  </Badge>
                  <span className="toolbar-spacer" />
                  {/* The id, not the key pair — it is what three collections store. */}
                  <span className="mono muted">{scheme.gradingSchemeDocsId}</span>
                </div>
                {/* A warning rides on a SUCCESSFUL 201. Never rendered as a failure. */}
                {scheme.warning ? (
                  <pre className="resp-body">⚠ {scheme.warning}</pre>
                ) : null}
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}

/**
 * The next version string, for the box after a successful create.
 *
 * A best guess and nothing more — the format is the SCHOOL'S convention, not ours, which is why
 * the field is free text on the API. "2026.1" becomes "2026.2"; anything this cannot read is left
 * exactly as it was rather than mangled into something the school did not choose.
 */
function nextVersion(current) {
  const match = /^(.*?)(\d+)$/.exec(current)
  if (!match) return current
  return `${match[1]}${Number(match[2]) + 1}`
}
