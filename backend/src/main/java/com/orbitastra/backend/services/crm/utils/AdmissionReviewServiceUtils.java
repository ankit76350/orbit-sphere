package com.orbitastra.backend.services.crm.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.AdmissionApplication;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.crm.admissionapplication.AdmissionApplicationRepository;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

/**
 * The reads {@link com.orbitastra.backend.services.crm.AdmissionReviewService} makes more than
 * once.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and <b>a method here
 * never calls another method here</b>. Only the service calls these.
 *
 * <p><b>This file did not exist while the service had one endpoint</b>, and that was right —
 * nothing in a single method can repeat. #27 and #28 gave it three, and the two things they share
 * are these: turning reviewer ids into names, and turning application ids into the forms they name.
 *
 * <p><b>Both are the BULK form, even where one row is being answered.</b> #27 resolves one
 * reviewer and one application; #28 resolves a page of each. Writing the single-id version as well
 * would leave two ways to do the same lookup, and the one-at-a-time way is the N+1 this project
 * keeps naming — the shape most likely to be copied into a loop by whoever comes next. For one id
 * the bulk call is the same single query, so nothing is paid for the safety.
 *
 * <p><b>What did NOT move:</b> {@code nextStepFor} and {@code names} have one caller each and stay
 * inline under their {@code //! step N}, where they read in the order they happen. So do the two
 * lookups that <i>throw</i> — #26 refuses an unknown reviewer with {@code STAFF_NOT_FOUND} and an
 * unknown form with {@code APPLICATION_NOT_FOUND}, and those are one-caller guards rather than
 * shared reads. A tolerant lookup and a throwing one look alike and are not: folding them together
 * would mean a flag that decides whether an endpoint refuses.
 */
@Component
@RequiredArgsConstructor
public class AdmissionReviewServiceUtils {

    private final StaffRepository staff;
    private final AdmissionApplicationRepository applications;

    /**
     * Reviewer ids to names, for as many as are asked about.
     *
     * <p><b>TOLERANT.</b> A reviewer who has left the school is simply absent from the map, and
     * the caller shows the id with no name — which is the honest answer. A review assessed by
     * somebody the school no longer employs is still a review that happened, and inventing a name
     * or dropping the row would hide that.
     *
     * <p><b>Nothing to look up is not a query.</b> An empty page is the common case on a filter
     * that matches nothing.
     *
     * <p>A merge function is needed even though ids are unique: {@code toMap} throws on a
     * duplicate key rather than keeping either.
     *
     * Used by:
     * - recordResult()
     * - listReviews()
     */
    public Map<String, String> reviewerNamesFor(School school, Collection<String> staffDocsIds) {
        List<String> wanted = usable(staffDocsIds);
        if (wanted.isEmpty()) {
            return Map.of();
        }

        // TODO: read staff
        return staff.findBySchoolIdAndIdIn(school.getId(), wanted).stream()
                .collect(Collectors.toMap(Staff::getId, Staff::getFullName,
                        (first, second) -> first));
    }

    /**
     * Application ids to the forms they name, for as many as are asked about.
     *
     * <p>Returns the whole document rather than one field, because the callers want different
     * parts of it: #27 wants the number, #28 wants the number <i>and</i> the applicant's name.
     *
     * <p><b>TOLERANT, for the same reason as the names above.</b> A review whose form is gone is a
     * broken record; the caller reports it by leaving the number off rather than by refusing to
     * answer at all.
     *
     * Used by:
     * - recordResult()
     * - listReviews()
     */
    public Map<String, AdmissionApplication> applicationsById(School school,
            Collection<String> admissionApplicationDocsIds) {

        List<String> wanted = usable(admissionApplicationDocsIds);
        if (wanted.isEmpty()) {
            return Map.of();
        }

        // TODO: read admission applications
        return applications.findBySchoolIdAndIdIn(school.getId(), wanted).stream()
                .collect(Collectors.toMap(AdmissionApplication::getId, form -> form,
                        (first, second) -> first));
    }

    /**
     * The ids worth asking the database about — no nulls, no blanks, no repeats.
     *
     * <p><b>Private, so it is not a second utils method calling a first.</b> The folder rule is
     * that a method here never calls another one here; that is about the module's shared surface,
     * and this is one file's own tidying rather than something a service could call.
     */
    private static List<String> usable(Collection<String> ids) {
        return ids == null ? List.of() : ids.stream()
                .filter(each -> each != null && !each.isBlank())
                .distinct()
                .toList();
    }
}
