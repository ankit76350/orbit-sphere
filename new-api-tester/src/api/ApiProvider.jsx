/**
 * The one place the app talks to the backend.
 *
 * Screens call `call('activate-school', { pathParams: { id } })` and get the result back.
 * Everything else happens here: the tenant header is added, the call is written into the
 * activity log, and anything that CHANGES something opens the details pop-up straight away —
 * what was sent, what came back, how long it took.
 *
 * Reads are silent. A GET runs when a page loads, and a pop-up on every page load would be
 * unusable; a list that fails says so in the page itself instead.
 *
 * The paths and methods are not repeated here — they come from config/endpoints.js, which is
 * generated from the Postman collection.
 *
 * The contexts and the hooks that read them are in apiContext.js — see the note there for why
 * there are two of them, and why this file must export nothing but its component.
 */

import { useCallback, useMemo, useRef, useState } from 'react';
import { sendRequest } from '../lib/httpClient.js';
import { store } from '../lib/store.js';
import { buildCall } from './buildCall.js';
import { ApiActionsContext, ApiStateContext } from './apiContext.js';

/**
 * The stand-in result for an environment with no address. Shaped exactly like a real one, so
 * the activity list and the details pop-up handle it without a special case.
 */
function notConfigured(prepared, environment) {
  const now = new Date().toISOString();
  return {
    ok: false,
    request: prepared,
    status: null,
    statusText: null,
    headers: {},
    bodyText: '',
    bodyJson: null,
    jsonParseError: null,
    sizeBytes: 0,
    durationMs: 0,
    startedAtIso: now,
    finishedAtIso: now,
    timeoutMs: 0,
    error: {
      kind: 'not-configured',
      title: `${environment.name} has no address`,
      message: `Nothing was sent. ${environment.name} has no server address set, so there is nowhere to send it.`,
      hint: 'Pick "Development (proxy)" in the header to work against the local backend.',
    },
  };
}

