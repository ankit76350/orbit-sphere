package com.orbitastra.backend.dto.academics;

import java.util.List;

import com.orbitastra.backend.models.academics.AcademicResult;

import lombok.Data;

/**
 * Partial-update payload for an exam result (PATCH). All fields optional; only
 * non-null fields are applied. {@code academicYear} is accepted only so the
 * service can reject an attempt to change it.
 */
@Data
public class UpdateAcademicResultRequest {

    private String schoolId;

    private String academicYear;

    private String studentId;

    private String grade;

    private String examName;

    private List<AcademicResult.SubjectMark> marks;

    private Double totalPercentage;

    private String overallGrade;

    private String feedback;
}
