/**
 * The plan catalogue — what we sell, and at what price.
 *
 * The Plans equivalent of the school list: search, filter, sort, page. Clicking a row opens that
 * plan version.
 *
 * ONE ROW PER VERSION, not per plan. PREMIUM v1 and v2 are two documents with two prices, and a
 * school is on exactly one of them, so collapsing them would hide the thing somebody opened the
 * catalogue to see. The default order groups them — by code, newest version of each first — which
 * reads as a menu rather than as a change log.
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Plus, Package, RefreshCw, ChevronLeft, ChevronRight, AlertTriangle, ShoppingCart, Lock,
} from 'lucide-react';
import { useApi, useApiState } from '../api/apiContext.js';
import {
  Button, SearchInput, SelectInput, Table, Th, Td, EmptyState, SkeletonRows, Badge,
} from '../components/ui.jsx';
import NewPlanModal from './NewPlanModal.jsx';
import EndpointTag from '../components/EndpointTag.jsx';

const STATUS_FILTERS = [
  { id: 'all', label: 'All', statuses: [] },
  { id: 'live', label: 'On sale', statuses: ['ACTIVE'] },
  { id: 'draft', label: 'Drafts', statuses: ['DRAFT'] },
  { id: 'retired', label: 'Retired', statuses: ['RETIRED'] },
];

const SORTS = [
  { id: '', label: 'By plan, newest version' },
  { id: 'name,asc', label: 'Name A–Z' },
  { id: 'listPrice,desc', label: 'Most expensive' },
  { id: 'listPrice,asc', label: 'Cheapest' },
  { id: 'createdAt,desc', label: 'Newest first' },
];

/** What a plan's status means in words somebody would use. */
const STATUS_LOOK = {
  DRAFT: ['amber', 'Draft'],
  ACTIVE: ['green', 'Published'],
  RETIRED: ['grey', 'Retired'],
};

export function PlanStatusBadge({ status }) {
  const [look, label] = STATUS_LOOK[status] || ['grey', status || 'Unknown'];
  return <Badge look={look}>{label}</Badge>;
}

/**
 * Whether a school could buy this today, said in words.
 *
 * The API returns `sellable` as one boolean off three separate facts — published, on the public
 * list, inside its selling window — so when it is false the useful thing is which fact is
 * missing, not the false itself.
 */
export function sellableReason(plan) {
  if (plan.sellable) return null;
  if (plan.status === 'DRAFT') return 'Not published yet';
  if (plan.status === 'RETIRED') return 'Retired';
  if (!plan.publiclyAvailable) return 'Not on the public list';
  if (plan.effectiveFrom && new Date(plan.effectiveFrom) > new Date()) return 'Not on sale yet';
  if (plan.effectiveUntil && new Date(plan.effectiveUntil) <= new Date()) return 'Past its selling window';
  return 'Not on sale';
}

