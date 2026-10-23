package com.orbitastra.backend.services.academics.examination.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.dto.academics.examcandidate.response.ExamCandidateResponse;
import com.orbitastra.backend.models.academics.enums.ExamCandidateStatus;
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

    /**
     * Whether a candidate has ever been debarred from this exam — debarred now, or debarred at any
     * point and let back in since.
     *
     * <p><b>Why "ever" and not "on the paper's day":</b> a paper cannot be completed while an
     * eligible child on its register is unmarked (#16). So a child with no entry on a completed
     * paper was not eligible when its hall closed — and if they have ever been debarred, that is
     * why they missed it. Asking "debarred on that day" gets it wrong when the debarment and the
     * return both fall on the paper's day, which is exactly the fees-paid-that-morning case.
     *
     * Used by:
     * - ExamService.getTabulation()
     */
    public static boolean everDebarred(ExamCandidate candidate) {
        if (candidate.getStatus() == ExamCandidateStatus.DEBARRED) {
            return true;
        }
        return candidate.getStatusChanges() != null && candidate.getStatusChanges().stream()
                .anyMatch(change -> change.getStatus() == ExamCandidateStatus.DEBARRED);
    }
}
