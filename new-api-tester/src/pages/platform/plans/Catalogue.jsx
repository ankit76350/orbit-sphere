import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronRight, Package, Plus, RefreshCw } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import { Badge, Button, Card, Empty, Field, Input, Modal } from '../../../components/ui/Kit.jsx'
import Select from '../../../components/ui/Select.jsx'
import { detailPath } from '../../../paths.js'
import { money, plural } from '../../../lib/money.js'
import { BILLING_CYCLES, STATUS_TONE, whyNotSellable } from './planFacts.js'

/**
 * Platform / Plans — the catalogue. The list and the create; the other seven are on a version's
 * own page.
 *
 * CARDS, NOT ROWS. A plan is a thing you compare against other plans — price against limits
 * against what is included — and that reads down a card far better than across a table row,
 * where the price and the feature count end up columns apart. It is also what a catalogue looks
 * like everywhere else, which is worth something when the screen exists to be read quickly.
 *
 * ONE CARD PER VERSION, NOT PER PLAN. PREMIUM v1 and v2 are two documents with two prices, and a
 * school is on exactly one of them — so collapsing them would hide the thing somebody opened the
 * catalogue to see. The default order groups them by code with the newest version of each first,
 * which reads as a menu rather than as a change log.
 *
 * `sellable` IS THREE FACTS AT ONCE — published, on the public list, inside its selling window —
 * so when it is false the useful thing is WHICH of them is missing, not the false itself. Every
 * card that cannot be sold says why.
 */

const FILTERS = [
  { id: 'all', label: 'All', status: undefined },
  { id: 'draft', label: 'Drafts', status: 'DRAFT' },
  { id: 'live', label: 'Published', status: 'ACTIVE' },
  { id: 'retired', label: 'Retired', status: 'RETIRED' },
]

const SORTS = ['', 'name,asc', 'listPrice,desc', 'listPrice,asc', 'createdAt,desc']

/**
 * The query the list sends, from what is on screen.
 *
 * A plain function outside the component, for the same reason as everywhere else: the tag has to
 * show the query the call sends, and building the object in the body would put a fresh one in
 * load()'s dependencies every render and turn the page into a loop.
 */
function listQuery({ search, filter, page, size, sort }) {
  return {
    search: search.trim() || undefined,
    status: FILTERS.find((one) => one.id === filter)?.status,
    page,
    size,
    sort: sort || undefined,
  }
}

export default function Catalogue() {
  const { call } = useApi()
  const { environment } = useApiState()

  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('all')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const size = 24

  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [problem, setProblem] = useState(null)
  const [creating, setCreating] = useState(false)

  const query = listQuery({ search, filter, page, size, sort })

  const load = useCallback(async () => {
    setLoading(true)
    const result = await call('list-plans', {
      label: 'The catalogue',
      query: listQuery({ search, filter, page, size, sort }),
    })
    setLoading(false)
    if (result.ok) {
      setData(result.bodyJson)
      setProblem(null)
    } else {
      setProblem(result)
    }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, search, filter, page, sort])

  useEffect(() => {
    const timer = setTimeout(load, search ? 350 : 0)
    return () => clearTimeout(timer)
  }, [load, search])

  const plans = data?.content ?? []

  return (
    <div className="page stack">
      <div className="toolbar">
        <div>
          <h1 className="page-title">Plan catalogue</h1>
          <p className="muted">
            {data
              ? `${plural(data.totalElements, 'plan version')} — what we sell and at what price`
              : 'What we sell, and at what price'}
          </p>
        </div>
        <span className="toolbar-spacer" />
        <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
        <EndpointTag id="list-plans" name="Refresh" query={query} showPath={false} />
        <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>New draft</Button>
      </div>

      <Card>
        <div className="toolbar">
          <span className="search search-sm">
            <input
              className="search-input"
              value={search}
              onChange={(event) => { setSearch(event.target.value); setPage(0) }}
              placeholder="Search name or code"
              aria-label="Search plans"
            />
          </span>

          <div className="segmented" role="tablist" aria-label="Status">
            {FILTERS.map((one) => (
              <button
                key={one.id}
                type="button"
                role="tab"
                className="segmented-item"
                data-active={filter === one.id || undefined}
                onClick={() => { setFilter(one.id); setPage(0) }}
              >
                {one.label}
              </button>
            ))}
          </div>

          <Select
            label="Sort"
            value={sort}
            onChange={setSort}
            options={SORTS.map((one) => one || 'by plan, newest version')}
          />

          <span className="toolbar-spacer" />
          <EndpointTag id="list-plans" name="The list, as filtered" query={query} />
        </div>
      </Card>

      {problem ? (
        <Card>
          <Empty
            title={problem.bodyJson?.code || `The server answered ${problem.status}`}
            description={problem.bodyJson?.message
              || 'Nothing came back. Is the backend running on 3456?'}
            action={<Button icon={RefreshCw} onClick={load}>Try again</Button>}
          />
        </Card>
      ) : plans.length === 0 && !loading ? (
        <Card>
          <Empty
            title="No plans match"
            description="Change the filter, or make the first draft."
            action={<Button look="primary" icon={Plus} onClick={() => setCreating(true)}>New draft</Button>}
          />
        </Card>
      ) : (
        <div className="plan-grid">
          {plans.map((plan) => (
            <PlanCard key={plan.planId} plan={plan} />
          ))}
        </div>
      )}

      {data ? (
        <div className="toolbar">
          <span className="muted">
            Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} versions
          </span>
          <span className="toolbar-spacer" />
          <div className="pager">
            <Button disabled={!data.hasPrevious} onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <Button disabled={!data.hasNext} onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </div>
      ) : null}

      <NewDraft
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={async () => { setCreating(false); await load() }}
      />
    </div>
  )
}

