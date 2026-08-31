/**
 * Small helpers for showing JSON, sizes and times. No packages needed for any of it.
 */

/** Tidies JSON up so it is readable. Gives the text back unchanged if it is not JSON. */
export function prettyPrint(text) {
  if (typeof text !== 'string' || text.trim() === '') return text;
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
}

/** Squeezes JSON onto one line. Gives the text back unchanged if it is not JSON. */
export function minifyJson(text) {
  try {
    return JSON.stringify(JSON.parse(text));
  } catch {
    return text;
  }
}

/**
 * Checks whether some text is valid JSON, and says what went wrong if not.
 * Empty text counts as fine, because several endpoints take no body.
 */
export function checkJson(text) {
  if (typeof text !== 'string' || text.trim() === '') return { valid: true, error: null };
  try {
    JSON.parse(text);
    return { valid: true, error: null };
  } catch (error) {
    return { valid: false, error: error.message };
  }
}

/** Turns a byte count into something a person can read. */
export function formatBytes(bytes) {
  if (bytes == null) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(2)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
}

/** Turns milliseconds into something a person can read. */
export function formatDuration(ms) {
  if (ms == null) return '—';
  if (ms < 1000) return `${Math.round(ms)} ms`;
  return `${(ms / 1000).toFixed(2)} s`;
}

/** A clock time with milliseconds, so two calls a moment apart can be told apart. */
export function formatClock(isoString) {
  if (!isoString) return '—';
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return '—';
  const pad = (n, width = 2) => String(n).padStart(width, '0');
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${pad(date.getMilliseconds(), 3)}`;
}

/** A short date and time, for the history list. */
export function formatDateTime(isoString) {
  if (!isoString) return '—';
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

/** "3m ago", for the history list. */
export function timeAgo(isoString) {
  const then = new Date(isoString).getTime();
  if (Number.isNaN(then)) return '';
  const seconds = Math.max(0, Math.round((Date.now() - then) / 1000));
  if (seconds < 45) return 'just now';
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.round(hours / 24)}d ago`;
}

/** The words that go with a status number, for when the browser does not give us one. */
const STATUS_TEXT = {
  200: 'OK',
  201: 'Created',
  202: 'Accepted',
  204: 'No Content',
  301: 'Moved Permanently',
  302: 'Found',
  304: 'Not Modified',
  400: 'Bad Request',
  401: 'Unauthorized',
  403: 'Forbidden',
  404: 'Not Found',
  405: 'Method Not Allowed',
  406: 'Not Acceptable',
  409: 'Conflict',
  415: 'Unsupported Media Type',
  422: 'Unprocessable Entity',
  429: 'Too Many Requests',
  500: 'Internal Server Error',
  502: 'Bad Gateway',
  503: 'Service Unavailable',
  504: 'Gateway Timeout',
};

export function statusText(status, fallback) {
  return fallback || STATUS_TEXT[status] || '';
}

/** A plain sentence saying what a status code means for this API. */
export const STATUS_MEANING = {
  200: 'The call worked.',
  201: 'Something new was created. Look for the Location header.',
  204: 'It worked and there is nothing to send back.',
  400: 'The request itself is wrong — a missing field, a bad value, or broken JSON.',
  401: 'Not signed in. Add a token on the Auth tab.',
  403: 'Signed in, but not allowed to do this.',
  404: 'Nothing at this path, or no record with that id.',
  405: 'This path exists but not for this method. Check the method selector.',
  409: 'The request is fine and the answer is still no — the record is in the wrong state, or the value is taken.',
  415: 'The backend did not like the Content-Type. It wants application/json.',
  422: 'The shape was understood but the values do not make sense together.',
  429: 'Too many requests. Wait and try again.',
  500: 'The backend threw. Check the server log — the body will not have a stack trace.',
  502: 'Something in front of the backend could not reach it.',
  503: 'The backend is up but not taking requests.',
  504: 'Something in front of the backend waited too long.',
};

/** Green for 2xx, amber for 4xx, red for 5xx and for a call that never got an answer. */
export function statusTone(status) {
  if (status == null) return 'rose';
  if (status >= 200 && status < 300) return 'emerald';
  if (status >= 300 && status < 400) return 'sky';
  if (status >= 400 && status < 500) return 'amber';
  return 'rose';
}
