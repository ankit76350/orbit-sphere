import { useState } from 'react'
import { Info, Plus } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * The reporting periods of one academic year: /school-academics/terms
 *
 * ONE ENDPOINT SO FAR — #1 creates a term. There is no list yet (#9 is GET /terms and is not
 * built), which is why this page shows what it has just created rather than what exists. It says
 * so, instead of rendering an empty table that looks like a year with no terms.
 *
 * A TERM IS A DOCUMENT, unlike a section or a subject. Six documents across three modules store
 * termDocsId, which is why it has an id — and why a term can be renamed where a sectionNo can
 * never be. The response shows the id for that reason.
 *
 * THE WARNING IS NOT AN ERROR. A weight total that is not 100 comes back as `warning` and the
 * create still succeeds, because 20/80 to 30/70 passes through 110. The page renders it as a
 * notice beside a successful response, never as a failure.
 */

const BLANK = { name: '', sequence: '', startDate: '', endDate: '', weightPercent: '' }

export default function Terms() {
  const { actingSubdomain, actingAcademicYear } = useApiState()
  const [open, setOpen] = useState(false)
  const [made, setMade] = useState([])

  if (!actingSubdomain) return <NoSchoolChosen what="Terms" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Terms</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {actingAcademicYear ? <> · <span className="mono">{actingAcademicYear}</span></> : null}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add a term</Button>
      </div>

      <Card
        title="Terms created here"
        description="#1 is the only term endpoint built. There is no list yet — #9 is GET /terms — so this shows what this page has created, not what the year holds."
        action={
          <div className="btn-row">
            <EndpointTag id="create-academic-term" name="Add a term"
              pathParams={{ year: actingAcademicYear }} />
            <Badge>{made.length} this session</Badge>
          </div>
        }
      >
        {made.length === 0 ? (
          <Empty
            title="Nothing created here yet"
            description="This is not 'the year has no terms' — nothing can answer that until #9 is built. Add one to see what #1 returns."
            action={
              <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add the first</Button>
            }
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Seq</th>
                  <th>Starts</th>
                  <th>Ends</th>
                  <th>Weight</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {made.map((one) => (
                  <tr key={one.termDocsId}>
                    {/* Derived from the name, and what six documents in three modules store. */}
                    <td><span className="mono">{one.termCode}</span></td>
                    <td>{one.name}</td>
                    <td>{one.sequence}</td>
                    <td><span className="mono">{one.startDate}</span></td>
                    <td><span className="mono">{one.endDate}</span></td>
                    {/* Absent means the school does not weight, which is a normal school. */}
                    <td>{one.weightPercent != null
                      ? `${one.weightPercent}%`
                      : <span className="muted">unweighted</span>}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                      {one.resultsLocked ? <Badge>results locked</Badge> : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="muted">
          <Info size={12} /> A term is a <b>document</b>, not an embedded row — six documents
          across three modules reference one by <span className="mono">termDocsId</span>. That is
          why it has an id, and why a term can be renamed where a{' '}
          <span className="mono">sectionNo</span> never can.
        </p>
      </Card>

      <AddTerm
        open={open}
        year={actingAcademicYear}
        onClose={() => setOpen(false)}
        onAdded={(term) => setMade((old) => [...old, term])}
      />
    </div>
  )
}

/**
 * Adding a term — #1.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD, because a year gets its terms in one sitting. The sequence
 * is bumped and the dates cleared; the name is cleared because it derives the code.
 *
 * EVERY BOX IS FREE TEXT AND NOTHING IS DISABLED, so every refusal stays reachable — an inverted
 * range, a sequence already taken, a weight of 101, dates outside the year.
 */
function AddTerm({ open, year, onClose, onAdded }) {
  const { call } = useApi()
  const [form, setForm] = useState(BLANK)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [made, setMade] = useState(null)

  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))

  // An empty optional box sends nothing rather than "", which the API would read as a value.
  const body = (() => {
    const out = {
      name: form.name,
      startDate: form.startDate,
      endDate: form.endDate,
    }
    if (form.sequence !== '') out.sequence = Number(form.sequence)
    if (form.weightPercent !== '') out.weightPercent = Number(form.weightPercent)
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-academic-term', {
      label: 'Add a term',
      pathParams: { year: year ?? '' },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setMade(result.bodyJson)
      onAdded(result.bodyJson)
      setForm((old) => ({
        ...old,
        name: '',
        startDate: '',
        endDate: '',
        sequence: old.sequence === '' ? '' : String(Number(old.sequence) + 1),
      }))
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
      title="Add a term"
      description="termCode is derived from the name — 'Term 1' becomes TERM_1 — and a term must fit inside the year without overlapping another."
      endpoint={<EndpointTag id="create-academic-term" name="Add" look="primary"
        pathParams={{ year }} />}
      footer={
        <>
          <Button onClick={onClose}>Close</Button>
          <Button look="primary" busy={saving} onClick={submit}>Add</Button>
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
              <span className="resp-status" data-ok="true">
                {made.termCode} · seq {made.sequence}
              </span>
            </div>
            <pre className="resp-body">{made.nextStep}</pre>
          </div>
        ) : null}

        {/* A warning rides on a SUCCESSFUL response. Rendered apart from the refusal above so it
            never reads as a failure — the term was created. */}
        {made?.warning ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">created, with a warning</span>
            </div>
            <pre className="resp-body">{made.warning}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Name"
            required
            hint="The code comes from this — 'Term 1' becomes TERM_1. A name with no letter or digit is a 409."
            error={errors.name}
          >
            <Input value={form.name} error={errors.name}
              onChange={set('name')} placeholder="Term 1" />
          </Field>
          <Field
            label="Sequence"
            required
            hint="Order inside the year, unique within it. A retired term still holds its number."
            error={errors.sequence}
          >
            <Input type="number" value={form.sequence} error={errors.sequence}
              onChange={set('sequence')} placeholder="1" />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Starts"
            required
            hint="Inclusive, and must fall inside the academic year."
            error={errors.startDate}
          >
            <Input type="date" value={form.startDate} error={errors.startDate}
              onChange={set('startDate')} />
          </Field>
          <Field
            label="Ends"
            required
            hint="Inclusive — a term ending 30 September includes the 30th. Before the start is a 400."
            error={errors.endDate}
          >
            <Input type="date" value={form.endDate} error={errors.endDate}
              onChange={set('endDate')} />
          </Field>
        </div>

        <Field
          label="Weight percent"
          hint="Optional, 0 to 100. Blank means this school does not weight the annual result — a normal school. Weighting one term and not another is refused."
          error={errors.weightPercent}
        >
          <Input type="number" value={form.weightPercent} error={errors.weightPercent}
            onChange={set('weightPercent')} placeholder="leave blank if you do not weight" />
        </Field>

        <p className="muted">
          <Info size={12} /> Terms may not overlap, but <b>adjacency is not overlap</b> — one
          ending 30 September and the next starting 1 October are fine. A retired term keeps its
          code and its sequence and releases its dates.
        </p>
      </div>
    </Modal>
  )
}
