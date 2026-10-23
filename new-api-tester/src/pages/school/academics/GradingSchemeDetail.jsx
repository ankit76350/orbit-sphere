import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Archive, ArchiveRestore, Info, Pencil, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { screenPath } from '../../../paths.js'
import BandEditor from './BandEditor.jsx'
import { PRESET, SCALE, SCALES, bandsChanged, bandsToRows, rowsToBands }
  from './gradingScale.js'

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
  const [editing, setEditing] = useState(false)
  const [toggling, setToggling] = useState(false)
  //! #8 IS ITS OWN READ, and deliberately not folded into the page load. Resolving is a question
  //! ABOUT a value, not a property of the scheme — and its three refusals are the point of having
  //! it on screen, so the answer has to be whatever the API said, including a 404 or a 409.
  const [markValue, setMarkValue] = useState('')
  const [resolved, setResolved] = useState(null)

  //! THE RESULT IS KEPT WHOLE, refusal included. A page that showed only the happy answer would
  //! hide exactly what #8 is worth testing for: which of the three refusals fires, and in what
  //! order. The card below renders the code and message as the API sent them.
  const resolveMark = useCallback(async () => {
    if (!actingSubdomain) return
    const result = await call('resolve-grade', {
      label: 'Turn a mark into a grade',
      pathParams: { id: id ?? '' },
      query: { value: markValue },
    })
    setResolved(result)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, id, markValue])

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

  // #4 and #5 are the same shape: no body, idempotent, one field. One function, because two that
  // differed only in a string would drift — and the URL is what says which way it goes, so a
  // half-read body cannot retire a scheme somebody meant to restore.
  const toggleActive = async () => {
    const retiring = data.active
    setToggling(true)
    await call(retiring ? 'deactivate-grading-scheme' : 'reactivate-grading-scheme', {
      label: retiring ? 'Retire a scheme' : 'Restore a scheme',
      pathParams: { id: id ?? '' },
    })
    setToggling(false)
    load()
  }

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
        {/* #3 — every field, while nothing references the scheme. Offered even then, because
            `active` stays editable and the refusal is the thing worth seeing. */}
        {data ? (
          <>
            {/* #4 and #5 — allowed even on a referenced scheme, which is the one state #3
                refuses outright. Retiring changes nothing a report card already resolved. */}
            <Button
              icon={data.active ? Archive : ArchiveRestore}
              busy={toggling}
              onClick={toggleActive}
            >
              {data.active ? 'Retire' : 'Restore'}
            </Button>
            <Button look="primary" icon={Pencil} onClick={() => setEditing(true)}>Edit</Button>
          </>
        ) : null}
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

          {/* #8 — THE ONLY ENDPOINT THAT EXERCISES A BAND SET ON READ. Every rule the create
              enforces was unproven until this existed, which is why the plan put it in phase 1
              beside the write rather than after it. */}
          <Card
            title="Resolve a mark"
            description="Turn a value into a grade under THIS scheme. Writes nothing — it is arithmetic over the bands above."
            action={<EndpointTag id="resolve-grade" name="Resolve" pathParams={{ id }}
              query={{ value: markValue }} />}
          >
            <div className="toolbar">
              <Field
                label="Value"
                hint="Between 0 and this scheme's ceiling. Both band bounds are inclusive, so a value on a boundary resolves to the band that declares it."
              >
                <Input value={markValue} onChange={(e) => setMarkValue(e.target.value)}
                  placeholder="95" />
              </Field>
              {/* NEVER DISABLED, even with the box empty: a missing ?value= is a 400 worth
                  triggering, and this is an API tester. */}
              <Button look="primary" onClick={resolveMark}>Resolve</Button>
              <Button onClick={() => { setResolved(null); setMarkValue('') }}>Clear</Button>
            </div>

            {resolved ? (
              <div className="resp">
                <div className="resp-head">
                  <span className="resp-status" data-ok={resolved.ok ? 'true' : 'false'}>
                    {resolved.status}
                  </span>
                  <span className="muted">
                    {resolved.ok
                      ? `${resolved.bodyJson?.value} → ${resolved.bodyJson?.gradeCode}`
                      : resolved.bodyJson?.code}
                  </span>
                </div>
                <pre className="resp-body">
                  {resolved.ok
                    ? [
                      `grade        ${resolved.bodyJson?.gradeCode}`,
                      `band         ${resolved.bodyJson?.bandMinimumValue} – ${resolved.bodyJson?.bandMaximumValue}`,
                      `point        ${resolved.bodyJson?.gradePoint ?? 'none'}`,
                      `description  ${resolved.bodyJson?.description ?? '—'}`,
                      `passed       ${resolved.bodyJson?.passed}`,
                      `under        ${resolved.bodyJson?.schemeName} v${resolved.bodyJson?.schemeVersion}`,
                    ].join('\n')
                    : resolved.bodyJson?.message ?? 'Nothing came back.'}
                </pre>
              </div>
            ) : null}

            <p className="muted">
              <Info size={12} /> <b>Three refusals, and the order is the answer.</b> A{' '}
              <span className="mono">DESCRIPTOR</span> scheme is{' '}
              <span className="mono">409 SCHEME_NOT_RESOLVABLE_BY_VALUE</span> — a grade is chosen,
              not computed. A value off the scale is{' '}
              <span className="mono">400 VALUE_OUTSIDE_SCALE</span>. Only then does a missing band
              become <span className="mono">404 GRADE_NOT_RESOLVABLE</span>. Asked in any other
              order, a descriptor scheme would be told it was "outside the scale" when it has none.
            </p>
            <p className="muted">
              <Info size={12} /> <b>A gap is a 404, not a 409</b>, and the message names the bands
              on either side — the school is the only one who can close the hole, and "no grade for
              90.5" does not say where to look. Try a value in any gap the warning above reports.
            </p>
          </Card>
        </>
      ) : null}

      {/* Keyed by the scheme, so a reload re-seeds the form rather than syncing it in an effect. */}
      {editing && data ? (
        <EditScheme
          key={data.gradingSchemeDocsId}
          scheme={data}
          onClose={() => setEditing(false)}
          onSaved={() => load()}
        />
      ) : null}
    </div>
  )
}

