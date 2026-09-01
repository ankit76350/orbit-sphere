/**
 * The school's academic years and the days it is closed.
 *
 * Everything on this screen is read from the backend: the list of years, the year itself, and
 * its whole calendar. Changing something re-reads the calendar rather than guessing at what
 * the change did.
 */

import { useCallback, useEffect, useState } from 'react';
import {
  CalendarDays, Plus, Trash2, Pencil, Repeat, Upload, RefreshCw, CalendarSearch, Sigma,
} from 'lucide-react';
import { useApi } from '../api/apiContext.js';
import {
  Card, Button, Field, TextInput, SelectInput, TextArea, Modal, Badge, Toggle, EmptyState,
  Detail, Loading,
} from '../components/ui.jsx';

const HOLIDAY_TYPES = [
  ['PUBLIC_HOLIDAY', 'Public holiday'],
  ['FESTIVAL', 'Festival'],
  ['RELIGIOUS', 'Religious'],
  ['WEEKLY_OFF', 'Weekly off'],
  ['SCHOOL_EVENT', 'School event'],
  ['VACATION', 'Vacation'],
  ['EXAM_BREAK', 'Exam break'],
  ['OTHER', 'Other'],
];

const TYPE_LABEL = Object.fromEntries(HOLIDAY_TYPES);

const TYPE_LOOK = {
  WEEKLY_OFF: 'grey',
  PUBLIC_HOLIDAY: 'blue',
  FESTIVAL: 'violet',
  RELIGIOUS: 'violet',
  SCHOOL_EVENT: 'green',
  VACATION: 'amber',
  EXAM_BREAK: 'red',
  OTHER: 'grey',
};

const WEEKDAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

const nice = (word) => (word ? word.charAt(0) + word.slice(1).toLowerCase() : '');
const onDate = (iso) => new Date(`${iso}T00:00:00`);
const showDate = (iso) => (iso ? onDate(iso).toLocaleDateString() : '—');

/** Groups the closed days by month, so a year's calendar can be read down the page. */
function byMonth(days) {
  const groups = [];
  (days || []).forEach((day) => {
    const date = onDate(day.date);
    const key = `${date.getFullYear()}-${date.getMonth()}`;
    let group = groups.find((one) => one.key === key);
    if (!group) {
      group = {
        key,
        label: date.toLocaleDateString(undefined, { month: 'long', year: 'numeric' }),
        days: [],
      };
      groups.push(group);
    }
    group.days.push(day);
  });
  return groups;
}

