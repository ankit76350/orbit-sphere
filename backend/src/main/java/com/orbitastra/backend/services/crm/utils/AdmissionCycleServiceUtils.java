package com.orbitastra.backend.services.crm.utils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleCapacityResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.models.crm.enums.AdmissionCycleStatus;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;

import lombok.RequiredArgsConstructor;

/**
 * The read {@link com.orbitastra.backend.services.crm.AdmissionCycleService} makes more than once.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and <b>a method here
 * never calls another method here</b>. Only the service calls these.
 *
 * <p><b>It held one method until 2026-09-24 and now holds five.</b> The rule changed rather than
 * the code: a service file holds nothing but its endpoint methods, whatever shape the rest are.
 * {@code datesRunForwards} and {@code nextStepFor} each have a single caller and used to stay
 * inline for that reason — the rule now counts what kind of thing a method is, not how many
 * callers it has.
 */
@Component
@RequiredArgsConstructor
public class AdmissionCycleServiceUtils {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    public static final String NO_AUTHORIZATION_YET =
            "No authorization is enforced on this endpoint yet: any caller who can reach it can "
                    + "run it.";

    /**
     * The four dates, in the order they must run, with what to call each one in a message.
     *
     * <p>One list because #1 and #2 both check the same ordering, and two copies of it would be
     * two chances for the order to disagree with itself.
     */
    public static final List<String> DATE_FIELDS = List.of(
            "inquiryOpenAt", "applicationOpenAt", "applicationCloseAt", "enrollmentDeadlineAt");

    private final SchoolClassRepository schoolClasses;

    /**
     * The names of the classes a seat table points at, keyed by id.
     *
     * <p><b>ONE QUERY FOR EVERY ROW, not one per row.</b> A cycle can hold twenty classes, and
     * reading them one at a time is the N+1 this project keeps naming.
     *
     * <p><b>Nothing to look up is not a query.</b> Every cycle is created with an empty seat table,
     * so an empty list is the common case rather than an edge one.
     *
     * <p><b>The year is the CYCLE'S, not the school's current one.</b> A cycle admits into one year
     * and seats against another year's class are seats nobody could fill — which is why the year is
     * a parameter and not something this reads for itself.
     *
     * <p><b>A class that is gone is simply absent from the map.</b> That is what lets #6 render a
     * seat row with no name, and what lets #4 tell the caller which of the ids it sent do not
     * exist: the keys are the ones that were found.
     *
     * <p>A merge function is needed even though ids are unique: {@code toMap} throws on a duplicate
     * key rather than keeping either.
     *
     * Used by:
     * - getCycle()
     * - setCapacities()
     */
    public Map<String, String> classNamesFor(School school, String academicYear,
            Collection<String> classDocsIds) {

        if (classDocsIds == null || classDocsIds.isEmpty()) {
            return Map.of();
        }

        // TODO: read school classes
        List<SchoolClass> found = schoolClasses.findBySchoolIdAndAcademicYearAndIdIn(
                school.getId(), academicYear, List.copyOf(classDocsIds));

        return found.stream().collect(Collectors.toMap(
                SchoolClass::getId, SchoolClass::getName, (first, second) -> first));
    }

    /**
     * Do these four run forwards, ignoring the ones that are absent?
     *
     * <p>The same rule #1 and #2 enforce, asked of a state that has not been saved yet. Private,
     * so it is an implementation detail of this class rather than something another service could
     * come to depend on.
     */
    public static boolean datesRunForwards(Map<String, Instant> dates) {
        Instant earlier = null;
        for (String field : DATE_FIELDS) {
            Instant when = dates.get(field);
            if (when == null) {
                continue;
            }
            if (earlier != null && when.isBefore(earlier)) {
                return false;
            }
            earlier = when;
        }
        return true;
    }

    /**
     * What to do now that the cycle has moved. Inline rather than in utils: one caller.
     *
     * <p>Says what the new status means for applications, because that is the only thing anybody
     * is moving a cycle for — and names the endpoint that is still missing where there is one.
     */
    public static String nextStepFor(AdmissionCycle cycle, AdmissionCycleStatus from) {
        String moved = "'" + cycle.getName() + "' moved from " + from + " to "
                + cycle.getStatus() + ". ";
        String what = switch (cycle.getStatus()) {
            case SCHEDULED -> "It is set up but not taking applications yet — move it to OPEN when "
                    + "the round starts.";
            case OPEN -> "Applications can be submitted into it now. #17 is the endpoint that "
                    + "takes one.";
            case CLOSED -> "No new applications. The ones already in can still be reviewed, "
                    + "offered and enrolled.";
            case COMPLETED -> "The round is finished and this is where it stops — nothing moves "
                    + "from COMPLETED.";
            case CANCELLED -> "The round is abandoned and nothing can be applied for. This is "
                    + "terminal; a replacement round is a new cycle.";
            case DRAFT -> "It is back to being set up.";
        };
        return moved + what + " " + NO_AUTHORIZATION_YET;
    }

    /**
     * Zero for a status nothing is in.
     *
     * <p><b>PRIVATE, so it is not one utils method calling another.</b> The folder rule is that a
     * method here never calls another one here — that is about the <i>shared surface</i> the
     * service calls, and this is one file's own tidying, exactly as
     * {@code AdmissionReviewServiceUtils.usable()} is. {@code rowFor} reads it ten times and
     * {@code capacityNextStep} reads it too.
     *
     * Used by: rowFor(), totalOf(), capacityNextStep().
     */
    private static long count(Map<AdmissionApplicationStatus, Long> counts,
            AdmissionApplicationStatus status) {
        return counts.getOrDefault(status, 0L);
    }

