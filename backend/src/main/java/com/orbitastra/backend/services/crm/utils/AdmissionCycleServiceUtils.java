package com.orbitastra.backend.services.crm.utils;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.time.Dates;
import com.orbitastra.backend.dto.crm.admissioncycle.response.AdmissionCycleCapacityResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.common.enums.SchoolTimeZone;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.embedded.AdmissionFormQuestion;
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
     * The two dates, in the order they must run, with what to call each one in a message.
     *
     * <p>One list because #1 and #2 both check the same ordering, and two copies of it would be
     * two chances for the order to disagree with itself.
     */
    public static final List<String> DATE_FIELDS = List.of(
            "applicationOpenAt", "applicationCloseAt");

    /**
     * What to call each of those two in a refusal, in the same order.
     *
     * <p><b>Beside DATE_FIELDS so the two cannot drift apart.</b> They are read by index against
     * each other, and a name list that fell one out of step would blame the wrong date in every
     * message the endpoint sends.
     *
     * <p>There is no list of dates that may fall <b>before</b> the academic year, and there was
     * one until 2026-09-28. All four may: a school plans a round in the months running up to the
     * year it admits for, and its closing date and enrolment deadline can fall there too. The only
     * rule left is that nothing may be after the year ends, which AcademicYearWindow now applies
     * to every field without being told which.
     */
    public static final List<String> DATE_NAMES = List.of(
            "applications open", "applications close");

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
     * <p>The same rule #1 and #2 enforce, asked of a state that has not been saved yet — by a
     * caller that wants to <b>decide</b> rather than to refuse. #3 fills an absent date as a cycle
     * moves and must not move a status just because the moment would read badly, so it asks and
     * then records nothing.
     *
     * <p>Used by: moveStatus().
     */
    public static boolean datesRunForwards(Map<String, Instant> dates) {
        return outOfOrderAt(dates) < 0;
    }

    /**
     * Refuses a set of dates that does not run forwards.
     *
     * <p>The same question as above with the refusal attached, for the callers that are
     * <b>validating</b>. #2 checks the stored dates merged with the sent ones, and #3 checks the
     * calendar a round would end up with when opening it stamps today onto the opening two.
     *
     * <p><b>Both go through this rather than each writing the loop</b>, because the message names
     * the two fields that clash and two copies of that wording is two chances for one endpoint to
     * describe the rule differently from the other.
     *
     * <p>Used by:
     * - updateCycle()
     * - moveStatus()
     *
     * @throws ApiException {@code 400 CYCLE_DATES_OUT_OF_ORDER}
     */
    public static void requireDatesRunForwards(Map<String, Instant> dates, SchoolTimeZone zone) {
        int at = outOfOrderAt(dates);
        if (at < 0) {
            return;
        }

        //! THE ONE BEFORE IT THAT IS ACTUALLY SET. The offending date clashes with the nearest
        //! earlier field that has a value, which is not always the one immediately before it.
        int earlier = at - 1;
        while (earlier >= 0 && dates.get(DATE_FIELDS.get(earlier)) == null) {
            earlier--;
        }

        throw ApiException.badRequest("CYCLE_DATES_OUT_OF_ORDER",
                "That would leave the dates in the wrong order: " + DATE_NAMES.get(at) + " is "
                        + Dates.readable(dates.get(DATE_FIELDS.get(at)), zone) + ", which is before "
                        + DATE_NAMES.get(earlier) + " at "
                        + Dates.readable(dates.get(DATE_FIELDS.get(earlier)), zone) + ".");
    }

    /**
     * The index in {@code DATE_FIELDS} of the first date that falls before one set earlier, or
     * {@code -1}.
     *
     * <p><b>Private, so this is not one public helper calling another.</b> The two methods above
     * are one question asked two ways, and the arithmetic lives here once so that the boolean and
     * the refusal can never disagree about what "forwards" means.
     */
    private static int outOfOrderAt(Map<String, Instant> dates) {
        Instant earlier = null;
        for (int i = 0; i < DATE_FIELDS.size(); i++) {
            Instant when = dates.get(DATE_FIELDS.get(i));
            if (when == null) {
                continue;
            }
            if (earlier != null && when.isBefore(earlier)) {
                return i;
            }
            earlier = when;
        }
        return -1;
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
                    + "offered and enrolled. Move it back to OPEN to take more — give it an "
                    + "applicationCloseAt in the future with #2 first, or reopening is refused "
                    + "for closing before it opened.";
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

    /**
     * Are these two question lists the same thing — same questions, same wording, same required
     * flags, same order?
     *
     * <p><b>Order counts.</b> The order of the list is the order the questions are asked in, so a
     * request that only reorders them is a real change and has to be saved rather than answered
     * with NOTHING_TO_UPDATE.
     *
     * <p><b>The id counts too.</b> Two questions reading the same words are not the same question
     * if they have different ids: answers are stored under the id, so which id a wording lives on
     * is a fact about the round.
     *
     * <p>Used by: updateCycle().
     */
    public static boolean sameQuestions(List<AdmissionFormQuestion> before,
            List<AdmissionFormQuestion> after) {

        List<AdmissionFormQuestion> left = before == null ? List.of() : before;
        List<AdmissionFormQuestion> right = after == null ? List.of() : after;

        if (left.size() != right.size()) {
            return false;
        }

        for (int i = 0; i < left.size(); i++) {
            AdmissionFormQuestion one = left.get(i);
            AdmissionFormQuestion two = right.get(i);

            //! required is compared as a plain true/false. A question saved before the field
            //! existed reads back null, and null and false mean the same thing to a family.
            if (!Objects.equals(one.getId(), two.getId())
                    || !Objects.equals(one.getQuestion(), two.getQuestion())
                    || Boolean.TRUE.equals(one.getRequired())
                            != Boolean.TRUE.equals(two.getRequired())) {
                return false;
            }
        }
        return true;
    }
}
