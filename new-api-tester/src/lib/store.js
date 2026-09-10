/**
 * The few settings that should survive a page refresh, kept in this browser.
 *
 * Nothing here leaves the machine, and every read copes with the value being missing or
 * damaged — a broken entry should never stop the app from loading.
 */

import { DEFAULT_ENVIRONMENTS, DEFAULT_TIMEOUT_MS } from '../config/environments.js';

const PREFIX = 'orbit.tester.';
const KEYS = {
  activeEnvironment: `${PREFIX}activeEnvironment`,
  actingSubdomain: `${PREFIX}actingSubdomain`,
  actingAcademicYear: `${PREFIX}actingAcademicYear`,
  timeout: `${PREFIX}timeout`,
};

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
  /**
   * The list is not saved. There is no screen for editing it, so saving it only ever meant an
   * old copy could outlive a change made here — which is how a stale placeholder address ended
   * up being used after it had already been removed from this file.
   */
  loadEnvironments() {
    return DEFAULT_ENVIRONMENTS;
  },

  /**
   * Which one is in use IS saved. Anything that no longer exists, or that has no address set,
   * falls back to the first — better to load against the local backend than to sit there
   * failing to look up a host nobody configured.
   */
  loadActiveEnvironmentId() {
    const saved = read(KEYS.activeEnvironment, null);
    const found = DEFAULT_ENVIRONMENTS.find((one) => one.id === saved);
    if (!found || (found.placeholder && !found.baseUrl)) return DEFAULT_ENVIRONMENTS[0].id;
    return found.id;
  },
  saveActiveEnvironmentId(id) {
    write(KEYS.activeEnvironment, id);
  },

  /**
   * Which school the app is acting as on the school surface.
   *
   * Saved because it is a mode, not a per-page choice: everything under `School` is asking "what
   * does THIS school see", and having to re-pick it on every screen would be the kind of friction
   * that ends in somebody testing the wrong tenant without noticing.
   */
  loadActingSubdomain() {
    const saved = read(KEYS.actingSubdomain, null);
    return typeof saved === 'string' && saved.trim() ? saved.trim() : null;
  },
  saveActingSubdomain(subdomain) {
    write(KEYS.actingSubdomain, subdomain || null);
  },

  /**
   * Which academic year the school surface is working in.
   *
   * Saved for the same reason the school is: everything under `School` that acts on a year needs
   * one, and re-picking it per screen is how somebody ends up writing to last year.
   *
   * STORED SEPARATELY FROM THE SCHOOL, AND CLEARED WHEN THE SCHOOL CHANGES. A year is identified
   * by its NAME, which is only unique within one school — "2026-2027" means a different document
   * for every tenant. Keeping a name across a school switch would silently point every call at a
   * year the new school may not even have, and the 404 would look like a bug in the endpoint.
   */
  loadActingAcademicYear() {
    const saved = read(KEYS.actingAcademicYear, null);
    return typeof saved === 'string' && saved.trim() ? saved.trim() : null;
  },
  saveActingAcademicYear(name) {
    write(KEYS.actingAcademicYear, name || null);
  },

  loadTimeout() {
    const value = read(KEYS.timeout, DEFAULT_TIMEOUT_MS);
    return Number.isFinite(value) && value > 0 ? value : DEFAULT_TIMEOUT_MS;
  },
  saveTimeout(ms) {
    write(KEYS.timeout, ms);
  },
};
