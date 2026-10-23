import { useEffect, useRef, useState } from 'react'

/**
 * A value that only catches up once the typing stops.
 *
 * <p>Used by anything that asks the server a question about a box somebody is still typing in —
 * the guardian phone and email checks are the first, and every "does this already exist" field
 * after them is the same shape.
 *
 * WHY A HOOK AND NOT A setTimeout INSIDE THE EFFECT. That is what it was until 2026-10-08, and it
 * worked, but the debounce was tangled up with the fetch, the "is it long enough to ask about"
 * guard and the stale-result handling — four concerns in one effect, where only one of them is
 * about timing. Pulled out, the caller's effect depends on a value that is simply *late*, and
 * needs to know nothing about timers at all.
 *
 * IT RETURNS THE FIRST VALUE IMMEDIATELY. A form that opens with a number already in the box —
 * the enrolment one does, seeded from the admission form — should not wait half a second to find
 * out that number is somebody's. Only *changes* are delayed.
 *
 * THE TIMER IS CLEARED ON EVERY CHANGE, so a run of keystrokes produces exactly one update: the
 * last one. That is the whole point — a ten digit phone number is one question, not ten.
 */
export default function useDebounced(value, delayMs = 400) {
  const [settled, setSettled] = useState(value)
  //! THE FIRST VALUE IS NOT A CHANGE. Without this the initial render schedules a pointless
  //! timeout and the caller asks the same question twice on open.
  const first = useRef(true)

  useEffect(() => {
    if (first.current) {
      first.current = false
      setSettled(value)
      return undefined
    }

    const timer = setTimeout(() => setSettled(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])

  return settled
}