/** Money, with the currency the plan is actually priced in. */
export function money(amount, currencyCode) {
  if (amount == null) return '—';
  try {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: currencyCode || 'INR', maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${amount} ${currencyCode ?? ''}`.trim();
  }
}

/**
 * Built once, outside the component, for the reason written in SchoolsPage: an object rebuilt in
 * the body would give load() a new identity every render and fire the effect in a loop.
 */
function listQueryFrom({ search, filter, page, size, sort }) {
  const statuses = STATUS_FILTERS.find((one) => one.id === filter)?.statuses ?? [];
  return {
    search: search.trim() || undefined,
    status: statuses.length ? statuses : undefined,
    page,
    size,
    sort: sort || undefined,
  };
}

export default function PlansPage({ onOpenPlan }) {
  const { call, inspect } = useApi();
  const { log, environment } = useApiState();

  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('all');
  const [sort, setSort] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [problem, setProblem] = useState(null);
  const [creating, setCreating] = useState(false);

  const latestLoad = useRef(0);

  const load = useCallback(async () => {
    const ticket = ++latestLoad.current;
    setLoading(true);
    const result = await call('list-plans', {
      label: 'Load the catalogue',
      query: listQueryFrom({ search, filter, page, size, sort }),
    });
    if (ticket !== latestLoad.current) return;
    setLoading(false);
    if (result.ok) {
      setData(result.bodyJson);
      setProblem(null);
    } else {
      setProblem(result);
    }
  }, [call, environment.id, search, filter, sort, page, size]);

  useEffect(() => {
    const timer = setTimeout(load, search ? 350 : 0);
    return () => clearTimeout(timer);
  }, [load, search]);

  // What the tag shows. Same function load() uses, so the two cannot disagree.
  const listQuery = listQueryFrom({ search, filter, page, size, sort });

  const rows = data?.content ?? [];
  const from = (data?.page ?? 0) * (data?.size ?? size) + 1;
  const to = from + rows.length - 1;

  const changeFilter = (id) => {
    setFilter(id);
    setPage(0);
  };

  return (
    <div className="space-y-5">
      <header className="flex flex-wrap items-end justify-between gap-3">
        <div className="min-w-0">
          <h1 className="text-xl font-semibold text-slate-900">Plans</h1>
          <p className="mt-0.5 text-sm text-slate-500">
            {data
              ? `${data.totalElements} plan version${data.totalElements === 1 ? '' : 's'} in the catalogue`
              : 'What we sell, and at what price'}
          </p>
          {/* The live request, filters and paging included. Click it for the last response. */}
          <EndpointTag id="list-plans" query={listQuery} className="mt-2" />
        </div>
        <div className="flex items-center gap-2">
          <div className="flex flex-col items-center gap-1">
            <Button icon={RefreshCw} onClick={load} busy={loading}>
              Refresh
            </Button>
            <EndpointTag id="list-plans" query={listQuery} showPath={false} />
          </div>
          <div className="flex flex-col items-center gap-1">
            <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>
              New plan
            </Button>
            <EndpointTag id="create-plan-draft" />
          </div>
        </div>
      </header>

      <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-100 px-4 py-3">
          <SearchInput
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Search by name or code"
            className="w-full sm:w-72"
          />
          <div className="flex flex-wrap items-center gap-1">
            {STATUS_FILTERS.map((one) => (
              <button
                key={one.id}
                type="button"
                onClick={() => changeFilter(one.id)}
                className={`rounded-full px-3 py-1.5 text-xs font-medium transition ${
                  filter === one.id
                    ? 'bg-slate-900 text-white'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                {one.label}
              </button>
            ))}
          </div>
          <SelectInput
            value={sort}
            onChange={(event) => {
              setSort(event.target.value);
              setPage(0);
            }}
            className="ml-auto w-auto"
          >
            {SORTS.map((one) => (
              <option key={one.id} value={one.id}>{one.label}</option>
            ))}
          </SelectInput>
        </div>

        {problem ? (
          <EmptyState
            icon={AlertTriangle}
            title={problem.error ? problem.error.title : 'The catalogue could not be loaded'}
            description={
              problem.error
                ? `${problem.error.message} ${problem.error.hint ?? ''}`
                : problem.bodyJson?.message || `The backend answered ${problem.status}.`
            }
            action={
              <div className="flex gap-2">
                <Button onClick={load}>Try again</Button>
                <Button look="quiet" onClick={() => inspect(log.find((one) => one.result === problem))}>
                  View the technical details
                </Button>
              </div>
            }
          />
        ) : (
          <Table
            head={
              <>
                <Th>Plan</Th>
                <Th>Status</Th>
                <Th>Price</Th>
                <Th>Limits</Th>
                <Th>Can be bought</Th>
              </>
            }
          >
            {loading && !data ? (
              <SkeletonRows rows={6} columns={5} />
            ) : rows.length === 0 ? (
              <tbody>
                <tr>
                  <td colSpan={5}>
                    <EmptyState
                      icon={Package}
                      title={search || filter !== 'all' ? 'No plans match' : 'No plans yet'}
                      description={
                        search || filter !== 'all'
                          ? 'Try a different search, or clear the filters.'
                          : 'Every plan starts as a draft, so nothing can be bought until one is published.'
                      }
                      action={
                        search || filter !== 'all' ? (
                          <Button onClick={() => { setSearch(''); changeFilter('all'); }}>
                            Clear the filters
                          </Button>
                        ) : (
                          <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>
                            New plan
                          </Button>
                        )
                      }
                    />
                  </td>
                </tr>
              </tbody>
            ) : (
              <tbody className={loading ? 'opacity-50 transition' : 'transition'}>
                {rows.map((plan) => {
                  const reason = sellableReason(plan);
                  return (
                    <tr
                      key={plan.planId}
                      onClick={() => onOpenPlan(plan)}
                      className="cursor-pointer border-t border-slate-100 transition hover:bg-blue-50/40"
                    >
                      <Td>
                        <span className="block font-medium text-slate-900">{plan.name}</span>
                        <span className="block font-mono text-[11px] text-slate-500">
                          {plan.planCode} · v{plan.planVersion}
                        </span>
                      </Td>
                      <Td><PlanStatusBadge status={plan.status} /></Td>
                      <Td className="whitespace-nowrap">
                        <span className="block">{money(plan.listPrice, plan.currencyCode)}</span>
                        <span className="block text-[11px] text-slate-500">
                          {(plan.billingCycle || '').toLowerCase().replace('_', ' ')}
                        </span>
                      </Td>
                      <Td className="whitespace-nowrap text-slate-600">
                        <span className="block text-[11px]">{plan.maxStudents} students</span>
                        <span className="block text-[11px]">{plan.maxUsers} users</span>
                      </Td>
                      <Td>
                        {plan.sellable ? (
                          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-emerald-700">
                            <ShoppingCart size={13} /> Yes
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 text-xs text-slate-500"
                            title="A plan is sellable only when it is published, on the public list, and inside its selling window">
                            <Lock size={13} className="text-slate-400" /> {reason}
                          </span>
                        )}
                      </Td>
                    </tr>
                  );
                })}
              </tbody>
            )}
          </Table>
        )}

        {data && rows.length > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-4 py-3">
            <p className="text-xs text-slate-500">
              Showing <span className="font-medium text-slate-700">{from}–{to}</span> of{' '}
              <span className="font-medium text-slate-700">{data.totalElements}</span>
            </p>
            <div className="flex items-center gap-2">
              <SelectInput
                value={size}
                onChange={(event) => { setSize(Number(event.target.value)); setPage(0); }}
                className="w-auto py-1.5 text-xs"
              >
                {[10, 20, 50, 100].map((one) => (
                  <option key={one} value={one}>{one} per page</option>
                ))}
              </SelectInput>
              <Button size="sm" icon={ChevronLeft} disabled={!data.hasPrevious}
                onClick={() => setPage((was) => Math.max(0, was - 1))}>
                Previous
              </Button>
              <Badge>Page {data.page + 1} of {Math.max(1, data.totalPages)}</Badge>
              <Button size="sm" disabled={!data.hasNext} onClick={() => setPage((was) => was + 1)}>
                Next
                <ChevronRight size={15} />
              </Button>
            </div>
          </div>
        )}
      </div>

      <NewPlanModal
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={(plan) => {
          setCreating(false);
          load();
          onOpenPlan(plan);
        }}
      />
    </div>
  );
}
