/**
 * What each grading scale allows, and how its bands convert — shared by #1 (create) and #3
 * (update).
 *
 * EXTRACTED WHEN #3 ARRIVED. The create modal and the edit modal ask the same question — what are
 * this scheme's bands — and a copy of the rules is how two forms come to disagree about which
 * fields a DESCRIPTOR scheme may send.
 *
 * NO JSX HERE, deliberately. The component lives in BandEditor.jsx: a module that exports both
 * constants and components breaks fast refresh, which is what planOptions.js is split for too.
 *
 * Nothing here talks to the API either. It says what a scale allows; deciding what to SEND is the
 * caller's, because #1 sends everything and #3 sends only what changed.
 */

export const SCALES = ['PERCENTAGE', 'MARKS', 'DESCRIPTOR']

export const BLANK_BAND = {
  gradeCode: '', minimumValue: '', maximumValue: '', gradePoint: '', description: '', passed: 'true',
}

/**
 * ONE CONFIG PER SCALE, and the form reads nothing else.
 *
 * The scale is the field that decides what every other field means, so a boolean `measured` flag
 * scattered through the JSX was the wrong shape: it could say "bounded or not" and nothing about
 * what the bounds are IN. A percentage band runs 91–100 of a fixed 100; a marks band runs 40–50 of
 * a total the school picks; a descriptor band has no numbers at all and its ceiling is a 400.
 *
 * `ceiling: null` means the scale is not measured — the API REFUSES maximumValue and refuses every
 * bound, so the form must not offer them.
 *
 * `ceiling.fixed` means the value is not the school's to choose. A PERCENTAGE scheme is out of 100
 * BY DEFINITION, so the box is filled and locked rather than left for somebody to type 90 into and
 * get a scheme whose bands mean something other than percent.
 */
export const SCALE = {
  PERCENTAGE: {
    blurb: 'Bands are percentages of 100. What CBSE and ICSE internal reporting use, and what an IB scheme looks like once its boundaries are expressed as percentages.',
    ceiling: {
      fixed: '100',
      hint: 'Locked: a percentage scheme is out of 100 by definition. Pick MARKS for a paper with a different total.',
    },
    unit: '%',
    from: '91',
    to: '100',
    bandHint: 'Stored in the order given, never re-sorted. Bounds are inclusive at BOTH ends, so two bands must not touch: 81–90 beside 90–100 both claim 90.',
    presetLabel: 'Load the CBSE scale',
  },
  MARKS: {
    blurb: 'Bands are raw marks out of a total you set. The same walk as a percentage — the difference is the reported figure, since "43 / 50" is not "86%".',
    ceiling: {
      fixed: null,
      hint: 'The paper total the bands are read against — 50, 25, 80. Required on this scale, and yours to choose.',
    },
    unit: 'marks',
    from: '40',
    to: '50',
    bandHint: 'Bounds are raw marks, inclusive at both ends, and none may reach past the total above. Stored in the order given.',
    presetLabel: 'Load an out-of-50 scale',
  },
  DESCRIPTOR: {
    blurb: 'No numbers anywhere. A teacher picks "Developing" directly, so there is nothing to measure and nothing to resolve by value.',
    ceiling: null,
    unit: null,
    from: null,
    to: null,
    bandHint: 'A descriptor grade is chosen, not computed — so a band is a code and a description. Sending any bound, or a ceiling, is a 400.',
    presetLabel: 'Load the three descriptors',
  },
}

