"""
Turns postman/"Orbit Sphere — API.postman_collection.json" into src/config/endpoints.js.

    cd api-battleground && python3 tools/generate-endpoints.py

Run it whenever the collection changes. Everything mechanical — the runnable bodies, the query
parameters, the headers, the markdown notes and all the numbered test cases — is copied from
the collection, so the battleground and Postman never drift apart.

What the collection cannot say is in CURATED below: which fields are required, what comes back,
which variable to remember after a call. Those were read out of the controllers, the DTOs and
the services. When a new endpoint is added to the collection it will appear here on the next
run with empty curated fields; fill its entry in.
"""
import json, re, os, sys

ROOT = '/Users/rohanshinde/FlutterDev/projects/school-edu-sphere'
COLLECTION = os.path.join(ROOT, 'postman', 'Orbit Sphere — API.postman_collection.json')
OUT = os.path.join(ROOT, 'api-battleground', 'src', 'config', 'endpoints.js')

# ---------------------------------------------------------------------------
# What the collection cannot tell us: read out of the DTOs, the services and
# the controllers. Keyed by the Postman request name.
# ---------------------------------------------------------------------------
CURATED = {
  'Create School': dict(
    summary='Makes the school row at PROVISIONING or TRIAL. That is all it does.',
    required=['schoolName','accountHolderName','subdomain','defaultLocale','defaultTimeZone','countryCode'],
    optional=['phoneNumber','emailAddress','addressLine','city','stateOrProvince','postalCode','trial'],
    responseFields=['schoolId','schoolName','subdomain','status','createdAt','nextStep'],
    successStatus=201,
    successNote='Also sends a Location header: /platform/schools/{schoolId}',
    captures=[('schoolId','schoolId'),('createdSubdomain','subdomain')],
  ),
  'Complete Provisioning': dict(
    summary='Finishes the setup: seeds 47 number sequences and the starting roles. Safe to run twice.',
    responseFields=['schoolId','subdomain','status','numberSequencesCreated','numberSequencesAlreadyPresent','rolesCreated','rolesAlreadyPresent','roleKeys','readyToActivate','nextStep'],
    successStatus=200,
    captures=[('schoolId','schoolId'),('createdSubdomain','subdomain')],
  ),
  'Activate School': dict(
    summary='Takes the school live. PROVISIONING or TRIAL to ACTIVE. Refuses a second call.',
    responseFields=['schoolId','subdomain','status','activatedAt','firstActivation','subscriptionStatus','subscriptionNote','nextStep'],
    successStatus=200,
    captures=[('schoolId','schoolId'),('createdSubdomain','subdomain')],
  ),
  'Suspend School': dict(
    summary='Blocks a live school. ACTIVE to SUSPENDED. A reason is required.',
    required=['reason'],
    responseFields=['schoolId','subdomain','status','activatedAt','suspendedAt','statusReason','nextStep'],
    successStatus=200,
    captures=[('schoolId','schoolId')],
  ),
  'Reactivate School': dict(
    summary='Lets a suspended school back in. SUSPENDED to ACTIVE. The body is optional.',
    optional=['note'],
    responseFields=['schoolId','subdomain','status','activatedAt','suspendedAt','statusReason','nextStep'],
    successStatus=200,
    captures=[('schoolId','schoolId')],
  ),
  'Change Subdomain': dict(
    summary='Changes the key that finds the tenant. Breaks every saved link, so it asks for the old value back.',
    required=['currentSubdomain','newSubdomain'],
    responseFields=['schoolId','schoolName','previousSubdomain','subdomain','nextStep'],
    successStatus=200,
    captures=[('createdSubdomain','subdomain')],
  ),
  'List Schools': dict(
    summary="The operator's school list: filtered, searched, sorted and paged. Every parameter is optional.",
    responseFields=['content','page','size','totalElements','totalPages','hasNext','hasPrevious'],
    successStatus=200,
  ),
  'Get School': dict(
    summary='One school in full, including its lifecycle timestamps and the reason for its current status.',
    responseFields=['schoolId','schoolName','accountHolderName','subdomain','logoUrl','phoneNumber',
                    'emailAddress','defaultLocale','defaultTimeZone','addressLine','city',
                    'stateOrProvince','postalCode','countryCode','status','statusReason',
                    'activatedAt','suspendedAt','createdAt','updatedAt'],
    successStatus=200,
    captures=[('schoolId','schoolId'),('createdSubdomain','subdomain')],
  ),
  'Get Profile': dict(
    summary="The school's own profile — the read behind the four settings forms.",
    responseFields=['schoolId','subdomain','status','schoolName','accountHolderName','phoneNumber',
                    'emailAddress','logoUrl','defaultLocale','defaultTimeZone','addressLine','city',
                    'stateOrProvince','postalCode','countryCode'],
    successStatus=200,
  ),
  'List Academic Years': dict(
    summary='Every academic year the school has, newest first.',
    responseFields=['academicYearId','name','startDate','endDate','durationDays','current',
                    'holidayCount','enrollmentEnabled','resultsLocked'],
    successStatus=200,
  ),
  'Get Current Academic Year': dict(
    summary='The year that contains today. A 404 when no year covers it — which is a real answer, not a fault.',
    responseFields=['academicYearId','name','startDate','endDate','durationDays','current','holidayCount'],
    successStatus=200,
    captures=[('academicYearName','name')],
  ),
  'Get Academic Year': dict(
    summary='One year by name.',
    responseFields=['academicYearId','name','startDate','endDate','durationDays','current',
                    'holidayCount','enrollmentEnabled','resultsLocked'],
    successStatus=200,
    captures=[('academicYearName','name')],
  ),
  'Get Holiday Calendar': dict(
    summary="The year's whole calendar: every closed day, and why each one is closed.",
    responseFields=['academicYearName','startDate','endDate','closedDayCount','eventCount',
                    'countsByType','holidays'],
    successStatus=200,
  ),
  'Get Day Status': dict(
    summary='Is the school closed on this day, and why.',
    responseFields=['academicYearName','date','dayOfWeek','closed','events'],
    successStatus=200,
  ),
  'Count Working Days': dict(
    summary='Which days in a range are working days, and how many.',
    responseFields=['academicYearName','from','to','totalDayCount','workingDayCount',
                    'closedDayCount','workingDays'],
    successStatus=200,
  ),
  'Update Profile': dict(
    summary="The school's own name, account holder and contact details.",
    optional=['schoolName','accountHolderName','phoneNumber','emailAddress'],
    responseFields=['schoolId','subdomain','status','schoolName','accountHolderName','phoneNumber','emailAddress','logoUrl','defaultLocale','defaultTimeZone','addressLine','city','stateOrProvince','postalCode','countryCode'],
    successStatus=200,
  ),
  'Replace Address': dict(
    summary='Replaces the whole address. A PUT, because patching city without state gives a place that does not exist.',
    optional=['addressLine','city','stateOrProvince','postalCode'],
    responseFields=['schoolId','subdomain','addressLine','city','stateOrProvince','postalCode','countryCode'],
    successStatus=200,
  ),
  'Update Localization': dict(
    summary='Language and time zone. Changing the zone reinterprets every school-local date already recorded.',
    optional=['defaultLocale','defaultTimeZone','confirmTimeZoneChange'],
    responseFields=['schoolId','subdomain','defaultLocale','defaultTimeZone'],
    successStatus=200,
  ),
  'Replace Logo': dict(
    summary='Replaces the logo. The URL must be https and on an allowed host.',
    optional=['logoUrl'],
    responseFields=['schoolId','subdomain','logoUrl'],
    successStatus=200,
  ),
  'Create Academic Year': dict(
    summary='Makes a year with an empty calendar. The name can never be changed afterwards.',
    required=['name','startDate','endDate'],
    responseFields=['academicYearId','name','startDate','endDate','durationDays','current','holidayCount','enrollmentEnabled','resultsLocked','nextStep'],
    successStatus=201,
    successNote='Also sends a Location header: /schools/current/academic-years/{name}',
    captures=[('academicYearName','name')],
  ),
  'Update Academic Year Dates': dict(
    summary='Moves a boundary. Shrinking past an existing holiday is refused.',
    optional=['startDate','endDate'],
    responseFields=['academicYearId','name','startDate','endDate','durationDays','current','holidayCount','nextStep'],
    successStatus=200,
    captures=[('academicYearName','name')],
  ),
  'Replace Holiday Calendar': dict(
    summary='Replaces the whole calendar in one go. The bulk import case.',
    required=['name','type','date'],
    optional=['description'],
    responseFields=['academicYearName','startDate','endDate','closedDayCount','eventCount','countsByType','holidays','changeSummary'],
    successStatus=200,
  ),
  'Add Holiday': dict(
    summary='Adds one reason to one day. A day that is already closed is not a conflict.',
    required=['name','type','date'],
    optional=['description'],
    responseFields=['academicYearName','closedDayCount','eventCount','countsByType','holidays','changeSummary'],
    successStatus=200,
  ),
  'Update Holiday': dict(
    summary='Edits one reason on a day. ?type= says which one when the day holds several.',
    optional=['name','description','newType'],
    responseFields=['academicYearName','closedDayCount','eventCount','countsByType','holidays','changeSummary'],
    successStatus=200,
  ),
  'Remove Holiday': dict(
    summary='Removes one reason, or the whole day when ?type= is left off.',
    responseFields=['academicYearName','closedDayCount','eventCount','countsByType','holidays','changeSummary'],
    successStatus=200,
  ),
  'Generate Weekly Off': dict(
    summary='Makes one dated entry per occurrence of a weekday. Needed because there is no weekly-off field anywhere.',
    required=['dayOfWeek'],
    optional=['fromDate','toDate','name'],
    responseFields=['academicYearName','dayOfWeek','fromDate','toDate','generated','skippedAlreadyWeeklyOff','skippedDates','closedDayCountAfter','eventCountAfter','changeSummary'],
    successStatus=200,
  ),
  'Remove Holidays By Type': dict(
    summary='Clears every entry of one type across the year. type is required here.',
    responseFields=['academicYearName','closedDayCount','eventCount','countsByType','holidays','changeSummary'],
    successStatus=200,
  ),
  'Enable Enrollment': dict(summary='Opens enrollment for the year. A gate, not a field edit.',
    responseFields=['academicYearId','name','enrollmentEnabled','nextStep'], successStatus=200),
  'Disable Enrollment': dict(summary='Closes enrollment for the year.',
    responseFields=['academicYearId','name','enrollmentEnabled','nextStep'], successStatus=200),
  'Lock Results': dict(summary='Locks results for the year. Routine.',
    responseFields=['academicYearId','name','resultsLocked','nextStep'], successStatus=200),
  'Unlock Results': dict(summary='Unlocking lets somebody change a mark a parent has already seen.',
    responseFields=['academicYearId','name','resultsLocked','nextStep'], successStatus=200),

  # ----------------------------------------------------------------- plans
  'Create Plan Draft': dict(
    summary='Makes a plan as a DRAFT at version 1, not publicly available. Nobody can buy it yet.',
    required=['name','billingCycle','listPrice','currencyCode','maxStudents','maxUsers'],
    optional=['planCode','description','effectiveFrom','effectiveUntil'],
    responseFields=['planId','planCode','planVersion','name','status','billingCycle','listPrice','currencyCode','maxStudents','maxUsers','publiclyAvailable','featureCount','sellable','nextStep'],
    successStatus=201,
    successNote='Also sends a Location header: /platform/plans/{planCode}/versions/1',
    captures=[('planCode','planCode'),('planVersion','planVersion')],
  ),
  'Update Plan Draft': dict(
    summary='Edits a draft — name, price, limits, selling window. Refused once the plan is published.',
    optional=['name','description','billingCycle','listPrice','currencyCode','maxStudents','maxUsers','sellingWindow'],
    responseFields=['planCode','planVersion','name','status','listPrice','currencyCode','maxStudents','maxUsers','featureCount','sellable','nextStep'],
    successStatus=200,
  ),
  'Set Plan Features': dict(
    summary='Replaces the whole feature list of a draft. featureCode is one of 24 fixed values.',
    required=['features'],
    responseFields=['planCode','planVersion','status','featureCount','features','changeSummary'],
    successStatus=200,
  ),
  'Publish Plan': dict(
    summary='Turns a draft into a plan schools can buy. One-way: it can never be edited again.',
    responseFields=['planCode','planVersion','status','effectiveFrom','publiclyAvailable','sellable','nextStep'],
    successStatus=200,
  ),
  'Set Plan Availability': dict(
    summary='Public list, or private quote only. The last of the three things that make a plan sellable.',
    required=['publiclyAvailable'],
    responseFields=['planCode','planVersion','status','publiclyAvailable','sellable','nextStep'],
    successStatus=200,
  ),
  'Retire Plan': dict(
    summary='Stops a plan being sold. Schools already on it keep it — their subscription does not change.',
    responseFields=['planCode','planVersion','status','effectiveUntil','sellable','nextStep'],
    successStatus=200,
  ),
  'List Plans': dict(
    summary='The catalogue, filtered and paged. One row per plan VERSION, newest version of each first.',
    responseFields=['content','page','size','totalElements','totalPages','hasNext','hasPrevious'],
    successStatus=200,
  ),
  'List Plan Versions': dict(
    summary='Every version of one plan, newest first, with the price change and who is on each.',
    responseFields=['planCode','name','versionCount','versions','note'],
    successStatus=200,
  ),
  'Get Plan Version': dict(
    summary='One plan version in full, with all its features and their labels.',
    responseFields=['planCode','planVersion','name','status','listPrice','currencyCode','publiclyAvailable','sellable','featureCount','features','schoolsOnThisVersion','note'],
    successStatus=200,
  ),
  'Create Subscription': dict(
    summary='Makes a school a paying customer. Closes the gap core activation complains about.',
    required=['planCode','planVersion'],
    optional=['trial','currentPeriodStart','currentPeriodEnd','autoRenew','contractedPrice','maxStudentsOverride','maxUsersOverride','billingCustomerReference','reason'],
    responseFields=['subscriptionId','subscriptionNo','schoolId','planCode','planVersion','planName','status','billingCycle','currentPeriodStart','currentPeriodEnd','autoRenew','contractedPrice','planListPrice','currencyCode','maxStudents','maxUsers','hasLimitOverrides','current','nextStep'],
    successStatus=201,
    captures=[('subscriptionNo','subscriptionNo')],
  ),
}

