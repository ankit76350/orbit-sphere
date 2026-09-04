import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  ArrowLeft, Ban, Check, CheckCircle2, Eye, EyeOff, RefreshCw, Rocket, XCircle,
} from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import { screenPath } from '../../../paths.js'
import { money, plural } from '../../../lib/money.js'
import { FEATURES, METRIC_LABEL, OVERAGE_POLICIES } from './features.js'
import { BILLING_CYCLES, STATUS_TONE, whyNotSellable } from './planFacts.js'

/**
 * One plan version, at its own address: /platform-plans/catalogue/PREMIUM@2
 *
 * THE ADDRESS CARRIES CODE AND VERSION, because that is how the API names a plan — every plan URL
 * is `/plans/{code}/versions/{version}`, never an id. They are joined with `@` rather than a
 * slash so this stays one route parameter, and because a second slash here would collide with
 * the dev server's proxy the same way the surface prefixes do.
 *
 * WHAT YOU CAN DO DEPENDS ENTIRELY ON THE STATUS, and the reason is worth knowing: publishing is
 * a ONE-WAY DOOR. A school can be on the plan from the moment it goes live, so editing what they
 * bought after they bought it is what this whole group is arranged to prevent. #2 and #3 refuse
 * anything that is not a draft, there is no unpublish, and retiring is not a way back either.
 *
 * SO A DRAFT AND A PUBLISHED PLAN ARE ALMOST DIFFERENT SCREENS. A draft can be edited and have
 * its features set; a published one can only be listed publicly or retired. Offering the edit
 * controls on a published plan would mean four buttons whose only outcome is `409`.
 */

const LIST = screenPath('platform', 'plans', 'catalogue')

