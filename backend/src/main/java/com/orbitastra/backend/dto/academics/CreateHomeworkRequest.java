package com.orbitastra.backend.dto.academics;

import java.time.LocalDate;
import java.util.List;

import com.orbitastra.backend.models.academics.enums.AssignmentScope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for creating a homework (used by both the full-create and the
 * definition endpoints). The class, section and assigned teacher are required
 * target references. {@code academicYear}, {@code submittedCount}, per-student
 * submission text, grades and status are server-managed and are not accepted
 * from the body.
 */
@Data
public class CreateHomeworkRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    @NotBlank(message = "classDocsId is required")
    private String classDocsId;

    @NotBlank(message = "sectionNo is required")
    private String sectionNo;

    private String subject;

    private String title;

    private String instructions;

    private LocalDate dueDate;

    private AssignmentScope assignmentScope;

    @Min(value = 0, message = "maxMarks cannot be negative")
    private Integer maxMarks;

    @NotBlank(message = "teacherDocsId is required")
    private String teacherDocsId;

    @Valid
    private List<StudentAssignmentRequest> studentAssignments;
}
