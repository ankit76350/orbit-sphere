package com.orbitastra.backend.dto.student.academicrecord.request;

import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;

/**
 * The filters #18 takes, all from the query string and all optional.
 *
 * <p>The year is in the path. <b>Sending no filter lists every record in that year</b>, closed
 * ones included; each filter sent narrows it further.
 */
public record StudentAcademicRecordSearchRequest(

        /** Only records in this class. */
        String classDocsId,

        /** Only records in this section. Usually sent with {@code classDocsId}. */
        String sectionNo,

        /** Only records with this status. {@code ACTIVE} is "who is placed right now". */
        AcademicRecordStatus status,

        /** Only this child's records in the year. */
        String studentDocsId,

        /** Page number, from 0. */
        Integer page,

        /** Rows per page, 1 to 100. */
        Integer size,

        /** {@code field} or {@code field,desc} — see the allowlist on the service. */
        String sort) {
}