export default function PlanDetail() {
  const { id } = useParams()
  const [code, rawVersion] = String(id).split('@')
  const version = rawVersion ?? '1'

  const { call } = useApi()
  const { environment } = useApiState()

  const [plan, setPlan] = useState(null)
  const [versions, setVersions] = useState(null)
  const [loading, setLoading] = useState(false)
  const [problem, setProblem] = useState(null)
  const [busy, setBusy] = useState(null)
  const [confirming, setConfirming] = useState(null)

  const path = { code, version }

  const load = useCallback(async () => {
    setLoading(true)
    const [one, all] = await Promise.all([
      call('get-plan-version', { label: 'The plan version', pathParams: { code, version } }),
      call('list-plan-versions', { label: 'Every version', pathParams: { code } }),
    ])
    setLoading(false)
    if (one.ok) {
      setPlan(one.bodyJson)
      setProblem(null)
    } else {
      setPlan(null)
      setProblem(one)
    }
    setVersions(all.ok ? all.bodyJson : null)
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, code, version, environment.id])

  useEffect(() => {
    load()
  }, [load])

  const run = async (key, endpoint, options) => {
    setBusy(key)
    await call(endpoint, { pathParams: { code, version }, ...options })
    setBusy(null)
    setConfirming(null)
    await load()
  }

  if (problem) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> The catalogue</Link>
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message
              || `No plan "${code}" at version ${version}.`}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
          <div className="toolbar" style={{ justifyContent: 'center', marginTop: 12 }}>
            <EndpointTag id="get-plan-version" name="The plan version" pathParams={path} />
          </div>
        </Card>
      </div>
    )
  }

  // Guarded on the data, not on `loading`: the effect that starts the read runs after the first
  // render, so there is always one pass with nothing loaded.
  if (!plan) {
    return (
      <div className="page stack">
        <Link className="back" to={LIST}><ArrowLeft size={13} /> The catalogue</Link>
        <p className="muted">Reading <span className="mono">{code} v{version}</span>…</p>
      </div>
    )
  }

  const draft = plan.status === 'DRAFT'
  const retired = plan.status === 'RETIRED'
  const why = whyNotSellable(plan)

  return (
    <div className="page stack">
      <div>
        <Link className="back" to={LIST}><ArrowLeft size={13} /> The catalogue</Link>
        <div className="toolbar">
          <div>
            <h1 className="page-title">{plan.name}</h1>
            <p className="muted">
              <span className="mono">{plan.planCode}</span> · version {plan.planVersion}
            </p>
          </div>
          <Badge tone={STATUS_TONE[plan.status]}>{plan.status}</Badge>
          {plan.sellable
            ? <Badge tone="good">sellable</Badge>
            : <Badge tone="warn" title={why}>{why}</Badge>}
          <span className="toolbar-spacer" />
          <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
          <EndpointTag id="get-plan-version" name="Refresh" pathParams={path} />
        </div>
      </div>

      {/* The API's own sentence about anything odd — an untrustworthy zero, mostly. */}
      {plan.note ? (
        <Card>
          <p className="muted">{plan.note}</p>
        </Card>
      ) : null}

      <Card title="The terms" description="What a school buying this version gets.">
        <dl className="dl">
          <div>
            <span className="dl-term">Price</span>
            <span className="dl-value">
              {money(plan.listPrice, plan.currencyCode)}{' '}
              <span className="muted">{plan.billingCycle?.toLowerCase().replace('_', ' ')}</span>
            </span>
          </div>
          <div>
            <span className="dl-term">Students</span>
            <span className="dl-value">{plan.maxStudents}</span>
          </div>
          <div>
            <span className="dl-term">Users</span>
            <span className="dl-value">{plan.maxUsers}</span>
          </div>
          <div>
            <span className="dl-term">On the public list</span>
            <span className="dl-value">{plan.publiclyAvailable ? 'Yes' : 'No — quote only'}</span>
          </div>
          <div>
            <span className="dl-term">On sale from</span>
            <span className="dl-value" data-empty={plan.effectiveFrom ? undefined : 'true'}>
              {plan.effectiveFrom ? new Date(plan.effectiveFrom).toLocaleDateString() : null}
            </span>
          </div>
          <div>
            <span className="dl-term">Until</span>
            <span className="dl-value" data-empty={plan.effectiveUntil ? undefined : 'true'}>
              {plan.effectiveUntil ? new Date(plan.effectiveUntil).toLocaleDateString() : null}
            </span>
          </div>
          <div className="dl-wide">
            <span className="dl-term">Description</span>
            <span className="dl-value" data-empty={plan.description ? undefined : 'true'}>
              {plan.description}
            </span>
          </div>
          <div>
            <span className="dl-term">Schools on this version</span>
            <span className="dl-value">{plan.schoolsOnThisVersion}</span>
          </div>
        </dl>
      </Card>

      {draft ? (
        <EditDraft plan={plan} path={path} onSaved={load} />
      ) : (
        <Card
          title="The terms cannot be changed"
          description={
            retired
              ? 'This version is retired. Nothing about it can move.'
              : 'Publishing is a one-way door: a school may already be on this version, and '
                + 'editing what they bought after they bought it is what that prevents.'
          }
        />
      )}

      <Features plan={plan} path={path} draft={draft} onSaved={load} />

      <Lifecycle
        plan={plan}
        path={path}
        busy={busy}
        onRun={run}
        onConfirm={setConfirming}
      />

      <Versions versions={versions} code={code} current={plan.planVersion} />

      <details className="raw">
        <summary>
          The raw version
          <span className="toolbar-spacer" />
          <EndpointTag id="get-plan-version" name="Read from" pathParams={path} />
        </summary>
        <pre className="resp-body">{JSON.stringify(plan, null, 2)}</pre>
      </details>

      <Confirm
        action={confirming}
        busy={Boolean(busy)}
        onCancel={() => setConfirming(null)}
        onGo={() => run(confirming.key, confirming.endpoint, { label: confirming.label })}
      />
    </div>
  )
}

/* ------------------------------------------------------------------- edit, drafts only */