# Errors every school-surface endpoint can give, because of the tenant header.
TENANT_ERRORS = [
  (400,'TENANT_NOT_RESOLVED','The X-School-Subdomain header is missing or blank.'),
  (404,'SCHOOL_NOT_FOUND','No school has that subdomain.'),
  (409,'SCHOOL_NOT_EDITABLE','The school is past PROVISIONING, TRIAL or ACTIVE and cannot be edited.'),
]

STATUS_WORDS = {
  '200':'200 OK','201':'201 Created','204':'204 No Content','400':'400 Bad Request',
  '401':'401 Unauthorized','403':'403 Forbidden','404':'404 Not Found','409':'409 Conflict',
  '422':'422 Unprocessable Entity','500':'500 Internal Server Error',
}

def slug(name):
    return re.sub(r'[^a-z0-9]+','-',name.lower()).strip('-')

def strip_strings(line):
    """Blanks out the quoted parts of a line, so braces inside a value are not counted."""
    return re.sub(r'"(?:[^"\\]|\\.)*"', '""', line)

def split_body(raw):
    """
    Separates the runnable body from the notes below it.

    Matches braces to find where the JSON ends, rather than looking for the ===== banner:
    not every request has one, and a body split on the banner alone kept the notes in the
    JSON and made the request unsendable.
    """
    if raw is None:
        return '', ''
    lines = raw.split('\n')

    start = None
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith('//') or stripped == '':
            continue
        if stripped[0] in '{[':
            start = i
        break

    # No JSON at all — the whole thing is notes. Several endpoints take no body.
    if start is None:
        return '', raw

    opener = lines[start].strip()[0]
    closer = '}' if opener == '{' else ']'
    depth = 0
    end = None
    for i in range(start, len(lines)):
        clean = strip_strings(lines[i])
        depth += clean.count(opener) - clean.count(closer)
        if depth <= 0:
            end = i
            break
    if end is None:
        return raw.strip(), ''

    return '\n'.join(lines[start : end + 1]).strip(), '\n'.join(lines[end + 1 :])

