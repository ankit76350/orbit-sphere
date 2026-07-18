package com.orbitastra.backend.dto.academics;

import java.util.List;

import com.orbitastra.backend.models.academics.AcademicResult;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for recording an exam result. Server-owned fields ({@code id}
 * and audit timestamps) are not accepted from the request body.
 */
@Data
public class CreateAcademicResultRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    private String academicYear;

    @NotBlank(message = "studentId is required")
    private String studentId;

    private String grade;

    private String examName;

    private List<AcademicResult.SubjectMark> marks;

    private Double totalPercentage;

    private String overallGrade;

    private String feedback;
}
