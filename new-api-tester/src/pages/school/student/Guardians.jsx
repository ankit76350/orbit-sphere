import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Info, Plus, RefreshCw, Search, X } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from '../crm/admissionDates.js'
import { detailPath } from '../../../paths.js'

/**
 * Guardians: /school-student/guardians
 *
 * TWO ENDPOINTS — #7 adds a guardian who is not being created with a child, and #9 lists them.
 *
 * #9 IS THE LIST AND THE SEARCH AT ONCE, which is why the filters sit above the table rather than
 * in a modal of their own: sending none of them is the list, and sending one is the check you make
 * before pressing Add. Those are the same act on this screen, and they are the same endpoint.
 *
 * THE FILTERS ARE AND-ED, unlike #6's, which are OR-ed. There the question is "is this one child
 * here" and any of three answers it; here you are narrowing a list.
 *
 * WHAT IS WORTH SEEING HERE is the contrast with Admit a Child. Both endpoints write into one
 * unique index — a phone number identifies one person per school — and they do OPPOSITE things
 * when they meet a number that is taken:
 *
 *   #1 MATCHES it, because a caller admitting a child is describing a family, and refusing would
 *      make a sibling's admission fail for a reason the desk cannot act on.
 *   #7 REFUSES it, because a caller adding a guardian is asserting a new person, and handing back
 *      an existing row would look like a successful create.
 *
 * Add a guardian here, then admit a child naming the same number on the roll screen: the guardian
 * is MATCHED and linked. Add the same number here twice: 409. Both are right.
 *
 * NOTHING IS DISABLED. A blank name, a taken number, a guardian with no details at all — each is
 * a documented answer, and this is the tool for reaching them.
 */

const LOCALES = ['', 'en-IN', 'hi-IN', 'en-GB', 'en-US']

const BLANK_FILTERS = {
  search: '', phone: '', email: '', name: '', occupation: '', address: '', sort: '',
}

