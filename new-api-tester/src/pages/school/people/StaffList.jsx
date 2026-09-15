import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronRight, Info, Plus, RefreshCw, Search } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import { detailPath } from '../../../paths.js'
import NoSchoolChosen from '../NoSchoolChosen.jsx'

/**
 * The people a school employs: /school-people/staff
 *
 * TWO ENDPOINTS. #1 creates a person and #7 lists them, so this table is the SCHOOL'S answer
 * rather than a memory of what this tab wrote — the session-only shape it had until 2026-09-15 is
 * gone, and a new person appears here by re-reading.
 *
 * THE ROW IS THIN, AND THE PAGE SAYS WHY. No date of birth, no address, no emergency contact —
 * those are on #8, which a row opens. A list carrying them would put every employee's personal
 * data in the network tab of every dropdown, and this module has no authorization yet.
 *
 * THE FOUR FILTERS A PICKER ACTUALLY WANTS ARE ABSENT, and the page states that rather than
 * leaving a reader to wonder where "department" went: they live on EmploymentRecord, which #16
 * writes and which does not exist.
 *
 * employeeNo LEADS EVERY ROW, because it is the thing the school writes down and the one field on
 * the response the caller did not send.
 *
 * THIS CREATES A PERSON, NOT AN EMPLOYEE. No status, no department, no joining date — none exist
 * on the document. The page says so under the table rather than leaving a reader to wonder where
 * the "active" column went.
 *
 * THREE REQUIRED FIELDS, AND THE PLAN SAID ONE. The model declares dateOfBirth and gender
 * @NotNull; the form marks all three required and the hint says which decision that was, because
 * somebody reading the plan will expect a name to be enough.
 */

const GENDERS = ['FEMALE', 'MALE', 'OTHER']
const GENDER_FILTER = ['', 'FEMALE', 'MALE', 'OTHER']
const TRISTATE = ['', 'true', 'false']
const SORTS = ['', 'fullName', 'fullName,desc', 'employeeNo', 'employeeNo,desc',
  'createdAt,desc', 'updatedAt,desc']
const SIZES = ['5', '20', '100']

const BLANK = {
  fullName: '',
  dateOfBirth: '',
  gender: 'FEMALE',
  nationalityCode: '',
  preferredLanguage: '',
  phoneNumber: '',
  emailAddress: '',
  addressLine1: '',
  city: '',
  postalCode: '',
  countryCode: '',
  contactName: '',
  contactRelationship: '',
  contactPhone: '',
}