export default function AcademicYearPage({ school }) {
  const { call } = useApi();

  const [years, setYears] = useState(null);
  const [yearName, setYearName] = useState('');
  const [year, setYear] = useState(null);
  const [calendar, setCalendar] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(null);
  const [dialog, setDialog] = useState(null);
  const [errors, setErrors] = useState({});

  const [createForm, setCreateForm] = useState({ name: '', startDate: '', endDate: '' });
  const [datesForm, setDatesForm] = useState({ startDate: '', endDate: '' });
  const [holidayForm, setHolidayForm] = useState({ name: '', description: '', type: 'FESTIVAL', date: '' });
  const [editing, setEditing] = useState(null);
  const [weeklyForm, setWeeklyForm] = useState({ dayOfWeek: 'SUNDAY', fromDate: '', toDate: '', name: '' });
  const [importRows, setImportRows] = useState([{ date: '', name: '', type: 'PUBLIC_HOLIDAY' }]);

  const [dayCheck, setDayCheck] = useState({ date: '', answer: null });
  const [range, setRange] = useState({ from: '', to: '', answer: null });

  const tenant = { subdomain: school.subdomain };

  /** Runs one action on the year, keeping any field errors for whichever form is open. */
  const act = useCallback(
    async (key, label, endpointId, options = {}) => {
      setBusy(key);
      setErrors({});
      const result = await call(endpointId, { label, ...tenant, ...options });
      setBusy(null);
      if (!result.ok && result.bodyJson?.fieldErrors) {
        setErrors(
          Object.fromEntries(
            Object.entries(result.bodyJson.fieldErrors).map(([k, v]) => [
              k,
              (Array.isArray(v) ? v : [v]).join(', '),
            ]),
          ),
        );
      }
      return result;
    },
    [call, school.subdomain], // eslint-disable-line react-hooks/exhaustive-deps
  );

  /* ------------------------------------------------------------------ reads */

  const loadYears = useCallback(async () => {
    setLoading(true);
    const result = await call('list-academic-years', { label: 'Load the years', ...tenant });
    setLoading(false);
    if (!result.ok) {
      setYears([]);
      return;
    }
    const list = result.bodyJson ?? [];
    setYears(list);
    // Open on the year that contains today, which is the one somebody almost always wants.
    setYearName((was) => was || list.find((one) => one.current)?.name || list[0]?.name || '');
  }, [call, school.subdomain]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadYear = useCallback(
    async (name) => {
      if (!name) {
        setYear(null);
        setCalendar(null);
        return;
      }
      const [oneYear, oneCalendar] = await Promise.all([
        call('get-academic-year', { label: 'Load the year', ...tenant, pathParams: { name } }),
        call('get-holiday-calendar', { label: 'Load the calendar', ...tenant, pathParams: { name } }),
      ]);
      setYear(oneYear.ok ? oneYear.bodyJson : null);
      setCalendar(oneCalendar.ok ? oneCalendar.bodyJson : null);
    },
    [call, school.subdomain], // eslint-disable-line react-hooks/exhaustive-deps
  );

  useEffect(() => {
    loadYears();
  }, [loadYears]);

  useEffect(() => {
    loadYear(yearName);
  }, [loadYear, yearName]);

  /** After anything that changes the calendar, read it back rather than guessing. */
  const refresh = async (result) => {
    if (!result.ok) return;
    setDialog(null);
    setEditing(null);
    await loadYear(yearName);
    loadYears();
  };

  /* ------------------------------------------------------------------ writes */

  const createYear = async () => {
    const result = await act('create', 'Create the academic year', 'create-academic-year', {
      body: createForm,
    });
    if (result.ok) {
      setDialog(null);
      setYearName(result.bodyJson.name);
      loadYears();
    }
  };

  const saveDates = async () =>
    refresh(
      await act('dates', 'Change the year dates', 'update-academic-year-dates', {
        pathParams: { name: yearName },
        body: Object.fromEntries(Object.entries(datesForm).filter(([, v]) => v)),
      }),
    );

  const setGate = async (endpointId, label, key) => {
    const result = await act(key, label, endpointId, { pathParams: { name: yearName } });
    if (result.ok) setYear(result.bodyJson);
  };

  const addHoliday = async () =>
    refresh(
      await act('add', 'Add a closed day', 'add-holiday', {
        pathParams: { name: yearName },
        body: Object.fromEntries(Object.entries(holidayForm).filter(([, v]) => v !== '')),
      }),
    );

  const saveHoliday = async () => {
    const body = {};
    if (editing.name !== editing.original.name) body.name = editing.name;
    if (editing.description !== (editing.original.description ?? '')) body.description = editing.description;
    if (editing.type !== editing.original.type) body.newType = editing.type;
    refresh(
      await act('edit', 'Change a closed day', 'update-holiday', {
        pathParams: { name: yearName, date: editing.date },
        query: { type: editing.original.type },
        body,
      }),
    );
  };

  const removeReason = async (date, type, label) =>
    refresh(
      await act(`remove-${date}-${type}`, `Remove “${label}”`, 'remove-holiday', {
        pathParams: { name: yearName, date },
        query: { type },
      }),
    );

  const removeWholeDay = async (date) =>
    refresh(
      await act(`remove-day-${date}`, 'Reopen the school that day', 'remove-holiday', {
        pathParams: { name: yearName, date },
      }),
    );

  const generateWeekly = async () =>
    refresh(
      await act('weekly', 'Add the weekly day off', 'generate-weekly-off', {
        pathParams: { name: yearName },
        body: Object.fromEntries(Object.entries(weeklyForm).filter(([, v]) => v !== '')),
      }),
    );

  const clearWeeklyOffs = async () =>
    refresh(
      await act('clearWeekly', 'Clear the weekly days off', 'remove-holidays-by-type', {
        pathParams: { name: yearName },
        query: { type: 'WEEKLY_OFF' },
      }),
    );

  const importCalendar = async (rows) =>
    refresh(
      await act('import', 'Replace the calendar', 'replace-holiday-calendar', {
        pathParams: { name: yearName },
        body: rows,
      }),
    );

  /* ------------------------------------------------------- the two questions */

  const checkDay = async () => {
    const result = await act('day', 'Check a day', 'get-day-status', {
      pathParams: { name: yearName, date: dayCheck.date },
    });
    setDayCheck((was) => ({ ...was, answer: result.ok ? result.bodyJson : null }));
  };

  const countWorkingDays = async () => {
    const result = await act('range', 'Count the working days', 'count-working-days', {
      pathParams: { name: yearName },
      query: { from: range.from || undefined, to: range.to || undefined },
    });
    setRange((was) => ({ ...was, answer: result.ok ? result.bodyJson : null }));
  };

  /* -------------------------------------------------------------------- view */

  const months = byMonth(calendar?.holidays);

  if (loading && years === null) return <Loading label="Loading the academic years…" />;

  if (years !== null && years.length === 0) {
    return (
      <Card>
        <EmptyState
          icon={CalendarDays}
          title="No academic year yet"
          description="A year is what attendance, timetables, fees and reports all hang off. Create the first one to get started."
          action={
            <Button look="primary" icon={Plus} onClick={() => setDialog({ kind: 'create' })}>
              Create an academic year
            </Button>
          }
        />
        <NewYearDialog
          open={dialog?.kind === 'create'}
          onClose={() => setDialog(null)}
          form={createForm}
          setForm={setCreateForm}
          errors={errors}
          busy={busy === 'create'}
          onSave={createYear}
        />
      </Card>
    );
  }

  return (
    <div className="space-y-5">
      {/* Which year */}
      <Card
        title="Academic year"
        description="Everything on this page applies to the year chosen here."
        action={
          <div className="flex gap-2">
            <Button icon={RefreshCw} size="sm" onClick={() => loadYear(yearName)} title="Read it again" />
            <Button look="primary" icon={Plus} size="sm" onClick={() => setDialog({ kind: 'create' })}>
              New year
            </Button>
          </div>
        }
      >
        <div className="flex flex-wrap items-end gap-x-6 gap-y-3">
          <Field label="Year" className="w-48">
            <SelectInput value={yearName} onChange={(event) => setYearName(event.target.value)}>
              {(years ?? []).map((one) => (
                <option key={one.name} value={one.name}>
                  {one.name}
                  {one.current ? ' — this year' : ''}
                </option>
              ))}
            </SelectInput>
          </Field>

          {year && (
            <>
              <Detail label="Runs">
                {showDate(year.startDate)} – {showDate(year.endDate)}
              </Detail>
              <Detail label="Length">{year.durationDays} days</Detail>
              <Detail label="Closed days">{year.holidayCount}</Detail>
              {year.current && <Badge look="green">Contains today</Badge>}
              <Button
                size="sm"
                icon={Pencil}
                className="ml-auto"
                onClick={() => {
                  setDatesForm({ startDate: year.startDate, endDate: year.endDate });
                  setDialog({ kind: 'dates' });
                }}
              >
                Change the dates
              </Button>
            </>
          )}
        </div>
      </Card>

      {/* Gates */}
      {year && (
        <Card title="Admissions and results">
          <div className="divide-y divide-slate-100">
            <Toggle
              label="Admissions are open"
              description="Whether new students can be enrolled into this year."
              checked={year.enrollmentEnabled ?? false}
              busy={busy === 'enrol'}
              onChange={(next) =>
                setGate(next ? 'enable-enrollment' : 'disable-enrollment',
                  next ? 'Open admissions' : 'Close admissions', 'enrol')
              }
            />
            <Toggle
              label="Results are locked"
              description="Once locked, marks cannot be changed. Unlocking is recorded every time."
              checked={year.resultsLocked ?? false}
              busy={busy === 'results'}
              onChange={(next) =>
                setGate(next ? 'lock-results' : 'unlock-results',
                  next ? 'Lock the results' : 'Unlock the results', 'results')
              }
            />
          </div>
        </Card>
      )}

      {/* Two quick questions the calendar can answer */}
      {year && (
        <div className="grid gap-5 lg:grid-cols-2">
          <Card title="Is the school open?" description="Check any single day.">
            <div className="flex flex-wrap items-end gap-3">
              <Field label="Date" className="w-44">
                <TextInput
                  type="date"
                  value={dayCheck.date}
                  min={year.startDate}
                  max={year.endDate}
                  onChange={(event) => setDayCheck({ date: event.target.value, answer: null })}
                />
              </Field>
              <Button busy={busy === 'day'} disabled={!dayCheck.date} onClick={checkDay}>
                <CalendarSearch size={15} /> Check
              </Button>
            </div>
            {dayCheck.answer && (
              <div
                className={`mt-4 rounded-lg border px-4 py-3 ${
                  dayCheck.answer.closed
                    ? 'border-amber-200 bg-amber-50'
                    : 'border-emerald-200 bg-emerald-50'
                }`}
              >
                <p className={`text-sm font-medium ${dayCheck.answer.closed ? 'text-amber-900' : 'text-emerald-900'}`}>
                  {nice(dayCheck.answer.dayOfWeek)} {showDate(dayCheck.answer.date)} —{' '}
                  {dayCheck.answer.closed ? 'the school is closed' : 'a normal working day'}
                </p>
                {dayCheck.answer.closed && (
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {dayCheck.answer.events.map((event) => (
                      <Badge key={event.type} look={TYPE_LOOK[event.type] || 'grey'} title={event.description || undefined}>
                        {event.name} · {TYPE_LABEL[event.type] || event.type}
                      </Badge>
                    ))}
                  </div>
                )}
              </div>
            )}
          </Card>

          <Card title="How many working days?" description="Across any range. Leave the dates empty for the whole year.">
            <div className="flex flex-wrap items-end gap-3">
              <Field label="From" className="w-40">
                <TextInput
                  type="date"
                  value={range.from}
                  onChange={(event) => setRange({ ...range, from: event.target.value, answer: null })}
                />
              </Field>
              <Field label="To" className="w-40">
                <TextInput
                  type="date"
                  value={range.to}
                  onChange={(event) => setRange({ ...range, to: event.target.value, answer: null })}
                />
              </Field>
              <Button busy={busy === 'range'} onClick={countWorkingDays}>
                <Sigma size={15} /> Count
              </Button>
            </div>
            {range.answer && (
              <div className="mt-4 grid grid-cols-3 gap-3">
                {[
                  ['Working days', range.answer.workingDayCount, 'text-emerald-700'],
                  ['Closed days', range.answer.closedDayCount, 'text-amber-700'],
                  ['Days in range', range.answer.totalDayCount, 'text-slate-900'],
                ].map(([label, count, tone]) => (
                  <div key={label} className="rounded-lg border border-slate-200 px-3 py-2.5 text-center">
                    <p className={`text-2xl font-semibold ${tone}`}>{count}</p>
                    <p className="text-[11px] text-slate-500">{label}</p>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      )}

      {/* The calendar */}
      {year && (
        <Card
          title="Days the school is closed"
          description="A day can be closed for more than one reason — a Sunday that is also Diwali."
          action={
            <div className="flex flex-wrap gap-2">
              <Button size="sm" icon={Repeat} onClick={() => setDialog({ kind: 'weekly' })}>
                Weekly day off
              </Button>
              <Button size="sm" icon={Upload} onClick={() => setDialog({ kind: 'import' })}>
                Import
              </Button>
              <Button
                look="primary"
                size="sm"
                icon={Plus}
                onClick={() => {
                  setHolidayForm({ name: '', description: '', type: 'FESTIVAL', date: year.startDate });
                  setDialog({ kind: 'addHoliday' });
                }}
              >
                Add a closed day
              </Button>
            </div>
          }
        >
          {calendar && (
            <div className="mb-5 flex flex-wrap items-center gap-3 rounded-lg bg-slate-50 px-4 py-3">
              <span className="text-sm text-slate-700">
                <strong className="text-slate-900">{calendar.closedDayCount}</strong> closed days
              </span>
              <span className="text-slate-300">·</span>
              <span className="text-sm text-slate-700">
                <strong className="text-slate-900">{calendar.eventCount}</strong> reasons
              </span>
              <span className="ml-auto flex flex-wrap gap-1.5">
                {Object.entries(calendar.countsByType || {}).map(([type, count]) => (
                  <Badge key={type} look={TYPE_LOOK[type] || 'grey'}>
                    {`${TYPE_LABEL[type] || type} · ${count}`}
                  </Badge>
                ))}
              </span>
              {calendar.countsByType?.WEEKLY_OFF > 0 && (
                <Button size="sm" look="danger" icon={Trash2} busy={busy === 'clearWeekly'} onClick={clearWeeklyOffs}>
                  Clear weekly offs
                </Button>
              )}
            </div>
          )}

          {months.length === 0 ? (
            <EmptyState
              icon={CalendarDays}
              title="The school is open every day this year"
              description="Add a closed day, or set a weekly day off."
            />
          ) : (
            <div className="space-y-5">
              {months.map((month) => (
                <div key={month.key}>
                  <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
                    {month.label}
                  </h3>
                  <ul className="divide-y divide-slate-100 overflow-hidden rounded-lg border border-slate-200">
                    {month.days.map((day) => (
                      <li key={day.date} className="flex flex-wrap items-center gap-3 px-4 py-2.5">
                        <span className="w-14 shrink-0">
                          <span className="block text-lg font-semibold leading-none text-slate-900">
                            {onDate(day.date).getDate()}
                          </span>
                          <span className="block text-[11px] text-slate-500">
                            {nice(day.dayOfWeek).slice(0, 3)}
                          </span>
                        </span>
                        <span className="flex min-w-0 flex-1 flex-wrap gap-1.5">
                          {day.events.map((event) => (
                            <span
                              key={event.type + event.name}
                              className="inline-flex items-center gap-1 rounded-full border border-slate-200 bg-white py-0.5 pl-2.5 pr-1"
                            >
                              <span className="text-xs font-medium text-slate-800">{event.name}</span>
                              <Badge look={TYPE_LOOK[event.type] || 'grey'}>
                                {TYPE_LABEL[event.type] || event.type}
                              </Badge>
                              <button
                                type="button"
                                title="Change this reason"
                                onClick={() =>
                                  setEditing({
                                    date: day.date,
                                    name: event.name,
                                    description: event.description ?? '',
                                    type: event.type,
                                    original: event,
                                  })
                                }
                                className="rounded-full p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
                              >
                                <Pencil size={11} />
                              </button>
                              <button
                                type="button"
                                title="Remove this reason"
                                onClick={() => removeReason(day.date, event.type, event.name)}
                                className="rounded-full p-1 text-slate-400 transition hover:bg-red-50 hover:text-red-600"
                              >
                                <Trash2 size={11} />
                              </button>
                            </span>
                          ))}
                        </span>
                        <Button
                          size="sm"
                          look="quiet"
                          onClick={() => removeWholeDay(day.date)}
                          title="Remove every reason, so the school is open that day"
                        >
                          Reopen
                        </Button>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          )}
        </Card>
      )}

      {/* ----------------------------------------------------------- dialogs */}

      <NewYearDialog
        open={dialog?.kind === 'create'}
        onClose={() => setDialog(null)}
        form={createForm}
        setForm={setCreateForm}
        errors={errors}
        busy={busy === 'create'}
        onSave={createYear}
      />

      <Modal
        open={dialog?.kind === 'dates'}
        onClose={() => setDialog(null)}
        title="Change the year's dates"
        footer={
          <>
            <Button onClick={() => setDialog(null)}>Cancel</Button>
            <Button look="primary" busy={busy === 'dates'} onClick={saveDates}>
              Save the dates
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="First day" error={errors.startDate}>
              <TextInput
                type="date"
                value={datesForm.startDate}
                onChange={(event) => setDatesForm({ ...datesForm, startDate: event.target.value })}
              />
            </Field>
            <Field label="Last day" error={errors.endDate}>
              <TextInput
                type="date"
                value={datesForm.endDate}
                onChange={(event) => setDatesForm({ ...datesForm, endDate: event.target.value })}
              />
            </Field>
          </div>
          <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900">
            Making the year shorter is the risky direction. It is refused if a closed day would end
            up outside the new dates.
          </p>
        </div>
      </Modal>

      <Modal
        open={dialog?.kind === 'addHoliday'}
        onClose={() => setDialog(null)}
        title="Add a closed day"
        description="If the day is already closed, this reason joins the ones already on it."
        footer={
          <>
            <Button onClick={() => setDialog(null)}>Cancel</Button>
            <Button look="primary" busy={busy === 'add'} onClick={addHoliday}>
              Add it
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Date" required error={errors.date}>
              <TextInput
                type="date"
                value={holidayForm.date}
                min={year?.startDate}
                max={year?.endDate}
                onChange={(event) => setHolidayForm({ ...holidayForm, date: event.target.value })}
              />
            </Field>
            <Field label="Kind" required error={errors.type}>
              <SelectInput
                value={holidayForm.type}
                onChange={(event) => setHolidayForm({ ...holidayForm, type: event.target.value })}
              >
                {HOLIDAY_TYPES.map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </SelectInput>
            </Field>
          </div>
          <Field label="Reason" required error={errors.name}>
            <TextInput
              value={holidayForm.name}
              onChange={(event) => setHolidayForm({ ...holidayForm, name: event.target.value })}
              placeholder="Diwali"
              autoFocus
            />
          </Field>
          <Field label="Note" hint="Optional.">
            <TextArea
              rows={2}
              value={holidayForm.description}
              onChange={(event) => setHolidayForm({ ...holidayForm, description: event.target.value })}
              placeholder="School closed for the festival of lights"
            />
          </Field>
        </div>
      </Modal>

      <Modal
        open={Boolean(editing)}
        onClose={() => setEditing(null)}
        title="Change this reason"
        description={editing ? `On ${showDate(editing.date)}` : ''}
        footer={
          <>
            <Button onClick={() => setEditing(null)}>Cancel</Button>
            <Button look="primary" busy={busy === 'edit'} onClick={saveHoliday}>
              Save
            </Button>
          </>
        }
      >
        {editing && (
          <div className="space-y-4">
            <Field label="Reason" error={errors.name}>
              <TextInput value={editing.name} onChange={(event) => setEditing({ ...editing, name: event.target.value })} />
            </Field>
            <Field label="Note" hint="Clear the box to remove the note.">
              <TextArea
                rows={2}
                value={editing.description}
                onChange={(event) => setEditing({ ...editing, description: event.target.value })}
              />
            </Field>
            <Field label="Kind">
              <SelectInput value={editing.type} onChange={(event) => setEditing({ ...editing, type: event.target.value })}>
                {HOLIDAY_TYPES.map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </SelectInput>
            </Field>
            <p className="text-xs text-slate-500">
              The date cannot be changed here. To move a holiday, remove it and add it on the new date.
            </p>
          </div>
        )}
      </Modal>

      <Modal
        open={dialog?.kind === 'weekly'}
        onClose={() => setDialog(null)}
        title="Set the weekly day off"
        description="Adds one closed day for every occurrence of that weekday in the year."
        footer={
          <>
            <Button onClick={() => setDialog(null)}>Cancel</Button>
            <Button look="primary" busy={busy === 'weekly'} onClick={generateWeekly}>
              Add them
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Field label="Which day" required error={errors.dayOfWeek}>
            <SelectInput
              value={weeklyForm.dayOfWeek}
              onChange={(event) => setWeeklyForm({ ...weeklyForm, dayOfWeek: event.target.value })}
            >
              {WEEKDAYS.map((day) => (
                <option key={day} value={day}>
                  {nice(day)}
                </option>
              ))}
            </SelectInput>
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="From" hint="Optional — the whole year by default.">
              <TextInput
                type="date"
                value={weeklyForm.fromDate}
                onChange={(event) => setWeeklyForm({ ...weeklyForm, fromDate: event.target.value })}
              />
            </Field>
            <Field label="Until" hint="Optional.">
              <TextInput
                type="date"
                value={weeklyForm.toDate}
                onChange={(event) => setWeeklyForm({ ...weeklyForm, toDate: event.target.value })}
              />
            </Field>
          </div>
          <p className="text-xs text-slate-500">
            There is no "weekly off day" setting anywhere in this system on purpose — a school may
            run on Sunday and close on another day. Every non-working day is a real dated entry,
            which is why they are generated rather than assumed.
          </p>
        </div>
      </Modal>

      <Modal
        open={dialog?.kind === 'import'}
        onClose={() => setDialog(null)}
        width="max-w-2xl"
        title="Import a calendar"
        description="This replaces the whole calendar, weekly days off included."
        footer={
          <>
            <Button onClick={() => setDialog(null)}>Cancel</Button>
            <Button look="danger" busy={busy === 'import'} onClick={() => importCalendar([])}>
              Clear the calendar
            </Button>
            <Button
              look="primary"
              busy={busy === 'import'}
              onClick={() => importCalendar(importRows.filter((row) => row.date && row.name))}
            >
              Replace the calendar
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <div className="space-y-2">
            {importRows.map((row, index) => (
              <div key={index} className="flex items-center gap-2">
                <TextInput
                  type="date"
                  value={row.date}
                  min={year?.startDate}
                  max={year?.endDate}
                  onChange={(event) =>
                    setImportRows(importRows.map((r, i) => (i === index ? { ...r, date: event.target.value } : r)))
                  }
                  className="w-40"
                />
                <TextInput
                  value={row.name}
                  placeholder="Reason"
                  onChange={(event) =>
                    setImportRows(importRows.map((r, i) => (i === index ? { ...r, name: event.target.value } : r)))
                  }
                  className="flex-1"
                />
                <SelectInput
                  value={row.type}
                  onChange={(event) =>
                    setImportRows(importRows.map((r, i) => (i === index ? { ...r, type: event.target.value } : r)))
                  }
                  className="w-44"
                >
                  {HOLIDAY_TYPES.map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </SelectInput>
                <button
                  type="button"
                  onClick={() => setImportRows(importRows.filter((_, i) => i !== index))}
                  className="rounded p-1.5 text-slate-400 transition hover:bg-red-50 hover:text-red-600"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            ))}
          </div>
          <Button
            size="sm"
            icon={Plus}
            onClick={() => setImportRows([...importRows, { date: '', name: '', type: 'PUBLIC_HOLIDAY' }])}
          >
            Add a row
          </Button>
          <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900">
            Two rows can share a date — that is a Sunday that is also a festival. What is refused is
            the same kind twice on one date.
          </p>
        </div>
      </Modal>
    </div>
  );
}

/** Used from two places — the empty state and the New year button — so it lives on its own. */
function NewYearDialog({ open, onClose, form, setForm, errors, busy, onSave }) {
  return (
    <Modal
      open={open}
      onClose={onClose}
      title="New academic year"
      description="A year starts with an empty calendar. Closed days are added afterwards."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button look="primary" busy={busy} onClick={onSave}>
            Create the year
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Field
          label="Name"
          required
          error={errors.name}
          hint="Other records point at a year by this name, so it can never be changed afterwards."
        >
          <TextInput
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            placeholder="2026-2027"
            className="font-mono"
            autoFocus
          />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="First day" required error={errors.startDate}>
            <TextInput
              type="date"
              value={form.startDate}
              onChange={(event) => setForm({ ...form, startDate: event.target.value })}
            />
          </Field>
          <Field label="Last day" required error={errors.endDate}>
            <TextInput
              type="date"
              value={form.endDate}
              onChange={(event) => setForm({ ...form, endDate: event.target.value })}
            />
          </Field>
        </div>
        <p className="text-xs text-slate-500">
          Years cannot overlap, and a year has to be roughly a year long — between 30 and 800 days.
        </p>
      </div>
    </Modal>
  );
}