def uncomment(block):
    out = []
    for line in block.split('\n'):
        stripped = line.lstrip()
        if stripped.startswith('//'):
            out.append(stripped[2:].removeprefix(' '))
        elif stripped == '':
            out.append('')
        else:
            out.append(line)
    return out

CASE_START = re.compile(r'^\s*(\d{2})\s\s+(.+?)\s+->\s+(.+?)\s*$')

def parse_cases(comment_block):
    """Pulls the numbered test cases out of the commented block under a body."""
    if not comment_block:
        return [], ''
    lines = uncomment(comment_block)
    starts = [i for i, line in enumerate(lines) if CASE_START.match(line)]

    preamble_lines = lines[: starts[0]] if starts else lines
    preamble = clean_notes_block(preamble_lines)

    cases = []
    for n, start in enumerate(starts):
        end = starts[n + 1] if n + 1 < len(starts) else len(lines)
        head = CASE_START.match(lines[start])
        chunk = lines[start + 1 : end]
        body, notes = extract_payload(chunk)
        cases.append({
            'id': head.group(1),
            'name': head.group(2).strip(),
            'expect': normalise_expect(head.group(3)),
            'notes': notes,
            'body': body,
            'query': query_override(notes),
        })
    return cases, preamble

def normalise_expect(text):
    text = text.strip()
    m = re.match(r'^(\d{3})\b', text)
    if m and len(text) <= 4:
        return STATUS_WORDS.get(m.group(1), text)
    return text