export default function StaffList() {
  const { call } = useApi()
  const navigate = useNavigate()
  const { environment, actingSubdomain } = useApiState()
  const [open, setOpen] = useState(false)

  const [typed, setTyped] = useState('')
  const [search, setSearch] = useState('')
  const [gender, setGender] = useState('')
  const [nationalityCode, setNationalityCode] = useState('')
  const [hasEmail, setHasEmail] = useState('')
  const [hasPhone, setHasPhone] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState('20')

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  const query = useCallback(() => {
    // An empty box sends nothing rather than "", which the API would read as a value.
    const out = { page: String(page), size }
    if (search.trim() !== '') out.search = search.trim()
    if (gender) out.gender = gender
    if (nationalityCode.trim() !== '') out.nationalityCode = nationalityCode.trim()
    if (hasEmail) out.hasEmail = hasEmail
    if (hasPhone) out.hasPhone = hasPhone
    if (sort) out.sort = sort
    return out
  }, [search, gender, nationalityCode, hasEmail, hasPhone, sort, page, size])

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-staff', { label: 'The staff list', query: query() })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, query])

  useEffect(() => { load() }, [load])

  const rows = data?.content ?? []

  if (!actingSubdomain) return <NoSchoolChosen what="Staff" />

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Staff</h1>
          <p className="muted">
            <span className="mono">{actingSubdomain}</span> · the people a school employs ·
            a person here is not employed until #16 writes the job
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add a person</Button>
      </div>

      <Card
        title="Filters"
        description="All five are AND-ed, and blank sends nothing at all — which is not the same as sending false."
        action={<EndpointTag id="list-staff" name="List" query={query()} />}
      >
        <div className="stack">
          <div className="toolbar">
            <Field label="Search" hint="Matches fullName OR employeeNo, case-insensitive, anywhere. A stray '(' is an empty answer, not a 500.">
              <Input value={typed} onChange={(e) => setTyped(e.target.value)}
                placeholder="Anita, or 000001" />
            </Field>
            <Button icon={Search} onClick={() => { setSearch(typed); setPage(0) }}>Search</Button>
            <Button onClick={() => { setSearch(''); setTyped(''); setPage(0) }}>Clear</Button>
            <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
          </div>

          <div className="field-grid">
            <Field label="Gender" hint="MALE | FEMALE | OTHER. Blank returns every gender.">
              <Select label="Gender" value={gender}
                onChange={(value) => { setGender(value); setPage(0) }} options={GENDER_FILTER} />
            </Field>
            <Field label="Nationality code" hint="Case-insensitive — #1 upper-cases on the way in, and a caller should not have to know that.">
              <Input value={nationalityCode}
                onChange={(e) => { setNationalityCode(e.target.value); setPage(0) }}
                placeholder="IN" />
            </Field>
            <Field label="Has email" hint="false is the interesting one: who are we missing contact details for. Blank returns BOTH.">
              <Select label="Has email" value={hasEmail}
                onChange={(value) => { setHasEmail(value); setPage(0) }} options={TRISTATE} />
            </Field>
            <Field label="Has phone" hint="The same, for a phone number.">
              <Select label="Has phone" value={hasPhone}
                onChange={(value) => { setHasPhone(value); setPage(0) }} options={TRISTATE} />
            </Field>
          </div>

          <div className="toolbar">
            <Field label="Sort" hint="Tiebroken by employeeNo — two people share a name, and a tie puts one row on two pages. A field the row HIDES cannot be sorted on: ?sort=dateOfBirth is a 400.">
              <Select label="Sort" value={sort}
                onChange={(value) => { setSort(value); setPage(0) }} options={SORTS} />
            </Field>
            <Field label="Page size" hint="Defaults to 20, capped at 100. 0 and 101 are refused, never clamped.">
              <Select label="Page size" value={size}
                onChange={(value) => { setSize(value); setPage(0) }} options={SIZES} />
            </Field>
            <span className="toolbar-spacer" />
            <Button onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <span className="muted">page {page}</span>
            <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </div>
      </Card>

      <Card
        title="Staff"
        description="From #7 — the school's own answer, ordered by name then employeeNo. The counts describe every match, not this page."
        action={
          <div className="btn-row">
            <EndpointTag id="create-staff" name="Add a person" />
            <Badge>{data?.totalElements ?? 0} matching</Badge>
            <Button icon={Plus} onClick={() => setOpen(true)}>Add</Button>
          </div>
        }
      >
        {problem ? (
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message || 'Nothing came back.'}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        ) : rows.length === 0 ? (
          <Empty
            title="Nobody matches"
            description="An empty page, never a 404. Clear the filters to see whether the school has anybody at all."
            action={<Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add one</Button>}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Employee no</th>
                  <th>Name</th>
                  <th>Gender</th>
                  <th>Phone</th>
                  <th>Email</th>
                  <th>Nationality</th>
                  <th>Staff id</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((one) => (
                  /* OPENING ONE IS A DELIBERATE ACT, and the page it opens carries a date of
                     birth and a home address that this row deliberately does not. */
                  <tr
                    key={one.staffDocsId}
                    data-opens
                    onClick={() => navigate(detailPath('school', 'people', 'staff',
                      one.staffDocsId))}
                  >
                    {/* LEADS, because it is the thing the school writes down. */}
                    <td><span className="mono">{one.employeeNo}</span></td>
                    <td>{one.fullName}</td>
                    <td>{one.gender}</td>
                    <td>{one.phoneNumber
                      ? <span className="mono">{one.phoneNumber}</span>
                      : <span className="muted">none</span>}</td>
                    <td>{one.emailAddress ?? <span className="muted">none</span>}</td>
                    <td>{one.nationalityCode ?? <span className="muted">none</span>}</td>
                    <td><span className="muted mono">{one.staffDocsId}</span></td>
                    <td><span className="muted">Open <ChevronRight size={13} /></span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="muted">
          <Info size={12} /> <b>There is no date of birth, address or emergency contact on this
          table, and that is the security decision on #7.</b> A list carrying them would put every
          employee&apos;s personal data in the network tab of every dropdown that reads it — and
          this module has no authorization yet. They are on{' '}
          <span className="mono">GET /staff/{'{id}'}</span> — #8, which is what a row opens.
        </p>
        <p className="muted">
          <Info size={12} /> <b>There is no department, position or &quot;employed&quot;
          filter</b>, which are the four a teacher picker actually wants. All of them live on{' '}
          <span className="mono">EmploymentRecord</span>, which #16 writes and which does not
          exist yet — sending one is ignored and narrows nothing, rather than looking like a filter
          that found no teachers.
        </p>
        <p className="muted">
          <Info size={12} /> <b>There is no status column, and there never will be on this
          document.</b> A person is not employed by existing — no status, no department, no
          joining date. All of that is <span className="mono">EmploymentRecord</span>, and the
          separation is this package&apos;s whole design.
        </p>
      </Card>

      <AddStaff
        open={open}
        onClose={() => setOpen(false)}
        onAdded={load}
      />
    </div>
  )
}

/**
 * Adding a person — #1.
 *
 * STAYS OPEN AFTER A SUCCESSFUL ADD, because a school enters its roll in one sitting. Only the
 * identifying fields are cleared; the country, language and address usually repeat down a list of
 * colleagues.
 *
 * NO employeeNo BOX, and that is not an omission. It is generated per school and atomically —
 * sending one is ignored rather than refused, and offering a box would suggest otherwise. The
 * response says what was allocated.
 *
 * THE ADDRESS IS FLAT HERE AND NESTED IN THE BODY. A form of nested fieldsets for two optional
 * objects reads worse than four boxes; the preview shows the real shape, which is what a tester
 * needs to see.
 */
function AddStaff({ open, onClose, onAdded }) {
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
    const out = { fullName: form.fullName, dateOfBirth: form.dateOfBirth, gender: form.gender }
    for (const field of ['nationalityCode', 'preferredLanguage', 'phoneNumber', 'emailAddress']) {
      if (form[field].trim() !== '') out[field] = form[field].trim()
    }

    const address = {}
    if (form.addressLine1.trim() !== '') address.addressLine1 = form.addressLine1.trim()
    if (form.city.trim() !== '') address.city = form.city.trim()
    if (form.postalCode.trim() !== '') address.postalCode = form.postalCode.trim()
    if (form.countryCode.trim() !== '') address.countryCode = form.countryCode.trim()
    if (Object.keys(address).length > 0) out.currentAddress = address

    const contact = {}
    if (form.contactName.trim() !== '') contact.fullName = form.contactName.trim()
    if (form.contactRelationship.trim() !== '') contact.relationship = form.contactRelationship.trim()
    if (form.contactPhone.trim() !== '') contact.phoneNumber = form.contactPhone.trim()
    if (Object.keys(contact).length > 0) out.emergencyContact = contact

    return out
  })()

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const result = await call('create-staff', { label: 'Add a person', body })
    setSaving(false)
    if (result.ok) {
      setMade(result.bodyJson)
      onAdded(result.bodyJson)
      setForm((old) => ({ ...old, fullName: '', dateOfBirth: '', emailAddress: '', phoneNumber: '' }))
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
      title="Add a person"
      description="A person, not an employee — #16 writes the job. The employee number is generated, so there is no box for it."
      endpoint={<EndpointTag id="create-staff" name="Add" look="primary" />}
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
              {/* The employee number, because it is what the school writes down. */}
              <span className="resp-status" data-ok="true">{made.employeeNo}</span>
            </div>
            <pre className="resp-body">{made.nextStep}</pre>
          </div>
        ) : null}

        <div className="field-grid">
          <Field
            label="Full name"
            required
            hint="The only field the module plan called mandatory."
            error={errors.fullName}
          >
            <Input value={form.fullName} error={errors.fullName}
              onChange={set('fullName')} placeholder="Anita Sharma" />
          </Field>
          <Field
            label="Date of birth"
            required
            hint="Required because Staff.java declares it @NotNull — the plan said a name alone was enough, and the model won. Must be in the past."
            error={errors.dateOfBirth}
          >
            <Input type="date" value={form.dateOfBirth} error={errors.dateOfBirth}
              onChange={set('dateOfBirth')} />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Gender"
            required
            hint="The shared Gender, not a people-specific one. @NotNull on the model, same decision as the date of birth."
            error={errors.gender}
          >
            <Select label="Gender" value={form.gender}
              onChange={(value) => setForm((old) => ({ ...old, gender: value }))}
              options={GENDERS} />
          </Field>
          <Field
            label="Phone number"
            hint="Spacing characters are stripped, and the number is UNIQUE within the school — checked after stripping, so retyping it with different spacing still collides. A number without a + is stored AS GIVEN; no country code is invented."
            error={errors.phoneNumber}
          >
            <Input value={form.phoneNumber} error={errors.phoneNumber}
              onChange={set('phoneNumber')} placeholder="+91 98765-43210" />
          </Field>
        </div>

        <div className="field-grid">
          <Field
            label="Email address"
            hint="Trimmed and lower-cased, and UNIQUE within the school — a duplicate is 409 STAFF_EMAIL_TAKEN, checked after lower-casing so another case still collides. Another school may hold the same address."
            error={errors.emailAddress}
          >
            <Input value={form.emailAddress} error={errors.emailAddress}
              onChange={set('emailAddress')} placeholder="anita.sharma@example.com" />
          </Field>
          <Field
            label="Nationality code"
            hint="ISO 3166-1 alpha-2, stored upper-cased."
            error={errors.nationalityCode}
          >
            <Input value={form.nationalityCode} error={errors.nationalityCode}
              onChange={set('nationalityCode')} placeholder="IN" />
          </Field>
        </div>

        <Field
          label="Preferred language"
          hint="IETF language tag."
          error={errors.preferredLanguage}
        >
          <Input value={form.preferredLanguage} error={errors.preferredLanguage}
            onChange={set('preferredLanguage')} placeholder="en-IN" />
        </Field>

        <div className="field-grid">
          <Field label="Address line" hint="Sent nested as currentAddress — see the preview.">
            <Input value={form.addressLine1} onChange={set('addressLine1')}
              placeholder="12 Park Road" />
          </Field>
          <Field label="City"><Input value={form.city} onChange={set('city')} placeholder="Pune" /></Field>
        </div>
        <div className="field-grid">
          <Field label="Postal code">
            <Input value={form.postalCode} onChange={set('postalCode')} placeholder="411001" />
          </Field>
          <Field label="Address country code" hint="Upper-cased on the way in.">
            <Input value={form.countryCode} onChange={set('countryCode')} placeholder="IN" />
          </Field>
        </div>

        <div className="field-grid">
          <Field label="Emergency contact name">
            <Input value={form.contactName} onChange={set('contactName')}
              placeholder="Rakesh Sharma" />
          </Field>
          <Field label="Relationship">
            <Input value={form.contactRelationship} onChange={set('contactRelationship')}
              placeholder="Spouse" />
          </Field>
        </div>
        <Field
          label="Emergency contact phone"
          hint="Normalised the same way the person's own number is."
        >
          <Input value={form.contactPhone} onChange={set('contactPhone')}
            placeholder="+91 98765 11111" />
        </Field>

        <p className="muted">
          <Info size={12} /> <b>An address or contact with every box empty is sent as nothing at
          all</b>, and stored as nothing — an empty address and no address are the same fact. A
          partly filled one is kept: a city and nothing else is real.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Leaving the phone and email empty is always fine, however many
          people you enter.</b> Both unique indexes are <span className="mono">partial</span>, so
          only a value that is actually there has to be unique — a plain unique index would let a
          school hold exactly one person without a phone.
        </p>
        <p className="muted">
          <Info size={12} /> <b>There is no employee-number box.</b> It is generated per school and
          atomically, so two simultaneous creates cannot share one and two schools can both hold{' '}
          <span className="mono">EMP/2026/000001</span>. Send one in the body and it is{' '}
          <b>ignored, not refused</b>.
        </p>
      </div>
    </Modal>
  )
}