/* ----------------------------------------------------------------------- #3, in a modal */

/**
 * Editing one scheme — #3.
 *
 * IT SENDS ONLY WHAT CHANGED. Absent means "leave it alone" on this endpoint, so a rename really
 * is a rename: sending every field would make each save a full overwrite, and would re-send the
 * bands on a scheme whose bands are the one thing you did not touch.
 *
 * THERE IS NO Active FIELD HERE. Retiring and restoring are #4 and #5, the Retire/Restore button
 * on the page behind this modal — every lifecycle flag in this API is a named POST event rather
 * than a field toggled in passing. Which is also why THIS endpoint is refused outright on a
 * referenced scheme: with `active` gone, every field it carries changes what a stored grade means.
 *
 * THE SCALE STILL DRIVES THE FORM. Switching to DESCRIPTOR clears the ceiling and loads that
 * scale's bands, exactly as the create modal does — and the server derives the cleared ceiling
 * anyway, because a PATCH cannot express "remove this number".
 */
function EditScheme({ scheme, onClose, onSaved }) {
  const { call } = useApi()

  // Seeded once from the loaded scheme. The parent keys this by id, so a different scheme is a
  // different component and gets its own seed.
  const [name, setName] = useState(scheme.name ?? '')
  const [schemeVersion, setSchemeVersion] = useState(scheme.schemeVersion ?? '')
  const [scaleType, setScaleType] = useState(scheme.scaleType)
  const [maximumValue, setMaximumValue] = useState(
    scheme.maximumValue != null ? String(scheme.maximumValue) : '')
  const [bands, setBands] = useState(() => bandsToRows(scheme.gradeBands))

  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(null)

  const scale = SCALE[scaleType]
  const measured = scale.ceiling !== null
  const original = bandsToRows(scheme.gradeBands)

  const pickScale = (next) => {
    setScaleType(next)
    setMaximumValue(SCALE[next].ceiling ? (SCALE[next].ceiling.fixed ?? '50') : '')
    setBands(PRESET[next])
  }

  // ONLY WHAT CHANGED. Built at render so the preview shows the real request — and so it is
  // obvious that touching nothing but `active` sends nothing but `active`.
  const body = (() => {
    const out = {}
    if (name !== (scheme.name ?? '')) out.name = name
    if (schemeVersion !== (scheme.schemeVersion ?? '')) out.schemeVersion = schemeVersion
    if (scaleType !== scheme.scaleType) out.scaleType = scaleType

    const storedMax = scheme.maximumValue != null ? String(scheme.maximumValue) : ''
    // Never sent on a descriptor scale: the server DERIVES the cleared ceiling, because a PATCH
    // has no way to say "remove this number".
    if (measured && maximumValue !== storedMax && maximumValue !== '') {
      out.maximumValue = Number(maximumValue)
    }

    if (bandsChanged(bands, original)) out.gradeBands = rowsToBands(bands, measured)
    return out
  })()

  const nothingToSend = Object.keys(body).length === 0

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaved(null)
    setSaving(true)
    const result = await call('update-grading-scheme', {
      label: 'Update a grading scheme',
      pathParams: { id: scheme.gradingSchemeDocsId },
      body,
    })
    setSaving(false)

    if (result.ok) {
      setSaved(result.bodyJson)
      onSaved(result.bodyJson)
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
      open
      onClose={onClose}
      preview={body}
      title={`Edit ${scheme.name}`}
      description="Every field, while nothing references this scheme. Once a subject, exam or report card points at it, only `active` is still editable — moving a boundary then would rewrite a report card already issued."
      endpoint={<EndpointTag id="update-grading-scheme" name="Update" look="primary"
        pathParams={{ id: scheme.gradingSchemeDocsId }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          {/* NEVER DISABLED. An empty PATCH is 400 NOTHING_TO_UPDATE — a refusal the API
              documents and this button used to make unreachable. The label states the
              condition; it does not gate on it. */}
          <Button look="primary" busy={saving} onClick={submit}>
            {nothingToSend ? 'Send it empty (400)' : 'Save'}
          </Button>
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

        {saved ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">200</span>
              <Badge>{saved.bandCount} bands</Badge>
            </div>
            {saved.warning ? <pre className="resp-body">⚠ {saved.warning}</pre> : null}
          </div>
        ) : null}

        <div className="field-grid">
          <Field label="Name" hint="Half the unique key. Renaming splits this rulebook's version history, which is why it is refused once anything references the scheme." error={errors.name}>
            <Input value={name} error={errors.name} onChange={(e) => setName(e.target.value)} />
          </Field>
          <Field label="Version" hint="The other half. Re-checked for uniqueness only when either half actually moved." error={errors.schemeVersion}>
            <Input value={schemeVersion} error={errors.schemeVersion}
              onChange={(e) => setSchemeVersion(e.target.value)} />
          </Field>
        </div>

        <div className="field-grid">
          <Field label="Scale" hint="It reinterprets every band beneath it, so switching loads that scale's bands too — the two are one change." error={errors.scaleType}>
            <Select value={scaleType} options={SCALES} label="Scale" onChange={pickScale} />
          </Field>
          {measured ? (
            <Field label={scaleType === 'PERCENTAGE' ? 'Out of' : 'Paper total'}
              hint={scale.ceiling.hint} error={errors.maximumValue}>
              {/* NOT LOCKED, though the scale implies it: sending a percentage scheme out of
                  90 is a request a tester should be able to make. The hint says what the scale
                  expects. */}
              <Input type="number" value={maximumValue} error={errors.maximumValue}
                onChange={(e) => setMaximumValue(e.target.value)} />
            </Field>
          ) : (
            <Field label="Ceiling"
              hint="Not sent on a DESCRIPTOR scale — the server derives it as absent, because a PATCH cannot say 'remove this number'.">
              {/* A ceiling on a DESCRIPTOR scheme is 400 GRADE_BAND_BOUNDS_NOT_ALLOWED, so
                  the box stays usable — that refusal is worth triggering. */}
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

        <p className="muted">
          <Info size={12} /> <b>Only what changed is sent</b> — the preview beside this form is the
          real request, so a rename really is a rename rather than a full overwrite.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Every field here is refused once anything references this
          scheme</b>, because every one of them changes what a grade already printed means. The
          only thing still allowed then is <b>Retire</b>, which is #4 — on the page behind this.
        </p>
      </div>
    </Modal>
  )
}
