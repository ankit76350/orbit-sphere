import { useApi, useApiState } from '../api/apiContext.js'
import SchoolPicker from './SchoolPicker.jsx'

/**
 * Which school the app is acting as, on the school surface.
 *
 * WHY THIS HAS TO EXIST. The school surface never names a school in the URL — the tenant comes
 * from the `X-School-Subdomain` header, which is the stand-in for a session until there is
 * sign-in. So `GET /schools/current/profile` is meaningless without somebody having said which
 * school "current" is, and every screen under `School` needs the answer.
 *
 * IT IS IN THE TOP BAR BECAUSE IT IS A MODE, NOT A PAGE'S SETTING. Everything under `School`
 * asks "what does THIS school see", and the answer should not change as you move between its
 * screens — nor should it be re-picked on each one, which is how somebody ends up testing the
 * wrong tenant without noticing. It is remembered between reloads for the same reason.
 *
 * The dropdown itself is SchoolPicker, which the platform's subscription screen also uses — but
 * there the school is an argument to a call rather than a mode. Keeping the distinction in the
 * callers means neither can read the other's choice by accident.
 *
 * IT IS LABELLED "SCHOOL", NOT "ACTING AS". The file keeps the name because that is what the
 * thing IS — the school every school-surface call is made as — but "acting as" is our word for
 * it, not a word anybody reads off a screen and understands. The label says what is being
 * chosen; the hover says what choosing it does.
 */
export default function ActingAs() {
  const { chooseSchool } = useApi()
  const { actingSubdomain } = useApiState()

  return (
    <SchoolPicker
      label="School"
      as="subdomain"
      value={actingSubdomain}
      onChange={chooseSchool}
      placeholder="none chosen"
      title="Which school every School endpoint reads and writes. Sent as the tenant header."
    />
  )
}
