package com.orbitastra.backend.repositories.crm.admissionreview;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.crm.AdmissionReview;
import com.orbitastra.backend.models.crm.enums.AdmissionReviewStatus;

/**
 * Reads and writes for the {@code admission_reviews} collection.
 *
 * <p><b>Nothing creates a review yet.</b> #26 assigns a reviewer and #27 records the result;
 * neither is built. This repository exists because #25 reads an application in full, and a review
 * is part of "in full" — so the read is written now and returns an empty list until those two
 * endpoints put rows in.
 *
 * <p>That is not a placeholder: the query runs, the scoping is real, and the day #26 writes its
 * first row #25 shows it without another change.
 */
public interface AdmissionReviewRepository
        extends MongoRepository<AdmissionReview, String>, AdmissionReviewRepositoryCustom {

    /**
     * Every review of one application, oldest round first. For #25.
     *
     * <p><b>Scoped by school in the query, never by the application id alone.</b> An application
     * id from another school is a real id, and asking only for reviews of that id would hand over
     * another tenant's reviewer names, scores and notes.
     *
     * <p><b>Ordered by round and then by when it was created.</b> A round can hold more than one
     * review — two people looking at the same application is the normal case for an interview
     * plus a test — and {@code school_application_round_reviewer_uniq} lets it. Round alone is
     * therefore not a total order, and without the second field two reviews of one round come back
     * in whatever order the database happened to hold them.
     */
    List<AdmissionReview> findBySchoolIdAndAdmissionApplicationDocsIdOrderByReviewRoundAscCreatedAtAsc(
            String schoolId, String admissionApplicationDocsId);

    /**
     * The reviews of one application that are in any of these statuses, oldest round first.
     * For #20's approval check.
     *
     * <p><b>Asked with {@code PENDING, IN_PROGRESS}</b>, which is "somebody is still assessing
     * this". #20 will not move a form to {@code APPROVED} while that list is non-empty.
     *
     * <p><b>It returns the rows rather than counting them</b>, because the refusal names the
     * rounds that are still open. "Two reviews are outstanding" sends somebody hunting; "round 1
     * is PENDING and round 2 is IN_PROGRESS" tells them what to chase.
     *
     * <p><b>Scoped by school in the query</b>, like every read in this module — and the status is
     * in the query rather than filtered afterwards, so a form with twenty finished reviews carries
     * none of them back to answer a question about the two that are not.
     *
     * <p>Same ordering as the finder above, and for the same reason: a round can hold more than
     * one review, so round alone is not a total order.
     */
    List<AdmissionReview>
            findBySchoolIdAndAdmissionApplicationDocsIdAndStatusInOrderByReviewRoundAscCreatedAtAsc(
                    String schoolId, String admissionApplicationDocsId,
                    Collection<AdmissionReviewStatus> statuses);

    /**
     * One review, for #27.
     *
     * <p><b>Scoped by schoolId in the query, never by id alone.</b> An id from another school is a
     * real id: a score written against it would be this school's mark on another school's child.
     */
    Optional<AdmissionReview> findByIdAndSchoolId(String id, String schoolId);
}
