package com.orbitastra.backend.services.academics;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.AcademicResult;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.academics.AcademicResultRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicResultService {

    private final AcademicResultRepository academicResultRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;

    private void validateStudent(String studentId, String schoolId) {
        if (studentId == null || studentId.isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty.");
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        if (!student.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Student does not belong to the same school as the academic result.");
        }
    }

    public AcademicResult createAcademicResult(AcademicResult academicResult) {
        if (academicResult.getSchoolId() == null || !schoolRepository.existsById(academicResult.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + academicResult.getSchoolId());
        }

        validateStudent(academicResult.getStudentId(), academicResult.getSchoolId());

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

    public List<AcademicResult> getAcademicResultsByStudent(String studentId) {
        return academicResultRepository.findByStudentId(studentId);
    }

    public List<AcademicResult> getAcademicResultsBySchoolAndStudent(String schoolId, String studentId) {
        return academicResultRepository.findBySchoolIdAndStudentId(schoolId, studentId);
    }

    public AcademicResult updateAcademicResult(String id, AcademicResult resultDetails) {
        AcademicResult academicResult = getAcademicResultById(id);

        if (resultDetails.getSchoolId() != null && !resultDetails.getSchoolId().equals(academicResult.getSchoolId())) {
            if (!schoolRepository.existsById(resultDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + resultDetails.getSchoolId());
            }
            academicResult.setSchoolId(resultDetails.getSchoolId());
        }

        if (resultDetails.getStudentId() != null && !resultDetails.getStudentId().equals(academicResult.getStudentId())) {
            validateStudent(resultDetails.getStudentId(), academicResult.getSchoolId());
            academicResult.setStudentId(resultDetails.getStudentId());
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
