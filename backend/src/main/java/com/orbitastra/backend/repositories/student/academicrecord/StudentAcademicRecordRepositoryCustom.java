package com.orbitastra.backend.repositories.student.academicrecord;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.orbitastra.backend.dto.student.academicrecord.request.StudentAcademicRecordSearchRequest;
import com.orbitastra.backend.models.student.StudentAcademicRecord;

/** The two academic-record reads that a derived query cannot express. */
public interface StudentAcademicRecordRepositoryCustom {

    /** #18 — one page of a year's records, with whichever filters were sent. */
    Page<StudentAcademicRecord> search(String schoolId, String academicYear,
            StudentAcademicRecordSearchRequest request, Pageable pageable);

    /** #22 — how many ACTIVE records each class and section of a year has, in one aggregation. */
    List<SectionCount> countActiveBySection(String schoolId, String academicYear);

    /** One group from the aggregation: a class, a section, and how many children are in it. */
    record SectionCount(String classDocsId, String sectionNo, long enrolled) {
    }

    /**
     * <b>Who was in one section on one date</b> — the records whose placement covered that day:
     * same class and section, {@code effectiveFrom} on or before it, {@code effectiveUntil} empty
     * or on or after it, and not {@code CANCELLED}.
     *
     * <p><b>Not "who is ACTIVE today"</b>: a child who transferred out on Wednesday was still in
     * Monday's section. Both ends are inclusive, so on the day of a transfer the child is in both
     * the section they left and the one they joined.
     *
     * <p>Used by attendance #6 today; it is the same question the roster (student #21) answers,
     * written once so the two cannot disagree.
     */
    List<StudentAcademicRecord> findInSectionOn(String schoolId, String academicYear,
            String classDocsId, String sectionNo, LocalDate date);

    /**
     * <b>How many children were in each section on one date</b> — the same date rule as
     * {@link #findInSectionOn}, for the whole year at once, in one aggregation. Used by attendance
     * #8, which needs a headcount for every section without reading every child.
     */
    List<SectionCount> countInSectionsOn(String schoolId, String academicYear, LocalDate date);
}
