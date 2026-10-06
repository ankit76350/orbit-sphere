package com.orbitastra.backend.repositories.crm.admissionapplication;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.crm.AdmissionApplication;

/**
 * Reads and writes for the {@code admission_applications} collection.
 *
 * <p>Only what #16, #17 and #24 need so far. #24's search is in
 * {@link AdmissionApplicationRepositoryCustom}: every filter on it is optional, so the query has
 * to be built at runtime rather than declared as a method name.
 */
public interface AdmissionApplicationRepository
        extends MongoRepository<AdmissionApplication, String>,
                AdmissionApplicationRepositoryCustom {

    /**
     * Has this inquiry already produced an application in this cycle?
     *
     * <p>The database also stops it with {@code school_cycle_inquiry_uniq}, which is unique on
     * {@code {schoolId, admissionCycleDocsId, inquiryDocsId}} and partial on the inquiry existing.
     * We ask first so the caller gets a message that says what to do, rather than a duplicate key
     * error the handler turns into a 500.
     */
    /**
     * One application, for #25.
     *
     * <p><b>Scoped by schoolId in the query, never by id alone.</b> An id from another school is a
     * real id: looking it up without the school would find it and hand over a child's date of
     * birth, their guardians' phone numbers and everything the family declared. This is the
     * difference between "not found" and a tenant leak, and it is one argument.
     */
    Optional<AdmissionApplication> findByIdAndSchoolId(String id, String schoolId);

    /**
     * Several applications at once, for a page of reviews. For #28.
     *
     * <p><b>ONE QUERY FOR A WHOLE PAGE, not one per row.</b> A reviewer's queue that showed raw
     * application ids would be unreadable, and reading each form separately is the N+1 this project
     * keeps naming.
     *
     * <p>Scoped by school like everything else here.
     */
    List<AdmissionApplication> findBySchoolIdAndIdIn(String schoolId, Collection<String> ids);

    boolean existsBySchoolIdAndAdmissionCycleDocsIdAndInquiryDocsId(
            String schoolId, String admissionCycleDocsId, String inquiryDocsId);

    /**
     * Every application one lead turned into, newest first. For #16.
     *
     * <p><b>A list, not a page.</b> {@code school_cycle_inquiry_uniq} allows one application per
     * lead per admission round, so the count here is the number of rounds the family applied in —
     * one, usually, and a handful at the very most. Paging a handful is machinery for nothing.
     *
     * <p><b>{@code school_inquiry_idx} exists for this one query</b>, and it had to be added:
     * {@code school_cycle_inquiry_uniq} is keyed {@code schoolId, admissionCycleDocsId,
     * inquiryDocsId}, and asking it about a lead <i>without</i> naming a cycle skips its middle
     * key, which leaves the whole school's applications to scan. It is <b>not</b> a partial index,
     * for a measured reason — see the model.
     *
     * <p><b>The {@code OrderBy} is not a sort.</b> The index ends in {@code createdAt: -1}, so the
     * newest-first order is read straight off it; an explain shows an {@code IXSCAN} with no sort
     * stage.
     *
     * <p>Scoped by school in the query like everything else here.
     */
    List<AdmissionApplication> findBySchoolIdAndInquiryDocsIdOrderByCreatedAtDesc(
            String schoolId, String inquiryDocsId);
}
