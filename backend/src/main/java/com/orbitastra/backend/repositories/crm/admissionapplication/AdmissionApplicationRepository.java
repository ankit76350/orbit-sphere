package com.orbitastra.backend.repositories.crm.admissionapplication;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.crm.AdmissionApplication;

/**
 * Reads and writes for the {@code admission_applications} collection.
 *
 * <p>Only what #17 and #24 need so far. #24's search is in
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

    boolean existsBySchoolIdAndAdmissionCycleDocsIdAndInquiryDocsId(
            String schoolId, String admissionCycleDocsId, String inquiryDocsId);
}
