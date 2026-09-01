/**
 * The settings a school changes about itself — name, address, language, logo.
 *
 * Four small forms, each saved on its own, because the backend keeps them as four separate
 * operations and a single big Save would hide which one failed.
 *
 * These all run on the school surface, so every call carries the school's subdomain as the
 * tenant. Nobody types a header.
 */

import { useCallback, useEffect, useState } from 'react';
import { Image as ImageIcon, AlertTriangle } from 'lucide-react';
import { useApi } from '../api/apiContext.js';
import EndpointTag from '../components/EndpointTag.jsx';
import { Card, Button, Field, TextInput, SelectInput, Modal, Loading } from '../components/ui.jsx';

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

/** Pulls the per-field messages out of a failed save. */
function fieldErrorsOf(result) {
  const raw = result?.bodyJson?.fieldErrors;
  if (!raw) return {};
  return Object.fromEntries(
    Object.entries(raw).map(([key, messages]) => [
      key,
      (Array.isArray(messages) ? messages : [messages]).join(', '),
    ]),
  );
}

export default function SchoolSettings({ school, onChanged }) {
  const { call } = useApi();

  const [profile, setProfile] = useState({});
  const [address, setAddress] = useState({});
  const [local, setLocal] = useState({});
  const [logo, setLogo] = useState('');

  const [busy, setBusy] = useState(null);
  const [errors, setErrors] = useState({});
  const [zoneWarning, setZoneWarning] = useState(false);

  const [loading, setLoading] = useState(true);

  /** Fills every form from one record, so the boxes show what is actually stored. */
  const fill = (record) => {
    setProfile({
      schoolName: record.schoolName ?? '',
      accountHolderName: record.accountHolderName ?? '',
      phoneNumber: record.phoneNumber ?? '',
      emailAddress: record.emailAddress ?? '',
    });
    setAddress({
      addressLine: record.addressLine ?? '',
      city: record.city ?? '',
      stateOrProvince: record.stateOrProvince ?? '',
      postalCode: record.postalCode ?? '',
    });
    setLocal({
      defaultLocale: record.defaultLocale ?? 'en-IN',
      defaultTimeZone: record.defaultTimeZone ?? 'Asia/Kolkata',
    });
    setLogo(record.logoUrl ?? '');
  };

  /*
   * Read the profile from the school surface rather than reusing the row that opened this
   * screen. This is the read the four forms below write back to, so what is on screen is what
   * those endpoints will be changing.
   */
  const editable = ['ACTIVE', 'TRIAL', 'PROVISIONING'].includes(school.status);

  const loadProfile = useCallback(async () => {
    // A school past its usable life cannot be edited, so there is nothing to fetch.
    if (!editable) {
      setLoading(false);
      return;
    }
    setLoading(true);
    const result = await call('get-profile', { label: 'Load the profile', subdomain: school.subdomain });
    setLoading(false);
    if (result.ok) {
      fill(result.bodyJson);
      onChanged({ ...school, ...result.bodyJson });
    } else {
      // Fall back to what the list gave us, so the forms are still usable.
      fill(school);
    }
  }, [call, school.subdomain, editable]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const save = async (key, label, endpointId, body) => {
    setBusy(key);
    setErrors((was) => ({ ...was, [key]: {} }));
    const result = await call(endpointId, {
      label,
      subdomain: school.subdomain,
      body,
    });
    setBusy(null);
    if (result.ok) {
      // Every one of these answers with the whole profile, so refill from it: a value the
      // backend tidied up on the way in then shows as it was actually stored.
      fill(result.bodyJson);
      onChanged({ ...school, ...result.bodyJson });
      return result;
    }
    setErrors((was) => ({ ...was, [key]: fieldErrorsOf(result) }));
    return result;
  };

  const saveLocalization = async (confirmed) => {
    const changingZone = local.defaultTimeZone !== school.defaultTimeZone;
    const result = await save('local', 'Save language and time', 'update-localization', {
      ...local,
      ...(confirmed ? { confirmTimeZoneChange: true } : {}),
    });
    // The backend refuses a time-zone change until it is confirmed, on purpose.
    if (!result.ok && result.bodyJson?.code === 'TIME_ZONE_CHANGE_NOT_CONFIRMED' && changingZone) {
      setZoneWarning(true);
    } else {
      setZoneWarning(false);
    }
  };

  if (!editable) {
    return (
      <Card>
        <p className="flex items-start gap-2 text-sm text-slate-600">
          <AlertTriangle size={16} className="mt-0.5 shrink-0 text-amber-500" />
          This school is <strong className="mx-1">{school.status.toLowerCase()}</strong> and its
          settings cannot be changed.
        </p>
      </Card>
    );
  }

  if (loading) return <Loading label="Loading the school's settings…" />;

  return (
    <div className="grid gap-5 lg:grid-cols-2">
      <Card
        title="School details"
        description="The name and contact details the school shows."
        action={
          <div className="flex items-center gap-2">
            <EndpointTag id="update-profile" />
            <Button
              look="primary"
              size="sm"
              busy={busy === 'profile'}
              onClick={() =>
                save('profile', 'Save school details', 'update-profile',
                  Object.fromEntries(Object.entries(profile).filter(([, v]) => v !== '')))
              }
            >
              Save
            </Button>
          </div>
        }
      >
        <div className="grid gap-4">
          <Field label="School name" error={errors.profile?.schoolName}>
            <TextInput
              value={profile.schoolName}
              onChange={(e) => setProfile({ ...profile, schoolName: e.target.value })}
              error={errors.profile?.schoolName}
            />
          </Field>
          <Field label="Account holder" error={errors.profile?.accountHolderName}>
            <TextInput
              value={profile.accountHolderName}
              onChange={(e) => setProfile({ ...profile, accountHolderName: e.target.value })}
              error={errors.profile?.accountHolderName}
            />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Email address" error={errors.profile?.emailAddress}>
              <TextInput
                type="email"
                value={profile.emailAddress}
                onChange={(e) => setProfile({ ...profile, emailAddress: e.target.value })}
                error={errors.profile?.emailAddress}
              />
            </Field>
            <Field label="Phone number" error={errors.profile?.phoneNumber}>
              <TextInput
                value={profile.phoneNumber}
                onChange={(e) => setProfile({ ...profile, phoneNumber: e.target.value })}
              />
            </Field>
          </div>
        </div>
      </Card>

      <Card
        title="Address"
        description="Saved together, because half an address is not an address."
        action={
          <div className="flex items-center gap-2">
            <EndpointTag id="replace-address" />
            <Button
              look="primary"
              size="sm"
              busy={busy === 'address'}
              onClick={() => save('address', 'Save the address', 'replace-address', address)}
            >
              Save
            </Button>
          </div>
        }
      >
        <div className="grid gap-4">
          <Field label="Street address" error={errors.address?.addressLine}>
            <TextInput
              value={address.addressLine}
              onChange={(e) => setAddress({ ...address, addressLine: e.target.value })}
            />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="City" error={errors.address?.city}>
              <TextInput value={address.city} onChange={(e) => setAddress({ ...address, city: e.target.value })} />
            </Field>
            <Field label="State or province" error={errors.address?.stateOrProvince}>
              <TextInput
                value={address.stateOrProvince}
                onChange={(e) => setAddress({ ...address, stateOrProvince: e.target.value })}
              />
            </Field>
          </div>
          <Field label="Postal code" error={errors.address?.postalCode}>
            <TextInput
              value={address.postalCode}
              onChange={(e) => setAddress({ ...address, postalCode: e.target.value })}
            />
          </Field>
          <p className="text-xs text-slate-500">
            The country is set when the school is created and cannot be changed here — it decides
            which tax rules and identity documents apply.
          </p>
        </div>
      </Card>

      <Card
        title="Language and time"
        description="The time zone decides which day attendance and timetables fall on."
        action={
          <div className="flex items-center gap-2">
            <EndpointTag id="update-localization" />
            <Button look="primary" size="sm" busy={busy === 'local'} onClick={() => saveLocalization(false)}>
              Save
            </Button>
          </div>
        }
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Language" error={errors.local?.defaultLocale}>
            <SelectInput
              value={local.defaultLocale}
              onChange={(e) => setLocal({ ...local, defaultLocale: e.target.value })}
            >
              {LOCALES.map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </SelectInput>
          </Field>
          <Field label="Time zone" error={errors.local?.defaultTimeZone}>
            <SelectInput
              value={local.defaultTimeZone}
              onChange={(e) => setLocal({ ...local, defaultTimeZone: e.target.value })}
            >
              {TIME_ZONES.map((zone) => (
                <option key={zone} value={zone}>
                  {zone}
                </option>
              ))}
            </SelectInput>
          </Field>
        </div>
      </Card>

      <Card
        title="Logo"
        description="Must be an https address on an allowed host."
        action={
          <div className="flex items-center gap-2">
            <EndpointTag id="replace-logo" />
            <Button
              look="primary"
              size="sm"
              busy={busy === 'logo'}
              onClick={() => save('logo', 'Save the logo', 'replace-logo', { logoUrl: logo })}
            >
              Save
            </Button>
          </div>
        }
      >
        <div className="grid gap-4">
          <div className="flex items-center gap-4">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
              {logo ? (
                <img src={logo} alt="" className="h-full w-full object-contain" />
              ) : (
                <ImageIcon size={20} className="text-slate-300" />
              )}
            </div>
            <Field label="Logo address" error={errors.logo?.logoUrl} className="flex-1">
              <TextInput
                value={logo}
                onChange={(e) => setLogo(e.target.value)}
                placeholder="https://cdn.example.com/logos/school.png"
                error={errors.logo?.logoUrl}
              />
            </Field>
          </div>
          <p className="text-xs text-slate-500">
            Leave it empty to remove the logo.
          </p>
        </div>
      </Card>

      <Modal
        open={zoneWarning}
        onClose={() => setZoneWarning(false)}
        title="Are you sure about the time zone?"
        description="This is not an ordinary setting."
        footer={
          <>
            <Button onClick={() => setZoneWarning(false)}>Cancel</Button>
            <Button
              look="solidDanger"
              busy={busy === 'local'}
              onClick={async () => {
                await saveLocalization(true);
                setZoneWarning(false);
              }}
            >
              Yes, change the time zone
            </Button>
          </>
        }
      >
        <div className="space-y-3 text-sm text-slate-600">
          <p>
            Changing from <strong className="font-mono text-xs">{school.defaultTimeZone}</strong> to{' '}
            <strong className="font-mono text-xs">{local.defaultTimeZone}</strong> does not move any
            record, but it changes which <em>day</em> the records you already have belong to.
          </p>
          <p>
            Attendance, timetable periods, holidays and transport trips are all decided by the
            school's local day. After this, some of them will read as a different day.
          </p>
        </div>
      </Modal>
    </div>
  );
}
