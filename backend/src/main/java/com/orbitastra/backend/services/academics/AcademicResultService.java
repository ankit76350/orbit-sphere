package com.orbitastra.backend.services.academics;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.AcademicResult;
import com.orbitastra.backend.repositories.academics.AcademicResultRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;
import com.orbitastra.backend.services.utils.StudentValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicResultService {

    private final AcademicResultRepository academicResultRepository;
    private final SchoolRepository schoolRepository;
    private final StudentValidator studentValidator;
    private final AcademicYearResolver academicYearResolver;

    public AcademicResult createAcademicResult(AcademicResult academicResult) {
        if (academicResult.getSchoolId() == null || !schoolRepository.existsById(academicResult.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + academicResult.getSchoolId());
        }

        studentValidator.validateStudent(academicResult.getStudentDocsId(), academicResult.getSchoolId());

        // No natural date on a result — the academic year must be supplied.
        academicResult.setAcademicYear(academicYearResolver
                .resolve(academicResult.getSchoolId(), academicResult.getAcademicYear(), null)
                .getName());

        return academicResultRepository.save(academicResult);
    }

    public List<AcademicResult> getAllAcademicResults() {
        return academicResultRepository.findAll();
    }

    public AcademicResult getAcademicResultById(String id) {
        return academicResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic result not found with id: " + id));
    }

    public List<AcademicResult> getAcademicResultsBySchool(String schoolId) {
        return academicResultRepository.findBySchoolId(schoolId);
    }

    public List<AcademicResult> getAcademicResultsBySchoolAndAcademicYear(String schoolId, String academicYear) {
        return academicResultRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
    }

    public List<AcademicResult> getAcademicResultsByStudent(String studentDocsId) {
        return academicResultRepository.findByStudentDocsId(studentDocsId);
    }

    public List<AcademicResult> getAcademicResultsBySchoolAndStudent(String schoolId, String studentDocsId) {
        return academicResultRepository.findBySchoolIdAndStudentDocsId(schoolId, studentDocsId);
    }

    public AcademicResult updateAcademicResult(String id, AcademicResult resultDetails) {
        AcademicResult academicResult = getAcademicResultById(id);
        academicYearResolver.assertImmutable(academicResult.getAcademicYear(), resultDetails.getAcademicYear());

        if (resultDetails.getSchoolId() != null && !resultDetails.getSchoolId().equals(academicResult.getSchoolId())) {
            if (!schoolRepository.existsById(resultDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + resultDetails.getSchoolId());
            }
            academicResult.setSchoolId(resultDetails.getSchoolId());
        }

        if (resultDetails.getStudentDocsId() != null && !resultDetails.getStudentDocsId().equals(academicResult.getStudentDocsId())) {
            studentValidator.validateStudent(resultDetails.getStudentDocsId(), academicResult.getSchoolId());
            academicResult.setStudentDocsId(resultDetails.getStudentDocsId());
        }

        if (resultDetails.getGrade() != null) {
            academicResult.setGrade(resultDetails.getGrade());
        }
        if (resultDetails.getExamName() != null) {
            academicResult.setExamName(resultDetails.getExamName());
        }
        if (resultDetails.getMarks() != null) {
            academicResult.setMarks(resultDetails.getMarks());
        }
        if (resultDetails.getTotalPercentage() != null) {
            academicResult.setTotalPercentage(resultDetails.getTotalPercentage());
        }
        if (resultDetails.getOverallGrade() != null) {
            academicResult.setOverallGrade(resultDetails.getOverallGrade());
        }
        if (resultDetails.getFeedback() != null) {
            academicResult.setFeedback(resultDetails.getFeedback());
        }

        return academicResultRepository.save(academicResult);
    }

    public void deleteAcademicResult(String id) {
        AcademicResult academicResult = getAcademicResultById(id);
        academicResultRepository.delete(academicResult);
    }
}