def extract_payload(chunk):
    """Finds the JSON payload in a case, if it has one, and keeps the rest as notes."""
    start = None
    for i, line in enumerate(chunk):
        if line.startswith('{') or line.startswith('['):
            start = i
            break
    if start is None:
        return None, clean_notes_block(chunk)

    opener = chunk[start][0]
    closer = '}' if opener == '{' else ']'
    depth = 0
    end = None
    for i in range(start, len(chunk)):
        depth += chunk[i].count(opener) - chunk[i].count(closer)
        if depth <= 0:
            end = i
            break
    if end is None:
        return None, clean_notes_block(chunk)

    body = '\n'.join(chunk[start : end + 1]).strip()
    notes = clean_notes_block(chunk[:start] + chunk[end + 1 :])
    return body, notes

def clean_notes_block(lines):
    kept = []
    for line in lines:
        s = line.strip()
        if re.match(r'^-{5,}$', s) or re.match(r'^={5,}$', s):
            continue
        if s.startswith('TEST CASES'):
            continue
        if s.startswith('Swap the body above') or s.startswith('Postman strips'):
            continue
        kept.append(line.rstrip())
    # collapse runs of blank lines
    text = '\n'.join(kept)
    text = re.sub(r'\n{3,}', '\n\n', text).strip()
    return text

