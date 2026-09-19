import { useState } from 'react'
import { Check, LogIn } from 'lucide-react'
import { useApi, useApiState } from '../api/apiContext.js'

/**
 * Writes the chosen school, year and staff member into the local-user cookie.
 *
 * WHY IT IS A BUTTON AND NOT AUTOMATIC. It used to fire whenever any of the three pickers changed,
 * which meant one decision — school, then year, then person — put THREE calls in the log, two of
 * them storing a context nobody meant. Signing in is what somebody does once they have finished
 * choosing, so it is something they press.
 *
 * IT NOW MATTERS TO EVERY OTHER REQUEST. Since 2026-09-19 the backend resolves the tenant from this
 * cookie's `schoolId` claim, so until this is pressed, school-scoped screens fall back to the
 * X-School-Subdomain header the tester still sends. Press it and the cookie wins over that header.
 *
 * IT IS STILL NOT A SIGN-IN, and that matters more now than it did. What it gets back is a signed
 * JWT in an `idtoken` cookie, which sounds far more like a credential than it is: nothing
 * authenticates the caller, so anybody can ask for a token asserting any school, and the signature
 * only proves the server issued it. The backend repeats that in a `warning` field on every
 * response, and the hover here says it too. The button is called "Sign in" because that is what the
 * person pressing it is doing in their head.
 *
 * NOTHING IS DISABLED, so pressing it with nothing chosen is allowed and stores an empty context.
 * That is a real thing to test, and a button that greyed itself out would be the tester deciding
 * which requests are worth making.
 *
 * THE TICK IS DERIVED, NOT TIMED. After a successful press the button shows a tick for exactly as
 * long as the pickers still match what was sent — change any of the three and it goes back to
 * "Sign in", because the token in the cookie no longer describes what the top bar shows. A timer would have said
 * "done" for two seconds and then nothing, which answers a question nobody asked; this answers
 * "does the cookie match what I am looking at".
 */
export default function SignIn() {
  const { signIn } = useApi()
  const { actingSchoolId, actingAcademicYear, actingStaffDocsId } = useApiState()

  const [sending, setSending] = useState(false)
  //! WHAT WAS LAST SENT, not whether anything was. Comparing it to the pickers is what makes the
  //! tick mean "the cookie is current" rather than "you pressed this at some point".
  const [signedInFor, setSignedInFor] = useState(null)

  const current = `${actingSchoolId ?? ''}|${actingAcademicYear ?? ''}|${actingStaffDocsId ?? ''}`
  const matches = signedInFor === current

  const press = async () => {
    setSending(true)
    const answer = await signIn()
    setSending(false)
    //! ONLY ON SUCCESS. A refusal leaves the button offering to try again, which is the truth —
    //! and the failure itself is already in the log, where every other one is.
    setSignedInFor(answer?.ok ? current : null)
  }

  return (
    <button
      type="button"
      className={`picker-trigger${matches ? ' is-on' : ''}`}
      onClick={press}
      title={
        'Writes the chosen school, year and staff member into the idtoken cookie. Every '
        + 'request then resolves its school from that cookie. Still not authentication — '
        + 'anybody can ask for a token saying anything.'
      }
    >
      {matches
        ? <Check size={13} className="picker-icon" aria-hidden="true" />
        : <LogIn size={13} className="picker-icon" aria-hidden="true" />}
      <span className="picker-label">{sending ? 'Signing in…' : 'Sign in'}</span>
    </button>
  )
}