function EditDraft({ plan, path, onSaved }) {
  const { call } = useApi()
  const [form, setForm] = useState({
    name: plan.name ?? '',
    description: plan.description ?? '',
    billingCycle: plan.billingCycle ?? 'YEARLY',
    listPrice: String(plan.listPrice ?? ''),
    currencyCode: plan.currencyCode ?? '',
    maxStudents: String(plan.maxStudents ?? ''),
    maxUsers: String(plan.maxUsers ?? ''),
  })
  const [refused, setRefused] = useState(null)
  const [errors, setErrors] = useState({})
  const [saving, setSaving] = useState(false)

  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value })

  const dirty = form.name !== (plan.name ?? '')
    || form.description !== (plan.description ?? '')
    || form.billingCycle !== plan.billingCycle
    || form.listPrice !== String(plan.listPrice ?? '')
    || form.currencyCode !== (plan.currencyCode ?? '')
    || form.maxStudents !== String(plan.maxStudents ?? '')
    || form.maxUsers !== String(plan.maxUsers ?? '')

  const save = async () => {
    setRefused(null)
    setErrors({})
    setSaving(true)
    // Only what moved. The endpoint refuses an entirely empty body, and sending unchanged
    // fields would make every save look like a change in the audit.
    const body = {}
    if (form.name !== (plan.name ?? '')) body.name = form.name.trim()
    if (form.description !== (plan.description ?? '')) body.description = form.description.trim() || null
    if (form.billingCycle !== plan.billingCycle) body.billingCycle = form.billingCycle
    if (form.listPrice !== String(plan.listPrice ?? '')) body.listPrice = Number(form.listPrice)
    if (form.currencyCode !== (plan.currencyCode ?? '')) body.currencyCode = form.currencyCode.trim().toUpperCase()
    if (form.maxStudents !== String(plan.maxStudents ?? '')) body.maxStudents = Number(form.maxStudents)
    if (form.maxUsers !== String(plan.maxUsers ?? '')) body.maxUsers = Number(form.maxUsers)

    const result = await call('update-plan-draft', { label: 'Save the draft', pathParams: path, body })
    setSaving(false)
    if (result.ok) {
      await onSaved()
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
    <Card
      title="Edit the draft"
      description="A partial edit — only what you change is sent. Possible only while it is a draft."
      action={<EndpointTag id="update-plan-draft" name="Save the draft" pathParams={path} />}
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

        <Field label="Name" error={errors.name}>
          <Input value={form.name} error={errors.name} onChange={set('name')} />
        </Field>
        <Field label="Description" error={errors.description}>
          <Input value={form.description} onChange={set('description')} />
        </Field>
        <div className="field-grid">
          <Field label="Billing cycle">
            <Select
              label="Billing cycle"
              value={form.billingCycle}
              onChange={(billingCycle) => setForm({ ...form, billingCycle })}
              options={BILLING_CYCLES}
            />
          </Field>
          <Field label="Currency" error={errors.currencyCode}>
            <Input value={form.currencyCode} error={errors.currencyCode} onChange={set('currencyCode')} />
          </Field>
          <Field label="List price" error={errors.listPrice}>
            <Input type="number" min="0" step="0.01" value={form.listPrice} error={errors.listPrice} onChange={set('listPrice')} />
          </Field>
          <Field label="Students" error={errors.maxStudents}>
            <Input type="number" min="1" value={form.maxStudents} error={errors.maxStudents} onChange={set('maxStudents')} />
          </Field>
          <Field label="Users" error={errors.maxUsers}>
            <Input type="number" min="1" value={form.maxUsers} error={errors.maxUsers} onChange={set('maxUsers')} />
          </Field>
        </div>
        <div className="toolbar">
          <Button look="primary" icon={Check} busy={saving} disabled={!dirty} onClick={save}>
            Save the draft
          </Button>
          <span className="muted">
            {dirty ? 'Only the changed fields are sent.' : 'Nothing changed yet.'}
          </span>
        </div>
      </div>
    </Card>
  )
}

/* ------------------------------------------------------------------------- the features */

/**
 * The whole feature list, replaced in one go.
 *
 * PUT, NOT A SET OF TOGGLES. The endpoint takes the entire list and replaces it, which is why
 * this is one Save rather than a switch per row: a plan that is half-priced against
 * half-a-feature-list is the state the single write exists to make impossible.
 */
