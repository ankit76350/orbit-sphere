/**
 * Replaces {{name}} placeholders in URLs, headers and bodies, the same way Postman does, so
 * the collection in postman/ and this tester can use the same text.
 *
 * Two kinds:
 *   {{schoolId}}     — a saved value, filled in after a call that captures it
 *   {{$timestamp}}   — a value made fresh every time the request is sent
 */

/** Values made up on the spot. These are what keep a unique subdomain unique per send. */
function builtIns() {
  const now = new Date();
  return {
    $timestamp: String(Math.floor(now.getTime() / 1000)),
    $isoTimestamp: now.toISOString(),
    $randomInt: String(Math.floor(Math.random() * 1000)),
    $guid: typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : String(now.getTime()),
  };
}

/**
 * Fills in every {{name}} in some text. Also returns what was put in, so the request details
 * panel can show which placeholder turned into which value.
 */
export function applyVariables(text, variables) {
  if (typeof text !== 'string' || !text.includes('{{')) {
    return { text, used: [] };
  }
  const generated = builtIns();
  const used = [];
  const filled = text.replace(/\{\{([^}\s]+)\}\}/g, (whole, name) => {
    if (name in generated) {
      used.push({ name, value: generated[name], kind: 'generated' });
      return generated[name];
    }
    if (variables && name in variables && variables[name] !== '') {
      used.push({ name, value: variables[name], kind: 'saved' });
      return variables[name];
    }
    // Leave an unknown placeholder alone rather than blanking it. A URL with a visible
    // {{schoolId}} in it tells you what went wrong; an empty gap does not.
    used.push({ name, value: null, kind: 'missing' });
    return whole;
  });
  return { text: filled, used };
}

/** Any {{name}} in the text that we have no value for. */
export function missingVariables(text, variables) {
  const { used } = applyVariables(text, variables);
  return [...new Set(used.filter((one) => one.kind === 'missing').map((one) => one.name))];
}

/** Reads a value out of a response body by a dotted path, for the capture rules. */
export function readPath(value, path) {
  if (value == null) return undefined;
  return path.split('.').reduce((current, key) => (current == null ? undefined : current[key]), value);
}
