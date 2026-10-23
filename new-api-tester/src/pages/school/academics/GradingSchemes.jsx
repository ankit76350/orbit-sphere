import { useCallback, useEffect, useMemo, useState } from 'react'
import { Info, Plus, RefreshCw, Search, Wand2 } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { detailPath } from '../../../paths.js'
import BandEditor from './BandEditor.jsx'
import { PRESET, SCALE, SCALES, rowsToBands } from './gradingScale.js'

/**
 * The school's grading rulebooks: /school-academics/grading
 *
 * TWO ENDPOINTS — #6 lists the school's schemes and #1 adds one. The table is #6's answer, so it
 * shows what the school HOLDS rather than what this page happened to create.
 *
 * THE PAGE IS THE LIST; CREATING IS A MODAL. The same shape as every other screen here. A form
 * sitting permanently above the table made the table look like a preview of the form, when the
 * table is the endpoint that matters and creating is the occasional act.
 *
 * THE TABLE CARRIES NO BANDS, because #6 does not return them — eight bands of six fields per row
 * would be ~600 values to render twelve names. bandCount is what survives, and it is how a person
 * recognises a scale they know: eight is the CBSE one, three is probably descriptors.
 *
 * NOTHING HERE CAN 404. Unlike the term list there is no {year} to resolve, so an empty table
 * means "this school has no schemes" rather than "no such year".
 */

const TRISTATE = ['', 'true', 'false']
const SORTS = ['', 'name', 'name,desc', 'schemeVersion', 'schemeVersion,desc', 'scaleType',
  'createdAt,desc', 'updatedAt,desc']
const SIZES = ['5', '20', '100']

export default function GradingSchemes() {
  const { call } = useApi()
  const { environment, actingSubdomain } = useApiState()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  const [active, setActive] = useState('')
  const [scaleFilter, setScaleFilter] = useState('')
  // Typed, then SENT. `search` is what the last request used; `typed` is what the box holds.
  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState('20')

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  // Built at render, so the endpoint tag shows the URL that will actually be sent. An empty box
  // sends nothing rather than an empty parameter — `?active=` is not the same as no filter.
  const query = useMemo(() => {
    const out = { page, size }
    if (active) out.active = active
    if (scaleFilter) out.scaleType = scaleFilter
    if (search.trim()) out.search = search.trim()
    if (sort) out.sort = sort
    return out
  }, [page, size, active, scaleFilter, search, sort])

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-grading-schemes', {
      label: "The school's schemes",
      query,
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, query])

  useEffect(() => { load() }, [load])

  const runSearch = () => { setPage(0); setSearch(typed) }
  const rows = data?.content ?? []

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
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add a scheme</Button>
      </div>

      <Card
        title="Filters"
        description="All three are AND-ed, and blank sends nothing at all — which is not the same as sending false."
        action={<EndpointTag id="list-grading-schemes" name="List" query={query} />}
      >
        <div className="field-grid">
          <Field label="Active" hint="Blank returns BOTH. A retired scheme still resolves every report card that used it.">
            <Select value={active} options={TRISTATE} label="Active filter" onChange={setActive} />
          </Field>
          <Field label="Scale" hint="The filter that answers 'what can grade a number' — DESCRIPTOR cannot be resolved by value at all.">
            <Select value={scaleFilter} options={['', ...SCALES]} label="Scale filter"
              onChange={setScaleFilter} />
          </Field>
        </div>
        <div className="field-grid">
          <Field label="Search" hint="Matches name only — a scheme has no code, unlike a term. Regex-quoted, so a stray ( is an empty page rather than a 500.">
            <Input value={typed} onChange={(e) => setTyped(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') runSearch() }}
              placeholder="cbse" />
          </Field>
          <Field label="Sort" hint="An allowlist. gradeBands is excluded on purpose: Mongo sorts an array by its first element.">
            <Select value={sort} options={SORTS} label="Sort" onChange={setSort} />
          </Field>
        </div>
        <div className="toolbar">
          <Button icon={Search} onClick={runSearch}>Search</Button>
          <span className="toolbar-spacer" />
          <Select value={size} options={SIZES} label="Page size"
            onChange={(value) => { setPage(0); setSize(value) }} />
        </div>
      </Card>

      <Card
        title={`Schemes · ${data?.totalElements ?? 0}`}
        description="#6's answer — what the school HOLDS. No bands here: #7 is one call away for the caller that wants them."
        action={
          <>
            {/* NEVER DISABLED — walking off either end of the page range is a request whose
                answer is worth seeing, and gating it hides that. */}
            <Button onClick={() => setPage((p) => p - 1)}>
              Previous
            </Button>
            <Badge>page {(data?.page ?? 0) + 1} of {data?.totalPages ?? 1}</Badge>
            <Button onClick={() => setPage((p) => p + 1)}>
              Next
            </Button>
          </>
        }
      >
        {problem ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? 'Request failed.'}</pre>
          </div>
        ) : rows.length === 0 ? (
          <Empty
            title="No schemes match"
            // Never a 404: there is no parent to resolve, so an empty page is a fact.
            description="An empty page, never a 404 — there is no year to be wrong about. Clear the filters to see whether the school has any at all."
            action={
              <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add one</Button>
            }
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Version</th>
                  <th>Scale</th>
                  <th>Max</th>
                  <th>Bands</th>
                  <th>Status</th>
                  <th>Id</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  // Opening a row is its OWN address, so it can be linked, reloaded and shared —
                  // and #7 is the only endpoint that returns the bands, so it is worth a page.
                  <tr
                    key={one.gradingSchemeDocsId}
                    data-opens
                    onClick={() => navigate(detailPath('school', 'academics', 'grading',
                      one.gradingSchemeDocsId))}
                  >
                    {/* name + schemeVersion is the KEY, so the two sit together. */}
                    <td>{one.name}</td>
                    <td><span className="mono">{one.schemeVersion}</span></td>
                    <td><Badge>{one.scaleType}</Badge></td>
                    {/* Absent on a DESCRIPTOR scheme — refused rather than optional. */}
                    <td>{one.maximumValue != null
                      ? <span className="mono">{one.maximumValue}</span>
                      : <span className="muted">not measured</span>}</td>
                    <td>{one.bandCount}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                    </td>
                    {/* What three collections store, and what #7 will be addressed by. */}
                    <td><span className="mono muted">{one.gradingSchemeDocsId}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <p className="muted">
          <Info size={12} /> Open a row for its <b>bands</b> — #6 returns only a count, because a
          page of full band tables is hundreds of values nobody reads.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Ordered by name, then version</b> — the first list in this API
          whose stable order needs two fields. Neither alone is unique: one name has many
          versions, and one version string spans many names. Grouping a rulebook's versions
          together is the side effect, not the reason.
        </p>
      </Card>

      <AddScheme open={open} onClose={() => setOpen(false)} onAdded={() => load()} />
    </div>
  )
}

