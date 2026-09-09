import { useCallback, useEffect, useMemo, useState } from 'react'
import { RefreshCw, ShieldAlert } from 'lucide-react'
import { useApi, useApiState } from '../../../api/apiContext.js'
import EndpointTag from '../../../components/EndpointTag.jsx'
import Select from '../../../components/ui/Select.jsx'
import { Badge, Button, Card, Empty, Field, Input } from '../../../components/ui/Kit.jsx'
import { endOfDay, readableDateTime, startOfDay } from '../../../lib/dates.js'
import { money, plural } from '../../../lib/money.js'

/**
 * Platform / Plans — every school's subscription. Endpoint #30.
 *
 * ITS OWN SCREEN, NOT A CARD ON THE SUBSCRIPTIONS PAGE. That page is school-scoped: it starts
 * with a School picker and every endpoint on it names a school in the URL. This one takes no
 * school at all, so a school picker would be meaningless here and the two do not belong on the
 * same screen. #28 answers "what has THIS school been on"; this answers "who is on what".
 *
 * ROWS ARE PERIODS, NOT SCHOOLS. The collection holds one document per billing period, so a
 * school on its fourth plan appears four times. `current=true` is the one-row-per-school view and
 * it is offered as a filter rather than applied by default, because the API does not default it
 * either and a screen that quietly narrowed would disagree with the count it printed.
 *
 * EVERY FILTER IS ON SCREEN, INCLUDING THE ONES THAT WILL BE REFUSED. `size` goes to 101, and the
 * sort list carries `subscriptionNo` — which #28 accepts and this endpoint refuses, because a
 * subscription number is unique only within a school. Both are here so the 400s can be seen.
 * Nothing is disabled.
 */

/** What #30 may sort on, plus two it refuses on purpose. */
const ALL_SORTS = [
  '', 'currentPeriodEnd,asc', 'currentPeriodEnd,desc',
  'currentPeriodStart,desc', 'status,asc',
  'contractedPrice,desc', 'contractedPrice,asc',
  'createdAt,desc', 'updatedAt,desc',
  // Off the allow-list. `subscriptionNo` is the interesting one: #28 sorts on it and this cannot,
  // because two schools both have a SUB/2026/09/000001.
  'subscriptionNo,asc', 'schoolName,asc',
]