function Features({ plan, path, draft, onSaved }) {
  const { call } = useApi()
  const [rows, setRows] = useState(() => {
    const existing = new Map((plan.features ?? []).map((one) => [one.featureCode, one]))
    return FEATURES.map((feature) => {
      const on = existing.get(feature.code)
      return {
        ...feature,
        included: Boolean(on),
        enabled: on ? on.enabled !== false : true,
        usageLimit: on?.usageLimit != null ? String(on.usageLimit) : '',
        overagePolicy: on?.overagePolicy ?? 'BLOCK',
      }
    })
  })
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  const update = (code, patch) => setRows(
    rows.map((row) => (row.code === code ? { ...row, ...patch } : row)),
  )

  const chosen = rows.filter((row) => row.included)

  const save = async () => {
    setRefused(null)
    setSaving(true)
    const features = chosen.map((row) => {
      const one = { featureCode: row.code, enabled: row.enabled }
      // A limit only goes on a feature that has something to count — anything else is
      // 400 FEATURE_NOT_MEASURABLE, which the row already refuses to offer.
      if (row.metric && row.usageLimit.trim()) {
        one.usageLimit = Number(row.usageLimit)
        one.overagePolicy = row.overagePolicy
      }
      return one
    })
    const result = await call('set-plan-features', {
      label: 'Set the features', pathParams: path, body: { features },
    })
    setSaving(false)
    if (result.ok) await onSaved()
    else setRefused(result.bodyJson || { message: `The server answered ${result.status}.` })
  }

  if (!draft) {
    return (
      <Card
        title="What this plan includes"
        description={`${plural(plan.featureCount, 'feature')} — frozen, because the plan is published.`}
      >
        {(plan.features ?? []).length === 0 ? (
          <Empty
            title="No features"
            description="This version grants nothing. It could not have been published — unless it was published before the check existed."
          />
        ) : (
          <ul className="divide">
            {plan.features.map((feature) => (
              <li key={feature.featureCode} className="feature-row">
                {feature.enabled
                  ? <CheckCircle2 size={15} className="feature-tick" />
                  : <XCircle size={15} className="feature-cross" />}
                <span className="feature-main">
                  <span className="feature-name">
                    {feature.label}
                    {feature.usageLimit != null ? (
                      <span className="muted">
                        {' '}up to {feature.usageLimit}{' '}
                        {METRIC_LABEL[feature.usageMetric] ?? ''}
                      </span>
                    ) : null}
                  </span>
                  <span className="feature-desc">{feature.description}</span>
                </span>
                {feature.usageLimit != null ? <Badge>{feature.overagePolicy}</Badge> : null}
              </li>
            ))}
          </ul>
        )}
      </Card>
    )
  }

  return (
    <Card
      title="What this plan includes"
      description={`${plural(chosen.length, 'feature')} chosen. Sent as one list — the whole thing is replaced.`}
      action={<EndpointTag id="set-plan-features" name="Set the features" pathParams={path} />}
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

        <ul className="divide">
          {rows.map((row) => (
            <li key={row.code} className="feature-row">
              <input
                type="checkbox"
                className="feature-check"
                checked={row.included}
                aria-label={row.label}
                onChange={(event) => update(row.code, { included: event.target.checked })}
              />
              <span className="feature-main">
                <span className="feature-name">{row.label}</span>
                <span className="feature-desc mono">{row.code}</span>
              </span>

              {row.included && row.metric ? (
                <>
                  <input
                    className="input feature-limit"
                    type="number"
                    min="1"
                    placeholder="no limit"
                    value={row.usageLimit}
                    aria-label={`Limit for ${row.label}`}
                    onChange={(event) => update(row.code, { usageLimit: event.target.value })}
                  />
                  <span className="muted feature-metric">{METRIC_LABEL[row.metric]}</span>
                  {row.usageLimit.trim() ? (
                    <Select
                      label={`Past the limit for ${row.label}`}
                      value={row.overagePolicy}
                      onChange={(overagePolicy) => update(row.code, { overagePolicy })}
                      options={OVERAGE_POLICIES}
                    />
                  ) : null}
                </>
              ) : row.included ? (
                <span className="muted feature-metric">nothing to count</span>
              ) : null}
            </li>
          ))}
        </ul>

        <div className="toolbar">
          <Button look="primary" icon={Check} busy={saving} onClick={save}>
            Save the whole list
          </Button>
          <span className="muted">
            An empty list is allowed and empties it — which is why there is no delete.
          </span>
        </div>
      </div>
    </Card>
  )
}

/* ------------------------------------------------------------------------ the lifecycle */

