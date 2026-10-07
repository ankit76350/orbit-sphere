import { useState } from 'react'
import { Info, Plus } from 'lucide-react'
import { useApiState } from '../../../api/apiContext.js'
import { useApi } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import NoSchoolChosen from '../NoSchoolChosen.jsx'
import { readable } from '../crm/admissionDates.js'

/**
 * Guardians: /school-student/guardians
 *
 * ONE ENDPOINT — #7, add a guardian who is not being created with a child.
 *
 * THERE IS NO LIST ON THIS SCREEN, and that is deliberate rather than unfinished. #9 finds a
 * guardian and #10 opens one; neither is built, so a table here would be an empty table claiming
 * the school has no guardians when it may have hundreds. The page says what it cannot do instead
 * — the same call Inquiries made before #13 existed.
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

export default function Guardians() {
  const { actingSubdomain } = useApiState()
  const [open, setOpen] = useState(false)
  const [added, setAdded] = useState([])

  if (!actingSubdomain) return <NoSchoolChosen what="Guardians" />

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
        <EndpointTag id="create-guardian" name="Add" />
        <Button look="primary" icon={Plus} onClick={() => setOpen(true)}>Add a guardian</Button>
      </div>

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

      {added.length > 0 ? (
        <Card
          title={`Added in this session — ${added.length}`}
          description="Not a list of the school's guardians; this screen has no way to read one. Just what you made here, so the ids are to hand."
        >
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr><th>Guardian</th><th>Phone (stored)</th><th>Email</th><th>Guardian id</th><th>Added</th></tr>
              </thead>
              <tbody>
                {added.map((g) => (
                  <tr key={g.guardianDocsId}>
                    <td>{g.fullName}</td>
                    <td><span className="mono">{g.phoneNumber ?? '—'}</span></td>
                    <td>{g.emailAddress ?? <span className="muted">none</span>}</td>
                    <td><span className="mono muted">{g.guardianDocsId}</span></td>
                    <td title={g.createdAt}>{readable(g.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      ) : null}

      <Card
        title="What cannot be done here yet"
        description="Said plainly rather than drawn as an empty table."
      >
        <p className="muted">
          <b>#9 finds a guardian</b> by phone, email or name — the check made before adding a
          second one — and <b>#10 opens one with every child they are attached to</b>. Neither is
          built, which is why there is no list on this page: a table here would claim the school
          has no guardians when it may have hundreds.
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
        <AddGuardian
          onClose={() => setOpen(false)}
          onAdded={(g) => setAdded((old) => [g, ...old])}
        />
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