export default function Guardians() {
  const { call } = useApi()
  const navigate = useNavigate()
  const { environment, actingSubdomain } = useApiState()
  const [open, setOpen] = useState(false)
  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)

  //! TWO COPIES OF THE FILTERS, and that is the whole of the Search button. `draft` is what the
  //! boxes hold, `applied` is what the last press asked for, and only `applied` is in the load
  //! effect's deps. They were one object until 2026-10-08, so every keystroke in the name box was
  //! a round trip — and in an API tester that is worse than slow: the request log filled with
  //! requests nobody made, which is the one thing this screen exists to show clearly.
  const [draft, setDraft] = useState(BLANK_FILTERS)
  const [applied, setApplied] = useState(BLANK_FILTERS)

  const load = useCallback(async () => {
    if (!actingSubdomain) return
    setLoading(true)
    const result = await call('list-guardians', {
      label: 'The guardians',
      //! `query`, NOT `queryParams`. buildCall reads options.query and silently ignores anything
      //! else — a wrong name sends no filters AND no paging, and the list still looks like it
      //! works because the server has defaults for both.
      query: {
        page: String(page), size: '20',
        //! SENT ONLY WHEN THERE IS SOMETHING IN IT. An empty filter means "do not narrow on
        //! this" — and with all of them empty the answer is every guardian, which is the point:
        //! #9 is the only read of this collection, so it has to be able to list it.
        ...Object.fromEntries(Object.entries(applied).filter(([, v]) => v !== '')),
      },
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, actingSubdomain, page, applied])

  useEffect(() => { load() }, [load])

  //! A SEARCH ALWAYS GOES BACK TO PAGE ONE. Staying on page 3 of the old answer and asking for
  //! page 3 of a new one is how somebody lands on an empty page and reads it as "no matches".
  const apply = () => { setPage(0); setApplied(draft) }
  const clear = () => { setPage(0); setDraft(BLANK_FILTERS); setApplied(BLANK_FILTERS) }

  const activeCount = Object.values(applied).filter((v) => v !== '').length
  const dirty = JSON.stringify(draft) !== JSON.stringify(applied)

  if (!actingSubdomain) return <NoSchoolChosen what="Guardians" />

  const rows = data?.content ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Guardians</h1>
          <p className="muted">
            The people a school contacts about its children. <b>One real person per school</b> —
            their phone number says so, and the database enforces it.
          </p>
        </div>
        <span className="toolbar-spacer" />
        <EndpointTag id="list-guardians" name="List" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add a guardian</Button>
      </div>

      <Card
        title="Filters"
        description="#9 — every one optional, nothing is sent until you press Search, and an empty form is the whole list. They narrow: sending two gives you what matches both."
        action={<EndpointTag id="list-guardians" name="Read" />}
      >
        {/* ENTER SEARCHES. A form rather than a div, so the key that means "go" in every other
            search box on earth means it here too — and the button is the submit, which is why it
            needs no onClick of its own. */}
        <form onSubmit={(e) => { e.preventDefault(); apply() }}>
          <div className="field-grid">
            <Field label="Name, phone or email"
              hint="ONE BOX, OR-ED across the three. Loose on purpose — &ldquo;9876&rdquo; finds any number containing it. The precise boxes below are for when you know which field you mean.">
              <Input value={draft.search} placeholder="rao"
                onChange={(e) => setDraft({ ...draft, search: e.target.value })} />
            </Field>
            <Field label="Phone"
              hint="Matched on its digits, across BOTH numbers. This is the check you make before pressing Add — whatever it finds, #7 refuses.">
              <Input value={draft.phone} placeholder="098765 11111"
                onChange={(e) => setDraft({ ...draft, phone: e.target.value })} />
            </Field>
            <Field label="Email" hint="Whole and case-insensitive — a question about identity, unlike the box above it.">
              <Input value={draft.email} placeholder="parent@example.com"
                onChange={(e) => setDraft({ ...draft, email: e.target.value })} />
            </Field>
            <Field label="Name" hint="Matched anywhere. A name is not an identifier.">
              <Input value={draft.name} placeholder="rao"
                onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
            </Field>
            <Field label="Occupation" hint="Matched anywhere — it is typed free-hand, so nobody can spell it the way it was stored.">
              <Input value={draft.occupation} placeholder="teacher"
                onChange={(e) => setDraft({ ...draft, occupation: e.target.value })} />
            </Field>
            <Field label="Address"
              hint="Matched anywhere. THE ONE A SCHOOL ACTUALLY ASKS: everybody in one village, so a bus route change can be rung round.">
              <Input value={draft.address} placeholder="Barachatti"
                onChange={(e) => setDraft({ ...draft, address: e.target.value })} />
            </Field>
            <Field label="Sort"
              hint="fullName · createdAt · updatedAt. Anything else is 400 — the allowlist is a security control, which is why address is filterable but not sortable.">
              <Input value={draft.sort} placeholder="createdAt,desc"
                onChange={(e) => setDraft({ ...draft, sort: e.target.value })} />
            </Field>
          </div>

          <div className="toolbar">
            <span className="muted">
              {activeCount === 0
                ? 'No filters — this is every guardian in the school.'
                : `${activeCount} filter${activeCount === 1 ? '' : 's'} applied.`}
              {dirty ? ' Edited since the last search.' : ''}
            </span>
            <span className="toolbar-spacer" />
            {/* NEVER DISABLED, even with nothing typed and nothing changed: pressing Search on an
                empty form is a real request — the whole list — and this is the tool for making
                requests on purpose. */}
            <Button icon={X} onClick={clear}>Clear</Button>
            <Button look="primary" icon={Search} type="submit" busy={loading}>Search</Button>
          </div>
        </form>

        <p className="muted">
          <Info size={12} /> <b>Whatever the phone filter finds, Add a guardian refuses.</b> They
          disagreed until 2026-10-07 — #9 compared digits and #7 compared the stored string, so one
          number gave &ldquo;found 1&rdquo; here and <span className="mono">201</span> there, which
          is a duplicate human. Type a number you can see in the table and press Add to watch the
          409.
        </p>
        <p className="muted">
          <Info size={12} /> <b>The search box is the only one that ORs, and it ORs within
          itself.</b> Type a name there and also type an occupation and you mean both — every
          filter beside it narrows. That is the opposite of a &ldquo;find this one person&rdquo;
          search, and deliberately so: here you are filtering a list.
        </p>
      </Card>

      <Card
        title={data ? `${data.totalElements} guardian${data.totalElements === 1 ? '' : 's'}` : 'Guardians'}
        description="Everything on the document — nothing here is joined, so a row costs no more than a list of names would."
      >
        {problem ? (
          <div className="resp">
            <div className="resp-head">
              <span className="resp-status" data-ok="false">
                {problem.bodyJson?.code ?? problem.status}
              </span>
            </div>
            <pre className="resp-body">{problem.bodyJson?.message ?? problem.bodyText}</pre>
          </div>
        ) : rows.length === 0 ? (
          <Empty
            title={activeCount > 0 ? 'Nothing matches' : 'No guardians yet'}
            description="An empty page, never a 404. Add one here, or admit a child — #1 creates most of a school's guardians as a side effect."
            action={<Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add one</Button>}
          />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Guardian</th>
                  <th>Phone</th>
                  <th>Alternate</th>
                  <th>Email</th>
                  <th>Occupation</th>
                  <th>Guardian id</th>
                  <th>Added</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((g) => (
                  // Opening a row is its own address, so it can be linked and reloaded — and #10
                  // is the only endpoint that says what this person is to each of their children.
                  <tr key={g.guardianDocsId} data-opens
                    onClick={() => navigate(detailPath('school', 'student', 'guardians',
                      g.guardianDocsId))}>
                    <td>{g.fullName}</td>
                    <td><span className="mono">{g.phoneNumber ?? '—'}</span></td>
                    <td><span className="mono muted">{g.alternatePhoneNumber ?? '—'}</span></td>
                    <td>{g.emailAddress ?? <span className="muted">none</span>}</td>
                    <td>{g.occupation ?? <span className="muted">—</span>}</td>
                    <td><span className="mono muted">{g.guardianDocsId}</span></td>
                    <td title={g.createdAt}>{readable(g.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="toolbar">
          <Button onClick={() => setPage((p) => Math.max(0, p - 1))}>Previous</Button>
          <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          <span className="toolbar-spacer" />
          <Badge>page {(data?.page ?? 0) + 1} of {data?.totalPages ?? 1}</Badge>
        </div>
      </Card>

      <Card
        title="Why this is separate from admitting a child"
        description="Both write into one unique index, and they do opposite things when they meet a number that is taken."
      >
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr><th>Endpoint</th><th>The caller is saying</th><th>A taken number</th></tr>
            </thead>
            <tbody>
              <tr>
                <td><b>#1</b> Admit a Child</td>
                <td>&ldquo;this child&rsquo;s father is on 98765 43210&rdquo; — describing a family</td>
                <td><Badge tone="good">matched and linked</Badge></td>
              </tr>
              <tr>
                <td><b>#7</b> Add a guardian</td>
                <td>&ldquo;add this guardian&rdquo; — asserting a new person</td>
                <td><Badge tone="bad">409 GUARDIAN_PHONE_TAKEN</Badge></td>
              </tr>
            </tbody>
          </table>
        </div>
        <p className="muted">
          <Info size={12} /> <b>Both are right.</b> Refusing in #1 would make a sibling&rsquo;s
          admission fail for a reason the front desk cannot act on — two siblings share a father.
          Matching in #7 would look like a successful create and leave somebody believing a
          guardian exists that does not.
        </p>
        <p className="muted">
          <Info size={12} /> <b>Worth doing in this order:</b> add a guardian here, then admit a
          child on the roll naming the same number. The guardian comes back{' '}
          <span className="mono">matched</span> with the id you just made — and their stored name is
          kept, not overwritten by whatever the admission form said.
        </p>
      </Card>

      <Card
        title="What cannot be done here yet"
        description="Said plainly rather than drawn as an empty table."
      >
        <p className="muted">
          <b>#11 attaches a guardian to a child, #12 changes the flags and #13 detaches</b> —
          unlinks, never deletes, because the same row may be three other children&rsquo;s mother.
          None is built. Neither is <b>#8</b>, which corrects a guardian <i>for every child linked
          to them</i>.
          </p>
          <p className="muted">
          There is still no &ldquo;attached to nobody&rdquo; filter, which is the one worth having:
          #7 creates guardians attached to no child, and the answer to <i>which</i> lives in{' '}
          <span className="mono">students</span>, a second collection this endpoint does not read.
          #10 crossing that boundary did not solve it — it reads <i>one</i> guardian&rsquo;s
          children through an index seek, where a filter here needs the opposite question asked of
          every guardian at once: collect every attached id in the school, then send them back as
          an <span className="mono">$in</span> that grows with the roll rather than with the page.
        </p>
        <p className="muted">
          <b>#8 corrects a guardian</b>, and that one changes it <i>for every child linked to
          them</i>, which is the whole point of the shared row. <b>#11 to #13</b> attach a guardian
          to a child, change the flags, and detach — <b>detach unlinks and never deletes</b>, because
          the same row may be three other children&rsquo;s mother.
        </p>
        <p className="muted">
          <Info size={12} /> Until #11 exists, a guardian added here is <b>attached to nobody</b>.
          That is a real state rather than a broken one — an emergency number on file before the
          child arrives is exactly what this endpoint is for.
        </p>
      </Card>

      {open ? (
        <AddGuardian onClose={() => setOpen(false)} onAdded={load} />
      ) : null}
    </div>
  )
}

/** #7 — add a guardian. No relation and no flags: those belong to the link, which is #11. */
function AddGuardian({ onClose, onAdded }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    fullName: '', phoneNumber: '', alternatePhoneNumber: '', emailAddress: '',
    address: '', occupation: '', preferredLanguage: '',
  })
  const [sending, setSending] = useState(false)
  const [result, setResult] = useState(null)

  //! BUILT IN RENDER so the JSON pane and the request are one object. An empty optional is
  //! omitted rather than sent as "": this is a create, where "" is not "no value" but a value
  //! nobody meant — and preferredLanguage is a closed set, where "" is a 400.
  const body = {
    fullName: form.fullName,
    ...(form.phoneNumber ? { phoneNumber: form.phoneNumber } : {}),
    ...(form.alternatePhoneNumber ? { alternatePhoneNumber: form.alternatePhoneNumber } : {}),
    ...(form.emailAddress ? { emailAddress: form.emailAddress } : {}),
    ...(form.address ? { address: form.address } : {}),
    ...(form.occupation ? { occupation: form.occupation } : {}),
    ...(form.preferredLanguage ? { preferredLanguage: form.preferredLanguage } : {}),
  }

  const send = async () => {
    setSending(true)
    const answer = await call('create-guardian', { label: 'Add a guardian', body })
    setSending(false)
    setResult(answer)
    if (answer.ok) onAdded(answer.bodyJson)
  }

  return (
    <Modal
      open
      onClose={onClose}
      title="Add a guardian"
      description="A person who is not being created with a child. No relation and no flags — those are facts about a person AND a child together, and they live on the link (#11)."
      endpoint={<EndpointTag id="create-guardian" name="Add" look="primary" />}
      previewLabel="WHAT WILL BE SENT"
      preview={body}
      footer={<Button look="primary" onClick={send} busy={sending}>Send it</Button>}
    >
      <div className="field-grid">
        <Field label="Full name" required>
          <Input value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </Field>
        <Field label="Phone"
          hint="THE IDENTITY of a guardian in this school. Stored without spaces and brackets. A number somebody already has is 409 — unlike #1, which matches it.">
          <Input value={form.phoneNumber}
            onChange={(e) => setForm({ ...form, phoneNumber: e.target.value })}
            placeholder="+91 98765 43210" />
        </Field>
        <Field label="Email" hint="Unique per school the same way. Stored lowercase.">
          <Input value={form.emailAddress}
            onChange={(e) => setForm({ ...form, emailAddress: e.target.value })} />
        </Field>
        <Field label="Alternate phone"
          hint="NOT unique and not checked — a family shares one landline, and refusing it would make a mother and a father impossible to enter.">
          <Input value={form.alternatePhoneNumber}
            onChange={(e) => setForm({ ...form, alternatePhoneNumber: e.target.value })} />
        </Field>
        <Field label="Occupation">
          <Input value={form.occupation}
            onChange={(e) => setForm({ ...form, occupation: e.target.value })} />
        </Field>
        <Field label="Preferred language" hint="A closed set. Empty is omitted, not sent as ''.">
          <Select value={form.preferredLanguage} options={LOCALES}
            onChange={(v) => setForm({ ...form, preferredLanguage: v })} />
        </Field>
        <Field label="Address" wide>
          <Input value={form.address}
            onChange={(e) => setForm({ ...form, address: e.target.value })} />
        </Field>
      </div>

      <p className="muted">
        <Info size={12} /> <b>A guardian with no phone and no email is allowed</b>, and you can add
        two of them. Nothing identifies them, so a family that gives no details will slowly collect
        duplicates — known and accepted rather than solved badly.
      </p>

      {result ? (
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok={result.ok ? 'true' : 'false'}>
              {result.ok ? `${result.status} Created` : (result.bodyJson?.code ?? result.status)}
            </span>
          </div>
          {result.ok ? (
            <p className="muted">
              <b>{result.bodyJson?.fullName}</b> is on file as{' '}
              <span className="mono">{result.bodyJson?.guardianDocsId}</span>, attached to nobody.
              Now admit a child naming{' '}
              <span className="mono">{result.bodyJson?.phoneNumber ?? 'that number'}</span> and
              watch #1 match them instead of making a second row.
            </p>
          ) : (
            <pre className="resp-body">{result.bodyJson?.message ?? result.bodyText}</pre>
          )}
        </div>
      ) : null}
    </Modal>
  )
}
