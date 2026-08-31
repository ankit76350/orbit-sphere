/**
 * The school's academic year and its closed days.
 *
 * One thing to know, and the screen says it out loud: the backend has no endpoint that READS a
 * year or its calendar. Every academic-year endpoint is a write. So this page shows the answer
 * from the last change made here, rather than pretending it fetched the year on load. When a
 * read endpoint exists this is the one place that has to change.
 */

import { useState } from 'react';
import {
  CalendarDays, Plus, Trash2, Pencil, Repeat, Upload, Info,
} from 'lucide-react';
import { useApi } from '../api/ApiProvider.jsx';
import {
  Card, Button, Field, TextInput, SelectInput, TextArea, Modal, Badge, Toggle, EmptyState, Detail,
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

const nice = (word) => word.charAt(0) + word.slice(1).toLowerCase();

/** Groups the closed days by month, so a year's calendar can be read down the page. */
function byMonth(days) {
  const groups = [];
  (days || []).forEach((day) => {
    const date = new Date(`${day.date}T00:00:00`);
    const key = `${date.getFullYear()}-${date.getMonth()}`;
    const label = date.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
    let group = groups.find((one) => one.key === key);
    if (!group) {
      group = { key, label, days: [] };
      groups.push(group);
    }
    group.days.push(day);
  });
  return groups;
}

export default function AcademicYearPage({ school }) {
  const { call } = useApi();

  const [yearName, setYearName] = useState('');
  const [year, setYear] = useState(null);        // the last AcademicYearResponse
  const [calendar, setCalendar] = useState(null); // the last HolidayCalendarResponse
  const [busy, setBusy] = useState(null);
  const [dialog, setDialog] = useState(null);
  const [errors, setErrors] = useState({});

  const [createForm, setCreateForm] = useState({ name: '', startDate: '', endDate: '' });
  const [datesForm, setDatesForm] = useState({ startDate: '', endDate: '' });
  const [holidayForm, setHolidayForm] = useState({ name: '', description: '', type: 'FESTIVAL', date: '' });
  const [editing, setEditing] = useState(null);
  const [weeklyForm, setWeeklyForm] = useState({ dayOfWeek: 'SUNDAY', fromDate: '', toDate: '', name: '' });
  const [importRows, setImportRows] = useState([{ date: '', name: '', type: 'PUBLIC_HOLIDAY' }]);

  /** Runs one action on the year, and keeps any field errors for the open form. */
  const act = async (key, label, endpointId, options = {}) => {
    setBusy(key);
    setErrors({});
    const result = await call(endpointId, {
      label,
      subdomain: school.subdomain,
      pathParams: { name: yearName, ...(options.pathParams || {}) },
      ...options,
    });
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
  };

  /* ------------------------------------------------------------------ year */

  const createYear = async () => {
    const result = await act('create', 'Create the academic year', 'create-academic-year', {
      body: createForm,
      pathParams: {},
    });
    if (result.ok) {
      setYear(result.bodyJson);
      setYearName(result.bodyJson.name);
      setCalendar(null);
      setDialog(null);
      setDatesForm({ startDate: result.bodyJson.startDate, endDate: result.bodyJson.endDate });
    }
  };

  const saveDates = async () => {
    const body = Object.fromEntries(Object.entries(datesForm).filter(([, v]) => v));
    const result = await act('dates', 'Change the year dates', 'update-academic-year-dates', { body });
    if (result.ok) {
      setYear(result.bodyJson);
      setDialog(null);
    }
  };

  const setGate = async (endpointId, label, key) => {
    const result = await act(key, label, endpointId);
    if (result.ok) setYear((was) => ({ ...was, ...result.bodyJson }));
  };

  /* -------------------------------------------------------------- holidays */

  const afterCalendar = (result) => {
    if (result.ok) {
      setCalendar(result.bodyJson);
      setDialog(null);
      setEditing(null);
      if (year) setYear({ ...year, holidayCount: result.bodyJson.closedDayCount });
    }
  };

  const addHoliday = async () => {
    const body = Object.fromEntries(Object.entries(holidayForm).filter(([, v]) => v !== ''));
    afterCalendar(await act('add', 'Add a closed day', 'add-holiday', { body }));
  };

  const saveHoliday = async () => {
    const body = {};
    if (editing.name !== editing.original.name) body.name = editing.name;
    if (editing.description !== (editing.original.description ?? '')) body.description = editing.description;
    if (editing.type !== editing.original.type) body.newType = editing.type;
    afterCalendar(
      await act('edit', 'Change a closed day', 'update-holiday', {
        pathParams: { name: yearName, date: editing.date },
        query: { type: editing.original.type },
        body,
      }),
    );
  };

  const removeReason = async (date, type, label) => {
    afterCalendar(
      await act(`remove-${date}-${type}`, `Remove “${label}”`, 'remove-holiday', {
        pathParams: { name: yearName, date },
        query: { type },
      }),
    );
  };

  const removeWholeDay = async (date) => {
    afterCalendar(
      await act(`remove-day-${date}`, 'Reopen the school that day', 'remove-holiday', {
        pathParams: { name: yearName, date },
      }),
    );
  };

  const generateWeekly = async () => {
    const body = Object.fromEntries(Object.entries(weeklyForm).filter(([, v]) => v !== ''));
    const result = await act('weekly', 'Add the weekly day off', 'generate-weekly-off', { body });
    if (result.ok) {
      setDialog(null);
      // This one answers with counts rather than the calendar, so there is nothing to redraw
      // from here. The next change to a closed day brings the calendar back.
      setCalendar(null);
    }
  };

  const clearWeeklyOffs = async () => {
    afterCalendar(
      await act('clearWeekly', 'Clear the weekly days off', 'remove-holidays-by-type', {
        query: { type: 'WEEKLY_OFF' },
      }),
    );
  };

  const importCalendar = async () => {
    const rows = importRows.filter((row) => row.date && row.name);
    afterCalendar(
      await act('import', 'Replace the calendar', 'replace-holiday-calendar', { body: rows }),
    );
  };

  /* ------------------------------------------------------------------ view */

  const months = byMonth(calendar?.holidays);

  return (
    <div className="space-y-5">
      <div className="flex items-start gap-2.5 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3">
        <Info size={16} className="mt-0.5 shrink-0 text-blue-600" />
        <p className="text-xs leading-relaxed text-blue-900">
          The backend has no endpoint that reads an academic year or its calendar yet — every
          academic-year endpoint changes something. So this page shows the answer from the last
          change made here. Create a year, or type the name of one that already exists and make a
          change, to see its calendar.
        </p>
      </div>

      {/* Which year we are working on */}
      <Card
        title="Academic year"
        description="Everything on this page applies to this year."
        action={
          <Button look="primary" icon={Plus} size="sm" onClick={() => setDialog({ kind: 'create' })}>
            New year
          </Button>
        }
      >
        <div className="flex flex-wrap items-end gap-3">
          <Field label="Year name" hint="The name is the key other records use. It can never be changed." className="w-56">
            <TextInput
              value={yearName}
              onChange={(event) => setYearName(event.target.value)}
              placeholder="2026-2027"
              className="font-mono"
            />
          </Field>
          {year && (
            <div className="flex flex-wrap items-center gap-x-6 gap-y-3 pb-1">
              <Detail label="Runs from">
                {new Date(`${year.startDate}T00:00:00`).toLocaleDateString()} –{' '}
                {new Date(`${year.endDate}T00:00:00`).toLocaleDateString()}
              </Detail>
              <Detail label="Length">{year.durationDays} days</Detail>
              <Detail label="Closed days">{year.holidayCount}</Detail>
              {year.current && <Badge look="green">This is the current year</Badge>}
            </div>
          )}
          {year && (
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
          )}
        </div>
      </Card>

      {/* Gates */}
      {yearName && (
        <Card title="Admissions and results" description="Two switches, each with its own permission behind it.">
          <div className="divide-y divide-slate-100">
            <Toggle
              label="Admissions are open"
              description="Whether new students can be enrolled into this year."
              checked={year?.enrollmentEnabled ?? false}
              busy={busy === 'enrol'}
              onChange={(next) =>
                setGate(next ? 'enable-enrollment' : 'disable-enrollment',
                  next ? 'Open admissions' : 'Close admissions', 'enrol')
              }
            />
            <Toggle
              label="Results are locked"
              description="Once locked, marks cannot be changed. Unlocking is recorded every time."
              checked={year?.resultsLocked ?? false}
              busy={busy === 'results'}
              onChange={(next) =>
                setGate(next ? 'lock-results' : 'unlock-results',
                  next ? 'Lock the results' : 'Unlock the results', 'results')
              }
            />
          </div>
        </Card>
      )}

      {/* The calendar */}
      {yearName && (
        <Card
          title="Days the school is closed"
          description="A day can be closed for more than one reason — a Sunday that is also Diwali."
          action={
            <div className="flex flex-wrap gap-2">
              <Button size="sm" icon={Repeat} onClick={() => setDialog({ kind: 'weekly' })}>
                Weekly day off
              </Button>
              <Button size="sm" icon={Upload} onClick={() => setDialog({ kind: 'import' })}>
                Import a calendar
              </Button>
              <Button
                look="primary"
                size="sm"
                icon={Plus}
                onClick={() => {
                  setHolidayForm({ name: '', description: '', type: 'FESTIVAL', date: year?.startDate ?? '' });
                  setDialog({ kind: 'addHoliday' });
                }}
              >
                Add a closed day
              </Button>
            </div>
          }
        >
          {calendar ? (
            <div className="space-y-5">
              <div className="flex flex-wrap items-center gap-3 rounded-lg bg-slate-50 px-4 py-3">
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
                      {TYPE_LABEL[type] || type} · {count}
                    </Badge>
                  ))}
                </span>
                {calendar.countsByType?.WEEKLY_OFF > 0 && (
                  <Button size="sm" look="danger" icon={Trash2} busy={busy === 'clearWeekly'} onClick={clearWeeklyOffs}>
                    Clear weekly offs
                  </Button>
                )}
              </div>

              {months.length === 0 ? (
                <EmptyState
                  icon={CalendarDays}
                  title="The school is open every day this year"
                  description="Add a closed day, or set a weekly day off."
                />
              ) : (
                months.map((month) => (
                  <div key={month.key}>
                    <h3 className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
                      {month.label}
                    </h3>
                    <ul className="divide-y divide-slate-100 overflow-hidden rounded-lg border border-slate-200">
                      {month.days.map((day) => {
                        const date = new Date(`${day.date}T00:00:00`);
                        return (
                          <li key={day.date} className="flex flex-wrap items-center gap-3 px-4 py-2.5">
                            <span className="w-16 shrink-0">
                              <span className="block text-lg font-semibold leading-none text-slate-900">
                                {date.getDate()}
                              </span>
                              <span className="block text-[11px] text-slate-500">
                                {nice(day.dayOfWeek).slice(0, 3)}
                              </span>
                            </span>
                            <span className="flex min-w-0 flex-1 flex-wrap gap-1.5">
                              {day.events.map((event) => (
                                <span
                                  key={event.type + event.name}
                                  className="group inline-flex items-center gap-1 rounded-full border border-slate-200 bg-white py-0.5 pl-2.5 pr-1"
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
                        );
                      })}
                    </ul>
                  </div>
                ))
              )}
            </div>
          ) : (
            <EmptyState
              icon={CalendarDays}
              title="Nothing loaded yet"
              description="Add a closed day or import a calendar, and the whole year's calendar appears here — the backend sends it back with every change."
            />
          )}
        </Card>
      )}

      {/* ----------------------------------------------------------- dialogs */}

      <Modal
        open={dialog?.kind === 'create'}
        onClose={() => setDialog(null)}
        title="New academic year"
        description="A year starts with an empty calendar. Closed days are added afterwards."
        footer={
          <>
            <Button onClick={() => setDialog(null)}>Cancel</Button>
            <Button look="primary" busy={busy === 'create'} onClick={createYear}>
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
              value={createForm.name}
              onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
              placeholder="2026-2027"
              className="font-mono"
              autoFocus
            />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="First day" required error={errors.startDate}>
              <TextInput
                type="date"
                value={createForm.startDate}
                onChange={(e) => setCreateForm({ ...createForm, startDate: e.target.value })}
              />
            </Field>
            <Field label="Last day" required error={errors.endDate}>
              <TextInput
                type="date"
                value={createForm.endDate}
                onChange={(e) => setCreateForm({ ...createForm, endDate: e.target.value })}
              />
            </Field>
          </div>
          <p className="text-xs text-slate-500">
            Years cannot overlap, and a year has to be roughly a year long — between 30 and 800 days.
          </p>
        </div>
      </Modal>

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
                onChange={(e) => setDatesForm({ ...datesForm, startDate: e.target.value })}
              />
            </Field>
            <Field label="Last day" error={errors.endDate}>
              <TextInput
                type="date"
                value={datesForm.endDate}
                onChange={(e) => setDatesForm({ ...datesForm, endDate: e.target.value })}
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
                onChange={(e) => setHolidayForm({ ...holidayForm, date: e.target.value })}
              />
            </Field>
            <Field label="Kind" required error={errors.type}>
              <SelectInput
                value={holidayForm.type}
                onChange={(e) => setHolidayForm({ ...holidayForm, type: e.target.value })}
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
              onChange={(e) => setHolidayForm({ ...holidayForm, name: e.target.value })}
              placeholder="Diwali"
              autoFocus
            />
          </Field>
          <Field label="Note" hint="Optional.">
            <TextArea
              rows={2}
              value={holidayForm.description}
              onChange={(e) => setHolidayForm({ ...holidayForm, description: e.target.value })}
              placeholder="School closed for the festival of lights"
            />
          </Field>
        </div>
      </Modal>

      <Modal
        open={Boolean(editing)}
        onClose={() => setEditing(null)}
        title="Change this reason"
        description={editing ? `On ${new Date(`${editing.date}T00:00:00`).toLocaleDateString()}` : ''}
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
              <TextInput value={editing.name} onChange={(e) => setEditing({ ...editing, name: e.target.value })} />
            </Field>
            <Field label="Note" hint="Clear the box to remove the note.">
              <TextArea
                rows={2}
                value={editing.description}
                onChange={(e) => setEditing({ ...editing, description: e.target.value })}
              />
            </Field>
            <Field label="Kind">
              <SelectInput value={editing.type} onChange={(e) => setEditing({ ...editing, type: e.target.value })}>
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
              onChange={(e) => setWeeklyForm({ ...weeklyForm, dayOfWeek: e.target.value })}
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
                onChange={(e) => setWeeklyForm({ ...weeklyForm, fromDate: e.target.value })}
              />
            </Field>
            <Field label="Until" hint="Optional.">
              <TextInput
                type="date"
                value={weeklyForm.toDate}
                onChange={(e) => setWeeklyForm({ ...weeklyForm, toDate: e.target.value })}
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
            <Button
              look="danger"
              onClick={() => {
                setImportRows([{ date: '', name: '', type: 'PUBLIC_HOLIDAY' }]);
                importCalendar();
              }}
              busy={busy === 'import'}
            >
              Clear the calendar
            </Button>
            <Button look="primary" busy={busy === 'import'} onClick={importCalendar}>
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
                  onChange={(e) =>
                    setImportRows(importRows.map((r, i) => (i === index ? { ...r, date: e.target.value } : r)))
                  }
                  className="w-40"
                />
                <TextInput
                  value={row.name}
                  placeholder="Reason"
                  onChange={(e) =>
                    setImportRows(importRows.map((r, i) => (i === index ? { ...r, name: e.target.value } : r)))
                  }
                  className="flex-1"
                />
                <SelectInput
                  value={row.type}
                  onChange={(e) =>
                    setImportRows(importRows.map((r, i) => (i === index ? { ...r, type: e.target.value } : r)))
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
