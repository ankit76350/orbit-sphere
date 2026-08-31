/**
 * Keeps the settings and the history in the browser, so a page refresh does not lose what you
 * were doing.
 *
 * Every read copes with the value being missing or damaged — a broken entry should never stop
 * the page from loading.
 */

import {
  DEFAULT_ENVIRONMENTS,
  DEFAULT_AUTH,
  DEFAULT_VARIABLES,
  DEFAULT_TIMEOUT_MS,
} from '../config/environments.js';

const PREFIX = 'orbit.battleground.';
const KEYS = {
  environments: `${PREFIX}environments`,
  activeEnvironment: `${PREFIX}activeEnvironment`,
  auth: `${PREFIX}auth`,
  variables: `${PREFIX}variables`,
  history: `${PREFIX}history`,
  timeout: `${PREFIX}timeout`,
};

/** Keeping the last 100 is enough to look back over a session without filling up storage. */
const HISTORY_LIMIT = 100;

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    if (raw == null) return fallback;
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

function write(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Storage can be full or switched off. Losing the saved copy is not worth an error on
    // screen, so carry on with what is in memory.
  }
}

export const store = {
  loadEnvironments() {
    const saved = read(KEYS.environments, null);
    if (!Array.isArray(saved) || saved.length === 0) return DEFAULT_ENVIRONMENTS;
    // Keep any new built-in environment added since the copy was saved.
    const savedIds = new Set(saved.map((one) => one.id));
    return [...saved, ...DEFAULT_ENVIRONMENTS.filter((one) => !savedIds.has(one.id))];
  },
  saveEnvironments(environments) {
    write(KEYS.environments, environments);
  },

  loadActiveEnvironmentId() {
    return read(KEYS.activeEnvironment, DEFAULT_ENVIRONMENTS[0].id);
  },
  saveActiveEnvironmentId(id) {
    write(KEYS.activeEnvironment, id);
  },

  loadAuth() {
    return { ...DEFAULT_AUTH, ...read(KEYS.auth, {}) };
  },
  saveAuth(auth) {
    write(KEYS.auth, auth);
  },

  loadVariables() {
    return { ...DEFAULT_VARIABLES, ...read(KEYS.variables, {}) };
  },
  saveVariables(variables) {
    write(KEYS.variables, variables);
  },

  loadTimeout() {
    const value = read(KEYS.timeout, DEFAULT_TIMEOUT_MS);
    return Number.isFinite(value) && value > 0 ? value : DEFAULT_TIMEOUT_MS;
  },
  saveTimeout(ms) {
    write(KEYS.timeout, ms);
  },

  loadHistory() {
    const saved = read(KEYS.history, []);
    return Array.isArray(saved) ? saved : [];
  },
  saveHistory(entries) {
    write(KEYS.history, entries.slice(0, HISTORY_LIMIT));
  },
  clearHistory() {
    write(KEYS.history, []);
  },
};

/**
 * Turns a finished call into the row the history list shows.
 *
 * The whole request and response are kept, not just a summary, because the point of the
 * history is being able to open an old call and look at what actually happened.
 */
export function toHistoryEntry({ endpoint, environment, result, draft }) {
  return {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    at: result.startedAtIso,
    endpointId: endpoint?.id ?? null,
    endpointName: endpoint?.name ?? 'Untitled request',
    module: endpoint?.module ?? null,
    method: result.request.method,
    url: result.request.url,
    path: result.request.path,
    environmentId: environment?.id ?? null,
    environmentName: environment?.name ?? null,
    status: result.status,
    statusText: result.statusText,
    ok: result.ok,
    durationMs: result.durationMs,
    sizeBytes: result.sizeBytes,
    errorKind: result.error?.kind ?? null,
    errorTitle: result.error?.title ?? null,
    // Kept whole so Inspect and Resend both work from the history alone.
    result,
    draft,
  };
}

export { HISTORY_LIMIT };
