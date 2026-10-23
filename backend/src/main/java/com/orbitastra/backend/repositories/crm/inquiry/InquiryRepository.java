package com.orbitastra.backend.repositories.crm.inquiry;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.crm.Inquiry;

/**
 * Reads and writes for the {@code inquiries} collection.
 *
 * <p><b>It existed before anything could create a row.</b> #17 has to read a lead when an
 * application names one, and move its status — so this was written for that, and for a while the
 * only rows here were ones put in directly. #8 closed that on 2026-09-24.
 *
 * <p><b>The custom half is #13's</b>: five optional filters, a two-condition "overdue" question and
 * a two-field search do not fit a derived method name.
 */
public interface InquiryRepository extends MongoRepository<Inquiry, String>,
        InquiryRepositoryCustom {

    /**
     * One inquiry, scoped by school in the query.
     *
     * <p>Never by id alone: an id from another school is a real id, and looking it up without the
     * school would find it and let an application quote another tenant's lead.
     */
    Optional<Inquiry> findByIdAndSchoolId(String id, String schoolId);
}