export default function ApiProvider({ children }) {
  const [environments] = useState(() => store.loadEnvironments());
  const [environmentId, setEnvironmentId] = useState(() => store.loadActiveEnvironmentId());
  const [log, setLog] = useState([]);
  const [inspecting, setInspecting] = useState(null);

  // Which school the app is acting as, for the school surface. The platform surface names its
  // school in the URL and ignores this entirely.
  const [actingSubdomain, setActingSubdomain] = useState(() => store.loadActingSubdomain());

  // Which academic year the school surface is working in. A year is named, and a name is only
  // unique within one school, so this is cleared whenever the school changes — see chooseSchool.
  const [actingAcademicYear, setActingAcademicYear] =
    useState(() => store.loadActingAcademicYear());

  // The chosen school's DOCUMENT id, beside its subdomain. The header takes a subdomain; the
  // local-user cookie stores an id, and the picker hands the whole school object over at the one
  // moment the id is known for free.
  const [actingSchoolId, setActingSchoolId] = useState(() => store.loadActingSchoolId());

  // Which staff member the app is acting as. Nothing sends it yet — it exists to go in the
  // cookie, which is what `POST /local-user` is for.
  const [actingStaffDocsId, setActingStaffDocsId] =
    useState(() => store.loadActingStaffDocsId());

  const environment = useMemo(
    () => environments.find((one) => one.id === environmentId) || environments[0],
    [environments, environmentId],
  );

  // Read through a ref inside call(), so switching environment does not give call() a new
  // identity and quietly reintroduce the reload loop.
  //
  // Assigning to a ref during render is what the linter objects to, and here it is the whole
  // point: call() has to keep ONE identity for the life of the provider. Putting `environment`
  // in its dependencies instead would give every screen that loads itself a new load(), which
  // re-runs the effect, which calls again — the loop this ref exists to prevent.
  const environmentRef = useRef(environment);
  // oxlint-disable-next-line react/refs
  environmentRef.current = environment;

  const chooseEnvironment = useCallback((id) => {
    setEnvironmentId(id);
    store.saveActiveEnvironmentId(id);
  }, []);

  // Same ref trick as the environment, and for the same reason: call() has to keep one identity.
  const actingRef = useRef(actingSubdomain);
  // oxlint-disable-next-line react/refs
  actingRef.current = actingSubdomain;

  // The second argument is the whole school, which SchoolPicker already passes. It is null when
  // the picker was told to use a typed subdomain that matches no loaded school — a real path,
  // and one where there simply is no document id to keep.
  const chooseSchool = useCallback((subdomain, school) => {
    const next = subdomain ? subdomain.trim() : null;
    setActingSubdomain(next);
    store.saveActingSubdomain(next);

    const nextId = next && school?.schoolId ? school.schoolId : null;
    setActingSchoolId(nextId);
    store.saveActingSchoolId(nextId);

    // THE YEAR GOES WITH IT. Academic years are identified by name, and a name is unique only
    // within a school: "2026-2027" is a different document for every tenant. Keeping the old
    // name would point every year-scoped call at a year the new school may not have, and the
    // 404 would read as a broken endpoint rather than a stale choice.
    setActingAcademicYear(null);
    store.saveActingAcademicYear(null);

    // AND SO DOES THE STAFF MEMBER, for a stronger reason than the year. A year is a NAME that
    // the new school may not have, and the miss shows up as a 404. A staff id is a DOCUMENT id
    // belonging to the old tenant: carried across, it would put another school's person in the
    // cookie and nothing would complain.
    setActingStaffDocsId(null);
    store.saveActingStaffDocsId(null);
  }, []);

  const chooseStaff = useCallback((staffDocsId) => {
    const next = staffDocsId ? staffDocsId.trim() : null;
    setActingStaffDocsId(next);
    store.saveActingStaffDocsId(next);
  }, []);

  const chooseAcademicYear = useCallback((name) => {
    const next = name ? name.trim() : null;
    setActingAcademicYear(next);
    store.saveActingAcademicYear(next);
  }, []);

  const call = useCallback(
    async (endpointId, options = {}) => {
      const environment = environmentRef.current;

      // The school surface reads the tenant from a header and never from the URL, so every call
      // to it needs one. Filled in here rather than at each call site: a screen that forgot
      // would get a 400 TENANT_NOT_RESOLVED that looks like a bug in the screen, and one that
      // passed the wrong school would be worse — it would answer, about the wrong tenant.
      const withTenant = options.subdomain
        ? options
        : { ...options, subdomain: actingRef.current || undefined };

      const { endpoint, prepared } = buildCall(endpointId, withTenant, environment);

      // An environment nobody has set up yet has no address. Say that, rather than letting the
      // browser fail to look up a host and report it as a network error.
      const result = environment?.placeholder && !environment.baseUrl
        ? notConfigured(prepared, environment)
        : await sendRequest(prepared, { timeoutMs: store.loadTimeout() });

      const entry = {
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
        at: result.startedAtIso,
        endpointId,
        endpointName: endpoint.name,
        action: options.label || endpoint.name,
        module: endpoint.module,
        method: result.request.method,
        path: result.request.path,
        url: result.request.url,
        status: result.status,
        ok: result.ok,
        durationMs: result.durationMs,
        sizeBytes: result.sizeBytes,
        environmentName: environment?.name,
        result,
        endpoint,
      };
      setLog((was) => [entry, ...was].slice(0, 100));

      // Anything that changes something shows its answer straight away. A read does not: it
      // runs on page load, and the page shows its own message if it fails.
      if (endpoint.method !== 'GET') setInspecting(entry);

      return result;
    },
    [],
  );

  const clearLog = useCallback(() => setLog([]), []);

  // THE COOKIE IS WRITTEN ON A BUTTON PRESS, NOT ON EVERY PICK — changed 2026-09-18.
  //
  // It used to follow the three pickers through an effect, which meant choosing a school, then a
  // year, then a person put THREE calls in the log for one decision, two of them describing a
  // context nobody meant to store. Signing in is a thing somebody does once they have finished
  // choosing, so it is a thing they press.
  //
  // The call itself lives in `SignIn`, beside the pickers it reads. Nothing here fires it.
  const signIn = useCallback(
    () => call('store-local-user', {
      label: 'Sign in — remember who this browser is acting as',
      body: {
        schoolId: actingSchoolId ?? '',
        academicYear: actingAcademicYear ?? '',
        staffDocsId: actingStaffDocsId ?? '',
      },
    }),
    [call, actingSchoolId, actingAcademicYear, actingStaffDocsId],
  );

  const actions = useMemo(
    () => ({ call, inspect: setInspecting, clearLog, chooseEnvironment, chooseSchool,
      chooseAcademicYear, chooseStaff, signIn }),
    [call, clearLog, chooseEnvironment, chooseSchool, chooseAcademicYear, chooseStaff, signIn],
  );

  const state = useMemo(
    () => ({ log, inspecting, environment, environments, actingSubdomain, actingAcademicYear,
      actingSchoolId, actingStaffDocsId }),
    [log, inspecting, environment, environments, actingSubdomain, actingAcademicYear,
      actingSchoolId, actingStaffDocsId],
  );

  return (
    <ApiActionsContext.Provider value={actions}>
      <ApiStateContext.Provider value={state}>{children}</ApiStateContext.Provider>
    </ApiActionsContext.Provider>
  );
}