def parse_cases_from_description(description):
    """
    Some endpoints keep their cases in the description instead of the body, because Postman
    sends no body on a GET or a DELETE. They are in a fenced code block, in the same shape.
    """
    if not description:
        return []
    blocks = re.findall(r'```(?:\w+)?\n(.*?)```', description, re.S)
    cases = []
    for block in blocks:
        lines = block.split('\n')
        starts = [i for i, line in enumerate(lines) if CASE_START.match(line)]
        for n, start in enumerate(starts):
            end = starts[n + 1] if n + 1 < len(starts) else len(lines)
            head = CASE_START.match(lines[start])
            notes = clean_notes_block(lines[start + 1 : end])
            cases.append({
                'id': head.group(1),
                'name': head.group(2).strip(),
                'expect': normalise_expect(head.group(3)),
                'notes': notes,
                'body': None,
                'query': query_override(notes),
            })
    return cases

QUERY_LINE = re.compile(r'^\s*\?([A-Za-z]+=[^\s]*(?:&[A-Za-z]+=[^\s]*)*)\s*(?:$|\s)')

def query_override(notes):
    """
    A case whose notes begin with ?something=... is really about the query string, so pull it
    out and let "Use this" set the parameters rather than making somebody retype them.
    """
    for line in notes.split('\n'):
        m = QUERY_LINE.match(line)
        if m:
            pairs = []
            for pair in m.group(1).split('&'):
                if '=' not in pair:
                    continue
                key, value = pair.split('=', 1)
                pairs.append((key, value))
            if pairs:
                return pairs
    return None

def path_of(raw_url):
    path = raw_url.replace('{{baseUrl}}', '')
    return path.split('?')[0]