/** The scale every example in the plan is drawn from — eight bands, nothing ungraded. */
export const CBSE = [
  { gradeCode: 'A1', minimumValue: '91', maximumValue: '100', gradePoint: '10', description: 'Outstanding', passed: 'true' },
  { gradeCode: 'A2', minimumValue: '81', maximumValue: '90', gradePoint: '9', description: 'Excellent', passed: 'true' },
  { gradeCode: 'B1', minimumValue: '71', maximumValue: '80', gradePoint: '8', description: 'Very Good', passed: 'true' },
  { gradeCode: 'B2', minimumValue: '61', maximumValue: '70', gradePoint: '7', description: 'Good', passed: 'true' },
  { gradeCode: 'C1', minimumValue: '51', maximumValue: '60', gradePoint: '6', description: 'Fair', passed: 'true' },
  { gradeCode: 'C2', minimumValue: '41', maximumValue: '50', gradePoint: '5', description: 'Average', passed: 'true' },
  { gradeCode: 'D', minimumValue: '33', maximumValue: '40', gradePoint: '4', description: 'Below Average', passed: 'true' },
  { gradeCode: 'E', minimumValue: '0', maximumValue: '32', gradePoint: '0', description: 'Needs Improvement', passed: 'false' },
]

/** A paper out of 50 — what MARKS is for, and the shape a unit test is graded on. */
export const OUT_OF_50 = [
  { gradeCode: 'A', minimumValue: '40', maximumValue: '50', gradePoint: '10', description: 'Outstanding', passed: 'true' },
  { gradeCode: 'B', minimumValue: '30', maximumValue: '39', gradePoint: '8', description: 'Good', passed: 'true' },
  { gradeCode: 'C', minimumValue: '17', maximumValue: '29', gradePoint: '6', description: 'Fair', passed: 'true' },
  { gradeCode: 'D', minimumValue: '0', maximumValue: '16', gradePoint: '0', description: 'Needs Improvement', passed: 'false' },
]

/** CBSE's co-scholastic areas grade this way — a code and a word, no range. */
export const DESCRIPTORS = [
  { gradeCode: 'SECURE', minimumValue: '', maximumValue: '', gradePoint: '', description: 'Secure', passed: 'true' },
  { gradeCode: 'DEVELOPING', minimumValue: '', maximumValue: '', gradePoint: '', description: 'Developing', passed: 'true' },
  { gradeCode: 'BEGINNING', minimumValue: '', maximumValue: '', gradePoint: '', description: 'Beginning', passed: 'false' },
]

export const PRESET = { PERCENTAGE: CBSE, MARKS: OUT_OF_50, DESCRIPTOR: DESCRIPTORS }

/** A stored scheme's bands as the editor's rows — every value a string, because inputs hold text. */
export function bandsToRows(bands) {
  return (bands ?? []).map((b) => ({
    gradeCode: b.gradeCode ?? '',
    minimumValue: b.minimumValue != null ? String(b.minimumValue) : '',
    maximumValue: b.maximumValue != null ? String(b.maximumValue) : '',
    gradePoint: b.gradePoint != null ? String(b.gradePoint) : '',
    description: b.description ?? '',
    passed: b.passed === false ? 'false' : 'true',
  }))
}

/**
 * The editor's rows as the API's bands.
 *
 * An empty optional box sends NOTHING rather than "", which the API would read as a value — and on
 * an unmeasured scale the bounds are omitted entirely rather than sent as null, because a
 * DESCRIPTOR band carrying either is a 400.
 */
export function rowsToBands(rows, measured) {
  return rows.map((band) => {
    const one = { gradeCode: band.gradeCode }
    if (measured) {
      if (band.minimumValue !== '') one.minimumValue = Number(band.minimumValue)
      if (band.maximumValue !== '') one.maximumValue = Number(band.maximumValue)
    }
    if (band.gradePoint !== '') one.gradePoint = Number(band.gradePoint)
    if (band.description !== '') one.description = band.description
    // Only sent when false: the API defaults it to true, so sending true on seven of eight bands
    // is noise in a body somebody is reading to understand the request.
    if (band.passed === 'false') one.passed = false
    return one
  })
}

/** Whether two row sets differ — what #3 uses to decide whether to send gradeBands at all. */
export function bandsChanged(a, b) {
  return JSON.stringify(a) !== JSON.stringify(b)
}