function Lifecycle({ plan, path, busy, onRun, onConfirm }) {
  const draft = plan.status === 'DRAFT'
  const retired = plan.status === 'RETIRED'
  const noFeatures = plan.featureCount === 0

  return (
    <Card
      title="Where it is in its life"
      description="Publishing and retiring cannot be undone, so both ask first."
    >
      <div className="stack">
        {draft ? (
          <div className="toolbar">
            <Button
              look="primary"
              icon={Rocket}
              disabled={noFeatures}
              busy={busy === 'publish'}
              onClick={() => onConfirm({
                key: 'publish',
                endpoint: 'publish-plan',
                label: 'Publish it',
                title: 'Publish this plan',
                body: 'From then on nothing about it can change — no price, no features, no '
                  + 'unpublish. A school can be on it within the minute.',
              })}
            >
              Publish it
            </Button>
            <span className="muted">
              {noFeatures
                ? 'Refused while it has no features — a school would pay and be granted nothing.'
                : 'Turns the draft into something a school can buy.'}
            </span>
            <span className="toolbar-spacer" />
            <EndpointTag id="publish-plan" name="Publish it" look="primary" pathParams={path} />
          </div>
        ) : null}

        {/* Availability is a two-way switch, unlike the other two, so it does not ask. */}
        {!retired ? (
          <div className="toolbar">
            <Button
              icon={plan.publiclyAvailable ? EyeOff : Eye}
              busy={busy === 'availability'}
              onClick={() => onRun('availability', 'set-plan-availability', {
                label: plan.publiclyAvailable ? 'Take it off the list' : 'Put it on the list',
                body: { publiclyAvailable: !plan.publiclyAvailable },
              })}
            >
              {plan.publiclyAvailable ? 'Take it off the public list' : 'Put it on the public list'}
            </Button>
            <span className="muted">
              Off the list is not unsellable — it is a private quote.
            </span>
            <span className="toolbar-spacer" />
            <EndpointTag id="set-plan-availability" name="Availability" pathParams={path} />
          </div>
        ) : null}

        {!retired ? (
          <div className="toolbar">
            <Button
              look="danger"
              icon={Ban}
              busy={busy === 'retire'}
              onClick={() => onConfirm({
                key: 'retire',
                endpoint: 'retire-plan',
                label: 'Retire it',
                title: 'Retire this plan',
                body: `Stops it being sold. The ${plan.schoolsOnThisVersion} school(s) already on `
                  + 'it keep it, at the same price and features — retiring is about the menu, not '
                  + 'about anybody\'s subscription. It cannot be undone.',
              })}
            >
              Retire it
            </Button>
            <span className="muted">Takes it off the menu. Schools on it keep it.</span>
            <span className="toolbar-spacer" />
            <EndpointTag id="retire-plan" name="Retire it" look="danger" pathParams={path} />
          </div>
        ) : (
          <p className="muted">Retired. There is no way back — a new version is the only route.</p>
        )}
      </div>
    </Card>
  )
}

function Confirm({ action, busy, onCancel, onGo }) {
  if (!action) return null
  return (
    <Modal
      open
      onClose={onCancel}
      title={action.title}
      footer={
        <>
          <Button onClick={onCancel}>Cancel</Button>
          <Button
            look={action.key === 'retire' ? 'danger' : 'primary'}
            busy={busy}
            onClick={onGo}
          >
            {action.label}
          </Button>
        </>
      }
    >
      <p className="muted">{action.body}</p>
    </Modal>
  )
}

/* -------------------------------------------------------------------------- the versions */

function Versions({ versions, code, current }) {
  if (!versions) return null
  return (
    <Card
      title="Every version of this plan"
      description={versions.note || `${plural(versions.versionCount, 'version')}, newest first.`}
      action={<EndpointTag id="list-plan-versions" name="Every version" pathParams={{ code }} />}
    >
      <div className="table-scroll">
        <table className="data-table">
          <thead>
            <tr><th>Version</th><th>Status</th><th>Price</th><th>Schools</th><th /></tr>
          </thead>
          <tbody>
            {(versions.versions ?? []).map((one) => (
              <tr key={one.planVersion}>
                <td className="mono">
                  v{one.planVersion}
                  {one.planVersion === current ? <Badge tone="brand">this one</Badge> : null}
                </td>
                <td><Badge tone={STATUS_TONE[one.status]}>{one.status}</Badge></td>
                <td>{money(one.listPrice, one.currencyCode)}</td>
                <td className="muted">{one.schoolsOnThisVersion ?? '—'}</td>
                <td>
                  {one.planVersion === current ? null : (
                    <Link
                      className="btn"
                      to={`${LIST}/${code}@${one.planVersion}`}
                    >
                      Open
                    </Link>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}
