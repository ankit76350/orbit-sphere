/**
 * The form for adding a school. An ordinary sign-up form, not a JSON editor.
 *
 * The subdomain gets a suggestion from the school's name and a live preview of the address the
 * school will be reachable at, because that is the field people get wrong.
 */

import { useEffect, useState } from 'react';
import { useApi } from '../api/apiContext.js';
import { Modal, Button, Field, TextInput, SelectInput, Badge } from '../components/ui.jsx';

const TIME_ZONES = [
  'Asia/Kolkata', 'Asia/Dubai', 'Asia/Singapore', 'Asia/Colombo', 'Asia/Kathmandu',
  'Europe/London', 'America/New_York', 'Australia/Sydney', 'UTC',
];

const LOCALES = [
  ['en-IN', 'English (India)'],
  ['en-GB', 'English (UK)'],
  ['en-US', 'English (US)'],
  ['hi-IN', 'Hindi (India)'],
  ['mr-IN', 'Marathi (India)'],
];

const COUNTRIES = [
  ['IN', 'India'], ['AE', 'United Arab Emirates'], ['SG', 'Singapore'],
  ['LK', 'Sri Lanka'], ['NP', 'Nepal'], ['GB', 'United Kingdom'],
  ['US', 'United States'], ['AU', 'Australia'],
];

const BLANK = {
  schoolName: '',
  accountHolderName: '',
  subdomain: '',
  emailAddress: '',
  phoneNumber: '',
  defaultLocale: 'en-IN',
  defaultTimeZone: 'Asia/Kolkata',
  countryCode: 'IN',
  addressLine: '',
  city: '',
  stateOrProvince: '',
  postalCode: '',
  trial: false,
};

/** Suggests a subdomain from the school's name, the way the backend will store it. */
function suggestSubdomain(name) {
  return name
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 63);
}