def build_path_params(path):
    """Turns the Postman variables in a URL into named path parameters."""
    params = []
    out = path
    if '{{schoolId}}' in out:
        out = out.replace('{{schoolId}}', '{id}')
        params.append(('id', '{{schoolId}}', "The school's MongoDB id. Create School fills this in."))
    if '{{academicYearName}}' in out:
        out = out.replace('{{academicYearName}}', '{name}')
        params.append(('name', '{{academicYearName}}',
                       'The year name, such as 2026-2027. It is the join key and can never change.'))
    if '{{planCode}}' in out:
        out = out.replace('{{planCode}}', '{code}')
        params.append(('code', '{{planCode}}',
                       "The plan's permanent family code. Create Plan Draft fills this in."))
    if '{{planVersion}}' in out:
        out = out.replace('{{planVersion}}', '{version}')
        params.append(('version', '{{planVersion}}',
                       'Which version of that plan. Versions are immutable once published.'))
    if '{{subscriptionNo}}' in out:
        out = out.replace('{{subscriptionNo}}', '{no}')
        params.append(('no', '{{subscriptionNo}}',
                       'The subscription number, such as SUB/2026/09/000001.'))

    # A literal date in the path is the holiday key.
    m = re.search(r'/(\d{4}-\d{2}-\d{2})(?=/|$)', out)
    if m:
        out = out[: m.start(1)] + '{date}' + out[m.end(1) :]
        params.append(('date', m.group(1), 'The closed day, as YYYY-MM-DD.'))
    return out, params

def derive_errors(cases, is_school_surface):
    """Builds the error table from the cases, since each one names its code."""
    seen = {}
    order = []
    for case in cases:
        m = re.match(r'^(\d{3})', case['expect'])
        if not m or m.group(1).startswith('2'):
            continue
        status = int(m.group(1))
        codes = re.findall(r'"code"\s*:\s*"([A-Z_]+)"', case['notes'])
        code = codes[0] if codes else '—'
        key = (status, code)
        if key in seen:
            continue
        seen[key] = True
        order.append((status, code, case['name'][0].upper() + case['name'][1:].lower()))
    if is_school_surface:
        for status, code, when in TENANT_ERRORS:
            if (status, code) not in seen:
                seen[(status, code)] = True
                order.append((status, code, when))
    order.sort(key=lambda row: row[0])
    return order

def js(value):
    return json.dumps(value, ensure_ascii=False)

def js_template(text):
    """A JS template literal, for bodies that contain quotes and newlines."""
    if text is None:
        return 'null'
    escaped = text.replace('\\', '\\\\').replace('`', '\\`').replace('${', '\\${')
    return '`' + escaped + '`'

# ---------------------------------------------------------------------------

collection = json.load(open(COLLECTION))

groups = []
for folder in collection['item']:
    for sub in folder.get('item', []):
        if 'item' in sub:
            groups.append((f"{folder['name']} / {sub['name']}", sub['item']))
        else:
            groups.append((folder['name'], folder['item']))
            break

seen_group_names = set()
unique_groups = []
for name, items in groups:
    if name in seen_group_names:
        continue
    seen_group_names.add(name)
    unique_groups.append((name, items))

out = []
w = out.append

w('''/**
 * Every endpoint the battleground knows about.
 *
 * Generated from postman/"Orbit Sphere — API.postman_collection.json" — the bodies, the query
 * parameters, the headers, the notes and every numbered test case come from there, unchanged.
 * What the collection cannot say (which fields are required, what comes back, which variable to
 * remember after a call) was read out of the controllers, the DTOs and the services.
 *
 * When the collection changes, the quickest way to update this file is to regenerate it rather
 * than hand-edit: everything here has a source in the repository.
 *
 * TENANT HEADER: every /schools/current/... endpoint needs X-School-Subdomain, because there is
 * no authentication yet and CurrentSchoolResolver reads the tenant from that header. It is set
 * on those requests already, pointing at {{createdSubdomain}}.
 */
''')

flat_ids = []
group_consts = []

