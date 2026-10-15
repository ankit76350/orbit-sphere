package com.orbitastra.backend.dto.academics.attendance.request;

import java.time.LocalDate;

import com.orbitastra.backend.models.academics.enums.AttendanceSessionStatus;
import com.orbitastra.backend.models.academics.enums.AttendanceSessionType;

/**
 * The filters attendance #7 takes, all from the query string and all optional. The year is in the
 * path; sending no filter lists every register of the year.
 */
public record AttendanceSessionSearchRequest(

        /** Only registers of this date. */
        LocalDate date,

        /** Only registers of this class. */
        String classDocsId,

        /** Only registers of this section. Usually with classDocsId. */
        String sectionNo,

        /** DAILY or PERIOD. */
        AttendanceSessionType sessionType,

        /** OPEN, LOCKED or CANCELLED. */
        AttendanceSessionStatus status,

        /** Page number, from 0. */
        Integer page,

        /** Rows per page, 1 to 100. */
        Integer size) {
}
