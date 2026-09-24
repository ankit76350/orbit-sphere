package com.orbitastra.backend.repositories.crm.admissionapplication;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.crm.admissionapplication.request.AdmissionApplicationSearchRequest;
import com.orbitastra.backend.models.crm.AdmissionApplication;

/** The search behind #24. Every filter is optional, so the query is built at runtime. */
public interface AdmissionApplicationRepositoryCustom {

    /**
     * One page of this school's applications.
     *
     * <p>The school id is passed in and never comes from the caller — it is the tenant boundary.
     */
    Page<AdmissionApplication> search(String schoolId, AdmissionApplicationSearchRequest request,
            Pageable pageable);

    /**
     * How many applications one round has, per class and per status. For #7.
     *
     * <p><b>ONE GROUPED AGGREGATION FOR THE WHOLE TABLE</b>, not one query per class — the same
     * N+1 {@code people} #15 names about {@code filledHeadcount}. A cycle with twenty classes is
     * one round trip, not twenty.
     *
     * <p><b>The counts are computed, never stored.</b> Keeping them on {@code AdmissionCycle}
     * would make it a high-contention document that every application write has to touch.
     *
     * <p>Returns only the pairs that exist. A class with no applications in a status has no row,
     * which the caller reads as zero.
     */
    List<ClassStatusCount> countByClassAndStatus(String schoolId, String admissionCycleDocsId);
}
