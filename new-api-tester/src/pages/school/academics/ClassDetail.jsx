import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, ChevronRight, Info, Plus, RefreshCw, Users } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { screenPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One class, at its own address: /school-academics/classes/{id}
 *
 * THREE OF THE GROUP'S ENDPOINTS LIVE HERE. #29 reads the class with its sections and subjects,
 * #17 adds a section, and #30 reads the sections on their own. They are together because they
 * all answer questions about one class, and because a class with four sections and ten subjects
 * is more than a modal's worth of screen.
 *
 * THE YEAR COMES FROM THE TOP BAR, NOT THE ADDRESS. A class id is globally unique, so the id
 * alone finds it — but the API scopes the lookup by year as well, deliberately, so that an id
 * from last year's URL cannot read this year's structure. Which means switching the year in the
 * top bar makes this page 404, and that is the scoping working rather than a bug. The page says
 * so instead of showing an empty shell.
 *
 * #29 ALREADY RETURNS THE SECTIONS, so the table below is drawn from it and #30 is not called on
 * load. Calling both would be two reads of the same document to draw one table. #30 is what the
 * section modal asks, because that is the endpoint a "move this student" dropdown would use and
 * the modal is where its ?active= filter can be seen.
 */

const LIST = screenPath('school', 'academics', 'classes')

/** Three-state: '' sends no parameter at all, which is not the same as false. */
const TRISTATE = ['', 'true', 'false']

export default function ClassDetail() {
  const { id } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [adding, setAdding] = useState(false)
  const [openSection, setOpenSection] = useState(null)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('get-school-class', {
      label: 'One class in full',
      // Empty when no year is picked, so the request still goes and the server answers 404.
      pathParams: { year: actingAcademicYear ?? '', id },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, actingAcademicYear, id])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="This class" />

  if (problem) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> All classes</Link>
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={
              problem.bodyJson?.code === 'CLASS_NOT_FOUND'
                ? `No class with this id in ${actingAcademicYear || 'the year picked above'}. `
                  + 'A class belongs to one year, so the same id under another year is a 404 — '
                  + 'check the year in the top bar.'
                : problem.bodyJson?.message || 'Nothing came back.'
            }
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        </Card>
      </div>
    )
  }

  const sections = data?.sections ?? []
  const subjects = data?.subjects ?? []

  return (
    <div className="page stack">
      <Link className="back" to={LIST}><ArrowLeft size={13} /> All classes</Link>

      <div className="toolbar">
        <div>
          <h1 className="page-title">{data?.name ?? 'Reading the class'}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {data ? <> · <span className="mono">{data.academicYear}</span></> : null}
            {data ? ` · ${data.sectionCount} section${data.sectionCount === 1 ? '' : 's'}` : ''}
            {data ? ` · ${data.subjectCount} subject${data.subjectCount === 1 ? '' : 's'}` : ''}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <EndpointTag id="get-school-class" name="Read" />
        <Button look="primary" icon={Plus} onClick={() => setAdding(true)}>Add a section</Button>
      </div>

      <Card title="The class" description="Everything #29 returns about it, in one read.">
        <div className="table-scroll">
          <table className="data-table">
            <tbody>
              <tr><td className="muted">Name</td><td>{data?.name}</td></tr>
              <tr><td className="muted">Year</td>
                <td><span className="mono">{data?.academicYear}</span></td></tr>
              <tr><td className="muted">Active</td>
                <td>{data
                  ? <Badge tone={data.active ? 'good' : undefined}>
                      {data.active ? 'active' : 'retired'}
                    </Badge>
                  : null}</td></tr>
              {/* The raw id, never resolved to a board name — one place should decide how a
                  programme is presented, and it is not this response. */}
              <tr><td className="muted">Affiliation programme</td>
                <td>{data?.affiliationProgrammeDocsId
                  ? <span className="mono">{data.affiliationProgrammeDocsId}</span>
                  : <span className="muted">none</span>}</td></tr>
              <tr><td className="muted">Class id</td>
                <td><span className="mono">{data?.schoolClassId}</span></td></tr>
            </tbody>
          </table>
        </div>
      </Card>

      <Card
        title="Sections"
        description="From #29's read — it returns them, so there is no second call to draw this. Click one to ask #30 about it."
        action={
          <div className="btn-row">
            <Badge>{data?.sectionCount ?? 0} total</Badge>
            <Badge tone="good">{data?.activeSectionCount ?? 0} active</Badge>
          </div>
        }
      >
        {sections.length === 0 ? (
          <Empty
            title="No sections yet"
            description="Nothing can be placed in this class until one exists — a student record stores sectionNo."
            action={
              <Button look="primary" icon={Plus} onClick={() => setAdding(true)}>
                Add the first
              </Button>
            }
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Section</th>
                  <th>Capacity</th>
                  <th>Class teacher</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {sections.map((one) => (
                  <tr
                    key={one.sectionNo}
                    data-opens
                    onClick={() => setOpenSection(one)}
                  >
                    {/* sectionNo is the display value as well as the reference, which is why
                        there is no separate name to show. */}
                    <td><span className="mono">{one.sectionNo}</span></td>
                    <td>
                      {one.capacity ?? <span className="muted">no plan recorded</span>}
                    </td>
                    <td>
                      {one.classTeacherDocsId
                        ? <span className="mono">{one.classTeacherDocsId}</span>
                        : <span className="muted">none</span>}
                    </td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                    </td>
                    <td><span className="muted">Open <ChevronRight size={13} /></span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="muted">
          <Info size={12} /> A capacity is a plan, not a limit — nothing enforces it. A class
          teacher is a raw <span className="mono">Staff.id</span>; read the staff record when you
          need the name.
        </p>
      </Card>

      <Card
        title="Subjects"
        description="Also from #29. This is the only endpoint that returns them."
        action={
          <div className="btn-row">
            <Badge>{data?.subjectCount ?? 0} total</Badge>
            <Badge tone="good">{data?.activeSubjectCount ?? 0} active</Badge>
          </div>
        }
      >
        {subjects.length === 0 ? (
          <Empty
            title="Nothing is taught in this class yet"
            description="#22 assigns a subject and is not built, so every class reads this way."
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Applies to</th>
                  <th>Teachers</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {/* The row key is the PAIR, because one subjectCode may appear twice — once
                    class-wide and once for a section with its own teacher. */}
                {subjects.map((one) => (
                  <tr key={`${one.subjectCode}/${one.sectionNo ?? 'all'}`}>
                    <td><span className="mono">{one.subjectCode}</span></td>
                    <td>{one.name}{one.shortName ? <span className="muted"> ({one.shortName})</span> : null}</td>
                    <td>{one.subjectType}</td>
                    {/* Absent means the whole class, which is the ordinary case. */}
                    <td>{one.sectionNo
                      ? <>section <span className="mono">{one.sectionNo}</span></>
                      : <span className="muted">all sections</span>}</td>
                    <td>{(one.teacherDocsIds ?? []).length}</td>
                    <td>
                      <Badge tone={one.active ? 'good' : undefined}>
                        {one.active ? 'active' : 'retired'}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <SectionModal
        section={openSection}
        classId={id}
        year={actingAcademicYear}
        onClose={() => setOpenSection(null)}
      />

      <AddSection
        open={adding}
        classId={id}
        year={actingAcademicYear}
        onClose={() => setAdding(false)}
        onAdded={() => load()}
      />
    </div>
  )
}

/**
 * One section, asked of #30 — `GET /classes/{id}/sections`.
 *
 * WHY THIS CALLS #30 AT ALL, when #29 already handed the row over. #30 is what a "move this
 * student" dropdown would use, and this is the only place its `?active=` filter and its
 * un-filtered counts can be seen. The row that was clicked is highlighted in the answer, so the
 * modal shows both the one section and the endpoint's real shape.
 *
 * THE COUNTS DO NOT FOLLOW THE FILTER, deliberately, and the modal prints both numbers side by
 * side so that is visible rather than surprising.
 */
function SectionModal({ section, classId, year, onClose }) {
  if (!section) return null
  return (
    <SectionModalBody
      key={section.sectionNo}
      section={section}
      classId={classId}
      year={year}
      onClose={onClose}
    />
  )
}

function SectionModalBody({ section, classId, year, onClose }) {
  const { call } = useApi()
  const [active, setActive] = useState('')
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)

  const read = useCallback(async () => {
    const result = await call('list-class-sections', {
      label: "The class's sections",
      pathParams: { year: year ?? '', id: classId },
      query: active ? { active } : {},
    })
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, year, classId, active])

  useEffect(() => { read() }, [read])

  const shown = data?.sections ?? []
  const mine = shown.find((one) => one.sectionNo === section.sectionNo)

  return (
    <Modal
      open
      onClose={onClose}
      title={`Section ${section.sectionNo}`}
      description="Read through #30, the endpoint a section dropdown would use — not from the class read that drew the table."
      endpoint={<EndpointTag id="list-class-sections" name="Read" />}
      footer={
        <>
          <Button icon={RefreshCw} onClick={read}>Re-read</Button>
          <Button onClick={onClose}>Close</Button>
        </>
      }
    >
      <div className="stack">
        {problem ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code || `HTTP ${problem.status}`}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message}</pre>
          </div>
        ) : null}

        {/* This section, from #30's answer rather than from the row that was clicked — so a
            filter that excludes it is visible as an absence. */}
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok={mine ? 'true' : 'false'}>
              {mine ? `section ${mine.sectionNo}` : 'not in the filtered answer'}
            </span>
          </div>
          <pre className="resp-body">
            {mine
              ? [
                  `sectionNo     ${mine.sectionNo}`,
                  `capacity      ${mine.capacity ?? 'no plan recorded'}`,
                  `class teacher ${mine.classTeacherDocsId ?? 'none'}`,
                  `active        ${mine.active}`,
                ].join('\n')
              : `Section ${section.sectionNo} is not in this filtered answer, which is what `
                + `?active=${active} means for it.`}
          </pre>
        </div>

        <div className="toolbar">
          <Field
            label="Show"
            hint="#30's ?active=. Blank sends no parameter, which is not the same as false."
          >
            <Select label="Show" value={active} onChange={setActive} options={TRISTATE} />
          </Field>
        </div>

        {data ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status">
                {data.sectionCount} in the class · {data.activeCount} active · showing{' '}
                {shown.length}
              </span>
            </div>
            <pre className="resp-body">
              {shown.length === 0
                ? 'no rows match that filter'
                : shown.map((one) => [
                    one.sectionNo,
                    one.capacity ? `cap ${one.capacity}` : 'no capacity',
                    one.classTeacherDocsId ?? 'no class teacher',
                    one.active ? 'active' : 'retired',
                  ].join('  ·  ')).join('\n')}
            </pre>
          </div>
        ) : null}

        <p className="muted">
          <Info size={12} /> The two counts describe the <em>whole class</em> however the rows are
          filtered — "how many does this class have" is not "how many did you ask to see".
        </p>
      </div>
    </Modal>
  )
}

/**
 * Adding a section — #17.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD, because a class gets A, B, C and D in one sitting. Only the
 * number is cleared: capacity and the class teacher usually repeat across a class's sections.
 */
function AddSection({ open, classId, year, onClose, onAdded }) {
  const { call } = useApi()
  const [form, setForm] = useState({ sectionNo: '', capacity: '', classTeacherDocsId: '' })
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)
  const [added, setAdded] = useState(null)

  const set = (field) => (event) =>
    setForm((old) => ({ ...old, [field]: event.target.value }))

  // An empty optional box sends nothing rather than "" — the API would read that as an
  // instruction rather than as "not provided".
  const body = (() => {
    const out = { sectionNo: form.sectionNo }
    if (form.capacity !== '') out.capacity = Number(form.capacity)
    if (form.classTeacherDocsId.trim() !== '') {
      out.classTeacherDocsId = form.classTeacherDocsId.trim()
    }
    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('add-class-section', {
      label: 'Add a section',
      pathParams: { year: year ?? '', id: classId },
      body,
    })
    setSaving(false)
    if (result.ok) {
      setAdded(result.bodyJson)
      setForm((old) => ({ ...old, sectionNo: '' }))
      onAdded()
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
      title="Add a section"
      description="sectionNo can never be changed once records reference it — eight collections store it as a plain string."
      endpoint={<EndpointTag id="add-class-section" name="Add" look="primary" />}
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

        {added ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="true">
                {added.sectionCount} section{added.sectionCount === 1 ? '' : 's'} ·{' '}
                {added.activeCount} active
              </span>
            </div>
            <pre className="resp-body">{added.changeSummary}</pre>
          </div>
        ) : null}

        <Field
          label="Section number"
          required
          hint="Stored exactly as typed — A, Blue, Alpha. Unique in the class, checked case-insensitively."
          error={errors.sectionNo}
        >
          <Input value={form.sectionNo} error={errors.sectionNo}
            onChange={set('sectionNo')} placeholder="A" />
        </Field>

        <div className="field-grid">
          <Field
            label="Capacity"
            hint="Optional, at least 1. A plan, not a limit — nothing enforces it, and 0 is refused."
            error={errors.capacity}
          >
            <Input type="number" value={form.capacity} error={errors.capacity}
              onChange={set('capacity')} placeholder="40" />
          </Field>
          <Field
            label="Class teacher id"
            hint="Optional Staff.id. Checked against this school — another school's real id is a 404."
            error={errors.classTeacherDocsId}
          >
            <Input value={form.classTeacherDocsId} error={errors.classTeacherDocsId}
              onChange={set('classTeacherDocsId')} placeholder="67aa15d9dc3f7d0011111111" />
          </Field>
        </div>

        <p className="muted">
          <Users size={12} /> The number is unique only within this class — another class may have
          its own "A". Nothing can be placed in a class until a section exists.
        </p>
      </div>
    </Modal>
  )
}
