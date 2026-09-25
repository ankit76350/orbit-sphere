package com.orbitastra.backend.services.crm.utils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.crm.AdmissionCycle;
import com.orbitastra.backend.models.crm.embedded.IntakeCapacity;
import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.crm.admissioncycle.AdmissionCycleRepository;

import lombok.RequiredArgsConstructor;

/**
 * The reads and the wording {@link com.orbitastra.backend.services.crm.AdmissionApplicationService}
 * does more than once.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and <b>a method here
 * never calls another method here</b>. Only the service calls these.
 *
 * <p><b>Everything in this file had two or more callers before it moved.</b> That is the whole
 * entry requirement — single-use logic stays inline under its {@code //! step N}, where it can be
 * read in the order it happens. {@code datesRunForwards} on the cycle service and
 * {@code describeWhyNothingIsAllowed} on this one both have one caller and both stayed put.
 */
@Component
@RequiredArgsConstructor
public class AdmissionApplicationServiceUtils {

    private final AdmissionApplicationRepository applications;
    private final AdmissionCycleRepository admissionCycles;
    private final SchoolClassRepository schoolClasses;

    /**
     * One application of this school, or a 404.
     *
     * <p><b>Scoped by school in the query, never checked afterwards.</b> An id from another school
     * is a real id: finding it first and testing the school after would already have read a
     * child's date of birth and their guardians' phone numbers, and a "not found" that depends on
     * remembering to check is one refactor from a leak.
     *
     * <p><b>The id is trimmed here rather than at each caller</b>, so a trailing space in a URL is
     * a 404 naming the id rather than a mismatch nobody can see. A null reads as "" and finds
     * nothing, which is the same answer without a {@code NullPointerException} on the way.
     *
     * Used by:
     * - getApplication()
     * - submitApplication()
     * - decide()
     */
    public AdmissionApplication loadApplication(School school, String admissionApplicationId) {
        String id = admissionApplicationId == null ? "" : admissionApplicationId.trim();

        // TODO: read admission application
        return applications.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("APPLICATION_NOT_FOUND",
                        "No admission application with id '" + id + "' in this school."));
    }

    /**
     * The round an application went into, <b>or nothing</b>.
     *
     * <p><b>This is the tolerant read, and it is not {@code CrmHelper.loadCycle}.</b> That one
     * throws, which is right for the endpoints that are <i>about</i> a cycle — but these callers
     * were asked for an application, and answering "no admission cycle found" to that is a
     * confusing 404 for a form that reads perfectly well. An application whose round is gone is a
     * broken record; the callers report it by leaving the name off rather than by refusing, the
     * same call #6 makes about a seat row naming a deleted class.
     *
     * <p>Returns the whole cycle rather than one field because the callers want different parts of
     * it: #25 wants the name and the year, #20 wants only the year.
     *
     * Used by:
     * - getApplication()
     * - decide()
     */
    public Optional<AdmissionCycle> loadCycleOrEmpty(School school, String admissionCycleDocsId) {
        // TODO: read admission cycle
        return admissionCycles.findByIdAndSchoolId(admissionCycleDocsId, school.getId());
    }

    /**
     * What a class is called, or null.
     *
     * <p><b>The year is a parameter rather than read here</b>, because classes are stored per
     * academic year and the year belongs to the application's cycle — which the caller has already
     * loaded. Passing it in keeps this to one query.
     *
     * <p><b>A null year gives a null name, without asking the database anything.</b> That is the
     * case where the cycle itself is gone: with no year there is nothing to scope the lookup by,
     * and guessing the school's current year would name a class from the wrong one.
     *
     * <p><b>Null rather than a guess when the class is missing too.</b> A form for a class the
     * school no longer has is a real problem, and inventing a label would hide it.
     *
     * Used by:
     * - submitApplication()
     * - decide()
     */
    public String classNameOrNull(School school, String classDocsId, String academicYear) {
        if (academicYear == null || classDocsId == null || classDocsId.isBlank()) {
            return null;
        }

        // TODO: read school class
        return schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(classDocsId, school.getId(), academicYear)
                .map(SchoolClass::getName)
                .orElse(null);
    }

    /**
     * What can be done to this application next, in plain words.
     *
     * <p><b>It names the endpoint AND says whether it exists.</b> Most of these are not built, and
     * an answer that said "submit it" without saying nothing can would send somebody looking for a
     * route that 404s.
     *
     * <p>No database call — it reads the status and nothing else — but it moved here with the
     * reads because two endpoints say it and they must not drift apart.
     *
     * Used by:
     * - getApplication()
     * - decide()
     */
    public String nextStepFor(AdmissionApplication application) {
        return switch (application.getStatus()) {
            case DRAFT -> "It is still a draft, so the family can keep editing it. #19 submits it "
                    + "and is not built, so nothing can move it on yet.";
            //! SAYS THE FORM IS FROZEN, which #19 is the moment of. It was left out until the
            //! #19 suite asked #25 what a submitted form says and got an answer that never
            //! mentioned the one thing that changed — a reader would not learn that #18 now
            //! refuses until they tried it.
            case SUBMITTED -> "It has been submitted, so the form is frozen — #18 refuses to edit "
                    + "it from here. It is waiting: #26 puts it on somebody's desk, or #20 "
                    + "decides it outright — a school does not have to review before it decides.";
            case UNDER_REVIEW -> "Somebody is reviewing it. #27 records what they found and is "
                    + "not built; #20 is what records the school's decision.";
            case ADDITIONAL_INFORMATION_REQUIRED -> "The school asked the family for something "
                    + "more. When it arrives, #20 moving it back to UNDER_REVIEW carries on — "
                    + "or it can be decided there and then.";
            case WAITLISTED -> "It was neither approved nor rejected — the school is holding it "
                    + "for a seat. #20 moving it to APPROVED is what takes it off the list when "
                    + "one comes free.";
            case APPROVED -> "It has been approved, so an offer can be issued. #29 issues one and "
                    + "is not built. #20 will not decide it again — changing your mind means "
                    + "withdrawing the offer, which is #31.";
            case REJECTED -> "The school decided against it, and the reason is on the form. "
                    + "Nothing moves from here.";
            case WITHDRAWN -> "The family pulled out. Nothing moves from here.";
            case OFFERED -> "An offer is out with the family. #30 records their answer and is not "
                    + "built.";
            case OFFER_ACCEPTED -> "The family accepted. #33 turns the applicant into a student "
                    + "and is not built — this is where the module runs out of road.";
            case ENROLLED -> "The child is a student now, and this application is history.";
        };
    }

    /**
     * The class a family may apply for in this round, or a refusal.
     *
     * <p><b>Two questions, and both have to be asked.</b> Is it a class of the <i>cycle's</i>
     * academic year — the only year that round admits into — and does the round have seats set up
     * for it. #3 refuses to open a cycle with an empty table, but a table can list some classes and
     * not others, and applying for a class with no seats is an application that could never be
     * offered anything.
     *
     * <p><b>The offer side asks the same two questions and has its own copy</b>, in
     * {@code AdmissionOfferServiceUtils.offerableClass}. That is the folder rule rather than an
     * oversight — a main service uses its own {@code utils} — and the two are not quite the same
     * anyway: this one refuses with `CLASS_NOT_IN_CYCLE_YEAR` because the family <i>applied</i> for
     * it, that one with `CLASS_NOT_FOUND` because the school <i>offered</i> it. If a third caller
     * ever appears, the shared {@code helper} is where it should go.
     *
     * Used by:
     * - createApplication()
     * - updateApplication()
     */
    public SchoolClass applicableClass(School school, AdmissionCycle cycle, String classDocsId) {
        String classId = classDocsId == null ? "" : classDocsId.trim();

        // TODO: read school class
        SchoolClass applied = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(classId, school.getId(),
                        cycle.getAcademicYear())
                .orElseThrow(() -> ApiException.conflict("CLASS_NOT_IN_CYCLE_YEAR",
                        "Class '" + classId + "' is not a class of '" + cycle.getAcademicYear()
                                + "', which is the year this cycle admits into."));

        List<IntakeCapacity> seats = cycle.getCapacities() == null
                ? List.of()
                : cycle.getCapacities();

        if (seats.stream().noneMatch(seat -> classId.equals(seat.getClassDocsId()))) {
            throw ApiException.conflict("CLASS_NOT_IN_CAPACITY",
                    "'" + cycle.getName() + "' has no seats set up for " + applied.getName()
                            + ". A class that is not in the seat table cannot be applied for — "
                            + "add it with the seat table endpoint first.");
        }

        return applied;
    }

    /**
     * Why a status can take no decision at all, in words rather than an empty list.
     *
     * <p>Inline as a private method rather than in the helper: used by one endpoint, and the folder
     * rules keep single-use logic where it is used.
     *
     * <p><b>"Nothing is allowed" is the answer somebody will hit most often by mistake</b>, and a
     * refusal that just said so would leave them guessing whether the endpoint or the form was
     * wrong.
     */
    public static String describeWhyNothingIsAllowed(AdmissionApplicationStatus status) {
        return switch (status) {
            case DRAFT -> "The family has not submitted it yet — #19 is what sends it, and there "
                    + "is nothing to decide until they do.";
            case APPROVED -> "It is already approved, so the next step is an offer (#29) rather "
                    + "than another decision. Changing your mind is withdrawing the offer (#31).";
            case REJECTED -> "It has been refused, and nothing moves from there.";
            case WITHDRAWN -> "The family pulled out, and nothing moves from there.";
            case OFFERED -> "An offer is out with the family — #30 records their answer.";
            case OFFER_ACCEPTED -> "They accepted. #33 turns the applicant into a student.";
            case ENROLLED -> "The child is a student now, and this application is history.";
            default -> "Nothing can be decided from there.";
        };
    }

    /** The reachable statuses as a sentence, so a refusal can list them. Used by: decide(). */
    public static String names(Set<AdmissionApplicationStatus> allowed) {
        return allowed.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    }
}