const ALL_STATUSES = ['TRIAL', 'ACTIVE', 'PAST_DUE', 'SUSPENDED', 'CANCELLED', 'EXPIRED']
const ALL_CYCLES = ['MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'YEARLY', 'CUSTOM']

/** A school's own status, which moves independently of its subscription's. */
const SCHOOL_TONE = {
  ACTIVE: 'good', PROVISIONING: 'warn', SUSPENDED: 'bad',
  CLOSED: 'bad', DELETION_PENDING: 'bad', DELETED: 'bad',
}

const SUBSCRIPTION_TONE = {
  TRIAL: 'warn', ACTIVE: 'good', PAST_DUE: 'warn',
  SUSPENDED: 'bad', CANCELLED: 'bad', EXPIRED: 'bad',
}

/** The date that steps around the ten corrupt rows. See the panel below. */
const CORRUPT_ROW_WORKAROUND = '2099-01-01'

export default function AllSubscriptions() {
  const { call } = useApi()
  const { environment } = useApiState()

  const [statuses, setStatuses] = useState([])
  const [cycles, setCycles] = useState([])
  const [planCode, setPlanCode] = useState('')
  const [planVersion, setPlanVersion] = useState('')
  const [current, setCurrent] = useState('')
  const [autoRenew, setAutoRenew] = useState('')
  const [startFrom, setStartFrom] = useState('')
  const [startTo, setStartTo] = useState('')
  const [endFrom, setEndFrom] = useState('')
  const [endTo, setEndTo] = useState('')
  const [sort, setSort] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState('20')

  const [data, setData] = useState(null)
  const [problem, setProblem] = useState(null)
  const [loading, setLoading] = useState(false)

  // Built at render so the endpoint tag shows the URL that will actually be sent, and changes as
  // the filters change. An empty box sends nothing rather than an empty parameter.
  const query = useMemo(() => {
    const out = { page, size }
    if (statuses.length) out.status = statuses
    if (cycles.length) out.billingCycle = cycles
    if (planCode.trim()) out.planCode = planCode.trim()
    if (planVersion.trim()) out.planVersion = planVersion.trim()
    if (current) out.current = current
    if (autoRenew) out.autoRenew = autoRenew
    if (startFrom) out.startDateFrom = startOfDay(startFrom)
    if (startTo) out.startDateTo = endOfDay(startTo)
    if (endFrom) out.endDateFrom = startOfDay(endFrom)
    if (endTo) out.endDateTo = endOfDay(endTo)
    if (sort) out.sort = sort
    return out
  }, [page, size, statuses, cycles, planCode, planVersion, current, autoRenew,
    startFrom, startTo, endFrom, endTo, sort])

  const load = useCallback(async () => {
    setLoading(true)
    const result = await call('list-all-subscriptions', {
      label: "Every school's subscription",
      query,
    })
    setLoading(false)
    if (result.ok) { setData(result.bodyJson); setProblem(null) } else { setProblem(result) }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [call, environment.id, query])

  useEffect(() => { load() }, [load])

  const rows = data?.content ?? []

  const toggle = (list, setList, value) => {
    setPage(0)
    setList(list.includes(value) ? list.filter((x) => x !== value) : [...list, value])
  }

  // The bare list is a 500 until ten fixture rows are repaired, and a bare INTERNAL_ERROR tells
  // nobody why. The panel names the cause and offers the one-click way past it.
  const corruptRows = problem?.status === 500 && !endTo

  return (
    <div className="stack">
      <Card
        title="Every school's subscription"
        description="Endpoint #30. The operator's whole platform in one list. Rows are billing periods, not schools — a school on its fourth plan appears four times, and `current` is the one it is on now."
        action={<EndpointTag
          id="list-all-subscriptions"
          name="The platform, as filtered"
          query={query}
        />}
      >
        <div className="stack">
          <div className="toolbar">
            <Button icon={RefreshCw} onClick={load} busy={loading}>Refresh</Button>
            <span className="muted">
              {data
                ? `${plural(data.totalElements, 'subscription')} · page ${data.page + 1} of ${Math.max(data.totalPages, 1)}`
                : 'Not read yet'}
            </span>
            <span className="toolbar-spacer" />
            <Select
              label="Sort"
              value={sort}
              onChange={(value) => { setSort(value); setPage(0) }}
              options={ALL_SORTS.map((one) => one || 'soonest to end first (default)')}
            />
          </div>

          {/* Status is the headline filter, and how you ask for trials. */}
          <Field label="Status" hint="Repeats the parameter, so several mean either. TRIAL is how you ask for trials — there is no `trial` field.">
            <div className="toolbar">
              {ALL_STATUSES.map((one) => (
                <button
                  key={one}
                  type="button"
                  className="segmented-item"
                  data-active={statuses.includes(one) || undefined}
                  onClick={() => toggle(statuses, setStatuses, one)}
                >
                  {one}
                </button>
              ))}
            </div>
          </Field>

          <Field label="Billing cycle" hint="Repeats too, so several mean either.">
            <div className="toolbar">
              {ALL_CYCLES.map((one) => (
                <button
                  key={one}
                  type="button"
                  className="segmented-item"
                  data-active={cycles.includes(one) || undefined}
                  onClick={() => toggle(cycles, setCycles, one)}
                >
                  {one}
                </button>
              ))}
            </div>
          </Field>

          <div className="field-grid">
            <Field label="Plan code" hint="Exact, and normalised — premium-plus finds PREMIUM_PLUS. A code matching nothing gives an empty page.">
              <Input value={planCode} onChange={(e) => { setPlanCode(e.target.value); setPage(0) }} placeholder="PREMIUM" />
            </Field>
            <Field label="Plan version" hint="Only means something beside a plan code.">
              <Input value={planVersion} onChange={(e) => { setPlanVersion(e.target.value); setPage(0) }} placeholder="1" />
            </Field>
            <Field label="Current" hint="true is one row per school — the `who is on what` view. Not the API's default.">
              <Select label="Current" value={current}
                onChange={(value) => { setCurrent(value); setPage(0) }}
                options={['', 'true', 'false'].map((one) => one || 'either')} />
            </Field>
            <Field label="Auto renew" hint="false beside current=true is `nobody has agreed a renewal`.">
              <Select label="Auto renew" value={autoRenew}
                onChange={(value) => { setAutoRenew(value); setPage(0) }}
                options={['', 'true', 'false'].map((one) => one || 'either')} />
            </Field>
          </div>

          <div className="field-grid">
            <Field label="Period starts from" hint="Inclusive.">
              <Input type="date" value={startFrom} onChange={(e) => { setStartFrom(e.target.value); setPage(0) }} />
            </Field>
            <Field label="Period starts to" hint="From after to is 400 INVALID_DATE_RANGE.">
              <Input type="date" value={startTo} onChange={(e) => { setStartTo(e.target.value); setPage(0) }} />
            </Field>
            <Field label="Period ends from" hint="This pair is the renewal question — everything ending this quarter.">
              <Input type="date" value={endFrom} onChange={(e) => { setEndFrom(e.target.value); setPage(0) }} />
            </Field>
            <Field label="Period ends to">
              <Input type="date" value={endTo} onChange={(e) => { setEndTo(e.target.value); setPage(0) }} />
            </Field>
          </div>

          {/* 101 is over the cap on purpose: the API refuses it rather than clamping. */}
          <div className="toolbar">
            <Field label="Page size" hint="Defaults to 20, capped at 100. 101 is refused, not clamped.">
              <Select label="Page size" value={size}
                onChange={(value) => { setSize(value); setPage(0) }}
                options={['1', '5', '20', '100', '101', '0']} />
            </Field>
            <span className="toolbar-spacer" />
            <Button onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <span className="muted">page {page}</span>
            <Button onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </div>
      </Card>

      {/*
        WHY THIS PANEL EXISTS. Ten rows in school_subscriptions have currentPeriodEnd stored as
        {"$date": "..."} instead of a BSON date, so Spring Data throws converting them and any
        page that touches one is a 500. It is NOT this endpoint's bug — #27 and #28 already fail
        on those ten schools — but #30's default request touches all of them, so without this
        panel the screen would just say INTERNAL_ERROR and blame itself.
      */}
      {corruptRows ? (
        <Card
          title="This is the ten corrupt fixture rows, not the endpoint"
          description={'Ten documents have currentPeriodEnd stored as a nested {"$date": …} object rather than a date, so Spring Data throws converting them. #27 and #28 already answer 500 on those ten schools; #30\u2019s default request is just the first that touches all of them.'}
        >
          <div className="stack">
            <span className="muted">
              Objects sort after dates in BSON, so any “period ends to” date steps around them.
              Repair them in mongosh and this goes away for #27 and #28 too.
            </span>
            <pre className="resp-body">{`db.school_subscriptions.find({currentPeriodEnd: {$type: 'object'}}).forEach(d =>
  db.school_subscriptions.updateOne({_id: d._id},
    {$set: {currentPeriodEnd: new Date(d.currentPeriodEnd['$date'])}}))`}</pre>
            <div className="toolbar">
              <Button
                icon={ShieldAlert}
                onClick={() => { setEndTo(CORRUPT_ROW_WORKAROUND); setPage(0) }}
              >
                Step around them
              </Button>
              <span className="muted">sets “period ends to” to {CORRUPT_ROW_WORKAROUND}</span>
            </div>
          </div>
        </Card>
      ) : null}

      {problem && !corruptRows ? (
        <div className="resp">
          <div className="resp-head">
            <span className="resp-status" data-ok="false">
              {problem.bodyJson?.code || `The server answered ${problem.status}`}
            </span>
          </div>
          <pre className="resp-body">
            {problem.bodyJson?.message || 'Nothing came back.'}
          </pre>
        </div>
      ) : null}

      {!problem && rows.length === 0 ? (
        <Empty
          title="No subscriptions match"
          description={data && data.totalElements === 0 && Object.keys(query).length <= 2
            ? 'No school on the platform has one yet. That is an empty page — there is no school in this URL, so there is nothing for it to 404 on.'
            : 'The filters match nothing. Clear one and read again.'}
        />
      ) : null}

      {rows.map((row) => (
        <div className="resp" key={row.subscriptionId}>
          <div className="resp-head">
            <strong>{row.schoolName ?? '(the school document has gone)'}</strong>
            {row.subdomain ? <span className="muted">{row.subdomain}</span> : null}
            {/* The school's own status, which nothing keeps in step with the subscription's. */}
            {row.schoolStatus
              ? <Badge tone={SCHOOL_TONE[row.schoolStatus]}>school {row.schoolStatus}</Badge>
              : null}
            <span className="toolbar-spacer" />
            <Badge tone={SUBSCRIPTION_TONE[row.status]}>{row.status}</Badge>
            {row.current ? <Badge tone="good">current</Badge> : null}
            {row.periodEnded ? <Badge tone="bad">period ended</Badge> : null}
          </div>
          <div className="stack" style={{ padding: '10px 12px', gap: 6 }}>
            <span>
              <strong>{row.planCode ?? '—'}</strong>
              {row.planVersion ? ` v${row.planVersion}` : ''}
              {row.planName ? ` · ${row.planName}` : ''}
              {row.planCode === null
                ? ' · the plan this points at has been deleted, so its name cannot be shown'
                : ''}
              {' · '}{row.billingCycle}
              {row.autoRenew ? ' · auto-renews' : ' · does not auto-renew'}
            </span>
            <span className="muted">
              {readableDateTime(row.currentPeriodStart)} → {readableDateTime(row.currentPeriodEnd)}
              {' · '}{money(row.contractedPrice, row.currencyCode)}
            </span>
            <span className="muted">
              {row.subscriptionNo}
              {' · school '}{row.schoolId}
              {' — paste that into #28 to see this school on its own'}
            </span>
          </div>
        </div>
      ))}
    </div>
  )
}
