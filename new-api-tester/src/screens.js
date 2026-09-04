import {
  Building2, CalendarDays, CreditCard, Package, School, Settings2,
} from 'lucide-react'

/**
 * The screens this app has, grouped the way the API groups itself.
 *
 * THE GROUPS ARE THE BACKEND'S, NOT INVENTED. These six are exactly the six the built endpoints
 * fall into, as `api-battleground/src/config/endpoints.js` reports them. Following the API's own
 * grouping means a screen is always about ONE SURFACE of one module, which is why the platform's
 * subscription endpoints and the school's own two reads are separate rows rather than one
 * "Subscriptions" screen: they answer to different callers, and one of them must never show what
 * the other does.
 *
 * `endpoints` is how many are built against that group today, shown as a badge so the nav says
 * where the work is rather than looking uniformly full. The numbers come from that generated
 * catalogue — count them again there rather than trusting these if they start to look stale.
 */
export const SCREENS = [
  { label: 'Schools', to: '/core/schools', icon: Building2, group: 'Core', endpoints: 8 },
  { label: 'School profile', to: '/core/profile', icon: Settings2, group: 'Core', endpoints: 5 },
  { label: 'Academic years', to: '/core/academic-years', icon: CalendarDays, group: 'Core', endpoints: 18 },
  { label: 'Plan catalogue', to: '/plans/catalogue', icon: Package, group: 'Plans', endpoints: 9 },
  { label: 'Subscriptions', to: '/plans/subscriptions', icon: CreditCard, group: 'Plans', endpoints: 3 },
  { label: "A school's own view", to: '/plans/my-subscription', icon: School, group: 'Plans', endpoints: 2 },
]

/** The groups, in the order they first appear above — so adding a screen cannot orphan it. */
export const GROUPS = SCREENS.reduce((groups, screen) => {
  const found = groups.find((one) => one.title === screen.group)
  if (found) found.items.push(screen)
  else groups.push({ title: screen.group, items: [screen] })
  return groups
}, [])
