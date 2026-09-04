/**
 * Which backend the app talks to.
 *
 * Nothing in the app writes a base URL inline. Everything reads it from here, and the choice
 * is remembered in this browser.
 */

export const DEFAULT_ENVIRONMENTS = [
  {
    id: 'dev-proxy',
    name: 'Development (proxy)',
    // An empty base URL means the request goes to this page's own origin. The dev server
    // forwards /platform and /schools to http://localhost:3456, so the browser sees a
    // same-origin call. This is the one to use: no CORS, and every response header is
    // readable.
    baseUrl: '',
    description: 'Through this dev server on port 1400. No CORS, and every response header is readable.',
  },
  {
    id: 'dev-direct',
    name: 'Development (direct)',
    // Straight at Spring Boot. Needs DevCorsConfig on the backend, which is only switched on
    // for the dev profile.
    baseUrl: 'http://localhost:3456',
    description: 'Straight at Spring Boot on port 3456. Needs the dev CORS config.',
  },
  {
    id: 'staging',
    name: 'Staging',
    // Deliberately empty rather than a made-up domain. A placeholder address that looks real
    // fails as a name-lookup error deep in the browser, which reads like a bug in the app
    // instead of "nobody has set this up yet".
    baseUrl: '',
    placeholder: true,
    description: 'No address set yet. Put the staging server here when there is one.',
  },
  {
    id: 'production',
    name: 'Production',
    baseUrl: '',
    placeholder: true,
    description: 'No address set yet. These endpoints create and suspend real schools.',
  },
];

/** Give up on a request after this long, so a dead backend does not hang the page forever. */
export const DEFAULT_TIMEOUT_MS = 30000;
