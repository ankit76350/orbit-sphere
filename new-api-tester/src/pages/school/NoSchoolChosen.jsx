import { Building2 } from 'lucide-react'
import { Card, Empty } from '../../components/ui/Kit.jsx'

/**
 * What every school-surface screen shows when nobody has said which school.
 *
 * One level up from the modules because it belongs to the SURFACE: Core and Plans both need it,
 * and neither owns it.
 *
 * The alternative is sending the call anyway and rendering `400 TENANT_NOT_RESOLVED`, which
 * reads as a bug in the screen rather than as a thing the person has not done yet. This says
 * what to do and where.
 */
export default function NoSchoolChosen({ what }) {
  return (
    <Card>
      <Empty
        title="No school chosen"
        description={
          `${what} is a school-surface read: the tenant comes from a header, not the URL, so `
          + 'there is no "current" school until you pick one. Set "Acting as" in the top bar to '
          + "a school's subdomain."
        }
        action={<Building2 size={22} aria-hidden="true" />}
      />
    </Card>
  )
}
