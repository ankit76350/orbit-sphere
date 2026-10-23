package com.orbitastra.backend.services.academics.examination.utils;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.academics.examination.ReportCard;
import com.orbitastra.backend.models.academics.structure.AcademicTerm;
import com.orbitastra.backend.models.academics.structure.SchoolClass;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.enums.AcademicRecordStatus;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.academicterm.AcademicTermRepository;
import com.orbitastra.backend.repositories.academics.reportcard.ReportCardRepository;
import com.orbitastra.backend.repositories.academics.schoolclass.SchoolClassRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;

import lombok.RequiredArgsConstructor;

/**
 * What the report-card endpoints share. A method here never calls another method here.
 */
@Component
@RequiredArgsConstructor
public class ReportCardServiceUtils {

    private final AcademicTermRepository academicTerms;
    private final StudentRepository students;
    private final StudentAcademicRecordRepository academicRecords;
    private final SchoolClassRepository schoolClasses;
    private final ReportCardRepository reportCards;

    /**
     * Whose card it is: the term, the child, the placement they hold on the term's last day, and
     * that placement's class.
     */
    public record CardOwner(AcademicTerm term, Student student, StudentAcademicRecord record,
            SchoolClass schoolClass) {
    }

    /**
     * The term, and whose card it is — the student record in the path, which must be this year's and
     * not cancelled. The card goes to that record's class and section, with its roll number.
     *
     * Used by:
     * - ReportCardService.draftCard()
     * - ReportCardService.saveCard()
     */
    public CardOwner ownerOf(School school, String academicYear, String termDocsId, String studentAcademicRecordDocsId) {
        // TODO: read academic term
        AcademicTerm term = academicTerms.findByIdAndSchoolIdAndAcademicYear(termDocsId, school.getId(), academicYear)
                .orElseThrow(() -> ApiException.notFound("TERM_NOT_FOUND",
                        "No term with id '" + termDocsId + "' in '" + academicYear + "'."));

        // TODO: read student academic record
        StudentAcademicRecord record = academicRecords.findByIdAndSchoolId(studentAcademicRecordDocsId, school.getId())
                .filter(one -> academicYear.equals(one.getAcademicYear()))
                .orElseThrow(() -> ApiException.notFound("STUDENT_RECORD_NOT_FOUND",
                        "No student record with id '" + studentAcademicRecordDocsId + "' in '" + academicYear + "'."));
        if (record.getStatus() == AcademicRecordStatus.CANCELLED) {
            throw ApiException.conflict("STUDENT_RECORD_CANCELLED",
                    "This student record was cancelled, so no card can be made on it.");
        }

        // TODO: read student
        Student student = students.findByIdAndSchoolId(record.getStudentDocsId(), school.getId())
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND",
                        "The student of this record is missing."));

        // TODO: read school class
        SchoolClass schoolClass = schoolClasses
                .findByIdAndSchoolIdAndAcademicYear(record.getClassDocsId(), school.getId(), academicYear)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND",
                        "The class of this student record is missing."));

        return new CardOwner(term, student, record, schoolClass);
    }

    /**
     * The child's latest card for the term — the highest version — or null when there is none.
     *
     * Used by:
     * - ReportCardService.draftCard()
     * - ReportCardService.saveCard()
     */
    public ReportCard latestCardOf(School school, String academicYear, String termDocsId, String studentDocsId) {
        // TODO: read report cards (this child, this term)
        return reportCards.findBySchoolIdAndAcademicYearAndTermDocsIdAndStudentDocsIdIn(school.getId(),
                academicYear, termDocsId, List.of(studentDocsId)).stream()
                .max(Comparator.comparing(ReportCard::getReportVersion))
                .orElse(null);
    }
}
