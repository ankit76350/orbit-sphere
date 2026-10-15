package com.orbitastra.backend.repositories.student.academicrecord;

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
}
