/**
 * The school list — the screen an operator would actually live on.
 *
 * Search, filter by status, sort, page. Clicking a row opens that school.
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { Plus, School as SchoolIcon, RefreshCw, ChevronLeft, ChevronRight, AlertTriangle } from 'lucide-react';
import { useApi, useApiState } from '../api/apiContext.js';
import {
  Button,
  SearchInput,
  SelectInput,
  StatusBadge,
  Table,
  Th,
  Td,
  EmptyState,
  SkeletonRows,
  Badge,
} from '../components/ui.jsx';
import NewSchoolModal from './NewSchoolModal.jsx';

const STATUS_FILTERS = [
  { id: 'all', label: 'All', statuses: [] },
  { id: 'live', label: 'Live', statuses: ['ACTIVE'] },
  { id: 'trial', label: 'On trial', statuses: ['TRIAL'] },
  { id: 'setup', label: 'Being set up', statuses: ['PROVISIONING'] },
  { id: 'suspended', label: 'Suspended', statuses: ['SUSPENDED'] },
];

const SORTS = [
  { id: 'createdAt,desc', label: 'Newest first' },
  { id: 'createdAt,asc', label: 'Oldest first' },
  { id: 'name,asc', label: 'Name A–Z' },
  { id: 'name,desc', label: 'Name Z–A' },
  { id: 'status,asc', label: 'Status' },
];

export default function SchoolsPage({ onOpenSchool }) {
  const { call, inspect } = useApi();
  const { log, environment } = useApiState();

  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState('all');
  const [sort, setSort] = useState('createdAt,desc');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [problem, setProblem] = useState(null);
  const [creating, setCreating] = useState(false);

  // Counts the loads, so an answer to an older request cannot land on top of a newer one when
  // somebody types quickly.
  const latestLoad = useRef(0);

  const load = useCallback(async () => {
    const ticket = ++latestLoad.current;
    setLoading(true);
    const statuses = STATUS_FILTERS.find((one) => one.id === filter)?.statuses ?? [];
    const result = await call('list-schools', {
      label: 'Load schools',
      query: {
        search: search.trim() || undefined,
        status: statuses.length ? statuses : undefined,
        page,
        size,
        sort,
      },
    });
    if (ticket !== latestLoad.current) return;
    setLoading(false);
    if (result.ok) {
      setData(result.bodyJson);
      setProblem(null);
    } else {
      setProblem(result);
    }
    // environment.id is in here so switching backend in the header reloads the list.
  }, [call, environment.id, search, filter, sort, page, size]);

  // The search box waits a moment before asking, so typing does not fire a call per keystroke.
  useEffect(() => {
    const timer = setTimeout(load, search ? 350 : 0);
    return () => clearTimeout(timer);
  }, [load, search]);

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
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Schools</h1>
          <p className="mt-0.5 text-sm text-slate-500">
            {data
              ? `${data.totalElements} school${data.totalElements === 1 ? '' : 's'} on the platform`
              : 'Every school on the platform'}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button icon={RefreshCw} onClick={load} busy={loading}>
            Refresh
          </Button>
          <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>
            Add a school
          </Button>
        </div>
      </header>

      <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
        {/* Filters */}
        <div className="flex flex-wrap items-center gap-3 border-b border-slate-100 px-4 py-3">
          <SearchInput
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Search by name or subdomain"
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
              <option key={one.id} value={one.id}>
                {one.label}
              </option>
            ))}
          </SelectInput>
        </div>

        {problem ? (
          <EmptyState
            icon={AlertTriangle}
            title={problem.error ? problem.error.title : 'The list could not be loaded'}
            description={
              problem.error
                ? `${problem.error.message} ${problem.error.hint ?? ''}`
                : problem.bodyJson?.message || `The backend answered ${problem.status}.`
            }
            action={
              <div className="flex gap-2">
                <Button onClick={load}>Try again</Button>
                <Button
                  look="quiet"
                  onClick={() => inspect(log.find((one) => one.result === problem))}
                >
                  View the technical details
                </Button>
              </div>
            }
          />
        ) : (
          <Table
            head={
              <>
                <Th>School</Th>
                <Th>Status</Th>
                <Th>Account holder</Th>
                <Th>Where</Th>
                <Th>Added</Th>
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
                      icon={SchoolIcon}
                      title={search || filter !== 'all' ? 'No schools match' : 'No schools yet'}
                      description={
                        search || filter !== 'all'
                          ? 'Try a different search, or clear the filters.'
                          : 'Add the first school to get started.'
                      }
                      action={
                        search || filter !== 'all' ? (
                          <Button
                            onClick={() => {
                              setSearch('');
                              changeFilter('all');
                            }}
                          >
                            Clear the filters
                          </Button>
                        ) : (
                          <Button look="primary" icon={Plus} onClick={() => setCreating(true)}>
                            Add a school
                          </Button>
                        )
                      }
                    />
                  </td>
                </tr>
              </tbody>
            ) : (
              <tbody className={loading ? 'opacity-50 transition' : 'transition'}>
                {rows.map((school) => (
                  <tr
                    key={school.schoolId}
                    onClick={() => onOpenSchool(school)}
                    className="cursor-pointer border-t border-slate-100 transition hover:bg-blue-50/40"
                  >
                    <Td>
                      <span className="block font-medium text-slate-900">{school.schoolName}</span>
                      <span className="block font-mono text-[11px] text-slate-500">
                        {school.subdomain}
                      </span>
                    </Td>
                    <Td>
                      <StatusBadge status={school.status} />
                    </Td>
                    <Td>
                      <span className="block">{school.accountHolderName || '—'}</span>
                      {school.emailAddress && (
                        <span className="block text-[11px] text-slate-500">{school.emailAddress}</span>
                      )}
                    </Td>
                    <Td>
                      {[school.city, school.countryCode].filter(Boolean).join(', ') || (
                        <span className="text-slate-400">Not set</span>
                      )}
                    </Td>
                    <Td className="whitespace-nowrap text-slate-500">
                      {school.createdAt ? new Date(school.createdAt).toLocaleDateString() : '—'}
                    </Td>
                  </tr>
                ))}
              </tbody>
            )}
          </Table>
        )}

        {/* Paging */}
        {data && rows.length > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 px-4 py-3">
            <p className="text-xs text-slate-500">
              Showing <span className="font-medium text-slate-700">{from}–{to}</span> of{' '}
              <span className="font-medium text-slate-700">{data.totalElements}</span>
            </p>
            <div className="flex items-center gap-2">
              <SelectInput
                value={size}
                onChange={(event) => {
                  setSize(Number(event.target.value));
                  setPage(0);
                }}
                className="w-auto py-1.5 text-xs"
              >
                {[10, 20, 50, 100].map((one) => (
                  <option key={one} value={one}>
                    {one} per page
                  </option>
                ))}
              </SelectInput>
              <Button
                size="sm"
                icon={ChevronLeft}
                disabled={!data.hasPrevious}
                onClick={() => setPage((was) => Math.max(0, was - 1))}
              >
                Previous
              </Button>
              <Badge>
                Page {data.page + 1} of {Math.max(1, data.totalPages)}
              </Badge>
              <Button size="sm" disabled={!data.hasNext} onClick={() => setPage((was) => was + 1)}>
                Next
                <ChevronRight size={15} />
              </Button>
            </div>
          </div>
        )}
      </div>

      <NewSchoolModal
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={(school) => {
          setCreating(false);
          load();
          onOpenSchool(school);
        }}
      />
    </div>
  );
}