/* ----------------------------------------------------------------------- #1, in a modal */

/**
 * Creating one rulebook — #1.
 *
 * THE SCALE DRIVES THE FORM. Picking DESCRIPTOR hides the ceiling and both bound columns, because
 * those are REFUSED on that scale rather than optional — a form that offered them would be
 * offering a 400.
 *
 * THE BODY PREVIEW IS THE POINT OF THE SPLIT. A scheme is the largest body this API takes, and
 * watching the JSON change as a band is typed is the fastest way to see that an empty box sends
 * nothing rather than "".
 *
 * IT STAYS OPEN AFTER A SUCCESSFUL CREATE, with the version bumped. The next thing anybody does
 * after 2026.1 is 2026.2 — which is what #2 will do properly once it exists.
 *
 * THE WARNING IS NOT AN ERROR. Bands that leave a whole mark ungraded come back as `warning` on a
 * 201, and it renders beside the success, never as a failure.
 */
function AddScheme({ open, onClose, onAdded }) {
  const { call } = useApi()

  const [name, setName] = useState('CBSE Percentage Grading')
  const [schemeVersion, setSchemeVersion] = useState('2026.1')
  const [scaleType, setScaleType] = useState('PERCENTAGE')
  const [maximumValue, setMaximumValue] = useState('100')
  // The percentage preset, because the scale starts at PERCENTAGE. pickScale swaps both.
  const [bands, setBands] = useState(PRESET.PERCENTAGE)

  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const scale = SCALE[scaleType]
  // `ceiling: null` is the scale saying it measures nothing. The form hides what the API would
  // REFUSE rather than offering a field whose only outcome is a 400.
  const measured = scale.ceiling !== null

  /**
   * Changing the scale rewrites the ceiling AND the bands, because neither survives the move.
   *
   * 91–100 means nothing on a paper out of 50, and a descriptor band has no bounds to keep. The
   * old form left the previous scale's bands sitting in the table where the columns had silently
   * vanished, so switching to DESCRIPTOR and back showed numbers that were never going to be sent.
   * Loading that scale's starter set is both correct and the thing somebody wanted next.
   */
  const pickScale = (next) => {
    setScaleType(next)
    setMaximumValue(SCALE[next].ceiling ? (SCALE[next].ceiling.fixed ?? '50') : '')
    setBands(PRESET[next])
  }

  // Built at render so the preview shows what will actually be sent. An empty optional box sends
  // nothing rather than "", which the API would read as a value — and on a DESCRIPTOR scheme the
  // bounds are omitted entirely rather than sent as null.
  const body = useMemo(() => {
    const out = { name, schemeVersion, scaleType }
    if (measured && maximumValue !== '') out.maximumValue = Number(maximumValue)

    out.gradeBands = rowsToBands(bands, measured)
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
      setMade(result.bodyJson)
      onAdded(result.bodyJson)
      // The version is bumped, not the name: the pair is the key, and the next thing anybody
      // does after creating 2026.1 is create 2026.2.
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

  return (
    <Modal
      open={open}
      onClose={onClose}
      preview={body}
      title="Add a grading scheme"
      description="name + schemeVersion is the key, and neither can be changed afterwards. There is no rename and no PATCH at all — moving a boundary means a new version."
      endpoint={<EndpointTag id="create-grading-scheme" name="Create" look="primary" />}
      footer={
        <>
          {/* Labelled for the scale in hand — "Load the CBSE scale" on a marks scheme would
              load bands that cannot fit, and the button would look broken. */}
          <Button icon={Wand2} onClick={() => setBands(PRESET[scaleType])}>
            {scale.presetLabel}
          </Button>
          <span className="toolbar-spacer" />
          <Button onClick={onClose}>Close</Button>
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

        {made ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">201</span>
              <Badge>{made.bandCount} bands</Badge>
              <span className="toolbar-spacer" />
              {/* The id, not the key pair — it is what three collections store. */}
              <span className="mono muted">{made.gradingSchemeDocsId}</span>
            </div>
            {/* A warning rides on a SUCCESSFUL 201. Never rendered as a failure. */}
            {made.warning ? <pre className="resp-body">⚠ {made.warning}</pre> : null}
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Name"
            required
            hint="Half the unique key, so it identifies rather than labels. Every version of one rulebook carries it identically."
            error={errors.name}
          >
            <Input value={name} error={errors.name}
              onChange={(e) => setName(e.target.value)}
              placeholder="CBSE Percentage Grading" />
          </Field>
          <Field
            label="Version"
            required
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
            required
            hint="Read before any band. It decides whether bands carry bounds at all — and it can never be changed, because it reinterprets every band under it."
            error={errors.scaleType}
          >
            <Select value={scaleType} options={SCALES} label="Scale" onChange={pickScale} />
          </Field>
          {measured ? (
            <Field
              label={scaleType === 'PERCENTAGE' ? 'Out of' : 'Paper total'}
              required
              hint={scale.ceiling.hint}
              error={errors.maximumValue}
            >
              {/* Locked when the scale defines it. A percentage scheme out of 90 is not a
                  percentage scheme, so the box states the fact rather than inviting a typo. */}
              {/* NOT LOCKED. A percentage scheme out of 90 is a request a tester should be
                  able to send; the hint states what the scale expects. */}
              <Input type="number" value={maximumValue} error={errors.maximumValue}
                onChange={(e) => setMaximumValue(e.target.value)}
                placeholder={scale.ceiling.fixed ?? '50'} />
            </Field>
          ) : (
            <Field
              label="Ceiling"
              hint="Not sent on a DESCRIPTOR scale — it is REFUSED rather than optional, because there is nothing to measure."
            >
              {/* A ceiling on a DESCRIPTOR scheme is 400 GRADE_BAND_BOUNDS_NOT_ALLOWED —
                  a refusal worth triggering, so the box stays usable. */}
              <Input value={maximumValue}
                onChange={(e) => setMaximumValue(e.target.value)}
                placeholder="leave blank; a value here is refused" />
            </Field>
          )}
        </div>

        <BandEditor
          scaleType={scaleType}
          maximumValue={maximumValue}
          rows={bands}
          onChange={setBands}
        />
      </div>
    </Modal>
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
