package com.orbitastra.backend.repositories.crm.inquiry;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.crm.Inquiry;

/**
 * Reads and writes for the {@code inquiries} collection.
 *
 * <p><b>Nothing creates an inquiry yet.</b> #8 is the endpoint that captures a lead and it is not
 * built, so today the only rows here are ones put in directly. This repository exists because
 * [#17] has to read one when an application names it, and move its status.
 */
public interface InquiryRepository extends MongoRepository<Inquiry, String> {

    /**
     * One inquiry, scoped by school in the query.
     *
     * <p>Never by id alone: an id from another school is a real id, and looking it up without the
     * school would find it and let an application quote another tenant's lead.
     */
    Optional<Inquiry> findByIdAndSchoolId(String id, String schoolId);
}
