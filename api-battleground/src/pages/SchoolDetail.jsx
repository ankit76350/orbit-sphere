/**
 * One school: what it is, what state it is in, and the things you can do to it.
 *
 * The buttons that show depend on where the school is in its life — a school being set up gets
 * "Finish setting up", a live one gets "Suspend". Actions that need a reason ask for it in a
 * dialog rather than making somebody write JSON.
 */

import { useState } from 'react';
import {
  ArrowLeft, CheckCircle2, PauseCircle, PlayCircle, Wrench, Globe, Settings, CalendarDays, Info,
} from 'lucide-react';
import { useApi } from '../api/apiContext.js';
import {
  Button, Card, StatusBadge, Detail, Modal, Field, TextInput, TextArea,
} from '../components/ui.jsx';
import SchoolSettings from './SchoolSettings.jsx';
import AcademicYearPage from './AcademicYearPage.jsx';

/** What can be done next, given where the school is now. */
function actionsFor(status) {
  switch (status) {
    case 'PROVISIONING':
    case 'TRIAL':
      return ['complete', 'activate'];
    case 'ACTIVE':
      return ['suspend'];
    case 'SUSPENDED':
      return ['reactivate'];
    default:
      return [];
  }
}

const TABS = [
  { id: 'overview', label: 'Overview', icon: Info },
  { id: 'settings', label: 'Settings', icon: Settings },
  { id: 'year', label: 'Academic year', icon: CalendarDays },
];

