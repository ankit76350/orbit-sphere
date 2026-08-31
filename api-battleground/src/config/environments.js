/**
 * Where the battleground sends requests.
 *
 * Nothing in the app writes a base URL inline. Everything reads it from here, or from the
 * copy the user has edited, which is kept in the browser's local storage.
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
    description: 'Through this dev server on port 1300. No CORS, all headers visible.',
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
    baseUrl: 'https://staging.orbitastra.example',
    description: 'Not set up yet. Edit the base URL when there is a staging server.',
  },
  {
    id: 'production',
    name: 'Production',
    baseUrl: 'https://api.orbitastra.example',
    description: 'Not set up yet. Careful — these endpoints create and suspend real schools.',
  },
];

/** How the tester should send credentials, when it sends any. */
export const AUTH_TYPES = [
  { id: 'none', name: 'No auth' },
  { id: 'bearer', name: 'Bearer token' },
  { id: 'basic', name: 'Basic auth' },
  { id: 'apiKey', name: 'API key header' },
];

export const DEFAULT_AUTH = {
  type: 'none',
  token: '',
  username: '',
  password: '',
  apiKeyName: 'X-API-Key',
  apiKeyValue: '',
};

/** Give up on a request after this long, so a dead backend does not hang the page forever. */
export const DEFAULT_TIMEOUT_MS = 30000;

/**
 * Values that get put into URLs, headers and bodies wherever {{name}} appears. Same idea as
 * Postman variables, and the same names the Postman collection already uses, so text can be
 * copied between the two.
 */
export const DEFAULT_VARIABLES = {
  schoolId: '',
  createdSubdomain: '',
  academicYear: '2026-2027',
};
