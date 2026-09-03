/**
 * The new-plan form. Endpoint #1.
 *
 * A plan always starts as a DRAFT at version 1 and not publicly available, so none of those
 * three are on the form — there is nothing to choose. What the form asks for is the commercial
 * shape: what it is called, what it costs, and what it lets a school have.
 *
 * NO CODE FIELD. The plan's permanent code is derived from the name — "Premium Plus" becomes
 * PREMIUM_PLUS — so asking for both would be the same words typed twice in two shapes. The form
 * shows what the code will be as you type, because it is permanent and worth seeing before it is
 * set.
 */

import { useEffect, useState } from 'react';
import { useApi } from '../api/apiContext.js';
import { Modal, Button, Field, TextInput, TextArea, SelectInput, Badge } from '../components/ui.jsx';
import EndpointTag from '../components/EndpointTag.jsx';

const CYCLES = [
  ['YEARLY', 'Yearly'],
  ['HALF_YEARLY', 'Every six months'],
  ['QUARTERLY', 'Quarterly'],
  ['MONTHLY', 'Monthly'],
  ['CUSTOM', 'Custom — the period is set per subscription'],
];

const CURRENCIES = ['INR', 'USD', 'GBP', 'AED', 'SGD'];

const EMPTY = {
  name: '',
  description: '',
  billingCycle: 'YEARLY',
  listPrice: '',
  currencyCode: 'INR',
  maxStudents: '',
  maxUsers: '',
};

/** The same shaping the backend does, so the form can show the code before it is created. */
function codeFrom(name) {
  return (name || '')
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 40);
}

export default function NewPlanModal({ open, onClose, onCreated }) {
  const { call } = useApi();
  const [form, setForm] = useState(EMPTY);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});
  const [refused, setRefused] = useState(null);

  useEffect(() => {
    if (open) {
      setForm(EMPTY);
      setErrors({});
      setRefused(null);
    }
  }, [open]);

  const set = (patch) => setForm((was) => ({ ...was, ...patch }));

  const save = async () => {
    setSaving(true);
    setErrors({});
    setRefused(null);

    // Numbers go as numbers. An empty string would arrive as a type error rather than as the
    // "this is required" the form should be showing.
    const body = {
      name: form.name.trim(),
      billingCycle: form.billingCycle,
      listPrice: form.listPrice === '' ? null : Number(form.listPrice),
      currencyCode: form.currencyCode,
      maxStudents: form.maxStudents === '' ? null : Number(form.maxStudents),
      maxUsers: form.maxUsers === '' ? null : Number(form.maxUsers),
    };
    if (form.description.trim()) body.description = form.description.trim();

    const result = await call('create-plan-draft', { label: 'Create a plan', body });
    setSaving(false);

    if (result.ok) {
      onCreated(result.bodyJson);
      return;
    }
    if (result.bodyJson?.fieldErrors) {
      setErrors(
        Object.fromEntries(
          Object.entries(result.bodyJson.fieldErrors).map(([field, messages]) => [
            field, [].concat(messages)[0],
          ]),
        ),
      );
    }
    // A refusal that is not about one field — a code already taken, a currency that does not
    // exist — has nowhere to sit next to an input, so it goes at the top of the form.
    if (result.bodyJson?.code && !result.bodyJson?.fieldErrors) {
      setRefused(result.bodyJson);
    }
  };

  const code = codeFrom(form.name);

  return (
    <Modal
      open={open}
      onClose={onClose}
      width="max-w-2xl"
      title="New plan"
      description="It starts as a draft, so nobody can buy it until you publish it."
      footer={
        <>
          <EndpointTag id="create-plan-draft" className="mr-auto" />
          <Button onClick={onClose}>Cancel</Button>
          <Button look="primary" onClick={save} busy={saving} disabled={!form.name.trim()}>
            Create the draft
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        {refused && (
          <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3">
            <p className="font-mono text-[11px] font-semibold text-red-900">{refused.code}</p>
            <p className="mt-1 text-xs text-red-800">{refused.message}</p>
          </div>
        )}

        <Field label="Name" required error={errors.name}
          hint={code ? undefined : 'The plan code is worked out from this.'}>
          <TextInput
            value={form.name}
            onChange={(event) => set({ name: event.target.value })}
            placeholder="Premium"
            error={errors.name}
          />
          {code && (
            <p className="mt-1.5 flex items-center gap-1.5 text-[11px] text-slate-500">
              Its permanent code will be <Badge look="blue">{code}</Badge>
              <span className="text-slate-400">— every version of the plan keeps it.</span>
            </p>
          )}
        </Field>

        <Field label="Description" hint="Optional. Shown to a school looking at the plan.">
          <TextArea
            rows={2}
            value={form.description}
            onChange={(event) => set({ description: event.target.value })}
            placeholder="Advanced modules for growing schools."
          />
        </Field>

        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Billing cycle" required error={errors.billingCycle}>
            <SelectInput value={form.billingCycle}
              onChange={(event) => set({ billingCycle: event.target.value })}>
              {CYCLES.map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </SelectInput>
          </Field>

          <Field label="Currency" required error={errors.currencyCode}>
            <SelectInput value={form.currencyCode}
              onChange={(event) => set({ currencyCode: event.target.value })}>
              {CURRENCIES.map((one) => <option key={one} value={one}>{one}</option>)}
            </SelectInput>
          </Field>

          <Field label="Price per cycle" required error={errors.listPrice}
            hint="Zero is allowed — a free tier is a real plan.">
            <TextInput
              type="number"
              min="0"
              step="0.01"
              value={form.listPrice}
              onChange={(event) => set({ listPrice: event.target.value })}
              placeholder="49999"
              error={errors.listPrice}
            />
          </Field>

          <Field label="Students included" required error={errors.maxStudents}>
            <TextInput
              type="number"
              min="1"
              value={form.maxStudents}
              onChange={(event) => set({ maxStudents: event.target.value })}
              placeholder="2000"
              error={errors.maxStudents}
            />
          </Field>

          <Field label="Staff logins included" required error={errors.maxUsers}>
            <TextInput
              type="number"
              min="1"
              value={form.maxUsers}
              onChange={(event) => set({ maxUsers: event.target.value })}
              placeholder="250"
              error={errors.maxUsers}
            />
          </Field>
        </div>

        <p className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 text-[11px] leading-relaxed text-slate-600">
          Features are set after this, on the plan itself. A plan with no features cannot be
          published — a school buying it would be paying for nothing.
        </p>
      </div>
    </Modal>
  );
}