export default function NewSchoolModal({ open, onClose, onCreated }) {
  const { call } = useApi();
  const [form, setForm] = useState(BLANK);
  const [touchedSubdomain, setTouchedSubdomain] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [problem, setProblem] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setForm(BLANK);
      setTouchedSubdomain(false);
      setFieldErrors({});
      setProblem(null);
    }
  }, [open]);

  const set = (key) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;
    setForm((was) => {
      const next = { ...was, [key]: value };
      // Keep the subdomain in step with the name until somebody edits it themselves.
      if (key === 'schoolName' && !touchedSubdomain) next.subdomain = suggestSubdomain(value);
      return next;
    });
  };

  const save = async () => {
    setSaving(true);
    setFieldErrors({});
    setProblem(null);

    // Empty optional fields are left out rather than sent as "", which the backend stores.
    const body = Object.fromEntries(
      Object.entries(form).filter(([, value]) => value !== '' && value !== null),
    );

    const result = await call('create-school', { label: 'Add a school', body });
    setSaving(false);

    if (result.ok) {
      onCreated(result.bodyJson);
      return;
    }
    if (result.bodyJson?.fieldErrors) {
      setFieldErrors(
        Object.fromEntries(
          Object.entries(result.bodyJson.fieldErrors).map(([key, messages]) => [
            key,
            (Array.isArray(messages) ? messages : [messages]).join(', '),
          ]),
        ),
      );
    }
    setProblem(result);
  };

  const problemMessage =
    problem &&
    (problem.error
      ? problem.error.message
      : problem.bodyJson?.fieldErrors
        ? 'Some details need fixing — see the fields below.'
        : problem.bodyJson?.message || `The backend answered ${problem.status}.`);

  return (
    <Modal
      open={open}
      onClose={onClose}
      width="max-w-2xl"
      title="Add a school"
      description="This creates the school. Setting it up and taking it live are the next two steps."
      footer={
        <>
          <Button onClick={onClose}>Cancel</Button>
          <Button look="primary" onClick={save} busy={saving}>
            Create the school
          </Button>
        </>
      }
    >
      {problem && (
        <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2.5">
          <p className="text-xs text-amber-900">{problemMessage}</p>
        </div>
      )}

      <div className="space-y-5">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="School name" required error={fieldErrors.schoolName} className="sm:col-span-2">
            <TextInput
              value={form.schoolName}
              onChange={set('schoolName')}
              error={fieldErrors.schoolName}
              placeholder="Orbit Astra International School"
              autoFocus
            />
          </Field>

          <Field
            label="Web address"
            required
            error={fieldErrors.subdomain}
            hint={form.subdomain ? `The school will be at ${form.subdomain}.orbitastra.com` : 'Letters, numbers and hyphens only.'}
            className="sm:col-span-2"
          >
            <TextInput
              value={form.subdomain}
              onChange={(event) => {
                setTouchedSubdomain(true);
                set('subdomain')(event);
              }}
              error={fieldErrors.subdomain}
              placeholder="orbit-astra"
              className="font-mono"
            />
          </Field>

          <Field label="Account holder" required error={fieldErrors.accountHolderName} hint="The person on the contract.">
            <TextInput
              value={form.accountHolderName}
              onChange={set('accountHolderName')}
              error={fieldErrors.accountHolderName}
              placeholder="Rohan Shinde"
            />
          </Field>

          <Field label="Email address" error={fieldErrors.emailAddress}>
            <TextInput
              type="email"
              value={form.emailAddress}
              onChange={set('emailAddress')}
              error={fieldErrors.emailAddress}
              placeholder="office@school.edu"
            />
          </Field>

          <Field label="Phone number" error={fieldErrors.phoneNumber}>
            <TextInput value={form.phoneNumber} onChange={set('phoneNumber')} placeholder="+91 98765 43210" />
          </Field>

          <Field label="Country" required error={fieldErrors.countryCode} hint="Cannot be changed once the school is live.">
            <SelectInput value={form.countryCode} onChange={set('countryCode')}>
              {COUNTRIES.map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </SelectInput>
          </Field>
        </div>

        <div>
          <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
            Language and time
          </h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Language" required error={fieldErrors.defaultLocale}>
              <SelectInput value={form.defaultLocale} onChange={set('defaultLocale')}>
                {LOCALES.map(([code, label]) => (
                  <option key={code} value={code}>
                    {label}
                  </option>
                ))}
              </SelectInput>
            </Field>
            <Field
              label="Time zone"
              required
              error={fieldErrors.defaultTimeZone}
              hint="Decides which day attendance and timetables fall on."
            >
              <SelectInput value={form.defaultTimeZone} onChange={set('defaultTimeZone')}>
                {TIME_ZONES.map((zone) => (
                  <option key={zone} value={zone}>
                    {zone}
                  </option>
                ))}
              </SelectInput>
            </Field>
          </div>
        </div>

        <div>
          <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
            Address <span className="font-normal normal-case text-slate-400">— optional</span>
          </h3>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Street address" className="sm:col-span-2">
              <TextInput value={form.addressLine} onChange={set('addressLine')} placeholder="12, MG Road" />
            </Field>
            <Field label="City">
              <TextInput value={form.city} onChange={set('city')} placeholder="Pune" />
            </Field>
            <Field label="State or province">
              <TextInput value={form.stateOrProvince} onChange={set('stateOrProvince')} placeholder="Maharashtra" />
            </Field>
            <Field label="Postal code">
              <TextInput value={form.postalCode} onChange={set('postalCode')} placeholder="411001" />
            </Field>
          </div>
        </div>

        <label className="flex cursor-pointer items-start gap-3 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
          <input
            type="checkbox"
            checked={form.trial}
            onChange={set('trial')}
            className="mt-0.5 h-4 w-4 accent-blue-600"
          />
          <span>
            <span className="flex items-center gap-2 text-sm font-medium text-slate-800">
              Start this school on a trial
              <Badge look="blue">On trial</Badge>
            </span>
            <span className="mt-0.5 block text-xs text-slate-500">
              Otherwise it starts as “being set up”. Either way it still has to be taken live.
            </span>
          </span>
        </label>
      </div>
    </Modal>
  );
}
