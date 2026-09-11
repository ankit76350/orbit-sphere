import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Info, Plus, RefreshCw, Users } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field } from '../../../components/ui/Kit.jsx'
import AddSubject from './AddSubject.jsx'
import { detailPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * One section, at its own address:
 * /school-academics/classes/{id}/sections/{sectionNo}
 *
 * ADDRESSED BY sectionNo, NOT AN ID, because a section has neither. It is embedded in its class:
 * no collection, no document id, no schoolId of its own. `sectionNo` is the one thing eight other
 * collections store about it, so it is the only thing that can name one — and it is why the
 * number can never be changed.
 *
 * #22 WRITES FROM HERE WITH THE SECTION FIXED, because a subject assigned from a section page
 * is a subject for that section — and because the refusal that matters is reachable this way: if
 * the class already teaches it class-wide, assigning it here is a 409, not a second row.
 *
 * TWO READS, AND THE SECOND ONE IS A STAND-IN.
 *
 *   #30  GET /classes/{id}/sections   the section itself — the endpoint that owns it, and where
 *                                     ?active= lives
 *   #29  GET /classes/{id}            the subjects, because nothing else returns them
 *
 * #29 alone would answer both, and that is worth saying rather than hiding: the page asks #30
 * anyway because that is the endpoint a section belongs to, and reading a section from the class
 * read would make this page depend on a shape it does not own.
 *
 * THE SUBJECT FILTERING HAPPENS IN THE BROWSER, and that is a gap rather than a design.
 * #31 — GET /classes/{id}/subjects?sectionNo= — is the endpoint that would do it in the
 * database, and it is not built. The page says so where the filtering happens.
 *
 * A SECTION'S SUBJECTS ARE ITS OWN PLUS THE CLASS-WIDE ONES. A row with no sectionNo applies to
 * every section, so filtering to `sectionNo === X` alone would hide most of what the section
 * studies — the trap #31's entry in the README already warns about.
 */

const TRISTATE = ['', 'true', 'false']

export default function SectionDetail() {
  const { id, sectionNo } = useParams()
  const { call } = useApi()
  const { environment, actingSubdomain, actingAcademicYear } = useApiState()

  const [list, setList] = useState(null)
  const [klass, setKlass] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [active, setActive] = useState('')
  const [assigning, setAssigning] = useState(false)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    // In parallel: neither depends on the other, and they read the same document anyway.
    const [sections, whole] = await Promise.all([
      call('list-class-sections', {
        label: "The class's sections",
        pathParams: { year: actingAcademicYear ?? '', id },
        query: active ? { active } : {},
      }),
      call('get-school-class', {
        label: 'The class, for its subjects',
        pathParams: { year: actingAcademicYear ?? '', id },
      }),
    ])
    setLoading(false)
    if (sections.ok) { setList(sections.bodyJson); setProblem(null) } else { setProblem(sections) }
    if (whole.ok) setKlass(whole.bodyJson)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, actingAcademicYear, id, active])

  useEffect(() => { load() }, [load])

  if (!actingSubdomain) return <NoSchoolChosen what="This section" />

  const classPath = detailPath('school', 'academics', 'classes', id)
  const section = (list?.sections ?? []).find((one) => one.sectionNo === sectionNo)

  if (problem) {
    return (
      <div className="page stack">
        <Link className="back" to={classPath}><ArrowLeft size={13} /> The class</Link>
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message || 'Nothing came back.'}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        </Card>
      </div>
    )
  }

  // Its own rows, plus the class-wide ones — see the note at the top of this file.
  const mine = (klass?.subjects ?? []).filter(
    (one) => one.sectionNo === sectionNo || !one.sectionNo,
  )

  return (
    <div className="page stack">
      <Link className="back" to={classPath}>
        <ArrowLeft size={13} /> {klass?.name ?? 'The class'}
      </Link>

      <div className="toolbar">
        <div>
          <h1 className="page-title">Section {sectionNo}</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span>
            {klass ? <> · <span className="mono">{klass.academicYear}</span></> : null}
            {klass ? ` · ${klass.name}` : ''}
            {list ? ` · ${list.sectionCount} section${list.sectionCount === 1 ? '' : 's'} in the class` : ''}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <EndpointTag id="list-class-sections" name="The section" />
        <EndpointTag id="get-school-class" name="Its subjects" />
        <Button look="primary" icon={Plus} onClick={() => setAssigning(true)}>
          Add a subject
        </Button>
      </div>

      <Card
        title="The section"
        description="From #30 — the endpoint a section belongs to, and the one a 'move this student' dropdown would use."
        action={
          <Field
            label="Show"
            hint="#30's ?active=. Blank sends no parameter, which is not the same as false."
          >
            <Select label="Show" value={active} onChange={setActive} options={TRISTATE} />
          </Field>
        }
      >
        {section ? (
          <div className="table-scroll">
            <table className="data-table">
              <tbody>
                {/* The display value as well as the reference, which is why there is no
                    separate name to show — and why it can never be changed. */}
                <tr><td className="muted">Section</td>
                  <td><span className="mono">{section.sectionNo}</span></td></tr>
                <tr><td className="muted">Capacity</td>
                  <td>{section.capacity ?? <span className="muted">no plan recorded</span>}</td></tr>
                {/* A raw Staff.id. Read the staff record when a name is needed — one place
                    should decide how a teacher is presented. */}
                <tr><td className="muted">Class teacher</td>
                  <td>{section.classTeacherDocsId
                    ? <span className="mono">{section.classTeacherDocsId}</span>
                    : <span className="muted">none</span>}</td></tr>
                <tr><td className="muted">Status</td>
                  <td>
                    <Badge tone={section.active ? 'good' : undefined}>
                      {section.active ? 'active' : 'retired'}
                    </Badge>
                  </td></tr>
              </tbody>
            </table>
          </div>
        ) : (
          <Empty
            title={active ? `Not in this filtered answer` : 'No such section'}
            description={active
              ? `Section ${sectionNo} exists, but ?active=${active} excludes it — which is what `
                + 'that filter means for it. Clear the filter to see it.'
              : `This class has no section '${sectionNo}'. A section number is unique only `
                + 'within its class, so another class may well have one.'}
          />
        )}
        <p className="muted">
          <Info size={12} /> {list ? `${list.sectionCount} in the class · ${list.activeCount} active` : ''}
          {' '}— those counts describe the whole class however the rows are filtered.
        </p>
      </Card>

      <Card
        title="What this section studies"
        description="Its own subject assignments, plus the class-wide ones — a row with no section applies to every section."
        action={
          <div className="btn-row">
            <Badge>{mine.length} applying</Badge>
            <Button icon={Plus} onClick={() => setAssigning(true)}>Add</Button>
          </div>
        }
      >
        {mine.length === 0 ? (
          <Empty
            title="Nothing is taught in this class yet"
            description="Neither this section's own rows nor the class-wide ones. #22 adds either, and from here it adds this section's."
            action={
              <Button look="primary" icon={Plus} onClick={() => setAssigning(true)}>
                Assign the first
              </Button>
            }
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Name</th>
                  <th>Type</th>
                  <th>Applies</th>
                  <th>Teachers</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {mine.map((one) => (
                  <tr key={`${one.subjectCode}/${one.sectionNo ?? 'all'}`}>
                    <td><span className="mono">{one.subjectCode}</span></td>
                    <td>
                      {one.name}
                      {one.shortName ? <span className="muted"> ({one.shortName})</span> : null}
                    </td>
                    <td>{one.subjectType}</td>
                    <td>
                      {one.sectionNo
                        ? <Badge tone="brand">this section</Badge>
                        : <span className="muted">the whole class</span>}
                    </td>
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
        <p className="muted">
          <Info size={12} /> This list is filtered <b>in the browser</b>, from the whole class
          read. <span className="mono">GET /classes/{'{id}'}/subjects?sectionNo=</span> is #31 and
          is not built — it is the endpoint that would do this in the database.
        </p>
      </Card>

      {/* Fixed to this section: a subject assigned from a section's page is that section's,
          and the modal hides the box rather than pre-filling one that could be edited away. */}
      <AddSubject
        open={assigning}
        classId={id}
        year={actingAcademicYear}
        fixedSection={sectionNo}
        onClose={() => setAssigning(false)}
        onAdded={() => load()}
      />

      <Card title="What this page cannot tell you">
        <p className="muted">
          <Users size={12} /> <b>Which students are in this section.</b> Nothing records that yet —
          a student record stores <span className="mono">sectionNo</span>, and the student module
          has no API. This section is the thing that was blocking it.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Whether a student actually takes an elective.</b> A subject listed
          here is what the class <em>offers</em>. Nothing records a per-student choice, so a
          register built from this list would include students who do not sit the subject.
        </p>
      </Card>
    </div>
  )
}
