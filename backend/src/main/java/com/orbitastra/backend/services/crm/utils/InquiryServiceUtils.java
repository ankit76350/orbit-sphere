package com.orbitastra.backend.services.crm.utils;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.dto.crm.inquiry.response.InquiryDetailResponse;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryFollowUp;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.crm.inquiry.InquiryRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

/**
 * Everything {@link com.orbitastra.backend.services.crm.InquiryService} does that is not an
 * endpoint.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, <b>a service file
 * holds nothing but its endpoint methods</b>, and <b>a method here never calls another method
 * here</b>. Only the service calls these.
 *
 * <p><b>It held one method until 2026-09-24 and now holds eight</b>, because the rule changed
 * rather than because the code did. Seven private helpers were sitting in the service — three of
 * them pure sentences about a document, which an earlier reading of the rules said should stay
 * inline. The rule now counts <i>what kind of thing a method is</i> rather than how many callers
 * it has, and a service file that is six endpoints and nothing else is easier to read than one
 * where the endpoints are separated by the helpers they happen to use.
 *
 * <p><b>The flat rule is what shapes {@code detailOf}.</b> It needs an {@code overdue} flag and a
 * {@code nextStep} sentence, and both have methods in this file — so it takes them as parameters
 * and the service asks for them, exactly as {@code AdmissionOfferServiceUtils.answerFor} does. A
 * {@code utils} whose methods called each other would be a second service with no endpoints.
 *
 * <p><b>The two transition constants live here, not in the service</b>, because the only things
 * that read them are {@code allowedNext} and {@code overdueNow}. {@code NOT_BY_HAND} stayed behind
 * for the mirror reason: #10 and #12 read it directly, and nothing here does.
 */
@Component
@RequiredArgsConstructor
public class InquiryServiceUtils {

    /**
     * <b>Where a lead may go next. This table is the product rule</b>, the same way
     * {@code CYCLE_MOVES}, {@code DECISION_MOVES} and {@code REVIEW_MOVES} are in this module.
     *
     * <p>It is the graph in {@code controllers/crm/README.md}, written out. <b>Both terminal
     * statuses are spelled out with an empty set</b> rather than left off the map: "nothing follows
     * LOST" is a decision, and a missing key would be a gap that reads the same as a forgotten one.
     *
     * <p><b>{@code LOST} is reachable from every non-terminal status</b>, which is what makes it
     * worth writing this out — a family can stop answering at any point.
     *
     * <p><b>THE EARLY HALF SKIPS FORWARD, and that is a product decision rather than a loose
     * table.</b> {@code VISIT_SCHEDULED} is reachable from {@code NEW}, {@code CONTACTED} and
     * {@code COUNSELLING}; {@code VISITED} from all three of those <i>and</i> from
     * {@code VISIT_SCHEDULED}. Two real things happen that a strict chain would refuse: <b>a family
     * walks in</b> — they visited, and nobody scheduled anything — and <b>a family books a visit on
     * the first call</b>, with no separate counselling step. Forcing either through the full chain
     * would mean logging calls that never happened.
     *
     * <p><b>{@code APPLICATION_STARTED} is reachable from every pre-application status</b>, for a
     * blunt reason: <b>#17 does not consult this table.</b> It sets the status unconditionally once
     * a form naming the lead is saved, from wherever the lead happened to be.
     *
     * <p><b>What it still refuses is going BACKWARDS.</b> A lead that has visited cannot return to
     * {@code NEW}, and {@code COUNSELLING} still follows contact rather than standing in for it.
     *
     * <p><b>What this table permits is not all #10 and #12 permit.</b> {@code APPLICATION_STARTED}
     * and {@code APPLICATION_SUBMITTED} are on it as legal <i>moves</i> and are refused by both
     * endpoints, because #17 and #19 own them. Keeping the two apart is what stops the table lying
     * about the product.
     *
     * <p>Used by {@code allowedNext()}.
     */
    private static final Map<InquiryStatus, Set<InquiryStatus>> LEAD_MOVES = Map.of(
            InquiryStatus.NEW, EnumSet.of(InquiryStatus.CONTACTED,
                    InquiryStatus.VISIT_SCHEDULED, InquiryStatus.VISITED,
                    InquiryStatus.APPLICATION_STARTED, InquiryStatus.LOST),
            InquiryStatus.CONTACTED, EnumSet.of(InquiryStatus.COUNSELLING,
                    InquiryStatus.VISIT_SCHEDULED, InquiryStatus.VISITED,
                    InquiryStatus.APPLICATION_STARTED, InquiryStatus.LOST),
            InquiryStatus.COUNSELLING, EnumSet.of(InquiryStatus.VISIT_SCHEDULED,
                    InquiryStatus.VISITED,
                    InquiryStatus.APPLICATION_STARTED, InquiryStatus.LOST),
            InquiryStatus.VISIT_SCHEDULED, EnumSet.of(InquiryStatus.VISITED,
                    InquiryStatus.APPLICATION_STARTED, InquiryStatus.LOST),
            InquiryStatus.VISITED, EnumSet.of(InquiryStatus.APPLICATION_STARTED,
                    InquiryStatus.LOST),
            InquiryStatus.APPLICATION_STARTED, EnumSet.of(InquiryStatus.APPLICATION_SUBMITTED,
                    InquiryStatus.LOST),
            //! LOST BELONGS HERE TOO. The rule is "any non-terminal reaches LOST", and
            //! APPLICATION_SUBMITTED is not terminal — CLOSED follows it.
            InquiryStatus.APPLICATION_SUBMITTED, EnumSet.of(InquiryStatus.CLOSED,
                    InquiryStatus.LOST),
            InquiryStatus.LOST, EnumSet.noneOf(InquiryStatus.class),
            InquiryStatus.CLOSED, EnumSet.noneOf(InquiryStatus.class));