    /**
     * What this report is telling the school, in plain words.
     *
     * <p>Used by: getCapacity().
     */
    public static String capacityNextStep(AdmissionCycle cycle,
            List<AdmissionCycleCapacityResponse.Row> rows, int over) {

        if (rows.isEmpty()) {
            return "'" + cycle.getName() + "' has no seat table, so there is nothing to count "
                    + "against. #4 is what sets one, and #3 refuses to open a round without it.";
        }
        if (over > 0) {
            return "OVER-COMMITTED in " + over + " class" + (over == 1 ? "" : "es")
                    + ". That is not necessarily wrong — schools offer more seats than they have "
                    + "because a fifth of families go elsewhere, and #29 does not cap it for that "
                    + "reason — but it is the number nobody could see until this endpoint existed.";
        }
        return "Every class is within its seats. Remember that approvals are not commitments: a "
                + "seat is promised when a letter goes out (#29), not when the school decides.";
    }

    /**
     * One class's row, from its seat entry and its counts.
     *
     * <p>Private and inline: used by {@code getCapacity()} alone, and the folder rules keep
     * single-use logic where it is used.
     */
    public static AdmissionCycleCapacityResponse.Row rowFor(IntakeCapacity seat, String className,
            Map<AdmissionApplicationStatus, Long> counts) {

        int total = seat.getTotalSeats() == null ? 0 : seat.getTotalSeats();
        int reserved = seat.getReservedSeats() == null ? 0 : seat.getReservedSeats();
        int open = total - reserved;

        //! PENDING IS EVERYTHING NOBODY HAS DECIDED — submitted, being reviewed, or waiting on the
        //! family for more. A DRAFT is not here: the family has not sent it, so it is not this
        //! round's problem yet.
        long pending = count(counts, AdmissionApplicationStatus.SUBMITTED)
                + count(counts, AdmissionApplicationStatus.UNDER_REVIEW)
                + count(counts, AdmissionApplicationStatus.ADDITIONAL_INFORMATION_REQUIRED);

        long offered = count(counts, AdmissionApplicationStatus.OFFERED);
        long accepted = count(counts, AdmissionApplicationStatus.OFFER_ACCEPTED);
        long enrolled = count(counts, AdmissionApplicationStatus.ENROLLED);

        //! COMMITTED IS WHAT HAS BEEN PROMISED OR GIVEN, and APPROVED is deliberately not in it. A
        //! school that approved forty children has decided something; it has not promised anybody
        //! a seat until a letter goes out, and counting approvals as commitments would make every
        //! round look over-subscribed the moment it started deciding.
        long committed = offered + accepted + enrolled;
        long free = open - committed;

        return new AdmissionCycleCapacityResponse.Row(
                seat.getClassDocsId(), className, total, reserved, open,
                pending,
                count(counts, AdmissionApplicationStatus.APPROVED),
                count(counts, AdmissionApplicationStatus.WAITLISTED),
                offered, accepted, enrolled,
                count(counts, AdmissionApplicationStatus.REJECTED),
                count(counts, AdmissionApplicationStatus.WITHDRAWN),
                committed, free, free < 0);
    }

    /**
     * The same numbers added up.
     *
     * <p><b>The total's {@code overCommitted} is NOT the sum of the flags.</b> It asks the same
     * question of the totals: a round can be over-committed overall while every class looks fine,
     * and the other way round. Both are worth knowing, which is why the count of over-committed
     * classes is a separate field.
     *
     * <p>Used by: getCapacity().
     */
    public static AdmissionCycleCapacityResponse.Row totalOf(
            List<AdmissionCycleCapacityResponse.Row> rows) {

        int total = rows.stream().mapToInt(AdmissionCycleCapacityResponse.Row::totalSeats).sum();
        int reserved = rows.stream()
                .mapToInt(AdmissionCycleCapacityResponse.Row::reservedSeats).sum();
        int open = rows.stream().mapToInt(AdmissionCycleCapacityResponse.Row::openSeats).sum();
        long committed = rows.stream()
                .mapToLong(AdmissionCycleCapacityResponse.Row::committed).sum();
        long free = open - committed;

        return new AdmissionCycleCapacityResponse.Row(
                null, null, total, reserved, open,
                rows.stream().mapToLong(AdmissionCycleCapacityResponse.Row::pending).sum(),
                rows.stream().mapToLong(AdmissionCycleCapacityResponse.Row::approved).sum(),
                rows.stream().mapToLong(AdmissionCycleCapacityResponse.Row::waitlisted).sum(),
                rows.stream().mapToLong(AdmissionCycleCapacityResponse.Row::offered).sum(),
                rows.stream().mapToLong(AdmissionCycleCapacityResponse.Row::accepted).sum(),
                rows.stream().mapToLong(AdmissionCycleCapacityResponse.Row::enrolled).sum(),
                rows.stream().mapToLong(AdmissionCycleCapacityResponse.Row::rejected).sum(),
                rows.stream().mapToLong(AdmissionCycleCapacityResponse.Row::withdrawn).sum(),
                committed, free, free < 0);
    }
}