export default function SchoolDetail({ school, onBack, onChanged }) {
  const { call } = useApi();
  const [tab, setTab] = useState('overview');
  const [busy, setBusy] = useState(null);
  const [asking, setAsking] = useState(null);
  const [reason, setReason] = useState('');
  const [newSubdomain, setNewSubdomain] = useState('');

  const actions = actionsFor(school.status);

  /** Runs one of the lifecycle actions and folds the answer back into the school on screen. */
  const run = async (id, label, endpointId, body) => {
    setBusy(id);
    const result = await call(endpointId, {
      label,
      pathParams: { id: school.schoolId },
      body,
    });
    setBusy(null);
    if (result.ok) {
      onChanged({ ...school, ...result.bodyJson });
      setAsking(null);
      setReason('');
      setNewSubdomain('');
    }
    return result;
  };

  return (
    <div className="space-y-5">
      <button
        type="button"
        onClick={onBack}
        className="inline-flex items-center gap-1.5 text-sm text-slate-500 transition hover:text-slate-800"
      >
        <ArrowLeft size={15} /> All schools
      </button>

      {/* Header */}
      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2.5">
              <h1 className="text-xl font-semibold text-slate-900">{school.schoolName}</h1>
              <StatusBadge status={school.status} />
            </div>
            <p className="mt-1 flex items-center gap-1.5 text-sm text-slate-500">
              <Globe size={14} className="text-slate-400" />
              <span className="font-mono">{school.subdomain}</span>
            </p>
            {school.statusReason && (
              <p className="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">
                <span className="font-medium text-slate-700">Last status note:</span>{' '}
                {school.statusReason}
              </p>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {actions.includes('complete') && (
              <Button
                icon={Wrench}
                busy={busy === 'complete'}
                onClick={() => run('complete', 'Finish setting up', 'complete-provisioning')}
              >
                Finish setting up
              </Button>
            )}
            {actions.includes('activate') && (
              <Button
                look="primary"
                icon={CheckCircle2}
                busy={busy === 'activate'}
                onClick={() => run('activate', 'Take the school live', 'activate-school')}
              >
                Take it live
              </Button>
            )}
            {actions.includes('suspend') && (
              <Button look="danger" icon={PauseCircle} onClick={() => setAsking('suspend')}>
                Suspend
              </Button>
            )}
            {actions.includes('reactivate') && (
              <Button
                look="primary"
                icon={PlayCircle}
                busy={busy === 'reactivate'}
                onClick={() => setAsking('reactivate')}
              >
                Let it back in
              </Button>
            )}
            <Button icon={Globe} onClick={() => setAsking('subdomain')}>
              Change web address
            </Button>
          </div>
        </div>

        {/* What to do next, in a sentence */}
        {(school.status === 'PROVISIONING' || school.status === 'TRIAL') && (
          <p className="mt-4 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2.5 text-xs text-blue-900">
            This school is not usable yet. <strong>Finish setting up</strong> creates its roles and
            number sequences, then <strong>Take it live</strong> opens it.
          </p>
        )}
      </div>

      {/* Tabs */}
      <div className="flex gap-1 border-b border-slate-200">
        {TABS.map((one) => (
          <button
            key={one.id}
            type="button"
            onClick={() => setTab(one.id)}
            className={`-mb-px flex items-center gap-1.5 border-b-2 px-3.5 py-2.5 text-sm font-medium transition ${
              tab === one.id
                ? 'border-blue-600 text-blue-700'
                : 'border-transparent text-slate-500 hover:text-slate-800'
            }`}
          >
            <one.icon size={15} />
            {one.label}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <div className="grid gap-5 lg:grid-cols-2">
          <Card title="Contact">
            <dl className="grid grid-cols-2 gap-x-4 gap-y-4">
              <Detail label="Account holder">{school.accountHolderName}</Detail>
              <Detail label="Email">{school.emailAddress}</Detail>
              <Detail label="Phone">{school.phoneNumber}</Detail>
              <Detail label="Web address">
                <span className="font-mono text-xs">{school.subdomain}</span>
              </Detail>
            </dl>
          </Card>

          <Card title="Where it is">
            <dl className="grid grid-cols-2 gap-x-4 gap-y-4">
              <Detail label="City">{school.city}</Detail>
              <Detail label="Country">{school.countryCode}</Detail>
              <Detail label="Language">{school.defaultLocale}</Detail>
              <Detail label="Time zone">{school.defaultTimeZone}</Detail>
            </dl>
          </Card>

          <Card title="History" className="lg:col-span-2">
            <dl className="grid gap-x-4 gap-y-4 sm:grid-cols-3">
              <Detail label="Added">
                {school.createdAt ? new Date(school.createdAt).toLocaleString() : null}
              </Detail>
              <Detail label="Went live">
                {school.activatedAt ? new Date(school.activatedAt).toLocaleString() : null}
              </Detail>
              <Detail label="Last suspended">
                {school.suspendedAt ? new Date(school.suspendedAt).toLocaleString() : null}
              </Detail>
            </dl>
          </Card>
        </div>
      )}

      {tab === 'settings' && <SchoolSettings school={school} onChanged={onChanged} />}
      {tab === 'year' && <AcademicYearPage school={school} />}

      {/* Suspend */}
      <Modal
        open={asking === 'suspend'}
        onClose={() => setAsking(null)}
        title="Suspend this school"
        description="Nobody at the school will be able to use it until it is let back in."
        footer={
          <>
            <Button onClick={() => setAsking(null)}>Cancel</Button>
            <Button
              look="solidDanger"
              busy={busy === 'suspend'}
              disabled={!reason.trim()}
              onClick={() => run('suspend', 'Suspend the school', 'suspend-school', { reason })}
            >
              Suspend the school
            </Button>
          </>
        }
      >
        <Field
          label="Why is it being suspended?"
          required
          hint="This is kept on the record, and stays there even after the school is let back in."
        >
          <TextArea
            rows={3}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Non-payment. Third invoice unpaid past 60 days."
            autoFocus
          />
        </Field>
      </Modal>

      {/* Reactivate */}
      <Modal
        open={asking === 'reactivate'}
        onClose={() => setAsking(null)}
        title="Let this school back in"
        description="Access is restored straight away."
        footer={
          <>
            <Button onClick={() => setAsking(null)}>Cancel</Button>
            <Button
              look="primary"
              busy={busy === 'reactivate'}
              onClick={() =>
                run('reactivate', 'Let the school back in', 'reactivate-school',
                  reason.trim() ? { note: reason.trim() } : undefined)
              }
            >
              Let it back in
            </Button>
          </>
        }
      >
        <Field
          label="Add a note"
          hint="Optional. Leave it empty and the suspension reason stays on the record."
        >
          <TextArea
            rows={3}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Outstanding invoices cleared on 27 August."
          />
        </Field>
      </Modal>

      {/* Change subdomain */}
      <Modal
        open={asking === 'subdomain'}
        onClose={() => setAsking(null)}
        title="Change the web address"
        description="Every saved link and bookmark to the old address stops working."
        footer={
          <>
            <Button onClick={() => setAsking(null)}>Cancel</Button>
            <Button
              look="primary"
              busy={busy === 'subdomain'}
              disabled={!newSubdomain.trim()}
              onClick={() =>
                run('subdomain', 'Change the web address', 'change-subdomain', {
                  currentSubdomain: school.subdomain,
                  newSubdomain: newSubdomain.trim(),
                })
              }
            >
              Change it
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Detail label="Current address">
            <span className="font-mono text-xs">{school.subdomain}</span>
          </Detail>
          <Field
            label="New address"
            required
            hint={newSubdomain ? `The school will move to ${newSubdomain}.orbitastra.com` : undefined}
          >
            <TextInput
              value={newSubdomain}
              onChange={(event) => setNewSubdomain(event.target.value)}
              placeholder="orbit-astra"
              className="font-mono"
              autoFocus
            />
          </Field>
          <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900">
            Some names are kept for the platform and will be refused — <span className="font-mono">api</span>,{' '}
            <span className="font-mono">www</span>, <span className="font-mono">login</span> and others.
          </p>
        </div>
      </Modal>

    </div>
  );
}
