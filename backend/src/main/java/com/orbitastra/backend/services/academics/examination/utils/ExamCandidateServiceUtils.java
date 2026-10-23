package com.orbitastra.backend.services.academics.examination.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.dto.academics.examcandidate.response.ExamCandidateResponse;
import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;

import lombok.RequiredArgsConstructor;

/**
 * What the candidate endpoints share. A method here never calls another method here.
 */
@Component
@RequiredArgsConstructor
public class ExamCandidateServiceUtils {

    private final StudentAcademicRecordRepository academicRecords;
    private final SchoolClassRepository schoolClasses;
    private final StudentRepository students;

    /**
     * One candidate as #44 lists them — with their name, and the class, section and roll from the
     * academic record they were added with. A record, class or child that cannot be found is left
     * out of the row rather than failing the answer.
     *
     * Used by:
     * - ExamCandidateService.debarCandidate()
     * - ExamCandidateService.readmitCandidate()
     */
    public ExamCandidateResponse rowOf(School school, String academicYear, ExamCandidate candidate) {
        // TODO: read student academic record
        StudentAcademicRecord record = academicRecords
                .findByIdAndSchoolId(candidate.getStudentAcademicRecordDocsId(), school.getId())
                .orElse(null);

        String className = null;
        if (record != null) {
            // TODO: read school class
            className = schoolClasses.findByIdAndSchoolIdAndAcademicYear(record.getClassDocsId(),
                    school.getId(), academicYear).map(SchoolClass::getName).orElse(null);
        }

        // TODO: read student
        Student child = students.findByIdAndSchoolId(candidate.getStudentDocsId(), school.getId())
                .orElse(null);

        return ExamCandidateResponse.of(candidate, child, record, className);
    }
}