for group_name, items in unique_groups:
    const = 'GROUP_' + re.sub(r'[^A-Z0-9]+', '_', group_name.upper()).strip('_')
    group_consts.append((const, group_name))
    w(f'const {const} = {{')
    w(f'  id: {js(slug(group_name))},')
    w(f'  module: {js(group_name)},')
    w('  endpoints: [')

    for item in items:
        name = item['name']
        req = item['request']
        method = req['method']
        url = req.get('url', {})
        raw = url if isinstance(url, str) else url.get('raw', '')
        path_only = path_of(raw)
        path, path_params = build_path_params(path_only)

        headers = [(h['key'], h.get('value', ''), not h.get('disabled'))
                   for h in req.get('header', [])]
        query = [(q['key'], q.get('value', ''), not q.get('disabled'))
                 for q in (url.get('query') if isinstance(url, dict) else None) or []]

        body_raw = (req.get('body') or {}).get('raw')
        active_body, comment_block = split_body(body_raw)
        cases, body_notes = parse_cases(comment_block)
        if not cases:
            # No body on this method, so the cases are in the description instead.
            cases = parse_cases_from_description(req.get('description'))

        meta = CURATED.get(name, {})
        is_school_surface = path.startswith('/schools/current')
        errors = derive_errors(cases, is_school_surface)

        method_takes_body = method not in ('GET', 'HEAD', 'DELETE')

        w('    {')
        w(f'      id: {js(slug(name))},')
        w(f'      name: {js(name)},')
        w(f'      method: {js(method)},')
        w(f'      path: {js(path)},')
        w("      status: 'live',")
        w(f'      summary: {js(meta.get("summary", ""))},')
        w(f'      schoolSurface: {"true" if is_school_surface else "false"},')
        w(f'      docs: {js_template(req.get("description") or "")},')
        if body_notes:
            w(f'      bodyNotes: {js_template(body_notes)},')
        if meta.get('required'):
            w(f'      requiredFields: {js(meta["required"])},')
        if meta.get('optional'):
            w(f'      optionalFields: {js(meta["optional"])},')

        if path_params:
            w('      pathParams: [')
            for pname, pvalue, pdesc in path_params:
                w(f'        {{ name: {js(pname)}, value: {js(pvalue)}, description: {js(pdesc)} }},')
            w('      ],')
        else:
            w('      pathParams: [],')

        if query:
            w('      queryParams: [')
            for qk, qv, qon in query:
                w(f'        {{ key: {js(qk)}, value: {js(qv)}, enabled: {"true" if qon else "false"} }},')
            w('      ],')
        else:
            w('      queryParams: [],')

        if headers:
            w('      headers: [')
            for hk, hv, hon in headers:
                w(f'        {{ key: {js(hk)}, value: {js(hv)}, enabled: {"true" if hon else "false"} }},')
            w('      ],')
        else:
            w('      headers: [],')

        w(f'      bodyAllowed: {"true" if (method_takes_body and active_body) else "false"},')
        w(f'      body: {js_template(active_body if method_takes_body else "")},')
        w(f'      successStatus: {meta.get("successStatus", 200)},')
        if meta.get('successNote'):
            w(f'      successNote: {js(meta["successNote"])},')
        w(f'      responseFields: {js(meta.get("responseFields", []))},')

        if meta.get('captures'):
            w('      captures: [')
            for var, frm in meta['captures']:
                w(f'        {{ variable: {js(var)}, from: {js(frm)} }},')
            w('      ],')
        else:
            w('      captures: [],')

        if errors:
            w('      errors: [')
            for status, code, when in errors:
                w(f'        {{ status: {status}, code: {js(code)}, when: {js(when)} }},')
            w('      ],')
        else:
            w('      errors: [],')

        if cases:
            w('      examples: [')
            for case in cases:
                w('        {')
                w(f'          id: {js(case["id"])},')
                w(f'          name: {js(case["name"])},')
                w(f'          expect: {js(case["expect"])},')
                w(f'          notes: {js_template(case["notes"])},')
                w(f'          body: {js_template(case["body"]) if case["body"] is not None else "null"},')
                if case.get('query'):
                    pairs = ', '.join(
                        '{ key: %s, value: %s, enabled: true }' % (js(k), js(v))
                        for k, v in case['query'])
                    w(f'          queryParams: [{pairs}],')
                w('        },')
            w('      ],')
        else:
            w('      examples: [],')

        w('    },')
        flat_ids.append(slug(name))

    w('  ],')
    w('};')
    w('')

w('export const API_CATALOG = [')
for const, _ in group_consts:
    w(f'  {const},')
w('];')
w('')
w('''/** Flat list, handy for searching and for finding an endpoint by id from the history. */
export const ALL_ENDPOINTS = API_CATALOG.flatMap((group) =>
  group.endpoints.map((endpoint) => ({ ...endpoint, module: group.module })),
);

export function findEndpoint(id) {
  return ALL_ENDPOINTS.find((endpoint) => endpoint.id === id) || null;
}

export const LIVE_COUNT = ALL_ENDPOINTS.length;

/** How many worked examples the collection carries, shown in the sidebar footer. */
export const CASE_COUNT = ALL_ENDPOINTS.reduce(
  (total, endpoint) => total + endpoint.examples.length,
  0,
);''')

open(OUT, 'w').write('\n'.join(out) + '\n')
print('wrote', OUT)
print('endpoints:', len(flat_ids))