/* --------------------------------------------------------------------------- one card */

function PlanCard({ plan }) {
  const why = whyNotSellable(plan)

  return (
    <article className="plan-card" data-sellable={plan.sellable} data-status={plan.status}>
      <div className="plan-card-head">
        <span className="plan-card-code" title={plan.planCode}>{plan.planCode}</span>
        <span className="toolbar-spacer" />
        <span className="plan-card-version">v{plan.planVersion}</span>
      </div>

      <p className="plan-card-name">{plan.name}</p>

      <div className="plan-card-price">
        <span className="plan-card-amount">{money(plan.listPrice, plan.currencyCode)}</span>
        <span className="plan-card-cycle">{plan.billingCycle?.toLowerCase().replace('_', ' ')}</span>
      </div>

      <div className="plan-card-badges">
        <Badge tone={STATUS_TONE[plan.status]}>{plan.status}</Badge>
        {plan.publiclyAvailable
          ? <Badge tone="brand">public</Badge>
          : <Badge title="Published but not advertised — a private quote">quote only</Badge>}
        {plan.sellable ? <Badge tone="good">sellable</Badge> : null}
      </div>

      <div className="plan-card-facts">
        <span className="plan-card-fact">
          <strong>{plan.maxStudents}</strong> students · <strong>{plan.maxUsers}</strong> users
        </span>
        <span className="plan-card-fact">
          {plan.featureCount === 0
            ? <span className="plan-card-why">No features — cannot be published</span>
            : <>{plural(plan.featureCount, 'feature')} included</>}
        </span>
      </div>

      <div className="plan-card-foot">
        {why ? <span className="plan-card-why">{why}</span> : null}
        <span className="toolbar-spacer" />
        <Link
          className="btn"
          to={detailPath('platform', 'plans', 'catalogue', `${plan.planCode}@${plan.planVersion}`)}
        >
          Open <ChevronRight size={13} />
        </Link>
      </div>
    </article>
  )
}

/* ---------------------------------------------------------------------- create a draft */

const BLANK = {
  name: '', description: '', billingCycle: 'YEARLY', listPrice: '',
  currencyCode: 'INR', maxStudents: '', maxUsers: '',
}

function NewDraft({ open, onClose, onCreated }) {
  const { call } = useApi()
  const [form, setForm] = useState(BLANK)
  const [errors, setErrors] = useState({})
  const [refused, setRefused] = useState(null)
  const [saving, setSaving] = useState(false)

  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value })

  const submit = async () => {
    setErrors({})
    setRefused(null)
    setSaving(true)
    const body = {
      name: form.name.trim(),
      billingCycle: form.billingCycle,
      listPrice: Number(form.listPrice),
      currencyCode: form.currencyCode.trim().toUpperCase(),
      maxStudents: Number(form.maxStudents),
      maxUsers: Number(form.maxUsers),
    }
    if (form.description.trim()) body.description = form.description.trim()

    const result = await call('create-plan-draft', { label: 'New draft', body })
    setSaving(false)
    if (result.ok) {
      setForm(BLANK)
      await onCreated()
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

  /** What the API will derive the code as, so the form can show it before sending. */
  const derived = form.name.trim().toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '')

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="New plan draft"
      description="Starts as a DRAFT with no features and off the public list. Nothing can buy it yet."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button look="primary" busy={saving} onClick={submit}>Create the draft</Button>
          <EndpointTag id="create-plan-draft" name="Create the draft" look="primary" />
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

        <Field
          label="Name"
          required
          hint={derived
            ? `The code is derived from this: ${derived}`
            : 'The plan code comes from the name — you do not send one.'}
          error={errors.name}
        >
          <Input value={form.name} error={errors.name} onChange={set('name')} placeholder="Premium Plus" />
        </Field>

        <Field label="Description" error={errors.description}>
          <Input value={form.description} onChange={set('description')} />
        </Field>

        <div className="field-grid">
          <Field label="Billing cycle" required>
            <Select
              label="Billing cycle"
              value={form.billingCycle}
              onChange={(billingCycle) => setForm({ ...form, billingCycle })}
              options={BILLING_CYCLES}
            />
          </Field>
          <Field label="Currency" required hint="ISO 4217." error={errors.currencyCode}>
            <Input value={form.currencyCode} error={errors.currencyCode} onChange={set('currencyCode')} />
          </Field>
          <Field
            label="List price"
            required
            hint="At most two decimal places — more is refused, not rounded."
            error={errors.listPrice}
          >
            <Input
              type="number" min="0" step="0.01"
              value={form.listPrice} error={errors.listPrice} onChange={set('listPrice')}
            />
          </Field>
          <Field label="Students included" required error={errors.maxStudents}>
            <Input type="number" min="1" value={form.maxStudents} error={errors.maxStudents} onChange={set('maxStudents')} />
          </Field>
          <Field label="Users included" required error={errors.maxUsers}>
            <Input type="number" min="1" value={form.maxUsers} error={errors.maxUsers} onChange={set('maxUsers')} />
          </Field>
        </div>

        <p className="muted">
          <Package size={12} /> Features are set after this, in one go, on the draft's own page.
        </p>
      </div>
    </Modal>
  )
}