    /**
     * The statuses a lead is finished in — the ones {@code overdue} ignores.
     *
     * <p>Mirrors {@code FINISHED} in the repository, which is where the <i>query</i> uses it. This
     * copy is for working the flag out on a row that has already been read: #13 reports
     * {@code overdue} on every row, not only the ones a filter asked for.
     *
     * <p>Used by {@code overdueNow()}.
     */
    private static final Set<InquiryStatus> FINISHED =
            EnumSet.of(InquiryStatus.LOST, InquiryStatus.CLOSED);

    private final InquiryRepository inquiries;
    private final SchoolClassRepository schoolClasses;
    private final StaffRepository staff;

    /**
     * One lead, or {@code 404 INQUIRY_NOT_FOUND}.
     *
     * <p><b>Scoped by school in the QUERY, never checked after the read.</b> An id from another
     * school is a real id; looking it up without the school would find it, and a check afterwards
     * is one {@code if} away from being forgotten. The difference is another tenant's family
     * details.
     *
     * <p><b>A 404 rather than a 403</b>, for the same reason it is everywhere else in this module:
     * a 403 would confirm that the lead exists.
     *
     * <p><b>It trims and tolerates null</b>, so a blank path segment is a 404 about an empty id
     * rather than a 500 about a null.
     *
     * Used by:
     * - updateInquiry()
     * - logFollowUp()
     * - moveStatus()
     * - getInquiry()
     */
    public Inquiry loadInquiry(School school, String inquiryId) {
        String id = inquiryId == null ? "" : inquiryId.trim();

        // TODO: read inquiry
        return inquiries.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND",
                        "No inquiry with id '" + id + "' in this school."));
    }

    /**
     * The class a lead names, checked against the year the lead is about.
     *
     * <p><b>Two callers, and the second is the interesting one</b> — #9 asks it both about a class
     * the caller just sent and about the one already on the document, when the <i>year</i> moved
     * underneath it. {@code alreadyStored} is what makes the refusal say which of those happened,
     * because "that class is not of that year" is useless advice when the caller never mentioned a
     * class.
     *
     * <p><b>Private and inline rather than in {@code utils}</b>: one endpoint calls it. #8 asks
     * the same question and does not use this, because #8 asks it unconditionally and in one
     * place, where it reads in the order it happens.
     *
     * Used by: updateInquiry().
     */
    public SchoolClass requireClassOfYear(School school, String classDocsId, String year,
            boolean alreadyStored) {

        // TODO: read school class
        return schoolClasses.findByIdAndSchoolIdAndAcademicYear(classDocsId, school.getId(), year)
                .orElseThrow(() -> ApiException.conflict("CLASS_NOT_IN_CYCLE_YEAR",
                        alreadyStored
                                ? "This lead is interested in class '" + classDocsId + "', which "
                                        + "is not a class of '" + year + "'. Send an "
                                        + "interestedClassDocsId of that year as well, or send "
                                        + "\"\" to clear it."
                                : "Class '" + classDocsId + "' is not a class of '" + year
                                        + "', which is the year this lead is about."));
    }

    /**
     * The name of the class a lead is interested in, or {@code null}.
     *
     * <p><b>TOLERANT, unlike {@code requireClassOfYear} above.</b> A class that was removed must
     * not stop a lead being read or corrected, and leaving the name off is the honest answer. The
     * two look alike and are not — one answers a question, the other enforces a rule — which is
     * why they are separate methods rather than one with a flag deciding whether to refuse.
     *
     * <p><b>{@code detailOf} does the same lookup inline rather than calling this</b>, and that is
     * the flat rule rather than an oversight: a method here may never call another one here. Two
     * copies of four lines is the price of a rule that keeps this file from becoming a second
     * service, and it is cheap.
     *
     * Used by:
     * - createInquiry()
     * - updateInquiry()
     */
    public String classNameOrNull(School school, Inquiry inquiry) {
        if (inquiry.getInterestedClassDocsId() == null) {
            return null;
        }

        // TODO: read school class
        return schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(inquiry.getInterestedClassDocsId(),
                        school.getId(), inquiry.getAcademicYear())
                .map(SchoolClass::getName)
                .orElse(null);
    }

    /**
     * The whole lead, named and answered — what a write hands back once it has finished writing.
     *
     * <p><b>{@code overdue} and {@code nextStep} are PARAMETERS, not worked out here.</b> Both
     * have methods in this very file — and a method here may never call another one here, which is
     * the folder rule that stops this becoming a second service. The caller asks for them and
     * passes them in, exactly as {@code AdmissionOfferServiceUtils.answerFor} takes its
     * {@code nextStep}.
     *
     * <p><b>#14 uses it too, and used not to.</b> It built the same response inline, with a
     * comment claiming the two were deliberately separate because one had read a lead and the
     * others had written to one. That distinction bought nothing: the response is the same
     * response, and the duplicate was two places for the same query to drift.
     *
     * Used by:
     * - logFollowUp()
     * - moveStatus()
     * - getInquiry()
     */
    public InquiryDetailResponse detailOf(School school, Inquiry saved, boolean overdue,
            String nextStep) {

        String interestedClassName = null;
        if (saved.getInterestedClassDocsId() != null) {
            // TODO: read school class
            interestedClassName = schoolClasses
                    .findByIdAndSchoolIdAndAcademicYear(saved.getInterestedClassDocsId(),
                            school.getId(), saved.getAcademicYear())
                    .map(SchoolClass::getName)
                    .orElse(null);
        }

        //! ONE STAFF QUERY FOR THE WHOLE LEAD: everybody who logged a follow-up, asked about
        //! together.
        List<String> staffIds = (saved.getFollowUps() == null ? List.<InquiryFollowUp>of()
                        : saved.getFollowUps()).stream()
                .map(InquiryFollowUp::getCounselorDocsId)
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();

        // TODO: read staff
        Map<String, String> staffNames = staffIds.isEmpty()
                ? Map.of()
                : staff.findBySchoolIdAndIdIn(school.getId(), staffIds).stream()
                        .collect(Collectors.toMap(Staff::getId, Staff::getFullName,
                                (first, second) -> first));

        return InquiryDetailResponse.fromInquiry(saved, interestedClassName, staffNames,
                overdue, nextStep);
    }

    /**
     * Where this lead may go next.
     *
     * <p><b>An unknown status is nowhere, not everywhere.</b> Every value of the enum is a key in
     * the table, so this cannot happen today — and if the enum grows a value and the table does
     * not, refusing every move is the failure that gets noticed rather than the one that lets
     * anything through.
     *
     * Used by: logFollowUp().
     */
    public static Set<InquiryStatus> allowedNext(InquiryStatus from) {
        return LEAD_MOVES.getOrDefault(from, Set.of());
    }

    /**
     * The reachable statuses as a sentence, so a refusal can list them.
     *
     * <p><b>"nothing" rather than an empty string</b> for a terminal lead. A refusal that trails
     * off with "it can go to: ." reads like a bug in the message; saying <i>nothing</i> is the
     * actual answer. The same call {@code AdmissionReviewServiceUtils.names} makes, and not shared
     * with it: that one is about reviews, and one sentence-builder over two unrelated enums would
     * be a generic helper nobody can read in place.
     *
     * <p><b>Sorted</b>, so the same set always reads the same way. {@code EnumSet} iterates in
     * declaration order, which would make the sentence depend on how the enum happens to be
     * written.
     *
     * Used by: logFollowUp().
     */
    public static String names(Set<InquiryStatus> allowed) {
        return allowed.isEmpty() ? "nothing"
                : allowed.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    }

    /**
     * Is this lead past its follow-up date and still worth chasing.
     *
     * <p><b>The same two conditions the query uses</b>, asked of a row already read. #13 reports
     * the flag on <i>every</i> row, not only the ones a filter asked for — a caller listing
     * everything still wants to see which are late, and making them compare a timestamp themselves
     * is how two screens end up disagreeing about what "overdue" means.
     *
     * Used by:
     * - listInquiries()
     * - getInquiry()
     */
    public static boolean overdueNow(Inquiry inquiry) {
        return inquiry.getNextFollowUpAt() != null
                && inquiry.getNextFollowUpAt().isBefore(Instant.now())
                && !FINISHED.contains(inquiry.getStatus());
    }

    /**
     * The number to ring, for a worklist row.
     *
     * <p><b>The primary guardian's, or the first one with a number.</b> A row that showed nothing
     * because the first guardian happened to have no phone would be a row nobody can use — and a
     * lead may carry a guardian with a number and no name at all, which is exactly what #8 is
     * built to accept.
     *
     * Used by: listInquiries().
     */
    public static String contactNumberOf(Inquiry inquiry) {
        if (inquiry.getGuardians() == null) {
            return null;
        }

        return inquiry.getGuardians().stream()
                .filter(one -> one.getPhoneNumber() != null && !one.getPhoneNumber().isBlank())
                .sorted(Comparator.comparing(
                        one -> !Boolean.TRUE.equals(one.getPrimaryContact())))
                .map(InquiryGuardian::getPhoneNumber)
                .findFirst()
                .orElse(null);
    }

    /**
     * What can be done with this lead next, in plain words.
     *
     * <p><b>A switch over the status, not a sentence about one.</b> It said the same thing about
     * every lead until 2026-09-24 — that #10 and #12 were not built and nothing could move it —
     * and both had been built for a day by then. A hand-written sentence about what exists is the
     * claim that rots fastest, which is why this one is now keyed on the thing that changes.
     *
     * <p><b>It names the endpoints by number</b>, deliberately: the answer is read by somebody
     * holding this module's README, and "#12 gives up on it" is a thing they can go and do.
     *
     * Used by:
     * - createInquiry()
     * - updateInquiry()
     * - getInquiry()
     * - logFollowUp()
     * - moveStatus()
     */
    public static String nextStepFor(Inquiry inquiry) {
        return switch (inquiry.getStatus()) {
            case NEW -> "Nobody has spoken to the family yet. #10 logs the first call and moves it "
                    + "to CONTACTED, or straight to VISIT_SCHEDULED or VISITED if they walked in. "
                    + "#12 moves it without a call.";
            case CONTACTED -> "Somebody has reached them. #10 logs the next call; #12 moves it on "
                    + "its own, and is the only thing that may give up on it.";
            case COUNSELLING -> "A counsellor is talking them through it. The next steps are a "
                    + "visit — #10 or #12 — or a form, which #17 starts.";
            case VISIT_SCHEDULED -> "A visit is booked. #10 logs how it went and moves it to "
                    + "VISITED.";
            case VISITED -> "They have seen the school. What happens now is a form: #17 starts "
                    + "one naming this lead and moves it to APPLICATION_STARTED by itself.";
            case APPLICATION_STARTED -> "A form exists. #19 moves this to APPLICATION_SUBMITTED "
                    + "when the family sends it — nothing here may set that by hand.";
            case APPLICATION_SUBMITTED -> "The form is in, and the application half takes over. "
                    + "#12 closes the file when admissions is done with it.";
            case LOST -> "The family went elsewhere, and the reason is on the record. Nothing "
                    + "moves it now — but #10 can still log a call, because somebody ringing back "
                    + "a family that gave up is exactly the call worth recording.";
            case CLOSED -> "The file is closed. Nothing moves it, and #10 can still log against "
                    + "it.";
        };
    }
}
